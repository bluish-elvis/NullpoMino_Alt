/*
 * Copyright (c) 2010-2023, NullNoname
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
package mu.nu.nullpo.game.subsystem.mode.another

import mu.nu.nullpo.game.component.BGMStatus.BGM
import mu.nu.nullpo.game.component.Block
import mu.nu.nullpo.game.component.Controller
import mu.nu.nullpo.game.event.EventReceiver
import mu.nu.nullpo.game.event.EventReceiver.COLOR
import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.game.play.GameManager
import mu.nu.nullpo.gui.common.GameKeyDummy.Companion.MAX_PLAYERS
import mu.nu.nullpo.util.CustomProperties
import mu.nu.nullpo.util.GeneralUtil.toTimeStr

/** AVALANCHE VS BOMB BATTLE mode (Release Candidate 1) */
class AvalancheVSBomb:AvalancheVSDummyMode() {
	/** Version */
	private var version = 0

	/** Settings for starting countdown for ojama blocks */
	private var ojamaCountdown = IntArray(0)

	/* Mode name */
	override val name = "AVALANCHE VS BOMB BATTLE (RC1)"

	/* Mode initialization */
	override fun modeInit(manager:GameManager) {
		super.modeInit(manager)

		ojamaCountdown = IntArray(MAX_PLAYERS)
		newChainPower = BooleanArray(MAX_PLAYERS)
	}

	/** Load settings into [engine] from [prop] not related to speeds */
	private fun loadOtherSetting(engine:GameEngine, prop:CustomProperties) {
		super.loadOtherSetting(engine, prop, "bombbattle")
		val playerID = engine.playerID
		ojamaRate[playerID] = prop.getProperty("avalanchevsbombbattle.ojamaRate.p$playerID", 60)
		ojamaHard[playerID] = prop.getProperty("avalanchevsbombbattle.ojamaHard.p$playerID", 1)
		newChainPower[playerID] = prop.getProperty("avalanchevsbombbattle.newChainPower.p$playerID", false)
		ojamaCountdown[playerID] = prop.getProperty("avalanchevsbombbattle.ojamaCountdown.p$playerID", 5)
	}

	/** Save settings from [engine] into [prop] not related to speeds */
	private fun saveOtherSetting(engine:GameEngine, prop:CustomProperties) {
		super.saveOtherSetting(engine, prop, "bombbattle")
		val playerID = engine.playerID
		prop.setProperty("avalanchevsbombbattle.newChainPower.p$playerID", newChainPower[playerID])
		prop.setProperty("avalanchevsbombbattle.ojamaCountdown.p$playerID", ojamaCountdown[playerID])
	}

	/* Initialization for each player */
	override fun playerInit(engine:GameEngine) {
		super.playerInit(engine)
		val playerID = engine.playerID
		version = if(!engine.owner.replayMode) {
			loadOtherSetting(engine, engine.owner.modeConfig)
			loadPreset(engine, engine.owner.modeConfig, -1-playerID, "bombbattle")
			CURRENT_VERSION
		} else {
			loadOtherSetting(engine, engine.owner.replayProp)
			loadPreset(engine, engine.owner.replayProp, -1-playerID, "bombbattle")
			owner.replayProp.getProperty("avalanchevs.version", 0)
		}
	}

	/* Called at settings screen */
	override fun onSetting(engine:GameEngine):Boolean {
		val playerID = engine.playerID
		// Menu
		if(!engine.owner.replayMode&&engine.statc[4]==0) {
			// Configuration changes
			val change = updateCursor(engine, 33)

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
						ojamaCounterMode[playerID] += change
						if(ojamaCounterMode[playerID]<0) ojamaCounterMode[playerID] = 2
						if(ojamaCounterMode[playerID]>2) ojamaCounterMode[playerID] = 0
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
						engine.colorClearSize += change
						if(engine.colorClearSize<2) engine.colorClearSize = 36
						if(engine.colorClearSize>36) engine.colorClearSize = 2
					}
					14 -> {
						if(m>=10)
							ojamaRate[playerID] += change*100
						else
							ojamaRate[playerID] += change*10
						if(ojamaRate[playerID]<10) ojamaRate[playerID] = 1000
						if(ojamaRate[playerID]>1000) ojamaRate[playerID] = 10
					}
					15 -> {
						if(m>10)
							hurryUpSeconds[playerID] += change*m/10
						else
							hurryUpSeconds[playerID] += change
						if(hurryUpSeconds[playerID]<0) hurryUpSeconds[playerID] = 300
						if(hurryUpSeconds[playerID]>300) hurryUpSeconds[playerID] = 0
					}
					16 -> {
						ojamaHard[playerID] += change
						if(ojamaHard[playerID]<0) ojamaHard[playerID] = 9
						if(ojamaHard[playerID]>9) ojamaHard[playerID] = 0
					}
					17 -> dangerColumnDouble[playerID] = !dangerColumnDouble[playerID]
					18 -> dangerColumnShowX[playerID] = !dangerColumnShowX[playerID]
					19 -> {
						ojamaCountdown[playerID] += change
						if(ojamaCountdown[playerID]<0) ojamaCountdown[playerID] = 9
						if(ojamaCountdown[playerID]>9) ojamaCountdown[playerID] = 0
					}
					20 -> {
						zenKeshiType[playerID] += change
						if(zenKeshiType[playerID]<0) zenKeshiType[playerID] = 2
						if(zenKeshiType[playerID]>2) zenKeshiType[playerID] = 0
					}
					21 -> {
						feverMapSet[playerID] += change
						if(feverMapSet[playerID]<0) feverMapSet[playerID] = FEVER_MAPS.size-1
						if(feverMapSet[playerID]>=FEVER_MAPS.size) feverMapSet[playerID] = 0
					}
					22 -> {
						outlineType[playerID] += change
						if(outlineType[playerID]<0) outlineType[playerID] = 2
						if(outlineType[playerID]>2) outlineType[playerID] = 0
					}
					23 -> {
						chainDisplayType[playerID] += change
						if(chainDisplayType[playerID]<0) chainDisplayType[playerID] = 3
						if(chainDisplayType[playerID]>3) chainDisplayType[playerID] = 0
					}
					24 -> cascadeSlow[playerID] = !cascadeSlow[playerID]
					25 -> newChainPower[playerID] = !newChainPower[playerID]
					26 -> {
						useMap[playerID] = !useMap[playerID]
						if(!useMap[playerID]) engine.field.reset() else
							loadMapPreview(engine, if(mapNumber[playerID]<0) 0 else mapNumber[playerID], true)
					}
					27 -> {
						mapSet[playerID] += change
						if(mapSet[playerID]<0) mapSet[playerID] = 99
						if(mapSet[playerID]>99) mapSet[playerID] = 0
						if(useMap[playerID]) {
							mapNumber[playerID] = -1
							loadMapPreview(engine, if(mapNumber[playerID]<0) 0 else mapNumber[playerID], true)
						}
					}
					28 -> if(useMap[playerID]) {
						mapNumber[playerID] += change
						if(mapNumber[playerID]<-1) mapNumber[playerID] = mapMaxNo[playerID]-1
						if(mapNumber[playerID]>mapMaxNo[playerID]-1) mapNumber[playerID] = -1
						loadMapPreview(engine, if(mapNumber[playerID]<0) 0 else mapNumber[playerID], true)
					} else
						mapNumber[playerID] = -1
					29 -> bigDisplay = !bigDisplay
					30 -> bgmId = rangeCursor(bgmId+change, 0, BGM.count-1)
					31 -> enableSE[playerID] = !enableSE[playerID]
					32, 33 -> presetNumber[playerID] = rangeCursor(presetNumber[playerID]+change, 0, 99)
				}
			}

			// 決定
			if(menuTime<5) menuTime++ else if(engine.ctrl.isPush(Controller.BUTTON_A)) {
				engine.playSE("decide")

				when(menuCursor) {
					32 -> loadPreset(engine, owner.modeConfig, presetNumber[playerID], "bombbattle")
					33 -> {
						savePreset(engine, owner.modeConfig, presetNumber[playerID], "bombbattle")
						owner.saveModeConfig()
					}
					else -> {
						saveOtherSetting(engine, owner.modeConfig)
						savePreset(engine, owner.modeConfig, -1-playerID, "bombbattle")
						owner.saveModeConfig()
						engine.statc[4] = 1
					}
				}
			}

			// Cancel
			if(engine.ctrl.isPush(Controller.BUTTON_B)) engine.quitFlag = true

			// プレビュー用Map読み込み
			if(useMap[playerID]&&menuTime==0)
				loadMapPreview(
					engine, if(mapNumber[playerID]<0)
						0
					else
						mapNumber[playerID], true
				)

			// Random values preview
			if(useMap[playerID]&&propMap[playerID]!=null&&mapNumber[playerID]<0)
				if(menuTime%30==0) {
					engine.statc[5]++
					if(engine.statc[5]>=mapMaxNo[playerID]) engine.statc[5] = 0
					loadMapPreview(engine, engine.statc[5], false)
				}
		} else if(engine.statc[4]==0) {
			menuTime++
			menuCursor = 0

			if(menuTime>=240)
				engine.statc[4] = 1
			else if(menuTime>=180)
				menuCursor = 26
			else if(menuTime>=120)
				menuCursor = 17
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
			if(menuCursor<9) {
				drawMenuSpeeds(engine, receiver, 0, COLOR.ORANGE, 0)
				drawMenu(engine, receiver, "FALL DELAY" to engine.cascadeDelay, "CLEAR DELAY" to engine.cascadeClearDelay)

				receiver.drawMenuFont(engine, 0, 19, "PAGE 1/4", COLOR.YELLOW)
			} else {
				val pid = engine.playerID
				if(menuCursor<17) {
					drawMenu(
						engine,
						receiver,
						0,
						COLOR.CYAN,
						9,
						"COUNTER" to OJAMA_COUNTER_STRING[ojamaCounterMode[pid]],
						"MAX ATTACK" to maxAttack[pid],
						"COLORS" to numColors[pid],
						"MIN CHAIN" to rensaShibari[pid],
						"CLEAR SIZE" to engine.colorClearSize,
						"OJAMA RATE" to ojamaRate[pid],
						"HURRYUP" to if(hurryUpSeconds[pid]==0) "NONE" else "${hurryUpSeconds[pid]}SEC",
						"HARD OJAMA" to ojamaHard[pid]
					)

					receiver.drawMenuFont(engine, 0, 19, "PAGE 2/4", COLOR.YELLOW)
				} else if(menuCursor<26) {
					drawMenu(
						engine, receiver, 0, COLOR.CYAN, 17, "X COLUMN" to if(dangerColumnDouble[pid]) "3 AND 4" else "3 ONLY",
						"X SHOW" to dangerColumnShowX[pid],
						"COUNTDOWN" to ojamaCountdown[pid], "ZENKESHI" to ZENKESHI_TYPE_NAMES[zenKeshiType[pid]]
					)
					drawMenu(
						engine,
						receiver,
						if(zenKeshiType[pid]==ZENKESHI_MODE_FEVER) COLOR.PURPLE else COLOR.WHITE,
						"F-MAP SET" to FEVER_MAPS[feverMapSet[pid]].uppercase()
					)

					drawMenu(
						engine,
						receiver,
						COLOR.COBALT,
						"OUTLINE" to OUTLINE_TYPE_NAMES[outlineType[pid]],
						"SHOW CHAIN" to CHAIN_DISPLAY_NAMES[chainDisplayType[pid]],
						"FALL ANIM" to if(cascadeSlow[pid]) "FEVER" else "CLASSIC"
					)
					drawMenu(engine, receiver, COLOR.CYAN, "CHAINPOWER" to if(newChainPower[pid]) "FEVER" else "CLASSIC")

					receiver.drawMenuFont(engine, 0, 19, "PAGE 3/4", COLOR.YELLOW)
				} else {
					drawMenu(
						engine,
						receiver,
						0,
						COLOR.PINK,
						26,
						"USE MAP" to useMap[pid],
						"MAP SET" to mapSet[pid],
						"MAP NO." to if(mapNumber[pid]<0) "RANDOM" else "${mapNumber[pid]}/${mapMaxNo[pid]-1}",
						"BIG DISP" to bigDisplay
					)

					drawMenu(engine, receiver, COLOR.COBALT, "BGM" to BGM.values[bgmId], "SE" to enableSE[pid])
					drawMenu(engine, receiver, COLOR.GREEN, "LOAD" to presetNumber[pid], "SAVE" to presetNumber[pid])

					receiver.drawMenuFont(engine, 0, 19, "PAGE 4/4", COLOR.YELLOW)
				}
			}
		} else
			receiver.drawMenuFont(engine, 3, 10, "WAIT", COLOR.YELLOW)
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
		if(ojama[pid]>=3) fontColor = COLOR.ORANGE
		if(ojama[pid]>=6) fontColor = COLOR.RED

		var strOjama = "${(ojama[pid]/6)} ${ojama[pid]%6}/6"
		if(ojamaAdd[pid]>0) strOjama += "(+${ojamaAdd[pid]/6} ${ojamaAdd[pid]%6}/6)"

		if(ojama[pid]>0||ojamaAdd[pid]>0)
			receiver.drawDirectFont(fldPosX+4, fldPosY+32, strOjama, fontColor)

		// Score
		var strScoreMultiplier = ""
		if(lastscores[pid]!=0&&lastmultiplier[pid]!=0&&scgettime[pid]>0)
			strScoreMultiplier = "(${lastscores[pid]}e${lastmultiplier[pid]})"

		if(engine.displaySize==1) {
			receiver.drawDirectFont(fldPosX+4, fldPosY+440, "%12d".format(score[pid]), playerColor)
			receiver.drawDirectFont(fldPosX+4, fldPosY+456, "%12s".format(strScoreMultiplier), playerColor)
		} else if(engine.gameStarted) {
			receiver.drawDirectFont(fldPosX-28, fldPosY+248, "%8d".format(score[pid]), playerColor)
			receiver.drawDirectFont(fldPosX-28, fldPosY+264, "%8s".format(strScoreMultiplier), playerColor)
		}

		if(engine.stat!=GameEngine.Status.MOVE&&engine.stat!=GameEngine.Status.RESULT
			&&engine.gameStarted)
			drawX(engine)

		if(engine.stat!=GameEngine.Status.RESULT&&engine.gameStarted)
			for(x in 0..<engine.field.width)
				for(y in 0..<engine.field.height) {
					val b = engine.field.getBlock(x, y) ?: continue
					if(b.isEmpty) continue
					if(b.hard>0)
						if(engine.displaySize==1)
							receiver.drawMenuFont(
								engine, x*2, y*2,
								b.hard.toString(), COLOR.YELLOW, 2f
							)
						else
							receiver.drawMenuFont(engine, x, y, b.hard.toString(), COLOR.YELLOW)
					if(b.countdown>0)
						if(engine.displaySize==1)
							receiver.drawMenuFont(
								engine, x*2, y*2,
								b.countdown.toString(), COLOR.RED, 2f
							)
						else
							receiver.drawMenuFont(engine, x, y, b.countdown.toString(), COLOR.RED)
				}

		super.renderLast(engine)
	}

	override fun lineClearEnd(engine:GameEngine):Boolean {
		val pid = engine.playerID
		val enemyID = if(pid==0) 1 else 0
		if(ojamaAdd[enemyID]>0) {
			ojama[enemyID] += ojamaAdd[enemyID]
			ojamaAdd[enemyID] = 0
		}
		if(zenKeshi[pid]&&zenKeshiType[pid]==ZENKESHI_MODE_FEVER) {
			loadFeverMap(engine, 4)
			zenKeshi[pid] = false
			zenKeshiDisplay[pid] = 120
		}
		//Drop garbage if needed.
		if(ojama[pid]>=6&&!ojamaDrop[pid]&&(!cleared[pid]||ojamaCounterMode[pid]!=OJAMA_COUNTER_FEVER)) {
			ojamaDrop[pid] = true
			val drop = minOf(ojama[pid]/6, maxAttack[pid])
			ojama[pid] -= drop*6
			engine.field.garbageDrop(engine, drop, false, 0, ojamaCountdown[pid])
			engine.field.setAllSkin(engine.skin)
			return true
		}
		//Decrement bomb blocks' countdowns and explode those that hit 0.
		for(y in engine.field.hiddenHeight*-1..<engine.field.height)
			for(x in 0..<engine.field.width) {
				val b = engine.field.getBlock(x, y)
				if(b==null)
					continue
				else if(b.isEmpty)
					continue
				else if(b.countdown>1)
					b.countdown--
				else if(b.countdown==1) explode(engine, x, y)
			}
		//Check for game over
		gameOverCheck(engine)
		return false
	}

	private fun explode(engine:GameEngine, x:Int, y:Int) {
		val b = engine.field.getBlock(x, y) ?: return
		b.countdown = 0
		for(x2 in x-1..x+1)
			for(y2 in y-1..y+1) {
				val b2 = engine.field.getBlock(x2, y2) ?: continue
				if(b2.isEmpty) continue
				if(b2.countdown>0) explode(engine, x2, y2)
				b2.cint = Block.COLOR_WHITE
				b2.setAttribute(true, Block.ATTRIBUTE.GARBAGE)
				b2.hard = ojamaHard[engine.playerID]

				owner.receiver.blockBreak(engine, x2, y2, b2)
			}
	}

	/* Called after every frame */
	override fun onLast(engine:GameEngine) {
		super.onLast(engine)
		updateOjamaMeter(engine)
	}

	override fun updateOjamaMeter(engine:GameEngine) {
		val width = engine.field.width*6
		val blockHeight = engine.blockSize
		// Rising auctionMeter
		val pid = engine.playerID
		val value = ojama[pid]*blockHeight/width
		engine.meterColor = when {
			ojama[pid]>=5*width -> GameEngine.METER_COLOR_RED
			ojama[pid]>=width -> GameEngine.METER_COLOR_ORANGE
			ojama[pid]>=1 -> GameEngine.METER_COLOR_YELLOW
			else -> GameEngine.METER_COLOR_GREEN
		}
		if(value>engine.meterValue) engine.meterValue++
		else if(value<engine.meterValue) engine.meterValue--
	}

	/* Called when saving replay */
	override fun saveReplay(engine:GameEngine, prop:CustomProperties):Boolean {
		saveOtherSetting(engine, owner.replayProp)
		val pid = engine.playerID
		savePreset(engine, owner.replayProp, -1-pid, "bombbattle")

		if(useMap[pid]) fldBackup[pid]?.let {saveMap(it, owner.replayProp, pid)}

		owner.replayProp.setProperty("avalanchevs.version", version)
		return false
	}

	companion object {
		/** Current version */
		private const val CURRENT_VERSION = 0
	}
}
