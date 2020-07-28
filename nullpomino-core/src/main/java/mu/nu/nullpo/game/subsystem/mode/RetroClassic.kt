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
package mu.nu.nullpo.game.subsystem.mode

import mu.nu.nullpo.game.component.BGMStatus.BGM
import mu.nu.nullpo.game.component.Block
import mu.nu.nullpo.game.component.Controller
import mu.nu.nullpo.game.event.EventReceiver.COLOR
import mu.nu.nullpo.game.event.EventReceiver.FONT
import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.util.CustomProperties
import mu.nu.nullpo.util.GeneralUtil

/** RETRO CLASSIC mode (Original from NullpoUE build 010210 by Zircean & NTD) */
class RetroClassic:AbstractMode() {

	/** Amount of points you just get from line clears */
	private var lastscore:Int = 0

	/** Elapsed time from last line clear (lastscore is displayed to screen
	 * until this reaches to 120) */
	private var scgettime:Int = 0

	/** Selected game type */
	private var gametype:Int = 0

	/** Selected starting level */
	private var startlevel:Int = 0

	/** Selected garbage height */
	private var startheight:Int = 0

	/** Used for soft drop scoring */
	private var softdropscore:Int = 0

	/** Used for hard drop scoring */
	private var harddropscore:Int = 0

	/** Next level lines */
	private var levellines:Int = 0

	/** Big mode on/off */
	private var big:Boolean = false

	/** Version of this mode */
	private var version:Int = 0

	/** Your place on leaderboard (-1: out of rank) */
	private var rankingRank:Int = 0

	/** Score records */
	private var rankingScore:Array<IntArray> = Array(RANKING_TYPE) {IntArray(RANKING_MAX)}

	/** Line records */
	private var rankingLines:Array<IntArray> = Array(RANKING_TYPE) {IntArray(RANKING_MAX)}

	/** Level records */
	private var rankingLevel:Array<IntArray> = Array(RANKING_TYPE) {IntArray(RANKING_MAX)}

	/** Time records Reaches Score Max-out 999999 */
	private var maxScoredTime:Int? = null

	/** Returns the name of this mode */
	override val name:String
		get() = "RETRO CLASSIC"

	/** This function will be called when the game enters the main game
	 * screen. */
	override fun playerInit(engine:GameEngine, playerID:Int) {
		super.playerInit(engine, playerID)
		lastscore = 0
		scgettime = 0
		softdropscore = 0
		harddropscore = 0
		levellines = 0

		rankingRank = -1
		rankingScore = Array(RANKING_TYPE) {IntArray(RANKING_MAX)}
		rankingLines = Array(RANKING_TYPE) {IntArray(RANKING_MAX)}
		rankingLevel = Array(RANKING_TYPE) {IntArray(RANKING_MAX)}

		engine.twistEnable = false
		engine.b2bEnable = false
		engine.comboType = GameEngine.COMBO_TYPE_DISABLE
		engine.bighalf = false
		engine.bigmove = false

		engine.speed.are = 10
		engine.speed.areLine = 20
		engine.speed.lineDelay = 20
		engine.speed.das = if(gametype==GAMETYPE_ARRANGE) 12 else 16
		engine.ruleopt.lockresetMove = false
		engine.ruleopt.softdropLock = true
		engine.ruleopt.softdropSpeed = .5f
		engine.ruleopt.softdropMultiplyNativeSpeed = false
		engine.ruleopt.softdropGravitySpeedLimit = true
		if(!owner.replayMode) {
			loadSetting(owner.modeConfig)
			loadRanking(owner.recordProp, engine.ruleopt.strRuleName)
			version = CURRENT_VERSION
		} else loadSetting(owner.replayProp)

		engine.owner.backgroundStatus.bg = startlevel
		if(engine.owner.backgroundStatus.bg>19) engine.owner.backgroundStatus.bg = 19
		levellines = minOf((startlevel+1)*10, maxOf(100, (startlevel-5)*10))
		engine.framecolor = GameEngine.FRAME_SKIN_GB
	}

	/** Set the gravity speed
	 * @param engine GameEngine object
	 */
	private fun setSpeed(engine:GameEngine) {
		var lv = engine.statistics.level

		if(gametype==GAMETYPE_ARRANGE) {
			if(lv<0) lv = 0
			if(lv>=tableDenominatorArrange.size) lv = tableDenominatorArrange.size-1

			engine.speed.gravity = tableGravityArrange[lv]
			engine.speed.denominator = tableDenominatorArrange[lv]
		} else {
			if(lv<0) lv = 0
			if(lv>=tableDenominator.size) lv = tableDenominator.size-1

			engine.speed.gravity = 1
			engine.speed.denominator = tableDenominator[lv]
		}
		engine.speed.lockDelay = engine.speed.denominator/engine.speed.gravity
	}

	/** Main routine for game setup screen */
	override fun onSetting(engine:GameEngine, playerID:Int):Boolean {
		// Menu
		if(!engine.owner.replayMode) {
			// Configuration changes
			val change = updateCursor(engine, 3)

			if(change!=0) {
				engine.playSE("change")

				when(menuCursor) {
					0 -> {
						gametype += change
						if(gametype<0) gametype = GAMETYPE_MAX-1
						if(gametype>GAMETYPE_MAX-1) gametype = 0
					}
					1 -> {
						startlevel += change
						if(startlevel<0) startlevel = 19
						if(startlevel>19) startlevel = 0
						engine.owner.backgroundStatus.bg = startlevel
						levellines = minOf((startlevel+1)*10, maxOf(100, (startlevel-5)*10))
					}
					2 -> {
						startheight += change
						if(startheight<0) startheight = 5
						if(startheight>5) startheight = 0
					}
					3 -> big = !big
				}
			}

			// Check for A button, when pressed this will begin the game
			if(engine.ctrl.isPush(Controller.BUTTON_A)&&menuTime>=5) {
				engine.playSE("decide")
				saveSetting(owner.modeConfig)
				owner.saveModeConfig()
				return false
			}

			// Check for B button, when pressed this will shutdown the game engine.
			if(engine.ctrl.isPush(Controller.BUTTON_B)) engine.quitflag = true

			menuTime++
		} else {
			menuTime++
			menuCursor = -1

			return menuTime<60
		}

		return true
	}

	/** Renders game setup screen */
	override fun renderSetting(engine:GameEngine, playerID:Int) {
		drawMenu(engine, playerID, receiver, 0, COLOR.BLUE, 0, "GAME TYPE", GAMETYPE_NAME[gametype], "Level", LEVEL_NAME[startlevel], "HEIGHT", "$startheight", "BIG", GeneralUtil.getONorOFF(big))
	}

	override fun onReady(engine:GameEngine, playerID:Int):Boolean {
		if(engine.statc[0]==0) {
			engine.createFieldIfNeeded()
			fillGarbage(engine, startheight)
		}
		return false
	}

	/** This function will be called before the game actually begins (after
	 * Ready&Go screen disappears) */
	override fun startGame(engine:GameEngine, playerID:Int) {
		engine.statistics.level = startlevel
		engine.statistics.levelDispAdd = 1
		engine.big = big

		owner.bgmStatus.bgm = BGM.RETRO_N(0)
		setSpeed(engine)
	}

	/** Renders HUD (leaderboard or game statistics) */
	override fun renderLast(engine:GameEngine, playerID:Int) {
		receiver.drawScoreFont(engine, playerID, 0, 0, "RETRO CLASSIC", COLOR.GREEN)
		receiver.drawScoreFont(engine, playerID, 0, 1, "(${GAMETYPE_NAME[gametype]})", COLOR.GREEN)

		if(engine.stat==GameEngine.Status.SETTING||engine.stat==GameEngine.Status.RESULT&&!owner.replayMode) {
			if(!owner.replayMode&&!big&&engine.ai==null) {
				receiver.drawScoreFont(engine, playerID, 3, 3, "SCORE    LINE LV.", COLOR.BLUE)

				for(i in 0 until RANKING_MAX) {
					receiver.drawScoreGrade(engine, playerID, 0, 4+i, String.format("%2d", i+1), COLOR.YELLOW)
					receiver.drawScoreNum(engine, playerID, 3, 4+i, GeneralUtil.capsInteger(rankingScore[gametype][i], 6), i==rankingRank)
					receiver.drawScoreNum(engine, playerID, 12, 4+i, GeneralUtil.capsInteger(rankingLines[gametype][i], 3), i==rankingRank)
					receiver.drawScore(engine, playerID, 17, 4+i, LEVEL_NAME[rankingLevel[gametype][i]],
						font = if(rankingLevel[gametype][i]<30) FONT.NUM else FONT.NORMAL, flag = i==rankingRank)
				}
			}
		} else {
			receiver.drawScoreFont(engine, playerID, 0, 3, "SCORE${if(lastscore>0) "(+$lastscore)" else ""}", COLOR.BLUE)
			receiver.drawScore(engine, playerID, 0, 4, GeneralUtil.capsInteger(engine.statistics.score, 6),
				font = if(engine.statistics.score<=999999) FONT.NUM else FONT.NORMAL, scale = 2f)

			receiver.drawScoreFont(engine, playerID, 0, 6, "LINE", COLOR.BLUE)
			receiver.drawScore(engine, playerID, 0, 7, when(gametype) {
				GAMETYPE_TYPE_B -> String.format("-%2d", maxOf(25-engine.statistics.lines, 0))
				else -> GeneralUtil.capsInteger(engine.statistics.lines, 3)
			}, if(gametype!=GAMETYPE_TYPE_B&&engine.statistics.lines<999) FONT.NUM else FONT.NORMAL, scale = 2f)

			receiver.drawScoreFont(engine, playerID, 0, 9, "Level", COLOR.BLUE)
			receiver.drawScore(engine, playerID, 0, 10, LEVEL_NAME[engine.statistics.level],
				if(engine.statistics.level<30) FONT.NUM else FONT.NORMAL, scale = 2f)

			receiver.drawScoreFont(engine, playerID, 0, 12, "Time", COLOR.BLUE)
			receiver.drawScoreNum(engine, playerID, 0, 13, GeneralUtil.getTime(engine.statistics.time), 2f)
		}
	}

	/** This function will be called when the game timer updates */
	override fun onLast(engine:GameEngine, playerID:Int) {
		scgettime++
	}

	/** Calculates line-clear score
	 * (This function will be called even if no lines are cleared) */
	override fun calcScore(engine:GameEngine, playerID:Int, lines:Int):Int {
		softdropscore /= 2
		engine.statistics.scoreSD += softdropscore
		softdropscore = 0

		engine.statistics.scoreHD += harddropscore
		harddropscore = 0

		// Line clear score
		var pts = 0
		// Level up
		when {
			lines==1 -> pts += 40*(engine.statistics.level+1) // Single
			lines==2 -> pts += 100*(engine.statistics.level+1) // Double
			lines==3 -> pts += 300*(engine.statistics.level+1) // Triple
			lines>=4 -> pts += 1200*(engine.statistics.level+1) // Quadruple
		}

		// B-TYPE game completed
		if(gametype==GAMETYPE_TYPE_B&&engine.statistics.lines>=25) {
			pts += (engine.statistics.level+startheight)*1000
			engine.ending = 1
			engine.gameEnded()
		}

		// Add score to total
		if(pts>0) {
			lastscore = pts
			scgettime = 0
			engine.statistics.scoreLine += pts
		}

		if(gametype!=GAMETYPE_ARRANGE&&engine.statistics.score>999999&&maxScoredTime==null) {
			maxScoredTime = engine.statistics.time
			engine.playSE("applause5")
		}

		// Update meter
		if(gametype==GAMETYPE_TYPE_B) {
			engine.meterValue = minOf(engine.statistics.lines, 25)*receiver.getMeterMax(engine)/25
			engine.meterColor = when {
				engine.statistics.lines>=20 -> GameEngine.METER_COLOR_RED
				engine.statistics.lines>=15 -> GameEngine.METER_COLOR_ORANGE
				engine.statistics.lines>=10 -> GameEngine.METER_COLOR_YELLOW
				else -> GameEngine.METER_COLOR_GREEN
			}

		} else {
			engine.meterValue = engine.statistics.lines%10*receiver.getMeterMax(engine)/9
			engine.meterColor = GameEngine.METER_COLOR_GREEN
			if(engine.statistics.lines%10>=2) engine.meterColor = GameEngine.METER_COLOR_YELLOW
			if(engine.statistics.lines%10>=5) engine.meterColor = GameEngine.METER_COLOR_ORANGE
			if(engine.statistics.lines%10>=8) engine.meterColor = GameEngine.METER_COLOR_RED
		}

		if(gametype!=GAMETYPE_TYPE_B&&engine.statistics.lines>=levellines) {
			// Level up
			engine.statistics.level++

			levellines += 10

			//engine.framecolor = engine.statistics.level
			if(engine.statistics.level>255) {
				engine.statistics.level = 0
			}

			owner.backgroundStatus.fadesw = true
			owner.backgroundStatus.fadecount = 0

			var lv = engine.statistics.level

			if(lv<0) lv = 0
			if(lv>=19) lv = 19

			owner.backgroundStatus.fadebg = lv

			setSpeed(engine)
			engine.playSE("levelup")
		}
		return pts
	}

	/** This function will be called when soft-drop is used */
	override fun afterSoftDropFall(engine:GameEngine, playerID:Int, fall:Int) {
		softdropscore += fall
	}

	/** This function will be called when hard-drop is used */
	override fun afterHardDropFall(engine:GameEngine, playerID:Int, fall:Int) {
		harddropscore += fall
	}

	/** Renders game result screen */
	override fun renderResult(engine:GameEngine, playerID:Int) {
		receiver.drawMenuFont(engine, playerID, 0, 1, "PLAY DATA", COLOR.ORANGE)

		drawResultStats(engine, playerID, receiver, 3, COLOR.BLUE, Statistic.SCORE, Statistic.LINES)
		receiver.drawMenuFont(engine, playerID, 0, 7, "Level", COLOR.BLUE)
		val strLevel = String.format("%10s", LEVEL_NAME[engine.statistics.level])
		receiver.drawMenuFont(engine, playerID, 0, 8, strLevel)
		drawResultStats(engine, playerID, receiver, 9, COLOR.BLUE, Statistic.TIME, Statistic.SPL, Statistic.LPM)
		drawResultRank(engine, playerID, receiver, 15, COLOR.BLUE, rankingRank)

	}

	/** This function will be called when the replay data is going to be
	 * saved */
	override fun saveReplay(engine:GameEngine, playerID:Int, prop:CustomProperties) {
		saveSetting(prop)

		// Checks/Updates the ranking
		if(!owner.replayMode&&!big&&engine.ai==null) {
			updateRanking(engine.statistics.score, engine.statistics.lines, engine.statistics.level, gametype)

			if(rankingRank!=-1) {
				saveRanking(owner.recordProp, engine.ruleopt.strRuleName)
				owner.saveModeConfig()
			}
		}
	}

	/** Load the settings
	 * @param prop CustomProperties
	 */
	override fun loadSetting(prop:CustomProperties) {
		gametype = prop.getProperty("retromarathon.gametype", 0)
		startlevel = prop.getProperty("retromarathon.startlevel", 0)
		startheight = prop.getProperty("retromarathon.startheight", 0)
		big = prop.getProperty("retromarathon.big", false)
		version = prop.getProperty("retromarathon.version", 0)
	}

	/** Save the settings
	 * @param prop CustomProperties
	 */
	override fun saveSetting(prop:CustomProperties) {
		prop.setProperty("retromarathon.gametype", gametype)
		prop.setProperty("retromarathon.startlevel", startlevel)
		prop.setProperty("retromarathon.startheight", startheight)
		prop.setProperty("retromarathon.big", big)
		prop.setProperty("retromarathon.version", version)
	}

	/** Load the ranking
	 * @param prop CustomProperties
	 * @param ruleName Rule name
	 */
	override fun loadRanking(prop:CustomProperties, ruleName:String) {
		for(i in 0 until RANKING_MAX)
			for(gametypeIndex in 0 until RANKING_TYPE) {
				rankingScore[gametypeIndex][i] = prop.getProperty("retromarathon.ranking.$ruleName.$gametypeIndex.score.$i", 0)
				rankingLines[gametypeIndex][i] = prop.getProperty("retromarathon.ranking.$ruleName.$gametypeIndex.lines.$i", 0)
				rankingLevel[gametypeIndex][i] = prop.getProperty("retromarathon.ranking.$ruleName.$gametypeIndex.level.$i", 0)
			}
	}

	/** Save the ranking
	 * @param prop CustomProperties
	 * @param ruleName Rule name
	 */
	fun saveRanking(prop:CustomProperties, ruleName:String) {
		for(i in 0 until RANKING_MAX)
			for(gametypeIndex in 0 until RANKING_TYPE) {
				prop.setProperty("retromarathon.ranking.$ruleName.$gametypeIndex.score.$i", rankingScore[gametypeIndex][i])
				prop.setProperty("retromarathon.ranking.$ruleName.$gametypeIndex.lines.$i", rankingLines[gametypeIndex][i])
				prop.setProperty("retromarathon.ranking.$ruleName.$gametypeIndex.level.$i", rankingLevel[gametypeIndex][i])
			}
	}

	/** Update the ranking
	 * @param sc Score
	 * @param li Lines
	 * @param lv Level
	 * @param type Game type
	 */
	private fun updateRanking(sc:Int, li:Int, lv:Int, type:Int) {
		rankingRank = checkRanking(sc, li, lv, type)

		if(rankingRank!=-1) {
			// Shift the ranking data
			for(i in RANKING_MAX-1 downTo rankingRank+1) {
				rankingScore[type][i] = rankingScore[type][i-1]
				rankingLines[type][i] = rankingLines[type][i-1]
				rankingLevel[type][i] = rankingLevel[type][i-1]
			}

			// Insert a new data
			rankingScore[type][rankingRank] = sc
			rankingLines[type][rankingRank] = li
			rankingLevel[type][rankingRank] = lv
		}
	}

	/** This function will check the ranking and returns which place you are.
	 * (-1: Out of rank)
	 * @param sc Score
	 * @param li Lines
	 * @param lv Level
	 * @return Place (First place is 0. -1 is Out of Rank)
	 */
	private fun checkRanking(sc:Int, li:Int, lv:Int, type:Int):Int {
		for(i in 0 until RANKING_MAX)
			if(sc>rankingScore[type][i]) return i
			else if(sc==rankingScore[type][i]&&li>rankingLines[type][i])
				return i
			else if(sc==rankingScore[type][i]&&li==rankingLines[type][i]&&lv<rankingLevel[type][i]) return i

		return -1
	}

	/** Fill the playfield with garbage
	 * @param engine GameEngine
	 * @param height Garbage height level number
	 */
	private fun fillGarbage(engine:GameEngine, height:Int) {
		val h = engine.field!!.height
		val startHeight = if(version>=2) h-1 else h
		var f:Float
		for(y in startHeight downTo h-tableGarbageHeight[height])
			for(x in 0 until engine.field!!.width) {
				f = engine.random.nextFloat()
				if(f<0.5)
					engine.field!!.setBlock(x, y, Block((f*14).toInt()+2, engine.skin, Block.ATTRIBUTE.VISIBLE, Block.ATTRIBUTE.GARBAGE))
			}
	}

	companion object {

		/** Current version of this mode */
		private const val CURRENT_VERSION = 2

		/** Denominator table (Normal) */
		private val tableDenominator = intArrayOf(
			//	0  1  2  3  4  5  6  7  8  9    +xx
			48, 43, 38, 33, 28, 23, 18, 13, 8, 6, // 00
			5, 5, 5, 4, 4, 4, 3, 3, 3, 2, // 10
			2, 2, 2, 2, 2, 2, 2, 2, 2, 1 // 20
		)

		/** Gravity table (Arrange) */
		private val tableGravityArrange = intArrayOf(
			//	0  1  2  3  4  5  6  7  8  9    +xx
			1, 1, 1, 1, 1, 1, 1, 1, 1, 1, // 00
			1, 1, 1, 1, 1, 1, 1, 1, 1, 1, // 10
			1, 1, 1, 1, 1, 1, 5, 5, 3, 3, // 20
			7, 7, 2, 2, 2, 3, 3, 5, 5, -1 // 30
		)

		/** Denominator table (Arrange) */
		private val tableDenominatorArrange = intArrayOf(
			//	0  1  2  3  4  5  6  7  8  9    +xx
			48, 43, 38, 33, 28, 23, 18, 13, 8, 6, // 00
			5, 5, 4, 4, 4, 3, 3, 3, 2, 2, // 10
			2, 2, 2, 1, 1, 1, 4, 4, 2, 2, // 20
			4, 4, 1, 1, 1, 1, 1, 1, 1, 1 // 30
		)

		/** Garbage height table */
		private val tableGarbageHeight = intArrayOf(0, 3, 5, 8, 10, 12)

		/** Game types */
		private const val GAMETYPE_TYPE_A = 0
		private const val GAMETYPE_TYPE_B = 1
		private const val GAMETYPE_ARRANGE = 2

		/** Number of game types */
		private const val GAMETYPE_MAX = 3

		/** Game type name */
		private val GAMETYPE_NAME = arrayOf("A-TYPE", "B+TYPE", "C#TYPE")

		/** Number of ranking records */
		private const val RANKING_MAX = 10

		/** Number of ranking types */
		private const val RANKING_TYPE = 3

		/** Maximum-Level name table */
		private val LEVEL_NAME = arrayOf(
			//    0    1    2    3    4    5    6    7    8    9      +xx
			"00", "01", "02", "03", "04", "05", "06", "07", "08", "09", // 000
			"10", "11", "12", "13", "14", "15", "16", "17", "18", "19", // 010
			"20", "21", "22", "23", "24", "25", "26", "27", "28", "29", // 020
			"00", "0A", "14", "1E", "28", "32", "3C", "46", "50", "5A", // 030
			"64", "6E", "78", "82", "8C", "96", "A0", "AA", "B4", "BE", // 040
			"C6", "20", "E6", "20", "06", "21", "26", "21", "46", "21", // 050
			"66", "21", "86", "21", "A6", "21", "C6", "21", "E6", "21", // 060
			"06", "22", "26", "22", "46", "22", "66", "22", "86", "22", // 070
			"A6", "22", "C6", "22", "E6", "22", "06", "23", "26", "23", // 080
			"85", "A8", "29", "F0", "4A", "4A", "4A", "4A", "8D", "07", // 090
			"20", "A5", "A8", "29", "0F", "8D", "07", "20", "60", "A6", // 100
			"49", "E0", "15", "10", "53", "BD", "D6", "96", "A8", "8A", // 110
			"0A", "AA", "E8", "BD", "EA", "96", "8D", "06", "20", "CA", // 120
			"A5", "BE", "C9", "01", "F0", "1E", "A5", "B9", "C9", "05", // 130
			"F0", "0C", "BD", "EA", "96", "38", "E9", "02", "8D", "06", // 140
			"20", "4C", "67", "97", "BD", "EA", "96", "18", "69", "0C", // 150
			"8D", "06", "20", "4C", "67", "97", "BD", "EA", "96", "18", // 160
			"69", "06", "8D", "06", "20", "A2", "0A", "B1", "B8", "8D", // 170
			"07", "20", "C8", "CA", "D0", "F7", "E6", "49", "A5", "49", // 180
			"C9", "14", "30", "04", "A9", "20", "85", "49", "60", "A5", // 190
			"B1", "29", "03", "D0", "78", "A9", "00", "85", "AA", "A6", // 200
			"AA", "B5", "4A", "F0", "5C", "0A", "A8", "B9", "EA", "96", // 210
			"85", "A8", "A5", "BE", "C9", "01", "D0", "0A", "A5", "A8", // 220
			"18", "69", "06", "85", "A8", "4C", "BD", "97", "A5", "B9", // 230
			"C9", "04", "D0", "0A", "A5", "A8", "38", "E9", "02", "85", // 240
			"A8", "4C", "BD", "97", "A5", "A8" // 250
		)
	}
}
