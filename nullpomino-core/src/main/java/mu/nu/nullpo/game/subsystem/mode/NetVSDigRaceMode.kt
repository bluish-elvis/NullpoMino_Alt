package mu.nu.nullpo.game.subsystem.mode

import mu.nu.nullpo.game.component.Block
import mu.nu.nullpo.game.event.EventReceiver
import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.game.play.GameManager
import mu.nu.nullpo.util.GeneralUtil

/** NET-VS-DIG RACE mode */
class NetVSDigRaceMode:NetDummyVSMode() {
	/** Number of garbage lines to clear */
	private var goalLines:Int = 0 // TODO: Add option to change this

	/** Number of garbage lines left */
	private var playerRemainLines:IntArray = IntArray(NetDummyVSMode.NETVS_MAX_PLAYERS)

	/** Number of gems available at the start of the game (for values game) */
	private var playerStartGems:IntArray = IntArray(NetDummyVSMode.NETVS_MAX_PLAYERS)

	/* Mode name */
	override val name:String
		get() = "NET-VS-DIG RACE"

	/* Mode init */
	override fun modeInit(manager:GameManager) {
		super.modeInit(manager)
		goalLines = 18
		playerRemainLines = IntArray(NetDummyVSMode.NETVS_MAX_PLAYERS)
		playerStartGems = IntArray(NetDummyVSMode.NETVS_MAX_PLAYERS)
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

	/** Fill the playfield with garbage
	 * @param engine GameEngine
	 * @param playerID Player ID
	 */
	private fun fillGarbage(engine:GameEngine, playerID:Int) {
		val w = engine.field!!.width
		val h = engine.field!!.height
		var hole = -1
		var skin = engine.skin
		if(playerID!=0||netvsIsWatch()) skin = netvsPlayerSkin[playerID]
		if(skin<0) skin = 0

		for(y in h-1 downTo h-goalLines) {
			if(hole==-1||engine.random.nextInt(100)<netCurrentRoomInfo!!.garbagePercent) {
				var newhole = -1
				do
					newhole = engine.random.nextInt(w)
				while(newhole==hole)
				hole = newhole
			}

			var prevColor = -1
			for(x in 0 until w)
				if(x!=hole) {
					var color = Block.BLOCK_COLOR_GRAY
					if(y==h-1) {
						do
							color = Block.BLOCK_COLOR_GEM_RED+engine.random.nextInt(7)
						while(color==prevColor)
						prevColor = color
					}
					engine.field!!.setBlock(x, y, Block(color, skin, Block.BLOCK_ATTRIBUTE_VISIBLE or Block.BLOCK_ATTRIBUTE_GARBAGE))
				}

			// Set connections
			if(y!=h-1)
				for(x in 0 until w)
					if(x!=hole) {
						val blk = engine.field!!.getBlock(x, y)
						if(blk!=null) {
							if(!engine.field!!.getBlockEmpty(x-1, y)) blk.setAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_LEFT, true)
							if(!engine.field!!.getBlockEmpty(x+1, y))
								blk.setAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_RIGHT, true)
						}
					}
		}
	}

	/** Get number of garbage lines left
	 * @param engine GameEngine
	 * @param playerID Player ID
	 * @return Number of garbage lines left
	 */
	private fun getRemainGarbageLines(engine:GameEngine?, playerID:Int):Int {
		if(engine?.field==null) return -1

		val w = engine.field!!.width
		val h = engine.field!!.height
		var lines = 0
		var hasGemBlock = false

		for(y in h-1 downTo h-goalLines)
			if(!engine.field!!.getLineFlag(y))
				for(x in 0 until w) {
					val blk = engine.field!!.getBlock(x, y)

					if(blk!=null&&blk.isGemBlock) hasGemBlock = true
					if(blk!=null&&blk.getAttribute(Block.BLOCK_ATTRIBUTE_GARBAGE)) {
						lines++
						break
					}
				}

		return if(!hasGemBlock) 0 else lines

	}

	/** Turn all normal blocks to gem (for values game)
	 * @param engine GameEngine
	 * @param playerID Player ID
	 */
	private fun turnAllBlocksToGem(engine:GameEngine, playerID:Int) {
		val w = engine.field!!.width
		val h = engine.field!!.height

		for(y in engine.field!!.highestBlockY until h)
			for(x in 0 until w) {
				val blk = engine.field!!.getBlock(x, y)
				if(blk!=null&&blk.cint>=Block.BLOCK_COLOR_RED&&blk.cint<=Block.BLOCK_COLOR_PURPLE)
					blk.cint = Block.BLOCK_COLOR_GEM_RED+blk.cint-2
			}
	}

	/* Ready */
	override fun onReady(engine:GameEngine, playerID:Int):Boolean {
		super.onReady(engine, playerID)

		if(engine.statc[0]==0&&netvsPlayerExist[playerID])
			if(netCurrentRoomInfo==null||!netCurrentRoomInfo!!.useMap) {
				// Fill the field with garbage
				engine.createFieldIfNeeded()
				fillGarbage(engine, playerID)

				// Update meter
				val remainLines = getRemainGarbageLines(engine, playerID)
				playerRemainLines[playerID] = remainLines
				engine.meterValue = remainLines*owner.receiver.getBlockGraphicsHeight(engine)
				engine.meterColor = GameEngine.METER_COLOR_GREEN
			} else {
				// Map game
				engine.createFieldIfNeeded()
				turnAllBlocksToGem(engine, playerID)
				playerStartGems[playerID] = engine.field!!.howManyGems
				playerRemainLines[playerID] = playerStartGems[playerID]
				engine.meterValue = owner.receiver.getMeterMax(engine)
				engine.meterColor = GameEngine.METER_COLOR_GREEN
			}
		return false
	}

	/** Get player's place
	 * @param engine GameEngine
	 * @param playerID Player ID
	 * @return Player's place
	 */
	private fun getNowPlayerPlace(engine:GameEngine, playerID:Int):Int {
		if(!netvsPlayerExist[playerID]||netvsPlayerDead[playerID]) return -1

		var place = 0

		for(i in 0 until players)
			if(i!=playerID&&netvsPlayerExist[i]&&!netvsPlayerDead[i]
				&&owner.engine[i].field!=null)
				if(playerRemainLines[playerID]>playerRemainLines[i])
					place++
				else if(playerRemainLines[playerID]==playerRemainLines[i]&&engine.field!!.highestBlockY<owner.engine[i].field!!.highestBlockY)
					place++

		return place
	}

	/** Update progress meter
	 * @param engine GameEngine
	 */
	private fun updateMeter(engine:GameEngine) {
		val playerID = engine.playerID
		var remainLines = 0

		if(netCurrentRoomInfo==null||!netCurrentRoomInfo!!.useMap) {
			// Normal game
			remainLines = playerRemainLines[playerID]
			engine.meterValue = remainLines*owner.receiver.getBlockGraphicsHeight(engine)
			engine.meterColor = GameEngine.METER_COLOR_GREEN
			if(remainLines<=14) engine.meterColor = GameEngine.METER_COLOR_YELLOW
			if(remainLines<=8) engine.meterColor = GameEngine.METER_COLOR_ORANGE
			if(remainLines<=4) engine.meterColor = GameEngine.METER_COLOR_RED
		} else if(engine.field!=null&&playerStartGems[playerID]>0) {
			// Map game
			remainLines = engine.field!!.howManyGems-engine.field!!.howManyGemClears
			engine.meterValue = remainLines*owner.receiver.getMeterMax(engine)/playerStartGems[playerID]
			engine.meterColor = GameEngine.METER_COLOR_GREEN
			if(remainLines<=playerStartGems[playerID]/2) engine.meterColor = GameEngine.METER_COLOR_YELLOW
			if(remainLines<=playerStartGems[playerID]/3) engine.meterColor = GameEngine.METER_COLOR_ORANGE
			if(remainLines<=playerStartGems[playerID]/4) engine.meterColor = GameEngine.METER_COLOR_RED
		}
	}

	override fun calcScore(engine:GameEngine, playerID:Int, lines:Int) {
		if(lines>0&&playerID==0) {
			if(netCurrentRoomInfo==null||!netCurrentRoomInfo!!.useMap)
				playerRemainLines[playerID] = getRemainGarbageLines(engine, playerID)
			else if(engine.field!=null)
				playerRemainLines[playerID] = engine.field!!.howManyGems-engine.field!!.howManyGemClears
			updateMeter(engine)

			// Game Completed
			if(playerRemainLines[playerID]<=0)
				if(netvsIsPractice) {
					engine.stat = GameEngine.Status.EXCELLENT
					engine.resetStatc()
				} else {
					// Send game end message
					val places = IntArray(NetDummyVSMode.NETVS_MAX_PLAYERS)
					val uidArray = IntArray(NetDummyVSMode.NETVS_MAX_PLAYERS)
					for(i in 0 until players) {
						places[i] = getNowPlayerPlace(owner.engine[i], i)
						uidArray[i] = -1
					}
					for(i in 0 until players)
						if(places[i]>=0&&places[i]<NetDummyVSMode.NETVS_MAX_PLAYERS) uidArray[places[i]] = netvsPlayerUID[i]

					val strMsg = StringBuilder("racewin")
					for(i in 0 until players)
						if(uidArray[i]!=-1) strMsg.append("\t").append(uidArray[i])
					strMsg.append("\n")
					netLobby!!.netPlayerClient!!.send(strMsg.toString())

					// Wait until everyone dies
					engine.stat = GameEngine.Status.NOTHING
					engine.resetStatc()
				}
		}
	}

	/* Drawing processing at the end of every frame */
	override fun renderLast(engine:GameEngine, playerID:Int) {
		super.renderLast(engine, playerID)

		val x = owner.receiver.getFieldDisplayPositionX(engine, playerID)
		val y = owner.receiver.getFieldDisplayPositionY(engine, playerID)

		if(netvsPlayerExist[playerID]&&engine.isVisible) {
			if((netvsIsGameActive||netvsIsPractice&&playerID==0)&&engine.stat!=GameEngine.Status.RESULT) {
				// Lines left
				val remainLines = maxOf(0, playerRemainLines[playerID])
				var fontColor = EventReceiver.COLOR.WHITE
				if(remainLines in 1..14) fontColor = EventReceiver.COLOR.YELLOW
				if(remainLines in 1..8) fontColor = EventReceiver.COLOR.ORANGE
				if(remainLines in 1..4) fontColor = EventReceiver.COLOR.RED

				val strLines = remainLines.toString()

				when {
					engine.displaysize!=-1 -> when {
						strLines.length==1 -> owner.receiver.drawMenuFont(engine, playerID, 4, 21, strLines, fontColor, 2f)
						strLines.length==2 -> owner.receiver.drawMenuFont(engine, playerID, 3, 21, strLines, fontColor, 2f)
						strLines.length==3 -> owner.receiver.drawMenuFont(engine, playerID, 2, 21, strLines, fontColor, 2f)
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
						0 -> owner.receiver.drawMenuFont(engine, playerID, -2, 22, "1ST", EventReceiver.COLOR.ORANGE)
						1 -> owner.receiver.drawMenuFont(engine, playerID, -2, 22, "2ND", EventReceiver.COLOR.WHITE)
						2 -> owner.receiver.drawMenuFont(engine, playerID, -2, 22, "3RD", EventReceiver.COLOR.RED)
						3 -> owner.receiver.drawMenuFont(engine, playerID, -2, 22, "4TH", EventReceiver.COLOR.GREEN)
						4 -> owner.receiver.drawMenuFont(engine, playerID, -2, 22, "5TH", EventReceiver.COLOR.BLUE)
						5 -> owner.receiver.drawMenuFont(engine, playerID, -2, 22, "6TH", EventReceiver.COLOR.PURPLE)
					}
					place==0 -> owner.receiver.drawDirectFont(x, y+168, "1ST", EventReceiver.COLOR.ORANGE, .5f)
					place==1 -> owner.receiver.drawDirectFont(x, y+168, "2ND", EventReceiver.COLOR.WHITE, .5f)
					place==2 -> owner.receiver.drawDirectFont(x, y+168, "3RD", EventReceiver.COLOR.RED, .5f)
					place==3 -> owner.receiver.drawDirectFont(x, y+168, "4TH", EventReceiver.COLOR.GREEN, .5f)
					place==4 -> owner.receiver.drawDirectFont(x, y+168, "5TH", EventReceiver.COLOR.BLUE, .5f)
					place==5 -> owner.receiver.drawDirectFont(x, y+168, "6TH", EventReceiver.COLOR.PURPLE, .5f)
				}
			} else if(!netvsIsPractice||playerID!=0) {
				val strTemp = netvsPlayerWinCount[playerID].toString()+"/"+netvsPlayerPlayCount[playerID]

				if(engine.displaysize!=-1) {
					var y2 = 21
					if(engine.stat==GameEngine.Status.RESULT) y2 = 22
					owner.receiver.drawMenuFont(engine, playerID, 0, y2, strTemp, EventReceiver.COLOR.WHITE)
				} else
					owner.receiver.drawDirectFont(x+4, y+168, strTemp, EventReceiver.COLOR.WHITE, .5f)
			}// Games count
		}
	}

	/* Render results screen */
	override fun renderResult(engine:GameEngine, playerID:Int) {
		super.renderResult(engine, playerID)

		var scale = 1f
		if(engine.displaysize==-1) scale = .5f

		drawResultScale(engine, playerID, owner.receiver, 2, EventReceiver.COLOR.ORANGE, scale, "LINE", String.format("%10d", engine.statistics.lines), "PIECE", String.format("%10d", engine.statistics.totalPieceLocked), "LINE/MIN", String.format("%10g", engine.statistics.lpm), "PIECE/SEC", String.format("%10g", engine.statistics.pps), "TIME", String.format("%10s", GeneralUtil.getTime(engine.statistics.time.toFloat())))
	}

	/* Send stats */
	override fun netSendStats(engine:GameEngine) {
		val playerID = engine.playerID

		if(playerID==0&&!netvsIsPractice&&!netvsIsWatch()) {
			val remainLines = playerRemainLines[playerID]
			val strMsg = "game\tstats\t$remainLines\n"
			netLobby!!.netPlayerClient!!.send(strMsg)
		}
	}

	/* Receive stats */
	override fun netRecvStats(engine:GameEngine, message:Array<String>) {
		val playerID = engine.playerID
		if(message.size>4) playerRemainLines[playerID] = Integer.parseInt(message[4])
		updateMeter(engine)
	}

	/* Send end-of-game stats */
	override fun netSendEndGameStats(engine:GameEngine) {
		val playerID = engine.playerID
		var msg = "gstat\t"
		msg += netvsPlayerPlace[playerID].toString()+"\t"
		msg += 0.toString()+"\t"+0+"\t"+0+"\t"
		msg += engine.statistics.lines.toString()+"\t"+engine.statistics.lpm+"\t"
		msg += engine.statistics.totalPieceLocked.toString()+"\t"+engine.statistics.pps+"\t"
		msg += netvsPlayTimer.toString()+"\t"+0+"\t"+netvsPlayerWinCount[playerID]+"\t"+netvsPlayerPlayCount[playerID]
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
