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

import mu.nu.nullpo.game.component.BGMStatus.BGM
import mu.nu.nullpo.game.component.Controller
import mu.nu.nullpo.game.event.EventReceiver.COLOR
import mu.nu.nullpo.game.event.ScoreEvent
import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.game.subsystem.mode.menu.BooleanMenuItem
import mu.nu.nullpo.game.subsystem.mode.menu.DelegateMenuItem
import mu.nu.nullpo.game.subsystem.mode.menu.LevelGrandMenuItem
import mu.nu.nullpo.game.subsystem.mode.menu.MenuList
import mu.nu.nullpo.game.subsystem.mode.menu.TimeMenuItem
import mu.nu.nullpo.gui.common.BaseFont
import mu.nu.nullpo.util.CustomProperties
import mu.nu.nullpo.util.GeneralUtil.toInt
import mu.nu.nullpo.util.GeneralUtil.toTimeStr

/** SPEED MANIA-DEATH Mode */
class GrandS1:AbstractMode() {
	/** Next Section の level (これ-1のときに levelストップする) */
	private var nextSecLv = 0
	/** Levelが増えた flag */
	private var lvupFlag = false

	/** Combo bonus */
	private var comboValue = 0

	/** Roll 経過 time */
	private var rollTime = 0

	/** Roll started flag */
	private var rollStarted = false

	/** 段位 */
	private var grade = 0
	/** 段位表示を光らせる残り frame count */
	private var gradeFlash = 0
	/** 裏段位 */
	private var secretGrade = 0

	/** Section Time */
	private val sectionTime = MutableList(SECTION_MAX) {0}
	/** 新記録が出たSection はtrue */
	private val sectionIsNewRecord = MutableList(SECTION_MAX) {false}
	/** Cleared Section count */
	private var sectionsDone = 0
	/** Average Section Time */
	private val sectionAvgTime
		get() = sectionTime.filter {it>0}.average().toFloat()
	/** 直前のSection Time */
	private var sectionLastTime = 0

	/** AC medal 状態 */
	private var medalAC = 0
	/** ST medal 状態 */
	private val medalST
		get() = medalsST.indexOfFirst {it>0}.let {
			when(it) {
				in 0..2 -> 3-it
				else -> 0
			}
		}

	private var medalsST = mutableListOf(0, 0, 0)
	/** SK medal 状態 */
	private var medalSK = 0
	/** RE medal 状態 */
	private var medalRE = 0
	/** RO medal 状態 */
	private var medalRO = 0
	/** CO medal 状態 */
	private var medalCO = 0
	/** 150個以上Blockがあるとtrue, 70個まで減らすとfalseになる */
	private var recoveryFlag = false
	/** spinした合計 count (Maximum4個ずつ増える) */
	private var spinCount = 0
	/** Section Time記録表示中ならtrue */
	private var isShowBestSectionTime = false

	private val itemLevel = LevelGrandMenuItem(COLOR.BLUE)
	/** Level at start */
	private var startLevel:Int by DelegateMenuItem(itemLevel)

	/** LV500の足切りTime */
	private val itemQualify = TimeMenuItem("lv500torikan", "QUALIFY", COLOR.BLUE, 12300, 0..36000)
	private var qualify:Int by DelegateMenuItem(itemQualify)

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
	/** Section Time記録 */
	private val bestSectionTime = MutableList(SECTION_MAX) {DEFAULT_SECTION_TIME}

	private var decoration = 0
	private var decTemp = 0

	/* Mode name */
	override val name = "Grand Storm"
	override val gameIntensity = 3
	/* Initialization */
	override val menu = MenuList("speedmania1", itemLevel, itemQualify, itemAlert, itemST, itemBig)

	override val rankMap
		get() = rankMapOf(
			"grade" to rankingGrade,
			"level" to rankingLevel,
			"time" to rankingTime,
			"section.time" to bestSectionTime
		)

	override fun playerInit(engine:GameEngine) {
		super.playerInit(engine)
		nextSecLv = 0
		lvupFlag = true
		comboValue = 0
		lastScore = 0
		rollTime = 0
		rollStarted = false
		grade = 0
		gradeFlash = 0
		secretGrade = 0
		sectionTime.fill(0)
		sectionIsNewRecord.fill(false)
		sectionsDone = 0
		sectionLastTime = 0
		medalAC = 0
		medalsST.fill(0)
		medalSK = 0
		medalRE = 0
		medalRO = 0
		medalCO = 0
		recoveryFlag = false
		spinCount = 0

		rankingRank = -1
		rankingGrade.fill(0)
		rankingLevel.fill(0)
		rankingTime.fill(-1)
		bestSectionTime.fill(DEFAULT_SECTION_TIME)

		engine.twistEnable = false
		engine.b2bEnable = true
		engine.splitB2B = true
		engine.comboType = GameEngine.COMBO_TYPE_DOUBLE
		engine.frameColor = GameEngine.FRAME_COLOR_RED
		engine.bigHalf = true
		engine.bigMove = true
		engine.staffrollEnable = true
		engine.staffrollNoDeath = true

		if(!owner.replayMode) {
			version = CURRENT_VERSION
		} else {
			for(i in 0..<SECTION_MAX)
				bestSectionTime[i] = DEFAULT_SECTION_TIME
			version = owner.replayProp.getProperty("speedmania.version", 0)
		}

		owner.bgMan.bg = startLevel
	}

	private fun calcBgmLv(lv:Int) = 3+tableBGMChange.count {lv>=it}

	/** Update falling speed
	 * @param engine GameEngine
	 */
	private fun setSpeed(engine:GameEngine) {
		engine.speed.gravity = -1

		val section = minOf(engine.statistics.level/100, tableARE.size-1)
		engine.speed.are = tableARE[section]
		engine.speed.areLine = tableARELine[section]
		engine.speed.lineDelay = tableLineDelay[section]
		engine.speed.lockDelay = tableLockDelay[section]
		engine.speed.das = tableDAS[section]
	}

	/** ST medal check
	 * @param engine GameEngine
	 * @param section Section number
	 */
	private fun stMedalCheck(engine:GameEngine, section:Int) {
		val best = bestSectionTime[section]

		if(sectionLastTime<best||best<=0) {
			engine.playSE("medal3")
			medalsST[0]++
			decTemp += 15
			if(!owner.replayMode) {
				decTemp++
				sectionIsNewRecord[section] = true
			}
		} else if(sectionLastTime<best+300) {
			engine.playSE("medal2")
			medalsST[1]++
			decTemp += 9
		} else if(sectionLastTime<best+600) {
			engine.playSE("medal1")
			medalsST[2]++
			decTemp += 3
		}
	}

	/** RO medal check
	 * @param engine Engine
	 */
	private fun roMedalCheck(engine:GameEngine) {
		val spinAverage = spinCount.toFloat()/engine.statistics.totalPieceLocked.toFloat()

		if(spinAverage>=1.2f&&medalRO<3) {
			engine.playSE("medal${++medalRO}")
			decTemp += 6
		}
	}

	/* Called at settings screen */
	override fun onSetting(engine:GameEngine):Boolean {
		// Menu
		if(!owner.replayMode) {
			// section time display切替
			if(engine.ctrl.isPush(Controller.BUTTON_F)) {
				engine.playSE("change")
				isShowBestSectionTime = !isShowBestSectionTime
			}
		}
		return super.onSetting(engine)
	}

	override fun onSettingChanged(engine:GameEngine) {
		if(qualify in 1..6150) qualify = 12300
		if(qualify in 6151..12299) qualify = 0
		super.onSettingChanged(engine)
	}

	override fun onReady(engine:GameEngine):Boolean {
		if(engine.statc[0]==0) {
			isShowBestSectionTime = false
			sectionsDone = 0
			owner.musMan.fadeSW = true
		}
		return super.onReady(engine)
	}

	/* Called at game start */
	override fun startGame(engine:GameEngine) {
		val lv = startLevel*100
		engine.statistics.level = lv

		nextSecLv = maxOf(100,minOf(engine.statistics.level+100,999))

		owner.bgMan.bg = engine.statistics.level/100

		engine.big = big
		decTemp = 0
		setSpeed(engine)
		owner.musMan.bgm = BGM.GrandA(calcBgmLv(lv))
	}

	override fun renderFirst(engine:GameEngine) {
		if(engine.ending==2) receiver.drawStaffRoll(engine, rollTime*1f/ROLLTIMELIMIT)
	}
	/* Render score */
	override fun renderLast(engine:GameEngine) {
		receiver.drawScoreFont(engine, 0, 0, name, COLOR.RED)

		receiver.drawScoreNano(engine, -1, -4*2, "DECORATION", scale = .5f)
		receiver.drawScoreBadges(engine, 0, -3, 100, decoration)
		receiver.drawScoreBadges(engine, 5, -4, 100, decTemp)
		if(engine.stat==GameEngine.Status.SETTING||engine.stat==GameEngine.Status.RESULT&&!owner.replayMode) {
			if(!owner.replayMode&&startLevel==0&&!big&&engine.ai==null)
				if(!isShowBestSectionTime) {
					// Rankings
					val topY = if(receiver.nextDisplayType==2) 5 else 3
					receiver.drawScoreFont(engine, 2, topY-1, "LEVEL  TIME", COLOR.BLUE)

					for(i in 0..<RANKING_MAX) {
						receiver.drawScoreGrade(
							engine, 0, topY+i, "%2d".format(i+1), if(rankingRank==i) COLOR.RAINBOW else COLOR.YELLOW
						)
						receiver.drawScoreGrade(engine, 2, topY+i, tableGradeName[rankingGrade[i]], i==rankingRank)
						receiver.drawScoreNum(engine, 5, topY+i, "${rankingLevel[i]}", i==rankingRank)
						receiver.drawScoreNum(engine, 8, topY+i, rankingTime[i].toTimeStr, i==rankingRank)
					}

					receiver.drawScoreFont(engine, 0, 17, "F:VIEW SECTION TIME", COLOR.GREEN)
				} else {
					// Section Time
					receiver.drawScoreFont(engine, 0, 2, "SECTION TIME", COLOR.BLUE)

					val totalTime = (0..<SECTION_MAX).fold(0) {tt, i ->
						val slv = minOf(i*100, 999)
						receiver.drawScoreNum(
							engine, 0, 3+i, "%3d-%3d %s".format(slv, slv+99, bestSectionTime[i].toTimeStr), sectionIsNewRecord[i]
						)
						tt+bestSectionTime[i]
					}

					receiver.drawScoreFont(engine, 0, 14, "TOTAL", COLOR.BLUE)
					receiver.drawScoreNum(engine, 0, 15, totalTime.toTimeStr, 2f)
					receiver.drawScoreFont(engine, 9, 14, "AVERAGE", COLOR.BLUE)
					receiver.drawScoreNum(engine, 9, 15, (totalTime/SECTION_MAX).toTimeStr, 2f)

					receiver.drawScoreFont(engine, 0, 17, "F:VIEW RANKING", COLOR.GREEN)
				}
		} else {
			// 段位
			if(grade>=1&&grade<tableGradeName.size)
				receiver.drawScoreGrade(
					engine, 0, 2, tableGradeName[grade], if(engine.statistics.time%3==0)
						COLOR.YELLOW
					else
						COLOR.ORANGE, 2f
				)

			// Score
			receiver.drawScoreFont(engine, 0, 6, "Score", engine.statistics.time%3!=0)
			receiver.drawScoreNum(engine, 5, 6, "+$lastScore", engine.statistics.time%3!=0)
			receiver.drawScoreNum(engine, 0, 7, "$scDisp", engine.statistics.time%3!=0, 2f)
			// level
			receiver.drawScoreFont(engine, 0, 9, "Level", engine.statistics.time%3!=0)
			receiver.drawScoreNum(
				engine, 1, 10, "%3d".format(maxOf(engine.statistics.level, 0)), engine.statistics.time%3!=0
			)
			receiver.drawScoreSpeed(engine, 0, 11, if(engine.statistics.time%3!=0) 40 else 0, 4)
			receiver.drawScoreNum(engine, 1, 12, "%3d".format(nextSecLv), engine.statistics.time%3!=0)

			// Time
			receiver.drawScoreFont(engine, 0, 14, "Time", engine.statistics.time%3!=0)
			if(engine.ending!=2||rollTime/20%2==0)
				receiver.drawScoreNum(
					engine, 0, 15, engine.statistics.time.toTimeStr, engine.statistics.time%3!=0, 2f
				)

			// Roll 残り time
			if(engine.gameActive&&engine.ending==2) {
				var time = ROLLTIMELIMIT-rollTime
				if(time<0) time = 0
				receiver.drawScoreFont(engine, 0, 17, "ROLL TIME", engine.statistics.time%3!=0)
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
				val x = if(receiver.nextDisplayType==2) 8 else 12
				val x2 = if(receiver.nextDisplayType==2) 9 else 12

				receiver.drawScoreFont(engine, x, 2, "SECTION TIME", COLOR.BLUE)

				val section = engine.statistics.level/100
				sectionTime.forEachIndexed {i, it ->
					if(it>0) receiver.drawScoreNum(
						engine, x, 3+i, "%3d%s%s".format(
							minOf(i*100, 999), if(i==section&&engine.ending==0) "+" else "-", it.toTimeStr
						), sectionIsNewRecord[i]
					)
				}
				receiver.drawScoreFont(engine, x2, 14, "AVERAGE", engine.statistics.time%3!=0)
				receiver.drawScoreNum(
					engine, x2, 15, (engine.statistics.time/(sectionsDone+(engine.ending==0).toInt())).toTimeStr,
					engine.statistics.time%3!=0,
					2f
				)
			}
		}
	}

	/* 移動中の処理 */
	override fun onMove(engine:GameEngine):Boolean {
		// 新規ピース出現時
		if(engine.ending==0&&engine.statc[0]==0&&!engine.holdDisable&&!lvupFlag)
		// Level up
			levelUp(engine, (engine.statistics.level<nextSecLv-1).toInt())

		if(engine.ending==0&&engine.statc[0]>0&&(version>=2||!engine.holdDisable)) lvupFlag = false

		// Endingスタート
		if(engine.ending==2&&!rollStarted) rollStarted = true

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
	private fun levelUp(engine:GameEngine, lu:Int) {
		val lb = engine.statistics.level
		engine.statistics.level += lu
		val lA = engine.statistics.level
		// Meter
		engine.meterValue = lA%100/99f
		engine.meterColor = GameEngine.METER_COLOR_LEVEL
		// 速度変更
		setSpeed(engine)

		// RE medal
		if(engine.timerActive&&medalRE<3) {
			val blocks = engine.field.howManyBlocks

			if(!recoveryFlag) {
				if(blocks>=150) recoveryFlag = true
			} else if(blocks<=70) {
				recoveryFlag = false
				engine.playSE("medal1")
				medalRE++
			}
		}
		if(lu<=0) return
		// BGM fadeout
		if(tableBGMFadeout.any {it in lb..lA}) owner.musMan.fadeSW = true
		// BGM切り替え
		if(tableBGMChange.any {it in lb..lA}) {
			owner.musMan.fadeSW = false
			owner.musMan.bgm = BGM.GrandM(calcBgmLv(lA))
		}
	}

	/* Calculate score */
	override fun calcScore(engine:GameEngine, ev:ScoreEvent):Int {
		// Combo
		val li = ev.lines
		comboValue = if(li==0) 1
		else maxOf(1, comboValue+2*li-2)

		// RO medal 用カウント
		var spinTemp = engine.nowPieceSpinCount
		if(spinTemp>4) spinTemp = 4
		spinCount += spinTemp

		if(li>=1&&engine.ending==0) {
			// 4-line clearカウント
			if(li>=4)
			// SK medal
				if(big) when(engine.statistics.totalQuadruple) {
					1, 2, 4 -> engine.playSE("medal${++medalSK}")
				} else when(engine.statistics.totalQuadruple) {
					5, 10, 17 -> {
						decTemp += 3+medalSK*2// 3 8 15
						engine.playSE("medal${++medalSK}")
					}
				}

			// AC medal
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
				decTemp += 3// 3
			} else if(engine.combo>=4&&medalCO<2) {
				engine.playSE("medal2")
				medalCO = 2
				decTemp += 4// 7
			} else if(engine.combo>=5&&medalCO<3) {
				engine.playSE("medal3")
				medalCO = 3
				decTemp += 5// 12
			}

			// Level up
			val lb = engine.statistics.level
			levelUp(engine, li+maxOf(0, minOf(2, ev.b2b)))
			if(engine.statistics.level>=999) {
				// Ending
				engine.playSE("endingstart")
				engine.statistics.level = 999
				engine.timerActive = false
				engine.ending = 2

				grade = 2
				gradeFlash = 180

				// Section Timeを記録
				sectionLastTime = sectionTime[lb/100]
				sectionsDone++
				decTemp++
				// ST medal
				stMedalCheck(engine, lb/100)

				owner.musMan.bgm = BGM.Ending(1)
				// RO medal
				roMedalCheck(engine)
			} else if(nextSecLv==500&&engine.statistics.level>=500&&qualify>0
				&&engine.statistics.time>qualify
			) {
				// level500とりカン
				engine.playSE("endingstart")
				engine.statistics.level = 500
				engine.timerActive = false
				engine.ending = 2

				// Section Timeを記録
				sectionLastTime = sectionTime[lb/100]
				sectionsDone++

				// ST medal
				stMedalCheck(engine, lb/100)
			} else if(engine.statistics.level>=nextSecLv) {
				// Next Section
				engine.playSE("levelup")

				// Background切り替え
				owner.bgMan.nextBg = nextSecLv/100

				// Section Timeを記録
				sectionLastTime = sectionTime[lb/100]
				sectionsDone++

				// ST medal
				stMedalCheck(engine, lb/100)

				// RO medal
				if(nextSecLv==300||nextSecLv==700) roMedalCheck(engine)

				// 段位上昇
				if(nextSecLv==500) {
					grade = 1
					gradeFlash = 180
				}

				// Update level for next section
				nextSecLv += 100
				if(nextSecLv>999) nextSecLv = 999
			}

			// Calculate score

			lastScore = ((((lb+li)/(if(engine.b2b) 3 else 4)+engine.softdropFall+if(engine.manualLock) 1 else 0)
				*li*comboValue*bravo)
				+engine.statistics.level/2+maxOf(0, engine.lockDelay-engine.lockDelayNow)*7)
			engine.statistics.scoreLine += lastScore
			return lastScore
		}
		return 0
	}

	/* 各 frame の終わりの処理 */
	override fun onLast(engine:GameEngine) {
		super.onLast(engine)
		// 段位上昇時のフラッシュ
		if(gradeFlash>0) gradeFlash--

		// Section Time増加
		if(engine.timerActive&&engine.ending==0) {
			val section = engine.statistics.level/100

			if(section>=0&&section<sectionTime.size) sectionTime[section] = engine.statistics.time-sectionTime.take(section).sum()
		}

		// Ending
		if(engine.gameActive&&engine.ending==2) {
			rollTime += if(version>=1&&engine.ctrl.isPress(Controller.BUTTON_F))
				5 else 1

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
		} else if(engine.statistics.level==nextSecLv-1)
			engine.meterColor = if(engine.meterColor==-0x1) -0x10000 else -0x1
	}

	/* Called at game over */
	override fun onGameOver(engine:GameEngine):Boolean {
		if(engine.statc[0]==0) {
			secretGrade = engine.field.secretGrade

			val time = engine.statistics.time
			if(time<6000)
				decTemp -= 3
			else {
				decTemp++
				if(time%3600<=60||time%3600>=3540) decTemp++
			}

			if(sectionsDone==0) decTemp -= 4
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
				if(grade>=1&&grade<tableGradeName.size) {
					receiver.drawMenuFont(engine, 0, 3, "GRADE", COLOR.BLUE)
					receiver.drawMenuGrade(engine, 6, 2, tableGradeName[grade], COLOR.ORANGE, 2f)
				}

				drawResultStats(
					engine, receiver, 4, COLOR.BLUE, Statistic.SCORE, Statistic.LINES, Statistic.LEVEL_MANIA, Statistic.TIME
				)
				drawResultRank(engine, receiver, 12, COLOR.BLUE, rankingRank)
				if(secretGrade>4)
					drawResult(
						engine, receiver, 14, COLOR.BLUE, "S. GRADE",
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
				receiver.drawMenuFont(engine, 0, 2, "MEDAL", COLOR.BLUE)
				receiver.drawMenuMedal(engine, 5, 2, "SK", medalSK)
				receiver.drawMenuMedal(engine, 8, 2, "ST", medalST)
				receiver.drawMenuMedal(engine, 1, 3, "AC", medalAC)
				receiver.drawMenuMedal(engine, 4, 3, "CO", medalCO)
				receiver.drawMenuMedal(engine, 7, 3, "RE", medalRE)
				receiver.drawMenuMedal(engine, 8, 4, "RO", medalRO)

				drawResultStats(
					engine, receiver, 5, COLOR.BLUE, Statistic.LPM,
					Statistic.SPM, Statistic.PIECE, Statistic.PPS
				)

				receiver.drawMenuFont(engine, 0, 15, "DECORATION", COLOR.BLUE)
				receiver.drawMenuFont(engine, 0, 16, "%10d".format(decTemp), COLOR.WHITE)
			}
		}
	}

	/* 結果画面の処理 */
	override fun onResult(engine:GameEngine):Boolean {
		owner.musMan.fadeSW = false
		owner.musMan.bgm = BGM.Result(
			if(engine.ending>0)
				if(engine.statistics.level<900) 2 else 3 else 0
		)
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
		owner.replayProp.setProperty("speedmania.version", version)

		// Update rankings
		if(!owner.replayMode&&startLevel==0&&!big&&engine.ai==null) {
			owner.statsProp.setProperty("decoration", decoration)
			updateRanking(grade, engine.statistics.level, engine.statistics.time)
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
			if(sectionIsNewRecord[i]) bestSectionTime[i] = sectionTime[i]
	}

	companion object {
		/** Current version */
		private const val CURRENT_VERSION = 3

		/** ARE table */
		private val tableARE = listOf(16, 12, 10, 8, 6, 4)
		/** ARE after line clear table */
		private val tableARELine = listOf(12, 9, 7, 6, 5, 4)
		/** Line clear times table */
		private val tableLineDelay = listOf(12, 9, 7, 6, 5, 4)
		/** 固定 times table */
		private val tableLockDelay = listOf(30, 28, 26, 24, 22, 20)
		/** DAS table */
		private val tableDAS = listOf(12, 10, 8, 6, 5, 4)
		/** BGM fadeout levels */
		private val tableBGMFadeout = listOf(280, 480, -1)
		/** BGM change levels */
		private val tableBGMChange = listOf(300, 500, 800)
		/** 段位のName */
		private val tableGradeName = listOf("", "m", "Gm", "GM")
		/** 裏段位のName */
		private val tableSecretGradeName =
			listOf("S1", "S2", "S3", "S4", "S5", "S6", "S7", "S8", "S9", "M1", "M2", "M3", "M4", "M5", "M6", "M7", "M8", "M9", "GM")

		/** LV999 roll time */
		private const val ROLLTIMELIMIT = 1982
		/** Number of entries in rankings */
		private const val RANKING_MAX = 13
		/** Number of sections */
		private const val SECTION_MAX = 10
		/** Default section time */
		private const val DEFAULT_SECTION_TIME = 2520
	}
}
