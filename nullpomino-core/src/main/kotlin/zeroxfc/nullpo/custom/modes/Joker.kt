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
import mu.nu.nullpo.gui.common.AbstractRenderer.FontBadge.Companion.b
import mu.nu.nullpo.gui.common.fx.particles.BlockParticle
import mu.nu.nullpo.util.CustomProperties
import mu.nu.nullpo.util.GeneralUtil.toTimeStr
import zeroxfc.nullpo.custom.libs.FlyInOutText
import kotlin.random.Random

class Joker:MarathonModeBase() {
	// Ingame Timer
	private var mainTimer = 0
	// should use timer?
	private var shouldUseTimer = false
	// Tetris line clear lifeline
	private var stock = 0
	// starting stock
	private var startingStock = 0
	// Ranking stuff
	private val rankingLevel = intArrayOf()
	private val rankingTime = intArrayOf()
	private val rankingLines = intArrayOf()
	// time score
	private var timeScore = 0
	// Local randomizer
	private var localRandom:Random? = null
	// Particle stuff
//	private var blockParticles:Mapper? = null
	// ANIMATION TYPE
	private var lineClearAnimType = 0
	// Warning texts
	private var warningText:FlyInOutText? = null
	private var warningTextSecondLine:FlyInOutText? = null
	// Custom asset variables
	//private val customHolder:ResourceHolderCustomAssetExtension = ResourceHolderCustomAssetExtension()
	private var efficiency = 0f
	private var efficiencyGrade = 0
	// PROFILE
	private var rankingRankPlayer = 0
	private val rankingLevelPlayer = intArrayOf()
	private val rankingTimePlayer = intArrayOf()
	private val rankingLinesPlayer = intArrayOf()
	// Last amount of lines cleared;
	private var lastLine = 0
	// Animated backgrounds
	private var useAnimBG = false
	/*	private val ANIMATED_BACKGROUNDS:Array<AnimatedBackgroundHook> by lazy {
			arrayOf(
				BackgroundVerticalBars(18, 60, 160, 1f, 4f, false),
				BackgroundDiagonalRipple(19, 8, 8, 60, 1f, 2f, false, false)
			)
		}*/

	override val name:String
		get() = "JOKER"
	/*
     * Initialization
     */
	override fun playerInit(engine:GameEngine) {
		super.playerInit(engine)
		efficiency = 0f
		efficiencyGrade = 0
		lastScore = 0
		lineClearAnimType = 0
		shouldUseTimer = false
		stock = 0
		startingStock = 0
		lastLine = 0
		useAnimBG = true
		mainTimer = 0
		warningText = null
		warningTextSecondLine = null
		rankingRank = -1
		rankingLevel.fill(0)
		rankingLines.fill(0)
		rankingTime.fill(-1)
		engine.playerProp.reset()
		showPlayerStats = false

		rankingRankPlayer = -1
		rankingLevelPlayer.fill(0)
		rankingLinesPlayer.fill(0)
		rankingTimePlayer.fill(0)
		netPlayerInit(engine)
		if(!owner.replayMode) {
			version = CURRENT_VERSION
		} else {
			if(version==0&&owner.replayProp.getProperty("joker.endless", false)) goalType = 2

			// NET: Load name
			netPlayerName = engine.owner.replayProp.getProperty("${engine.playerID}.net.netPlayerName", "")
		}
		engine.owner.bgMan.bg = 18
		engine.frameColor = GameEngine.FRAME_COLOR_PURPLE
		engine.twistEnable = true
		engine.comboType = GameEngine.COMBO_TYPE_NORMAL
		engine.statistics.levelDispAdd = 0
		engine.speed.gravity = -1
		engine.speed.das = 6
		engine.speed.denominator = 256
		engine.speed.are = 15
		engine.speed.areLine = 15
		engine.speed.lineDelay = 0
		engine.blockShowOutlineOnly = true
		engine.bigHalf = true
		engine.bigMove = true

		// 92 x 96
		//customHolder.loadImage("res/graphics/efficiency_grades.png", "grades")
	}
	/**
	 * Set the gravity rate
	 *
	 * @param engine GameEngine
	 */
	override fun setSpeed(engine:GameEngine) {
		if(engine.statistics.level>=60) {
			var ldAreIndex = 0
			while(engine.statistics.level>=LEVEL_ARE_LOCK_CHANGE[ldAreIndex]) ldAreIndex++
			engine.speed.are = ARE_TABLE[ldAreIndex]
			engine.speed.areLine = ARE_TABLE[ldAreIndex]
			engine.speed.lockDelay = LOCK_TABLE[ldAreIndex]
		}
	}

	private fun calculateEfficiencyGrade(engine:GameEngine) {
		efficiency = engine.statistics.lines.toFloat()/((engine.statistics.level-50)*4)
		for(i in GRADE_BOUNDARIES.indices) {
			if(efficiency>=GRADE_BOUNDARIES[i]) efficiencyGrade = i
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
			val change = updateCursor(engine, 4)
			if(change!=0) {
				engine.playSE("change")
				when(engine.statc[2]) {
					0 -> {
						startingStock += change
						if(startingStock>25) startingStock = 0
						if(startingStock<0) startingStock = 25
					}
					1 -> big = !big
					2 -> {
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
			}

			// New acc
			if(engine.ctrl.isPush(Controller.BUTTON_E)&&engine.ai==null&&!netIsNetPlay) {
				engine.playerProp.reset()
				engine.playSE("decide")
				engine.stat = GameEngine.Status.PROFILE
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
			drawMenu(engine, receiver, 0, COLOR.RED, 0, "ST. STOCK" to startingStock)
			drawMenu(engine, receiver, 2, COLOR.BLUE, 1, "BIG" to big, "LINE ANIM." to lc)
		}
	}

	override fun onReady(engine:GameEngine):Boolean {
		mainTimer = STARTING_TIMER
		engine.statistics.level = 50
		stock = startingStock
		timeScore = 0
		efficiencyGrade = 0
		efficiency = 0f
		engine.lives = 0
		if(engine.statc[0]==0&&useAnimBG) {
			engine.owner.bgMan.bg = -2
			//for(bg in ANIMATED_BACKGROUNDS) bg.reset()
		}
		return false
	}
	/*
     * Called for initialization during "Ready" screen
     */
	override fun startGame(engine:GameEngine) {
		engine.statistics.levelDispAdd = 0
		engine.ruleOpt.areCancelHold = true
		engine.ruleOpt.areCancelMove = true
		engine.ruleOpt.areCancelSpin = true
		engine.ruleOpt.harddropEnable = true
		engine.ruleOpt.softdropSurfaceLock = true
		engine.ruleOpt.softdropEnable = true
		shouldUseTimer = true
		engine.speed.lineDelay = 0
		lastLine = 0
		engine.ruleOpt.spinDoubleKey = true
		engine.ghost = false
		engine.big = big

		// scoreBeforeIncrease = 0;
		engine.speed.gravity = -1
		engine.speed.das = 6
		engine.speed.denominator = 256
		engine.speed.are = 15
		engine.speed.areLine = 15
		engine.blockShowOutlineOnly = true
		engine.blockOutlineType = GameEngine.BLOCK_OUTLINE_NORMAL
		engine.bigHalf = true
		engine.bigMove = true
		localRandom = Random(engine.randSeed)

		// l = STARTING_LIVES + lifeOffset + 1;

		// o = false;
		setSpeed(engine)
		owner.musMan.bgm = BGMStatus.BGM.Finale(1)

		owner.musMan.fadeSW = false
		if(netIsWatch) {
			owner.musMan.bgm = BGMStatus.BGM.Silent
		}
	}

	/*override fun renderFirst(engine:GameEngine) {
		if(useAnimBG&&engine.owner.bgMan.bg<0) {
			ANIMATED_BACKGROUNDS[engine.owner.bgMan.bg+2].draw(engine)
		}
	}*/
	/*
     * Render score
     */
	override fun renderLast(engine:GameEngine) {
		if(owner.menuOnly) return
		receiver.drawScoreFont(engine, 0, 0, name, COLOR.RED)
		if(engine.stat===GameEngine.Status.SETTING||engine.stat===GameEngine.Status.RESULT&&!owner.replayMode) {
			if(!owner.replayMode&&!big&&engine.ai==null&&startingStock==0) {
				val topY = if(receiver.nextDisplayType==2) 6 else 4
				receiver.drawScoreFont(engine, 3, topY-1, "LEVEL  LINE TIME", COLOR.BLUE)
				if(showPlayerStats) {
					for(i in 0..<RANKING_MAX) {
						receiver.drawScoreFont(engine, 0, topY+i, "%2d".format(i+1), COLOR.YELLOW)
						val s = "${rankingLevelPlayer[i]}"
						val isLong = s.length>6&&receiver.nextDisplayType!=2
						receiver.drawScoreFont(
							engine, if(isLong) 6 else 3, if(isLong) (topY+i)*2 else topY+i, s, i==rankingRankPlayer,
							if(isLong) .5f else 1f
						)
						receiver.drawScoreFont(engine, 10, topY+i, "${rankingLinesPlayer[i]}", i==rankingRankPlayer)
						receiver.drawScoreFont(engine, 15, topY+i, rankingTimePlayer[i].toTimeStr, i==rankingRankPlayer)
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
						val s = "${rankingLevel[i]}"
						val isLong = s.length>6&&receiver.nextDisplayType!=2
						receiver.drawScoreFont(
							engine, if(isLong) 6 else 3, if(isLong) (topY+i)*2 else topY+i, s, i==rankingRank,
							if(isLong) .5f else 1f
						)
						receiver.drawScoreFont(engine, 10, topY+i, "${rankingLines[i]}", i==rankingRank)
						receiver.drawScoreFont(engine, 15, topY+i, rankingTime[i].toTimeStr, i==rankingRank)
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
			receiver.drawScoreFont(engine, 0, 3, "TIME", COLOR.BLUE)
			val strScore = "${timeScore.toTimeStr}(+${(engine.statistics.time-timeScore).toTimeStr})"
			receiver.drawScoreFont(engine, 0, 4, strScore)
			receiver.drawScoreFont(engine, 0, 6, "LINE", COLOR.BLUE)
			receiver.drawScoreFont(engine, 0, 7, "${engine.statistics.lines}")
			receiver.drawScoreFont(engine, 0, 9, "LEVEL", COLOR.BLUE)
			receiver.drawScoreFont(engine, 0, 10, "${engine.statistics.level}")

			receiver.drawScoreFont(engine, 0, 12, "STOCK", COLOR.GREEN)
			receiver.drawScoreFont(engine, 0, 13, "$stock"+b, (stock<=2))
			receiver.drawScoreFont(engine, 0, 15, "EFFICIENCY", COLOR.GREEN)
			receiver.drawScoreNum(
				engine, 0, 16, "%.2f%%".format(efficiency*100),
				if(engine.statistics.level>=300) COLOR.PINK else COLOR.WHITE
			)

			if(engine.playerProp.isLoggedIn||engine.playerName.isNotEmpty()) {
				receiver.drawScoreFont(engine, 0, 18, "PLAYER", COLOR.BLUE)
				receiver.drawScoreFont(
					engine, 0, 19, if(owner.replayMode) engine.playerName else engine.playerProp.nameDisplay,
					COLOR.WHITE,
					2f
				)
			}
			if(shouldUseTimer) {
				receiver.drawMenuFont(engine, 0, 21, "TIME LIMIT", COLOR.RED)
				receiver.drawMenuFont(engine, 1, 22, mainTimer.toTimeStr, mainTimer<=600&&mainTimer/2%2==0)
			}

			warningText?.draw(receiver)
			warningTextSecondLine?.draw(receiver)
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

	override fun onGameOver(engine:GameEngine):Boolean {
		shouldUseTimer = false
		return false
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
	/*
     * Called after every frame
     */
	override fun onLast(engine:GameEngine) {
		super.onLast(engine)
		/*if(useAnimBG&&engine.owner.bgMan.bg<0) {
			ANIMATED_BACKGROUNDS[engine.owner.bgMan.bg+2].update()
		}*/
		if(engine.gameStarted&&engine.ending==0) {
			/*if(engine.stat===GameEngine.Status.ARE) {
				engine.dasCount = engine.speed.das
			}*/
			if(shouldUseTimer&&engine.stat!==GameEngine.Status.GAMEOVER) {
				mainTimer--
				if(mainTimer<=600&&mainTimer%60==0) {
					receiver.playSE("countdown")
				}
			}

			// Meter - use as timer.
			if(engine.statistics.level<300) {
				engine.meterValue = mainTimer*1f/TIMER_MAX
				engine.meterColor = GameEngine.METER_COLOR_LIMIT
			} else {
				engine.meterValue = minOf(stock/20f, 1f)
				engine.meterColor = GameEngine.METER_COLOR_LIMIT
			}
			if(engine.statistics.level<300&&mainTimer<=0) {
				engine.playSE("died")
				engine.lives = 0
				engine.stat = GameEngine.Status.GAMEOVER
				engine.resetStatc()
				engine.gameEnded()
			}
			if(engine.statistics.level>=300&&stock<0) {
				engine.playSE("died")
				stock = 0
				engine.lives = 0
				engine.stat = GameEngine.Status.GAMEOVER
				engine.resetStatc()
				engine.gameEnded()
			}
		}
		if(engine.gameStarted) {
//			blockParticles?.update()
			warningText?.update()
			if(warningText?.shouldPurge()==true) warningText = null
			warningTextSecondLine?.update()
			if(warningTextSecondLine?.shouldPurge()==true) warningTextSecondLine = null
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

	/*
     * Calculate score - PAIN
     */
	override fun calcScore(engine:GameEngine, ev:ScoreEvent):Int {
		// Line clear bonus
		// if (linesUsed > 4) linesUsed = 4;
		var res = 0
		val li = ev.lines
		if(li>0) {
			val pts:Int = engine.statistics.time-timeScore
			val flag = li>=4||!ev.twist
			// Add to score
			if(pts>0) {
				// scoreBeforeIncrease = engine.statistics.score;
				lastScore = pts
				timeScore += pts
				lastLine = li
			}

			if(flag||stock>0||engine.statistics.level<=200||engine.statistics.level>=300) {
				// Level up
				engine.statistics.level++
				mainTimer += LEVEL_TIMEBONUS
				if(mainTimer>TIMER_MAX) mainTimer = TIMER_MAX

				if(engine.statistics.level>200&&!flag) {
					res--
					stock--
					if(stock<=4) {
						val destinationX:Int = receiver.scoreX(engine)
						val destinationY:Int = receiver.scoreY(engine,16)
						val colors = arrayOf(COLOR.RED, COLOR.PURPLE)
						warningText = FlyInOutText(
							if(stock>=0) "WARNING: STOCK LOW!" else "STOCK DEPLETED!", destinationX, destinationY,
							15, 60, 15, colors, 1.0f, engine.randSeed+engine.statistics.time, stock>=0
						)
					}
				} else if(engine.statistics.level<=200&&flag) {
					stock++
					res++
				}

				if(engine.statistics.level<=300) {
					calculateEfficiencyGrade(engine)
				}
				// owner.backgroundStatus.fadeSW = true;
				// owner.backgroundStatus.fadeCount = 0;
				// owner.backgroundStatus.nextBg = engine.statistics.level / 5;
				if(engine.statistics.level==200) {
					shouldUseTimer = false
					engine.playSE("medal")
					++engine.owner.bgMan.bg
					val destinationX:Int = receiver.scoreX(engine)
					val destinationY:Int = receiver.scoreY(engine,if(engine.displaySize==0) 18 else 36)
					val colors = arrayOf(COLOR.PINK, COLOR.RED, COLOR.PURPLE)
					warningText = FlyInOutText(
						"WARNING: NON-QUADS", destinationX, destinationY, 9, 162, 9, colors, 1.0f,
						engine.randSeed+engine.statistics.time, true
					)
					warningTextSecondLine = FlyInOutText(
						"REDUCE STOCK!", destinationX+4*if(engine.displaySize==0) 16 else 32,
						destinationY+if(engine.displaySize==0) 16 else 32, 9, 162, 9, colors, 1.0f, engine.randSeed+engine.statistics.time,
						true
					)
				}
				if(engine.statistics.level==300) {
					engine.playSE("endingstart")
					val destinationX:Int = receiver.scoreX(engine)
					val destinationY:Int = receiver.scoreY(engine,if(engine.displaySize==0) 18 else 36)
					val colors = arrayOf(COLOR.YELLOW, COLOR.ORANGE, COLOR.RED)
					warningText = FlyInOutText(
						"OUTSTANDING!", destinationX, destinationY, 15, 120, 15, colors, 1.0f,
						engine.randSeed+engine.statistics.time, true
					)
				}
				setSpeed(engine)
				engine.playSE("levelup")
			}
		}
		return res
	}
	/*
     * Render results screen
     */
	override fun renderResult(engine:GameEngine) {
		drawResultStats(
			engine, receiver, 0, COLOR.BLUE, Statistic.LINES,
			Statistic.LEVEL, Statistic.LPM
		)
		drawResultRank(engine, receiver, 15, COLOR.BLUE, rankingRank)
		receiver.drawMenuFont(engine, 0, 6, "TIME SCORE", COLOR.BLUE)
		receiver.drawMenuFont(engine, 0, 7, "%10s".format(timeScore.toTimeStr))
		receiver.drawMenuFont(engine, 0, 8, "EFFICIENCY", COLOR.BLUE)
		receiver.drawMenuFont(engine, 0, 9, "%10s$$".format("%.2f".format(efficiency*100)))
		if(engine.statistics.level>=300) {
			receiver.drawMenuFont(engine, 0, 10, "GRADE", COLOR.BLUE)
			val dX = 4+receiver.fieldX(engine,3)
			val dY = 52+receiver.fieldY(engine,11.5f)
			//customHolder.drawImage("grades", dX, dY, 64*efficiencyGrade, 0, 64, 48, 255, 255, 255, 255, 1.0f)
		}
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
		if(!owner.replayMode&&!big&&engine.ai==null&&startingStock==0) {
			return updateRanking(engine.statistics.level, engine.statistics.lines, timeScore, engine.playerProp.isLoggedIn)
		}
		return false
	}
	/**
	 * Update rankings
	 *
	 * @param li   Lines
	 * @param time Time
	 */
	private fun updateRanking(lv:Int, li:Int, time:Int, isLoggedIn:Boolean):Boolean {
		rankingRank = checkRanking(lv, li, time)
		if(rankingRank!=-1) {
			// Shift down ranking entries
			for(i in RANKING_MAX-1 downTo rankingRank+1) {
				rankingLevel[i] = rankingLevel[i-1]
				rankingLines[i] = rankingLines[i-1]
				rankingTime[i] = rankingTime[i-1]
			}

			// Add new data
			rankingLevel[rankingRank] = lv
			rankingLines[rankingRank] = li
			rankingTime[rankingRank] = time
		}
		if(isLoggedIn) {
			rankingRankPlayer = checkRankingPlayer(lv, li, time)
			if(rankingRankPlayer!=-1) {
				// Shift down ranking entries
				for(i in RANKING_MAX-1 downTo rankingRankPlayer+1) {
					rankingLevelPlayer[i] = rankingLevelPlayer[i-1]
					rankingLinesPlayer[i] = rankingLinesPlayer[i-1]
					rankingTimePlayer[i] = rankingTimePlayer[i-1]
				}

				// Add new data
				rankingLevelPlayer[rankingRankPlayer] = lv
				rankingLinesPlayer[rankingRankPlayer] = li
				rankingTimePlayer[rankingRankPlayer] = time
			}
		} else rankingRankPlayer = -1
		return rankingRank!=-1||rankingRankPlayer!=-1
	}
	/**
	 * Calculate ranking position
	 *
	 * @param li   Lines
	 * @param time Time
	 * @return Position (-1 if unranked)
	 */
	private fun checkRanking(lv:Int, li:Int, time:Int):Int {
		for(i in 0..<RANKING_MAX) {
			if(lv>rankingLevel[i]) {
				return i
			} else if(lv==rankingLevel[i]&&li>rankingLines[i]) {
				return i
			} else if(lv==rankingLevel[i]&&li==rankingLines[i]&&time<rankingTime[i]) {
				return i
			}
		}
		return -1
	}
	/**
	 * Calculate ranking position
	 *
	 * @param li   Lines
	 * @param time Time
	 * @return Position (-1 if unranked)
	 */
	private fun checkRankingPlayer(lv:Int, li:Int, time:Int):Int {
		for(i in 0..<RANKING_MAX) {
			if(lv>rankingLevelPlayer[i]) {
				return i
			} else if(lv==rankingLevelPlayer[i]&&li>rankingLinesPlayer[i]) {
				return i
			} else if(lv==rankingLevelPlayer[i]&&li==rankingLinesPlayer[i]&&time<rankingTimePlayer[i]) {
				return i
			}
		}
		return -1
	}

	companion object {
		// Speed Tables
		private val ARE_TABLE = intArrayOf(
			15, 15, 15, 15, 14,
			14, 13, 12, 11, 10,
			9, 8, 7, 6, 5,
			15, 13, 10, 10, 9,
			9, 8, 8, 7, 6, 5
		)
		private val LOCK_TABLE = intArrayOf(
			30, 29, 28, 27, 26,
			25, 24, 23, 22, 21,
			20, 19, 18, 17, 17,
			30, 27, 25, 23, 21,
			20, 19, 18, 17, 16,
			15
		)
		// Levels for speed changes
		private val LEVEL_ARE_LOCK_CHANGE = intArrayOf(
			60, 70, 80, 90, 100,
			110, 120, 130, 140, 150,
			160, 170, 180, 190, 200,
			210, 220, 230, 240, 250,
			260, 270, 280, 290, 300,
			2000000000
		)
		// Timer constants
		private const val STARTING_TIMER = 7200
		private const val LEVEL_TIMEBONUS = 900
		private const val TIMER_MAX = 18000
		private val GRADE_BOUNDARIES = floatArrayOf(
			0f,  // F
			0.25f,  // E
			0.375f,  // D
			0.5f,  // C
			0.65f,  // B
			0.75f,  // A
			0.90f,  // S
			1.0f // PF
		)
		private val headerColor = COLOR.RED
	}
}
