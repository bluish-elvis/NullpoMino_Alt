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
import mu.nu.nullpo.game.component.Block
import mu.nu.nullpo.game.component.Controller
import mu.nu.nullpo.game.event.EventReceiver.COLOR
import mu.nu.nullpo.game.event.ScoreEvent
import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.game.subsystem.mode.menu.BooleanMenuItem
import mu.nu.nullpo.game.subsystem.mode.menu.DelegateMenuItem
import mu.nu.nullpo.game.subsystem.mode.menu.LevelGrandMenuItem
import mu.nu.nullpo.game.subsystem.mode.menu.MenuList
import mu.nu.nullpo.gui.common.fx.particles.BlockParticle
import mu.nu.nullpo.gui.common.fx.particles.BlockParticle.Mapper
import mu.nu.nullpo.util.CustomProperties
import mu.nu.nullpo.util.GeneralUtil.toInt
import mu.nu.nullpo.util.GeneralUtil.toTimeStr
import kotlin.math.floor
import kotlin.math.ln

/** GRAND MANIA Mode */
class GrandM1:AbstractMode() {
	/** Next Section の level (これ-1のときに levelストップする) */
	private var nextSecLv = 0

	/** Levelが増えた flag */
	private var lvupFlag = false

	/** 段位 */
	private var grade = 0
	private var gmPier = 0

	/** 最後に段位が上がった time */
	private var lastGradeTime = 0

	/** Combo bonus */
	private var comboValue = 0

	/** Roll 経過 time */
	private var rollTime = 0

	/** LV300到達時に段位が規定数以上だったらtrueになる */
	private var gm300 = false

	/** LV500到達時に段位が規定数以上＆Timeが規定以下だったらtrueになる */
	private var gm500 = false

	/** 裏段位 */
	private var secretGrade = 0

	/** Current BGM */
	private var bgmLv = 0

	/** 段位表示を光らせる残り frame count */
	private var gradeFlash = 0

	/** Section Time */
	private val sectionTime = MutableList(SECTION_MAX) {0}
	private val sectionScore = MutableList(SECTION_MAX) {0}

	/** Section Time記録 */
	private val bestSectionTime = MutableList(SECTION_MAX) {DEFAULT_SECTION_TIME}
	private val bestSectionScore = MutableList(SECTION_MAX) {0}

	/** 新記録が出たSection はtrue */
	private val sectionIsNewRecord = MutableList(SECTION_MAX) {false}

	/** どこかのSection で新記録を出すとtrue */
	private val sectionAnyNewRecord:Boolean get() = sectionIsNewRecord.any {true}

	/** Cleared Section count */
	private var sectionsDone = 0

	/** Average Section Time */
	private val sectionAvgTime
		get() = sectionTime.filter {it>0}.average().toFloat()

	/** Section Time記録表示中ならtrue */
	private var isShowBestSectionTime = false

	private val itemLevel = LevelGrandMenuItem(COLOR.BLUE)
	/** Level at start */
	private var startLevel:Int by DelegateMenuItem(itemLevel)

	private val itemGhost = BooleanMenuItem("alwaysghost", "FULL GHOST", COLOR.BLUE, true)
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

	/** Version */
	private var version = 0

	/** Current round's ranking position */
	private var rankingRank = 0

	/** Rankings' 段位 */
	private val rankingGrade = MutableList(RANKING_MAX) {0}

	/** Rankings' level */
	private val rankingLevel = MutableList(RANKING_MAX) {0}

	/** Rankings' times */
	private val rankingTime = MutableList(RANKING_MAX) {-1}

	private var medalAC = 0
	private var decoration = 0
	private var decTemp = 0

	/* Mode name */
	override val name = "Grand Marathon"
	override val gameIntensity = 1

	/* Initialization */
	override val menu = MenuList("grademania1", itemGhost, itemAlert, itemST, itemLevel, item20g, itemBig)
	override val rankMap
		get() = rankMapOf(
			"grade" to rankingGrade, "level" to rankingLevel, "time" to rankingTime, "section.time" to bestSectionTime
		)

	override fun playerInit(engine:GameEngine) {
		super.playerInit(engine)
		nextSecLv = 0
		lvupFlag = true
		gmPier = 0
		grade = 0
		lastGradeTime = 0
		comboValue = 0
		lastScore = 0
		rollTime = 0
		gm300 = false
		gm500 = false
		secretGrade = 0
		bgmLv = 0
		gradeFlash = 0
		sectionScore.fill(0)
		sectionTime.fill(0)
		sectionIsNewRecord.fill(false)
		sectionsDone = 0
		medalAC = 0
		decTemp = 0

		rankingRank = -1
		rankingGrade.fill(0)
		rankingLevel.fill(0)
		rankingTime.fill(-1)
		bestSectionScore.fill(0)
		bestSectionTime.fill(DEFAULT_SECTION_TIME)

		engine.twistEnable = false
		engine.b2bEnable = true
		engine.splitB2B = true
		engine.comboType = GameEngine.COMBO_TYPE_DOUBLE
		engine.bigHalf = false
		engine.bigMove = false
		engine.staffrollNoDeath = true

		engine.speed.are = 30
		engine.speed.areLine = 30
		engine.speed.lineDelay = 41
		engine.speed.lockDelay = 30
		engine.speed.das = 15

		version = if(owner.replayMode) {
			owner.replayProp.getProperty("grademania1.version", 0)
		} else {
			CURRENT_VERSION
		}

		owner.bgMan.bg = startLevel
		setSpeed(engine)
	}

	/*	override fun loadSetting(prop:CustomProperties, ruleName:String, playerID:Int) {
			startLevel = prop.getProperty("grademania1.startLevel", 0)
			alwaysGhost = prop.getProperty("grademania1.alwaysGhost", true)
			always20g = prop.getProperty("grademania1.always20g", false)
			secAlert = prop.getProperty("grademania1.secAlert", true)
			showST = prop.getProperty("grademania1.showST", true)
			big = prop.getProperty("grademania1.big", false)
		}

		override fun saveSetting(prop:CustomProperties, ruleName:String, playerID:Int) {
			prop.setProperty("grademania1.startLevel", startLevel)
			prop.setProperty("grademania1.alwaysGhost", alwaysGhost)
			prop.setProperty("grademania1.always20g", always20g)
			prop.setProperty("grademania1.secAlert", secAlert)
			prop.setProperty("grademania1.showST", showST)
			prop.setProperty("grademania1.big", big)
		}*/

	/** Update falling speed
	 * @param engine GameEngine
	 */
	private fun setSpeed(engine:GameEngine) {
		engine.speed.gravity = if(always20g) -1
		else tableGravityValue[(tableGravityChangeLevel.indexOfFirst {engine.statistics.level<it})
			.let {if(it<0) tableGravityChangeLevel.size-1 else it}]

		val section = minOf(
			tableARE.size-1, if(engine.statistics.level<300) 0 else if(engine.statistics.level<500) 1 else 2
		)
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

	/** Section Time更新処理
	 * @param sectionNumber Section number
	 */
	private fun stNewRecordCheck(sectionNumber:Int) {
		if(!owner.replayMode&&
			(sectionTime[sectionNumber]<bestSectionTime[sectionNumber]||bestSectionTime[sectionNumber]<0))
			sectionIsNewRecord[sectionNumber] = true
	}

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

		engine.statistics.level = startLevel*100
		setSpeed(engine)
		owner.bgMan.bg = minOf(9, startLevel)
		super.onSettingChanged(engine)
	}

	override fun onReady(engine:GameEngine):Boolean {
		if(engine.statc[0]==0) {
			isShowBestSectionTime = false
			sectionsDone = 0
			bgmLv = if(engine.statistics.level<500) 0 else 1
			owner.musMan.bgm = if(engine.statistics.level<500) BGM.GrandM(0) else BGM.GrandM(1)
		}
		return super.onReady(engine)
	}
	/* Render the settings screen */
	/*override fun renderSetting(engine:GameEngine, playerID:Int) {
		drawMenu(engine, playerID, receiver, 0, COLOR.BLUE, 0, "Level" to (startLevel*100), "FULL GHOST" to alwaysGhost,
			"FULL 20G" to always20g, "LVSTOPSE" to secAlert, "SHOW STIME" to showST, "BIG" to big)
	}*/

	/* Called at game start */
	override fun startGame(engine:GameEngine) {
		engine.statistics.level = startLevel*100

		decTemp = 0
		nextSecLv = engine.statistics.level+100
		if(engine.statistics.level<0) nextSecLv = 100
		if(engine.statistics.level>=900) nextSecLv = 999

		owner.bgMan.bg = engine.statistics.level/100

		engine.big = big

		setSpeed(engine)
	}

	/* Render score */
	override fun renderLast(engine:GameEngine) {
		receiver.drawScoreFont(engine, 0, 0, name, COLOR.CYAN)

		receiver.drawScoreFont(engine, -1, -4*2, "DECORATION", scale = .5f)
		receiver.drawScoreBadges(engine, 0, -3, 100, decoration)
		receiver.drawScoreBadges(engine, 5, -4, 100, decTemp)
		if(engine.stat==GameEngine.Status.SETTING||engine.stat==GameEngine.Status.RESULT&&!owner.replayMode) {
			if(!owner.replayMode&&startLevel==0&&!big&&!always20g&&engine.ai==null)
				if(!isShowBestSectionTime) {
					// Rankings
					receiver.drawScoreFont(engine, 0, 2, "GRADE TIME LEVEL", COLOR.BLUE)

					for(i in 0..<RANKING_MAX) {
						receiver.drawScoreGrade(
							engine, 0, 3+i, "%2d".format(i+1), if(rankingRank==i) COLOR.RAINBOW else COLOR.YELLOW
						)
						var gc = if(i==rankingRank)
							if(engine.playerID%2==0) COLOR.YELLOW else COLOR.ORANGE
						else COLOR.WHITE
						if(rankingGrade[i]>=18) {
							var gmP = 0
							for(l in 1..<tablePier21GradeTime.size)
								if(rankingTime[i]<tablePier21GradeTime[l]) gmP = l
							gc = tablePier21GradeColor[gmP]
						}
						if(rankingGrade[i]>=0&&rankingGrade[i]<tableGradeName.size)
							receiver.drawScoreGrade(engine, 2, 3+i, tableGradeName[rankingGrade[i]], gc)
						receiver.drawScoreNum(engine, 5, 3+i, rankingTime[i].toTimeStr, i==rankingRank)
						receiver.drawScoreNum(engine, 12, 3+i, "%03d".format(rankingLevel[i]), i==rankingRank)
					}

					receiver.drawScoreFont(engine, 0, 17, "F:VIEW SECTION TIME", COLOR.GREEN)
				} else {
					// Section Time
					receiver.drawScoreFont(engine, 0, 2, "SECTION TIME SCORE", COLOR.BLUE)

					var totalTime = 0
					for(i in 0..<SECTION_MAX) {
						val temp = minOf(i*100, 999)
						val temp2 = minOf((i+1)*100-1, 999)

						val strSectionTime = String.format(
							"%3d-%3d %s %d", temp, temp2, bestSectionTime[i].toTimeStr,
							bestSectionScore[i]
						)

						receiver.drawScoreNum(engine, 0, 3+i, strSectionTime, sectionIsNewRecord[i])
						totalTime += bestSectionTime[i]
					}

					receiver.drawScoreFont(engine, 0, 14, "TOTAL", color = COLOR.BLUE)
					receiver.drawScoreNum(engine, 0, 15, totalTime.toTimeStr, 2f)
					receiver.drawScoreFont(
						engine, if(receiver.nextDisplayType==2)
							0 else 12, if(receiver.nextDisplayType==2) 18 else 14, "AVERAGE", color = COLOR.BLUE
					)
					receiver.drawScoreNum(
						engine, if(receiver.nextDisplayType==2)
							0 else 12, if(receiver.nextDisplayType==2) 19 else 15, (totalTime/SECTION_MAX).toTimeStr, scale = 2f
					)

					receiver.drawScoreFont(engine, 0, 17, "F:VIEW RANKING", color = COLOR.GREEN)
				}
		} else {
			val g20 = engine.speed.gravity<0&&(engine.statistics.time/2%2==0||engine.ending>0)
			receiver.drawScoreFont(
				engine, 0, 2, "GRADE",
				color = if(g20 or (gradeFlash>0&&gradeFlash%4==0)) if(gm300) COLOR.YELLOW else COLOR.CYAN else COLOR.BLUE
			)
			receiver.drawScoreGrade(
				engine, 5, 2, tableGradeName[grade], color = if(gradeFlash>0&&gradeFlash%(if(grade>=18) 2 else 4)==0)
					(if(grade>=18) tablePier21GradeColor[gmPier] else COLOR.YELLOW) else COLOR.WHITE,
				scale = 2f
			)

			if(grade<17) {
				receiver.drawScoreNano(
					engine, 0, 3*2+1, "NEXT AT",
					if(g20 or (gradeFlash>0&&gradeFlash%4==0)) if(gm300) COLOR.YELLOW else COLOR.CYAN else COLOR.BLUE,
					.5f
				)
				receiver.drawScoreNum(engine, 0, 4, "${tableGradeScore[grade]}")
				val prev = tableGradeScore.getOrElse(grade-1) {0}
				val nextNorm = tableGradeScore[grade]-prev
				receiver.drawScoreSpeed(engine, 0, 3, (engine.statistics.score-prev)*1f/nextNorm, 5f)
			}

			// 段位上昇時のフラッシュ
			if(gradeFlash>0) gradeFlash--

			receiver.drawScoreFont(
				engine, 0, 5, "Score", color = if(g20) if(gm300&&gm500) COLOR.YELLOW else COLOR.CYAN else COLOR.BLUE
			)
			receiver.drawScoreNum(engine, 5, 5, "+$lastScore", if(g20) COLOR.YELLOW else COLOR.WHITE)
			receiver.drawScoreNum(
				engine, 0, 6, "$scDisp", if(g20) COLOR.YELLOW else COLOR.WHITE,
				2f
			)

			receiver.drawScoreFont(
				engine, 0, 9, "Level", if(g20) if(gm300&&gm500) COLOR.YELLOW else COLOR.CYAN else COLOR.BLUE
			)
			receiver.drawScoreNum(engine, 1, 10, "%3d".format(maxOf(engine.statistics.level, 0)), g20)
			receiver.drawScoreSpeed(
				engine, 0, 11, if(g20) 40 else floor(ln(engine.speed.gravity.toDouble())).toInt()*4,
				4
			)
			receiver.drawScoreNum(engine, 1, 12, "%3d".format(nextSecLv), g20)

			receiver.drawScoreFont(
				engine, 0, 14, "Time", if(g20) if(gm500) COLOR.YELLOW else COLOR.CYAN else COLOR.BLUE
			)
			if(engine.ending!=2||rollTime/30%2==0)
				receiver.drawScoreNum(engine, 0, 15, engine.statistics.time.toTimeStr, g20, 2f)

			if(engine.gameActive&&engine.ending==2) {
				var time = ROLLTIMELIMIT-rollTime
				if(time<0) time = 0
				receiver.drawScoreFont(engine, 0, 17, "ROLL TIME", COLOR.BLUE)
				receiver.drawScoreNum(engine, 0, 18, time.toTimeStr, time>0&&time<10*60, 2f)
			}

			// Section Time
			if(showST&&sectionTime.isNotEmpty()) {
				val x = receiver.nextDisplayType==2
				receiver.drawScoreFont(engine, if(x) 8 else 10, 2, "SECTION TIME", COLOR.BLUE)

				for(i in sectionTime.indices)
					if(sectionTime[i]>0) {
						var temp = i*100
						if(temp>999) temp = 999

						val section = engine.statistics.level/100
						var strSeparator = "-"
						if(i==section&&engine.ending==0) strSeparator = "+"
						val strSectionTime = StringBuilder()
						for(l in 0..<i)
							strSectionTime.append("\n")
						strSectionTime.append(
							"%3d%s%s %d\n".format(temp, strSeparator, sectionTime[i].toTimeStr, sectionScore[i])
						)
						receiver.drawScoreNum(
							engine, if(x) 9 else 10, 3, "$strSectionTime", sectionIsNewRecord[i], if(x) .75f else 1f
						)
					}

				receiver.drawScoreFont(engine, if(x) 8 else 12, if(x) 11 else 14, "AVERAGE", COLOR.BLUE)
				receiver.drawScoreNum(
					engine,
					if(x) 8 else 12,
					if(x) 12 else 15,
					(engine.statistics.time/(sectionsDone+(engine.ending==0).toInt())).toTimeStr,
					scale = 2f
				)
			}
			// medal
			receiver.drawScoreMedal(engine, 0, 20, "AC", medalAC)
		}
	}

	/* 移動中の処理 */
	override fun onMove(engine:GameEngine):Boolean {
		// 新規ピース出現時
		if(engine.ending==0&&engine.statc[0]==0&&!engine.holdDisable&&!lvupFlag)
			levelUp(engine, (engine.statistics.level<nextSecLv-1).toInt())

		if(engine.ending==0&&engine.statc[0]>0&&(version>=1||!engine.holdDisable)) lvupFlag = false

		return false
	}

	/* ARE中の処理 */
	override fun onARE(engine:GameEngine):Boolean {
		// 最後の frame
		if(engine.ending==0&&engine.statc[0]>=engine.statc[1]-1&&!lvupFlag) {
			levelUp(engine, (engine.statistics.level<nextSecLv-1).toInt())
			lvupFlag = true
		}

		return false
	}

	/** levelが上がったときの共通処理 */
	private fun levelUp(engine:GameEngine, lu:Int = 0) {

		engine.statistics.level+=lu
		engine.meterValue = engine.statistics.level%100/99f
		engine.meterColor = GameEngine.METER_COLOR_LEVEL

		// 速度変更
		setSpeed(engine)

		// LV100到達でghost を消す
		if(engine.statistics.level>=100&&!alwaysGhost) engine.ghost = false

		if(lu<=0) return
		if(engine.statistics.level==nextSecLv-1&&secAlert) engine.playSE("levelstop")
		// BGM fadeout
		if(bgmLv==0&&engine.statistics.level>=490) owner.musMan.fadeSW = true
	}

	override fun blockBreak(engine:GameEngine, blk:Map<Int, Map<Int, Block>>):Boolean {
		engine.owner.receiver.efxFG.addAll(
			Mapper(engine, engine.owner.receiver, blk, BlockParticle.Type.TGM, engine.combo>=0, 4f).particles
		)
		return true
	}
	/* Calculate score */
	override fun calcScore(engine:GameEngine, ev:ScoreEvent):Int {
		if(engine.ending!=0) return 0
		// Combo
		val li = ev.lines
		comboValue = if(li==0) 1
		else maxOf(1, comboValue+2*li-2)

		if(li>=1) {
			// Calculate score
			val bravo = if(engine.field.isEmpty) {
				decTemp += li*25
				if(li==3) decTemp += 25
				if(li==4) decTemp += 150
				if(medalAC<3) {
					decTemp += 3+medalAC*4// 3 10 21
					engine.playSE("medal${++medalAC}")
				}
				4
			} else 1

			lastScore =
				(((engine.statistics.level+li)/(if(ev.b2b>0) 4 else 3)+engine.softdropFall+engine.harddropFall+engine.manualLock.toInt())
					*li*comboValue*bravo)
			sectionScore[sectionsDone] += lastScore
			engine.statistics.scoreLine += lastScore

			// 段位上昇
			while(grade<17&&engine.statistics.score>=tableGradeScore[grade]) {
				engine.playSE("grade${grade*4/17}")
				grade++
				when {
					grade in 0..4 -> engine.playSE("grade0")
					grade in 5..8 -> engine.playSE("grade1")
					grade in 9..12 -> engine.playSE("grade2")
					grade in 13..16 -> engine.playSE("grade3")
					grade>=17 -> engine.playSE("grade4")
				}

				decTemp++
				gradeFlash = 180
				lastGradeTime = engine.statistics.time
			}

			// Level up
			levelUp(engine, li)
			if(engine.statistics.level>=999) {
				// Ending
				engine.statistics.level = 999
				engine.timerActive = false
				lastGradeTime = engine.statistics.time

				sectionsDone++
				stNewRecordCheck(sectionsDone-1)

				if(engine.statistics.time<=GM_999_TIME_REQUIRE&&engine.statistics.score>=tableGradeScore[17]
					&&gm300&&gm500
				) {
					engine.playSE("applause5")
					engine.playSE("endingstart")
					engine.playSE("grade4")

					gmPier = tablePier21GradeTime.count {it>=engine.statistics.time}-1
					grade = 18+(gmPier>3).toInt()
					gradeFlash = ROLLTIMELIMIT

					owner.musMan.fadeSW = false
					owner.musMan.bgm = BGM.Ending(0)

					engine.ending = 2
				} else {
					engine.playSE("applause4")
					engine.gameEnded()
					engine.ending = 1
				}
			} else if(engine.statistics.level>=nextSecLv) {
				// Next Section
				owner.bgMan.nextBg = nextSecLv/100

				if(nextSecLv==300&&grade>=GM_300_GRADE_REQUIRE&&engine.statistics.time<=GM_300_TIME_REQUIRE) {
					gm300 = true
					engine.playSE("cool")
				} else if(nextSecLv==500&&grade>=GM_500_GRADE_REQUIRE&&engine.statistics.time<=GM_500_TIME_REQUIRE) {
					gm500 = true
					engine.playSE("cool")
				}
				if(nextSecLv==300||nextSecLv==500) engine.playSE("levelup_section")
				engine.playSE("levelup")

				sectionsDone++
				stNewRecordCheck(sectionsDone-1)

				if(bgmLv==0&&nextSecLv==500) {
					bgmLv++
					owner.musMan.fadeSW = false
					owner.musMan.bgm = BGM.GrandM(1)
				}

				nextSecLv += 100
				if(nextSecLv>999) nextSecLv = 999
			} else if(engine.statistics.level==nextSecLv-1&&secAlert) engine.playSE("levelstop")

			return lastScore
		}
		return 0
	}

	/* 各 frame の終わりの処理 */
	override fun onLast(engine:GameEngine) {
		super.onLast(engine)

		// Section Time増加
		if(engine.timerActive&&engine.ending==0) {
			val section = engine.statistics.level/100

			if(section>=0&&section<sectionTime.size) sectionTime[section] = engine.statistics.time-sectionTime.take(section).sum()

			if(engine.statistics.level==nextSecLv-1)
				engine.meterColor = if(engine.meterColor==-0x1)
					-0x10000
				else
					-0x1
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
				engine.gameEnded()
				engine.resetStatc()
				engine.stat = GameEngine.Status.EXCELLENT
			}
		}
	}

	/* Called at game over */
	override fun onGameOver(engine:GameEngine):Boolean {
		if(engine.statc[0]==0) {
			secretGrade = engine.field.secretGrade
			val time = engine.statistics.time
			if(grade>=18)
				for(i in 1..<tablePier21GradeTime.size)
					if(time<tablePier21GradeTime[i]) decTemp++

			if(time<6000) decTemp -= 3
			else {
				decTemp++
				if(time%3600<=60||time%3600>=3540) decTemp++
			}
			decoration += decTemp+secretGrade
		}
		return false
	}

	override fun renderExcellent(engine:GameEngine) {
		if(grade==18) {
			val col = if(engine.statc[0]%4<2) COLOR.WHITE else tablePier21GradeColor[gmPier]
			receiver.drawMenuFont(engine, .5f, 8f, "YOU ARE A", COLOR.WHITE, 1f)
			receiver.drawMenuFont(engine, 1.25f, 9f, "GRAND", col, 1.5f)
			receiver.drawMenuFont(engine, .5f, 10.5f, "MASTER", col, 1.5f)
		} else if(grade==17) {
			val col = when {
				engine.statc[0]%4==0 -> COLOR.CYAN
				engine.statc[0]%2==0 -> COLOR.WHITE
				else -> COLOR.BLUE
			}
			receiver.drawMenuFont(engine, 3.5f, 8.5f, "BUT...", COLOR.WHITE, 1f)
			receiver.drawMenuFont(engine, .5f, 10f, "CHALLENGE", COLOR.BLUE, 1f)
			receiver.drawMenuFont(engine, -.5f, 11f, "MORE FASTER", col, 1f)
			receiver.drawMenuFont(engine, .5f, 12f, "NEXT TIME", COLOR.WHITE, 1f)
		}
	}

	/* 結果画面 */
	override fun renderResult(engine:GameEngine) {
		receiver.drawMenuFont(engine, 0, 0, "\u0090\u0093 PAGE${engine.statc[1]+1}/3", COLOR.RED)

		when(engine.statc[1]) {
			0 -> {
				receiver.drawMenuFont(engine, 0, 2, "GRADE", COLOR.BLUE)
				var gc = COLOR.WHITE
				if(grade>=18) {
					gc = tablePier21GradeColor[gmPier]
					receiver.drawMenuFont(engine, 0, 3, tablePier21GradeName[gmPier], gc)
				}
				receiver.drawMenuGrade(engine, 6, 2, tableGradeName[grade], gc, 2f)
				drawResultStats(
					engine, receiver, 4, COLOR.BLUE, Statistic.SCORE, Statistic.LINES, Statistic.LEVEL_MANIA, Statistic.TIME
				)
				drawResultRank(engine, receiver, 13, COLOR.BLUE, rankingRank)
				if(secretGrade>4)
					drawResult(
						engine, receiver, 15, COLOR.BLUE, "S. GRADE",
						"%10s".format(tableGradeName[secretGrade-1])
					)
			}
			1 -> {
				receiver.drawMenuFont(engine, 0, 2, "SECTION", COLOR.BLUE)

				for(i in sectionTime.indices)
					if(sectionTime[i]>0)
						receiver.drawMenuNum(engine, 2, 3+i, sectionTime[i].toTimeStr, sectionIsNewRecord[i])

				if(sectionAvgTime>0) {
					receiver.drawMenuFont(engine, 0, 14, "AVERAGE", COLOR.BLUE)
					receiver.drawMenuNum(engine, 0, 15, sectionAvgTime.toTimeStr, 1.7f)
				}
			}
			2 -> {
				receiver.drawMenuFont(engine, 0, 2, "MEDAL", COLOR.BLUE)
				receiver.drawMenuMedal(engine, 8, 2, "AC", medalAC)
				drawResultStats(engine, receiver, 4, COLOR.BLUE, Statistic.LPM, Statistic.SPM, Statistic.PIECE, Statistic.PPS)

				drawResult(engine, receiver, 15, COLOR.BLUE, "DECORATION", "%d".format(decTemp))
			}
		}
	}

	/* 結果画面の処理 */
	override fun onResult(engine:GameEngine):Boolean {
		owner.musMan.fadeSW = false
		owner.musMan.bgm = when(engine.ending) {
			0 -> BGM.Result(0)
			2 -> BGM.Result(3)
			else -> BGM.Result(2)
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

		return false
	}

	/* リプレイ保存 */
	override fun saveReplay(engine:GameEngine, prop:CustomProperties):Boolean {
		owner.replayProp.setProperty("result.grade.name", tableGradeName[grade])
		owner.replayProp.setProperty("result.grade.number", grade)
		owner.replayProp.setProperty("grademania1.version", version)

		owner.statsProp.setProperty("decoration", decoration)
		// Update rankings
		if(!owner.replayMode&&startLevel==0&&!always20g&&!big&&engine.ai==null) {
			updateRanking(grade, engine.statistics.level, lastGradeTime)
			if(sectionAnyNewRecord) updateBestSectionTime()

			if(rankingRank!=-1||sectionAnyNewRecord) return true
		}
		return false
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
		for(i in 0..<RANKING_MAX)
			if(gr>rankingGrade[i]) return i
			else if(gr==rankingGrade[i]&&lv>rankingLevel[i]) return i
			else if(gr==rankingGrade[i]&&lv==rankingLevel[i]&&time<rankingTime[i]) return i

		return -1
	}

	/** Update best section time records */
	private fun updateBestSectionTime() {
		for(i in 0..<SECTION_MAX)
			if(sectionIsNewRecord[i]) {
				bestSectionTime[i] = sectionTime[i]
				bestSectionScore[i] = sectionScore[i]
			}
	}

	companion object {
		/** Current version */
		private const val CURRENT_VERSION = 1

		/** 落下速度 table */
		private val tableGravityValue = listOf(
			4, 6, 8, 10, 12, 16, 32, 48, 64, 80, 96, 112, 128, 144, 4, 32, 64, 96, 128, 160,
			192, 224, 256, 512, 768, 1024, 1280, 1024, 768, -1
		)

		/** 落下速度が変わる level */
		private val tableGravityChangeLevel = listOf(
			30, 35, 40, 50, 60, 70, 80, 90, 100, 120, 140, 160, 170, 200, 220, 230,
			233, 236, 239, 243, 247, 251, 300, 330, 360, 400, 420, 450, 500, 10000
		)

		/** ARE table */
		private val tableARE = listOf(30, 27, 25)

		/** Line clear times table */
		private val tableLineDelay = listOf(41, 40, 25)

		/** 固定 times table */
		private val tableLockDelay = listOf(30, 30, 30)

		/** DAS table */
		private val tableDAS = listOf(16, 15, 14)

		/** 段位上昇に必要なScore */
		private val tableGradeScore = listOf(
			500, 1000, 1500, 2500, 3500, 5000, 8000, 12000, // 8～1
			16000, 22000, 30000, 40000, 52000, 66000, 82000, 100000, 123456, // S1～S9
			131072 // GM
		)

		/** 段位のName */
		private val tableGradeName = listOf(
			"9", "8", "7", "6", "5", "4", "3", "2", "1", // 0～8
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
		private val tablePier21GradeTime = listOf(GM_999_TIME_REQUIRE, 44800, 40000, 38166, 36333, 34500, 33000)

		/** GM */
		private val tablePier21GradeName = listOf("CARBON", "STEEL", "BRONZE", "SILVER", "GOLD", "PLATINUM", "DIAMOND")

		private val tablePier21GradeColor = listOf(
			COLOR.PURPLE, COLOR.BLUE, COLOR.RED, COLOR.WHITE, COLOR.YELLOW, COLOR.CYAN,
			COLOR.GREEN
		)
		/** LV999 roll time */
		/** Number of entries in rankings */
		private const val RANKING_MAX = 13

		/** Number of sections */
		private const val SECTION_MAX = 10

		/** Default section time */
		private const val DEFAULT_SECTION_TIME = 5400
	}

}
