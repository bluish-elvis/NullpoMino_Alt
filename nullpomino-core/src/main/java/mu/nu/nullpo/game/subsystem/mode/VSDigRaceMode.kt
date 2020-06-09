package mu.nu.nullpo.game.subsystem.mode

import mu.nu.nullpo.game.component.BGMStatus.BGM
import mu.nu.nullpo.game.component.Block
import mu.nu.nullpo.game.component.Block.COLOR
import mu.nu.nullpo.game.component.Controller
import mu.nu.nullpo.game.event.EventReceiver
import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.game.play.GameManager
import mu.nu.nullpo.util.CustomProperties
import mu.nu.nullpo.util.GeneralUtil
import java.util.*

/** VS-DIG RACE mode */
class VSDigRaceMode:AbstractMode() {

	/** Each player's frame cint */
	private val PLAYER_COLOR_FRAME = intArrayOf(GameEngine.FRAME_COLOR_RED, GameEngine.FRAME_COLOR_BLUE)

	/** Number of garbage lines to clear */
	private var goalLines:IntArray = IntArray(0)

	/** Rate of garbage holes change */
	private var garbagePercent:IntArray = IntArray(0)

	/** BGM number */
	private var bgmno:Int = 0

	/** Sound effects ON/OFF */
	private var enableSE:BooleanArray = BooleanArray(0)

	/** Last preset number used */
	private var presetNumber:IntArray = IntArray(0)

	/** Winner player ID */
	private var winnerID:Int = 0

	/** Win count for each player */
	private var winCount:IntArray = IntArray(0)

	/** Version */
	private var version:Int = 0

	/* Mode name */
	override val name:String
		get() = "VS-DIG RACE"

	override val isVSMode:Boolean
		get() = true

	/* Number of players */
	override val players:Int
		get() = MAX_PLAYERS

	/* Mode init */
	override fun modeInit(manager:GameManager) {
		owner = manager
		receiver = manager.receiver

		goalLines = IntArray(MAX_PLAYERS)
		garbagePercent = IntArray(MAX_PLAYERS)
		bgmno = 0
		enableSE = BooleanArray(MAX_PLAYERS)
		presetNumber = IntArray(MAX_PLAYERS)
		winnerID = -1
		winCount = IntArray(MAX_PLAYERS)
		version = CURRENT_VERSION
	}

	/** Read speed presets
	 * @param engine GameEngine
	 * @param prop Property file to read from
	 * @param preset Preset number
	 */
	private fun loadPreset(engine:GameEngine, prop:CustomProperties, preset:Int) {
		engine.speed.gravity = prop.getProperty("vsdigrace.gravity.$preset", 4)
		engine.speed.denominator = prop.getProperty("vsdigrace.denominator.$preset", 256)
		engine.speed.are = prop.getProperty("vsdigrace.are.$preset", 10)
		engine.speed.areLine = prop.getProperty("vsdigrace.areLine.$preset", 5)
		engine.speed.lineDelay = prop.getProperty("vsdigrace. lineDelay.$preset", 20)
		engine.speed.lockDelay = prop.getProperty("vsdigrace.lockDelay.$preset", 30)
		engine.speed.das = prop.getProperty("vsdigrace.das.$preset", 14)
	}

	/** Save speed presets
	 * @param engine GameEngine
	 * @param prop Property file to save to
	 * @param preset Preset number
	 */
	private fun savePreset(engine:GameEngine, prop:CustomProperties, preset:Int) {
		prop.setProperty("vsdigrace.gravity.$preset", engine.speed.gravity)
		prop.setProperty("vsdigrace.denominator.$preset", engine.speed.denominator)
		prop.setProperty("vsdigrace.are.$preset", engine.speed.are)
		prop.setProperty("vsdigrace.areLine.$preset", engine.speed.areLine)
		prop.setProperty("vsdigrace.lineDelay.$preset", engine.speed.lineDelay)
		prop.setProperty("vsdigrace.lockDelay.$preset", engine.speed.lockDelay)
		prop.setProperty("vsdigrace.das.$preset", engine.speed.das)
	}

	/** Load settings not related to speeds
	 * @param engine GameEngine
	 * @param prop Property file to read from
	 */
	private fun loadOtherSetting(engine:GameEngine, prop:CustomProperties) {
		val playerID = engine.playerID
		goalLines[playerID] = prop.getProperty("vsdigrace.goalLines.p$playerID", 18)
		garbagePercent[playerID] = prop.getProperty("vsdigrace.garbagePercent.p$playerID", 100)
		bgmno = prop.getProperty("vsdigrace.bgmno", 0)
		enableSE[playerID] = prop.getProperty("vsdigrace.enableSE.p$playerID", true)
		presetNumber[playerID] = prop.getProperty("vsdigrace.presetNumber.p$playerID", 0)
	}

	/** Save settings not related to speeds
	 * @param engine GameEngine
	 * @param prop Property file to save to
	 */
	private fun saveOtherSetting(engine:GameEngine, prop:CustomProperties) {
		val playerID = engine.playerID
		prop.setProperty("vsdigrace.goalLines.p$playerID", goalLines[playerID])
		prop.setProperty("vsdigrace.garbagePercent.p$playerID", garbagePercent[playerID])
		prop.setProperty("vsdigrace.bgmno", bgmno)
		prop.setProperty("vsdigrace.enableSE.p$playerID", enableSE[playerID])
		prop.setProperty("vsdigrace.presetNumber.p$playerID", presetNumber[playerID])
	}

	/* Initialization for each player */
	override fun playerInit(engine:GameEngine, playerID:Int) {
		if(playerID==1) {
			engine.randSeed = owner.engine[0].randSeed
			engine.random = Random(owner.engine[0].randSeed)
		}

		engine.framecolor = PLAYER_COLOR_FRAME[playerID]

		if(!engine.owner.replayMode) {
			version = CURRENT_VERSION
			loadOtherSetting(engine, engine.owner.modeConfig)
			loadPreset(engine, engine.owner.modeConfig, -1-playerID)
		} else {
			version = owner.replayProp.getProperty("vsbattle.version", 0)
			loadOtherSetting(engine, engine.owner.replayProp)
			loadPreset(engine, engine.owner.replayProp, -1-playerID)
		}
	}

	/* Called at settings screen */
	override fun onSetting(engine:GameEngine, playerID:Int):Boolean {
		// Menu
		if(!engine.owner.replayMode&&engine.statc[4]==0) {
			// Configuration changes
			val change = updateCursor(engine, 12, playerID)

			if(change!=0) {
				engine.playSE("change")

				var m = 1
				if(engine.ctrl.isPress(Controller.BUTTON_E)) m = 100
				if(engine.ctrl.isPress(Controller.BUTTON_F)) m = 1000

				when(menuCursor) {
					0 -> {
						engine.speed.gravity += change*m
						if(engine.speed.gravity<-1) engine.speed.gravity = 99999
						if(engine.speed.gravity>99999) engine.speed.gravity = -1
					}
					1 -> {
						engine.speed.denominator += change*m
						if(engine.speed.denominator<-1) engine.speed.denominator = 99999
						if(engine.speed.denominator>99999) engine.speed.denominator = -1
					}
					2 -> {
						engine.speed.are += change
						if(engine.speed.are<0) engine.speed.are = 99
						if(engine.speed.are>99) engine.speed.are = 0
					}
					3 -> {
						engine.speed.areLine += change
						if(engine.speed.areLine<0) engine.speed.areLine = 99
						if(engine.speed.areLine>99) engine.speed.areLine = 0
					}
					4 -> {
						engine.speed.lineDelay += change
						if(engine.speed.lineDelay<0) engine.speed.lineDelay = 99
						if(engine.speed.lineDelay>99) engine.speed.lineDelay = 0
					}
					5 -> {
						engine.speed.lockDelay += change
						if(engine.speed.lockDelay<0) engine.speed.lockDelay = 99
						if(engine.speed.lockDelay>99) engine.speed.lockDelay = 0
					}
					6 -> {
						engine.speed.das += change
						if(engine.speed.das<0) engine.speed.das = 99
						if(engine.speed.das>99) engine.speed.das = 0
					}
					7, 8 -> {
						presetNumber[playerID] += change
						if(presetNumber[playerID]<0) presetNumber[playerID] = 99
						if(presetNumber[playerID]>99) presetNumber[playerID] = 0
					}
					9 -> {
						goalLines[playerID] += change
						if(goalLines[playerID]<1) goalLines[playerID] = 18
						if(goalLines[playerID]>18) goalLines[playerID] = 1
					}
					10 -> {
						garbagePercent[playerID] += change
						if(garbagePercent[playerID]<0) garbagePercent[playerID] = 100
						if(garbagePercent[playerID]>100) garbagePercent[playerID] = 0
					}
					11 -> enableSE[playerID] = !enableSE[playerID]
					12 -> {
						bgmno += change
						if(bgmno<0) bgmno = BGM.count-1
						if(bgmno>=BGM.count) bgmno = 0
					}
				}
			}

			// Confirm
			if(engine.ctrl.isPush(Controller.BUTTON_A)&&menuTime>=5) {
				engine.playSE("decide")

				if(menuCursor==7)
					loadPreset(engine, owner.modeConfig, presetNumber[playerID])
				else if(menuCursor==8) {
					savePreset(engine, owner.modeConfig, presetNumber[playerID])
					owner.saveModeConfig()
				} else {
					saveOtherSetting(engine, owner.modeConfig)
					savePreset(engine, owner.modeConfig, -1-playerID)
					owner.saveModeConfig()
					engine.statc[4] = 1
				}
			}

			// Cancel
			if(engine.ctrl.isPush(Controller.BUTTON_B)) engine.quitflag = true

			menuTime++
		} else if(engine.statc[4]==0) {
			// Replay start
			menuTime++
			menuCursor = 0

			if(menuTime>=60) menuCursor = 9
			if(menuTime>=120) engine.statc[4] = 1
		} else // Start the game when both players are ready
			if(owner.engine[0].statc[4]==1&&owner.engine[1].statc[4]==1&&playerID==1) {
				owner.engine[0].stat = GameEngine.Status.READY
				owner.engine[1].stat = GameEngine.Status.READY
				owner.engine[0].resetStatc()
				owner.engine[1].resetStatc()
			} else if(engine.ctrl.isPush(Controller.BUTTON_B)) engine.statc[4] = 0// Cancel

		return true
	}

	/* Settings screen */
	override fun renderSetting(engine:GameEngine, playerID:Int) {
		if(engine.statc[4]==0) {
			if(menuCursor<9) {
				drawMenu(engine, playerID, receiver, 0, EventReceiver.COLOR.ORANGE, 0, "GRAVITY", engine.speed.gravity.toString(), "G-MAX", engine.speed.denominator.toString(), "ARE", engine.speed.are.toString(), "ARE LINE", engine.speed.areLine.toString(), "LINE DELAY", engine.speed.lineDelay.toString(), "LOCK DELAY", engine.speed.lockDelay.toString(), "DAS", engine.speed.das.toString())
				drawMenu(engine, playerID, receiver, 14, EventReceiver.COLOR.GREEN, 7, "LOAD", "${presetNumber[playerID]}", "SAVE", "${presetNumber[playerID]}")
			} else {
				drawMenu(engine, playerID, receiver, 0, EventReceiver.COLOR.CYAN, 9, "GOAL", "${goalLines[playerID]}", "CHANGERATE",
					"${garbagePercent[playerID]}%", "SE", GeneralUtil.getONorOFF(enableSE[playerID]))
				drawMenu(engine, playerID, receiver, 6, EventReceiver.COLOR.PINK, 12, "BGM", "${BGM.values[bgmno]}")
			}
		} else
			receiver.drawMenuFont(engine, playerID, 3, 10, "WAIT", EventReceiver.COLOR.YELLOW)
	}

	/* Ready */
	override fun onReady(engine:GameEngine, playerID:Int):Boolean {
		if(engine.statc[0]==0) {
			engine.createFieldIfNeeded()
			fillGarbage(engine, playerID)

			// Update meter
			val remainLines = getRemainGarbageLines(engine, playerID)
			engine.meterValue = remainLines*receiver.getBlockSize(engine)
			engine.meterColor = GameEngine.METER_COLOR_GREEN
		}
		return false
	}

	/* Called at game start */
	override fun startGame(engine:GameEngine, playerID:Int) {
		engine.enableSE = enableSE[playerID]
		if(playerID==1) owner.bgmStatus.bgm = BGM.values[bgmno]

		engine.meterColor = GameEngine.METER_COLOR_GREEN
		engine.meterValue = receiver.getMeterMax(engine)
	}

	/** Fill the playfield with garbage
	 * @param engine GameEngine
	 */
	private fun fillGarbage(engine:GameEngine, playerID:Int) {
		val w:Int = engine.field!!.width
		val h:Int = engine.field!!.height
		var hole:Int = -1

		for(y:Int in h-1 downTo h-goalLines[playerID]) {
			if(hole==-1||engine.random.nextInt(100)<garbagePercent[playerID]) {
				var newhole = -1
				do
					newhole = engine.random.nextInt(w)
				while(newhole==hole)
				hole = newhole
			}

			var prevColor:COLOR? = null
			for(x:Int in 0 until w)
				if(x!=hole) {
					var color:COLOR = COLOR.WHITE
					if(y==h-1) {
						do
							color = COLOR.values()[1+engine.random.nextInt(7)]
						while(color==prevColor)
						prevColor = color
					}
					engine.field!!.setBlock(x, y, Block(color, if(y==h-1) Block.TYPE.BLOCK else Block.TYPE.GEM,
						engine.skin, Block.ATTRIBUTE.VISIBLE, Block.ATTRIBUTE.GARBAGE))
				}
			// Set connections
			if(receiver.isStickySkin(engine)&&y!=h-1)
				for(x:Int in 0 until w)
					if(x!=hole) {
						val blk = engine.field!!.getBlock(x, y)
						if(blk!=null) {
							if(!engine.field!!.getBlockEmpty(x-1, y)) blk.setAttribute(true, Block.ATTRIBUTE.CONNECT_LEFT)
							if(!engine.field!!.getBlockEmpty(x+1, y))
								blk.setAttribute(true, Block.ATTRIBUTE.CONNECT_RIGHT)
						}
					}
		}
	}

	private fun getRemainGarbageLines(engine:GameEngine?, playerID:Int):Int {
		if(engine?.field==null) return -1

		val w:Int = engine.field!!.width
		val h:Int = engine.field!!.height
		var lines:Int = 0
		var hasGemBlock = false

		for(y:Int in h-1 downTo h-goalLines[playerID])
			if(!engine.field!!.getLineFlag(y))
				for(x:Int in 0 until w) {
					val blk = engine.field!!.getBlock(x, y)

					if(blk!=null&&blk.isGemBlock) hasGemBlock = true
					if(blk!=null&&blk.getAttribute(Block.ATTRIBUTE.GARBAGE)) {
						lines++
						break
					}
				}

		return if(!hasGemBlock) 0 else lines

	}

	/* Render score */
	override fun renderLast(engine:GameEngine, playerID:Int) {
		var enemyID = 0
		if(playerID==0) enemyID = 1

		val x:Int = receiver.fieldX(engine, playerID)
		val y:Int = receiver.fieldY(engine, playerID)

		val remainLines = maxOf(0, getRemainGarbageLines(engine, playerID))
		var fontColor = EventReceiver.COLOR.WHITE
		if(remainLines in 1..14) fontColor = EventReceiver.COLOR.YELLOW
		if(remainLines in 1..8) fontColor = EventReceiver.COLOR.ORANGE
		if(remainLines in 1..4) fontColor = EventReceiver.COLOR.RED

		val enemyRemainLines = maxOf(0, getRemainGarbageLines(owner.engine[enemyID], enemyID))
		/* int fontColorEnemy = EventReceiver.COLOR.WHITE;
 * if((enemyRemainLines <= 14) && (enemyRemainLines > 0)) fontColorEnemy
 * = EventReceiver.COLOR.YELLOW;
 * if((enemyRemainLines <= 8) && (enemyRemainLines > 0)) fontColorEnemy
 * = EventReceiver.COLOR.ORANGE;
 * if((enemyRemainLines <= 4) && (enemyRemainLines > 0)) fontColorEnemy
 * = EventReceiver.COLOR.RED; */

		// Lines left (bottom)
		val strLines = "$remainLines"

		if(remainLines>0)
			when(strLines.length) {
				1 -> receiver.drawMenuFont(engine, playerID, 4, 21, strLines, fontColor, 2f)
				2 -> receiver.drawMenuFont(engine, playerID, 3, 21, strLines, fontColor, 2f)
				3 -> receiver.drawMenuFont(engine, playerID, 2, 21, strLines, fontColor, 2f)
			}

		// 1st/2nd
		if(remainLines<enemyRemainLines)
			receiver.drawMenuFont(engine, playerID, -2, 22, "1ST", EventReceiver.COLOR.ORANGE)
		else if(remainLines>enemyRemainLines) receiver.drawMenuFont(engine, playerID, -2, 22, "2ND", EventReceiver.COLOR.WHITE)

		// Timer
		if(playerID==0) receiver.drawDirectFont(256, 16, GeneralUtil.getTime(engine.statistics.time))

		// Normal layout
		if(owner.receiver.nextDisplayType!=2&&playerID==0) {
			receiver.drawScoreFont(engine, playerID, 0, 2, "1P LINES", EventReceiver.COLOR.RED)
			receiver.drawScoreFont(engine, playerID, 0, 3, owner.engine[0].statistics.lines.toString())

			receiver.drawScoreFont(engine, playerID, 0, 5, "2P LINES", EventReceiver.COLOR.BLUE)
			receiver.drawScoreFont(engine, playerID, 0, 6, owner.engine[1].statistics.lines.toString())

			if(!owner.replayMode) {
				receiver.drawScoreFont(engine, playerID, 0, 8, "1P WINS", EventReceiver.COLOR.RED)
				receiver.drawScoreFont(engine, playerID, 0, 9, "${winCount[0]}")

				receiver.drawScoreFont(engine, playerID, 0, 11, "2P WINS", EventReceiver.COLOR.BLUE)
				receiver.drawScoreFont(engine, playerID, 0, 12, "${winCount[1]}")
			}
		}

		// Big-side-next layout
		if(owner.receiver.nextDisplayType==2) {
			val fontColor2 = if(playerID==0) EventReceiver.COLOR.RED else EventReceiver.COLOR.BLUE

			if(!owner.replayMode) {
				receiver.drawDirectFont(x-44, y+190, "WINS", fontColor2, .5f)
				receiver.drawDirectFont(x-if(winCount[playerID]>=10) 44 else 36, y+204,
					"${winCount[playerID]}")
			}
		}
	}

	/* Calculate score */
	override fun calcScore(engine:GameEngine, playerID:Int, lines:Int):Int {
		var enemyID = 0
		if(playerID==0) enemyID = 1

		// Update meter
		val remainLines = getRemainGarbageLines(engine, playerID)
		engine.meterValue = remainLines*receiver.getBlockSize(engine)
		if(remainLines<=14) engine.meterColor = GameEngine.METER_COLOR_YELLOW
		if(remainLines<=8) engine.meterColor = GameEngine.METER_COLOR_ORANGE
		if(remainLines<=4) engine.meterColor = GameEngine.METER_COLOR_RED

		// Game completed
		if(lines>0&&remainLines<=0) {
			engine.timerActive = false
			owner.engine[enemyID].stat = GameEngine.Status.GAMEOVER
			owner.engine[enemyID].resetStatc()
		}
		return 0
	}

	override fun onLast(engine:GameEngine, playerID:Int) {
		// Game End
		if(playerID==1&&owner.engine[0].gameActive)
			if(owner.engine[0].stat==GameEngine.Status.GAMEOVER&&owner.engine[1].stat==GameEngine.Status.GAMEOVER) {
				// Draw
				winnerID = -1
				owner.engine[0].gameEnded()
				owner.engine[1].gameEnded()
				owner.bgmStatus.bgm = BGM.SILENT
			} else if(owner.engine[0].stat!=GameEngine.Status.GAMEOVER&&owner.engine[1].stat==GameEngine.Status.GAMEOVER) {
				// 1P win
				winnerID = 0
				owner.engine[0].gameEnded()
				owner.engine[1].gameEnded()
				owner.engine[0].stat = GameEngine.Status.EXCELLENT
				owner.engine[0].resetStatc()
				owner.engine[0].statc[1] = 1
				owner.bgmStatus.bgm = BGM.SILENT
				if(!owner.replayMode) winCount[0]++
			} else if(owner.engine[0].stat==GameEngine.Status.GAMEOVER&&owner.engine[1].stat!=GameEngine.Status.GAMEOVER) {
				// 2P win
				winnerID = 1
				owner.engine[0].gameEnded()
				owner.engine[1].gameEnded()
				owner.engine[1].stat = GameEngine.Status.EXCELLENT
				owner.engine[1].resetStatc()
				owner.engine[1].statc[1] = 1
				owner.bgmStatus.bgm = BGM.SILENT
				if(!owner.replayMode) winCount[1]++
			}
	}

	/* Render results screen */
	override fun renderResult(engine:GameEngine, playerID:Int) {
		receiver.drawMenuFont(engine, playerID, 0, 0, "RESULT", EventReceiver.COLOR.ORANGE)
		when(winnerID) {
			-1 -> receiver.drawMenuFont(engine, playerID, 6, 1, "DRAW", EventReceiver.COLOR.GREEN)
			playerID -> receiver.drawMenuFont(engine, playerID, 6, 1, "WIN!", EventReceiver.COLOR.YELLOW)
			else -> receiver.drawMenuFont(engine, playerID, 6, 1, "LOSE", EventReceiver.COLOR.WHITE)
		}
		drawResultStats(engine, playerID, receiver, 2, EventReceiver.COLOR.ORANGE, Statistic.LINES, Statistic.PIECE, Statistic.LPM, Statistic.PPS, Statistic.TIME)
	}

	/* Called when saving replay */
	override fun saveReplay(engine:GameEngine, playerID:Int, prop:CustomProperties) {
		saveOtherSetting(engine, owner.replayProp)
		savePreset(engine, owner.replayProp, -1-playerID)
		owner.replayProp.setProperty("vsdigrace.version", version)
	}

	companion object {
		/** Current version */
		private const val CURRENT_VERSION = 0

		/** Number of players */
		private const val MAX_PLAYERS = 2
	}
}
