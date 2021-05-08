/*
 * COLOR POWER mode
 * - Marathon, but with powers given by breaking enough blocks of a certain color.
 * - Enjoy rainbows!
 */
package zeroxfc.nullpo.custom.modes

import mu.nu.nullpo.game.component.*
import mu.nu.nullpo.game.event.EventReceiver
import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.gui.slick.RendererExtension
import mu.nu.nullpo.util.CustomProperties
import mu.nu.nullpo.util.GeneralUtil
import zeroxfc.nullpo.custom.libs.*
import kotlin.random.Random

class ColorPower:MarathonModeBase() {
	var l = 0
	// Power meter values
	private var meterValues:IntArray = intArrayOf()
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
	// Rulebound mode: default = false.
	private var ruleboundMode = false
	// Randomizer for non-rulebound mode
	private var nonRuleboundRandomiser:Random? = null
	// Color history
	private var colorHistory:IntArray = intArrayOf()
	// engine dif
	private var defaultColors:IntArray = intArrayOf()
	// Hm
	private var preset = false
	// LastScore
	private var scoreBeforeIncrease = 0
	/**
	 * Rankings' scores
	 */
	private var rankingScore:Array<Array<IntArray>> = emptyArray()
	/**
	 * Rankings' line counts
	 */
	private var rankingLines:Array<Array<IntArray>> = emptyArray()
	/**
	 * Rankings' times
	 */
	private var rankingTime:Array<Array<IntArray>> = emptyArray()
	/**
	 * Player profile
	 */
	private val playerProperties:ProfileProperties = ProfileProperties(EventReceiver.COLOR.GREEN)
	/**
	 * Player rank
	 */
	private var rankingRankPlayer = 0
	/**
	 * Rankings' scores
	 */
	private var rankingScorePlayer:Array<Array<IntArray>> = emptyArray()
	/**
	 * Rankings' line counts
	 */
	private var rankingLinesPlayer:Array<Array<IntArray>> = emptyArray()
	/**
	 * Rankings' times
	 */
	private var rankingTimePlayer:Array<Array<IntArray>> = emptyArray()
	private var showPlayerStats = false
	/**
	 * The good hard drop effect
	 */
	private var pCoordList:MutableList<IntArray>? = null
	private var PLAYER_NAME:String = ""
	// Mode Name
	override val name:String
		get() = "COLOR POWER"
	/*
     * Initialization
     */
	override fun playerInit(engine:GameEngine, playerID:Int) {
		owner = engine.owner
		lastscore = 0
		scgettime = 0
		bgmlv = 0
		l = -1
		defaultColors = intArrayOf()
		pCoordList = ArrayList()
		customTimer = 0
		meterValues = IntArray(POWERUP_AMOUNT)
		currentActivePower = -1
		scoreMultiplier = 1
		currentModified = -1
		hasSet = false
		ruleboundMode = false
		preset = false
		scoreBeforeIncrease = 0
		colorHistory = intArrayOf(-1, -1, -1, -1)
		playerProperties.reset()
		showPlayerStats = false

		rankingRankPlayer = -1
		rankingScorePlayer = Array(2) {Array(RANKING_TYPE) {IntArray(RANKING_MAX)}}
		rankingLinesPlayer = Array(2) {Array(RANKING_TYPE) {IntArray(RANKING_MAX)}}
		rankingTimePlayer = Array(2) {Array(RANKING_TYPE) {IntArray(RANKING_MAX)}}
		rankingRank = -1
		rankingScore = Array(2) {Array(RANKING_TYPE) {IntArray(RANKING_MAX)}}
		rankingLines = Array(2) {Array(RANKING_TYPE) {IntArray(RANKING_MAX)}}
		rankingTime = Array(2) {Array(RANKING_TYPE) {IntArray(RANKING_MAX)}}
		netPlayerInit(engine, playerID)
		if(!owner.replayMode) {
			loadSetting(owner.modeConfig)
			loadRanking(owner.modeConfig, engine.ruleopt.strRuleName)
			if(playerProperties.isLoggedIn) {
				loadSettingPlayer(playerProperties)
				loadRankingPlayer(playerProperties, engine.ruleopt.strRuleName)
			}
			PLAYER_NAME = ""
			version = CURRENT_VERSION
		} else {
			loadSetting(owner.replayProp)
			if(version==0&&owner.replayProp.getProperty("colorpower.endless", false)) goaltype = 2
			PLAYER_NAME = owner.replayProp.getProperty("colorpower.playerName", "")

			// NET: Load name
			netPlayerName = engine.owner.replayProp.getProperty("$playerID.net.netPlayerName", "")
		}
		engine.owner.backgroundStatus.bg = startlevel
		engine.framecolor = GameEngine.FRAME_COLOR_GREEN
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
			val change = updateCursor(engine, 8, playerID)
			if(change!=0) {
				engine.playSE("change")
				when(engine.statc[2]) {
					0 -> {
						startlevel += change
						if(tableGameClearLines[goaltype]>=0) {
							if(startlevel<0) startlevel = (tableGameClearLines[goaltype]-1)/10
							if(startlevel>(tableGameClearLines[goaltype]-1)/10) startlevel = 0
						} else {
							if(startlevel<0) startlevel = 19
							if(startlevel>19) startlevel = 0
						}
						engine.owner.backgroundStatus.bg = startlevel
					}
					1 -> {
						//enableTSpin = !enableTSpin;
						twistEnableType += change
						if(twistEnableType<0) twistEnableType = 2
						if(twistEnableType>2) twistEnableType = 0
					}
					2 -> enableTSpinKick = !enableTSpinKick
					3 -> twistEnableEZ = !twistEnableEZ
					4 -> enableB2B = !enableB2B
					5 -> enableCombo = !enableCombo
					6 -> {
						goaltype += change
						if(goaltype<0) goaltype = GAMETYPE_MAX-1
						if(goaltype>GAMETYPE_MAX-1) goaltype = 0
						if(startlevel>(tableGameClearLines[goaltype]-1)/10&&tableGameClearLines[goaltype]>=0) {
							startlevel = (tableGameClearLines[goaltype]-1)/10
							engine.owner.backgroundStatus.bg = startlevel
						}
					}
					7 -> big = !big
					8 -> ruleboundMode = !ruleboundMode
				}

				// NET: Signal options change
				if(netIsNetPlay&&netNumSpectators>0) {
					netSendOptions(engine)
				}
			}
			engine.owner.backgroundStatus.bg = startlevel

			// Confirm
			if(engine.ctrl.isPush(Controller.BUTTON_A)&&engine.statc[3]>=5) {
				engine.playSE("decide")
				saveSetting(owner.modeConfig)
				owner.saveModeConfig()

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

	private fun randomizeColors(engine:GameEngine, singlePiece:Boolean) {
		if(singlePiece) {
			var v = -1
			for(j in 0..7) {
				var flag = false
				v = nonRuleboundRandomiser!!.nextInt(8)+1
				for(elem in colorHistory) {
					if(elem==v) {
						flag = true
						break
					}
				}
				if(!flag) break
			}
			appendToHistory(v)
			engine.getNextObject(engine.nextPieceCount+engine.ruleopt.nextDisplay-1)?.setColor(v)
		} else {
			for(i in 0 until engine.nextPieceArrayObject.size) {
				var v = -1
				for(j in 0..7) {
					var flag = false
					v = nonRuleboundRandomiser!!.nextInt(8)+1
					for(elem in colorHistory) {
						if(elem==v) {
							flag = true
							break
						}
					}
					if(!flag) break
				}
				appendToHistory(v)
				engine.nextPieceArrayObject[i]?.setColor(v)
			}
		}
	}
	/*
     * Render the settings screen
     */
	override fun renderSetting(engine:GameEngine, playerID:Int) {
		if(netIsNetRankingDisplayMode) {
			// NET: Netplay Ranking
			netOnRenderNetPlayRanking(engine, playerID, receiver)
		} else {
			var strtwistEnable = ""
			if(version>=2) {
				if(twistEnableType==0) strtwistEnable = "OFF"
				if(twistEnableType==1) strtwistEnable = "T-ONLY"
				if(twistEnableType==2) strtwistEnable = "ALL"
			} else {
				strtwistEnable = GeneralUtil.getONorOFF(enableTSpin)
			}
			drawMenu(engine, playerID, receiver, 0, EventReceiver.COLOR.BLUE, 0,
				"LEVEL", "${startlevel+1}",
				"SPIN BONUS", strtwistEnable,
				"EZ SPIN", GeneralUtil.getONorOFF(enableTSpinKick),
				"EZIMMOBILE", GeneralUtil.getONorOFF(twistEnableEZ),
				"B2B", GeneralUtil.getONorOFF(enableB2B),
				"COMBO", GeneralUtil.getONorOFF(enableCombo),
				"GOAL", if(goaltype==2) "ENDLESS" else "${tableGameClearLines[goaltype]}"+" LINES",
				"BIG", GeneralUtil.getONorOFF(big))
			drawMenu(engine, playerID, receiver, 18, EventReceiver.COLOR.RED, 9,
				"RULEBOUND", GeneralUtil.getONorOFF(ruleboundMode))
		}
	}

	override fun onReady(engine:GameEngine, playerID:Int):Boolean {
		if(engine.statc[0]==1) {
			if(defaultColors==null) defaultColors = engine.ruleopt.pieceColor
			nonRuleboundRandomiser = Random(engine.randSeed)
			colorHistory = intArrayOf(621, 621, 621, 621)
			if(!ruleboundMode) {
				randomizeColors(engine, false)
			} else {
				engine.ruleopt.pieceColor = defaultColors
			}
		}
		return false
	}
	/*
     * Called for initialization during "Ready" screen
     */
	override fun startGame(engine:GameEngine, playerID:Int) {
		engine.statistics.level = startlevel
		engine.statistics.levelDispAdd = 1
		engine.b2bEnable = enableB2B
		if(enableCombo) {
			engine.comboType = GameEngine.COMBO_TYPE_NORMAL
		} else {
			engine.comboType = GameEngine.COMBO_TYPE_DISABLE
		}
		engine.big = big
		if(version>=2) {
			engine.twistAllowKick = enableTSpinKick
			if(twistEnableType==0) {
				engine.twistEnable = false
			} else if(twistEnableType==1) {
				engine.twistEnable = true
			} else {
				engine.twistEnable = true
				engine.useAllSpinBonus = true
			}
		} else {
			engine.twistEnable = enableTSpin
		}
		engine.twistEnableEZ = twistEnableEZ
		setSpeed(engine)
		customTimer = 0
		meterValues = IntArray(POWERUP_AMOUNT)
		currentActivePower = -1
		scoreMultiplier = 1
		currentModified = -1
		hasSet = false
		scoreBeforeIncrease = 0
		preset = true
		if(netIsWatch) {
			owner.bgmStatus.bgm = BGMStatus.BGM.Silent
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

	override fun onFirst(engine:GameEngine, playerID:Int) {
		pCoordList!!.clear()
	}
	/*
     * Hard drop
     */
	override fun afterHardDropFall(engine:GameEngine, playerID:Int, fall:Int) {
		engine.statistics.scoreHD += fall*2

		//int x = (16 * engine.nowPieceX) + 4 + receiver.fieldX(engine, playerID) + (16 * engine.nowPieceObject.getWidth() / 2);
		//int y = (16 * engine.nowPieceY) + 52 + receiver.fieldY(engine, playerID) + (16 * engine.nowPieceObject.getHeight() / 2);
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
		//RendererExtension.addBlockBreakEffect(receiver, 1, x, y, 1);
		//
//		backgroundCircularRipple.manualRipple(x, y);
	}

	override fun onLast(engine:GameEngine, playerID:Int) {
		scgettime++
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
		receiver.drawScoreFont(engine, playerID, 0, 0, (if(ruleboundMode) "RULEBOUND " else "")+name, EventReceiver.COLOR.GREEN)
		if(tableGameClearLines[goaltype]==-1) {
			receiver.drawScoreFont(engine, playerID, 0, 1, "(ENDLESS GAME)", EventReceiver.COLOR.GREEN)
		} else {
			receiver.drawScoreFont(engine, playerID, 0, 1, "("+tableGameClearLines[goaltype]+" LINES GAME)",
				EventReceiver.COLOR.GREEN)
		}
		if(engine.stat===GameEngine.Status.SETTING||engine.stat===GameEngine.Status.RESULT&&!owner.replayMode) {
			if(!owner.replayMode&&!big&&engine.ai==null) {
				val scale = if(receiver.nextDisplayType==2) 0.5f else 1.0f
				val topY = if(receiver.nextDisplayType==2) 6 else 4
				receiver.drawScoreFont(engine, playerID, 3, topY-1, "SCORE  LINE TIME", EventReceiver.COLOR.BLUE, scale)
				if(showPlayerStats) {
					for(i in 0 until RANKING_MAX) {
						receiver.drawScoreFont(engine, playerID, 0, topY+i, String.format("%2d", i+1), EventReceiver.COLOR.YELLOW, scale)
						val s = "${rankingScorePlayer[if(ruleboundMode) 1 else 0][goaltype][i]}"
						receiver.drawScoreFont(engine, playerID, if(s.length>6&&receiver.nextDisplayType!=2) 6 else 3,
							if(s.length>6&&receiver.nextDisplayType!=2) (topY+i)*2 else topY+i, s, i==rankingRankPlayer,
							if(s.length>6&&receiver.nextDisplayType!=2) scale*0.5f else scale)
						receiver.drawScoreFont(engine, playerID, 10, topY+i,
							"${rankingLinesPlayer[if(ruleboundMode) 1 else 0][goaltype][i]}", i==rankingRankPlayer, scale)
						receiver.drawScoreFont(engine, playerID, 15, topY+i, GeneralUtil.getTime(
							rankingTimePlayer[if(ruleboundMode) 1 else 0][goaltype][i]), i==rankingRankPlayer, scale)
					}
					receiver.drawScoreFont(engine, playerID, 0, topY+RANKING_MAX+1, "PLAYER SCORES", EventReceiver.COLOR.BLUE)
					receiver.drawScoreFont(engine, playerID, 0, topY+RANKING_MAX+2, playerProperties.nameDisplay,
						EventReceiver.COLOR.WHITE, 2f)
					receiver.drawScoreFont(engine, playerID, 0, topY+RANKING_MAX+5, "F:SWITCH RANK SCREEN", EventReceiver.COLOR.GREEN)
				} else {
					for(i in 0 until RANKING_MAX) {
						receiver.drawScoreFont(engine, playerID, 0, topY+i, String.format("%2d", i+1), EventReceiver.COLOR.YELLOW, scale)
						val s = "${rankingScore[if(ruleboundMode) 1 else 0][goaltype][i]}"
						receiver.drawScoreFont(engine, playerID, if(s.length>6&&receiver.nextDisplayType!=2) 6 else 3,
							if(s.length>6&&receiver.nextDisplayType!=2) (topY+i)*2 else topY+i, s, i==rankingRank,
							if(s.length>6&&receiver.nextDisplayType!=2) scale*0.5f else scale)
						receiver.drawScoreFont(engine, playerID, 10, topY+i,
							"${rankingLines[if(ruleboundMode) 1 else 0][goaltype][i]}", i==rankingRank, scale)
						receiver.drawScoreFont(engine, playerID, 15, topY+i, GeneralUtil.getTime(
							rankingTime[if(ruleboundMode) 1 else 0][goaltype][i]), i==rankingRank, scale)
					}
					receiver.drawScoreFont(engine, playerID, 0, topY+RANKING_MAX+1, "LOCAL SCORES", EventReceiver.COLOR.BLUE)
					if(!playerProperties.isLoggedIn) receiver.drawScoreFont(engine, playerID, 0, topY+RANKING_MAX+2,
						"(NOT LOGGED IN)\n(E:LOG IN)")
					if(playerProperties.isLoggedIn) receiver.drawScoreFont(engine, playerID, 0, topY+RANKING_MAX+5,
						"F:SWITCH RANK SCREEN", EventReceiver.COLOR.GREEN)
				}
			}
		} else if(engine.stat===GameEngine.Status.CUSTOM&&!engine.gameActive) {
			playerProperties.loginScreen.renderScreen(receiver, engine, playerID)
		} else {
			receiver.drawScoreFont(engine, playerID, 0, 3, "SCORE", EventReceiver.COLOR.BLUE)
			val strScore:String = if(lastscore==0||scgettime>=120)
				engine.statistics.score.toString()
			else
				"${
					Interpolation.sineStep(scoreBeforeIncrease.toDouble(), engine.statistics.score.toDouble(),
						scgettime/120.0)
				}(+$lastscore)"

			receiver.drawScoreFont(engine, playerID, 0, 4, strScore)
			receiver.drawScoreFont(engine, playerID, 0, 6, "LINE", EventReceiver.COLOR.BLUE)
			if(engine.statistics.level>=19&&tableGameClearLines[goaltype]<0) receiver.drawScoreFont(engine, playerID, 0, 7,
				engine.statistics.lines.toString()+"") else receiver.drawScoreFont(engine, playerID, 0, 7,
				engine.statistics.lines.toString()+"/"+(engine.statistics.level+1)*10)
			receiver.drawScoreFont(engine, playerID, 0, 9, "LEVEL", EventReceiver.COLOR.BLUE)
			receiver.drawScoreFont(engine, playerID, 0, 10, (engine.statistics.level+1).toString())
			receiver.drawScoreFont(engine, playerID, 0, 12, "TIME", EventReceiver.COLOR.BLUE)
			receiver.drawScoreFont(engine, playerID, 0, 13, GeneralUtil.getTime(engine.statistics.time))

			// Power-up progess
			var scale = 1f
			var base = 12
			if(receiver.nextDisplayType>=1) {
				scale = 0.5f
				base = 24
			}
			receiver.drawScoreFont(engine, playerID, (10/scale).toInt(), (base/scale).toInt(), "POWER-UPS", EventReceiver.COLOR.PINK,
				scale)
			receiver.drawScoreFont(engine, playerID, (10/scale).toInt(), (base+1/scale).toInt(),
				"  GREY:"+String.format("%d/%d", meterValues[0], POWER_METER_MAX), POWERUP_TEXT_COLORS[0], scale)
			receiver.drawScoreFont(engine, playerID, (10/scale).toInt(), (base+2/scale).toInt(),
				"   RED:"+String.format("%d/%d", meterValues[1], POWER_METER_MAX), POWERUP_TEXT_COLORS[1], scale)
			receiver.drawScoreFont(engine, playerID, (10/scale).toInt(), (base+3/scale).toInt(),
				"ORANGE:"+String.format("%d/%d", meterValues[2], POWER_METER_MAX), POWERUP_TEXT_COLORS[2], scale)
			receiver.drawScoreFont(engine, playerID, (10/scale).toInt(), (base+4/scale).toInt(),
				"YELLOW:"+String.format("%d/%d", meterValues[3], POWER_METER_MAX), POWERUP_TEXT_COLORS[3], scale)
			receiver.drawScoreFont(engine, playerID, (10/scale).toInt(), (base+5/scale).toInt(),
				" GREEN:"+String.format("%d/%d", meterValues[4], POWER_METER_MAX), POWERUP_TEXT_COLORS[4], scale)
			receiver.drawScoreFont(engine, playerID, (10/scale).toInt(), (base+6/scale).toInt(),
				"  CYAN:"+String.format("%d/%d", meterValues[5], POWER_METER_MAX), POWERUP_TEXT_COLORS[5], scale)
			receiver.drawScoreFont(engine, playerID, (10/scale).toInt(), (base+7/scale).toInt(),
				"  BLUE:"+String.format("%d/%d", meterValues[6], POWER_METER_MAX), POWERUP_TEXT_COLORS[6], scale)
			receiver.drawScoreFont(engine, playerID, (10/scale).toInt(), (base+8/scale).toInt(),
				"PURPLE:"+String.format("%d/%d", meterValues[7], POWER_METER_MAX), POWERUP_TEXT_COLORS[7], scale)
			receiver.drawScoreFont(engine, playerID, 0, 15, "MULTI.", EventReceiver.COLOR.BLUE)
			receiver.drawScoreFont(engine, playerID, 0, 16, "$scoreMultiplier"+"X")
			receiver.drawScoreFont(engine, playerID, 0, 18, "LIVES", EventReceiver.COLOR.GREEN)
			receiver.drawScoreFont(engine, playerID, 0, 19,
				if(engine.stat!==GameEngine.Status.GAMEOVER) (engine.lives+1).toString()+"/5" else "$l/5")
			if(playerProperties.isLoggedIn||PLAYER_NAME.isNotEmpty()) {
				receiver.drawScoreFont(engine, playerID, 0, 21, "PLAYER", EventReceiver.COLOR.BLUE)
				receiver.drawScoreFont(engine, playerID, 0, 22, if(owner.replayMode) PLAYER_NAME else playerProperties.nameDisplay,
					EventReceiver.COLOR.WHITE, 2f)
			}
			engine.nowPieceObject?.let {cPiece ->
				val baseX:Int = receiver.fieldX(engine, playerID)+4
				val baseY:Int = receiver.fieldY(engine, playerID)+52
				if(pCoordList!!.size>0) {
					for(loc in pCoordList!!) {
						val cx = baseX+16*loc[0]
						val cy = baseY+16*loc[1]
						receiver.drawPiece(cx, cy, cPiece, 1f, 0f)
					}
				}
			}
			engine.field?.let {
				if(engine.stat===GameEngine.Status.CUSTOM&&customTimer<120&&!(currentActivePower==0&&engine.lives>=4)) {
					val offset = (10-POWERUP_NAMES[currentActivePower].length)/2
					receiver.drawMenuFont(engine, playerID, offset, it.height/2, POWERUP_NAMES[currentActivePower],
						POWERUP_TEXT_COLORS[currentActivePower])
					receiver.drawMenuFont(engine, playerID, 0, it.height/2+1, "ACTIVATED!")
				} else if(currentActivePower==0&&customTimer<120&&engine.stat===GameEngine.Status.CUSTOM&&engine.lives>=4) {
					val offset = (10-"SMALL SCORE BONUS".length)/2
					receiver.drawMenuFont(engine, playerID, 0, it.height/2-1, "LIVES FULL!", EventReceiver.COLOR.PINK)
					receiver.drawMenuFont(engine, playerID, offset, it.height/2+1, "SMALL SCORE BONUS",
						EventReceiver.COLOR.PINK)
					receiver.drawMenuFont(engine, playerID, 0, it.height/2+2, "ACTIVATED!")
				}
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

	override fun onLineClear(engine:GameEngine, playerID:Int):Boolean {
		engine.nowPieceObject?.let {cPiece ->
			if(!hasSet) {
				currentModified = addPowerToIndex(cPiece.colors[0]-1)
				hasSet = true
			}
		}
		return false
	}

	override fun onGameOver(engine:GameEngine, playerID:Int):Boolean {
		if(l==-1) l = 0
		return false
	}

	override fun onExcellent(engine:GameEngine, playerID:Int):Boolean {
		if(engine.lives>0) {
			val bonus:Int = engine.lives*50000*scoreMultiplier
			lastscore = bonus
			scgettime = 0
			scoreBeforeIncrease = engine.statistics.score
			engine.statistics.scoreBonus += bonus
			engine.lives = 0
		}
		return false
	}

	override fun onMove(engine:GameEngine, playerID:Int):Boolean {
		if(currentModified!=-1) {
			currentActivePower = currentModified
			engine.resetStatc()
			engine.stat = GameEngine.Status.CUSTOM
			customTimer = 0
			return true
		}

//		if (engine.statc[0] > 0 && !ruleboundMode && !preset) {
//			int v = -1;
//			for (int j = 0; j < 8; j++) {
//				boolean flag = false;
//				
//				 v = nonRuleboundRandomiser.nextInt(POWERUP_NAMES.length);
//				 for (int elem : colorHistory) {
//					if (elem == v) {
//						flag = true;
//						break;
//					}
//				 }
//				 if (!flag) break;
//			}
//			appendToHistory(v);
//			
//			try {
//				engine.nextPieceArrayObject[engine.nextPieceCount + engine.ruleopt.nextDisplay - 1].setColor(v + 1);
//			} catch (IndexOutOfBoundsException e) {
//				// DO NOTHING
//			}
//			
//			preset = true;
//		}
		if(engine.statc[0]==0&&!ruleboundMode&&preset&&!engine.holdDisable) {
			preset = false
		}
		hasSet = false
		return false
	}
	// Where the magic happens
	override fun onCustom(engine:GameEngine, playerID:Int):Boolean {
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
			} else if(customTimer==120) engine.field?.let {
				when(currentActivePower) {
					POWERUP_EXTRALIFE -> if(engine.lives<4) {
						engine.lives++
						engine.playSE("cool")
					} else {
						scgettime = 0
						scoreBeforeIncrease = engine.statistics.score
						engine.statistics.scoreBonus += 3200*scoreMultiplier*(engine.statistics.level+1)
						lastscore = 3200*scoreMultiplier*(engine.statistics.level+1)
						engine.playSE("medal")
					}
					POWERUP_FREEFALL -> if(it.freeFall()) {
						it.checkLine()
						it.clearLine()
						var i = 0
						while(i<it.height) {
							if(it.getLineFlag(i)) {
								var j = 0
								while(j<it.width) {
									val blk = it.getBlock(j, i)
									if(blk!=null) {
										// blockBreak(engine, j, i, blk);
										receiver.blockBreak(engine, j, i, blk)
									}
									j++
								}
							}
							i++
						}
						it.downFloatingBlocks()
						engine.playSE("linefall")
					}
					POWERUP_BOTTOMCLEAR -> if(!it.isEmpty) {
						it.delLower()
						it.clearLine()
						var i = 0
						while(i<it.height) {
							if(it.getLineFlag(i)) {
								var j = 0
								while(j<it.width) {
									val blk = it.getBlock(j, i)
									if(blk!=null) {
										// blockBreak(engine, j, i, blk);
										receiver.blockBreak(engine, j, i, blk)
									}
									j++
								}
							}
							i++
						}
						it.downFloatingBlocks()
						engine.playSE("linefall")
					}
					POWERUP_TOPCLEAR -> if(!it.isEmpty) {
						it.delUpperFix()
						it.clearLine()
						var i = 0
						while(i<it.height) {
							if(it.getLineFlag(i)) {
								var j = 0
								while(j<it.width) {
									val blk = it.getBlock(j, i)
									if(blk!=null) {
										// blockBreak(engine, j, i, blk);
										receiver.blockBreak(engine, j, i, blk)
									}
									j++
								}
							}
							i++
						}
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
						scgettime = 0
						scoreBeforeIncrease = engine.statistics.score
						engine.statistics.scoreBonus += 6400*scoreMultiplier*(engine.statistics.level+1)
						lastscore = 6400*scoreMultiplier*(engine.statistics.level+1)
						engine.playSE("medal")
					}
				}
			}
		} else {
			showPlayerStats = false
			engine.isInGame = true
			val s:Boolean = playerProperties.loginScreen.updateScreen(engine, playerID)
			if(playerProperties.isLoggedIn) {
				loadRankingPlayer(playerProperties, engine.ruleopt.strRuleName)
				loadSettingPlayer(playerProperties)
			}
			if(engine.stat===GameEngine.Status.SETTING) engine.isInGame = false
		}
		return false
	}
	/*
     * Calculate score
     */
	override fun calcScore(engine:GameEngine, playerID:Int, lines:Int):Int {
		setSpeed(engine)
		if(!ruleboundMode) {
			randomizeColors(engine, true)
		} else {
			engine.ruleopt.pieceColor = defaultColors
		}

		// Line clear bonus
		val pts = calcScore(engine, lines)
		val cmb = if(engine.combo>=1&&lines>=1) engine.combo-1 else 0
		var get = 0
		// Combo
		val spd = maxOf(0, engine.lockDelay-engine.lockDelayNow)+if(engine.manualLock) 1 else 0
		// Add to score
		if(pts+cmb+spd>0) {
			get = pts*(10+engine.statistics.level)/10+spd
			if(cmb>=1) {
				var b = sum*(1+cmb)/2
				sum += get
				b = sum*(2+cmb)/2-b
				get = b
			} else
				sum = get
			get *= scoreMultiplier
			if(pts>0) lastscore = get

			scoreBeforeIncrease = engine.statistics.score
			if(lines>=1) engine.statistics.scoreLine += get
			else engine.statistics.scoreBonus += get
			scgettime += spd*scoreMultiplier
		}

		if(lines>0&&engine.lives>0&&engine.statistics.lines%100==0&&tableGameClearLines[goaltype]<0&&engine.statistics.lines>=200) {
			val bonus:Int = engine.lives*30000*scoreMultiplier
			engine.statistics.scoreBonus += bonus
		}

		// BGM fade-out effects and BGM changes
		if(tableBGMChange[bgmlv]!=-1) {
			if(engine.statistics.lines>=tableBGMChange[bgmlv]-5) owner.bgmStatus.fadesw = true
			if(engine.statistics.lines>=tableBGMChange[bgmlv]&&
				(engine.statistics.lines<tableGameClearLines[goaltype]||tableGameClearLines[goaltype]<0)) {
				bgmlv++
				owner.bgmStatus.bgm = BGMStatus.BGM.GrandT(bgmlv)
				owner.bgmStatus.fadesw = false
			}
		}

		// Meter
		engine.meterValue = engine.statistics.lines%10*receiver.getMeterMax(engine)/9
		engine.meterColor = GameEngine.METER_COLOR_GREEN
		if(engine.statistics.lines%10>=4) engine.meterColor = GameEngine.METER_COLOR_YELLOW
		if(engine.statistics.lines%10>=6) engine.meterColor = GameEngine.METER_COLOR_ORANGE
		if(engine.statistics.lines%10>=8) engine.meterColor = GameEngine.METER_COLOR_RED
		if(engine.statistics.lines>=tableGameClearLines[goaltype]&&tableGameClearLines[goaltype]>=0) {
			// Ending
			l = engine.lives
			engine.ending = 1
			engine.gameEnded()
		} else if(engine.statistics.lines>=(engine.statistics.level+1)*10&&engine.statistics.level<19) {
			// Level up
			engine.statistics.level++
			owner.backgroundStatus.fadesw = true
			owner.backgroundStatus.fadecount = 0
			owner.backgroundStatus.fadebg = engine.statistics.level
			setSpeed(engine)
			engine.playSE("levelup")
		}
		return get
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
			updateRanking(engine.statistics.score, engine.statistics.lines, engine.statistics.time, goaltype)
			if(playerProperties.isLoggedIn) {
				prop.setProperty("colorpower.playerName", playerProperties.nameDisplay)
			}
			if(rankingRank!=-1) {
				saveRanking(owner.modeConfig, engine.ruleopt.strRuleName)
				owner.saveModeConfig()
			}
			if(rankingRankPlayer!=-1&&playerProperties.isLoggedIn) {
				saveRankingPlayer(playerProperties, engine.ruleopt.strRuleName)
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
		startlevel = prop.getProperty("colorpower.startlevel", 0)
		twistEnableType = prop.getProperty("colorpower.twistEnableType", 1)
		enableTSpin = prop.getProperty("colorpower.enableTSpin", true)
		enableTSpinKick = prop.getProperty("colorpower.enableTSpinKick", true)
		twistEnableEZ = prop.getProperty("colorpower.twistEnableEZ", false)
		enableB2B = prop.getProperty("colorpower.enableB2B", true)
		enableCombo = prop.getProperty("colorpower.enableCombo", true)
		goaltype = prop.getProperty("colorpower.gametype", 0)
		big = prop.getProperty("colorpower.big", false)
		version = prop.getProperty("colorpower.version", 0)
		ruleboundMode = prop.getProperty("colorpower.rulebound", false)
	}
	/**
	 * Save settings to property file
	 *
	 * @param prop Property file
	 */
	override fun saveSetting(prop:CustomProperties) {
		prop.setProperty("colorpower.startlevel", startlevel)
		prop.setProperty("colorpower.twistEnableType", twistEnableType)
		prop.setProperty("colorpower.enableTSpin", enableTSpin)
		prop.setProperty("colorpower.enableTSpinKick", enableTSpinKick)
		prop.setProperty("colorpower.twistEnableEZ", twistEnableEZ)
		prop.setProperty("colorpower.enableB2B", enableB2B)
		prop.setProperty("colorpower.enableCombo", enableCombo)
		prop.setProperty("colorpower.gametype", goaltype)
		prop.setProperty("colorpower.big", big)
		prop.setProperty("colorpower.version", version)
		prop.setProperty("colorpower.rulebound", ruleboundMode)
	}
	/**
	 * Load settings from property file
	 *
	 * @param prop Property file
	 */
	private fun loadSettingPlayer(prop:ProfileProperties?) {
		if(prop?.isLoggedIn!=true) return
		startlevel = prop.getProperty("colorpower.startlevel", 0)
		twistEnableType = prop.getProperty("colorpower.twistEnableType", 1)
		enableTSpin = prop.getProperty("colorpower.enableTSpin", true)
		enableTSpinKick = prop.getProperty("colorpower.enableTSpinKick", true)
		twistEnableEZ = prop.getProperty("colorpower.twistEnableEZ", false)
		enableB2B = prop.getProperty("colorpower.enableB2B", true)
		enableCombo = prop.getProperty("colorpower.enableCombo", true)
		goaltype = prop.getProperty("colorpower.gametype", 0)
		big = prop.getProperty("colorpower.big", false)
		ruleboundMode = prop.getProperty("colorpower.rulebound", false)
	}
	/**
	 * Save settings to property file
	 *
	 * @param prop Property file
	 */
	private fun saveSettingPlayer(prop:ProfileProperties) {
		if(!prop.isLoggedIn) return
		prop.setProperty("colorpower.startlevel", startlevel)
		prop.setProperty("colorpower.twistEnableType", twistEnableType)
		prop.setProperty("colorpower.enableTSpin", enableTSpin)
		prop.setProperty("colorpower.enableTSpinKick", enableTSpinKick)
		prop.setProperty("colorpower.twistEnableEZ", twistEnableEZ)
		prop.setProperty("colorpower.enableB2B", enableB2B)
		prop.setProperty("colorpower.enableCombo", enableCombo)
		prop.setProperty("colorpower.gametype", goaltype)
		prop.setProperty("colorpower.big", big)
		prop.setProperty("colorpower.rulebound", ruleboundMode)
	}
	/**
	 * Read rankings from property file
	 *
	 * @param prop     Property file
	 * @param ruleName Rule name
	 */
	override fun loadRanking(prop:CustomProperties, ruleName:String) {
		for(h in 0..1) {
			for(i in 0 until RANKING_MAX) {
				for(j in 0 until GAMETYPE_MAX) {
					rankingScore[h][j][i] = prop.getProperty("colorpower.ranking.$ruleName.$h.$j.score.$i", 0)
					rankingLines[h][j][i] = prop.getProperty("colorpower.ranking.$ruleName.$h.$j.lines.$i", 0)
					rankingTime[h][j][i] = prop.getProperty("colorpower.ranking.$ruleName.$h.$j.time.$i", 0)
				}
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
		for(h in 0..1) {
			for(i in 0 until RANKING_MAX) {
				for(j in 0 until GAMETYPE_MAX) {
					prop.setProperty("colorpower.ranking.$ruleName.$h.$j.score.$i", rankingScore[h][j][i])
					prop.setProperty("colorpower.ranking.$ruleName.$h.$j.lines.$i", rankingLines[h][j][i])
					prop.setProperty("colorpower.ranking.$ruleName.$h.$j.time.$i", rankingTime[h][j][i])
				}
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
		for(h in 0..1) {
			for(i in 0 until RANKING_MAX) {
				for(j in 0 until GAMETYPE_MAX) {
					rankingScorePlayer[h][j][i] = prop.getProperty("colorpower.ranking.$ruleName.$h.$j.score.$i", 0)
					rankingLinesPlayer[h][j][i] = prop.getProperty("colorpower.ranking.$ruleName.$h.$j.lines.$i", 0)
					rankingTimePlayer[h][j][i] = prop.getProperty("colorpower.ranking.$ruleName.$h.$j.time.$i", 0)
				}
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
		for(h in 0..1) {
			for(i in 0 until RANKING_MAX) {
				for(j in 0 until GAMETYPE_MAX) {
					prop.setProperty("colorpower.ranking.$ruleName.$h.$j.score.$i", rankingScorePlayer[h][j][i])
					prop.setProperty("colorpower.ranking.$ruleName.$h.$j.lines.$i", rankingLinesPlayer[h][j][i])
					prop.setProperty("colorpower.ranking.$ruleName.$h.$j.time.$i", rankingTimePlayer[h][j][i])
				}
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
	private fun updateRanking(sc:Int, li:Int, time:Int, type:Int) {
		rankingRank = checkRanking(sc, li, time, type)
		if(rankingRank!=-1) {
			// Shift down ranking entries
			for(i in RANKING_MAX-1 downTo rankingRank+1) {
				rankingScore[if(ruleboundMode) 1 else 0][type][i] = rankingScore[if(ruleboundMode) 1 else 0][type][i-1]
				rankingLines[if(ruleboundMode) 1 else 0][type][i] = rankingLines[if(ruleboundMode) 1 else 0][type][i-1]
				rankingTime[if(ruleboundMode) 1 else 0][type][i] = rankingTime[if(ruleboundMode) 1 else 0][type][i-1]
			}

			// Add new data
			rankingScore[if(ruleboundMode) 1 else 0][type][rankingRank] = sc
			rankingLines[if(ruleboundMode) 1 else 0][type][rankingRank] = li
			rankingTime[if(ruleboundMode) 1 else 0][type][rankingRank] = time
		}
		if(playerProperties.isLoggedIn) {
			rankingRankPlayer = checkRankingPlayer(sc, li, time, type)
			if(rankingRankPlayer!=-1) {
				// Shift down ranking entries
				for(i in RANKING_MAX-1 downTo rankingRankPlayer+1) {
					rankingScorePlayer[if(ruleboundMode) 1 else 0][type][i] = rankingScorePlayer[if(ruleboundMode) 1 else 0][type][i-1]
					rankingLinesPlayer[if(ruleboundMode) 1 else 0][type][i] = rankingLinesPlayer[if(ruleboundMode) 1 else 0][type][i-1]
					rankingTimePlayer[if(ruleboundMode) 1 else 0][type][i] = rankingTimePlayer[if(ruleboundMode) 1 else 0][type][i-1]
				}

				// Add new data
				rankingScorePlayer[if(ruleboundMode) 1 else 0][type][rankingRankPlayer] = sc
				rankingLinesPlayer[if(ruleboundMode) 1 else 0][type][rankingRankPlayer] = li
				rankingTimePlayer[if(ruleboundMode) 1 else 0][type][rankingRankPlayer] = time
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
	private fun checkRanking(sc:Int, li:Int, time:Int, type:Int):Int {
		for(i in 0 until RANKING_MAX) {
			if(sc>rankingScore[if(ruleboundMode) 1 else 0][type][i]) {
				return i
			} else if(sc==rankingScore[if(ruleboundMode) 1 else 0][type][i]&&li>rankingLines[if(ruleboundMode) 1 else 0][type][i]) {
				return i
			} else if(sc==rankingScore[if(ruleboundMode) 1 else 0][type][i]&&li==rankingLines[if(ruleboundMode) 1 else 0][type][i]&&time<rankingTime[if(ruleboundMode) 1 else 0][type][i]) {
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
		for(i in 0 until RANKING_MAX) {
			if(sc>rankingScorePlayer[if(ruleboundMode) 1 else 0][type][i]) {
				return i
			} else if(sc==rankingScorePlayer[if(ruleboundMode) 1 else 0][type][i]&&li>rankingLinesPlayer[if(ruleboundMode) 1 else 0][type][i]) {
				return i
			} else if(sc==rankingScorePlayer[if(ruleboundMode) 1 else 0][type][i]&&li==rankingLinesPlayer[if(ruleboundMode) 1 else 0][type][i]&&time<rankingTimePlayer[if(ruleboundMode) 1 else 0][type][i]) {
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
			EventReceiver.COLOR.WHITE,
			EventReceiver.COLOR.RED,
			EventReceiver.COLOR.ORANGE,
			EventReceiver.COLOR.YELLOW,
			EventReceiver.COLOR.GREEN,
			EventReceiver.COLOR.CYAN,
			EventReceiver.COLOR.COBALT,
			EventReceiver.COLOR.PURPLE)
	}
}