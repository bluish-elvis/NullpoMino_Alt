/*
 Copyright (c) 2010-2024, NullNoname
 All rights reserved.

 Converted to Kotlin and modified by Venom_Nhelv as bluish-elvis
 THIS IS NOT MADE IN ASSOCIATION WITH THE GAME CREATOR.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions are met:
     * Redistributions of source code must retain the above copyright
       notice, this list of conditions and the following disclaimer.
     * Redistributions in binary form must reproduce the above copyright
       notice, this list of conditions and the following disclaimer in the
       documentation and/or other materials provided with the distribution.
     * Neither the name of NullNoname nor the names of its
       contributors may be used to endorse or promote products derived from
       this software without specific prior written permission.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 POSSIBILITY OF SUCH DAMAGE.
 */
package mu.nu.nullpo.game.subsystem.mode.another

import mu.nu.nullpo.game.component.BGMStatus.BGM
import mu.nu.nullpo.game.component.Controller
import mu.nu.nullpo.game.event.EventReceiver
import mu.nu.nullpo.game.event.EventReceiver.COLOR
import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.game.play.GameManager
import mu.nu.nullpo.gui.common.BaseFont
import mu.nu.nullpo.gui.common.GameKeyDummy.Companion.MAX_PLAYERS
import mu.nu.nullpo.util.CustomProperties
import mu.nu.nullpo.util.GeneralUtil.toTimeStr

/** AVALANCHE VS FEVER MARATHON mode (Release Candidate 1) */
class AvalancheVSFever:AvalancheVSDummyMode() {
	/** Version */
	private var version = 0

	/** Second ojama counter for Fever Mode */
	private var ojamaHandicapLeft = IntArray(0)

	/** Chain levels for Fever Mode */
	private var feverChain = IntArray(0)

	/** Ojama handicap to start with */
	private var ojamaHandicap = IntArray(0)

	/** Fever chain count when last chain hit occurred */
	private var feverChainDisplay = IntArray(0)

	/** Chain size for first fever setup */
	private var feverChainStart = IntArray(0)

	/* Mode name */
	override val name = "AVALANCHE VS FEVER MARATHON (RC1)"

	/* Mode initialization */
	override fun modeInit(manager:GameManager) {
		super.modeInit(manager)
		ojamaHandicapLeft = IntArray(MAX_PLAYERS)
		feverChain = IntArray(MAX_PLAYERS)
		ojamaHandicap = IntArray(MAX_PLAYERS)
		feverChainDisplay = IntArray(MAX_PLAYERS)
		feverChainStart = IntArray(MAX_PLAYERS)
	}

	/** Load settings into [engine] from [prop] not related to speeds */
	private fun loadOtherSetting(engine:GameEngine, prop:CustomProperties) {
		super.loadOtherSetting(engine, prop, "fever")
		val playerID = engine.playerID
		ojamaRate[playerID] = prop.getProperty("avalanchevsfever.ojamaRate.p$playerID", 120)
		ojamaHard[playerID] = prop.getProperty("avalanchevsfever.ojamaHard.p$playerID", 0)
		ojamaHandicap[playerID] = prop.getProperty("avalanchevsfever.ojamaHandicap.p$playerID", 270)
		feverChainStart[playerID] = prop.getProperty("avalanchevsfever.feverChainStart.p$playerID", 5)
	}

	/** Save settings from [engine] into [prop] not related to speeds */
	private fun saveOtherSetting(engine:GameEngine, prop:CustomProperties) {
		super.saveOtherSetting(engine, prop, "fever")
		val playerID = engine.playerID
		prop.setProperty("avalanchevsfever.ojamaHandicap.p$playerID", ojamaHandicap[playerID])
		prop.setProperty("avalanchevsfever.feverChainStart.p$playerID", feverChainStart[playerID])
	}

	/* Initialization for each player */
	override fun playerInit(engine:GameEngine) {
		super.playerInit(engine)
		val playerID = engine.playerID
		ojamaCounterMode[playerID] = OJAMA_COUNTER_FEVER

		ojama[playerID] = 0
		feverChainDisplay[playerID] = 0

		version = if(!engine.owner.replayMode) {
			loadOtherSetting(engine, engine.owner.modeConfig)
			loadPreset(engine, engine.owner.modeConfig, -1-playerID, "fever")
			CURRENT_VERSION
		} else {
			loadOtherSetting(engine, engine.owner.replayProp)
			loadPreset(engine, engine.owner.replayProp, -1-playerID, "fever")
			owner.replayProp.getProperty("avalanchevsfever.version", 0)
		}
	}

	/* Called at settings screen */
	override fun onSetting(engine:GameEngine):Boolean {
		val playerID = engine.playerID
		// Menu
		if(!engine.owner.replayMode&&engine.statc[4]==0) {
			// Configuration changes
			val change = updateCursor(engine, 29)

			if(change!=0) {
				engine.playSE("change")

				var m = 1
				if(engine.ctrl.isPress(Controller.BUTTON_E)) m = 100
				if(engine.ctrl.isPress(Controller.BUTTON_F)) m = 1000

				when(menuCursor) {
					0 -> engine.speed.gravity = rangeCursor(engine.speed.gravity+change*m, -1, 99999)
					1 -> engine.speed.denominator = rangeCursor(change*m, -1, 99999)
					2 -> engine.speed.are = rangeCursor(engine.speed.are+change, 0, 99)
					3 -> engine.speed.areLine = rangeCursor(engine.speed.areLine+change, 0, 99)
					4 -> engine.speed.lineDelay = rangeCursor(engine.speed.lineDelay+change, 0, 99)
					5 -> engine.speed.lockDelay = rangeCursor(engine.speed.lockDelay+change*minOf(m, 10), 0, 999)
					6 -> engine.speed.das = rangeCursor(engine.speed.das+change, 0, 99)
					7 -> {
						engine.cascadeDelay += change
						if(engine.cascadeDelay<0) engine.cascadeDelay = 20
						if(engine.cascadeDelay>20) engine.cascadeDelay = 0
					}
					8 -> {
						engine.cascadeClearDelay += change
						if(engine.cascadeClearDelay<0) engine.cascadeClearDelay = 99
						if(engine.cascadeClearDelay>99) engine.cascadeClearDelay = 0
					}
					9 -> {
						zenKeshiType[playerID] += change
						if(zenKeshiType[playerID]<0) zenKeshiType[playerID] = 2
						if(zenKeshiType[playerID]>2) zenKeshiType[playerID] = 0
					}
					10 -> {
						if(m>=10)
							maxAttack[playerID] += change*10
						else
							maxAttack[playerID] += change
						if(maxAttack[playerID]<0) maxAttack[playerID] = 99
						if(maxAttack[playerID]>99) maxAttack[playerID] = 0
					}
					11 -> {
						numColors[playerID] += change
						if(numColors[playerID]<3) numColors[playerID] = 5
						if(numColors[playerID]>5) numColors[playerID] = 3
					}
					12 -> {
						rensaShibari[playerID] += change
						if(rensaShibari[playerID]<1) rensaShibari[playerID] = 20
						if(rensaShibari[playerID]>20) rensaShibari[playerID] = 1
					}
					13 -> {
						if(m>=10)
							ojamaRate[playerID] += change*100
						else
							ojamaRate[playerID] += change*10
						if(ojamaRate[playerID]<10) ojamaRate[playerID] = 1000
						if(ojamaRate[playerID]>1000) ojamaRate[playerID] = 10
					}
					14 -> {
						if(m>10)
							hurryUpSeconds[playerID] += change*m/10
						else
							hurryUpSeconds[playerID] += change
						if(hurryUpSeconds[playerID]<0) hurryUpSeconds[playerID] = 300
						if(hurryUpSeconds[playerID]>300) hurryUpSeconds[playerID] = 0
					}
					15 -> {
						ojamaHard[playerID] += change
						if(ojamaHard[playerID]<0) ojamaHard[playerID] = 9
						if(ojamaHard[playerID]>9) ojamaHard[playerID] = 0
					}
					16 -> dangerColumnDouble[playerID] = !dangerColumnDouble[playerID]
					17 -> dangerColumnShowX[playerID] = !dangerColumnShowX[playerID]
					18 -> {
						ojamaHandicap[playerID] += change*m
						if(ojamaHandicap[playerID]<0) ojamaHandicap[playerID] = 9999
						if(ojamaHandicap[playerID]>9999) ojamaHandicap[playerID] = 0
					}
					19 -> {
						feverMapSet[playerID] += change
						if(feverMapSet[playerID]<0) feverMapSet[playerID] = FEVER_MAPS.size-1
						if(feverMapSet[playerID]>=FEVER_MAPS.size) feverMapSet[playerID] = 0
						loadMapSetFever(engine, feverMapSet[playerID], true)
						if(feverChainStart[playerID]<feverChainMin[playerID])
							feverChainStart[playerID] = feverChainMax[playerID]
						if(feverChainStart[playerID]>feverChainMax[playerID])
							feverChainStart[playerID] = feverChainMin[playerID]
					}
					20 -> {
						feverChainStart[playerID] += change
						if(feverChainStart[playerID]<feverChainMin[playerID])
							feverChainStart[playerID] = feverChainMax[playerID]
						if(feverChainStart[playerID]>feverChainMax[playerID])
							feverChainStart[playerID] = feverChainMin[playerID]
					}
					21 -> {
						outlineType[playerID] += change
						if(outlineType[playerID]<0) outlineType[playerID] = 2
						if(outlineType[playerID]>2) outlineType[playerID] = 0
					}
					22 -> {
						chainDisplayType[playerID] += change
						if(chainDisplayType[playerID]<0) chainDisplayType[playerID] = 4
						if(chainDisplayType[playerID]>4) chainDisplayType[playerID] = 0
					}
					23 -> cascadeSlow[playerID] = !cascadeSlow[playerID]
					24 -> newChainPower[playerID] = !newChainPower[playerID]
					25 -> bgmId = rangeCursor(bgmId+change, 0, BGM.count-1)
					26 -> enableSE[playerID] = !enableSE[playerID]
					27 -> bigDisplay = !bigDisplay
					28, 29 -> presetNumber[playerID] = rangeCursor(presetNumber[playerID]+change, 0, 99)
				}
			}

			// 決定
			if(menuTime<5) menuTime++ else if(engine.ctrl.isPush(Controller.BUTTON_A)) {
				engine.playSE("decide")

				when(menuCursor) {
					28 -> loadPreset(engine, owner.modeConfig, presetNumber[playerID], "fever")
					29 -> {
						savePreset(engine, owner.modeConfig, presetNumber[playerID], "fever")
						owner.saveModeConfig()
					}
					else -> {
						saveOtherSetting(engine, owner.modeConfig)
						savePreset(engine, owner.modeConfig, -1-playerID, "fever")
						owner.saveModeConfig()
						engine.statc[4] = 1
					}
				}
			}

			// Cancel
			if(engine.ctrl.isPush(Controller.BUTTON_B)) engine.quitFlag = true
		} else if(engine.statc[4]==0) {
			menuTime++
			menuCursor = 0

			if(menuTime>=240)
				engine.statc[4] = 1
			else if(menuTime>=180)
				menuCursor = 24
			else if(menuTime>=120)
				menuCursor = 18
			else if(menuTime>=60) menuCursor = 9
		} else // Start
			if(owner.engine[0].statc[4]==1&&owner.engine[1].statc[4]==1&&playerID==1) {
				owner.engine[0].stat = GameEngine.Status.READY
				owner.engine[1].stat = GameEngine.Status.READY
				owner.engine[0].resetStatc()
				owner.engine[1].resetStatc()
			} else if(engine.ctrl.isPush(Controller.BUTTON_B)) engine.statc[4] = 0// Cancel

		return true
	}

	/* Setting screen drawing */
	override fun renderSetting(engine:GameEngine) {
		if(engine.statc[4]==0) {
			val pid = engine.playerID
			when {
				menuCursor<9 -> {
					drawMenuSpeeds(engine, receiver, 0, COLOR.ORANGE, 0)
					drawMenu(engine, receiver, "FALL DELAY" to engine.cascadeDelay, "CLEAR DELAY" to engine.cascadeClearDelay)

					receiver.drawMenuFont(engine, 0, 19, "${BaseFont.UP_S}${BaseFont.DOWN_S} PAGE 1/4", COLOR.YELLOW)
				}
				menuCursor<18 -> {
					drawMenu(
						engine, receiver, 0, COLOR.CYAN, 9,
						"ZENKESHI" to ZENKESHI_TYPE_NAMES[zenKeshiType[pid]],
						"MAX ATTACK" to maxAttack[pid],
						"COLORS" to numColors[pid],
						"MIN CHAIN" to "${rensaShibari[pid]}",
						"OJAMA RATE" to ojamaRate[pid],
						"HURRYUP" to if(hurryUpSeconds[pid]==0) "NONE" else "${hurryUpSeconds[pid]} SEC",
						"HARD OJAMA" to ojamaHard[pid],
						"X COLUMN" to if(dangerColumnDouble[pid]) "3 AND 4" else "3 ONLY",
						"X SHOW" to dangerColumnShowX[pid]
					)

					receiver.drawMenuFont(engine, 0, 19, "${BaseFont.UP_S}${BaseFont.DOWN_S} PAGE 2/4", COLOR.YELLOW)
				}
				menuCursor<25 -> {
					drawMenu(
						engine, receiver, 0, COLOR.PURPLE, 18,
						"HANDICAP" to ojamaHandicap[pid],
						"F-MAP SET" to FEVER_MAPS[feverMapSet[pid]].uppercase(),
						"STARTCHAIN" to feverChainStart[pid]
					)

					drawMenu(
						engine, receiver, COLOR.COBALT,
						"OUTLINE" to OUTLINE_TYPE_NAMES[outlineType[pid]],
						"SHOW CHAIN" to if(chainDisplayType[pid]==CHAIN_DISPLAY_FEVERSIZE) "FEVERSIZE" else CHAIN_DISPLAY_NAMES[chainDisplayType[pid]],
						"FALL ANIM" to if(cascadeSlow[pid]) "FEVER" else "CLASSIC"
					)

					drawMenu(engine, receiver, COLOR.CYAN, "CHAINPOWER" to if(newChainPower[pid]) "FEVER" else "CLASSIC")

					receiver.drawMenuFont(engine, 0, 19, "${BaseFont.UP_S}${BaseFont.DOWN_S} PAGE 3/4", COLOR.YELLOW)
				}
				else -> {
					drawMenu(engine, receiver, 0, COLOR.PINK, 25, "BGM" to BGM.values[bgmId])
					drawMenu(engine, receiver, COLOR.YELLOW, "SE" to enableSE[pid])
					drawMenu(engine, receiver, COLOR.PINK, "BIG DISP" to bigDisplay)
					drawMenu(engine, receiver, COLOR.GREEN, "LOAD" to presetNumber[pid], "SAVE" to presetNumber[pid])

					receiver.drawMenuFont(engine, 0, 19, "${BaseFont.UP_S}${BaseFont.DOWN_S} PAGE 4/4", COLOR.YELLOW)
				}
			}
		} else receiver.drawMenuFont(engine, 3, 10, "WAIT", COLOR.YELLOW)
	}

	/* Called for initialization during Ready (before initialization) */
	override fun readyInit(engine:GameEngine):Boolean {
		super.readyInit(engine)
		val pid = engine.playerID
		ojamaHandicapLeft[pid] = ojamaHandicap[pid]
		feverChain[pid] = feverChainStart[pid]
		engine.field.reset()
		loadMapSetFever(engine, feverMapSet[pid], true)
		return false
	}

	/* Called at game start */
	override fun startGame(engine:GameEngine) {
		super.startGame(engine)
		loadFeverMap(engine, feverChain[engine.playerID])
	}

	/* When the current piece is in action */
	override fun renderMove(engine:GameEngine) {
		if(engine.gameStarted) drawX(engine)
	}

	/* Render score */
	override fun renderLast(engine:GameEngine) {
		val fldPosX = receiver.fieldX(engine)
		val fldPosY = receiver.fieldY(engine)
		val pid = engine.playerID
		val playerColor = EventReceiver.getPlayerColor(pid)

		// Timer
		if(pid==0) receiver.drawDirectFont(224, 8, engine.statistics.time.toTimeStr)

		// Ojama Counter
		var fontColor = COLOR.WHITE
		if(ojama[pid]>=1) fontColor = COLOR.YELLOW
		if(ojama[pid]>=6) fontColor = COLOR.ORANGE
		if(ojama[pid]>=12) fontColor = COLOR.RED

		var strOjama = "${ojama[pid]}"
		if(ojamaAdd[pid]>0) strOjama += "(+${ojamaAdd[pid]})"

		if(strOjama!="0") receiver.drawDirectFont(fldPosX+4, fldPosY+32, strOjama, fontColor)

		// Handicap Counter
		fontColor = COLOR.WHITE
		if(ojamaHandicapLeft[pid]<ojamaHandicap[pid]/2) fontColor = COLOR.YELLOW
		if(ojamaHandicapLeft[pid]<ojamaHandicap[pid]/3) fontColor = COLOR.ORANGE
		if(ojamaHandicapLeft[pid]<ojamaHandicap[pid]/4) fontColor = COLOR.RED

		var strOjamaHandicapLeft = ""
		if(ojamaHandicapLeft[pid]>0) strOjamaHandicapLeft = "${ojamaHandicapLeft[pid]}"

		if(strOjamaHandicapLeft!="0")
			receiver.drawDirectFont(fldPosX+4, fldPosY+16, strOjamaHandicapLeft, fontColor)

		// Score
		var strScoreMultiplier = ""
		if(lastscores[pid]!=0&&lastmultiplier[pid]!=0&&scgettime[pid]>0)
			strScoreMultiplier = "(${lastscores[pid]}e${lastmultiplier[pid]})"

		if(engine.displaySize==1) {
			receiver.drawDirectFont(fldPosX+4, fldPosY+440, String.format("%12d", score[pid]), playerColor)
			receiver.drawDirectFont(fldPosX+4, fldPosY+456, String.format("%12s", strScoreMultiplier), playerColor)
		} else if(engine.gameStarted) {
			receiver.drawDirectFont(fldPosX-28, fldPosY+248, String.format("%8d", score[pid]), playerColor)
			receiver.drawDirectFont(fldPosX-28, fldPosY+264, String.format("%8s", strScoreMultiplier), playerColor)
		}

		if(engine.stat!=GameEngine.Status.MOVE&&engine.stat!=GameEngine.Status.RESULT
			&&engine.gameStarted)
			drawX(engine)

		if(ojamaHard[pid]>0) drawHardOjama(engine)

		super.renderLast(engine)
	}

	override fun getChainColor(engine:GameEngine):COLOR = engine.playerID.let {pid ->
		if(chainDisplayType[pid]==CHAIN_DISPLAY_FEVERSIZE)
			when {
				engine.chain>=feverChainDisplay[pid] -> COLOR.GREEN
				engine.chain==feverChainDisplay[pid]-2 -> COLOR.ORANGE
				engine.chain<feverChainDisplay[pid]-2 -> COLOR.RED
				else -> COLOR.YELLOW
			}
		else super.getChainColor(engine)
	}

	override fun calcChainNewPower(engine:GameEngine, chain:Int):Int {
		return if(chain>FEVER_POWERS.size) FEVER_POWERS[FEVER_POWERS.size-1]
		else FEVER_POWERS[chain-1]
	}

	override fun onClear(engine:GameEngine) {
		feverChainDisplay[engine.playerID] = feverChain[engine.playerID]
	}

	override fun addOjama(engine:GameEngine, pts:Int):Int {
		val pid = engine.playerID
		val enemyID = if(pid==0) 1 else 0

		var pow = 0
		if(zenKeshi[pid]&&zenKeshiType[pid]==ZENKESHI_MODE_ON) pow += 30
		//Add ojama
		var rate = ojamaRate[pid]
		if(hurryUpSeconds[pid]>0&&engine.statistics.time>hurryUpSeconds[pid])
			rate = rate shr engine.statistics.time/(hurryUpSeconds[pid]*60)
		if(rate<=0) rate = 1
		pow += (pts+rate-1)/rate
		ojamaSent[pid] += pow
		var send = pow
		//Counter ojama
		if(ojama[pid]>0&&send>0) {
			val delta = minOf(ojama[pid], send)
			ojama[pid] -= delta
			send -= delta
		}
		if(ojamaAdd[pid]>0&&send>0) {
			val delta = minOf(ojamaAdd[pid], send)
			ojamaAdd[pid] -= delta
			send -= delta
		}
		if(ojamaHandicapLeft[pid]>0&&send>0) {
			val delta = minOf(ojamaHandicapLeft[pid], send)
			ojamaHandicapLeft[pid] -= delta
			send -= delta
		}
		if(send>0) ojamaAdd[enemyID] += send
		return pow
	}

	override fun lineClearEnd(engine:GameEngine):Boolean {
		val pid = engine.playerID
		val enemyID = if(pid==0) 1 else 0
		if(ojamaAdd[enemyID]>0) {
			ojama[enemyID] += ojamaAdd[enemyID]
			ojamaAdd[enemyID] = 0
		}
		//Reset Fever board if necessary
		if(cleared[pid]) {
			val newFeverChain = maxOf(engine.chain+1, feverChain[pid]-2)
			if(newFeverChain>feverChain[pid])
				engine.playSE("cool")
			else if(newFeverChain<feverChain[pid]) engine.playSE("regret")
			feverChain[pid] = newFeverChain
			if(zenKeshi[pid]&&zenKeshiType[pid]==ZENKESHI_MODE_FEVER) {
				feverChain[pid] += 2
				zenKeshi[pid] = false
				zenKeshiDisplay[pid] = 120
			}
			if(feverChain[pid]<feverChainMin[pid]) feverChain[pid] = feverChainMin[pid]
			if(feverChain[pid]>feverChainMax[pid]) feverChain[pid] = feverChainMax[pid]
			loadFeverMap(engine, feverChain[pid])
		}
		//Drop garbage if needed.
		if(ojama[pid]>0&&!ojamaDrop[pid]&&!cleared[pid]) {
			ojamaDrop[pid] = true
			val drop = minOf(ojama[pid], maxAttack[pid])
			ojama[pid] -= drop
			engine.field.garbageDrop(engine, drop, false, ojamaHard[pid])
			engine.field.setAllSkin(engine.skin)
			return true
		}
		//Check for game over
		gameOverCheck(engine)
		return false
	}

	/* Called after every frame */
	override fun onLast(engine:GameEngine) {
		super.onLast(engine)
		updateOjamaMeter(engine)
	}

	/* Called when saving replay */
	override fun saveReplay(engine:GameEngine, prop:CustomProperties):Boolean {
		saveOtherSetting(engine, owner.replayProp)
		savePreset(engine, owner.replayProp, -1-engine.playerID, "digrace")

		owner.replayProp.setProperty("avalanchevsfever.version", version)
		return false
	}

	companion object {
		/** Current version */
		private const val CURRENT_VERSION = 1

		/** Chain multipliers */
		private val FEVER_POWERS = listOf(
			4, 10, 18, 21, 29, 46, 76, 113, 150, 223, 259, 266, 313, 364, 398, 432, 468, 504, 540,
			576, 612, 648, 684, 720 //Arle
		)

		/** Constants for chain display settings */
		const val CHAIN_DISPLAY_FEVERSIZE = 4
	}
}
