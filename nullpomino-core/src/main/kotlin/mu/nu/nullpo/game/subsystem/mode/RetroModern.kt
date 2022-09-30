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
	private var totalnorma = 0

	/** Selected game type */
	private var gametype = 0

	/** Selected starting level */
	private var startLevel = 0

	/** Ending Level timer */
	private var rolltime = 0

	/** Amount of lines cleared (It will be reset when the level increases) */
	private var norm = 0

	private var lineslot = IntArray(3)
	private var linecount = 0

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
		lastscore = 0
		rolltime = 0
		norm = 0
		special = false

		lineslot = IntArray(3)
		linecount = 0

		rankingRank = -1
		rankingScore.forEach {it.fill(0)}
		rankingLevel.forEach {it.fill(0)}
		rankingLines.forEach {it.fill(0)}
		rankingTime.forEach {it.fill(0)}
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
			loadSetting(owner.replayProp, engine)
		if(startLevel>15) startLevel = 15
		engine.owner.bgMan.bg = levelBG[startLevel]
		engine.frameColor = GameEngine.FRAME_SKIN_HEBO
	}

	/** Set the gravity speed
	 * @param engine GameEngine object
	 */
	private fun setSpeed(engine:GameEngine) {
		val lv = maxOf(0, engine.statistics.level)

		engine.ruleOpt.lockResetMove = gametype==4
		engine.ruleOpt.lockResetSpin = gametype==4
		engine.ruleOpt.lockResetWallkick = gametype==4
		engine.ruleOpt.lockResetFall = true
		engine.ruleOpt.softdropLock = true
		engine.ruleOpt.softdropMultiplyNativeSpeed = false
		engine.ruleOpt.softdropGravitySpeedLimit = true
		engine.ruleOpt.softdropSpeed = 1f
		engine.owSDSpd = -1
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
	override fun onSetting(engine:GameEngine):Boolean {
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
						startLevel += change
						if(startLevel<0) startLevel = 15
						if(startLevel>15) startLevel = 0
						engine.owner.bgMan.bg = levelBG[startLevel]
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
			engine, receiver, 0, COLOR.BLUE, 0, "DIFFICULTY" to GAMETYPE_NAME[gametype], "Level" to startLevel, "BIG" to big
		)
	}

	private fun setBGM(lv:Int) {
		owner.musMan.bgm = when(lv) {
			MAX_LEVEL -> BGM.GrandM(1)
			MAX_LEVEL+1 -> BGM.Silent
			MAX_LEVEL+2 -> BGM.Ending(if(gametype<3) 3 else 4)
			else -> BGM.RetroS(tableBGMlevel.count {it<=lv})
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
//				engine.framecolor = if(gametype==4) GameEngine.FRAME_COLOR_RED else GameEngine.FRAME_COLOR_WHITE
				totalnorma = MAX_LINES-startLevel*16
				engine.statistics.level = startLevel
			} else
				engine.nextPieceArrayID = GeneralUtil.createNextPieceArrayFromNumberString(STRING_POWERON_PATTERN)

		return false
	}

	/** Renders HUD (leaderboard or game statistics) */
	override fun renderLast(engine:GameEngine) {
		receiver.drawScoreFont(engine, 0, 0, "RETRO MODERN", color = COLOR.COBALT)
		receiver.drawScoreFont(engine, 0, 1, "(${GAMETYPE_NAME[gametype]} SPEED)", COLOR.COBALT)

		if(engine.stat==GameEngine.Status.SETTING||engine.stat==GameEngine.Status.RESULT&&!owner.replayMode) {
			// Leaderboard
			if(!owner.replayMode&&!big&&startLevel==0&&engine.ai==null) {
				val scale = if(receiver.nextDisplayType==2) .5f else 1f
				val topY = if(receiver.nextDisplayType==2) 6 else 4
				receiver.drawScoreFont(engine, 3, topY-1, "SCORE LINE LV TIME", color = COLOR.BLUE, scale = scale)

				for(i in 0 until RANKING_MAX) {
					receiver.drawScoreGrade(engine, 0, topY+i, String.format("%2d", i+1), COLOR.YELLOW, scale)
					receiver.drawScoreNum(engine, 3, topY+i, "${rankingScore[gametype][i]}", i==rankingRank, scale)
					receiver.drawScoreNum(
						engine, 9, topY+i, String.format("%2d", rankingLines[gametype][i]), i==rankingRank, scale
					)
					receiver.drawScoreNum(
						engine, 12, topY+i, String.format("%3d", rankingLevel[gametype][i]), i==rankingRank, scale
					)
					receiver.drawScoreNum(
						engine, 16, topY+i, rankingTime[gametype][i].toTimeStr, i==rankingRank, scale
					)
				}
			}
		} else {
			// Game statistics
			receiver.drawScoreFont(engine, 0, 3, "Score", COLOR.BLUE)
			receiver.drawScoreNum(engine, 5, 3, "+$lastscore")
			val scget = scDisp<engine.statistics.score
			receiver.drawScoreNum(engine, 0, 4, "$scDisp", scget, 2f)

			receiver.drawScoreFont(engine, 0, 7, "LINE", COLOR.BLUE)
			receiver.drawScoreNum(engine, 0, 8, String.format("%03d/%03d", engine.statistics.lines, totalnorma), scale = 2f)

			receiver.drawScoreFont(engine, 0, 10, "Level", COLOR.BLUE)
			var lvdem = 0
			if(rolltime>0)
				lvdem = rolltime*100/ROLLTIMELIMIT
			else if(engine.statistics.level<levelNorma.size) lvdem = norm*100/levelNorma[engine.statistics.level]
			if(lvdem<0) lvdem *= -1
			if(lvdem>=100) lvdem -= lvdem-lvdem%100
			receiver.drawScoreNum(engine, 5, 10, String.format("%02d.%02d", engine.statistics.level, lvdem), scale = 2f)

			receiver.drawScoreFont(engine, 0, 11, "Time", COLOR.BLUE)
			receiver.drawScoreNum(engine, 0, 12, engine.statistics.time.toTimeStr, scale = 2f)

			// Roll 残り time
			if(rolltime>0) {
				val time = ROLLTIMELIMIT-rolltime
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
			if(engine.statistics.level<levelNorma.size)
				engine.meterValue = 1f*norm/levelNorma[engine.statistics.level]
			else
				engine.meterValue = 1f*engine.statistics.lines/totalnorma

			engine.meterColor = GameEngine.METER_COLOR_LEVEL

		} else
		// Ending
			if(engine.gameActive&&engine.statistics.level==17) {
				rolltime++

				// Time meter
				val remainRollTime = ROLLTIMELIMIT-rolltime
				engine.meterValue = remainRollTime*1f/ROLLTIMELIMIT
				engine.meterColor = GameEngine.METER_COLOR_LIMIT
				var bg = levelBG[levelBG.size-1]
				if(rolltime<=ROLLTIMELIMIT-3600) bg = levelBG[rolltime*MAX_LEVEL/(ROLLTIMELIMIT-3600)]
				//else owner.bgmStatus.bgm=Ending(1);

				if(owner.bgMan.fadebg!=bg) {
					owner.bgMan.fadebg = bg
					owner.bgMan.fadecount = 0
					owner.bgMan.fadesw = true
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
	override fun calcScore(engine:GameEngine, ev:ScoreEvent):Int {
		// Determines line-clear bonus
		var pts = 0
		val mult = tableScoreMult[gametype][engine.statistics.level]*10
		val li = ev.lines
		if(li==1)
			pts = 5*mult // Single
		else if(li==2)
			pts = (if(engine.split) 30 else 20)*mult // Double
		else if(li==3)
			pts = (if(engine.split) 55 else 45)*mult // Triple
		else if(li>=4) pts = 100*mult // Four
		if(li>0&&engine.field.isEmpty)
		// Perfect clear bonus
			pts = 2000*tableBonusMult[engine.statistics.level]
		// Add score
		if(pts>0) {
			lastscore = pts
			engine.statistics.scoreLine += pts
		}
		if(engine.manualLock) {
			scDisp++
			if(engine.ruleOpt.harddropLock) engine.statistics.scoreHD++
			else engine.statistics.scoreSD++
		}
		if(li>0) {
			lineslot[linecount] = if(li>4) 4 else li
			linecount++

			// Add lines
			norm += li
		}
		// Level up
		var lvup = false
		if(engine.statistics.level<MAX_LEVEL&&norm>=levelNorma[engine.statistics.level]||
			engine.statistics.level==MAX_LEVEL&&engine.statistics.lines>=totalnorma||
			engine.statistics.level==MAX_LEVEL+1
		)
			lvup = li>0

		if(lvup) {
			val newlevel = ++engine.statistics.level
			if(engine.ending==0)
				if(engine.statistics.lines>=totalnorma)
					engine.ending = 1
				else
					engine.playSE("levelup")


			if(newlevel!=MAX_LEVEL+1) setSpeed(engine)

			if(newlevel<levelBG.size-1) {
				owner.bgMan.fadecount = 0
				owner.bgMan.fadebg = levelBG[newlevel]
				owner.bgMan.fadesw = true
			}
			norm = 0
			engine.meterValue = 0f
			setBGM(newlevel)
		}
		return pts
	}

	override fun renderLineClear(engine:GameEngine) {
		var num = 0

		when {
			engine.lineClearing==1 -> num = 1
			engine.lineClearing==2 -> num = 10
			engine.lineClearing==3 -> num = 100
			engine.lineClearing>=4 -> num = 100000
		}
		if(linecount>=3) num *= 5
		receiver.drawMenuBadges(engine, 2, engine.lastLine-if(num>=100000) if(num>=500000) 3 else 1 else 0, num)
		receiver.drawMenuNum(engine, 4, engine.lastLine, "$lastscore", COLOR.CYAN)

		if(engine.split) when(engine.lineClearing) {
			2 -> receiver.drawMenuFont(engine, 0, engine.lastLines[0], "SPLIT TWIN", COLOR.PURPLE)
			3 -> receiver.drawMenuFont(engine, 0, engine.lastLines[0], "1.2.TRIPLE", COLOR.PURPLE)
		}
	}

	override fun lineClearEnd(engine:GameEngine):Boolean {
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
		return false
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
		saveSetting(prop, engine)

		// Checks/Updates the ranking
		if(!owner.replayMode&&!big&&engine.ai==null) {
			updateRanking(engine.statistics.score, engine.statistics.level, engine.statistics.lines, engine.statistics.time, gametype)

			if(rankingRank!=-1) return true
		}
		return false
	}

	/** Load the settings */
	override fun loadSetting(prop:CustomProperties, ruleName:String, playerID:Int) {
		startLevel = prop.getProperty("retromodern.startLevel", 0)
		gametype = prop.getProperty("retromodern.gametype", 0)
		big = prop.getProperty("retromodern.big", false)
		version = prop.getProperty("retromodern.version", 0)
	}

	/** Save the settings */
	override fun saveSetting(prop:CustomProperties, ruleName:String, playerID:Int) {
		prop.setProperty("retromodern.startLevel", startLevel)
		prop.setProperty("retromodern.gametype", gametype)
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
		for(i in 0 until RANKING_MAX)
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
		private val tableDenominator = arrayOf(
			intArrayOf(24, 15, 10, 6, 20, 5, 5, 4, 3, 3, 2, 2, 2, 2, 2, 1),
			intArrayOf(24, 15, 10, 4, 20, 3, 2, 2, 2, 2, 1, 1, 1, 1, 1, 1),
			intArrayOf(15, 6, 4, 3, 20, 2, 2, 1, 1, 1, 1, 1, 1, 1, 1, 1),
			intArrayOf(1, 1, 1, 1, 20, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1),
			intArrayOf(1, 1, 1, -2, 20, 1, 1, -2, -3, -4, -4, -4, -5, -4, -3, 0)
		)
		/** Lock delay table */
		private val tableLockDelay = arrayOf(
			intArrayOf(44, 39, 34, 30, 39, 29, 29, 29, 28, 28, 28, 28, 28, 28, 28, 24),
			intArrayOf(44, 39, 34, 30, 39, 29, 29, 29, 28, 28, 24, 24, 24, 20, 20, 19),
			intArrayOf(39, 30, 30, 29, 39, 28, 28, 24, 24, 24, 24, 24, 24, 20, 20, 19),
			intArrayOf(24, 24, 24, 30, 39, 24, 24, 24, 24, 24, 24, 24, 24, 20, 20, 19),
			intArrayOf(24, 24, 24, 30, 39, 24, 24, 25, 25, 25, 24, 23, 23, 23, 23, 25)
		)
		/** ARE table */
		private val tableARE = arrayOf(
			intArrayOf(31, 28, 26, 26, 28, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26),
			intArrayOf(31, 28, 26, 26, 28, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26),
			intArrayOf(28, 28, 26, 26, 28, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26),
			intArrayOf(26, 26, 26, 26, 28, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26),
			intArrayOf(26, 26, 26, 26, 28, 25, 25, 24, 24, 23, 23, 22, 22, 21, 21, 20)
		)

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
			intArrayOf(5, 5, 6, 7, 8, 9, 10, 10, 11, 12, 13, 14, 14, 15, 15, 16, 25, 25)
		)
		private val tableBonusMult = intArrayOf(1, 1, 1, 1, 1, 2, 2, 2, 2, 2, 3, 3, 3, 3, 3, 5, 5, 10)

		private val levelBG = intArrayOf(
			0, 1, 2, 3, 4, 5,
			6, 7, 8, 9, 14, 19,
			10, 11, 12, 13, 29, 36
		)
		private val tableBGMlevel = intArrayOf(0, 4, 6, 8, 10, 11, 13, 15)
		/** Name of game types */
		private val GAMETYPE_NAME = arrayOf("EASY", "NORMAL", "INTENSE", "HARD", "EXTRA")

		/** Number of game type */
		private const val GAMETYPE_MAX = 5

		/** Number of ranking records */
		private const val RANKING_MAX = 13

		/** LV17 roll time */
		private const val ROLLTIMELIMIT = 12000
	}
}
