/*
 * Copyright (c) 2021-2021,
 * This library class was created by 0xFC963F18DC21 / Shots243
 * It is part of an extension library for the game NullpoMino (copyright 2021-2021)
 *
 * Kotlin converted and modified by Venom=Nhelv
 *
 * Herewith shall the term "Library Creator" be given to 0xFC963F18DC21.
 * Herewith shall the term "Game Creator" be given to the original creator of NullpoMino, NullNoname.
 *
 * THIS LIBRARY AND MODE PACK WAS NOT MADE IN ASSOCIATION WITH THE GAME CREATOR.
 *
 * Repository: https://github.com/Shots243/ModePile
 *
 * When using this library in a mode / library pack of your own, the following
 * conditions must be satisfied:
 *     - This license must remain visible at the top of the document, unmodified.
 *     - You are allowed to use this library for any modding purpose.
 *         - If this is the case, the Library Creator must be credited somewhere.
 *             - Source comments only are fine, but in a README is recommended.
 *     - Modification of this library is allowed, but only in the condition that a
 *       pull request is made to merge the changes to the repository.
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

package zeroxfc.nullpo.custom.modes

import mu.nu.nullpo.game.component.BGMStatus.BGM
import mu.nu.nullpo.game.component.Controller
import mu.nu.nullpo.game.component.Piece
import mu.nu.nullpo.game.event.EventReceiver
import mu.nu.nullpo.game.event.EventReceiver.COLOR
import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.util.CustomProperties
import mu.nu.nullpo.util.GeneralUtil.toTimeStr
import zeroxfc.nullpo.custom.libs.ProfileProperties
import zeroxfc.nullpo.custom.modes.MissionMode.Companion.MissionType.*
import kotlin.random.Random

class MissionMode:MarathonModeBase() {
	// Mission randomiser.
	private var missionRandomiser:Random = Random.Default
	// Mission text.
	private var currentMissionText:LinkedHashMap<String, COLOR?> = LinkedHashMap()
	// Mission data.
	private var missionCategory:MissionType? = null
	private var missionGoal = 0
	private var missionProgress = 0
	private var lineAmount = 0
	// private boolean isSpinMission;
	// private boolean isSpecific;
	private var specificPieceName:String = ""
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
	private var PLAYER_NAME = ""
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
		missionCategory = null
		missionGoal = -1
		missionProgress = 0
		lineAmount = 0
		// isSpinMission = false;
		// isSpecific = false;
		specificPieceName = ""
		rankingRank = -1
		rankingScore = Array(GAMETYPE_MAX) {IntArray(RANKING_MAX)}
		rankingTime = Array(GAMETYPE_MAX) {IntArray(RANKING_MAX)}
		playerProperties.reset()
		showPlayerStats = false

		netPlayerInit(engine, playerID)
		if(!owner.replayMode) {
			loadSetting(owner.modeConfig)
			loadRanking(owner.modeConfig, engine.ruleOpt.strRuleName)
			if(playerProperties.isLoggedIn) {
				loadSettingPlayer(playerProperties)
				loadRankingPlayer(playerProperties, engine.ruleOpt.strRuleName)
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
						if(startlevel>(tableGameClearMissions[goaltype]-1)/10&&tableGameClearMissions[goaltype]>=0) {
							startlevel = (tableGameClearMissions[goaltype]-1)/10
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
			loadRankingPlayer(playerProperties, engine.ruleOpt.strRuleName)
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
			drawMenu(engine, playerID, receiver, 0, COLOR.BLUE, 0,
				"GOAL" to if(tableGameClearMissions[goaltype]<0) "ENDLESS" else "${tableGameClearMissions[goaltype]} MISS.")
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
			generateNewMission(engine, engine.statistics.score==tableGameClearMissions[goaltype]-1)
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
		receiver.drawScoreFont(engine, playerID, 0, 0, name, COLOR.GREEN)
		if(tableGameClearMissions[goaltype]<0)
			receiver.drawScoreFont(engine, playerID, 0, 1, "(Endless run)", COLOR.GREEN)
		else receiver.drawScoreFont(engine, playerID, 0, 1, "(${tableGameClearMissions[goaltype]} missions run)", COLOR.GREEN)
		if(engine.stat===GameEngine.Status.SETTING||engine.stat===GameEngine.Status.RESULT&&!owner.replayMode) {
			if(!owner.replayMode&&!big&&engine.ai==null) {
				val scale = if(receiver.nextDisplayType==2) 0.5f else 1.0f
				val topY = if(receiver.nextDisplayType==2) 6 else 4
				receiver.drawScoreFont(engine, playerID, 3, topY-1, "SCORE TIME", COLOR.BLUE, scale)
				if(showPlayerStats) {
					for(i in 0 until RANKING_MAX) {
						receiver.drawScoreGrade(engine, playerID, 0, topY+i, String.format("%2d", i+1), COLOR.YELLOW, scale)
						receiver.drawScoreNum(engine, playerID, 3, topY+i, "${rankingScorePlayer[goaltype][i]}",
							i==rankingRankPlayer, scale)
						receiver.drawScoreNum(engine, playerID, 9, topY+i, rankingTimePlayer[goaltype][i].toTimeStr, i==rankingRankPlayer,
							scale)
					}
					receiver.drawScoreFont(engine, playerID, 0, topY+RANKING_MAX+1, "PLAYER SCORES", COLOR.BLUE)
					receiver.drawScoreFont(engine, playerID, 0, topY+RANKING_MAX+2, playerProperties.nameDisplay,
						COLOR.WHITE, 2f)
					receiver.drawScoreFont(engine, playerID, 0, topY+RANKING_MAX+5, "F:SWITCH RANK SCREEN", COLOR.GREEN)
				} else {
					for(i in 0 until RANKING_MAX) {
						receiver.drawScoreGrade(engine, playerID, 0, topY+i, String.format("%2d", i+1), COLOR.YELLOW, scale)
						receiver.drawScoreNum(engine, playerID, 3, topY+i, "${rankingScore[goaltype][i]}", i==rankingRank, scale)
						receiver.drawScoreNum(engine, playerID, 9, topY+i, rankingTime[goaltype][i].toTimeStr, i==rankingRank, scale)
					}
					receiver.drawScoreFont(engine, playerID, 0, topY+RANKING_MAX+1, "LOCAL SCORES", COLOR.BLUE)
					if(!playerProperties.isLoggedIn) receiver.drawScoreFont(engine, playerID, 0, topY+RANKING_MAX+2,
						"(NOT LOGGED IN)\n(E:LOG IN)")
					if(playerProperties.isLoggedIn) receiver.drawScoreFont(engine, playerID, 0, topY+RANKING_MAX+5,
						"F:SWITCH RANK SCREEN", COLOR.GREEN)
				}
			}
		} else if(engine.stat===GameEngine.Status.CUSTOM) {
			playerProperties.loginScreen.renderScreen(receiver, engine, playerID)
		} else {
			val strScore = "${engine.statistics.score}/${(engine.statistics.level+1)*5}"
			receiver.drawScoreNum(engine, playerID, 0, 3, strScore, 2f)
			receiver.drawScoreFont(engine, playerID, 0, 5, "Done", COLOR.BLUE)

			receiver.drawScoreFont(engine, playerID, 0, 7, "Level", COLOR.BLUE)
			receiver.drawScoreNum(engine, playerID, 5, 6, "${engine.statistics.level+1}", 2f)
			receiver.drawScoreFont(engine, playerID, 0, 9, "Time", COLOR.BLUE)
			receiver.drawScoreNum(engine, playerID, 0, 10, engine.statistics.time.toTimeStr, 2f)
			receiver.drawScoreFont(engine, playerID, 0, 12, "Objective", COLOR.GREEN)
			receiver.drawScoreNum(engine, playerID, 0, 13, "$missionProgress/$missionGoal", missionIsComplete)
			if(!missionIsComplete) drawMissionStrings(engine, playerID, currentMissionText, 0, 15, 1.0f)
			if(playerProperties.isLoggedIn||PLAYER_NAME.isNotEmpty()) {
				receiver.drawScoreFont(engine, playerID, 0, 20, "PLAYER", COLOR.BLUE)
				receiver.drawScoreFont(engine, playerID, 0, 21, if(owner.replayMode) PLAYER_NAME else playerProperties.nameDisplay,
					COLOR.WHITE, 2f)
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
				SpinWith -> if(engine.twist) {
					if(lines>=lineAmount&&shapeName===specificPieceName) {
						missionProgress++
						incremented = true
					}
				}
				Spin -> if(engine.twist) {
					if(lines>=lineAmount) {
						missionProgress++
						incremented = true
					}
				}
				ClearWith -> if(lines==lineAmount&&shapeName===specificPieceName) {
					missionProgress++
					incremented = true
				}
				Clear -> if(lines==lineAmount) {
					missionProgress++
					incremented = true
				}
				Combo -> {
					missionProgress = engine.combo
					if(missionProgress==missionGoal) incremented = true
				}
			}
		}

		// Combo
		if(missionProgress==missionGoal&&incremented) {
			pts = 1
			missionIsComplete = true
			engine.playSE("levelup")
		} else if(incremented) engine.playSE("b2b_start")

		// Add to score
		if(pts>0) {
			if(lines>=1) engine.statistics.scoreLine += pts else engine.statistics.scoreBonus += pts
		}

		// BGM fade-out effects and BGM changes
		if(tableBGMChange[bgmlv]!=-1) {
			if(engine.statistics.score>=tableBGMChange[bgmlv]-1) owner.bgmStatus.fadesw = true
			if(engine.statistics.score>=tableBGMChange[bgmlv]&&
				(engine.statistics.score<tableGameClearMissions[goaltype]||tableGameClearMissions[goaltype]<0)) {
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
		if(engine.statistics.score>=tableGameClearMissions[goaltype]&&tableGameClearMissions[goaltype]>=0) {
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
			engine.playSE("levelup_section")
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
			for(i in 1..fall)
				pCoordList!!.add(intArrayOf(engine.nowPieceX, engine.nowPieceY-i))

			for(i in 0 until cPiece.maxBlock) {
				if(!cPiece.big) {
					val x2:Int = baseX+cPiece.dataX[cPiece.direction][i]*16
					val y2:Int = baseY+cPiece.dataY[cPiece.direction][i]*16
					receiver.blockBreak(engine, x2, y2, cPiece.block[i])
				} else {
					val x2:Int = baseX+cPiece.dataX[cPiece.direction][i]*32
					val y2:Int = baseY+cPiece.dataY[cPiece.direction][i]*32
					receiver.blockBreak(engine, x2, y2, cPiece.block[i])
					receiver.blockBreak(engine, x2+16, y2, cPiece.block[i])
					receiver.blockBreak(engine, x2, y2+16, cPiece.block[i])
					receiver.blockBreak(engine, x2+16, y2+16, cPiece.block[i])
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
		engine.b2bEnable = false
		engine.twistEnable = true
		engine.useAllSpinBonus = true
		engine.twistAllowKick = true
		engine.twistEnableEZ = false
		setSpeed(engine)
		owner.bgmStatus.bgm = tableBGM[bgmlv]
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
				saveRanking(owner.modeConfig, engine.ruleOpt.strRuleName)
				owner.saveModeConfig()
			}
			if(rankingRankPlayer!=-1&&playerProperties.isLoggedIn) {
				saveRankingPlayer(playerProperties, engine.ruleOpt.strRuleName)
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
		for(i in 0 until RANKING_MAX) for(j in 0 until GAMETYPE_MAX) {
			prop.setProperty("missionmode.ranking.$ruleName.$j.score.$i", rankingScorePlayer[j][i])
			prop.setProperty("missionmode.ranking.$ruleName.$j.time.$i", rankingTimePlayer[j][i])
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
		drawResultStats(engine, playerID, receiver, 0, COLOR.BLUE,
			Statistic.SCORE, Statistic.LEVEL, Statistic.TIME, Statistic.LPM)
		drawResultRank(engine, playerID, receiver, 8, COLOR.BLUE, rankingRank)
		drawResultNetRank(engine, playerID, receiver, 10, COLOR.BLUE, netRankingRank[0])
		drawResultNetRankDaily(engine, playerID, receiver, 12, COLOR.BLUE, netRankingRank[1])
		if(netIsPB) {
			receiver.drawMenuFont(engine, playerID, 2, 21, "NEW PB", COLOR.ORANGE)
		}
		if(netIsNetPlay&&netReplaySendStatus==1) {
			receiver.drawMenuFont(engine, playerID, 0, 22, "SENDING...", COLOR.PINK)
		} else if(netIsNetPlay&&!netIsWatch&&netReplaySendStatus==2) {
			receiver.drawMenuFont(engine, playerID, 1, 22, "A: RETRY", COLOR.RED)
		}
	}
	// ------------------------------------------------------------------------------------------
	// CRAPPY MISSION GENERATION METHODS >_<
	// ------------------------------------------------------------------------------------------
	/*
     * I don't even know how these even are supposed to integrate into the rest of the mode but we'll get there later.
     */
	private fun generateNewMission(engine:GameEngine, lastMission:Boolean) {
		val classic:Boolean = engine.ruleOpt.strWallkick.contains("Classic")
			||engine.ruleOpt.strWallkick.contains("GBC")||
			engine.ruleOpt.strWallkick.contains("WallOnly")
			||!(engine.ruleOpt.lockresetMove||engine.ruleOpt.lockresetRotate)
		val missionTypeRd = missionRandomiser.nextInt(10)
		val missionType = when {
			missionTypeRd<4 -> Clear
			missionTypeRd<6 -> ClearWith
			missionTypeRd<8 -> Spin
			missionTypeRd<9 -> Combo
			else -> SpinWith
		}
		missionCategory = missionType
		missionIsComplete = false
		when(missionType) {
			ClearWith -> {
				val pieceNum = missionRandomiser.nextInt(VALID_LINECLEAR_PIECES.size)
				val pieceName = VALID_LINECLEAR_PIECES[pieceNum]
				val bodyColor = missionColor(engine, pieceName)
				val lineClear = missionRandomiser.nextInt(VALID_MAX_LINECLEARS[pieceNum])+1
				val amount = missionRandomiser.nextInt(MAX_MISSION_GOAL-lineClear/2)+1
				missionGoal = amount
				missionProgress = 0
				lineAmount = lineClear
				//isSpecific = true;
				//isSpinMission = false;
				specificPieceName = pieceName
				currentMissionText = generateSpecificLineMissionData(pieceName, bodyColor, lineClear, amount, lastMission)
			}
			Clear -> {
				val lineClear = missionRandomiser.nextInt(4)+1
				val amount = missionRandomiser.nextInt(MAX_MISSION_GOAL-lineClear/2)+1
				val bodyColor = if(lineClear>=4) missionColor(engine, "I") else MISSION_GENERIC
				missionGoal = amount
				missionProgress = 0
				lineAmount = lineClear
				//isSpecific = false;
				//isSpinMission = false;
				specificPieceName = ""
				currentMissionText = generateLineMissionData(bodyColor, lineClear, amount, lastMission)
			}
			SpinWith -> {
				val pieceNum = missionRandomiser.nextInt(VALID_SPIN_PIECES.size)
				val pieceName = VALID_SPIN_PIECES[pieceNum]
				val bodyColor = missionColor(engine, pieceName)
				val lineClear = missionRandomiser.nextInt(
					if(classic) VALID_CLASSIC_SPIN_MAX_LINES[pieceNum] else VALID_SPIN_MAX_LINES[pieceNum])
				val amount = missionRandomiser.nextInt(MAX_MISSION_GOAL-lineClear/2)+1
				missionGoal = amount
				missionProgress = 0
				lineAmount = lineClear
				//isSpecific = true;
				//isSpinMission = true;
				specificPieceName = pieceName
				currentMissionText = generateSpinMissionData(pieceName, bodyColor, lineClear, amount, lastMission)
			}
			Spin -> {
				val lineClear = missionRandomiser.nextInt(if(classic) 2 else 3)
				val amount = missionRandomiser.nextInt(MAX_MISSION_GOAL-lineClear/2)+1
				missionGoal = amount
				missionProgress = 0
				lineAmount = lineClear
				//isSpecific = false;
				//isSpinMission = true;
				specificPieceName = ""
				currentMissionText = generateNonSpecificSpinMissionData(lineClear, amount, lastMission)
			}
			Combo -> {
				val comboLimit = missionRandomiser.nextInt(MAX_MISSION_COMBO)+3
				missionGoal = comboLimit
				missionProgress = 0
				lineAmount = 0
				//isSpecific = false;
				//isSpinMission = false;
				specificPieceName = ""
				currentMissionText = generateComboMissionData(comboLimit, lastMission)
			}
		}
		engine.comboType = if(missionType==Combo) GameEngine.COMBO_TYPE_NORMAL else 0
		engine.combo = 0
	}

	private fun missionColor(engine:GameEngine, pieceName:String):COLOR = try {
		EventReceiver.getBlockColor(engine, Piece.Shape.valueOf(pieceName))
	} catch(e:Exception) {
		COLOR.WHITE
	}

	private fun generateSpinMissionData(pieceName:String, bodyColor:COLOR, spinLine:Int, amount:Int,
		lastMission:Boolean):LinkedHashMap<String, COLOR?> {
		val header = "Perform"
		val footer = if(lastMission) "TO WIN!" else "TO GO"
		val missionBody = "$amount"+"X "+pieceName+SPIN_MISSION_TABLE[spinLine]+if(amount>1) "S" else ""
		val result = LinkedHashMap<String, COLOR?>()
		result[header] = MISSION_NEUTRAL // Key is text to draw, value is color.
		result["[NEWLINE]"] = null// Tells the text-drawing method to move one line down.
		result[missionBody] = bodyColor
		result["[NEWLINE1]"] = null
		result["(or More Lines)"] = MISSION_NEUTRAL
		result["[NEWLINE2]"] = null
		result[footer] = MISSION_NEUTRAL
		return result
	}

	private fun generateNonSpecificSpinMissionData(spinLine:Int, amount:Int,
		lastMission:Boolean):LinkedHashMap<String, COLOR?> {
		val header = "Perform"
		val footer = if(lastMission) "TO WIN!" else "TO GO"
		val missionBody = "$amount"+"X "+NON_SPECIFIC_SPIN_MISSION_TABLE[spinLine]+if(amount>1) "S" else ""
		val result = LinkedHashMap<String, COLOR?>()
		result[header] = MISSION_NEUTRAL // Key is text to draw, value is color.
		result["[NEWLINE]"] = null // Tells the text-drawing method to move one line down.
		result[missionBody] = MISSION_GENERIC
		result["[NEWLINE1]"] = null
		result["(or More Lines)"] = MISSION_NEUTRAL
		result["[NEWLINE2]"] = null
		result[footer] = MISSION_NEUTRAL
		return result
	}

	private fun generateLineMissionData(bodyColor:COLOR, lineClear:Int, amount:Int,
		lastMission:Boolean):LinkedHashMap<String, COLOR?> {
		val header = "Perform"
		val footer = if(lastMission) "TO WIN!" else "TO GO"
		val missionBody = "$amount"+"X "+LINECLEAR_MISSION_TABLE[lineClear-1]+if(amount>1) if(lineClear!=4) "S" else "ES" else ""
		val result = LinkedHashMap<String, COLOR?>()
		result[header] = MISSION_NEUTRAL
		result["[NEWLINE]"] = null
		result[missionBody] = bodyColor
		result["[NEWLINE1]"] = null
		result[footer] = MISSION_NEUTRAL
		return result
	}

	private fun generateSpecificLineMissionData(pieceName:String, bodyColor:COLOR, lineClear:Int, amount:Int,
		lastMission:Boolean):LinkedHashMap<String, COLOR?> {
		val header = "Perform"
		val footer = if(lastMission) "TO WIN!" else "TO GO"
		val missionBody = "$amount"+"X "+LINECLEAR_MISSION_TABLE[lineClear-1]+if(amount>1) if(lineClear!=4) "S" else "ES" else ""
		val result = LinkedHashMap<String, COLOR?>()
		result[header] = MISSION_NEUTRAL
		result["[NEWLINE]"] = null
		result[missionBody] = MISSION_GENERIC
		result["[NEWLINE1]"] = null
		result["With "] = MISSION_NEUTRAL
		result["$pieceName-PIECES"] = bodyColor
		result["[NEWLINE2]"] = null
		result[footer] = MISSION_NEUTRAL
		return result
	}

	private fun generateComboMissionData(maxCombo:Int, lastMission:Boolean):LinkedHashMap<String, COLOR?> {
		val header = "Perform"
		val footer = if(lastMission) "TO WIN!" else "TO GO"
		val missionBody = "A "+maxCombo+"X "+"Combo"
		val result = LinkedHashMap<String, COLOR?>()
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
	private fun drawMissionStrings(engine:GameEngine, playerID:Int, missionData:LinkedHashMap<String, COLOR?>?,
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
		val tableBGMChange = intArrayOf(5, 10, 15, 20, 25, 37, 40, 50, -1)
		private val tableBGM =
			arrayOf(
				BGM.Puzzle(0), BGM.Generic(0), BGM.Generic(1), BGM.Puzzle(1), BGM.Puzzle(2),
				BGM.Generic(5), BGM.Generic(6), BGM.Generic(7), BGM.Generic(8))
		/**
		 * Line counts when game ending occurs
		 */
		private val tableGameClearMissions = intArrayOf(10, 25, 50, -1)
		private val GAMETYPE_MAX get() = tableGameClearMissions.size
		private val MISSION_GENERIC = COLOR.RAINBOW
		private val MISSION_NEUTRAL = COLOR.WHITE
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
			3, 3, 2, 3, 2
		)
		private val VALID_CLASSIC_SPIN_MAX_LINES = intArrayOf(
			2, 2, 1, 2, 1
		)
		private val VALID_MAX_LINECLEARS = intArrayOf(
			4, 3, 3, 2, 2, 2, 2
		)
		private const val MAX_MISSION_GOAL = 4
		private const val MAX_MISSION_COMBO = 5 // Add two. (3 because random...)
		// private static final int MAX_MISSION_TYPES = 5;
		private val headerColor = COLOR.GREEN

		private enum class MissionType {
			SpinWith, ClearWith, Spin, Clear, Combo
		}
	}
}