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

import mu.nu.nullpo.game.component.BGM
import mu.nu.nullpo.game.component.Controller
import mu.nu.nullpo.game.component.LevelData
import mu.nu.nullpo.game.event.EventReceiver.COLOR
import mu.nu.nullpo.game.event.ScoreEvent
import mu.nu.nullpo.game.net.NetUtil
import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.game.subsystem.mode.menu.BooleanMenuItem
import mu.nu.nullpo.game.subsystem.mode.menu.DelegateMenuItem
import mu.nu.nullpo.game.subsystem.mode.menu.LevelMenuItem
import mu.nu.nullpo.game.subsystem.mode.menu.MenuList
import mu.nu.nullpo.util.CustomProperties
import mu.nu.nullpo.util.GeneralUtil.toTimeStr

/** EXTREME Mode */
class MarathonExtreme:NetDummyMode() {
	/** Ending time */
	private var rollTime = 0

	/** Current BGM */
	private var bgmLv = 0

	private val itemLevel = LevelMenuItem("startlevel", "LEVEL", COLOR.RED, 0, 0..19, false, true)
	/** Level at start */
	private var startLevel:Int by DelegateMenuItem(itemLevel)

	private val itemEndless = BooleanMenuItem("endless", "ENDLESS", COLOR.RED, false)
	/** Endless flag */
	private var endless:Boolean by DelegateMenuItem(itemEndless)

	private val itemBig = BooleanMenuItem("big", "BIG", COLOR.RED, false)
	/** BigMode */
	private var big:Boolean by DelegateMenuItem(itemBig)

	/** Version */
	private var version = 0

	/** Current round's ranking position */
	private var rankingRank = 0

	/** Rankings' scores */
	private val rankingScore = List(RANKING_TYPE) {MutableList(rankingMax) {0L}}

	/** Rankings' line counts */
	private val rankingLines = List(RANKING_TYPE) {MutableList(rankingMax) {0}}

	/** Rankings' times */
	private val rankingTime = List(RANKING_TYPE) {MutableList(rankingMax) {-1}}

	override val propRank
		get() = rankMapOf(
			rankingScore.mapIndexed {a, x -> "$a.score" to x}+
				rankingLines.mapIndexed {a, x -> "$a.lines" to x}+
				rankingTime.mapIndexed {a, x -> "$a.time" to x})

	/* Mode name */
	override val name = "Marathon:Extreme"
	override val gameIntensity = 3
	/* Initialization */
	override val menu = MenuList("extreme", itemEndless, itemLevel, itemBig)
	override fun playerInit(engine:GameEngine) {
		super.playerInit(engine)
		engine.owner.receiver.setBGSpd(engine.owner, 2)
		lastScore = 0
		bgmLv = 0
		rollTime = 0

		rankingRank = -1
		rankingScore.forEach {it.fill(0)}
		rankingLines.forEach {it.fill(0)}
		rankingTime.forEach {it.fill(-1)}

		netPlayerInit(engine)
		// NET: Load name
		if(!owner.replayMode) version = CURRENT_VERSION
		else netPlayerName = engine.owner.replayProp.getProperty("${engine.playerID}.net.netPlayerName", "")

		engine.staffrollEnable = true
		engine.staffrollNoDeath = true
		engine.staffrollEnableStatistics = true

		engine.owner.bgMan.bg = startLevel
		engine.frameSkin = GameEngine.FRAME_COLOR_RED
	}

	/** Set the gravity rate
	 * @param engine GameEngine
	 */
	override fun setSpeed(engine:GameEngine) {
		val lv = engine.statistics.level.coerceIn(0, tableSpeed.size-1)
		engine.speed.replace(tableSpeed[lv])
	}

	/* Called at settings screen */
	override fun onSetting(engine:GameEngine):Boolean {
		// NET: Net Ranking
		if(netIsNetRankingDisplayMode)
			netOnUpdateNetPlayRanking(engine, netGetGoalType)
		else if(!engine.owner.replayMode) {
			// Configuration changes
			val change = updateMenu(engine)

			if(change!=0) {
				engine.playSE("change")

				// NET: Signal options change
				if(netIsNetPlay&&netNumSpectators>0) netSendOptions(engine)
			}

			// Confirm
			if(menuTime<5) menuTime++ else if(engine.ctrl.isPush(Controller.BUTTON_A)) {
				engine.playSE("decide")

				// NET: Signal start of the game
				if(netIsNetPlay) netLobby?.netPlayerClient?.send("start1p\n")

				return false
			}

			// Cancel
			if(engine.ctrl.isPush(Controller.BUTTON_B)&&!netIsNetPlay) engine.quitFlag = true

			// NET: Netplay Ranking
			if(engine.ctrl.isPush(Controller.BUTTON_D)&&netIsNetPlay&&startLevel==0&&!big&&engine.ai==null)
				netEnterNetPlayRankingScreen(netGetGoalType)
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

		owner.musMan.bgm = if(netIsWatch) BGM.Silent
		else tableBGM[bgmLv]

		engine.twistAllowKick = true
		engine.twistEnable = true
		engine.useAllSpinBonus = true
		engine.twistEnableEZ = true

		setSpeed(engine)
	}

	override fun renderFirst(engine:GameEngine) {
		if(engine.gameActive&&engine.ending==2) receiver.drawStaffRoll(engine, rollTime*1f/ROLLTIMELIMIT)
	}
	/* Render score */
	override fun renderLast(engine:GameEngine) {
		if(owner.menuOnly) return

		receiver.drawScoreFont(engine, 0, 0, "EXTREME MARATHON!", COLOR.RED)

		if(engine.stat==GameEngine.Status.SETTING||engine.stat==GameEngine.Status.RESULT&&!owner.replayMode) {
			if(!owner.replayMode&&!big&&engine.ai==null) {
				val topY = if(receiver.nextDisplayType==2) 6 else 4
				receiver.drawScoreFont(engine, 2, topY-1, "SCORE LINE TIME", COLOR.RED)

				for(i in 0..<rankingMax) {
					var endlessIndex = 0
					if(endless) endlessIndex = 1

					receiver.drawScoreGrade(
						engine, 0, topY+i, "%02d".format(i+1),
						if(rankingRank==i) COLOR.RAINBOW else if(rankingLines[endlessIndex][i]>=200) COLOR.ORANGE else COLOR.RED
					)
					receiver.drawScoreNum(engine, 2, topY+i, "${rankingScore[endlessIndex][i]}", i==rankingRank)
					receiver.drawScoreNum(engine, 9, topY+i, "${rankingLines[endlessIndex][i]}", i==rankingRank)
					receiver.drawScoreNum(engine, 13, topY+i, rankingTime[endlessIndex][i].toTimeStr, i==rankingRank)
				}
			}
		} else {
			receiver.drawScoreFont(engine, 0, 3, "LINE", COLOR.RED)
			receiver.drawScoreNum(engine, 5, 2, engine.statistics.lines.toString(), 2f)

			receiver.drawScoreFont(engine, 0, 4, "Score", COLOR.RED)
			receiver.drawScoreNum(engine, 5, 4, "+$lastScore")
			val scget = scDisp<engine.statistics.score
			receiver.drawScoreNum(engine, 0, 5, "$scDisp", scget, 2f)
			if(engine.gameActive&&engine.ending==2) {
				val remainRollTime = maxOf(0, ROLLTIMELIMIT-rollTime)

				receiver.drawScoreFont(engine, 0, 7, "ROLL TIME", COLOR.RED)
				receiver.drawScoreNum(engine, 5, 7, remainRollTime.toTimeStr, remainRollTime>0&&remainRollTime<10*60, 2f)
			} else {
				receiver.drawScoreFont(engine, 0, 7, "Level", COLOR.RED)
				receiver.drawScoreNum(
					engine, 5, 7, "%.1f".format(engine.statistics.level.toDouble()+1.0+engine.statistics.lines%10*0.1),
					2f
				)
			}
			receiver.drawScoreFont(engine, 0, 8, "Time", COLOR.RED)
			receiver.drawScoreNum(engine, 0, 9, engine.statistics.time.toTimeStr, 2f)
		}

		super.renderLast(engine)
	}

	/* Called after every frame */
	override fun onLast(engine:GameEngine) {
		super.onLast(engine)
		// Ending
		if(engine.gameActive&&engine.ending==2) {
			rollTime++

			// Time meter
			var remainRollTime = ROLLTIMELIMIT-rollTime
			if(remainRollTime<0) remainRollTime = 0
			engine.meterValue = remainRollTime*1f/ROLLTIMELIMIT
			engine.meterColor = GameEngine.METER_COLOR_GREEN
			if(remainRollTime<=30*60) engine.meterColor = GameEngine.METER_COLOR_YELLOW
			if(remainRollTime<=20*60) engine.meterColor = GameEngine.METER_COLOR_ORANGE
			if(remainRollTime<=10*60) engine.meterColor = GameEngine.METER_COLOR_RED

			// Finished
			if(rollTime>=ROLLTIMELIMIT) {
				engine.gameEnded()
				engine.resetStatc()
				engine.stat = GameEngine.Status.EXCELLENT
			}
		}
	}

	fun nextbgmLine(lines:Int) =
		tableBGMChange.firstOrNull {lines<it} ?: if(endless) lines+20 else 200

	fun bgmLv(lines:Int) =
		tableBGMChange.indexOfFirst {lines<it}.let {if(it<0) tableBGMChange.size else it}
	/* Calculate score */
	override fun calcScore(engine:GameEngine, ev:ScoreEvent):Int {
		super.calcScore(engine, ev)
		calcPower(engine, ev, true)
		if(engine.ending==0) {
			// BGM fade-out effects and BGM changes

			if(engine.statistics.lines>=nextbgmLine(engine.statistics.lines)-5) owner.musMan.fadeSW = true
			val newbgm = bgmLv(engine.statistics.lines).coerceIn(0, tableBGM.size-1)
			if(bgmLv!=newbgm) {
				bgmLv = newbgm
				owner.musMan.bgm = tableBGM[bgmLv]
				owner.musMan.fadeSW = false
			}

			// Meter
			engine.meterValue = engine.statistics.lines%10/9f
			engine.meterColor = GameEngine.METER_COLOR_LEVEL

			if(engine.statistics.lines>=200&&!endless) {
				// Ending
				engine.playSE("levelup")
				engine.playSE("endingstart")
				owner.musMan.fadeSW = false
				owner.musMan.bgm = BGM.Ending(2)
				engine.bone = true
				engine.ending = 2
				engine.timerActive = false
			} else if(engine.statistics.lines>=(engine.statistics.level+1)*10&&engine.statistics.level<19) {
				// Level up
				engine.statistics.level++

				owner.bgMan.nextBg = engine.statistics.level

				setSpeed(engine)
				engine.playSE("levelup")
			}
		}
		return if(ev.lines>0) lastScore else 0
	}

	override fun onResult(engine:GameEngine):Boolean {
		val b = if(engine.ending==0) BGM.Result(0) else BGM.Result(3)
		owner.musMan.fadeSW = false
		owner.musMan.bgm = b

		// Page change
		if(engine.ctrl.isMenuRepeatKey(Controller.BUTTON_UP)) {
			engine.statc[1]--
			if(engine.statc[1]<0) engine.statc[1] = 1
			engine.playSE("change")
		}
		if(engine.ctrl.isMenuRepeatKey(Controller.BUTTON_DOWN)) {
			engine.statc[1]++
			if(engine.statc[1]>1) engine.statc[1] = 0
			engine.playSE("change")
		}

		return super.onResult(engine)
	}

	/* Render results screen */
	override fun renderResult(engine:GameEngine) {
		when {
			engine.statc[1]==0 -> drawResultStats(
				engine, receiver, 0, COLOR.RED, Statistic.SCORE, Statistic.LINES, Statistic.TIME,
				Statistic.PIECE, Statistic.ATTACKS, Statistic.SPL
			)
			engine.statc[1]==1 -> drawResultStats(
				engine, receiver, 0, COLOR.RED, Statistic.SPM, Statistic.LPM, Statistic.TIME,
				Statistic.PPS, Statistic.APM, Statistic.SPL
			)
		}
		drawResultRank(engine, receiver, 12, COLOR.RED, rankingRank)
		drawResultNetRank(engine, receiver, 14, COLOR.RED, netRankingRank[0])
		drawResultNetRankDaily(engine, receiver, 16, COLOR.RED, netRankingRank[1])

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
		saveSetting(engine, prop)

		// NET: Save name
		if(netPlayerName!=null&&netPlayerName!!.isNotEmpty()) prop.setProperty(
			"${engine.playerID}.net.netPlayerName",
			netPlayerName
		)

		// Update rankings
		if(!owner.replayMode&&!big&&engine.ai==null) {
			//owner.statsProp.setProperty("decoration", decoration)
			updateRanking(engine.statistics.score, engine.statistics.lines, engine.statistics.time, endless)

			if(rankingRank!=-1) return true
		}
		return false
	}

	override fun loadSetting(engine:GameEngine, prop:CustomProperties, ruleName:String, playerID:Int) {
		startLevel = prop.getProperty("extreme.startLevel", 0)
		endless = prop.getProperty("extreme.endless", false)
		big = prop.getProperty("extreme.big", false)
		version = prop.getProperty("extreme.version", 0)
	}

	override fun saveSetting(engine:GameEngine, prop:CustomProperties, ruleName:String, playerID:Int) {
		prop.setProperty("extreme.startLevel", startLevel)
		prop.setProperty("extreme.endless", endless)
		prop.setProperty("extreme.big", big)
		prop.setProperty("extreme.version", version)
	}

	/** Update rankings
	 * @param sc Score
	 * @param li Lines
	 * @param time Time
	 */
	private fun updateRanking(sc:Long, li:Int, time:Int, endlessMode:Boolean) {
		rankingRank = checkRanking(sc, li, time, endlessMode)

		if(rankingRank!=-1) {
			var endlessIndex = 0
			if(endlessMode) endlessIndex = 1

			// Shift down ranking entries
			for(i in rankingMax-1 downTo rankingRank+1) {
				rankingScore[endlessIndex][i] = rankingScore[endlessIndex][i-1]
				rankingLines[endlessIndex][i] = rankingLines[endlessIndex][i-1]
				rankingTime[endlessIndex][i] = rankingTime[endlessIndex][i-1]
			}

			// Add new data
			rankingScore[endlessIndex][rankingRank] = sc
			rankingLines[endlessIndex][rankingRank] = li
			rankingTime[endlessIndex][rankingRank] = time
		}
	}

	/** Calculate ranking position
	 * @param sc Score
	 * @param li Lines
	 * @param time Time
	 * @return Position (-1 if unranked)
	 */
	private fun checkRanking(sc:Long, li:Int, time:Int, endlessMode:Boolean):Int {
		var endlessIndex = 0
		if(endlessMode) endlessIndex = 1

		for(i in 0..<rankingMax)
			if(sc>rankingScore[endlessIndex][i])
				return i
			else if(sc==rankingScore[endlessIndex][i]&&li>rankingLines[endlessIndex][i])
				return i
			else if(sc==rankingScore[endlessIndex][i]&&li==rankingLines[endlessIndex][i]
				&&time<rankingTime[endlessIndex][i]
			)
				return i

		return -1
	}

	/** NET: Send various in-game stats of [engine] */
	override fun netSendStats(engine:GameEngine) {
		val bg = if(engine.owner.bgMan.fadeSW) engine.owner.bgMan.nextBg else engine.owner.bgMan.bg
		val msg = "game\tstats\t"+engine.run {
			statistics.run {
				"${scoreLine}\t${scoreSD}\t${scoreHD}\t${scoreBonus}\t${lines}\t${totalPieceLocked}\t${time}\t${level}\t$endless\t"
			}+"${gameActive}\t${timerActive}\t$lastScore\t$scDisp\t${lastEvent}\t$bg\t$rollTime\n"
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
			{endless = it.toBoolean()},
			{engine.gameActive = it.toBoolean()},
			{engine.timerActive = it.toBoolean()},
			{lastScore = it.toInt()},
			{/*scDisp = it.toInt()*/},
			{engine.lastEvent = ScoreEvent.parseStr(it)},
			{engine.owner.bgMan.bg = it.toInt()},
			{rollTime = it.toInt()}).zip(message).forEach {(x, y) ->
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
			"SCORE;${score}\tLINE;${lines}\tLEVEL;${(level+levelDispAdd)}\tTIME;${time.toTimeStr}\t"+
				"SCORE/LINE;${spl}\tLINE/MIN;${lpm}\t"
		}

		val msg = "gstat1p\t${NetUtil.urlEncode(subMsg)}\n"
		netLobby?.netPlayerClient?.send(msg)
	}

	/** NET: Send game options to all spectators
	 * @param engine GameEngine
	 */
	override fun netSendOptions(engine:GameEngine) {
		val msg = "game\toption\t$startLevel\t$endless\t$big\n"
		netLobby?.netPlayerClient?.send(msg)
	}

	/** NET: Receive game options */
	override fun netRecvOptions(engine:GameEngine, message:List<String>) {
		startLevel = message[4].toInt()
		endless = message[5].toBoolean()
		big = message[6].toBoolean()
	}

	/** NET: Get goal type */
	override val netGetGoalType get() = if(endless) 1 else 0

	/** NET: It returns true when the current settings don't prevent leaderboard screen from showing.*/
	override fun netIsNetRankingViewOK(engine:GameEngine):Boolean = startLevel==0&&!big&&engine.ai==null

	companion object {
		/** Current version */
		private const val CURRENT_VERSION = 1

		/** Ending time */
		private const val ROLLTIMELIMIT = 3238
		private val tableSpeed = LevelData(
			listOf(25, 24, 23, 22, 21, 20, 18, 16, 14, 12, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1)/*ARE*/,
			listOf(20, 18, 16, 14, 12, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1, 1, 1, 1, 1, 1)/*ARE Lines*/,
			listOf(30, 25, 20, 17, 14, 11, 9, 8, 7, 6, 5, 5, 5, 5, 5, 4, 3, 2, 1, 0)/*Line Delay*/,
			//   50,43,36,31,26,21,18,16,14,12,10, 9, 8, 7, 6, 5, 4, 3, 2, 1
			listOf(30, 29, 28, 27, 27, 26, 26, 25, 25, 24, 24, 23, 23, 22, 22, 22, 21, 21, 21, 20)/*Lock Delay*/,
			listOf(10, 10, 10, 9, 9, 9, 8, 8, 8, 7, 7, 7, 6, 6, 6, 5, 5, 5, 4, 4)/*DAS*/

		)

		/** Line counts when BGM changes occur */
		private val tableBGMChange = listOf(20, 40, 60, 80, 110, 140, 170)
		private val tableBGM = listOf(
			BGM.Rush(0), BGM.Generic(6), BGM.Generic(7), BGM.Rush(1), BGM.Generic(8),
			BGM.Generic(9), BGM.Rush(2), BGM.Rush(3)
		)

		/** Number of ranking types */
		private const val RANKING_TYPE = 2
	}
}
