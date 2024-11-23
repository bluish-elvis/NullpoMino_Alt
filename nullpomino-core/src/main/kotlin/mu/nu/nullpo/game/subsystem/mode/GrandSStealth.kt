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
import mu.nu.nullpo.gui.common.BaseFont
import mu.nu.nullpo.util.CustomProperties
import mu.nu.nullpo.util.GeneralUtil.toTimeStr

/** PHANTOM MANIA mode (Original from NullpoUE build 121909 by Zircean) */
class GrandSStealth:AbstractGrand() {

	/** Current grade */
	private var grade = 0
	/** Remaining frames of flash effect of grade display */
	private var gradeFlash = 0

	/** Secret Grade */
	private var secretGrade = 0

	/** Remaining ending time limit */
	private var rollTime = 0
	/** 0:Died before ending, 1:Died during ending, 2:Completed ending */
	private var rollClear = 0
	/** True if ending has started */
	private var rollStarted = false

	/** Current BGM */
	private var bgmLv = 0

	/** Section 内で4-line clearした count */
	private var sectionQuads = MutableList(sectionMax) {0}
	/** Set to true by default, set to false when sectionQuads is below 2 */
	private var gmQuads = false

	/** false:Leaderboard, true:Section time record
	 * (Push F in settings screen to flip it) */
	private var isShowBestSectionTime = false

	/** Selected start level */
	private var startLevel = 0

	private val itemBig = BooleanMenuItem("big", "BIG", COLOR.BLUE, false)
	/** BigMode */
	private var big:Boolean by DelegateMenuItem(itemBig)

	/** Version of this mode */
	private var version = 0

	/** Your place on leaderboard (-1: out of rank) */
	private var rankingRank = 0

	/** Grade records */
	private val rankingGrade = MutableList(rankingMax) {0}
	/** Level records */
	private val rankingLevel = MutableList(rankingMax) {0}
	/** Time records */
	private val rankingTime = MutableList(rankingMax) {-1}
	/** Roll-Cleared records */
	private val rankingRollClear = MutableList(rankingMax) {0}
	/** Best section time records */
	private val bestSectionTime = MutableList(sectionMax) {DEFAULT_SECTION_TIME}

	/** Returns the name of this mode */
	override val name = "Grand Phantom"
	override val gameIntensity = 3
	override val propRank
		get() = rankMapOf(
			"grade" to rankingGrade, "level" to rankingLevel, "time" to rankingTime, "rollClear" to rankingRollClear,
			"section.time" to bestSectionTime
		)
	/** This function will be called when the game enters
	 * the main game screen. */
	override fun playerInit(engine:GameEngine) {
		super.playerInit(engine)
		grade = 0
		gradeFlash = 0
		rollTime = 0
		rollClear = 0
		rollStarted = false
		bgmLv = 0
		sectionQuads.fill(0)
		medals.reset()
		isShowBestSectionTime = false
		startLevel = 0
		secAlert = false
		big = false
		showST = true

		rankingRank = -1
		rankingGrade.fill(0)
		rankingLevel.fill(0)
		rankingTime.fill(-1)
		rankingRollClear.fill(0)
		bestSectionTime.fill(DEFAULT_SECTION_TIME)

		engine.twistEnable = false
		engine.b2bEnable = false
		engine.splitB2B = false
		engine.comboType = GameEngine.COMBO_TYPE_DOUBLE
		engine.frameColor = GameEngine.FRAME_COLOR_CYAN
		engine.bigHalf = true
		engine.bigMove = true
		engine.staffrollEnable = true
		engine.staffrollNoDeath = false

		if(!owner.replayMode) {
			version = CURRENT_VERSION
		} else {
			for(i in 0..<sectionMax)
				bestSectionTime[i] = DEFAULT_SECTION_TIME
			version = owner.replayProp.getProperty("phantommania.version", 0)
		}

		owner.bgMan.bg = startLevel
	}

	/** Load the settings */
	override fun loadSetting(engine:GameEngine, prop:CustomProperties, ruleName:String, playerID:Int) {
		startLevel = prop.getProperty("phantommania.startLevel", 0)
		secAlert = prop.getProperty("phantommania.lvstopse", true)
		showST = prop.getProperty("phantommania.showsectiontime", true)
		big = prop.getProperty("phantommania.big", false)
	}

	/** Save the settings */
	override fun saveSetting(engine:GameEngine, prop:CustomProperties, ruleName:String, playerID:Int) {
		prop.setProperty("phantommania.startLevel", startLevel)
		prop.setProperty("phantommania.lvstopse", secAlert)
		prop.setProperty("phantommania.showsectiontime", showST)
		prop.setProperty("phantommania.big", big)
	}

	/** Set the starting bgmLv */
	private fun setStartBgmlv(engine:GameEngine) {
		bgmLv = 0
		while(tableBGMChange[bgmLv]!=-1&&engine.statistics.level>=tableBGMChange[bgmLv])
			bgmLv++
	}

	/** Set the gravity speed
	 * @param engine GameEngine object
	 */
	override fun setSpeed(engine:GameEngine) {
		engine.speed.gravity = -1

		val section = minOf(engine.statistics.level/100, tableARE.size-1)
		engine.speed.are = tableARE[section]
		engine.speed.areLine = tableARELine[section]
		engine.speed.lineDelay = tableLineDelay[section]
		engine.speed.lockDelay = tableLockDelay[section]
		engine.speed.das = tableDAS[section]

		engine.blockHidden = maxOf(tableHiddenDelay[section], engine.ruleOpt.lockFlash)
	}

	/** Set Pressure Hidden params
	 * @param engine GameEngine
	 */
	private fun setHeboHidden(engine:GameEngine) {
		var section = engine.statistics.level/100
		if(section>tableARE.size-1) section = tableARE.size-1
		if(section==1) {
			engine.heboHiddenYLimit = 15
			engine.heboHiddenTimerMax = (engine.heboHiddenYNow+2)*120
		}
		if(section==2) {
			engine.heboHiddenYLimit = 17
			engine.heboHiddenTimerMax = (engine.heboHiddenYNow+1)*90
		}
		if(section==3) {
			engine.heboHiddenYLimit = 19
			engine.heboHiddenTimerMax = engine.heboHiddenYNow*60+60
		}
		if(section==4) {
			engine.heboHiddenYLimit = 19
			engine.heboHiddenTimerMax = engine.heboHiddenYNow*45+45
		}
		if(section>=5) {
			engine.heboHiddenYLimit = 19
			engine.heboHiddenTimerMax = engine.heboHiddenYNow*30+30
		}
	}

	/** Checks ST medal
	 * @param engine GameEngine
	 * @param section Section Number
	 */
	private fun stMedalCheck(engine:GameEngine, section:Int) =
		stMedalCheck(engine, section, sectionTime[section], bestSectionTime[section])

	/** Main routine for game setup screen */
	override fun onSetting(engine:GameEngine):Boolean {
		if(!owner.replayMode) {
			// Configuration changes
			val change = updateCursor(engine, 3)

			if(change!=0) {
				engine.playSE("change")

				when(menuCursor) {
					0 -> {
						startLevel += change
						if(startLevel<0) startLevel = 9
						if(startLevel>9) startLevel = 0
						owner.bgMan.bg = startLevel
					}
					1 -> secAlert = !secAlert
					2 -> showST = !showST
					3 -> big = !big
				}
			}

			// Check for F button, when pressed this will flip Leaderboard/Best Section Time Records
			if(engine.ctrl.isPush(Controller.BUTTON_F)) {
				engine.playSE("change")
				isShowBestSectionTime = !isShowBestSectionTime
			}

			// Check for A button, when pressed this will begin the game
			if(menuTime<5) menuTime++ else if(engine.ctrl.isPush(Controller.BUTTON_A)) {
				engine.playSE("decide")
				isShowBestSectionTime = false
				sectionsDone = 0
				return false
			}

			// Check for B button, when pressed this will shut down the game engine.
			if(engine.ctrl.isPush(Controller.BUTTON_B)) engine.quitFlag = true
		} else {
			menuTime++
			menuCursor = -1

			return menuTime<60
		}

		return true
	}

	/** Renders game setup screen */
	override fun renderSetting(engine:GameEngine) {
		drawMenu(
			engine, receiver, 0, COLOR.PURPLE, 0, "Level" to startLevel*100, "LVSTOPSE" to secAlert, "SHOW STIME" to showST,
			"BIG" to big
		)
	}

	/** This function will be called before the game actually begins (after
	 * Ready&Go screen disappears) */
	override fun startGame(engine:GameEngine) {
		engine.statistics.level = startLevel*100
		nextSecLv = (startLevel*100+100).coerceIn(100, 999)

		owner.bgMan.bg = engine.statistics.level/100

		engine.big = big
		engine.heboHiddenEnable = true

		setSpeed(engine)
		setStartBgmlv(engine)
		owner.musMan.bgm = tableBGM[bgmLv]
	}

	override fun renderFirst(engine:GameEngine) {
		if(engine.gameActive&&engine.ending==2) receiver.drawStaffRoll(engine, rollTime*1f/ROLLTIMELIMIT)
	}
	/** Renders HUD (leaderboard or game statistics) */
	override fun renderLast(engine:GameEngine) {
		receiver.drawScoreFont(engine, 0, 0, name, COLOR.WHITE)

		if(engine.stat==GameEngine.Status.SETTING||engine.stat==GameEngine.Status.RESULT&&!owner.replayMode) {
			if(!owner.replayMode&&startLevel==0&&!big&&engine.ai==null)
				if(!isShowBestSectionTime) {
					// Leaderboard
					val topY = if(receiver.nextDisplayType==2) 5 else 3
					receiver.drawScoreFont(engine, 0, topY-1, "GRADE LV TIME", COLOR.PURPLE)

					for(i in 0..<rankingMax) {

						receiver.drawScoreGrade(
							engine, 0, topY+i, "%2d".format(i+1), if(rankingRank==i) COLOR.RAINBOW else COLOR.YELLOW
						)
						if(rankingGrade[i]>=0&&rankingGrade[i]<tableGradeName.size)
							receiver.drawScoreFont(
								engine, 3, topY+i, tableGradeName[rankingGrade[i]],
								when {
									rankingRollClear[i]==1 -> COLOR.GREEN;rankingRollClear[i]==2 -> COLOR.ORANGE;else -> COLOR.WHITE
								}
							)
						receiver.drawScoreNum(engine, 5, topY+i, "%03d".format(rankingLevel[i]), i==rankingRank)
						receiver.drawScoreNum(engine, 8, topY+i, rankingTime[i].toTimeStr, i==rankingRank)
					}

					receiver.drawScoreFont(engine, 0, 17, "F:VIEW SECTION TIME", COLOR.GREEN)
				} else {
					// Best section time records
					receiver.drawScoreFont(engine, 0, 2, "SECTION TIME", COLOR.PURPLE)

					val totalTime = (0..<sectionMax).fold(0) {tt, i ->
						val slv = minOf(i*100, 999)
						receiver.drawScoreNum(
							engine, 0, 3+i, "%3d-%3d %s".format(slv, slv+99, bestSectionTime[i].toTimeStr), sectionIsNewRecord[i]
						)
						tt+bestSectionTime[i]
					}
					receiver.drawScoreFont(engine, 0, 17, "TOTAL", COLOR.PURPLE)
					receiver.drawScoreNum(engine, 0, 18, totalTime.toTimeStr, 2f)
					receiver.drawScoreFont(engine, 9, 17, "AVERAGE", COLOR.PURPLE)
					receiver.drawScoreNum(engine, 9, 18, (totalTime/sectionMax).toTimeStr, 2f)

					receiver.drawScoreFont(engine, 0, 17, "F:VIEW RANKING", COLOR.GREEN)
				}
		} else {
			if(grade>=0&&grade<tableGradeName.size)
				receiver.drawScoreFont(engine, 0, 2, tableGradeName[grade], gradeFlash>0&&gradeFlash%4==0, 2f)

			// Score
			receiver.drawScoreFont(engine, 0, 5, "Score", COLOR.PURPLE)
			receiver.drawScoreNum(engine, 0, 6, "$scDisp"+"\n"+lastScore)

			receiver.drawScoreFont(engine, 0, 9, "Level", COLOR.PURPLE)
			receiver.drawScoreNum(engine, 1, 10, "%3d".format(maxOf(engine.statistics.level, 0)))
			receiver.drawScoreSpeed(engine, 0, 11, if(engine.speed.gravity<0) 40 else engine.speed.gravity/128, 4)
			receiver.drawScoreNum(engine, 1, 12, "%3d".format(nextSecLv))

			receiver.drawScoreFont(engine, 0, 14, "Time", COLOR.PURPLE)
			receiver.drawScoreNum(engine, 0, 15, engine.statistics.time.toTimeStr, 2f)

			if(engine.gameActive&&engine.ending==2) {
				var time = ROLLTIMELIMIT-rollTime
				if(time<0) time = 0
				receiver.drawScoreFont(engine, 0, 17, "ROLL TIME", COLOR.PURPLE)
				receiver.drawScoreNum(engine, 0, 18, time.toTimeStr, time>0&&time<10*60, 2f)
			}

			receiver.drawScoreMedal(engine, 0, 20, "AC", medalAC)
			receiver.drawScoreMedal(engine, 3, 20, "ST", medalST)
			receiver.drawScoreMedal(engine, 0, 21, "SK", medalSK)
			receiver.drawScoreMedal(engine, 3, 21, "RE", medalRE)
			receiver.drawScoreMedal(engine, 0, 22, "RO", medalRO)
			receiver.drawScoreMedal(engine, 3, 22, "CO", medalCO)

			if(showST&&sectionTime.isNotEmpty()) {
				val x = if(receiver.nextDisplayType==2) 8 else 12
				val x2 = if(receiver.nextDisplayType==2) 9 else 12

				receiver.drawScoreFont(engine, x, 2, "SECTION TIME", COLOR.PURPLE)

				val section = engine.statistics.level/100
				sectionTime.forEachIndexed {i, it ->
					if(it>0) receiver.drawScoreNum(
						engine, x, 3+i, "%3d%s%s".format(
							minOf(i*100, 999), if(i==section&&engine.ending==0) "+" else "-", it.toTimeStr
						), sectionIsNewRecord[i]
					)
				}
				receiver.drawScoreFont(engine, x2, 17, "AVERAGE", COLOR.PURPLE)
				receiver.drawScoreNum(engine, x2, 18, (engine.statistics.time/(sectionsDone+1)).toTimeStr, 2f)
			}
		}
	}

	/** This function will be called when the piece is active */
	override fun onMove(engine:GameEngine):Boolean {
		if(engine.ending==2&&!rollStarted) rollStarted = true
		return super.onMove(engine)
	}

	/** Levelup */
	override fun levelUp(engine:GameEngine, lu:Int) {
		super.levelUp(engine, lu)
		if(lu>0&&tableBGMFadeout[bgmLv]!=-1&&engine.statistics.level>=tableBGMFadeout[bgmLv]) owner.musMan.fadeSW = true
	}

	/** Calculates line-clear score
	 * (This function will be called even if no lines are cleared) */
	override fun calcScore(engine:GameEngine, ev:ScoreEvent):Int {
		val pts = super.calcScore(engine, ev)
		val li = ev.lines

		if(li>=1&&engine.ending==0) {
			val levelb = engine.statistics.level
			val section = levelb/100
			if(li>=4) sectionQuads[section]++

			levelUp(engine, li)

			if(engine.statistics.level>=999) {
				if(engine.timerActive) sectionsDone++

				engine.playSE("endingstart")
				engine.statistics.level = 999
				engine.timerActive = false
				engine.ending = 2
				rollClear = 1

				stMedalCheck(engine, levelb/100)
				roMedalCheck(engine, 999)

				if(engine.statistics.totalQuadruple>=31&&sectionQuads.all {it>=1}) {
					grade = 6
					gradeFlash = 180
				}
			} else if(nextSecLv==300&&engine.statistics.level>=300&&engine.statistics.time>LV300TORIKAN) {
				if(engine.timerActive) sectionsDone++

				engine.playSE("endingstart")
				engine.statistics.level = 300
				engine.timerActive = false
				engine.ending = 2

				if(tableBGMChange[bgmLv]!=-1&&engine.statistics.level>=tableBGMChange[bgmLv]) {
					bgmLv++
					owner.musMan.fadeSW = false
					owner.musMan.bgm = tableBGM[bgmLv]
				}

				stMedalCheck(engine, section)
			} else if(nextSecLv==500&&engine.statistics.level>=500&&engine.statistics.time>LV500TORIKAN) {
				if(engine.timerActive) sectionsDone++


				engine.playSE("endingstart")
				engine.statistics.level = 500
				engine.timerActive = false
				engine.ending = 2

				if(tableBGMChange[bgmLv]!=-1&&engine.statistics.level>=tableBGMChange[bgmLv]) {
					bgmLv++
					owner.musMan.fadeSW = false
					owner.musMan.bgm = tableBGM[bgmLv]
				}

				stMedalCheck(engine, section)
			} else if(nextSecLv==800&&engine.statistics.level>=800&&engine.statistics.time>LV800TORIKAN) {
				if(engine.timerActive) sectionsDone++


				engine.playSE("endingstart")
				engine.statistics.level = 800
				engine.timerActive = false
				engine.ending = 2

				if(tableBGMChange[bgmLv]!=-1&&engine.statistics.level>=tableBGMChange[bgmLv]) {
					bgmLv++
					owner.musMan.fadeSW = false
					owner.musMan.bgm = tableBGM[bgmLv]
				}

				stMedalCheck(engine, section)
			} else if(engine.statistics.level>=nextSecLv) {
				owner.bgMan.nextBg = nextSecLv/100

				if(tableBGMChange[bgmLv]!=-1&&engine.statistics.level>=tableBGMChange[bgmLv]) {
					bgmLv++
					owner.musMan.fadeSW = false
					owner.musMan.bgm = tableBGM[bgmLv]
					engine.playSE("levelup_section")
				}
				engine.playSE("levelup")

				sectionsDone++

				gmQuads = sectionQuads.all {it>=2}
				sectionQuads[section+1] = 0
				stMedalCheck(engine, section)

				if(nextSecLv==300||nextSecLv==700) roMedalCheck(engine, nextSecLv)

				if(startLevel==0)
					for(i in 0..<tableGradeLevel.size-1)
						if(engine.statistics.level>=tableGradeLevel[i]) {
							grade = i
							gradeFlash = 180
						}

				nextSecLv += 100
				if(nextSecLv>999) nextSecLv = 999
			}
			lastScore = pts
			engine.statistics.scoreLine += pts
			return pts
		}
		return 0
	}

	/** This function will be called when the game timer updates */
	override fun onLast(engine:GameEngine) {
		super.onLast(engine)
		if(gradeFlash>0) gradeFlash--
		setHeboHidden(engine)
		if(engine.timerActive&&engine.ending==0) {
			val section = engine.statistics.level/100

			if(section>=0&&section<sectionTime.size) {
				sectionTime[section] = engine.statistics.time-sectionTime.take(section).sum()
			}
		}

		if(engine.gameActive&&engine.ending==2) {
			if(engine.ctrl.isPress(Controller.BUTTON_F)&&engine.statistics.level<999) rollTime += 5
			rollTime++

			val remainRollTime = ROLLTIMELIMIT-rollTime
			engine.meterValue = remainRollTime*1f/ROLLTIMELIMIT
			engine.meterColor = GameEngine.METER_COLOR_GREEN
			if(remainRollTime<=30*60) engine.meterColor = GameEngine.METER_COLOR_YELLOW
			if(remainRollTime<=20*60) engine.meterColor = GameEngine.METER_COLOR_ORANGE
			if(remainRollTime<=10*60) engine.meterColor = GameEngine.METER_COLOR_RED

			if(rollTime>=ROLLTIMELIMIT) {
				if(engine.statistics.level>=999) rollClear = 2

				engine.gameEnded()
				engine.resetStatc()
				engine.stat = GameEngine.Status.EXCELLENT
			}
		}
	}

	/** This function will be called when the player tops out */
	override fun onGameOver(engine:GameEngine):Boolean {
		if(engine.statc[0]==0) secretGrade = engine.field.secretGrade
		return false
	}

	/** Renders game result screen */
	override fun renderResult(engine:GameEngine) {
		receiver.drawMenuFont(engine, 0, 0, "${BaseFont.UP_S}${BaseFont.DOWN_S} PAGE${(engine.statc[1]+1)}/3", COLOR.RED)

		if(engine.statc[1]==0) {
			val col = when(rollClear) {
				1 -> COLOR.GREEN
				2 -> COLOR.ORANGE
				else -> COLOR.WHITE
			}
			receiver.drawMenuFont(engine, 0, 2, "GRADE", COLOR.PURPLE)
			val strGrade = "%10s".format(tableGradeName[grade])
			receiver.drawMenuFont(engine, 0, 3, strGrade, col)

			drawResultStats(
				engine, receiver, 4, COLOR.PURPLE, Statistic.SCORE, Statistic.LINES, Statistic.LEVEL_MANIA, Statistic.TIME
			)
			drawResultRank(engine, receiver, 12, COLOR.PURPLE, rankingRank)
			if(secretGrade>4)
				drawResult(
					engine, receiver, 15, COLOR.PURPLE, "S. GRADE",
					"%10s".format(tableSecretGradeName[secretGrade-1])
				)
		} else if(engine.statc[1]==1) {
			receiver.drawMenuFont(engine, 0, 2, "SECTION", COLOR.PURPLE)

			for(i in sectionTime.indices)
				if(sectionTime[i]>0)
					receiver.drawMenuFont(engine, 2, 3+i, sectionTime[i].toTimeStr, sectionIsNewRecord[i])

			if(sectionAvgTime>0) {
				receiver.drawMenuFont(engine, 0, 14, "AVERAGE", COLOR.PURPLE)
				receiver.drawMenuFont(engine, 2, 15, sectionAvgTime.toTimeStr)
			}
		} else if(engine.statc[1]==2) {
			receiver.drawMenuNano(engine, 0, 1.5f, "MEDAL", COLOR.PURPLE, .5f)
			receiver.drawMenuMedal(engine, 2, 2, "AC", medalAC)
			receiver.drawMenuMedal(engine, 5, 2, "ST", medalST)
			receiver.drawMenuMedal(engine, 8, 2, "SK", medalSK)
			receiver.drawMenuMedal(engine, 1, 3, "RE", medalRE)
			receiver.drawMenuMedal(engine, 4, 3, "RO", medalRO)
			receiver.drawMenuMedal(engine, 7, 3, "CO", medalCO)

			drawResultStats(engine, receiver, 6, COLOR.PURPLE, Statistic.LPM, Statistic.SPM, Statistic.PIECE, Statistic.PPS)
		}
	}

	/** Additional routine for game result screen */
	override fun onResult(engine:GameEngine):Boolean {
		// Page change
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
		// Flip Leaderboard/Best Section Time Records
		if(engine.ctrl.isPush(Controller.BUTTON_F)) {
			engine.playSE("change")
			isShowBestSectionTime = !isShowBestSectionTime
		}

		return false
	}

	/** This function will be called when the replay data is going to be
	 * saved */
	override fun saveReplay(engine:GameEngine, prop:CustomProperties):Boolean {
		saveSetting(engine, owner.replayProp)
		owner.replayProp.setProperty("phantommania.version", version)

		if(!owner.replayMode&&startLevel==0&&!big&&engine.ai==null) {
//			owner.statsProp.setProperty("decoration", decoration)
			updateRanking(grade, engine.statistics.level, engine.statistics.time, rollClear)
			if(medalST==3) updateBestSectionTime()

			if(rankingRank!=-1||medalST==3) return true
		}
		return false
	}

	/** Update the ranking */
	private fun updateRanking(gr:Int, lv:Int, time:Int, clear:Int) {
		rankingRank = checkRanking(gr, lv, time, clear)

		if(rankingRank!=-1) {
			for(i in rankingMax-1 downTo rankingRank+1) {
				rankingGrade[i] = rankingGrade[i-1]
				rankingLevel[i] = rankingLevel[i-1]
				rankingTime[i] = rankingTime[i-1]
				rankingRollClear[i] = rankingRollClear[i-1]
			}

			rankingGrade[rankingRank] = gr
			rankingLevel[rankingRank] = lv
			rankingTime[rankingRank] = time
			rankingRollClear[rankingRank] = clear
		}
	}

	/** This function will check the ranking and returns which place you are.
	 * (-1: Out of rank) */
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

	/** Updates best section time records */
	private fun updateBestSectionTime() {
		for(i in 0..<sectionMax)
			if(sectionIsNewRecord[i]) bestSectionTime[i] = sectionTime[i]
	}

	companion object {
		/** Current version */
		private const val CURRENT_VERSION = 1

		/** ARE table */
		private val tableARE = listOf(15, 11, 11, 5, 4, 3)

		/** ARE Line table */
		private val tableARELine = listOf(11, 5, 5, 4, 4, 3)

		/** Line Delay table */
		private val tableLineDelay = listOf(12, 6, 6, 7, 5, 4)

		/** Lock Delay table */
		private val tableLockDelay = listOf(31, 29, 28, 27, 26, 25)

		/** DAS table */
		private val tableDAS = listOf(11, 11, 10, 9, 7, 7)
		private val tableHiddenDelay = listOf(360, 320, 256, 192, 160, 128)

		/** BGM fadeout level */
		private val tableBGMFadeout = listOf(280, 480, -1)

		/** BGM change level */
		private val tableBGMChange = listOf(300, 500, -1)
		private val tableBGM = listOf(BGM.GrandT(5), BGM.GrandT(4), BGM.GrandT(3), BGM.GrandA(3))
		/** Grade names */

		private val tableGradeName = listOf("", "m", "MK", "MV", "MO", "MM", "GM")

		/** Required level for grade */
		private val tableGradeLevel = listOf(0, 500, 600, 700, 800, 900, 999)

		/** Secret grade names */
		private val tableSecretGradeName = listOf(
			"S1", "S2", "S3", "S4", "S5", "S6", "S7", "S8", "S9", //  0?` 8
			"M1", "M2", "M3", "M4", "M5", "M6", "M7", "M8", "M9", //  9?`17
			"GM" // 18
		)

		/** Ending time limit */
		private const val ROLLTIMELIMIT = 1982

		/** Level 300 time limit */
		private const val LV300TORIKAN = 8880

		/** Level 500 time limit */
		private const val LV500TORIKAN = 13080

		/** Level 800 time limit */
		private const val LV800TORIKAN = 19380

		/** Default section time */
		private const val DEFAULT_SECTION_TIME = 3600
	}
}
