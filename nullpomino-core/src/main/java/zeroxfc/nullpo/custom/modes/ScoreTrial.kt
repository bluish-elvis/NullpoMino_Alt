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

import mu.nu.nullpo.game.component.BGMStatus
import mu.nu.nullpo.game.component.Controller
import mu.nu.nullpo.game.event.EventReceiver.COLOR
import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.gui.common.particles.BlockParticleCollection
import mu.nu.nullpo.gui.slick.RendererExtension
import mu.nu.nullpo.util.CustomProperties
import mu.nu.nullpo.util.GeneralUtil.toTimeStr
import zeroxfc.nullpo.custom.libs.FlyInOutText
import zeroxfc.nullpo.custom.libs.ProfileProperties
import kotlin.math.ceil
import kotlin.random.Random

class ScoreTrial:MarathonModeBase() {
	// Ingame Timer
	private var mainTimer = 0
	// Ranking stuff
	private var rankingScore:Array<IntArray> = emptyArray()
	private var rankingLines:Array<IntArray> = emptyArray()
	private var rankingTime:Array<IntArray> = emptyArray()
	// Lives added/removed
	private var lifeOffset = 0
	// Score before increase;
	private var scoreBeforeIncrease = 0
	// ANIMATION TYPE
	private var lineClearAnimType = 0
	// Difficulty selector
	private var difficultySelected = 0
	// should use timer?
	private var shouldUseTimer = false
	// Local randomiser
	private var localRandom:Random = Random.Default
	// Goal line
	private var goalLine = 0
	// Lives started with
	private var livesStartedWith = 0
	// Particle stuff
	private var blockParticles:BlockParticleCollection? = null
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
	private val playerProperties:ProfileProperties = ProfileProperties(headerColor)
	private var showPlayerStats = false
	private var PLAYER_NAME = ""
	private var rankingRankPlayer = 0
	private var rankingScorePlayer:Array<IntArray> = emptyArray()
	private var rankingLinesPlayer:Array<IntArray> = emptyArray()
	private var rankingTimePlayer:Array<IntArray> = emptyArray()
	// Mode name
	override val name:String
		get() = "SCORE TRIAL"
	/*
     * Initialization
     */
	override fun playerInit(engine:GameEngine, playerID:Int) {
		owner = engine.owner
		lastscore = 0
		scgettime = 0
		bgmlv = 0
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
		rankingScore = Array(MAX_DIFFICULTIES) {IntArray(RANKING_MAX)}
		rankingLines = Array(MAX_DIFFICULTIES) {IntArray(RANKING_MAX)}
		rankingTime = Array(MAX_DIFFICULTIES) {IntArray(RANKING_MAX)}
		playerProperties.reset()
		showPlayerStats = false

		rankingRankPlayer = -1
		rankingScorePlayer = Array(MAX_DIFFICULTIES) {IntArray(RANKING_MAX)}
		rankingLinesPlayer = Array(MAX_DIFFICULTIES) {IntArray(RANKING_MAX)}
		rankingTimePlayer = Array(MAX_DIFFICULTIES) {IntArray(RANKING_MAX)}
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
			if(version==0&&owner.replayProp.getProperty("scoretrial.endless", false)) goaltype = 2
			PLAYER_NAME = owner.replayProp.getProperty("scoretrial.playerName", "")

			// NET: Load name
			netPlayerName = engine.owner.replayProp.getProperty("$playerID.net.netPlayerName", "")
		}
		engine.owner.backgroundStatus.bg = startlevel
		engine.framecolor = GameEngine.FRAME_COLOR_GRAY
		engine.twistEnable = false
		engine.comboType = GameEngine.COMBO_TYPE_NORMAL
		engine.b2b = false
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
	override fun onSetting(engine:GameEngine, playerID:Int):Boolean {
		// NET: Net Ranking
		if(netIsNetRankingDisplayMode) {
			netOnUpdateNetPlayRanking(engine, goaltype)
		} else if(!engine.owner.replayMode) {
			// Configuration changes
			val change = updateCursor(engine, 3, playerID)
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
						if(lineClearAnimType>BlockParticleCollection.ANIMATION_TYPES-1) lineClearAnimType = 0
						if(lineClearAnimType<0) lineClearAnimType = BlockParticleCollection.ANIMATION_TYPES-1
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
	/*
     * Render the settings screen
     */
	override fun renderSetting(engine:GameEngine, playerID:Int) {
		val lc = when(lineClearAnimType) {
			0 -> "DTET"
			1 -> "TGM"
			else -> ""
		}
		if(netIsNetRankingDisplayMode) {
			// NET: Netplay Ranking
			netOnRenderNetPlayRanking(engine, playerID, receiver)
		} else {
			drawMenu(engine, playerID, receiver, 0, COLOR.RED, 0, "DIFFICULTY" to DIFFICULTY_NAMES[difficultySelected])
			drawMenu(engine, playerID, receiver, 2, COLOR.GREEN, 1, "LIVES" to STARTING_LIVES+lifeOffset+1)
			drawMenu(engine, playerID, receiver, 4, COLOR.BLUE, 2, "BIG" to big, "LINE ANIM." to lc)
		}
	}

	override fun onReady(engine:GameEngine, playerID:Int):Boolean {
		mainTimer = STARTING_TIMER
		goalLine = 8
		when(difficultySelected) {
			0 -> mainTimer = NORMAL_30_TIMER
			1 -> mainTimer = HARD_50_TIMER
			2 -> mainTimer = STARTING_TIMER
		}
		livesStartedWith = STARTING_LIVES+lifeOffset
		engine.lives = STARTING_LIVES+lifeOffset
		if(blockParticles!=null) {
			blockParticles = null
		}
		return false
	}
	/*
     * Called for initialization during "Ready" screen
     */
	override fun startGame(engine:GameEngine, playerID:Int) {
		engine.statistics.level = startlevel
		engine.statistics.levelDispAdd = 0
		when(difficultySelected) {
			0 -> {
				engine.ruleOpt.areCancelHold = false
				engine.ruleOpt.areCancelMove = false
				engine.ruleOpt.areCancelRotate = false
				engine.ruleOpt.harddropEnable = false
				engine.ruleOpt.softdropSurfaceLock = true
				engine.ruleOpt.softdropEnable = true
				shouldUseTimer = false
				engine.speed.lineDelay = 8
			}
			1 -> {
				engine.ruleOpt.areCancelHold = false
				engine.ruleOpt.areCancelMove = false
				engine.ruleOpt.areCancelRotate = false
				engine.ruleOpt.harddropEnable = true
				engine.ruleOpt.softdropSurfaceLock = true
				engine.ruleOpt.softdropEnable = true
				shouldUseTimer = false
				engine.speed.lineDelay = 8
			}
			2 -> {
				engine.ruleOpt.areCancelHold = true
				engine.ruleOpt.areCancelMove = true
				engine.ruleOpt.areCancelRotate = true
				engine.ruleOpt.harddropEnable = true
				engine.ruleOpt.softdropSurfaceLock = true
				engine.ruleOpt.softdropEnable = true
				shouldUseTimer = true
				engine.speed.lineDelay = 0
			}
		}
		engine.ruleOpt.rotateButtonAllowDouble = true
		engine.ghost = false
		engine.big = big
		scoreBeforeIncrease = 0
		engine.speed.das = 8
		engine.speed.denominator = 256
		engine.speed.are = 15
		engine.speed.areLine = 15
		engine.speed.lockDelay = 30
		blockParticles = BlockParticleCollection(engine.field.let {(it.height+it.hiddenHeight)*it.width*2}, lineClearAnimType)
		localRandom = Random(engine.randSeed)
		l = STARTING_LIVES+lifeOffset+1
		o = false
		setSpeed(engine)
		owner.bgmStatus.bgm = tableBGM[difficultySelected][0]
		owner.bgmStatus.fadesw = false
		if(netIsWatch) {
			owner.bgmStatus.bgm = BGMStatus.BGM.Silent
		}
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
     * Render score
     */
	override fun renderLast(engine:GameEngine, playerID:Int) {
		if(owner.menuOnly) return
		receiver.drawScoreFont(engine, playerID, 0, 0, name, COLOR.GREEN)
		receiver.drawScoreFont(engine, playerID, 0, 1, "("+DIFFICULTY_NAMES[difficultySelected]+" TIER)", COLOR.GREEN)
		if(engine.stat===GameEngine.Status.SETTING||engine.stat===GameEngine.Status.RESULT&&!owner.replayMode) {
			if(!owner.replayMode&&!big&&engine.ai==null&&lifeOffset==0) {
				val scale = if(receiver.nextDisplayType==2) 0.5f else 1.0f
				val topY = if(receiver.nextDisplayType==2) 6 else 4
				receiver.drawScoreFont(engine, playerID, 3, topY-1, "SCORE  LINE TIME", COLOR.BLUE, scale)
				if(showPlayerStats) {
					for(i in 0 until RANKING_MAX) {
						receiver.drawScoreFont(engine, playerID, 0, topY+i, String.format("%2d", i+1), COLOR.YELLOW, scale)
						val s = "${rankingScorePlayer[difficultySelected][i]}"
						receiver.drawScoreFont(engine, playerID, if(s.length>6&&receiver.nextDisplayType!=2) 6 else 3,
							if(s.length>6&&receiver.nextDisplayType!=2) (topY+i)*2 else topY+i, s, i==rankingRankPlayer,
							if(s.length>6&&receiver.nextDisplayType!=2) scale*0.5f else scale)
						receiver.drawScoreFont(engine, playerID, 10, topY+i, "${rankingLinesPlayer[difficultySelected][i]}",
							i==rankingRankPlayer, scale)
						receiver.drawScoreFont(engine, playerID, 15, topY+i, rankingTimePlayer[difficultySelected][i].toTimeStr,
							i==rankingRankPlayer, scale)
					}
					receiver.drawScoreFont(engine, playerID, 0, topY+RANKING_MAX+1, "PLAYER SCORES", COLOR.BLUE)
					receiver.drawScoreFont(engine, playerID, 0, topY+RANKING_MAX+2, playerProperties.nameDisplay,
						COLOR.WHITE, 2f)
					receiver.drawScoreFont(engine, playerID, 0, topY+RANKING_MAX+5, "F:SWITCH RANK SCREEN", COLOR.GREEN)
				} else {
					for(i in 0 until RANKING_MAX) {
						receiver.drawScoreFont(engine, playerID, 0, topY+i, String.format("%2d", i+1), COLOR.YELLOW, scale)
						val s = "${rankingScore[difficultySelected][i]}"
						receiver.drawScoreFont(engine, playerID, if(s.length>6&&receiver.nextDisplayType!=2) 6 else 3,
							if(s.length>6&&receiver.nextDisplayType!=2) (topY+i)*2 else topY+i, s, i==rankingRank,
							if(s.length>6&&receiver.nextDisplayType!=2) scale*0.5f else scale)
						receiver.drawScoreFont(engine, playerID, 10, topY+i, "${rankingLines[difficultySelected][i]}",
							i==rankingRank, scale)
						receiver.drawScoreFont(engine, playerID, 15, topY+i, rankingTime[difficultySelected][i].toTimeStr,
							i==rankingRank, scale)
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
			receiver.drawScoreFont(engine, playerID, 0, 3, "LINE", COLOR.BLUE)

			receiver.drawScoreNum(engine, playerID, 5, 2, String.format("%3d/%3d", engine.statistics.lines, goalLine), 2f)
			val scget:Boolean = scgettime<engine.statistics.score
			receiver.drawScoreFont(engine, playerID, 0, 4, "Score", COLOR.BLUE)
			receiver.drawScoreNum(engine, playerID, 5, 4, "+$lastscore")
			if(scget) scgettime += ceil((engine.statistics.score-scgettime)/24.0).toInt()
			sc += ceil(((scgettime-sc)/10f).toDouble()).toInt()
			receiver.drawScoreNum(engine, playerID, 0, 5, "$sc", scget, 2f)
			if(!o) {
				l = engine.lives+1
			}
			if(engine.stat===GameEngine.Status.GAMEOVER&&!o&&engine.lives==0) l = 0
			receiver.drawScoreFont(engine, playerID, 0, 9, "LIVES", COLOR.BLUE)
			receiver.drawScoreFont(engine, playerID, 0, 10, "$l", (l<=1))
			receiver.drawScoreFont(engine, playerID, 0, 12, "LEVEL", COLOR.BLUE)
			receiver.drawScoreFont(engine, playerID, 0, 13, "${engine.statistics.level}")
			receiver.drawScoreFont(engine, playerID, 0, 15, "TIME", COLOR.BLUE)
			receiver.drawScoreFont(engine, playerID, 0, 16, engine.statistics.time.toTimeStr)
			if(playerProperties.isLoggedIn||PLAYER_NAME.isNotEmpty()) {
				receiver.drawScoreFont(engine, playerID, 11, 6, "PLAYER", COLOR.BLUE)
				receiver.drawScoreFont(engine, playerID, 11, 7, if(owner.replayMode) PLAYER_NAME else playerProperties.nameDisplay,
					COLOR.WHITE, 2f)
			}

			if(shouldUseTimer) {
				receiver.drawMenuFont(engine, playerID, 0, 21, "TIME LIMIT", COLOR.RED)
				receiver.drawMenuFont(engine, playerID, 1, 22, mainTimer.toTimeStr, mainTimer<=600&&mainTimer/2%2==0)
			}
			blockParticles?.drawAll(engine, receiver, playerID)
			congratulationsText?.draw(engine, receiver, playerID)
			comboTextAward?.draw(engine, receiver, playerID)
			comboTextNumber?.draw(engine, receiver, playerID)

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
     * Called after every frame
     */
	override fun onLast(engine:GameEngine, playerID:Int) {
		scgettime++
		if(engine.gameStarted&&engine.ending==0) {
			if(engine.stat===GameEngine.Status.ARE&&difficultySelected==2) {
				engine.dasCount = engine.speed.das
			}
			if(shouldUseTimer&&engine.stat!==GameEngine.Status.GAMEOVER) {
				mainTimer--
				if(mainTimer<=600&&mainTimer%60==0) {
					receiver.playSE("countdown")
				}
			}

			// Meter - use as timer.
			var timerMax = TIMER_MAX
			if(difficultySelected==0) timerMax = NORMAL_30_TIMER
			if(difficultySelected==1) timerMax = HARD_50_TIMER
			engine.meterValue = (mainTimer/timerMax.toDouble()*receiver.getMeterMax(engine)).toInt()
			engine.meterColor = GameEngine.METER_COLOR_GREEN
			if(mainTimer in 3601..5399) engine.meterColor = GameEngine.METER_COLOR_YELLOW
			if(mainTimer in 1801..3600) engine.meterColor = GameEngine.METER_COLOR_ORANGE
			if(mainTimer<=1800) engine.meterColor = GameEngine.METER_COLOR_RED
			if(mainTimer<=0) {
				if(engine.statistics.level<199&&difficultySelected==2) {
					engine.lives = 0
					engine.stat = GameEngine.Status.GAMEOVER
					engine.resetStatc()
					engine.gameEnded()
				} else {
					// Ending
					var baseBonus:Int = engine.statistics.score
					baseBonus *= ((engine.lives+1) as Float/(livesStartedWith+1)).toInt()
					engine.statistics.scoreBonus += baseBonus
					lastscore = baseBonus
					o = true
					l = engine.lives+1
					scgettime = 0
					if(engine.lives==STARTING_LIVES+lifeOffset) {
						val destinationX = 192
						val destinationY = 224
						congratulationsText = FlyInOutText("PERFECT!", destinationX, destinationY, 15, 90, 15,
							arrayOf(COLOR.GREEN, COLOR.CYAN), 2.0f, engine.randSeed+engine.statistics.time,
							true)
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
			blockParticles?.update()
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
			if(engine.ctrl.isPush(Controller.BUTTON_F)&&playerProperties.isLoggedIn&&engine.stat!==GameEngine.Status.CUSTOM) {
				showPlayerStats = !showPlayerStats
				engine.playSE("change")
			}
		}
		if(engine.quitflag) {
			playerProperties.reset()
		}
	}

	override fun onGameOver(engine:GameEngine, playerID:Int):Boolean {
		if(engine.lives<=0) {
			shouldUseTimer = false
		}
		return false
	}

	/*
     * Calculate score - PAIN
     */
	override fun calcScore(engine:GameEngine, playerID:Int, lines:Int):Int {
		// Line clear bonus
		var linesUsed = lines
		if(linesUsed>4) linesUsed = 4
		if(lines>0) {
			var pts = LINECLEAR_SCORES[linesUsed-1]
			pts *= engine.combo
			pts *= engine.statistics.level+1
			if(engine.combo>=2) {
				val destinationX:Int = receiver.scoreX(engine, playerID)
				val destinationY:Int = receiver.scoreY(engine, playerID)+20*if(engine.displaysize==0) 16 else 32
				var colors = arrayOf(COLOR.COBALT)
				if(engine.combo>=4) colors = arrayOf(COLOR.BLUE, COLOR.COBALT)
				if(engine.combo>=8) colors = arrayOf(COLOR.CYAN, COLOR.BLUE,
					COLOR.COBALT)
				if(engine.combo>=11) colors = arrayOf(COLOR.YELLOW, COLOR.ORANGE,
					COLOR.RED)
				comboTextNumber = FlyInOutText("${(engine.combo-1)} COMBO", destinationX, destinationY, 15, 60, 15, colors,
					1.0f, engine.randSeed+engine.statistics.time, engine.combo>=8)
				if(engine.combo==4) comboTextAward = FlyInOutText("GOOD!", destinationX,
					destinationY+if(engine.displaysize==0) 16 else 32, 15, 60, 15, arrayOf(COLOR.YELLOW), 1.0f,
					engine.randSeed+engine.statistics.time, engine.combo>=8)
				if(engine.combo==8) comboTextAward = FlyInOutText("AWESOME!", destinationX,
					destinationY+if(engine.displaysize==0) 16 else 32, 15, 60, 15, arrayOf(COLOR.GREEN), 1.0f,
					engine.randSeed+engine.statistics.time, engine.combo>=8)
				if(engine.combo==11) comboTextAward = FlyInOutText("UNREAL!", destinationX,
					destinationY+if(engine.displaysize==0) 16 else 32, 15, 60, 15,
					arrayOf(COLOR.ORANGE, COLOR.RED), 1.0f, engine.randSeed+engine.statistics.time,
					engine.combo>=8)
			}

			// Add to score
			if(pts>0) {
				scoreBeforeIncrease = engine.statistics.score
				lastscore = pts
				scgettime = 0
				if(lines>=1) engine.statistics.scoreLine += pts else engine.statistics.scoreBonus += pts
			}
		}
		if(engine.statistics.level in 50..199&&lines>0&&difficultySelected==2) {
			// Level up
			engine.statistics.level++

			// owner.backgroundStatus.fadesw = true;
			// owner.backgroundStatus.fadecount = 0;
			// owner.backgroundStatus.fadebg = engine.statistics.level / 5;
			if(engine.statistics.level==200) {
				owner.backgroundStatus.bg = 19
				engine.playSE("endingstart")
				val destinationX:Int = receiver.scoreX(engine, playerID)
				val destinationY:Int = receiver.scoreY(engine, playerID)+18*if(engine.displaysize==0) 16 else 32
				congratulationsText = FlyInOutText("WELL DONE!", destinationX, destinationY, 30, 120, 30,
					arrayOf(COLOR.YELLOW, COLOR.ORANGE, COLOR.RED), 1.0f,
					engine.randSeed+engine.statistics.time, false)
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

				// owner.backgroundStatus.fadesw = true;
				// owner.backgroundStatus.fadecount = 0;
				owner.backgroundStatus.bg = engine.statistics.level/5
				if(engine.statistics.level==lMax&&difficultySelected!=2) {
					owner.backgroundStatus.bg = 19
					shouldUseTimer = true
					engine.playSE("endingstart")
					val destinationX:Int = receiver.scoreX(engine, playerID)
					val destinationY:Int = receiver.scoreY(engine, playerID)+18*if(engine.displaysize==0) 16 else 32
					congratulationsText = FlyInOutText("WELL DONE!", destinationX, destinationY, 30, 120, 30,
						arrayOf(COLOR.YELLOW, COLOR.ORANGE, COLOR.RED), 1.0f,
						engine.randSeed+engine.statistics.time, false)
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
	override fun afterSoftDropFall(engine:GameEngine, playerID:Int, fall:Int) {
		engine.statistics.scoreSD += fall*(engine.statistics.level+1)
	}

	override fun onFirst(engine:GameEngine, playerID:Int) {
		pCoordList!!.clear()
	}
	/*
     * Hard drop
     */
	override fun afterHardDropFall(engine:GameEngine, playerID:Int, fall:Int) {
		engine.statistics.scoreHD += (fall*3+45)*(engine.statistics.level+1)
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
		if(!owner.replayMode&&!big&&engine.ai==null&&lifeOffset==0) {
			updateRanking(engine.statistics.score, engine.statistics.lines, engine.statistics.time, difficultySelected)
			if(playerProperties.isLoggedIn) {
				prop.setProperty("scoretrial.playerName", playerProperties.nameDisplay)
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
		startlevel = prop.getProperty("scoretrial.startlevel", 0)
		big = prop.getProperty("scoretrial.big", false)
		lifeOffset = prop.getProperty("scoretrial.extralives", 0)
		version = prop.getProperty("scoretrial.version", 0)
		lineClearAnimType = prop.getProperty("scoretrial.lcat", 0)
		difficultySelected = prop.getProperty("scoretrial.difficulty", 0)
	}
	/**
	 * Save settings to property file
	 *
	 * @param prop Property file
	 */
	override fun saveSetting(prop:CustomProperties) {
		prop.setProperty("scoretrial.startlevel", startlevel)
		prop.setProperty("scoretrial.big", big)
		prop.setProperty("scoretrial.extralives", lifeOffset)
		prop.setProperty("scoretrial.version", version)
		prop.setProperty("scoretrial.lcat", lineClearAnimType)
		prop.setProperty("scoretrial.difficulty", difficultySelected)
	}
	/**
	 * Load settings from property file
	 *
	 * @param prop Property file
	 */
	private fun loadSettingPlayer(prop:ProfileProperties?) {
		if(prop?.isLoggedIn!=true) return
		startlevel = prop.getProperty("scoretrial.startlevel", 0)
		big = prop.getProperty("scoretrial.big", false)
		lifeOffset = prop.getProperty("scoretrial.extralives", 0)
		lineClearAnimType = prop.getProperty("scoretrial.lcat", 0)
		difficultySelected = prop.getProperty("scoretrial.difficulty", 0)
	}
	/**
	 * Save settings to property file
	 *
	 * @param prop Property file
	 */
	private fun saveSettingPlayer(prop:ProfileProperties) {
		if(!prop.isLoggedIn) return
		prop.setProperty("scoretrial.startlevel", startlevel)
		prop.setProperty("scoretrial.big", big)
		prop.setProperty("scoretrial.extralives", lifeOffset)
		prop.setProperty("scoretrial.lcat", lineClearAnimType)
		prop.setProperty("scoretrial.difficulty", difficultySelected)
	}
	/**
	 * Read rankings from property file
	 *
	 * @param prop     Property file
	 * @param ruleName Rule name
	 */
	override fun loadRanking(prop:CustomProperties, ruleName:String) {
		for(j in 0 until MAX_DIFFICULTIES) {
			for(i in 0 until RANKING_MAX) {
				rankingScore[j][i] = prop.getProperty("scoretrial.ranking.$ruleName.$j.score.$i", 0)
				rankingLines[j][i] = prop.getProperty("scoretrial.ranking.$ruleName.$j.lines.$i", 0)
				rankingTime[j][i] = prop.getProperty("scoretrial.ranking.$ruleName.$j.time.$i", 0)
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
		for(j in 0 until MAX_DIFFICULTIES) {
			for(i in 0 until RANKING_MAX) {
				prop.setProperty("scoretrial.ranking.$ruleName.$j.score.$i", rankingScore[j][i])
				prop.setProperty("scoretrial.ranking.$ruleName.$j.lines.$i", rankingLines[j][i])
				prop.setProperty("scoretrial.ranking.$ruleName.$j.time.$i", rankingTime[j][i])
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
		for(j in 0 until MAX_DIFFICULTIES) {
			for(i in 0 until RANKING_MAX) {
				rankingScorePlayer[j][i] = prop.getProperty("scoretrial.ranking.$ruleName.$j.score.$i", 0)
				rankingLinesPlayer[j][i] = prop.getProperty("scoretrial.ranking.$ruleName.$j.lines.$i", 0)
				rankingTimePlayer[j][i] = prop.getProperty("scoretrial.ranking.$ruleName.$j.time.$i", 0)
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
		for(j in 0 until MAX_DIFFICULTIES) {
			for(i in 0 until RANKING_MAX) {
				prop.setProperty("scoretrial.ranking.$ruleName.$j.score.$i", rankingScorePlayer[j][i])
				prop.setProperty("scoretrial.ranking.$ruleName.$j.lines.$i", rankingLinesPlayer[j][i])
				prop.setProperty("scoretrial.ranking.$ruleName.$j.time.$i", rankingTimePlayer[j][i])
			}
		}
	}
	/**
	 * Update rankings
	 *
	 * @param sc   Score
	 * @param li   Lines
	 * @param time Time
	 */
	private fun updateRanking(sc:Int, li:Int, time:Int, diff:Int) {
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
		if(playerProperties.isLoggedIn) {
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
		}
	}
	/**
	 * Calculate ranking position
	 *
	 * @param sc   Score
	 * @param li   Lines
	 * @param time Time
	 * @return Position (-1 if unranked)
	 */
	private fun checkRanking(sc:Int, li:Int, time:Int, diff:Int):Int {
		for(i in 0 until RANKING_MAX) {
			if(sc>rankingScore[diff][i]) {
				return i
			} else if(sc==rankingScore[diff][i]&&li>rankingLines[diff][i]) {
				return i
			} else if(sc==rankingScore[diff][i]&&li==rankingLines[diff][i]&&time<rankingTime[diff][i]) {
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
	private fun checkRankingPlayer(sc:Int, li:Int, time:Int, diff:Int):Int {
		for(i in 0 until RANKING_MAX)
			if(sc>rankingScorePlayer[diff][i]) return i
			else if(sc==rankingScorePlayer[diff][i]&&li>rankingLinesPlayer[diff][i]) return i
			else if(sc==rankingScorePlayer[diff][i]&&li==rankingLinesPlayer[diff][i]&&time<rankingTimePlayer[diff][i]) return i
		return -1
	}

	companion object {
		// Speed tables
		private val GRAVITY_TABLE = intArrayOf(8, 16, 24, 32, 40, 48, 56, 64, 72, 80,
			96, 104, 112, 128, 144, 152, 160, 192, 224, 256, 64,
			80, 96, 112, 128, 144, 160, 192, 224, 224, 256,
			256, 384, 512, 512, 768, 768, 1024, 1024, 1280, 512,
			512, 768, 1024, 1280, 1536, 1792, 2048, 2304, 2560, -1)
		private val ARE_TABLE = intArrayOf(15, 15, 15, 15, 14, 14,
			13, 12, 11, 10, 9,
			8, 7, 6, 5, 15)
		private val LOCK_TABLE = intArrayOf(30, 29, 28, 27, 26, 25,
			24, 23, 22, 21, 20,
			19, 18, 17, 17, 30)
		// Levels for speed changes
		private val LEVEL_ARE_LOCK_CHANGE = intArrayOf(60, 70, 80, 90, 100,
			110, 120, 130, 140, 150,
			160, 170, 180, 190, 200, 10000)
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
				arrayOf(BGMStatus.BGM.Generic(0), BGMStatus.BGM.Puzzle(1), BGMStatus.BGM.Generic(2),
					BGMStatus.BGM.GrandM(1)), //30levels
				arrayOf(BGMStatus.BGM.Generic(1), BGMStatus.BGM.GrandM(1), BGMStatus.BGM.GrandT(1),
					BGMStatus.BGM.Generic(4)), //50levels
				arrayOf(
					BGMStatus.BGM.Generic(2), BGMStatus.BGM.Generic(3), BGMStatus.BGM.Puzzle(2), BGMStatus.BGM.GrandT(1),
					BGMStatus.BGM.Generic(5)), //200levels
			)
	}
}