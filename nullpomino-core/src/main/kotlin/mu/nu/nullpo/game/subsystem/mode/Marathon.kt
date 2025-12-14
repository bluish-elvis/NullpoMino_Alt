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
import mu.nu.nullpo.game.event.Leaderboard
import mu.nu.nullpo.game.event.Rankable
import mu.nu.nullpo.game.event.ScoreEvent
import mu.nu.nullpo.game.net.NetUtil
import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.game.subsystem.mode.menu.*
import mu.nu.nullpo.gui.common.BaseFont.FONT.*
import mu.nu.nullpo.util.CustomProperties
import mu.nu.nullpo.util.GeneralUtil.times
import mu.nu.nullpo.util.GeneralUtil.toInt
import mu.nu.nullpo.util.GeneralUtil.toTimeStr

/** MARATHON Mode */
class Marathon:NetDummyMode() {
	/** Current BGM */
	private var bgmLv:BGM = BGM.Silent

	private val itemLevel = LevelMenuItem("startlevel", "LEVEL", COLOR.RED, 0, 0..99, true, true)
	/** Level at start */
	private var startLevel:Int by DelegateMenuItem(itemLevel)

	private val itemMode =
		StringsMenuItem("goalType", "GOAL", COLOR.BLUE, 0, tableGameClearLines.map {if(it<=0) "ENDLESS" else "$it LINES"})
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

	override val ranking = List(RANKING_TYPE) {
		Leaderboard(rankingMax, kotlinx.serialization.serializer<List<Rankable.ScoreRow>>()){Rankable.ScoreRow()}
	}

	// Mode name
	override val name = "Marathon"

	/* Initialization */
	override fun playerInit(engine:GameEngine) {
		super.playerInit(engine)
		lastScore = 0
		bgmLv = BGM.Silent

		rankingRank = -1

		netPlayerInit(engine)

		if(!owner.replayMode) version = CURRENT_VERSION else {
			if(version==0&&owner.replayProp.getProperty("marathon.endless", false)) goalType = tableGameClearLines.size-1

			// NET: Load name
			netPlayerName = owner.replayProp.getProperty("${engine.playerID}.net.netPlayerName", "")
		}
		engine.statistics.level = startLevel
		owner.bgMan.bg = startLevel
		setSpeed(engine)
		engine.frameSkin = GameEngine.FRAME_COLOR_GREEN
	}

	/** Set the gravity rate
	 * @param engine GameEngine
	 */
	override fun setSpeed(engine:GameEngine) {
		val goal = tableGameClearLines[goalType]
		val starts = startLevel*10
		val lv = engine.statistics.lines.coerceIn(starts, goal.let {
			if(it>starts) it else maxOf(engine.statistics.lines, starts)
		})
		val data = if(goal in 0..200) tableSpeeds.first else tableSpeeds.second
		val sLv = (lv/(if(goal<=500) 10 else 20).coerceIn(0, data.size-1))

		engine.speed.replace(data[sLv])

		val ln = lv/when {
			goal<0 -> 3
			goal<=200 -> 1
			goal<=500 -> 2
			else -> 4
		}
		engine.speed.are = maxOf(0, minOf(20-ln/30, 50-ln/3), if(goal in 0..200) 0 else 9-ln/25)
		engine.speed.areLine = maxOf(0, minOf(20-ln/30, 50-ln/3), if(goal in 0..200) 0 else 10-ln/15)
		engine.speed.lineDelay = maxOf(0, minOf(30-ln/10, 35-ln/5, 50-ln/3), if(goal in 0..200) 0 else 9-ln/25)
		engine.speed.lockDelay = when {
			goal<0 -> maxOf(65-lv/10, 53-lv/30)
			goal<=200 -> 50-lv/10
			goal<=500 -> maxOf(65-lv/10, 52-lv/20)
			else -> maxOf(65-lv/20, 52-lv/40)
		}.coerceIn(20, 48)
		engine.speed.das = (17-ln/20).coerceIn(6, 14)

	}

	/* Called at settings screen */
	override fun onSettingChanged(engine:GameEngine) {
		if(startLevel>(tableGameClearLines[goalType]-1)/10&&tableGameClearLines[goalType]>=0) {
			startLevel = 0
			owner.bgMan.bg = startLevel
		}

		owner.bgMan.bg = startLevel
		engine.statistics.level = startLevel
		engine.frameSkin = (1+startLevel)%GameEngine.FRAME_COLOR_ALL
		setSpeed(engine)

		return super.onSettingChanged(engine)
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

		owner.musMan.bgm = if(netIsWatch) BGM.Silent else bgmLv(0)
		engine.frameSkin = (1+startLevel)%GameEngine.FRAME_COLOR_ALL
	}

	/* Render score */
	override fun renderLast(engine:GameEngine) {
		if(owner.menuOnly) return
		if(tableGameClearLines[goalType]<=0) receiver.drawScore(engine, 0, 0, "ENDLESS MARATHON", BASE, COLOR.GREEN)
		else receiver.drawScore(engine, 0, 0, "${tableGameClearLines[goalType]} LINES MARATHON", BASE, COLOR.GREEN)

		if(engine.stat==GameEngine.Status.SETTING||engine.stat==GameEngine.Status.RESULT&&!owner.replayMode) {
			if(!owner.replayMode&&!big&&engine.ai==null) {
				val topY = if(receiver.nextDisplayType==2) 6 else 4
				receiver.drawScore(engine, 2, topY-1, "SCORE LINE TIME", BASE, COLOR.BLUE)

				ranking[goalType].forEachIndexed { i, it ->
					receiver.drawScore(engine, 0, topY+i, "%2d".format(i+1),
						GRADE,
						if(rankingRank==i) COLOR.RAINBOW else if(it.li>tableGameClearLines[goalType]) COLOR.CYAN else
							COLOR.YELLOW)
					receiver.drawScore(engine, 2, topY+i, "${it.sc}", NUM, i==rankingRank)
					receiver.drawScore(engine, 9, topY+i, "${it.li}", NUM, i==rankingRank)
					receiver.drawScore(engine, 12, topY+i, it.ti.toTimeStr, NUM, i==rankingRank)
				}
			}
		} else {
			receiver.drawScore(engine, 0, 3, "Lines", BASE, COLOR.BLUE)
			receiver.drawScore(engine, 5, 2, engine.statistics.lines.toString(), NUM, 2f)

			receiver.drawScore(engine, 0, 4, "Score", BASE, COLOR.BLUE)
			receiver.drawScore(engine, 5, 4, "+$lastScore", NUM)
			val scget = scDisp<engine.statistics.score
			receiver.drawScore(engine, 0, 5, "$scDisp", NUM, scget, 2f)

			receiver.drawScore(engine, 0, 8, "Level", BASE, COLOR.BLUE)
			receiver.drawScore(engine, 5, 8, "%.1f".format(
				engine.statistics.level.toFloat()+if(engine.statistics.level>=19&&tableGameClearLines[goalType]<0) 1f else engine.statistics.lines%10*0.1f+1f),
				NUM, 2f)

			receiver.drawScore(engine, 0, 9, "Time", BASE, COLOR.BLUE)
			receiver.drawScore(engine, 0, 10, engine.statistics.time.toTimeStr, NUM_T)
		}

		super.renderLast(engine)
	}

	private fun nextbgmLine(lines:Int) =
		tableBGMChange[goalType].filterKeys {it>0&&lines+startLevel*10<it}.minByOrNull {it.key}?.key
			?:tableGameClearLines[goalType].let {if(it>0) it else lines+10}

	fun bgmLv(lines:Int) =
		tableBGMChange[goalType].filterKeys {it>0&&lines+startLevel*10<it}.minByOrNull {it.key}?.value
			?:tableBGMChange[goalType].maxBy {it.key}.value
	/* Calculate score */
	override fun calcScore(engine:GameEngine, ev:ScoreEvent):Int {
		super.calcScore(engine, ev)
		// BGM fade-out effects and BGM changes
		if(engine.statistics.lines>=nextbgmLine(engine.statistics.lines)-1) owner.musMan.fadeSW = true
		bgmLv(engine.statistics.lines).let {newBgm ->
			if(bgmLv!=newBgm) {
				bgmLv = newBgm
				owner.musMan.bgm = newBgm
				owner.musMan.fadeSW = false
			}
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
			engine.frameSkin = (1+engine.statistics.level)%GameEngine.FRAME_COLOR_ALL
			owner.bgMan.nextBg = engine.statistics.level

			setSpeed(engine)
			engine.playSE("levelup")
		}
		return if(ev.lines>0) lastScore else 0
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
				engine, receiver, 0, COLOR.BLUE, Statistic.SCORE, Statistic.LINES, Statistic.TIME,
				Statistic.PIECE, Statistic.ATTACKS, Statistic.SPL
			)
			engine.statc[1]==1 -> drawResultStats(
				engine, receiver, 0, COLOR.BLUE, Statistic.SPM, Statistic.LPM, Statistic.TIME,
				Statistic.PPS, Statistic.APM, Statistic.SPL
			)
		}
		drawResultRank(engine, receiver, 12, COLOR.BLUE, rankingRank)
		drawResultNetRank(engine, receiver, 14, COLOR.BLUE, netRankingRank[0])
		drawResultNetRankDaily(engine, receiver, 16, COLOR.BLUE, netRankingRank[1])

		if(netIsPB) receiver.drawMenu(engine, 2, 19, "NEW PB", BASE, COLOR.ORANGE)

		if(netIsNetPlay&&netReplaySendStatus==1)
			receiver.drawMenu(engine, 0, 22, "SENDING...", BASE, COLOR.PINK)
		else if(netIsNetPlay&&!netIsWatch&&netReplaySendStatus==2)
			receiver.drawMenu(engine, 1, 22, "A: RETRY", BASE, COLOR.RED)
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
			rankingRank=ranking[goalType].add(Rankable.ScoreRow(engine.statistics))
			if(rankingRank!=-1) return true
		}
		return false
	}

	/** NET: Send various in-game stats of [engine] */
	override fun netSendStats(engine:GameEngine) {
		val bg = if(owner.bgMan.fadeSW) owner.bgMan.nextBg else owner.bgMan.bg
		val msg = "game\tstats\t"+engine.run {
			statistics.run {
				"${scoreLine}\t${scoreSD}\t${scoreHD}\t${scoreBonus}\t${lines}\t${totalPieceLocked}\t${time}\t${level}\t"
			}+"$goalType\t${gameActive}\t${timerActive}\t$lastScore\t$scDisp\t${lastEvent}\t$bg\n"
		}
		netLobby?.netPlayerClient?.send(msg)
	}

	/** NET: Parse Received [message] as in-game stats of [engine] */
	override fun netRecvStats(engine:GameEngine, message:List<String>) {
		listOf<(String)->Unit>({}, {}, {}, {}, {engine.statistics.scoreLine = it.toInt()},
			{engine.statistics.scoreSD = it.toInt()}, {engine.statistics.scoreHD = it.toInt()},
			{engine.statistics.scoreBonus = it.toInt()}, {engine.statistics.lines = it.toInt()},
			{engine.statistics.totalPieceLocked = it.toInt()}, {engine.statistics.time = it.toInt()},
			{engine.statistics.level = it.toInt()}, {goalType = it.toInt()}, {engine.gameActive = it.toBoolean()},
			{engine.timerActive = it.toBoolean()}, {lastScore = it.toInt()}, {/*scDisp = it.toInt()*/},
			{engine.lastEvent = ScoreEvent.parseStr(it)}, {owner.bgMan.bg = it.toInt()}).zip(message).forEach {(x, y) ->
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
		netLobby?.netPlayerClient?.send(msg)
	}

	/** NET: Send game options to all spectators
	 * @param engine GameEngine
	 */
	override fun netSendOptions(engine:GameEngine) {
		val msg = "game\toption\t$startLevel\t$goalType\t$big\n"
		netLobby?.netPlayerClient?.send(msg)
	}

	/** NET: Receive game options */
	override fun netRecvOptions(engine:GameEngine, message:List<String>) {
		startLevel = message[4].toInt()
		goalType = message[5].toInt()
		big = message[6].toBoolean()
	}

	/** NET: Get goal type */
	override val netGetGoalType get() = goalType

	/** NET: It returns true when the current settings don't prevent leaderboard screen from showing. */
	override fun netIsNetRankingViewOK(engine:GameEngine):Boolean = startLevel==0&&!big&&engine.ai==null

	companion object {
		/** Current version */
		private const val CURRENT_VERSION = 2

		/** Fall velocity table */
		private val tableSpeeds =
			LevelData(listOf(1, 2, +1, +3, +3, +7, +3, 12, +30, +26, 26, 3, 1, 3, 2, 3, 5, 15, 10, -1)/*(numerators) */,
				listOf(60, 95, 37, 85, 64, 110, 34, 97, 169, 100, 67, 5, 1, 2, 1, 1, 1, +2, +1, 1)/*(denominators)*/) to
				LevelData(listOf(1, 1, 1, +1, +1, +1, +2, +1, +1, +1, +3, +1, 1, 1, 1, 1, 1, 1, 3, 1, 2, 3, 5, 15, 10, -1),
					listOf(60, 50, 45, 40, 35, 30, 54, 25, 20, 15, 34, 10, 8, 6, 5, 4, 3, 2, 4, 1, 1, 1, 1, +2, +1, 1))

		//1/60 = 0.0166666666666667
		//2/95 = 0.0210526315789474
		//1/37 = 0.027027027027027
		//3/85 = 0.0352941176470588
		//3/64 = 0.046875
		//7/110 = 0.0636363636363636
		//3/34 = 0.0882352941176471
		//12/97 = 0.123711340206186
		//30/169 = 0.177514792899408
		//26/100 = 0.26
		//26/67 = 0.388059701492537
		//3/5 = 0.6
		//1/1 = 1

		//1/60 = 0.0166666666666667
		//1/50 = 0.02
		//1/45 = 0.0222222222222222
		//1/40 = 0.025
		//1/35 = 0.0285714285714286
		//1/30 = 0.0333333333333333
		//2/54 = 0.037037037037037
		//1/25 = 0.04
		//1/20 = 0.05
		//1/15 = 0.0666666666666667
		//3/34 = 0.0882352941176471
		//1/10 = 0.1
		//1/8 = 0.125
		//1/6 = 0.166666666666667
		//1/5 = 0.2
		//1/4 = 0.25
		//1/3 = 0.333333333333333
		//1/2 = 0.5

		/** Line counts when BGM changes occur
		 * value plays until key_Int level */
		private val tableBGMChange =
			listOf(List(4) {30+30*it to BGM.Zen(it)}.toMap(), List(7) {30+30*it to BGM.Zen(it)}.toMap(),
				List(7) {70+70*it+(it>=6)*10 to BGM.Generic(it)}.toMap(),
				(List(7) {70+70*it to BGM.Generic(it)}+List(5) {590+100*it-(it>=4).toInt() to BGM.Rush(it)}).toMap(),
				(List(3) {30+30*it to BGM.Puzzle(it)}+List(6) {240+40*it to BGM.Generic(it+1)}+List(5) {
					400+40*it to BGM.Rush(it)
				}).toMap())

		/** Line counts when game ending occurs */
		private val tableGameClearLines = listOf(150, 200, 500, 999, -1)

		/** Number of ranking types */
		private val RANKING_TYPE = tableGameClearLines.size

	}
}
