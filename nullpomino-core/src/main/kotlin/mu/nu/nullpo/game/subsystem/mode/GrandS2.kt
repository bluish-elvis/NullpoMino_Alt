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
import mu.nu.nullpo.game.component.Block
import mu.nu.nullpo.game.component.Controller
import mu.nu.nullpo.game.event.EventReceiver.COLOR
import mu.nu.nullpo.game.event.ScoreEvent
import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.game.subsystem.mode.menu.*
import mu.nu.nullpo.gui.common.BaseFont
import mu.nu.nullpo.util.CustomProperties
import mu.nu.nullpo.util.GeneralUtil.toInt
import mu.nu.nullpo.util.GeneralUtil.toTimeStr

/** SPEED MANIA 2 Mode */
class GrandS2:AbstractGrand() {
	/** Number of sections */
	override val sectionMax = 13

	override val medalSKQuads = listOf(listOf(5, 10, 17, 25), listOf(1, 2, 4, 6))

	/** 最終結果などに表示される実際の段位 */
	private var grade = 0

	/** 段位表示を光らせる残り frame count */
	private var gradeFlash = 0

	/** Combo bonus */
	private var comboValue = 0

	/** Roll 経過 time */
	private var rollTime = 0

	/** Roll started flag */
	private var rollStarted = false

	/** Roll completely cleared flag */
	private var rollClear = 0

	/** せり上がりまでのBlock count */
	private var garbageCount = 0

	/** REGRET display time frame count */
	private var regretDispFrame = 0

	/** 裏段位 */
	private var secretGrade = 0

	/** Section Time記録表示中ならtrue */
	private var isShowBestSectionTime = false

	private val itemLevel = LevelGrandMenuItem(COLOR.BLUE, sectionMax)
	/** Level at start */
	private var startLevel:Int by DelegateMenuItem(itemLevel)

	/** LV500の足切りTime */
	private val itemQualify = TimeMenuItem("lv500torikan", "QUALIFY", COLOR.BLUE, 12300, 0..72000)
	private var qualify:Int by DelegateMenuItem(itemQualify)

	private val itemBig = BooleanMenuItem("big", "BIG", COLOR.BLUE, false)
	/** BigMode */
	private var big:Boolean by DelegateMenuItem(itemBig)

	private var itemGrade = BooleanMenuItem("showgrade", "SHOW GRADE", COLOR.BLUE, false)
	/** 段位表示 */
	private var gradeDisp:Boolean by DelegateMenuItem(itemGrade)

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
	private val bestSectionTime = MutableList(sectionMax) {tableTimeRegret[minOf(it, tableTimeRegret.lastIndex)]}

	/* Mode name */
	override val name = "Grand Lightning"
	override val gameIntensity = 3
	/* Initialization */
	override val menu = MenuList("speedmania2", itemLevel, itemQualify, itemAlert, itemST, itemBig)

	override val propRank
		get() = rankMapOf(
			"grade" to rankingGrade, "level" to rankingLevel, "time" to rankingTime, "rollClear" to rankingRollClear,
			"section.time" to bestSectionTime
		)

	override fun playerInit(engine:GameEngine) {
		super.playerInit(engine)
		grade = 0
		gradeFlash = 0
		comboValue = 0
		lastScore = 0
		rollTime = 0
		rollStarted = false
		rollClear = 0
		garbageCount = 0
		regretDispFrame = 0
		secretGrade = 0

		rankingRank = -1
		rankingGrade.fill(0)
		rankingLevel.fill(0)
		rankingTime.fill(-1)
		rankingRollClear.fill(0)
		bestSectionTime.forEachIndexed {i, _ ->
			bestSectionTime[i] = tableTimeRegret[minOf(i, tableTimeRegret.lastIndex)]
		}

		engine.twistEnable = true
		engine.b2bEnable = true
		engine.splitB2B = true
		engine.comboType = GameEngine.COMBO_TYPE_DOUBLE
		engine.frameColor = GameEngine.FRAME_COLOR_RED
		engine.bigHalf = true
		engine.bigMove = true
		engine.staffrollEnable = true
		engine.staffrollNoDeath = false

		if(!owner.replayMode) {
			version = CURRENT_VERSION
			qualify =
				owner.modeConfig.getProperty(
					"speedmania2.torikan.${engine.ruleOpt.strRuleName}",
					if(engine.ruleOpt.lockResetMove) DEFAULT_TORIKAN else DEFAULT_TORIKAN_CLASSIC
				)
		} else {
			version = owner.replayProp.getProperty("speedmania2.version", 0)
			System.arraycopy(tableTimeRegret, 0, bestSectionTime, 0, sectionMax)
		}

		owner.bgMan.bg = 20+startLevel
	}

	/** Set BGM at start of game
	 */
	private fun calcBgmLv(lv:Int) = tableBGMChange.count {lv>=it}

	/** Update falling speed
	 * @param engine GameEngine
	 */
	override fun setSpeed(engine:GameEngine) {
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
	private fun stMedalCheck(engine:GameEngine, section:Int) =
		stMedalCheck(engine, section, sectionTime[section], bestSectionTime[section])

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

	/* Called at game start */
	override fun startGame(engine:GameEngine) {
		val lv = startLevel*100
		engine.statistics.level = lv

		nextSecLv = (lv+100).coerceIn(100, 1300)
		if(engine.statistics.level>=1000) engine.bone = true

		owner.bgMan.bg = 20+engine.statistics.level/100

		engine.big = big

		setSpeed(engine)
		owner.musMan.bgm = BGM.GrandTS(calcBgmLv(lv))
	}

	override fun renderFirst(engine:GameEngine) {
		if(engine.gameActive&&engine.ending==2) receiver.drawStaffRoll(engine, rollTime*1f/ROLLTIMELIMIT)
	}
	/* Render score */
	override fun renderLast(engine:GameEngine) {
		receiver.drawScoreFont(engine, 0, 0, name, COLOR.RED)

		receiver.drawScoreFont(engine, -1, -4*2, "DECORATION", scale = .5f)
		receiver.drawScoreBadges(engine, 0, -3, 100, decoration)
		receiver.drawScoreBadges(engine, 5, -4, 100, decTemp)
		if(engine.stat==GameEngine.Status.SETTING||engine.stat==GameEngine.Status.RESULT&&!owner.replayMode) {
			if(!owner.replayMode&&startLevel==0&&!big&&engine.ai==null)
				if(!isShowBestSectionTime) {
					// Rankings
					val topY = if(receiver.nextDisplayType==2) 5 else 3
					receiver.drawScoreFont(engine, 0, topY-1, "GRADE LV TIME", COLOR.RED)

					for(i in 0..<rankingMax) {
						receiver.drawScoreGrade(engine, 0, topY+i, "%02d".format(i+1), COLOR.YELLOW)
						receiver.drawScoreGrade(
							engine, 2, topY+i, tableGradeName[rankingGrade[i]], if(rankingRollClear[i]==1) COLOR.GREEN
							else if(rankingRollClear[i]==2) COLOR.ORANGE else COLOR.WHITE
						)
						receiver.drawScoreNum(engine, 5, topY+i, "%03d".format(rankingLevel[i]), i==rankingRank)
						receiver.drawScoreNum(engine, 8, topY+i, rankingTime[i].toTimeStr, i==rankingRank)
					}

					receiver.drawScoreFont(engine, 0, 20, "F:VIEW SECTION TIME", COLOR.GREEN)
				} else {
					// Section Time
					receiver.drawScoreFont(engine, 0, 2, "SECTION TIME", COLOR.RED)

					val totalTime = (0..<sectionMax).fold(0) {tt, i ->
						val slv = i*100
						receiver.drawScoreNum(
							engine, 0, 3+i, "%4d-%4d %s".format(slv, slv+99, bestSectionTime[i].toTimeStr),
							sectionIsNewRecord[i]
						)
						tt+bestSectionTime[i]
					}

					receiver.drawScoreFont(engine, 0, 17, "TOTAL", COLOR.RED)
					receiver.drawScoreNum(engine, 0, 18, totalTime.toTimeStr, 2f)
					receiver.drawScoreFont(engine, 9, 17, "AVERAGE", COLOR.RED)
					receiver.drawScoreNum(engine, 9, 18, (totalTime/sectionMax).toTimeStr, 2f)

					receiver.drawScoreFont(engine, 0, 20, "F:VIEW RANKING", COLOR.GREEN)
				}
		} else {
			if(gradeDisp) {
				// 段位
				if(grade>=0&&grade<tableGradeName.size)
					receiver.drawScoreGrade(engine, 0, 1, tableGradeName[grade], gradeFlash>0&&gradeFlash%4==0, 2f)

				// Score
				receiver.drawScoreFont(engine, 0, 6, "Score", COLOR.RED)
				receiver.drawScoreNum(engine, 5, 6, "+$lastScore")
				receiver.drawScoreNum(engine, 0, 7, "$scDisp", 2f)
			}

			// level
			receiver.drawScoreFont(engine, 0, 9, "Level", COLOR.RED)
			receiver.drawScoreNum(engine, 1, 10, "%3d".format(maxOf(engine.statistics.level, 0)))
			receiver.drawScoreSpeed(engine, 0, 11, if(engine.speed.gravity<0) 40 else engine.speed.gravity/128, 4)
			receiver.drawScoreNum(engine, 1, 12, "%3d".format(nextSecLv))

			// Time
			receiver.drawScoreFont(engine, 0, 14, "Time", COLOR.RED)
			if((engine.ending!=2) or (rollTime/10%2==0))
				receiver.drawScoreNum(engine, 0, 15, engine.statistics.time.toTimeStr, 2f)

			// Roll 残り time
			if(engine.gameActive&&engine.ending==2) {
				var time = ROLLTIMELIMIT-rollTime
				if(time<0) time = 0
				receiver.drawScoreFont(engine, 0, 17, "ROLL TIME", COLOR.RED)
				receiver.drawScoreNum(engine, 0, 18, time.toTimeStr, time>0&&time<10*60, 2f)
			}

			// REGRET表示
			if(regretDispFrame>0)
				receiver.drawMenuFont(
					engine, 2, 21, "REGRET", when {
						regretDispFrame%4==0 -> COLOR.YELLOW;regretDispFrame%4==2 -> COLOR.RED;else -> COLOR.ORANGE
					}
				)

			// medal
			receiver.drawScoreMedal(engine, 0, 20, "AC", medalAC)
			receiver.drawScoreMedal(engine, 3, 20, "ST", medalST)
			receiver.drawScoreMedal(engine, 0, 21, "SK", medalSK)
			receiver.drawScoreMedal(engine, 3, 21, "CO", medalCO)
			// Section Time
			if(showST&&sectionTime.isNotEmpty()) {
				val y = if(receiver.nextDisplayType==2) 4 else 2
				val x = if(receiver.nextDisplayType==2) 20 else 12
				val x2 = if(receiver.nextDisplayType==2) 9 else 12

				receiver.drawScoreFont(engine, x, y, "SECTION TIME", COLOR.RED)
				val section = engine.statistics.level/100

				sectionTime.forEachIndexed {i, it ->
					if(it>0) receiver.drawScoreNum(
						engine, x-1, y+1+i, "%4d%s%s".format(i*100, if(i==section&&engine.ending==0) "+" else "-", it.toTimeStr),
						sectionIsNewRecord[i]
					)
				}
				receiver.drawScoreFont(engine, x2, 17, "AVERAGE", COLOR.RED)
				receiver.drawScoreNum(
					engine, x2, 18, (engine.statistics.time/(sectionsDone+(engine.ending==0).toInt())).toTimeStr,
					2f
				)
			}
		}
	}

	/* 移動中の処理 */
	override fun onMove(engine:GameEngine):Boolean {
		super.onMove(engine)

		if(engine.ending==0&&engine.statc[0]==0&&!engine.holdDisable) {
			// せり上がりカウント
			if(tableGarbage[engine.statistics.level/100]!=0) garbageCount++

			// せり上がり
			if(garbageCount>=tableGarbage[engine.statistics.level/100]&&tableGarbage[engine.statistics.level/100]!=0) {
				engine.playSE("garbage0")
				engine.field.addBottomCopyGarbage(
					engine.skin, 1,
					Block.ATTRIBUTE.GARBAGE, Block.ATTRIBUTE.VISIBLE, Block.ATTRIBUTE.OUTLINE
				)
				garbageCount = 0
			}
		}

		// Endingスタート
		if(engine.ending==2&&!rollStarted) {
			rollStarted = true
			engine.big = true
			owner.musMan.bgm = BGM.Ending(2)
		}

		return false
	}

	/** levelが上がったときの共通処理 */
	override fun levelUp(engine:GameEngine, lu:Int) {
		val lb = engine.statistics.level
		super.levelUp(engine, lu)
		val lA = engine.statistics.level

		if(lu<=0) return
		// BGM fadeout
		if(tableBGMFadeout.any {it in lb..lA}) owner.musMan.fadeSW = true
		// BGM切り替え
		if(tableBGMChange.any {it in lb..lA}) {
			owner.musMan.fadeSW = false
			owner.musMan.bgm = BGM.GrandTS(calcBgmLv(lA))
		}
	}

	/* Calculate score */
	override fun calcScore(engine:GameEngine, ev:ScoreEvent):Int {
		// Combo
		val li = ev.lines
		// Calculate score
		val pts = super.calcScore(engine, ev)

		if(li>=1&&engine.ending==0) {

			// せり上がりカウント減少
			if(tableGarbage[engine.statistics.level/100]!=0) garbageCount -= li
			if(garbageCount<0) garbageCount = 0

			// Level up
			val levelb = engine.statistics.level
			val section = levelb/100
			val isRegret = sectionTime[section]>tableTimeRegret[section]
			levelUp(engine, li.let {it+maxOf(0, it-2)})

			if(engine.statistics.level>=1300) {
				// Ending
				engine.playSE("endingstart")
				engine.statistics.level = 1300
				engine.timerActive = false
				engine.ending = 1
				rollClear = 1

				sectionsDone++
				decTemp++
				// ST medal
				stMedalCheck(engine, section)

				if(isRegret) {
					// REGRET判定
					regretDispFrame = 180
					engine.playSE("regret")
				} else {
					// 段位上昇
					grade++
					if(grade>13) grade = 13
					gradeFlash = 180
				}
			} else if(nextSecLv==500&&engine.statistics.level>=500&&qualify>0&&engine.statistics.time>qualify||nextSecLv==1000&&engine.statistics.level>=1000&&qualify>0&&engine.statistics.time>qualify*2) {
				// level500/1000とりカン
				engine.playSE("endingstart")

				if(nextSecLv==500) engine.statistics.level = 500
				if(nextSecLv==1000) engine.statistics.level = 1000

				engine.gameEnded()
				engine.staffrollEnable = false
				engine.ending = 1

				secretGrade = engine.field.secretGrade

				sectionsDone++
				// ST medal
				stMedalCheck(engine, section)

				if(isRegret) {
					// REGRET判定
					regretDispFrame = 180
					engine.playSE("regret")
				} else {
					// 段位上昇
					grade++
					if(grade>13) grade = 13
					gradeFlash = 180
				}
			} else if(engine.statistics.level>=nextSecLv) {
				// Next Section
				engine.playSE("levelup")

				// Background切り替え
				owner.bgMan.nextBg = 20+nextSecLv/100

				sectionsDone++
				// ST medal
				stMedalCheck(engine, section)

				// 骨Block出現開始
				if(engine.statistics.level>=1000) engine.bone = true

				// Update level for next section
				nextSecLv += 100

				if(isRegret) {
					// REGRET判定
					regretDispFrame = 180
					engine.playSE("regret")
				} else {
					// 段位上昇
					grade++
					if(grade>13) grade = 13
					gradeFlash = 180
				}
			}
			lastScore = pts
			engine.statistics.scoreLine += pts
			return pts
		}
		return 0
	}

	/* 各 frame の終わりの処理 */
	override fun onLast(engine:GameEngine) {
		super.onLast(engine)
		// 段位上昇時のフラッシュ
		if(gradeFlash>0) gradeFlash--

		// REGRET表示
		if(regretDispFrame>0) regretDispFrame--

		// Section Time増加
		if(engine.timerActive&&engine.ending==0) {
			val section = engine.statistics.level/100

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
				secretGrade = engine.field.secretGrade
				rollClear = 2
				engine.gameEnded()
				engine.resetStatc()
				engine.stat = GameEngine.Status.EXCELLENT
			}
		} else if(engine.statistics.level==nextSecLv-1)
			engine.meterColor = if(engine.meterColor==-0x1)
				-0x10000
			else
				-0x1
	}

	/* Called at game over */
	override fun onGameOver(engine:GameEngine):Boolean {
		if(engine.statc[0]==0&&engine.gameActive) {
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
				var gcolor = COLOR.WHITE
				if(rollClear==1||rollClear==3) gcolor = COLOR.GREEN
				if(rollClear==2||rollClear==4) gcolor = COLOR.ORANGE
				receiver.drawMenuFont(engine, 0, 2, "GRADE", COLOR.RED)
				receiver.drawMenuGrade(engine, 0, 1.66f, tableGradeName[grade], gcolor, 2f)

				drawResultStats(
					engine, receiver, 4, COLOR.RED, Statistic.SCORE, Statistic.LINES, Statistic.LEVEL_MANIA, Statistic.TIME
				)
				drawResultRank(engine, receiver, 12, COLOR.RED, rankingRank)
				if(secretGrade>4)
					drawResult(
						engine, receiver, 14, COLOR.RED, "S. GRADE",
						"%10s".format(tableSecretGradeName[secretGrade-1])
					)
			}
			1 -> {
				receiver.drawMenuFont(engine, 0, 2, "SECTION", COLOR.RED)

				for(i in sectionTime.indices)
					if(sectionTime[i]>0)
						receiver.drawMenuFont(engine, 2, 3+i, sectionTime[i].toTimeStr, sectionIsNewRecord[i])

				if(sectionAvgTime>0) {
					receiver.drawMenuFont(engine, 0, 16, "AVERAGE", COLOR.RED)
					receiver.drawMenuFont(engine, 2, 17, sectionAvgTime.toTimeStr)
				}
			}
			2 -> {
				receiver.drawMenuNano(engine, 0, 1.5f, "MEDAL", COLOR.RED, .5f)
				receiver.drawMenuMedal(engine, 2, 2, "AC", medalAC)
				receiver.drawMenuMedal(engine, 5, 2, "ST", medalST)
				receiver.drawMenuMedal(engine, 8, 2, "SK", medalSK)
				receiver.drawMenuMedal(engine, 1, 3, "RE", medalRE)
				receiver.drawMenuMedal(engine, 4, 3, "RO", medalRO)
				receiver.drawMenuMedal(engine, 7, 3, "CO", medalCO)

				drawResultStats(engine, receiver, 4, COLOR.RED, Statistic.LPM, Statistic.SPM, Statistic.PIECE, Statistic.PPS)

				receiver.drawMenuFont(engine, 0, 15, "DECORATION", COLOR.RED)
				receiver.drawMenuFont(engine, 0, 16, "%10d".format(decTemp), COLOR.WHITE)
			}
		}
	}

	/* 結果画面の処理 */
	override fun onResult(engine:GameEngine):Boolean {
		owner.musMan.fadeSW = false
		owner.musMan.bgm = when {
			engine.ending==0 -> BGM.Result(0)
			engine.ending==2&&rollClear>0 -> BGM.Result(3)
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
		owner.replayProp.setProperty("speedmania2.version", version)

		// Update rankings
		if(!owner.replayMode&&startLevel==0&&!big&&engine.ai==null) {
			owner.statsProp.setProperty("decoration", decoration)
			updateRanking(grade, engine.statistics.level, engine.statistics.time, rollClear)
			if(medalST==3) updateBestSectionTime()

			if(rankingRank!=-1||medalST==3) return true
		}
		return false
	}

	/** Update rankings
	 * @param gr 段位
	 * @param lv level
	 * @param time Time
	 * @param clear Roll クリア flag
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
	 * @param clear Roll クリア flag
	 * @return Position (-1 if unranked)
	 */
	private fun checkRanking(gr:Int, lv:Int, time:Int, clear:Int):Int {
		for(i in 0..<rankingMax)
			if(clear>rankingRollClear[i])
				return i
			else if(clear==rankingRollClear[i]&&gr>rankingGrade[i])
				return i
			else if(clear==rankingRollClear[i]&&gr==rankingGrade[i]&&lv>rankingLevel[i])
				return i
			else if(clear==rankingRollClear[i]&&gr==rankingGrade[i]&&lv==rankingLevel[i]&&time<rankingTime[i]) return i

		return -1
	}

	/** Update best section time records */
	private fun updateBestSectionTime() {
		for(i in 0..<sectionMax)
			if(sectionIsNewRecord[i]) bestSectionTime[i] = sectionTime[i]
	}

	companion object {
		/** Current version */
		private const val CURRENT_VERSION = 2

		/** Default torikan time for non-classic rules */
		private const val DEFAULT_TORIKAN = 12300

		/** Default torikan time for classic rules */
		private const val DEFAULT_TORIKAN_CLASSIC = 8880

		/** ARE table */
		private val tableARE = listOf(16, 12, 11, 10, 9, 8, 7, 6, 5, 4, 3, 2, 2, 2)
		/** ARE after line clear table */
		private val tableARELine = listOf(6, 6, 5, 5, 4, 4, 3, 3, 2, 2, 1, 1, 1, 2)
		/** Line clear times table */
		private val tableLineDelay = listOf(7, 6, 6, 5, 5, 4, 4, 3, 3, 2, 2, 2, 1, 6)
		/** 固定 times table */
		private val tableLockDelay = listOf(30, 29, 28, 27, 26, 25, 24, 23, 22, 21, 20, 19, 18, 20)
		/** DAS table */
		private val tableDAS = listOf(9, 8, 7, 6, 5, 4, 4, 4, 4, 4, 4, 4, 4, 5)

		/** せり上がり間隔 */
		private val tableGarbage = listOf(50, 33, 27, 25, 21, 18, 15, 12, 9, 7, 0, 0, 0, 0)

		/** REGRET criteria Time */
		private val tableTimeRegret =
			listOf(6000, 5800, 5600, 5400, 5200, 5000, 4750, 4500, 4250, 4000, 3750, 3500, 3250, 3000)

		/** BGM fadeout levels */
		private val tableBGMFadeout = listOf(485, 685, 985)
		/** BGM change levels */
		private val tableBGMChange = listOf(500, 700, 1000)
		private val tableBGM = listOf(BGM.GrandT(2), BGM.GrandT(3), BGM.GrandT(4), BGM.GrandT(5))
		/** 段位のName */
		private val tableGradeName =
			listOf("1", "S1", "S2", "S3", "S4", "S5", "S6", "S7", "S8", "S9", "S10", "S11", "S12", "S13")

		/** 裏段位のName */
		private val tableSecretGradeName =
			listOf("S1", "S2", "S3", "S4", "S5", "S6", "S7", "S8", "S9", "M1", "M2", "M3", "M4", "M5", "M6", "M7", "M8", "M9", "GM")

		/** LV999 roll time */
		private const val ROLLTIMELIMIT = 3238
	}
}
