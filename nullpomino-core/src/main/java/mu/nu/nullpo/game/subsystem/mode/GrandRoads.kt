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

import mu.nu.nullpo.game.component.BGMStatus
import mu.nu.nullpo.game.component.Controller
import mu.nu.nullpo.game.event.EventReceiver
import mu.nu.nullpo.game.net.NetUtil
import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.util.CustomProperties
import mu.nu.nullpo.util.GeneralUtil

/** TIME ATTACK mode (Original from NullpoUE build 010210 by Zircean.
 * This mode is heavily modified from the original.) */
class GrandRoads:NetDummyMode() {

	/** Remaining level time */
	private var levelTimer:Int = 0
	private var lastlinetime:Int = 0

	/** Original level time */
	private var levelTimerMax:Int = 0

	/** Current lines (for levelup) */
	private var norm:Int = 0

	/** Current BGM number */
	private var bgmlv:Int = 0

	/** Elapsed ending time */
	private var rolltime:Int = 0

	/** Ending started flag */
	private var rollstarted:Boolean = false

	/** Section time */
	private var sectionTime:IntArray = IntArray(0)

	/** Number of sections completed */
	private var sectionscomp:Int = 0

	/** Average section time */
	private var sectionavgtime:Int = 0

	/** Game type */
	private var goaltype:Int = 0

	/** Selected starting level */
	private var startlevel:Int = 0

	/** Big mode on/off */
	private var big:Boolean = false

	/** Show section time */
	private var showsectiontime:Boolean = false

	/** Version of this mode */
	private var version:Int = 0

	/** Your place on leaderboard (-1: out of rank) */
	private var rankingRank:Int = 0

	/** Ranking ecords */
	private var rankingLines:Array<IntArray> = Array(RANKING_TYPE) {IntArray(RANKING_MAX)}
	private var rankingLifes:Array<IntArray> = Array(RANKING_TYPE) {IntArray(RANKING_MAX)}
	private var rankingTime:Array<IntArray> = Array(RANKING_TYPE) {IntArray(RANKING_MAX)}
	private var rankingRollclear:Array<IntArray> = Array(RANKING_TYPE) {IntArray(RANKING_MAX)}

	/** Returns the name of this mode */
	override val name:String
		get() = "GRAND ROAD"

	/** This function will be called when the game enters the main game
	 * screen. */
	override fun playerInit(engine:GameEngine, playerID:Int) {
		super.playerInit(engine, playerID)
		menuTime = 0
		norm = 0
		goaltype = 0
		startlevel = 0
		rolltime = 0
		lastlinetime = 0
		rollstarted = false
		var m = 0
		for(i in 0 until tableGoalLevel.size-1)
			m = maxOf(m, tableGoalLevel[i])
		sectionTime = IntArray(m)
		sectionscomp = 0
		sectionavgtime = 0
		big = false
		showsectiontime = true

		rankingRank = -1
		rankingLines = Array(RANKING_TYPE) {IntArray(RANKING_MAX)}
		rankingLifes = Array(RANKING_TYPE) {IntArray(RANKING_MAX)}
		rankingTime = Array(RANKING_TYPE) {IntArray(RANKING_MAX)}
		rankingRollclear = Array(RANKING_TYPE) {IntArray(RANKING_MAX)}

		engine.tspinEnable = false
		engine.b2bEnable = false
		engine.comboType = GameEngine.COMBO_TYPE_DISABLE
		engine.framecolor = GameEngine.FRAME_COLOR_WHITE
		engine.bighalf = true
		engine.bigmove = true
		engine.staffrollEnable = false
		engine.staffrollNoDeath = false

		netPlayerInit(engine, playerID)

		if(!owner.replayMode) {
			loadSetting(owner.modeConfig)
			loadRanking(owner.modeConfig, engine.ruleopt.strRuleName)
			version = CURRENT_VERSION
		} else {
			loadSetting(owner.replayProp)

			// NET: Load name
			netPlayerName = engine.owner.replayProp.getProperty(playerID.toString()+".net.netPlayerName", "")
		}

		engine.owner.backgroundStatus.bg = startlevel
	}

	/** Set the gravity speed and some other things
	 * @param engine GameEngine object
	 */
	private fun setSpeed(engine:GameEngine) {
		var gravlv:Int
		var speedlv:Int
		var timelv:Int
		var lv = engine.statistics.level
		if(lv<0) lv = 0
		// Gravity speed
		timelv = lv
		gravlv = timelv
		speedlv = gravlv
		if(gravlv>=tableGravity[goaltype].size) gravlv = tableGravity[goaltype].size-1
		engine.speed.gravity = tableGravity[goaltype][gravlv]
		engine.speed.denominator = tableDenominator[goaltype]

		// Other speed values
		when(goaltype) {
			GAMETYPE_NORMAL, GAMETYPE_HIGHSPEED1, GAMETYPE_HIGHSPEED2 -> {
				engine.speed.are = 25
				engine.speed.areLine = 25
				engine.speed.lineDelay = 41
				engine.speed.lockDelay = 30
				engine.speed.das = 15
			}
			GAMETYPE_ANOTHER, GAMETYPE_ANOTHER200 -> {
				if(speedlv>=tableAnother[0].size) speedlv = tableAnother[0].size-1
				engine.speed.are = tableAnother[0][speedlv]
				engine.speed.areLine = tableAnother[0][speedlv]
				engine.speed.lineDelay = tableAnother[1][speedlv]
				engine.speed.lockDelay = tableAnother[2][speedlv]
				engine.speed.das = tableAnother[3][speedlv]
			}
			GAMETYPE_ANOTHER2 -> {
				engine.speed.are = 6
				engine.speed.areLine = 6
				engine.speed.lineDelay = 4
				engine.speed.lockDelay = 13
				engine.speed.das = 7
			}
			GAMETYPE_NORMAL200 -> {
				if(speedlv>=tableNormal200[0].size) speedlv = tableNormal200[0].size-1
				engine.speed.are = tableNormal200[0][speedlv]
				engine.speed.areLine = tableNormal200[0][speedlv]
				engine.speed.lineDelay = tableNormal200[1][speedlv]
				engine.speed.lockDelay = tableNormal200[2][speedlv]
				engine.speed.das = tableNormal200[3][speedlv]
			}
			GAMETYPE_BASIC -> {
				if(speedlv>=tableBasic[0].size) speedlv = tableBasic[0].size-1
				engine.speed.are = tableBasic[0][speedlv]
				engine.speed.areLine = tableBasic[0][speedlv]
				engine.speed.lineDelay = tableBasic[1][speedlv]
				engine.speed.lockDelay = tableBasic[2][speedlv]
				engine.speed.das = tableBasic[3][speedlv]
			}
			GAMETYPE_HELL -> {
				engine.speed.are = 2
				engine.speed.areLine = 2
				engine.speed.lineDelay = 3
				engine.speed.lockDelay = 20
				engine.speed.das = 7
			}
			GAMETYPE_HELLX -> {
				engine.speed.are = 2
				engine.speed.areLine = 2
				engine.speed.lineDelay = 3
				engine.speed.lockDelay = 22
				engine.speed.das = 7
			}
			GAMETYPE_VOID -> {
				if(speedlv>=tableVoid[0].size) speedlv = tableVoid[0].size-1
				engine.speed.are = tableVoid[0][speedlv]
				engine.speed.areLine = tableVoid[0][speedlv]
				engine.speed.lineDelay = tableVoid[1][speedlv]
				engine.speed.lockDelay = tableVoid[2][speedlv]
				engine.speed.das = tableVoid[3][speedlv]
			}
		}

		// Show outline only
		if(goaltype==GAMETYPE_HELL||goaltype==GAMETYPE_HELLX) engine.blockShowOutlineOnly = true
		// Bone blocks
		if(goaltype==GAMETYPE_HELLX&&engine.statistics.level>=20||goaltype==GAMETYPE_VOID)
			engine.bone = true

		// for test
		/* engine.speed.are = 25; engine.speed.areLine = 25;
 * engine.speed.lineDelay = 10; engine.speed.lockDelay = 30;
 * engine.speed.das = 12; levelTimerMax = levelTimer = 3600 * 3; */

		if(timelv>=tableLevelTimer[goaltype].size) timelv = tableLevelTimer[goaltype].size-1
		levelTimer = tableLevelTimer[goaltype][timelv]
		levelTimerMax = levelTimer
		// Block fade for HELL-X
		if(goaltype==GAMETYPE_HELLX) {
			var fadelv = engine.statistics.level
			if(fadelv<0) fadelv = 0
			if(fadelv>=tableHellXFade.size) fadelv = tableHellXFade.size-1
			engine.blockHidden = tableHellXFade[fadelv]
		}

		lastlinetime = levelTimer
	}

	/** Set Pressure Hidden params
	 * @param engine GameEngine
	 */
	private fun setHeboHidden(engine:GameEngine) {
		if(goaltype==GAMETYPE_HELLX&&engine.statistics.level>=15||goaltype==GAMETYPE_VOID) {
			engine.heboHiddenEnable = true
			val section = if(goaltype==GAMETYPE_VOID) engine.statistics.level/5 else (engine.statistics.level-15)/2
			if(section==1) {
				engine.heboHiddenYLimit = 15
				engine.heboHiddenTimerMax = (engine.heboHiddenYNow+2)*120
			}
			if(section==2) {
				engine.heboHiddenYLimit = 17
				engine.heboHiddenTimerMax = (engine.heboHiddenYNow+1)*90
			}
			if(section==3) {
				engine.heboHiddenYLimit = 19
				engine.heboHiddenTimerMax = engine.heboHiddenYNow*60+60
			}
			if(section==4) {
				engine.heboHiddenYLimit = 19
				engine.heboHiddenTimerMax = engine.heboHiddenYNow*45+45
			}
			if(section==5) {
				engine.heboHiddenYLimit = 19
				engine.heboHiddenTimerMax = engine.heboHiddenYNow*30+30
			}
			if(section==6) {
				engine.heboHiddenYLimit = 19
				engine.heboHiddenTimerMax = engine.heboHiddenYNow*7+15
			}
			if(section==7) {
				engine.heboHiddenYLimit = 20
				engine.heboHiddenTimerMax = engine.heboHiddenYNow*3+15
			}
		} else
			engine.heboHiddenEnable = false
	}

	/** Set the starting bgmlv
	 * @param engine GameEngine
	 */
	private fun setStartBgmlv(engine:GameEngine) {
		bgmlv = 0
		while(bgmlv<tableBGMChange[goaltype].size&&norm>=tableBGMChange[goaltype][bgmlv])
			bgmlv++
	}

	/** Main routine for game setup screen */
	override fun onSetting(engine:GameEngine, playerID:Int):Boolean {
		// NET: Net Ranking
		if(netIsNetRankingDisplayMode)
			netOnUpdateNetPlayRanking(engine, netGetGoalType())
		else if(!engine.owner.replayMode) {
			// Configuration changes
			val change = updateCursor(engine, 3)

			if(change!=0) {
				receiver.playSE("change")

				when(menuCursor) {
					0 -> {
						goaltype += change
						if(goaltype<0) goaltype = GAMETYPE_MAX-1
						if(goaltype>GAMETYPE_MAX-1) goaltype = 0
						if(startlevel>tableGoalLevel[goaltype]-1) startlevel = tableGoalLevel[goaltype]-1
						engine.owner.backgroundStatus.bg = startlevel
					}
					1 -> {
						startlevel += change
						if(startlevel<0) startlevel = tableGoalLevel[goaltype]-1
						if(startlevel>tableGoalLevel[goaltype]-1) startlevel = 0
						engine.owner.backgroundStatus.bg = startlevel
					}
					2 -> showsectiontime = !showsectiontime
					3 -> big = !big
				}

				// NET: Signal options change
				if(netIsNetPlay&&netNumSpectators>0) netSendOptions(engine)
			}

			// Check for A button, when pressed this will begin the game
			if(engine.ctrl!!.isPush(Controller.BUTTON_A)&&menuTime>=5) {
				receiver.playSE("decide")
				saveSetting(owner.modeConfig)
				receiver.saveModeConfig(owner.modeConfig)

				// NET: Signal start of the game
				if(netIsNetPlay) netLobby!!.netPlayerClient!!.send("start1p\n")

				return false
			}

			// Check for B button, when pressed this will shutdown the game engine.
			if(engine.ctrl!!.isPush(Controller.BUTTON_B)&&!netIsNetPlay) engine.quitflag = true

			// NET: Netplay Ranking
			if(engine.ctrl!!.isPush(Controller.BUTTON_D)&&netIsNetPlay&&!big
				&&engine.ai==null)
				netEnterNetPlayRankingScreen(engine, playerID, goaltype)

			menuTime++
		} else {
			menuTime++
			menuCursor = -1

			return menuTime<60
		}// Menu

		return true
	}

	/** Renders game setup screen */
	override fun renderSetting(engine:GameEngine, playerID:Int) {
		if(netIsNetRankingDisplayMode)
		// NET: Netplay Ranking
			netOnRenderNetPlayRanking(engine, playerID, receiver)
		else
			drawMenu(engine, playerID, receiver, 0, EventReceiver.COLOR.BLUE, 0,
				"DIFFICULTY", GAMETYPE_NAME[goaltype],
				"LEVEL", (startlevel+1).toString(),
				"SHOW STIME", GeneralUtil.getONorOFF(showsectiontime),
				"BIG", GeneralUtil.getONorOFF(big))
	}

	/** Ready screen */
	override fun onReady(engine:GameEngine, playerID:Int):Boolean {
		if(engine.statc[0]==0) {
			engine.statistics.level = startlevel
			engine.statistics.levelDispAdd = 1
			engine.big = big
			norm = startlevel*10
			setSpeed(engine)
			setStartBgmlv(engine)
		}

		return false
	}

	/** This function will be called before the game actually begins
	 * (after Ready&Go screen disappears) */
	override fun startGame(engine:GameEngine, playerID:Int) {
		if(netIsWatch)
			owner.bgmStatus.bgm = BGMStatus.BGM.SILENT
		else
			owner.bgmStatus.bgm = tableBGM[goaltype][bgmlv]
		engine.lives = tableLives[goaltype]
	}

	/** Renders HUD (leaderboard or game statistics) */
	override fun renderLast(engine:GameEngine, playerID:Int) {
		if(owner.menuOnly) return

		receiver.drawScoreFont(engine, playerID, 0, -1, "GRAND ROADS", EventReceiver.COLOR.PURPLE)
		receiver.drawScoreFont(engine, playerID, 0, 0, "TIME ATTACK", EventReceiver.COLOR.PURPLE, .75f)
		receiver.drawScoreFont(engine, playerID, 0, 1, GAMETYPE_NAME_LONG[goaltype]+" COURSE", EventReceiver.COLOR.PURPLE)
		//rereceiver.drawScore(engine, playerID, -1, -4*2, "DECORATION", scale = .5f);
		//receiver.drawScoreDecorations(engine, playerID,0,-3,100,decoration);
		//receiver.drawScoreDecorations(engine, playerID,5,-4,100,dectemp);
		if(engine.stat==GameEngine.Status.SETTING||engine.stat==GameEngine.Status.RESULT&&!owner.replayMode) {
			if(!owner.replayMode&&startlevel==0&&!big&&engine.ai==null&&!netIsWatch) {
				receiver.drawScoreFont(engine, playerID, 8, 3, "TIME", EventReceiver.COLOR.BLUE)

				for(i in 0 until RANKING_MAX) {
					var gcolor = EventReceiver.COLOR.WHITE
					if(rankingRollclear[goaltype][i]==1) gcolor = EventReceiver.COLOR.GREEN
					if(rankingRollclear[goaltype][i]==2) gcolor = EventReceiver.COLOR.ORANGE
					receiver.drawScoreNum(engine, playerID, 0, 4+i, String.format("%2d", i+1), if(i==rankingRank)
						EventReceiver.COLOR.RED
					else
						EventReceiver.COLOR.YELLOW)

					receiver.drawScoreNum(engine, playerID, 8, 4+i, GeneralUtil.getTime(rankingTime[goaltype][i].toFloat()), gcolor)

					receiver.drawScoreNano(engine, playerID, 11, 8+i*2, if(gcolor==EventReceiver.COLOR.WHITE)
						"LINES\nCLEARED"
					else
						"LIFES\nREMAINED", gcolor, .5f)
					receiver.drawScoreNum(engine, playerID, 3, 4+i, String.format("%3d", if(gcolor==EventReceiver.COLOR.WHITE)
						rankingLines[goaltype][i]
					else
						rankingLifes[goaltype][i]), gcolor)
				}
			}
		} else {
			receiver.drawScoreFont(engine, playerID, 0, 3, "LEVEL", EventReceiver.COLOR.BLUE)
			receiver.drawScoreNum(engine, playerID, 5, 2, String.format("%02d", engine.statistics.level+1), 2f)
			receiver.drawScoreNum(engine, playerID, 8, 3, String.format("/%3d", tableGoalLevel[goaltype]))
			val strLevel = String.format("%3d/%3d", norm, (engine.statistics.level+1)*10)
			receiver.drawScoreNum(engine, playerID, 0, 4, strLevel)

			receiver.drawSpeedMeter(engine, playerID, 0, 5, if(engine.speed.gravity<0)
				40
			else
				Math.floor(Math.log(engine.speed.gravity.toDouble())).toInt()*(engine.speed.denominator/60))

			receiver.drawScoreFont(engine, playerID, 0, 7, "TIME LIMIT", EventReceiver.COLOR.BLUE)
			receiver.drawScoreNum(engine, playerID, 0, 8, GeneralUtil.getTime(levelTimer.toFloat()), levelTimer in 1..599&&levelTimer%4==0, 2f)

			receiver.drawScoreFont(engine, playerID, 0, 10, "TOTAL TIME", EventReceiver.COLOR.BLUE)
			receiver.drawScoreNum(engine, playerID, 0, 11, GeneralUtil.getTime(engine.statistics.time.toFloat()), 2f)

			// Remaining ending time
			if(engine.gameActive&&engine.ending==2&&engine.staffrollEnable) {
				var time = ROLLTIMELIMIT-rolltime
				if(time<0) time = 0
				receiver.drawScoreFont(engine, playerID, 0, 17, "ROLL TIME", EventReceiver.COLOR.BLUE)
				receiver.drawScoreNum(engine, playerID, 0, 18, GeneralUtil.getTime(time.toFloat()), time>0&&time<10*60, 2f)
			}

			// Section time
			if(showsectiontime&&sectionTime!=null&&!netIsWatch) {
				val x = if(receiver.nextDisplayType==2) 25 else 12
				val y = if(receiver.nextDisplayType==2) 4 else 2
				val scale = if(receiver.nextDisplayType==2) .5f else 1f

				receiver.drawScoreFont(engine, playerID, x, y, "SECTION TIME", EventReceiver.COLOR.BLUE, scale)

				val l = maxOf(0, engine.statistics.level-20)
				var i = l
				while(i<sectionTime.size) {
					if(sectionTime[i]>0) {
						var strSeparator = "-"
						if(i==engine.statistics.level&&engine.ending==0) strSeparator = "+"

						val strSectionTime:String
						strSectionTime = String.format("%2d%s%s", i+1, strSeparator, GeneralUtil.getTime(sectionTime[i].toFloat()))
						receiver.drawScoreNum(engine, playerID, x+1, y+1+i-l, strSectionTime, scale)
					}
					i++
				}
				receiver.drawScoreFont(engine, playerID, 0, 13, "AVERAGE", EventReceiver.COLOR.BLUE)
				receiver.drawScoreNum(engine, playerID, 0, 14, GeneralUtil.getTime((engine.statistics.time/(sectionscomp+1)).toFloat()), 2f)
			}
		}
		super.renderLast(engine, playerID)
	}

	/** This function will be called when the piece is active */
	override fun onMove(engine:GameEngine, playerID:Int):Boolean {
		// Enable timer again after the levelup
		if(engine.ending==0&&engine.statc[0]==0&&!engine.timerActive&&!engine.holdDisable)
			engine.timerActive = true

		// Ending start
		if(engine.ending==2&&engine.staffrollEnable&&!rollstarted&&!netIsWatch) {
			rollstarted = true
			owner.bgmStatus.bgm = BGMStatus.BGM.FINALE_3
			owner.bgmStatus.fadesw = false

			// VOID ending
			if(goaltype==GAMETYPE_VOID) {
				engine.blockHidden = engine.ruleopt.lockflash
				engine.blockHiddenAnim = false
				engine.blockOutlineType = GameEngine.BLOCK_OUTLINE_NONE
			}
		}

		return super.onMove(engine, playerID)
	}

	/** This function will be called when the game timer updates */
	override fun onLast(engine:GameEngine, playerID:Int) {
		// Level timer
		if(engine.timerActive&&engine.ending==0)
			if(levelTimer>0) {
				levelTimer--
				if(levelTimer<=600&&levelTimer%60==0) receiver.playSE("countdown")
			} else if(!netIsWatch) {
				engine.lives = 0
				engine.gameEnded()
				engine.resetStatc()
				engine.stat = GameEngine.Status.GAMEOVER
			}

		// Update meter
		if(engine.ending==0&&levelTimerMax!=0) {
			engine.meterValue = receiver.getMeterMax(engine)*levelTimer/2/levelTimerMax
			if(norm%10>0)
				engine.meterValue += ((receiver.getMeterMax(engine)-engine.meterValue)*(norm%10)*levelTimer
					/lastlinetime/10)
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
			engine.meterValue = remainRollTime*receiver.getMeterMax(engine)/ROLLTIMELIMIT
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

	/** Calculates line-clear score
	 * (This function will be called even if no lines are cleared) */
	override fun calcScore(engine:GameEngine, playerID:Int, lines:Int) {
		// Don't do anything during the ending
		if(engine.ending!=0) return

		// Add lines to norm
		norm += lines
		if(lines>0) lastlinetime = levelTimer
		// Decrease Pressure Hidden
		if(engine.heboHiddenEnable&&lines>0) {
			engine.heboHiddenTimerNow = 0
			engine.heboHiddenYNow -= lines
			if(engine.heboHiddenYNow<0) engine.heboHiddenYNow = 0
		}

		// BGM change
		if(bgmlv<tableBGMChange[goaltype].size&&norm>=tableBGMChange[goaltype][bgmlv]) {
			bgmlv++
			owner.bgmStatus.bgm = tableBGM[goaltype][bgmlv]
			owner.bgmStatus.fadesw = false
		} else if(bgmlv<tableBGMFadeout[goaltype].size&&norm>=tableBGMFadeout[goaltype][bgmlv])
			owner.bgmStatus.fadesw = true// BGM fadeout

		// Game completed
		if(norm>=tableGoalLevel[goaltype]*10) {
			receiver.playSE("levelup")

			// Update section time
			if(engine.timerActive) sectionscomp++

			norm = tableGoalLevel[goaltype]*10
			engine.ending = 1
			engine.timerActive = false

			if(goaltype==GAMETYPE_HELLX||goaltype==GAMETYPE_VOID) {
				// HELL-X ending & VOID ending
				engine.staffrollEnable = true
				engine.statistics.rollclear = 1
			} else {
				engine.gameEnded()
				engine.statistics.rollclear = if(engine.lives>=tableLives[goaltype]) 2 else 1
			}
		} else if(norm>=(engine.statistics.level+1)*10&&engine.statistics.level<tableGoalLevel[goaltype]-1) {
			receiver.playSE("levelup")
			engine.statistics.level++

			owner.backgroundStatus.fadesw = true
			owner.backgroundStatus.fadecount = 0
			owner.backgroundStatus.fadebg = engine.statistics.level+tableBGoffset[goaltype]

			sectionscomp++

			engine.timerActive = false // Stop timer until the next piece becomes active
			setSpeed(engine)
		}// Level up
	}

	/** Renders game result screen */
	override fun renderResult(engine:GameEngine, playerID:Int) {
		if(!netIsWatch)
			receiver.drawMenuFont(engine, playerID, 0, 0, "kn PAGE"+(engine.statc[1]+1)
				+"/3", EventReceiver.COLOR.RED)

		if(engine.statc[1]==0) {
			var gcolor = EventReceiver.COLOR.RED
			if(engine.statistics.rollclear==1) gcolor = EventReceiver.COLOR.GREEN
			if(engine.statistics.rollclear==2) gcolor = EventReceiver.COLOR.ORANGE

			receiver.drawMenuNum(engine, playerID, 7, 1, String.format("%2d", engine.lives), gcolor, 2f)
			receiver.drawMenuFont(engine, playerID, 0, 1, "LIFE REMAINED", EventReceiver.COLOR.BLUE, .8f)

			receiver.drawMenuNum(engine, playerID, 0, 2, String.format("%04d", norm), gcolor, 2f)
			receiver.drawMenuFont(engine, playerID, 6, 3, "LINES", EventReceiver.COLOR.BLUE, .8f)

			drawResultStats(engine, playerID, receiver, 4, EventReceiver.COLOR.BLUE, AbstractMode.Statistic.LPM, AbstractMode.Statistic.TIME, AbstractMode.Statistic.PPS, AbstractMode.Statistic.PIECE)
			drawResultRank(engine, playerID, receiver, 14, EventReceiver.COLOR.BLUE, rankingRank)
			drawResultNetRank(engine, playerID, receiver, 16, EventReceiver.COLOR.BLUE, netRankingRank[0])
			drawResultNetRankDaily(engine, playerID, receiver, 18, EventReceiver.COLOR.BLUE, netRankingRank[1])
		} else if(engine.statc[1]==1||engine.statc[1]==2) {
			receiver.drawMenuFont(engine, playerID, 0, 2, "SECTION", EventReceiver.COLOR.BLUE)

			var i = 0
			var x:Int
			while(i<10&&i<sectionTime.size-engine.statc[1]*10) {
				x = i+engine.statc[1]*10-10
				if(x>=0)
					if(sectionTime[x]>0)
						receiver.drawMenuNum(engine, playerID, 2, 3+i, GeneralUtil.getTime(sectionTime[x].toFloat()))
				i++
			}
			if(sectionavgtime>0) {
				receiver.drawMenuFont(engine, playerID, 0, 14, "AVERAGE", EventReceiver.COLOR.BLUE)
				receiver.drawMenuFont(engine, playerID, 2, 15, GeneralUtil.getTime(sectionavgtime.toFloat()))
			}
		}

		if(netIsPB) receiver.drawMenuFont(engine, playerID, 2, 20, "NEW PB", EventReceiver.COLOR.ORANGE)

		if(netIsNetPlay&&netReplaySendStatus==1)
			receiver.drawMenuFont(engine, playerID, 0, 21, "SENDING...", EventReceiver.COLOR.PINK)
		else if(netIsNetPlay&&!netIsWatch
			&&netReplaySendStatus==2)
			receiver.drawMenuFont(engine, playerID, 1, 21, "A: RETRY", EventReceiver.COLOR.RED)
	}

	/** Additional routine for game result screen */
	override fun onResult(engine:GameEngine, playerID:Int):Boolean {
		if(goaltype>=GAMETYPE_HELL&&engine.statistics.rollclear>=1) owner.bgmStatus.bgm = BGMStatus.BGM.CLEARED
		if(!netIsWatch) {
			if(engine.ctrl!!.isMenuRepeatKey(Controller.BUTTON_UP)) {
				engine.statc[1]--
				if(engine.statc[1]<0) engine.statc[1] = 2
				receiver.playSE("change")
			}
			if(engine.ctrl!!.isMenuRepeatKey(Controller.BUTTON_DOWN)) {
				engine.statc[1]++
				if(engine.statc[1]>2) engine.statc[1] = 0
				receiver.playSE("change")
			}
		}

		return super.onResult(engine, playerID)
	}

	/** This function will be called when the replay data is going to be
	 * saved */
	override fun saveReplay(engine:GameEngine, playerID:Int, prop:CustomProperties) {
		saveSetting(prop)

		// NET: Save name
		if(netPlayerName!=null&&netPlayerName!!.isNotEmpty()) prop.setProperty(playerID.toString()+".net.netPlayerName", netPlayerName)

		if(!owner.replayMode&&startlevel==0&&!big&&engine.ai==null) {
			updateRanking(engine.lives, norm, engine.statistics.time, goaltype, engine.statistics.rollclear)

			if(rankingRank!=-1) {
				saveRanking(owner.modeConfig, engine.ruleopt.strRuleName)
				receiver.saveModeConfig(owner.modeConfig)
			}
		}
	}

	/** Load the settings
	 * @param prop CustomProperties
	 */
	override fun loadSetting(prop:CustomProperties) {
		goaltype = prop.getProperty("timeattack.gametype", 0)
		startlevel = prop.getProperty("timeattack.startlevel", 0)
		big = prop.getProperty("timeattack.big", false)
		showsectiontime = prop.getProperty("timeattack.showsectiontime", true)
		version = prop.getProperty("timeattack.version", 0)
	}

	/** Save the settings
	 * @param prop CustomProperties
	 */
	override fun saveSetting(prop:CustomProperties) {
		prop.setProperty("timeattack.gametype", goaltype)
		prop.setProperty("timeattack.startlevel", startlevel)
		prop.setProperty("timeattack.big", big)
		prop.setProperty("timeattack.showsectiontime", showsectiontime)
		prop.setProperty("timeattack.version", version)
	}

	/** Load the ranking
	 * @param prop CustomProperties
	 * @param ruleName Rule name
	 */
	override fun loadRanking(prop:CustomProperties, ruleName:String) {
		for(i in 0 until RANKING_MAX)
			for(type in 0 until GAMETYPE_MAX) {
				rankingLines[type][i] = prop.getProperty("timeattack.ranking.$ruleName.$type.lines.$i", 0)
				rankingLifes[type][i] = prop.getProperty("timeattack.ranking.$ruleName.$type.lifes.$i", 0)
				rankingTime[type][i] = prop.getProperty("timeattack.ranking.$ruleName.$type.time.$i", 0)
				rankingRollclear[type][i] = prop.getProperty("timeattack.ranking.$ruleName.$type.rollclear.$i", 0)
			}
	}

	/** Save the ranking
	 * @param prop CustomProperties
	 * @param ruleName Rule name
	 */
	private fun saveRanking(prop:CustomProperties?, ruleName:String) {
		for(i in 0 until RANKING_MAX)
			for(type in 0 until GAMETYPE_MAX) {
				prop!!.setProperty("timeattack.ranking.$ruleName.$type.lines.$i", rankingLines[type][i])
				prop.setProperty("timeattack.ranking.$ruleName.$type.lifes.$i", rankingLifes[type][i])
				prop.setProperty("timeattack.ranking.$ruleName.$type.time.$i", rankingTime[type][i])
				prop.setProperty("timeattack.ranking.$ruleName.$type.rollclear.$i", rankingRollclear[type][i])
			}
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
			if(clear>rankingRollclear[type][i])
				return i
			else if(clear==rankingRollclear[type][i]&&ln>rankingLines[type][i])
				return i
			else if(clear==rankingRollclear[type][i]&&ln==rankingLines[type][i]&&lf>rankingLifes[type][i])
				return i
			else if(clear==rankingRollclear[type][i]&&ln==rankingLines[type][i]&&lf==rankingLifes[type][i]
				&&time<rankingTime[type][i])
				return i

		return -1
	}

	/** NET: Send various in-game stats (as well as goaltype)
	 * @param engine GameEngine
	 */
	override fun netSendStats(engine:GameEngine) {
		val bg = if(engine.owner.backgroundStatus.fadesw)
			engine.owner.backgroundStatus.fadebg
		else
			engine.owner.backgroundStatus.bg
		var msg = "game\tstats\t"
		msg += engine.statistics.lines.toString()+"\t"+engine.statistics.totalPieceLocked+"\t"
		msg += engine.statistics.time.toString()+"\t"+engine.statistics.lpm+"\t"
		msg += engine.statistics.pps.toString()+"\t"+goaltype+"\t"
		msg += engine.gameActive.toString()+"\t"+engine.timerActive+"\t"
		msg += engine.statistics.level.toString()+"\t"+levelTimer+"\t"+levelTimerMax+"\t"
		msg += rolltime.toString()+"\t"+norm+"\t"+bg+"\t"+engine.meterValue+"\t"+engine.meterColor+"\t"
		msg += engine.heboHiddenEnable.toString()+"\t"+engine.heboHiddenTimerNow+"\t"+engine.heboHiddenTimerMax+"\t"
		msg += engine.heboHiddenYNow.toString()+"\t"+engine.heboHiddenYLimit+"\n"+engine.lives+"\n"
		netLobby!!.netPlayerClient!!.send(msg)
	}

	/** NET: Receive various in-game stats (as well as goaltype) */
	override fun netRecvStats(engine:GameEngine, message:Array<String>) {
		engine.statistics.lines = Integer.parseInt(message[4])
		engine.statistics.totalPieceLocked = Integer.parseInt(message[5])
		engine.statistics.time = Integer.parseInt(message[6])
		//engine.statistics.lpm = java.lang.Float.parseFloat(message[7])
		//engine.statistics.pps = java.lang.Float.parseFloat(message[8])
		goaltype = Integer.parseInt(message[9])
		engine.gameActive = java.lang.Boolean.parseBoolean(message[10])
		engine.timerActive = java.lang.Boolean.parseBoolean(message[11])
		engine.statistics.level = Integer.parseInt(message[12])
		levelTimer = Integer.parseInt(message[13])
		levelTimerMax = Integer.parseInt(message[14])
		rolltime = Integer.parseInt(message[15])
		norm = Integer.parseInt(message[16])
		engine.owner.backgroundStatus.bg = Integer.parseInt(message[17])
		engine.meterValue = Integer.parseInt(message[18])
		engine.meterColor = Integer.parseInt(message[19])
		engine.heboHiddenEnable = java.lang.Boolean.parseBoolean(message[20])
		engine.heboHiddenTimerNow = Integer.parseInt(message[21])
		engine.heboHiddenTimerMax = Integer.parseInt(message[21])
		engine.heboHiddenYNow = Integer.parseInt(message[22])
		engine.heboHiddenYLimit = Integer.parseInt(message[23])
		engine.lives = Integer.parseInt(message[24])
	}

	/** NET: Send end-of-game stats
	 * @param engine GameEngine
	 */
	override fun netSendEndGameStats(engine:GameEngine) {
		val subMsg = StringBuilder()
		subMsg.append("NORM;").append(norm).append("\t")
		subMsg.append("LEVEL;").append(engine.statistics.level+engine.statistics.levelDispAdd).append("\t")
		subMsg.append("TIME;").append(GeneralUtil.getTime(engine.statistics.time.toFloat())).append("\t")
		subMsg.append("PIECE;").append(engine.statistics.totalPieceLocked).append("\t")
		subMsg.append("LINE/MIN;").append(engine.statistics.lpm).append("\t")
		subMsg.append("PIECE/SEC;").append(engine.statistics.pps).append("\t")
		subMsg.append("SECTION AVERAGE;").append(GeneralUtil.getTime(sectionavgtime.toFloat())).append("\t")
		for(i in sectionTime.indices)
			if(sectionTime[i]>0)
				subMsg.append("SECTION ").append(
					i+1).append(";").append(GeneralUtil.getTime(sectionTime[i].toFloat())).append("\t")

		val msg = "gstat1p\t"+NetUtil.urlEncode(subMsg.toString())+"\n"
		netLobby!!.netPlayerClient!!.send(msg)
	}

	/** NET: Send game options to all spectators
	 * @param engine GameEngine
	 */
	override fun netSendOptions(engine:GameEngine) {
		var msg = "game\toption\t"
		msg += goaltype.toString()+"\t"+startlevel+"\t"+showsectiontime+"\t"+big+"\n"
		netLobby!!.netPlayerClient!!.send(msg)
	}

	/** NET: Receive game options */
	override fun netRecvOptions(engine:GameEngine, message:Array<String>) {
		goaltype = Integer.parseInt(message[4])
		startlevel = Integer.parseInt(message[5])
		showsectiontime = java.lang.Boolean.parseBoolean(message[6])
		big = java.lang.Boolean.parseBoolean(message[7])
	}

	/** NET: Get goal type */
	override fun netGetGoalType():Int = goaltype

	/** NET: It returns true when the current settings doesn't prevent
	 * leaderboard screen from showing. */
	override fun netIsNetRankingViewOK(engine:GameEngine):Boolean = startlevel==0&&!big&&engine.ai==null

	companion object {
		/** Current version of this mode */
		private const val CURRENT_VERSION = 1

		/** Gravity tables */
		private val tableGravity = arrayOf(intArrayOf(4, 12, 48, 72, 96, 128, 256, 384, 512, 768, 1024, 1280, -1), // NORMAL
			intArrayOf(84, 128, 256, 512, 768, 1024, 1280, -1), // HIGH SPEED 1
			intArrayOf(-1), // HIGH SPEED 2
			intArrayOf(-1), // ANOTHER
			intArrayOf(-1), // ANOTHER 2
			intArrayOf(4, 12, 48, 72, 96, 128, 256, 384, 512, 768, 1024, 1280, -1), // NORMAL 200
			intArrayOf(-1), // ANOTHER 200
			intArrayOf(1, 3, 15, 30, 60, 120, 180, 240, 300, 300, -1), // BASIC
			intArrayOf(-1), // HELL
			intArrayOf(-1), // HELL-X
			intArrayOf(-1))// VOID

		/** Denominator table */
		private val tableDenominator = intArrayOf(256, // NORMAL
			256, // HIGH SPEED 1
			256, // HIGH SPEED 2
			256, // ANOTHER
			256, // ANOTHER2
			256, // NORMAL 200
			256, // ANOTHER 200
			60, // BASIC
			256, // HELL
			256, // HELL-X
			256)// VOID

		/** Max level table */
		private val tableGoalLevel = intArrayOf(15, // NORMAL
			15, // HIGH SPEED 1
			15, // HIGH SPEED 2
			15, // ANOTHER
			15, // ANOTHER2
			20, // NORMAL 200
			20, // ANOTHER 200
			25, // BASIC
			30, // HELL
			30, // HELL-X
			30)// VOID
		/** Max Life table */
		private val tableLives = intArrayOf(2, // NORMAL
			2, // HIGH SPEED 1
			2, // HIGH SPEED 2
			3, // ANOTHER
			3, // ANOTHER2
			4, // NORMAL 200
			4, // ANOTHER 200
			2, // BASIC
			9, // HELL
			9, // HELL-X
			9)// VOID
		/** Level timer tables */
		private val tableLevelTimer = arrayOf(intArrayOf(6400, 6250, 6000, 5750, 5500, 5250, 5000, 4750, 4500, 4250, // NORMAL 000-100
			4000, 3750, 3500, 3250, 3000), // NORMAL 100-150
			intArrayOf(4500, 4200, 4100, 3900, 3700, 3500, 3300, 3100, 2900, 2700, 2500, 2350, 2200, 2100, 2000), // HIGH SPEED 1
			intArrayOf(4000, 3900, 3800, 3700, 3600, 3500, 3400, 3300, 3200, 3100, 3000, 2900, 2800, 2700, 2500), // HIGH SPEED 2
			intArrayOf(3600, 3500, 3400, 3300, 3200, 3100, 3000, 2900, 2800, 2700, 2550, 2400, 2250, 2100, 2000), // ANOTHER
			intArrayOf(3000, 2900, 2800, 2700, 2600, 2500, 2400, 2300, 2200, 2100, 2000, 2000, 2000, 2000, 2000), // ANOTHER 2
			intArrayOf(6400, 6200, 6000, 5800, 5600, 5400, 5200, 5000, 4800, 4600, // NORMAL 000-100
				4300, 4000, 3800, 3600, 3500, 3400, 3300, 3200, 3100, 3000), // NORMAL 100-200
			intArrayOf(4000, 3890, 3780, 3670, 3560, 3450, 3340, 3230, 3120, 3010, // ANOTHER 000-100
				2900, 2800, 2700, 2600, 2500, 2400, 2300, 2200, 2100, 2000), // ANOTHER 100-200
			intArrayOf(4000, 3890, 3780, 3670, 3560, 3450, 3340, 3230, 3120, 3010, // BASIC 000-100
				2900, 2800, 2700, 2600, 2500, 2400, 2300, 2200, 2100, 2000), // BASIC 100-200
			intArrayOf(2000), // HELL
			intArrayOf(2000), // VOID
			intArrayOf(2000))// VOID

		/** Speed table for ANOTHER */
		private val tableAnother = arrayOf(intArrayOf(19, 18, 17, 16, 15, 14, 13, 12, 11, 10), // ARE
			intArrayOf(30, 29, 28, 27, 26, 25, 24, 23, 22, 21), // Line delay
			intArrayOf(30, 29, 28, 27, 26, 25, 24, 23, 22, 21), // Lock delay
			intArrayOf(10, 10, 9, 9, 8, 8, 8, 7, 7, 7) // DAS
		)

		/** Speed table for NORMAL 200 */
		private val tableNormal200 = arrayOf(intArrayOf(25, 25, 25, 25, 25, 24, 23, 22, 21, 20, 19, 18, 17, 16, 15, 14, 13, 12, 11, 10), // ARE
			intArrayOf(25, 24, 23, 22, 21, 20, 19, 18, 17, 16, 15, 14, 13, 12, 11, 10, 9, 8, 7, 6), // Line delay
			intArrayOf(30, 30, 30, 30, 29, 29, 29, 29, 28, 28, 28, 27, 27, 27, 26, 26, 25, 25, 24, 24), // Lock delay
			intArrayOf(15, 15, 15, 15, 15, 14, 14, 14, 14, 13, 13, 13, 12, 12, 11, 10, 9, 8, 7, 6) // DAS
		)

		/** Speed table for VOID */
		private val tableVoid = arrayOf(intArrayOf(16, 15, 14, 13, 12, 11, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1), // ARE
			intArrayOf(8, 7, 7, 6, 6, 5, 5, 4, 4, 3, 3, 2, 2, 1, 1, 0), // Line delay
			intArrayOf(25, 24, 24, 23, 23, 22, 22, 21, 21, 20, 20, 19, 18, 17, 16, 15), // Lock delay
			intArrayOf(9, 8, 8, 7, 7, 6, 6, 5, 5, 4, 4, 4, 4, 4, 4, 4) // DAS
		)

		/** Speed table for BASIC */
		private val tableBasic = arrayOf(intArrayOf(26, 25, 24, 23, 22, 21, 20, 19, 18, 17, 16, 15, 14, 13, 12, 11, 10, 9, 8, 7), // ARE
			intArrayOf(40, 36, 33, 30, 27, 24, 21, 19, 17, 15, 13, 11, 9, 8, 7, 6, 5, 4, 3, 3), // Line delay
			intArrayOf(28, 28, 28, 27, 27, 27, 26, 26, 26, 25, 25, 25, 24, 24, 23, 22, 22, 21, 21, 20), // Lock delay
			intArrayOf(15, 15, 15, 15, 15, 14, 14, 14, 14, 13, 13, 13, 12, 12, 11, 10, 9, 8, 7, 6) // DAS
		)

		/** BGM change lines table */
		private val tableBGMChange = arrayOf(intArrayOf(50, 100), // NORMAL
			intArrayOf(50, 100), // HI-SPEED 1
			intArrayOf(50, 100), // HI-SPEED 2
			intArrayOf(40, 100), // ANOTHER
			intArrayOf(40, 100), // ANOTHER2
			intArrayOf(50, 100, 150), // NORMAL 200
			intArrayOf(40, 100, 150), // ANOTHER 200
			intArrayOf(50, 100, 150, 200), // BASIC 250
			intArrayOf(), // HELL
			intArrayOf(), // HELL-X
			intArrayOf())// VOID

		/** BGM fadeout lines table */
		private val tableBGMFadeout = arrayOf(intArrayOf(46, 96, 146), // NORMAL
			intArrayOf(46, 96, 146), // HI-SPEED 1
			intArrayOf(46, 96, 146), // HI-SPEED 2
			intArrayOf(36, 96, 146), // ANOTHER
			intArrayOf(36, 96, 146), // ANOTHER2
			intArrayOf(46, 96, 146, 196), // NORMAL 200
			intArrayOf(46, 96, 146, 196), // ANOTHER 200
			intArrayOf(46, 96, 146, 196, 246), // BASIC 250
			intArrayOf(), // HELL
			intArrayOf(), // HELL-X
			intArrayOf())// VOID

		/** Backgrounds table */
		private val tableBGoffset = intArrayOf(0, // NORMAL
			3, // HIGH SPEED 1
			15, // HIGH SPEED 2
			14, // ANOTHER
			15, // ANOTHER2
			0, // NORMAL 200
			10, // ANOTHER 200
			3, // BASIC
			10, // HELL
			10, // HELL-X
			10)// VOID
		private val tableBGM = arrayOf(arrayOf(BGMStatus.BGM.GM_2, BGMStatus.BGM.GM_1, BGMStatus.BGM.EXTRA_2), // NORMAL
			arrayOf(BGMStatus.BGM.GM_3, BGMStatus.BGM.EXTRA_1, BGMStatus.BGM.GM_20G_2), // HI-SPEED 1
			arrayOf(BGMStatus.BGM.GM_20G_1, BGMStatus.BGM.GM_20G_2, BGMStatus.BGM.GM_20G_3), // HI-SPEED 2
			arrayOf(BGMStatus.BGM.BLITZ_1, BGMStatus.BGM.STORM_1, BGMStatus.BGM.STORM_2), // ANOTHER
			arrayOf(BGMStatus.BGM.BLITZ_2, BGMStatus.BGM.BLITZ_3, BGMStatus.BGM.BLITZ_4), // ANOTHER2
			arrayOf(BGMStatus.BGM.EXTRA_2, BGMStatus.BGM.GM_2, BGMStatus.BGM.GM_3, BGMStatus.BGM.EXTRA_1, BGMStatus.BGM.BLITZ_1), // NORMAL 200
			arrayOf(BGMStatus.BGM.BLITZ_1, BGMStatus.BGM.STORM_1, BGMStatus.BGM.BLITZ_2, BGMStatus.BGM.STORM_2, BGMStatus.BGM.BLITZ_3), // ANOTHER 200
			arrayOf(BGMStatus.BGM.EXTRA_3, BGMStatus.BGM.GM_2, BGMStatus.BGM.GM_20G_1, BGMStatus.BGM.BLITZ_1, BGMStatus.BGM.STORM_2), // BASIC
			arrayOf(BGMStatus.BGM.FINALE_3), // HELL
			arrayOf(BGMStatus.BGM.FINALE_1), // HELL-X
			arrayOf(BGMStatus.BGM.FINALE_2))// VOID

		/** Game types */
		private const val GAMETYPE_NORMAL = 0
		private const val GAMETYPE_HIGHSPEED1 = 1
		private const val GAMETYPE_HIGHSPEED2 = 2
		private const val GAMETYPE_ANOTHER = 3
		private const val GAMETYPE_ANOTHER2 = 4
		private const val GAMETYPE_NORMAL200 = 5
		private const val GAMETYPE_ANOTHER200 = 6
		private const val GAMETYPE_BASIC = 7
		private const val GAMETYPE_HELL = 8
		private const val GAMETYPE_HELLX = 9
		private const val GAMETYPE_VOID = 10

		/** Number of game types */
		private const val GAMETYPE_MAX = 11

		/** Game type names (short) */
		private val GAMETYPE_NAME = arrayOf("EASY", "HARD", "20G", "ANOTHER", "EXTREME", "MODERATE", "EXHAUST", "CHALLENGE", "FURTHEST", "FORGOTTEN", "PRIME.01")

		/** Game type names (long) */
		private val GAMETYPE_NAME_LONG = arrayOf("EASY", "HARD", "20G", "ANOTHER", "EXTREME", "MODERATE", "EXHAUST", "CHALLENGE", "FURTHEST", "FORGOTTEN", "PRIMORDIAL BIT")

		/** HELL-X fade table */
		private val tableHellXFade = intArrayOf(600, 550, 500, 450, 400, 350, 300, 270, 240, 210, 190, 170, 160, 150, 140, 130, 125, 120, 115, 110, 100, 90, 80, 70, 60, 58, 56, 54, 52, 50)

		/** Ending time limit */
		private const val ROLLTIMELIMIT = 3238

		/** Number of ranking records */
		private const val RANKING_MAX = 10

		/** Number of ranking types */
		private const val RANKING_TYPE = 11
	}
}
