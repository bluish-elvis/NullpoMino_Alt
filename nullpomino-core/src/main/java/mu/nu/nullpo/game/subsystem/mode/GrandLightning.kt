/*
 * Copyright (c) 2010-2021, NullNoname
 * Kotlin converted and modified by Venom=Nhelv
 * All rights reserved.
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
import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.game.subsystem.mode.menu.*
import mu.nu.nullpo.util.CustomProperties
import mu.nu.nullpo.util.GeneralUtil.toInt
import mu.nu.nullpo.util.GeneralUtil.toTimeStr

/** SPEED MANIA 2 Mode */
class GrandLightning:AbstractMode() {

	/** Default section time */
	// private static final int DEFAULT_SECTION_TIME = 2520;

	/** Next Section の level (これ-1のときに levelストップする) */
	private var nextseclv = 0

	/** Levelが増えた flag */
	private var lvupflag = false

	/** 最終結果などに表示される実際の段位 */
	private var grade = 0

	/** 段位表示を光らせる残り frame count */
	private var gradeflash = 0

	/** Combo bonus */
	private var comboValue = 0

	/** Roll 経過 time */
	private var rolltime = 0

	/** Roll started flag */
	private var rollstarted = false

	/** Roll completely cleared flag */
	private var rollclear = 0

	/** せり上がりまでのBlockcount */
	private var garbageCount = 0

	/** REGRET display time frame count */
	private var regretdispframe = 0

	/** 裏段位 */
	private var secretGrade = 0

	/** Current BGM */
	private var bgmlv = 0

	/** Section Time */
	private var sectionTime = IntArray(SECTION_MAX)

	/** 新記録が出たSection はtrue */
	private var sectionIsNewRecord = BooleanArray(SECTION_MAX)

	/** Cleared Section count */
	private var sectionscomp = 0

	/** Average Section Time */
	private var sectionavgtime = 0

	/** 直前のSection Time */
	private var sectionlasttime = 0

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

	/** LV500の足切りTime */
	private val itemQualify = TimeMenuItem("lv500torikan", "QUALIFY", COLOR.BLUE, 12300, 0.. 36000)
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

	/** 段位表示 */
	private var gradedisp = false

	/** Version */
	private var version = 0

	/** Current round's ranking rank */
	private var rankingRank = 0

	/** Rankings' 段位 */
	private var rankingGrade = IntArray(RANKING_MAX)

	/** Rankings' level */
	private var rankingLevel = IntArray(RANKING_MAX)

	/** Rankings' times */
	private var rankingTime = IntArray(RANKING_MAX)

	/** Rankings' Roll completely cleared flag */
	private var rankingRollclear = IntArray(RANKING_MAX)

	/** Section Time記録 */
	private var bestSectionTime = IntArray(SECTION_MAX)

	private var decoration = 0
	private var dectemp = 0

	/* Mode name */
	override val name = "Grand Lightning"
	override val gameIntensity = 3
	/* Initialization */
	override val menu:MenuList = MenuList("speedmania2", itemLevel, itemQualify, itemAlert, itemST, itemBig)

	override val rankMap:Map<String, IntArray>
		get() = mapOf("grade" to rankingGrade, "level" to rankingLevel, "time" to rankingTime, "rollclear" to rankingRollclear,
		"section.time" to bestSectionTime)

	override fun playerInit(engine:GameEngine, playerID:Int) {
		super.playerInit(engine, playerID)
		menuTime = 0
		nextseclv = 0
		lvupflag = true
		grade = 0
		gradeflash = 0
		comboValue = 0
		lastscore = 0
		rolltime = 0
		rollstarted = false
		rollclear = 0
		garbageCount = 0
		regretdispframe = 0
		secretGrade = 0
		bgmlv = 0
		sectionTime = IntArray(SECTION_MAX)
		sectionIsNewRecord = BooleanArray(SECTION_MAX)
		sectionscomp = 0
		sectionavgtime = 0
		sectionlasttime = 0
		medalAC = 0
		medalST = 0
		medalSK = 0
		medalCO = 0
		isShowBestSectionTime = false
		startLevel = 0
		secAlert = false
		big = false
		qualify = DEFAULT_TORIKAN
		showST = false
		gradedisp = false
		decoration = 0
		dectemp = 0

		rankingRank = -1
		rankingGrade = IntArray(RANKING_MAX)
		rankingLevel = IntArray(RANKING_MAX)
		rankingTime = IntArray(RANKING_MAX)
		rankingRollclear = IntArray(RANKING_MAX)
		bestSectionTime = IntArray(SECTION_MAX)

		engine.twistEnable = true
		engine.b2bEnable = true
		engine.splitb2b = true
		engine.comboType = GameEngine.COMBO_TYPE_DOUBLE
		engine.framecolor = GameEngine.FRAME_COLOR_RED
		engine.bighalf = true
		engine.bigmove = true
		engine.staffrollEnable = true
		engine.staffrollNoDeath = false

		if(!owner.replayMode) {
			version = CURRENT_VERSION
			qualify =
				owner.modeConfig.getProperty("speedmania2.torikan.${engine.ruleOpt.strRuleName}",
					if(engine.ruleOpt.lockresetMove) DEFAULT_TORIKAN else DEFAULT_TORIKAN_CLASSIC)
		} else {
			version = owner.replayProp.getProperty("speedmania2.version", 0)
			System.arraycopy(tableTimeRegret, 0, bestSectionTime, 0, SECTION_MAX)
		}

		owner.backgroundStatus.bg = 20+startLevel
	}

	/*
	override fun loadSetting(prop:CustomProperties, ruleName:String, playerID:Int) {
		startLevel = prop.getProperty("speedmania2.startLevel", 0)
		secAlert = prop.getProperty("speedmania2.lvstopse", true)
		showST = prop.getProperty("speedmania2.showsectiontime", true)
		big = prop.getProperty("speedmania2.big", false)
		gradedisp = prop.getProperty("speedmania2.gradedisp", false)
	}

	override fun saveSetting(prop:CustomProperties, ruleName:String, playerID:Int) {
		prop.setProperty("speedmania2.startLevel", startLevel)
		prop.setProperty("speedmania2.lvstopse", secAlert)
		prop.setProperty("speedmania2.showsectiontime", showST)
		prop.setProperty("speedmania2.big", big)
		prop.setProperty("speedmania2.gradedisp", gradedisp)
	}
	private fun saveSetting(prop:CustomProperties, strRuleName:String) {
		saveSetting(prop)
		prop.setProperty("speedmania2.torikan.$strRuleName", qualify)
	}*/

	/** Set BGM at start of game
	 * @param engine GameEngine
	 */
	private fun setStartBgmlv(engine:GameEngine) {
		bgmlv = tableBGMChange.count {engine.statistics.level>=it}
		while(tableBGMChange[bgmlv]!=-1&&engine.statistics.level>=tableBGMChange[bgmlv])
			bgmlv++
	}

	/** Update falling speed
	 * @param engine GameEngine
	 */
	private fun setSpeed(engine:GameEngine) {
		engine.speed.gravity = -1

		var section = engine.statistics.level/100
		if(section>tableARE.size-1) section = tableARE.size-1
		engine.speed.are = tableARE[section]
		engine.speed.areLine = tableARELine[section]
		engine.speed.lineDelay = tableLineDelay[section]
		engine.speed.lockDelay = tableLockDelay[section]
		engine.speed.das = tableDAS[section]
	}

	/** Update average section time */
	private fun setAverageSectionTime() {
		if(sectionscomp>0) {
			var temp = 0
			for(i in startLevel until startLevel+sectionscomp)
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
						startLevel += change
						if(startLevel<0) startLevel = 12
						if(startLevel>12) startLevel = 0
						owner.backgroundStatus.bg = 20+startLevel
					}
					1 -> secAlert = !secAlert
					2 -> showST = !showST
					3 -> big = !big
					4 -> {
						qualify += 60*change
						if(qualify<0) qualify = 72000
						if(qualify>72000) qualify = 0
					}
					5 -> gradedisp = !gradedisp
				}
			}

			// section time display切替
			if(engine.ctrl.isPush(Controller.BUTTON_F)&&menuTime>=5) {
				engine.playSE("change")
				isShowBestSectionTime = !isShowBestSectionTime
			}

			// 決定
			if(engine.ctrl.isPush(Controller.BUTTON_A)&&menuTime>=5) {
				engine.playSE("decide")

				sectionscomp = 0

				return false
			}

			// Cancel
			if(engine.ctrl.isPush(Controller.BUTTON_B)) engine.quitflag = true

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
		drawMenu(engine, playerID, receiver, 0, COLOR.RED, 0, "Level" to (startLevel*100),
			"LVSTOPSE" to secAlert, "SHOW STIME" to showST, "BIG" to big,
			"LV500LIMIT" to if(qualify==0) "NONE" else qualify.toTimeStr, "GRADE DISP" to gradedisp)
	}

	/* Called at game start */
	override fun startGame(engine:GameEngine, playerID:Int) {
		engine.statistics.level = startLevel*100

		nextseclv = engine.statistics.level+100
		if(engine.statistics.level<0) nextseclv = 100
		if(engine.statistics.level>=1300) nextseclv = 1300
		if(engine.statistics.level>=1000) engine.bone = true

		owner.backgroundStatus.bg = 20+engine.statistics.level/100

		engine.big = big

		setSpeed(engine)
		setStartBgmlv(engine)
		owner.bgmStatus.bgm = tableBGM[bgmlv]
	}

	/* Render score */
	override fun renderLast(engine:GameEngine, playerID:Int) {
		receiver.drawScoreFont(engine, playerID, 0, 0, "SPEED MANIA 2", COLOR.RED)

		receiver.drawScoreFont(engine, playerID, -1, -4*2, "DECORATION", scale = .5f)
		receiver.drawScoreBadges(engine, playerID, 0, -3, 100, decoration)
		receiver.drawScoreBadges(engine, playerID, 5, -4, 100, dectemp)
		if(engine.stat==GameEngine.Status.SETTING||engine.stat==GameEngine.Status.RESULT&&!owner.replayMode) {
			if(!owner.replayMode&&startLevel==0&&!big&&engine.ai==null)
				if(!isShowBestSectionTime) {
					// Rankings
					val scale = if(receiver.nextDisplayType==2) .5f else 1f
					val topY = if(receiver.nextDisplayType==2) 5 else 3
					receiver.drawScoreFont(engine, playerID, 3, topY-1, "GRADE LEVEL TIME", COLOR.RED, scale)

					for(i in 0 until RANKING_MAX) {
						var gcolor = COLOR.WHITE
						if(rankingRollclear[i]==1) gcolor = COLOR.GREEN
						if(rankingRollclear[i]==2) gcolor = COLOR.ORANGE

						receiver.drawScoreNum(engine, playerID, 0, topY+i, String.format("%02d", i+1), COLOR.YELLOW, scale)
						receiver.drawScoreGrade(engine, playerID, 3, topY+i, tableGradeName[rankingGrade[i]], gcolor, scale)
						receiver.drawScoreNum(engine, playerID, 9, topY+i, String.format("%03d", rankingLevel[i]), i==rankingRank, scale)
						receiver.drawScoreNum(engine, playerID, 15, topY+i, rankingTime[i].toTimeStr, i==rankingRank, scale)
					}

					receiver.drawScoreFont(engine, playerID, 0, 20, "F:VIEW SECTION TIME", COLOR.GREEN)
				} else {
					// Section Time
					receiver.drawScoreFont(engine, playerID, 0, 2, "SECTION TIME", COLOR.RED)

					var totalTime = 0
					for(i in 0 until SECTION_MAX) {
						val temp = i*100
						val temp2 = (i+1)*100-1

						val strSectionTime:String = String.format("%4d-%4d %s", temp, temp2, bestSectionTime[i].toTimeStr)

						receiver.drawScoreNum(engine, playerID, 0, 3+i, strSectionTime, sectionIsNewRecord[i])

						totalTime += bestSectionTime[i]
					}

					receiver.drawScoreFont(engine, playerID, 0, 17, "TOTAL", COLOR.RED)
					receiver.drawScoreNum(engine, playerID, 0, 18, totalTime.toTimeStr, 2f)
					receiver.drawScoreFont(engine, playerID, 9, 17, "AVERAGE", COLOR.RED)
					receiver.drawScoreNum(engine, playerID, 9, 18, (totalTime/SECTION_MAX).toTimeStr, 2f)

					receiver.drawScoreFont(engine, playerID, 0, 20, "F:VIEW RANKING", COLOR.GREEN)
				}
		} else {
			if(gradedisp) {
				// 段位
				if(grade>=0&&grade<tableGradeName.size)
					receiver.drawScoreGrade(engine, playerID, 0, 1, tableGradeName[grade], gradeflash>0&&gradeflash%4==0, 2f)

				// Score
				receiver.drawScoreFont(engine, playerID, 0, 6, "Score", COLOR.RED)
				receiver.drawScoreNum(engine, playerID, 5, 6, "+$lastscore")
				receiver.drawScoreNum(engine, playerID, 0, 7, "$scDisp", 2f)
			}

			// level
			receiver.drawScoreFont(engine, playerID, 0, 9, "Level", COLOR.RED)
			receiver.drawScoreNum(engine, playerID, 1, 10, String.format("%3d", maxOf(engine.statistics.level, 0)))
			receiver.drawSpeedMeter(engine, playerID, 0, 11, if(engine.speed.gravity<0) 40 else engine.speed.gravity/128, 4)
			receiver.drawScoreNum(engine, playerID, 1, 12, String.format("%3d", nextseclv))

			// Time
			receiver.drawScoreFont(engine, playerID, 0, 14, "Time", COLOR.RED)
			if((engine.ending!=2) or (rolltime/10%2==0))
				receiver.drawScoreNum(engine, playerID, 0, 15, engine.statistics.time.toTimeStr, 2f)

			// Roll 残り time
			if(engine.gameActive&&engine.ending==2) {
				var time = ROLLTIMELIMIT-rolltime
				if(time<0) time = 0
				receiver.drawScoreFont(engine, playerID, 0, 17, "ROLL TIME", COLOR.RED)
				receiver.drawScoreNum(engine, playerID, 0, 18, time.toTimeStr, time>0&&time<10*60, 2f)
			}

			// REGRET表示
			if(regretdispframe>0)
				receiver.drawMenuFont(engine, playerID, 2, 21, "REGRET", when {
					regretdispframe%4==0 -> COLOR.YELLOW
					regretdispframe%4==2 -> COLOR.RED
					else -> COLOR.ORANGE
				})

			// medal
			receiver.drawScoreMedal(engine, playerID, 0, 20, "AC", medalAC)
			receiver.drawScoreMedal(engine, playerID, 3, 20, "ST", medalST)
			receiver.drawScoreMedal(engine, playerID, 0, 21, "SK", medalSK)
			receiver.drawScoreMedal(engine, playerID, 3, 21, "CO", medalCO)
			// Section Time
			if(showST&&sectionTime.isNotEmpty()) {
				val y = if(receiver.nextDisplayType==2) 4 else 2
				val x = if(receiver.nextDisplayType==2) 20 else 12
				val x2 = if(receiver.nextDisplayType==2) 9 else 12
				val scale = if(receiver.nextDisplayType==2) .5f else 1f

				receiver.drawScoreFont(engine, playerID, x, y, "SECTION TIME", COLOR.RED, scale)

				for(i in sectionTime.indices)
					if(sectionTime[i]>0) {
						val temp = i*100

						val section = engine.statistics.level/100
						var strSeparator = "-"
						if(i==section&&engine.ending==0) strSeparator = "+"

						val strSectionTime:String = String.format("%4d%s%s", temp, strSeparator, sectionTime[i].toTimeStr)

						receiver.drawScoreNum(engine, playerID, x-1, y+1
							+i, strSectionTime, sectionIsNewRecord[i], scale)

					}

				receiver.drawScoreFont(engine, playerID, x2, 17, "AVERAGE", COLOR.RED)
				receiver.drawScoreNum(engine, playerID, x2, 18,
					(engine.statistics.time/(sectionscomp+(engine.ending==0).toInt())).toTimeStr, 2f)

			}
		}
	}

	/* 移動中の処理 */
	override fun onMove(engine:GameEngine, playerID:Int):Boolean {
		// 新規ピース出現時
		if(engine.ending==0&&engine.statc[0]==0&&!engine.holdDisable&&!lvupflag) {
			// Level up
			if(engine.statistics.level<nextseclv-1) {
				engine.statistics.level++
				if(engine.statistics.level==nextseclv-1&&secAlert) engine.playSE("levelstop")
			}
			levelUp(engine)
		}
		if(engine.ending==0&&engine.statc[0]>0&&(version>=1||!engine.holdDisable))
			lvupflag = false

		if(engine.ending==0&&engine.statc[0]==0&&!engine.holdDisable) {
			// せり上がりカウント
			if(tableGarbage[engine.statistics.level/100]!=0) garbageCount++

			// せり上がり
			if(garbageCount>=tableGarbage[engine.statistics.level/100]&&tableGarbage[engine.statistics.level/100]!=0) {
				engine.playSE("garbage0")
				engine.field.addBottomCopyGarbage(engine.skin, 1,
					Block.ATTRIBUTE.GARBAGE, Block.ATTRIBUTE.VISIBLE, Block.ATTRIBUTE.OUTLINE)
				garbageCount = 0
			}
		}

		// Endingスタート
		if(engine.ending==2&&!rollstarted) {
			rollstarted = true
			engine.big = true
			owner.bgmStatus.bgm = BGM.Ending(2)
		}

		return false
	}

	/* ARE中の処理 */
	override fun onARE(engine:GameEngine, playerID:Int):Boolean {
		// 最後の frame
		if(engine.ending==0&&engine.statc[0]>=engine.statc[1]-1&&!lvupflag) {
			if(engine.statistics.level<nextseclv-1) {
				engine.statistics.level++
				if(engine.statistics.level==nextseclv-1&&secAlert) engine.playSE("levelstop")
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

		// BGM fadeout
		if(tableBGMFadeout[bgmlv]!=-1&&engine.statistics.level>=tableBGMFadeout[bgmlv]) owner.bgmStatus.fadesw = true
	}

	/* Calculate score */
	override fun calcScore(engine:GameEngine, playerID:Int, lines:Int):Int {
		// Combo
		comboValue = if(lines==0) 1
		else maxOf(1, comboValue+2*lines-2)

		if(lines>=1&&engine.ending==0) {
			// 4-line clearカウント
			if(lines>=4)
			// SK medal
				if(big) {
					if(engine.statistics.totalQuadruple==1||engine.statistics.totalQuadruple==2
						||engine.statistics.totalQuadruple==4) {
						engine.playSE("medal1")
						medalSK++
					}
				} else if(engine.statistics.totalQuadruple==5||engine.statistics.totalQuadruple==10
					||engine.statistics.totalQuadruple==17) {
					engine.playSE("medal1")
					medalSK++
					dectemp += 3+medalSK*2// 3 8 15
				}

			// AC medal
			if(engine.field.isEmpty) {

				dectemp += lines*25
				if(lines==3) dectemp += 25
				if(lines==4) dectemp += 150
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

			// せり上がりカウント減少
			if(tableGarbage[engine.statistics.level/100]!=0) garbageCount -= lines
			if(garbageCount<0) garbageCount = 0

			// Level up
			val levelb = engine.statistics.level
			var levelplus = lines
			if(lines==3) levelplus = 4
			if(lines>=4) levelplus = 6

			engine.statistics.level += levelplus

			levelUp(engine)

			if(engine.statistics.level>=1300) {
				// Ending
				engine.playSE("endingstart")
				engine.statistics.level = 1300
				engine.timerActive = false
				engine.ending = 1
				rollclear = 1

				// Section Timeを記録
				sectionlasttime = sectionTime[levelb/100]
				sectionscomp++
				setAverageSectionTime()
				dectemp++
				// ST medal
				stMedalCheck(engine, levelb/100)

				if(sectionlasttime>tableTimeRegret[levelb/100]) {
					// REGRET判定
					regretdispframe = 180
					engine.playSE("regret")
				} else {
					// 段位上昇
					grade++
					if(grade>13) grade = 13
					gradeflash = 180
				}
			} else if(nextseclv==500&&engine.statistics.level>=500&&qualify>0&&engine.statistics.time>qualify||nextseclv==1000&&engine.statistics.level>=1000&&qualify>0&&engine.statistics.time>qualify*2) {
				// level500/1000とりカン
				engine.playSE("endingstart")

				if(nextseclv==500) engine.statistics.level = 500
				if(nextseclv==1000) engine.statistics.level = 1000

				engine.gameEnded()
				engine.staffrollEnable = false
				engine.ending = 1

				secretGrade = engine.field.secretGrade

				// Section Timeを記録
				sectionlasttime = sectionTime[levelb/100]
				sectionscomp++
				setAverageSectionTime()

				// ST medal
				stMedalCheck(engine, levelb/100)

				if(sectionlasttime>tableTimeRegret[levelb/100]) {
					// REGRET判定
					regretdispframe = 180
					engine.playSE("regret")
				} else {
					// 段位上昇
					grade++
					if(grade>13) grade = 13
					gradeflash = 180
				}
			} else if(engine.statistics.level>=nextseclv) {
				// Next Section
				engine.playSE("levelup")

				// Background切り替え
				owner.backgroundStatus.fadesw = true
				owner.backgroundStatus.fadecount = 0
				owner.backgroundStatus.fadebg = 20+nextseclv/100

				// BGM切り替え
				if(tableBGMChange[bgmlv]!=-1&&engine.statistics.level>=tableBGMChange[bgmlv]) {
					bgmlv++
					owner.bgmStatus.fadesw = false
					owner.bgmStatus.bgm = tableBGM[bgmlv]
				}

				// Section Timeを記録
				sectionlasttime = sectionTime[levelb/100]
				sectionscomp++
				setAverageSectionTime()

				// ST medal
				stMedalCheck(engine, levelb/100)

				// 骨Block出現開始
				if(engine.statistics.level>=1000) engine.bone = true

				// Update level for next section
				nextseclv += 100

				if(sectionlasttime>tableTimeRegret[levelb/100]) {
					// REGRET判定
					regretdispframe = 180
					engine.playSE("regret")
				} else {
					// 段位上昇
					grade++
					if(grade>13) grade = 13
					gradeflash = 180
				}
			} else if(engine.statistics.level==nextseclv-1&&secAlert) engine.playSE("levelstop")

			// Calculate score

			lastscore = ((((levelb+lines)/(if(engine.b2b) 3 else 4)+engine.softdropFall+engine.manualLock.toInt())
				*lines*comboValue)+maxOf(0, engine.lockDelay-engine.lockDelayNow)
				+engine.statistics.level/if(engine.twist) 2 else 3)*if(engine.field.isEmpty) 2 else 1

			engine.statistics.scoreLine += lastscore
		}
		return 0
	}

	/* 各 frame の終わりの処理 */
	override fun onLast(engine:GameEngine, playerID:Int) {
		super.onLast(engine, playerID)
		// 段位上昇時のフラッシュ
		if(gradeflash>0) gradeflash--

		// REGRET表示
		if(regretdispframe>0) regretdispframe--

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
			engine.meterValue = remainRollTime*receiver.getMeterMax(engine)/ROLLTIMELIMIT
			engine.meterColor = GameEngine.METER_COLOR_LIMIT

			// Roll 終了
			if(rolltime>=ROLLTIMELIMIT) {
				secretGrade = engine.field.secretGrade
				rollclear = 2
				engine.gameEnded()
				engine.resetStatc()
				engine.stat = GameEngine.Status.EXCELLENT
			}
		} else if(engine.statistics.level==nextseclv-1)
			engine.meterColor = if(engine.meterColor==-0x1)
				-0x10000
			else
				-0x1

	}

	/* Called at game over */
	override fun onGameOver(engine:GameEngine, playerID:Int):Boolean {
		if(engine.statc[0]==0&&engine.gameActive) {
			secretGrade = engine.field.secretGrade

			val time = engine.statistics.time
			if(time<6000)
				dectemp -= 3
			else {
				dectemp++
				if(time%3600<=60||time%3600>=3540) dectemp++
			}

			if(sectionscomp==0) dectemp -= 4
			// Blockの表示を元に戻す
			engine.blockOutlineType = GameEngine.BLOCK_OUTLINE_NORMAL
			// 裏段位
			secretGrade = engine.field.secretGrade
			decoration += dectemp+secretGrade
		}
		return false
	}

	/* 結果画面 */
	override fun renderResult(engine:GameEngine, playerID:Int) {
		receiver.drawMenuFont(engine, playerID, 0, 0, "\u0090\u0093 PAGE${engine.statc[1]+1}/3", COLOR.RED)

		when(engine.statc[1]) {
			0 -> {
				var gcolor = COLOR.WHITE
				if(rollclear==1||rollclear==3) gcolor = COLOR.GREEN
				if(rollclear==2||rollclear==4) gcolor = COLOR.ORANGE
				receiver.drawMenuFont(engine, playerID, 0, 2, "GRADE", COLOR.RED)
				receiver.drawMenuGrade(engine, playerID, 0, 2, tableGradeName[grade], gcolor, 2f)

				drawResultStats(engine, playerID, receiver, 4, COLOR.RED, Statistic.SCORE, Statistic.LINES, Statistic.LEVEL_MANIA,
					Statistic.TIME)
				drawResultRank(engine, playerID, receiver, 12, COLOR.RED, rankingRank)
				if(secretGrade>4)
					drawResult(engine, playerID, receiver, 14, COLOR.RED, "S. GRADE",
						String.format("%10s", tableSecretGradeName[secretGrade-1]))
			}
			1 -> {
				receiver.drawMenuFont(engine, playerID, 0, 2, "SECTION", COLOR.RED)

				for(i in sectionTime.indices)
					if(sectionTime[i]>0)
						receiver.drawMenuFont(engine, playerID, 2, 3+i, sectionTime[i].toTimeStr, sectionIsNewRecord[i])

				if(sectionavgtime>0) {
					receiver.drawMenuFont(engine, playerID, 0, 16, "AVERAGE", COLOR.RED)
					receiver.drawMenuFont(engine, playerID, 2, 17, sectionavgtime.toTimeStr)
				}
			}
			2 -> {
				receiver.drawMenuFont(engine, playerID, 0, 2, "MEDAL", COLOR.BLUE)
				receiver.drawMenuMedal(engine, playerID, 5, 2, "AC", medalAC)
				receiver.drawMenuMedal(engine, playerID, 8, 2, "CO", medalCO)
				receiver.drawMenuMedal(engine, playerID, 2, 3, "ST", medalST)
				receiver.drawMenuMedal(engine, playerID, 6, 3, "SK", medalSK)

				drawResultStats(engine, playerID, receiver, 4, COLOR.RED, Statistic.LPM, Statistic.SPM, Statistic.PIECE, Statistic.PPS)

				receiver.drawMenuFont(engine, playerID, 0, 15, "DECORATION", COLOR.RED)
				receiver.drawMenuFont(engine, playerID, 0, 16, String.format("%10d", dectemp), COLOR.WHITE)
			}
		}
	}

	/* 結果画面の処理 */
	override fun onResult(engine:GameEngine, playerID:Int):Boolean {

		owner.bgmStatus.fadesw = false
		owner.bgmStatus.bgm = when {
			engine.ending==0 -> BGM.Result(0)
			engine.ending==2&&rollclear>0 -> BGM.Result(3)
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
	override fun saveReplay(engine:GameEngine, playerID:Int, prop:CustomProperties):Boolean {
		owner.replayProp.setProperty("speedmania2.version", version)

		// Update rankings
		if(!owner.replayMode&&startLevel==0&&!big&&engine.ai==null) {
			owner.statsProp.setProperty("decoration", decoration)
			updateRanking(grade, engine.statistics.level, engine.statistics.time, rollclear)
			if(medalST==3) updateBestSectionTime()

			if(rankingRank!=-1||medalST==3) return true
		}
		return false
	}

	override fun loadRanking(prop:CustomProperties, ruleName:String) {
		for(i in 0 until RANKING_MAX) {
			rankingGrade[i] = prop.getProperty("$ruleName.$i.grade", 0)
			rankingLevel[i] = prop.getProperty("$ruleName.$i.level", 0)
			rankingTime[i] = prop.getProperty("$ruleName.$i.time", 0)
			rankingRollclear[i] = prop.getProperty("$ruleName.$i.clear", 0)
		}
		for(i in 0 until SECTION_MAX)
			bestSectionTime[i] = prop.getProperty("$ruleName.$i.sectiontime", tableTimeRegret[i])
	}

	/** Save rankings of [ruleName] to owner.recordProp */
	private fun saveRanking(ruleName:String) {
		super.saveRanking((0 until RANKING_MAX).flatMap {i ->
			listOf("$ruleName.$i.grade" to rankingGrade[i],
				"$ruleName.$i.level" to rankingLevel[i],
				"$ruleName.$i.time" to rankingTime[i],
				"$ruleName.$i.clear" to rankingRollclear[i])
		}+(0 until SECTION_MAX).flatMap {i ->
			listOf("$ruleName.sectiontime.$i" to bestSectionTime[i])
		})
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
	 * @param clear Roll クリア flag
	 * @return Position (-1 if unranked)
	 */
	private fun checkRanking(gr:Int, lv:Int, time:Int, clear:Int):Int {
		for(i in 0 until RANKING_MAX)
			if(clear>rankingRollclear[i])
				return i
			else if(clear==rankingRollclear[i]&&gr>rankingGrade[i])
				return i
			else if(clear==rankingRollclear[i]&&gr==rankingGrade[i]&&lv>rankingLevel[i])
				return i
			else if(clear==rankingRollclear[i]&&gr==rankingGrade[i]&&lv==rankingLevel[i]&&time<rankingTime[i]) return i

		return -1
	}

	/** Update best section time records */
	private fun updateBestSectionTime() {
		for(i in 0 until SECTION_MAX)
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
		private val tableARE = intArrayOf(16, 12, 11, 10, 9, 8, 7, 6, 5, 4, 3, 2, 2, 2)
		/** ARE after line clear table */
		private val tableARELine = intArrayOf(6, 6, 5, 5, 4, 4, 3, 3, 2, 2, 1, 1, 1, 2)
		/** Line clear time table */
		private val tableLineDelay = intArrayOf(7, 6, 6, 5, 5, 4, 4, 3, 3, 2, 2, 2, 1, 6)
		/** 固定 time table */
		private val tableLockDelay = intArrayOf(30, 29, 28, 27, 26, 25, 24, 23, 22, 21, 20, 19, 18, 20)
		/** DAS table */
		private val tableDAS = intArrayOf(9, 8, 7, 6, 5, 4, 4, 4, 4, 4, 4, 4, 4, 5)

		/** せり上がり間隔 */
		private val tableGarbage = intArrayOf(50, 33, 27, 25, 21, 18, 15, 12, 9, 7, 0, 0, 0, 0)

		/** REGRET criteria Time */
		private val tableTimeRegret =
			intArrayOf(6000, 5800, 5600, 5400, 5200, 5000, 4750, 4500, 4250, 4000, 3750, 3500, 3250, 3000)

		/** BGM fadeout levels */
		private val tableBGMFadeout = intArrayOf(485, 685, 985)
		/** BGM change levels */
		private val tableBGMChange = intArrayOf(500, 700, 1000)
		private val tableBGM = arrayOf(BGM.GrandT(2), BGM.GrandT(3), BGM.GrandT(4), BGM.GrandT(5))
		/** 段位のName */
		private val tableGradeName =
			arrayOf("1", "S1", "S2", "S3", "S4", "S5", "S6", "S7", "S8", "S9", "S10", "S11", "S12", "S13")

		/** 裏段位のName */

		private val tableSecretGradeName =
			arrayOf("S1", "S2", "S3", "S4", "S5", "S6", "S7", "S8", "S9", "M1", "M2", "M3", "M4", "M5", "M6", "M7", "M8", "M9", "GM")

		/** LV999 roll time */
		private const val ROLLTIMELIMIT = 3238

		/** Number of entries in rankings */
		private const val RANKING_MAX = 13

		/** Number of sections */
		private const val SECTION_MAX = 13
	}
}
