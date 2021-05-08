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
import kotlin.math.ceil

/** RETRO MODERN mode (Based from NAOMI, build by Venom_Nhelv 20180131-2020 */
class RetroModern:AbstractMode() {
	private var totalnorma:Int = 0

	/** Amount of points you just get from line clears */
	private var lastscore:Int = 0

	/** Elapsed time from last line clear (lastscore is displayed to screen
	 * until this reaches to 120) */
	private var scgettime:Int = 0
	private var sc:Int = 0

	/** Selected game type */
	private var gametype:Int = 0

	/** Selected starting level */
	private var startlevel:Int = 0

	/** Ending Level timer */
	private var rolltime:Int = 0

	/** Amount of lines cleared (It will be reset when the level increases) */
	private var norm:Int = 0

	private var lineslot:IntArray = IntArray(3)
	private var linecount:Int = 0

	private var special:Boolean = false
	/** Big mode on/off */
	private var big:Boolean = false

	/** Version of this mode */
	private var version:Int = 0

	/** Your place on leaderboard (-1: out of rank) */
	private var rankingRank:Int = 0

	/** Ranking records */
	private var rankingScore:Array<IntArray> = Array(GAMETYPE_MAX) {IntArray(RANKING_MAX)}
	private var rankingLevel:Array<IntArray> = Array(GAMETYPE_MAX) {IntArray(RANKING_MAX)}
	private var rankingLines:Array<IntArray> = Array(GAMETYPE_MAX) {IntArray(RANKING_MAX)}
	private var rankingTime:Array<IntArray> = Array(GAMETYPE_MAX) {IntArray(RANKING_MAX)}

	/** Returns the name of this mode */
	override val name:String = "Retro Modern.S"

	/** This function will be called when the game enters the main game
	 * screen. */
	override fun playerInit(engine:GameEngine, playerID:Int) {
		super.playerInit(engine, playerID)
		lastscore = 0
		sc = 0
		scgettime = sc
		rolltime = 0
		norm = 0
		menuTime = 0
		special = false

		lineslot = IntArray(3)
		linecount = 0

		rankingRank = -1
		rankingScore = Array(GAMETYPE_MAX) {IntArray(RANKING_MAX)}
		rankingLevel = Array(GAMETYPE_MAX) {IntArray(RANKING_MAX)}
		rankingLines = Array(GAMETYPE_MAX) {IntArray(RANKING_MAX)}
		rankingTime = Array(GAMETYPE_MAX) {IntArray(RANKING_MAX)}

		engine.twistEnable = false
		engine.b2bEnable = false
		engine.splitb2b = false
		engine.comboType = GameEngine.COMBO_TYPE_DISABLE
		engine.bighalf = false
		engine.bigmove = false

		engine.staffrollEnable = true
		engine.staffrollNoDeath = false

		engine.speed.are = 30
		engine.speed.areLine = 30
		engine.speed.lineDelay = 57//42
		engine.speed.lockDelay = 30
		engine.speed.das = 15

		if(!owner.replayMode) {

			version = CURRENT_VERSION
		} else
			loadSetting(owner.replayProp)
		if(startlevel>15) startlevel = 15
		engine.owner.backgroundStatus.bg = levelBG[startlevel]
		engine.framecolor = GameEngine.FRAME_SKIN_HEBO
	}

	/** Set the gravity speed
	 * @param engine GameEngine object
	 */
	private fun setSpeed(engine:GameEngine) {
		var lv = engine.statistics.level

		engine.ruleopt.lockresetMove = gametype==4
		engine.ruleopt.lockresetRotate = gametype==4
		engine.ruleopt.lockresetWallkick = gametype==4
		engine.ruleopt.lockresetFall = true
		engine.ruleopt.softdropLock = true
		engine.ruleopt.softdropMultiplyNativeSpeed = false
		engine.ruleopt.softdropGravitySpeedLimit = true
		engine.ruleopt.softdropSpeed = 1f
		engine.owSDSpd = -1
		if(lv<0) lv = 0
		when {
			lv<=MAX_LEVEL -> {
				val d = tableDenominator[gametype][lv]
				if(d==0)
					engine.speed.gravity = -1
				else {
					engine.speed.gravity = if(d<0) d*-1 else 1
					engine.speed.denominator = if(d>0) d else 1
				}
				engine.speed.areLine = tableARE[gametype][lv]
				engine.speed.are = engine.speed.areLine
				engine.speed.lockDelay = tableLockDelay[gametype][lv]

				engine.speed.lineDelay = when(gametype) {
					0 -> 57
					1 -> 48
					2 -> 39
					3 -> 30
					else -> 20
				}
			}
			lv==MAX_LEVEL+1 -> {
				engine.speed.gravity = 1
				engine.speed.denominator = 24
				engine.speed.areLine = 31
				engine.speed.are = engine.speed.areLine
				engine.speed.lockDelay = 44
				engine.speed.lineDelay = 57
			}
			gametype==4 -> {
				engine.speed.gravity = -1
				engine.speed.denominator = 1
				engine.speed.areLine = 15
				engine.speed.are = engine.speed.areLine
				engine.speed.lineDelay = 25
				engine.speed.lockDelay = 22
			}
			else -> {
				engine.speed.denominator = 1
				engine.speed.gravity = engine.speed.denominator
				engine.speed.areLine = 17
				engine.speed.are = engine.speed.areLine
				engine.speed.lineDelay = 42
				engine.speed.lockDelay = 18
			}
		}
	}

	/** Main routine for game setup screen */
	override fun onSetting(engine:GameEngine, playerID:Int):Boolean {
		if(!engine.owner.replayMode) {
			// Configuration changes
			val change = updateCursor(engine, 2)

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
						engine.owner.backgroundStatus.bg = levelBG[startlevel]
					}
					2 -> big = !big
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
			engine.statc[3]++
			engine.statc[2] = -1

			return engine.statc[3]<60
		}

		return true
	}

	/** Renders game setup screen */
	override fun renderSetting(engine:GameEngine, playerID:Int) {
		drawMenu(engine, playerID, receiver, 0, COLOR.BLUE, 0, "DIFFICULTY", GAMETYPE_NAME[gametype], "Level", "$startlevel", "BIG",
			GeneralUtil.getONorOFF(big))
	}

	private fun setBGM(lv:Int) {
		owner.bgmStatus.bgm = when(lv) {
			MAX_LEVEL -> BGM.GrandM(1)
			MAX_LEVEL+1 -> BGM.Silent
			MAX_LEVEL+2 -> BGM.Ending(3)
			else -> BGM.RetroS(tableBGMlevel.count {it<=lv})
		}
	}
	/** This function will be called before the game actually begins (after
	 * Ready&Go screen disappears) */
	override fun startGame(engine:GameEngine, playerID:Int) {
		if(engine.ending!=0) return
		engine.big = big
		special = true
		setBGM(startlevel)
		setSpeed(engine)
	}

	/** Ready */
	override fun onReady(engine:GameEngine, playerID:Int):Boolean {
		if(engine.statc[0]==0)
			if(engine.ending==0) {
				engine.framecolor = if(gametype==4) GameEngine.FRAME_COLOR_RED else GameEngine.FRAME_COLOR_WHITE
				totalnorma = MAX_LINES-startlevel*16
				engine.statistics.level = startlevel
			} else
				engine.nextPieceArrayID = GeneralUtil.createNextPieceArrayFromNumberString(STRING_POWERON_PATTERN)

		return false
	}

	/** Renders HUD (leaderboard or game statistics) */
	override fun renderLast(engine:GameEngine, playerID:Int) {
		receiver.drawScoreFont(engine, playerID, 0, 0, "RETRO MODERN", color = COLOR.COBALT)
		receiver.drawScoreFont(engine, playerID, 0, 1, "(${GAMETYPE_NAME[gametype]} SPEED)", COLOR.COBALT)

		if(engine.stat==GameEngine.Status.SETTING||engine.stat==GameEngine.Status.RESULT&&!owner.replayMode) {
			// Leaderboard
			if(!owner.replayMode&&!big&&startlevel==0&&engine.ai==null) {
				val scale = if(receiver.nextDisplayType==2) .5f else 1f
				val topY = if(receiver.nextDisplayType==2) 6 else 4
				receiver.drawScoreFont(engine, playerID, 3, topY-1, "SCORE LV LINE TIME", color = COLOR.BLUE, scale = scale)

				for(i in 0 until RANKING_MAX) {
					receiver.drawScoreGrade(engine, playerID, 0, topY+i, String.format("%2d", i+1), COLOR.YELLOW, scale)
					receiver.drawScoreNum(engine, playerID, 3, topY+i, "${rankingScore[gametype][i]}", i==rankingRank, scale)
					receiver.drawScoreNum(engine, playerID, 9, topY+i, "${rankingLines[gametype][i]}", i==rankingRank, scale)
					receiver.drawScoreNum(engine, playerID, 12, topY+i, "${rankingLevel[gametype][i]}", i==rankingRank, scale)
					receiver.drawScoreNum(engine, playerID, 16, topY+i, GeneralUtil.getTime(rankingTime[gametype][i]), i==rankingRank,
						scale)
				}
			}
		} else {
			// Game statistics
			receiver.drawScoreFont(engine, playerID, 0, 3, "Score", COLOR.BLUE)
			receiver.drawScoreNum(engine, playerID, 5, 3, "+$lastscore")
			val scget = scgettime<engine.statistics.score
			if(scget) scgettime += ceil((engine.statistics.score-scgettime)/24.0).toInt()
			sc += ceil(((scgettime-sc)/10f).toDouble()).toInt()
			receiver.drawScoreNum(engine, playerID, 0, 4, "$sc", scget, 2f)

			receiver.drawScoreFont(engine, playerID, 0, 7, "LINE", COLOR.BLUE)
			receiver.drawScoreNum(engine, playerID, 0, 8, String.format("%03d/%03d", engine.statistics.lines, totalnorma), scale = 2f)

			receiver.drawScoreFont(engine, playerID, 0, 10, "Level", COLOR.BLUE)
			var lvdem = 0
			if(rolltime>0)
				lvdem = rolltime*100/ROLLTIMELIMIT
			else if(engine.statistics.level<levelNorma.size) lvdem = norm*100/levelNorma[engine.statistics.level]
			if(lvdem<0) lvdem *= -1
			if(lvdem>=100) lvdem -= lvdem-lvdem%100
			receiver.drawScoreNum(engine, playerID, 5, 10, String.format("%02d.%02d", engine.statistics.level, lvdem), scale = 2f)

			receiver.drawScoreFont(engine, playerID, 0, 11, "Time", COLOR.BLUE)
			receiver.drawScoreNum(engine, playerID, 0, 12, GeneralUtil.getTime(engine.statistics.time), scale = 2f)

			// Roll 残り time
			if(rolltime>0) {
				val time = ROLLTIMELIMIT-rolltime
				receiver.drawScoreFont(engine, playerID, 0, 15, "FLASH BACK", COLOR.CYAN)
				receiver.drawScoreNum(engine, playerID, 0, 16, GeneralUtil.getTime(time), time>0&&time<10*60, 2f)
			}

		}
	}

	/** This function will be called when the game timer updates */
	override fun onLast(engine:GameEngine, playerID:Int) {
		if(special&&(engine.ctrl.isPress(Controller.BUTTON_B)||engine.ctrl.isPress(Controller.BUTTON_E))) special = false
		// Update the meter
		if(engine.ending==0) {
			if(engine.statistics.level<levelNorma.size)
				engine.meterValue = receiver.getMeterMax(engine)*norm/levelNorma[engine.statistics.level]
			else
				engine.meterValue = receiver.getMeterMax(engine)*engine.statistics.lines/totalnorma

			engine.meterColor = -0xff0001
			if(engine.statistics.level>=4) engine.meterColor = 0xFF00FF
			if(engine.statistics.level>=10) engine.meterColor = -0x100
			if(engine.statistics.level>=15) engine.meterColor = -0x1

		} else
		// Ending
			if(engine.gameActive&&engine.statistics.level==17) {
				rolltime++

				// Time meter
				val remainRollTime = ROLLTIMELIMIT-rolltime
				engine.meterValue = remainRollTime*receiver.getMeterMax(engine)/ROLLTIMELIMIT
				engine.meterColor = GameEngine.METER_COLOR_LIMIT
				var bg = levelBG[levelBG.size-1]
				if(rolltime<=ROLLTIMELIMIT-3600) bg = levelBG[rolltime*MAX_LEVEL/(ROLLTIMELIMIT-3600)]
				//else owner.bgmStatus.bgm=Ending(1);

				if(owner.backgroundStatus.fadebg!=bg) {
					owner.backgroundStatus.fadebg = bg
					owner.backgroundStatus.fadecount = 0
					owner.backgroundStatus.fadesw = true
				}
				// Roll 終了
				if(rolltime>=ROLLTIMELIMIT) {
					engine.statistics.level++
					engine.gameEnded()
					engine.resetStatc()
					engine.stat = GameEngine.Status.EXCELLENT
					if(special) {
						lastscore = 10000000
						engine.statistics.scoreBonus += lastscore
					}
				}
			}
	}

	/** Calculates line-clear score
	 * (This function will be called even if no lines are cleared) */
	override fun calcScore(engine:GameEngine, playerID:Int, lines:Int):Int {
		// Determines line-clear bonus
		var pts = 0
		val mult = tableScoreMult[gametype][engine.statistics.level]*10
		if(lines==1)
			pts = 5*mult // Single
		else if(lines==2)
			pts = (if(engine.split) 30 else 20)*mult // Double
		else if(lines==3)
			pts = (if(engine.split) 55 else 45)*mult // Triple
		else if(lines>=4) pts = 100*mult // Four
		if(lines>0&&engine.field!!.isEmpty)
		// Perfect clear bonus
			pts = 2000*tableBonusMult[engine.statistics.level]
		// Add score
		if(pts>0) {
			lastscore = pts
			engine.statistics.scoreLine += pts
		}
		if(engine.manualLock) {
			scgettime++
			if(engine.ruleopt.harddropLock) engine.statistics.scoreHD++
			else engine.statistics.scoreSD++
		}
		if(lines>0) {
			lineslot[linecount] = if(lines>4) 4 else lines
			linecount++

			// Add lines
			norm += lines
		}
		// Level up
		var lvup = false
		if(engine.statistics.level<MAX_LEVEL&&norm>=levelNorma[engine.statistics.level]||
			engine.statistics.level==MAX_LEVEL&&engine.statistics.lines>=totalnorma||
			engine.statistics.level==MAX_LEVEL+1)
			lvup = lines>0

		if(lvup) {
			val newlevel = ++engine.statistics.level
			if(engine.ending==0)
				if(engine.statistics.lines>=totalnorma)
					engine.ending = 1
				else
					engine.playSE("levelup")


			if(newlevel!=MAX_LEVEL+1) setSpeed(engine)

			if(newlevel<levelBG.size-1) {
				owner.backgroundStatus.fadecount = 0
				owner.backgroundStatus.fadebg = levelBG[newlevel]
				owner.backgroundStatus.fadesw = true
			}
			norm = 0
			engine.meterValue = 0
			setBGM(newlevel)
		}
		return pts
	}

	override fun renderLineClear(engine:GameEngine, playerID:Int) {
		var num = 0

		when {
			engine.lineClearing==1 -> num = 1
			engine.lineClearing==2 -> num = 10
			engine.lineClearing==3 -> num = 100
			engine.lineClearing>=4 -> num = 100000
		}
		if(linecount>=3) num *= 5
		receiver.drawMenuBadges(engine, playerID, 2, engine.lastline-if(num>=100000) if(num>=500000) 3 else 1 else 0, num)
		receiver.drawMenuNum(engine, playerID, 4, engine.lastline, "$lastscore", COLOR.CYAN)

		if(engine.split) when(engine.lineClearing) {
			2 -> receiver.drawMenuFont(engine, playerID, 0, engine.lastlines[0], "SPLIT TWIN", COLOR.PURPLE)
			3 -> receiver.drawMenuFont(engine, playerID, 0, engine.lastlines[0], "1.2.TRIPLE", COLOR.PURPLE)
		}
	}

	override fun lineClearEnd(engine:GameEngine, playerID:Int):Boolean {
		if(linecount>=3) {
			var pts = 0
			if(lineslot[0]==lineslot[1]&&lineslot[1]==lineslot[2]) {
				val y = lineslot[0]
				if(y==1) {
					pts = 5
					engine.playSE("b2b_start")
				} else if(y>=4) {
					pts = 10000
					engine.playSE("combo_continue")
					engine.playSE("b2b_combo")
				} else {
					engine.playSE("b2b_combo")
					if(y==2) pts = 1000
					if(y==3) pts = 5000
				}
			} else if(lineslot[0]==lineslot[1]) engine.playSE("b2b_end")

			linecount = 0
			if(pts>0) {
				pts *= 10*tableBonusMult[engine.statistics.level]
				lastscore = pts
				engine.statistics.scoreBonus += pts
				receiver.addScore(engine, engine.fieldWidth/2, engine.fieldHeight+1, pts, COLOR.RAINBOW)
			}
		}
		return super.lineClearEnd(engine, playerID)
	}

	/** This function will be called when soft-drop is used */
	override fun afterSoftDropFall(engine:GameEngine, playerID:Int, fall:Int) {
		engine.statistics.scoreSD += fall
		scgettime += fall
	}

	/** This function will be called when hard-drop is used */
	override fun afterHardDropFall(engine:GameEngine, playerID:Int, fall:Int) {
		engine.statistics.scoreHD += fall
		scgettime += fall
	}

	override fun onEndingStart(engine:GameEngine, playerID:Int):Boolean {
		val time = engine.statc[0]>=engine.lineDelay+2
		if(time) {
			engine.ending = 2
			engine.resetStatc()
			engine.field!!.reset()
			engine.nowPieceObject = null
			engine.stat = GameEngine.Status.CUSTOM
		}
		return time

	}

	override fun onCustom(engine:GameEngine, playerID:Int):Boolean {
		engine.nextPieceArrayID = GeneralUtil.createNextPieceArrayFromNumberString(STRING_POWERON_PATTERN)
		engine.nextPieceArrayObject = emptyArray()
		engine.holdPieceObject = null
		if(engine.statc[0]==0) engine.playSE("excellent")
		when {
			engine.statc[0]>=200&&engine.ctrl.isPush(Controller.BUTTON_A) -> engine.statc[0] = 300
		}
		if(engine.statc[0]==300) {
			setSpeed(engine)
			engine.resetStatc()
			engine.stat = GameEngine.Status.READY
		} else
			engine.statc[0]++
		return true
	}

	override fun renderCustom(engine:GameEngine, playerID:Int) {
		val offsetX = receiver.fieldX(engine, playerID)
		val offsetY = receiver.fieldY(engine, playerID)
		val col = COLOR.WHITE

		receiver.drawDirectFont(offsetX+4, offsetY+204, "EXCELLENT!", COLOR.ORANGE, 1f)
		if(engine.statc[0]>=100) {
			receiver.drawDirectFont(offsetX+36, offsetY+228, "BUT...", col)
			receiver.drawDirectFont(offsetX-4, offsetY+244, "THIS IS NOT", col)
			receiver.drawDirectFont(offsetX+12, offsetY+260, "OVER YET!", col)
		}

	}

	override fun renderExcellent(engine:GameEngine, playerID:Int) {

		val offsetX = receiver.fieldX(engine, playerID)
		val offsetY = receiver.fieldY(engine, playerID)
		val col = COLOR.WHITE

		receiver.drawDirectFont(offsetX-4, offsetY+228, "YOU REACHED", col)
		receiver.drawDirectFont(offsetX-4, offsetY+244, "THE EDGE OF", col)
		receiver.drawDirectFont(offsetX-4, offsetY+260, "THE JOURNEY", col)
		if(special) {
			receiver.drawDirectFont(offsetX+4, offsetY+292, "CLOCK WISE", col)
			receiver.drawDirectFont(offsetX+40, offsetY+308, "BONUS", col)
			receiver.drawDirectFont(offsetX-4, offsetY+266, "+ 10000000", when {
				engine.statc[0]%4==0 -> COLOR.YELLOW
				engine.statc[0]%2==0 -> col
				else -> COLOR.ORANGE
			})
		}
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
			updateRanking(engine.statistics.score, engine.statistics.lines, engine.statistics.level, engine.statistics.time, gametype)

			if(rankingRank!=-1) {
				saveRanking(owner.recordProp, engine.ruleopt.strRuleName)
				owner.saveModeConfig()
			}
		}
	}

	/** Load the settings */
	override fun loadSetting(prop:CustomProperties) {
		startlevel = prop.getProperty("retromodern.startlevel", 0)
		gametype = prop.getProperty("retromodern.gametype", 0)
		big = prop.getProperty("retromodern.big", false)
		version = prop.getProperty("retromodern.version", 0)
	}

	/** Save the settings */
	override fun saveSetting(prop:CustomProperties) {
		prop.setProperty("retromodern.startlevel", startlevel)
		prop.setProperty("retromodern.gametype", gametype)
		prop.setProperty("retromodern.big", big)
		prop.setProperty("retromodern.version", version)
	}

	/** Load the ranking */
	override fun loadRanking(prop:CustomProperties, ruleName:String) {
		for(i in 0 until RANKING_MAX)
			for(gametypeIndex in 0 until GAMETYPE_MAX) {
				rankingScore[gametypeIndex][i] = prop.getProperty("retromodern.ranking.$ruleName.$gametypeIndex.score.$i", 0)
				rankingLevel[gametypeIndex][i] = prop.getProperty("retromodern.ranking.$ruleName.$gametypeIndex.level.$i", 0)
				rankingLines[gametypeIndex][i] = prop.getProperty("retromodern.ranking.$ruleName.$gametypeIndex.lines.$i", 0)
				rankingTime[gametypeIndex][i] = prop.getProperty("retromodern.ranking.$ruleName.$gametypeIndex.time.$i", 0)

			}
	}

	/** Save the ranking */
	fun saveRanking(prop:CustomProperties, ruleName:String) {
		for(i in 0 until RANKING_MAX)
			for(gametypeIndex in 0 until GAMETYPE_MAX) {
				prop.setProperty("retromodern.ranking.$ruleName.$gametypeIndex.score.$i", rankingScore[gametypeIndex][i])
				prop.setProperty("retromodern.ranking.$ruleName.$gametypeIndex.level.$i", rankingLevel[gametypeIndex][i])
				prop.setProperty("retromodern.ranking.$ruleName.$gametypeIndex.lines.$i", rankingLines[gametypeIndex][i])
				prop.setProperty("retromodern.ranking.$ruleName.$gametypeIndex.time.$i", rankingTime[gametypeIndex][i])
			}
	}

	/** Update the ranking */
	private fun updateRanking(sc:Int, lv:Int, li:Int, time:Int, type:Int) {
		rankingRank = checkRanking(sc, li, lv, time, type)

		if(rankingRank!=-1) {
			// Shift the old records
			for(i in RANKING_MAX-1 downTo rankingRank+1) {
				rankingScore[type][i] = rankingScore[type][i-1]
				rankingLevel[type][i] = rankingLevel[type][i-1]
				rankingLines[type][i] = rankingLines[type][i-1]
				rankingTime[type][i] = rankingTime[type][i-1]
			}

			// Insert a new record
			rankingScore[type][rankingRank] = sc
			rankingLevel[type][rankingRank] = lv
			rankingLines[type][rankingRank] = li
			rankingTime[type][rankingRank] = time
		}
	}

	/** This function will check the ranking and returns which place you are.
	 * (-1: Out of rank) */
	private fun checkRanking(sc:Int, lv:Int, li:Int, time:Int, type:Int):Int {
		for(i in 0 until RANKING_MAX)
			if(sc>rankingScore[type][i])
				return i
			else if(sc==rankingScore[type][i]&&lv>rankingLines[type][i])
				return i
			else if(sc==rankingScore[type][i]&&lv==rankingLines[type][i]&&li>rankingLines[type][i])
				return i
			else if(sc==rankingScore[type][i]&&li==rankingLines[type][i]&&time<rankingTime[type][i]) return i

		return -1
	}

	companion object {
		/** Current version */
		private const val CURRENT_VERSION = 2

		/** Poweron Pattern */
		private const val STRING_POWERON_PATTERN = "4040050165233516506133350555213560141520224542633206134255165200333560031332022463366645230432611435533550326251231351500244220365666413154321122014634420132506140113461064400566344110153223400634050546214410040214650102256233133116353263111335043461206211262231565235306361150440653002222453302523255563545455656660124120450663502223206465164461126135621055103645066644052535021110020361422122352566156434351304346510363640453452505655142263102605202216351615031650050464160613325366023413453036542441246445101562252141201460505435130040221311400543416041660644410106141444041454511600413146353206260246251556635262420616451361336106153451563316660054255631510320566516465265421144640513424316315421664414026440165341010302443625101652205230550602002033120044344034100160442632436645325512265351205642343342312121523120061530234443062420033310461403306365402313212656105101254352514216210355230014040335464640401464125332132315552404146634264364245513600336065666305002023203545052006445544450440460"

		/** Gravity table */
		private val tableDenominator = arrayOf(
			intArrayOf(24, 15, 10, 6, 20, 5, 5, 4, 3, 3, 2, 2, 2, 2, 2, 1),
			intArrayOf(24, 15, 10, 4, 20, 3, 2, 2, 2, 2, 1, 1, 1, 1, 1, 1),
			intArrayOf(15, 6, 4, 3, 20, 2, 2, 1, 1, 1, 1, 1, 1, 1, 1, 1),
			intArrayOf(1, 1, 1, 1, 20, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1),
			intArrayOf(1, 1, 1, -2, 20, 1, 1, -2, -3, -4, -4, -4, -5, -4, -3, 0))
		/** Lock delay table */
		private val tableLockDelay = arrayOf(
			intArrayOf(44, 39, 34, 30, 39, 29, 29, 29, 28, 28, 28, 28, 28, 28, 28, 24),
			intArrayOf(44, 39, 34, 30, 39, 29, 29, 29, 28, 28, 24, 24, 24, 20, 20, 19),
			intArrayOf(39, 30, 30, 29, 39, 28, 28, 24, 24, 24, 24, 24, 24, 20, 20, 19),
			intArrayOf(24, 24, 24, 30, 39, 24, 24, 24, 24, 24, 24, 24, 24, 20, 20, 19),
			intArrayOf(24, 24, 24, 30, 39, 24, 24, 25, 25, 25, 24, 23, 23, 23, 23, 25))
		/** ARE table */
		private val tableARE = arrayOf(
			intArrayOf(31, 28, 26, 26, 28, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26),
			intArrayOf(31, 28, 26, 26, 28, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26),
			intArrayOf(28, 28, 26, 26, 28, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26),
			intArrayOf(26, 26, 26, 26, 28, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26),
			intArrayOf(26, 26, 26, 26, 28, 25, 25, 24, 24, 23, 23, 22, 22, 21, 21, 20))

		/** Lines until level up occers */
		private val levelNorma = intArrayOf(6, 6, 7, 9, 6, 9, 9, 9, 10, 10, 20, 16, 16, 16, 16)
		/** Max level */
		private const val MAX_LEVEL = 15
		private const val MAX_LINES = 300

		/** Score multiply table */
		private val tableScoreMult = arrayOf(
			intArrayOf(1, 2, 3, 4, 5, 6, 6, 6, 8, 8, 10, 10, 10, 10, 10, 11, 12, 13),
			intArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 20, 20),
			intArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 20, 20),
			intArrayOf(5, 5, 6, 6, 8, 8, 10, 10, 10, 10, 11, 12, 13, 14, 15, 16, 20, 20),
			intArrayOf(5, 5, 6, 7, 8, 9, 10, 10, 11, 12, 13, 14, 14, 15, 15, 16, 25, 25))
		private val tableBonusMult = intArrayOf(1, 1, 1, 1, 1, 2, 2, 2, 2, 2, 3, 3, 3, 3, 3, 5, 5, 10)

		private val levelBG = intArrayOf(
			0, 1, 2, 3, 4, 5,
			6, 7, 8, 9, 14, 19,
			10, 11, 12, 13, 29, 36)
		private val tableBGMlevel = arrayOf(4, 6, 8, 10, 11, 13, 15)
		/** Name of game types */
		private val GAMETYPE_NAME = arrayOf("EASY", "NORMAL", "INTENSE", "HARD", "OVERED")

		/** Number of game type */
		private const val GAMETYPE_MAX = 5

		/** Number of ranking records */
		private const val RANKING_MAX = 10

		/** LV17 roll time */
		private const val ROLLTIMELIMIT = 12000
	}
}
