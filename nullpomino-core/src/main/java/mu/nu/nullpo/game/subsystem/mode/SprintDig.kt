/* Copyright (c) 2010, NullNoname
 * All rights reserved.
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * Neither the name of NullNoname nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
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
 * POSSIBILITY OF SUCH DAMAGE. */
package mu.nu.nullpo.game.subsystem.mode

import mu.nu.nullpo.game.component.BGMStatus.BGM
import mu.nu.nullpo.game.component.Block
import mu.nu.nullpo.game.component.Block.COLOR
import mu.nu.nullpo.game.component.Controller
import mu.nu.nullpo.game.event.EventReceiver
import mu.nu.nullpo.game.net.NetUtil
import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.util.CustomProperties
import mu.nu.nullpo.util.GeneralUtil
import org.apache.log4j.Logger

/** DIG RACE Mode */
class SprintDig:NetDummyMode() {

	/** BGM number */
	private var bgmno:Int = 0

	/** Big (leftover from old versions) */
	private var big:Boolean = false

	/** Goal type (0=5 garbages, 1=10 garbages, 2=18 garbages) */
	private var goaltype:Int = 0

	/** Current version */
	private var version:Int = 0

	/** Last preset number used */
	private var presetNumber:Int = 0

	/** Current round's ranking rank */
	private var rankingRank:Int = 0

	/** Rankings' times */
	private var rankingTime:Array<IntArray> = Array(GOALTYPE_MAX) {IntArray(RANKING_MAX)}

	/** Rankings' line counts */
	private var rankingLines:Array<IntArray> = Array(GOALTYPE_MAX) {IntArray(RANKING_MAX)}

	/** Rankings' piece counts */
	private var rankingPiece:Array<IntArray> = Array(GOALTYPE_MAX) {IntArray(RANKING_MAX)}

	/* Mode name */
	override val name:String
		get() = "DIG RACE"

	/* Initialization for each player */
	override fun playerInit(engine:GameEngine, playerID:Int) {
		super.playerInit(engine, playerID)

		bgmno = 0
		big = false
		goaltype = 0
		presetNumber = 0

		rankingRank = -1
		rankingTime = Array(GOALTYPE_MAX) {IntArray(RANKING_MAX)}
		rankingLines = Array(GOALTYPE_MAX) {IntArray(RANKING_MAX)}
		rankingPiece = Array(GOALTYPE_MAX) {IntArray(RANKING_MAX)}

		engine.framecolor = GameEngine.FRAME_COLOR_GREEN

		netPlayerInit(engine, playerID)

		if(!engine.owner.replayMode) {
			version = CURRENT_VERSION
			presetNumber = engine.owner.modeConfig.getProperty("digrace.presetNumber", 0)
			loadPreset(engine, engine.owner.modeConfig, -1)
			loadRanking(owner.recordProp, engine.ruleopt.strRuleName)
		} else {
			version = engine.owner.replayProp.getProperty("digrace.version", 0)
			presetNumber = 0
			loadPreset(engine, engine.owner.replayProp, -1)

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
		engine.speed.gravity = prop.getProperty("digrace.gravity.$preset", 4)
		engine.speed.denominator = prop.getProperty("digrace.denominator.$preset", 256)
		engine.speed.are = prop.getProperty("digrace.are.$preset", 10)
		engine.speed.areLine = prop.getProperty("digrace.areLine.$preset", 5)
		engine.speed.lineDelay = prop.getProperty("digrace. lineDelay.$preset", 20)
		engine.speed.lockDelay = prop.getProperty("digrace.lockDelay.$preset", 30)
		engine.speed.das = prop.getProperty("digrace.das.$preset", 14)
		bgmno = prop.getProperty("digrace.bgmno.$preset", 0)
		big = prop.getProperty("digrace.big.$preset", false)
		goaltype = prop.getProperty("digrace.goaltype.$preset", 1)
	}

	/** Save options to a preset
	 * @param engine GameEngine
	 * @param prop Property file to save to
	 * @param preset Preset number
	 */
	private fun savePreset(engine:GameEngine, prop:CustomProperties, preset:Int) {
		prop.setProperty("digrace.gravity.$preset", engine.speed.gravity)
		prop.setProperty("digrace.denominator.$preset", engine.speed.denominator)
		prop.setProperty("digrace.are.$preset", engine.speed.are)
		prop.setProperty("digrace.areLine.$preset", engine.speed.areLine)
		prop.setProperty("digrace.lineDelay.$preset", engine.speed.lineDelay)
		prop.setProperty("digrace.lockDelay.$preset", engine.speed.lockDelay)
		prop.setProperty("digrace.das.$preset", engine.speed.das)
		prop.setProperty("digrace.bgmno.$preset", bgmno)
		prop.setProperty("digrace.big.$preset", big)
		prop.setProperty("digrace.goaltype.$preset", goaltype)
	}

	/* Called at settings screen */
	override fun onSetting(engine:GameEngine, playerID:Int):Boolean {
		// NET: Net Ranking
		if(netIsNetRankingDisplayMode)
			netOnUpdateNetPlayRanking(engine, goaltype)
		else if(!engine.owner.replayMode) {
			// Configuration changes
			val change = updateCursor(engine, 10, playerID)

			if(change!=0) {
				engine.playSE("change")

				var m = 1
				if(engine.ctrl.isPress(Controller.BUTTON_E)) m = 100
				if(engine.ctrl.isPress(Controller.BUTTON_F)) m = 1000

				when(menuCursor) {
					0 -> {
						engine.speed.gravity += change*m
						if(engine.speed.gravity<-1) engine.speed.gravity = 99999
						if(engine.speed.gravity>99999) engine.speed.gravity = -1
					}
					1 -> {
						engine.speed.denominator += change*m
						if(engine.speed.denominator<-1) engine.speed.denominator = 99999
						if(engine.speed.denominator>99999) engine.speed.denominator = -1
					}
					2 -> {
						engine.speed.are += change
						if(engine.speed.are<0) engine.speed.are = 99
						if(engine.speed.are>99) engine.speed.are = 0
					}
					3 -> {
						engine.speed.areLine += change
						if(engine.speed.areLine<0) engine.speed.areLine = 99
						if(engine.speed.areLine>99) engine.speed.areLine = 0
					}
					4 -> {
						engine.speed.lineDelay += change
						if(engine.speed.lineDelay<0) engine.speed.lineDelay = 99
						if(engine.speed.lineDelay>99) engine.speed.lineDelay = 0
					}
					5 -> {
						engine.speed.lockDelay += change
						if(engine.speed.lockDelay<0) engine.speed.lockDelay = 99
						if(engine.speed.lockDelay>99) engine.speed.lockDelay = 0
					}
					6 -> {
						engine.speed.das += change
						if(engine.speed.das<0) engine.speed.das = 99
						if(engine.speed.das>99) engine.speed.das = 0
					}
					7 -> {
						bgmno += change
						if(bgmno<0) bgmno = BGM.count-1
						if(bgmno>=BGM.count) bgmno = 0
					}
					8 -> {
						goaltype += change
						if(goaltype<0) goaltype = 2
						if(goaltype>2) goaltype = 0
					}
					9, 10 -> {
						presetNumber += change
						if(presetNumber<0) presetNumber = 99
						if(presetNumber>99) presetNumber = 0
					}
				}

				// NET: Signal options change
				if(netIsNetPlay&&netNumSpectators>0) netSendOptions(engine)
			}

			// Confirm
			if(engine.ctrl.isPush(Controller.BUTTON_A)&&menuTime>=5) {
				engine.playSE("decide")

				if(menuCursor==9) {
					// Load preset
					loadPreset(engine, owner.modeConfig, presetNumber)

					// NET: Signal options change
					if(netIsNetPlay&&netNumSpectators>0) netSendOptions(engine)
				} else if(menuCursor==10) {
					// Save preset
					savePreset(engine, owner.modeConfig, presetNumber)
					owner.saveModeConfig()
				} else {
					// Save settings
					owner.modeConfig.setProperty("digrace.presetNumber", presetNumber)
					savePreset(engine, owner.modeConfig, -1)
					owner.saveModeConfig()

					// NET: Signal start of the game
					if(netIsNetPlay) netLobby!!.netPlayerClient!!.send("start1p\n")

					// Start game
					return false
				}
			}

			// Cancel
			if(engine.ctrl.isPush(Controller.BUTTON_B)&&!netIsNetPlay) engine.quitflag = true

			// NET: Netplay Ranking
			if(engine.ctrl.isPush(Controller.BUTTON_D)&&netIsNetPlay&&!big
				&&engine.ai==null)
				netEnterNetPlayRankingScreen(engine, playerID, goaltype)

			menuTime++
		} else {
			menuTime++
			menuCursor = -1

			return menuTime<60
		}// Replay

		return true
	}

	/* Render settings screen */
	override fun renderSetting(engine:GameEngine, playerID:Int) {
		if(netIsNetRankingDisplayMode)
		// NET: Netplay Ranking
			netOnRenderNetPlayRanking(engine, playerID, receiver)
		else {
			drawMenuSpeeds(engine, playerID, receiver, 0, EventReceiver.COLOR.BLUE, 0, engine.speed.gravity, engine.speed.denominator,
				engine.speed.are, engine.speed.areLine, engine.speed.lineDelay, engine.speed.lockDelay, engine.speed.das)
			drawMenuBGM(engine, playerID, receiver, bgmno)
			drawMenuCompact(engine, playerID, receiver, "GOAL", "${GOAL_TABLE[goaltype]}")
			if(!engine.owner.replayMode) {
				menuColor = EventReceiver.COLOR.GREEN
				drawMenuCompact(engine, playerID, receiver, "LOAD", "$presetNumber", "SAVE", "$presetNumber")
			}
		}
	}

	/* Ready */
	override fun onReady(engine:GameEngine, playerID:Int):Boolean {
		if(engine.statc[0]==0)
			if(!netIsNetPlay||!netIsWatch) {
				engine.createFieldIfNeeded()
				fillGarbage(engine, goaltype)

				// Update meter
				engine.meterValue = GOAL_TABLE[goaltype]*receiver.getBlockHeight(engine)
				engine.meterColor = GameEngine.METER_COLOR_GREEN

				// NET: Send field
				if(netNumSpectators>0) netSendField(engine)
			}
		return false
	}

	/* This function will be called before the game actually begins (after
 * Ready&Go screen disappears) */
	override fun startGame(engine:GameEngine, playerID:Int) {
		if(version<=0) engine.big = big

		if(netIsWatch)
			owner.bgmStatus.bgm = BGM.SILENT
		else
			owner.bgmStatus.bgm = BGM.values[bgmno]
	}

	/** Fill the playfield with garbage
	 * @param engine GameEngine
	 * @param height Garbage height level number
	 */
	private fun fillGarbage(engine:GameEngine, height:Int) {
		val w:Int = engine.field!!.width
		val h:Int = engine.field!!.height
		var hole:Int = -1

		for(y:Int in h-1 downTo h-GOAL_TABLE[height]) {
			var newhole:Int = -1
			do newhole = engine.random.nextInt(w)
			while(newhole==hole)
			hole = newhole

			var prevColor:COLOR? = null
			for(x:Int in 0 until w)
				if(x!=hole) {
					var color:COLOR = COLOR.WHITE
					if(y==h-1) {
						do
							color = COLOR.values()[1+engine.random.nextInt(7)]
						while(color==prevColor)
						prevColor = color
					}
					engine.field!!.setBlock(x, y, Block(color, if(y==h-1) Block.TYPE.BLOCK else Block.TYPE.GEM,
						engine.skin, Block.ATTRIBUTE.VISIBLE, Block.ATTRIBUTE.GARBAGE))
				}

			// Set connections
			if(receiver.isStickySkin(engine)&&y!=h-1)
				for(x:Int in 0 until w)
					if(x!=hole) {
						val blk:Block? = engine.field!!.getBlock(x, y)
						if(blk!=null) {
							if(!engine.field!!.getBlockEmpty(x-1, y)) blk.setAttribute(true, Block.ATTRIBUTE.CONNECT_LEFT)
							if(!engine.field!!.getBlockEmpty(x+1, y))
								blk.setAttribute(true, Block.ATTRIBUTE.CONNECT_RIGHT)
						}
					}
		}
	}

	private fun getRemainGarbageLines(engine:GameEngine?, height:Int):Int {
		if(engine?.field==null) return -1

		val w = engine.field!!.width
		val h = engine.field!!.height
		var lines = 0

		for(y in h-1 downTo h-GOAL_TABLE[height])
			if(!engine.field!!.getLineFlag(y))
				for(x in 0 until w) {
					val blk = engine.field!!.getBlock(x, y)

					if(blk!=null&&blk.getAttribute(Block.ATTRIBUTE.GARBAGE)) {
						lines++
						break
					}
				}

		return lines
	}

	/* Score display */
	override fun renderLast(engine:GameEngine, playerID:Int) {
		if(owner.menuOnly) return

		receiver.drawScoreFont(engine, playerID, 0, 0, "DIG RACE", EventReceiver.COLOR.GREEN)
		receiver.drawScoreFont(engine, playerID, 0, 1, "("+GOAL_TABLE[goaltype]
			+" GARBAGE GAME)", EventReceiver.COLOR.GREEN)

		if(engine.stat==GameEngine.Status.SETTING||engine.stat==GameEngine.Status.RESULT&&!owner.replayMode) {
			if(!owner.replayMode&&engine.ai==null&&!netIsWatch) {
				val strPieceTemp = if(owner.receiver.nextDisplayType==2) "P." else "PIECE"
				receiver.drawScoreFont(engine, playerID, 3, 3, "TIME     LINE $strPieceTemp", EventReceiver.COLOR.BLUE)

				for(i in 0 until RANKING_MAX) {
					receiver.drawScoreGrade(engine, playerID, 0, 4+i, String.format("%2d", i+1), EventReceiver.COLOR.YELLOW)
					receiver.drawScoreNum(engine, playerID, 3, 4+i, GeneralUtil.getTime(rankingTime[goaltype][i]), rankingRank==i)
					receiver.drawScoreNum(engine, playerID, 12, 4+i, "${rankingLines[goaltype][i]}", rankingRank==i)
					receiver.drawScoreNum(engine, playerID, 17, 4+i, "${rankingPiece[goaltype][i]}", rankingRank==i)
				}
			}
		} else {
			val remainLines = getRemainGarbageLines(engine, goaltype)
			var strLines = "$remainLines"
			if(remainLines<0) strLines = "0"
			var fontcolor = EventReceiver.COLOR.WHITE
			if(remainLines in 1..14) fontcolor = EventReceiver.COLOR.YELLOW
			if(remainLines in 1..8) fontcolor = EventReceiver.COLOR.ORANGE
			if(remainLines in 1..4) fontcolor = EventReceiver.COLOR.RED

			if(remainLines>0)
				receiver.drawMenuNum(engine, playerID, 5-maxOf(minOf(1, strLines.length), 3), 21, strLines, fontcolor, 2f)

			receiver.drawScoreFont(engine, playerID, 0, 3, "LINE", EventReceiver.COLOR.BLUE)
			receiver.drawScoreNum(engine, playerID, 0, 4, engine.statistics.lines.toString())

			receiver.drawScoreFont(engine, playerID, 0, 6, "PIECE", EventReceiver.COLOR.BLUE)
			receiver.drawScoreNum(engine, playerID, 0, 7, engine.statistics.totalPieceLocked.toString())

			receiver.drawScoreFont(engine, playerID, 0, 9, "LINE/MIN", EventReceiver.COLOR.BLUE)
			receiver.drawScoreNum(engine, playerID, 0, 10, "${engine.statistics.lpm}")

			receiver.drawScoreFont(engine, playerID, 0, 12, "PIECE/SEC", EventReceiver.COLOR.BLUE)
			receiver.drawScoreNum(engine, playerID, 0, 13, engine.statistics.pps.toString())

			receiver.drawScoreFont(engine, playerID, 0, 15, "TIME", EventReceiver.COLOR.BLUE)
			receiver.drawScoreNum(engine, playerID, 0, 16, GeneralUtil.getTime(engine.statistics.time))
		}

		super.renderLast(engine, playerID)
	}

	/* Calculate score */
	override fun calcScore(engine:GameEngine, playerID:Int, lines:Int):Int {
		// Update meter
		val remainLines = getRemainGarbageLines(engine, goaltype)
		engine.meterValue = remainLines*receiver.getBlockHeight(engine)
		engine.meterColor = GameEngine.METER_COLOR_GREEN
		if(remainLines<=14) engine.meterColor = GameEngine.METER_COLOR_YELLOW
		if(remainLines<=8) engine.meterColor = GameEngine.METER_COLOR_ORANGE
		if(remainLines<=4) engine.meterColor = GameEngine.METER_COLOR_RED

		// Game is completed when there is no gem blocks
		if(lines>0&&remainLines==0) {
			engine.ending = 1
			engine.gameEnded()
		}
		return 0
	}

	/* Render results screen */
	override fun renderResult(engine:GameEngine, playerID:Int) {
		drawResultStats(engine, playerID, receiver, 1, EventReceiver.COLOR.BLUE, Statistic.LINES, Statistic.PIECE, Statistic.TIME, Statistic.LPM, Statistic.PPS)
		drawResultRank(engine, playerID, receiver, 11, EventReceiver.COLOR.BLUE, rankingRank)
		drawResultNetRank(engine, playerID, receiver, 13, EventReceiver.COLOR.BLUE, netRankingRank[0])
		drawResultNetRankDaily(engine, playerID, receiver, 15, EventReceiver.COLOR.BLUE, netRankingRank[1])

		if(netIsPB) receiver.drawMenuFont(engine, playerID, 2, 18, "NEW PB", EventReceiver.COLOR.ORANGE)

		if(netIsNetPlay&&netReplaySendStatus==1)
			receiver.drawMenuFont(engine, playerID, 0, 19, "SENDING...", EventReceiver.COLOR.PINK)
		else if(netIsNetPlay&&!netIsWatch
			&&netReplaySendStatus==2)
			receiver.drawMenuFont(engine, playerID, 1, 19, "A: RETRY", EventReceiver.COLOR.RED)
	}

	/* Save replay file */
	override fun saveReplay(engine:GameEngine, playerID:Int, prop:CustomProperties) {
		engine.owner.replayProp.setProperty("digrace.version", version)
		savePreset(engine, engine.owner.replayProp, -1)

		// NET: Save name
		if(netPlayerName!=null&&netPlayerName!!.isNotEmpty()) prop.setProperty("$playerID.net.netPlayerName", netPlayerName)

		// Update rankings
		if(!owner.replayMode&&getRemainGarbageLines(engine, goaltype)==0&&engine.ending!=0&&engine.ai==null&&!netIsWatch) {
			updateRanking(engine.statistics.time, engine.statistics.lines, engine.statistics.totalPieceLocked)

			if(rankingRank!=-1) {
				saveRanking(owner.recordProp, engine.ruleopt.strRuleName)
				owner.saveModeConfig()
			}
		}
	}

	/** Read rankings from property file
	 * @param prop Property file
	 * @param ruleName Rule name
	 */
	override fun loadRanking(prop:CustomProperties, ruleName:String) {
		for(i in 0 until GOALTYPE_MAX)
			for(j in 0 until RANKING_MAX) {
				rankingTime[i][j] = prop.getProperty("digrace.ranking.$ruleName.$i.time.$j", -1)
				rankingLines[i][j] = prop.getProperty("digrace.ranking.$ruleName.$i.lines.$j", 0)
				rankingPiece[i][j] = prop.getProperty("digrace.ranking.$ruleName.$i.piece.$j", 0)
			}
	}

	/** Save rankings to property file
	 * @param prop Property file
	 * @param ruleName Rule name
	 */
	fun saveRanking(prop:CustomProperties, ruleName:String) {
		for(i in 0 until GOALTYPE_MAX)
			for(j in 0 until RANKING_MAX) {
				prop.setProperty("digrace.ranking.$ruleName.$i.time.$j", rankingTime[i][j])
				prop.setProperty("digrace.ranking.$ruleName.$i.lines.$j", rankingLines[i][j])
				prop.setProperty("digrace.ranking.$ruleName.$i.piece.$j", rankingPiece[i][j])
			}
	}

	/** Update rankings
	 * @param time Time
	 * @param piece Piececount
	 */
	private fun updateRanking(time:Int, lines:Int, piece:Int) {
		rankingRank = checkRanking(time, lines, piece)

		if(rankingRank!=-1) {
			// Shift down ranking entries
			for(i in RANKING_MAX-1 downTo rankingRank+1) {
				rankingTime[goaltype][i] = rankingTime[goaltype][i-1]
				rankingLines[goaltype][i] = rankingLines[goaltype][i-1]
				rankingPiece[goaltype][i] = rankingPiece[goaltype][i-1]
			}

			// Add new data
			rankingTime[goaltype][rankingRank] = time
			rankingLines[goaltype][rankingRank] = lines
			rankingPiece[goaltype][rankingRank] = piece
		}
	}

	/** Calculate ranking position
	 * @param time Time
	 * @param piece Piececount
	 * @return Position (-1 if unranked)
	 */
	private fun checkRanking(time:Int, lines:Int, piece:Int):Int {
		for(i in 0 until RANKING_MAX)
			if(time<rankingTime[goaltype][i]||rankingTime[goaltype][i]<0)
				return i
			else if(time==rankingTime[goaltype][i]&&lines<rankingLines[goaltype][i])
				return i
			else if(time==rankingTime[goaltype][i]&&lines==rankingLines[goaltype][i]
				&&piece<rankingPiece[goaltype][i])
				return i

		return -1
	}

	/** NET: Send various in-game stats (as well as goaltype)
	 * @param engine GameEngine
	 */
	override fun netSendStats(engine:GameEngine) {
		var msg = "game\tstats\t"
		msg += "${engine.statistics.lines}\t${engine.statistics.totalPieceLocked}\t"
		msg += "${engine.statistics.time}\t${engine.statistics.lpm}\t"
		msg += engine.statistics.pps.toString()+"\t$goaltype\t"
		msg += engine.gameActive.toString()+"\t${engine.timerActive}\t"
		msg += engine.meterColor.toString()+"\t"+engine.meterValue
		msg += "\n"
		netLobby!!.netPlayerClient!!.send(msg)
	}

	/** NET: Receive various in-game stats (as well as goaltype) */
	override fun netRecvStats(engine:GameEngine, message:Array<String>) {
		engine.statistics.lines = Integer.parseInt(message[4])
		engine.statistics.totalPieceLocked = Integer.parseInt(message[5])
		engine.statistics.time = Integer.parseInt(message[6])
		//engine.statistics.lpm = java.lang.Float.parseFloat(message[7])
		//engine.statistics.pps = java.lang.Float.parseFloat(message[8])
		goaltype = Integer.parseInt(message[9])
		engine.gameActive = java.lang.Boolean.parseBoolean(message[10])
		engine.timerActive = java.lang.Boolean.parseBoolean(message[11])
		engine.meterColor = Integer.parseInt(message[12])
		engine.meterValue = Integer.parseInt(message[13])
	}

	/** NET: Send end-of-game stats
	 * @param engine GameEngine
	 */
	override fun netSendEndGameStats(engine:GameEngine) {
		var subMsg = ""
		subMsg += "GARBAGE;${(GOAL_TABLE[goaltype]-getRemainGarbageLines(engine, goaltype))}/${GOAL_TABLE[goaltype]}\t"
		subMsg += "LINE;${engine.statistics.lines}\t"
		subMsg += "PIECE;${engine.statistics.totalPieceLocked}\t"
		subMsg += "TIME;${GeneralUtil.getTime(engine.statistics.time)}\t"
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
		msg += engine.speed.gravity.toString()+"\t${engine.speed.denominator}\t${engine.speed.are}\t"
		msg += engine.speed.areLine.toString()+"\t${engine.speed.lineDelay}\t${engine.speed.lockDelay}\t"
		msg += engine.speed.das.toString()+"\t$bgmno\t$goaltype\t"+presetNumber
		msg += "\n"
		netLobby!!.netPlayerClient!!.send(msg)
	}

	/** NET: Receive game options */
	override fun netRecvOptions(engine:GameEngine, message:Array<String>) {
		engine.speed.gravity = Integer.parseInt(message[4])
		engine.speed.denominator = Integer.parseInt(message[5])
		engine.speed.are = Integer.parseInt(message[6])
		engine.speed.areLine = Integer.parseInt(message[7])
		engine.speed.lineDelay = Integer.parseInt(message[8])
		engine.speed.lockDelay = Integer.parseInt(message[9])
		engine.speed.das = Integer.parseInt(message[10])
		bgmno = Integer.parseInt(message[11])
		goaltype = Integer.parseInt(message[12])
		presetNumber = Integer.parseInt(message[13])
	}

	/** NET: Get goal type */
	override fun netGetGoalType():Int = goaltype

	/** NET: It returns true when the current settings doesn't prevent
	 * leaderboard screen from showing. */
	override fun netIsNetRankingViewOK(engine:GameEngine):Boolean = engine.ai==null

	/** NET: It returns true when the current settings doesn't prevent replay
	 * data from sending. */
	override fun netIsNetRankingSendOK(engine:GameEngine):Boolean =
		netIsNetRankingViewOK(engine)&&getRemainGarbageLines(engine, goaltype)==0&&engine.ending!=0

	companion object {
		/* ----- Main variables ----- */
		/** Logger */
		internal var log = Logger.getLogger(SprintDig::class.java)

		/** Current version */
		private const val CURRENT_VERSION = 1

		/** Number of entries in rankings */
		private const val RANKING_MAX = 10

		/** Number of goal type */
		private const val GOALTYPE_MAX = 3

		/** Table of garbage lines */
		private val GOAL_TABLE = intArrayOf(5, 10, 18)
	}
}
