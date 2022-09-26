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
package mu.nu.nullpo.game.subsystem.mode.another

import mu.nu.nullpo.game.component.BGMStatus.BGM
import mu.nu.nullpo.game.component.Block
import mu.nu.nullpo.game.component.Controller
import mu.nu.nullpo.game.event.EventReceiver
import mu.nu.nullpo.game.event.EventReceiver.COLOR
import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.game.play.GameManager
import mu.nu.nullpo.util.CustomProperties
import mu.nu.nullpo.util.GeneralUtil.toTimeStr
import kotlin.random.Random

/** AVALANCHE VS DIG RACE mode (Release Candidate 1) */
class AvalancheVSDig:AvalancheVSDummyMode() {

	/** Version */
	private var version = 0

	/** Ojama handicap to start with */
	private var handicapRows = IntArray(MAX_PLAYERS)

	/* Mode name */
	override val name = "AVALANCHE VS DIG RACE (RC1)"

	/* Mode initialization */
	override fun modeInit(manager:GameManager) {
		super.modeInit(manager)
		handicapRows = IntArray(MAX_PLAYERS)
	}

	/** Load settings into [engine] from [prop] not related to speeds */
	private fun loadOtherSetting(engine:GameEngine, prop:CustomProperties) {
		super.loadOtherSetting(engine, prop, "digrace")
		val playerID = engine.playerID
		ojamaRate[playerID] = prop.getProperty("avalanchevsdigrace.ojamaRate.p$playerID", 420)
		ojamaHard[playerID] = prop.getProperty("avalanchevsdigrace.ojamaHard.p$playerID", 0)
		handicapRows[playerID] = prop.getProperty("avalanchevsdigrace.ojamaHandicap.p$playerID", 6)
	}

	/** Save settings from [engine] into [prop] not related to speeds */
	private fun saveOtherSetting(engine:GameEngine, prop:CustomProperties) {
		super.saveOtherSetting(engine, prop, "digrace")
		val playerID = engine.playerID
		prop.setProperty("avalanchevsdigrace.ojamaHandicap.p$playerID", handicapRows[playerID])
	}

	/* Initialization for each player */
	override fun playerInit(engine:GameEngine) {
		super.playerInit(engine)
		val playerID = engine.playerID
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
	override fun onSetting(engine:GameEngine):Boolean {
		val pid = engine.playerID
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
						ojamaCounterMode[pid] += change
						if(ojamaCounterMode[pid]<0) ojamaCounterMode[pid] = 2
						if(ojamaCounterMode[pid]>2) ojamaCounterMode[pid] = 0
					}
					10 -> {
						if(m>=10)
							maxAttack[pid] += change*10
						else
							maxAttack[pid] += change
						if(maxAttack[pid]<0) maxAttack[pid] = 99
						if(maxAttack[pid]>99) maxAttack[pid] = 0
					}
					11 -> {
						numColors[pid] += change
						if(numColors[pid]<3) numColors[pid] = 5
						if(numColors[pid]>5) numColors[pid] = 3
					}
					12 -> {
						rensaShibari[pid] += change
						if(rensaShibari[pid]<1) rensaShibari[pid] = 20
						if(rensaShibari[pid]>20) rensaShibari[pid] = 1
					}
					13 -> {
						if(m>=10)
							ojamaRate[pid] += change*100
						else
							ojamaRate[pid] += change*10
						if(ojamaRate[pid]<10) ojamaRate[pid] = 1000
						if(ojamaRate[pid]>1000) ojamaRate[pid] = 10
					}
					14 -> {
						if(m>10)
							hurryupSeconds[pid] += change*m/10
						else
							hurryupSeconds[pid] += change
						if(hurryupSeconds[pid]<0) hurryupSeconds[pid] = 300
						if(hurryupSeconds[pid]>300) hurryupSeconds[pid] = 0
					}
					15 -> {
						ojamaHard[pid] += change
						if(ojamaHard[pid]<0) ojamaHard[pid] = 9
						if(ojamaHard[pid]>9) ojamaHard[pid] = 0
					}
					16 -> dangerColumnDouble[pid] = !dangerColumnDouble[pid]
					17 -> dangerColumnShowX[pid] = !dangerColumnShowX[pid]
					18 -> {
						handicapRows[pid] += change
						if(handicapRows[pid]<0) handicapRows[pid] = 11
						if(handicapRows[pid]>11) handicapRows[pid] = 0
					}
					19 -> newChainPower[pid] = !newChainPower[pid]
					20 -> {
						engine.colorClearSize += change
						if(engine.colorClearSize<2) engine.colorClearSize = 36
						if(engine.colorClearSize>36) engine.colorClearSize = 2
					}
					21 -> {
						outlineType[pid] += change
						if(outlineType[pid]<0) outlineType[pid] = 2
						if(outlineType[pid]>2) outlineType[pid] = 0
					}
					22 -> {
						chainDisplayType[pid] += change
						if(chainDisplayType[pid]<0) chainDisplayType[pid] = 3
						if(chainDisplayType[pid]>3) chainDisplayType[pid] = 0
					}
					23 -> cascadeSlow[pid] = !cascadeSlow[pid]
					24 -> bgmno = rangeCursor(bgmno+change, 0, BGM.count-1)
					25 -> enableSE[pid] = !enableSE[pid]
					26 -> bigDisplay = !bigDisplay
					27, 28 -> presetNumber[pid] = rangeCursor(presetNumber[pid]+change, 0, 99)
				}
			}

			// 決定
			if(menuTime<5) menuTime++ else if(engine.ctrl.isPush(Controller.BUTTON_A)) {
				engine.playSE("decide")

				when(menuCursor) {
					27 -> loadPreset(engine, owner.modeConfig, presetNumber[pid], "digrace")
					28 -> {
						savePreset(engine, owner.modeConfig, presetNumber[pid], "digrace")
						owner.saveModeConfig()
					}
					else -> {
						saveOtherSetting(engine, owner.modeConfig)
						savePreset(engine, owner.modeConfig, -1-pid, "digrace")
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

			when {
				menuTime>=180 -> engine.statc[4] = 1
				menuTime>=120 -> menuCursor = 18
				menuTime>=60 -> menuCursor = 9
			}
		} else // Start
			if(owner.engine[0].statc[4]==1&&owner.engine[1].statc[4]==1&&pid==1) {
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

					receiver.drawMenuFont(engine, 0, 19, "PAGE 1/3", COLOR.YELLOW)
				}
				menuCursor<18 -> {
					drawMenu(
						engine, receiver, 0, COLOR.CYAN, 9,
						"COUNTER" to OJAMA_COUNTER_STRING[ojamaCounterMode[pid]],
						"MAX ATTACK" to maxAttack[pid],
						"COLORS" to numColors[pid],
						"MIN CHAIN" to rensaShibari[pid],
						"OJAMA RATE" to ojamaRate[pid],
						"HURRYUP" to if(hurryupSeconds[pid]==0) "NONE" else "${hurryupSeconds[pid]}SEC",
						"HARD OJAMA" to ojamaHard[pid],
						"X COLUMN" to if(dangerColumnDouble[pid]) "3 AND 4" else "3 ONLY",
						"X SHOW" to dangerColumnShowX[pid]
					)

					receiver.drawMenuFont(engine, 0, 19, "PAGE 2/3", COLOR.YELLOW)
				}
				else -> {
					drawMenu(engine, receiver, 0, COLOR.PURPLE, 18, "ROWS" to handicapRows[pid])

					drawMenu(
						engine, receiver, COLOR.CYAN, "CHAINPOWER" to if(newChainPower[pid]) "FEVER" else "CLASSIC",
						"CLEAR SIZE" to engine.colorClearSize
					)

					drawMenu(
						engine, receiver, COLOR.COBALT,
						"OUTLINE" to OUTLINE_TYPE_NAMES[outlineType[pid]],
						"SHOW CHAIN" to CHAIN_DISPLAY_NAMES[chainDisplayType[pid]],
						"FALL ANIM" to if(cascadeSlow[pid]) "FEVER" else "CLASSIC"
					)

					drawMenuCompact(engine, receiver, COLOR.PINK, "BGM" to BGM.values[bgmno])
					drawMenuCompact(engine, receiver, COLOR.YELLOW, "SE" to enableSE[pid])
					drawMenu(engine, receiver, COLOR.PINK, "BIG DISP" to bigDisplay)
					drawMenuCompact(engine, receiver, COLOR.GREEN, "LOAD" to presetNumber[pid], "SAVE" to presetNumber[pid])

					receiver.drawMenuFont(engine, 0, 19, "PAGE 3/3", COLOR.YELLOW)
				}
			}
		} else
			receiver.drawMenuFont(engine, 3, 10, "WAIT", COLOR.YELLOW)
	}

	/* Called for initialization during Ready (before initialization) */
	override fun onReady(engine:GameEngine):Boolean {
		val pid = engine.playerID
		if(engine.statc[0]==0) {
			engine.numColors = numColors[pid]
			engine.lineGravityType = if(cascadeSlow[pid])
				GameEngine.LineGravity.CASCADE_SLOW
			else
				GameEngine.LineGravity.CASCADE
			engine.rainbowAnimate = true
			engine.displaySize = if(bigDisplay) 1 else 0

			if(outlineType[pid]==0) engine.blockOutlineType = GameEngine.BLOCK_OUTLINE_NORMAL
			if(outlineType[pid]==1) engine.blockOutlineType = GameEngine.BLOCK_OUTLINE_SAMECOLOR
			if(outlineType[pid]==2) engine.blockOutlineType = GameEngine.BLOCK_OUTLINE_NONE

			engine.field.reset()
		}

		return false
	}

	/* Called at game start */
	override fun startGame(engine:GameEngine) {
		super.startGame(engine)

		engine.createFieldIfNeeded()
		engine.field.also {
			val y = it.height-1
			val rand = Random(engine.random.nextLong())
			val width = it.width
			val x = rand.nextInt(width)
			it.garbageDropPlace(x, y, false, 0)
			it.setBlockColor(x, y, Block.COLOR_GEM_RAINBOW)
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
				for(i in 0 until handicapRows[engine.playerID])
					for(j in 0 until width)
						if(it.getBlockEmpty(j, y-i))
							it.setBlockColor(j, y-i, BLOCK_COLORS[rand.nextInt(numColors[engine.playerID])])
			while(it.clearColor(sizeLimit, false, false, true)>0)
			it.setAllAttribute(true, Block.ATTRIBUTE.VISIBLE)
			it.setAllSkin(engine.skin)
		}
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

		if(!owner.engine[pid].gameActive) return
		if(engine.stat!=GameEngine.Status.MOVE&&engine.stat!=GameEngine.Status.RESULT
			&&engine.gameStarted)
			drawX(engine)
		drawHardOjama(engine)

		super.renderLast(engine)
	}

	override fun lineClearEnd(engine:GameEngine):Boolean {
		val pid = engine.playerID
		val enemyID = if(pid==0) 1 else 0
		if(ojamaAdd[enemyID]>0) {
			ojama[enemyID] += ojamaAdd[enemyID]
			ojamaAdd[enemyID] = 0
		}
		//Drop garbage if needed.
		if(ojama[pid]>0&&!ojamaDrop[pid]&&(!cleared[pid]||ojamaCounterMode[pid]!=OJAMA_COUNTER_FEVER)) {
			ojamaDrop[pid] = true
			val drop = minOf(ojama[pid], maxAttack[pid])
			ojama[pid] -= drop
			engine.field.garbageDrop(engine, drop, false, ojamaHard[pid])
			engine.field.setAllSkin(engine.skin)
			return true
		}
		//Check for game over
		engine.field.also {
			if(!it.getBlockEmpty(2, 0)||dangerColumnDouble[pid]&&!it.getBlockEmpty(3, 0))
				engine.stat = GameEngine.Status.GAMEOVER
		}
		return false
	}

	/* Called after every frame */
	override fun onLast(engine:GameEngine) {
		super.onLast(engine)
		val pid = engine.playerID
		if(scgettime[pid]>0) scgettime[pid]--
		if(chainDisplay[pid]>0) chainDisplay[pid]--

		updateOjamaMeter(engine)

		// Settlement
		if(pid==1&&owner.engine[0].gameActive) {
			var p1Lose = owner.engine[0].stat==GameEngine.Status.GAMEOVER
			if(!p1Lose&&owner.engine[1].stat!=GameEngine.Status.READY)
				p1Lose = owner.engine[1].field.howManyGems==0
			var p2Lose = owner.engine[1].stat==GameEngine.Status.GAMEOVER
			if(!p2Lose&&owner.engine[0].stat!=GameEngine.Status.READY)
				p2Lose = owner.engine[0].field.howManyGems==0
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
				owner.musMan.bgm = BGM.Silent
			}
		}
	}

	/* Called when saving replay */
	override fun saveReplay(engine:GameEngine, prop:CustomProperties):Boolean {
		saveOtherSetting(engine, owner.replayProp)
		savePreset(engine, owner.replayProp, -1-engine.playerID, "digrace")

		owner.replayProp.setProperty("avalanchevsdigrace.version", version)
		return false
	}

	companion object {
		/** Current version */
		private const val CURRENT_VERSION = 0
	}
}
