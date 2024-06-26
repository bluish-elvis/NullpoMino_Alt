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

import mu.nu.nullpo.game.component.Controller
import mu.nu.nullpo.game.event.EventReceiver.COLOR
import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.gui.common.BaseFont
import mu.nu.nullpo.util.CustomProperties
import mu.nu.nullpo.util.GeneralUtil.toTimeStr

/** AVALANCHE mode (Release Candidate 2) */
class Avalanche1P:Avalanche1PDummyMode() {
	/** Selected game type */
	private var gametype = 0

	/** Version number */
	private var version = 0

	/** Current round's ranking position */
	private var rankingRank = 0

	/** Rankings' line counts */
	private val rankingScore = List(SCORETYPE_MAX) {List(3) {List(RANKING_TYPE) {MutableList(rankingMax) {0L}}}}

	/** Rankings' times */
	private val rankingTime = List(SCORETYPE_MAX) {List(3) {List(RANKING_TYPE) {MutableList(rankingMax) {-1}}}}

	/** Chain display enable/disable */
	private var showChains = false

	/** If true, both columns 3 and 4 are danger columns */
	private var dangerColumnDouble = false

	/** If true, red X's appear at tops of danger columns */
	private var dangerColumnShowX = false

	/** True for classic scoring, false for 15th scoring algorithm */
	private var scoreType = 0

	/** Sprint target score */
	private var sprintTarget = 0
	override val propRank
		get() = rankMapOf(rankingScore.flatMapIndexed {a, x ->
			x.flatMapIndexed {b, y -> y.mapIndexed {c, z -> "$a.$b.$c.score" to z}}

		}+rankingTime.flatMapIndexed {a, x ->
			x.flatMapIndexed {b, y -> y.mapIndexed {c, z -> "$a.$b.$c.time" to z}}

		})
	/* Mode name */
	override val name = "AVALANCHE 1P (RC2)"

	/* Initialization */
	override fun playerInit(engine:GameEngine) {
		super.playerInit(engine)

		showChains = true

		scoreType = 0
		sprintTarget = 0

		rankingRank = -1
		rankingScore.forEach {it.forEach {t -> t.forEach {p -> p.fill(0)}}}
		rankingTime.forEach {it.forEach {t -> t.forEach {p -> p.fill(0)}}}

		if(!owner.replayMode) {
			version = CURRENT_VERSION
		}
	}

	/** Set the gravity rate
	 * @param engine GameEngine
	 */
	override fun setSpeed(engine:GameEngine) {
		engine.speed.gravity = 1
		if(gametype==0)
			engine.speed.denominator = maxOf(41-level, 2)
		else
			engine.speed.denominator = 40
	}

	/* Called at settings screen */
	override fun onSetting(engine:GameEngine):Boolean {
		// Menu
		if(!engine.owner.replayMode) {
			// Up
			if(engine.ctrl.isMenuRepeatKey(Controller.BUTTON_UP)) {
				menuCursor--
				if(menuCursor<0)
					menuCursor = 10
				else if(menuCursor==1&&gametype!=2) menuCursor--
				engine.playSE("cursor")
			}
			// Down
			if(engine.ctrl.isMenuRepeatKey(Controller.BUTTON_DOWN)) {
				menuCursor++
				if(menuCursor>10)
					menuCursor = 0
				else if(menuCursor==1&&gametype!=2) menuCursor++
				engine.playSE("cursor")
			}

			// Configuration changes
			var change = 0
			if(engine.ctrl.isMenuRepeatKey(Controller.BUTTON_LEFT)) change = -1
			if(engine.ctrl.isMenuRepeatKey(Controller.BUTTON_RIGHT)) change = 1

			if(change!=0) {
				engine.playSE("change")

				when(menuCursor) {
					0 -> {
						gametype += change
						if(gametype<0) gametype = GAMETYPE_MAX-1
						if(gametype>GAMETYPE_MAX-1) gametype = 0
					}
					1 -> {
						sprintTarget += change
						if(sprintTarget<0) sprintTarget = SPRINT_MAX_SCORE.size-1
						if(sprintTarget>=SPRINT_MAX_SCORE.size) sprintTarget = 0
					}
					2 -> {
						scoreType += change
						if(scoreType<0) scoreType = SCORETYPE_MAX-1
						if(scoreType>=SCORETYPE_MAX) scoreType = 0
					}
					3 -> {
						numColors += change
						if(numColors<3) numColors = 5
						if(numColors>5) numColors = 3
					}
					4 -> dangerColumnDouble = !dangerColumnDouble
					5 -> dangerColumnShowX = !dangerColumnShowX
					6 -> {
						engine.colorClearSize += change
						if(engine.colorClearSize<2) engine.colorClearSize = 36
						if(engine.colorClearSize>36) engine.colorClearSize = 2
					}
					7 -> cascadeSlow = !cascadeSlow
					8 -> bigDisplay = !bigDisplay
					9 -> {
						outlinetype += change
						if(outlinetype<0) outlinetype = 2
						if(outlinetype>2) outlinetype = 0
					}
					10 -> showChains = !showChains
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
			menuCursor = if(menuTime>=60) 9 else -1
			return menuTime<120
		}

		return true
	}

	/* Render the settings screen */
	override fun renderSetting(engine:GameEngine) {
		if(menuCursor<=8) {
			drawMenu(engine, receiver, 0, COLOR.BLUE, 0, "GAME TYPE" to GAMETYPE_NAME[gametype])
			if(gametype==2)
				drawMenu(engine, receiver, 2, COLOR.BLUE, 1, "TARGET" to SPRINT_MAX_SCORE[sprintTarget])
			drawMenu(
				engine, receiver, 4, COLOR.BLUE, 2, "SCORE TYPE" to SCORETYPE_NAME[scoreType], "COLORS" to numColors,
				"X COLUMN" to if(dangerColumnDouble) "3 AND 4" else "3 ONLY", "X SHOW" to dangerColumnShowX,
				"CLEAR SIZE" to engine.colorClearSize, "FALL ANIM" to if(cascadeSlow) "NEW SLOW" else "OLD FAST",
				"BIG DISP" to bigDisplay
			)

			receiver.drawMenuFont(engine, 0, 19, "${BaseFont.UP_S}${BaseFont.DOWN_S} PAGE 1/2", COLOR.YELLOW)
		} else {
			val strOutline = when(outlinetype) {
				1 -> "COLOR"
				2 -> "NONE"
				else -> "NORMAL"
			}
			drawMenu(engine, receiver, 0, COLOR.BLUE, 9, "OUTLINE" to strOutline, "SHOW CHAIN" to showChains)

			receiver.drawMenuFont(engine, 0, 19, "${BaseFont.UP_S}${BaseFont.DOWN_S} PAGE 2/2", COLOR.YELLOW)
		}
	}

	/* When the piece is movable */
	override fun renderMove(engine:GameEngine) {
		if(dangerColumnShowX&&engine.gameStarted) drawX(engine)
	}

	/* Render score */
	override fun renderLast(engine:GameEngine) {
		var modeStr = GAMETYPE_NAME[gametype]
		if(gametype==2) modeStr = "$modeStr ${SPRINT_MAX_SCORE[sprintTarget]/1000}K"
		receiver.drawScoreFont(engine, 0, 0, "AVALANCHE ($modeStr)", COLOR.COBALT)
		receiver.drawScoreFont(engine, 0, 1, "(${SCORETYPE_NAME[scoreType]} $numColors COLORS)", COLOR.COBALT)

		if(engine.stat==GameEngine.Status.SETTING||engine.stat==GameEngine.Status.RESULT&&!owner.replayMode) {
			if(!owner.replayMode&&engine.ai==null&&engine.colorClearSize==4) {
				val scale = if(receiver.nextDisplayType==2&&gametype==0) .5f else 1f
				val topY = if(receiver.nextDisplayType==2&&gametype==0) 6 else 4

				when(gametype) {
					0 -> receiver.drawScoreFont(engine, 3, topY-1, "SCORE      TIME", COLOR.BLUE, scale)
					1 -> receiver.drawScoreFont(engine, 3, 3, "Score", COLOR.BLUE)
					2 -> receiver.drawScoreFont(engine, 3, 3, "Time", COLOR.BLUE)
				}

				for(i in 0..<rankingMax) {
					receiver.drawScoreGrade(engine, 0, topY+i, "%2d".format(i+1), COLOR.YELLOW, scale)
					when(gametype) {
						0 -> {
							receiver.drawScoreFont(
								engine, 3, topY+i, "${rankingScore[scoreType][numColors-3][gametype][i]}", i==rankingRank,
								scale
							)
							receiver.drawScoreFont(
								engine, 14, topY+i, rankingTime[scoreType][numColors-3][gametype][i].toTimeStr,
								i==rankingRank, scale
							)
						}
						1 -> receiver.drawScoreFont(
							engine, 3, 4+i, "${rankingScore[scoreType][numColors-3][gametype][i]}", i==rankingRank
						)
						2 -> receiver.drawScoreFont(
							engine, 3, 4+i, rankingTime[scoreType][numColors-3][gametype][i].toTimeStr,
							i==rankingRank
						)
					}
				}
			}
		} else {
			receiver.drawScoreFont(engine, 0, 3, "Score", COLOR.BLUE)
			val strScore = "${engine.statistics.score}(+${lastScore}X$lastmultiplier)"
			receiver.drawScoreFont(engine, 0, 4, strScore)

			receiver.drawScoreFont(engine, 0, 6, "Level", COLOR.BLUE)
			receiver.drawScoreFont(engine, 0, 7, "$level")

			receiver.drawScoreFont(engine, 0, 9, "POWER", COLOR.BLUE)
			var strSent = "$garbageSent"
			if(garbageAdd>0) strSent = "$strSent(+$garbageAdd)"
			receiver.drawScoreFont(engine, 0, 10, strSent)

			receiver.drawScoreFont(engine, 0, 12, "Time", COLOR.BLUE)
			receiver.drawScoreFont(engine, 0, 13, engine.statistics.time.toTimeStr)

			receiver.drawScoreFont(engine, 11, 6, "CLEARED", COLOR.BLUE)
			receiver.drawScoreFont(engine, 11, 7, "$blocksCleared")

			receiver.drawScoreFont(engine, 11, 9, "ZENKESHI", COLOR.BLUE)
			receiver.drawScoreFont(engine, 11, 10, "$zenKeshiCount")

			receiver.drawScoreFont(engine, 11, 12, "MAX CHAIN", COLOR.BLUE)
			receiver.drawScoreFont(engine, 11, 13, engine.statistics.maxChain.toString())

			if(dangerColumnShowX&&engine.gameStarted&&engine.stat!=GameEngine.Status.MOVE
				&&engine.stat!=GameEngine.Status.RESULT)
				drawX(engine)

			val textHeight = if(engine.displaySize==1) 11 else engine.field.height+1

			val baseX = if(engine.displaySize==1) 1 else 0
			if(engine.chain>0&&chainDisplay>0&&showChains)
				receiver.drawMenuFont(
					engine, baseX+if(engine.chain>9)
						0
					else
						1, textHeight, "${engine.chain} CHAIN!", COLOR.YELLOW
				)
			if(zenKeshi)
				receiver.drawMenuFont(engine, baseX, textHeight+1, "ZENKESHI!", COLOR.YELLOW)
		}
	}

	/** Draw X on death columns*/
	override fun drawX(engine:GameEngine) {
		for(i in 0..<if(dangerColumnDouble) 2 else 1)
			if(engine.field.getBlockEmpty(2+i, 0))
				if(engine.displaySize==1) receiver.drawMenuFont(engine, 4+i*2, 0, "\u0085", COLOR.RED, 2f)
				else receiver.drawMenuFont(engine, 2+i, 0, "\u0085", COLOR.RED)
	}

	/* Called after every frame */
	override fun onLast(engine:GameEngine) {
		super.onLast(engine)
		if(chainDisplay>0) chainDisplay--

		if(gametype==1) {
			val remainTime = ULTRA_MAX_TIME-engine.statistics.time
			// Time meter
			engine.meterValue = remainTime*1f/ULTRA_MAX_TIME
			engine.meterColor = GameEngine.METER_COLOR_GREEN
			if(remainTime<=3600) engine.meterColor = GameEngine.METER_COLOR_YELLOW
			if(remainTime<=1800) engine.meterColor = GameEngine.METER_COLOR_ORANGE
			if(remainTime<=600) engine.meterColor = GameEngine.METER_COLOR_RED

			// Out of time
			if(engine.statistics.time>=ULTRA_MAX_TIME&&engine.timerActive) {
				engine.gameEnded()
				engine.resetStatc()
				engine.stat = GameEngine.Status.ENDINGSTART
				return
			}
		} else if(gametype==2) {
			var remainScore = SPRINT_MAX_SCORE[sprintTarget]-engine.statistics.score
			if(!engine.timerActive) remainScore = 0
			engine.meterValue = remainScore*1f/SPRINT_MAX_SCORE[sprintTarget]
			engine.meterColor = GameEngine.METER_COLOR_GREEN
			if(remainScore<=50) engine.meterColor = GameEngine.METER_COLOR_YELLOW
			if(remainScore<=30) engine.meterColor = GameEngine.METER_COLOR_ORANGE
			if(remainScore<=10) engine.meterColor = GameEngine.METER_COLOR_RED

			// Goal
			if(engine.statistics.score>=SPRINT_MAX_SCORE[sprintTarget]&&engine.timerActive) {
				engine.gameEnded()
				engine.resetStatc()
				engine.stat = GameEngine.Status.ENDINGSTART
			}
		}
	}

	override fun addBonus(engine:GameEngine) {
		if(gametype!=2) super.addBonus(engine)
	}

	override fun calcChainMultiplier(chain:Int):Int {
		if(scoreType==0) {
			when {
				chain==2 -> return 8
				chain==3 -> return 16
				chain>=4 -> return 32*(chain-3)
			}
		}
		return if(chain>CHAIN_POWERS_FEVERTYPE.size)
			CHAIN_POWERS_FEVERTYPE[CHAIN_POWERS_FEVERTYPE.size-1]
		else
			CHAIN_POWERS_FEVERTYPE[chain-1]
	}

	override fun lineClearEnd(engine:GameEngine):Boolean {
		super.lineClearEnd(engine)

		if(!engine.field.getBlockEmpty(2, 0)||dangerColumnDouble&&!engine.field.getBlockEmpty(3, 0)) {
			engine.stat = GameEngine.Status.GAMEOVER
			engine.gameEnded()
			engine.resetStatc()
			engine.statc[1] = 1
		}

		return false
	}

	/* Render results screen */
	override fun renderResult(engine:GameEngine) {
		if(gametype==2) {
			receiver.drawMenuFont(engine, 0, 1, "PLAY DATA", COLOR.ORANGE)
			receiver.drawMenuFont(engine, 0, 3, "Time", COLOR.BLUE)
			val strTime = "%10s".format(engine.statistics.time.toTimeStr)
			receiver.drawMenuFont(engine, 0, 4, strTime)
			receiver.drawMenuFont(engine, 0, 5, "Score", COLOR.BLUE)
			receiver.drawMenuFont(engine, 0, 6, "${engine.statistics.score}")
			receiver.drawMenuFont(engine, 0, 7, "ZENKESHI", COLOR.BLUE)
			receiver.drawMenuFont(engine, 0, 8, "%10d".format(zenKeshiCount))
			receiver.drawMenuFont(engine, 0, 9, "MAX CHAIN", COLOR.BLUE)
			receiver.drawMenuFont(engine, 0, 10, "%10d".format(engine.statistics.maxChain))
			if(rankingRank!=-1) {
				receiver.drawMenuFont(engine, 0, 11, "RANK", COLOR.BLUE)
				val strRank = "%10d".format(rankingRank+1)
				receiver.drawMenuFont(engine, 0, 12, strRank)
			}
		} else {
			super.renderResult(engine)

			if(rankingRank!=-1) {
				receiver.drawMenuFont(engine, 0, 15, "RANK", COLOR.BLUE)
				val strRank = "%10d".format(rankingRank+1)
				receiver.drawMenuFont(engine, 0, 16, strRank)
			}
		}
	}

	/* Called when saving replay */
	override fun saveReplay(engine:GameEngine, prop:CustomProperties):Boolean =
		!owner.replayMode&&engine.ai==null&&engine.colorClearSize==4&&
			updateRanking(engine.statistics.score, engine.statistics.time, gametype, scoreType, numColors)!=-1

	override fun loadSetting(engine:GameEngine, prop:CustomProperties, ruleName:String, playerID:Int) {
		gametype = prop.getProperty("avalanche.gametype", 0)
		sprintTarget = prop.getProperty("avalanche.sprintTarget", 0)
		scoreType = prop.getProperty("avalanche.scoreType", 0)
		outlinetype = prop.getProperty("avalanche.outlinetype", 0)
		numColors = prop.getProperty("avalanche.numcolors", 4)
		version = prop.getProperty("avalanche.version", 0)
		dangerColumnDouble = prop.getProperty("avalanche.dangerColumnDouble", false)
		dangerColumnShowX = prop.getProperty("avalanche.dangerColumnShowX", false)
		showChains = prop.getProperty("avalanche.showChains", true)
		cascadeSlow = prop.getProperty("avalanche.cascadeSlow", false)
		bigDisplay = prop.getProperty("avalanche.bigDisplay", false)
	}

	override fun saveSetting(engine:GameEngine, prop:CustomProperties, ruleName:String, playerID:Int) {
		prop.setProperty("avalanche.gametype", gametype)
		prop.setProperty("avalanche.sprintTarget", sprintTarget)
		prop.setProperty("avalanche.scoreType", scoreType)
		prop.setProperty("avalanche.outlinetype", outlinetype)
		prop.setProperty("avalanche.numcolors", numColors)
		prop.setProperty("avalanche.version", version)
		prop.setProperty("avalanche.dangerColumnDouble", dangerColumnDouble)
		prop.setProperty("avalanche.dangerColumnShowX", dangerColumnShowX)
		prop.setProperty("avalanche.showChains", showChains)
		prop.setProperty("avalanche.cascadeSlow", cascadeSlow)
		prop.setProperty("avalanche.bigDisplay", bigDisplay)
	}

	/** Update rankings
	 * @param sc Score
	 */
	private fun updateRanking(sc:Long, time:Int, type:Int, sctype:Int, colors:Int):Int {
		rankingRank = checkRanking(sc, time, type, sctype, colors)

		if(rankingRank!=-1) {
			// Shift down ranking entries
			for(i in rankingMax-1 downTo rankingRank+1) {
				rankingScore[sctype][colors-3][type][i] = rankingScore[sctype][colors-3][type][i-1]
				rankingTime[sctype][colors-3][type][i] = rankingTime[sctype][colors-3][type][i-1]
			}

			// Add new data
			rankingScore[sctype][colors-3][type][rankingRank] = sc
			rankingTime[sctype][colors-3][type][rankingRank] = time
		}
		return rankingRank
	}

	/** Calculate ranking position
	 * @param sc Score
	 * @param time Time
	 * @return Position (-1 if unranked)
	 */
	private fun checkRanking(sc:Long, time:Int, type:Int, sctype:Int, colors:Int):Int {
		if(type==2&&sc<SPRINT_MAX_SCORE[sprintTarget]) return -1
		for(i in 0..<rankingMax)
			if(type==0) {
				if(sc>rankingScore[sctype][colors-3][type][i])
					return i
				else if(sc==rankingScore[sctype][colors-3][type][i]&&time<rankingTime[sctype][colors-3][type][i]) return i
			} else if(type==1) {
				if(sc>rankingScore[sctype][colors-3][type][i]) return i
			} else if(type==2)
				if(time<rankingTime[sctype][colors-3][type+sprintTarget][i]||rankingTime[sctype][colors-3][type+sprintTarget][i]<0)
					return i

		return -1
	}

	companion object {
		/** Current version */
		private const val CURRENT_VERSION = 0

		/** Enabled piece types */
		private val CHAIN_POWERS_FEVERTYPE =
			listOf(4, 12, 24, 32, 48, 96, 160, 240, 320, 400, 500, 600, 700, 800, 900, 999)

		/** Number of ranking types */
		private const val RANKING_TYPE = 7

		/** Name of game types */
		private val GAMETYPE_NAME = listOf("MARATHON", "ULTRA", "SPRINT")

		/** Number of game types */
		private const val GAMETYPE_MAX = 3

		/** Name of score types */
		private val SCORETYPE_NAME = listOf("CLASSIC", "FEVER")

		/** Number of score types */
		private const val SCORETYPE_MAX = 2

		/** Max time in Ultra */
		private const val ULTRA_MAX_TIME = 10800

		/** Max score in Sprint */
		private val SPRINT_MAX_SCORE = listOf(15000, 20000, 100000, 175000, 350000)
	}
}
