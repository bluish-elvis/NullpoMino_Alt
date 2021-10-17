/*
 * Copyright (c) 2010-2021, NullNoname
 * Kotlin converted and modified by Venom=Nhelv
 * All rights reserved.
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
import mu.nu.nullpo.game.component.Block
import mu.nu.nullpo.game.component.Controller
import mu.nu.nullpo.game.component.RuleOptions
import mu.nu.nullpo.game.event.EventReceiver
import mu.nu.nullpo.game.event.ScoreEvent
import mu.nu.nullpo.game.net.NetPlayerClient
import mu.nu.nullpo.game.net.NetUtil
import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.game.subsystem.mode.menu.*
import mu.nu.nullpo.gui.net.NetLobbyFrame
import mu.nu.nullpo.util.CustomProperties
import mu.nu.nullpo.util.GeneralUtil.toTimeStr
import java.io.IOException
import kotlin.math.ceil

/** MARATHON+ Mode */
class MarathonPlus:NetDummyMode() {

	private var sc = 0
	private var sum = 0
	private var lastlinetime = 0

	/** Current BGM */
	private var bgmlv = 0

	/** Bonus level line count */
	private var bonusLines = 0

	/** Bonus level piece count */
	private var bonusPieceCount = 0

	/** Bonus level remaining flash time */
	private var bonusFlashNow = 0

	/** Bonus level time */
	private var bonusTime = 0
	private var bonusTimeMax = 0

	/** Bonus score from life */
	private var bonusScore = 0
	private var lastlives = 0

	/** lines per level */
	private var norm = 0
	private var nextsec = 0

	private val itemMode = object:StringsMenuItem("goaltype", "GOAL", EventReceiver.COLOR.BLUE, 0,
		tableGameClearLevel.map {"$it LEVEL"}.toTypedArray()) {
		override val showHeight = 2
		override fun draw(engine:GameEngine, playerID:Int, receiver:EventReceiver, y:Int, focus:Int) {
			super.draw(engine, playerID, receiver, y, focus)
			receiver.drawMenuNano(engine, playerID, 1, y+1, "${tableGameClearLines[value]} LINES")
		}
	}
	/** Game type */
	private var goaltype:Int by DelegateMenuItem(itemMode)

	private val itemSpeed = IntegerMenuItem("turbo", "TURBO", EventReceiver.COLOR.BLUE, 0, 0 until TURBO_MAX, true)
	/** Speed Difficulty */
	private var turbo:Int by DelegateMenuItem(itemSpeed)

	private val itemTT = BooleanMenuItem("turbo", "TRIAL", EventReceiver.COLOR.BLUE, false, true)
	/** Level at start time */
	private var startLevel:Boolean by DelegateMenuItem(itemTT)

	private val itemBig = BooleanMenuItem("big", "BIG", EventReceiver.COLOR.BLUE, false, true)
	/** BigMode */
	private var big:Boolean by DelegateMenuItem(itemBig)

	override val menu = MenuList("marathonplus", itemMode, itemTT, itemSpeed, itemBig)
	/** Version */
	private var version = 0

	/** Current round's ranking rank */
	private var rankingRank = 0

	/** Rankings' scores */
	private var rankingScore = Array(RANKING_TYPE) {IntArray(RANKING_MAX)}
	private var rankingLives = Array(RANKING_TYPE) {IntArray(RANKING_MAX)}
	private var rankingLines = Array(RANKING_TYPE) {IntArray(RANKING_MAX)}
	private var rankingTime = Array(RANKING_TYPE) {IntArray(RANKING_MAX)}

	override val rankMap:Map<String, IntArray>
		get() = mapOf(*(
			(rankingScore.mapIndexed {a, x -> "$a.score" to x}+
				rankingLives.mapIndexed {a, x -> "$a.ives" to x}+
				rankingLines.mapIndexed {a, x -> "$a.lines" to x}+
				rankingTime.mapIndexed {a, x -> "$a.time" to x}).toTypedArray()))
	private var ruleOptOrg = RuleOptions()
	/* Mode name */
	override val name = "Marathon+ ScoreAttack"
	override val gameIntensity = 1
	/* Initialization */
	override fun playerInit(engine:GameEngine, playerID:Int) {
		super.playerInit(engine, playerID)
		if(ruleOptOrg.strRuleName.isEmpty()&&ruleOptOrg.strWallkick.isEmpty())
			ruleOptOrg = engine.ruleOpt
		lastlives = 0
		bonusScore = lastlives
		lastscore = bonusScore
		bgmlv = 0
		nextsec = 0
		norm = nextsec
		bonusTimeMax = 0
		bonusTime = bonusTimeMax
		bonusFlashNow = bonusTime
		bonusPieceCount = bonusFlashNow
		bonusLines = bonusPieceCount

		rankingRank = -1
		rankingScore = Array(RANKING_TYPE) {IntArray(RANKING_MAX)}
		rankingLines = Array(RANKING_TYPE) {IntArray(RANKING_MAX)}
		rankingLives = Array(RANKING_TYPE) {IntArray(RANKING_MAX)}
		rankingTime = Array(RANKING_TYPE) {IntArray(RANKING_MAX)}

		netPlayerInit(engine, playerID)
		if(!owner.replayMode) version = CURRENT_VERSION else
		// NET: Load name
			netPlayerName = engine.owner.replayProp.getProperty("$playerID.net.netPlayerName", "")

		engine.staffrollNoDeath = false
		engine.staffrollEnableStatistics = true
		engine.owner.backgroundStatus.bg = if(startLevel) 36 else 0
		engine.framecolor = GameEngine.FRAME_COLOR_WHITE
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
				0 -> 24-goaltype-lv/10
				else -> 20-goaltype-lv/maxOf(10, goaltype*10)
			}

			engine.speed.areLine = engine.speed.are
			engine.speed.lockDelay = when(turbo) {
				0 -> 50-lv/2
				else -> 30-lv/10
			}
			engine.speed.das = if(turbo==0) 14-lv/10 else 12-lv/20

		}
		engine.speed.lineDelay = tableLineDelay[turbo][goaltype]
	}

	/* Called at settings screen */
	override fun onSetting(engine:GameEngine, playerID:Int):Boolean {
		// NET: Net Ranking
		if(netIsNetRankingDisplayMode)
			netOnUpdateNetPlayRanking(engine, netGetGoalType())
		else if(!engine.owner.replayMode) {
			// Configuration changes
			val change = updateMenu(engine)

			if(change!=0) {
				engine.playSE("change")

				engine.owner.backgroundStatus.bg = 0

				// NET: Signal options change
				if(netIsNetPlay&&netNumSpectators>0) netSendOptions(engine)
			}

			// Confirm
			if(engine.ctrl.isPush(Controller.BUTTON_A)&&menuTime>=5) {
				engine.playSE("decide")

				// NET: Signal start of the game
				if(netIsNetPlay) netLobby!!.netPlayerClient!!.send("start1p\n")

				return false
			}

			// Cancel
			if(engine.ctrl.isPush(Controller.BUTTON_B)&&!netIsNetPlay) engine.quitflag = true

			// NET: Netplay Ranking
			if(engine.ctrl.isPush(Controller.BUTTON_D)&&netIsNetPlay&&!big
				&&engine.ai==null)
				netEnterNetPlayRankingScreen(engine, playerID, netGetGoalType())

			menuTime++
		} else {
			menuTime++
			menuCursor = -1

			return menuTime<60
		}// Replay

		return true
	}

	override fun onReady(engine:GameEngine, playerID:Int):Boolean {
		super.onReady(engine, playerID)
		if(goaltype==3) bonusTime = 10800
		return false
	}

	/* Called for initialization after "Ready" screen */
	override fun startGame(engine:GameEngine, playerID:Int) {
		//if(!engine.readyDone){
		engine.run {
			if(startLevel) statistics.level = tableGameClearLevel[goaltype]
			else nextsec = tableNorma[goaltype][0]

			b2bEnable = true
			splitb2b = true
			comboType = GameEngine.COMBO_TYPE_NORMAL
			big = this@MarathonPlus.big

			twistAllowKick = true
			twistEnable = true
			useAllSpinBonus = true
			twistEnableEZ = true

			lives = if(goaltype==0||startLevel) 2 else 4
			ruleOpt = ruleOptOrg
			if(goaltype>0) {
				if(goaltype==1)
					owSDSpd = 5
				ruleOpt.softdropEnable = goaltype==1
				ruleOpt.softdropLock = goaltype==1
				ruleOpt.softdropSurfaceLock = goaltype>1
				ruleOpt.harddropEnable = goaltype>1
				if(goaltype==3) owDelayCancel = 7
			}
			staffrollEnable = startLevel&&goaltype>0
		}
		setSpeed(engine)
		owner.bgmStatus.bgm = if(netIsWatch) BGM.Silent
		else if(startLevel) tableBGM[4][goaltype] else tableBGM[goaltype][0]
		owner.bgmStatus.fadesw = false
		if(goaltype==3) bonusTime = 10800
		bonusTimeMax = ROLLTIMELIMIT[goaltype]
	}

	/* Render score */
	override fun renderLast(engine:GameEngine, playerID:Int) {
		if(owner.menuOnly) return

		receiver.drawScoreFont(engine, playerID, 0, 0, "MARATHON +", EventReceiver.COLOR.GREEN)
		if(goaltype!=0) receiver.drawScoreFont(engine, playerID, 0, 1,
			if(startLevel) "(TIME ATTACK MODE)" else "(SCORE ATTACK MODE)", EventReceiver.COLOR.GREEN)

		if(engine.stat==GameEngine.Status.SETTING||engine.stat==GameEngine.Status.RESULT&&!owner.replayMode) {
			if(!owner.replayMode&&!big&&engine.ai==null) {
				val scale = if(receiver.nextDisplayType==2) .5f else 1f
				val topY = if(receiver.nextDisplayType==2) 6 else 4
				receiver.drawScoreFont(engine, playerID, 3, topY-1, "SCORE   LINE TIME", EventReceiver.COLOR.BLUE, scale)
				val gametype = typeSerial(goaltype, turbo, startLevel)
				for(i in 0 until RANKING_MAX) {
					receiver.drawScoreGrade(engine, playerID, 0, topY+i, String.format("%2d", i+1), EventReceiver.COLOR.YELLOW, scale)
					receiver.drawScoreNum(engine, playerID, 3, topY+i, "${rankingScore[gametype][i]}", i==rankingRank, scale)
					receiver.drawScoreNum(engine, playerID, 11, topY+i, "${rankingLines[gametype][i]}", i==rankingRank, scale)
					receiver.drawScoreNum(engine, playerID, 16, topY+i,
						rankingTime[gametype][i].toTimeStr, i==rankingRank, scale)
				}
			}
		} else {
			receiver.drawScoreFont(engine, playerID, 0, 3, "LINE", EventReceiver.COLOR.BLUE)
			val level = engine.statistics.level
			var strline = String.format(if(startLevel) "%03d/100"
			else if(level>=tableGameClearLevel[goaltype]||level>=50) "%d"
			else "%3d/%3d", engine.statistics.lines, nextsec)
			if(level>=tableGameClearLevel[goaltype]&&!startLevel) strline += "/$bonusLines"
			receiver.drawScoreNum(engine, playerID, 5, 2, strline, 2f)
			val scget:Boolean = scDisp<engine.statistics.score
			receiver.drawScoreFont(engine, playerID, 0, 4, "Score", EventReceiver.COLOR.BLUE)
			receiver.drawScoreNum(engine, playerID, 5, 4, "+$lastscore")
			receiver.drawScoreNum(engine, playerID, 0, 5, "$scDisp", scget, 2f)

			receiver.drawScoreFont(engine, playerID, 0, 7, "Level", EventReceiver.COLOR.BLUE)
			receiver.drawScoreNum(engine, playerID, 5, 7, "$level/"+tableGameClearLevel[goaltype], 2f)
			receiver.drawScoreFont(engine, playerID, 0, 8, "Time", EventReceiver.COLOR.BLUE)
			receiver.drawScoreNum(engine, playerID, 0, 9, engine.statistics.time.toTimeStr, 2f)
			if(engine.ending==2) {
				val remainRollTime = maxOf(0, bonusTimeMax-bonusTime)

				receiver.drawScoreFont(engine, playerID, 0, 11, "BONUS", EventReceiver.COLOR.RED)
				receiver.drawScoreNum(engine, playerID, 0, 12, remainRollTime.toTimeStr,
					remainRollTime>0&&remainRollTime<10*60, 2f)
			} else if(goaltype==3&&!startLevel) {
				receiver.drawScoreFont(engine, playerID, 0, 11, "BONUS", EventReceiver.COLOR.RED)
				receiver.drawScoreNum(engine, playerID, 6, 11, bonusTime.toTimeStr, bonusTime>0&&bonusTime<10*60)
				receiver.drawScoreNum(engine, playerID, 0, 12, (bonusTime*(1+engine.lives)*10).toString(), bonusTime>0&&bonusTime<10*60,
					2f)
			}
		}
		super.renderLast(engine, playerID)
	}

	/* Called after every frame */
	override fun onLast(engine:GameEngine, playerID:Int) {
		super.onLast(engine, playerID)
		if(engine.gameActive)
			if(engine.statistics.level>=tableGameClearLevel[goaltype]) {
				if(bonusFlashNow>0) bonusFlashNow--

				if(goaltype==0)
					bonusLevelProc(engine)
				else {
					if(startLevel) {
						engine.meterValue = (100-engine.statistics.lines)*receiver.getMeterMax(engine)/100
						engine.meterColor = GameEngine.METER_COLOR_LIMIT
					} else {
						bonusTime++
						// Time meter
						engine.meterValue = (bonusTimeMax-bonusTime)*receiver.getMeterMax(engine)/bonusTimeMax
						engine.meterColor = GameEngine.METER_COLOR_LIMIT
					}
					if(if(startLevel) engine.statistics.lines>=100 else bonusTime>=bonusTimeMax) {
						// Completed
						if(!netIsWatch) {
							if(!startLevel) {
								bonusScore = engine.statistics.score*(1+engine.lives)/4
								engine.statistics.scoreBonus += bonusScore
							} else {
								engine.staffrollEnable = false
							}
							engine.statistics.rollclear = 2
							engine.gameEnded()
							engine.resetStatc()
							engine.stat = if(startLevel) GameEngine.Status.ENDINGSTART else GameEngine.Status.EXCELLENT
						}
					}
				}
			} else if(!startLevel&&goaltype==3&&engine.timerActive) {
				if(bonusTime>0) {
					bonusTime--
					if(bonusTime<=600&&bonusTime%60==0) engine.playSE("countdown")
					engine.meterValue = receiver.getMeterMax(engine)*bonusTime/2/18000
					if(engine.statistics.level<50) {
						if(norm>0)
							engine.meterValue += ((receiver.getMeterMax(engine)-engine.meterValue)*norm*bonusTime
								/lastlinetime/tableNorma[goaltype][engine.statistics.level/10])
					} else {
						engine.meterValue += ((receiver.getMeterMax(engine)-engine.meterValue)*(engine.statistics.level-50)
							*bonusTime)/lastlinetime/150
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
				for(i in 0 until field.height)
					for(j in 0 until field.width)
						field.getBlock(j, i)?.apply {
							if(color!=null) {
								setAttribute(false, Block.ATTRIBUTE.VISIBLE)
								setAttribute(goaltype>0, Block.ATTRIBUTE.OUTLINE)
							}
						}

			}
		}
	}

	/* Calculate score */
	override fun calcScore(engine:GameEngine, playerID:Int, lines:Int):Int {
		// Line clear bonus
		val pts = calcScoreBase(engine, lines)
		var lv = engine.statistics.level
		// Combo
		val cmb = if(engine.combo>=1&&lines>=1) engine.combo-1 else 0

		val spd = maxOf(0, engine.lockDelay-engine.lockDelayNow)+if(engine.manualLock) 1 else 0
		// Add to score
		if(pts+cmb+spd>0) {
			if(lines>0) lastlinetime = bonusTime
			val mul = if(lv>40)55+lv/10 else 10+lv
			var get = pts*mul/10+spd
			if(cmb>=1) {
				var b = sum*(1+cmb)/2
				sum += get
				b = sum*(2+cmb)/2-b
				get = b
			} else
				sum = get
			if(pts>0) lastscore = get
			if(lines>=1) engine.statistics.scoreLine += get
			else engine.statistics.scoreBonus += get
			scDisp += spd
		}

		// Bonus level
		if(lv>=tableGameClearLevel[goaltype]) {
			bonusLines += lines
			bonusPieceCount++
			if(bonusPieceCount>bonusLines/4) {
				bonusPieceCount = 0
				bonusFlashNow = 30
				engine.blockOutlineType = GameEngine.BLOCK_OUTLINE_NORMAL
				engine.resetFieldVisible()
			}
			if(goaltype>0) {
				if(startLevel&&engine.statistics.lines>=100)
					engine.ending = 2
				else if(!startLevel) bonusTimeMax += engine.speed.are*lines//20,64,99,
			}
		} else {
			var bgmChanged = false
			if(bgmlv<tableBGMChange[goaltype].size&&tableBGMChange[goaltype][bgmlv]>=5) {
				if(engine.statistics.lines>=tableBGMChange[goaltype][bgmlv]-5) owner.bgmStatus.fadesw = true
				if(engine.statistics.lines>=tableBGMChange[goaltype][bgmlv]) {
					bgmChanged = true
					bgmlv++
					owner.bgmStatus.bgm = tableBGM[goaltype][bgmlv]
					owner.bgmStatus.fadesw = false
				}
			}
			var normMax = tableNorma[goaltype][minOf(lv/10, tableNorma[goaltype].size-1)]
			norm += lines
			// Meter
			if(goaltype<3) {
				engine.meterValue = norm*receiver.getMeterMax(engine)/(normMax-1)
				engine.meterColor = GameEngine.METER_COLOR_LEVEL
			}
			while(lines>0&&engine.statistics.lines>=nextsec) {
				// Level up
				lv = ++engine.statistics.level
				setSpeed(engine)
				owner.backgroundStatus.fadecount = 0
				owner.backgroundStatus.fadebg = if(lv<20) lv/2 else if(lv<50) 10+(lv-20)/3 else 20+(lv-50)/15
				owner.backgroundStatus.fadesw = owner.backgroundStatus.fadebg!=owner.backgroundStatus.bg
				if(lv>=tableGameClearLevel[goaltype]) {
					// Bonus level unlocked
					bonusTime = 0
					if(goaltype==0) {
						lastlives = engine.lives
						bonusScore = engine.statistics.score*(1+lastlives)/3
						engine.statistics.scoreBonus += bonusScore

						owner.bgmStatus.bgm = BGM.Ending(1)
						engine.ending = 1
						engine.timerActive = false
					} else {
						lastlives = engine.lives
						bonusScore = engine.statistics.score*(1+lastlives)*10
						bonusScore = bonusTime*(1+lastlives)
						engine.statistics.scoreBonus += bonusScore
						engine.playSE("endingstart")
						engine.playSE("levelup_section")
						engine.ending = 2
					}

					owner.bgmStatus.bgm = tableBGM[goaltype][tableBGM[goaltype].size-1]
					owner.bgmStatus.fadesw = false
					break
				}
				normMax = tableNorma[goaltype][minOf(lv/10, tableNorma[goaltype].size-1)]
				if(lv<50) {
					nextsec += normMax
					norm -= normMax
					bonusTime += (80+engine.speed.lockDelay+engine.speed.are)*normMax
					engine.playSE(if(bgmChanged) "levelup_section" else "levelup")
				} else {
					norm = engine.statistics.lines
					nextsec = norm+1
					bonusTime += (engine.speed.lockDelay+engine.speed.are+36+lines*15)*lines//65,230,390,580
				}

			}
		}
		return if(pts>0) lastscore else 0
	}

	/* Soft drop */
	override fun afterSoftDropFall(engine:GameEngine, playerID:Int, fall:Int) {
		engine.statistics.scoreSD += fall
		scDisp += fall
	}

	/* Hard drop */
	override fun afterHardDropFall(engine:GameEngine, playerID:Int, fall:Int) {
		engine.statistics.scoreHD += fall*2
		scDisp += fall*2
	}

	/* Ending */
	override fun onEndingStart(engine:GameEngine, playerID:Int):Boolean {
		if(!engine.gameActive) return super.onEndingStart(engine, playerID)

		engine.stat = GameEngine.Status.CUSTOM
		engine.resetStatc()
		return true
	}

	/* Bonus level unlocked screen */
	override fun onCustom(engine:GameEngine, playerID:Int):Boolean {
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
	override fun renderCustom(engine:GameEngine, playerID:Int) {
		if(engine.statc[0]>=90) {
			receiver.drawMenuFont(engine, playerID, 0, 8, "EXCELLENT!", EventReceiver.COLOR.ORANGE)

			receiver.drawMenuFont(engine, playerID, 0, 10, "HERE COMES", EventReceiver.COLOR.ORANGE)
			receiver.drawMenuFont(engine, playerID, 1, 11, "BONUS", if(engine.statc[0]%2==0)
				EventReceiver.COLOR.YELLOW
			else
				EventReceiver.COLOR.WHITE)
			receiver.drawMenuFont(engine, playerID, 4, 12, "Level", if(engine.statc[0]%2==0)
				EventReceiver.COLOR.YELLOW
			else
				EventReceiver.COLOR.WHITE)
		}
	}

	/* game over */
	override fun onGameOver(engine:GameEngine, playerID:Int):Boolean {
		if(engine.statc[0]==0&&engine.gameActive) engine.blockOutlineType = GameEngine.BLOCK_OUTLINE_NORMAL
		return super.onGameOver(engine, playerID)
	}

	/* Render results screen */
	override fun renderResult(engine:GameEngine, playerID:Int) {
		receiver.drawMenuFont(engine, playerID, 0, 0, "\u0090\u0093 PAGE${(engine.statc[1]+1)}/2", EventReceiver.COLOR.RED)

		if(engine.statc[1]==0) {
			drawResultStats(engine, playerID, receiver, 2, EventReceiver.COLOR.BLUE, Statistic.SCORE, Statistic.LINES)
			if(engine.statistics.level>=tableGameClearLevel[goaltype]&&(startLevel||goaltype==0))
				drawResult(engine, playerID, receiver, 6, EventReceiver.COLOR.BLUE, "BONUS LINE", bonusLines)
			else
				drawResultStats(engine, playerID, receiver, 6, EventReceiver.COLOR.BLUE, Statistic.LEVEL)
			drawResult(engine, playerID, receiver, 8, EventReceiver.COLOR.BLUE,
				"TOTAL TIME", String.format("%10s", engine.statistics.time.toTimeStr),
				"LV20- TIME", String.format("%10s", (engine.statistics.time-bonusTime).toTimeStr),
				"BONUS TIME", String.format("%10s", bonusTime.toTimeStr))

			drawResultRank(engine, playerID, receiver, 14, EventReceiver.COLOR.BLUE, rankingRank)
			drawResultNetRank(engine, playerID, receiver, 16, EventReceiver.COLOR.BLUE, netRankingRank[0])
			drawResultNetRankDaily(engine, playerID, receiver, 18, EventReceiver.COLOR.BLUE, netRankingRank[1])
		} else
			drawResultStats(engine, playerID, receiver, 2, EventReceiver.COLOR.BLUE, Statistic.SPL, Statistic.SPM, Statistic.LPM,
				Statistic.PPS)

		if(netIsPB) receiver.drawMenuFont(engine, playerID, 2, 21, "NEW PB", EventReceiver.COLOR.ORANGE)

		if(netIsNetPlay&&netReplaySendStatus==1)
			receiver.drawMenuFont(engine, playerID, 0, 22, "SENDING...", EventReceiver.COLOR.PINK)
		else if(netIsNetPlay&&!netIsWatch
			&&netReplaySendStatus==2)
			receiver.drawMenuFont(engine, playerID, 1, 22, "A: RETRY", EventReceiver.COLOR.RED)
	}

	/* Results screen */
	override fun onResult(engine:GameEngine, playerID:Int):Boolean {
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

		return super.onResult(engine, playerID)
	}

	/* Called when saving replay */
	override fun saveReplay(engine:GameEngine, playerID:Int, prop:CustomProperties):Boolean {
		saveSetting(prop, engine)

		// NET: Save name
		if(netPlayerName!=null&&netPlayerName!!.isNotEmpty()) prop.setProperty("$playerID.net.netPlayerName", netPlayerName)

		// Update rankings
		if(!owner.replayMode&&!big&&engine.ai==null) {
			updateRanking(engine.statistics.score, engine.statistics.lines, engine.lives, engine.statistics.time,
				typeSerial(goaltype, turbo, startLevel))

			if(rankingRank!=-1) return true
		}
		return false
	}

	/** Update rankings
	 * @param sc Score
	 * @param li Lines
	 * @param time Time
	 */
	private fun updateRanking(sc:Int, li:Int, lf:Int, time:Int, type:Int) {
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
	private fun checkRanking(sc:Int, li:Int, lf:Int, time:Int, type:Int):Int {
		for(i in 0 until RANKING_MAX)
			if(goaltype>0&&startLevel) {
				if(time<rankingTime[type][i]) return i
				else if(time==rankingTime[type][i]&&sc>rankingScore[type][i]) return i
			} else if(sc>rankingScore[type][i]) return i
			else if(sc==rankingScore[type][i]&&li>rankingLines[type][i]) return i
			else if(sc==rankingScore[type][i]&&li==rankingLines[type][i]&&lf>rankingLives[type][i]) return i

		return -1
	}

	/* NET: Message received */
	@Throws(IOException::class)
	override fun netlobbyOnMessage(lobby:NetLobbyFrame, client:NetPlayerClient, message:Array<String>) {
		super.netlobbyOnMessage(lobby, client, message)

		// Game messages
		if(message[0]=="game") {
			val engine = owner.engine[0]

			// Bonus level entered
			if(message[3]=="bonuslevelenter") {
				engine.meterValue = 0
				owner.bgmStatus.bgm = BGM.GrandM(1)
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
	override fun netRecvField(engine:GameEngine, message:Array<String>) {
		super.netRecvField(engine, message)

		if(engine.statistics.level>=20&&engine.timerActive&&engine.gameActive) bonusLevelProc(engine)
	}

	/** NET: Send various in-game stats
	 * @param engine GameEngine
	 */
	override fun netSendStats(engine:GameEngine) {
		val bg =
			if(engine.owner.backgroundStatus.fadesw) engine.owner.backgroundStatus.fadebg else engine.owner.backgroundStatus.bg
		var msg = "game\tstats\t"
		msg += "${engine.statistics.scoreLine}\t${engine.statistics.scoreSD}\t${engine.statistics.scoreHD}\t${engine.statistics.scoreBonus}\t"
		msg += "${engine.statistics.lines}\t${engine.statistics.totalPieceLocked}\t${engine.statistics.time}\t${engine.statistics.level}\t"
		msg += "${engine.gameActive}\t${engine.timerActive}\t"
		msg += "$lastscore\t$scDisp\t${engine.lastevent}\t${engine.b2bbuf}\t${engine.combobuf}\t${engine.lasteventpiece}\t"
		msg += "$bg\t$bonusLines\t$bonusFlashNow\t$bonusPieceCount\t$bonusTime\n"
		netLobby?.netPlayerClient?.send(msg)
	}

	/** NET: Parse Received [message] as in-game stats of [engine] */
	override fun netRecvStats(engine:GameEngine, message:Array<String>) {

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
			{lastscore = it.toInt()},
			{/*scDisp = it.toInt()*/},
			{engine.lastevent = ScoreEvent.parseInt(it)},
			{engine.b2bbuf = it.toInt()},
			{engine.combobuf = it.toInt()},
			{engine.owner.backgroundStatus.bg = it.toInt()},
			{bonusLines = it.toInt()},
			{bonusFlashNow = it.toInt()},
			{bonusPieceCount = it.toInt()},
			{bonusTime = it.toInt()}).zip(message).forEach {(x, y) ->
			x(y)
		}

		// Meter
		if(engine.statistics.level<20) {
			engine.meterValue = engine.statistics.lines%10*receiver.getMeterMax(engine)/9
			engine.meterColor = GameEngine.METER_COLOR_GREEN
			if(engine.statistics.lines%10>=4) engine.meterColor = GameEngine.METER_COLOR_YELLOW
			if(engine.statistics.lines%10>=6) engine.meterColor = GameEngine.METER_COLOR_ORANGE
			if(engine.statistics.lines%10>=8) engine.meterColor = GameEngine.METER_COLOR_RED
		} else engine.meterValue = 0
	}

	/** NET: Send end-of-game stats
	 * @param engine GameEngine
	 */
	override fun netSendEndGameStats(engine:GameEngine) {
		var subMsg = ""
		subMsg += "SCORE;${engine.statistics.score}\t"
		subMsg += "LINE;${engine.statistics.lines}\t"
		subMsg += "BONUS LINE;$bonusLines\t"
		subMsg += "LEVEL;${(engine.statistics.level+engine.statistics.levelDispAdd)}\t"
		subMsg += "TOTAL TIME;${engine.statistics.time.toTimeStr}\t"
		subMsg += "LV20- TIME;${(engine.statistics.time-bonusTime).toTimeStr}\t"
		subMsg += "BONUS TIME;${bonusTime.toTimeStr}\t"
		subMsg += "SCORE/LINE;${engine.statistics.spl}\t"
		subMsg += "SCORE/MIN;${engine.statistics.spm}\t"
		subMsg += "LINE/MIN;${engine.statistics.lpm}\t"
		subMsg += "PIECE/SEC;${engine.statistics.pps}\t"

		val msg = "gstat1p\t${NetUtil.urlEncode(subMsg)}\n"
		netLobby!!.netPlayerClient!!.send(msg)
	}

	/** NET: Send game options to all spectators
	 * @param engine GameEngine
	 */
	override fun netSendOptions(engine:GameEngine) {
		var msg = "game\toption\t"
		msg += "$startLevel\t$big\n"
		netLobby!!.netPlayerClient!!.send(msg)
	}

	/** NET: Receive game options */
	override fun netRecvOptions(engine:GameEngine, message:Array<String>) {
		startLevel = message[4].toBoolean()
		big = message[5].toBoolean()
	}

	/** NET: Get goal type */
	override fun netGetGoalType():Int = goaltype+if(startLevel) GAMETYPE_MAX else 0

	/** NET: It returns true when the current settings doesn't prevent
	 * leaderboard screen from showing. */
	override fun netIsNetRankingViewOK(engine:GameEngine):Boolean = !big&&engine.ai==null

	companion object {
		/** Current version */
		private const val CURRENT_VERSION = 1

		/** Fall velocity table (numerators) */
		private val tableSpeed = arrayOf(
			intArrayOf(
				5, 8, 10, 12, 16, 20, 24, 30, 36, 42,
				48, 54, 60, 66, 72, 80, 88, 96, 108, 120,
				48, 60, 75, 90, 105, 120, 140, 160, 180, 200,
				240, 280, 320, 360, 420, 480, 600, 720, 840, 960,
				240, 320, 480, 640, 800, 960, 1200, 1440, 1920, 2400, -1),
			intArrayOf(
				1, 2, 3, 4, 5, 6, 7, 8, 9, 10,
				11, 12, 13, 14, 15, 16, 17, 18, 19, 20,
				8, 10, 12, 14, 16, 18, 20, 24, 28, 30,
				32, 40, 48, 64, 80, 96, 112, 128, 114, 160,
				64, 80, 96, 128, 160, 192, 224, 256, 288, 320, -1)
		)
		private val tableDenominator = intArrayOf(240, 32)
		private val tableLineDelay = arrayOf(intArrayOf(12, 24, 16, 0), intArrayOf(6, 18, 8, 0))
		private val ARE_TABLE = intArrayOf(15, 15, 15, 15, 14, 14,
			13, 12, 11, 10, 9, 8, 7, 6, 5, 15)
		private val LOCK_TABLE = intArrayOf(30, 29, 28, 27, 26, 25,
			24, 23, 22, 21, 20, 19, 18, 17, 17, 30)

		// Levels for speed changes
		private val LEVEL_ARE_LOCK_CHANGE = intArrayOf(60, 70, 80, 90, 100,
			110, 120, 130, 140, 150,
			160, 170, 180, 190, 200, 10000)

		/*
* 8 18 30
* 6,13,21,30,40
* 7,14,21,28,35...50
* */
		/** Line counts when Level changes occur */
		private val tableNorma =
			arrayOf(intArrayOf(10), intArrayOf(8, 10, 12), intArrayOf(6, 7, 8, 9, 10), intArrayOf(7, 7, 7, 7, 7, 1))

		/** Line counts when BGM changes occur */
		private val tableBGMChange =
			arrayOf(intArrayOf(100, 150), intArrayOf(80, 180), intArrayOf(130, 300), intArrayOf(140, 280, 350))
		private val tableBGM =
			arrayOf(arrayOf(BGM.Generic(0), BGM.Generic(1), BGM.Generic(2), BGM.GrandM(0)),
				arrayOf(BGM.Puzzle(0), BGM.Generic(2), BGM.Generic(3), BGM.Generic(4)), //30levels
				arrayOf(BGM.Puzzle(1), BGM.Generic(4), BGM.Generic(5), BGM.Generic(6)), //50levels
				arrayOf(BGM.Puzzle(2), BGM.Generic(5), BGM.Generic(6), BGM.Generic(7), BGM.Generic(8)), //200levels
				arrayOf(BGM.Rush(0), BGM.Rush(1), BGM.Rush(2), BGM.Rush(3)))//challenge mode

		/** Ending time */
		private val ROLLTIMELIMIT = intArrayOf(-1, 5000, 7500, 10000)

		/** Line counts when game ending occurs */
		private val tableGameClearLevel = intArrayOf(20, 30, 50, 200)
		private val tableGameClearLines = tableGameClearLevel.mapIndexed {i, l ->
			tableNorma[i].sumOf {it*10}+tableNorma[i].last {true}*(l-tableNorma[i].size*10)
		}.toIntArray()
		/** Number of game types */
		private val GAMETYPE_MAX = tableGameClearLevel.size

		private val TURBO_MAX = tableSpeed.size

		/** Number of entries in rankings */
		private const val RANKING_MAX = 13

		/** Number of ranking types */
		private val RANKING_TYPE = GAMETYPE_MAX*TURBO_MAX*2
		private fun typeSerial(type:Int, turbo:Int, startLevel:Boolean):Int = type*TURBO_MAX*2+turbo*2+if(startLevel) 1 else 0
	}
}
