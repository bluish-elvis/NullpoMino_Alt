/*
 Copyright (c) 2010-2024, NullNoname
 All rights reserved.

 Converted to Kotlin and modified by Venom_Nhelv as bluish-elvis
 THIS IS NOT MADE IN ASSOCIATION WITH THE GAME CREATOR.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions are met:
     * Redistributions of source code must retain the above copyright
       notice, this list of conditions and the following disclaimer.
     * Redistributions in binary form must reproduce the above copyright
       notice, this list of conditions and the following disclaimer in the
       documentation and/or other materials provided with the distribution.
     * Neither the name of NullNoname nor the names of its
       contributors may be used to endorse or promote products derived from
       this software without specific prior written permission.

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
package mu.nu.nullpo.game.subsystem.mode.another

import mu.nu.nullpo.game.component.Block
import mu.nu.nullpo.game.component.Block.COLOR.*
import mu.nu.nullpo.game.component.Controller
import mu.nu.nullpo.game.event.EventReceiver
import mu.nu.nullpo.game.event.ScoreEvent
import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.game.play.GameEngine.GameStyle
import mu.nu.nullpo.game.play.LineGravity
import mu.nu.nullpo.game.play.clearRule.ColorStraight
import mu.nu.nullpo.game.subsystem.mode.AbstractMode
import mu.nu.nullpo.gui.common.BaseFont.FONT.BASE
import mu.nu.nullpo.util.CustomProperties
import mu.nu.nullpo.util.GeneralUtil.toTimeStr

/** PHYSICIAN mode (beta) */
class Physician:AbstractMode() {
	/** GameManager object (Manages entire game status) */

	/** EventReceiver object (This receives many game events, can also be used
	 * for drawing the fonts.) */

	/** Version number */
	private var version = 0

	/** Current round's ranking position */
	private var rankingRank = 0

	/** Rankings' line counts */
	private val rankingScore = MutableList(rankingMax) {0L}

	/** Rankings' times */
	private val rankingTime = MutableList(rankingMax) {-1}
	override val propRank
		get() = rankMapOf("score" to rankingScore, "time" to rankingTime)
	/** Number of initial gem blocks */
	private var hoverBlocks = 0

	/** Speed mode */
	private var speed = 0

	/** Number gem blocks cleared in current chain */
	private var gemsClearedChainTotal = 0

	/* Mode name */
	override val name = "PHYSICIAN (RC1)"

	/* Game style */
	override val gameStyle = GameStyle.PHYSICIAN

	/* Initialization */
	override fun playerInit(engine:GameEngine) {
		super.playerInit(engine)
		lastScore = 0
		gemsClearedChainTotal = 0

		rankingRank = -1
		rankingScore.fill(0L)
		rankingTime.fill(-1)
		if(!owner.replayMode) {
			loadSetting(engine, owner.modeConfig)

			version = CURRENT_VERSION
		} else
			loadSetting(engine, owner.replayProp)

		engine.frameSkin = GameEngine.FRAME_COLOR_PURPLE
		engine.clearMode = ColorStraight(4, false, true)
		engine.garbageColorClear = false
		engine.colorClearSize = 4
		engine.lineGravityType = LineGravity.CASCADE
		engine.nextPieceEnable = PIECE_ENABLE.map {it==1}
		engine.randomBlockColor = true
		engine.blockColors = BLOCK_COLORS
		engine.connectBlocks = true
		engine.cascadeDelay = 18
		engine.gemSameColor = true
	}

	/** Set the gravity rate
	 * @param engine GameEngine
	 */
	override fun setSpeed(engine:GameEngine) {
		engine.speed.gravity = BASE_SPEEDS[speed]*(10+engine.statistics.totalPieceLocked/10)
		engine.speed.denominator = 3600
	}

	/* Called at settings screen */
	override fun onSetting(engine:GameEngine):Boolean {
		// Menu
		if(!engine.owner.replayMode) {
			// Configuration changes
			val change = updateCursor(engine, 1)

			var m = 1
			if(engine.ctrl.isPress(Controller.BUTTON_E)) m = 100
			if(engine.ctrl.isPress(Controller.BUTTON_F)) m = 1000

			if(change!=0) {
				engine.playSE("change")

				when(menuCursor) {
					0 -> {
						hoverBlocks += if(m>=10)
							change*10
						else
							change
						if(hoverBlocks<1) hoverBlocks = 99
						if(hoverBlocks>99) hoverBlocks = 1
					}
					1 -> {
						speed += change
						if(speed<0) speed = 2
						if(speed>2) speed = 0
					}
				}
			}

			// 決定
			if(menuTime<5) menuTime++ else if(engine.ctrl.isPush(Controller.BUTTON_A)) {
				engine.playSE("decide")
				return false
			}

			// Cancel
			if(engine.ctrl.isPush(Controller.BUTTON_B)) engine.quitFlag = true
		} else {
			menuTime++
			menuCursor = -1

			return menuTime<60
		}

		return true
	}

	/* Render the settings screen */
	override fun renderSetting(engine:GameEngine) {
		drawMenu(engine, receiver, 0, EventReceiver.COLOR.BLUE, 0, "GEMS" to hoverBlocks, "SPEED" to SPEED_NAME[speed])
	}

	/* Called for initialization during "Ready" screen */
	override fun startGame(engine:GameEngine) {
		engine.comboType = GameEngine.COMBO_TYPE_DISABLE

		engine.speed.are = 30
		engine.speed.areLine = 30
		engine.speed.das = 10
		engine.speed.lockDelay = 30

		setSpeed(engine)
	}

	override fun renderLast(engine:GameEngine) {
		receiver.drawScore(engine, 0, 0, name, BASE, EventReceiver.COLOR.COBALT)

		if(engine.stat==GameEngine.Status.SETTING||engine.stat==GameEngine.Status.RESULT&&!owner.replayMode) {
			if(!owner.replayMode&&engine.ai==null) {
				receiver.drawScore(engine, 3, 3, "SCORE  TIME", BASE, EventReceiver.COLOR.BLUE)
				for(i in 0..<rankingMax) {
					receiver.drawScore(engine, 0, 4+i, "%2d".format(i+1), BASE, EventReceiver.COLOR.YELLOW)
					receiver.drawScore(engine, 3, 4+i, "${rankingScore[i]}", BASE, i==rankingRank)
					receiver.drawScore(engine, 10, 4+i, rankingTime[i].toTimeStr, BASE, i==rankingRank)
				}
			}
		} else {
			receiver.drawScore(engine, 0, 3, "Score", BASE, EventReceiver.COLOR.BLUE)
			receiver.drawScore(engine, 6, 3, "(+$lastScore)", BASE)
			receiver.drawScore(engine, 0, 4, "$scDisp", BASE)

			receiver.drawScore(engine, 0, 6, "Target", BASE, EventReceiver.COLOR.BLUE)
			receiver.drawScore(engine, 0, 7, engine.field.howManyGems.toString(), BASE)

			var red = 0
			var yellow = 0
			var blue = 0
			for(y in 0..<engine.field.height)
				for(x in 0..<engine.field.width) {
					engine.field.getBlock(x, y)?.run {
						if(type==Block.TYPE.GEM) when(color) {
							BLUE -> blue++
							RED -> red++
							YELLOW -> yellow++
							else -> null
						}
					}
				}
			receiver.drawScore(engine, 0, 8, "(", BASE)
			receiver.drawScore(engine, 1, 8, "%2d".format(red), BASE, EventReceiver.COLOR.RED)
			receiver.drawScore(engine, 4, 8, "%2d".format(yellow), BASE, EventReceiver.COLOR.YELLOW)
			receiver.drawScore(engine, 7, 8, "%2d".format(blue), BASE, EventReceiver.COLOR.BLUE)
			receiver.drawScore(engine, 9, 8, ")", BASE)

			receiver.drawScore(engine, 0, 10, "SPEED", BASE, EventReceiver.COLOR.BLUE)
			receiver.drawScore(engine, 0, 11, SPEED_NAME[speed], BASE, SPEED_COLOR[speed])

			receiver.drawScore(engine, 0, 13, "Time", BASE, EventReceiver.COLOR.BLUE)
			receiver.drawScore(engine, 0, 14, engine.statistics.time.toTimeStr, BASE)
		}
	}

	/* ReadyScreen processing */
	override fun onReady(engine:GameEngine):Boolean {
		if(hoverBlocks>0&&engine.statc[0]==0) {
			engine.createFieldIfNeeded()
			var minY = 6
			when {
				hoverBlocks>=80 -> minY = 3
				hoverBlocks>=72 -> minY = 4
				hoverBlocks>=64 -> minY = 5
			}

			engine.field.addRandomHoverBlocks(engine, hoverBlocks, HOVER_BLOCK_COLORS, minY, true)
			engine.blockOutlineType = GameEngine.BLOCK_OUTLINE_CONNECT
		}
		return false
	}

	/* Called after every frame */
	override fun onLast(engine:GameEngine) {
		super.onLast(engine)

//		if(engine.field==null) return

		val rest = engine.field.howManyGems
		engine.meterValue = rest*1f/hoverBlocks
		engine.meterColor = when {
			rest<=3 -> GameEngine.METER_COLOR_GREEN
			rest<hoverBlocks shr 2 -> GameEngine.METER_COLOR_YELLOW
			rest<hoverBlocks shr 1 -> GameEngine.METER_COLOR_ORANGE
			else -> GameEngine.METER_COLOR_RED
		}

		if(rest==0&&engine.timerActive) {
			engine.gameEnded()
			engine.timerActive = false
			engine.resetStatc()
			engine.stat = GameEngine.Status.EXCELLENT
		}
	}

	/* Calculate score */
	override fun calcScore(engine:GameEngine, ev:ScoreEvent):Int {
		var gemsCleared = engine.field.gemsCleared
		val blkc = ev.lines
		if(gemsCleared>0&&blkc>0) {
			var pts = 0
			while(gemsCleared>0&&gemsClearedChainTotal<5) {
				pts += 1 shl gemsClearedChainTotal
				gemsClearedChainTotal++
				gemsCleared--
			}
			if(gemsClearedChainTotal>=5) pts += gemsCleared shl 5
			pts *= (speed+1)*100
			gemsClearedChainTotal += gemsCleared
			lastScore = pts
			engine.statistics.scoreLine += pts
			engine.playSE("gem")
			setSpeed(engine)
			return pts
		}
		return 0
	}

	override fun lineClearEnd(engine:GameEngine):Boolean {
		gemsClearedChainTotal = 0
		return false
	}

	/* Render results screen */
	override fun renderResult(engine:GameEngine) {
		receiver.drawMenu(engine, 0, 1, "PLAY DATA", BASE, EventReceiver.COLOR.ORANGE)

		drawResult(
			engine, receiver, 3, EventReceiver.COLOR.BLUE, "Score", "%10d".format(engine.statistics.score),
			"CLEARED", "%10d".format(engine.statistics.lines), "Time",
			"%10s".format(engine.statistics.time.toTimeStr)
		)
		drawResultRank(engine, receiver, 9, EventReceiver.COLOR.BLUE, rankingRank)
	}

	/* Called when saving replay */
	override fun saveReplay(engine:GameEngine, prop:CustomProperties):Boolean {
		saveSetting(engine, prop)

		// Update rankings
		if(!owner.replayMode&&engine.ai==null) {
			updateRanking(engine.statistics.score, engine.statistics.time)

			if(rankingRank!=-1) return true
		}
		return false
	}

	override fun loadSetting(engine:GameEngine, prop:CustomProperties, ruleName:String, playerID:Int) {
		hoverBlocks = prop.getProperty("physician.hoverBlocks", 40)
		speed = prop.getProperty("physician.speed", 1)
		version = prop.getProperty("physician.version", 0)
	}

	override fun saveSetting(engine:GameEngine, prop:CustomProperties, ruleName:String, playerID:Int) {
		prop.setProperty("physician.hoverBlocks", hoverBlocks)
		prop.setProperty("physician.speed", speed)
		prop.setProperty("physician.version", version)
	}

	/** Update rankings
	 * @param sc Score
	 * @param time Time
	 */
	private fun updateRanking(sc:Long, time:Int) {
		rankingRank = checkRanking(sc, time)

		if(rankingRank!=-1) {
			// Shift down ranking entries
			for(i in rankingMax-1 downTo rankingRank+1) {
				rankingScore[i] = rankingScore[i-1]
				rankingTime[i] = rankingTime[i-1]
			}

			// Add new data
			rankingScore[rankingRank] = sc
			rankingTime[rankingRank] = time
		}
	}

	/** Calculate ranking position
	 * @param sc Score
	 * @param time Time
	 * @return Position (-1 if unranked)
	 */
	private fun checkRanking(sc:Long, time:Int):Int {
		for(i in 0..<rankingMax)
			if(sc>rankingScore[i])
				return i
			else if(sc==rankingScore[i]&&time<rankingTime[i]) return i

		return -1
	}

	companion object {
		/** Current version */
		private const val CURRENT_VERSION = 0

		/** Enabled piece types */
		private val PIECE_ENABLE = listOf(0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0)

		/** Block colors */
		private val BLOCK_COLORS = listOf(RED, BLUE, YELLOW)
		//.map {it to Block.TYPE.BLOCK}

		private val TAB_BLOCK_COLORS = BLOCK_COLORS.map {it to Block.TYPE.BLOCK}
		/** Hovering block colors */
		private val HOVER_BLOCK_COLORS = BLOCK_COLORS.map {it to Block.TYPE.GEM}
		private val BASE_SPEEDS = listOf(10, 20, 25)

		/** Names of speed settings */
		private val SPEED_NAME = listOf("LOW", "MED", "HI")

		/** Colors for speed settings */
		private val SPEED_COLOR = listOf(EventReceiver.COLOR.BLUE, EventReceiver.COLOR.YELLOW, EventReceiver.COLOR.RED)
	}
}
