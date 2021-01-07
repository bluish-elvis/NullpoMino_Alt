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

/** SCORE ATTACK mode (Original from NullpoUE build 121909 by Zircean) */
class GrandFestival:AbstractMode() {

	/** GameManager object (Manages entire game status) */

	/** EventReceiver object (This receives many game events, can also be used
	 * for drawing the fonts.) */

	/** Current gravity index number (Increases when the level reaches to
	 * certain value that defined in tableGravityChangeLevel) */
	private var gravityindex:Int = 0

	/** Next section level */
	private var nextseclv:Int = 0

	/** Level up flag (Set to true when the level increases) */
	private var lvupflag:Boolean = false

	/** Used by Hard-drop scoring */
	private var harddropBonus:Int = 0

	/** Used by combo scoring */
	private var comboValue:Int = 0

	/** Amount of points you just get from line clears */
	private var lastscore:Int = 0
	private var hanabi:Int = 0
	private var temphanabi:Int = 0
	private var inthanabi:Int = 0
	private var bonusspeed:Int = 0
	private var bonusint:Int = 0
	private var halfminline:Int = 0
	private var halfminbonus:Boolean = false

	/** Elapsed time from last line clear */
	private var lastlinetime:Int = 0

	/** Elapsed time from last piece spawns */
	private var lastspawntime:Int = 0

	private var scgettime:Int = 0

	/** Remaining ending time limit */
	private var rolltime:Int = 0

	/** Secret Grade */
	private var secretGrade:Int = 0

	/** Current BGM number */
	private var bgmlv:Int = 0

	/** Section Record */
	private var sectionhanabi:IntArray = IntArray(SECTION_MAX+1)
	private var sectionscore:IntArray = IntArray(SECTION_MAX+1)
	private var sectionTime:IntArray = IntArray(SECTION_MAX+1)

	/** This will be true if the player achieves new section time record in
	 * specific section */
	private var sectionIsNewRecord:BooleanArray = BooleanArray(SECTION_MAX)

	/** This will be true if the player achieves new section time record
	 * somewhere */
	private var sectionAnyNewRecord:Boolean = false

	/** Amount of sections completed */
	private var sectionscomp:Int = 0

	/** Average section time */
	private var sectionavgtime:Int = 0

	/** false:Leaderboard, true:Section time record (Push F in settings screen
	 * to
	 * flip it) */
	private var isShowBestSectionTime:Boolean = false

	/** Selected start level */
	private var startlevel:Int = 0

	/** Always show ghost */
	private var alwaysghost:Boolean = false

	/** Always 20G */
	private var always20g:Boolean = false

	/** Big Mode */
	private var big:Boolean = false

	/** Show section time */
	private var showsectiontime:Boolean = false

	/** Version of this mode */
	private var version:Int = 0

	/** Your place on leaderboard (-1: out of rank) */
	private var rankingRank:Int = 0

	/** Score records */
	private var rankingScore:IntArray = IntArray(RANKING_MAX)
	private var rankingHanabi:IntArray = IntArray(RANKING_MAX)
	private var bestSectionScore:IntArray = IntArray(RANKING_MAX)
	private var bestSectionHanabi:IntArray = IntArray(RANKING_MAX)
	private var bestSectionTime:IntArray = IntArray(RANKING_MAX)

	/** Level records */
	private var rankingLevel:IntArray = IntArray(RANKING_MAX)

	/** Time records */
	private var rankingTime:IntArray = IntArray(RANKING_MAX)

	private var decoration:Int = 0
	private var dectemp:Int = 0

	/** Returns the name of this mode */
	override val name:String = "Grand Festival"

	/** This function will be called when the game enters the main game
	 * screen. */
	override fun playerInit(engine:GameEngine, playerID:Int) {
		super.playerInit(engine, playerID)
		menuTime = 0
		gravityindex = 0
		nextseclv = 0
		lvupflag = true
		comboValue = 0
		scgettime = 0
		lastscore = scgettime
		bonusint = 0
		bonusspeed = bonusint
		temphanabi = bonusspeed
		hanabi = temphanabi
		rolltime = 0
		bgmlv = 0
		halfminline = 0
		halfminbonus = false
		lastlinetime = 0
		lastspawntime = 0
		sectionhanabi = IntArray(SECTION_MAX+1)
		sectionscore = IntArray(SECTION_MAX+1)
		sectionTime = IntArray(SECTION_MAX)
		sectionIsNewRecord = BooleanArray(SECTION_MAX+1)
		sectionAnyNewRecord = false
		sectionavgtime = 0
		sectionscomp = sectionavgtime
		isShowBestSectionTime = false
		startlevel = 0
		big = false
		always20g = big
		alwaysghost = always20g
		showsectiontime = true

		decoration = 0
		dectemp = 0

		rankingRank = -1
		rankingScore = IntArray(RANKING_MAX)
		rankingHanabi = IntArray(RANKING_MAX)
		rankingLevel = IntArray(RANKING_MAX)
		rankingTime = IntArray(RANKING_MAX)
		bestSectionHanabi = IntArray(SECTION_MAX)
		bestSectionScore = IntArray(SECTION_MAX)
		bestSectionTime = IntArray(SECTION_MAX)

		engine.twistEnable = true
		engine.twistEnableEZ = true
		engine.twistminiType = GameEngine.TWISTMINI_TYPE_ROTATECHECK
		engine.b2bEnable = true
		engine.splitb2b = true
		engine.comboType = GameEngine.COMBO_TYPE_DOUBLE
		engine.bighalf = false
		engine.bigmove = false
		engine.staffrollNoDeath = true

		engine.speed.are = 25
		engine.speed.areLine = 25
		engine.speed.lineDelay = 40
		engine.speed.lockDelay = 30
		engine.speed.das = 15

		version = if(!owner.replayMode) {
			loadSetting(owner.modeConfig)
			loadRanking(owner.recordProp, engine.ruleopt.strRuleName)
			CURRENT_VERSION
		} else {
			loadSetting(owner.replayProp)
			owner.replayProp.getProperty("scoreattack.version", 0)
		}

		owner.backgroundStatus.bg = startlevel
	}

	/** Load the settings */
	override fun loadSetting(prop:CustomProperties) {
		startlevel = prop.getProperty("scoreattack.startlevel", 0)
		alwaysghost = prop.getProperty("scoreattack.alwaysghost", true)
		always20g = prop.getProperty("scoreattack.always20g", false)
		showsectiontime = prop.getProperty("scoreattack.showsectiontime", true)
		big = prop.getProperty("scoreattack.big", false)
		version = prop.getProperty("scoreattack.version", 0)
	}

	/** Save the settings */
	override fun saveSetting(prop:CustomProperties) {
		prop.setProperty("scoreattack.startlevel", startlevel)
		prop.setProperty("scoreattack.alwaysghost", alwaysghost)
		prop.setProperty("scoreattack.always20g", always20g)
		prop.setProperty("scoreattack.showsectiontime", showsectiontime)
		prop.setProperty("scoreattack.big", big)
		prop.setProperty("scoreattack.version", version)
	}

	/** Set the gravity speed
	 * @param engine GameEngine object
	 */
	private fun setSpeed(engine:GameEngine) {
		if(always20g)
			engine.speed.gravity = -1
		else {
			while(engine.statistics.level>=tableGravityChangeLevel[gravityindex])
				gravityindex++
			engine.speed.gravity = tableGravityValue[gravityindex]
		}
	}

	/** Calculates average section time */
	private fun setAverageSectionTime() {
		if(sectionscomp>0) {
			var temp = 0
			for(i in startlevel until startlevel+sectionscomp)
				temp += sectionTime[i]
			sectionavgtime = temp/sectionscomp
		} else
			sectionavgtime = 0
	}

	/** Best section time update check routine
	 * @param numsec Section Number
	 */
	private fun stNewRecordCheck(numsec:Int) {
		if(!owner.replayMode&&(sectionhanabi[numsec]>bestSectionHanabi[numsec]||sectionscore[numsec]>bestSectionScore[numsec])||(sectionhanabi[numsec]==bestSectionHanabi[numsec]&&sectionscore[numsec]==bestSectionScore[numsec]
				&&sectionTime[numsec]<bestSectionTime[numsec])) {
			sectionIsNewRecord[numsec] = true
			sectionAnyNewRecord = true
		}
	}

	/** Main routine for game setup screen */
	override fun onSetting(engine:GameEngine, playerID:Int):Boolean {
		if(!engine.owner.replayMode) {
			// Configuration changes
			val change = updateCursor(engine, 4)
			if(change!=0) {
				engine.playSE("change")

				when(menuCursor) {
					0 -> {
						startlevel += change
						if(startlevel<0) startlevel = 2
						if(startlevel>2) startlevel = 0
						owner.backgroundStatus.bg = startlevel
					}
					1 -> alwaysghost = !alwaysghost
					2 -> always20g = !always20g
					3 -> showsectiontime = !showsectiontime
					4 -> big = !big
				}
			}

			// Check for F button, when pressed this will flip Leaderboard/Best
			// Section Time Records
			if(engine.ctrl.isPush(Controller.BUTTON_F)&&menuTime>=5) {
				engine.playSE("change")
				isShowBestSectionTime = !isShowBestSectionTime
			}

			// Check for A button, when pressed this will begin the game
			if(engine.ctrl.isPush(Controller.BUTTON_A)&&menuTime>=5) {
				engine.playSE("decide")
				saveSetting(owner.modeConfig)
				owner.saveModeConfig()
				isShowBestSectionTime = false
				sectionscomp = 0
				return false
			}

			// Check for B button, when pressed this will shutdown the game
			// engine.
			if(engine.ctrl.isPush(Controller.BUTTON_B)) engine.quitflag = true

			menuTime++
		} else {
			menuTime++
			menuCursor = -1

			return menuTime<60
		}

		return true
	}

	/** Renders game setup screen */
	override fun renderSetting(engine:GameEngine, playerID:Int) {
		drawMenu(engine, playerID, receiver, 0, COLOR.BLUE, 0, "Level", (startlevel*100).toString(), "FULL GHOST",
			GeneralUtil.getONorOFF(alwaysghost), "20G MODE", GeneralUtil.getONorOFF(always20g), "SHOW SECT.",
			GeneralUtil.getONorOFF(showsectiontime), "BIG", GeneralUtil.getONorOFF(big))
	}

	/** This function will be called before the game actually begins (after
	 * Ready&Go screen disappears) */
	override fun startGame(engine:GameEngine, playerID:Int) {
		engine.statistics.level = startlevel*100

		nextseclv = engine.statistics.level+100
		if(engine.statistics.level<0) nextseclv = 100
		if(engine.statistics.level>=900) nextseclv = 999

		owner.backgroundStatus.bg = engine.statistics.level/100

		engine.big = big

		setSpeed(engine)
		bgmlv = if(engine.statistics.level<500) 0 else 1
		owner.bgmStatus.bgm = if(engine.statistics.level<500) BGM.GM_2(0) else BGM.GM_2(1)

	}

	/** Renders HUD (leaderboard or game statistics) */
	override fun renderLast(engine:GameEngine, playerID:Int) {
		receiver.drawScoreFont(engine, playerID, 0, 0, "GRAND FESTIVAL", COLOR.COBALT)
		receiver.drawScoreFont(engine, playerID, 1, 1, "SCORE ATTACK", COLOR.COBALT)

		receiver.drawScoreFont(engine, playerID, -1, -4*2, "DECORATION", scale = .5f)
		receiver.drawScoreBadges(engine, playerID, 0, -3, 100, decoration)
		receiver.drawScoreBadges(engine, playerID, 5, -4, 100, dectemp)
		if(engine.stat==GameEngine.Status.SETTING||engine.stat==GameEngine.Status.RESULT&&!owner.replayMode) {
			if(!owner.replayMode&&startlevel==0&&!big&&!always20g
				&&engine.ai==null)
				if(!isShowBestSectionTime) {
					// Score Leaderboard
					receiver.drawScoreFont(engine, playerID, 0, 2, "HANABI SCORE TIME", COLOR.BLUE)

					for(i in 0 until RANKING_MAX) {
						receiver.drawScoreNum(engine, playerID, 0, 3+i, String.format("%2d", i+1), COLOR.YELLOW)
						receiver.drawScoreNum(engine, playerID, 2, 3+i, "${rankingHanabi[i]}", i==rankingRank)
						receiver.drawScoreNum(engine, playerID, 6, 3+i, "${rankingScore[i]}", i==rankingRank)
						receiver.drawScoreNum(engine, playerID, 13, 3+i, GeneralUtil.getTime(rankingTime[i]), i==rankingRank)
					}

					receiver.drawScoreFont(engine, playerID, 0, 17, "F:VIEW SECTION TIME", COLOR.GREEN)
				} else {
					// Best Section Time Records
					receiver.drawScoreFont(engine, playerID, 0, 2, "SECTION SCORE TIME", COLOR.BLUE)

					var totalTime = 0
					var totalScore = 0
					var totalHanabi = 0
					for(i in 0 until SECTION_MAX) {
						val temp = i*100
						receiver.drawScoreNum(engine, playerID, 0, 3+i, String.format("%3d-", temp), sectionIsNewRecord[i])
						receiver.drawScoreNum(engine, playerID, 5, 3+i,
							String.format("%4d %6d %s", bestSectionHanabi[i], bestSectionScore[i],
								GeneralUtil.getTime(bestSectionTime[i])), sectionIsNewRecord[i])
						totalScore += bestSectionScore[i]
						totalHanabi += bestSectionHanabi[i]
						totalTime += bestSectionTime[i]
					}
					receiver.drawScoreFont(engine, playerID, 0, 5+SECTION_MAX, "TOTAL", COLOR.BLUE)
					receiver.drawScoreNum(engine, playerID, 5, 6+SECTION_MAX,
						String.format("%4d %6d %s", totalHanabi, totalScore, GeneralUtil.getTime(totalTime)))

					receiver.drawScoreFont(engine, playerID, 0, 7+SECTION_MAX, "AVERAGE", COLOR.BLUE)
					receiver.drawScoreNum(engine, playerID, 5, 8+SECTION_MAX,
						String.format("%4d %6d %s", totalHanabi/SECTION_MAX, totalScore/SECTION_MAX,
							GeneralUtil.getTime((totalTime/SECTION_MAX))))

					receiver.drawScoreFont(engine, playerID, 0, 17, "F:VIEW RANKING", COLOR.GREEN)
				}
		} else {
			val g20 = engine.speed.gravity<0&&rolltime%2==0
			receiver.drawScoreFont(engine, playerID, 0, 5, "Score", COLOR.BLUE)
			if(scgettime<engine.statistics.score) scgettime += ceil(((engine.statistics.score-scgettime)/10f).toDouble()).toInt()
			receiver.drawScoreNum(engine, playerID, 0, 6, "$scgettime", g20, 2f)
			receiver.drawScoreNum(engine, playerID, 5, 4, "$hanabi", g20||inthanabi>-100, 2f)

			receiver.drawScoreFont(engine, playerID, 0, 9, "Level", COLOR.BLUE)
			receiver.drawScoreNum(engine, playerID, 1, 10, String.format("%3d", maxOf(engine.statistics.level, 0)))
			receiver.drawSpeedMeter(engine, playerID, 0, 11,
				if(g20) 40 else floor(ln(engine.speed.gravity.toDouble())).toInt()*4, 4)
			receiver.drawScoreNum(engine, playerID, 1, 12, "300")

			receiver.drawScoreFont(engine, playerID, 0, 14, "Time", COLOR.BLUE)
			receiver.drawScoreNum(engine, playerID, 0, 15, GeneralUtil.getTime(engine.statistics.time), 2f)

			if(engine.gameActive&&engine.ending==2) {
				val time = maxOf(0, ROLLTIMELIMIT-rolltime)
				receiver.drawScoreFont(engine, playerID, 0, 17, "ROLL TIME", COLOR.BLUE)
				receiver.drawScoreNum(engine, playerID, 0, 18, GeneralUtil.getTime(time), time>0&&time<10*60, 2f)
			}

			// Section time
			if(showsectiontime&&sectionTime.isNotEmpty()) {
				val x = if(receiver.nextDisplayType==2) 8 else 12
				val x2 = if(receiver.nextDisplayType==2) 9 else 12

				receiver.drawScoreFont(engine, playerID, x-1, 2, "SECTION SCORE", COLOR.BLUE)

				for(i in sectionscore.indices)
					if(i<=sectionscomp) {
						var temp = i*100
						if(temp>=300) {
							temp = 300
							receiver.drawScoreFont(engine, playerID, x-1, 4+i, "BONUS", COLOR.BLUE)
							receiver.drawScoreNum(engine, playerID, x, 5+i,
								String.format("%4d %d", sectionhanabi[i+1], sectionscore[i+1]))

						}

						var strSeparator = "-"
						if(i==sectionscomp) strSeparator = "+"

						val strSection = String.format("%3d%s%4d %d", temp, strSeparator, sectionhanabi[i], sectionscore[i])

						receiver.drawScoreNum(engine, playerID, x, 3+i, strSection, sectionIsNewRecord[i])
					}

				receiver.drawScoreFont(engine, playerID, x2, 14, "AVERAGE", COLOR.BLUE)
				receiver.drawScoreNum(engine, playerID, x2, 15,
					GeneralUtil.getTime((engine.statistics.time/(sectionscomp+if(engine.ending==0) 1 else 0))), 2f)

			}
		}
	}

	/** This function will be called when the piece is active */
	override fun onMove(engine:GameEngine, playerID:Int):Boolean {
		if(lastspawntime<120) lastspawntime++
		if(engine.statc[0]==0&&!engine.holdDisable) {
			lastspawntime = 0
			if(engine.ending==0&&!lvupflag) {
				if(engine.statistics.level<299) engine.statistics.level++
				levelUp(engine)
			}
		}
		if(engine.ending==0&&engine.statc[0]>0) lvupflag = false

		return false
	}

	/** This function will be called during ARE */
	override fun onARE(engine:GameEngine, playerID:Int):Boolean {
		if(engine.ending==0&&engine.statc[0]>=engine.statc[1]-1&&!lvupflag) {
			if(engine.statistics.level<299) engine.statistics.level++
			levelUp(engine)
			lvupflag = true
		}

		return false
	}

	/** Levelup */
	private fun levelUp(engine:GameEngine) {
		engine.meterValue = engine.statistics.level%100*receiver.getMeterMax(engine)/99
		engine.meterColor = GameEngine.METER_COLOR_LEVEL

		if(engine.statistics.level>=nextseclv) {
			nextseclv += 100
			engine.playSE("levelup")

			// owner.backgroundStatus.fadesw = true;
			// owner.backgroundStatus.fadecount = 0;
			// owner.backgroundStatus.fadebg = nextseclv / 100;

			sectionscomp++
			setAverageSectionTime()
			stNewRecordCheck(sectionscomp-1)
		}

		setSpeed(engine)

		if(engine.statistics.level>=100&&!alwaysghost) engine.ghost = false

		if(bgmlv==0&&engine.statistics.level>=280&&engine.ending==0) owner.bgmStatus.fadesw = true
	}

	/** Calculates line-clear score (This function will be called even if no
	 * lines are cleared) */
	override fun calcScore(engine:GameEngine, playerID:Int, lines:Int):Int {

		comboValue = if(lines==0) 1
		else maxOf(1, comboValue+2*lines-2)

		if(lines>=1) {
			halfminline += lines
			val levelb = engine.statistics.level
			var indexcombo = engine.combo+if(engine.b2b) 0 else -1
			if(indexcombo<0) indexcombo = 0
			if(indexcombo>tableHanabiComboBonus.size-1) indexcombo = tableHanabiComboBonus.size-1
			val combobonus = tableHanabiComboBonus[indexcombo]

			if(engine.ending==0) {
				engine.statistics.level += lines
				levelUp(engine)

				if(engine.statistics.level>=300) {
					if(engine.timerActive) {
						val timebonus = (1253*ceil(maxOf(18000-engine.statistics.time, 0)/60.0)).toInt()
						sectionscore[SECTION_MAX] = timebonus
						engine.statistics.scoreBonus += timebonus
					}
					bonusspeed = 3265/maxOf(1, hanabi)
					bgmlv++
					owner.bgmStatus.fadesw = false
					owner.bgmStatus.bgm = BGM.GM_2(1)

					engine.statistics.level = 300
					engine.timerActive = false
					engine.ending = 2
					halfminbonus = true
					halfminline = 0
				} else if(owner.backgroundStatus.bg<(nextseclv-100)/100) {
					owner.backgroundStatus.fadesw = true
					owner.backgroundStatus.fadecount = 0
					owner.backgroundStatus.fadebg = (nextseclv-100)/100
				}
			}
			lastscore = 6*
				((((levelb+lines)/(if(engine.b2b) 3 else 4)+engine.softdropFall+(if(engine.manualLock) 1 else 0)+harddropBonus)
					*lines
					*comboValue*if(engine.field!!.isEmpty) 4 else 1)
					+engine.statistics.level/(if(engine.twist) 2 else 3)+maxOf(0, engine.lockDelay-engine.lockDelayNow)*7)
			// AC medal
			if(engine.field!!.isEmpty) {

				dectemp += lines*25
				if(lines==3) dectemp += 25
				if(lines==4) dectemp += 150
			}
			temphanabi += maxOf(1, (
				when(lines) {
					2 -> 2.9
					3 -> 3.8
					else -> if(lines>=4) 4.7 else 1.0
				}*combobonus*(if(engine.twist) 4.0 else if(engine.twistmini) 2.0 else 1.0)*(if(engine.split) 1.4 else 1.0)
					*(if(inthanabi>-100) 1.3 else 1.0)*(maxOf(engine.statistics.level-lastspawntime, 100)/100.0)
					*(maxOf(engine.statistics.level-lastspawntime, 120)/120.0)
					*(if(halfminbonus) 1.4 else 1.0)*(if(engine.ending==0&&(levelb%25==0||levelb==299)) 1.3 else 1.0)
				).toInt())
			halfminbonus = false
			lastlinetime = 0
			if(sectionscomp>=0&&sectionscomp<sectionscore.size) sectionscore[sectionscomp] += lastscore
			engine.statistics.scoreLine += lastscore
			return lastscore
		}
		return 0
	}

	/** This function will be called when hard-drop is used */
	override fun afterHardDropFall(engine:GameEngine, playerID:Int, fall:Int) {
		if(fall*2>harddropBonus) harddropBonus = fall*2
	}

	/** This function will be called when the game timer updates */
	override fun onLast(engine:GameEngine, playerID:Int) {
		if(lastlinetime<100) lastlinetime++
		if(inthanabi>-100) inthanabi--
		if(temphanabi>0&&inthanabi<=0) {
			receiver.shootFireworks(engine)
			hanabi++
			sectionhanabi[sectionscomp]++
			temphanabi--
			inthanabi += GameEngine.HANABI_INTERVAL-minOf(inthanabi, 0)
		}
		if(engine.temphanabi>0&&engine.inthanabi<=0) {
			hanabi++
			sectionhanabi[sectionscomp]++
		}
		// Increase section timer
		if(engine.timerActive&&engine.ending==0) {
			val section = engine.statistics.level/100
			if(section>=0&&section<sectionTime.size) sectionTime[section]++
			if(engine.statistics.time%1800==0) {
				if(halfminline>0) halfminbonus = true
				halfminline = 0
			}
		}

		// Increase ending timer
		if(engine.gameActive&&engine.ending==2) {
			rolltime++
			bonusint--
			if(bonusint<=0) {
				receiver.shootFireworks(engine)
				hanabi++
				sectionhanabi[SECTION_MAX]++
				bonusint += bonusspeed
			}
			val remainRollTime = ROLLTIMELIMIT-rolltime
			engine.meterValue = remainRollTime*receiver.getMeterMax(engine)/ROLLTIMELIMIT
			engine.meterColor = GameEngine.METER_COLOR_LIMIT

			if(rolltime>=ROLLTIMELIMIT) {
				engine.gameEnded()
				engine.resetStatc()
				engine.stat = GameEngine.Status.EXCELLENT
			}
		}
	}

	override fun onExcellent(engine:GameEngine, playerID:Int):Boolean {
		engine.statc[1] = temphanabi
		return super.onExcellent(engine, playerID)
	}

	/** This function will be called when the player tops out */
	override fun onGameOver(engine:GameEngine, playerID:Int):Boolean {
		if(engine.statc[0]==0) {
			secretGrade = engine.field!!.secretGrade
			inthanabi = 0
			temphanabi = inthanabi
			if(engine.ending==2) {
				dectemp += hanabi/150
				decoration += dectemp+secretGrade
			}
		}
		return false
	}

	/** Renders game result screen */
	override fun renderResult(engine:GameEngine, playerID:Int) {
		receiver.drawMenuFont(engine, playerID, 0, 0, "\u0090\u0093 PAGE${engine.statc[1]+1}/3", COLOR.RED)

		if(engine.statc[1]==0) {
			receiver.drawMenuNum(engine, playerID, 0, 2, String.format("%04d", hanabi), 2f)
			receiver.drawMenuFont(engine, playerID, 6, 3, "Score", COLOR.BLUE, .8f)
			receiver.drawMenuNum(engine, playerID, 0, 4, String.format("%7d", engine.statistics.score), 1.9f)
			drawResultStats(engine, playerID, receiver, 6, COLOR.BLUE, Statistic.LINES, Statistic.LEVEL, Statistic.TIME)
			drawResultRank(engine, playerID, receiver, 13, COLOR.BLUE, rankingRank)
			if(secretGrade>4)
				drawResult(engine, playerID, receiver, 15, COLOR.BLUE, "S. GRADE",
					String.format("%10s", tableSecretGradeName[secretGrade-1]))

		} else if(engine.statc[1]==1) {
			receiver.drawMenuFont(engine, playerID, 0, 2, "SECTION", COLOR.BLUE)
			receiver.drawMenuFont(engine, playerID, 0, 3, "Score", COLOR.BLUE)

			for(i in sectionscore.indices)
				receiver.drawMenuNum(engine, playerID, 0, (if(i==SECTION_MAX) 5 else 4)+i,
					String.format("%4d %d", sectionhanabi[i], sectionscore[i]), sectionIsNewRecord[i])
			receiver.drawMenuFont(engine, playerID, 0, 4+SECTION_MAX, "BONUS", COLOR.BLUE)

			receiver.drawMenuFont(engine, playerID, 0, 7+SECTION_MAX, "Time", COLOR.BLUE)
			for(i in sectionTime.indices)
				if(sectionTime[i]>0)
					receiver.drawMenuNum(engine, playerID, 2, 8+SECTION_MAX
						+i, GeneralUtil.getTime(sectionTime[i]))

			if(sectionavgtime>0) {
				receiver.drawMenuFont(engine, playerID, 0, 15, "AVERAGE", COLOR.BLUE)
				receiver.drawMenuNum(engine, playerID, 2, 16, GeneralUtil.getTime(sectionavgtime))
			}
		} else if(engine.statc[1]==2)
			drawResultStats(engine, playerID, receiver, 2, COLOR.BLUE, Statistic.LPM, Statistic.SPM, Statistic.PIECE,
				Statistic.PPS)
	}

	/** Additional routine for game result screen */
	override fun onResult(engine:GameEngine, playerID:Int):Boolean {
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
	override fun saveReplay(engine:GameEngine, playerID:Int, prop:CustomProperties) {
		saveSetting(prop)

		if(!owner.replayMode&&startlevel==0&&!always20g&&!big&&engine.ai==null) {
			updateRanking(engine.statistics.score, hanabi, engine.statistics.level, engine.statistics.time)
			if(sectionAnyNewRecord) updateBestSectionTime()

			if(rankingRank!=-1||sectionAnyNewRecord) {
				saveRanking(engine.ruleopt.strRuleName)
				owner.saveModeConfig()
			}
			owner.modeConfig.setProperty("decoration", decoration)
		}
	}

	/** Load the ranking */
	override fun loadRanking(prop:CustomProperties, ruleName:String) {
		for(i in 0 until RANKING_MAX) {
			rankingScore[i] = prop.getProperty("scoreattack.ranking.$ruleName.$i.score", 0)
			rankingHanabi[i] = prop.getProperty("scoreattack.ranking.$ruleName.hanabi.$i", 0)
			rankingLevel[i] = prop.getProperty("scoreattack.ranking.$ruleName.$i.level", 0)
			rankingTime[i] = prop.getProperty("scoreattack.ranking.$ruleName.$i.time", 0)
		}
		for(i in 0 until SECTION_MAX) {
			bestSectionHanabi[i] = prop.getProperty("$ruleName.sectionhanabi.$i", 0)
			bestSectionScore[i] = prop.getProperty("$ruleName.sectionscore.$i", 0)
			bestSectionTime[i] = prop.getProperty("$ruleName.sectiontime.$i",
				if(i==SECTION_MAX-1) ROLLTIMELIMIT else DEFAULT_SECTION_TIME)
		}
		decoration = owner.statsProp.getProperty("decoration", 0)
	}

	/** Save the ranking */
	fun saveRanking(ruleName:String) {
		super.saveRanking(ruleName, (0 until RANKING_MAX).flatMap {i ->
			listOf("$ruleName.$i.score" to rankingScore[i],
				"$ruleName.$i.hanabi" to rankingHanabi[i],
				"$ruleName.$i.level" to rankingLevel[i],
				"$ruleName.$i.time" to rankingTime[i])
		}+(0 until SECTION_MAX).flatMap {i ->
			listOf(
				"$ruleName.sectionscore.$i" to bestSectionScore[i],
				"$ruleName.sectionhanabi.$i" to bestSectionHanabi[i],
				"$ruleName.sectiontime.$i" to bestSectionTime[i])
		})

		owner.statsProp.setProperty("decoration", decoration)
		receiver.saveProperties(owner.statsFile, owner.statsProp)
	}

	/** Update the ranking */
	private fun updateRanking(sc:Int, fw:Int, lv:Int, time:Int) {
		rankingRank = checkRanking(sc, fw, lv, time)

		if(rankingRank!=-1) {
			for(i in RANKING_MAX-1 downTo rankingRank+1) {
				rankingScore[i] = rankingScore[i-1]
				rankingHanabi[i] = rankingHanabi[i-1]
				rankingLevel[i] = rankingLevel[i-1]
				rankingTime[i] = rankingTime[i-1]
			}

			rankingHanabi[rankingRank] = fw
			rankingScore[rankingRank] = sc
			rankingLevel[rankingRank] = lv
			rankingTime[rankingRank] = time
		}
	}

	/** This function will check the ranking and returns which place you are.
	 * (-1: Out of rank) */
	private fun checkRanking(sc:Int, fw:Int, lv:Int, time:Int):Int {
		for(i in 0 until RANKING_MAX)
			when {
				sc>rankingScore[i] -> return i
				fw>rankingHanabi[i] -> return i
				lv>rankingLevel[i] -> return i
				time<rankingTime[i] -> return i
			}

		return -1
	}

	/** Updates best section time records */
	private fun updateBestSectionTime() {
		for(i in 0 until SECTION_MAX)
			if(sectionIsNewRecord[i]) {
				bestSectionScore[i] = sectionscore[i]
				bestSectionHanabi[i] = sectionhanabi[i]
				bestSectionTime[i] = sectionTime[i]
			}
	}

	companion object {
		/** Current version */
		private const val CURRENT_VERSION = 0

		/** Gravity table (Gravity speed value) */
		private val tableGravityValue =
			intArrayOf(4, 5, 6, 8, 10, 12, 16, 32, 48, 64, 4, 5, 6, 8, 12, 32, 48, 80, 112, 128, 144, 16, 48, 80, 112, 144, 176,
				192, 208, 224, 240, -1)

		/** Gravity table (Gravity change level) */
		private val tableGravityChangeLevel =
			intArrayOf(8, 19, 35, 40, 50, 60, 70, 80, 90, 100, 108, 119, 125, 131, 139, 149, 146, 164, 174, 180, 200, 212, 221,
				232, 244, 256, 267, 277, 287, 295, 300, 10000)

		/** 段位 pointのCombo bonus */
		private val tableHanabiComboBonus = doubleArrayOf(1.0, 1.5, 1.9, 2.2, 2.9, 3.5, 3.9, 4.2, 4.5)

		/** Ending time limit */
		private const val ROLLTIMELIMIT = 3265

		/** Number of hiscore records */
		private const val RANKING_MAX = 10

		/** Secret grade names */
		private val tableSecretGradeName = arrayOf("S1", "S2", "S3", "S4", "S5", "S6", "S7", "S8", "S9", // 0-8
			"M1", "M2", "M3", "M4", "M5", "M6", "M7", "M8", "M9", // 9 - 17
			"GM" // 18
		)

		/** Number of sections */
		private const val SECTION_MAX = 4

		/** Default section time */
		private const val DEFAULT_SECTION_TIME = 6000
	}
}
