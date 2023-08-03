/*
 Copyright (c) 2021-2023,
 This library class was created by 0xFC963F18DC21 / Shots243
 It is part of an extension library for the game NullpoMino (copyright 2010-2023)

 Kotlin converted and modified by Venom=Nhelv

 Herewith shall the term "Library Creator" be given to 0xFC963F18DC21.
 Herewith shall the term "Game Creator" be given to the original creator of NullpoMino, NullNoname.

 THIS LIBRARY AND MODE PACK WAS NOT MADE IN ASSOCIATION WITH THE GAME CREATOR.

 Original Repository: https://github.com/Shots243/ModePile

 When using this library in a mode / library pack of your own, the following
 conditions must be satisfied:
     - This license must remain visible at the top of the document, unmodified.
     - You are allowed to use this library for any modding purpose.
         - If this is the case, the Library Creator must be credited somewhere.
             - Source comments only are fine, but in a README is recommended.
     - Modification of this library is allowed, but only in the condition that a
       pull request is made to merge the changes to the repository.

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

package zeroxfc.nullpo.custom.modes

import mu.nu.nullpo.game.component.BGMStatus.BGM
import mu.nu.nullpo.game.component.Controller
import mu.nu.nullpo.game.component.Piece
import mu.nu.nullpo.game.event.EventReceiver
import mu.nu.nullpo.game.event.EventReceiver.COLOR
import mu.nu.nullpo.game.event.ScoreEvent
import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.util.CustomProperties
import mu.nu.nullpo.util.GeneralUtil.toTimeStr
import zeroxfc.nullpo.custom.modes.MissionMode.Companion.MissionType.Clear
import zeroxfc.nullpo.custom.modes.MissionMode.Companion.MissionType.ClearWith
import zeroxfc.nullpo.custom.modes.MissionMode.Companion.MissionType.Combo
import zeroxfc.nullpo.custom.modes.MissionMode.Companion.MissionType.Spin
import zeroxfc.nullpo.custom.modes.MissionMode.Companion.MissionType.SpinWith
import zeroxfc.nullpo.custom.modes.MissionMode.Companion.MissionType.Split
import kotlin.random.Random

class MissionMode:MarathonModeBase() {
	private var scgettime = 0
	// Mission randomizer.
	private var missionRandomizer:Random = Random.Default
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
	private val rankingScore = List(GAMETYPE_MAX) {MutableList(RANKING_MAX) {0L}}
	/** Rankings' times */
	private val rankingTime = List(GAMETYPE_MAX) {MutableList(RANKING_MAX) {-1}}

	private val rankingScorePlayer = List(GAMETYPE_MAX) {MutableList(RANKING_MAX) {0L}}
	private val rankingTimePlayer = List(GAMETYPE_MAX) {MutableList(RANKING_MAX) {-1}}
	private var rankingRankPlayer = 0
	override val rankMap = rankMapOf(rankingScore.mapIndexed {a, x -> "$a.score" to x}
		+rankingTime.mapIndexed {a, x -> "$a.time" to x})

	override val rankPersMap = rankMapOf(rankingScorePlayer.mapIndexed {a, x -> "$a.score" to x}
		+rankingTimePlayer.mapIndexed {a, x -> "$a.time" to x})

	override val name:String
		get() = "Mission Rush"

	// Initialization
	override fun playerInit(engine:GameEngine) {
		owner = engine.owner

		currentMissionText = LinkedHashMap()
		missionCategory = null
		missionGoal = -1
		missionProgress = 0
		scgettime = 0
		lineAmount = 0
		// isSpinMission = false;
		// isSpecific = false;
		specificPieceName = ""
		rankingRank = -1
		rankingScore.forEach {it.fill(0)}
		rankingTime.forEach {it.fill(-1)}
		rankingScorePlayer.forEach {it.fill(0)}
		rankingTimePlayer.forEach {it.fill(0)}
		engine.playerProp.reset()
		showPlayerStats = false

		netPlayerInit(engine)
		if(!owner.replayMode) version = CURRENT_VERSION else {
			if(version==0&&owner.replayProp.getProperty("missionmode.endless", false)) goalType = 2

			// NET: Load name
			netPlayerName = engine.owner.replayProp.getProperty("${engine.playerID}.net.netPlayerName", "")
		}
		engine.owner.bgMan.bg = startLevel
		engine.frameColor = GameEngine.FRAME_COLOR_GREEN
		missionIsComplete = false
	}

	override fun onSetting(engine:GameEngine):Boolean {
		// NET: Net Ranking
		if(netIsNetRankingDisplayMode) {
			netOnUpdateNetPlayRanking(engine, goalType)
		} else if(!engine.owner.replayMode) {
			// Configuration changes
			val change = updateCursor(engine, 0)
			if(change!=0) {
				engine.playSE("change")
				when(engine.statc[2]) {
					0 -> {
						goalType += change
						if(goalType<0) goalType = GAMETYPE_MAX-1
						if(goalType>GAMETYPE_MAX-1) goalType = 0
						if(startLevel>(tableGameClearMissions[goalType]-1)/10&&tableGameClearMissions[goalType]>=0) {
							startLevel = (tableGameClearMissions[goalType]-1)/10
							engine.owner.bgMan.bg = startLevel
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
				// NET: Signal start of the game
				if(netIsNetPlay) netLobby!!.netPlayerClient!!.send("start1p\n")
				return false
			}

			// Cancel
			if(engine.ctrl.isPush(Controller.BUTTON_B)&&!netIsNetPlay) {
				engine.quitFlag = true
				engine.playerProp.reset()
			}

			// New acc
			if(engine.ctrl.isPush(Controller.BUTTON_E)&&engine.ai==null&&!netIsNetPlay) {
				engine.playerProp.reset()
				engine.playSE("decide")
				engine.stat = GameEngine.Status.CUSTOM
				engine.resetStatc()
				return true
			}

			// NET: Netplay Ranking
			if(engine.ctrl.isPush(Controller.BUTTON_D)&&netIsNetPlay&&startLevel==0&&!big&&engine.ai==null) {
				netEnterNetPlayRankingScreen(goalType)
			}
			engine.statc[3]++
		} else {
			engine.statc[3]++
			engine.statc[2] = -1
			return engine.statc[3]<60
		}
		return true
	}

	override fun onCustom(engine:GameEngine):Boolean {
		showPlayerStats = false
		engine.isInGame = true
		val s:Boolean = engine.playerProp.loginScreen.updateScreen(engine)
		if(engine.playerProp.isLoggedIn) {
			loadRankingPlayer(engine.playerProp)
			loadSetting(engine, engine.playerProp.propProfile)
		}
		if(engine.stat===GameEngine.Status.SETTING) engine.isInGame = false
		return s
	}

	override fun renderSetting(engine:GameEngine) {
		if(netIsNetRankingDisplayMode) {
			// NET: Netplay Ranking
			netOnRenderNetPlayRanking(engine, receiver)
		} else {
			drawMenu(
				engine,
				receiver,
				0,
				COLOR.BLUE,
				0,
				"GOAL" to if(tableGameClearMissions[goalType]<0) "ENDLESS" else "${tableGameClearMissions[goalType]} MISS."
			)
		}
	}

	override fun onReady(engine:GameEngine):Boolean {
		if(engine.statc[0]==0) {
			missionRandomizer = Random(engine.randSeed)
			generateNewMission(engine, false)
		}
		return false
	}

	override fun onLast(engine:GameEngine) {
		if(scgettime<120) scgettime++
		else if(missionIsComplete&&engine.timerActive) {
			generateNewMission(engine, engine.statistics.scoreBonus==tableGameClearMissions[goalType]-1)
			scgettime = 0
		}
		if(engine.stat===GameEngine.Status.SETTING||engine.stat===GameEngine.Status.RESULT&&!owner.replayMode||engine.stat===GameEngine.Status.CUSTOM) {
			// Show rank
			if(engine.ctrl.isPush(Controller.BUTTON_F)&&engine.playerProp.isLoggedIn&&engine.stat!==GameEngine.Status.CUSTOM) {
				showPlayerStats = !showPlayerStats
				engine.playSE("change")
			}
		}
		if(engine.quitFlag) {
			engine.playerProp.reset()
		}
	}

	override fun renderLast(engine:GameEngine) {
		if(owner.menuOnly) return
		receiver.drawScoreFont(engine, 0, 0, name, COLOR.GREEN)
		if(tableGameClearMissions[goalType]<0)
			receiver.drawScoreFont(engine, 0, 1, "(Endless run)", COLOR.GREEN)
		else receiver.drawScoreFont(engine, 0, 1, "(${tableGameClearMissions[goalType]} missions run)", COLOR.GREEN)
		if(engine.stat===GameEngine.Status.SETTING||engine.stat===GameEngine.Status.RESULT&&!owner.replayMode) {
			if(!owner.replayMode&&!big&&engine.ai==null) {
				val topY = if(receiver.nextDisplayType==2) 6 else 4
				receiver.drawScoreFont(engine, 3, topY-1, "SCORE TIME", COLOR.BLUE)
				if(showPlayerStats) {
					for(i in 0..<RANKING_MAX) {
						receiver.drawScoreGrade(engine, 0, topY+i, "%2d".format(i+1), COLOR.YELLOW)
						receiver.drawScoreNum(engine, 3, topY+i, "${rankingScorePlayer[goalType][i]}", i==rankingRankPlayer)
						receiver.drawScoreNum(engine, 9, topY+i, rankingTimePlayer[goalType][i].toTimeStr, i==rankingRankPlayer)
					}
					receiver.drawScoreFont(engine, 0, topY+RANKING_MAX+1, "PLAYER SCORES", COLOR.BLUE)
					receiver.drawScoreFont(
						engine, 0, topY+RANKING_MAX+2, engine.playerProp.nameDisplay, COLOR.WHITE,
						2f
					)
					receiver.drawScoreFont(engine, 0, topY+RANKING_MAX+5, "F:SWITCH RANK SCREEN", COLOR.GREEN)
				} else {
					for(i in 0..<RANKING_MAX) {
						receiver.drawScoreGrade(engine, 0, topY+i, "%2d".format(i+1), COLOR.YELLOW)
						receiver.drawScoreNum(engine, 3, topY+i, "${rankingScore[goalType][i]}", i==rankingRank)
						receiver.drawScoreNum(engine, 9, topY+i, rankingTime[goalType][i].toTimeStr, i==rankingRank)
					}
					receiver.drawScoreFont(engine, 0, topY+RANKING_MAX+1, "LOCAL SCORES", COLOR.BLUE)
					if(!engine.playerProp.isLoggedIn)
						receiver.drawScoreFont(engine, 0, topY+RANKING_MAX+2, "(NOT LOGGED IN)\n(E:LOG IN)")
					if(engine.playerProp.isLoggedIn)
						receiver.drawScoreFont(engine, 0, topY+RANKING_MAX+5, "F:SWITCH RANK SCREEN", COLOR.GREEN)
				}
			}
		} else if(engine.stat===GameEngine.Status.CUSTOM) {
			engine.playerProp.loginScreen.renderScreen(receiver, engine)
		} else {
			val strScore = "${engine.statistics.score}/${(engine.statistics.level+1)*5}"
			receiver.drawScoreNum(engine, 0, 3, strScore, 2f)
			receiver.drawScoreFont(engine, 0, 5, "Done", COLOR.BLUE)

			receiver.drawScoreFont(engine, 0, 7, "Level", COLOR.BLUE)
			receiver.drawScoreNum(engine, 5, 6, "${engine.statistics.level+1}", 2f)
			receiver.drawScoreFont(engine, 0, 9, "Time", COLOR.BLUE)
			receiver.drawScoreNum(engine, 0, 10, engine.statistics.time.toTimeStr, 2f)
			receiver.drawScoreFont(engine, 0, 12, "Objective", COLOR.GREEN)
			receiver.drawScoreNum(engine, 0, 13, "$missionProgress/$missionGoal", missionIsComplete)
			if(!missionIsComplete) drawMissionStrings(engine, currentMissionText, 0, 15, 1.0f)
			if(engine.playerProp.isLoggedIn||engine.playerName.isNotEmpty()) {
				receiver.drawScoreFont(engine, 0, 20, "PLAYER", COLOR.BLUE)
				receiver.drawScoreFont(
					engine, 0, 21, if(owner.replayMode) engine.playerName else engine.playerProp.nameDisplay,
					COLOR.WHITE,
					2f
				)
			}
		}

		// NET: Number of spectators
		netDrawSpectatorsCount(engine, 0, 18)
		// NET: All number of players
		if(engine.playerID==players-1) {
			netDrawAllPlayersCount()
			netDrawGameRate(engine)
		}
		// NET: Player name (It may also appear in offline replay)
		netDrawPlayerName(engine)
	}

	override fun calcScore(engine:GameEngine, ev:ScoreEvent):Int {
		// Line clear bonus
		var pts = 0
		var incremented = false
		val shapeName = ev.piece?.type?.name ?: ""
		if(!missionIsComplete) {
			incremented = when(missionCategory) {
				Clear -> ev.lines==lineAmount
				ClearWith -> ev.lines==lineAmount&&shapeName===specificPieceName
				Spin -> ev.twist&&ev.lines>=lineAmount
				SpinWith -> ev.twist&&ev.lines>=lineAmount&&shapeName===specificPieceName
				Split -> ev.split
				Combo -> ev.combo>missionProgress
				null -> true
			}
		}

		// Combo
		if(incremented) {
			missionProgress++
			scgettime = 0
			// Add to score
			engine.statistics.scoreBonus++
			// BGM fade-out effects and BGM changes
			if(tableBGMChange[bgmLv]!=-1) {
				if(engine.statistics.score>=tableBGMChange[bgmLv]-1&&
					(when(missionCategory) {
						Clear, ClearWith -> missionProgress>=missionGoal-maxOf(2, 6-lineAmount)
						Spin, SpinWith -> missionProgress>=missionGoal-2
						else -> true
					})) owner.musMan.fadeSW = true
				if(engine.statistics.score>=tableBGMChange[bgmLv]&&
					(engine.statistics.score<tableGameClearMissions[goalType]||tableGameClearMissions[goalType]<0)
				) {
					bgmLv++
					owner.musMan.bgm = tableBGM[bgmLv]
					owner.musMan.fadeSW = false
				}
			}

			if(missionProgress>=missionGoal) {
				missionIsComplete = true

				// Meter
				engine.meterValue = engine.statistics.score%10/9f
				engine.meterColor = GameEngine.METER_COLOR_LEVEL
				if(engine.statistics.score>=tableGameClearMissions[goalType]&&tableGameClearMissions[goalType]>=0) {
					// Ending
					engine.ending = 1
					engine.gameEnded()
				} else if(engine.statistics.score>=(engine.statistics.level+1)*5&&engine.statistics.level<19) {
					// Level up
					engine.statistics.level++
					owner.bgMan.nextBg = engine.statistics.level
					engine.playSE("levelup_section")
				} else engine.playSE("levelup")
			} else engine.playSE("gem")
			return 1
		}
		return 0
	}

	override fun afterSoftDropFall(engine:GameEngine, fall:Int) {
	}

	override fun onFirst(engine:GameEngine) {
	}

	override fun afterHardDropFall(engine:GameEngine, fall:Int) {
	}
	// ------------------------------------------------------------------------------------------
	// Ranking/Setting Saving/Loading
	// ------------------------------------------------------------------------------------------
	/*
     * Called for initialization during "Ready" screen
     */
	override fun startGame(engine:GameEngine) {
		engine.statistics.level = startLevel
		engine.statistics.levelDispAdd = 1
		engine.b2bEnable = false
		engine.twistEnable = true
		engine.useAllSpinBonus = true
		engine.twistAllowKick = true
		engine.twistEnableEZ = false
		setSpeed(engine)
		owner.musMan.bgm = tableBGM[bgmLv]
		if(netIsWatch) {
			owner.musMan.bgm = BGM.Silent
		}
	}
	/*
     * Called when saving replay
     */
	override fun saveReplay(engine:GameEngine, prop:CustomProperties):Boolean {
		saveSetting(engine, prop)

		// NET: Save name
		if(netPlayerName!=null&&netPlayerName!!.isNotEmpty()) {
			prop.setProperty("${engine.playerID}.net.netPlayerName", netPlayerName)
		}

		// Update rankings
		if(!owner.replayMode&&!big&&engine.ai==null) {
			return updateRanking(engine.statistics.score, engine.statistics.time, goalType, engine.playerProp.isLoggedIn)
		}
		return false
	}
	/**
	 * Update rankings
	 *
	 * @param sc   Score
	 * @param time Time
	 */
	private fun updateRanking(sc:Long, time:Int, type:Int, isLoggedIn:Boolean):Boolean {
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
		if(isLoggedIn) {
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
		} else rankingRankPlayer = -1
		return rankingRank!=-1||rankingRankPlayer!=-1
	}
	/**
	 * Calculate ranking position
	 *
	 * @param sc   Score
	 * @param time Time
	 * @return Position (-1 if unranked)
	 */
	private fun checkRanking(sc:Long, time:Int, type:Int):Int {
		for(i in 0..<RANKING_MAX) {
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
	private fun checkRankingPlayer(sc:Long, time:Int, type:Int):Int {
		for(i in 0..<RANKING_MAX) {
			if(sc>rankingScorePlayer[type][i]) {
				return i
			} else if(sc==rankingScorePlayer[type][i]&&time<rankingTimePlayer[type][i]) {
				return i
			}
		}
		return -1
	}

	override fun renderResult(engine:GameEngine) {
		drawResultStats(
			engine, receiver, 0, COLOR.BLUE, Statistic.SCORE,
			Statistic.LEVEL, Statistic.TIME, Statistic.LPM
		)
		drawResultRank(engine, receiver, 8, COLOR.BLUE, rankingRank)
		drawResultNetRank(engine, receiver, 10, COLOR.BLUE, netRankingRank[0])
		drawResultNetRankDaily(engine, receiver, 12, COLOR.BLUE, netRankingRank[1])
		if(netIsPB) {
			receiver.drawMenuFont(engine, 2, 21, "NEW PB", COLOR.ORANGE)
		}
		if(netIsNetPlay&&netReplaySendStatus==1) {
			receiver.drawMenuFont(engine, 0, 22, "SENDING...", COLOR.PINK)
		} else if(netIsNetPlay&&!netIsWatch&&netReplaySendStatus==2) {
			receiver.drawMenuFont(engine, 1, 22, "A: RETRY", COLOR.RED)
		}
	}
	// ------------------------------------------------------------------------------------------
// CRAPPY MISSION GENERATION METHODS >_<
// ------------------------------------------------------------------------------------------
//I don't even know how these even are supposed to integrate into the rest of the mode, but we'll get there later.
	private fun generateNewMission(engine:GameEngine, lastMission:Boolean) {
		val classic:Boolean = engine.ruleOpt.strWallkick.contains("Classic")
			||engine.ruleOpt.strWallkick.contains("GBC")||
			engine.ruleOpt.strWallkick.contains("WallOnly")
			||!(engine.ruleOpt.lockResetMove||engine.ruleOpt.lockResetSpin)
		val missionTypeRd = missionRandomizer.nextInt(13)
		val missionType = when {
			missionTypeRd<5 -> Clear
			missionTypeRd<8 -> ClearWith
			missionTypeRd<10 -> Split
			missionTypeRd<11 -> Spin
			missionTypeRd<12 -> SpinWith
			else -> Combo
		}
		missionCategory = missionType
		missionIsComplete = false
		when(missionType) {
			ClearWith -> {
				val pieceNum = missionRandomizer.nextInt(VALID_LINECLEAR_PIECES.size)
				val pieceName = VALID_LINECLEAR_PIECES[pieceNum]
				val bodyColor = missionColor(engine, pieceName)
				val lineClear = missionRandomizer.nextInt(VALID_MAX_LINECLEARS[pieceNum])+1
				val amount = missionRandomizer.nextInt(MAX_MISSION_GOAL-lineClear/2)+1
				missionGoal = amount
				missionProgress = 0
				lineAmount = lineClear
				//isSpecific = true;
				//isSpinMission = false;
				specificPieceName = pieceName
				currentMissionText = generateSpecificLineMissionData(pieceName, bodyColor, lineClear, amount, lastMission)
			}
			Clear -> {
				val lineClear = missionRandomizer.nextInt(4)+1
				val amount = missionRandomizer.nextInt(MAX_MISSION_GOAL-lineClear/2)+1
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
				val pieceNum = missionRandomizer.nextInt(VALID_SPIN_PIECES.size)
				val pieceName = VALID_SPIN_PIECES[pieceNum]
				val bodyColor = missionColor(engine, pieceName)
				val lineClear = missionRandomizer.nextInt(
					if(classic) VALID_CLASSIC_SPIN_MAX_LINES[pieceNum] else VALID_SPIN_MAX_LINES[pieceNum]
				)
				val amount = missionRandomizer.nextInt(MAX_MISSION_GOAL-lineClear/2)+1
				missionGoal = amount
				missionProgress = 0
				lineAmount = lineClear
				//isSpecific = true;
				//isSpinMission = true;
				specificPieceName = pieceName
				currentMissionText = generateSpinMissionData(pieceName, bodyColor, lineClear, amount, lastMission)
			}
			Spin -> {
				val lineClear = missionRandomizer.nextInt(if(classic) 2 else 3)
				val amount = missionRandomizer.nextInt(MAX_MISSION_GOAL-lineClear/2)+1
				missionGoal = amount
				missionProgress = 0
				lineAmount = lineClear
				//isSpecific = false;
				//isSpinMission = true;
				specificPieceName = ""
				currentMissionText = generateNonSpecificSpinMissionData(lineClear, amount, lastMission)
			}
			Split -> {
//				val lineClear = missionRandomizer.nextInt(if(classic) 2 else 3)
				val amount = missionRandomizer.nextInt(MAX_MISSION_GOAL-1)+1
				missionGoal = amount
				missionProgress = 0
				//isSpecific = false;
				//isSpinMission = true;
				specificPieceName = ""
				currentMissionText = generateSplitMissionData(amount, lastMission)
			}
			Combo -> {
				val comboLimit = missionRandomizer.nextInt(MAX_MISSION_COMBO)+3
				missionGoal = comboLimit
				missionProgress = 0
				lineAmount = 0
				//isSpecific = false;
				//isSpinMission = false;
				specificPieceName = ""
				currentMissionText = generateComboMissionData(comboLimit, lastMission)
			}
		}
		setSpeed(engine)
		engine.comboType = if(missionType==Combo) GameEngine.COMBO_TYPE_NORMAL else 0
	}
	/*override fun setSpeed(engine:GameEngine) {
		super.setSpeed(engine)
	}*/

	private fun missionColor(engine:GameEngine, pieceName:String):COLOR = try {
		EventReceiver.getBlockColor(engine, Piece.Shape.valueOf(pieceName))
	} catch(e:Exception) {
		COLOR.WHITE
	}

	private fun generateSpinMissionData(pieceName:String, bodyColor:COLOR, spinLine:Int, amount:Int, lastMission:Boolean)
		:LinkedHashMap<String, COLOR?> {
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

	private fun generateNonSpecificSpinMissionData(spinLine:Int, amount:Int, lastMission:Boolean)
		:LinkedHashMap<String, COLOR?> {
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

	private fun generateLineMissionData(
		bodyColor:COLOR, lineClear:Int, amount:Int, lastMission:Boolean
	):LinkedHashMap<String, COLOR?> {
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

	private fun generateSpecificLineMissionData(pieceName:String, bodyColor:COLOR, lineClear:Int, amount:Int, lastMission:Boolean)
		:LinkedHashMap<String, COLOR?> {
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
		val missionBody = "A $maxCombo REN Combo"
		val result = LinkedHashMap<String, COLOR?>()
		result[header] = MISSION_NEUTRAL
		result["[NEWLINE]"] = null
		result[missionBody] = MISSION_GENERIC
		result["[NEWLINE1]"] = null
		result[footer] = MISSION_NEUTRAL
		return result
	}

	private fun generateSplitMissionData(amount:Int, lastMission:Boolean)
		:LinkedHashMap<String, COLOR?> {
		val header = "Perform"
		val footer = if(lastMission) "TO WIN!" else "TO GO"
		val missionBody = "$amount"+"X Split Lines"
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
	 * @param missionData  LinkedHashMap containing mission text and color data.
	 * @param destinationX X of destination (uses drawScoreFont(...)).
	 * @param destinationY Y of destination (uses drawScoreFont(...)).
	 * @param scale        Text scale (0.5f, 1.0f, 2.0f).
	 */
	private fun drawMissionStrings(
		engine:GameEngine, missionData:LinkedHashMap<String, COLOR?>, destinationX:Int, destinationY:Int, scale:Float
	) {
		var counterX = 0
		var counterY = 0
		missionData.forEach {(str, col) ->
			if(col!=null) {
				receiver.drawScoreFont(engine, destinationX+counterX, destinationY+counterY, str, col, scale)
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
				BGM.Generic(5), BGM.Generic(6), BGM.Generic(7), BGM.Generic(8)
			)
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
			"-SPIN QUAD"
		)
		private val NON_SPECIFIC_SPIN_MISSION_TABLE = arrayOf(
			"SPIN",
			"SPIN SINGLE",
			"SPIN DOUBLE",
			"SPIN QUAD"
		)
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
			Clear, ClearWith, Spin, SpinWith, Split, Combo
			//HiSpeed
		}
	}
}
