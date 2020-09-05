/* Copyright (c) 2010, NullNoname
 * All rights reserved.
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * Neither the name of NullNoname nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE. */
package mu.nu.nullpo.game.subsystem.mode

import mu.nu.nullpo.game.component.*
import mu.nu.nullpo.game.event.EventReceiver.COLOR
import mu.nu.nullpo.game.net.NetPlayerClient
import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.game.play.GameManager
import mu.nu.nullpo.gui.net.NetLobbyFrame
import mu.nu.nullpo.util.GeneralUtil
import java.io.IOException
import java.util.*
import kotlin.math.abs

/** NET-VS-BATTLE Mode */
class NetVSBattleMode:NetDummyVSMode() {

	/** Column number of hole in most recent garbage line */
	private var lastHole = -1

	/** true if Hurry Up has been started */
	private var hurryupStarted:Boolean = false

	/** Number of frames left to show "HURRY UP!" text */
	private var hurryupShowFrames:Int = 0

	/** Number of pieces placed after Hurry Up has started */
	private var hurryupCount:Int = 0

	/** true if you KO'd player */
	private var playerKObyYou:BooleanArray=BooleanArray(0)

	/** Your KO count */
	private var currentKO:Int = 0

	/** Time to display the most recent increase in score */
	private var scgettime:IntArray = IntArray(0)

	/** Most recent scoring event type */
	private var lastevent:IntArray = IntArray(0)

	/** true if most recent scoring event was B2B */
	private var lastb2b:BooleanArray=BooleanArray(0)

	/** Most recent scoring event Combo count */
	private var lastcombo:IntArray = IntArray(0)

	/** Most recent scoring event piece type */
	private var lastpiece:IntArray = IntArray(0)

	/** Count of garbage lines send */
	private var garbageSent:IntArray = IntArray(0)

	/** Amount of garbage in garbage queue */
	private var garbage:IntArray = IntArray(0)

	/** Recieved garbage entries */
	private var garbageEntries:LinkedList<GarbageEntry> = LinkedList()

	/** APL (Attack Per Line) */
	private var playerAPL:FloatArray=FloatArray(0)

	/** APM (Attack Per Minute) */
	private var playerAPM:FloatArray=FloatArray(0)

	/** Target ID (-1:All) */
	private var targetID:Int = 0

	/** Target Timer */
	private var targetTimer:Int = 0

	/* Mode name */
	override val name:String
		get() = "NET-VS-BATTLE"

	override val isVSMode:Boolean
		get() = true

	/** Get number of possible targets (number of opponents)
	 * @return Number of possible targets (number of opponents)
	 */
	private val numberOfPossibleTargets:Int
		get() {
			var count = 0
			for(i in 1 until players)
				if(netvsIsAttackable(i)) count++
			return count
		}

	/** Get number of garbage lines the local player has
	 * @return Number of garbage lines
	 */
	private val totalGarbageLines:Int
		get() {
			var count = 0
			for(entry in garbageEntries)
				count += entry.lines
			return count
		}

	/* Mode Initialization */
	override fun modeInit(manager:GameManager) {
		super.modeInit(manager)
		playerKObyYou = BooleanArray(NETVS_MAX_PLAYERS)
		scgettime = IntArray(NETVS_MAX_PLAYERS)
		lastevent = IntArray(NETVS_MAX_PLAYERS)
		lastb2b = BooleanArray(NETVS_MAX_PLAYERS)
		lastcombo = IntArray(NETVS_MAX_PLAYERS)
		lastpiece = IntArray(NETVS_MAX_PLAYERS)
		garbageSent = IntArray(NETVS_MAX_PLAYERS)
		garbage = IntArray(NETVS_MAX_PLAYERS)
		playerAPL = FloatArray(NETVS_MAX_PLAYERS)
		playerAPM = FloatArray(NETVS_MAX_PLAYERS)
	}

	/** Set new target */
	private fun setNewTarget() {
		if(numberOfPossibleTargets>=1&&netCurrentRoomInfo!=null&&netCurrentRoomInfo!!.isTarget&&
			!netvsIsWatch()&&!netvsIsPractice)
			do {
				targetID++
				if(targetID>=players) targetID = 1
			} while(!netvsIsAttackable(targetID))
		else
			targetID = -1
	}

	/* Initialization for each player */
	override fun playerInit(engine:GameEngine, playerID:Int) {
		super.playerInit(engine, playerID)

		if(playerID==0&&!netvsIsWatch()) {
			lastHole = -1
			hurryupCount = 0
			currentKO = 0
			targetID = -1
			targetTimer = 0

			if(garbageEntries==null)
				garbageEntries = LinkedList()
			else
				garbageEntries.clear()
		}

		playerKObyYou[playerID] = false
		scgettime[playerID] = 0
		lastevent[playerID] = EVENT_NONE
		lastb2b[playerID] = false
		lastcombo[playerID] = 0
		lastpiece[playerID] = 0
		garbageSent[playerID] = 0
		garbage[playerID] = 0
		playerAPL[playerID] = 0f
		playerAPM[playerID] = 0f
	}

	/* Executed after Ready->Go, before the first piece appears. */
	override fun startGame(engine:GameEngine, playerID:Int) {
		super.startGame(engine, playerID)

		if(playerID==0&&!netvsIsWatch()) {
			if(!netvsIsPractice) {
				hurryupStarted = false
				hurryupShowFrames = 0
			}
			setNewTarget()
			targetTimer = 0
		}
	}

	/* Calculate Score */
	override fun calcScore(engine:GameEngine, playerID:Int, lines:Int):Int {
		// Attack
		if(lines>0&&playerID==0) {
			val pts = IntArray(ATTACK_CATEGORIES)

			scgettime[playerID] = 0

			val numAliveTeams = netvsGetNumberOfTeamsAlive()
			var attackNumPlayerIndex = numAliveTeams-2
			if(netvsIsPractice||!netCurrentRoomInfo!!.reduceLineSend) attackNumPlayerIndex = 0
			if(attackNumPlayerIndex<0) attackNumPlayerIndex = 0
			if(attackNumPlayerIndex>4) attackNumPlayerIndex = 4

			var attackLineIndex = LINE_ATTACK_INDEX_SINGLE
			var mainAttackCategory = ATTACK_CATEGORY_NORMAL

			// Twister style attack
			if(engine.twist) {
				mainAttackCategory = ATTACK_CATEGORY_SPIN

				// EZ-T
				if(engine.twistez) {
					attackLineIndex = LINE_ATTACK_INDEX_EZ_T
					lastevent[playerID] = EVENT_TWIST_EZ
				} else if(lines==1) {
					if(engine.twistmini) {
						attackLineIndex = LINE_ATTACK_INDEX_TMINI
						lastevent[playerID] = EVENT_TWIST_SINGLE_MINI
					} else {
						attackLineIndex = LINE_ATTACK_INDEX_TSINGLE
						lastevent[playerID] = EVENT_TWIST_SINGLE
					}
				} else if(lines==2) {
					if(engine.twistmini&&engine.useAllSpinBonus) {
						attackLineIndex = LINE_ATTACK_INDEX_TMINI_D
						lastevent[playerID] = EVENT_TWIST_DOUBLE_MINI
					} else {
						attackLineIndex = LINE_ATTACK_INDEX_TDOUBLE
						lastevent[playerID] = EVENT_TWIST_DOUBLE
					}
				} else if(lines>=3) {
					attackLineIndex = LINE_ATTACK_INDEX_TTRIPLE
					lastevent[playerID] = EVENT_TWIST_TRIPLE
				}// Twister 3 lines
				// Twister 2 lines
				// Twister 1 line
			} else // Single
				if(lines==1) {
					attackLineIndex = LINE_ATTACK_INDEX_SINGLE
					lastevent[playerID] = EVENT_SINGLE
				} else if(lines==2) {
					attackLineIndex = LINE_ATTACK_INDEX_DOUBLE
					lastevent[playerID] = EVENT_DOUBLE
				} else if(lines==3) {
					attackLineIndex = LINE_ATTACK_INDEX_TRIPLE
					lastevent[playerID] = EVENT_TRIPLE
				} else if(lines>=4) {
					attackLineIndex = LINE_ATTACK_INDEX_FOUR
					lastevent[playerID] = EVENT_FOUR
				}// Four
			// Triple
			// Double

			if(engine.useAllSpinBonus)
				pts[mainAttackCategory] += LINE_ATTACK_TABLE_ALLSPIN[attackLineIndex][attackNumPlayerIndex]
			else
				pts[mainAttackCategory] += LINE_ATTACK_TABLE[attackLineIndex][attackNumPlayerIndex]

			// B2B
			if(engine.b2b) {
				lastb2b[playerID] = true

				if(pts[mainAttackCategory]>0)
					if(attackLineIndex==LINE_ATTACK_INDEX_TTRIPLE&&!engine.useAllSpinBonus)
						pts[ATTACK_CATEGORY_B2B] += 2
					else
						pts[ATTACK_CATEGORY_B2B] += 1
			} else
				lastb2b[playerID] = false

			// Combo
			if(engine.comboType!=GameEngine.COMBO_TYPE_DISABLE) {
				var cmbindex = engine.combo-1
				if(cmbindex<0) cmbindex = 0
				if(cmbindex>=COMBO_ATTACK_TABLE[attackNumPlayerIndex].size)
					cmbindex = COMBO_ATTACK_TABLE[attackNumPlayerIndex].size-1
				pts[ATTACK_CATEGORY_COMBO] += COMBO_ATTACK_TABLE[attackNumPlayerIndex][cmbindex]
				lastcombo[playerID] = engine.combo
			}

			// All clear (Bravo)
			if(lines>=1&&engine.field!!.isEmpty&&netCurrentRoomInfo!!.bravo)
				pts[ATTACK_CATEGORY_BRAVO] += 6

			// Gem block attack
			pts[ATTACK_CATEGORY_GEM] += engine.field!!.howManyGemClears

			lastpiece[playerID] = engine.nowPieceObject!!.id

			for(i in pts.indices)
				pts[i] *= GARBAGE_DENOMINATOR
			if(netCurrentRoomInfo!!.useFractionalGarbage&&!netvsIsPractice)
				if(numAliveTeams>=3)
					for(i in pts.indices)
						pts[i] = pts[i]/(numAliveTeams-1)

			// Attack lines count
			for(i in pts)
				garbageSent[playerID] += i

			// Garbage countering
			garbage[playerID] = totalGarbageLines
			for(i in pts.indices)
				if(pts[i]>0&&garbage[playerID]>0
					&&netCurrentRoomInfo!!.counter)
					while((!netCurrentRoomInfo!!.useFractionalGarbage&&!garbageEntries.isEmpty()
							&&pts[i]>0)||netCurrentRoomInfo!!.useFractionalGarbage&&!garbageEntries.isEmpty()&&pts[i]>=GARBAGE_DENOMINATOR) {
						val garbageEntry = garbageEntries.first
						garbageEntry.lines -= pts[i]

						if(garbageEntry.lines<=0) {
							pts[i] = abs(garbageEntry.lines)
							garbageEntries.removeFirst()
						} else
							pts[i] = 0
					}

			// Send garbage lines
			if(!netvsIsPractice) {
				garbage[playerID] = totalGarbageLines

				val stringPts = StringBuilder()
				for(i in pts)
					stringPts.append(i).append("\t")

				if(targetID!=-1&&!netvsIsAttackable(targetID)) setNewTarget()
				val targetSeatID = if(targetID==-1) -1 else netvsPlayerSeatID[targetID]

				netLobby!!.netPlayerClient!!.send("game\tattack\t$stringPts\t${lastevent[playerID]}\t"+lastb2b[playerID]
					+"\t"+
					lastcombo[playerID]+"\t${garbage[playerID]}\t${lastpiece[playerID]}\t$targetSeatID\n")
			}
		}

		// Garbage lines appear
		if((lines==0||!netCurrentRoomInfo!!.rensaBlock)&&totalGarbageLines>=GARBAGE_DENOMINATOR&&!netvsIsPractice) {
			engine.playSE("garbage${if(totalGarbageLines-GARBAGE_DENOMINATOR>3)1 else 0}")

			var smallGarbageCount = 0
			var hole = lastHole
			var newHole:Int
			if(hole==-1) hole = engine.random.nextInt(engine.field!!.width)

			var finalGarbagePercent = netCurrentRoomInfo!!.garbagePercent
			if(netCurrentRoomInfo!!.divideChangeRateByPlayers) finalGarbagePercent /= netvsGetNumberOfTeamsAlive()-1

			// Make regular garbage lines appear
			while(!garbageEntries.isEmpty()) {
				val garbageEntry = garbageEntries.poll()
				smallGarbageCount += garbageEntry.lines%GARBAGE_DENOMINATOR

				if(garbageEntry.lines/GARBAGE_DENOMINATOR>0) {
					val seatFrom = netvsPlayerSeatID[garbageEntry.playerID]
					val garbageColor = if(seatFrom<0) Block.BLOCK_COLOR_GRAY else NETVS_PLAYER_COLOR_BLOCK[seatFrom]
					netvsLastAttackerUID = garbageEntry.uid
					if(netCurrentRoomInfo!!.garbageChangePerAttack) {
						if(engine.random.nextInt(100)<finalGarbagePercent) {
							newHole = engine.random.nextInt(engine.field!!.width-1)
							if(newHole>=hole) newHole++
							hole = newHole
						}
						engine.field!!.addSingleHoleGarbage(hole, garbageColor, engine.skin, garbageEntry.lines/GARBAGE_DENOMINATOR)
					} else
						for(i in garbageEntry.lines/GARBAGE_DENOMINATOR downTo 1) {
							if(engine.random.nextInt(100)<finalGarbagePercent) {
								newHole = engine.random.nextInt(engine.field!!.width-1)
								if(newHole>=hole) newHole++
								hole = newHole
							}

							engine.field!!.addSingleHoleGarbage(hole, garbageColor, engine.skin, 1)
						}
				}
			}

			// Make small garbage lines appear
			if(smallGarbageCount>0) {
				if(smallGarbageCount/GARBAGE_DENOMINATOR>0) {
					netvsLastAttackerUID = -1

					if(netCurrentRoomInfo!!.garbageChangePerAttack) {
						if(engine.random.nextInt(100)<finalGarbagePercent) {
							newHole = engine.random.nextInt(engine.field!!.width-1)
							if(newHole>=hole) newHole++
							hole = newHole
						}
						engine.field!!.addSingleHoleGarbage(hole, Block.BLOCK_COLOR_GRAY, engine.skin, smallGarbageCount/GARBAGE_DENOMINATOR)
					} else
						for(i in smallGarbageCount/GARBAGE_DENOMINATOR downTo 1) {
							if(engine.random.nextInt(100)<finalGarbagePercent) {
								newHole = engine.random.nextInt(engine.field!!.width-1)
								if(newHole>=hole) newHole++
								hole = newHole
							}

							engine.field!!.addSingleHoleGarbage(hole, Block.BLOCK_COLOR_GRAY, engine.skin, 1)
						}
				}

				if(smallGarbageCount%GARBAGE_DENOMINATOR>0) {
					val smallGarbageEntry = GarbageEntry(smallGarbageCount%GARBAGE_DENOMINATOR, -1)
					garbageEntries.add(smallGarbageEntry)
				}
			}

			lastHole = hole
			garbage[playerID] = totalGarbageLines
		}

		// HURRY UP!
		if(netCurrentRoomInfo!!.hurryupSeconds>=0&&engine.timerActive&&!netvsIsPractice)
			if(hurryupStarted) {
				hurryupCount++

				if(hurryupCount%netCurrentRoomInfo!!.hurryupInterval==0) engine.field!!.addHurryupFloor(1, engine.skin)
			} else
				hurryupCount = netCurrentRoomInfo!!.hurryupInterval-1
		return 0
	}

	/* Executed at the end of each frame */
	override fun onLast(engine:GameEngine, playerID:Int) {
		super.onLast(engine, playerID)

		scgettime[playerID]++
		if(playerID==0&&hurryupShowFrames>0) hurryupShowFrames--

		// HURRY UP!
		if(playerID==0&&engine.timerActive&&netCurrentRoomInfo!=null&&netCurrentRoomInfo!!.hurryupSeconds>=0&&
			netvsPlayTimer==netCurrentRoomInfo!!.hurryupSeconds*60&&!hurryupStarted) {
			if(!netvsIsWatch()&&!netvsIsPractice) {
				netLobby!!.netPlayerClient!!.send("game\thurryup\n")
				engine.playSE("hurryup")
			}
			hurryupStarted = true
			hurryupShowFrames = 60*5
		}

		// Garbage meter
		val tempGarbage = garbage[playerID]/GARBAGE_DENOMINATOR
		val tempGarbageF = garbage[playerID].toFloat()/GARBAGE_DENOMINATOR
		val newMeterValue = (tempGarbageF*owner.receiver.getBlockSize(engine)).toInt()
		if(playerID==0&&!netvsIsWatch()) {
			if(newMeterValue>engine.meterValue) {
				engine.meterValue += owner.receiver.getBlockSize(engine)/2
				if(engine.meterValue>newMeterValue) engine.meterValue = newMeterValue
			} else if(newMeterValue<engine.meterValue) engine.meterValue--
		} else
			engine.meterValue = newMeterValue
		engine.meterColor = when {
			tempGarbage>=4 -> GameEngine.METER_COLOR_RED
			tempGarbage>=3 -> GameEngine.METER_COLOR_ORANGE
			tempGarbage>=1 -> GameEngine.METER_COLOR_YELLOW
			else -> GameEngine.METER_COLOR_GREEN
		}

		// APL & APM
		if(playerID==0&&engine.gameActive&&engine.timerActive&&!netvsIsWatch()) {
			val tempGarbageSent = garbageSent[playerID].toFloat()/GARBAGE_DENOMINATOR
			playerAPM[0] = tempGarbageSent*3600/engine.statistics.time

			if(engine.statistics.lines>0)
				playerAPL[0] = tempGarbageSent/engine.statistics.lines
			else
				playerAPL[0] = 0f
		}

		// Target
		if(playerID==0&&!netvsIsWatch()&&netvsPlayTimerActive&&engine.gameActive&&engine.timerActive&&
			numberOfPossibleTargets>=1&&netCurrentRoomInfo!=null&&netCurrentRoomInfo!!.isTarget) {
			targetTimer++

			if(targetTimer>=netCurrentRoomInfo!!.targetTimer||!netvsIsAttackable(targetID)) {
				targetTimer = 0
				setNewTarget()
			}
		}
	}

	/* Drawing processing at the end of every frame */
	override fun renderLast(engine:GameEngine, playerID:Int) {
		super.renderLast(engine, playerID)

		val x = owner.receiver.fieldX(engine, playerID)
		val y = owner.receiver.fieldY(engine, playerID)

		if(netvsPlayerExist[playerID]&&engine.isVisible) {
			// Garbage Count
			if(garbage[playerID]>0&&netCurrentRoomInfo!!.useFractionalGarbage&&engine.stat!=GameEngine.Status.RESULT) {
				val strTempGarbage:String

				var fontColor = COLOR.WHITE
				if(garbage[playerID]>=GARBAGE_DENOMINATOR) fontColor = COLOR.YELLOW
				if(garbage[playerID]>=GARBAGE_DENOMINATOR*3) fontColor = COLOR.ORANGE
				if(garbage[playerID]>=GARBAGE_DENOMINATOR*4) fontColor = COLOR.RED

				if(engine.displaysize!=-1) {
					strTempGarbage = String.format(Locale.US, "%5.2f", garbage[playerID].toFloat()/GARBAGE_DENOMINATOR)
					owner.receiver.drawDirectFont(x+96, y+372, strTempGarbage, fontColor, 1f)
				} else {
					strTempGarbage = String.format(Locale.US, "%4.1f", garbage[playerID].toFloat()/GARBAGE_DENOMINATOR)
					owner.receiver.drawDirectFont(x+64, y+168, strTempGarbage, fontColor, .5f)
				}
			}

			// Target
			if(playerID==targetID&&netCurrentRoomInfo!=null&&netCurrentRoomInfo!!.isTarget&&netvsNumAlivePlayers>=3&&
				netvsIsGameActive&&netvsIsAttackable(playerID)&&!netvsIsWatch()) {
				var fontcolor = COLOR.GREEN
				if(targetTimer>=netCurrentRoomInfo!!.targetTimer-20&&targetTimer%2==0)
					fontcolor = COLOR.WHITE

				if(engine.displaysize!=-1)
					owner.receiver.drawMenuFont(engine, playerID, 2, 12, "TARGET", fontcolor)
				else
					owner.receiver.drawDirectFont(x+4+16, y+80, "TARGET", fontcolor, .5f)
			}
		}

		// Practice mode
		if(playerID==0&&netvsIsPractice&&netvsIsPracticeExitAllowed
			&&engine.stat!=GameEngine.Status.RESULT)
			if(lastevent[playerID]==EVENT_NONE||scgettime[playerID]>=120)
				owner.receiver.drawMenuFont(engine, 0, 0, 21, "F("
					+owner.receiver.getKeyNameByButtonID(engine, Controller.BUTTON_F)
					+" KEY):\n END GAME", COLOR.PURPLE)

		// Hurry Up
		if(netCurrentRoomInfo!=null&&playerID==0)
			if(netCurrentRoomInfo!!.hurryupSeconds>=0&&hurryupShowFrames>0
				&&!netvsIsPractice&&hurryupStarted)
				owner.receiver.drawDirectFont(playerID, 256-8, 32, "HURRY UP!", hurryupShowFrames%2==0)

		// Bottom message
		if(netvsPlayerExist[playerID]&&engine.isVisible)
		// K.O.
			if(playerKObyYou[playerID]) {
				if(engine.displaysize!=-1)
					owner.receiver.drawMenuFont(engine, playerID, 3, 21, "K.O.", COLOR.PINK)
				else
					owner.receiver.drawDirectFont(x+4+24, y+168, "K.O.", COLOR.PINK, .5f)
			} else if(lastevent[playerID]!=EVENT_NONE&&scgettime[playerID]<120) {
				val strPieceName = Piece.Shape.names[lastpiece[playerID]]

				if(engine.displaysize!=-1) {
					when(lastevent[playerID]) {
						EVENT_SINGLE -> owner.receiver.drawMenuFont(engine, playerID, 2, 21, "SINGLE", COLOR.COBALT)
						EVENT_DOUBLE -> owner.receiver.drawMenuFont(engine, playerID, 2, 21, "DOUBLE", COLOR.BLUE)
						EVENT_TRIPLE -> owner.receiver.drawMenuFont(engine, playerID, 2, 21, "TRIPLE", COLOR.GREEN)
						EVENT_FOUR -> if(lastb2b[playerID])
							owner.receiver.drawMenuFont(engine, playerID, 3, 21, "FOUR", COLOR.RED)
						else
							owner.receiver.drawMenuFont(engine, playerID, 3, 21, "FOUR", COLOR.ORANGE)
						EVENT_TWIST_SINGLE_MINI -> if(lastb2b[playerID])
							owner.receiver.drawMenuFont(engine, playerID, 1, 21, "$strPieceName-MINI-S", COLOR.RED)
						else
							owner.receiver.drawMenuFont(engine, playerID, 1, 21, "$strPieceName-MINI-S", COLOR.ORANGE)
						EVENT_TWIST_SINGLE -> if(lastb2b[playerID])
							owner.receiver.drawMenuFont(engine, playerID, 1, 21, "$strPieceName-SINGLE", COLOR.RED)
						else
							owner.receiver.drawMenuFont(engine, playerID, 1, 21, "$strPieceName-SINGLE", COLOR.ORANGE)
						EVENT_TWIST_DOUBLE_MINI -> if(lastb2b[playerID])
							owner.receiver.drawMenuFont(engine, playerID, 1, 21, "$strPieceName-MINI-D", COLOR.RED)
						else
							owner.receiver.drawMenuFont(engine, playerID, 1, 21, "$strPieceName-MINI-D", COLOR.ORANGE)
						EVENT_TWIST_DOUBLE -> if(lastb2b[playerID])
							owner.receiver.drawMenuFont(engine, playerID, 1, 21, "$strPieceName-DOUBLE", COLOR.RED)
						else
							owner.receiver.drawMenuFont(engine, playerID, 1, 21, "$strPieceName-DOUBLE", COLOR.ORANGE)
						EVENT_TWIST_TRIPLE -> if(lastb2b[playerID])
							owner.receiver.drawMenuFont(engine, playerID, 1, 21, "$strPieceName-TRIPLE", COLOR.RED)
						else
							owner.receiver.drawMenuFont(engine, playerID, 1, 21, "$strPieceName-TRIPLE", COLOR.ORANGE)
						EVENT_TWIST_EZ -> if(lastb2b[playerID])
							owner.receiver.drawMenuFont(engine, playerID, 3, 21, "EZ-$strPieceName", COLOR.RED)
						else
							owner.receiver.drawMenuFont(engine, playerID, 3, 21, "EZ-$strPieceName", COLOR.ORANGE)
					}

					if(lastcombo[playerID]>=2)
						owner.receiver.drawMenuFont(engine, playerID, 2, 22, (lastcombo[playerID]-1).toString()+"COMBO", COLOR.CYAN)
				} else {
					var x2 = 8
					if(Objects.requireNonNull(netCurrentRoomInfo)!!.useFractionalGarbage&&garbage[playerID]>0) x2 = 0

					when(lastevent[playerID]) {
						EVENT_SINGLE -> owner.receiver.drawDirectFont(x+4+16, y+168, "SINGLE", COLOR.COBALT, .5f)
						EVENT_DOUBLE -> owner.receiver.drawDirectFont(x+4+16, y+168, "DOUBLE", COLOR.BLUE, .5f)
						EVENT_TRIPLE -> owner.receiver.drawDirectFont(x+4+16, y+168, "TRIPLE", COLOR.GREEN, .5f)
						EVENT_FOUR -> if(lastb2b[playerID])
							owner.receiver.drawDirectFont(x-4, y+168, "QUADRUPLE", COLOR.RED, .5f)
						else
							owner.receiver.drawDirectFont(x-4, y+168, "QUADRUPLE", COLOR.ORANGE, .5f)
						EVENT_TWIST_SINGLE_MINI -> if(lastb2b[playerID])
							owner.receiver.drawDirectFont(x+4+x2, y+168, "$strPieceName-MINI-S", COLOR.RED, .5f)
						else
							owner.receiver.drawDirectFont(x+4+x2, y+168, "$strPieceName-MINI-S", COLOR.ORANGE, .5f)
						EVENT_TWIST_SINGLE -> if(lastb2b[playerID])
							owner.receiver.drawDirectFont(x+4+x2, y+168, "$strPieceName-SINGLE", COLOR.RED, .5f)
						else
							owner.receiver.drawDirectFont(x+4+x2, y+168, "$strPieceName-SINGLE", COLOR.ORANGE, .5f)
						EVENT_TWIST_DOUBLE_MINI -> if(lastb2b[playerID])
							owner.receiver.drawDirectFont(x+4+x2, y+168, "$strPieceName-MINI-D", COLOR.RED, .5f)
						else
							owner.receiver.drawDirectFont(x+4+x2, y+168, "$strPieceName-MINI-D", COLOR.ORANGE, .5f)
						EVENT_TWIST_DOUBLE -> if(lastb2b[playerID])
							owner.receiver.drawDirectFont(x+4+x2, y+168, "$strPieceName-DOUBLE", COLOR.RED, .5f)
						else
							owner.receiver.drawDirectFont(x+4+x2, y+168, "$strPieceName-DOUBLE", COLOR.ORANGE, .5f)
						EVENT_TWIST_TRIPLE -> if(lastb2b[playerID])
							owner.receiver.drawDirectFont(x+4+x2, y+168, "$strPieceName-TRIPLE", COLOR.RED, .5f)
						else
							owner.receiver.drawDirectFont(x+4+x2, y+168, "$strPieceName-TRIPLE", COLOR.ORANGE, .5f)
						EVENT_TWIST_EZ -> if(lastb2b[playerID])
							owner.receiver.drawDirectFont(x+4+24, y+168, "EZ-$strPieceName", COLOR.RED, .5f)
						else
							owner.receiver.drawDirectFont(x+4+24, y+168, "EZ-$strPieceName", COLOR.ORANGE, .5f)
					}

					if(lastcombo[playerID]>=2)
						owner.receiver.drawDirectFont(x+4+16, y+176, ((lastcombo[playerID]-1).toString()+"COMBO"), COLOR.CYAN, .5f)
				}
			} else if(!netvsIsPractice||playerID!=0) {
				val strTemp = "${netvsPlayerWinCount[playerID]}/${netvsPlayerPlayCount[playerID]}"

				if(engine.displaysize!=-1) {
					var y2 = 21
					if(engine.stat==GameEngine.Status.RESULT) y2 = 22
					owner.receiver.drawMenuFont(engine, playerID, 0, y2, strTemp, COLOR.WHITE)
				} else
					owner.receiver.drawDirectFont(x+4, y+168, strTemp, COLOR.WHITE, .5f)
			}// Games count
		// Line clear event
	}

	/* Render results screen */
	override fun renderResult(engine:GameEngine, playerID:Int) {
		super.renderResult(engine, playerID)

		var scale = 1f
		if(engine.displaysize==-1) scale = .5f

		drawResultScale(engine, playerID, owner.receiver, 2, COLOR.ORANGE, scale, "ATTACK", String.format("%10g",
			garbageSent[playerID].toFloat()/GARBAGE_DENOMINATOR), "LINE", String.format("%10d", engine.statistics.lines), "PIECE", String.format("%10d", engine.statistics.totalPieceLocked), "ATK/LINE", String.format("%10g", playerAPL[playerID]), "ATTACK/MIN", String.format("%10g", playerAPM[playerID]), "LINE/MIN", String.format("%10g", engine.statistics.lpm), "PIECE/SEC", String.format("%10g", engine.statistics.pps), "Time", String.format("%10s", GeneralUtil.getTime(engine.statistics.time)))
	}

	/* Send stats */
	override fun netSendStats(engine:GameEngine) {
		if(engine.playerID==0&&!netvsIsPractice&&!netvsIsWatch())
			netLobby!!.netPlayerClient!!.send("game\tstats\t"
				+garbage[engine.playerID]+"\n")
	}

	/* Receive stats */
	override fun netRecvStats(engine:GameEngine, message:Array<String>) {
		if(message.size>4) garbage[engine.playerID] = Integer.parseInt(message[4])
	}

	/* Send end-of-game stats */
	override fun netSendEndGameStats(engine:GameEngine) {
		val playerID = engine.playerID
		var msg = "gstat\t"
		msg += "${netvsPlayerPlace[playerID]}\t"
		msg += (garbageSent[playerID].toFloat()/GARBAGE_DENOMINATOR).toString()+"\t${playerAPL[playerID]}\t${playerAPM[playerID]}\t"
		msg += "${engine.statistics.lines}\t${engine.statistics.lpm}\t"
		msg += engine.statistics.totalPieceLocked.toString()+"\t${engine.statistics.pps}\t"
		msg += "$netvsPlayTimer${"\t$currentKO\t"+netvsPlayerWinCount[playerID]}\t"+netvsPlayerPlayCount[playerID]
		msg += "\n"
		netLobby!!.netPlayerClient!!.send(msg)
	}

	/* Receive end-of-game stats */
	override fun netvsRecvEndGameStats(message:Array<String>) {
		val seatID = Integer.parseInt(message[2])
		val playerID = netvsGetPlayerIDbySeatID(seatID)

		if(playerID!=0||netvsIsWatch()) {
			val engine = owner.engine[playerID]

			val tempGarbageSend = java.lang.Float.parseFloat(message[5])
			garbageSent[playerID] = (tempGarbageSend*GARBAGE_DENOMINATOR).toInt()

			playerAPL[playerID] = java.lang.Float.parseFloat(message[6])
			playerAPM[playerID] = java.lang.Float.parseFloat(message[7])
			engine.statistics.lines = Integer.parseInt(message[8])
			//engine.statistics.lpm = java.lang.Float.parseFloat(message[9])
			engine.statistics.totalPieceLocked = Integer.parseInt(message[10])
			//engine.statistics.pps = java.lang.Float.parseFloat(message[11])
			engine.statistics.time = Integer.parseInt(message[12])

			netvsPlayerResultReceived[playerID] = true
		}
	}

	/* Message received */
	@Throws(IOException::class)
	override fun netlobbyOnMessage(lobby:NetLobbyFrame, client:NetPlayerClient, message:Array<String>) {
		super.netlobbyOnMessage(lobby, client, message)

		// Dead
		if(message[0]=="dead") {
			val seatID = Integer.parseInt(message[3])
			val playerID = netvsGetPlayerIDbySeatID(seatID)
			var koUID = -1
			if(message.size>5) koUID = Integer.parseInt(message[5])

			// Increase KO count
			if(koUID==netLobby!!.netPlayerClient!!.playerUID) {
				playerKObyYou[playerID] = true
				currentKO++
			}
		}
		// Game messages
		if(message[0]=="game") {
			val uid = Integer.parseInt(message[1])
			val seatID = Integer.parseInt(message[2])
			val playerID = netvsGetPlayerIDbySeatID(seatID)
			//GameEngine engine = owner.engine[playerID];

			// Attack
			if(message[3]=="attack") {
				val pts = IntArray(ATTACK_CATEGORIES)
				var sumPts = 0

				for(i in 0 until ATTACK_CATEGORIES) {
					pts[i] = Integer.parseInt(message[4+i])
					sumPts += pts[i]
				}

				lastevent[playerID] = Integer.parseInt(message[ATTACK_CATEGORIES+5])
				lastb2b[playerID] = java.lang.Boolean.parseBoolean(message[ATTACK_CATEGORIES+6])
				lastcombo[playerID] = Integer.parseInt(message[ATTACK_CATEGORIES+7])
				garbage[playerID] = Integer.parseInt(message[ATTACK_CATEGORIES+8])
				lastpiece[playerID] = Integer.parseInt(message[ATTACK_CATEGORIES+9])
				scgettime[playerID] = 0
				val targetSeatID = Integer.parseInt(message[ATTACK_CATEGORIES+10])

				if(!netvsIsWatch()&&owner.engine[0].timerActive&&sumPts>0&&!netvsIsPractice&&!netvsIsNewcomer&&
					(targetSeatID==-1||netvsPlayerSeatID[0]==targetSeatID||!netCurrentRoomInfo!!.isTarget)&&
					netvsIsAttackable(playerID)) {
					var secondAdd = 0 //TODO: Allow for chunking of attack types other than b2b.
					if(netCurrentRoomInfo!!.b2bChunk) secondAdd = pts[ATTACK_CATEGORY_B2B]

					var garbageEntry = GarbageEntry(sumPts-secondAdd, playerID, uid)
					garbageEntries.add(garbageEntry)

					if(secondAdd>0) {
						garbageEntry = GarbageEntry(secondAdd, playerID, uid)
						garbageEntries.add(garbageEntry)
					}

					garbage[0] = totalGarbageLines
					if(garbage[0]>=4*GARBAGE_DENOMINATOR) owner.engine[0].loopSE("danger")
					netSendStats(owner.engine[0])
				}
			}
			// HurryUp
			if(message[3]=="hurryup")
				if(!hurryupStarted&&netCurrentRoomInfo!=null
					&&netCurrentRoomInfo!!.hurryupSeconds>0) {
					if(!netvsIsWatch()&&!netvsIsPractice&&owner.engine[0].timerActive) owner.receiver.playSE("hurryup")
					hurryupStarted = true
					hurryupShowFrames = 60*5
				}
		}
	}

	/** Garbage data */
	private inner class GarbageEntry {
		/** Number of garbage lines */
		var lines:Int = 0

		/** Sender's playerID */
		var playerID:Int = 0

		/** Sender's UID */
		var uid = 0

		/** Constructor
		 * @param g Lines
		 * @param p Sender's playerID
		 */
		constructor(g:Int, p:Int) {
			lines = g
			playerID = p
		}

		/** Constructor
		 * @param g Lines
		 * @param p Sender's playerID
		 * @param s Sender's UID
		 */
		constructor(g:Int, p:Int, s:Int) {
			lines = g
			playerID = p
			uid = s
		}
	}

	companion object {
		/** Most recent scoring event type constants */
		private const val EVENT_NONE = 0
		private const val EVENT_SINGLE = 1
		private const val EVENT_DOUBLE = 2
		private const val EVENT_TRIPLE = 3
		private const val EVENT_FOUR = 4
		private const val EVENT_TWIST_SINGLE_MINI = 5
		private const val EVENT_TWIST_SINGLE = 6
		private const val EVENT_TWIST_DOUBLE = 7
		private const val EVENT_TWIST_TRIPLE = 8
		private const val EVENT_TWIST_DOUBLE_MINI = 9
		private const val EVENT_TWIST_EZ = 10

		/** Type of attack performed */
		private const val ATTACK_CATEGORY_NORMAL = 0
		private const val ATTACK_CATEGORY_B2B = 1
		private const val ATTACK_CATEGORY_SPIN = 2
		private const val ATTACK_CATEGORY_COMBO = 3
		private const val ATTACK_CATEGORY_BRAVO = 4
		private const val ATTACK_CATEGORY_GEM = 5
		private const val ATTACK_CATEGORIES = 6

		/** Attack table (for Twister only) */
		private val LINE_ATTACK_TABLE = arrayOf(
			// 1-2P, 3P, 4P, 5P, 6P
			intArrayOf(0, 0, 0, 0, 0), // Single
			intArrayOf(1, 1, 0, 0, 0), // Double
			intArrayOf(2, 2, 1, 1, 1), // Triple
			intArrayOf(4, 3, 2, 2, 2), // Four
			intArrayOf(1, 1, 0, 0, 0), // T-Mini-S
			intArrayOf(2, 2, 1, 1, 1), // T-Single
			intArrayOf(4, 3, 2, 2, 2), // T-Double
			intArrayOf(6, 4, 3, 3, 3), // T-Triple
			intArrayOf(4, 3, 2, 2, 2), // T-Mini-D
			intArrayOf(1, 1, 0, 0, 0))// EZ-T

		/** Attack table(for All Spin) */
		private val LINE_ATTACK_TABLE_ALLSPIN = arrayOf(
			// 1-2P, 3P, 4P, 5P, 6P
			intArrayOf(0, 0, 0, 0, 0), // Single
			intArrayOf(1, 1, 0, 0, 0), // Double
			intArrayOf(2, 2, 1, 1, 1), // Triple
			intArrayOf(4, 3, 2, 2, 2), // Four
			intArrayOf(0, 0, 0, 0, 0), // T-Mini-S
			intArrayOf(2, 2, 1, 1, 1), // T-Single
			intArrayOf(4, 3, 2, 2, 2), // T-Double
			intArrayOf(6, 4, 3, 3, 3), // T-Triple
			intArrayOf(3, 2, 1, 1, 1), // T-Mini-D
			intArrayOf(0, 0, 0, 0, 0))// EZ-T

		/** Indexes of attack types in attack table */
		private const val LINE_ATTACK_INDEX_SINGLE = 0
		private const val LINE_ATTACK_INDEX_DOUBLE = 1
		private const val LINE_ATTACK_INDEX_TRIPLE = 2
		private const val LINE_ATTACK_INDEX_FOUR = 3
		private const val LINE_ATTACK_INDEX_TMINI = 4
		private const val LINE_ATTACK_INDEX_TSINGLE = 5
		private const val LINE_ATTACK_INDEX_TDOUBLE = 6
		private const val LINE_ATTACK_INDEX_TTRIPLE = 7
		private const val LINE_ATTACK_INDEX_TMINI_D = 8
		private const val LINE_ATTACK_INDEX_EZ_T = 9

		/** Combo attack table */
		private val COMBO_ATTACK_TABLE = arrayOf(intArrayOf(0, 0, 1, 1, 2, 2, 3, 3, 4, 4, 4, 5), // 1-2 Player(s)
			intArrayOf(0, 0, 1, 1, 1, 2, 2, 3, 3, 4, 4, 4), // 3 Player
			intArrayOf(0, 0, 0, 1, 1, 1, 2, 2, 3, 3, 4, 4), // 4 Player
			intArrayOf(0, 0, 0, 1, 1, 1, 1, 2, 2, 3, 3, 4), // 5 Player
			intArrayOf(0, 0, 0, 0, 1, 1, 1, 1, 2, 2, 3, 3))// 6 Payers

		/** Garbage denominator (can be divided by 2,3,4,5) */
		private const val GARBAGE_DENOMINATOR = 60
	}
}
