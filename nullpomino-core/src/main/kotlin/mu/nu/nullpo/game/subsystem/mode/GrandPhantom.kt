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
import mu.nu.nullpo.util.CustomProperties
import mu.nu.nullpo.util.GeneralUtil.toTimeStr

/** PHANTOM MANIA mode (Original from NullpoUE build 121909 by Zircean) */
class GrandPhantom:AbstractMode() {

	/** Next section level */
	private var nextseclv = 0

	/** Level up flag (Set to true when the level increases) */
	private var lvupflag = false

	/** Current grade */
	private var grade = 0

	/** Remaining frames of flash effect of grade display */
	private var gradeflash = 0

	/** Used by combo scoring */
	private var comboValue = 0

	/** Secret Grade */
	private var secretGrade = 0

	/** Remaining ending time limit */
	private var rolltime = 0

	/** 0:Died before ending, 1:Died during ending, 2:Completed ending */
	private var rollclear = 0

	/** True if ending has started */
	private var rollstarted = false

	/** Current BGM */
	private var bgmLv = 0

	/** Section Time */
	private var sectionTime = MutableList(SECTION_MAX) {0}

	/** This will be true if the player achieves
	 * new section time record in specific section */
	private var sectionIsNewRecord = MutableList(SECTION_MAX) {false}

	/** Amount of sections completed */
	private var sectionscomp = 0

	/** Average section time */
	private var sectionavgtime = 0

	/** Current section time */
	private var sectionlasttime = 0

	/** Number of 4-Line clears in current section */
	private var sectionfourline = 0

	/** Set to true by default, set to false when sectionfourline is below 2 */
	private var gmfourline = false

	/** AC medal (0:None, 1:Bronze, 2:Silver, 3:Gold) */
	private var medalAC = 0

	/** ST medal */
	private var medalST = 0

	/** SK medal */
	private var medalSK = 0

	/** RE medal */
	private var medalRE = 0

	/** RO medal */
	private var medalRO = 0

	/** CO medal */
	private var medalCO = 0

	/** Used by RE medal */
	private var recoveryFlag = false

	/** Total spins */
	private var spinCount = 0

	/** false:Leaderboard, true:Section time record
	 * (Push F in settings screen to flip it) */
	private var isShowBestSectionTime = false

	/** Selected start level */
	private var startLevel = 0

	/** Enable/Disable level stop sfx */
	private var secAlert = false

	private val itemBig = BooleanMenuItem("big", "BIG", COLOR.BLUE, false)
	/** BigMode */
	private var big:Boolean by DelegateMenuItem(itemBig)

	/** Show section time */
	private var showST = false

	/** Version of this mode */
	private var version = 0

	/** Your place on leaderboard (-1: out of rank) */
	private var rankingRank = 0

	/** Grade records */
	private val rankingGrade = MutableList(RANKING_MAX) {0}

	/** Level records */
	private val rankingLevel = MutableList(RANKING_MAX) {0}

	/** Time records */
	private val rankingTime = MutableList(RANKING_MAX) {0}

	/** Roll-Cleared records */
	private val rankingRollclear = MutableList(RANKING_MAX) {0}

	/** Best section time records */
	private val bestSectionTime = MutableList(SECTION_MAX) {0}

	/** Returns the name of this mode */
	override val name = "Grand Phantom"
	override val gameIntensity = 3
	override val rankMap
		get() = rankMapOf(
			"grade" to rankingGrade, "level" to rankingLevel, "time" to rankingTime, "rollclear" to rankingRollclear,
			"section.time" to bestSectionTime
		)
	/** This function will be called when the game enters
	 * the main game screen. */
	override fun playerInit(engine:GameEngine) {
		super.playerInit(engine)
		nextseclv = 0
		lvupflag = true
		grade = 0
		gradeflash = 0
		comboValue = 0
		lastscore = 0
		rolltime = 0
		rollclear = 0
		rollstarted = false
		bgmLv = 0
		sectionTime.fill(0)
		sectionIsNewRecord.fill(false)
		sectionavgtime = 0
		sectionlasttime = 0
		sectionfourline = 0
		gmfourline = true
		medalAC = 0
		medalST = 0
		medalSK = 0
		medalRE = 0
		medalRO = 0
		medalCO = 0
		recoveryFlag = false
		spinCount = 0
		isShowBestSectionTime = false
		startLevel = 0
		secAlert = false
		big = false
		showST = true

		rankingRank = -1
		rankingGrade.fill(0)
		rankingLevel.fill(0)
		rankingTime.fill(0)
		rankingRollclear.fill(0)
		bestSectionTime.fill(0)

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
			for(i in 0 until SECTION_MAX)
				bestSectionTime[i] = DEFAULT_SECTION_TIME
			version = owner.replayProp.getProperty("phantommania.version", 0)
		}

		owner.bgMan.bg = startLevel
	}

	/** Load the settings */
	override fun loadSetting(prop:CustomProperties, ruleName:String, playerID:Int) {
		startLevel = prop.getProperty("phantommania.startLevel", 0)
		secAlert = prop.getProperty("phantommania.lvstopse", true)
		showST = prop.getProperty("phantommania.showsectiontime", true)
		big = prop.getProperty("phantommania.big", false)
	}

	/** Save the settings */
	override fun saveSetting(prop:CustomProperties, ruleName:String, playerID:Int) {
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
	private fun setSpeed(engine:GameEngine) {
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

	/** Calculates average section time */
	private fun setAverageSectionTime() {
		if(sectionscomp>0) {
			var temp = 0
			for(i in startLevel until startLevel+sectionscomp)
				temp += sectionTime[i]
			sectionavgtime = temp/sectionscomp
		} else
			sectionavgtime = 0
	}

	/** Checks ST medal
	 * @param engine GameEngine
	 * @param sectionNumber Section Number
	 */
	private fun stMedalCheck(engine:GameEngine, sectionNumber:Int) {
		val best = bestSectionTime[sectionNumber]

		if(sectionlasttime<best) {
			if(medalST<3) {
				engine.playSE("medal3")
				medalST = 3
			}
			if(!owner.replayMode) sectionIsNewRecord[sectionNumber] = true
		} else if(sectionlasttime<best+300&&medalST<2) {
			engine.playSE("medal2")
			medalST = 2
		} else if(sectionlasttime<best+600&&medalST<1) {
			engine.playSE("medal1")
			medalST = 1
		}
	}

	/** Checks RO medal */
	private fun roMedalCheck(engine:GameEngine) {
		val spinAverage = spinCount.toFloat()/engine.statistics.totalPieceLocked.toFloat()

		if(spinAverage>=1.2f&&medalRO<3) {
			engine.playSE("medal${++medalRO}")
		}
	}

	/** Main routine for game setup screen */
	override fun onSetting(engine:GameEngine):Boolean {
		if(!engine.owner.replayMode) {
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
				sectionscomp = 0
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

		nextseclv = engine.statistics.level+100
		if(engine.statistics.level<0) nextseclv = 100
		if(engine.statistics.level>=900) nextseclv = 999

		owner.bgMan.bg = engine.statistics.level/100

		engine.big = big
		engine.heboHiddenEnable = true

		setSpeed(engine)
		setStartBgmlv(engine)
		owner.musMan.bgm = tableBGM[bgmLv]
	}

	/** Renders HUD (leaderboard or game statistics) */
	override fun renderLast(engine:GameEngine) {
		receiver.drawScoreFont(engine, 0, 0, "PHANTOM MANIA", COLOR.WHITE)

		if(engine.stat==GameEngine.Status.SETTING||engine.stat==GameEngine.Status.RESULT&&!owner.replayMode) {
			if(!owner.replayMode&&startLevel==0&&!big&&engine.ai==null)
				if(!isShowBestSectionTime) {
					// Leaderboard
					val topY = if(receiver.nextDisplayType==2) 5 else 3
					receiver.drawScoreFont(engine, 3, topY-1, "GRADE LEVEL TIME", COLOR.PURPLE)

					for(i in 0 until RANKING_MAX) {
						var gcolor = COLOR.WHITE
						if(rankingRollclear[i]==1) gcolor = COLOR.GREEN
						if(rankingRollclear[i]==2) gcolor = COLOR.ORANGE
						receiver.drawScoreGrade(
							engine, 0, topY+i, String.format("%2d", i+1), if(rankingRank==i) COLOR.RAINBOW else COLOR.YELLOW
						)
						if(rankingGrade[i]>=0&&rankingGrade[i]<tableGradeName.size)
							receiver.drawScoreFont(engine, 3, topY+i, tableGradeName[rankingGrade[i]], gcolor)
						receiver.drawScoreNum(engine, 9, topY+i, String.format("%03d", rankingLevel[i]), i==rankingRank)
						receiver.drawScoreNum(engine, 15, topY+i, rankingTime[i].toTimeStr, i==rankingRank)
					}

					receiver.drawScoreFont(engine, 0, 17, "F:VIEW SECTION TIME", COLOR.GREEN)
				} else {
					// Best section time records
					receiver.drawScoreFont(engine, 0, 2, "SECTION TIME", COLOR.PURPLE)

					var totalTime = 0
					for(i in 0 until SECTION_MAX) {
						val temp = minOf(i*100, 999)
						val temp2 = minOf((i+1)*100-1, 999)

						val strSectionTime:String = String.format("%3d-%3d %s", temp, temp2, bestSectionTime[i].toTimeStr)

						receiver.drawScoreNum(engine, 0, 3+i, strSectionTime, sectionIsNewRecord[i])

						totalTime += bestSectionTime[i]
					}
					receiver.drawScoreFont(engine, 0, 17, "TOTAL", COLOR.PURPLE)
					receiver.drawScoreNum(engine, 0, 18, totalTime.toTimeStr, 2f)
					receiver.drawScoreFont(engine, 9, 17, "AVERAGE", COLOR.PURPLE)
					receiver.drawScoreNum(engine, 9, 18, (totalTime/SECTION_MAX).toTimeStr, 2f)

					receiver.drawScoreFont(engine, 0, 17, "F:VIEW RANKING", COLOR.GREEN)
				}
		} else {
			if(grade>=0&&grade<tableGradeName.size)
				receiver.drawScoreFont(engine, 0, 2, tableGradeName[grade], gradeflash>0&&gradeflash%4==0, 2f)

			// Score
			receiver.drawScoreFont(engine, 0, 5, "Score", COLOR.PURPLE)
			receiver.drawScoreNum(engine, 0, 6, "$scDisp"+"\n"+lastscore)

			receiver.drawScoreFont(engine, 0, 9, "Level", COLOR.PURPLE)
			receiver.drawScoreNum(engine, 1, 10, String.format("%3d", maxOf(engine.statistics.level, 0)))
			receiver.drawScoreSpeed(engine, 0, 11, if(engine.speed.gravity<0) 40 else engine.speed.gravity/128, 4)
			receiver.drawScoreNum(engine, 1, 12, String.format("%3d", nextseclv))

			receiver.drawScoreFont(engine, 0, 14, "Time", COLOR.PURPLE)
			receiver.drawScoreNum(engine, 0, 15, engine.statistics.time.toTimeStr, 2f)

			if(engine.gameActive&&engine.ending==2) {
				var time = ROLLTIMELIMIT-rolltime
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

				for(i in sectionTime.indices)
					if(sectionTime[i]>0) {
						var temp = i*100
						if(temp>999) temp = 999

						val section = engine.statistics.level/100
						var strSeparator = " "
						if(i==section&&engine.ending==0) strSeparator = "\u0082"

						val strSectionTime:String = String.format("%3d%s%s", temp, strSeparator, sectionTime[i].toTimeStr)

						receiver.drawScoreNum(engine, x, 3+i, strSectionTime, sectionIsNewRecord[i])
					}

				receiver.drawScoreFont(engine, x2, 17, "AVERAGE", COLOR.PURPLE)
				receiver.drawScoreNum(engine, x2, 18, (engine.statistics.time/(sectionscomp+1)).toTimeStr, 2f)
			}
		}
	}

	/** This function will be called when the piece is active */
	override fun onMove(engine:GameEngine):Boolean {
		if(engine.ending==0&&engine.statc[0]==0&&!engine.holdDisable&&!lvupflag) {
			if(engine.statistics.level<nextseclv-1) {
				engine.statistics.level++
				if(engine.statistics.level==nextseclv-1&&secAlert) engine.playSE("levelstop")
			}
			levelUp(engine)

			if(engine.timerActive&&medalRE<3) {
				val blocks = engine.field.howManyBlocks

				if(!recoveryFlag) {
					if(blocks>=150) recoveryFlag = true
				} else if(blocks<=70) {
					recoveryFlag = false
					engine.playSE("medal${++medalRE}")
				}
			}
		}
		if(engine.ending==0&&engine.statc[0]>0) lvupflag = false

		if(engine.ending==2&&!rollstarted) rollstarted = true

		return false
	}

	/** This function will be called during ARE */
	override fun onARE(engine:GameEngine):Boolean {
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

	/** Levelup */
	private fun levelUp(engine:GameEngine) {
		engine.meterValue = engine.statistics.level%100/99f
		engine.meterColor = GameEngine.METER_COLOR_GREEN
		if(engine.statistics.level%100>=50) engine.meterColor = GameEngine.METER_COLOR_YELLOW
		if(engine.statistics.level%100>=80) engine.meterColor = GameEngine.METER_COLOR_ORANGE
		if(engine.statistics.level>=nextseclv-1) engine.meterColor = GameEngine.METER_COLOR_RED

		setSpeed(engine)

		if(tableBGMFadeout[bgmLv]!=-1&&engine.statistics.level>=tableBGMFadeout[bgmLv]) owner.musMan.fadesw = true
	}

	/** Calculates line-clear score
	 * (This function will be called even if no lines are cleared) */
	override fun calcScore(engine:GameEngine, ev:ScoreEvent):Int {
		val li = ev.lines
		comboValue = if(li==0) 1
		else maxOf(1, comboValue+2*li-2)

		var spinTemp = engine.nowPieceSpinCount
		if(spinTemp>4) spinTemp = 4
		spinCount += spinTemp

		if(li>=1&&engine.ending==0) {
			if(li>=4) {
				sectionfourline++

				if(big) {
					if(engine.statistics.totalQuadruple==1||engine.statistics.totalQuadruple==2
						||engine.statistics.totalQuadruple==4
					) {
						engine.playSE("medal${++medalSK}")

					}
				} else if(engine.statistics.totalQuadruple==5||engine.statistics.totalQuadruple==10
					||engine.statistics.totalQuadruple==17
				) {
					engine.playSE("medal${++medalSK}")
				}
			}

			if(engine.field.isEmpty)
				if(medalAC<3) {
					engine.playSE("medal${++medalAC}")
				}

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
			} else if(engine.combo>=4&&medalCO<1) {
				engine.playSE("medal1")
				medalCO = 1
			} else if(engine.combo>=5&&medalCO<2) {
				engine.playSE("medal2")
				medalCO = 2
			} else if(engine.combo>=7&&medalCO<3) {
				engine.playSE("medal3")
				medalCO = 3
			}

			val levelb = engine.statistics.level
			engine.statistics.level += li
			levelUp(engine)

			if(engine.statistics.level>=999) {
				if(engine.timerActive) {
					sectionscomp++
					setAverageSectionTime()
				}

				engine.playSE("endingstart")
				engine.statistics.level = 999
				engine.timerActive = false
				engine.ending = 2
				rollclear = 1

				sectionlasttime = sectionTime[levelb/100]

				stMedalCheck(engine, levelb/100)

				roMedalCheck(engine)

				if(engine.statistics.totalQuadruple>=31&&gmfourline&&sectionfourline>=1) {
					grade = 6
					gradeflash = 180
				}
			} else if(nextseclv==300&&engine.statistics.level>=300&&engine.statistics.time>LV300TORIKAN) {
				if(engine.timerActive) {
					sectionscomp++
					setAverageSectionTime()
				}

				engine.playSE("endingstart")
				engine.statistics.level = 300
				engine.timerActive = false
				engine.ending = 2

				if(tableBGMChange[bgmLv]!=-1&&engine.statistics.level>=tableBGMChange[bgmLv]) {
					bgmLv++
					owner.musMan.fadesw = false
					owner.musMan.bgm = tableBGM[bgmLv]
				}

				sectionlasttime = sectionTime[levelb/100]

				stMedalCheck(engine, levelb/100)
			} else if(nextseclv==500&&engine.statistics.level>=500&&engine.statistics.time>LV500TORIKAN) {
				if(engine.timerActive) {
					sectionscomp++
					setAverageSectionTime()
				}

				engine.playSE("endingstart")
				engine.statistics.level = 500
				engine.timerActive = false
				engine.ending = 2

				if(tableBGMChange[bgmLv]!=-1&&engine.statistics.level>=tableBGMChange[bgmLv]) {
					bgmLv++
					owner.musMan.fadesw = false
					owner.musMan.bgm = tableBGM[bgmLv]
				}

				sectionlasttime = sectionTime[levelb/100]

				stMedalCheck(engine, levelb/100)
			} else if(nextseclv==800&&engine.statistics.level>=800&&engine.statistics.time>LV800TORIKAN) {
				if(engine.timerActive) {
					sectionscomp++
					setAverageSectionTime()
				}

				engine.playSE("endingstart")
				engine.statistics.level = 800
				engine.timerActive = false
				engine.ending = 2

				if(tableBGMChange[bgmLv]!=-1&&engine.statistics.level>=tableBGMChange[bgmLv]) {
					bgmLv++
					owner.musMan.fadesw = false
					owner.musMan.bgm = tableBGM[bgmLv]
				}

				sectionlasttime = sectionTime[levelb/100]

				stMedalCheck(engine, levelb/100)
			} else if(engine.statistics.level>=nextseclv) {

				owner.bgMan.fadesw = true
				owner.bgMan.fadecount = 0
				owner.bgMan.fadebg = nextseclv/100

				if(tableBGMChange[bgmLv]!=-1&&engine.statistics.level>=tableBGMChange[bgmLv]) {
					bgmLv++
					owner.musMan.fadesw = false
					owner.musMan.bgm = tableBGM[bgmLv]
					engine.playSE("levelup_section")
				}
				engine.playSE("levelup")

				sectionscomp++

				sectionlasttime = sectionTime[levelb/100]

				if(sectionfourline<2) gmfourline = false

				sectionfourline = 0

				stMedalCheck(engine, levelb/100)

				if(nextseclv==300||nextseclv==700) roMedalCheck(engine)

				if(startLevel==0)
					for(i in 0 until tableGradeLevel.size-1)
						if(engine.statistics.level>=tableGradeLevel[i]) {
							grade = i
							gradeflash = 180
						}

				nextseclv += 100
				if(nextseclv>999) nextseclv = 999
			} else if(engine.statistics.level==nextseclv-1&&secAlert) engine.playSE("levelstop")

			lastscore = ((((levelb+li)/4+engine.softdropFall+if(engine.manualLock) 1 else 0)*li*comboValue
				*if(engine.field.isEmpty) 4 else 1)
				+engine.statistics.level/2+maxOf(0, engine.lockDelay-engine.lockDelayNow)*7)
			engine.statistics.scoreLine += lastscore
			//return lastscore
		}
		return 0
	}

	/** This function will be called when the game timer updates */
	override fun onLast(engine:GameEngine) {
		super.onLast(engine)
		if(gradeflash>0) gradeflash--
		setHeboHidden(engine)
		if(engine.timerActive&&engine.ending==0) {
			val section = engine.statistics.level/100

			if(section>=0&&section<sectionTime.size) {
				sectionTime[section]++
				setAverageSectionTime()
			}
		}

		if(engine.gameActive&&engine.ending==2) {
			if(engine.ctrl.isPress(Controller.BUTTON_F)&&engine.statistics.level<999) rolltime += 5
			rolltime++

			val remainRollTime = ROLLTIMELIMIT-rolltime
			engine.meterValue = remainRollTime*1f/ROLLTIMELIMIT
			engine.meterColor = GameEngine.METER_COLOR_GREEN
			if(remainRollTime<=30*60) engine.meterColor = GameEngine.METER_COLOR_YELLOW
			if(remainRollTime<=20*60) engine.meterColor = GameEngine.METER_COLOR_ORANGE
			if(remainRollTime<=10*60) engine.meterColor = GameEngine.METER_COLOR_RED

			if(rolltime>=ROLLTIMELIMIT) {
				if(engine.statistics.level>=999) rollclear = 2

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
		receiver.drawMenuFont(engine, 0, 0, "\u0090\u0093 PAGE${(engine.statc[1]+1)}/3", COLOR.RED)

		if(engine.statc[1]==0) {
			var gcolor = COLOR.WHITE
			if(rollclear==1) gcolor = COLOR.GREEN
			if(rollclear==2) gcolor = COLOR.ORANGE
			receiver.drawMenuFont(engine, 0, 2, "GRADE", COLOR.PURPLE)
			val strGrade = String.format("%10s", tableGradeName[grade])
			receiver.drawMenuFont(engine, 0, 3, strGrade, gcolor)

			drawResultStats(
				engine, receiver, 4, COLOR.PURPLE, Statistic.SCORE, Statistic.LINES, Statistic.LEVEL_MANIA, Statistic.TIME
			)
			drawResultRank(engine, receiver, 12, COLOR.PURPLE, rankingRank)
			if(secretGrade>4)
				drawResult(
					engine, receiver, 15, COLOR.PURPLE, "S. GRADE",
					String.format("%10s", tableSecretGradeName[secretGrade-1])
				)
		} else if(engine.statc[1]==1) {
			receiver.drawMenuFont(engine, 0, 2, "SECTION", COLOR.PURPLE)

			for(i in sectionTime.indices)
				if(sectionTime[i]>0)
					receiver.drawMenuFont(engine, 2, 3+i, sectionTime[i].toTimeStr, sectionIsNewRecord[i])

			if(sectionavgtime>0) {
				receiver.drawMenuFont(engine, 0, 14, "AVERAGE", COLOR.PURPLE)
				receiver.drawMenuFont(engine, 2, 15, sectionavgtime.toTimeStr)
			}
		} else if(engine.statc[1]==2) {
			receiver.drawMenuFont(engine, 0, 2, "MEDAL", COLOR.PURPLE)
			getMedalFontColor(medalAC)?.let {receiver.drawMenuFont(engine, 5, 3, "AC", it)}
			getMedalFontColor(medalST)?.let {receiver.drawMenuFont(engine, 8, 3, "ST", it)}
			getMedalFontColor(medalSK)?.let {receiver.drawMenuFont(engine, 5, 4, "SK", it)}
			getMedalFontColor(medalRE)?.let {receiver.drawMenuFont(engine, 8, 4, "RE", it)}
			getMedalFontColor(medalRO)?.let {receiver.drawMenuFont(engine, 5, 5, "SK", it)}
			getMedalFontColor(medalCO)?.let {receiver.drawMenuFont(engine, 8, 5, "CO", it)}

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
		saveSetting(owner.replayProp, engine)
		owner.replayProp.setProperty("phantommania.version", version)

		if(!owner.replayMode&&startLevel==0&&!big&&engine.ai==null) {
//			owner.statsProp.setProperty("decoration", decoration)
			updateRanking(grade, engine.statistics.level, engine.statistics.time, rollclear)
			if(medalST==3) updateBestSectionTime()

			if(rankingRank!=-1||medalST==3) return true

		}
		return false
	}

	/** Update the ranking */
	private fun updateRanking(gr:Int, lv:Int, time:Int, clear:Int) {
		rankingRank = checkRanking(gr, lv, time, clear)

		if(rankingRank!=-1) {
			for(i in RANKING_MAX-1 downTo rankingRank+1) {
				rankingGrade[i] = rankingGrade[i-1]
				rankingLevel[i] = rankingLevel[i-1]
				rankingTime[i] = rankingTime[i-1]
				rankingRollclear[i] = rankingRollclear[i-1]
			}

			rankingGrade[rankingRank] = gr
			rankingLevel[rankingRank] = lv
			rankingTime[rankingRank] = time
			rankingRollclear[rankingRank] = clear
		}
	}

	/** This function will check the ranking and returns which place you are.
	 * (-1: Out of rank) */
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

	/** Updates best section time records */
	private fun updateBestSectionTime() {
		for(i in 0 until SECTION_MAX)
			if(sectionIsNewRecord[i]) bestSectionTime[i] = sectionTime[i]
	}

	companion object {
		/** Current version */
		private const val CURRENT_VERSION = 1

		/** ARE table */
		private val tableARE = intArrayOf(15, 11, 11, 5, 4, 3)

		/** ARE Line table */
		private val tableARELine = intArrayOf(11, 5, 5, 4, 4, 3)

		/** Line Delay table */
		private val tableLineDelay = intArrayOf(12, 6, 6, 7, 5, 4)

		/** Lock Delay table */
		private val tableLockDelay = intArrayOf(31, 29, 28, 27, 26, 25)

		/** DAS table */
		private val tableDAS = intArrayOf(11, 11, 10, 9, 7, 7)
		private val tableHiddenDelay = intArrayOf(360, 320, 256, 192, 160, 128)

		/** BGM fadeout level */
		private val tableBGMFadeout = intArrayOf(280, 480, -1)

		/** BGM change level */
		private val tableBGMChange = intArrayOf(300, 500, -1)
		private val tableBGM = arrayOf(BGM.GrandT(5), BGM.GrandT(4), BGM.GrandT(3), BGM.GrandA(3))
		/** Grade names */

		private val tableGradeName = arrayOf("", "m", "MK", "MV", "MO", "MM", "GM")

		/** Required level for grade */
		private val tableGradeLevel = intArrayOf(0, 500, 600, 700, 800, 900, 999)

		/** Secret grade names */
		private val tableSecretGradeName = arrayOf(
			"S1", "S2", "S3", "S4", "S5", "S6", "S7", "S8", "S9", //  0?` 8
			"M1", "M2", "M3", "M4", "M5", "M6", "M7", "M8", "M9", //  9?`17
			"GM" // 18
		)

		/** Ending time limit */
		private const val ROLLTIMELIMIT = 1982

		/** Number of hiscore records */
		private const val RANKING_MAX = 13

		/** Level 300 time limit */
		private const val LV300TORIKAN = 8880

		/** Level 500 time limit */
		private const val LV500TORIKAN = 13080

		/** Level 800 time limit */
		private const val LV800TORIKAN = 19380

		/** Number of sections */
		private const val SECTION_MAX = 10

		/** Default section time */
		private const val DEFAULT_SECTION_TIME = 3600
	}
}
