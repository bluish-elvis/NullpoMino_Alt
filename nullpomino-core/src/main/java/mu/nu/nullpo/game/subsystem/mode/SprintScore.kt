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
import mu.nu.nullpo.game.component.Controller
import mu.nu.nullpo.game.event.EventReceiver
import mu.nu.nullpo.game.net.NetUtil
import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.game.subsystem.mode.menu.BooleanMenuItem
import mu.nu.nullpo.game.subsystem.mode.menu.DelegateMenuItem
import mu.nu.nullpo.util.CustomProperties
import mu.nu.nullpo.util.GeneralUtil.toTimeStr
import org.apache.log4j.Logger

/** SCORE RACE Mode */
class SprintScore:NetDummyMode() {

	/** Most recent scoring event b2b */
	private var lastb2b = false

	/** Most recent scoring event combo count */
	private var lastcombo = 0

	/** Most recent scoring event piece ID */
	private var lastpiece = 0

	/** BGM number */
	private var bgmno = 0

	/** Flag for types of Twisters allowed (0=none, 1=normal, 2=all spin) */
	private var twistEnableType = 0

	/** Old flag for allowing Twisters */
	private var enableTwist = false

	/** Flag for enabling wallkick Twisters */
	private var enableTwistKick = false

	/** Immobile EZ spin */
	private var twistEnableEZ = false

	/** Flag for enabling B2B */
	private var enableB2B = false

	private var enableSplitB2B = false

	/** Flag for enabling combos */
	private var enableCombo = false

	private val itemBig = BooleanMenuItem("big", "BIG", EventReceiver.COLOR.BLUE, false)
	/** BigMode */
	private var big:Boolean by DelegateMenuItem(itemBig)

	/** Goal score type */
	private var goaltype = 0

	/** Last preset number used */
	private var presetNumber = 0

	/** Version */
	private var version = 0

	/** Current round's ranking rank */
	private var rankingRank = 0

	/** Rankings' times */
	private var rankingTime:Array<IntArray> = Array(GOALTYPE_MAX) {IntArray(RANKING_MAX)}

	/** Rankings' line counts */
	private var rankingLines:Array<IntArray> = Array(GOALTYPE_MAX) {IntArray(RANKING_MAX)}

	/** Rankings' score/line */
	private var rankingSPL:Array<DoubleArray> = Array(GOALTYPE_MAX) {DoubleArray(RANKING_MAX)}

	/* Mode name */
	override val name = "Score SprintRace"
	override val gameIntensity = 2

	/* Initialization */
	override fun playerInit(engine:GameEngine, playerID:Int) {
		super.playerInit(engine, playerID)
		lastscore = 0
		scDisp = 0
		lastb2b = false
		lastcombo = 0
		lastpiece = 0
		bgmno = 0

		rankingRank = -1
		rankingTime = Array(GOALTYPE_MAX) {IntArray(RANKING_MAX)}
		rankingLines = Array(GOALTYPE_MAX) {IntArray(RANKING_MAX)}
		rankingSPL = Array(GOALTYPE_MAX) {DoubleArray(RANKING_MAX)}

		engine.framecolor = GameEngine.FRAME_COLOR_BRONZE

		netPlayerInit(engine, playerID)

		if(!engine.owner.replayMode) {
			presetNumber = engine.owner.modeConfig.getProperty("scorerace.presetNumber", 0)
			loadPreset(engine, engine.owner.modeConfig, -1)

			version = CURRENT_VERSION
		} else {
			presetNumber = 0
			loadPreset(engine, engine.owner.replayProp, -1)
			version = engine.owner.replayProp.getProperty("scorerace.version", 0)
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
		engine.speed.gravity = prop.getProperty("scorerace.gravity.$preset", 1)
		engine.speed.denominator = prop.getProperty("scorerace.denominator.$preset", 256)
		engine.speed.are = prop.getProperty("scorerace.are.$preset", 0)
		engine.speed.areLine = prop.getProperty("scorerace.areLine.$preset", 0)
		engine.speed.lineDelay = prop.getProperty("scorerace. lineDelay.$preset", 0)
		engine.speed.lockDelay = prop.getProperty("scorerace.lockDelay.$preset", 30)
		engine.speed.das = prop.getProperty("scorerace.das.$preset", 10)
		bgmno = prop.getProperty("scorerace.bgmno.$preset", BGM.values.indexOf(BGM.Rush(0)))
		twistEnableType = prop.getProperty("scorerace.twistEnableType.$preset", 1)
		enableTwist = prop.getProperty("scorerace.enableTwist.$preset", true)
		enableTwistKick = prop.getProperty("scorerace.enableTwistKick.$preset", true)
		twistEnableEZ = prop.getProperty("scorerace.twistEnableEZ.$preset", false)
		enableB2B = prop.getProperty("scorerace.enableB2B.$preset", true)
		enableCombo = prop.getProperty("scorerace.enableCombo.$preset", true)
		big = prop.getProperty("scorerace.big.$preset", false)
		goaltype = prop.getProperty("scorerace.goaltype.$preset", 1)
	}

	/** Save options to a preset
	 * @param engine GameEngine
	 * @param prop Property file to save to
	 * @param preset Preset number
	 */
	private fun savePreset(engine:GameEngine, prop:CustomProperties, preset:Int) {
		prop.setProperty("scorerace.gravity.$preset", engine.speed.gravity)
		prop.setProperty("scorerace.denominator.$preset", engine.speed.denominator)
		prop.setProperty("scorerace.are.$preset", engine.speed.are)
		prop.setProperty("scorerace.areLine.$preset", engine.speed.areLine)
		prop.setProperty("scorerace.lineDelay.$preset", engine.speed.lineDelay)
		prop.setProperty("scorerace.lockDelay.$preset", engine.speed.lockDelay)
		prop.setProperty("scorerace.das.$preset", engine.speed.das)
		prop.setProperty("scorerace.bgmno.$preset", bgmno)
		prop.setProperty("scorerace.twistEnableType.$preset", twistEnableType)
		prop.setProperty("scorerace.enableTwist.$preset", enableTwist)
		prop.setProperty("scorerace.enableTwistKick.$preset", enableTwistKick)
		prop.setProperty("scorerace.twistEnableEZ.$preset", twistEnableEZ)
		prop.setProperty("scorerace.enableB2B.$preset", enableB2B)
		prop.setProperty("scorerace.enableCombo.$preset", enableCombo)
		prop.setProperty("scorerace.big.$preset", big)
		prop.setProperty("scorerace.goaltype.$preset", goaltype)
	}

	/* Called at settings screen */
	override fun onSetting(engine:GameEngine, playerID:Int):Boolean {
		// NET: Net Ranking
		if(netIsNetRankingDisplayMode)
			netOnUpdateNetPlayRanking(engine, goaltype)
		else if(!engine.owner.replayMode) {
			// Configuration changes
			val change = updateCursor(engine, 16, playerID)

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
					7 -> bgmno = rangeCursor(bgmno+change, 0, BGM.count-1)
					8 -> big = !big
					9 -> {
						goaltype += change
						if(goaltype<0) goaltype = GOALTYPE_MAX-1
						if(goaltype>GOALTYPE_MAX-1) goaltype = 0
					}
					10 -> {
						twistEnableType += change
						if(twistEnableType<0) twistEnableType = 2
						if(twistEnableType>2) twistEnableType = 0
					}
					11 -> enableTwistKick = !enableTwistKick
					12 -> twistEnableEZ = !twistEnableEZ
					13 -> enableB2B = !enableB2B
					14 -> enableCombo = !enableCombo
					15, 16 -> presetNumber = rangeCursor(presetNumber+change, 0, 99)
				}

				// NET: Signal options change
				if(netIsNetPlay&&netNumSpectators>0) netSendOptions(engine)
			}

			// Confirm
			if(engine.ctrl.isPush(Controller.BUTTON_A)&&menuTime>=5) {
				engine.playSE("decide")

				if(menuCursor==16) {
					// Load preset
					loadPreset(engine, owner.modeConfig, presetNumber)

					// NET: Signal options change
					if(netIsNetPlay&&netNumSpectators>0) netSendOptions(engine)
				} else if(menuCursor==17) {
					// Save preset
					savePreset(engine, owner.modeConfig, presetNumber)
					owner.saveModeConfig()
				} else {
					// Save settings
					owner.modeConfig.setProperty("scorerace.presetNumber", presetNumber)
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
			if(engine.ctrl.isPush(Controller.BUTTON_D)&&netIsNetPlay&&!netIsWatch&&!big
				&&engine.ai==null
			)
				netEnterNetPlayRankingScreen(engine, playerID, goaltype)

			menuTime++
		} else {
			menuTime++
			menuCursor = 0

			if(menuTime>=60) menuCursor = 10
			return menuTime<120
		}// Replay

		return true
	}

	/* Render settings screen */
	override fun renderSetting(engine:GameEngine, playerID:Int) {
		if(netIsNetRankingDisplayMode)
		// NET: Netplay Ranking
			netOnRenderNetPlayRanking(engine, playerID, receiver)
		else {
			drawMenuSpeeds(engine, playerID, receiver, 0, EventReceiver.COLOR.BLUE, 0)
			drawMenuBGM(engine, playerID, receiver, bgmno)
			drawMenuCompact(
				engine, playerID, receiver, "BIG" to big,
				"GOAL" to String.format("%3dK", GOAL_TABLE[goaltype]/1000)
			)

			drawMenu(
				engine, playerID, receiver,
				"SPIN BONUS" to if(twistEnableType==0) "OFF" else if(twistEnableType==1) "T-ONLY" else "ALL",
				"EZ SPIN" to enableTwistKick, "EZIMMOBILE" to twistEnableEZ
			)
			drawMenuCompact(
				engine, playerID, receiver,
				"B2B" to enableB2B, "COMBO" to enableCombo
			)
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
		engine.b2bEnable = enableB2B
		engine.splitb2b = enableSplitB2B
		engine.comboType = if(enableCombo) GameEngine.COMBO_TYPE_NORMAL
		else GameEngine.COMBO_TYPE_DISABLE

		owner.bgmStatus.bgm = if(netIsWatch) BGM.Silent
		else BGM.values[bgmno]

		if(version>=1) {
			engine.twistAllowKick = enableTwistKick
			when(twistEnableType) {
				0 -> engine.twistEnable = false
				1 -> engine.twistEnable = true
				else -> {
					engine.twistEnable = true
					engine.useAllSpinBonus = true
				}
			}
		} else
			engine.twistEnable = enableTwist

		engine.twistEnableEZ = twistEnableEZ
	}

	/* Score display */
	override fun renderLast(engine:GameEngine, playerID:Int) {
		if(owner.menuOnly) return

		receiver.drawScoreFont(engine, playerID, 0, 0, "SCORE RACE", EventReceiver.COLOR.RED)
		receiver.drawScoreFont(engine, playerID, 0, 1, "(${GOAL_TABLE[goaltype]} points run)", EventReceiver.COLOR.RED)

		if(engine.stat==GameEngine.Status.SETTING||engine.stat==GameEngine.Status.RESULT&&!owner.replayMode) {
			if(!owner.replayMode&&!big&&engine.ai==null&&!netIsWatch) {
				val scale = if(receiver.nextDisplayType==2) .5f else 1f
				val topY = if(receiver.nextDisplayType==2) 6 else 4
				receiver.drawScoreFont(engine, playerID, 3, topY-1, "TIME   LINE SCR/LINE", EventReceiver.COLOR.BLUE, scale)

				for(i in 0 until RANKING_MAX) {
					receiver.drawScoreGrade(
						engine, playerID, 0, topY+i, String.format("%2d", i+1), EventReceiver.COLOR.YELLOW,
						scale
					)
					receiver.drawScoreNum(
						engine, playerID, 3,
						topY+i, rankingTime[goaltype][i].toTimeStr, rankingRank==i, scale
					)
					receiver.drawScoreNum(
						engine, playerID, 12, topY+i,
						"${rankingLines[goaltype][i]}", rankingRank==i, scale
					)
					receiver.drawScoreNum(
						engine, playerID, 17, topY+i,
						String.format("%.6g", rankingSPL[goaltype][i]), rankingRank==i, scale
					)
				}
			}
		} else {
			receiver.drawScoreFont(engine, playerID, 0, 3, "Score", EventReceiver.COLOR.BLUE)
			var sc = GOAL_TABLE[goaltype]-engine.statistics.score
			if(sc<0) sc = 0
			var fontcolor = EventReceiver.COLOR.WHITE
			if(sc in 1..9600) fontcolor = EventReceiver.COLOR.YELLOW
			if(sc in 1..4800) fontcolor = EventReceiver.COLOR.ORANGE
			if(sc in 1..2400) fontcolor = EventReceiver.COLOR.RED
			receiver.drawScoreNum(engine, playerID, 5, 6, "+$lastscore")
			receiver.drawScoreNum(engine, playerID, 0, 4, "$scDisp", fontcolor, 2f)

			receiver.drawScoreFont(engine, playerID, 0, 6, "LINE", EventReceiver.COLOR.BLUE)
			receiver.drawScoreNum(engine, playerID, 0, 7, engine.statistics.lines.toString(), 2f)

			receiver.drawScoreFont(engine, playerID, 0, 9, "SCORE/MIN", EventReceiver.COLOR.BLUE)
			receiver.drawScoreNum(engine, playerID, 0, 10, String.format("%10f", engine.statistics.spm), 2f)

			receiver.drawScoreFont(engine, playerID, 0, 12, "LINE/MIN", EventReceiver.COLOR.BLUE)
			receiver.drawScoreNum(engine, playerID, 0, 13, "${engine.statistics.lpm}", 2f)

			receiver.drawScoreFont(engine, playerID, 0, 15, "SCORE/LINE", EventReceiver.COLOR.BLUE)
			receiver.drawScoreNum(engine, playerID, 0, 16, String.format("%10f", engine.statistics.spl), 2f)

			receiver.drawScoreFont(engine, playerID, 0, 18, "Time", EventReceiver.COLOR.BLUE)
			receiver.drawScoreNum(engine, playerID, 0, 19, engine.statistics.time.toTimeStr, 2f)

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
			val get = calcScoreCombo(pts, cmb, 0, spd)
			if(pts>0) lastscore = get
			if(lines>=1) engine.statistics.scoreLine += get
			else engine.statistics.scoreBonus += get
			scDisp += spd
		}
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

	/* Called after every frame */
	override fun onLast(engine:GameEngine, playerID:Int) {
		super.onLast(engine, playerID)
		// Update meter
		var remainScore = GOAL_TABLE[goaltype]-engine.statistics.score
		if(!engine.timerActive) remainScore = 0
		engine.meterValue = remainScore*receiver.getMeterMax(engine)/GOAL_TABLE[goaltype]
		engine.meterColor = GameEngine.METER_COLOR_GREEN
		if(remainScore<=9600) engine.meterColor = GameEngine.METER_COLOR_YELLOW
		if(remainScore<=4800) engine.meterColor = GameEngine.METER_COLOR_ORANGE
		if(remainScore<=2400) engine.meterColor = GameEngine.METER_COLOR_RED

		// Goal reached
		if(engine.statistics.score>=GOAL_TABLE[goaltype]&&engine.timerActive) {
			engine.gameEnded()
			engine.resetStatc()
			engine.stat = GameEngine.Status.ENDINGSTART
		}

		// BGM fadeout
		if(remainScore<=1000&&engine.timerActive) owner.bgmStatus.fadesw = true

	}

	/* Render results screen */
	override fun renderResult(engine:GameEngine, playerID:Int) {
		receiver.drawMenuFont(engine, playerID, 0, 0, "\u0090\u0093 PAGE${(engine.statc[1]+1)}/2", EventReceiver.COLOR.RED)

		if(engine.statc[1]==0) {
			drawResultStats(
				engine, playerID, receiver, 2, EventReceiver.COLOR.BLUE, Statistic.SCORE, Statistic.LINES,
				Statistic.TIME, Statistic.PIECE
			)
			drawResultRank(engine, playerID, receiver, 10, EventReceiver.COLOR.BLUE, rankingRank)
			drawResultNetRank(engine, playerID, receiver, 12, EventReceiver.COLOR.BLUE, netRankingRank[0])
			drawResultNetRankDaily(engine, playerID, receiver, 14, EventReceiver.COLOR.BLUE, netRankingRank[1])
		} else if(engine.statc[1]==1)
			drawResultStats(
				engine, playerID, receiver, 2, EventReceiver.COLOR.BLUE, Statistic.SPL, Statistic.SPM, Statistic.LPM,
				Statistic.PPS
			)

		if(netIsPB) receiver.drawMenuFont(engine, playerID, 2, 21, "NEW PB", EventReceiver.COLOR.ORANGE)

		if(netIsNetPlay&&netReplaySendStatus==1)
			receiver.drawMenuFont(engine, playerID, 0, 22, "SENDING...", EventReceiver.COLOR.PINK)
		else if(netIsNetPlay&&!netIsWatch
			&&netReplaySendStatus==2
		)
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

	/* Save replay */
	override fun saveReplay(engine:GameEngine, playerID:Int, prop:CustomProperties):Boolean {
		savePreset(engine, engine.owner.replayProp, -1)
		engine.owner.replayProp.setProperty("scorerace.version", version)

		// NET: Save name
		if(netPlayerName!=null&&netPlayerName!!.isNotEmpty()) prop.setProperty("$playerID.net.netPlayerName", netPlayerName)

		// Update rankings
		if(!owner.replayMode&&engine.statistics.score>=GOAL_TABLE[goaltype]&&!big&&engine.ai==null) {
			updateRanking(engine.statistics.time, engine.statistics.lines, engine.statistics.spl)

			if(rankingRank!=-1) return true
		}

		return false
	}

	override fun loadRanking(prop:CustomProperties, ruleName:String) {
		for(i in 0 until GOALTYPE_MAX)
			for(j in 0 until RANKING_MAX) {
				rankingTime[i][j] = prop.getProperty("$ruleName.$i.time.$j", -1)
				rankingLines[i][j] = prop.getProperty("$ruleName.$i.lines.$j", 0)

				if(rankingLines[i][j]>0) {
					val defaultSPL = GOAL_TABLE[i].toDouble()/rankingLines[i][j].toDouble()
					rankingSPL[i][j] = prop.getProperty("$ruleName.$i.spl.$j", defaultSPL)
				} else
					rankingSPL[i][j] = 0.0
			}
	}

	/** Save rankings of [ruleName] to [prop] */
	private fun saveRanking(ruleName:String) {
		super.saveRanking((0 until GOALTYPE_MAX).flatMap {j ->
			(0 until RANKING_MAX).flatMap {i ->
				listOf(
					"$ruleName.$j.time.$i" to rankingTime[j][i],
					"$ruleName.$j.lines.$i" to rankingLines[j][i],
					"$ruleName.$j.spl.$i" to rankingSPL[j][i]
				)
			}
		})
	}

	/** Update rankings
	 * @param time Time
	 * @param lines Lines
	 * @param spl Score/Line
	 */
	private fun updateRanking(time:Int, lines:Int, spl:Double) {
		rankingRank = checkRanking(time, lines, spl)

		if(rankingRank!=-1) {
			// Shift down ranking entries
			for(i in RANKING_MAX-1 downTo rankingRank+1) {
				rankingTime[goaltype][i] = rankingTime[goaltype][i-1]
				rankingLines[goaltype][i] = rankingLines[goaltype][i-1]
				rankingSPL[goaltype][i] = rankingSPL[goaltype][i-1]
			}

			// Add new data
			rankingTime[goaltype][rankingRank] = time
			rankingLines[goaltype][rankingRank] = lines
			rankingSPL[goaltype][rankingRank] = spl
		}
	}

	/** Calculate ranking position
	 * @param time Time
	 * @param lines Lines
	 * @param spl Score/Line
	 * @return Position (-1 if unranked)
	 */
	private fun checkRanking(time:Int, lines:Int, spl:Double):Int {
		for(i in 0 until RANKING_MAX)
			if(time<rankingTime[goaltype][i]||rankingTime[goaltype][i]<0)
				return i
			else if(time==rankingTime[goaltype][i]&&(lines<rankingLines[goaltype][i]||rankingLines[goaltype][i]==0))
				return i
			else if(time==rankingTime[goaltype][i]&&lines==rankingLines[goaltype][i]
				&&spl>rankingSPL[goaltype][i]
			)
				return i

		return -1
	}

	/** NET: Send various in-game stats of [engine] */
	override fun netSendStats(engine:GameEngine) {
		var msg = "game\tstats\t"
		msg += "${engine.statistics.scoreLine}\t${engine.statistics.scoreBonus}\t${engine.statistics.lines}\t"
		msg += "${engine.statistics.totalPieceLocked}\t${engine.statistics.time}\t"
		msg += "${engine.statistics.lpm}\t${engine.statistics.spl}\t$goaltype\t"
		msg += "${engine.gameActive}\t${engine.timerActive}\t"
		msg += "$lastscore\t$scDisp\t$lastb2b\t$lastcombo\t$lastpiece\n"
		netLobby!!.netPlayerClient!!.send(msg)
	}

	/** NET: Parse Received [message] as in-game stats of [engine] */
	override fun netRecvStats(engine:GameEngine, message:Array<String>) {
		engine.statistics.scoreLine = message[4].toInt()
		engine.statistics.scoreHD = message[5].toInt()
		engine.statistics.scoreSD = message[6].toInt()
		engine.statistics.scoreBonus = message[7].toInt()
		engine.statistics.lines = message[8].toInt()
		engine.statistics.totalPieceLocked = message[9].toInt()
		engine.statistics.time = message[10].toInt()
		goaltype = message[11].toInt()
		engine.gameActive = message[12].toBoolean()
		engine.timerActive = message[13].toBoolean()
		lastscore = message[14].toInt()
//		scDisp = message[15].toInt()
		lastb2b = message[16].toBoolean()
		lastcombo = message[17].toInt()
		lastpiece = message[18].toInt()
	}

	/** NET: Send end-of-game stats
	 * @param engine GameEngine
	 */
	override fun netSendEndGameStats(engine:GameEngine) {
		var subMsg = ""
		subMsg += "SCORE;${engine.statistics.score}/${GOAL_TABLE[goaltype]}\t"
		subMsg += "LINE;${engine.statistics.lines}\t"
		subMsg += "TIME;${engine.statistics.time.toTimeStr}\t"
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
		msg += "${engine.speed.das}\t$bgmno\t$big\t$goaltype\t$twistEnableType\t"
		msg += "$enableTwistKick${"\t$enableB2B\t"+enableCombo}\t$presetNumber\t\t$twistEnableEZ\n"
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
		bgmno = message[11].toInt()
		big = message[12].toBoolean()
		goaltype = message[13].toInt()
		twistEnableType = message[14].toInt()
		enableTwistKick = message[15].toBoolean()
		enableB2B = message[16].toBoolean()
		enableCombo = message[17].toBoolean()
		presetNumber = message[18].toInt()
		twistEnableEZ = message[20].toBoolean()
	}

	/** NET: Get goal type */
	override fun netGetGoalType():Int = goaltype

	/** NET: It returns true when the current settings doesn't prevent
	 * leaderboard screen from showing. */
	override fun netIsNetRankingViewOK(engine:GameEngine):Boolean = !big&&engine.ai==null

	/** NET: It returns true when the current settings doesn't prevent replay
	 * data from sending. */
	override fun netIsNetRankingSendOK(engine:GameEngine):Boolean =
		netIsNetRankingViewOK(engine)&&engine.statistics.score>=GOAL_TABLE[goaltype]

	companion object {
		/* ----- Main constants ----- */
		/** Current version */
		private const val CURRENT_VERSION = 1

		/** Number of entries in rankings */
		private const val RANKING_MAX = 13

		/** Goal score type */
		private const val GOALTYPE_MAX = 3

		/** Goal score constants */
		private val GOAL_TABLE = intArrayOf(10000, 25000, 30000)

		/* ----- Main variables ----- */
		/** Log */
		internal var log = Logger.getLogger(SprintScore::class.java)
	}
}
