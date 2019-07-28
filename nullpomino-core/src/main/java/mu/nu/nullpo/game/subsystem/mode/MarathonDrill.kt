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
		rankingScore = Array(GOALTYPE_MAX) {IntArray(RANKING_MAX)}
		rankingLines = Array(GOALTYPE_MAX) {IntArray(RANKING_MAX)}
		rankingDepth = Array(GOALTYPE_MAX) {IntArray(RANKING_MAX)}

		engine.framecolor = GameEngine.FRAME_COLOR_GREEN
		engine.statistics.levelDispAdd = 1

		netPlayerInit(engine, playerID)

		if(!owner.replayMode) {
			loadSetting(owner.modeConfig)
			loadRanking(owner.modeConfig, engine.ruleopt.strRuleName)
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
				receiver.saveModeConfig(owner.modeConfig)

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
					"GAME TYPE", if(goaltype==0) "NORMAL" else "REALTIME", "LEVEL", (startlevel+1).toString())
				drawMenuBGM(engine, playerID, it, bgmno)
				drawMenu(engine, playerID, it, "SPIN BONUS", if(tspinEnableType==0) "OFF" else if(tspinEnableType==1) "T-ONLY" else "ALL",
					"EZ SPIN", GeneralUtil.getONorOFF(enableTSpinKick),
					"SPIN TYPE", if(spinCheckType==0) "4POINT" else "IMMOBILE", "EZIMMOBILE", GeneralUtil.getONorOFF(tspinEnableEZ))
				drawMenuCompact(engine, playerID, it,
					"B2B", GeneralUtil.getONorOFF(enableB2B), "COMBO", GeneralUtil.getONorOFF(enableCombo), "DAS", engine.speed.das.toString())
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
			receiver.drawScoreNum(engine, playerID, 0, 4, engine.statistics.score.toString(), scale = 2f)
			receiver.drawScoreNum(engine, playerID, 5, 3, "+$lastscore")

			receiver.drawScoreFont(engine, playerID, 0, 6, "DEPTH", COLOR.BLUE)
			receiver.drawScoreNum(engine, playerID, 0, 7, "$garbageDigged", scale = 2f)

			receiver.drawScoreFont(engine, playerID, 0, 9, "LINE", COLOR.BLUE)
			receiver.drawScoreNum(engine, playerID, 0, 10, engine.statistics.lines.toString(), scale = 2f)

			receiver.drawScoreFont(engine, playerID, 0, 12, "LEVEL", COLOR.BLUE)
			receiver.drawScoreNum(engine, playerID, 5, 12, (engine.statistics.level+1).toString(), scale = 2f)
			receiver.drawScoreNum(engine, playerID, 1, 13, "$garbageTotal")
			receiver.drawSpeedMeter(engine, playerID, 0, 14,
				garbageTotal%LEVEL_GARBAGE_LINES*1f/(LEVEL_GARBAGE_LINES-1))
			receiver.drawScoreNum(engine, playerID, 1, 15, "$garbageNextLevelLines")

			receiver.drawScoreFont(engine, playerID, 0, 16, "TIME", COLOR.BLUE)
			receiver.drawScoreNum(engine, playerID, 0, 17, GeneralUtil.getTime(engine.statistics.time), scale = 2f)

			renderLineAlert(engine, playerID, receiver)

			if(garbagePending>0) {
				val x = receiver.getFieldDisplayPositionX(engine, playerID)
				val y = receiver.getFieldDisplayPositionY(engine, playerID)
				var fontColor = COLOR.WHITE

				if(garbagePending>=1) fontColor = COLOR.YELLOW
				if(garbagePending>=3) fontColor = COLOR.ORANGE
				if(garbagePending>=4) fontColor = COLOR.RED

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
		if(limitTime>0)
			engine.meterValue = remainTime*receiver.getMeterMax(engine)/limitTime
		else
			engine.meterValue = 0
		engine.meterColor = GameEngine.METER_COLOR_LIMIT
	}

	override fun calcScore(engine:GameEngine, lines:Int):Int {
		menuTime = 0
		var pts = menuTime
		when {
			engine.tspin -> when {
				lines==0&&!engine.tspinez -> pts = if(engine.tspinmini) 0 else 1// T-Spin 0 lines
				engine.tspinez&&lines>0 -> pts = 1+lines+(if(engine.b2b) 1 else 0)// Immobile EZ Spin
				lines==1 -> pts += if(engine.tspinmini) if(engine.b2b) 3 else 2 else if(engine.b2b) 2 else 1// T-Spin 1 line
				lines==2 -> pts += if(engine.tspinmini&&engine.useAllSpinBonus) if(engine.b2b) 5 else 4 else if(engine.b2b) 4 else 3// T-Spin 2 lines
				lines>=3 -> pts += if(engine.b2b) 5 else 4// Twister 3 lines
			}// Twister 3 lines
			lines==1 -> pts = 1 // 1列
			lines==2 -> pts = if(engine.split) if(engine.b2b) 4 else 3 else 2 // 2列
			lines==3 -> pts = if(engine.split) if(engine.b2b) 5 else 4 else 3 // 3列
			lines>=4 -> pts = if(engine.b2b) 6 else 5
			// All clear
		}
		// All clear
		if(lines>=1&&engine.field!!.isEmpty) pts += 10
		return pts
	}

	/* Calculate score */
	override fun calcScore(engine:GameEngine, playerID:Int, lines:Int) {
		garbageTimer -= getGarbageMaxTime(engine.statistics.level)/50
		// Line clear bonus
		val pts = calcScore(engine, lines)
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
			} else
				sum = get

			get += cln*10
			// Decrease waiting garbage
			garbageTimer -= pts*15+cmb*4-cln*5
			if(goaltype==GOALTYPE_NORMAL)
				while(garbagePending>0&&garbageTimer<0) {
					garbageTimer += getGarbageMaxTime(engine.statistics.level)
					garbagePending--
				}

			lastscore = get
			engine.statistics.scoreFromLineClear += get
			engine.statistics.score += get
		} else {
			if(goaltype==GOALTYPE_NORMAL&&garbagePending>0) {
				addGarbage(engine, garbagePending)
				garbagePending = 0
			}
			if(pts>0) {
				lastscore = pts
				engine.statistics.scoreFromOtherBonus += pts
				engine.statistics.score += pts
			}
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
	 * @return Garbage time limi
	 */
	private fun getGarbageMaxTime(lv:Int):Int {
		var lv = lv
		val t = goaltype
		if(lv>GARBAGE_TIMER_TABLE[t].size-1) lv = GARBAGE_TIMER_TABLE[t].size-1
		return GARBAGE_TIMER_TABLE[t][lv]
	}

	/** Add garbage line(s)
	 * @param engine GameEngine
	 * @param lines Number of garbage lines to add
	 */
	private fun addGarbage(engine:GameEngine, lines:Int = 1) {
		// Add garbages
		val field = engine.field
		val w = field!!.width
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
					field.setBlock(x, h-1, Block(Block.COLOR.WHITE, engine.skin, Block.ATTRIBUTE.VISIBLE , Block.ATTRIBUTE.GARBAGE))

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
		owner.bgmStatus.bgm = if(engine.statistics.time<10800)BGM.RESULT(1) else BGM.RESULT(2)

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
			updateRanking(engine.statistics.score, engine.statistics.lines, garbageDigged, goaltype)

			if(rankingRank!=-1) {
				saveRanking(owner.modeConfig, engine.ruleopt.strRuleName)
				receiver.saveModeConfig(owner.modeConfig)
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
				rankingScore[j][i] = prop.getProperty("digchallenge.ranking.$ruleName.$j.score.$i", 0)
				rankingLines[j][i] = prop.getProperty("digchallenge.ranking.$ruleName.$j.lines.$i", 0)
				rankingDepth[j][i] = prop.getProperty("digchallenge.ranking.$ruleName.$j.depth.$i", 0)
			}
	}

	/** Save rankings to property file
	 * @param prop Property file
	 * @param ruleName Rule name
	 */
	private fun saveRanking(prop:CustomProperties?, ruleName:String) {
		for(i in 0 until RANKING_MAX)
			for(j in 0 until GOALTYPE_MAX) {
				prop!!.setProperty("digchallenge.ranking.$ruleName.$j.score.$i", rankingScore[j][i])
				prop.setProperty("digchallenge.ranking.$ruleName.$j.lines.$i", rankingLines[j][i])
				prop.setProperty("digchallenge.ranking.$ruleName.$j.depth.$i", rankingDepth[j][i])
			}
	}

	/** Update rankings
	 * @param sc Score
	 * @param li Lines
	 * @param dep Depth
	 */
	private fun updateRanking(sc:Int, li:Int, dep:Int, type:Int) {
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
		msg += "${engine.statistics.score}\t${engine.statistics.lines}\t${engine.statistics.totalPieceLocked}\t"
		msg += "${engine.statistics.time}\t${engine.statistics.level}\t"
		msg += "$garbageTimer\t$garbageTotal\t$garbageDigged\t$goaltype\t"
		msg += "${engine.gameActive}\t${engine.timerActive}\t"
		msg += "$lastscore\t$scgettime\t$bg\t$garbagePending\n"
		netLobby!!.netPlayerClient!!.send(msg)
	}

	/** NET: Receive various in-game stats (as well as goaltype) */
	override fun netRecvStats(engine:GameEngine, message:Array<String>) {
		engine.statistics.score = Integer.parseInt(message[4])
		engine.statistics.lines = Integer.parseInt(message[5])
		engine.statistics.totalPieceLocked = Integer.parseInt(message[6])
		engine.statistics.time = Integer.parseInt(message[7])
		engine.statistics.level = Integer.parseInt(message[8])
		garbageTimer = Integer.parseInt(message[9])
		garbageTotal = Integer.parseInt(message[10])
		garbageDigged = Integer.parseInt(message[11])
		goaltype = Integer.parseInt(message[12])
		engine.gameActive = java.lang.Boolean.parseBoolean(message[13])
		engine.timerActive = java.lang.Boolean.parseBoolean(message[14])
		lastscore = Integer.parseInt(message[15])
		scgettime = Integer.parseInt(message[16])
		engine.owner.backgroundStatus.bg = Integer.parseInt(message[17])
		garbagePending = Integer.parseInt(message[18])

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
		private const val GOALTYPE_NORMAL = 0
		private const val GOALTYPE_REALTIME = 1

		/** Fall velocity table (numerators) */
		private val tableGravity = intArrayOf(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 465, 731, 1280, 1707, -1, -1, -1)
		/** Fall velocity table (denominators) */
		private val tableDenominator =
			intArrayOf(64, 50, 39, 30, 22, 16, 12, 8, 6, 4, 3, 2, 1, 256, 256, 256, 256, 256, 256, 256)
		/** Garbage speed table */
		private val GARBAGE_TIMER_TABLE = arrayOf(
			intArrayOf(200, 190, 180, 170, 160, 150, 140, 130, 120, 110, 100, 95, 90, 85, 80, 75, 70, 65, 60, 60), // Normal
			intArrayOf(200, 190, 185, 180, 175, 170, 165, 160, 155, 150, 145, 140, 135, 130, 125, 120, 115, 110, 105, 100))// Realtime
	}
}
