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
import mu.nu.nullpo.game.component.Piece.Companion.createQueueFromIntStr
import mu.nu.nullpo.game.component.SpeedParam
import mu.nu.nullpo.game.event.EventReceiver
import mu.nu.nullpo.game.event.EventReceiver.COLOR
import mu.nu.nullpo.game.event.ScoreEvent
import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.game.subsystem.mode.menu.BooleanMenuItem
import mu.nu.nullpo.game.subsystem.mode.menu.DelegateMenuItem
import mu.nu.nullpo.game.subsystem.mode.menu.LevelMenuItem
import mu.nu.nullpo.game.subsystem.mode.menu.MenuList
import mu.nu.nullpo.game.subsystem.mode.menu.StringsMenuItem
import mu.nu.nullpo.gui.common.BaseFont
import mu.nu.nullpo.util.CustomProperties
import mu.nu.nullpo.util.GeneralUtil.toTimeStr

/** RETRO MODERN mode (Based from NAOMI, build by Venom_Nhelv 20180131-2020 */
class RetroModern:AbstractMode() {
	private var totalNorma = 0

	/** Ending Level timer */
	private var rollTime = 0

	/** Amount of lines cleared (It will be reset when the level increases) */
	private var norm = 0

	private var lineSlot = IntArray(4)
	private var lineCount = 0

	private var special = false

	private val itemMode = StringsMenuItem("gametype", "DIFFICULTY", COLOR.BLUE, 0, GAMETYPE_NAME)
	/** Selected game type */
	private var gameType:Int by DelegateMenuItem(itemMode)

	private val itemLevel = LevelMenuItem("startlevel", "LEVEL", COLOR.BLUE, 0, 0..15, false, true)
	/** Selected starting level */
	private var startLevel:Int by DelegateMenuItem(itemLevel)
	private val itemBig = BooleanMenuItem("big", "BIG", COLOR.BLUE, false)
	/** BigMode */
	private var big:Boolean by DelegateMenuItem(itemBig)

	override val menu = MenuList("retromodern", itemMode, itemLevel, itemBig)

	/** Version of this mode */
	private var version = 0

	/** Your place on leaderboard (-1: out of rank) */
	private var rankingRank = 0

	/** Ranking records */
	private val rankingScore:List<MutableList<Long>> = List(GAMETYPE_MAX) {MutableList(RANKING_MAX) {0L}}
	private val rankingLevel:List<MutableList<Int>> = List(GAMETYPE_MAX) {MutableList(RANKING_MAX) {0}}
	private val rankingLines:List<MutableList<Int>> = List(GAMETYPE_MAX) {MutableList(RANKING_MAX) {0}}
	private val rankingTime:List<MutableList<Int>> = List(GAMETYPE_MAX) {MutableList(RANKING_MAX) {0}}

	override val propRank
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

		lineSlot.fill(0)
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
		engine.statistics.level = startLevel
		setSpeed(engine)
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
		if(lv<=MAX_LEVEL) {
			receiver.setBGSpd(owner, .5f+gameType*.4f)
			engine.speed.replace(tableSpd[gameType][lv])
		} else if(lv==MAX_LEVEL+1)
			engine.speed.replace(SpeedParam(1, 24, 31, 57, 44, 15))
		else {
			engine.ruleOpt.lockResetMove = gameType==0||gameType==4
			engine.speed.replace(
				if(gameType==4) SpeedParam(-1, 1, 15, 25, 22, 10)
				else SpeedParam(1, 1, 17, 42, 21-gameType, 6)
			)
		}

	}

	/** Main routine for game setup screen */
	override fun onSettingChanged(engine:GameEngine) {
		super.onSettingChanged(engine)
		engine.statistics.level = startLevel
		totalNorma = MAX_LINES-startLevel*16
		owner.bgMan.bg = levelBG[startLevel]
		receiver.setBGSpd(owner, .5f+gameType*.4f, levelBG[startLevel])
		setSpeed(engine)
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
				engine.nextPieceArrayID = createQueueFromIntStr(STRING_POWERON_PATTERN)

		return false
	}

	override fun renderFirst(engine:GameEngine) {
		if(engine.ending==2&&rollTime>ROLLTIMELIMIT-3600) receiver.drawStaffRoll(engine, (rollTime-ROLLTIMELIMIT+3600)/3600f)
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
			val scGet = scDisp<engine.statistics.score
			receiver.drawScoreNum(engine, 0, 4, "$scDisp", scGet, 2f)
//			if(lineCount>=3)  5 else 1
			receiver.drawScoreFont(engine, 0, 6, "Bonus\n${BaseFont.CURSOR}", COLOR.BLUE)
			receiver.drawDirectFont(receiver.scoreX(engine,1)+lineSlot.fold(0f) {s, it ->
				s+when {
					it in 1..3 -> 10
					it>=4 -> 32
					else -> 0
				}
			}, receiver.scoreY(engine,7), BaseFont.DOWN_S, COLOR.BLUE)
			receiver.drawScoreBadges(engine, 1, 7, 200, lineSlot.fold(0) {s, it ->
				s+when {
					it==1 -> 1
					it==2 -> 10
					it==3 -> 100
					it>=4 -> 100000
					else -> 0
				}
			})

			receiver.drawScoreFont(engine, 0, 10, "LINE", COLOR.BLUE)
			receiver.drawScoreNum(engine, 0, 11, "%03d/%03d".format(engine.statistics.lines, totalNorma), scale = 2f)

			receiver.drawScoreFont(engine, 0, 13, "Level", COLOR.BLUE)
			val lvDem = (if(rollTime>0) rollTime*100/ROLLTIMELIMIT
			else if(engine.statistics.level<levelNorma.size) norm*100/levelNorma[engine.statistics.level]
			else 0)%100
			receiver.drawScoreNum(engine, 5, 13, "%02d.%02d".format(engine.statistics.level, lvDem), scale = 2f)

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
			lineSlot[minOf(lineSlot.lastIndex, lineCount)] = if(li>4) 4 else li
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
			val newLv = ++engine.statistics.level
			if(engine.ending==0)
				if(engine.statistics.lines>=totalNorma) engine.ending = 1
				else engine.playSE("levelup")
			if(newLv!=MAX_LEVEL+1) setSpeed(engine)
			if(newLv<levelBG.size-1) owner.bgMan.nextBg = levelBG[newLv]
			norm = 0
			engine.meterValue = 0f
			setBGM(newLv)
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
		/*if(lineCount>=2&&lineSlot[0]!=lineSlot[1]) {
			engine.playSE("b2b_end")
			lineCount = 1
		} else*/
		fun bonus(engine:GameEngine, pts:Int) {
			val pts1 = pts*10*tableBonusMult[engine.statistics.level]
			lastScore = pts1
			engine.statistics.scoreBonus += pts1
			receiver.addScore(engine, engine.fieldWidth/2, engine.lastLineY, pts1, COLOR.RAINBOW)
		}

		val slot = lineSlot.filter {it>0}
		if(slot.size>=3) {
			if(slot.distinct().size<=1) {
				val y = lineSlot[0]
				if(y==1) {
					bonus(engine, 5)
					engine.playSE("b2b_start")
				} else {
					engine.playSE("b2b_combo")
					bonus(
						engine, when {
							y>=4 -> {
								engine.playSE("combo")
								10000
							}
							y==2 -> 1000
							y==3 -> 5000
							else -> 0
						}
					)
				}
				lineCount = 0
			} else if(slot.size>=4&&slot.distinct().size>=slot.size) {
				engine.playSE("b2b_combo")
				engine.playSE("combo")
				bonus(engine, 13500)
				lineCount = 0
			} else if(slot.distinct().size==slot.size-1) {
				engine.playSE("b2b_end")
				lineCount = 1
			}
		}

		return false
	}

	override fun onMove(engine:GameEngine):Boolean {
		if(lineCount<lineSlot.count {it>0}) {
			(if(lineCount>0) lineSlot.last {it>0} else 0).let {
				lineSlot.fill(0)
				lineSlot[0] = it
			}
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
		engine.nextPieceArrayID = createQueueFromIntStr(STRING_POWERON_PATTERN)
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

		receiver.drawMenuFont(engine, -2f, +11f, "YOU'VE REACHED", col)
		receiver.drawMenuFont(engine, -.5f, 12f, "THE EDGE OF", col)
		receiver.drawMenuFont(engine, -1f, 13f, "THIS JOURNEY", col)
		if(special) {
			receiver.drawMenuFont(engine, 0f, 2f, "C.C.W.ONLY", col)
			receiver.drawMenuFont(engine, 2.5f, 3f, "BONUS", col)
			receiver.drawMenuNum(
				engine, .5f, 4f, "+ 10000000", when {
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
			updateRanking(
				engine.statistics.score,
				engine.statistics.level,
				engine.statistics.lines,
				engine.statistics.time,
				gameType
			)

			if(rankingRank!=-1) return true
		}
		return false
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
			else if(sc==rankingScore[type][i]&&lv>rankingLevel[type][i]) return i
			else if(sc==rankingScore[type][i]&&lv==rankingLevel[type][i]&&li>rankingLines[type][i]) return i
			else if(sc==rankingScore[type][i]&&li==rankingLines[type][i]&&time<rankingTime[type][i]) return i

		return -1
	}

	companion object {
		/** Current version */
		private const val CURRENT_VERSION = 2

		/** Poweron Pattern */
		private const val STRING_POWERON_PATTERN =
			"4040050165233516506133350555213560141520224542633206134255165200333560031332022463366645230432611435533550326251231351500244220365666413154321122014634420132506140113461064400566344110153223400634050546214410040214650102256233133116353263111335043461206211262231565235306361150440653002222453302523255563545455656660124120450663502223206465164461126135621055103645066644052535021110020361422122352566156434351304346510363640453452505655142263102605202216351615031650050464160613325366023413453036542441246445101562252141201460505435130040221311400543416041660644410106141444041454511600413146353206260246251556635262420616451361336106153451563316660054255631510320566516465265421144640513424316315421664414026440165341010302443625101652205230550602002033120044344034100160442632436645325512265351205642343342312121523120061530234443062420033310461403306365402313212656105101254352514216210355230014040335464640401464125332132315552404146634264364245513600336065666305002023203545052006445544450440460"

		private val tableSpd = listOf(
			LevelData(
				gravity = listOf(1), listOf(24, 15, 10, 6, 20, 5, 5, 4, 3, 3, 2, 2, 2, 2, 2, 1),
				listOf(31, 28, 26, 26, 28, 26, 26, 26, 26, 26, 26, 26, 26, 25, 26, 26),
				lineDelay = listOf(57), lockDelay = listOf(44, 39, 34, 30, 39, 29, 29, 29, 28, 28, 28, 28, 28, 28, 28, 24),
			),
			LevelData(
				gravity = listOf(1), listOf(24, 15, 10, 4, 20, 3, 2, 2, 2, 2, 1, 1, 1, 1, 1, 1),
				listOf(31, 28, 26, 26, 28, 26, 26, 26, 26, 26, 26, 26, 25, 24, 26, 25),
				lineDelay = listOf(48), lockDelay = listOf(44, 39, 34, 30, 39, 29, 29, 29, 28, 28, 27, 26, 25, 24, 20, 22),
			),
			LevelData(
				gravity = listOf(1), listOf(15, 10, +5, 3, 20, 2, 2, 1, 1, 1, 1, 1, 1, 1, 1, 1),
				listOf(28, 28, 26, 26, 28, 26, 26, 26, 26, 26, 26, 25, 24, 23, 26, 24),
				lineDelay = listOf(39), lockDelay = listOf(39, 34, 32, 30, 39, 28, 28, 27, 26, 25, 24, 24, 24, 22, 20, 20),
			),
			LevelData(
				gravity = listOf(1), listOf(+1, +1, +1, 1, 20, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1),
				listOf(28, 28, 26, 26, 28, 26, 26, 26, 26, 26, 26, 25, 24, 23, 26, 24),
				lineDelay = listOf(30), lockDelay = listOf(24, 24, 24, 30, 39, 24, 24, 24, 24, 24, 24, 24, 24, 20, 20, 19),
			),
			LevelData(
				gravity = listOf(1, 1, 1, 2, +1, 1, 1, 2, 3, 4, 4, 4, 5, 4, 3, -1), listOf(1, 1, 1, 1, 20, 1),
				listOf(26, 25, 24, 24, 28, 25, 24, 24, 24, 24, 23, 22, 22, 21, 21, 20),
				lineDelay = listOf(20), lockDelay = listOf(24, 24, 24, 30, 39, 24, 24, 25, 25, 25, 24, 23, 23, 23, 23, 25)
			)
		).map {(g, gd, are, _, lined, lock) ->
			LevelData(g, gd, are, lined, lock, lock.map {minOf(15, it-12)})
		}

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

		/** LV17 roll time */
		private const val ROLLTIMELIMIT = 12000
	}
}
