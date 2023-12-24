/*
 * Copyright (c) 2010-2024, NullNoname
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
import mu.nu.nullpo.game.event.EventReceiver
import mu.nu.nullpo.game.event.EventReceiver.COLOR
import mu.nu.nullpo.game.event.ScoreEvent
import mu.nu.nullpo.game.net.NetUtil
import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.game.subsystem.mode.menu.BGMMenuItem
import mu.nu.nullpo.game.subsystem.mode.menu.BooleanMenuItem
import mu.nu.nullpo.game.subsystem.mode.menu.DelegateMenuItem
import mu.nu.nullpo.game.subsystem.mode.menu.MenuList
import mu.nu.nullpo.game.subsystem.mode.menu.SpeedPresets
import mu.nu.nullpo.game.subsystem.mode.menu.StringsMenuItem
import mu.nu.nullpo.util.CustomProperties
import mu.nu.nullpo.util.GeneralUtil.toTimeStr
import org.apache.logging.log4j.LogManager

/** LINE RACE Mode */
class SprintLine:NetDummyMode() {
	private val itemSpd = object:SpeedPresets(COLOR.BLUE, 0) {
		override fun presetLoad(engine:GameEngine, prop:CustomProperties, ruleName:String, setId:Int) {
			super.presetLoad(engine, prop, ruleName, setId)
			bgmId = prop.getProperty("$ruleName.bgmno.$setId", BGM.values.indexOf(BGM.Rush(3)))
			big = prop.getProperty("$ruleName.big.$setId", false)
			goalType = prop.getProperty("$ruleName.goalType.$setId", 1)
		}

		override fun presetSave(engine:GameEngine, prop:CustomProperties, ruleName:String, setId:Int) {
			super.presetSave(engine, prop, ruleName, setId)
			prop.setProperty("$ruleName.bgmno.$setId", bgmId)
			prop.setProperty("$ruleName.big.$setId", big)
			prop.setProperty("$ruleName.goalType.$setId", goalType)
		}
	}
	/** Last preset number used */
	private var presetNumber:Int by DelegateMenuItem(itemSpd)

	private val itemBGM = BGMMenuItem("bgmno", COLOR.BLUE, BGM.values.indexOf(BGM.Rush(3)))
	/** BGM number */
	private var bgmId:Int by DelegateMenuItem(itemBGM)

	private val itemBig = BooleanMenuItem("big", "BIG", COLOR.BLUE, false)
	/** BigMode */
	private var big:Boolean by DelegateMenuItem(itemBig)

	/** Target count type (Lines: 0=20,1=40,2=100 / TSDs: 0=10,1=20,2=50) */
	private val itemGoal = object:StringsMenuItem(
		"goalType", "GOAL", COLOR.BLUE, 0, GOAL_TABLE.map {"$it LINES"}
	) {
		override fun draw(engine:GameEngine, playerID:Int, receiver:EventReceiver, y:Int, focus:Int) {
			super.draw(engine, playerID, receiver, y, focus)
			if(gameMode>=2) receiver.drawMenuNano(
				engine, 1, y+1, "${GOAL_TABLE[value]/2} TSDs",
				if(focus==0) COLOR.RAINBOW else COLOR.WHITE
			)
		}
	}
	/** Time limit type */
	private var goalType:Int by DelegateMenuItem(itemGoal)
	/** Only-Twist mode (0:OFF,1:Twist w/Single,2:TSD or not count,3:TSD ONLY or die) */
	private var gameMode = 0

	override val menu = MenuList("linerace", itemGoal, itemSpd, itemBGM, itemBig)

	/** Current round's ranking position */
	private var rankingRank = 0

	/** Rankings' times */
	private val rankingTime = List(GOALTYPE_MAX) {MutableList(RANKING_MAX) {1}}

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

		bgmId = 0
		big = false
		goalType = 0
		presetNumber = 0
		super.playerInit(engine)

		rankingRank = -1
		rankingTime.forEach {it.fill(-1)}
		rankingPiece.forEach {it.fill(0)}
//		rankingPPS = Array(GOALTYPE_MAX) {FloatArray(RANKING_MAX)}

		owner.bgMan.bg = -14
		engine.frameColor = GameEngine.FRAME_COLOR_RED

		netPlayerInit(engine)

		if(owner.replayMode) {
			// NET: Load name
			netPlayerName = owner.replayProp.getProperty("${engine.playerID}.net.netPlayerName", "")
		}
	}

	/* This function will be called before the game actually begins (after
	* Ready&Go screen disappears) */
	override fun startGame(engine:GameEngine) {
		engine.big = big
		owner.musMan.bgm = if(netIsWatch) BGM.Silent else BGM.values[bgmId]

		engine.meterColor = GameEngine.METER_COLOR_GREEN
		engine.meterValue = 1f
	}

	/* Score display */
	override fun renderLast(engine:GameEngine) {
		if(owner.menuOnly) return

		receiver.drawScoreFont(engine, 0, 0, name, COLOR.RED)
		receiver.drawScoreFont(engine, 0, 1, "(${GOAL_TABLE[goalType]} Lines run)", COLOR.RED)

		if(engine.stat==GameEngine.Status.SETTING||engine.stat==GameEngine.Status.RESULT&&!owner.replayMode) {
			if(!owner.replayMode&&!big&&engine.ai==null&&!netIsWatch) {
				val topY = if(receiver.nextDisplayType==2) 6 else 4
				receiver.drawScoreFont(engine, 1, topY-1, "TIME   PIECE/sec", COLOR.BLUE)

				for(i in 0..<RANKING_MAX) {
					receiver.drawScoreGrade(engine, 0, topY+i, "%2d".format(i+1), COLOR.YELLOW)
					receiver.drawScoreNum(engine, 2, topY+i, rankingTime[goalType][i].toTimeStr, rankingRank==i)
					if(rankingTime[goalType][i]>=0) {
						receiver.drawScoreNum(engine, 9, topY+i, "${rankingPiece[goalType][i]}", rankingRank==i)
						receiver.drawScoreNum(engine, 12, topY+i, rankingPPS[goalType][i], null to null, rankingRank==i)
					}
				}
			}
		} else {
			val remainLines = maxOf(0, GOAL_TABLE[goalType]-engine.statistics.lines)
			val fontColor = when(remainLines) {
				in 1..10 -> COLOR.RED
				in 10..20 -> COLOR.ORANGE
				in 20..30 -> COLOR.YELLOW
				else -> COLOR.WHITE
			}
			"$remainLines".let {
				receiver.drawMenuNum(engine, -30, 1, it, fontColor, 4f, .5f)
				receiver.drawScoreNum(engine, 0, 3, it, fontColor, 2f)
			}
			receiver.drawScoreNano(engine, 4, 3, "LINES\nTO GO", COLOR.BLUE)

			receiver.drawScoreNum(engine, 0, 5, "${engine.statistics.totalPieceLocked}", 2f)
			receiver.drawScoreFont(engine, 0, 7, "Piece\n/sec", COLOR.BLUE)
			receiver.drawScoreNum(engine, 5, 8, engine.statistics.pps, scale = 2f)
			receiver.drawScoreFont(engine, 6, 7, "Finesse", COLOR.BLUE)
			receiver.drawScoreNum(engine, 5, 5, "${engine.statistics.finesse}", 2f)
			receiver.drawScoreNum(engine, 0, 10, engine.statistics.lpm, scale = 2f)
			receiver.drawScoreFont(engine, 0, 12, "Lines/MIN", COLOR.BLUE)


			receiver.drawScoreFont(engine, 0, 15, "Time", COLOR.BLUE)
			receiver.drawScoreNum(engine, 0, 16, engine.statistics.time.toTimeStr, 2f)
		}

		super.renderLast(engine)
	}

	/* Calculate score */
	override fun calcScore(engine:GameEngine, ev:ScoreEvent):Int {
		val li = ev.lines
		if(gameMode==3&&!(engine.twist&&li>=2&&!engine.twistMini)) {
			engine.resetStatc()
			engine.stat = GameEngine.Status.GAMEOVER
			return 0
		}
		val maxLines = GOAL_TABLE[goalType]
		val remainLines = maxLines-(if(gameMode<=1) engine.statistics.lines else
			engine.statistics.totalTwistDouble+engine.statistics.totalTwistTriple)

		engine.meterValue = remainLines*1f/GOAL_TABLE[goalType]
		engine.meterColor = GameEngine.METER_COLOR_LIMIT

		// Game completed
		if(remainLines<=0) {
			engine.ending = 1
			engine.gameEnded()
		} else if(engine.statistics.lines>=GOAL_TABLE[goalType]-5) owner.musMan.fadeSW = true
		return li
	}

	/* Render results screen */
	override fun renderResult(engine:GameEngine) {
		drawResultStats(
			engine, receiver, 1, COLOR.BLUE, Statistic.LINES, Statistic.PIECE, Statistic.TIME, Statistic.LPM,
			Statistic.PPS
		)
		drawResultRank(engine, receiver, 11, COLOR.BLUE, rankingRank)
		drawResultNetRank(engine, receiver, 13, COLOR.BLUE, netRankingRank[0])
		drawResultNetRankDaily(engine, receiver, 15, COLOR.BLUE, netRankingRank[1])

		if(netIsPB) receiver.drawMenuFont(engine, 2, 18, "NEW PB", COLOR.ORANGE)

		if(netIsNetPlay&&netReplaySendStatus==1)
			receiver.drawMenuFont(engine, 0, 19, "SENDING...", COLOR.PINK)
		else if(netIsNetPlay&&!netIsWatch&&netReplaySendStatus==2)
			receiver.drawMenuFont(engine, 1, 19, "A: RETRY", COLOR.RED)
	}

	/* Save replay file */
	override fun saveReplay(engine:GameEngine, prop:CustomProperties):Boolean {
		itemSpd.presetSave(engine, prop, menu.propName, -1)

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
		for(i in 0..<RANKING_MAX)
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
		netLobby?.netPlayerClient?.send(msg)
	}

	/** NET: Parse Received [message] as in-game stats of [engine] */
	override fun netRecvStats(engine:GameEngine, message:List<String>) {
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
		engine.meterColor = when {
			remainLines<=10 -> GameEngine.METER_COLOR_RED
			remainLines<=20 -> GameEngine.METER_COLOR_ORANGE
			remainLines<=30 -> GameEngine.METER_COLOR_YELLOW
			else -> GameEngine.METER_COLOR_GREEN
		}
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
		netLobby?.netPlayerClient?.send(msg)
	}

	/** NET: Send game options to all spectators
	 * @param engine GameEngine
	 */
	override fun netSendOptions(engine:GameEngine) {
		val msg = "game\toption\t"+
			"${engine.speed.gravity}\t${engine.speed.denominator}\t${engine.speed.are}\t"+
			"${engine.speed.areLine}\t${engine.speed.lineDelay}\t${engine.speed.lockDelay}\t"+
			"${engine.speed.das}\t$bgmId\t$big\t$goalType\t$presetNumber\n"
		netLobby?.netPlayerClient?.send(msg)
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
		bgmId = message[11].toInt()
		big = message[12].toBoolean()
		goalType = message[13].toInt()
		presetNumber = message[14].toInt()
	}

	/** NET: Get goal type */
	override val netGetGoalType get() = goalType

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
		private val GOAL_TABLE = listOf(20, 40, 100)
	}
}
