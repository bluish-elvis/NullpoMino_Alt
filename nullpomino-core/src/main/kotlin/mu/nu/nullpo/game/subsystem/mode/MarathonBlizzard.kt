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

import kotlinx.serialization.Serializable
import kotlinx.serialization.serializer
import mu.nu.nullpo.game.component.*
import mu.nu.nullpo.game.event.*
import mu.nu.nullpo.game.event.EventReceiver.COLOR
import mu.nu.nullpo.game.net.NetUtil
import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.game.subsystem.mode.menu.*
import mu.nu.nullpo.gui.common.AbstractRenderer
import mu.nu.nullpo.gui.common.BaseFont.FONT.*
import mu.nu.nullpo.gui.common.bg.tech.Snow
import mu.nu.nullpo.util.CustomProperties
import mu.nu.nullpo.util.GeneralUtil.times
import mu.nu.nullpo.util.GeneralUtil.toInt
import mu.nu.nullpo.util.GeneralUtil.toTimeStr

/** FREEZE CHALLENGE mode */
class MarathonBlizzard:NetDummyMode() {
	private val bgSnow = Snow()
	/** bottom line clears point */
	private var water = 0
	private var iceDug = 1
	private var norm = 0
	/** Number of garbage lines needed for next level */
	private var normMax = 0

	/** Version */
	private var version = 0

	/** Current round's ranking position */
	private var rankingRank = 0

	private val itemLevel = LevelMenuItem("startlevel", "LEVEL", COLOR.BLUE, 0, 0..19, false, true)
	/** Level at start */
	private var startLevel:Int by DelegateMenuItem(itemLevel)

	private val itemDas = IntegerMenuItem("das", "DAS", COLOR.BLUE, 0, 1..20, true, true)
	private var das:Int by DelegateMenuItem(itemDas)

	private val itemBGM = BGMMenuItem("bgmno", COLOR.BLUE, 0)
	/** BGM number */
	private var bgmId:Int by DelegateMenuItem(itemBGM)

	override val menu = MenuList("icestorm", itemLevel, itemDas, itemBGM)

	@Serializable
	data class ScoreRow(override val st:Statistics = Statistics(),
		val depth:Int = 0):Rankable, Comparable<Rankable> {
		override operator fun compareTo(other:Rankable):Int =
			if(other is ScoreRow)
				compareValuesBy(this, other, {it.sc}, {it.depth}, {it.li}, {it.lv}, {-it.ti})
			else super.compareTo(other)

	}

	override val ranking = listOf(Leaderboard(rankingMax, serializer<List<ScoreRow>>()))

	/* Mode name */
	override val name = "Blizzard Marathon"
	override val gameIntensity = 1
	/* Initialization for each player */
	override fun playerInit(engine:GameEngine) {
		super.playerInit(engine)
		bgSnow.reset()
		lastScore = 0

		water = 0
		norm = 0
		normMax = 0

		rankingRank = -1

		engine.frame = GameEngine.Frame.CYAN
		engine.statistics.levelDispAdd = 1

		netPlayerInit(engine)
		// NET: Load name
		if(owner.replayMode) netPlayerName = engine.owner.replayProp.getProperty(
			"${engine.playerID}.net.netPlayerName", ""
		)

		engine.owner.bgMan.bg = startLevel
	}

	/** Set the gravity rate
	 * @param engine GameEngine
	 */
	override fun setSpeed(engine:GameEngine) {
		val lv = engine.statistics.level.coerceIn(0, tableSpeeds.size-1)
		engine.speed.apply {
			replace(tableSpeeds[lv])
			das = this@MarathonBlizzard.das
		}
	}

	/* This function will be called before the game actually begins
 (afterReady&Go screen disappears) */
	override fun startGame(engine:GameEngine) {
		engine.statistics.level = startLevel
		engine.b2bEnable = true
		engine.splitB2B = true
		engine.comboType = GameEngine.COMBO_TYPE_NORMAL
		engine.twistAllowKick = true
		engine.twistEnable = true
		engine.useAllSpinBonus = true
		engine.twistEnableEZ = true

		norm = 0
		normMax = ICE_BLOCKS_NORM_BASE*(startLevel+1)

		setSpeed(engine)

		owner.musMan.bgm = if(netIsWatch) BGM.Silent else BGM.values[bgmId]
	}

	override fun renderFirst(engine:GameEngine) {
		super.renderFirst(engine)
		bgSnow.draw(engine.receiver as AbstractRenderer, false)
	}

	override fun onFirst(engine:GameEngine) {
		super.onFirst(engine)
		bgSnow.update()
	}
	/* Render score */
	override fun renderLast(engine:GameEngine) {
		super.renderLast(engine)
		if(owner.menuOnly) return

		receiver.drawScore(engine, 0, 0, name, BASE, color = COLOR.CYAN)

		if(engine.stat==GameEngine.Status.SETTING||engine.stat==GameEngine.Status.RESULT&&!owner.replayMode) {
			if(!owner.replayMode&&startLevel==0) {
				val topY = if(receiver.bigSideNext) 6 else 4
				receiver.drawScore(engine, 0, topY-1, "SCORE LINE DEPTH", BASE, COLOR.BLUE)

				ranking[goalType].forEachIndexed {i:Int, (it, depth):ScoreRow ->
					receiver.drawScore(engine, 0, topY+i, "%2d".format(i+1), GRADE, COLOR.YELLOW)
					receiver.drawScore(engine, 2, topY+i, "%6d".format(it.score), NUM, i==rankingRank)
					receiver.drawScore(engine, 7, topY+i, "%4d".format(it.lines), NUM, i==rankingRank)
					receiver.drawScore(engine, 12, topY+i, "$depth", NUM, i==rankingRank)
				}
			}
		} else {
			receiver.drawScore(engine, 0, 3, "Score", BASE, COLOR.BLUE)
			receiver.drawScore(engine, 0, 4, "${engine.statistics.score}", NUM, scale = 2f)
			receiver.drawScore(engine, 5, 3, "+$lastScore", NUM)

			receiver.drawScore(engine, 0, 6, "DEPTH", BASE, COLOR.BLUE)
			receiver.drawScore(engine, 0, 7, "$iceDug", NUM, scale = 2f)

			receiver.drawScore(engine, 0, 9, "LINE", BASE, COLOR.BLUE)
			receiver.drawScore(engine, 0, 10, "${engine.statistics.lines}", NUM, scale = 2f)

			receiver.drawScore(engine, 0, 12, "Level", BASE, COLOR.BLUE)
			receiver.drawScore(engine, 5, 12, "${engine.statistics.level+1}", NUM, scale = 2f)
			receiver.drawScore(engine, 1, 13, "$norm", NUM)
			val nextNorm = getNextNorm(engine.statistics.level)
			receiver.drawScoreSpeed(
				engine, 0, 14, (norm-normMax+nextNorm)*1f/nextNorm, 2f
			)
			receiver.drawScore(engine, 1, 15, "$normMax", NUM)

			receiver.drawScore(engine, 0, 16, "Time", BASE, COLOR.BLUE)
			receiver.drawScore(engine, 0, 17, engine.statistics.time.toTimeStr, NUM_T)
			receiver.drawMenu(engine, 10, 20, "%1d".format(water), NUM, COLOR.BLUE)

		}
	}

	/* Called after every frame */
	override fun onLast(engine:GameEngine) {
		super.onLast(engine)

		if(engine.gameActive&&engine.timerActive) {

			val freezeTime = getFreezeMaxTime(engine.statistics.level)
			if(!netIsWatch) {
				engine.field.filterBlocks {it, _, _ ->
					!it.getAttribute(Block.ATTRIBUTE.ERASE)
						&&it.elapsedFrames>=freezeTime&&it.hard!=-1
				}.forEach {(b) ->
					b.hard = -1
					b.darkness = -.5f
					b.color?.let {b.secondaryColor = it}
				}
				// NET: Send stats
				if(netIsNetPlay&&!netIsWatch&&netNumSpectators>0) netSendStats(engine)
			}
		}
	}

	/* Calculate score */
	override fun calcScore(engine:GameEngine, ev:ScoreEvent):Int {
		// Line clear bonus
		val li = ev.lines
		val pts = calcPoint(engine, ev)
		val ices = engine.field.lastLinesCleared.values.sumOf {it.values.count {it?.hard!=0}}
		if(li>0) {
			// Decrease waiting garbage
			val pow = calcPower(engine, ev, true)
			val lv = engine.statistics.level
			val iceBreak = getLineDownNorm(lv)*10
			water += iceBreak*(li>=4).toInt()+pow+engine.field.lastLinesCleared.values.let {llc ->
				val fli = llc.count {it.any {(_, b) -> b?.hard!=0}}
				val fbc = llc.sumOf {it.values.filter {b -> b?.hard!=0}.size}
				5+fli*5+minOf(maxOf(fbc/2, minOf(fli*10, 20-(engine.field.highestBlockY*2-10).coerceIn(0, 20))), 20)
				+ev.combo+ev.twist*10+ev.split*10+lv
			}

			var get = pts
			while(water>=iceBreak) {
				val it = engine.field.getRow(engine.field.bottomY).count {b -> b?.hard!=0}
				engine.field.delLine(engine.field.heightWoFloor-maxOf(1, water/iceBreak))
				norm += it
				water -= iceBreak
				iceDug++
				get += 10+10*it
			}
			// Combo
			val cmb = if(ev.combo>0) ev.combo else 0
			// Add to score
			get = calcScoreCombo(get, cmb, 0, 0)

			get += ices*3
			levelUp(engine)

			lastScore = get
			engine.statistics.scoreLine += get
		} else {
			if(pts>0) {
				lastScore = pts
				engine.statistics.scoreBonus += pts
			}
		}

		return pts+ices*10
	}

	/** Get garbage time limit
	 * @param lv Level
	 * @return Garbage time limit
	 */
	private fun getFreezeMaxTime(lv:Int):Int =
		FREEZE_TIMER_TABLE[lv.coerceIn(FREEZE_TIMER_TABLE.indices)]

	private fun getNextNorm(lv:Int):Int =
		ICE_BLOCKS_NORM_BASE*(lv+5)/5

	private fun getLineDownNorm(lv:Int):Int =
		ICE_BLOCK_TABLE[lv]
	private fun levelUp(engine:GameEngine) {
		if(engine.gameActive&&engine.timerActive) {
			// Level up
			updateMeter(engine)
			var lvupFlag = false
			while(norm>=normMax&&engine.statistics.level<19) {
				engine.statistics.level++
				normMax += getNextNorm(engine.statistics.level)
				lvupFlag = true
			}

			if(lvupFlag) {
				owner.bgMan.nextBg = engine.statistics.level
				setSpeed(engine)
				engine.playSE("levelup")
			}
		}

	}

	/** Update progress meter*/
	private fun updateMeter(engine:GameEngine) {
		if(normMax>0) {
			val remainLines = getNextNorm(engine.statistics.level)
			engine.meterValue = (norm-normMax+remainLines)*1f/remainLines
			engine.meterColor = GameEngine.METER_COLOR_LIMIT
		}
	}

	override fun onResult(engine:GameEngine):Boolean {
		owner.musMan.fadeSW = false
		owner.musMan.bgm = if(engine.statistics.time<10800) BGM.Result(1) else BGM.Result(2)

		return super.onResult(engine)
	}

	/* Results screen */
	override fun renderResult(engine:GameEngine) {
		drawResultStats(engine, receiver, 0, COLOR.BLUE, Statistic.SCORE, Statistic.LINES)
		drawResult(engine, receiver, 4, COLOR.BLUE, "DEPTH", "%10d".format(iceDug))
		drawResultStats(engine, receiver, 6, COLOR.BLUE, Statistic.PIECE, Statistic.LEVEL, Statistic.TIME)
		drawResultRank(engine, receiver, 12, COLOR.BLUE, rankingRank)
		drawResultNetRank(engine, receiver, 14, COLOR.BLUE, netRankingRank[0])
		drawResultNetRankDaily(engine, receiver, 16, COLOR.BLUE, netRankingRank[1])

		if(netIsPB) receiver.drawMenu(engine, 2, 18, "NEW PB", BASE, COLOR.ORANGE)

		if(netIsNetPlay&&netReplaySendStatus==1) receiver.drawMenu(engine, 0, 19, "SENDING...", BASE, COLOR.PINK)
		else if(netIsNetPlay&&!netIsWatch&&netReplaySendStatus==2)
			receiver.drawMenu(engine, 1, 19, "A: RETRY", BASE, COLOR.RED)
	}

	/* Called when saving replay */
	override fun saveReplay(engine:GameEngine, prop:CustomProperties):Boolean {
		saveSetting(engine, prop)

		// NET: Save name
		if(netPlayerName!=null&&netPlayerName!!.isNotEmpty()) prop.setProperty(
			"${engine.playerID}.net.netPlayerName", netPlayerName
		)

		// Update rankings
		if(!owner.replayMode&&startLevel==0&&engine.ai==null) {
			//owner.statsProp.setProperty("decoration", decoration)
			return ranking[goalType].add(ScoreRow(engine.statistics, iceDug)).also {
				rankingRank = it
			}!=-1

		}
		return false
	}

	/** NET: Send various in-game stats of [engine] */
	override fun netSendStats(engine:GameEngine) {
		val bg =
			if(engine.owner.bgMan.fadeSW) engine.owner.bgMan.nextBg else engine.owner.bgMan.bg
		val msg = "game\tstats\t"+
			"${engine.statistics.scoreLine}\t${engine.statistics.scoreBonus}\t${engine.statistics.lines}\t"+
			"${engine.statistics.totalPieceLocked}\t${engine.statistics.time}\t${engine.statistics.level}\t"+
			"$iceDug\t${engine.gameActive}\t${engine.timerActive}\t"+
			"$lastScore\t$scDisp\t$bg\n"
		netLobby?.netPlayerClient?.send(msg)
	}

	/** NET: Parse Received [message] as in-game stats of [engine] */
	override fun netRecvStats(engine:GameEngine, message:List<String>) {
		listOf<(String)->Unit>(
			{},
			{},
			{},
			{},
			{engine.statistics.scoreLine = it.toInt()},
			{engine.statistics.scoreBonus = it.toInt()},
			{engine.statistics.lines = it.toInt()},
			{engine.statistics.totalPieceLocked = it.toInt()},
			{engine.statistics.time = it.toInt()},
			{engine.statistics.level = it.toInt()},
			{iceDug = it.toInt()},
			{engine.gameActive = it.toBoolean()},
			{engine.timerActive = it.toBoolean()},
			{lastScore = it.toInt()},
			{/*scDisp = it.toInt()*/},
			{engine.owner.bgMan.bg = it.toInt()},
		).zip(message).forEach {(x, y) -> x(y)}

		// Meter
		updateMeter(engine)
	}

	/** NET: Send end-of-game stats
	 * @param engine GameEngine
	 */
	override fun netSendEndGameStats(engine:GameEngine) {
		val subMsg =
			"SCORE;${engine.statistics.score}\t"+
				"LINE;${engine.statistics.lines}\t"+
				"DEOTH;$iceDug\t"+
				"PIECE;${engine.statistics.totalPieceLocked}\t"+
				"LEVEL;${engine.statistics.level+engine.statistics.levelDispAdd}\t"+
				"TIME;${engine.statistics.time.toTimeStr}\t"

		val msg = "gstat1p\t${NetUtil.urlEncode(subMsg)}\n"
		netLobby?.netPlayerClient?.send(msg)
	}

	/** NET: Send game options to all spectators
	 * @param engine GameEngine
	 */
	override fun netSendOptions(engine:GameEngine) {
		val msg = "game\toption\t0\t$startLevel\t$bgmId\t${engine.speed.das}\n"
		netLobby?.netPlayerClient?.send(msg)
	}

	/** NET: Receive game options */
	override fun netRecvOptions(engine:GameEngine, message:List<String>) {
		startLevel = message[5].toInt()
		bgmId = message[6].toInt()
		engine.speed.das = message[7].toInt()
	}

	val goalType = 0
	/** NET: Get goal type */
	override val netGetGoalType get() = 0

	/** NET: It returns true when the current settings don't prevent leaderboard screen from showing. */
	override fun netIsNetRankingViewOK(engine:GameEngine):Boolean = startLevel==0&&engine.ai==null

	companion object {
		private const val GOALTYPE_MAX = 1

		/** Number of garbage lines for each level */
		private const val ICE_BLOCKS_NORM_BASE = 45

		private val tableSpeeds = LevelData(
			listOf(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 465, 731, 1280, 1707, -1, -1, -1)/*(numerators)*/,
			listOf(64, 50, 39, 30, 22, 16, 12, 8, 6, 4, 3, 2, 1, 256, 256, 256, 256, 256, 256, 256)/* (denominators) */,
			12, 10, 3, 30, 14
		)

		/** Garbage speed table */
		private val FREEZE_TIMER_TABLE = intArrayOf(
			360, 340, 320, 310, 300, 290, 280, 270, 260, 250,
			240, 230, 220, 210, 200, 190, 180, 170, 160, 150)

		private val ICE_BLOCK_TABLE = intArrayOf(
			9, 12, 15, 18, 21, 24, 27, 30, 33, 36,
			40, 45, 50, 54, 58, 63, 72, 81, 90, 100)

	}
}
