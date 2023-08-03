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

import mu.nu.nullpo.game.event.EventReceiver.COLOR
import mu.nu.nullpo.game.event.ScoreEvent
import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.game.play.GameManager
import mu.nu.nullpo.util.GeneralUtil.toTimeStr

/** NET-VS-LINE RACE mode */
class NetVSSprintLine:NetDummyVSMode() {
	/** Number of lines required to win */
	private var goalLines:Int = 0 // TODO: Add option to change this

	/* Mode name */
	override val name = "NET-VS-LINE RACE"

	/* Mode init */
	override fun modeInit(manager:GameManager) {
		super.modeInit(manager)
		goalLines = 40
	}

	/* Player init */
	override fun netPlayerInit(engine:GameEngine) {
		super.netPlayerInit(engine)
		engine.meterColor = GameEngine.METER_COLOR_GREEN
	}

	/** Apply room settings, but ignore non-speed settings */
	override fun netVSApplyRoomSettings(engine:GameEngine) {
		if(netCurrentRoomInfo!=null) {
			engine.speed.gravity = netCurrentRoomInfo!!.gravity
			engine.speed.denominator = netCurrentRoomInfo!!.denominator
			engine.speed.are = netCurrentRoomInfo!!.are
			engine.speed.areLine = netCurrentRoomInfo!!.areLine
			engine.speed.lineDelay = netCurrentRoomInfo!!.lineDelay
			engine.speed.lockDelay = netCurrentRoomInfo!!.lockDelay
			engine.speed.das = netCurrentRoomInfo!!.das
		}
	}

	/* Called at game start */
	override fun startGame(engine:GameEngine) {
		super.startGame(engine)
		engine.meterColor = GameEngine.METER_COLOR_GREEN
		engine.meterValue = 1f
	}

	/** @return Player's place */
	private fun getNowPlayerPlace(engine:GameEngine):Int {
		val pid = engine.playerID
		if(!netVSPlayerExist[pid]||netVSPlayerDead[pid]) return -1

		var place = 0
		val myLines = minOf(engine.statistics.lines, goalLines)

		for(i in 0..<players)
			if(i!=pid&&netVSPlayerExist[i]&&!netVSPlayerDead[i]) {
				val enemyLines = minOf(owner.engine[i].statistics.lines, goalLines)

				if(myLines<enemyLines)
					place++
				else if(myLines==enemyLines&&engine.statistics.pps<owner.engine[i].statistics.pps)
					place++
				else if(myLines==enemyLines&&engine.statistics.pps==owner.engine[i].statistics.pps&&
					engine.statistics.lpm<owner.engine[i].statistics.lpm)
					place++
			}

		return place
	}

	/** Update progress meter*/
	private fun updateMeter(engine:GameEngine) {
		if(goalLines>0) {
			val remainLines = goalLines-engine.statistics.lines
			engine.meterValue = remainLines*1f/goalLines
			engine.meterColor = GameEngine.METER_COLOR_LIMIT
		}
	}

	/* Calculate Score */
	override fun calcScore(engine:GameEngine, ev:ScoreEvent):Int {
		// Meter
		updateMeter(engine)

		// Game Completed
		if(engine.statistics.lines>=goalLines&&engine.playerID==0)
			if(netVSIsPractice) {
				engine.stat = GameEngine.Status.EXCELLENT
				engine.resetStatc()
			} else {
				// Send game end message
				val places = IntArray(NET_MAX_PLAYERS)
				val uidArray = IntArray(NET_MAX_PLAYERS)
				for(i in 0..<players) {
					places[i] = getNowPlayerPlace(owner.engine[i])
					uidArray[i] = -1
				}
				for(i in 0..<players)
					if(places[i] in 0..<NET_MAX_PLAYERS) uidArray[places[i]] = netVSPlayerUID[i]

				val strMsg = StringBuilder("racewin")
				for(i in 0..<players)
					if(uidArray[i]!=-1) strMsg.append("\t").append(uidArray[i])
				strMsg.append("\n")
				netLobby!!.netPlayerClient!!.send("$strMsg")

				// Wait until everyone dies
				engine.stat = GameEngine.Status.NOTHING
				engine.resetStatc()
			}
		return 0
	}

	/* Drawing processing at the end of every frame */
	override fun renderLast(engine:GameEngine) {
		super.renderLast(engine)

		val x = receiver.fieldX(engine)
		val y = receiver.fieldY(engine)

		val pid = engine.playerID
		if(netVSPlayerExist[pid]&&engine.isVisible) {
			if((netVSIsGameActive||netVSIsPractice&&pid==0)&&engine.stat!=GameEngine.Status.RESULT) {
				// Lines left
				val remainLines = maxOf(0, goalLines-engine.statistics.lines)
				val fontColor = when(remainLines) {
					in 1..10 -> COLOR.RED
					in 1..20 -> COLOR.ORANGE
					in 1..30 -> COLOR.YELLOW
					else -> COLOR.WHITE
				}

				val strLines = "$remainLines"

				when {
					engine.displaySize!=-1 -> when(strLines.length) {
						1 -> receiver.drawMenuFont(engine, 4, 21, strLines, fontColor, 2f)
						2 -> receiver.drawMenuFont(engine, 3, 21, strLines, fontColor, 2f)
						3 -> receiver.drawMenuFont(engine, 2, 21, strLines, fontColor, 2f)
					}
					strLines.length==1 -> receiver.drawDirectFont(x+4+32, y+168, strLines, fontColor, 1f)
					strLines.length==2 -> receiver.drawDirectFont(x+4+24, y+168, strLines, fontColor, 1f)
					strLines.length==3 -> receiver.drawDirectFont(x+4+16, y+168, strLines, fontColor, 1f)
				}
			}

			if(netVSIsGameActive&&engine.stat!=GameEngine.Status.RESULT) {
				// Place
				var place = getNowPlayerPlace(engine)
				if(netVSPlayerDead[pid]) place = netVSPlayerPlace[pid]

				when {
					engine.displaySize!=-1 -> when(place) {
						0 -> receiver.drawMenuFont(engine, -3, 22, "1ST", COLOR.ORANGE)
						1 -> receiver.drawMenuFont(engine, -3, 22, "2ND", COLOR.WHITE)
						2 -> receiver.drawMenuFont(engine, -3, 22, "3RD", COLOR.RED)
						3 -> receiver.drawMenuFont(engine, -3, 22, "4TH", COLOR.GREEN)
						4 -> receiver.drawMenuFont(engine, -3, 22, "5TH", COLOR.BLUE)
						5 -> receiver.drawMenuFont(engine, -3, 22, "6TH", COLOR.PURPLE)
					}
					place==0 -> receiver.drawDirectFont(x, y+168, "1ST", COLOR.ORANGE, .5f)
					place==1 -> receiver.drawDirectFont(x, y+168, "2ND", COLOR.WHITE, .5f)
					place==2 -> receiver.drawDirectFont(x, y+168, "3RD", COLOR.RED, .5f)
					place==3 -> receiver.drawDirectFont(x, y+168, "4TH", COLOR.GREEN, .5f)
					place==4 -> receiver.drawDirectFont(x, y+168, "5TH", COLOR.BLUE, .5f)
					place==5 -> receiver.drawDirectFont(x, y+168, "6TH", COLOR.PURPLE, .5f)
				}
			} else if(!netVSIsPractice||pid!=0) {
				val strTemp = "${netVSPlayerWinCount[pid]}/${netVSPlayerPlayCount[pid]}"

				if(engine.displaySize!=-1) {
					var y2 = 21
					if(engine.stat==GameEngine.Status.RESULT) y2 = 22
					owner.receiver.drawMenuFont(engine, 0, y2, strTemp, COLOR.WHITE)
				} else
					owner.receiver.drawDirectFont(x+4, y+168, strTemp, COLOR.WHITE, .5f)
			}// Games count
		}
	}

	/* Render results screen */
	override fun renderResult(engine:GameEngine) {
		super.renderResult(engine)

		val scale = if(engine.displaySize==-1) .5f else 1f

		drawResultScale(
			engine, receiver, 2, COLOR.ORANGE, scale, "LINE",
			"%10d".format(engine.statistics.lines), "PIECE", "%10d".format(engine.statistics.totalPieceLocked),
			"LINE/MIN", "%10g".format(engine.statistics.lpm), "PIECE/SEC", "%10g".format(engine.statistics.pps),
			"Time", "%10s".format(engine.statistics.time.toTimeStr)
		)
	}

	/* Send stats */
	override fun netSendStats(engine:GameEngine) {
		if(engine.playerID==0&&!netVSIsPractice&&!netVSIsWatch()) {
			val strMsg = ("game\tstats\t${engine.statistics.lines}\t${engine.statistics.pps}\t"+engine.statistics.lpm
				+"\n")
			netLobby!!.netPlayerClient!!.send(strMsg)
		}
	}

	/* Receive stats */
	override fun netRecvStats(engine:GameEngine, message:List<String>) {
		if(message.size>4) engine.statistics.lines = message[4].toInt()
		//if(message.size>5) engine.statistics.pps = message[5].toFloat()
		//if(message.size>6) engine.statistics.lpm = message[6].toFloat()
		updateMeter(engine)
	}

	/* Send end-of-game stats */
	override fun netSendEndGameStats(engine:GameEngine) {
		val playerID = engine.playerID
		val msg = "gstat\t"+
			"${netVSPlayerPlace[playerID]}\t"+
			"0\t0\t0\t"+
			"${engine.statistics.lines}\t${engine.statistics.lpm}\t"+
			"${engine.statistics.totalPieceLocked}\t${engine.statistics.pps}\t"+
			"$netVSPlayTimer\t0\t${netVSPlayerWinCount[playerID]}\t${netVSPlayerPlayCount[playerID]}"+
			"\n"
		netLobby?.netPlayerClient?.send(msg)
	}

	/* Receive end-of-game stats */
	override fun netVSRecvEndGameStats(message:List<String>) {
		val seatID = message[2].toInt()
		val playerID = netVSGetPlayerIDbySeatID(seatID)

		if(playerID!=0||netVSIsWatch()) {
			val engine = owner.engine[playerID]

			engine.statistics.lines = message[8].toInt()
			//engine.statistics.lpm = message[9].toFloat()
			engine.statistics.totalPieceLocked = message[10].toInt()
			//engine.statistics.pps = message[11].toFloat()
			engine.statistics.time = message[12].toInt()

			netVSPlayerResultReceived[playerID] = true
		}
	}
}
