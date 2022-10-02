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

/** MARATHON Mode */
class Marathon:NetDummyMode() {

	/** Current BGM */
	private var bgmLv = 0

	private val itemLevel = LevelMenuItem("startlevel", "LEVEL", COLOR.RED, 0, 0..19, false, true)
	/** Level at start */
	private var startLevel:Int by DelegateMenuItem(itemLevel)

	private val itemMode = StringsMenuItem(
		"goalType", "GOAL", COLOR.BLUE, 0,
		tableGameClearLines.map {if(it<=0) "ENDLESS" else "$it LINES"}
	)
	/** Game type  */
	private var goalType:Int by DelegateMenuItem(itemMode)

	private val itemBig = BooleanMenuItem("big", "BIG", COLOR.BLUE, false)
	/** BigMode */
	private var big:Boolean by DelegateMenuItem(itemBig)

	override val menu = MenuList("marathon", itemMode, itemLevel, itemBig)
	/** Version */
	private var version = 0

	/** Current round's ranking position */
	private var rankingRank = -1

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

	// Mode name
	override val name = "Marathon"

	/* Initialization */
	override fun playerInit(engine:GameEngine) {
		super.playerInit(engine)
		lastscore = 0
		bgmLv = 0

		rankingRank = -1
		rankingScore.forEach {it.fill(0L)}
		rankingLines.forEach {it.fill(0)}
		rankingTime.forEach {it.fill(0)}

		netPlayerInit(engine)

		if(!owner.replayMode) version = CURRENT_VERSION else {
			if(version==0&&owner.replayProp.getProperty("marathon.endless", false)) goalType = 2

			// NET: Load name
			netPlayerName = engine.owner.replayProp.getProperty("${engine.playerID}.net.netPlayerName", "")
		}

		engine.owner.bgMan.bg = startLevel
		engine.frameColor = GameEngine.FRAME_COLOR_GREEN
	}

	/** Set the gravity rate
	 * @param engine GameEngine
	 */
	fun setSpeed(engine:GameEngine) {
		val ln = (engine.statistics.lines+startLevel*10)/if(tableGameClearLines[goalType]<=200) 1 else 4

		val lv = maxOf(
			0,
			minOf(
				if(tableGameClearLines[goalType]<=200) engine.statistics.level else engine.statistics.level*2/5,
				tableGravity.size-1
			)
		)
		engine.speed.gravity = tableGravity[lv]
		engine.speed.denominator = tableDenominator[lv]
		engine.speed.are = maxOf(0, minOf(20-ln/30, 50-ln/3))
		engine.speed.areLine = maxOf(0, minOf(20-ln/30, 50-ln/3))
		engine.speed.lineDelay = maxOf(0, minOf(30-ln/10, 35-ln/5, 50-ln/3))
		engine.speed.lockDelay = maxOf(18, minOf(48, 60-ln/7))
		engine.speed.das = maxOf(6, minOf(14, 21-ln/10))
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

				if(startLevel>(tableGameClearLines[goalType]-1)/10&&tableGameClearLines[goalType]>=0) {
					startLevel = (tableGameClearLines[goalType]-1)/10
					engine.owner.bgMan.bg = startLevel
				}

				engine.owner.bgMan.bg = startLevel
				engine.statistics.level = startLevel
				engine.frameColor = (1+startLevel)%GameEngine.FRAME_COLOR_ALL
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
			if(engine.ctrl.isPush(Controller.BUTTON_D)&&netIsNetPlay&&startLevel==0&&!big&&engine.ai==null)
				netEnterNetPlayRankingScreen(goalType)

		} else {
			menuTime++
			menuCursor = -1

			return menuTime<60
		}// Replay

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

		setSpeed(engine)

		owner.musMan.bgm = if(netIsWatch) BGM.Silent else BGM.Generic(bgmLv)
		engine.frameColor = (1+startLevel)%GameEngine.FRAME_COLOR_ALL
	}

	/* Render score */
	override fun renderLast(engine:GameEngine) {
		if(owner.menuOnly) return
		if(tableGameClearLines[goalType]<=0)
			receiver.drawScoreFont(engine, 0, 0, "ENDLESS MARATHON", COLOR.GREEN)
		else
			receiver.drawScoreFont(
				engine, 0, 0, "${tableGameClearLines[goalType]} LINES MARATHON",
				COLOR.GREEN
			)

		if(engine.stat==GameEngine.Status.SETTING||engine.stat==GameEngine.Status.RESULT&&!owner.replayMode) {
			if(!owner.replayMode&&!big&&engine.ai==null) {
				val scale = if(receiver.nextDisplayType==2) .5f else 1f
				val topY = if(receiver.nextDisplayType==2) 6 else 4
				receiver.drawScoreFont(engine, 3, topY-1, "SCORE  LINE TIME", COLOR.BLUE, scale)

				for(i in 0 until RANKING_MAX) {
					receiver.drawScoreGrade(
						engine, 0, topY+i, String.format("%2d", i+1),
						if(rankingRank==i) COLOR.RAINBOW else
							if(rankingLines[goalType][i]>tableGameClearLines[goalType]) COLOR.CYAN else COLOR.YELLOW,
						scale
					)
					receiver.drawScoreNum(engine, 3, topY+i, "${rankingScore[goalType][i]}", i==rankingRank, scale)
					receiver.drawScoreNum(engine, 10, topY+i, "${rankingLines[goalType][i]}", i==rankingRank, scale)
					receiver.drawScoreNum(engine, 15, topY+i, rankingTime[goalType][i].toTimeStr, i==rankingRank, scale)
				}
			}
		} else {
			receiver.drawScoreFont(engine, 0, 3, "Lines", COLOR.BLUE)
			receiver.drawScoreNum(engine, 5, 2, engine.statistics.lines.toString(), 2f)

			receiver.drawScoreFont(engine, 0, 4, "Score", COLOR.BLUE)
			receiver.drawScoreNum(engine, 5, 4, "+$lastscore")
			val scget = scDisp<engine.statistics.score
			receiver.drawScoreNum(engine, 0, 5, "$scDisp", scget, 2f)

			receiver.drawScoreFont(engine, 0, 8, "Level", COLOR.BLUE)
			receiver.drawScoreNum(
				engine, 5, 8, String.format(
					"%.1f", engine.statistics.level.toFloat()+
						if(engine.statistics.level>=19&&tableGameClearLines[goalType]<0) 1f else engine.statistics.lines%10*0.1f+1f
				), 2f
			)

			receiver.drawScoreFont(engine, 0, 9, "Time", COLOR.BLUE)
			receiver.drawScoreNum(engine, 0, 10, engine.statistics.time.toTimeStr, 2f)
		}

		super.renderLast(engine)
	}

	fun nextbgmLine(lines:Int) = tableBGMChange[goalType].firstOrNull {lines<=it} ?: tableGameClearLines[goalType]
	fun bgmLv(lines:Int) =
		tableBGMChange[goalType].indexOfFirst {lines<=it}.let {if(it<0) tableBGMChange[goalType].size else it}
	/* Calculate score */
	override fun calcScore(engine:GameEngine, ev:ScoreEvent):Int {
		super.calcScore(engine, ev)
		// BGM fade-out effects and BGM changes
		if(engine.statistics.lines>=nextbgmLine(engine.statistics.lines)-5) owner.musMan.fadesw = true
		val newbgm = minOf(maxOf(0, bgmLv(engine.statistics.lines)), 8)
		if(bgmLv!=newbgm) {
			bgmLv = newbgm
			owner.musMan.bgm = BGM.Generic(bgmLv)
			owner.musMan.fadesw = false
		}

		// Meter
		engine.meterValue = engine.statistics.lines%10/9f
		engine.meterColor = GameEngine.METER_COLOR_LEVEL

		if(engine.statistics.lines>=tableGameClearLines[goalType]&&tableGameClearLines[goalType]>=0) {
			// Ending
			engine.ending = 1
			engine.gameEnded()
		} else if(engine.statistics.lines>=(engine.statistics.level+1)*10&&engine.statistics.level<tableGameClearLines[goalType]/10) {
			// Level up
			engine.statistics.level++
			engine.frameColor = (1+engine.statistics.level)%GameEngine.FRAME_COLOR_ALL
			owner.bgMan.fadesw = true
			owner.bgMan.fadecount = 0
			owner.bgMan.fadebg = engine.statistics.level

			setSpeed(engine)
			engine.playSE("levelup")
		}
		return if(ev.lines>0) lastscore else 0
	}

	/* Soft drop */
	override fun afterSoftDropFall(engine:GameEngine, fall:Int) {
		engine.statistics.scoreSD += fall
		scDisp += fall
	}

	/* Hard drop */
	override fun afterHardDropFall(engine:GameEngine, fall:Int) {
		engine.statistics.scoreHD += fall*2
		scDisp += fall*2
	}

	override fun onResult(engine:GameEngine):Boolean {
		val b = if(engine.ending==0) BGM.Result(1) else BGM.Result(2)
		owner.musMan.fadesw = false
		owner.musMan.bgm = b

		return super.onResult(engine)
	}

	/* Render results screen */
	override fun renderResult(engine:GameEngine) {
		drawResultStats(
			engine, receiver, 0, COLOR.BLUE, Statistic.SCORE, Statistic.LINES, Statistic.TIME, Statistic.SPM,
			Statistic.LPM, Statistic.SPL
		)
		drawResultRank(engine, receiver, 12, COLOR.BLUE, rankingRank)
		drawResultNetRank(engine, receiver, 14, COLOR.BLUE, netRankingRank[0])
		drawResultNetRankDaily(engine, receiver, 16, COLOR.BLUE, netRankingRank[1])

		if(netIsPB) receiver.drawMenuFont(engine, 2, 21, "NEW PB", COLOR.ORANGE)

		if(netIsNetPlay&&netReplaySendStatus==1)
			receiver.drawMenuFont(engine, 0, 22, "SENDING...", COLOR.PINK)
		else if(netIsNetPlay&&!netIsWatch
			&&netReplaySendStatus==2
		)
			receiver.drawMenuFont(engine, 1, 22, "A: RETRY", COLOR.RED)
	}

	/* Called when saving replay */
	override fun saveReplay(engine:GameEngine, prop:CustomProperties):Boolean {
		saveSetting(prop, engine)

		// NET: Save name
		if(netPlayerName!=null&&netPlayerName!!.isNotEmpty()) prop.setProperty(
			"${engine.playerID}.net.netPlayerName",
			netPlayerName
		)

		// Update rankings
		if(!owner.replayMode&&!big&&engine.ai==null) {
			updateRanking(engine.statistics.score, engine.statistics.lines, engine.statistics.time, goalType)

			if(rankingRank!=-1) return true
		}
		return false
	}

	/** Update rankings
	 * @param sc Score
	 * @param li Lines
	 * @param time Time
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

	/** NET: Send various in-game stats of [engine] */
	override fun netSendStats(engine:GameEngine) {
		val bg =
			if(engine.owner.bgMan.fadesw) engine.owner.bgMan.fadebg else engine.owner.bgMan.bg
		val msg = "game\tstats\t"+engine.run {
			statistics.run {
				"${scoreLine}\t${scoreSD}\t${scoreHD}\t${scoreBonus}\t${lines}\t${totalPieceLocked}\t${time}\t${level}\t"
			}+"$goalType\t${gameActive}\t${timerActive}\t$lastscore\t$scDisp\t${lastEvent}\t$bg\n"
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
			{/*scDisp = it.toInt()*/},
			{engine.lastEvent = ScoreEvent.parseStr(it)},
			{engine.owner.bgMan.bg = it.toInt()}).zip(message).forEach {(x, y) ->
			x(y)
		}

		// Meter
		engine.meterValue = engine.statistics.lines%10/9f
		engine.meterColor = GameEngine.METER_COLOR_LEVEL
	}

	/** NET: Send end-of-game stats
	 * @param engine GameEngine
	 */
	override fun netSendEndGameStats(engine:GameEngine) {
		val subMsg = engine.statistics.run {
			"SCORE;${score}\tLINE;${lines}\tLEVEL;${level+levelDispAdd}\tTIME;${time.toTimeStr}\tSCORE/LINE;${spl}\tLINE/MIN;${lpm}\t"
		}

		val msg = "gstat1p\t${NetUtil.urlEncode(subMsg)}\n"
		netLobby!!.netPlayerClient!!.send(msg)
	}

	/** NET: Send game options to all spectators
	 * @param engine GameEngine
	 */
	override fun netSendOptions(engine:GameEngine) {
		val msg = "game\toption\t$startLevel\t$goalType\t$big\n"
		netLobby!!.netPlayerClient!!.send(msg)
	}

	/** NET: Receive game options */
	override fun netRecvOptions(engine:GameEngine, message:List<String>) {
		startLevel = message[4].toInt()
		goalType = message[5].toInt()
		big = message[6].toBoolean()
	}

	/** NET: Get goal type */
	override fun netGetGoalType():Int = goalType

	/** NET: It returns true when the current settings don't prevent leaderboard screen from showing. */
	override fun netIsNetRankingViewOK(engine:GameEngine):Boolean = startLevel==0&&!big&&engine.ai==null

	companion object {
		/** Current version */
		private const val CURRENT_VERSION = 2

		/** Fall velocity table (numerators) */
		private val tableGravity = listOf(1, 2, 1, 3, 3, 7, 3, 12, 30, 26, 26, 3, 1, 3, 2, 3, 5, 15, 10, -1)

		/** Fall velocity table (denominators) */
		private val tableDenominator = listOf(60, 95, 37, 85, 64, 110, 34, 97, 169, 100, 67, 5, 1, 2, 1, 1, 1, 2, 1, 1)

		/** Line counts when BGM changes occur */
		private val tableBGMChange = listOf(
			listOf(30, 60, 90, 120),
			listOf(30, 60, 90, 120, 140, 160, 180),
			listOf(110, 220, 330, 440, 550, 660, 770, 880)
		)

		/** Line counts when game ending occurs */
		private val tableGameClearLines = listOf(150, 200, 999, -1)

		/** Number of entries in rankings */
		private const val RANKING_MAX = 13

		/** Number of ranking types */
		private val RANKING_TYPE = tableGameClearLines.size

		/** Number of game types */
		private val GOALTYPE_MAX = tableGameClearLines.size
	}
}
