/*
 * Copyright (c) 2010-2022, NullNoname
 * Kotlin converted and modified by Venom=Nhelv.
 * THIS WAS NOT MADE IN ASSOCIATION WITH THE GAME CREATOR.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of NullNoname nor the names of its
 *       contributors may be used to endorse or promote products derived from
 *       this software without specific prior written permission.
 *
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
 * POSSIBILITY OF SUCH DAMAGE.
 */
package mu.nu.nullpo.game.subsystem.mode

import mu.nu.nullpo.game.component.Block
import mu.nu.nullpo.game.component.Controller
import mu.nu.nullpo.game.component.Piece
import mu.nu.nullpo.game.event.EventReceiver.COLOR
import mu.nu.nullpo.game.net.NetPlayerClient
import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.game.play.GameManager
import mu.nu.nullpo.gui.net.NetLobbyFrame
import mu.nu.nullpo.util.GeneralUtil.toTimeStr
import java.io.IOException
import java.util.LinkedList
import java.util.Locale
import java.util.Objects
import kotlin.math.abs

/** NET-VS-BATTLE Mode */
class NetVSBattleMode:NetDummyVSMode() {

	/** Column number of hole in most recent garbage line */
	private var lastHole = -1

	/** True if Hurry Up has been started */
	private var hurryupStarted = false

	/** Number of frames left to show "HURRY UP!" text */
	private var hurryupShowFrames = 0

	/** Number of pieces placed after Hurry Up has started */
	private var hurryupCount = 0

	/** True if you KO'd player */
	private var playerKObyYou = BooleanArray(0)

	/** Your KO count */
	private var currentKO = 0

	/** Time to display the most recent increase in score */
	private var scgettime = IntArray(0)

	/** Most recent scoring event type */
	private var lastevent = IntArray(0)

	/** True if most recent scoring event was B2B */
	private var lastb2b = BooleanArray(0)

	/** Most recent scoring event Combo count */
	private var lastcombo = IntArray(0)

	/** Most recent scoring event piece type */
	private var lastpiece = IntArray(0)

	/** Count of garbage lines send */
	private var garbageSent = IntArray(0)

	/** Amount of garbage in garbage queue */
	private var garbage = IntArray(0)

	/** Recieved garbage entries */
	private var garbageEntries:LinkedList<GarbageEntry> = LinkedList()

	/** APL (Attack Per Line) */
	private var playerAPL = FloatArray(0)

	/** APM (Attack Per Minute) */
	private var playerAPM = FloatArray(0)

	/** Target ID (-1:All) */
	private var targetID = 0

	/** Target Timer */
	private var targetTimer = 0

	/* Mode name */
	override val name = "NET-VS-BATTLE"

	override val isVSMode:Boolean
		get() = true

	/** Get number of possible targets (number of opponents)
	 * @return Number of possible targets (number of opponents)
	 */
	private val numberOfPossibleTargets:Int
		get() {
			var count = 0
			for(i in 1 until players)
				if(netVSIsAttackable(i)) count++
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
		playerKObyYou = BooleanArray(NET_MAX_PLAYERS)
		scgettime = IntArray(NET_MAX_PLAYERS)
		lastevent = IntArray(NET_MAX_PLAYERS)
		lastb2b = BooleanArray(NET_MAX_PLAYERS)
		lastcombo = IntArray(NET_MAX_PLAYERS)
		lastpiece = IntArray(NET_MAX_PLAYERS)
		garbageSent = IntArray(NET_MAX_PLAYERS)
		garbage = IntArray(NET_MAX_PLAYERS)
		playerAPL = FloatArray(NET_MAX_PLAYERS)
		playerAPM = FloatArray(NET_MAX_PLAYERS)
	}

	/** Set new target */
	private fun setNewTarget() {
		if(numberOfPossibleTargets>=1&&netCurrentRoomInfo!=null&&netCurrentRoomInfo!!.isTarget&&
			!netVSIsWatch()&&!netVSIsPractice)
			do {
				targetID++
				if(targetID>=players) targetID = 1
			} while(!netVSIsAttackable(targetID))
		else
			targetID = -1
	}

	/* Initialization for each player */
	override fun playerInit(engine:GameEngine) {
		super.playerInit(engine)
		val pid = engine.playerID
		if(pid==0&&!netVSIsWatch()) {
			lastHole = -1
			hurryupCount = 0
			currentKO = 0
			targetID = -1
			targetTimer = 0

			garbageEntries.clear()
		}

		playerKObyYou[pid] = false
		scgettime[pid] = 0
		lastevent[pid] = EVENT_NONE
		lastb2b[pid] = false
		lastcombo[pid] = 0
		lastpiece[pid] = 0
		garbageSent[pid] = 0
		garbage[pid] = 0
		playerAPL[pid] = 0f
		playerAPM[pid] = 0f
	}

	/* Executed after Ready->Go, before the first piece appears. */
	override fun startGame(engine:GameEngine) {
		super.startGame(engine)

		if(engine.playerID==0&&!netVSIsWatch()) {
			if(!netVSIsPractice) {
				hurryupStarted = false
				hurryupShowFrames = 0
			}
			setNewTarget()
			targetTimer = 0
		}
	}

	/* Calculate Score */
	override fun calcScore(engine:GameEngine, lines:Int):Int {
		// Attack
		val pid = engine.playerID
		if(lines>0&&pid==0) {
			val pts = IntArray(ATTACK_CATEGORIES)

			scgettime[pid] = 0

			val numAliveTeams = netVSGetNumberOfTeamsAlive()
			var attackNumPlayerIndex = numAliveTeams-2
			if(netVSIsPractice||!netCurrentRoomInfo!!.reduceLineSend) attackNumPlayerIndex = 0
			if(attackNumPlayerIndex<0) attackNumPlayerIndex = 0
			if(attackNumPlayerIndex>4) attackNumPlayerIndex = 4

			var attackLineIndex = LINE_ATTACK_INDEX_SINGLE
			var mainAttackCategory = ATTACK_CATEGORY_NORMAL

			// Twister style attack
			if(engine.twist) {
				mainAttackCategory = ATTACK_CATEGORY_SPIN

				// EZ-T
				if(engine.twistEZ) {
					attackLineIndex = LINE_ATTACK_INDEX_EZ_T
					lastevent[pid] = EVENT_TWIST_EZ
				} else if(lines==1) {
					if(engine.twistMini) {
						attackLineIndex = LINE_ATTACK_INDEX_TMINI
						lastevent[pid] = EVENT_TWIST_SINGLE_MINI
					} else {
						attackLineIndex = LINE_ATTACK_INDEX_TSINGLE
						lastevent[pid] = EVENT_TWIST_SINGLE
					}
				} else if(lines==2) {
					if(engine.twistMini&&engine.useAllSpinBonus) {
						attackLineIndex = LINE_ATTACK_INDEX_TMINI_D
						lastevent[pid] = EVENT_TWIST_DOUBLE_MINI
					} else {
						attackLineIndex = LINE_ATTACK_INDEX_TDOUBLE
						lastevent[pid] = EVENT_TWIST_DOUBLE
					}
				} else if(lines>=3) {
					attackLineIndex = LINE_ATTACK_INDEX_TTRIPLE
					lastevent[pid] = EVENT_TWIST_TRIPLE
				}// Twister 3 lines
				// Twister 2 lines
				// Twister 1 line
			} else // Single
				if(lines==1) {
					attackLineIndex = LINE_ATTACK_INDEX_SINGLE
					lastevent[pid] = EVENT_SINGLE
				} else if(lines==2) {
					attackLineIndex = LINE_ATTACK_INDEX_DOUBLE
					lastevent[pid] = EVENT_DOUBLE
				} else if(lines==3) {
					attackLineIndex = LINE_ATTACK_INDEX_TRIPLE
					lastevent[pid] = EVENT_TRIPLE
				} else if(lines>=4) {
					attackLineIndex = LINE_ATTACK_INDEX_FOUR
					lastevent[pid] = EVENT_FOUR
				}// Four
			// Triple
			// Double

			if(engine.useAllSpinBonus)
				pts[mainAttackCategory] += LINE_ATTACK_TABLE_ALLSPIN[attackLineIndex][attackNumPlayerIndex]
			else
				pts[mainAttackCategory] += LINE_ATTACK_TABLE[attackLineIndex][attackNumPlayerIndex]

			// B2B
			if(engine.b2b) {
				lastb2b[pid] = true

				if(pts[mainAttackCategory]>0)
					pts[ATTACK_CATEGORY_B2B] += if(attackLineIndex==LINE_ATTACK_INDEX_TTRIPLE&&!engine.useAllSpinBonus)
						2 else 1
			} else lastb2b[pid] = false

			// Combo
			if(engine.comboType!=GameEngine.COMBO_TYPE_DISABLE) {
				val cmbindex = maxOf(0, minOf(engine.combo, COMBO_ATTACK_TABLE[attackNumPlayerIndex].size-1))
				pts[ATTACK_CATEGORY_COMBO] += COMBO_ATTACK_TABLE[attackNumPlayerIndex][cmbindex]
				lastcombo[pid] = engine.combo
			}

			// All clear (Bravo)
			if(lines>=1&&engine.field.isEmpty&&netCurrentRoomInfo!!.bravo)
				pts[ATTACK_CATEGORY_BRAVO] += 6

			// Gem block attack
			pts[ATTACK_CATEGORY_GEM] += engine.field.howManyGemClears

			lastpiece[pid] = engine.nowPieceObject!!.id

			for(i in pts.indices)
				pts[i] *= GARBAGE_DENOMINATOR
			if(netCurrentRoomInfo!!.useFractionalGarbage&&!netVSIsPractice)
				if(numAliveTeams>=3)
					for(i in pts.indices)
						pts[i] = pts[i]/(numAliveTeams-1)

			// Attack lines count
			for(i in pts)
				garbageSent[pid] += i

			// Garbage countering
			garbage[pid] = totalGarbageLines
			for(i in pts.indices)
				if(pts[i]>0&&garbage[pid]>0
					&&netCurrentRoomInfo!!.counter)
					while((!netCurrentRoomInfo!!.useFractionalGarbage&&!garbageEntries.isEmpty()&&pts[i]>0)||
						netCurrentRoomInfo!!.useFractionalGarbage&&!garbageEntries.isEmpty()&&pts[i]>=GARBAGE_DENOMINATOR) {
						val garbageEntry = garbageEntries.first
						garbageEntry.lines -= pts[i]

						if(garbageEntry.lines<=0) {
							pts[i] = abs(garbageEntry.lines)
							garbageEntries.removeFirst()
						} else pts[i] = 0
					}

			// Send garbage lines
			if(!netVSIsPractice) {
				garbage[pid] = totalGarbageLines

				val stringPts = StringBuilder()
				for(i in pts)
					stringPts.append(i).append("\t")

				if(targetID!=-1&&!netVSIsAttackable(targetID)) setNewTarget()
				val targetSeatID = if(targetID==-1) -1 else netVSPlayerSeatID[targetID]

				netLobby!!.netPlayerClient!!.send(
					"game\tattack\t$stringPts\t${lastevent[pid]}\t"+lastb2b[pid]
						+"\t"+
						lastcombo[pid]+"\t${garbage[pid]}\t${lastpiece[pid]}\t$targetSeatID\n"
				)
			}
		}

		// Garbage lines appear
		if((lines==0||!netCurrentRoomInfo!!.rensaBlock)&&totalGarbageLines>=GARBAGE_DENOMINATOR&&!netVSIsPractice) {
			engine.playSE("garbage${if(totalGarbageLines-GARBAGE_DENOMINATOR>3) 1 else 0}")

			var smallGarbageCount = 0
			var hole = lastHole
			var newHole:Int
			if(hole==-1) hole = engine.random.nextInt(engine.field.width)

			var finalGarbagePercent = netCurrentRoomInfo!!.messiness
			if(netCurrentRoomInfo!!.divideChangeRateByPlayers) finalGarbagePercent /= netVSGetNumberOfTeamsAlive()-1

			// Make regular garbage lines appear
			while(!garbageEntries.isEmpty()) {
				val garbageEntry = garbageEntries.poll()
				smallGarbageCount += garbageEntry.lines%GARBAGE_DENOMINATOR

				if(garbageEntry.lines/GARBAGE_DENOMINATOR>0) {
					val seatFrom = netVSPlayerSeatID[garbageEntry.playerID]
					val garbageColor = if(seatFrom<0) Block.COLOR.WHITE else NET_PLAYER_COLOR_BLOCK[seatFrom]
					netVSLastAttackerUID = garbageEntry.uid
					if(netCurrentRoomInfo!!.garbageChangePerAttack) {
						if(engine.random.nextInt(100)<finalGarbagePercent) {
							newHole = engine.random.nextInt(engine.field.width-1)
							if(newHole>=hole) newHole++
							hole = newHole
						}
						engine.field.addSingleHoleGarbage(hole, garbageColor, engine.skin, garbageEntry.lines/GARBAGE_DENOMINATOR)
					} else
						for(i in garbageEntry.lines/GARBAGE_DENOMINATOR downTo 1) {
							if(engine.random.nextInt(100)<finalGarbagePercent) {
								newHole = engine.random.nextInt(engine.field.width-1)
								if(newHole>=hole) newHole++
								hole = newHole
							}

							engine.field.addSingleHoleGarbage(hole, garbageColor, engine.skin, 1)
						}
				}
			}

			// Make small garbage lines appear
			if(smallGarbageCount>0) {
				if(smallGarbageCount/GARBAGE_DENOMINATOR>0) {
					netVSLastAttackerUID = -1

					if(netCurrentRoomInfo!!.garbageChangePerAttack) {
						if(engine.random.nextInt(100)<finalGarbagePercent) {
							newHole = engine.random.nextInt(engine.field.width-1)
							if(newHole>=hole) newHole++
							hole = newHole
						}
						engine.field.addSingleHoleGarbage(
							hole, Block.COLOR.WHITE, engine.skin,
							smallGarbageCount/GARBAGE_DENOMINATOR
						)
					} else
						for(i in smallGarbageCount/GARBAGE_DENOMINATOR downTo 1) {
							if(engine.random.nextInt(100)<finalGarbagePercent) {
								newHole = engine.random.nextInt(engine.field.width-1)
								if(newHole>=hole) newHole++
								hole = newHole
							}

							engine.field.addSingleHoleGarbage(hole, Block.COLOR.WHITE, engine.skin, 1)
						}
				}

				if(smallGarbageCount%GARBAGE_DENOMINATOR>0) {
					val smallGarbageEntry = GarbageEntry(smallGarbageCount%GARBAGE_DENOMINATOR, -1)
					garbageEntries.add(smallGarbageEntry)
				}
			}

			lastHole = hole
			garbage[pid] = totalGarbageLines
		}

		// HURRY UP!
		if(netCurrentRoomInfo!!.hurryupSeconds>=0&&engine.timerActive&&!netVSIsPractice)
			if(hurryupStarted) {
				hurryupCount++

				if(hurryupCount%netCurrentRoomInfo!!.hurryupInterval==0) engine.field.addHurryupFloor(1, engine.skin)
			} else
				hurryupCount = netCurrentRoomInfo!!.hurryupInterval-1
		return 0
	}

	/* Executed at the end of each frame */
	override fun onLast(engine:GameEngine) {
		val pid = engine.playerID
		super.onLast(engine)

		scgettime[pid]++
		if(pid==0&&hurryupShowFrames>0) hurryupShowFrames--

		// HURRY UP!
		if(pid==0&&engine.timerActive&&netCurrentRoomInfo!=null&&netCurrentRoomInfo!!.hurryupSeconds>=0&&
			netVSPlayTimer==netCurrentRoomInfo!!.hurryupSeconds*60&&!hurryupStarted) {
			if(!netVSIsWatch()&&!netVSIsPractice) {
				netLobby!!.netPlayerClient!!.send("game\thurryup\n")
				engine.playSE("hurryup")
			}
			hurryupStarted = true
			hurryupShowFrames = 60*5
		}

		// Garbage meter
		val tempGarbage = garbage[pid]/GARBAGE_DENOMINATOR
		val tempGarbageF = garbage[pid].toFloat()/GARBAGE_DENOMINATOR
		if(pid==0&&!netVSIsWatch()) {
			if(tempGarbageF>engine.meterValue) {
				engine.meterValue += 1f/engine.fieldHeight/8
				if(engine.meterValue>tempGarbageF) engine.meterValue = tempGarbageF
			} else if(tempGarbageF<engine.meterValue) engine.meterValue--
		} else engine.meterValue = tempGarbageF
		engine.meterColor = when {
			tempGarbage>=4 -> GameEngine.METER_COLOR_RED
			tempGarbage>=3 -> GameEngine.METER_COLOR_ORANGE
			tempGarbage>=1 -> GameEngine.METER_COLOR_YELLOW
			else -> GameEngine.METER_COLOR_GREEN
		}

		// APL & APM
		if(pid==0&&engine.gameActive&&engine.timerActive&&!netVSIsWatch()) {
			val tempGarbageSent = garbageSent[pid].toFloat()/GARBAGE_DENOMINATOR
			playerAPM[0] = tempGarbageSent*3600/engine.statistics.time

			if(engine.statistics.lines>0)
				playerAPL[0] = tempGarbageSent/engine.statistics.lines
			else
				playerAPL[0] = 0f
		}

		// Target
		if(pid==0&&!netVSIsWatch()&&netVSPlayTimerActive&&engine.gameActive&&engine.timerActive&&
			numberOfPossibleTargets>=1&&netCurrentRoomInfo!=null&&netCurrentRoomInfo!!.isTarget) {
			targetTimer++

			if(targetTimer>=netCurrentRoomInfo!!.targetTimer||!netVSIsAttackable(targetID)) {
				targetTimer = 0
				setNewTarget()
			}
		}
	}

	/* Drawing processing at the end of every frame */
	override fun renderLast(engine:GameEngine) {
		super.renderLast(engine)

		val x = owner.receiver.fieldX(engine)
		val y = owner.receiver.fieldY(engine)

		val pid = engine.playerID
		if(netVSPlayerExist[pid]&&engine.isVisible) {
			// Garbage Count
			if(garbage[pid]>0&&netCurrentRoomInfo!!.useFractionalGarbage&&engine.stat!=GameEngine.Status.RESULT) {
				val strTempGarbage:String

				var fontColor = COLOR.WHITE
				if(garbage[pid]>=GARBAGE_DENOMINATOR) fontColor = COLOR.YELLOW
				if(garbage[pid]>=GARBAGE_DENOMINATOR*3) fontColor = COLOR.ORANGE
				if(garbage[pid]>=GARBAGE_DENOMINATOR*4) fontColor = COLOR.RED

				if(engine.displaySize!=-1) {
					strTempGarbage = String.format(Locale.US, "%5.2f", garbage[pid].toFloat()/GARBAGE_DENOMINATOR)
					owner.receiver.drawDirectFont(x+96, y+372, strTempGarbage, fontColor, 1f)
				} else {
					strTempGarbage = String.format(Locale.US, "%4.1f", garbage[pid].toFloat()/GARBAGE_DENOMINATOR)
					owner.receiver.drawDirectFont(x+64, y+168, strTempGarbage, fontColor, .5f)
				}
			}

			// Target
			if(pid==targetID&&netCurrentRoomInfo!=null&&netCurrentRoomInfo!!.isTarget&&netVSNumAlivePlayers>=3&&
				netVSIsGameActive&&netVSIsAttackable(pid)&&!netVSIsWatch()) {
				val fontcolor = if(targetTimer>=netCurrentRoomInfo!!.targetTimer-20&&targetTimer%2==0)
					COLOR.WHITE else COLOR.GREEN

				owner.receiver.drawMenuFont(engine, 2, 12, "TARGET", fontcolor)
			}
		}

		// Practice mode
		if(pid==0&&netVSIsPractice&&netVSIsPracticeExitAllowed
			&&engine.stat!=GameEngine.Status.RESULT)
			if(lastevent[pid]==EVENT_NONE||scgettime[pid]>=120)
				owner.receiver.drawMenuFont(
					engine, 0, 21, "F("
						+owner.receiver.getKeyNameByButtonID(engine, Controller.BUTTON_F)
						+" KEY):\n END GAME", COLOR.PURPLE
				)

		// Hurry Up
		if(netCurrentRoomInfo!=null&&pid==0)
			if(netCurrentRoomInfo!!.hurryupSeconds>=0&&hurryupShowFrames>0
				&&!netVSIsPractice&&hurryupStarted)
				owner.receiver.drawDirectFont(pid, 256-8, 32, "HURRY UP!", hurryupShowFrames%2==0)

		// Bottom message
		if(netVSPlayerExist[pid]&&engine.isVisible)
		// K.O.
			if(playerKObyYou[pid]) {
				if(engine.displaySize!=-1)
					owner.receiver.drawMenuFont(engine, 3, 21, "K.O.", COLOR.PINK)
				else
					owner.receiver.drawDirectFont(x+4+24, y+168, "K.O.", COLOR.PINK, .5f)
			} else if(lastevent[pid]!=EVENT_NONE&&scgettime[pid]<120) {
				val strPieceName = Piece.Shape.names[lastpiece[pid]]

				if(engine.displaySize!=-1) {
					when(lastevent[pid]) {
						EVENT_SINGLE -> owner.receiver.drawMenuFont(engine, 2, 21, "SINGLE", COLOR.COBALT)
						EVENT_DOUBLE -> owner.receiver.drawMenuFont(engine, 2, 21, "DOUBLE", COLOR.BLUE)
						EVENT_TRIPLE -> owner.receiver.drawMenuFont(engine, 2, 21, "TRIPLE", COLOR.GREEN)
						EVENT_FOUR -> if(lastb2b[pid])
							owner.receiver.drawMenuFont(engine, 3, 21, "FOUR", COLOR.RED)
						else
							owner.receiver.drawMenuFont(engine, 3, 21, "FOUR", COLOR.ORANGE)
						EVENT_TWIST_SINGLE_MINI -> if(lastb2b[pid])
							owner.receiver.drawMenuFont(engine, 1, 21, "$strPieceName-MINI-S", COLOR.RED)
						else
							owner.receiver.drawMenuFont(engine, 1, 21, "$strPieceName-MINI-S", COLOR.ORANGE)
						EVENT_TWIST_SINGLE -> if(lastb2b[pid])
							owner.receiver.drawMenuFont(engine, 1, 21, "$strPieceName-SINGLE", COLOR.RED)
						else
							owner.receiver.drawMenuFont(engine, 1, 21, "$strPieceName-SINGLE", COLOR.ORANGE)
						EVENT_TWIST_DOUBLE_MINI -> if(lastb2b[pid])
							owner.receiver.drawMenuFont(engine, 1, 21, "$strPieceName-MINI-D", COLOR.RED)
						else
							owner.receiver.drawMenuFont(engine, 1, 21, "$strPieceName-MINI-D", COLOR.ORANGE)
						EVENT_TWIST_DOUBLE -> if(lastb2b[pid])
							owner.receiver.drawMenuFont(engine, 1, 21, "$strPieceName-DOUBLE", COLOR.RED)
						else
							owner.receiver.drawMenuFont(engine, 1, 21, "$strPieceName-DOUBLE", COLOR.ORANGE)
						EVENT_TWIST_TRIPLE -> if(lastb2b[pid])
							owner.receiver.drawMenuFont(engine, 1, 21, "$strPieceName-TRIPLE", COLOR.RED)
						else
							owner.receiver.drawMenuFont(engine, 1, 21, "$strPieceName-TRIPLE", COLOR.ORANGE)
						EVENT_TWIST_EZ -> if(lastb2b[pid])
							owner.receiver.drawMenuFont(engine, 3, 21, "EZ-$strPieceName", COLOR.RED)
						else
							owner.receiver.drawMenuFont(engine, 3, 21, "EZ-$strPieceName", COLOR.ORANGE)
					}

					if(lastcombo[pid]>=2)
						owner.receiver.drawMenuFont(engine, 2, 22, "${(lastcombo[pid]-1)}COMBO", COLOR.CYAN)
				} else {
					var x2 = 8
					if(Objects.requireNonNull(netCurrentRoomInfo)!!.useFractionalGarbage&&garbage[pid]>0) x2 = 0

					when(lastevent[pid]) {
						EVENT_SINGLE -> owner.receiver.drawDirectFont(x+4+16, y+168, "SINGLE", COLOR.COBALT, .5f)
						EVENT_DOUBLE -> owner.receiver.drawDirectFont(x+4+16, y+168, "DOUBLE", COLOR.BLUE, .5f)
						EVENT_TRIPLE -> owner.receiver.drawDirectFont(x+4+16, y+168, "TRIPLE", COLOR.GREEN, .5f)
						EVENT_FOUR -> if(lastb2b[pid])
							owner.receiver.drawDirectFont(x-4, y+168, "QUADRUPLE", COLOR.RED, .5f)
						else
							owner.receiver.drawDirectFont(x-4, y+168, "QUADRUPLE", COLOR.ORANGE, .5f)
						EVENT_TWIST_SINGLE_MINI -> if(lastb2b[pid])
							owner.receiver.drawDirectFont(x+4+x2, y+168, "$strPieceName-MINI-S", COLOR.RED, .5f)
						else
							owner.receiver.drawDirectFont(x+4+x2, y+168, "$strPieceName-MINI-S", COLOR.ORANGE, .5f)
						EVENT_TWIST_SINGLE -> if(lastb2b[pid])
							owner.receiver.drawDirectFont(x+4+x2, y+168, "$strPieceName-SINGLE", COLOR.RED, .5f)
						else
							owner.receiver.drawDirectFont(x+4+x2, y+168, "$strPieceName-SINGLE", COLOR.ORANGE, .5f)
						EVENT_TWIST_DOUBLE_MINI -> if(lastb2b[pid])
							owner.receiver.drawDirectFont(x+4+x2, y+168, "$strPieceName-MINI-D", COLOR.RED, .5f)
						else
							owner.receiver.drawDirectFont(x+4+x2, y+168, "$strPieceName-MINI-D", COLOR.ORANGE, .5f)
						EVENT_TWIST_DOUBLE -> if(lastb2b[pid])
							owner.receiver.drawDirectFont(x+4+x2, y+168, "$strPieceName-DOUBLE", COLOR.RED, .5f)
						else
							owner.receiver.drawDirectFont(x+4+x2, y+168, "$strPieceName-DOUBLE", COLOR.ORANGE, .5f)
						EVENT_TWIST_TRIPLE -> if(lastb2b[pid])
							owner.receiver.drawDirectFont(x+4+x2, y+168, "$strPieceName-TRIPLE", COLOR.RED, .5f)
						else
							owner.receiver.drawDirectFont(x+4+x2, y+168, "$strPieceName-TRIPLE", COLOR.ORANGE, .5f)
						EVENT_TWIST_EZ -> if(lastb2b[pid])
							owner.receiver.drawDirectFont(x+4+24, y+168, "EZ-$strPieceName", COLOR.RED, .5f)
						else
							owner.receiver.drawDirectFont(x+4+24, y+168, "EZ-$strPieceName", COLOR.ORANGE, .5f)
					}

					if(lastcombo[pid]>=2)
						owner.receiver.drawDirectFont(x+4+16, y+176, ("${(lastcombo[pid]-1)}COMBO"), COLOR.CYAN, .5f)
				}
			} else if(!netVSIsPractice||pid!=0) {
				val strTemp = "${netVSPlayerWinCount[pid]}/${netVSPlayerPlayCount[pid]}"

				if(engine.displaySize!=-1) {
					var y2 = 21
					if(engine.stat==GameEngine.Status.RESULT) y2 = 22
					owner.receiver.drawMenuFont(engine, 0, y2, strTemp, COLOR.WHITE)
				} else
					owner.receiver.drawDirectFont(x+4, y+168, strTemp, COLOR.WHITE, .5f)
			}// Games count
		// Line clear event
	}

	/* Render results screen */
	override fun renderResult(engine:GameEngine) {
		val pid = engine.playerID
		super.renderResult(engine)
		val scale = if(engine.displaySize==-1) .5f else 1f

		drawResultScale(
			engine, owner.receiver, 2, COLOR.ORANGE, scale,
			"ATTACK", String.format("%10g", garbageSent[pid].toFloat()/GARBAGE_DENOMINATOR),
			"LINE", String.format("%10d", engine.statistics.lines),
			"PIECE", String.format("%10d", engine.statistics.totalPieceLocked),
			"ATK/LINE", String.format("%10g", playerAPL[pid]),
			"ATTACK/MIN", String.format("%10g", playerAPM[pid]),
			"LINE/MIN", String.format("%10g", engine.statistics.lpm),
			"PIECE/SEC", String.format("%10g", engine.statistics.pps),
			"Time", String.format("%10s", engine.statistics.time.toTimeStr)
		)
	}

	/* Send stats */
	override fun netSendStats(engine:GameEngine) {
		if(engine.playerID==0&&!netVSIsPractice&&!netVSIsWatch())
			netLobby!!.netPlayerClient!!.send("game\tstats\tgarbage[engine.playerID]\n")
	}

	/* Receive stats */
	override fun netRecvStats(engine:GameEngine, message:Array<String>) {
		if(message.size>4) garbage[engine.playerID] = message[4].toInt()
	}

	/* Send end-of-game stats */
	override fun netSendEndGameStats(engine:GameEngine) {
		val playerID = engine.playerID
		val msg = "gstat\t"+
			"${netVSPlayerPlace[playerID]}\t"+
			"${(garbageSent[playerID].toFloat()/GARBAGE_DENOMINATOR)}\t${playerAPL[playerID]}\t${playerAPM[playerID]}\t"+
			"${engine.statistics.lines}\t${engine.statistics.lpm}\t"+
			"${engine.statistics.totalPieceLocked}\t${engine.statistics.pps}\t"+
			"$netVSPlayTimer${"\t$currentKO\t"+netVSPlayerWinCount[playerID]}\t"+netVSPlayerPlayCount[playerID]+
			"\n"
		netLobby!!.netPlayerClient!!.send(msg)
	}

	/* Receive end-of-game stats */
	override fun netVSRecvEndGameStats(message:Array<String>) {
		val seatID = message[2].toInt()
		val playerID = netVSGetPlayerIDbySeatID(seatID)

		if(playerID!=0||netVSIsWatch()) {
			val engine = owner.engine[playerID]

			val tempGarbageSend = message[5].toFloat()
			garbageSent[playerID] = (tempGarbageSend*GARBAGE_DENOMINATOR).toInt()

			playerAPL[playerID] = message[6].toFloat()
			playerAPM[playerID] = message[7].toFloat()
			engine.statistics.lines = message[8].toInt()
			//engine.statistics.lpm = message[9].toFloat()
			engine.statistics.totalPieceLocked = message[10].toInt()
			//engine.statistics.pps = message[11].toFloat()
			engine.statistics.time = message[12].toInt()

			netVSPlayerResultReceived[playerID] = true
		}
	}

	/* Message received */
	@Throws(IOException::class)
	override fun netlobbyOnMessage(lobby:NetLobbyFrame, client:NetPlayerClient, message:Array<String>) {
		super.netlobbyOnMessage(lobby, client, message)

		// Dead
		if(message[0]=="dead") {
			val seatID = message[3].toInt()
			val playerID = netVSGetPlayerIDbySeatID(seatID)
			var koUID = -1
			if(message.size>5) koUID = message[5].toInt()

			// Increase KO count
			if(koUID==netLobby!!.netPlayerClient!!.playerUID) {
				playerKObyYou[playerID] = true
				currentKO++
			}
		}
		// Game messages
		if(message[0]=="game") {
			val uid = message[1].toInt()
			val seatID = message[2].toInt()
			val playerID = netVSGetPlayerIDbySeatID(seatID)
			//GameEngine engine = owner.engine[playerID];

			// Attack
			if(message[3]=="attack") {
				val pts = IntArray(ATTACK_CATEGORIES)
				var sumPts = 0

				for(i in 0 until ATTACK_CATEGORIES) {
					pts[i] = message[4+i].toInt()
					sumPts += pts[i]
				}

				lastevent[playerID] = message[ATTACK_CATEGORIES+5].toInt()
				lastb2b[playerID] = message[ATTACK_CATEGORIES+6].toBoolean()
				lastcombo[playerID] = message[ATTACK_CATEGORIES+7].toInt()
				garbage[playerID] = message[ATTACK_CATEGORIES+8].toInt()
				lastpiece[playerID] = message[ATTACK_CATEGORIES+9].toInt()
				scgettime[playerID] = 0
				val targetSeatID = message[ATTACK_CATEGORIES+10].toInt()

				if(!netVSIsWatch()&&owner.engine[0].timerActive&&sumPts>0&&!netVSIsPractice&&!netVSIsNewcomer&&
					(targetSeatID==-1||netVSPlayerSeatID[0]==targetSeatID||!netCurrentRoomInfo!!.isTarget)&&
					netVSIsAttackable(playerID)) {
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
					if(!netVSIsWatch()&&!netVSIsPractice&&owner.engine[0].timerActive) owner.receiver.playSE("hurryup")
					hurryupStarted = true
					hurryupShowFrames = 60*5
				}
		}
	}

	/** Garbage data */
	private class GarbageEntry {
		/** Number of garbage lines */
		var lines = 0

		/** Sender's playerID */
		var playerID = 0

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
			intArrayOf(1, 1, 0, 0, 0)
		)// EZ-T

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
			intArrayOf(0, 0, 0, 0, 0)
		)// EZ-T

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
		private val COMBO_ATTACK_TABLE = arrayOf(
			intArrayOf(0, 0, 1, 1, 2, 2, 3, 3, 4, 4, 4, 5), // 1-2 Player(s)
			intArrayOf(0, 0, 1, 1, 1, 2, 2, 3, 3, 4, 4, 4), // 3 Player
			intArrayOf(0, 0, 0, 1, 1, 1, 2, 2, 3, 3, 4, 4), // 4 Player
			intArrayOf(0, 0, 0, 1, 1, 1, 1, 2, 2, 3, 3, 4), // 5 Player
			intArrayOf(0, 0, 0, 0, 1, 1, 1, 1, 2, 2, 3, 3)
		)// 6 Payers

		/** Garbage denominator (can be divided by 2,3,4,5) */
		private const val GARBAGE_DENOMINATOR = 60
	}
}
