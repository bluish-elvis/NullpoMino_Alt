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
import mu.nu.nullpo.gui.common.BaseFont
import mu.nu.nullpo.util.CustomProperties
import mu.nu.nullpo.util.GeneralUtil.toInt
import mu.nu.nullpo.util.GeneralUtil.toTimeStr

/** FINAL mode (Original from NullpoUE build 010210 by Zircean) */
class GrandZ:AbstractMode() {
	private var gametype = 0
	private var joker = 0
	private var stacks = 0

	/** Next section level */
	private var nextSecLv = 0

	/** Level up flag (Set to true when the level increases) */
	private var lvupFlag = false

	/** Elapsed ending time */
	private var rollTime = 0

	/** Ending started flag */
	private var rollStarted = false

	/** Grade */
	private var grade = 0
	private var gradeinternal = 0

	/** Remaining frames of flashing grade display */
	private var gradeFlash = 0

	/** Used by combo scoring */
	private var comboValue = 0

	/** Game completed flag (0=Died before Lv999 1=Died during credits roll
	 * 2=Survived credits roll */
	private var rollClear = 0

	/** Secret Grade */
	private var secretGrade = 0

	/** Section Time */
	private val sectionTime = MutableList(SECTION_MAX) {0}
	private val sectionLine = MutableList(SECTION_MAX) {0}

	/** This will be true if the player achieves new section time record in
	 * specific section */
	private val sectionIsNewRecord = MutableList(SECTION_MAX) {false}

	/** Amount of sections completed */
	private var sectionsDone = 0

	/** Average section time */
	private val sectionAvgTime
		get() = sectionTime.filter {it>0}.average().toFloat()

	/** Current section time */
	private var sectionLastTime = 0

	/** AC medal */
	private var medalAC = 0

	/** ST medal */
	private var medalST = 0

	/** SK medal */
	private var medalSK = 0

	/** CO medal */
	private var medalCO = 0

	/** false:Leaderboard, true:Section time record (Push F in settings screen
	 * to
	 * flip it) */
	private var isShowBestSectionTime = false

	/** Selected start level */
	private var startLevel = 0

	/** Level stop sound */
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
	private val rankingGrade = List(RANKING_TYPE) {MutableList(RANKING_MAX) {0}}

	/** Level records */
	private val rankingLevel = List(RANKING_TYPE) {MutableList(RANKING_MAX) {0}}

	/** Time records */
	private val rankingTime = List(RANKING_TYPE) {MutableList(RANKING_MAX) {-1}}

	/** Game completed flag records */
	private val rankingRollClear = List(RANKING_TYPE) {MutableList(RANKING_MAX) {0}}

	/** Best section time records */
	private val bestSectionTime = List(RANKING_TYPE) {MutableList(SECTION_MAX) {DEFAULT_SECTION_TIME}}
	private val bestSectionLine = List(RANKING_TYPE) {MutableList(SECTION_MAX) {0}}

	override val rankMap
		get() = rankMapOf(
			rankingGrade.mapIndexed {a, x -> "$a.lines" to x}+
				rankingLevel.mapIndexed {a, x -> "$a.level" to x}+
				rankingTime.mapIndexed {a, x -> "$a.time" to x}+
				rankingRollClear.mapIndexed {a, x -> "$a.rollClear" to x}+
				bestSectionTime.mapIndexed {a, x -> "$a.section.time" to x}+
				bestSectionLine.mapIndexed {a, x -> "$a.section.line" to x}
		)
	/** Returns the name of this mode */
	override val name = "Grand Finale"
	override val gameIntensity = 3

	/** This function will be called when the game enters the main game
	 * screen. */
	override fun playerInit(engine:GameEngine) {
		super.playerInit(engine)
		stacks = 0
		joker = 0
		nextSecLv = 0
		lvupFlag = true
		rollTime = 0
		rollStarted = false
		gradeinternal = 0
		grade = 0
		gradeFlash = 0
		comboValue = 0
		lastScore = 0
		secretGrade = 0
		sectionTime.fill(0)
		sectionLine.fill(0)
		sectionIsNewRecord.fill(false)
		sectionsDone = 0
		sectionLastTime = 0
		medalAC = 0
		medalST = 0
		medalSK = 0
		medalCO = 0
		isShowBestSectionTime = false
		startLevel = 0
		secAlert = false
		big = false

		rankingRank = -1
		rankingGrade.forEach {it.fill(0)}
		rankingLevel.forEach {it.fill(0)}
		rankingTime.forEach {it.fill(-1)}
		rankingRollClear.forEach {it.fill(0)}
		bestSectionTime.forEach {it.fill(-1)}
		bestSectionLine.forEach {it.fill(0)}

		engine.twistEnable = true
		engine.b2bEnable = true
		engine.splitB2B = true
		engine.comboType = GameEngine.COMBO_TYPE_DOUBLE
		engine.frameColor = GameEngine.FRAME_COLOR_GRAY
		engine.bigHalf = true
		engine.bigMove = true
		engine.staffrollEnable = true
		engine.staffrollNoDeath = true

		version = (if(!owner.replayMode) CURRENT_VERSION else owner.replayProp.getProperty("final.version", 0))

		owner.bgMan.bg = 30+startLevel
	}

	override fun loadSetting(engine:GameEngine, prop:CustomProperties, ruleName:String, playerID:Int) {
		gametype = prop.getProperty("final.gametype", 0)
		startLevel = prop.getProperty("final.startLevel", 0)
		secAlert = prop.getProperty("final.lvstopse", true)
		showST = prop.getProperty("final.showsectiontime", true)
		big = prop.getProperty("final.big", false)
	}

	override fun saveSetting(engine:GameEngine, prop:CustomProperties, ruleName:String, playerID:Int) {
		prop.setProperty("final.gametype", gametype)
		prop.setProperty("final.startLevel", startLevel)
		prop.setProperty("final.lvstopse", secAlert)
		prop.setProperty("final.showsectiontime", showST)
		prop.setProperty("final.big", big)
	}

	/** Set the gravity speed
	 * @param engine GameEngine object
	 */
	private fun setSpeed(engine:GameEngine) {
		engine.speed.gravity = -1
		if(engine.ending==2) {
			engine.speed.are = 30
			engine.speed.areLine = 14
			engine.speed.lineDelay = 41
			engine.speed.lockDelay = 99
			engine.blockShowOutlineOnly = false
			engine.blockHidden = -1
		} else {
			var section = if(gametype==1) engine.statistics.level/40 else engine.statistics.level/100
			if(section>tableARE[gametype].size-1) section = tableARE[gametype].size-1
			engine.speed.are = tableARE[gametype][section]
			engine.speed.areLine = 0
			engine.speed.lineDelay = tableARE[gametype][section]
			engine.speed.lockDelay = tableLockDelay[gametype][section]
			if(!engine.ruleOpt.lockResetMove&&!engine.ruleOpt.lockResetSpin) engine.speed.lockDelay++
			engine.speed.das = 10-gametype*2

			engine.blockHidden = if(gametype==0) maxOf(engine.ruleOpt.lockFlash, tableHiddenDelay[section])
			else -1

			engine.blockShowOutlineOnly = true
		}
	}

	/** Checks ST medal
	 * @param engine GameEngine
	 * @param sectionNumber Section Number
	 */
	private fun stMedalCheck(engine:GameEngine, sectionNumber:Int) {
		val best = bestSectionTime[gametype][sectionNumber]

		if(sectionLastTime<best) {
			if(medalST<3) {
				engine.playSE("medal3")
				medalST = 3
			}
			if(!owner.replayMode) sectionIsNewRecord[sectionNumber] = true
		} else if(sectionLastTime<best+300&&medalST<2) {
			engine.playSE("medal2")
			medalST = 2
		} else if(sectionLastTime<best+600&&medalST<1) {
			engine.playSE("medal1")
			medalST = 1
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
						gametype += change
						if(gametype<0) gametype = 2
						if(gametype>2) gametype = 0
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
				return false
			}

			// Check for B button, when pressed this will shut down the game engine.
			if(engine.ctrl.isPush(Controller.BUTTON_B)) engine.quitFlag = true

			sectionsDone = 0
		} else {
			menuTime++
			menuCursor = -1
			owner.musMan.bgm = BGM.Finale(gametype)
			return menuTime<60
		}

		return true
	}

	/** Renders game setup screen */
	override fun renderSetting(engine:GameEngine) {
		drawMenu(
			engine, receiver, 0, COLOR.RED, 0, "COURSE" to tableModeName[gametype],
			"LVSTOPSE" to secAlert, "SHOW STIME" to showST, "BIG" to big
		)
	}

	/** Ready screen */
	override fun onReady(engine:GameEngine):Boolean {
		owner.musMan.bgm = BGM.Finale(gametype)
		return false
	}

	/** This function will be called before the game actually begins
	 * (after Ready&Go screen disappears) */
	override fun startGame(engine:GameEngine) {
		if(gametype==2)
			engine.statistics.level = 0
		else {
			engine.statistics.level = startLevel*100

			nextSecLv = engine.statistics.level+100
			if(engine.statistics.level<0) nextSecLv = 100
			if(engine.statistics.level>=900) nextSecLv = 999

			owner.bgMan.bg = 30+engine.statistics.level/100
		}
		engine.big = big

		engine.blockHiddenAnim = gametype==0
		engine.blockShowOutlineOnly = gametype>0

		engine.lives = MAX_LIVES[gametype]
		setSpeed(engine)
		stacks = 0
		joker = 0
	}

	/** Renders HUD (leaderboard or game statistics) */
	override fun renderLast(engine:GameEngine) {
		receiver.drawScoreFont(engine, 0, 0, name, COLOR.WHITE)

		receiver.drawScoreFont(engine, 0, 1, "b${tableModeName[gametype]}", COLOR.WHITE)

		if(engine.stat==GameEngine.Status.SETTING||engine.stat==GameEngine.Status.RESULT&&!owner.replayMode) {
			if(!owner.replayMode&&startLevel==0&&!big&&engine.ai==null)
				if(!isShowBestSectionTime) {
					// Leaderboard
					val topY = if(receiver.nextDisplayType==2) 5 else 3
					receiver.drawScoreFont(engine, 0, topY-1, "GRADE LV TIME", COLOR.RED)

					for(i in 0..<RANKING_MAX) {

						receiver.drawScoreGrade(engine, 0, topY+i, "%02d".format(i+1), COLOR.YELLOW)
						receiver.drawScoreGrade(
							engine, 3, topY+i, tableGradeName[minOf(tableGradeName.lastIndex, rankingGrade[gametype][i])],
							when {
								rankingRollClear[gametype][i]==1 -> COLOR.RED
								rankingRollClear[gametype][i]==2 -> COLOR.ORANGE
								else -> COLOR.WHITE
							}
						)
						receiver.drawScoreNum(engine, 5, topY+i, "%03d".format(rankingLevel[gametype][i]), i==rankingRank)
						receiver.drawScoreNum(engine, 8, topY+i, rankingTime[gametype][i].toTimeStr, i==rankingRank)
					}

					receiver.drawScoreFont(engine, 0, 19, "F:VIEW SECTION TIME", COLOR.ORANGE)
				} else {
					// Best section time records
					receiver.drawScoreFont(engine, 0, 2, "SECTION TIME", COLOR.RED)

					var totalTime = 0
					for(i in 0..<SECTION_MAX) {
						val temp = minOf(i*100, 999)
						val temp2 = minOf((i+1)*100-1, 999)

						val strSectionTime:String = String.format(
							"%3d-%3d %s %3d", temp, temp2,
							bestSectionTime[gametype][i].toTimeStr, bestSectionLine[gametype][i]
						)

						receiver.drawScoreNum(engine, 0, 3+i, strSectionTime, sectionIsNewRecord[i])

						totalTime += bestSectionTime[gametype][i]
					}

					receiver.drawScoreFont(engine, 0, 14, "TOTAL", COLOR.RED)
					receiver.drawScoreNum(engine, 0, 15, totalTime.toTimeStr, 2f)
					receiver.drawScoreFont(engine, 9, 14, "AVERAGE", COLOR.RED)
					receiver.drawScoreNum(engine, 9, 15, (totalTime*1f/SECTION_MAX).toTimeStr, 2f)

					receiver.drawScoreFont(engine, 0, 19, "F:VIEW RANKING", COLOR.ORANGE)
				}
		} else {
			val color:COLOR = COLOR.all[(engine.statistics.time+rollTime)%COLOR.all.size]
			// Grade
			if(grade>=1&&grade<tableGradeName.size)
				receiver.drawScoreGrade(engine, 0, 2, tableGradeName[grade], gradeFlash>0&&gradeFlash%4==0, 2f)

			// Time
			receiver.drawScoreFont(engine, 0, 4, "Time", color)
			if((engine.ending!=2) or (rollTime/10%2==0))
				receiver.drawScoreNum(engine, 0, 5, engine.statistics.time.toTimeStr, 2f)
			// Level
			receiver.drawScoreFont(engine, 0, 8, "Level", color)
			receiver.drawScoreNum(engine, 0, 9, "%3d".format(maxOf(engine.statistics.level, 0)))
			receiver.drawScoreSpeed(
				engine, 0, 10, if(gametype==1)
					if(engine.statistics.level<600) engine.statistics.level/600f else 1-(engine.statistics.level-600)/400f
				else engine.statistics.level/999f,
				4f
			)
			receiver.drawScoreNum(engine, 0, 11, "%3d".format(nextSecLv))
			// Lines
			receiver.drawScoreFont(
				engine, 0, 13, if(gametype==1) "JOKERS" else "Lines", if(gametype==1&&joker<=0) COLOR.WHITE else color
			)
			receiver.drawScoreNum(
				engine, 1, 14, "%3d".format(if(gametype==1) joker else engine.statistics.lines), 2f
			)
			if(gametype!=0)
				receiver.drawScoreSpeed(
					engine, 0, 16, if(gametype==1) 40-joker else engine.statistics.lines*40/FURTHEST_LINES,
					4
				)
			if(gametype==2)
				receiver.drawScoreNum(engine, 1, 17, "%3d".format(FURTHEST_LINES))

			// Remain roll time
			if(engine.gameActive&&engine.ending==2) {
				var time = ROLLTIMELIMIT-rollTime
				if(time<0) time = 0
				receiver.drawScoreFont(engine, 0, 17, "ROLL TIME", COLOR.RED)
				receiver.drawScoreNum(engine, 0, 18, time.toTimeStr, time>0&&time<10*60, 2f)
			}

			// Medals
			receiver.drawScoreMedal(engine, 0, 20, "AC", medalAC)
			receiver.drawScoreMedal(engine, 3, 20, "ST", medalST)
			receiver.drawScoreMedal(engine, 0, 21, "SK", medalSK)
			receiver.drawScoreMedal(engine, 3, 21, "CO", medalCO)

			// Section Time
			if(showST&&sectionTime.isNotEmpty()) {
				val x = if(receiver.nextDisplayType==2) 8 else 12
				val x2 = if(receiver.nextDisplayType==2) 9 else 12

				receiver.drawScoreFont(engine, x, 2, "SECTION TIME", COLOR.RED)

				for(i in sectionTime.indices)
					if(sectionTime[i]>0) {
						var temp = i*100
						if(temp>999) temp = 999

						val section = engine.statistics.level/100
						var strSeparator = "-"
						if(i==section&&engine.ending==0) strSeparator = "+"

						val strSectionTime:String = "%3d%s%s".format(temp, strSeparator, sectionTime[i].toTimeStr)

						receiver.drawScoreNum(engine, x, 3+i, strSectionTime, sectionIsNewRecord[i])
					}

				receiver.drawScoreFont(engine, x2, 17, "AVERAGE", COLOR.RED)
				receiver.drawScoreNum(engine, x2, 18, (engine.statistics.time/(sectionsDone+1)).toTimeStr, 2f)
			}
		}
	}

	/** This function will be called when the piece is active */
	override fun onMove(engine:GameEngine):Boolean {
		// New piece is active
		if(engine.ending==0&&engine.statc[0]==0&&gametype==0&&!engine.holdDisable&&!lvupFlag) {
			// Level up
			if(engine.statistics.level<nextSecLv-1) {
				engine.statistics.level++
				if(engine.statistics.level==nextSecLv-1&&secAlert) engine.playSE("levelstop")
			}
			levelUp(engine)
		}
		if(engine.ending==0&&engine.statc[0]>0&&(version>=2||!engine.holdDisable)) lvupFlag = false

		// Ending start
		if(engine.ending==2&&!rollStarted) {
			rollStarted = true
			owner.musMan.bgm = BGM.Ending(3)
		}

		return false
	}

	/** This function will be called during ARE */
	override fun onARE(engine:GameEngine):Boolean {
		// Last frame of ARE
		if(engine.ending==0&&engine.statc[0]>=engine.statc[1]-1&&gametype==0&&!lvupFlag) {
			if(engine.statistics.level<nextSecLv-1) {
				engine.statistics.level++
				if(engine.statistics.level==nextSecLv-1&&secAlert) engine.playSE("levelstop")
			}
			levelUp(engine)
			lvupFlag = true
		}

		return false
	}

	/** Levelup */
	private fun levelUp(engine:GameEngine) {
		// Meter
		engine.meterValue = engine.statistics.level%100/99f
		engine.meterColor = GameEngine.METER_COLOR_LEVEL

		// Update speed
		setSpeed(engine)
	}

	/** Calculates line-clear score
	 * (This function will be called even if no lines are cleared) */
	override fun calcScore(engine:GameEngine, ev:ScoreEvent):Int {
		val li = ev.lines
		if(engine.ending>0&&li>0)
			engine.tempHanabi += ((li*1.9f-.9f)*(if(grade==31) 3.5f else 1f+grade/10f)*
				(if(ev.twistMini) 2 else if(ev.twist) 4 else 1)*
				if(engine.lockDelay>engine.lockDelayNow) 1.3f else 1f).toInt()

		// Combo
		comboValue = if(li==0) 1
		else maxOf(1, comboValue+2*li-2)

		if(li>=1&&engine.ending==0) {
			if(gametype==2) {
				gradeinternal += li*(1+engine.lives)
				val gm = FURTHEST_LINES*(MAX_LIVES[2]+1)
				val gi = gradeinternal*31/gm
				if(gradeinternal>gm) gradeinternal = gm
				if(grade!=gi) {
					grade = gi
					gradeFlash = 180
					engine.playSE("cool")
				}
			}
			// 4 lines clear count
			if(li>=4) { // SK medal
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
			// AC medal
			if(engine.field.isEmpty)
				if(medalAC<3) {
					engine.playSE("medal${++medalAC}")
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
			} else if(engine.combo>=4&&medalCO<2) {
				engine.playSE("medal2")
				medalCO = 2
			} else if(engine.combo>=5&&medalCO<3) {
				engine.playSE("medal3")
				medalCO = 3
			}

			// Levelup
			val levelb = engine.statistics.level
			when(gametype) {
				1 -> {//joker {
					engine.statistics.level += 4
					val jo = li>=3||ev.twist||ev.split
					if(engine.statistics.level<600&&jo) joker++
					else if(engine.statistics.level>=600&&!jo) joker--
				}
				0 -> {
					engine.statistics.level += li
					if(li>2) engine.statistics.level += li-2
				}
				2 -> engine.statistics.level = engine.statistics.lines*999/FURTHEST_LINES
			}

			if((gametype==1&&engine.statistics.level>=1000&&joker<0)||
				gametype!=1&&engine.statistics.level>=999) {
				// Ending Start
				engine.playSE("endingstart")
				if(gametype==0) engine.statistics.level = 999
				engine.staffrollEnable = true
				engine.staffrollNoDeath = true
				engine.timerActive = false
				engine.ending = 2
				owner.musMan.bgm = BGM.Ending(3)
				rollClear = 1

				if(gametype==1&&(joker>0||engine.statistics.level>1000)) grade = 31
				else if(engine.lives>=MAX_LIVES[gametype]) grade = 31
				gradeFlash = 180

				// Records section time
				sectionLastTime = sectionTime[levelb/100]
				sectionsDone++

				// Check for ST medal
				stMedalCheck(engine, levelb/100)
			} else if(engine.statistics.level>=600&&gametype==1&&joker<0) {
				// ストック切れとりカン
				engine.gameEnded()
				engine.staffrollEnable = false
				engine.ending = 1
				rollClear = if(engine.lives==MAX_LIVES[gametype]) 2 else 1

				secretGrade = engine.field.secretGrade
				// Section Timeを記録
				sectionLastTime = sectionTime[levelb/100]
				sectionsDone++

				// ST medal
				stMedalCheck(engine, levelb/100)
			} else if(engine.statistics.level>=nextSecLv) {
				// Next section
				engine.playSE("levelup")

				// Change background image
				owner.bgMan.nextBg = 30+nextSecLv/100

				// Records section time
				sectionLastTime = sectionTime[levelb/100]
				sectionsDone++

				// Check for ST medal
				stMedalCheck(engine, levelb/100)
				// Grade Increase
				if(gametype==0) {
					val gm = SECTION_MAX*(MAX_LIVES[0]+1)
					gradeinternal = maxOf(gm, gradeinternal+1+engine.lives)
					val gi = gradeinternal*31/gm
					if(grade!=gi) {
						grade = gi
						gradeFlash = 180
						engine.playSE("cool")
					}
				}
				gradeFlash = 180

				// Update next section level
				nextSecLv += 100
				if(nextSecLv>999) nextSecLv = 999
			} else if(engine.statistics.level==nextSecLv-1&&secAlert) engine.playSE("levelstop")

			// Add score

			val section = levelb/100
			if(section>=0&&section<sectionTime.size) sectionLine[levelb/100] += if(gametype==1) (li>=4).toInt() else li
			lastScore = (((levelb+li)/4+engine.softdropFall+engine.manualLock.toInt())*li*comboValue+
				maxOf(0, engine.lockDelay-engine.lockDelayNow)+engine.statistics.level/2)*if(engine.field.isEmpty) 2 else 1

			engine.statistics.scoreLine += lastScore
			levelUp(engine)
		}
		return 0
	}

	/** This function will be called when the game timer updates */
	override fun onLast(engine:GameEngine) {
		super.onLast(engine)
		// Grade up flash
		if(gradeFlash>0) gradeFlash--

		// Increase section timer
		if(engine.timerActive&&engine.ending==0) {
			val section = engine.statistics.level/100

			if(section>=0&&section<sectionTime.size)
				sectionTime[section] = engine.statistics.time-sectionTime.take(section).sum()
			if(engine.statistics.level==nextSecLv-1)
				engine.meterColor = if(engine.meterColor==-0x1) -0x10000 else -0x1
		}

		// Engine
		if(engine.gameActive&&engine.ending==2) {
			rollTime++

			// Time meter
			val remainRollTime = ROLLTIMELIMIT-rollTime
			engine.meterValue = remainRollTime*1f/ROLLTIMELIMIT
			engine.meterColor = GameEngine.METER_COLOR_LIMIT

			// Player has survived the roll
			if(rollTime>=ROLLTIMELIMIT) {
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
		receiver.drawMenuFont(engine, 0, 0, "${BaseFont.UP_S}${BaseFont.DOWN_S} PAGE${engine.statc[1]+1}/3", COLOR.RED)

		if(engine.statc[1]==0) {
			if(grade>=1&&grade<tableGradeName.size) {
				val gcolor = when(rollClear) {
					1 -> COLOR.GREEN
					2 -> COLOR.ORANGE
					else -> COLOR.WHITE
				}
				receiver.drawMenuFont(engine, 0, 2, "GRADE", COLOR.RED)
				receiver.drawMenuGrade(engine, 6, 2, tableGradeName[grade], gcolor, 2f)
			}

			drawResultStats(
				engine, receiver, 4, COLOR.RED, Statistic.SCORE, Statistic.LINES, Statistic.LEVEL_MANIA, Statistic.TIME
			)
			if(secretGrade>4)
				drawResult(
					engine, receiver, 14, COLOR.RED, "S. GRADE",
					"%10s".format(tableSecretGradeName[secretGrade-1])
				)
		} else if(engine.statc[1]==1) {
			receiver.drawMenuFont(engine, 0, 2, "SECTION", COLOR.RED)

			for(i in sectionTime.indices)
				if(sectionTime[i]>0)
					receiver.drawMenuFont(engine, 2, 3+i, sectionTime[i].toTimeStr, sectionIsNewRecord[i])

			if(sectionAvgTime>0) {
				receiver.drawMenuFont(engine, 0, 14, "AVERAGE", COLOR.RED)
				receiver.drawMenuNum(engine, 2, 15, sectionAvgTime.toTimeStr)
			}
		} else if(engine.statc[1]==2) {
			receiver.drawMenuFont(engine, 0, 2, "MEDAL", COLOR.RED)
			receiver.drawMenuMedal(engine, 5, 3, "AC", medalAC)
			receiver.drawMenuMedal(engine, 8, 3, "ST", medalST)
			receiver.drawMenuMedal(engine, 5, 4, "SK", medalSK)
			receiver.drawMenuMedal(engine, 8, 4, "CO", medalCO)

			drawResultStats(engine, receiver, 6, COLOR.RED, Statistic.LPS, Statistic.SPS, Statistic.PIECE, Statistic.PPS)
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
		owner.replayProp.setProperty("final.version", version)

		// Updates leaderboard and best section time records
		if(!owner.replayMode&&startLevel==0&&!big&&engine.ai==null) {
			updateRanking(gametype, grade, engine.statistics.level, engine.statistics.time, rollClear)
			if(medalST==3) updateBestSectionTime(gametype)

			if(rankingRank!=-1||medalST==3) return true
		}
		return false
	}

	/** Save the ranking
	 * @param ruleName Rule name
	 */
	private fun saveRanking(ruleName:String) {
		super.saveRanking((0..<RANKING_TYPE).flatMap {j ->
			(0..<RANKING_MAX).flatMap {i ->
				listOf(
					"$j.$ruleName.$i.grade" to rankingGrade[j][i],
					"$j.$ruleName.$i.level" to rankingLevel[j][i],
					"$j.$ruleName.$i.time" to rankingTime[j][i],
					"$j.$ruleName.$i.clear" to rankingRollClear[j][i]
				)
			}+(0..<SECTION_MAX).flatMap {i ->
				listOf("$j,$ruleName.sectiontime.$i" to bestSectionTime[j][i])
			}
		})
	}

	/** Update the ranking
	 * @param gr Grade
	 * @param lv Level
	 * @param time time
	 * @param clear Game completed flag
	 */
	private fun updateRanking(type:Int, gr:Int, lv:Int, time:Int, clear:Int) {
		rankingRank = checkRanking(type, gr, lv, time, clear)

		if(rankingRank!=-1) {
			for(i in RANKING_MAX-1 downTo rankingRank+1) {
				rankingGrade[type][i] = rankingGrade[type][i-1]
				rankingLevel[type][i] = rankingLevel[type][i-1]
				rankingTime[type][i] = rankingTime[type][i-1]
				rankingRollClear[type][i] = rankingRollClear[type][i-1]
			}

			rankingGrade[type][rankingRank] = gr
			rankingLevel[type][rankingRank] = lv
			rankingTime[type][rankingRank] = time
			rankingRollClear[type][rankingRank] = clear
		}
	}

	/** This function will check the ranking and returns which place you are.
	 * @param gr Grade
	 * @param lv Level
	 * @param time Time
	 * @param clear Game completed flag
	 * @return Place (First place is 0. -1 is Out of Rank)
	 */
	private fun checkRanking(type:Int, gr:Int, lv:Int, time:Int, clear:Int):Int {
		for(i in 0..<RANKING_MAX)
			if(clear>rankingRollClear[type][i])
				return i
			else if(clear==rankingRollClear[type][i]&&gr>rankingGrade[type][i])
				return i
			else if(clear==rankingRollClear[type][i]&&gr==rankingGrade[type][i]&&lv>rankingLevel[type][i])
				return i
			else if(clear==rankingRollClear[type][i]&&gr==rankingGrade[type][i]&&lv==rankingLevel[type][i]
				&&time<rankingTime[type][i]
			)
				return i

		return -1
	}

	/** Updates best section time records */
	private fun updateBestSectionTime(t:Int) {
		for(i in 0..<SECTION_MAX)
			if(sectionIsNewRecord[i]) {
				bestSectionTime[t][i] = sectionTime[i]
				bestSectionLine[t][i] = sectionLine[i]
			}
	}

	companion object {
		/** Current version */
		private const val CURRENT_VERSION = 3

		/** ARE */
		private val tableARE =
			listOf(
				listOf(20, 19, 18, 17, 16, 15, 14, 13, 12, 11),
				listOf(
					15, 15, 15, 15, 14, 14, 13, 12, 11, 10, 9, 8, 7, 6, 5,
					15, 14, 13, 12, 11, 10, 9, 8, 7, 6, 5
				),
				listOf(10, 9, 8, 7, 6, 5, 4, 3, 2, 1)
			)

		/** Lock delay */
		private val tableLockDelay =
			listOf(
				listOf(25, 25, 24, 24, 23, 23, 22, 22, 21, 21),
				listOf(
					30, 29, 28, 27, 26, 25, 24, 23, 22, 21, 20, 19, 18, 17, 17,//0,40-599
					30, 28, 26, 24, 22, 20, 19, 18, 17, 16, 15//600,640-999
				),
				listOf(22, 22, 21, 21, 20, 20, 19, 18, 17, 16)
			)

		private val tableHiddenDelay = listOf(320, 300, 275, 250, 225, 200, 180, 150, 120, 60)
		/** Mode names */
		private val tableModeName = listOf("GENUINE", "BEST BOWER", "LONGOMINIAD")
		/** Grade names */
		private val tableGradeName =
			listOf(
				"", "S1", "S2", "S3", "S4", "S5", "S6", "S7", "S8", "S9", "S10", "m", "m1", "m2", "m3", "m4", "m5", "m6", "m7",
				"m8", "m9", "mK", "mV", "mO", "M", "MK", "MV", "MO", "MM", "Gm", "GM", "GOD"
			)

		/** Secret grade names */
		private val tableSecretGradeName = listOf(
			"M1", "M2", "M3", "M4", "M5", "M6", "M7", "M8", "M9", //  0～ 8
			"M10", "M11", "M12", "M13", "MK", "MV", "MO", "MM", "GM", //  9～17
			"GOD" // 18
		)

		/** Ending time limit */
		private const val ROLLTIMELIMIT = 12000

		/** Number of ranking records */
		private const val RANKING_MAX = 13
		/** Number of ranking types */
		private const val RANKING_TYPE = 3

		/** Number of sections */
		private const val SECTION_MAX = 10

		/** Default section time */
		private const val DEFAULT_SECTION_TIME = 4000

		/** Allowed Topouts */
		private val MAX_LIVES = listOf(4, 0, 2)

		/** goal of gamemode:1 */
		private val JOKER_SECTION_LEVEL = listOf(150, 250)
		/** goal of gamemode:2 */
		private const val FURTHEST_LINES = 300
	}
}
