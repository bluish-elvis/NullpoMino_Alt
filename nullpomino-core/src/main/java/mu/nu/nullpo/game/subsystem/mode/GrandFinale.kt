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

/** FINAL mode (Original from NullpoUE build 010210 by Zircean) */
class GrandFinale:AbstractMode() {

	private var gametype:Int = 0
	private var joker:Int = 0
	private var stacks:Int = 0

	/** Next section level */
	private var nextseclv:Int = 0

	/** Level up flag (Set to true when the level increases) */
	private var lvupflag:Boolean = false

	/** Elapsed ending time */
	private var rolltime:Int = 0

	/** Ending started flag */
	private var rollstarted:Boolean = false

	/** Grade */
	private var grade:Int = 0
	private var gradeinternal:Int = 0

	/** Remaining frames of flashing grade display */
	private var gradeflash:Int = 0

	/** Used by combo scoring */
	private var comboValue:Int = 0

	/** Amount of points you just get from line clears */
	private var lastscore:Int = 0

	/** Game completed flag (0=Died before Lv999 1=Died during credits roll
	 * 2=Survived credits roll */
	private var rollclear:Int = 0

	/** Secret Grade */
	private var secretGrade:Int = 0

	/** Section Time */
	private var sectionTime:IntArray = IntArray(SECTION_MAX)
	private var sectionLine:IntArray = IntArray(SECTION_MAX)

	/** This will be true if the player achieves new section time record in
	 * specific section */
	private var sectionIsNewRecord:BooleanArray = BooleanArray(SECTION_MAX)

	/** Amount of sections completed */
	private var sectionscomp:Int = 0

	/** Average section time */
	private var sectionavgtime:Int = 0

	/** Current section time */
	private var sectionlasttime:Int = 0

	/** AC medal */
	private var medalAC:Int = 0

	/** ST medal */
	private var medalST:Int = 0

	/** SK medal */
	private var medalSK:Int = 0

	/** CO medal */
	private var medalCO:Int = 0

	/** false:Leaderboard, true:Section time record (Push F in settings screen
	 * to
	 * flip it) */
	private var isShowBestSectionTime:Boolean = false

	/** Selected start level */
	private var startlevel:Int = 0

	/** Level stop sound */
	private var lvstopse:Boolean = false

	/** Big mode ON/OFF */
	private var big:Boolean = false

	/** Show section time */
	private var showsectiontime:Boolean = false

	/** Version of this mode */
	private var version:Int = 0

	/** Your place on leaderboard (-1: out of rank) */
	private var rankingRank:Int = 0

	/** Grade records */
	private var rankingGrade:Array<IntArray> = Array(RANKING_TYPE) {IntArray(RANKING_MAX)}

	/** Level records */
	private var rankingLevel:Array<IntArray> = Array(RANKING_TYPE) {IntArray(RANKING_MAX)}

	/** Time records */
	private var rankingTime:Array<IntArray> = Array(RANKING_TYPE) {IntArray(RANKING_MAX)}

	/** Game completed flag records */
	private var rankingRollclear:Array<IntArray> = Array(RANKING_TYPE) {IntArray(RANKING_MAX)}

	/** Best section time records */
	private var bestSectionTime:Array<IntArray> = Array(RANKING_TYPE) {IntArray(SECTION_MAX)}
	private var bestSectionLine:Array<IntArray> = Array(RANKING_TYPE) {IntArray(SECTION_MAX)}

	/** Returns the name of this mode */
	override val name:String
		get() = "GRAND FINALE"

	/** This function will be called when the game enters the main game
	 * screen. */
	override fun playerInit(engine:GameEngine, playerID:Int) {
		super.playerInit(engine, playerID)
		stacks = 0
		joker = stacks
		nextseclv = 0
		lvupflag = true
		rolltime = 0
		rollstarted = false
		gradeinternal = 0
		grade = gradeinternal
		gradeflash = 0
		comboValue = 0
		lastscore = 0
		secretGrade = 0
		sectionTime = IntArray(SECTION_MAX)
		sectionLine = IntArray(SECTION_MAX)
		sectionIsNewRecord = BooleanArray(SECTION_MAX)
		sectionscomp = 0
		sectionavgtime = 0
		sectionlasttime = 0
		medalAC = 0
		medalST = 0
		medalSK = 0
		medalCO = 0
		isShowBestSectionTime = false
		startlevel = 0
		lvstopse = false
		big = false
		menuTime = 0

		rankingRank = -1
		rankingGrade = Array(RANKING_TYPE) {IntArray(RANKING_MAX)}
		rankingLevel = Array(RANKING_TYPE) {IntArray(RANKING_MAX)}
		rankingTime = Array(RANKING_TYPE) {IntArray(RANKING_MAX)}
		rankingRollclear = Array(RANKING_TYPE) {IntArray(RANKING_MAX)}
		bestSectionTime = Array(RANKING_TYPE) {IntArray(SECTION_MAX)}
		bestSectionLine = Array(RANKING_TYPE) {IntArray(SECTION_MAX)}

		engine.tspinEnable = false
		engine.b2bEnable = false
		engine.comboType = GameEngine.COMBO_TYPE_DOUBLE
		engine.framecolor = GameEngine.FRAME_COLOR_GRAY
		engine.bighalf = true
		engine.bigmove = true
		engine.staffrollEnable = true
		engine.staffrollNoDeath = true

		version = if(!owner.replayMode) {
			loadSetting(owner.modeConfig)
			loadRanking(owner.modeConfig, engine.ruleopt.strRuleName)
			CURRENT_VERSION
		} else {
			loadSetting(owner.replayProp)
			owner.replayProp.getProperty("final.version", 0)
		}

		owner.backgroundStatus.bg = 30+startlevel
	}

	/** Load the settings
	 * @param prop CustomProperties
	 */
	override fun loadSetting(prop:CustomProperties) {
		gametype = prop.getProperty("final.gametype", 0)
		startlevel = prop.getProperty("final.startlevel", 0)
		lvstopse = prop.getProperty("final.lvstopse", true)
		showsectiontime = prop.getProperty("final.showsectiontime", true)
		big = prop.getProperty("final.big", false)
	}

	/** Save the settings
	 * @param prop CustomProperties
	 */
	override fun saveSetting(prop:CustomProperties) {
		prop.setProperty("final.gametype", gametype)
		prop.setProperty("final.startlevel", startlevel)
		prop.setProperty("final.lvstopse", lvstopse)
		prop.setProperty("final.showsectiontime", showsectiontime)
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
			var section = engine.statistics.level/100
			if(section>tableARE[gametype].size-1) section = tableARE[gametype].size-1
			engine.speed.are = tableARE[gametype][section]
			engine.speed.areLine = 0
			engine.speed.lineDelay = tableARE[gametype][section]
			engine.speed.lockDelay = tableLockDelay[gametype][section]
			if(!engine.ruleopt.lockresetMove&&!engine.ruleopt.lockresetRotate) engine.speed.lockDelay++
			engine.speed.das = 8-gametype*2
			if(gametype==0) {
				engine.blockHidden = tableHiddenDelay[section]
				if(engine.blockHidden<engine.ruleopt.lockflash) engine.blockHidden = engine.ruleopt.lockflash
			} else
				engine.blockHidden = -1

			engine.blockShowOutlineOnly = true
		}
	}

	/** Calculates the average section time */
	private fun setAverageSectionTime() {
		if(sectionscomp>0) {
			var temp = 0
			for(i in startlevel until startlevel+sectionscomp)
				if(i>=0&&i<sectionTime.size) temp += sectionTime[i]
			sectionavgtime = temp/sectionscomp
		} else
			sectionavgtime = 0
	}

	/** Checks ST medal
	 * @param engine GameEngine
	 * @param sectionNumber Section Number
	 */
	private fun stMedalCheck(engine:GameEngine, sectionNumber:Int) {
		val best = bestSectionTime[gametype][sectionNumber]

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


	/** Main routine for game setup screen */
	override fun onSetting(engine:GameEngine, playerID:Int):Boolean {
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
					1 -> lvstopse = !lvstopse
					2 -> showsectiontime = !showsectiontime
					3 -> big = !big
				}
			}

			// Check for F button, when pressed this will flip Leaderboard/Best Section Time Records
			if(engine.ctrl!!.isPush(Controller.BUTTON_F)&&menuTime>=5) {
				engine.playSE("change")
				isShowBestSectionTime = !isShowBestSectionTime
			}

			// Check for A button, when pressed this will begin the game
			if(engine.ctrl!!.isPush(Controller.BUTTON_A)&&menuTime>=5) {
				engine.playSE("decide")
				saveSetting(owner.modeConfig)
				receiver.saveModeConfig(owner.modeConfig)
				return false
			}

			// Check for B button, when pressed this will shutdown the game engine.
			if(engine.ctrl!!.isPush(Controller.BUTTON_B)) engine.quitflag = true

			sectionscomp = 0

			menuTime++
		} else {
			menuTime++
			menuCursor = -1
			owner.bgmStatus.bgm = BGM.FINALE(gametype)
			return menuTime<60
		}

		return true
	}

	/** Renders game setup screen */
	override fun renderSetting(engine:GameEngine, playerID:Int) {
		drawMenu(engine, playerID, receiver, 0, COLOR.RED, 0,
			"COURSE", tableModeName[gametype],
			"LVSTOPSE", GeneralUtil.getONorOFF(lvstopse),
			"SHOW STIME", GeneralUtil.getONorOFF(showsectiontime), "BIG", GeneralUtil.getONorOFF(big))
	}

	/** Ready screen */
	override fun onReady(engine:GameEngine, playerID:Int):Boolean {

		owner.bgmStatus.bgm = BGM.FINALE(gametype)
		return false
	}

	/** This function will be called before the game actually begins (after
	 * Ready&Go screen disappears) */
	override fun startGame(engine:GameEngine, playerID:Int) {
		if(gametype==2)
			engine.statistics.level = 0
		else {
			engine.statistics.level = startlevel*100

			nextseclv = engine.statistics.level+100
			if(engine.statistics.level<0) nextseclv = 100
			if(engine.statistics.level>=900) nextseclv = 999

			owner.backgroundStatus.bg = 30+engine.statistics.level/100
		}
		engine.big = big

		engine.blockHiddenAnim = gametype==0
		engine.blockShowOutlineOnly = gametype>0

		engine.lives = MAX_LIVES[gametype]
		setSpeed(engine)
		stacks = 0
		joker = stacks
	}

	/** Renders HUD (leaderboard or game statistics) */
	override fun renderLast(engine:GameEngine, playerID:Int) {
		receiver.drawScoreFont(engine, playerID, 0, 0, "GRAND FINALE", COLOR.WHITE)

		receiver.drawScoreFont(engine, playerID, 0, 1, "b${tableModeName[gametype]}", COLOR.WHITE)

		if(engine.stat==GameEngine.Status.SETTING||engine.stat==GameEngine.Status.RESULT&&!owner.replayMode) {
			if(!owner.replayMode&&startlevel==0&&!big&&engine.ai==null)
				if(!isShowBestSectionTime) {
					// Leaderboard
					val scale = if(receiver.nextDisplayType==2) .5f else 1f
					val topY = if(receiver.nextDisplayType==2) 5 else 3
					receiver.drawScoreFont(engine, playerID, 3, topY-1, "GRADE LEVEL TIME", COLOR.RED, scale)

					for(i in 0 until RANKING_MAX) {
						var gcolor = COLOR.WHITE
						if(rankingRollclear[gametype][i]==1) gcolor = COLOR.RED
						if(rankingRollclear[gametype][i]==2) gcolor = COLOR.ORANGE

						receiver.drawScoreNum(engine, playerID, 0, topY+i, String.format("%02d", i+1), COLOR.YELLOW, scale)
						receiver.drawScoreGrade(engine, playerID, 3, topY+i, tableGradeName[rankingGrade[gametype][i]], gcolor, scale)
						receiver.drawScoreNum(engine, playerID, 9, topY+i, String.format("%03d", rankingLevel[gametype][i]), i==rankingRank, scale)
						receiver.drawScoreNum(engine, playerID, 15, topY+i, GeneralUtil.getTime(rankingTime[gametype][i]), i==rankingRank, scale)
					}

					receiver.drawScoreFont(engine, playerID, 0, 17, "F:VIEW SECTION TIME", COLOR.ORANGE)
				} else {
					// Best section time records
					receiver.drawScoreFont(engine, playerID, 0, 2, "SECTION TIME", COLOR.RED)

					var totalTime = 0
					for(i in 0 until SECTION_MAX) {
						val temp = minOf(i*100, 999)
						val temp2 = minOf((i+1)*100-1, 999)

						val strSectionTime:String
						strSectionTime =
							String.format("%3d-%3d %s %3d", temp, temp2, GeneralUtil.getTime(bestSectionTime[gametype][i]), bestSectionLine[gametype][i])

						receiver.drawScoreNum(engine, playerID, 0, 3+i, strSectionTime, sectionIsNewRecord[i])

						totalTime += bestSectionTime[gametype][i]
					}

					receiver.drawScoreFont(engine, playerID, 0, 14, "TOTAL", COLOR.RED)
					receiver.drawScoreNum(engine, playerID, 0, 15, GeneralUtil.getTime(totalTime), 2f)
					receiver.drawScoreFont(engine, playerID, 9, 14, "AVERAGE", COLOR.RED)
					receiver.drawScoreNum(engine, playerID, 9, 15, GeneralUtil.getTime(totalTime*1f/SECTION_MAX), 2f)

					receiver.drawScoreFont(engine, playerID, 0, 17, "F:VIEW RANKING", COLOR.ORANGE)
				}
		} else {
			val color:COLOR = COLOR.values()[(engine.statistics.time+rolltime)%COLOR.values().size]
			// Grade
			if(grade>=1&&grade<tableGradeName.size)
				receiver.drawScoreGrade(engine, playerID, 0, 2, tableGradeName[grade], gradeflash>0&&gradeflash%4==0, 2f)

			// Time
			receiver.drawScoreFont(engine, playerID, 0, 4, "TIME", color)
			if((engine.ending!=2) or (rolltime/10%2==0))
				receiver.drawScoreNum(engine, playerID, 0, 5, GeneralUtil.getTime(engine.statistics.time), 2f)
			// Level
			receiver.drawScoreFont(engine, playerID, 0, 8, "LEVEL", color)
			receiver.drawScoreNum(engine, playerID, 0, 9, String.format("%3d", maxOf(engine.statistics.level, 0)))
			receiver.drawSpeedMeter(engine, playerID, 0, 10,
				if(gametype==1)
					if(engine.statistics.level<500) engine.statistics.level*20/500 else 80-engine.statistics.level*80/999
				else
					engine.statistics.level*40/999)
			receiver.drawScoreNum(engine, playerID, 0, 11, String.format("%3d", nextseclv))
			// Lines
			receiver.drawScoreFont(engine, playerID, 0, 13, if(gametype==1) "JOKERS" else "LINES",
				if(gametype==1&&joker<=0) COLOR.WHITE else color)
			receiver.drawScoreNum(engine, playerID, 0, 14, String.format("%3d",
				if(gametype==1) joker else engine.statistics.lines), 2f)
			if(gametype!=0)
				receiver.drawSpeedMeter(engine, playerID, 1, 16,
					if(gametype==1) 40-joker else engine.statistics.lines*40/FURTHEST_LINES)
			if(gametype==2)
				receiver.drawScoreNum(engine, playerID, 2, 17, String.format("%3d", FURTHEST_LINES))

			// Remain roll time
			if(engine.gameActive&&engine.ending==2) {
				var time = ROLLTIMELIMIT-rolltime
				if(time<0) time = 0
				receiver.drawScoreFont(engine, playerID, 0, 17, "ROLL TIME", COLOR.RED)
				receiver.drawScoreNum(engine, playerID, 0, 18, GeneralUtil.getTime(time), time>0&&time<10*60, 2f)
			}

			// Medals
			receiver.drawScoreMedal(engine, playerID, 0, 20, "AC", medalAC)
			receiver.drawScoreMedal(engine, playerID, 3, 20, "ST", medalST)
			receiver.drawScoreMedal(engine, playerID, 0, 21, "SK", medalSK)
			receiver.drawScoreMedal(engine, playerID, 3, 21, "CO", medalCO)

			// Section Time
			if(showsectiontime&&sectionTime.isNotEmpty()) {
				val x = if(receiver.nextDisplayType==2) 8 else 12
				val x2 = if(receiver.nextDisplayType==2) 9 else 12

				receiver.drawScoreFont(engine, playerID, x, 2, "SECTION TIME", COLOR.RED)

				for(i in sectionTime.indices)
					if(sectionTime[i]>0) {
						var temp = i*100
						if(temp>999) temp = 999

						val section = engine.statistics.level/100
						var strSeparator = "-"
						if(i==section&&engine.ending==0) strSeparator = "+"

						val strSectionTime:String
						strSectionTime = String.format("%3d%s%s", temp, strSeparator, GeneralUtil.getTime(sectionTime[i]))

						receiver.drawScoreNum(engine, playerID, x, 3+i, strSectionTime, sectionIsNewRecord[i])
					}

				receiver.drawScoreFont(engine, playerID, x2, 17, "AVERAGE", COLOR.RED)
				receiver.drawScoreNum(engine, playerID, x2, 18, GeneralUtil.getTime((engine.statistics.time/(sectionscomp+1))), 2f)
			}
		}
	}

	/** This function will be called when the piece is active */
	override fun onMove(engine:GameEngine, playerID:Int):Boolean {
		// New piece is active
		if(engine.ending==0&&engine.statc[0]==0&&!engine.holdDisable&&!lvupflag) {
			// Level up
			if(gametype==1&&engine.statistics.level>=500&&stacks<40) stacks++
			if(isLeveledPerBlock(engine.statistics.level)) {
				engine.statistics.level++
				if(engine.statistics.level==nextseclv-1&&lvstopse) engine.playSE("levelstop")
			}
			levelUp(engine)
		}
		if(engine.ending==0&&engine.statc[0]>0&&(version>=2||!engine.holdDisable)) lvupflag = false

		// Ending start
		if(engine.ending==2&&!rollstarted) {
			rollstarted = true
			owner.bgmStatus.bgm = BGM.ENDING(3)
		}

		return false
	}

	/** This function will be called during ARE */
	override fun onARE(engine:GameEngine, playerID:Int):Boolean {
		// Last frame of ARE
		if(engine.ending==0&&engine.statc[0]>=engine.statc[1]-1&&!lvupflag) {
			if(gametype==1&&engine.statistics.level>=500&&stacks<40) stacks++
			if(isLeveledPerBlock(engine.statistics.level)) {
				engine.statistics.level++
				if(engine.statistics.level==nextseclv-1&&lvstopse) engine.playSE("levelstop")
			}
			levelUp(engine)
			lvupflag = true
		}

		return false
	}

	private fun isLeveledPerBlock(level:Int):Boolean = level<nextseclv-1&&(gametype==0||gametype==1&&level<499)

	/** Levelup */
	private fun levelUp(engine:GameEngine) {
		// Meter
		engine.meterValue = engine.statistics.level%100*receiver.getMeterMax(engine)/99
		engine.meterColor = GameEngine.METER_COLOR_LEVEL

		// Update speed
		setSpeed(engine)
	}

	/** Calculates line-clear score
	 * (This function will be called even if no lines are cleared) */
	override fun calcScore(engine:GameEngine, playerID:Int, lines:Int) {
		if(engine.ending>0&&lines>0)
			engine.temphanabi += ((lines*1.9-.9)*(if(grade==31) 3.5 else 1.0+grade/10.0)*
				(if(engine.tspin) 4.0 else if(engine.tspinmini) 2.0 else 1.0)
				*if(engine.lockDelay>engine.lockDelayNow) 1.3 else 1.0).toInt()

		// Combo
		if(lines==0)
			comboValue = 1
		else {
			comboValue = comboValue+2*lines-2
			if(comboValue<1) comboValue = 1
		}

		if(lines>=1&&engine.ending==0) {
			if(gametype==2) {
				gradeinternal += lines*(1+engine.lives)
				val gm = FURTHEST_LINES*(MAX_LIVES[2]+1)
				val gi = gradeinternal*31/gm
				if(gradeinternal>gm) gradeinternal = gm
				if(grade!=gi) {
					grade = gi
					gradeflash = 180
					engine.playSE("cool")
				}
			}
			// 4 lines clear count
			if(lines>=4) { // SK medal
				if(big) {
					if(engine.statistics.totalQuadruple==1||engine.statistics.totalQuadruple==2
						||engine.statistics.totalQuadruple==4) {
						engine.playSE("medal${++medalSK}")
					}
				} else if(engine.statistics.totalQuadruple==5||engine.statistics.totalQuadruple==10
					||engine.statistics.totalQuadruple==17) {
					engine.playSE("medal${++medalSK}")
				}
			}
			// AC medal
			if(engine.field!!.isEmpty)
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
				1 -> {//joker
					if(lines>=1) engine.statistics.level++
					if(lines>=3) joker++
					if(engine.statistics.level>=500&&lines>0&&joker>0) {
						engine.statistics.level += stacks
						stacks = 0
						if(lines<=3||engine.split) joker--

					}
					if(engine.statistics.level<500||joker>0){
						engine.statistics.level += lines
						if(lines>2) engine.statistics.level += lines-2
					}
				}
				0 -> {
					engine.statistics.level += lines
					if(lines>2) engine.statistics.level += lines-2
				}
				2 -> engine.statistics.level = engine.statistics.lines*999/FURTHEST_LINES
			}

			if(engine.statistics.level>=999) {
				// Ending Start
				engine.playSE("endingstart")
				engine.statistics.level = 999
				engine.timerActive = false
				engine.ending = 2
				owner.bgmStatus.bgm = BGM.ENDING(3)
				rollclear = 1

				if(gametype==1&&joker>0) grade = 31
				else if(engine.lives>=MAX_LIVES[gametype]) grade = 31
				gradeflash = 180

				// Records section time
				sectionlasttime = sectionTime[levelb/100]
				sectionscomp++
				setAverageSectionTime()

				// Check for ST medal
				stMedalCheck(engine, levelb/100)
			} else if(nextseclv==500&&engine.statistics.level>=500&&gametype==1&&joker<=0) {
				// level500とりカン
				engine.statistics.level = 500
				engine.gameEnded()
				engine.staffrollEnable = false
				engine.ending = 1
				rollclear = 1

				secretGrade = engine.field!!.secretGrade
				// Section Timeを記録
				sectionlasttime = sectionTime[levelb/100]
				sectionscomp++
				setAverageSectionTime()

				// ST medal
				stMedalCheck(engine, levelb/100)

			} else if(engine.statistics.level>=nextseclv) {
				// Next section
				engine.playSE("levelup")

				// Change background image
				owner.backgroundStatus.fadesw = true
				owner.backgroundStatus.fadecount = 0
				owner.backgroundStatus.fadebg = 30+nextseclv/100

				// Records section time
				sectionlasttime = sectionTime[levelb/100]
				sectionscomp++
				setAverageSectionTime()

				// Check for ST medal
				stMedalCheck(engine, levelb/100)
				// Grade Increase
				if(gametype==0) {
					val gm = SECTION_MAX*(MAX_LIVES[0]+1)
					gradeinternal = maxOf(gm, gradeinternal+1+engine.lives)
					val gi = gradeinternal*31/gm
					if(grade!=gi) {
						grade = gi
						gradeflash = 180
						engine.playSE("cool")
					}
				}
				gradeflash = 180

				// Update next section level
				nextseclv += 100
				if(nextseclv>999) nextseclv = 999
			} else if(engine.statistics.level==nextseclv-1&&lvstopse) engine.playSE("levelstop")

			// Add score

			val section = levelb/100
			if(section>=0&&section<sectionTime.size) sectionLine[levelb/100] += if(gametype==1) if(lines>=4) 1 else 0 else lines
			lastscore = (((levelb+lines)/4+engine.softdropFall+if(engine.manualLock) 1 else 0)*lines*comboValue+
				maxOf(0, engine.lockDelay-engine.lockDelayNow)+engine.statistics.level/2)*if(engine.field!!.isEmpty) 2 else 1

			engine.statistics.score += lastscore
			levelUp(engine)
		}

	}

	/** This function will be called when the game timer updates */
	override fun onLast(engine:GameEngine, playerID:Int) {
		// Grade up flash
		if(gradeflash>0) gradeflash--

		// Increase section timer
		if(engine.timerActive&&engine.ending==0) {
			val section = engine.statistics.level/100

			if(section>=0&&section<sectionTime.size) sectionTime[section]++
			if(engine.statistics.level==nextseclv-1)
				engine.meterColor = if(engine.meterColor==-0x1)
					-0x10000
				else
					-0x1
		}

		// Engine
		if(engine.gameActive&&engine.ending==2) {
			rolltime++

			// Time meter
			val remainRollTime = ROLLTIMELIMIT-rolltime
			engine.meterValue = remainRollTime*receiver.getMeterMax(engine)/ROLLTIMELIMIT
			engine.meterColor = GameEngine.METER_COLOR_LIMIT

			// Player has survived the roll
			if(rolltime>=ROLLTIMELIMIT) {
				rollclear = if(engine.lives==MAX_LIVES[gametype]) 2 else 1
				engine.gameEnded()
				engine.resetStatc()
				engine.stat = GameEngine.Status.EXCELLENT
			}
		}
	}

	/** This function will be called when the player tops out */
	override fun onGameOver(engine:GameEngine, playerID:Int):Boolean {
		if(engine.statc[0]==0) secretGrade = engine.field!!.secretGrade
		return false
	}

	/** Renders game result screen */
	override fun renderResult(engine:GameEngine, playerID:Int) {
		receiver.drawMenuFont(engine, playerID, 0, 0, "kn PAGE${engine.statc[1]+1}/3", COLOR.RED)

		if(engine.statc[1]==0) {
			if(grade>=1&&grade<tableGradeName.size) {
				var gcolor = COLOR.WHITE
				if(rollclear==1) gcolor = COLOR.GREEN
				if(rollclear==2) gcolor = COLOR.ORANGE
				receiver.drawMenuFont(engine, playerID, 0, 2, "GRADE", COLOR.RED)
				receiver.drawMenuGrade(engine, playerID, 6, 2, tableGradeName[grade], gcolor, 2f)
			}

			drawResultStats(engine, playerID, receiver, 4, COLOR.RED, Statistic.SCORE, Statistic.LINES, Statistic.LEVEL_MANIA, Statistic.TIME)
			if(secretGrade>4)
				drawResult(engine, playerID, receiver, 14, COLOR.RED, "S. GRADE", String.format("%10s", tableSecretGradeName[secretGrade-1]))
		} else if(engine.statc[1]==1) {
			receiver.drawMenuFont(engine, playerID, 0, 2, "SECTION", COLOR.RED)

			for(i in sectionTime.indices)
				if(sectionTime[i]>0)
					receiver.drawMenuFont(engine, playerID, 2, 3+i, GeneralUtil.getTime(sectionTime[i]), sectionIsNewRecord[i])

			if(sectionavgtime>0) {
				receiver.drawMenuFont(engine, playerID, 0, 14, "AVERAGE", COLOR.RED)
				receiver.drawMenuNum(engine, playerID, 2, 15, GeneralUtil.getTime(sectionavgtime))
			}
		} else if(engine.statc[1]==2) {
			receiver.drawMenuFont(engine, playerID, 0, 2, "MEDAL", COLOR.RED)
			receiver.drawMenuMedal(engine, playerID, 5, 3, "AC", medalAC)
			receiver.drawMenuMedal(engine, playerID, 8, 3, "ST", medalST)
			receiver.drawMenuMedal(engine, playerID, 5, 4, "SK", medalSK)
			receiver.drawMenuMedal(engine, playerID, 8, 4, "CO", medalCO)

			drawResultStats(engine, playerID, receiver, 6, COLOR.RED, Statistic.LPS, Statistic.SPS, Statistic.PIECE, Statistic.PPS)
		}
	}

	/** Additional routine for game result screen */
	override fun onResult(engine:GameEngine, playerID:Int):Boolean {
		// Page change
		if(engine.ctrl!!.isMenuRepeatKey(Controller.BUTTON_UP)) {
			engine.statc[1]--
			if(engine.statc[1]<0) engine.statc[1] = 2
			engine.playSE("change")
		}
		if(engine.ctrl!!.isMenuRepeatKey(Controller.BUTTON_DOWN)) {
			engine.statc[1]++
			if(engine.statc[1]>2) engine.statc[1] = 0
			engine.playSE("change")
		}
		// Flip Leaderboard/Best Section Time Records
		if(engine.ctrl!!.isPush(Controller.BUTTON_F)) {
			engine.playSE("change")
			isShowBestSectionTime = !isShowBestSectionTime
		}

		return false
	}

	/** This function will be called when the replay data is going to be
	 * saved */
	override fun saveReplay(engine:GameEngine, playerID:Int, prop:CustomProperties) {
		saveSetting(owner.replayProp)
		owner.replayProp.setProperty("final.version", version)

		// Updates leaderboard and best section time records
		if(!owner.replayMode&&startlevel==0&&!big&&engine.ai==null) {
			updateRanking(gametype, grade, engine.statistics.level, engine.statistics.time, rollclear)
			if(medalST==3) updateBestSectionTime(gametype)

			if(rankingRank!=-1||medalST==3) {
				saveRanking(owner.modeConfig, engine.ruleopt.strRuleName)
				receiver.saveModeConfig(owner.modeConfig)
			}
		}
	}

	/** Load the ranking
	 * @param prop CustomProperties
	 * @param ruleName Rule name
	 */
	private fun loadRanking(prop:CustomProperties?, ruleName:String) {
		for(j in 0 until RANKING_TYPE) {
			for(i in 0 until RANKING_MAX) {
				rankingGrade[j][i] = prop!!.getProperty("final.ranking.$ruleName.$j.grade.$i", 0)
				rankingLevel[j][i] = prop.getProperty("final.ranking.$ruleName.$j.level.$i", 0)
				rankingTime[j][i] = prop.getProperty("final.ranking.$ruleName.$j.time.$i", 0)
				rankingRollclear[j][i] = prop.getProperty("final.ranking.$ruleName.$j.rollclear.$i", 0)
			}
			for(i in 0 until SECTION_MAX)
				bestSectionTime[j][i] = prop!!.getProperty("final.bestSectionTime.$ruleName.$j.$i", DEFAULT_SECTION_TIME)
		}
	}

	/** Save the ranking
	 * @param prop CustomProperties
	 * @param ruleName Rule name
	 */
	private fun saveRanking(prop:CustomProperties?, ruleName:String) {
		for(j in 0 until RANKING_TYPE) {
			for(i in 0 until RANKING_MAX) {
				prop!!.setProperty("final.ranking.$ruleName.$j.grade.$i", rankingGrade[j][i])
				prop.setProperty("final.ranking.$ruleName.$j.level.$i", rankingLevel[j][i])
				prop.setProperty("final.ranking.$ruleName.$j.time.$i", rankingTime[j][i])
				prop.setProperty("final.ranking.$ruleName.$j.rollclear.$i", rankingRollclear[j][i])
			}
			for(i in 0 until SECTION_MAX)
				prop!!.setProperty("final.bestSectionTime.$ruleName.$j.$i", bestSectionTime[j][i])
		}
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
				rankingRollclear[type][i] = rankingRollclear[type][i-1]
			}

			rankingGrade[type][rankingRank] = gr
			rankingLevel[type][rankingRank] = lv
			rankingTime[type][rankingRank] = time
			rankingRollclear[type][rankingRank] = clear
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
		for(i in 0 until RANKING_MAX)
			if(clear>rankingRollclear[type][i])
				return i
			else if(clear==rankingRollclear[type][i]&&gr>rankingGrade[type][i])
				return i
			else if(clear==rankingRollclear[type][i]&&gr==rankingGrade[type][i]&&lv>rankingLevel[type][i])
				return i
			else if(clear==rankingRollclear[type][i]&&gr==rankingGrade[type][i]&&lv==rankingLevel[type][i]
				&&time<rankingTime[type][i])
				return i

		return -1
	}

	/** Updates best section time records */
	private fun updateBestSectionTime(t:Int) {
		for(i in 0 until SECTION_MAX)
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
			arrayOf(intArrayOf(20, 19, 18, 17, 16, 15, 14, 13, 12, 11), intArrayOf(15, 14, 13, 12, 11, 10, 9, 8, 7, 5), intArrayOf(10, 9, 8, 7, 6, 5, 4, 3, 2, 1))

		/** Lock delay */
		private val tableLockDelay =
			arrayOf(intArrayOf(25, 25, 24, 24, 23, 23, 22, 22, 21, 21), intArrayOf(30, 29, 28, 27, 26, 25, 24, 23, 22, 21), intArrayOf(22, 22, 21, 21, 20, 20, 19, 18, 17, 16))

		private val tableHiddenDelay = intArrayOf(320, 300, 275, 250, 225, 200, 180, 150, 120, 60)
		/** Mode names */
		private val tableModeName = arrayOf("GENUINE", "BEST BOWER", "LONGOMINIAD")
		/** Grade names */
		private val tableGradeName =
			arrayOf("", "S1", "S2", "S3", "S4", "S5", "S6", "S7", "S8", "S9", "S10", "m", "m1", "m2", "m3", "m4", "m5", "m6", "m7", "m8", "m9", "mK", "mV", "mO", "M", "MK", "MV", "MO", "MM", "Gm", "GM", "GOD")

		/** Secret grade names */
		private val tableSecretGradeName = arrayOf("M1", "M2", "M3", "M4", "M5", "M6", "M7", "M8", "M9", //  0～ 8
			"M10", "M11", "M12", "M13", "MK", "MV", "MO", "MM", "GM", //  9～17
			"GOD" // 18
		)

		/** Ending time limit */
		private const val ROLLTIMELIMIT = 12000

		/** Number of ranking records */
		private const val RANKING_MAX = 10
		/** Number of ranking types */
		private const val RANKING_TYPE = 3

		/** Number of sections */
		private const val SECTION_MAX = 10

		/** Default section time */
		private const val DEFAULT_SECTION_TIME = 4000

		/** Allowed Topouts */
		private val MAX_LIVES = intArrayOf(4, 0, 2)

		/** goal of gamemode:2 */
		private const val FURTHEST_LINES = 300
	}
}
