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
import mu.nu.nullpo.game.event.ScoreEvent
import mu.nu.nullpo.game.net.NetUtil
import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.game.subsystem.mode.menu.BooleanMenuItem
import mu.nu.nullpo.game.subsystem.mode.menu.DelegateMenuItem
import mu.nu.nullpo.util.CustomProperties
import mu.nu.nullpo.util.GeneralUtil.toTimeStr
import org.apache.logging.log4j.LogManager

/** LINE RACE Mode */
class SprintLine:NetDummyMode() {

	/** BGM number */
	private var bgmno = 0

	private val itemBig = BooleanMenuItem("big", "BIG", EventReceiver.COLOR.BLUE, false)
	/** BigMode */
	private var big:Boolean by DelegateMenuItem(itemBig)

	/** Target count type (Lines: 0=20,1=40,2=100 / TSDs: 0=10,1=20,2=100) */
	private var goalType = 0
	/** Only-Twist mode (0:OFF,1:Twist w/Single,2:TSD or not count,3:TSD ONLY or die) */
	private var gamemode = 0

	/** Last preset number used */
	private var presetNumber = 0

	/** Current round's ranking position */
	private var rankingRank = 0

	/** Rankings' times */
	private val rankingTime = List(GOALTYPE_MAX) {MutableList(RANKING_MAX) {0}}

	/** Rankings' piece counts */
	private val rankingPiece = List(GOALTYPE_MAX) {MutableList(RANKING_MAX) {0}}

	/** Rankings' PPS values */
	private val rankingPPS
		get() = List(GOALTYPE_MAX) {x ->
			List(RANKING_MAX) {y -> rankingPiece[x][y]*60f/rankingTime[x][y]}
		}

	/* Mode name */
	override val name = "Lines SprintRace"
	override val gameIntensity = 2
	/* Initialization for each player */
	override val rankMap
		get() = rankMapOf(rankingTime.mapIndexed {i, a -> "$i.time" to a}+
			rankingPiece.mapIndexed {i, a -> "$i.piece" to a})

	override fun playerInit(engine:GameEngine) {
		log.debug("playerInit")

		super.playerInit(engine)

		bgmno = 0
		big = false
		goalType = 0
		presetNumber = 0

		rankingRank = -1
		rankingTime.forEach {it.fill(0)}
		rankingPiece.forEach {it.fill(0)}
//		rankingPPS = Array(GOALTYPE_MAX) {FloatArray(RANKING_MAX)}

		engine.frameColor = GameEngine.FRAME_COLOR_RED

		netPlayerInit(engine)

		if(!engine.owner.replayMode) {
			presetNumber = engine.owner.modeConfig.getProperty("linerace.presetNumber", 0)
			loadPreset(engine, engine.owner.modeConfig, -1)
		} else {
			presetNumber = 0
			loadPreset(engine, engine.owner.replayProp, -1)

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
		engine.speed.gravity = prop.getProperty("linerace.gravity.$preset", 1)
		engine.speed.denominator = prop.getProperty("linerace.denominator.$preset", 256)
		engine.speed.are = prop.getProperty("linerace.are.$preset", 0)
		engine.speed.areLine = prop.getProperty("linerace.areLine.$preset", 0)
		engine.speed.lineDelay = prop.getProperty("linerace. lineDelay.$preset", 0)
		engine.speed.lockDelay = prop.getProperty("linerace.lockDelay.$preset", 30)
		engine.speed.das = prop.getProperty("linerace.das.$preset", 10)
		bgmno = prop.getProperty("linerace.bgmno.$preset", BGM.values.indexOf(BGM.Rush(0)))
		big = prop.getProperty("linerace.big.$preset", false)
		goalType = prop.getProperty("linerace.goalType.$preset", 1)
	}

	/** Save options to a preset
	 * @param engine GameEngine
	 * @param prop Property file to save to
	 * @param preset Preset number
	 */
	private fun savePreset(engine:GameEngine, prop:CustomProperties, preset:Int) {
		prop.setProperty("linerace.gravity.$preset", engine.speed.gravity)
		prop.setProperty("linerace.denominator.$preset", engine.speed.denominator)
		prop.setProperty("linerace.are.$preset", engine.speed.are)
		prop.setProperty("linerace.areLine.$preset", engine.speed.areLine)
		prop.setProperty("linerace.lineDelay.$preset", engine.speed.lineDelay)
		prop.setProperty("linerace.lockDelay.$preset", engine.speed.lockDelay)
		prop.setProperty("linerace.das.$preset", engine.speed.das)
		prop.setProperty("linerace.bgmno.$preset", bgmno)
		prop.setProperty("linerace.big.$preset", big)
		prop.setProperty("linerace.goalType.$preset", goalType)
	}

	/* Called at settings screen */
	override fun onSetting(engine:GameEngine):Boolean {
		// NET: Net Ranking
		if(netIsNetRankingDisplayMode)
			netOnUpdateNetPlayRanking(engine, goalType)
		else if(!engine.owner.replayMode) {
			// Configuration changes
			val change = updateCursor(engine, 11)

			if(change!=0) {
				engine.playSE("change")

				var m = 1
				if(engine.ctrl.isPress(Controller.BUTTON_C)) m = 100
				if(engine.ctrl.isPress(Controller.BUTTON_D)) m = 1000

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
						if(goalType<0) goalType = 2
						if(goalType>2) goalType = 0
					}
					10, 11 -> presetNumber = rangeCursor(presetNumber+change, 0, 99)
				}

				// NET: Signal options change
				if(netIsNetPlay&&netNumSpectators>0) netSendOptions(engine)
			}

			// Confirm
			if(menuTime<5) menuTime++ else if(engine.ctrl.isPush(Controller.BUTTON_A)&&!netIsWatch) {
				engine.playSE("decide")

				if(menuCursor==10) {
					// Load preset
					loadPreset(engine, owner.modeConfig, presetNumber)

					// NET: Signal options change
					if(netIsNetPlay&&netNumSpectators>0) netSendOptions(engine)
				} else if(menuCursor==11) {
					// Save preset
					savePreset(engine, owner.modeConfig, presetNumber)
					owner.saveModeConfig()
				} else {
					// Save settings
					owner.modeConfig.setProperty("linerace.presetNumber", presetNumber)
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
			if(engine.ctrl.isPush(Controller.BUTTON_D)&&netIsNetPlay&&!big&&engine.ai==null)
				netEnterNetPlayRankingScreen(goalType)

		} else {
			menuTime++
			menuCursor = -1

			return menuTime<60
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
			drawMenuCompact(engine, receiver, "BIG" to big, "GOAL" to GOAL_TABLE[goalType])
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

		owner.musMan.bgm = if(netIsWatch) BGM.Silent else BGM.values[bgmno]

		engine.meterColor = GameEngine.METER_COLOR_GREEN
		engine.meterValue = 1f
	}

	/* Score display */
	override fun renderLast(engine:GameEngine) {
		if(owner.menuOnly) return

		receiver.drawScoreFont(engine, 0, 0, "LINE RACE", EventReceiver.COLOR.RED)
		receiver.drawScoreFont(engine, 0, 1, "(${GOAL_TABLE[goalType]} Lines run)", EventReceiver.COLOR.RED)

		if(engine.stat==GameEngine.Status.SETTING||engine.stat==GameEngine.Status.RESULT&&!owner.replayMode) {
			if(!owner.replayMode&&!big&&engine.ai==null&&!netIsWatch) {
				val scale = if(receiver.nextDisplayType==2) .5f else 1f
				val topY = if(receiver.nextDisplayType==2) 6 else 4
				receiver.drawScoreFont(engine, 3, topY-1, "TIME     PIECE P/SEC", EventReceiver.COLOR.BLUE, scale)

				for(i in 0 until RANKING_MAX) {
					receiver.drawScoreGrade(engine, 0, topY+i, String.format("%2d", i+1), EventReceiver.COLOR.YELLOW, scale)
					receiver.drawScoreNum(engine, 3, topY+i, rankingTime[goalType][i].toTimeStr, rankingRank==i, scale)
					receiver.drawScoreNum(engine, 12, topY+i, "${rankingPiece[goalType][i]}", rankingRank==i, scale)
					receiver.drawScoreNum(
						engine, 18, topY+i, String.format("%.5g", rankingPPS[goalType][i]), rankingRank==i,
						scale
					)
				}
			}
		} else {
			receiver.drawMenuNano(engine, 6, 21, "LINES\nTO GO", EventReceiver.COLOR.BLUE)
			val remainLines = maxOf(0, GOAL_TABLE[goalType]-engine.statistics.lines)
			val fontcolor = when(remainLines) {
				in 1..10 -> EventReceiver.COLOR.RED
				in 1..20 -> EventReceiver.COLOR.ORANGE
				in 1..30 -> EventReceiver.COLOR.YELLOW
				else -> EventReceiver.COLOR.WHITE
			}
			receiver.drawScoreNum(engine, 0, 4, "$remainLines", fontcolor)
			when("$remainLines".length) {
				1 -> receiver.drawMenuNum(engine, 4, 21, "$remainLines", fontcolor, 2f)
				2 -> receiver.drawMenuNum(engine, 3, 21, "$remainLines", fontcolor, 2f)
				3 -> receiver.drawMenuNum(engine, 2, 21, "$remainLines", fontcolor, 2f)
			}

			receiver.drawScoreNum(engine, 0, 6, "${engine.statistics.totalPieceLocked}", 2f)
			receiver.drawScoreFont(engine, 0, 8, "Piece\n/sec", EventReceiver.COLOR.BLUE)
			receiver.drawScoreNum(engine, 5, 9, "${engine.statistics.pps}")

			receiver.drawScoreNum(engine, 0, 11, "${engine.statistics.lpm}")
			receiver.drawScoreFont(engine, 0, 12, "Lines/MIN", EventReceiver.COLOR.BLUE)

			receiver.drawScoreFont(engine, 0, 15, "Time", EventReceiver.COLOR.BLUE)
			receiver.drawScoreNum(engine, 0, 16, engine.statistics.time.toTimeStr, 2f)
		}

		super.renderLast(engine)
	}

	/* Calculate score */
	override fun calcScore(engine:GameEngine, ev:ScoreEvent):Int {
		val li = ev.lines
		if(gamemode==3&&!(engine.twist&&li>=2&&!engine.twistMini)) {
			engine.resetStatc()
			engine.stat = GameEngine.Status.GAMEOVER
			return 0
		}
		val maxLines = GOAL_TABLE[goalType]
		val remainLines = maxLines-(if(gamemode<=1) engine.statistics.lines else
			engine.statistics.totalTwistDouble+engine.statistics.totalTwistTriple)

		engine.meterValue = remainLines*1f/GOAL_TABLE[goalType]
		engine.meterColor = GameEngine.METER_COLOR_LIMIT

		// Game completed
		if(remainLines<=0) {
			engine.ending = 1
			engine.gameEnded()
		} else if(engine.statistics.lines>=GOAL_TABLE[goalType]-5) owner.musMan.fadesw = true
		return li
	}

	/* Render results screen */
	override fun renderResult(engine:GameEngine) {
		drawResultStats(
			engine, receiver, 1, EventReceiver.COLOR.BLUE, Statistic.LINES, Statistic.PIECE, Statistic.TIME, Statistic.LPM,
			Statistic.PPS
		)
		drawResultRank(engine, receiver, 11, EventReceiver.COLOR.BLUE, rankingRank)
		drawResultNetRank(engine, receiver, 13, EventReceiver.COLOR.BLUE, netRankingRank[0])
		drawResultNetRankDaily(engine, receiver, 15, EventReceiver.COLOR.BLUE, netRankingRank[1])

		if(netIsPB) receiver.drawMenuFont(engine, 2, 18, "NEW PB", EventReceiver.COLOR.ORANGE)

		if(netIsNetPlay&&netReplaySendStatus==1)
			receiver.drawMenuFont(engine, 0, 19, "SENDING...", EventReceiver.COLOR.PINK)
		else if(netIsNetPlay&&!netIsWatch&&netReplaySendStatus==2)
			receiver.drawMenuFont(engine, 1, 19, "A: RETRY", EventReceiver.COLOR.RED)
	}

	/* Save replay file */
	override fun saveReplay(engine:GameEngine, prop:CustomProperties):Boolean {
		savePreset(engine, engine.owner.replayProp, -1)

		// NET: Save name
		if(netPlayerName!=null&&netPlayerName!!.isNotEmpty()) prop.setProperty(
			"${engine.playerID}.net.netPlayerName",
			netPlayerName
		)

		// Update rankings
		if(!owner.replayMode&&engine.statistics.lines>=GOAL_TABLE[goalType]&&!big&&engine.ai==null&&!netIsWatch) {
			updateRanking(engine.statistics.time, engine.statistics.totalPieceLocked, engine.statistics.pps)

			if(rankingRank!=-1) return true
		}

		return false
	}

	/*override fun loadRanking(prop:CustomProperties, ruleName:String) {
		super.loadRanking(prop, ruleName)
		for(i in 0 until GOALTYPE_MAX)
			for(j in 0 until RANKING_MAX) rankingPPS[i][j] = rankingPiece[i][j]*60f/rankingTime[i][j]
	}*/

	/** Update rankings
	 * @param time Time
	 * @param piece Piece count
	 */
	private fun updateRanking(time:Int, piece:Int, pps:Float) {
		rankingRank = checkRanking(time, piece, pps)

		if(rankingRank!=-1) {
			// Shift down ranking entries
			for(i in RANKING_MAX-1 downTo rankingRank+1) {
				rankingTime[goalType][i] = rankingTime[goalType][i-1]
				rankingPiece[goalType][i] = rankingPiece[goalType][i-1]
//				rankingPPS[goalType][i] = rankingPPS[goalType][i-1]
			}

			// Add new data
			rankingTime[goalType][rankingRank] = time
			rankingPiece[goalType][rankingRank] = piece
//			rankingPPS[goalType][rankingRank] = pps
		}
	}

	/** Calculate ranking position
	 * @param time Time
	 * @param piece Piece count
	 * @return Position (-1 if unranked)
	 */
	private fun checkRanking(time:Int, piece:Int, pps:Float):Int {
		for(i in 0 until RANKING_MAX)
			if(time<rankingTime[goalType][i]||rankingTime[goalType][i]<=0) return i
			else if(time==rankingTime[goalType][i]&&(piece<rankingPiece[goalType][i]||rankingPiece[goalType][i]==0)) return i
			else if(time==rankingTime[goalType][i]&&piece==rankingPiece[goalType][i]&&pps>rankingPPS[goalType][i]) return i

		return -1
	}

	/** NET: Send various in-game stats of [engine] */
	override fun netSendStats(engine:GameEngine) {
		val msg = "game\tstats\t"+
			"${engine.statistics.lines}\t${engine.statistics.totalPieceLocked}\t"+
			"${engine.statistics.time}\t${engine.statistics.lpm}\t"+
			"${engine.statistics.pps}\t$goalType\t"+
			"${engine.gameActive}\t${engine.timerActive}"+
			"\n"
		netLobby!!.netPlayerClient!!.send(msg)
	}

	/** NET: Parse Received [message] as in-game stats of [engine] */
	override fun netRecvStats(engine:GameEngine, message:Array<String>) {
		engine.statistics.lines = message[4].toInt()
		engine.statistics.totalPieceLocked = message[5].toInt()
		engine.statistics.time = message[6].toInt()
		//engine.statistics.lpm = message[7].toFloat()
		//engine.statistics.pps = message[8].toFloat()
		goalType = message[9].toInt()
		engine.gameActive = message[10].toBoolean()
		engine.timerActive = message[11].toBoolean()

		// Update meter
		val remainLines = GOAL_TABLE[goalType]-engine.statistics.lines
		engine.meterValue = remainLines*1f/GOAL_TABLE[goalType]
		if(remainLines<=30) engine.meterColor = GameEngine.METER_COLOR_YELLOW
		if(remainLines<=20) engine.meterColor = GameEngine.METER_COLOR_ORANGE
		if(remainLines<=10) engine.meterColor = GameEngine.METER_COLOR_RED
	}

	/** NET: Send end-of-game stats
	 * @param engine GameEngine
	 */
	override fun netSendEndGameStats(engine:GameEngine) {
		val subMsg =
			"LINE;${engine.statistics.lines}/${GOAL_TABLE[goalType]}\t"+
				"PIECE;${engine.statistics.totalPieceLocked}\t"+
				"TIME;${engine.statistics.time.toTimeStr}\t"+
				"LINE/MIN;${engine.statistics.lpm}\t"+
				"PIECE/SEC;${engine.statistics.pps}\t"

		val msg = "gstat1p\t${NetUtil.urlEncode(subMsg)}\n"
		netLobby!!.netPlayerClient!!.send(msg)
	}

	/** NET: Send game options to all spectators
	 * @param engine GameEngine
	 */
	override fun netSendOptions(engine:GameEngine) {
		val msg = "game\toption\t"+
			"${engine.speed.gravity}\t${engine.speed.denominator}\t${engine.speed.are}\t"+
			"${engine.speed.areLine}\t${engine.speed.lineDelay}\t${engine.speed.lockDelay}\t"+
			"${engine.speed.das}\t$bgmno\t$big\t$goalType\t$presetNumber\n"
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
		goalType = message[13].toInt()
		presetNumber = message[14].toInt()
	}

	/** NET: Get goal type */
	override fun netGetGoalType():Int = goalType

	/** NET: It returns true when the current settings don't prevent
	 * leaderboard screen from showing. */
	override fun netIsNetRankingViewOK(engine:GameEngine):Boolean = !big&&engine.ai==null

	/** NET: It returns true when the current settings don't prevent replay
	 * data from sending. */
	override fun netIsNetRankingSendOK(engine:GameEngine):Boolean =
		netIsNetRankingViewOK(engine)&&engine.statistics.lines>=GOAL_TABLE[goalType]

	companion object {
		/* ----- Main variables ----- */
		/** Logger */
		internal val log = LogManager.getLogger()

		/** Number of entries in rankings */
		private const val RANKING_MAX = 13

		/** Target line count type */
		private const val GOALTYPE_MAX = 3

		/** Target line count constants */
		private val GOAL_TABLE = intArrayOf(20, 40, 100)
	}
}
