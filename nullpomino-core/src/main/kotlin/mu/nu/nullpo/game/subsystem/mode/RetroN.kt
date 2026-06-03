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

import kotlinx.serialization.Serializable
import kotlinx.serialization.serializer
import mu.nu.nullpo.game.component.*
import mu.nu.nullpo.game.event.*
import mu.nu.nullpo.game.event.EventReceiver.COLOR
import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.game.subsystem.mode.menu.*
import mu.nu.nullpo.gui.common.BaseFont
import mu.nu.nullpo.gui.common.BaseFont.FONT.*
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
	private var lvLBegin = 0

	private val itemGame = EnumMenuItem("gametype", "GAME TYPE", COLOR.BLUE, GameType.A, GameType.entries)
	/** Selected game type */
	private var gameType:GameType by DelegateMenuItem(itemGame)

	private val itemSpeed = EnumMenuItem("speedtype", "DIFFICULTY", COLOR.BLUE, SpeedLevel.N_NTSC, SpeedLevel.entries)
	/** Selected Speed Difficulty */
	private var speedType:SpeedLevel by DelegateMenuItem(itemSpeed)

	private val itemLevel = LevelMenuItem("startlevel", "LEVEL", COLOR.BLUE, 0, 0..19, false, true)
	/** Selected starting level */
	private var startLevel:Int by DelegateMenuItem(itemLevel)

	private val itemHeight = IntegerMenuItem("height", "HEIGHT", COLOR.BLUE, 0, 0..5)
	/** Selected garbage height */
	private var startHeight:Int by DelegateMenuItem(itemHeight)

	private val itemBig = BooleanMenuItem("big", "BIG", COLOR.ORANGE, false)
	/** Big mode on/off */
	private var big:Boolean by DelegateMenuItem(itemBig)

	override val menu = MenuList("retromarathon", itemGame, itemSpeed, itemLevel, itemHeight, itemBig)

	/** Max interval of I-piece */
	private var drought = 0
	private val droughts:MutableList<Int> = mutableListOf()

	/** Your place on leaderboard (-1: out of rank) */
	private var rankingRank = 0

	@Serializable data class ScoreRow(override val st:Statistics = Statistics(), val scMaxed:Int = -1)
		:Rankable, Comparable<Rankable> {
		override operator fun compareTo(other:Rankable):Int =
			if(other is ScoreRow)
				compareValuesBy(this, other, {it.sc}, {it.li}, {it.lv}, {-it.scMaxed}, {-it.ti})
			else super.compareTo(other)

	}

	override val ranking = List(RANKING_TYPE) {
		Leaderboard(rankingMax, serializer<List<ScoreRow>>()) {ScoreRow()}
	}

	/** Time records Reaches Score Max-out 999999 */
	private var maxScoredTime:Int? = null

	/** Returns the name of this mode */
	override val name = "Retro Classic .N"

	override val gameIntensity = -2

	/** This function will be called when the game enters the main game
	 * screen. */
	override fun playerInit(engine:GameEngine) {
		super.playerInit(engine)
		lastScore = 0
		sdScore = 0
		hdScore = 0
		lvLines = 0
		lvLBegin = 0
		drought = 0
		droughts.removeAll {true}

		rankingRank = -1
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
			randomizer = NintendoRandomizer()
			ruleOpt.run {
				replace(ruleOptBuf)
				pieceOffsetX = RuleOptions.PIECEOFFSET_ARSPRESET[0]
				pieceOffsetY = RuleOptions.PIECEOFFSET_ARSPRESET[1]
				nextDisplay = 1
				harddropEnable = false
				strWallkick = ""
				spinInitial = false
				spinReverseKey = true
				lockResetMove = false
				softdropLock = true
				softdropMultiplyNativeSpeed = false
				softdropGravitySpeedLimit = false
				holdEnable = false
				dasInARE = true
				dasInReady = false
				dasChargeOnBlockedMove = true
				dasStoreChargeOnNeutral = true
				dasRedirectInDelay = true
			}
			owDelayCancel = 0
		}

		owner.bgMan.bg = startLevel
		if(owner.bgMan.bg>19) owner.bgMan.bg = 19
		lvLines = ((startLevel-5)*10).coerceIn(minOf((startLevel+1)*10, 100), 100)
		engine.frame = GameEngine.Frame.GB
	}

	/** Set the gravity speed
	 * @param engine GameEngine object
	 */
	override fun setSpeed(engine:GameEngine) {
		val lv = engine.statistics.level
//			.coerceIn(0, if(speedType!=SpeedLevel.N_NTSC) tableDenominatorHard.size-1 else tableDenominator.size-1)
		engine.owSkin = if(speedType==SpeedLevel.N_PAL_RAW!=lv>=10) 8 else 9
		engine.speed.replace(speedType.table[lv])
		engine.owARR = speedType.owARR
		/*if(speedType!=SpeedLevel.N_NTSC) {
			engine.speed.gravity = if(speedType==SpeedLevel.N_PAL_RAW) 60000 else 50007
			engine.speed.denominator = tableDenominatorHard[lv]*60000
			engine.speed.das = 12
			engine.owARR = 4
		} else {
			engine.speed.gravity = 601
			engine.speed.denominator = tableDenominator[lv]*600
			engine.speed.das = 16
			engine.owARR = 6
		}
		if(gameType==GameType.C) {
			engine.speed.das = 6
			engine.owARR = 2
		}*/
		engine.speed.lockDelay = engine.speed.denominator/engine.speed.gravity
	}

	/** Main routine for game setup screen */
	override fun onSettingChanged(engine:GameEngine) {
		engine.statistics.level = startLevel
		engine.statistics.levelDispAdd = 1
		lvLBegin = 0
		engine.big = big
		lvLines = ((startLevel-5)*10).coerceIn(minOf((startLevel+1)*10, 100), 100)
		setSpeed(engine)
		super.onSettingChanged(engine)
	}

	override fun onReady(engine:GameEngine):Boolean {
		if(engine.stime==0) {
			engine.run {
				randomizer = NintendoRandomizer()
				ruleOpt.run {
					spinInitial = false
					spinReverseKey = true
					lockResetMove = false
					softdropLock = true
					softdropMultiplyNativeSpeed = false
					softdropGravitySpeedLimit = false
					holdEnable = false
					dasInARE = true
					dasInReady = false
					dasChargeOnBlockedMove = true
					dasStoreChargeOnNeutral = true
					dasRedirectInDelay = true
				}
				ghost = false
				createFieldIfNeeded()
			}
			fillGarbage(engine, startHeight)
		}
		return false
	}

	/** This function will be called before the game actually begins
	 * (after Ready&Go screen disappears) */
	override fun startGame(engine:GameEngine) {

		owner.musMan.bgm = BGM.RetroN(if(startLevel>9) 3 else (gameType.ordinal+startLevel)%3)
		setSpeed(engine)
	}

	override fun onMove(engine:GameEngine):Boolean {
		// 新規ピース出現時
		if(engine.ending==0&&engine.statc[0]==0&&!engine.holdDisable)
			if(engine.getNextObjectCopy()?.shape!=Piece.Shape.I) drought++
			else {
				if(drought>7) droughts += drought
				drought = 0
			}

		return false
	}

	/** Renders HUD (leaderboard or game statistics) */
	override fun renderLast(engine:GameEngine) {
		receiver.drawScore(engine, 0, 0, name, BASE, COLOR.GREEN)
		receiver.drawScore(engine, 0, 1, "(${speedType} ${gameType}Type)", BASE, COLOR.GREEN)

		if(engine.isShowRanking&&!big) {
			receiver.drawScore(engine, 3, 3, "SCORE    LINE LV.", BASE, COLOR.BLUE)
			ranking[gameType.ordinal].forEachIndexed {i, it ->
				receiver.drawScore(
					engine, 0, 4+i, "%2d".format(i+1), GRADE, if(rankingRank==i) COLOR.RAINBOW else COLOR.YELLOW
				)
				receiver.drawScore(engine, 3, 4+i, GeneralUtil.capsNum(it.sc, 6), NUM, i==rankingRank)
				receiver.drawScore(
					engine, 12, 4+i, GeneralUtil.capsNum(it.li, 3), NUM, i==rankingRank
				)
				receiver.drawScore(engine, 17, 4+i, LEVEL_NAME[it.lv],
					font = if(it.lv<30) NUM else BASE, flag = i==rankingRank
				)
			}
		} else {
			receiver.drawScore(engine, 0, 3, "SCORE${if(lastScore>0) "(+$lastScore)" else ""}", BASE, COLOR.BLUE)
			receiver.drawScore(
				engine, 0, 4, GeneralUtil.capsNum(scDisp, 6), font = if(engine.statistics.score<=999999) NUM else BASE,
				scale = 2f
			)

			receiver.drawScore(engine, 0, 6, "LINE", BASE, COLOR.BLUE)
			receiver.drawScore(
				engine, 0, 7, when(gameType) {
					GameType.B -> "-%2d".format(maxOf(25-engine.statistics.lines, 0))
					else -> GeneralUtil.capsNum(engine.statistics.lines, 3)
				}, if(gameType!=GameType.B&&engine.statistics.lines<999) NUM else BASE, scale = 2f
			)

			receiver.drawScore(engine, 0, 9, "Level", BASE, COLOR.BLUE)
			receiver.drawScore(
				engine, 0, 10, LEVEL_NAME[engine.statistics.level], if(engine.statistics.level<30) NUM else BASE,
				scale = 2f
			)

			receiver.drawScore(engine, 0, 12, "Time", BASE, COLOR.BLUE)
			receiver.drawScore(engine, 0, 13, engine.statistics.time.toTimeStr, NUM_T)

			receiver.drawScore(engine, 0, 16, "I-Piece Drought", BASE, COLOR.BLUE)
			receiver.drawScore(engine, 0, 17, "$drought / ${maxOf(drought, droughts.maxOrNull()?:0)}", NUM, 2f)
		}
	}

	/** Calculates lines-clear score
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
		pts += when {
			li==1 -> 40 // Single
			li==2 -> 100 // Double
			li==3 -> 300 // Triple
			li>=4 -> 1200 // Quadruple
			else -> 0
		}*(engine.statistics.level+1)

		// Add score to total
		if(pts>0) {
			lastScore = pts
			engine.statistics.scoreLine += pts
		}

		if(engine.statistics.score>999999&&maxScoredTime==null) {
			maxScoredTime = engine.statistics.time
			engine.playSE("applause5")
		}

		// Update meter
		engine.meterColor = if(gameType==GameType.B) GameEngine.METER_COLOR_LIMIT else GameEngine.METER_COLOR_LEVEL
		engine.meterValue =
			if(gameType==GameType.B) minOf(engine.statistics.lines, 25)/25f else
				(engine.statistics.lines-lvLBegin)*1f/lvLines

		if(gameType!=GameType.B&&engine.statistics.lines>=lvLines) {
			// Level up
			engine.statistics.level++

			lvLines += if(engine.statistics.level==235) 810 else 10
			lvLBegin = engine.statistics.lines
			if(gameType!=GameType.C&&engine.statistics.level>255) {
				engine.statistics.level = 0
				engine.playSE("levelup_section")
			}
			if((gameType!=GameType.C&&engine.statistics.level>=10||engine.statistics.lines>=130)
				&&owner.musMan.bgm.idx!=BGM.RetroN(3).idx) owner.musMan.bgm = BGM.RetroN(3)
			owner.bgMan.nextBg = engine.statistics.level

			setSpeed(engine)
			engine.playSE("levelup")
		}

		// Game ends: B-TYPE: 25 Lines, C-TYPE:255 levels(3300 lines)

		if((gameType==GameType.B&&engine.statistics.lines>=25)||gameType==GameType.C&&engine.statistics.level>255) {
			engine.statistics.scoreBonus += ((engine.statistics.level+startHeight)*1000).also {
				engine.receiver.addScore(engine, +2, engine.field.highestBlockY-2, it, COLOR.RAINBOW, "CLEAR BONUS", true)
			}
			engine.ending = 1
			engine.gameEnded()
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
			if(engine.statc[1]<0) engine.statc[1] = 1
			engine.playSE("change")
		}
		if(engine.ctrl.isMenuRepeatKey(Controller.BUTTON_DOWN)) {
			engine.statc[1]++
			if(engine.statc[1]>1) engine.statc[1] = 0
			engine.playSE("change")
		}

		return false
	}

	/** Renders game result screen */
	override fun renderResult(engine:GameEngine) {
		receiver.drawMenu(engine, 0, 0, "${BaseFont.UP_S}${BaseFont.DOWN_S} PAGE${engine.statc[1]+1}/2", BASE, COLOR.ORANGE)
		receiver.drawMenu(engine, 0, 1, "PLAY DATA", BASE, COLOR.ORANGE)

		if(engine.statc[1]==0) {
			drawResultStats(engine, receiver, 3, COLOR.BLUE, Statistic.SCORE, Statistic.LINES)
			receiver.drawMenu(engine, 0, 7, "Level", BASE, COLOR.BLUE)
			val strLevel = "%10s".format(LEVEL_NAME[engine.statistics.level])
			receiver.drawMenu(engine, 0, 8, strLevel, BASE)

			drawResultStats(engine, receiver, 10, COLOR.BLUE, Statistic.SPL)
			receiver.drawMenu(engine, 0, 12, "%3d".format(engine.statistics.totalQuadruple), NUM, 2f)
			receiver.drawMenu(engine, 5, 13, "Quads", BASE, COLOR.BLUE)
			receiver.drawMenuNum(engine, 5, 12, engine.statistics.run {
				totalQuadruple*100f/maxOf(1, totalQuadruple+totalSingle+totalDouble+totalTriple+totalSplitDouble+totalSplitTriple)
			}, 7 to 3)
			receiver.drawMenu(engine, 9.2f, 12, "%", NUM, COLOR.BLUE)
			drawResultRank(engine, receiver, 15, COLOR.BLUE, rankingRank)
		} else {
			drawResultStats(engine, receiver, 3, COLOR.BLUE, Statistic.TIME, Statistic.LPM)
			receiver.drawMenu(engine, 0, 7, "I-Droughts", BASE, COLOR.BLUE)
			receiver.drawMenu(engine, 0, 8, "Longest", BASE, COLOR.BLUE, .8f)
			receiver.drawMenu(engine, 6, 8, "%3d".format(droughts.maxOrNull()?:0), NUM, 2f)
			receiver.drawMenu(engine, 0, 9.5f, "Average", BASE, COLOR.BLUE, .8f)
			receiver.drawMenuNum(engine, 2, 10.5f, droughts.average(), 7 to 3, scale = 2f)
			drawResult(
				engine, receiver, 13, COLOR.RED, "Burnouts",
				"%5d".format(engine.statistics.run {totalSingle+totalDouble+totalSplitDouble+totalTriple+totalSplitTriple})
			)
			receiver.drawMenu(engine, 5, 14, "S:", BASE, COLOR.BLUE)
			receiver.drawMenu(engine, 7, 14, "%3d".format(engine.statistics.totalSingle), NUM)
			receiver.drawMenu(engine, 5, 15, "D:", BASE, COLOR.BLUE)
			receiver.drawMenu(engine, 7, 15, "%3d".format(engine.statistics.run {totalDouble+totalSplitDouble}), NUM)
			receiver.drawMenu(engine, 5, 16, "T:", BASE, COLOR.BLUE)
			receiver.drawMenu(engine, 7, 16, "%3d".format(engine.statistics.run {totalTriple+totalSplitTriple}), NUM)
		}
	}

	/** This function will be called when the replay data is going to be
	 * saved */
	override fun saveReplay(engine:GameEngine, prop:CustomProperties):Boolean {
		saveSetting(engine, prop)

		// Checks/Updates the ranking
		if(!owner.replayMode&&!big&&engine.ai==null) {
			rankingRank = ranking[typeSerial(gameType, speedType)].add(ScoreRow(engine.statistics, maxScoredTime?:-1))
			if(rankingRank!=-1) return true
		}
		return false
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
					engine.field.setBlock(x, y,
						Block((f*14).toInt()+2, engine.blkSkin, Block.ATTRIBUTE.VISIBLE, Block.ATTRIBUTE.GARBAGE))
			}
	}

	companion object {

		/*
		* 		if(speedType!=SpeedLevel.NOA_NTSC) {
			engine.speed.gravity = if(speedType==SpeedLevel.PAL_RAW) 60000 else 50007
			engine.speed.denominator = tableDenominatorHard[lv]*60000
			engine.speed.das = 12
			engine.owARR = 4
		} else {
			engine.speed.gravity = 601
			engine.speed.denominator = tableDenominator[lv]*600
			engine.speed.das = 16
			engine.owARR = 6
		}*/
		/** Denominator table (NOE_PAL) */
		private val tableDenominatorHard = listOf(
			//	0  1  2  3  4  5  6  7  8  9    +xx
			36, 32, 29, 25, 22, 18, 15, 11, 7, 5, // 00
			4, 4, 4, 3, 3, 3, 2, 2, 2, 1, // 10
		)
		/** Garbage height table */
		private val tableGarbagePeriod = intArrayOf(6, 5)
		/** Garbage height table */
		private val tableGarbageHeight = intArrayOf(0, 3, 5, 8, 10, 12)

		/** Game types */
		private enum class GameType(val showName:String) {
			A("A-"), B("B+"), C("C#");

			override fun toString() = showName
		}
		/** Number of game types */
		private val GAMETYPE_MAX = GameType.entries.size

		private fun pal(raw:Boolean):LevelData = ((if(raw) 60000 else 50007) to (listOf(
			//	0  1  2  3  4  5  6  7  8  9    +xx
			36, 32, 29, 25, 22, 18, 15, 11, 7, 5, // 00
			4, 4, 4, 3, 3, 3, 2, 2, 2, 1, // 10
		).map {it*60000})).let {(g, d) ->
			LevelData(listOf(g), d, listOf(10), listOf(10), listOf(20), d.map {g/it}, listOf(12))
		}
		/** Speed types */
		private enum class SpeedLevel(val table:LevelData, val owARR:Int = -1, title:String? = null) {
			N_NTSC(listOf(
				//	0  1  2  3  4  5  6  7  8  9    +xx
				48, 43, 38, 33, 28, 23, 18, 13, 8, 6, // 00
				5, 5, 5, 4, 4, 4, 3, 3, 3, 2, // 10
				2, 2, 2, 2, 2, 2, 2, 2, 2, 1 // 20
			).map {it*600}.let {LevelData(listOf(601), it, listOf(10), listOf(10), listOf(20), it.map {d -> 601/d}, listOf(16))},
				6, "NOA-NTSC"),

			N_PAL(pal(false), 4, "NOE-PAL"), N_PAL_RAW(pal(true), 4, "NOE-PAL HARD"),
			BPS2(LevelData(listOf(1, 3, 5, 7, 9, 11, 13, 15, 17, 19, 23, 27, 31, 35, 39, 43, 47, 51, 55, 59,
				69, 77, 85, 93, 101, 109, 117, 125, 133, 141, 152, 164, 175, 187, 198, 210, 221, 233, 244, 256), listOf(128),
				8, 8, 16, 26, 12), 10, "BPS-S2");

			val showName:String = title?:name
			override fun toString() = showName
		}

		/** Number of ranking types */
		private val RANKING_TYPE = GAMETYPE_MAX*SpeedLevel.entries.size

		private fun typeSerial(gameType:GameType, speedType:SpeedLevel):Int = gameType.ordinal+speedType.ordinal*GAMETYPE_MAX
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
