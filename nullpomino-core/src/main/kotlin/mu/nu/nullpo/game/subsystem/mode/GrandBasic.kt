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
import mu.nu.nullpo.game.component.BGMStatus.BGM
import mu.nu.nullpo.game.component.Controller
import mu.nu.nullpo.game.component.Statistics
import mu.nu.nullpo.game.event.EventReceiver.COLOR
import mu.nu.nullpo.game.event.Leaderboard
import mu.nu.nullpo.game.event.ScoreEvent
import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.game.subsystem.mode.menu.BooleanMenuItem
import mu.nu.nullpo.game.subsystem.mode.menu.DelegateMenuItem
import mu.nu.nullpo.game.subsystem.mode.menu.LevelGrandMenuItem
import mu.nu.nullpo.game.subsystem.mode.menu.MenuList
import mu.nu.nullpo.gui.common.BaseFont
import mu.nu.nullpo.util.CustomProperties
import mu.nu.nullpo.util.GeneralUtil.toTimeStr
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.ln

/** SCORE ATTACK mode (Original from NullpoUE build 121909 by Zircean) */
class GrandBasic:AbstractGrand() {

	private var hanabi = 0
	private var tempHanabi = 0
	private var intHanabi = 0
	private var bonusSpeed = 0
	private var bonusInt = 0
	private var halfMinLine = 0
	private var halfMinBonus = false

	/** Elapsed time from last line clear */
	private var lastLineTime = 0

	/** Elapsed time from last piece spawns */
	private var lastSpawnTime = 0

	/** Remaining ending time limit */
	private var rollTime = 0

	/** Secret Grade */
	private var secretGrade = 0

	/** Current BGM number */
	private var bgmLv = 0

	/** Section Record */
	private val sectionHanabi = MutableList(SECTION_MAX+1) {0}
	private val sectionScore = MutableList(SECTION_MAX+1) {0L}

	/** false:Leaderboard, true:Section time record
	 *  (Push F in settings screen to flip it) */
	private var isShowBestSectionTime = false

	private val itemLevel = LevelGrandMenuItem(COLOR.GREEN, 3)
	/** Level at start */
	private var startLevel:Int by DelegateMenuItem(itemLevel)

	private val item20g = BooleanMenuItem("always20g", "20G MODE", COLOR.RED, false)
	/** Always 20G */
	private var always20g:Boolean by DelegateMenuItem(item20g)

	private val itemBig = BooleanMenuItem("big", "BIG", COLOR.GREEN, false)
	/** BigMode */
	private var big:Boolean by DelegateMenuItem(itemBig)

	override val menu = MenuList("scoreattack", itemLevel, itemGhost, itemST, item20g, itemBig)
	/** Version of this mode */
	private var version = 0

	/** Your place on leaderboard (-1: out of rank) */
	private var rankingRank = 0

	override val RANKING_MAX = 20

	/** Score records */
	@Serializable
	data class ScoreData(val hanabi:Int, val st:Statistics, val clear:Int):Comparable<ScoreData> {
		constructor():this(0, Statistics(), 0)

		val score get() = st.score
		val level get() = st.level
		val time get() = st.time.let {if(it<0) Int.MAX_VALUE else it}
		override operator fun compareTo(other:ScoreData):Int =
			compareValuesBy(this, other, {it.hanabi}, {it.score}, {-it.time}, {it.level})

	}

	override val ranking = Leaderboard(RANKING_MAX, serializer<List<ScoreData>>())

	private val bestSectionScore = MutableList(RANKING_MAX) {0L}
	private val bestSectionHanabi = MutableList(RANKING_MAX) {0}
	private val bestSectionTime = MutableList(RANKING_MAX) {DEFAULT_SECTION_TIME}

	/** Returns the name of this mode */
	override val name = "Grand Festival"

	override val propRank
		get() = rankMapOf(
			"section.score" to bestSectionScore, "section.hanabi" to bestSectionHanabi, "section.time" to bestSectionTime
		)
	/** This function will be called when the game enters the main game
	 * screen. */
	override fun playerInit(engine:GameEngine) {
		super.playerInit(engine)
		bonusInt = 0
		bonusSpeed = 0
		tempHanabi = 0
		hanabi = 0
		rollTime = 0
		bgmLv = 0
		halfMinLine = 0
		halfMinBonus = false
		lastLineTime = 0
		lastSpawnTime = 0
		sectionHanabi.fill(0)
		sectionScore.fill(0)

		rankingRank = -1
		ranking.fill(ScoreData())
		bestSectionHanabi.fill(0)
		bestSectionScore.fill(0)
		bestSectionTime.fill(DEFAULT_SECTION_TIME)

		engine.frameColor = GameEngine.FRAME_COLOR_GREEN
		engine.twistEnable = true
		engine.twistEnableEZ = true
		engine.b2bEnable = true
		engine.splitB2B = true
		engine.comboType = GameEngine.COMBO_TYPE_DOUBLE
		engine.bigHalf = false
		engine.bigMove = false
		engine.staffrollNoDeath = true

		engine.speed.are = 25
		engine.speed.areLine = 25
		engine.speed.lineDelay = 40
		engine.speed.lockDelay = 30
		engine.speed.das = 15

		version = (if(!owner.replayMode) CURRENT_VERSION else owner.replayProp.getProperty("scoreattack.version", 0))

		owner.bgMan.bg = -1
	}

	/** Set the gravity speed
	 * @param engine GameEngine object
	 */
	override fun setSpeed(engine:GameEngine) {
		engine.speed.gravity = if(always20g) -1
		else tableGravityValue.lastOrNull {engine.statistics.level>=it.first}?.second ?: -1
	}

	/** Best section time update check routine
	 * @param sec Section Number
	 */
	private fun stNewRecordCheck(sec:Int) {
		if(!owner.replayMode&&(sectionHanabi[sec]>bestSectionHanabi[sec]||sectionScore[sec]>bestSectionScore[sec])||
			(sectionHanabi[sec]==bestSectionHanabi[sec]&&sectionScore[sec]==bestSectionScore[sec]
				&&sectionTime[sec]<bestSectionTime[sec])
		) {
			sectionIsNewRecord[sec] = true
		}
	}

	/** Main routine for game setup screen */
	override fun onSetting(engine:GameEngine):Boolean {
		if(!engine.owner.replayMode) {
			// Check for F button, when pressed this will flip Leaderboard/Best Section Time Records
			if(engine.ctrl.isPush(Controller.BUTTON_F)) {
				engine.playSE("change")
				isShowBestSectionTime = !isShowBestSectionTime
			}
		}
		return super.onSetting(engine)
	}

	override fun onSettingChanged(engine:GameEngine) {

		if(startLevel<0) startLevel = 2
		if(startLevel>2) startLevel = 0
		owner.bgMan.bg = -1-startLevel
		engine.statistics.level = startLevel*100
		nextSecLv = startLevel*100+100
		setSpeed(engine)
		super.onSettingChanged(engine)
	}

	override fun onReady(engine:GameEngine):Boolean {
		if(engine.statc[0]==0) {
			isShowBestSectionTime = false
			owner.musMan.fadeSW = true
		}
		return super.onReady(engine)
	}
	/** This function will be called before the game actually begins (after
	 * Ready&Go screen disappears) */
	override fun startGame(engine:GameEngine) {
		engine.statistics.level = startLevel*100

		nextSecLv = minOf(maxOf(100, startLevel*100+100), 999)

		owner.bgMan.bg = -engine.statistics.level/100-1

		engine.big = big

		setSpeed(engine)
		bgmLv = if(engine.statistics.level<300) 0 else 1
		owner.musMan.bgm = if(engine.statistics.level<300) BGM.GrandA(0) else BGM.GrandA(1)
	}

	override fun renderFirst(engine:GameEngine) {
		if(engine.ending==2) receiver.drawStaffRoll(engine, rollTime*1f/ROLLTIMELIMIT)
	}
	/** Renders HUD (leaderboard or game statistics) */
	override fun renderLast(engine:GameEngine) {
		receiver.drawScoreFont(engine, 0, 0, name, COLOR.COBALT)
		receiver.drawScoreFont(engine, 1, 1, "Score Attack", COLOR.COBALT)

		receiver.drawScoreFont(engine, -1, -4*2, "DECORATION", scale = .5f)
		receiver.drawScoreBadges(engine, 0, -3, 100, decoration)
		receiver.drawScoreBadges(engine, 5, -4, 100, decTemp)
		if(engine.stat==GameEngine.Status.SETTING||engine.stat==GameEngine.Status.RESULT&&!owner.replayMode) {
			if(!owner.replayMode&&startLevel==0&&!big&&!always20g
				&&engine.ai==null
			)
				if(!isShowBestSectionTime) {
					// Score Leaderboard
					receiver.drawScoreFont(engine, 0, 2, "HANABI SCORE TIME", COLOR.BLUE)

					ranking.forEachIndexed {i, (fw, st, _) ->
						receiver.drawScoreGrade(engine, 0, 3+i, "%2d".format(i+1), COLOR.YELLOW)
						receiver.drawScoreNum(engine, 2, 3+i, "$fw", i==rankingRank)
						receiver.drawScoreNum(engine, 6, 3+i, "${st.score}", i==rankingRank)
						receiver.drawScoreNum(engine, 12, 3+i, st.time.toTimeStr, i==rankingRank)
					}

					receiver.drawScoreFont(engine, 0, 24, "F:VIEW SECTION SCORE", COLOR.GREEN)
				} else {
					// Best Section Time Records
					receiver.drawScoreFont(engine, 0, 2, "SECTION SCORE TIME", COLOR.BLUE)

					val totalTime = bestSectionTime.sum()
					val totalScore = bestSectionScore.sum()
					val totalHanabi = bestSectionHanabi.sum()
					for(i in 0..<SECTION_MAX) {

						receiver.drawScoreNum(
							engine, 0, 3+i,
							"%3d${if(i==SECTION_MAX-1) "+" else "-"}".format(i*100), sectionIsNewRecord[i]
						)
						receiver.drawScoreNum(engine, 4, 3+i, "%4d".format(bestSectionHanabi[i]), sectionIsNewRecord[i])
						receiver.drawScoreNum(engine, 8, 3+i, "%6d".format(bestSectionScore[i]), sectionIsNewRecord[i])
						receiver.drawScoreNum(engine, 14, 3+i, bestSectionTime[i].toTimeStr, sectionIsNewRecord[i])
					}
					receiver.drawScoreFont(engine, 0, 4+SECTION_MAX, "ALL", COLOR.BLUE)
					receiver.drawScoreNum(engine, 4, 4+SECTION_MAX, "%4d".format(totalHanabi))
					receiver.drawScoreNum(engine, 8, 4+SECTION_MAX, "%6d".format(totalScore))
					receiver.drawScoreNum(engine, 14, 4+SECTION_MAX, totalTime.toTimeStr)
					receiver.drawScoreFont(engine, 0, 5+SECTION_MAX, "AVG", COLOR.BLUE)
					receiver.drawScoreNum(engine, 4, 5+SECTION_MAX, "%4d".format(totalHanabi/SECTION_MAX))
					receiver.drawScoreNum(engine, 8, 5+SECTION_MAX, "%6d".format(totalScore/SECTION_MAX))
					receiver.drawScoreNum(engine, 14, 5+SECTION_MAX, (totalTime/SECTION_MAX).toTimeStr)

					receiver.drawScoreFont(engine, 0, 17, "F:VIEW RANKING", COLOR.GREEN)
				}
		} else {
			val g20 = engine.speed.gravity<0&&rollTime%2==0
			receiver.drawScoreFont(engine, 0, 5, "Score", COLOR.BLUE)
			receiver.drawScoreNum(engine, 0, 6, "$scDisp", g20, 2f)
			receiver.drawScoreNum(engine, 5, 4, "$hanabi", g20||intHanabi>-100, 2f)

			receiver.drawScoreFont(engine, 0, 9, "Level", COLOR.BLUE)
			receiver.drawScoreNum(engine, 1, 10, "%3d".format(maxOf(engine.statistics.level, 0)))
			receiver.drawScoreSpeed(
				engine, 0, 11, if(g20) 40 else floor(ln(engine.speed.gravity.toDouble())).toInt()*4,
				4
			)
			receiver.drawScoreNum(engine, 1, 12, "300")

			receiver.drawScoreFont(engine, 0, 14, "Time", COLOR.BLUE)
			receiver.drawScoreNum(engine, 0, 15, engine.statistics.time.toTimeStr, 2f)

			if(engine.gameActive&&engine.ending==2) {
				val time = maxOf(0, ROLLTIMELIMIT-rollTime)
				receiver.drawScoreFont(engine, 0, 17, "ROLL TIME", COLOR.BLUE)
				receiver.drawScoreNum(engine, 0, 18, time.toTimeStr, time>0&&time<10*60, 2f)
			}

			// Section time
			if(showST&&sectionTime.isNotEmpty()) {
				val (x, x2) = if(receiver.nextDisplayType==2) 8 to 9 else 12 to 12

				receiver.drawScoreFont(engine, x-1, 2, "SECTION SCORE", COLOR.BLUE)

				for(i in sectionScore.indices)
					if(i<=sectionsDone) {
						var temp = i*100
						if(temp>=300) {
							temp = 300
							receiver.drawScoreFont(engine, x-1, 4+i, "BONUS", COLOR.BLUE)
							receiver.drawScoreNum(engine, x, 5+i, "%4d %d".format(sectionHanabi[i+1], sectionScore[i+1]))
						}

						val strSection = "%3d%s%4d %d".format(temp, if(i==sectionsDone) "+" else "-", sectionHanabi[i], sectionScore[i])

						receiver.drawScoreNum(engine, x, 3+i, strSection, sectionIsNewRecord[i])
					}

				receiver.drawScoreFont(engine, x2, 14, "AVERAGE", COLOR.BLUE)
				receiver.drawScoreNum(
					engine, x2, 15, (engine.statistics.time/(sectionsDone+if(engine.ending==0) 1 else 0)).toTimeStr, 2f
				)
			}
		}
	}

	/** This function will be called when the piece is active */
	override fun onMove(engine:GameEngine):Boolean {
		if(lastSpawnTime<engine.statistics.level) lastSpawnTime++
		return super.onMove(engine)
	}

	/** This function will be called during ARE */
	override fun onARE(engine:GameEngine):Boolean {
		lastSpawnTime = 0
		return super.onARE(engine)
	}

	/** Levelup */
	override fun levelUp(engine:GameEngine, lu:Int) {
		super.levelUp(engine, lu)

		if(engine.statistics.level>=nextSecLv) {
			nextSecLv += 100
			engine.playSE("levelup")

			sectionsDone++
			stNewRecordCheck(sectionsDone-1)
		}

		if(bgmLv==0&&engine.statistics.level>=280&&engine.ending==0) owner.musMan.fadeSW = true
	}

	/** Calculates line-clear score (This function will be called even if no
	 * lines are cleared) */
	override fun calcScore(engine:GameEngine, ev:ScoreEvent):Int {
		val li = ev.lines
		val pts = super.calcScore(engine, ev)
		if(li>=1) {
			halfMinLine += li
			val levelb = engine.statistics.level
			val combobonus = tableHanabiComboBonus[maxOf(0, minOf(ev.combo+if(ev.b2b>0) 0 else -1, tableHanabiComboBonus.size-1))]

			if(engine.ending==0) {
				levelUp(engine, li)

				if(engine.statistics.level>=300) {
					if(engine.timerActive) {
						val timeBonus = (1253*ceil(maxOf(18000-engine.statistics.time, 0)/60.0)).toInt()
						sectionScore[SECTION_MAX] = timeBonus.toLong()
						engine.statistics.scoreBonus += timeBonus
					}
					bonusSpeed = 3265/maxOf(1, hanabi)
					bgmLv++
					owner.musMan.fadeSW = false
					owner.musMan.bgm = BGM.GrandA(1)

					engine.statistics.level = 300
					engine.timerActive = false
					engine.ending = 2
					halfMinBonus = true
					halfMinLine = 0
				}
				(-(nextSecLv-100)/100).let {
					if(owner.bgMan.bg!=it) owner.bgMan.nextBg = it
				}
			}
			lastScore = 6*pts
			tempHanabi += maxOf(
				1, (
					when(li) {
						2 -> 2.9f
						3 -> 3.8f
						else -> if(li>=4) 4.7f else 1f
					}*combobonus*(if(ev.twistMini) 2 else if(ev.twist) 4 else 1)*(if(ev.split) 1.4f else 1f)
						*(if(intHanabi>-100) 1.3f else 1f)*(maxOf(engine.statistics.level/2, 100)/100f)
						*(maxOf(engine.statistics.level/2+70, 120)/120f)
						*(if(halfMinBonus) 1.4f else 1f)*(if(engine.ending==0&&(levelb%25==0||levelb==299)) 1.3f else 1f)
					).toInt()
			)
			halfMinBonus = false
			lastLineTime = 0
			if(sectionsDone>=0&&sectionsDone<sectionScore.size) sectionScore[sectionsDone] += lastScore.toLong()
			engine.statistics.scoreLine += lastScore
			return lastScore
		}
		return 0
	}

	/** This function will be called when the game timer updates */
	override fun onLast(engine:GameEngine) {
		super.onLast(engine)
		if(lastLineTime<100) lastLineTime++
		if(intHanabi>-100) intHanabi--
		if(tempHanabi>0&&intHanabi<=0) {
			receiver.shootFireworks(engine)
			hanabi++
			sectionHanabi[sectionsDone]++
			tempHanabi--
			intHanabi += GameEngine.HANABI_INTERVAL-minOf(intHanabi, 0)
		}
		if(engine.tempHanabi>0&&engine.intHanabi<=0) {
			hanabi++
			sectionHanabi[sectionsDone]++
		}
		// Increase section timer
		if(engine.timerActive&&engine.ending==0) {
			val section = engine.statistics.level/100
			if(section>=0&&section<sectionTime.size) sectionTime[section] = engine.statistics.time-sectionTime.take(section).sum()
			if(engine.statistics.time%1800==0) {
				if(halfMinLine>0) halfMinBonus = true
				halfMinLine = 0
			}
		}

		// Increase ending timer
		if(engine.gameActive&&engine.ending==2) {
			rollTime++
			bonusInt--
			if(bonusInt<=0) {
				receiver.shootFireworks(engine)
				hanabi++
				sectionHanabi[SECTION_MAX]++
				bonusInt += bonusSpeed
			}
			val remainRollTime = ROLLTIMELIMIT-rollTime
			engine.meterValue = remainRollTime*1f/ROLLTIMELIMIT
			engine.meterColor = GameEngine.METER_COLOR_LIMIT

			if(rollTime>=ROLLTIMELIMIT) {
				engine.gameEnded()
				engine.resetStatc()
				engine.stat = GameEngine.Status.EXCELLENT
			}
		}
	}

	override fun onExcellent(engine:GameEngine):Boolean {
		engine.statc[1] = tempHanabi
		return false
	}

	/** This function will be called when the player tops out */
	override fun onGameOver(engine:GameEngine):Boolean {
		if(engine.statc[0]==0) {
			secretGrade = engine.field.secretGrade
			intHanabi = 0
			tempHanabi = 0
			stNewRecordCheck(SECTION_MAX-1)
			if(engine.ending==2) {
				decTemp += hanabi/150
				decoration += decTemp+secretGrade
			}
		}
		return false
	}

	/** Renders game result screen */
	override fun renderResult(engine:GameEngine) {
		receiver.drawMenuFont(engine, 0, 0, "${BaseFont.UP_S}${BaseFont.DOWN_S} PAGE${engine.statc[1]+1}/3", COLOR.RED)

		if(engine.statc[1]==0) {
			receiver.drawMenuNum(engine, 0, 2, "%04d".format(hanabi), 2f)
			receiver.drawMenuFont(engine, 6, 3, "Score", COLOR.GREEN, .8f)
			receiver.drawMenuNum(engine, 0, 4, "%7d".format(engine.statistics.score), 1.9f)
			drawResultStats(engine, receiver, 6, COLOR.GREEN, Statistic.LINES, Statistic.LEVEL, Statistic.TIME)
			drawResultRank(engine, receiver, 13, COLOR.GREEN, rankingRank)
			if(secretGrade>4)
				drawResult(
					engine, receiver, 15, COLOR.GREEN, "S. GRADE",
					"%10s".format(tableSecretGradeName[secretGrade-1])
				)
		} else if(engine.statc[1]==1) {
			receiver.drawMenuFont(engine, 0, 2, "SECTION", COLOR.GREEN)
			receiver.drawMenuFont(engine, 0, 3, "Score", COLOR.GREEN)

			for(i in sectionScore.indices)
				receiver.drawMenuNum(
					engine, 1, (if(i==SECTION_MAX) 5 else 4)+i,
					"%4d:%d".format(sectionHanabi[i], sectionScore[i]), sectionIsNewRecord[i]
				)
			receiver.drawMenuFont(engine, 0, 4+SECTION_MAX, "BONUS", COLOR.GREEN)

			receiver.drawMenuFont(engine, 0, 7+SECTION_MAX, "Time", COLOR.GREEN)
			for(i in sectionTime.indices)
				if(sectionTime[i]>0)
					receiver.drawMenuNum(engine, 2, 8+SECTION_MAX+i, sectionTime[i].toTimeStr)

			if(sectionAvgTime>0) {
				receiver.drawMenuFont(engine, 0, 15, "AVERAGE", COLOR.GREEN)
				receiver.drawMenuNum(engine, 2, 16, sectionAvgTime.toTimeStr)
			}
		} else if(engine.statc[1]==2)
			drawResultStats(
				engine, receiver, 2, COLOR.GREEN, Statistic.LPM, Statistic.SPM, Statistic.PIECE, Statistic.PPS
			)
	}

	/** Additional routine for game result screen */
	override fun onResult(engine:GameEngine):Boolean {
		// Page change
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
		// Flip Leaderboard/Best Section Time Records
		if(engine.ctrl.isPush(Controller.BUTTON_F)) {
			engine.playSE("change")
			isShowBestSectionTime = !isShowBestSectionTime
		}

		return false
	}

	/** This function will be called when the replay data is going to be saved */
	override fun saveReplay(engine:GameEngine, prop:CustomProperties):Boolean {
		if(!owner.replayMode&&startLevel==0&&!always20g&&!big&&engine.ai==null) {
			owner.statsProp.setProperty("decoration", decoration)
			rankingRank = ranking.add(ScoreData(hanabi, engine.statistics, engine.ending))
			if(sectionAnyNewRecord) updateBestSectionTime()

			if(rankingRank!=-1||sectionAnyNewRecord) return true
		}
		return false
	}

	/** Updates best section time records */
	private fun updateBestSectionTime() {
		for(i in 0..<SECTION_MAX)
			if(sectionIsNewRecord[i]) {
				bestSectionScore[i] = sectionScore[i]
				bestSectionHanabi[i] = sectionHanabi[i]
				bestSectionTime[i] = sectionTime[i]
			}
	}

	companion object {
		/** Current version */
		private const val CURRENT_VERSION = 0

		/** Gravity table (Gravity speed value) */
		private val tableGravityValue =
			listOf(
				0 to 4, 8 to 5, 19 to 6, 35 to 8, 40 to 10, 50 to 12, 60 to 16, 70 to 32, 80 to 48, 90 to 64,
				100 to 4, 108 to 5, 119 to 6, 125 to 8, 131 to 12, 139 to 32, 149 to 48, 156 to 80, 164 to 112, 174 to 128, 180 to 144,
				200 to 16, 212 to 48, 221 to 80, 232 to 112, 244 to 144, 256 to 176, 267 to 192, 277 to 208, 287 to 224, 295 to 240,
				300 to -1
			)

		/** 段位 pointのCombo bonus */
		private val tableHanabiComboBonus = doubleArrayOf(1.0, 1.5, 1.9, 2.2, 2.9, 3.5, 3.9, 4.2, 4.5)

		/** Ending time limit */
		private const val ROLLTIMELIMIT = 3265

		/** Secret grade names */
		private val tableSecretGradeName = listOf(
			"S1", "S2", "S3", "S4", "S5", "S6", "S7", "S8", "S9", // 0-8
			"M1", "M2", "M3", "M4", "M5", "M6", "M7", "M8", "M9", // 9 - 17
			"GM" // 18
		)

		/** Number of sections */
		private const val SECTION_MAX = 4

		/** Default section time */
		private const val DEFAULT_SECTION_TIME = 6000
	}
}
