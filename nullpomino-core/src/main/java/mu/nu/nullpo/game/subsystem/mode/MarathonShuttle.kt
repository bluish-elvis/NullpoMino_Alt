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
import mu.nu.nullpo.game.net.NetUtil
import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.util.CustomProperties
import mu.nu.nullpo.util.GeneralUtil
import kotlin.math.ceil

/** TECHNICIAN Mode */
class MarathonShuttle:NetDummyMode() {

	/** Number of Goal-points remaining */
	private var goal:Int = 0
	private var goalmax:Int = 0

	/** Level timer */
	private var levelTimer:Int = 0
	private var lastlineTime:Int = 0

	/** true if level timer runs out */
	private var levelTimeOut:Boolean = false

	/** Master time limit */
	private var totalTimer:Int = 0

	/** Ending time */
	private var rolltime:Int = 0

	/** Most recent increase in goal-points */
	private var lastgoal:Int = 0

	/** Most recent increase in score */
	private var lastscore:Int = 0

	/** Most recent increase in time limit */
	private var lasttimebonus:Int = 0

	/** Time to display the most recent increase in score */
	private var scgettime:Int = 0
	private var sc:Int = 0
	private var sum:Int = 0

	/** REGRET display time frame count */
	private var regretdispframe:Int = 0

	/** Current BGM */
	private var bgmlv:Int = 0

	/** Game type */
	private var goaltype:Int = 0

	/** Level at start time */
	private var startlevel:Int = 0

	/** Flag for types of T-Spins allowed (0=none, 1=normal, 2=all spin) */
	private var tspinEnableType:Int = 0

	/** Old flag for allowing T-Spins */
	private var enableTSpin:Boolean = false

	/** Flag for enabling wallkick T-Spins */
	private var enableTSpinKick:Boolean = false

	/** Spin check type (4Point or Immobile) */
	private var spinCheckType:Int = 0

	/** Immobile EZ spin */
	private var tspinEnableEZ:Boolean = false

	/** Flag for enabling B2B */
	private var enableB2B:Boolean = false

	/** Flag for enabling combos */
	private var enableCombo:Boolean = false

	/** Big */
	private var big:Boolean = false

	/** Version */
	private var version:Int = 0

	/** Current round's ranking rank */
	private var rankingRank:Int = 0

	/** Rankings' scores */
	private var rankingScore:Array<IntArray> = Array(RANKING_TYPE) {IntArray(RANKING_MAX)}

	/** Rankings' line counts */
	private var rankingLines:Array<IntArray> = Array(RANKING_TYPE) {IntArray(RANKING_MAX)}

	/** Rankings' times */
	private var rankingTime:Array<IntArray> = Array(RANKING_TYPE) {IntArray(RANKING_MAX)}

	/* Mode name */
	override val name:String
		get() = "MARATHON SHUTTLE RUN"

	/* Initialization */
	override fun playerInit(engine:GameEngine, playerID:Int) {
		super.playerInit(engine, playerID)
		goal = 0
		lastlineTime = 0
		levelTimer = lastlineTime
		levelTimeOut = false
		totalTimer = 0
		rolltime = 0
		lastgoal = 0
		lastscore = 0
		lasttimebonus = 0
		sc = 0
		scgettime = sc
		regretdispframe = 0
		bgmlv = 0

		rankingRank = -1
		rankingScore = Array(RANKING_TYPE) {IntArray(RANKING_MAX)}
		rankingLines = Array(RANKING_TYPE) {IntArray(RANKING_MAX)}
		rankingTime = Array(RANKING_TYPE) {IntArray(RANKING_MAX)}

		netPlayerInit(engine, playerID)

		if(!owner.replayMode) {
			loadSetting(owner.modeConfig)
			loadRanking(owner.recordProp, engine.ruleopt.strRuleName)
			version = CURRENT_VERSION
		} else {
			loadSetting(owner.replayProp)
			// NET: Load name
			netPlayerName = engine.owner.replayProp.getProperty("$playerID.net.netPlayerName", "")
		}

		engine.owner.backgroundStatus.bg = startlevel
		if(engine.owner.backgroundStatus.bg>19) engine.owner.backgroundStatus.bg = 19
		engine.framecolor = GameEngine.FRAME_COLOR_WHITE
	}

	/* Render the settings screen */
	override fun renderSetting(engine:GameEngine, playerID:Int) {
		if(netIsNetRankingDisplayMode)
		// NET: Netplay Ranking
			netOnRenderNetPlayRanking(engine, playerID, receiver)
		else {
			drawMenu(engine, playerID, receiver, 0, COLOR.BLUE, 0,
				"GAME TYPE", GAMETYPE_SHORTNAME[goaltype], "LEVEL", (startlevel+1).toString(),
				"SPIN BONUS", if(tspinEnableType==0) "OFF" else if(tspinEnableType==1) "T-ONLY" else "ALL",
				"EZ SPIN", GeneralUtil.getONorOFF(enableTSpinKick), "SPIN TYPE", if(spinCheckType==0) "4POINT" else "IMMOBILE",
				"EZIMMOBILE", GeneralUtil.getONorOFF(tspinEnableEZ))

			drawMenuCompact(engine, playerID, receiver,
				"B2B", GeneralUtil.getONorOFF(enableB2B), "COMBO", GeneralUtil.getONorOFF(enableCombo), "BIG", GeneralUtil.getONorOFF(big))
		}
	}

	/* Called at settings screen */
	override fun onSetting(engine:GameEngine, playerID:Int):Boolean {
		// NET: Net Ranking
		if(netIsNetRankingDisplayMode)
			netOnUpdateNetPlayRanking(engine, goaltype)
		else if(!engine.owner.replayMode) {
			// Configuration changes
			val change = updateCursor(engine, 8)

			if(change!=0) {
				engine.playSE("change")

				when(menuCursor) {
					0 -> {
						goaltype += change
						if(goaltype<0) goaltype = GAMETYPE_MAX-1
						if(goaltype>GAMETYPE_MAX-1) goaltype = 0
					}
					1 -> {
						startlevel += change
						if(startlevel<0) startlevel = 29
						if(startlevel>29) startlevel = 0
						engine.owner.backgroundStatus.bg = startlevel
						if(engine.owner.backgroundStatus.bg>19) engine.owner.backgroundStatus.bg = 19
					}
					2 -> {
						//enableTSpin = !enableTSpin;
						tspinEnableType += change
						if(tspinEnableType<0) tspinEnableType = 2
						if(tspinEnableType>2) tspinEnableType = 0
					}
					3 -> enableTSpinKick = !enableTSpinKick
					4 -> {
						spinCheckType += change
						if(spinCheckType<0) spinCheckType = 1
						if(spinCheckType>1) spinCheckType = 0
					}
					5 -> tspinEnableEZ = !tspinEnableEZ
					6 -> enableB2B = !enableB2B
					7 -> enableCombo = !enableCombo
					8 -> big = !big
				}

				// NET: Signal options change
				if(netIsNetPlay&&netNumSpectators>0) netSendOptions(engine)
			}

			// Confirm
			if(engine.ctrl!!.isPush(Controller.BUTTON_A)&&menuTime>=5) {
				engine.playSE("decide")
				saveSetting(owner.modeConfig)
				owner.saveModeConfig()

				// NET: Signal start of the game
				if(netIsNetPlay) netLobby!!.netPlayerClient!!.send("start1p\n")

				return false
			}

			// Cancel
			if(engine.ctrl!!.isPush(Controller.BUTTON_B)&&!netIsNetPlay) engine.quitflag = true

			// NET: Netplay Ranking
			if(engine.ctrl!!.isPush(Controller.BUTTON_D)&&netIsNetPlay
				&&netIsNetRankingViewOK(engine))
				netEnterNetPlayRankingScreen(engine, playerID, goaltype)

			menuTime++
		} else {
			menuTime++
			menuCursor = -1

			return menuTime<60
		}// Replay
		// Menu

		return true
	}

	/* Called for initialization during "Ready" screen */
	override fun startGame(engine:GameEngine, playerID:Int) {
		engine.statistics.level = startlevel
		engine.statistics.levelDispAdd = 1
		engine.b2bEnable = enableB2B
		if(enableCombo)
			engine.comboType = GameEngine.COMBO_TYPE_NORMAL
		else
			engine.comboType = GameEngine.COMBO_TYPE_DISABLE

		engine.big = big

		if(version>=2) {
			engine.tspinAllowKick = enableTSpinKick
			if(tspinEnableType==0)
				engine.tspinEnable = false
			else if(tspinEnableType==1)
				engine.tspinEnable = true
			else {
				engine.tspinEnable = true
				engine.useAllSpinBonus = true
			}
		} else
			engine.tspinEnable = enableTSpin

		engine.spinCheckType = spinCheckType
		engine.tspinEnableEZ = tspinEnableEZ

		engine.speed.lineDelay = 8

		goalmax = (engine.statistics.level+1)*5
		goal = goalmax

		setSpeed(engine)

		if(netIsWatch)
			owner.bgmStatus.bgm = BGM.SILENT
		else {
			setStartBgmlv(engine)
			owner.bgmStatus.bgm = tableBGM[bgmlv]
		}

		if(goaltype==GAMETYPE_10MIN_EASY||goaltype==GAMETYPE_10MIN_HARD) totalTimer = TIMELIMIT_10MIN
		if(goaltype==GAMETYPE_SPECIAL) totalTimer = TIMELIMIT_SPECIAL

		if(goaltype==GAMETYPE_SPECIAL) {
			engine.staffrollEnable = true
			engine.staffrollEnableStatistics = true
			engine.staffrollNoDeath = true
		}
	}

	/** Set the gravity rate
	 * @param engine GameEngine
	 */
	private fun setSpeed(engine:GameEngine) {
		var lv = engine.statistics.level

		if(lv<0) lv = 0
		if(lv>=tableGravity.size) lv = tableGravity.size-1

		engine.speed.gravity = tableGravity[lv]
		engine.speed.denominator = tableDenominator[lv]
	}

	/** Set BGM at start of game
	 * @param engine GameEngine
	 */
	private fun setStartBgmlv(engine:GameEngine) {
		bgmlv = 0
		while(tableBGMChange[bgmlv]!=-1&&engine.statistics.level>=tableBGMChange[bgmlv])
			bgmlv++
	}

	/* Render score */
	override fun renderLast(engine:GameEngine, playerID:Int) {
		if(owner.menuOnly) return

		receiver.drawScoreFont(engine, playerID, 0, 0, "SHUTTLE RUN\n("+GAMETYPE_NAME[goaltype]
			+")", COLOR.WHITE)

		if(engine.stat==GameEngine.Status.SETTING||engine.stat==GameEngine.Status.RESULT&&!owner.replayMode) {
			if(!owner.replayMode&&!big&&startlevel==0&&engine.ai==null) {
				val scale = if(receiver.nextDisplayType==2) .5f else 1f
				val topY = if(receiver.nextDisplayType==2) 6 else 4
				receiver.drawScoreFont(engine, playerID, 3, topY-1, "SCORE   LINE TIME", COLOR.BLUE, scale)

				for(i in 0 until RANKING_MAX) {
					receiver.drawScoreGrade(engine, playerID, 0, topY+i, String.format("%2d", i+1), COLOR.YELLOW, scale)
					receiver.drawScoreNum(engine, playerID, 3, topY+i, "${rankingScore[goaltype][i]}", i==rankingRank, scale)
					receiver.drawScoreNum(engine, playerID, 11, topY+i, "${rankingLines[goaltype][i]}", i==rankingRank, scale)
					receiver.drawScoreNum(engine, playerID, 16,
						topY+i, GeneralUtil.getTime(rankingTime[goaltype][i]), i==rankingRank, scale)
				}
			}
		} else {
			// SCORE
			receiver.drawScoreFont(engine, playerID, 0, 2, "SCORE", COLOR.BLUE)
			receiver.drawScoreNum(engine, playerID, 5, 2, "+$lastscore")
			val scget = scgettime<engine.statistics.score
			if(scget) scgettime += ceil((engine.statistics.score-scgettime)/24.0).toInt()
			sc += ceil(((scgettime-sc)/10f).toDouble()).toInt()
			receiver.drawScoreNum(engine, playerID, 0, 3, "$sc", scget, 2f)

			// GOAL
			receiver.drawScoreFont(engine, playerID, 0, 5, "GOAL", COLOR.BLUE)
			var strGoal = "$goal"
			if(lastgoal!=0&&scget&&engine.ending==0) strGoal += "-$lastgoal"
			receiver.drawScoreNum(engine, playerID, 5, 5, strGoal, 2f)

			val remainLevelTime = maxOf(0, if(goaltype!=GAMETYPE_SPECIAL) TIMELIMIT_LEVEL-levelTimer else totalTimer)
			val fontcolorLevelTime = when {
				remainLevelTime<10*60 -> COLOR.RED
				remainLevelTime<30*60 -> COLOR.ORANGE
				remainLevelTime<60*60 -> COLOR.YELLOW
				else -> COLOR.WHITE
			}
			if(goaltype==GAMETYPE_SPECIAL) {
				// LEVEL TIME
				receiver.drawScoreFont(engine, playerID, 0, 6, "LIMIT", COLOR.BLUE)
				receiver.drawScoreNum(engine, playerID, 0, 7, GeneralUtil.getTime(remainLevelTime), fontcolorLevelTime, 2f)
				// +30sec
				if(lasttimebonus>0&&scget&&engine.ending==0)
					receiver.drawScoreFont(engine, playerID, 6, 6,
						String.format("+%3.2fSEC.", lasttimebonus/60.0f), COLOR.YELLOW)
			} else {
				// LEVEL BONUS
				receiver.drawScoreFont(engine, playerID, 0, 6, "BONUS", COLOR.BLUE)
				val levelBonus = maxOf(0, (TIMELIMIT_LEVEL-levelTimer)*(engine.statistics.level+1))
				receiver.drawScoreNum(engine, playerID, 0, 7, "$levelBonus", fontcolorLevelTime, 2f)
				receiver.drawScoreNum(engine, playerID, 6, 6, GeneralUtil.getTime(remainLevelTime), fontcolorLevelTime)
			}
			// LEVEL
			receiver.drawScoreFont(engine, playerID, 0, 9, "LEVEL", COLOR.BLUE)
			receiver.drawScoreNum(engine, playerID, 5, 9, (engine.statistics.level+1).toString(), 2f)

			// TOTAL TIME
			receiver.drawScoreFont(engine, playerID, 0, 10, "TIME", COLOR.BLUE)
			var totaltime = engine.statistics.time
			var fontcolorTotalTime = COLOR.WHITE
			if(goaltype==GAMETYPE_10MIN_EASY||goaltype==GAMETYPE_10MIN_HARD) {
				totaltime = TIMELIMIT_10MIN-totaltime

				fontcolorTotalTime = if(totaltime<10*60)
					COLOR.RED
				else if(totaltime<30*60)
					COLOR.ORANGE
				else if(totaltime<60*60)
					COLOR.YELLOW
				else
					COLOR.WHITE
			}
			receiver.drawScoreNum(engine, playerID, 0, 11, GeneralUtil.getTime(totaltime), fontcolorTotalTime, 2f)

			// Ending time
			if(engine.gameActive&&(engine.ending==2||rolltime>0)) {
				var remainRollTime = TIMELIMIT_ROLL-rolltime
				if(remainRollTime<0) remainRollTime = 0

				receiver.drawScoreFont(engine, playerID, 0, 13, "ROLL TIME", COLOR.BLUE)
				receiver.drawScoreNum(engine, playerID, 0, 14, GeneralUtil.getTime(remainRollTime), remainRollTime>0&&remainRollTime<10*60, 2f)
			}

			if(regretdispframe>0)
			// REGRET
				receiver.drawMenuFont(engine, playerID, 2, 21, "REGRET", when {
					regretdispframe%4==0 -> COLOR.YELLOW
					regretdispframe%4==2 -> COLOR.RED
					else -> COLOR.ORANGE
				})
			else if(scget) renderLineAlert(engine, playerID, receiver)

		}

		super.renderLast(engine, playerID)
	}

	/* Called after every frame */
	override fun onLast(engine:GameEngine, playerID:Int) {

		if(regretdispframe>0) regretdispframe--

		// Level Time
		if(engine.gameActive&&engine.timerActive&&goaltype!=GAMETYPE_SPECIAL) {
			levelTimer++
			val remainTime = TIMELIMIT_LEVEL-levelTimer
			// Time meter
			engine.meterValue = remainTime*receiver.getMeterMax(engine)/TIMELIMIT_LEVEL/2
			engine.meterValue += ((receiver.getMeterMax(engine)-engine.meterValue)*(goalmax-goal)*remainTime/goalmax
				/(TIMELIMIT_LEVEL-lastlineTime))
			engine.meterColor = GameEngine.METER_COLOR_GREEN
			if(remainTime<=60*60) engine.meterColor = GameEngine.METER_COLOR_YELLOW
			if(remainTime<=30*60) engine.meterColor = GameEngine.METER_COLOR_ORANGE
			if(remainTime<=10*60) engine.meterColor = GameEngine.METER_COLOR_RED

			if(!netIsWatch)
				if(levelTimer>=TIMELIMIT_LEVEL) {
					// Out of time
					levelTimeOut = true

					if(goaltype==GAMETYPE_LV15_HARD||goaltype==GAMETYPE_10MIN_HARD) {
						engine.gameEnded()
						engine.resetStatc()
						engine.stat = GameEngine.Status.GAMEOVER
					} else if(goaltype==GAMETYPE_10MIN_EASY||goaltype==GAMETYPE_LV15_EASY) {
						regretdispframe = 180
						engine.playSE("regret")
						if(goaltype==GAMETYPE_10MIN_EASY) {
							goal = goalmax
							levelTimer = 0
						}
					}
				} else if(remainTime<=10*60&&remainTime%60==0)
				// Countdown
					engine.playSE("countdown")
				else if(remainTime==30*60)
					engine.playSE("hurryup")
				else if(remainTime==60*60) engine.playSE("levelstop")
		}

		// Total Time
		if(engine.gameActive&&engine.timerActive&&goaltype!=GAMETYPE_LV15_EASY&&goaltype!=GAMETYPE_LV15_HARD) {
			totalTimer--

			// Time meter
			if(goaltype==GAMETYPE_SPECIAL) {
				engine.meterValue = totalTimer*receiver.getMeterMax(engine)/(5*3600)
				engine.meterColor = GameEngine.METER_COLOR_GREEN
				if(totalTimer<=60*60) engine.meterColor = GameEngine.METER_COLOR_YELLOW
				if(totalTimer<=30*60) engine.meterColor = GameEngine.METER_COLOR_ORANGE
				if(totalTimer<=10*60) engine.meterColor = GameEngine.METER_COLOR_RED
			}

			if(!netIsWatch)
				if(totalTimer<0) {
					// Out of time
					engine.gameEnded()
					engine.resetStatc()

					if(goaltype==GAMETYPE_10MIN_EASY||goaltype==GAMETYPE_10MIN_HARD)
						engine.stat = GameEngine.Status.ENDINGSTART
					else
						engine.stat = GameEngine.Status.GAMEOVER

					totalTimer = 0
				} else if(totalTimer<=10*60&&totalTimer%60==0)
				// Countdown
					engine.playSE("countdown")
		}

		// Ending
		if(engine.gameActive&&engine.ending==2) {
			rolltime++

			// Time meter
			val remainRollTime = TIMELIMIT_ROLL-rolltime
			engine.meterValue = remainRollTime*receiver.getMeterMax(engine)/TIMELIMIT_ROLL
			engine.meterColor = GameEngine.METER_COLOR_GREEN
			if(remainRollTime<=30*60) engine.meterColor = GameEngine.METER_COLOR_YELLOW
			if(remainRollTime<=20*60) engine.meterColor = GameEngine.METER_COLOR_ORANGE
			if(remainRollTime<=10*60) engine.meterColor = GameEngine.METER_COLOR_RED

			// Finished
			if(rolltime>=TIMELIMIT_ROLL&&!netIsWatch) {
				lastscore = totalTimer*2
				engine.statistics.scoreBonus += lastscore
				engine.lastevent = GameEngine.EVENT_NONE

				engine.gameEnded()
				engine.resetStatc()
				engine.stat = GameEngine.Status.EXCELLENT
			}
		}
	}



	/* Calculate score */
	override fun calcScore(engine:GameEngine, playerID:Int, lines:Int) {
		// Line clear bonus
		val pts = super.calcScore(engine, lines)
		var cmb = 0
		// Combo
		if(enableCombo&&engine.combo>=1&&lines>=1) cmb = engine.combo-1
		val spd = maxOf(0, engine.lockDelay-engine.lockDelayNow)+if(engine.manualLock) 1 else 0
		// Add to score
		if(pts+cmb+spd>0) {
			var get = pts*(10+engine.statistics.level)/10+spd
			if(cmb>=1) {
				var b = sum*(1+cmb)/2
				sum += get
				b = sum*(2+cmb)/2-b
				get = b
			} else
				sum = get
			if(pts>0) lastscore = get
			if(lines>=1) engine.statistics.scoreLine += get
			else engine.statistics.scoreBonus += get
			scgettime += spd

			var cmbindex = engine.combo-1
			if(cmbindex<0) cmbindex = 0
			if(cmbindex>=COMBO_GOAL_TABLE.size) cmbindex = COMBO_GOAL_TABLE.size-1
			lastgoal = calcPoint(engine, lines)+COMBO_GOAL_TABLE[cmbindex]
			goal -= lastgoal
			lastlineTime = levelTimer
			if(goal<=0) goal = 0
		}

		if(engine.ending==0) {
			// Time bonus
			if(goal<=0&&!levelTimeOut&&goaltype!=GAMETYPE_SPECIAL) {
				lasttimebonus = (TIMELIMIT_LEVEL-levelTimer)*(engine.statistics.level+1)
				if(lasttimebonus<0) lasttimebonus = 0
				engine.statistics.scoreBonus += lasttimebonus
			} else if(goal<=0&&goaltype==GAMETYPE_SPECIAL) {
				lasttimebonus = TIMELIMIT_SPECIAL_BONUS
				totalTimer += lasttimebonus
			} else if(pts>0) lasttimebonus = 0

			// BGM fade-out effects and BGM changes
			if(tableBGMChange[bgmlv]!=-1&&engine.statistics.level==tableBGMChange[bgmlv]-1)
				if(goal in 1..10)
					owner.bgmStatus.fadesw = true
				else if(goal<=0) {
					bgmlv++
					owner.bgmStatus.bgm = tableBGM[bgmlv]
					owner.bgmStatus.fadesw = false
				}

			if(goal<=0)
				if(engine.statistics.level>=14&&(goaltype==GAMETYPE_LV15_EASY||goaltype==GAMETYPE_LV15_HARD)) {
					// Ending (LV15-EASY/HARD）
					engine.ending = 1
					engine.gameEnded()
				} else if(engine.statistics.level>=29&&goaltype==GAMETYPE_SPECIAL) {
					// Ending (SPECIAL）
					engine.ending = 2
					engine.timerActive = false
					owner.bgmStatus.bgm = BGM.ENDING(0)
					owner.bgmStatus.fadesw = false
					engine.playSE("endingstart")
				} else {
					// Level up
					engine.statistics.level++
					if(engine.statistics.level>29) engine.statistics.level = 29

					goalmax = (engine.statistics.level+1)*5
					goal += goalmax
					if(owner.backgroundStatus.bg<19) {
						owner.backgroundStatus.fadesw = true
						owner.backgroundStatus.fadecount = 0
						owner.backgroundStatus.fadebg = engine.statistics.level
					}

					levelTimer = 0
					if(version>=1) engine.holdUsedCount = 0

					setSpeed(engine)
					engine.playSE("levelup")
				}
		}
	}

	/* Hard drop */
	override fun afterHardDropFall(engine:GameEngine, playerID:Int, fall:Int) {
		engine.statistics.scoreHD += fall*2
		scgettime += fall*2
	}

	/* Soft drop */
	override fun afterSoftDropFall(engine:GameEngine, playerID:Int, fall:Int) {
		engine.statistics.scoreSD += fall
		scgettime += fall
	}

	override fun onResult(engine:GameEngine, playerID:Int):Boolean {
		val b = if(engine.statistics.time<10800) BGM.RESULT(1) else BGM.RESULT(2)
		owner.bgmStatus.fadesw = false
		owner.bgmStatus.bgm = b

		return super.onResult(engine, playerID)
	}

	/* Render results screen */
	override fun renderResult(engine:GameEngine, playerID:Int) {
		drawResultStats(engine, playerID, receiver, 0, COLOR.BLUE, Statistic.SCORE, Statistic.LINES, Statistic.LEVEL, Statistic.TIME, Statistic.SPL, Statistic.LPM)
		drawResultRank(engine, playerID, receiver, 12, COLOR.BLUE, rankingRank)
		drawResultNetRank(engine, playerID, receiver, 14, COLOR.BLUE, netRankingRank[0])
		drawResultNetRankDaily(engine, playerID, receiver, 16, COLOR.BLUE, netRankingRank[1])

		if(netIsPB) receiver.drawMenuFont(engine, playerID, 2, 18, "NEW PB", COLOR.ORANGE)

		if(netIsNetPlay&&netReplaySendStatus==1)
			receiver.drawMenuFont(engine, playerID, 0, 19, "SENDING...", COLOR.PINK)
		else if(netIsNetPlay&&!netIsWatch
			&&netReplaySendStatus==2)
			receiver.drawMenuFont(engine, playerID, 1, 19, "A: RETRY", COLOR.RED)

	}

	/** Read rankings from property file
	 * @param prop Property file
	 * @param ruleName Rule name
	 */
	override fun loadRanking(prop:CustomProperties, ruleName:String) {
		for(i in 0 until RANKING_MAX)
			for(gametypeIndex in 0 until RANKING_TYPE) {
				rankingScore[gametypeIndex][i] = prop.getProperty("technician.ranking.$ruleName.$gametypeIndex.score.$i", 0)
				rankingLines[gametypeIndex][i] = prop.getProperty("technician.ranking.$ruleName.$gametypeIndex.lines.$i", 0)
				rankingTime[gametypeIndex][i] = prop.getProperty("technician.ranking.$ruleName.$gametypeIndex.time.$i", 0)
			}

	}

	/** Load settings from property file
	 * @param prop Property file
	 */
	override fun loadSetting(prop:CustomProperties) {
		goaltype = prop.getProperty("technician.gametype", 0)
		startlevel = prop.getProperty("technician.startlevel", 0)
		tspinEnableType = prop.getProperty("technician.tspinEnableType", 2)
		enableTSpin = prop.getProperty("technician.enableTSpin", true)
		enableTSpinKick = prop.getProperty("technician.enableTSpinKick", true)
		spinCheckType = prop.getProperty("technician.spinCheckType", 1)
		tspinEnableEZ = prop.getProperty("technician.tspinEnableEZ", true)
		enableB2B = prop.getProperty("technician.enableB2B", true)
		enableCombo = prop.getProperty("technician.enableCombo", true)
		big = prop.getProperty("technician.big", false)
		version = prop.getProperty("technician.version", 0)
	}

	/** Calculate ranking position
	 * @param sc Score
	 * @param li Lines
	 * @param time Time
	 * @return Position (-1 if unranked)
	 */
	private fun checkRanking(sc:Int, li:Int, time:Int, type:Int):Int {
		for(i in 0 until RANKING_MAX)
			if(sc>rankingScore[type][i])
				return i
			else if(sc==rankingScore[type][i]&&li>rankingLines[type][i])
				return i
			else if(sc==rankingScore[type][i]&&li==rankingLines[type][i]&&time<rankingTime[type][i]) return i

		return -1
	}

	/** NET: Get goal type */
	override fun netGetGoalType():Int = goaltype

	/** NET: It returns true when the current settings doesn't prevent
	 * leaderboard screen from showing. */
	override fun netIsNetRankingViewOK(engine:GameEngine):Boolean = !big&&engine.ai==null&&startlevel==0

	/** NET: Send game options to all spectators
	 * @param engine GameEngine
	 */
	override fun netSendOptions(engine:GameEngine) {
		var msg = "game\toption\t"
		msg += "$goaltype${"\t$startlevel\t"+tspinEnableType}\t"
		msg += "$enableTSpinKick${"\t$enableB2B\t"+enableCombo}\t$big\t"
		msg += "$spinCheckType${"\t"+tspinEnableEZ}\n"
		netLobby!!.netPlayerClient!!.send(msg)
	}

	/** NET: Receive game options */
	override fun netRecvOptions(engine:GameEngine, message:Array<String>) {
		goaltype = Integer.parseInt(message[4])
		startlevel = Integer.parseInt(message[5])
		tspinEnableType = Integer.parseInt(message[6])
		enableTSpinKick = java.lang.Boolean.parseBoolean(message[7])
		enableB2B = java.lang.Boolean.parseBoolean(message[8])
		enableCombo = java.lang.Boolean.parseBoolean(message[9])
		big = java.lang.Boolean.parseBoolean(message[10])
		spinCheckType = Integer.parseInt(message[11])
		tspinEnableEZ = java.lang.Boolean.parseBoolean(message[12])
	}

	/** NET: Send various in-game stats (as well as goaltype)
	 * @param engine GameEngine
	 */
	override fun netSendStats(engine:GameEngine) {
		val bg = if(owner.backgroundStatus.fadesw) owner.backgroundStatus.fadebg else owner.backgroundStatus.bg
		var msg = "game\tstats\t"
		msg += "${engine.statistics.scoreLine}\t${engine.statistics.scoreSD}\t${engine.statistics.scoreHD}\t${engine.statistics.scoreBonus}\t"
		msg += "${engine.statistics.lines}\t${engine.statistics.totalPieceLocked}\t${engine.statistics.time}\t"
		msg += "$goaltype\t${engine.gameActive}\t${engine.timerActive}\t"
		msg += "$lastscore\t$scgettime\t${engine.lastevent}\t${engine.b2bbuf}\t${engine.combobuf}\t${engine.lasteventpiece}\t"
		msg += "$lastgoal\t$lasttimebonus\t$regretdispframe\t"
		msg += "$bg\t${engine.meterValue}\t${engine.meterColor}\t"
		msg += "${engine.statistics.level}\t$levelTimer\t$totalTimer\t$rolltime\t$goal\n"
		netLobby!!.netPlayerClient!!.send(msg)
	}

	/** NET: Receive various in-game stats (as well as goaltype) */
	override fun netRecvStats(engine:GameEngine, message:Array<String>) {

		listOf<(String)->Unit>({},{},{},{},
			{engine.statistics.scoreLine = Integer.parseInt(it)},
			{engine.statistics.scoreSD = Integer.parseInt(it)},
			{engine.statistics.scoreHD = Integer.parseInt(it)},
			{engine.statistics.scoreBonus = Integer.parseInt(it)},
			{engine.statistics.lines = Integer.parseInt(it)},
			{engine.statistics.totalPieceLocked = Integer.parseInt(it)},
			{engine.statistics.time = Integer.parseInt(it)},
			{goaltype = Integer.parseInt(it)},
			{engine.gameActive = java.lang.Boolean.parseBoolean(it)},
			{engine.timerActive = java.lang.Boolean.parseBoolean(it)},
			{lastscore = Integer.parseInt(it)},
			{scgettime = Integer.parseInt(it)},
			{engine.lastevent = Integer.parseInt(it)},
			{engine.b2bbuf = Integer.parseInt(it)},
			{engine.combobuf = Integer.parseInt(it)},
			{engine.lasteventpiece = Integer.parseInt(it)},
			{lastgoal = Integer.parseInt(it)},
			{lasttimebonus = Integer.parseInt(it)},
			{regretdispframe = Integer.parseInt(it)},
			{owner.backgroundStatus.bg = Integer.parseInt(it)},
			{engine.meterValue = Integer.parseInt(it)},
			{engine.meterColor = Integer.parseInt(it)},
			{engine.statistics.level = Integer.parseInt(it)},
			{levelTimer = Integer.parseInt(it)},
			{totalTimer = Integer.parseInt(it)},
			{rolltime = Integer.parseInt(it)},
			{goal = Integer.parseInt(it)}).zip(message).forEach{
			(x,y)->x(y)
		}


	}

	/** NET: Send end-of-game stats
	 * @param engine GameEngine
	 */
	override fun netSendEndGameStats(engine:GameEngine) {
		var subMsg = ""
		subMsg += "SCORE;${engine.statistics.score}\t"
		subMsg += "LINE;${engine.statistics.lines}\t"
		subMsg += "LEVEL;${(engine.statistics.level+engine.statistics.levelDispAdd)}\t"
		subMsg += "TIME;${GeneralUtil.getTime(engine.statistics.time)}\t"
		subMsg += "SCORE/LINE;${engine.statistics.spl}\t"
		subMsg += "LINE/MIN;${engine.statistics.lpm}\t"

		val msg = "gstat1p\t${NetUtil.urlEncode(subMsg)}\n"
		netLobby!!.netPlayerClient!!.send(msg)
	}

	/** Save rankings to property file
	 * @param prop Property file
	 * @param ruleName Rule name
	 */
	fun saveRanking(prop:CustomProperties, ruleName:String) {
		for(i in 0 until RANKING_MAX)
			for(gametypeIndex in 0 until RANKING_TYPE) {
				prop.setProperty("technician.ranking.$ruleName.$gametypeIndex.score.$i", rankingScore[gametypeIndex][i])
				prop.setProperty("technician.ranking.$ruleName.$gametypeIndex.lines.$i", rankingLines[gametypeIndex][i])
				prop.setProperty("technician.ranking.$ruleName.$gametypeIndex.time.$i", rankingTime[gametypeIndex][i])
			}

	}

	/* Called when saving replay */
	override fun saveReplay(engine:GameEngine, playerID:Int, prop:CustomProperties) {
		saveSetting(prop)

		// NET: Save name
		if(netPlayerName!=null&&netPlayerName!!.isNotEmpty()) prop.setProperty("$playerID.net.netPlayerName", netPlayerName)

		// Update rankings
		if(!owner.replayMode&&!big&&engine.ai==null&&startlevel==0) {
			updateRanking(engine.statistics.score, engine.statistics.lines, engine.statistics.time, goaltype)

			if(rankingRank!=-1) {
				saveRanking(owner.recordProp, engine.ruleopt.strRuleName)
				owner.saveModeConfig()
			}
		}
	}

	/** Save settings to property file
	 * @param prop Property file
	 */
	override fun saveSetting(prop:CustomProperties) {
		prop.setProperty("technician.gametype", goaltype)
		prop.setProperty("technician.startlevel", startlevel)
		prop.setProperty("technician.tspinEnableType", tspinEnableType)
		prop.setProperty("technician.enableTSpin", enableTSpin)
		prop.setProperty("technician.enableTSpinKick", enableTSpinKick)
		prop.setProperty("technician.spinCheckType", spinCheckType)
		prop.setProperty("technician.tspinEnableEZ", tspinEnableEZ)
		prop.setProperty("technician.enableB2B", enableB2B)
		prop.setProperty("technician.enableCombo", enableCombo)
		prop.setProperty("technician.big", big)
		prop.setProperty("technician.version", version)
	}

	/** Update rankings
	 * @param sc Score
	 * @param li Lines
	 * @param time Time
	 * @param type Game type
	 */
	private fun updateRanking(sc:Int, li:Int, time:Int, type:Int) {
		rankingRank = checkRanking(sc, li, time, type)

		if(rankingRank!=-1) {
			// Shift down ranking entries
			for(i in RANKING_MAX-1 downTo rankingRank+1) {
				rankingScore[type][i] = rankingScore[type][i-1]
				rankingLines[type][i] = rankingLines[type][i-1]
				rankingTime[type][i] = rankingTime[type][i-1]
			}

			// Add new data
			rankingScore[type][rankingRank] = sc
			rankingLines[type][rankingRank] = li
			rankingTime[type][rankingRank] = time
		}
	}

	companion object {
		/** Current version */
		private const val CURRENT_VERSION = 2

		/** Fall velocity table (numerators) */
		private val tableGravity = intArrayOf(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 3, 4, 5, 7, 10, -1)

		/** Fall velocity table (denominators) */
		private val tableDenominator = intArrayOf(64, 50, 40, 33, 25, 20, 13, 10, 8, 4, 3, 2, 1, 1, 1, 1, 1, 1, 1, 1)

		/** BGM change levels */
		private val tableBGMChange = intArrayOf(5, 8, 15, 17, 19, -1)
		private val tableBGM = arrayOf(BGM.GENERIC(0), BGM.GENERIC(1), BGM.GENERIC(2), BGM.GENERIC(3), BGM.GENERIC(4), BGM.GENERIC(5))
		/** Combo goal table */
		private val COMBO_GOAL_TABLE = intArrayOf(0, 0, 1, 1, 2, 2, 3, 3, 4, 4, 4, 5)

		/** Number of entries in rankings */
		private const val RANKING_MAX = 10

		/** Number of ranking types */
		private const val RANKING_TYPE = 5

		/** Game type constants */
		private const val GAMETYPE_LV15_EASY = 0
		private const val GAMETYPE_LV15_HARD = 1
		private const val GAMETYPE_10MIN_EASY = 2
		private const val GAMETYPE_10MIN_HARD = 3
		private const val GAMETYPE_SPECIAL = 4

		/** Game type names */
		private val GAMETYPE_NAME = arrayOf("15LEVELS TIME TRIAL", "15LEVELS SPEED RUN", "10MINUITES TRIAL", "10MINUTES SURVIVAL", "UNLIMITED ENDURANCE")
		private val GAMETYPE_SHORTNAME = arrayOf("15LV T.A.", "15LV S.R.", "10MIN.TRY", "10MIN.SURV", "ULM.ENDURO")

		/** Game type max */
		private const val GAMETYPE_MAX = 5

		/** Time limit for each level */
		private const val TIMELIMIT_LEVEL = 3600*2

		/** Time limit of 10min games */
		private const val TIMELIMIT_10MIN = 3600*10

		/** Default time limit of Special game */
		private const val TIMELIMIT_SPECIAL = 3600*2

		/** Extra time of Special game */
		private const val TIMELIMIT_SPECIAL_BONUS = 2000

		/** Ending time */
		private const val TIMELIMIT_ROLL = 3600
	}
}
