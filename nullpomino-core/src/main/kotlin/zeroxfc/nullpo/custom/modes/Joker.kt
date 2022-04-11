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
import mu.nu.nullpo.game.event.EventReceiver
import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.gui.common.AbstractRenderer.FontBadge.Companion.b
import mu.nu.nullpo.gui.common.particles.BlockParticleCollection
import mu.nu.nullpo.util.CustomProperties
import mu.nu.nullpo.util.GeneralUtil.toTimeStr
import zeroxfc.nullpo.custom.libs.FlyInOutText
import zeroxfc.nullpo.custom.libs.backgroundtypes.AnimatedBackgroundHook
import zeroxfc.nullpo.custom.libs.backgroundtypes.BackgroundDiagonalRipple
import zeroxfc.nullpo.custom.libs.backgroundtypes.BackgroundVerticalBars
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
	private var rankingLevel = intArrayOf()
	private var rankingTime = intArrayOf()
	private var rankingLines = intArrayOf()
	// time score
	private var timeScore = 0
	// Local randomiser
	private var localRandom:Random? = null
	// Particle stuff
	private var blockParticles:BlockParticleCollection? = null
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
	private var rankingLevelPlayer = intArrayOf()
	private var rankingTimePlayer = intArrayOf()
	private var rankingLinesPlayer = intArrayOf()
	// Last amount of lines cleared;
	private var lastLine = 0
	// Animated backgrounds
	private var useAnimBG = false
	private val ANIMATED_BACKGROUNDS:Array<AnimatedBackgroundHook> by lazy {
		arrayOf(
			BackgroundVerticalBars(18, 60, 160, 1f, 4f, false),
			BackgroundDiagonalRipple(19, 8, 8, 60, 1f, 2f, false, false)
		)
	}

	override val name:String
		get() = "JOKER"
	/*
     * Initialization
     */
	override fun playerInit(engine:GameEngine, playerID:Int) {
		super.playerInit(engine, playerID)
		efficiency = 0f
		efficiencyGrade = 0
		lastscore = 0
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
		rankingLevel = IntArray(RANKING_MAX)
		rankingLines = IntArray(RANKING_MAX)
		rankingTime = IntArray(RANKING_MAX)
		engine.playerProp.reset()
		showPlayerStats = false

		rankingRankPlayer = -1
		rankingLevelPlayer = IntArray(RANKING_MAX)
		rankingLinesPlayer = IntArray(RANKING_MAX)
		rankingTimePlayer = IntArray(RANKING_MAX)
		netPlayerInit(engine, playerID)
		if(!owner.replayMode) {

			version = CURRENT_VERSION
		} else {
			if(version==0&&owner.replayProp.getProperty("joker.endless", false)) goaltype = 2

			// NET: Load name
			netPlayerName = engine.owner.replayProp.getProperty("$playerID.net.netPlayerName", "")
		}
		engine.owner.backgroundStatus.bg = 18
		engine.framecolor = GameEngine.FRAME_COLOR_PURPLE
		engine.twistEnable = true
		engine.comboType = GameEngine.COMBO_TYPE_NORMAL
		engine.b2b = false
		engine.statistics.levelDispAdd = 0
		engine.speed.gravity = -1
		engine.speed.das = 6
		engine.speed.denominator = 256
		engine.speed.are = 15
		engine.speed.areLine = 15
		engine.speed.lineDelay = 0
		engine.blockShowOutlineOnly = true
		engine.bighalf = true
		engine.bigmove = true

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
	override fun onSetting(engine:GameEngine, playerID:Int):Boolean {

		// NET: Net Ranking
		if(netIsNetRankingDisplayMode) {
			netOnUpdateNetPlayRanking(engine, goaltype)
		} else if(!engine.owner.replayMode) {
			// Configuration changes
			val change = updateCursor(engine, 4, playerID)
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
				// NET: Signal start of the game
				if(netIsNetPlay) netLobby!!.netPlayerClient!!.send("start1p\n")
				return false
			}

			// Cancel
			if(engine.ctrl.isPush(Controller.BUTTON_B)&&!netIsNetPlay) {
				engine.quitflag = true
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
			drawMenu(engine, playerID, receiver, 0, EventReceiver.COLOR.RED, 0, "ST. STOCK" to startingStock)
			drawMenu(engine, playerID, receiver, 2, EventReceiver.COLOR.BLUE, 1, "BIG" to big, "LINE ANIM." to lc)
		}
	}

	override fun onReady(engine:GameEngine, playerID:Int):Boolean {
		mainTimer = STARTING_TIMER
		engine.statistics.level = 50
		stock = startingStock
		timeScore = 0
		efficiencyGrade = 0
		efficiency = 0f
		engine.lives = 0
		if(blockParticles!=null) {
			blockParticles = null
		}
		if(engine.statc[0]==0&&useAnimBG) {
			engine.owner.backgroundStatus.bg = -2
			for(bg in ANIMATED_BACKGROUNDS) {
				bg.reset()
			}
		}
		return false
	}
	/*
     * Called for initialization during "Ready" screen
     */
	override fun startGame(engine:GameEngine, playerID:Int) {
		engine.statistics.levelDispAdd = 0
		engine.ruleOpt.areCancelHold = true
		engine.ruleOpt.areCancelMove = true
		engine.ruleOpt.areCancelRotate = true
		engine.ruleOpt.harddropEnable = true
		engine.ruleOpt.softdropSurfaceLock = true
		engine.ruleOpt.softdropEnable = true
		shouldUseTimer = true
		engine.speed.lineDelay = 0
		lastLine = 0
		engine.ruleOpt.rotateButtonAllowDouble = true
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
		engine.bighalf = true
		engine.bigmove = true
		blockParticles = BlockParticleCollection(engine.field.let {(it.height+it.hiddenHeight)*it.width*2}, lineClearAnimType)
		localRandom = Random(engine.randSeed)

		// l = STARTING_LIVES + lifeOffset + 1;

		// o = false;
		setSpeed(engine)
		owner.bgmStatus.bgm = BGMStatus.BGM.Finale(1)

		owner.bgmStatus.fadesw = false
		if(netIsWatch) {
			owner.bgmStatus.bgm = BGMStatus.BGM.Silent
		}
	}

	override fun renderFirst(engine:GameEngine, playerID:Int) {
		if(useAnimBG&&engine.owner.backgroundStatus.bg<0) {
			ANIMATED_BACKGROUNDS[engine.owner.backgroundStatus.bg+2].draw(engine, playerID)
		}
	}
	/*
     * Render score
     */
	override fun renderLast(engine:GameEngine, playerID:Int) {
		if(owner.menuOnly) return
		receiver.drawScoreFont(engine, playerID, 0, 0, name, EventReceiver.COLOR.RED)
		receiver.drawScoreFont(engine, playerID, 0, 1, "(FINAL TIER)", EventReceiver.COLOR.RED)
		if(engine.stat===GameEngine.Status.SETTING||engine.stat===GameEngine.Status.RESULT&&!owner.replayMode) {
			if(!owner.replayMode&&!big&&engine.ai==null&&startingStock==0) {
				val scale = if(receiver.nextDisplayType==2) 0.5f else 1.0f
				val topY = if(receiver.nextDisplayType==2) 6 else 4
				receiver.drawScoreFont(engine, playerID, 3, topY-1, "LEVEL  LINE TIME", EventReceiver.COLOR.BLUE, scale)
				if(showPlayerStats) {
					for(i in 0 until RANKING_MAX) {
						receiver.drawScoreFont(engine, playerID, 0, topY+i, String.format("%2d", i+1), EventReceiver.COLOR.YELLOW, scale)
						val s = "${rankingLevelPlayer[i]}"
						receiver.drawScoreFont(
							engine, playerID, if(s.length>6&&receiver.nextDisplayType!=2) 6 else 3,
							if(s.length>6&&receiver.nextDisplayType!=2) (topY+i)*2 else topY+i, s, i==rankingRankPlayer,
							if(s.length>6&&receiver.nextDisplayType!=2) scale*0.5f else scale
						)
						receiver.drawScoreFont(
							engine, playerID, 10, topY+i, "${rankingLinesPlayer[i]}", i==rankingRankPlayer,
							scale
						)
						receiver.drawScoreFont(engine, playerID, 15, topY+i, rankingTimePlayer[i].toTimeStr, i==rankingRankPlayer, scale)
					}
					receiver.drawScoreFont(engine, playerID, 0, topY+RANKING_MAX+1, "PLAYER SCORES", EventReceiver.COLOR.BLUE)
					receiver.drawScoreFont(
						engine, playerID, 0, topY+RANKING_MAX+2, engine.playerProp.nameDisplay,
						EventReceiver.COLOR.WHITE, 2f
					)
					receiver.drawScoreFont(engine, playerID, 0, topY+RANKING_MAX+5, "F:SWITCH RANK SCREEN", EventReceiver.COLOR.GREEN)
				} else {
					for(i in 0 until RANKING_MAX) {
						receiver.drawScoreFont(engine, playerID, 0, topY+i, String.format("%2d", i+1), EventReceiver.COLOR.YELLOW, scale)
						val s = "${rankingLevel[i]}"
						receiver.drawScoreFont(
							engine, playerID, if(s.length>6&&receiver.nextDisplayType!=2) 6 else 3,
							if(s.length>6&&receiver.nextDisplayType!=2) (topY+i)*2 else topY+i, s, i==rankingRank,
							if(s.length>6&&receiver.nextDisplayType!=2) scale*0.5f else scale
						)
						receiver.drawScoreFont(engine, playerID, 10, topY+i, "${rankingLines[i]}", i==rankingRank, scale)
						receiver.drawScoreFont(engine, playerID, 15, topY+i, rankingTime[i].toTimeStr, i==rankingRank, scale)
					}
					receiver.drawScoreFont(engine, playerID, 0, topY+RANKING_MAX+1, "LOCAL SCORES", EventReceiver.COLOR.BLUE)
					if(!engine.playerProp.isLoggedIn) receiver.drawScoreFont(
						engine, playerID, 0, topY+RANKING_MAX+2,
						"(NOT LOGGED IN)\n(E:LOG IN)"
					)
					if(engine.playerProp.isLoggedIn) receiver.drawScoreFont(
						engine, playerID, 0, topY+RANKING_MAX+5,
						"F:SWITCH RANK SCREEN", EventReceiver.COLOR.GREEN
					)
				}
			}
		} else if(engine.stat===GameEngine.Status.CUSTOM) {
			engine.playerProp.loginScreen.renderScreen(receiver, engine, playerID)
		} else {
			receiver.drawScoreFont(engine, playerID, 0, 3, "TIME", EventReceiver.COLOR.BLUE)
			val strScore = "${timeScore.toTimeStr}(+${(engine.statistics.time-timeScore).toTimeStr})"
			receiver.drawScoreFont(engine, playerID, 0, 4, strScore)
			receiver.drawScoreFont(engine, playerID, 0, 6, "LINE", EventReceiver.COLOR.BLUE)
			receiver.drawScoreFont(engine, playerID, 0, 7, "${engine.statistics.lines}")
			receiver.drawScoreFont(engine, playerID, 0, 9, "LEVEL", EventReceiver.COLOR.BLUE)
			receiver.drawScoreFont(engine, playerID, 0, 10, "${engine.statistics.level}")

			receiver.drawScoreFont(engine, playerID, 0, 12, "STOCK", EventReceiver.COLOR.GREEN)
			receiver.drawScoreFont(engine, playerID, 0, 13, "$stock"+b, (stock<=2))
			receiver.drawScoreFont(engine, playerID, 0, 15, "EFFICIENCY", EventReceiver.COLOR.GREEN)
			receiver.drawScoreNum(
				engine, playerID, 0, 16, String.format("%.2f", efficiency*100)+"%",
				if(engine.statistics.level>=300) EventReceiver.COLOR.PINK else EventReceiver.COLOR.WHITE
			)

			if(engine.playerProp.isLoggedIn||engine.playerName.isNotEmpty()) {
				receiver.drawScoreFont(engine, playerID, 0, 18, "PLAYER", EventReceiver.COLOR.BLUE)
				receiver.drawScoreFont(
					engine, playerID, 0, 19,
					if(owner.replayMode) engine.playerName else engine.playerProp.nameDisplay,
					EventReceiver.COLOR.WHITE, 2f
				)
			}
			if(shouldUseTimer) {
				receiver.drawMenuFont(engine, playerID, 0, 21, "TIME LIMIT", EventReceiver.COLOR.RED)
				receiver.drawMenuFont(engine, playerID, 1, 22, mainTimer.toTimeStr, mainTimer<=600&&mainTimer/2%2==0)
			}

			blockParticles?.drawAll(engine, receiver, playerID)
			warningText?.draw(engine, receiver, playerID)
			warningTextSecondLine?.draw(engine, receiver, playerID)
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

	override fun onGameOver(engine:GameEngine, playerID:Int):Boolean {
		shouldUseTimer = false
		return false
	}

	override fun onCustom(engine:GameEngine, playerID:Int):Boolean {
		showPlayerStats = false
		engine.isInGame = true
		val s:Boolean = engine.playerProp.loginScreen.updateScreen(engine, playerID)
		if(engine.playerProp.isLoggedIn) {
			loadRankingPlayer(engine.playerProp, engine.ruleOpt.strRuleName)
			loadSetting(engine.playerProp.propProfile, engine)
		}
		if(engine.stat===GameEngine.Status.SETTING) engine.isInGame = false
		return s
	}
	/*
     * Called after every frame
     */
	override fun onLast(engine:GameEngine, playerID:Int) {
		super.onLast(engine, playerID)
		if(useAnimBG&&engine.owner.backgroundStatus.bg<0) {
			ANIMATED_BACKGROUNDS[engine.owner.backgroundStatus.bg+2].update()
		}
		if(engine.gameStarted&&engine.ending==0) {
			if(engine.stat===GameEngine.Status.ARE) {
				engine.dasCount = engine.speed.das
			}
			if(shouldUseTimer&&engine.stat!==GameEngine.Status.GAMEOVER) {
				mainTimer--
				if(mainTimer<=600&&mainTimer%60==0) {
					receiver.playSE("countdown")
				}
			}

			// Meter - use as timer.
			if(engine.statistics.level<200) {
				engine.meterValue = (mainTimer/TIMER_MAX.toDouble()*receiver.getMeterMax(engine)).toInt()
				engine.meterColor = GameEngine.METER_COLOR_GREEN
				if(mainTimer in 3601..5399) engine.meterColor = GameEngine.METER_COLOR_YELLOW
				if(mainTimer in 1801..3600) engine.meterColor = GameEngine.METER_COLOR_ORANGE
				if(mainTimer<=1800) engine.meterColor = GameEngine.METER_COLOR_RED
			} else {
				val meterMax = 20
				engine.meterValue = (stock/meterMax.toDouble()*receiver.getMeterMax(engine)).toInt()
				engine.meterColor = GameEngine.METER_COLOR_GREEN
				if(stock in 11..15) engine.meterColor = GameEngine.METER_COLOR_YELLOW
				if(stock in 6..10) engine.meterColor = GameEngine.METER_COLOR_ORANGE
				if(stock<=5) engine.meterColor = GameEngine.METER_COLOR_RED
			}
			if(mainTimer<=0) {
				if(engine.statistics.level<200) {
					engine.playSE("died")
					engine.lives = 0
					engine.stat = GameEngine.Status.GAMEOVER
					engine.resetStatc()
					engine.gameEnded()
				}
			}
		}
		if(engine.gameStarted) {
			blockParticles?.update()
			warningText?.update()
			if(warningText?.shouldPurge()==true) warningText = null
			warningTextSecondLine?.update()
			if(warningTextSecondLine?.shouldPurge()==true) warningTextSecondLine = null
		}
		if(stock<0) {
			engine.playSE("died")
			stock = 0
			engine.lives = 0
			engine.stat = GameEngine.Status.GAMEOVER
			engine.resetStatc()
			engine.gameEnded()
		}
		if(engine.stat===GameEngine.Status.SETTING||engine.stat===GameEngine.Status.RESULT&&!owner.replayMode||engine.stat===GameEngine.Status.CUSTOM) {
			// Show rank
			if(engine.ctrl.isPush(Controller.BUTTON_F)&&engine.playerProp.isLoggedIn&&engine.stat!==GameEngine.Status.CUSTOM) {
				showPlayerStats = !showPlayerStats
				engine.playSE("change")
			}
		}
		if(engine.quitflag) {
			engine.playerProp.reset()
		}
	}

	/*override fun onLineClear(engine:GameEngine, playerID:Int):Boolean {
		//  event 発生
		owner.receiver.onLineClear(engine, playerID)
		engine.checkDropContinuousUse()

		// 横溜め
		if(engine.ruleOpt.dasInLineClear) engine.padRepeat() else if(engine.ruleOpt.dasRedirectInDelay) {
			engine.dasRedirect()
		}

		// 最初の frame
		if(engine.statc.get(0)==0) {
			// Line clear flagを設定
			if(engine.clearMode===GameEngine.ClearType.LINE) engine.lineClearing = engine.field.checkLine() else if(engine.clearMode===GameEngine.CLEAR_COLOR) engine.lineClearing = engine.field.checkColor(
				engine.colorClearSize, true, engine.garbageColorClear, engine.gemSameColor,
				engine.ignoreHidden) else if(engine.clearMode===GameEngine.ClearType.LINE_COLOR) engine.lineClearing = engine.field.checkLineColor(
				engine.colorClearSize, true, engine.lineColorDiagonals,
				engine.gemSameColor) else if(engine.clearMode===GameEngine.ClearType.GEM_COLOR) engine.lineClearing = engine.field.gemColorCheck(
				engine.colorClearSize, true, engine.garbageColorClear, engine.ignoreHidden)

			// Linescountを決める
			var li:Int = engine.lineClearing
			if(big&&engine.bighalf) li = li shr 1
			//if(li > 4) li = 4;
			if(engine.twist) {
				engine.playSE("tspin$li")
				if(engine.ending==0||engine.staffrollEnableStatistics) {
					if(li==1&&engine.twistmini) engine.statistics.totalTSpinSingleMini++
					if(li==1&&!engine.twistmini) engine.statistics.totalTSpinSingle++
					if(li==2&&engine.twistmini) engine.statistics.totalTSpinDoubleMini++
					if(li==2&&!engine.twistmini) engine.statistics.totalTSpinDouble++
					if(li==3) engine.statistics.totalTSpinTriple++
				}
			} else {
				if(engine.clearMode===GameEngine.CLEAR_LINE) engine.playSE("erase$li")
				if(engine.ending==0||engine.staffrollEnableStatistics) {
					if(li==1) engine.statistics.totalSingle++
					if(li==2) engine.statistics.totalDouble++
					if(li==3) engine.statistics.totalTriple++
					if(li==4) engine.statistics.totalFour++
				}
			}

			// B2B bonus
			if(engine.b2bEnable) {
				if(engine.twist||li>=4) {
					engine.b2bcount++
					if(engine.b2bcount==1) {
						engine.playSE("b2b_start")
					} else {
						engine.b2b = true
						engine.playSE("b2b_continue")
						if(engine.ending==0||engine.staffrollEnableStatistics) {
							if(li==4) engine.statistics.totalB2BFour++ else engine.statistics.totalB2BTSpin++
						}
					}
				} else if(engine.b2bcount!=0) {
					engine.b2b = false
					engine.b2bcount = 0
					engine.playSE("b2b_end")
				}
			}

			// Combo
			if(engine.comboType!=GameEngine.COMBO_TYPE_DISABLE&&engine.chain==0) {
				if(engine.comboType==GameEngine.COMBO_TYPE_NORMAL||engine.comboType==GameEngine.COMBO_TYPE_DOUBLE&&li>=2) engine.combo++
				if(engine.combo>=2) {
					var cmbse:Int = engine.combo-1
					if(cmbse>20) cmbse = 20
					engine.playSE("combo$cmbse")
				}
				if(engine.ending==0||engine.staffrollEnableStatistics) {
					if(engine.combo>engine.statistics.maxCombo) engine.statistics.maxCombo = engine.combo
				}
			}
			engine.lineGravityTotalLines += engine.lineClearing
			if(engine.ending==0||engine.staffrollEnableStatistics) engine.statistics.lines += li
			if(engine.field.howManyGemClears>0) engine.playSE("gem")

			// Calculate score
			calcScore(engine, playerID, li)
			owner.receiver.calcScore(engine, playerID, li)

			// Blockを消す演出を出す (まだ実際には消えていない）
			if(engine.clearMode===GameEngine.CLEAR_LINE) {
				var cY = 0
				for(i in 0 until engine.field.height) {
					if(engine.field.getLineFlag(i)) {
						cY++
						for(j in 0 until engine.field.width) {
							val blk = engine.field.getBlock(j, i)
							if(blk!=null) {
								if(blockParticles!=null) {
									when(lineClearAnimType) {
										0 -> blockParticles.addBlock(engine, receiver, playerID, blk, j, i, 10, 90, li>=4, localRandom)
										1 -> blockParticles.addBlock(engine, receiver, playerID, blk, j, i, engine.field.width, cY, li, 120)
										else -> {
										}
									}
								}
							}
						}
					}
				}
			} else if(engine.clearMode===GameEngine.CLEAR_LINE_COLOR||engine.clearMode===GameEngine.CLEAR_COLOR||engine.clearMode===GameEngine.CLEAR_GEM_COLOR) for(i in 0 until engine.field.height) {
				for(j in 0 until engine.field.width) {
					val blk = engine.field.getBlock(j, i) ?: continue
					if(blk.getAttribute(Block.ATTRIBUTE.ERASE)) {
						//blockParticles.addBlock(engine, receiver, playerID, blk, j, i, 4, 60, localRandom);
						if(engine.displaysize==1) {
							//owner.receiver.blockBreak(engine, 2*j, 2*i, blk);
							//owner.receiver.blockBreak(engine, 2*j+1, 2*i, blk);
							//owner.receiver.blockBreak(engine, 2*j, 2*i+1, blk);
							//owner.receiver.blockBreak(engine, 2*j+1, 2*i+1, blk);
							when(lineClearAnimType) {
								0 -> {
									blockParticles.addBlock(engine, receiver, playerID, blk, 2*j, 2*i, 10, 90, false, localRandom)
									blockParticles.addBlock(engine, receiver, playerID, blk, 2*j+1, 2*i, 10, 90, false, localRandom)
									blockParticles.addBlock(engine, receiver, playerID, blk, 2*j, 2*i+1, 10, 90, false, localRandom)
									blockParticles.addBlock(engine, receiver, playerID, blk, 2*j+1, 2*i+1, 10, 90, false, localRandom)
								}
								1 -> {
									blockParticles.addBlock(engine, receiver, playerID, blk, 2*j, 2*i, engine.field.width, 1, 1, 120)
									blockParticles.addBlock(engine, receiver, playerID, blk, 2*j+1, 2*i, engine.field.width, 1, 1, 120)
									blockParticles.addBlock(engine, receiver, playerID, blk, 2*j, 2*i+1, engine.field.width, 1, 1, 120)
									blockParticles.addBlock(engine, receiver, playerID, blk, 2*j+1, 2*i+1, engine.field.width, 1, 1, 120)
								}
								else -> {
								}
							}
						} else when(lineClearAnimType) {
							0 -> blockParticles.addBlock(engine, receiver, playerID, blk, j, i, 10, 90, false, localRandom)
							1 -> blockParticles.addBlock(engine, receiver, playerID, blk, j, i, engine.field.width, 1, 1, 120)
							else -> {
							}
						}
					}
				}
			}

			// Blockを消す
			if(engine.clearMode===GameEngine.CLEAR_LINE) engine.field.clearLine() else if(engine.clearMode===GameEngine.CLEAR_COLOR) engine.field.clearColor(
				engine.colorClearSize, engine.garbageColorClear, engine.gemSameColor,
				engine.ignoreHidden) else if(engine.clearMode===GameEngine.CLEAR_LINE_COLOR) engine.field.clearLineColor(
				engine.colorClearSize, engine.lineColorDiagonals,
				engine.gemSameColor) else if(engine.clearMode===GameEngine.CLEAR_GEM_COLOR) engine.lineClearing = engine.field.gemClearColor(
				engine.colorClearSize, engine.garbageColorClear, engine.ignoreHidden)
		}

		// Linesを1段落とす
		if(engine.lineGravityType===GameEngine.LINE_GRAVITY_NATIVE&&
			engine.lineDelay>=engine.lineClearing-1&&engine.statc.get(
				0)>=engine.lineDelay-(engine.lineClearing-1)&&engine.ruleOpt.lineFallAnim) {
			engine.field.downFloatingBlocksSingleLine()
		}

		// Line delay cancel check
		engine.delayCancelMoveLeft = engine.ctrl.isPush(Controller.BUTTON_LEFT)
		engine.delayCancelMoveRight = engine.ctrl.isPush(Controller.BUTTON_RIGHT)
		val moveCancel = engine.ruleOpt.lineCancelMove&&(engine.ctrl.isPush(engine.up)||
			engine.ctrl.isPush(engine.down)||engine.delayCancelMoveLeft||engine.delayCancelMoveRight)
		val rotateCancel = engine.ruleOpt.lineCancelRotate&&(engine.ctrl.isPush(Controller.BUTTON_A)||
			engine.ctrl.isPush(Controller.BUTTON_B)||engine.ctrl.isPush(Controller.BUTTON_C)||
			engine.ctrl.isPush(Controller.BUTTON_E))
		val holdCancel = engine.ruleOpt.lineCancelHold&&engine.ctrl.isPush(Controller.BUTTON_D)
		engine.delayCancel = moveCancel||rotateCancel||holdCancel
		if(engine.statc.get(0)<engine.lineDelay&&engine.delayCancel) {
			engine.statc.get(0) = engine.lineDelay
		}

		// Next ステータス
		if(engine.statc.get(0)>=engine.lineDelay) {
			// Cascade
			if(engine.lineGravityType===GameEngine.LINE_GRAVITY_CASCADE||engine.lineGravityType===GameEngine.LINE_GRAVITY_CASCADE_SLOW) {
				if(engine.statc.get(6)<engine.cascadeDelay) {
					engine.statc.get(6)++
					return true
				} else if(engine.field.doCascadeGravity(engine.lineGravityType)) {
					engine.statc.get(6) = 0
					return true
				} else if(engine.statc.get(6)<engine.cascadeClearDelay) {
					engine.statc.get(6)++
					return true
				} else if(engine.clearMode===GameEngine.CLEAR_LINE&&engine.field.checkLineNoFlag()>0||
					engine.clearMode===GameEngine.CLEAR_COLOR&&engine.field.checkColor(engine.colorClearSize, false,
						engine.garbageColorClear, engine.gemSameColor, engine.ignoreHidden)>0||
					engine.clearMode===GameEngine.CLEAR_LINE_COLOR&&engine.field.checkLineColor(engine.colorClearSize, false,
						engine.lineColorDiagonals, engine.gemSameColor)>0||
					engine.clearMode===GameEngine.CLEAR_GEM_COLOR&&engine.field.gemColorCheck(engine.colorClearSize, false,
						engine.garbageColorClear, engine.ignoreHidden)>0) {
					engine.twist = false
					engine.twistmini = false
					engine.chain++
					if(engine.chain>engine.statistics.maxChain) engine.statistics.maxChain = engine.chain
					engine.statc.get(0) = 0
					engine.statc.get(6) = 0
					return true
				}
			}
			val skip:Boolean
			skip = lineClearEnd(engine, playerID)
			owner.receiver.lineClearEnd(engine, playerID)
			if(!skip) {
				if(engine.lineGravityType===GameEngine.LINE_GRAVITY_NATIVE) engine.field.downFloatingBlocks()
				engine.playSE("linefall")
				engine.field.lineColorsCleared = null
				if(engine.stat===GameEngine.Status.LINECLEAR||engine.versionMajor<=6.3f) {
					engine.resetStatc()
					if(engine.ending==1) {
						// Ending
						engine.stat = GameEngine.Status.ENDINGSTART
					} else if(engine.getARELine()>0||engine.lagARE) {
						// AREあり
						engine.statc.get(0) = 0
						engine.statc.get(1) = engine.getARELine()
						engine.statc.get(2) = 1
						engine.stat = GameEngine.Status.ARE
					} else if(engine.interruptItemNumber!=GameEngine.INTERRUPTITEM_NONE) {
						// 中断効果のあるアイテム処理
						engine.nowPieceObject = null
						engine.interruptItemPreviousStat = GameEngine.Status.MOVE
						engine.stat = GameEngine.Status.INTERRUPTITEM
					} else {
						// AREなし
						engine.nowPieceObject = null
						if(engine.versionMajor<7.5f) engine.initialRotate() //XXX: Weird IRS thing on lines cleared but no ARE
						engine.stat = GameEngine.Status.MOVE
					}
				}
			}
			return true
		}
		engine.statc.get(0)++
		return true
	}
	*/
	/*
     * Calculate score - PAIN
     */
	override fun calcScore(engine:GameEngine, playerID:Int, lines:Int):Int {
		// Line clear bonus
		// if (linesUsed > 4) linesUsed = 4;
		var res = 0
		if(lines>0) {
			val pts:Int = engine.statistics.time-timeScore

			// Add to score
			if(pts>0) {
				// scoreBeforeIncrease = engine.statistics.score;
				lastscore = pts
				timeScore += pts
				lastLine = lines
			}
		}
		if(lines>0) {
			// Level up
			engine.statistics.level++

			// owner.backgroundStatus.fadesw = true;
			// owner.backgroundStatus.fadecount = 0;
			// owner.backgroundStatus.fadebg = engine.statistics.level / 5;
			if(engine.statistics.level==200) {
				shouldUseTimer = false
				engine.playSE("medal")
				++engine.owner.backgroundStatus.bg
				val destinationX:Int = receiver.scoreX(engine, playerID)
				val destinationY:Int = receiver.scoreY(engine, playerID)+18*if(engine.displaysize==0) 16 else 32
				val colors = arrayOf(EventReceiver.COLOR.PINK, EventReceiver.COLOR.RED, EventReceiver.COLOR.PURPLE)
				warningText = FlyInOutText(
					"WARNING: NON-TETRISES", destinationX, destinationY, 9, 162, 9, colors, 1.0f,
					engine.randSeed+engine.statistics.time, true
				)
				warningTextSecondLine = FlyInOutText(
					"REDUCE STOCK!", destinationX+4*if(engine.displaysize==0) 16 else 32,
					destinationY+if(engine.displaysize==0) 16 else 32, 9, 162, 9, colors, 1.0f, engine.randSeed+engine.statistics.time,
					true
				)
			}
			if(engine.statistics.level<200) {
				mainTimer += LEVEL_TIMEBONUS
				if(mainTimer>TIMER_MAX) mainTimer = TIMER_MAX
			}
			if(engine.statistics.level==300) {
				engine.playSE("endingstart")
				val destinationX:Int = receiver.scoreX(engine, playerID)
				val destinationY:Int = receiver.scoreY(engine, playerID)+18*if(engine.displaysize==0) 16 else 32
				val colors = arrayOf(EventReceiver.COLOR.YELLOW, EventReceiver.COLOR.ORANGE, EventReceiver.COLOR.RED)
				warningText = FlyInOutText(
					"CONGRATULATIONS!", destinationX, destinationY, 15, 120, 15, colors, 1.0f,
					engine.randSeed+engine.statistics.time, true
				)
			}
			if(lines<4) {
				if(engine.statistics.level>200) {
					res--
					stock--
					if(stock<=2) {
						if(stock>=0) engine.playSE("danger")
						val destinationX:Int = receiver.scoreX(engine, playerID)
						val destinationY:Int = receiver.scoreY(engine, playerID)+18*if(engine.displaysize==0) 16 else 32
						val colors = arrayOf(EventReceiver.COLOR.RED, EventReceiver.COLOR.PURPLE)
						warningText = FlyInOutText(
							if(stock>=0) "WARNING: STOCK LOW!" else "STOCK DEPLETED!", destinationX, destinationY,
							15, 60, 15, colors, 1.0f, engine.randSeed+engine.statistics.time, stock>=0
						)
					}
				}
			} else {
				if(engine.statistics.level<=200) {
					stock++
					res++
				}
			}
			if(engine.statistics.level<=300) {
				calculateEfficiencyGrade(engine)
			}
			setSpeed(engine)
			engine.playSE("levelup")
		}
		return res
	}
	/*
     * Render results screen
     */
	override fun renderResult(engine:GameEngine, playerID:Int) {
		drawResultStats(
			engine, playerID, receiver, 0, EventReceiver.COLOR.BLUE,
			Statistic.LINES, Statistic.LEVEL, Statistic.LPM
		)
		drawResultRank(engine, playerID, receiver, 15, EventReceiver.COLOR.BLUE, rankingRank)
		receiver.drawMenuFont(engine, playerID, 0, 6, "TIME SCORE", EventReceiver.COLOR.BLUE)
		receiver.drawMenuFont(engine, playerID, 0, 7, String.format("%10s", timeScore.toTimeStr))
		receiver.drawMenuFont(engine, playerID, 0, 8, "EFFICIENCY", EventReceiver.COLOR.BLUE)
		receiver.drawMenuFont(engine, playerID, 0, 9, String.format("%10s", String.format("%.2f", efficiency*100)+"%"))
		if(engine.statistics.level>=300) {
			receiver.drawMenuFont(engine, playerID, 0, 10, "GRADE", EventReceiver.COLOR.BLUE)
			val dX:Int = 4+receiver.fieldX(engine, playerID)+3*16
			val dY:Int = 52+receiver.fieldY(engine, playerID)+(11.5*16).toInt()
			//customHolder.drawImage("grades", dX, dY, 64*efficiencyGrade, 0, 64, 48, 255, 255, 255, 255, 1.0f)
		}
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
	/*
     * Called when saving replay
     */
	override fun saveReplay(engine:GameEngine, playerID:Int, prop:CustomProperties):Boolean {
		saveSetting(prop, engine)

		// NET: Save name
		if(netPlayerName!=null&&netPlayerName!!.isNotEmpty()) {
			prop.setProperty("$playerID.net.netPlayerName", netPlayerName)
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
		for(i in 0 until RANKING_MAX) {
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
		for(i in 0 until RANKING_MAX) {
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
			15, 15, 15, 15, 14, 14,
			13, 12, 11, 10, 9,
			8, 7, 6, 5, 15,
			13, 10, 10, 9, 9,
			8, 8, 7, 6, 5
		)
		private val LOCK_TABLE = intArrayOf(
			30, 29, 28, 27, 26, 25,
			24, 23, 22, 21, 20,
			19, 18, 17, 17, 30,
			27, 25, 23, 21, 20,
			19, 18, 17, 16, 15
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
		private val headerColor = EventReceiver.COLOR.RED
	}
}
