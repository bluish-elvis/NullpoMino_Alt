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
import mu.nu.nullpo.game.component.LevelData
import mu.nu.nullpo.game.event.EventReceiver
import mu.nu.nullpo.game.event.ScoreEvent
import mu.nu.nullpo.game.net.NetUtil
import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.game.subsystem.mode.menu.BooleanMenuItem
import mu.nu.nullpo.game.subsystem.mode.menu.DelegateMenuItem
import mu.nu.nullpo.game.subsystem.mode.menu.LevelMenuItem
import mu.nu.nullpo.game.subsystem.mode.menu.MenuList
import mu.nu.nullpo.game.subsystem.mode.menu.StringsMenuItem
import mu.nu.nullpo.util.CustomProperties
import mu.nu.nullpo.util.GeneralUtil.toTimeStr
import kotlin.math.floor
import kotlin.math.ln

/** TIME ATTACK mode (Original from NullpoUE build 010210 by Zircean.
 * This mode is heavily modified from the original.) */
class GrandRoads:NetDummyMode() {

	/** Remaining level time */
	private var levelTimer = 0
	private var lastlinetime = 0

	/** Original level time */
	private var levelTimerMax = 0

	/** Current lines (for levelup) */
	private var norm = 0

	/** Lines for Next level */
	private var nextLv = 0
	/** Current BGM number */
	private var bgmLv = 0

	/** Elapsed ending time */
	private var rolltime = 0

	/** Ending started flag */
	private var rollstarted = false

	/** Section time */
	private var sectionTime = IntArray(0)

	/** Number of sections completed */
	private var sectionscomp = 0

	/** Average section time */
	private var sectionavgtime = 0

	private val itemMode = StringsMenuItem(
		"goalType", "DIFFICULTY", EventReceiver.COLOR.BLUE, 0,
		courses.map {it.showName}.toTypedArray()
	)
	/** Game type */
	private var goalType:Int by DelegateMenuItem(itemMode)
	private var nowCourse
		get() = courses[maxOf(0, goalType%courses.size)]
		set(value) {
			goalType = courses.indexOfFirst {it==value}
		}

	private val itemLevel = LevelMenuItem(
		"startlevel", "LEVEL", EventReceiver.COLOR.RED, 0,
		0..(courses.maxOfOrNull {it.goalLevel} ?: 0), true, true
	)
	/** Selected starting level */
	private var startLevel:Int by DelegateMenuItem(itemLevel)

	private val itemBig = BooleanMenuItem("big", "BIG", EventReceiver.COLOR.BLUE, false)
	/** BigMode */
	private var big:Boolean by DelegateMenuItem(itemBig)

	private val itemST = BooleanMenuItem("showsectiontime", "SHOW STIME", EventReceiver.COLOR.BLUE, true)
	/** Show section time */
	private var showST:Boolean by DelegateMenuItem(itemST)

	override val menu:MenuList = MenuList("timeattack", itemMode, itemLevel, itemST, itemBig)
	/** Version of this mode */
	private var version = 0

	/** Your place on leaderboard (-1: out of rank) */
	private var rankingRank = 0

	/** Ranking ecords */
	private val rankingLines = List(COURSE_MAX) {MutableList(RANKING_MAX) {0}}
	private val rankingLifes = List(COURSE_MAX) {MutableList(RANKING_MAX) {0}}
	private val rankingTime = List(COURSE_MAX) {MutableList(RANKING_MAX) {0}}
	private val rankingRollclear = List(COURSE_MAX) {MutableList(RANKING_MAX) {0}}

	/** Returns the name of this mode */
	override val name = "Grand Roads"
	override val gameIntensity:Int
		get() = when(nowCourse) {
			Course.HARD, Course.CHALLENGE -> 1
			Course.HARDEST, Course.SUPER, Course.SURVIVAL -> 2
			Course.XTREME, Course.HELL, Course.HIDE, Course.VOID -> 3
			else -> 0
		}
	override val rankMap
		get() = rankMapOf(
			rankingLines.mapIndexed {a, x -> "$a.lines" to x}+
				rankingLifes.mapIndexed {a, x -> "$a.lifes" to x}+
				rankingTime.mapIndexed {a, x -> "$a.time" to x}+
				rankingRollclear.mapIndexed {a, x -> "$a.rollclear" to x})

	/** This function will be called when the game enters the main game
	 * screen. */
	override fun playerInit(engine:GameEngine) {
		super.playerInit(engine)
		norm = 0
		goalType = 0
		startLevel = 0
		rolltime = 0
		lastlinetime = 0
		rollstarted = false
		sectionTime = IntArray(courses.maxOf {it.goalLevel})
		sectionscomp = 0
		sectionavgtime = 0
		big = false
		showST = true

		rankingRank = -1
		rankingLines.forEach {it.fill(0)}
		rankingLifes.forEach {it.fill(0)}
		rankingTime.forEach {it.fill(0)}
		rankingRollclear.forEach {it.fill(0)}

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
		engine.speed.replace(nowCourse.speeds[engine.statistics.level])
		engine.owDelayCancel = if(nowCourse==Course.HARDEST||nowCourse==Course.LONG||nowCourse==Course.CHALLENGE) 7 else -1
		// Show outline only
		engine.blockShowOutlineOnly = nowCourse==Course.HELL||nowCourse==Course.HIDE
		// Bone blocks
		engine.bone = (nowCourse==Course.HIDE&&engine.statistics.level>=20||nowCourse==Course.VOID)

		// for test
		/* engine.speed.are = 25; engine.speed.areLine = 25;
 * engine.speed.lineDelay = 10; engine.speed.lockDelay = 30;
 * engine.speed.das = 12; levelTimerMax = levelTimer = 3600 * 3; */

		levelTimer = LevelData.lv(nowCourse.levelTimer, engine.statistics.level)+levelTimer/2
		levelTimerMax = levelTimer
		// Blocks fade for HELL-X
		engine.blockHidden = if(nowCourse==Course.HIDE) LevelData.lv(tableHellXFade, engine.statistics.level) else -1


		lastlinetime = levelTimer
	}

	/** Set Pressure Hidden params
	 * @param engine GameEngine
	 */
	private fun setHeboHidden(engine:GameEngine) {
		if(nowCourse==Course.HIDE&&engine.statistics.level>=15||nowCourse==Course.VOID) {
			engine.heboHiddenEnable = true
			when(if(nowCourse==Course.VOID) engine.statistics.level/5 else (engine.statistics.level-15)/2) {
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
			}
		} else
			engine.heboHiddenEnable = false
	}

	/** Main routine for game setup screen */
	override fun onSetting(engine:GameEngine):Boolean {
		// NET: Net Ranking
		if(netIsNetRankingDisplayMode)
			netOnUpdateNetPlayRanking(engine, netGetGoalType())
		else if(!engine.owner.replayMode) {
			// Configuration changes
			val change = updateMenu(engine)

			if(change!=0) {
				engine.playSE("change")

				if(startLevel>nowCourse.goalLevel-1) startLevel =
					if(menuCursor==menu.items.indexOf(itemLevel)) 0 else nowCourse.goalLevel-1
				engine.owner.bgMan.bg = startLevel
				engine.statistics.level = startLevel
				setSpeed(engine)
				// NET: Signal options change
				if(netIsNetPlay&&netNumSpectators>0) netSendOptions(engine)
			}

			// Check for A button, when pressed this will begin the game
			if(menuTime<5) menuTime++ else if(engine.ctrl.isPush(Controller.BUTTON_A)) {
				engine.playSE("decide")

				// NET: Signal start of the game
				if(netIsNetPlay) netLobby!!.netPlayerClient!!.send("start1p\n")

				return false
			}

			// Check for B button, when pressed this will shut down the game engine.
			if(engine.ctrl.isPush(Controller.BUTTON_B)&&!netIsNetPlay) engine.quitFlag = true

			// NET: Netplay Ranking
			if(engine.ctrl.isPush(Controller.BUTTON_D)&&netIsNetPlay&&!big
				&&engine.ai==null
			)
				netEnterNetPlayRankingScreen(goalType)

		} else {
			menuTime++
			menuCursor = -1

			return menuTime<60
		}// Menu

		return true
	}

	/** Ready screen */
	override fun onReady(engine:GameEngine):Boolean {
		if(engine.statc[0]==0) {
			engine.statistics.level = startLevel
			engine.statistics.levelDispAdd = 1
			engine.big = big
			norm = (0 until startLevel).reduce {acc, i -> acc+nowCourse.goalLines(i)}
			nextLv = (0..startLevel).reduce {acc, i -> acc+nowCourse.goalLines(i)}
			levelTimer = 0
			setSpeed(engine)
			bgmLv = nowCourse.bgmChange.indexOfLast {it<=startLevel}.let {if(it<0) nowCourse.bgmChange.size-1 else it}
		}

		return false
	}

	/** This function will be called before the game actually begins
	 * (after Ready&Go screen disappears) */
	override fun startGame(engine:GameEngine) {
		owner.musMan.bgm = if(netIsWatch) BGM.Silent else nowCourse.bgmList[bgmLv]
		engine.lives = nowCourse.lives
	}

	/** Renders HUD (leaderboard or game statistics) */
	override fun renderLast(engine:GameEngine) {
		if(owner.menuOnly) return

		receiver.drawScoreFont(engine, 0, -1, "GRAND ROADS", EventReceiver.COLOR.PURPLE)
		receiver.drawScoreFont(engine, 0, 0, "TIME ATTACK", EventReceiver.COLOR.PURPLE, .75f)
		receiver.drawScoreFont(engine, 0, 1, "${nowCourse.showName} COURSE", EventReceiver.COLOR.PURPLE)
		//rereceiver.drawScore(engine, playerID, -1, -4*2, "DECORATION", scale = .5f);
		//receiver.drawScoreBadges(engine, playerID,0,-3,100,decoration);
		//receiver.drawScoreBadges(engine, playerID,5,-4,100,dectemp);
		if(engine.stat==GameEngine.Status.SETTING||engine.stat==GameEngine.Status.RESULT&&!owner.replayMode) {
			if(!owner.replayMode&&startLevel==0&&!big&&engine.ai==null&&!netIsWatch) {
				receiver.drawScoreFont(engine, 8, 3, "Time", EventReceiver.COLOR.BLUE)

				for(i in 0 until RANKING_MAX) {
					var gcolor = EventReceiver.COLOR.WHITE
					if(rankingRollclear[goalType][i]==1) gcolor = EventReceiver.COLOR.GREEN
					if(rankingRollclear[goalType][i]==2) gcolor = EventReceiver.COLOR.ORANGE
					receiver.drawScoreGrade(
						engine,
						0,
						4+i,
						String.format("%2d", i+1),
						if(i==rankingRank) EventReceiver.COLOR.RED else EventReceiver.COLOR.YELLOW
					)

					receiver.drawScoreNum(engine, 8, 4+i, rankingTime[goalType][i].toTimeStr, gcolor)
					receiver.drawScoreNano(
						engine, 10, 8+i*2, if(gcolor==EventReceiver.COLOR.WHITE) "LINES\nCLEARED" else "LIFES\nREMAINED",
						gcolor,
						.5f
					)
					receiver.drawScoreNum(
						engine, 2, 4+i, String.format(
							"%3d",
							if(gcolor==EventReceiver.COLOR.WHITE) rankingLines[goalType][i] else rankingLifes[goalType][i]
						), gcolor
					)
				}
			}
		} else {
			receiver.drawScoreFont(engine, 0, 3, "Level", EventReceiver.COLOR.BLUE)
			receiver.drawScoreNum(engine, 5, 2, String.format("%02d", engine.statistics.level+1), 2f)
			receiver.drawScoreNum(engine, 8, 3, String.format("/%3d", nowCourse.goalLevel))
			receiver.drawScoreNum(engine, 0, 4, String.format("%3d/%3d", norm, (engine.statistics.level+1)*10))

			receiver.drawScoreSpeed(
				engine, 0, 5, if(engine.speed.gravity<0) 40 else floor(
					ln(engine.speed.gravity.toDouble())
				).toInt()*(engine.speed.denominator/60),
				6
			)

			receiver.drawScoreFont(engine, 0, 7, "TIME LIMIT", EventReceiver.COLOR.BLUE)
			receiver.drawScoreNum(
				engine, 0, 8, levelTimer.toTimeStr, levelTimer in 1 until 600&&levelTimer%4==0,
				2f
			)

			receiver.drawScoreFont(engine, 0, 10, "TOTAL TIME", EventReceiver.COLOR.BLUE)
			receiver.drawScoreNum(engine, 0, 11, engine.statistics.time.toTimeStr, 2f)

			// Remaining ending time
			if(engine.gameActive&&engine.ending==2&&engine.staffrollEnable) {
				var time = ROLLTIMELIMIT-rolltime
				if(time<0) time = 0
				receiver.drawScoreFont(engine, 0, 17, "ROLL TIME", EventReceiver.COLOR.BLUE)
				receiver.drawScoreNum(engine, 0, 18, time.toTimeStr, time>0&&time<10*60, 2f)
			}

			// Section time
			if(showST&&sectionTime.isNotEmpty()&&!netIsWatch) {
				val x = if(receiver.nextDisplayType==2) 25 else 12
				val y = if(receiver.nextDisplayType==2) 4 else 2
				val scale = if(receiver.nextDisplayType==2) .5f else 1f

				receiver.drawScoreFont(engine, x, y, "SECTION TIME", EventReceiver.COLOR.BLUE, scale)

				val l = maxOf(0, engine.statistics.level-20)
				var i = l
				while(i<sectionTime.size) {
					if(sectionTime[i]>0) {
						var strSeparator = "-"
						if(i==engine.statistics.level&&engine.ending==0) strSeparator = "+"

						val strSectionTime:String = String.format("%2d%s%s", i+1, strSeparator, sectionTime[i].toTimeStr)
						receiver.drawScoreNum(engine, x+1, y+1+i-l, strSectionTime, scale)
					}
					i++
				}
				receiver.drawScoreFont(engine, 0, 13, "AVERAGE", EventReceiver.COLOR.BLUE)
				receiver.drawScoreNum(engine, 0, 14, (engine.statistics.time/(sectionscomp+1)).toTimeStr, 2f)
			}
		}
		super.renderLast(engine)
	}

	/** This function will be called when the piece is active */
	override fun onMove(engine:GameEngine):Boolean {
		// Enable timer again after the levelup
		if(engine.ending==0&&engine.statc[0]==0&&!engine.timerActive&&!engine.holdDisable)
			engine.timerActive = true

		// Ending start
		if(engine.ending==2&&engine.staffrollEnable&&!rollstarted&&!netIsWatch) {
			rollstarted = true
			owner.musMan.bgm = BGM.Finale(2)
			owner.musMan.fadesw = false
			if(nowCourse==Course.VOID) {
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
		if(engine.timerActive&&engine.ending==0)
			if(levelTimer>0) {
				levelTimer--
				if(levelTimer<=600&&levelTimer%60==0) engine.playSE("countdown")
			} else if(!netIsWatch) {
				engine.resetStatc()
				engine.stat = GameEngine.Status.GAMEOVER
			}

		// Update meter
		if(engine.ending==0&&levelTimerMax!=0) {
			engine.meterValue = levelTimer/2f/levelTimerMax
			if(norm%10>0)
				engine.meterValue += ((1-engine.meterValue)*(norm%10)*levelTimer/lastlinetime/10)
			engine.meterColor = GameEngine.METER_COLOR_LIMIT
		}

		// Section time
		if(engine.timerActive&&engine.ending==0)
			if(engine.statistics.level>=0&&engine.statistics.level<sectionTime.size) {
				sectionTime[engine.statistics.level]++
				//setAverageSectionTime();
				setHeboHidden(engine)
			}

		// Ending
		if(engine.gameActive&&engine.ending==2) {
			rolltime++

			// Update meter
			val remainRollTime = ROLLTIMELIMIT-rolltime
			engine.meterValue = remainRollTime*1f/ROLLTIMELIMIT
			engine.meterColor = GameEngine.METER_COLOR_LIMIT
			// Completed
			if(rolltime>=ROLLTIMELIMIT&&!netIsWatch) {
				engine.statistics.rollclear = 2
				engine.gameEnded()
				engine.resetStatc()
				engine.stat = GameEngine.Status.EXCELLENT
			}
		}
	}

	override fun onGameOver(engine:GameEngine):Boolean {
		if(engine.statc[0]==0) {
			if(engine.lives>0)
				setSpeed(engine)

		}
		return super.onGameOver(engine)
	}
	/** Calculates line-clear score
	 * (This function will be called even if no lines are cleared) */
	override fun calcScore(engine:GameEngine, ev:ScoreEvent):Int {
		// Don't do anything during the ending
		if(engine.ending!=0) return 0
		val lv = engine.statistics.level
		// Add lines to norm
		norm = engine.statistics.lines
		val li = ev.lines
		if(li>0) lastlinetime = levelTimer
		// Decrease Pressure Hidden
		if(engine.heboHiddenEnable&&li>0) {
			engine.heboHiddenTimerNow = 0
			engine.heboHiddenYNow -= li
			if(engine.heboHiddenYNow<0) engine.heboHiddenYNow = 0
		}
		var bgmChanged = false
		// BGM change
		if(nextLv-norm>=nowCourse.goalLines(lv)/2&&bgmLv<nowCourse.bgmChange.size&&
			(lv==nowCourse.goalLevel-1||nowCourse.bgmChange.any {it-1==lv})
		)
			owner.musMan.fadesw = true// BGM fadeout

		// Level up
		if(li>0&&norm>=nextLv)
		// Game completed
			if(lv>=nowCourse.goalLevel) {
				engine.playSE("levelup_section")

				// Update section time
				if(engine.timerActive) sectionscomp++

				engine.ending = 1
				engine.timerActive = false

				if(nowCourse==Course.HIDE||nowCourse==Course.VOID) {
					// HELL-X ending & VOID ending
					engine.staffrollEnable = true
					engine.statistics.rollclear = 1
				} else {
					engine.gameEnded()
					engine.statistics.rollclear = if(engine.lives>=nowCourse.lives) 2 else 1
				}
			} else {
				nextLv += nowCourse.goalLevel
				if(owner.musMan.fadesw) {
					bgmChanged = true
					bgmLv++
					owner.musMan.bgm = nowCourse.bgmList[bgmLv]
					owner.musMan.fadesw = false
				}
				if(bgmChanged) engine.playSE("levelup_section")
				engine.playSE("levelup")
				engine.statistics.level++

				owner.bgMan.fadesw = true
				owner.bgMan.fadecount = 0
				owner.bgMan.fadebg = engine.statistics.level+nowCourse.bgOffset

				sectionscomp++

				engine.timerActive = false // Stop timer until the next piece becomes active
				setSpeed(engine)
			}
		return 0
	}

	/** Renders game result screen */
	override fun renderResult(engine:GameEngine) {
		if(!netIsWatch)
			receiver.drawMenuFont(
				engine, 0, 0, "\u0090\u0093 PAGE"+(engine.statc[1]+1)
					+"/3", EventReceiver.COLOR.RED
			)

		if(engine.statc[1]==0) {
			var gcolor = EventReceiver.COLOR.RED
			if(engine.statistics.rollclear==1) gcolor = EventReceiver.COLOR.GREEN
			if(engine.statistics.rollclear==2) gcolor = EventReceiver.COLOR.ORANGE

			receiver.drawMenuFont(engine, 0, 1, "LIFE REMAINED", EventReceiver.COLOR.BLUE, .8f)
			receiver.drawMenuNum(engine, 7, 1, String.format("%2d", engine.lives), gcolor, 2f)

			receiver.drawMenuNum(engine, 0, 2, String.format("%04d", norm), gcolor, 2f)
			receiver.drawMenuFont(engine, 6, 3, "Lines", EventReceiver.COLOR.BLUE, .8f)

			drawResultStats(
				engine, receiver, 4, EventReceiver.COLOR.BLUE, Statistic.LPM, Statistic.TIME, Statistic.PPS,
				Statistic.PIECE
			)
			drawResultRank(engine, receiver, 14, EventReceiver.COLOR.BLUE, rankingRank)
			drawResultNetRank(engine, receiver, 16, EventReceiver.COLOR.BLUE, netRankingRank[0])
			drawResultNetRankDaily(engine, receiver, 18, EventReceiver.COLOR.BLUE, netRankingRank[1])
		} else if(engine.statc[1]==1||engine.statc[1]==2) {
			receiver.drawMenuFont(engine, 0, 2, "SECTION", EventReceiver.COLOR.BLUE)

			var i = 0
			var x:Int
			while(i<10&&i<sectionTime.size-engine.statc[1]*10) {
				x = i+engine.statc[1]*10-10
				if(x>=0)
					if(sectionTime[x]>0)
						receiver.drawMenuNum(engine, 2, 3+i, sectionTime[x].toTimeStr)
				i++
			}
			if(sectionavgtime>0) {
				receiver.drawMenuFont(engine, 0, 14, "AVERAGE", EventReceiver.COLOR.BLUE)
				receiver.drawMenuFont(engine, 2, 15, sectionavgtime.toTimeStr)
			}
		}

		if(netIsPB) receiver.drawMenuFont(engine, 2, 20, "NEW PB", EventReceiver.COLOR.ORANGE)

		if(netIsNetPlay&&netReplaySendStatus==1)
			receiver.drawMenuFont(engine, 0, 21, "SENDING...", EventReceiver.COLOR.PINK)
		else if(netIsNetPlay&&!netIsWatch&&netReplaySendStatus==2)
			receiver.drawMenuFont(engine, 1, 21, "A: RETRY", EventReceiver.COLOR.RED)
	}

	/** Additional routine for game result screen */
	override fun onResult(engine:GameEngine):Boolean {
		if(goalType>=Course.HELL.ordinal&&engine.statistics.rollclear>=1) owner.musMan.bgm = BGM.Result(3)
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
		saveSetting(prop, engine)

		// NET: Save name
		if(netPlayerName!=null&&netPlayerName!!.isNotEmpty()) prop.setProperty(
			"${engine.playerID}.net.netPlayerName",
			netPlayerName
		)

		if(!owner.replayMode&&startLevel==0&&!big&&engine.ai==null) {
			updateRanking(engine.lives, norm, engine.statistics.time, goalType, engine.statistics.rollclear)

			if(rankingRank!=-1) return true
		}
		return false
	}

	/** Load the settings from [prop] */
	override fun loadSetting(prop:CustomProperties, ruleName:String, playerID:Int) {
		goalType = prop.getProperty("timeattack.gametype", 0)
		startLevel = prop.getProperty("timeattack.startLevel", 0)
		big = prop.getProperty("timeattack.big", false)
		showST = prop.getProperty("timeattack.showsectiontime", true)
		version = prop.getProperty("timeattack.version", 0)
	}

	/** Save the settings
	 * @param prop CustomProperties
	 */
	override fun saveSetting(prop:CustomProperties, ruleName:String, playerID:Int) {
		prop.setProperty("timeattack.gametype", goalType)
		prop.setProperty("timeattack.startLevel", startLevel)
		prop.setProperty("timeattack.big", big)
		prop.setProperty("timeattack.showsectiontime", showST)
		prop.setProperty("timeattack.version", version)
	}

	/** Update the ranking
	 * @param ln Lines
	 * @param time Time
	 * @param type Game type
	 * @param clear Game completed flag
	 */
	private fun updateRanking(lf:Int, ln:Int, time:Int, type:Int, clear:Int) {
		rankingRank = checkRanking(lf, ln, time, type, clear)

		if(rankingRank!=-1) {
			for(i in RANKING_MAX-1 downTo rankingRank+1) {
				rankingLifes[type][i] = rankingLifes[type][i-1]
				rankingLines[type][i] = rankingLines[type][i-1]
				rankingTime[type][i] = rankingTime[type][i-1]
				rankingRollclear[type][i] = rankingRollclear[type][i-1]
			}

			rankingLifes[type][rankingRank] = lf
			rankingLines[type][rankingRank] = ln
			rankingTime[type][rankingRank] = time
			rankingRollclear[type][rankingRank] = clear
		}
	}

	/** This function will check the ranking and returns which place you are.
	 * (-1: Out of rank)
	 * @param lf Lifes
	 * @param ln Lines
	 * @param time Time
	 * @param type Game type
	 * @param clear Game completed flag
	 * @return Place (First place is 0. -1 is Out of Rank)
	 */
	private fun checkRanking(lf:Int, ln:Int, time:Int, type:Int, clear:Int):Int {
		for(i in 0 until RANKING_MAX)
			if(clear>rankingRollclear[type][i]) return i
			else if(clear==rankingRollclear[type][i]&&ln>rankingLines[type][i]) return i
			else if(clear==rankingRollclear[type][i]&&ln==rankingLines[type][i]&&lf>rankingLifes[type][i]) return i
			else if(clear==rankingRollclear[type][i]&&ln==rankingLines[type][i]&&lf==rankingLifes[type][i]&&time<rankingTime[type][i])
				return i

		return -1
	}

	/** NET: Send various in-game stats of [engine] */
	override fun netSendStats(engine:GameEngine) {
		val bg = if(engine.owner.bgMan.fadesw)
			engine.owner.bgMan.fadebg
		else
			engine.owner.bgMan.bg
		var msg = "game\tstats\t"
		msg += "${engine.statistics.lines}\t${engine.statistics.totalPieceLocked}\t"
		msg += "${engine.statistics.time}\t${engine.statistics.lpm}\t"
		msg += "${engine.statistics.pps}\t$goalType\t"
		msg += "${engine.gameActive}\t${engine.timerActive}\t"
		msg += "${engine.statistics.level}\t$levelTimer\t$levelTimerMax\t"
		msg += "$rolltime${"\t$norm\t$bg\t${engine.meterValue}\t"+engine.meterColor}\t"
		msg += "${engine.heboHiddenEnable}\t${engine.heboHiddenTimerNow}\t${engine.heboHiddenTimerMax}\t"
		msg += "${engine.heboHiddenYNow}\t${engine.heboHiddenYLimit}\n${engine.lives}\n"
		netLobby!!.netPlayerClient!!.send(msg)
	}

	/** NET: Parse Received [message] as in-game stats of [engine] */
	override fun netRecvStats(engine:GameEngine, message:Array<String>) {
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
		rolltime = message[15].toInt()
		norm = message[16].toInt()
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
		val subMsg = StringBuilder()
		subMsg.append("NORM;").append(norm).append("\t")
		subMsg.append("LEVEL;").append(engine.statistics.level+engine.statistics.levelDispAdd).append("\t")
		subMsg.append("TIME;").append(engine.statistics.time.toTimeStr).append("\t")
		subMsg.append("PIECE;").append(engine.statistics.totalPieceLocked).append("\t")
		subMsg.append("LINE/MIN;").append(engine.statistics.lpm).append("\t")
		subMsg.append("PIECE/SEC;").append(engine.statistics.pps).append("\t")
		subMsg.append("SECTION AVERAGE;").append(sectionavgtime.toTimeStr).append("\t")
		for(i in sectionTime.indices)
			if(sectionTime[i]>0)
				subMsg.append("SECTION ").append(
					i+1
				).append(";").append(sectionTime[i].toTimeStr).append("\t")

		val msg = "gstat1p\t${NetUtil.urlEncode("$subMsg")}\n"
		netLobby?.netPlayerClient?.send(msg)
	}

	/** NET: Send game options to all spectators
	 * @param engine GameEngine
	 */
	override fun netSendOptions(engine:GameEngine) {
		val msg = "game\toption\t"+
			"$goalType\t$startLevel\t$showST\t$big\n"
		netLobby?.netPlayerClient?.send(msg)
	}

	/** NET: Receive game options */
	override fun netRecvOptions(engine:GameEngine, message:Array<String>) {
		goalType = message[4].toInt()
		startLevel = message[5].toInt()
		showST = message[6].toBoolean()
		big = message[7].toBoolean()
	}

	/** NET: Get goal type */
	override fun netGetGoalType():Int = goalType

	/** NET: It returns true when the current settings don't prevent
	 * leaderboard screen from showing. */
	override fun netIsNetRankingViewOK(engine:GameEngine):Boolean = startLevel==0&&!big&&engine.ai==null

	companion object {
		/** Current version of this mode */
		private const val CURRENT_VERSION = 1
		private val courses = Course.values()
		/** Game types */
		enum class Course {
			EASY, HARD, HARDEST, SUPER, XTREME, LONG, SURVIVAL, CHALLENGE, HELL, HIDE, VOID;

			/** Gravity tables */
			val gravity by lazy {
				when(this) {
					EASY -> intArrayOf(4, 12, 48, 72, 96, 128, 256, 384, 512, 768, 1024, 1280, -1)
					HARD -> intArrayOf(84, 128, 256, 512, 768, 1024, 1280, -1)
					LONG -> intArrayOf(4, 12, 48, 72, 96, 128, 256, 384, 512, 768, 1024, 1280, -1)
					CHALLENGE -> intArrayOf(1, 3, 15, 30, 60, 120, 180, 240, 300, 300, -1)
					else -> intArrayOf(-1)
				}
			}

			/** Denominator table */
			val denominator by lazy {intArrayOf(256)}

			/** Max level table */
			val goalLevel by lazy {
				when(this) {
					LONG -> 20
					SURVIVAL -> 20
					CHALLENGE -> 25
					HELL -> 30
					HIDE -> 30
					VOID -> 30
					else -> 15
				}
			}

			@Suppress("UNUSED_PARAMETER") fun goalLines(lv:Int) = 10
			val speeds by lazy {
				// Other speed values
				when(this) {
					EASY, HARD, HARDEST -> LevelData(gravity, denominator, 25, 25, 41, 30, 15)
					SUPER, SURVIVAL ->
						LevelData(
							intArrayOf(19, 18, 17, 16, 15, 14, 13, 12, 11, 10), // ARE
							intArrayOf(30, 29, 28, 27, 26, 25, 24, 23, 22, 21), // Line delay
							intArrayOf(30, 29, 28, 27, 26, 25, 24, 23, 22, 21), // Lock delay
							intArrayOf(10, 10, 9, 9, 8, 8, 8, 7, 7, 7) // DAS
						)
					XTREME -> LevelData(6, 6, 4, 20, 7)//(6,6,4,13,7)
					LONG ->
						/** Speed table for BASIC */
						LevelData(
							gravity, denominator,
							intArrayOf(25, 25, 25, 25, 25, 24, 23, 22, 21, 20, 19, 18, 17, 16, 15, 14, 13, 12, 11, 10), // ARE
							intArrayOf(25, 24, 23, 22, 21, 20, 19, 18, 17, 16, 15, 14, 13, 12, 11, 10, 9, 8, 7, 6), // Line delay
							intArrayOf(30, 30, 30, 30, 29, 29, 29, 29, 28, 28, 28, 27, 27, 27, 26, 26, 25, 25, 24, 24), // Lock delay
							intArrayOf(15, 15, 15, 15, 15, 14, 14, 14, 14, 13, 13, 13, 12, 12, 11, 10, 9, 8, 7, 6) // DAS
						)
					CHALLENGE ->
						LevelData(
							gravity, denominator,
							intArrayOf(26, 25, 24, 23, 22, 21, 20, 19, 18, 17, 16, 15, 14, 13, 12, 11, 10, 9, 8, 7), // ARE
							intArrayOf(40, 36, 33, 30, 27, 24, 21, 19, 17, 15, 13, 11, 9, 8, 7, 6, 5, 4, 3, 3), // Line delay
							intArrayOf(28, 28, 28, 27, 27, 27, 26, 26, 26, 25, 25, 25, 24, 24, 23, 22, 22, 21, 21, 20), // Lock delay
							intArrayOf(15, 15, 15, 15, 15, 14, 14, 14, 14, 13, 13, 13, 12, 12, 11, 10, 9, 8, 7, 6) // DAS
						)
					HELL, HIDE -> LevelData(2, 2, 3, 22, 7)
					VOID -> LevelData(
						intArrayOf(16, 15, 14, 13, 12, 11, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1), // ARE
						intArrayOf(8, 7, 7, 6, 6, 5, 5, 4, 4, 3, 3, 2, 2, 1, 1, 0), // Line delay
						intArrayOf(25, 24, 24, 23, 23, 22, 22, 21, 21, 20, 20, 19, 18, 17, 16, 15), // Lock delay
						intArrayOf(9, 8, 8, 7, 7, 6, 6, 5, 5, 4, 4, 4, 4, 4, 4, 4) // DAS
					)
				}
			}
			/** Max Life table */
			val lives by lazy {
				when(this) {
					SUPER -> 3
					XTREME -> 3
					LONG -> 4
					SURVIVAL -> 4
					HELL -> 9
					HIDE -> 9
					VOID -> 9
					else -> 2
				}
			}

			/** Level timer tables */
			val levelTimer by lazy {
				when(this) {
					EASY -> intArrayOf(6400, 6250, 6000, 5750, 5500, 5250, 5000, 4750, 4500, 4250, 4000, 3750, 3500, 3250, 3000)
					HARD -> intArrayOf(4500, 4200, 4100, 3900, 3700, 3500, 3300, 3100, 2900, 2700, 2500, 2350, 2200, 2100, 2000)
					HARDEST -> intArrayOf(4000, 3900, 3800, 3700, 3600, 3500, 3400, 3300, 3200, 3100, 3000, 2900, 2800, 2700, 2500)
					SUPER -> intArrayOf(3600, 3500, 3400, 3300, 3200, 3100, 3000, 2900, 2800, 2700, 2550, 2400, 2250, 2100, 2000)
					LONG -> intArrayOf(
						6400, 6200, 6000, 5800, 5600, 5400, 5200, 5000, 4800, 4600, // NORMAL 000-100
						4300, 4000, 3800, 3600, 3500, 3400, 3300, 3200, 3100, 3000
					) // NORMAL 100-200
					SURVIVAL -> intArrayOf(
						4000, 3890, 3780, 3670, 3560, 3450, 3340, 3230, 3120, 3010, // ANOTHER 000-100
						2900, 2800, 2700, 2600, 2500, 2400, 2300, 2200, 2100, 2000
					) // ANOTHER 100-200
					CHALLENGE -> intArrayOf(
						4000, 3890, 3780, 3670, 3560, 3450, 3340, 3230, 3120, 3010, // BASIC 000-100
						2900, 2800, 2700, 2600, 2500, 2400, 2300, 2200, 2100, 2000
					) // BASIC 100-200
					else -> intArrayOf(3000, 2900, 2800, 2700, 2600, 2500, 2400, 2300, 2200, 2100, 2000, 2000, 2000, 2000, 2000)
				}
			}

			/** BGM table */
			val bgmList by lazy {
				when(this) {
					EASY -> arrayOf(BGM.GrandA(0), BGM.GrandM(0), BGM.Extra(1))
					HARD -> arrayOf(BGM.GrandT(0), BGM.Extra(0), BGM.GrandA(1))
					HARDEST -> arrayOf(BGM.GrandM(1), BGM.GrandA(1), BGM.GrandT(1))
					SUPER -> arrayOf(BGM.GrandT(2), BGM.GrandA(2), BGM.GrandA(3))
					XTREME -> arrayOf(BGM.GrandT(3), BGM.GrandT(4), BGM.GrandT(5))
					LONG -> arrayOf(BGM.Extra(1), BGM.GrandA(0), BGM.GrandT(0), BGM.Extra(0), BGM.GrandT(2))
					SURVIVAL -> arrayOf(BGM.GrandT(2), BGM.GrandA(2), BGM.GrandT(3), BGM.GrandA(3), BGM.GrandT(4))
					CHALLENGE -> arrayOf(BGM.Extra(2), BGM.GrandA(0), BGM.GrandM(1), BGM.GrandT(2), BGM.GrandA(3))
					HELL -> arrayOf(BGM.Finale(2))
					HIDE -> arrayOf(BGM.Finale(0))
					VOID -> arrayOf(BGM.Finale(1))
					else -> arrayOf(BGM.Silent)
				}
			}
			/** BGM change lines table */
			val bgmChange by lazy {
				when(this) {
					EASY -> intArrayOf(5, 10)
					HARD -> intArrayOf(5, 10)
					HARDEST -> intArrayOf(5, 10)
					SUPER -> intArrayOf(5, 10)
					XTREME -> intArrayOf(5, 10)
					LONG -> intArrayOf(5, 10, 15)
					SURVIVAL -> intArrayOf(5, 10, 15)
					CHALLENGE -> intArrayOf(5, 10, 15, 20)
					else -> intArrayOf()
				}
			}

			val bgOffset by lazy {
				when(this) {
					EASY -> 0
					HARD -> 3
					HARDEST -> 15
					SUPER -> 14
					XTREME -> 15
					LONG -> 0
					SURVIVAL -> 10
					CHALLENGE -> 3
					HELL -> 10
					HIDE -> 10
					VOID -> 10
				}
			}

			/** Game type names (short) */
			val showName by lazy {
				when(this) {
					EASY -> "Easy"
					HARD -> "Hard"
					HARDEST -> "20G"
					SUPER -> "Super Hard"
					XTREME -> "eXtreme"
					LONG -> "LONG:Normal"
					SURVIVAL -> "Survival"
					CHALLENGE -> "Challenge"
					HELL -> "HELL SPEED"
					HIDE -> "HIDDEN HELL"
					VOID -> "Void&Speed"
				}
			}
		}

		/** Number of Course & ranking typ s */
		private val COURSE_MAX = courses.size

		/** HELL-X fade table */
		private val tableHellXFade =
			intArrayOf(
				600, 550, 500, 450, 400, 350, 300, 270, 240, 210,
				190, 170, 160, 150, 140, 130, 125, 120, 115, 110,
				100, 90, 80, 70, 60, 58, 56, 54, 52, 50
			)

		/** Ending time limit */
		private const val ROLLTIMELIMIT = 3238

		/** Number of ranking records */
		private const val RANKING_MAX = 13

	}
}
