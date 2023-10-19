/*
 * Copyright (c) 2010-2022, NullNoname
 * Kotlin converted and modified by Venom=Nhelv.
 * THIS WAS NOT MADE IN ASSOCIATION WITH THE GAME CREATOR.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of NullNoname nor the names of its
 *       contributors may be used to endorse or promote products derived from
 *       this software without specific prior written permission.
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

package mu.nu.nullpo.game.subsystem.mode

import mu.nu.nullpo.game.component.BGMStatus.BGM
import mu.nu.nullpo.game.component.Block
import mu.nu.nullpo.game.component.Block.COLOR
import mu.nu.nullpo.game.component.Controller
import mu.nu.nullpo.game.event.EventReceiver
import mu.nu.nullpo.game.event.ScoreEvent
import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.game.play.GameManager
import mu.nu.nullpo.gui.common.GameKeyDummy.Companion.MAX_PLAYERS
import mu.nu.nullpo.util.CustomProperties
import mu.nu.nullpo.util.GeneralUtil.toTimeStr
import kotlin.random.Random

/** VS-DIG RACE mode */
class VSSprintDig:AbstractMode() {
	/** Number of garbage lines to clear */
	private var goalLines = IntArray(0)

	/** Rate of garbage holes change */
	private var messiness = IntArray(0)

	/** BGM number */
	private var bgmId = 0

	/** Sound effects ON/OFF */
	private var enableSE = BooleanArray(0)

	/** Last preset number used */
	private var presetNumber = IntArray(0)

	/** Winner player ID */
	private var winnerID = 0

	/** Win count for each player */
	private var winCount = IntArray(0)

	/** Version */
	private var version = 0

	/* Mode name */
	override val name = "VS-DIG RACE"

	override val isVSMode:Boolean
		get() = true

	/* Number of players */
	override val players:Int
		get() = MAX_PLAYERS

	/* Mode init */
	override fun modeInit(manager:GameManager) {
		super.modeInit(manager)
		goalLines = IntArray(MAX_PLAYERS)
		messiness = IntArray(MAX_PLAYERS)
		bgmId = 0
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

	/** Load settings into [engine] from [prop] not related to speeds */
	private fun loadOtherSetting(engine:GameEngine, prop:CustomProperties) {
		val playerID = engine.playerID
		goalLines[playerID] = prop.getProperty("vsdigrace.goalLines.p$playerID", 18)
		messiness[playerID] = prop.getProperty("vsdigrace.garbagePercent.p$playerID", 100)
		bgmId = prop.getProperty("vsdigrace.bgmno", 0)
		enableSE[playerID] = prop.getProperty("vsdigrace.enableSE.p$playerID", true)
		presetNumber[playerID] = prop.getProperty("vsdigrace.presetNumber.p$playerID", 0)
	}

	/** Save settings from [engine] into [prop] not related to speeds */
	private fun saveOtherSetting(engine:GameEngine, prop:CustomProperties) {
		val playerID = engine.playerID
		prop.setProperty("vsdigrace.goalLines.p$playerID", goalLines[playerID])
		prop.setProperty("vsdigrace.garbagePercent.p$playerID", messiness[playerID])
		prop.setProperty("vsdigrace.bgmno", bgmId)
		prop.setProperty("vsdigrace.enableSE.p$playerID", enableSE[playerID])
		prop.setProperty("vsdigrace.presetNumber.p$playerID", presetNumber[playerID])
	}

	/* Initialization for each player */
	override fun playerInit(engine:GameEngine) {
		val pid = engine.playerID
		if(pid==1) {
			engine.randSeed = owner.engine[0].randSeed
			engine.random = Random(owner.engine[0].randSeed)
		}

		engine.frameColor = PLAYER_COLOR_FRAME[pid]

		if(!engine.owner.replayMode) {
			version = CURRENT_VERSION
			loadOtherSetting(engine, engine.owner.modeConfig)
			loadPreset(engine, engine.owner.modeConfig, -1-pid)
		} else {
			version = owner.replayProp.getProperty("vsbattle.version", 0)
			loadOtherSetting(engine, engine.owner.replayProp)
			loadPreset(engine, engine.owner.replayProp, -1-pid)
		}
	}

	/* Called at settings screen */
	override fun onSetting(engine:GameEngine):Boolean {
		// Menu
		val pid = engine.playerID
		if(!engine.owner.replayMode&&engine.statc[4]==0) {
			// Configuration changes
			val change = updateCursor(engine, 12)

			if(change!=0) {
				engine.playSE("change")

				var m = 1
				if(engine.ctrl.isPress(Controller.BUTTON_E)) m = 100
				if(engine.ctrl.isPress(Controller.BUTTON_F)) m = 1000

				when(menuCursor) {
					0 -> engine.speed.gravity = rangeCursor(engine.speed.gravity+change*m, -1, 99999)
					1 -> engine.speed.denominator = rangeCursor(change*m, -1, 99999)
					2 -> engine.speed.are = rangeCursor(engine.speed.are+change, 0, 99)
					3 -> engine.speed.areLine = rangeCursor(engine.speed.areLine+change, 0, 99)
					4 -> engine.speed.lineDelay = rangeCursor(engine.speed.lineDelay+change, 0, 99)
					5 -> engine.speed.lockDelay = rangeCursor(engine.speed.lockDelay+change, 0, 99)
					6 -> engine.speed.das = rangeCursor(engine.speed.das+change, 0, 99)
					7, 8 -> presetNumber[pid] = rangeCursor(presetNumber[pid]+change, 0, 99)
					9 -> {
						goalLines[pid] += change
						if(goalLines[pid]<1) goalLines[pid] = 18
						if(goalLines[pid]>18) goalLines[pid] = 1
					}
					10 -> {
						messiness[pid] += change
						if(messiness[pid]<0) messiness[pid] = 100
						if(messiness[pid]>100) messiness[pid] = 0
					}
					11 -> enableSE[pid] = !enableSE[pid]
					12 -> bgmId = rangeCursor(bgmId+change, 0, BGM.count-1)
				}
			}

			// Confirm
			if(menuTime<5) menuTime++ else if(engine.ctrl.isPush(Controller.BUTTON_A)) {
				engine.playSE("decide")

				when(menuCursor) {
					7 -> loadPreset(engine, owner.modeConfig, presetNumber[pid])
					8 -> {
						savePreset(engine, owner.modeConfig, presetNumber[pid])
						owner.saveModeConfig()
					}
					else -> {
						saveOtherSetting(engine, owner.modeConfig)
						savePreset(engine, owner.modeConfig, -1-pid)
						owner.saveModeConfig()
						engine.statc[4] = 1
					}
				}
			}

			// Cancel
			if(engine.ctrl.isPush(Controller.BUTTON_B)) engine.quitFlag = true
		} else if(engine.statc[4]==0) {
			// Replay start
			menuTime++
			menuCursor = 0

			if(menuTime>=60) menuCursor = 9
			if(menuTime>=120) engine.statc[4] = 1
		} else // Start the game when both players are ready
			if(owner.engine[0].statc[4]==1&&owner.engine[1].statc[4]==1&&pid==1) {
				owner.engine[0].stat = GameEngine.Status.READY
				owner.engine[1].stat = GameEngine.Status.READY
				owner.engine[0].resetStatc()
				owner.engine[1].resetStatc()
			} else if(engine.ctrl.isPush(Controller.BUTTON_B)) engine.statc[4] = 0// Cancel

		return true
	}

	/* Settings screen */
	override fun renderSetting(engine:GameEngine) {
		if(engine.statc[4]==0) {
			val pid = engine.playerID
			if(menuCursor<9) {
				drawMenuSpeeds(engine, receiver, 0, EventReceiver.COLOR.ORANGE, 0)
				drawMenu(
					engine, receiver, 14, EventReceiver.COLOR.GREEN, 7,
					"LOAD" to presetNumber[pid], "SAVE" to presetNumber[pid]
				)
			} else {
				drawMenu(
					engine, receiver, 0, EventReceiver.COLOR.CYAN, 9,
					"GOAL" to goalLines[pid],
					"CHANGERATE" to "${messiness[pid]}%",
					"SE" to enableSE[pid]
				)
				drawMenu(engine, receiver, 6, EventReceiver.COLOR.PINK, 12, "BGM" to BGM.values[bgmId])
			}
		} else
			receiver.drawMenuFont(engine, 3, 10, "WAIT", EventReceiver.COLOR.YELLOW)
	}

	/* Ready */
	override fun onReady(engine:GameEngine):Boolean {
		if(engine.statc[0]==0) {
			engine.createFieldIfNeeded()
			fillGarbage(engine)

			// Update meter
			val remainLines = getRemainGarbageLines(engine)
			engine.meterValue = remainLines*1f/engine.fieldHeight
			engine.meterColor = GameEngine.METER_COLOR_GREEN
		}
		return false
	}

	/* Called at game start */
	override fun startGame(engine:GameEngine) {
		engine.enableSE = enableSE[engine.playerID]
		if(engine.playerID==1) owner.musMan.bgm = BGM.values[bgmId]

		engine.meterColor = GameEngine.METER_COLOR_GREEN
		engine.meterValue = 1f
	}

	/** Fill the playfield with garbage */
	private fun fillGarbage(engine:GameEngine) {
		val pid = engine.playerID
		val w:Int = engine.field.width
		val h:Int = engine.field.height
		var hole:Int = -1

		for(y:Int in h-1 downTo h-goalLines[pid]) {
			if(hole==-1||engine.random.nextInt(100)<messiness[pid]) {
				var newHole = -1
				do
					newHole = engine.random.nextInt(w)
				while(newHole==hole)
				hole = newHole
			}

			var prevColor:COLOR? = null
			for(x:Int in 0..<w)
				if(x!=hole) {
					var color:COLOR = COLOR.WHITE
					if(y==h-1) {
						do
							color = COLOR.all[1+engine.random.nextInt(7)]
						while(color==prevColor)
						prevColor = color
					}
					engine.field.setBlock(
						x, y, Block(
							color, if(y==h-1) Block.TYPE.BLOCK else Block.TYPE.GEM,
							engine.skin, Block.ATTRIBUTE.VISIBLE, Block.ATTRIBUTE.GARBAGE
						)
					)
				}
			// Set connections
			if(receiver.isStickySkin(engine)&&y!=h-1)
				for(x:Int in 0..<w)
					if(x!=hole) {
						val blk = engine.field.getBlock(x, y)
						if(blk!=null) {
							if(!engine.field.getBlockEmpty(x-1, y)) blk.setAttribute(true, Block.ATTRIBUTE.CONNECT_LEFT)
							if(!engine.field.getBlockEmpty(x+1, y))
								blk.setAttribute(true, Block.ATTRIBUTE.CONNECT_RIGHT)
						}
					}
		}
	}

	private fun getRemainGarbageLines(engine:GameEngine):Int {
		if(engine.field.isEmpty) return -1
		val playerID = engine.playerID
		val w:Int = engine.field.width
		val h:Int = engine.field.height
		var lines = 0
		var hasGemBlock = false

		for(y:Int in h-1 downTo h-goalLines[playerID])
			if(!engine.field.getLineFlag(y))
				for(x:Int in 0..<w) {
					val blk = engine.field.getBlock(x, y)

					if(blk!=null&&blk.isGemBlock) hasGemBlock = true
					if(blk!=null&&blk.getAttribute(Block.ATTRIBUTE.GARBAGE)) {
						lines++
						break
					}
				}

		return if(!hasGemBlock) 0 else lines
	}

	/* Render score */
	override fun renderLast(engine:GameEngine) {
		val pid = engine.playerID
		val enemyID = if(pid==0) 1 else 0

		val x = receiver.fieldX(engine)
		val y = receiver.fieldY(engine)

		val remainLines = maxOf(0, getRemainGarbageLines(engine))
		val fontColor = when (remainLines) {
			in 1..4 -> EventReceiver.COLOR.RED
			in 5..8 -> EventReceiver.COLOR.ORANGE
			in 9..14 -> EventReceiver.COLOR.YELLOW
			else -> EventReceiver.COLOR.WHITE
		}

		val enemyRemainLines = maxOf(0, getRemainGarbageLines(owner.engine[enemyID]))
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
				1 -> receiver.drawMenuFont(engine, 4, 21, strLines, fontColor, 2f)
				2 -> receiver.drawMenuFont(engine, 3, 21, strLines, fontColor, 2f)
				3 -> receiver.drawMenuFont(engine, 2, 21, strLines, fontColor, 2f)
			}

		// 1st/2nd
		if(remainLines<enemyRemainLines)
			receiver.drawMenuFont(engine, -3, 22, "1ST", EventReceiver.COLOR.ORANGE)
		else if(remainLines>enemyRemainLines) receiver.drawMenuFont(engine, -3, 22, "2ND", EventReceiver.COLOR.WHITE)

		// Timer
		if(pid==0) receiver.drawDirectFont(256, 16, engine.statistics.time.toTimeStr)

		// Normal layout
		if(owner.receiver.nextDisplayType!=2&&pid==0) {
			receiver.drawScoreFont(engine, 0, 2, "1P LINES", EventReceiver.COLOR.RED)
			receiver.drawScoreFont(engine, 0, 3, owner.engine[0].statistics.lines.toString())

			receiver.drawScoreFont(engine, 0, 5, "2P LINES", EventReceiver.COLOR.BLUE)
			receiver.drawScoreFont(engine, 0, 6, owner.engine[1].statistics.lines.toString())

			if(!owner.replayMode) {
				receiver.drawScoreFont(engine, 0, 8, "1P WINS", EventReceiver.COLOR.RED)
				receiver.drawScoreFont(engine, 0, 9, "${winCount[0]}")

				receiver.drawScoreFont(engine, 0, 11, "2P WINS", EventReceiver.COLOR.BLUE)
				receiver.drawScoreFont(engine, 0, 12, "${winCount[1]}")
			}
		}

		// Big-side-next layout
		if(owner.receiver.nextDisplayType==2) {
			val fontColor2 = if(pid==0) EventReceiver.COLOR.RED else EventReceiver.COLOR.BLUE

			if(!owner.replayMode) {
				receiver.drawDirectFont(x-44, y+190, "WINS", fontColor2, .5f)
				receiver.drawDirectFont(
					x-if(winCount[pid]>=10) 44 else 36, y+204,
					"${winCount[pid]}"
				)
			}
		}
	}

	/* Calculate score */
	override fun calcScore(engine:GameEngine, ev:ScoreEvent):Int {
		val enemyID = if(engine.playerID==0) 1 else 0

		// Update meter
		val remainLines = getRemainGarbageLines(engine)
		engine.meterValue = remainLines*1f/engine.fieldHeight
		engine.meterColor = GameEngine.METER_COLOR_LEVEL

		// Game completed
		if(ev.lines>0&&remainLines<=0) {
			engine.timerActive = false
			owner.engine[enemyID].stat = GameEngine.Status.GAMEOVER
			owner.engine[enemyID].resetStatc()
		}
		return 0
	}

	override fun onLast(engine:GameEngine) {
		super.onLast(engine)
		// Game End
		if(engine.playerID==1&&owner.engine[0].gameActive)
			if(owner.engine[0].stat==GameEngine.Status.GAMEOVER&&owner.engine[1].stat==GameEngine.Status.GAMEOVER) {
				// Draw
				winnerID = -1
				owner.engine[0].gameEnded()
				owner.engine[1].gameEnded()
				owner.musMan.bgm = BGM.Silent
			} else if(owner.engine[0].stat!=GameEngine.Status.GAMEOVER&&owner.engine[1].stat==GameEngine.Status.GAMEOVER) {
				// 1P win
				winnerID = 0
				owner.engine[0].gameEnded()
				owner.engine[1].gameEnded()
				owner.engine[0].stat = GameEngine.Status.EXCELLENT
				owner.engine[0].resetStatc()
				owner.engine[0].statc[1] = 1
				owner.musMan.bgm = BGM.Silent
				if(!owner.replayMode) winCount[0]++
			} else if(owner.engine[0].stat==GameEngine.Status.GAMEOVER&&owner.engine[1].stat!=GameEngine.Status.GAMEOVER) {
				// 2P win
				winnerID = 1
				owner.engine[0].gameEnded()
				owner.engine[1].gameEnded()
				owner.engine[1].stat = GameEngine.Status.EXCELLENT
				owner.engine[1].resetStatc()
				owner.engine[1].statc[1] = 1
				owner.musMan.bgm = BGM.Silent
				if(!owner.replayMode) winCount[1]++
			}
	}

	/* Render results screen */
	override fun renderResult(engine:GameEngine) {
		receiver.drawMenuFont(engine, 0, 0, "RESULT", EventReceiver.COLOR.ORANGE)
		when(winnerID) {
			-1 -> receiver.drawMenuFont(engine, 6, 1, "DRAW", EventReceiver.COLOR.GREEN)
			engine.playerID -> receiver.drawMenuFont(engine, 6, 1, "WIN!", EventReceiver.COLOR.YELLOW)
			else -> receiver.drawMenuFont(engine, 6, 1, "LOSE", EventReceiver.COLOR.WHITE)
		}
		drawResultStats(
			engine, receiver, 2, EventReceiver.COLOR.ORANGE, Statistic.LINES, Statistic.PIECE, Statistic.LPM, Statistic.PPS,
			Statistic.TIME
		)
	}

	/* Called when saving replay */
	override fun saveReplay(engine:GameEngine, prop:CustomProperties):Boolean {
		saveOtherSetting(engine, owner.replayProp)
		savePreset(engine, owner.replayProp, -1-engine.playerID)
		owner.replayProp.setProperty("vsdigrace.version", version)
		return false
	}

	companion object {
		/** Current version */
		private const val CURRENT_VERSION = 0

		/** Each player's frame cint */
		private val PLAYER_COLOR_FRAME = listOf(GameEngine.FRAME_COLOR_RED, GameEngine.FRAME_COLOR_BLUE)
	}
}
