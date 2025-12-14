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
package mu.nu.nullpo.game.subsystem.mode

import mu.nu.nullpo.game.component.BGM
import mu.nu.nullpo.game.component.LevelData
import mu.nu.nullpo.game.event.EventReceiver.COLOR
import mu.nu.nullpo.game.event.Leaderboard
import mu.nu.nullpo.game.event.Rankable
import mu.nu.nullpo.game.event.ScoreEvent
import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.game.subsystem.mode.menu.*
import mu.nu.nullpo.gui.common.BaseFont.FONT.*
import mu.nu.nullpo.util.CustomProperties
import mu.nu.nullpo.util.GeneralUtil.toTimeStr

/** RETRO MASTERY mode by Pineapple 20100722 - 20100808 */
class RetroA:AbstractMode() {
	/** GameManager object (Manages entire game status) */

	/** EventReceiver object (This receives many game events, can also be used
	 * for drawing the fonts.) */

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

	private val itemMode = StringsMenuItem(
		"mode", "GAME TYPE", COLOR.BLUE,
		GAMETYPE.RACE200.ordinal, GAMETYPE.entries.map {it.name})
	private var gameMode:Int by DelegateMenuItem(itemMode)
	/** Selected game type */
	private val gameType get() = GAMETYPE.entries[gameMode]

	private val itemLevel = LevelMenuItem("startlevel", "Level", COLOR.BLUE, 0, 0..19)
	/** Selected starting level */
	private var startLevel:Int by DelegateMenuItem(itemLevel)

	private val itemBig = BooleanMenuItem("big", "BIG", COLOR.BLUE, false)
	/** BigMode */
	private var big:Boolean by DelegateMenuItem(itemBig)
	override val menu = MenuList("retromastery", itemMode, itemLevel, itemBig)
	/** Version of this mode */
	private var version = 0

	/** Your place on leaderboard (-1: out of rank) */
	private var rankingRank = 0

	override val ranking=
		List(RANKING_TYPE) {Leaderboard(rankingMax,kotlinx.serialization.serializer<List<Rankable.ScoreRow>>())}

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

		if(!owner.replayMode) version = CURRENT_VERSION
		engine.run {
			twistEnable = false
			b2bEnable = false
			splitB2B = false
			comboType = GameEngine.COMBO_TYPE_DISABLE
			bigHalf = true
			bigMove = true

			speed.are = 12
			speed.areLine = 15
			speed.das = 12
			ruleOpt.lockResetMove = false
			ruleOpt.lockResetSpin = false
			ruleOpt.lockResetWallkick = false
			ruleOpt.lockResetFall = true
			ruleOpt.softdropLock = true
			ruleOpt.nextDisplay = 1
//			ruleOpt.softdropMultiplyNativeSpeed = false
//			ruleOpt.softdropGravitySpeedLimit = false
			owSDSpd = 1

			owner.bgMan.bg = if(gameType==GAMETYPE.PRESSURE) 0 else minOf(startLevel, 19)
			frameSkin = GameEngine.FRAME_SKIN_GB
		}
	}

	/** Set the gravity speed
	 * @param engine GameEngine object
	 */
	override fun setSpeed(engine:GameEngine) {
		val lv = engine.statistics.level.coerceIn(0, tableSpeed.size-1)
		engine.speed.replace(tableSpeed[lv])
		//engine.speed.lineDelay = if(lv>=10) 20 else 25
	}

	/** Main routine for game setup screen */
	override fun onSettingChanged(engine:GameEngine) {

		engine.owner.bgMan.bg = if(gameType==GAMETYPE.PRESSURE) 0 else startLevel
		super.onSettingChanged(engine)
	}

	/** This function will be called before the game actually begins (after
	 * Ready&Go screen disappears) */
	override fun startGame(engine:GameEngine) {
		engine.big = big
		engine.statistics.levelDispAdd = 1
		engine.owSDSpd = 1

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
		receiver.drawScore(engine, 0, 0, name, BASE, COLOR.GREEN)
		receiver.drawScore(engine, 0, 1, "(${gameType.name})", BASE, COLOR.GREEN)

		if(engine.stat==GameEngine.Status.SETTING||engine.stat==GameEngine.Status.RESULT&&!owner.replayMode) {
			if(!owner.replayMode&&!big&&engine.ai==null) {
				receiver.drawScore(engine, 3, 3, "SCORE    LINE LV.", BASE, COLOR.BLUE)

				ranking[gameType.ordinal].forEachIndexed { i, it ->
					receiver.drawScore(
						engine, 0, 4+i, "%2d".format(i+1), GRADE, if(rankingRank==i) COLOR.RAINBOW else COLOR.YELLOW
					)
					receiver.drawScore(engine, 3, 4+i, "${it.sc}", NUM, i==rankingRank)
					receiver.drawScore(engine, 12, 4+i, "${it.li}", NUM, i==rankingRank)
					receiver.drawScore(engine, 17, 4+i, "%02d".format(it.lv), NUM, i==rankingRank)
				}
			}
		} else {
			receiver.drawScore(engine, 0, 3, "Score", BASE, COLOR.BLUE)
			receiver.drawScore(engine, 6, 3, "(+$lastScore)", BASE)
			receiver.drawScore(engine, 0, 4, "$scDisp", NUM, 2f)

			val strLine = "$loons"

			receiver.drawScore(engine, 0, 6, "Lines", BASE, COLOR.BLUE)
			receiver.drawScore(engine, 0, 7, strLine, NUM, 2f)

			receiver.drawScore(engine, 0, 9, "Level", BASE, COLOR.BLUE)
			receiver.drawScore(engine, 0, 10, "%02d".format(engine.statistics.level), BASE)

			receiver.drawScore(engine, 0, 12, "Time", BASE, COLOR.BLUE)
			receiver.drawScore(engine, 0, 13, engine.statistics.time.toTimeStr, NUM_T)
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

			val lv = engine.statistics.level.coerceIn(0, 19)

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
		receiver.drawMenu(engine, 0, 1, "PLAY DATA", BASE, COLOR.ORANGE)

		drawResultStats(engine, receiver, 3, COLOR.BLUE, Statistic.SCORE)

		receiver.drawMenu(engine, 0, 5, "Lines", BASE, COLOR.BLUE)
		val strLines = "%10d".format(loons)
		receiver.drawMenu(engine, 0, 6, strLines, BASE)
		val strFour = "%10s".format("+%d".format(engine.statistics.totalQuadruple))
		receiver.drawMenu(engine, 0, 7, strFour, BASE)

		drawResultStats(engine, receiver, 8, COLOR.BLUE, Statistic.LEVEL, Statistic.TIME)
		drawResult(engine, receiver, 12, COLOR.BLUE, "EFFICIENCY", "%1.3f".format(efficiency))
		drawResultRank(engine, receiver, 14, COLOR.BLUE, rankingRank)
	}

	/** This function will be called when the replay data is going to be saved */
	override fun saveReplay(engine:GameEngine, prop:CustomProperties):Boolean {
		saveSetting(engine, prop)

		// Checks/Updates the ranking
		if(!owner.replayMode&&!big&&engine.ai==null) {
//			updateRanking(engine.statistics.score, loons, engine.statistics.level, gameType)
			rankingRank=ranking[gameType.ordinal].add(Rankable.ScoreRow(engine.statistics))
			if(rankingRank!=-1) return true
		}
		return false
	}

	companion object {
		/** Current version of this mode */
		private const val CURRENT_VERSION = 1

		/** Denominator table */
		private val tableSpeed = LevelData(
			listOf(
				//0,1 2, 3, 4, 5, 6, 7, 8, 9,    +xx
				1, 1, 1, 1, 1, 1, 1, 1, 1, 1, // 00
				1, 1, 2, 1, 2, 1, 2, 1, 4, 4, // 10
				4, 4, 8, 8, 8, 8, 8, 8, 8, 8, // 20
				1
			),
			listOf(
				//0, 1   2,  3,  4,  5,  6,  7,  8,  9    +xx
				48, 40, 32, 27, 22, 18, 15, 12, 10, 8, // 00
				+7, +6, 11, +5, +9, +4, +7, +3, 11, 10, // 10
				+9, +8, 15, 14, 13, 12, 11, 10, +9, 8, // 20
				1
			),
			listOf(12),
			listOf(15),
			listOf(
				25, 25, 25, 25, 25, 25, 25, 25, 25, 25,
				20
			),
			listOf(
				//0, 1   2,  3,  4,  5,  6,  7,  8,  9    +xx
				60, 52, 45, 39, 34, 30, 27, 24, 22, 20, // 00
				19, 18, 17, 16, 15, 14, 13, 12, 11, 10, // 10
				9, 8, 8, 8, 8, 7, 7, 7, 7, 7, // 20
				6
			),
			listOf(12),
		)

		/** Game type name */
		private enum class GAMETYPE {
			RACE200, ENDLESS, PRESSURE;
		}

		/** Number of ranking types */
		private val RANKING_TYPE:Int = GAMETYPE.entries.size
	}
}
