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
import mu.nu.nullpo.game.component.Controller
import mu.nu.nullpo.game.event.EventReceiver.COLOR
import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.game.play.GameManager
import mu.nu.nullpo.util.CustomProperties
import mu.nu.nullpo.util.GeneralUtil

/** AVALANCHE VS FEVER MARATHON mode (Release Candidate 1) */
class AvalancheVSFever:AvalancheVSDummyMode() {

	/** Version */
	private var version:Int = 0

	/** Second ojama counter for Fever Mode */
	private var ojamaHandicapLeft:IntArray = IntArray(0)

	/** Chain levels for Fever Mode */
	private var feverChain:IntArray = IntArray(0)

	/** Ojama handicap to start with */
	private var ojamaHandicap:IntArray = IntArray(0)

	/** Fever chain count when last chain hit occurred */
	private var feverChainDisplay:IntArray = IntArray(0)

	/** Chain size for first fever setup */
	private var feverChainStart:IntArray = IntArray(0)

	/* Mode name */
	override val name:String
		get() = "AVALANCHE VS FEVER MARATHON (RC1)"

	/* Mode initialization */
	override fun modeInit(manager:GameManager) {
		super.modeInit(manager)
		ojamaHandicapLeft = IntArray(MAX_PLAYERS)
		feverChain = IntArray(MAX_PLAYERS)
		ojamaHandicap = IntArray(MAX_PLAYERS)
		feverChainDisplay = IntArray(MAX_PLAYERS)
		feverChainStart = IntArray(MAX_PLAYERS)
	}

	/** Load settings not related to speeds
	 * @param engine GameEngine
	 * @param prop Property file to read from
	 */
	private fun loadOtherSetting(engine:GameEngine, prop:CustomProperties) {
		super.loadOtherSetting(engine, prop, "fever")
		val playerID = engine.playerID
		ojamaRate[playerID] = prop.getProperty("avalanchevsfever.ojamaRate.p$playerID", 120)
		ojamaHard[playerID] = prop.getProperty("avalanchevsfever.ojamaHard.p$playerID", 0)
		ojamaHandicap[playerID] = prop.getProperty("avalanchevsfever.ojamaHandicap.p$playerID", 270)
		feverChainStart[playerID] = prop.getProperty("avalanchevsfever.feverChainStart.p$playerID", 5)
	}

	/** Save settings not related to speeds
	 * @param engine GameEngine
	 * @param prop Property file to save to
	 */
	private fun saveOtherSetting(engine:GameEngine, prop:CustomProperties) {
		super.saveOtherSetting(engine, prop, "fever")
		val playerID = engine.playerID
		prop.setProperty("avalanchevsfever.ojamaHandicap.p$playerID", ojamaHandicap[playerID])
		prop.setProperty("avalanchevsfever.feverChainStart.p$playerID", feverChainStart[playerID])
	}

	/* Initialization for each player */
	override fun playerInit(engine:GameEngine, playerID:Int) {
		super.playerInit(engine, playerID)
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
	override fun onSetting(engine:GameEngine, playerID:Int):Boolean {
		// Menu
		if(!engine.owner.replayMode&&engine.statc[4]==0) {
			// Configuration changes
			val change = updateCursor(engine, 29)

			if(change!=0) {
				engine.playSE("change")

				var m = 1
				if(engine.ctrl!!.isPress(Controller.BUTTON_E)) m = 100
				if(engine.ctrl!!.isPress(Controller.BUTTON_F)) m = 1000

				when(menuCursor) {
					0 -> {
						engine.speed.gravity += change*m
						if(engine.speed.gravity<-1) engine.speed.gravity = 99999
						if(engine.speed.gravity>99999) engine.speed.gravity = -1
					}
					1 -> {
						engine.speed.denominator += change*m
						if(engine.speed.denominator<-1) engine.speed.denominator = 99999
						if(engine.speed.denominator>99999) engine.speed.denominator = -1
					}
					2 -> {
						engine.speed.are += change
						if(engine.speed.are<0) engine.speed.are = 99
						if(engine.speed.are>99) engine.speed.are = 0
					}
					3 -> {
						engine.speed.areLine += change
						if(engine.speed.areLine<0) engine.speed.areLine = 99
						if(engine.speed.areLine>99) engine.speed.areLine = 0
					}
					4 -> {
						engine.speed.lineDelay += change
						if(engine.speed.lineDelay<0) engine.speed.lineDelay = 99
						if(engine.speed.lineDelay>99) engine.speed.lineDelay = 0
					}
					5 -> {
						if(m>=10)
							engine.speed.lockDelay += change*10
						else
							engine.speed.lockDelay += change
						if(engine.speed.lockDelay<0) engine.speed.lockDelay = 999
						if(engine.speed.lockDelay>999) engine.speed.lockDelay = 0
					}
					6 -> {
						engine.speed.das += change
						if(engine.speed.das<0) engine.speed.das = 99
						if(engine.speed.das>99) engine.speed.das = 0
					}
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
						ojamaHandicap[playerID] += change*m
						if(ojamaHandicap[playerID]<0) ojamaHandicap[playerID] = 9999
						if(ojamaHandicap[playerID]>9999) ojamaHandicap[playerID] = 0
					}
					19 -> {
						feverMapSet[playerID] += change
						if(feverMapSet[playerID]<0) feverMapSet[playerID] = FEVER_MAPS.size-1
						if(feverMapSet[playerID]>=FEVER_MAPS.size) feverMapSet[playerID] = 0
						loadMapSetFever(engine, playerID, feverMapSet[playerID], true)
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
					25 -> {
						bgmno += change
						if(bgmno<0) bgmno = BGM.count-1
						if(bgmno>=BGM.count) bgmno = 0
					}
					26 -> enableSE[playerID] = !enableSE[playerID]
					27 -> bigDisplay = !bigDisplay
					28, 29 -> {
						presetNumber[playerID] += change
						if(presetNumber[playerID]<0) presetNumber[playerID] = 99
						if(presetNumber[playerID]>99) presetNumber[playerID] = 0
					}
				}
			}

			// 決定
			if(engine.ctrl!!.isPush(Controller.BUTTON_A)&&menuTime>=5) {
				engine.playSE("decide")

				if(menuCursor==28)
					loadPreset(engine, owner.modeConfig, presetNumber[playerID], "fever")
				else if(menuCursor==29) {
					savePreset(engine, owner.modeConfig, presetNumber[playerID], "fever")
					receiver.saveModeConfig(owner.modeConfig)
				} else {
					saveOtherSetting(engine, owner.modeConfig)
					savePreset(engine, owner.modeConfig, -1-playerID, "fever")
					receiver.saveModeConfig(owner.modeConfig)
					engine.statc[4] = 1
				}
			}

			// Cancel
			if(engine.ctrl!!.isPush(Controller.BUTTON_B)) engine.quitflag = true
			menuTime++
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
			} else if(engine.ctrl!!.isPush(Controller.BUTTON_B)) engine.statc[4] = 0// Cancel

		return true
	}

	/* Setting screen drawing */
	override fun renderSetting(engine:GameEngine, playerID:Int) {
		if(engine.statc[4]==0) {
			when {
				menuCursor<9 -> {
					drawMenu(engine, playerID, receiver, 0, COLOR.ORANGE, 0, "GRAVITY", engine.speed.gravity.toString(), "G-MAX", engine.speed.denominator.toString(), "ARE", engine.speed.are.toString(), "ARE LINE", engine.speed.areLine.toString(), "LINE DELAY", engine.speed.lineDelay.toString(), "LOCK DELAY", engine.speed.lockDelay.toString(), "DAS", engine.speed.das.toString(), "FALL DELAY", engine.cascadeDelay.toString(), "CLEAR DELAY", engine.cascadeClearDelay.toString())

					receiver.drawMenuFont(engine, playerID, 0, 19, "PAGE 1/4", COLOR.YELLOW)
				}
				menuCursor<18 -> {
					drawMenu(engine, playerID, receiver, 0, COLOR.CYAN, 9, "ZENKESHI", ZENKESHI_TYPE_NAMES[zenKeshiType[playerID]], "MAX ATTACK", "$maxAttack[playerID]", "COLORS", "$numColors[playerID]", "MIN CHAIN", "$rensaShibari[playerID]", "OJAMA RATE", "$ojamaRate[playerID]",
						"HURRYUP", if(hurryupSeconds[playerID]==0) "NONE" else "${hurryupSeconds[playerID]} SEC",
						"HARD OJAMA", "$ojamaHard[playerID]",
						"X COLUMN", if(dangerColumnDouble[playerID]) "3 AND 4" else "3 ONLY",
						"X SHOW", GeneralUtil.getONorOFF(dangerColumnShowX[playerID]))

					receiver.drawMenuFont(engine, playerID, 0, 19, "PAGE 2/4", COLOR.YELLOW)
				}
				menuCursor<25 -> {
					initMenu(COLOR.PURPLE, 18)
					drawMenu(engine, playerID, receiver, "HANDICAP", "$ojamaHandicap[playerID]", "F-MAP SET", FEVER_MAPS[feverMapSet[playerID]].toUpperCase(), "STARTCHAIN", "$feverChainStart[playerID]")
					menuColor = COLOR.COBALT
					drawMenu(engine, playerID, receiver, "OUTLINE", OUTLINE_TYPE_NAMES[outlineType[playerID]],
						"SHOW CHAIN", if(chainDisplayType[playerID]==CHAIN_DISPLAY_FEVERSIZE)
						"FEVERSIZE" else CHAIN_DISPLAY_NAMES[chainDisplayType[playerID]],
						"FALL ANIM", if(cascadeSlow[playerID]) "FEVER" else "CLASSIC")
					menuColor = COLOR.CYAN
					drawMenu(engine, playerID, receiver, "CHAINPOWER", if(newChainPower[playerID]) "FEVER" else "CLASSIC")

					receiver.drawMenuFont(engine, playerID, 0, 19, "PAGE 3/4", COLOR.YELLOW)
				}
				else -> {
					initMenu(COLOR.PINK, 25)
					drawMenu(engine, playerID, receiver, "BGM", "$BGM.values[bgmno]")
					menuColor = COLOR.YELLOW
					drawMenu(engine, playerID, receiver, "SE", GeneralUtil.getONorOFF(enableSE[playerID]))
					menuColor = COLOR.PINK
					drawMenu(engine, playerID, receiver, "BIG DISP", GeneralUtil.getONorOFF(bigDisplay))
					menuColor = COLOR.GREEN
					drawMenu(engine, playerID, receiver, "LOAD", "$presetNumber[playerID]", "SAVE", "$presetNumber[playerID]")

					receiver.drawMenuFont(engine, playerID, 0, 19, "PAGE 4/4", COLOR.YELLOW)
				}
			}
		} else receiver.drawMenuFont(engine, playerID, 3, 10, "WAIT", COLOR.YELLOW)
	}

	/* Called for initialization during Ready (before initialization) */
	override fun readyInit(engine:GameEngine, playerID:Int):Boolean {
		super.readyInit(engine, playerID)
		ojamaHandicapLeft[playerID] = ojamaHandicap[playerID]
		feverChain[playerID] = feverChainStart[playerID]
		if(engine.field!=null) engine.field!!.reset()
		loadMapSetFever(engine, playerID, feverMapSet[playerID], true)
		return false
	}

	/* Called at game start */
	override fun startGame(engine:GameEngine, playerID:Int) {
		super.startGame(engine, playerID)
		loadFeverMap(engine, playerID, feverChain[playerID])
	}

	/* When the current piece is in action */
	override fun renderMove(engine:GameEngine, playerID:Int) {
		if(engine.gameStarted) drawX(engine, playerID)
	}

	/* Render score */
	override fun renderLast(engine:GameEngine, playerID:Int) {
		val fldPosX = receiver.getFieldDisplayPositionX(engine, playerID)
		val fldPosY = receiver.getFieldDisplayPositionY(engine, playerID)
		val playerColor = if(playerID==0) COLOR.RED else COLOR.BLUE

		// Timer
		if(playerID==0) receiver.drawDirectFont(224, 8, GeneralUtil.getTime(engine.statistics.time))

		// Ojama Counter
		var fontColor = COLOR.WHITE
		if(ojama[playerID]>=1) fontColor = COLOR.YELLOW
		if(ojama[playerID]>=6) fontColor = COLOR.ORANGE
		if(ojama[playerID]>=12) fontColor = COLOR.RED

		var strOjama = "$ojama[playerID]"
		if(ojamaAdd[playerID]>0) strOjama += "(+${ojamaAdd[playerID]})"

		if(strOjama!="0") receiver.drawDirectFont(fldPosX+4, fldPosY+32, strOjama, fontColor)

		// Handicap Counter
		fontColor = COLOR.WHITE
		if(ojamaHandicapLeft[playerID]<ojamaHandicap[playerID]/2) fontColor = COLOR.YELLOW
		if(ojamaHandicapLeft[playerID]<ojamaHandicap[playerID]/3) fontColor = COLOR.ORANGE
		if(ojamaHandicapLeft[playerID]<ojamaHandicap[playerID]/4) fontColor = COLOR.RED

		var strOjamaHandicapLeft = ""
		if(ojamaHandicapLeft[playerID]>0) strOjamaHandicapLeft = "$ojamaHandicapLeft[playerID]"

		if(strOjamaHandicapLeft!="0")
			receiver.drawDirectFont(fldPosX+4, fldPosY+16, strOjamaHandicapLeft, fontColor)

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

		if(engine.stat!=GameEngine.Status.MOVE&&engine.stat!=GameEngine.Status.RESULT
			&&engine.gameStarted)
			drawX(engine, playerID)

		if(ojamaHard[playerID]>0) drawHardOjama(engine, playerID)

		super.renderLast(engine, playerID)
	}

	override fun getChainColor(engine:GameEngine, playerID:Int):COLOR {
		return if(chainDisplayType[playerID]==CHAIN_DISPLAY_FEVERSIZE) {
			when {
				engine.chain>=feverChainDisplay[playerID] -> COLOR.GREEN
				engine.chain==feverChainDisplay[playerID]-2 -> COLOR.ORANGE
				engine.chain<feverChainDisplay[playerID]-2 -> COLOR.RED
				else -> COLOR.YELLOW
			}
		} else
			super.getChainColor(engine, playerID)
	}

	override fun calcChainNewPower(engine:GameEngine, playerID:Int, chain:Int):Int {
		return if(chain>FEVER_POWERS.size)
			FEVER_POWERS[FEVER_POWERS.size-1]
		else
			FEVER_POWERS[chain-1]
	}

	override fun onClear(engine:GameEngine, playerID:Int) {
		feverChainDisplay[playerID] = feverChain[playerID]
	}

	override fun addOjama(engine:GameEngine, playerID:Int, pts:Int) {
		var enemyID = 0
		if(playerID==0) enemyID = 1

		var ojamaNew = 0
		if(zenKeshi[playerID]&&zenKeshiType[playerID]==ZENKESHI_MODE_ON) ojamaNew += 30
		//Add ojama
		var rate = ojamaRate[playerID]
		if(hurryupSeconds[playerID]>0&&engine.statistics.time>hurryupSeconds[playerID])
			rate = rate shr engine.statistics.time/(hurryupSeconds[playerID]*60)
		if(rate<=0) rate = 1
		ojamaNew += (pts+rate-1)/rate
		ojamaSent[playerID] += ojamaNew

		//Counter ojama
		if(ojama[playerID]>0&&ojamaNew>0) {
			val delta = minOf(ojama[playerID], ojamaNew)
			ojama[playerID] -= delta
			ojamaNew -= delta
		}
		if(ojamaAdd[playerID]>0&&ojamaNew>0) {
			val delta = minOf(ojamaAdd[playerID], ojamaNew)
			ojamaAdd[playerID] -= delta
			ojamaNew -= delta
		}
		if(ojamaHandicapLeft[playerID]>0&&ojamaNew>0) {
			val delta = minOf(ojamaHandicapLeft[playerID], ojamaNew)
			ojamaHandicapLeft[playerID] -= delta
			ojamaNew -= delta
		}
		if(ojamaNew>0) ojamaAdd[enemyID] += ojamaNew
	}

	override fun lineClearEnd(engine:GameEngine, playerID:Int):Boolean {
		var enemyID = 0
		if(playerID==0) enemyID = 1
		if(ojamaAdd[enemyID]>0) {
			ojama[enemyID] += ojamaAdd[enemyID]
			ojamaAdd[enemyID] = 0
		}
		//Reset Fever board if necessary
		if(cleared[playerID]) {
			val newFeverChain = maxOf(engine.chain+1, feverChain[playerID]-2)
			if(newFeverChain>feverChain[playerID])
				engine.playSE("cool")
			else if(newFeverChain<feverChain[playerID]) engine.playSE("regret")
			feverChain[playerID] = newFeverChain
			if(zenKeshi[playerID]&&zenKeshiType[playerID]==ZENKESHI_MODE_FEVER) {
				feverChain[playerID] += 2
				zenKeshi[playerID] = false
				zenKeshiDisplay[playerID] = 120
			}
			if(feverChain[playerID]<feverChainMin[playerID]) feverChain[playerID] = feverChainMin[playerID]
			if(feverChain[playerID]>feverChainMax[playerID]) feverChain[playerID] = feverChainMax[playerID]
			loadFeverMap(engine, playerID, feverChain[playerID])
		}
		//Drop garbage if needed.
		if(ojama[playerID]>0&&!ojamaDrop[playerID]&&!cleared[playerID]) {
			ojamaDrop[playerID] = true
			val drop = minOf(ojama[playerID], maxAttack[playerID])
			ojama[playerID] -= drop
			engine.field!!.garbageDrop(engine, drop, false, ojamaHard[playerID])
			engine.field!!.setAllSkin(engine.skin)
			return true
		}
		//Check for game over
		gameOverCheck(engine, playerID)
		return false
	}

	/* Called after every frame */
	override fun onLast(engine:GameEngine, playerID:Int) {
		super.onLast(engine, playerID)
		updateOjamaMeter(engine, playerID)
	}

	/* Called when saving replay */
	override fun saveReplay(engine:GameEngine, playerID:Int, prop:CustomProperties) {
		saveOtherSetting(engine, owner.replayProp)
		savePreset(engine, owner.replayProp, -1-playerID, "digrace")

		owner.replayProp.setProperty("avalanchevsfever.version", version)
	}

	companion object {
		/** Current version */
		private const val CURRENT_VERSION = 1

		/** Chain multipliers */
		private val FEVER_POWERS = intArrayOf(4, 10, 18, 21, 29, 46, 76, 113, 150, 223, 259, 266, 313, 364, 398, 432, 468, 504, 540, 576, 612, 648, 684, 720 //Arle
		)
		/** Constants for chain display settings */
		const val CHAIN_DISPLAY_FEVERSIZE = 4
	}
}
