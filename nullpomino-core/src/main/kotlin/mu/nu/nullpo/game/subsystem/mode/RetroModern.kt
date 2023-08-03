/*
 * Copyright (c) 2010-2023, NullNoname
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
import mu.nu.nullpo.game.event.EventReceiver.COLOR
import mu.nu.nullpo.game.event.ScoreEvent
import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.game.subsystem.mode.menu.BooleanMenuItem
import mu.nu.nullpo.game.subsystem.mode.menu.DelegateMenuItem
import mu.nu.nullpo.util.CustomProperties
import mu.nu.nullpo.util.GeneralUtil
import mu.nu.nullpo.util.GeneralUtil.toTimeStr

/** RETRO MODERN mode (Based from NAOMI, build by Venom_Nhelv 20180131-2020 */
class RetroModern:AbstractMode() {
	private var totalNorma = 0

	/** Selected game type */
	private var gameType = 0

	/** Selected starting level */
	private var startLevel = 0

	/** Ending Level timer */
	private var rollTime = 0

	/** Amount of lines cleared (It will be reset when the level increases) */
	private var norm = 0

	private var lineSlot = IntArray(3)
	private var lineCount = 0

	private var special = false

	private val itemBig = BooleanMenuItem("big", "BIG", COLOR.BLUE, false)
	/** BigMode */
	private var big:Boolean by DelegateMenuItem(itemBig)

	/** Version of this mode */
	private var version = 0

	/** Your place on leaderboard (-1: out of rank) */
	private var rankingRank = 0

	/** Ranking records */
	private val rankingScore:List<MutableList<Long>> = List(GAMETYPE_MAX) {MutableList(RANKING_MAX) {0L}}
	private val rankingLevel:List<MutableList<Int>> = List(GAMETYPE_MAX) {MutableList(RANKING_MAX) {0}}
	private val rankingLines:List<MutableList<Int>> = List(GAMETYPE_MAX) {MutableList(RANKING_MAX) {0}}
	private val rankingTime:List<MutableList<Int>> = List(GAMETYPE_MAX) {MutableList(RANKING_MAX) {0}}

	override val rankMap
		get() = rankMapOf(
			rankingScore.mapIndexed {a, x -> "$a.score" to x}+
				rankingLines.mapIndexed {a, x -> "$a.lines" to x}+
				rankingLevel.mapIndexed {a, x -> "$a.level" to x}+
				rankingTime.mapIndexed {a, x -> "$a.time" to x})

	/** Returns the name of this mode */
	override val name = "Retro Modern.S"

	/** This function will be called when the game enters the main game
	 * screen. */
	override fun playerInit(engine:GameEngine) {
		super.playerInit(engine)
		lastScore = 0
		rollTime = 0
		norm = 0
		special = false

		lineSlot = IntArray(3)
		lineCount = 0

		rankingRank = -1
		rankingScore.forEach {it.fill(0)}
		rankingLevel.forEach {it.fill(0)}
		rankingLines.forEach {it.fill(0)}
		rankingTime.forEach {it.fill(-1)}
		engine.twistEnable = false
		engine.b2bEnable = false
		engine.splitB2B = false
		engine.comboType = GameEngine.COMBO_TYPE_DISABLE
		engine.bigHalf = false
		engine.bigMove = false
		engine.statistics.levelDispAdd = 0

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
			loadSetting(engine, owner.replayProp)
		if(startLevel>15) startLevel = 15
		owner.bgMan.bg = levelBG[startLevel]
		engine.frameColor = GameEngine.FRAME_SKIN_HEBO
	}

	/** Set the gravity speed
	 * @param engine GameEngine object
	 */
	private fun setSpeed(engine:GameEngine) {
		val lv = maxOf(0, engine.statistics.level)

		engine.ruleOpt.lockResetMove = gameType!=0
		engine.ruleOpt.lockResetSpin = gameType!=1
		engine.ruleOpt.lockResetWallkick = gameType!=2
		engine.ruleOpt.lockResetFall = true
		engine.ruleOpt.softdropLock = true
		engine.ruleOpt.softdropMultiplyNativeSpeed = false
		engine.ruleOpt.softdropGravitySpeedLimit = true
		engine.ruleOpt.softdropSpeed = 1f
		engine.owSDSpd = -1
		when {
			lv<=MAX_LEVEL -> {
				receiver.setBGSpd(owner, .5f+gameType*.4f)
				val d = tableDenominator[gameType][lv]
				if(d==0)
					engine.speed.gravity = -1
				else {
					engine.speed.gravity = if(d<0) d*-1 else 1
					engine.speed.denominator = if(d>0) d else 1
				}
				engine.speed.areLine = tableARE[gameType][lv]
				engine.speed.are = engine.speed.areLine
				engine.speed.lockDelay = tableLockDelay[gameType][lv]
				engine.speed.das = minOf(15, tableLockDelay[gameType][lv]-12)

				engine.speed.lineDelay = when(gameType) {
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
				engine.speed.lineDelay = 57
				engine.speed.lockDelay = 44
				engine.speed.das = 15
			}
			gameType==4 -> {
				engine.speed.gravity = -1
				engine.speed.denominator = 1
				engine.speed.areLine = 15
				engine.speed.are = engine.speed.areLine
				engine.speed.lineDelay = 25
				engine.speed.lockDelay = 22
				engine.speed.das = 13
			}
			else -> {
				engine.ruleOpt.lockResetMove = false
				engine.speed.denominator = 1
				engine.speed.gravity = engine.speed.denominator
				engine.speed.areLine = 17
				engine.speed.are = engine.speed.areLine
				engine.speed.lineDelay = 42
				engine.speed.lockDelay = 18
				engine.speed.das = 6
			}
		}
	}

	/** Main routine for game setup screen */
	override fun onSetting(engine:GameEngine):Boolean {
		if(!engine.owner.replayMode) {
			// Configuration changes
			val change = updateCursor(engine, 2)

			if(change!=0) {
				engine.playSE("change")

				when(menuCursor) {
					0 -> {
						gameType += change
						if(gameType<0) gameType = GAMETYPE_MAX-1
						if(gameType>GAMETYPE_MAX-1) gameType = 0
						receiver.setBGSpd(owner, .5f+gameType*.4f, levelBG[startLevel])
					}
					1 -> {
						startLevel += change
						if(startLevel<0) startLevel = 15
						if(startLevel>15) startLevel = 0
						owner.bgMan.bg = levelBG[startLevel]
						receiver.setBGSpd(owner, .5f+gameType*.4f, levelBG[startLevel])
					}
					2 -> big = !big
				}
			}

			// Check for A button, when pressed this will begin the game
			if(menuTime<5) menuTime++ else if(engine.ctrl.isPush(Controller.BUTTON_A)) {
				engine.playSE("decide")
				return false
			}

			// Check for B button, when pressed this will shut down the game engine.
			if(engine.ctrl.isPush(Controller.BUTTON_B)) engine.quitFlag = true
		} else {
			engine.statc[3]++
			engine.statc[2] = -1

			return engine.statc[3]<60
		}

		return true
	}

	/** Renders game setup screen */
	override fun renderSetting(engine:GameEngine) {
		drawMenu(
			engine, receiver, 0, COLOR.BLUE, 0, "DIFFICULTY" to GAMETYPE_NAME[gameType], "Level" to startLevel, "BIG" to big
		)
	}

	private fun setBGM(lv:Int) {
		owner.musMan.bgm = when(lv) {
//			MAX_LEVEL -> BGM.GrandM(1)
			MAX_LEVEL+1 -> BGM.Silent
			MAX_LEVEL+2 -> BGM.Ending(if(gameType<3) 3 else 4)
			else -> BGM.RetroS(1+tableBGMLevel.count {it<=lv})
		}
	}
	/** This function will be called before the game actually begins (after
	 * Ready&Go screen disappears) */
	override fun startGame(engine:GameEngine) {
		if(engine.ending!=0) return
		engine.big = big
		special = true
		setBGM(startLevel)
		setSpeed(engine)
	}

	/** Ready */
	override fun onReady(engine:GameEngine):Boolean {
		if(engine.statc[0]==0)
			if(engine.ending==0) {
//				engine.framecolor = if(gameType==4) GameEngine.FRAME_COLOR_RED else GameEngine.FRAME_COLOR_WHITE
				totalNorma = MAX_LINES-startLevel*16
				engine.statistics.level = startLevel
			} else
				engine.nextPieceArrayID = GeneralUtil.createNextPieceArrayFromNumberString(STRING_POWERON_PATTERN)

		return false
	}

	/** Renders HUD (leaderboard or game statistics) */
	override fun renderLast(engine:GameEngine) {
		receiver.drawScoreFont(engine, 0, 0, name, color = COLOR.COBALT)
		receiver.drawScoreFont(engine, 0, 1, "(${GAMETYPE_NAME[gameType]} SPEED)", COLOR.COBALT)

		if(engine.stat==GameEngine.Status.SETTING||engine.stat==GameEngine.Status.RESULT&&!owner.replayMode) {
			// Leaderboard
			if(!owner.replayMode&&!big&&startLevel==0&&engine.ai==null) {
				val topY = if(receiver.nextDisplayType==2) 6 else 4
				receiver.drawScoreFont(engine, 2, topY-1, "SCORE LINE LV TIME", color = COLOR.BLUE)

				for(i in 0..<RANKING_MAX) {
					receiver.drawScoreGrade(engine, 0, topY+i, "%2d".format(i+1), COLOR.YELLOW)
					receiver.drawScoreNum(engine, 2, topY+i, "${rankingScore[gameType][i]}", i==rankingRank)
					receiver.drawScoreNum(engine, 8.8f, topY+i, "%3d".format(rankingLines[gameType][i]), i==rankingRank)
					receiver.drawScoreNum(engine, 13, topY+i, "%2d".format(rankingLevel[gameType][i]), i==rankingRank)
					receiver.drawScoreNum(engine, 15, topY+i, rankingTime[gameType][i].toTimeStr, i==rankingRank)
				}
			}
		} else {
			// Game statistics
			receiver.drawScoreFont(engine, 0, 3, "Score", COLOR.BLUE)
			receiver.drawScoreNum(engine, 5, 3, "+$lastScore")
			val scget = scDisp<engine.statistics.score
			receiver.drawScoreNum(engine, 0, 4, "$scDisp", scget, 2f)
			val num = lineSlot.fold(0) {s, it ->
				s+when {
					it==1 -> 1
					it==2 -> 10
					it==3 -> 100
					it>=4 -> 100000
					else -> 0
				}
			}
//			if(lineCount>=3)  5 else 1
			receiver.drawScoreBadges(engine, 2, 6, 200, num)

			receiver.drawScoreFont(engine, 0, 10, "LINE", COLOR.BLUE)
			receiver.drawScoreNum(engine, 0, 11, "%03d/%03d".format(engine.statistics.lines, totalNorma), scale = 2f)

			receiver.drawScoreFont(engine, 0, 13, "Level", COLOR.BLUE)
			var lvdem = 0
			if(rollTime>0)
				lvdem = rollTime*100/ROLLTIMELIMIT
			else if(engine.statistics.level<levelNorma.size) lvdem = norm*100/levelNorma[engine.statistics.level]
			if(lvdem<0) lvdem *= -1
			if(lvdem>=100) lvdem -= lvdem-lvdem%100
			receiver.drawScoreNum(engine, 5, 13, "%02d.%02d".format(engine.statistics.level, lvdem), scale = 2f)

			receiver.drawScoreFont(engine, 0, 14, "Time", COLOR.BLUE)
			receiver.drawScoreNum(engine, 0, 15, engine.statistics.time.toTimeStr, scale = 2f)

			// Roll 残り time
			if(rollTime>0) {
				val time = ROLLTIMELIMIT-rollTime
				receiver.drawScoreFont(engine, 0, 15, "FLASH BACK", COLOR.CYAN)
				receiver.drawScoreNum(engine, 0, 16, time.toTimeStr, time>0&&time<10*60, 2f)
			}
		}
	}

	/** This function will be called when the game timer updates */
	override fun onLast(engine:GameEngine) {
		super.onLast(engine)
		if(special&&(engine.ctrl.isPress(Controller.BUTTON_B)||engine.ctrl.isPress(Controller.BUTTON_E))) special = false
		// Update the meter
		if(engine.ending==0) {
			engine.meterValue = if(engine.statistics.level<levelNorma.size)
				1f*norm/levelNorma[engine.statistics.level]
			else 1f*engine.statistics.lines/totalNorma

			engine.meterColor = GameEngine.METER_COLOR_LEVEL
		} else
		// Ending
			if(engine.gameActive&&engine.statistics.level==17) {
				rollTime++

				// Time meter
				val remainRollTime = ROLLTIMELIMIT-rollTime
				engine.meterValue = remainRollTime*1f/ROLLTIMELIMIT
				engine.meterColor = GameEngine.METER_COLOR_LIMIT
				var bg = levelBG[levelBG.size-1]
				if(rollTime<=ROLLTIMELIMIT-3600) bg = levelBG[rollTime*MAX_LEVEL/(ROLLTIMELIMIT-3600)]
				//else owner.bgmStatus.bgm=Ending(1);

				if(owner.bgMan.nextBg!=bg) {
					owner.bgMan.nextBg = bg
				}
				// Roll 終了
				if(rollTime>=ROLLTIMELIMIT) {
					engine.statistics.level++
					engine.gameEnded()
					engine.resetStatc()
					engine.stat = GameEngine.Status.EXCELLENT
					if(special) {
						lastScore = 10000000
						engine.statistics.scoreBonus += lastScore
					}
				}
			}
	}

	/** Calculates line-clear score
	 * (This function will be called even if no lines are cleared) */
	override fun calcScore(engine:GameEngine, ev:ScoreEvent):Int {
		// Determines line-clear bonus

		val mult = tableScoreMult[gameType][engine.statistics.level]*10
		val li = ev.lines
		val pts = when {
			li>0&&engine.field.isEmpty -> 2000*tableBonusMult[engine.statistics.level]
			// Perfect clear bonus
			li==1 -> 5*mult // Single
			li==2 -> (if(engine.split) 30 else 20)*mult // Double
			li==3 -> (if(engine.split) 55 else 45)*mult // Triple
			li>=4 -> 100*mult // Four
			else -> 0
		}
		// Add score
		if(pts>0) {
			lastScore = pts
			engine.statistics.scoreLine += pts
		}
		if(engine.manualLock) {
			scDisp++
			if(engine.ruleOpt.harddropLock) engine.statistics.scoreHD++
			else engine.statistics.scoreSD++
		}
		if(li>0) {
			lineSlot[lineCount] = if(li>4) 4 else li
			lineCount++

			// Add lines
			norm += li
		}
		// Level up
		var lvup = false
		if(engine.statistics.level<MAX_LEVEL&&norm>=levelNorma[engine.statistics.level]||
			engine.statistics.level==MAX_LEVEL&&engine.statistics.lines>=totalNorma||
			engine.statistics.level==MAX_LEVEL+1
		)
			lvup = li>0

		if(lvup) {
			val newlevel = ++engine.statistics.level
			if(engine.ending==0)
				if(engine.statistics.lines>=totalNorma) engine.ending = 1
				else engine.playSE("levelup")
			if(newlevel!=MAX_LEVEL+1) setSpeed(engine)
			if(newlevel<levelBG.size-1) {
				owner.bgMan.nextBg = levelBG[newlevel]
			}
			norm = 0
			engine.meterValue = 0f
			setBGM(newlevel)
		}
		return pts
	}

	override fun renderLineClear(engine:GameEngine) {
		val num = when {
			engine.lineClearing==1 -> 1
			engine.lineClearing==2 -> 10
			engine.lineClearing==3 -> 100
			engine.lineClearing>=4 -> 100000
			else -> 0
		}*if(lineCount>=3) 5 else 1
		receiver.drawMenuBadges(engine, 2, engine.lastLineY-if(num>=100000) if(num>=500000) 3 else 1 else 0, num)
		receiver.drawMenuNum(engine, 4, engine.lastLineY, "$lastScore", COLOR.CYAN)

		if(engine.split) when(engine.lineClearing) {
			2 -> receiver.drawMenuFont(engine, 0f, engine.lastLinesY.minOf {it.average().toFloat()}, "SPLIT TWIN", COLOR.PURPLE)
			3 -> receiver.drawMenuFont(engine, 0f, engine.lastLinesY.minOf {it.average().toFloat()}, "1.2.TRIPLE", COLOR.PURPLE)
		}
	}

	override fun lineClearEnd(engine:GameEngine):Boolean {
		if(lineCount>=2&&lineSlot[0]!=lineSlot[1]) {
			engine.playSE("b2b_end")
			lineCount = 1
		} else if(lineCount>=3) {
			var pts = 0
			if(lineSlot[0]==lineSlot[1]&&lineSlot[1]==lineSlot[2]) {
				val y = lineSlot[0]
				if(y==1) {
					pts = 5
					engine.playSE("b2b_start")
				} else {
					engine.playSE("b2b_combo")
					if(y>=4) {
						pts = 10000
						engine.playSE("combo")
					} else if(y==2) pts = 1000
					else if(y==3) pts = 5000

				}
				lineCount = 0
			} else {
				if(lineSlot[0]==lineSlot[1]) engine.playSE("b2b_end")
				lineCount = 1
			}
			if(pts>0) {
				pts *= 10*tableBonusMult[engine.statistics.level]
				lastScore = pts
				engine.statistics.scoreBonus += pts
				receiver.addScore(engine, engine.fieldWidth/2, engine.lastLineY, pts, COLOR.RAINBOW)
			}
		}
		return false
	}

	override fun onMove(engine:GameEngine):Boolean {
		if(lineCount<lineSlot.count {it>0}) {
			lineSlot[0] = if(lineCount>0) lineSlot.last {it>0} else 0
			lineSlot[1] = 0
			lineSlot[2] = 0
		}
		return super.onMove(engine)
	}
	/** This function will be called when soft-drop is used */
	override fun afterSoftDropFall(engine:GameEngine, fall:Int) {
		engine.statistics.scoreSD += fall
		scDisp += fall
	}

	/** This function will be called when hard-drop is used */
	override fun afterHardDropFall(engine:GameEngine, fall:Int) {
		engine.statistics.scoreHD += fall
		scDisp += fall
	}

	override fun onEndingStart(engine:GameEngine):Boolean {
		val time = engine.statc[0]>=engine.lineDelay+2
		if(time) {
			engine.ending = 2
			engine.resetStatc()
			engine.field.reset()
			engine.nowPieceObject = null
			engine.stat = GameEngine.Status.CUSTOM
		}
		return time
	}

	override fun onCustom(engine:GameEngine):Boolean {
		engine.nextPieceArrayID = GeneralUtil.createNextPieceArrayFromNumberString(STRING_POWERON_PATTERN)
		engine.nextPieceArrayObject = emptyList()
		engine.holdPieceObject = null
		engine.nextPieceCount = 0
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

	/*	override fun blockBreak(engine:GameEngine, blk:Map<Int, Map<Int, Block>>):Boolean {
			engine.owner.receiver.efxFG.add()
			return true
		}*/

	override fun renderCustom(engine:GameEngine) {
		val col = COLOR.WHITE

		val cY = (engine.fieldHeight-1)
		receiver.drawMenuFont(engine, 0f, cY/3f, "EXCELLENT!", COLOR.ORANGE, 1f)
		if(engine.statc[0]>=100) {
			receiver.drawMenuFont(engine, 2f, 8f, "BUT...", col)
			receiver.drawMenuFont(engine, -.25f, 9f, "THIS IS NOT", col)
			receiver.drawMenuFont(engine, .5f, 10f, "OVER YET!", col)
		}
	}

	override fun renderExcellent(engine:GameEngine) {
		val col = COLOR.WHITE

		receiver.drawMenuFont(engine, -.5f, 9f, "YOU REACHED", col)
		receiver.drawMenuFont(engine, -.5f, 10f, "THE EDGE OF", col)
		receiver.drawMenuFont(engine, -.5f, 11f, "THE JOURNEY", col)
		if(special) {
			receiver.drawMenuFont(engine, 0f, 13f, "C.C.W.ONLY", col)
			receiver.drawMenuFont(engine, 2.5f, 14f, "BONUS", col)
			receiver.drawMenuFont(
				engine, .5f, 16f, "+ 10000000", when {
					engine.statc[0]%4==0 -> COLOR.YELLOW
					engine.statc[0]%2==0 -> col
					else -> COLOR.ORANGE
				}
			)
		}
	}

	/** Renders game result screen */
	override fun renderResult(engine:GameEngine) {
		receiver.drawMenuFont(engine, 0, 1, "PLAY DATA", COLOR.ORANGE)

		drawResultStats(
			engine, receiver, 3, COLOR.BLUE, Statistic.SCORE, Statistic.LINES, Statistic.LEVEL, Statistic.TIME
		)
		drawResultRank(engine, receiver, 11, COLOR.BLUE, rankingRank)
	}

	/** This function will be called when the replay data is going to be saved */
	override fun saveReplay(engine:GameEngine, prop:CustomProperties):Boolean {
		saveSetting(engine, prop)

		// Checks/Updates the ranking
		if(!owner.replayMode&&!big&&engine.ai==null) {
			updateRanking(engine.statistics.score, engine.statistics.level, engine.statistics.lines, engine.statistics.time, gameType)

			if(rankingRank!=-1) return true
		}
		return false
	}

	/** Load the settings */
	override fun loadSetting(engine:GameEngine, prop:CustomProperties, ruleName:String, playerID:Int) {
		startLevel = prop.getProperty("retromodern.startLevel", 0)
		gameType = prop.getProperty("retromodern.gameType", 0)
		big = prop.getProperty("retromodern.big", false)
		version = prop.getProperty("retromodern.version", 0)
	}

	/** Save the settings */
	override fun saveSetting(engine:GameEngine, prop:CustomProperties, ruleName:String, playerID:Int) {
		prop.setProperty("retromodern.startLevel", startLevel)
		prop.setProperty("retromodern.gameType", gameType)
		prop.setProperty("retromodern.big", big)
		prop.setProperty("retromodern.version", version)
	}

	/** Update the ranking */
	private fun updateRanking(sc:Long, lv:Int, li:Int, time:Int, type:Int) {
		rankingRank = checkRanking(sc, lv, li, time, type)

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
	private fun checkRanking(sc:Long, lv:Int, li:Int, time:Int, type:Int):Int {
		for(i in 0..<RANKING_MAX)
			if(sc>rankingScore[type][i]) return i
			else if(sc==rankingScore[type][i]&&lv>rankingLines[type][i]) return i
			else if(sc==rankingScore[type][i]&&lv==rankingLines[type][i]&&li>rankingLines[type][i]) return i
			else if(sc==rankingScore[type][i]&&li==rankingLines[type][i]&&time<rankingTime[type][i]) return i

		return -1
	}

	companion object {
		/** Current version */
		private const val CURRENT_VERSION = 2

		/** Poweron Pattern */
		private const val STRING_POWERON_PATTERN =
			"4040050165233516506133350555213560141520224542633206134255165200333560031332022463366645230432611435533550326251231351500244220365666413154321122014634420132506140113461064400566344110153223400634050546214410040214650102256233133116353263111335043461206211262231565235306361150440653002222453302523255563545455656660124120450663502223206465164461126135621055103645066644052535021110020361422122352566156434351304346510363640453452505655142263102605202216351615031650050464160613325366023413453036542441246445101562252141201460505435130040221311400543416041660644410106141444041454511600413146353206260246251556635262420616451361336106153451563316660054255631510320566516465265421144640513424316315421664414026440165341010302443625101652205230550602002033120044344034100160442632436645325512265351205642343342312121523120061530234443062420033310461403306365402313212656105101254352514216210355230014040335464640401464125332132315552404146634264364245513600336065666305002023203545052006445544450440460"

		/** Gravity table */
		private val tableDenominator = listOf(
			listOf(24, 15, 10, 6, 20, 5, 5, 4, 3, 3, 2, 2, 2, 2, 2, 1),
			listOf(24, 15, 10, 4, 20, 3, 2, 2, 2, 2, 1, 1, 1, 1, 1, 1),
			listOf(15, 10, +5, 3, 20, 2, 2, 1, 1, 1, 1, 1, 1, 1, 1, 1),
			listOf(+1, +1, +1, 1, 20, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1),
			listOf(1, 1, 1, -2, 20, 1, 1, -2, -3, -4, -4, -4, -5, -4, -3, 0)
		)
		/** Lock delay table */
		private val tableLockDelay = listOf(
			listOf(44, 39, 34, 30, 39, 29, 29, 29, 28, 28, 28, 28, 28, 28, 28, 24),
			listOf(44, 39, 34, 30, 39, 29, 29, 29, 28, 28, 27, 26, 25, 24, 20, 22),
			listOf(39, 34, 32, 30, 39, 28, 28, 27, 26, 25, 24, 24, 24, 22, 20, 20),
			listOf(24, 24, 24, 30, 39, 24, 24, 24, 24, 24, 24, 24, 24, 20, 20, 19),
			listOf(24, 24, 24, 30, 39, 24, 24, 25, 25, 25, 24, 23, 23, 23, 23, 25)
		)
		/** ARE table */
		private val tableARE = listOf(
			listOf(31, 28, 26, 26, 28, 26, 26, 26, 26, 26, 26, 26, 26, 25, 26, 26),
			listOf(31, 28, 26, 26, 28, 26, 26, 26, 26, 26, 26, 26, 25, 24, 26, 25),
			listOf(28, 28, 26, 26, 28, 26, 26, 26, 26, 26, 26, 25, 24, 23, 26, 24),
			listOf(26, 26, 26, 26, 28, 26, 26, 26, 26, 26, 25, 24, 23, 22, 26, 22),
			listOf(26, 25, 24, 24, 28, 25, 24, 24, 24, 24, 23, 22, 22, 21, 21, 20)
		)

		/** Score multiply table */
		private val tableScoreMult = listOf(
			listOf(1, 2, 3, 4, 5, 6, +6, +6, +8, +8, 10, 10, 10, 10, 10, 11, 12, 13),
			listOf(1, 2, 3, 4, 5, 6, +7, +8, +9, 10, 11, 12, 13, 14, 15, 16, 20, 20),
			listOf(2, 3, 4, 5, 6, 7, +8, +9, 10, 10, 11, 12, 13, 14, 15, 16, 20, 21),
			listOf(5, 5, 6, 6, 8, 8, 10, 10, 10, 10, 11, 12, 13, 14, 15, 16, 21, 22),
			listOf(5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 25, 25)
		)
		private val tableBonusMult = listOf(1, 1, 1, 1, 1, 2, 2, 2, 2, 2, 3, 3, 3, 3, 3, 5, 5, 10)
		/** Lines until level up occers */
		private val levelNorma = listOf(6, 6, 7, 9, 6, 9, 9, 9, 10, 10, 20, 16, 16, 16, 16)
		private val tableBGMLevel = listOf(5, 9, 12, 15)

		private val levelBG = listOf(
			-1, -2, -3, -4, -5, -6,
			-7, -8, -9, -10, -11, -12,
			-13, -12, -14, -15, 29, -15
		)
		/** Max level */
		private const val MAX_LEVEL = 15
		private const val MAX_LINES = 300
		/** Name of game types */
		private val GAMETYPE_NAME = listOf("EASY", "NORMAL", "INTENSE", "HARD", "EXTRA")

		/** Number of game type */
		private const val GAMETYPE_MAX = 5

		/** Number of ranking records */
		private const val RANKING_MAX = 13

		/** LV17 roll time */
		private const val ROLLTIMELIMIT = 12000
	}
}
