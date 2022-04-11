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
import mu.nu.nullpo.game.component.Block
import mu.nu.nullpo.game.component.Controller
import mu.nu.nullpo.game.event.EventReceiver.COLOR
import mu.nu.nullpo.game.net.NetUtil
import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.util.CustomProperties
import mu.nu.nullpo.util.GeneralUtil.toTimeStr

/** DIG CHALLENGE mode */
class MarathonDrill:NetDummyMode() {

	/** Previous garbage hole */
	private var garbageHole = 0
	private var garbageHistory = IntArray(7) {0}

	/** Garbage timer */
	private var garbageTimer = 0

	/** Garbage Height */
	private var garbageHeight:Int = GARBAGE_BOTTOM

	/** Number of total garbage lines digged */
	private var garbageDigged = 0

	/** Number of total garbage lines rised */
	private var garbageTotal = 0

	/** Number of garbage lines needed for next level */
	private var garbageNextLevelLines = 0

	/** Number of garbage lines waiting to appear (Normal type) */
	private var garbagePending = 0

	/** Game type */
	private var goaltype = 0

	/** Level at the start of the game */
	private var startLevel = 0

	/** BGM number */
	private var bgmno = 0

	/** Version */
	private var version = 0

	/** Current round's ranking rank */
	private var rankingRank = 0

	/** Rankings' scores */
	private var rankingScore:Array<IntArray> = Array(GOALTYPE_MAX) {IntArray(RANKING_MAX)}

	/** Rankings' line counts */
	private var rankingLines:Array<IntArray> = Array(GOALTYPE_MAX) {IntArray(RANKING_MAX)}

	/** Rankings' depth */
	private var rankingDepth:Array<IntArray> = Array(GOALTYPE_MAX) {IntArray(RANKING_MAX)}

	override val rankMap:Map<String, IntArray>
		get() = mapOf(
			*((rankingScore.mapIndexed {a, x -> "$a.stage" to x}+
				rankingLines.mapIndexed {a, x -> "$a.lines" to x}+
				rankingDepth.mapIndexed {a, x -> "$a.deoth" to x}/*+
				rankingTime.mapIndexed {a, x -> "$a.time" to x}*/).toTypedArray())
		)
	/* Mode name */
	override val name = "Drill Marathon"
	override val gameIntensity = 1
	/* Initialization for each player */
	override fun playerInit(engine:GameEngine, playerID:Int) {
		rankingScore = Array(GOALTYPE_MAX) {IntArray(RANKING_MAX)}
		rankingLines = Array(GOALTYPE_MAX) {IntArray(RANKING_MAX)}
		rankingDepth = Array(GOALTYPE_MAX) {IntArray(RANKING_MAX)}
		super.playerInit(engine, playerID)

		lastscore = 0

		garbageHole = -1
		garbageHistory = IntArray(7) {-1}
		garbageTimer = 0
		garbageDigged = 0
		garbageTotal = 0
		garbageNextLevelLines = 0
		garbagePending = 0
		garbageHeight = GARBAGE_BOTTOM

		rankingRank = -1

		engine.framecolor = GameEngine.FRAME_COLOR_GREEN
		engine.statistics.levelDispAdd = 1

		netPlayerInit(engine, playerID)
		// NET: Load name
		if(!owner.replayMode) version = CURRENT_VERSION else netPlayerName = engine.owner.replayProp.getProperty(
			"$playerID.net.netPlayerName", ""
		)

		engine.owner.backgroundStatus.bg = startLevel
	}

	/** Set the gravity rate
	 * @param engine GameEngine
	 */
	private fun setSpeed(engine:GameEngine) {
		engine.speed.apply {
			if(goaltype==GOALTYPE_REALTIME) {
				gravity = 0
				denominator = 60
			} else {
				val lv = maxOf(0, minOf(engine.statistics.level, tableGravity.size-1))
				gravity = tableGravity[lv]
				denominator = tableDenominator[lv]
			}

			are = 0
			areLine = 0
			lineDelay = 0
			lockDelay = 30
		}
	}

	/* Called at settings screen */
	override fun onSetting(engine:GameEngine, playerID:Int):Boolean {
		// NET: Net Ranking
		if(netIsNetRankingDisplayMode)
			netOnUpdateNetPlayRanking(engine, goaltype)
		else if(!engine.owner.replayMode) {
			// Configuration changes
			val change = updateCursor(engine, 4, playerID)

			if(change!=0) {
				engine.playSE("change")

				when(menuCursor) {
					0 -> {
						goaltype += change
						if(goaltype<0) goaltype = GOALTYPE_MAX-1
						if(goaltype>GOALTYPE_MAX-1) goaltype = 0
					}
					1 -> {
						garbageHeight += change
						if(garbageHeight<0) garbageHeight = 10
						if(garbageHeight>10) garbageHeight = 0
					}
					2 -> {
						startLevel += change
						if(startLevel<0) startLevel = 19
						if(startLevel>19) startLevel = 0
						engine.owner.backgroundStatus.bg = startLevel
					}
					3 -> bgmno = rangeCursor(bgmno+change, 0, BGM.count-1)
					4 -> engine.speed.das = rangeCursor(engine.speed.das+change, 0, 99)
				}

				// NET: Signal options change
				if(netIsNetPlay&&netNumSpectators>0) netSendOptions(engine)
			}

			// Confirm
			if(engine.ctrl.isPush(Controller.BUTTON_A)&&menuTime>=5) {
				engine.playSE("decide")

				// Save settings

				// NET: Signal start of the game
				if(netIsNetPlay) netLobby?.netPlayerClient?.send("start1p\n")

				return false
			}

			// Cancel
			if(engine.ctrl.isPush(Controller.BUTTON_B)&&!netIsNetPlay) engine.quitflag = true

			// NET: Netplay Ranking
			if(engine.ctrl.isPush(Controller.BUTTON_D)&&netIsNetPlay
				&&netIsNetRankingViewOK(engine)
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

	/* Render settings screen */
	override fun renderSetting(engine:GameEngine, playerID:Int) {
		receiver.let {
			if(netIsNetRankingDisplayMode)
			// NET: Netplay Ranking
				netOnRenderNetPlayRanking(engine, playerID, it)
			else {
				drawMenu(engine, playerID, it, 0, COLOR.BLUE, 0, "GAME TYPE" to if(goaltype==0) "NORMAL" else "REALTIME")
				drawMenuCompact(engine, playerID, it, "HEIGHT" to garbageHeight, "Level" to startLevel+1)
				drawMenuBGM(engine, playerID, it, bgmno)
				drawMenuCompact(engine, playerID, it, "DAS" to engine.speed.das)
			}
		}
	}

	/* This function will be called before the game actually begins
 * (afterReady&Go screen disappears) */
	override fun startGame(engine:GameEngine, playerID:Int) {
		engine.statistics.level = startLevel
		engine.b2bEnable = true
		engine.splitb2b = true
		engine.comboType = GameEngine.COMBO_TYPE_NORMAL
		engine.twistAllowKick = true
		engine.twistEnable = true
		engine.useAllSpinBonus = true
		engine.twistEnableEZ = true

		garbageTotal = LEVEL_GARBAGE_LINES*startLevel
		garbageNextLevelLines = LEVEL_GARBAGE_LINES*(startLevel+1)

		setSpeed(engine)

		owner.bgmStatus.bgm = if(netIsWatch) BGM.Silent else BGM.values[bgmno]

	}

	override fun onReady(engine:GameEngine, playerID:Int):Boolean {
		if(engine.statc[0]<=1) {
			engine.goStart = maxOf(50, 40+garbageHeight*5)
			engine.readyEnd = engine.goStart-1
			engine.goEnd = engine.goStart+50
		}
		if(garbageHeight>0&&engine.statc[0] in 30 until 30+garbageHeight*5&&engine.statc[0]%5==0)
			addGarbage(engine)
		return false
	}

	/* Render score */
	override fun renderLast(engine:GameEngine, playerID:Int) {
		super.renderLast(engine, playerID)
		if(owner.menuOnly) return

		receiver.drawScoreFont(engine, playerID, 0, 0, name, color = COLOR.GREEN)
		receiver.drawScoreFont(engine, playerID, 0, 1, if(goaltype==0) "(NORMAL RUN)" else "(REALTIME RUN)", COLOR.GREEN)

		if(engine.stat==GameEngine.Status.SETTING||engine.stat==GameEngine.Status.RESULT&&!owner.replayMode) {
			if(!owner.replayMode&&startLevel==0) {
				val scale = if(receiver.nextDisplayType==2) .5f else 1f
				val topY = if(receiver.nextDisplayType==2) 6 else 4
				receiver.drawScoreFont(engine, playerID, 3, topY-1, "SCORE  LINE DEPTH", COLOR.BLUE, scale)

				for(i in 0 until RANKING_MAX) {
					receiver.drawScoreGrade(
						engine, playerID, 0, topY+i,
						String.format("%2d", i+1), COLOR.YELLOW, scale
					)
					receiver.drawScoreNum(
						engine, playerID, 15, topY+i,
						"${rankingDepth[goaltype][i]}", i==rankingRank, scale
					)
					receiver.drawScoreNum(
						engine, playerID, 3, topY+i,
						"${rankingScore[goaltype][i]}", i==rankingRank, scale
					)
					receiver.drawScoreNum(
						engine, playerID, 10, topY+i,
						"${rankingLines[goaltype][i]}", i==rankingRank, scale
					)
				}
			}
		} else {
			receiver.drawScoreFont(engine, playerID, 0, 3, "Score", COLOR.BLUE)
			receiver.drawScoreNum(engine, playerID, 0, 4, "${engine.statistics.score}", scale = 2f)
			receiver.drawScoreNum(engine, playerID, 5, 3, "+$lastscore")

			receiver.drawScoreFont(engine, playerID, 0, 6, "DEPTH", COLOR.BLUE)
			receiver.drawScoreNum(engine, playerID, 0, 7, "$garbageDigged", scale = 2f)

			receiver.drawScoreFont(engine, playerID, 0, 9, "LINE", COLOR.BLUE)
			receiver.drawScoreNum(engine, playerID, 0, 10, "${engine.statistics.lines}", scale = 2f)

			receiver.drawScoreFont(engine, playerID, 0, 12, "Level", COLOR.BLUE)
			receiver.drawScoreNum(engine, playerID, 5, 12, "${engine.statistics.level+1}", scale = 2f)
			receiver.drawScoreNum(engine, playerID, 1, 13, "$garbageTotal")
			receiver.drawSpeedMeter(
				engine, playerID, 0, 14,
				garbageTotal%LEVEL_GARBAGE_LINES*1f/(LEVEL_GARBAGE_LINES-1),
				2f
			)
			receiver.drawScoreNum(engine, playerID, 1, 15, "$garbageNextLevelLines")

			receiver.drawScoreFont(engine, playerID, 0, 16, "Time", COLOR.BLUE)
			receiver.drawScoreNum(engine, playerID, 0, 17, engine.statistics.time.toTimeStr, scale = 2f)

			if(garbagePending>0) {
				val fontColor = when {
					garbagePending>=1 -> COLOR.YELLOW
					garbagePending>=3 -> COLOR.ORANGE
					garbagePending>=4 -> COLOR.RED
					else -> COLOR.WHITE
				}
				val strTempGarbage = String.format("%2d", garbagePending)
				receiver.drawMenuNum(engine, playerID, 10, 20, strTempGarbage, fontColor)
			}
			receiver.drawMenuFont(engine, playerID, garbageHole, 20, "\u008b")
		}

	}

	/* Called after every frame */
	override fun onLast(engine:GameEngine, playerID:Int) {
		super.onLast(engine, playerID)

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
					if(goaltype==GOALTYPE_REALTIME&&garbagePending>0&&engine.stat!=GameEngine.Status.LINECLEAR) {

						// Add Garbage (Realtime)
						garbageTimer %= maxTime

						addGarbage(engine, garbagePending)
						garbagePending = 0

						// NET: Send field and stats
						if(netIsNetPlay&&!netIsWatch&&netNumSpectators>0)
							netSendField(engine)


						if(engine.stat==GameEngine.Status.MOVE) engine.nowPieceObject?.let {nowPieceObject ->
							if(nowPieceObject.checkCollision(engine.nowPieceX, engine.nowPieceY, it)) {
								// Push up the current piece
								while(nowPieceObject.checkCollision(engine.nowPieceX, engine.nowPieceY, it))
									engine.nowPieceY--

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
		engine.meterValue = if(limitTime>0)
			remainTime*receiver.getMeterMax(engine)/limitTime else 0
		engine.meterColor = GameEngine.METER_COLOR_LIMIT
	}

	/* Calculate score */
	override fun calcScore(engine:GameEngine, playerID:Int, lines:Int):Int {
		garbageTimer -= getGarbageMaxTime(engine.statistics.level)/50
		// Line clear bonus
		val pts = calcPoint(engine, lines)
		val cln = engine.garbageClearing
		if(lines>0) {
			garbageDigged += cln
			// Combo
			val cmb = if(engine.combo>=1) engine.combo-1 else 0
			// Add to score
			var get = calcScoreCombo(pts, cmb, 0, 0)

			get += cln*100
			// Decrease waiting garbage
			garbageTimer -= maxOf(0, 60-engine.statistics.level*2-cmb*7)+calcPower(engine, lines)*20
			if(goaltype==GOALTYPE_NORMAL)
				while(garbagePending>0&&garbageTimer<0) {
					garbageTimer += getGarbageMaxTime(engine.statistics.level)
					garbagePending--
				}

			lastscore = get
			engine.statistics.scoreLine += get
		} else {
			if(goaltype==GOALTYPE_NORMAL&&garbagePending>0) {
				addGarbage(engine, garbagePending)
				garbagePending = 0
			}
			if(pts>0) {
				lastscore = pts
				engine.statistics.scoreBonus += pts
			}
		}

		engine.field.let {
			garbageTimer -= (it.howManyBlocks+it.howManyBlocksCovered+it.howManyHoles+it.howManyLidAboveHoles)/(it.width-1)
			val gh = garbageHeight-(it.height-it.highestGarbageBlockY)
			if(gh>0) if(goaltype==GOALTYPE_NORMAL) garbagePending = maxOf(garbagePending, gh)
			else addGarbage(engine, gh, false)

		}
		return pts+cln*100
	}

	override fun afterSoftDropFall(engine:GameEngine, playerID:Int, fall:Int) {
		super.afterSoftDropFall(engine, playerID, fall)
		garbageTimer -= fall
	}

	override fun afterHardDropFall(engine:GameEngine, playerID:Int, fall:Int) {
		super.afterHardDropFall(engine, playerID, fall)
		garbageTimer -= fall
	}

	/** Get garbage time limit
	 * @param lv Level
	 * @return Garbage time limit
	 */
	private fun getGarbageMaxTime(lv:Int):Int =
		GARBAGE_TIMER_TABLE[goaltype][minOf(lv, GARBAGE_TIMER_TABLE[goaltype].size-1)]

	private fun getGarbageMessRate(lv:Int):Float =
		GARBAGE_MESSINESS_TABLE[goaltype][minOf(lv, GARBAGE_MESSINESS_TABLE[goaltype].size-1)]/100f
	/** Add garbage line(s)
	 * @param engine GameEngine
	 * @param lines Number of garbage lines to add
	 */
	private fun addGarbage(engine:GameEngine, lines:Int = 1, change:Boolean = true) {
		// Add garbages
		val field = engine.field
		val w = field.width
		val h = field.height

		engine.playSE("garbage${if(lines>3) 1 else 0}")

		if(garbageHole<0||engine.random.nextFloat()<getGarbageMessRate(engine.statistics.level))
			garbageHole = engine.random.nextInt(w)

		for(i in 0 until lines) {
			field.pushUp()

			for(x in 0 until w)
				if(x!=garbageHole)
					field.setBlock(
						x, h-1, Block(
							Block.COLOR.WHITE, engine.skin, Block.ATTRIBUTE.VISIBLE, Block.ATTRIBUTE.GARBAGE,
							Block.ATTRIBUTE.CONNECT_DOWN
						)
					)

			// Set connections
			if(receiver.isStickySkin(engine))
				for(x in 0 until w) {
					field.getBlock(x, h-1)?.apply {
						if(!field.getBlockEmpty(x-1, h-1, false)) setAttribute(true, Block.ATTRIBUTE.CONNECT_LEFT)
						if(!field.getBlockEmpty(x+1, h-1, false)) setAttribute(true, Block.ATTRIBUTE.CONNECT_RIGHT)
						if(field.getBlock(x, h-2)
								?.getAttribute(Block.ATTRIBUTE.GARBAGE)==true
						) setAttribute(true, Block.ATTRIBUTE.CONNECT_UP)
					}
				}
		}

		if(engine.gameActive&&engine.timerActive) {
			// Levelup
			var lvupflag = false
			garbageTotal += lines

			while(garbageTotal>=garbageNextLevelLines&&engine.statistics.level<19) {
				garbageNextLevelLines += LEVEL_GARBAGE_LINES
				engine.statistics.level++
				lvupflag = true
			}

			if(lvupflag) {
				owner.backgroundStatus.fadesw = true
				owner.backgroundStatus.fadecount = 0
				owner.backgroundStatus.fadebg = engine.statistics.level
				setSpeed(engine)
				engine.playSE("levelup")
			}
		}

		if(change) {
			garbageHistory = (garbageHistory.drop(1)+garbageHole).toIntArray()
			do garbageHole = engine.random.nextInt(w)
			while(garbageHistory.any {it==garbageHole}||(garbageHistory.last()-garbageHole) in -2..2)
		}
	}

	override fun onResult(engine:GameEngine, playerID:Int):Boolean {
		owner.bgmStatus.fadesw = false
		owner.bgmStatus.bgm = if(engine.statistics.time<10800) BGM.Result(1) else BGM.Result(2)

		return super.onResult(engine, playerID)
	}

	/* Results screen */
	override fun renderResult(engine:GameEngine, playerID:Int) {
		drawResultStats(engine, playerID, receiver, 0, COLOR.BLUE, Statistic.SCORE, Statistic.LINES)
		drawResult(engine, playerID, receiver, 4, COLOR.BLUE, "GARBAGE", String.format("%10d", garbageDigged))
		drawResultStats(engine, playerID, receiver, 6, COLOR.BLUE, Statistic.PIECE, Statistic.LEVEL, Statistic.TIME)
		drawResultRank(engine, playerID, receiver, 12, COLOR.BLUE, rankingRank)
		drawResultNetRank(engine, playerID, receiver, 14, COLOR.BLUE, netRankingRank[0])
		drawResultNetRankDaily(engine, playerID, receiver, 16, COLOR.BLUE, netRankingRank[1])

		if(netIsPB) receiver.drawMenuFont(engine, playerID, 2, 18, "NEW PB", COLOR.ORANGE)

		if(netIsNetPlay&&netReplaySendStatus==1)
			receiver.drawMenuFont(engine, playerID, 0, 19, "SENDING...", COLOR.PINK)
		else if(netIsNetPlay&&!netIsWatch
			&&netReplaySendStatus==2
		)
			receiver.drawMenuFont(engine, playerID, 1, 19, "A: RETRY", COLOR.RED)
	}

	/* Called when saving replay */
	override fun saveReplay(engine:GameEngine, playerID:Int, prop:CustomProperties):Boolean {
		saveSetting(prop, engine)

		// NET: Save name
		if(netPlayerName!=null&&netPlayerName!!.isNotEmpty()) prop.setProperty("$playerID.net.netPlayerName", netPlayerName)

		// Update rankings
		if(!owner.replayMode&&startLevel==0&&engine.ai==null) {
			//owner.statsProp.setProperty("decoration", decoration)
			if(updateRanking(engine.statistics.score, engine.statistics.lines, garbageDigged, goaltype)!=-1) return true
		}
		return false
	}

	override fun loadSetting(prop:CustomProperties, ruleName:String, playerID:Int) {
		goaltype = prop.getProperty("digchallenge.goaltype", GOALTYPE_NORMAL)
		startLevel = prop.getProperty("digchallenge.startLevel", 0)
		bgmno = prop.getProperty("digchallenge.bgmno", 0)
		owner.engine[0].speed.das = prop.getProperty("digchallenge.das", 11)
		version = prop.getProperty("digchallenge.version", 0)
	}

	override fun saveSetting(prop:CustomProperties, ruleName:String, playerID:Int) {
		prop.setProperty("digchallenge.goaltype", goaltype)
		prop.setProperty("digchallenge.startLevel", startLevel)
		prop.setProperty("digchallenge.bgmno", bgmno)
		prop.setProperty("digchallenge.das", owner.engine[0].speed.das)
		prop.setProperty("digchallenge.version", version)
	}

	override fun loadRanking(prop:CustomProperties, ruleName:String) {
		for(i in 0 until RANKING_MAX)
			for(j in 0 until GOALTYPE_MAX) {
				rankingScore[j][i] = prop.getProperty("$ruleName.$j.$i.score", 0)
				rankingLines[j][i] = prop.getProperty("$ruleName.$j.$i.lines", 0)
				rankingDepth[j][i] = prop.getProperty("$ruleName.$j.$i.depth", 0)
			}
	}

	/** Save rankings to [prop]
	 * @param type Goal Type
	 * @param ruleName Rule name
	 */
	private fun saveRanking(type:Int, ruleName:String) {
		super.saveRanking((0 until RANKING_MAX).flatMap {i ->
			listOf(
				"$ruleName.$type.$i.score" to rankingScore[type][i],
				"$ruleName.$type.$i.lines" to rankingLines[type][i],
				"$ruleName.$type.$i.depth" to rankingDepth[type][i]
			)
		}.toMap())
	}

	/** Update rankings
	 * @param sc Score
	 * @param li Lines
	 * @param dep Depth
	 */
	private fun updateRanking(sc:Int, li:Int, dep:Int, type:Int):Int {
		rankingRank = checkRanking(sc, li, dep, type)

		if(rankingRank!=-1) {
			// Shift down ranking entries
			for(i in RANKING_MAX-1 downTo rankingRank+1) {
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
	private fun checkRanking(sc:Int, li:Int, dep:Int, type:Int):Int {
		for(i in 0 until RANKING_MAX)
			if(sc>rankingScore[type][i]) return i
			else if(sc==rankingScore[type][i]&&li>rankingLines[type][i]) return i
			else if(sc==rankingScore[type][i]&&li==rankingLines[type][i]&&dep>rankingDepth[type][i]) return i

		return -1
	}

	/** NET: Send various in-game stats of [engine] */
	override fun netSendStats(engine:GameEngine) {
		val bg =
			if(engine.owner.backgroundStatus.fadesw) engine.owner.backgroundStatus.fadebg else engine.owner.backgroundStatus.bg
		var msg = "game\tstats\t"
		msg += "${engine.statistics.scoreLine}\t${engine.statistics.scoreBonus}\t${engine.statistics.lines}\t"
		msg += "${engine.statistics.totalPieceLocked}\t${engine.statistics.time}\t${engine.statistics.level}\t"
		msg += "$garbageTimer\t$garbageTotal\t$garbageDigged\t$goaltype\t"
		msg += "${engine.gameActive}\t${engine.timerActive}\t"
		msg += "$lastscore\t$scDisp\t$bg\t$garbagePending\n"
		netLobby!!.netPlayerClient!!.send(msg)
	}

	/** NET: Parse Received [message] as in-game stats of [engine] */
	override fun netRecvStats(engine:GameEngine, message:Array<String>) {
		listOf<(String)->Unit>({}, {}, {}, {},
			{engine.statistics.scoreLine = it.toInt()},
			{engine.statistics.scoreBonus = it.toInt()},
			{engine.statistics.lines = it.toInt()},
			{engine.statistics.totalPieceLocked = it.toInt()},
			{engine.statistics.time = it.toInt()},
			{engine.statistics.level = it.toInt()},
			{garbageTimer = it.toInt()},
			{garbageTotal = it.toInt()},
			{garbageDigged = it.toInt()},
			{goaltype = it.toInt()},
			{engine.gameActive = it.toBoolean()},
			{engine.timerActive = it.toBoolean()},
			{lastscore = it.toInt()},
			{/*scDisp = it.toInt()*/},
			{engine.owner.backgroundStatus.bg = it.toInt()},
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
		var subMsg = ""
		subMsg += "SCORE;${engine.statistics.score}\t"
		subMsg += "LINE;${engine.statistics.lines}\t"
		subMsg += "GARBAGE;$garbageDigged\t"
		subMsg += "PIECE;${engine.statistics.totalPieceLocked}\t"
		subMsg += "LEVEL;${engine.statistics.level+engine.statistics.levelDispAdd}\t"
		subMsg += "TIME;${engine.statistics.time.toTimeStr}\t"

		val msg = "gstat1p\t${NetUtil.urlEncode(subMsg)}\n"
		netLobby!!.netPlayerClient!!.send(msg)
	}

	/** NET: Send game options to all spectators
	 * @param engine GameEngine
	 */
	override fun netSendOptions(engine:GameEngine) {
		var msg = "game\toption\t"
		msg += "$goaltype\t$startLevel\t$bgmno\t${engine.speed.das}\n"
		netLobby!!.netPlayerClient!!.send(msg)
	}

	/** NET: Receive game options */
	override fun netRecvOptions(engine:GameEngine, message:Array<String>) {
		goaltype = message[4].toInt()
		startLevel = message[5].toInt()
		bgmno = message[6].toInt()
		engine.speed.das = message[7].toInt()
	}

	/** NET: Get goal type */
	override fun netGetGoalType():Int = goaltype

	/** NET: It returns true when the current settings doesn't prevent
	 * leaderboard screen from showing. */
	override fun netIsNetRankingViewOK(engine:GameEngine):Boolean = startLevel==0&&engine.ai==null

	companion object {
		/** Current version */
		private const val CURRENT_VERSION = 2

		/** Number of goal type */
		private const val GOALTYPE_MAX = 2

		/** Number of entries in rankings */
		private const val RANKING_MAX = 13

		/** Number of garbage lines for each level */
		private const val LEVEL_GARBAGE_LINES = 10

		/** Goal type constants */
		enum class Type { NORMAL, REALTIME }

		private const val GOALTYPE_NORMAL = 0
		private const val GOALTYPE_REALTIME = 1

		private const val GARBAGE_BOTTOM = 4

		/** Fall velocity table (numerators) */
		private val tableGravity = intArrayOf(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 465, 731, 1280, 1707, -1, -1, -1)

		/** Fall velocity table (denominators) */
		private val tableDenominator =
			intArrayOf(64, 50, 39, 30, 22, 16, 12, 8, 6, 4, 3, 2, 1, 256, 256, 256, 256, 256, 256, 256)

		/** Garbage speed table */
		private val GARBAGE_TIMER_TABLE = arrayOf(
			intArrayOf(360, 340, 320, 310, 300, 290, 280, 270, 260, 250, 240, 230, 220, 210, 200, 190, 180, 170, 160, 150), // Normal
			intArrayOf(420, 410, 400, 385, 370, 350, 330, 305, 280, 265, 240, 230, 220, 210, 205, 200, 195, 190, 185, 180)// Realtime
		)
		private val GARBAGE_MESSINESS_TABLE = arrayOf(
			intArrayOf(20, 22, 25, 27, 30, 32, 35, 37, 40, 43, 46, 50, 55, 60, 65, 70, 75, 80, 85, 90), // Normal
			intArrayOf(20, 25, 30, 32, 35, 37, 40, 45, 50, 52, 55, 57, 60, 62, 64, 66, 68, 70, 72, 75), // Realtime
		)
	}
}
