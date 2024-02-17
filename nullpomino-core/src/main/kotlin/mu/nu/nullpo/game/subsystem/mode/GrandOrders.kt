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
import mu.nu.nullpo.game.component.LevelData
import mu.nu.nullpo.game.event.EventReceiver.COLOR
import mu.nu.nullpo.game.event.ScoreEvent
import mu.nu.nullpo.game.net.NetUtil
import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.game.subsystem.mode.menu.BooleanMenuItem
import mu.nu.nullpo.game.subsystem.mode.menu.DelegateMenuItem
import mu.nu.nullpo.game.subsystem.mode.menu.LevelMenuItem
import mu.nu.nullpo.game.subsystem.mode.menu.MenuList
import mu.nu.nullpo.game.subsystem.mode.menu.StringsMenuItem
import mu.nu.nullpo.gui.common.BaseFont
import mu.nu.nullpo.util.CustomProperties
import mu.nu.nullpo.util.GeneralUtil.toInt
import mu.nu.nullpo.util.GeneralUtil.toTimeStr
import kotlin.random.Random

/** MISSION MANIA mode */
class GrandOrders:NetDummyMode() {
	/** Remaining level time */
	private var levelTimer = 0
	private var lastLineTime = 0

	/** Original level time */
	private var levelTimerMax = 0

	/** Current BGM number */
	private var bgmLv = 0

	/** Elapsed ending time */
	private var rollTime = 0

	/** Ending started flag */
	private var rollStarted = false

	/** Section time */
	private val sectionTime = MutableList(courses.maxOf {it.goalLevel}) {0}

	/** Number of sections completed */
	private var sectionsDone = 0

	/** Average section time */
	private val sectionAvgTime
		get() = sectionTime.filter {it>0}.average().toFloat()

	private val itemMode = StringsMenuItem("goalType", "COURSE", COLOR.BLUE, 0, courses.map {it.showName})
	/** Game type */
	private var goalType:Int by DelegateMenuItem(itemMode)
	private var nowCourse
		get() = courses[maxOf(0, goalType%courses.size)]
		set(value) {
			goalType = courses.indexOfFirst {it==value}
		}

	/** Current Mission No. */
	private var missionPos = 0
	private val nowMission get() = nowCourse.missions[missionPos]

	/** Current lines (for level up) */
	private val norm get() = nowMission.prog

	/** Lines for Next level */
	private val nextLv get() = nowMission.norm

	private val itemLevel =
		LevelMenuItem("startlevel", "LEVEL", COLOR.RED, 0, 0..(courses.maxOfOrNull {it.goalLevel} ?: 0), true, true)
	/** Selected starting level */
	private var startLevel:Int by DelegateMenuItem(itemLevel)

	private val itemBig = BooleanMenuItem("big", "BIG", COLOR.BLUE, false)
	/** BigMode */
	private var big:Boolean by DelegateMenuItem(itemBig)

	private val itemST = BooleanMenuItem("showsectiontime", "SHOW STIME", COLOR.BLUE, true)
	/** Show section time */
	private var showST:Boolean by DelegateMenuItem(itemST)

	override val menu = MenuList("missioncourse", itemMode, itemLevel, itemST, itemBig)
	/** Version of this mode */
	private var version = 0

	/** Your place on leaderboard (-1: out of rank) */
	private var rankingRank = 0

	/** Ranking ecords */
	private val rankingLines = List(COURSE_MAX) {MutableList(RANKING_MAX) {0}}
	private val rankingLives = List(COURSE_MAX) {MutableList(RANKING_MAX) {0}}
	private val rankingTime = List(COURSE_MAX) {MutableList(RANKING_MAX) {-1}}
	private val rankingRollClear = List(COURSE_MAX) {MutableList(RANKING_MAX) {0}}

	/** Returns the name of this mode */
	override val name = "Grand Roads"
	override val gameIntensity:Int
		get() = 2/* when(nowCourse) {
			Mission.HARD, Mission.CHALLENGE -> 1
			Mission.HARDEST, Mission.SUPER, Mission.SURVIVAL -> 2
			Mission.XTREME, Mission.HELL, Mission.Dark, Mission.VOID -> 3
			else -> 0
		}*/
	override val propRank
		get() = rankMapOf(
			rankingLines.mapIndexed {a, x -> "$a.lines" to x}+
				rankingLives.mapIndexed {a, x -> "$a.lives" to x}+
				rankingTime.mapIndexed {a, x -> "$a.time" to x}+
				rankingRollClear.mapIndexed {a, x -> "$a.rollClear" to x})

	/** This function will be called when the game enters the main game
	 * screen. */
	override fun playerInit(engine:GameEngine) {
		super.playerInit(engine)
		goalType = 0
		startLevel = 0
		missionPos = 0
		rollTime = 0
		lastLineTime = 0
		rollStarted = false
		sectionsDone = 0
		big = false
		showST = true

		rankingRank = -1
		rankingLines.forEach {it.fill(0)}
		rankingLives.forEach {it.fill(0)}
		rankingTime.forEach {it.fill(-1)}
		rankingRollClear.forEach {it.fill(0)}

		engine.twistEnable = false
		engine.b2bEnable = false
		engine.splitB2B = false
		engine.comboType = GameEngine.COMBO_TYPE_DISABLE
		engine.frameColor = GameEngine.FRAME_COLOR_WHITE
		engine.bigHalf = true
		engine.bigMove = true
		engine.staffrollEnable = false
		engine.staffrollNoDeath = false

		netPlayerInit(engine)
		// NET: Load name
		if(!owner.replayMode) version = CURRENT_VERSION else netPlayerName = engine.owner.replayProp.getProperty(
			"${engine.playerID}.net.netPlayerName", ""
		)

		engine.owner.bgMan.bg = startLevel
	}

	/** Set the gravity speed and some other things
	 * @param engine GameEngine object
	 */
	private fun setSpeed(engine:GameEngine) {
		val nowMission = nowCourse.missions[engine.statistics.level]
		engine.speed.replace(nowMission.let {it.speeds[it.lv]})
		// Show outline only
		engine.blockShowOutlineOnly = nowMission is Mission.Dark
		// Bone blocks
		engine.bone = nowMission is Mission.Monochrome

		// for test
		/* engine.speed.are = 25; engine.speed.areLine = 25;
 engine.speed.lineDelay = 10; engine.speed.lockDelay = 30;
 engine.speed.das = 12; levelTimerMax = levelTimer = 3600 * 3; */

		levelTimer = nowMission.time*60
		levelTimerMax = levelTimer
		// Blocks fade for HELL-X
		engine.blockHidden = if(nowMission is Mission.Dark) nowMission.misc else -1


		lastLineTime = levelTimer
	}

	/** Set Pressure Hidden params
	 * @param engine GameEngine
	 */
	private fun setHeboHidden(engine:GameEngine) {
		if(engine.heboHiddenEnable) when(nowMission.misc) {
			1 -> {
				engine.heboHiddenYLimit = 15
				engine.heboHiddenTimerMax = (engine.heboHiddenYNow+2)*120
			}
			2 -> {
				engine.heboHiddenYLimit = 17
				engine.heboHiddenTimerMax = (engine.heboHiddenYNow+1)*90
			}
			3 -> {
				engine.heboHiddenYLimit = 19
				engine.heboHiddenTimerMax = engine.heboHiddenYNow*60+60
			}
			4 -> {
				engine.heboHiddenYLimit = 19
				engine.heboHiddenTimerMax = engine.heboHiddenYNow*45+45
			}
			5 -> {
				engine.heboHiddenYLimit = 19
				engine.heboHiddenTimerMax = engine.heboHiddenYNow*30+30
			}
			6 -> {
				engine.heboHiddenYLimit = 19
				engine.heboHiddenTimerMax = engine.heboHiddenYNow*7+15
			}
			7 -> {
				engine.heboHiddenYLimit = 20
				engine.heboHiddenTimerMax = engine.heboHiddenYNow*3+15
			}
		} else engine.heboHiddenYLimit = 0
	}

	/** Main routine for game setup screen */
	override fun onSettingChanged(engine:GameEngine) {
		if(startLevel>nowCourse.goalLevel-1) startLevel = if(menuCursor!=menu.items.indexOf(itemLevel)) 0 else nowCourse.goalLevel-1
		engine.owner.bgMan.bg = startLevel
		engine.statistics.level = startLevel
		missionPos = engine.statistics.level
		setSpeed(engine)
		super.onSettingChanged(engine)
	}

	/** Ready screen */
	override fun onReady(engine:GameEngine):Boolean {
		if(engine.statc[0]==0) {
			engine.statistics.level = startLevel
			engine.statistics.levelDispAdd = 1
			engine.big = big
			missionPos = engine.statistics.level
			setSpeed(engine)
			bgmLv = maxOf(0, nowCourse.bgmChange.indexOfLast {it<=startLevel}.let {if(it<0) nowCourse.bgmChange.size-1 else it})
			nowMission.ready(engine)
		}
		return false
	}

	/** This function will be called before the game actually begins
	 * (after Ready&Go screen disappears) */
	override fun startGame(engine:GameEngine) {
		if(owner.musMan.fadeSW) {
			bgmLv = nowCourse.bgmChange.count {it<=engine.statistics.level}
			owner.musMan.bgm = if(netIsWatch) BGM.Silent else nowCourse.bgmList[bgmLv]
			owner.musMan.fadeSW = false
		}
	}

	/** Renders HUD (leaderboard or game statistics) */
	override fun renderLast(engine:GameEngine) {
		if(owner.menuOnly) return

		receiver.drawScoreFont(engine, 0, -1, name, COLOR.PURPLE)
		receiver.drawScoreFont(engine, 0, 0, "TIME ATTACK", COLOR.PURPLE, .75f)
		receiver.drawScoreFont(engine, 0, 1, "${nowCourse.showName} COURSE", COLOR.PURPLE)
		//receiver.drawScore(engine, playerID, -1, -4*2, "DECORATION", scale = .5f);
		//receiver.drawScoreBadges(engine, playerID,0,-3,100,decoration);
		//receiver.drawScoreBadges(engine, playerID,5,-4,100,decTemp);
		if(engine.stat==GameEngine.Status.SETTING||engine.stat==GameEngine.Status.RESULT&&!owner.replayMode) {
			if(!owner.replayMode&&startLevel==0&&!big&&engine.ai==null&&!netIsWatch) {
				receiver.drawScoreFont(engine, 8, 3, "Time", COLOR.BLUE)

				for(i in 0..<RANKING_MAX) {
					val cleared = rankingRollClear[goalType][i]>0
					val gColor = when {
						rankingRollClear[goalType][i]==1 -> COLOR.GREEN
						rankingRollClear[goalType][i]==2 -> COLOR.ORANGE
						else -> COLOR.WHITE
					}
					receiver.drawScoreGrade(engine, 0, 4+i, "%2d".format(i+1), if(i==rankingRank) COLOR.RED else COLOR.YELLOW)
					receiver.drawScoreNum(engine, 8, 4+i, rankingTime[goalType][i].toTimeStr, gColor)
					receiver.drawScoreNano(engine, 10, 4+i, if(cleared) "LIVES\nREMAINED" else "LINES\nCLEARED", gColor, .5f)
					receiver.drawScoreNum(
						engine, 2, 4+i, "%3d".format(if(cleared) rankingLives[goalType][i] else rankingLines[goalType][i]), gColor
					)
				}
			}
		} else {
			receiver.drawScoreFont(engine, 0, 3, "Missions", COLOR.BLUE)
			receiver.drawScoreNum(engine, 5, 2, "%02d".format(engine.statistics.level+1), 2f)
			receiver.drawScoreNum(engine, 8, 3, "/%3d".format(nowCourse.goalLevel))

			receiver.drawScoreFont(engine, 0, 6, nowMission.showName, COLOR.BLUE)
			receiver.drawScoreSpeed(engine, 0, 7, engine.speed.rank, 6f)
			receiver.drawScoreNum(engine, 0, 8, "%3d/%3d".format(norm, nextLv))

			receiver.drawScoreFont(engine, 0, 10, "TIME LIMIT", COLOR.BLUE)
			receiver.drawScoreNum(engine, 0, 11, levelTimer.toTimeStr, levelTimer in 1..<600&&levelTimer%4==0, 2f)

			receiver.drawScoreFont(engine, 0, 13, "TOTAL TIME", COLOR.BLUE)
			receiver.drawScoreNum(engine, 0, 14, engine.statistics.time.toTimeStr, 2f)

			// Remaining ending time
			if(engine.gameActive&&engine.ending==2&&engine.staffrollEnable) {
				var time = ROLLTIMELIMIT-rollTime
				if(time<0) time = 0
				receiver.drawScoreFont(engine, 0, 17, "ROLL TIME", COLOR.BLUE)
				receiver.drawScoreNum(engine, 0, 18, time.toTimeStr, time>0&&time<10*60, 2f)
			}

			// Section time
			if(showST&&sectionTime.isNotEmpty()&&!netIsWatch) {
				val x = if(receiver.nextDisplayType==2) 25 else 12
				val y = if(receiver.nextDisplayType==2) 4 else 2

				receiver.drawScoreFont(engine, x, y, "SECTION TIME", COLOR.BLUE)

				val l = maxOf(0, engine.statistics.level-20)
				var i = l
				while(i<sectionTime.size) {
					if(sectionTime[i]>0) {
						val strSeparator = if(i==engine.statistics.level&&engine.ending==0) "+" else "-"
						val strSectionTime = "%2d%s%s".format(i+1, strSeparator, sectionTime[i].toTimeStr)
						receiver.drawScoreNum(engine, x+1, y+1+i-l, strSectionTime)
					}
					i++
				}
				receiver.drawScoreFont(engine, 0, 13, "AVERAGE", COLOR.BLUE)
				receiver.drawScoreNum(engine, 0, 14, (engine.statistics.time/(sectionsDone+1)).toTimeStr, 2f)
			}
		}
		super.renderLast(engine)
	}

	/** This function will be called when the piece is active */
	override fun onMove(engine:GameEngine):Boolean {
		// Enable timer again after the levelup
		if(engine.ending==0&&engine.statc[0]==0&&!engine.timerActive&&!engine.holdDisable) engine.timerActive = true

		// Ending start
		if(engine.ending==2&&engine.staffrollEnable&&!rollStarted&&!netIsWatch) {
			rollStarted = true
			owner.musMan.bgm = BGM.Finale(2)
			owner.musMan.fadeSW = false
			if(nowCourse.missions[engine.statistics.level] is Mission.Dark) {
				engine.blockHidden = engine.ruleOpt.lockFlash
				engine.blockHiddenAnim = false
				engine.blockOutlineType = GameEngine.BLOCK_OUTLINE_NONE
			}
		}

		return super.onMove(engine)
	}

	/** This function will be called when the game timer updates */
	override fun onLast(engine:GameEngine) {
		super.onLast(engine)
		// Level timer
		if(engine.timerActive&&engine.ending==0) if(levelTimer>0) {
			levelTimer--
			if(levelTimer<=600&&levelTimer%60==0) engine.playSE("countdown")
		} else if(!netIsWatch) {
			engine.resetStatc()
			engine.stat = GameEngine.Status.GAMEOVER
		}

		// Update meter
		if(engine.ending==0&&levelTimerMax!=0) {
			engine.meterValue = levelTimer/2f/levelTimerMax
			if(norm%10>0) engine.meterValue += ((1-engine.meterValue)*(norm%10)*levelTimer/lastLineTime/10)
			engine.meterColor = GameEngine.METER_COLOR_LIMIT
		}

		// Section time
		if(engine.timerActive&&engine.ending==0) if(engine.statistics.level>=0&&engine.statistics.level<sectionTime.size) {
			sectionTime[engine.statistics.level] = engine.statistics.time-sectionTime.take(engine.statistics.level).sum()
			setHeboHidden(engine)
		}

		// Ending
		if(engine.gameActive&&engine.ending==2) {
			rollTime++

			// Update meter
			val remainRollTime = ROLLTIMELIMIT-rollTime
			engine.meterValue = remainRollTime*1f/ROLLTIMELIMIT
			engine.meterColor = GameEngine.METER_COLOR_LIMIT
			// Completed
			if(rollTime>=ROLLTIMELIMIT&&!netIsWatch) {
				engine.statistics.rollClear = 2
				engine.gameEnded()
				engine.resetStatc()
				engine.stat = GameEngine.Status.EXCELLENT
			}
		}
	}

	override fun onGameOver(engine:GameEngine):Boolean {
		if(engine.statc[0]==0) {
			if(engine.lives>0) setSpeed(engine)
		}
		return super.onGameOver(engine)
	}
	/** Calculates line-clear score
	 * (This function will be called even if no lines are cleared) */
	override fun calcScore(engine:GameEngine, ev:ScoreEvent):Int {
		// Don't do anything during the ending
		if(engine.ending!=0) return 0

		val mission = nowCourse.missions[engine.statistics.level]
		val lv = engine.statistics.level

		// Add lines to norm
		val checked = mission.checkProgress(engine, ev)
		if(checked) lastLineTime = levelTimer

		// Level up
		if(checked&&norm>=nextLv) {
			// Game completed
			if(lv+engine.statistics.levelDispAdd>=nowCourse.goalLevel) {
				owner.musMan.fadeSW = true
				engine.playSE("levelup_section")

				// Update section time
				if(engine.timerActive) sectionsDone++

				engine.ending = 1
				engine.timerActive = false

				if(mission is Mission.Dark||mission is Mission.Shutter) {
					// HELL-X ending & VOID ending
					engine.staffrollEnable = true
					engine.statistics.rollClear = 1
				} else {
					engine.gameEnded()
					engine.statistics.rollClear = 2
				}
			} else {
				nowMission.unload(engine)
				if(nowCourse.bgmChange.any {it==lv}) {
					owner.musMan.fadeSW = true// BGM fadeout
					engine.playSE("levelup_section")
				}
				engine.playSE("levelup")
				engine.statistics.level++
				missionPos = engine.statistics.level
				owner.bgMan.nextBg = engine.statistics.level+nowCourse.bgOffset

				sectionsDone++

				engine.timerActive = false // Stop timer until the next piece becomes active
				engine.stat = GameEngine.Status.READY
			}
		}
		return 0
	}

	override fun onARE(engine:GameEngine):Boolean {
		if(engine.statc[0]==0) nowMission.ready(engine)

		return super.onARE(engine)
	}

	/** Renders game result screen */
	override fun renderResult(engine:GameEngine) {
		if(!netIsWatch) receiver.drawMenuFont(
			engine, 0, 0, "${BaseFont.UP_S}${BaseFont.DOWN_S} PAGE${engine.statc[1]+1}/3", COLOR.RED
		)

		if(engine.statc[1]==0) {
			val gcolor = when(engine.statistics.rollClear) {
				2 -> COLOR.ORANGE
				1 -> COLOR.GREEN
				else -> COLOR.RED
			}
			receiver.drawMenuFont(engine, 0, 1, "LIFE REMAINED", COLOR.BLUE, .8f)
			receiver.drawMenuNum(engine, 7, 1, "%2d".format(engine.lives), gcolor, 2f)

			receiver.drawMenuNum(engine, 0, 2, "%04d".format(norm), gcolor, 2f)
			receiver.drawMenuFont(engine, 6, 3, "Lines", COLOR.BLUE, .8f)

			drawResultStats(
				engine, receiver, 4, COLOR.BLUE, Statistic.LPM, Statistic.TIME, Statistic.PPS, Statistic.PIECE
			)
			drawResultRank(engine, receiver, 14, COLOR.BLUE, rankingRank)
			drawResultNetRank(engine, receiver, 16, COLOR.BLUE, netRankingRank[0])
			drawResultNetRankDaily(engine, receiver, 18, COLOR.BLUE, netRankingRank[1])
		} else if(engine.statc[1]==1||engine.statc[1]==2) {
			receiver.drawMenuFont(engine, 0, 2, "SECTION", COLOR.BLUE)

			var i = 0
			var x:Int
			while(i<10&&i<sectionTime.size-engine.statc[1]*10) {
				x = i+engine.statc[1]*10-10
				if(x>=0) if(sectionTime[x]>0) receiver.drawMenuNum(engine, 2, 3+i, sectionTime[x].toTimeStr)
				i++
			}
			if(sectionAvgTime>0) {
				receiver.drawMenuFont(engine, 0, 14, "AVERAGE", COLOR.BLUE)
				receiver.drawMenuFont(engine, 2, 15, sectionAvgTime.toTimeStr)
			}
		}

		if(netIsPB) receiver.drawMenuFont(engine, 2, 20, "NEW PB", COLOR.ORANGE)

		if(netIsNetPlay&&netReplaySendStatus==1) receiver.drawMenuFont(engine, 0, 21, "SENDING...", COLOR.PINK)
		else if(netIsNetPlay&&!netIsWatch&&netReplaySendStatus==2) receiver.drawMenuFont(engine, 1, 21, "A: RETRY", COLOR.RED)
	}

	/** Additional routine for game result screen */
	override fun onResult(engine:GameEngine):Boolean {
//		if(goalType>=Course.HELL.ordinal&&engine.statistics.rollClear>=1) owner.musMan.bgm = BGM.Result(3)
		if(!netIsWatch) {
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
		}

		return super.onResult(engine)
	}

	/** This function will be called when the replay data is going to be
	 * saved */
	override fun saveReplay(engine:GameEngine, prop:CustomProperties):Boolean {
		saveSetting(engine, prop)

		// NET: Save name
		if(netPlayerName!=null&&netPlayerName!!.isNotEmpty()) prop.setProperty(
			"${engine.playerID}.net.netPlayerName", netPlayerName
		)

		if(!owner.replayMode&&startLevel==0&&!big&&engine.ai==null) {
			updateRanking(engine.lives, norm, engine.statistics.time, goalType, engine.statistics.rollClear)

			if(rankingRank!=-1) return true
		}
		return false
	}

	/** Update the ranking
	 * @param lf Lives
	 * @param ln Lines
	 * @param time Time
	 * @param type Course type
	 * @param clear Game completed flag
	 */
	private fun updateRanking(lf:Int, ln:Int, time:Int, type:Int, clear:Int) {
		rankingRank = checkRanking(lf, ln, time, type, clear)

		if(rankingRank!=-1) {
			for(i in RANKING_MAX-1 downTo rankingRank+1) {
				rankingLives[type][i] = rankingLives[type][i-1]
				rankingLines[type][i] = rankingLines[type][i-1]
				rankingTime[type][i] = rankingTime[type][i-1]
				rankingRollClear[type][i] = rankingRollClear[type][i-1]
			}

			rankingLives[type][rankingRank] = lf
			rankingLines[type][rankingRank] = ln
			rankingTime[type][rankingRank] = time
			rankingRollClear[type][rankingRank] = clear
		}
	}

	/** This function will check the ranking and returns which place you are.
	 * (-1: Out of rank)
	 * @param lf Lives
	 * @param ln Lines
	 * @param time Time
	 * @param type Course type
	 * @param clear Game completed flag
	 * @return Place (First place is 0. -1 is Out of Rank)
	 */
	private fun checkRanking(lf:Int, ln:Int, time:Int, type:Int, clear:Int):Int {
		for(i in 0..<RANKING_MAX) if(clear>rankingRollClear[type][i]) return i
		else if(clear==rankingRollClear[type][i]&&ln>rankingLines[type][i]) return i
		else if(clear==rankingRollClear[type][i]&&ln==rankingLines[type][i]&&lf>rankingLives[type][i]) return i
		else if(clear==rankingRollClear[type][i]&&ln==rankingLines[type][i]&&lf==rankingLives[type][i]&&time<rankingTime[type][i]) return i

		return -1
	}

	/** NET: Send various in-game stats of [engine] */
	override fun netSendStats(engine:GameEngine) {
		val bg = if(engine.owner.bgMan.fadeSW) engine.owner.bgMan.nextBg else engine.owner.bgMan.bg
		val msg = "game\tstats\t"+
			"${engine.statistics.lines}\t${engine.statistics.totalPieceLocked}\t"+
			"${engine.statistics.time}\t${engine.statistics.lpm}\t"+
			"${engine.statistics.pps}\t$goalType\t"+
			"${engine.gameActive}\t${engine.timerActive}\t"+
			"${engine.statistics.level}\t$levelTimer\t$levelTimerMax\t"+
			"$rollTime${"\t$norm\t$bg\t${engine.meterValue}\t"+engine.meterColor}\t"+
			"${engine.heboHiddenEnable}\t${engine.heboHiddenTimerNow}\t${engine.heboHiddenTimerMax}\t"+
			"${engine.heboHiddenYNow}\t${engine.heboHiddenYLimit}\n${engine.lives}\n"
		netLobby?.netPlayerClient?.send(msg)
	}

	/** NET: Parse Received [message] as in-game stats of [engine] */
	override fun netRecvStats(engine:GameEngine, message:List<String>) {
		engine.statistics.lines = message[4].toInt()
		engine.statistics.totalPieceLocked = message[5].toInt()
		engine.statistics.time = message[6].toInt()
		//engine.statistics.lpm = message[7].toFloat()
		//engine.statistics.pps = message[8].toFloat()
		goalType = message[9].toInt()
		engine.gameActive = message[10].toBoolean()
		engine.timerActive = message[11].toBoolean()
		engine.statistics.level = message[12].toInt()
		levelTimer = message[13].toInt()
		levelTimerMax = message[14].toInt()
		rollTime = message[15].toInt()
		nowMission.prog = message[16].toInt()
		engine.owner.bgMan.bg = message[17].toInt()
		engine.meterValue = message[18].toFloat()
		engine.meterColor = message[19].toInt()
		engine.heboHiddenEnable = message[20].toBoolean()
		engine.heboHiddenTimerNow = message[21].toInt()
		engine.heboHiddenTimerMax = message[21].toInt()
		engine.heboHiddenYNow = message[22].toInt()
		engine.heboHiddenYLimit = message[23].toInt()
		engine.lives = message[24].toInt()
	}

	/** NET: Send end-of-game stats
	 * @param engine GameEngine
	 */
	override fun netSendEndGameStats(engine:GameEngine) {
		val subMsg =
			"NORM;$norm\tLEVEL;${engine.statistics.level+engine.statistics.levelDispAdd}\t"+
				"TIME;${engine.statistics.time.toTimeStr}\tPIECE;${engine.statistics.totalPieceLocked}\t"+
				"LINE/MIN;${engine.statistics.lpm}\tPIECE/SEC;${engine.statistics.pps}\t"+
				"SECTION AVERAGE;${sectionAvgTime.toTimeStr}\t"+
				sectionTime.filter {it>0}.mapIndexed {i, it -> "SECTION ${i+1};"+it.toTimeStr}.joinToString("\t")

		val msg = "gstat1p\t${NetUtil.urlEncode(subMsg)}\n"
		netLobby?.netPlayerClient?.send(msg)
	}

	/** NET: Send game options to all spectators
	 * @param engine GameEngine
	 */
	override fun netSendOptions(engine:GameEngine) {
		val msg = "game\toption\t$goalType\t$startLevel\t$showST\t$big\n"
		netLobby?.netPlayerClient?.send(msg)
	}

	/** NET: Receive game options */
	override fun netRecvOptions(engine:GameEngine, message:List<String>) {
		goalType = message[4].toInt()
		startLevel = message[5].toInt()
		showST = message[6].toBoolean()
		big = message[7].toBoolean()
	}

	/** NET: Get goal type */
	override val netGetGoalType get() = goalType

	/** NET: It returns true when the current settings don't prevent
	 * leaderboard screen from showing. */
	override fun netIsNetRankingViewOK(engine:GameEngine):Boolean = startLevel==0&&!big&&engine.ai==null

	companion object {
		/** Current version of this mode */
		private const val CURRENT_VERSION = 1
		private val courses = Course.entries
		/** Game types */
		sealed class Mission(val lv:Int = 0,
			/**この問題のミッションクリアまでのノルマカウントです。**/
			val norm:Int = 5,
			/**この問題の制限時間です。ミッションスタート時から減っていき、これが0になるとゲームオーバーです。
			（耐久では0になった状態で操作中のブロックを設置するとクリアになる）*/
			val time:Int = 90, val misc:Int = 0) {
			/** 合計して[norm]ライン数を消すとクリアとなります。*/
			class LevelStar(lv:Int, norm:Int, time:Int):Mission(lv, norm, time)
			/**高速落下状態で、合計して[norm]ラインを消すとクリアです。*/
			class SpeedStar(lv:Int, norm:Int, time:Int):Mission(lv, norm, time)
			/**速度はSpeed Starと同じです。時間切れまで耐えてください。
			　無限回転を行っているとタイマーが減らなくなります。*/
			class Enduro(lv:Int, time:Int):Mission(lv, time, time)

			/** 1ライン消しを[norm]回数行うとクリアです。*/
			class Singles(lv:Int, norm:Int, time:Int, misc:Int):Mission(lv, norm, time, misc)
			/** 2ライン消しを[norm]回数行うとクリアです。
			 * @param misc 0:2ライン消しを1回行うとNORMが1つ上昇します。
			 *   1:2ライン消し以上を1回行うとNORMが1つ上昇します。
			 *   2:2ライン消しを連続で行った回数がNORMになります。それ以外では０に戻ります。
			 *   2:2ライン消し以上を連続で行った回数がNORMになります。それ未満では０に戻ります。*/
			class Doubles(lv:Int, norm:Int, time:Int, misc:Int):Mission(lv, norm, time, misc)
			/** 3ライン消しを[norm]回数行うとクリアです。
			 * @param misc 0:3ライン消しを1回行うとNORMが1つ上昇します。
			 *  1:3ライン消し以上を1回行うとNORMが1つ上昇します。
			 *  2:3ライン消しを連続で行った回数がNORMになります。それ以外では０に戻ります。
			 *  3:3ライン消し以上を連続で行った回数がNORMになります。それ未満では０に戻ります。*/
			class Triples(lv:Int, norm:Int, time:Int, misc:Int):Mission(lv, norm, time, misc)
			/** 4ライン消しを[norm]回数行うとクリアです。*/
			class Quads(lv:Int, norm:Int, time:Int, misc:Int):Mission(lv, norm, time, misc)

			/**1ライン消し、2ライン消し、3ライン消し、4ライン消しをそれぞれ1回以上するとクリアです。*/
			class Cycle(lv:Int, time:Int):Mission(lv, 4, time) {
				var tmp = 0
			}

			/**[norm]回以上連続でラインを消すコンボを達成すればクリアです。
			 * @param multiLine trueにすると1ライン消しがコンボにカウントされなくなります。*/
			class Combo(lv:Int, norm:Int, time:Int, multiLine:Boolean = false):Mission(lv, norm, time, multiLine.toInt())
			/** [norm]回以上連続で4ライン消しやTwistを行えばクリアです。
			途中でそれら以外を行うと1からやり直しになります。*/
			class B2BChain(lv:Int, norm:Int, time:Int, misc:Int):Mission(lv, norm, time, misc)
			/** 中抜きで複数ライン消しを[norm]回数行うとクリアです。*/
			class Split(lv:Int, norm:Int, time:Int, misc:Int):Mission(lv, norm, time, misc)
			/** T-SPINでライン消しを[norm]回数行うとクリアです。 消した列数は関係ありません。*/
			class Twister(lv:Int, norm:Int, time:Int, misc:Int):Mission(lv, norm, time, misc)
			/*

・X-RAY
・カラー (COLOR)
・ロールロール (ROLL ROLL)
・ミラー (MIRROR)
　常に同名のオジャマがかかっているレベルスターです。

・回転不可 (ROTATE LOCK)
　ブロックを回転できない＆向きがランダムな状態でレベルスターを行います。

・NEXT不可視 (HIDE NEXT)
　NEXTブロックが全く見えない状態でレベルスターを行います。

・DEVIL 800
　DEVILモードのレベル800台と同じ速度でレベルスターを行います。
　最下段コピーせり上がりも発生します
　□ OPTIONSの値
　　 OPT: せり上がる間隔（0だと20に設定される）

・DEVIL 1200
　DEVILモードのレベル1200台と同じ速度でレベルスターを行います。
　ブロックも[ ]に変化します。
　□ OPTIONSの値
　　 OPT: 0以外にすると　激 ム ズ　に

・GARBAGE
　レベルスターです。が、
　最初のブロックを設置すると、大量のせり上がりが発生します。
　□ OPTIONSの値
　　 OPT: せり上がるライン数（18が最大）

・オールドスタイル (OLD STYLE)
　ブロック・フィールド枠・スピードが専用の物になります。
　ブロックは接着後即次出現で、
　NEXT表示1つまで、壁蹴り無し、HOLD不可、IRS不可となります
　後はレベルスターと同じです。

・耐久 (ENDURANCE)

　ノルマは必ず1以上に設定してください。

・上下左右逆転(LRUD REV)
　操作の上と下・左と右が逆転した状態でレベルスターを行います。

・ブラインド(BLIND)
　積んであるブロックが枠しか見えない状態でレベルスターを行います。

・全消し(ALL CLEAR)
　全消しを指定回数行うとクリアです。
　難易度の都合上、（通常は）BIGがかかります。
　□ OPTIONSの値
　　 OPT: 0以外にすると…？

・OOBAKA
　（・w・）

・ブロックオーダー(BLOCK ORDER)
　指定されたブロックでラインを消さないとノルマが
　上昇しないレベルスターです。
　□ OPTIONSの値
     HOLD USE: ブロックをHOLDに入れる必要があります。
　　 OPT3: ブロックを指定します。(0～8)

・シングルオーダー(SINGLE ORDER)
　指定されたブロックで1ライン消しを行わないとノルマが
　上昇しないシングルです。
　□ OPTIONSの値
     HOLD USE: ブロックをHOLDに入れる必要があります。
　　 OPT: ブロックを指定します。(0～8)

・ダブルオーダー(DOUBLE ORDER)
　指定されたブロックで2ライン消しを行わないとノルマが
　上昇しないダブルです。
　□ OPTIONSの値
     HOLD USE: ブロックをHOLDに入れる必要があります。
　　 OPT: ブロックを指定します。(0～8)

・裏トリプルオーダー(RE TRIPLE ORDER)
　指定されたブロック"以外"で3ライン消しを行わないとノルマが
　上昇しないトリプルです。
　□ OPTIONSの値
     HOLD USE: ブロックをHOLDに入れる必要があります。
　　 OPT: カウントしないブロックを指定します。(0～8)

・トリプルオーダー(TRIPLE ORDER)
　指定されたブロックで3ライン消しを行わないとノルマが
　上昇しないトリプルです。
　□ OPTIONSの値
     HOLD USE: ブロックをHOLDに入れる必要があります。
　　 OPT: カウントしないブロックを指定します。(0～6)

・イレイサーヘボリス(TRIPLE ORDER)
　指定されたラインで4ライン消しを行わないとノルマが
　上昇しません。
　□ OPTIONSの値
　　 MIN: 出現する線の位置の「上から数えた場合の」上限(0～21まで)
　　 MAX: 出現する線の位置の「上から数えた場合の」下限(0～21まで)
　　 OPT: 同時に出現する線の数(実際はこれに+1されます。0～3まで)

・スクウェア(SQUARE)
　4x4の正方形を指定個数作るとクリアになります。
　正方形は
　１．破片を含まない
　２．ブロックが4x4の外側に繋がっていない
　と完成します。

・ゴールドスクウェア(GOLD SQUARE)
　一種類のブロックだけを使って4x4の正方形を指定個数作ればクリアです。


　　LEVELをPLUS #にすると速度がアナザーになり、
　　ミッション名に「+」が付きます。

			・ターゲット (TARGET)
			　フィールド上に配置されたプラチナブロックを全て消すとNORMが1つ上昇し、別のステージが始まります。
			　せり上がり以外のギミックは発動しません。
			　□ OPTIONSの値
			　　 MIN: 出現するステージ番号の下限(0～26=Ti 27～44=EH 45～67=ACE)
			　　 MAX: 出現するステージ番号の上限(同上)
			　　 RANDTGT: 1以上にするとプラチナブロックを数値分ランダムで配置します。0(OFF)だと初期配置のままです。
			　　　99(FULL)にすると…？
			 */
			/** ブロックが大きい状態で合計して[norm]ライン数を消せばクリアです。
			ただし消したライン数 = [prog]は、（見た目での消したライン数÷２）だけカウントされます。*/
			class BIG(lv:Int, norm:Int, time:Int):Mission(lv, norm, time)

			/** 線と同じ高さでラインを合計[norm]本を消すとクリアです。
			全ての線を消してもまだ[norm]が残っている場合は全ての線が復活します。*/
			class Ladder(lv:Int, norm:Int, time:Int,
				/**Range of Height spawning target lines*/
				private val heightRange:IntRange,
				/** Count of spawning target lines*/
				private val multi:Int):Mission(lv, norm, time) {
				val target = mutableSetOf<Int>()
				fun reset(random:Random) {
					while(target.size<minOf(norm-prog, multi)) {
						target.add(random.nextInt(heightRange.last-heightRange.first)+heightRange.first)
					}
				}
			}

			class Target(lv:Int, norm:Int, time:Int):Mission(lv, norm, time)
			class Dark(lv:Int, norm:Int, time:Int, misc:Int):Mission(lv, norm, time, misc)
			class Shutter(lv:Int, norm:Int, time:Int):Mission(lv, norm, time)
			/**ブロックが[ ]に変化します。*/
			class Monochrome(lv:Int, norm:Int, time:Int):Mission(lv, norm, time)

			/** Mission Ready */
			val ready:(GameEngine)->Unit by lazy {
				when(this) {
					is BIG -> {e ->
						e.big = true
						e.bigMove = true
						e.bigHalf = true
					}
					is Monochrome -> {e -> e.bone = true}
					is Combo -> {e -> e.comboType = 1+misc}
					is B2BChain -> {e ->
						e.b2bEnable = true
						e.splitB2B = true
					}
					is Twister -> {e ->
						e.twistEnable = true
						e.twistAllowKick = true
						e.useAllSpinBonus = true
					}
					is Ladder -> {e -> if(target.isEmpty()) reset(e.random)}
					is Dark -> {e ->
						if(misc>10) {
							e.blockHidden = maxOf(2, e.ruleOpt.lockFlash)
							e.blockHiddenAnim = false
							e.blockShowOutlineOnly = true
						} else e.blockHidden = when(misc) {
							1 -> 180
							2 -> 150
							3 -> 120
							4 -> 90
							5 -> 60
							6 -> 45
							7 -> 30
							8 -> 15
							9 -> 8
							10 -> 3
							else -> 200
						}
					}
					is Shutter -> {e ->
						e.heboHiddenEnable = true
					}
					else -> {_ ->}
				}
			}
			/** Mission Unload */
			val unload:(GameEngine)->Unit by lazy {
				when(this) {
					is BIG -> {e ->
						e.big = false
						e.bigMove = false
						e.bigHalf = false
					}
					is Combo -> {e -> e.comboType = GameEngine.COMBO_TYPE_DISABLE}
					is B2BChain -> {e ->
						e.b2bEnable = false
						e.splitB2B = false
					}
					is Twister -> {e ->
						e.twistEnable = false
						e.twistAllowKick = false
						e.useAllSpinBonus = false
					}
					is Monochrome -> {e -> e.bone = false}
					is Dark -> {e -> e.blockHidden = -1}
					is Shutter -> {e ->
						e.heboHiddenEnable = false
						e.heboHiddenYLimit = 0
					}
					else -> {_ ->}
				}
			}
			/** Mission Progress */
			var prog = 0
			/** Check if the mission progress has been updated */
			fun checkProgress(engine:GameEngine, ev:ScoreEvent):Boolean {
				val prev = prog
				val li = ev.lines
				when(this) {
					is Doubles -> if(if(misc%2==1) li>=2 else li==2) prog++ else if(misc>=2) prog = 0
					is Triples -> if(if(misc%2==1) li>=3 else li==3) prog++ else if(misc>=2) prog = 0
					is Quads -> if(li==4) prog++
					is Split -> if(ev.split&&(misc !in 2..3||li==misc)) if(misc<=0) prog += li else prog++
					is Twister -> if(ev.twist&&(misc !in 1..3||li==misc)) if(misc<=0) prog += li else prog++
					is Combo -> prog = ev.combo
					is B2BChain -> prog = ev.b2b
					is Cycle -> {
						tmp = tmp or (1 shl li)
						prog = tmp and 1+(tmp and 2>0).toInt()+(tmp and 4>0).toInt()+(tmp and 8>0).toInt()
					}
					is Shutter -> {
						prog += li
						// Decrease Pressure Hidden
						if(engine.heboHiddenEnable&&li>0) {
							engine.heboHiddenTimerNow = 0
							engine.heboHiddenYNow -= li
							if(engine.heboHiddenYNow<0) engine.heboHiddenYNow = 0
						}
					}
					is Ladder -> {
						target.forEach {
							if(engine.field.lastLinesHeight.contains(it)) {
								prog++
								target.remove(it)
							}
						}
//						if(prog<norm&&target.isEmpty()) reset(engine.random)
					}
					else -> prog += li

				}
				return prog>prev
			}

			/** Level data */
			val speeds by lazy {
				when(this) {
					is SpeedStar -> LevelData(
						listOf(25, 24, 23, 22, 21, 20, 19, 18, 17, 16, 15, 14, 13, 12, 11, 10), // ARE
						listOf(30, 28, 26, 24, 22, 20, 18, 16, 14, 12, 10, +9, +8, +7, +6, +5), // Line delay
						listOf(30, 30, 29, 29, 28, 28, 27, 27, 26, 25, 25, 24, 23, 22, 21, 20), // Lock delay
						listOf(10, 10, 10, 10, 10, 10, +9, +9, +9, +8, +8, +8, +7, +7, +7, +7) // DAS
					)
					else -> LevelData(
						listOf(
							+4, 12, 48, 72, 96, 128, 256, 384, 512, 768, 1024, 1280, -1
						), listOf(256),
						listOf(
							25, 25, 25, 25, 25, +25, +25, +25, +25, +25, +25, +25, 25,
							24, 23, 22, 21, 20, 19, 18, 17, 16, 15, 14, 13, 12, 11, 10
						), // ARE
						listOf(
							30, 30, 30, 30, 30, +30, +30, +30, +30, +30, +30, +30, 30,
							28, 26, 24, 22, 20, 18, 16, 14, 12, 10, +9, +8, +7, +6, +5
						), // Line delay
						listOf(
							30, 30, 30, 30, 30, +30, +30, +30, +30, +30, +30, +30, 30,
							30, 29, 29, 28, 28, 27, 27, 26, 25, 25, 24, 23, 22, 21, 20
						), // Lock delay
						listOf(
							10, 10, 10, 10, 10, +10, +10, +10, +10, +10, +10, +10, 10,
							10, 10, 10, 10, 10, +9, +9, +9, +8, +8, +8, +7, +7, +7, +7
						) // DAS
					)
				}
			}
			/** Game type names (short) */
			val showName by lazy {
				when(this) {
					is LevelStar -> "Level Star"
					is SpeedStar -> "Speed Star"
					is Enduro -> "Enduro"
					is Singles -> "Singles"
					is Doubles -> when(misc) {
						1 -> "Doubles+"
						2 -> "Doubles Chain"
						3 -> "Doubles+ Chain"
						else -> "Doubles"
					}
					is Triples -> when(misc) {
						1 -> "Triples+"
						2 -> "Triples Chain"
						3 -> "Triples+ Chain"
						else -> "Triples"
					}
					is Quads -> "Quads"
					is Split -> when(misc) {
						1 -> "Splits Count"
						2 -> "Split Double"
						3 -> "1.2. Triples"
						else -> "Splits Total"
					}
					is Twister -> when(misc) {
						0 -> "Twister Count"
						1 -> "Singles"
						2 -> "Doubles"
						3 -> "Triples"
						else -> "Twister Total"
					}
					is Cycle -> "Line Cycles"
					is Combo -> "Combo"
					is B2BChain -> "B2B Chain"
					is BIG -> "BIG"
					is Ladder -> "Ladder Liner"
					is Target -> "Target Dig"
					is Dark -> "Hiding Dark"
					is Shutter -> "Rising Shutter"
					is Monochrome -> "Monochrome"
				}
			}
		}

		enum class Course {
			S1;

			/*
* ◆あらかじめ入っているミッションセットの解説

▼BIGロード
　BIGが何度も出てくるロードです
　EXミッション:BIG

▼トリッキーロード
　ターゲットやイレイサーが多いロードです
　5問目の追加条件を達成するのは一苦労？
　EXミッション:イレイサー

▼グランドロード
　ここからミッションの総数が多くなってきます
　追加条件のあるミッションが2つ出てくるので注意。
　EXミッション:レベルスター

▼スターロード
　グランドロードに似ていますが、終盤に
　速度が20Gな「ハイスピード2」が出現します。
　EXミッション:ハイスピード2

▼アナザーロード
　ミッションの総数が多く、
　追加条件のあるミッションもかなりあります。
　集中力をどれだけ持続できるかが勝負の分かれ目です。
　EXミッション:アナザー

▼DSロード
　ミッションは多いですが、1つ1つのミッションの
　ノルマは少なめで、サクッと楽しめます。
　しかし、終盤は制限時間が極端に短くなっています。
　EXミッション:耐久

▼デビルロード
　アナザーロードも楽々クリアーできるプレイヤーへの挑戦状。
　ほとんどのミッションの速度がアナザーと同じという、
　凶悪極まりないロードです。
　EXミッション:DEVIL 1200(REAL)　…?

▼トライアル　一段～十段
　まずはこれらのミッションで肩慣らしをするといいでしょう。

▼トライアル　HM（ヘボマニア）
　アナザーが3つもある難度の高いトライアルです。

▼トライアル　ネ申（GOD）
　(・w・)

▼アマチュア・プロ・ブロンズ・シルバー・ゴールド
　制限時間はありませんが、ミッション間のライン消去は一切発生しません。
　常に後のミッションの事を考えてプレイする必要があります。

▼プラチナ
　時間無制限ミッションセットの中で最高の難易度を誇ります。

*/
			val missions:List<Mission> by lazy {
				when(this) {
					S1 -> listOf(Mission.LevelStar(1, 6, 70))
					else -> listOf(Mission.LevelStar(1, 6, 70))
				}
			}
			val goalLevel by lazy { missions.size}

			/** BGM table */
			val bgmList by lazy {
				when(this) {
					S1 -> listOf(BGM.Puzzle(0), BGM.GrandA(0), BGM.GrandM(0))
				}
			}
			/** BGM change lines table */
			val bgmChange by lazy {
				when(this) {
					S1 -> listOf(5, 10)
					else -> listOf()
				}
			}

			val bgOffset by lazy {
				when(this) {
					S1 -> 0
				}
			}

			/** Game type names (short) */
			val showName by lazy {
				when(this) {
					S1 -> "Easy"

				}
			}
		}

		/** Number of Course & ranking typ s */
		private val COURSE_MAX = courses.size

		/** HELL-X fade table */
		private val tableHellXFade =
			listOf(
				600, 550, 500, 450, 400, 350, 300, 270, 240, 210,
				190, 170, 160, 150, 140, 130, 125, 120, 115, 110,
				100, 90, 80, 70, 60, 58, 56, 54, 52, 50
			)

		/** Ending time limit */
		private const val ROLLTIMELIMIT = 3238

	}
}

/*
◆ミッションエディタ詳細

■TYPE
※特に注意書きがない場合は、OPTIONSの値は全て無視されます。

■ENDING
この問題をクリアすると何が起こるかを指定します。
　NO    : 次の問題へ
　END   : エンディング
　EXTRA : この問題の最後を2ライン消しでクリアすると次の問題へ、2ライン消し以外の場合は足切りエンディング
　EXTRA+: 基本はEXTRAと同じですが、ライン消去の演出が変わります。
　STAFF ROLL :クリアした問題の速度、状態などはそのままで、スタッフロールに突入。
　　　　　　　スタッフロールをクリアすればエンディング。
　M ROLL: 基本はSTAFF ROLLと同じですが、設置したブロックが見えなくなります。
　DEVIL+M ROLL : ひ　ど　い

■ERASE LINE/RISEE LINE/RISEH LINE
この問題をクリアしたときに上から消去(上昇)するライン数を指定します。
0にすると消去されません。21にするとすべて消去されます。
RISEEはせり上がるラインの穴がそろっています。
RISEHはせり上がるラインの穴がばらばらです

■OPTIONS
追加情報です。ターゲット、イレイサー、GARBAGEで使われます。

■BGM
BGMを指定できます。番号はSOUND TESTと同じです。
FADEにするとBGMがフェードアウトします。
*/
