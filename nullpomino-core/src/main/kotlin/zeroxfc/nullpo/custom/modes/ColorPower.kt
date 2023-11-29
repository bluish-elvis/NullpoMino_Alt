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

/*
 * COLOR POWER mode
 * - Marathon, but with powers given by breaking enough blocks of a certain color.
 * - Enjoy rainbows!
 */
package zeroxfc.nullpo.custom.modes

import mu.nu.nullpo.game.component.BGMStatus
import mu.nu.nullpo.game.component.Block
import mu.nu.nullpo.game.component.Controller
import mu.nu.nullpo.game.component.Piece
import mu.nu.nullpo.game.event.EventReceiver.COLOR
import mu.nu.nullpo.game.event.ScoreEvent
import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.game.subsystem.mode.menu.BooleanMenuItem
import mu.nu.nullpo.game.subsystem.mode.menu.DelegateMenuItem
import mu.nu.nullpo.game.subsystem.mode.menu.MenuList
import mu.nu.nullpo.gui.slick.img.ext.RendererExtension
import mu.nu.nullpo.util.CustomProperties
import mu.nu.nullpo.util.GeneralUtil.toTimeStr
import kotlin.random.Random

class ColorPower:MarathonModeBase() {
	var l = 0
	// Power meter values
	private var meterValues = intArrayOf()
	// Custom status timer
	private var customTimer = 0
	// Current active power
	private var currentActivePower = 0
	// Current score multiplier
	private var scoreMultiplier = 0
	// Current thing being modified
	private var currentModified = 0
	// HAS SET
	private var hasSet = false

	private val itemRule = BooleanMenuItem("rulebound", "RULE BOUND", COLOR.BLUE, false)
	// Rulebound mode: default = false.
	private var ruleBoundMode:Boolean by DelegateMenuItem(itemRule)
	// Randomizer for non-rulebound mode
	private var nonRuleBoundRandomizer:Random = Random.Default
	// Color history
	private val colorHistory = MutableList(4) {-1}
	// engine dif
	private val defaultColors = MutableList(Piece.PIECE_COUNT) {0}
	// Hm
	private var preset = false
	/** Rankings' scores */
	private val rankingScore = List(2) {List(RANKING_TYPE) {MutableList(RANKING_MAX) {0L}}}
	/** Rankings' line counts */
	private val rankingLines = List(2) {List(RANKING_TYPE) {MutableList(RANKING_MAX) {0}}}
	/** Rankings' times */
	private val rankingTime = List(2) {List(RANKING_TYPE) {MutableList(RANKING_MAX) {-1}}}
	/** Player rank */
	private var rankingRankPlayer = 0
	/** Rankings' scores */
	private val rankingScorePlayer = List(2) {List(RANKING_TYPE) {MutableList(RANKING_MAX) {0L}}}
	/** Rankings' line counts */
	private val rankingLinesPlayer = List(2) {List(RANKING_TYPE) {MutableList(RANKING_MAX) {0}}}
	/** Rankings' times */
	private val rankingTimePlayer = List(2) {List(RANKING_TYPE) {MutableList(RANKING_MAX) {-1}}}

	override val rankMap
		get() = rankMapOf(rankingScore.flatMapIndexed {a, x -> x.mapIndexed {b, y -> "$a.$b.score" to y}}+
			rankingLines.flatMapIndexed {a, x -> x.mapIndexed {b, y -> "$a.$b.lines" to y}}+
			rankingTime.flatMapIndexed {a, x -> x.mapIndexed {b, y -> "$a.$b.time" to y}})

	override val rankPersMap
		get() = rankMapOf(rankingScorePlayer.flatMapIndexed {a, x -> x.mapIndexed {b, y -> "$a.$b.score" to y}}+
			rankingLinesPlayer.flatMapIndexed {a, x -> x.mapIndexed {b, y -> "$a.$b.lines" to y}}+
			rankingTimePlayer.flatMapIndexed {a, x -> x.mapIndexed {b, y -> "$a.$b.time" to y}})

	/** The good hard drop effect */
	private var pCoordList:MutableList<IntArray>? = null
	// Mode Name
	override val name:String
		get() = "COLOR POWER"
	// Initialization
	override val menu:MenuList = MenuList("colorpower", itemMode, itemLevel, itemRule, itemBig)
	override fun playerInit(engine:GameEngine) {
		super.playerInit(engine)
		l = -1
		defaultColors.fill(0)
		pCoordList = ArrayList()
		customTimer = 0
		meterValues = IntArray(POWERUP_AMOUNT)
		currentActivePower = -1
		scoreMultiplier = 1
		currentModified = -1
		hasSet = false
		ruleBoundMode = false
		preset = false
		colorHistory.fill(-1)
		engine.playerProp.reset()
		showPlayerStats = false

		rankingRankPlayer = -1
		rankingScore.forEach {it.forEach {i -> i.fill(0)}}
		rankingLines.forEach {it.forEach {i -> i.fill(0)}}
		rankingTime.forEach {it.forEach {i -> i.fill(0)}}
		rankingScorePlayer.forEach {it.forEach {i -> i.fill(0)}}
		rankingLinesPlayer.forEach {it.forEach {i -> i.fill(0)}}
		rankingTimePlayer.forEach {it.forEach {i -> i.fill(0)}}
		rankingRank = -1
//		rankingScore = Array(2) {Array(RANKING_TYPE) {IntArray(RANKING_MAX)}}
//		rankingLines = Array(2) {Array(RANKING_TYPE) {IntArray(RANKING_MAX)}}
//		rankingTime = Array(2) {Array(RANKING_TYPE) {IntArray(RANKING_MAX)}}
		netPlayerInit(engine)
		if(!owner.replayMode) version = CURRENT_VERSION else {
			if(version==0&&owner.replayProp.getProperty("colorpower.endless", false)) goalType = 2

			// NET: Load name
			netPlayerName = engine.owner.replayProp.getProperty("${engine.playerID}.net.netPlayerName", "")
		}
		engine.owner.bgMan.bg = startLevel
		engine.frameColor = GameEngine.FRAME_COLOR_GREEN
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
			val change = updateCursor(engine, 8)
			if(change!=0) {
				engine.playSE("change")
				when(engine.statc[2]) {
					0 -> {
						startLevel += change
						if(tableGameClearLines[goalType]>=0) {
							if(startLevel<0) startLevel = (tableGameClearLines[goalType]-1)/10
							if(startLevel>(tableGameClearLines[goalType]-1)/10) startLevel = 0
						} else {
							if(startLevel<0) startLevel = 19
							if(startLevel>19) startLevel = 0
						}
						engine.owner.bgMan.bg = startLevel
					}
					6 -> {
						goalType += change
						if(goalType<0) goalType = GAMETYPE_MAX-1
						if(goalType>GAMETYPE_MAX-1) goalType = 0
						if(startLevel>(tableGameClearLines[goalType]-1)/10&&tableGameClearLines[goalType]>=0) {
							startLevel = (tableGameClearLines[goalType]-1)/10
							engine.owner.bgMan.bg = startLevel
						}
					}
					7 -> big = !big
					8 -> ruleBoundMode = !ruleBoundMode
				}

				// NET: Signal options change
				if(netIsNetPlay&&netNumSpectators>0) {
					netSendOptions(engine)
				}
			}
			engine.owner.bgMan.bg = startLevel

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

	private fun randomizeColors(engine:GameEngine, singlePiece:Boolean) {
		if(singlePiece) {
			var v = -1
			for(j in 0..7) {
				var flag = false
				v = nonRuleBoundRandomizer.nextInt(8)+1
				for(elem in colorHistory) {
					if(elem==v) {
						flag = true
						break
					}
				}
				if(!flag) break
			}
			appendToHistory(v)
			engine.getNextObject(engine.nextPieceCount+engine.ruleOpt.nextDisplay-1)?.setColor(v)
		} else {
			for(i in 0..<engine.nextPieceArrayObject.size) {
				var v = -1
				for(j in 0..7) {
					var flag = false
					v = nonRuleBoundRandomizer.nextInt(8)+1
					for(elem in colorHistory) {
						if(elem==v) {
							flag = true
							break
						}
					}
					if(!flag) break
				}
				appendToHistory(v)
				engine.nextPieceArrayObject[i].setColor(v)
			}
		}
	}

	override fun onReady(engine:GameEngine):Boolean {
		if(engine.statc[0]==1) {
			engine.ruleOpt.pieceColor.forEachIndexed {i, it ->
				defaultColors[i] = it
			}
			nonRuleBoundRandomizer = Random(engine.randSeed)
			colorHistory.fill(621)
			if(!ruleBoundMode) {
				randomizeColors(engine, false)
			} else {
				engine.ruleOpt.pieceColor = defaultColors
			}
		}
		return false
	}
	/*
		 * Called for initialization during "Ready" screen
		 */
	override fun startGame(engine:GameEngine) {
		engine.statistics.level = startLevel
		engine.statistics.levelDispAdd = 1
		engine.b2bEnable = true
		engine.comboType = GameEngine.COMBO_TYPE_NORMAL
		engine.big = big
		engine.twistAllowKick = true
		engine.twistEnable = true
		engine.useAllSpinBonus = true
		engine.twistEnableEZ = true
		setSpeed(engine)
		customTimer = 0
		meterValues = IntArray(POWERUP_AMOUNT)
		currentActivePower = -1
		scoreMultiplier = 1
		currentModified = -1
		hasSet = false
		preset = true
		if(netIsWatch) {
			owner.musMan.bgm = BGMStatus.BGM.Silent
		}
		l = -1
	}
	/**
	 * Set the gravity rate
	 *
	 * @param engine GameEngine
	 */
	override fun setSpeed(engine:GameEngine) {
		var lv:Int = engine.statistics.level
		if(lv<0) lv = 0
		if(lv>=tableGravity.size) lv = tableGravity.size-1
		engine.speed.gravity = tableGravity[lv]
		engine.speed.denominator = tableDenominator[lv]
	}

	override fun onFirst(engine:GameEngine) {
		pCoordList?.clear()
	}
	/*
		 * Hard drop
		 */
	override fun afterHardDropFall(engine:GameEngine, fall:Int) {
		engine.statistics.scoreHD += fall*2

		//int x = (16 * engine.nowPieceX) + 4 + receiver.fieldX(engine, playerID) + (16 * engine.nowPieceObject.getWidth() / 2);
		//int y = (16 * engine.nowPieceY) + 52 + receiver.fieldY(engine, playerID) + (16 * engine.nowPieceObject.getHeight() / 2);
		val baseX= 16*engine.nowPieceX+receiver.fieldX(engine)
		val baseY = 16*engine.nowPieceY+receiver.fieldY(engine)
		engine.nowPieceObject?.let {cPiece ->
			for(i in 1..fall) {
				pCoordList!!.add(intArrayOf(engine.nowPieceX, engine.nowPieceY-i))
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
		//RendererExtension.addBlockBreakEffect(receiver, 1, x, y, 1);
		//
//		backgroundCircularRipple.manualRipple(x, y);
	}

	override fun onLast(engine:GameEngine) {
		super.onLast(engine)
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
		 * Render score
		 */
	override fun renderLast(engine:GameEngine) {
		if(owner.menuOnly) return
		receiver.drawScoreFont(engine, 0, 0, "${if(ruleBoundMode) "RULEBOUND " else ""}$name", COLOR.GREEN)
		receiver.drawScoreFont(
			engine, 0, 1, if(tableGameClearLines[goalType]==-1) "(Endless run)" else "(${tableGameClearLines[goalType]} Lines run)",
			COLOR.GREEN
		)
		if(engine.stat===GameEngine.Status.SETTING||engine.stat===GameEngine.Status.RESULT&&!owner.replayMode) {
			if(!owner.replayMode&&!big&&engine.ai==null) {
				val topY = if(receiver.nextDisplayType==2) 6 else 4
				receiver.drawScoreFont(engine, 3, topY-1, "SCORE  LINE TIME", COLOR.BLUE)
				if(showPlayerStats) {
					for(i in 0..<RANKING_MAX) {
						receiver.drawScoreFont(engine, 0, topY+i, "%2d".format(i+1), COLOR.YELLOW)
						val s = "${rankingScorePlayer[if(ruleBoundMode) 1 else 0][goalType][i]}"
						val isLong = s.length>6&&receiver.nextDisplayType!=2
						receiver.drawScoreFont(
							engine, if(isLong) 6 else 3, if(isLong) (topY+i)*2 else topY+i, s, i==rankingRankPlayer,
							if(isLong) .5f else 1f
						)
						receiver.drawScoreFont(
							engine, 10, topY+i, "${rankingLinesPlayer[if(ruleBoundMode) 1 else 0][goalType][i]}",
							i==rankingRankPlayer
						)
						receiver.drawScoreFont(
							engine, 15, topY+i, rankingTimePlayer[if(ruleBoundMode) 1 else 0][goalType][i].toTimeStr,
							i==rankingRankPlayer
						)
					}
					receiver.drawScoreFont(engine, 0, topY+RANKING_MAX+1, "PLAYER SCORES", COLOR.BLUE)
					receiver.drawScoreFont(engine, 0, topY+RANKING_MAX+2, engine.playerProp.nameDisplay, COLOR.WHITE, 2f)
					receiver.drawScoreFont(engine, 0, topY+RANKING_MAX+5, "F:SWITCH RANK SCREEN", COLOR.GREEN)
				} else {
					for(i in 0..<RANKING_MAX) {
						receiver.drawScoreFont(engine, 0, topY+i, "%2d".format(i+1), COLOR.YELLOW)
						val s = "${rankingScore[if(ruleBoundMode) 1 else 0][goalType][i]}"
						val isLong = s.length>6&&receiver.nextDisplayType!=2
						receiver.drawScoreFont(
							engine, if(isLong) 6 else 3, if(isLong) (topY+i)*2 else topY+i, s, i==rankingRank, if(isLong) .5f else 1f
						)
						receiver.drawScoreFont(
							engine, 10, topY+i, "${rankingLines[if(ruleBoundMode) 1 else 0][goalType][i]}", i==rankingRank
						)
						receiver.drawScoreFont(
							engine, 15, topY+i, rankingTime[if(ruleBoundMode) 1 else 0][goalType][i].toTimeStr, i==rankingRank
						)
					}
					receiver.drawScoreFont(engine, 0, topY+RANKING_MAX+1, "LOCAL SCORES", COLOR.BLUE)
					if(!engine.playerProp.isLoggedIn)
						receiver.drawScoreFont(engine, 0, topY+RANKING_MAX+2, "(NOT LOGGED IN)\n(E:LOG IN)")
					if(engine.playerProp.isLoggedIn)
						receiver.drawScoreFont(engine, 0, topY+RANKING_MAX+5, "F:SWITCH RANK SCREEN", COLOR.GREEN)
				}
			}
		} else if(engine.stat===GameEngine.Status.CUSTOM&&!engine.gameActive)
			engine.playerProp.loginScreen.renderScreen(receiver, engine) else {
			receiver.drawScoreFont(engine, 0, 3, "LINE", COLOR.BLUE)
			receiver.drawScoreNum(engine, 5, 2, engine.statistics.lines.toString(), 2f)

			receiver.drawScoreFont(engine, 0, 4, "Score", COLOR.BLUE)
			receiver.drawScoreNum(engine, 5, 4, "+$lastScore")
			val scget = scDisp<engine.statistics.score
			receiver.drawScoreNum(engine, 0, 5, "$scDisp", scget, 2f)

			receiver.drawScoreFont(engine, 0, 8, "Level", COLOR.BLUE)
			receiver.drawScoreNum(
				engine, 5, 8,
				"%.1f".format(
					engine.statistics.level.toFloat()+
						if(engine.statistics.level>=19&&tableGameClearLines[goalType]<0) 1f else engine.statistics.lines%10*0.1f+1f
				), 2f
			)

			receiver.drawScoreFont(engine, 0, 9, "Time", COLOR.BLUE)
			receiver.drawScoreNum(engine, 0, 10, engine.statistics.time.toTimeStr, 2f)

			// Power-up progess
			var scale = 1f
			var base = 12
			if(receiver.nextDisplayType>=1) {
				scale = 0.5f
				base = 24
			}
			receiver.drawScoreFont(
				engine, 10/scale, base/scale, "POWER-UPS", COLOR.PINK, scale
			)
			receiver.drawScoreFont(
				engine, 10/scale, base+1/scale, "  GREY:"+"%d/%d".format(meterValues[0], POWER_METER_MAX),
				POWERUP_TEXT_COLORS[0], scale
			)
			receiver.drawScoreFont(
				engine, 10/scale, base+2/scale, "   RED:"+"%d/%d".format(meterValues[1], POWER_METER_MAX),
				POWERUP_TEXT_COLORS[1], scale
			)
			receiver.drawScoreFont(
				engine, 10/scale, base+3/scale, "ORANGE:"+"%d/%d".format(meterValues[2], POWER_METER_MAX),
				POWERUP_TEXT_COLORS[2], scale
			)
			receiver.drawScoreFont(
				engine, 10/scale, base+4/scale, "YELLOW:"+"%d/%d".format(meterValues[3], POWER_METER_MAX),
				POWERUP_TEXT_COLORS[3], scale
			)
			receiver.drawScoreFont(
				engine, 10/scale, base+5/scale, " GREEN:"+"%d/%d".format(meterValues[4], POWER_METER_MAX),
				POWERUP_TEXT_COLORS[4], scale
			)
			receiver.drawScoreFont(
				engine, 10/scale, base+6/scale, "  CYAN:"+"%d/%d".format(meterValues[5], POWER_METER_MAX),
				POWERUP_TEXT_COLORS[5], scale
			)
			receiver.drawScoreFont(
				engine, 10/scale, base+7/scale, "  BLUE:"+"%d/%d".format(meterValues[6], POWER_METER_MAX),
				POWERUP_TEXT_COLORS[6], scale
			)
			receiver.drawScoreFont(
				engine, 10/scale, base+8/scale, "PURPLE:"+"%d/%d".format(meterValues[7], POWER_METER_MAX),
				POWERUP_TEXT_COLORS[7], scale
			)
			receiver.drawScoreFont(engine, 0, 15, "MULTI.", COLOR.BLUE)
			receiver.drawScoreFont(engine, 0, 16, "$scoreMultiplier"+"X")
			receiver.drawScoreFont(engine, 0, 18, "LIVES", COLOR.GREEN)
			receiver.drawScoreFont(
				engine, 0, 19, if(engine.stat!==GameEngine.Status.GAMEOVER) "${(engine.lives+1)}/5" else "$l/5"
			)
			if(engine.playerProp.isLoggedIn||engine.playerName.isNotEmpty()) {
				receiver.drawScoreFont(engine, 0, 21, "PLAYER", COLOR.BLUE)
				receiver.drawScoreFont(
					engine, 0, 22, if(owner.replayMode) engine.playerName else engine.playerProp.nameDisplay,
					COLOR.WHITE,
					2f
				)
			}
			engine.nowPieceObject?.let {cPiece ->
				val baseX = receiver.fieldX(engine)
				val baseY = receiver.fieldY(engine)
				if(pCoordList!!.size>0) {
					for(loc in pCoordList!!) {
						val cx = baseX+16*loc[0]
						val cy = baseY+16*loc[1]
						receiver.drawPiece(cx, cy, cPiece, 1f, 0f)
					}
				}
			}
			engine.field.let {
				if(engine.stat===GameEngine.Status.CUSTOM&&customTimer<120&&!(currentActivePower==0&&engine.lives>=4)) {
					val offset = (10-POWERUP_NAMES[currentActivePower].length)/2
					receiver.drawMenuFont(
						engine, offset, it.height/2, POWERUP_NAMES[currentActivePower], POWERUP_TEXT_COLORS[currentActivePower]
					)
					receiver.drawMenuFont(engine, 0, it.height/2+1, "ACTIVATED!")
				} else if(currentActivePower==0&&customTimer<120&&engine.stat===GameEngine.Status.CUSTOM&&engine.lives>=4) {
					val offset = (10-"SMALL SCORE BONUS".length)/2
					receiver.drawMenuFont(engine, 0, it.height/2-1, "LIVES FULL!", COLOR.PINK)
					receiver.drawMenuFont(
						engine, offset, it.height/2+1, "SMALL SCORE BONUS", COLOR.PINK
					)
					receiver.drawMenuFont(engine, 0, it.height/2+2, "ACTIVATED!")
				}
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

	override fun onLineClear(engine:GameEngine):Boolean {
		engine.nowPieceObject?.let {cPiece ->
			if(!hasSet) {
				currentModified = addPowerToIndex(cPiece.colors[0]-1)
				hasSet = true
			}
		}
		return false
	}

	override fun onGameOver(engine:GameEngine):Boolean {
		if(l==-1) l = 0
		return false
	}

	override fun onExcellent(engine:GameEngine):Boolean {
		if(engine.lives>0) {
			val bonus:Int = engine.lives*50000*scoreMultiplier
			lastScore = bonus
			engine.statistics.scoreBonus += bonus
			engine.lives = 0
		}
		return false
	}

	override fun onMove(engine:GameEngine):Boolean {
		if(currentModified!=-1) {
			currentActivePower = currentModified
			engine.resetStatc()
			engine.stat = GameEngine.Status.CUSTOM
			customTimer = 0
			return true
		}

		/*		if (engine.statc[0] > 0 && !ruleBoundMode && !preset) {
					int v = -1;
					for (int j = 0; j < 8; j++) {
						boolean flag = false;

						 v = nonRuleBoundRandomizer.nextInt(POWERUP_NAMES.length);
						 for (int elem : colorHistory) {
							if (elem == v) {
								flag = true;
								break;
							}
						 }
						 if (!flag) break;
					}
					appendToHistory(v);

					try {
						engine.nextPieceArrayObject[engine.nextPieceCount + engine.ruleOpt.nextDisplay - 1].setColor(v + 1);
					} catch (IndexOutOfBoundsException e) {
						// DO NOTHING
					}

					preset = true;
				}*/
		if(engine.statc[0]==0&&!ruleBoundMode&&preset&&!engine.holdDisable) {
			preset = false
		}
		hasSet = false
		return false
	}
	// Where the magic happens
	override fun onCustom(engine:GameEngine):Boolean {
		if(engine.gameActive) {
			customTimer++
			if(customTimer>=150) {
				/*
				switch (currentActivePower) {
				case POWERUP_BOTTOMCLEAR:
				case POWERUP_TOPCLEAR:
				case POWERUP_FREEFALL:
					engine.stat = GameEngine.Status.LINECLEAR;
					// engine.stat = GameEngine.Status.MOVE;
					break;
				default:
					engine.stat = GameEngine.Status.MOVE;
					break;
				} */
				engine.resetStatc()
				engine.stat = GameEngine.Status.MOVE
				currentModified = -1
				currentActivePower = 1
				return true
			} else if(customTimer==120) engine.field.let {
				when(currentActivePower) {
					POWERUP_EXTRALIFE -> if(engine.lives<4) {
						engine.lives++
						engine.playSE("cool")
					} else {
						engine.statistics.scoreBonus += 3200*scoreMultiplier*(engine.statistics.level+1)
						lastScore = 3200*scoreMultiplier*(engine.statistics.level+1)
						engine.playSE("medal")
					}
					POWERUP_FREEFALL -> if(it.freeFall()) {
						it.checkLine()
						receiver.blockBreak(engine, it.findBlocks {b -> b.getAttribute(Block.ATTRIBUTE.ERASE)})
						it.clearLine()
						it.downFloatingBlocks()
						engine.playSE("linefall")
					}
					POWERUP_BOTTOMCLEAR -> if(!it.isEmpty) {
						it.delLower()
						receiver.blockBreak(engine, it.findBlocks {b -> b.getAttribute(Block.ATTRIBUTE.ERASE)})
						it.clearLine()
						it.downFloatingBlocks()
						engine.playSE("linefall")
					}
					POWERUP_TOPCLEAR -> if(!it.isEmpty) {
						it.delUpper()
						receiver.blockBreak(engine, it.findBlocks {b -> b.getAttribute(Block.ATTRIBUTE.ERASE)})
						it.clearLine()
						it.downFloatingBlocks()
						engine.playSE("linefall")
					}
					POWERUP_MULTIPLIER -> {
						scoreMultiplier++
						engine.playSE("cool")
					}
					POWERUP_FREEZE -> engine.speed.gravity = 0
					POWERUP_SIDESHIFT -> {
						it.moveLeft()
						engine.playSE("linefall")
					}
					POWERUP_SCOREBONUS -> {
						engine.statistics.scoreBonus += 6400*scoreMultiplier*(engine.statistics.level+1)
						lastScore = 6400*scoreMultiplier*(engine.statistics.level+1)
						engine.playSE("medal")
					}
				}
			}
		} else {
			showPlayerStats = false
			engine.isInGame = true
			val s:Boolean = engine.playerProp.loginScreen.updateScreen(engine)
			if(engine.playerProp.isLoggedIn) {
				loadRankingPlayer(engine.playerProp)
				loadSetting(engine, engine.playerProp.propProfile)
			}
			if(engine.stat===GameEngine.Status.SETTING) engine.isInGame = false
		}
		return false
	}
	/*
		 * Calculate score
		 */
	override fun calcScore(engine:GameEngine, ev:ScoreEvent):Int {
		setSpeed(engine)
		if(!ruleBoundMode) {
			randomizeColors(engine, true)
		} else {
			engine.ruleOpt.pieceColor = defaultColors
		}

		// Line clear bonus
		val li = ev.lines
		val pts = calcScoreBase(engine, ev)
		val cmb = if(ev.combo>0&&li>=1) ev.combo else 0
		// Combo
		val spd = maxOf(0, engine.lockDelay-engine.lockDelayNow)+if(engine.manualLock) 1 else 0
		// Add to score
		var get = 0
		if(pts+cmb+spd>0) {
			get = calcScoreCombo(pts, cmb, engine.statistics.level, spd)*scoreMultiplier
			if(pts>0) lastScore = get
			if(li>=1) engine.statistics.scoreLine += get
			else engine.statistics.scoreBonus += get
			scDisp += spd*scoreMultiplier
		}

		if(li>0&&engine.lives>0&&engine.statistics.lines%100==0&&tableGameClearLines[goalType]<0&&engine.statistics.lines>=200) {
			val bonus:Int = engine.lives*30000*scoreMultiplier
			engine.statistics.scoreBonus += bonus
		}

		// BGM fade-out effects and BGM changes
		if(tableBGMChange[bgmLv]!=-1) {
			if(engine.statistics.lines>=tableBGMChange[bgmLv]-5) owner.musMan.fadeSW = true
			if(engine.statistics.lines>=tableBGMChange[bgmLv]&&
				(engine.statistics.lines<tableGameClearLines[goalType]||tableGameClearLines[goalType]<0)
			) {
				bgmLv++
				owner.musMan.bgm = BGMStatus.BGM.GrandT(bgmLv)
				owner.musMan.fadeSW = false
			}
		}

		// Meter
		engine.meterValue = engine.statistics.lines%10/9f
		engine.meterColor = GameEngine.METER_COLOR_LIMIT
		if(engine.statistics.lines>=tableGameClearLines[goalType]&&tableGameClearLines[goalType]>=0) {
			// Ending
			l = engine.lives
			engine.ending = 1
			engine.gameEnded()
		} else if(engine.statistics.lines>=(engine.statistics.level+1)*10&&engine.statistics.level<19) {
			// Level up
			engine.statistics.level++
			owner.bgMan.nextBg = engine.statistics.level
			setSpeed(engine)
			engine.playSE("levelup")
		}
		return get
	}
	/*
		 * Called when saving replay
		 */
	override fun saveReplay(engine:GameEngine, prop:CustomProperties):Boolean {
		saveSetting(engine, prop)

		// NET: Save name
		if(!netPlayerName.isNullOrEmpty()) {
			prop.setProperty("${engine.playerID}.net.netPlayerName", netPlayerName)
		}

		// Update rankings
		if(!owner.replayMode&&!big&&engine.ai==null) {
			if(updateRanking(
					engine.statistics.score, engine.statistics.lines, engine.statistics.time, goalType,
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
				rankingScore[if(ruleBoundMode) 1 else 0][type][i] = rankingScore[if(ruleBoundMode) 1 else 0][type][i-1]
				rankingLines[if(ruleBoundMode) 1 else 0][type][i] = rankingLines[if(ruleBoundMode) 1 else 0][type][i-1]
				rankingTime[if(ruleBoundMode) 1 else 0][type][i] = rankingTime[if(ruleBoundMode) 1 else 0][type][i-1]
			}

			// Add new data
			rankingScore[if(ruleBoundMode) 1 else 0][type][rankingRank] = sc
			rankingLines[if(ruleBoundMode) 1 else 0][type][rankingRank] = li
			rankingTime[if(ruleBoundMode) 1 else 0][type][rankingRank] = time
		}
		if(isLoggedIn) {
			rankingRankPlayer = checkRankingPlayer(sc, li, time, type)
			if(rankingRankPlayer!=-1) {
				// Shift down ranking entries
				for(i in RANKING_MAX-1 downTo rankingRankPlayer+1) {
					rankingScorePlayer[if(ruleBoundMode) 1 else 0][type][i] =
						rankingScorePlayer[if(ruleBoundMode) 1 else 0][type][i-1]
					rankingLinesPlayer[if(ruleBoundMode) 1 else 0][type][i] =
						rankingLinesPlayer[if(ruleBoundMode) 1 else 0][type][i-1]
					rankingTimePlayer[if(ruleBoundMode) 1 else 0][type][i] =
						rankingTimePlayer[if(ruleBoundMode) 1 else 0][type][i-1]
				}

				// Add new data
				rankingScorePlayer[if(ruleBoundMode) 1 else 0][type][rankingRankPlayer] = sc
				rankingLinesPlayer[if(ruleBoundMode) 1 else 0][type][rankingRankPlayer] = li
				rankingTimePlayer[if(ruleBoundMode) 1 else 0][type][rankingRankPlayer] = time
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
		for(i in 0..<RANKING_MAX) {
			if(sc>rankingScore[if(ruleBoundMode) 1 else 0][type][i]) {
				return i
			} else if(sc==rankingScore[if(ruleBoundMode) 1 else 0][type][i]&&li>rankingLines[if(ruleBoundMode) 1 else 0][type][i]) {
				return i
			} else if(sc==rankingScore[if(ruleBoundMode) 1 else 0][type][i]&&li==rankingLines[if(ruleBoundMode) 1 else 0][type][i]&&time<rankingTime[if(ruleBoundMode) 1 else 0][type][i]) {
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
	private fun checkRankingPlayer(sc:Long, li:Int, time:Int, type:Int):Int {
		for(i in 0..<RANKING_MAX) {
			if(sc>rankingScorePlayer[if(ruleBoundMode) 1 else 0][type][i]) {
				return i
			} else if(sc==rankingScorePlayer[if(ruleBoundMode) 1 else 0][type][i]&&li>rankingLinesPlayer[if(ruleBoundMode) 1 else 0][type][i]) {
				return i
			} else if(sc==rankingScorePlayer[if(ruleBoundMode) 1 else 0][type][i]&&li==rankingLinesPlayer[if(ruleBoundMode) 1 else 0][type][i]&&time<rankingTimePlayer[if(ruleBoundMode) 1 else 0][type][i]) {
				return i
			}
		}
		return -1
	}
	// Add to meter at specific index
	private fun addPowerToIndex(index:Int):Int {
		meterValues[index]++
		if(meterValues[index]>=POWER_METER_MAX) {
			meterValues[index] = 0
			return index
		}
		return -1
	}
	// Append to history
	private fun appendToHistory(`val`:Int) {
		for(i in colorHistory.size-1 downTo 1) {
			colorHistory[i-1] = colorHistory[i]
		}
		colorHistory[colorHistory.size-1] = `val`
	}

	companion object {
		// Power meter max
		private const val POWER_METER_MAX = 10
		// Power-up amounts
		private const val POWERUP_AMOUNT = 8
		// Power-up names
		private val POWERUP_NAMES = arrayOf(
			"Extra LIFE",
			"FREE-FALL",
			"DEL. LOWER-1/2",
			"DEL. UPPER-1/2",
			"MULTIPLIER UP",
			"FREEZE-TIME",
			"SIDE SHIFT",
			"SCORE BONUS"
		)
		private const val POWERUP_EXTRALIFE = 0
		private const val POWERUP_FREEFALL = 1
		private const val POWERUP_BOTTOMCLEAR = 2
		private const val POWERUP_TOPCLEAR = 3
		private const val POWERUP_MULTIPLIER = 4
		private const val POWERUP_FREEZE = 5
		private const val POWERUP_SIDESHIFT = 6
		private const val POWERUP_SCOREBONUS = 7
		// Power-up text colors
		private val POWERUP_TEXT_COLORS = arrayOf(
			COLOR.WHITE,
			COLOR.RED,
			COLOR.ORANGE,
			COLOR.YELLOW,
			COLOR.GREEN,
			COLOR.CYAN,
			COLOR.COBALT,
			COLOR.PURPLE
		)
	}
}
