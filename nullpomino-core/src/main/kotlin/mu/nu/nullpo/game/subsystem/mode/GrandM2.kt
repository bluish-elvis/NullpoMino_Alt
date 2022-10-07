/*
 * Copyright (c) 2010-2022, NullNoname
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
import mu.nu.nullpo.util.CustomProperties
import mu.nu.nullpo.util.GeneralUtil.toInt
import mu.nu.nullpo.util.GeneralUtil.toTimeStr

/** GRADE MANIA 2 Mode */
class GrandM2:AbstractMode() {
	/** Next Section の level (これ-1のときに levelストップする) */
	private var nextseclv = 0

	/** Levelが増えた flag */
	private var lvupflag = false

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

	/** Hard dropした段count */
	private var harddropBonus = 0

	/** Combo bonus */
	private var comboValue = 0

	/** Roll 経過 time */
	private var rolltime = 0

	/** Roll completely cleared flag */
	private var rollclear = 0

	/** Roll started flag */
	private var rollstarted = false

	/** 裏段位 */
	private var secretGrade = 0

	/** 段位表示を光らせる残り frame count */
	private var gradeflash = 0

	/** Section Time */
	private val sectionTime = MutableList(SECTION_MAX) {0}

	/** 新記録が出たSection はtrue */
	private val sectionIsNewRecord = MutableList(SECTION_MAX) {false}

	/** Cleared Section count */
	private var sectionscomp = 0

	/** Average Section Time */
	private val sectionavgtime get() = setAverageSectionTime()

	/** 直前のSection Time */
	private var sectionlasttime = 0

	/** Section 内で4-line clearした count */
	private var sectionQuads = MutableList(SECTION_MAX) {0}

	/** 消えRoll flag１ (Section Time) */
	private var mRollSTime = false

	/** 消えRoll flag２ (4-line clear) */
	private var mRollQuads = false

	/** 消えRoll started flag */
	private var mRollFlag = false

	/** 消えRoll 中に消したline count */
	private var mRollLines = 0

	/** medal 状態 */
	private var medalAC = 0
	private var medalST = 0
	private var medalSK = 0
	private var medalRE = 0
	private var medalRO = 0
	private var medalCO = 0

	/** 150個以上Blockがあるとtrue, 70個まで減らすとfalseになる */
	private var recoveryFlag = false

	/** rotationした合計 count (Maximum4個ずつ増える) */
	private var spinCount = 0

	/** Section Time記録表示中ならtrue */
	private var isShowBestSectionTime = false

	private val itemLevel = LevelGrandMenuItem(COLOR.BLUE, true, true)
	/** Level at start */
	private var startLevel:Int by DelegateMenuItem(itemLevel)

	private val itemGhost = BooleanMenuItem("alwaysghost", "FULL GHOST", COLOR.BLUE, false)
	/** When true, always ghost ON */
	private var alwaysghost:Boolean by DelegateMenuItem(itemGhost)

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
	private val rankingTime = MutableList(RANKING_MAX) {0}

	/** Rankings' Roll completely cleared flag */
	private val rankingRollclear = MutableList(RANKING_MAX) {0}

	/** Section Time記録 */
	private val bestSectionTime = MutableList(SECTION_MAX) {0}
	private val bestSectionQuads = MutableList(SECTION_MAX) {0}

	private var decoration = 0
	private var dectemp = 0

	/* Mode name */
	override val name = "Grand Mania"

	/* Initialization */
	override val menu:MenuList = MenuList("grademania2", itemGhost, itemAlert, itemST, itemLevel, item20g, itemBig)
	override val rankMap
		get() = rankMapOf(
			"grade" to rankingGrade, "level" to rankingLevel, "time" to rankingTime, "clear" to rankingRollclear,
			"section.time" to bestSectionTime, "section.quads" to bestSectionQuads
		)
	/* Initialization */
	override fun playerInit(engine:GameEngine) {
		super.playerInit(engine)
		nextseclv = 0
		lvupflag = true
		grade = 0
		gradeInternal = 0
		gradePoint = 0
		gradeDecay = 0
		lastGradeTime = 0
		harddropBonus = 0
		comboValue = 0
		lastscore = 0
		rolltime = 0
		rollclear = 0
		rollstarted = false
		secretGrade = 0
		gradeflash = 0
		sectionTime.fill(0)
		sectionIsNewRecord.fill(false)
		sectionscomp = 0
		sectionlasttime = 0
		sectionQuads.fill(0)
		mRollSTime = true
		mRollQuads = true
		mRollFlag = false
		mRollLines = 0
		medalAC = 0
		medalST = 0
		medalSK = 0
		medalRE = 0
		medalRO = 0
		medalCO = 0
		recoveryFlag = false
		spinCount = 0

		rankingRank = -1
		rankingGrade.fill(0)
		rankingLevel.fill(0)
		rankingTime.fill(0)
		rankingRollclear.fill(0)
		bestSectionTime.fill(0)
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
			for(i in 0 until SECTION_MAX)
				bestSectionTime[i] = DEFAULT_SECTION_TIME
			version = owner.replayProp.getProperty("grademania2.version", 0)
		}

		owner.bgMan.bg = startLevel+10
		setSpeed(engine)
	}

	private fun calcBgmLv(lv:Int) = tableBGMChange.count {lv>=it}.let {
		it+maxOf(0, it-3)+(mRollSTime&&mRollQuads).toInt()
	}

	/** Update falling speed
	 * @param engine GameEngine
	 */
	private fun setSpeed(engine:GameEngine) {
		engine.speed.gravity = if(always20g||engine.statistics.time>=54000) -1
		else tableGravityValue[(tableGravityChangeLevel.indexOfFirst {engine.statistics.level<it}).let {if(it<0) tableGravityChangeLevel.size-1 else it}]

		val section = minOf(engine.statistics.level/100, tableARE.size-1)
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

	/** Update average section time */
	private fun setAverageSectionTime() =
		if(sectionscomp>0) {
			val i = minOf(sectionscomp+startLevel, sectionTime.size)
			sectionTime.slice(startLevel until i).sum()/i
		} else 0

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
		section==6 -> sectionTime.take(5).sum()/5+1000
		section>6 -> sectionTime[section-1]+(1140-section*60)
		else -> 4500-section*100
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

	/** RO medal check
	 * @param engine Engine
	 */
	private fun roMedalCheck(engine:GameEngine) {
		val spinAverage = spinCount.toFloat()/engine.statistics.totalPieceLocked.toFloat()

		if(spinAverage>=1.2f&&medalRO<3) {
			engine.playSE("medal${++medalRO}")
			dectemp += 6
		}
	}

	/* Called at settings screen */
	override fun onSetting(engine:GameEngine):Boolean {
		// Menu
		if(!engine.owner.replayMode) {
			// Configuration changes
			val change = updateMenu(engine)

			if(change!=0) {
				owner.bgMan.bg = 10+startLevel
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
				sectionscomp = 0

				owner.musMan.fadesw = true
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

	/* Render the settings screen */
	/*override fun renderSetting(engine:GameEngine, playerID:Int) {
		drawMenu(engine, playerID, receiver, 0, COLOR.BLUE, 0, "Level" to (startLevel*100),
			"FULL GHOST" to alwaysghost, "FULL 20G" to always20g, "LVSTOPSE" to secAlert, "SHOW STIME" to showST,
			"BIG" to big)
	}*/

	/* Called at game start */
	override fun startGame(engine:GameEngine) {
		val lv = startLevel*100
		engine.statistics.level = lv

		dectemp = 0
		nextseclv = maxOf(100, minOf(999, lv+100))

		owner.bgMan.bg = 10+startLevel/100

		engine.big = big

		setSpeed(engine)
		owner.musMan.bgm = BGM.GrandA(calcBgmLv(lv))
	}

	/* Render score */
	override fun renderLast(engine:GameEngine) {
		receiver.drawScoreFont(engine, 0, 0, "GRAND MANIA", COLOR.CYAN)

		receiver.drawScoreFont(engine, -1, -4*2, "DECORATION", scale = .5f)
		receiver.drawScoreBadges(engine, 0, -3, 100, decoration)
		receiver.drawScoreBadges(engine, 5, -4, 100, dectemp)
		if(engine.stat==GameEngine.Status.SETTING||engine.stat==GameEngine.Status.RESULT&&!owner.replayMode) {
			if(!owner.replayMode&&startLevel==0&&!big&&!always20g&&engine.ai==null)
				if(!isShowBestSectionTime) {
					// Rankings

					receiver.drawScoreFont(engine, 3, 2, "GRADE TIME LEVEL", COLOR.BLUE)

					for(i in 0 until RANKING_MAX) {
						var gcolor = COLOR.WHITE
						if(rankingRollclear[i]==1||rankingRollclear[i]==3) gcolor = COLOR.GREEN
						if(rankingRollclear[i]==2||rankingRollclear[i]==4) gcolor = COLOR.ORANGE

						receiver.drawScoreGrade(
							engine, 0, 3+i, String.format("%2d", i+1), if(rankingRank==i) COLOR.RAINBOW else COLOR.YELLOW
						)
						if(rankingGrade[i]>=0&&rankingGrade[i]<tableGradeName.size)
							receiver.drawScoreGrade(engine, 3, 3+i, tableGradeName[rankingGrade[i]], gcolor)
						receiver.drawScoreNum(engine, 7, 3+i, rankingTime[i].toTimeStr, i==rankingRank)
						receiver.drawScoreNum(engine, 15, 3+i, String.format("%03d", rankingLevel[i]), i==rankingRank)
					}

					receiver.drawScoreFont(engine, 0, 17, "F:VIEW SECTION TIME", COLOR.GREEN)
				} else {
					// Section Time
					receiver.drawScoreFont(engine, 0, 2, "SECTION TIME QUADS", COLOR.BLUE)

					var totalTime = 0
					for(i in 0 until SECTION_MAX) {
						val temp = minOf(i*100, 999)
						val temp2 = minOf((i+1)*100-1, 999)

						val strSectionTime:String = String.format(
							"%3d-%3d %s %d", temp, temp2, bestSectionTime[i].toTimeStr,
							bestSectionQuads[i]
						)

						receiver.drawScoreNum(engine, 0, 3+i, strSectionTime, sectionIsNewRecord[i])

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
			val g20 = engine.speed.gravity<0&&engine.statistics.time%3==0
			// 段位
			if(grade>=0&&grade<tableGradeName.size)
				receiver.drawScoreGrade(engine, 0, 1, tableGradeName[grade], gradeflash>0&&gradeflash%4==0||g20, 2f)
			if(grade<17) {
				receiver.drawScoreSpeed(engine, 0, 3, (gradePoint-gradeDecay*1f/tableGradeDecayRate[gradeInternal])/100f, 5f)
				receiver.drawScoreNum(
					engine, 0, 4, String.format("%02.1f%%", gradePoint-gradeDecay*1f/tableGradeDecayRate[gradeInternal]),
					if(g20) COLOR.YELLOW
					else if(mRollQuads&&mRollSTime) COLOR.CYAN else COLOR.BLUE
				)
			}
			if(gradeInternal>=0&&gradeInternal<tableDetailGradeName.size)
				receiver.drawScoreGrade(
					engine, 3, 3, tableDetailGradeName[gradeInternal], gradeflash>0&&gradeflash%4==0||g20
				)

			// Score
			receiver.drawScoreFont(
				engine, 0, 6, "Score", if(g20&&mRollQuads) COLOR.CYAN else COLOR.BLUE
			)
			receiver.drawScoreNum(engine, 5, 6, "+$lastscore", g20)
			receiver.drawScoreNum(engine, 0, 7, "$scDisp", g20, 2f)

			// level
			receiver.drawScoreFont(
				engine, 0, 9, "Level", if(g20&&mRollSTime&&mRollQuads) COLOR.CYAN else COLOR.BLUE
			)
			receiver.drawScoreNum(engine, 1, 10, String.format("%3d", maxOf(engine.statistics.level, 0)), g20)
			receiver.drawScoreSpeed(engine, 0, 11, if(g20) 1f else engine.speed.rank, 4f)
			receiver.drawScoreNum(engine, 1, 12, String.format("%3d", nextseclv), g20)

			// Time
			receiver.drawScoreFont(engine, 0, 14, "Time", if(g20&&mRollSTime) COLOR.CYAN else COLOR.BLUE)
			if(engine.ending!=2||rolltime/20%2==0)
				receiver.drawScoreNum(
					engine, 0, 15, engine.statistics.time.toTimeStr, g20&&mRollSTime, 2f
				)

			// Roll 残り time
			if(engine.gameActive&&engine.ending==2) {
				var time = ROLLTIMELIMIT-rolltime
				if(time<0) time = 0
				receiver.drawScoreFont(engine, 0, 17, "ROLL TIME", COLOR.BLUE)
				receiver.drawScoreNum(engine, 0, 18, time.toTimeStr, time>0&&time<10*60, 2f)
			}

			// medal
			receiver.drawScoreMedal(engine, 0, 20, "AC", medalAC)
			receiver.drawScoreMedal(engine, 3, 20, "ST", medalST)
			receiver.drawScoreMedal(engine, 0, 21, "SK", medalSK)
			receiver.drawScoreMedal(engine, 3, 21, "RE", medalRE)
			receiver.drawScoreMedal(engine, 0, 22, "SK", medalRO)
			receiver.drawScoreMedal(engine, 3, 22, "CO", medalCO)

			// Section Time
			if(showST&&sectionTime.isNotEmpty()) {
				val x = receiver.nextDisplayType==2
				receiver.drawScoreFont(engine, if(x) 8 else 10, 2, "SECTION TIME", COLOR.BLUE)

				val section = engine.statistics.level/100
				for(i in sectionTime.indices)
					if(sectionTime[i]>0) {
						var strSeparator = "-"
						if(i==section&&engine.ending==0) strSeparator = "+"

						val color:COLOR =
							if(sectionQuads[i]>=(if(i<5) 2 else 1)&&sectionTime[i]<mRollTime(i))
								if(sectionIsNewRecord[i]) COLOR.CYAN else COLOR.GREEN
							else if(sectionIsNewRecord[i]) COLOR.RED else COLOR.WHITE

						val strSectionTime = StringBuilder()
						for(l in 0 until i)
							strSectionTime.append("\n")
						strSectionTime.append(
							String.format(
								"%3d%s%s %d\n",
								if(i<10) i*100 else 999, strSeparator, sectionTime[i].toTimeStr, sectionQuads[i]
							)
						)
						receiver.drawScoreNum(engine, if(x) 9 else 10, 3, "$strSectionTime", color, if(x) .75f else 1f)
					}

				receiver.drawScoreFont(engine, if(x) 8 else 12, if(x) 11 else 14, "AVERAGE", COLOR.BLUE)
				receiver.drawScoreNum(
					engine,
					if(x) 8 else 12,
					if(x) 12 else 15,
					(engine.statistics.time/(sectionscomp+(engine.ending==0).toInt())).toTimeStr,
					2f
				)
			}
		}
	}

	/* 移動中の処理 */
	override fun onMove(engine:GameEngine):Boolean {
		// 新規ピース出現時
		if(engine.ending==0&&engine.statc[0]==0&&!engine.holdDisable&&!lvupflag) {
			// Level up
			levelUp(engine, (engine.statistics.level<nextseclv-1).toInt())
		}
		if(engine.ending==0&&engine.statc[0]>0&&(version>=1||!engine.holdDisable)) lvupflag = false

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
		if(engine.ending==2&&!rollstarted) {
			rollstarted = true

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

	/* ARE中の処理 */
	override fun onARE(engine:GameEngine):Boolean {
		// 最後の frame
		if(engine.ending==0&&engine.statc[0]>=engine.statc[1]-1&&!lvupflag) {
			levelUp(engine, (engine.statistics.level<nextseclv-1).toInt())

			lvupflag = true
		}

		return false
	}

	/** levelが上がったときの共通処理 */
	private fun levelUp(engine:GameEngine, lu:Int) {
		val levelb = engine.statistics.level
		engine.statistics.level += lu
		val lA = engine.statistics.level
		// Meter
		engine.meterValue = lA%100/99f
		engine.meterColor = GameEngine.METER_COLOR_LEVEL

		// 速度変更
		setSpeed(engine)

		if(version>=2) {
			// Hard drop bonusInitialization
			harddropBonus = 0

			// RE medal
			if(engine.timerActive&&medalRE<3) {
				val blocks = engine.field.howManyBlocks

				if(!recoveryFlag) {
					if(blocks>=150) recoveryFlag = true
				} else if(blocks<=70) {
					recoveryFlag = false
					dectemp += 5+medalRE// 5 11 18
					engine.playSE("medal${++medalRE}")
				}
			}
		}
		// LV100到達でghost を消す
		if(lA>=100&&!alwaysghost) engine.ghost = false

		if(lu<=0) return
		if(engine.statistics.level==nextseclv-1&&secAlert) engine.playSE("levelstop")
		// BGM fadeout
		if(tableBGMFadeout.any {it in levelb..lA}) owner.musMan.fadesw = true
		// BGM切り替え
		if(tableBGMChange.any {it in levelb..lA}) {
			owner.musMan.fadesw = false
			owner.musMan.bgm = BGM.GrandA(calcBgmLv(lA))
		}
	}

	/* Calculate score */
	override fun calcScore(engine:GameEngine, ev:ScoreEvent):Int {
		// Combo
		val li = ev.lines
		comboValue = if(li==0) 1
		else maxOf(1, comboValue+2*li-2)

		// RO medal 用カウント
		spinCount += minOf(4, engine.nowPieceSpinCount)

		if(li>=1&&engine.ending==0) {
			// 段位 point
			var index = gradeInternal
			if(index>10) index = 10
			val basepoint = tableGradePoint[li-1][index]

			var indexcombo = engine.combo+if(ev.b2b>0) 0 else -1
			if(indexcombo<0) indexcombo = 0
			if(indexcombo>tableGradeComboBonus[li-1].size-1) indexcombo = tableGradeComboBonus[li-1].size-1
			val combobonus = tableGradeComboBonus[li-1][indexcombo]

			val levelbonus = 1+engine.statistics.level/250

			val point = basepoint.toFloat()*combobonus*levelbonus.toFloat()
			gradePoint += point.toInt()

			// 内部段位上昇
			while(gradePoint>=100) {
				gradePoint -= 100
				gradeDecay = 0
				if(gradeInternal<31) gradeInternal++

				if(tableGradeChange[grade]!=-1&&gradeInternal>=tableGradeChange[grade]) {
					engine.playSE("grade${grade*4/17}")
					grade++
					dectemp++
					gradeflash = 180
					lastGradeTime = engine.statistics.time
				}
			}

			// 4-line clearカウント
			if(li>=4) {
				sectionQuads[engine.statistics.level/100]++

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
					dectemp += 3+medalSK*2// 3 8 15
					engine.playSE("medal${++medalSK}")
				}
			}

			// AC medal
			if(engine.field.isEmpty) {
				dectemp += li*25
				if(li==3) dectemp += 25
				if(li==4) dectemp += 150
				if(medalAC<3) {
					dectemp += 3+medalAC*4// 3 10 21
					engine.playSE("medal${++medalAC}")
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
			val lu = li+maxOf(0, minOf(2, ev.b2b))
			levelUp(engine, lu)

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

				// 消えRoll check
				mRollFlag = mRollCheck(levelb)
				// ST medal
				stMedalCheck(engine, levelb/100)

				// RO medal
				roMedalCheck(engine)

				// 段位M
				if(mRollFlag) {
					engine.playSE("applause4")
					grade = 18
					gradeInternal = 32
					gradeflash = 180
					lastGradeTime = engine.statistics.time
				} else engine.playSE("applause3")
			} else if(engine.statistics.level>=nextseclv) {
				// Next Section

				// Background切り替え
				owner.bgMan.fadesw = true
				owner.bgMan.fadecount = 0
				owner.bgMan.fadebg = 10+nextseclv/100

				engine.playSE("levelup")

				// Section Timeを記録
				sectionlasttime = sectionTime[levelb/100]
				sectionscomp++
				setAverageSectionTime()

				// 消えRoll check
				mRollFlag = mRollCheck(levelb)
				if(mRollFlag) engine.playSE("cool")
				// ST medal
				stMedalCheck(engine, levelb/100)

				// RO medal
				if(nextseclv==300||nextseclv==700) roMedalCheck(engine)

				// Update level for next section
				nextseclv += 100
				if(nextseclv>999) nextseclv = 999
			} else if(engine.statistics.level==nextseclv-1&&secAlert) engine.playSE("levelstop")

			// Calculate score
			lastscore =
				((((levelb+li)/(if(engine.b2b) 3 else 4)+engine.softdropFall+(if(engine.manualLock) 1 else 0)+harddropBonus)*li
					*comboValue*if(engine.field.isEmpty) 4 else 1)+engine.statistics.level/2
					+maxOf(0, engine.lockDelay-engine.lockDelayNow)*7)
			engine.statistics.scoreLine += lastscore
			return lastscore
		} else if(li>=1&&mRollFlag&&engine.ending==2)
		// 消えRoll 中のLine clear
			mRollLines += li
		return 0
	}

	/* Called when hard drop used */
	override fun afterHardDropFall(engine:GameEngine, fall:Int) {
		if(fall*2>harddropBonus) harddropBonus = fall*2
	}

	/* 各 frame の終わりの処理 */
	override fun onLast(engine:GameEngine) {
		super.onLast(engine)
		// 段位上昇時のフラッシュ
		if(gradeflash>0) gradeflash--

		// 15分経過
		if(engine.statistics.time>=54000) setSpeed(engine)

		// Section Time増加
		if(engine.timerActive&&engine.ending==0) {
			val section = engine.statistics.level/100

			if(section>=0&&section<sectionTime.size) sectionTime[section]++
		}

		// Ending
		if(engine.gameActive&&engine.ending==2) {
			rolltime++

			// Time meter
			val remainRollTime = ROLLTIMELIMIT-rolltime
			engine.meterValue = remainRollTime*1f/ROLLTIMELIMIT
			engine.meterColor = GameEngine.METER_COLOR_LIMIT

			// Roll 終了
			if(rolltime>=ROLLTIMELIMIT) {
				rollclear = 2

				if(mRollFlag) {
					grade = 19
					gradeflash = 180
					lastGradeTime = engine.statistics.time
					engine.playSE("applause5")
					engine.playSE("grade4")
					gradeInternal = 33
					rollclear = 3
					if(mRollLines>=16) {
						rollclear = 4
						grade = 20
					}
				}

				engine.blockOutlineType = GameEngine.BLOCK_OUTLINE_NORMAL

				engine.gameEnded()
				engine.resetStatc()
				engine.stat = GameEngine.Status.EXCELLENT
				if(engine.statistics.time<28800) dectemp += (28800-engine.statistics.time)/1800
			}
		} else if(engine.statistics.level==nextseclv-1)
			engine.meterColor = if(engine.meterColor==0xFFFFFF) 0 else 0xFFFFFF
	}

	/* game over */
	override fun onGameOver(engine:GameEngine):Boolean {
		if(engine.statc[0]==0) {
			val time = engine.statistics.time
			if(time<6000)
				dectemp -= 3
			else {
				dectemp++
				if(time%3600<=60||time%3600>=3540) dectemp++
			}

			if(time>41100) dectemp -= 1+(time-41100)/1800

			// Blockの表示を元に戻す
			engine.blockOutlineType = GameEngine.BLOCK_OUTLINE_NORMAL
			// 裏段位
			secretGrade = engine.field.secretGrade
			decoration += dectemp+secretGrade
		}

		return false
	}

	/* 結果画面 */
	override fun renderResult(engine:GameEngine) {
		receiver.drawMenuFont(engine, 0, 0, "\u0090\u0093 PAGE${engine.statc[1]+1}/3", COLOR.RED)

		when(engine.statc[1]) {
			0 -> {
				var gcolor = COLOR.WHITE
				if(rollclear==1||rollclear==3) gcolor = COLOR.GREEN
				if(rollclear==2||rollclear==4) gcolor = COLOR.ORANGE
				receiver.drawMenuFont(engine, 0, 3, "GRADE", COLOR.BLUE)
				receiver.drawMenuGrade(engine, 6, 2, tableGradeName[grade], gcolor, 2f)
				receiver.drawMenuGrade(engine, 3, 2, tableDetailGradeName[gradeInternal], gcolor)

				drawResultStats(
					engine, receiver, 4, COLOR.BLUE, Statistic.SCORE, Statistic.LINES, Statistic.LEVEL_MANIA, Statistic.TIME
				)
				drawResultRank(engine, receiver, 13, COLOR.BLUE, rankingRank)
				if(secretGrade>4)
					drawResult(
						engine, receiver, 15, COLOR.BLUE, "S. GRADE",
						String.format("%10s", tableSecretGradeName[secretGrade-1])
					)
			}
			1 -> {
				receiver.drawMenuFont(engine, 0, 2, "SECTION", COLOR.BLUE)

				for(i in sectionTime.indices)
					if(sectionTime[i]>0)
						receiver.drawMenuNum(engine, 2, 3+i, sectionTime[i].toTimeStr, sectionIsNewRecord[i])

				if(sectionavgtime>0) {
					receiver.drawMenuFont(engine, 0, 14, "AVERAGE", COLOR.BLUE)
					receiver.drawMenuNum(engine, 0, 15, sectionavgtime.toTimeStr, 1.7f)
				}
			}
			2 -> {
				receiver.drawMenuFont(engine, 0, 2, "MEDAL", COLOR.BLUE)
				receiver.drawMenuMedal(engine, 5, 2, "SK", medalSK)
				receiver.drawMenuMedal(engine, 8, 2, "ST", medalST)
				receiver.drawMenuMedal(engine, 1, 3, "AC", medalAC)
				receiver.drawMenuMedal(engine, 4, 3, "CO", medalCO)
				receiver.drawMenuMedal(engine, 7, 3, "RE", medalRE)
				receiver.drawMenuMedal(engine, 8, 4, "RO", medalRO)

				drawResultStats(
					engine, receiver, 4, COLOR.BLUE, Statistic.LPM, Statistic.SPM, Statistic.PIECE, Statistic.PPS
				)

				drawResult(engine, receiver, 15, COLOR.BLUE, "DECORATION", String.format("%10d", dectemp))
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
		owner.musMan.fadesw = false
		owner.musMan.bgm = if(engine.ending>0)
			if(rollclear<=1) BGM.Result(2) else BGM.Result(3)
		else BGM.Result(0)
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
			updateRanking(grade, engine.statistics.level, lastGradeTime, rollclear)
			if(medalST==3) updateBestSectionTime()

			if(rankingRank!=-1||medalST==3) return true
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
			if(clear>rankingRollclear[i]) return i
			else if(clear==rankingRollclear[i]&&gr>rankingGrade[i]) return i
			else if(clear==rankingRollclear[i]&&gr==rankingGrade[i]&&lv>rankingLevel[i]) return i
			else if(clear==rankingRollclear[i]&&gr==rankingGrade[i]&&lv==rankingLevel[i]&&time<rankingTime[i]) return i

		return -1
	}

	/** Update best section time records */
	private fun updateBestSectionTime() {
		for(i in 0 until SECTION_MAX)
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
				4, 6, 8, 10, 12, 16, 32, 48, 64, 80, 96, 112, 128, 144, 4, 32, 64, 96, 128, 160, 192, 224, 256, 512, 768,
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
		private val tableBGMChange = listOf(500, 700, 900)

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

		/** Number of entries in rankings */
		private const val RANKING_MAX = 13

		/** Number of sections */
		private const val SECTION_MAX = 10

		/** Default section time */
		private const val DEFAULT_SECTION_TIME = 5400
	}
}
