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

/** AVALANCHE-SPF VS-BATTLE mode (Release Candidate 1) */
class AvalancheVSSPF:AvalancheVSDummyMode() {
	/** Version */
	private var version = 0

	/** Settings for starting countdown for ojama blocks */
	private var ojamaCountdown = IntArray(MAX_PLAYERS)

	/** Drop patterns */
	private val dropPattern:MutableList<List<List<Int>>> = MutableList(MAX_PLAYERS) {emptyList()}

	/** Drop values set selected */
	private var dropSet = IntArray(MAX_PLAYERS)

	/** Drop values selected */
	private var dropMap = IntArray(MAX_PLAYERS)

	/** Drop multipliers */
	private var attackMultiplier = DoubleArray(MAX_PLAYERS)
	private var defendMultiplier = DoubleArray(MAX_PLAYERS)

	/** Flag set when counters have been decremented */
	private var countdownDecremented = BooleanArray(MAX_PLAYERS)

	/** Flag set when cleared ojama have been turned into normal blocks */
	private var ojamaChecked = BooleanArray(MAX_PLAYERS)

	/* Mode name */
	override val name = "AVALANCHE-SPF VS-BATTLE (BETA)"
	override val gameIntensity = 1
	/* Mode initialization */
	override fun modeInit(manager:GameManager) {
		super.modeInit(manager)
		ojamaCountdown = IntArray(MAX_PLAYERS)
		dropSet = IntArray(MAX_PLAYERS)
		dropMap = IntArray(MAX_PLAYERS)
		dropPattern.fill(emptyList())
		attackMultiplier = DoubleArray(MAX_PLAYERS)
		defendMultiplier = DoubleArray(MAX_PLAYERS)
		countdownDecremented = BooleanArray(MAX_PLAYERS)
		ojamaChecked = BooleanArray(MAX_PLAYERS)
	}

	/** Load settings into [engine] from [prop] not related to speeds */
	private fun loadOtherSetting(engine:GameEngine, prop:CustomProperties) {
		super.loadOtherSetting(engine, prop, "spf")
		val playerID = engine.playerID
		ojamaHard[playerID] = 4
		ojamaRate[playerID] = prop.getProperty("avalanchevsspf.ojamaRate.p$playerID", 120)
		ojamaCountdown[playerID] = prop.getProperty("avalanchevsspf.ojamaCountdown.p$playerID", 3)
		dropSet[playerID] = prop.getProperty("avalanchevsspf.dropSet.p$playerID", 4)
		dropMap[playerID] = prop.getProperty("avalanchevsspf.dropMap.p$playerID", 0)
	}

	/** Save settings from [engine] into [prop] not related to speeds */
	private fun saveOtherSetting(engine:GameEngine, prop:CustomProperties) {
		super.saveOtherSetting(engine, prop, "spf")
		val playerID = engine.playerID
		prop.setProperty("avalanchevsspf.ojamaCountdown.p$playerID", ojamaCountdown[playerID])
		prop.setProperty("avalanchevsspf.dropSet.p$playerID", dropSet[playerID])
		prop.setProperty("avalanchevsspf.dropMap.p$playerID", dropMap[playerID])
	}

	private fun loadDropMapPreview(engine:GameEngine, pattern:List<List<Int>>?) {
		if(pattern==null)
			engine.field.reset()
		else {
			engine.createFieldIfNeeded()
			engine.field.reset()
			var patternCol = 0
			val maxHeight = engine.field.height-1
			for(x in 0..<engine.field.width) {
				if(patternCol>=pattern.size) patternCol = 0
				for(patternRow in pattern[patternCol].indices) {
					engine.field.setBlockColor(x, maxHeight-patternRow, pattern[patternCol][patternRow])
					engine.field.getBlock(x, maxHeight-patternRow)?.let {blk ->
						blk.setAttribute(true, Block.ATTRIBUTE.VISIBLE)
						blk.setAttribute(true, Block.ATTRIBUTE.OUTLINE)
					}
				}
				patternCol++
			}
			engine.field.setAllSkin(engine.skin)
		}
	}

	/* Initialization for each player */
	override fun playerInit(engine:GameEngine) {
		super.playerInit(engine)
		val playerID = engine.playerID
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
	override fun onSetting(engine:GameEngine):Boolean {
		val playerID = engine.playerID
		// Menu
		if(!engine.owner.replayMode&&engine.statc[4]==0) {
			// Configuration changes
			// Up
			if(engine.ctrl.isMenuRepeatKey(Controller.BUTTON_UP)) {
				menuCursor--
				if(menuCursor<0) {
					menuCursor = 33
					loadDropMapPreview(engine, DROP_PATTERNS[dropSet[playerID]][dropMap[playerID]])
				} else if(menuCursor==31) engine.field.reset()
				engine.playSE("cursor")
			}
			// Down
			if(engine.ctrl.isMenuRepeatKey(Controller.BUTTON_DOWN)) {
				menuCursor++
				if(menuCursor>33) {
					menuCursor = 0
					engine.field.reset()
				} else if(menuCursor==32) loadDropMapPreview(engine, DROP_PATTERNS[dropSet[playerID]][dropMap[playerID]])
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
							hurryUpSeconds[playerID] += change*m/10
						else
							hurryUpSeconds[playerID] += change
						if(hurryUpSeconds[playerID]<0) hurryUpSeconds[playerID] = 300
						if(hurryUpSeconds[playerID]>300) hurryUpSeconds[playerID] = 0
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
						if(!useMap[playerID]) engine.field.reset() else
							loadMapPreview(engine, if(mapNumber[playerID]<0) 0 else mapNumber[playerID], true)
					}
					25 -> {
						mapSet[playerID] += change
						if(mapSet[playerID]<0) mapSet[playerID] = 99
						if(mapSet[playerID]>99) mapSet[playerID] = 0
						if(useMap[playerID]) {
							mapNumber[playerID] = -1
							loadMapPreview(engine, if(mapNumber[playerID]<0) 0 else mapNumber[playerID], true)
						}
					}
					26 -> if(useMap[playerID]) {
						mapNumber[playerID] += change
						if(mapNumber[playerID]<-1) mapNumber[playerID] = mapMaxNo[playerID]-1
						if(mapNumber[playerID]>mapMaxNo[playerID]-1) mapNumber[playerID] = -1
						loadMapPreview(engine, if(mapNumber[playerID]<0) 0 else mapNumber[playerID], true)
					} else
						mapNumber[playerID] = -1
					27 -> bigDisplay = !bigDisplay
					28 -> bgmId = rangeCursor(bgmId+change, 0, BGM.count-1)
					29 -> enableSE[playerID] = !enableSE[playerID]
					30, 31 -> presetNumber[playerID] = rangeCursor(presetNumber[playerID]+change, 0, 99)
					32 -> {
						dropSet[playerID] += change
						if(dropSet[playerID]<0) dropSet[playerID] = DROP_PATTERNS.size-1
						if(dropSet[playerID]>=DROP_PATTERNS.size) dropSet[playerID] = 0
						if(dropMap[playerID]>=DROP_PATTERNS[dropSet[playerID]].size) dropMap[playerID] = 0
						loadDropMapPreview(engine, DROP_PATTERNS[dropSet[playerID]][dropMap[playerID]])
					}
					33 -> {
						dropMap[playerID] += change
						if(dropMap[playerID]<0) dropMap[playerID] = DROP_PATTERNS[dropSet[playerID]].size-1
						if(dropMap[playerID]>=DROP_PATTERNS[dropSet[playerID]].size) dropMap[playerID] = 0
						loadDropMapPreview(engine, DROP_PATTERNS[dropSet[playerID]][dropMap[playerID]])
					}
				}
			}

			// 決定
			if(menuTime<5) menuTime++ else if(engine.ctrl.isPush(Controller.BUTTON_A)) {
				engine.playSE("decide")

				when(menuCursor) {
					30 -> loadPreset(engine, owner.modeConfig, presetNumber[playerID], "spf")
					31 -> {
						savePreset(engine, owner.modeConfig, presetNumber[playerID], "spf")
						owner.saveModeConfig()
					}
					else -> {
						saveOtherSetting(engine, owner.modeConfig)
						savePreset(engine, owner.modeConfig, -1-playerID, "spf")
						owner.saveModeConfig()
						engine.statc[4] = 1
					}
				}
			}

			// Cancel
			if(engine.ctrl.isPush(Controller.BUTTON_B)) engine.quitFlag = true

			// プレビュー用Map読み込み
			if(useMap[playerID]&&menuTime==0)
				loadMapPreview(engine, if(mapNumber[playerID]<0) 0 else mapNumber[playerID], true)

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

			if(menuTime>=300)
				engine.statc[4] = 1
			else if(menuTime==240) {
				menuCursor = 32
				loadDropMapPreview(engine, DROP_PATTERNS[dropSet[playerID]][dropMap[playerID]])
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

	override fun onMove(engine:GameEngine):Boolean {
		val pid = engine.playerID
		cleared[pid] = false
		ojamaDrop[pid] = false
		countdownDecremented[pid] = false
		return false
	}

	public override fun onClear(engine:GameEngine) {
		ojamaChecked[engine.playerID] = false
	}

	/* Setting screen drawing */
	override fun renderSetting(engine:GameEngine) {
		if(engine.statc[4]==0) {
			if(menuCursor<9) {
				drawMenuSpeeds(engine, receiver, 0, COLOR.ORANGE, 0)
				drawMenu(engine, receiver, "FALL DELAY" to engine.cascadeDelay, "CLEAR DELAY" to engine.cascadeClearDelay)
				receiver.drawMenuFont(engine, 0, 19, "PAGE 1/5", COLOR.YELLOW)
			} else {
				val pid = engine.playerID
				if(menuCursor<17) {
					drawMenu(
						engine, receiver, 0, COLOR.CYAN, 9,
						"COUNTER" to OJAMA_COUNTER_STRING[ojamaCounterMode[pid]],
						"MAX ATTACK" to maxAttack[pid],
						"MIN CHAIN" to rensaShibari[pid],
						"CLEAR SIZE" to
							engine.colorClearSize.toString(),
						"OJAMA RATE" to ojamaRate[pid],
						"HURRYUP" to
							if(hurryUpSeconds[pid]==0) "NONE" else "${hurryUpSeconds[pid]}SEC",
						"X COLUMN" to if(dangerColumnDouble[pid]) "3 AND 4" else "3 ONLY",
						"X SHOW" to dangerColumnShowX[pid]
					)

					receiver.drawMenuFont(engine, 0, 19, "PAGE 2/5", COLOR.YELLOW)
				} else if(menuCursor<24) {
					drawMenu(
						engine, receiver, 0, COLOR.CYAN, 17,
						"COUNTDOWN" to if(ojamaCountdown[pid]==10) "NONE" else "${ojamaCountdown[pid]}",
						"ZENKESHI" to ZENKESHI_TYPE_NAMES[zenKeshiType[pid]]
					)
					drawMenu(
						engine, receiver,
						if(zenKeshiType[pid]==ZENKESHI_MODE_FEVER) COLOR.PURPLE else COLOR.WHITE,
						"F-MAP SET" to FEVER_MAPS[feverMapSet[pid]].uppercase()
					)

					drawMenu(
						engine, receiver, COLOR.COBALT,
						"OUTLINE" to OUTLINE_TYPE_NAMES[outlineType[pid]],
						"SHOW CHAIN" to CHAIN_DISPLAY_NAMES[chainDisplayType[pid]],
						"FALL ANIM" to if(cascadeSlow[pid]) "FEVER" else "CLASSIC"
					)

					drawMenu(engine, receiver, COLOR.CYAN, "CHAINPOWER" to if(newChainPower[pid]) "FEVER" else "CLASSIC")

					receiver.drawMenuFont(engine, 0, 19, "PAGE 3/5", COLOR.YELLOW)
				} else if(menuCursor<32) {
					initMenu()
					drawMenu(
						engine, receiver, 0, COLOR.PINK, 24,
						"USE MAP" to useMap[pid],
						"MAP SET" to mapSet[pid],
						"MAP NO." to if(mapNumber[pid]<0) "RANDOM" else "${mapNumber[pid]}/${mapMaxNo[pid]-1}",
						"BIG DISP" to bigDisplay
					)

					drawMenu(engine, receiver, COLOR.COBALT, "BGM" to BGM.values[bgmId], "SE" to enableSE[pid])

					drawMenu(engine, receiver, COLOR.GREEN, "LOAD" to presetNumber[pid], "SAVE" to presetNumber[pid])

					receiver.drawMenuFont(engine, 0, 19, "PAGE 4/5", COLOR.YELLOW)
				} else {
					receiver.drawMenuFont(engine, 0, 0, "ATTACK", COLOR.CYAN)
					var multiplier = (100*getAttackMultiplier(dropSet[pid], dropMap[pid])).toInt()
					if(multiplier>=100)
						receiver.drawMenuFont(engine, 2, 1, "$multiplier%", if(multiplier==100) COLOR.YELLOW else COLOR.GREEN)
					else
						receiver.drawMenuFont(engine, 3, 1, "$multiplier%", COLOR.RED)
					receiver.drawMenuFont(engine, 0, 2, "DEFEND", COLOR.CYAN)
					multiplier = (100*getDefendMultiplier(dropSet[pid], dropMap[pid])).toInt()
					if(multiplier>=100)
						receiver.drawMenuFont(
							engine, 2, 3, "$multiplier%", if(multiplier==100) COLOR.YELLOW else COLOR.RED
						)
					else
						receiver.drawMenuFont(engine, 3, 3, "$multiplier%", COLOR.GREEN)

					drawMenu(
						engine,
						receiver,
						14,
						COLOR.CYAN,
						32,
						"DROP SET" to DROP_SET_NAMES[dropSet[pid]],
						"DROP MAP" to "${"%2d".format(dropMap[pid]+1)}/${
							"%2d".format(DROP_PATTERNS[dropSet[pid]].size)
						}"
					)

					receiver.drawMenuFont(engine, 0, 19, "PAGE 5/5", COLOR.YELLOW)
				}
			}
		} else
			receiver.drawMenuFont(engine, 3, 10, "WAIT", COLOR.YELLOW)
	}

	/* Called for initialization during Ready (before initialization) */
	override fun readyInit(engine:GameEngine):Boolean {
		val pid = engine.playerID
		super.readyInit(engine)
		engine.blockColors = BLOCK_COLORS
		engine.numColors = 4
		dropPattern[pid] = DROP_PATTERNS[dropSet[pid]][dropMap[pid]]
		attackMultiplier[pid] = getAttackMultiplier(dropSet[pid], dropMap[pid])
		defendMultiplier[pid] = getDefendMultiplier(dropSet[pid], dropMap[pid])
		return false
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
			receiver.drawDirectFont(fldPosX+4, fldPosY+440, "%12d".format(score[pid]), playerColor)
			receiver.drawDirectFont(fldPosX+4, fldPosY+456, "%12s".format(strScoreMultiplier), playerColor)
		} else if(engine.gameStarted) {
			receiver.drawDirectFont(fldPosX-28, fldPosY+248, "%8d".format(score[pid]), playerColor)
			receiver.drawDirectFont(fldPosX-28, fldPosY+264, "%8s".format(strScoreMultiplier), playerColor)
		}

		if(engine.stat!=GameEngine.Status.MOVE&&engine.stat!=GameEngine.Status.RESULT&&engine.gameStarted)
			drawX(engine)

		if(!owner.engine[pid].gameActive) return

		// Countdown Blocks

		val d = if(engine.displaySize==1) 2 else 1
		if(engine.stat!=GameEngine.Status.RESULT&&engine.gameStarted)
			for(x in 0..<engine.field.width)
				for(y in 0..<engine.field.height)
					engine.field.getBlock(x, y)?.let {b ->
						if(!b.isEmpty&&b.countdown>0) {
							val textColor:COLOR =
								when(b.color) {
									Block.COLOR.BLUE -> COLOR.BLUE
									Block.COLOR.GREEN -> COLOR.GREEN
									Block.COLOR.RED -> COLOR.RED
									Block.COLOR.YELLOW -> COLOR.YELLOW
									else -> COLOR.WHITE
								}
							receiver.drawMenuFont(
								engine, x*d, y*d, if(b.countdown>=10) "\u0084" else "${b.countdown}", textColor, 1f*d
							)
						}
					}


		super.renderLast(engine)
	}

	override fun ptsToOjama(engine:GameEngine, pts:Int, rate:Int):Int {
		val enemyID = if(engine.playerID==0) 1 else 0
		return ((pts.toDouble()*attackMultiplier[engine.playerID]*defendMultiplier[enemyID]).toInt()+rate-1)/rate
	}

	override fun onLineClear(engine:GameEngine):Boolean {
		if(engine.field.isEmpty||ojamaChecked[engine.playerID]) return false

		ojamaChecked[engine.playerID] = true
		//Turn cleared ojama into normal blocks
		for(x in 0..<engine.field.width)
			for(y in -1*engine.field.hiddenHeight..<engine.field.height)
				engine.field.getBlock(x, y)?.also {b ->
					if(b.getAttribute(Block.ATTRIBUTE.GARBAGE)&&b.hard<4) {
						b.hard = 0
						b.color = b.secondaryColor
						b.countdown = 0
						b.setAttribute(false, Block.ATTRIBUTE.GARBAGE)
					}
				}
		return false
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

		var result = false
		//Decrement countdowns
		if(ojamaCountdown[pid]!=10&&!countdownDecremented[pid]) {
			countdownDecremented[pid] = true
			for(y in engine.field.hiddenHeight*-1..<engine.field.height)
				for(x in 0..<engine.field.width) {
					val b = engine.field.getBlock(x, y) ?: continue
					if(b.countdown>1) b.countdown--
					else if(b.countdown==1) {
						b.countdown = 0
						b.hard = 0
						b.setAttribute(false, Block.ATTRIBUTE.GARBAGE)
						b.color = b.secondaryColor
						result = true
					}
				}
			if(result) return true
		}
		//Drop garbage if needed.
		if(ojama[pid]>0&&!ojamaDrop[pid]&&(!cleared[pid]||ojamaCounterMode[pid]!=OJAMA_COUNTER_FEVER)) {
			ojamaDrop[pid] = true
			val width = engine.field.width
			val hiddenHeight = engine.field.hiddenHeight
			val drop = minOf(ojama[pid], maxAttack[pid])
			ojama[pid] -= drop
			engine.field.garbageDrop(engine, drop, false, 4, ojamaCountdown[pid])
			engine.field.setAllSkin(engine.skin)
			var patternCol = 0
			for(x in 0..<engine.field.width) {
				if(patternCol>=dropPattern[enemyID].size) patternCol = 0
				var patternRow = 0
				for(y in (drop+width-1)/width-hiddenHeight downTo -1*hiddenHeight) {
					engine.field.getBlock(x, y)?.let {b ->
						if(b.getAttribute(Block.ATTRIBUTE.GARBAGE)&&!b.secondaryColor.color) {
							if(patternRow>=dropPattern[enemyID][patternCol].size) patternRow = 0
							Block.intToColor(dropPattern[enemyID][patternCol][patternRow]).first?.let {
								b.secondaryColor = it
							}
							patternRow++
						}
					}
				}
				patternCol++
			}
			return true
		}
		//Check for game over
		if(!engine.field.getBlockEmpty(2, 0)||dangerColumnDouble[pid]&&!engine.field.getBlockEmpty(3, 0))
			engine.stat = GameEngine.Status.GAMEOVER
		return false
	}

	/* Called when saving replay */
	override fun saveReplay(engine:GameEngine, prop:CustomProperties):Boolean {
		saveOtherSetting(engine, owner.replayProp)
		val pid = engine.playerID
		savePreset(engine, owner.replayProp, -1-pid, "spf")

		if(useMap[pid]) fldBackup[pid]?.let {saveMap(it, owner.replayProp, pid)}

		owner.replayProp.setProperty("avalanchevs.version", version)
		return false
	}

	companion object {
		/** Current version */
		private const val CURRENT_VERSION = 0

		/** Block colors */
		private val BLOCK_COLORS =
			listOf(Block.COLOR.RED, Block.COLOR.GREEN, Block.COLOR.BLUE, Block.COLOR.YELLOW)

		/** Names of drop values sets */
		private val DROP_SET_NAMES = listOf("CLASSIC", "REMIX", "SWORD", "S-MIRROR", "AVALANCHE", "A-MIRROR")

		private val DROP_PATTERNS =
			listOf(
				listOf(
					listOf(listOf(2, 2, 2, 2), listOf(5, 5, 5, 5), listOf(7, 7, 7, 7), listOf(4, 4, 4, 4)),
					listOf(
						listOf(2, 2, 4, 4), listOf(2, 2, 4, 4), listOf(5, 5, 2, 2), listOf(5, 5, 2, 2),
						listOf(7, 7, 5, 5), listOf(7, 7, 5, 5)
					),
					listOf(
						listOf(5, 5, 5, 5), listOf(2, 7, 2, 7), listOf(2, 7, 2, 7), listOf(2, 7, 2, 7),
						listOf(2, 7, 2, 7), listOf(4, 4, 4, 4)
					), listOf(listOf(2, 5, 7, 4)),
					listOf(
						listOf(7, 7, 4, 4), listOf(4, 4, 7, 7), listOf(2, 2, 5, 5), listOf(2, 2, 5, 5),
						listOf(4, 4, 7, 7), listOf(7, 7, 4, 4)
					),
					listOf(
						listOf(4, 7, 7, 5), listOf(7, 7, 5, 5), listOf(7, 5, 5, 2), listOf(5, 5, 2, 2),
						listOf(5, 2, 2, 4), listOf(2, 2, 4, 4)
					),
					listOf(
						listOf(2, 2, 5, 5), listOf(4, 4, 5, 5), listOf(2, 2, 5, 5), listOf(4, 4, 7, 7),
						listOf(2, 2, 7, 7), listOf(4, 4, 7, 7)
					),
					listOf(
						listOf(5, 5, 5, 5), listOf(2, 2, 7, 7), listOf(2, 2, 7, 7), listOf(7, 7, 2, 2),
						listOf(7, 7, 2, 2), listOf(4, 4, 4, 4)
					),
					listOf(listOf(5, 7, 4, 2), listOf(2, 5, 7, 4), listOf(4, 2, 5, 7), listOf(7, 4, 2, 5)),
					listOf(listOf(2, 5, 7, 4), listOf(5, 7, 4, 2), listOf(7, 4, 2, 5), listOf(4, 2, 5, 7)),
					listOf(listOf(2, 2, 2, 2))
				),
				listOf(
					listOf(listOf(2, 2, 7, 2), listOf(5, 5, 4, 5), listOf(7, 7, 5, 7), listOf(4, 4, 2, 4)),
					listOf(
						listOf(2, 2, 4, 4), listOf(2, 2, 4, 4), listOf(5, 5, 2, 2), listOf(5, 5, 2, 2),
						listOf(7, 7, 5, 5), listOf(7, 7, 5, 5)
					),
					listOf(
						listOf(5, 5, 4, 4), listOf(2, 7, 2, 7), listOf(2, 7, 2, 7), listOf(2, 7, 2, 7),
						listOf(2, 7, 2, 7), listOf(4, 4, 5, 5)
					), listOf(listOf(2, 5, 7, 4)),
					listOf(
						listOf(7, 7, 4, 4), listOf(4, 4, 7, 7), listOf(2, 5, 5, 5), listOf(2, 2, 2, 5),
						listOf(4, 4, 7, 7), listOf(7, 7, 4, 4)
					),
					listOf(
						listOf(7, 7, 7, 7), listOf(5, 7, 4, 2), listOf(7, 4, 2, 5), listOf(4, 2, 5, 7),
						listOf(2, 5, 7, 4), listOf(5, 5, 5, 5)
					),
					listOf(
						listOf(2, 2, 5, 5), listOf(4, 4, 5, 5), listOf(2, 2, 5, 5), listOf(4, 4, 7, 7),
						listOf(2, 2, 7, 7), listOf(4, 4, 7, 7)
					),
					listOf(
						listOf(5, 4, 5, 4), listOf(2, 2, 2, 7), listOf(2, 7, 7, 7), listOf(7, 2, 2, 2),
						listOf(7, 7, 7, 2), listOf(4, 5, 4, 5)
					),
					listOf(listOf(5, 7, 4, 2), listOf(2, 5, 7, 4), listOf(4, 2, 5, 7), listOf(7, 4, 2, 5)),
					listOf(listOf(2, 5, 7, 4), listOf(5, 7, 4, 2), listOf(7, 4, 2, 5), listOf(4, 2, 5, 7)),
					listOf(listOf(2, 2, 2, 2))
				), listOf(
					listOf(
						listOf(2, 5, 5, 5), listOf(5, 2, 2, 5), listOf(5, 5, 2, 2), listOf(4, 4, 7, 7),
						listOf(4, 7, 7, 4), listOf(7, 4, 4, 4)
					),
					listOf(
						listOf(2, 2, 2, 5, 5, 5), listOf(5, 3, 7, 5, 4, 5), listOf(5, 5, 7, 7, 4, 4),
						listOf(4, 4, 2, 4, 4, 7), listOf(4, 2, 4, 4, 7, 4), listOf(2, 4, 4, 7, 4, 4)
					),
					listOf(
						listOf(4, 4, 5, 5, 7, 2), listOf(4, 4, 5, 5, 7, 2), listOf(5, 5, 7, 7, 7, 5),
						listOf(5, 7, 7, 7, 4, 5), listOf(7, 7, 2, 2, 5, 4), listOf(7, 2, 2, 2, 5, 4)
					),
					listOf(
						listOf(2, 2, 5, 4, 2, 7), listOf(2, 7, 4, 5, 7, 2), listOf(2, 7, 4, 4, 7, 7),
						listOf(2, 7, 5, 5, 2, 2), listOf(2, 7, 5, 4, 2, 7), listOf(7, 7, 4, 5, 7, 2)
					),
					listOf(
						listOf(2, 7, 7, 7, 7), listOf(2, 7, 5, 7, 7), listOf(2, 2, 5, 5, 5), listOf(2, 2, 2, 5, 5),
						listOf(2, 4, 2, 4, 4), listOf(4, 4, 4, 4, 4)
					),
					listOf(
						listOf(2, 2, 5, 5), listOf(2, 7, 7, 5), listOf(5, 7, 4, 4), listOf(5, 5, 2, 4),
						listOf(4, 2, 2, 7), listOf(4, 4, 7, 7)
					),
					listOf(
						listOf(2, 2, 5, 5), listOf(2, 2, 5, 5), listOf(5, 5, 7, 7), listOf(5, 5, 7, 7),
						listOf(7, 7, 4, 4), listOf(7, 7, 4, 4)
					),
					listOf(
						listOf(2, 2, 5, 4, 2, 7), listOf(2, 2, 4, 5, 7, 2), listOf(7, 7, 4, 5, 7, 2),
						listOf(7, 7, 5, 4, 2, 7), listOf(2, 2, 5, 4, 2, 7), listOf(2, 2, 4, 5, 7, 2)
					),
					listOf(
						listOf(7, 7, 4, 4, 7, 7), listOf(7, 7, 7, 7, 5, 7), listOf(2, 5, 2, 2, 5, 2),
						listOf(2, 5, 2, 2, 5, 2), listOf(4, 4, 4, 4, 5, 4), listOf(4, 4, 7, 7, 4, 4)
					),
					listOf(
						listOf(2, 5, 5, 5, 5, 4), listOf(5, 2, 5, 5, 4, 4), listOf(2, 2, 2, 2, 2, 2),
						listOf(7, 7, 7, 7, 7, 7), listOf(4, 7, 4, 4, 5, 5), listOf(7, 4, 4, 4, 4, 5)
					),
					listOf(
						listOf(2, 2, 5, 2, 2, 4), listOf(2, 5, 5, 2, 5, 5), listOf(5, 5, 5, 7, 7, 2),
						listOf(7, 7, 7, 5, 5, 4), listOf(4, 7, 7, 4, 7, 7), listOf(4, 4, 7, 4, 4, 2)
					),
					listOf(
						listOf(7, 7, 5, 5, 5, 5), listOf(7, 2, 2, 5, 5, 7), listOf(7, 2, 2, 4, 4, 7),
						listOf(2, 7, 7, 4, 4, 2), listOf(2, 7, 7, 5, 5, 2), listOf(7, 7, 5, 5, 5, 5)
					),
					listOf(
						listOf(7, 7, 5, 5), listOf(7, 2, 5, 2), listOf(5, 5, 5, 2), listOf(4, 4, 4, 2),
						listOf(7, 2, 4, 2), listOf(7, 7, 4, 4)
					),
					listOf(
						listOf(2, 2, 5, 5), listOf(2, 7, 5, 5), listOf(5, 5, 7, 7), listOf(5, 5, 7, 7),
						listOf(4, 7, 4, 4), listOf(7, 7, 4, 4)
					),
					listOf(
						listOf(7, 7, 5, 5, 5), listOf(4, 7, 7, 7, 5), listOf(5, 4, 4, 4, 4), listOf(5, 2, 2, 2, 2),
						listOf(2, 7, 7, 7, 5), listOf(7, 7, 5, 5, 5)
					),
					listOf(
						listOf(2, 2, 4), listOf(2, 2, 2), listOf(7, 7, 7), listOf(7, 7, 7), listOf(5, 5, 5),
						listOf(5, 5, 4)
					),
					listOf(
						listOf(7, 7, 7, 7), listOf(7, 2, 2, 7), listOf(2, 7, 5, 4), listOf(4, 5, 7, 2),
						listOf(5, 4, 4, 5), listOf(5, 5, 5, 5)
					)
				), listOf(
					listOf(
						listOf(7, 4, 4, 4), listOf(4, 7, 7, 4), listOf(4, 4, 7, 7), listOf(5, 5, 2, 2),
						listOf(5, 2, 2, 5), listOf(2, 5, 5, 5)
					),
					listOf(
						listOf(2, 4, 4, 7, 4, 4), listOf(4, 2, 4, 4, 7, 4), listOf(4, 4, 2, 4, 4, 7),
						listOf(5, 5, 7, 7, 4, 4), listOf(5, 3, 7, 5, 4, 5), listOf(2, 2, 2, 5, 5, 5)
					),
					listOf(
						listOf(7, 2, 2, 2, 5, 4), listOf(7, 7, 2, 2, 5, 4), listOf(5, 7, 7, 7, 4, 5),
						listOf(5, 5, 7, 7, 7, 5), listOf(4, 4, 5, 5, 7, 2), listOf(4, 4, 5, 5, 7, 2)
					),
					listOf(
						listOf(7, 7, 4, 5, 7, 2), listOf(2, 7, 5, 4, 2, 7), listOf(2, 7, 5, 5, 2, 2),
						listOf(2, 7, 4, 4, 7, 7), listOf(2, 7, 4, 5, 7, 2), listOf(2, 2, 5, 4, 2, 7)
					),
					listOf(
						listOf(4, 4, 4, 4, 4), listOf(2, 4, 2, 4, 4), listOf(2, 2, 2, 5, 5), listOf(2, 2, 5, 5, 5),
						listOf(2, 7, 5, 7, 7), listOf(2, 7, 7, 7, 7)
					),
					listOf(
						listOf(4, 4, 7, 7), listOf(4, 2, 2, 7), listOf(5, 5, 2, 4), listOf(5, 7, 4, 4),
						listOf(2, 7, 7, 5), listOf(2, 2, 5, 5)
					),
					listOf(
						listOf(7, 7, 4, 4), listOf(7, 7, 4, 4), listOf(5, 5, 7, 7), listOf(5, 5, 7, 7),
						listOf(2, 2, 5, 5), listOf(2, 2, 5, 5)
					),
					listOf(
						listOf(2, 2, 4, 5, 7, 2), listOf(2, 2, 5, 4, 2, 7), listOf(7, 7, 5, 4, 2, 7),
						listOf(7, 7, 4, 5, 7, 2), listOf(2, 2, 4, 5, 7, 2), listOf(2, 2, 5, 4, 2, 7)
					),
					listOf(
						listOf(4, 4, 7, 7, 4, 4), listOf(4, 4, 4, 4, 5, 4), listOf(2, 5, 2, 2, 5, 2),
						listOf(2, 5, 2, 2, 5, 2), listOf(7, 7, 7, 7, 5, 7), listOf(7, 7, 4, 4, 7, 7)
					),
					listOf(
						listOf(7, 4, 4, 4, 4, 5), listOf(4, 7, 4, 4, 5, 5), listOf(7, 7, 7, 7, 7, 7),
						listOf(2, 2, 2, 2, 2, 2), listOf(5, 2, 5, 5, 4, 4), listOf(2, 5, 5, 5, 5, 4)
					),
					listOf(
						listOf(4, 4, 7, 4, 4, 2), listOf(4, 7, 7, 4, 7, 7), listOf(7, 7, 7, 5, 5, 4),
						listOf(5, 5, 5, 7, 7, 2), listOf(2, 5, 5, 2, 5, 5), listOf(2, 2, 5, 2, 2, 4)
					),
					listOf(
						listOf(7, 7, 5, 5, 5, 5), listOf(2, 7, 7, 5, 5, 2), listOf(2, 7, 7, 4, 4, 2),
						listOf(7, 2, 2, 4, 4, 7), listOf(7, 2, 2, 5, 5, 7), listOf(7, 7, 5, 5, 5, 5)
					),
					listOf(
						listOf(7, 7, 4, 4), listOf(7, 2, 4, 2), listOf(4, 4, 4, 2), listOf(5, 5, 5, 2),
						listOf(7, 2, 5, 2), listOf(7, 7, 5, 5)
					),
					listOf(
						listOf(7, 7, 4, 4), listOf(4, 7, 4, 4), listOf(5, 5, 7, 7), listOf(5, 5, 7, 7),
						listOf(2, 7, 5, 5), listOf(2, 2, 5, 5)
					),
					listOf(
						listOf(7, 7, 5, 5, 5), listOf(2, 7, 7, 7, 5), listOf(5, 2, 2, 2, 2), listOf(5, 4, 4, 4, 4),
						listOf(4, 7, 7, 7, 5), listOf(7, 7, 5, 5, 5)
					),
					listOf(
						listOf(5, 5, 4), listOf(5, 5, 5), listOf(7, 7, 7), listOf(7, 7, 7), listOf(2, 2, 2),
						listOf(2, 2, 4)
					),
					listOf(
						listOf(5, 5, 5, 5), listOf(5, 4, 4, 5), listOf(4, 5, 7, 2), listOf(2, 7, 5, 4),
						listOf(7, 2, 2, 7), listOf(7, 7, 7, 7)
					)
				), listOf(
					listOf(
						listOf(5, 4, 4, 5, 5), listOf(2, 5, 5, 2, 2), listOf(4, 2, 2, 4, 4), listOf(7, 4, 4, 7, 7),
						listOf(5, 7, 7, 5, 5), listOf(2, 5, 5, 2, 2)
					),
					listOf(
						listOf(2, 7, 7, 7, 2), listOf(5, 2, 2, 2, 5), listOf(5, 4, 4, 4, 5), listOf(4, 5, 5, 5, 4),
						listOf(4, 7, 7, 7, 4), listOf(7, 2, 2, 2, 7)
					),
					listOf(
						listOf(2, 2, 5, 5, 5), listOf(5, 7, 7, 2, 2), listOf(7, 7, 2, 2, 5), listOf(5, 4, 4, 7, 7),
						listOf(4, 4, 7, 7, 5), listOf(5, 5, 5, 4, 4)
					),
					listOf(
						listOf(7, 2, 2, 5, 5), listOf(4, 4, 5, 5, 2), listOf(4, 7, 7, 2, 2), listOf(7, 7, 4, 4, 5),
						listOf(5, 4, 4, 7, 7), listOf(2, 2, 7, 7, 4)
					),
					listOf(
						listOf(7, 2, 7, 2, 2), listOf(7, 4, 7, 7, 2), listOf(5, 4, 4, 7, 4), listOf(5, 5, 4, 5, 4),
						listOf(2, 5, 2, 5, 5), listOf(2, 7, 2, 2, 4)
					),
					listOf(
						listOf(5, 5, 4, 2, 2), listOf(5, 4, 4, 2, 7), listOf(4, 2, 2, 7, 7), listOf(4, 2, 7, 5, 5),
						listOf(2, 7, 7, 5, 4), listOf(7, 5, 5, 4, 4)
					),
					listOf(listOf(7, 7, 4, 7, 7), listOf(5, 5, 7, 5, 5), listOf(2, 2, 5, 2, 2), listOf(4, 4, 2, 4, 4)),
					listOf(listOf(4, 4, 2, 2, 5), listOf(2, 2, 5, 5, 7), listOf(5, 5, 7, 7, 4), listOf(7, 7, 4, 4, 2)),
					listOf(listOf(5, 5, 5, 2, 4), listOf(7, 7, 7, 5, 2), listOf(4, 4, 4, 7, 5), listOf(2, 2, 2, 4, 7)),
					listOf(listOf(4, 4, 4, 5, 7), listOf(2, 2, 2, 7, 4), listOf(5, 5, 5, 4, 2), listOf(7, 7, 7, 2, 5)),
					listOf(listOf(4, 2, 5, 5, 5), listOf(7, 4, 2, 2, 2), listOf(5, 7, 4, 4, 4), listOf(2, 5, 7, 7, 7))
				),
				listOf(
					listOf(
						listOf(2, 5, 5, 2, 2), listOf(5, 7, 7, 5, 5), listOf(7, 4, 4, 7, 7), listOf(4, 2, 2, 4, 4),
						listOf(2, 5, 5, 2, 2), listOf(5, 4, 4, 5, 5)
					),
					listOf(
						listOf(7, 2, 2, 2, 7), listOf(4, 7, 7, 7, 4), listOf(4, 5, 5, 5, 4), listOf(5, 4, 4, 4, 5),
						listOf(5, 2, 2, 2, 5), listOf(2, 7, 7, 7, 2)
					),
					listOf(
						listOf(5, 5, 5, 4, 4), listOf(4, 4, 7, 7, 5), listOf(5, 4, 4, 7, 7), listOf(7, 7, 2, 2, 5),
						listOf(5, 7, 7, 2, 2), listOf(2, 2, 5, 5, 5)
					),
					listOf(
						listOf(2, 2, 7, 7, 4), listOf(5, 4, 4, 7, 7), listOf(7, 7, 4, 4, 5), listOf(4, 7, 7, 2, 2),
						listOf(4, 4, 5, 5, 2), listOf(7, 2, 2, 5, 5)
					),
					listOf(
						listOf(2, 7, 2, 2, 4), listOf(2, 5, 2, 5, 5), listOf(5, 5, 4, 5, 4), listOf(5, 4, 4, 7, 4),
						listOf(7, 4, 7, 7, 2), listOf(7, 2, 7, 2, 2)
					),
					listOf(
						listOf(7, 5, 5, 4, 4), listOf(2, 7, 7, 5, 4), listOf(4, 2, 7, 5, 5), listOf(4, 2, 2, 7, 7),
						listOf(5, 4, 4, 2, 7), listOf(5, 5, 4, 2, 2)
					),
					listOf(listOf(5, 5, 7, 5, 5), listOf(7, 7, 4, 7, 7), listOf(4, 4, 2, 4, 4), listOf(2, 2, 5, 2, 2)),
					listOf(listOf(2, 2, 5, 5, 7), listOf(4, 4, 2, 2, 5), listOf(7, 7, 4, 4, 2), listOf(5, 5, 7, 7, 4)),
					listOf(listOf(7, 7, 7, 5, 2), listOf(5, 5, 5, 2, 4), listOf(2, 2, 2, 4, 7), listOf(4, 4, 4, 7, 5)),
					listOf(listOf(2, 2, 2, 7, 4), listOf(4, 4, 4, 5, 7), listOf(7, 7, 7, 2, 5), listOf(5, 5, 5, 4, 2)),
					listOf(listOf(7, 4, 2, 2, 2), listOf(4, 2, 5, 5, 5), listOf(2, 5, 7, 7, 7), listOf(5, 7, 4, 4, 4))
				)
			)
		private val DROP_PATTERNS_ATTACK_MULTIPLIERS =
			listOf(
				doubleArrayOf(1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 0.7, 0.7, 1.0),
				doubleArrayOf(1.0, 1.2, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 0.85, 1.0)
			)
		private val DROP_PATTERNS_DEFEND_MULTIPLIERS =
			listOf(
				doubleArrayOf(1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0),
				doubleArrayOf(1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.2, 1.0, 1.0)
			)

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
