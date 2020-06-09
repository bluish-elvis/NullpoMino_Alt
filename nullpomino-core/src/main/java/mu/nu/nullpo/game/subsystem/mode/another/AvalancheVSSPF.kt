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

/** AVALANCHE-SPF VS-BATTLE mode (Release Candidate 1) */
class AvalancheVSSPF:AvalancheVSDummyMode() {

	/** Version */
	private var version:Int = 0

	/** Settings for starting countdown for ojama blocks */
	private var ojamaCountdown:IntArray = IntArray(MAX_PLAYERS)

	/** Drop patterns */
	private var dropPattern:Array<Array<IntArray>> = emptyArray()

	/** Drop values set selected */
	private var dropSet:IntArray = IntArray(MAX_PLAYERS)

	/** Drop values selected */
	private var dropMap:IntArray = IntArray(MAX_PLAYERS)

	/** Drop multipliers */
	private var attackMultiplier:DoubleArray = DoubleArray(MAX_PLAYERS)
	private var defendMultiplier:DoubleArray = DoubleArray(MAX_PLAYERS)

	/** Flag set when counters have been decremented */
	private var countdownDecremented:BooleanArray = BooleanArray(MAX_PLAYERS)

	/** Flag set when cleared ojama have been turned into normal blocks */
	private var ojamaChecked:BooleanArray = BooleanArray(MAX_PLAYERS)

	/* Mode name */
	override val name:String
		get() = "AVALANCHE-SPF VS-BATTLE (BETA)"

	/* Mode initialization */
	override fun modeInit(manager:GameManager) {
		super.modeInit(manager)
		ojamaCountdown = IntArray(MAX_PLAYERS)
		dropSet = IntArray(MAX_PLAYERS)
		dropMap = IntArray(MAX_PLAYERS)
		dropPattern = Array(MAX_PLAYERS){emptyArray<IntArray>()}
		attackMultiplier = DoubleArray(MAX_PLAYERS)
		defendMultiplier = DoubleArray(MAX_PLAYERS)
		countdownDecremented = BooleanArray(MAX_PLAYERS)
		ojamaChecked = BooleanArray(MAX_PLAYERS)
	}

	/** Load settings not related to speeds
	 * @param engine GameEngine
	 * @param prop Property file to read from
	 */
	private fun loadOtherSetting(engine:GameEngine, prop:CustomProperties) {
		super.loadOtherSetting(engine, prop, "spf")
		val playerID = engine.playerID
		ojamaHard[playerID] = 4
		ojamaRate[playerID] = prop.getProperty("avalanchevsspf.ojamaRate.p$playerID", 120)
		ojamaCountdown[playerID] = prop.getProperty("avalanchevsspf.ojamaCountdown.p$playerID", 3)
		dropSet[playerID] = prop.getProperty("avalanchevsspf.dropSet.p$playerID", 4)
		dropMap[playerID] = prop.getProperty("avalanchevsspf.dropMap.p$playerID", 0)
	}

	/** Save settings not related to speeds
	 * @param engine GameEngine
	 * @param prop Property file to save to
	 */
	private fun saveOtherSetting(engine:GameEngine, prop:CustomProperties) {
		super.saveOtherSetting(engine, prop, "spf")
		val playerID = engine.playerID
		prop.setProperty("avalanchevsspf.ojamaCountdown.p$playerID", ojamaCountdown[playerID])
		prop.setProperty("avalanchevsspf.dropSet.p$playerID", dropSet[playerID])
		prop.setProperty("avalanchevsspf.dropMap.p$playerID", dropMap[playerID])
	}

	private fun loadDropMapPreview(engine:GameEngine, playerID:Int, pattern:Array<IntArray>?) {
		if(pattern==null&&engine.field!=null)
			engine.field!!.reset()
		else if(pattern!=null) {
			engine.createFieldIfNeeded()
			engine.field!!.reset()
			var patternCol = 0
			val maxHeight = engine.field!!.height-1
			for(x in 0 until engine.field!!.width) {
				if(patternCol>=pattern.size) patternCol = 0
				for(patternRow in pattern[patternCol].indices) {
					engine.field!!.setBlockColor(x, maxHeight-patternRow, pattern[patternCol][patternRow])
					val blk = engine.field!!.getBlock(x, maxHeight-patternRow)
					blk!!.setAttribute(true, Block.ATTRIBUTE.VISIBLE)
					blk.setAttribute(true, Block.ATTRIBUTE.OUTLINE)
				}
				patternCol++
			}
			engine.field!!.setAllSkin(engine.skin)
		}
	}

	/* Initialization for each player */
	override fun playerInit(engine:GameEngine, playerID:Int) {
		super.playerInit(engine, playerID)
		numColors[playerID] = 4
		ojamaHard[playerID] = 4
		countdownDecremented[playerID] = true
		ojamaChecked[playerID] = false

		version = if(!engine.owner.replayMode) {
			loadOtherSetting(engine, engine.owner.modeConfig)
			loadPreset(engine, engine.owner.modeConfig, -1-playerID, "spf")
			CURRENT_VERSION
		} else {
			loadOtherSetting(engine, engine.owner.replayProp)
			loadPreset(engine, engine.owner.replayProp, -1-playerID, "spf")
			owner.replayProp.getProperty("avalanchevs.version", 0)
		}
	}

	/* Called at settings screen */
	override fun onSetting(engine:GameEngine, playerID:Int):Boolean {
		// Menu
		if(!engine.owner.replayMode&&engine.statc[4]==0) {
			// Configuration changes
			// Up
			if(engine.ctrl.isMenuRepeatKey(Controller.BUTTON_UP)) {
				menuCursor--
				if(menuCursor<0) {
					menuCursor = 33
					loadDropMapPreview(engine, playerID, DROP_PATTERNS[dropSet[playerID]][dropMap[playerID]])
				} else if(menuCursor==31) engine.field = null
				engine.playSE("cursor")
			}
			// Down
			if(engine.ctrl.isMenuRepeatKey(Controller.BUTTON_DOWN)) {
				menuCursor++
				if(menuCursor>33) {
					menuCursor = 0
					engine.field = null
				} else if(menuCursor==32) loadDropMapPreview(engine, playerID, DROP_PATTERNS[dropSet[playerID]][dropMap[playerID]])
				engine.playSE("cursor")
			}

			// Configuration changes
			var change = 0
			if(engine.ctrl.isMenuRepeatKey(Controller.BUTTON_LEFT)) change = -1
			if(engine.ctrl.isMenuRepeatKey(Controller.BUTTON_RIGHT)) change = 1

			if(change!=0) {
				engine.playSE("change")

				var m = 1
				if(engine.ctrl.isPress(Controller.BUTTON_E)) m = 100
				if(engine.ctrl.isPress(Controller.BUTTON_F)) m = 1000

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
						rensaShibari[playerID] += change
						if(rensaShibari[playerID]<1) rensaShibari[playerID] = 20
						if(rensaShibari[playerID]>20) rensaShibari[playerID] = 1
					}
					12 -> {
						engine.colorClearSize += change
						if(engine.colorClearSize<2) engine.colorClearSize = 36
						if(engine.colorClearSize>36) engine.colorClearSize = 2
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
					15 -> dangerColumnDouble[playerID] = !dangerColumnDouble[playerID]
					16 -> dangerColumnShowX[playerID] = !dangerColumnShowX[playerID]
					17 -> {
						ojamaCountdown[playerID] += change
						if(ojamaCountdown[playerID]<1) ojamaCountdown[playerID] = 10
						if(ojamaCountdown[playerID]>10) ojamaCountdown[playerID] = 1
					}
					18 -> {
						zenKeshiType[playerID] += change
						if(zenKeshiType[playerID]<0) zenKeshiType[playerID] = 2
						if(zenKeshiType[playerID]>2) zenKeshiType[playerID] = 0
					}
					19 -> {
						feverMapSet[playerID] += change
						if(feverMapSet[playerID]<0) feverMapSet[playerID] = FEVER_MAPS.size-1
						if(feverMapSet[playerID]>=FEVER_MAPS.size) feverMapSet[playerID] = 0
					}
					20 -> {
						outlineType[playerID] += change
						if(outlineType[playerID]<0) outlineType[playerID] = 2
						if(outlineType[playerID]>2) outlineType[playerID] = 0
					}
					21 -> {
						chainDisplayType[playerID] += change
						if(chainDisplayType[playerID]<0) chainDisplayType[playerID] = 3
						if(chainDisplayType[playerID]>3) chainDisplayType[playerID] = 0
					}
					22 -> cascadeSlow[playerID] = !cascadeSlow[playerID]
					23 -> newChainPower[playerID] = !newChainPower[playerID]
					24 -> {
						useMap[playerID] = !useMap[playerID]
						if(!useMap[playerID]) {
							if(engine.field!=null) engine.field!!.reset()
						} else
							loadMapPreview(engine, playerID, if(mapNumber[playerID]<0) 0 else mapNumber[playerID], true)
					}
					25 -> {
						mapSet[playerID] += change
						if(mapSet[playerID]<0) mapSet[playerID] = 99
						if(mapSet[playerID]>99) mapSet[playerID] = 0
						if(useMap[playerID]) {
							mapNumber[playerID] = -1
							loadMapPreview(engine, playerID, if(mapNumber[playerID]<0) 0 else mapNumber[playerID], true)
						}
					}
					26 -> if(useMap[playerID]) {
						mapNumber[playerID] += change
						if(mapNumber[playerID]<-1) mapNumber[playerID] = mapMaxNo[playerID]-1
						if(mapNumber[playerID]>mapMaxNo[playerID]-1) mapNumber[playerID] = -1
						loadMapPreview(engine, playerID, if(mapNumber[playerID]<0) 0 else mapNumber[playerID], true)
					} else
						mapNumber[playerID] = -1
					27 -> bigDisplay = !bigDisplay
					28 -> {
						bgmno += change
						if(bgmno<0) bgmno = BGM.count-1
						if(bgmno>=BGM.count) bgmno = 0
					}
					29 -> enableSE[playerID] = !enableSE[playerID]
					30, 31 -> {
						presetNumber[playerID] += change
						if(presetNumber[playerID]<0) presetNumber[playerID] = 99
						if(presetNumber[playerID]>99) presetNumber[playerID] = 0
					}
					32 -> {
						dropSet[playerID] += change
						if(dropSet[playerID]<0) dropSet[playerID] = DROP_PATTERNS.size-1
						if(dropSet[playerID]>=DROP_PATTERNS.size) dropSet[playerID] = 0
						if(dropMap[playerID]>=DROP_PATTERNS[dropSet[playerID]].size) dropMap[playerID] = 0
						loadDropMapPreview(engine, playerID, DROP_PATTERNS[dropSet[playerID]][dropMap[playerID]])
					}
					33 -> {
						dropMap[playerID] += change
						if(dropMap[playerID]<0) dropMap[playerID] = DROP_PATTERNS[dropSet[playerID]].size-1
						if(dropMap[playerID]>=DROP_PATTERNS[dropSet[playerID]].size) dropMap[playerID] = 0
						loadDropMapPreview(engine, playerID, DROP_PATTERNS[dropSet[playerID]][dropMap[playerID]])
					}
				}
			}

			// 決定
			if(engine.ctrl.isPush(Controller.BUTTON_A)&&menuTime>=5) {
				engine.playSE("decide")

				if(menuCursor==30)
					loadPreset(engine, owner.modeConfig, presetNumber[playerID], "spf")
				else if(menuCursor==31) {
					savePreset(engine, owner.modeConfig, presetNumber[playerID], "spf")
					owner.saveModeConfig()
				} else {
					saveOtherSetting(engine, owner.modeConfig)
					savePreset(engine, owner.modeConfig, -1-playerID, "spf")
					owner.saveModeConfig()
					engine.statc[4] = 1
				}
			}

			// Cancel
			if(engine.ctrl.isPush(Controller.BUTTON_B)) engine.quitflag = true

			// プレビュー用Map読み込み
			if(useMap[playerID]&&menuTime==0)
				loadMapPreview(engine, playerID, if(mapNumber[playerID]<0)
					0
				else
					mapNumber[playerID], true)

			// Random values preview
			if(useMap[playerID]&&propMap[playerID]!=null&&mapNumber[playerID]<0)
				if(menuTime%30==0) {
					engine.statc[5]++
					if(engine.statc[5]>=mapMaxNo[playerID]) engine.statc[5] = 0
					loadMapPreview(engine, playerID, engine.statc[5], false)
				}

			menuTime++
		} else if(engine.statc[4]==0) {
			menuTime++
			menuCursor = 0

			if(menuTime>=300)
				engine.statc[4] = 1
			else if(menuTime==240) {
				menuCursor = 32
				loadDropMapPreview(engine, playerID, DROP_PATTERNS[dropSet[playerID]][dropMap[playerID]])
			} else if(menuTime>=180)
				menuCursor = 24
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

	override fun onMove(engine:GameEngine, playerID:Int):Boolean {
		cleared[playerID] = false
		ojamaDrop[playerID] = false
		countdownDecremented[playerID] = false
		return false
	}

	public override fun onClear(engine:GameEngine, playerID:Int) {
		ojamaChecked[playerID] = false
	}

	/* Setting screen drawing */
	override fun renderSetting(engine:GameEngine, playerID:Int) {
		if(engine.statc[4]==0) {
			if(menuCursor<9) {
				drawMenu(engine, playerID, receiver, 0, COLOR.ORANGE, 0, "GRAVITY", engine.speed.gravity.toString(), "G-MAX", engine.speed.denominator.toString(), "ARE", engine.speed.are.toString(), "ARE LINE", engine.speed.areLine.toString(), "LINE DELAY", engine.speed.lineDelay.toString(), "LOCK DELAY", engine.speed.lockDelay.toString(), "DAS", engine.speed.das.toString(), "FALL DELAY", engine.cascadeDelay.toString(), "CLEAR DELAY", engine.cascadeClearDelay.toString())

				receiver.drawMenuFont(engine, playerID, 0, 19, "PAGE 1/5", COLOR.YELLOW)
			} else if(menuCursor<17) {
				drawMenu(engine, playerID, receiver, 0, COLOR.CYAN, 9, "COUNTER", OJAMA_COUNTER_STRING[ojamaCounterMode[playerID]], "MAX ATTACK", "${maxAttack[playerID]}", "MIN CHAIN", "${rensaShibari[playerID]}", "CLEAR SIZE", engine.colorClearSize.toString(), "OJAMA RATE", "${ojamaRate[playerID]}", "HURRYUP",
					if(hurryupSeconds[playerID]==0)
						"NONE"
					else
						"${hurryupSeconds[playerID]}SEC", "X COLUMN", if(dangerColumnDouble[playerID])
					"3 AND 4"
				else
					"3 ONLY", "X SHOW", GeneralUtil.getONorOFF(dangerColumnShowX[playerID]))

				receiver.drawMenuFont(engine, playerID, 0, 19, "PAGE 2/5", COLOR.YELLOW)
			} else if(menuCursor<24) {
				initMenu(COLOR.CYAN, 17)
				drawMenu(engine, playerID, receiver, "COUNTDOWN", if(ojamaCountdown[playerID]==10)
					"NONE"
				else
					"${ojamaCountdown[playerID]}", "ZENKESHI", ZENKESHI_TYPE_NAMES[zenKeshiType[playerID]])
				menuColor = if(zenKeshiType[playerID]==ZENKESHI_MODE_FEVER)
					COLOR.PURPLE else COLOR.WHITE
				drawMenu(engine, playerID, receiver, "F-MAP SET", FEVER_MAPS[feverMapSet[playerID]].toUpperCase())
				menuColor = COLOR.COBALT
				drawMenu(engine, playerID, receiver, "OUTLINE", OUTLINE_TYPE_NAMES[outlineType[playerID]], "SHOW CHAIN", CHAIN_DISPLAY_NAMES[chainDisplayType[playerID]],
					"FALL ANIM", if(cascadeSlow[playerID]) "FEVER" else "CLASSIC")
				menuColor = COLOR.CYAN
				drawMenu(engine, playerID, receiver, "CHAINPOWER", if(newChainPower[playerID]) "FEVER" else "CLASSIC")

				receiver.drawMenuFont(engine, playerID, 0, 19, "PAGE 3/5", COLOR.YELLOW)
			} else if(menuCursor<32) {
				initMenu(COLOR.PINK, 24)
				drawMenu(engine, playerID, receiver, "USE MAP", GeneralUtil.getONorOFF(useMap[playerID]), "MAP SET", "${mapSet[playerID]}",
					"MAP NO.", if(mapNumber[playerID]<0) "RANDOM" else "${mapNumber[playerID]}/${mapMaxNo[playerID]-1}",
					"BIG DISP", GeneralUtil.getONorOFF(bigDisplay))
				menuColor = COLOR.COBALT
				drawMenu(engine, playerID, receiver, "BGM", "${BGM.values[bgmno]}", "SE", GeneralUtil.getONorOFF(enableSE[playerID]))
				menuColor = COLOR.GREEN
				drawMenu(engine, playerID, receiver, "LOAD", "${presetNumber[playerID]}", "SAVE", "${presetNumber[playerID]}")

				receiver.drawMenuFont(engine, playerID, 0, 19, "PAGE 4/5", COLOR.YELLOW)
			} else {
				receiver.drawMenuFont(engine, playerID, 0, 0, "ATTACK", COLOR.CYAN)
				var multiplier = (100*getAttackMultiplier(dropSet[playerID], dropMap[playerID])).toInt()
				if(multiplier>=100)
					receiver.drawMenuFont(engine, playerID, 2, 1, "$multiplier%", if(multiplier==100)
						COLOR.YELLOW
					else
						COLOR.GREEN)
				else
					receiver.drawMenuFont(engine, playerID, 3, 1, "$multiplier%", COLOR.RED)
				receiver.drawMenuFont(engine, playerID, 0, 2, "DEFEND", COLOR.CYAN)
				multiplier = (100*getDefendMultiplier(dropSet[playerID], dropMap[playerID])).toInt()
				if(multiplier>=100)
					receiver.drawMenuFont(engine, playerID, 2, 3, "$multiplier%",
						if(multiplier==100) COLOR.YELLOW else COLOR.RED)
				else
					receiver.drawMenuFont(engine, playerID, 3, 3, "$multiplier%", COLOR.GREEN)

				drawMenu(engine, playerID, receiver, 14, COLOR.CYAN, 32, "DROP SET", DROP_SET_NAMES[dropSet[playerID]],
					"DROP MAP", "${String.format("%2d", dropMap[playerID]+1)}/${String.format("%2d", DROP_PATTERNS[dropSet[playerID]].size)}")

				receiver.drawMenuFont(engine, playerID, 0, 19, "PAGE 5/5", COLOR.YELLOW)
			}
		} else
			receiver.drawMenuFont(engine, playerID, 3, 10, "WAIT", COLOR.YELLOW)
	}

	/* Called for initialization during Ready (before initialization) */
	override fun readyInit(engine:GameEngine, playerID:Int):Boolean {
		super.readyInit(engine, playerID)
		engine.blockColors = BLOCK_COLORS
		engine.numColors = 4
		dropPattern[playerID] = DROP_PATTERNS[dropSet[playerID]][dropMap[playerID]]
		attackMultiplier[playerID] = getAttackMultiplier(dropSet[playerID], dropMap[playerID])
		defendMultiplier[playerID] = getDefendMultiplier(dropSet[playerID], dropMap[playerID])
		return false
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

		if(engine.stat!=GameEngine.Status.MOVE&&engine.stat!=GameEngine.Status.RESULT&&engine.gameStarted)
			drawX(engine, playerID)

		if(!owner.engine[playerID].gameActive) return

		// Countdown Blocks
		var b:Block?
		var blockColor:Int
		val d = if(engine.displaysize==1) 2 else 1
		var str:String
		if(engine.field!=null&&engine.stat!=GameEngine.Status.RESULT&&engine.gameStarted)
			for(x in 0 until engine.field!!.width)
				for(y in 0 until engine.field!!.height) {
					b = engine.field!!.getBlock(x, y)
					if(!b!!.isEmpty&&b.countdown>0) {
						blockColor = b.secondaryColor

						val textColor:COLOR =
							when(blockColor) {
								Block.BLOCK_COLOR_BLUE -> COLOR.BLUE
								Block.BLOCK_COLOR_GREEN -> COLOR.GREEN
								Block.BLOCK_COLOR_RED -> COLOR.RED
								Block.BLOCK_COLOR_YELLOW -> COLOR.YELLOW
								else -> COLOR.WHITE
							}
						str = if(b.countdown>=10) "\u0084" else b.countdown.toString()
						receiver.drawMenuFont(engine, playerID, x*d, y*d, str, textColor, 1f*d)
					}
				}

		super.renderLast(engine, playerID)
	}

	override fun ptsToOjama(engine:GameEngine, playerID:Int, pts:Int, rate:Int):Int {
		var enemyID = 0
		if(playerID==0) enemyID = 1
		return ((pts.toDouble()*attackMultiplier[playerID]*defendMultiplier[enemyID]).toInt()+rate-1)/rate
	}

	override fun onLineClear(engine:GameEngine, playerID:Int):Boolean {
		if(engine.field==null||ojamaChecked[playerID]) return false

		ojamaChecked[playerID] = true
		//Turn cleared ojama into normal blocks
		for(x in 0 until engine.field!!.width)
			for(y in -1*engine.field!!.hiddenHeight until engine.field!!.height) {
				engine.field!!.getBlock(x, y)?.also{b->
				if(b.getAttribute(Block.ATTRIBUTE.GARBAGE)&&b.hard<4) {
					b.hard = 0
					b.cint = b.secondaryColor
					b.countdown = 0
					b.setAttribute(false, Block.ATTRIBUTE.GARBAGE)
				}}
			}
		return false
	}

	override fun lineClearEnd(engine:GameEngine, playerID:Int):Boolean {
		var enemyID = 0
		if(playerID==0) enemyID = 1
		if(ojamaAdd[enemyID]>0) {
			ojama[enemyID] += ojamaAdd[enemyID]
			ojamaAdd[enemyID] = 0
		}
		if(zenKeshi[playerID]&&zenKeshiType[playerID]==ZENKESHI_MODE_FEVER) {
			loadFeverMap(engine, playerID, 4)
			zenKeshi[playerID] = false
			zenKeshiDisplay[playerID] = 120
		}

		if(engine.field==null) return false

		var result = false
		//Decrement countdowns
		if(ojamaCountdown[playerID]!=10&&!countdownDecremented[playerID]) {
			countdownDecremented[playerID] = true
			for(y in engine.field!!.hiddenHeight*-1 until engine.field!!.height)
				for(x in 0 until engine.field!!.width) {
					val b = engine.field!!.getBlock(x, y) ?: continue
					if(b.countdown>1) b.countdown--
					else if(b.countdown==1) {
						b.countdown = 0
						b.hard = 0
						b.setAttribute(false, Block.ATTRIBUTE.GARBAGE)
						b.cint = b.secondaryColor
						result = true
					}
				}
			if(result) return true
		}
		//Drop garbage if needed.
		if(ojama[playerID]>0&&!ojamaDrop[playerID]&&(!cleared[playerID]||ojamaCounterMode[playerID]!=OJAMA_COUNTER_FEVER)) {
			ojamaDrop[playerID] = true
			val width = engine.field!!.width
			val hiddenHeight = engine.field!!.hiddenHeight
			val drop = minOf(ojama[playerID], maxAttack[playerID])
			ojama[playerID] -= drop
			engine.field!!.garbageDrop(engine, drop, false, 4, ojamaCountdown[playerID])
			engine.field!!.setAllSkin(engine.skin)
			var patternCol = 0
			for(x in 0 until engine.field!!.width) {
				if(patternCol>=dropPattern[enemyID].size) patternCol = 0
				var patternRow = 0
				for(y in (drop+width-1)/width-hiddenHeight downTo -1*hiddenHeight) {
					val b = engine.field!!.getBlock(x, y)
					if(b!!.getAttribute(Block.ATTRIBUTE.GARBAGE)&&b.secondaryColor==0) {
						if(patternRow>=dropPattern[enemyID][patternCol].size) patternRow = 0
						b.secondaryColor = dropPattern[enemyID][patternCol][patternRow]
						patternRow++
					}
				}
				patternCol++
			}
			return true
		}
		//Check for game over
		if(!engine.field!!.getBlockEmpty(2, 0)||dangerColumnDouble[playerID]&&!engine.field!!.getBlockEmpty(3, 0))
			engine.stat = GameEngine.Status.GAMEOVER
		return false
	}

	/* Called when saving replay */
	override fun saveReplay(engine:GameEngine, playerID:Int, prop:CustomProperties) {
		saveOtherSetting(engine, owner.replayProp)
		savePreset(engine, owner.replayProp, -1-playerID, "spf")

		if(useMap[playerID])fldBackup[playerID]?.let{saveMap(it, owner.replayProp, playerID)}

		owner.replayProp.setProperty("avalanchevs.version", version)
	}

	companion object {
		/** Current version */
		private const val CURRENT_VERSION = 0

		/** Block colors */
		private val BLOCK_COLORS =
			intArrayOf(Block.BLOCK_COLOR_RED, Block.BLOCK_COLOR_GREEN, Block.BLOCK_COLOR_BLUE, Block.BLOCK_COLOR_YELLOW)

		/** Names of drop values sets */
		private val DROP_SET_NAMES = arrayOf("CLASSIC", "REMIX", "SWORD", "S-MIRROR", "AVALANCHE", "A-MIRROR")

		private val DROP_PATTERNS =
			arrayOf(arrayOf(arrayOf(intArrayOf(2, 2, 2, 2), intArrayOf(5, 5, 5, 5), intArrayOf(7, 7, 7, 7), intArrayOf(4, 4, 4, 4)), arrayOf(intArrayOf(2, 2, 4, 4), intArrayOf(2, 2, 4, 4), intArrayOf(5, 5, 2, 2), intArrayOf(5, 5, 2, 2), intArrayOf(7, 7, 5, 5), intArrayOf(7, 7, 5, 5)), arrayOf(intArrayOf(5, 5, 5, 5), intArrayOf(2, 7, 2, 7), intArrayOf(2, 7, 2, 7), intArrayOf(2, 7, 2, 7), intArrayOf(2, 7, 2, 7), intArrayOf(4, 4, 4, 4)), arrayOf(intArrayOf(2, 5, 7, 4)), arrayOf(intArrayOf(7, 7, 4, 4), intArrayOf(4, 4, 7, 7), intArrayOf(2, 2, 5, 5), intArrayOf(2, 2, 5, 5), intArrayOf(4, 4, 7, 7), intArrayOf(7, 7, 4, 4)), arrayOf(intArrayOf(4, 7, 7, 5), intArrayOf(7, 7, 5, 5), intArrayOf(7, 5, 5, 2), intArrayOf(5, 5, 2, 2), intArrayOf(5, 2, 2, 4), intArrayOf(2, 2, 4, 4)), arrayOf(intArrayOf(2, 2, 5, 5), intArrayOf(4, 4, 5, 5), intArrayOf(2, 2, 5, 5), intArrayOf(4, 4, 7, 7), intArrayOf(2, 2, 7, 7), intArrayOf(4, 4, 7, 7)), arrayOf(intArrayOf(5, 5, 5, 5), intArrayOf(2, 2, 7, 7), intArrayOf(2, 2, 7, 7), intArrayOf(7, 7, 2, 2), intArrayOf(7, 7, 2, 2), intArrayOf(4, 4, 4, 4)), arrayOf(intArrayOf(5, 7, 4, 2), intArrayOf(2, 5, 7, 4), intArrayOf(4, 2, 5, 7), intArrayOf(7, 4, 2, 5)), arrayOf(intArrayOf(2, 5, 7, 4), intArrayOf(5, 7, 4, 2), intArrayOf(7, 4, 2, 5), intArrayOf(4, 2, 5, 7)), arrayOf(intArrayOf(2, 2, 2, 2))), arrayOf(arrayOf(intArrayOf(2, 2, 7, 2), intArrayOf(5, 5, 4, 5), intArrayOf(7, 7, 5, 7), intArrayOf(4, 4, 2, 4)), arrayOf(intArrayOf(2, 2, 4, 4), intArrayOf(2, 2, 4, 4), intArrayOf(5, 5, 2, 2), intArrayOf(5, 5, 2, 2), intArrayOf(7, 7, 5, 5), intArrayOf(7, 7, 5, 5)), arrayOf(intArrayOf(5, 5, 4, 4), intArrayOf(2, 7, 2, 7), intArrayOf(2, 7, 2, 7), intArrayOf(2, 7, 2, 7), intArrayOf(2, 7, 2, 7), intArrayOf(4, 4, 5, 5)), arrayOf(intArrayOf(2, 5, 7, 4)), arrayOf(intArrayOf(7, 7, 4, 4), intArrayOf(4, 4, 7, 7), intArrayOf(2, 5, 5, 5), intArrayOf(2, 2, 2, 5), intArrayOf(4, 4, 7, 7), intArrayOf(7, 7, 4, 4)), arrayOf(intArrayOf(7, 7, 7, 7), intArrayOf(5, 7, 4, 2), intArrayOf(7, 4, 2, 5), intArrayOf(4, 2, 5, 7), intArrayOf(2, 5, 7, 4), intArrayOf(5, 5, 5, 5)), arrayOf(intArrayOf(2, 2, 5, 5), intArrayOf(4, 4, 5, 5), intArrayOf(2, 2, 5, 5), intArrayOf(4, 4, 7, 7), intArrayOf(2, 2, 7, 7), intArrayOf(4, 4, 7, 7)), arrayOf(intArrayOf(5, 4, 5, 4), intArrayOf(2, 2, 2, 7), intArrayOf(2, 7, 7, 7), intArrayOf(7, 2, 2, 2), intArrayOf(7, 7, 7, 2), intArrayOf(4, 5, 4, 5)), arrayOf(intArrayOf(5, 7, 4, 2), intArrayOf(2, 5, 7, 4), intArrayOf(4, 2, 5, 7), intArrayOf(7, 4, 2, 5)), arrayOf(intArrayOf(2, 5, 7, 4), intArrayOf(5, 7, 4, 2), intArrayOf(7, 4, 2, 5), intArrayOf(4, 2, 5, 7)), arrayOf(intArrayOf(2, 2, 2, 2))), arrayOf(arrayOf(intArrayOf(2, 5, 5, 5), intArrayOf(5, 2, 2, 5), intArrayOf(5, 5, 2, 2), intArrayOf(4, 4, 7, 7), intArrayOf(4, 7, 7, 4), intArrayOf(7, 4, 4, 4)), arrayOf(intArrayOf(2, 2, 2, 5, 5, 5), intArrayOf(5, 3, 7, 5, 4, 5), intArrayOf(5, 5, 7, 7, 4, 4), intArrayOf(4, 4, 2, 4, 4, 7), intArrayOf(4, 2, 4, 4, 7, 4), intArrayOf(2, 4, 4, 7, 4, 4)), arrayOf(intArrayOf(4, 4, 5, 5, 7, 2), intArrayOf(4, 4, 5, 5, 7, 2), intArrayOf(5, 5, 7, 7, 7, 5), intArrayOf(5, 7, 7, 7, 4, 5), intArrayOf(7, 7, 2, 2, 5, 4), intArrayOf(7, 2, 2, 2, 5, 4)), arrayOf(intArrayOf(2, 2, 5, 4, 2, 7), intArrayOf(2, 7, 4, 5, 7, 2), intArrayOf(2, 7, 4, 4, 7, 7), intArrayOf(2, 7, 5, 5, 2, 2), intArrayOf(2, 7, 5, 4, 2, 7), intArrayOf(7, 7, 4, 5, 7, 2)), arrayOf(intArrayOf(2, 7, 7, 7, 7), intArrayOf(2, 7, 5, 7, 7), intArrayOf(2, 2, 5, 5, 5), intArrayOf(2, 2, 2, 5, 5), intArrayOf(2, 4, 2, 4, 4), intArrayOf(4, 4, 4, 4, 4)), arrayOf(intArrayOf(2, 2, 5, 5), intArrayOf(2, 7, 7, 5), intArrayOf(5, 7, 4, 4), intArrayOf(5, 5, 2, 4), intArrayOf(4, 2, 2, 7), intArrayOf(4, 4, 7, 7)), arrayOf(intArrayOf(2, 2, 5, 5), intArrayOf(2, 2, 5, 5), intArrayOf(5, 5, 7, 7), intArrayOf(5, 5, 7, 7), intArrayOf(7, 7, 4, 4), intArrayOf(7, 7, 4, 4)), arrayOf(intArrayOf(2, 2, 5, 4, 2, 7), intArrayOf(2, 2, 4, 5, 7, 2), intArrayOf(7, 7, 4, 5, 7, 2), intArrayOf(7, 7, 5, 4, 2, 7), intArrayOf(2, 2, 5, 4, 2, 7), intArrayOf(2, 2, 4, 5, 7, 2)), arrayOf(intArrayOf(7, 7, 4, 4, 7, 7), intArrayOf(7, 7, 7, 7, 5, 7), intArrayOf(2, 5, 2, 2, 5, 2), intArrayOf(2, 5, 2, 2, 5, 2), intArrayOf(4, 4, 4, 4, 5, 4), intArrayOf(4, 4, 7, 7, 4, 4)), arrayOf(intArrayOf(2, 5, 5, 5, 5, 4), intArrayOf(5, 2, 5, 5, 4, 4), intArrayOf(2, 2, 2, 2, 2, 2), intArrayOf(7, 7, 7, 7, 7, 7), intArrayOf(4, 7, 4, 4, 5, 5), intArrayOf(7, 4, 4, 4, 4, 5)), arrayOf(intArrayOf(2, 2, 5, 2, 2, 4), intArrayOf(2, 5, 5, 2, 5, 5), intArrayOf(5, 5, 5, 7, 7, 2), intArrayOf(7, 7, 7, 5, 5, 4), intArrayOf(4, 7, 7, 4, 7, 7), intArrayOf(4, 4, 7, 4, 4, 2)), arrayOf(intArrayOf(7, 7, 5, 5, 5, 5), intArrayOf(7, 2, 2, 5, 5, 7), intArrayOf(7, 2, 2, 4, 4, 7), intArrayOf(2, 7, 7, 4, 4, 2), intArrayOf(2, 7, 7, 5, 5, 2), intArrayOf(7, 7, 5, 5, 5, 5)), arrayOf(intArrayOf(7, 7, 5, 5), intArrayOf(7, 2, 5, 2), intArrayOf(5, 5, 5, 2), intArrayOf(4, 4, 4, 2), intArrayOf(7, 2, 4, 2), intArrayOf(7, 7, 4, 4)), arrayOf(intArrayOf(2, 2, 5, 5), intArrayOf(2, 7, 5, 5), intArrayOf(5, 5, 7, 7), intArrayOf(5, 5, 7, 7), intArrayOf(4, 7, 4, 4), intArrayOf(7, 7, 4, 4)), arrayOf(intArrayOf(7, 7, 5, 5, 5), intArrayOf(4, 7, 7, 7, 5), intArrayOf(5, 4, 4, 4, 4), intArrayOf(5, 2, 2, 2, 2), intArrayOf(2, 7, 7, 7, 5), intArrayOf(7, 7, 5, 5, 5)), arrayOf(intArrayOf(2, 2, 4), intArrayOf(2, 2, 2), intArrayOf(7, 7, 7), intArrayOf(7, 7, 7), intArrayOf(5, 5, 5), intArrayOf(5, 5, 4)), arrayOf(intArrayOf(7, 7, 7, 7), intArrayOf(7, 2, 2, 7), intArrayOf(2, 7, 5, 4), intArrayOf(4, 5, 7, 2), intArrayOf(5, 4, 4, 5), intArrayOf(5, 5, 5, 5))), arrayOf(arrayOf(intArrayOf(7, 4, 4, 4), intArrayOf(4, 7, 7, 4), intArrayOf(4, 4, 7, 7), intArrayOf(5, 5, 2, 2), intArrayOf(5, 2, 2, 5), intArrayOf(2, 5, 5, 5)), arrayOf(intArrayOf(2, 4, 4, 7, 4, 4), intArrayOf(4, 2, 4, 4, 7, 4), intArrayOf(4, 4, 2, 4, 4, 7), intArrayOf(5, 5, 7, 7, 4, 4), intArrayOf(5, 3, 7, 5, 4, 5), intArrayOf(2, 2, 2, 5, 5, 5)), arrayOf(intArrayOf(7, 2, 2, 2, 5, 4), intArrayOf(7, 7, 2, 2, 5, 4), intArrayOf(5, 7, 7, 7, 4, 5), intArrayOf(5, 5, 7, 7, 7, 5), intArrayOf(4, 4, 5, 5, 7, 2), intArrayOf(4, 4, 5, 5, 7, 2)), arrayOf(intArrayOf(7, 7, 4, 5, 7, 2), intArrayOf(2, 7, 5, 4, 2, 7), intArrayOf(2, 7, 5, 5, 2, 2), intArrayOf(2, 7, 4, 4, 7, 7), intArrayOf(2, 7, 4, 5, 7, 2), intArrayOf(2, 2, 5, 4, 2, 7)), arrayOf(intArrayOf(4, 4, 4, 4, 4), intArrayOf(2, 4, 2, 4, 4), intArrayOf(2, 2, 2, 5, 5), intArrayOf(2, 2, 5, 5, 5), intArrayOf(2, 7, 5, 7, 7), intArrayOf(2, 7, 7, 7, 7)), arrayOf(intArrayOf(4, 4, 7, 7), intArrayOf(4, 2, 2, 7), intArrayOf(5, 5, 2, 4), intArrayOf(5, 7, 4, 4), intArrayOf(2, 7, 7, 5), intArrayOf(2, 2, 5, 5)), arrayOf(intArrayOf(7, 7, 4, 4), intArrayOf(7, 7, 4, 4), intArrayOf(5, 5, 7, 7), intArrayOf(5, 5, 7, 7), intArrayOf(2, 2, 5, 5), intArrayOf(2, 2, 5, 5)), arrayOf(intArrayOf(2, 2, 4, 5, 7, 2), intArrayOf(2, 2, 5, 4, 2, 7), intArrayOf(7, 7, 5, 4, 2, 7), intArrayOf(7, 7, 4, 5, 7, 2), intArrayOf(2, 2, 4, 5, 7, 2), intArrayOf(2, 2, 5, 4, 2, 7)), arrayOf(intArrayOf(4, 4, 7, 7, 4, 4), intArrayOf(4, 4, 4, 4, 5, 4), intArrayOf(2, 5, 2, 2, 5, 2), intArrayOf(2, 5, 2, 2, 5, 2), intArrayOf(7, 7, 7, 7, 5, 7), intArrayOf(7, 7, 4, 4, 7, 7)), arrayOf(intArrayOf(7, 4, 4, 4, 4, 5), intArrayOf(4, 7, 4, 4, 5, 5), intArrayOf(7, 7, 7, 7, 7, 7), intArrayOf(2, 2, 2, 2, 2, 2), intArrayOf(5, 2, 5, 5, 4, 4), intArrayOf(2, 5, 5, 5, 5, 4)), arrayOf(intArrayOf(4, 4, 7, 4, 4, 2), intArrayOf(4, 7, 7, 4, 7, 7), intArrayOf(7, 7, 7, 5, 5, 4), intArrayOf(5, 5, 5, 7, 7, 2), intArrayOf(2, 5, 5, 2, 5, 5), intArrayOf(2, 2, 5, 2, 2, 4)), arrayOf(intArrayOf(7, 7, 5, 5, 5, 5), intArrayOf(2, 7, 7, 5, 5, 2), intArrayOf(2, 7, 7, 4, 4, 2), intArrayOf(7, 2, 2, 4, 4, 7), intArrayOf(7, 2, 2, 5, 5, 7), intArrayOf(7, 7, 5, 5, 5, 5)), arrayOf(intArrayOf(7, 7, 4, 4), intArrayOf(7, 2, 4, 2), intArrayOf(4, 4, 4, 2), intArrayOf(5, 5, 5, 2), intArrayOf(7, 2, 5, 2), intArrayOf(7, 7, 5, 5)), arrayOf(intArrayOf(7, 7, 4, 4), intArrayOf(4, 7, 4, 4), intArrayOf(5, 5, 7, 7), intArrayOf(5, 5, 7, 7), intArrayOf(2, 7, 5, 5), intArrayOf(2, 2, 5, 5)), arrayOf(intArrayOf(7, 7, 5, 5, 5), intArrayOf(2, 7, 7, 7, 5), intArrayOf(5, 2, 2, 2, 2), intArrayOf(5, 4, 4, 4, 4), intArrayOf(4, 7, 7, 7, 5), intArrayOf(7, 7, 5, 5, 5)), arrayOf(intArrayOf(5, 5, 4), intArrayOf(5, 5, 5), intArrayOf(7, 7, 7), intArrayOf(7, 7, 7), intArrayOf(2, 2, 2), intArrayOf(2, 2, 4)), arrayOf(intArrayOf(5, 5, 5, 5), intArrayOf(5, 4, 4, 5), intArrayOf(4, 5, 7, 2), intArrayOf(2, 7, 5, 4), intArrayOf(7, 2, 2, 7), intArrayOf(7, 7, 7, 7))), arrayOf(arrayOf(intArrayOf(5, 4, 4, 5, 5), intArrayOf(2, 5, 5, 2, 2), intArrayOf(4, 2, 2, 4, 4), intArrayOf(7, 4, 4, 7, 7), intArrayOf(5, 7, 7, 5, 5), intArrayOf(2, 5, 5, 2, 2)), arrayOf(intArrayOf(2, 7, 7, 7, 2), intArrayOf(5, 2, 2, 2, 5), intArrayOf(5, 4, 4, 4, 5), intArrayOf(4, 5, 5, 5, 4), intArrayOf(4, 7, 7, 7, 4), intArrayOf(7, 2, 2, 2, 7)), arrayOf(intArrayOf(2, 2, 5, 5, 5), intArrayOf(5, 7, 7, 2, 2), intArrayOf(7, 7, 2, 2, 5), intArrayOf(5, 4, 4, 7, 7), intArrayOf(4, 4, 7, 7, 5), intArrayOf(5, 5, 5, 4, 4)), arrayOf(intArrayOf(7, 2, 2, 5, 5), intArrayOf(4, 4, 5, 5, 2), intArrayOf(4, 7, 7, 2, 2), intArrayOf(7, 7, 4, 4, 5), intArrayOf(5, 4, 4, 7, 7), intArrayOf(2, 2, 7, 7, 4)), arrayOf(intArrayOf(7, 2, 7, 2, 2), intArrayOf(7, 4, 7, 7, 2), intArrayOf(5, 4, 4, 7, 4), intArrayOf(5, 5, 4, 5, 4), intArrayOf(2, 5, 2, 5, 5), intArrayOf(2, 7, 2, 2, 4)), arrayOf(intArrayOf(5, 5, 4, 2, 2), intArrayOf(5, 4, 4, 2, 7), intArrayOf(4, 2, 2, 7, 7), intArrayOf(4, 2, 7, 5, 5), intArrayOf(2, 7, 7, 5, 4), intArrayOf(7, 5, 5, 4, 4)), arrayOf(intArrayOf(7, 7, 4, 7, 7), intArrayOf(5, 5, 7, 5, 5), intArrayOf(2, 2, 5, 2, 2), intArrayOf(4, 4, 2, 4, 4)), arrayOf(intArrayOf(4, 4, 2, 2, 5), intArrayOf(2, 2, 5, 5, 7), intArrayOf(5, 5, 7, 7, 4), intArrayOf(7, 7, 4, 4, 2)), arrayOf(intArrayOf(5, 5, 5, 2, 4), intArrayOf(7, 7, 7, 5, 2), intArrayOf(4, 4, 4, 7, 5), intArrayOf(2, 2, 2, 4, 7)), arrayOf(intArrayOf(4, 4, 4, 5, 7), intArrayOf(2, 2, 2, 7, 4), intArrayOf(5, 5, 5, 4, 2), intArrayOf(7, 7, 7, 2, 5)), arrayOf(intArrayOf(4, 2, 5, 5, 5), intArrayOf(7, 4, 2, 2, 2), intArrayOf(5, 7, 4, 4, 4), intArrayOf(2, 5, 7, 7, 7))), arrayOf(arrayOf(intArrayOf(2, 5, 5, 2, 2), intArrayOf(5, 7, 7, 5, 5), intArrayOf(7, 4, 4, 7, 7), intArrayOf(4, 2, 2, 4, 4), intArrayOf(2, 5, 5, 2, 2), intArrayOf(5, 4, 4, 5, 5)), arrayOf(intArrayOf(7, 2, 2, 2, 7), intArrayOf(4, 7, 7, 7, 4), intArrayOf(4, 5, 5, 5, 4), intArrayOf(5, 4, 4, 4, 5), intArrayOf(5, 2, 2, 2, 5), intArrayOf(2, 7, 7, 7, 2)), arrayOf(intArrayOf(5, 5, 5, 4, 4), intArrayOf(4, 4, 7, 7, 5), intArrayOf(5, 4, 4, 7, 7), intArrayOf(7, 7, 2, 2, 5), intArrayOf(5, 7, 7, 2, 2), intArrayOf(2, 2, 5, 5, 5)), arrayOf(intArrayOf(2, 2, 7, 7, 4), intArrayOf(5, 4, 4, 7, 7), intArrayOf(7, 7, 4, 4, 5), intArrayOf(4, 7, 7, 2, 2), intArrayOf(4, 4, 5, 5, 2), intArrayOf(7, 2, 2, 5, 5)), arrayOf(intArrayOf(2, 7, 2, 2, 4), intArrayOf(2, 5, 2, 5, 5), intArrayOf(5, 5, 4, 5, 4), intArrayOf(5, 4, 4, 7, 4), intArrayOf(7, 4, 7, 7, 2), intArrayOf(7, 2, 7, 2, 2)), arrayOf(intArrayOf(7, 5, 5, 4, 4), intArrayOf(2, 7, 7, 5, 4), intArrayOf(4, 2, 7, 5, 5), intArrayOf(4, 2, 2, 7, 7), intArrayOf(5, 4, 4, 2, 7), intArrayOf(5, 5, 4, 2, 2)), arrayOf(intArrayOf(5, 5, 7, 5, 5), intArrayOf(7, 7, 4, 7, 7), intArrayOf(4, 4, 2, 4, 4), intArrayOf(2, 2, 5, 2, 2)), arrayOf(intArrayOf(2, 2, 5, 5, 7), intArrayOf(4, 4, 2, 2, 5), intArrayOf(7, 7, 4, 4, 2), intArrayOf(5, 5, 7, 7, 4)), arrayOf(intArrayOf(7, 7, 7, 5, 2), intArrayOf(5, 5, 5, 2, 4), intArrayOf(2, 2, 2, 4, 7), intArrayOf(4, 4, 4, 7, 5)), arrayOf(intArrayOf(2, 2, 2, 7, 4), intArrayOf(4, 4, 4, 5, 7), intArrayOf(7, 7, 7, 2, 5), intArrayOf(5, 5, 5, 4, 2)), arrayOf(intArrayOf(7, 4, 2, 2, 2), intArrayOf(4, 2, 5, 5, 5), intArrayOf(2, 5, 7, 7, 7), intArrayOf(5, 7, 4, 4, 4))))
		private val DROP_PATTERNS_ATTACK_MULTIPLIERS =
			arrayOf(doubleArrayOf(1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 0.7, 0.7, 1.0), doubleArrayOf(1.0, 1.2, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 0.85, 1.0))
		private val DROP_PATTERNS_DEFEND_MULTIPLIERS =
			arrayOf(doubleArrayOf(1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0), doubleArrayOf(1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.2, 1.0, 1.0))

		fun getAttackMultiplier(set:Int, map:Int):Double {
			return try {
				DROP_PATTERNS_ATTACK_MULTIPLIERS[set][map]
			} catch(e:ArrayIndexOutOfBoundsException) {
				1.0
			}

		}

		fun getDefendMultiplier(set:Int, map:Int):Double {
			return try {
				DROP_PATTERNS_DEFEND_MULTIPLIERS[set][map]
			} catch(e:ArrayIndexOutOfBoundsException) {
				1.0
			}

		}
	}
}
