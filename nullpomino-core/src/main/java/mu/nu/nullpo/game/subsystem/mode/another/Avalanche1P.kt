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

import mu.nu.nullpo.game.component.Controller
import mu.nu.nullpo.game.event.EventReceiver
import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.util.CustomProperties
import mu.nu.nullpo.util.GeneralUtil

/** AVALANCHE mode (Release Candidate 2) */
class Avalanche1P:Avalanche1PDummyMode() {

	/** Selected game type */
	private var gametype:Int = 0

	/** Version number */
	private var version:Int = 0

	/** Current round's ranking rank */
	private var rankingRank:Int = 0

	/** Rankings' line counts */
	private var rankingScore:Array<Array<Array<IntArray>>> =
		Array(SCORETYPE_MAX) {Array(3) {Array(RANKING_TYPE) {IntArray(RANKING_MAX)}}}

	/** Rankings' times */
	private var rankingTime:Array<Array<Array<IntArray>>> =
		Array(SCORETYPE_MAX) {Array(3) {Array(RANKING_TYPE) {IntArray(RANKING_MAX)}}}

	/** Chain display enable/disable */
	private var showChains:Boolean = false

	/** If true, both columns 3 and 4 are danger columns */
	private var dangerColumnDouble:Boolean = false

	/** If true, red X's appear at tops of danger columns */
	private var dangerColumnShowX:Boolean = false

	/** True for classic scoring, false for 15th scoring algorithm */
	private var scoreType:Int = 0

	/** Sprint target score */
	private var sprintTarget:Int = 0

	/* Mode name */
	override val name:String
		get() = "AVALANCHE 1P (RC2)"

	/* Initialization */
	override fun playerInit(engine:GameEngine, playerID:Int) {
		super.playerInit(engine, playerID)

		showChains = true

		scoreType = 0
		sprintTarget = 0

		rankingRank = -1
		rankingScore = Array(SCORETYPE_MAX) {Array(3) {Array(RANKING_TYPE) {IntArray(RANKING_MAX)}}}
		rankingTime = Array(SCORETYPE_MAX) {Array(3) {Array(RANKING_TYPE) {IntArray(RANKING_MAX)}}}

		if(!owner.replayMode) {
			loadSetting(owner.modeConfig)
			loadRanking(owner.recordProp, engine.ruleopt.strRuleName)
			version = CURRENT_VERSION
		} else
			loadSetting(owner.replayProp)
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
	override fun onSetting(engine:GameEngine, playerID:Int):Boolean {
		// Menu
		if(!engine.owner.replayMode) {
			// Up
			if(engine.ctrl!!.isMenuRepeatKey(Controller.BUTTON_UP)) {
				menuCursor--
				if(menuCursor<0)
					menuCursor = 10
				else if(menuCursor==1&&gametype!=2) menuCursor--
				engine.playSE("cursor")
			}
			// Down
			if(engine.ctrl!!.isMenuRepeatKey(Controller.BUTTON_DOWN)) {
				menuCursor++
				if(menuCursor>10)
					menuCursor = 0
				else if(menuCursor==1&&gametype!=2) menuCursor++
				engine.playSE("cursor")
			}

			// Configuration changes
			var change = 0
			if(engine.ctrl!!.isMenuRepeatKey(Controller.BUTTON_LEFT)) change = -1
			if(engine.ctrl!!.isMenuRepeatKey(Controller.BUTTON_RIGHT)) change = 1

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
			if(engine.ctrl!!.isPush(Controller.BUTTON_A)&&menuTime>=5) {
				engine.playSE("decide")
				saveSetting(owner.modeConfig)
				owner.saveModeConfig()
				return false
			}

			// Cancel
			if(engine.ctrl!!.isPush(Controller.BUTTON_B)) engine.quitflag = true

			menuTime++
		} else {
			menuTime++
			menuCursor = -1

			if(menuTime>=60)
				menuCursor = 9
			else
				return menuTime<120
		}

		return true
	}

	/* Render the settings screen */
	override fun renderSetting(engine:GameEngine, playerID:Int) {
		if(menuCursor<=8) {
			drawMenu(engine, playerID, receiver, 0, EventReceiver.COLOR.BLUE, 0, "GAME TYPE", GAMETYPE_NAME[gametype])
			if(gametype==2)
				drawMenu(engine, playerID, receiver, 2, EventReceiver.COLOR.BLUE, 1, "TARGET", "${SPRINT_MAX_SCORE[sprintTarget]}")
			drawMenu(engine, playerID, receiver, 4, EventReceiver.COLOR.BLUE, 2, "SCORE TYPE", SCORETYPE_NAME[scoreType], "COLORS", "$numColors", "X COLUMN",
				if(dangerColumnDouble)
					"3 AND 4"
				else
					"3 ONLY", "X SHOW", GeneralUtil.getONorOFF(dangerColumnShowX), "CLEAR SIZE", engine.colorClearSize.toString(), "FALL ANIM",
				if(cascadeSlow)
					"FEVER"
				else
					"CLASSIC", "BIG DISP", GeneralUtil.getONorOFF(bigDisplay))

			receiver.drawMenuFont(engine, playerID, 0, 19, "PAGE 1/2", EventReceiver.COLOR.YELLOW)
		} else {
			var strOutline = ""
			if(outlinetype==0) strOutline = "NORMAL"
			if(outlinetype==1) strOutline = "COLOR"
			if(outlinetype==2) strOutline = "NONE"
			drawMenu(engine, playerID, receiver, 0, EventReceiver.COLOR.BLUE, 9, "OUTLINE", strOutline, "SHOW CHAIN", GeneralUtil.getONorOFF(showChains))

			receiver.drawMenuFont(engine, playerID, 0, 19, "PAGE 2/2", EventReceiver.COLOR.YELLOW)
		}
	}

	/* When the piece is movable */
	override fun renderMove(engine:GameEngine, playerID:Int) {
		if(dangerColumnShowX&&engine.gameStarted) drawXorTimer(engine, playerID)
	}

	/* Render score */
	override fun renderLast(engine:GameEngine, playerID:Int) {
		var modeStr = GAMETYPE_NAME[gametype]
		if(gametype==2) modeStr = "$modeStr ${SPRINT_MAX_SCORE[sprintTarget]/1000}K"
		receiver.drawScoreFont(engine, playerID, 0, 0, "AVALANCHE ($modeStr)", EventReceiver.COLOR.COBALT)
		receiver.drawScoreFont(engine, playerID, 0, 1, "(${SCORETYPE_NAME[scoreType]} $numColors COLORS)", EventReceiver.COLOR.COBALT)

		if(engine.stat==GameEngine.Status.SETTING||engine.stat==GameEngine.Status.RESULT&&!owner.replayMode) {
			if(!owner.replayMode&&engine.ai==null&&engine.colorClearSize==4) {
				val scale = if(receiver.nextDisplayType==2&&gametype==0) .5f else 1f
				val topY = if(receiver.nextDisplayType==2&&gametype==0) 6 else 4

				when(gametype) {
					0 -> receiver.drawScoreFont(engine, playerID, 3, topY-1, "SCORE      TIME", EventReceiver.COLOR.BLUE, scale)
					1 -> receiver.drawScoreFont(engine, playerID, 3, 3, "SCORE", EventReceiver.COLOR.BLUE)
					2 -> receiver.drawScoreFont(engine, playerID, 3, 3, "TIME", EventReceiver.COLOR.BLUE)
				}

				for(i in 0 until RANKING_MAX) {
					receiver.drawScoreGrade(engine, playerID, 0, topY+i, String.format("%2d", i+1), EventReceiver.COLOR.YELLOW, scale)
					when(gametype) {
						0 -> {
							receiver.drawScoreFont(engine, playerID, 3, topY+i, "${rankingScore[scoreType][numColors-3][gametype][i]}", i==rankingRank, scale)
							receiver.drawScoreFont(engine, playerID, 14, topY+i, GeneralUtil.getTime(rankingTime[scoreType][numColors-3][gametype][i]), i==rankingRank, scale)
						}
						1 -> receiver.drawScoreFont(engine, playerID, 3, 4+i, "${rankingScore[scoreType][numColors-3][gametype][i]}", i==rankingRank)
						2 -> receiver.drawScoreFont(engine, playerID, 3, 4+i, GeneralUtil.getTime(rankingTime[scoreType][numColors-3][gametype][i]), i==rankingRank)
					}
				}
			}
		} else {
			receiver.drawScoreFont(engine, playerID, 0, 3, "SCORE", EventReceiver.COLOR.BLUE)
			val strScore:String = if(lastscore==0||lastmultiplier==0||scgettime<=0)
				"${engine.statistics.score}"
			else "${engine.statistics.score}(+${lastscore}X$lastmultiplier)"
			receiver.drawScoreFont(engine, playerID, 0, 4, strScore)

			receiver.drawScoreFont(engine, playerID, 0, 6, "LEVEL", EventReceiver.COLOR.BLUE)
			receiver.drawScoreFont(engine, playerID, 0, 7, "$level")

			receiver.drawScoreFont(engine, playerID, 0, 9, "OJAMA SENT", EventReceiver.COLOR.BLUE)
			var strSent = "$garbageSent"
			if(garbageAdd>0) strSent = "$strSent(+$garbageAdd)"
			receiver.drawScoreFont(engine, playerID, 0, 10, strSent)

			receiver.drawScoreFont(engine, playerID, 0, 12, "TIME", EventReceiver.COLOR.BLUE)
			receiver.drawScoreFont(engine, playerID, 0, 13, GeneralUtil.getTime(engine.statistics.time))

			receiver.drawScoreFont(engine, playerID, 11, 6, "CLEARED", EventReceiver.COLOR.BLUE)
			receiver.drawScoreFont(engine, playerID, 11, 7, "$blocksCleared")

			receiver.drawScoreFont(engine, playerID, 11, 9, "ZENKESHI", EventReceiver.COLOR.BLUE)
			receiver.drawScoreFont(engine, playerID, 11, 10, "$zenKeshiCount")

			receiver.drawScoreFont(engine, playerID, 11, 12, "MAX CHAIN", EventReceiver.COLOR.BLUE)
			receiver.drawScoreFont(engine, playerID, 11, 13, engine.statistics.maxChain.toString())

			if(dangerColumnShowX&&engine.gameStarted&&engine.stat!=GameEngine.Status.MOVE
				&&engine.stat!=GameEngine.Status.RESULT)
				drawXorTimer(engine, playerID)

			var textHeight = 13
			if(engine.field!=null) textHeight = engine.field!!.height+1
			if(engine.displaysize==1) textHeight = 11

			val baseX = if(engine.displaysize==1) 1 else 0
			if(engine.chain>0&&chainDisplay>0&&showChains)
				receiver.drawMenuFont(engine, playerID, baseX+if(engine.chain>9)
					0
				else
					1, textHeight, "${engine.chain} CHAIN!", EventReceiver.COLOR.YELLOW)
			if(zenKeshi)
				receiver.drawMenuFont(engine, playerID, baseX, textHeight+1, "ZENKESHI!", EventReceiver.COLOR.YELLOW)
		}
	}

	/** Draw X on death columns
	 * @param engine GameEngine
	 * @param playerID Player ID
	 */
	private fun drawXorTimer(engine:GameEngine, playerID:Int) {
		for(i in 0 until if(dangerColumnDouble) 2 else 1)
			if(engine.field==null||engine.field!!.getBlockEmpty(2+i, 0))
				if(engine.displaysize==1)
					receiver.drawMenuFont(engine, playerID, 4+i*2, 0, "e", EventReceiver.COLOR.RED, 2f)
				else
					receiver.drawMenuFont(engine, playerID, 2+i, 0, "e", EventReceiver.COLOR.RED)
	}

	/* Called after every frame */
	override fun onLast(engine:GameEngine, playerID:Int) {
		if(scgettime>0) scgettime--
		if(chainDisplay>0) chainDisplay--

		if(gametype==1) {
			val remainTime = ULTRA_MAX_TIME-engine.statistics.time
			// Time meter
			engine.meterValue = remainTime*receiver.getMeterMax(engine)/ULTRA_MAX_TIME
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
			engine.meterValue = remainScore*receiver.getMeterMax(engine)/SPRINT_MAX_SCORE[sprintTarget]
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

	override fun addBonus(engine:GameEngine, playerID:Int) {
		if(gametype!=2) super.addBonus(engine, playerID)
	}

	override fun calcChainMultiplier(chain:Int):Int {
		if(scoreType==0) {
			when {
				chain==2 -> return 8
				chain==3 -> return 16
				chain>=4 -> return 32*(chain-3)
			}
		} else return if(chain>CHAIN_POWERS_FEVERTYPE.size)
			CHAIN_POWERS_FEVERTYPE[CHAIN_POWERS_FEVERTYPE.size-1]
		else
			CHAIN_POWERS_FEVERTYPE[chain-1]
		return 0
	}

	override fun lineClearEnd(engine:GameEngine, playerID:Int):Boolean {
		super.lineClearEnd(engine, playerID)

		if(engine.field!=null)
			if(!engine.field!!.getBlockEmpty(2, 0)||dangerColumnDouble&&!engine.field!!.getBlockEmpty(3, 0)) {
				engine.stat = GameEngine.Status.GAMEOVER
				engine.gameEnded()
				engine.resetStatc()
				engine.statc[1] = 1
			}

		return false
	}

	/* Render results screen */
	override fun renderResult(engine:GameEngine, playerID:Int) {

		if(gametype==2) {
			receiver.drawMenuFont(engine, playerID, 0, 1, "PLAY DATA", EventReceiver.COLOR.ORANGE)
			receiver.drawMenuFont(engine, playerID, 0, 3, "TIME", EventReceiver.COLOR.BLUE)
			val strTime = String.format("%10s", GeneralUtil.getTime(engine.statistics.time))
			receiver.drawMenuFont(engine, playerID, 0, 4, strTime)
			receiver.drawMenuFont(engine, playerID, 0, 5, "SCORE", EventReceiver.COLOR.BLUE)
			receiver.drawMenuFont(engine, playerID, 0, 6, "${engine.statistics.score}")
			receiver.drawMenuFont(engine, playerID, 0, 7, "ZENKESHI", EventReceiver.COLOR.BLUE)
			receiver.drawMenuFont(engine, playerID, 0, 8, String.format("%10d", zenKeshiCount))
			receiver.drawMenuFont(engine, playerID, 0, 9, "MAX CHAIN", EventReceiver.COLOR.BLUE)
			receiver.drawMenuFont(engine, playerID, 0, 10, String.format("%10d", engine.statistics.maxChain))
			if(rankingRank!=-1) {
				receiver.drawMenuFont(engine, playerID, 0, 11, "RANK", EventReceiver.COLOR.BLUE)
				val strRank = String.format("%10d", rankingRank+1)
				receiver.drawMenuFont(engine, playerID, 0, 12, strRank)
			}
		} else {
			super.renderResult(engine, playerID)

			if(rankingRank!=-1) {
				receiver.drawMenuFont(engine, playerID, 0, 15, "RANK", EventReceiver.COLOR.BLUE)
				val strRank = String.format("%10d", rankingRank+1)
				receiver.drawMenuFont(engine, playerID, 0, 16, strRank)
			}
		}
	}

	/* Called when saving replay */
	override fun saveReplay(engine:GameEngine, playerID:Int, prop:CustomProperties) {
		saveSetting(prop)

		// Update rankings
		if(!owner.replayMode&&engine.ai==null&&engine.colorClearSize==4) {
			updateRanking(engine.statistics.score, engine.statistics.time, gametype, scoreType, numColors)

			if(rankingRank!=-1) {
				saveRanking(owner.modeConfig, engine.ruleopt.strRuleName)
				owner.saveModeConfig()
			}
		}
	}

	/** Load settings from property file
	 * @param prop Property file
	 */
	override fun loadSetting(prop:CustomProperties) {
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

	/** Save settings to property file
	 * @param prop Property file
	 */
	override fun saveSetting(prop:CustomProperties) {
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

	/** Read rankings from property file
	 * @param prop Property file
	 * @param ruleName Rule name
	 */
	override fun loadRanking(prop:CustomProperties, ruleName:String) {
		for(i in 0 until RANKING_MAX)
			for(j in 0 until GAMETYPE_MAX)
				for(colors in 3..5)
					for(sctype in 0 until SCORETYPE_MAX) {
						rankingScore[sctype][colors-3][j][i] = prop.getProperty("avalanche.ranking.$ruleName.scoretype$sctype.${colors}colors.$j.score.$i", 0)
						rankingTime[sctype][colors-3][j][i] = prop.getProperty("avalanche.ranking.$ruleName.scoretype$sctype.${colors}colors.$j.time.$i", -1)
					}
	}

	/** Save rankings to property file
	 * @param prop Property file
	 * @param ruleName Rule name
	 */
	private fun saveRanking(prop:CustomProperties, ruleName:String) {
		for(i in 0 until RANKING_MAX)
			for(j in 0 until GAMETYPE_MAX)
				for(colors in 3..5)
					for(sctype in 0 until SCORETYPE_MAX) {
						prop.setProperty("avalanche.ranking.$ruleName.scoretype$sctype.${colors}colors.$j.score.$i", rankingScore[sctype][colors-3][j][i])
						prop.setProperty("avalanche.ranking.$ruleName.scoretype$sctype.${colors}colors.$j.time.$i", rankingTime[sctype][colors-3][j][i])
					}
	}

	/** Update rankings
	 * @param sc Score
	 */
	private fun updateRanking(sc:Int, time:Int, type:Int, sctype:Int, colors:Int) {
		rankingRank = checkRanking(sc, time, type, sctype, colors)

		if(rankingRank!=-1) {
			// Shift down ranking entries
			for(i in RANKING_MAX-1 downTo rankingRank+1) {
				rankingScore[sctype][colors-3][type][i] = rankingScore[sctype][colors-3][type][i-1]
				rankingTime[sctype][colors-3][type][i] = rankingTime[sctype][colors-3][type][i-1]
			}

			// Add new data
			rankingScore[sctype][colors-3][type][rankingRank] = sc
			rankingTime[sctype][colors-3][type][rankingRank] = time
		}
	}

	/** Calculate ranking position
	 * @param sc Score
	 * @param time Time
	 * @return Position (-1 if unranked)
	 */
	private fun checkRanking(sc:Int, time:Int, type:Int, sctype:Int, colors:Int):Int {
		if(type==2&&sc<SPRINT_MAX_SCORE[sprintTarget]) return -1
		for(i in 0 until RANKING_MAX)
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
			intArrayOf(4, 12, 24, 32, 48, 96, 160, 240, 320, 400, 500, 600, 700, 800, 900, 999)

		/** Number of ranking records */
		private const val RANKING_MAX = 10

		/** Number of ranking types */
		private const val RANKING_TYPE = 7

		/** Name of game types */
		private val GAMETYPE_NAME = arrayOf("MARATHON", "ULTRA", "SPRINT")

		/** Number of game types */
		private const val GAMETYPE_MAX = 3

		/** Name of score types */
		private val SCORETYPE_NAME = arrayOf("CLASSIC", "FEVER")

		/** Number of score types */
		private const val SCORETYPE_MAX = 2

		/** Max time in Ultra */
		private const val ULTRA_MAX_TIME = 10800

		/** Max score in Sprint */
		private val SPRINT_MAX_SCORE = intArrayOf(15000, 20000, 100000, 175000, 350000)
	}
}
