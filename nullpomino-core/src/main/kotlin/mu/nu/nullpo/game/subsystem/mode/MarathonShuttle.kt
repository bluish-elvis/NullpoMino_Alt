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
package mu.nu.nullpo.game.subsystem.mode

import mu.nu.nullpo.game.component.BGMStatus.BGM
import mu.nu.nullpo.game.component.Controller
import mu.nu.nullpo.game.event.EventReceiver.COLOR
import mu.nu.nullpo.game.event.ScoreEvent
import mu.nu.nullpo.game.net.NetUtil
import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.game.subsystem.mode.menu.BooleanMenuItem
import mu.nu.nullpo.game.subsystem.mode.menu.DelegateMenuItem
import mu.nu.nullpo.game.subsystem.mode.menu.LevelMenuItem
import mu.nu.nullpo.game.subsystem.mode.menu.MenuList
import mu.nu.nullpo.game.subsystem.mode.menu.StringsMenuItem
import mu.nu.nullpo.util.CustomProperties
import mu.nu.nullpo.util.GeneralUtil.toTimeStr
import kotlin.math.ceil

/** TECHNICIAN Mode */
class MarathonShuttle:NetDummyMode() {

	/** Number of Goal-points remaining */
	private var goal = 0
	private var goalmax = 0

	/** Level timer */
	private var levelTimer = 0
	private var lastlineTime = 0

	/** True if level timer runs out */
	private var levelTimeOut = false

	/** Master time limit */
	private var totalTimer = 0

	/** Ending time */
	private var rolltime = 0

	/** Most recent increase in goal-points */
	private var lastgoal = 0

	/** Most recent increase in time limit */
	private var lasttimebonus = 0

	/** Time to display the most recent increase in score */
	private var scgettime = 0
	private var sc = 0
	private var sum = 0

	/** REGRET display time frame count */
	private var regretdispframe = 0

	/** Current BGM */
	private var bgmLv = 0

	private val itemMode = StringsMenuItem(
		"goalType", "TYPE", COLOR.BLUE, 0,
		GAMETYPE_SHORTNAME
	)
	/** Game type */
	private var goalType:Int by DelegateMenuItem(itemMode)

	private val itemLevel = LevelMenuItem("startlevel", "LEVEL", COLOR.RED, 0, 0..19, false, true)
	/** Level at start */
	private var startLevel:Int by DelegateMenuItem(itemLevel)

	private val itemBig = BooleanMenuItem("big", "BIG", COLOR.BLUE, false)
	/** BigMode */
	private var big:Boolean by DelegateMenuItem(itemBig)

	override val menu = MenuList("technician", itemMode, itemLevel, itemBig)
	/** Version */
	private var version = 0

	/** Current round's ranking position */
	private var rankingRank = 0

	/** Rankings' scores */
	private val rankingScore = List(RANKING_TYPE) {MutableList(RANKING_MAX) {0L}}

	/** Rankings' line counts */
	private val rankingLines = List(RANKING_TYPE) {MutableList(RANKING_MAX) {0}}

	/** Rankings' times */
	private val rankingTime = List(RANKING_TYPE) {MutableList(RANKING_MAX) {0}}

	override val rankMap
		get() = rankMapOf(rankingScore.mapIndexed {a, x -> "$a.score" to x}+
			rankingLines.mapIndexed {a, x -> "$a.lines" to x}+
			rankingTime.mapIndexed {a, x -> "$a.time" to x})

	/* Mode name */
	override val name = "MARATHON ShuttleRun"
	override val gameIntensity = 1
	/* Initialization */
	override fun playerInit(engine:GameEngine) {
		super.playerInit(engine)
		goal = 0
		lastlineTime = 0
		levelTimer = lastlineTime
		levelTimeOut = false
		totalTimer = 0
		rolltime = 0
		lastgoal = 0
		lastscore = 0
		lasttimebonus = 0
		sc = 0
		scgettime = sc
		regretdispframe = 0
		bgmLv = 0

		rankingRank = -1
		rankingScore.forEach {it.fill(0)}
		rankingLines.forEach {it.fill(0)}
		rankingTime.forEach {it.fill(0)}

		netPlayerInit(engine)

		if(!owner.replayMode) {
			loadSetting(owner.modeConfig, engine)

			version = CURRENT_VERSION
		} else {
			loadSetting(owner.replayProp, engine)
			// NET: Load name
			netPlayerName = engine.owner.replayProp.getProperty("${engine.playerID}.net.netPlayerName", "")
		}

		engine.owner.bgMan.bg = startLevel
		if(engine.owner.bgMan.bg>19) engine.owner.bgMan.bg = 19
		engine.frameColor = GameEngine.FRAME_COLOR_WHITE
	}

	/* Render the settings screen */
	override fun renderSetting(engine:GameEngine) {
		if(netIsNetRankingDisplayMode)
		// NET: Netplay Ranking
			netOnRenderNetPlayRanking(engine, receiver)
		else {
			drawMenu(
				engine, receiver, 0, COLOR.BLUE, 0, "GAME TYPE" to GAMETYPE_SHORTNAME[goalType],
				"Level" to startLevel+1
			)

			drawMenuCompact(engine, receiver, "BIG" to big)
		}
	}

	/* Called at settings screen */
	override fun onSetting(engine:GameEngine):Boolean {
		// NET: Net Ranking
		if(netIsNetRankingDisplayMode)
			netOnUpdateNetPlayRanking(engine, goalType)
		else if(!engine.owner.replayMode) {
			// Configuration changes
			val change = updateMenu(engine)

			if(change!=0) {
				engine.playSE("change")
				engine.owner.bgMan.bg = minOf(19, startLevel)
				engine.statistics.level = startLevel
				engine.statistics.levelDispAdd = 1
				setSpeed(engine)
				// NET: Signal options change
				if(netIsNetPlay&&netNumSpectators>0) netSendOptions(engine)
			}

			// Confirm
			if(menuTime<5) menuTime++ else if(engine.ctrl.isPush(Controller.BUTTON_A)) {
				engine.playSE("decide")

				// NET: Signal start of the game
				if(netIsNetPlay) netLobby!!.netPlayerClient!!.send("start1p\n")

				return false
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
		// Menu

		return true
	}

	/* Called for initialization during "Ready" screen */
	override fun startGame(engine:GameEngine) {
		engine.statistics.level = startLevel
		engine.statistics.levelDispAdd = 1
		engine.b2bEnable = true
		engine.splitB2B = true
		engine.comboType = GameEngine.COMBO_TYPE_NORMAL

		engine.big = big

		engine.twistAllowKick = true
		engine.twistEnable = true
		engine.useAllSpinBonus = true
		engine.twistEnableEZ = true

		engine.speed.lineDelay = 8

		goalmax = (engine.statistics.level+1)*5
		goal = goalmax

		setSpeed(engine)

		if(netIsWatch)
			owner.musMan.bgm = BGM.Silent
		else {
			setStartBgmlv(engine)
			owner.musMan.bgm = tableBGM[bgmLv]
		}

		if(goalType==GAMETYPE_10MIN_EASY||goalType==GAMETYPE_10MIN_HARD) totalTimer = TIMELIMIT_10MIN
		if(goalType==GAMETYPE_SPECIAL) totalTimer = TIMELIMIT_SPECIAL

		if(goalType==GAMETYPE_SPECIAL) {
			engine.staffrollEnable = true
			engine.staffrollEnableStatistics = true
			engine.staffrollNoDeath = true
		}
	}

	/** Set the gravity rate
	 * @param engine GameEngine
	 */
	private fun setSpeed(engine:GameEngine) {
		val lv = maxOf(0, minOf(engine.statistics.level, tableGravity.size-1))

		engine.speed.gravity = tableGravity[lv]
		engine.speed.denominator = tableDenominator[lv]
	}

	/** Set BGM at start of game
	 * @param engine GameEngine
	 */
	private fun setStartBgmlv(engine:GameEngine) {
		bgmLv = 0
		while(tableBGMChange[bgmLv]!=-1&&engine.statistics.level>=tableBGMChange[bgmLv])
			bgmLv++
	}

	/* Render score */
	override fun renderLast(engine:GameEngine) {
		if(owner.menuOnly) return

		receiver.drawScoreFont(
			engine, 0, 0, "SHUTTLE RUN\n("+GAMETYPE_NAME[goalType]
				+")", COLOR.WHITE
		)

		if(engine.stat==GameEngine.Status.SETTING||engine.stat==GameEngine.Status.RESULT&&!owner.replayMode) {
			if(!owner.replayMode&&!big&&startLevel==0&&engine.ai==null) {
				val scale = if(receiver.nextDisplayType==2) .5f else 1f
				val topY = if(receiver.nextDisplayType==2) 6 else 4
				receiver.drawScoreFont(engine, 3, topY-1, "SCORE   LINE TIME", COLOR.BLUE, scale)

				for(i in 0 until RANKING_MAX) {
					receiver.drawScoreGrade(engine, 0, topY+i, String.format("%2d", i+1), COLOR.YELLOW, scale)
					receiver.drawScoreNum(engine, 3, topY+i, "${rankingScore[goalType][i]}", i==rankingRank, scale)
					receiver.drawScoreNum(engine, 11, topY+i, "${rankingLines[goalType][i]}", i==rankingRank, scale)
					receiver.drawScoreNum(engine, 16, topY+i, rankingTime[goalType][i].toTimeStr, i==rankingRank, scale)
				}
			}
		} else {
			// SCORE
			receiver.drawScoreFont(engine, 0, 2, "Score", COLOR.BLUE)
			receiver.drawScoreNum(engine, 5, 2, "+$lastscore")
			val scget = scgettime<engine.statistics.score
			if(scget) scgettime += ceil((engine.statistics.score-scgettime)/24.0).toInt()
			sc += ceil(((scgettime-sc)/10f).toDouble()).toInt()
			receiver.drawScoreNum(engine, 0, 3, "$sc", scget, 2f)

			// GOAL
			receiver.drawScoreFont(engine, 0, 5, "GOAL", COLOR.BLUE)
			var strGoal = "$goal"
			if(lastgoal!=0&&scget&&engine.ending==0) strGoal += "-$lastgoal"
			receiver.drawScoreNum(engine, 5, 5, strGoal, 2f)

			val remainLevelTime = maxOf(0, if(goalType!=GAMETYPE_SPECIAL) TIMELIMIT_LEVEL-levelTimer else totalTimer)
			val fontcolorLevelTime = when {
				remainLevelTime<10*60 -> COLOR.RED
				remainLevelTime<30*60 -> COLOR.ORANGE
				remainLevelTime<60*60 -> COLOR.YELLOW
				else -> COLOR.WHITE
			}
			if(goalType==GAMETYPE_SPECIAL) {
				// LEVEL TIME
				receiver.drawScoreFont(engine, 0, 6, "LIMIT", COLOR.BLUE)
				receiver.drawScoreNum(engine, 0, 7, remainLevelTime.toTimeStr, fontcolorLevelTime, 2f)
				// +30sec
				if(lasttimebonus>0&&scget&&engine.ending==0)
					receiver.drawScoreFont(
						engine, 6, 6, String.format("+%3.2fSEC.", lasttimebonus/60.0f),
						COLOR.YELLOW
					)
			} else {
				// LEVEL BONUS
				receiver.drawScoreFont(engine, 0, 6, "BONUS", COLOR.BLUE)
				val levelBonus = maxOf(0, (TIMELIMIT_LEVEL-levelTimer)*(engine.statistics.level+1))
				receiver.drawScoreNum(engine, 0, 7, "$levelBonus", fontcolorLevelTime, 2f)
				receiver.drawScoreNum(engine, 6, 6, remainLevelTime.toTimeStr, fontcolorLevelTime)
			}
			// LEVEL
			receiver.drawScoreFont(engine, 0, 9, "Level", COLOR.BLUE)
			receiver.drawScoreNum(engine, 5, 9, (engine.statistics.level+1).toString(), 2f)

			// TOTAL TIME
			receiver.drawScoreFont(engine, 0, 10, "Time", COLOR.BLUE)
			var totaltime = engine.statistics.time
			var fontcolorTotalTime = COLOR.WHITE
			if(goalType==GAMETYPE_10MIN_EASY||goalType==GAMETYPE_10MIN_HARD) {
				totaltime = TIMELIMIT_10MIN-totaltime

				fontcolorTotalTime = if(totaltime<10*60)
					COLOR.RED
				else if(totaltime<30*60)
					COLOR.ORANGE
				else if(totaltime<60*60)
					COLOR.YELLOW
				else
					COLOR.WHITE
			}
			receiver.drawScoreNum(engine, 0, 11, totaltime.toTimeStr, fontcolorTotalTime, 2f)

			// Ending time
			if(engine.gameActive&&(engine.ending==2||rolltime>0)) {
				var remainRollTime = TIMELIMIT_ROLL-rolltime
				if(remainRollTime<0) remainRollTime = 0

				receiver.drawScoreFont(engine, 0, 13, "ROLL TIME", COLOR.BLUE)
				receiver.drawScoreNum(
					engine, 0, 14, remainRollTime.toTimeStr, remainRollTime>0&&remainRollTime<10*60,
					2f
				)
			}

			if(regretdispframe>0)
			// REGRET
				receiver.drawMenuFont(
					engine, 2, 21, "REGRET", when {
						regretdispframe%4==0 -> COLOR.YELLOW
						regretdispframe%4==2 -> COLOR.RED
						else -> COLOR.ORANGE
					}
				)

		}

		super.renderLast(engine)
	}

	/* Called after every frame */
	override fun onLast(engine:GameEngine) {
		super.onLast(engine)

		if(regretdispframe>0) regretdispframe--

		// Level Time
		if(engine.gameActive&&engine.timerActive&&goalType!=GAMETYPE_SPECIAL) {
			levelTimer++
			val remainTime = TIMELIMIT_LEVEL-levelTimer
			// Time meter
			engine.meterValue = remainTime/2f/TIMELIMIT_LEVEL
			engine.meterValue += ((1-engine.meterValue)*(goalmax-goal)*remainTime/goalmax
				/(TIMELIMIT_LEVEL-lastlineTime))
			engine.meterColor = GameEngine.METER_COLOR_GREEN
			if(remainTime<=60*60) engine.meterColor = GameEngine.METER_COLOR_YELLOW
			if(remainTime<=30*60) engine.meterColor = GameEngine.METER_COLOR_ORANGE
			if(remainTime<=10*60) engine.meterColor = GameEngine.METER_COLOR_RED

			if(!netIsWatch)
				if(levelTimer>=TIMELIMIT_LEVEL) {
					// Out of time
					levelTimeOut = true

					if(goalType==GAMETYPE_LV15_HARD||goalType==GAMETYPE_10MIN_HARD) {
						engine.gameEnded()
						engine.resetStatc()
						engine.stat = GameEngine.Status.GAMEOVER
					} else if(goalType==GAMETYPE_10MIN_EASY||goalType==GAMETYPE_LV15_EASY) {
						regretdispframe = 180
						engine.playSE("regret")
						if(goalType==GAMETYPE_10MIN_EASY) {
							goal = goalmax
							levelTimer = 0
						}
					}
				} else if(remainTime<=10*60&&remainTime%60==0)
				// Countdown
					engine.playSE("countdown")
				else if(remainTime==30*60)
					engine.playSE("hurryup")
				else if(remainTime==60*60) engine.playSE("levelstop")
		}

		// Total Time
		if(engine.gameActive&&engine.timerActive&&goalType!=GAMETYPE_LV15_EASY&&goalType!=GAMETYPE_LV15_HARD) {
			totalTimer--

			// Time meter
			if(goalType==GAMETYPE_SPECIAL) {
				engine.meterValue = totalTimer/(5f*3600)
				engine.meterColor = GameEngine.METER_COLOR_LIMIT
			}

			if(!netIsWatch)
				if(totalTimer<0) {
					// Out of time
					engine.gameEnded()
					engine.resetStatc()

					if(goalType==GAMETYPE_10MIN_EASY||goalType==GAMETYPE_10MIN_HARD)
						engine.stat = GameEngine.Status.ENDINGSTART
					else
						engine.stat = GameEngine.Status.GAMEOVER

					totalTimer = 0
				} else if(totalTimer<=10*60&&totalTimer%60==0)
				// Countdown
					engine.playSE("countdown")
		}

		// Ending
		if(engine.gameActive&&engine.ending==2) {
			rolltime++

			// Time meter
			val remainRollTime = TIMELIMIT_ROLL-rolltime
			engine.meterValue = remainRollTime*1f/TIMELIMIT_ROLL
			engine.meterColor = GameEngine.METER_COLOR_LEVEL

			// Finished
			if(rolltime>=TIMELIMIT_ROLL&&!netIsWatch) {
				lastscore = totalTimer*2
				engine.statistics.scoreBonus += lastscore
				engine.lastEvent = null

				engine.gameEnded()
				engine.resetStatc()
				engine.stat = GameEngine.Status.EXCELLENT
			}
		}
	}

	/* Calculate score */
	override fun calcScore(engine:GameEngine, ev:ScoreEvent):Int {
		// Line clear bonus
		val pts = super.calcScore(engine, ev)
		if(pts>0) lastscore = pts

		lastgoal = calcPoint(engine, ev)
		goal -= lastgoal
		lastlineTime = levelTimer
		if(goal<=0) goal = 0


		if(engine.ending==0) {
			// Time bonus
			if(goal<=0&&!levelTimeOut&&goalType!=GAMETYPE_SPECIAL) {
				lasttimebonus = maxOf(0, (TIMELIMIT_LEVEL-levelTimer)*(engine.statistics.level+1))
				engine.statistics.scoreBonus += lasttimebonus
			} else if(goal<=0&&goalType==GAMETYPE_SPECIAL) {
				lasttimebonus = TIMELIMIT_SPECIAL_BONUS
				totalTimer += lasttimebonus
			} else if(pts>0) lasttimebonus = 0

			// BGM fade-out effects and BGM changes
			if(tableBGMChange[bgmLv]!=-1&&engine.statistics.level==tableBGMChange[bgmLv]-1)
				if(goal in 1..10)
					owner.musMan.fadesw = true
				else if(goal<=0) {
					bgmLv++
					owner.musMan.bgm = tableBGM[bgmLv]
					owner.musMan.fadesw = false
				}

			if(goal<=0)
				if(engine.statistics.level>=14&&(goalType==GAMETYPE_LV15_EASY||goalType==GAMETYPE_LV15_HARD)) {
					// Ending (LV15-EASY/HARD）
					engine.ending = 1
					engine.gameEnded()
				} else if(engine.statistics.level>=29&&goalType==GAMETYPE_SPECIAL) {
					// Ending (SPECIAL）
					engine.ending = 2
					engine.timerActive = false
					owner.musMan.bgm = BGM.Ending(0)
					owner.musMan.fadesw = false
					engine.playSE("endingstart")
				} else {
					// Level up
					engine.statistics.level++
					if(engine.statistics.level>29) engine.statistics.level = 29

					goalmax = (engine.statistics.level+1)*5
					goal += goalmax
					if(owner.bgMan.bg<19) {
						owner.bgMan.fadesw = true
						owner.bgMan.fadecount = 0
						owner.bgMan.fadebg = engine.statistics.level
					}

					levelTimer = 0
					if(version>=1) engine.holdUsedCount = 0

					setSpeed(engine)
					engine.playSE("levelup")
				}
		}
		return if(pts>0) lastscore else 0
	}

	/* Hard drop */
	override fun afterHardDropFall(engine:GameEngine, fall:Int) {
		engine.statistics.scoreHD += fall*2
		scgettime += fall*2
	}

	/* Soft drop */
	override fun afterSoftDropFall(engine:GameEngine, fall:Int) {
		engine.statistics.scoreSD += fall
		scgettime += fall
	}

	override fun onResult(engine:GameEngine):Boolean {
		val b = if(engine.statistics.time<10800) BGM.Result(1) else BGM.Result(2)
		owner.musMan.fadesw = false
		owner.musMan.bgm = b

		return super.onResult(engine)
	}

	/* Render results screen */
	override fun renderResult(engine:GameEngine) {
		drawResultStats(
			engine, receiver, 0, COLOR.BLUE, Statistic.SCORE, Statistic.LINES, Statistic.LEVEL, Statistic.TIME,
			Statistic.SPL, Statistic.LPM
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

	/** Calculate ranking position
	 * @param sc Score
	 * @param li Lines
	 * @param time Time
	 * @return Position (-1 if unranked)
	 */
	private fun checkRanking(sc:Long, li:Int, time:Int, type:Int):Int {
		for(i in 0 until RANKING_MAX)
			if(sc>rankingScore[type][i]) return i
			else if(sc==rankingScore[type][i]&&li>rankingLines[type][i]) return i
			else if(sc==rankingScore[type][i]&&li==rankingLines[type][i]&&time<rankingTime[type][i]) return i

		return -1
	}

	/** NET: Get goal type */
	override fun netGetGoalType():Int = goalType

	/** NET: It returns true when the current settings don't prevent
	 * leaderboard screen from showing. */
	override fun netIsNetRankingViewOK(engine:GameEngine):Boolean = !big&&engine.ai==null&&startLevel==0

	/** NET: Send game options to all spectators
	 * @param engine GameEngine
	 */
	override fun netSendOptions(engine:GameEngine) {
		val msg = "game\toption\t$goalType\t$startLevel\t$big\n"
		netLobby?.netPlayerClient?.send(msg)
	}

	/** NET: Receive game options */
	override fun netRecvOptions(engine:GameEngine, message:List<String>) {
		goalType = message[4].toInt()
		startLevel = message[5].toInt()
		big = message[6].toBoolean()
	}

	/** NET: Send various in-game stats of [engine] */
	override fun netSendStats(engine:GameEngine) {
		val bg = if(owner.bgMan.fadesw) owner.bgMan.fadebg else owner.bgMan.bg
		val msg = "game\tstats\t"+
			engine.run {
				statistics.run {"${scoreLine}\t${scoreSD}\t${scoreHD}\t${scoreBonus}\t${lines}\t${totalPieceLocked}\t${time}\t${level}\t"}+
					"$goalType\t${gameActive}\t${timerActive}\t$lastscore\t$scgettime\t${lastEvent}\t"+
					"$lastgoal\t$lasttimebonus\t$regretdispframe\t$bg\t${meterValue}\t${meterColor}\t$levelTimer\t$totalTimer\t$rolltime\t$goal\n"
			}
		netLobby!!.netPlayerClient!!.send(msg)
	}

	/** NET: Parse Received [message] as in-game stats of [engine] */
	override fun netRecvStats(engine:GameEngine, message:List<String>) {

		listOf<(String)->Unit>({}, {}, {}, {},
			{engine.statistics.scoreLine = it.toInt()},
			{engine.statistics.scoreSD = it.toInt()},
			{engine.statistics.scoreHD = it.toInt()},
			{engine.statistics.scoreBonus = it.toInt()},
			{engine.statistics.lines = it.toInt()},
			{engine.statistics.totalPieceLocked = it.toInt()},
			{engine.statistics.time = it.toInt()},
			{engine.statistics.level = it.toInt()},
			{goalType = it.toInt()},
			{engine.gameActive = it.toBoolean()},
			{engine.timerActive = it.toBoolean()},
			{lastscore = it.toInt()},
			{scgettime = it.toInt()},
			{engine.lastEvent = ScoreEvent.parseStr(it)},
			{lastgoal = it.toInt()},
			{lasttimebonus = it.toInt()},
			{regretdispframe = it.toInt()},
			{owner.bgMan.bg = it.toInt()},
			{engine.meterValue = it.toFloat()},
			{engine.meterColor = it.toInt()},
			{levelTimer = it.toInt()},
			{totalTimer = it.toInt()},
			{rolltime = it.toInt()},
			{goal = it.toInt()}).zip(message).forEach {(x, y) ->
			x(y)
		}

	}

	/** NET: Send end-of-game stats
	 * @param engine GameEngine
	 */
	override fun netSendEndGameStats(engine:GameEngine) {
		val subMsg = engine.statistics.run {
			"SCORE;${score}\tLINE;${lines}\tLEVEL;${(level+levelDispAdd)}\tTIME;${time.toTimeStr}\t"+
				"SCORE/LINE;${spl}\tLINE/MIN;${lpm}\t"
		}

		val msg = "gstat1p\t${NetUtil.urlEncode(subMsg)}\n"
		netLobby!!.netPlayerClient!!.send(msg)
	}

	/* Called when saving replay */
	override fun saveReplay(engine:GameEngine, prop:CustomProperties):Boolean {
		saveSetting(prop, engine)

		// NET: Save name
		if(!netPlayerName.isNullOrEmpty()) prop.setProperty("${engine.playerID}.net.netPlayerName", netPlayerName)

		// Update rankings
		if(!owner.replayMode&&!big&&engine.ai==null&&startLevel==0) {
			updateRanking(engine.statistics.score, engine.statistics.lines, engine.statistics.time, goalType)

			if(rankingRank!=-1) return true
		}
		return false
	}

	/** Update rankings
	 * @param sc Score
	 * @param li Lines
	 * @param time Time
	 * @param type Game type
	 */
	private fun updateRanking(sc:Long, li:Int, time:Int, type:Int) {
		rankingRank = checkRanking(sc, li, time, type)

		if(rankingRank!=-1) {
			// Shift down ranking entries
			for(i in RANKING_MAX-1 downTo rankingRank+1) {
				rankingScore[type][i] = rankingScore[type][i-1]
				rankingLines[type][i] = rankingLines[type][i-1]
				rankingTime[type][i] = rankingTime[type][i-1]
			}

			// Add new data
			rankingScore[type][rankingRank] = sc
			rankingLines[type][rankingRank] = li
			rankingTime[type][rankingRank] = time
		}
	}

	companion object {
		/** Current version */
		private const val CURRENT_VERSION = 2

		/** Fall velocity table (numerators) */
		private val tableGravity = listOf(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 3, 4, 5, 7, 10, -1)

		/** Fall velocity table (denominators) */
		private val tableDenominator = listOf(64, 50, 40, 33, 25, 20, 13, 10, 8, 4, 3, 2, 1, 1, 1, 1, 1, 1, 1, 1)

		/** BGM change levels */
		private val tableBGMChange = listOf(5, 8, 15, 17, 19, -1)
		private val tableBGM = listOf(
			BGM.Generic(0), BGM.Generic(1), BGM.Generic(2), BGM.Generic(3), BGM.Generic(4),
			BGM.Generic(5)
		)

		/** Combo goal table */
		private val COMBO_GOAL_TABLE = listOf(0, 0, 1, 1, 2, 2, 3, 3, 4, 4, 4, 5)

		/** Number of entries in rankings */
		private const val RANKING_MAX = 13

		/** Number of ranking types */
		private const val RANKING_TYPE = 5

		/** Game type constants */
		private const val GAMETYPE_LV15_EASY = 0
		private const val GAMETYPE_LV15_HARD = 1
		private const val GAMETYPE_10MIN_EASY = 2
		private const val GAMETYPE_10MIN_HARD = 3
		private const val GAMETYPE_SPECIAL = 4

		/** Game type names */
		private val GAMETYPE_NAME = listOf(
			"15LEVELS TIME TRIAL", "15LEVELS SPEED RUN", "10MINUITES TRIAL", "10MINUTES SURVIVAL",
			"UNLIMITED ENDURANCE"
		)
		private val GAMETYPE_SHORTNAME = listOf("15LV T.A.", "15LV S.R.", "10MIN.TRY", "10MIN.SURV", "ULM.ENDURO")

		/** Game type max */
		private const val GAMETYPE_MAX = 5

		/** Time limit for each level */
		private const val TIMELIMIT_LEVEL = 3600*2

		/** Time limit of 10min games */
		private const val TIMELIMIT_10MIN = 3600*10

		/** Default time limit of Special game */
		private const val TIMELIMIT_SPECIAL = 3600*2

		/** Extra time of Special game */
		private const val TIMELIMIT_SPECIAL_BONUS = 2000

		/** Ending time */
		private const val TIMELIMIT_ROLL = 3600
	}
}
