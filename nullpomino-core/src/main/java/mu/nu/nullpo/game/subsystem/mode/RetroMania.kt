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
import mu.nu.nullpo.game.component.Controller
import mu.nu.nullpo.game.event.EventReceiver.COLOR
import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.util.CustomProperties
import mu.nu.nullpo.util.GeneralUtil
import mu.nu.nullpo.util.GeneralUtil.toTimeStr

/** RETRO MANIA mode (Based System16, Original from NullpoUE build 121909 by Zircean) */
class RetroMania:AbstractMode() {

	/** Amount of points you just get from line clears */
	private var lastscore = 0

	/** Elapsed time from last line clear (lastscore is displayed to screen
	 * until this reaches to 120) */
	private var scgettime = 0

	/** Selected game type */
	private var gametype = 0

	/** Selected starting level */
	private var startlevel = 0

	/** Level timer */
	private var levelTimer = 0

	/** Amount of lines cleared (It will be reset when the level increases) */
	private var linesAfterLastLevelUp = 0

	/** Big mode on/off */
	private var big = false

	/** Poweron Pattern on/off */
	private var poweron = false

	/** Version of this mode */
	private var version = 0

	/** Your place on leaderboard (-1: out of rank) */
	private var rankingRank = 0

	/** Score records */
	private var rankingScore:Array<IntArray> = Array(RANKING_TYPE) {IntArray(RANKING_MAX)}

	/** Line records */
	private var rankingLines:Array<IntArray> = Array(RANKING_TYPE) {IntArray(RANKING_MAX)}

	/** Line records */
	private var rankingLevel:Array<IntArray> = Array(RANKING_TYPE) {IntArray(RANKING_MAX)}

	/** Time records */
	private var rankingTime:Array<IntArray> = Array(RANKING_TYPE) {IntArray(RANKING_MAX)}

	/** Score 999,999 Reached time */
	private var maxScoreTime = -1
	/** 99 Lines Reached time */
	private var maxLinesTime = -1
	/** Level 99 Reached time */
	private var maxLevelTime = -1

	/** Returns the name of this mode */
	override val name = "Retro Mania .S"

	override val gameIntensity:Int = -1

	/** This function will be called when
	 * the game enters the main game screen. */
	override fun playerInit(engine:GameEngine, playerID:Int) {
		super.playerInit(engine, playerID)
		lastscore = 0
		scgettime = 0
		levelTimer = 0
		linesAfterLastLevelUp = 0
		maxScoreTime = -1
		maxLinesTime = -1
		maxLevelTime = -1

		rankingRank = -1
		rankingScore = Array(RANKING_TYPE) {IntArray(RANKING_MAX)}
		rankingLines = Array(RANKING_TYPE) {IntArray(RANKING_MAX)}
		rankingLevel = Array(RANKING_TYPE) {IntArray(RANKING_MAX)}
		rankingTime = Array(RANKING_TYPE) {IntArray(RANKING_MAX)}

		engine.twistEnable = false
		engine.b2bEnable = false
		engine.splitb2b = false
		engine.comboType = GameEngine.COMBO_TYPE_DISABLE
		engine.bighalf = false
		engine.bigmove = false

		engine.speed.are = 30
		engine.speed.areLine = 30
		engine.speed.lineDelay = 42
		engine.speed.lockDelay = 30
		engine.speed.das = 20
		engine.owMinDAS = -1
		engine.owMaxDAS = -1
		engine.owARR = 1
		engine.owSDSpd = 2
		engine.owDelayCancel = 0

		if(!owner.replayMode) {
			loadSetting(owner.modeConfig)
			loadRanking(owner.recordProp, engine.ruleOpt.strRuleName)
			version = CURRENT_VERSION
		} else loadSetting(owner.replayProp)

		engine.owner.backgroundStatus.bg = startlevel/2
		if(engine.owner.backgroundStatus.bg>19) engine.owner.backgroundStatus.bg = 19
		engine.framecolor = GameEngine.FRAME_SKIN_SG
	}

	/** Set the gravity speed
	 * @param engine GameEngine object
	 */
	private fun setSpeed(engine:GameEngine) {
		var lv = engine.statistics.level

		if(lv<0) lv = 0
		if(lv>=tableDenominator[0].size) lv = tableDenominator[0].size-1

		engine.speed.gravity = 1
		engine.speed.denominator = tableDenominator[gametype][lv]
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
						if(gametype<0) gametype = GAMETYPE_MAX-1
						if(gametype>GAMETYPE_MAX-1) gametype = 0
					}
					1 -> {
						startlevel += change
						if(startlevel<0) startlevel = 15
						if(startlevel>15) startlevel = 0
						engine.owner.backgroundStatus.bg = startlevel/2
					}
					2 -> big = !big
					3 -> poweron = !poweron
				}
			}

			// Check for A button, when pressed this will begin the game
			if(engine.ctrl.isPush(Controller.BUTTON_A)&&menuTime>=5) {
				engine.playSE("decide")
				saveSetting(owner.modeConfig)
				owner.saveModeConfig()
				return false
			}

			// Check for B button, when pressed this will shutdown the game engine.
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
		drawMenu(engine, playerID, receiver, 0, COLOR.BLUE, 0, "DIFFICULTY" to GAMETYPE_NAME[gametype], "Level" to startlevel,
			"BIG" to big, "POWERON" to poweron)
	}

	/** Ready */
	override fun onReady(engine:GameEngine, playerID:Int):Boolean {
		if(engine.statc[0]==0) {

			engine.ruleOpt.run {
				lockresetMove = false
				lockresetRotate = false
				lockresetWallkick = false
				lockresetFall = true
				softdropLock = true
				softdropMultiplyNativeSpeed = false
				softdropGravitySpeedLimit = true
				rotateInitial = false
				holdEnable = false
				dasInARE = false
				dasInReady = false
			}
			if(poweron)
				engine.nextPieceArrayID = GeneralUtil.createNextPieceArrayFromNumberString(STRING_POWERON_PATTERN)
		}
		return false
	}

	/** This function will be called before the game actually begins (after
	 * Ready&Go screen disappears) */
	override fun startGame(engine:GameEngine, playerID:Int) {
		engine.statistics.level = startlevel
		engine.statistics.levelDispAdd = 1

		engine.big = big

		owner.bgmStatus.bgm = BGM.RetroS(0)
		setSpeed(engine)
	}

	/** Renders HUD (leaderboard or game statistics) */
	override fun renderLast(engine:GameEngine, playerID:Int) {
		receiver.drawScoreFont(engine, playerID, 0, 0, "RETRO MANIA", COLOR.GREEN)
		receiver.drawScoreFont(engine, playerID, 0, 1, "(${GAMETYPE_NAME[gametype]} SPEED)", COLOR.GREEN)

		if(engine.stat==GameEngine.Status.SETTING||engine.stat==GameEngine.Status.RESULT&&!owner.replayMode) {
			// Leaderboard
			if(!owner.replayMode&&!big&&startlevel==0&&engine.ai==null) {
				val scale = if(receiver.nextDisplayType==2) .5f else 1f
				val topY = if(receiver.nextDisplayType==2) 6 else 4
				receiver.drawScoreFont(engine, playerID, 3, topY-1, "SCORE LINE LV TIME", COLOR.BLUE, scale)

				for(i in 0 until RANKING_MAX) {
					receiver.drawScoreGrade(engine, playerID, 0, topY+i, "${i+1}", COLOR.YELLOW)
					receiver.drawScoreNum(engine, playerID, 2, topY+i,
						if(rankingScore[gametype][i]>=0) String.format("%6d", rankingScore[gametype][i])
						else rankingScore[gametype][i].toTimeStr, i==rankingRank, scale)
					receiver.drawScoreNum(engine, playerID, 8, topY+i,
						if(rankingLines[gametype][i]>=0) String.format("%3d", rankingLines[gametype][i])
						else rankingLines[gametype][i].toTimeStr, i==rankingRank, scale)
					receiver.drawScoreNum(engine, playerID, 11, topY+i,
						if(rankingLevel[gametype][i]>=0) String.format("%2d", rankingLevel[gametype][i])
						else rankingLevel[gametype][i].toTimeStr, i==rankingRank, scale)
					receiver.drawScoreNum(engine, playerID, 15, topY+i,
						rankingTime[gametype][i].toTimeStr, i==rankingRank,
						scale)
				}
			}
		} else {
			// Game statistics
			receiver.drawScoreFont(engine, playerID, 1, 3, "SCORE", COLOR.CYAN)
			receiver.drawScoreFont(engine, playerID, 0, 4, String.format("%6d", engine.statistics.score), COLOR.CYAN)

			receiver.drawScoreFont(engine, playerID, 1, 6, "LINES", COLOR.CYAN)
			receiver.drawScoreFont(engine, playerID, 0, 7, String.format("%6d", engine.statistics.lines), COLOR.CYAN)

			receiver.drawScoreFont(engine, playerID, 1, 9, "LEVEL", COLOR.CYAN)
			receiver.drawScoreFont(engine, playerID, 0, 10, String.format("%6d", engine.statistics.level), COLOR.CYAN)

			receiver.drawScoreFont(engine, playerID, 0, 13, "Time", COLOR.BLUE)
			receiver.drawScoreFont(engine, playerID, 0, 14, engine.statistics.time.toTimeStr, COLOR.BLUE)

			receiver.drawScoreNano(engine, playerID, 0, 31, "${4-linesAfterLastLevelUp} LINES TO GO", COLOR.CYAN, .5f)
			receiver.drawScoreNano(engine, playerID, 0, 32,
				"OR "+(levelTime[minOf(engine.statistics.level, 15)]-levelTimer).toTimeStr, COLOR.CYAN, .5f)
		}
	}

	/** This function will be called when the game timer updates */
	override fun onLast(engine:GameEngine, playerID:Int) {
		scgettime++
		if(engine.timerActive) levelTimer++
		// Update the meter
		engine.meterValue = levelTimer*receiver.getMeterMax(engine)/levelTime[minOf(engine.statistics.level, 15)]
		+linesAfterLastLevelUp%4*receiver.getMeterMax(engine)/6

	}

	/** Calculates line-clear score
	 * (This function will be called even if no lines are cleared) */
	override fun calcScore(engine:GameEngine, playerID:Int, lines:Int):Int {
		// Determines line-clear bonus
		val pts = minOf(engine.statistics.level/2+1, 5)*when {
			lines==1 -> 100 // Single
			lines==2 -> 400 // Double
			lines==3 -> 900 // Triple
			lines>=4 -> 2000 // Quadruple
			else -> 0
		}*if(engine.field.isEmpty) 10 else 1  // Perfect clear bonus

		// Add score
		if(pts>0) {
			lastscore = pts
			scgettime = 0
			engine.statistics.scoreLine += pts
			// Max-out score, lines, and level
			if(version>=2) {

				if(engine.statistics.score>MAX_SCORE) {
					engine.statistics.scoreBonus = MAX_SCORE-engine.statistics.scoreLine-engine.statistics.scoreSD-engine.statistics.scoreHD
					if(maxScoreTime<0) {
						maxScoreTime = engine.statistics.time
						engine.playSE("endingstart")
					}
				}
				if(engine.statistics.lines>MAX_LINES) {
					engine.statistics.lines = MAX_LINES
					if(maxLevelTime<0) {

						engine.playSE("grade4")
						maxLinesTime = engine.statistics.time
					}
				}
			}
		}

		// Add lines
		linesAfterLastLevelUp += lines

		engine.meterColor = when {
			linesAfterLastLevelUp>=1 -> GameEngine.METER_COLOR_YELLOW
			linesAfterLastLevelUp>=2 -> GameEngine.METER_COLOR_ORANGE
			linesAfterLastLevelUp>=3 -> GameEngine.METER_COLOR_RED
			else -> GameEngine.METER_COLOR_GREEN
		}

		// Level up
		if(linesAfterLastLevelUp>=4||levelTimer>=levelTime[minOf(engine.statistics.level, 15)]&&lines==0||engine.field.isEmpty) {
			engine.statistics.level++

			owner.backgroundStatus.fadecount = 0
			owner.backgroundStatus.fadebg = engine.statistics.level/2
			if(owner.backgroundStatus.fadebg>19) owner.backgroundStatus.fadebg = 19
			owner.backgroundStatus.fadesw = owner.backgroundStatus.fadebg!=owner.backgroundStatus.bg

			levelTimer = 0
			linesAfterLastLevelUp = 0

			engine.meterValue = 0

			setSpeed(engine)
			if(engine.statistics.level>=MAX_LEVEL) {
				engine.statistics.level = MAX_LEVEL
				if(maxLevelTime<0) {
					maxLevelTime = engine.statistics.time
					engine.playSE("levelup_section")
				}
			} else engine.playSE("levelup")

		}
		return pts
	}

	/** This function will be called when soft-drop is used */
	override fun afterSoftDropFall(engine:GameEngine, playerID:Int, fall:Int) {
		if(version>=2&&engine.speed.denominator==1) return
		engine.statistics.scoreSD += fall
	}

	/** This function will be called when hard-drop is used */
	override fun afterHardDropFall(engine:GameEngine, playerID:Int, fall:Int) {
		engine.statistics.scoreHD += fall
	}

	/** Renders game result screen */
	override fun renderResult(engine:GameEngine, playerID:Int) {
		receiver.drawMenuFont(engine, playerID, 0, 1, "PLAY DATA", COLOR.ORANGE)

		drawResultStats(engine, playerID, receiver, 3, COLOR.BLUE, Statistic.SCORE, Statistic.LINES, Statistic.LEVEL,
			Statistic.TIME)
		drawResultRank(engine, playerID, receiver, 11, COLOR.BLUE, rankingRank)
	}

	/** This function will be called when the replay data is going to be
	 * saved */
	override fun saveReplay(engine:GameEngine, playerID:Int, prop:CustomProperties) {
		saveSetting(prop)

		// Checks/Updates the ranking
		if(!owner.replayMode&&!big&&engine.ai==null) {
			updateRanking(
				if(engine.statistics.score>=MAX_SCORE) -maxScoreTime else engine.statistics.score,
				if(engine.statistics.lines>=MAX_LINES) -maxLinesTime else engine.statistics.lines,
				if(engine.statistics.level>=MAX_LEVEL) -maxLevelTime else engine.statistics.level,
				engine.statistics.time, gametype)

			if(rankingRank!=-1) {
				saveRanking(engine.ruleOpt.strRuleName)
				owner.saveModeConfig()
			}
		}
	}

	/** Load the settings */
	override fun loadSetting(prop:CustomProperties) {
		startlevel = prop.getProperty("retromania.startlevel", 0)
		gametype = prop.getProperty("retromania.gametype", 0)
		big = prop.getProperty("retromania.big", false)
		poweron = prop.getProperty("retromania.poweron", false)
		version = prop.getProperty("retromania.version", 0)
	}

	/** Save the settings */
	override fun saveSetting(prop:CustomProperties) {
		prop.setProperty("retromania.startlevel", startlevel)
		prop.setProperty("retromania.gametype", gametype)
		prop.setProperty("retromania.big", big)
		prop.setProperty("retromania.poweron", poweron)
		prop.setProperty("retromania.version", version)
	}

	/** Load the ranking */
	override fun loadRanking(prop:CustomProperties, ruleName:String) {
		for(i in 0 until RANKING_MAX)
			for(type in 0 until RANKING_TYPE) {
				rankingScore[type][i] = prop.getProperty("$ruleName.$type.score.$i", 0)
				rankingLines[type][i] = prop.getProperty("$ruleName.$type.lines.$i", 0)
				rankingLevel[type][i] = prop.getProperty("$ruleName.$type.level.$i", 0)
				rankingTime[type][i] = prop.getProperty("$ruleName.$type.time.$i", 0)

				if(rankingScore[type][i]>MAX_SCORE) rankingScore[type][i] = MAX_SCORE
				if(rankingLines[type][i]>MAX_LINES) rankingLines[type][i] = MAX_LINES
				if(rankingLevel[type][i]>MAX_LEVEL) rankingLevel[type][i] = MAX_LEVEL
			}
	}

	/** Save the ranking */
	private fun saveRanking(ruleName:String) {
		super.saveRanking(ruleName, (0 until RANKING_TYPE).flatMap {j ->
			(0 until RANKING_MAX).flatMap {i ->
				listOf(
					"$ruleName.$j.score.$i" to rankingScore[j][i],
					"$ruleName.$j.lines.$i" to rankingLines[j][i],
					"$ruleName.$j.level.$i" to rankingLevel[j][i],
					"$ruleName.$j.time.$i" to rankingTime[j][i])
			}
		})
	}

	/** Update the ranking */
	private fun updateRanking(sc:Int, li:Int, lv:Int, time:Int, type:Int) {
		rankingRank = checkRanking(sc, li, lv, time, type)

		if(rankingRank!=-1) {
			// Shift the old records
			for(i in RANKING_MAX-1 downTo rankingRank+1) {
				rankingScore[type][i] = rankingScore[type][i-1]
				rankingLines[type][i] = rankingLines[type][i-1]
				rankingLevel[type][i] = rankingLevel[type][i-1]
				rankingTime[type][i] = rankingTime[type][i-1]
			}

			// Insert a new record
			rankingScore[type][rankingRank] = sc
			rankingLines[type][rankingRank] = li
			rankingLevel[type][rankingRank] = lv
			rankingTime[type][rankingRank] = time
		}
	}

	/** This function will check the ranking and returns which place you are.
	 * (-1: Out of rank) */
	private fun checkRanking(sc:Int, li:Int, lv:Int, time:Int, type:Int):Int {
		for(i in 0 until RANKING_MAX)
			if(if(sc<0) sc<rankingScore[type][i] else sc>rankingScore[type][i]) return i
			else if(sc==rankingScore[type][i]&&if(li<0) li<rankingLines[type][i] else li>rankingLines[type][i]) return i
			else if(sc==rankingScore[type][i]&&li==rankingLines[type][i]&&
				if(lv<0) lv<rankingLevel[type][i] else lv>rankingLevel[type][i]) return i
			else if(sc==rankingScore[type][i]&&li==rankingLines[type][i]&&lv==rankingLevel[type][i]&&time<rankingTime[type][i]) return i

		return -1
	}

	companion object {
		/** Current version */
		private const val CURRENT_VERSION = 2

		/** Poweron Pattern */
		private const val STRING_POWERON_PATTERN =
			"4040050165233516506133350555213560141520224542633206134255165200333560031332022463366645230432611435"+
				"5335503262512313515002442203656664131543211220146344201325061401134610644005663441101532234006340505"+
				"4621441004021465010225623313311635326311133504346120621126223156523530636115044065300222245330252325"+
				"5563545455656660124120450663502223206465164461126135621055103645066644052535021110020361422122352566"+
				"1564343513043465103636404534525056551422631026052022163516150316500504641606133253660234134530365424"+
				"4124644510156225214120146050543513004022131140054341604166064441010614144404145451160041314635320626"+
				"0246251556635262420616451361336106153451563316660054255631510320566516465265421144640513424316315421"+
				"6644140264401653410103024436251016522052305506020020331200443440341001604426324366453255122653512056"+
				"4234334231212152312006153023444306242003331046140330636540231321265610510125435251421621035523001404"+
				"0335464640401464125332132315552404146634264364245513600336065666305002023203545052006445544450440460"

		/** Gravity table */
		private val tableDenominator =
			arrayOf(intArrayOf(48, 32, 24, 18, 14, 12, 10, 8, 6, 4, 12, 10, 8, 6, 4, 2),
				intArrayOf(48, 24, 18, 15, 12, 10, 8, 6, 4, 2, 10, 8, 6, 4, 2, 1),
				intArrayOf(40, 20, 16, 12, 10, 8, 6, 4, 2, 1, 10, 8, 6, 4, 2, 1),
				intArrayOf(30, 15, 12, 10, 8, 6, 4, 2, 1, 1, 8, 6, 4, 2, 1, 1))

		/** Time until auto-level up occers */
		private val levelTime =
			intArrayOf(3584, 2304, 2304, 2304, 2304, 2304, 2304, 2304, 2304, 3584, 3584, 2304, 2304, 2304, 2304, 3584)

		/** Name of game types */
		private val GAMETYPE_NAME = arrayOf("EASY", "NORMAL", "HARD", "HARDEST")

		/** Number of game type */
		private val GAMETYPE_MAX = tableDenominator.size

		/** Number of ranking records */
		private const val RANKING_MAX = 13

		/** Number of ranking types */
		private const val RANKING_TYPE = 4

		/** Max score */
		private const val MAX_SCORE = 999999

		/** Max lines */
		private const val MAX_LINES = 999

		/** Max level */
		private const val MAX_LEVEL = 99
	}
}
