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
import mu.nu.nullpo.game.subsystem.mode.menu.LevelGrandMenuItem
import mu.nu.nullpo.game.subsystem.mode.menu.MenuList
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

/** GRADE MANIA 3 Cambridge Mode */
class GrandM3C:AbstractMode() {
	/** Default section time */
	// private static final int DEFAULT_SECTION_TIME = 6000;

	/** Next Section の level (これ-1のときに levelストップする) */
	private var nextSecLv = 0

	/** 内部 level */
	private var internalLevel = 0

	/** Levelが増えた flag */
	private var lvupFlag = false

	/** 最終結果などに表示される実際の段位 */
	private var grade = 0

	/** 内部段位 */
	private var gradeBasicInternal = 0

	/** 内部段位 point */
	private var gradeBasicPoint = 0f

	/** 最後に段位が上がった time */
	private var lastGradeTime = 0

	/** Hard dropした段count */
	private var hardDropBonus = 0

	/** Combo bonus */
	private var comboValue = 0

	/** このSection でCOOLを出すとtrue */
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

	/** COOL section flags */
	private val coolSection = MutableList(SECTION_MAX) {false}

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

	/** Section Time
	 * [section][0]: x00-x99 [section][1]: x00-x70 */
	private val sectionTime = List(SECTION_MAX) {MutableList(2) {0}}

	/** 新記録が出たSection はtrue */
	private val sectionIsNewRecord = MutableList(SECTION_MAX) {false}

	/** Cleared Section count */
	private var sectionsDone = 0

	/** Average Section Time */
	private val sectionAvgTime
		get() = sectionTime.filter {it[0]>0}.map {it[0]}.average().toFloat()

	/** 直前のSection Time */
	private var sectionLastTime = 0

	/** 消えRoll started flag */
	private var mRollFlag = false

	/** Roll 中に稼いだ point (段位上昇用) */
	private var rollPoints = 0f

	/** Roll 中に稼いだ point (合計) */
	private var rollPointsTotal = 0f

	/** AC medal 状態 */
	private var medalAC = 0

	/** ST medal 状態 */
	private var medalST = 0

	/** SK medal 状態 */
	private var medalSK = 0

	/** CO medal 状態 */
	private var medalCO = 0

	/** Section Time記録表示中ならtrue */
	private var isShowBestSectionTime = false

	private val itemLevel = LevelGrandMenuItem(COLOR.BLUE, true, true)
	/** Level at start */
	private var startLevel:Int by DelegateMenuItem(itemLevel)

	private val itemGhost = BooleanMenuItem("alwaysghost", "FULL GHOST", COLOR.BLUE, false)
	/** When true, always ghost ON */
	private var alwaysGhost:Boolean by DelegateMenuItem(itemGhost)

	private val item20g = BooleanMenuItem("always20g", "20G MODE", COLOR.BLUE, false)
	/** When true, always 20G */
	private var always20g:Boolean by DelegateMenuItem(item20g)

	private val itemAlert = BooleanMenuItem("lvstopse", "Sect.ALERT", COLOR.BLUE, true)
	/** When true, levelstop sound is enabled */
	private var secAlert:Boolean by DelegateMenuItem(itemAlert)

	private val itemST = BooleanMenuItem("showsectiontime", "SHOW STIME", COLOR.BLUE, true)
	/** When true, section time display is enabled */
	private var showST:Boolean by DelegateMenuItem(itemST)

	private val itemBig = BooleanMenuItem("big", "BIG", COLOR.BLUE, false)
	/** BigMode */
	private var big:Boolean by DelegateMenuItem(itemBig)

	private var itemGrade = BooleanMenuItem("showgrade", "SHOW GRADE", COLOR.BLUE, false)
	/** 段位表示 */
	private var gradeDisp:Boolean by DelegateMenuItem(itemGrade)

	private var itemExam = BooleanMenuItem("enableexam", "EXAMINATION", COLOR.YELLOW, true)
	/** 昇格・降格試験 is enabled */
	private var enableExam:Boolean by DelegateMenuItem(itemExam)

	override val menu:MenuList
		get() = MenuList("grademania3", itemGhost, itemAlert, itemST, itemGrade, itemExam, itemLevel, item20g, itemBig)
	/** Version */
	private var version = 0

	/** Current round's ranking position */
	private var rankingRank = 0

	/** Rankings' 段位 */
	private val rankingGrade = MutableList(RANKING_MAX) {0}

	/** Rankings' levels */
	private val rankingLevel = MutableList(RANKING_MAX) {0}

	/** Rankings' times */
	private val rankingTime = MutableList(RANKING_MAX) {-1}

	/** Rankings' Roll completely cleared flag */
	private val rankingRollClear = MutableList(RANKING_MAX) {0}

	/** Section Time記録 */
	private val bestSectionTime = MutableList(SECTION_MAX) {i -> tableQualifyPace(i)}

	/** 段位履歴 (昇格・降格試験用) */
	private val gradeHistory = MutableList(GRADE_HISTORY_SIZE) {0}

	var examRecord = mutableListOf(0, 0)
		get() = mutableListOf(qualifiedGrade, demotionPoints)
		set(value) {
			field = value
			qualifiedGrade = value[0]
			demotionPoints = value[1]
		}
	override val rankMap
		get() = rankMapOf(
			"grade" to rankingGrade, "level" to rankingLevel, "time" to rankingTime, "rollClear" to rankingRollClear,
			"section.time" to bestSectionTime, "exam.history" to gradeHistory, "exam.record" to examRecord
		)
	/*override val rankPersMap:Map<String, IntArray>
		get() = rankMap("grade" to rankingGrade, "level" to rankingLevel, "time" to rankingTime, "rollClear" to rankingRollClear,
			"section.time" to bestSectionTime, "exam.history" to gradeHistory, "exam.record" to examRecord)*/

	/** 昇格試験の目標段位 */
	private var promotionalExam = 0

	/** Current 認定段位 */
	private var qualifiedGrade = 0

	/** 降格試験 point (30以上溜まると降格試験発生) */
	private var demotionPoints = 0

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

	private var decoration = 0
	private var decTemp = 0

	/* Mode name */
	override val name = "Grand Mastery"
	override val gameIntensity = 1
	/** @return 何らかの試験中ならtrue
	 */
	private val isAnyExam:Boolean
		get() = promotionFlag||demotionFlag

	/* Initialization */
	override fun playerInit(engine:GameEngine) {
		super.playerInit(engine)
		nextSecLv = 0
		internalLevel = 0
		lvupFlag = true
		grade = 0
		gradeBasicInternal = 0
		gradeBasicPoint = 0f
		lastGradeTime = 0
		hardDropBonus = 0
		comboValue = 0
		lastScore = 0
		cool = false
		previousCool = false
		coolChecked = false
		coolDisplayed = false
		coolDispFrame = 0
		rollTime = 0
		rollClear = 0
		rollStarted = false
		secretGrade = 0
		gradeFlash = 0
		sectionTime.forEachIndexed {i, _ -> sectionTime[i].fill(0)}
		sectionIsNewRecord.fill(false)
		coolSection.fill(false)
		sectionsDone = 0
		sectionLastTime = 0
		mRollFlag = false
		rollPoints = 0f
		rollPointsTotal = 0f
		medalCO = 0
		medalSK = 0
		medalST = 0
		medalAC = 0

		demotionPoints = 0
		qualifiedGrade = 0
		promotionalExam = 0
		passFrame = 0
		readyFrame = 0
		gradeHistory.fill(0)
		demotionFlag = false
		promotionFlag = false
		demotionExamGrade = 0

		rankingRank = -1
		rankingGrade.fill(0)
		rankingLevel.fill(0)
		rankingTime.fill(-1)
		rankingRollClear.fill(0)
		bestSectionTime.forEachIndexed {i, _ ->
			bestSectionTime[i] = tableQualifyPace(i)
		}

		engine.twistEnable = true
		engine.twistEnableEZ = true
		engine.b2bEnable = true
		engine.splitB2B = true
		engine.frameColor = GameEngine.FRAME_COLOR_SILVER
		engine.comboType = GameEngine.COMBO_TYPE_DOUBLE
		engine.bigHalf = true
		engine.bigMove = true
		engine.staffrollEnable = true
		engine.staffrollNoDeath = false

		if(!owner.replayMode) {
			loadSetting(owner.modeConfig, engine)

			version = CURRENT_VERSION
		} else {
			loadSetting(owner.replayProp, engine)
			version = owner.replayProp.getProperty("grademania3.version", 0)

			if(enableExam) {
				promotionalExam = owner.replayProp.getProperty("grademania3.exam", 0)
				demotionPoints = owner.replayProp.getProperty("grademania3.demopoint", 0)
				demotionExamGrade = owner.replayProp.getProperty("grademania3.demotionExamGrade", 0)
				if(promotionalExam>0) {
					promotionFlag = true
					readyFrame = 100
					passFrame = 600
				} else if(demotionPoints>=30) {
					demotionFlag = true
					passFrame = 600
				}

				log.debug("** Exam data from replay START **")
				log.debug("Promotional Exam Grade:${getGradeName(promotionalExam)} ($promotionalExam)")
				log.debug("Promotional Exam Flag:$promotionFlag")
				log.debug("Demotion Points:$demotionPoints")
				log.debug("Demotional Exam Grade:${getGradeName(demotionExamGrade)} ($demotionExamGrade)")
				log.debug("Demotional Exam Flag:$demotionFlag")
				log.debug("*** Exam data from replay END ***")
			}
		}

		owner.bgMan.bg = 20+startLevel
		setSpeed(engine)
	}

	/** Set BGM at start of game
	 */
	private fun calcBgmLv(lv:Int, rLv:Int = lv):Int =
		if(rLv>=900&&grade>=15&&coolCount>=9) BGM.GrandT().nums-1
		else tableBGMChange.count {lv>=it}.let {
			minOf(it+maxOf(0, it-4)+(it>=3&&coolCount>=3).toInt(), 6)
		}

	/** Update falling speed
	 * @param engine GameEngine
	 */
	private fun setSpeed(engine:GameEngine) {
		engine.speed.gravity = if(always20g||engine.statistics.time>=54000) -1
		else tableGravityValue[tableGravityChangeLevel.indexOfFirst {internalLevel<it}
			.let {if(it<0) tableGravityChangeLevel.size-1 else it}]


		if(engine.statistics.time>=54000) {
			engine.speed.are = 2
			engine.speed.areLine = 1
			engine.speed.lineDelay = 3
			engine.speed.lockDelay = 13
			engine.speed.das = 5
		} else {
			engine.speed.are = tableARE.let {it[minOf(it.size-1, internalLevel/100)]}
			engine.speed.areLine = tableARE.let {it[minOf(it.size-1, internalLevel/100)]}
			engine.speed.lineDelay = tableLineDelay.let {it[minOf(it.size-1, internalLevel/100)]}
			engine.speed.lockDelay = tableLockDelay.let {it[minOf(it.size-1, internalLevel/100)]}
			engine.speed.das = tableDAS.let {it[minOf(it.size-1, internalLevel/100)]}
		}
	}

	/** ST medal check
	 * @param engine GameEngine
	 * @param sectionNumber Section number
	 */
	private fun stMedalCheck(engine:GameEngine, sectionNumber:Int) {
		val best = bestSectionTime[sectionNumber]

		if(sectionLastTime<best||best<=0) {
			if(medalST<3) {
				engine.playSE("medal3")
				if(medalST<1) decTemp += 3
				if(medalST<2) decTemp += 6
				medalST = 3
				decTemp += 6
			}
			if(!owner.replayMode) {
				decTemp++
				sectionIsNewRecord[sectionNumber] = true
			}
		} else if(sectionLastTime<best+300&&medalST<2) {
			engine.playSE("medal2")
			if(medalST<1) decTemp += 3
			medalST = 2
			decTemp += 6
		} else if(sectionLastTime<best+600&&medalST<1) {
			engine.playSE("medal1")
			medalST = 1
			decTemp += 3// 12
		}
	}

	/** COOLの check
	 * @param engine GameEngine
	 */
	private fun checkCool(engine:GameEngine) {
		// COOL check
		if(engine.statistics.level%100>=70&&!coolChecked) {
			val section = engine.statistics.level/100
			val coolPrevTime = sectionTime[coolSection.indexOfLast {it}][1]

			if(sectionTime[1][section]<=tableTimeCool[section]&&
				(!previousCool||sectionTime[section][1]<=coolPrevTime+(200-section*9))) {
				cool = true
				coolSection[section] = true
			} else
				coolSection[section] = false
			coolChecked = true
		}

		// COOL表示
		if(engine.statistics.level%100>=82&&cool&&!coolDisplayed) {
			engine.playSE("cool")
			coolDispFrame = 180
			coolDisplayed = true
			decTemp += 2
		}
	}

	/** 段位名を取得
	 * @param g 段位 number
	 * @return 段位名(範囲外ならN / A)
	 */
	private fun getGradeName(g:Int):String = tableGradeName.getOrElse(g) {"N/A"}

	/* Called at settings screen */
	override fun onSetting(engine:GameEngine):Boolean {
		// Menu
		if(!engine.owner.replayMode) {
			// Configuration changes
			val change = updateMenu(engine)

			if(change!=0) {
				owner.bgMan.bg = 20+startLevel
				engine.statistics.level = startLevel*100
				setSpeed(engine)
			}

			// section time display切替
			if(engine.ctrl.isPush(Controller.BUTTON_F)) {
				engine.playSE("change")
				isShowBestSectionTime = !isShowBestSectionTime
			}

			// 決定
			if(menuTime<5) menuTime++ else if(engine.ctrl.isPush(Controller.BUTTON_A)) {
				engine.playSE("decide")

				isShowBestSectionTime = false

				sectionsDone = 0
				val rand = Random.Default
				if(!always20g&&!big&&enableExam&&rand.nextInt(EXAM_CHANCE)==0) {
					setPromotionalGrade()
					if(promotionalExam>qualifiedGrade) {
						promotionFlag = true
						readyFrame = 100
						passFrame = 600
					} else if(demotionPoints>=50) {
						demotionFlag = true
						demotionExamGrade = qualifiedGrade
						passFrame = 600
						demotionPoints = 0
					}

					log.debug("** Exam debug log START **")
					log.debug("Current Qualified Grade:${getGradeName(qualifiedGrade)} ($qualifiedGrade)")
					log.debug("Promotional Exam Grade:${getGradeName(promotionalExam)} ($promotionalExam)")
					log.debug("Promotional Exam Flag:$promotionFlag")
					log.debug("Demotion Points:$demotionPoints")
					log.debug("Demotional Exam Grade:${getGradeName(demotionExamGrade)} ($demotionExamGrade)")
					log.debug("Demotional Exam Flag:$demotionFlag")
					log.debug("*** Exam debug log END ***")
				}

				return false
			}

			// Cancel
			if(engine.ctrl.isPush(Controller.BUTTON_B)) engine.quitFlag = true
		} else {
			menuTime++
			menuCursor = -1

			return menuTime<60
		}

		return true
	}

	/* Called at game start */
	override fun startGame(engine:GameEngine) {
		val lv = startLevel*100
		engine.statistics.level = lv
		internalLevel = lv
		decTemp = 0
		nextSecLv = maxOf(100, minOf(lv+100, 999))

		owner.bgMan.bg = 20+startLevel

		engine.big = big

		setSpeed(engine)
		owner.musMan.bgm = BGM.GrandT(calcBgmLv(lv))
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

					receiver.drawScoreFont(engine, 3, 2, "GRADE TIME LEVEL", COLOR.BLUE)

					for(i in 0 until RANKING_MAX) {
						receiver.drawScoreGrade(
							engine, 0, 3+i, "%2d".format(i+1), if(rankingRank==i) COLOR.RAINBOW else COLOR.YELLOW
						)
						receiver.drawScoreGrade(
							engine, 3, 3+i, getGradeName(rankingGrade[i]), if(rankingRollClear[i]==1||rankingRollClear[i]==3) COLOR.GREEN
							else if(rankingRollClear[i]==2||rankingRollClear[i]==4) COLOR.ORANGE else COLOR.WHITE
						)
						receiver.drawScoreNum(engine, 7, 3+i, rankingTime[i].toTimeStr, i==rankingRank)
						receiver.drawScoreNum(engine, 15, 3+i, "%03d".format(rankingLevel[i]), i==rankingRank)
					}

					receiver.drawScoreNano(engine, 0, 18, "NEXT QUALIFY PREDICATE", COLOR.YELLOW)
					if(promotionalExam>qualifiedGrade) {
						receiver.drawScoreFont(engine, 4, 19, BaseFont.CURSOR)
						receiver.drawScoreGrade(engine, 5, 19, getGradeName(promotionalExam), scale = 1.67f)
					}
					receiver.drawScoreGrade(engine, 0, 19, getGradeName(qualifiedGrade), scale = 2f)

					for(i in 0 until GRADE_HISTORY_SIZE)
						if(gradeHistory[i]>=0)
							receiver.drawScoreGrade(
								engine, -2, 15+i, getGradeName(gradeHistory[i]),
								if(gradeHistory[i]>qualifiedGrade) COLOR.YELLOW else COLOR.WHITE
							)

					receiver.drawScoreFont(engine, 0, 17, "F:VIEW SECTION TIME", COLOR.GREEN)
				} else {
					// Section Time
					receiver.drawScoreFont(engine, 0, 2, "SECTION TIME", COLOR.BLUE)

					var totalTime = 0
					for(i in 0 until SECTION_MAX) {
						val temp = minOf(i*100, 999)
						val temp2 = minOf((i+1)*100-1, 999)

						val strSectionTime:String = "%3d-%3d %s".format(temp, temp2, bestSectionTime[i].toTimeStr)

						receiver.drawScoreNum(engine, 0, 3+i, strSectionTime, sectionIsNewRecord[i]&&!isAnyExam)

						totalTime += bestSectionTime[i]
					}

					receiver.drawScoreFont(engine, 0, 14, "TOTAL", COLOR.BLUE)
					receiver.drawScoreNum(engine, 0, 15, totalTime.toTimeStr, 2f)
					receiver.drawScoreFont(
						engine, if(receiver.nextDisplayType==2) 0 else 12, if(receiver.nextDisplayType==2) 18 else 14,
						"AVERAGE", COLOR.BLUE
					)
					receiver.drawScoreNum(
						engine, if(receiver.nextDisplayType==2) 0 else 12, if(receiver.nextDisplayType==2) 19 else 15,
						(totalTime/SECTION_MAX).toTimeStr, 2f
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
			if(gradeDisp) {
				receiver.drawScoreFont(engine, 0, 2, "GRADE", if(g20) COLOR.YELLOW else COLOR.BLUE)
				// 段位
				receiver.drawScoreGrade(
					engine, 0, 3, getGradeName(if(grade>=50&&qualifiedGrade<50) 49 else grade), gradeFlash>0&&gradeFlash%4==0, 2f
				)
				receiver.drawScoreSpeed(engine, 3, 3, gp%1f, 5f)
				receiver.drawScoreNum(
					engine, 6, 2, "%02.1f%%".format((gp*100%100)), gradeFlash>0&&gradeFlash%4==0
				)
			}

			// Score
			receiver.drawScoreFont(engine, 0, 5, "Score", if(g20) COLOR.YELLOW else COLOR.BLUE)
			receiver.drawScoreNum(engine, 5, 5, "+$lastScore")
			receiver.drawScoreNum(engine, 0, 6, "$scDisp", 2f)

			// level
			receiver.drawScoreFont(engine, 0, 9, "Level", if(g20) COLOR.YELLOW else COLOR.BLUE)

			receiver.drawScoreNum(engine, 1, 10, "%3d".format(maxOf(engine.statistics.level, 0)))
			receiver.drawScoreSpeed(
				engine, 0, 11, if(g20) 40 else floor(ln(engine.speed.gravity.toDouble())).toInt()*4,
				4
			)
			if(coolCount>0) {
				receiver.drawScoreFont(engine, 4, 11, "+")
				receiver.drawScoreGrade(engine, 5, 11, "%1d".format(coolCount))
			}
			receiver.drawScoreNum(engine, 1, 12, "%3d".format(nextSecLv))

			// Time
			receiver.drawScoreFont(
				engine, 0, 14, "Time", if(g20) COLOR.YELLOW else COLOR.BLUE
			)
			if(engine.ending!=2||rollTime/10%2==0)
				receiver.drawScoreNum(engine, 0, 15, engine.statistics.time.toTimeStr, 2f)

			// Roll 残り time
			if(engine.gameActive&&engine.ending==2) {
				var time = ROLLTIMELIMIT-rollTime
				if(time<0) time = 0
				receiver.drawScoreFont(engine, 0, 17, "ROLL TIME", COLOR.CYAN)
				receiver.drawScoreNum(engine, 0, 18, time.toTimeStr, time>0&&time<10*60, 2f)
			}

			if(coolDispFrame>0)
			// COOL表示
				receiver.drawMenuFont(
					engine, 2, 21, "COOL!!", when {
						coolDispFrame%4==0 -> COLOR.BLUE
						coolDispFrame%2==0 -> COLOR.GREEN
						else -> COLOR.CYAN
					}
				)

			// medal
			receiver.drawScoreMedal(engine, 0, 20, "AC", medalAC)
			receiver.drawScoreMedal(engine, 3, 20, "ST", medalST)
			receiver.drawScoreMedal(engine, 0, 21, "SK", medalSK)
			receiver.drawScoreMedal(engine, 3, 21, "CO", medalCO)

			// Section Time
			if(showST&&sectionTime.isNotEmpty()) {
				val x = receiver.nextDisplayType==2
				receiver.drawScoreFont(engine, if(x) 8 else 10, 2, "SECTION TIME", COLOR.BLUE)

				val section = engine.statistics.level/100
				for(i in sectionTime.indices)
					if(sectionTime[i][0]>0) {
						var temp = i*100
						if(temp>999) temp = 999

						var strSeparator = "-"


						if(i==section&&engine.ending==0) strSeparator = "+"
						val color = when {
							coolSection[i] -> if(sectionIsNewRecord[i]) COLOR.CYAN else COLOR.GREEN
							else -> COLOR.WHITE
						}
						val strSectionTime = StringBuilder()
						for(l in 0 until i)
							strSectionTime.append("\n")
						strSectionTime.append(
							String.format(
								"%3d%s%s %02d.%02d", temp,
								strSeparator, sectionTime[i][0].toTimeStr, sectionTime[i][1]%60/60f
							)
						)

						receiver.drawScoreNum(engine, if(x) 9 else 10, 3, "$strSectionTime", color, if(x) .75f else 1f)
					}

				receiver.drawScoreFont(engine, if(x) 8 else 12, if(x) 11 else 14, "AVERAGE", COLOR.BLUE)
				receiver.drawScoreNum(
					engine,
					if(x) 8 else 12,
					if(x) 12 else 15,
					(engine.statistics.time/(sectionsDone+(engine.ending==0).toInt())).toTimeStr,
					2f
				)
			}
		}
	}

	/* Ready→Goの処理 */
	override fun onReady(engine:GameEngine):Boolean {
		if(promotionFlag) {
			engine.frameColor = GameEngine.FRAME_SKIN_GRADE

			if(readyFrame==100) engine.playSE("item_spawn")

			if(engine.ctrl.isPush(Controller.BUTTON_A)||engine.ctrl.isPush(Controller.BUTTON_B)) readyFrame = 0

			if(readyFrame>0) {
				readyFrame--
				return true
			}
		} else if(demotionFlag) {
			engine.frameColor = GameEngine.FRAME_COLOR_WHITE
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
				engine, 2, 6, getGradeName(promotionalExam), if(readyFrame%4==0) COLOR.ORANGE else COLOR.WHITE,
				2f
			)
		}
	}

	/* 移動中の処理 */
	override fun onMove(engine:GameEngine):Boolean {
		// 新規ピース出現時
		if(engine.ending==0&&engine.statc[0]==0&&!engine.holdDisable&&!lvupFlag) {
			// Level up
			levelUp(engine, (engine.statistics.level<nextSecLv-1).toInt())

			// Hard drop bonusInitialization
			hardDropBonus = 0
		}
		if(engine.ending==0&&engine.statc[0]>0&&(version>=1||!engine.holdDisable)) lvupFlag = false

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

	/* ARE中の処理 */
	override fun onARE(engine:GameEngine):Boolean {
		// 最後の frame
		if(engine.ending==0&&engine.statc[0]>=engine.statc[1]-1&&!lvupFlag) {
			if(engine.statistics.level<nextSecLv-1) {
				engine.statistics.level++
				internalLevel++
				if(engine.statistics.level==nextSecLv-1&&secAlert) engine.playSE("levelstop")
			}
			levelUp(engine)
			lvupFlag = true
		}

		return false
	}

	/** levelが上がったときの共通処理 */
	private fun levelUp(engine:GameEngine, lu:Int = 0) {
		val lb = internalLevel
		engine.statistics.level += lu
		internalLevel += lu
		// Meter
		engine.meterValue = engine.statistics.level%100/99f
		engine.meterColor = GameEngine.METER_COLOR_LEVEL
		// COOL check
		checkCool(engine)

		// 速度変更
		setSpeed(engine)

		// LV100到達でghost を消す
		if(engine.statistics.level>=100&&!alwaysGhost) engine.ghost = false

		if(lu<=0) return
		if(engine.statistics.level==nextSecLv-1&&secAlert) engine.playSE("levelstop")
		// BGM fadeout
		val lA = internalLevel+cool.toInt()*100
		if(tableBGMFadeout.any {it in lb..lA}) owner.musMan.fadeSW = true
		// BGM切り替え
		if(tableBGMChange.any {it in lb..lA}) {
			owner.musMan.fadeSW = false
			owner.musMan.bgm = BGM.GrandT(calcBgmLv(lA, engine.statistics.level))
		}
	}
	/** Grade points*/
	val gp get() = minOf(30f, (sqrt((gradeBasicPoint/50f)*8+1)/2-.5f))
	/* Calculate score */
	override fun calcScore(engine:GameEngine, ev:ScoreEvent):Int {
		// Combo
		val li = ev.lines
		comboValue = if(li==0) 1
		else maxOf(1, comboValue+2*li-2)

		if(li>=1&&engine.ending==0) {
			// 段位 point
			val index = internalLevel/100
			val basePoint = tableGradePoint.map {it[li-1]}.let {
				maxOf(
					0f+it[0], it[1]*(1+index/2f), it[2]*(index-2f),
					(engine.statistics.level>=1000&&li>=4).toInt()*gradeBasicInternal*30f
				)
			}
			val indexCombo = minOf(maxOf(0, ev.combo+if(ev.b2b>0) 0 else -1), tableGradeComboBonus[li-1].size-1)
			val comboBonus = tableGradeComboBonus[li-1][indexCombo]

			val levelBonus = 1+engine.statistics.level/250

			val point = basePoint*comboBonus*levelBonus
			gradeBasicPoint += point
			// 内部段位上昇
			while(grade<50&&gradeBasicInternal<30&&floor(gp)>=gradeBasicInternal) {
				gradeBasicInternal++
				engine.playSE("cool")
				coolDispFrame = 180
				decTemp++
				grade++
				engine.playSE(if(gradeDisp) "grade${grade/8}" else "medal1")
				gradeFlash = 180
				lastGradeTime = engine.statistics.time
			}

			// 4-line clearカウント
			if(li>=4)
			// SK medal
				if(big) {
					if(engine.statistics.totalQuadruple==1||engine.statistics.totalQuadruple==2
						||engine.statistics.totalQuadruple==4
					) {
						engine.playSE("medal${++medalSK}")
					}
				} else if(engine.statistics.totalQuadruple==10||engine.statistics.totalQuadruple==20
					||engine.statistics.totalQuadruple==30
				) {
					decTemp += 3+medalSK*2// 3 8 15
					engine.playSE("medal${++medalSK}")
				}

			// AC medal
			if(engine.field.isEmpty) {
				decTemp += li*25
				if(li==3) decTemp += 25
				if(li==4) decTemp += 150
				if(medalAC<3) {
					engine.playSE("medal1")
					decTemp += 3+medalAC*4// 3 10 21
					medalAC++
				}
			}

			// CO medal
			if(big) {
				if(ev.combo>=2&&medalCO<1) {
					engine.playSE("medal1")
					medalCO = 1
				} else if(ev.combo>=3&&medalCO<2) {
					engine.playSE("medal2")
					medalCO = 2
				} else if(ev.combo>=4&&medalCO<3) {
					engine.playSE("medal3")
					medalCO = 3
				}
			} else if(ev.combo>=3&&medalCO<1) {
				engine.playSE("medal1")
				medalCO = 1
				decTemp += 3// 3
			} else if(ev.combo>=4&&medalCO<2) {
				engine.playSE("medal2")
				medalCO = 2
				decTemp += 4// 7
			} else if(ev.combo>=5&&medalCO<3) {
				engine.playSE("medal3")
				medalCO = 3
				decTemp += 5// 12
			}

			// Level up
			val levelb = engine.statistics.level
			val lu = li.let {it+maxOf(0, it-2)}
			levelUp(engine, lu)

			if(engine.statistics.level>=2020) {
				// Ending
				engine.statistics.level = 2020
				engine.timerActive = false
				engine.ending = 1
				rollClear = 1

				lastGradeTime = engine.statistics.time

				// Section Timeを記録
				sectionLastTime = sectionTime[levelb/100][0]
				sectionsDone++

				// ST medal
				stMedalCheck(engine, levelb/100)

				// 条件を全て満たしているなら消えRoll 発動
				if(grade>=50&&gradeBasicPoint>=25000&&coolCount>=20) {
					mRollFlag = true
					engine.playSE("applause4")
				} else engine.playSE("applause3")
			} else if(tableQualifyTime.firstOrNull {nextSecLv/100==it.first}.let
				{it!=null&&engine.statistics.time>it.second&&engine.statistics.level>=it.first*100}
				&&!promotionFlag&&!demotionFlag
			) {
				// level500とりカン
				engine.statistics.level = nextSecLv
				engine.gameEnded()
				engine.staffrollEnable = false
				engine.ending = 1
				engine.playSE("applause2")

				secretGrade = engine.field.secretGrade
				lastGradeTime = engine.statistics.time
				// Section Timeを記録
				sectionLastTime = sectionTime[levelb/100][1]
				sectionsDone++

				// ST medal
				stMedalCheck(engine, levelb/100)

			} else if(engine.statistics.level>=nextSecLv) {
				// Next Section

				// Background切り替え
				owner.bgMan.nextBg = 20+nextSecLv/100

				engine.playSE("levelup")

				// Section Timeを記録
				sectionLastTime = sectionTime[levelb/100][1]
				sectionsDone++

				// ST medal
				stMedalCheck(engine, levelb/100)

				// COOLを取ってたら
				if(cool) {
					previousCool = true
					if(grade<50) {
						grade++
						gradeFlash = 180
						if(gradeDisp) engine.playSE("grade${grade/8}")
					}
					internalLevel += 100
				} else
					previousCool = false

				cool = false
				coolChecked = false
				coolDisplayed = false
				// Update level for next section
				nextSecLv += 100
				if(nextSecLv>=2000) nextSecLv = 2020
			} else if(engine.statistics.level==nextSecLv-1&&secAlert) engine.playSE("levelstop")

			// Calculate score

			lastScore =
				((((levelb+li)/(if(ev.b2b>0) 3 else 4)+engine.softdropFall+(if(engine.manualLock) 1 else 0)+hardDropBonus)
					*li*comboValue)+maxOf(0, engine.lockDelay-engine.lockDelayNow)
					+engine.statistics.level/if(engine.twist) 2 else 3)*if(engine.field.isEmpty) 3 else 1

			engine.statistics.scoreLine += lastScore
			return lastScore
		} else if(li>=1&&engine.ending==2) {
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
				grade++
				gradeFlash = 180
			}
			if(gradeDisp&&gradeFlash==180) engine.playSE("grade${grade/8}")
			return (points*100).toInt()
		}
		return 0
	}

	/* Called when hard drop used */
	override fun afterHardDropFall(engine:GameEngine, fall:Int) {
		if(fall*2>hardDropBonus) hardDropBonus = fall*2
	}

	/* 各 frame の終わりの処理 */
	override fun onLast(engine:GameEngine) {
		super.onLast(engine)
		// 段位上昇時のフラッシュ
		if(gradeFlash>0) gradeFlash--

		// COOL表示
		if(coolDispFrame>0) coolDispFrame--

		// 15分経過
		if(engine.statistics.time>=54000) setSpeed(engine)

		// Section Time増加
		if(engine.timerActive&&engine.ending==0) {
			val section = engine.statistics.level/100

			if(section>=0&&section<sectionTime.size)
				(engine.statistics.time-sectionTime.take(section).sumOf {it[0]}).let {
					sectionTime[section][0] = it
					if(engine.statistics.level%100<70) sectionTime[section][1] = it
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
				if(gradeDisp&&gradeFlash==180) engine.playSE("grade${grade/8}")

				engine.blockOutlineType = GameEngine.BLOCK_OUTLINE_NORMAL
				engine.blockShowOutlineOnly = false

				engine.gameEnded()
				engine.resetStatc()
				engine.stat = GameEngine.Status.EXCELLENT
				if(engine.statistics.time<28800) decTemp += (28800-engine.statistics.time)/1800
			}
		}
	}

	/* game over */
	override fun onGameOver(engine:GameEngine):Boolean {
		// This code block will execute only once
		if(engine.statc[0]==0) {
			secretGrade = engine.field.secretGrade
			val time = engine.statistics.time
			if(time<6000)
				decTemp -= 3
			else {
				decTemp++
				if(time%3600<=60||time%3600>=3540) decTemp++
			}

			if(time>41100) decTemp -= 1+(time-41100)/1800
			if(enableExam) {
				if(grade<qualifiedGrade-7) demotionPoints += qualifiedGrade-grade-7

				if(promotionFlag&&grade>=promotionalExam) {
					qualifiedGrade = promotionalExam
					demotionPoints = 0
					decTemp += 6
					engine.tempHanabi += 24
				}
				if(demotionFlag&&grade<demotionExamGrade) {
					qualifiedGrade = maxOf(0, demotionExamGrade-1)
					decTemp -= 10
				}

				log.debug("** Exam result log START **")
				log.debug("Current Qualified Grade:${getGradeName(qualifiedGrade)} ($qualifiedGrade)")
				log.debug("Promotional Exam Grade:${getGradeName(promotionalExam)} ($promotionalExam)")
				log.debug("Promotional Exam Flag:$promotionFlag")
				log.debug("Demotion Points:$demotionPoints")
				log.debug("Demotional Exam Grade:${getGradeName(demotionExamGrade)} ($demotionExamGrade)")
				log.debug("Demotional Exam Flag:$demotionFlag")
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
					engine, 2, 6, getGradeName(qualifiedGrade), if(passFrame%4==0) COLOR.ORANGE else COLOR.WHITE,
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
				receiver.drawMenuFont(engine, 6, 3, "EXAM", COLOR.RED)

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
			receiver.drawMenuFont(engine, 0, 0, "\u0090\u0093 PAGE${engine.statc[1]+1}/3", COLOR.RED)

			when(engine.statc[1]) {
				0 -> {
					val rgrade = if(enableExam&&grade>=50&&qualifiedGrade<50) 49 else grade
					val gcolor = when {
						grade>=32&&engine.statc[2]%2==0 -> COLOR.YELLOW
						rollClear==1||rollClear==3 -> COLOR.GREEN
						rollClear==2||rollClear==4 -> COLOR.ORANGE
						else -> COLOR.WHITE
					}
					receiver.drawMenuFont(engine, 0, 3, "GRADE", COLOR.BLUE)
					receiver.drawMenuGrade(engine, 6, 2, getGradeName(rgrade), gcolor, 2f)

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

					for(i in sectionTime.indices)
						if(sectionTime[i][0]>0) receiver.drawMenuNum(
							engine, 2, 3+i, sectionTime[i][0].toTimeStr,
							if(coolSection[i]) if(sectionIsNewRecord[i]) COLOR.CYAN else COLOR.GREEN else COLOR.WHITE
						)

					if(sectionAvgTime>0) {
						receiver.drawMenuFont(engine, 0, 14, "AVERAGE", COLOR.BLUE)
						receiver.drawMenuNum(engine, 0, 15, sectionAvgTime.toTimeStr, 1.7f)
					}
				}
				2 -> {
					receiver.drawMenuFont(engine, 0, 2, "MEDAL", COLOR.BLUE)
					receiver.drawMenuMedal(engine, 5, 2, "AC", medalAC)
					receiver.drawMenuMedal(engine, 8, 2, "CO", medalCO)
					receiver.drawMenuMedal(engine, 2, 3, "ST", medalST)
					receiver.drawMenuMedal(engine, 6, 3, "SK", medalSK)

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
					if(grade>=qualifiedGrade) {
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

		engine.allowTextRenderByReceiver = true

		owner.musMan.fadeSW = false
		owner.musMan.bgm = when {
			engine.ending==1||engine.ending==2&&rollClear==0 -> BGM.Result(2)
			rollClear>0||promotionFlag&&grade>=promotionalExam -> BGM.Result(3)
			demotionFlag&&grade<=qualifiedGrade -> BGM.Result(1)
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
			var rgrade = grade
			if(enableExam&&rgrade>=32&&qualifiedGrade<32) rgrade = 31
			// if(!enableexam || !isAnyExam())
			updateRanking(rgrade, engine.statistics.level, lastGradeTime, rollClear)
			// else
			// rankingRank = -1;

			if(enableExam) updateGradeHistory(grade)

			if(medalST==3&&!isAnyExam) updateBestSectionTime()

			if(rankingRank!=-1||enableExam||medalST==3) return true
		}
		return false
	}

	/** Update rankings
	 * @param gr 段位
	 * @param lv level
	 * @param time Time
	 */
	private fun updateRanking(gr:Int, lv:Int, time:Int, clear:Int) {
		rankingRank = checkRanking(gr, lv, time, clear)

		if(rankingRank!=-1) {
			// Shift down ranking entries
			for(i in RANKING_MAX-1 downTo rankingRank+1) {
				rankingGrade[i] = rankingGrade[i-1]
				rankingLevel[i] = rankingLevel[i-1]
				rankingTime[i] = rankingTime[i-1]
				rankingRollClear[i] = rankingRollClear[i-1]
			}

			// Add new data
			rankingGrade[rankingRank] = gr
			rankingLevel[rankingRank] = lv
			rankingTime[rankingRank] = time
			rankingRollClear[rankingRank] = clear
		}
	}

	/** Calculate ranking position
	 * @param gr 段位
	 * @param lv level
	 * @param time Time
	 * @return Position (-1 if unranked)
	 */
	private fun checkRanking(gr:Int, lv:Int, time:Int, clear:Int):Int {
		for(i in 0 until RANKING_MAX)
			if(gr>rankingGrade[i]) return i
			else if(gr==rankingGrade[i]&&clear>rankingRollClear[i]) return i
			else if(gr==rankingGrade[i]&&clear==rankingRollClear[i]&&lv>rankingLevel[i]) return i
			else if(gr==rankingGrade[i]&&clear==rankingRollClear[i]&&lv==rankingLevel[i]&&time<rankingTime[i]) return i

		return -1
	}

	/** 段位履歴を更新
	 * @param gr 段位
	 */
	private fun updateGradeHistory(gr:Int) {
		System.arraycopy(gradeHistory, 0, gradeHistory, 1, GRADE_HISTORY_SIZE-1)

		gradeHistory[0] = gr

		// Debug log
		log.debug("** Exam grade history START **")
		for(i in gradeHistory.indices)
			log.debug("$i: ${getGradeName(gradeHistory[i])} (${gradeHistory[i]})")

		log.debug("*** Exam grade history END ***")
		setPromotionalGrade()
	}

	/** 昇格試験の目標段位を設定 */
	private fun setPromotionalGrade() {
		promotionalExam = gradeHistory.filter {it>qualifiedGrade}.let {
			if(it.size>3) minOf(if(qualifiedGrade<49) 49 else 50, it.average().roundToInt())
			else qualifiedGrade
		}
	}

	/** Update best section time records */
	private fun updateBestSectionTime() {
		for(i in 0 until SECTION_MAX)
			if(sectionIsNewRecord[i]) bestSectionTime[i] = sectionTime[i][0]
	}

	companion object {
		/** Log */
		internal val log = LogManager.getLogger()

		/** Current version */
		private const val CURRENT_VERSION = 2

		/** Section COOL criteria Time */
		private val tableTimeCool = listOf(
			3360, 3280, 3200, 3110, 3020, 2930, 2835, 2740, 2645, 2550,
			2520, 2490, 2460, 2430, 2400, 2340, 2280, 2220, 2160, 2100
		)
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
			25, 24, 23, 22, 21, 20, 18, 16, 14, 12,
			10, +9, +8, +7, +6, +6, +5, +5, +4, 4,
		)
		/** Line clear times table */
		private val tableLineDelay = listOf(
			25, 24, 22, 20, 18, 16, 14, 12, 11, 10,
			+9, +8, +7, +6, +5, +4, +4, +3, +2, +2
		)
		/** 固定 times table */
		private val tableLockDelay = listOf(
			30, 30, 30, 30, 30, 30, 29, 28, 27, 26,
			25, 24, 23, 22, 21, 20, 19, 18, 17, 16
		)
		/** DAS table */
		private val tableDAS = listOf(15, 14, 13, 12, 11, 10, 9, 8, 7, 6, 6, 6, 6)

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
				listOf(10, 20, 30, 40),
				listOf(2, 6, 12, 24),
				listOf(1, 4, 9, 20)
			)
		/** 段位 pointのCombo bonus */
		private val tableGradeComboBonus =
			listOf(
				floatArrayOf(1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f),
				floatArrayOf(1.0f, 1.2f, 1.2f, 1.4f, 1.4f, 1.4f, 1.4f, 1.5f, 1.5f, 2.0f),
				floatArrayOf(1.0f, 1.4f, 1.5f, 1.6f, 1.7f, 1.8f, 1.9f, 2.0f, 2.1f, 2.5f),
				floatArrayOf(1.0f, 1.5f, 1.8f, 2.0f, 2.2f, 2.3f, 2.4f, 2.5f, 2.6f, 3.0f)
			)

		// /** 段位のcount */
		// private static final int GRADE_MAX = 33;

		/** 段位のName */
		private val tableGradeName = listOf(
			"9", "8", "7", "6", "5", "4", "3", "2", "1", // 0～ 8
			"S1", "S2", "S3", "S4", "S5", "S6", "S7", "S8", "S9", "S10", "S11", "S12", "S13", // 9～21
			"m1", "m2", "m3", "m4", "m5", "m6", "m7", "m8", "m9", "mK", "mV", "mO", "mM", // 22～34
			"M1", "M2", "M3", "M4", "M5", "M6", "M7", "M8", "M9", "MK", "MV", "MO", "MM", // 35～47
			"G", "GS", "Gm", "GM" // 48～50
		)

		/** 裏段位のName */
		private val tableSecretGradeName = listOf(
			"9", "8", "7", "6", "5", "4", "3", "2", "1", // 0～ 8
			"S1", "S2", "S3", "S4", "S5", "S6", "S7", "S8", "S9", // 9～17
			"GM" // 18
		)

		/**とりカン条件タイム*/
		private val tableQualifyTime = listOf(5 to 28800, 9 to 39600, 10 to 43210, 15 to 50000, 20 to 54000)
		private fun tableQualifyPace(s:Int) = tableQualifyTime.let {t ->
			minOf(t.lastIndex, t.indexOfFirst {s<it.first}).let {id ->
				if(id<=0) t.first().let {it.second/it.first}
				else t[id].let {x ->
					t[id-1].let {y -> (x.second-y.second)/(x.first-y.first)}
				}
			}
		}

		/** LV999 roll time */
		private const val ROLLTIMELIMIT = 4000

		/** Number of entries in rankings */
		private const val RANKING_MAX = 13

		/** 段位履歴のサイズ */
		private const val GRADE_HISTORY_SIZE = 7

		/** 段位認定試験の発生確率(EXAM_CHANCE分の1の確率で発生) */
		private const val EXAM_CHANCE = 2

		/** Number of sections */
		private const val SECTION_MAX = 20
	}
}
