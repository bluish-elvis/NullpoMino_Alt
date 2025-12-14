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
import mu.nu.nullpo.game.component.Piece.Shape
import mu.nu.nullpo.game.event.EventReceiver.COLOR
import mu.nu.nullpo.game.event.Leaderboard
import mu.nu.nullpo.game.event.ScoreEvent
import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.game.subsystem.mode.menu.*
import mu.nu.nullpo.gui.common.BaseFont
import mu.nu.nullpo.gui.common.BaseFont.FONT.*
import mu.nu.nullpo.util.CustomProperties
import mu.nu.nullpo.util.GeneralUtil.plus
import mu.nu.nullpo.util.GeneralUtil.times
import mu.nu.nullpo.util.GeneralUtil.toTimeStr
import org.apache.logging.log4j.LogManager

/** ALL CLEAR MANIA */
class GrandCleaner:AbstractMode() {
	private val itemEasy = BooleanStrMenuItem("piece", "PIECES", COLOR.BLUE, false, "EASY" to "HARD")
	/** PIECES SET */
	private var easyPiece:Boolean by DelegateMenuItem(itemEasy)

	private val itemQueue = BooleanStrMenuItem("randomnext", "RANDOM", COLOR.BLUE, false, "IRM" to "BAG")
	/** NEXT抽選方法 */
	private var queueType:Boolean by DelegateMenuItem(itemQueue)

	private val itemTT = BooleanMenuItem("timetrial", "TIME TRIAL", COLOR.RED, false)
	/** Time Attack mode */
	private var timeTrial:Boolean by DelegateMenuItem(itemTT)

	private val item20g = BooleanMenuItem("always20g", "20G MODE", COLOR.RED, false)
	/** Always 20G */
	private var always20g:Boolean by DelegateMenuItem(item20g)

	override val menu = MenuList("allclearmania", itemEasy, itemQueue, itemTT, item20g)

	override val rankingMax = 20

	/** Next section level (levelstop when this is -1) */
	private var nextSecLv = 0
	/** Levelが増えた flag */
	private var lvupFlag = false

	/** Current time limit */
	private var timeLimit = 0

	private var scgetdisp = 0

	/** Version */
	private var version = 0

	/** Current round's ranking position */
	private var rankingRank = 0

	/** Score records */
	@Serializable
	data class ScoreData(val st:Statistics = Statistics()):Comparable<ScoreData> {
		val clears = st.bravos
		val level get() = st.level
		val time get() = st.time.let {if(it<0) Int.MAX_VALUE else it}
		override operator fun compareTo(other:ScoreData):Int =
			compareValuesBy(this, other, {it.clears}, {-it.time}, {-it.level})
	}

	val rankingMode get() = easyPiece+timeTrial*2+queueType*4

	//	val rankLists = List(RANKING_TYPE) {Leaderboard(rankingMax, serializer<List<ScoreData>>())}
//	override val ranking:List<Leaderboard<*>> get() = rankLists[rankingMode]
	override val ranking = List(RANKING_TYPE) {Leaderboard(rankingMax, serializer<List<ScoreData>>())}
	private var decoration = 0
	private var decTemp = 0

	/** Mode nameを取得 */
	override val name = "Grand Cleaner"
	override val gameIntensity:Int = -1

	/** Initialization */
	override fun playerInit(engine:GameEngine) {
		log.debug("playerInit called")

		super.playerInit(engine)

		nextSecLv = 0
		lvupFlag = false

		timeLimit = TIME_LIMIT_MAX
		scgetdisp = 0

		rankingRank = -1
		ranking.forEach {it.fill(ScoreData())}

		engine.twistEnable = false
		engine.b2bEnable = false
		engine.splitB2B = false
		engine.frameSkin = GameEngine.FRAME_COLOR_PINK
		engine.big = true
		engine.bigHalf = true
		engine.bigMove = true
		engine.staffrollEnable = false

		engine.fieldWidth = 10
		engine.fieldHeight = 20
		engine.createFieldIfNeeded()
		version = if(!owner.replayMode) CURRENT_VERSION
		else owner.replayProp.getProperty("allclearmania.version", 0)



		if(version<=0) {
			engine.readyStart = 45
			engine.readyEnd = 155
			engine.goStart = 160
			engine.goEnd = 225
		}
	}

	/** Update falling speed
	 * @param engine GameEngine
	 */
	override fun setSpeed(engine:GameEngine) {
		engine.speed.gravity = if(always20g) -1 else 4
		engine.speed.denominator = 256
		engine.speed.are = 23
		engine.speed.areLine = 23
		engine.speed.lockDelay = 31
		engine.speed.lineDelay = 25
		engine.speed.das = 15
		engine.ghost = true
	}

	/** Main routine for game setup screen */
	override fun onSetting(engine:GameEngine):Boolean {
		if(!engine.owner.replayMode) {
			// Check for F button, when pressed this will flip Leaderboard/Best Section Time Records
			if(engine.ctrl.isPush(Controller.BUTTON_F)) {
				engine.playSE("change")
			}
		}
		return super.onSetting(engine)
	}

	override fun onSettingChanged(engine:GameEngine) {
		/*
				if(startLevel<0) startLevel = 2
				if(startLevel>2) startLevel = 0
				owner.bgMan.bg = -1-startLevel
				engine.statistics.level = startLevel*100
				nextSecLv = startLevel*100+100*/

		setSpeed(engine)
		super.onSettingChanged(engine)
	}
	/* Ready画面の処理 */
	override fun onReady(engine:GameEngine):Boolean {
		if(engine.statc[0]==0) {
			owner.musMan.fadeSW = true
			engine.nextPieceEnable = List(Piece.PIECE_STANDARD_COUNT) {i ->
				!easyPiece||EASY_PIECES.any {it.ordinal==i}
			}
		}
		return super.onReady(engine)
	}

	/* Called at game start(2回目以降のReadyも含む) */
	override fun startGame(engine:GameEngine) {
		engine.big = true
		// BGM切り替え
		owner.musMan.fadeSW = false
		owner.musMan.bgm = BGM.Puzzle(0)
	}

	/* Render score */
	override fun renderLast(engine:GameEngine) {
		receiver.drawScore(
			engine, 0, 0, "GRAND Cleaner", BASE, COLOR.RED
		)

		receiver.drawScore(engine, -1, -4*2, "DECORATION", BASE, scale = .5f)
		receiver.drawScoreBadges(engine, 0, -3, 100, decoration)
		receiver.drawScoreBadges(engine, 5, -4, 100, decTemp)
		if(engine.stat==GameEngine.Status.SETTING||engine.stat==GameEngine.Status.RESULT&&!owner.replayMode) {
			if(!always20g&&engine.ai==null) {
				val topY = if(receiver.nextDisplayType==2) 5 else 3

				receiver.drawScore(engine, 2, topY-1, "CLEAR TIME LEVEL", BASE, COLOR.PINK)
				val type = queueType

				ranking[rankingMode].forEachIndexed {i, it ->
					receiver.drawScore(
						engine, 0, topY+i, "%2d".format(i+1), GRADE,
						when {
							i==rankingRank -> COLOR.RAINBOW
							it.level>=1000 -> COLOR.GREEN
							it.clears>=MAX_CLEAR_TOTAL -> COLOR.ORANGE
							else -> COLOR.YELLOW
						}
					)
					receiver.drawScore(engine, 3, topY+i, "%3d".format(it.clears), NUM, i==rankingRank)
					receiver.drawScore(engine, 7, topY+i, it.time.toTimeStr, NUM, i==rankingRank)
					receiver.drawScore(engine, 13, topY+i, "%4d".format(it.level), NUM, i==rankingRank)

				}
			}
		} else {
			receiver.drawScore(engine, 0, 11, "All Clears", BASE, COLOR.PINK)
			receiver.drawScore(engine, 1, 12, "%3d".format(engine.statistics.bravos), NUM, scgetdisp>0, 2f)

			//  level
			receiver.drawScore(engine, 0, 11, "Level", BASE, COLOR.PINK)
			receiver.drawScore(engine, 1, 12, "%3d".format(maxOf(0, engine.statistics.level)), NUM_T)

			// Time limit
			receiver.drawScore(
				engine, 0, 20, timeLimit.toTimeStr, NUM, engine.timerActive&&timeLimit<600&&timeLimit%4==0, 2f
			)

		}
	}

	/* Called after every frame */
	override fun onLast(engine:GameEngine) {
		super.onLast(engine)

		if(scgetdisp>0) scgetdisp--

		// Time limit
		if(engine.gameActive&&engine.timerActive&&timeLimit>0) {
			timeLimit--

			// Time meter
			engine.meterValue = minOf(1f, timeLimit*1f/TIME_LIMIT_MAX)
			engine.meterColor = GameEngine.METER_COLOR_LIMIT

			if(timeLimit>0&&timeLimit<=10*60&&timeLimit%60==0)
			// 10秒前からのカウントダウン
				engine.playSE("countdown")
		}

	}

	/* 移動中の処理 */
	override fun onMove(engine:GameEngine):Boolean {
		// 新規ピース出現時
		if(engine.ending==0&&engine.statc[0]==0&&!engine.holdDisable&&!lvupFlag) {
			// Level up
			if(engine.statistics.level<nextSecLv-1) {
				engine.statistics.level++
				if(engine.statistics.level==nextSecLv-1) engine.playSE("levelstop")
			}
			setSpeed(engine)
		}
		if(engine.ending==0&&engine.statc[0]>0) lvupFlag = false

		return false
	}

	/* ARE中の処理 */
	override fun onARE(engine:GameEngine):Boolean {
		// 最後の frame
		if(engine.ending==0&&engine.statc[0]>=engine.statc[1]-1&&!lvupFlag) {
			if(engine.statistics.level<nextSecLv-1) {
				engine.statistics.level++
				if(engine.statistics.level==nextSecLv-1) engine.playSE("levelstop")
			}
			setSpeed(engine)
			lvupFlag = true
		}

		return false
	}

	/* Calculate score */
	override fun calcScore(engine:GameEngine, ev:ScoreEvent):Int {
		// 実際に消えるLines count(Big時半分にならない)
		val realLines = engine.field.lines

		if(realLines>=1&&engine.ending==0) {
			val it = ev.lines
			val timeRec = if(engine.statistics.level<=TIME_LIMIT_THIRST_LV)
				if(engine.field.isEmpty) when(it) {
					1 -> 300
					2 -> 480
					3 -> 660
					else -> 900
				} else when(it) {
					1 -> 1
					2 -> 2
					3 -> 5
					else -> 11
				}
			else if(it>=4) 60
			else 0
			// Time limit
			timeLimit = minOf(timeLimit+timeRec, TIME_LIMIT_MAX)

			// Level up
			val levelPlus = when {
				it==3 -> 6
				it>=4 -> 12
				else -> it*2
			}
			engine.statistics.level += levelPlus

//			setSpeed(engine)
			if(engine.statistics.lines>=MAX_CLEAR_TOTAL) {
				// Ending
				engine.ending = 1
				engine.gameEnded()
			} else if(engine.statistics.level>=nextSecLv) {
				// Next Section
				engine.playSE("levelup")

				// Background切り替え
				owner.bgMan.nextBg = nextSecLv/100

				// Update level for next section
				nextSecLv += 100
			} else if(engine.statistics.level==nextSecLv-1) engine.playSE("levelstop")
			return timeRec/60
		}
		return 0
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

	/* Render results screen */
	override fun renderResult(engine:GameEngine) {
		receiver.drawMenu(engine, 0, 0, "${BaseFont.UP_S}${BaseFont.DOWN_S} PAGE${engine.statc[1]+1}/3", BASE, COLOR.RED)

		receiver.drawMenu(engine, 5, 2, "ALL\nCLEARs", BASE, COLOR.PINK)
		receiver.drawMenu(engine, -.25f, 2, "%3d".format(engine.statistics.bravos), BASE, 2f)

		drawResultStats(engine, receiver, 6, COLOR.PINK, Statistic.LINES, Statistic.LEVEL_MANIA, Statistic.PIECE,
			Statistic.TIME)
		drawResultRank(engine, receiver, 14, COLOR.PINK, rankingRank)

	}

	/** This function will be called when the replay data is going to be saved */
	override fun saveReplay(engine:GameEngine, prop:CustomProperties):Boolean {
		if(!owner.replayMode&&!always20g&&engine.ai==null) {
			owner.statsProp.setProperty("decoration", decoration)
			rankingRank = ranking[rankingMode].add(ScoreData(engine.statistics))
			if(rankingRank!=-1) return true
		}
		return false
	}

	companion object {
		/** Log */
		internal val log = LogManager.getLogger()

		/** Current version */
		private const val CURRENT_VERSION = 1

		/** Maximum level count */
		private const val MAX_CLEAR_TOTAL = 110

		/** Time limit recover stops at this level */
		private const val TIME_LIMIT_THIRST_LV = 1000

		private const val TIME_LIMIT_START = 3*3600
		private const val TIME_LIMIT_MAX = 5*3600

		private val EASY_PIECES = listOf(Shape.O, Shape.T, Shape.I, Shape.J, Shape.L)

		/** Number of ranking typesのcount */
		private const val RANKING_TYPE = 8

	}
}
