package mu.nu.nullpo.game.subsystem.mode

import mu.nu.nullpo.game.component.*
import mu.nu.nullpo.game.event.EventReceiver
import mu.nu.nullpo.game.event.EventReceiver.COLOR
import mu.nu.nullpo.game.net.*
import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.game.play.GameManager
import mu.nu.nullpo.gui.net.NetLobbyFrame
import mu.nu.nullpo.gui.net.NetLobbyListener
import mu.nu.nullpo.util.CustomProperties
import mu.nu.nullpo.util.GeneralUtil
import org.apache.log4j.Logger
import java.io.IOException
import java.util.*
import java.util.zip.Adler32

/** Special base class for netplay */
open class NetDummyMode:AbstractMode(), NetLobbyListener {

	/** NET: Lobby (Declared in NetDummyMode) */
	protected var netLobby:NetLobbyFrame? = null

	/** NET: true if netplay (Declared in NetDummyMode) */
	internal var netIsNetPlay:Boolean = false

	/** NET: true if watch mode (Declared in NetDummyMode) */
	protected var netIsWatch:Boolean = false

	/** NET: Current room info. Sometimes null. (Declared in NetDummyMode) */
	internal var netCurrentRoomInfo:NetRoomInfo? = null

	/** NET: Number of spectators (Declared in NetDummyMode) */
	protected var netNumSpectators:Int = 0

	/** NET: Send all movements even if there are no spectators */
	internal var netForceSendMovements:Boolean = false

	/** NET: Previous piece informations (Declared in NetDummyMode) */
	private var netPrevPieceID:Int = 0
	private var netPrevPieceX:Int = 0
	private var netPrevPieceY:Int = 0
	private var netPrevPieceDir:Int = 0

	/** NET: The skin player using (Declared in NetDummyMode) */
	private var netPlayerSkin:Int = 0

	/** NET: If true, NetDummyMode will always send attributes when sending the
	 * field (Declared in NetDummyMode) */
	internal var netAlwaysSendFieldAttributes:Boolean = false

	/** NET: Player name (Declared in NetDummyMode) */
	protected var netPlayerName:String? = null

	/** NET: Replay send status (0:Before Send 1:Sending 2:Sent) (Declared in
	 * NetDummyMode) */
	protected var netReplaySendStatus:Int = 0

	/** NET: Current round's online ranking rank (Declared in NetDummyMode) */
	protected var netRankingRank:IntArray = intArrayOf(-1, -1)

	/** NET: True if new personal record (Declared in NetDummyMode) */
	protected var netIsPB:Boolean = false

	/** NET: True if net ranking display mode (Declared in NetDummyMode) */
	protected var netIsNetRankingDisplayMode:Boolean = false

	/** NET: Net ranking cursor position (Declared in NetDummyMode) */
	private var netRankingCursor:IntArray = IntArray(0)

	/** NET: Net ranking player's current rank (Declared in NetDummyMode) */
	private var netRankingMyRank:IntArray = IntArray(0)

	/** NET: 0 if viewing all-time ranking, 1 if viewing daily ranking (Declared
	 * in NetDummyMode) */
	private var netRankingView:Int = 0

	/** NET: Net ranking type (Declared in NetDummyMode) */
	private var netRankingType:Int = 0

	/** NET: True if no data is present. [0] for all-time and [1] for daily.
	 * (Declared in NetDummyMode) */
	private var netRankingNoDataFlag:BooleanArray = BooleanArray(0)

	/** NET: True if loading is complete. [0] for all-time and [1] for daily.
	 * (Declared in NetDummyMode) */
	private var netRankingReady:BooleanArray = BooleanArray(0)

	/** NET: Net Rankings' rank (Declared in NetDummyMode) */
	protected var netRankingPlace:Array<LinkedList<Int>> = emptyArray()

	/** NET: Net Rankings' names (Declared in NetDummyMode) */
	protected var netRankingName:Array<LinkedList<String>> = emptyArray()

	/** NET: Net Rankings' timestamps (Declared in NetDummyMode) */
	protected var netRankingDate:Array<LinkedList<Calendar>> = emptyArray()

	/** NET: Net Rankings' gamerates (Declared in NetDummyMode) */
	protected var netRankingGamerate:Array<LinkedList<Float>> = emptyArray()

	/** NET: Net Rankings' times (Declared in NetDummyMode) */
	protected var netRankingTime:Array<LinkedList<Int>> = emptyArray()

	/** NET: Net Rankings' score (Declared in NetDummyMode) */
	protected var netRankingScore:Array<LinkedList<Int>> = emptyArray()

	/** NET: Net Rankings' piece counts (Declared in NetDummyMode) */
	protected var netRankingPiece:Array<LinkedList<Int>> = emptyArray()

	/** NET: Net Rankings' PPS values (Declared in NetDummyMode) */
	protected var netRankingPPS:Array<LinkedList<Float>> = emptyArray()

	/** NET: Net Rankings' line counts (Declared in NetDummyMode) */
	protected var netRankingLines:Array<LinkedList<Int>> = emptyArray()

	/** NET: Net Rankings' digged depth (Declared in NetDummyMode) */
	protected var netRankingDepth:Array<LinkedList<Int>> = emptyArray()

	/** NET: Net Rankings' score/line (Declared in NetDummyMode) */
	protected var netRankingSPL:Array<LinkedList<Double>> = emptyArray()

	/** NET: Net Rankings' roll completed flag (Declared in NetDummyMode) */
	protected var netRankingRollclear:Array<LinkedList<Int>> = emptyArray()

	/* NET: Mode name */
	override val name:String
		get() = "NETWORK MODE"

	/** NET: Netplay Initialization. NetDummyMode will set the lobby's current
	 * mode to this. */
	override fun netplayInit(obj:NetLobbyFrame) {
		netLobby = obj
		netLobby!!.netDummyMode = this

		try {
			netLobby!!.ruleOptPlayer = RuleOptions(owner.engine[0].ruleopt)
		} catch(e:NullPointerException) {
			log.error("NPE on netplayInit; Most likely the mode is overriding 'owner' variable", e)
		}

		netLobby?.let {
			if(it.netPlayerClient!=null&&it.netPlayerClient!!.currentRoomInfo!=null)
				netOnJoin(it, it.netPlayerClient, it.netPlayerClient!!.currentRoomInfo)
		}
	}

	/** NET: Netplay Unload. NetDummyMode will set the lobby's current mode to
	 * null. */
	override fun netplayUnload(obj:NetLobbyFrame) {
		if(netLobby!=null) {
			netLobby!!.netDummyMode = null
			netLobby = null
		}
	}

	/** NET: Mode Initialization. NetDummyMode will set the "owner" variable. */
	override fun modeInit(manager:GameManager) {
		log.debug("modeInit() on NetDummyMode")
		owner = manager
		receiver = manager.receiver
		menuTime = 0
		netIsNetPlay = false
		netIsWatch = false
		netNumSpectators = 0
		netForceSendMovements = false
		netPlayerName = ""
		netRankingCursor = IntArray(2)
		netRankingMyRank = IntArray(2)
		netRankingView = 0
		netRankingNoDataFlag = BooleanArray(2)
		netRankingReady = BooleanArray(2)

		netRankingPlace = Array(2) {LinkedList<Int>()}
		netRankingName = Array(2) {LinkedList<String>()}
		netRankingDate = Array(2) {LinkedList<Calendar>()}
		netRankingGamerate = Array(2) {LinkedList<Float>()}
		netRankingTime = Array(2) {LinkedList<Int>()}
		netRankingScore = Array(2) {LinkedList<Int>()}
		netRankingPiece = Array(2) {LinkedList<Int>()}
		netRankingPPS = Array(2) {LinkedList<Float>()}
		netRankingLines = Array(2) {LinkedList<Int>()}
		netRankingDepth = Array(2) {LinkedList<Int>()}
		netRankingSPL = Array(2) {LinkedList<Double>()}
		netRankingRollclear = Array(2) {LinkedList<Int>()}
	}

	/** NET: Initialization for each player.
	 * NetDummyMode will stop and hide all players.
	 * Call netPlayerInit if you want to init NetPlay variables. */
	override fun playerInit(engine:GameEngine, playerID:Int) {
		engine.stat = GameEngine.Status.NOTHING
		engine.isVisible = false
	}

	/** NET: Initialize various NetPlay variables. Usually called from
	 * playerInit.
	 * @param engine GameEngine
	 * @param playerID Player ID
	 */
	protected open fun netPlayerInit(engine:GameEngine, playerID:Int) {
		netPrevPieceID = Piece.PIECE_NONE
		netPrevPieceX = 0
		netPrevPieceY = 0
		netPrevPieceDir = 0
		netPlayerSkin = 0
		netReplaySendStatus = 0
		netRankingRank = IntArray(2)
		netRankingRank[0] = -1
		netRankingRank[1] = -1
		netIsPB = false
		netIsNetRankingDisplayMode = false
		netAlwaysSendFieldAttributes = false
		if(netIsWatch) {
			engine.isNextVisible = false
			engine.isHoldVisible = false
		}

		engine.stat = GameEngine.Status.SETTING
		engine.isVisible = true
	}

	/** NET: When the pieces can move.
	 * NetDummyMode will send field/next/stats/piece movements. */
	override fun onMove(engine:GameEngine, playerID:Int):Boolean {
		// NET: Send field, next, and stats
		if(engine.ending==0&&engine.statc[0]==0&&!engine.holdDisable&&
			netIsNetPlay&&!netIsWatch&&(netNumSpectators>0||netForceSendMovements)) {
			netSendField(engine)
			netSendStats(engine)
		}
		// NET: Send piece movement
		if(engine.ending==0&&netIsNetPlay&&!netIsWatch&&engine.nowPieceObject!=null
			&&(netNumSpectators>0||netForceSendMovements))
			if(netSendPieceMovement(engine, false)) netSendNextAndHold(engine)
		// NET: Stop game in watch mode
		return netIsWatch

	}

	/** NET: When the piece locked. NetDummyMode will send field and stats. */
	override fun pieceLocked(engine:GameEngine, playerID:Int, lines:Int) {
		// NET: Send field and stats
		if(engine.ending==0&&netIsNetPlay&&!netIsWatch&&(netNumSpectators>0||netForceSendMovements)) {
			netSendField(engine)
			netSendStats(engine)
		}
	}

	/** NET: Line clear. NetDummyMode will send field and stats. */
	override fun onLineClear(engine:GameEngine, playerID:Int):Boolean {
		// NET: Send field and stats
		if(engine.statc[0]==1&&engine.ending==0&&netIsNetPlay&&!netIsWatch&&(netNumSpectators>0||netForceSendMovements)) {
			netSendField(engine)
			netSendStats(engine)
		}
		return false
	}

	/** NET: ARE. NetDummyMode will send field, next and stats. */
	override fun onARE(engine:GameEngine, playerID:Int):Boolean {
		// NET: Send field, next, and stats
		if(engine.statc[0]==0&&engine.ending==0&&netIsNetPlay&&!netIsWatch&&(netNumSpectators>0||netForceSendMovements)) {
			netSendField(engine)
			netSendNextAndHold(engine)
			netSendStats(engine)
		}
		return false
	}

	/** NET: Ending start. NetDummyMode will send ending start messages. */
	override fun onEndingStart(engine:GameEngine, playerID:Int):Boolean {
		if(menuCursor==0)
		// NET: Send game completed messages
			if(netIsNetPlay&&!netIsWatch&&(netNumSpectators>0||netForceSendMovements)) {
				netSendField(engine)
				netSendNextAndHold(engine)
				netSendStats(engine)
				netLobby!!.netPlayerClient!!.send("game\tending\n")
			}
		return false
	}

	/** NET: "Excellent!" screen */
	override fun onExcellent(engine:GameEngine, playerID:Int):Boolean {
		if(engine.statc[0]==0)
		// NET: Send game completed messages
			if(netIsNetPlay&&!netIsWatch&&(netNumSpectators>0||netForceSendMovements)) {
				netSendField(engine)
				netSendNextAndHold(engine)
				netSendStats(engine)
				netLobby!!.netPlayerClient!!.send("game\texcellent\n")
			}
		return false
	}

	/** NET: Game Over */
	override fun onGameOver(engine:GameEngine, playerID:Int):Boolean {
		// NET: Send messages / Wait for messages
		if(netIsNetPlay)
			if(!netIsWatch) {
				if(engine.statc[0]==0) {
					// Send end-of-game messages
					if(netNumSpectators>0||netForceSendMovements) {
						netSendField(engine)
						netSendNextAndHold(engine)
						netSendStats(engine)
					}
					netSendEndGameStats(engine)
					netLobby!!.netPlayerClient!!.send("dead\t-1\n")
				} else if(engine.statc[0]>=engine.field!!.height+1+180)
				// To results screen
					netLobby!!.netPlayerClient!!.send("game\tresultsscreen\n")
			} else if(engine.statc[0]<engine.field!!.height+1+180)
				return false
			else {
				engine.field!!.reset()
				engine.stat = GameEngine.Status.RESULT
				engine.resetStatc()
				return true
			}

		return false
	}

	/** NET: Results screen */
	override fun onResult(engine:GameEngine, playerID:Int):Boolean {
		/*
    BGMStatus.BGM b=BGM.RESULT(0);
    if(engine.ending>=2)b=engine.statistics.time<10800?BGM.RESULT(1):BGM.RESULT(2);
    owner.bgmStatus.fadesw=false;
    owner.bgmStatus.bgm=b; */
		// NET: Retry
		if(netIsNetPlay) {
			engine.allowTextRenderByReceiver = false

			// Replay Send
			if(netIsWatch||owner.replayMode)
				netReplaySendStatus = 2
			else if(netReplaySendStatus==0) {
				netReplaySendStatus = 1
				netSendReplay(engine)
			}

			// Retry
			if(engine.ctrl!!.isPush(Controller.BUTTON_A)&&!netIsWatch&&netReplaySendStatus==2) {
				engine.playSE("decide")
				if(netNumSpectators>0||netForceSendMovements) {
					netLobby!!.netPlayerClient!!.send("game\tretry\n")
					netSendOptions(engine)
				}
				owner.reset()
			}

			return true
		}

		return false
	}

	/** NET: Render something such as HUD. NetDummyMode will render the number
	 * of players to bottom-right of the screen. */
	override fun renderLast(engine:GameEngine, playerID:Int) {
		// NET: Number of spectators
		netDrawSpectatorsCount(engine, 0, 20)
		// NET: All number of players
		if(playerID==players-1) {
			netDrawAllPlayersCount()
			netDrawGameRate(engine)
		}
		// NET: Player name (It may also appear in offline replay)
		netDrawPlayerName(engine)
	}

	/** NET: Update menu cursor. NetDummyMode will signal cursor movement to all
	 * spectators. */
	override fun updateCursor(engine:GameEngine, maxCursor:Int, playerID:Int):Int {
		// NET: Don't execute in watch mode
		if(netIsWatch) return 0

		val change = super.updateCursor(engine, maxCursor, playerID)

		// NET: Signal cursor change
		if((engine.ctrl!!.isMenuRepeatKey(Controller.BUTTON_UP)||engine.ctrl!!.isMenuRepeatKey(Controller.BUTTON_DOWN))&&
			netIsNetPlay&&(netNumSpectators>0||netForceSendMovements))
			netLobby!!.netPlayerClient!!.send("game\tcursor\t"
				+menuCursor+"\n")

		return change
	}

	/** NET: Retry key */
	override fun netplayOnRetryKey(engine:GameEngine, playerID:Int) {
		if(netIsNetPlay&&!netIsWatch) {
			owner.reset()
			netLobby!!.netPlayerClient!!.send("reset1p\n")
			netSendOptions(engine)
		}
	}

	/** NET: Initialization Completed (Never called) */
	override fun netlobbyOnInit(lobby:NetLobbyFrame) {}

	/** NET: Login completed (Never called) */
	override fun netlobbyOnLoginOK(lobby:NetLobbyFrame, client:NetPlayerClient) {}

	/** NET: When you enter a room (Never called) */
	override fun netlobbyOnRoomJoin(lobby:NetLobbyFrame, client:NetPlayerClient, roomInfo:NetRoomInfo) {}

	/** NET: When you returned to lobby (Never called) */
	override fun netlobbyOnRoomLeave(lobby:NetLobbyFrame, client:NetPlayerClient) {}

	/* NET: When disconnected */
	override fun netlobbyOnDisconnect(lobby:NetLobbyFrame, client:NetPlayerClient, ex:Throwable?) {}

	/* NET: Message received */
	@Throws(IOException::class)
	override fun netlobbyOnMessage(lobby:NetLobbyFrame, client:NetPlayerClient, message:Array<String>) {
		// Player status update
		if(message[0]=="playerupdate") netUpdatePlayerExist()
		// When someone logout
		if(message[0]=="playerlogout") {
			val pInfo = NetPlayerInfo(message[1])

			if(netCurrentRoomInfo!=null&&pInfo.roomID==netCurrentRoomInfo!!.roomID) netUpdatePlayerExist()
		}
		// Game started
		if(message[0]=="start") {
			log.debug("NET: Game started")

			if(netIsWatch) {
				owner.reset()
				owner.engine[0].stat = GameEngine.Status.READY
				owner.engine[0].resetStatc()
			}
		}
		// Dead
		if(message[0]=="dead") {
			log.debug("NET: Dead")

			if(netIsWatch) {
				owner.engine[0].gameEnded()

				if(owner.engine[0].stat!=GameEngine.Status.GAMEOVER&&owner.engine[0].stat!=GameEngine.Status.RESULT) {
					owner.engine[0].stat = GameEngine.Status.GAMEOVER
					owner.engine[0].resetStatc()
				}
			}
		}
		// Replay send fail
		if(message[0]=="spsendng") {
			netReplaySendStatus = 1
			netSendReplay(owner.engine[0])
		}
		// Replay send complete
		if(message[0]=="spsendok") {
			netReplaySendStatus = 2
			netRankingRank[0] = Integer.parseInt(message[1])
			netIsPB = java.lang.Boolean.parseBoolean(message[2])
			netRankingRank[1] = Integer.parseInt(message[3])
		}
		// Netplay Ranking
		if(message[0]=="spranking") netRecvNetPlayRanking(message)
		// Reset
		if(message[0]=="reset1p") if(netIsWatch) owner.reset()
		// Game messages
		if(message[0]=="game")
			if(netIsWatch) {
				val engine = owner.engine[0]
				if(engine.field==null) engine.field = Field()

				// Move cursor
				if(message[3]=="cursor")
					if(engine.stat==GameEngine.Status.SETTING) {
						menuCursor = Integer.parseInt(message[4])
						engine.playSE("cursor")
					}
				// Change game options
				if(message[3]=="option") netRecvOptions(engine, message)
				// Field
				if(message[3]=="field"||message[3]=="fieldattr") netRecvField(engine, message)
				// Stats
				if(message[3]=="stats") netRecvStats(engine, message)
				// Current Piece
				if(message[3]=="piece") netRecvPieceMovement(engine, message)
				// Next and Hold
				if(message[3]=="next") netRecvNextAndHold(engine, message)
				// Ending
				if(message[3]=="ending") {
					engine.ending = 1
					if(!engine.staffrollEnable) engine.gameEnded()
					engine.stat = GameEngine.Status.ENDINGSTART
					engine.resetStatc()
				}
				// Excellent
				if(message[3]=="excellent") {
					engine.stat = GameEngine.Status.EXCELLENT
					engine.resetStatc()
				}
				// Retry
				if(message[3]=="retry") {
					engine.ending = 0
					engine.gameEnded()
					engine.stat = GameEngine.Status.SETTING
					engine.resetStatc()
					engine.playSE("decide")
				}
				// Display results screen
				if(message[3]=="resultsscreen") {
					engine.field!!.reset()
					engine.stat = GameEngine.Status.RESULT
					engine.resetStatc()
				}
			}
	}

	/* NET: When the lobby window is closed */
	override fun netlobbyOnExit(lobby:NetLobbyFrame) {
		try {
			for(i in owner.engine.indices)
				owner.engine[i].quitflag = true
		} catch(e:Exception) {
		}

	}

	/** NET: When you join the room
	 * @param lobby NetLobbyFrame
	 * @param client NetPlayerClient
	 * @param roomInfo NetRoomInfo
	 */
	protected open fun netOnJoin(lobby:NetLobbyFrame, client:NetPlayerClient?, roomInfo:NetRoomInfo?) {
		log.debug("onJoin on NetDummyMode")

		netCurrentRoomInfo = roomInfo
		netIsNetPlay = true
		netIsWatch = netLobby!!.netPlayerClient!!.yourPlayerInfo!!.seatID==-1
		netNumSpectators = 0
		netUpdatePlayerExist()

		if(netIsWatch) {
			owner.engine[0].isNextVisible = false
			owner.engine[0].isHoldVisible = false
		}

		if(roomInfo!=null&&roomInfo.ruleLock) netLobby?.ruleOptLock?.let {
			// Set to locked rule
			log.info("Set locked rule")
			val randomizer = GeneralUtil.loadRandomizer(it.strRandomizer)
			val wallkick = GeneralUtil.loadWallkick(it.strWallkick)
			owner.engine[0].ruleopt.copy(it)
			owner.engine[0].randomizer = randomizer
			owner.engine[0].wallkick = wallkick
			loadRanking(owner.modeConfig, owner.engine[0].ruleopt.strRuleName)
		}
	}

	/** NET: Read rankings from property file. This is used from netOnJoin.
	 * @param prop Property file
	 * @param ruleName Rule name
	 */
	protected open fun loadRanking(prop:CustomProperties, ruleName:String) {}

	/** NET: Update player count */
	protected open fun netUpdatePlayerExist() {
		netNumSpectators = 0
		netPlayerName = ""

		if(netCurrentRoomInfo!=null&&netCurrentRoomInfo!!.roomID!=-1
			&&netLobby!=null)
			for(pInfo in netLobby!!.updateSameRoomPlayerInfoList())
				if(pInfo.roomID==netCurrentRoomInfo!!.roomID)
					if(pInfo.seatID==0)
						netPlayerName = pInfo.strName
					else if(pInfo.seatID==-1) netNumSpectators++
	}

	/** NET: Draw number of players to bottom-right of screen.
	 * This subroutine uses "netLobby" and "owner" variables. */
	private fun netDrawAllPlayersCount() {
		if(netLobby!=null&&netLobby!!.netPlayerClient!=null&&netLobby!!.netPlayerClient!!.isConnected) {
			var fontcolor = COLOR.BLUE
			if(netLobby!!.netPlayerClient!!.observerCount>0) fontcolor = COLOR.GREEN
			if(netLobby!!.netPlayerClient!!.playerCount>1) fontcolor = COLOR.RED
			val strObserverInfo =
				String.format("%d/%d", netLobby!!.netPlayerClient!!.observerCount, netLobby!!.netPlayerClient!!.playerCount)
			val strObserverString = String.format("%40s", strObserverInfo)
			owner.receiver.drawDirectFont(0, 480-16, strObserverString, fontcolor)
		}
	}

	/** NET: Draw game-rate to bottom-right of screen.
	 * @param engine GameEngine
	 */
	private fun netDrawGameRate(engine:GameEngine) {
		if(netIsNetPlay&&!netIsWatch&&engine.gameStarted&&engine.startTime!=0L) {
			val gamerate:Float
			gamerate = if(engine.endTime!=0L)
				engine.statistics.gamerate
			else {
				val nowtime = System.nanoTime()
				(engine.replayTimer/(0.00000006*(nowtime-engine.startTime))).toFloat()
			}

			val strTemp = String.format("%.0f%%", gamerate*100f)
			val strTemp2 = String.format("%40s", strTemp)

			var fontcolor = COLOR.BLUE
			if(gamerate<1f) fontcolor = COLOR.YELLOW
			if(gamerate<.9f) fontcolor = COLOR.ORANGE
			if(gamerate<.8f) fontcolor = COLOR.RED
			owner.receiver.drawDirectFont(0, 480-32, strTemp2, fontcolor)
		}
	}

	/** NET: Draw spectator count in score area.
	 * @param engine GameEngine
	 * @param x X offset
	 * @param y Y offset
	 */
	private fun netDrawSpectatorsCount(engine:GameEngine, x:Int, y:Int) {
		if(netIsNetPlay) {
			val fontcolor = if(netIsWatch) COLOR.GREEN else COLOR.RED
			owner.receiver.drawScoreFont(engine, engine.playerID, x, y, "SPECTATORS", fontcolor)
			owner.receiver.drawScoreFont(engine, engine.playerID, x, y+1, ""+netNumSpectators, COLOR.WHITE)

			if(engine.stat==GameEngine.Status.SETTING&&!netIsWatch&&netIsNetRankingViewOK(engine)) {
				var y2 = y+2
				if(y2>24) y2 = 24
				val strBtnD = engine.owner.receiver.getKeyNameByButtonID(engine, Controller.BUTTON_D)
				owner.receiver.drawScoreFont(engine, engine.playerID, x, y2,
					"D($strBtnD KEY):\n NET RANKING", COLOR.GREEN)
			}
		}
	}

	/** NET: Draw player's name. It may also appear in offline replay.
	 * @param engine GameEngine
	 */
	protected open fun netDrawPlayerName(engine:GameEngine) {
		if(netPlayerName!=null&&netPlayerName!!.isNotEmpty()) {
			val name = netPlayerName
			owner.receiver.drawDirectTTF(owner.receiver.getFieldDisplayPositionX(engine, engine.playerID),
				owner.receiver.getFieldDisplayPositionY(engine, engine.playerID)-20, name!!)
		}
	}

	/** NET: Send the current piece's movement to all spectators.
	 * @param engine GameEngine
	 * @param forceSend `true` to force send a message
	 * (if `false`, it won't send a message unless there is a
	 * movement)
	 * @return `true` if the message is sent
	 */
	protected open fun netSendPieceMovement(engine:GameEngine, forceSend:Boolean):Boolean {
		engine.nowPieceObject?.let {
			if(it.id!=netPrevPieceID||engine.nowPieceX!=netPrevPieceX||
				engine.nowPieceY!=netPrevPieceY||it.direction!=netPrevPieceDir||forceSend) {
				netPrevPieceID = it.id
				netPrevPieceX = engine.nowPieceX
				netPrevPieceY = engine.nowPieceY
				netPrevPieceDir = it.direction

				val x = netPrevPieceX+it.dataOffsetX[netPrevPieceDir]
				val y = netPrevPieceY+it.dataOffsetY[netPrevPieceDir]
				netLobby!!.netPlayerClient!!.send("game\tpiece\t"+netPrevPieceID+"\t"+x+"\t"+y+"\t"+netPrevPieceDir+"\t"+
					engine.nowPieceBottomY+"\t"+engine.ruleopt.pieceColor[netPrevPieceID]+"\t"+engine.skin+"\t"+
					it.big+"\n")
				return true
			}
		} ?: if(netPrevPieceID!=Piece.PIECE_NONE||engine.manualLock) {
			netPrevPieceID = Piece.PIECE_NONE
			netLobby!!.netPlayerClient!!.send("game\tpiece\t"+netPrevPieceID+"\t"+netPrevPieceX+"\t"+netPrevPieceY+"\t"
				+netPrevPieceDir+"\t"+0+"\t"+engine.skin+"\t"+false+"\n")
			return true
		}
		return false
	}

	/** NET: Receive the current piece's movement. You can override it if you
	 * customize "piece" message.
	 * @param engine GameEngine
	 * @param message Message array
	 */
	internal fun netRecvPieceMovement(engine:GameEngine, message:Array<String>) {
		val id = Integer.parseInt(message[4])

		if(id>=0) {
			val pieceX = Integer.parseInt(message[5])
			val pieceY = Integer.parseInt(message[6])
			val pieceDir = Integer.parseInt(message[7])
			//int pieceBottomY = Integer.parseInt(message[8]);
			val pieceColor = Integer.parseInt(message[9])
			val pieceSkin = Integer.parseInt(message[10])
			val pieceBig = message.size>11&&java.lang.Boolean.parseBoolean(message[11])

			engine.nowPieceObject = Piece(id).apply {
				direction = pieceDir
				setAttribute(true, Block.ATTRIBUTE.VISIBLE)
				setColor(pieceColor)
				setSkin(pieceSkin)
				big = pieceBig
				updateConnectData()
			}
			engine.nowPieceX = pieceX
			engine.nowPieceY = pieceY
			//engine.nowPieceBottomY = pieceBottomY;
			engine.nowPieceBottomY = engine.nowPieceObject!!.getBottom(pieceX, pieceY, engine.field)

			if(engine.stat!=GameEngine.Status.EXCELLENT&&engine.stat!=GameEngine.Status.GAMEOVER&&
				engine.stat!=GameEngine.Status.RESULT) {
				engine.gameActive = true
				engine.timerActive = true
				engine.stat = GameEngine.Status.MOVE
				engine.statc[0] = 2
			}

			netPlayerSkin = pieceSkin
		} else
			engine.nowPieceObject = null
	}

	/** NET: Send field to all spectators
	 * @param engine GameEngine
	 */
	protected open fun netSendField(engine:GameEngine) {
		if(owner.receiver.isStickySkin(engine)||netAlwaysSendFieldAttributes) {
			// Send with attributes
			val strSrcFieldData = engine.field!!.attrFieldToString()
			val nocompSize = strSrcFieldData.length

			val strCompFieldData = NetUtil.compressString(strSrcFieldData)
			val compSize = strCompFieldData.length

			var strFieldData = strSrcFieldData
			var isCompressed = false
			if(compSize<nocompSize) {
				strFieldData = strCompFieldData
				isCompressed = true
			}

			var msg = "game\tfieldattr\t"
			msg += engine.skin.toString()+"\t"
			msg += strFieldData+"\t"+isCompressed+"\n"
			netLobby!!.netPlayerClient!!.send(msg)
		} else {
			// Send without attributes
			val strSrcFieldData = engine.field!!.fieldToString()
			val nocompSize = strSrcFieldData.length

			val strCompFieldData = NetUtil.compressString(strSrcFieldData)
			val compSize = strCompFieldData.length

			var strFieldData = strSrcFieldData
			var isCompressed = false
			if(compSize<nocompSize) {
				strFieldData = strCompFieldData
				isCompressed = true
			}

			var msg = "game\tfield\t"
			msg += engine.skin.toString()+"\t"
			msg += engine.field!!.heightWithoutHurryupFloor.toString()+"\t"
			msg += strFieldData+"\t"+isCompressed+"\n"
			netLobby!!.netPlayerClient!!.send(msg)
		}
	}

	/** NET: Receive field message
	 * @param engine GameEngine
	 * @param message Message array
	 */
	protected open fun netRecvField(engine:GameEngine, message:Array<String>) {
		if(message[3]=="fieldattr") {
			// With attributes
			if(message.size>4) {
				engine.nowPieceObject = null
				engine.holdDisable = false
				if(engine.stat==GameEngine.Status.SETTING) engine.stat = GameEngine.Status.MOVE
				val skin = Integer.parseInt(message[4])
				netPlayerSkin = skin
				if(message.size>6) {
					val isCompressed = java.lang.Boolean.parseBoolean(message[6])
					var strFieldData = message[5]
					if(isCompressed) strFieldData = NetUtil.decompressString(strFieldData)
					engine.field!!.attrStringToField(strFieldData, skin)
				}
			}
		} else // Without attributes
			if(message.size>5) {
				engine.nowPieceObject = null
				engine.holdDisable = false
				if(engine.stat==GameEngine.Status.SETTING) engine.stat = GameEngine.Status.MOVE
				val skin = Integer.parseInt(message[4])
				val highestWallY = Integer.parseInt(message[5])
				netPlayerSkin = skin
				if(message.size>7) {
					var strFieldData = message[6]
					val isCompressed = java.lang.Boolean.parseBoolean(message[7])
					if(isCompressed) strFieldData = NetUtil.decompressString(strFieldData)
					engine.field!!.stringToField(strFieldData, skin, highestWallY, highestWallY)
				} else
					engine.field!!.reset()
			}
	}

	/** NET: Send next and hold piece informations to all spectators
	 * @param engine GameEngine
	 */
	protected open fun netSendNextAndHold(engine:GameEngine) {
		var holdID = Piece.PIECE_NONE
		var holdDirection = Piece.DIRECTION_UP
		var holdColor = Block.BLOCK_COLOR_GRAY
		if(engine.holdPieceObject!=null) {
			holdID = engine.holdPieceObject!!.id
			holdDirection = engine.holdPieceObject!!.direction
			holdColor = engine.ruleopt.pieceColor[engine.holdPieceObject!!.id]
		}

		val msg = StringBuilder("game\tnext\t"+engine.ruleopt.nextDisplay+"\t"+engine.holdDisable+"\t")

		for(i in -1 until engine.ruleopt.nextDisplay) {
			if(i<0)
				msg.append(holdID).append(";").append(holdDirection).append(";").append(holdColor)
			else {
				val nextObj = engine.getNextObject(engine.nextPieceCount+i)
				msg.append(nextObj!!.id).append(";").append(nextObj.direction).append(";").append(engine.ruleopt.pieceColor[nextObj.id])
			}

			if(i<engine.ruleopt.nextDisplay-1) msg.append("\t")
		}

		msg.append("\n")
		netLobby!!.netPlayerClient!!.send(msg.toString())
	}

	/** NET: Receive next and hold piece informations
	 * @param engine GameEngine
	 * @param message Message array
	 */
	internal fun netRecvNextAndHold(engine:GameEngine, message:Array<String>) {
		val maxNext = Integer.parseInt(message[4])
		engine.ruleopt.nextDisplay = maxNext
		engine.holdDisable = java.lang.Boolean.parseBoolean(message[5])

		for(i in 0 until maxNext+1)
			if(i+6<message.size) {
				val strPieceData = message[i+6].split(";".toRegex()).dropLastWhile {it.isEmpty()}.toTypedArray()
				val pieceID = Integer.parseInt(strPieceData[0])
				val pieceDirection = Integer.parseInt(strPieceData[1])
				val pieceColor = Integer.parseInt(strPieceData[2])

				if(i==0) {
					if(pieceID==Piece.PIECE_NONE)
						engine.holdPieceObject = null
					else {
						engine.holdPieceObject = Piece(pieceID)
						engine.holdPieceObject!!.direction = pieceDirection
						engine.holdPieceObject!!.setColor(pieceColor)
						engine.holdPieceObject!!.setSkin(netPlayerSkin)
						engine.holdPieceObject!!.updateConnectData()
					}
				} else {
					if(engine.nextPieceArrayObject.size<maxNext)
						engine.nextPieceArrayObject = arrayOfNulls(maxNext)
					engine.nextPieceArrayObject[i-1] = Piece(pieceID).apply {
						direction = pieceDirection
						setColor(pieceColor)
						setSkin(netPlayerSkin)
						updateConnectData()
					}
				}
			}

		engine.isNextVisible = true
		engine.isHoldVisible = true
	}

	/** Menu routine for 1P NetPlay online ranking screen. Usually called from
	 * onSetting(engine, playerID).
	 * @param engine GameEngine
	 * @param goaltype Goal Type
	 */
	protected fun netOnUpdateNetPlayRanking(engine:GameEngine, goaltype:Int) {
		if(netIsNetRankingDisplayMode) {
			val d = netRankingView

			if(!netRankingNoDataFlag[d]&&netRankingReady[d]&&netRankingPlace!=null&&netRankingPlace[d]!=null) {
				// Up
				if(engine.ctrl!!.isMenuRepeatKey(Controller.BUTTON_UP)) {
					netRankingCursor[d]--
					if(netRankingCursor[d]<0) netRankingCursor[d] = netRankingPlace[d].size-1
					engine.playSE("cursor")
				}
				// Down
				if(engine.ctrl!!.isMenuRepeatKey(Controller.BUTTON_DOWN)) {
					netRankingCursor[d]++
					if(netRankingCursor[d]>netRankingPlace[d].size-1) netRankingCursor[d] = 0
					engine.playSE("cursor")
				}
				// Download
				if(engine.ctrl!!.isPush(Controller.BUTTON_A)) {
					engine.playSE("decide")
					val strMsg = "spdownload\t"+NetUtil.urlEncode(netCurrentRoomInfo!!.ruleName)+"\t"+
						NetUtil.urlEncode(name)+"\t"+goaltype+"\t"+
						(netRankingView!=0)+"\t"+NetUtil.urlEncode(netRankingName[d][netRankingCursor[d]])+"\n"
					netLobby!!.netPlayerClient!!.send(strMsg)
					netIsNetRankingDisplayMode = false
					owner.menuOnly = false
				}
			}

			// Left/Right
			if(engine.ctrl!!.isPush(Controller.BUTTON_LEFT)||engine.ctrl!!.isPush(Controller.BUTTON_RIGHT)) {
				netRankingView = if(netRankingView==0)
					1
				else
					0
				engine.playSE("change")
			}

			// Exit
			if(engine.ctrl!!.isPush(Controller.BUTTON_B)) {
				netIsNetRankingDisplayMode = false
				owner.menuOnly = false
			}
		}
	}

	/** Render 1P NetPlay online ranking screen. Usually called from
	 * renderSetting(engine, playerID).
	 * @param engine GameEngine
	 * @param playerID Player ID
	 * @param receiver EventReceiver
	 */
	protected fun netOnRenderNetPlayRanking(engine:GameEngine, playerID:Int, receiver:EventReceiver) {
		if(netIsNetRankingDisplayMode) {
			val strBtnA = receiver.getKeyNameByButtonID(engine, Controller.BUTTON_A)
			val strBtnB = receiver.getKeyNameByButtonID(engine, Controller.BUTTON_B)

			val d = netRankingView

			if(!netRankingNoDataFlag[d]&&netRankingReady[d]&&netRankingPlace!=null&&netRankingPlace[d]!=null) {
				receiver.drawMenuFont(engine, playerID, 0, 1, "<<", COLOR.ORANGE)
				receiver.drawMenuFont(engine, playerID, 38, 1, ">>", COLOR.ORANGE)
				receiver.drawMenuFont(engine, playerID, 3, 1, (if(d!=0) "DAILY" else "ALL-TIME")+" RANKING ("+(netRankingCursor[d]+1)+"/"+netRankingPlace[d].size+")", COLOR.GREEN)

				val startIndex = netRankingCursor[d]/20*20
				var endIndex = startIndex+20
				if(endIndex>netRankingPlace[d].size) endIndex = netRankingPlace[d].size
				var c = 0

				if(netRankingType==NetSPRecord.RANKINGTYPE_GENERIC_SCORE)
					receiver.drawMenuFont(engine, playerID, 1, 3, "    SCORE   LINE TIME     NAME", COLOR.BLUE)
				else if(netRankingType==NetSPRecord.RANKINGTYPE_GENERIC_TIME)
					receiver.drawMenuFont(engine, playerID, 1, 3, "    TIME     PIECE PPS    NAME", COLOR.BLUE)
				else if(netRankingType==NetSPRecord.RANKINGTYPE_SCORERACE)
					receiver.drawMenuFont(engine, playerID, 1, 3, "    TIME     LINE SPL    NAME", COLOR.BLUE)
				else if(netRankingType==NetSPRecord.RANKINGTYPE_DIGRACE)
					receiver.drawMenuFont(engine, playerID, 1, 3, "    TIME     LINE PIECE  NAME", COLOR.BLUE)
				else if(netRankingType==NetSPRecord.RANKINGTYPE_ULTRA)
					receiver.drawMenuFont(engine, playerID, 1, 3, "    SCORE   LINE PIECE    NAME", COLOR.BLUE)
				else if(netRankingType==NetSPRecord.RANKINGTYPE_COMBORACE)
					receiver.drawMenuFont(engine, playerID, 1, 3, "    COMBO TIME     PPS    NAME", COLOR.BLUE)
				else if(netRankingType==NetSPRecord.RANKINGTYPE_DIGCHALLENGE)
					receiver.drawMenuFont(engine, playerID, 1, 3, "    SCORE   LINE TIME     NAME", COLOR.BLUE)
				else if(netRankingType==NetSPRecord.RANKINGTYPE_TIMEATTACK)
					receiver.drawMenuFont(engine, playerID, 1, 3, "    LINE  TIME     PPS    NAME", COLOR.BLUE)

				for(i in startIndex until endIndex) {
					if(i==netRankingCursor[d])
						receiver.drawMenuFont(engine, playerID, 0, 4+c, "b", COLOR.RED)

					val rankColor = if(i==netRankingMyRank[d])
						COLOR.PINK
					else
						COLOR.YELLOW
					if(netRankingPlace[d][i]==-1)
						receiver.drawMenuFont(engine, playerID, 1, 4+c, "N/A", rankColor)
					else
						receiver.drawMenuNum(engine, playerID, 1, 4+c, String.format("%3d", netRankingPlace[d][i]+1), rankColor)

					if(netRankingType==NetSPRecord.RANKINGTYPE_GENERIC_SCORE) {
						receiver.drawMenuNum(engine, playerID, 5, 4+c, ""+netRankingScore[d][i], i==netRankingCursor[d])
						receiver.drawMenuNum(engine, playerID, 13, 4+c, ""+netRankingLines[d][i], i==netRankingCursor[d])
						receiver.drawMenuNum(engine, playerID, 18, 4+c, GeneralUtil.getTime(netRankingTime[d][i].toFloat()), i==netRankingCursor[d])
						receiver.drawMenuTTF(engine, playerID, 27, 4+c, netRankingName[d][i], i==netRankingCursor[d])
					} else if(netRankingType==NetSPRecord.RANKINGTYPE_GENERIC_TIME) {
						receiver.drawMenuNum(engine, playerID, 5, 4+c, GeneralUtil.getTime(netRankingTime[d][i].toFloat()), i==netRankingCursor[d])
						receiver.drawMenuNum(engine, playerID, 14, 4+c, ""+netRankingPiece[d][i], i==netRankingCursor[d])
						receiver.drawMenuNum(engine, playerID, 20, 4+c, String.format("%.5g", netRankingPPS[d][i]), i==netRankingCursor[d])
						receiver.drawMenuTTF(engine, playerID, 27, 4+c, netRankingName[d][i], i==netRankingCursor[d])
					} else if(netRankingType==NetSPRecord.RANKINGTYPE_SCORERACE) {
						receiver.drawMenuNum(engine, playerID, 5, 4+c, GeneralUtil.getTime(netRankingTime[d][i].toFloat()), i==netRankingCursor[d])
						receiver.drawMenuNum(engine, playerID, 14, 4+c, ""+netRankingLines[d][i], i==netRankingCursor[d])
						receiver.drawMenuNum(engine, playerID, 19, 4+c, String.format("%.5g", netRankingSPL[d][i]), i==netRankingCursor[d])
						receiver.drawMenuTTF(engine, playerID, 26, 4+c, netRankingName[d][i], i==netRankingCursor[d])
					} else if(netRankingType==NetSPRecord.RANKINGTYPE_DIGRACE) {
						receiver.drawMenuNum(engine, playerID, 5, 4+c, GeneralUtil.getTime(netRankingTime[d][i].toFloat()), i==netRankingCursor[d])
						receiver.drawMenuNum(engine, playerID, 14, 4+c, ""+netRankingLines[d][i], i==netRankingCursor[d])
						receiver.drawMenuNum(engine, playerID, 19, 4+c, ""+netRankingPiece[d][i], i==netRankingCursor[d])
						receiver.drawMenuTTF(engine, playerID, 26, 4+c, netRankingName[d][i], i==netRankingCursor[d])
					} else if(netRankingType==NetSPRecord.RANKINGTYPE_ULTRA) {
						receiver.drawMenuNum(engine, playerID, 5, 4+c, ""+netRankingScore[d][i], i==netRankingCursor[d])
						receiver.drawMenuNum(engine, playerID, 13, 4+c, ""+netRankingLines[d][i], i==netRankingCursor[d])
						receiver.drawMenuNum(engine, playerID, 18, 4+c, ""+netRankingPiece[d][i], i==netRankingCursor[d])
						receiver.drawMenuTTF(engine, playerID, 27, 4+c,
							netRankingName[d][i], i==netRankingCursor[d])
					} else if(netRankingType==NetSPRecord.RANKINGTYPE_COMBORACE) {
						receiver.drawMenuNum(engine, playerID, 5, 4+c,
							""+(netRankingScore[d][i]-1), i==netRankingCursor[d])
						receiver.drawMenuNum(engine, playerID, 11, 4+c,
							GeneralUtil.getTime(netRankingTime[d][i].toFloat()), i==netRankingCursor[d])
						receiver.drawMenuNum(engine, playerID, 20, 4+c,
							String.format("%.4g", netRankingPPS[d][i]), i==netRankingCursor[d])
						receiver.drawMenuTTF(engine, playerID, 27, 4+c,
							netRankingName[d][i], i==netRankingCursor[d])
					} else if(netRankingType==NetSPRecord.RANKINGTYPE_DIGCHALLENGE) {
						receiver.drawMenuNum(engine, playerID, 5, 4+c,
							""+netRankingScore[d][i], i==netRankingCursor[d])
						receiver.drawMenuNum(engine, playerID, 13, 4+c,
							""+netRankingLines[d][i], i==netRankingCursor[d])
						receiver.drawMenuNum(engine, playerID, 18, 4+c,
							""+netRankingDepth[d][i], i==netRankingCursor[d])
						receiver.drawMenuTTF(engine, playerID, 27, 4+c, netRankingName[d][i], i==netRankingCursor[d])
					} else if(netRankingType==NetSPRecord.RANKINGTYPE_TIMEATTACK) {
						var fontcolor = COLOR.WHITE
						if(netRankingRollclear[d][i]==1) fontcolor = COLOR.GREEN
						if(netRankingRollclear[d][i]==2) fontcolor = COLOR.ORANGE
						receiver.drawMenuNum(engine, playerID, 5, 4+c, ""+netRankingLines[d][i], fontcolor)
						receiver.drawMenuNum(engine, playerID, 11, 4+c, GeneralUtil.getTime(netRankingTime[d][i].toFloat()), i==netRankingCursor[d])
						receiver.drawMenuNum(engine, playerID, 20, 4+c, String.format("%.4g", netRankingPPS[d][i]), i==netRankingCursor[d])
						receiver.drawMenuTTF(engine, playerID, 27, 4+c, netRankingName[d][i], i==netRankingCursor[d])
					}

					c++
				}

				if(netRankingCursor[d]>=0&&netRankingCursor[d]<netRankingDate[d].size) {
					var strDate = "----/--/-- --:--:--"
					val calendar = netRankingDate[d][netRankingCursor[d]]
					if(calendar!=null) strDate = GeneralUtil.getCalendarString(calendar, TimeZone.getDefault())
					receiver.drawMenuFont(engine, playerID, 1, 25, "DATE:$strDate", COLOR.CYAN)

					val gamerate = netRankingGamerate[d][netRankingCursor[d]]
					receiver.drawMenuFont(engine, playerID, 1, 26, "GAMERATE:"+if(gamerate==0f) "UNKNOWN" else (100*gamerate).toString()+"%", COLOR.CYAN)
				}

				receiver.drawMenuFont(engine, playerID, 1, 27, "A("+strBtnA+" KEY):DOWNLOAD\nB("+strBtnB
					+" KEY):BACK LEFT/RIGHT:"+if(d==0) "DAILY" else "ALL-TIME", COLOR.ORANGE)
			} else if(netRankingNoDataFlag[d]) {
				receiver.drawMenuFont(engine, playerID, 0, 1, "<<", COLOR.ORANGE)
				receiver.drawMenuFont(engine, playerID, 38, 1, ">>", COLOR.ORANGE)
				receiver.drawMenuFont(engine, playerID, 3, 1, (if(d!=0) "DAILY" else "ALL-TIME")+" RANKING", COLOR.GREEN)

				receiver.drawMenuFont(engine, playerID, 1, 3, "NO DATA", COLOR.COBALT)

				receiver.drawMenuFont(engine, playerID, 1, 28, "B("+strBtnB+" KEY):BACK LEFT/RIGHT:"
					+if(d==0) "DAILY" else "ALL-TIME", COLOR.ORANGE)
			} else if(!netRankingReady[d]&&netRankingPlace==null||Objects.requireNonNull<Array<LinkedList<Int>>>(netRankingPlace)[d]==null) {
				receiver.drawMenuFont(engine, playerID, 0, 1, "<<", COLOR.ORANGE)
				receiver.drawMenuFont(engine, playerID, 38, 1, ">>", COLOR.ORANGE)
				receiver.drawMenuFont(engine, playerID, 3, 1, (if(d!=0) "DAILY" else "ALL-TIME")+" RANKING", COLOR.GREEN)

				receiver.drawMenuFont(engine, playerID, 1, 3, "LOADING...", COLOR.CYAN)

				receiver.drawMenuFont(engine, playerID, 1, 28, "B("+strBtnB+" KEY):BACK LEFT/RIGHT:"
					+if(d==0) "DAILY" else "ALL-TIME", COLOR.ORANGE)
			}
		}
	}

	/** Enter the netplay ranking screen
	 * @param engine GameEngine
	 * @param playerID Player ID
	 * @param goaltype Game Type
	 */
	protected fun netEnterNetPlayRankingScreen(engine:GameEngine, playerID:Int, goaltype:Int) {
		if(netRankingPlace.isNotEmpty()) {
			netRankingPlace[0].clear()
			netRankingPlace[1].clear()
		}
		netRankingCursor[0] = 0
		netRankingCursor[1] = 0
		netRankingMyRank[0] = -1
		netRankingMyRank[1] = -1
		netIsNetRankingDisplayMode = true
		owner.menuOnly = true
		val rule = if(netCurrentRoomInfo!!.rated) netCurrentRoomInfo!!.ruleName else "all"
		netLobby!!.netPlayerClient!!.send("spranking\t"+NetUtil.urlEncode(rule)+"\t"+
			NetUtil.urlEncode(name)+"\t"+goaltype+"\t"+false+"\n")
		netLobby!!.netPlayerClient!!.send("spranking\t"+NetUtil.urlEncode(rule)+"\t"+
			NetUtil.urlEncode(name)+"\t"+goaltype+"\t"+true+"\n")
	}

	/** Receive 1P NetPlay ranking.
	 * @param message Message array
	 */
	private fun netRecvNetPlayRanking(message:Array<String>) {
		val strDebugTemp = StringBuilder()
		for(element in message)
			strDebugTemp.append(element).append(" ")
		log.debug(strDebugTemp.toString())

		if(message.size>7) {
			val isDaily = java.lang.Boolean.parseBoolean(message[4])
			val d = if(isDaily) 1 else 0

			netRankingType = Integer.parseInt(message[5])
			var maxRecords = Integer.parseInt(message[6])
			val arrayRow = message[7].split(";".toRegex()).dropLastWhile {it.isEmpty()}.toTypedArray()
			maxRecords = minOf(maxRecords, arrayRow.size)

			netRankingNoDataFlag[d] = false
			netRankingReady[d] = false
			netRankingPlace[d] = LinkedList()
			netRankingName[d] = LinkedList()
			netRankingDate[d] = LinkedList()
			netRankingGamerate[d] = LinkedList()
			netRankingTime[d] = LinkedList()
			netRankingScore[d] = LinkedList()
			netRankingPiece[d] = LinkedList()
			netRankingPPS[d] = LinkedList()
			netRankingLines[d] = LinkedList()
			netRankingDepth[d] = LinkedList()
			netRankingSPL[d] = LinkedList()
			netRankingRollclear[d] = LinkedList()

			for(i in 0 until maxRecords) {
				val arrayData = arrayRow[i].split(",".toRegex()).dropLastWhile {it.isEmpty()}.toTypedArray()
				netRankingPlace[d].add(Integer.parseInt(arrayData[0]))
				val pName = NetUtil.urlDecode(arrayData[1])
				netRankingName[d].add(pName)
				GeneralUtil.importCalendarString(arrayData[2])?.let {netRankingDate[d].add(it)}
				netRankingGamerate[d].add(java.lang.Float.parseFloat(arrayData[3]))

				when(netRankingType) {
					NetSPRecord.RANKINGTYPE_GENERIC_SCORE -> {
						netRankingScore[d].add(Integer.parseInt(arrayData[4]))
						netRankingLines[d].add(Integer.parseInt(arrayData[5]))
						netRankingTime[d].add(Integer.parseInt(arrayData[6]))
					}
					NetSPRecord.RANKINGTYPE_GENERIC_TIME -> {
						netRankingTime[d].add(Integer.parseInt(arrayData[4]))
						netRankingPiece[d].add(Integer.parseInt(arrayData[5]))
						netRankingPPS[d].add(java.lang.Float.parseFloat(arrayData[6]))
					}
					NetSPRecord.RANKINGTYPE_SCORERACE -> {
						netRankingTime[d].add(Integer.parseInt(arrayData[4]))
						netRankingLines[d].add(Integer.parseInt(arrayData[5]))
						netRankingSPL[d].add(java.lang.Double.parseDouble(arrayData[6]))
					}
					NetSPRecord.RANKINGTYPE_DIGRACE -> {
						netRankingTime[d].add(Integer.parseInt(arrayData[4]))
						netRankingLines[d].add(Integer.parseInt(arrayData[5]))
						netRankingPiece[d].add(Integer.parseInt(arrayData[6]))
					}
					NetSPRecord.RANKINGTYPE_ULTRA -> {
						netRankingScore[d].add(Integer.parseInt(arrayData[4]))
						netRankingLines[d].add(Integer.parseInt(arrayData[5]))
						netRankingPiece[d].add(Integer.parseInt(arrayData[6]))
					}
					NetSPRecord.RANKINGTYPE_COMBORACE -> {
						netRankingScore[d].add(Integer.parseInt(arrayData[4]))
						netRankingTime[d].add(Integer.parseInt(arrayData[5]))
						netRankingPPS[d].add(java.lang.Float.parseFloat(arrayData[6]))
					}
					NetSPRecord.RANKINGTYPE_DIGCHALLENGE -> {
						netRankingScore[d].add(Integer.parseInt(arrayData[4]))
						netRankingLines[d].add(Integer.parseInt(arrayData[5]))
						netRankingDepth[d].add(Integer.parseInt(arrayData[6]))
					}
					NetSPRecord.RANKINGTYPE_TIMEATTACK -> {
						netRankingLines[d].add(Integer.parseInt(arrayData[4]))
						netRankingTime[d].add(Integer.parseInt(arrayData[5]))
						netRankingPPS[d].add(java.lang.Float.parseFloat(arrayData[6]))
						netRankingRollclear[d].add(Integer.parseInt(arrayData[7]))
					}
					else -> log.error("Unknown ranking type:$netRankingType")
				}

				if(pName==netPlayerName) {
					netRankingCursor[d] = i
					netRankingMyRank[d] = i
				}
			}

			netRankingReady[d] = true
		} else if(message.size>4) {
			val isDaily = java.lang.Boolean.parseBoolean(message[4])
			val d = if(isDaily) 1 else 0
			netRankingNoDataFlag[d] = true
			netRankingReady[d] = false
		}
	}

	/** NET: Send various in-game stats (as well as goaltype)<br></br>
	 * Game modes should implement this.
	 * @param engine GameEngine
	 */
	protected open fun netSendStats(engine:GameEngine) {}

	/** NET: Receive various in-game stats (as well as goaltype)<br></br>
	 * Game modes should implement this.
	 * @param engine GameEngine
	 * @param message Message array
	 */
	protected open fun netRecvStats(engine:GameEngine, message:Array<String>) {}

	/** NET: Send end-of-game stats<br></br>
	 * Game modes should implement this.
	 * @param engine GameEngine
	 */
	protected open fun netSendEndGameStats(engine:GameEngine) {}

	/** NET: Send game options to all spectators<br></br>
	 * Game modes should implement this.
	 * @param engine GameEngine
	 */
	protected open fun netSendOptions(engine:GameEngine) {}

	/** NET: Receive game options.<br></br>
	 * Game modes should implement this.
	 * @param engine GameEngine
	 * @param message Message array
	 */
	protected open fun netRecvOptions(engine:GameEngine, message:Array<String>) {}

	/** NET: Send replay data<br></br>
	 * Game modes should implement this. However, some basic codes are already
	 * implemented in NetDummyMode.
	 * @param engine GameEngine
	 */
	private fun netSendReplay(engine:GameEngine) {
		if(netIsNetRankingSendOK(engine)) {
			val record = NetSPRecord()
			record.replayProp = owner.replayProp
			record.stats = Statistics(engine.statistics)
			record.gameType = netGetGoalType()

			val strData = NetUtil.compressString(record.exportString())

			val checksumObj = Adler32()
			checksumObj.update(NetUtil.stringToBytes(strData))
			val sChecksum = checksumObj.value

			netLobby!!.netPlayerClient!!.send("spsend\t$sChecksum\t$strData\n")
		} else
			netReplaySendStatus = 2
	}

	/** NET: Get goal type (used from the default implementation of
	 * netSendReplay)<br></br>
	 * Game modes should implement this, unless there is only 1 goal type.
	 * @return Goal type (default implementation will return 0)
	 */
	protected open fun netGetGoalType():Int = 0

	/** NET: It returns `true` when the current settings doesn't
	 * prevent leaderboard screen from showing.
	 * Game modes should implement this. By default, this always returns false.
	 * @param engine GameEngine
	 * @return `true` when the current settings doesn't prevent
	 * leaderboard screen from showing.
	 */
	protected open fun netIsNetRankingViewOK(engine:GameEngine):Boolean = false

	/** NET: It returns `true` when the current settings doesn't
	 * prevent replay data from sending.
	 * By default, it just calls netIsNetRankingViewOK, but you should override
	 * it if you make "race" modes.
	 * @param engine GameEngine
	 * @return `true` when the current settings doesn't prevent
	 * replay data from sending.
	 */
	protected open fun netIsNetRankingSendOK(engine:GameEngine):Boolean = netIsNetRankingViewOK(engine)

	companion object {
		/** Log (Declared in NetDummyMode) */
		internal val log = Logger.getLogger(NetDummyMode::class.java)
	}
}
