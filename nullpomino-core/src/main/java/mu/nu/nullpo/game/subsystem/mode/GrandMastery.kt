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
import mu.nu.nullpo.util.CustomProperties
import mu.nu.nullpo.util.GeneralUtil
import org.apache.log4j.Logger
import java.util.*

/** GRADE MANIA 3 Mode */
class GrandMastery:AbstractMode() {

	/** Default section time */
	// private static final int DEFAULT_SECTION_TIME = 6000;

	/** Current 落下速度の number (tableGravityChangeLevelの levelに到達するたびに1つ増える) */
	private var gravityindex:Int = 0

	/** Next Section の level (これ-1のときに levelストップする) */
	private var nextseclv:Int = 0

	/** 内部 level */
	private var internalLevel:Int = 0

	/** Levelが増えた flag */
	private var lvupflag:Boolean = false

	/** 最終結果などに表示される実際の段位 */
	private var grade:Int = 0

	/** メインの段位 */
	private var gradeBasicReal:Int = 0

	/** メインの段位の内部段位 */
	private var gradeBasicInternal:Int = 0

	/** メインの段位の内部段位 point */
	private var gradeBasicPoint:Int = 0

	/** メインの段位の内部段位 pointが1つ減る time */
	private var gradeBasicDecay:Int = 0

	/** 最後に段位が上がった time */
	private var lastGradeTime:Int = 0

	/** Hard dropした段count */
	private var harddropBonus:Int = 0

	/** Combo bonus */
	private var comboValue:Int = 0

	/** Most recent increase in score */
	private var lastscore:Int = 0

	/** 獲得Render scoreがされる残り time */
	private var scgettime:Int = 0

	/** このSection でCOOLを出すとtrue */
	private var cool:Boolean = false

	/** COOL count */
	private var coolcount:Int = 0

	/** 直前のSection でCOOLを出したらtrue */
	private var previouscool:Boolean = false

	/** 直前のSection での level70通過Time */
	private var coolprevtime:Int = 0

	/** このSection でCOOL check をしたらtrue */
	private var coolchecked:Boolean = false

	/** このSection でCOOL表示をしたらtrue */
	private var cooldisplayed:Boolean = false

	/** COOL display time frame count */
	private var cooldispframe:Int = 0

	/** REGRET display time frame count */
	private var regretdispframe:Int = 0

	/** COOL section flags */
	private var coolsection:BooleanArray = BooleanArray(0)

	/** REGRET section flags */
	private var regretsection:BooleanArray = BooleanArray(0)

	/** Roll 経過 time */
	private var rolltime:Int = 0
	/** Roll completely cleared flag */
	private var rollclear:Int = 0

	/** Roll started flag */
	private var rollstarted:Boolean = false

	/** 裏段位 */
	private var secretGrade:Int = 0

	/** Current BGM */
	private var bgmlv:Int = 0

	/** 段位表示を光らせる残り frame count */
	private var gradeflash:Int = 0

	/** Section Time */
	private var sectionTime:IntArray = IntArray(SECTION_MAX)

	/** 新記録が出たSection はtrue */
	private var sectionIsNewRecord:BooleanArray = BooleanArray(SECTION_MAX)

	/** Cleared Section count */
	private var sectionscomp:Int = 0

	/** Average Section Time */
	private var sectionavgtime:Int = 0

	/** 直前のSection Time */
	private var sectionlasttime:Int = 0

	/** 消えRoll started flag */
	private var mrollFlag:Boolean = false

	/** Roll 中に稼いだ point (段位上昇用) */
	private var rollPoints:Float = 0f

	/** Roll 中に稼いだ point (合計) */
	private var rollPointsTotal:Float = 0f

	/** AC medal 状態 */
	private var medalAC:Int = 0

	/** ST medal 状態 */
	private var medalST:Int = 0

	/** SK medal 状態 */
	private var medalSK:Int = 0

	/** CO medal 状態 */
	private var medalCO:Int = 0

	/** Section Time記録表示中ならtrue */
	private var isShowBestSectionTime:Boolean = false

	/** Level at start */
	private var startlevel:Int = 0

	/** When true, always ghost ON */
	private var alwaysghost:Boolean = false

	/** When true, always 20G */
	private var always20g:Boolean = false

	/** When true, levelstop sound is enabled */
	private var lvstopse:Boolean = false

	/** BigMode */
	private var big:Boolean = false

	/** When true, section time display is enabled */
	private var showsectiontime:Boolean = false

	/** 段位表示 */
	private var gradedisp:Boolean = false

	/** LV500の足切りTime */
	private var lv500torikan:Int = 0

	/** 昇格・降格試験 is enabled */
	private var enableexam:Boolean = false

	/** Version */
	private var version:Int = 0

	/** Current round's ranking rank */
	private var rankingRank:Int = 0

	/** Rankings' 段位 */
	private var rankingGrade:IntArray = IntArray(RANKING_MAX)

	/** Rankings' levels */
	private var rankingLevel:IntArray = IntArray(RANKING_MAX)

	/** Rankings' times */
	private var rankingTime:IntArray = IntArray(RANKING_MAX)

	/** Rankings' Roll completely cleared flag */
	private var rankingRollclear:IntArray = IntArray(RANKING_MAX)

	/** Section Time記録 */
	private var bestSectionTime:IntArray = IntArray(SECTION_MAX)

	/** 段位履歴 (昇格・降格試験用) */
	private var gradeHistory:IntArray = IntArray(0)

	/** 昇格試験の目標段位 */
	private var promotionalExam:Int = 0

	/** Current 認定段位 */
	private var qualifiedGrade:Int = 0

	/** 降格試験 point (30以上溜まると降格試験発生) */
	private var demotionPoints:Int = 0

	/** 昇格試験 flag */
	private var promotionFlag:Boolean = false

	/** 降格試験 flag */
	private var demotionFlag:Boolean = false

	/** 降格試験での目標段位 */
	private var demotionExamGrade:Int = 0

	/** 試験開始前演出の frame count */
	private var readyframe:Int = 0

	/** 試験終了演出の frame count */
	private var passframe:Int = 0

	private var decoration:Int = 0
	private var dectemp:Int = 0

	/* Mode name */
	override val name:String
		get() = "GRAND MASTERY"

	/** @return 何らかの試験中ならtrue
	 */
	private val isAnyExam:Boolean
		get() = promotionFlag||demotionFlag

	/* Initialization */
	override fun playerInit(engine:GameEngine, playerID:Int) {
		super.playerInit(engine, playerID)
		menuTime = 0
		gravityindex = 0
		nextseclv = 0
		internalLevel = 0
		lvupflag = true
		grade = 0
		gradeBasicReal = 0
		gradeBasicInternal = 0
		gradeBasicPoint = 0
		gradeBasicDecay = 0
		lastGradeTime = 0
		harddropBonus = 0
		comboValue = 0
		lastscore = 0
		scgettime = 0
		cool = false
		coolcount = 0
		previouscool = false
		coolprevtime = 0
		coolchecked = false
		cooldisplayed = false
		cooldispframe = 0
		regretdispframe = 0
		rolltime = 0
		rollclear = 0
		rollstarted = false
		secretGrade = 0
		bgmlv = 0
		gradeflash = 0
		sectionTime = IntArray(SECTION_MAX)
		sectionIsNewRecord = BooleanArray(SECTION_MAX)
		regretsection = BooleanArray(SECTION_MAX)
		coolsection = BooleanArray(SECTION_MAX)
		sectionscomp = 0
		sectionavgtime = 0
		sectionlasttime = 0
		mrollFlag = false
		rollPoints = 0f
		rollPointsTotal = 0f
		medalCO = 0
		medalSK = medalCO
		medalST = medalSK
		medalAC = medalST
		isShowBestSectionTime = false
		startlevel = 0
		alwaysghost = false
		always20g = false
		lvstopse = false
		big = false
		gradedisp = false
		lv500torikan = 25200
		enableexam = true

		demotionPoints = 0
		qualifiedGrade = demotionPoints
		promotionalExam = qualifiedGrade
		passframe = 0
		readyframe = passframe
		gradeHistory = IntArray(GRADE_HISTORY_SIZE)
		demotionFlag = false
		promotionFlag = demotionFlag
		demotionExamGrade = 0

		dectemp = 0
		decoration = dectemp

		rankingRank = -1
		rankingGrade = IntArray(RANKING_MAX)
		rankingLevel = IntArray(RANKING_MAX)
		rankingTime = IntArray(RANKING_MAX)
		rankingRollclear = IntArray(RANKING_MAX)
		bestSectionTime = IntArray(SECTION_MAX)

		engine.tspinEnable = true
		engine.tspinEnableEZ = true
		engine.b2bEnable = true
		engine.framecolor = GameEngine.FRAME_COLOR_SILVER
		engine.comboType = GameEngine.COMBO_TYPE_DOUBLE
		engine.bighalf = true
		engine.bigmove = true
		engine.staffrollEnable = true
		engine.staffrollNoDeath = false

		if(!owner.replayMode) {
			loadSetting(owner.modeConfig)
			loadRanking(owner.modeConfig, engine.ruleopt.strRuleName)
			version = CURRENT_VERSION
		} else {
			loadSetting(owner.replayProp)
			version = owner.replayProp.getProperty("grademania3.version", 0)

			System.arraycopy(tableTimeRegret, 0, bestSectionTime, 0, SECTION_MAX)

			if(enableexam) {
				promotionalExam = owner.replayProp.getProperty("grademania3.exam", 0)
				demotionPoints = owner.replayProp.getProperty("grademania3.demopoint", 0)
				demotionExamGrade = owner.replayProp.getProperty("grademania3.demotionExamGrade", 0)
				if(promotionalExam>0) {
					promotionFlag = true
					readyframe = 100
					passframe = 600
				} else if(demotionPoints>=30) {
					demotionFlag = true
					passframe = 600
				}

				log.debug("** Exam data from replay START **")
				log.debug("Promotional Exam Grade:"+getGradeName(promotionalExam)+" ("+promotionalExam+")")
				log.debug("Promotional Exam Flag:$promotionFlag")
				log.debug("Demotion Points:$demotionPoints")
				log.debug("Demotional Exam Grade:"+getGradeName(demotionExamGrade)+" ("+demotionExamGrade+")")
				log.debug("Demotional Exam Flag:$demotionFlag")
				log.debug("*** Exam data from replay END ***")
			}
		}

		owner.backgroundStatus.bg = 20+startlevel
	}

	/** Load settings from property file
	 * @param prop Property file
	 */
	override fun loadSetting(prop:CustomProperties) {
		startlevel = prop.getProperty("grademania3.startlevel", 0)
		alwaysghost = prop.getProperty("grademania3.alwaysghost", true)
		always20g = prop.getProperty("grademania3.always20g", false)
		lvstopse = prop.getProperty("grademania3.lvstopse", true)
		showsectiontime = prop.getProperty("grademania3.showsectiontime", true)
		big = prop.getProperty("grademania3.big", false)
		gradedisp = prop.getProperty("grademania3.gradedisp", false)
		lv500torikan = prop.getProperty("grademania3.lv500torikan", 25200)
		enableexam = prop.getProperty("grademania3.enableexam", true)
	}

	/** Save settings to property file
	 * @param prop Property file
	 */
	override fun saveSetting(prop:CustomProperties) {
		prop.setProperty("grademania3.startlevel", startlevel)
		prop.setProperty("grademania3.alwaysghost", alwaysghost)
		prop.setProperty("grademania3.always20g", always20g)
		prop.setProperty("grademania3.lvstopse", lvstopse)
		prop.setProperty("grademania3.showsectiontime", showsectiontime)
		prop.setProperty("grademania3.big", big)
		prop.setProperty("grademania3.gradedisp", gradedisp)
		prop.setProperty("grademania3.lv500torikan", lv500torikan)
		prop.setProperty("grademania3.enableexam", enableexam)
	}

	/** Set BGM at start of game
	 * @param engine GameEngine
	 */
	private fun setStartBgmlv(engine:GameEngine) {
		bgmlv = 0
		while(tableBGMChange[bgmlv]!=-1&&engine.statistics.level>=tableBGMChange[bgmlv])
			bgmlv++
	}

	/** Update falling speed
	 * @param engine GameEngine
	 */
	private fun setSpeed(engine:GameEngine) {
		if(always20g||engine.statistics.time>=54000)
			engine.speed.gravity = -1
		else {
			while(internalLevel>=tableGravityChangeLevel[gravityindex])
				gravityindex++
			engine.speed.gravity = tableGravityValue[gravityindex]
		}

		if(engine.statistics.time>=54000) {
			engine.speed.are = 2
			engine.speed.areLine = 1
			engine.speed.lineDelay = 3
			engine.speed.lockDelay = 13
			engine.speed.das = 5
		} else {
			var section = internalLevel/100
			if(section>tableARE.size-1) section = tableARE.size-1
			engine.speed.are = tableARE[section]
			engine.speed.areLine = tableARELine[section]
			engine.speed.lineDelay = tableLineDelay[section]
			engine.speed.lockDelay = tableLockDelay[section]
			engine.speed.das = tableDAS[section]
		}
	}

	/** Update average section time */
	private fun setAverageSectionTime() {
		if(sectionscomp>0) {
			var temp = 0
			for(i in startlevel until startlevel+sectionscomp)
				if(i>=0&&i<sectionTime.size) temp += sectionTime[i]

			sectionavgtime = temp/sectionscomp
		} else
			sectionavgtime = 0

	}

	/** ST medal check
	 * @param engine GameEngine
	 * @param sectionNumber Section number
	 */
	private fun stMedalCheck(engine:GameEngine, sectionNumber:Int) {
		val best = bestSectionTime[sectionNumber]

		if(sectionlasttime<best) {
			if(medalST<3) {
				engine.playSE("medal3")
				if(medalST<1) dectemp += 3
				if(medalST<2) dectemp += 6
				medalST = 3
				dectemp += 6
			}
			if(!owner.replayMode) {
				dectemp++
				sectionIsNewRecord[sectionNumber] = true
			}
		} else if(sectionlasttime<best+300&&medalST<2) {
			engine.playSE("medal2")
			if(medalST<1) dectemp += 3
			medalST = 2
			dectemp += 6
		} else if(sectionlasttime<best+600&&medalST<1) {
			engine.playSE("medal1")
			medalST = 1
			dectemp += 3// 12
		}
	}

	/** COOLの check
	 * @param engine GameEngine
	 */
	private fun checkCool(engine:GameEngine) {
		// COOL check
		if(engine.statistics.level%100>=70&&!coolchecked) {
			val section = engine.statistics.level/100

			if(sectionTime[section]<=tableTimeCool[section]&&(!previouscool||previouscool&&sectionTime[section]<=coolprevtime+120)) {
				cool = true
				coolsection[section] = true
			} else
				coolsection[section] = false

			coolprevtime = sectionTime[section]
			coolchecked = true
		}

		// COOL表示
		if(engine.statistics.level%100>=82&&cool&&!cooldisplayed) {
			engine.playSE("cool")
			cooldispframe = 180
			cooldisplayed = true
			dectemp += 2
		}
	}

	/** REGRETの check
	 * @param engine GameEngine
	 * @param levelb Line clear前の level
	 */
	private fun checkRegret(engine:GameEngine, levelb:Int) {
		val section = levelb/100
		if(sectionlasttime>tableTimeRegret[section]) {
			previouscool = false

			coolcount--
			if(coolcount<0) coolcount = 0

			grade--
			if(grade<0) grade = 0
			gradeflash = 180
			dectemp -= 3
			regretdispframe = 180
			engine.playSE("regret")
			regretsection[section] = true
		} else
			regretsection[section] = false

	}

	/** 段位名を取得
	 * @param g 段位 number
	 * @return 段位名(範囲外ならN / A)
	 */
	private fun getGradeName(g:Int):String = if(g<0||g>=tableGradeName.size) "N/A" else tableGradeName[g]

	/* Called at settings screen */
	override fun onSetting(engine:GameEngine, playerID:Int):Boolean {
		// Menu
		if(!engine.owner.replayMode) {
			// Configuration changes
			val change = updateCursor(engine, 8)

			if(change!=0) {
				engine.playSE("change")

				when(menuCursor) {
					0 -> {
						startlevel += change
						if(startlevel<0) startlevel = 9
						if(startlevel>9) startlevel = 0
						owner.backgroundStatus.bg = 20+startlevel
					}
					1 -> alwaysghost = !alwaysghost
					2 -> always20g = !always20g
					3 -> lvstopse = !lvstopse
					4 -> showsectiontime = !showsectiontime
					5 -> big = !big
					6 -> gradedisp = !gradedisp
					7 -> {
						lv500torikan += if(engine.ctrl!!.isPress(Controller.BUTTON_E))
							3600*change
						else
							60*change
						if(lv500torikan<0) lv500torikan = 72000
						if(lv500torikan>72000) lv500torikan = 0
					}
					8 -> enableexam = !enableexam
				}
			}

			// section time display切替
			if(engine.ctrl!!.isPush(Controller.BUTTON_F)&&menuTime>=5) {
				engine.playSE("change")
				isShowBestSectionTime = !isShowBestSectionTime
			}

			// 決定
			if(engine.ctrl!!.isPush(Controller.BUTTON_A)&&menuTime>=5) {
				engine.playSE("decide")
				saveSetting(owner.modeConfig)
				receiver.saveModeConfig(owner.modeConfig)

				isShowBestSectionTime = false

				sectionscomp = 0
				val rand = Random()
				if(!always20g&&!big&&enableexam&&rand.nextInt(EXAM_CHANCE)==0) {
					setPromotionalGrade()
					if(promotionalExam>qualifiedGrade) {
						promotionFlag = true
						readyframe = 100
						passframe = 600
					} else if(demotionPoints>=30) {
						demotionFlag = true
						demotionExamGrade = qualifiedGrade
						passframe = 600
						demotionPoints = 0
					}

					log.debug("** Exam debug log START **")
					log.debug("Current Qualified Grade:"+getGradeName(qualifiedGrade)+" ("+qualifiedGrade+")")
					log.debug("Promotional Exam Grade:"+getGradeName(promotionalExam)+" ("+promotionalExam+")")
					log.debug("Promotional Exam Flag:$promotionFlag")
					log.debug("Demotion Points:$demotionPoints")
					log.debug("Demotional Exam Grade:"+getGradeName(demotionExamGrade)+" ("+demotionExamGrade+")")
					log.debug("Demotional Exam Flag:$demotionFlag")
					log.debug("*** Exam debug log END ***")
				}

				return false
			}

			// Cancel
			if(engine.ctrl!!.isPush(Controller.BUTTON_B)) engine.quitflag = true

			menuTime++
		} else {
			menuTime++
			menuCursor = -1

			return menuTime<60
		}

		return true
	}

	/* Render the settings screen */
	override fun renderSetting(engine:GameEngine, playerID:Int) {
		drawMenu(engine, playerID, receiver, 0, COLOR.BLUE, 0, "LEVEL", (startlevel*100).toString(), "FULL GHOST", GeneralUtil.getONorOFF(alwaysghost), "20G MODE", GeneralUtil.getONorOFF(always20g), "LVSTOPSE", GeneralUtil.getONorOFF(lvstopse), "SHOW STIME", GeneralUtil.getONorOFF(showsectiontime), "BIG", GeneralUtil.getONorOFF(big), "GRADE DISP", GeneralUtil.getONorOFF(gradedisp), "LV500LIMIT",
			if(lv500torikan==0)
				"NONE"
			else
				GeneralUtil.getTime(lv500torikan.toFloat()), "EXAM", GeneralUtil.getONorOFF(enableexam))
	}

	/* Called at game start */
	override fun startGame(engine:GameEngine, playerID:Int) {
		engine.statistics.level = startlevel*100

		dectemp = 0
		nextseclv = engine.statistics.level+100
		if(engine.statistics.level<0) nextseclv = 100
		if(engine.statistics.level>=900) nextseclv = 999

		internalLevel = engine.statistics.level

		owner.backgroundStatus.bg = 20+engine.statistics.level/100

		engine.big = big

		setSpeed(engine)
		setStartBgmlv(engine)
		owner.bgmStatus.bgm = tableBGM[bgmlv]
	}

	/* Render score */
	override fun renderLast(engine:GameEngine, playerID:Int) {
		receiver.drawScoreFont(engine, playerID, 0, 0, "GRAND MASTERY", COLOR.CYAN)

		receiver.drawScoreFont(engine, playerID, -1, -4*2, "DECORATION", scale = .5f)
		receiver.drawScoreBadges(engine, playerID, 0, -3, 100, decoration)
		receiver.drawScoreBadges(engine, playerID, 5, -4, 100, dectemp)
		if(engine.stat==GameEngine.Status.SETTING||engine.stat==GameEngine.Status.RESULT&&!owner.replayMode) {
			if(startlevel==0&&!big&&!always20g&&!owner.replayMode&&engine.ai==null)
				if(!isShowBestSectionTime) {
					// Rankings

					receiver.drawScoreFont(engine, playerID, 3, 2, "GRADE TIME LEVEL", COLOR.BLUE)

					for(i in 0 until RANKING_MAX) {
						var gcolor = COLOR.WHITE
						if(rankingRollclear[i]==1||rankingRollclear[i]==3) gcolor = COLOR.GREEN
						if(rankingRollclear[i]==2||rankingRollclear[i]==4) gcolor = COLOR.ORANGE

						receiver.drawScoreGrade(engine, playerID, 0, 3+i, String.format("%2d", i+1), COLOR.YELLOW)
						receiver.drawScoreGrade(engine, playerID, 3, 3+i, getGradeName(rankingGrade[i]), gcolor)
						receiver.drawScoreNum(engine, playerID, 7, 3+i, GeneralUtil.getTime(rankingTime[i].toFloat()), i==rankingRank)
						receiver.drawScoreNum(engine, playerID, 15, 3+i, String.format("%03d", rankingLevel[i]), i==rankingRank)
					}

					receiver.drawScoreFont(engine, playerID, 0, 14, "QUALIFY CANDIDATE", COLOR.YELLOW)
					if(promotionalExam>qualifiedGrade) {
						receiver.drawScoreFont(engine, playerID, 4, 15, "b")
						receiver.drawScoreGrade(engine, playerID, 5, 15, getGradeName(promotionalExam), scale = 1.67f)
					}
					receiver.drawScoreGrade(engine, playerID, 0, 15, getGradeName(qualifiedGrade), scale = 2f)

					for(i in 0 until GRADE_HISTORY_SIZE)
						if(gradeHistory[i]>=0)
							receiver.drawScoreGrade(engine, playerID, -2, 15+i, getGradeName(gradeHistory[i]),
								if(gradeHistory[i]>qualifiedGrade) COLOR.YELLOW else COLOR.WHITE)

					receiver.drawScoreFont(engine, playerID, 0, 17, "F:VIEW SECTION TIME", COLOR.GREEN)

				} else {
					// Section Time
					receiver.drawScoreFont(engine, playerID, 0, 2, "SECTION TIME", COLOR.BLUE)

					var totalTime = 0
					for(i in 0 until SECTION_MAX) {

						val temp = minOf(i*100, 999)
						val temp2 = minOf((i+1)*100-1, 999)

						val strSectionTime:String
						strSectionTime = String.format("%3d-%3d %s", temp, temp2, GeneralUtil.getTime(bestSectionTime[i].toFloat()))

						receiver.drawScoreNum(engine, playerID, 0, 3+i, strSectionTime, sectionIsNewRecord[i]&&!isAnyExam)

						totalTime += bestSectionTime[i]
					}

					receiver.drawScoreFont(engine, playerID, 0, 14, "TOTAL", COLOR.BLUE)
					receiver.drawScoreNum(engine, playerID, 0, 15, GeneralUtil.getTime(totalTime.toFloat()), 2f)
					receiver.drawScoreFont(engine, playerID, if(receiver.nextDisplayType==2)
						0
					else
						12, if(receiver.nextDisplayType==2) 18 else 14, "AVERAGE", COLOR.BLUE)
					receiver.drawScoreNum(engine, playerID, if(receiver.nextDisplayType==2)
						0
					else
						12, if(receiver.nextDisplayType==2) 19 else 15, GeneralUtil.getTime((totalTime/SECTION_MAX).toFloat()), 2f)

					receiver.drawScoreFont(engine, playerID, 0, 17, "F:VIEW RANKING", COLOR.GREEN)

				}
		} else {
			val g20 = engine.speed.gravity<0
			if(promotionFlag) {
				// 試験段位
				receiver.drawScoreNano(engine, playerID, 0, 1, "QUALIFY", COLOR.ORANGE)
				receiver.drawScoreGrade(engine, playerID, 6, 1, getGradeName(promotionalExam))
			}
			if(gradedisp) {
				receiver.drawScoreFont(engine, playerID, 0, 2, "GRADE", if(g20) COLOR.YELLOW else COLOR.BLUE)
				// 段位
				var rgrade = grade
				if(rgrade>=32&&qualifiedGrade<32) rgrade = 31
				receiver.drawScoreGrade(engine, playerID, 0, 3, getGradeName(rgrade), gradeflash>0&&gradeflash%4==0, 2f)
				var index = gradeBasicInternal
				if(index>tableGradeDecayRate.size-1) index = tableGradeDecayRate.size-1
				receiver.drawScoreGrade(engine, playerID, 3, 3, getGradeName(index), gradeflash>0&&gradeflash%4==0)
				receiver.drawScoreNum(engine, playerID, 6, 2, String.format("%02.1f%%", gradeBasicPoint-gradeBasicDecay*1f/tableGradeDecayRate[index]), gradeflash>0&&gradeflash%4==0)

			}

			// Score
			receiver.drawScoreFont(engine, playerID, 0, 5, "SCORE", if(g20) COLOR.YELLOW else COLOR.BLUE)
			receiver.drawScoreNum(engine, playerID, 5, 5, "+$lastscore")
			receiver.drawScoreNum(engine, playerID, 0, 6, scgettime.toString(), 2f)
			if(scgettime<engine.statistics.score) scgettime += Math.ceil(((engine.statistics.score-scgettime)/10f).toDouble()).toInt()

			// level
			receiver.drawScoreFont(engine, playerID, 0, 9, "LEVEL", if(g20) COLOR.YELLOW else COLOR.BLUE)

			receiver.drawScoreNum(engine, playerID, 0, 10, String.format("%3d", maxOf(engine.statistics.level, 0)))
			receiver.drawSpeedMeter(engine, playerID, 0, 11, if(g20) 40 else Math.floor(Math.log(engine.speed.gravity.toDouble())).toInt()*4)
			if(coolcount>0) {
				receiver.drawScoreFont(engine, playerID, 3, 11, "+")
				receiver.drawScoreGrade(engine, playerID, 4, 11, String.format("%1d", coolcount))
			}
			receiver.drawScoreNum(engine, playerID, 0, 12, String.format("%3d", nextseclv))

			// Time
			receiver.drawScoreFont(engine, playerID, 0, 14, "TIME", if(g20)
				COLOR.YELLOW
			else
				COLOR.BLUE)
			if(engine.ending!=2||rolltime/10%2==0)
				receiver.drawScoreNum(engine, playerID, 0, 15, GeneralUtil.getTime(engine.statistics.time.toFloat()), 2f)

			// Roll 残り time
			if(engine.gameActive&&engine.ending==2) {
				var time = ROLLTIMELIMIT-rolltime
				if(time<0) time = 0
				receiver.drawScoreFont(engine, playerID, 0, 17, "ROLL TIME", COLOR.CYAN)
				receiver.drawScoreNum(engine, playerID, 0, 18, GeneralUtil.getTime(time.toFloat()), time>0&&time<10*60, 2f)
			}

			if(regretdispframe>0)
			// REGRET表示
				receiver.drawMenuFont(engine, playerID, 2, 21, "REGRET", when {
					regretdispframe%4==0 -> COLOR.YELLOW
					regretdispframe%4==2 -> COLOR.RED
					else -> COLOR.ORANGE
				})
			else if(cooldispframe>0)
			// COOL表示
				receiver.drawMenuFont(engine, playerID, 2, 21, "COOL!!", when {
					cooldispframe%2==0 -> COLOR.GREEN
					cooldispframe%4==0 -> COLOR.BLUE
					else -> COLOR.CYAN
				})

			// medal
			receiver.drawScoreMedal(engine, playerID, 0, 20, "AC", medalAC)
			receiver.drawScoreMedal(engine, playerID, 3, 20, "ST", medalST)
			receiver.drawScoreMedal(engine, playerID, 0, 21, "SK", medalSK)
			receiver.drawScoreMedal(engine, playerID, 3, 21, "CO", medalCO)

			// Section Time
			if(showsectiontime&&sectionTime.isNotEmpty()) {
				val x = receiver.nextDisplayType==2
				receiver.drawScoreFont(engine, playerID, if(x) 8 else 10, 2, "SECTION TIME", COLOR.BLUE)

				val section = engine.statistics.level/100
				for(i in sectionTime.indices)
					if(sectionTime[i]>0) {
						var temp = i*100
						if(temp>999) temp = 999

						var strSeparator = "-"


						if(i==section&&engine.ending==0) strSeparator = "+"
						val color = when {
							regretsection[i] -> if(sectionIsNewRecord[i]) COLOR.ORANGE else COLOR.RED
							coolsection[i] -> if(sectionIsNewRecord[i]) COLOR.CYAN else COLOR.GREEN
							else -> COLOR.WHITE
						}
						val strSectionTime = StringBuilder()
						for(l in 0 until i)
							strSectionTime.append("\n")
						strSectionTime.append(String.format("%3d%s%s", temp, strSeparator, GeneralUtil.getTime(sectionTime[i].toFloat())))

						receiver.drawScoreNum(engine, playerID, if(x) 9 else 10, 3, strSectionTime.toString(), color, if(x) .75f else 1f)
					}

				receiver.drawScoreFont(engine, playerID, if(x) 8 else 12, if(x) 11 else 14, "AVERAGE", COLOR.BLUE)
				receiver.drawScoreNum(engine, playerID, if(x) 8 else 12, if(x) 12 else 15, GeneralUtil.getTime((engine.statistics.time/(sectionscomp+if(engine.ending==0) 1 else 0)).toFloat()), 2f)

			}
		}
	}

	/* Ready→Goの処理 */
	override fun onReady(engine:GameEngine, playerID:Int):Boolean {
		if(promotionFlag) {
			engine.framecolor = GameEngine.FRAME_COLOR_BRONZE

			if(readyframe==100) engine.playSE("item_spawn")

			if(engine.ctrl!!.isPush(Controller.BUTTON_A)||engine.ctrl!!.isPush(Controller.BUTTON_B)) readyframe = 0

			if(readyframe>0) {
				readyframe--
				return true
			}
		} else if(demotionFlag) {
			engine.framecolor = GameEngine.FRAME_COLOR_WHITE
			engine.playSE("item_trigger")
		}

		return false
	}

	/* Ready→Goのときの描画 */
	override fun renderReady(engine:GameEngine, playerID:Int) {
		if(promotionFlag&&readyframe>0) {
			receiver.drawMenuFont(engine, playerID, 0, 2, "PROMOTION", COLOR.YELLOW)
			receiver.drawMenuFont(engine, playerID, 6, 3, "EXAM", COLOR.YELLOW)
			receiver.drawMenuGrade(engine, playerID, 2, 6, getGradeName(promotionalExam), if(readyframe%4==0)
				COLOR.ORANGE
			else
				COLOR.WHITE, 2f)
		}
	}

	/* 移動中の処理 */
	override fun onMove(engine:GameEngine, playerID:Int):Boolean {
		// 新規ピース出現時
		if(engine.ending==0&&engine.statc[0]==0&&!engine.holdDisable&&!lvupflag) {
			// Level up
			if(engine.statistics.level<nextseclv-1) {
				engine.statistics.level++
				internalLevel++
				if(engine.statistics.level==nextseclv-1&&lvstopse) engine.playSE("levelstop")
			}
			levelUp(engine)

			// Hard drop bonusInitialization
			harddropBonus = 0
		}
		if(engine.ending==0&&engine.statc[0]>0&&(version>=1||!engine.holdDisable)) lvupflag = false

		// 段位 point減少
		if(engine.timerActive&&gradeBasicPoint>0&&engine.combo<=0
			&&engine.lockDelayNow<engine.lockDelay-1) {
			gradeBasicDecay++

			var index = gradeBasicInternal
			if(index>tableGradeDecayRate.size-1) index = tableGradeDecayRate.size-1

			if(gradeBasicDecay>=tableGradeDecayRate[index]) {
				gradeBasicDecay = 0
				gradeBasicPoint--
			}
		}

		// Endingスタート
		if(engine.ending==2&&!rollstarted) {
			rollstarted = true

			if(mrollFlag) {
				engine.blockHidden = engine.ruleopt.lockflash
				engine.blockHiddenAnim = false
				engine.blockShowOutlineOnly = true
			} else {
				engine.blockHidden = 300
				engine.blockHiddenAnim = true
			}

			owner.bgmStatus.bgm = BGM.ENDING(2)
		}

		return false
	}

	/* ARE中の処理 */
	override fun onARE(engine:GameEngine, playerID:Int):Boolean {
		// 最後の frame
		if(engine.ending==0&&engine.statc[0]>=engine.statc[1]-1&&!lvupflag) {
			if(engine.statistics.level<nextseclv-1) {
				engine.statistics.level++
				internalLevel++
				if(engine.statistics.level==nextseclv-1&&lvstopse) engine.playSE("levelstop")
			}
			levelUp(engine)
			lvupflag = true
		}

		return false
	}

	/** levelが上がったときの共通処理 */
	private fun levelUp(engine:GameEngine) {
		// Meter
		engine.meterValue = engine.statistics.level%100*receiver.getMeterMax(engine)/99
		engine.meterColor = GameEngine.METER_COLOR_LEVEL
		// COOL check
		checkCool(engine)

		// 速度変更
		setSpeed(engine)

		// LV100到達でghost を消す
		if(engine.statistics.level>=100&&!alwaysghost) engine.ghost = false

		// BGM fadeout
		var tempLevel = internalLevel
		if(cool) tempLevel += 100

		if(tableBGMFadeout[bgmlv]!=-1&&tempLevel>=tableBGMFadeout[bgmlv]) owner.bgmStatus.fadesw = true

		// BGM切り替え
		if(tableBGMChange[bgmlv]!=-1&&internalLevel>=tableBGMChange[bgmlv]) {
			bgmlv++
			owner.bgmStatus.fadesw = false
			owner.bgmStatus.bgm = tableBGM[bgmlv]
		}
	}

	/* Calculate score */
	override fun calcScore(engine:GameEngine, playerID:Int, lines:Int) {
		// Combo
		if(lines==0)
			comboValue = 1
		else {
			comboValue = comboValue+2*lines-2
			if(comboValue<1) comboValue = 1
		}

		if(lines>=1&&engine.ending==0) {
			// 段位 point
			var index = gradeBasicInternal
			if(index>10) index = 10
			val basepoint = tableGradePoint[lines-1][index]
			var indexcombo = engine.combo+if(engine.b2b) 0 else -1
			if(indexcombo<0) indexcombo = 0
			if(indexcombo>tableGradeComboBonus[lines-1].size-1) indexcombo = tableGradeComboBonus[lines-1].size-1
			val combobonus = tableGradeComboBonus[lines-1][indexcombo]

			val levelbonus = 1+engine.statistics.level/250

			val point = basepoint.toFloat()*combobonus*levelbonus.toFloat()
			gradeBasicPoint += point.toInt()

			// 内部段位上昇
			while(gradeBasicPoint>=100) {
				gradeBasicPoint -= 100
				gradeBasicDecay = 0
				gradeBasicInternal++
				engine.playSE("cool")
				cooldispframe = 180
				if(tableGradeChange[gradeBasicReal]!=-1&&gradeBasicInternal>=tableGradeChange[gradeBasicReal]) {
					dectemp++
					gradeBasicReal++
					grade++
					engine.playSE("medal1")
					if(grade>31) grade = 31
					engine.playSE("grade${grade/8}")
					gradeflash = 180
					lastGradeTime = engine.statistics.time
				}
			}

			// 4-line clearカウント
			if(lines>=4)
			// SK medal
				if(big) {
					if(engine.statistics.totalQuadruple==1||engine.statistics.totalQuadruple==2
						||engine.statistics.totalQuadruple==4) {
						engine.playSE("medal${++medalSK}")
					}
				} else if(engine.statistics.totalQuadruple==10||engine.statistics.totalQuadruple==20
					||engine.statistics.totalQuadruple==30) {
					dectemp += 3+medalSK*2// 3 8 15
					engine.playSE("medal${++medalSK}")
				}

			// AC medal
			if(engine.field!!.isEmpty) {

				dectemp += lines*25
				if(lines==3) dectemp += 25
				if(lines==4) dectemp += 150
				if(medalAC<3) {
					engine.playSE("medal1")
					dectemp += 3+medalAC*4// 3 10 21
					medalAC++
				}
			}

			// CO medal
			if(big) {
				if(engine.combo>=2&&medalCO<1) {
					engine.playSE("medal1")
					medalCO = 1
				} else if(engine.combo>=3&&medalCO<2) {
					engine.playSE("medal2")
					medalCO = 2
				} else if(engine.combo>=4&&medalCO<3) {
					engine.playSE("medal3")
					medalCO = 3
				}
			} else if(engine.combo>=3&&medalCO<1) {
				engine.playSE("medal1")
				medalCO = 1
				dectemp += 3// 3
			} else if(engine.combo>=4&&medalCO<2) {
				engine.playSE("medal2")
				medalCO = 2
				dectemp += 4// 7
			} else if(engine.combo>=5&&medalCO<3) {
				engine.playSE("medal3")
				medalCO = 3
				dectemp += 5// 12
			}

			// Level up
			val levelb = engine.statistics.level

			var levelplus = lines
			if(lines>=3) levelplus += lines-2
			//if(lines>=4) levelplus=6;

			engine.statistics.level += levelplus
			internalLevel += levelplus

			levelUp(engine)

			if(engine.statistics.level>=999) {
				// Ending
				engine.statistics.level = 999
				engine.timerActive = false
				engine.ending = 1
				rollclear = 1

				lastGradeTime = engine.statistics.time

				// Section Timeを記録
				sectionlasttime = sectionTime[levelb/100]
				sectionscomp++
				setAverageSectionTime()

				// ST medal
				stMedalCheck(engine, levelb/100)

				// REGRET判定
				checkRegret(engine, levelb)

				// 条件を全て満たしているなら消えRoll 発動
				if(grade>=15&&coolcount>=9) {
					mrollFlag = true
					engine.playSE("applause4")
				}else engine.playSE("applause3")

			} else if(nextseclv==500&&engine.statistics.level>=500&&lv500torikan>0
				&&engine.statistics.time>lv500torikan&&!promotionFlag&&!demotionFlag) {
				// level500とりカン
				engine.statistics.level = 500
				engine.gameEnded()
				engine.staffrollEnable = false
				engine.ending = 1
				engine.playSE("applause2")

				secretGrade = engine.field!!.secretGrade
				lastGradeTime = engine.statistics.time
				// Section Timeを記録
				sectionlasttime = sectionTime[levelb/100]
				sectionscomp++
				setAverageSectionTime()

				// ST medal
				stMedalCheck(engine, levelb/100)

				// REGRET判定
				checkRegret(engine, levelb)
			} else if(engine.statistics.level>=nextseclv) {
				// Next Section

				// Background切り替え
				owner.backgroundStatus.fadesw = true
				owner.backgroundStatus.fadecount = 0
				owner.backgroundStatus.fadebg = 20+nextseclv/100

				// BGM切り替え
				if(tableBGMChange[bgmlv]!=-1&&internalLevel>=tableBGMChange[bgmlv]) {
					bgmlv++
					owner.bgmStatus.fadesw = false
					owner.bgmStatus.bgm = tableBGM[bgmlv]
					engine.playSE("levelup_section")
				}else engine.playSE("levelup")

				// Section Timeを記録
				sectionlasttime = sectionTime[levelb/100]
				sectionscomp++
				setAverageSectionTime()

				// ST medal
				stMedalCheck(engine, levelb/100)

				// REGRET判定
				checkRegret(engine, levelb)

				// COOLを取ってたら
				if(cool&&!regretsection[levelb/100]) {
					previouscool = true

					coolcount++
					grade++
					if(grade>31) grade = 31
					gradeflash = 180

					if(gradedisp) {
						engine.playSE("gradeup")
						engine.playSE("grade${grade/8}")
					}

					internalLevel += 100
				} else
					previouscool = false

				cool = false
				coolchecked = false
				cooldisplayed = false

				// Update level for next section
				nextseclv += 100
				if(nextseclv>999) nextseclv = 999
			} else if(engine.statistics.level==nextseclv-1&&lvstopse) engine.playSE("levelstop")

			// Calculate score

			lastscore = ((((levelb+lines)/(if(engine.b2b) 3 else 4)+engine.softdropFall+(if(engine.manualLock) 1 else 0)+harddropBonus)
				*lines*comboValue)+maxOf(0, engine.lockDelay-engine.lockDelayNow)
				+engine.statistics.level/if(engine.tspin) 2 else 3)*if(engine.field!!.isEmpty) 3 else 1

			engine.statistics.score += lastscore
		} else if(lines>=1&&engine.ending==2) {
			// Roll 中のLine clear
			var points = 0f
			if(!mrollFlag||engine.tspin||engine.b2b) {
				if(lines==1) points = 0.04f
				if(lines==2) points = 0.09f
				if(lines==3) points = 0.15f
				if(lines==4) points = 0.27f
			} else {
				if(lines==1) points = .1f
				if(lines==2) points = .2f
				if(lines==3) points = .5f
				if(lines==4) points = 1f
			}
			rollPoints += points
			rollPointsTotal += points

			while(rollPoints>=1f&&grade<31) {
				rollPoints -= 1f
				grade++
				gradeflash = 180
				if(gradedisp) {
					engine.playSE("gradeup")
					engine.playSE("grade${grade/8}")
				}
			}
		}
	}

	/* Called when hard drop used */
	override fun afterHardDropFall(engine:GameEngine, playerID:Int, fall:Int) {
		if(fall*2>harddropBonus) harddropBonus = fall*2
	}

	/* 各 frame の終わりの処理 */
	override fun onLast(engine:GameEngine, playerID:Int) {
		// 段位上昇時のフラッシュ
		if(gradeflash>0) gradeflash--

		// REGRET表示
		if(regretdispframe>0) regretdispframe--

		// COOL表示
		if(cooldispframe>0) cooldispframe--

		// 15分経過
		if(engine.statistics.time>=54000) setSpeed(engine)

		// Section Time増加
		if(engine.timerActive&&engine.ending==0) {
			val section = engine.statistics.level/100

			if(section>=0&&section<sectionTime.size) sectionTime[section]++

			if(engine.statistics.level==nextseclv-1)
				engine.meterColor = if(engine.meterColor==-0x1)
					-0x10000
				else
					-0x1
		}

		// Ending
		if(engine.gameActive&&engine.ending==2) {
			rolltime++

			// Time meter
			val remainRollTime = ROLLTIMELIMIT-rolltime
			engine.meterValue = remainRollTime*receiver.getMeterMax(engine)/ROLLTIMELIMIT
			engine.meterColor = GameEngine.METER_COLOR_LIMIT

			// Roll 終了
			if(rolltime>=ROLLTIMELIMIT) {
				rollclear = 2

				secretGrade = engine.field!!.secretGrade

				if(!mrollFlag) {
					rollPoints += .5f
					rollPointsTotal += .5f
				} else {
					rollPoints += 1.6f
					rollPointsTotal += 1.6f
				}

				while(rollPoints>=1f) {
					rollPoints -= 1f
					grade++
					if(grade>32) grade = 32
					gradeflash = 180
					if(gradedisp) {
						engine.playSE("gradeup")
						engine.playSE("grade${grade/8}")
					}
				}

				engine.blockOutlineType = GameEngine.BLOCK_OUTLINE_NORMAL
				engine.blockShowOutlineOnly = false

				engine.gameEnded()
				engine.resetStatc()
				engine.stat = GameEngine.Status.EXCELLENT
				if(engine.statistics.time<28800) dectemp += (28800-engine.statistics.time)/1800
			}
		}
	}

	/* game over */
	override fun onGameOver(engine:GameEngine, playerID:Int):Boolean {
		// This code block will executed only once
		if(engine.statc[0]==0) {
			secretGrade = engine.field!!.secretGrade
			val time = engine.statistics.time
			if(time<6000)
				dectemp -= 3
			else {
				dectemp++
				if(time%3600<=60||time%3600>=3540) dectemp++
			}

			if(time>41100) dectemp -= 1+(time-41100)/1800
			if(enableexam) {
				if(grade<qualifiedGrade-7) demotionPoints += qualifiedGrade-grade-7

				if(promotionFlag&&grade>=promotionalExam) {
					qualifiedGrade = promotionalExam
					demotionPoints = 0
					dectemp += 6
					engine.temphanabi += 24
				}
				if(demotionFlag&&grade<demotionExamGrade) {
					qualifiedGrade = demotionExamGrade-1
					if(qualifiedGrade<0) qualifiedGrade = 0
					dectemp -= 10
				}

				log.debug("** Exam result log START **")
				log.debug("Current Qualified Grade:"+getGradeName(qualifiedGrade)+" ("+qualifiedGrade+")")
				log.debug("Promotional Exam Grade:"+getGradeName(promotionalExam)+" ("+promotionalExam+")")
				log.debug("Promotional Exam Flag:$promotionFlag")
				log.debug("Demotion Points:$demotionPoints")
				log.debug("Demotional Exam Grade:"+getGradeName(demotionExamGrade)+" ("+demotionExamGrade+")")
				log.debug("Demotional Exam Flag:$demotionFlag")
				log.debug("*** Exam result log END ***")
			}
			decoration += dectemp+secretGrade
		}

		return false
	}

	/* 結果画面 */
	override fun renderResult(engine:GameEngine, playerID:Int) {
		if(passframe>0) {
			if(promotionFlag) {
				receiver.drawMenuFont(engine, playerID, 0, 2, "PROMOTION", COLOR.YELLOW)
				receiver.drawMenuFont(engine, playerID, 1, 3, "CHANCE TO", COLOR.YELLOW)
				receiver.drawMenuGrade(engine, playerID, 2, 6, getGradeName(qualifiedGrade), if(passframe%4==0)
					COLOR.ORANGE
				else
					COLOR.WHITE, 2f)

				if(passframe<420)
					if(grade<promotionalExam)
						receiver.drawMenuFont(engine, playerID, 3, 11, "FAIL",
							when {
								passframe%4==0 -> COLOR.YELLOW
								passframe%4==2 -> COLOR.RED
								else -> COLOR.ORANGE
							})
					else {
						receiver.drawMenuFont(engine, playerID, 2, 11, "PASS!!",
							when {
								passframe%2==0 -> COLOR.GREEN
								passframe%4==0 -> COLOR.BLUE
								else -> COLOR.CYAN
							})
						receiver.drawMenuFont(engine, playerID, 1, 5, "YOU ARE",
							when {
								passframe%2==0 -> COLOR.GREEN
								passframe%4==0 -> COLOR.BLUE
								else -> COLOR.CYAN
							})
					}
			} else if(demotionFlag) {
				receiver.drawMenuFont(engine, playerID, 0, 2, "DEMOTION", COLOR.RED)
				receiver.drawMenuFont(engine, playerID, 6, 3, "EXAM", COLOR.RED)

				if(passframe<420)
					if(grade<demotionExamGrade)
						receiver.drawMenuFont(engine, playerID, 2, 11, "FUCKED", when {
							passframe%4==0 -> COLOR.YELLOW
							passframe%4==2 -> COLOR.RED
							else -> COLOR.ORANGE
						})
					else
						receiver.drawMenuFont(engine, playerID, 3, 11, "SAFE", when {
							passframe%2==0 -> COLOR.GREEN
							passframe%4==0 -> COLOR.BLUE
							else -> COLOR.CYAN
						})

			}
		} else {
			receiver.drawMenuFont(engine, playerID, 0, 0, "kn PAGE"+(engine.statc[1]+1)+"/3", COLOR.RED)

			when(engine.statc[1]) {
				0 -> {
					var rgrade = grade
					if(enableexam&&rgrade>=32&&qualifiedGrade<32) rgrade = 31
					var gcolor = COLOR.WHITE
					if(rollclear==1||rollclear==3) gcolor = COLOR.GREEN
					if(rollclear==2||rollclear==4) gcolor = COLOR.ORANGE
					if(grade>=32&&engine.statc[2]%2==0) gcolor = COLOR.YELLOW
					receiver.drawMenuFont(engine, playerID, 0, 3, "GRADE", COLOR.BLUE)
					receiver.drawMenuGrade(engine, playerID, 6, 2, getGradeName(rgrade), gcolor, 2f)

					drawResultStats(engine, playerID, receiver, 4, COLOR.BLUE, AbstractMode.Statistic.SCORE, AbstractMode.Statistic.LINES, AbstractMode.Statistic.LEVEL_MANIA, AbstractMode.Statistic.TIME)
					drawResultRank(engine, playerID, receiver, 12, COLOR.BLUE, rankingRank)
					if(secretGrade>4)
						drawResult(engine, playerID, receiver, 15, COLOR.BLUE, "S. GRADE", String.format("%10s", tableSecretGradeName[secretGrade-1]))
				}
				1 -> {

					receiver.drawMenuFont(engine, playerID, 0, 2, "SECTION", COLOR.BLUE)

					for(i in sectionTime.indices)
						if(sectionTime[i]>0) {
							var color = COLOR.WHITE
							if(regretsection[i])
								color = if(sectionIsNewRecord[i])
									COLOR.ORANGE
								else
									COLOR.RED
							else if(coolsection[i])
								color = if(sectionIsNewRecord[i])
									COLOR.CYAN
								else
									COLOR.GREEN
							receiver.drawMenuNum(engine, playerID, 2, 3+i, GeneralUtil.getTime(sectionTime[i].toFloat()), color)
						}

					if(sectionavgtime>0) {
						receiver.drawMenuFont(engine, playerID, 0, 14, "AVERAGE", COLOR.BLUE)
						receiver.drawMenuNum(engine, playerID, 0, 15, GeneralUtil.getTime(sectionavgtime.toFloat()), 1.7f)
					}
				}
				2 -> {
					receiver.drawMenuFont(engine, playerID, 0, 2, "MEDAL", COLOR.BLUE)
					receiver.drawMenuMedal(engine, playerID, 5, 2, "AC", medalAC)
					receiver.drawMenuMedal(engine, playerID, 8, 2, "CO", medalCO)
					receiver.drawMenuMedal(engine, playerID, 2, 3, "ST", medalST)
					receiver.drawMenuMedal(engine, playerID, 6, 3, "SK", medalSK)

					if(rollPointsTotal>0) {
						receiver.drawMenuFont(engine, playerID, 0, 4, "ROLL POINT", COLOR.BLUE)
						val strRollPointsTotal = String.format("%10g", rollPointsTotal)
						receiver.drawMenuFont(engine, playerID, 0, 5, strRollPointsTotal)
					}

					drawResultStats(engine, playerID, receiver, 6, COLOR.BLUE, AbstractMode.Statistic.LPM, AbstractMode.Statistic.SPM, AbstractMode.Statistic.PIECE, AbstractMode.Statistic.PPS)
					drawResult(engine, playerID, receiver, 15, COLOR.BLUE, "DECORATION", String.format("%d", dectemp))
				}
			}
		}
	}

	/* 結果画面の処理 */
	override fun onResult(engine:GameEngine, playerID:Int):Boolean {
		if(passframe>0) {
			engine.allowTextRenderByReceiver = false // Turn off RETRY/END menu

			if(engine.ctrl!!.isPush(Controller.BUTTON_A)||engine.ctrl!!.isPush(Controller.BUTTON_B))
				if(passframe>420)
					passframe = 420
				else if(passframe<300) passframe = 0

			if(promotionFlag) {
				if(passframe==420)
					if(grade>=promotionalExam) engine.playSE("excellent")
					else engine.playSE("regret")

			} else if(demotionFlag)
				if(passframe==420)
					if(grade>=qualifiedGrade) engine.playSE("gradeup")
					else engine.playSE("gameover")

			passframe--
			return true
		}

		engine.allowTextRenderByReceiver = true

		owner.bgmStatus.fadesw = false
		owner.bgmStatus.bgm = when {
			engine.ending==1||engine.ending==2&&rollclear==0 -> BGM.RESULT(2)
			rollclear>0||promotionFlag&&grade>=promotionalExam -> BGM.RESULT(3)
			demotionFlag&&grade<=qualifiedGrade -> BGM.RESULT(0)
			else -> BGM.RESULT(0)
		}

		// ページ切り替え
		if(engine.ctrl!!.isMenuRepeatKey(Controller.BUTTON_UP)) {
			engine.statc[1]--
			if(engine.statc[1]<0) engine.statc[1] = 2
			engine.playSE("change")
		}
		if(engine.ctrl!!.isMenuRepeatKey(Controller.BUTTON_DOWN)) {
			engine.statc[1]++
			if(engine.statc[1]>2) engine.statc[1] = 0
			engine.playSE("change")
		}
		// section time display切替
		if(engine.ctrl!!.isPush(Controller.BUTTON_F)) {
			engine.playSE("change")
			isShowBestSectionTime = !isShowBestSectionTime
		}

		engine.statc[2]++

		return false
	}

	/* リプレイ保存 */
	override fun saveReplay(engine:GameEngine, playerID:Int, prop:CustomProperties) {
		saveSetting(owner.replayProp)
		owner.replayProp.setProperty("result.grade.name", getGradeName(grade))
		owner.replayProp.setProperty("result.grade.number", grade)
		owner.replayProp.setProperty("grademania3.version", version)
		owner.replayProp.setProperty("grademania3.exam", if(promotionFlag) promotionalExam else 0)
		owner.replayProp.setProperty("grademania3.demopoint", demotionPoints)
		owner.replayProp.setProperty("grademania3.demotionExamGrade", demotionExamGrade)

		// Update rankings
		if(!owner.replayMode&&startlevel==0&&!always20g&&!big&&engine.ai==null) {
			var rgrade = grade
			if(enableexam&&rgrade>=32&&qualifiedGrade<32) rgrade = 31
			// if(!enableexam || !isAnyExam())
			updateRanking(rgrade, engine.statistics.level, lastGradeTime, rollclear)
			// else
			// rankingRank = -1;

			if(enableexam) updateGradeHistory(grade)

			if(medalST==3&&!isAnyExam) updateBestSectionTime()

			if(rankingRank!=-1||enableexam||medalST==3) {
				saveRanking(owner.modeConfig, engine.ruleopt.strRuleName)
				receiver.saveModeConfig(owner.modeConfig)
			}
			owner.modeConfig.setProperty("decoration", decoration)
		}
	}

	/** Read rankings from property file
	 * @param prop Property file
	 * @param ruleName Rule name
	 */
	private fun loadRanking(prop:CustomProperties?, ruleName:String) {

		for(i in 0 until RANKING_MAX) {
			rankingGrade[i] = prop!!.getProperty("grademania3.ranking.$ruleName.grade.$i", 0)
			rankingLevel[i] = prop.getProperty("grademania3.ranking.$ruleName.level.$i", 0)
			rankingTime[i] = prop.getProperty("grademania3.ranking.$ruleName.time.$i", 0)
			rankingRollclear[i] = prop.getProperty("grademania3.ranking.$ruleName.rollclear.$i", 0)
		}
		for(i in 0 until SECTION_MAX)
			bestSectionTime[i] = prop!!.getProperty("grademania3.bestSectionTime."+ruleName+"."
				+i, tableTimeRegret[i])

		for(i in 0 until GRADE_HISTORY_SIZE)
			gradeHistory[i] = prop!!.getProperty("grademania3.gradehistory.$ruleName.$i", -1)

		qualifiedGrade = prop!!.getProperty("grademania3.qualified.$ruleName", 0)
		demotionPoints = prop.getProperty("grademania3.demopoint.$ruleName", 0)
		decoration = prop.getProperty("decoration", 0)

		setPromotionalGrade()
	}

	/** Save rankings to property file
	 * @param prop Property file
	 * @param ruleName Rule name
	 */
	private fun saveRanking(prop:CustomProperties?, ruleName:String) {
		for(i in 0 until RANKING_MAX) {
			prop!!.setProperty("grademania3.ranking.$ruleName.grade.$i", rankingGrade[i])
			prop.setProperty("grademania3.ranking.$ruleName.level.$i", rankingLevel[i])
			prop.setProperty("grademania3.ranking.$ruleName.time.$i", rankingTime[i])
			prop.setProperty("grademania3.ranking.$ruleName.rollclear.$i", rankingRollclear[i])
		}

		for(i in 0 until SECTION_MAX)
			prop!!.setProperty("grademania3.bestSectionTime.$ruleName.$i", bestSectionTime[i])

		for(i in 0 until GRADE_HISTORY_SIZE)
			prop!!.setProperty("grademania3.gradehistory.$ruleName.$i", gradeHistory[i])

		prop!!.setProperty("grademania3.qualified.$ruleName", qualifiedGrade)
		prop.setProperty("grademania3.demopoint.$ruleName", demotionPoints)

		prop.setProperty("decoration", decoration)
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
				rankingRollclear[i] = rankingRollclear[i-1]
			}

			// Add new data
			rankingGrade[rankingRank] = gr
			rankingLevel[rankingRank] = lv
			rankingTime[rankingRank] = time
			rankingRollclear[rankingRank] = clear
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
			if(gr>rankingGrade[i])
				return i
			else if(gr==rankingGrade[i]&&clear>rankingRollclear[i])
				return i
			else if(gr==rankingGrade[i]&&clear==rankingRollclear[i]&&lv>rankingLevel[i])
				return i
			else if(gr==rankingGrade[i]&&clear==rankingRollclear[i]&&lv==rankingLevel[i]
				&&time<rankingTime[i])
				return i

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
			log.debug(i.toString()+": "+getGradeName(gradeHistory[i])+" ("+gradeHistory[i]+")")

		log.debug("*** Exam grade history END ***")
		setPromotionalGrade()
	}

	/** 昇格試験の目標段位を設定 */
	private fun setPromotionalGrade() {
		var gradesOver = 0
		var highgrade:Int = 0

		for(j in 0 until GRADE_HISTORY_SIZE)
			if(gradeHistory[j]>qualifiedGrade) {
				gradesOver++
				highgrade += gradeHistory[j]

			}
		if(gradesOver>3) {
			promotionalExam = highgrade/gradesOver
			if(qualifiedGrade<31&&promotionalExam==32) promotionalExam = 31

			return
		} else
			promotionalExam = qualifiedGrade

	}

	/** Update best section time records */
	private fun updateBestSectionTime() {
		for(i in 0 until SECTION_MAX)
			if(sectionIsNewRecord[i]) bestSectionTime[i] = sectionTime[i]

	}

	companion object {
		/** Log */
		internal val log = Logger.getLogger(GrandMastery::class.java)

		/** Current version */
		private const val CURRENT_VERSION = 2

		/** Section COOL criteria Time */
		private val tableTimeCool = intArrayOf(3360, 3260, 3170, 3080, 2985, 2890, 2800, 2700, 2610, 2520)
		/** Section REGRET criteria Time */
		private val tableTimeRegret = intArrayOf(6000, 5400, 5100, 4800, 4600, 4400, 4200, 4000, 3800, 3600)
		/** 落下速度 table */
		private val tableGravityValue =
			intArrayOf(4, 6, 8, 10, 12, 16, 32, 48, 64, 80, 96, 112, 128, 144, 4, 32, 64, 96, 128, 160, 192, 224, 256, 512, 768, 1024, 1280, 1024, 768, -1)
		/** 落下速度が変わる level */
		private val tableGravityChangeLevel =
			intArrayOf(30, 35, 40, 50, 60, 70, 80, 90, 100, 120, 140, 160, 170, 200, 220, 230, 233, 236, 239, 243, 247, 251, 300, 330, 360, 400, 420, 450, 500, 10000)

		/** ARE table */
		private val tableARE = intArrayOf(25, 24, 23, 22, 21, 20, 18, 15, 11, 8, 6, 5, 4)
		/** ARE after line clear table */
		private val tableARELine = intArrayOf(25, 25, 25, 24, 22, 19, 16, 12, 9, 7, 6, 5, 4)
		/** Line clear time table */
		private val tableLineDelay = intArrayOf(40, 39, 37, 34, 30, 25, 20, 16, 13, 10, 8, 7, 6)
		/** 固定 time table */
		private val tableLockDelay = intArrayOf(30, 30, 30, 30, 30, 30, 29, 28, 26, 24, 23, 22, 21)
		/** DAS table */
		private val tableDAS = intArrayOf(15, 14, 13, 12, 11, 10, 9, 8, 7, 6, 6, 6, 6)

		/** BGM fadeout level */
		private val tableBGMFadeout = intArrayOf(485, 785, 1185, -1)
		/** BGM change level */
		private val tableBGMChange = intArrayOf(500, 800, 1200, -1)
		private val tableBGM = arrayOf(BGM.GM_3(0), BGM.GM_3(1), BGM.GM_3(2), BGM.GM_3(3))
		/** Line clear時に入る段位 point */
		private val tableGradePoint =
			arrayOf(intArrayOf(10, 10, 10, 10, 10, 9, 8, 7, 6, 5, 4, 3, 2), intArrayOf(20, 20, 20, 18, 16, 15, 13, 10, 11, 11, 12), intArrayOf(40, 36, 33, 30, 27, 24, 20, 18, 17, 16, 15), intArrayOf(50, 47, 44, 40, 40, 38, 36, 34, 32, 31, 30))
		/** 段位 pointのCombo bonus */
		private val tableGradeComboBonus =
			arrayOf(floatArrayOf(1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f), floatArrayOf(1.0f, 1.2f, 1.2f, 1.4f, 1.4f, 1.4f, 1.4f, 1.5f, 1.5f, 2.0f), floatArrayOf(1.0f, 1.4f, 1.5f, 1.6f, 1.7f, 1.8f, 1.9f, 2.0f, 2.1f, 2.5f), floatArrayOf(1.0f, 1.5f, 1.8f, 2.0f, 2.2f, 2.3f, 2.4f, 2.5f, 2.6f, 3.0f))
		/** 実際の段位を上げるのに必要な内部段位 */
		private val tableGradeChange = intArrayOf(1, 2, 3, 4, 5, 7, 9, 12, 15, 18, 19, 20, 23, 25, 27, 29, 31, -1)
		/** 段位 pointが1つ減る time */
		private val tableGradeDecayRate =
			intArrayOf(125, 100, 80, 50, 48, 47, 45, 44, 43, 42, 41, 40, 36, 33, 30, 28, 26, 24, 22, 20, 19, 18, 17, 16, 15, 15, 14, 14, 13, 13, 11, 10)

		// /** 段位のcount */
		// private static final int GRADE_MAX = 33;

		/** 段位のName */
		private val tableGradeName = arrayOf("9", "8", "7", "6", "5", "4", "3", "2", "1", // 0～ 8
			"S1", "S2", "S3", "S4", "S5", "S6", "S7", "S8", "S9", // 9～17
			"m1", "m2", "m3", "m4", "m5", "m6", "m7", "m8", "m9", // 18～26
			"M", "MK", "MV", "MO", "MM", "GM" // 27～32
		)

		/** 裏段位のName */
		private val tableSecretGradeName = arrayOf("9", "8", "7", "6", "5", "4", "3", "2", "1", // 0～ 8
			"S1", "S2", "S3", "S4", "S5", "S6", "S7", "S8", "S9", // 9～17
			"GM" // 18
		)

		/** LV999 roll time */
		private const val ROLLTIMELIMIT = 3238

		/** Number of entries in rankings */
		private const val RANKING_MAX = 10

		/** 段位履歴のサイズ */
		private const val GRADE_HISTORY_SIZE = 7

		/** 段位認定試験の発生確率(EXAM_CHANCE分の1の確率で発生) */
		private const val EXAM_CHANCE = 2

		/** Number of sections */
		private const val SECTION_MAX = 10
	}
}
