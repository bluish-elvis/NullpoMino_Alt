/*
 Copyright (c) 2019-2024,
 This library class was created by 0xFC963F18DC21 / Shots243
 It is part of an extension library for the game NullpoMino (copyright 2010-2024)

 Converted to Kotlin and modified by Venom_Nhelv as bluish-elvis

 Herewith shall the term "Library Creator" be given to 0xFC963F18DC21.
 Herewith shall the term "Game Creator" be given to the original creator of NullpoMino, NullNoname.

 THIS LIBRARY AND MODE PACK WAS NOT MADE IN ASSOCIATION WITH THE GAME CREATOR.
 THIS KOTLIN VERSION WAS NOT MADE IN ASSOCIATION WITH THE LIBRARY CREATOR.

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

import mu.nu.nullpo.game.component.BGM
import mu.nu.nullpo.game.component.Controller
import mu.nu.nullpo.game.component.SpeedParam
import mu.nu.nullpo.game.event.EventReceiver.COLOR
import mu.nu.nullpo.game.event.ScoreEvent
import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.game.subsystem.mode.menu.BooleanMenuItem
import mu.nu.nullpo.game.subsystem.mode.menu.DelegateMenuItem
import mu.nu.nullpo.game.subsystem.mode.menu.IntegerMenuItem
import mu.nu.nullpo.game.subsystem.mode.menu.MenuList
import mu.nu.nullpo.game.subsystem.mode.menu.StringsMenuItem
import mu.nu.nullpo.util.CustomProperties
import mu.nu.nullpo.util.GeneralUtil.toInt
import mu.nu.nullpo.util.GeneralUtil.toTimeStr
import zeroxfc.nullpo.custom.libs.ProfileProperties

class RollTraining:MarathonModeBase() {
	companion object {
		/** Number of ranking types*/
		const val RANKING_TYPE = 8
		/** Number of game types*/
		const val GAMETYPE_MAX = 8
		private const val SPEED_TAP = 0
		private const val SPEED_TI = 1
		private const val SPEED_SETTING_COUNT = 2
		private val SPEED_SETTINGS = arrayOf(
			SpeedParam(), SpeedParam()
		)
		/** Note: M-roll uses the lock flash values. */
		private const val FADING_FRAMES = 300
		private val GRADE_INCREASES = arrayOf(
			doubleArrayOf(
				0.04, 0.08, 0.12, 0.26
			), doubleArrayOf(
				0.1, 0.2, 0.3, 1.0
			)
		)
		private val CLEAR_GRADE_BONUS = doubleArrayOf(
			0.5, 1.6
		)
		private val TIME_LIMITS = intArrayOf(
			3694, 3238
		)
		private val HEADER:COLOR = COLOR.RED

		init {
			// TAP settings
			SPEED_SETTINGS[0].gravity = -1
			SPEED_SETTINGS[0].are = 14
			SPEED_SETTINGS[0].areLine = 8
			SPEED_SETTINGS[0].lockDelay = 17
			SPEED_SETTINGS[0].lineDelay = 6
			SPEED_SETTINGS[0].das = 8

			// TI settings
			SPEED_SETTINGS[1].gravity = -1
			SPEED_SETTINGS[1].are = 6
			SPEED_SETTINGS[1].areLine = 6
			SPEED_SETTINGS[1].lockDelay = 15
			SPEED_SETTINGS[1].lineDelay = 6
			SPEED_SETTINGS[1].das = 8
		}
	}

	private val rankingGrade = List(RANKING_TYPE) {MutableList(rankingMax) {0}}
	private val rankingLines = List(RANKING_TYPE) {MutableList(rankingMax) {0}}
	private val rankingTime = List(RANKING_TYPE) {MutableList(rankingMax) {-1}}

	private val itemHide = BooleanMenuItem("useMRoll", "Stealth", COLOR.RED, false)
	private var useMRoll:Boolean by DelegateMenuItem(itemHide)
	override val itemMode:IntegerMenuItem = StringsMenuItem("usedSpeed", "TYPE", COLOR.BLUE, SPEED_TI, listOf("TAP", "TI"), true)
	private var usedSpeed:Int by DelegateMenuItem(itemMode)
	private val itemGoal = BooleanMenuItem("endless", "ENDLESS", COLOR.BLUE, false)
	private var endless:Boolean by DelegateMenuItem(itemGoal)

	private var tiGrade = 0.0
	private var tapGrade = 0.0
	private var lastGrade = 0
	private var timer = 0
	private val rankingGradePlayer = List(RANKING_TYPE) {MutableList(rankingMax) {0}}
	private val rankingLinesPlayer = List(RANKING_TYPE) {MutableList(rankingMax) {0}}
	private val rankingTimePlayer = List(RANKING_TYPE) {MutableList(rankingMax) {0}}
	private var rankingRankPlayer = 0
	private val rankIndex:Int
		get() {
			var raw = usedSpeed
			if(endless) raw += 2
			if(!useMRoll) raw += 4
			return raw
		}
	//      Mode name
	override val name:String = "ROLL TRAINING"
	override val menu = MenuList("rolltraining", itemMode, itemHide, itemGoal, itemLevel)
	//     * Initialization
	override val propRank
		get() = rankMapOf(rankingGrade.mapIndexed {a, x -> "$a.grade" to x}+
			rankingLines.mapIndexed {a, x -> "$a.lines" to x}+
			rankingTime.mapIndexed {a, x -> "$a.time" to x})

	override val propPB
		get() = rankMapOf(rankingGradePlayer.mapIndexed {a, x -> "$a.grade" to x}+
			rankingLinesPlayer.mapIndexed {a, x -> "$a.lines" to x}+
			rankingTimePlayer.mapIndexed {a, x -> "$a.time" to x})

	override fun playerInit(engine:GameEngine) {
		super.playerInit(engine)
		showPlayerStats = false
		engine.playerProp.reset()

		useMRoll = true
		usedSpeed = SPEED_TAP
		endless = false
		tiGrade = 0.0
		tapGrade = 0.0
		timer = 0
		lastGrade = 0
		rankingRank = -1
		rankingGrade.forEach {it.fill(0)}
		rankingLines.forEach {it.fill(0)}
		rankingTime.forEach {it.fill(-1)}
		rankingRankPlayer = -1
		rankingGradePlayer.forEach {it.fill(0)}
		rankingLinesPlayer.forEach {it.fill(0)}
		rankingTimePlayer.forEach {it.fill(0)}
		showPlayerStats = false
		big = false
		netPlayerInit(engine)
		if(!owner.replayMode) version = CURRENT_VERSION else {
			if(owner.replayProp.getProperty("rollTraining.endless", false)) goalType = 2

			// NET: Load name
			netPlayerName = engine.owner.replayProp.getProperty("${engine.playerID}.net.netPlayerName", "")
		}
		engine.owner.bgMan.bg = startLevel
		engine.frameColor = if(usedSpeed==SPEED_TAP) GameEngine.FRAME_COLOR_GRAY else GameEngine.FRAME_COLOR_BLUE
	}
	/**
	 * Set the gravity rate
	 *
	 * @param engine GameEngine
	 */
	override fun setSpeed(engine:GameEngine) {
		engine.speed.replace(SPEED_SETTINGS[usedSpeed])
	}
	/*
     * Called at settings screen
     */
	override fun onSetting(engine:GameEngine):Boolean {
		// NET: Net Ranking
		if(netIsNetRankingDisplayMode) {
			netOnUpdateNetPlayRanking(engine, goalType)
		} else if(!engine.owner.replayMode) {
			// Configuration changes
			val change = updateMenu(engine)
			if(change!=0) {
				engine.frameColor = if(usedSpeed==SPEED_TAP) GameEngine.FRAME_COLOR_GRAY else GameEngine.FRAME_COLOR_BLUE
				engine.owner.bgMan.bg = startLevel

				// NET: Signal options change
				if(netIsNetPlay&&netNumSpectators>0) {
					netSendOptions(engine)
				}
			}

			// Confirm
			if(engine.ctrl.isPush(Controller.BUTTON_A)&&engine.statc[3]>=5) {
				engine.playSE("decide")
				// NET: Signal start of the game
				if(netIsNetPlay) netLobby?.netPlayerClient?.send("start1p\n")
				return false
			}

			// Cancel
			if(engine.ctrl.isPush(Controller.BUTTON_B)&&!netIsNetPlay) {
				engine.quitFlag = true
				engine.playerProp.reset()
			}

			// New acc
			if(engine.ctrl.isPush(Controller.BUTTON_E)&&engine.ai==null) {
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

	/*
     * Called for initialization during "Ready" screen
     */
	override fun startGame(engine:GameEngine) {
		engine.statistics.level = 0
		engine.b2bEnable = true
		engine.comboType = GameEngine.COMBO_TYPE_NORMAL
		engine.big = big
		engine.twistAllowKick = true
		engine.twistEnable = true
		engine.useAllSpinBonus = true
		engine.twistEnableEZ = true
		setSpeed(engine)
		timer = TIME_LIMITS[usedSpeed]
		engine.blockHidden = if(useMRoll) engine.ruleOpt.lockFlash else FADING_FRAMES
		engine.blockHiddenAnim = !useMRoll
		engine.blockOutlineType = if(useMRoll) GameEngine.BLOCK_OUTLINE_NORMAL else GameEngine.BLOCK_OUTLINE_NONE
		owner.musMan.bgm = BGM.Ending(if(usedSpeed==SPEED_TAP) 1 else 2)
		if(netIsWatch) {
			owner.musMan.bgm = BGM.Silent
		}
	}

	override fun onCustom(engine:GameEngine):Boolean {
		showPlayerStats = false
		engine.isInGame = true
		val s = engine.playerProp.loginScreen.updateScreen(engine)
		if(engine.playerProp.isLoggedIn) {
			loadRankingPlayer(engine.playerProp)
			loadSetting(engine, engine.playerProp.propProfile)
			engine.owner.bgMan.bg = startLevel
		}
		if(engine.stat===GameEngine.Status.SETTING) engine.isInGame = false
		return s
	}
	/*
		 * Render score
		 */
	override fun renderLast(engine:GameEngine) {
		if(owner.menuOnly) return
		receiver.drawScoreFont(engine, 0, 0, name, COLOR.RED)
		val sb = StringBuilder("(")
		if(endless) sb.append("ENDLESS ")
		if(usedSpeed==SPEED_TAP) sb.append("TAP ") else sb.append("TI ")
		if(useMRoll) sb.append("M-") else sb.append("FADING ")
		sb.append("ROLL)")
		receiver.drawScoreFont(engine, 0, 1, "$sb", COLOR.RED)
		if(engine.stat===GameEngine.Status.SETTING||engine.stat===GameEngine.Status.RESULT&&!owner.replayMode) {
			if(!owner.replayMode&&!big&&engine.ai==null) {
				if(showPlayerStats) {
					val topY = if(receiver.nextDisplayType==2) 6 else 4
					receiver.drawScoreFont(engine, 3, topY-1, "GRADE  LINE TIME", COLOR.BLUE)
					for(i in 0..<rankingMax) {
						receiver.drawScoreFont(engine, 0, topY+i, "%2d".format(i+1), COLOR.YELLOW)
						val color = if(rankingRankPlayer==i) COLOR.RED else {
							if(usedSpeed==SPEED_TAP) {
								if(!useMRoll&&rankingTimePlayer[rankIndex][i]>=TIME_LIMITS[0]||useMRoll&&rankingLinesPlayer[rankIndex][i]>=32)
									COLOR.ORANGE else COLOR.GREEN
							} else if(rankingTimePlayer[rankIndex][i]>=TIME_LIMITS[1]) COLOR.ORANGE else COLOR.GREEN
						}
						val gText:String = if(usedSpeed==SPEED_TAP) {
							if(!useMRoll) "S9" else if(rankingGradePlayer[rankIndex][i]>=1.0) "GM" else "M"
						} else {
							"+%.2f".format(rankingGradePlayer[rankIndex][i])
						}
						receiver.drawScoreFont(engine, 3, topY+i, gText, color)
						receiver.drawScoreFont(engine, 10, topY+i, "${rankingLinesPlayer[rankIndex][i]}", i==rankingRankPlayer)
						receiver.drawScoreFont(engine, 15, topY+i, rankingTimePlayer[rankIndex][i].toTimeStr, i==rankingRankPlayer)
						receiver.drawScoreFont(engine, 0, topY+rankingMax+1, "PLAYER SCORES", COLOR.BLUE)
						receiver.drawScoreFont(engine, 0, topY+rankingMax+2, engine.playerProp.nameDisplay, COLOR.WHITE, 2f)
						receiver.drawScoreFont(engine, 0, topY+rankingMax+5, "F:SWITCH RANK SCREEN", COLOR.GREEN)
					}
				} else {
					val topY = if(receiver.nextDisplayType==2) 6 else 4
					receiver.drawScoreFont(engine, 3, topY-1, "GRADE  LINE TIME", COLOR.BLUE)
					for(i in 0..<rankingMax) {
						receiver.drawScoreFont(engine, 0, topY+i, "%2d".format(i+1), COLOR.YELLOW)
						val color = if(rankingRank==i) COLOR.RED else {
							if(usedSpeed==SPEED_TAP) {
								if(!useMRoll&&rankingTime[rankIndex][i]>=TIME_LIMITS[0]||useMRoll&&rankingLines[rankIndex][i]>=32) COLOR.ORANGE else COLOR.GREEN
							} else if(rankingTime[rankIndex][i]>=TIME_LIMITS[1]) COLOR.ORANGE else COLOR.GREEN
						}
						val gText:String = if(usedSpeed==SPEED_TAP) {
							if(!useMRoll) "S9" else if(rankingGrade[rankIndex][i]>=1.0) "GM" else "M"
						} else "+%.2f".format(rankingGrade[rankIndex][i])
						receiver.drawScoreGrade(engine, 3, topY+i, gText, color)
						receiver.drawScoreFont(engine, 10, topY+i, "${rankingLines[rankIndex][i]}", i==rankingRank)
						receiver.drawScoreFont(engine, 15, topY+i, rankingTime[rankIndex][i].toTimeStr, i==rankingRank)
						receiver.drawScoreFont(engine, 0, topY+rankingMax+1, "LOCAL SCORES", COLOR.BLUE)
						if(!engine.playerProp.isLoggedIn)
							receiver.drawScoreFont(engine, 0, topY+rankingMax+2, "(NOT LOGGED IN)\n(E:LOG IN)")
						if(engine.playerProp.isLoggedIn)
							receiver.drawScoreFont(engine, 0, topY+rankingMax+5, "F:SWITCH RANK SCREEN", COLOR.GREEN)
					}
				}
			}
		} else if(engine.stat===GameEngine.Status.CUSTOM) {
			engine.playerProp.loginScreen.renderScreen(receiver, engine)
		} else {
			receiver.drawScoreFont(engine, 0, 3, if(usedSpeed==SPEED_TAP) "GRADE" else "BONUS", COLOR.BLUE)
			val grade:String
			val gc:COLOR
			if(usedSpeed==SPEED_TAP) {
				grade = if(!useMRoll) "S9" else if(tapGrade>=1.0) "GM" else "M"
				gc =
					if(!useMRoll&&engine.statistics.time>=TIME_LIMITS[0]||useMRoll&&engine.statistics.lines>=32) COLOR.ORANGE else COLOR.GREEN
			} else {
				grade = "+%.2f".format(tiGrade)
				gc = if(engine.statistics.time>=TIME_LIMITS[1]) COLOR.ORANGE else COLOR.GREEN
			}
			receiver.drawScoreGrade(engine, 0, 4, grade, gc)
			receiver.drawScoreFont(engine, 0, 6, "LINE", COLOR.BLUE)
			receiver.drawScoreFont(engine, 0, 7, "${engine.statistics.lines}")
			receiver.drawScoreFont(engine, 0, 9, "TIME", COLOR.BLUE)
			receiver.drawScoreFont(engine, 0, 10, engine.statistics.time.toTimeStr)
			if(!endless) {
				receiver.drawScoreFont(engine, 0, 12, "REMAINING", COLOR.YELLOW)
				receiver.drawScoreFont(engine, 0, 13, maxOf(timer, 0).toTimeStr, (timer<=600&&timer/2%2==0))
			}
			if(engine.playerProp.isLoggedIn||engine.playerName.isNotEmpty()) {
				receiver.drawScoreFont(engine, 0, if(endless) 12 else 15, "PLAYER", COLOR.BLUE)
				receiver.drawScoreFont(
					engine, 0, if(endless) 13 else 16, if(owner.replayMode) engine.playerName else engine.playerProp.nameDisplay,
					COLOR.WHITE, 2f
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

	override fun onFirst(engine:GameEngine) {
		lastGrade = if(usedSpeed==SPEED_TAP) tapGrade.toInt() else tiGrade.toInt()
	}
	/*
		 * Called after every frame
		 */
	override fun onLast(engine:GameEngine) {
		super.onLast(engine)
		if(engine.timerActive) {
			if(timer==0) {
				if(usedSpeed==SPEED_TI) tiGrade += CLEAR_GRADE_BONUS[useMRoll.toInt()] else {
					tapGrade += 1.0
				}
				if(!endless) {
					engine.stat = GameEngine.Status.EXCELLENT
					engine.resetStatc()
					engine.resetFieldVisible()
					engine.gameEnded()
				}
			}
			if(timer>-1) --timer
			if(!endless&&timer<=600&&timer>0&&timer%60==0) engine.playSE("countdown")
		}
		val cg = if(usedSpeed==SPEED_TAP) tapGrade.toInt() else tiGrade.toInt()
		if(cg>lastGrade) engine.playSE("grade1")

		// Meter
		var lt = timer
		if(lt<0) lt = 0
		if(!endless) engine.meterValue = lt*1f/TIME_LIMITS[usedSpeed] else 1f
		engine.meterColor = GameEngine.METER_COLOR_LEVEL
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
	/*
		 * Calculate score
		 */
	override fun calcScore(engine:GameEngine, ev:ScoreEvent):Int {
		val li = ev.lines
		if(usedSpeed==SPEED_TI&&li>0) tiGrade += GRADE_INCREASES[useMRoll.toInt()][maxOf(0, li-1)]
		return 0
	}
	/*
		 * Soft drop
		 */
	override fun afterSoftDropFall(engine:GameEngine, fall:Int) {
		// NOTHING
	}
	/*
		 * Hard drop
		 */
	override fun afterHardDropFall(engine:GameEngine, fall:Int) {
		// NOTHING
	}
	/*
		 * Render results screen
		 */
	override fun renderResult(engine:GameEngine) {
		val grade = if(usedSpeed==SPEED_TAP)
			if(tapGrade>=1.0) "GM" else if(useMRoll) "M" else "S9"
		else "+%.2f".format(tiGrade)
		val gc =
			if(if(usedSpeed==SPEED_TAP) !useMRoll&&engine.statistics.time>=TIME_LIMITS[0]||useMRoll&&engine.statistics.lines>=32
				else engine.statistics.time>=TIME_LIMITS[1]
			) COLOR.ORANGE else COLOR.GREEN

		receiver.drawMenuFont(engine, 0, 0, if(usedSpeed==SPEED_TAP) "GRADE" else "BONUS", COLOR.BLUE)
		receiver.drawMenuFont(engine, 0, 1, "%10s".format(grade), gc)
		drawResultStats(
			engine, receiver, 2, COLOR.BLUE, Statistic.LINES,
			Statistic.TIME, Statistic.LPM
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
			return updateRanking(
				((if(usedSpeed==SPEED_TAP) tapGrade else tiGrade)*100).toInt(),
				engine.statistics.lines, engine.statistics.time, rankIndex, engine.playerProp.isLoggedIn
			)
		}
		return false
	}
	/**
	 * Load settings from [prop]
	 *
	 * @param prop Property file
	 */
	override fun loadSetting(engine: GameEngine, prop: CustomProperties, ruleName: String, playerID: Int) {
		startLevel = prop.getProperty("rollTraining.startLevel", 0)
		usedSpeed = prop.getProperty("rollTraining.usedSpeed", SPEED_TI)
		useMRoll = prop.getProperty("rollTraining.useMRoll", true)
		endless = prop.getProperty("rollTraining.endlessMode", false)
	}
	/**
	 * Save settings to [prop]
	 *
	 * @param prop Property file
	 */
	override fun saveSetting(engine:GameEngine, prop:CustomProperties, ruleName:String, playerID:Int) {
		prop.setProperty("rollTraining.startLevel", startLevel)
		prop.setProperty("rollTraining.usedSpeed", usedSpeed)
		prop.setProperty("rollTraining.useMRoll", useMRoll)
		prop.setProperty("rollTraining.endlessMode", endless)
	}
	/**
	 * Save settings to [prop]
	 *
	 * @param prop Property file
	 */
	private fun saveSettingPlayer(prop:ProfileProperties?) {
		prop?.setProperty("rollTraining.startLevel", startLevel)
		prop?.setProperty("rollTraining.usedSpeed", usedSpeed)
		prop?.setProperty("rollTraining.useMRoll", useMRoll)
		prop?.setProperty("rollTraining.endlessMode", endless)
	}
	/**
	 * Update rankings
	 *
	 * @param sc   Score
	 * @param li   Lines
	 * @param time Time
	 */
	private fun updateRanking(sc:Int, li:Int, time:Int, type:Int, isLoggedIn:Boolean):Boolean {
		rankingRank = checkRanking(sc, li, time, type)
		if(rankingRank!=-1) {
			// Shift down ranking entries
			for(i in rankingMax-1 downTo rankingRank+1) {
				rankingGrade[type][i] = rankingGrade[type][i-1]
				rankingLines[type][i] = rankingLines[type][i-1]
				rankingTime[type][i] = rankingTime[type][i-1]
			}

			// Add new data
			rankingGrade[type][rankingRank] = sc
			rankingLines[type][rankingRank] = li
			rankingTime[type][rankingRank] = time
		}
		if(isLoggedIn) {
			rankingRankPlayer = checkRankingPlayer(sc, li, time, type)
			if(rankingRankPlayer!=-1) {
				// Shift down ranking entries
				for(i in rankingMax-1 downTo rankingRankPlayer+1) {
					rankingGradePlayer[type][i] = rankingGradePlayer[type][i-1]
					rankingLinesPlayer[type][i] = rankingLinesPlayer[type][i-1]
					rankingTimePlayer[type][i] = rankingTimePlayer[type][i-1]
				}

				// Add new data
				rankingGradePlayer[type][rankingRankPlayer] = sc
				rankingLinesPlayer[type][rankingRankPlayer] = li
				rankingTimePlayer[type][rankingRankPlayer] = time
			}
		} else rankingRankPlayer = -1
		return rankingRank!=-1||rankingRankPlayer!=-1
	}

	private fun getClear(type:Int, time:Int, lines:Int):Int =
		if(!useMRoll||type and 1>0) if(time>=TIME_LIMITS[type and 1]) 1 else 0 else if(lines>=32) 1 else 0
	/**
	 * Calculate ranking position
	 *
	 * @param sc   Score
	 * @param li   Lines
	 * @param time Time
	 * @return Position (-1 if unranked)
	 */
	private fun checkRanking(sc:Int, li:Int, time:Int, type:Int):Int {
		for(i in 0..<rankingMax) {
			if(getClear(type, time, li)>getClear(type, rankingTime[type][i], rankingLines[type][i])) {
				return i
			} else if(getClear(type, time, li)==getClear(
					type, rankingTime[type][i],
					rankingLines[type][i]
				)&&sc>rankingGrade[type][i]
			) {
				return i
			} else if(getClear(type, time, li)==getClear(
					type, rankingTime[type][i],
					rankingLines[type][i]
				)&&sc==rankingGrade[type][i]&&time>rankingTime[type][i]
			) {
				return i
			} else if(getClear(type, time, li)==getClear(
					type, rankingTime[type][i],
					rankingLines[type][i]
				)&&sc==rankingGrade[type][i]&&time==rankingTime[type][i]&&li>rankingLines[type][i]
			) {
				return i
			}
		}
		return -1
	}
	/**
	 * Calculate ranking position
	 *
	 * @param sc   Score
	 * @param li   Lines
	 * @param time Time
	 * @return Position (-1 if unranked)
	 */
	private fun checkRankingPlayer(sc:Int, li:Int, time:Int, type:Int):Int {
		for(i in 0..<rankingMax) {
			if(getClear(type, time, li)>getClear(type, rankingTimePlayer[type][i], rankingLinesPlayer[type][i])) {
				return i
			} else if(getClear(type, time, li)==getClear(
					type, rankingTimePlayer[type][i],
					rankingLinesPlayer[type][i]
				)&&sc>rankingGradePlayer[type][i]
			) {
				return i
			} else if(getClear(type, time, li)==getClear(
					type, rankingTimePlayer[type][i],
					rankingLinesPlayer[type][i]
				)&&sc==rankingGradePlayer[type][i]&&time>rankingTimePlayer[type][i]
			) {
				return i
			} else if(getClear(type, time, li)==getClear(
					type, rankingTimePlayer[type][i],
					rankingLinesPlayer[type][i]
				)&&sc==rankingGradePlayer[type][i]&&time==rankingTimePlayer[type][i]&&li>rankingLinesPlayer[type][i]
			) {
				return i
			}
		}
		return -1
	}
}
