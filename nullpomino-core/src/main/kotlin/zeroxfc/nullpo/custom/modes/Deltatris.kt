/*
 * Copyright (c) 2021-2023,
 * This library class was created by 0xFC963F18DC21 / Shots243
 * It is part of an extension library for the game NullpoMino (copyright 2021-2023)
 *
 * Kotlin converted and modified by Venom=Nhelv
 *
 * Herewith shall the term "Library Creator" be given to 0xFC963F18DC21.
 * Herewith shall the term "Game Creator" be given to the original creator of NullpoMino, NullNoname.
 *
 * THIS LIBRARY AND MODE PACK WAS NOT MADE IN ASSOCIATION WITH THE GAME CREATOR.
 *
 * Original Repository: https://github.com/Shots243/ModePile
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
import mu.nu.nullpo.game.event.ScoreEvent
import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.game.subsystem.mode.menu.DelegateMenuItem
import mu.nu.nullpo.game.subsystem.mode.menu.MenuList
import mu.nu.nullpo.game.subsystem.mode.menu.StringsMenuItem
import mu.nu.nullpo.gui.slick.img.ext.RendererExtension
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

	override val itemMode = StringsMenuItem("difficulty", "Difficulty", COLOR.RED, 1, difficultyName)
	private var difficulty:Int by DelegateMenuItem(itemMode)
	private var multiplier = 1f
	private var grav = 0.0
	private var mScale = 1f
	private var scoreBbefore = 0
	// Generic
	private val rankingScore = List(RANKING_TYPE) {MutableList(RANKING_MAX) {0L}}
	private val rankingTime = List(RANKING_TYPE) {MutableList(RANKING_MAX) {-1}}
	private val rankingLines = List(RANKING_TYPE) {MutableList(RANKING_MAX) {0}}
	// PROFILE
	private var rankingRankPlayer = 0
	private val rankingScorePlayer = List(RANKING_TYPE) {MutableList(RANKING_MAX) {0L}}
	private val rankingTimePlayer = List(RANKING_TYPE) {MutableList(RANKING_MAX) {0}}
	private val rankingLinesPlayer = List(RANKING_TYPE) {MutableList(RANKING_MAX) {0}}
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
	// help me I forgot how to make modes
	// Initialization
	override val menu:MenuList = MenuList("deltatris", itemMode, itemBig)
	override val rankMap
		get() = rankMapOf(rankingScore.mapIndexed {a, x -> "$a.score" to x}+
			rankingLines.mapIndexed {a, x -> "$a.lines" to x}+
			rankingTime.mapIndexed {a, x -> "$a.time" to x})

	override val rankPersMap
		get() = rankMapOf(rankingScorePlayer.mapIndexed {a, x -> "$a.score" to x}+
			rankingLinesPlayer.mapIndexed {a, x -> "$a.lines" to x}+
			rankingTimePlayer.mapIndexed {a, x -> "$a.time" to x})

	override fun playerInit(engine:GameEngine) {
		super.playerInit(engine)
		lastScore = 0
		bgmLv = 0
		multiplier = 1f
		mScale = 1f
		grav = START_GRAVITY.toDouble()
		scoreBbefore = 0
		difficulty = 1
		rankingRank = -1
		rankingScore
		rankingLines
		rankingTime
		engine.playerProp.reset()
		showPlayerStats = false

		rankingRankPlayer = -1
		rankingScorePlayer.forEach {it.fill(0)}
		rankingLinesPlayer.forEach {it.fill(0)}
		rankingTimePlayer.forEach {it.fill(0)}
		pCoordList = ArrayList()
		netPlayerInit(engine)
		if(!owner.replayMode) version = CURRENT_VERSION else {
			if(version==0&&owner.replayProp.getProperty("deltatris.endless", false)) goalType = 2
			// NET: Load name
			netPlayerName = engine.owner.replayProp.getProperty("${engine.playerID}.net.netPlayerName", "")
		}
		engine.owner.bgMan.bg = 0
		engine.frameColor = GameEngine.FRAME_COLOR_GRAY
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
	override fun onSetting(engine:GameEngine):Boolean {
		// NET: Net Ranking
		if(netIsNetRankingDisplayMode) {
			netOnUpdateNetPlayRanking(engine, goalType)
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

	override fun onFirst(engine:GameEngine) {
		pCoordList.clear()
	}
	/*
     * Called for initialization during "Ready" screen
     */
	override fun startGame(engine:GameEngine) {
		engine.statistics.level = 0
		engine.statistics.levelDispAdd = 1
		engine.b2bEnable = true
		scoreBbefore = 0
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
			owner.musMan.bgm = BGMStatus.BGM.Silent
		}
	}

	override fun onMove(engine:GameEngine):Boolean {
		if(engine.statc[0]>engine.speed.lockDelay*3) {
			multiplier = maxOf(1f, multiplier*.99225f)
		}
		return false
	}
	/*
     * Render score
     */
	override fun renderLast(engine:GameEngine) {
		if(owner.menuOnly) return
		receiver.drawScoreFont(engine, 0, 0, name, COLOR.RED)
		receiver.drawScoreFont(
			engine, 0, 1, "(${difficultyName[difficulty]} DIFFICULTY)", COLOR.RED
		)
		val pid = engine.playerID
		if(engine.stat===GameEngine.Status.SETTING||engine.stat===GameEngine.Status.RESULT&&!owner.replayMode) {
			if(!owner.replayMode&&!big&&engine.ai==null) {
				val topY = if(receiver.nextDisplayType==2) 6 else 4
				receiver.drawScoreFont(engine, 3, topY-1, "SCORE  LINE TIME", COLOR.BLUE)
				if(showPlayerStats) {
					for(i in 0..<RANKING_MAX) {
						receiver.drawScoreFont(engine, 0, topY+i, "%2d".format(i+1), COLOR.YELLOW)
						receiver.drawScoreFont(engine, 3, topY+i, "${rankingScorePlayer[difficulty][i]}", i==rankingRankPlayer)
						receiver.drawScoreFont(engine, 10, topY+i, "${rankingLinesPlayer[difficulty][i]}", i==rankingRankPlayer)
						receiver.drawScoreFont(engine, 15, topY+i, rankingTimePlayer[difficulty][i].toTimeStr, i==rankingRankPlayer)
					}
					receiver.drawScoreFont(engine, 0, topY+RANKING_MAX+1, "PLAYER SCORES", COLOR.BLUE)
					receiver.drawScoreFont(engine, 0, topY+RANKING_MAX+2, engine.playerProp.nameDisplay, COLOR.WHITE, 2f)
					receiver.drawScoreFont(engine, 0, topY+RANKING_MAX+5, "F:SWITCH RANK SCREEN", COLOR.GREEN)
				} else {
					for(i in 0..<RANKING_MAX) {
						receiver.drawScoreFont(engine, 0, topY+i, "%2d".format(i+1), COLOR.YELLOW)
						receiver.drawScoreFont(engine, 3, topY+i, "${rankingScore[difficulty][i]}", i==rankingRank)
						receiver.drawScoreFont(engine, 10, topY+i, "${rankingLines[difficulty][i]}", i==rankingRank)
						receiver.drawScoreFont(engine, 15, topY+i, rankingTime[difficulty][i].toTimeStr, i==rankingRank)
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
			val baseX = receiver.fieldX(engine)+4
			val baseY = receiver.fieldY(engine)+52
			engine.nowPieceObject?.let {cPiece ->
				if(pCoordList.size>0) for(loc in pCoordList) {
					val cx = baseX+16*loc[0]
					val cy = baseY+16*loc[1]
					receiver.drawPiece(cx, cy, cPiece, 1f, 0f)
				}
			}
			receiver.drawScoreFont(engine, 0, 4, "Score", COLOR.BLUE)
			receiver.drawScoreNum(engine, 5, 4, "+$lastScore")
			val scget = scDisp<engine.statistics.score
			receiver.drawScoreNum(engine, 0, 5, "$scDisp", scget, 2f)

			val rix = receiver.scoreX(engine)
			val riy = receiver.scoreY(engine)+13*16
			GameTextUtilities.drawDirectTextAlign(
				receiver, rix, riy, GameTextUtilities.ALIGN_TOP_LEFT, "%.2f".format(multiplier)+"X",
				if(engine.stat===GameEngine.Status.MOVE&&engine.statc[0]>engine.speed.lockDelay*3) COLOR.RED else if(mScale>1) COLOR.ORANGE else COLOR.WHITE,
				mScale
			)
			receiver.drawScoreFont(engine, 0, 6, "LINE", COLOR.BLUE)
			receiver.drawScoreFont(engine, 0, 7, "${engine.statistics.lines}")
			receiver.drawScoreFont(engine, 0, 9, "TIME", COLOR.BLUE)
			receiver.drawScoreFont(engine, 0, 10, engine.statistics.time.toTimeStr)
			receiver.drawScoreFont(engine, 0, 12, "MULTIPLIER", COLOR.GREEN)
			receiver.drawScoreFont(engine, 0, 17, "SPEED", COLOR.RED)
			if(engine.playerProp.isLoggedIn||engine.playerName.isNotEmpty()) {
				receiver.drawScoreFont(engine, 8, 17, "PLAYER", COLOR.BLUE)
				receiver.drawScoreFont(
					engine, 8, 18, if(owner.replayMode) engine.playerName else engine.playerProp.nameDisplay,
					COLOR.WHITE,
					2f
				)
			}
			receiver.drawScoreSpeed(
				engine, 0, 18, when {
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
					receiver,
					engine,
					0,
					20,
					4,
					2,
					"MAXIMUM VELOCITY",
					if(engine.statistics.time/3%3==0) COLOR.ORANGE else COLOR.YELLOW,
					1.25f
				)
			}
		}

		// NET: Number of spectators
		netDrawSpectatorsCount(engine, 0, 18)
		// NET: All number of players
		if(pid==players-1) {
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
		mScale = maxOf(1f, mScale*0.98f)

		// Meter
		engine.meterValue = (multiplier/20f)
		engine.meterColor = GameEngine.METER_COLOR_LIMIT
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
	override fun calcScore(engine:GameEngine, ev:ScoreEvent):Int {
		// Line clear bonus
		val li = ev.lines
		val pts = calcScoreBase(engine, ev)
		var get = 0
		val spd = maxOf(0, engine.lockDelay-engine.lockDelayNow)+if(engine.manualLock) 1 else 0
		// Combo
		val cmb = if(ev.combo>0&&li>=1) ev.combo else 0

		if(ev.twist) {
			// T-Spin 0 lines
			if(ev.twistEZ&&li>0) multiplier += (if(ev.b2b>0) MULTIPLIER_SINGLE*MULTIPLIER_MINI_SPIN*MULTIPLIER_B2B else MULTIPLIER_SINGLE*MULTIPLIER_MINI_SPIN)
			else if(li==1) multiplier += if(engine.twistMini) {
				if(ev.b2b>0) MULTIPLIER_SINGLE*MULTIPLIER_MINI_SPIN*MULTIPLIER_B2B else MULTIPLIER_SINGLE*MULTIPLIER_MINI_SPIN
			} else
				if(ev.b2b>0) MULTIPLIER_SINGLE*MULTIPLIER_SPIN*MULTIPLIER_B2B else MULTIPLIER_SINGLE*MULTIPLIER_SPIN
			else if(li==2) multiplier += (if(engine.twistMini&&engine.useAllSpinBonus) {
				if(ev.b2b>0) MULTIPLIER_DOUBLE*MULTIPLIER_MINI_SPIN*MULTIPLIER_B2B else MULTIPLIER_DOUBLE*MULTIPLIER_MINI_SPIN
			} else if(ev.b2b>0) MULTIPLIER_DOUBLE*MULTIPLIER_SPIN*MULTIPLIER_B2B else MULTIPLIER_DOUBLE*MULTIPLIER_SPIN)
			else if(li>=3) multiplier += (if(ev.b2b>0) MULTIPLIER_TRIPLE*MULTIPLIER_SPIN*MULTIPLIER_B2B else MULTIPLIER_TRIPLE*MULTIPLIER_SPIN)
			if(li>0) mScale += 1f/3f*li
		} else {
			multiplier += when {
				li==1 -> MULTIPLIER_SINGLE
				li==2 -> MULTIPLIER_DOUBLE
				li==3 -> MULTIPLIER_TRIPLE
				li>=4 -> (if(ev.b2b>0) MULTIPLIER_TETRIS*MULTIPLIER_B2B else MULTIPLIER_TETRIS)
				else -> .0f
			}
			if(li>0) mScale += .25f*li
		}

		// Combo
		if(engine.combo>0&&li>=1) {
			multiplier += engine.combo*MULTIPLIER_COMBO
			mScale += engine.combo*0.1f
		}

		// All clear
		if(li>=1&&engine.field.isEmpty) {
			multiplier += 2f
			mScale += 0.5f
		}
		multiplier = minOf(MULTIPLIER_MAXIMUM, multiplier)

		// Add to score
		if(pts+cmb+spd>0) {
			get = calcScoreCombo(pts, cmb, engine.statistics.level, spd)
			get = (get*multiplier).toInt()
			if(pts>0) lastScore = get

			if(li>=1) engine.statistics.scoreLine += get
			else engine.statistics.scoreBonus += get
			scDisp += (spd*multiplier).toInt()
		}
		// BGM fade-out effects and BGM changes
		val pieces = minOf(
			PIECES_MAX[difficulty]+PIECES_MAX[difficulty]/20, engine.statistics.totalPieceLocked
		)
		val lastLevel:Int = engine.statistics.level

		/*		if ((pieces - (PIECES_MAX[difficulty] / 20)) % (PIECES_MAX[difficulty] / 5) >= ((PIECES_MAX[difficulty] / 5) - 10) && engine.statistics.totalPieceLocked - (PIECES_MAX[difficulty] / 20) <= PIECES_MAX[difficulty]) {
		//			owner.bgmStatus.fadeSW = true;
				} else if ((0 == pieces % (PIECES_MAX[difficulty] / 5)) && engine.statistics.totalPieceLocked - (PIECES_MAX[difficulty] / 20) <= PIECES_MAX[difficulty] && (pieces - (PIECES_MAX[difficulty] / 20)) > 0) {
		//			bgmLv++;
		//			owner.bgmStatus.bgm = bgmLv;
		//			owner.bgmStatus.fadeSW = false;
				}*/

		// Level up
		engine.statistics.level = minOf(19, pieces/(PIECES_MAX[difficulty]/20))
		val levelDec = pieces.toDouble()/(PIECES_MAX[difficulty].toDouble()/20.0)
		if(levelDec-levelDec.toInt()>=0.8&&pieces<PIECES_MAX[difficulty]) {
			if(engine.statistics.level==3||engine.statistics.level==7||engine.statistics.level==11||engine.statistics.level==15||engine.statistics.level==19) owner.musMan.fadeSW =
				true
		}
		if(engine.statistics.level>lastLevel) {
			owner.bgMan.nextBg = engine.statistics.level
			if(engine.statistics.level==4||engine.statistics.level==8||engine.statistics.level==12||engine.statistics.level==16) {
				bgmLv++
				owner.musMan.bgm = BGMStatus.BGM.GrandT(bgmLv)
				owner.musMan.fadeSW = false
			}
			engine.playSE("levelup")
		}
		if(engine.statistics.totalPieceLocked==PIECES_MAX[difficulty]) {
			engine.playSE("hurryup")
			bgmLv++
			owner.musMan.bgm = BGMStatus.BGM.GrandT(bgmLv)
			owner.musMan.fadeSW = false
		}
		setSpeed(engine)
		return get
	}
	/*
     * Soft drop
     */
	override fun afterSoftDropFall(engine:GameEngine, fall:Int) {
		engine.statistics.scoreSD += (fall*multiplier).toInt()
	}
	/*
     * Hard drop
     */
	override fun afterHardDropFall(engine:GameEngine, fall:Int) {
		engine.statistics.scoreHD += (fall*2*multiplier).toInt()
		val baseX = 16*engine.nowPieceX+4+receiver.fieldX(engine)
		val baseY = 16*engine.nowPieceY+52+receiver.fieldY(engine)
		engine.nowPieceObject?.let {cPiece ->
			for(i in 1..fall) {
				pCoordList.add(intArrayOf(engine.nowPieceX, engine.nowPieceY-i))
			}
			for(i in 0..<cPiece.maxBlock) {
				if(!cPiece.big) {
					val x2 = baseX+cPiece.dataX[cPiece.direction][i]*16
					val y2 = baseY+cPiece.dataY[cPiece.direction][i]*16
					RendererExtension.addBlockBreakEffect(receiver, x2, y2, cPiece.block[i])
				} else {
					val x2 = baseX+cPiece.dataX[cPiece.direction][i]*32
					val y2 = baseY+cPiece.dataY[cPiece.direction][i]*32
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
	override fun renderResult(engine:GameEngine) {
		drawResultStats(
			engine, receiver, 0, COLOR.BLUE, Statistic.SCORE,
			Statistic.LINES, Statistic.TIME, Statistic.SPL, Statistic.LPM
		)
		drawResultRank(engine, receiver, 10, COLOR.BLUE, rankingRank)
		drawResultNetRank(engine, receiver, 12, COLOR.BLUE, netRankingRank[0])
		drawResultNetRankDaily(engine, receiver, 14, COLOR.BLUE, netRankingRank[1])
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
		// NET: Save name
		if(netPlayerName!=null&&netPlayerName!!.isNotEmpty()) {
			prop.setProperty("${engine.playerID}.net.netPlayerName", netPlayerName)
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
	private fun updateRanking(sc:Long, li:Int, time:Int, type:Int, isLoggedIn:Boolean):Boolean {
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
	private fun checkRanking(sc:Long, li:Int, time:Int, type:Int):Int {
		for(i in 0..<RANKING_MAX) if(sc>rankingScore[type][i]) return i
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
	private fun checkRankingPlayer(sc:Long, li:Int, time:Int, type:Int):Int {
		for(i in 0..<RANKING_MAX) if(sc>rankingScorePlayer[type][i]) return i
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
		private const val MULTIPLIER_MINIMUM = 1f
		private const val MULTIPLIER_SINGLE = .05f
		private const val MULTIPLIER_DOUBLE = .125f
		private const val MULTIPLIER_TRIPLE = .25f
		private const val MULTIPLIER_TETRIS = .5f
		private const val MULTIPLIER_B2B = 1.5f
		private const val MULTIPLIER_COMBO = .025f
		private const val MULTIPLIER_SPIN = 5f
		private const val MULTIPLIER_MINI_SPIN = 3f
		private const val MULTIPLIER_MAXIMUM = 20f
		/**
		 * Pieces until max
		 * Use interpolation to get the delays and gravity.
		 *
		 *
		 * -- Planned --
		 * GRAVITY, ARE, LINE ARE: Linear
		 * DAS, LOCK DELAY, LINE DELAY: Ease-in-ease-out
		 */
		private val PIECES_MAX = listOf(1000, 800, 600)
		private val GRAVITY_MULTIPLIERS = listOf(1.014412098, 1.018047461, 1.024135373)
		/**
		 * Difficulties
		 */
		private val difficultyName = listOf("EASY", "NORMAL", "HARD")
	}
}
