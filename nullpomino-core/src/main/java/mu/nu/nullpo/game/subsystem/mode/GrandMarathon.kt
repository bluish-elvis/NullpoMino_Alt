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
import kotlin.math.*

/** GRAND MANIA Mode */
class GrandMarathon:AbstractMode() {

	/** Current 落下速度の number (tableGravityChangeLevelの levelに到達するたびに1つ増える) */
	private var gravityindex:Int = 0

	/** Next Section の level (これ-1のときに levelストップする) */
	private var nextseclv:Int = 0

	/** Levelが増えた flag */
	private var lvupflag:Boolean = false

	/** 段位 */
	private var grade:Int = 0
	private var gmPier:Int = 0

	/** 最後に段位が上がった time */
	private var lastGradeTime:Int = 0

	/** Combo bonus */
	private var comboValue:Int = 0

	/** Most recent increase in score */
	private var lastscore:Int = 0

	/** 獲得Render scoreがされる残り time */
	private var scgettime:Int = 0

	/** Roll 経過 time */
	private var rolltime:Int = 0

	/** LV300到達時に段位が規定数以上だったらtrueになる */
	private var gm300:Boolean = false

	/** LV500到達時に段位が規定数以上＆Timeが規定以下だったらtrueになる */
	private var gm500:Boolean = false

	/** 裏段位 */
	private var secretGrade:Int = 0

	/** Current BGM */
	private var bgmlv:Int = 0

	/** 段位表示を光らせる残り frame count */
	private var gradeflash:Int = 0

	/** Section Time */
	private var sectionTime:IntArray = IntArray(SECTION_MAX)
	private var sectionscore:IntArray = IntArray(SECTION_MAX)
	/** Section Time記録 */
	private var bestSectionTime:IntArray = IntArray(SECTION_MAX)
	private var bestSectionScore:IntArray = IntArray(SECTION_MAX)

	/** 新記録が出たSection はtrue */
	private var sectionIsNewRecord:BooleanArray = BooleanArray(SECTION_MAX)

	/** どこかのSection で新記録を出すとtrue */
	private val sectionAnyNewRecord:Boolean get() = sectionIsNewRecord.any {true}

	/** Cleared Section count */
	private var sectionscomp:Int = 0

	/** Average Section Time */
	private var sectionavgtime:Int = 0

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

	/** Version */
	private var version:Int = 0

	/** Current round's ranking rank */
	private var rankingRank:Int = 0

	/** Rankings' 段位 */
	private var rankingGrade:IntArray = IntArray(RANKING_MAX)

	/** Rankings' level */
	private var rankingLevel:IntArray = IntArray(RANKING_MAX)

	/** Rankings' times */
	private var rankingTime:IntArray = IntArray(RANKING_MAX)

	private var medalAC:Int = 0
	private var decoration:Int = 0
	private var dectemp:Int = 0

	/* Mode name */
	override val name:String
		get() = "GRAND MARATHON"

	/* Initialization */
	override fun playerInit(engine:GameEngine, playerID:Int) {
		super.playerInit(engine, playerID)
		menuTime = 0
		gravityindex = 0
		nextseclv = 0
		lvupflag = true
		gmPier = 0
		grade = gmPier
		lastGradeTime = 0
		comboValue = 0
		lastscore = 0
		scgettime = 0
		rolltime = 0
		gm300 = false
		gm500 = false
		secretGrade = 0
		bgmlv = 0
		gradeflash = 0
		sectionscore = IntArray(SECTION_MAX)
		sectionTime = IntArray(SECTION_MAX)
		sectionIsNewRecord = BooleanArray(SECTION_MAX)
		sectionscomp = 0
		sectionavgtime = 0
		isShowBestSectionTime = false
		startlevel = 0
		alwaysghost = false
		always20g = false
		lvstopse = false
		big = false
		medalAC = 0
		decoration = 0
		dectemp = 0

		rankingRank = -1
		rankingGrade = IntArray(RANKING_MAX)
		rankingLevel = IntArray(RANKING_MAX)
		rankingTime = IntArray(RANKING_MAX)
		bestSectionScore = IntArray(SECTION_MAX)
		bestSectionTime = IntArray(SECTION_MAX)

		engine.tspinEnable = false
		engine.b2bEnable = true
		engine.comboType = GameEngine.COMBO_TYPE_DOUBLE
		engine.bighalf = false
		engine.bigmove = false
		engine.staffrollNoDeath = true

		engine.speed.are = 30
		engine.speed.areLine = 30
		engine.speed.lineDelay = 41
		engine.speed.lockDelay = 30
		engine.speed.das = 15

		version = if(owner.replayMode) {
			loadSetting(owner.replayProp)
			owner.replayProp.getProperty("grademania1.version", 0)
		} else {
			loadSetting(owner.modeConfig)
			loadRanking(owner.recordProp, engine.ruleopt.strRuleName)
			CURRENT_VERSION
		}

		owner.backgroundStatus.bg = startlevel
	}

	/** Load settings from property file
	 * @param prop Property file
	 */
	override fun loadSetting(prop:CustomProperties) {
		startlevel = prop.getProperty("grademania1.startlevel", 0)
		alwaysghost = prop.getProperty("grademania1.alwaysghost", true)
		always20g = prop.getProperty("grademania1.always20g", false)
		lvstopse = prop.getProperty("grademania1.lvstopse", true)
		showsectiontime = prop.getProperty("grademania1.showsectiontime", true)
		big = prop.getProperty("grademania1.big", false)
	}

	/** Save settings to property file
	 * @param prop Property file
	 */
	override fun saveSetting(prop:CustomProperties) {
		prop.setProperty("grademania1.startlevel", startlevel)
		prop.setProperty("grademania1.alwaysghost", alwaysghost)
		prop.setProperty("grademania1.always20g", always20g)
		prop.setProperty("grademania1.lvstopse", lvstopse)
		prop.setProperty("grademania1.showsectiontime", showsectiontime)
		prop.setProperty("grademania1.big", big)
	}

	/** Update falling speed
	 * @param engine GameEngine
	 */
	private fun setSpeed(engine:GameEngine) {
		if(always20g) engine.speed.gravity = -1
		else {
			while(engine.statistics.level>=tableGravityChangeLevel[gravityindex])
				gravityindex++
			engine.speed.gravity = tableGravityValue[gravityindex]
		}

		var section = if(engine.statistics.level<300) 0 else if(engine.statistics.level<500) 1 else 2
		if(section>tableARE.size-1) section = tableARE.size-1
		engine.speed.das = tableDAS[section]

		if(engine.statistics.time>=54000) {
			engine.speed.are = 3
			engine.speed.areLine = 6
			engine.speed.lineDelay = 6
			engine.speed.lockDelay = 19
			engine.speed.gravity = -1
		} else {
			engine.speed.areLine = tableARE[section]
			engine.speed.are = engine.speed.areLine
			engine.speed.lineDelay = tableLineDelay[section]
			engine.speed.lockDelay = tableLockDelay[section]
		}
	}

	/** Update average section time */
	private fun setAverageSectionTime() {
		if(sectionscomp>0) {
			var temp = 0
			for(i in startlevel until startlevel+sectionscomp)
				if(i>=0&&i<sectionTime.size) temp += sectionTime[i]

			sectionavgtime = temp/sectionscomp
		} else sectionavgtime = 0

	}

	/** Section Time更新処理
	 * @param sectionNumber Section number
	 */
	private fun stNewRecordCheck(sectionNumber:Int) {
		if(sectionTime[sectionNumber]<bestSectionTime[sectionNumber]&&!owner.replayMode)
			sectionIsNewRecord[sectionNumber] = true

	}

	/* Called at settings screen */
	override fun onSetting(engine:GameEngine, playerID:Int):Boolean {
		// Menu
		if(!engine.owner.replayMode) {
			// Configuration changes
			val change = updateCursor(engine, 5)

			if(change!=0) {
				engine.playSE("change")

				when(menuCursor) {
					0 -> {
						startlevel += change
						if(startlevel<0) startlevel = 9
						if(startlevel>9) startlevel = 0
						owner.backgroundStatus.bg = startlevel
					}
					1 -> alwaysghost = !alwaysghost
					2 -> always20g = !always20g
					3 -> lvstopse = !lvstopse
					4 -> showsectiontime = !showsectiontime
					5 -> big = !big
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
				owner.saveModeConfig()
				isShowBestSectionTime = false
				sectionscomp = 0
				bgmlv = if(engine.statistics.level<500) 0 else 1
				owner.bgmStatus.bgm = if(engine.statistics.level<500) BGM.GM_1(0) else BGM.GM_1(1)
				return false
			}

			// Cancel
			if(engine.ctrl!!.isPush(Controller.BUTTON_B)) engine.quitflag = true

			menuTime++
		} else {
			menuTime++
			menuCursor = -1

			bgmlv = if(engine.statistics.level<500) 0 else 1
			owner.bgmStatus.bgm = if(engine.statistics.level<500) BGM.GM_1(0) else BGM.GM_1(1)
			return menuTime<60
		}

		return true
	}

	/* Render the settings screen */
	override fun renderSetting(engine:GameEngine, playerID:Int) {
		drawMenu(engine, playerID, receiver, 0, COLOR.BLUE, 0, "LEVEL", (startlevel*100).toString(),
			"FULL GHOST", GeneralUtil.getONorOFF(alwaysghost), "20G MODE", GeneralUtil.getONorOFF(always20g),
			"LVSTOPSE", GeneralUtil.getONorOFF(lvstopse), "SHOW STIME", GeneralUtil.getONorOFF(showsectiontime), "BIG", GeneralUtil.getONorOFF(big))
	}

	/* Called at game start */
	override fun startGame(engine:GameEngine, playerID:Int) {
		engine.statistics.level = startlevel*100

		dectemp = 0
		nextseclv = engine.statistics.level+100
		if(engine.statistics.level<0) nextseclv = 100
		if(engine.statistics.level>=900) nextseclv = 999

		owner.backgroundStatus.bg = engine.statistics.level/100

		engine.big = big

		setSpeed(engine)
	}

	/* Render score */
	override fun renderLast(engine:GameEngine, playerID:Int) {
		receiver.drawScoreFont(engine, playerID, 0, 0, "GRAND MARATHON", COLOR.CYAN)

		receiver.drawScoreFont(engine, playerID, -1, -4*2, "DECORATION", scale = .5f)
		receiver.drawScoreBadges(engine, playerID, 0, -3, 100, decoration)
		receiver.drawScoreBadges(engine, playerID, 5, -4, 100, dectemp)
		if(engine.stat==GameEngine.Status.SETTING||engine.stat==GameEngine.Status.RESULT&&!owner.replayMode) {
			if(!owner.replayMode&&startlevel==0&&!big&&!always20g
				&&engine.ai==null)
				if(!isShowBestSectionTime) {
					// Rankings
					receiver.drawScoreFont(engine, playerID, 3, 2, "GRADE TIME LEVEL", COLOR.BLUE)

					for(i in 0 until RANKING_MAX) {
						receiver.drawScoreGrade(engine, playerID, 0, 3+i, String.format("%2d", i+1), COLOR.YELLOW)
						var gc = if(i==rankingRank)
							if(playerID%2==0) COLOR.YELLOW
							else COLOR.ORANGE
						else COLOR.WHITE
						if(rankingGrade[i]>=18) {
							var gmP = 0
							for(l in 1 until tablePier21GradeTime.size)
								if(rankingTime[i]<tablePier21GradeTime[l]) gmP = l
							gc = tablePier21GradeColor[gmP]
						}
						if(rankingGrade[i]>=0&&rankingGrade[i]<tableGradeName.size)
							receiver.drawScoreGrade(engine, playerID, 3, 3+i, tableGradeName[rankingGrade[i]], gc)
						receiver.drawScoreNum(engine, playerID, 7, 3+i, GeneralUtil.getTime(rankingTime[i]), i==rankingRank)
						receiver.drawScoreNum(engine, playerID, 15, 3+i, String.format("%03d", rankingLevel[i]), i==rankingRank)
					}

					receiver.drawScoreFont(engine, playerID, 0, 17, "F:VIEW SECTION TIME", COLOR.GREEN)
				} else {
					// Section Time
					receiver.drawScoreFont(engine, playerID, 0, 2, "SECTION TIME SCORE", COLOR.BLUE)

					var totalTime = 0
					for(i in 0 until SECTION_MAX) {
						val temp = minOf(i*100, 999)
						val temp2 = minOf((i+1)*100-1, 999)

						val strSectionTime = String.format("%3d-%3d %s %d", temp, temp2, GeneralUtil.getTime(bestSectionTime[i]), bestSectionScore[i])

						receiver.drawScoreNum(engine, playerID, 0, 3+i, strSectionTime, sectionIsNewRecord[i])
						totalTime += bestSectionTime[i]
					}

					receiver.drawScoreFont(engine, playerID, 0, 14, "TOTAL", color = COLOR.BLUE)
					receiver.drawScoreNum(engine, playerID, 0, 15, GeneralUtil.getTime(totalTime), 2f)
					receiver.drawScoreFont(engine, playerID, if(receiver.nextDisplayType==2)
						0 else 12, if(receiver.nextDisplayType==2) 18 else 14, "AVERAGE", color = COLOR.BLUE)
					receiver.drawScoreNum(engine, playerID, if(receiver.nextDisplayType==2)
						0 else 12, if(receiver.nextDisplayType==2) 19 else 15, GeneralUtil.getTime((totalTime/SECTION_MAX)), scale = 2f)

					receiver.drawScoreFont(engine, playerID, 0, 17, "F:VIEW RANKING", color = COLOR.GREEN)
				}
		} else {
			val g20 = engine.speed.gravity<0&&(engine.statistics.time/2%2==0||engine.ending>0)
			receiver.drawScoreFont(engine, playerID, 0, 2, "GRADE",
				color = if(g20 or (gradeflash>0&&gradeflash%4==0)) if(gm300) COLOR.YELLOW else COLOR.CYAN else COLOR.BLUE)
			receiver.drawScoreGrade(engine, playerID, 5, 2, tableGradeName[grade],
				color = if(gradeflash>0&&gradeflash%(if(grade>=18) 2 else 4)==0)
					(if(grade>=18) tablePier21GradeColor[gmPier] else COLOR.YELLOW) else COLOR.WHITE, scale = 2f)

			if(grade<17) {
				receiver.drawScoreNano(engine, playerID, 0, 3*2+1, "NEXT AT",
					if(g20 or (gradeflash>0&&gradeflash%4==0)) if(gm300) COLOR.YELLOW else COLOR.CYAN else COLOR.BLUE, .5f)
				receiver.drawScoreNum(engine, playerID, 0, 4, "${tableGradeScore[grade]}")
			}

			// 段位上昇時のフラッシュ
			if(gradeflash>0) gradeflash--

			receiver.drawScoreFont(engine, playerID, 0, 5, "SCORE",
				color = if(g20) if(gm300&&gm500) COLOR.YELLOW else COLOR.CYAN else COLOR.BLUE)
			receiver.drawScoreNum(engine, playerID, 5, 5, "+$lastscore", if(g20) COLOR.YELLOW else COLOR.WHITE)
			if(scgettime<engine.statistics.score) scgettime += ceil(((engine.statistics.score-scgettime)/10f).toDouble())
				.toInt()
			receiver.drawScoreNum(engine, playerID, 0, 6, "$scgettime",
				if(g20) COLOR.YELLOW else COLOR.WHITE, 2f)

			receiver.drawScoreFont(engine, playerID, 0, 9, "LEVEL",
				if(g20) if(gm300&&gm500) COLOR.YELLOW else COLOR.CYAN else COLOR.BLUE)
			receiver.drawScoreNum(engine, playerID, 0, 10, String.format("%3d", maxOf(engine.statistics.level, 0)), g20)
			receiver.drawSpeedMeter(engine, playerID, 0, 11, if(g20) 40 else floor(ln(engine.speed.gravity.toDouble())).toInt()*4)
			receiver.drawScoreNum(engine, playerID, 0, 12, String.format("%3d", nextseclv), g20)

			receiver.drawScoreFont(engine, playerID, 0, 14, "TIME",
				if(g20) if(gm500) COLOR.YELLOW else COLOR.CYAN else COLOR.BLUE)
			if(engine.ending!=2||rolltime/30%2==0)
				receiver.drawScoreNum(engine, playerID, 0, 15, GeneralUtil.getTime(engine.statistics.time), g20, 2f)

			if(engine.gameActive&&engine.ending==2) {
				var time = ROLLTIMELIMIT-rolltime
				if(time<0) time = 0
				receiver.drawScoreFont(engine, playerID, 0, 17, "ROLL TIME", COLOR.BLUE)
				receiver.drawScoreNum(engine, playerID, 0, 18, GeneralUtil.getTime(time), time>0&&time<10*60, 2f)
			}

			// Section Time
			if(showsectiontime&&sectionTime.isNotEmpty()) {
				val x = receiver.nextDisplayType==2
				receiver.drawScoreFont(engine, playerID, if(x) 8 else 10, 2, "SECTION TIME", COLOR.BLUE)

				for(i in sectionTime.indices)
					if(sectionTime[i]>0) {
						var temp = i*100
						if(temp>999) temp = 999

						val section = engine.statistics.level/100
						var strSeparator = "-"
						if(i==section&&engine.ending==0) strSeparator = "+"
						val strSectionTime = StringBuilder()
						for(l in 0 until i)
							strSectionTime.append("\n")
						strSectionTime.append(String.format("%3d%s%s %d\n", temp, strSeparator, GeneralUtil.getTime(sectionTime[i]), sectionscore[i]))
						receiver.drawScoreNum(engine, playerID, if(x) 9 else 10, 3, "$strSectionTime", sectionIsNewRecord[i],
							if(x) .75f else 1f)
					}

				receiver.drawScoreFont(engine, playerID, if(x) 8 else 12, if(x) 11 else 14, "AVERAGE", COLOR.BLUE)
				receiver.drawScoreNum(engine, playerID, if(x) 8 else 12, if(x) 12 else 15,
					GeneralUtil.getTime((engine.statistics.time/(sectionscomp+if(engine.ending==0) 1 else 0))), scale = 2f)

			}
			// medal
			receiver.drawScoreMedal(engine, playerID, 0, 20, "AC", medalAC)

		}
	}

	/* 移動中の処理 */
	override fun onMove(engine:GameEngine, playerID:Int):Boolean {
		// 新規ピース出現時
		if(engine.ending==0&&engine.statc[0]==0&&!engine.holdDisable&&!lvupflag) {
			if(engine.statistics.level<nextseclv-1) {
				engine.statistics.level++
				if(engine.statistics.level==nextseclv-1&&lvstopse) engine.playSE("levelstop")
			}
			levelUp(engine)
		}
		if(engine.ending==0&&engine.statc[0]>0&&(version>=1||!engine.holdDisable)) lvupflag = false

		return false
	}

	/* ARE中の処理 */
	override fun onARE(engine:GameEngine, playerID:Int):Boolean {
		// 最後の frame
		if(engine.ending==0&&engine.statc[0]>=engine.statc[1]-1&&!lvupflag) {
			if(engine.statistics.level<nextseclv-1) {
				engine.statistics.level++
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

		// 速度変更
		setSpeed(engine)

		// LV100到達でghost を消す
		if(engine.statistics.level>=100&&!alwaysghost) engine.ghost = false

		// BGM fadeout
		if(bgmlv==0&&engine.statistics.level>=490) owner.bgmStatus.fadesw = true
	}

	/* Calculate score */
	override fun calcScore(engine:GameEngine, playerID:Int, lines:Int) {
		if(engine.ending!=0) return

		// Combo
		comboValue = if(lines==0) 1
		else maxOf(1,comboValue+2*lines-2)

		if(lines>=1) {
			// Calculate score
			var manuallock = 0
			if(engine.manualLock) manuallock = 1

			var bravo = 1
			if(engine.field!!.isEmpty) {
				bravo = 4

				dectemp += lines*25
				if(lines==3) dectemp += 25
				if(lines==4) dectemp += 150
				if(medalAC<3) {
					dectemp += 3+medalAC*4// 3 10 21
					engine.playSE("medal${++medalAC}")
				}
			}

			lastscore = (((engine.statistics.level+lines)/(if(engine.b2b) 4 else 3)+engine.softdropFall+engine.harddropFall+manuallock)
				*lines*comboValue*bravo)
			sectionscore[sectionscomp] += lastscore
			engine.statistics.scoreLine += lastscore

			// 段位上昇
			while(grade<17&&engine.statistics.score>=tableGradeScore[grade]) {
				engine.playSE("gradeup")
				engine.playSE("grade${grade*4/17}")
				grade++
				dectemp++
				gradeflash = 180
				lastGradeTime = engine.statistics.time
			}

			// Level up
			engine.statistics.level += lines
			levelUp(engine)
			if(engine.statistics.level>=999) {
				// Ending
				engine.statistics.level = 999
				engine.timerActive = false
				lastGradeTime = engine.statistics.time

				sectionscomp++
				setAverageSectionTime()
				stNewRecordCheck(sectionscomp-1)

				if(engine.statistics.time<=GM_999_TIME_REQUIRE&&engine.statistics.score>=tableGradeScore[17]&&gm300
					&&gm500) {
					engine.playSE("applause5")
					engine.playSE("endingstart")
					engine.playSE("grade4")

					grade = 18
					gradeflash = ROLLTIMELIMIT

					gmPier = 0
					for(i in 1 until tablePier21GradeTime.size)
						if(engine.statistics.time<tablePier21GradeTime[i]) gmPier = i
					if(gmPier>3) grade++
					owner.bgmStatus.fadesw = false
					owner.bgmStatus.bgm = BGM.ENDING(0)

					engine.ending = 2
				} else {
					engine.playSE("applause4")
					engine.gameEnded()
					engine.ending = 1
				}
			} else if(engine.statistics.level>=nextseclv) {
				// Next Section

				owner.backgroundStatus.fadesw = true
				owner.backgroundStatus.fadecount = 0
				owner.backgroundStatus.fadebg = nextseclv/100

				if(nextseclv==300&&grade>=GM_300_GRADE_REQUIRE&&engine.statistics.time<=GM_300_TIME_REQUIRE) {
					gm300 = true
					engine.playSE("cool")
				} else if(nextseclv==500&&grade>=GM_500_GRADE_REQUIRE&&engine.statistics.time<=GM_500_TIME_REQUIRE) {
					gm500 = true
					engine.playSE("cool")
				}
				engine.playSE(if(nextseclv==300||nextseclv==500) "levelup_section" else "levelup")

				sectionscomp++
				setAverageSectionTime()
				stNewRecordCheck(sectionscomp-1)

				if(bgmlv==0&&nextseclv==500) {
					bgmlv++
					owner.bgmStatus.fadesw = false
					owner.bgmStatus.bgm = BGM.GM_1(1)
				}

				nextseclv += 100
				if(nextseclv>999) nextseclv = 999
			} else if(engine.statistics.level==nextseclv-1&&lvstopse) engine.playSE("levelstop")

		}
	}

	/* 各 frame の終わりの処理 */
	override fun onLast(engine:GameEngine, playerID:Int) {

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
				engine.gameEnded()
				engine.resetStatc()
				engine.stat = GameEngine.Status.EXCELLENT
			}
		}
	}

	/* Called at game over */
	override fun onGameOver(engine:GameEngine, playerID:Int):Boolean {
		if(engine.statc[0]==0) {
			secretGrade = engine.field!!.secretGrade
			val time = engine.statistics.time
			if(grade>=18)
				for(i in 1 until tablePier21GradeTime.size)
					if(time<tablePier21GradeTime[i]) dectemp++

			if(time<6000) dectemp -= 3
			else {
				dectemp++
				if(time%3600<=60||time%3600>=3540) dectemp++
			}
			decoration += dectemp+secretGrade
		}
		return false
	}

	override fun renderExcellent(engine:GameEngine, playerID:Int) {
		val offsetX = receiver.fieldX(engine, playerID)
		val offsetY = receiver.fieldY(engine, playerID)

		if(grade==18) {
			val col = when {
				engine.statc[0]%4==0 -> COLOR.YELLOW
				engine.statc[0]%2==0 -> COLOR.WHITE
				else -> COLOR.ORANGE
			}
			receiver.drawDirectFont(offsetX+12, offsetY+230, "YOU ARE A", COLOR.WHITE, 1f)
			receiver.drawDirectFont(offsetX+22, offsetY+250, "GRAND", col, 1.5f)
			receiver.drawDirectFont(offsetX+10, offsetY+274, "MASTER", col, 1.5f)
		} else if(grade==17) {
			val col = when {
				engine.statc[0]%4==0 -> COLOR.CYAN
				engine.statc[0]%2==0 -> COLOR.WHITE
				else -> COLOR.BLUE
			}
			receiver.drawDirectFont(offsetX+44, offsetY+250, "BUT...", COLOR.WHITE, 1f)
			receiver.drawDirectFont(offsetX+12, offsetY+266, "CHALLENGE", COLOR.BLUE, 1f)
			receiver.drawDirectFont(offsetX-4, offsetY+282, "MORE FASTER", col, 1f)
			receiver.drawDirectFont(offsetX+12, offsetY+298, "NEXT TIME", COLOR.WHITE, 1f)
		}
	}

	/* 結果画面 */
	override fun renderResult(engine:GameEngine, playerID:Int) {
		receiver.drawMenuFont(engine, playerID, 0, 0, "kn PAGE${engine.statc[1]+1}/3", COLOR.RED)

		when(engine.statc[1]) {
			0 -> {
				receiver.drawMenuFont(engine, playerID, 0, 2, "GRADE", COLOR.BLUE)
				var gc = COLOR.WHITE
				if(grade>=18) {
					gc = tablePier21GradeColor[gmPier]
					receiver.drawMenuFont(engine, playerID, 0, 3, tablePier21GradeName[gmPier], gc)
				}
				receiver.drawMenuGrade(engine, playerID, 6, 2, tableGradeName[grade], gc, 2f)
				drawResultStats(engine, playerID, receiver, 4, COLOR.BLUE, Statistic.SCORE, Statistic.LINES, Statistic.LEVEL_MANIA, Statistic.TIME)
				drawResultRank(engine, playerID, receiver, 13, COLOR.BLUE, rankingRank)
				if(secretGrade>4)
					drawResult(engine, playerID, receiver, 15, COLOR.BLUE, "S. GRADE", String.format("%10s", tableGradeName[secretGrade-1]))
			}
			1 -> {
				receiver.drawMenuFont(engine, playerID, 0, 2, "SECTION", COLOR.BLUE)

				for(i in sectionTime.indices)
					if(sectionTime[i]>0)
						receiver.drawMenuNum(engine, playerID, 2, 3+i, GeneralUtil.getTime(sectionTime[i]), sectionIsNewRecord[i])

				if(sectionavgtime>0) {
					receiver.drawMenuFont(engine, playerID, 0, 14, "AVERAGE", COLOR.BLUE)
					receiver.drawMenuNum(engine, playerID, 0, 15, GeneralUtil.getTime(sectionavgtime), 1.7f)
				}
			}
			2 -> {

				receiver.drawMenuFont(engine, playerID, 0, 2, "MEDAL", COLOR.BLUE)
				receiver.drawMenuMedal(engine, playerID, 8, 2, "AC", medalAC)
				drawResultStats(engine, playerID, receiver, 4, COLOR.BLUE, Statistic.LPM, Statistic.SPM, Statistic.PIECE, Statistic.PPS)

				drawResult(engine, playerID, receiver, 15, COLOR.BLUE, "DECORATION", String.format("%d", dectemp))
			}
		}
	}

	/* 結果画面の処理 */
	override fun onResult(engine:GameEngine, playerID:Int):Boolean {
		owner.bgmStatus.fadesw = false
		owner.bgmStatus.bgm = when(engine.ending) {
			0 -> BGM.RESULT(0)
			2 -> BGM.RESULT(3)
			else -> BGM.RESULT(2)

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

		return false
	}

	/* リプレイ保存 */
	override fun saveReplay(engine:GameEngine, playerID:Int, prop:CustomProperties) {
		saveSetting(owner.replayProp)
		owner.replayProp.setProperty("result.grade.name", tableGradeName[grade])
		owner.replayProp.setProperty("result.grade.number", grade)
		owner.replayProp.setProperty("grademania1.version", version)

		// Update rankings
		if(!owner.replayMode&&startlevel==0&&!always20g&&!big&&engine.ai==null) {
			updateRanking(grade, engine.statistics.level, lastGradeTime)
			if(sectionAnyNewRecord) updateBestSectionTime()

			if(rankingRank!=-1||sectionAnyNewRecord) {
				saveRanking(engine.ruleopt.strRuleName)
				owner.saveModeConfig()
			}
		}
	}

	/** Read rankings from property file
	 * @param prop Property file
	 * @param ruleName Rule name
	 */
	override fun loadRanking(prop:CustomProperties, ruleName:String) {
		for(i in 0 until RANKING_MAX) {
			rankingGrade[i] = prop.getProperty("$ruleName.$i.grade", 0)
			rankingLevel[i] = prop.getProperty("$ruleName.$i.level", 0)
			rankingTime[i] = prop.getProperty("$ruleName.$i.time", 0)
		}
		for(i in 0 until SECTION_MAX) {
			bestSectionScore[i] = prop.getProperty("$ruleName.sectionscore.$i", 0)
			bestSectionTime[i] = prop.getProperty("$ruleName.sectiontime.$i", DEFAULT_SECTION_TIME)
		}
		decoration = owner.statsProp.getProperty("decoration", 0)
	}

	/** Save rankings to property file
	 * @param prop Property file
	 * @param ruleName Rule name
	 */
	fun saveRanking(ruleName:String) {
		super.saveRanking(ruleName, (0 until RANKING_MAX).flatMap {i ->
			listOf("$ruleName.$i.grade" to rankingGrade[i],
				"$ruleName.$i.level" to rankingLevel[i],
				"$ruleName.$i.time" to rankingTime[i])
		}+ (0 until SECTION_MAX).flatMap {i ->
			listOf("$ruleName.sectiontime.$i" to bestSectionTime[i],
				"$ruleName.sectionscore.$i" to bestSectionScore[i])
		})

		owner.statsProp.setProperty("decoration", decoration)
		receiver.saveProperties(owner.statsFile, owner.statsProp)
	}

	/** Update rankings
	 * @param gr 段位
	 * @param lv level
	 * @param time Time
	 */
	private fun updateRanking(gr:Int, lv:Int, time:Int) {
		rankingRank = checkRanking(gr, lv, time)

		if(rankingRank!=-1) {
			// Shift down ranking entries
			for(i in RANKING_MAX-1 downTo rankingRank+1) {
				rankingGrade[i] = rankingGrade[i-1]
				rankingLevel[i] = rankingLevel[i-1]
				rankingTime[i] = rankingTime[i-1]
			}

			// Add new data
			rankingGrade[rankingRank] = gr
			rankingLevel[rankingRank] = lv
			rankingTime[rankingRank] = time
		}
	}

	/** Calculate ranking position
	 * @param gr 段位
	 * @param lv level
	 * @param time Time
	 * @return Position (-1 if unranked)
	 */
	private fun checkRanking(gr:Int, lv:Int, time:Int):Int {
		for(i in 0 until RANKING_MAX)
			if(gr>rankingGrade[i])
				return i
			else if(gr==rankingGrade[i]&&lv>rankingLevel[i])
				return i
			else if(gr==rankingGrade[i]&&lv==rankingLevel[i]&&time<rankingTime[i]) return i

		return -1
	}

	/** Update best section time records */
	private fun updateBestSectionTime() {
		for(i in 0 until SECTION_MAX)
			if(sectionIsNewRecord[i]) {
				bestSectionTime[i] = sectionTime[i]
				bestSectionScore[i] = sectionscore[i]
			}
	}

	companion object {
		/** Current version */
		private const val CURRENT_VERSION = 1

		/** 落下速度 table */
		private val tableGravityValue = intArrayOf(4, 6, 8, 10, 12, 16, 32, 48, 64, 80, 96, 112, 128, 144, 4, 32, 64, 96, 128, 160, 192, 224, 256, 512, 768, 1024, 1280, 1024, 768, -1)
		/** 落下速度が変わる level */
		private val tableGravityChangeLevel = intArrayOf(30, 35, 40, 50, 60, 70, 80, 90, 100, 120, 140, 160, 170, 200, 220, 230, 233, 236, 239, 243, 247, 251, 300, 330, 360, 400, 420, 450, 500, 10000)
		/** ARE table */
		private val tableARE = intArrayOf(30, 27, 25)
		/** Line clear time table */
		private val tableLineDelay = intArrayOf(41, 40, 25)
		/** 固定 time table */
		private val tableLockDelay = intArrayOf(30, 30, 30)
		/** DAS table */
		private val tableDAS = intArrayOf(16, 15, 14)

		/** 段位上昇に必要なScore */
		private val tableGradeScore = intArrayOf(500, 1000, 1500, 2500, 3500, 5000, 8000, 12000, // 8～1
			16000, 22000, 30000, 40000, 52000, 66000, 82000, 100000, 123456, // S1～S9
			131072 // GM
		)

		/** 段位のName */
		private val tableGradeName = arrayOf("9", "8", "7", "6", "5", "4", "3", "2", "1", // 0～8
			"S1", "S2", "S3", "S4", "S5", "S6", "S7", "S8", "S9", // 9～17
			"Gm", "GM" // 18
		)

		private const val ROLLTIMELIMIT = 2968

		/** GMを取るために必要なLV300到達時の最低段位 */
		private const val GM_300_GRADE_REQUIRE = 8

		/** GMを取るために必要なLV500到達時の最低段位 */
		private const val GM_500_GRADE_REQUIRE = 12

		/** GMを取るために必要なLV300到達時のTime */
		private const val GM_300_TIME_REQUIRE = 16000

		/** GMを取るために必要なLV500到達時のTime */
		private const val GM_500_TIME_REQUIRE = 27000

		/** GMを取るために必要なLV999到達時のTime */
		private const val GM_999_TIME_REQUIRE = 48600

		/** GMの時の評価Time */
		private val tablePier21GradeTime = intArrayOf(GM_999_TIME_REQUIRE, 44800, 40000, 38166, 36333, 34500, 33000)
		/** GM */
		private val tablePier21GradeName = arrayOf("CARBON", "STEEL", "BRONZE", "SILVER", "GOLD", "PLATINUM", "DIAMOND")

		private val tablePier21GradeColor = arrayOf(COLOR.PURPLE, COLOR.BLUE, COLOR.RED, COLOR.WHITE, COLOR.YELLOW, COLOR.CYAN, COLOR.GREEN)
		/** LV999 roll time */
		/** Number of entries in rankings */
		private const val RANKING_MAX = 10

		/** Number of sections */
		private const val SECTION_MAX = 10

		/** Default section time */
		private const val DEFAULT_SECTION_TIME = 5400
	}

}
