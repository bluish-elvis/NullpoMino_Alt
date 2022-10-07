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
import mu.nu.nullpo.game.event.EventReceiver
import mu.nu.nullpo.game.net.NetUtil
import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.game.subsystem.mode.menu.BooleanMenuItem
import mu.nu.nullpo.game.subsystem.mode.menu.DelegateMenuItem
import mu.nu.nullpo.gui.common.BaseFont
import mu.nu.nullpo.util.CustomProperties
import mu.nu.nullpo.util.GeneralUtil.toTimeStr
import org.apache.logging.log4j.LogManager

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
	private var goalType = 0

	/** Last preset number used */
	private var presetNumber = 0

	/** Version */
	private var version = 0

	/** Current round's ranking position */
	private var rankingRank = 0

	/** Rankings' times */
	private val rankingTime = List(GOALTYPE_MAX) {MutableList(RANKING_MAX) {0}}

	/** Rankings' line counts */
	private val rankingLines = List(GOALTYPE_MAX) {MutableList(RANKING_MAX) {0}}

	/** Rankings' score/line */
	private val rankingSPL = List(GOALTYPE_MAX) {MutableList(RANKING_MAX) {0.0}}
	override val rankMap
		get() = rankMapOf(rankingTime.flatMapIndexed {a, x -> x.mapIndexed {b, y -> "$a.$b.time" to y}}+
			rankingSPL.flatMapIndexed {a, x -> x.mapIndexed {b, y -> "$a.$b.spl" to y}}+
			rankingLines.flatMapIndexed {a, x -> x.mapIndexed {b, y -> "$a.$b.lines" to y}})

	/* Mode name */
	override val name = "Score SprintRace"
	override val gameIntensity = 2

	/* Initialization */
	override fun playerInit(engine:GameEngine) {
		super.playerInit(engine)
		lastscore = 0
		lastb2b = false
		lastcombo = 0
		lastpiece = 0
		bgmno = 0

		rankingRank = -1
		rankingTime.forEach {it.fill(0)}
		rankingLines.forEach {it.fill(0)}
		rankingSPL.forEach {it.fill(0.0)}

		engine.frameColor = GameEngine.FRAME_COLOR_BRONZE

		netPlayerInit(engine)

		if(!engine.owner.replayMode) {
			presetNumber = engine.owner.modeConfig.getProperty("scorerace.presetNumber", 0)
			loadPreset(engine, engine.owner.modeConfig, -1)

			version = CURRENT_VERSION
		} else {
			presetNumber = 0
			loadPreset(engine, engine.owner.replayProp, -1)
			version = engine.owner.replayProp.getProperty("scorerace.version", 0)
			// NET: Load name
			netPlayerName = engine.owner.replayProp.getProperty("${engine.playerID}.net.netPlayerName", "")
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
		goalType = prop.getProperty("scorerace.goalType.$preset", 1)
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
		prop.setProperty("scorerace.goalType.$preset", goalType)
	}

	/* Called at settings screen */
	override fun onSetting(engine:GameEngine):Boolean {
		// NET: Net Ranking
		if(netIsNetRankingDisplayMode)
			netOnUpdateNetPlayRanking(engine, goalType)
		else if(!engine.owner.replayMode) {
			// Configuration changes
			val change = updateCursor(engine, 16)

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
						goalType += change
						if(goalType<0) goalType = GOALTYPE_MAX-1
						if(goalType>GOALTYPE_MAX-1) goalType = 0
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
			if(menuTime<5) menuTime++ else if(engine.ctrl.isPush(Controller.BUTTON_A)) {
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
			if(engine.ctrl.isPush(Controller.BUTTON_B)&&!netIsNetPlay) engine.quitFlag = true

			// NET: Netplay Ranking
			if(engine.ctrl.isPush(Controller.BUTTON_D)&&netIsNetPlay&&!netIsWatch&&!big&&engine.ai==null)
				netEnterNetPlayRankingScreen(goalType)
		} else {
			menuTime++
			menuCursor = 0

			if(menuTime>=60) menuCursor = 10
			return menuTime<120
		}// Replay

		return true
	}

	/* Render settings screen */
	override fun renderSetting(engine:GameEngine) {
		if(netIsNetRankingDisplayMode)
		// NET: Netplay Ranking
			netOnRenderNetPlayRanking(engine, receiver)
		else {
			drawMenuSpeeds(engine, receiver, 0, EventReceiver.COLOR.BLUE, 0)
			drawMenuBGM(engine, receiver, bgmno)
			drawMenuCompact(
				engine, receiver, "BIG" to big, "GOAL" to String.format("%3dK", GOAL_TABLE[goalType]/1000)
			)

			drawMenu(
				engine, receiver,
				"SPIN BONUS" to if(twistEnableType==0) "OFF" else if(twistEnableType==1) "T-ONLY" else "ALL",
				"EZ SPIN" to enableTwistKick, "EZIMMOBILE" to twistEnableEZ
			)
			drawMenuCompact(
				engine, receiver, "B2B" to enableB2B,
				"COMBO" to enableCombo
			)
			if(!engine.owner.replayMode) {
				menuColor = EventReceiver.COLOR.GREEN
				drawMenuCompact(engine, receiver, "LOAD" to presetNumber, "SAVE" to presetNumber)
			}
		}
	}

	/* This function will be called before the game actually begins (after
 * Ready&Go screen disappears) */
	override fun startGame(engine:GameEngine) {
		engine.big = big
		engine.b2bEnable = enableB2B
		engine.splitB2B = enableSplitB2B
		engine.comboType = if(enableCombo) GameEngine.COMBO_TYPE_NORMAL
		else GameEngine.COMBO_TYPE_DISABLE

		owner.musMan.bgm = if(netIsWatch) BGM.Silent
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
	override fun renderLast(engine:GameEngine) {
		if(owner.menuOnly) return

		receiver.drawScoreFont(engine, 0, 0, "SCORE RACE", EventReceiver.COLOR.RED)
		receiver.drawScoreFont(engine, 0, 1, "(${GOAL_TABLE[goalType]} points run)", EventReceiver.COLOR.RED)

		if(engine.stat==GameEngine.Status.SETTING||engine.stat==GameEngine.Status.RESULT&&!owner.replayMode) {
			if(!owner.replayMode&&!big&&engine.ai==null&&!netIsWatch) {
				val scale = if(receiver.nextDisplayType==2) .5f else 1f
				val topY = if(receiver.nextDisplayType==2) 6 else 4
				receiver.drawScoreFont(engine, 3, topY-1, "TIME   LINE SCR/LINE", EventReceiver.COLOR.BLUE, scale)

				for(i in 0 until RANKING_MAX) {
					receiver.drawScoreGrade(
						engine, 0, topY+i, String.format("%2d", i+1), EventReceiver.COLOR.YELLOW, scale
					)
					receiver.drawScoreNum(
						engine, 3, topY+i,
						rankingTime[goalType][i].toTimeStr, rankingRank==i, scale
					)
					receiver.drawScoreNum(
						engine, 12, topY+i, "${rankingLines[goalType][i]}",
						rankingRank==i, scale
					)
					receiver.drawScoreNum(
						engine, 17, topY+i, String.format("%.6g", rankingSPL[goalType][i]),
						rankingRank==i, scale
					)
				}
			}
		} else {
			receiver.drawScoreFont(engine, 0, 3, "Score", EventReceiver.COLOR.BLUE)
			var sc = GOAL_TABLE[goalType]-engine.statistics.score
			if(sc<0) sc = 0
			var fontcolor = EventReceiver.COLOR.WHITE
			if(sc in 1..9600) fontcolor = EventReceiver.COLOR.YELLOW
			if(sc in 1..4800) fontcolor = EventReceiver.COLOR.ORANGE
			if(sc in 1..2400) fontcolor = EventReceiver.COLOR.RED
			receiver.drawScoreNum(engine, 5, 6, "+$lastscore")
			receiver.drawScoreNum(engine, 0, 4, "$scDisp", fontcolor, 2f)

			receiver.drawScoreFont(engine, 0, 6, "LINE", EventReceiver.COLOR.BLUE)
			receiver.drawScoreNum(engine, 0, 7, engine.statistics.lines.toString(), 2f)

			receiver.drawScoreFont(engine, 0, 9, "SCORE/MIN", EventReceiver.COLOR.BLUE)
			receiver.drawScoreNum(engine, 0, 10, String.format("%10f", engine.statistics.spm), 2f)

			receiver.drawScoreFont(engine, 0, 12, "LINE/MIN", EventReceiver.COLOR.BLUE)
			receiver.drawScoreNum(engine, 0, 13, "${engine.statistics.lpm}", 2f)

			receiver.drawScoreFont(engine, 0, 15, "SCORE/LINE", EventReceiver.COLOR.BLUE)
			receiver.drawScoreNum(engine, 0, 16, String.format("%10f", engine.statistics.spl), 2f)

			receiver.drawScoreFont(engine, 0, 18, "Time", EventReceiver.COLOR.BLUE)
			receiver.drawScoreNum(engine, 0, 19, engine.statistics.time.toTimeStr, 2f)
		}

		super.renderLast(engine)
	}

	/* Soft drop */
	override fun afterSoftDropFall(engine:GameEngine, fall:Int) {
		engine.statistics.scoreSD += fall
	}

	/* Hard drop */
	override fun afterHardDropFall(engine:GameEngine, fall:Int) {
		engine.statistics.scoreHD += fall*2
	}

	/* Called after every frame */
	override fun onLast(engine:GameEngine) {
		super.onLast(engine)
		// Update meter
		var remainScore = GOAL_TABLE[goalType]-engine.statistics.score
		if(!engine.timerActive) remainScore = 0
		engine.meterValue = remainScore*1f/GOAL_TABLE[goalType]
		engine.meterColor = GameEngine.METER_COLOR_GREEN
		if(remainScore<=9600) engine.meterColor = GameEngine.METER_COLOR_YELLOW
		if(remainScore<=4800) engine.meterColor = GameEngine.METER_COLOR_ORANGE
		if(remainScore<=2400) engine.meterColor = GameEngine.METER_COLOR_RED

		// Goal reached
		if(engine.statistics.score>=GOAL_TABLE[goalType]&&engine.timerActive) {
			engine.gameEnded()
			engine.resetStatc()
			engine.stat = GameEngine.Status.ENDINGSTART
		}

		// BGM fadeout
		if(remainScore<=1000&&engine.timerActive) owner.musMan.fadesw = true
	}

	/* Render results screen */
	override fun renderResult(engine:GameEngine) {
		receiver.drawMenuFont(
			engine, 0, 0, "${BaseFont.UP_L}${BaseFont.DOWN_L} PAGE${(engine.statc[1]+1)}/2",
			EventReceiver.COLOR.RED
		)

		when {
			engine.statc[1]==0 -> {
				drawResultStats(
					engine, receiver, 2, EventReceiver.COLOR.BLUE, Statistic.SCORE, Statistic.LINES, Statistic.TIME,
					Statistic.PIECE
				)
				drawResultRank(engine, receiver, 10, EventReceiver.COLOR.BLUE, rankingRank)
				drawResultNetRank(engine, receiver, 12, EventReceiver.COLOR.BLUE, netRankingRank[0])
				drawResultNetRankDaily(engine, receiver, 14, EventReceiver.COLOR.BLUE, netRankingRank[1])
			}
			engine.statc[1]==1 -> drawResultStats(
				engine, receiver, 2, EventReceiver.COLOR.BLUE, Statistic.SPL, Statistic.SPM, Statistic.LPM, Statistic.PPS
			)
		}

		if(netIsPB) receiver.drawMenuFont(engine, 2, 21, "NEW PB", EventReceiver.COLOR.ORANGE)

		if(netIsNetPlay&&netReplaySendStatus==1)
			receiver.drawMenuFont(engine, 0, 22, "SENDING...", EventReceiver.COLOR.PINK)
		else if(netIsNetPlay&&!netIsWatch&&netReplaySendStatus==2)
			receiver.drawMenuFont(engine, 1, 22, "A: RETRY", EventReceiver.COLOR.RED)
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

	/* Save replay */
	override fun saveReplay(engine:GameEngine, prop:CustomProperties):Boolean {
		savePreset(engine, engine.owner.replayProp, -1)
		engine.owner.replayProp.setProperty("scorerace.version", version)

		// NET: Save name
		if(netPlayerName!=null&&netPlayerName!!.isNotEmpty()) prop.setProperty(
			"${engine.playerID}.net.netPlayerName",
			netPlayerName
		)

		// Update rankings
		if(!owner.replayMode&&engine.statistics.score>=GOAL_TABLE[goalType]&&!big&&engine.ai==null) {
			updateRanking(engine.statistics.time, engine.statistics.lines, engine.statistics.spl)

			if(rankingRank!=-1) return true
		}

		return false
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
				rankingTime[goalType][i] = rankingTime[goalType][i-1]
				rankingLines[goalType][i] = rankingLines[goalType][i-1]
				rankingSPL[goalType][i] = rankingSPL[goalType][i-1]
			}

			// Add new data
			rankingTime[goalType][rankingRank] = time
			rankingLines[goalType][rankingRank] = lines
			rankingSPL[goalType][rankingRank] = spl
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
			if(time<rankingTime[goalType][i]||rankingTime[goalType][i]<0)
				return i
			else if(time==rankingTime[goalType][i]&&(lines<rankingLines[goalType][i]||rankingLines[goalType][i]==0))
				return i
			else if(time==rankingTime[goalType][i]&&lines==rankingLines[goalType][i]
				&&spl>rankingSPL[goalType][i]
			)
				return i

		return -1
	}

	/** NET: Send various in-game stats of [engine] */
	override fun netSendStats(engine:GameEngine) {
		var msg = "game\tstats\t"
		msg += "${engine.statistics.scoreLine}\t${engine.statistics.scoreBonus}\t${engine.statistics.lines}\t"
		msg += "${engine.statistics.totalPieceLocked}\t${engine.statistics.time}\t"
		msg += "${engine.statistics.lpm}\t${engine.statistics.spl}\t$goalType\t"
		msg += "${engine.gameActive}\t${engine.timerActive}\t"
		msg += "$lastscore\t$scDisp\t$lastb2b\t$lastcombo\t$lastpiece\n"
		netLobby!!.netPlayerClient!!.send(msg)
	}

	/** NET: Parse Received [message] as in-game stats of [engine] */
	override fun netRecvStats(engine:GameEngine, message:List<String>) {
		engine.statistics.scoreLine = message[4].toInt()
		engine.statistics.scoreHD = message[5].toInt()
		engine.statistics.scoreSD = message[6].toInt()
		engine.statistics.scoreBonus = message[7].toInt()
		engine.statistics.lines = message[8].toInt()
		engine.statistics.totalPieceLocked = message[9].toInt()
		engine.statistics.time = message[10].toInt()
		goalType = message[11].toInt()
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
		subMsg += "SCORE;${engine.statistics.score}/${GOAL_TABLE[goalType]}\t"
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
		msg += "${engine.speed.das}\t$bgmno\t$big\t$goalType\t$twistEnableType\t"
		msg += "$enableTwistKick${"\t$enableB2B\t"+enableCombo}\t$presetNumber\t\t$twistEnableEZ\n"
		netLobby!!.netPlayerClient!!.send(msg)
	}

	/** NET: Receive game options */
	override fun netRecvOptions(engine:GameEngine, message:List<String>) {
		engine.speed.gravity = message[4].toInt()
		engine.speed.denominator = message[5].toInt()
		engine.speed.are = message[6].toInt()
		engine.speed.areLine = message[7].toInt()
		engine.speed.lineDelay = message[8].toInt()
		engine.speed.lockDelay = message[9].toInt()
		engine.speed.das = message[10].toInt()
		bgmno = message[11].toInt()
		big = message[12].toBoolean()
		goalType = message[13].toInt()
		twistEnableType = message[14].toInt()
		enableTwistKick = message[15].toBoolean()
		enableB2B = message[16].toBoolean()
		enableCombo = message[17].toBoolean()
		presetNumber = message[18].toInt()
		twistEnableEZ = message[20].toBoolean()
	}

	/** NET: Get goal type */
	override fun netGetGoalType():Int = goalType

	/** NET: It returns true when the current settings don't prevent
	 * leaderboard screen from showing. */
	override fun netIsNetRankingViewOK(engine:GameEngine):Boolean = !big&&engine.ai==null

	/** NET: It returns true when the current settings don't prevent replay
	 * data from sending. */
	override fun netIsNetRankingSendOK(engine:GameEngine):Boolean =
		netIsNetRankingViewOK(engine)&&engine.statistics.score>=GOAL_TABLE[goalType]

	companion object {
		/* ----- Main constants ----- */
		/** Current version */
		private const val CURRENT_VERSION = 1

		/** Number of entries in rankings */
		private const val RANKING_MAX = 13

		/** Goal score type */
		private const val GOALTYPE_MAX = 3

		/** Goal score constants */
		private val GOAL_TABLE = listOf(10000, 25000, 30000)

		/* ----- Main variables ----- */
		/** Log */
		internal var log = LogManager.getLogger()
	}
}
