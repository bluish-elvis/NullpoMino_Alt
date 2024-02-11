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

import mu.nu.nullpo.game.component.BGMStatus
import mu.nu.nullpo.game.component.Controller
import mu.nu.nullpo.game.event.EventReceiver.COLOR
import mu.nu.nullpo.game.event.ScoreEvent
import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.gui.common.fx.particles.BlockParticle
import mu.nu.nullpo.util.CustomProperties
import mu.nu.nullpo.util.GeneralUtil.toTimeStr
import zeroxfc.nullpo.custom.libs.FlyInOutText
import kotlin.random.Random

class ScoreTrial:MarathonModeBase() {
	// In-game Timer
	private var mainTimer = 0
	// Ranking stuff
	private val rankingScore = List(MAX_DIFFICULTIES) {MutableList(RANKING_MAX) {0L}}
	private val rankingLines = List(MAX_DIFFICULTIES) {MutableList(RANKING_MAX) {0}}
	private val rankingTime = List(MAX_DIFFICULTIES) {MutableList(RANKING_MAX) {-1}}
	// Lives added/removed
	private var lifeOffset = 0
	// Score before increase;
	private var scoreBeforeIncrease = 0L
	// ANIMATION TYPE
	private var lineClearAnimType = 0
	// Difficulty selector
	private var difficultySelected = 0
	// should use timer?
	private var shouldUseTimer = false
	// Local randomizer
	private var localRandom:Random = Random.Default
	// Goal line
	private var goalLine = 0
	// Lives started with
	private var livesStartedWith = 0
	// Particle stuff
//	private var blockParticles:Mapper? = null
	// Flying congratulations text
	private var congratulationsText:FlyInOutText? = null
	// Combo Text
	private var comboTextAward:FlyInOutText? = null
	private var comboTextNumber:FlyInOutText? = null
	/**
	 * The good hard drop effect
	 */
	private var pCoordList:ArrayList<IntArray>? = null
	private var o = false
	private var l = 0
	private var rankingRankPlayer = 0
	private val rankingScorePlayer = List(MAX_DIFFICULTIES) {MutableList(RANKING_MAX) {0L}}
	private val rankingLinesPlayer = List(MAX_DIFFICULTIES) {MutableList(RANKING_MAX) {0}}
	private val rankingTimePlayer = List(MAX_DIFFICULTIES) {MutableList(RANKING_MAX) {0}}
	// Mode name
	override val name:String
		get() = "SCORE TRIAL"
	/*
     * Initialization
     */
	override fun playerInit(engine:GameEngine) {
		super.playerInit(engine)
		lastScore = 0
		bgmLv = 0
		lifeOffset = 0
		o = false
		l = 0
		scoreBeforeIncrease = 0
		lineClearAnimType = 0
		difficultySelected = 0
		shouldUseTimer = false
		goalLine = 0
		pCoordList = ArrayList()
		congratulationsText = null
		comboTextAward = null
		comboTextNumber = null
		mainTimer = 0
		livesStartedWith = 0
		rankingRank = -1
		rankingScore.forEach {it.fill(0)}
		rankingLines.forEach {it.fill(0)}
		rankingTime.forEach {it.fill(-1)}

		engine.playerProp.reset()
		showPlayerStats = false

		rankingRankPlayer = -1
		rankingScorePlayer.forEach {it.fill(0)}
		rankingLinesPlayer.forEach {it.fill(0)}
		rankingTimePlayer.forEach {it.fill(0)}

		netPlayerInit(engine)
		if(!owner.replayMode) {
			version = CURRENT_VERSION
		} else {
			if(version==0&&owner.replayProp.getProperty("scoretrial.endless", false)) goalType = 2

			// NET: Load name
			netPlayerName = engine.owner.replayProp.getProperty("${engine.playerID}.net.netPlayerName", "")
		}
		engine.owner.bgMan.bg = startLevel
		engine.frameColor = GameEngine.FRAME_COLOR_GRAY
		engine.twistEnable = false
		engine.comboType = GameEngine.COMBO_TYPE_NORMAL
		engine.statistics.levelDispAdd = 0
		engine.speed.das = 8
		engine.speed.denominator = 256
		engine.speed.are = 15
		engine.speed.areLine = 15
		engine.speed.lineDelay = 0
	}
	/**
	 * Set the gravity rate
	 *
	 * @param engine GameEngine
	 */
	override fun setSpeed(engine:GameEngine) {
		var lv:Int = engine.statistics.level
		if(lv<0) lv = 0
		if(lv>=GRAVITY_TABLE.size) lv = GRAVITY_TABLE.size-1
		engine.speed.gravity = GRAVITY_TABLE[lv]
		if(engine.speed.gravity==-1) engine.speed.das = 6
		if(engine.statistics.level>=60) {
			var ldAreIndex = 0
			while(engine.statistics.level>=LEVEL_ARE_LOCK_CHANGE[ldAreIndex]) ldAreIndex++
			engine.speed.are = ARE_TABLE[ldAreIndex]
			engine.speed.areLine = ARE_TABLE[ldAreIndex]
			engine.speed.lockDelay = LOCK_TABLE[ldAreIndex]
		}
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
			val change = updateCursor(engine, 3)
			if(change!=0) {
				engine.playSE("change")
				when(engine.statc[2]) {
					0 -> {
						difficultySelected += change
						if(difficultySelected>MAX_DIFFICULTIES-1) difficultySelected = 0
						if(difficultySelected<0) difficultySelected = MAX_DIFFICULTIES-1
					}
					1 -> {
						// if ((STARTING_LIVES + lifeOffset + change) >= 0 && (STARTING_LIVES + lifeOffset + change) <= 9) lifeOffset += change;
						lifeOffset += change
						if(STARTING_LIVES+lifeOffset<0) lifeOffset = 5
						if(STARTING_LIVES+lifeOffset>9) lifeOffset = -4
					}
					2 -> big = !big
					3 -> {
						lineClearAnimType += change
						if(lineClearAnimType>BlockParticle.ANIMATION_TYPES-1) lineClearAnimType = 0
						if(lineClearAnimType<0) lineClearAnimType = BlockParticle.ANIMATION_TYPES-1
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
	/*
		 * Render the settings screen
		 */
	override fun renderSetting(engine:GameEngine) {
		val lc = when(lineClearAnimType) {
			0 -> "DTET"
			1 -> "TGM"
			else -> ""
		}
		if(netIsNetRankingDisplayMode) {
			// NET: Netplay Ranking
			netOnRenderNetPlayRanking(engine, receiver)
		} else {
			drawMenu(engine, receiver, 0, COLOR.RED, 0, "DIFFICULTY" to DIFFICULTY_NAMES[difficultySelected])
			drawMenu(engine, receiver, 2, COLOR.GREEN, 1, "LIVES" to STARTING_LIVES+lifeOffset+1)
			drawMenu(engine, receiver, 4, COLOR.BLUE, 2, "BIG" to big, "LINE ANIM." to lc)
		}
	}

	override fun onReady(engine:GameEngine):Boolean {
		mainTimer = STARTING_TIMER
		goalLine = 8
		when(difficultySelected) {
			0 -> mainTimer = NORMAL_30_TIMER
			1 -> mainTimer = HARD_50_TIMER
			2 -> mainTimer = STARTING_TIMER
		}
		livesStartedWith = STARTING_LIVES+lifeOffset
		engine.lives = STARTING_LIVES+lifeOffset
		return false
	}
	/*
		 * Called for initialization during "Ready" screen
		 */
	override fun startGame(engine:GameEngine) {
		engine.statistics.level = startLevel
		engine.statistics.levelDispAdd = 0
		when(difficultySelected) {
			0 -> {
				engine.ruleOpt.areCancelHold = false
				engine.ruleOpt.areCancelMove = false
				engine.ruleOpt.areCancelSpin = false
				engine.ruleOpt.harddropEnable = false
				engine.ruleOpt.softdropSurfaceLock = true
				engine.ruleOpt.softdropEnable = true
				shouldUseTimer = false
				engine.speed.lineDelay = 8
			}
			1 -> {
				engine.ruleOpt.areCancelHold = false
				engine.ruleOpt.areCancelMove = false
				engine.ruleOpt.areCancelSpin = false
				engine.ruleOpt.harddropEnable = true
				engine.ruleOpt.softdropSurfaceLock = true
				engine.ruleOpt.softdropEnable = true
				shouldUseTimer = false
				engine.speed.lineDelay = 8
			}
			2 -> {
				engine.ruleOpt.areCancelHold = true
				engine.ruleOpt.areCancelMove = true
				engine.ruleOpt.areCancelSpin = true
				engine.ruleOpt.harddropEnable = true
				engine.ruleOpt.softdropSurfaceLock = true
				engine.ruleOpt.softdropEnable = true
				shouldUseTimer = true
				engine.speed.lineDelay = 0
			}
		}
		engine.ruleOpt.spinDoubleKey = true
		engine.ghost = false
		engine.big = big
		scoreBeforeIncrease = 0
		engine.speed.das = 8
		engine.speed.denominator = 256
		engine.speed.are = 15
		engine.speed.areLine = 15
		engine.speed.lockDelay = 30
		localRandom = Random(engine.randSeed)
		l = STARTING_LIVES+lifeOffset+1
		o = false
		setSpeed(engine)
		owner.musMan.bgm = tableBGM[difficultySelected][0]
		owner.musMan.fadeSW = false
		if(netIsWatch) {
			owner.musMan.bgm = BGMStatus.BGM.Silent
		}
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
	// Render score
	override fun renderLast(engine:GameEngine) {
		if(owner.menuOnly) return
		receiver.drawScoreFont(engine, 0, 0, name, COLOR.GREEN)
		receiver.drawScoreFont(engine, 0, 1, "("+DIFFICULTY_NAMES[difficultySelected]+" TIER)", COLOR.GREEN)
		if(engine.stat===GameEngine.Status.SETTING||engine.stat===GameEngine.Status.RESULT&&!owner.replayMode) {
			if(!owner.replayMode&&!big&&engine.ai==null&&lifeOffset==0) {
				val topY = if(receiver.nextDisplayType==2) 6 else 4
				receiver.drawScoreFont(engine, 3, topY-1, "SCORE  LINE TIME", COLOR.BLUE)
				if(showPlayerStats) {
					for(i in 0..<RANKING_MAX) {
						receiver.drawScoreFont(engine, 0, topY+i, "%2d".format(i+1), COLOR.YELLOW)
						val s = "${rankingScorePlayer[difficultySelected][i]}"
						val isLong = s.length>6&&receiver.nextDisplayType!=2
						receiver.drawScoreFont(
							engine, if(isLong) 6 else 3, if(isLong) (topY+i)*2 else topY+i, s, i==rankingRankPlayer,
							if(isLong) .5f else 1f
						)
						receiver.drawScoreFont(engine, 10, topY+i, "${rankingLinesPlayer[difficultySelected][i]}", i==rankingRankPlayer)
						receiver.drawScoreFont(engine, 15, topY+i, rankingTimePlayer[difficultySelected][i].toTimeStr, i==rankingRankPlayer)
					}
					receiver.drawScoreFont(engine, 0, topY+RANKING_MAX+1, "PLAYER SCORES", COLOR.BLUE)
					receiver.drawScoreFont(
						engine, 0, topY+RANKING_MAX+2, engine.playerProp.nameDisplay, COLOR.WHITE,
						2f
					)
					receiver.drawScoreFont(engine, 0, topY+RANKING_MAX+5, "F:SWITCH RANK SCREEN", COLOR.GREEN)
				} else {
					for(i in 0..<RANKING_MAX) {
						receiver.drawScoreFont(engine, 0, topY+i, "%2d".format(i+1), COLOR.YELLOW)
						val s = "${rankingScore[difficultySelected][i]}"
						val isLong = s.length>6&&receiver.nextDisplayType!=2
						receiver.drawScoreFont(
							engine, if(isLong) 6 else 3, if(isLong) (topY+i)*2 else topY+i, s, i==rankingRank,
							if(isLong) .5f else 1f
						)
						receiver.drawScoreFont(engine, 10, topY+i, "${rankingLines[difficultySelected][i]}", i==rankingRank)
						receiver.drawScoreFont(engine, 15, topY+i, rankingTime[difficultySelected][i].toTimeStr, i==rankingRank)
					}
					receiver.drawScoreFont(engine, 0, topY+RANKING_MAX+1, "LOCAL SCORES", COLOR.BLUE)
					if(!engine.playerProp.isLoggedIn) receiver.drawScoreFont(
						engine, 0, topY+RANKING_MAX+2, "(NOT LOGGED IN)\n(E:LOG IN)"
					)
					if(engine.playerProp.isLoggedIn) receiver.drawScoreFont(
						engine, 0, topY+RANKING_MAX+5, "F:SWITCH RANK SCREEN",
						COLOR.GREEN
					)
				}
			}
		} else if(engine.stat===GameEngine.Status.CUSTOM) {
			engine.playerProp.loginScreen.renderScreen(receiver, engine)
		} else {
			receiver.drawScoreFont(engine, 0, 3, "LINE", COLOR.BLUE)

			receiver.drawScoreNum(engine, 5, 2, "%3d/%3d".format(engine.statistics.lines, goalLine), 2f)
			val scget:Boolean = scDisp<engine.statistics.score
			receiver.drawScoreFont(engine, 0, 4, "Score", COLOR.BLUE)
			receiver.drawScoreNum(engine, 5, 4, "+$lastScore")

			receiver.drawScoreNum(engine, 0, 5, "$scDisp", scget, 2f)
			if(!o) {
				l = engine.lives+1
			}
			if(engine.stat===GameEngine.Status.GAMEOVER&&!o&&engine.lives==0) l = 0
			receiver.drawScoreFont(engine, 0, 9, "LIVES", COLOR.BLUE)
			receiver.drawScoreFont(engine, 0, 10, "$l", (l<=1))
			receiver.drawScoreFont(engine, 0, 12, "LEVEL", COLOR.BLUE)
			receiver.drawScoreFont(engine, 0, 13, "${engine.statistics.level}")
			receiver.drawScoreFont(engine, 0, 15, "TIME", COLOR.BLUE)
			receiver.drawScoreFont(engine, 0, 16, engine.statistics.time.toTimeStr)
			if(engine.playerProp.isLoggedIn||engine.playerName.isNotEmpty()) {
				receiver.drawScoreFont(engine, 11, 6, "PLAYER", COLOR.BLUE)
				receiver.drawScoreFont(
					engine, 11, 7, if(owner.replayMode) engine.playerName else engine.playerProp.nameDisplay,
					COLOR.WHITE,
					2f
				)
			}

			if(shouldUseTimer) {
				receiver.drawMenuFont(engine, 0, 21, "TIME LIMIT", COLOR.RED)
				receiver.drawMenuFont(engine, 1, 22, mainTimer.toTimeStr, mainTimer<=600&&mainTimer/2%2==0)
			}
			congratulationsText?.draw(receiver)
			comboTextAward?.draw(receiver)
			comboTextNumber?.draw(receiver)
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
	/*
		 * Called after every frame
		 */
	override fun onLast(engine:GameEngine) {
		super.onLast(engine)
		if(engine.gameStarted&&engine.ending==0) {
			/*if(engine.stat===GameEngine.Status.ARE&&difficultySelected==2) {
				engine.dasCount = engine.speed.das
			}*/
			if(shouldUseTimer&&engine.stat!==GameEngine.Status.GAMEOVER) {
				mainTimer--
				if(mainTimer<=600&&mainTimer%60==0) {
					receiver.playSE("countdown")
				}
			}

			// Meter - use as timer.
			val timerMax = when(difficultySelected) {
				0 -> NORMAL_30_TIMER
				1 -> HARD_50_TIMER
				else -> TIMER_MAX
			}
			engine.meterValue = mainTimer/timerMax.toFloat()
			engine.meterColor = GameEngine.METER_COLOR_LIMIT
			if(mainTimer<=0) {
				if(engine.statistics.level<199&&difficultySelected==2) {
					engine.lives = 0
					engine.stat = GameEngine.Status.GAMEOVER
					engine.resetStatc()
					engine.gameEnded()
				} else {
					// Ending
					val baseBonus = (engine.statistics.score*((engine.lives+1).toFloat()/(livesStartedWith+1))).toInt()
					engine.statistics.scoreBonus += baseBonus
					lastScore = baseBonus
					o = true
					l = engine.lives+1
					if(engine.lives==STARTING_LIVES+lifeOffset) {
						val destinationX = 192
						val destinationY = 224
						congratulationsText = FlyInOutText(
							"PERFECT!", destinationX, destinationY, 15, 90, 15,
							arrayOf(COLOR.GREEN, COLOR.CYAN), 2.0f, engine.randSeed+engine.statistics.time,
							true
						)
						engine.playSE("cool")
					}
					engine.lives = 0
					engine.ending = 1
					engine.resetStatc()
					engine.gameEnded()
					engine.stat = GameEngine.Status.ENDINGSTART
				}
			}
		}
		if(engine.gameStarted) {
//			blockParticles?.update()
			congratulationsText?.update()
			if(congratulationsText?.shouldPurge()==true)
				congratulationsText = null
			comboTextAward?.update()
			if(comboTextAward?.shouldPurge()==true) {
				comboTextAward = null
			}
			comboTextNumber?.update()
			if(comboTextNumber?.shouldPurge()==true) comboTextNumber = null
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

	override fun onGameOver(engine:GameEngine):Boolean {
		if(engine.lives<=0) {
			shouldUseTimer = false
		}
		return false
	}

	/*
		 * Calculate score - PAIN
		 */
	override fun calcScore(engine:GameEngine, ev:ScoreEvent):Int {
		// Line clear bonus
		val li = ev.lines
		if(li>0) {
			var pts = LINECLEAR_SCORES[li-1]
			pts *= ev.combo
			pts *= engine.statistics.level+1
			if(ev.combo>=2) {
				val destinationX:Int = receiver.scoreX(engine)
				val destinationY:Int = receiver.scoreY(engine,if(engine.displaySize==0) 20 else 40)
				val colors = when {
					ev.combo>10 -> arrayOf(COLOR.YELLOW, COLOR.ORANGE, COLOR.RED)
					ev.combo>=8 -> arrayOf(COLOR.CYAN, COLOR.BLUE, COLOR.COBALT)
					ev.combo>=4 -> arrayOf(COLOR.BLUE, COLOR.COBALT)
					else -> arrayOf(COLOR.COBALT)
				}
				comboTextNumber = FlyInOutText(
					"${(engine.combo)} COMBO", destinationX, destinationY, 15, 60, 15, colors,
					1.0f, engine.randSeed+engine.statistics.time, engine.combo>=8
				)
				if(ev.combo==4) comboTextAward = FlyInOutText(
					"GOOD!", destinationX,
					destinationY+if(engine.displaySize==0) 16 else 32, 15, 60, 15, arrayOf(COLOR.YELLOW), 1.0f,
					engine.randSeed+engine.statistics.time, engine.combo>=8
				)
				if(ev.combo==8) comboTextAward = FlyInOutText(
					"AWESOME!", destinationX,
					destinationY+if(engine.displaySize==0) 16 else 32, 15, 60, 15, arrayOf(COLOR.GREEN), 1.0f,
					engine.randSeed+engine.statistics.time, engine.combo>=8
				)
				if(ev.combo==11) comboTextAward = FlyInOutText(
					"UNREAL!", destinationX,
					destinationY+if(engine.displaySize==0) 16 else 32, 15, 60, 15,
					arrayOf(COLOR.ORANGE, COLOR.RED), 1.0f, engine.randSeed+engine.statistics.time,
					engine.combo>=8
				)
			}

			// Add to score
			if(pts>0) {
				scoreBeforeIncrease = engine.statistics.score
				lastScore = pts
				engine.statistics.scoreLine += pts

			}
		}
		if(engine.statistics.level in 50..199&&li>0&&difficultySelected==2) {
			// Level up
			engine.statistics.level++

			// owner.backgroundStatus.fadeSW = true;
			// owner.backgroundStatus.fadeCount = 0;
			// owner.backgroundStatus.nextBg = engine.statistics.level / 5;
			if(engine.statistics.level==200) {
				owner.bgMan.bg = 19
				engine.playSE("endingstart")
				val destinationX:Int = receiver.scoreX(engine)
				val destinationY:Int = receiver.scoreY(engine,if(engine.displaySize==0) 18 else 36)
				congratulationsText = FlyInOutText(
					"WELL DONE!", destinationX, destinationY, 30, 120, 30,
					arrayOf(COLOR.YELLOW, COLOR.ORANGE, COLOR.RED), 1.0f,
					engine.randSeed+engine.statistics.time, false
				)
			}
			mainTimer += LEVEL_TIMEBONUS
			if(mainTimer>TIMER_MAX) mainTimer = TIMER_MAX
			setSpeed(engine)
			engine.playSE("levelup")
		} else if(engine.statistics.lines>=goalLine) {
			var lMax = 50
			if(difficultySelected==0) lMax = 30
			if(engine.statistics.level<lMax) {
				// Level up
				engine.statistics.level++
				when(difficultySelected) {
					0 -> goalLine += 8+engine.statistics.level/10*2
					1 -> goalLine += 8+engine.statistics.level/10
					2 -> goalLine += 8
				}

				// owner.backgroundStatus.fadeSW = true;
				// owner.backgroundStatus.fadeCount = 0;
				owner.bgMan.bg = engine.statistics.level/5
				if(engine.statistics.level==lMax&&difficultySelected!=2) {
					owner.bgMan.bg = 19
					shouldUseTimer = true
					engine.playSE("endingstart")
					val destinationX:Int = receiver.scoreX(engine)
					val destinationY:Int = receiver.scoreY(engine,if(engine.displaySize==0) 18 else 36)
					congratulationsText = FlyInOutText(
						"WELL DONE!", destinationX, destinationY, 30, 120, 30,
						arrayOf(COLOR.YELLOW, COLOR.ORANGE, COLOR.RED), 1.0f,
						engine.randSeed+engine.statistics.time, false
					)
				}
				if(difficultySelected==2) {
					mainTimer += LEVEL_TIMEBONUS
					if(mainTimer>TIMER_MAX) mainTimer = TIMER_MAX
				}
				setSpeed(engine)
				engine.playSE("levelup")
			}
		}
		return 0
	}
	/*
		 * Soft drop
		 */
	override fun afterSoftDropFall(engine:GameEngine, fall:Int) {
		engine.statistics.scoreSD += fall*(engine.statistics.level+1)
	}

	override fun onFirst(engine:GameEngine) {
		pCoordList?.clear()
	}
	/*
		 * Hard drop
		 */
	override fun afterHardDropFall(engine:GameEngine, fall:Int) {
		engine.statistics.scoreHD += (fall*3+45)*(engine.statistics.level+1)
		/*	val baseX:Int = 16*engine.nowPieceX+receiver.fieldX(engine, playerID)
			val baseY:Int = 16*engine.nowPieceY+receiver.fieldY(engine, playerID)
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
			}*/
	}
	/*
		 * Called when saving replay
		 */
	override fun saveReplay(engine:GameEngine, prop:CustomProperties):Boolean {
		// NET: Save name
		if(netPlayerName!=null&&netPlayerName!!.isNotEmpty()) {
			prop.setProperty("${engine.playerID}.net.netPlayerName", netPlayerName)
		}

		// Update rankings
		if(!owner.replayMode&&!big&&engine.ai==null&&lifeOffset==0) {
			return updateRanking(
				engine.statistics.score, engine.statistics.lines, engine.statistics.time, difficultySelected,
				engine.playerProp.isLoggedIn
			)
		}
		return false
	}
	/**
	 * Update rankings
	 *
	 * @param sc   Score
	 * @param li   Lines
	 * @param time Time
	 */
	private fun updateRanking(sc:Long, li:Int, time:Int, diff:Int, isLogin:Boolean):Boolean {
		rankingRank = checkRanking(sc, li, time, diff)
		if(rankingRank!=-1) {
			// Shift down ranking entries
			for(i in RANKING_MAX-1 downTo rankingRank+1) {
				rankingScore[diff][i] = rankingScore[diff][i-1]
				rankingLines[diff][i] = rankingLines[diff][i-1]
				rankingTime[diff][i] = rankingTime[diff][i-1]
			}

			// Add new data
			rankingScore[diff][rankingRank] = sc
			rankingLines[diff][rankingRank] = li
			rankingTime[diff][rankingRank] = time
		}
		if(isLogin) {
			rankingRankPlayer = checkRankingPlayer(sc, li, time, diff)
			if(rankingRank!=-1) {
				// Shift down ranking entries
				for(i in RANKING_MAX-1 downTo rankingRankPlayer+1) {
					rankingScorePlayer[diff][i] = rankingScorePlayer[diff][i-1]
					rankingLinesPlayer[diff][i] = rankingLinesPlayer[diff][i-1]
					rankingTimePlayer[diff][i] = rankingTimePlayer[diff][i-1]
				}

				// Add new data
				rankingScorePlayer[diff][rankingRankPlayer] = sc
				rankingLinesPlayer[diff][rankingRankPlayer] = li
				rankingTimePlayer[diff][rankingRankPlayer] = time
			}
		} else rankingRankPlayer = -1
		return rankingRank!=-1||rankingRankPlayer!=-1
	}
	/**
	 * Calculate ranking position
	 *
	 * @param sc   Score
	 * @param li   Lines
	 * @param time Time
	 * @return Position (-1 if unranked)
	 */
	private fun checkRanking(sc:Long, li:Int, time:Int, diff:Int):Int {
		for(i in 0..<RANKING_MAX)
			if(sc>rankingScore[diff][i]) return i
			else if(sc==rankingScore[diff][i]&&li>rankingLines[diff][i]) return i
			else if(sc==rankingScore[diff][i]&&li==rankingLines[diff][i]&&time<rankingTime[diff][i]) return i
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
	private fun checkRankingPlayer(sc:Long, li:Int, time:Int, diff:Int):Int {
		for(i in 0..<RANKING_MAX)
			if(sc>rankingScorePlayer[diff][i]) return i
			else if(sc==rankingScorePlayer[diff][i]&&li>rankingLinesPlayer[diff][i]) return i
			else if(sc==rankingScorePlayer[diff][i]&&li==rankingLinesPlayer[diff][i]&&time<rankingTimePlayer[diff][i]) return i
		return -1
	}

	companion object {
		// Speed tables
		private val GRAVITY_TABLE = intArrayOf(
			8, 16, 24, 32, 40, 48, 56, 64, 72, 80,
			96, 104, 112, 128, 144, 152, 160, 192, 224, 256, 64,
			80, 96, 112, 128, 144, 160, 192, 224, 224, 256,
			256, 384, 512, 512, 768, 768, 1024, 1024, 1280, 512,
			512, 768, 1024, 1280, 1536, 1792, 2048, 2304, 2560, -1
		)
		private val ARE_TABLE = intArrayOf(
			15, 15, 15, 15, 14, 14,
			13, 12, 11, 10, 9,
			8, 7, 6, 5, 15
		)
		private val LOCK_TABLE = intArrayOf(
			30, 29, 28, 27, 26, 25,
			24, 23, 22, 21, 20,
			19, 18, 17, 17, 30
		)
		// Levels for speed changes
		private val LEVEL_ARE_LOCK_CHANGE = intArrayOf(
			60, 70, 80, 90, 100,
			110, 120, 130, 140, 150,
			160, 170, 180, 190, 200, 10000
		)
		// Timer constants
		private const val STARTING_TIMER = 7200
		private const val HARD_50_TIMER = 16200
		private const val NORMAL_30_TIMER = 10800
		private const val LEVEL_TIMEBONUS = 900
		private const val TIMER_MAX = 18000
		// Max difficulties
		private const val MAX_DIFFICULTIES = 3
		// Difficulty names
		private val DIFFICULTY_NAMES = arrayOf(
			"NORMAL",
			"HARD",
			"ADVANCE"
		)
		// Starting lives (4 here = 5 in play).
		// - N.B. when timer runs out, simply make the lifecount 0 before triggering game over.
		private const val STARTING_LIVES = 4
		// BG changes every 5 levels until 50, then at 200.
		// Level changes every 8 lines until lv 50, then every line clear increases it by 1 until 200.
		// 6f LD for NORMAL/HARD
		// 4'30" LV50 Time Limit for HARD
		// 3'00" LV30 Time Limit for NORMAL
		// Line clear scores.
		private val LINECLEAR_SCORES = intArrayOf(40, 100, 300, 1200)
		private val headerColor = COLOR.PINK
		private val tableBGM =
			arrayOf(
				arrayOf(
					BGMStatus.BGM.Generic(0), BGMStatus.BGM.Puzzle(1), BGMStatus.BGM.Generic(2),
					BGMStatus.BGM.GrandM(1)
				), //30levels
				arrayOf(
					BGMStatus.BGM.Generic(1), BGMStatus.BGM.GrandM(1), BGMStatus.BGM.GrandT(1),
					BGMStatus.BGM.Generic(4)
				), //50levels
				arrayOf(
					BGMStatus.BGM.Generic(2), BGMStatus.BGM.Generic(3), BGMStatus.BGM.Puzzle(2), BGMStatus.BGM.GrandT(1),
					BGMStatus.BGM.Generic(5)
				), //200levels
			)
	}
}
