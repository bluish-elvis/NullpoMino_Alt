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
import mu.nu.nullpo.game.subsystem.mode.menu.BooleanMenuItem
import mu.nu.nullpo.game.subsystem.mode.menu.DelegateMenuItem
import mu.nu.nullpo.game.subsystem.mode.menu.LevelGrandMenuItem
import mu.nu.nullpo.game.subsystem.mode.menu.MenuList
import mu.nu.nullpo.gui.common.BaseFont
import mu.nu.nullpo.util.CustomProperties
import mu.nu.nullpo.util.GeneralUtil.toInt
import mu.nu.nullpo.util.GeneralUtil.toTimeStr

/** GRADE MANIA 2 Mode */
class GrandM2:AbstractGrand() {
	/** 画面に表示されている実際の段位 */
	private var grade = 0

	/** 内部段位 */
	private var gradeInternal = 0

	/** 段位 point 100以上で昇段 */
	private var gradePoint = 0

	/** 段位 pointが1つ減る time */
	private var gradeDecay = 0

	/** 最後に段位が上がった time */
	private var lastGradeTime = 0

	/** Combo bonus */
	private var comboValue = 0

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

	/** Section 内で4-line clearした count */
	private var sectionQuads = MutableList(sectionMax) {0}

	/** 消えRoll flag１ (Section Time) */
	private var mRollSTime = false

	/** 消えRoll flag２ (4-line clear) */
	private var mRollQuads = false

	/** 消えRoll started flag */
	private var mRollFlag = false

	/** 消えRoll 中に消したline count */
	private var mRollLines = 0

	/** Section Time記録表示中ならtrue */
	private var isShowBestSectionTime = false

	private val itemLevel = LevelGrandMenuItem(COLOR.BLUE)
	/** Level at start */
	private var startLevel:Int by DelegateMenuItem(itemLevel)

	private val item20g = BooleanMenuItem("always20g", "20G MODE", COLOR.BLUE, false)
	/** When true, always 20G */
	private var always20g:Boolean by DelegateMenuItem(item20g)

	private val itemBig = BooleanMenuItem("big", "BIG", COLOR.BLUE, false)
	/** BigMode */
	private var big:Boolean by DelegateMenuItem(itemBig)

	/** Version */
	private var version = 0

	/** Current round's ranking position */
	private var rankingRank = 0

	/** Rankings' 段位 */
	private val rankingGrade = MutableList(rankingMax) {0}

	/** Rankings' level */
	private val rankingLevel = MutableList(rankingMax) {0}

	/** Rankings' times */
	private val rankingTime = MutableList(rankingMax) {-1}

	/** Rankings' Roll completely cleared flag */
	private val rankingRollClear = MutableList(rankingMax) {0}

	/** Section Time記録 */
	private val bestSectionTime = MutableList(sectionMax) {DEFAULT_SECTION_TIME}
	private val bestSectionQuads = MutableList(sectionMax) {0}

	/* Mode name */
	override val name = "Grand Mania"
	override val gameIntensity = 1

	/* Initialization */
	override val menu = MenuList("grademania2", itemGhost, itemAlert, itemST, itemLevel, item20g, itemBig)
	override val propRank
		get() = rankMapOf(
			"grade" to rankingGrade, "level" to rankingLevel, "time" to rankingTime, "clear" to rankingRollClear,
			"section.time" to bestSectionTime, "section.quads" to bestSectionQuads
		)
	/* Initialization */
	override fun playerInit(engine:GameEngine) {
		super.playerInit(engine)
		grade = 0
		gradeInternal = 0
		gradePoint = 0
		gradeDecay = 0
		lastGradeTime = 0
		rollTime = 0
		rollClear = 0
		rollStarted = false
		secretGrade = 0
		gradeFlash = 0
		sectionQuads.fill(0)
		mRollSTime = true
		mRollQuads = true
		mRollFlag = false
		mRollLines = 0
		medals.reset()
		rankingRank = -1
		rankingGrade.fill(0)
		rankingLevel.fill(0)
		rankingTime.fill(-1)
		rankingRollClear.fill(0)
		bestSectionTime.fill(DEFAULT_SECTION_TIME)
		bestSectionQuads.fill(0)

		engine.twistEnable = false
		engine.b2bEnable = true
		engine.splitB2B = true
		engine.comboType = GameEngine.COMBO_TYPE_DOUBLE
		engine.bigHalf = true
		engine.bigMove = true
		engine.staffrollEnable = true
		engine.staffrollNoDeath = false

		if(!owner.replayMode) {
			version = CURRENT_VERSION
		} else {
			for(i in 0..<sectionMax)
				bestSectionTime[i] = DEFAULT_SECTION_TIME
			version = owner.replayProp.getProperty("grademania2.version", 0)
		}

		owner.bgMan.bg = startLevel+10
		setSpeed(engine)
	}

	private fun calcBgmLv(lv:Int) = tableBGMChange.count {lv>=it}.let {
		it+maxOf(0, it-3)+(it>=2&&mRollSTime&&mRollQuads).toInt()
	}

	/** Update falling speed
	 * @param engine GameEngine
	 */
	override fun setSpeed(engine:GameEngine) {
		engine.speed.gravity = if(always20g||engine.statistics.time>=54000) -1
		else tableGravityValue[(tableGravityChangeLevel.indexOfFirst {engine.statistics.level<it}).let {if(it<0) tableGravityChangeLevel.lastIndex else it}]

		val section = minOf(engine.statistics.level/100, tableARE.lastIndex)
		engine.speed.das = tableDAS[section]

		if(engine.statistics.time>=54000) {
			engine.speed.are = 3
			engine.speed.areLine = 6
			engine.speed.lineDelay = 6
			engine.speed.lockDelay = 19
		} else {
			engine.speed.are = tableARE[section]
			engine.speed.areLine = tableARELine[section]
			engine.speed.lineDelay = tableLineDelay[section]
			engine.speed.lockDelay = tableLockDelay[section]
		}
	}

	/** 消えRoll 条件を満たしているか check
	 * @param levelb 上がる前の level
	 */
	private fun mRollCheck(levelb:Int):Boolean {
		mRollSTime = sectionTime.take(1+levelb/100).mapIndexed {i, t -> t<=mRollTime(i)}.all {it}
			&&sectionTime.sum()<=M_ROLL_TIME_REQUIRE
		mRollQuads = sectionQuads.take(1+levelb/100).mapIndexed {i, t ->
			t>=when(i) {
				in 0..4 -> 2
				in 5..8 -> 1
				else -> 0
			}
		}.all {it}
		return mRollSTime&&mRollQuads
	}

	private fun mRollTime(section:Int):Int = when {
		section==6 -> (sectionTime.take(5).sum())/5+400
		section>6 -> sectionTime[section-1]+(640-section*40)
		else -> 6500-section*500
	}

	/** ST medal check
	 * @param engine GameEngine
	 * @param section Section number
	 */
	private fun stMedalCheck(engine:GameEngine, section:Int) =
		stMedalCheck(engine, section, sectionTime[section], bestSectionTime[section])

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
		owner.bgMan.bg = 10+startLevel
		engine.statistics.level = startLevel*100
		setSpeed(engine)
		super.onSettingChanged(engine)
	}
	/* Render the settings screen */
	/*override fun renderSetting(engine:GameEngine, playerID:Int) {
		drawMenu(engine, playerID, receiver, 0, COLOR.BLUE, 0, "Level" to (startLevel*100),
			"FULL GHOST" to alwaysGhost, "FULL 20G" to always20g, "LVSTOPSE" to secAlert, "SHOW STIME" to showST,
			"BIG" to big)
	}*/
	override fun onReady(engine:GameEngine):Boolean {
		if(engine.statc[0]==0) {
			isShowBestSectionTime = false
			owner.musMan.fadeSW = true
		}
		return super.onReady(engine)
	}
	/* Called at game start */
	override fun startGame(engine:GameEngine) {
		val lv = startLevel*100
		engine.statistics.level = lv

		decTemp = 0
		nextSecLv = (lv+100).coerceIn(100, 999)

		owner.bgMan.bg = 10+startLevel/100

		engine.big = big

		setSpeed(engine)
		owner.musMan.bgm = BGM.GrandA(calcBgmLv(lv))
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
			if(!owner.replayMode&&startLevel==0&&!big&&!always20g&&engine.ai==null)
				if(!isShowBestSectionTime) {
					// Rankings
					receiver.drawScoreFont(engine, 0, 2, "GRADE TIME LEVEL", COLOR.BLUE)

					for(i in 0..<rankingMax) {
						receiver.drawScoreGrade(
							engine, 0, 3+i, "%2d".format(i+1), if(rankingRank==i) COLOR.RAINBOW else COLOR.YELLOW
						)
						if(rankingGrade[i]>=0&&rankingGrade[i]<tableGradeName.size)
							receiver.drawScoreGrade(
								engine, 2, 3+i, tableGradeName[rankingGrade[i]],
								if(rankingRollClear[i]==1||rankingRollClear[i]==3) COLOR.GREEN
								else if(rankingRollClear[i]==2||rankingRollClear[i]==4) COLOR.ORANGE
								else COLOR.WHITE
							)
						receiver.drawScoreNum(engine, 5, 3+i, rankingTime[i].toTimeStr, i==rankingRank)
						receiver.drawScoreNum(engine, 12, 3+i, "%03d".format(rankingLevel[i]), i==rankingRank)
					}

					receiver.drawScoreFont(engine, 0, 17, "F:VIEW SECTION TIME", COLOR.GREEN)
				} else {
					// Section Time
					receiver.drawScoreFont(engine, 0, 2, "SECTION TIME QUADS", COLOR.BLUE)

					val totalTime = (0..<sectionMax).fold(0) {tt, i ->
						val slv = minOf(i*100, 999)
						receiver.drawScoreNum(
							engine,
							0,
							3+i,
							"%3d-%3d %s %d".format(slv, slv+99, bestSectionTime[i].toTimeStr, bestSectionQuads[i]),
							sectionIsNewRecord[i]
						)
						tt+bestSectionTime[i]
					}

					receiver.drawScoreFont(engine, 0, 14, "TOTAL", COLOR.BLUE)
					receiver.drawScoreNum(engine, 0, 15, totalTime.toTimeStr, 2f)
					receiver.drawScoreFont(
						engine, if(receiver.nextDisplayType==2) 0 else 12, if(receiver.nextDisplayType==2) 18 else 14,
						"AVERAGE", COLOR.BLUE
					)
					receiver.drawScoreNum(
						engine, if(receiver.nextDisplayType==2) 0 else 12, if(receiver.nextDisplayType==2) 19 else 15,
						(totalTime/sectionMax).toTimeStr, 2f
					)

					receiver.drawScoreFont(engine, 0, 17, "F:VIEW RANKING", COLOR.GREEN)
				}
		} else {
			val g20 = engine.speed.gravity<0&&engine.statistics.time%3==0
			// 段位
			if(grade>=0&&grade<tableGradeName.size)
				receiver.drawScoreGrade(engine, 1, 2, tableGradeName[grade], gradeFlash>0&&gradeFlash%4==0||g20, 2f)
			if(grade<17) {
				receiver.drawScoreSpeed(engine, 0, 4, (gradePoint-gradeDecay*1f/tableGradeDecayRate[gradeInternal])/100f, 5f)
				receiver.drawScoreNum(
					engine, 1, 5, "%02.1f%%".format(gradePoint-gradeDecay*1f/tableGradeDecayRate[gradeInternal]),
					if(g20) COLOR.YELLOW else if(mRollQuads&&mRollSTime) COLOR.CYAN else COLOR.BLUE
				)
			}
			if(gradeInternal>=0&&gradeInternal<tableDetailGradeName.size)
				receiver.drawScoreGrade(engine, 4, 2, tableDetailGradeName[gradeInternal], gradeFlash>0&&gradeFlash%4==0||g20)

			// Score
			receiver.drawScoreFont(engine, 0, 6, "Score", if(g20&&mRollQuads) COLOR.CYAN else COLOR.BLUE)
			receiver.drawScoreNum(engine, 5, 6, "+$lastScore", g20)
			receiver.drawScoreNum(engine, 0, 7, "$scDisp", g20, 2f)

			// level
			receiver.drawScoreFont(engine, 0, 9, "Level", if(g20&&mRollSTime&&mRollQuads) COLOR.CYAN else COLOR.BLUE)
			receiver.drawScoreNum(engine, 1, 10, "%3d".format(maxOf(engine.statistics.level, 0)), g20)
			receiver.drawScoreSpeed(engine, 0, 11, if(g20) 1f else engine.speed.rank, 4f)
			receiver.drawScoreNum(engine, 1, 12, "%3d".format(nextSecLv), g20)

			// Time
			receiver.drawScoreFont(engine, 0, 14, "Time", if(g20&&mRollSTime) COLOR.CYAN else COLOR.BLUE)
			if(engine.ending!=2||rollTime/20%2==0)
				receiver.drawScoreNum(engine, 0, 15, engine.statistics.time.toTimeStr, g20&&mRollSTime, 2f)

			// Roll 残り time
			if(engine.gameActive&&engine.ending==2) {
				var time = ROLLTIMELIMIT-rollTime
				if(time<0) time = 0
				receiver.drawScoreFont(engine, 0, 17, "ROLL TIME", COLOR.BLUE)
				receiver.drawScoreNum(engine, 0, 18, time.toTimeStr, time>0&&time<10*60, 2f)
			}

			// medal
			receiver.drawScoreMedal(engine, 0, 20, "AC", medalAC)
			receiver.drawScoreNum(engine, 2, 20, "%3d".format(engine.statistics.bravos))
			receiver.drawScoreMedal(engine, 5, 20, "ST", medalST)
			receiver.drawScoreNum(engine, 7, 20, medalsST.joinToString("."))
			receiver.drawScoreMedal(engine, 0, 21, "SK", medalSK)
			receiver.drawScoreNum(engine, 2, 21, "%3d".format(engine.statistics.totalQuadruple))
			receiver.drawScoreMedal(engine, 5, 21, "RE", medalRE)
			receiver.drawScoreMedal(engine, 0, 22, "RO", medalRO)
			receiver.drawScoreMedal(engine, 5, 22, "CO", medalCO)
			receiver.drawScoreNum(engine, 7, 22, "%3d".format(engine.statistics.maxCombo))

			// Section Time
			if(showST&&sectionTime.isNotEmpty()) {
				val x = receiver.nextDisplayType==2
				receiver.drawScoreFont(engine, if(x) 8 else 10, 2, "SECTION TIME", COLOR.BLUE)
				val section = engine.statistics.level/100
				sectionTime.forEachIndexed {i, it ->
					if(it>0) {
						receiver.drawScoreNum(
							engine, if(x) 9 else 10, 3+i, "%3d%s%s %d".format(
								if(i<10) i*100 else 999, if(i==section&&engine.ending==0) "+" else "-", it.toTimeStr, sectionQuads[i]
							), when {
								sectionQuads[i]>=(if(i<5) 2 else 1)&&sectionTime[i]<mRollTime(i) -> if(sectionIsNewRecord[i]) COLOR.CYAN else COLOR.GREEN
								sectionIsNewRecord[i] -> COLOR.RED; else -> COLOR.WHITE
							}, if(x) .75f else 1f
						)
					}
				}

				receiver.drawScoreFont(engine, if(x) 8 else 12, if(x) 11 else 14, "AVERAGE", COLOR.BLUE)
				receiver.drawScoreNum(
					engine, if(x) 8 else 12, if(x) 12 else 15,
					(engine.statistics.time/(sectionsDone+(engine.ending==0).toInt())).toTimeStr, 2f
				)
			}
		}
	}

	/* 移動中の処理 */
	override fun onMove(engine:GameEngine):Boolean {
		super.onMove(engine)
		// 段位 point減少
		if(engine.timerActive&&gradePoint>0&&engine.combo<=0&&engine.lockDelayNow<engine.lockDelay-1) {
			gradeDecay++

			var index = gradeInternal
			if(index>tableGradeDecayRate.size-1) index = tableGradeDecayRate.size-1

			if(gradeDecay>=tableGradeDecayRate[index]) {
				gradeDecay = 0
				gradePoint--
			}
		}

		// Endingスタート
		if(engine.ending==2&&!rollStarted) {
			rollStarted = true

			if(mRollFlag) {
				engine.blockHidden = engine.ruleOpt.lockFlash
				engine.blockHiddenAnim = false
				engine.blockOutlineType = GameEngine.BLOCK_OUTLINE_NONE
			} else {
				engine.blockHidden = 300
				engine.blockHiddenAnim = true
				engine.blockOutlineType = GameEngine.BLOCK_OUTLINE_NONE
			}

			owner.musMan.bgm = BGM.Ending(1)
		}

		return false
	}

	/** levelが上がったときの共通処理 */
	override fun levelUp(engine:GameEngine, lu:Int) {
		val levelb = engine.statistics.level
		super.levelUp(engine, lu)
		val lA = engine.statistics.level
		if(lu<=0) return
		// BGM fadeout
		if(tableBGMFadeout.any {it in levelb..lA}) owner.musMan.fadeSW = true
		// BGM切り替え
		if(tableBGMChange.any {it in levelb..lA}) {
			owner.musMan.fadeSW = false
			owner.musMan.bgm = BGM.GrandA(calcBgmLv(lA))
		}
	}

	/* Calculate score */
	override fun calcScore(engine:GameEngine, ev:ScoreEvent):Int {
		val li = ev.lines
		val pts = super.calcScore(engine, ev)
		if(li>=1&&engine.ending==0) {
			// 段位 point
			val index = minOf(gradeInternal, tableGradePoint[li-1].size-1)
			val basePoint = tableGradePoint[li-1][index]

			val indexCombo = (engine.combo+if(ev.b2b>0) 0 else -1).coerceIn(0, tableGradeComboBonus[li-1].size-1)
			val comboBonus = tableGradeComboBonus[li-1][indexCombo]

			val levelBonus = 1+engine.statistics.level/250

			val point = basePoint.toFloat()*comboBonus*levelBonus.toFloat()
			gradePoint += point.toInt()

			// 内部段位上昇
			while(gradePoint>=100) {
				gradePoint -= 100
				gradeDecay = 0
				if(gradeInternal<31) gradeInternal++

				if(tableGradeChange[grade]!=-1&&gradeInternal>=tableGradeChange[grade]) {
					engine.playSE("grade${grade*4/17}")
					grade++
					decTemp++
					gradeFlash = 180
					lastGradeTime = engine.statistics.time
				}
			}

			// 4-line clearカウント
			if(li>=4) sectionQuads[engine.statistics.level/100]++

			// Level up
			val levelb = engine.statistics.level
			val section = levelb/100
			levelUp(engine, li+(ev.b2b).coerceIn(0, 2))

			if(engine.statistics.level>=999) {
				// Ending
				engine.statistics.level = 999
				engine.timerActive = false
				engine.ending = 1
				rollClear = 1

				lastGradeTime = engine.statistics.time

				// Section Timeを記録
				sectionsDone++

				// 消えRoll check
				mRollFlag = mRollCheck(levelb)
				// ST medal
				stMedalCheck(engine, section)

				// RO medal
				roMedalCheck(engine, nextSecLv)

				// 段位M
				if(mRollFlag) {
					engine.playSE("applause4")
					grade = 18
					gradeInternal = 32
					gradeFlash = 180
					lastGradeTime = engine.statistics.time
				} else engine.playSE("applause3")
			} else if(engine.statistics.level>=nextSecLv) {
				// Next Section

				// Background切り替え
				owner.bgMan.nextBg = 10+nextSecLv/100

				engine.playSE("levelup")

				// Section Timeを記録
				sectionsDone++

				// 消えRoll check
				mRollFlag = mRollCheck(levelb)
				if(mRollFlag) engine.playSE("cool")
				// ST medal
				stMedalCheck(engine, section)

				// RO medal
				if(nextSecLv==300||nextSecLv==700) roMedalCheck(engine, nextSecLv)

				// Update level for next section
				nextSecLv += 100
				if(nextSecLv>999) nextSecLv = 999
			}

			// Calculate score
			lastScore = pts
			engine.statistics.scoreLine += lastScore
			return lastScore
		} else if(li>=1&&mRollFlag&&engine.ending==2)
		// 消えRoll 中のLine clear
			mRollLines += li
		return 0
	}

	/* 各 frame の終わりの処理 */
	override fun onLast(engine:GameEngine) {
		super.onLast(engine)
		// 段位上昇時のフラッシュ
		if(gradeFlash>0) gradeFlash--

		// 15分経過
		if(engine.statistics.time>=54000) setSpeed(engine)

		// Section Time増加
		if(engine.timerActive&&engine.ending==0)
			(engine.statistics.level/100).let {section ->
				if(section>=0&&section<sectionTime.size) sectionTime[section] = engine.statistics.time-sectionTime.take(section).sum()
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

				if(mRollFlag) {
					grade = 19
					gradeFlash = 180
					lastGradeTime = engine.statistics.time
					engine.playSE("applause5")
					engine.playSE("grade4")
					gradeInternal = 33
					rollClear = 3
					if(mRollLines>=16) {
						rollClear = 4
						grade = 20
					}
				}

				engine.blockOutlineType = GameEngine.BLOCK_OUTLINE_NORMAL

				engine.gameEnded()
				engine.resetStatc()
				engine.stat = GameEngine.Status.EXCELLENT
				if(engine.statistics.time<28800) decTemp += (28800-engine.statistics.time)/1800
			}
		} else if(engine.statistics.level==nextSecLv-1)
			engine.meterColor = if(engine.meterColor==0xFFFFFF) 0 else 0xFFFFFF
	}

	/* game over */
	override fun onGameOver(engine:GameEngine):Boolean {
		if(engine.statc[0]==0) {
			val time = engine.statistics.time
			if(time<6000)
				decTemp -= 3
			else {
				decTemp++
				if(time%3600<=60||time%3600>=3540) decTemp++
			}

			if(time>41100) decTemp -= 1+(time-41100)/1800

			// Blockの表示を元に戻す
			engine.blockOutlineType = GameEngine.BLOCK_OUTLINE_NORMAL
			// 裏段位
			secretGrade = engine.field.secretGrade
			decoration += decTemp+secretGrade
		}

		return false
	}

	/* 結果画面 */
	override fun renderResult(engine:GameEngine) {
		receiver.drawMenuFont(engine, 0, 0, "${BaseFont.UP_S}${BaseFont.DOWN_S} PAGE${engine.statc[1]+1}/3", COLOR.RED)

		when(engine.statc[1]) {
			0 -> {
				val gcolor = when(rollClear) {
					1, 3 -> COLOR.GREEN
					2, 4 -> COLOR.ORANGE
					else -> COLOR.WHITE
				}
				receiver.drawMenuFont(engine, 0, 3, "GRADE", COLOR.BLUE)
				receiver.drawMenuGrade(engine, 6, 1.66f, tableGradeName[grade], gcolor, 2f)
				receiver.drawMenuGrade(engine, 3, 2, tableDetailGradeName[gradeInternal], gcolor)

				drawResultStats(
					engine, receiver, 4, COLOR.BLUE, Statistic.SCORE, Statistic.LINES, Statistic.LEVEL_MANIA, Statistic.TIME
				)
				drawResultRank(engine, receiver, 13, COLOR.BLUE, rankingRank)
				if(secretGrade>4)
					drawResult(
						engine, receiver, 15, COLOR.BLUE, "S. GRADE",
						"%10s".format(tableSecretGradeName[secretGrade-1])
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
				receiver.drawMenuNano(engine, 0, 1.5f, "MEDAL", COLOR.BLUE, .5f)
				receiver.drawMenuMedal(engine, 2, 2, "AC", medalAC)
				receiver.drawMenuMedal(engine, 5, 2, "ST", medalST)
				receiver.drawMenuMedal(engine, 8, 2, "SK", medalSK)
				receiver.drawMenuMedal(engine, 1, 3, "RE", medalRE)
				receiver.drawMenuMedal(engine, 4, 3, "RO", medalRO)
				receiver.drawMenuMedal(engine, 7, 3, "CO", medalCO)

				drawResultStats(
					engine, receiver, 4, COLOR.BLUE, Statistic.LPM, Statistic.SPM, Statistic.PIECE, Statistic.PPS
				)

				drawResult(engine, receiver, 15, COLOR.BLUE, "DECORATION", "%10d".format(decTemp))
			}
		}
	}

	override fun renderExcellent(engine:GameEngine) {
		var col = COLOR.WHITE

		if(grade>=19) {
			col = when {
				engine.statc[0]%4==0 -> COLOR.YELLOW
				engine.statc[0]%2==0 -> col
				else -> COLOR.ORANGE
			}
			receiver.drawMenuFont(engine, .5f, 8f, "YOU ARE A", COLOR.WHITE, 1f)
			receiver.drawMenuFont(engine, 1.25f, 9f, "GRAND", col, 1.5f)
			receiver.drawMenuFont(engine, .5f, 10.5f, "MASTER", col, 1.5f)
			if(grade==19) {
				col = when {
					engine.statc[0]%4==0 -> COLOR.RED
					engine.statc[0]%2==0 -> COLOR.YELLOW
					else -> COLOR.ORANGE
				}
				receiver.drawMenuFont(engine, 1, 12, "LET'S TRY", COLOR.BLUE, 1f)
				receiver.drawMenuFont(engine, 0, 13, "MORE LINES", col, 1f)
				receiver.drawMenuFont(engine, .5f, 14f, "IN STEALTH", COLOR.WHITE, 1f)
			}
		} else if(grade>=17) {
			receiver.drawMenuFont(engine, 3.5f, 8.5f, "BUT...", COLOR.WHITE, 1f)
			if(mRollSTime&&!mRollQuads) {
				col = when {
					engine.statc[0]%4==0 -> COLOR.RED
					engine.statc[0]%2==0 -> COLOR.YELLOW
					else -> COLOR.ORANGE
				}
				receiver.drawMenuFont(engine, 1, 10, "CHALLENGE", COLOR.BLUE, 1f)
				receiver.drawMenuFont(engine, 0, 11, "MORE QUADS", col, 1f)
				receiver.drawMenuFont(engine, .5f, 12f, "NEXT TIME", COLOR.WHITE, 1f)
			} else {
				col = when {
					engine.statc[0]%4==0 -> COLOR.CYAN
					engine.statc[0]%2==0 -> col
					else -> COLOR.BLUE
				}
				receiver.drawMenuFont(engine, .5f, 10f, "CHALLENGE", COLOR.BLUE, 1f)
				receiver.drawMenuFont(engine, -.5f, 11f, "MORE FASTER", col, 1f)
				receiver.drawMenuFont(engine, .5f, 12f, "NEXT TIME", COLOR.WHITE, 1f)
			}
		}
	}

	/* 結果画面の処理 */
	override fun onResult(engine:GameEngine):Boolean {
		owner.musMan.fadeSW = false
		owner.musMan.bgm = if(engine.ending>0)
			if(rollClear<=1) BGM.Result(2) else BGM.Result(3)
		else BGM.Result(0)
		engine.statistics.time = lastGradeTime
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
		owner.replayProp.setProperty("grademania2.version", version)
		owner.statsProp.setProperty("decoration", decoration)

		// Update rankings
		if(!owner.replayMode&&startLevel==0&&!always20g&&!big&&engine.ai==null) {
			updateRanking(grade, engine.statistics.level, lastGradeTime, rollClear)
			if(medalsST[0]>0) updateBestSectionTime()

			if(rankingRank!=-1||medalsST[0]>0) return true
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
			for(i in rankingMax-1 downTo rankingRank+1) {
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
		for(i in 0..<rankingMax)
			if(clear>rankingRollClear[i]) return i
			else if(clear==rankingRollClear[i]&&gr>rankingGrade[i]) return i
			else if(clear==rankingRollClear[i]&&gr==rankingGrade[i]&&lv>rankingLevel[i]) return i
			else if(clear==rankingRollClear[i]&&gr==rankingGrade[i]&&lv==rankingLevel[i]&&time<rankingTime[i]) return i

		return -1
	}

	/** Update best section time records */
	private fun updateBestSectionTime() {
		for(i in 0..<sectionMax)
			if(sectionIsNewRecord[i]) {
				bestSectionTime[i] = sectionTime[i]
				bestSectionQuads[i] = sectionQuads[i]
			}
	}

	companion object {
		/** Current version */
		private const val CURRENT_VERSION = 2

		/** 落下速度 table */
		private val tableGravityValue =
			listOf(
				+4, +6, +8, 10, 12, 16, 32, 48, 64, 80, 96, 112, 128, 144, 4, 32, 64, 96, 128, 160, 192, 224, 256, 512, 768,
				1024, 1280, 1024, 768, -1
			)

		/** 落下速度が変わる level */
		private val tableGravityChangeLevel =
			listOf(
				30, 35, 40, 50, 60, 70, 80, 90, 100, 120, 140, 160, 170, 200, 220, 230, 233, 236, 239, 243, 247, 251, 300,
				330, 360, 400, 420, 450, 500, 10000
			)

		/** ARE table */
		private val tableARE = listOf(25, 25, 24, 23, 22, 21, 19, 16, 14, 12)

		/** ARE after line clear table */
		private val tableARELine = listOf(25, 25, 24, 22, 20, 18, 15, 12, 9, 6)

		/** Line clear times table */
		private val tableLineDelay = listOf(40, 38, 35, 31, 25, 20, 16, 12, 9, 6)

		/** 固定 times table */
		private val tableLockDelay = listOf(30, 30, 30, 30, 30, 30, 28, 26, 24, 20)

		/** DAS table */
		private val tableDAS = listOf(15, 14, 13, 12, 11, 10, 9, 8, 7, 6)

		/** BGM fadeout levels */
		private val tableBGMFadeout = listOf(475, 680, 880)

		/** BGM change levels */
		private val tableBGMChange = listOf(200, 500, 700, 900)

		/** Line clear時に入る段位 point */
		private val tableGradePoint =
			listOf(
				listOf(10, 10, 10, 10, 10, 9, 8, 7, 6, 5, 4, 3, 2),
				listOf(20, 20, 20, 18, 16, 15, 13, 10, 11, 11, 12), listOf(40, 36, 33, 30, 27, 24, 20, 18, 17, 16, 15),
				listOf(50, 47, 44, 40, 40, 38, 36, 34, 32, 31, 30)
			)

		/** 段位 pointのCombo bonus */
		private val tableGradeComboBonus =
			listOf(
				listOf(1.0f, 1.2f, 1.2f, 1.4f, 1.4f, 1.4f, 1.4f, 1.5f, 1.5f, 2.0f),
				listOf(1.0f, 1.4f, 1.5f, 1.6f, 1.7f, 1.8f, 1.9f, 2.0f, 2.1f, 2.5f),
				listOf(1.0f, 1.5f, 1.8f, 2.0f, 2.2f, 2.3f, 2.4f, 2.5f, 2.6f, 3.0f),
				listOf(1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f)
			)

		/** 実際の段位を上げるのに必要な内部段位 */
		private val tableGradeChange = listOf(1, 2, 3, 4, 5, 7, 9, 12, 15, 18, 19, 20, 23, 25, 27, 29, 31, -1)

		/** 段位 pointが1つ減る time */
		private val tableGradeDecayRate =
			listOf(
				125, 100, 80, 50, 48, 47, 45, 44, 43, 42, 41, 40, 36, 33, 30, 28, 26, 24, 22, 20, 19, 18, 17, 16, 15, 15,
				14, 14, 13, 13, 11, 10
			)

		/** 段位のName */
		private val tableGradeName = listOf(
			"9", "8", "7", "6", "5", "4", "3", "2", "1", // 0～8
			"S1", "S2", "S3", "S4", "S5", "S6", "S7", "S8", "S9", // 9～17
			"m", "Gm", "GM" // 18～20
		)

		/** 詳細段位のName */
		private val tableDetailGradeName = listOf(
			"9", "8", "7", "6", "5", "4", "3", "2", "1", // 0～8
			"S1", "S2", "S3", "S4", "S5", "S6", "S7", "S8", "S9", // 9～17
			"M1", "M2", "M3", "M4", "M5", "M6", "M7", "M8", "M9", // 18～26
			"M", "MK", "MV", "MO", "MM", "Gm", "GM" // 27～33
		)

		/** 裏段位のName */
		private val tableSecretGradeName = listOf(
			"9", "8", "7", "6", "5", "4", "3", "2", "1", // 0～8
			"S1", "S2", "S3", "S4", "S5", "S6", "S7", "S8", "S9", // 9～17
			"GM" // 18
		)

		/** LV999 roll time */
		private const val ROLLTIMELIMIT = 3590

		/** 消えRoll に必要なLV999到達時のTime */
		private const val M_ROLL_TIME_REQUIRE = 37440

		/** Number of sections */
		private const val SECTION_MAX = 10

		/** Default section time */
		private const val DEFAULT_SECTION_TIME = 5400
	}
}
