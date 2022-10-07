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
import mu.nu.nullpo.game.component.SpeedParam
import mu.nu.nullpo.game.event.EventReceiver.COLOR
import mu.nu.nullpo.game.event.ScoreEvent
import mu.nu.nullpo.game.net.NetUtil
import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.game.subsystem.mode.menu.BooleanMenuItem
import mu.nu.nullpo.game.subsystem.mode.menu.DelegateMenuItem
import mu.nu.nullpo.game.subsystem.mode.menu.StringsMenuItem
import mu.nu.nullpo.gui.common.BaseFont
import mu.nu.nullpo.util.CustomProperties
import mu.nu.nullpo.util.GeneralUtil.toInt
import mu.nu.nullpo.util.GeneralUtil.toTimeStr

/** ULTRA Mode */
class SprintUltra:NetDummyMode() {
	private var pow = 0

	/** Most recent scoring event b2b */
	private var lastb2b = false

	/** Most recent scoring event combo count */
	private var lastcombo = 0

	/** Most recent scoring event piece ID */
	private var lastpiece = 0

	private val itemBig = BooleanMenuItem("big", "BIG", COLOR.BLUE, false)
	/** BigMode */
	private var big:Boolean by DelegateMenuItem(itemBig)

	private val itemGoal = StringsMenuItem(
		"goalType", "Length", COLOR.BLUE, 0, tableLength.map {"$it Min"}
	)
	/** Time limit type */
	private var goalType:Int by DelegateMenuItem(itemGoal)

	/** Last preset number used */
	private var presetNumber = 0

	/** Version */
	private var version = 0

	/** Current round's ranking position */
	private val rankingRank = MutableList(RANKTYPE_MAX) {-1}

	/** Rankings' scores */
	private val rankingScore = List(RANKTYPE_MAX) {List(GOALTYPE_MAX) {MutableList(RANKING_MAX) {0L}}}

	/** Rankings' sent line counts */
	private val rankingPower = List(RANKTYPE_MAX) {List(GOALTYPE_MAX) {MutableList(RANKING_MAX) {0}}}

	/** Rankings' line counts */
	private val rankingLines = List(RANKTYPE_MAX) {List(GOALTYPE_MAX) {MutableList(RANKING_MAX) {0}}}

	private var rankingShow = 0
	override val rankMap
		get() = rankMapOf(rankingScore.flatMapIndexed {a, x -> x.mapIndexed {b, y -> "$a.$b.score" to y}}+
			rankingPower.flatMapIndexed {a, x -> x.mapIndexed {b, y -> "$a.$b.power" to y}}+
			rankingLines.flatMapIndexed {a, x -> x.mapIndexed {b, y -> "$a.$b.lines" to y}})

	/* Mode name */
	override val name = "ULTRA Score Attack"
	override val gameIntensity = 2

	/* Initialization */
	override fun playerInit(engine:GameEngine) {
		super.playerInit(engine)
		lastscore = 0
		lastb2b = false
		lastcombo = 0
		lastpiece = 0
		pow = 0
		rankingShow = 0

		rankingRank.fill(-1)

		rankingScore.forEach {it.forEach {it.fill(0)}}
		rankingLines.forEach {it.forEach {it.fill(0)}}
		rankingPower.forEach {it.forEach {it.fill(0)}}

		engine.frameColor = GameEngine.FRAME_COLOR_BLUE

		netPlayerInit(engine)

		if(!engine.owner.replayMode) {
			presetNumber = engine.owner.modeConfig.getProperty("ultra.presetNumber", 0)
			loadPreset(engine, engine.owner.modeConfig, -1)

			version = CURRENT_VERSION
		} else {
			presetNumber = 0
			loadPreset(engine, engine.owner.replayProp, -1)
			version = engine.owner.replayProp.getProperty("ultra.version", 0)
			// NET: Load name
			netPlayerName = engine.owner.replayProp.getProperty("${engine.playerID}.net.netPlayerName", "")
		}
	}

	/** Load options from a preset
	 * @param engine GameEngine
	 * @param prop Property file to read from
	 * @param preset Preset number
	 */
	private fun loadPreset(engine:GameEngine, prop:CustomProperties, preset:Int) {
		engine.speed.gravity = prop.getProperty("ultra.gravity.$preset", 1)
		engine.speed.denominator = prop.getProperty("ultra.denominator.$preset", 256)
		engine.speed.are = prop.getProperty("ultra.are.$preset", 0)
		engine.speed.areLine = prop.getProperty("ultra.areLine.$preset", 0)
		engine.speed.lineDelay = prop.getProperty("ultra. lineDelay.$preset", 0)
		engine.speed.lockDelay = prop.getProperty("ultra.lockDelay.$preset", 30)
		engine.speed.das = prop.getProperty("ultra.das.$preset", 10)
		big = prop.getProperty("ultra.big.$preset", false)
		goalType = prop.getProperty("ultra.goalType.$preset", 0)
	}

	/** Save options to a preset
	 * @param engine GameEngine
	 * @param prop Property file to save to
	 * @param preset Preset number
	 */
	private fun savePreset(engine:GameEngine, prop:CustomProperties, preset:Int) {
		prop.setProperty("ultra.gravity.$preset", engine.speed.gravity)
		prop.setProperty("ultra.denominator.$preset", engine.speed.denominator)
		prop.setProperty("ultra.are.$preset", engine.speed.are)
		prop.setProperty("ultra.areLine.$preset", engine.speed.areLine)
		prop.setProperty("ultra.lineDelay.$preset", engine.speed.lineDelay)
		prop.setProperty("ultra.lockDelay.$preset", engine.speed.lockDelay)
		prop.setProperty("ultra.das.$preset", engine.speed.das)
		prop.setProperty("ultra.big.$preset", big)
		prop.setProperty("ultra.goalType.$preset", goalType)

		engine.frameColor = if(is20g(engine.speed)) GameEngine.FRAME_COLOR_RED else GameEngine.FRAME_COLOR_BLUE
	}

	fun is20g(speed:SpeedParam) = speed.rank>=.3f
	fun goalType(speed:SpeedParam) = goalType+tableLength.size*is20g(speed).toInt()

	/* Called at settings screen */
	override fun onSetting(engine:GameEngine):Boolean {
		// NET: Net Ranking
		if(netIsNetRankingDisplayMode)
			netOnUpdateNetPlayRanking(engine, goalType)
		else if(!engine.owner.replayMode) {
			// Configuration changes
			val change = updateCursor(engine, 17)

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
					5 -> engine.speed.lockDelay = rangeCursor(engine.speed.lockDelay+change, 0, 99)
					6 -> engine.speed.das = rangeCursor(engine.speed.das+change, 0, 99)
					7 -> big = !big
					8 -> {
						goalType += change
						if(goalType<0) goalType = tableLength.size-1
						if(goalType>tableLength.size-1) goalType = 0
					}
					10, 11 -> presetNumber = rangeCursor(presetNumber+change, 0, 99)
				}
				engine.frameColor = if(is20g(engine.speed)) GameEngine.FRAME_COLOR_RED else GameEngine.FRAME_COLOR_BLUE
				// NET: Signal options change
				if(netIsNetPlay&&netNumSpectators>0) netSendOptions(engine)
			}

			// Confirm
			if(menuTime<5) menuTime++ else if(engine.ctrl.isPush(Controller.BUTTON_A)) {
				engine.playSE("decide")

				if(menuCursor==16) {
					loadPreset(engine, owner.modeConfig, presetNumber)

					// NET: Signal options change
					if(netIsNetPlay&&netNumSpectators>0) netSendOptions(engine)
				} else if(menuCursor==17) {
					savePreset(engine, owner.modeConfig, presetNumber)
					owner.saveModeConfig()
				} else {
					owner.modeConfig.setProperty("ultra.presetNumber", presetNumber)
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
			if(engine.ctrl.isPush(Controller.BUTTON_D)&&netIsNetPlay&&!netIsWatch
				&&netIsNetRankingViewOK(engine)
			)
				netEnterNetPlayRankingScreen(goalType)
			if(engine.ctrl.isPush(Controller.BUTTON_F)) {
				rankingShow++
				if(rankingShow>=RANKTYPE_MAX) rankingShow = 0
			}
		} else {
			menuTime++
			menuCursor = 0

			if(menuTime>=60) menuCursor = 10
			return menuTime<120
		}// Replay

		return true
	}

	/* Setting screen drawing */
	override fun renderSetting(engine:GameEngine) {
		if(netIsNetRankingDisplayMode)
		// NET: Netplay Ranking
			netOnRenderNetPlayRanking(engine, receiver)
		else {
			drawMenuSpeeds(engine, receiver, 0, COLOR.BLUE, 0)
			drawMenuCompact(engine, receiver, "BIG" to big, "Length" to "${tableLength[goalType]} Min")
			if(!engine.owner.replayMode) {
				menuColor = COLOR.GREEN
				drawMenuCompact(engine, receiver, "LOAD" to presetNumber, "SAVE" to presetNumber)
			}
		}
	}

	/* This function will be called before the game actually begins (after
 * Ready&Go screen disappears) */
	override fun startGame(engine:GameEngine) {
		engine.big = big
		engine.b2bEnable = true
		engine.splitB2B = true
		engine.comboType = GameEngine.COMBO_TYPE_NORMAL
		engine.meterValue = 1f
		engine.meterColor = GameEngine.METER_COLOR_GREEN
		owner.musMan.bgm = if(netIsWatch) BGM.Silent
		else BGM.Blitz((tableLength[goalType]>3).toInt()+is20g(engine.speed).toInt()*2)
		engine.twistAllowKick = true
		engine.twistEnable = true
		engine.useAllSpinBonus = true
		engine.twistEnableEZ = true
	}

	/* Render score */
	override fun renderLast(engine:GameEngine) {
		if(owner.menuOnly) return

		receiver.drawScoreFont(engine, 0, 0, name, if(is20g(engine.speed)) COLOR.PINK else COLOR.CYAN)
		val is20g = is20g(engine.speed)
		if(is20g) receiver.drawScoreFont(engine, 0, 1, "(${(tableLength[goalType])} Minutes Rush)", COLOR.PINK)
		else receiver.drawScoreFont(engine, 0, 1, "(${(tableLength[goalType])} Minutes sprint)", COLOR.CYAN)

		if(engine.stat==GameEngine.Status.SETTING||engine.stat==GameEngine.Status.RESULT&&!owner.replayMode) {
			val gt = goalType(engine.speed)
			val col1 = if(is20g) COLOR.RED else COLOR.BLUE
			val col2 = if(is20g) COLOR.YELLOW else COLOR.GREEN
			val col3 = if(is20g) COLOR.ORANGE else COLOR.YELLOW
			if(!owner.replayMode&&!big&&engine.ai==null) {
				receiver.drawScoreFont(engine, 0, 3, "Score RANKING", col2)
				receiver.drawScoreFont(engine, 1, 4, "Score Power Lines", col1)

				for(i in 0 until minOf(RANKING_MAX, 12)) {
					receiver.drawScoreGrade(engine, 0, 5+i, String.format("%2d", i+1), col3)
					receiver.drawScoreNum(
						engine, 1, 5+i, String.format("%7d", rankingScore[0][gt][i]), i==rankingRank[0]
					)
					receiver.drawScoreNum(
						engine, 8, 5+i, String.format("%5d", rankingPower[0][gt][i]), i==rankingRank[0]
					)
					receiver.drawScoreNum(
						engine, 14, 5+i, String.format("%5d", rankingLines[0][gt][i]), i==rankingRank[0]
					)
				}

				receiver.drawScoreFont(engine, 0, 11, "Power RANKING", col2)
				receiver.drawScoreFont(engine, 1, 12, "Power Score Lines", col1)

				for(i in 0 until RANKING_MAX) {
					receiver.drawScoreGrade(engine, 0, 13+i, String.format("%2d", i+1), col3)
					receiver.drawScoreNum(
						engine, 2, 13+i, String.format("%5d", rankingPower[1][gt][i]), i==rankingRank[1]
					)
					receiver.drawScoreNum(
						engine, 7, 13+i, String.format("%7d", rankingScore[1][gt][i]), i==rankingRank[1]
					)
					receiver.drawScoreNum(
						engine, 14, 13+i, String.format("%5d", rankingLines[1][gt][i]), i==rankingRank[1]
					)
				}

				receiver.drawScoreFont(engine, 0, 19, "Lines RANKING", col2)
				receiver.drawScoreFont(engine, 1, 20, "Lines Score Power", col1)

				for(i in 0 until RANKING_MAX) {
					receiver.drawScoreGrade(engine, 0, 21+i, String.format("%2d", i+1), col3)
					receiver.drawScoreNum(
						engine, 2, 21+i, String.format("%5d", rankingLines[2][gt][i]), i==rankingRank[2]
					)
					receiver.drawScoreNum(
						engine, 7, 21+i, String.format("%7d", rankingScore[2][gt][i]), i==rankingRank[2]
					)
					receiver.drawScoreNum(
						engine, 14, 21+i, String.format("%5d", rankingPower[2][gt][i]), i==rankingRank[2]
					)
				}
			}
		} else {
			receiver.drawScoreFont(engine, 0, 3, "Score", COLOR.BLUE)
			receiver.drawScoreNum(engine, 5, 3, "+${String.format("%6d", lastscore)}")
			receiver.drawScoreNum(engine, 0, 4, String.format("%7d", scDisp), scDisp<engine.statistics.score, 2f)

			receiver.drawScoreFont(engine, 10, 3, "/min", COLOR.BLUE)
			receiver.drawScoreNum(
				engine, 10, 4, String.format("%7g", engine.statistics.spm), scDisp<engine.statistics.score,
				1.5f
			)

			receiver.drawScoreFont(engine, 0, 6, "Spike", COLOR.BLUE)
			receiver.drawScoreNum(engine, 0, 7, String.format("%5d", engine.statistics.attacks), 2f)

			receiver.drawScoreFont(engine, 3, 9, "Lines", COLOR.BLUE)
			receiver.drawScoreNum(engine, 8, 8, "${engine.statistics.lines}", 2f)

			receiver.drawScoreFont(engine, 4, 10, "/min", COLOR.BLUE)
			receiver.drawScoreNum(engine, 8, 10, "${engine.statistics.lpm}", 1.5f)

			receiver.drawScoreFont(engine, 0, 14, "Time", COLOR.BLUE)
			val time = maxOf(0, (tableLength[goalType])*3600-engine.statistics.time)
			receiver.drawScoreSpeed(engine, 0, 15, engine.statistics.time/(tableLength[goalType]*3600f), 12f)
			receiver.drawScoreNum(engine, 0, 16, time.toTimeStr, getTimeFontColor(time), 2f)
		}

		super.renderLast(engine)
	}

	/* Calculate score */
	override fun calcScore(engine:GameEngine, ev:ScoreEvent):Int {
		calcPower(engine, ev, true)
		engine.statistics.level = engine.statistics.attacks/2
		return super.calcScore(engine, ev)
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

	/* Each frame Processing at the end of */
	override fun onLast(engine:GameEngine) {
		super.onLast(engine)
		if(engine.gameActive&&engine.timerActive) {
			val limitTime = (tableLength[goalType])*3600
			val remainTime = limitTime-engine.statistics.time

			// Time meter
			engine.meterValue = remainTime*1f/limitTime
			engine.meterColor = GameEngine.METER_COLOR_GREEN
			if(remainTime<=30*60) engine.meterColor = GameEngine.METER_COLOR_YELLOW
			if(remainTime<=20*60) engine.meterColor = GameEngine.METER_COLOR_ORANGE
			if(remainTime<=10*60) engine.meterColor = GameEngine.METER_COLOR_RED

			if(!netIsWatch) {
				// Out of time
				if(engine.statistics.time>=limitTime) {
					engine.gameEnded()
					engine.resetStatc()
					engine.stat = GameEngine.Status.ENDINGSTART
					return
				}

				// 10Seconds before the countdown
				if(engine.statistics.time>=limitTime-10*60&&engine.statistics.time%60==0) engine.playSE("countdown")

				// 5 seconds beforeBGM fadeout
//				if(engine.statistics.time>=limitTime-5*60) owner.bgmStatus.fadesw = true

				// 1Per-minuteBackgroundSwitching
				if(engine.statistics.time>0&&engine.statistics.time%3600==0) {
					engine.playSE("levelup")
					owner.bgMan.fadesw = true
					owner.bgMan.fadebg = owner.bgMan.bg+1
				}
			}
		}
	}

	/* Results screen */
	override fun onResult(engine:GameEngine):Boolean {
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
		receiver.drawMenuFont(
			engine,
			0,
			0,
			"${BaseFont.UP_L}${BaseFont.DOWN_L} PAGE${engine.statc[1]+1}/2",
			COLOR.RED
		)
		if(rankingRank[0]==0) receiver.drawMenuFont(engine, 0, 3, "NEW RECORD", COLOR.ORANGE)
		else if(rankingRank[0]!=-1)
			receiver.drawMenuFont(engine, 4, 3, String.format("RANK %d", rankingRank[0]+1), COLOR.ORANGE)
		if(rankingRank[1]==0) receiver.drawMenuFont(engine, 0, 6, "NEW RECORD", COLOR.ORANGE)
		else if(rankingRank[1]!=-1)
			receiver.drawMenuFont(engine, 4, 6, String.format("RANK %d", rankingRank[1]+1), COLOR.ORANGE)
		if(rankingRank[2]==0) receiver.drawMenuFont(engine, 0, 9, "NEW RECORD", COLOR.ORANGE)
		else if(rankingRank[2]!=-1)
			receiver.drawMenuFont(engine, 4, 9, String.format("RANK %d", rankingRank[2]+1), COLOR.ORANGE)

		when(engine.statc[1]) {
			0 -> {
				drawResultStats(engine, receiver, 1, COLOR.BLUE, Statistic.SCORE)
				drawResultStats(engine, receiver, 4, COLOR.BLUE, Statistic.ATTACKS)
				drawResultStats(engine, receiver, 7, COLOR.BLUE, Statistic.LINES)
				drawResultStats(
					engine, receiver, 10, COLOR.BLUE, Statistic.PPS, Statistic.VS
				)
			}
			1 -> {
				drawResultStats(engine, receiver, 1, COLOR.BLUE, Statistic.SPM)
				drawResultStats(engine, receiver, 4, COLOR.BLUE, Statistic.APM)
				drawResultStats(engine, receiver, 7, COLOR.BLUE, Statistic.LPM)

				drawResultStats(engine, receiver, 10, COLOR.BLUE, Statistic.SPL, Statistic.APL)
			}
		}
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
		savePreset(engine, engine.owner.replayProp, -1)
		engine.owner.replayProp.setProperty("ultra.version", version)

		// NET: Save name
		if(netPlayerName!=null&&netPlayerName!!.isNotEmpty()) prop.setProperty(
			"${engine.playerID}.net.netPlayerName",
			netPlayerName
		)

		// Update rankings
		if(!owner.replayMode&&!big&&engine.ai==null) {
			if(updateRanking(
					goalType(engine.speed),
					engine.statistics.score,
					engine.statistics.attacks,
					engine.statistics.lines
				).any {it!=-1})
				return true
		}
		return false
	}
	/** Update rankings
	 * @param sc Score
	 * @param po Power
	 * @param li Lines
	 */
	private fun updateRanking(goal:Int, sc:Long, po:Int, li:Int):List<Int> {
		RANKTYPE_ALL.mapIndexed {i, it ->
			val ret = checkRanking(it, goal, sc, po, li)
			if(ret!=-1) {
				// Shift down ranking entries
				for(j in RANKING_MAX-1 downTo ret+1) {
					rankingScore[i][goal][j] = rankingScore[i][goal][j-1]
					rankingPower[i][goal][j] = rankingPower[i][goal][j-1]
					rankingLines[i][goal][j] = rankingLines[i][goal][j-1]
				}

				// Add new data
				rankingScore[i][goal][ret] = sc
				rankingPower[i][goal][ret] = po
				rankingLines[i][goal][ret] = li
			}
			rankingRank[i] = ret
		}
		return rankingRank
	}

	/** Calculate ranking position
	 * @param sc Score
	 * @param li Lines
	 * @param type Number of ranking types
	 * @return Position (-1 if unranked)
	 */
	private fun checkRanking(type:RankingType, goal:Int, sc:Long, po:Int, li:Int):Int {
		val ord = type.ordinal
		for(i in 0 until RANKING_MAX)
			when(type) {
				RankingType.Score ->
					when {
						sc>rankingScore[ord][goal][i] -> return i
						sc==rankingScore[ord][goal][i]&&po>rankingPower[ord][goal][i] -> return i
						sc==rankingScore[ord][goal][i]&&po==rankingPower[ord][goal][i]&&li>rankingLines[ord][goal][i] -> return i
					}
				RankingType.Power -> when {
					po>rankingPower[ord][goal][i] -> return i
					po==rankingPower[ord][goal][i]&&sc>rankingScore[ord][goal][i] -> return i
					po==rankingPower[ord][goal][i]&&sc==rankingScore[ord][goal][i]&&li>rankingLines[ord][goal][i] -> return i
				}
				RankingType.Lines -> when {
					li>rankingLines[ord][goal][i] -> return i
					li==rankingLines[ord][goal][i]&&sc>rankingScore[ord][goal][i] -> return i
					li==rankingLines[ord][goal][i]&&sc>rankingScore[ord][goal][i]&&po>rankingPower[ord][goal][i] -> return i
				}
			}

		return -1
	}

	/** NET: Send various in-game stats of [engine] */
	override fun netSendStats(engine:GameEngine) {
		val bg = if(owner.bgMan.fadesw) owner.bgMan.fadebg else owner.bgMan.bg
		val msg = "game\tstats\t"+
			"${engine.statistics.scoreLine}\t${engine.statistics.scoreSD}\t${engine.statistics.scoreHD}\t"+
			"${engine.statistics.scoreBonus}\t${engine.statistics.lines}\t"+
			"${engine.statistics.totalPieceLocked}\t${engine.statistics.time}\t$goalType\t"+
			"${engine.gameActive}\t${engine.timerActive}\t"+
			"$lastscore\t$scDisp\t${engine.lastEvent}\t$lastb2b\t$lastcombo\t$lastpiece\t"+
			"$bg\n"
		netLobby?.netPlayerClient?.send(msg)
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
			{goalType = it.toInt()},
			{engine.gameActive = it.toBoolean()},
			{engine.timerActive = it.toBoolean()},
			{lastscore = it.toInt()},
			{/*scDisp = it.toInt()*/},
			{engine.lastEvent = ScoreEvent.parseStr(it)},
			{lastb2b = it.toBoolean()},
			{lastcombo = it.toInt()},
			{lastpiece = it.toInt()},
			{owner.bgMan.bg = it.toInt()}).zip(message).forEach {(x, y) ->
			x(y)
		}

		// Time meter
		val limitTime = (goalType+1)*3600
		val remainTime = (goalType+1)*3600-engine.statistics.time
		engine.meterValue = remainTime*1f/limitTime
		engine.meterColor = GameEngine.METER_COLOR_LIMIT
	}

	/** NET: Send end-of-game stats
	 * @param engine GameEngine
	 */
	override fun netSendEndGameStats(engine:GameEngine) {
		val subMsg =
			"SCORE;${engine.statistics.score}\t"+
				"LINE;${engine.statistics.lines}\t"+
				"PIECE;${engine.statistics.totalPieceLocked}\t"+
				"SCORE/LINE;${engine.statistics.spl}\t"+
				"SCORE/MIN;${engine.statistics.spm}\t"+
				"LINE/MIN;${engine.statistics.lpm}\t"+
				"PIECE/SEC;${engine.statistics.pps}\t"

		val msg = "gstat1p\t${NetUtil.urlEncode(subMsg)}\n"
		netLobby!!.netPlayerClient!!.send(msg)
	}

	/** NET: Send game options to all spectators
	 * @param engine GameEngine
	 */
	override fun netSendOptions(engine:GameEngine) {
		val msg = "game\toption\t"+
			"${engine.speed.gravity}\t${engine.speed.denominator}\t${engine.speed.are}\t"+
			"${engine.speed.areLine}\t${engine.speed.lineDelay}\t${engine.speed.lockDelay}\t"+
			"${engine.speed.das}\t$big\t$goalType\t$presetNumber\t"
		netLobby!!.netPlayerClient!!.send(msg)
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
		big = message[11].toBoolean()
		goalType = message[12].toInt()
		presetNumber = message[13].toInt()
	}

	/** NET: Get goal type */
	override fun netGetGoalType():Int = goalType

	/** NET: It returns true when the current settings don't prevent
	 * leaderboard screen from showing. */
	override fun netIsNetRankingViewOK(engine:GameEngine):Boolean = !big&&engine.ai==null

	companion object {
		/** Current version */
		private const val CURRENT_VERSION = 1

		/** Number of entries in rankings */
		private const val RANKING_MAX = 5

		/** Minutes counts when game ending occurs */
		private val tableLength = listOf(3, 5)

		/** Number of ranking types */
		private enum class RankingType { Score, Power, Lines }

		private val RANKTYPE_ALL = SprintUltra.Companion.RankingType.values()
		private val RANKTYPE_MAX = RankingType.values().size

		/** Time limit type */
		private val GOALTYPE_MAX = tableLength.size*2
	}
}
