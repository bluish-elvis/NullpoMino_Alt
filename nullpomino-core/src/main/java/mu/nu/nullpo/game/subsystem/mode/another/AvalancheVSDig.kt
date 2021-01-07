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
package mu.nu.nullpo.game.subsystem.mode.another

import mu.nu.nullpo.game.component.BGMStatus.BGM
import mu.nu.nullpo.game.component.Block
import mu.nu.nullpo.game.component.Controller
import mu.nu.nullpo.game.event.EventReceiver
import mu.nu.nullpo.game.event.EventReceiver.COLOR
import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.game.play.GameManager
import mu.nu.nullpo.util.CustomProperties
import mu.nu.nullpo.util.GeneralUtil
import java.util.*

/** AVALANCHE VS DIG RACE mode (Release Candidate 1) */
class AvalancheVSDig:AvalancheVSDummyMode() {

	/** Version */
	private var version:Int = 0

	/** Ojama handicap to start with */
	private var handicapRows:IntArray = IntArray(MAX_PLAYERS)

	/* Mode name */
	override val name:String = "AVALANCHE VS DIG RACE (RC1)"

	/* Mode initialization */
	override fun modeInit(manager:GameManager) {
		super.modeInit(manager)
		handicapRows = IntArray(MAX_PLAYERS)
	}

	/** Load settings not related to speeds
	 * @param engine GameEngine
	 * @param prop Property file to read from
	 */
	private fun loadOtherSetting(engine:GameEngine, prop:CustomProperties) {
		super.loadOtherSetting(engine, prop, "digrace")
		val playerID = engine.playerID
		ojamaRate[playerID] = prop.getProperty("avalanchevsdigrace.ojamaRate.p$playerID", 420)
		ojamaHard[playerID] = prop.getProperty("avalanchevsdigrace.ojamaHard.p$playerID", 0)
		handicapRows[playerID] = prop.getProperty("avalanchevsdigrace.ojamaHandicap.p$playerID", 6)
	}

	/** Save settings not related to speeds
	 * @param engine GameEngine
	 * @param prop Property file to save to
	 */
	private fun saveOtherSetting(engine:GameEngine, prop:CustomProperties) {
		super.saveOtherSetting(engine, prop, "digrace")
		val playerID = engine.playerID
		prop.setProperty("avalanchevsdigrace.ojamaHandicap.p$playerID", handicapRows[playerID])
	}

	/* Initialization for each player */
	override fun playerInit(engine:GameEngine, playerID:Int) {
		super.playerInit(engine, playerID)
		useMap[playerID] = false
		feverMapSet[playerID] = -1

		version = if(!engine.owner.replayMode) {
			loadOtherSetting(engine, engine.owner.modeConfig)
			loadPreset(engine, engine.owner.modeConfig, -1-playerID, "digrace")
			CURRENT_VERSION
		} else {
			loadOtherSetting(engine, engine.owner.replayProp)
			loadPreset(engine, engine.owner.replayProp, -1-playerID, "digrace")
			owner.replayProp.getProperty("avalanchevsdigrace.version", 0)
		}
	}

	/* Called at settings screen */
	override fun onSetting(engine:GameEngine, playerID:Int):Boolean {
		// Menu
		if(!engine.owner.replayMode&&engine.statc[4]==0) {
			// Configuration changes
			val change = updateCursor(engine, 28)

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
						if(m>=10)
							ojamaRate[playerID] += change*100
						else
							ojamaRate[playerID] += change*10
						if(ojamaRate[playerID]<10) ojamaRate[playerID] = 1000
						if(ojamaRate[playerID]>1000) ojamaRate[playerID] = 10
					}
					14 -> {
						if(m>10)
							hurryupSeconds[playerID] += change*m/10
						else
							hurryupSeconds[playerID] += change
						if(hurryupSeconds[playerID]<0) hurryupSeconds[playerID] = 300
						if(hurryupSeconds[playerID]>300) hurryupSeconds[playerID] = 0
					}
					15 -> {
						ojamaHard[playerID] += change
						if(ojamaHard[playerID]<0) ojamaHard[playerID] = 9
						if(ojamaHard[playerID]>9) ojamaHard[playerID] = 0
					}
					16 -> dangerColumnDouble[playerID] = !dangerColumnDouble[playerID]
					17 -> dangerColumnShowX[playerID] = !dangerColumnShowX[playerID]
					18 -> {
						handicapRows[playerID] += change
						if(handicapRows[playerID]<0) handicapRows[playerID] = 11
						if(handicapRows[playerID]>11) handicapRows[playerID] = 0
					}
					19 -> newChainPower[playerID] = !newChainPower[playerID]
					20 -> {
						engine.colorClearSize += change
						if(engine.colorClearSize<2) engine.colorClearSize = 36
						if(engine.colorClearSize>36) engine.colorClearSize = 2
					}
					21 -> {
						outlineType[playerID] += change
						if(outlineType[playerID]<0) outlineType[playerID] = 2
						if(outlineType[playerID]>2) outlineType[playerID] = 0
					}
					22 -> {
						chainDisplayType[playerID] += change
						if(chainDisplayType[playerID]<0) chainDisplayType[playerID] = 3
						if(chainDisplayType[playerID]>3) chainDisplayType[playerID] = 0
					}
					23 -> cascadeSlow[playerID] = !cascadeSlow[playerID]
					24 -> bgmno = rangeCursor(bgmno+change,0,BGM.count-1)
					25 -> enableSE[playerID] = !enableSE[playerID]
					26 -> bigDisplay = !bigDisplay
					27, 28 -> presetNumber[playerID] = rangeCursor(presetNumber[playerID]+change,0,99)
				}
			}

			// 決定
			if(engine.ctrl.isPush(Controller.BUTTON_A)&&menuTime>=5) {
				engine.playSE("decide")

				when(menuCursor) {
					27 -> loadPreset(engine, owner.modeConfig, presetNumber[playerID], "digrace")
					28 -> {
						savePreset(engine, owner.modeConfig, presetNumber[playerID], "digrace")
						owner.saveModeConfig()
					}
					else -> {
						saveOtherSetting(engine, owner.modeConfig)
						savePreset(engine, owner.modeConfig, -1-playerID, "digrace")
						owner.saveModeConfig()
						engine.statc[4] = 1
					}
				}
			}

			// Cancel
			if(engine.ctrl.isPush(Controller.BUTTON_B)) engine.quitflag = true
			menuTime++
		} else if(engine.statc[4]==0) {
			menuTime++
			menuCursor = 0

			when {
				menuTime>=180 -> engine.statc[4] = 1
				menuTime>=120 -> menuCursor = 18
				menuTime>=60 -> menuCursor = 9
			}
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
	override fun renderSetting(engine:GameEngine, playerID:Int) {
		if(engine.statc[4]==0) {
			when {
				menuCursor<9 -> {
					drawMenu(engine, playerID, receiver, 0, COLOR.ORANGE, 0, "GRAVITY", engine.speed.gravity.toString(), "G-MAX",
						engine.speed.denominator.toString(), "ARE", engine.speed.are.toString(), "ARE LINE",
						engine.speed.areLine.toString(), "LINE DELAY", engine.speed.lineDelay.toString(), "LOCK DELAY",
						engine.speed.lockDelay.toString(), "DAS", engine.speed.das.toString(), "FALL DELAY", engine.cascadeDelay.toString(),
						"CLEAR DELAY", engine.cascadeClearDelay.toString())

					receiver.drawMenuFont(engine, playerID, 0, 19, "PAGE 1/3", COLOR.YELLOW)
				}
				menuCursor<18 -> {
					drawMenu(engine, playerID, receiver, 0, COLOR.CYAN, 9,
						"COUNTER", OJAMA_COUNTER_STRING[ojamaCounterMode[playerID]], "MAX ATTACK", "${maxAttack[playerID]}", "COLORS",
						"${numColors[playerID]}", "MIN CHAIN", "${rensaShibari[playerID]}", "OJAMA RATE", "${ojamaRate[playerID]}",
						"HURRYUP", if(hurryupSeconds[playerID]==0) "NONE" else "${hurryupSeconds[playerID]}SEC",
						"HARD OJAMA", "${ojamaHard[playerID]}",
						"X COLUMN", if(dangerColumnDouble[playerID]) "3 AND 4" else "3 ONLY",
						"X SHOW", GeneralUtil.getONorOFF(dangerColumnShowX[playerID]))

					receiver.drawMenuFont(engine, playerID, 0, 19, "PAGE 2/3", COLOR.YELLOW)
				}
				else -> {
					initMenu(COLOR.PURPLE, 18)
					drawMenu(engine, playerID, receiver, "ROWS", "${handicapRows[playerID]}")
					menuColor = COLOR.CYAN
					drawMenu(engine, playerID, receiver, "CHAINPOWER", if(newChainPower[playerID])
						"FEVER" else "CLASSIC", "CLEAR SIZE", engine.colorClearSize.toString())
					menuColor = COLOR.COBALT
					drawMenu(engine, playerID, receiver, "OUTLINE", OUTLINE_TYPE_NAMES[outlineType[playerID]], "SHOW CHAIN",
						CHAIN_DISPLAY_NAMES[chainDisplayType[playerID]], "FALL ANIM",
						if(cascadeSlow[playerID]) "FEVER" else "CLASSIC")
					menuColor = COLOR.PINK
					drawMenuCompact(engine, playerID, receiver, "BGM", "${BGM.values[bgmno]}")
					menuColor = COLOR.YELLOW
					drawMenuCompact(engine, playerID, receiver, "SE", GeneralUtil.getONorOFF(enableSE[playerID]))
					menuColor = COLOR.PINK
					drawMenu(engine, playerID, receiver, "BIG DISP", GeneralUtil.getONorOFF(bigDisplay))
					menuColor = COLOR.GREEN
					drawMenuCompact(engine, playerID, receiver, "LOAD", "${presetNumber[playerID]}", "SAVE", "${presetNumber[playerID]}")

					receiver.drawMenuFont(engine, playerID, 0, 19, "PAGE 3/3", COLOR.YELLOW)
				}
			}
		} else
			receiver.drawMenuFont(engine, playerID, 3, 10, "WAIT", COLOR.YELLOW)
	}

	/* Called for initialization during Ready (before initialization) */
	override fun onReady(engine:GameEngine, playerID:Int):Boolean {
		if(engine.statc[0]==0) {
			engine.numColors = numColors[playerID]
			engine.lineGravityType = if(cascadeSlow[playerID])
				GameEngine.LineGravity.CASCADE_SLOW
			else
				GameEngine.LineGravity.CASCADE
			engine.rainbowAnimate = true
			engine.displaysize = if(bigDisplay) 1 else 0

			if(outlineType[playerID]==0) engine.blockOutlineType = GameEngine.BLOCK_OUTLINE_NORMAL
			if(outlineType[playerID]==1) engine.blockOutlineType = GameEngine.BLOCK_OUTLINE_SAMECOLOR
			if(outlineType[playerID]==2) engine.blockOutlineType = GameEngine.BLOCK_OUTLINE_NONE

			engine.field?.reset()
		}

		return false
	}

	/* Called at game start */
	override fun startGame(engine:GameEngine, playerID:Int) {
		super.startGame(engine, playerID)

		engine.createFieldIfNeeded()
		engine.field?.also {
			val y = it.height-1
			val rand = Random(engine.random.nextLong())
			val width = it.width
			val x = rand.nextInt(width)
			it.garbageDropPlace(x, y, false, 0)
			it.setBlockColor(x, y, Block.BLOCK_COLOR_GEM_RAINBOW)
			it.garbageDropPlace(x, y-1, false, 1)
			if(x>0) {
				it.garbageDropPlace(x-1, y, false, 1)
				it.garbageDropPlace(x-1, y-1, false, 1)
			}
			if(x<width-1) {
				it.garbageDropPlace(x+1, y, false, 1)
				it.garbageDropPlace(x+1, y-1, false, 1)
			}
			val sizeLimit = maxOf(engine.colorClearSize-1, 2)
			do
				for(i in 0 until handicapRows[playerID])
					for(j in 0 until width)
						if(it.getBlockEmpty(j, y-i))
							it.setBlockColor(j, y-i, BLOCK_COLORS[rand.nextInt(numColors[playerID])])
			while(it.clearColor(sizeLimit, false, false, true)>0)
			it.setAllAttribute(true, Block.ATTRIBUTE.VISIBLE)
			it.setAllSkin(engine.skin)
		}
	}

	/* When the current piece is in action */
	override fun renderMove(engine:GameEngine, playerID:Int) {
		if(engine.gameStarted) drawX(engine, playerID)
	}

	/* Render score */
	override fun renderLast(engine:GameEngine, playerID:Int) {
		val fldPosX = receiver.fieldX(engine, playerID)
		val fldPosY = receiver.fieldY(engine, playerID)
		val playerColor = EventReceiver.getPlayerColor(playerID)
		// Timer
		if(playerID==0) receiver.drawDirectFont(224, 8, GeneralUtil.getTime(engine.statistics.time))

		// Ojama Counter
		var fontColor = COLOR.WHITE
		if(ojama[playerID]>=1) fontColor = COLOR.YELLOW
		if(ojama[playerID]>=6) fontColor = COLOR.ORANGE
		if(ojama[playerID]>=12) fontColor = COLOR.RED

		var strOjama = "${ojama[playerID]}"
		if(ojamaAdd[playerID]>0) strOjama += "(+${ojamaAdd[playerID]})"

		if(strOjama!="0") receiver.drawDirectFont(fldPosX+4, fldPosY+32, strOjama, fontColor)

		// Score
		var strScoreMultiplier = ""
		if(lastscore[playerID]!=0&&lastmultiplier[playerID]!=0&&scgettime[playerID]>0)
			strScoreMultiplier = "(${lastscore[playerID]}e${lastmultiplier[playerID]})"

		if(engine.displaysize==1) {
			receiver.drawDirectFont(fldPosX+4, fldPosY+440, String.format("%12d", score[playerID]), playerColor)
			receiver.drawDirectFont(fldPosX+4, fldPosY+456, String.format("%12s", strScoreMultiplier), playerColor)
		} else if(engine.gameStarted) {
			receiver.drawDirectFont(fldPosX-28, fldPosY+248, String.format("%8d", score[playerID]), playerColor)
			receiver.drawDirectFont(fldPosX-28, fldPosY+264, String.format("%8s", strScoreMultiplier), playerColor)
		}

		if(!owner.engine[playerID].gameActive) return
		if(engine.stat!=GameEngine.Status.MOVE&&engine.stat!=GameEngine.Status.RESULT
			&&engine.gameStarted)
			drawX(engine, playerID)
		drawHardOjama(engine, playerID)

		super.renderLast(engine, playerID)
	}

	override fun lineClearEnd(engine:GameEngine, playerID:Int):Boolean {
		var enemyID = 0
		if(playerID==0) enemyID = 1
		if(ojamaAdd[enemyID]>0) {
			ojama[enemyID] += ojamaAdd[enemyID]
			ojamaAdd[enemyID] = 0
		}
		//Drop garbage if needed.
		if(ojama[playerID]>0&&!ojamaDrop[playerID]&&(!cleared[playerID]||ojamaCounterMode[playerID]!=OJAMA_COUNTER_FEVER)) {
			ojamaDrop[playerID] = true
			val drop = minOf(ojama[playerID], maxAttack[playerID])
			ojama[playerID] -= drop
			engine.field?.garbageDrop(engine, drop, false, ojamaHard[playerID])
			engine.field?.setAllSkin(engine.skin)
			return true
		}
		//Check for game over
		engine.field?.also {
			if(!it.getBlockEmpty(2, 0)||dangerColumnDouble[playerID]&&!it.getBlockEmpty(3, 0))
				engine.stat = GameEngine.Status.GAMEOVER
		}
		return false
	}

	/* Called after every frame */
	override fun onLast(engine:GameEngine, playerID:Int) {
		if(scgettime[playerID]>0) scgettime[playerID]--
		if(chainDisplay[playerID]>0) chainDisplay[playerID]--

		updateOjamaMeter(engine, playerID)

		// Settlement
		if(playerID==1&&owner.engine[0].gameActive) {
			var p1Lose = owner.engine[0].stat==GameEngine.Status.GAMEOVER
			if(!p1Lose&&owner.engine[1].field!=null&&owner.engine[1].stat!=GameEngine.Status.READY)
				p1Lose = owner.engine[1].field!!.howManyGems==0
			var p2Lose = owner.engine[1].stat==GameEngine.Status.GAMEOVER
			if(!p2Lose&&owner.engine[0].field!=null&&owner.engine[0].stat!=GameEngine.Status.READY)
				p2Lose = owner.engine[0].field!!.howManyGems==0
			if(p1Lose&&p2Lose) {
				// Draw
				winnerID = -1
				owner.engine[0].stat = GameEngine.Status.GAMEOVER
				owner.engine[1].stat = GameEngine.Status.GAMEOVER
			} else if(p2Lose&&!p1Lose) {
				// 1P win
				winnerID = 0
				owner.engine[0].stat = GameEngine.Status.EXCELLENT
				owner.engine[1].stat = GameEngine.Status.GAMEOVER
			} else if(p1Lose&&!p2Lose) {
				// 2P win
				winnerID = 1
				owner.engine[0].stat = GameEngine.Status.GAMEOVER
				owner.engine[1].stat = GameEngine.Status.EXCELLENT
			}
			if(p1Lose||p2Lose) {
				owner.engine[0].gameEnded()
				owner.engine[1].gameEnded()
				owner.engine[0].resetStatc()
				owner.engine[1].resetStatc()
				owner.engine[0].statc[1] = 1
				owner.engine[1].statc[1] = 1
				owner.bgmStatus.bgm = BGM.SILENT
			}
		}
	}

	/* Called when saving replay */
	override fun saveReplay(engine:GameEngine, playerID:Int, prop:CustomProperties) {
		saveOtherSetting(engine, owner.replayProp)
		savePreset(engine, owner.replayProp, -1-playerID, "digrace")

		owner.replayProp.setProperty("avalanchevsdigrace.version", version)
	}

	companion object {
		/** Current version */
		private const val CURRENT_VERSION = 0
	}
}
