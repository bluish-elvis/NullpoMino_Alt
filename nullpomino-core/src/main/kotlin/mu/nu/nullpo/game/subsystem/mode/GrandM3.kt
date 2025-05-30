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
import mu.nu.nullpo.game.component.Controller
import mu.nu.nullpo.game.event.EventReceiver.COLOR
import mu.nu.nullpo.game.event.ScoreEvent
import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.game.subsystem.mode.menu.*
import mu.nu.nullpo.gui.common.BaseFont
import mu.nu.nullpo.util.CustomProperties
import mu.nu.nullpo.util.GeneralUtil.toInt
import mu.nu.nullpo.util.GeneralUtil.toTimeStr
import org.apache.logging.log4j.LogManager
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlin.random.Random

/** GRADE MANIA 3 Mode */
class GrandM3:AbstractGrand() {
	override val sectionMax get() = tableSectionMax.max()

	/** 内部 level */
	private var internalLevel = 0

	/** 最終結果などに表示される実際の段位 */
	private var grade = 0

	/** メインの段位 */
	private var gradeBasicReal = 0

	/** メインの段位の内部段位 */
	private var gradeBasicInternal = 0

	/** メインの段位の内部段位 point */
	private var gradeBasicPoint = 0

	/** メインの段位の内部段位 pointが1つ減る time */
	private var gradeBasicDecay = 0

	/** 最後に段位が上がった time */
	private var lastGradeTime = 0

	/** 現在のSectionにおいてTimeCOOLを達成するとtrue つぎせくｓ */
	private var cool = false

	/** COOL count */
	private val coolCount get() = coolSection.count {it}

	/** 直前のSection でCOOLを出したらtrue */
	private var previousCool = false

	/** このSection でCOOL check をしたらtrue */
	private var coolChecked = false

	/** このSection でCOOL表示をしたらtrue */
	private var coolDisplayed = false

	/** COOL display time frame count */
	private var coolDispFrame = 0

	/** REGRET display time frame count */
	private var regretDispFrame = 0

	/** COOL section flags */
	private val coolSection = MutableList(sectionMax) {false}

	/** REGRET section flags */
	private val regretSection = MutableList(sectionMax) {false}

	/** Roll 経過 time */
	private var rollTime = 0
	/** Roll completely cleared flag */
	private var rollClear = 0

	/** Roll started flag */
	private var rollStarted = false

	/** 裏段位 */
	private var secretGrade = 0

	/** 段位表示を光らせる残り frame count */
	private var gradeFlash = 0

	/** Section Cool Time x00-x70 */
	private val section70Time = MutableList(sectionMax) {0}

	/** 消えRoll started flag */
	private var mRollFlag = false

	/** Roll 中に稼いだ point (段位上昇用) */
	private var rollPoints = 0f

	/** Roll 中に稼いだ point (合計) */
	private var rollPointsTotal = 0f

	/** Section Time記録表示中ならtrue */
	private var isShowBestSectionTime = false

	private val itemGoal = StringsMenuItem(
		"goalType", "GOAL", COLOR.BLUE, 0,
		tableGoalLevel.map {"$it LEVEL"}
	)
	/** Game type  */
	private var goalType:Int by DelegateMenuItem(itemGoal)

	private val itemLevel = LevelGrandMenuItem(COLOR.BLUE)
	/** Level at start */
	private var startLevel:Int by DelegateMenuItem(itemLevel)

	private val item20g = BooleanMenuItem("always20g", "20G MODE", COLOR.BLUE, false)
	/** When true, always 20G */
	private var always20g:Boolean by DelegateMenuItem(item20g)

	private val itemBig = BooleanMenuItem("big", "BIG", COLOR.BLUE, false)
	/** BigMode */
	private var big:Boolean by DelegateMenuItem(itemBig)

	private val itemGrade = BooleanMenuItem("showgrade", "SHOW GRADE", COLOR.BLUE, false)
	/** 段位表示 */
	private var gradeDisp:Boolean by DelegateMenuItem(itemGrade)

	private val itemQualify = TimeMenuItem("lv500torikan", "QUALIFY", COLOR.BLUE, 27000, 0..72000)
	/** LV500の足切りTime */
	private var lv500Qualify:Int by DelegateMenuItem(itemQualify)

	private val itemExam = BooleanMenuItem("enableexam", "EXAMINATION", COLOR.YELLOW, true)
	/** 昇格・降格試験 is enabled */
	private var enableExam:Boolean by DelegateMenuItem(itemExam)

	override val menu = MenuList(
		"grademania3",
		itemGoal, itemGhost, itemAlert, itemST, itemGrade, itemExam, itemLevel, item20g, itemBig, itemQualify
	)
	/** Version */
	private var version = 0

	/** Current round's ranking position */
	private var rankingRank = 0

	/** Rankings' 段位 */
	private val rankingGrade = List(RANKING_TYPE) {MutableList(rankingMax) {0}}

	/** Rankings' levels */
	private val rankingLevel = List(RANKING_TYPE) {MutableList(rankingMax) {0}}

	/** Rankings' times */
	private val rankingTime = List(RANKING_TYPE) {MutableList(rankingMax) {-1}}

	/** Rankings' Roll completely cleared flag */
	private val rankingRollClear = List(RANKING_TYPE) {MutableList(rankingMax) {0}}

	/** Section Time記録 */
	private val bestSectionTime = List(RANKING_TYPE) {i ->
		MutableList(tableSectionMax[i]) {
			when(i) {
				0 -> tableTimeRegret[minOf(it, tableTimeRegret.lastIndex)]
				else -> tableQualifyPace(it, 27000)
			}
		}
	}

	/** 段位履歴 (昇格・降格試験用) */
	private val gradeHistory = List(RANKING_TYPE) {MutableList(GRADE_HISTORY_SIZE) {0}}

	override val propRank
		get() = rankMapOf(
			rankingGrade.mapIndexed {a, x -> "$a.grade" to x}+
				rankingLevel.mapIndexed {a, x -> "$a.level" to x}+
				rankingTime.mapIndexed {a, x -> "$a.time" to x}+
				rankingRollClear.mapIndexed {a, x -> "$a.rollClear" to x}+
				bestSectionTime.mapIndexed {a, x -> "$a.section.time" to x}+
				gradeHistory.mapIndexed {a, x -> "exam.history.$a" to x}
		)+rankMapOf("exam.record" to recordGrade, "exam.flaws" to demotionPoints)
	/*override val rankPersMap:Map<String, IntArray>
		get() = rankMap("grade" to rankingGrade, "level" to rankingLevel, "time" to rankingTime, "rollClear" to rankingRollClear,
			"section.time" to bestSectionTime, "exam.history" to gradeHistory, "exam.record" to examRecord)*/

	/** 昇格試験の目標段位 */
	private var promotionalExam = 0

	/** Current 認定段位 */
	private var recordGrade = MutableList(RANKING_TYPE) {0}

	/** 降格試験 point (30以上溜まると降格試験発生) */
	private var demotionPoints = MutableList(RANKING_TYPE) {0}

	/** 昇格試験 flag */
	private var promotionFlag = false

	/** 降格試験 flag */
	private var demotionFlag = false

	/** 降格試験での目標段位 */
	private var demotionExamGrade = 0

	/** 試験開始前演出の frame count */
	private var readyFrame = 0

	/** 試験終了演出の frame count */
	private var passFrame = 0

	/* Mode name */
	override val name = "Grand Mastery"
	override val gameIntensity = 1
	/** @return 何らかの試験中ならtrue
	 */
	private val isAnyExam get() = promotionFlag||demotionFlag

	/* Initialization */
	override fun playerInit(engine:GameEngine) {
		super.playerInit(engine)
		internalLevel = 0
		grade = 0
		gradeBasicReal = 0
		gradeBasicInternal = 0
		gradeBasicPoint = 0
		gradeBasicDecay = 0
		lastGradeTime = 0
		cool = false
		previousCool = false
		coolChecked = false
		coolDisplayed = false
		coolDispFrame = 0
		regretDispFrame = 0
		rollTime = 0
		rollClear = 0
		rollStarted = false
		secretGrade = 0
		gradeFlash = 0
		section70Time.fill(0)
		regretSection.fill(false)
		coolSection.fill(false)
		mRollFlag = false
		rollPoints = 0f
		rollPointsTotal = 0f

		demotionPoints.fill(0)
		recordGrade.fill(0)
		gradeHistory.forEach {it.fill(0)}
		promotionalExam = 0
		passFrame = 0
		readyFrame = 0
		demotionFlag = false
		promotionFlag = false
		demotionExamGrade = 0

		rankingRank = -1
		rankingGrade.forEach {it.fill(0)}
		rankingLevel.forEach {it.fill(0)}
		rankingTime.forEach {it.fill(-1)}
		rankingRollClear.forEach {it.fill(0)}
		bestSectionTime.forEachIndexed {x, it ->
			it.forEachIndexed {i, _ ->
				bestSectionTime[x][i] = tableTimeRegret[minOf(i, tableTimeRegret.lastIndex)]
			}
		}

		engine.twistEnable = true
		engine.twistEnableEZ = true
		engine.b2bEnable = true
		engine.splitB2B = true
		engine.frameSkin = GameEngine.FRAME_COLOR_SILVER
		engine.comboType = GameEngine.COMBO_TYPE_DOUBLE
		engine.bigHalf = true
		engine.bigMove = true
		engine.staffrollEnable = true
		engine.staffrollNoDeath = false

		if(!owner.replayMode) {
			loadSetting(engine, owner.modeConfig)

			version = CURRENT_VERSION
		} else {
			loadSetting(engine, owner.replayProp)
			version = owner.replayProp.getProperty("grademania3.version", 0)

			bestSectionTime.forEachIndexed {x, it ->
				it.forEachIndexed {i, _ ->
					bestSectionTime[x][i] = if(x==0) tableTimeRegret[minOf(i, tableTimeRegret.lastIndex)] else
						tableQualifyPace(i, 27000)
				}
			}

			if(enableExam) {
				promotionalExam = owner.replayProp.getProperty("grademania3.exam", 0)
				demotionPoints[goalType] = owner.replayProp.getProperty("grademania3.demopoint", 0)
				demotionExamGrade = owner.replayProp.getProperty("grademania3.demotionExamGrade", 0)
				if(promotionalExam>0) {
					promotionFlag = true
					readyFrame = 100
					passFrame = 600
				} else if(demotionPoints[goalType]>=30) {
					demotionFlag = true
					passFrame = 600
				}

				log.debug("** Exam data from replay START **")
				logExam()
				log.debug("*** Exam data from replay END ***")
			}
		}

		owner.bgMan.bg = 20+startLevel
		setSpeed(engine)
	}

	private fun logExam() {
		log.debug("Promotional Exam Grade:${getGradeName(promotionalExam)} ($promotionalExam)")
		log.debug("Promotional Exam Flag:$promotionFlag")
		log.debug("Demotion Points:${demotionPoints[goalType]}")
		log.debug("Demotional Exam Grade:${getGradeName(demotionExamGrade)} ($demotionExamGrade)")
		log.debug("Demotional Exam Flag:$demotionFlag")
	}

	/** Set BGM at start of game
	 */
	private fun calcBgmLv(lv:Int, rLv:Int = lv):Int =
		if(rLv>=900&&grade>=24&&coolCount>=9) BGM.GrandT().nums-1
		else tableBGMChange.count {lv>=it}.let {
			minOf(it+maxOf(0, it-4)+(it>=3&&coolCount>=3).toInt(), 6)
		}

	/** Update falling speed
	 * @param engine GameEngine
	 */
	override fun setSpeed(engine:GameEngine) {
		val mode = goalType
		val hazardTime = 54*(tableGoalLevel[mode]+1)
		engine.speed.gravity = if(always20g||engine.statistics.time>=hazardTime) -1
		else tableGravityValue[tableGravityChangeLevel.indexOfFirst {internalLevel<it}
			.let {if(it<0) tableGravityChangeLevel.size-1 else it}]


		if(engine.statistics.time>=hazardTime) {
			engine.speed.are = 2
			engine.speed.areLine = 1
			engine.speed.lineDelay = 3
			engine.speed.lockDelay = 13
			engine.speed.das = 5
		} else {
			engine.speed.are = tableARE[mode].let {it[minOf(it.size-1, internalLevel/100)]}
			engine.speed.areLine = tableARELine[mode].let {it[minOf(it.size-1, internalLevel/100)]}
			engine.speed.lineDelay = tableLineDelay[mode].let {it[minOf(it.size-1, internalLevel/100)]}
			engine.speed.lockDelay = tableLockDelay[mode].let {it[minOf(it.size-1, internalLevel/100)]}
			engine.speed.das = tableDAS.let {it[minOf(it.size-1, internalLevel/100)]}
		}
	}

	private fun stMedalCheck(engine:GameEngine, section:Int) {
		stMedalCheck(engine, section, sectionTime[section], bestSectionTime[goalType][section])
		checkRegret(engine, section, sectionTime[section])
	}
	/** COOLの check
	 * @param engine GameEngine
	 */
	private fun checkCool(engine:GameEngine) {
		val section = engine.statistics.level/100
		// COOL check
		if(engine.statistics.level%100>=70&&!coolChecked) {
			val coolPrevTime = if(section>0&&coolSection.any()) section70Time[coolSection.indexOfLast {it}] else 0

			if(section70Time[section]<=tableTimeCool[section]&&
				(!previousCool||section70Time[section]<=coolPrevTime+(200-section*9)))
				cool = true

			coolChecked = true
		}

		// COOL表示
		if(engine.statistics.level%100>=82&&cool&&!coolDisplayed) {
			engine.playSE("cool")
			coolSection[section] = true
			coolDispFrame = 180
			coolDisplayed = true
			decTemp += 2
		}
	}

	/** REGRETの check
	 * @param engine GameEngine
	 * @param section Line clear前の level
	 */
	private fun checkRegret(engine:GameEngine, section:Int, sectionLastTime:Int) {
		if(goalType!=0) return
		if(sectionLastTime>tableTimeRegret[section]) {
			previousCool = false
			grade--
			if(grade<0) grade = 0
			gradeFlash = 180
			decTemp -= 3
			regretDispFrame = 180
			engine.playSE("regret")
			regretSection[section] = true
		} else
			regretSection[section] = false
	}

	/** 段位名を取得
	 * @param g 段位 number
	 * @return 段位名(範囲外ならN / A)
	 */
	private fun getGradeName(g:Int):String = (if(goalType==0) tableGradeName else tableLongGradeName).getOrElse(g) {"N/A"}

	/* Called at settings screen */
	override fun onSetting(engine:GameEngine):Boolean {
		// Menu
		if(!engine.owner.replayMode) {
			// section time display切替
			if(engine.ctrl.isPush(Controller.BUTTON_F)) {
				engine.playSE("change")
				isShowBestSectionTime = !isShowBestSectionTime
			}
		}
		return super.onSetting(engine)
	}

	override fun onSettingChanged(engine:GameEngine) {
		owner.bgMan.bg = 20+startLevel
		engine.statistics.level = startLevel*100
		val lv = startLevel*100
		engine.statistics.level = lv
		internalLevel = lv
		nextSecLv = if(startLevel>=tableSectionMax[goalType]-1) tableGoalLevel[goalType]
		else maxOf(0, lv)+100
		setSpeed(engine)

		super.onSettingChanged(engine)
	}

	/* Called at game start */
	override fun startGame(engine:GameEngine) {
		val lv = startLevel*100
		engine.statistics.level = lv
		internalLevel = lv
		nextSecLv = if(startLevel>=tableSectionMax[goalType]-1) tableGoalLevel[goalType]
		else maxOf(0, lv)+100

		owner.bgMan.bg = 20+startLevel

		engine.big = big

		setSpeed(engine)
		owner.musMan.bgm = BGM.GrandT(calcBgmLv(lv))
	}

	override fun renderFirst(engine:GameEngine) {
		if(engine.gameActive&&engine.ending==2) receiver.drawStaffRoll(engine, rollTime*1f/ROLLTIMELIMIT)
	}
	/* Render score */
	override fun renderLast(engine:GameEngine) {
		receiver.drawScoreFont(engine, 0, 0, name, COLOR.CYAN)

		receiver.drawScoreFont(engine, -1, -4*2, "DECORATION", scale = .5f)
		receiver.drawScoreBadges(engine, 0, -3, 100, decoration)
		receiver.drawScoreBadges(engine, 5, -4, 100, decTemp)
		if(engine.stat==GameEngine.Status.SETTING||engine.stat==GameEngine.Status.RESULT&&!owner.replayMode) {
			if(startLevel==0&&!big&&!always20g&&!owner.replayMode&&engine.ai==null)
				if(!isShowBestSectionTime) {
					// Rankings

					receiver.drawScoreFont(engine, 0, 2, "GRADE TIME LEVEL", COLOR.BLUE)

					for(i in 0..<rankingMax) {
						receiver.drawScoreGrade(
							engine, 0, 3+i, "%2d".format(i+1), if(rankingRank==i) COLOR.RAINBOW else COLOR.YELLOW
						)
						receiver.drawScoreGrade(
							engine, 2, 3+i, getGradeName(rankingGrade[goalType][i]), when {
								rankingRollClear[goalType][i]==1||rankingRollClear[goalType][i]==3 -> COLOR.GREEN
								rankingRollClear[goalType][i]==2||rankingRollClear[goalType][i]==4 -> COLOR.ORANGE
								else -> COLOR.WHITE
							}
						)
						receiver.drawScoreNum(engine, 5, 3+i, rankingTime[goalType][i].toTimeStr, i==rankingRank)
						receiver.drawScoreNum(
							engine, 12, 3+i, "%0${if(goalType==0) 3 else 4}d".format(rankingLevel[goalType][i]),
							i==rankingRank
						)
					}
					receiver.drawScoreNano(engine, 0, 18, "CERTIFIED GRADE", COLOR.YELLOW)
					receiver.drawScoreGrade(engine, 0, 19, getGradeName(recordGrade[goalType]), scale = 2f,
						alpha = if(enableExam) 1f else .5f)
					if(promotionalExam>recordGrade[goalType]) {
						receiver.drawScoreNano(engine, 4, 19, "NEXT QUALIFY", COLOR.YELLOW)
						receiver.drawScoreFont(engine, 4, 20, BaseFont.CURSOR, alpha = if(enableExam) 1f else .5f)
						receiver.drawScoreGrade(engine, 5, 20, getGradeName(promotionalExam), scale = 1.67f,
							alpha = if(enableExam) 1f else .5f)
					}
					for(i in 0..<GRADE_HISTORY_SIZE)
						if(gradeHistory[goalType][i]>=0)
							receiver.drawScoreGrade(
								engine, -2, 15+i, getGradeName(gradeHistory[goalType][i]),
								if(gradeHistory[goalType][i]>recordGrade[goalType]) COLOR.YELLOW else COLOR.WHITE,
								alpha = if(enableExam) 1f else .5f
							)
					receiver.drawScoreNum(engine, -2, 16+GRADE_HISTORY_SIZE, "-${demotionPoints[goalType]}", COLOR.RED,
						alpha = if(enableExam) 1f else .5f)

					receiver.drawScoreFont(engine, 0, 17, "F:VIEW SECTION TIME", COLOR.GREEN)
				} else {
					// Section Time
					receiver.drawScoreFont(engine, 0, 2, "SECTION TIME", COLOR.BLUE)

					val totalTime = (0..<tableSectionMax[goalType]).fold(0) {_, i ->
						val slv = minOf(i*100, tableGoalLevel[goalType])
						receiver.drawScoreNum(
							engine, 0, 3+i, "%4d-%4d/%s".format(slv, slv+99, bestSectionTime[goalType][i].toTimeStr),
							sectionIsNewRecord[i]&&!isAnyExam
						)
						bestSectionTime[goalType][i]
					}

					receiver.drawScoreFont(engine, 13, 2, "TOTAL", COLOR.BLUE)
					receiver.drawScoreNum(engine, 13, 3, totalTime.toTimeStr, 2f)
					receiver.drawScoreFont(
						engine, if(receiver.nextDisplayType==2) 0 else 12, if(receiver.nextDisplayType==2) 18 else 14,
						"AVERAGE", COLOR.BLUE
					)
					receiver.drawScoreNum(
						engine, if(receiver.nextDisplayType==2) 0 else 12, if(receiver.nextDisplayType==2) 19 else 15,
						(totalTime/tableSectionMax[goalType]).toTimeStr, 2f
					)

					receiver.drawScoreFont(engine, 0, 17, "F:VIEW RANKING", COLOR.GREEN)
				}
		} else {
			val g20 = engine.speed.gravity<0
			if(promotionFlag) {
				// 試験段位
				receiver.drawScoreNano(engine, 0, 1, "QUALIFY", COLOR.ORANGE)
				receiver.drawScoreGrade(engine, 6, 1, getGradeName(promotionalExam))
			}
			val headCol = if(g20) COLOR.YELLOW else COLOR.BLUE
			if(gradeDisp) {
				receiver.drawScoreFont(engine, 0, 2, "GRADE", headCol)
				// 段位
				receiver.drawScoreGrade(engine, 0, 3, getGradeName(grade), gradeFlash>0&&gradeFlash%4==0, 2f)
				if(goalType==0) {
					val index = minOf(tableGradeDecayRate.size-1, gradeBasicInternal)
					receiver.drawScoreGrade(engine, 3, 3, getGradeName(index), gradeFlash>0&&gradeFlash%4==0)
					receiver.drawScoreSpeed(engine, 3, 3, gradeBasicPoint-gradeBasicDecay*1f/tableGradeDecayRate[index], 5f)
					receiver.drawScoreNum(
						engine, 6, 2, "%02.1f%%".format(gradeBasicPoint-gradeBasicDecay*1f/tableGradeDecayRate[index]),
						gradeFlash>0&&gradeFlash%4==0
					)
				} else {
					receiver.drawScoreNum(
						engine, 0, 3, "%05.1f%%".format(gradeBasicPoint-gradeBasicDecay/240f),
						gradeFlash>0&&gradeFlash%4==0
					)
				}
			}

			// Score
			receiver.drawScoreFont(engine, 0, 5, "Score", headCol)
			receiver.drawScoreNum(engine, 5, 5, "+$lastScore")
			receiver.drawScoreNum(engine, 0, 6, "$scDisp", 2f)

			// level
			receiver.drawScoreFont(engine, 0, 9, "Level", headCol)

			receiver.drawScoreNum(engine, 1, 10, "%3d".format(maxOf(engine.statistics.level, 0)))
			receiver.drawScoreSpeed(
				engine, 0, 11, if(g20) 40 else floor(ln(engine.speed.gravity.toDouble())).toInt()*4, 4
			)
			if(coolCount>0) {
				receiver.drawScoreFont(engine, 4, 11, "+")
				receiver.drawScoreGrade(engine, 5, 11, "%1d".format(coolCount))
			}
			receiver.drawScoreNum(engine, 1, 12, "%3d".format(nextSecLv))

			// Time
			receiver.drawScoreFont(engine, 0, 14, "Time", headCol)
			if(engine.ending!=2||rollTime/10%2==0)
				receiver.drawScoreNum(engine, 0, 15, engine.statistics.time.toTimeStr, 2f)

			// Roll 残り time
			if(engine.gameActive&&engine.ending==2) {
				val time = maxOf(ROLLTIMELIMIT-rollTime, 0)
				receiver.drawScoreFont(engine, 0, 17, "ROLL TIME", COLOR.CYAN)
				receiver.drawScoreNum(engine, 0, 18, time.toTimeStr, time>0&&time<10*60, 2f)
			}

			if(regretDispFrame>0)
			// REGRET表示
				receiver.drawMenuFont(
					engine, 2, 21, "REGRET", when {
						regretDispFrame%4==0 -> COLOR.YELLOW; regretDispFrame%4==2 -> COLOR.RED; else -> COLOR.ORANGE
					}
				)
			else if(coolDispFrame>0)
			// COOL表示
				receiver.drawMenuFont(
					engine, 2, 21, "COOL!!", when {
						coolDispFrame%4==0 -> COLOR.BLUE; coolDispFrame%2==0 -> COLOR.GREEN; else -> COLOR.CYAN
					}
				)

			// medal
			receiver.drawScoreMedal(engine, 0, 20, "AC", medalAC)
			receiver.drawScoreNum(engine, 2, 20, "%3d".format(engine.statistics.bravos))
			receiver.drawScoreMedal(engine, 5, 20, "ST", medalST)
			receiver.drawScoreNum(engine, 7, 20, medalsST.joinToString("."))
			receiver.drawScoreMedal(engine, 0, 21, "SK", medalSK)
			receiver.drawScoreNum(engine, 2, 21, "%3d".format(engine.statistics.totalQuadruple))
			receiver.drawScoreMedal(engine, 5, 21, "CO", medalCO)
			receiver.drawScoreNum(engine, 7, 21, "%3d".format(engine.statistics.maxCombo))

			// Section Time
			if(showST&&sectionTime.isNotEmpty()) {
				val x = receiver.nextDisplayType==2
				receiver.drawScoreFont(engine, if(x) 8 else 10, 2, "SECTION TIME", COLOR.BLUE)

				val section = engine.statistics.level/100
				sectionTime.forEachIndexed {i, it ->
					if(it>0) receiver.drawScoreNum(
						engine, if(x) 9 else 10, 3+i, "%3d%s%s %02.2f".format(
							minOf(i*100, tableGoalLevel[goalType]),
							if(i==section&&engine.ending==0) "+" else "-", it.toTimeStr, section70Time[i]/60f
						), when {
							regretSection[i] -> if(sectionIsNewRecord[i]) COLOR.ORANGE else COLOR.RED
							coolSection[i] -> if(sectionIsNewRecord[i]) COLOR.CYAN else COLOR.GREEN
							else -> COLOR.WHITE
						}, if(x) .75f else 1f
					)
				}
				receiver.drawScoreFont(engine, if(x) 8 else 12, if(x) 11 else 14, "AVERAGE", COLOR.BLUE)
				receiver.drawScoreNum(
					engine, if(x) 8 else 12, if(x) 12 else 15,
					(engine.statistics.time/(sectionsDone+(engine.ending==0).toInt())).toTimeStr,
					2f
				)
			}
		}
	}

	/* Ready→Goの処理 */
	override fun onReady(engine:GameEngine):Boolean {
		if(!owner.replayMode&&engine.statc[0]==0) {
			isShowBestSectionTime = false
			sectionsDone = 0
			if(!always20g&&!big&&enableExam) {
				log.debug("** Exam debug log START **")
				log.debug("Current Qualified Grade:${getGradeName(recordGrade[goalType])} (${recordGrade[goalType]})")
				log.debug("Current Demotion Points:${demotionPoints[goalType]}")
				if(demotionPoints[goalType]>=30) {
					demotionFlag = true
					demotionExamGrade = recordGrade[goalType]
					passFrame = 600
					demotionPoints[goalType] = 0
				} else {
					setPromotionalGrade(goalType)
					log.debug("Promotional Chance Grade:${getGradeName(promotionalExam)} ($promotionalExam)")
					if(promotionalExam>recordGrade[goalType]) {
						val rand = Random.Default.nextInt(EXAM_CHANCE)
						log.debug("Promotional Chance RNG:$rand (1 / $EXAM_CHANCE)")
						if(rand==0) {
							promotionFlag = true
							readyFrame = 100
							passFrame = 600
						}
					}
				}
				log.debug("*** Exam debug log END ***")
			}
		}
		if(promotionFlag) {
			engine.frameSkin = GameEngine.FRAME_SKIN_GRADE

			if(readyFrame==100) engine.playSE("item_spawn")

			if(engine.ctrl.isPush(Controller.BUTTON_A)||engine.ctrl.isPush(Controller.BUTTON_B)) readyFrame = 0

			if(readyFrame>0) {
				readyFrame--
				return true
			}
		} else if(demotionFlag) {
			engine.frameSkin = GameEngine.FRAME_COLOR_WHITE
			engine.playSE("item_trigger")
		}

		return false
	}

	/* Ready→Goのときの描画 */
	override fun renderReady(engine:GameEngine) {
		if(promotionFlag&&readyFrame>0) {
			receiver.drawMenuFont(engine, 0, 2, "PROMOTION", COLOR.YELLOW)
			receiver.drawMenuFont(engine, 6, 3, "EXAM", COLOR.YELLOW)
			receiver.drawMenuGrade(
				engine, 2, 5.66f, getGradeName(promotionalExam), if(readyFrame%4==0) COLOR.ORANGE else COLOR.WHITE,
				2f
			)
		}
	}

	/* 移動中の処理 */
	override fun onMove(engine:GameEngine):Boolean {
		super.onMove(engine)
		// 段位 point減少
		if(engine.timerActive&&gradeBasicPoint>0&&engine.combo<0&&engine.lockDelayNow<engine.lockDelay-1) {
			gradeBasicDecay += if(goalType==0) 1 else (gradeBasicReal+2)

			val rate = if(goalType==0) tableGradeDecayRate[minOf(gradeBasicInternal, tableGradeDecayRate.size-1)]
			else 240

			if(gradeBasicDecay>=rate) {
				gradeBasicDecay = 0
				gradeBasicPoint--
			}
		}

		// Endingスタート
		if(engine.ending==2&&!rollStarted) {
			rollStarted = true

			if(mRollFlag) {
				engine.blockHidden = maxOf(2, engine.ruleOpt.lockFlash)
				engine.blockHiddenAnim = false
				engine.blockShowOutlineOnly = true
			} else {
				engine.blockHidden = 300
				engine.blockHiddenAnim = true
			}

			owner.musMan.bgm = BGM.Ending(2)
		}

		return false
	}

	/** levelが上がったときの共通処理 */
	override fun levelUp(engine:GameEngine, lu:Int) {
		val lb = internalLevel
		super.levelUp(engine, lu)
		internalLevel += lu
		// COOL check
		checkCool(engine)


		if(lu<=0) return
		// BGM fadeout
		val lA = internalLevel+cool.toInt()*100
		if(tableBGMFadeout.any {it in lb..lA}||
			900 in (engine.statistics.level-lu)..engine.statistics.level&&grade>=23&&coolCount>=9)
			owner.musMan.fadeSW = true
		// BGM切り替え
		if(owner.musMan.fadeSW&&tableBGMChange.any {it in lb..lA}) {
			owner.musMan.fadeSW = false
			owner.musMan.bgm = BGM.GrandT(calcBgmLv(lA, engine.statistics.level))
		}
	}

	/* Calculate score */
	override fun calcScore(engine:GameEngine, ev:ScoreEvent):Int {
		// Combo
		val li = ev.lines
		// Calculate score
		val pts = super.calcScore(engine, ev)

		val ret = if(li>=1&&engine.ending==0) {
			if(goalType==0) {
				// 段位 point
				val index = minOf(gradeBasicInternal, tableGradePoint[li-1].size-1)

				val basePoint = tableGradePoint[li-1][index]
				val indexCombo = (ev.combo+if(ev.b2b>0) 0 else -1).coerceIn(0, tableGradeComboBonus[li-1].size-1)
				val comboBonus = tableGradeComboBonus[li-1][indexCombo]

				val levelBonus = 1+engine.statistics.level/250

				val point = basePoint.toFloat()*comboBonus*levelBonus.toFloat()
				gradeBasicPoint += point.toInt()

				// 内部段位上昇
				while(gradeBasicPoint>=100) {
					gradeBasicPoint -= 100
					gradeBasicDecay = 0
					gradeBasicInternal++
					engine.playSE("cool")
					coolDispFrame = 180
					if(tableGradeChange[gradeBasicReal]!=-1&&gradeBasicInternal>=tableGradeChange[gradeBasicReal]) {
						decTemp++
						gradeBasicReal++
						if(grade<31) {
							grade = minOf(31, gradeBasicReal+coolCount)
							engine.playSE(if(gradeDisp) "grade${grade/8}" else "medal1")
							gradeFlash = 180
							lastGradeTime = engine.statistics.time
						}
					}
				}
			}

			// Level up
			val levelb = engine.statistics.level
			val section = levelb/100
			levelUp(engine, li.let {it+maxOf(0, it-2)})

			if(engine.statistics.level>=tableGoalLevel[goalType]) {
				// Ending
				engine.statistics.level = tableGoalLevel[goalType]
				engine.timerActive = false
				engine.ending = 1
				rollClear = 1

				lastGradeTime = engine.statistics.time

				sectionsDone++
				// ST medal/REGRET
				stMedalCheck(engine, section)

				// 条件を全て満たしているなら消えRoll 発動
				if(when(goalType) {
						0 -> grade>=24&&coolCount>=9
						else -> grade>=50
					}) {
					mRollFlag = true
					engine.playSE("applause4")
				} else engine.playSE("applause3")
			} else {
				val torikan = if(lv500Qualify>0&&!promotionFlag&&!demotionFlag)
					tableQualifyTimeMul.firstOrNull {(s, mul) ->
						nextSecLv==s*100&&engine.statistics.level>=s*100&&engine.statistics.time>lv500Qualify*mul
					}?.first else null
				if(engine.statistics.level>=nextSecLv) {
					sectionsDone++
					// ST medal/REGRET
					stMedalCheck(engine, section)

					if(torikan==5||goalType==1&&(torikan==15||nextSecLv==999||nextSecLv==2000)) {
						// level500,1500とりカン
						engine.statistics.level = nextSecLv
						engine.gameEnded()
						engine.staffrollEnable = false
						engine.ending = 1
						engine.playSE("applause2")

						secretGrade = engine.field.secretGrade
						lastGradeTime = engine.statistics.time
					} else {
						// Next Section
						// Background切り替え
						owner.bgMan.nextBg = 20+nextSecLv/100

						engine.playSE("levelup")
						// COOLを取ってたら
						if(cool&&!regretSection[section]) {
							previousCool = true
							if(grade<31) {
								grade++
								gradeFlash = 180
								lastGradeTime = engine.statistics.time
							}
							internalLevel += 100
						} else previousCool = false

						cool = false
						coolChecked = false
						coolDisplayed = false
						// Update level for next section
						nextSecLv = when {
							goalType>0&&torikan==9 -> 999
							torikan==19 -> 2000
							nextSecLv>tableGoalLevel[goalType]||sectionsDone>=tableSectionMax[goalType]-1 -> tableGoalLevel[goalType]
							else -> nextSecLv+100
						}
					}
				}
			}
			lastScore = pts
			engine.statistics.scoreLine += pts
			pts
		} else if(li>=1&&engine.ending==2&&goalType==0) {
			// Roll 中のLine clear
			val points = if(!mRollFlag||engine.twist||ev.b2b>0) when {
				li<=1 -> 0.04f
				li==2 -> 0.09f
				li==3 -> 0.15f
				else -> 0.27f
			} else when {
				li<=1 -> .1f
				li==2 -> .2f
				li==3 -> .5f
				else -> 1f
			}
			rollPoints += points
			rollPointsTotal += points
			while(rollPoints>=1f&&grade<31) {
				rollPoints -= 1f
				grade = minOf(31, gradeBasicReal+coolCount+rollPointsTotal.toInt())
				gradeFlash = 180
			}
			(points*100).toInt()
		} else 0
		if(li>=1&&goalType==1) {
			gradeBasicPoint += maxOf(
				li*10, when(li) {
					1 -> 2; 2 -> 6; 3 -> 12; else -> 24
				}*(1+internalLevel/200),
				when(li) {
					1 -> 1; 2 -> 4; 3 -> 9; else -> 20
				}*(internalLevel/100-2),
				if(engine.statistics.level>=1000&&li>=4) gradeBasicInternal*30 else 0
			)
			gradeBasicInternal = minOf(30, (sqrt((gradeBasicPoint/50)*8+1.0)/2-.5).toInt())
			if(gradeBasicInternal>gradeBasicReal) {
				gradeBasicReal = gradeBasicInternal
				grade = minOf(50, gradeBasicReal+coolCount)
				engine.playSE("cool")
				coolDispFrame = 180
				gradeFlash = 180
				lastGradeTime = engine.statistics.time
			}
		}
		if(gradeFlash==180) engine.playSE(if(gradeDisp) "grade${grade/8}" else "medal1")
		return ret
	}

	/* 各 frame の終わりの処理 */
	override fun onLast(engine:GameEngine) {
		super.onLast(engine)
		// 段位上昇時のフラッシュ
		if(gradeFlash>0) gradeFlash--

		// REGRET表示
		if(regretDispFrame>0) regretDispFrame--

		// COOL表示
		if(coolDispFrame>0) coolDispFrame--

		// 15分経過
		if(engine.statistics.time>=54000) setSpeed(engine)

		// Section Time増加
		if(engine.timerActive&&engine.ending==0) {
			val section = engine.statistics.level/100

			if(section>=0&&section<sectionTime.size)
				(engine.statistics.time-sectionTime.take(section).sumOf {it}).let {
					sectionTime[section] = it
					if(engine.statistics.level%100<70) section70Time[section] = it
				}

			if(engine.statistics.level==nextSecLv-1)
				engine.meterColor = if(engine.meterColor==-0x1) -0x10000 else -0x1
		}

		// Ending
		if(engine.gameActive&&engine.ending==2) {
			rollTime++

			// Time meter
			val remainRollTime = ROLLTIMELIMIT-rollTime
			engine.meterValue = remainRollTime*1f/ROLLTIMELIMIT
			engine.meterColor = GameEngine.METER_COLOR_LIMIT

			// Roll 終了
			if(rollTime>=ROLLTIMELIMIT) {
				rollClear = 2

				secretGrade = engine.field.secretGrade
				if(goalType==0) {
					if(!mRollFlag) {
						rollPoints += .5f
						rollPointsTotal += .5f
					} else {
						rollPoints += 1.6f
						rollPointsTotal += 1.6f
					}

					while(rollPoints>=1f) {
						rollPoints -= 1f
						if(grade<32) {
							grade++
							gradeFlash = 180
						}
					}
				} else {
					if(mRollFlag&&gradeBasicPoint>=25000) {
						grade = 51
						gradeFlash = 180
					}
				}
				if(gradeDisp&&gradeFlash==180) engine.playSE("grade${grade/8}")

				engine.blockOutlineType = GameEngine.BLOCK_OUTLINE_NORMAL
				engine.blockShowOutlineOnly = false

				engine.gameEnded()
				engine.resetStatc()
				engine.stat = GameEngine.Status.EXCELLENT
				val timeQ = 28800*(goalType+1)
				if(engine.statistics.time<timeQ) decTemp += (timeQ-engine.statistics.time)/1800
			}
		}
	}

	/* game over */
	override fun onGameOver(engine:GameEngine):Boolean {
		// This code block will execute only once
		if(engine.statc[0]==0) {
			secretGrade = engine.field.secretGrade
			val time = engine.statistics.time
			if(time<6000) decTemp -= 3
			else {
				decTemp++
				if(time%3600<=60||time%3600>=3540) decTemp++
			}

			if(time>41100) decTemp -= 1+(time-41100)/1800
			if(enableExam) {
				log.debug("** Exam result log START **")
				log.debug("Grade Result/Record:${getGradeName(grade)} ($grade) / ${
					getGradeName(recordGrade[goalType])
				} (${recordGrade[goalType]})")
				log.debug(
					"Promotional Exam: {}",
					if(promotionFlag) "${getGradeName(promotionalExam)} ($promotionalExam)" else "disabled"
				)
				if(promotionFlag&&grade>=promotionalExam) {
					recordGrade[goalType] = promotionalExam
					demotionPoints[goalType] = 0
					decTemp += 6
					engine.tempHanabi += 24
				} else {
					val demo = recordGrade[goalType]-grade-7
					if(demo>0) demotionPoints[goalType] += demo
					log.debug("Demotion Points:${demotionPoints[goalType]} (+ ${maxOf(0, demo)})")
				}
				log.debug("Demotion Exam:$demotionFlag")
				if(demotionFlag&&grade<demotionExamGrade) {
					recordGrade[goalType] = maxOf(0, demotionExamGrade-1)
					decTemp -= 10
					log.debug("Demoted into:${getGradeName(recordGrade[goalType])} (${recordGrade[goalType]})")
				}

				log.debug("*** Exam result log END ***")
			}
			decoration += decTemp+secretGrade
		}

		return false
	}

	/* 結果画面 */
	override fun renderResult(engine:GameEngine) {
		if(passFrame>0) {
			if(promotionFlag) {
				receiver.drawMenuFont(engine, 0, 2, "PROMOTION", COLOR.YELLOW)
				receiver.drawMenuFont(engine, 1, 3, "CHANCE TO", COLOR.YELLOW)
				receiver.drawMenuGrade(
					engine, 2, 5.66f, getGradeName(promotionalExam), if(passFrame%4==0) COLOR.ORANGE else COLOR.WHITE,
					2f
				)

				if(passFrame<420)
					if(grade<promotionalExam)
						receiver.drawMenuFont(
							engine, 3, 11, "FAIL", when {
								passFrame%4==0 -> COLOR.YELLOW
								passFrame%4==2 -> COLOR.RED
								else -> COLOR.ORANGE
							}
						)
					else {
						receiver.drawMenuFont(
							engine, 2, 11, "PASS!!", when {
								passFrame%4==0 -> COLOR.GREEN
								passFrame%4==2 -> COLOR.BLUE
								else -> COLOR.CYAN
							}
						)
						receiver.drawMenuFont(
							engine, 1, 5, "YOU ARE", when {
								passFrame%4==0 -> COLOR.GREEN
								passFrame%4==2 -> COLOR.BLUE
								else -> COLOR.CYAN
							}
						)
					}
			} else if(demotionFlag) {
				receiver.drawMenuFont(engine, 0, 2, "DEMOTION", COLOR.RED)
				receiver.drawMenuFont(engine, 6, 3, "PINCH OF", COLOR.RED)
				receiver.drawMenuGrade(
					engine, 2, 5.66f, getGradeName(demotionExamGrade), if(passFrame%4==0) COLOR.RED else COLOR.WHITE,
					2f
				)
				if(passFrame<420)
					if(grade<demotionExamGrade)
						receiver.drawMenuFont(
							engine, 2, 11, "FUCKED", when {
								passFrame%4==0 -> COLOR.YELLOW
								passFrame%4==2 -> COLOR.RED
								else -> COLOR.ORANGE
							}
						)
					else
						receiver.drawMenuFont(
							engine, 3, 11, "SAFE", when {
								passFrame%4==0 -> COLOR.GREEN
								passFrame%4==2 -> COLOR.BLUE
								else -> COLOR.CYAN
							}
						)
			}
		} else {
			receiver.drawMenuFont(engine, 0, 0, "${BaseFont.UP_S}${BaseFont.DOWN_S} PAGE${engine.statc[1]+1}/3", COLOR.RED)

			when(engine.statc[1]) {
				0 -> {
					receiver.drawMenuFont(engine, 0, 3, "GRADE", COLOR.BLUE)
					receiver.drawMenuGrade(
						engine, 6, 1.66f, getGradeName(grade), when {
							grade>=32&&engine.statc[2]%2==0 -> COLOR.YELLOW
							rollClear==1||rollClear==3 -> COLOR.GREEN
							rollClear==2||rollClear==4 -> COLOR.ORANGE
							else -> COLOR.WHITE
						}, 2f
					)

					drawResultStats(
						engine, receiver, 4, COLOR.BLUE, Statistic.SCORE, Statistic.LINES, Statistic.LEVEL_MANIA, Statistic.TIME
					)
					drawResultRank(engine, receiver, 12, COLOR.BLUE, rankingRank)
					if(secretGrade>4)
						drawResult(
							engine, receiver, 15, COLOR.BLUE, "S. GRADE",
							"%10s".format(tableSecretGradeName[secretGrade-1])
						)
				}
				1 -> {
					receiver.drawMenuFont(engine, 0, 2, "SECTION", COLOR.BLUE)

					sectionTime.forEachIndexed {i, it ->
						if(it>0) receiver.drawMenuNum(
							engine, 2, 3+i, it.toTimeStr,
							if(regretSection[i]) if(sectionIsNewRecord[i]) COLOR.ORANGE else COLOR.RED
							else if(coolSection[i]) if(sectionIsNewRecord[i]) COLOR.CYAN else COLOR.GREEN
							else COLOR.WHITE
						)
					}

					if(sectionAvgTime>0) {
						receiver.drawMenuFont(engine, 0, 14, "AVERAGE", COLOR.BLUE)
						receiver.drawMenuNum(engine, 0, 15, sectionAvgTime.toTimeStr, 1.7f)
					}
				}
				2 -> {
					receiver.drawMenuNano(engine, 0, 1.5f, "MEDAL", COLOR.BLUE, .5f)
					receiver.drawMenuMedal(engine, 2, 2, "AC", medalAC)
					receiver.drawMenuMedal(engine, 5, 2, "ST", medalST)
					receiver.drawMenuMedal(engine, 8, 2, "SK", medalSK)
					receiver.drawMenuMedal(engine, 1, 3, "RE", medalRE)
					receiver.drawMenuMedal(engine, 4, 3, "RO", medalRO)
					receiver.drawMenuMedal(engine, 7, 3, "CO", medalCO)

					if(rollPointsTotal>0) {
						receiver.drawMenuFont(engine, 0, 4, "ROLL POINT", COLOR.BLUE)
						receiver.drawMenuNum(engine, 0, 5, rollPointsTotal, 10 to null)
					}

					drawResultStats(
						engine, receiver, 6, COLOR.BLUE, Statistic.LPM, Statistic.SPM, Statistic.PIECE, Statistic.PPS
					)
					drawResult(engine, receiver, 15, COLOR.BLUE, "DECORATION", "%d".format(decTemp))
				}
			}
		}
	}

	/* 結果画面の処理 */
	override fun onResult(engine:GameEngine):Boolean {
		if(passFrame>0) {
			engine.allowTextRenderByReceiver = false // Turn off RETRY/END menu

			if(engine.ctrl.isPush(Controller.BUTTON_A)||engine.ctrl.isPush(Controller.BUTTON_B))
				if(passFrame>420) passFrame = 420
				else if(passFrame<300) passFrame = 0

			if(promotionFlag) {
				if(passFrame==420) {
					engine.playSE("linefall0")
					if(grade>=promotionalExam) {
						engine.playSE("excellent")
						engine.playSE("applause5")
					} else engine.playSE("regret")
				}
			} else if(demotionFlag)
				if(passFrame==420) {
					engine.playSE("linefall0")
					if(grade>=demotionExamGrade) {
						engine.playSE("grade0")
						engine.playSE("applause2")
					} else {
						engine.playSE("regret")
						engine.playSE("dead_last")
					}
				}

			passFrame--
			return true
		}
		engine.statistics.time = lastGradeTime
		engine.allowTextRenderByReceiver = true

		owner.musMan.fadeSW = false
		owner.musMan.bgm = when {
			engine.ending==1||engine.ending==2&&rollClear==0 -> BGM.Result(2)
			rollClear>0||promotionFlag&&grade>=promotionalExam -> BGM.Result(3)
			demotionFlag&&grade<=demotionExamGrade -> BGM.Result(1)
			else -> BGM.Result(0)
		}

		// ページ切り替え
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
		// section time display切替
		if(engine.ctrl.isPush(Controller.BUTTON_F)) {
			engine.playSE("change")
			isShowBestSectionTime = !isShowBestSectionTime
		}

		engine.statc[2]++

		return false
	}

	/* リプレイ保存 */
	override fun saveReplay(engine:GameEngine, prop:CustomProperties):Boolean {
		owner.replayProp.setProperty("result.grade.name", getGradeName(grade))
		owner.replayProp.setProperty("result.grade.number", grade)
		owner.replayProp.setProperty("grademania3.version", version)
		owner.replayProp.setProperty("grademania3.exam", if(promotionFlag) promotionalExam else 0)
		owner.replayProp.setProperty("grademania3.demopoint", demotionPoints)
		owner.replayProp.setProperty("grademania3.demotionExamGrade", demotionExamGrade)

		// Update rankings
		if(!owner.replayMode&&startLevel==0&&!always20g&&!big&&engine.ai==null) {
			owner.statsProp.setProperty("decoration", decoration)
			// if(!enableexam || !isAnyExam())
			updateRanking(goalType, grade, engine.statistics.level, lastGradeTime, rollClear)
			// else
			// rankingRank = -1;

			if(enableExam) updateGradeHistory(grade, goalType)

			/** Update best section time records */
			if(medalST==3)
				for(i in 0..<tableSectionMax[goalType])
					if(sectionIsNewRecord[i]) bestSectionTime[goalType][i] = sectionTime[i]

			if(rankingRank!=-1||enableExam||medalST==3) return true
		}
		return false
	}

	/** Update rankings
	 * @param t Goal Type
	 * @param gr 段位
	 * @param lv level
	 * @param time Time
	 */
	private fun updateRanking(t:Int, gr:Int, lv:Int, time:Int, clear:Int) {
		rankingRank = checkRanking(t, gr, lv, time, clear)

		if(rankingRank!=-1) {
			// Shift down ranking entries
			for(i in rankingMax-1 downTo rankingRank+1) {
				rankingGrade[t][i] = rankingGrade[t][i-1]
				rankingLevel[t][i] = rankingLevel[t][i-1]
				rankingTime[t][i] = rankingTime[t][i-1]
				rankingRollClear[t][i] = rankingRollClear[t][i-1]
			}

			// Add new data
			rankingGrade[t][rankingRank] = gr
			rankingLevel[t][rankingRank] = lv
			rankingTime[t][rankingRank] = time
			rankingRollClear[t][rankingRank] = clear
		}
	}

	/** Calculate ranking position
	 * @param t Goal Type
	 * @param gr 段位
	 * @param lv level
	 * @param time Time
	 * @return Position (-1 if unranked)
	 */
	private fun checkRanking(t:Int, gr:Int, lv:Int, time:Int, clear:Int):Int {
		for(i in 0..<rankingMax)
			if(gr>rankingGrade[t][i]) return i
			else if(gr==rankingGrade[t][i]&&clear>rankingRollClear[t][i]) return i
			else if(gr==rankingGrade[t][i]&&clear==rankingRollClear[t][i]&&lv>rankingLevel[t][i]) return i
			else if(gr==rankingGrade[t][i]&&clear==rankingRollClear[t][i]&&lv==rankingLevel[t][i]&&time<rankingTime[t][i]) return i

		return -1
	}

	/** 段位履歴を更新
	 * @param gr 段位
	 */
	private fun updateGradeHistory(gr:Int, gt:Int) {
		gradeHistory[gt].add(gr)
		if(gradeHistory[gt].size>=GRADE_HISTORY_SIZE) gradeHistory[gt].removeFirst()
		// Debug log
		log.debug("** Exam grade history START **")
		for(i in gradeHistory[gt].indices)
			log.debug("$i: ${getGradeName(gradeHistory[gt][i])} (${gradeHistory[gt][i]})")

		log.debug("*** Exam grade history END ***")
		setPromotionalGrade(gt)
	}

	/** 昇格試験の目標段位を設定 */
	private fun setPromotionalGrade(gt:Int) {
		promotionalExam = gradeHistory[gt].filter {it>recordGrade[gt]}.let {
			if(it.size>3) minOf(if(recordGrade[gt]<31) 31 else 32, it.average().roundToInt())
			else recordGrade[gt]
		}
	}

	companion object {
		/** Log */
		internal val log = LogManager.getLogger()

		/** Current version */
		private const val CURRENT_VERSION = 2

		/** Section COOL criteria Time */
		private val tableTimeCool = listOf(
			3360, 3280, 3200, 3110, 3020, 2930, 2835, 2740, 2645, 2550,
			2520, 2490, 2460, 2430, 2400, 2370, 2340, 2310, 2280, 2250
		)
		/** Section REGRET criteria Time */
		private val tableTimeRegret = listOf(6000, 5400, 5100, 4800, 4600, 4400, 4300, 4200, 4100, 4000)
		/** 落下速度 table */
		private val tableGravityValue =
			listOf(
				4, 6, 8, 10, 12, 16, 32, 48, 64, 80, 96, 112, 128, 144, 4, 32, 64, 96, 128, 160, 192, 224, 256, 512, 768, 1024,
				1280, 1024, 768, -1
			)
		/** 落下速度が変わる level */
		private val tableGravityChangeLevel =
			listOf(
				30, 35, 40, 50, 60, 70, 80, 90, 100, 120, 140, 160, 170, 200, 220, 230, 233, 236, 239, 243, 247, 251, 300, 330,
				360, 400, 420, 450, 500, 10000
			)

		/** ARE table */
		private val tableARE = listOf(
			listOf(25, 24, 23, 22, 21, 20, 18, 15, 11, 8, 6, 5, 4),
			listOf(
				25, 22, 20, 18, 16, 15, 14, 13, 12, 11,
				10, +9, +8, +7, +6, +6, +5, +5, +4, 4,
			)
		)
		/** ARE after line clear table */
		private val tableARELine = listOf(
			listOf(25, 25, 25, 24, 22, 19, 16, 12, 9, 7, 6, 5, 4),
			tableARE[1]
		)
		/** Line clear times table */
		private val tableLineDelay = listOf(
			listOf(40, 39, 37, 34, 30, 25, 16, 12, 6),
			listOf(
				40, 30, 25, 20, 15, 13, 12, 10, 10, +8,
				+6, +6, +5, +4, +4, +3, +2
			)
		)
		/** 固定 times table */
		private val tableLockDelay = listOf(
			listOf(30, 30, 30, 30, 30, 30, 29, 28, 27, 25, 24, 22, 20),
			listOf(
				30, 30, 30, 30, 30, 29, 28, 27, 26, 26,
				25, 25, 24, 24, 24, 23, 23, 23, 22
			)
		)
		/** DAS table */
		private val tableDAS = listOf(
			15, 14, 13, 12, 11, 10, 9, 8, 7, 7,
			+6, +6, +6, +6, +5, +5, 4
		)

		/** BGM change level */
		private val tableBGMChange = listOf(200, 500, 800, 1000)
		/** BGM fadeout level */
		private val tableBGMFadeout = tableBGMChange.map {it-15}
		/** BGM change level in exam */
		private val tableBGMXChange = listOf(300, 600, 800)
		/** BGM fadeout level in exam */
		private val tableBGMXFadeout = tableBGMXChange.map {it-15}
		/** Line clear時に入る段位 point */
		private val tableGradePoint =
			listOf(
				listOf(10, 10, 10, 10, 10, 9, 8, 7, 6, 5, 4, 3, 2),
				listOf(20, 20, 20, 18, 16, 15, 13, 10, 11, 11, 12),
				listOf(40, 36, 33, 30, 27, 24, 20, 18, 17, 16, 15),
				listOf(50, 47, 44, 40, 40, 38, 36, 34, 32, 31, 30)
			)
		/** 段位 pointのCombo bonus */
		private val tableGradeComboBonus =
			listOf(
				floatArrayOf(1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f),
				floatArrayOf(1.0f, 1.2f, 1.2f, 1.4f, 1.4f, 1.4f, 1.4f, 1.5f, 1.5f, 2.0f),
				floatArrayOf(1.0f, 1.4f, 1.5f, 1.6f, 1.7f, 1.8f, 1.9f, 2.0f, 2.1f, 2.5f),
				floatArrayOf(1.0f, 1.5f, 1.8f, 2.0f, 2.2f, 2.3f, 2.4f, 2.5f, 2.6f, 3.0f)
			)
		/** 実際の段位を上げるのに必要な内部段位 */
		private val tableGradeChange = listOf(1, 2, 3, 4, 5, 7, 9, 12, 15, 18, 19, 20, 23, 25, 27, 29, 31, -1)
		/** 段位 pointが1つ減る time */
		private val tableGradeDecayRate =
			listOf(
				125, 100, 80, 50, 48, 47, 45, 44, 43, 42, 41, 40, 36, 33, 30, 28, 26, 24, 22, 20, 19, 18, 17, 16, 15, 15, 14,
				14, 13, 13, 11, 10
			)

		// /** 段位のcount */
		// private static final int GRADE_MAX = 33;

		/** 段位のName */
		private val tableGradeName = listOf(
			"9", "8", "7", "6", "5", "4", "3", "2", "1", // 0～ 8
			"S1", "S2", "S3", "S4", "S5", "S6", "S7", "S8", "S9", // 9～17
			"m1", "m2", "m3", "m4", "m5", "m6", "m7", "m8", "m9", // 18～26
			"M", "MK", "MV", "MO", "MM", "GM" // 27～32
		)
		private val tableLongGradeName = listOf(
			"9", "8", "7", "6", "5", "4", "3", "2", "1", // 0～ 8
			"S1", "S2", "S3", "S4", "S5", "S6", "S7", "S8", "S9", "S10", "S11", "S12", "S13", // 9～21
			"m1", "m2", "m3", "m4", "m5", "m6", "m7", "m8", "m9", "mK", "mV", "mO", "mM", // 22～34
			"M1", "M2", "M3", "M4", "M5", "M6", "M7", "M8", "M9", "MK", "MV", "MO", "MM", // 35～47
			"G", "GS", "Gm", "GM" // 48～50,51
		)

		/** 裏段位のName */
		private val tableSecretGradeName = listOf(
			"9", "8", "7", "6", "5", "4", "3", "2", "1", // 0～ 8
			"S1", "S2", "S3", "S4", "S5", "S6", "S7", "S8", "S9", // 9～17
			"GM" // 18
		)

		/**とりカン条件タイム*/
		val tableQualifyTimeMul =
			listOf(5 to 1f, 9 to 1.6f, 15 to 2.275f, 19 to 2.7f)

		fun tableQualifyPace(s:Int, baseTime:Int) = tableQualifyTimeMul.let {t ->
			minOf(t.lastIndex, t.indexOfFirst {s<it.first}).let {id ->
				if(id<=0) t.first().let {baseTime*it.second/it.first}
				else t[id].let {(x1, x2) ->
					t[id-1].let {(y1, y2) -> baseTime*(x2-y2)/(x1-y1)}
				}
			}
		}.toInt()

		/** LV999 roll time */
		private const val ROLLTIMELIMIT = 3238

		/** 段位履歴のサイズ */
		private const val GRADE_HISTORY_SIZE = 7

		/** 段位認定試験の発生確率(EXAM_CHANCE分の1の確率で発生) */
		private const val EXAM_CHANCE = 2

		/** Number of sections */
		private val tableSectionMax = listOf(10, 20)
		private val tableGoalLevel = listOf(999, 2020)
		private val RANKING_TYPE = tableSectionMax.size
	}
}
