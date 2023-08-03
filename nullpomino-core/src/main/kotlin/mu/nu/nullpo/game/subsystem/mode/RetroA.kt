/*
 * Copyright (c) 2010-2023, NullNoname
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
import mu.nu.nullpo.game.component.Controller
import mu.nu.nullpo.game.event.EventReceiver.COLOR
import mu.nu.nullpo.game.event.ScoreEvent
import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.game.subsystem.mode.menu.BooleanMenuItem
import mu.nu.nullpo.game.subsystem.mode.menu.DelegateMenuItem
import mu.nu.nullpo.gui.common.BaseFont
import mu.nu.nullpo.util.CustomProperties
import mu.nu.nullpo.util.GeneralUtil.getONorOFF
import mu.nu.nullpo.util.GeneralUtil.toTimeStr

/** RETRO MASTERY mode by Pineapple 20100722 - 20100808 */
class RetroA:AbstractMode() {
	/** GameManager object (Manages entire game status) */

	/** EventReceiver object (This receives many game events, can also be used
	 * for drawing the fonts.) */

	/** Selected game type */
	private var gameType:GAMETYPE = GAMETYPE.RACE200

	/** Selected starting level */
	private var startLevel = 0

	/** Used for soft drop scoring */
	private var scoreSD = 0

	/** Used for hard drop scoring */
	private var scoreHD = 0

	/** Number of "lines" cleared (most things use this instead of
	 * engine.statistics.lines); don't ask me why I called it this... */
	private var loons = 0

	/** Number of line clear actions */
	private var actions = 0

	/** Efficiency (engine.statistics.lines / actions) */
	private var efficiency = 0f

	/** Next level lines */
	private var levelLines = 0

	private val itemBig = BooleanMenuItem("big", "BIG", COLOR.BLUE, false)
	/** BigMode */
	private var big:Boolean by DelegateMenuItem(itemBig)

	/** Version of this mode */
	private var version = 0

	/** Your place on leaderboard (-1: out of rank) */
	private var rankingRank = 0

	/** Score records */
	private val rankingScore = List(RANKING_TYPE) {MutableList(RANKING_MAX) {0L}}

	/** Line records */
	private val rankingLines = List(RANKING_TYPE) {MutableList(RANKING_MAX) {0}}

	/** Level records */
	private val rankingLevel = List(RANKING_TYPE) {MutableList(RANKING_MAX) {0}}

	override val rankMap
		get() = rankMapOf(
			rankingScore.mapIndexed {a, x -> "$a.score" to x}+
				rankingLines.mapIndexed {a, x -> "$a.lines" to x}+
				rankingLevel.mapIndexed {a, x -> "$a.level" to x})

	/** Returns the name of this mode */
	override val name = "Retro Marathon.A"

	/** This function will be called when the game enters the main game
	 * screen. */
	override fun playerInit(engine:GameEngine) {
		super.playerInit(engine)
		lastScore = 0
		scoreSD = 0
		scoreHD = 0
		levelLines = 0
		loons = 0
		actions = 0
		efficiency = 0f

		rankingRank = -1
		rankingScore.forEach {it.fill(0)}
		rankingLines.forEach {it.fill(0)}
		rankingLevel.forEach {it.fill(0)}

		engine.twistEnable = false
		engine.b2bEnable = false
		engine.splitB2B = false
		engine.comboType = GameEngine.COMBO_TYPE_DISABLE
		engine.bigHalf = true
		engine.bigMove = true

		engine.speed.are = 12
		engine.speed.areLine = 15
		engine.speed.das = 12
		engine.ruleOpt.lockResetMove = false
		engine.ruleOpt.lockResetSpin = false
		engine.ruleOpt.lockResetWallkick = false
		engine.ruleOpt.lockResetFall = true
		engine.ruleOpt.softdropLock = true
		engine.ruleOpt.softdropMultiplyNativeSpeed = false
		engine.ruleOpt.softdropGravitySpeedLimit = false
		engine.ruleOpt.softdropSpeed = .5f
		engine.owSDSpd = -1
		if(!owner.replayMode) version = CURRENT_VERSION

		engine.owner.bgMan.bg = if(gameType==GAMETYPE.PRESSURE) 0 else startLevel
		if(engine.owner.bgMan.bg>19) engine.owner.bgMan.bg = 19
		engine.frameColor = GameEngine.FRAME_COLOR_GRAY
	}

	/** Set the gravity speed
	 * @param engine GameEngine object
	 */
	private fun setSpeed(engine:GameEngine) {
		val lv = maxOf(0, minOf(engine.statistics.level, tableDenominator.size-1))

		engine.speed.gravity = tableGravity[lv]
		engine.speed.denominator = tableDenominator[lv]
		engine.speed.lockDelay = tableLockDelay[lv]
		engine.speed.lineDelay = if(lv>=10) 20 else 25
	}

	/** Main routine for game setup screen */
	override fun onSetting(engine:GameEngine):Boolean {
		// Menu
		if(!engine.owner.replayMode) {
			// Check for UP button, when pressed it will move cursor up.
			if(engine.ctrl.isMenuRepeatKey(Controller.BUTTON_UP)) {
				menuCursor--
				if(menuCursor==1&&gameType==GAMETYPE.PRESSURE) menuCursor--
				if(menuCursor<0) menuCursor = 2
				engine.playSE("cursor")
			}
			// Check for DOWN button, when pressed it will move cursor down.
			if(engine.ctrl.isMenuRepeatKey(Controller.BUTTON_DOWN)) {
				menuCursor++
				if(menuCursor==1&&gameType==GAMETYPE.PRESSURE) menuCursor++
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
						gameType = when(gameType) {
							GAMETYPE.entries.first() -> GAMETYPE.entries.last()
							GAMETYPE.entries.last() -> GAMETYPE.entries.first()
							else -> GAMETYPE.entries[gameType.ordinal+change]
						}
						engine.owner.bgMan.bg = if(gameType==GAMETYPE.PRESSURE) 0 else startLevel
					}
					1 -> {
						startLevel += change
						if(startLevel<0) startLevel = 19
						if(startLevel>19) startLevel = 0
						engine.owner.bgMan.bg = startLevel
					}
					2 -> big = !big
				}
			}

			// Check for A button, when pressed this will begin the game
			if(menuTime<5) menuTime++ else if(engine.ctrl.isPush(Controller.BUTTON_A)) {
				engine.playSE("decide")
				return false
			}

			// Check for B button, when pressed this will shut down the game engine.
			if(engine.ctrl.isPush(Controller.BUTTON_B)) engine.quitFlag = true
		} else {
			menuTime++
			menuCursor = -1

			return menuTime<60
		}

		return true
	}

	/** Renders game setup screen */
	override fun renderSetting(engine:GameEngine) {
		if(!engine.owner.replayMode)
			receiver.drawMenuFont(engine, 0, menuCursor*2+1, BaseFont.CURSOR, COLOR.RED)

		receiver.drawMenuFont(engine, 0, 0, "GAME TYPE", COLOR.BLUE)
		receiver.drawMenuFont(engine, 1, 1, gameType.name, menuCursor==0)
		if(gameType!=GAMETYPE.ENDLESS) {
			receiver.drawMenuFont(engine, 0, 2, "Level", COLOR.BLUE)
			receiver.drawMenuFont(engine, 1, 3, "%02d".format(startLevel), menuCursor==1)
		}
		receiver.drawMenuFont(engine, 0, 4, "BIG", COLOR.BLUE)
		receiver.drawMenuFont(engine, 1, 5, big.getONorOFF(), menuCursor==2)
	}

	/** This function will be called before the game actually begins (after
	 * Ready&Go screen disappears) */
	override fun startGame(engine:GameEngine) {
		engine.big = big
		engine.statistics.levelDispAdd = 1

		owner.musMan.bgm = BGM.RetroA(0)
		when(gameType) {
			GAMETYPE.PRESSURE -> {
				engine.statistics.level = 0
				levelLines = 5
			}
			GAMETYPE.RACE200 -> {
				engine.statistics.level = startLevel
				levelLines = 10*minOf(startLevel+1, 10)
			}
			GAMETYPE.ENDLESS -> {
				engine.statistics.level = startLevel
				levelLines = if(startLevel<=9) (startLevel+1)*10 else (startLevel+11)*5
			}
		}

		setSpeed(engine)
	}

	/** Renders HUD (leaderboard or game statistics) */
	override fun renderLast(engine:GameEngine) {
		receiver.drawScoreFont(engine, 0, 0, name, COLOR.GREEN)
		receiver.drawScoreFont(engine, 0, 1, "(${gameType.name})", COLOR.GREEN)

		if(engine.stat==GameEngine.Status.SETTING||engine.stat==GameEngine.Status.RESULT&&!owner.replayMode) {
			if(!owner.replayMode&&!big&&engine.ai==null) {
				receiver.drawScoreFont(engine, 3, 3, "SCORE    LINE LV.", COLOR.BLUE)

				for(i in 0..<RANKING_MAX) {
					receiver.drawScoreGrade(
						engine, 0, 4+i, "%2d".format(i+1), if(rankingRank==i) COLOR.RAINBOW else COLOR.YELLOW
					)
					receiver.drawScoreNum(engine, 3, 4+i, "${rankingScore[gameType.ordinal][i]}", i==rankingRank)
					receiver.drawScoreNum(engine, 12, 4+i, "${rankingLines[gameType.ordinal][i]}", i==rankingRank)
					receiver.drawScoreNum(
						engine, 17, 4+i, "%02d".format(rankingLevel[gameType.ordinal][i]), i==rankingRank
					)
				}
			}
		} else {
			receiver.drawScoreFont(engine, 0, 3, "Score", COLOR.BLUE)
			receiver.drawScoreFont(engine, 6, 3, "(+$lastScore)")
			receiver.drawScoreNum(engine, 0, 4, "$scDisp", 2f)

			val strLine = "$loons"

			receiver.drawScoreFont(engine, 0, 6, "Lines", COLOR.BLUE)
			receiver.drawScoreNum(engine, 0, 7, strLine, 2f)

			receiver.drawScoreFont(engine, 0, 9, "Level", COLOR.BLUE)
			receiver.drawScoreFont(engine, 0, 10, "%02d".format(engine.statistics.level))

			receiver.drawScoreFont(engine, 0, 12, "Time", COLOR.BLUE)
			receiver.drawScoreNum(engine, 0, 13, engine.statistics.time.toTimeStr, 2f)
		}
	}

	override fun calcScore(engine:GameEngine, ev:ScoreEvent):Int {
		scoreSD /= 2
		engine.statistics.scoreSD += scoreSD
		scoreSD = 0

		scoreHD /= 2
		engine.statistics.scoreHD += scoreHD
		scoreHD = 0

		// Line clear score
		var pts = 0
		// Level up

		// Update meter
		val li = ev.lines
		when {
			li==1 -> {
				pts += 40*(engine.statistics.level+1) // Single
				loons += 1
			}
			li==2 -> {
				pts += 100*(engine.statistics.level+1) // Double
				loons += 2
			}
			li==3 -> {
				pts += 200*(engine.statistics.level+1) // Triple
				loons += 3
			}
			li>=4 -> {
				pts += 300*(engine.statistics.level+1) // Four
				loons += 3
			}
		}

		// Do the ending (at 200 lines for now)
		if(gameType==GAMETYPE.RACE200&&loons>=200) {
			engine.ending = 1
			engine.gameEnded()
		}

		// Add score to total
		if(pts>0) {
			actions++
			lastScore = pts
			engine.statistics.scoreLine += pts
		}

		efficiency = if(actions!=0) engine.statistics.lines/actions.toFloat() else 0f

		if(loons>=levelLines) {
			// Level up
			engine.statistics.level++

			levelLines += if(gameType==GAMETYPE.PRESSURE) 5 else 10

			val lv = maxOf(0, minOf(engine.statistics.level, 19))

			owner.bgMan.nextBg = lv
			owner.musMan.bgm = BGM.RetroA(maxOf(lv/4, 4))
			setSpeed(engine)
			engine.playSE("levelup")
		}

		// Update meter
		val togo = levelLines-loons
		engine.meterColor = GameEngine.METER_COLOR_LEVEL
		engine.meterValue =
			if(gameType==GAMETYPE.PRESSURE) loons%5/4f
			else if(engine.statistics.level==startLevel&&startLevel!=0) loons/(levelLines-1f)
			else (10-togo)/9f
		return pts
	}

	/** This function will be called when soft-drop is used */
	override fun afterSoftDropFall(engine:GameEngine, fall:Int) {
		scoreSD += fall
	}

	/** This function will be called when hard-drop is used */
	override fun afterHardDropFall(engine:GameEngine, fall:Int) {
		scoreHD += fall
	}

	/** Renders game result screen */
	override fun renderResult(engine:GameEngine) {
		receiver.drawMenuFont(engine, 0, 1, "PLAY DATA", COLOR.ORANGE)

		drawResultStats(engine, receiver, 3, COLOR.BLUE, Statistic.SCORE)

		receiver.drawMenuFont(engine, 0, 5, "Lines", COLOR.BLUE)
		val strLines = "%10d".format(loons)
		receiver.drawMenuFont(engine, 0, 6, strLines)
		val strFour = "%10s".format("+%d".format(engine.statistics.totalQuadruple))
		receiver.drawMenuFont(engine, 0, 7, strFour)

		drawResultStats(engine, receiver, 8, COLOR.BLUE, Statistic.LEVEL, Statistic.TIME)
		drawResult(engine, receiver, 12, COLOR.BLUE, "EFFICIENCY", "%1.3f".format(efficiency))
		drawResultRank(engine, receiver, 14, COLOR.BLUE, rankingRank)
	}

	/** This function will be called when the replay data is going to be
	 * saved */
	override fun saveReplay(engine:GameEngine, prop:CustomProperties):Boolean {
		saveSetting(engine, prop)

		// Checks/Updates the ranking
		if(!owner.replayMode&&!big&&engine.ai==null) {
			updateRanking(engine.statistics.score, loons, engine.statistics.level, gameType)

			if(rankingRank!=-1) return true
		}
		return false
	}

	/** Load the settings from [prop] */
	override fun loadSetting(engine:GameEngine, prop:CustomProperties, ruleName:String, playerID:Int) {
		gameType = GAMETYPE.entries[prop.getProperty("retromastery.gametype", 0)]
		startLevel = prop.getProperty("retromastery.startLevel", 0)
		big = prop.getProperty("retromastery.big", false)
		version = prop.getProperty("retromastery.version", 0)
	}

	/** Save the settings
	 * @param prop CustomProperties
	 */
	override fun saveSetting(engine:GameEngine, prop:CustomProperties, ruleName:String, playerID:Int) {
		prop.setProperty("retromastery.gametype", gameType.ordinal)
		prop.setProperty("retromastery.startLevel", startLevel)
		prop.setProperty("retromastery.big", big)
		prop.setProperty("retromastery.version", version)
	}

	/** Update the ranking
	 * @param sc Score
	 * @param li Lines
	 * @param lv Level
	 * @param type Game type
	 */
	private fun updateRanking(sc:Long, li:Int, lv:Int, type:GAMETYPE) {
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
	private fun checkRanking(sc:Long, li:Int, lv:Int, type:GAMETYPE):Int {
		val t = type.ordinal
		for(i in 0..<RANKING_MAX)
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
		private val tableDenominator = listOf(
			//	0  1  2  3  4  5  6  7  8  9    +xx
			48, 40, 32, 27, 22, 18, 15, 12, 10, 8, // 00
			7, 6, 11, 5, 9, 4, 7, 3, 11, 10, // 10
			9, 8, 15, 14, 13, 12, 11, 10, 9, 8, // 20
			1
		)

		/** Gravity table */
		private val tableGravity = listOf(
			//	0  1  2  3  4  5  6  7  8  9    +xx
			1, 1, 1, 1, 1, 1, 1, 1, 1, 1, // 00
			1, 1, 2, 1, 2, 1, 2, 1, 4, 4, // 10
			4, 4, 8, 8, 8, 8, 8, 8, 8, 8, // 20
			1
		)

		/** Lock delay table */
		private val tableLockDelay = listOf(
			//	0  1  2  3  4  5  6  7  8  9    +xx
			60, 52, 45, 39, 34, 30, 27, 24, 22, 20, // 00
			19, 18, 17, 16, 15, 14, 13, 12, 11, 10, // 10
			9, 8, 8, 8, 8, 7, 7, 7, 7, 7, // 20
			6
		)

		/** Game type name */
		private enum class GAMETYPE {
			RACE200, ENDLESS, PRESSURE;
		}

		/** Number of ranking records */
		private const val RANKING_MAX = 13

		/** Number of ranking types */
		private val RANKING_TYPE:Int = GAMETYPE.entries.size
	}
}
