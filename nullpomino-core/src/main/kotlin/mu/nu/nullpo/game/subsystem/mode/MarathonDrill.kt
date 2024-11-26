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
import mu.nu.nullpo.game.component.Block
import mu.nu.nullpo.game.component.LevelData
import mu.nu.nullpo.game.event.EventReceiver.COLOR
import mu.nu.nullpo.game.event.ScoreEvent
import mu.nu.nullpo.game.net.NetUtil
import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.game.subsystem.mode.menu.*
import mu.nu.nullpo.util.CustomProperties
import mu.nu.nullpo.util.GeneralUtil.toTimeStr

/** DIG CHALLENGE mode */
class MarathonDrill:NetDummyMode() {
	/** Previous garbage hole */
	private var garbageHole = 0
	private var garbageHistory = MutableList(7) {0}

	/** Garbage timer */
	private var garbageTimer = 0

	/** Number of total garbage lines dug */
	private var garbageDug = 0

	/** Number of total garbage lines risen */
	private var garbageTotal = 0

	private var norm = 0
	/** Number of garbage lines needed for next level */
	private var normMax = 0

	/** Number of garbage lines waiting to appear (Normal type) */
	private var garbagePending = 0

	/** Version */
	private var version = 0

	/** Current round's ranking position */
	private var rankingRank = 0

	private val itemLevel = LevelMenuItem("startlevel", "LEVEL", COLOR.BLUE, 0, 0..19, false, true)
	/** Level at start */
	private var startLevel:Int by DelegateMenuItem(itemLevel)

	private val itemMode = StringsMenuItem(
		"goalType", "GAME TYPE", COLOR.BLUE, 0, listOf("NORMAL", "REALTIME")
	)
	/** Game type */
	private var goalType:Int by DelegateMenuItem(itemMode)

	private val itemHeight = IntegerMenuItem("height", "HEIGHT", COLOR.BLUE, GARBAGE_BOTTOM, 0..10, true, true)
	/** Garbage Height */
	private var garbageHeight:Int by DelegateMenuItem(itemHeight)

	private val itemDas = IntegerMenuItem("das", "DAS", COLOR.BLUE, 0, 1..20, true, true)
	private var das:Int by DelegateMenuItem(itemDas)

	private val itemBGM = BGMMenuItem("bgmno", COLOR.BLUE, 0)
	/** BGM number */
	private var bgmId:Int by DelegateMenuItem(itemBGM)

	override val menu = MenuList("digchallenge", itemMode, itemHeight, itemLevel, itemDas, itemBGM)

	/** Rankings' scores */
	private val rankingScore = List(GOALTYPE_MAX) {MutableList(rankingMax) {0L}}

	/** Rankings' line counts */
	private val rankingLines = List(GOALTYPE_MAX) {MutableList(rankingMax) {0}}

	/** Rankings' depth */
	private val rankingDepth = List(GOALTYPE_MAX) {MutableList(rankingMax) {0}}

	override val propRank
		get() = rankMapOf(
			rankingScore.mapIndexed {a, x -> "$a.stage" to x}+rankingLines.mapIndexed {a, x -> "$a.lines" to x}+rankingDepth.mapIndexed {a, x -> "$a.depth" to x}/*+
				rankingTime.mapIndexed {a, x -> "$a.time" to x}*/
		)

	/* Mode name */
	override val name = "Drill Marathon"
	override val gameIntensity = 1
	/* Initialization for each player */
	override fun playerInit(engine:GameEngine) {
		rankingScore.forEach {it.fill(0)}
		rankingLines.forEach {it.fill(0)}
		rankingDepth.forEach {it.fill(0)}
		super.playerInit(engine)

		lastScore = 0

		garbageHole = -1
		garbageHistory.fill(-1)
		garbageTimer = 0
		garbageDug = 0
		garbageTotal = 0
		norm = 0
		normMax = 0
		garbagePending = 0
		garbageHeight = GARBAGE_BOTTOM

		rankingRank = -1

		engine.frameColor = GameEngine.FRAME_COLOR_GREEN
		engine.statistics.levelDispAdd = 1

		netPlayerInit(engine)
		// NET: Load name
		if(!owner.replayMode) version = CURRENT_VERSION else netPlayerName = engine.owner.replayProp.getProperty(
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
			das = this@MarathonDrill.das
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

		garbageTotal = LEVEL_GARBAGE_LINES*startLevel
		norm = 0
		normMax = LEVEL_GARBAGE_LINES*(startLevel+1)

		setSpeed(engine)

		owner.musMan.bgm = if(netIsWatch) BGM.Silent else BGM.values[bgmId]
	}

	override fun onReady(engine:GameEngine):Boolean {
		if(engine.statc[0]<=1) {
			engine.goStart = maxOf(50, 40+garbageHeight*5)
			engine.readyEnd = engine.goStart-1
			engine.goEnd = engine.goStart+50
		}
		if(garbageHeight>0&&engine.statc[0] in 30..<30+garbageHeight*5&&engine.statc[0]%5==0) addGarbage(engine)
		return false
	}

	/* Render score */
	override fun renderLast(engine:GameEngine) {
		super.renderLast(engine)
		if(owner.menuOnly) return

		receiver.drawScoreFont(engine, 0, 0, name, color = COLOR.GREEN)
		receiver.drawScoreFont(engine, 0, 1, if(goalType==0) "(NORMAL RUN)" else "(REALTIME RUN)", COLOR.GREEN)

		if(engine.stat==GameEngine.Status.SETTING||engine.stat==GameEngine.Status.RESULT&&!owner.replayMode) {
			if(!owner.replayMode&&startLevel==0) {
				val topY = if(receiver.nextDisplayType==2) 6 else 4
				receiver.drawScoreFont(engine, 0, topY-1, "SCORE LINE DEPTH", COLOR.BLUE)

				for(i in 0..<rankingMax) {
					receiver.drawScoreGrade(engine, 0, topY+i, "%2d".format(i+1), COLOR.YELLOW)
					receiver.drawScoreNum(engine, 2, topY+i, "${rankingScore[goalType][i]}", i==rankingRank)
					receiver.drawScoreNum(engine, 7, topY+i, "${rankingLines[goalType][i]}", i==rankingRank)
					receiver.drawScoreNum(engine, 12, topY+i, "${rankingDepth[goalType][i]}", i==rankingRank)
				}
			}
		} else {
			receiver.drawScoreFont(engine, 0, 3, "Score", COLOR.BLUE)
			receiver.drawScoreNum(engine, 0, 4, "${engine.statistics.score}", scale = 2f)
			receiver.drawScoreNum(engine, 5, 3, "+$lastScore")

			receiver.drawScoreFont(engine, 0, 6, "DEPTH", COLOR.BLUE)
			receiver.drawScoreNum(engine, 0, 7, "$garbageDug", scale = 2f)

			receiver.drawScoreFont(engine, 0, 9, "LINE", COLOR.BLUE)
			receiver.drawScoreNum(engine, 0, 10, "${engine.statistics.lines}", scale = 2f)

			receiver.drawScoreFont(engine, 0, 12, "Level", COLOR.BLUE)
			receiver.drawScoreNum(engine, 5, 12, "${engine.statistics.level+1}", scale = 2f)
			receiver.drawScoreNum(engine, 1, 13, "$norm")
			receiver.drawScoreSpeed(
				engine, 0, 14, norm%LEVEL_GARBAGE_LINES*1f/(LEVEL_GARBAGE_LINES-1), 2f
			)
			receiver.drawScoreNum(engine, 1, 15, "$normMax")

			receiver.drawScoreFont(engine, 0, 16, "Time", COLOR.BLUE)
			receiver.drawScoreNum(engine, 0, 17, engine.statistics.time.toTimeStr, scale = 2f)

			if(garbagePending>0) {
				val fontColor = when {
					garbagePending>=4 -> COLOR.RED
					garbagePending>=3 -> COLOR.ORANGE
					else -> COLOR.YELLOW
				}
				val strTempGarbage = "%2d".format(garbagePending)
				receiver.drawMenuNum(engine, 10, 20, strTempGarbage, fontColor)
			}
			receiver.drawMenuFont(engine, garbageHole, 20, "\u008b")
		}
	}

	/* Called after every frame */
	override fun onLast(engine:GameEngine) {
		super.onLast(engine)

		if(engine.gameActive&&engine.timerActive) {
			garbageTimer++
			val maxTime = getGarbageMaxTime(engine.statistics.level)
			// Update meter
			updateMeter(engine)
			if(!netIsWatch) {
				engine.field.let {
					// Add pending garbage (Normal)
					while(garbageTimer>=maxTime) {
						garbagePending++
						garbageTimer -= maxTime
					}
					if(goalType==GOALTYPE_REALTIME&&garbagePending>0&&engine.stat!=GameEngine.Status.LINECLEAR) {
						// Add Garbage (Realtime)
						garbageTimer %= maxTime

						addGarbage(engine, garbagePending)
						garbagePending = 0

						// NET: Send field and stats
						if(netIsNetPlay&&!netIsWatch&&netNumSpectators>0) netSendField(engine)


						if(engine.stat==GameEngine.Status.MOVE) engine.nowPieceObject?.let {nowPieceObject ->
							if(nowPieceObject.checkCollision(engine.nowPieceX, engine.nowPieceY, it)) {
								// Push up the current piece
								while(nowPieceObject.checkCollision(engine.nowPieceX, engine.nowPieceY, it)) engine.nowPieceY--

								// Pushed out from the visible part of the field
								if(!nowPieceObject.canPlaceToVisibleField(engine.nowPieceX, engine.nowPieceY)) {
									engine.stat = GameEngine.Status.GAMEOVER
									engine.resetStatc()
									engine.gameEnded()
								}
							}

							// Update ghost position
							engine.nowPieceBottomY = nowPieceObject.getBottom(engine.nowPieceX, engine.nowPieceY, it)

							// NET: Send piece movement
							if(netIsNetPlay&&!netIsWatch&&netNumSpectators>0) netSendPieceMovement(engine, true)
						}
					}
				}
				// NET: Send stats
				if(netIsNetPlay&&!netIsWatch&&netNumSpectators>0) netSendStats(engine)
			}
		}
	}

	/** Update timer meter
	 * @param engine GameEngine
	 */
	private fun updateMeter(engine:GameEngine) {
		val limitTime = getGarbageMaxTime(engine.statistics.level)
		var remainTime = limitTime-garbageTimer
		if(remainTime<0) remainTime = 0
		engine.meterValue = if(limitTime>0) remainTime*1f/limitTime else 0f
		engine.meterColor = GameEngine.METER_COLOR_LIMIT
	}

	/* Calculate score */
	override fun calcScore(engine:GameEngine, ev:ScoreEvent):Int {
		garbageTimer -= getGarbageMaxTime(engine.statistics.level)/50
		// Line clear bonus
		val li = ev.lines
		val pts = calcPoint(engine, ev)
		val cln = engine.garbageClearing
		if(li>0) {
			garbageDug += cln
			// Combo
			val cmb = if(ev.combo>0) ev.combo else 0
			// Add to score
			var get = calcScoreCombo(pts, cmb, 0, 0)

			get += cln*100
			// Decrease waiting garbage
			val pow = calcPower(engine, ev, true)
			norm += pow
			levelUp(engine)
			garbageTimer -= maxOf(0, 60-engine.statistics.level*2-cmb*7)+pow*maxOf(1, 30-engine.statistics.level)/2
			if(goalType==GOALTYPE_NORMAL) while(garbagePending>0&&garbageTimer<0) {
				garbageTimer += getGarbageMaxTime(engine.statistics.level)
				garbagePending--
			}

			lastScore = get
			engine.statistics.scoreLine += get
		} else {
			if(goalType==GOALTYPE_NORMAL&&garbagePending>0) {
				addGarbage(engine, garbagePending)
				garbagePending = 0
			}
			if(pts>0) {
				lastScore = pts
				engine.statistics.scoreBonus += pts
			}
		}

		engine.field.let {
			garbageTimer -= (it.howManyBlocks+it.howManyBlocksCovered+it.howManyHoles+it.howManyLidAboveHoles)/(it.width-1)
			val gh = garbageHeight-(it.height-it.highestGarbageBlockY)
			if(gh>0) if(goalType==GOALTYPE_NORMAL) garbagePending = maxOf(garbagePending, gh)
			else addGarbage(engine, gh, false)
		}
		return pts+cln*100
	}

	override fun afterSoftDropFall(engine:GameEngine, fall:Int) {
		garbageTimer -= fall
	}

	override fun afterHardDropFall(engine:GameEngine, fall:Int) {
		garbageTimer -= fall
	}

	/** Get garbage time limit
	 * @param lv Level
	 * @return Garbage time limit
	 */
	private fun getGarbageMaxTime(lv:Int):Int = GARBAGE_TIMER_TABLE[goalType][minOf(lv, GARBAGE_TIMER_TABLE[goalType].size-1)]

	private fun getGarbageMessRate(lv:Int):Float =
		GARBAGE_MESSINESS_TABLE[goalType][minOf(lv, GARBAGE_MESSINESS_TABLE[goalType].size-1)]/100f
	/** Add garbage line(s)
	 * @param engine GameEngine
	 * @param lines Number of garbage lines to add
	 */
	private fun addGarbage(engine:GameEngine, lines:Int = 1, change:Boolean = true) {
		if(lines<=0) return
		// Add garbages
		val field = engine.field
		val w = field.width
		val h = field.height

		engine.playSE("garbage${if(lines>3) 1 else 0}")

		if(garbageHole<0||engine.random.nextFloat()<getGarbageMessRate(engine.statistics.level)) garbageHole =
			engine.random.nextInt(w)

		for(i in 0..<lines) {
			field.pushUp()

			for(x in 0..<w) if(x!=garbageHole) field.setBlock(
				x, h-1, Block(
					Block.COLOR.WHITE, engine.skin, Block.ATTRIBUTE.VISIBLE, Block.ATTRIBUTE.GARBAGE, Block.ATTRIBUTE.CONNECT_DOWN
				)
			)

			// Set connections
			if(receiver.isStickySkin(engine)) for(x in 0..<w) {
				field.getBlock(x, h-1)?.apply {
					if(!field.getBlockEmpty(x-1, h-1, false)) setAttribute(true, Block.ATTRIBUTE.CONNECT_LEFT)
					if(!field.getBlockEmpty(x+1, h-1, false)) setAttribute(true, Block.ATTRIBUTE.CONNECT_RIGHT)
					if(field.getBlock(x, h-2)?.getAttribute(Block.ATTRIBUTE.GARBAGE)==true)
						setAttribute(true, Block.ATTRIBUTE.CONNECT_UP)
				}
			}
		}

		garbageTotal += lines
		norm += lines
		levelUp(engine)
		if(change) {
			garbageHistory = (garbageHistory.drop(1)+garbageHole).toMutableList()
			do garbageHole = engine.random.nextInt(w)
			while(garbageHistory.any {it==garbageHole}||(garbageHistory.last()-garbageHole) in -2..2)
		}
	}

	private fun levelUp(engine:GameEngine) {
		if(engine.gameActive&&engine.timerActive) {
			// Level up
			var lvupFlag = false
			while(norm>=normMax&&engine.statistics.level<19) {
				normMax += LEVEL_GARBAGE_LINES
				engine.statistics.level++
				lvupFlag = true
			}

			if(lvupFlag) {
				owner.bgMan.nextBg = engine.statistics.level
				setSpeed(engine)
				engine.playSE("levelup")
			}
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
		drawResult(engine, receiver, 4, COLOR.BLUE, "GARBAGE", "%10d".format(garbageDug))
		drawResultStats(engine, receiver, 6, COLOR.BLUE, Statistic.PIECE, Statistic.LEVEL, Statistic.TIME)
		drawResultRank(engine, receiver, 12, COLOR.BLUE, rankingRank)
		drawResultNetRank(engine, receiver, 14, COLOR.BLUE, netRankingRank[0])
		drawResultNetRankDaily(engine, receiver, 16, COLOR.BLUE, netRankingRank[1])

		if(netIsPB) receiver.drawMenuFont(engine, 2, 18, "NEW PB", COLOR.ORANGE)

		if(netIsNetPlay&&netReplaySendStatus==1) receiver.drawMenuFont(engine, 0, 19, "SENDING...", COLOR.PINK)
		else if(netIsNetPlay&&!netIsWatch&&netReplaySendStatus==2)
			receiver.drawMenuFont(engine, 1, 19, "A: RETRY", COLOR.RED)
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
			if(updateRanking(engine.statistics.score, engine.statistics.lines, garbageDug, goalType)!=-1) return true
		}
		return false
	}

	/** Update rankings
	 * @param sc Score
	 * @param li Lines
	 * @param dep Depth
	 */
	private fun updateRanking(sc:Long, li:Int, dep:Int, type:Int):Int {
		rankingRank = checkRanking(sc, li, dep, type)

		if(rankingRank!=-1) {
			// Shift down ranking entries
			for(i in rankingMax-1 downTo rankingRank+1) {
				rankingScore[type][i] = rankingScore[type][i-1]
				rankingLines[type][i] = rankingLines[type][i-1]
				rankingDepth[type][i] = rankingDepth[type][i-1]
			}

			// Add new data
			rankingScore[type][rankingRank] = sc
			rankingLines[type][rankingRank] = li
			rankingDepth[type][rankingRank] = dep
		}
		return rankingRank
	}

	/** Calculate ranking position
	 * @param sc Score
	 * @param li Lines
	 * @param dep Depth
	 * @return Position (-1 if unranked)
	 */
	private fun checkRanking(sc:Long, li:Int, dep:Int, type:Int):Int {
		for(i in 0..<rankingMax) if(sc>rankingScore[type][i]) return i
		else if(sc==rankingScore[type][i]&&li>rankingLines[type][i]) return i
		else if(sc==rankingScore[type][i]&&li==rankingLines[type][i]&&dep>rankingDepth[type][i]) return i

		return -1
	}

	/** NET: Send various in-game stats of [engine] */
	override fun netSendStats(engine:GameEngine) {
		val bg =
			if(engine.owner.bgMan.fadeSW) engine.owner.bgMan.nextBg else engine.owner.bgMan.bg
		val msg = "game\tstats\t"+
			"${engine.statistics.scoreLine}\t${engine.statistics.scoreBonus}\t${engine.statistics.lines}\t"+
			"${engine.statistics.totalPieceLocked}\t${engine.statistics.time}\t${engine.statistics.level}\t"+
			"$garbageTimer\t$garbageTotal\t$garbageDug\t$goalType\t"+
			"${engine.gameActive}\t${engine.timerActive}\t"+
			"$lastScore\t$scDisp\t$bg\t$garbagePending\n"
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
			{garbageTimer = it.toInt()},
			{garbageTotal = it.toInt()},
			{garbageDug = it.toInt()},
			{goalType = it.toInt()},
			{engine.gameActive = it.toBoolean()},
			{engine.timerActive = it.toBoolean()},
			{lastScore = it.toInt()},
			{/*scDisp = it.toInt()*/},
			{engine.owner.bgMan.bg = it.toInt()},
			{garbagePending = it.toInt()}).zip(message).forEach {(x, y) ->
			x(y)
		}

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
				"GARBAGE;$garbageDug\t"+
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
		val msg = "game\toption\t$goalType\t$startLevel\t$bgmId\t${engine.speed.das}\n"
		netLobby?.netPlayerClient?.send(msg)
	}

	/** NET: Receive game options */
	override fun netRecvOptions(engine:GameEngine, message:List<String>) {
		goalType = message[4].toInt()
		startLevel = message[5].toInt()
		bgmId = message[6].toInt()
		engine.speed.das = message[7].toInt()
	}

	/** NET: Get goal type */
	override val netGetGoalType get() = goalType

	/** NET: It returns true when the current settings don't prevent leaderboard screen from showing. */
	override fun netIsNetRankingViewOK(engine:GameEngine):Boolean = startLevel==0&&engine.ai==null

	companion object {
		/** Current version */
		private const val CURRENT_VERSION = 2

		/** Number of goal type */
		private const val GOALTYPE_MAX = 2

		/** Number of garbage lines for each level */
		private const val LEVEL_GARBAGE_LINES = 10

		/** Goal type constants */
		enum class Type { NORMAL, REALTIME }

		private const val GOALTYPE_NORMAL = 0
		private const val GOALTYPE_REALTIME = 1

		private const val GARBAGE_BOTTOM = 4

		private val tableSpeeds = LevelData(
			listOf(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 465, 731, 1280, 1707, -1, -1, -1)/*(numerators)*/,
			listOf(64, 50, 39, 30, 22, 16, 12, 8, 6, 4, 3, 2, 1, 256, 256, 256, 256, 256, 256, 256)/* (denominators) */,
			0, 0, 0, 30, 14
		)

		/** Garbage speed table */
		private val GARBAGE_TIMER_TABLE = listOf(
			listOf(360, 340, 320, 310, 300, 290, 280, 270, 260, 250, 240, 230, 220, 210, 200, 190, 180, 170, 160, 150), // Normal
			listOf(420, 410, 400, 385, 370, 350, 330, 305, 280, 265, 240, 230, 220, 210, 205, 200, 195, 190, 185, 180)// Realtime
		)
		private val GARBAGE_MESSINESS_TABLE = listOf(
			listOf(20, 22, 25, 27, 30, 32, 35, 37, 40, 43, 46, 50, 55, 60, 65, 70, 75, 80, 85, 90), // Normal
			listOf(20, 25, 30, 32, 35, 37, 40, 45, 50, 52, 55, 57, 60, 62, 64, 66, 68, 70, 72, 75), // Realtime
		)
	}
}
