/*
 * Copyright (c) 2010-2021, NullNoname
 * Kotlin converted and modified by Venom=Nhelv
 * All rights reserved.
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

import mu.nu.nullpo.game.component.BGMStatus.BGM
import mu.nu.nullpo.game.component.Block
import mu.nu.nullpo.game.component.Controller
import mu.nu.nullpo.game.event.EventReceiver
import mu.nu.nullpo.game.net.NetUtil
import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.game.subsystem.mode.menu.BooleanMenuItem
import mu.nu.nullpo.game.subsystem.mode.menu.DelegateMenuItem
import mu.nu.nullpo.util.CustomProperties
import mu.nu.nullpo.util.GeneralUtil.toTimeStr

/** COMBO RACE Mode */
class SprintCombo:NetDummyMode() {

	/** EventReceiver object (This receives many game events, can also be used
	 * for drawing the fonts.) */

	/** Elapsed time from last line clear */
	private var scgettime = 0

	/** Most recent scoring eventInB2BIf it&#39;s the casetrue */
	private var lastb2b = false

	/** Most recent scoring eventInCombocount */
	private var lastcombo = 0

	/** Most recent scoring eventPeace inID */
	private var lastpiece = 0

	/** BGM number */
	private var bgmno = 0

	private val itemBig = BooleanMenuItem("big", "BIG", EventReceiver.COLOR.BLUE, false)
	/** BigMode */
	private var big:Boolean by DelegateMenuItem(itemBig)

	/** HindranceLinescount type (0=5,1=10,2=18) */
	private var goaltype = 0

	/** Current version */
	private var version = 0

	/** Last preset number used */
	private var presetNumber = 0

	/** Current round's ranking rank */
	private var rankingRank = 0

	/** Rankings' times */
	private var rankingTime:Array<Array<IntArray>> = Array(GOAL_TABLE.size) {Array(GAMETYPE_MAX) {IntArray(RANKING_MAX)}}

	/** Rankings' Combo */
	private var rankingCombo:Array<Array<IntArray>> = Array(GOAL_TABLE.size) {Array(GAMETYPE_MAX) {IntArray(RANKING_MAX)}}

	/** Shape type */
	private var shapetype = 0

	/** Stack color */
	private var stackColor = 0

	/** Width of combo well */
	private var comboWidth = 0

	/** */
	private val gameType:Int
		get() = when {
			comboWidth<4 -> 0
			comboWidth==4 -> 1
			else -> 2
		}

	/** Height difference between ceiling and stack (negative number lowers the
	 * stack height) */
	private var ceilingAdjust = 0

	/** Piece spawns above field if true */
	private var spawnAboveField = false

	/** Number of remaining stack lines that need to be added when lines are
	 * cleared */
	private var remainStack = 0

	/** Next section lines */
	private var nextseclines = 0

	/** Returns the name of this mode */
	override val name = "REN Sprint"
	override val gameIntensity = 2
	/** This function will be called when the game enters the main game
	 * screen. */
	override fun playerInit(engine:GameEngine, playerID:Int) {
		super.playerInit(engine, playerID)

		scgettime = 0
		lastb2b = false
		lastcombo = 0
		lastpiece = 0
		bgmno = 0
		big = false
		goaltype = 0
		shapetype = 1
		presetNumber = 0
		remainStack = 0
		stackColor = 0
		nextseclines = 10

		rankingRank = -1
		rankingTime = Array(GOAL_TABLE.size) {Array(GAMETYPE_MAX) {IntArray(RANKING_MAX)}}
		rankingCombo = Array(GOAL_TABLE.size) {Array(GAMETYPE_MAX) {IntArray(RANKING_MAX)}}

		engine.framecolor = GameEngine.FRAME_COLOR_RED

		netPlayerInit(engine, playerID)

		if(!engine.owner.replayMode) {
			version = CURRENT_VERSION
			presetNumber = engine.owner.modeConfig.getProperty("comborace.presetNumber", 0)
			loadPreset(engine, engine.owner.modeConfig, -1)

		} else {
			version = engine.owner.replayProp.getProperty("comborace.version", 0)
			presetNumber = 0
			loadPreset(engine, engine.owner.replayProp, -1)

			// NET: Load name
			netPlayerName = engine.owner.replayProp.getProperty("$playerID.net.netPlayerName", "")
		}
	}

	/** Load the settings */
	private fun loadPreset(engine:GameEngine, prop:CustomProperties, preset:Int) {
		engine.speed.gravity = prop.getProperty("comborace.gravity.$preset", 1)
		engine.speed.denominator = prop.getProperty("comborace.denominator.$preset", 256)
		engine.speed.are = prop.getProperty("comborace.are.$preset", 0)
		engine.speed.areLine = prop.getProperty("comborace.areLine.$preset", 0)
		engine.speed.lineDelay = prop.getProperty("comborace. lineDelay.$preset", 0)
		engine.speed.lockDelay = prop.getProperty("comborace.lockDelay.$preset", 30)
		engine.speed.das = prop.getProperty("comborace.das.$preset", 10)
		bgmno = prop.getProperty("comborace.bgmno.$preset", BGM.values.indexOf(BGM.Rush(0)))
		big = prop.getProperty("comborace.big.$preset", false)
		goaltype = prop.getProperty("comborace.goaltype.$preset", 1)
		shapetype = prop.getProperty("comborace.shapetype.$preset", 1)
		comboWidth = prop.getProperty("comborace.comboWidth.$preset", 4)
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
			val change = updateCursor(engine, 13)

			if(change!=0) {
				engine.playSE("change")

				var m = 1
				if(engine.ctrl.isPress(Controller.BUTTON_E)) m = 100
				if(engine.ctrl.isPress(Controller.BUTTON_F)) m = 1000

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
						comboWidth += change
						if(comboWidth>10) comboWidth = 2
						if(comboWidth<2) comboWidth = 10
					}
					3 -> {
						ceilingAdjust += change
						if(ceilingAdjust>10) ceilingAdjust = -10
						if(ceilingAdjust<-10) ceilingAdjust = 10
					}
					4 -> engine.speed.gravity = rangeCursor(engine.speed.gravity+change*m, -1, 99999)
					5 -> engine.speed.denominator = rangeCursor(change*m, -1, 99999)
					6 -> engine.speed.are = rangeCursor(engine.speed.are+change, 0, 99)
					7 -> engine.speed.areLine = rangeCursor(engine.speed.areLine+change, 0, 99)
					8 -> engine.speed.lineDelay = rangeCursor(engine.speed.lineDelay+change, 0, 99)
					9 -> engine.speed.lockDelay = rangeCursor(engine.speed.lockDelay+change, 0, 99)
					10 -> engine.speed.das = rangeCursor(engine.speed.das+change, 0, 99)
					11 -> bgmno = rangeCursor(bgmno+change, 0, BGM.count-1)
					12, 13 -> presetNumber = rangeCursor(presetNumber+change, 0, 99)
				}

				// NET: Signal options change
				if(netIsNetPlay&&netNumSpectators>0) netSendOptions(engine)
			}

			// Confirm
			if(engine.ctrl.isPush(Controller.BUTTON_A)&&menuTime>=5) {
				engine.playSE("decide")

				if(menuCursor==14) {
					loadPreset(engine, owner.modeConfig, presetNumber)

					// NET: Signal options change
					if(netIsNetPlay&&netNumSpectators>0) netSendOptions(engine)
				} else if(menuCursor==15) {
					savePreset(engine, owner.modeConfig, presetNumber)
					owner.saveModeConfig()
				} else {
					owner.modeConfig.setProperty("comborace.presetNumber", presetNumber)
					savePreset(engine, owner.modeConfig, -1)
					owner.saveModeConfig()

					// NET: Signal start of the game
					if(netIsNetPlay) netLobby!!.netPlayerClient!!.send("start1p\n")

					return false
				}
			}

			// Cancel
			if(engine.ctrl.isPush(Controller.BUTTON_B)&&!netIsNetPlay) engine.quitflag = true

			// NET: Netplay Ranking
			if(engine.ctrl.isPush(Controller.BUTTON_D)&&netIsNetPlay
				&&netIsNetRankingViewOK(engine)
			)
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

			drawMenu(
				engine, playerID, receiver, 0, EventReceiver.COLOR.BLUE, 0,
				"GOAL" to if(GOAL_TABLE[goaltype]==-1) "ENDLESS" else GOAL_TABLE[goaltype]
			)
			drawMenu(
				engine, playerID, receiver, 2,
				if(comboWidth==4) EventReceiver.COLOR.BLUE else EventReceiver.COLOR.WHITE,
				1, "STARTSHAPE" to SHAPE_NAME_TABLE[shapetype]
			)
			menuColor = EventReceiver.COLOR.BLUE
			drawMenuCompact(engine, playerID, receiver, "WIDTH" to comboWidth)

			drawMenuSpeeds(engine, playerID, receiver)
			drawMenuBGM(engine, playerID, receiver, bgmno)
			if(!engine.owner.replayMode) {
				menuColor = EventReceiver.COLOR.GREEN
				drawMenuCompact(engine, playerID, receiver, "LOAD" to presetNumber, "SAVE" to presetNumber)
			}
		}
	}

	/** Ready */
	override fun onReady(engine:GameEngine, playerID:Int):Boolean {
		if(engine.statc[0]==0) {
			engine.fieldWidth = maxOf(4, comboWidth)
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
			owner.bgmStatus.bgm = BGM.Silent
		else
			owner.bgmStatus.bgm = BGM.values[bgmno]
		engine.comboType = GameEngine.COMBO_TYPE_NORMAL
		engine.twistEnable = true
		engine.twistAllowKick = true
		engine.ruleOpt.pieceEnterAboveField = spawnAboveField
	}

	/** Fill the playfield with stack
	 * @param engine GameEngine
	 * @param height Stack height level number
	 */
	private fun fillStack(engine:GameEngine, height:Int) {
		val w = engine.field.width
		val h = engine.field.height
		val stackHeight:Int
		val cx = w-comboWidth/2-1
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
		if(w>comboWidth)
			for(y in h-1 downTo h-stackHeight) {
				for(x in 0 until w)
					if(x<cx-1||x>cx-2+comboWidth)
						engine.field.setBlock(
							x, y,
							Block(
								STACK_COLOR_TABLE[stackColor%STACK_COLOR_TABLE.size], engine.skin, Block.ATTRIBUTE.VISIBLE,
								Block.ATTRIBUTE.GARBAGE
							)
						)
				stackColor++
			}

		// insert starting shape
		if(comboWidth==4)
			for(i in 0..11)
				if(SHAPE_TABLE[shapetype][i]==1)
					engine.field.setBlock(
						i%4, h-1-i/4,
						Block(SHAPE_COLOR_TABLE[shapetype], engine.skin, Block.ATTRIBUTE.VISIBLE, Block.ATTRIBUTE.GARBAGE)
					)
	}

	/** Renders HUD (leaderboard or game statistics) */
	override fun renderLast(engine:GameEngine, playerID:Int) {
		if(owner.menuOnly) return

		receiver.drawScoreFont(engine, playerID, 0, 0, name, EventReceiver.COLOR.RED)
		if(GOAL_TABLE[goaltype]==-1)
			receiver.drawScoreFont(engine, playerID, 0, 1, "(Endless run)", EventReceiver.COLOR.WHITE)
		else
			receiver.drawScoreFont(
				engine, playerID, 0, 1, "("+(GOAL_TABLE[goaltype]-1)
					+"CHAIN Challenge)", EventReceiver.COLOR.WHITE
			)

		if(engine.stat==GameEngine.Status.SETTING||engine.stat==GameEngine.Status.RESULT&&!owner.replayMode) {
			if(!owner.replayMode&&!big&&engine.ai==null) {
				receiver.drawScoreFont(engine, playerID, 3, 3, "RECORD", EventReceiver.COLOR.BLUE)

				for(i in 0 until RANKING_MAX) {
					receiver.drawScoreGrade(
						engine, playerID, 0, 4+i, String.format("%2d", i+1),
						if(rankingRank==i) EventReceiver.COLOR.RAINBOW else EventReceiver.COLOR.YELLOW
					)
					if(rankingCombo[goaltype][gameType][i]==GOAL_TABLE[goaltype]-1)
						receiver.drawScoreFont(engine, playerID, 2, 4+i, "PERFECT", true)
					else receiver.drawScoreNum(engine, playerID, 3, 4+i, "${rankingCombo[goaltype][gameType][i]}", rankingRank==i)
					receiver.drawScoreNum(engine, playerID, 9, 4+i, rankingTime[goaltype][gameType][i].toTimeStr, rankingRank==i)
				}
			}
		} else {

			receiver.drawScoreFont(engine, playerID, 0, 3, "Longest Chain", EventReceiver.COLOR.BLUE)
			receiver.drawScoreNum(
				engine, playerID, 0, 4, "${engine.statistics.maxCombo}",
				engine.statistics.maxCombo>0&&engine.combo-1==engine.statistics.maxCombo, 2f
			)

			receiver.drawScoreFont(engine, playerID, 0, 6, "Lines", EventReceiver.COLOR.BLUE)
			receiver.drawScoreNum(
				engine, playerID, 0, 7, "${engine.statistics.lines}",
				engine.statistics.lines==engine.statistics.totalPieceLocked, 2f
			)

			receiver.drawScoreFont(engine, playerID, 0, 9, "PIECE", EventReceiver.COLOR.BLUE)
			receiver.drawScoreNum(engine, playerID, 0, 10, "${engine.statistics.totalPieceLocked}", 2f)


			receiver.drawScoreFont(engine, playerID, 0, 15, "Time", EventReceiver.COLOR.BLUE)
			receiver.drawScoreNum(engine, playerID, 0, 16, scgettime.toTimeStr, 2f)
			receiver.drawScoreNano(engine, playerID, 0, 17, engine.statistics.time.toTimeStr)
		}

		super.renderLast(engine, playerID)
	}

	/** Calculates line-clear score
	 * (This function will be called even if no lines are cleared) */
	override fun calcScore(engine:GameEngine, playerID:Int, lines:Int):Int {
		//  Attack
		if(lines>0) {
			// B2B
			lastb2b = engine.b2b

			// Combo
			lastcombo = engine.combo
			scgettime = engine.statistics.time
			lastpiece = engine.nowPieceObject!!.id

			// add any remaining stack lines
			val w = engine.field.width
			if(w>comboWidth) {
				if(GOAL_TABLE[goaltype]==-1) remainStack = Integer.MAX_VALUE
				val cx = w-comboWidth/2-1
				var tmplines = 1
				while(tmplines<=lines&&remainStack>0) {
					for(x in 0 until w)
						if(x<cx-1||x>cx-2+comboWidth)
							engine.field.setBlock(
								x, -ceilingAdjust-tmplines,
								Block(
									STACK_COLOR_TABLE[stackColor%STACK_COLOR_TABLE.size], engine.skin, Block.ATTRIBUTE.VISIBLE,
									Block.ATTRIBUTE.GARBAGE
								)
							)
					stackColor++
					tmplines++
					remainStack--
				}
			}
			if(GOAL_TABLE[goaltype]==-1) {
				val meterMax = receiver.getMeterMax(engine)
				val colorIndex = (engine.statistics.maxCombo-1)/meterMax
				engine.meterValue = (engine.statistics.maxCombo-1)%meterMax
				engine.meterColor = METER_COLOR_TABLE[colorIndex%METER_COLOR_TABLE.size]
				engine.meterValueSub = if(colorIndex>0) meterMax else 0
				engine.meterColorSub = METER_COLOR_TABLE[maxOf(colorIndex-1, 0)%METER_COLOR_TABLE.size]
			} else {
				val remainLines = GOAL_TABLE[goaltype]-engine.statistics.lines
				engine.meterValue = remainLines*receiver.getMeterMax(engine)/GOAL_TABLE[goaltype]

				if(remainLines<=30) engine.meterColor = GameEngine.METER_COLOR_YELLOW
				if(remainLines<=20) engine.meterColor = GameEngine.METER_COLOR_ORANGE
				if(remainLines<=10) engine.meterColor = GameEngine.METER_COLOR_RED

				// Goal
				when {
					engine.statistics.lines>=GOAL_TABLE[goaltype] -> {
						engine.ending = 1
						engine.gameEnded()
					}
					engine.statistics.lines>=GOAL_TABLE[goaltype]-5 -> owner.bgmStatus.fadesw = true
					engine.statistics.lines>=nextseclines -> {
						owner.backgroundStatus.fadesw = true
						owner.backgroundStatus.fadecount = 0
						owner.backgroundStatus.fadebg = nextseclines/10
						nextseclines += 10
					}
				}
			}
		} else if(engine.statistics.maxCombo>=(if(GOAL_TABLE[goaltype]==-1) 2 else GOAL_TABLE[goaltype]-engine.statistics.lines)) {
			engine.ending = 1
			engine.gameEnded()
			engine.resetStatc()
			engine.stat = if(engine.statistics.maxCombo>=if(GOAL_TABLE[goaltype]==-1) 40 else GOAL_TABLE[goaltype]-1)
				GameEngine.Status.EXCELLENT else GameEngine.Status.GAMEOVER
			engine.statistics.time = scgettime
		}
		return 0
	}

	/** Renders game result screen */
	override fun renderResult(engine:GameEngine, playerID:Int) {
		drawResultStats(engine, playerID, receiver, 0, EventReceiver.COLOR.CYAN, Statistic.MAXCOMBO, Statistic.TIME)
		drawResultStats(
			engine, playerID, receiver, 4, EventReceiver.COLOR.BLUE, Statistic.LINES, Statistic.PIECE, Statistic.LPM,
			Statistic.PPS
		)
		drawResultRank(engine, playerID, receiver, 12, EventReceiver.COLOR.BLUE, rankingRank)
		drawResultNetRank(engine, playerID, receiver, 14, EventReceiver.COLOR.BLUE, netRankingRank[0])
		drawResultNetRankDaily(engine, playerID, receiver, 16, EventReceiver.COLOR.BLUE, netRankingRank[1])

		if(netIsPB) receiver.drawMenuFont(engine, playerID, 2, 18, "NEW PB", EventReceiver.COLOR.ORANGE)

		if(netIsNetPlay&&netReplaySendStatus==1)
			receiver.drawMenuFont(engine, playerID, 0, 19, "SENDING...", EventReceiver.COLOR.PINK)
		else if(netIsNetPlay&&!netIsWatch
			&&netReplaySendStatus==2
		)
			receiver.drawMenuFont(engine, playerID, 1, 19, "A: RETRY", EventReceiver.COLOR.RED)
	}

	/** This function will be called when the replay data is going to be saved */
	override fun saveReplay(engine:GameEngine, playerID:Int, prop:CustomProperties):Boolean {
		engine.owner.replayProp.setProperty("comborace.version", version)
		savePreset(engine, engine.owner.replayProp, -1)

		// NET: Save name
		if(netPlayerName!=null&&netPlayerName!!.isNotEmpty()) prop.setProperty("$playerID.net.netPlayerName", netPlayerName)

		// Update rankings
		if(!owner.replayMode&&!big&&engine.ai==null) {
			updateRanking(engine.statistics.maxCombo, if(engine.ending==0) -1 else engine.statistics.time)

			if(rankingRank!=-1) return true
		}
		return false
	}

	/** Load the ranking */
	override fun loadRanking(prop:CustomProperties, ruleName:String) {
		for(i in GOAL_TABLE.indices)
			for(j in 0 until GAMETYPE_MAX)
				for(k in 0 until RANKING_MAX) {
					rankingCombo[i][j][k] = prop.getProperty("$ruleName.$i.$j.maxcombo.$k", 0)
					rankingTime[i][j][k] = prop.getProperty("$ruleName.$i.$j.time.$k", -1)
				}
	}

	/** Save the ranking */
	private fun saveRanking(ruleName:String) {
		super.saveRanking((GOAL_TABLE.indices).flatMap {i ->
			(0 until GAMETYPE_MAX).flatMap {j ->
				(0 until RANKING_MAX).flatMap {k ->
					listOf(
						"$ruleName.$i.$j.maxcombo.$k" to rankingCombo[i][j][k],
						"$ruleName.$i.$j.time.$k" to rankingTime[i][j][k]
					)
				}
			}
		})
	}

	/** Update the ranking */
	private fun updateRanking(maxcombo:Int, time:Int) {
		rankingRank = checkRanking(maxcombo, time)

		if(rankingRank!=-1) {
			// Shift down ranking entries
			for(i in RANKING_MAX-1 downTo rankingRank+1) {
				rankingCombo[goaltype][gameType][i] = rankingCombo[goaltype][gameType][i-1]
				rankingTime[goaltype][gameType][i] = rankingTime[goaltype][gameType][i-1]
			}

			// Add new data
			rankingCombo[goaltype][gameType][rankingRank] = maxcombo
			rankingTime[goaltype][gameType][rankingRank] = time
		}
	}

	/** This function will check the ranking and returns which place you are.
	 * (-1: Out of rank) */
	private fun checkRanking(combo:Int, time:Int):Int {
		for(i in 0 until RANKING_MAX)
			if(combo>rankingCombo[goaltype][gameType][i])
				return i
			else if(combo==rankingCombo[goaltype][gameType][i]&&time>=0&&
				(time<rankingTime[goaltype][gameType][i]||rankingTime[goaltype][gameType][i]==-1)
			)
				return i

		return -1
	}

	/** NET: Send various in-game stats of [engine] */
	override fun netSendStats(engine:GameEngine) {
		val bg = if(owner.backgroundStatus.fadesw) owner.backgroundStatus.fadebg else owner.backgroundStatus.bg
		var msg = "game\tstats\t"
		msg += "${engine.statistics.lines}\t${engine.statistics.totalPieceLocked}\t"
		msg += "${engine.statistics.time}\t${engine.statistics.lpm}\t"
		msg += "${engine.statistics.pps}\t$goaltype\t"
		msg += "${engine.gameActive}\t${engine.timerActive}\t"
		msg += "${engine.meterColor}\t${engine.meterValue}\t"
		msg += "$bg"+"\t"
		msg += "$scgettime${"\t\t$lastb2b\t$lastcombo\t"+lastpiece}\t"
		msg += "${engine.statistics.maxCombo}\t${engine.combo}\n"
		netLobby!!.netPlayerClient!!.send(msg)
	}

	/** NET: Parse Received [message] as in-game stats of [engine] */
	override fun netRecvStats(engine:GameEngine, message:Array<String>) {
		engine.statistics.lines = message[4].toInt()
		engine.statistics.totalPieceLocked = message[5].toInt()
		engine.statistics.time = message[6].toInt()
		//engine.statistics.lpm = message[7].toFloat()
		//engine.statistics.pps = message[8].toFloat()
		goaltype = message[9].toInt()
		engine.gameActive = message[10].toBoolean()
		engine.timerActive = message[11].toBoolean()
		engine.meterColor = message[12].toInt()
		engine.meterValue = message[13].toInt()
		owner.backgroundStatus.bg = message[14].toInt()
		scgettime = message[15].toInt()
		lastb2b = message[17].toBoolean()
		lastcombo = message[18].toInt()
		lastpiece = message[19].toInt()
		engine.statistics.maxCombo = message[20].toInt()
		engine.combo = message[21].toInt()
	}

	/** NET: Send end-of-game stats
	 * @param engine GameEngine
	 */
	override fun netSendEndGameStats(engine:GameEngine) {
		var subMsg = ""
		subMsg += "MAX COMBO;${(engine.statistics.maxCombo-1)}\t"
		subMsg += "TIME;${engine.statistics.time.toTimeStr}\t"
		subMsg += "LINE;${engine.statistics.lines}\t"
		subMsg += "PIECE;${engine.statistics.totalPieceLocked}\t"
		subMsg += "LINE/MIN;${engine.statistics.lpm}\t"
		subMsg += "PIECE/SEC;${engine.statistics.pps}\t"
		val msg = "gstat1p\t${NetUtil.urlEncode(subMsg)}\n"
		netLobby!!.netPlayerClient!!.send(msg)
	}

	/** NET: Send game options to all spectators
	 * @param engine GameEngine
	 */
	override fun netSendOptions(engine:GameEngine) {
		var msg = "game\toption\t"
		msg += "${engine.speed.gravity}\t${engine.speed.denominator}\t${engine.speed.are}\t"
		msg += "${engine.speed.areLine}\t${engine.speed.lineDelay}\t${engine.speed.lockDelay}\t"
		msg += "${engine.speed.das}\t$bgmno\t$goaltype\t$presetNumber\t"
		msg += "$shapetype${"\t\t$comboWidth\t$ceilingAdjust\t"+spawnAboveField}\n"
		netLobby!!.netPlayerClient!!.send(msg)
	}

	/** NET: Receive game options */
	override fun netRecvOptions(engine:GameEngine, message:Array<String>) {
		engine.speed.gravity = message[4].toInt()
		engine.speed.denominator = message[5].toInt()
		engine.speed.are = message[6].toInt()
		engine.speed.areLine = message[7].toInt()
		engine.speed.lineDelay = message[8].toInt()
		engine.speed.lockDelay = message[9].toInt()
		engine.speed.das = message[10].toInt()
		bgmno = message[11].toInt()
		goaltype = message[12].toInt()
		presetNumber = message[13].toInt()
		shapetype = message[14].toInt()
		comboWidth = message[16].toInt()
		ceilingAdjust = message[17].toInt()
		spawnAboveField = message[18].toBoolean()
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
		private const val RANKING_MAX = 13

		/** Number of ranking types */
		private const val GAMETYPE_MAX = 3

		/** HindranceLinescountConstantcount */
		private val GOAL_TABLE = intArrayOf(21, 41, 101, -1)

		/** Number of starting shapes */
		private const val SHAPETYPE_MAX = 9

		/** Names of starting shapes */
		private val SHAPE_NAME_TABLE = arrayOf(
			"NONE", "LEFT I", "RIGHT I", "LEFT Z", "RIGHT S", "LEFT S", "RIGHT Z", "LEFT J",
			"RIGHT L"
		)

		/** Starting shape table */
		private val SHAPE_TABLE = arrayOf(
			intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0),
			intArrayOf(1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0), intArrayOf(0, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0),
			intArrayOf(1, 1, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0), intArrayOf(0, 0, 1, 1, 0, 0, 0, 1, 0, 0, 0, 0),
			intArrayOf(1, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0, 0), intArrayOf(0, 0, 0, 1, 0, 0, 1, 1, 0, 0, 0, 0),
			intArrayOf(1, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0), intArrayOf(0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 1)
		)

		/** Starting shape color */
		private val SHAPE_COLOR_TABLE:Array<Block.COLOR?> = arrayOf(
			null, Block.COLOR.CYAN, Block.COLOR.CYAN, Block.COLOR.RED,
			Block.COLOR.GREEN, Block.COLOR.GREEN, Block.COLOR.RED, Block.COLOR.BLUE, Block.COLOR.ORANGE
		)

		/** Stack color order */
		private val STACK_COLOR_TABLE:Array<Block.COLOR> = arrayOf(
			Block.COLOR.RED, Block.COLOR.ORANGE, Block.COLOR.YELLOW,
			Block.COLOR.GREEN, Block.COLOR.CYAN, Block.COLOR.BLUE, Block.COLOR.PURPLE
		)

		/** Meter colors for really high combos in Endless */
		private val METER_COLOR_TABLE = intArrayOf(
			GameEngine.METER_COLOR_GREEN, GameEngine.METER_COLOR_YELLOW,
			GameEngine.METER_COLOR_ORANGE, GameEngine.METER_COLOR_RED, GameEngine.METER_COLOR_PINK, GameEngine.METER_COLOR_PURPLE,
			GameEngine.METER_COLOR_DARKBLUE, GameEngine.METER_COLOR_BLUE, GameEngine.METER_COLOR_CYAN,
			GameEngine.METER_COLOR_DARKGREEN
		)
	}
}
