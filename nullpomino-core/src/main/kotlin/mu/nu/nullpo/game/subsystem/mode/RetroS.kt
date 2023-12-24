/*
 * Copyright (c) 2010-2024, NullNoname
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
import mu.nu.nullpo.game.component.Piece.Companion.createQueueFromIntStr
import mu.nu.nullpo.game.event.EventReceiver.COLOR
import mu.nu.nullpo.game.event.ScoreEvent
import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.game.subsystem.mode.menu.BooleanMenuItem
import mu.nu.nullpo.game.subsystem.mode.menu.DelegateMenuItem
import mu.nu.nullpo.game.subsystem.mode.menu.LevelMenuItem
import mu.nu.nullpo.game.subsystem.mode.menu.MenuList
import mu.nu.nullpo.game.subsystem.mode.menu.StringsMenuItem
import mu.nu.nullpo.util.CustomProperties
import mu.nu.nullpo.util.GeneralUtil.toTimeStr

/** RETRO MANIA mode (Based System16, Original from NullpoUE build 121909 by Zircean) */
class RetroS:AbstractMode() {

	/** Level timer */
	private var levelTimer = 0

	/** Amount of lines cleared (It will be reset when the level increases) */
	private var linesAfterLastLevelUp = 0

	private val itemMode = StringsMenuItem("gameType", "GAME TYPE", COLOR.BLUE, 0, GAMETYPE_NAME)
	/** Selected game type */
	private var gameType:Int by DelegateMenuItem(itemMode)

	private val itemLevel = LevelMenuItem("startLevel", "LEVEL", COLOR.BLUE, 0, 0..15)
	/** Selected starting level */
	private var startLevel:Int by DelegateMenuItem(itemLevel)

	private val itemBig = BooleanMenuItem("big", "BIG", COLOR.BLUE, false)
	/** Big mode on/off */
	private var big:Boolean by DelegateMenuItem(itemBig)

	private val itemPower = BooleanMenuItem("powerOn", "POWERON", COLOR.BLUE, false)
	/** PowerOn Pattern on/off */
	private var powerOn:Boolean by DelegateMenuItem(itemPower)

	override val menu = MenuList("retromania")
	/** Version of this mode */
	private var version = 0

	/** Your place on leaderboard (-1: out of rank) */
	private var rankingRank = 0

	/** Score records */
	private val rankingScore = List(RANKING_TYPE) {MutableList(RANKING_MAX) {0L}}

	/** Line records */
	private val rankingLines = List(RANKING_TYPE) {MutableList(RANKING_MAX) {0}}

	/** Line records */
	private val rankingLevel = List(RANKING_TYPE) {MutableList(RANKING_MAX) {0}}

	/** Time records */
	private val rankingTime = List(RANKING_TYPE) {MutableList(RANKING_MAX) {-1}}

	/** Score 999,999 Reached time */
	private var maxScoreTime = -1
	/** 99 Lines Reached time */
	private var maxLinesTime = -1
	/** Level 99 Reached time */
	private var maxLevelTime = -1

	/** Returns the name of this mode */
	override val name = "Retro Mania .S"

	override val gameIntensity:Int = -1
	override val rankMap
		get() = rankMapOf(
			rankingScore.mapIndexed {a, x -> "$a.score" to x}+
				rankingLines.mapIndexed {a, x -> "$a.lines" to x}+
				rankingLevel.mapIndexed {a, x -> "$a.level" to x}+
				rankingTime.mapIndexed {a, x -> "$a.time" to x})

	/** This function will be called when
	 * the game enters the main game screen. */
	override fun playerInit(engine:GameEngine) {
		super.playerInit(engine)
		lastScore = 0
		levelTimer = 0
		linesAfterLastLevelUp = 0
		maxScoreTime = -1
		maxLinesTime = -1
		maxLevelTime = -1

		rankingRank = -1
		rankingScore.forEach {it.fill(0)}
		rankingLines.forEach {it.fill(0)}
		rankingLevel.forEach {it.fill(0)}
		rankingTime.forEach {it.fill(-1)}

		engine.twistEnable = false
		engine.b2bEnable = false
		engine.splitB2B = false
		engine.comboType = GameEngine.COMBO_TYPE_DISABLE
		engine.bigHalf = false
		engine.bigMove = false

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

		if(!owner.replayMode) version = CURRENT_VERSION
		owner.bgMan.bg = startLevel/2
		if(owner.bgMan.bg>19) owner.bgMan.bg = 19
		engine.frameColor = GameEngine.FRAME_SKIN_SG
	}

	/** Set the gravity speed
	 * @param engine GameEngine object
	 */
	private fun setSpeed(engine:GameEngine) {
		val lv = maxOf(0, minOf(engine.statistics.level, tableDenominator[gameType].size-1))

		engine.speed.gravity = 1
		engine.speed.denominator = tableDenominator[gameType][lv]

		owner.musMan.bgm = BGM.RetroS(maxOf(0, minOf(engine.statistics.level/6, 5)))
	}

	/** Main routine for game setup screen */
	override fun onSettingChanged(engine:GameEngine) {
		owner.bgMan.bg = startLevel/2
		super.onSettingChanged(engine)
	}

	/** Ready */
	override fun onReady(engine:GameEngine):Boolean {
		if(engine.statc[0]==0) {
			engine.ruleOpt.run {
				lockResetMove = false
				lockResetSpin = false
				lockResetWallkick = false
				lockResetFall = true
				softdropLock = true
				softdropMultiplyNativeSpeed = false
				softdropGravitySpeedLimit = true
				spinInitial = false
				holdEnable = false
				dasInARE = false
				dasInReady = false
				nextDisplay = 1
			}
			if(powerOn)
				engine.nextPieceArrayID = createQueueFromIntStr(STRING_POWERON_PATTERN)
		}
		return false
	}

	/** This function will be called before the game actually begins (after
	 * Ready&Go screen disappears) */
	override fun startGame(engine:GameEngine) {
		engine.statistics.level = startLevel
		engine.statistics.levelDispAdd = 1

		engine.big = big

		setSpeed(engine)
	}

	/** Renders HUD (leaderboard or game statistics) */
	override fun renderLast(engine:GameEngine) {
		receiver.drawScoreFont(engine, 0, 0, name, COLOR.GREEN)
		receiver.drawScoreFont(engine, 0, 1, "(${GAMETYPE_NAME[gameType]} SPEED)", COLOR.GREEN)

		if(engine.stat==GameEngine.Status.SETTING||engine.stat==GameEngine.Status.RESULT&&!owner.replayMode) {
			// Leaderboard
			if(!owner.replayMode&&!big&&startLevel==0&&engine.ai==null) {
				val topY = if(receiver.nextDisplayType==2) 6 else 4
				receiver.drawScoreFont(engine, 3, topY-1, "SCORE LINE LV TIME", COLOR.BLUE)

				for(i in 0..<RANKING_MAX) {
					receiver.drawScoreGrade(engine, 0, topY+i, "${i+1}", COLOR.YELLOW)
					receiver.drawScoreNum(
						engine, 2, topY+i,
						if(rankingScore[gameType][i]>=0) "%6d".format(rankingScore[gameType][i])
						else rankingScore[gameType][i].toTimeStr, i==rankingRank
					)
					receiver.drawScoreNum(
						engine, 8, topY+i,
						if(rankingLines[gameType][i]>=0) "%3d".format(rankingLines[gameType][i])
						else rankingLines[gameType][i].toTimeStr, i==rankingRank
					)
					receiver.drawScoreNum(
						engine, 11, topY+i,
						if(rankingLevel[gameType][i]>=0) "%2d".format(rankingLevel[gameType][i])
						else rankingLevel[gameType][i].toTimeStr, i==rankingRank
					)
					receiver.drawScoreNum(engine, 15, topY+i, rankingTime[gameType][i].toTimeStr, i==rankingRank)
				}
			}
		} else {
			// Game statistics
			receiver.drawScoreFont(engine, 1, 3, "SCORE", COLOR.CYAN)
			receiver.drawScoreFont(engine, 0, 4, "%6d".format(scDisp), COLOR.CYAN)

			receiver.drawScoreFont(engine, 1, 6, "LINES", COLOR.CYAN)
			receiver.drawScoreFont(engine, 0, 7, "%6d".format(engine.statistics.lines), COLOR.CYAN)

			receiver.drawScoreFont(engine, 1, 9, "LEVEL", COLOR.CYAN)
			receiver.drawScoreFont(engine, 0, 10, "%6d".format(engine.statistics.level), COLOR.CYAN)

			receiver.drawScoreFont(engine, 0, 13, "Time", COLOR.BLUE)
			receiver.drawScoreFont(engine, 0, 14, engine.statistics.time.toTimeStr, COLOR.BLUE)

			receiver.drawScoreNano(engine, 0, 31, "${4-linesAfterLastLevelUp} LINES TO GO", COLOR.CYAN, .5f)
			receiver.drawScoreNano(
				engine, 0, 32, "OR "+(levelTime[minOf(engine.statistics.level, 15)]-levelTimer).toTimeStr,
				COLOR.CYAN, .5f
			)
		}
	}

	/** This function will be called when the game timer updates */
	override fun onLast(engine:GameEngine) {
		super.onLast(engine)
		if(engine.timerActive) levelTimer++
		// Update the meter
		engine.meterValue = levelTimer*1f/levelTime[minOf(engine.statistics.level, 15)]
		engine.meterValue += (1-engine.meterValue)*linesAfterLastLevelUp%4/4
	}

	/** Calculates line-clear score
	 * (This function will be called even if no lines are cleared) */
	override fun calcScore(engine:GameEngine, ev:ScoreEvent):Int {
		// Determines line-clear bonus
		val li = ev.lines
		val pts = minOf(engine.statistics.level/2+1, 5)*when {
			li==1 -> 100 // Single
			li==2 -> 400 // Double
			li==3 -> 900 // Triple
			li>=4 -> 2000 // Quadruple
			else -> 0
		}*if(engine.field.isEmpty) 10 else 1  // Perfect clear bonus

		// Add score
		if(pts>0) {
			lastScore = pts
			engine.statistics.scoreLine += pts
			// Max-out score, lines, and level
			if(version>=2) {
				if(engine.statistics.score>MAX_SCORE) {
					engine.statistics.scoreBonus =
						MAX_SCORE-engine.statistics.scoreLine-engine.statistics.scoreSD-engine.statistics.scoreHD
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
		linesAfterLastLevelUp += li

		engine.meterColor = GameEngine.METER_COLOR_LEVEL

		// Level up
		if(linesAfterLastLevelUp>=4||levelTimer>=levelTime[minOf(engine.statistics.level, 15)]&&li==0||engine.field.isEmpty) {
			engine.statistics.level++

			owner.bgMan.nextBg = minOf(engine.statistics.level/2, 19)

			levelTimer = 0
			linesAfterLastLevelUp = 0

			engine.meterValue = 0f

			setSpeed(engine)
			if(engine.statistics.level>=MAX_LEVEL) {
				engine.statistics.level = MAX_LEVEL
				if(maxLevelTime<0) {
					maxLevelTime = engine.statistics.time
					engine.playSE("levelup_section")
				}
			}
			engine.playSE("levelup")
		}
		return pts
	}

	/** This function will be called when soft-drop is used */
	override fun afterSoftDropFall(engine:GameEngine, fall:Int) {
		if(version>=2&&engine.speed.denominator==1) return
		engine.statistics.scoreSD += fall
	}

	/** This function will be called when hard-drop is used */
	override fun afterHardDropFall(engine:GameEngine, fall:Int) {
		engine.statistics.scoreHD += fall
	}

	/** Renders game result screen */
	override fun renderResult(engine:GameEngine) {
		receiver.drawMenuFont(engine, 0, 1, "PLAY DATA", COLOR.ORANGE)

		drawResultStats(
			engine, receiver, 3, COLOR.BLUE, Statistic.SCORE, Statistic.LINES, Statistic.LEVEL, Statistic.TIME
		)
		drawResultRank(engine, receiver, 11, COLOR.BLUE, rankingRank)
	}

	/** This function will be called when the replay data is going to be
	 * saved */
	override fun saveReplay(engine:GameEngine, prop:CustomProperties):Boolean {
		saveSetting(engine, prop)

		// Checks/Updates the ranking
		if(!owner.replayMode&&!big&&engine.ai==null) {
			updateRanking(
				if(engine.statistics.score>=MAX_SCORE) -maxScoreTime.toLong() else engine.statistics.score,
				if(engine.statistics.lines>=MAX_LINES) -maxLinesTime else engine.statistics.lines,
				if(engine.statistics.level>=MAX_LEVEL) -maxLevelTime else engine.statistics.level,
				engine.statistics.time, gameType
			)

			if(rankingRank!=-1) return true
		}
		return false
	}

	/** Update the ranking */
	private fun updateRanking(sc:Long, li:Int, lv:Int, time:Int, type:Int) {
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
	private fun checkRanking(sc:Long, li:Int, lv:Int, time:Int, type:Int):Int {
		for(i in 0..<RANKING_MAX)
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
			listOf(
				listOf(48, 32, 24, 18, 14, 12, 10, 8, 6, 4, 12, 10, 8, 6, 4, 2),
				listOf(48, 24, 18, 15, 12, 10, 8, 6, 4, 2, 10, 8, 6, 4, 2, 1),
				listOf(40, 20, 16, 12, 10, 8, 6, 4, 2, 1, 10, 8, 6, 4, 2, 1),
				listOf(30, 15, 12, 10, 8, 6, 4, 2, 1, 1, 8, 6, 4, 2, 1, 1)
			)

		/** Time until auto-level up occers */
		private val levelTime =
			listOf(3584, 2304, 2304, 2304, 2304, 2304, 2304, 2304, 2304, 3584, 3584, 2304, 2304, 2304, 2304, 3584)

		/** Name of game types */
		private val GAMETYPE_NAME = listOf("EASY", "NORMAL", "HARD", "HARDEST")

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
