package mu.nu.nullpo.game.subsystem.mode

import mu.nu.nullpo.game.event.EventReceiver.COLOR
import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.game.play.GameManager
import mu.nu.nullpo.util.GeneralUtil

/** NET-VS-LINE RACE mode */
class NetVSLineRaceMode:NetDummyVSMode() {
	/** Number of lines required to win */
	private var goalLines:Int = 0 // TODO: Add option to change this

	/* Mode name */
	override val name:String
		get() = "NET-VS-LINE RACE"

	/* Mode init */
	override fun modeInit(manager:GameManager) {
		super.modeInit(manager)
		goalLines = 40
	}

	/* Player init */
	override fun netPlayerInit(engine:GameEngine, playerID:Int) {
		super.netPlayerInit(engine, playerID)
		engine.meterColor = GameEngine.METER_COLOR_GREEN
	}

	/** Apply room settings, but ignore non-speed settings */
	override fun netvsApplyRoomSettings(engine:GameEngine) {
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
	override fun startGame(engine:GameEngine, playerID:Int) {
		super.startGame(engine, playerID)
		engine.meterColor = GameEngine.METER_COLOR_GREEN
		engine.meterValue = owner.receiver.getMeterMax(engine)
	}

	/** Get player's place
	 * @param engine GameEngine
	 * @param playerID Player ID
	 * @return Player's place
	 */
	private fun getNowPlayerPlace(engine:GameEngine, playerID:Int):Int {
		if(!netvsPlayerExist[playerID]||netvsPlayerDead[playerID]) return -1

		var place = 0
		val myLines = minOf(engine.statistics.lines, goalLines)

		for(i in 0 until players)
			if(i!=playerID&&netvsPlayerExist[i]&&!netvsPlayerDead[i]) {
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

	/** Update progress meter
	 * @param engine GameEngine
	 */
	private fun updateMeter(engine:GameEngine) {
		if(goalLines>0) {
			val remainLines = goalLines-engine.statistics.lines
			engine.meterValue = remainLines*owner.receiver.getMeterMax(engine)/goalLines
			engine.meterColor = GameEngine.METER_COLOR_GREEN
			if(remainLines<=30) engine.meterColor = GameEngine.METER_COLOR_YELLOW
			if(remainLines<=20) engine.meterColor = GameEngine.METER_COLOR_ORANGE
			if(remainLines<=10) engine.meterColor = GameEngine.METER_COLOR_RED
		}
	}

	/* Calculate Score */
	override fun calcScore(engine:GameEngine, playerID:Int, lines:Int):Int {
		// Meter
		updateMeter(engine)

		// Game Completed
		if(engine.statistics.lines>=goalLines&&playerID==0)
			if(netvsIsPractice) {
				engine.stat = GameEngine.Status.EXCELLENT
				engine.resetStatc()
			} else {
				// Send game end message
				val places = IntArray(NETVS_MAX_PLAYERS)
				val uidArray = IntArray(NETVS_MAX_PLAYERS)
				for(i in 0 until players) {
					places[i] = getNowPlayerPlace(owner.engine[i], i)
					uidArray[i] = -1
				}
				for(i in 0 until players)
					if(places[i] in 0 until NETVS_MAX_PLAYERS) uidArray[places[i]] = netvsPlayerUID[i]

				val strMsg = StringBuilder("racewin")
				for(i in 0 until players)
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
	override fun renderLast(engine:GameEngine, playerID:Int) {
		super.renderLast(engine, playerID)

		val x = owner.receiver.fieldX(engine, playerID)
		val y = owner.receiver.fieldY(engine, playerID)


		if(netvsPlayerExist[playerID]&&engine.isVisible) {
			if((netvsIsGameActive||netvsIsPractice&&playerID==0)&&engine.stat!=GameEngine.Status.RESULT) {
				// Lines left
				val remainLines = maxOf(0, goalLines-engine.statistics.lines)
				val fontColor = when(remainLines) {
						in 1..30 -> COLOR.YELLOW
						in 1..20 -> COLOR.ORANGE
						in 1..10 -> COLOR.RED
						else -> COLOR.WHITE
					}

				val strLines = "$remainLines"

				when {
					engine.displaysize!=-1 -> when(strLines.length) {
						1 -> owner.receiver.drawMenuFont(engine, playerID, 4, 21, strLines, fontColor, 2f)
						2 -> owner.receiver.drawMenuFont(engine, playerID, 3, 21, strLines, fontColor, 2f)
						3 -> owner.receiver.drawMenuFont(engine, playerID, 2, 21, strLines, fontColor, 2f)
					}
					strLines.length==1 -> owner.receiver.drawDirectFont(x+4+32, y+168, strLines, fontColor, 1f)
					strLines.length==2 -> owner.receiver.drawDirectFont(x+4+24, y+168, strLines, fontColor, 1f)
					strLines.length==3 -> owner.receiver.drawDirectFont(x+4+16, y+168, strLines, fontColor, 1f)
				}
			}

			if(netvsIsGameActive&&engine.stat!=GameEngine.Status.RESULT) {
				// Place
				var place = getNowPlayerPlace(engine, playerID)
				if(netvsPlayerDead[playerID]) place = netvsPlayerPlace[playerID]

				when {
					engine.displaysize!=-1 -> when(place) {
						0 -> owner.receiver.drawMenuFont(engine, playerID, -2, 22, "1ST", COLOR.ORANGE)
						1 -> owner.receiver.drawMenuFont(engine, playerID, -2, 22, "2ND", COLOR.WHITE)
						2 -> owner.receiver.drawMenuFont(engine, playerID, -2, 22, "3RD", COLOR.RED)
						3 -> owner.receiver.drawMenuFont(engine, playerID, -2, 22, "4TH", COLOR.GREEN)
						4 -> owner.receiver.drawMenuFont(engine, playerID, -2, 22, "5TH", COLOR.BLUE)
						5 -> owner.receiver.drawMenuFont(engine, playerID, -2, 22, "6TH", COLOR.PURPLE)
					}
					place==0 -> owner.receiver.drawDirectFont(x, y+168, "1ST", COLOR.ORANGE, .5f)
					place==1 -> owner.receiver.drawDirectFont(x, y+168, "2ND", COLOR.WHITE, .5f)
					place==2 -> owner.receiver.drawDirectFont(x, y+168, "3RD", COLOR.RED, .5f)
					place==3 -> owner.receiver.drawDirectFont(x, y+168, "4TH", COLOR.GREEN, .5f)
					place==4 -> owner.receiver.drawDirectFont(x, y+168, "5TH", COLOR.BLUE, .5f)
					place==5 -> owner.receiver.drawDirectFont(x, y+168, "6TH", COLOR.PURPLE, .5f)
				}
			} else if(!netvsIsPractice||playerID!=0) {
				val strTemp = "${netvsPlayerWinCount[playerID]}/${netvsPlayerPlayCount[playerID]}"

				if(engine.displaysize!=-1) {
					var y2 = 21
					if(engine.stat==GameEngine.Status.RESULT) y2 = 22
					owner.receiver.drawMenuFont(engine, playerID, 0, y2, strTemp, COLOR.WHITE)
				} else
					owner.receiver.drawDirectFont(x+4, y+168, strTemp, COLOR.WHITE, .5f)
			}// Games count
		}
	}

	/* Render results screen */
	override fun renderResult(engine:GameEngine, playerID:Int) {
		super.renderResult(engine, playerID)

		var scale = 1f
		if(engine.displaysize==-1) scale = .5f

		drawResultScale(engine, playerID, owner.receiver, 2, COLOR.ORANGE, scale, "LINE", String.format("%10d", engine.statistics.lines), "PIECE", String.format("%10d", engine.statistics.totalPieceLocked), "LINE/MIN", String.format("%10g", engine.statistics.lpm), "PIECE/SEC", String.format("%10g", engine.statistics.pps), "TIME", String.format("%10s", GeneralUtil.getTime(engine.statistics.time)))
	}

	/* Send stats */
	override fun netSendStats(engine:GameEngine) {
		if(engine.playerID==0&&!netvsIsPractice&&!netvsIsWatch()) {
			val strMsg = ("game\tstats\t${engine.statistics.lines}\t${engine.statistics.pps}\t"+engine.statistics.lpm
				+"\n")
			netLobby!!.netPlayerClient!!.send(strMsg)
		}
	}

	/* Receive stats */
	override fun netRecvStats(engine:GameEngine, message:Array<String>) {
		if(message.size>4) engine.statistics.lines = Integer.parseInt(message[4])
		//if(message.size>5) engine.statistics.pps = java.lang.Float.parseFloat(message[5])
		//if(message.size>6) engine.statistics.lpm = java.lang.Float.parseFloat(message[6])
		updateMeter(engine)
	}

	/* Send end-of-game stats */
	override fun netSendEndGameStats(engine:GameEngine) {
		val playerID = engine.playerID
		var msg = "gstat\t"
		msg += "${netvsPlayerPlace[playerID]}\t"
		msg += 0.toString()+"\t${0}\t${0}\t"
		msg += "${engine.statistics.lines}\t${engine.statistics.lpm}\t"
		msg += engine.statistics.totalPieceLocked.toString()+"\t${engine.statistics.pps}\t"
		msg += "$netvsPlayTimer${"\t${0}\t"+netvsPlayerWinCount[playerID]}\t"+netvsPlayerPlayCount[playerID]
		msg += "\n"
		netLobby!!.netPlayerClient!!.send(msg)
	}

	/* Receive end-of-game stats */
	override fun netvsRecvEndGameStats(message:Array<String>) {
		val seatID = Integer.parseInt(message[2])
		val playerID = netvsGetPlayerIDbySeatID(seatID)

		if(playerID!=0||netvsIsWatch()) {
			val engine = owner.engine[playerID]

			engine.statistics.lines = Integer.parseInt(message[8])
			//engine.statistics.lpm = java.lang.Float.parseFloat(message[9])
			engine.statistics.totalPieceLocked = Integer.parseInt(message[10])
			//engine.statistics.pps = java.lang.Float.parseFloat(message[11])
			engine.statistics.time = Integer.parseInt(message[12])

			netvsPlayerResultReceived[playerID] = true
		}
	}
}
