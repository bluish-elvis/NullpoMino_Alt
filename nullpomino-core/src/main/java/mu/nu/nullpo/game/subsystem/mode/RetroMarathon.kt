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
import mu.nu.nullpo.game.component.Controller
import mu.nu.nullpo.game.event.EventReceiver.COLOR
import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.game.subsystem.mode.RetroMarathon.Companion.GAMETYPE.*
import mu.nu.nullpo.util.CustomProperties
import mu.nu.nullpo.util.GeneralUtil

/** RETRO MASTERY mode by Pineapple 20100722 - 20100808 */
class RetroMarathon:AbstractMode() {

	/** GameManager object (Manages entire game status) */

	/** EventReceiver object (This receives many game events, can also be used
	 * for drawing the fonts.) */

	/** Amount of points you just get from line clears */
	private var lastscore:Int = 0

	/** Elapsed time from last line clear (lastscore is displayed to screen
	 * until this reaches to 120) */
	private var scgettime:Int = 0

	/** Selected game type */
	private var gametype:GAMETYPE = RACE200

	/** Selected starting level */
	private var startlevel:Int = 0

	/** Used for soft drop scoring */
	private var softdropscore:Int = 0

	/** Used for hard drop scoring */
	private var harddropscore:Int = 0

	/** Number of "lines" cleared (most things use this instead of
	 * engine.statistics.lines); don't ask me why I called it this... */
	private var loons:Int = 0

	/** Number of line clear actions */
	private var actions:Int = 0

	/** Efficiency (engine.statistics.lines / actions) */
	private var efficiency:Float = 0f

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

	/** Returns the name of this mode */
	override val name:String = "Retro Marathon.A"

	/** This function will be called when the game enters the main game
	 * screen. */
	override fun playerInit(engine:GameEngine, playerID:Int) {
		super.playerInit(engine, playerID)
		lastscore = 0
		scgettime = 0
		softdropscore = 0
		harddropscore = 0
		levellines = 0
		loons = 0
		actions = 0
		efficiency = 0f

		rankingRank = -1
		rankingScore = Array(RANKING_TYPE) {IntArray(RANKING_MAX)}
		rankingLines = Array(RANKING_TYPE) {IntArray(RANKING_MAX)}
		rankingLevel = Array(RANKING_TYPE) {IntArray(RANKING_MAX)}

		engine.twistEnable = false
		engine.b2bEnable = false
		engine.splitb2b = false
		engine.comboType = GameEngine.COMBO_TYPE_DISABLE
		engine.bighalf = true
		engine.bigmove = true

		engine.speed.are = 12
		engine.speed.areLine = 15
		engine.speed.das = 12
		engine.ruleopt.lockresetMove = false
		engine.ruleopt.lockresetRotate = false
		engine.ruleopt.lockresetWallkick = false
		engine.ruleopt.lockresetFall = true
		engine.ruleopt.softdropLock = true
		engine.ruleopt.softdropMultiplyNativeSpeed = false
		engine.ruleopt.softdropGravitySpeedLimit = false
		engine.ruleopt.softdropSpeed = .5f
		engine.owSDSpd = -1
		if(!owner.replayMode) {
			loadSetting(owner.modeConfig)
			loadRanking(owner.recordProp, engine.ruleopt.strRuleName)
			version = CURRENT_VERSION
		} else
			loadSetting(owner.replayProp)

		engine.owner.backgroundStatus.bg = if(gametype==PRESSURE) 0 else startlevel
		if(engine.owner.backgroundStatus.bg>19) engine.owner.backgroundStatus.bg = 19
		engine.framecolor = GameEngine.FRAME_COLOR_GRAY
	}

	/** Set the gravity speed
	 * @param engine GameEngine object
	 */
	private fun setSpeed(engine:GameEngine) {
		var lv = engine.statistics.level

		if(lv<0) lv = 0
		if(lv>=tableDenominator.size) lv = tableDenominator.size-1

		engine.speed.gravity = tableGravity[lv]
		engine.speed.denominator = tableDenominator[lv]
		engine.speed.lockDelay = tableLockDelay[lv]
		engine.speed.lineDelay = if(lv>=10) 20 else 25
	}

	/** Main routine for game setup screen */
	override fun onSetting(engine:GameEngine, playerID:Int):Boolean {
		// Menu
		if(!engine.owner.replayMode) {
			// Check for UP button, when pressed it will move cursor up.
			if(engine.ctrl.isMenuRepeatKey(Controller.BUTTON_UP)) {
				menuCursor--
				if(menuCursor==1&&gametype==PRESSURE) menuCursor--
				if(menuCursor<0) menuCursor = 2
				engine.playSE("cursor")
			}
			// Check for DOWN button, when pressed it will move cursor down.
			if(engine.ctrl.isMenuRepeatKey(Controller.BUTTON_DOWN)) {
				menuCursor++
				if(menuCursor==1&&gametype==PRESSURE) menuCursor++
				if(menuCursor>2) menuCursor = 0
				engine.playSE("cursor")
			}

			// Check for LEFT/RIGHT keys
			var change = 0
			if(engine.ctrl.isMenuRepeatKey(Controller.BUTTON_LEFT)) change = -1
			if(engine.ctrl.isMenuRepeatKey(Controller.BUTTON_RIGHT)) change = 1

			if(change!=0) {
				engine.playSE("change")

				when(menuCursor) {
					0 -> {
						gametype = when(gametype) {
							values().first() -> values().last()
							values().last() -> values().first()
							else -> values()[gametype.ordinal+change]
						}
						engine.owner.backgroundStatus.bg = if(gametype==PRESSURE) 0 else startlevel
					}
					1 -> {
						startlevel += change
						if(startlevel<0) startlevel = 19
						if(startlevel>19) startlevel = 0
						engine.owner.backgroundStatus.bg = startlevel
					}
					2 -> big = !big
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
		if(!engine.owner.replayMode)
			receiver.drawMenuFont(engine, playerID, 0, menuCursor*2+1, "\u0082", COLOR.RED)

		receiver.drawMenuFont(engine, playerID, 0, 0, "GAME TYPE", COLOR.BLUE)
		receiver.drawMenuFont(engine, playerID, 1, 1, gametype.name, menuCursor==0)
		if(gametype!=ENDLESS) {
			receiver.drawMenuFont(engine, playerID, 0, 2, "Level", COLOR.BLUE)
			receiver.drawMenuFont(engine, playerID, 1, 3, String.format("%02d", startlevel), menuCursor==1)
		}
		receiver.drawMenuFont(engine, playerID, 0, 4, "BIG", COLOR.BLUE)
		receiver.drawMenuFont(engine, playerID, 1, 5, GeneralUtil.getONorOFF(big), menuCursor==2)
	}

	/** This function will be called before the game actually begins (after
	 * Ready&Go screen disappears) */
	override fun startGame(engine:GameEngine, playerID:Int) {
		engine.big = big
		engine.statistics.levelDispAdd = 1

		owner.bgmStatus.bgm = BGM.RETRO_A(0)
		when(gametype) {
			PRESSURE -> {
				engine.statistics.level = 0
				levellines = 5
			}
			RACE200 -> {
				engine.statistics.level = startlevel
				levellines = 10*minOf(startlevel+1, 10)
			}
			ENDLESS -> {
				engine.statistics.level = startlevel
				levellines = if(startlevel<=9) (startlevel+1)*10 else (startlevel+11)*5
			}
		}

		setSpeed(engine)
	}

	/** Renders HUD (leaderboard or game statistics) */
	override fun renderLast(engine:GameEngine, playerID:Int) {
		receiver.drawScoreFont(engine, playerID, 0, 0, "RETRO MASTERY", COLOR.GREEN)
		receiver.drawScoreFont(engine, playerID, 0, 1, "(${gametype.name})", COLOR.GREEN)

		if(engine.stat==GameEngine.Status.SETTING||engine.stat==GameEngine.Status.RESULT&&!owner.replayMode) {
			if(!owner.replayMode&&!big&&engine.ai==null) {
				receiver.drawScoreFont(engine, playerID, 3, 3, "SCORE    LINE LV.", COLOR.BLUE)

				for(i in 0 until RANKING_MAX) {
					receiver.drawScoreGrade(engine, playerID, 0, 4+i, String.format("%2d", i+1),
						if(rankingRank==i) COLOR.RAINBOW else COLOR.YELLOW)
					receiver.drawScoreNum(engine, playerID, 3, 4+i, "${rankingScore[gametype.ordinal][i]}", i==rankingRank)
					receiver.drawScoreNum(engine, playerID, 12, 4+i, "${rankingLines[gametype.ordinal][i]}", i==rankingRank)
					receiver.drawScoreNum(engine, playerID, 17, 4+i, String.format("%02d", rankingLevel[gametype.ordinal][i]), i==rankingRank)
				}
			}
		} else {
			receiver.drawScoreFont(engine, playerID, 0, 3, "Score", COLOR.BLUE)
			val strScore:String = if(lastscore==0||scgettime>=120)
				"${engine.statistics.score}"
			else
				"${engine.statistics.score} (+$lastscore)"
			receiver.drawScoreNum(engine, playerID, 0, 4, strScore, 2f)

			val strLine = "$loons"

			receiver.drawScoreFont(engine, playerID, 0, 6, "Lines", COLOR.BLUE)
			receiver.drawScoreNum(engine, playerID, 0, 7, strLine, 2f)

			receiver.drawScoreFont(engine, playerID, 0, 9, "Level", COLOR.BLUE)
			receiver.drawScoreFont(engine, playerID, 0, 10, String.format("%02d", engine.statistics.level))

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

		harddropscore /= 2
		engine.statistics.scoreHD += harddropscore
		harddropscore = 0

		// Line clear score
		var pts = 0
		// Level up

		// Update meter
		when {
			lines==1 -> {
				pts += 40*(engine.statistics.level+1) // Single
				loons += 1
			}
			lines==2 -> {
				pts += 100*(engine.statistics.level+1) // Double
				loons += 2
			}
			lines==3 -> {
				pts += 200*(engine.statistics.level+1) // Triple
				loons += 3
			}
			lines>=4 -> {
				pts += 300*(engine.statistics.level+1) // Four
				loons += 3
			}
		}

		// Do the ending (at 200 lines for now)
		if(gametype==RACE200&&loons>=200) {
			engine.ending = 1
			engine.gameEnded()
		}

		// Add score to total
		if(pts>0) {
			actions++
			lastscore = pts
			scgettime = 0
			engine.statistics.scoreLine += pts
		}

		efficiency = if(actions!=0) engine.statistics.lines/actions.toFloat() else 0f

		if(loons>=levellines) {
			// Level up
			engine.statistics.level++

			levellines += if(gametype==PRESSURE) 5 else 10

			owner.backgroundStatus.fadesw = true
			owner.backgroundStatus.fadecount = 0

			var lv = engine.statistics.level

			if(lv<0)
				lv = 0
			else if(lv>=19) lv = 19

			owner.backgroundStatus.fadebg = lv
			owner.bgmStatus.bgm = BGM.RETRO_A(maxOf(lv/4, 4))
			setSpeed(engine)
			engine.playSE("levelup")
		}

		// Update meter
		val togo = levellines-loons
		if(gametype==PRESSURE) {
			engine.meterValue = loons%5*receiver.getMeterMax(engine)/4
			when(togo) {
				1 -> engine.meterColor = GameEngine.METER_COLOR_RED
				2 -> engine.meterColor = GameEngine.METER_COLOR_ORANGE
				3 -> engine.meterColor = GameEngine.METER_COLOR_YELLOW
				else -> engine.meterColor = GameEngine.METER_COLOR_GREEN
			}
		} else if(engine.statistics.level==startlevel&&startlevel!=0) {
			engine.meterValue = loons*receiver.getMeterMax(engine)/(levellines-1)
			when {
				togo<=5 -> engine.meterColor = GameEngine.METER_COLOR_RED
				togo<=10 -> engine.meterColor = GameEngine.METER_COLOR_ORANGE
				togo<=20 -> engine.meterColor = GameEngine.METER_COLOR_YELLOW
				else -> engine.meterColor = GameEngine.METER_COLOR_GREEN
			}
		} else {
			engine.meterValue = (10-togo)*receiver.getMeterMax(engine)/9
			when {
				togo<=2 -> engine.meterColor = GameEngine.METER_COLOR_RED
				togo<=5 -> engine.meterColor = GameEngine.METER_COLOR_ORANGE
				togo<=8 -> engine.meterColor = GameEngine.METER_COLOR_YELLOW
				else -> engine.meterColor = GameEngine.METER_COLOR_GREEN
			}

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

		drawResultStats(engine, playerID, receiver, 3, COLOR.BLUE, Statistic.SCORE)

		receiver.drawMenuFont(engine, playerID, 0, 5, "Lines", COLOR.BLUE)
		val strLines = String.format("%10d", loons)
		receiver.drawMenuFont(engine, playerID, 0, 6, strLines)
		val strFour = String.format("%10s", String.format("+%d", engine.statistics.totalQuadruple))
		receiver.drawMenuFont(engine, playerID, 0, 7, strFour)

		drawResultStats(engine, playerID, receiver, 8, COLOR.BLUE, Statistic.LEVEL, Statistic.TIME)
		drawResult(engine, playerID, receiver, 12, COLOR.BLUE, "EFFICIENCY", String.format("%1.3f", efficiency))
		drawResultRank(engine, playerID, receiver, 14, COLOR.BLUE, rankingRank)
	}

	/** This function will be called when the replay data is going to be
	 * saved */
	override fun saveReplay(engine:GameEngine, playerID:Int, prop:CustomProperties) {
		saveSetting(prop)

		// Checks/Updates the ranking
		if(!owner.replayMode&&!big&&engine.ai==null) {
			updateRanking(engine.statistics.score, loons, engine.statistics.level, gametype)

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
		gametype = values()[prop.getProperty("retromastery.gametype", 0)]
		startlevel = prop.getProperty("retromastery.startlevel", 0)
		big = prop.getProperty("retromastery.big", false)
		version = prop.getProperty("retromastery.version", 0)
	}

	/** Save the settings
	 * @param prop CustomProperties
	 */
	override fun saveSetting(prop:CustomProperties) {
		prop.setProperty("retromastery.gametype", gametype.ordinal)
		prop.setProperty("retromastery.startlevel", startlevel)
		prop.setProperty("retromastery.big", big)
		prop.setProperty("retromastery.version", version)
	}

	/** Load the ranking
	 * @param prop CustomProperties
	 * @param ruleName Rule name
	 */
	override fun loadRanking(prop:CustomProperties, ruleName:String) {
		for(i in 0 until RANKING_MAX)
			for(gametypeIndex in 0 until RANKING_TYPE) {
				rankingScore[gametypeIndex][i] = prop.getProperty(
					"retromarathon.ranking.$ruleName.$gametypeIndex.score.$i", 0)
				rankingLines[gametypeIndex][i] = prop.getProperty(
					"retromarathon.ranking.$ruleName.$gametypeIndex.lines.$i", 0)
				rankingLevel[gametypeIndex][i] = prop.getProperty(
					"retromarathon.ranking.$ruleName.$gametypeIndex.level.$i", 0)
			}
	}

	/** Save the ranking
	 * @param prop CustomProperties
	 * @param ruleName Rule name
	 */
	fun saveRanking(prop:CustomProperties, ruleName:String) {
		for(i in 0 until RANKING_MAX)
			for(gametypeIndex in 0 until RANKING_TYPE) {
				prop.setProperty("retromarathon.ranking.$ruleName.$gametypeIndex.score.$i",
					rankingScore[gametypeIndex][i])
				prop.setProperty("retromarathon.ranking.$ruleName.$gametypeIndex.lines.$i",
					rankingLines[gametypeIndex][i])
				prop.setProperty("retromarathon.ranking.$ruleName.$gametypeIndex.level.$i",
					rankingLevel[gametypeIndex][i])
			}
	}

	/** Update the ranking
	 * @param sc Score
	 * @param li Lines
	 * @param lv Level
	 * @param type Game type
	 */
	private fun updateRanking(sc:Int, li:Int, lv:Int, type:GAMETYPE) {
		rankingRank = checkRanking(sc, li, lv, type)
		val t = type.ordinal
		if(rankingRank!=-1) {
			// Shift the ranking data
			for(i in RANKING_MAX-1 downTo rankingRank+1) {
				rankingScore[t][i] = rankingScore[t][i-1]
				rankingLines[t][i] = rankingLines[t][i-1]
				rankingLevel[t][i] = rankingLevel[t][i-1]
			}

			// Insert a new data
			rankingScore[t][rankingRank] = sc
			rankingLines[t][rankingRank] = li
			rankingLevel[t][rankingRank] = lv
		}
	}

	/** This function will check the ranking and returns which place you are.
	 * (-1: Out of rank)
	 * @param sc Score
	 * @param li Lines
	 * @param lv Level
	 * @return Place (First place is 0. -1 is Out of Rank)
	 */
	private fun checkRanking(sc:Int, li:Int, lv:Int, type:GAMETYPE):Int {
		val t = type.ordinal
		for(i in 0 until RANKING_MAX)
			if(sc>rankingScore[t][i])
				return i
			else if(sc==rankingScore[t][i]&&li>rankingLines[t][i])
				return i
			else if(sc==rankingScore[t][i]&&li==rankingLines[t][i]&&lv<rankingLevel[t][i]) return i

		return -1
	}

	companion object {
		/** Current version of this mode */
		private const val CURRENT_VERSION = 1

		/** Denominator table */
		private val tableDenominator = intArrayOf(
			//	0  1  2  3  4  5  6  7  8  9    +xx
			48, 40, 32, 27, 22, 18, 15, 12, 10, 8, // 00
			7, 6, 11, 5, 9, 4, 7, 3, 11, 10, // 10
			9, 8, 15, 14, 13, 12, 11, 10, 9, 8, // 20
			1)

		/** Gravity table */
		private val tableGravity = intArrayOf(
			//	0  1  2  3  4  5  6  7  8  9    +xx
			1, 1, 1, 1, 1, 1, 1, 1, 1, 1, // 00
			1, 1, 2, 1, 2, 1, 2, 1, 4, 4, // 10
			4, 4, 8, 8, 8, 8, 8, 8, 8, 8, // 20
			1)

		/** Lock delay table */
		private val tableLockDelay = intArrayOf(
			//	0  1  2  3  4  5  6  7  8  9    +xx
			60, 52, 45, 39, 34, 30, 27, 24, 22, 20, // 00
			19, 18, 17, 16, 15, 14, 13, 12, 11, 10, // 10
			9, 8, 8, 8, 8, 7, 7, 7, 7, 7, // 20
			6)

		/** Game type name */
		private enum class GAMETYPE {
			RACE200, ENDLESS, PRESSURE
		}

		/** Number of ranking records */
		private const val RANKING_MAX = 10

		/** Number of ranking types */
		private val RANKING_TYPE:Int
			get() = values().size

	}
}
