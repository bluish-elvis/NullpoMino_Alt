/* Copyright (c) 2010, NullNoname
 * All rights reserved.
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * Neither the name of NullNoname nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
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
 * POSSIBILITY OF SUCH DAMAGE. */
package mu.nu.nullpo.game.subsystem.mode.another

import mu.nu.nullpo.game.component.*
import mu.nu.nullpo.game.event.EventReceiver
import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.game.subsystem.mode.AbstractMode
import mu.nu.nullpo.util.CustomProperties
import mu.nu.nullpo.util.GeneralUtil

/** PHYSICIAN mode (beta) */
class Physician:AbstractMode() {

	/** GameManager object (Manages entire game status) */

	/** EventReceiver object (This receives many game events, can also be used
	 * for drawing the fonts.) */

	/** Amount of points you just get from line clears */
	private var lastscore:Int = 0

	/** Elapsed time from last line clear (lastscore is displayed to screen
	 * until this reaches to 120) */
	private var scgettime:Int = 0

	/** Version number */
	private var version:Int = 0

	/** Current round's ranking rank */
	private var rankingRank:Int = 0

	/** Rankings' line counts */
	private var rankingScore:IntArray = IntArray(RANKING_MAX)

	/** Rankings' times */
	private var rankingTime:IntArray = IntArray(RANKING_MAX)

	/** Number of initial gem blocks */
	private var hoverBlocks:Int = 0

	/** Speed mode */
	private var speed:Int = 0

	/** Number gem blocks cleared in current chain */
	private var gemsClearedChainTotal:Int = 0

	/* Mode name */
	override val name:String
		get() = "PHYSICIAN (RC1)"

	/* Game style */
	override val gameStyle:Int
		get() = GameEngine.GAMESTYLE_PHYSICIAN

	/* Initialization */
	override fun playerInit(engine:GameEngine, playerID:Int) {
		super.playerInit(engine, playerID)
		lastscore = 0
		scgettime = 0
		gemsClearedChainTotal = 0

		rankingRank = -1
		rankingScore = IntArray(RANKING_MAX)
		rankingTime = IntArray(RANKING_MAX)

		if(!owner.replayMode) {
			loadSetting(owner.modeConfig)
			loadRanking(owner.modeConfig, engine.ruleopt.strRuleName)
			version = CURRENT_VERSION
		} else
			loadSetting(owner.replayProp)

		engine.framecolor = GameEngine.FRAME_COLOR_PURPLE
		engine.clearMode = GameEngine.ClearType.LINE_COLOR
		engine.garbageColorClear = false
		engine.colorClearSize = 4
		engine.lineGravityType = GameEngine.LineGravity.CASCADE
		for(i in 0 until Piece.PIECE_COUNT)
			engine.nextPieceEnable[i] = PIECE_ENABLE[i]==1
		engine.randomBlockColor = true
		engine.blockColors = BLOCK_COLORS
		engine.connectBlocks = true
		engine.cascadeDelay = 18
		engine.gemSameColor = true
	}

	/** Set the gravity rate
	 * @param engine GameEngine
	 */
	fun setSpeed(engine:GameEngine) {
		engine.speed.gravity = BASE_SPEEDS[speed]*(10+engine.statistics.totalPieceLocked/10)
		engine.speed.denominator = 3600
	}

	/* Called at settings screen */
	override fun onSetting(engine:GameEngine, playerID:Int):Boolean {
		// Menu
		if(!engine.owner.replayMode) {
			// Configuration changes
			val change = updateCursor(engine, 1)

			var m = 1
			if(engine.ctrl!!.isPress(Controller.BUTTON_E)) m = 100
			if(engine.ctrl!!.isPress(Controller.BUTTON_F)) m = 1000

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
			if(engine.ctrl!!.isPush(Controller.BUTTON_A)&&menuTime>=5) {
				engine.playSE("decide")
				saveSetting(owner.modeConfig)
				receiver.saveModeConfig(owner.modeConfig)
				return false
			}

			// Cancel
			if(engine.ctrl!!.isPush(Controller.BUTTON_B)) engine.quitflag = true

			menuTime++
		} else {
			menuTime++
			menuCursor = -1

			return menuTime<60
		}

		return true
	}

	/* Render the settings screen */
	override fun renderSetting(engine:GameEngine, playerID:Int) {
		drawMenu(engine, playerID, receiver, 0, EventReceiver.COLOR.BLUE, 0, "GEMS", "$hoverBlocks", "SPEED", SPEED_NAME[speed])
	}

	/* Called for initialization during "Ready" screen */
	override fun startGame(engine:GameEngine, playerID:Int) {
		engine.comboType = GameEngine.COMBO_TYPE_DISABLE

		engine.speed.are = 30
		engine.speed.areLine = 30
		engine.speed.das = 10
		engine.speed.lockDelay = 30

		setSpeed(engine)
	}

	override fun renderLast(engine:GameEngine, playerID:Int) {
		receiver.drawScoreFont(engine, playerID, 0, 0, "PHYSICIAN", EventReceiver.COLOR.COBALT)

		if(engine.stat==GameEngine.Status.SETTING||engine.stat==GameEngine.Status.RESULT&&!owner.replayMode) {
			if(!owner.replayMode&&engine.ai==null) {
				receiver.drawScoreFont(engine, playerID, 3, 3, "SCORE  TIME", EventReceiver.COLOR.BLUE)
				for(i in 0 until RANKING_MAX) {
					receiver.drawScoreFont(engine, playerID, 0, 4+i, String.format("%2d", i+1), EventReceiver.COLOR.YELLOW)
					receiver.drawScoreFont(engine, playerID, 3, 4+i, "${rankingScore[i]}", i==rankingRank)
					receiver.drawScoreFont(engine, playerID, 10, 4+i, GeneralUtil.getTime(rankingTime[i]), i==rankingRank)
				}
			}
		} else {
			receiver.drawScoreFont(engine, playerID, 0, 3, "SCORE", EventReceiver.COLOR.BLUE)
			val strScore:String = if(lastscore==0||scgettime<=0)
				"${engine.statistics.score}"
			else
				"${engine.statistics.score}(+$lastscore)"
			receiver.drawScoreFont(engine, playerID, 0, 4, strScore)

			receiver.drawScoreFont(engine, playerID, 0, 6, "REST", EventReceiver.COLOR.BLUE)
			if(engine.field!=null) {
				receiver.drawScoreFont(engine, playerID, 0, 7, engine.field!!.howManyGems.toString())
				var red = 0
				var yellow = 0
				var blue = 0
				for(y in 0 until engine.field!!.height)
					for(x in 0 until engine.field!!.width) {
						val blockColor = engine.field!!.getBlockColor(x, y)
						if(blockColor==Block.BLOCK_COLOR_GEM_BLUE)
							blue++
						else if(blockColor==Block.BLOCK_COLOR_GEM_RED)
							red++
						else if(blockColor==Block.BLOCK_COLOR_GEM_YELLOW) yellow++
					}
				receiver.drawScoreFont(engine, playerID, 0, 8, "(")
				receiver.drawScoreFont(engine, playerID, 1, 8, String.format("%2d", red), EventReceiver.COLOR.RED)
				receiver.drawScoreFont(engine, playerID, 4, 8, String.format("%2d", yellow), EventReceiver.COLOR.YELLOW)
				receiver.drawScoreFont(engine, playerID, 7, 8, String.format("%2d", blue), EventReceiver.COLOR.BLUE)
				receiver.drawScoreFont(engine, playerID, 9, 8, ")")
			}

			receiver.drawScoreFont(engine, playerID, 0, 10, "SPEED", EventReceiver.COLOR.BLUE)
			receiver.drawScoreFont(engine, playerID, 0, 11, SPEED_NAME[speed], SPEED_COLOR[speed])

			receiver.drawScoreFont(engine, playerID, 0, 13, "TIME", EventReceiver.COLOR.BLUE)
			receiver.drawScoreFont(engine, playerID, 0, 14, GeneralUtil.getTime(engine.statistics.time))
		}
	}

	/* ReadyScreen processing */
	override fun onReady(engine:GameEngine, playerID:Int):Boolean {
		if(hoverBlocks>0&&engine.statc[0]==0) {
			engine.createFieldIfNeeded()
			var minY = 6
			if(hoverBlocks>=80)
				minY = 3
			else if(hoverBlocks>=72)
				minY = 4
			else if(hoverBlocks>=64) minY = 5
			engine.field!!.addRandomHoverBlocks(engine, hoverBlocks, HOVER_BLOCK_COLORS, minY, true)
			engine.blockOutlineType = GameEngine.BLOCK_OUTLINE_CONNECT
		}
		return false
	}

	/* Called after every frame */
	override fun onLast(engine:GameEngine, playerID:Int) {
		if(scgettime>0) scgettime--

		if(engine.field==null) return

		val rest = engine.field!!.howManyGems
		engine.meterValue = rest*receiver.getMeterMax(engine)/hoverBlocks
		if(rest<=3)
			engine.meterColor = GameEngine.METER_COLOR_GREEN
		else if(rest<hoverBlocks shr 2)
			engine.meterColor = GameEngine.METER_COLOR_YELLOW
		else if(rest<hoverBlocks shr 1)
			engine.meterColor = GameEngine.METER_COLOR_ORANGE
		else
			engine.meterColor = GameEngine.METER_COLOR_RED

		if(rest==0&&engine.timerActive) {
			engine.gameEnded()
			engine.timerActive = false
			engine.resetStatc()
			engine.stat = GameEngine.Status.EXCELLENT
		}
	}

	/* Calculate score */
	override fun calcScore(engine:GameEngine, playerID:Int, lines:Int) {
		var gemsCleared = engine.field!!.gemsCleared
		if(gemsCleared>0&&lines>0) {
			var pts = 0
			while(gemsCleared>0&&gemsClearedChainTotal<5) {
				pts += 1 shl gemsClearedChainTotal
				gemsClearedChainTotal++
				gemsCleared--
			}
			if(gemsClearedChainTotal>=5) pts += gemsCleared shl 5
			pts *= (speed+1)*100
			gemsClearedChainTotal += gemsCleared
			lastscore = pts
			scgettime = 120
			engine.statistics.scoreFromLineClear += pts
			engine.statistics.score += pts
			engine.playSE("gem")
			setSpeed(engine)
		}
	}

	override fun lineClearEnd(engine:GameEngine, playerID:Int):Boolean {
		gemsClearedChainTotal = 0
		return false
	}

	/* Render results screen */
	override fun renderResult(engine:GameEngine, playerID:Int) {
		receiver.drawMenuFont(engine, playerID, 0, 1, "PLAY DATA", EventReceiver.COLOR.ORANGE)

		drawResult(engine, playerID, receiver, 3, EventReceiver.COLOR.BLUE, "SCORE", String.format("%10d", engine.statistics.score), "CLEARED", String.format("%10d", engine.statistics.lines), "TIME", String.format("%10s", GeneralUtil.getTime(engine.statistics.time)))
		drawResultRank(engine, playerID, receiver, 9, EventReceiver.COLOR.BLUE, rankingRank)
	}

	/* Called when saving replay */
	override fun saveReplay(engine:GameEngine, playerID:Int, prop:CustomProperties) {
		saveSetting(prop)

		// Update rankings
		if(!owner.replayMode&&engine.ai==null) {
			updateRanking(engine.statistics.score, engine.statistics.time)

			if(rankingRank!=-1) {
				saveRanking(owner.modeConfig, engine.ruleopt.strRuleName)
				receiver.saveModeConfig(owner.modeConfig)
			}
		}
	}

	/** Load settings from property file
	 * @param prop Property file
	 */
	override fun loadSetting(prop:CustomProperties) {
		hoverBlocks = prop.getProperty("physician.hoverBlocks", 40)
		speed = prop.getProperty("physician.speed", 1)
		version = prop.getProperty("physician.version", 0)
	}

	/** Save settings to property file
	 * @param prop Property file
	 */
	override fun saveSetting(prop:CustomProperties) {
		prop.setProperty("physician.hoverBlocks", hoverBlocks)
		prop.setProperty("physician.speed", speed)
		prop.setProperty("physician.version", version)
	}

	/** Read rankings from property file
	 * @param prop Property file
	 * @param ruleName Rule name
	 */
	private fun loadRanking(prop:CustomProperties?, ruleName:String) {
		for(i in 0 until RANKING_MAX) {
			rankingScore[i] = prop!!.getProperty("physician.ranking.$ruleName.score.$i", 0)
			rankingTime[i] = prop.getProperty("physician.ranking.$ruleName.time.$i", -1)
		}
	}

	/** Save rankings to property file
	 * @param prop Property file
	 * @param ruleName Rule name
	 */
	private fun saveRanking(prop:CustomProperties?, ruleName:String) {
		for(i in 0 until RANKING_MAX) {
			prop!!.setProperty("physician.ranking.$ruleName.score.$i", rankingScore[i])
			prop.setProperty("physician.ranking.$ruleName.time.$i", rankingTime[i])
		}
	}

	/** Update rankings
	 * @param sc Score
	 * @param time Time
	 */
	private fun updateRanking(sc:Int, time:Int) {
		rankingRank = checkRanking(sc, time)

		if(rankingRank!=-1) {
			// Shift down ranking entries
			for(i in RANKING_MAX-1 downTo rankingRank+1) {
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
	private fun checkRanking(sc:Int, time:Int):Int {
		for(i in 0 until RANKING_MAX)
			if(sc>rankingScore[i])
				return i
			else if(sc==rankingScore[i]&&time<rankingTime[i]) return i

		return -1
	}

	companion object {
		/** Current version */
		private const val CURRENT_VERSION = 0

		/** Enabled piece types */
		private val PIECE_ENABLE = intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0)

		/** Block colors */
		private val BLOCK_COLORS = intArrayOf(Block.BLOCK_COLOR_RED, Block.BLOCK_COLOR_BLUE, Block.BLOCK_COLOR_YELLOW)
		/** Hovering block colors */
		private val HOVER_BLOCK_COLORS = intArrayOf(Block.BLOCK_COLOR_GEM_RED, Block.BLOCK_COLOR_GEM_BLUE, Block.BLOCK_COLOR_GEM_YELLOW)
		private val BASE_SPEEDS = intArrayOf(10, 20, 25)

		/** Number of ranking records */
		private const val RANKING_MAX = 10

		/** Names of speed settings */
		private val SPEED_NAME = arrayOf("LOW", "MED", "HI")

		/** Colors for speed settings */
		private val SPEED_COLOR = arrayOf(EventReceiver.COLOR.BLUE, EventReceiver.COLOR.YELLOW, EventReceiver.COLOR.RED)
	}
}
