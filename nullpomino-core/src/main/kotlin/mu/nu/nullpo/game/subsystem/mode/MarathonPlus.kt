/*
 * Copyright (c) 2010-2024, NullNoname
 * Kotlin converted and modified by Venom=Nhelv.
 * THIS WAS NOT MADE IN ASSOCIATION WITH THE GAME CREATOR.
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
import mu.nu.nullpo.game.component.Block
import mu.nu.nullpo.game.component.Controller
import mu.nu.nullpo.game.component.RuleOptions
import mu.nu.nullpo.game.event.EventReceiver
import mu.nu.nullpo.game.event.EventReceiver.COLOR
import mu.nu.nullpo.game.event.ScoreEvent
import mu.nu.nullpo.game.net.NetPlayerClient
import mu.nu.nullpo.game.net.NetUtil
import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.game.subsystem.mode.menu.BooleanMenuItem
import mu.nu.nullpo.game.subsystem.mode.menu.DelegateMenuItem
import mu.nu.nullpo.game.subsystem.mode.menu.IntegerMenuItem
import mu.nu.nullpo.game.subsystem.mode.menu.MenuList
import mu.nu.nullpo.game.subsystem.mode.menu.StringsMenuItem
import mu.nu.nullpo.gui.common.BaseFont
import mu.nu.nullpo.gui.net.NetLobbyFrame
import mu.nu.nullpo.util.CustomProperties
import mu.nu.nullpo.util.GeneralUtil.toTimeStr
import java.io.IOException

/** MARATHON+ Mode */
class MarathonPlus:NetDummyMode() {
	private var lastlinetime = 0

	/** Current BGM */
	private var bgmLv = 0

	/** Bonus level line count */
	private var bonusLines = 0

	/** Bonus level piece count */
	private var bonusPieceCount = 0

	/** Bonus level remaining flash time */
	private var bonusFlashNow = 0

	/** Bonus level time */
	private var bonusTime = 0
	private var bonusTimeMax = 0

	private var lastlives = 0

	/** lines per level */
	private var norm = 0
	private var nextsec = 0

	private val itemMode = object:StringsMenuItem(
		"goalType", "GOAL", COLOR.BLUE, 0, tableGameClearLevel.map {"$it LEVEL"}
	) {
		override val showHeight = 3
		override fun draw(engine:GameEngine, playerID:Int, receiver:EventReceiver, y:Int, focus:Int) {
			super.draw(engine, playerID, receiver, y, focus)
			receiver.drawMenuNano(
				engine, 1, y+2,
				"${if(startLevel) if(goalType>0) 100 else "???" else tableGameClearLines[value]} LINES",
				if(focus==0) COLOR.RAINBOW else COLOR.WHITE
			)
		}
	}
	/** Game type */
	private var goalType:Int by DelegateMenuItem(itemMode)

	private val itemSpeed = IntegerMenuItem("turbo", "TURBO", COLOR.BLUE, 0, 0..<TURBO_MAX, true)
	/** Speed Difficulty */
	private var turbo:Int by DelegateMenuItem(itemSpeed)

	private val itemTT = BooleanMenuItem("turbo", "TRIAL", COLOR.BLUE, false, true)
	/** Level at start time */
	private var startLevel:Boolean by DelegateMenuItem(itemTT)

	private val itemBig = BooleanMenuItem("big", "BIG", COLOR.BLUE, false, true)
	/** BigMode */
	private var big:Boolean by DelegateMenuItem(itemBig)

	override val menu = MenuList("marathonplus", itemMode, itemTT, itemSpeed, itemBig)
	/** Version */
	private var version = 0

	/** Current round's ranking position */
	private var rankingRank = 0

	/** Rankings' scores */
	private val rankingScore = List(RANKING_TYPE) {MutableList(RANKING_MAX) {0L}}
	private val rankingLives = List(RANKING_TYPE) {MutableList(RANKING_MAX) {0}}
	private val rankingLines = List(RANKING_TYPE) {MutableList(RANKING_MAX) {0}}
	private val rankingTime = List(RANKING_TYPE) {MutableList(RANKING_MAX) {-1}}

	override val rankMap
		get() = rankMapOf(rankingScore.mapIndexed {a, x -> "$a.score" to x}+
			rankingLives.mapIndexed {a, x -> "$a.lives" to x}+
			rankingLines.mapIndexed {a, x -> "$a.lines" to x}+
			rankingTime.mapIndexed {a, x -> "$a.time" to x})

	private var ruleOptOrg = RuleOptions()
	/* Mode name */
	override val name = "Marathon+ ScoreAttack"
	override val gameIntensity = 1
	/* Initialization */
	override fun playerInit(engine:GameEngine) {
		super.playerInit(engine)
		if(ruleOptOrg.strRuleName.isEmpty()&&ruleOptOrg.strWallkick.isEmpty())
			ruleOptOrg = engine.ruleOpt
		lastlives = 0
		lastScore = 0
		bgmLv = 0
		nextsec = 0
		norm = 0
		bonusTimeMax = 0
		bonusTime = 0
		bonusFlashNow = 0
		bonusPieceCount = 0
		bonusLines = 0

		rankingRank = -1
		rankingScore.forEach {it.fill(0)}
		rankingLines.forEach {it.fill(0)}
		rankingLives.forEach {it.fill(0)}
		rankingTime.forEach {it.fill(-1)}

		netPlayerInit(engine)
		if(!owner.replayMode) version = CURRENT_VERSION else
		// NET: Load name
			netPlayerName = owner.replayProp.getProperty("${engine.playerID}.net.netPlayerName", "")


		engine.staffrollEnable = true
		engine.staffrollNoDeath = false
		engine.staffrollEnableStatistics = true
		owner.bgMan.bg = if(startLevel) 36 else -1
		engine.frameColor = GameEngine.FRAME_COLOR_WHITE
	}

	/** Set the gravity rate
	 * @param engine GameEngine
	 */
	private fun setSpeed(engine:GameEngine) {
		val l = maxOf(0, engine.statistics.level)
		if(l>=50) {
			val lv = if(l>=200&&turbo==0) 50 else l-50
			engine.speed.gravity = -1
			engine.speed.are = 15-lv/10
			engine.speed.areLine = engine.speed.are
			engine.speed.lockDelay = if(turbo==0) 30-lv/20 else 25-lv/25
			engine.speed.das = 9-lv/32
		} else {
			val lv = minOf(l, tableSpeed[turbo].size-1)
			val g = tableSpeed[turbo][lv]
			engine.speed.gravity = when {
				g>0 -> g
				g==0 -> -1
				else -> -g
			}
			engine.speed.denominator = if(g>=0) tableDenominator[minOf(turbo, tableDenominator.size-1)] else 1

			engine.speed.are = when(turbo) {
				0 -> 24-goalType-lv/10
				else -> 20-goalType-lv/maxOf(10, goalType*10)
			}

			engine.speed.areLine = engine.speed.are
			engine.speed.lockDelay = when(turbo) {
				0 -> 50-lv/2
				else -> 30-lv/10
			}
			engine.speed.das = if(turbo==0) 14-lv/10 else 12-lv/20
		}
		engine.speed.lineDelay = tableLineDelay[turbo][goalType]
	}

	/* Called at settings screen */
	override fun onSetting(engine:GameEngine):Boolean {
		// NET: Net Ranking
		if(netIsNetRankingDisplayMode)
			netOnUpdateNetPlayRanking(engine, netGetGoalType)
		else if(!owner.replayMode) {
			// Configuration changes
			val change = updateMenu(engine)

			if(change!=0) {
				engine.playSE("change")

				owner.bgMan.bg = if(startLevel) (if(goalType==0) -13 else -14) else -1
				receiver.setBGSpd(owner, .5f+goalType*.4f+turbo*.5f)
				// NET: Signal options change
				if(netIsNetPlay&&netNumSpectators>0) netSendOptions(engine)
			}

			// Confirm
			if(menuTime<5) menuTime++ else if(engine.ctrl.isPush(Controller.BUTTON_A)) {
				engine.playSE("decide")

				// NET: Signal start of the game
				if(netIsNetPlay) netLobby!!.netPlayerClient!!.send("start1p\n")

				return false
			}

			// Cancel
			if(engine.ctrl.isPush(Controller.BUTTON_B)&&!netIsNetPlay) engine.quitFlag = true

			// NET: Netplay Ranking
			if(engine.ctrl.isPush(Controller.BUTTON_D)&&netIsNetPlay&&!big&&engine.ai==null)
				netEnterNetPlayRankingScreen(netGetGoalType)
		} else {
			menuTime++
			menuCursor = -1

			return menuTime<60
		}// Replay

		return true
	}

	override fun onReady(engine:GameEngine):Boolean {
		super.onReady(engine)
		if(goalType==3) bonusTime = 10800
		return false
	}

	/* Called for initialization after "Ready" screen */
	override fun startGame(engine:GameEngine) {
		//if(!engine.readyDone){
		engine.run {
			if(startLevel) statistics.level = tableGameClearLevel[goalType]
			else nextsec = tableNorma[goalType][0]

			b2bEnable = true
			splitB2B = true
			comboType = GameEngine.COMBO_TYPE_NORMAL
			big = this@MarathonPlus.big

			twistAllowKick = true
			twistEnable = true
			useAllSpinBonus = true
			twistEnableEZ = true

			lives = if(goalType==0||startLevel) 2 else 4
			ruleOpt = ruleOptOrg
			if(goalType>0) {
				if(goalType==1) owSDSpd = 5
				ruleOpt.softdropLock = goalType==1
				ruleOpt.softdropSurfaceLock = goalType>1
				ruleOpt.harddropEnable = goalType>1
				owDelayCancel = if(goalType==3) 7 else -1
			}
			staffrollEnable = startLevel&&goalType>0
		}
		setSpeed(engine)
		owner.musMan.bgm = if(netIsWatch) BGM.Silent
		else if(startLevel) tableBGM[4][goalType] else tableBGM[goalType][0]
		owner.musMan.fadeSW = false
		if(goalType==3) bonusTime = 10800
		bonusTimeMax = ROLLTIMELIMIT[goalType]
	}

	/* Render score */
	override fun renderLast(engine:GameEngine) {
		if(owner.menuOnly) return

		receiver.drawScoreFont(engine, 0, 0, "Marathon +", COLOR.GREEN)
		if(goalType!=0) receiver.drawScoreFont(
			engine, 0, 1, if(startLevel) "(Time Attack Mode)" else "(Score Attack Mode)",
			COLOR.GREEN
		)

		if(engine.stat==GameEngine.Status.SETTING||engine.stat==GameEngine.Status.RESULT&&!owner.replayMode) {
			if(!owner.replayMode&&!big&&engine.ai==null) {
				val topY = if(receiver.nextDisplayType==2) 6 else 4
				receiver.drawScoreFont(engine, 2, topY-1, "SCORE   LINE TIME", COLOR.BLUE)
				val gameType = typeSerial(goalType, turbo, startLevel)
				for(i in 0..<RANKING_MAX) {
					receiver.drawScoreGrade(engine, 0, topY+i, "%2d".format(i+1), COLOR.YELLOW)
					receiver.drawScoreNum(engine, 2, topY+i, "${rankingScore[gameType][i]}", i==rankingRank)
					receiver.drawScoreNum(engine, 10, topY+i, "${rankingLines[gameType][i]}", i==rankingRank)
					receiver.drawScoreNum(engine, 15, topY+i, rankingTime[gameType][i].toTimeStr, i==rankingRank)
				}
			}
		} else {
			receiver.drawScoreFont(engine, 0, 3, "LINE", COLOR.BLUE)
			val level = engine.statistics.level
			var strLine = String.format(
				if(startLevel) "%03d/100"
				else if(level>=tableGameClearLevel[goalType]||level>=50) "%d"
				else "%3d/%3d", engine.statistics.lines, nextsec
			)
			if(level>=tableGameClearLevel[goalType]&&!startLevel) strLine += "/$bonusLines"
			receiver.drawScoreNum(engine, 5, 2, strLine, 2f)
			val scGet:Boolean = scDisp<engine.statistics.score
			receiver.drawScoreFont(engine, 0, 4, "Score", COLOR.BLUE)
			receiver.drawScoreNum(engine, 5, 4, "+$lastScore")
			receiver.drawScoreNum(engine, 0, 5, "$scDisp", scGet, 2f)

			receiver.drawScoreFont(engine, 0, 7, "Level", COLOR.BLUE)
			receiver.drawScoreNum(engine, 5, 7, "$level/"+tableGameClearLevel[goalType], 2f)
			receiver.drawScoreFont(engine, 0, 8, "Time", COLOR.BLUE)
			receiver.drawScoreNum(engine, 0, 9, engine.statistics.time.toTimeStr, 2f)
			if(engine.ending==2) {
				val remainRollTime = maxOf(0, bonusTimeMax-bonusTime)

				receiver.drawScoreFont(engine, 0, 11, "BONUS", COLOR.RED)
				receiver.drawScoreNum(
					engine, 0, 12, remainRollTime.toTimeStr, remainRollTime>0&&remainRollTime<10*60,
					2f
				)
			} else if(goalType==3&&!startLevel) {
				receiver.drawScoreFont(engine, 0, 11, "BONUS", COLOR.RED)
				receiver.drawScoreNum(engine, 6, 11, bonusTime.toTimeStr, bonusTime>0&&bonusTime<10*60)
				receiver.drawScoreNum(
					engine, 0, 12, (bonusTime*(1+engine.lives)*10).toString(), bonusTime>0&&bonusTime<10*60, 2f
				)
			}
		}
		super.renderLast(engine)
	}

	/* Called after every frame */
	override fun onLast(engine:GameEngine) {
		super.onLast(engine)
		if(engine.gameActive)
			if(engine.statistics.level>=tableGameClearLevel[goalType]) {
				if(bonusFlashNow>0) bonusFlashNow--

				if(goalType==0)
					bonusLevelProc(engine)
				else {
					if(startLevel) {
						engine.meterValue = (100-engine.statistics.lines)/100f
						engine.meterColor = GameEngine.METER_COLOR_LIMIT
					} else {
						bonusTime++
						// Time meter
						engine.meterValue = (bonusTimeMax-bonusTime)*1f/bonusTimeMax
						engine.meterColor = GameEngine.METER_COLOR_LIMIT
					}
					if(if(startLevel) engine.statistics.lines>=100 else bonusTime>=bonusTimeMax) {
						// Completed
						if(!netIsWatch) {
							if(!startLevel) {
								val lifeBonus = (engine.statistics.score*(1+engine.lives)/4).toInt()
								engine.statistics.scoreBonus += lifeBonus
							} else {
								engine.staffrollEnable = false
							}
							engine.statistics.rollClear = 2
							engine.gameEnded()
							engine.resetStatc()
							engine.stat = if(startLevel) GameEngine.Status.ENDINGSTART else GameEngine.Status.EXCELLENT
						}
					}
				}
			} else if(!startLevel&&goalType==3&&engine.timerActive) {
				if(bonusTime>0) {
					bonusTime--
					if(bonusTime<=600&&bonusTime%60==0) engine.playSE("countdown")
					engine.meterValue = bonusTime/2f/18000
					if(engine.statistics.level<50) {
						if(norm>0) engine.meterValue += ((1-engine.meterValue)*norm*
							bonusTime/lastlinetime/tableNorma[goalType][engine.statistics.level/10])
					} else {
						engine.meterValue += ((1-engine.meterValue)*(engine.statistics.level-50)*bonusTime)/lastlinetime/150
					}
					engine.meterColor = GameEngine.METER_COLOR_LIMIT
				} else if(!netIsWatch) {
					engine.lives = 0
					engine.gameEnded()
					engine.resetStatc()
					engine.stat = GameEngine.Status.GAMEOVER
				}
			}
	}

	/** Bonus level subroutine
	 * @param engine GameEngine
	 */
	private fun bonusLevelProc(engine:GameEngine) {
		if(bonusFlashNow>0) engine.blockOutlineType = GameEngine.BLOCK_OUTLINE_NORMAL
		else {
			engine.blockOutlineType = GameEngine.BLOCK_OUTLINE_NONE
			engine.field.let {field ->
				for(i in 0..<field.height)
					for(j in 0..<field.width)
						field.getBlock(j, i)?.apply {
							if(color!=null) {
								setAttribute(false, Block.ATTRIBUTE.VISIBLE)
								setAttribute(goalType>0, Block.ATTRIBUTE.OUTLINE)
							}
						}
			}
		}
	}

	/* Calculate score */
	override fun calcScore(engine:GameEngine, ev:ScoreEvent):Int {
		super.calcScore(engine, ev)
		var lv = engine.statistics.level

		// Bonus level
		if(lv>=tableGameClearLevel[goalType]) {
			bonusLines += ev.lines
			bonusPieceCount++
			if(bonusPieceCount>bonusLines/4) {
				bonusPieceCount = 0
				bonusFlashNow = 30
				engine.blockOutlineType = GameEngine.BLOCK_OUTLINE_NORMAL
				engine.resetFieldVisible()
			}
			if(goalType>0) {
				if(startLevel&&engine.statistics.lines>=100)
					engine.ending = 2
				else if(!startLevel) bonusTimeMax += engine.speed.are*ev.lines//20,64,99,
			}
		} else {
			if(bgmLv<tableBGMChange[goalType].size&&tableBGMChange[goalType][bgmLv]>=5) {
				if(engine.statistics.lines>=tableBGMChange[goalType][bgmLv]-5) owner.musMan.fadeSW = true
				if(engine.statistics.lines>=tableBGMChange[goalType][bgmLv]) {
					bgmLv++
					owner.musMan.bgm = tableBGM[goalType][bgmLv]
					owner.musMan.fadeSW = false
				}
			}
			var normMax = tableNorma[goalType][minOf(lv/10, tableNorma[goalType].size-1)]
			norm += ev.lines
			// Meter
			if(goalType<3) {
				engine.meterValue = norm/(normMax-1f)
				engine.meterColor = GameEngine.METER_COLOR_LEVEL
			}
			while(ev.lines>0&&engine.statistics.lines>=nextsec) {
				// Level up
				lv = ++engine.statistics.level
				setSpeed(engine)
				owner.bgMan.nextBg = -1-when {
					goalType==0 -> lv/2
					goalType==3&&lv>=50 -> 11
					else -> lv/5
				}//if(lv<20) lv/2 else if(lv<50) 10+(lv-20)/3 else 20+(lv-50)/15

				if(lv>=tableGameClearLevel[goalType]) {
					// Bonus level unlocked
					bonusTime = 0
					val bonusScore = (engine.statistics.score*(1+lastlives)/3f).toInt()
					owner.bgMan.nextBg = -12
					if(goalType==0) {
						lastlives = engine.lives
						engine.statistics.scoreBonus += bonusScore

						owner.musMan.bgm = BGM.Ending(1)
						engine.ending = 1
						engine.timerActive = false
					} else {
						lastlives = engine.lives
						engine.statistics.scoreBonus += bonusScore+bonusTime*(1+lastlives)/2
						engine.playSE("endingstart")
						engine.playSE("levelup_section")
						engine.ending = 2
					}

					owner.musMan.bgm = tableBGM[goalType][tableBGM[goalType].size-1]
					owner.musMan.fadeSW = false
					break
				}
				normMax = tableNorma[goalType][minOf(lv/10, tableNorma[goalType].size-1)]
				if(lv<50) {
					nextsec += normMax
					norm -= normMax
					bonusTime += (80+engine.speed.lockDelay+engine.speed.are)*normMax
					engine.playSE("levelup")
				} else {
					norm = engine.statistics.lines
					nextsec = norm+1
					bonusTime += (engine.speed.lockDelay+engine.speed.are+36+ev.lines*15)*ev.lines//65,230,390,580
				}
			}
		}
		return if(ev.lines>0) lastScore else 0
	}

	/* Soft drop */
	override fun afterSoftDropFall(engine:GameEngine, fall:Int) {
		engine.statistics.scoreSD += fall
		scDisp += fall
	}

	/* Hard drop */
	override fun afterHardDropFall(engine:GameEngine, fall:Int) {
		engine.statistics.scoreHD += fall*2
		scDisp += fall*2
	}

	/* Ending */
	override fun onEndingStart(engine:GameEngine):Boolean {
		if(!engine.gameActive) return super.onEndingStart(engine)

		engine.stat = GameEngine.Status.CUSTOM
		engine.resetStatc()
		return true
	}

	/* Bonus level unlocked screen */
	override fun onCustom(engine:GameEngine):Boolean {
		if(engine.statc[0]==0) {
			engine.nowPieceObject = null
			engine.timerActive = false
			engine.playSE("endingstart")
			// NET: Send bonus level entered messages
			if(netIsNetPlay&&!netIsWatch)
				if(netNumSpectators>0) {
					netSendField(engine)
					netSendNextAndHold(engine)
					netSendStats(engine)
					netLobby!!.netPlayerClient!!.send("game\tbonuslevelenter\n")
				}
		} else if(engine.statc[0]==90)
			engine.playSE("excellent")
		else if(engine.statc[0] in 120..479) {
			if(engine.ctrl.isPush(Controller.BUTTON_A)&&!netIsWatch) engine.statc[0] = 480
		} else if(engine.statc[0]>=480) {
			engine.ending = 0
			engine.stat = GameEngine.Status.READY
			engine.resetStatc()

			// NET: Send game restarted messages
			if(netIsNetPlay&&!netIsWatch)
				if(netNumSpectators>0) {
					netSendField(engine)
					netSendNextAndHold(engine)
					netSendStats(engine)
					netLobby!!.netPlayerClient!!.send("game\tbonuslevelstart\n")
				}

			return true
		}

		engine.statc[0]++
		return false
	}

	/* Render bonus level unlocked screen */
	override fun renderCustom(engine:GameEngine) {
		if(engine.statc[0]>=90) {
			receiver.drawMenuFont(engine, 0, 8, "EXCELLENT!", COLOR.ORANGE)

			receiver.drawMenuFont(engine, 0, 10, "HERE COMES", COLOR.ORANGE)
			receiver.drawMenuFont(
				engine, 1, 11, "BONUS", if(engine.statc[0]%2==0) COLOR.YELLOW else COLOR.WHITE
			)
			receiver.drawMenuFont(
				engine, 4, 12, "Level", if(engine.statc[0]%2==1) COLOR.YELLOW else COLOR.WHITE
			)
		}
	}

	/* game over */
	override fun onGameOver(engine:GameEngine):Boolean {
		if(engine.statc[0]==0&&engine.gameActive) engine.blockOutlineType = GameEngine.BLOCK_OUTLINE_NORMAL
		return super.onGameOver(engine)
	}

	/* Render results screen */
	override fun renderResult(engine:GameEngine) {
		receiver.drawMenuFont(engine, 0, 0, "${BaseFont.UP_S}${BaseFont.DOWN_S} PAGE${(engine.statc[1]+1)}/2", COLOR.RED)

		if(engine.statc[1]==0) {

			drawResultStats(engine, receiver, 1, COLOR.BLUE, Statistic.SCORE, Statistic.LINES, Statistic.LEVEL, Statistic.TIME)
			drawResult(engine, receiver, 10, COLOR.BLUE, "BONUS PTS.", engine.statistics.scoreBonus)

			drawResultRank(engine, receiver, 14, COLOR.BLUE, rankingRank)
			drawResultNetRank(engine, receiver, 16, COLOR.BLUE, netRankingRank[0])
			drawResultNetRankDaily(engine, receiver, 18, COLOR.BLUE, netRankingRank[1])
		} else {
			drawResultStats(
				engine, receiver, 2, COLOR.BLUE, Statistic.SPL, Statistic.SPM, Statistic.LPM, Statistic.PPS
			)
			if(engine.statistics.level>=tableGameClearLevel[goalType]&&(startLevel||goalType==0)) {
				drawResult(engine, receiver, 11, COLOR.BLUE, "BONUS LINE", bonusLines)
				drawResult(engine, receiver, 13, COLOR.BLUE, "BONUS TIME", "%10s".format(bonusTime.toTimeStr))
			}
		}
		if(netIsPB) receiver.drawMenuFont(engine, 2, 21, "NEW PB", COLOR.ORANGE)

		if(netIsNetPlay&&netReplaySendStatus==1)
			receiver.drawMenuFont(engine, 0, 22, "SENDING...", COLOR.PINK)
		else if(netIsNetPlay&&!netIsWatch
			&&netReplaySendStatus==2)
			receiver.drawMenuFont(engine, 1, 22, "A: RETRY", COLOR.RED)
	}

	/* Results screen */
	override fun onResult(engine:GameEngine):Boolean {
		// Page change
		if(engine.ctrl.isMenuRepeatKey(Controller.BUTTON_UP)) {
			engine.statc[1]--
			if(engine.statc[1]<0) engine.statc[1] = 1
			engine.playSE("change")
		}
		if(engine.ctrl.isMenuRepeatKey(Controller.BUTTON_DOWN)) {
			engine.statc[1]++
			if(engine.statc[1]>1) engine.statc[1] = 0
			engine.playSE("change")
		}

		return super.onResult(engine)
	}

	/* Called when saving replay */
	override fun saveReplay(engine:GameEngine, prop:CustomProperties):Boolean {
		saveSetting(engine, prop)

		// NET: Save name
		if(netPlayerName!=null&&netPlayerName!!.isNotEmpty()) prop.setProperty(
			"${engine.playerID}.net.netPlayerName",
			netPlayerName
		)

		// Update rankings
		if(!owner.replayMode&&!big&&engine.ai==null) {
			updateRanking(
				engine.statistics.score, engine.statistics.lines, engine.lives, engine.statistics.time,
				typeSerial(goalType, turbo, startLevel)
			)

			if(rankingRank!=-1) return true
		}
		return false
	}

	/** Update rankings
	 * @param sc Score
	 * @param li Lines
	 * @param time Time
	 */
	private fun updateRanking(sc:Long, li:Int, lf:Int, time:Int, type:Int) {
		rankingRank = checkRanking(sc, li, lf, time, type)

		if(rankingRank!=-1) {
			// Shift down ranking entries
			for(i in RANKING_MAX-1 downTo rankingRank+1) {
				rankingScore[type][i] = rankingScore[type][i-1]
				rankingLines[type][i] = rankingLines[type][i-1]
				rankingLives[type][i] = rankingLives[type][i-1]
				rankingTime[type][i] = rankingTime[type][i-1]
			}

			// Add new data
			rankingScore[type][rankingRank] = sc
			rankingLines[type][rankingRank] = li
			rankingLives[type][rankingRank] = lf
			rankingTime[type][rankingRank] = time
		}
	}

	/** Calculate ranking position
	 * @param sc Score
	 * @param li Lines
	 * @param time Time
	 * @return Position (-1 if unranked)
	 */
	private fun checkRanking(sc:Long, li:Int, lf:Int, time:Int, type:Int):Int {
		for(i in 0..<RANKING_MAX)
			if(goalType>0&&startLevel) {
				if(time<rankingTime[type][i]) return i
				else if(time==rankingTime[type][i]&&sc>rankingScore[type][i]) return i
			} else if(sc>rankingScore[type][i]) return i
			else if(sc==rankingScore[type][i]&&li>rankingLines[type][i]) return i
			else if(sc==rankingScore[type][i]&&li==rankingLines[type][i]&&lf>rankingLives[type][i]) return i

		return -1
	}

	/* NET: Message received */
	@Throws(IOException::class)
	override fun onMessage(lobby:NetLobbyFrame, client:NetPlayerClient, message:List<String>) {
		super.onMessage(lobby, client, message)

		// Game messages
		if(message[0]=="game") {
			val engine = owner.engine[0]

			// Bonus level entered
			if(message[3]=="bonuslevelenter") {
				engine.meterValue = 0f
				owner.musMan.bgm = BGM.GrandM(1)
				engine.timerActive = false
				engine.ending = 1
				engine.stat = GameEngine.Status.CUSTOM
				engine.resetStatc()
			} else if(message[3]=="bonuslevelstart") {
				engine.ending = 0
				engine.stat = GameEngine.Status.READY
				engine.resetStatc()
			}// Bonus level started
		}
	}

	/* NET: Receive field message */
	override fun netRecvField(engine:GameEngine, message:List<String>) {
		super.netRecvField(engine, message)

		if(engine.statistics.level>=20&&engine.timerActive&&engine.gameActive) bonusLevelProc(engine)
	}

	/** NET: Send various in-game stats
	 * @param engine GameEngine
	 */
	override fun netSendStats(engine:GameEngine) {
		val bg =
			if(owner.bgMan.fadeSW) owner.bgMan.nextBg else engine.owner.bgMan.bg
		val msg = "game\tstats\t"+engine.run {
			statistics.run {"${scoreLine}\t${scoreSD}\t${scoreHD}\t${scoreBonus}\t${lines}\t${totalPieceLocked}\t${time}\t${level}\t"}+
				"${gameActive}\t${timerActive}\t$lastScore\t$scDisp\t${lastEvent}\tt${lastEventPiece}\t"
		}+"$bg\t$bonusLines\t$bonusFlashNow\t$bonusPieceCount\t$bonusTime\n"
		netLobby?.netPlayerClient?.send(msg)
	}

	/** NET: Parse Received [message] as in-game stats of [engine] */
	override fun netRecvStats(engine:GameEngine, message:List<String>) {
		listOf<(String)->Unit>({}, {}, {}, {},
			{engine.statistics.scoreLine = it.toInt()},
			{engine.statistics.scoreSD = it.toInt()},
			{engine.statistics.scoreHD = it.toInt()},
			{engine.statistics.scoreBonus = it.toInt()},
			{engine.statistics.lines = it.toInt()},
			{engine.statistics.totalPieceLocked = it.toInt()},
			{engine.statistics.time = it.toInt()},
			{engine.statistics.level = it.toInt()},
			{engine.gameActive = it.toBoolean()},
			{engine.timerActive = it.toBoolean()},
			{lastScore = it.toInt()},
			{/*scDisp = it.toInt()*/},
			{engine.lastEvent = ScoreEvent.parseStr(it)},
			{engine.owner.bgMan.bg = it.toInt()},
			{bonusLines = it.toInt()},
			{bonusFlashNow = it.toInt()},
			{bonusPieceCount = it.toInt()},
			{bonusTime = it.toInt()}).zip(message).forEach {(x, y) ->
			x(y)
		}

		// Meter
		if(engine.statistics.level<20) {
			engine.meterValue = engine.statistics.lines%10/9f
			engine.meterColor = GameEngine.METER_COLOR_LEVEL
		} else engine.meterValue = 0f
	}

	/** NET: Send end-of-game stats
	 * @param engine GameEngine
	 */
	override fun netSendEndGameStats(engine:GameEngine) {
		val subMsg = engine.statistics.run {
			"SCORE;${score}\tLINE;${lines}\tBONUS LINE;$bonusLines\tLEVEL;${(level+levelDispAdd)}\t"+
				"TOTAL TIME;${time.toTimeStr}\tLV20- TIME;${(time-bonusTime).toTimeStr}\tBONUS TIME;${bonusTime.toTimeStr}\t"+
				"SCORE/LINE;${spl}\tSCORE/MIN;${spm}\tLINE/MIN;${lpm}\tPIECE/SEC;${pps}\t"
		}

		val msg = "gstat1p\t${NetUtil.urlEncode(subMsg)}\n"
		netLobby?.netPlayerClient?.send(msg)
	}

	/** NET: Send game options to all spectators
	 * @param engine GameEngine
	 */
	override fun netSendOptions(engine:GameEngine) {
		val msg = "game\toption\t$startLevel\t$big\n"
		netLobby?.netPlayerClient?.send(msg)
	}

	/** NET: Receive game options */
	override fun netRecvOptions(engine:GameEngine, message:List<String>) {
		startLevel = message[4].toBoolean()
		big = message[5].toBoolean()
	}

	/** NET: Get goal type */
	override val netGetGoalType get() = goalType+if(startLevel) GAMETYPE_MAX else 0

	/** NET: It returns true when the current settings don't prevent
	 * leaderboard screen from showing. */
	override fun netIsNetRankingViewOK(engine:GameEngine):Boolean = !big&&engine.ai==null

	companion object {
		/** Current version */
		private const val CURRENT_VERSION = 1

		/** Fall velocity table (numerators) */
		private val tableSpeed = listOf(
			listOf(
				5, 8, 10, 12, 16, 20, 24, 30, 36, 42,
				48, 54, 60, 66, 72, 80, 88, 96, 108, 120,
				48, 60, 75, 90, 105, 120, 140, 160, 180, 200,
				240, 280, 320, 360, 420, 480, 600, 720, 840, 960,
				240, 320, 480, 640, 800, 960, 1200, 1440, 1920, 2400, -1
			),
			listOf(
				1, 2, 3, 4, 5, 6, 7, 8, 9, 10,
				11, 12, 13, 14, 15, 16, 17, 18, 19, 20,
				8, 10, 12, 14, 16, 18, 20, 24, 28, 30,
				32, 40, 48, 64, 80, 96, 112, 128, 114, 160,
				64, 80, 96, 128, 160, 192, 224, 256, 288, 320, -1
			)
		)
		private val tableDenominator = listOf(240, 32)
		private val tableLineDelay = listOf(listOf(12, 24, 16, 0), listOf(6, 18, 8, 0))
		private val ARE_TABLE = listOf(
			15, 15, 15, 15, 14, 14,
			13, 12, 11, 10, 9, 8, 7, 6, 5, 15
		)
		private val LOCK_TABLE = listOf(
			30, 29, 28, 27, 26, 25,
			24, 23, 22, 21, 20, 19, 18, 17, 17, 30
		)

		// Levels for speed changes
		private val LEVEL_ARE_LOCK_CHANGE = listOf(
			60, 70, 80, 90, 100,
			110, 120, 130, 140, 150,
			160, 170, 180, 190, 200, 10000
		)

		/*
* 8 18 30
* 6,13,21,30,40
* 7,14,21,28,35...50
* */
		/** Line counts when Level changes occur */
		private val tableNorma =
			listOf(listOf(10), listOf(8, 10, 12), listOf(6, 7, 8, 9, 10), listOf(7, 7, 7, 7, 7, 1))

		/** Line counts when BGM changes occur */
		private val tableBGMChange =
			listOf(listOf(100, 150), listOf(80, 180), listOf(130, 300), listOf(140, 280, 350))
		private val tableBGM =
			listOf(
				listOf(BGM.Puzzle(0), BGM.Generic(0), BGM.Extra(2), BGM.GrandM(0)),
				listOf(BGM.Generic(0), BGM.Generic(1), BGM.Generic(2), BGM.Puzzle(2)), //30levels
				listOf(BGM.Puzzle(2), BGM.Generic(3), BGM.Generic(4), BGM.Generic(5)), //50levels
				listOf(BGM.Puzzle(3), BGM.Generic(6), BGM.Generic(7), BGM.Generic(8), BGM.Generic(9)), //200levels
				listOf(BGM.Puzzle(4), BGM.Rush(1), BGM.Rush(2), BGM.Rush(3))
			)//challenge mode

		/** Ending time */
		private val ROLLTIMELIMIT = listOf(-1, 5000, 7500, 10000)

		/** Line counts when game ending occurs */
		private val tableGameClearLevel = listOf(20, 30, 50, 200)
		private val tableGameClearLines = tableGameClearLevel.mapIndexed {i, l ->
			tableNorma[i].sumOf {it*10}+tableNorma[i].last {true}*(l-tableNorma[i].size*10)
		}
		/** Number of game types */
		private val GAMETYPE_MAX = tableGameClearLevel.size

		private val TURBO_MAX = minOf(tableSpeed.size, tableDenominator.size)

		/** Number of entries in rankings */
		private const val RANKING_MAX = 13

		/** Number of ranking types */
		private val RANKING_TYPE = GAMETYPE_MAX*TURBO_MAX*2
		private fun typeSerial(type:Int, turbo:Int, startLevel:Boolean):Int = type*TURBO_MAX*2+turbo*2+if(startLevel) 1 else 0
	}
}
