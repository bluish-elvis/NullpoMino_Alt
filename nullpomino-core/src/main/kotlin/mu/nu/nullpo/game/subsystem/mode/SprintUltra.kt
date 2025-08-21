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
import mu.nu.nullpo.game.component.SpeedParam
import mu.nu.nullpo.game.event.EventReceiver.COLOR
import mu.nu.nullpo.game.event.ScoreEvent
import mu.nu.nullpo.game.net.NetUtil
import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.game.subsystem.mode.menu.*
import mu.nu.nullpo.gui.common.BaseFont
import mu.nu.nullpo.gui.common.BaseFont.FONT.*
import mu.nu.nullpo.util.CustomProperties
import mu.nu.nullpo.util.GeneralUtil.toInt
import mu.nu.nullpo.util.GeneralUtil.toTimeStr

/** ULTRA Mode */
class SprintUltra:NetDummyMode() {
	private var pow = 0
	private val itemSpd = object:SpeedPresets(COLOR.BLUE, 0) {
		override fun presetLoad(engine:GameEngine, prop:CustomProperties, ruleName:String, setId:Int) {
			super.presetLoad(engine, prop, ruleName, setId)
			big = prop.getProperty("$ruleName.big.$setId", false)
			goalType = prop.getProperty("$ruleName.goalType.$setId", 1)
		}

		override fun presetSave(engine:GameEngine, prop:CustomProperties, ruleName:String, setId:Int) {
			super.presetSave(engine, prop, ruleName, setId)
			prop.setProperty("$ruleName.big.$setId", big)
			prop.setProperty("$ruleName.goalType.$setId", goalType)
		}
	}
	/** Last preset number used */
	private var presetNumber:Int by DelegateMenuItem(itemSpd)

	private val itemBig = BooleanMenuItem("big", "BIG", COLOR.BLUE, false)
	/** BigMode */
	private var big:Boolean by DelegateMenuItem(itemBig)

	private val itemGoal = StringsMenuItem(
		"goalType", "Length", COLOR.BLUE, 0, tableLength.map {"$it Min"}
	)
	/** Time limit type */
	private var goalType:Int by DelegateMenuItem(itemGoal)

	override val menu = MenuList("ultra", itemGoal, itemSpd, itemBig)
	/** Version */
	private var version = 0

	/** Number of entries in rankings */
	override val rankingMax = 5

	/** Current round's ranking position */
	private val rankingRank = MutableList(RANKTYPE_MAX) {-1}

	/** Rankings' scores */
	private val rankingScore = List(RANKTYPE_MAX) {List(GOALTYPE_MAX) {MutableList(rankingMax) {0L}}}

	/** Rankings' sent line counts */
	private val rankingPower = List(RANKTYPE_MAX) {List(GOALTYPE_MAX) {MutableList(rankingMax) {0}}}

	/** Rankings' line counts */
	private val rankingLines = List(RANKTYPE_MAX) {List(GOALTYPE_MAX) {MutableList(rankingMax) {0}}}

	private var rankingShow = 0
	override val propRank
		get() = rankMapOf(rankingScore.flatMapIndexed {a, x -> x.mapIndexed {b, y -> "$a.$b.score" to y}}+
			rankingPower.flatMapIndexed {a, x -> x.mapIndexed {b, y -> "$a.$b.power" to y}}+
			rankingLines.flatMapIndexed {a, x -> x.mapIndexed {b, y -> "$a.$b.lines" to y}})

	/* Mode name */
	override val name = "ULTRA Score Attack"
	override val gameIntensity = 2

	/* Initialization */
	override fun playerInit(engine:GameEngine) {
		super.playerInit(engine)
		lastScore = 0
		pow = 0
		rankingShow = 0

		rankingRank.fill(-1)

		rankingScore.forEach {it.forEach {p -> p.fill(0)}}
		rankingLines.forEach {it.forEach {p -> p.fill(0)}}
		rankingPower.forEach {it.forEach {p -> p.fill(0)}}

		owner.bgMan.bg = -14
		engine.frameSkin = if(is20g(engine.speed)) GameEngine.FRAME_COLOR_RED else GameEngine.FRAME_COLOR_BLUE

		netPlayerInit(engine)

		if(!owner.replayMode) {
			version = CURRENT_VERSION
		} else {
			version = owner.replayProp.getProperty("ultra.version", 0)
			// NET: Load name
			netPlayerName = owner.replayProp.getProperty("${engine.playerID}.net.netPlayerName", "")
		}
	}

	fun is20g(speed:SpeedParam) = speed.rank>=.3f
	fun goalType(speed:SpeedParam) = goalType+tableLength.size*is20g(speed).toInt()

	/* Called at settings screen */
	override fun onSetting(engine:GameEngine):Boolean {
		if(!owner.replayMode) {
			if(engine.ctrl.isPush(Controller.BUTTON_F)) {
				rankingShow++
				if(rankingShow>=RANKTYPE_MAX) rankingShow = 0
			}
		}
		return super.onSetting(engine)
	}

	override fun onSettingChanged(engine:GameEngine) {
		super.onSettingChanged(engine)
		engine.frameSkin = if(is20g(engine.speed)) GameEngine.FRAME_COLOR_RED else GameEngine.FRAME_COLOR_BLUE

	}

	/* This function will be called before the game actually begins (after
 Ready&Go screen disappears) */
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

		receiver.drawScore(engine, 0, 0, name, BASE, if(is20g(engine.speed)) COLOR.PINK else COLOR.CYAN)
		val is20g = is20g(engine.speed)
		if(is20g) receiver.drawScore(engine, 0, 1, "(${(tableLength[goalType])} Minutes Rush)", BASE, COLOR.PINK)
		else receiver.drawScore(engine, 0, 1, "(${(tableLength[goalType])} Minutes sprint)", BASE, COLOR.CYAN)

		if(engine.stat==GameEngine.Status.SETTING||engine.stat==GameEngine.Status.RESULT&&!owner.replayMode) {
			val gt = goalType(engine.speed)
			val col1 = if(is20g) COLOR.RED else COLOR.BLUE
			val col2 = if(is20g) COLOR.YELLOW else COLOR.GREEN
			val col3 = if(is20g) COLOR.ORANGE else COLOR.YELLOW
			if(!owner.replayMode&&!big&&engine.ai==null) {
				receiver.drawScore(engine, 0, 3, "Score RANKING", BASE, col2)
				receiver.drawScore(engine, 1, 4, "Score Power Lines", BASE, col1)

				for(i in 0..<minOf(rankingMax, 12)) {
					receiver.drawScore(engine, 0, 5+i, "%2d".format(i+1), GRADE, col3)
					receiver.drawScore(
						engine, 1, 5+i, "%7d".format(rankingScore[0][gt][i]), NUM, i==rankingRank[0]
					)
					receiver.drawScore(
						engine, 8, 5+i, "%5d".format(rankingPower[0][gt][i]), NUM, i==rankingRank[0]
					)
					receiver.drawScore(
						engine, 14, 5+i, "%5d".format(rankingLines[0][gt][i]), NUM, i==rankingRank[0]
					)
				}

				receiver.drawScore(engine, 0, 11, "Power RANKING", BASE, col2)
				receiver.drawScore(engine, 1, 12, "Power Score Lines", BASE, col1)

				for(i in 0..<rankingMax) {
					receiver.drawScore(engine, 0, 13+i, "%2d".format(i+1), GRADE, col3)
					receiver.drawScore(
						engine, 2, 13+i, "%5d".format(rankingPower[1][gt][i]), NUM, i==rankingRank[1]
					)
					receiver.drawScore(
						engine, 7, 13+i, "%7d".format(rankingScore[1][gt][i]), NUM, i==rankingRank[1]
					)
					receiver.drawScore(
						engine, 14, 13+i, "%5d".format(rankingLines[1][gt][i]), NUM, i==rankingRank[1]
					)
				}

				receiver.drawScore(engine, 0, 19, "Lines RANKING", BASE, col2)
				receiver.drawScore(engine, 1, 20, "Lines Score Power", BASE, col1)

				for(i in 0..<rankingMax) {
					receiver.drawScore(engine, 0, 21+i, "%2d".format(i+1), GRADE, col3)
					receiver.drawScore(
						engine, 2, 21+i, "%5d".format(rankingLines[2][gt][i]), NUM, i==rankingRank[2]
					)
					receiver.drawScore(
						engine, 7, 21+i, "%7d".format(rankingScore[2][gt][i]), NUM, i==rankingRank[2]
					)
					receiver.drawScore(
						engine, 14, 21+i, "%5d".format(rankingPower[2][gt][i]), NUM, i==rankingRank[2]
					)
				}
			}
		} else {
			receiver.drawScore(engine, 0, 3, "Score", BASE, COLOR.BLUE)
			receiver.drawScore(engine, 5, 3, "+${"%6d".format(lastScore)}", NUM)
			receiver.drawScore(engine, 0, 4, "%7d".format(scDisp), NUM, scDisp<engine.statistics.score, 2f)

			receiver.drawScore(engine, 5, 6, "/min", BASE, COLOR.BLUE)
			receiver.drawScoreNum(
				engine, 0f, 6.3f, engine.statistics.spm, 7 to null, scDisp<engine.statistics.score,
				1.6f
			)

			receiver.drawScore(engine, 0, 8, "Spike", BASE, COLOR.BLUE)
			receiver.drawScore(engine, 0, 9, "%5d".format(engine.statistics.attacks), NUM, 2f)

			receiver.drawScore(engine, 3, 11, "Lines", BASE, COLOR.BLUE)
			receiver.drawScore(engine, 8, 10, "${engine.statistics.lines}", NUM, 2f)

			receiver.drawScore(engine, 4, 12, "/min", BASE, COLOR.BLUE)
			receiver.drawScoreNum(engine, 8, 12, engine.statistics.lpm, 7 to null, scale = 1.5f)

			receiver.drawScore(engine, 0, 14, "Time", BASE, COLOR.BLUE)
			val time = maxOf(0, (tableLength[goalType])*3600-engine.statistics.time)
			receiver.drawScoreSpeed(engine, 0, 15, engine.statistics.time/(tableLength[goalType]*3600f), 12f)
			receiver.drawScore(engine, 0, 16, time.toTimeStr, NUM, getTimeFontColor(time), 2f)
		}

		super.renderLast(engine)
	}

	/* Calculate score */
	override fun calcScore(engine:GameEngine, ev:ScoreEvent):Int {
		engine.tempHanabi += calcPower(engine, ev, true)
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
			engine.meterColor = if(remainTime<=10*60) GameEngine.METER_COLOR_RED
			else if(remainTime<=20*60) GameEngine.METER_COLOR_ORANGE
			else if(remainTime<=30*60) GameEngine.METER_COLOR_YELLOW
			else GameEngine.METER_COLOR_GREEN

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
//				if(engine.statistics.time>=limitTime-5*60) owner.bgmStatus.fadeSW = true

				// 1Per-minuteBackgroundSwitching
				if(engine.statistics.time>0&&engine.statistics.time%3600==0) {
					engine.playSE("levelup")
					owner.bgMan.nextBg = owner.bgMan.bg+1
				}
			}
		}
	}

	/* Results screen */
	override fun onResult(engine:GameEngine):Boolean {
		// Page change
		if(engine.ctrl.isMenuRepeatKey(Controller.BUTTON_UP)) {
			engine.statc[1]--
			if(engine.statc[1]<0) engine.statc[1] = 2
			engine.playSE("change")
		}
		if(engine.ctrl.isMenuRepeatKey(Controller.BUTTON_DOWN)) {
			engine.statc[1]++
			if(engine.statc[1]>2) engine.statc[1] = 0
			engine.playSE("change")
		}

		return super.onResult(engine)
	}

	/* Render results screen */
	override fun renderResult(engine:GameEngine) {
		receiver.drawMenu(
			engine, 0, 0, "${BaseFont.UP_L}${BaseFont.DOWN_L} PAGE${engine.statc[1]+1}/2", BASE, COLOR.RED
		)
		if(engine.statc[1]<=1) {
			if(rankingRank[0]==0) receiver.drawMenu(engine, 0, 3, "NEW RECORD", BASE, COLOR.ORANGE)
			else if(rankingRank[0]!=-1)
				receiver.drawMenu(engine, 4, 3, "RANK %d".format(rankingRank[0]+1), BASE, COLOR.ORANGE)
			if(rankingRank[1]==0) receiver.drawMenu(engine, 0, 6, "NEW RECORD", BASE, COLOR.ORANGE)
			else if(rankingRank[1]!=-1)
				receiver.drawMenu(engine, 4, 6, "RANK %d".format(rankingRank[1]+1), BASE, COLOR.ORANGE)
			if(rankingRank[2]==0) receiver.drawMenu(engine, 0, 9, "NEW RECORD", BASE, COLOR.ORANGE)
			else if(rankingRank[2]!=-1)
				receiver.drawMenu(engine, 4, 9, "RANK %d".format(rankingRank[2]+1), BASE, COLOR.ORANGE)
		}
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

				drawResultStats(engine, receiver, 10, COLOR.BLUE, Statistic.PPS, Statistic.APL)
			}
			2 -> {
				drawResultStats(engine, receiver, 1, COLOR.BLUE, Statistic.MAXCOMBO, Statistic.MAXB2B, Statistic.PIECE)
				drawResultStats(engine, receiver, 7, COLOR.BLUE, Statistic.LPM)

				drawResultStats(engine, receiver, 10, COLOR.BLUE, Statistic.SPL, Statistic.APL)
			}
		}
		drawResultNetRank(engine, receiver, 14, COLOR.BLUE, netRankingRank[0])
		drawResultNetRankDaily(engine, receiver, 16, COLOR.BLUE, netRankingRank[1])

		if(netIsPB) receiver.drawMenu(engine, 2, 21, "NEW PB", BASE, COLOR.ORANGE)

		if(netIsNetPlay&&netReplaySendStatus==1)
			receiver.drawMenu(engine, 0, 22, "SENDING...", BASE, COLOR.PINK)
		else if(netIsNetPlay&&!netIsWatch
			&&netReplaySendStatus==2
		)
			receiver.drawMenu(engine, 1, 22, "A: RETRY", BASE, COLOR.RED)
	}

	/* Called when saving replay */
	override fun saveReplay(engine:GameEngine, prop:CustomProperties):Boolean {
		itemSpd.presetSave(engine, prop, menu.propName, -1)
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
				for(j in rankingMax-1 downTo ret+1) {
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
		for(i in 0..<rankingMax)
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
		val bg = if(owner.bgMan.fadeSW) owner.bgMan.nextBg else owner.bgMan.bg
		val msg = "game\tstats\t"+
			"${engine.statistics.scoreLine}\t${engine.statistics.scoreSD}\t${engine.statistics.scoreHD}\t"+
			"${engine.statistics.scoreBonus}\t${engine.statistics.lines}\t"+
			"${engine.statistics.totalPieceLocked}\t${engine.statistics.time}\t$goalType\t"+
			"${engine.gameActive}\t${engine.timerActive}\t"+
			"$lastScore\t$scDisp\t${engine.lastEvent}\t$bg\n"
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
			{lastScore = it.toInt()},
			{/*scDisp = it.toInt()*/},
			{engine.lastEvent = ScoreEvent.parseStr(it)},
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
		netLobby?.netPlayerClient?.send(msg)
	}

	/** NET: Send game options to all spectators
	 * @param engine GameEngine
	 */
	override fun netSendOptions(engine:GameEngine) {
		val msg = "game\toption\t"+
			"${engine.speed.gravity}\t${engine.speed.denominator}\t${engine.speed.are}\t"+
			"${engine.speed.areLine}\t${engine.speed.lineDelay}\t${engine.speed.lockDelay}\t"+
			"${engine.speed.das}\t$big\t$goalType\t$presetNumber\t"
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
		big = message[11].toBoolean()
		goalType = message[12].toInt()
		presetNumber = message[13].toInt()
	}

	/** NET: Get goal type */
	override val netGetGoalType get() = goalType

	/** NET: It returns true when the current settings don't prevent
	 * leaderboard screen from showing. */
	override fun netIsNetRankingViewOK(engine:GameEngine):Boolean = !big&&engine.ai==null

	companion object {
		/** Current version */
		private const val CURRENT_VERSION = 1

		/** Minutes counts when game ending occurs */
		private val tableLength = listOf(3, 5)

		/** Number of ranking types */
		private enum class RankingType { Score, Power, Lines }

		private val RANKTYPE_ALL = RankingType.entries
		private val RANKTYPE_MAX = RANKTYPE_ALL.size

		/** Time limit type */
		private val GOALTYPE_MAX = tableLength.size*2
	}
}
