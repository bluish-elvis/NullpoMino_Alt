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

import mu.nu.nullpo.game.component.BGMStatus.BGM
import mu.nu.nullpo.game.component.Block
import mu.nu.nullpo.game.component.Controller
import mu.nu.nullpo.game.component.Piece
import mu.nu.nullpo.game.event.EventReceiver.COLOR
import mu.nu.nullpo.game.event.ScoreEvent
import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.game.subsystem.mode.menu.BooleanMenuItem
import mu.nu.nullpo.game.subsystem.mode.menu.DelegateMenuItem
import mu.nu.nullpo.game.subsystem.mode.menu.IntegerMenuItem
import mu.nu.nullpo.game.subsystem.mode.menu.LevelMenuItem
import mu.nu.nullpo.game.subsystem.mode.menu.MenuList
import mu.nu.nullpo.game.subsystem.mode.menu.StringsMenuItem
import mu.nu.nullpo.gui.common.BaseFont
import mu.nu.nullpo.gui.common.BaseFont.FONT
import mu.nu.nullpo.util.CustomProperties
import mu.nu.nullpo.util.GeneralUtil
import mu.nu.nullpo.util.GeneralUtil.toTimeStr
import net.omegaboshi.nullpomino.game.subsystem.randomizer.NintendoRandomizer

/** RETRO CLASSIC mode (Based from NES, Original from NullpoUE build 010210 by Zircean) */
class RetroN:AbstractMode() {
	/** Used for soft drop scoring */
	private var sdScore = 0

	/** Used for hard drop scoring */
	private var hdScore = 0

	/** Next level lines */
	private var lvLines = 0

	private val itemGame = StringsMenuItem("gametype", "GAME TYPE", COLOR.BLUE, 0, GAMETYPE_NAME)
	/** Selected game type */
	private var gameType:Int by DelegateMenuItem(itemGame)

	private val itemSpeed = StringsMenuItem("speedtype", "DIFFICULTY", COLOR.BLUE, 0, SPEED_NAME)
	/** Selected Speed Difficulty */
	private var speedType:Int by DelegateMenuItem(itemSpeed)

	private val itemLevel = LevelMenuItem("startlevel", "LEVEL", COLOR.BLUE, 0, 0..19, false, true)
	/** Selected starting level */
	private var startLevel:Int by DelegateMenuItem(itemLevel)

	private val itemHeight = IntegerMenuItem("height", "HEIGHT", COLOR.BLUE, 0, 0..5)
	/** Selected garbage height */
	private var startHeight:Int by DelegateMenuItem(itemHeight)

	private val itemBig = BooleanMenuItem("big", "BIG", COLOR.BLUE, false)
	/** Big mode on/off */
	private var big:Boolean by DelegateMenuItem(itemBig)

	override val menu = MenuList("retromarathon", itemGame, itemSpeed, itemLevel, itemHeight, itemBig)

	/** Max interval of I-piece */
	private var drought = 0
	private val droughts:MutableList<Int> = mutableListOf()

	/** Your place on leaderboard (-1: out of rank) */
	private var rankingRank = 0

	/** Score records */
	private val rankingScore = List(RANKING_TYPE) {MutableList(RANKING_MAX) {0L}}

	/** Line records */
	private val rankingLines = List(RANKING_TYPE) {MutableList(RANKING_MAX) {0}}

	/** Level records */
	private val rankingLevel = List(RANKING_TYPE) {MutableList(RANKING_MAX) {0}}

	/** Time records Reaches Score Max-out 999999 */
	private var maxScoredTime:Int? = null

	/** Returns the name of this mode */
	override val name = "Retro Classic .N"

	override val gameIntensity = -1
	override val rankMap
		get() = rankMapOf(
			rankingScore.mapIndexed {a, x -> "$a.score" to x}+
				rankingLines.mapIndexed {a, x -> "$a.lines" to x}+
				rankingLevel.mapIndexed {a, x -> "$a.level" to x})

	/** This function will be called when the game enters the main game
	 * screen. */
	override fun playerInit(engine:GameEngine) {
		super.playerInit(engine)
		lastScore = 0
		sdScore = 0
		hdScore = 0
		lvLines = 0
		drought = 0
		droughts.removeAll {true}

		rankingRank = -1
		rankingScore.forEach {it.fill(0)}
		rankingLines.forEach {it.fill(0)}
		rankingLevel.forEach {it.fill(0)}

		engine.run {
			twistEnable = false
			b2bEnable = false
			splitB2B = false
			comboType = GameEngine.COMBO_TYPE_DISABLE
			bigHalf = false
			bigMove = false
			speed.are = 10
			speed.areLine = 20
			speed.lineDelay = 20
			owMinDAS = -1
			owMaxDAS = -1
			owSDSpd = 1
			ruleOpt.nextDisplay = 1
			ruleOpt.harddropEnable = false

			owSkin = if(speedType==2!=startLevel>=10) 8 else 9
			owDelayCancel = 0
		}

		owner.bgMan.bg = startLevel
		if(owner.bgMan.bg>19) owner.bgMan.bg = 19
		lvLines = minOf((startLevel+1)*10, maxOf(100, (startLevel-5)*10))
		engine.frameColor = GameEngine.FRAME_SKIN_GB
	}

	/** Set the gravity speed
	 * @param engine GameEngine object
	 */
	private fun setSpeed(engine:GameEngine) {
		var lv = engine.statistics.level

		engine.owSkin = if(speedType==2!=lv>=10) 8 else 9
		if(speedType>0) {
			lv = maxOf(0, minOf(lv, tableDenominatorHard.size-1))

			engine.speed.gravity = if(speedType>1) 60000 else 50007
			engine.speed.denominator = tableDenominatorHard[lv]*60000
			engine.speed.das = 12
			engine.owARR = 4
		} else {
			lv = maxOf(0, minOf(lv, tableDenominator.size-1))

			engine.speed.gravity = 601
			engine.speed.denominator = tableDenominator[lv]*600
			engine.speed.das = 16
			engine.owARR = 6
		}
		engine.speed.lockDelay = engine.speed.denominator/engine.speed.gravity
	}

	/** Main routine for game setup screen */
	override fun onSettingChanged(engine:GameEngine) {
		lvLines = minOf((startLevel+1)*10, maxOf(100, (startLevel-5)*10))
		super.onSettingChanged(engine)
	}

	override fun onReady(engine:GameEngine):Boolean {
		if(engine.statc[0]==0) {
			engine.run {
				randomizer = NintendoRandomizer()
				ruleOpt.run {
					spinInitial = false
					lockResetMove = false
					softdropLock = true
					softdropMultiplyNativeSpeed = false
					softdropGravitySpeedLimit = false
					holdEnable = false
					dasInARE = false
					dasInReady = false
					dasChargeOnBlockedMove = true
					dasStoreChargeOnNeutral = true
					dasRedirectInDelay = true
				}
				createFieldIfNeeded()
			}
			fillGarbage(engine, startHeight)
			lvLines = minOf((startLevel+1)*10, maxOf(100, (startLevel-5)*10))
		}
		return false
	}

	/** This function will be called before the game actually begins
	 * (after Ready&Go screen disappears) */
	override fun startGame(engine:GameEngine) {
		engine.statistics.level = startLevel
		engine.statistics.levelDispAdd = 1
		engine.big = big

		owner.musMan.bgm = BGM.RetroN((gameType+startLevel)%3)
		setSpeed(engine)
	}

	override fun onMove(engine:GameEngine):Boolean {
		// 新規ピース出現時
		if(engine.ending==0&&engine.statc[0]==0&&!engine.holdDisable)
			if(engine.run {getNextObjectCopy(nextPieceCount)}?.type!=Piece.Shape.I) drought++
			else {
				if(drought>7) droughts += drought
				drought = 0
			}

		return false
	}

	/** Renders HUD (leaderboard or game statistics) */
	override fun renderLast(engine:GameEngine) {
		receiver.drawScoreFont(engine, 0, 0, name, COLOR.GREEN)
		receiver.drawScoreFont(engine, 0, 1, "(${SPEED_NAME[speedType]} ${GAMETYPE_NAME[gameType]})", COLOR.GREEN)

		if(engine.stat==GameEngine.Status.SETTING||engine.stat==GameEngine.Status.RESULT&&!owner.replayMode) {
			if(!owner.replayMode&&!big&&engine.ai==null) {
				receiver.drawScoreFont(engine, 3, 3, "SCORE    LINE LV.", COLOR.BLUE)

				for(i in 0..<RANKING_MAX) {
					receiver.drawScoreGrade(
						engine, 0, 4+i, "%2d".format(i+1), if(rankingRank==i) COLOR.RAINBOW else COLOR.YELLOW
					)
					receiver.drawScoreNum(engine, 3, 4+i, GeneralUtil.capsNum(rankingScore[gameType][i], 6), i==rankingRank)
					receiver.drawScoreNum(
						engine, 12, 4+i, GeneralUtil.capsNum(rankingLines[gameType][i], 3), i==rankingRank
					)
					receiver.drawScore(
						engine,
						17,
						4+i,
						LEVEL_NAME[rankingLevel[gameType][i]],
						font = if(rankingLevel[gameType][i]<30) FONT.NUM else FONT.NORMAL,
						flag = i==rankingRank
					)
				}
			}
		} else {
			receiver.drawScoreFont(engine, 0, 3, "SCORE${if(lastScore>0) "(+$lastScore)" else ""}", COLOR.BLUE)
			receiver.drawScore(
				engine, 0, 4, GeneralUtil.capsNum(scDisp, 6), font = if(engine.statistics.score<=999999) FONT.NUM else FONT.NORMAL,
				scale = 2f
			)

			receiver.drawScoreFont(engine, 0, 6, "LINE", COLOR.BLUE)
			receiver.drawScore(
				engine, 0, 7, when(gameType) {
					GAMETYPE_TYPE_B -> "-%2d".format(maxOf(25-engine.statistics.lines, 0))
					else -> GeneralUtil.capsNum(engine.statistics.lines, 3)
				}, if(gameType!=GAMETYPE_TYPE_B&&engine.statistics.lines<999) FONT.NUM else FONT.NORMAL, scale = 2f
			)

			receiver.drawScoreFont(engine, 0, 9, "Level", COLOR.BLUE)
			receiver.drawScore(
				engine, 0, 10, LEVEL_NAME[engine.statistics.level], if(engine.statistics.level<30) FONT.NUM else FONT.NORMAL,
				scale = 2f
			)

			receiver.drawScoreFont(engine, 0, 12, "Time", COLOR.BLUE)
			receiver.drawScoreNum(engine, 0, 13, engine.statistics.time.toTimeStr, 2f)

			receiver.drawScoreFont(engine, 0, 16, "I-Piece Drought", COLOR.BLUE)
			receiver.drawScoreNum(engine, 0, 17, "$drought / ${maxOf(drought, droughts.maxOrNull() ?: 0)}", 2f)
		}
	}

	/** Calculates line-clear score
	 * (This function will be called even if no lines are cleared) */
	override fun calcScore(engine:GameEngine, ev:ScoreEvent):Int {
		sdScore /= 2
		engine.statistics.scoreSD += sdScore
		sdScore = 0

		engine.statistics.scoreHD += hdScore
		hdScore = 0

		// Line clear score
		var pts = 0
		// Level up
		val li = ev.lines
		when {
			li==1 -> pts += 40*(engine.statistics.level+1) // Single
			li==2 -> pts += 100*(engine.statistics.level+1) // Double
			li==3 -> pts += 300*(engine.statistics.level+1) // Triple
			li>=4 -> pts += 1200*(engine.statistics.level+1) // Quadruple
		}

		// B-TYPE game completed
		if(gameType==GAMETYPE_TYPE_B&&engine.statistics.lines>=25) {
			pts += (engine.statistics.level+startHeight)*1000
			engine.ending = 1
			engine.gameEnded()
		}

		// Add score to total
		if(pts>0) {
			lastScore = pts
			engine.statistics.scoreLine += pts
		}

		if(gameType!=GAMETYPE_ARRANGE&&engine.statistics.score>999999&&maxScoredTime==null) {
			maxScoredTime = engine.statistics.time
			engine.playSE("applause5")
		}

		// Update meter
		engine.meterColor = GameEngine.METER_COLOR_LEVEL
		engine.meterValue =
			if(gameType==GAMETYPE_TYPE_B) minOf(engine.statistics.lines, 25)/25f else
				engine.statistics.lines%10*1f/9f

		if(gameType!=GAMETYPE_TYPE_B&&engine.statistics.lines>=lvLines) {
			// Level up
			engine.statistics.level++

			lvLines += 10

			//engine.framecolor = engine.statistics.level
			if(engine.statistics.level>255) engine.statistics.level = 0

			owner.bgMan.nextBg = maxOf(0, minOf(19, engine.statistics.level))

			setSpeed(engine)
			engine.playSE("levelup")
		}
		return pts
	}

	/** This function will be called when soft-drop is used */
	override fun afterSoftDropFall(engine:GameEngine, fall:Int) {
		sdScore += fall
	}

	/** This function will be called when hard-drop is used */
	override fun afterHardDropFall(engine:GameEngine, fall:Int) {
		hdScore += fall
	}

	override fun onGameOver(engine:GameEngine):Boolean {
		if(engine.statc[0]==0) {
			if(drought>7) droughts += drought
			drought = 0
		}
		return false
	}

	/* 結果画面の処理 */
	override fun onResult(engine:GameEngine):Boolean {
		if(engine.ctrl.isMenuRepeatKey(Controller.BUTTON_UP)) {
			engine.statc[1]--
			if(engine.statc[1]<0) engine.statc[1] = 2
			engine.playSE("change")
		}
		if(engine.ctrl.isMenuRepeatKey(Controller.BUTTON_DOWN)) {
			engine.statc[1]++
			if(engine.statc[1]>2) engine.statc[1] = 0
			engine.playSE("change")
		}

		return false
	}

	/** Renders game result screen */
	override fun renderResult(engine:GameEngine) {
		receiver.drawMenuFont(engine, 0, 0, "${BaseFont.UP_S}${BaseFont.DOWN_S} PAGE${engine.statc[1]+1}/2", COLOR.ORANGE)
		receiver.drawMenuFont(engine, 0, 1, "PLAY DATA", COLOR.ORANGE)

		if(engine.statc[1]==0) {
			drawResultStats(engine, receiver, 3, COLOR.BLUE, Statistic.SCORE, Statistic.LINES)
			receiver.drawMenuFont(engine, 0, 7, "Level", COLOR.BLUE)
			val strLevel = "%10s".format(LEVEL_NAME[engine.statistics.level])
			receiver.drawMenuFont(engine, 0, 8, strLevel)
			drawResultStats(engine, receiver, 9, COLOR.BLUE, Statistic.SPL)
			drawResultRank(engine, receiver, 15, COLOR.BLUE, rankingRank)
			drawResult(engine, receiver, 11, COLOR.BLUE, "QUAD%", String.format("%3d%%", engine.statistics.run {
				totalQuadruple*100/maxOf(1, totalQuadruple+totalSingle+totalDouble+totalTriple+totalSplitDouble+totalSplitTriple)
			}))
		} else {
			drawResultStats(engine, receiver, 3, COLOR.BLUE, Statistic.TIME, Statistic.LPM)
			receiver.drawMenuFont(engine, 0, 7, "I-Droughts", COLOR.BLUE)
			receiver.drawMenuFont(engine, 0, 8, "Longest", COLOR.BLUE, .8f)
			receiver.drawMenuNum(engine, 0, 8, "%3d".format(droughts.maxOrNull() ?: 0), 2f)
			receiver.drawMenuFont(engine, 0, 10, "Average", COLOR.BLUE, .8f)
			receiver.drawMenuNum(engine, 0, 11, droughts.average(), null to 3, scale = 2f)
			drawResult(
				engine, receiver, 13, COLOR.RED, "Burnouts",
				"%3d".format(engine.statistics.run {totalSingle+totalDouble+totalSplitDouble+totalTriple+totalSplitTriple})
			)
		}
	}

	/** This function will be called when the replay data is going to be
	 * saved */
	override fun saveReplay(engine:GameEngine, prop:CustomProperties):Boolean {
		saveSetting(engine, prop)

		// Checks/Updates the ranking
		if(!owner.replayMode&&!big&&engine.ai==null) {
			updateRanking(
				engine.statistics.score, engine.statistics.lines, engine.statistics.level,
				gameType+speedType*GAMETYPE_MAX
			)

			if(rankingRank!=-1) return true
		}
		return false
	}

	/** Update the ranking
	 * @param sc Score
	 * @param li Lines
	 * @param lv Level
	 * @param type Game type
	 */
	private fun updateRanking(sc:Long, li:Int, lv:Int, type:Int) {
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
	private fun checkRanking(sc:Long, li:Int, lv:Int, type:Int):Int {
		for(i in 0..<RANKING_MAX)
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
		val h = engine.field.height
		val startHeight = h-1
		var f:Float
		for(y in startHeight downTo h-tableGarbageHeight[height])
			for(x in 0..<engine.field.width) {
				f = engine.random.nextFloat()
				if(f<0.5)
					engine.field.setBlock(x, y, Block((f*14).toInt()+2, engine.skin, Block.ATTRIBUTE.VISIBLE, Block.ATTRIBUTE.GARBAGE))
			}
	}

	companion object {
		/** Denominator table (NTSC-U) */
		private val tableDenominator = listOf(
			//	0  1  2  3  4  5  6  7  8  9    +xx
			48, 43, 38, 33, 28, 23, 18, 13, 8, 6, // 00
			5, 5, 5, 4, 4, 4, 3, 3, 3, 2, // 10
			2, 2, 2, 2, 2, 2, 2, 2, 2, 1 // 20
		)

		/** Denominator table (Arrange) */
		private val tableDenominatorHard = listOf(
			//	0  1  2  3  4  5  6  7  8  9    +xx
			36, 32, 29, 25, 22, 18, 15, 11, 7, 5, // 00
			4, 4, 4, 3, 3, 3, 2, 2, 2, 1, // 10
		)
		/** Garbage height table */
		private val tableGarbagePeriod = listOf(6, 5)
		/** Garbage height table */
		private val tableGarbageHeight = listOf(0, 3, 5, 8, 10, 12)

		/** Game types */
		private enum class GameType(val showName:String) { A("A-"), B("B+"), C("C#") }

		/** Number of game types */
		private val GAMETYPE_MAX = GameType.entries.size

		/** Game type name */
		private val GAMETYPE_NAME = GameType.entries.map {it.showName}

		private val GAMETYPE_TYPE_A = GameType.A.ordinal
		private val GAMETYPE_TYPE_B = GameType.B.ordinal
		private val GAMETYPE_ARRANGE = GameType.C.ordinal

		/** Speed types */
		private enum class SpeedLevel(title:String? = null) {
			NTSC, PAL, PAL_RAW("PAL HARD");

			val showName:String = title ?: name
		}
		/** Number of speed types */
		private val SPEED_MAX = SpeedLevel.entries.size

		/** Speed type name */
		private val SPEED_NAME = SpeedLevel.entries.map {it.showName}

		/** Number of ranking records */
		private const val RANKING_MAX = 13

		/** Number of ranking types */
		private val RANKING_TYPE = GAMETYPE_MAX*SpeedLevel.entries.size

		/** Maximum-Level name table */
		private val LEVEL_NAME = listOf(
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
