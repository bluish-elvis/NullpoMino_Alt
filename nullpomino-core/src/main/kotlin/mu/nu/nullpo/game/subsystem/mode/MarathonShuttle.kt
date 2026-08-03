/*
 Copyright (c) 2010-2024, NullNoname
 All rights reserved.

 Converted to Kotlin and modified by Venom_Nhelv as bluish-elvis
 THIS IS NOT MADE IN ASSOCIATION WITH THE GAME CREATOR.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions are met:
     * Redistributions of source code must retain the above copyright
       notice, this list of conditions and the following disclaimer.
     * Redistributions in binary form must reproduce the above copyright
       notice, this list of conditions and the following disclaimer in the
       documentation and/or other materials provided with the distribution.
     * Neither the name of NullNoname nor the names of its
       contributors may be used to endorse or promote products derived from
       this software without specific prior written permission.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 POSSIBILITY OF SUCH DAMAGE.
 */
package mu.nu.nullpo.game.subsystem.mode

import kotlinx.serialization.serializer
import mu.nu.nullpo.game.component.BGM
import mu.nu.nullpo.game.component.LevelData
import mu.nu.nullpo.game.event.*
import mu.nu.nullpo.game.event.EventReceiver.COLOR
import mu.nu.nullpo.game.net.NetUtil
import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.game.play.GameEngine.Status
import mu.nu.nullpo.game.subsystem.mode.menu.*
import mu.nu.nullpo.gui.common.BaseFont.FONT.*
import mu.nu.nullpo.util.CustomProperties
import mu.nu.nullpo.util.GeneralUtil.toTimeStr

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
	private var rollTime = 0

	/** Most recent increase in goal-points */
	private var lastgoal = 0

	/** Most recent increase in time limit */
	private var lasttimebonus = 0
	val timer get() = maxOf(0, if(goalType!=GAMETYPE.SPECIAL) TIMELIMIT_LEVEL-levelTimer else totalTimer)

	/** REGRET display time frame count */
	private var regretdispframe = 0

	/** Current BGM */
	private var bgmLv = 0

	private val itemMode = EnumMenuItem("goalType", "TYPE", COLOR.BLUE, GAMETYPE.LV15_EASY, GAMETYPE.entries) {it.shortName}
	/** Game type */
	private var goalType:GAMETYPE by DelegateMenuItem(itemMode)

	private val itemNorma = EnumMenuItem("normaType", "TYPE", COLOR.BLUE, NORMATYPE.POINTS, NORMATYPE.entries) {it.showName}
	/** Game type */
	private var normaType:NORMATYPE by DelegateMenuItem(itemNorma)

	private val itemLevel = LevelMenuItem("startlevel", "LEVEL", COLOR.RED, 0, 0..19, false, true)
	/** Level at start */
	private var startLevel:Int by DelegateMenuItem(itemLevel)

	private val itemBig = BooleanMenuItem("big", "BIG", COLOR.ORANGE, false)
	/** BigMode */
	private var big:Boolean by DelegateMenuItem(itemBig)

	override val menu = MenuList("technician", itemMode, itemNorma, itemLevel, itemBig)
	/** Version */
	private var version = 0

	/** Current round's ranking position */
	private var rankingRank = 0

	private var rankingType:Int
		get() = goalType.ordinal+normaType.ordinal*GAMETYPE.entries.size
		set(value) {
			goalType = GAMETYPE.entries[value%GAMETYPE.entries.size]
			normaType = NORMATYPE.entries[value/GAMETYPE.entries.size]
		}
	override val ranking =
		List(RANKING_TYPE) {Leaderboard(rankingMax, serializer<List<Rankable.ScoreRow>>()) {Rankable.ScoreRow()}}

	/* Mode name */
	override val name = "MARATHON ShuttleRun"
	override val gameIntensity = 1
	/* Initialization */
	override fun playerInit(engine:GameEngine) {
		super.playerInit(engine)
		goal = 0
		lastlineTime = 0
		levelTimer = 0
		levelTimeOut = false
		totalTimer = 0
		rollTime = 0
		lastgoal = 0
		lastScore = 0
		lasttimebonus = 0
		regretdispframe = 0
		bgmLv = 0

		rankingRank = -1
		netPlayerInit(engine)

		if(!owner.replayMode) {
			loadSetting(engine, owner.modeConfig)

			version = CURRENT_VERSION
		} else {
			loadSetting(engine, owner.replayProp)
			// NET: Load name
			netPlayerName = owner.replayProp.getProperty("${engine.playerID}.net.netPlayerName", "")
		}

		owner.bgMan.bg = startLevel
		if(owner.bgMan.bg>19) owner.bgMan.bg = 19
		engine.frame = GameEngine.Frame.WHITE
	}

	override fun onSettingChanged(engine:GameEngine) {
		owner.bgMan.bg = minOf(19, startLevel)
		engine.statistics.level = startLevel
		engine.statistics.levelDispAdd = 1
		super.onSettingChanged(engine)
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

		goalmax = normaType.normaQuota(startLevel)
		goal = goalmax

		setSpeed(engine)

		if(netIsWatch)
			owner.musMan.bgm = BGM.Silent
		else {
			setStartBgmlv(engine)
			owner.musMan.bgm = tableBGM[bgmLv]
		}

		if(goalType==GAMETYPE.MIN10_EASY||goalType==GAMETYPE.MIN10_HARD) totalTimer = TIMELIMIT_10MIN
		if(goalType==GAMETYPE.SPECIAL) {
			totalTimer = TIMELIMIT_SPECIAL
			engine.staffrollEnable = true
			engine.staffrollEnableStatistics = true
			engine.staffrollNoDeath = true
		}
	}

	/** Set the gravity rate
	 * @param engine GameEngine
	 */
	override fun setSpeed(engine:GameEngine) {
		engine.speed.replace(tableSpeed[engine.statistics.level])
	}

	/** Set BGM at start of game
	 * @param engine GameEngine
	 */
	private fun setStartBgmlv(engine:GameEngine) {
		bgmLv = 0
		while(tableBGMChange[bgmLv]!=-1&&engine.statistics.level>=tableBGMChange[bgmLv])
			bgmLv++
	}

	fun levelBonus(engine:GameEngine) = maxOf(0, timer*(engine.statistics.level+1))
	/* Render score */
	override fun renderLast(engine:GameEngine) {
		if(owner.menuOnly) return

		receiver.drawScore(
			engine, 0, 0, "SHUTTLE RUN\n(${goalType.longName})", BASE, COLOR.WHITE
		)

		if(engine.isShowRanking) {
			if(!owner.replayMode&&!big&&startLevel==0&&engine.ai==null) {
				val topY = if(receiver.bigSideNext) 6 else 4
				receiver.drawScore(engine, 2, topY-1, "Score", BASE, COLOR.BLUE)
				receiver.drawScore(engine, 7.5f, topY-1, "Lv", BASE, COLOR.BLUE)
				receiver.drawScore(engine, 10, topY-1, "Line", BASE, COLOR.BLUE)
				receiver.drawScore(engine, 15, topY-1, "TIME", BASE, COLOR.BLUE)

				ranking[rankingType].forEachIndexed {i, it ->
					receiver.drawScore(engine, 0, topY+i, "%2d".format(i+1), GRADE, COLOR.YELLOW)
					receiver.drawScore(engine, 2, topY+i, "%7d".format(it.sc), NUM, i==rankingRank)
					receiver.drawScore(engine, 8, topY+i, "%2d".format(it.lv), NUM, i==rankingRank)
					receiver.drawScore(engine, 10, topY+i, "%4d".format(it.li), NUM, i==rankingRank)
					receiver.drawScore(engine, 14, topY+i, it.ti.toTimeStr, NUM, i==rankingRank)
				}
			}
		} else {
			// SCORE
			receiver.drawScore(engine, 0, 3, "Score", BASE, COLOR.BLUE)
			receiver.drawScore(engine, 5, 3, "+$lastScore", NUM)
			val scget = scDisp<engine.statistics.score
			receiver.drawScore(engine, 0, 4, "$scDisp", NUM, scget, 2f)

			// GOAL
			receiver.drawScore(engine, 0, 7, "GOAL", BASE, COLOR.BLUE)
			receiver.drawScore(engine, 4, 7, "%3d".format(goal), NUM, 2f)
			receiver.drawScore(engine, 9, 8, "/%3d".format(goalmax), NUM)
			//if(lastgoal!=0&&scget&&engine.ending==0)
			receiver.drawScore(engine, 1, 8, "-%2d".format(lastgoal), NUM)
			val timerColor = when {
				timer<10*60 -> COLOR.RED
				timer<30*60 -> COLOR.ORANGE
				timer<60*60 -> COLOR.YELLOW
				else -> COLOR.WHITE
			}
			if(goalType==GAMETYPE.SPECIAL) {
				// LEVEL TIME
				receiver.drawScore(engine, 0, 9, "LIMIT", BASE, COLOR.YELLOW)
				receiver.drawScore(engine, 0, 10, timer.toTimeStr, NUM, timerColor, 2f)
			} else {
				// LEVEL BONUS
				receiver.drawScore(engine, 0, 9, "BONUS", BASE, COLOR.BLUE)

				receiver.drawScore(engine, 6, 9, timer.toTimeStr, NUM, timerColor)
				receiver.drawScore(engine, 0, 10, "${levelBonus(engine)}", NUM, timerColor, 2f)
			}
			// LEVEL
			receiver.drawScore(engine, 0, 12, "Level", BASE, COLOR.BLUE)
			receiver.drawScore(engine, 5, 12, (engine.statistics.level+1).toString(), NUM, 2f)

			// TOTAL TIME
			receiver.drawScore(engine, 0, 13, "Time", BASE, COLOR.BLUE)

			val b = goalType==GAMETYPE.MIN10_EASY||goalType==GAMETYPE.MIN10_HARD
			val elapsed = engine.statistics.time.let {if(b) TIMELIMIT_10MIN-it else it}
			receiver.drawScore(
				engine, 0, 14, elapsed.toTimeStr, NUM_T,
				when {
					!b -> COLOR.WHITE
					elapsed<10*60 -> COLOR.RED
					elapsed<30*60 -> COLOR.ORANGE
					elapsed<60*60 -> COLOR.YELLOW
					else -> COLOR.GREEN
				},
			)

			// Ending time
			if(engine.gameActive&&(engine.ending==2||rollTime>0)) {
				val remainRollTime = (TIMELIMIT_ROLL-rollTime).coerceAtLeast(0)

				receiver.drawScore(engine, 0, 15, "ROLL TIME", BASE, COLOR.BLUE)
				receiver.drawScore(
					engine, 0, 16, remainRollTime.toTimeStr, NUM_T, remainRollTime>0&&remainRollTime<10*60
				)
			}

			if(regretdispframe>0)
			// REGRET
				receiver.drawMenu(
					engine, 2, 21, "REGRET", BASE, when {
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
		if(engine.gameActive&&engine.timerActive&&goalType!=GAMETYPE.SPECIAL) {
			levelTimer++
			// Time meter
			engine.meterValue = timer/2f/TIMELIMIT_LEVEL
			engine.meterValue += ((1-engine.meterValue)*(goalmax-goal)*timer/goalmax
				/(TIMELIMIT_LEVEL-lastlineTime))
			engine.meterColor = GameEngine.METER_COLOR_LIMIT

			if(!netIsWatch)
				if(levelTimer>=TIMELIMIT_LEVEL) {
					// Out of time
					levelTimeOut = true
					engine.playSE("timeover")
					if(goalType==GAMETYPE.LV15_HARD||goalType==GAMETYPE.MIN10_HARD) {
						engine.gameEnded()
						engine.stat = Status.GAMEOVER
					} else if(goalType==GAMETYPE.MIN10_EASY||goalType==GAMETYPE.LV15_EASY) {
						regretdispframe = 180
						engine.playSE("regret")
						if(goalType==GAMETYPE.MIN10_EASY) {
							goal = goalmax
							levelTimer = 0
						}
					}
				} else if(timer<=600&&timer%60==0) {
					engine.playSE("countdown")
					if(timer<=300) engine.playSE("countdown${timer/60}")
				} else if(timer==30*60)
					engine.playSE("hurryup")
				else if(timer==60*60) engine.playSE("levelstop")
		}

		// Total Time
		if(engine.gameActive&&engine.timerActive&&goalType!=GAMETYPE.LV15_EASY&&goalType!=GAMETYPE.LV15_HARD) {
			totalTimer--

			// Time meter
			if(goalType==GAMETYPE.SPECIAL) {
				engine.meterValue = totalTimer/(5f*3600)
				engine.meterColor = GameEngine.METER_COLOR_LIMIT
			}

			if(!netIsWatch)
				if(totalTimer<0) {
					// Out of time
					engine.playSE("timeover")
					engine.gameEnded()
					engine.stat = if(goalType==GAMETYPE.MIN10_EASY||goalType==GAMETYPE.MIN10_HARD)
						Status.ENDINGSTART else Status.GAMEOVER

					totalTimer = 0
				} else if(totalTimer<=10*60&&totalTimer%60==0) {
					engine.playSE("countdown")
					if(totalTimer<=300) engine.playSE("countdown${totalTimer/60}")
				}
		}

		// Ending
		if(engine.gameActive&&engine.ending==2) {
			rollTime++

			// Time meter
			val remainRollTime = TIMELIMIT_ROLL-rollTime
			engine.meterValue = remainRollTime*1f/TIMELIMIT_ROLL
			engine.meterColor = GameEngine.METER_COLOR_LEVEL

			// Finished
			if(rollTime>=TIMELIMIT_ROLL&&!netIsWatch) {
				lastScore = totalTimer*2
				engine.statistics.scoreBonus += lastScore
				engine.lastEvent = null
				engine.statistics.rollClear = 2
				engine.gameEnded()
				engine.stat = Status.EXCELLENT
			}
		}
	}

	/* Calculate score */
	override fun calcScore(engine:GameEngine, ev:ScoreEvent):Int {
		// Line clear bonus
		val pts = super.calcScore(engine, ev)
		if(pts>0) lastScore = pts

		when(normaType) {
			NORMATYPE.POINTS -> calcPoint(engine, ev)
			NORMATYPE.LINE -> ev.lines
			NORMATYPE.POWER -> lastPow
			NORMATYPE.SCORE -> pts
		}.also {
			if(it>0) {
				lastgoal = it
				goal -= it
				lastlineTime = levelTimer
				if(normaType!=NORMATYPE.SCORE) engine.receiver.addScore(engine, engine.nowPieceX+2,
					(engine.lastLinesY.maxByOrNull {i -> i.size}?.average()?.toInt()?:engine.nowPieceY)-2,
					it, COLOR.RAINBOW, big = true)
			}
		}
		if(engine.ending==0) {

			// BGM fade-out effects and BGM changes
			if(tableBGMChange[bgmLv]!=-1&&engine.statistics.level==tableBGMChange[bgmLv]-1)
				if(goal in 1..10)
					owner.musMan.fadeSW = true
				else if(goal<=0) {
					bgmLv++
					owner.musMan.bgm = tableBGM[bgmLv]
					owner.musMan.fadeSW = false
				}

			if(goal<=0) {
				// Time bonus
				lasttimebonus = if(!levelTimeOut&&goalType!=GAMETYPE.SPECIAL)
					maxOf(0, (TIMELIMIT_LEVEL-levelTimer)*(engine.statistics.level+1)).also {
						engine.statistics.scoreBonus += it
						engine.receiver.addScore(engine, +2, engine.field.highestBlockY-2, it, COLOR.RAINBOW, "TIME BONUS", true)
					} else if(goalType==GAMETYPE.SPECIAL) normaType.normaTime(engine.statistics.level).also {
					totalTimer += it
					engine.playSE("timebonus_10")
					engine.receiver.addScore(engine, +2, engine.field.highestBlockY-2, it/60, COLOR.RAINBOW, "TIME EXTENSION", true)
				} else 0

				if(engine.statistics.level>=14&&(goalType==GAMETYPE.LV15_EASY||goalType==GAMETYPE.LV15_HARD)) {
					// Ending (LV15-EASY/HARD)
					engine.ending = 1
					engine.statistics.rollClear = 2
					engine.gameEnded()
				} else if(engine.statistics.level>=29&&goalType==GAMETYPE.SPECIAL) {
					// Ending (SPECIAL)
					engine.ending = 2
					engine.timerActive = false
					owner.musMan.bgm = BGM.Ending(0)
					owner.musMan.fadeSW = false
					engine.statistics.rollClear = 1
					(totalTimer*17).let{
						engine.statistics.scoreBonus += it
						engine.receiver.addScore(engine, +2, engine.field.highestBlockY-2, it, COLOR.RAINBOW, "TIME BONUS", true)
					}
					engine.playSE("endingstart")
				} else {
					// Level up
					if(engine.statistics.level<29) engine.statistics.level++

					goalmax = normaType.normaQuota(engine.statistics.level)
					goal += goalmax

					levelTimer = 0
					if(version>=1) engine.holdUsedCount = 0

					setSpeed(engine)
					engine.playSE("levelup")
				}
			}
		}
		return pts
	}

	/* Hard drop */
	override fun afterHardDropFall(engine:GameEngine, fall:Int) = (fall*2).let {
		engine.statistics.scoreHD += it
		scDisp += it
		if(normaType==NORMATYPE.SCORE) {
			lastgoal = it
			goal -= it
			engine.receiver.addScore(engine, engine.nowPieceX+2, engine.nowPieceBottomY+2, 0, str = "+$it")
		}
	}

	/* Soft drop */
	override fun afterSoftDropFall(engine:GameEngine, fall:Int) =fall.let{
		engine.statistics.scoreSD += it
		scDisp += it
		if(normaType==NORMATYPE.SCORE) {
			lastgoal = it
			goal -= it
		}
	}

	override fun onResult(engine:GameEngine):Boolean {
		val b = if(engine.statistics.time<10800) BGM.Result(1) else BGM.Result(2)
		owner.musMan.fadeSW = false
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

		if(netIsPB) receiver.drawMenu(engine, 2, 18, "NEW PB", BASE, COLOR.ORANGE)

		if(netIsNetPlay&&netReplaySendStatus==1)
			receiver.drawMenu(engine, 0, 19, "SENDING...", BASE, COLOR.PINK)
		else if(netIsNetPlay&&!netIsWatch
			&&netReplaySendStatus==2
		)
			receiver.drawMenu(engine, 1, 19, "A: RETRY", BASE, COLOR.RED)
	}

	/** NET: Get goal type */
	override val netGetGoalType get() = rankingType

	/** NET: It returns true when the current settings don't prevent
	 * leaderboard screen from showing. */
	override fun netIsNetRankingViewOK(engine:GameEngine):Boolean = !big&&engine.ai==null&&startLevel==0

	/** NET: Send game options to all spectators
	 * @param engine GameEngine
	 */
	override fun netSendOptions(engine:GameEngine) {
		val msg = "game\toption\t$rankingType\t$startLevel\t$big\n"
		netLobby?.netPlayerClient?.send(msg)
	}

	/** NET: Receive game options */
	override fun netRecvOptions(engine:GameEngine, message:List<String>) {
		rankingType = message[4].toInt()
		startLevel = message[5].toInt()
		big = message[6].toBoolean()
	}

	/** NET: Send various in-game stats of [engine] */
	override fun netSendStats(engine:GameEngine) {
		val bg = if(owner.bgMan.fadeSW) owner.bgMan.nextBg else owner.bgMan.bg
		val msg = "game\tstats\t"+
			engine.run {
				statistics.run {"${scoreLine}\t${scoreSD}\t${scoreHD}\t${scoreBonus}\t${lines}\t${totalPieceLocked}\t${time}\t${level}\t"}+
					"$rankingType\t${gameActive}\t${timerActive}\t$lastScore\t${lastEvent}\t"+
					"$lastgoal\t$lasttimebonus\t$regretdispframe\t$bg\t${meterValue}\t${meterColor}\t$levelTimer\t$totalTimer\t$rollTime\t$goal\n"
			}
		netLobby?.netPlayerClient?.send(msg)
	}

	/** NET: Parse Received [message] as in-game stats of [engine] */
	override fun netRecvStats(engine:GameEngine, message:List<String>) {
		listOf<(String)->Unit>(
			{}, {}, {}, {},
			{engine.statistics.scoreLine = it.toInt()},
			{engine.statistics.scoreSD = it.toInt()},
			{engine.statistics.scoreHD = it.toInt()},
			{engine.statistics.scoreBonus = it.toInt()},
			{engine.statistics.lines = it.toInt()},
			{engine.statistics.totalPieceLocked = it.toInt()},
			{engine.statistics.time = it.toInt()},
			{engine.statistics.level = it.toInt()},
			{rankingType = it.toInt()},
			{engine.gameActive = it.toBoolean()},
			{engine.timerActive = it.toBoolean()},
			{lastScore = it.toInt()},
			{engine.lastEvent = ScoreEvent.parseStr(it)},
			{lastgoal = it.toInt()},
			{lasttimebonus = it.toInt()},
			{regretdispframe = it.toInt()},
			{owner.bgMan.bg = it.toInt()},
			{engine.meterValue = it.toFloat()},
			{engine.meterColor = it.toInt()},
			{levelTimer = it.toInt()},
			{totalTimer = it.toInt()},
			{rollTime = it.toInt()},
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
		netLobby?.netPlayerClient?.send(msg)
	}

	/* Called when saving replay */
	override fun saveReplay(engine:GameEngine, prop:CustomProperties):Boolean {
		saveSetting(engine, prop)

		// NET: Save name
		if(!netPlayerName.isNullOrEmpty()) prop.setProperty("${engine.playerID}.net.netPlayerName", netPlayerName)

		// Update rankings
		if(!owner.replayMode&&!big&&engine.ai==null&&startLevel==0) {
			rankingRank = ranking[rankingType].add(Rankable.ScoreRow(engine.statistics))
			if(rankingRank!=-1) return true
		}
		return false
	}

	companion object {
		/** Current version */
		private const val CURRENT_VERSION = 2

		/** Fall velocity table */
		private val tableSpeed = LevelData(
			listOf(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 3, 4, 5, 7, 10, -1),
			listOf(64, 50, 40, 33, 25, 20, 13, 10, 8, 4, 3, 2, 1, 1, 1, 1, 1, 1, 1, 1),
			16, 16, 8, 30, 14
		)
		/** BGM change levels */
		private val tableBGMChange = intArrayOf(5, 8, 15, 17, 19, -1)
		private val tableBGM = arrayOf(
			BGM.Generic(0), BGM.Generic(1), BGM.Generic(2), BGM.Generic(3), BGM.Generic(4),
			BGM.Generic(5)
		)

		/** Number of ranking types */
		private val RANKING_TYPE = GAMETYPE.entries.size*NORMATYPE.entries.size

		/** Game type names */
		private enum class GAMETYPE(val longName:String, val shortName:String) {
			LV15_EASY("15Levels TIME TRIAL", "15LV T.A."),
			LV15_HARD("15Levels SPEED RUN", "15LV S.R."),
			MIN10_EASY("10Minutes TRIAL", "10MIN.TRY"),
			MIN10_HARD("10Minutes SURVIVAL", "10MIN.SURV"),
			SPECIAL("Unlimited ENDURANCE", "ULM.ENDURO")
		}

		private enum class NORMATYPE(val showName:String, val normaQuota:(Int)->Int, val normaTime:(Int)->Int) {
			POINTS("Points", {10+it*5-it*it/15}, {TIMELIMIT_SPECIAL_BONUS+it*80}),
			LINE("Lines", {4+it+minOf(it,6)-maxOf(0,(it+1)/2-5)}, {TIMELIMIT_SPECIAL_BONUS+it*40}),
			POWER("Spike", {it+it/5+4}, {TIMELIMIT_SPECIAL_BONUS+it*80}),
			SCORE("Score", {it*1000-maxOf(0,it-1)*it*17+4000}, {TIMELIMIT_SPECIAL_BONUS+maxOf(0,it*20-200)})//5k,25k,85k

		}

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
