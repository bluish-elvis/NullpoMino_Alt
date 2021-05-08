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

import mu.nu.nullpo.game.component.BGMStatus.BGM
import mu.nu.nullpo.game.component.Controller
import mu.nu.nullpo.game.event.EventReceiver
import mu.nu.nullpo.game.net.NetUtil
import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.util.CustomProperties
import mu.nu.nullpo.util.GeneralUtil
import kotlin.math.ceil

/** MARATHON Mode */
class Marathon:NetDummyMode() {

	/** Most recent increase in score */
	private var lastscore:Int = 0

	/** Time to display the most recent increase in score */
	private var scgettime:Int = 0
	private var sc:Int = 0
	private var sum:Int = 0

	/** Current BGM */
	private var bgmlv:Int = 0

	/** Level at start time */
	private var startlevel:Int = 0

	/** Game type */
	private var goaltype:Int = 0

	/** Big */
	private var big:Boolean = false

	/** Version */
	private var version:Int = 0

	/** Current round's ranking rank */
	private var rankingRank:Int = -1

	/** Rankings' scores */
	private var rankingScore:Array<IntArray> = Array(RANKING_TYPE) {IntArray(RANKING_MAX)}

	/** Rankings' line counts */
	private var rankingLines:Array<IntArray> = Array(RANKING_TYPE) {IntArray(RANKING_MAX)}

	/** Rankings' times */
	private var rankingTime:Array<IntArray> = Array(RANKING_TYPE) {IntArray(RANKING_MAX)}

	/* Mode name */
	override val name:String = "Marathon"

	/* Initialization */
	override fun playerInit(engine:GameEngine, playerID:Int) {
		super.playerInit(engine, playerID)
		lastscore = 0
		sc = 0
		scgettime = sc
		bgmlv = 0

		rankingRank = -1
		rankingScore = Array(RANKING_TYPE) {IntArray(RANKING_MAX)}
		rankingLines = Array(RANKING_TYPE) {IntArray(RANKING_MAX)}
		rankingTime = Array(RANKING_TYPE) {IntArray(RANKING_MAX)}

		netPlayerInit(engine, playerID)

		if(!owner.replayMode) {
			loadSetting(owner.modeConfig)
			loadRanking(owner.recordProp, engine.ruleopt.strRuleName)
			version = CURRENT_VERSION
		} else {
			loadSetting(owner.replayProp)
			if(version==0&&owner.replayProp.getProperty("marathon.endless", false)) goaltype = 2

			// NET: Load name
			netPlayerName = engine.owner.replayProp.getProperty("$playerID.net.netPlayerName", "")
		}

		engine.owner.backgroundStatus.bg = startlevel
		engine.framecolor = GameEngine.FRAME_COLOR_GREEN
	}

	/** Set the gravity rate
	 * @param engine GameEngine
	 */
	fun setSpeed(engine:GameEngine) {
		val ln = (engine.statistics.lines+startlevel*10)/if(tableGameClearLines[goaltype]<=200) 1 else 4

		val lv = maxOf(0,
			minOf(if(tableGameClearLines[goaltype]<=200) engine.statistics.level else engine.statistics.level*2/5,
				tableGravity.size-1))
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
			val change = updateCursor(engine, 2, playerID)

			if(change!=0) {
				engine.playSE("change")

				when(menuCursor) {
					0 -> {
						goaltype += change
						if(goaltype<0) goaltype = GOALTYPE_MAX-1
						if(goaltype>GOALTYPE_MAX-1) goaltype = 0

						if(startlevel>(tableGameClearLines[goaltype]-1)/10&&tableGameClearLines[goaltype]>=0) {
							startlevel = (tableGameClearLines[goaltype]-1)/10
							engine.owner.backgroundStatus.bg = startlevel
						}
					}
					1 -> {
						startlevel += change
						if(tableGameClearLines[goaltype]>=0) {
							if(startlevel<0) startlevel = (tableGameClearLines[goaltype]-1)/10
							if(startlevel>(tableGameClearLines[goaltype]-1)/10) startlevel = 0
						} else {
							if(startlevel<0) startlevel = 19
							if(startlevel>19) startlevel = 0
						}
						engine.owner.backgroundStatus.bg = startlevel
						engine.statistics.level = startlevel
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
				saveSetting(owner.modeConfig)
				owner.saveModeConfig()

				// NET: Signal start of the game
				if(netIsNetPlay) netLobby!!.netPlayerClient!!.send("start1p\n")

				return false
			}

			// Cancel
			if(engine.ctrl.isPush(Controller.BUTTON_B)&&!netIsNetPlay) engine.quitflag = true

			// NET: Netplay Ranking
			if(engine.ctrl.isPush(Controller.BUTTON_D)&&netIsNetPlay&&startlevel==0&&!big&&
				engine.ai==null)
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
			drawMenu(engine, playerID, receiver, 0, EventReceiver.COLOR.BLUE, 0,
				"GOAL", if(tableGameClearLines[goaltype]<=0) "ENDLESS" else "${tableGameClearLines[goaltype]} LINES")
			drawMenuCompact(engine, playerID, receiver, "Level", "${startlevel+1}")
			drawMenuSpeeds(engine, playerID, receiver, 5, EventReceiver.COLOR.BLUE, 10)
			drawMenuCompact(engine, playerID, receiver, 10, EventReceiver.COLOR.BLUE, 2, "BIG", GeneralUtil.getONorOFF(big))
		}
	}

	/* Called for initialization during "Ready" screen */
	override fun startGame(engine:GameEngine, playerID:Int) {
		engine.statistics.level = startlevel
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
			receiver.drawScoreFont(engine, playerID, 0, 0, "ENDLESS MARATHON", EventReceiver.COLOR.GREEN)
		else
			receiver.drawScoreFont(engine, playerID, 0, 0,
				"${tableGameClearLines[goaltype]} LINES MARATHON", EventReceiver.COLOR.GREEN)

		if(engine.stat==GameEngine.Status.SETTING||engine.stat==GameEngine.Status.RESULT&&!owner.replayMode) {
			if(!owner.replayMode&&!big&&engine.ai==null) {
				val scale = if(receiver.nextDisplayType==2) .5f else 1f
				val topY = if(receiver.nextDisplayType==2) 6 else 4
				receiver.drawScoreFont(engine, playerID, 3, topY-1, "SCORE  LINE TIME", EventReceiver.COLOR.BLUE, scale)

				for(i in 0 until RANKING_MAX) {
					receiver.drawScoreGrade(engine, playerID, 0, topY+i, String.format("%2d", i+1),
						if(rankingRank==i) EventReceiver.COLOR.RAINBOW else if(rankingLines[goaltype][i]>tableGameClearLines[goaltype]) EventReceiver.COLOR.CYAN else EventReceiver.COLOR.YELLOW,
						scale)
					receiver.drawScoreNum(engine, playerID, 3, topY+i, "${rankingScore[goaltype][i]}", i==rankingRank, scale)
					receiver.drawScoreNum(engine, playerID, 10, topY+i, "${rankingLines[goaltype][i]}", i==rankingRank, scale)
					receiver.drawScoreNum(engine, playerID, 15, topY+i,
						GeneralUtil.getTime(rankingTime[goaltype][i]), i==rankingRank, scale)
				}
			}
		} else {
			receiver.drawScoreFont(engine, playerID, 0, 3, "LINE", EventReceiver.COLOR.BLUE)
			receiver.drawScoreNum(engine, playerID, 5, 2, engine.statistics.lines.toString(), 2f)

			receiver.drawScoreFont(engine, playerID, 0, 4, "Score", EventReceiver.COLOR.BLUE)
			receiver.drawScoreNum(engine, playerID, 5, 4, "+$lastscore")
			val scget = scgettime<engine.statistics.score
			if(scget) scgettime += ceil((engine.statistics.score-scgettime)/24.0).toInt()
			sc += ceil(((scgettime-sc)/10f).toDouble()).toInt()
			receiver.drawScoreNum(engine, playerID, 0, 5, "$sc", scget, 2f)

			receiver.drawScoreFont(engine, playerID, 0, 8, "Level", EventReceiver.COLOR.BLUE)
			receiver.drawScoreNum(engine, playerID, 5, 8, String.format("%.1f", engine.statistics.level.toFloat()+
				if(engine.statistics.level>=19&&tableGameClearLines[goaltype]<0) 1f else engine.statistics.lines%10*0.1f+1f), 2f)

			receiver.drawScoreFont(engine, playerID, 0, 9, "Time", EventReceiver.COLOR.BLUE)
			receiver.drawScoreNum(engine, playerID, 0, 10, GeneralUtil.getTime(engine.statistics.time), 2f)
		}

		super.renderLast(engine, playerID)
	}

	/* Called after every frame */
	override fun onLast(engine:GameEngine, playerID:Int) {}

	/* Calculate score */
	override fun calcScore(engine:GameEngine, playerID:Int, lines:Int):Int {
		// Line clear bonus
		val pts = calcScore(engine, lines)
		val cmb = if(engine.combo>=1&&lines>=1) engine.combo-1 else 0
		// Combo
		val spd = maxOf(0, engine.lockDelay-engine.lockDelayNow)+if(engine.manualLock) 1 else 0
		// Add to score
		if(pts+cmb+spd>0) {
			var get = pts*(10+engine.statistics.level)/10+spd
			if(cmb>=1) {
				var b = sum*(1+cmb)/2
				sum += get
				b = sum*(2+cmb)/2-b
				get = b
			} else
				sum = get
			if(pts>0) lastscore = get
			if(lines>=1) engine.statistics.scoreLine += get
			else engine.statistics.scoreBonus += get
			scgettime += spd
		}
		// BGM fade-out effects and BGM changes
		if(engine.statistics.lines<(tableBGMChange[goaltype].maxOrNull() ?: 0)) {
			if(engine.statistics.lines>=tableBGMChange[goaltype][bgmlv]-5) owner.bgmStatus.fadesw = true

			if(engine.statistics.lines in tableBGMChange[goaltype][bgmlv] until tableGameClearLines[goaltype]) {
				bgmlv++
				owner.bgmStatus.bgm = BGM.Generic(bgmlv)
				owner.bgmStatus.fadesw = false
			}
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
		scgettime += fall
	}

	/* Hard drop */
	override fun afterHardDropFall(engine:GameEngine, playerID:Int, fall:Int) {
		engine.statistics.scoreHD += fall*2
		scgettime += fall*2
	}

	override fun onResult(engine:GameEngine, playerID:Int):Boolean {
		val b = if(engine.ending==0) BGM.Result(1) else BGM.Result(2)
		owner.bgmStatus.fadesw = false
		owner.bgmStatus.bgm = b

		return super.onResult(engine, playerID)
	}

	/* Render results screen */
	override fun renderResult(engine:GameEngine, playerID:Int) {
		drawResultStats(engine, playerID, receiver, 0, EventReceiver.COLOR.BLUE, Statistic.SCORE, Statistic.LINES, Statistic.LEVEL,
			Statistic.TIME, Statistic.SPL, Statistic.LPM)
		drawResultRank(engine, playerID, receiver, 12, EventReceiver.COLOR.BLUE, rankingRank)
		drawResultNetRank(engine, playerID, receiver, 14, EventReceiver.COLOR.BLUE, netRankingRank[0])
		drawResultNetRankDaily(engine, playerID, receiver, 16, EventReceiver.COLOR.BLUE, netRankingRank[1])

		if(netIsPB) receiver.drawMenuFont(engine, playerID, 2, 21, "NEW PB", EventReceiver.COLOR.ORANGE)

		if(netIsNetPlay&&netReplaySendStatus==1)
			receiver.drawMenuFont(engine, playerID, 0, 22, "SENDING...", EventReceiver.COLOR.PINK)
		else if(netIsNetPlay&&!netIsWatch
			&&netReplaySendStatus==2)
			receiver.drawMenuFont(engine, playerID, 1, 22, "A: RETRY", EventReceiver.COLOR.RED)
	}

	/* Called when saving replay */
	override fun saveReplay(engine:GameEngine, playerID:Int, prop:CustomProperties) {
		saveSetting(prop)

		// NET: Save name
		if(netPlayerName!=null&&netPlayerName!!.isNotEmpty()) prop.setProperty("$playerID.net.netPlayerName", netPlayerName)

		// Update rankings
		if(!owner.replayMode&&!big&&engine.ai==null) {
			updateRanking(engine.statistics.score, engine.statistics.lines, engine.statistics.time, goaltype)

			if(rankingRank!=-1) {
				saveRanking(engine.ruleopt.strRuleName)
				owner.saveModeConfig()
			}
		}
	}

	/** Load settings from property file
	 * @param prop Property file
	 */
	override fun loadSetting(prop:CustomProperties) {
		startlevel = prop.getProperty("marathon.startlevel", 0)
		goaltype = prop.getProperty("marathon.goaltype", 0)
		big = prop.getProperty("marathon.big", false)
		version = prop.getProperty("marathon.version", 0)
	}

	/** Save settings to property file
	 * @param prop Property file
	 */
	override fun saveSetting(prop:CustomProperties) {

		prop.setProperty("marathon.startlevel", startlevel)
		prop.setProperty("marathon.goaltype", goaltype)
		prop.setProperty("marathon.big", big)
		prop.setProperty("marathon.version", version)
	}

	/** Read rankings from property file
	 * @param prop Property file
	 * @param ruleName Rule name
	 */
	override fun loadRanking(prop:CustomProperties, ruleName:String) {
		for(i in 0 until RANKING_MAX)
			for(j in 0 until GOALTYPE_MAX) {
				rankingScore[j][i] = prop.getProperty("marathon.$ruleName.$j.score.$i", 0)
				rankingLines[j][i] = prop.getProperty("marathon.$ruleName.$j.lines.$i", 0)
				rankingTime[j][i] = prop.getProperty("marathon.$ruleName.$j.time.$i", 0)
			}
	}

	/** Save rankings to property file
	 * @param ruleName Rule name
	 */
	fun saveRanking(ruleName:String) {
		super.saveRanking(ruleName, (0 until GOALTYPE_MAX).flatMap {j ->
			(0 until RANKING_MAX).flatMap {i ->
				listOf(
					"marathon.$ruleName.$j.score.$i" to rankingScore[j][i],
					"marathon.$ruleName.$j.lines.$i" to rankingLines[j][i],
					"marathon.$ruleName.$j.time.$i" to rankingTime[j][i])
			}
		})
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
			if(sc>rankingScore[type][i])
				return i
			else if(sc==rankingScore[type][i]&&li>rankingLines[type][i])
				return i
			else if(sc==rankingScore[type][i]&&li==rankingLines[type][i]&&time<rankingTime[type][i]) return i

		return -1
	}

	/** NET: Send various in-game stats (as well as goaltype)
	 * @param engine GameEngine
	 */
	override fun netSendStats(engine:GameEngine) {
		val bg =
			if(engine.owner.backgroundStatus.fadesw) engine.owner.backgroundStatus.fadebg else engine.owner.backgroundStatus.bg
		var msg = "game\tstats\t"
		msg += "${engine.statistics.scoreLine}\t${engine.statistics.scoreSD}\t${engine.statistics.scoreHD}\t${engine.statistics.scoreBonus}\t"
		msg += "${engine.statistics.lines}\t${engine.statistics.totalPieceLocked}\t"
		msg += "${engine.statistics.time}\t${engine.statistics.level}\t"
		msg += "$goaltype\t${engine.gameActive}\t${engine.timerActive}\t"
		msg += "$lastscore\t$scgettime\t${engine.lastevent}\t${engine.b2bbuf}\t${engine.combobuf}\t$bg\n"
		netLobby!!.netPlayerClient!!.send(msg)
	}

	/** NET: Receive various in-game stats (as well as goaltype) */
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
			{scgettime = it.toInt()},
			{engine.lastevent = GameEngine.ScoreEvent.parseInt(it)},
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
		subMsg += "TIME;${GeneralUtil.getTime(engine.statistics.time)}\t"
		subMsg += "SCORE/LINE;${engine.statistics.spl}\t"
		subMsg += "LINE/MIN;${engine.statistics.lpm}\t"

		val msg = "gstat1p\t${NetUtil.urlEncode(subMsg)}\n"
		netLobby!!.netPlayerClient!!.send(msg)
	}

	/** NET: Send game options to all spectators
	 * @param engine GameEngine
	 */
	override fun netSendOptions(engine:GameEngine) {
		val msg = "game\toption\t$startlevel\t$goaltype\t$big\n"
		netLobby!!.netPlayerClient!!.send(msg)
	}

	/** NET: Receive game options */
	override fun netRecvOptions(engine:GameEngine, message:Array<String>) {
		startlevel = message[4].toInt()
		goaltype = message[5].toInt()
		big = message[6].toBoolean()
	}

	/** NET: Get goal type */
	override fun netGetGoalType():Int = goaltype

	/** NET: It returns true when the current settings doesn't prevent
	 * leaderboard screen from showing. */
	override fun netIsNetRankingViewOK(engine:GameEngine):Boolean = startlevel==0&&!big&&engine.ai==null

	companion object {
		/** Current version */
		private const val CURRENT_VERSION = 2

		/** Fall velocity table (numerators) */
		private val tableGravity = intArrayOf(1, 2, 1, 3, 3, 7, 3, 12, 30, 26, 26, 3, 1, 3, 2, 3, 5, 15, 10, -1)

		/** Fall velocity table (denominators) */
		private val tableDenominator = intArrayOf(60, 95, 37, 85, 64, 110, 34, 97, 169, 100, 67, 5, 1, 2, 1, 1, 1, 2, 1, 1)

		/** Line counts when BGM changes occur */
		private val tableBGMChange = arrayOf(intArrayOf(30, 60, 90, 120), intArrayOf(20, 40, 60, 80, 100, 110, 140, 170),
			intArrayOf(110, 220, 330, 440, 550, 660, 770, 880))

		/** Line counts when game ending occurs */
		private val tableGameClearLines = intArrayOf(150, 200, 999)

		/** Number of entries in rankings */
		private const val RANKING_MAX = 10

		/** Number of ranking types */
		private val RANKING_TYPE = tableGameClearLines.size

		/** Number of game types */
		private val GOALTYPE_MAX = tableGameClearLines.size
	}
}
