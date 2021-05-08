package zeroxfc.nullpo.custom.modes

import mu.nu.nullpo.game.component.BGMStatus.BGM
import mu.nu.nullpo.game.component.Controller
import mu.nu.nullpo.game.event.EventReceiver
import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.gui.slick.RendererExtension
import mu.nu.nullpo.util.CustomProperties
import mu.nu.nullpo.util.GeneralUtil
import zeroxfc.nullpo.custom.libs.ProfileProperties
import kotlin.random.Random

class MissionMode:MarathonModeBase() {
	// Mission randomiser.
	private var missionRandomiser:Random = Random.Default
	// Mission text.
	private var currentMissionText:LinkedHashMap<String, EventReceiver.COLOR?> = LinkedHashMap()
	// Mission data.
	private var missionCategory = 0
	private var missionGoal = 0
	private var missionProgress = 0
	private var lineAmount = 0
	private var spinDetectionType = 0
	// private boolean isSpinMission;
	// private boolean isSpecific;
	private var specificPieceName:String? = null
	private var missionIsComplete = false

	/** Rankings' scores */
	private var rankingScore:Array<IntArray> = emptyArray()
	/** Rankings' times */
	private var rankingTime:Array<IntArray> = emptyArray()
	/**
	 * The good hard drop effect
	 */
	private var pCoordList:ArrayList<IntArray>? = null
	private val playerProperties:ProfileProperties = ProfileProperties(headerColor)
	private var PLAYER_NAME:String = ""
	private var showPlayerStats = false
	private val rankingScorePlayer:Array<IntArray> = emptyArray()
	private val rankingTimePlayer:Array<IntArray> = emptyArray()
	private var rankingRankPlayer = 0
	override val name:String
		get() = "MISSION MODE"
	/*
     * Initialization
     */
	override fun playerInit(engine:GameEngine, playerID:Int) {
		owner = engine.owner

		pCoordList = ArrayList()
		currentMissionText = LinkedHashMap()
		missionCategory = -1
		missionGoal = -1
		missionProgress = 0
		lineAmount = 0
		spinDetectionType = 0
		// isSpinMission = false;
		// isSpecific = false;
		specificPieceName = ""
		rankingRank = -1
		rankingScore = Array(RANKING_TYPE) {IntArray(RANKING_MAX)}
		rankingTime = Array(RANKING_TYPE) {IntArray(RANKING_MAX)}
		playerProperties.reset()
		showPlayerStats = false

		rankingRank = -1
		rankingScore = Array(RANKING_TYPE) {IntArray(RANKING_MAX)}
		rankingTime = Array(RANKING_TYPE) {IntArray(RANKING_MAX)}
		netPlayerInit(engine, playerID)
		if(!owner.replayMode) {
			loadSetting(owner.modeConfig)
			loadRanking(owner.modeConfig, engine.ruleopt.strRuleName)
			if(playerProperties.isLoggedIn) {
				loadSettingPlayer(playerProperties)
				loadRankingPlayer(playerProperties, engine.ruleopt.strRuleName)
			}
			PLAYER_NAME = ""
			version = CURRENT_VERSION
		} else {
			loadSetting(owner.replayProp)
			if(version==0&&owner.replayProp.getProperty("missionmode.endless", false)) goaltype = 2
			PLAYER_NAME = owner.replayProp.getProperty("missionmode.playerName", "")

			// NET: Load name
			netPlayerName = engine.owner.replayProp.getProperty("$playerID.net.netPlayerName", "")
		}
		engine.owner.backgroundStatus.bg = startlevel
		engine.framecolor = GameEngine.FRAME_COLOR_GREEN
		missionIsComplete = false
	}
	/*
     * Called at settings screen
     */
	override fun onSetting(engine:GameEngine, playerID:Int):Boolean {
		// NET: Net Ranking
		if(netIsNetRankingDisplayMode) {
			netOnUpdateNetPlayRanking(engine, goaltype)
		} else if(!engine.owner.replayMode) {
			// Configuration changes
			val change = updateCursor(engine, 0, playerID)
			if(change!=0) {
				engine.playSE("change")
				when(engine.statc[2]) {
					0 -> {
						goaltype += change
						if(goaltype<0) goaltype = GAMETYPE_MAX-1
						if(goaltype>GAMETYPE_MAX-1) goaltype = 0
						if(startlevel>(tableGameClearLines[goaltype]-1)/10&&tableGameClearLines[goaltype]>=0) {
							startlevel = (tableGameClearLines[goaltype]-1)/10
							engine.owner.backgroundStatus.bg = startlevel
						}
					}
				}

				// NET: Signal options change
				if(netIsNetPlay&&netNumSpectators>0) {
					netSendOptions(engine)
				}
			}

			// Confirm
			if(engine.ctrl.isPush(Controller.BUTTON_A)&&engine.statc[3]>=5) {
				engine.playSE("decide")
				if(playerProperties.isLoggedIn) {
					saveSettingPlayer(playerProperties)
					playerProperties.saveProfileConfig()
				} else {
					saveSetting(owner.modeConfig)
					owner.saveModeConfig()
				}

				// NET: Signal start of the game
				if(netIsNetPlay) netLobby!!.netPlayerClient!!.send("start1p\n")
				return false
			}

			// Cancel
			if(engine.ctrl.isPush(Controller.BUTTON_B)&&!netIsNetPlay) {
				engine.quitflag = true
				playerProperties.reset()
			}

			// New acc
			if(engine.ctrl.isPush(Controller.BUTTON_E)&&engine.ai==null&&!netIsNetPlay) {
				playerProperties.reset()
				engine.playSE("decide")
				engine.stat = GameEngine.Status.CUSTOM
				engine.resetStatc()
				return true
			}

			// NET: Netplay Ranking
			if(engine.ctrl.isPush(Controller.BUTTON_D)&&netIsNetPlay&&startlevel==0&&!big&&engine.ai==null) {
				netEnterNetPlayRankingScreen(engine, playerID, goaltype)
			}
			engine.statc[3]++
		} else {
			engine.statc[3]++
			engine.statc[2] = -1
			return engine.statc[3]<60
		}
		return true
	}

	override fun onCustom(engine:GameEngine, playerID:Int):Boolean {
		showPlayerStats = false
		engine.isInGame = true
		val s:Boolean = playerProperties.loginScreen.updateScreen(engine, playerID)
		if(playerProperties.isLoggedIn) {
			loadRankingPlayer(playerProperties, engine.ruleopt.strRuleName)
			loadSettingPlayer(playerProperties)
		}
		if(engine.stat===GameEngine.Status.SETTING) engine.isInGame = false
		return s
	}
	/*
     * Render the settings screen
     */
	override fun renderSetting(engine:GameEngine, playerID:Int) {
		if(netIsNetRankingDisplayMode) {
			// NET: Netplay Ranking
			netOnRenderNetPlayRanking(engine, playerID, receiver)
		} else {
			drawMenu(engine, playerID, receiver, 0, EventReceiver.COLOR.BLUE, 0,
				"GOAL", if(goaltype==2) "ENDLESS" else "${tableGameClearLines[goaltype]}"+" MISS.")
		}
	}

	override fun onReady(engine:GameEngine, playerID:Int):Boolean {
		if(engine.statc[0]==0) {
			missionRandomiser = Random(engine.randSeed)
			generateNewMission(engine, false)
		}
		return false
	}
	/*
     * Called after every frame
     */
	override fun onLast(engine:GameEngine, playerID:Int) {
		scgettime++
		if(scgettime>=120&&missionIsComplete&&engine.timerActive) {
			generateNewMission(engine, engine.statistics.score==tableGameClearLines[goaltype]-1)
		}
		if(engine.stat===GameEngine.Status.SETTING||engine.stat===GameEngine.Status.RESULT&&!owner.replayMode||engine.stat===GameEngine.Status.CUSTOM) {
			// Show rank
			if(engine.ctrl.isPush(Controller.BUTTON_F)&&playerProperties.isLoggedIn&&engine.stat!==GameEngine.Status.CUSTOM) {
				showPlayerStats = !showPlayerStats
				engine.playSE("change")
			}
		}
		if(engine.quitflag) {
			playerProperties.reset()
		}
	}
	/*
     * Render score
     */
	override fun renderLast(engine:GameEngine, playerID:Int) {
		if(owner.menuOnly) return
		receiver.drawScoreFont(engine, playerID, 0, 0, name, EventReceiver.COLOR.GREEN)
		if(tableGameClearLines[goaltype]==-1) {
			receiver.drawScoreFont(engine, playerID, 0, 1, "(ENDLESS GAME)", EventReceiver.COLOR.GREEN)
		} else {
			receiver.drawScoreFont(engine, playerID, 0, 1, "("+tableGameClearLines[goaltype]+" MISSIONS GAME)",
				EventReceiver.COLOR.GREEN)
		}
		if(engine.stat===GameEngine.Status.SETTING||engine.stat===GameEngine.Status.RESULT&&!owner.replayMode) {
			if(!owner.replayMode&&!big&&engine.ai==null) {
				val scale = if(receiver.nextDisplayType==2) 0.5f else 1.0f
				val topY = if(receiver.nextDisplayType==2) 6 else 4
				receiver.drawScoreFont(engine, playerID, 3, topY-1, "SCORE TIME", EventReceiver.COLOR.BLUE, scale)
				if(showPlayerStats) {
					for(i in 0 until RANKING_MAX) {
						receiver.drawScoreFont(engine, playerID, 0, topY+i, String.format("%2d", i+1), EventReceiver.COLOR.YELLOW, scale)
						receiver.drawScoreFont(engine, playerID, 3, topY+i, "${rankingScorePlayer[goaltype][i]}",
							i==rankingRankPlayer, scale)
						receiver.drawScoreFont(engine, playerID, 9, topY+i, GeneralUtil.getTime(
							rankingTimePlayer[goaltype][i]), i==rankingRankPlayer, scale)
					}
					receiver.drawScoreFont(engine, playerID, 0, topY+RANKING_MAX+1, "PLAYER SCORES", EventReceiver.COLOR.BLUE)
					receiver.drawScoreFont(engine, playerID, 0, topY+RANKING_MAX+2, playerProperties.nameDisplay,
						EventReceiver.COLOR.WHITE, 2f)
					receiver.drawScoreFont(engine, playerID, 0, topY+RANKING_MAX+5, "F:SWITCH RANK SCREEN", EventReceiver.COLOR.GREEN)
				} else {
					for(i in 0 until RANKING_MAX) {
						receiver.drawScoreFont(engine, playerID, 0, topY+i, String.format("%2d", i+1), EventReceiver.COLOR.YELLOW, scale)
						receiver.drawScoreFont(engine, playerID, 3, topY+i, "${rankingScore[goaltype][i]}", i==rankingRank, scale)
						receiver.drawScoreFont(engine, playerID, 9, topY+i, GeneralUtil.getTime(
							rankingTime[goaltype][i]), i==rankingRank, scale)
					}
					receiver.drawScoreFont(engine, playerID, 0, topY+RANKING_MAX+1, "LOCAL SCORES", EventReceiver.COLOR.BLUE)
					if(!playerProperties.isLoggedIn) receiver.drawScoreFont(engine, playerID, 0, topY+RANKING_MAX+2,
						"(NOT LOGGED IN)\n(E:LOG IN)")
					if(playerProperties.isLoggedIn) receiver.drawScoreFont(engine, playerID, 0, topY+RANKING_MAX+5,
						"F:SWITCH RANK SCREEN", EventReceiver.COLOR.GREEN)
				}
			}
		} else if(engine.stat===GameEngine.Status.CUSTOM) {
			playerProperties.loginScreen.renderScreen(receiver, engine, playerID)
		} else {
			receiver.drawScoreFont(engine, playerID, 0, 3, "COMPLETED", EventReceiver.COLOR.BLUE)
			val strScore:String = if(lastscore==0||scgettime>=120) {
				engine.statistics.score.toString()+"/"+(engine.statistics.level+1)*5
			} else {
				engine.statistics.score.toString()+"(+"+lastscore+")"
			}
			receiver.drawScoreFont(engine, playerID, 0, 4, strScore)
			receiver.drawScoreFont(engine, playerID, 0, 6, "LEVEL", EventReceiver.COLOR.BLUE)
			receiver.drawScoreFont(engine, playerID, 0, 7, (engine.statistics.level+1).toString())
			receiver.drawScoreFont(engine, playerID, 0, 9, "TIME", EventReceiver.COLOR.BLUE)
			receiver.drawScoreFont(engine, playerID, 0, 10, GeneralUtil.getTime(engine.statistics.time))
			receiver.drawScoreFont(engine, playerID, 0, 12, "CURRENT MISSION", EventReceiver.COLOR.GREEN)
			receiver.drawScoreFont(engine, playerID, 0, 13, "$missionProgress/$missionGoal", missionIsComplete)
			if(!missionIsComplete) {
				drawMissionStrings(engine, playerID, currentMissionText, 0, 15, 1.0f)
			}
			if(playerProperties.isLoggedIn||PLAYER_NAME.isNotEmpty()) {
				receiver.drawScoreFont(engine, playerID, 0, 20, "PLAYER", EventReceiver.COLOR.BLUE)
				receiver.drawScoreFont(engine, playerID, 0, 21, if(owner.replayMode) PLAYER_NAME else playerProperties.nameDisplay,
					EventReceiver.COLOR.WHITE, 2f)
			}
		}

		// NET: Number of spectators
		netDrawSpectatorsCount(engine, 0, 18)
		// NET: All number of players
		if(playerID==players-1) {
			netDrawAllPlayersCount()
			netDrawGameRate(engine)
		}
		// NET: Player name (It may also appear in offline replay)
		netDrawPlayerName(engine)
	}
	/*
     * Calculate score
     */
	override fun calcScore(engine:GameEngine, playerID:Int, lines:Int):Int {
		// Line clear bonus
		var pts = 0
		var incremented = false
		val shapeName = engine.nowPieceObject?.type?.name ?: ""
		if(!missionIsComplete) {
			when(missionCategory) {
				MISSION_CATEGORY_SPIN -> if(engine.twist) {
					if(lines==lineAmount&&shapeName===specificPieceName) {
						missionProgress++
						incremented = true
					}
				}
				MISSION_CATEGORY_SPIN_NONSPECIFIC -> if(engine.twist) {
					if(lines==lineAmount) {
						missionProgress++
						incremented = true
					}
				}
				MISSION_CATEGORY_CLEAR -> if(lines==lineAmount&&shapeName===specificPieceName) {
					missionProgress++
					incremented = true
				}
				MISSION_CATEGORY_CLEAR_NONSPECIFIC -> if(lines==lineAmount) {
					missionProgress++
					incremented = true
				}
			}
		}
		if(!missionIsComplete) {
			if(missionCategory==MISSION_CATEGORY_COMBO) {
				missionProgress = engine.combo
			}
			if(missionCategory==MISSION_CATEGORY_COMBO&&missionProgress==missionGoal) {
				incremented = true
			}
		}

		// Combo
		if(missionProgress==missionGoal&&incremented) {
			pts = 1
			missionIsComplete = true
			engine.playSE("cool")
		}

		// Add to score
		if(pts>0) {
			if(lines>=1) engine.statistics.scoreLine += pts else engine.statistics.scoreBonus += pts
		}

		// BGM fade-out effects and BGM changes
		if(tableBGMChange[bgmlv]!=-1) {
			if(engine.statistics.score>=tableBGMChange[bgmlv]-1) owner.bgmStatus.fadesw = true
			if(engine.statistics.score>=tableBGMChange[bgmlv]&&
				(engine.statistics.score<tableGameClearLines[goaltype]||tableGameClearLines[goaltype]<0)) {
				bgmlv++
				owner.bgmStatus.bgm = tableBGM[bgmlv]
				owner.bgmStatus.fadesw = false
			}
		}

		// Meter
		engine.meterValue = engine.statistics.score%10*receiver.getMeterMax(engine)/9
		engine.meterColor = GameEngine.METER_COLOR_GREEN
		if(engine.statistics.score%10>=4) engine.meterColor = GameEngine.METER_COLOR_YELLOW
		if(engine.statistics.score%10>=6) engine.meterColor = GameEngine.METER_COLOR_ORANGE
		if(engine.statistics.score%10>=8) engine.meterColor = GameEngine.METER_COLOR_RED
		if(engine.statistics.score>=tableGameClearLines[goaltype]&&tableGameClearLines[goaltype]>=0) {
			// Ending
			engine.ending = 1
			engine.gameEnded()
		} else if(engine.statistics.score>=(engine.statistics.level+1)*5&&engine.statistics.level<19) {
			// Level up
			engine.statistics.level++
			owner.backgroundStatus.fadesw = true
			owner.backgroundStatus.fadecount = 0
			owner.backgroundStatus.fadebg = engine.statistics.level
			setSpeed(engine)
			engine.playSE("levelup")
		}
		return 0
	}
	/*
     * Soft drop
     */
	override fun afterSoftDropFall(engine:GameEngine, playerID:Int, fall:Int) {
		return
	}

	override fun onFirst(engine:GameEngine, playerID:Int) {
		pCoordList?.clear()
	}
	/*
     * Hard drop
     */
	override fun afterHardDropFall(engine:GameEngine, playerID:Int, fall:Int) {
		val baseX:Int = 16*engine.nowPieceX+4+receiver.fieldX(engine, playerID)
		val baseY:Int = 16*engine.nowPieceY+52+receiver.fieldY(engine, playerID)
		engine.nowPieceObject?.let {cPiece ->
			for(i in 1..fall) {
				pCoordList!!.add(intArrayOf(engine.nowPieceX, engine.nowPieceY-i))
			}
			for(i in 0 until cPiece.maxBlock) {
				if(!cPiece.big) {
					val x2:Int = baseX+cPiece.dataX[cPiece.direction][i]*16
					val y2:Int = baseY+cPiece.dataY[cPiece.direction][i]*16
					RendererExtension.addBlockBreakEffect(receiver, x2, y2, cPiece.block[i])
				} else {
					val x2:Int = baseX+cPiece.dataX[cPiece.direction][i]*32
					val y2:Int = baseY+cPiece.dataY[cPiece.direction][i]*32
					RendererExtension.addBlockBreakEffect(receiver, x2, y2, cPiece.block[i])
					RendererExtension.addBlockBreakEffect(receiver, x2+16, y2, cPiece.block[i])
					RendererExtension.addBlockBreakEffect(receiver, x2, y2+16, cPiece.block[i])
					RendererExtension.addBlockBreakEffect(receiver, x2+16, y2+16, cPiece.block[i])
				}
			}
		}
	}
	// ------------------------------------------------------------------------------------------
	// Ranking/Setting Saving/Loading
	// ------------------------------------------------------------------------------------------
	/*
     * Called for initialization during "Ready" screen
     */
	override fun startGame(engine:GameEngine, playerID:Int) {
		engine.statistics.level = startlevel
		engine.statistics.levelDispAdd = 1
		engine.b2bEnable = true
		engine.twistEnable = true
		engine.useAllSpinBonus = true
		engine.twistAllowKick = true
		engine.twistEnableEZ = false
		setSpeed(engine)
		if(netIsWatch) {
			owner.bgmStatus.bgm = BGM.Silent
		}
	}
	/*
     * Called when saving replay
     */
	override fun saveReplay(engine:GameEngine, playerID:Int, prop:CustomProperties) {
		saveSetting(prop)

		// NET: Save name
		if(netPlayerName!=null&&netPlayerName!!.isNotEmpty()) {
			prop.setProperty("$playerID.net.netPlayerName", netPlayerName)
		}

		// Update rankings
		if(!owner.replayMode&&!big&&engine.ai==null) {
			updateRanking(engine.statistics.score, engine.statistics.time, goaltype)
			if(playerProperties.isLoggedIn) {
				prop.setProperty("missionmode.playerName", playerProperties.nameDisplay)
			}
			if(rankingRank!=-1) {
				saveRanking(owner.modeConfig, engine.ruleopt.strRuleName)
				owner.saveModeConfig()
			}
			if(rankingRankPlayer!=-1&&playerProperties.isLoggedIn) {
				saveRankingPlayer(playerProperties, engine.ruleopt.strRuleName)
				playerProperties.saveProfileConfig()
			}
		}
	}
	/**
	 * Load settings from property file
	 *
	 * @param prop Property file
	 */
	override fun loadSetting(prop:CustomProperties) {
		goaltype = prop.getProperty("missionmode.gametype", 0)
		version = prop.getProperty("missionmode.version", 0)
	}
	/**
	 * Save settings to property file
	 *
	 * @param prop Property file
	 */
	override fun saveSetting(prop:CustomProperties) {
		prop.setProperty("missionmode.gametype", goaltype)
		prop.setProperty("missionmode.version", version)
	}
	/**
	 * Load settings from property file
	 *
	 * @param prop Property file
	 */
	private fun loadSettingPlayer(prop:ProfileProperties?) {
		if(prop?.isLoggedIn!=true) return
		goaltype = prop.getProperty("missionmode.gametype", 0)
	}
	/**
	 * Save settings to property file
	 *
	 * @param prop Property file
	 */
	private fun saveSettingPlayer(prop:ProfileProperties) {
		if(!prop.isLoggedIn) return
		prop.setProperty("missionmode.gametype", goaltype)
	}
	/**
	 * Read rankings from property file
	 *
	 * @param prop     Property file
	 * @param ruleName Rule name
	 */
	override fun loadRanking(prop:CustomProperties, ruleName:String) {
		for(i in 0 until RANKING_MAX) {
			for(j in 0 until GAMETYPE_MAX) {
				rankingScore[j][i] = prop.getProperty("missionmode.ranking.$ruleName.$j.score.$i", 0)
				rankingTime[j][i] = prop.getProperty("missionmode.ranking.$ruleName.$j.time.$i", 0)
			}
		}
	}
	/**
	 * Save rankings to property file
	 *
	 * @param prop     Property file
	 * @param ruleName Rule name
	 */
	private fun saveRanking(prop:CustomProperties, ruleName:String) {
		for(i in 0 until RANKING_MAX) {
			for(j in 0 until GAMETYPE_MAX) {
				prop.setProperty("missionmode.ranking.$ruleName.$j.score.$i", rankingScore[j][i])
				prop.setProperty("missionmode.ranking.$ruleName.$j.time.$i", rankingTime[j][i])
			}
		}
	}
	/**
	 * Read rankings from property file
	 *
	 * @param prop     Property file
	 * @param ruleName Rule name
	 */
	private fun loadRankingPlayer(prop:ProfileProperties?, ruleName:String) {
		if(prop?.isLoggedIn!=true) return
		for(i in 0 until RANKING_MAX) {
			for(j in 0 until GAMETYPE_MAX) {
				rankingScorePlayer[j][i] = prop.getProperty("missionmode.ranking.$ruleName.$j.score.$i", 0)
				rankingTimePlayer[j][i] = prop.getProperty("missionmode.ranking.$ruleName.$j.time.$i", 0)
			}
		}
	}
	/**
	 * Save rankings to property file
	 *
	 * @param prop     Property file
	 * @param ruleName Rule name
	 */
	private fun saveRankingPlayer(prop:ProfileProperties?, ruleName:String) {
		if(prop?.isLoggedIn!=true) return
		for(i in 0 until RANKING_MAX) {
			for(j in 0 until GAMETYPE_MAX) {
				prop.setProperty("missionmode.ranking.$ruleName.$j.score.$i", rankingScorePlayer[j][i])
				prop.setProperty("missionmode.ranking.$ruleName.$j.time.$i", rankingTimePlayer[j][i])
			}
		}
	}
	/**
	 * Update rankings
	 *
	 * @param sc   Score
	 * @param time Time
	 */
	private fun updateRanking(sc:Int, time:Int, type:Int) {
		rankingRank = checkRanking(sc, time, type)
		if(rankingRank!=-1) {
			// Shift down ranking entries
			for(i in RANKING_MAX-1 downTo rankingRank+1) {
				rankingScore[type][i] = rankingScore[type][i-1]
				rankingTime[type][i] = rankingTime[type][i-1]
			}

			// Add new data
			rankingScore[type][rankingRank] = sc
			rankingTime[type][rankingRank] = time
		}
		if(playerProperties.isLoggedIn) {
			rankingRankPlayer = checkRankingPlayer(sc, time, type)
			if(rankingRankPlayer!=-1) {
				// Shift down ranking entries
				for(i in RANKING_MAX-1 downTo rankingRankPlayer+1) {
					rankingScorePlayer[type][i] = rankingScorePlayer[type][i-1]
					rankingTimePlayer[type][i] = rankingTimePlayer[type][i-1]
				}

				// Add new data
				rankingScorePlayer[type][rankingRankPlayer] = sc
				rankingTimePlayer[type][rankingRankPlayer] = time
			}
		}
	}
	/**
	 * Calculate ranking position
	 *
	 * @param sc   Score
	 * @param time Time
	 * @return Position (-1 if unranked)
	 */
	private fun checkRanking(sc:Int, time:Int, type:Int):Int {
		for(i in 0 until RANKING_MAX) {
			if(sc>rankingScore[type][i]) {
				return i
			} else if(sc==rankingScore[type][i]&&time<rankingTime[type][i]) {
				return i
			}
		}
		return -1
	}
	/**
	 * Calculate ranking position
	 *
	 * @param sc   Score
	 * @param time Time
	 * @return Position (-1 if unranked)
	 */
	private fun checkRankingPlayer(sc:Int, time:Int, type:Int):Int {
		for(i in 0 until RANKING_MAX) {
			if(sc>rankingScorePlayer[type][i]) {
				return i
			} else if(sc==rankingScorePlayer[type][i]&&time<rankingTimePlayer[type][i]) {
				return i
			}
		}
		return -1
	}
	/*
     * Render results screen
     */
	override fun renderResult(engine:GameEngine, playerID:Int) {
		drawResultStats(engine, playerID, receiver, 0, EventReceiver.COLOR.BLUE,
			Statistic.SCORE, Statistic.LEVEL, Statistic.TIME, Statistic.LPM)
		drawResultRank(engine, playerID, receiver, 8, EventReceiver.COLOR.BLUE, rankingRank)
		drawResultNetRank(engine, playerID, receiver, 10, EventReceiver.COLOR.BLUE, netRankingRank[0])
		drawResultNetRankDaily(engine, playerID, receiver, 12, EventReceiver.COLOR.BLUE, netRankingRank[1])
		if(netIsPB) {
			receiver.drawMenuFont(engine, playerID, 2, 21, "NEW PB", EventReceiver.COLOR.ORANGE)
		}
		if(netIsNetPlay&&netReplaySendStatus==1) {
			receiver.drawMenuFont(engine, playerID, 0, 22, "SENDING...", EventReceiver.COLOR.PINK)
		} else if(netIsNetPlay&&!netIsWatch&&netReplaySendStatus==2) {
			receiver.drawMenuFont(engine, playerID, 1, 22, "A: RETRY", EventReceiver.COLOR.RED)
		}
	}
	// ------------------------------------------------------------------------------------------
	// CRAPPY MISSION GENERATION METHODS >_<
	// ------------------------------------------------------------------------------------------
	/*
     * I don't even know how these even are supposed to integrate into the rest of the mode but we'll get there later.
     */
	private fun generateNewMission(engine:GameEngine, lastMission:Boolean) {
		val classic:Boolean = engine.ruleopt.strWallkick.contains("Classic")
			||engine.ruleopt.strWallkick.contains("GBC")||
			engine.ruleopt.strWallkick.contains("WallOnly")
		val missionTypeRd = missionRandomiser.nextInt(100)
		val missionType = when {
			missionTypeRd<40 -> MISSION_CATEGORY_CLEAR_NONSPECIFIC
			missionTypeRd<60 -> MISSION_CATEGORY_COMBO
			missionTypeRd<80 -> MISSION_CATEGORY_SPIN_NONSPECIFIC
			missionTypeRd<90 -> MISSION_CATEGORY_CLEAR
			else -> MISSION_CATEGORY_SPIN
		}
		missionCategory = missionType
		missionIsComplete = false
		when(missionType) {
			MISSION_CATEGORY_CLEAR -> {
				val pieceNum = missionRandomiser.nextInt(VALID_LINECLEAR_PIECES.size)
				val pieceName = VALID_LINECLEAR_PIECES[pieceNum]
				val bodyColor = missionColor(pieceName, classic)
				val lineClear = missionRandomiser.nextInt(
					if(classic) VALID_CLASSIC_MAX_LINECLEARS[pieceNum] else VALID_MAX_LINECLEARS[pieceNum])+1
				val amount = missionRandomiser.nextInt(MAX_MISSION_GOAL)+1
				missionGoal = amount
				missionProgress = 0
				lineAmount = lineClear
				//isSpecific = true;
				//isSpinMission = false;
				specificPieceName = pieceName
				spinDetectionType = 0
				currentMissionText = generateSpecificLineMissionData(pieceName, bodyColor, lineClear, amount, lastMission)
			}
			MISSION_CATEGORY_CLEAR_NONSPECIFIC -> {
				val lineClear1 = missionRandomiser.nextInt(4)+1
				val amount1 = missionRandomiser.nextInt(MAX_MISSION_GOAL)+1
				missionGoal = amount1
				missionProgress = 0
				lineAmount = lineClear1
				//isSpecific = false;
				//isSpinMission = false;
				specificPieceName = "NULL"
				spinDetectionType = 0
				currentMissionText = generateLineMissionData(lineClear1, amount1, lastMission)
			}
			MISSION_CATEGORY_SPIN -> {
				val pieceNum2 = missionRandomiser.nextInt(VALID_SPIN_PIECES.size)
				val pieceName2 = VALID_SPIN_PIECES[pieceNum2]
				val bodyColor2 = missionColor(pieceName2, classic)
				val lineClear2 = missionRandomiser.nextInt(
					if(classic) VALID_CLASSIC_SPIN_MAX_LINES[pieceNum2] else VALID_SPIN_MAX_LINES[pieceNum2])
				val amount2 = missionRandomiser.nextInt(MAX_MISSION_GOAL)+1
				missionGoal = amount2
				missionProgress = 0
				lineAmount = lineClear2
				//isSpecific = true;
				//isSpinMission = true;
				specificPieceName = pieceName2
				spinDetectionType = missionRandomiser.nextInt(2)
				currentMissionText = generateSpinMissionData(pieceName2, bodyColor2, spinDetectionType, lineClear2, amount2,
					lastMission)
			}
			MISSION_CATEGORY_SPIN_NONSPECIFIC -> {
				val lineClear3 = missionRandomiser.nextInt(if(classic) 2 else 3)
				val amount3 = missionRandomiser.nextInt(MAX_MISSION_GOAL)+1
				missionGoal = amount3
				missionProgress = 0
				lineAmount = lineClear3
				//isSpecific = false;
				//isSpinMission = true;
				specificPieceName = "NULL"
				spinDetectionType = missionRandomiser.nextInt(2)
				currentMissionText = generateNonSpecificSpinMissionData(spinDetectionType, lineClear3, amount3, lastMission)
			}
			MISSION_CATEGORY_COMBO -> {
				val comboLimit = missionRandomiser.nextInt(MAX_MISSION_COMBO)+3
				missionGoal = comboLimit
				missionProgress = 0
				lineAmount = 0
				//isSpecific = false;
				//isSpinMission = false;
				specificPieceName = "NULL"
				spinDetectionType = 0
				currentMissionText = generateComboMissionData(comboLimit, lastMission)
			}
		}
		engine.comboType = if(missionType==MISSION_CATEGORY_COMBO) GameEngine.COMBO_TYPE_NORMAL else 0
		engine.combo = 0
	}

	private fun missionColor(pieceName:String, classic:Boolean):EventReceiver.COLOR {
		return when(pieceName) {
			"I" -> if(classic) MISSION_I_CLASSIC else MISSION_I
			"J" -> MISSION_J
			"L" -> MISSION_L
			"O" -> MISSION_O
			"S" -> if(classic) MISSION_S_CLASSIC else MISSION_S
			"T" -> if(classic) MISSION_T_CLASSIC else MISSION_T
			"Z" -> if(classic) MISSION_Z_CLASSIC else MISSION_Z
			else -> MISSION_GENERIC
		}
	}

	private fun generateSpinMissionData(pieceName:String, bodyColor:EventReceiver.COLOR, spinType:Int, spinLine:Int, amount:Int,
		lastMission:Boolean):LinkedHashMap<String, EventReceiver.COLOR?> {
		val header = "PERFORM"
		val footer = if(lastMission) "TO WIN!" else "TO ADVANCE!"
		val missionBody = "$amount"+"X "+pieceName+SPIN_MISSION_TABLE[spinLine]+if(amount>1) "S" else ""
		val result = LinkedHashMap<String, EventReceiver.COLOR?>()
		result[header] = MISSION_NEUTRAL // Key is text to draw, value is color.
		result["[NEWLINE]"] = null// Tells the text-drawing method to move one line down.
		result[missionBody] = bodyColor
		result["[NEWLINE1]"] = null
		result["USING THE "] = MISSION_NEUTRAL
		result["[NEWLINE3]"] = null
		result[if(spinType==0) "4-POINT" else "IMMOBILE"] = MISSION_GENERIC
		result[" SPIN TYPE"] = MISSION_NEUTRAL
		result["[NEWLINE2]"] = null
		result[footer] = MISSION_NEUTRAL
		return result
	}

	private fun generateNonSpecificSpinMissionData(spinType:Int, spinLine:Int, amount:Int,
		lastMission:Boolean):LinkedHashMap<String, EventReceiver.COLOR?> {
		val header = "PERFORM"
		val footer = if(lastMission) "TO WIN!" else "TO ADVANCE!"
		val missionBody = "$amount"+"X "+NON_SPECIFIC_SPIN_MISSION_TABLE[spinLine]+if(amount>1) "S" else ""
		val result = LinkedHashMap<String, EventReceiver.COLOR?>()
		result[header] = MISSION_NEUTRAL // Key is text to draw, value is color.
		result["[NEWLINE]"] = null // Tells the text-drawing method to move one line down.
		result[missionBody] = MISSION_GENERIC
		result["[NEWLINE1]"] = null
		result["USING THE "] = null
		result["[NEWLINE3]"] = MISSION_NEUTRAL
		result[if(spinType==0) "4-POINT" else "IMMOBILE"] = MISSION_GENERIC
		result[" SPIN TYPE"] = MISSION_NEUTRAL
		result["[NEWLINE2]"] = null
		result[footer] = MISSION_NEUTRAL
		return result
	}

	private fun generateLineMissionData(lineClear:Int, amount:Int,
		lastMission:Boolean):LinkedHashMap<String, EventReceiver.COLOR?> {
		val header = "PERFORM"
		val footer = if(lastMission) "TO WIN!" else "TO ADVANCE!"
		val missionBody = "$amount"+"X "+LINECLEAR_MISSION_TABLE[lineClear-1]+if(amount>1) if(lineClear!=4) "S" else "ES" else ""
		val result = LinkedHashMap<String, EventReceiver.COLOR?>()
		result[header] = MISSION_NEUTRAL
		result["[NEWLINE]"] = null
		result[missionBody] = MISSION_GENERIC
		result["[NEWLINE1]"] = null
		result[footer] = MISSION_NEUTRAL
		return result
	}

	private fun generateSpecificLineMissionData(pieceName:String, bodyColor:EventReceiver.COLOR, lineClear:Int, amount:Int,
		lastMission:Boolean):LinkedHashMap<String, EventReceiver.COLOR?> {
		val header = "PERFORM"
		val footer = if(lastMission) "TO WIN!" else "TO ADVANCE!"
		val missionBody = "$amount"+"X "+LINECLEAR_MISSION_TABLE[lineClear-1]+if(amount>1) if(lineClear!=4) "S" else "ES" else ""
		val result = LinkedHashMap<String, EventReceiver.COLOR?>()
		result[header] = MISSION_NEUTRAL
		result["[NEWLINE]"] = null
		result[missionBody] = MISSION_GENERIC
		result["[NEWLINE1]"] = null
		result["USING "] = MISSION_NEUTRAL
		result["$pieceName-PIECES"] = bodyColor
		result["[NEWLINE2]"] = null
		result[footer] = MISSION_NEUTRAL
		return result
	}

	private fun generateComboMissionData(maxCombo:Int, lastMission:Boolean):LinkedHashMap<String, EventReceiver.COLOR?> {
		val header = "PERFORM"
		val footer = if(lastMission) "TO WIN!" else "TO ADVANCE!"
		val missionBody = "A "+maxCombo+"X "+"COMBO"
		val result = LinkedHashMap<String, EventReceiver.COLOR?>()
		result[header] = MISSION_NEUTRAL
		result["[NEWLINE]"] = null
		result[missionBody] = MISSION_GENERIC
		result["[NEWLINE1]"] = null
		result[footer] = MISSION_NEUTRAL
		return result
	}
	/**
	 * Draws the mission texts to a location.
	 *
	 * @param engine       GameEngine to draw with.
	 * @param playerID     Player to draw next to (0 = 1P).
	 * @param missionData  LinkedHashMap containing mission text and color data.
	 * @param destinationX X of destination (uses drawScoreFont(...)).
	 * @param destinationY Y of destination (uses drawScoreFont(...)).
	 * @param scale        Text scale (0.5f, 1.0f, 2.0f).
	 */
	private fun drawMissionStrings(engine:GameEngine, playerID:Int, missionData:LinkedHashMap<String, EventReceiver.COLOR?>?,
		destinationX:Int, destinationY:Int, scale:Float) {
		var counterX = 0
		var counterY = 0
		currentMissionText.forEach {(str, col) ->
			if(col!=null) {
				receiver.drawScoreFont(engine, playerID, destinationX+counterX, destinationY+counterY, str, col, scale)
				counterX += str.length
			} else {
				counterX = 0
				counterY++
			}
		}
	}

	companion object {
		/**
		 * Line counts when BGM changes occur
		 */
		val tableBGMChange = intArrayOf(25, 50, 75, 100, -1)
		private val tableBGM =
			arrayOf(BGM.Puzzle(0), BGM.Generic(1), BGM.Generic(2), BGM.GrandM(0), BGM.GrandM(1))
		/**
		 * Line counts when game ending occurs
		 */
		val tableGameClearLines = intArrayOf(50, 100, -1)
		private val MISSION_GENERIC = EventReceiver.COLOR.PINK
		private val MISSION_I = EventReceiver.COLOR.CYAN
		private val MISSION_I_CLASSIC = EventReceiver.COLOR.RED
		private val MISSION_J = EventReceiver.COLOR.COBALT
		private val MISSION_L = EventReceiver.COLOR.ORANGE
		private val MISSION_O = EventReceiver.COLOR.YELLOW
		private val MISSION_S = EventReceiver.COLOR.GREEN
		private val MISSION_S_CLASSIC = EventReceiver.COLOR.PURPLE
		private val MISSION_T = EventReceiver.COLOR.PURPLE
		private val MISSION_T_CLASSIC = EventReceiver.COLOR.CYAN
		private val MISSION_Z = EventReceiver.COLOR.RED
		private val MISSION_Z_CLASSIC = EventReceiver.COLOR.GREEN
		private val MISSION_NEUTRAL = EventReceiver.COLOR.WHITE
		private const val MISSION_CATEGORY_SPIN = 0
		private const val MISSION_CATEGORY_CLEAR = 1
		private const val MISSION_CATEGORY_SPIN_NONSPECIFIC = 2
		private const val MISSION_CATEGORY_CLEAR_NONSPECIFIC = 3
		private const val MISSION_CATEGORY_COMBO = 4
		private val SPIN_MISSION_TABLE = arrayOf(
			"-SPIN",
			"-SPIN SINGLE",
			"-SPIN DOUBLE",
			"-SPIN QUAD")
		private val NON_SPECIFIC_SPIN_MISSION_TABLE = arrayOf(
			"SPIN",
			"SPIN SINGLE",
			"SPIN DOUBLE",
			"SPIN QUAD")
		private val LINECLEAR_MISSION_TABLE = arrayOf(
			"SINGLE",
			"DOUBLE",
			"TRIPLE",
			"QUADRUPLE"
		)
		private val VALID_SPIN_PIECES = arrayOf(
			"J", "L", "S", "T", "Z"
		)
		private val VALID_LINECLEAR_PIECES = arrayOf(
			"I", "J", "L", "O", "S", "T", "Z"
		)
		private val VALID_SPIN_MAX_LINES = intArrayOf(
			3, 3, 3, 3, 3
		)
		private val VALID_CLASSIC_SPIN_MAX_LINES = intArrayOf(
			2, 2, 1, 2, 1
		)
		private val VALID_MAX_LINECLEARS = intArrayOf(
			4, 3, 3, 2, 3, 3, 3
		)
		private val VALID_CLASSIC_MAX_LINECLEARS = intArrayOf(
			4, 3, 3, 2, 1, 2, 1
		)
		private const val MAX_MISSION_GOAL = 4
		private const val MAX_MISSION_COMBO = 5 // Add two. (3 because random...)
		// private static final int MAX_MISSION_TYPES = 5;
		private val headerColor = EventReceiver.COLOR.GREEN
	}
}