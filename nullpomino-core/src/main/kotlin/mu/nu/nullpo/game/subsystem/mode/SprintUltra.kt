/*
 * Copyright (c) 2010-2022, NullNoname
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
import mu.nu.nullpo.game.component.Controller
import mu.nu.nullpo.game.event.EventReceiver
import mu.nu.nullpo.game.event.ScoreEvent
import mu.nu.nullpo.game.net.NetUtil
import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.game.subsystem.mode.menu.BooleanMenuItem
import mu.nu.nullpo.game.subsystem.mode.menu.DelegateMenuItem
import mu.nu.nullpo.game.subsystem.mode.menu.StringsMenuItem
import mu.nu.nullpo.gui.common.BaseFont
import mu.nu.nullpo.util.CustomProperties
import mu.nu.nullpo.util.GeneralUtil.toTimeStr

/** ULTRA Mode */
class SprintUltra:NetDummyMode() {

	private var pow = 0

	/** Most recent scoring event b2b */
	private var lastb2b = false

	/** Most recent scoring event combo count */
	private var lastcombo = 0

	/** Most recent scoring event piece ID */
	private var lastpiece = 0

	private val itemBig = BooleanMenuItem("big", "BIG", EventReceiver.COLOR.BLUE, false)
	/** BigMode */
	private var big:Boolean by DelegateMenuItem(itemBig)

	private val itemGoal = StringsMenuItem(
		"goaltype", "Length", EventReceiver.COLOR.BLUE,
		0, tableLength.map {"$it Min"}.toTypedArray()
	)
	/** Time limit type */
	private var goaltype:Int by DelegateMenuItem(itemGoal)

	/** Last preset number used */
	private var presetNumber = 0

	/** Version */
	private var version = 0

	/** Current round's ranking rank */
	private var rankingRank = IntArray(RANKTYPE_MAX)

	/** Rankings' scores */
	private var rankingScore:Array<Array<IntArray>> = Array(RANKTYPE_MAX) {Array(GOALTYPE_MAX) {IntArray(RANKING_MAX)}}

	/** Rankings' sent line counts */
	private var rankingPower:Array<Array<IntArray>> = Array(RANKTYPE_MAX) {Array(GOALTYPE_MAX) {IntArray(RANKING_MAX)}}

	/** Rankings' line counts */
	private var rankingLines:Array<Array<IntArray>> = Array(RANKTYPE_MAX) {Array(GOALTYPE_MAX) {IntArray(RANKING_MAX)}}

	private var rankingShow = 0
	override val rankMap:Map<String, IntArray>
		get() = mapOf(
			*(
				(rankingScore.mapIndexed {a, x -> x.mapIndexed {b, y -> "$a.$b.score" to y}}+
					rankingPower.mapIndexed {a, x -> x.mapIndexed {b, y -> "$a.$b.power" to y}}+
					rankingLines.mapIndexed {a, x -> x.mapIndexed {b, y -> "$a.$b.lines" to y}}).flatten().toTypedArray())
		)
	/* Mode name */
	override val name = "ULTRA Score Attack"
	override val gameIntensity = 2

	/* Initialization */
	override fun playerInit(engine:GameEngine, playerID:Int) {
		super.playerInit(engine, playerID)
		lastscore = 0
		lastb2b = false
		lastcombo = 0
		lastpiece = 0
		pow = 0
		rankingShow = 0

		rankingRank = IntArray(RANKTYPE_MAX) {-1}

		rankingScore = Array(RANKTYPE_MAX) {Array(GOALTYPE_MAX) {IntArray(RANKING_MAX)}}
		rankingLines = Array(RANKTYPE_MAX) {Array(GOALTYPE_MAX) {IntArray(RANKING_MAX)}}
		rankingPower = Array(RANKTYPE_MAX) {Array(GOALTYPE_MAX) {IntArray(RANKING_MAX)}}

		engine.framecolor = GameEngine.FRAME_COLOR_BLUE

		netPlayerInit(engine, playerID)

		if(!engine.owner.replayMode) {
			presetNumber = engine.owner.modeConfig.getProperty("ultra.presetNumber", 0)
			loadPreset(engine, engine.owner.modeConfig, -1)

			version = CURRENT_VERSION
		} else {
			presetNumber = 0
			loadPreset(engine, engine.owner.replayProp, -1)
			version = engine.owner.replayProp.getProperty("ultra.version", 0)
			// NET: Load name
			netPlayerName = engine.owner.replayProp.getProperty("$playerID.net.netPlayerName", "")
		}
	}

	/** Load options from a preset
	 * @param engine GameEngine
	 * @param prop Property file to read from
	 * @param preset Preset number
	 */
	private fun loadPreset(engine:GameEngine, prop:CustomProperties, preset:Int) {
		engine.speed.gravity = prop.getProperty("ultra.gravity.$preset", 1)
		engine.speed.denominator = prop.getProperty("ultra.denominator.$preset", 256)
		engine.speed.are = prop.getProperty("ultra.are.$preset", 0)
		engine.speed.areLine = prop.getProperty("ultra.areLine.$preset", 0)
		engine.speed.lineDelay = prop.getProperty("ultra. lineDelay.$preset", 0)
		engine.speed.lockDelay = prop.getProperty("ultra.lockDelay.$preset", 30)
		engine.speed.das = prop.getProperty("ultra.das.$preset", 10)
		big = prop.getProperty("ultra.big.$preset", false)
		goaltype = prop.getProperty("ultra.goaltype.$preset", 0)
	}

	/** Save options to a preset
	 * @param engine GameEngine
	 * @param prop Property file to save to
	 * @param preset Preset number
	 */
	private fun savePreset(engine:GameEngine, prop:CustomProperties, preset:Int) {
		prop.setProperty("ultra.gravity.$preset", engine.speed.gravity)
		prop.setProperty("ultra.denominator.$preset", engine.speed.denominator)
		prop.setProperty("ultra.are.$preset", engine.speed.are)
		prop.setProperty("ultra.areLine.$preset", engine.speed.areLine)
		prop.setProperty("ultra.lineDelay.$preset", engine.speed.lineDelay)
		prop.setProperty("ultra.lockDelay.$preset", engine.speed.lockDelay)
		prop.setProperty("ultra.das.$preset", engine.speed.das)
		prop.setProperty("ultra.big.$preset", big)
		prop.setProperty("ultra.goaltype.$preset", goaltype)
	}

	/* Called at settings screen */
	override fun onSetting(engine:GameEngine, playerID:Int):Boolean {
		// NET: Net Ranking
		if(netIsNetRankingDisplayMode)
			netOnUpdateNetPlayRanking(engine, goaltype)
		else if(!engine.owner.replayMode) {
			// Configuration changes
			val change = updateCursor(engine, 17)

			if(change!=0) {
				engine.playSE("change")

				var m = 1
				if(engine.ctrl.isPress(Controller.BUTTON_E)) m = 100
				if(engine.ctrl.isPress(Controller.BUTTON_F)) m = 1000

				when(menuCursor) {

					0 -> engine.speed.gravity = rangeCursor(engine.speed.gravity+change*m, -1, 99999)
					1 -> engine.speed.denominator = rangeCursor(change*m, -1, 99999)
					2 -> engine.speed.are = rangeCursor(engine.speed.are+change, 0, 99)
					3 -> engine.speed.areLine = rangeCursor(engine.speed.areLine+change, 0, 99)
					4 -> engine.speed.lineDelay = rangeCursor(engine.speed.lineDelay+change, 0, 99)
					5 -> engine.speed.lockDelay = rangeCursor(engine.speed.lockDelay+change, 0, 99)
					6 -> engine.speed.das = rangeCursor(engine.speed.das+change, 0, 99)
					7 -> big = !big
					8 -> {
						goaltype += change
						if(goaltype<0) goaltype = GOALTYPE_MAX-1
						if(goaltype>GOALTYPE_MAX-1) goaltype = 0
					}
					10, 11 -> presetNumber = rangeCursor(presetNumber+change, 0, 99)
				}

				// NET: Signal options change
				if(netIsNetPlay&&netNumSpectators>0) netSendOptions(engine)
			}

			// Confirm
			if(engine.ctrl.isPush(Controller.BUTTON_A)&&menuTime>=5) {
				engine.playSE("decide")

				if(menuCursor==16) {
					loadPreset(engine, owner.modeConfig, presetNumber)

					// NET: Signal options change
					if(netIsNetPlay&&netNumSpectators>0) netSendOptions(engine)
				} else if(menuCursor==17) {
					savePreset(engine, owner.modeConfig, presetNumber)
					owner.saveModeConfig()
				} else {
					owner.modeConfig.setProperty("ultra.presetNumber", presetNumber)
					savePreset(engine, owner.modeConfig, -1)
					owner.saveModeConfig()

					// NET: Signal start of the game
					if(netIsNetPlay) netLobby!!.netPlayerClient!!.send("start1p\n")

					return false
				}
			}

			// Cancel
			if(engine.ctrl.isPush(Controller.BUTTON_B)&&!netIsNetPlay) engine.quitflag = true

			// NET: Netplay Ranking
			if(engine.ctrl.isPush(Controller.BUTTON_D)&&netIsNetPlay&&!netIsWatch
				&&netIsNetRankingViewOK(engine)
			)
				netEnterNetPlayRankingScreen(engine, playerID, goaltype)
			if(engine.ctrl.isPush(Controller.BUTTON_F)) {
				rankingShow++
				if(rankingShow>=RANKTYPE_MAX) rankingShow = 0
			}
			menuTime++
		} else {
			menuTime++
			menuCursor = 0

			if(menuTime>=60) menuCursor = 10
			return menuTime<120
		}// Replay

		return true
	}

	/* Setting screen drawing */
	override fun renderSetting(engine:GameEngine, playerID:Int) {
		if(netIsNetRankingDisplayMode)
		// NET: Netplay Ranking
			netOnRenderNetPlayRanking(engine, playerID, receiver)
		else {
			drawMenuSpeeds(engine, playerID, receiver, 0, EventReceiver.COLOR.BLUE, 0)
			drawMenuCompact(engine, playerID, receiver, "BIG" to big, "Length" to "${tableLength[goaltype]} Min")
			if(!engine.owner.replayMode) {
				menuColor = EventReceiver.COLOR.GREEN
				drawMenuCompact(engine, playerID, receiver, "LOAD" to presetNumber, "SAVE" to presetNumber)
			}
		}
	}

	/* This function will be called before the game actually begins (after
 * Ready&Go screen disappears) */
	override fun startGame(engine:GameEngine, playerID:Int) {
		engine.big = big
		engine.b2bEnable = true
		engine.splitb2b = true
		engine.comboType = GameEngine.COMBO_TYPE_NORMAL
		engine.meterValue = 320
		engine.meterColor = GameEngine.METER_COLOR_GREEN

		owner.bgmStatus.bgm = if(netIsWatch) BGM.Silent
		else if(tableLength[goaltype]<=3) BGM.Blitz(0) else BGM.Blitz(1)
		engine.twistAllowKick = true
		engine.twistEnable = true
		engine.useAllSpinBonus = true
		engine.twistEnableEZ = true
	}

	/* Render score */
	override fun renderLast(engine:GameEngine, playerID:Int) {
		if(owner.menuOnly) return

		receiver.drawScoreFont(engine, playerID, 0, -3, name, EventReceiver.COLOR.CYAN)
		receiver.drawScoreFont(engine, playerID, 0, -2, "(${(tableLength[goaltype])} Minutes sprint)", EventReceiver.COLOR.CYAN)

		if(engine.stat==GameEngine.Status.SETTING||engine.stat==GameEngine.Status.RESULT&&!owner.replayMode) {
			if(!owner.replayMode&&!big&&engine.ai==null) {
				receiver.drawScoreFont(engine, playerID, 0, 0, "Score RANKING", EventReceiver.COLOR.GREEN)
				receiver.drawScoreFont(engine, playerID, 3, 1, "Score Power Lines", EventReceiver.COLOR.BLUE)

				for(i in 0 until minOf(RANKING_MAX, 12)) {
					receiver.drawScoreGrade(engine, playerID, 0, 2+i, String.format("%2d", i+1), EventReceiver.COLOR.YELLOW)
					receiver.drawScoreNum(
						engine, playerID, 3, 2+i, String.format("%7d", rankingScore[0][goaltype][i]),
						i==rankingRank[0]
					)
					receiver.drawScoreNum(
						engine, playerID, 9, 2+i, String.format("%5d", rankingPower[0][goaltype][i]),
						i==rankingRank[0]
					)
					receiver.drawScoreNum(
						engine, playerID, 15, 2+i, String.format("%5d", rankingLines[0][goaltype][i]),
						i==rankingRank[0]
					)
				}

				receiver.drawScoreFont(engine, playerID, 0, 8, "Power RANKING", EventReceiver.COLOR.GREEN)
				receiver.drawScoreFont(engine, playerID, 3, 9, "Power Score Lines", EventReceiver.COLOR.BLUE)

				for(i in 0 until RANKING_MAX) {
					receiver.drawScoreGrade(engine, playerID, 0, 10+i, String.format("%2d", i+1), EventReceiver.COLOR.YELLOW)
					receiver.drawScoreNum(
						engine, playerID, 3, 10+i, String.format("%5d", rankingPower[1][goaltype][i]),
						i==rankingRank[1]
					)
					receiver.drawScoreNum(
						engine, playerID, 9, 10+i, String.format("%7d", rankingScore[1][goaltype][i]),
						i==rankingRank[1]
					)
					receiver.drawScoreNum(
						engine, playerID, 15, 10+i, String.format("%5d", rankingLines[1][goaltype][i]),
						i==rankingRank[1]
					)
				}

				receiver.drawScoreFont(engine, playerID, 0, 16, "Lines RANKING", EventReceiver.COLOR.GREEN)
				receiver.drawScoreFont(engine, playerID, 3, 17, "Lines Score Power", EventReceiver.COLOR.BLUE)

				for(i in 0 until RANKING_MAX) {
					receiver.drawScoreGrade(engine, playerID, 0, 18+i, String.format("%2d", i+1), EventReceiver.COLOR.YELLOW)
					receiver.drawScoreNum(
						engine, playerID, 3, 18+i, String.format("%5d", rankingLines[2][goaltype][i]),
						i==rankingRank[1]
					)
					receiver.drawScoreNum(
						engine, playerID, 9, 18+i, String.format("%7d", rankingScore[2][goaltype][i]),
						i==rankingRank[1]
					)
					receiver.drawScoreNum(
						engine, playerID, 15, 18+i, String.format("%5d", rankingPower[2][goaltype][i]),
						i==rankingRank[1]
					)
				}
			}
		} else {
			receiver.drawScoreFont(engine, playerID, 0, 3, "Score", EventReceiver.COLOR.BLUE)
			receiver.drawScoreNum(engine, playerID, 5, 3, "+$lastscore")
			receiver.drawScoreNum(engine, playerID, 0, 4, "$scDisp", scDisp<engine.statistics.score, 2f)


			receiver.drawScoreFont(engine, playerID, 0, 6, "Spike", EventReceiver.COLOR.BLUE)
			receiver.drawScoreNum(engine, playerID, 0, 7, "${engine.statistics.attacks}", 2f)

			receiver.drawScoreFont(engine, playerID, 0, 9, "Lines", EventReceiver.COLOR.BLUE)
			receiver.drawScoreNum(engine, playerID, 5, 8, "${engine.statistics.lines}", 2f)

			receiver.drawScoreFont(engine, playerID, 0, 10, "SCORE/MIN", EventReceiver.COLOR.BLUE)
			receiver.drawScoreNum(
				engine, playerID, 0, 11, String.format("%7g", engine.statistics.spm),
				scDisp<engine.statistics.score, 2f
			)

			receiver.drawScoreFont(engine, playerID, 0, 13, "LINE/MIN", EventReceiver.COLOR.BLUE)
			receiver.drawScoreNum(engine, playerID, 0, 14, "${engine.statistics.lpm}", 2f)

			receiver.drawScoreFont(engine, playerID, 0, 15, "Time", EventReceiver.COLOR.BLUE)
			val time = maxOf(0, (tableLength[goaltype])*3600-engine.statistics.time)
			receiver.drawScoreNum(engine, playerID, 0, 16, time.toTimeStr, getTimeFontColor(time), 2f)
		}

		super.renderLast(engine, playerID)
	}

	/* Calculate score */
	override fun calcScore(engine:GameEngine, playerID:Int, lines:Int):Int {
		// Line clear bonus
		val pts = calcScoreBase(engine, lines)
		val spd = maxOf(0, engine.lockDelay-engine.lockDelayNow)+if(engine.manualLock) 1 else 0
		// Combo
		val cmb = if(engine.combo>=1&&lines>=1) engine.combo-1 else 0
		// Add to score
		if(pts+cmb+spd>0) {
			val get = calcScoreCombo(pts, cmb, pow/2, spd)

			if(pts>0) lastscore = get
			if(lines>=1) engine.statistics.scoreLine += get
			else engine.statistics.scoreBonus += get
			scDisp += spd
		}
		engine.statistics.attacks += calcPower(engine, lines)
		return if(pts>0) lastscore else 0
	}

	/* Soft drop */
	override fun afterSoftDropFall(engine:GameEngine, playerID:Int, fall:Int) {
		engine.statistics.scoreSD += fall
	}

	/* Hard drop */
	override fun afterHardDropFall(engine:GameEngine, playerID:Int, fall:Int) {
		engine.statistics.scoreHD += fall*2
	}

	/* Each frame Processing at the end of */
	override fun onLast(engine:GameEngine, playerID:Int) {
		super.onLast(engine, playerID)
		if(engine.gameActive&&engine.timerActive) {
			val limitTime = (tableLength[goaltype])*3600
			val remainTime = limitTime-engine.statistics.time

			// Time meter
			engine.meterValue = remainTime*receiver.getMeterMax(engine)/limitTime
			engine.meterColor = GameEngine.METER_COLOR_GREEN
			if(remainTime<=30*60) engine.meterColor = GameEngine.METER_COLOR_YELLOW
			if(remainTime<=20*60) engine.meterColor = GameEngine.METER_COLOR_ORANGE
			if(remainTime<=10*60) engine.meterColor = GameEngine.METER_COLOR_RED

			if(!netIsWatch) {
				// Out of time
				if(engine.statistics.time>=limitTime) {
					engine.gameEnded()
					engine.resetStatc()
					engine.stat = GameEngine.Status.ENDINGSTART
					return
				}

				// 10Seconds before the countdown
				if(engine.statistics.time>=limitTime-10*60&&engine.statistics.time%60==0) engine.playSE("countdown")

				// 5 seconds beforeBGM fadeout
//				if(engine.statistics.time>=limitTime-5*60) owner.bgmStatus.fadesw = true

				// 1Per-minuteBackgroundSwitching
				if(engine.statistics.time>0&&engine.statistics.time%3600==0) {
					engine.playSE("levelup")
					owner.backgroundStatus.fadesw = true
					owner.backgroundStatus.fadebg = owner.backgroundStatus.bg+1
				}
			}
		}
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

	/* Render results screen */
	override fun renderResult(engine:GameEngine, playerID:Int) {

		receiver.drawMenuFont(
			engine,
			playerID,
			0,
			0,
			"${BaseFont.UP_L}${BaseFont.DOWN_L} PAGE${engine.statc[1]+1}/2",
			EventReceiver.COLOR.RED
		)
		if(rankingRank[0]==0) receiver.drawMenuFont(engine, playerID, 0, 3, "NEW RECORD", EventReceiver.COLOR.ORANGE)
		else if(rankingRank[0]!=-1)
			receiver.drawMenuFont(engine, playerID, 4, 3, String.format("RANK %d", rankingRank[0]+1), EventReceiver.COLOR.ORANGE)
		if(rankingRank[1]==0) receiver.drawMenuFont(engine, playerID, 0, 6, "NEW RECORD", EventReceiver.COLOR.ORANGE)
		else if(rankingRank[1]!=-1)
			receiver.drawMenuFont(engine, playerID, 4, 6, String.format("RANK %d", rankingRank[1]+1), EventReceiver.COLOR.ORANGE)
		if(rankingRank[2]==0) receiver.drawMenuFont(engine, playerID, 0, 9, "NEW RECORD", EventReceiver.COLOR.ORANGE)
		else if(rankingRank[2]!=-1)
			receiver.drawMenuFont(engine, playerID, 4, 9, String.format("RANK %d", rankingRank[2]+1), EventReceiver.COLOR.ORANGE)

		when(engine.statc[1]) {
			0 -> {
				drawResultStats(engine, playerID, receiver, 1, EventReceiver.COLOR.BLUE, Statistic.SCORE)
				drawResultStats(engine, playerID, receiver, 4, EventReceiver.COLOR.BLUE, Statistic.ATTACKS)
				drawResultStats(engine, playerID, receiver, 7, EventReceiver.COLOR.BLUE, Statistic.LINES)
				drawResultStats(
					engine, playerID, receiver, 10, EventReceiver.COLOR.BLUE, Statistic.PPS, Statistic.VS
				)
			}
			1 -> {
				drawResultStats(engine, playerID, receiver, 1, EventReceiver.COLOR.BLUE, Statistic.SPM)
				drawResultStats(engine, playerID, receiver, 4, EventReceiver.COLOR.BLUE, Statistic.APM)
				drawResultStats(engine, playerID, receiver, 7, EventReceiver.COLOR.BLUE, Statistic.LPM)

				drawResultStats(engine, playerID, receiver, 10, EventReceiver.COLOR.BLUE, Statistic.SPL, Statistic.APL)
			}
		}
		drawResultNetRank(engine, playerID, receiver, 14, EventReceiver.COLOR.BLUE, netRankingRank[0])
		drawResultNetRankDaily(engine, playerID, receiver, 16, EventReceiver.COLOR.BLUE, netRankingRank[1])

		if(netIsPB) receiver.drawMenuFont(engine, playerID, 2, 21, "NEW PB", EventReceiver.COLOR.ORANGE)

		if(netIsNetPlay&&netReplaySendStatus==1)
			receiver.drawMenuFont(engine, playerID, 0, 22, "SENDING...", EventReceiver.COLOR.PINK)
		else if(netIsNetPlay&&!netIsWatch
			&&netReplaySendStatus==2
		)
			receiver.drawMenuFont(engine, playerID, 1, 22, "A: RETRY", EventReceiver.COLOR.RED)
	}

	/* Called when saving replay */
	override fun saveReplay(engine:GameEngine, playerID:Int, prop:CustomProperties):Boolean {
		savePreset(engine, engine.owner.replayProp, -1)
		engine.owner.replayProp.setProperty("ultra.version", version)

		// NET: Save name
		if(netPlayerName!=null&&netPlayerName!!.isNotEmpty()) prop.setProperty("$playerID.net.netPlayerName", netPlayerName)

		// Update rankings
		if(!owner.replayMode&&!big&&engine.ai==null) {
			if(updateRanking(goaltype, engine.statistics.score, engine.statistics.attacks, engine.statistics.lines).any {it!=-1})
				return true
		}
		return false
	}
	/** Update rankings
	 * @param sc Score
	 * @param po Power
	 * @param li Lines
	 */
	private fun updateRanking(goal:Int, sc:Int, po:Int, li:Int):IntArray {
		rankingRank = RANKTYPE_ALL.mapIndexed {i, it ->
			val ret = checkRanking(it, goal, sc, po, li)
			if(ret!=-1) {
				// Shift down ranking entries
				for(j in RANKING_MAX-1 downTo ret+1) {
					rankingScore[i][goal][j] = rankingScore[i][goal][j-1]
					rankingPower[i][goal][j] = rankingPower[i][goal][j-1]
					rankingLines[i][goal][j] = rankingLines[i][goal][j-1]
				}

				// Add new data
				rankingScore[i][goal][ret] = sc
				rankingPower[i][goal][ret] = po
				rankingLines[i][goal][ret] = li
			}
			ret
		}.toIntArray()
		return rankingRank
	}

	/** Calculate ranking position
	 * @param sc Score
	 * @param li Lines
	 * @param type Number of ranking types
	 * @return Position (-1 if unranked)
	 */
	private fun checkRanking(type:RANKING_TYPE, goal:Int, sc:Int, po:Int, li:Int):Int {
		val ord = type.ordinal
		for(i in 0 until RANKING_MAX)
			when(type) {
				RANKING_TYPE.Score ->
					when {
						sc>rankingScore[ord][goal][i] -> return i
						sc==rankingScore[ord][goal][i]&&po>rankingPower[ord][goal][i] -> return i
						sc==rankingScore[ord][goal][i]&&po==rankingPower[ord][goal][i]&&li>rankingLines[ord][goal][i] -> return i
					}
				RANKING_TYPE.Power -> when {
					po>rankingPower[ord][goal][i] -> return i
					po==rankingPower[ord][goal][i]&&sc>rankingScore[ord][goal][i] -> return i
					po==rankingPower[ord][goal][i]&&sc==rankingScore[ord][goal][i]&&li>rankingLines[ord][goal][i] -> return i

				}
				RANKING_TYPE.Lines -> when {
					li>rankingLines[ord][goal][i] -> return i
					li==rankingLines[ord][goal][i]&&sc>rankingScore[ord][goal][i] -> return i
					li==rankingLines[ord][goal][i]&&sc>rankingScore[ord][goal][i]&&po>rankingPower[ord][goal][i] -> return i
				}
			}

		return -1
	}

	/** NET: Send various in-game stats of [engine] */
	override fun netSendStats(engine:GameEngine) {
		val bg = if(owner.backgroundStatus.fadesw) owner.backgroundStatus.fadebg else owner.backgroundStatus.bg
		val msg = "game\tstats\t"+
			"${engine.statistics.scoreLine}\t${engine.statistics.scoreSD}\t${engine.statistics.scoreHD}\t"+
			"${engine.statistics.scoreBonus}\t${engine.statistics.lines}\t"+
			"${engine.statistics.totalPieceLocked}\t${engine.statistics.time}\t$goaltype\t"+
			"${engine.gameActive}\t${engine.timerActive}\t"+
			"$lastscore\t$scDisp\t${engine.lastevent}\t$lastb2b\t$lastcombo\t$lastpiece\t"+
			"$bg\n"
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
			{goaltype = it.toInt()},
			{engine.gameActive = it.toBoolean()},
			{engine.timerActive = it.toBoolean()},
			{lastscore = it.toInt()},
			{/*scDisp = it.toInt()*/},
			{engine.lastevent = ScoreEvent.parseInt(it)},
			{lastb2b = it.toBoolean()},
			{lastcombo = it.toInt()},
			{lastpiece = it.toInt()},
			{owner.backgroundStatus.bg = it.toInt()}).zip(message).forEach {(x, y) ->
			x(y)
		}

		// Time meter
		val limitTime = (goaltype+1)*3600
		val remainTime = (goaltype+1)*3600-engine.statistics.time
		engine.meterValue = remainTime*receiver.getMeterMax(engine)/limitTime
		engine.meterColor = GameEngine.METER_COLOR_GREEN
		if(remainTime<=30*60) engine.meterColor = GameEngine.METER_COLOR_YELLOW
		if(remainTime<=20*60) engine.meterColor = GameEngine.METER_COLOR_ORANGE
		if(remainTime<=10*60) engine.meterColor = GameEngine.METER_COLOR_RED
	}

	/** NET: Send end-of-game stats
	 * @param engine GameEngine
	 */
	override fun netSendEndGameStats(engine:GameEngine) {
		var subMsg = ""
		subMsg += "SCORE;${engine.statistics.score}\t"
		subMsg += "LINE;${engine.statistics.lines}\t"
		subMsg += "PIECE;${engine.statistics.totalPieceLocked}\t"
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
		msg += "${engine.speed.gravity}\t${engine.speed.denominator}\t${engine.speed.are}\t"
		msg += "${engine.speed.areLine}\t${engine.speed.lineDelay}\t${engine.speed.lockDelay}\t"
		msg += "${engine.speed.das}\t$big\t$goaltype\t$presetNumber\t"
		netLobby!!.netPlayerClient!!.send(msg)
	}

	/** NET: Receive game options */
	override fun netRecvOptions(engine:GameEngine, message:Array<String>) {
		engine.speed.gravity = message[4].toInt()
		engine.speed.denominator = message[5].toInt()
		engine.speed.are = message[6].toInt()
		engine.speed.areLine = message[7].toInt()
		engine.speed.lineDelay = message[8].toInt()
		engine.speed.lockDelay = message[9].toInt()
		engine.speed.das = message[10].toInt()
		big = message[11].toBoolean()
		goaltype = message[12].toInt()
		presetNumber = message[13].toInt()
	}

	/** NET: Get goal type */
	override fun netGetGoalType():Int = goaltype

	/** NET: It returns true when the current settings doesn't prevent
	 * leaderboard screen from showing. */
	override fun netIsNetRankingViewOK(engine:GameEngine):Boolean = !big&&engine.ai==null

	companion object {
		/** Current version */
		private const val CURRENT_VERSION = 1

		/** Number of entries in rankings */
		private const val RANKING_MAX = 5

		/** Line counts when game ending occurs */
		private val tableLength = intArrayOf(3, 5)

		/** Number of ranking types */
		private enum class RANKING_TYPE { Score, Power, Lines }

		private val RANKTYPE_ALL = SprintUltra.Companion.RANKING_TYPE.values()
		private val RANKTYPE_MAX = RANKING_TYPE.values().size

		/** Time limit type */
		private val GOALTYPE_MAX = tableLength.size

	}
}
