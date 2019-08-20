package mu.nu.nullpo.game.subsystem.mode

import mu.nu.nullpo.game.component.BGMStatus.BGM
import mu.nu.nullpo.game.component.Block
import mu.nu.nullpo.game.component.Controller
import mu.nu.nullpo.game.event.EventReceiver.COLOR
import mu.nu.nullpo.game.net.NetUtil
import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.util.CustomProperties
import mu.nu.nullpo.util.GeneralUtil

/** DIG CHALLENGE mode */
class MarathonDrill:NetDummyMode() {

	/** Most recent increase in score */
	private var lastscore:Int = 0

	/** Time to display the most recent increase in score */
	private var scgettime:Int = 0
	private var sum:Int = 0

	/** Previous garbage hole */
	private var garbageHole:Int = 0

	/** Garbage timer */
	private var garbageTimer:Int = 0

	/** Number of total garbage lines digged */
	private var garbageDigged:Int = 0
	/** Number of total garbage lines rised */
	private var garbageTotal:Int = 0

	/** Number of garbage lines needed for next level */
	private var garbageNextLevelLines:Int = 0

	/** Number of garbage lines waiting to appear (Normal type) */
	private var garbagePending:Int = 0

	/** Game type */
	private var goaltype:Int = 0

	/** Level at the start of the game */
	private var startlevel:Int = 0

	/** BGM number */
	private var bgmno:Int = 0

	/** Flag for types of T-Spins allowed (0=none, 1=normal, 2=all spin) */
	private var tspinEnableType:Int = 0

	/** Flag for enabling wallkick T-Spins */
	private var enableTSpinKick:Boolean = false

	/** Spin check type (4Point or Immobile) */
	private var spinCheckType:Int = 0

	/** Immobile EZ spin */
	private var tspinEnableEZ:Boolean = false

	/** Flag for enabling B2B */
	private var enableB2B:Boolean = false

	/** Flag for enabling combos */
	private var enableCombo:Boolean = false

	/** Version */
	private var version:Int = 0

	/** Current round's ranking rank */
	private var rankingRank:Int = 0

	/** Rankings' scores */
	private var rankingScore:Array<IntArray> = Array(GOALTYPE_MAX) {IntArray(RANKING_MAX)}

	/** Rankings' line counts */
	private var rankingLines:Array<IntArray> = Array(GOALTYPE_MAX) {IntArray(RANKING_MAX)}

	/** Rankings' depth */
	private var rankingDepth:Array<IntArray> = Array(GOALTYPE_MAX) {IntArray(RANKING_MAX)}

	/* Mode name */
	override val name:String
		get() = "DIG CHALLENGE"

	/* Initialization for each player */
	override fun playerInit(engine:GameEngine, playerID:Int) {
		rankingScore = Array(GOALTYPE_MAX) {IntArray(RANKING_MAX)}
		rankingLines = Array(GOALTYPE_MAX) {IntArray(RANKING_MAX)}
		rankingDepth = Array(GOALTYPE_MAX) {IntArray(RANKING_MAX)}
		super.playerInit(engine, playerID)

		lastscore = 0
		scgettime = 0

		garbageHole = -1
		garbageTimer = 0
		garbageDigged = 0
		garbageTotal = 0
		garbageNextLevelLines = 0
		garbagePending = 0

		rankingRank = -1

		engine.framecolor = GameEngine.FRAME_COLOR_GREEN
		engine.statistics.levelDispAdd = 1

		netPlayerInit(engine, playerID)

		if(!owner.replayMode) {
			loadSetting(owner.modeConfig)
			loadRanking(owner.recordProp, engine.ruleopt.strRuleName)
			version = CURRENT_VERSION
		} else {
			loadSetting(owner.replayProp)

			// NET: Load name
			netPlayerName = engine.owner.replayProp.getProperty("$playerID.net.netPlayerName", "")
		}

		engine.owner.backgroundStatus.bg = startlevel
	}

	/** Set the gravity rate
	 * @param engine GameEngine
	 */
	private fun setSpeed(engine:GameEngine) {
		if(goaltype==GOALTYPE_REALTIME) {
			engine.speed.gravity = 0
			engine.speed.denominator = 60
		} else {
			var lv = engine.statistics.level

			if(lv<0) lv = 0
			if(lv>=tableGravity.size) lv = tableGravity.size-1

			engine.speed.gravity = tableGravity[lv]
			engine.speed.denominator = tableDenominator[lv]
		}

		engine.speed.are = 0
		engine.speed.areLine = 0
		engine.speed.lineDelay = 0
		engine.speed.lockDelay = 30
	}

	/* Called at settings screen */
	override fun onSetting(engine:GameEngine, playerID:Int):Boolean {
		// NET: Net Ranking
		if(netIsNetRankingDisplayMode)
			netOnUpdateNetPlayRanking(engine, goaltype)
		else if(!engine.owner.replayMode) {
			// Configuration changes
			val change = updateCursor(engine, 9, playerID)

			if(change!=0) {
				engine.playSE("change")

				when(menuCursor) {
					0 -> {
						goaltype += change
						if(goaltype<0) goaltype = GOALTYPE_MAX-1
						if(goaltype>GOALTYPE_MAX-1) goaltype = 0
					}
					1 -> {
						startlevel += change
						if(startlevel<0) startlevel = 19
						if(startlevel>19) startlevel = 0
						engine.owner.backgroundStatus.bg = startlevel
					}
					2 -> {
						bgmno += change
						if(bgmno<-1) bgmno = BGM.count-1
						if(bgmno>=BGM.count) bgmno = 0
					}
					3 -> {
						tspinEnableType += change
						if(tspinEnableType<0) tspinEnableType = 2
						if(tspinEnableType>2) tspinEnableType = 0
					}
					4 -> enableTSpinKick = !enableTSpinKick
					5 -> {
						spinCheckType += change
						if(spinCheckType<0) spinCheckType = 1
						if(spinCheckType>1) spinCheckType = 0
					}
					6 -> tspinEnableEZ = !tspinEnableEZ
					7 -> enableB2B = !enableB2B
					8 -> enableCombo = !enableCombo
					9 -> {
						engine.speed.das += change
						if(engine.speed.das<0) engine.speed.das = 99
						if(engine.speed.das>99) engine.speed.das = 0
					}
				}

				// NET: Signal options change
				if(netIsNetPlay&&netNumSpectators>0) netSendOptions(engine)
			}

			// Confirm
			if(engine.ctrl!!.isPush(Controller.BUTTON_A)&&menuTime>=5) {
				engine.playSE("decide")

				// Save settings
				saveSetting(owner.modeConfig)
				owner.saveModeConfig()

				// NET: Signal start of the game
				if(netIsNetPlay) netLobby!!.netPlayerClient!!.send("start1p\n")

				return false
			}

			// Cancel
			if(engine.ctrl!!.isPush(Controller.BUTTON_B)&&!netIsNetPlay) engine.quitflag = true

			// NET: Netplay Ranking
			if(engine.ctrl!!.isPush(Controller.BUTTON_D)&&netIsNetPlay
				&&netIsNetRankingViewOK(engine))
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
				drawMenu(engine, playerID, it, 0, COLOR.BLUE, 0,
					"GAME TYPE", if(goaltype==0) "NORMAL" else "REALTIME", "LEVEL", "${startlevel+1}")
				drawMenuBGM(engine, playerID, it, bgmno)
				drawMenu(engine, playerID, it, "SPIN BONUS", if(tspinEnableType==0) "OFF" else if(tspinEnableType==1) "T-ONLY" else "ALL",
					"EZ SPIN", GeneralUtil.getONorOFF(enableTSpinKick),
					"SPIN TYPE", if(spinCheckType==0) "4POINT" else "IMMOBILE", "EZIMMOBILE", GeneralUtil.getONorOFF(tspinEnableEZ))
				drawMenuCompact(engine, playerID, it,
					"B2B", GeneralUtil.getONorOFF(enableB2B), "COMBO", GeneralUtil.getONorOFF(enableCombo), "DAS", "${engine.speed.das}")
			}
		}
	}

	/* This function will be called before the game actually begins
 * (afterReady&Go screen disappears) */
	override fun startGame(engine:GameEngine, playerID:Int) {
		engine.statistics.level = startlevel
		engine.b2bEnable = enableB2B
		engine.comboType = if(enableCombo) GameEngine.COMBO_TYPE_NORMAL
		else GameEngine.COMBO_TYPE_DISABLE

		engine.tspinAllowKick = enableTSpinKick
		when(tspinEnableType) {
			0 -> engine.tspinEnable = false
			1 -> engine.tspinEnable = true
			else -> {
				engine.tspinEnable = true
				engine.useAllSpinBonus = true
			}
		}

		engine.spinCheckType = spinCheckType
		engine.tspinEnableEZ = tspinEnableEZ

		garbageTotal = LEVEL_GARBAGE_LINES*startlevel
		garbageNextLevelLines = LEVEL_GARBAGE_LINES*(startlevel+1)

		setSpeed(engine)

		owner.bgmStatus.bgm = if(netIsWatch) BGM.SILENT else BGM.values[bgmno]

		(0 until GARBAGE_BOTTOM).forEach {addGarbage(engine)}
	}

	/* Render score */
	override fun renderLast(engine:GameEngine, playerID:Int) {
		super.renderLast(engine, playerID)
		if(owner.menuOnly) return

		receiver.drawScoreFont(engine, playerID, 0, 0, "DRILL MARATHON", color = COLOR.GREEN)
		receiver.drawScoreFont(engine, playerID, 0, 1, if(goaltype==0) "(NORMAL RUN)" else "(REALTIME RUN)", COLOR.GREEN)

		if(engine.stat==GameEngine.Status.SETTING||engine.stat==GameEngine.Status.RESULT&&!owner.replayMode) {
			if(!owner.replayMode&&startlevel==0) {
				val scale = if(receiver.nextDisplayType==2) .5f else 1f
				val topY = if(receiver.nextDisplayType==2) 6 else 4
				receiver.drawScoreFont(engine, playerID, 3, topY-1, "SCORE  LINE DEPTH", COLOR.BLUE, scale)

				for(i in 0 until RANKING_MAX) {
					receiver.drawScoreGrade(engine, playerID, 0, topY+i,
						String.format("%2d", i+1), COLOR.YELLOW, scale)
					receiver.drawScoreNum(engine, playerID, 15, topY+i,
						"${rankingDepth[goaltype][i]}", i==rankingRank, scale)
					receiver.drawScoreNum(engine, playerID, 3, topY+i,
						"${rankingScore[goaltype][i]}", i==rankingRank, scale)
					receiver.drawScoreNum(engine, playerID, 10, topY+i,
						"${rankingLines[goaltype][i]}", i==rankingRank, scale)
				}
			}
		} else {
			receiver.drawScoreFont(engine, playerID, 0, 3, "SCORE", COLOR.BLUE)
			receiver.drawScoreNum(engine, playerID, 0, 4, "${engine.statistics.score}", scale = 2f)
			receiver.drawScoreNum(engine, playerID, 5, 3, "+$lastscore")

			receiver.drawScoreFont(engine, playerID, 0, 6, "DEPTH", COLOR.BLUE)
			receiver.drawScoreNum(engine, playerID, 0, 7, "$garbageDigged", scale = 2f)

			receiver.drawScoreFont(engine, playerID, 0, 9, "LINE", COLOR.BLUE)
			receiver.drawScoreNum(engine, playerID, 0, 10, "${engine.statistics.lines}", scale = 2f)

			receiver.drawScoreFont(engine, playerID, 0, 12, "LEVEL", COLOR.BLUE)
			receiver.drawScoreNum(engine, playerID, 5, 12, "${engine.statistics.level+1}", scale = 2f)
			receiver.drawScoreNum(engine, playerID, 1, 13, "$garbageTotal")
			receiver.drawSpeedMeter(engine, playerID, 0, 14,
				garbageTotal%LEVEL_GARBAGE_LINES*1f/(LEVEL_GARBAGE_LINES-1))
			receiver.drawScoreNum(engine, playerID, 1, 15, "$garbageNextLevelLines")

			receiver.drawScoreFont(engine, playerID, 0, 16, "TIME", COLOR.BLUE)
			receiver.drawScoreNum(engine, playerID, 0, 17, GeneralUtil.getTime(engine.statistics.time), scale = 2f)

			renderLineAlert(engine, playerID, receiver)

			if(garbagePending>0) {
				val x = receiver.fieldX(engine, playerID)
				val y = receiver.fieldY(engine, playerID)
				val fontColor = when {
					garbagePending>=1 -> COLOR.YELLOW
					garbagePending>=3 -> COLOR.ORANGE
					garbagePending>=4 -> COLOR.RED
					else -> COLOR.WHITE
				}
				val strTempGarbage = String.format("%5d", garbagePending)
				receiver.drawDirectNum(x+96, y+372, strTempGarbage, fontColor)
			}
		}

	}

	/* Called after every frame */
	override fun onLast(engine:GameEngine, playerID:Int) {

		if(engine.gameActive&&engine.timerActive) {
			garbageTimer++
			// Update meter
			updateMeter(engine)
			engine.field?.let {
				// Add pending garbage (Normal)
				if(garbageTimer>=getGarbageMaxTime(engine.statistics.level)&&goaltype==GOALTYPE_NORMAL
					&&!netIsWatch)
					if(version>=1) {
						garbagePending++
						garbageTimer = 0

						// NET: Send stats
						if(netIsNetPlay&&!netIsWatch&&netNumSpectators>0) netSendStats(engine)
					} else
						garbagePending = 1

				// Add Garbage (Realtime)
				if(garbageTimer>=getGarbageMaxTime(engine.statistics.level)&&goaltype==GOALTYPE_REALTIME&&
					engine.stat!=GameEngine.Status.LINECLEAR&&!netIsWatch) {
					addGarbage(engine)
					garbageTimer = 0

					// NET: Send field and stats
					if(netIsNetPlay&&!netIsWatch&&netNumSpectators>0) {
						netSendField(engine)
						netSendStats(engine)
					}

					if(engine.stat==GameEngine.Status.MOVE&&engine.nowPieceObject!=null) {
						if(engine.nowPieceObject!!.checkCollision(engine.nowPieceX, engine.nowPieceY, it)) {
							// Push up the current piece
							while(engine.nowPieceObject!!.checkCollision(engine.nowPieceX, engine.nowPieceY, it))
								engine.nowPieceY--

							// Pushed out from the visible part of the field
							if(!engine.nowPieceObject!!.canPlaceToVisibleField(engine.nowPieceX, engine.nowPieceY, it)) {
								engine.stat = GameEngine.Status.GAMEOVER
								engine.resetStatc()
								engine.gameEnded()
							}
						}

						// Update ghost position
						engine.nowPieceBottomY = engine.nowPieceObject!!.getBottom(engine.nowPieceX, engine.nowPieceY, it)

						// NET: Send piece movement
						if(netIsNetPlay&&!netIsWatch&&netNumSpectators>0) netSendPieceMovement(engine, true)
					}
				}
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
	override fun calcScore(engine:GameEngine, playerID:Int, lines:Int) {
		garbageTimer -= getGarbageMaxTime(engine.statistics.level)/50
		// Line clear bonus
		val pts = calcPoint(engine, lines)
		if(lines>0) {
			var cmb = 0
			val cln = engine.garbageClearing
			garbageDigged += cln
			// Combo
			if(enableCombo&&engine.combo>=1) cmb = engine.combo-1
			// Add to score
			var get = pts
			if(cmb>=1) {
				var b = sum*(1+cmb)/2
				sum += get
				b = sum*(2+cmb)/2-b
				get = b
			} else sum = get

			get += cln*100
			// Decrease waiting garbage
			garbageTimer -= pts*15+cmb*4-cln*5
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

		engine.field?.let {
			garbageTimer -= (it.howManyBlocks+it.howManyBlocksCovered + it.howManyHoles + it.howManyLidAboveHoles)/(it.width-1)
			val gh = GARBAGE_BOTTOM-(it.height-it.highestGarbageBlockY)
			if(gh>0)
				garbagePending = maxOf(garbagePending, gh)
		}
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

	/** Add garbage line(s)
	 * @param engine GameEngine
	 * @param lines Number of garbage lines to add
	 */
	private fun addGarbage(engine:GameEngine, lines:Int = 1) {
		// Add garbages
		val field = engine.field ?: return
		val w = field.width
		val h = field.height

		engine.playSE("garbage")

		val prevHole = garbageHole

		for(i in 0 until lines) {
			do
				garbageHole = engine.random.nextInt(w)
			while(garbageHole==prevHole)

			field.pushUp()

			for(x in 0 until w)
				if(x!=garbageHole)
					field.setBlock(x, h-1, Block(Block.COLOR.WHITE, engine.skin, Block.ATTRIBUTE.VISIBLE, Block.ATTRIBUTE.GARBAGE))

			// Set connections
			if(receiver.isStickySkin(engine))
				for(x in 0 until w)
					if(x!=garbageHole) {
						field.getBlock(x, h-1)?.apply {
							if(!field.getBlockEmpty(x-1, h-1)) setAttribute(true, Block.ATTRIBUTE.CONNECT_LEFT)
							if(!field.getBlockEmpty(x+1, h-1)) setAttribute(true, Block.ATTRIBUTE.CONNECT_RIGHT)
						}
					}
		}

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

	override fun onResult(engine:GameEngine, playerID:Int):Boolean {
		owner.bgmStatus.fadesw = false
		owner.bgmStatus.bgm = if(engine.statistics.time<10800) BGM.RESULT(1) else BGM.RESULT(2)

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
			&&netReplaySendStatus==2)
			receiver.drawMenuFont(engine, playerID, 1, 19, "A: RETRY", COLOR.RED)
	}

	/* Called when saving replay */
	override fun saveReplay(engine:GameEngine, playerID:Int, prop:CustomProperties) {
		saveSetting(prop)

		// NET: Save name
		if(netPlayerName!=null&&netPlayerName!!.isNotEmpty()) prop.setProperty("$playerID.net.netPlayerName", netPlayerName)

		// Update rankings
		if(!owner.replayMode&&startlevel==0&&engine.ai==null) {

			if(updateRanking(engine.statistics.score, engine.statistics.lines, garbageDigged, goaltype)!=-1) {
				saveRanking(goaltype, engine.ruleopt.strRuleName)
				owner.saveModeConfig()
			}
		}
	}

	/** Load settings from property file
	 * @param prop Property file
	 */
	override fun loadSetting(prop:CustomProperties) {
		goaltype = prop.getProperty("digchallenge.goaltype", GOALTYPE_NORMAL)
		startlevel = prop.getProperty("digchallenge.startlevel", 0)
		bgmno = prop.getProperty("digchallenge.bgmno", 0)
		tspinEnableType = prop.getProperty("digchallenge.tspinEnableType", 2)
		enableTSpinKick = prop.getProperty("digchallenge.enableTSpinKick", true)
		spinCheckType = prop.getProperty("digchallenge.spinCheckType", 1)
		tspinEnableEZ = prop.getProperty("digchallenge.tspinEnableEZ", true)
		enableB2B = prop.getProperty("digchallenge.enableB2B", true)
		enableCombo = prop.getProperty("digchallenge.enableCombo", true)
		owner.engine[0].speed.das = prop.getProperty("digchallenge.das", 11)
		version = prop.getProperty("digchallenge.version", 0)
	}

	/** Save settings to property file
	 * @param prop Property file
	 */
	override fun saveSetting(prop:CustomProperties) {
		prop.setProperty("digchallenge.goaltype", goaltype)
		prop.setProperty("digchallenge.startlevel", startlevel)
		prop.setProperty("digchallenge.bgmno", bgmno)
		prop.setProperty("digchallenge.tspinEnableType", tspinEnableType)
		prop.setProperty("digchallenge.spinCheckType", spinCheckType)
		prop.setProperty("digchallenge.tspinEnableEZ", tspinEnableEZ)
		prop.setProperty("digchallenge.enableTSpinKick", enableTSpinKick)
		prop.setProperty("digchallenge.enableB2B", enableB2B)
		prop.setProperty("digchallenge.enableCombo", enableCombo)
		prop.setProperty("digchallenge.das", owner.engine[0].speed.das)
		prop.setProperty("digchallenge.version", version)
	}

	/** Read rankings from property file
	 * @param prop Property file
	 * @param ruleName Rule name
	 */
	override fun loadRanking(prop:CustomProperties, ruleName:String) {
		for(i in 0 until RANKING_MAX)
			for(j in 0 until GOALTYPE_MAX) {
				rankingScore[j][i] = prop.getProperty("$ruleName.$j.$i.score", 0)
				rankingLines[j][i] = prop.getProperty("$ruleName.$j.$i.lines", 0)
				rankingDepth[j][i] = prop.getProperty("$ruleName.$j.$i.depth", 0)
			}
	}

	/** Save rankings to property file
	 * @param type Goal Type
	 * @param ruleName Rule name
	 */
	private fun saveRanking(type:Int, ruleName:String) {
		super.saveRanking(ruleName, (0 until RANKING_MAX).flatMap {i ->
			listOf("$ruleName.$type.$i.score" to rankingScore[type][i],
				"$ruleName.$type.$i.lines" to rankingLines[type][i],
				"$ruleName.$type.$i.depth" to rankingDepth[type][i])
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
			if(sc>rankingScore[type][i])
				return i
			else if(sc==rankingScore[type][i]&&li>rankingLines[type][i])
				return i
			else if(sc==rankingScore[type][i]&&li==rankingLines[type][i]&&dep>rankingDepth[type][i]) return i

		return -1
	}

	/** NET: Send various in-game stats (as well as goaltype)
	 * @param engine GameEngine
	 */
	override fun netSendStats(engine:GameEngine) {
		val bg =
			if(engine.owner.backgroundStatus.fadesw) engine.owner.backgroundStatus.fadebg else engine.owner.backgroundStatus.bg
		var msg = "game\tstats\t"
		msg += "${engine.statistics.scoreLine}\t${engine.statistics.scoreBonus}\t${engine.statistics.lines}\t"
		msg += "${engine.statistics.totalPieceLocked}\t${engine.statistics.time}\t${engine.statistics.level}\t"
		msg += "$garbageTimer\t$garbageTotal\t$garbageDigged\t$goaltype\t"
		msg += "${engine.gameActive}\t${engine.timerActive}\t"
		msg += "$lastscore\t$scgettime\t$bg\t$garbagePending\n"
		netLobby!!.netPlayerClient!!.send(msg)
	}

	/** NET: Receive various in-game stats (as well as goaltype) */
		override fun netRecvStats(engine:GameEngine, message:Array<String>) {
		listOf<(String)->Unit>({},{},{},{},
		{engine.statistics.scoreLine = Integer.parseInt(it)},
		{engine.statistics.scoreBonus = Integer.parseInt(it)},
		{engine.statistics.lines = Integer.parseInt(it)},
		{engine.statistics.totalPieceLocked = Integer.parseInt(it)},
		{engine.statistics.time = Integer.parseInt(it)},
		{engine.statistics.level = Integer.parseInt(it)},
		{garbageTimer = Integer.parseInt(it)},
		{garbageTotal = Integer.parseInt(it)},
		{garbageDigged = Integer.parseInt(it)},
		{goaltype = Integer.parseInt(it)},
		{engine.gameActive = java.lang.Boolean.parseBoolean(it)},
		{engine.timerActive = java.lang.Boolean.parseBoolean(it)},
		{lastscore = Integer.parseInt(it)},
		{scgettime = Integer.parseInt(it)},
		{engine.owner.backgroundStatus.bg = Integer.parseInt(it)},
		{garbagePending = Integer.parseInt(it)}).zip(message).forEach{
			(x,y)->x(y)
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
		subMsg += "TIME;${GeneralUtil.getTime(engine.statistics.time)}\t"

		val msg = "gstat1p\t${NetUtil.urlEncode(subMsg)}\n"
		netLobby!!.netPlayerClient!!.send(msg)
	}

	/** NET: Send game options to all spectators
	 * @param engine GameEngine
	 */
	override fun netSendOptions(engine:GameEngine) {
		var msg = "game\toption\t"
		msg += "$goaltype\t$startlevel\t$bgmno\t"
		msg += "$tspinEnableType\t$enableTSpinKick\t$spinCheckType\t$tspinEnableEZ\t"
		msg += "$enableB2B\t$enableCombo\t${engine.speed.das}\n"
		netLobby!!.netPlayerClient!!.send(msg)
	}

	/** NET: Receive game options */
	override fun netRecvOptions(engine:GameEngine, message:Array<String>) {
		goaltype = Integer.parseInt(message[4])
		startlevel = Integer.parseInt(message[5])
		bgmno = Integer.parseInt(message[6])
		tspinEnableType = Integer.parseInt(message[7])
		enableTSpinKick = java.lang.Boolean.parseBoolean(message[8])
		spinCheckType = Integer.parseInt(message[9])
		tspinEnableEZ = java.lang.Boolean.parseBoolean(message[10])
		enableB2B = java.lang.Boolean.parseBoolean(message[11])
		enableCombo = java.lang.Boolean.parseBoolean(message[12])
		engine.speed.das = Integer.parseInt(message[13])
	}

	/** NET: Get goal type */
	override fun netGetGoalType():Int = goaltype

	/** NET: It returns true when the current settings doesn't prevent
	 * leaderboard screen from showing. */
	override fun netIsNetRankingViewOK(engine:GameEngine):Boolean = startlevel==0&&engine.ai==null

	companion object {
		/** Current version */
		private const val CURRENT_VERSION = 2

		/** Number of goal type */
		private const val GOALTYPE_MAX = 2

		/** Number of entries in rankings */
		private const val RANKING_MAX = 10

		/** Number of garbage lines for each level */
		private const val LEVEL_GARBAGE_LINES = 10

		/** Goal type constants */
		enum class gametime { NORMAL, REALTIME }

		private const val GOALTYPE_NORMAL = 0
		private const val GOALTYPE_REALTIME = 1

		private const val GARBAGE_BOTTOM = 5
		/** Fall velocity table (numerators) */
		private val tableGravity = intArrayOf(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 465, 731, 1280, 1707, -1, -1, -1)
		/** Fall velocity table (denominators) */
		private val tableDenominator =
			intArrayOf(64, 50, 39, 30, 22, 16, 12, 8, 6, 4, 3, 2, 1, 256, 256, 256, 256, 256, 256, 256)
		/** Garbage speed table */
		private val GARBAGE_TIMER_TABLE = arrayOf(
			intArrayOf(255, 250, 245, 240, 235, 230, 225, 220, 215, 210, 205, 200, 190, 180, 170, 165, 160, 150, 140, 120), // Normal
			intArrayOf(300, 290, 280, 270, 260, 250, 240, 230, 220, 210, 200, 190, 180, 175, 170, 165, 160, 150, 140, 125))// Realtime
	}
}
