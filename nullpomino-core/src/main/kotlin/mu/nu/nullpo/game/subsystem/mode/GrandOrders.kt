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

import kotlinx.serialization.Serializable
import mu.nu.nullpo.game.component.BGM
import mu.nu.nullpo.game.component.Controller
import mu.nu.nullpo.game.component.LevelData
import mu.nu.nullpo.game.component.Piece.Shape
import mu.nu.nullpo.game.event.EventReceiver.COLOR
import mu.nu.nullpo.game.event.Leaderboard
import mu.nu.nullpo.game.event.Rankable
import mu.nu.nullpo.game.event.ScoreEvent
import mu.nu.nullpo.game.net.NetUtil
import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.game.subsystem.mode.GrandOrders.Companion.Mission.*
import mu.nu.nullpo.game.subsystem.mode.menu.*
import mu.nu.nullpo.gui.common.BaseFont.Companion.CURSOR
import mu.nu.nullpo.gui.common.BaseFont.Companion.DOWN_S
import mu.nu.nullpo.gui.common.BaseFont.Companion.UP_S
import mu.nu.nullpo.gui.common.BaseFont.FONT.*
import mu.nu.nullpo.util.CustomProperties
import mu.nu.nullpo.util.GeneralUtil.toInt
import mu.nu.nullpo.util.GeneralUtil.toTimeStr
import kotlin.math.absoluteValue
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
	private val nowMission get() = nowCourse.missions.getOrNull(missionPos)

	/** Current Mission progress (for level up) */
	private var prog
		get() = nowMission?.prog?:0
		set(value) {
			nowMission?.prog = value
		}

	/** Lines for Next level */
	private val norm get() = nowMission?.norm?:0

	/** Course記録表示中ならtrue */
	private var isShowBest = false

	private val itemLevel =
		LevelMenuItem("startlevel", "LEVEL", COLOR.RED, 0, 0..(courses.maxOfOrNull {it.goalLevel}?:0), true, true)
	/** Selected starting level */
	private var startLevel:Int by DelegateMenuItem(itemLevel)

	private val itemST = BooleanMenuItem("showsectiontime", "SHOW STIME", COLOR.BLUE, true)
	/** Show section time */
	private var showST:Boolean by DelegateMenuItem(itemST)

	override val menu = MenuList("missioncourse", itemMode, itemLevel, itemST)
	/** Version of this mode */
	private var version = 0

	/** Your place on leaderboard (-1: out of rank) */
	private var rankingRank = 0

	/** Ranking records */
	override val ranking =
		List(COURSE_MAX) {Leaderboard(rankingMax, kotlinx.serialization.serializer<List<Rankable.TimeRow>>())}
	/** Section Time記録 (Lifeが減った場合は記録されない) */
	private val bestSectionTime = List(COURSE_MAX) {MutableList(courses[it].goalLevel) {DEFAULT_SECTION_TIME}}
	override val propRank
		get() = rankMapOf(
			bestSectionTime.mapIndexed {c, it -> "$c.section.time" to it}
		)
	/** Returns the name of this mode */
	override val name = "Grand Orders"
	override val gameIntensity:Int
		get() = 2/* when(nowCourse) {
			Mission.HARD, Mission.CHALLENGE -> 1
			Mission.HARDEST, Mission.SUPER, Mission.SURVIVAL -> 2
			Mission.XTREME, Mission.HELL, Mission.Dark, Mission.VOID -> 3
			else -> 0
		}*/

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
		isShowBest = false
		showST = true

		rankingRank = -1
		bestSectionTime.forEachIndexed {x, it ->
			for(i in it.indices) bestSectionTime[x][i] = DEFAULT_SECTION_TIME
		}

		engine.twistEnable = true
		engine.b2bEnable = true
		engine.splitB2B = true
		engine.comboType = GameEngine.COMBO_TYPE_DISABLE
		engine.frameSkin = DEFAULT_SKIN
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
	override fun setSpeed(engine:GameEngine) {
		val nowMission = nowCourse.missions.getOrNull(engine.statistics.level)?:return
		engine.speed.replace(nowMission.lv.let {(it<0).toInt().let {m -> speeds[m][it.absoluteValue-m]}})
		// Show outline only
		engine.blockShowOutlineOnly = nowMission is Dark
		// Bone blocks
		engine.bone = nowMission is Monochrome

		// for test
		/* engine.speed.are = 25; engine.speed.areLine = 25;
 engine.speed.lineDelay = 10; engine.speed.lockDelay = 30;
 engine.speed.das = 12; levelTimerMax = levelTimer = 3600 * 3; */

		levelTimer = nowMission.time*60
		levelTimerMax = levelTimer
		// Blocks fade for HELL-X
		engine.blockHidden = if(nowMission is Dark) nowMission.misc else -1


		lastLineTime = levelTimer
	}

	/** Set Pressure Hidden params
	 * @param engine GameEngine
	 */
	private fun setHeboHidden(engine:GameEngine) {
		if(engine.heboHiddenEnable&&nowMission is Shutter) when((nowMission as Shutter).misc) {
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
	override fun onSetting(engine:GameEngine):Boolean {
		// Menu
		if(!engine.owner.replayMode) {
			// section time display切替
			if(engine.ctrl.isPush(Controller.BUTTON_F)) {
				engine.playSE("change")
				isShowBest = !isShowBest
			}
			return if(engine.statc[1]==1) {
				// Cancel
				if(engine.ctrl.isPush(Controller.BUTTON_B)&&!netIsNetPlay) {
					engine.playSE("cancel")
					engine.statc[1] = 0
					true
				} else super.onSetting(engine)
			} else {
				if(engine.ctrl.isMenuRepeatKey(Controller.BUTTON_UP)) {
					engine.playSE("change")
					goalType--
					if(goalType<0) goalType = COURSE_MAX-1
					onSettingChanged(engine)
				}
				if(engine.ctrl.isMenuRepeatKey(Controller.BUTTON_DOWN)) {
					engine.playSE("change")
					goalType++
					if(goalType>=COURSE_MAX) goalType = 0
					onSettingChanged(engine)
				}
				// Cancel
				if(engine.ctrl.isPush(Controller.BUTTON_B)&&!netIsNetPlay) engine.quitFlag = true

				// 決定
				if(menuTime<5) menuTime++ else if(engine.ctrl.isPush(Controller.BUTTON_A)) {
					engine.playSE("decide")
					engine.statc[1] = 1
					owner.musMan.bgm = BGM.Menu(4+gameIntensity)
					menuTime = 0
					menuCursor = 1
				}
				true
			}
		} else return super.onSetting(engine)
	}

	override fun renderSetting(engine:GameEngine) {
		if(!engine.owner.replayMode&&engine.statc[1]<1) {
			receiver.drawMenu(engine, 0, 0, "${UP_S}${DOWN_S} Courses", BASE, COLOR.BLUE)

			val mH = engine.field.height-1
			val ofs = goalType*maxOf(0, COURSE_MAX-mH)/COURSE_MAX
			courses.drop(ofs).take(mH).forEachIndexed {i, it ->
				receiver.drawMenu(engine, 1, 1+i, it.name, BASE, if(it==nowCourse) COLOR.RAINBOW else COLOR.WHITE)
				if(it==nowCourse) receiver.drawMenu(engine, 0, 1+i, CURSOR, BASE, COLOR.RAINBOW)
			}
		} else super.renderSetting(engine)
	}

	override fun onSettingChanged(engine:GameEngine) {
		if(startLevel>nowCourse.goalLevel-1) startLevel =
			if(menuCursor!=menu.items.indexOf(itemLevel)) 0 else nowCourse.goalLevel-1
		engine.owner.bgMan.bg = startLevel
		engine.statistics.level = startLevel
		missionPos = engine.statistics.level
		setSpeed(engine)
		super.onSettingChanged(engine)
	}

	/** Ready screen */
	override fun onReady(engine:GameEngine):Boolean {
		if(engine.statc[0]==0) {
			setSpeed(engine)
			bgmLv = maxOf(0, nowCourse.bgmChange.indexOfLast {it<=startLevel}.let {if(it<0) nowCourse.bgmChange.size-1 else it})
			nowMission?.ready(engine)
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

		receiver.drawScore(engine, 0, -1, name, BASE, COLOR.PURPLE)
		receiver.drawScore(engine, 0, 0, "Mission", BASE, COLOR.PURPLE, .75f)
		receiver.drawScore(engine, 0, 1, "${nowCourse.showName} COURSE", BASE, COLOR.PURPLE)
		//receiver.drawScore(engine, playerID, -1, -4*2, "DECORATION", scale = .5f);
		//receiver.drawScoreBadges(engine, playerID,0,-3,100,decoration);
		//receiver.drawScoreBadges(engine, playerID,5,-4,100,decTemp);
		if(engine.stat==GameEngine.Status.SETTING||engine.stat==GameEngine.Status.RESULT&&!owner.replayMode) {
			if(!owner.replayMode&&netIsNetRankingViewOK(engine)&&!netIsWatch&&isShowBest) {
				receiver.drawScore(engine, 8, 3, "Time", BASE, COLOR.BLUE)
				ranking[goalType].forEachIndexed {i, (st) ->
					val clear = st.rollClear>0
					val gColor = when(st.rollClear) {
						1 -> COLOR.GREEN
						2 -> COLOR.ORANGE
						else -> COLOR.WHITE
					}
					receiver.drawScore(engine, 0, 4+i, "%2d".format(i+1), GRADE, if(i==rankingRank) COLOR.RED else COLOR.YELLOW)
					receiver.drawScore(engine, 2, 4+i, "%3d".format(if(clear) st.lives else st.lines), NUM, gColor)
					receiver.drawScore(engine, 4.5f, 4+i, if(clear) "LIVES\nREMAINED" else "LEVELS\nDONE", NANO, gColor, .5f)
					receiver.drawScore(engine, 8, 4+i, st.time.toTimeStr, NUM, gColor)
				}
			} else {
				nowCourse.missions.forEachIndexed {i, it ->
					val y = i+(engine.stat==GameEngine.Status.SETTING&&engine.statc[1]==1&&i>startLevel).toInt()
					val a = if(i<startLevel) .5f else 1f
					receiver.drawScore(engine, 0, 2+y, "%2d".format(i+1), GRADE,
						if(i==startLevel) COLOR.RED else COLOR.YELLOW, 1f, a)
					receiver.drawScore(engine, 2.5f, 2+y, it.showName, BASE, i==startLevel, 1f, a)
				}
				if(!owner.replayMode) {
					receiver.drawScore(engine, 0, 17, "F:VIEW SECTION TIME", BASE, COLOR.GREEN)
				}
			}
		} else {
			receiver.drawScore(engine, 3, 2, "Missions", BASE, COLOR.BLUE)
			receiver.drawScore(engine, 0, 2, "%02d".format(engine.statistics.level+1), NUM, 2f)
			receiver.drawScore(engine, 3, 3, "/%3d".format(nowCourse.goalLevel), NUM)

			receiver.drawScore(engine, 0, 5, nowMission?.showName?:"", BASE, COLOR.PURPLE)
			receiver.drawScoreSpeed(engine, 0, 7, engine.speed.rank, 6f)
			receiver.drawScore(engine, 0, 8, "%3d/%3d".format(prog, norm), NUM)

			receiver.drawScore(engine, 0, 10, "TIME LIMIT", BASE, COLOR.BLUE)
			receiver.drawScore(engine, 0, 11, levelTimer.toTimeStr, NUM, levelTimer in 1..<600&&levelTimer%4==0, 2f)

			receiver.drawScore(engine, 0, 13, "TOTAL TIME", BASE, COLOR.BLUE)
			receiver.drawScore(engine, 0, 14, engine.statistics.time.toTimeStr, NUM_T)

			// Remaining ending time
			if(engine.gameActive&&engine.ending==2&&engine.staffrollEnable) {
				var time = ROLLTIMELIMIT-rollTime
				if(time<0) time = 0
				receiver.drawScore(engine, 0, 17, "ROLL TIME", BASE, COLOR.BLUE)
				receiver.drawScore(engine, 0, 18, time.toTimeStr, NUM, time>0&&time<10*60, 2f)
			}

			// Section time
			if(showST&&sectionTime.isNotEmpty()&&!netIsWatch) {
				val x = if(receiver.nextDisplayType==2) 25 else 12
				val y = if(receiver.nextDisplayType==2) 4 else 2

				receiver.drawScore(engine, x, y, "SECTION TIME", BASE, COLOR.BLUE)

				val l = maxOf(0, engine.statistics.level-20)
				var i = l
				while(i<sectionTime.size) {
					if(sectionTime[i]>0) {
						val strSeparator = if(i==engine.statistics.level&&engine.ending==0) "+" else "-"
						val strSectionTime = "%2d%s%s".format(i+1, strSeparator, sectionTime[i].toTimeStr)
						receiver.drawScore(engine, x+1, y+1+i-l, strSectionTime, NUM)
					}
					i++
				}
				receiver.drawScore(engine, x, 13, "AVERAGE", BASE, COLOR.BLUE)
				receiver.drawScore(engine, x, 14, (engine.statistics.time/(sectionsDone+1)).toTimeStr, NUM_T)
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
			if(nowCourse.missions[engine.statistics.level] is Dark) {
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
		if(engine.timerActive&&engine.ending==0) {
			if(levelTimer>0) {
				levelTimer--
				if(nowMission is Enduro) prog = levelTimer/60

				if(nowMission is Enduro&&levelTimer<=0) engine.playSE("levelstop")
				else if(levelTimer<=600&&levelTimer%60==0) engine.playSE("countdown")
			} else if(!netIsWatch&&nowMission !is Enduro) {
				engine.resetStatc()
				engine.lives = -1
				engine.stat = GameEngine.Status.GAMEOVER
				engine.playSE("timeover")
			}

			// Update meter
			if(levelTimerMax!=0) {
				if(nowMission is Enduro) {
					engine.meterValue = (levelTimerMax-levelTimer)*1f/levelTimerMax
					engine.meterColor = GameEngine.METER_COLOR_LEVEL
				} else {
					engine.meterValue = levelTimer/2f/levelTimerMax
					engine.meterValue += (1-engine.meterValue)*(prog)*levelTimer/lastLineTime/norm
					engine.meterColor = GameEngine.METER_COLOR_LIMIT
				}
			}
			// Section time
			if(engine.statistics.level>=0&&engine.statistics.level<sectionTime.size)
				sectionTime[engine.statistics.level] = engine.statistics.time-sectionTime.take(engine.statistics.level).sum()

			if(nowMission is Shutter) setHeboHidden(engine)
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

	override fun onARE(engine:GameEngine):Boolean {
		if(!engine.timerActive&&engine.statc[0]==0) {
			//Next mission
			if(prog>=norm)
				missionPos = engine.statistics.level
			engine.resetStatc()
			return true
		}

		return super.onARE(engine)
	}
	/** Calculates lines-clear score
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
		if(checked&&prog>=norm) {
			// Game completed
			if(lv>=nowCourse.goalLevel) {
				owner.musMan.fadeSW = true
				engine.playSE("levelup_section")

				// Update section time
				if(engine.timerActive) sectionsDone++

				engine.ending = 1
				engine.timerActive = false

				/*if(mission is Dark||mission is Shutter) {
					// HELL-X ending & VOID ending
					engine.staffrollEnable = true
					engine.statistics.rollClear = 1
				} else {*/
				engine.gameEnded()
				engine.statistics.rollClear = 2
				//}
			} else {
				nowMission?.unload(engine)
				if(nowCourse.bgmChange.any {it==lv}) {
					owner.musMan.fadeSW = true// BGM fadeout
					engine.playSE("levelup_section")
				}
				engine.playSE("levelup")
				engine.statistics.level++
				owner.bgMan.nextBg = engine.statistics.level+nowCourse.bgOffset

				sectionsDone++

				engine.timerActive = false // Stop timer until the next piece becomes active

			}
		}
		return 0
	}

	override fun onGameOver(engine:GameEngine):Boolean {
		if(engine.statc[0]==0) {
			if(engine.lives>0) setSpeed(engine)
		}
		return super.onGameOver(engine)
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

	/** Renders game result screen */
	override fun renderResult(engine:GameEngine) {
		if(!netIsWatch) receiver.drawMenu(
			engine, 0, 0, "${UP_S}${DOWN_S} PAGE${engine.statc[1]+1}/3", BASE,
			COLOR.RED
		)

		if(engine.statc[1]==0) {
			val gcolor = when(engine.statistics.rollClear) {
				2 -> COLOR.ORANGE
				1 -> COLOR.GREEN
				else -> COLOR.RED
			}
			receiver.drawMenu(engine, 0, 1, "LIFE REMAINED", BASE, COLOR.BLUE, .8f)
			receiver.drawMenu(engine, 7, 1, "%2d".format(engine.lives), NUM, gcolor, 2f)

			receiver.drawMenu(engine, 0, 2, "%04d".format(prog), NUM, gcolor, 2f)
			receiver.drawMenu(engine, 6, 3, "Lines", BASE, COLOR.BLUE, .8f)

			drawResultStats(
				engine, receiver, 4, COLOR.BLUE, Statistic.LPM, Statistic.TIME, Statistic.PPS, Statistic.PIECE
			)
			drawResultRank(engine, receiver, 14, COLOR.BLUE, rankingRank)
			drawResultNetRank(engine, receiver, 16, COLOR.BLUE, netRankingRank[0])
			drawResultNetRankDaily(engine, receiver, 18, COLOR.BLUE, netRankingRank[1])
		} else if(engine.statc[1]==1||engine.statc[1]==2) {
			receiver.drawMenu(engine, 0, 2, "SECTION", BASE, COLOR.BLUE)

			var i = 0
			while(i<10&&i<sectionTime.size-engine.statc[2]*10) {
				val x = i+engine.statc[2]*10-10
				if(x>=0) if(sectionTime[x]>0) receiver.drawMenu(engine, 2, 3+i, sectionTime[x].toTimeStr, NUM)
				i++
			}
			if(sectionAvgTime>0) {
				receiver.drawMenu(engine, 0, 14, "AVERAGE", BASE, COLOR.BLUE)
				receiver.drawMenu(engine, 2, 15, sectionAvgTime.toTimeStr, BASE)
			}
		}

		if(netIsPB) receiver.drawMenu(engine, 2, 20, "NEW PB", BASE, COLOR.ORANGE)

		if(netIsNetPlay&&netReplaySendStatus==1) receiver.drawMenu(engine, 0, 21, "SENDING...", BASE, COLOR.PINK)
		else if(netIsNetPlay&&!netIsWatch&&netReplaySendStatus==2) receiver.drawMenu(engine, 1, 21, "A: RETRY", BASE,
			COLOR.RED)
	}

	/** This function will be called when the replay data is going to be
	 * saved */
	override fun saveReplay(engine:GameEngine, prop:CustomProperties):Boolean {
		saveSetting(engine, prop)

		// NET: Save name
		if(netPlayerName!=null&&netPlayerName!!.isNotEmpty()) prop.setProperty(
			"${engine.playerID}.net.netPlayerName", netPlayerName
		)

		if(!owner.replayMode&&netIsNetRankingViewOK(engine)) {
			ranking[goalType].add(Rankable.TimeRow(engine.statistics))

			if(rankingRank!=-1) return true
		}
		return false
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
			"$rollTime${"\t$prog\t$bg\t${engine.meterValue}\t"+engine.meterColor}\t"+
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
		prog = message[16].toInt()
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
			"NORM;$prog\tLEVEL;${engine.statistics.level+engine.statistics.levelDispAdd}\t"+
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
		val msg = "game\toption\t$goalType\t$startLevel\t$showST\n"
		netLobby?.netPlayerClient?.send(msg)
	}

	/** NET: Receive game options */
	override fun netRecvOptions(engine:GameEngine, message:List<String>) {
		goalType = message[4].toInt()
		startLevel = message[5].toInt()
		showST = message[6].toBoolean()
	}

	/** NET: Get goal type */
	override val netGetGoalType get() = goalType

	/** NET: It returns true when the current settings don't prevent
	 * leaderboard screen from showing. */
	override fun netIsNetRankingViewOK(engine:GameEngine):Boolean = startLevel==0&&engine.ai==null

	companion object {
		/** Current version of this mode */
		private const val CURRENT_VERSION = 1
		private const val DEFAULT_SKIN = GameEngine.FRAME_COLOR_WHITE

		/** Default section record */
		private const val DEFAULT_SECTION_TIME = 6000
		@Serializable
		/** ミッションクリア後にライン上下　*/
		sealed class InterLines {
			abstract val lines:Int
			/** ミッションクリア時にラインが[lines]段下がる */
			@Serializable
			data class FALL private constructor(override val lines:Int):InterLines() {
				companion object {
					operator fun invoke(li:Int) = FALL(li.coerceAtLeast(0))
				}
			}
			/** ミッションクリア時にラインが[lines]段上がる
			 *@param messiness falseでせりあがるラインの穴が揃う */
			data class RISE private constructor(override val lines:Int, val messiness:Boolean = true):InterLines() {
				companion object {
					operator fun invoke(li:Int, me:Boolean = true) = RISE(li.coerceIn(0, 18), me)
				}
			}

		}

		/** Mision Types */
		sealed class Mission {
			/** この問題におけるスピードレベル。0~27で指定される。
			 * マイナス値に設定するとアナザーレベル（超高速帯）の設定となる**/
			open val lv:Int = 0
			/** この問題のミッションクリアまでのノルマカウント。**/
			open val norm:Int = 5
			/** Mission Progress */
			var prog = 0

			/** この問題の制限時間(秒)です。ミッションスタート時から減っていき、これが0になるとゲームオーバーです。
			 * 0の場合は時間制限なしになります。(耐久では0になった状態で操作中のブロックを設置するとクリアです)*/
			open val time:Int = 0
			/** [norm]ライン数を消すとクリア*/
			data class LevelStar(override val lv:Int, override val norm:Int, override val time:Int = 0):Mission()

			/**消去ライン数にかかわらず、時間切れまで耐えるとクリアです。
			　無限回転を行っているとタイマーが減らなくなります。*/
			data class Enduro(override val lv:Int, override val time:Int = 90):Mission()
			/** 1ライン消しを[norm]回数行うとクリア。2ライン消し以上は対象外。
			 * @param chain trueでは1ライン消し以外を行うとやりなおし。
			 */
			data class Singles(override val lv:Int, override val norm:Int, override val time:Int = 0, val chain:Boolean = false):
				Mission()
			/** 2ライン消しを[norm]回数行うとクリア
			 * @param strict falseだと3ライン消し以上も対象となる
			 * @param chain 非対象のライン消しをするとやりなおし*/
			data class Doubles(override val lv:Int, override val norm:Int, override val time:Int = 0,
				val strict:Boolean = false, val chain:Boolean = false):Mission()
			/** 3ライン消しを[norm]回数行うとクリア
			 * @param strict falseだと3ライン消し以上も対象となる
			 * @param chain 非対象のライン消しをするとやりなおし*/
			data class Triples(override val lv:Int, override val norm:Int, override val time:Int = 0,
				val strict:Boolean = false, val chain:Boolean = false):Mission()

			/** 4ライン消しを[norm]回数行うとクリア
			 * 5ライン消し以上も対象、非対象のライン消しでやり直しにならない*/
			data class Quads(override val lv:Int, override val norm:Int, override val time:Int = 0):Mission()

			/**1ライン消し、2ライン消し、3ライン消し、4ライン消しをそれぞれ1回以上するとクリア*/
			data class Cycle(override val lv:Int, override val time:Int = 0):Mission() {
				var tmp = 0
				override val norm:Int
					get() = tmp and 1+(tmp ushr 1 and 1)+(tmp ushr 2 and 1)+(tmp ushr 3 and 1)
			}
			/**ライン消しをしない設置をせずに、[norm]回以上連続でラインを消すコンボを達成すればクリア
			 *@param multiLine trueにすると1ライン消しではカウントが変化しなくなる*/
			data class Combo(override val lv:Int, override val norm:Int, override val time:Int = 0,
				val multiLine:Boolean = false):Mission()
			/** [norm]回以上連続で4ライン消しやTwistを行うとクリア、途中でそれら以外のライン消しを行うとやり直し*/
			data class B2BChain(override val lv:Int, override val norm:Int, override val time:Int = 0):Mission()

			/** 中抜きで2~3ライン消しを[time]秒内に[norm]回数行うとクリア
			 * @param lines 2,3のどちらかで指定同時ライン数以外を対象外にする 0にすると同時消ししたライン数に応じて進行*/
			data class Split private constructor(override val lv:Int, override val norm:Int, override val time:Int = 0,
				val lines:Int):Mission() {
				companion object {
					operator fun invoke(lv:Int, norm:Int, time:Int = 0, lines:Int = 0) = Split(lv, norm, time, lines.coerceIn(0, 3))
				}
			}

			/** Twistでライン消しを[time]秒内に[norm]回数行うとクリア
			 * @param zeroL 1以上で、ライン消しをしないTwistの指定回数でノルマ1回分とカウントする 0ではカウントしない
			 * @param shapes 指定ピース以外を対象外にする
			 * @param lines 指定同時ライン数以外を対象外にする
			 * @param progL trueにすると同時消ししたライン数に応じて進行
			 */
			data class Twister private constructor(override val lv:Int, override val norm:Int, override val time:Int = 0,
				val zeroL:Int, val shapes:Set<Shape> = emptySet(), val lines:Set<Int> = emptySet(), val progL:Boolean = false):
				Mission() {
				companion object {
					operator fun invoke(lv:Int, norm:Int, time:Int = 0, zeroL:Int, shape:Set<Shape> = emptySet(),
						line:Set<Int> = emptySet(), progL:Boolean = false) =
						Twister(lv, norm, time, zeroL, shape.filter {it in Shape.Tetras}.toSet(),
							line.filter {it in 1..3}.toSet(), progL)

					operator fun invoke(lv:Int, norm:Int, time:Int = 0, zeroL:Int, shapes:Set<Shape> = emptySet(), lines:Int,
						progL:Boolean = false) =
						invoke(lv, norm, time, zeroL, shapes, setOf(lines), false)

					operator fun invoke(lv:Int, norm:Int, time:Int = 0, zeroL:Int, shapes:Shape, lines:Set<Int>, progL:Boolean =
						false) =
						invoke(lv, norm, time, zeroL, setOf(shapes), lines, false)

					operator fun invoke(lv:Int, norm:Int, time:Int = 0, zeroL:Int, shapes:Shape, lines:Int, progL:Boolean = false) =
						invoke(lv, norm, time, zeroL, setOf(shapes), setOf(lines), false)

				}
				/** 0line Twistの回数をカウントするための変数。*/
				var tmp = 0
			}

			/** [shapes]のピースで[time]秒以内に[norm]回以上ラインを消すとクリア。[shapes]以外のピースはカウント対象外
			 * @param lines 1～4で、同時に消すライン数の指定。0以下の場合は指定なし。
			 * @param holdUse trueの場合、[shapes]をHOLDスロットに経由させなければカウントされない
			 */
			data class Order private constructor(override val lv:Int, override val norm:Int, override val time:Int = 0,
				val shapes:Set<Shape> = emptySet(), val lines:Set<Int> = emptySet(), val holdUse:Boolean = false):Mission() {
				companion object {
					operator fun invoke(lv:Int, norm:Int, time:Int = 0, shapes:Set<Shape> = emptySet(), lines:Set<Int> = emptySet(),
						holdUse:Boolean = false) =
						Order(lv, norm, time, shapes.filter {it in Shape.Tetras}.toSet(), lines.filter {it in 1..4}.toSet(), holdUse)

					operator fun invoke(lv:Int, norm:Int, time:Int = 0, shapes:Set<Shape> = emptySet(), lines:Int = 0,
						holdUse:Boolean = false) = Order(lv, norm, time, shapes, setOf(lines), holdUse)

					operator fun invoke(lv:Int, norm:Int, time:Int = 0, shapes:Shape, lines:Set<Int> = emptySet(),
						holdUse:Boolean = false) = Order(lv, norm, time, setOf(shapes), lines, holdUse)

					operator fun invoke(lv:Int, norm:Int, time:Int = 0, shapes:Shape, lines:Int = 0,
						holdUse:Boolean = false) = Order(lv, norm, time, setOf(shapes), setOf(lines), holdUse)
				}
			}
			/** XRAY(ブロックが一瞬しか見えない)状態で[time]秒以内に[norm]ラインを消すとクリア*/
			data class XRay(override val lv:Int, override val norm:Int, override val time:Int = 0):Mission()
			/** Color Illuminate(ブロックが点滅する)状態で[time]秒以内に[norm]ラインを消すとクリア*/
			data class Color(override val lv:Int, override val norm:Int, override val time:Int = 0):Mission()
			/** Roll(ブロックが強制的に回転する)状態で[time]秒以内に[norm]ラインを消すとクリア*/
			data class RollRoll(override val lv:Int, override val norm:Int, override val time:Int = 0):Mission()
			/** Mirror([freq]ピース毎にフィールドが左右反転する)状態で[time]秒以内に[norm]ラインを消すとクリア*/
			data class Mirror private constructor(override val lv:Int, override val norm:Int, override val time:Int = 0,
				val freq:Int = 0):Mission() {
				companion object {
					operator fun invoke(lv:Int, norm:Int, time:Int = 0, freq:Int = 0) = Mirror(lv, norm, time, freq.coerceAtLeast(0))
				}
			}
			/** 操作の上下左右が逆転した状態で[time]秒以内に[norm]ラインを消すとクリア*/
			data class Dizzy(override val lv:Int, override val norm:Int, override val time:Int = 0):Mission()
			/** 回転が不可能な状態で[time]秒以内に[norm]ラインを消すとクリア*/
			data class SpinLock(override val lv:Int, override val norm:Int, override val time:Int = 0):Mission()
			/** 次に出現するピースが見えない状態で[time]秒以内に[norm]ラインを消すとクリア*/
			data class HideNext(
				override val lv:Int, override val norm:Int, override val time:Int = 0):Mission()
			/** [freq]ピース毎に最下段コピーせり上がりが起こる中で[time]秒以内に[norm]ラインを消すとクリア
			 * [lv]がマイナス値ならGrand Lightningモードlv800に近くなる*/
			data class Pressure(override val lv:Int, override val norm:Int, override val time:Int = 0, val freq:Int = 0):
				Mission()

			/**[time]秒以内に[norm]ラインを消すとクリア。ただし、最初のピースを置くと[height]段せり上げられる*/
			data class Spiked private constructor(override val lv:Int, override val norm:Int, override val time:Int = 0,
				val height:Int = 1):Mission() {
				var temp = false

				companion object {
					operator fun invoke(lv:Int, norm:Int, time:Int = 0, height:Int) = Spiked(lv, norm, time, height.coerceIn(0, 18))
				}
			}
			/** wallkickとHOLD、先行回転、NEXT表示数が制限された状態で[time]秒以内に[norm]ラインを消すとクリア
			 * ブロック、フィールド枠やスピードテーブルも一時的に変化する
			 * @param type 1で接地時の固定猶予を無しにする	スピードテーブルも調整される
			 */
			data class Retro(override val lv:Int, override val norm:Int, override val time:Int = 0, val type:Int = 0):Mission()
			/*


	・上下左右逆転(LRUD REV)
	　操作の上と下・左と右が逆転した状態でレベルスターを行います。

	・全消し(ALL CLEAR)
	　全消しを指定回数行うとクリアです。
	　難易度の都合上、(通常は)BIGがかかります。
	　□ OPTIONSの値
	　　 OPT: 0以外にすると…？

	・ASSHOLES
	　(・w・)接地固定猶予とAREが一切ない状態で[norm]個ピースを置くとクリア

			 */
			/** ピースを組み合わせて4x4の正方形を[time]秒以内に[norm]個作るとクリア。
			 * @sample「ライン消しに巻き込まれて残った破片を含まない」「ブロックが4x4の外側に繋がっていない」が条件。
			 * @see MarathonSquare*/
			data class Squares(
				override val lv:Int, override val norm:Int, override val time:Int = 0):Mission()
			/** 1色のピースだけ([shapes]指定時はそのピースのみ)を使って4x4の正方形を[time]秒以内に[norm]個作るとクリア
			 * S,Zは指定不可 */
			data class GoldSquares private constructor(override val lv:Int, override val norm:Int, override val time:Int = 0,
				val shapes:Set<Shape> = emptySet())
				:Mission() {
				companion object {
					operator fun invoke(lv:Int, norm:Int, time:Int = 0, shapes:Set<Shape> = emptySet()) =
						GoldSquares(lv, norm, time, shapes.filter {it in Shape.Tetras&&it!=Shape.S&&it!=Shape.Z}.toSet())

				}
			}

			/** ピースブロックが大きい状態で[time]秒以内に[norm]本ラインを消せばクリア
			消したライン数 = [prog]は、(見た目での消したライン数÷２)だけカウント*/
			data class BigBlock(override val lv:Int, override val norm:Int, override val time:Int = 0):Mission()

			/** [time]秒以内に[norm]本ターゲットライン上でライン消しをするとクリア
			全ての線を消してもまだ[norm]が残っている場合はターゲット再出現
			 *@param heightRange ターゲットの出現範囲
			 *@param multi ターゲットの同時出現数 1~4
			 *@param quad trueの場合、4ライン消しのみがカウントされる*/
			data class Ladder(override val lv:Int, override val norm:Int, override val time:Int = 0,
				private val heightRange:IntRange, val multi:Int, val quad:Boolean):Mission() {
				val target = mutableSetOf<Int>()
				fun reset(random:Random) {
					while(target.size<minOf(norm-prog, multi)) {
						target.add(random.nextInt(heightRange.last-heightRange.first)+heightRange.first)
					}
				}
			}
			/** Grand Blossamモードで使用されるパズルマップのジェムブロック全消しを[time]秒以内に[norm]回達成でクリア。
			 * せり上がり以外のギミックは発動しない
			 * @param mapRange 出現するマップの範囲 (0～26=Ti 27～44=EH 45～67=ACE)
			 * @param rndTarget 1以上にするとジェムブロックを数値分ランダムで配置する。0(OFF)では初期配置のまま。
			 * 	99以上にすると…？*/
			data class Target(override val lv:Int, override val norm:Int, val mapRange:IntRange, override val time:Int = 0,
				val rndTarget:Int):Mission()
			/**積んであるブロックが枠しか見えない状態で[time]秒以内に[norm]本ラインを消せばクリア
			 * @param misc 1以上で、数値に応じた速度で固定したブロックが順次不可視になる。*/
			data class Dark(override val lv:Int, override val norm:Int, override val time:Int = 0, val misc:Int):
				Mission()
			/**フィールドが下段から隠されていく状態で[time]秒以内に[norm]本ラインを消せばクリア
			 * @param misc フィールドが隠れる速度*/
			data class Shutter(override val lv:Int, override val norm:Int, override val time:Int = 0, val misc:Int)
				:Mission()

			/**ブロックが[ ]に変化した状態で[time]秒以内に[norm]本ラインを消せばクリア
			 * lvがマイナスならGrand Lightning 1000~準拠となる */
			data class Monochrome(override val lv:Int, override val norm:Int, override val time:Int = 0):Mission()
			/** Mission Ready */
			val ready:(GameEngine)->Unit by lazy {

				when(this) {
					is Combo -> {e -> e.comboType = 1+multiLine.toInt()}
					is Twister -> {e ->
						e.twistEnable = true
						e.twistAllowKick = true
						e.useAllSpinBonus = true
					}
					is Retro -> {e ->
						e.frameSkin =
							if(type>0) {
								e.speed.lockDelay = 0
								e.speed.are = 20
								e.speed.areLine = 20
								GameEngine.FRAME_SKIN_GB
							} else GameEngine.FRAME_SKIN_SG
//							e.wallkickEnable = false
						e.holdDisable = true
//							e.initialSpinEnable = false
					}
					is BigBlock -> {e ->
						e.big = true
						e.bigMove = true
						e.bigHalf = true
					}
					is Ladder -> {e -> reset(e.random)}
					is Target -> {e ->
//						e.field.replace(loadMap(mapRange.random(e.random)))
//						if(misc>0) e
					}
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
					is Monochrome -> {e -> e.bone = true}
					else -> {_ ->}
				}
			}
			/** Mission Unload */
			val unload:(GameEngine)->Unit by lazy {
				when(this) {
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
					is Retro -> {e ->
						e.frameSkin = DEFAULT_SKIN
//							e.wallkickEnable = true
						e.holdDisable = false
//							e.initialSpinEnable = true
					}
					is BigBlock -> {e ->
						e.big = false
						e.bigMove = false
						e.bigHalf = false
					}
					is Dark -> {e -> e.blockHidden = -1}
					is Shutter -> {e ->
						e.heboHiddenEnable = false
						e.heboHiddenYLimit = 0
					}
					is Monochrome -> {e -> e.bone = false}
					else -> {_ ->}
				}
			}
			/** Check if the mission progress has been updated */
			fun checkProgress(engine:GameEngine, ev:ScoreEvent):Boolean {
				val prev = prog
				val li = ev.lines
				when(this) {
					is Singles -> if(li==1) prog++ else if(chain) prog = 0
					is Doubles -> if(if(strict) li==2 else li>=2) prog++ else if(chain) prog = 0
					is Triples -> if(if(strict) li==3 else li>=3) prog++ else if(chain) prog = 0
					is Quads -> if(li==4) prog++
					is Split -> if(ev.split&&(lines !in 2..3||li==lines)) if(lines<=0) prog += li else prog++
					is Twister -> if(ev.twist) if(li<=0) {
						if(++tmp>=zeroL) {
							prog++
							tmp = 0
						}
					} else if(lines.contains(li)) if(progL) prog += li else prog++
					is Combo -> prog = ev.combo
					is B2BChain -> prog = ev.b2b
					is Cycle -> {
						tmp = tmp or (1 shl li)
						prog = tmp and 1+(tmp and 2>0).toInt()+(tmp and 4>0).toInt()+(tmp and 8>0).toInt()
					}
					is Shutter -> {
						prog += li
						// Decrease Shutter Hidden
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

			/** Game type names (short) */
			val showName by lazy {
				"${
					when(this) {
						is LevelStar -> if(lv>=0) "Level Star" else "Speed Star"
//						is Enduro -> "Enduro"
//						is Singles -> "Singles"
						is Doubles -> "$norm Doubles${"+".takeIf {!strict}?:""}${" Chain".takeIf {chain}?:""}"
						is Triples -> "$norm Triples${"+".takeIf {!strict}?:""}${" Chain".takeIf {chain}?:""}"
//						is Quads -> "Quads"
						is Cycle -> "Line Cycles"
//						is Combo -> "Combo"
						is B2BChain -> "B2B Chain"
						is Split -> when(lines) {
							1 -> "Splits Count"
							2 -> "Split Double"
							3 -> "1.2. Triples"
							else -> "Splits Total"
						}
						is Twister -> when {
							lines.all {it==1} -> "$norm Twister-Singles"
							lines.all {it==2} -> "$norm Twister-Doubles"
							lines.all {it==3} -> "$norm Twister-Triples"
							progL -> "$norm Twister Total"
							else -> "Twister Count"
						}
						is Order -> "${if(shapes.size<=Shape.numTetras/2) shapes.str else "Anti ${(Shape.Tetras-shapes).str}"}Order"+
							(" From Hold".takeIf {holdUse}?:"")
						is XRay -> "X-Ray"
						is Color -> "Color Blind"
						is RollRoll -> "Roll Roll"
						is Mirror -> "Mirror"
						is Dizzy -> "Dizzy"
						is SpinLock -> "Rotate Lock"
						is HideNext -> "Hide Next"
						is Pressure -> if(lv>=0) "Pressure" else "Blitz Rizer"
						is Spiked -> "$height Spiked!"
						is Retro -> "Retrospective"
						is GoldSquares -> "Gold ${shapes.str}Square"
						is BigBlock -> "BIG block"
						is Ladder -> "Ladder Liner"
						is Target -> "Target Dig"
						is Dark -> "Hiding Dark"
						is Shutter -> "Rising Shutter"
						is Monochrome -> if(lv>=0) "Monochrome" else "Blitz Ball"
						else -> "$norm ${this::class.simpleName}"
					}
				}${
					when(this) {
						is LevelStar -> "+${lv}"
						is Monochrome, is Pressure -> "L${lv}"
						else -> "${if(lv>=0) "L" else "EX"}${lv.absoluteValue}"
					}.let {if(it.isNotBlank()) " $it" else it}
				}"
			}

			companion object {
				val Collection<Shape>.str get() = joinToString(separator = "", postfix = " ") {it.name}
			}
		}
		@Serializable
		enum class Course {
			ARookie, G8, G7, G6, G5, G4, G3, G2, G1,
			S1, S2, S3, S4, S5, S6, S7, S8, S9, S10, GM,
			GiantStep, Tricky, Grand, Star, Another, DS, Devil,
			Amateur, Pro, Bronze, Silver, Gold, Platinum;
			/** Game type names */
			val showName by lazy {
				when(this) {
					S1 -> "Easy"
					Amateur, Pro, Bronze, Silver, Gold, Platinum -> name.first().toString()+name.drop(1).lowercase()
					else -> name

				}
			}
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
	　グランドロードに似ていますが、
	　終盤には20G状態の"Speed Star"が待ち構えているぞ！
	　EXミッション:ハイスピード2

	▼アナザーロード
	　ミッションの総数が多く、その大半に追加制限があるコース
	　長丁場にどこまで耐えられるか？
	　EXミッション:アナザー

	▼DSロード
	　ノルマ控えめの短いミッション多数で構成されたコース
	　その分だけ制限時間も短くなってくる。
	　EXミッション:耐久

	▼デビルロード
	　アナザーロードも楽々クリアーできるプレイヤーへの挑戦状。
	　ほとんどのミッションの速度がアナザーと同じという、
	　凶悪極まりないロードです。
	　EXミッション:DEVIL 1200(REAL)　…?

	▼トライアル　一段～十段
	　まずはこれらのミッションで肩慣らしをするといいでしょう。

	▼トライアル　HM(ヘボマニア)
	　アナザーが3つもある難度の高いトライアルです。

	▼トライアル　ネ申(GOD)
	　(・w・)

	▼アマチュア・プロ・ブロンズ・シルバー・ゴールド・プラチナ
	　制限時間はありませんが、ミッション間のライン消去は一切発生しません。
	　常に後のミッションの事を考えてプレイする必要があります。

	*/
			val missions:List<Mission> by lazy {
				when(this) {
					S1 -> listOf(BigBlock(2, 20, 60),
						LevelStar(1, 6, 70))
					GiantStep -> listOf(
						BigBlock(2, 10, 60), Quads(4, 5, 120),
						Cycle(4, 120), BigBlock(5, 20, 120), LevelStar(6, 15, 90))
					Amateur -> listOf(
						LevelStar(1, 6), Doubles(1, 1),
					)
					else -> emptyList()
				}
			}
			val afterMission:(i:Int)->InterLines? by lazy {
				when(this) {
					S1 -> {_ -> InterLines.FALL(5)}
					S2, S3 -> {i ->
						InterLines.FALL(if(i==0) 10 else 5)
					}
					Amateur, Pro, Bronze, Silver, Gold, Platinum -> {_ -> null}
					else -> {_ -> InterLines.FALL(1)}
				}
			}
			val goalLevel by lazy {missions.size}

			/** BGM table */
			val bgmList by lazy {
				when(this) {
					G1 -> listOf(BGM.Puzzle(0))
					S1 -> listOf(BGM.Puzzle(0), BGM.GrandA(0), BGM.GrandM(0))
					else -> listOf(BGM.Puzzle(0), BGM.GrandA(0), BGM.GrandM(0))

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
					else -> ordinal
				}
			}

			companion object {
				val all = entries.toList()
				/*private object Serializer:KSerializer<Course> {
					override val descriptor:SerialDescriptor = PrimitiveSerialDescriptor("Course", PrimitiveKind.STRING)

					override fun deserialize(decoder: Decoder): Course = all[decoder.decodeInt()]
					override fun serialize(encoder: Encoder, value: Course) = encoder.encodeStructure(descriptor){
						encodeStringElement(descriptor, 0, value.name)
						encodeSerializableElement(descriptor, 1, value.missions)

					}

				}*/
			}
		}

		/** HELL-X fade table */
		private val tableHellXFade =
			listOf(
				600, 550, 500, 450, 400, 350, 300, 270, 240, 210,
				190, 170, 160, 150, 140, 130, 125, 120, 115, 110,
				100, 90, 80, 70, 60, 58, 56, 54, 52, 50
			)
		/** Ending time limit */
		/** Number of Course & ranking types */
		val courses = Course.all
		private val COURSE_MAX = courses.size
		private const val ROLLTIMELIMIT = 3238

		/** Level data */
		val speeds = listOf(
			LevelData(
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
			),
			LevelData(
				listOf(25, 24, 23, 22, 21, 20, 19, 18, 17, 16, 15, 14, 13, 12, 11, 10), // ARE
				listOf(30, 28, 26, 24, 22, 20, 18, 16, 14, 12, 10, +9, +8, +7, +6, +5), // Line delay
				listOf(30, 30, 29, 29, 28, 28, 27, 27, 26, 25, 25, 24, 23, 22, 21, 20), // Lock delay
				listOf(10, 10, 10, 10, 10, 10, +9, +9, +9, +8, +8, +8, +7, +7, +7, +7) // DAS
			)
		)
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

■OPTIONS
追加情報です。ターゲット、イレイサー、GARBAGEで使われます。

■BGM
BGMを指定できます。番号はSOUND TESTと同じです。
FADEにするとBGMがフェードアウトします。
*/
