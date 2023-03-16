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
package mu.nu.nullpo.game.subsystem.mode

import mu.nu.nullpo.game.component.BGMStatus.BGM
import mu.nu.nullpo.game.component.Block
import mu.nu.nullpo.game.component.Controller
import mu.nu.nullpo.game.event.EventReceiver.COLOR
import mu.nu.nullpo.game.event.ScoreEvent
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

	/** Most recent scoring eventPiece inID */
	private var lastpiece = 0

	/** BGM number */
	private var bgmId = 0

	private val itemBig = BooleanMenuItem("big", "BIG", COLOR.BLUE, false)
	/** BigMode */
	private var big:Boolean by DelegateMenuItem(itemBig)

	/** HindranceLinescount type (0=5,1=10,2=18) */
	private var goalType = 0

	/** Current version */
	private var version = 0

	/** Last preset number used */
	private var presetNumber = 0

	/** Current round's ranking position */
	private var rankingRank = 0

	/** Rankings' times */
	private val rankingTime = List(GOAL_TABLE.size) {List(GAMETYPE_MAX) {MutableList(RANKING_MAX) {-1}}}

	/** Rankings' Combo */
	private val rankingCombo = List(GOAL_TABLE.size) {List(GAMETYPE_MAX) {MutableList(RANKING_MAX) {-1}}}

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
	override fun playerInit(engine:GameEngine) {
		super.playerInit(engine)

		scgettime = 0
		lastb2b = false
		lastcombo = 0
		lastpiece = 0
		bgmId = 0
		big = false
		goalType = 0
		shapetype = 1
		presetNumber = 0
		remainStack = 0
		stackColor = 0
		nextseclines = 10

		rankingRank = -1
		rankingTime.forEach {it.forEach {p -> p.fill(0)}}
		rankingCombo.forEach {it.forEach {p -> p.fill(0)}}

		engine.frameColor = GameEngine.FRAME_COLOR_RED

		netPlayerInit(engine)

		if(!engine.owner.replayMode) {
			version = CURRENT_VERSION
			presetNumber = engine.owner.modeConfig.getProperty("comborace.presetNumber", 0)
			loadPreset(engine, engine.owner.modeConfig, -1)
		} else {
			version = engine.owner.replayProp.getProperty("comborace.version", 0)
			presetNumber = 0
			loadPreset(engine, engine.owner.replayProp, -1)

			// NET: Load name
			netPlayerName = engine.owner.replayProp.getProperty("${engine.playerID}.net.netPlayerName", "")
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
		bgmId = prop.getProperty("comborace.bgmno.$preset", BGM.values.indexOf(BGM.Rush(0)))
		big = prop.getProperty("comborace.big.$preset", false)
		goalType = prop.getProperty("comborace.goalType.$preset", 1)
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
		prop.setProperty("comborace.bgmno.$preset", bgmId)
		prop.setProperty("comborace.big.$preset", big)
		prop.setProperty("comborace.goalType.$preset", goalType)
		prop.setProperty("comborace.shapetype.$preset", shapetype)
		prop.setProperty("comborace.comboWidth.$preset", comboWidth)
		prop.setProperty("comborace.ceilingAdjust.$preset", ceilingAdjust)
		prop.setProperty("comborace.spawnAboveField.$preset", spawnAboveField)
	}

	/** Main routine for game setup screen */
	override fun onSetting(engine:GameEngine):Boolean {
		// NET: Net Ranking
		if(netIsNetRankingDisplayMode)
			netOnUpdateNetPlayRanking(engine, goalType)
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
						goalType += change
						if(goalType<0) goalType = GOAL_TABLE.size-1
						if(goalType>GOAL_TABLE.size-1) goalType = 0
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
					11 -> bgmId = rangeCursor(bgmId+change, 0, BGM.count-1)
					12, 13 -> presetNumber = rangeCursor(presetNumber+change, 0, 99)
				}

				// NET: Signal options change
				if(netIsNetPlay&&netNumSpectators>0) netSendOptions(engine)
			}

			// Confirm
			if(menuTime<5) menuTime++ else if(engine.ctrl.isPush(Controller.BUTTON_A)) {
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
			if(engine.ctrl.isPush(Controller.BUTTON_B)&&!netIsNetPlay) engine.quitFlag = true

			// NET: Netplay Ranking
			if(engine.ctrl.isPush(Controller.BUTTON_D)&&netIsNetPlay&&netIsNetRankingViewOK(engine))
				netEnterNetPlayRankingScreen(goalType)
		} else {
			menuTime++
			menuCursor = -1

			return menuTime<60
		}// Replay

		return true
	}

	/** Renders game setup screen */
	override fun renderSetting(engine:GameEngine) {
		if(netIsNetRankingDisplayMode)
		// NET: Netplay Ranking
			netOnRenderNetPlayRanking(engine, receiver)
		else {
			drawMenu(
				engine, receiver, 0, COLOR.BLUE, 0,
				"GOAL" to if(GOAL_TABLE[goalType]==-1) "ENDLESS" else GOAL_TABLE[goalType]
			)
			drawMenu(
				engine, receiver, 2, if(comboWidth==4) COLOR.BLUE else COLOR.WHITE,
				1,
				"STARTSHAPE" to SHAPE_NAME_TABLE[shapetype]
			)
			menuColor = COLOR.BLUE
			drawMenuCompact(engine, receiver, "WIDTH" to comboWidth)

			drawMenuSpeeds(engine, receiver)
			drawMenuBGM(engine, receiver, bgmId)
			if(!engine.owner.replayMode) {
				menuColor = COLOR.GREEN
				drawMenuCompact(engine, receiver, "LOAD" to presetNumber, "SAVE" to presetNumber)
			}
		}
	}

	/** Ready */
	override fun onReady(engine:GameEngine):Boolean {
		if(engine.statc[0]==0) {
			engine.fieldWidth = maxOf(4, comboWidth)
			engine.createFieldIfNeeded()
			engine.meterColor = GameEngine.METER_COLOR_GREEN
			engine.meterValue = if(GOAL_TABLE[goalType]==-1) 0f else 1f

			if(!netIsWatch) {
				fillStack(engine, goalType)

				// NET: Send field
				if(netNumSpectators>0) netSendField(engine)
			}
		}
		return false
	}

	/** This function will be called before the game actually begins (after
	 * Ready&Go screen disappears) */
	override fun startGame(engine:GameEngine) {
		if(version<=0) engine.big = big
		owner.musMan.bgm = if(netIsWatch) BGM.Silent else BGM.values[bgmId]
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
	override fun renderLast(engine:GameEngine) {
		if(owner.menuOnly) return

		receiver.drawScoreFont(engine, 0, 0, name, COLOR.RED)
		if(GOAL_TABLE[goalType]==-1)
			receiver.drawScoreFont(engine, 0, 1, "(Endless run)", COLOR.WHITE)
		else
			receiver.drawScoreFont(
				engine, 0, 1, "("+(GOAL_TABLE[goalType]-1)
					+"CHAIN Challenge)", COLOR.WHITE
			)

		if(engine.stat==GameEngine.Status.SETTING||engine.stat==GameEngine.Status.RESULT&&!owner.replayMode) {
			if(!owner.replayMode&&!big&&engine.ai==null) {
				receiver.drawScoreFont(engine, 3, 3, "RECORD", COLOR.BLUE)

				for(i in 0 until RANKING_MAX) {
					receiver.drawScoreGrade(
						engine,
						0,
						4+i,
						"%2d".format(i+1),
						if(rankingRank==i) COLOR.RAINBOW else COLOR.YELLOW
					)
					if(rankingCombo[goalType][gameType][i]==GOAL_TABLE[goalType]-1)
						receiver.drawScoreFont(engine, 2, 4+i, "PERFECT", true)
					else receiver.drawScoreNum(engine, 3, 4+i, "${rankingCombo[goalType][gameType][i]}", rankingRank==i)
					receiver.drawScoreNum(engine, 9, 4+i, rankingTime[goalType][gameType][i].toTimeStr, rankingRank==i)
				}
			}
		} else {
			receiver.drawScoreFont(engine, 0, 3, "Longest Chain", COLOR.BLUE)
			receiver.drawScoreNum(
				engine, 0, 4, "${engine.statistics.maxCombo}", engine.statistics.maxCombo>0&&engine.combo==engine.statistics.maxCombo,
				2f
			)

			receiver.drawScoreFont(engine, 0, 6, "Lines", COLOR.BLUE)
			receiver.drawScoreNum(
				engine, 0, 7, "${engine.statistics.lines}", engine.statistics.lines==engine.statistics.totalPieceLocked,
				2f
			)

			receiver.drawScoreFont(engine, 0, 9, "PIECE", COLOR.BLUE)
			receiver.drawScoreNum(engine, 0, 10, "${engine.statistics.totalPieceLocked}", 2f)


			receiver.drawScoreFont(engine, 0, 15, "Time", COLOR.BLUE)
			receiver.drawScoreNum(engine, 0, 16, scgettime.toTimeStr, 2f)
			receiver.drawScoreNano(engine, 0, 17, engine.statistics.time.toTimeStr)
		}

		super.renderLast(engine)
	}

	/** Calculates line-clear score
	 * (This function will be called even if no lines are cleared) */
	override fun calcScore(engine:GameEngine, ev:ScoreEvent):Int {
		//  Attack
		if(ev.lines>0) {
			// B2B
			lastb2b = ev.b2b>0

			// Combo
			lastcombo = ev.combo
			scgettime = engine.statistics.time
			lastpiece = ev.piece!!.id

			// add any remaining stack lines
			val w = engine.field.width
			if(w>comboWidth) {
				if(GOAL_TABLE[goalType]==-1) remainStack = Integer.MAX_VALUE
				val cx = w-comboWidth/2-1
				var tmplines = 1
				while(tmplines<=ev.lines&&remainStack>0) {
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
			if(GOAL_TABLE[goalType]==-1) {
				val meterMax = 1
				val colorIndex = (engine.statistics.maxCombo-1)/meterMax
				engine.meterValue = (engine.statistics.maxCombo-1f)%meterMax
				engine.meterColor = METER_COLOR_TABLE[colorIndex%METER_COLOR_TABLE.size]
				engine.meterValueSub = if(colorIndex>0) 1f else 0f
				engine.meterColorSub = METER_COLOR_TABLE[maxOf(colorIndex-1, 0)%METER_COLOR_TABLE.size]
			} else {
				val remainLines = GOAL_TABLE[goalType]-engine.statistics.lines
				engine.meterValue = remainLines*1f/GOAL_TABLE[goalType]

				if(remainLines<=30) engine.meterColor = GameEngine.METER_COLOR_YELLOW
				if(remainLines<=20) engine.meterColor = GameEngine.METER_COLOR_ORANGE
				if(remainLines<=10) engine.meterColor = GameEngine.METER_COLOR_RED

				// Goal
				when {
					engine.statistics.lines>=GOAL_TABLE[goalType] -> {
						engine.ending = 1
						engine.gameEnded()
					}
					engine.statistics.lines>=GOAL_TABLE[goalType]-5 -> owner.musMan.fadeSW = true
					engine.statistics.lines>=nextseclines -> {
						owner.bgMan.nextBg = nextseclines/10
						nextseclines += 10
					}
				}
			}
		} else if(engine.statistics.maxCombo>=(if(GOAL_TABLE[goalType]==-1) 2 else GOAL_TABLE[goalType]-engine.statistics.lines)) {
			engine.ending = 1
			engine.gameEnded()
			engine.resetStatc()
			engine.stat = if(engine.statistics.maxCombo>=if(GOAL_TABLE[goalType]==-1) 40 else GOAL_TABLE[goalType]-1)
				GameEngine.Status.EXCELLENT else GameEngine.Status.GAMEOVER
			engine.statistics.time = scgettime
		}
		return 0
	}

	/** Renders game result screen */
	override fun renderResult(engine:GameEngine) {
		drawResultStats(engine, receiver, 0, COLOR.CYAN, Statistic.MAXCOMBO, Statistic.TIME)
		drawResultStats(
			engine, receiver, 4, COLOR.BLUE, Statistic.LINES, Statistic.PIECE, Statistic.LPM, Statistic.PPS
		)
		drawResultRank(engine, receiver, 12, COLOR.BLUE, rankingRank)
		drawResultNetRank(engine, receiver, 14, COLOR.BLUE, netRankingRank[0])
		drawResultNetRankDaily(engine, receiver, 16, COLOR.BLUE, netRankingRank[1])

		if(netIsPB) receiver.drawMenuFont(engine, 2, 18, "NEW PB", COLOR.ORANGE)

		if(netIsNetPlay&&netReplaySendStatus==1)
			receiver.drawMenuFont(engine, 0, 19, "SENDING...", COLOR.PINK)
		else if(netIsNetPlay&&!netIsWatch
			&&netReplaySendStatus==2
		)
			receiver.drawMenuFont(engine, 1, 19, "A: RETRY", COLOR.RED)
	}

	/** This function will be called when the replay data is going to be saved */
	override fun saveReplay(engine:GameEngine, prop:CustomProperties):Boolean {
		engine.owner.replayProp.setProperty("comborace.version", version)
		savePreset(engine, engine.owner.replayProp, -1)

		// NET: Save name
		if(netPlayerName!=null&&netPlayerName!!.isNotEmpty()) prop.setProperty(
			"${engine.playerID}.net.netPlayerName",
			netPlayerName
		)

		// Update rankings
		if(!owner.replayMode&&!big&&engine.ai==null) {
			updateRanking(engine.statistics.maxCombo, if(engine.ending==0) -1 else engine.statistics.time)

			if(rankingRank!=-1) return true
		}
		return false
	}

	/** Update the ranking */
	private fun updateRanking(maxcombo:Int, time:Int) {
		rankingRank = checkRanking(maxcombo, time)

		if(rankingRank!=-1) {
			// Shift down ranking entries
			for(i in RANKING_MAX-1 downTo rankingRank+1) {
				rankingCombo[goalType][gameType][i] = rankingCombo[goalType][gameType][i-1]
				rankingTime[goalType][gameType][i] = rankingTime[goalType][gameType][i-1]
			}

			// Add new data
			rankingCombo[goalType][gameType][rankingRank] = maxcombo
			rankingTime[goalType][gameType][rankingRank] = time
		}
	}

	/** This function will check the ranking and returns which place you are.
	 * (-1: Out of rank) */
	private fun checkRanking(combo:Int, time:Int):Int {
		for(i in 0 until RANKING_MAX)
			if(combo>rankingCombo[goalType][gameType][i])
				return i
			else if(combo==rankingCombo[goalType][gameType][i]&&time>=0&&
				(time<rankingTime[goalType][gameType][i]||rankingTime[goalType][gameType][i]==-1)
			)
				return i

		return -1
	}

	/** NET: Send various in-game stats of [engine] */
	override fun netSendStats(engine:GameEngine) {
		val bg = if(owner.bgMan.fadeSW) owner.bgMan.nextBg else owner.bgMan.bg
		val msg = "game\tstats\t"+
			engine.run {
				statistics.run {"${maxCombo}\t${lines}\t${totalPieceLocked}\t${time}\t${lpm}\t${pps}\t"}+
					"$goalType\t${gameActive}\t${timerActive}\t${meterColor}\t${meterValue}\t"
			}+"$bg\t$scgettime\t\t$lastb2b\t$lastcombo\t$lastpiece\n"
		netLobby?.netPlayerClient?.send(msg)
	}

	/** NET: Parse Received [message] as in-game stats of [engine] */
	override fun netRecvStats(engine:GameEngine, message:List<String>) {
		listOf<(String)->Unit>(
			{}, {}, {}, {},
			{engine.statistics.maxCombo = it.toInt()},
			{engine.statistics.lines = it.toInt()},
			{engine.statistics.totalPieceLocked = it.toInt()},
			{engine.statistics.time = it.toInt()},
			{goalType = it.toInt()},
			{engine.gameActive = it.toBoolean()},
			{engine.timerActive = it.toBoolean()},
			{engine.meterColor = it.toInt()},
			{engine.meterValue = it.toFloat()},
			{owner.bgMan.bg = it.toInt()},
			{scgettime = it.toInt()},
			{lastb2b = it.toBoolean()},
			{lastcombo = it.toInt()},
			{lastpiece = it.toInt()}
		).zip(message).forEach {(x, y) ->
			x(y)
		}
	}

	/** NET: Send end-of-game stats
	 * @param engine GameEngine
	 */
	override fun netSendEndGameStats(engine:GameEngine) {
		val subMsg = engine.statistics.run {
			"MAX COMBO;${(maxCombo-1)}\tTIME;${time.toTimeStr}\tLINE;${lines}\tPIECE;${totalPieceLocked}\tLINE/MIN;${lpm}\tPIECE/SEC;${pps}\t"
		}
		val msg = "gstat1p\t${NetUtil.urlEncode(subMsg)}\n"
		netLobby?.netPlayerClient?.send(msg)
	}

	/** NET: Send game options to all spectators
	 * @param engine GameEngine
	 */
	override fun netSendOptions(engine:GameEngine) {
		val msg = "game\toption\t"+engine.speed.run {
			"${gravity}\t${denominator}\t${are}\t${areLine}\t${lineDelay}\t${lockDelay}\t${das}\t"
		}+"$bgmId\t$goalType\t$presetNumber\t$shapetype${"\t\t$comboWidth\t$ceilingAdjust\t"+spawnAboveField}\n"
		netLobby?.netPlayerClient?.send(msg)
	}

	/** NET: Receive game options */
	override fun netRecvOptions(engine:GameEngine, message:List<String>) {
		engine.speed.gravity = message[4].toInt()
		engine.speed.denominator = message[5].toInt()
		engine.speed.are = message[6].toInt()
		engine.speed.areLine = message[7].toInt()
		engine.speed.lineDelay = message[8].toInt()
		engine.speed.lockDelay = message[9].toInt()
		engine.speed.das = message[10].toInt()
		bgmId = message[11].toInt()
		goalType = message[12].toInt()
		presetNumber = message[13].toInt()
		shapetype = message[14].toInt()
		comboWidth = message[16].toInt()
		ceilingAdjust = message[17].toInt()
		spawnAboveField = message[18].toBoolean()
	}

	/** NET: Get goal type */
	override val netGetGoalType get() = goalType

	/** NET: It returns true when the current settings don't prevent
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
		private val GOAL_TABLE = listOf(21, 41, 101, -1)

		/** Number of starting shapes */
		private const val SHAPETYPE_MAX = 9

		/** Names of starting shapes */
		private val SHAPE_NAME_TABLE = listOf(
			"NONE", "LEFT I", "RIGHT I", "LEFT Z", "RIGHT S", "LEFT S", "RIGHT Z", "LEFT J",
			"RIGHT L"
		)

		/** Starting shape table */
		private val SHAPE_TABLE = listOf(
			listOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0),
			listOf(1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0), listOf(0, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0),
			listOf(1, 1, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0), listOf(0, 0, 1, 1, 0, 0, 0, 1, 0, 0, 0, 0),
			listOf(1, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0, 0), listOf(0, 0, 0, 1, 0, 0, 1, 1, 0, 0, 0, 0),
			listOf(1, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0), listOf(0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 1)
		)

		/** Starting shape color */
		private val SHAPE_COLOR_TABLE = listOf(
			null, Block.COLOR.CYAN, Block.COLOR.CYAN, Block.COLOR.RED,
			Block.COLOR.GREEN, Block.COLOR.GREEN, Block.COLOR.RED, Block.COLOR.BLUE, Block.COLOR.ORANGE
		)

		/** Stack color order */
		private val STACK_COLOR_TABLE = listOf(
			Block.COLOR.RED, Block.COLOR.ORANGE, Block.COLOR.YELLOW,
			Block.COLOR.GREEN, Block.COLOR.CYAN, Block.COLOR.BLUE, Block.COLOR.PURPLE
		)

		/** Meter colors for really high combos in Endless */
		private val METER_COLOR_TABLE = listOf(
			GameEngine.METER_COLOR_GREEN, GameEngine.METER_COLOR_YELLOW,
			GameEngine.METER_COLOR_ORANGE, GameEngine.METER_COLOR_RED, GameEngine.METER_COLOR_PINK, GameEngine.METER_COLOR_PURPLE,
			GameEngine.METER_COLOR_DARKBLUE, GameEngine.METER_COLOR_BLUE, GameEngine.METER_COLOR_CYAN,
			GameEngine.METER_COLOR_DARKGREEN
		)
	}
}
