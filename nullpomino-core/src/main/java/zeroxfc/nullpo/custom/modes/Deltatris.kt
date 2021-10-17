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
import mu.nu.nullpo.game.subsystem.mode.menu.DelegateMenuItem
import mu.nu.nullpo.game.subsystem.mode.menu.MenuList
import mu.nu.nullpo.game.subsystem.mode.menu.StringsMenuItem
import mu.nu.nullpo.gui.slick.RendererExtension
import mu.nu.nullpo.util.CustomProperties
import mu.nu.nullpo.util.GeneralUtil.toTimeStr
import zeroxfc.nullpo.custom.libs.GameTextUtilities
import zeroxfc.nullpo.custom.libs.Interpolation
import zeroxfc.nullpo.custom.libs.ShakingText
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.random.Random

class Deltatris:MarathonModeBase() {
	private var stext:ShakingText = ShakingText()

	override val itemMode = StringsMenuItem("difficulty", "Difficulty", EventReceiver.COLOR.RED, 1, difficultyName)
	private var difficulty:Int by DelegateMenuItem(itemMode)
	private var multiplier = 1.0
	private var grav = 0.0
	private var mScale = 1.0f
	private var scorebefore = 0
	// Generic
	private var rankingScore:Array<IntArray> = emptyArray()
	private var rankingTime:Array<IntArray> = emptyArray()
	private var rankingLines:Array<IntArray> = emptyArray()
	// PROFILE
	private var rankingRankPlayer = 0
	private var rankingScorePlayer:Array<IntArray> = emptyArray()
	private var rankingTimePlayer:Array<IntArray> = emptyArray()
	private var rankingLinesPlayer:Array<IntArray> = emptyArray()
	/**
	 * The good hard drop effect
	 */
	private var pCoordList:MutableList<IntArray> = mutableListOf()
	/**
	 * Deltatris - How fast can you go in this ΔMAX-inspired gamemode?
	 *
	 * @return Mode name
	 */
	override val name:String get() = "DeltaTris"
	// help me i forgot how to make modes
	// Initialization
	override val menu:MenuList = MenuList("deltatris", itemMode, itemBig)
	override val rankMap:Map<String, IntArray>
		get() = mapOf(
			*((rankingScore.mapIndexed {a, x -> "$a.score" to x}+
				rankingLines.mapIndexed {a, x -> "$a.lines" to x}+
				rankingTime.mapIndexed {a, x -> "$a.time" to x}).toTypedArray())
		)
	override val rankPersMap:Map<String, IntArray>
		get() = mapOf(*((rankingScorePlayer.mapIndexed {a, x -> "$a.score" to x}+
			rankingLinesPlayer.mapIndexed {a, x -> "$a.lines" to x}+
			rankingTimePlayer.mapIndexed {a, x -> "$a.time" to x}).toTypedArray())
		)

	override fun playerInit(engine:GameEngine, playerID:Int) {
		super.playerInit(engine, playerID)
		lastscore = 0
		bgmlv = 0
		multiplier = 1.0
		mScale = 1.0f
		grav = START_GRAVITY.toDouble()
		scorebefore = 0
		difficulty = 1
		rankingRank = -1
		rankingScore = Array(RANKING_TYPE) {IntArray(RANKING_MAX)}
		rankingLines = Array(RANKING_TYPE) {IntArray(RANKING_MAX)}
		rankingTime = Array(RANKING_TYPE) {IntArray(RANKING_MAX)}
		engine.playerProp.reset()
		showPlayerStats = false

		rankingRankPlayer = -1
		rankingScorePlayer = Array(RANKING_TYPE) {IntArray(RANKING_MAX)}
		rankingLinesPlayer = Array(RANKING_TYPE) {IntArray(RANKING_MAX)}
		rankingTimePlayer = Array(RANKING_TYPE) {IntArray(RANKING_MAX)}
		pCoordList = ArrayList()
		netPlayerInit(engine, playerID)
		if(!owner.replayMode) version = CURRENT_VERSION else {
			if(version==0&&owner.replayProp.getProperty("deltatris.endless", false)) goaltype = 2
			// NET: Load name
			netPlayerName = engine.owner.replayProp.getProperty("$playerID.net.netPlayerName", "")
		}
		engine.owner.backgroundStatus.bg = 0
		engine.framecolor = GameEngine.FRAME_COLOR_GRAY
	}
	/**
	 * Set the overall game speed
	 *
	 * @param engine GameEngine
	 */
	override fun setSpeed(engine:GameEngine) {
		var percentage:Double = engine.statistics.totalPieceLocked/PIECES_MAX[difficulty].toDouble()
		if(percentage>1.0) percentage = 1.0
		if(engine.speed.gravity<GRAVITY_DENOMINATOR*20) {
			grav *= GRAVITY_MULTIPLIERS[difficulty]
			grav = minOf(grav, GRAVITY_DENOMINATOR*20.0)
		}
		engine.speed.gravity = grav.toInt()
		engine.speed.are = ceil(
			Interpolation.lerp(START_ARE.toDouble(), END_ARE[difficulty].toDouble(), percentage)
		).toInt()
		engine.speed.areLine = ceil(
			Interpolation.lerp(
				START_LINE_ARE.toDouble(), END_LINE_ARE[difficulty].toDouble(),
				percentage
			)
		).toInt()
		engine.speed.lineDelay = ceil(
			Interpolation.smoothStep(
				START_LINE_DELAY.toDouble(),
				END_LINE_DELAY[difficulty].toDouble(), percentage
			)
		).toInt()
		engine.speed.das = floor(
			Interpolation.smoothStep(START_DAS.toDouble(), END_DAS[difficulty].toDouble(), percentage)
		)
			.toInt()
		engine.speed.lockDelay = ceil(
			Interpolation.smoothStep(
				START_LOCK_DELAY.toDouble(),
				END_LOCK_DELAY[difficulty].toDouble(), percentage
			)
		).toInt()
		engine.speed.denominator = GRAVITY_DENOMINATOR
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
			val change = updateMenu(engine)
			if(change!=0) {
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
		val s:Boolean = engine.playerProp.loginScreen.updateScreen(engine, playerID)
		if(engine.playerProp.isLoggedIn) {
			loadRankingPlayer(engine.playerProp, engine.ruleOpt.strRuleName)
			loadSetting(engine.playerProp.propProfile)
		}
		if(engine.stat===GameEngine.Status.SETTING) engine.isInGame = false
		return s
	}

	override fun onFirst(engine:GameEngine, playerID:Int) {
		pCoordList.clear()
	}
	/*
     * Called for initialization during "Ready" screen
     */
	override fun startGame(engine:GameEngine, playerID:Int) {
		engine.statistics.level = 0
		engine.statistics.levelDispAdd = 1
		engine.b2bEnable = true
		scorebefore = 0
		stext = ShakingText(Random(engine.randSeed))
		multiplier = MULTIPLIER_MINIMUM
		engine.comboType = GameEngine.COMBO_TYPE_NORMAL
		engine.big = big
		engine.twistAllowKick = true
		engine.twistEnable = true
		engine.useAllSpinBonus = true
		engine.twistEnableEZ = true
		setSpeed(engine)
		grav = START_GRAVITY.toDouble()
		if(netIsWatch) {
			owner.bgmStatus.bgm = BGMStatus.BGM.Silent
		}
	}

	override fun onMove(engine:GameEngine, playerID:Int):Boolean {
		if(engine.statc[0]>engine.speed.lockDelay*3) {
			multiplier = maxOf(1.0, multiplier*0.99225)
		}
		return false
	}
	/*
     * Render score
     */
	override fun renderLast(engine:GameEngine, playerID:Int) {
		if(owner.menuOnly) return
		receiver.drawScoreFont(engine, playerID, 0, 0, name, EventReceiver.COLOR.RED)
		receiver.drawScoreFont(
			engine, playerID, 0, 1, "(${Companion.difficultyName[difficulty]} DIFFICULTY)",
			EventReceiver.COLOR.RED
		)
		if(engine.stat===GameEngine.Status.SETTING||engine.stat===GameEngine.Status.RESULT&&!owner.replayMode) {
			if(!owner.replayMode&&!big&&engine.ai==null) {
				val scale = if(receiver.nextDisplayType==2) 0.5f else 1.0f
				val topY = if(receiver.nextDisplayType==2) 6 else 4
				receiver.drawScoreFont(engine, playerID, 3, topY-1, "SCORE  LINE TIME", EventReceiver.COLOR.BLUE, scale)
				if(showPlayerStats) {
					for(i in 0 until RANKING_MAX) {
						receiver.drawScoreFont(engine, playerID, 0, topY+i, String.format("%2d", i+1), EventReceiver.COLOR.YELLOW, scale)
						receiver.drawScoreFont(
							engine, playerID, 3, topY+i, "${rankingScorePlayer[difficulty][i]}",
							i==rankingRankPlayer, scale
						)
						receiver.drawScoreFont(
							engine, playerID, 10, topY+i, "${rankingLinesPlayer[difficulty][i]}",
							i==rankingRankPlayer, scale
						)
						receiver.drawScoreFont(
							engine, playerID, 15, topY+i,
							rankingTimePlayer[difficulty][i].toTimeStr, i==rankingRankPlayer, scale
						)
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
						receiver.drawScoreFont(engine, playerID, 3, topY+i, "${rankingScore[difficulty][i]}", i==rankingRank, scale)
						receiver.drawScoreFont(
							engine, playerID, 10, topY+i, "${rankingLines[difficulty][i]}", i==rankingRank,
							scale
						)
						receiver.drawScoreFont(
							engine, playerID, 15, topY+i, rankingTime[difficulty][i].toTimeStr,
							i==rankingRank, scale
						)
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
			val baseX:Int = receiver.fieldX(engine, playerID)+4
			val baseY:Int = receiver.fieldY(engine, playerID)+52
			engine.nowPieceObject?.let {cPiece ->
				if(pCoordList.size>0) for(loc in pCoordList) {
					val cx = baseX+16*loc[0]
					val cy = baseY+16*loc[1]
					receiver.drawPiece(cx, cy, cPiece, 1f, 0f)
				}
			}
			receiver.drawScoreFont(engine, playerID, 0, 4, "Score", EventReceiver.COLOR.BLUE)
			receiver.drawScoreNum(engine, playerID, 5, 4, "+$lastscore")
			val scget = scDisp<engine.statistics.score
			receiver.drawScoreNum(engine, playerID, 0, 5, "$scDisp", scget, 2f)

			val rix:Int = receiver.scoreX(engine, playerID)
			val riy:Int = receiver.scoreY(engine, playerID)+13*16
			GameTextUtilities.drawDirectTextAlign(
				receiver, engine, playerID, rix, riy,
				GameTextUtilities.ALIGN_TOP_LEFT,
				String.format("%.2f", multiplier)+"X",
				if(engine.stat===GameEngine.Status.MOVE&&engine.statc[0]>engine.speed.lockDelay*3) EventReceiver.COLOR.RED else if(mScale>1) EventReceiver.COLOR.ORANGE else EventReceiver.COLOR.WHITE,
				mScale
			)
			receiver.drawScoreFont(engine, playerID, 0, 6, "LINE", EventReceiver.COLOR.BLUE)
			receiver.drawScoreFont(engine, playerID, 0, 7, "${engine.statistics.lines}")
			receiver.drawScoreFont(engine, playerID, 0, 9, "TIME", EventReceiver.COLOR.BLUE)
			receiver.drawScoreFont(engine, playerID, 0, 10, engine.statistics.time.toTimeStr)
			receiver.drawScoreFont(engine, playerID, 0, 12, "MULTIPLIER", EventReceiver.COLOR.GREEN)
			receiver.drawScoreFont(engine, playerID, 0, 17, "SPEED", EventReceiver.COLOR.RED)
			if(engine.playerProp.isLoggedIn||engine.playerName.isNotEmpty()) {
				receiver.drawScoreFont(engine, playerID, 8, 17, "PLAYER", EventReceiver.COLOR.BLUE)
				receiver.drawScoreFont(
					engine, playerID, 8, 18,
					if(owner.replayMode) engine.playerName else engine.playerProp.nameDisplay,
					EventReceiver.COLOR.WHITE, 2f
				)
			}
			receiver.drawSpeedMeter(
				engine, playerID, 0, 18,
				when {
					engine.statistics.totalPieceLocked<PIECES_MAX[difficulty] -> minOf(
						1.0,
						engine.statistics.totalPieceLocked/PIECES_MAX[difficulty]
							.toDouble()
					).toFloat()
					engine.statistics.time/12%2==0 -> 1f
					else -> 0f
				},
				2f
			)
			if(engine.statistics.totalPieceLocked>=PIECES_MAX[difficulty]) {
				stext.drawScoreText(
					receiver, engine, playerID, 0, 20, 4, 2, "MAXIMUM VELOCITY",
					if(engine.statistics.time/3%3==0) EventReceiver.COLOR.ORANGE else EventReceiver.COLOR.YELLOW, 1.25f
				)
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
     * Called after every frame
     */
	override fun onLast(engine:GameEngine, playerID:Int) {
		super.onLast(engine, playerID)
		mScale = maxOf(1f, mScale*0.98f)

		// Meter
		engine.meterValue = (multiplier/20.0*receiver.getMeterMax(engine)).toInt()
		engine.meterColor = GameEngine.METER_COLOR_GREEN
		if(multiplier<15) engine.meterColor = GameEngine.METER_COLOR_YELLOW
		if(multiplier<10) engine.meterColor = GameEngine.METER_COLOR_ORANGE
		if(multiplier<5) engine.meterColor = GameEngine.METER_COLOR_RED
		if(engine.stat===GameEngine.Status.SETTING||engine.stat===GameEngine.Status.RESULT&&!owner.replayMode||engine.stat===GameEngine.Status.CUSTOM) {
			// Show rank
			if(engine.ctrl.isPush(Controller.BUTTON_F)&&engine.playerProp.isLoggedIn&&engine.stat!==GameEngine.Status.CUSTOM) {
				showPlayerStats = !showPlayerStats
				engine.playSE("change")
			}
		}
	}
	/*
     * Calculate score
     */
	override fun calcScore(engine:GameEngine, playerID:Int, lines:Int):Int {
		// Line clear bonus
		val pts = calcScoreBase(engine, lines)
		var get = 0
		val spd = maxOf(0, engine.lockDelay-engine.lockDelayNow)+if(engine.manualLock) 1 else 0
		// Combo
		val cmb = if(engine.combo>=1&&lines>=1) engine.combo-1 else 0

		if(engine.twist) {
			// T-Spin 0 lines
			if(engine.twistez&&lines>0) multiplier += (if(engine.b2b) MULTIPLIER_SINGLE*MULTIPLIER_MINI_SPIN*MULTIPLIER_B2B else MULTIPLIER_SINGLE*MULTIPLIER_MINI_SPIN) else if(lines==1) multiplier += if(engine.twistmini) {
				if(engine.b2b) MULTIPLIER_SINGLE*MULTIPLIER_MINI_SPIN*MULTIPLIER_B2B else MULTIPLIER_SINGLE*MULTIPLIER_MINI_SPIN
			} else
				if(engine.b2b) MULTIPLIER_SINGLE*MULTIPLIER_SPIN*MULTIPLIER_B2B else MULTIPLIER_SINGLE*MULTIPLIER_SPIN
			else if(lines==2) multiplier += (if(engine.twistmini&&engine.useAllSpinBonus) {
				if(engine.b2b) MULTIPLIER_DOUBLE*MULTIPLIER_MINI_SPIN*MULTIPLIER_B2B else MULTIPLIER_DOUBLE*MULTIPLIER_MINI_SPIN
			} else if(engine.b2b) MULTIPLIER_DOUBLE*MULTIPLIER_SPIN*MULTIPLIER_B2B else MULTIPLIER_DOUBLE*MULTIPLIER_SPIN)
			else if(lines>=3) multiplier += (if(engine.b2b) MULTIPLIER_TRIPLE*MULTIPLIER_SPIN*MULTIPLIER_B2B else MULTIPLIER_TRIPLE*MULTIPLIER_SPIN)
			if(lines>0) mScale += 1f/3f*lines
		} else {
			multiplier += when {
				lines==1 -> MULTIPLIER_SINGLE
				lines==2 -> MULTIPLIER_DOUBLE
				lines==3 -> MULTIPLIER_TRIPLE
				lines>=4 -> (if(engine.b2b) MULTIPLIER_TETRIS*MULTIPLIER_B2B else MULTIPLIER_TETRIS)
				else -> 0.0
			}
			if(lines>0) mScale += 0.25f*lines
		}

		// Combo
		if(engine.combo>=1&&lines>=1) {
			multiplier += engine.combo*MULTIPLIER_COMBO
			mScale += engine.combo*0.1f
		}

		// All clear
		if(lines>=1&&engine.field.isEmpty) {
			multiplier += 2.0
			mScale += 0.5f
		}
		multiplier = minOf(MULTIPLIER_MAXIMUM, multiplier)

		// Add to score
		if(pts+cmb+spd>0) {
			get = calcScoreCombo(pts, cmb, engine.statistics.level, spd)
			get = (get*multiplier).toInt()
			if(pts>0) lastscore = get

			if(lines>=1) engine.statistics.scoreLine += get
			else engine.statistics.scoreBonus += get
			scDisp += (spd*multiplier).toInt()
		}
		// BGM fade-out effects and BGM changes
		val pieces = minOf(
			PIECES_MAX[difficulty]+PIECES_MAX[difficulty]/20, engine.statistics.totalPieceLocked
		)
		val lastLevel:Int = engine.statistics.level

//		if ((pieces - (PIECES_MAX[difficulty] / 20)) % (PIECES_MAX[difficulty] / 5) >= ((PIECES_MAX[difficulty] / 5) - 10) && engine.statistics.totalPieceLocked - (PIECES_MAX[difficulty] / 20) <= PIECES_MAX[difficulty]) {
//			owner.bgmStatus.fadesw = true;
//		} else if ((0 == pieces % (PIECES_MAX[difficulty] / 5)) && engine.statistics.totalPieceLocked - (PIECES_MAX[difficulty] / 20) <= PIECES_MAX[difficulty] && (pieces - (PIECES_MAX[difficulty] / 20)) > 0) {
//			bgmlv++;
//			owner.bgmStatus.bgm = bgmlv;
//			owner.bgmStatus.fadesw = false;
//		}

		// Level up
		engine.statistics.level = minOf(19, pieces/(PIECES_MAX[difficulty]/20))
		val levelDec = pieces.toDouble()/(PIECES_MAX[difficulty].toDouble()/20.0)
		if(levelDec-levelDec.toInt()>=0.8&&pieces<PIECES_MAX[difficulty]) {
			if(engine.statistics.level==3||engine.statistics.level==7||engine.statistics.level==11||engine.statistics.level==15||engine.statistics.level==19) owner.bgmStatus.fadesw =
				true
		}
		if(engine.statistics.level>lastLevel) {
			owner.backgroundStatus.fadesw = true
			owner.backgroundStatus.fadecount = 0
			owner.backgroundStatus.fadebg = engine.statistics.level
			if(engine.statistics.level==4||engine.statistics.level==8||engine.statistics.level==12||engine.statistics.level==16) {
				bgmlv++
				owner.bgmStatus.bgm = BGMStatus.BGM.GrandT(bgmlv)
				owner.bgmStatus.fadesw = false
			}
			engine.playSE("levelup")
		}
		if(engine.statistics.totalPieceLocked==PIECES_MAX[difficulty]) {
			engine.playSE("hurryup")
			bgmlv++
			owner.bgmStatus.bgm = BGMStatus.BGM.GrandT(bgmlv)
			owner.bgmStatus.fadesw = false
		}
		setSpeed(engine)
		return get
	}
	/*
     * Soft drop
     */
	override fun afterSoftDropFall(engine:GameEngine, playerID:Int, fall:Int) {
		engine.statistics.scoreSD += (fall*multiplier).toInt()
	}
	/*
     * Hard drop
     */
	override fun afterHardDropFall(engine:GameEngine, playerID:Int, fall:Int) {
		engine.statistics.scoreHD += (fall*2*multiplier).toInt()
		val baseX:Int = 16*engine.nowPieceX+4+receiver.fieldX(engine, playerID)
		val baseY:Int = 16*engine.nowPieceY+52+receiver.fieldY(engine, playerID)
		engine.nowPieceObject?.let {cPiece ->
			for(i in 1..fall) {
				pCoordList.add(intArrayOf(engine.nowPieceX, engine.nowPieceY-i))
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
     * Render results screen
     */
	override fun renderResult(engine:GameEngine, playerID:Int) {
		drawResultStats(
			engine, playerID, receiver, 0, EventReceiver.COLOR.BLUE,
			Statistic.SCORE, Statistic.LINES, Statistic.TIME, Statistic.SPL, Statistic.LPM
		)
		drawResultRank(engine, playerID, receiver, 10, EventReceiver.COLOR.BLUE, rankingRank)
		drawResultNetRank(engine, playerID, receiver, 12, EventReceiver.COLOR.BLUE, netRankingRank[0])
		drawResultNetRankDaily(engine, playerID, receiver, 14, EventReceiver.COLOR.BLUE, netRankingRank[1])
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

		// NET: Save name
		if(netPlayerName!=null&&netPlayerName!!.isNotEmpty()) {
			prop.setProperty("$playerID.net.netPlayerName", netPlayerName)
		}

		// Update rankings
		if(!owner.replayMode&&!big&&engine.ai==null) {
			if(updateRanking(
					engine.statistics.score, engine.statistics.lines, engine.statistics.time, difficulty,
					engine.playerProp.isLoggedIn
				)
			) return true

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
	private fun updateRanking(sc:Int, li:Int, time:Int, type:Int, isLoggedIn:Boolean):Boolean {
		rankingRank = checkRanking(sc, li, time, type)
		if(rankingRank!=-1) {
			// Shift down ranking entries
			for(i in RANKING_MAX-1 downTo rankingRank+1) {
				rankingScore[type][i] = rankingScore[type][i-1]
				rankingLines[type][i] = rankingLines[type][i-1]
				rankingTime[type][i] = rankingTime[type][i-1]
			}

			// Add new data
			rankingScore[type][rankingRank] = sc
			rankingLines[type][rankingRank] = li
			rankingTime[type][rankingRank] = time
		}
		if(isLoggedIn) {
			rankingRankPlayer = checkRankingPlayer(sc, li, time, type)
			if(rankingRankPlayer!=-1) {
				// Shift down ranking entries
				for(i in RANKING_MAX-1 downTo rankingRankPlayer+1) {
					rankingScorePlayer[type][i] = rankingScorePlayer[type][i-1]
					rankingLinesPlayer[type][i] = rankingLinesPlayer[type][i-1]
					rankingTimePlayer[type][i] = rankingTimePlayer[type][i-1]
				}

				// Add new data
				rankingScorePlayer[type][rankingRankPlayer] = sc
				rankingLinesPlayer[type][rankingRankPlayer] = li
				rankingTimePlayer[type][rankingRankPlayer] = time
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
	private fun checkRanking(sc:Int, li:Int, time:Int, type:Int):Int {
		for(i in 0 until RANKING_MAX) if(sc>rankingScore[type][i]) return i
		else if(sc==rankingScore[type][i]&&li>rankingLines[type][i]) return i
		else if(sc==rankingScore[type][i]&&li==rankingLines[type][i]&&time<rankingTime[type][i]) return i
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
		for(i in 0 until RANKING_MAX) if(sc>rankingScorePlayer[type][i]) return i
		else if(sc==rankingScorePlayer[type][i]&&li>rankingLinesPlayer[type][i]) return i
		else if(sc==rankingScorePlayer[type][i]&&li==rankingLinesPlayer[type][i]&&time<rankingTimePlayer[type][i]) return i
		return -1
	}

	companion object {
		/**
		 * Gravity denominator for G calculation
		 */
		private const val GRAVITY_DENOMINATOR = 256
		/**
		 * Starting delays
		 */
		private const val START_ARE = 25
		private const val START_LINE_ARE = 25
		private const val START_LINE_DELAY = 40
		private const val START_DAS = 14
		private const val START_LOCK_DELAY = 30
		private const val START_GRAVITY = 4
		/**
		 * Delays after reaching max piece
		 */
		private val END_ARE = intArrayOf(8, 6, 4)
		private val END_LINE_ARE = intArrayOf(5, 4, 3)
		private val END_LINE_DELAY = intArrayOf(9, 6, 3)
		private val END_DAS = intArrayOf(6, 6, 4)
		private val END_LOCK_DELAY = intArrayOf(17, 15, 8)
		/**
		 * Score multipliers
		 */
		private const val MULTIPLIER_MINIMUM = 1.0
		private const val MULTIPLIER_SINGLE = 0.05
		private const val MULTIPLIER_DOUBLE = 0.125
		private const val MULTIPLIER_TRIPLE = 0.25
		private const val MULTIPLIER_TETRIS = 0.5
		private const val MULTIPLIER_B2B = 1.5
		private const val MULTIPLIER_COMBO = 0.025
		private const val MULTIPLIER_SPIN = 5.0
		private const val MULTIPLIER_MINI_SPIN = 3.0
		private const val MULTIPLIER_MAXIMUM = 20.0
		/**
		 * Pieces until max
		 * Use interpolation to get the delays and gravity.
		 *
		 *
		 * -- Planned --
		 * GRAVITY, ARE, LINE ARE: Linear
		 * DAS, LOCK DELAY, LINE DELAY: Ease-in-ease-out
		 */
		private val PIECES_MAX = intArrayOf(1000, 800, 600)
		private val GRAVITY_MULTIPLIERS = doubleArrayOf(1.014412098, 1.018047461, 1.024135373)
		/**
		 * Difficulties
		 */
		private val difficultyName = arrayOf("EASY", "NORMAL", "HARD")
		private const val DIFFICULTIES = 3
		private const val DIFFICULTY_EASY = 0
		private const val DIFFICULTY_NORMAL = 1
		private const val DIFFICULTY_HARD = 2
		private val headerColor:EventReceiver.COLOR = EventReceiver.COLOR.RED
	}
}
