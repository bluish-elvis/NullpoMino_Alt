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

/** RETRO MANIA mode (Based System16, Original from NullpoUE build 121909 by Zircean) */
class RetroMania:AbstractMode() {

	/** Amount of points you just get from line clears */
	private var lastscore:Int = 0

	/** Elapsed time from last line clear (lastscore is displayed to screen
	 * until this reaches to 120) */
	private var scgettime:Int = 0

	/** Selected game type */
	private var gametype:Int = 0

	/** Selected starting level */
	private var startlevel:Int = 0

	/** Level timer */
	private var levelTimer:Int = 0

	/** Amount of lines cleared (It will be reset when the level increases) */
	private var linesAfterLastLevelUp:Int = 0

	/** Big mode on/off */
	private var big:Boolean = false

	/** Poweron Pattern on/off */
	private var poweron:Boolean = false

	/** Version of this mode */
	private var version:Int = 0

	/** Your place on leaderboard (-1: out of rank) */
	private var rankingRank:Int = 0

	/** Score records */
	private var rankingScore:Array<IntArray> = Array(RANKING_TYPE) {IntArray(RANKING_MAX)}

	/** Line records */
	private var rankingLines:Array<IntArray> = Array(RANKING_TYPE) {IntArray(RANKING_MAX)}

	/** Time records */
	private var rankingTime:Array<IntArray> = Array(RANKING_TYPE) {IntArray(RANKING_MAX)}

	/** Returns the name of this mode */
	override val name:String
		get() = "Retro Mania .S"

	override val gameIntensity:Int = -1

	/** This function will be called when
	 * the game enters the main game screen. */
	override fun playerInit(engine:GameEngine, playerID:Int) {
		super.playerInit(engine, playerID)
		lastscore = 0
		scgettime = 0
		levelTimer = 0
		linesAfterLastLevelUp = 0

		rankingRank = -1
		rankingScore = Array(RANKING_TYPE) {IntArray(RANKING_MAX)}
		rankingLines = Array(RANKING_TYPE) {IntArray(RANKING_MAX)}
		rankingTime = Array(RANKING_TYPE) {IntArray(RANKING_MAX)}

		engine.twistEnable = false
		engine.b2bEnable = false
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

		if(!owner.replayMode) {
			loadSetting(owner.modeConfig)
			loadRanking(owner.recordProp, engine.ruleopt.strRuleName)
			version = CURRENT_VERSION
		} else
			loadSetting(owner.replayProp)

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
		drawMenu(engine, playerID, receiver, 0, COLOR.BLUE, 0, "DIFFICULTY", GAMETYPE_NAME[gametype], "Level", "$startlevel", "BIG", GeneralUtil.getONorOFF(big), "POWERON", GeneralUtil.getONorOFF(poweron))
	}

	/** Ready */
	override fun onReady(engine:GameEngine, playerID:Int):Boolean {
		if(engine.statc[0]==0) {

			engine.ruleopt.run {
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
				areCancelMove = false
				areCancelRotate = false
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

		owner.bgmStatus.bgm = BGM.RETRO_S(0)
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
				receiver.drawScoreFont(engine, playerID, 3, topY-1, "SCORE  LINE TIME", COLOR.BLUE, scale)

				for(i in 0 until RANKING_MAX) {
					receiver.drawScoreNum(engine, playerID, 0, topY+i,
						String.format("%2d", i+1), COLOR.YELLOW, scale)
					receiver.drawScoreNum(engine, playerID, 3, topY+i, "${rankingScore[gametype][i]}", i==rankingRank, scale)
					receiver.drawScoreNum(engine, playerID, 10, topY+i, "${rankingLines[gametype][i]}", i==rankingRank, scale)
					receiver.drawScoreNum(engine, playerID, 15, topY+i, GeneralUtil.getTime(rankingTime[gametype][i]), i==rankingRank, scale)
				}
			}
		} else {
			// Game statistics
			receiver.drawScoreFont(engine, playerID, 0, 3, "Score", COLOR.BLUE)
			val strScore:String = if(lastscore==0||scgettime>=120)
				"${engine.statistics.score}"
			else
				"${engine.statistics.score}(+$lastscore)"
			receiver.drawScoreNum(engine, playerID, 0, 4, strScore, 2f)

			receiver.drawScoreFont(engine, playerID, 0, 6, "LINE", COLOR.BLUE)
			receiver.drawScoreNum(engine, playerID, 0, 7, engine.statistics.lines.toString(), 2f)

			receiver.drawScoreFont(engine, playerID, 0, 9, "Level", COLOR.BLUE)
			receiver.drawScoreNum(engine, playerID, 0, 10, "${engine.statistics.level}", 2f)

			receiver.drawScoreFont(engine, playerID, 0, 12, "Time", COLOR.BLUE)
			receiver.drawScoreNum(engine, playerID, 0, 13, GeneralUtil.getTime(engine.statistics.time), 2f)

			//receiver.drawScore(engine, playerID, 0, 15, String.valueOf(linesAfterLastLevelUp));
			//receiver.drawScore(engine, playerID, 0, 16, GeneralUtil.getTime(levelTime[minOf(engine.statistics.level,15)] - levelTimer));
		}
	}

	/** This function will be called when the game timer updates */
	override fun onLast(engine:GameEngine, playerID:Int) {
		scgettime++
		if(engine.timerActive) levelTimer++

		// Max-out score, lines, and level
		if(version>=2) {
			if(engine.statistics.score>MAX_SCORE) engine.statistics.scoreBonus = engine.statistics.score-MAX_SCORE
			if(engine.statistics.lines>MAX_LINES) engine.statistics.lines = MAX_LINES
			if(engine.statistics.level>MAX_LEVEL) engine.statistics.level = MAX_LEVEL
		}
	}

	/** Calculates line-clear score
	 * (This function will be called even if no lines are cleared) */
	override fun calcScore(engine:GameEngine, playerID:Int, lines:Int):Int {
		// Determines line-clear bonus
		var pts = 0
		val mult = minOf(engine.statistics.level/2+1, 5)
		when {
			lines==1 -> pts += 100*mult // Single
			lines==2 -> pts += 400*mult // Double
			lines==3 -> pts += 900*mult // Triple
			lines>=4 -> pts += 2000*mult // Quadruple
		}

		// Perfect clear bonus
		if(engine.field!!.isEmpty) pts *= 10

		// Add score
		if(pts>0) {
			lastscore = pts
			scgettime = 0
			engine.statistics.scoreLine += pts
		}

		// Add lines
		linesAfterLastLevelUp += lines

		// Update the meter
		engine.meterValue = linesAfterLastLevelUp%4*receiver.getMeterMax(engine)/3
		engine.meterColor = GameEngine.METER_COLOR_GREEN
		if(linesAfterLastLevelUp>=1) engine.meterColor = GameEngine.METER_COLOR_YELLOW
		if(linesAfterLastLevelUp>=2) engine.meterColor = GameEngine.METER_COLOR_ORANGE
		if(linesAfterLastLevelUp>=3) engine.meterColor = GameEngine.METER_COLOR_RED

		// Level up
		if(linesAfterLastLevelUp>=4||levelTimer>=levelTime[minOf(engine.statistics.level, 15)]&&lines==0) {
			engine.statistics.level++

			owner.backgroundStatus.fadecount = 0
			owner.backgroundStatus.fadebg = engine.statistics.level/2
			if(owner.backgroundStatus.fadebg>19) owner.backgroundStatus.fadebg = 19
			owner.backgroundStatus.fadesw = owner.backgroundStatus.fadebg!=owner.backgroundStatus.bg

			levelTimer = 0
			linesAfterLastLevelUp = 0

			engine.meterValue = 0

			setSpeed(engine)
			engine.playSE("levelup")
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

		drawResultStats(engine, playerID, receiver, 3, COLOR.BLUE, Statistic.SCORE, Statistic.LINES, Statistic.LEVEL, Statistic.TIME)
		drawResultRank(engine, playerID, receiver, 11, COLOR.BLUE, rankingRank)
	}

	/** This function will be called when the replay data is going to be
	 * saved */
	override fun saveReplay(engine:GameEngine, playerID:Int, prop:CustomProperties) {
		saveSetting(prop)

		// Checks/Updates the ranking
		if(!owner.replayMode&&!big&&engine.ai==null) {
			updateRanking(engine.statistics.score, engine.statistics.lines, engine.statistics.time, gametype)

			if(rankingRank!=-1) {
				saveRanking(owner.recordProp, engine.ruleopt.strRuleName)
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
			for(gametypeIndex in 0 until RANKING_TYPE) {
				rankingScore[gametypeIndex][i] = prop.getProperty("retromania.ranking.$ruleName.$gametypeIndex.score.$i", 0)
				rankingLines[gametypeIndex][i] = prop.getProperty("retromania.ranking.$ruleName.$gametypeIndex.lines.$i", 0)
				rankingTime[gametypeIndex][i] = prop.getProperty("retromania.ranking.$ruleName.$gametypeIndex.time.$i", 0)

				if(rankingScore[gametypeIndex][i]>MAX_SCORE) rankingScore[gametypeIndex][i] = MAX_SCORE
				if(rankingLines[gametypeIndex][i]>MAX_LINES) rankingLines[gametypeIndex][i] = MAX_LINES
			}
	}

	/** Save the ranking */
	private fun saveRanking(prop:CustomProperties, ruleName:String) {
		for(i in 0 until RANKING_MAX)
			for(gametypeIndex in 0 until RANKING_TYPE) {
				prop.setProperty("retromania.ranking.$ruleName.$gametypeIndex.score.$i", rankingScore[gametypeIndex][i])
				prop.setProperty("retromania.ranking.$ruleName.$gametypeIndex.lines.$i", rankingLines[gametypeIndex][i])
				prop.setProperty("retromania.ranking.$ruleName.$gametypeIndex.time.$i", rankingTime[gametypeIndex][i])
			}
	}

	/** Update the ranking */
	private fun updateRanking(sc:Int, li:Int, time:Int, type:Int) {
		rankingRank = checkRanking(sc, li, time, type)

		if(rankingRank!=-1) {
			// Shift the old records
			for(i in RANKING_MAX-1 downTo rankingRank+1) {
				rankingScore[type][i] = rankingScore[type][i-1]
				rankingLines[type][i] = rankingLines[type][i-1]
				rankingTime[type][i] = rankingTime[type][i-1]
			}

			// Insert a new record
			rankingScore[type][rankingRank] = sc
			rankingLines[type][rankingRank] = li
			rankingTime[type][rankingRank] = time
		}
	}

	/** This function will check the ranking and returns which place you are.
	 * (-1: Out of rank) */
	private fun checkRanking(sc:Int, li:Int, time:Int, type:Int):Int {
		for(i in 0 until RANKING_MAX)
			if(sc>rankingScore[type][i])
				return i
			else if(sc==rankingScore[type][i]&&li>rankingLines[type][i])
				return i
			else if(sc==rankingScore[type][i]&&li==rankingLines[type][i]&&time<rankingTime[type][i]) return i

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
			arrayOf(intArrayOf(48, 32, 24, 18, 14, 12, 10, 8, 6, 4, 12, 10, 8, 6, 4, 2), intArrayOf(48, 24, 18, 15, 12, 10, 8, 6, 4, 2, 10, 8, 6, 4, 2, 1), intArrayOf(40, 20, 16, 12, 10, 8, 6, 4, 2, 1, 10, 8, 6, 4, 2, 1), intArrayOf(30, 15, 12, 10, 8, 6, 4, 2, 1, 1, 8, 6, 4, 2, 1, 1))

		/** Time until auto-level up occers */
		private val levelTime =
			intArrayOf(3584, 2304, 2304, 2304, 2304, 2304, 2304, 2304, 2304, 3584, 3584, 2304, 2304, 2304, 2304, 3584)

		/** Name of game types */
		private val GAMETYPE_NAME = arrayOf("EASY", "NORMAL", "HARD", "HARDEST")

		/** Number of game type */
		private const val GAMETYPE_MAX = 4

		/** Number of ranking records */
		private const val RANKING_MAX = 10

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
