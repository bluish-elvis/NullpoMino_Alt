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
import mu.nu.nullpo.game.component.Controller
import mu.nu.nullpo.game.event.EventReceiver.COLOR
import mu.nu.nullpo.game.event.ScoreEvent
import mu.nu.nullpo.game.net.NetUtil
import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.game.subsystem.mode.menu.*
import mu.nu.nullpo.util.CustomProperties
import mu.nu.nullpo.util.GeneralUtil.toTimeStr

/** MARATHON Mode */
class Marathon:NetDummyMode() {

	/** Current BGM */
	private var bgmlv = 0

	private val itemLevel = LevelMenuItem("startlevel", "LEVEL", COLOR.RED, 0, 0..19, false, true)
	/** Level at start */
	private var startLevel:Int by DelegateMenuItem(itemLevel)

	private val itemMode = StringsMenuItem(
		"goaltype", "GOAL", COLOR.BLUE, 0,
		tableGameClearLines.map {if(it<=0) "ENDLESS" else "$it LINES"}.toTypedArray()
	)
	/** Game type  */
	private var goaltype:Int by DelegateMenuItem(itemMode)

	private val itemBig = BooleanMenuItem("big", "BIG", COLOR.BLUE, false)
	/** BigMode */
	private var big:Boolean by DelegateMenuItem(itemBig)

	override val menu = MenuList("marathon", itemMode, itemLevel, itemBig)
	/** Version */
	private var version = 0

	/** Current round's ranking rank */
	private var rankingRank:Int = -1

	/** Rankings' scores */
	private var rankingScore:Array<IntArray> = Array(RANKING_TYPE) {IntArray(RANKING_MAX)}

	/** Rankings' line counts */
	private var rankingLines:Array<IntArray> = Array(RANKING_TYPE) {IntArray(RANKING_MAX)}

	/** Rankings' times */
	private var rankingTime:Array<IntArray> = Array(RANKING_TYPE) {IntArray(RANKING_MAX)}

	override val rankMap:Map<String, IntArray>
		get() = mapOf(
			*(
				(rankingScore.mapIndexed {a, x -> "$a.score" to x}+
					rankingLines.mapIndexed {a, x -> "$a.lines" to x}+
					rankingTime.mapIndexed {a, x -> "$a.time" to x}).toTypedArray())
		)
	// Mode name
	override val name = "Marathon"

	/* Initialization */
	override fun playerInit(engine:GameEngine, playerID:Int) {
		super.playerInit(engine, playerID)
		lastscore = 0
		bgmlv = 0

		rankingRank = -1
		rankingScore = Array(RANKING_TYPE) {IntArray(RANKING_MAX)}
		rankingLines = Array(RANKING_TYPE) {IntArray(RANKING_MAX)}
		rankingTime = Array(RANKING_TYPE) {IntArray(RANKING_MAX)}

		netPlayerInit(engine, playerID)

		if(!owner.replayMode) version = CURRENT_VERSION else {
			if(version==0&&owner.replayProp.getProperty("marathon.endless", false)) goaltype = 2

			// NET: Load name
			netPlayerName = engine.owner.replayProp.getProperty("$playerID.net.netPlayerName", "")
		}

		engine.owner.backgroundStatus.bg = startLevel
		engine.framecolor = GameEngine.FRAME_COLOR_GREEN
	}

	/** Set the gravity rate
	 * @param engine GameEngine
	 */
	fun setSpeed(engine:GameEngine) {
		val ln = (engine.statistics.lines+startLevel*10)/if(tableGameClearLines[goaltype]<=200) 1 else 4

		val lv = maxOf(
			0,
			minOf(
				if(tableGameClearLines[goaltype]<=200) engine.statistics.level else engine.statistics.level*2/5,
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
	override fun onSetting(engine:GameEngine, playerID:Int):Boolean {
		// NET: Net Ranking
		if(netIsNetRankingDisplayMode)
			netOnUpdateNetPlayRanking(engine, goaltype)
		else if(!engine.owner.replayMode) {
			// Configuration changes
			val change = updateMenu(engine)

			if(change!=0) {
				engine.playSE("change")

				when(menuCursor) {
					0 -> {
						goaltype += change
						if(goaltype<0) goaltype = GOALTYPE_MAX-1
						if(goaltype>GOALTYPE_MAX-1) goaltype = 0

						if(startLevel>(tableGameClearLines[goaltype]-1)/10&&tableGameClearLines[goaltype]>=0) {
							startLevel = (tableGameClearLines[goaltype]-1)/10
							engine.owner.backgroundStatus.bg = startLevel
						}
					}
					1 -> {
						startLevel += change
						if(tableGameClearLines[goaltype]>=0) {
							if(startLevel<0) startLevel = (tableGameClearLines[goaltype]-1)/10
							if(startLevel>(tableGameClearLines[goaltype]-1)/10) startLevel = 0
						} else {
							if(startLevel<0) startLevel = 19
							if(startLevel>19) startLevel = 0
						}
						engine.owner.backgroundStatus.bg = startLevel
						engine.statistics.level = startLevel
						setSpeed(engine)
					}
					2 -> big = !big
				}

				// NET: Signal options change
				if(netIsNetPlay&&netNumSpectators>0) netSendOptions(engine)
			}

			// Confirm
			if(engine.ctrl.isPush(Controller.BUTTON_A)&&menuTime>=5) {
				engine.playSE("decide")

				// NET: Signal start of the game
				if(netIsNetPlay) netLobby!!.netPlayerClient!!.send("start1p\n")

				return false
			}

			// Cancel
			if(engine.ctrl.isPush(Controller.BUTTON_B)&&!netIsNetPlay) engine.quitflag = true

			// NET: Netplay Ranking
			if(engine.ctrl.isPush(Controller.BUTTON_D)&&netIsNetPlay&&startLevel==0&&!big&&
				engine.ai==null
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

	/* Render the settings screen */
	override fun renderSetting(engine:GameEngine, playerID:Int) {
		if(netIsNetRankingDisplayMode)
		// NET: Netplay Ranking
			netOnRenderNetPlayRanking(engine, playerID, receiver)
		else {
			drawMenu(
				engine, playerID, receiver, 0, COLOR.BLUE, 0,
				"GOAL" to if(tableGameClearLines[goaltype]<=0) "ENDLESS" else "${tableGameClearLines[goaltype]} LINES"
			)
			drawMenuCompact(engine, playerID, receiver, "Level" to startLevel+1)
			drawMenuSpeeds(engine, playerID, receiver, 4, COLOR.WHITE, 10)
			drawMenuCompact(engine, playerID, receiver, 9, COLOR.BLUE, 2, "BIG" to big)
		}
	}

	/* Called for initialization during "Ready" screen */
	override fun startGame(engine:GameEngine, playerID:Int) {
		engine.statistics.level = startLevel
		engine.statistics.levelDispAdd = 1
		engine.b2bEnable = true
		engine.splitb2b = true
		engine.comboType = GameEngine.COMBO_TYPE_NORMAL
		engine.big = big

		engine.twistAllowKick = true
		engine.twistEnable = true
		engine.useAllSpinBonus = true

		setSpeed(engine)

		owner.bgmStatus.bgm = if(netIsWatch) BGM.Silent else BGM.Generic(bgmlv)
	}

	/* Render score */
	override fun renderLast(engine:GameEngine, playerID:Int) {
		if(owner.menuOnly) return
		if(tableGameClearLines[goaltype]<=0)
			receiver.drawScoreFont(engine, playerID, 0, 0, "ENDLESS MARATHON", COLOR.GREEN)
		else
			receiver.drawScoreFont(
				engine, playerID, 0, 0,
				"${tableGameClearLines[goaltype]} LINES MARATHON", COLOR.GREEN
			)

		if(engine.stat==GameEngine.Status.SETTING||engine.stat==GameEngine.Status.RESULT&&!owner.replayMode) {
			if(!owner.replayMode&&!big&&engine.ai==null) {
				val scale = if(receiver.nextDisplayType==2) .5f else 1f
				val topY = if(receiver.nextDisplayType==2) 6 else 4
				receiver.drawScoreFont(engine, playerID, 3, topY-1, "SCORE  LINE TIME", COLOR.BLUE, scale)

				for(i in 0 until RANKING_MAX) {
					receiver.drawScoreGrade(
						engine, playerID, 0, topY+i, String.format("%2d", i+1),
						if(rankingRank==i) COLOR.RAINBOW else if(rankingLines[goaltype][i]>tableGameClearLines[goaltype]) COLOR.CYAN else COLOR.YELLOW,
						scale
					)
					receiver.drawScoreNum(engine, playerID, 3, topY+i, "${rankingScore[goaltype][i]}", i==rankingRank, scale)
					receiver.drawScoreNum(engine, playerID, 10, topY+i, "${rankingLines[goaltype][i]}", i==rankingRank, scale)
					receiver.drawScoreNum(engine, playerID, 15, topY+i, rankingTime[goaltype][i].toTimeStr, i==rankingRank, scale)
				}
			}
		} else {
			receiver.drawScoreFont(engine, playerID, 0, 3, "LINE", COLOR.BLUE)
			receiver.drawScoreNum(engine, playerID, 5, 2, engine.statistics.lines.toString(), 2f)

			receiver.drawScoreFont(engine, playerID, 0, 4, "Score", COLOR.BLUE)
			receiver.drawScoreNum(engine, playerID, 5, 4, "+$lastscore")
			val scget = scDisp<engine.statistics.score
			receiver.drawScoreNum(engine, playerID, 0, 5, "$scDisp", scget, 2f)

			receiver.drawScoreFont(engine, playerID, 0, 8, "Level", COLOR.BLUE)
			receiver.drawScoreNum(
				engine, playerID, 5, 8, String.format(
					"%.1f", engine.statistics.level.toFloat()+
						if(engine.statistics.level>=19&&tableGameClearLines[goaltype]<0) 1f else engine.statistics.lines%10*0.1f+1f
				), 2f
			)

			receiver.drawScoreFont(engine, playerID, 0, 9, "Time", COLOR.BLUE)
			receiver.drawScoreNum(engine, playerID, 0, 10, engine.statistics.time.toTimeStr, 2f)
		}

		super.renderLast(engine, playerID)
	}

	fun nextbgmLine(lines:Int) = tableBGMChange[goaltype].firstOrNull {lines<=it} ?: tableGameClearLines[goaltype]
	fun bgmlv(lines:Int) =
		tableBGMChange[goaltype].indexOfFirst {lines<=it}.let {if(it<0) tableBGMChange[goaltype].size else it}
	/* Calculate score */
	override fun calcScore(engine:GameEngine, playerID:Int, lines:Int):Int {
		// Line clear bonus
		val pts = calcScoreBase(engine, lines)
		val cmb = if(engine.combo>=1&&lines>=1) engine.combo-1 else 0
		// Combo
		val spd = maxOf(0, engine.lockDelay-engine.lockDelayNow)+if(engine.manualLock) 1 else 0
		// Add to score
		if(pts+cmb+spd>0) {
			val get = calcScoreCombo(pts, cmb, engine.statistics.level, spd)

			if(pts>0) lastscore = get
			if(lines>=1) engine.statistics.scoreLine += get
			else engine.statistics.scoreBonus += get
			scDisp += spd
		}
		// BGM fade-out effects and BGM changes
		if(engine.statistics.lines>=nextbgmLine(engine.statistics.lines)-5) owner.bgmStatus.fadesw = true
		val newbgm = minOf(maxOf(0, bgmlv(engine.statistics.lines)), 8)
		if(bgmlv!=newbgm) {
			bgmlv = newbgm
			owner.bgmStatus.bgm = BGM.Generic(bgmlv)
			owner.bgmStatus.fadesw = false
		}

		// Meter
		engine.meterValue = engine.statistics.lines%10*receiver.getMeterMax(engine)/9
		engine.meterColor = GameEngine.METER_COLOR_GREEN
		if(engine.statistics.lines%10>=4) engine.meterColor = GameEngine.METER_COLOR_YELLOW
		if(engine.statistics.lines%10>=6) engine.meterColor = GameEngine.METER_COLOR_ORANGE
		if(engine.statistics.lines%10>=8) engine.meterColor = GameEngine.METER_COLOR_RED

		if(engine.statistics.lines>=tableGameClearLines[goaltype]&&tableGameClearLines[goaltype]>=0) {
			// Ending
			engine.ending = 1
			engine.gameEnded()
		} else if(engine.statistics.lines>=(engine.statistics.level+1)*10&&engine.statistics.level<tableGameClearLines[goaltype]/10) {
			// Level up
			engine.statistics.level++

			owner.backgroundStatus.fadesw = true
			owner.backgroundStatus.fadecount = 0
			owner.backgroundStatus.fadebg = engine.statistics.level

			setSpeed(engine)
			engine.playSE("levelup")
		}
		return if(pts>0) lastscore else 0
	}

	/* Soft drop */
	override fun afterSoftDropFall(engine:GameEngine, playerID:Int, fall:Int) {
		engine.statistics.scoreSD += fall
		scDisp += fall
	}

	/* Hard drop */
	override fun afterHardDropFall(engine:GameEngine, playerID:Int, fall:Int) {
		engine.statistics.scoreHD += fall*2
		scDisp += fall*2
	}

	override fun onResult(engine:GameEngine, playerID:Int):Boolean {
		val b = if(engine.ending==0) BGM.Result(1) else BGM.Result(2)
		owner.bgmStatus.fadesw = false
		owner.bgmStatus.bgm = b

		return super.onResult(engine, playerID)
	}

	/* Render results screen */
	override fun renderResult(engine:GameEngine, playerID:Int) {
		drawResultStats(
			engine, playerID, receiver, 0, COLOR.BLUE, Statistic.SCORE, Statistic.LINES, Statistic.TIME,
			Statistic.SPM, Statistic.LPM, Statistic.SPL
		)
		drawResultRank(engine, playerID, receiver, 12, COLOR.BLUE, rankingRank)
		drawResultNetRank(engine, playerID, receiver, 14, COLOR.BLUE, netRankingRank[0])
		drawResultNetRankDaily(engine, playerID, receiver, 16, COLOR.BLUE, netRankingRank[1])

		if(netIsPB) receiver.drawMenuFont(engine, playerID, 2, 21, "NEW PB", COLOR.ORANGE)

		if(netIsNetPlay&&netReplaySendStatus==1)
			receiver.drawMenuFont(engine, playerID, 0, 22, "SENDING...", COLOR.PINK)
		else if(netIsNetPlay&&!netIsWatch
			&&netReplaySendStatus==2
		)
			receiver.drawMenuFont(engine, playerID, 1, 22, "A: RETRY", COLOR.RED)
	}

	/* Called when saving replay */
	override fun saveReplay(engine:GameEngine, playerID:Int, prop:CustomProperties):Boolean {
		saveSetting(prop, engine)

		// NET: Save name
		if(netPlayerName!=null&&netPlayerName!!.isNotEmpty()) prop.setProperty("$playerID.net.netPlayerName", netPlayerName)

		// Update rankings
		if(!owner.replayMode&&!big&&engine.ai==null) {
			updateRanking(engine.statistics.score, engine.statistics.lines, engine.statistics.time, goaltype)

			if(rankingRank!=-1) return true
		}
		return false
	}

	override fun loadSetting(prop:CustomProperties, ruleName:String, playerID:Int) {
		startLevel = prop.getProperty("marathon.startLevel", 0)
		goaltype = prop.getProperty("marathon.goaltype", 0)
		big = prop.getProperty("marathon.big", false)
		version = prop.getProperty("marathon.version", 0)
	}

	override fun saveSetting(prop:CustomProperties, ruleName:String, playerID:Int) {

		prop.setProperty("marathon.startLevel", startLevel)
		prop.setProperty("marathon.goaltype", goaltype)
		prop.setProperty("marathon.big", big)
		prop.setProperty("marathon.version", version)
	}

	/** Update rankings
	 * @param sc Score
	 * @param li Lines
	 * @param time Time
	 */
	private fun updateRanking(sc:Int, li:Int, time:Int, type:Int) {
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
	private fun checkRanking(sc:Int, li:Int, time:Int, type:Int):Int {
		for(i in 0 until RANKING_MAX)
			if(sc>rankingScore[type][i]) return i
			else if(sc==rankingScore[type][i]&&li>rankingLines[type][i]) return i
			else if(sc==rankingScore[type][i]&&li==rankingLines[type][i]&&time<rankingTime[type][i]) return i

		return -1
	}

	/** NET: Send various in-game stats of [engine] */
	override fun netSendStats(engine:GameEngine) {
		val bg =
			if(engine.owner.backgroundStatus.fadesw) engine.owner.backgroundStatus.fadebg else engine.owner.backgroundStatus.bg
		var msg = "game\tstats\t"
		msg += "${engine.statistics.scoreLine}\t${engine.statistics.scoreSD}\t${engine.statistics.scoreHD}\t${engine.statistics.scoreBonus}\t"
		msg += "${engine.statistics.lines}\t${engine.statistics.totalPieceLocked}\t"
		msg += "${engine.statistics.time}\t${engine.statistics.level}\t"
		msg += "$goaltype\t${engine.gameActive}\t${engine.timerActive}\t"
		msg += "$lastscore\t$scDisp\t${engine.lastevent}\t${engine.b2bbuf}\t${engine.combobuf}\t$bg\n"
		netLobby!!.netPlayerClient!!.send(msg)
	}

	/** NET: Parse Received [message] as in-game stats of [engine] */
	override fun netRecvStats(engine:GameEngine, message:Array<String>) {
		listOf<(String)->Unit>({}, {}, {}, {},
			{engine.statistics.scoreLine = it.toInt()},
			{engine.statistics.scoreSD = it.toInt()},
			{engine.statistics.scoreHD = it.toInt()},
			{engine.statistics.scoreBonus = it.toInt()},
			{engine.statistics.lines = it.toInt()},
			{engine.statistics.totalPieceLocked = it.toInt()},
			{engine.statistics.time = it.toInt()},
			{engine.statistics.level = it.toInt()},
			{goaltype = it.toInt()},
			{engine.gameActive = it.toBoolean()},
			{engine.timerActive = it.toBoolean()},
			{lastscore = it.toInt()},
			{/*scDisp = it.toInt()*/},
			{engine.lastevent = ScoreEvent.parseInt(it)},
			{engine.b2bbuf = it.toInt()},
			{engine.combobuf = it.toInt()},
			{engine.owner.backgroundStatus.bg = it.toInt()}).zip(message).forEach {(x, y) ->
			x(y)
		}

		// Meter
		engine.meterValue = engine.statistics.lines%10*receiver.getMeterMax(engine)/9
		engine.meterColor = GameEngine.METER_COLOR_GREEN
		if(engine.statistics.lines%10>=4) engine.meterColor = GameEngine.METER_COLOR_YELLOW
		if(engine.statistics.lines%10>=6) engine.meterColor = GameEngine.METER_COLOR_ORANGE
		if(engine.statistics.lines%10>=8) engine.meterColor = GameEngine.METER_COLOR_RED
	}

	/** NET: Send end-of-game stats
	 * @param engine GameEngine
	 */
	override fun netSendEndGameStats(engine:GameEngine) {
		var subMsg = ""
		subMsg += "SCORE;${engine.statistics.score}\t"
		subMsg += "LINE;${engine.statistics.lines}\t"
		subMsg += "LEVEL;${engine.statistics.level+engine.statistics.levelDispAdd}\t"
		subMsg += "TIME;${engine.statistics.time.toTimeStr}\t"
		subMsg += "SCORE/LINE;${engine.statistics.spl}\t"
		subMsg += "LINE/MIN;${engine.statistics.lpm}\t"

		val msg = "gstat1p\t${NetUtil.urlEncode(subMsg)}\n"
		netLobby!!.netPlayerClient!!.send(msg)
	}

	/** NET: Send game options to all spectators
	 * @param engine GameEngine
	 */
	override fun netSendOptions(engine:GameEngine) {
		val msg = "game\toption\t$startLevel\t$goaltype\t$big\n"
		netLobby!!.netPlayerClient!!.send(msg)
	}

	/** NET: Receive game options */
	override fun netRecvOptions(engine:GameEngine, message:Array<String>) {
		startLevel = message[4].toInt()
		goaltype = message[5].toInt()
		big = message[6].toBoolean()
	}

	/** NET: Get goal type */
	override fun netGetGoalType():Int = goaltype

	/** NET: It returns true when the current settings doesn't prevent
	 * leaderboard screen from showing. */
	override fun netIsNetRankingViewOK(engine:GameEngine):Boolean = startLevel==0&&!big&&engine.ai==null

	companion object {
		/** Current version */
		private const val CURRENT_VERSION = 2

		/** Fall velocity table (numerators) */
		private val tableGravity = intArrayOf(1, 2, 1, 3, 3, 7, 3, 12, 30, 26, 26, 3, 1, 3, 2, 3, 5, 15, 10, -1)

		/** Fall velocity table (denominators) */
		private val tableDenominator = intArrayOf(60, 95, 37, 85, 64, 110, 34, 97, 169, 100, 67, 5, 1, 2, 1, 1, 1, 2, 1, 1)

		/** Line counts when BGM changes occur */
		private val tableBGMChange = arrayOf(
			intArrayOf(30, 60, 90, 120),
			intArrayOf(30, 60, 90, 120, 140, 160, 180),
			intArrayOf(110, 220, 330, 440, 550, 660, 770, 880)
		)

		/** Line counts when game ending occurs */
		private val tableGameClearLines = intArrayOf(150, 200, 999, -1)

		/** Number of entries in rankings */
		private const val RANKING_MAX = 13

		/** Number of ranking types */
		private val RANKING_TYPE = tableGameClearLines.size

		/** Number of game types */
		private val GOALTYPE_MAX = tableGameClearLines.size
	}
}
