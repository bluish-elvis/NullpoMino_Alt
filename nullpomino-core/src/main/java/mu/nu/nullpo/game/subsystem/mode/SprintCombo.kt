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
import mu.nu.nullpo.game.event.EventReceiver
import mu.nu.nullpo.game.net.NetUtil
import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.util.CustomProperties
import mu.nu.nullpo.util.GeneralUtil

/** COMBO RACE Mode */
class SprintCombo:NetDummyMode() {

	/** EventReceiver object (This receives many game events, can also be used
	 * for drawing the fonts.) */

	/** Elapsed time from last line clear (lastscore is displayed to screen
	 * until this reaches to 120) */
	private var scgettime:Int = 0

	/** Most recent scoring event type */
	private var lastevent:Int = 0

	/** Most recent scoring eventInB2BIf it&#39;s the casetrue */
	private var lastb2b:Boolean = false

	/** Most recent scoring eventInCombocount */
	private var lastcombo:Int = 0

	/** Most recent scoring eventPeace inID */
	private var lastpiece:Int = 0

	/** BGM number */
	private var bgmno:Int = 0

	/** Big */
	private var big:Boolean = false

	/** HindranceLinescount type (0=5,1=10,2=18) */
	private var goaltype:Int = 0

	/** Current version */
	private var version:Int = 0

	/** Last preset number used */
	private var presetNumber:Int = 0

	/** Current round's ranking rank */
	private var rankingRank:Int = 0

	/** Rankings' times */
	private var rankingTime:Array<IntArray> = Array(GOAL_TABLE.size) {IntArray(RANKING_MAX)}

	/** Rankings' Combo */
	private var rankingCombo:Array<IntArray> = Array(GOAL_TABLE.size) {IntArray(RANKING_MAX)}

	/** Shape type */
	private var shapetype:Int = 0

	/** Stack colour */
	private var stackColour:Int = 0

	/** Column number of combo well (starts from 1) */
	private var comboColumn:Int = 0

	/** Width of combo well */
	private var comboWidth:Int = 0

	/** Height difference between ceiling and stack (negative number lowers the
	 * stack height) */
	private var ceilingAdjust:Int = 0

	/** Piece spawns above field if true */
	private var spawnAboveField:Boolean = false

	/** Number of remaining stack lines that need to be added when lines are
	 * cleared */
	private var remainStack:Int = 0

	/** Next section lines */
	private var nextseclines:Int = 0

	/** Returns the name of this mode */
	override val name:String
		get() = "COMBO RACE"

	/** This function will be called when the game enters the main game
	 * screen. */
	override fun playerInit(engine:GameEngine, playerID:Int) {
		super.playerInit(engine, playerID)

		scgettime = 0
		lastevent = EVENT_NONE
		lastb2b = false
		lastcombo = 0
		lastpiece = 0
		bgmno = 0
		big = false
		goaltype = 0
		shapetype = 1
		presetNumber = 0
		remainStack = 0
		stackColour = 0
		nextseclines = 10

		rankingRank = -1
		rankingTime = Array(GOAL_TABLE.size) {IntArray(RANKING_MAX)}
		rankingCombo = Array(GOAL_TABLE.size) {IntArray(RANKING_MAX)}

		engine.framecolor = GameEngine.FRAME_COLOR_RED

		netPlayerInit(engine, playerID)

		if(!engine.owner.replayMode) {
			version = CURRENT_VERSION
			presetNumber = engine.owner.modeConfig.getProperty("comborace.presetNumber", 0)
			loadPreset(engine, engine.owner.modeConfig, -1)
			loadRanking(owner.modeConfig, engine.ruleopt.strRuleName)
		} else {
			version = engine.owner.replayProp.getProperty("comborace.version", 0)
			presetNumber = 0
			loadPreset(engine, engine.owner.replayProp, -1)

			// NET: Load name
			netPlayerName = engine.owner.replayProp.getProperty(playerID.toString()+".net.netPlayerName", "")
		}
	}

	/** Load the settings */
	private fun loadPreset(engine:GameEngine, prop:CustomProperties, preset:Int) {
		engine.speed.gravity = prop.getProperty("comborace.gravity.$preset", 4)
		engine.speed.denominator = prop.getProperty("comborace.denominator.$preset", 256)
		engine.speed.are = prop.getProperty("comborace.are.$preset", 10)
		engine.speed.areLine = prop.getProperty("comborace.areLine.$preset", 5)
		engine.speed.lineDelay = prop.getProperty("comborace. lineDelay.$preset", 20)
		engine.speed.lockDelay = prop.getProperty("comborace.lockDelay.$preset", 30)
		engine.speed.das = prop.getProperty("comborace.das.$preset", 14)
		bgmno = prop.getProperty("comborace.bgmno.$preset", 0)
		big = prop.getProperty("comborace.big.$preset", false)
		goaltype = prop.getProperty("comborace.goaltype.$preset", 1)
		shapetype = prop.getProperty("comborace.shapetype.$preset", 1)
		comboWidth = prop.getProperty("comborace.comboWidth.$preset", 4)
		comboColumn = prop.getProperty("comborace.comboColumn.$preset", 4)
		ceilingAdjust = prop.getProperty("comborace.ceilingAdjust.$preset", -2)
		spawnAboveField = prop.getProperty("comborace.spawnAboveField.$preset", true)
	}

	/** Save the settings */
	private fun savePreset(engine:GameEngine, prop:CustomProperties, preset:Int) {
		prop.setProperty("comborace.gravity.$preset", engine.speed.gravity)
		prop.setProperty("comborace.denominator.$preset", engine.speed.denominator)
		prop.setProperty("comborace.are.$preset", engine.speed.are)
		prop.setProperty("comborace.areLine.$preset", engine.speed.areLine)
		prop.setProperty("comborace.lineDelay.$preset", engine.speed.lineDelay)
		prop.setProperty("comborace.lockDelay.$preset", engine.speed.lockDelay)
		prop.setProperty("comborace.das.$preset", engine.speed.das)
		prop.setProperty("comborace.bgmno.$preset", bgmno)
		prop.setProperty("comborace.big.$preset", big)
		prop.setProperty("comborace.goaltype.$preset", goaltype)
		prop.setProperty("comborace.shapetype.$preset", shapetype)
		prop.setProperty("comborace.comboWidth.$preset", comboWidth)
		prop.setProperty("comborace.comboColumn.$preset", comboColumn)
		prop.setProperty("comborace.ceilingAdjust.$preset", ceilingAdjust)
		prop.setProperty("comborace.spawnAboveField.$preset", spawnAboveField)
	}

	/** Main routine for game setup screen */
	override fun onSetting(engine:GameEngine, playerID:Int):Boolean {
		// NET: Net Ranking
		if(netIsNetRankingDisplayMode)
			netOnUpdateNetPlayRanking(engine, goaltype)
		else if(!engine.owner.replayMode) {
			// Configuration changes
			val change = updateCursor(engine, 15)

			if(change!=0) {
				engine.playSE("change")

				var m = 1
				if(engine.ctrl!!.isPress(Controller.BUTTON_E)) m = 100
				if(engine.ctrl!!.isPress(Controller.BUTTON_F)) m = 1000

				when(menuCursor) {
					0 -> {
						goaltype += change
						if(goaltype<0) goaltype = GOAL_TABLE.size-1
						if(goaltype>GOAL_TABLE.size-1) goaltype = 0
					}
					1 -> {
						shapetype += change
						if(shapetype<0) shapetype = SHAPETYPE_MAX-1
						if(shapetype>SHAPETYPE_MAX-1) shapetype = 0
					}
					2 -> {
						comboColumn += change
						if(comboColumn>10) comboColumn = 1
						if(comboColumn<1) comboColumn = 10
						while(comboColumn+comboWidth-1>10)
							comboWidth--
					}
					3 -> {
						comboWidth += change
						if(comboWidth>10) comboWidth = 1
						if(comboWidth<1) comboWidth = 10
						while(comboColumn+comboWidth-1>10)
							comboColumn--
					}
					4 -> {
						ceilingAdjust += change
						if(ceilingAdjust>10) ceilingAdjust = -10
						if(ceilingAdjust<-10) ceilingAdjust = 10
					}
					5 -> spawnAboveField = !spawnAboveField
					6 -> {
						engine.speed.gravity += change*m
						if(engine.speed.gravity<-1) engine.speed.gravity = 99999
						if(engine.speed.gravity>99999) engine.speed.gravity = -1
					}
					7 -> {
						engine.speed.denominator += change*m
						if(engine.speed.denominator<-1) engine.speed.denominator = 99999
						if(engine.speed.denominator>99999) engine.speed.denominator = -1
					}
					8 -> {
						engine.speed.are += change
						if(engine.speed.are<0) engine.speed.are = 99
						if(engine.speed.are>99) engine.speed.are = 0
					}
					9 -> {
						engine.speed.areLine += change
						if(engine.speed.areLine<0) engine.speed.areLine = 99
						if(engine.speed.areLine>99) engine.speed.areLine = 0
					}
					10 -> {
						engine.speed.lineDelay += change
						if(engine.speed.lineDelay<0) engine.speed.lineDelay = 99
						if(engine.speed.lineDelay>99) engine.speed.lineDelay = 0
					}
					11 -> {
						engine.speed.lockDelay += change
						if(engine.speed.lockDelay<0) engine.speed.lockDelay = 99
						if(engine.speed.lockDelay>99) engine.speed.lockDelay = 0
					}
					12 -> {
						engine.speed.das += change
						if(engine.speed.das<0) engine.speed.das = 99
						if(engine.speed.das>99) engine.speed.das = 0
					}
					13 -> {
						bgmno += change
						if(bgmno<0) bgmno = BGMStatus.count
						if(bgmno>BGMStatus.count) bgmno = 0
					}
					14, 15 -> {
						presetNumber += change
						if(presetNumber<0) presetNumber = 99
						if(presetNumber>99) presetNumber = 0
					}
				}

				// NET: Signal options change
				if(netIsNetPlay&&netNumSpectators>0) netSendOptions(engine)
			}

			// Confirm
			if(engine.ctrl!!.isPush(Controller.BUTTON_A)&&menuTime>=5) {
				engine.playSE("decide")

				if(menuCursor==14) {
					loadPreset(engine, owner.modeConfig, presetNumber)

					// NET: Signal options change
					if(netIsNetPlay&&netNumSpectators>0) netSendOptions(engine)
				} else if(menuCursor==15) {
					savePreset(engine, owner.modeConfig, presetNumber)
					receiver.saveModeConfig(owner.modeConfig)
				} else {
					owner.modeConfig.setProperty("comborace.presetNumber", presetNumber)
					savePreset(engine, owner.modeConfig, -1)
					receiver.saveModeConfig(owner.modeConfig)

					// NET: Signal start of the game
					if(netIsNetPlay) netLobby!!.netPlayerClient!!.send("start1p\n")

					return false
				}
			}

			// Cancel
			if(engine.ctrl!!.isPush(Controller.BUTTON_B)&&!netIsNetPlay) engine.quitflag = true

			// NET: Netplay Ranking
			if(engine.ctrl!!.isPush(Controller.BUTTON_D)&&netIsNetPlay
				&&netIsNetRankingViewOK(engine))
				netEnterNetPlayRankingScreen(engine, playerID, goaltype)

			menuTime++
		} else {
			menuTime++
			menuCursor = -1

			return menuTime<60
		}// Replay

		return true
	}

	/** Renders game setup screen */
	override fun renderSetting(engine:GameEngine, playerID:Int) {
		if(netIsNetRankingDisplayMode)
		// NET: Netplay Ranking
			netOnRenderNetPlayRanking(engine, playerID, receiver)
		else {
			val strSpawn = if(spawnAboveField) "ABOVE" else "BELOW"

			drawMenu(engine, playerID, receiver, 0, EventReceiver.COLOR.BLUE, 0,
				"GOAL", if(GOAL_TABLE[goaltype]==-1) "ENDLESS" else GOAL_TABLE[goaltype].toString())
			drawMenu(engine, playerID, receiver, 2, if(comboWidth==4)
				EventReceiver.COLOR.BLUE
			else
				EventReceiver.COLOR.WHITE, 1,
				"STARTSHAPE", SHAPE_NAME_TABLE[shapetype])
			menuColor = EventReceiver.COLOR.BLUE
			drawMenuCompact(engine, playerID, receiver,
				"COLUMN", comboColumn.toString(), "WIDTH", comboWidth.toString(), "CEILING", ceilingAdjust.toString())
			drawMenu(engine, playerID, receiver, "PIECESPAWN", strSpawn)

			drawMenuSpeeds(engine, playerID, receiver, engine.speed.gravity, engine.speed.denominator,
				engine.speed.are, engine.speed.areLine, engine.speed.lineDelay, engine.speed.lockDelay, engine.speed.das)
			drawMenuBGM(engine, playerID, receiver, bgmno)
			if(!engine.owner.replayMode) {
				menuColor = EventReceiver.COLOR.GREEN
				drawMenuCompact(engine, playerID, receiver, "LOAD", presetNumber.toString(), "SAVE", presetNumber.toString())
			}
		}
	}

	/** Ready */
	override fun onReady(engine:GameEngine, playerID:Int):Boolean {
		if(engine.statc[0]==0) {
			engine.createFieldIfNeeded()
			engine.meterColor = GameEngine.METER_COLOR_GREEN
			engine.meterValue = if(GOAL_TABLE[goaltype]==-1) 0 else receiver.getMeterMax(engine)

			if(!netIsWatch) {
				fillStack(engine, goaltype)

				// NET: Send field
				if(netNumSpectators>0) netSendField(engine)
			}
		}
		return false
	}

	/** This function will be called before the game actually begins (after
	 * Ready&Go screen disappears) */
	override fun startGame(engine:GameEngine, playerID:Int) {
		if(version<=0) engine.big = big
		if(netIsWatch)
			owner.bgmStatus.bgm = BGMStatus.BGM.SILENT
		else
			owner.bgmStatus.bgm = BGMStatus[bgmno]
		engine.comboType = GameEngine.COMBO_TYPE_NORMAL
		engine.tspinEnable = true
		engine.tspinAllowKick = true
		engine.ruleopt.pieceEnterAboveField = spawnAboveField
	}

	/** Fill the playfield with stack
	 * @param engine GameEngine
	 * @param height Stack height level number
	 */
	private fun fillStack(engine:GameEngine, height:Int) {
		val w = engine.field!!.width
		val h = engine.field!!.height
		val stackHeight:Int

		/* set initial stack height and remaining stack lines
 * depending on the goal lines and ceiling height adjustment */
		if(GOAL_TABLE[height]>h+ceilingAdjust||GOAL_TABLE[height]==-1) {
			stackHeight = h+ceilingAdjust
			remainStack = GOAL_TABLE[height]-h-ceilingAdjust
		} else {
			stackHeight = GOAL_TABLE[height]
			remainStack = 0
		}

		// fill stack from the bottom to the top
		for(y in h-1 downTo h-stackHeight) {
			for(x in 0 until w)
				if(x<comboColumn-1||x>comboColumn-2+comboWidth)
					engine.field!!.setBlock(x, y, Block(STACK_COLOUR_TABLE[stackColour%STACK_COLOUR_TABLE.size], engine.skin, Block.BLOCK_ATTRIBUTE_VISIBLE or Block.BLOCK_ATTRIBUTE_GARBAGE))
			stackColour++
		}

		// insert starting shape
		if(comboWidth==4)
			for(i in 0..11)
				if(SHAPE_TABLE[shapetype][i]==1)
					engine.field!!.setBlock(i%4+comboColumn-1, h-1-i/4, Block(SHAPE_COLOUR_TABLE[shapetype], engine.skin, Block.BLOCK_ATTRIBUTE_VISIBLE or Block.BLOCK_ATTRIBUTE_GARBAGE))
	}

	/** Renders HUD (leaderboard or game statistics) */
	override fun renderLast(engine:GameEngine, playerID:Int) {
		if(owner.menuOnly) return

		receiver.drawScoreFont(engine, playerID, 0, 0, "COMBO RACE", EventReceiver.COLOR.RED)
		if(GOAL_TABLE[goaltype]==-1)
			receiver.drawScoreFont(engine, playerID, 0, 1, "(ENDLESS GAME)", EventReceiver.COLOR.WHITE)
		else
			receiver.drawScoreFont(engine, playerID, 0, 1, "("+GOAL_TABLE[goaltype]
				+" LINES GAME)", EventReceiver.COLOR.WHITE)

		if(engine.stat==GameEngine.Status.SETTING||engine.stat==GameEngine.Status.RESULT&&!owner.replayMode) {
			if(!owner.replayMode&&!big&&engine.ai==null) {
				receiver.drawScoreFont(engine, playerID, 3, 3, "COMBO TIME", EventReceiver.COLOR.BLUE)

				for(i in 0 until RANKING_MAX) {
					receiver.drawScoreGrade(engine, playerID, 0, 4+i, String.format("%2d", i+1), EventReceiver.COLOR.YELLOW)
					receiver.drawScoreNum(engine, playerID, 3, 4+i, rankingCombo[goaltype][i].toString(), rankingRank==i)
					receiver.drawScoreNum(engine, playerID, 9, 4+i, GeneralUtil.getTime(rankingTime[goaltype][i].toFloat()), rankingRank==i)
				}
			}
		} else {
			receiver.drawScoreFont(engine, playerID, 0, 3, "LINE", EventReceiver.COLOR.BLUE)
			receiver.drawScoreNum(engine, playerID, 0, 4, engine.statistics.lines.toString())

			receiver.drawScoreFont(engine, playerID, 0, 6, "PIECE", EventReceiver.COLOR.BLUE)
			receiver.drawScoreNum(engine, playerID, 0, 7, engine.statistics.totalPieceLocked.toString())

			receiver.drawScoreFont(engine, playerID, 0, 9, "LINE/MIN", EventReceiver.COLOR.BLUE)
			receiver.drawScoreNum(engine, playerID, 0, 10, engine.statistics.lpm.toString())

			receiver.drawScoreFont(engine, playerID, 0, 12, "PIECE/SEC", EventReceiver.COLOR.BLUE)
			receiver.drawScoreNum(engine, playerID, 0, 13, engine.statistics.pps.toString())

			receiver.drawScoreFont(engine, playerID, 0, 15, "TIME", EventReceiver.COLOR.BLUE)
			receiver.drawScoreNum(engine, playerID, 0, 16, GeneralUtil.getTime(engine.statistics.time.toFloat()))

			if(lastevent!=EVENT_NONE&&scgettime<120) renderLineAlert(engine, playerID, receiver)
		}

		super.renderLast(engine, playerID)
	}

	/** Calculates line-clear score
	 * (This function will be called even if no lines are cleared) */
	override fun calcScore(engine:GameEngine, playerID:Int, lines:Int) {
		//  Attack
		if(lines>0) {
			scgettime = 0

			if(engine.tspin) {
				// T-Spin 1 line
				if(lines==1) {
					lastevent = if(engine.tspinmini)
						EVENT_TSPIN_SINGLE_MINI
					else
						EVENT_TSPIN_SINGLE
				} else if(lines==2) {
					lastevent = if(engine.tspinmini&&engine.useAllSpinBonus)
						EVENT_TSPIN_DOUBLE_MINI
					else
						EVENT_TSPIN_DOUBLE
				} else if(lines>=3) lastevent = EVENT_TSPIN_TRIPLE// T-Spin 3 lines
				// T-Spin 2 lines
			} else if(lines==1)
				lastevent = EVENT_SINGLE
			else if(lines==2)
				lastevent = EVENT_DOUBLE
			else if(lines==3)
				lastevent = EVENT_TRIPLE
			else if(lines>=4) lastevent = EVENT_FOUR

			// B2B
			lastb2b = engine.b2b

			// Combo
			lastcombo = engine.combo

			lastpiece = engine.nowPieceObject!!.id

			// add any remaining stack lines
			if(GOAL_TABLE[goaltype]==-1) remainStack = Integer.MAX_VALUE
			var tmplines = 1
			while(tmplines<=lines&&remainStack>0) {
				for(x in 0 until engine.field!!.width)
					if(x<comboColumn-1||x>comboColumn-2+comboWidth)
						engine.field!!.setBlock(x, -ceilingAdjust-tmplines, Block(STACK_COLOUR_TABLE[stackColour%STACK_COLOUR_TABLE.size], engine.skin, Block.BLOCK_ATTRIBUTE_VISIBLE or Block.BLOCK_ATTRIBUTE_GARBAGE))
				stackColour++
				tmplines++
				remainStack--
			}

			if(GOAL_TABLE[goaltype]==-1) {
				val meterMax = receiver.getMeterMax(engine)
				val colorIndex = (engine.statistics.maxCombo-1)/meterMax
				engine.meterValue = (engine.statistics.maxCombo-1)%meterMax
				engine.meterColor = METER_COLOUR_TABLE[colorIndex%METER_COLOUR_TABLE.size]
				engine.meterValueSub = if(colorIndex>0) meterMax else 0
				engine.meterColorSub = METER_COLOUR_TABLE[maxOf(colorIndex-1, 0)%METER_COLOUR_TABLE.size]
			} else {
				val remainLines = GOAL_TABLE[goaltype]-engine.statistics.lines
				engine.meterValue = remainLines*receiver.getMeterMax(engine)/GOAL_TABLE[goaltype]

				if(remainLines<=30) engine.meterColor = GameEngine.METER_COLOR_YELLOW
				if(remainLines<=20) engine.meterColor = GameEngine.METER_COLOR_ORANGE
				if(remainLines<=10) engine.meterColor = GameEngine.METER_COLOR_RED

				// Goal
				if(engine.statistics.lines>=GOAL_TABLE[goaltype]) {
					engine.ending = 1
					engine.gameEnded()
				} else if(engine.statistics.lines>=GOAL_TABLE[goaltype]-5)
					owner.bgmStatus.fadesw = true
				else if(engine.statistics.lines>=nextseclines) {
					owner.backgroundStatus.fadesw = true
					owner.backgroundStatus.fadecount = 0
					owner.backgroundStatus.fadebg = nextseclines/10
					nextseclines += 10
				}
			}
		} else if(GOAL_TABLE[goaltype]==-1&&engine.statistics.maxCombo>=2) {
			engine.ending = 1
			engine.gameEnded()
			engine.resetStatc()
			engine.stat = if(engine.statistics.maxCombo>40) GameEngine.Status.EXCELLENT else GameEngine.Status.GAMEOVER
		}
	}

	/** This function will be called when the game timer updates */
	override fun onLast(engine:GameEngine, playerID:Int) {
		scgettime++
	}

	/** Renders game result screen */
	override fun renderResult(engine:GameEngine, playerID:Int) {
		drawResultStats(engine, playerID, receiver, 0, EventReceiver.COLOR.CYAN, AbstractMode.Statistic.MAXCOMBO, AbstractMode.Statistic.TIME)
		drawResultStats(engine, playerID, receiver, 4, EventReceiver.COLOR.BLUE, AbstractMode.Statistic.LINES, AbstractMode.Statistic.PIECE, AbstractMode.Statistic.LPM, AbstractMode.Statistic.PPS)
		drawResultRank(engine, playerID, receiver, 12, EventReceiver.COLOR.BLUE, rankingRank)
		drawResultNetRank(engine, playerID, receiver, 14, EventReceiver.COLOR.BLUE, netRankingRank[0])
		drawResultNetRankDaily(engine, playerID, receiver, 16, EventReceiver.COLOR.BLUE, netRankingRank[1])

		if(netIsPB) receiver.drawMenuFont(engine, playerID, 2, 18, "NEW PB", EventReceiver.COLOR.ORANGE)

		if(netIsNetPlay&&netReplaySendStatus==1)
			receiver.drawMenuFont(engine, playerID, 0, 19, "SENDING...", EventReceiver.COLOR.PINK)
		else if(netIsNetPlay&&!netIsWatch
			&&netReplaySendStatus==2)
			receiver.drawMenuFont(engine, playerID, 1, 19, "A: RETRY", EventReceiver.COLOR.RED)
	}

	/** This function will be called when the replay data is going to be
	 * saved */
	override fun saveReplay(engine:GameEngine, playerID:Int, prop:CustomProperties) {
		engine.owner.replayProp.setProperty("comborace.version", version)
		savePreset(engine, engine.owner.replayProp, -1)

		// NET: Save name
		if(netPlayerName!=null&&netPlayerName!!.isNotEmpty()) prop.setProperty(playerID.toString()+".net.netPlayerName", netPlayerName)

		// Update rankings
		if(!owner.replayMode&&!big&&engine.ai==null) {
			updateRanking(engine.statistics.maxCombo-1, if(engine.ending==0) -1 else engine.statistics.time)

			if(rankingRank!=-1) {
				saveRanking(owner.modeConfig, engine.ruleopt.strRuleName)
				receiver.saveModeConfig(owner.modeConfig)
			}
		}
	}

	/** Load the ranking */
	override fun loadRanking(prop:CustomProperties, ruleName:String) {
		for(i in GOAL_TABLE.indices)
			for(j in 0 until RANKING_MAX) {
				rankingCombo[i][j] = prop.getProperty("comborace.ranking.$ruleName.$i.maxcombo.$j", 0)
				rankingTime[i][j] = prop.getProperty("comborace.ranking.$ruleName.$i.time.$j", -1)
			}
	}

	/** Save the ranking */
	private fun saveRanking(prop:CustomProperties?, ruleName:String) {
		for(i in GOAL_TABLE.indices)
			for(j in 0 until RANKING_MAX) {
				prop!!.setProperty("comborace.ranking.$ruleName.$i.maxcombo.$j", rankingCombo[i][j])
				prop.setProperty("comborace.ranking.$ruleName.$i.time.$j", rankingTime[i][j])
			}
	}

	/** Update the ranking */
	private fun updateRanking(maxcombo:Int, time:Int) {
		rankingRank = checkRanking(maxcombo, time)

		if(rankingRank!=-1) {
			// Shift down ranking entries
			for(i in RANKING_MAX-1 downTo rankingRank+1) {
				rankingCombo[goaltype][i] = rankingCombo[goaltype][i-1]
				rankingTime[goaltype][i] = rankingTime[goaltype][i-1]
			}

			// Add new data
			rankingCombo[goaltype][rankingRank] = maxcombo
			rankingTime[goaltype][rankingRank] = time
		}
	}

	/** This function will check the ranking and returns which place you are.
	 * (-1: Out of rank) */
	private fun checkRanking(maxcombo:Int, time:Int):Int {
		for(i in 0 until RANKING_MAX)
			if(maxcombo>rankingCombo[goaltype][i])
				return i
			else if(maxcombo==rankingCombo[goaltype][i]&&time>=0&&
				(time<rankingTime[goaltype][i]||rankingTime[goaltype][i]==-1))
				return i

		return -1
	}

	/** NET: Send various in-game stats (as well as goaltype)
	 * @param engine GameEngine
	 */
	override fun netSendStats(engine:GameEngine) {
		val bg = if(owner.backgroundStatus.fadesw) owner.backgroundStatus.fadebg else owner.backgroundStatus.bg
		var msg = "game\tstats\t"
		msg += engine.statistics.lines.toString()+"\t"+engine.statistics.totalPieceLocked+"\t"
		msg += engine.statistics.time.toString()+"\t"+engine.statistics.lpm+"\t"
		msg += engine.statistics.pps.toString()+"\t"+goaltype+"\t"
		msg += engine.gameActive.toString()+"\t"+engine.timerActive+"\t"
		msg += engine.meterColor.toString()+"\t"+engine.meterValue+"\t"
		msg += bg.toString()+"\t"
		msg += scgettime.toString()+"\t"+lastevent+"\t"+lastb2b+"\t"+lastcombo+"\t"+lastpiece+"\t"
		msg += engine.statistics.maxCombo.toString()+"\t"+engine.combo+"\n"
		netLobby!!.netPlayerClient!!.send(msg)
	}

	/** NET: Receive various in-game stats (as well as goaltype) */
	override fun netRecvStats(engine:GameEngine, message:Array<String>) {
		engine.statistics.lines = Integer.parseInt(message[4])
		engine.statistics.totalPieceLocked = Integer.parseInt(message[5])
		engine.statistics.time = Integer.parseInt(message[6])
		//engine.statistics.lpm = java.lang.Float.parseFloat(message[7])
		//engine.statistics.pps = java.lang.Float.parseFloat(message[8])
		goaltype = Integer.parseInt(message[9])
		engine.gameActive = java.lang.Boolean.parseBoolean(message[10])
		engine.timerActive = java.lang.Boolean.parseBoolean(message[11])
		engine.meterColor = Integer.parseInt(message[12])
		engine.meterValue = Integer.parseInt(message[13])
		owner.backgroundStatus.bg = Integer.parseInt(message[14])
		scgettime = Integer.parseInt(message[15])
		lastevent = Integer.parseInt(message[16])
		lastb2b = java.lang.Boolean.parseBoolean(message[17])
		lastcombo = Integer.parseInt(message[18])
		lastpiece = Integer.parseInt(message[19])
		engine.statistics.maxCombo = Integer.parseInt(message[20])
		engine.combo = Integer.parseInt(message[21])
	}

	/** NET: Send end-of-game stats
	 * @param engine GameEngine
	 */
	override fun netSendEndGameStats(engine:GameEngine) {
		var subMsg = ""
		subMsg += "MAX COMBO;"+(engine.statistics.maxCombo-1)+"\t"
		subMsg += "TIME;"+GeneralUtil.getTime(engine.statistics.time.toFloat())+"\t"
		subMsg += "LINE;"+engine.statistics.lines+"\t"
		subMsg += "PIECE;"+engine.statistics.totalPieceLocked+"\t"
		subMsg += "LINE/MIN;"+engine.statistics.lpm+"\t"
		subMsg += "PIECE/SEC;"+engine.statistics.pps+"\t"
		val msg = "gstat1p\t"+NetUtil.urlEncode(subMsg)+"\n"
		netLobby!!.netPlayerClient!!.send(msg)
	}

	/** NET: Send game options to all spectators
	 * @param engine GameEngine
	 */
	override fun netSendOptions(engine:GameEngine) {
		var msg = "game\toption\t"
		msg += engine.speed.gravity.toString()+"\t"+engine.speed.denominator+"\t"+engine.speed.are+"\t"
		msg += engine.speed.areLine.toString()+"\t"+engine.speed.lineDelay+"\t"+engine.speed.lockDelay+"\t"
		msg += engine.speed.das.toString()+"\t"+bgmno+"\t"+goaltype+"\t"+presetNumber+"\t"
		msg += shapetype.toString()+"\t"+comboColumn+"\t"+comboWidth+"\t"+ceilingAdjust+"\t"+spawnAboveField+"\n"
		netLobby!!.netPlayerClient!!.send(msg)
	}

	/** NET: Receive game options */
	override fun netRecvOptions(engine:GameEngine, message:Array<String>) {
		engine.speed.gravity = Integer.parseInt(message[4])
		engine.speed.denominator = Integer.parseInt(message[5])
		engine.speed.are = Integer.parseInt(message[6])
		engine.speed.areLine = Integer.parseInt(message[7])
		engine.speed.lineDelay = Integer.parseInt(message[8])
		engine.speed.lockDelay = Integer.parseInt(message[9])
		engine.speed.das = Integer.parseInt(message[10])
		bgmno = Integer.parseInt(message[11])
		goaltype = Integer.parseInt(message[12])
		presetNumber = Integer.parseInt(message[13])
		shapetype = Integer.parseInt(message[14])
		comboColumn = Integer.parseInt(message[15])
		comboWidth = Integer.parseInt(message[16])
		ceilingAdjust = Integer.parseInt(message[17])
		spawnAboveField = java.lang.Boolean.parseBoolean(message[18])
	}

	/** NET: Get goal type */
	override fun netGetGoalType():Int = goaltype

	/** NET: It returns true when the current settings doesn't prevent
	 * leaderboard screen from showing. */
	override fun netIsNetRankingViewOK(engine:GameEngine):Boolean = !big&&engine.ai==null

	companion object {
		/** Current version */
		private const val CURRENT_VERSION = 1

		/** Number of ranking records */
		private const val RANKING_MAX = 10

		/** HindranceLinescountConstantcount */
		private val GOAL_TABLE = intArrayOf(20, 40, 100, -1)

		/** Most recent scoring event typeConstantcount */
		private const val EVENT_NONE = 0
		private const val EVENT_SINGLE = 1
		private const val EVENT_DOUBLE = 2
		private const val EVENT_TRIPLE = 3
		private const val EVENT_FOUR = 4
		private const val EVENT_TSPIN_SINGLE_MINI = 5
		private const val EVENT_TSPIN_SINGLE = 6
		private const val EVENT_TSPIN_DOUBLE = 7
		private const val EVENT_TSPIN_TRIPLE = 8
		private const val EVENT_TSPIN_DOUBLE_MINI = 9

		/** Number of starting shapes */
		private const val SHAPETYPE_MAX = 9

		/** Names of starting shapes */
		private val SHAPE_NAME_TABLE = arrayOf("NONE", "LEFT I", "RIGHT I", "LEFT Z", "RIGHT S", "LEFT S", "RIGHT Z", "LEFT J", "RIGHT L")

		/** Starting shape table */
		private val SHAPE_TABLE = arrayOf(intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0), intArrayOf(1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0), intArrayOf(0, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0), intArrayOf(1, 1, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0), intArrayOf(0, 0, 1, 1, 0, 0, 0, 1, 0, 0, 0, 0), intArrayOf(1, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0, 0), intArrayOf(0, 0, 0, 1, 0, 0, 1, 1, 0, 0, 0, 0), intArrayOf(1, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0), intArrayOf(0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 1))

		/** Starting shape colour */
		private val SHAPE_COLOUR_TABLE = intArrayOf(Block.BLOCK_COLOR_NONE, Block.BLOCK_COLOR_CYAN, Block.BLOCK_COLOR_CYAN, Block.BLOCK_COLOR_RED, Block.BLOCK_COLOR_GREEN, Block.BLOCK_COLOR_GREEN, Block.BLOCK_COLOR_RED, Block.BLOCK_COLOR_BLUE, Block.BLOCK_COLOR_ORANGE)

		/** Stack colour order */
		private val STACK_COLOUR_TABLE = intArrayOf(Block.BLOCK_COLOR_RED, Block.BLOCK_COLOR_ORANGE, Block.BLOCK_COLOR_YELLOW, Block.BLOCK_COLOR_GREEN, Block.BLOCK_COLOR_CYAN, Block.BLOCK_COLOR_BLUE, Block.BLOCK_COLOR_PURPLE)

		/** Meter colors for really high combos in Endless */
		private val METER_COLOUR_TABLE = intArrayOf(GameEngine.METER_COLOR_GREEN, GameEngine.METER_COLOR_YELLOW, GameEngine.METER_COLOR_ORANGE, GameEngine.METER_COLOR_RED, GameEngine.METER_COLOR_PINK, GameEngine.METER_COLOR_PURPLE, GameEngine.METER_COLOR_DARKBLUE, GameEngine.METER_COLOR_BLUE, GameEngine.METER_COLOR_CYAN, GameEngine.METER_COLOR_DARKGREEN)
	}
}
