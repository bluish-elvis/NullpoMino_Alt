/*
 Copyright (c) 2010-2024, NullNoname
 All rights reserved.

 Converted to Kotlin and modified by Venom_Nhelv as bluish-elvis
 THIS IS NOT MADE IN ASSOCIATION WITH THE GAME CREATOR.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions are met:
     * Redistributions of source code must retain the above copyright
       notice, this list of conditions and the following disclaimer.
     * Redistributions in binary form must reproduce the above copyright
       notice, this list of conditions and the following disclaimer in the
       documentation and/or other materials provided with the distribution.
     * Neither the name of NullNoname nor the names of its
       contributors may be used to endorse or promote products derived from
       this software without specific prior written permission.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 POSSIBILITY OF SUCH DAMAGE.
 */

package mu.nu.nullpo.game.subsystem.mode

import mu.nu.nullpo.game.component.*
import mu.nu.nullpo.game.event.EventReceiver
import mu.nu.nullpo.game.event.EventReceiver.COLOR
import mu.nu.nullpo.game.event.ScoreEvent
import mu.nu.nullpo.game.net.*
import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.game.play.GameManager
import mu.nu.nullpo.gui.common.BaseFont
import mu.nu.nullpo.gui.common.BaseFont.FONT.*
import mu.nu.nullpo.gui.net.NetLobbyFrame
import mu.nu.nullpo.gui.net.NetLobbyListener
import mu.nu.nullpo.util.GeneralUtil.strDateTime
import mu.nu.nullpo.util.GeneralUtil.toTimeStr
import org.apache.logging.log4j.LogManager
import java.io.IOException
import java.util.*
import java.util.zip.Adler32
import mu.nu.nullpo.util.GeneralUtil as Util

/** Special base class for netplay */
abstract class NetDummyMode:AbstractMode(), NetLobbyListener {
	/** NET: Lobby (Declared in NetDummyMode) */
	protected var netLobby:NetLobbyFrame? = null

	/** NET: true if net-play (Declared in NetDummyMode) */
	internal var netIsNetPlay = false

	/** NET: true if watch mode (Declared in NetDummyMode) */
	protected var netIsWatch = false

	/** NET: Current room info. Sometimes null. (Declared in NetDummyMode) */
	internal var netCurrentRoomInfo:NetRoomInfo? = null

	/** NET: Number of spectators (Declared in NetDummyMode) */
	protected var netNumSpectators = 0

	/** NET: Send all movements even if there are no spectators */
	internal var netForceSendMovements = false

	/** NET: Previous piece information (Declared in NetDummyMode) */
	private var netPrevPieceX = 0
	/** NET: Previous piece information (Declared in NetDummyMode) */
	private var netPrevPieceID = 0
	private var netPrevPieceY = 0
	private var netPrevPieceDir = 0

	/** NET: The skin player using (Declared in NetDummyMode) */
	private var netPlayerSkin = 0

	/** NET: If true, NetDummyMode will always send attributes when sending the
	 * field (Declared in NetDummyMode) */
	internal var netAlwaysSendFieldAttributes = false

	/** NET: Player name (Declared in NetDummyMode) */
	protected var netPlayerName:String? = null

	/** NET: Replay send status (0:Before Send 1:Sending 2:Sent) (Declared in
	 * NetDummyMode) */
	protected var netReplaySendStatus = 0

	/** NET: Current round's online ranking position (Declared in NetDummyMode) */
	protected var netRankingRank = intArrayOf(-1, -1)

	/** NET: True if new personal record (Declared in NetDummyMode) */
	protected var netIsPB = false

	/** NET: True if net ranking display mode (Declared in NetDummyMode) */
	protected var netIsNetRankingDisplayMode = false

	/** NET: Net ranking cursor position (Declared in NetDummyMode) */
	private var netRankingCursor = IntArray(0)

	/** NET: Net ranking player's current rank (Declared in NetDummyMode) */
	private var netRankingMyRank = IntArray(0)

	/** NET: 0 if viewing all-time ranking, 1 if viewing daily ranking (Declared
	 * in NetDummyMode) */
	private var netRankingView = 0

	/** NET: Net ranking type (Declared in NetDummyMode) */
	private var netRankingType = 0

	/** NET: True if no data is present. [0] for all-time and [1] for daily.
	 * (Declared in NetDummyMode) */
	private var netRankingNoDataFlag = BooleanArray(0)

	/** NET: True if loading is complete. [0] for all-time and [1] for daily.
	 * (Declared in NetDummyMode) */
	private var netRankingReady = BooleanArray(0)

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
	override val name = "NETWORK MODE"

	/** NET: Net-play Initialization. NetDummyMode will set the lobby's current
	 * mode to this. */
	override fun netplayInit(obj:NetLobbyFrame) {
		netLobby = obj
		netLobby?.netDummyMode = this

		try {
			netLobby?.ruleOptPlayer = RuleOptions(owner.engine[0].ruleOpt)
		} catch(e:NullPointerException) {
			log.error("NPE on netplayInit; Most likely the mode is overriding 'owner' variable", e)
		}

		netLobby?.let {
			if(it.netPlayerClient?.currentRoomInfo!=null)
				netOnJoin(it, it.netPlayerClient, it.netPlayerClient!!.currentRoomInfo)
		}
	}

	/** NET: Netplay Unload. NetDummyMode will set the lobby's current mode to
	 * null. */
	override fun netplayUnload(obj:NetLobbyFrame) {
		netLobby?.netDummyMode = null
		netLobby = null
	}

	/** NET: Mode Initialization. NetDummyMode will set the "owner" variable. */
	override fun modeInit(manager:GameManager) {
		log.debug("modeInit() on NetDummyMode")

		super.modeInit(manager)
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
	override fun playerInit(engine:GameEngine) {
		super.playerInit(engine)
		engine.stat = GameEngine.Status.NOTHING
		engine.isVisible = false
	}

	/** NET: Initialize various NetPlay variables.
	 *  Usually called from [playerInit]. */
	protected open fun netPlayerInit(engine:GameEngine) {
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

	override fun onSetting(engine:GameEngine):Boolean {

		if(netIsNetRankingDisplayMode) {
			netOnUpdateNetPlayRanking(engine, netGetGoalType)
			return true
		} else
		// Menu
			if(!owner.replayMode&&menu.size>0) {
				// Configuration changes val change = updateCursor(engine, 5)
				updateMenu(engine)

				// 決定
				if(menuTime<5) menuTime++ else if(engine.ctrl.isPush(Controller.BUTTON_A)) {
					engine.playSE("decide")
					saveSetting(engine, owner.modeConfig)
					owner.saveModeConfig()
					onSettingChanged(engine)
					// NET: Signal start of the game
					if(netIsNetPlay) netLobby!!.netPlayerClient!!.send("start${engine.playerID+1}p\n")
					return false
				}
				// Cancel
				if(engine.ctrl.isPush(Controller.BUTTON_B)&&!netIsNetPlay) engine.quitFlag = true

				// NET: Netplay Ranking
				if(engine.ctrl.isPush(Controller.BUTTON_D)&&netIsNetPlay&&!netIsWatch&&netIsNetRankingViewOK(engine))
					netEnterNetPlayRankingScreen(netGetGoalType)

			} else {
				menuTime++
				menuCursor = -1
				return menuTime<60*(1+(menu.loc.maxOrNull()?:0)/engine.fieldHeight)
			}
		return true
	}

	override fun onSettingChanged(engine:GameEngine) {
		// NET: Signal options change
		if(netIsNetPlay&&netNumSpectators>0) netSendOptions(engine)
	}

	/** NET: When the pieces can move.
	 * NetDummyMode will send field/next/stats/piece movements. */
	override fun onMove(engine:GameEngine):Boolean {
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
	override fun pieceLocked(engine:GameEngine, lines:Int, finesse:Boolean) {
		// NET: Send field and stats
		if(engine.ending==0&&netIsNetPlay&&!netIsWatch&&(netNumSpectators>0||netForceSendMovements)) {
			netSendField(engine)
			netSendStats(engine)
		}
	}

	/** NET: Line clear. NetDummyMode will send field and stats. */
	override fun onLineClear(engine:GameEngine):Boolean {
		// NET: Send field and stats
		if(engine.statc[0]==1&&engine.ending==0&&netIsNetPlay&&!netIsWatch&&(netNumSpectators>0||netForceSendMovements)) {
			netSendField(engine)
			netSendStats(engine)
		}
		return false
	}

	/** NET: ARE. NetDummyMode will send field, next and stats. */
	override fun onARE(engine:GameEngine):Boolean {
		// NET: Send field, next, and stats
		if(engine.statc[0]==0&&engine.ending==0&&netIsNetPlay&&!netIsWatch&&(netNumSpectators>0||netForceSendMovements)) {
			netSendField(engine)
			netSendNextAndHold(engine)
			netSendStats(engine)
		}
		return false
	}

	/** NET: Ending start. NetDummyMode will send ending start messages. */
	override fun onEndingStart(engine:GameEngine):Boolean {
		if(menuCursor==0)
		// NET: Send game completed messages
			if(netIsNetPlay&&!netIsWatch&&(netNumSpectators>0||netForceSendMovements)) {
				netSendField(engine)
				netSendNextAndHold(engine)
				netSendStats(engine)
				netLobby?.netPlayerClient?.send("game\tending\n")
			}
		return false
	}

	/** NET: "Excellent!" screen */
	override fun onExcellent(engine:GameEngine):Boolean {
		if(engine.statc[0]==0)
		// NET: Send game completed messages
			if(netIsNetPlay&&!netIsWatch&&(netNumSpectators>0||netForceSendMovements)) {
				netSendField(engine)
				netSendNextAndHold(engine)
				netSendStats(engine)
				netLobby?.netPlayerClient?.send("game\texcellent\n")
			}
		return false
	}

	/** NET: Game Over */
	override fun onGameOver(engine:GameEngine):Boolean {
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
					netLobby?.netPlayerClient?.send("dead\t-1\n")
				} else if(engine.statc[0]>=engine.field.height+1+180)
				// To results screen
					netLobby?.netPlayerClient?.send("game\tresultsscreen\n")
			} else if(engine.statc[0]<engine.field.height+1+180)
				return false
			else {
				engine.field.reset()
				engine.stat = GameEngine.Status.RESULT
				engine.resetStatc()
				return true
			}

		return false
	}

	/** NET: Results screen */
	override fun onResult(engine:GameEngine):Boolean {
		/*
    BGMStatus.BGM b=BGM.Result(0);
    if(engine.ending>=2)b=engine.statistics.time<10800?BGM.Result(1):BGM.Result(2);
    owner.bgmStatus.fadeSW=false;
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
			if(engine.ctrl.isPush(Controller.BUTTON_A)&&!netIsWatch&&netReplaySendStatus==2) {
				engine.playSE("decide")
				if(netNumSpectators>0||netForceSendMovements) {
					netLobby?.netPlayerClient?.send("game\tretry\n")
					netSendOptions(engine)
				}
				owner.reset()
			}

			return true
		}

		return false
	}

	override fun renderSetting(engine:GameEngine) {
		// NET: Netplay Ranking
		if(netIsNetRankingDisplayMode) netOnRenderNetPlayRanking(engine, receiver)
		else super.renderSetting(engine)
	}
	/** NET: Render something such as HUD. NetDummyMode will render the number
	 * of players to bottom-right of the screen. */
	override fun renderLast(engine:GameEngine) {
		// NET: Number of spectators
		netDrawSpectatorsCount(engine, 0, 20)
		// NET: All number of players
		if(engine.playerID==players-1) {
			netDrawAllPlayersCount()
			netDrawGameRate(engine)
		}
		// NET: Player name (It may also appear in offline replay)
		netDrawPlayerName(engine)
	}

	/** NET: Update menu cursor. NetDummyMode will signal cursor movement to all
	 * spectators. */
	override fun updateCursor(engine:GameEngine, maxCursor:Int):Int {
		// NET: Don't execute in watch mode
		if(netIsWatch) return 0

		val change = super.updateCursor(engine, maxCursor)

		// NET: Signal cursor change
		if((engine.ctrl.isMenuRepeatKey(Controller.BUTTON_UP)||engine.ctrl.isMenuRepeatKey(Controller.BUTTON_DOWN))&&
			netIsNetPlay&&(netNumSpectators>0||netForceSendMovements))
			netLobby?.netPlayerClient?.send("game\tcursor\t$menuCursor\n")

		return change
	}

	/** NET: Retry key */
	override fun netplayOnRetryKey(engine:GameEngine) {
		if(netIsNetPlay&&!netIsWatch) {
			owner.reset()
			netLobby?.netPlayerClient?.send("reset1p\n")
			netSendOptions(engine)
		}
	}

	/** NET: Initialization Completed (Never called) */
	override fun onLobbyInit(lobby:NetLobbyFrame) {}

	/** NET: Login completed (Never called) */
	override fun onLoginOK(lobby:NetLobbyFrame, client:NetPlayerClient) {}

	/** NET: When you enter a room (Never called) */
	override fun onRoomJoin(lobby:NetLobbyFrame, client:NetPlayerClient, roomInfo:NetRoomInfo) {}

	/** NET: When you returned to lobby (Never called) */
	override fun onRoomLeave(lobby:NetLobbyFrame, client:NetPlayerClient) {}

	/* NET: When disconnected */
	override fun onDisconnect(lobby:NetLobbyFrame, client:NetPlayerClient, ex:Throwable?) {}

	/* NET: Message received */
	@Throws(IOException::class)
	override fun onMessage(lobby:NetLobbyFrame, client:NetPlayerClient, message:List<String>) {
		// Player status update
		if(message[0]=="playerupdate") netUpdatePlayerExist()
		// When someone log out
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
			netRankingRank[0] = message[1].toInt()
			netIsPB = message[2].toBoolean()
			netRankingRank[1] = message[3].toInt()
		}
		// Netplay Ranking
		if(message[0]=="spranking") netRecvNetPlayRanking(message)
		// Reset
		if(message[0]=="reset1p") if(netIsWatch) owner.reset()
		// Game messages
		if(message[0]=="game")
			if(netIsWatch) {
				val engine = owner.engine[0]
//				if(engine.field==null) engine.field = Field()

				// Move cursor
				if(message[3]=="cursor")
					if(engine.stat==GameEngine.Status.SETTING) {
						menuCursor = message[4].toInt()
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
					engine.field.reset()
					engine.stat = GameEngine.Status.RESULT
					engine.resetStatc()
				}
			}
	}

	/* NET: When the lobby window is closed */
	override fun onLobbyExit(lobby:NetLobbyFrame) {
		try {
			for(i in owner.engine.indices)
				owner.engine[i].quitFlag = true
		} catch(_:Exception) {
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
		netIsWatch = netLobby?.netPlayerClient?.yourPlayerInfo?.seatID==-1
		netNumSpectators = 0
		netUpdatePlayerExist()

		if(netIsWatch) {
			owner.engine[0].isNextVisible = false
			owner.engine[0].isHoldVisible = false
		}

		if(roomInfo!=null&&roomInfo.ruleLock) netLobby?.ruleOptLock?.let {
			// Set to locked rule
			log.info("Set locked rule")
			val randomizer = Util.loadRandomizer(it.strRandomizer)
			val wallkick = Util.loadWallkick(it.strWallkick)
			owner.engine[0].ruleOpt.replace(it)
			owner.engine[0].randomizer = randomizer
			owner.engine[0].wallkick = wallkick
		}
	}

	/** NET: Update player count */
	protected open fun netUpdatePlayerExist() {
		netNumSpectators = 0
		netPlayerName = ""

		if(netLobby!=null&&netCurrentRoomInfo!=null&&netCurrentRoomInfo?.roomID!=-1)
			for(pInfo in netLobby?.updateSameRoomPlayerInfoList()?:emptyList())
				if(pInfo.roomID==netCurrentRoomInfo?.roomID)
					if(pInfo.seatID==0)
						netPlayerName = pInfo.strName
					else if(pInfo.seatID==-1) netNumSpectators++
	}

	/** NET: Draw number of players to bottom-right of screen.
	 * This subroutine uses "netLobby" and "owner" variables. */
	fun netDrawAllPlayersCount() {
		netLobby?.netPlayerClient?.let {
			if(it.isConnected) {
				receiver.drawFont(
					0, 480-16,
					"%40s".format("%d/%d".format(it.observerCount, it.playerCount)), BASE, when {
						it.playerCount>1 -> COLOR.RED
						it.observerCount>0 -> COLOR.GREEN
						else -> COLOR.BLUE
					}
				)
			}
		}
	}

	/** NET: Draw game-rate to bottom-right of screen.
	 * @param engine GameEngine
	 */
	fun netDrawGameRate(engine:GameEngine) {
		if(netIsNetPlay&&!netIsWatch&&engine.gameStarted&&engine.startTime!=0L) {
			val gamerate:Float = if(engine.endTime!=0L) engine.statistics.gameRate
			else {
				val nowtime = System.nanoTime()
				(engine.replayTimer/(0.00000006*(nowtime-engine.startTime))).toFloat()
			}

			receiver.drawFont(
				0, 480-32, "%40s".format("%.0f%%".format(gamerate*100f)), BASE, when {
					gamerate<.8f -> COLOR.RED
					gamerate<.9f -> COLOR.ORANGE
					gamerate<1f -> COLOR.YELLOW
					else -> COLOR.BLUE
				}
			)
		}
	}

	/** NET: Draw spectator count in score area.
	 * @param engine GameEngine
	 * @param x X offset
	 * @param y Y offset
	 */
	fun netDrawSpectatorsCount(engine:GameEngine, x:Int, y:Int) {
		if(netIsNetPlay) {
			val fontcolor = if(netIsWatch) COLOR.GREEN else COLOR.RED
			receiver.drawScore(engine, x, y, "SPECTATORS", BASE, fontcolor)
			receiver.drawScore(engine, x, y+1, "$netNumSpectators", BASE, COLOR.WHITE)

			if(engine.stat==GameEngine.Status.SETTING&&!netIsWatch&&netIsNetRankingViewOK(engine)) {
				var y2 = y+2
				if(y2>24) y2 = 24
				val strBtnD = owner.receiver.getKeyNameByButtonID(engine, Controller.BUTTON_D)
				receiver.drawScore(
					engine, x, y2, "D($strBtnD KEY):\n NET RANKING",
					BASE, COLOR.GREEN
				)
			}
		}
	}

	/** NET: Draw player's name. It may also appear in offline replay.
	 * @param engine GameEngine
	 */
	protected open fun netDrawPlayerName(engine:GameEngine) {
		if(netPlayerName!=null&&netPlayerName!!.isNotEmpty()) {
			val name = netPlayerName
			receiver.drawFont(receiver.fieldX(engine), receiver.fieldY(engine)-20, name!!, TTF)
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
				netLobby?.netPlayerClient?.send(
					"game\tpiece\t$netPrevPieceID\t$x\t$y\t$netPrevPieceDir\t${engine.nowPieceBottomY}\t"+
						"${engine.ruleOpt.pieceColor[netPrevPieceID]}\t${engine.blkSkin}\t${it.big}\n"
				)
				return@netSendPieceMovement true
			}
		}?:(return if(netPrevPieceID!=Piece.PIECE_NONE||engine.manualLock) {
			netPrevPieceID = Piece.PIECE_NONE
			netLobby?.netPlayerClient?.send(
				"game\tpiece\t$netPrevPieceID\t$netPrevPieceX\t$netPrevPieceY\t$netPrevPieceDir\t0\t${engine.blkSkin}\tfalse\n"
			)
			true
		} else false)
		return false
	}

	/** NET: Receive the current piece's movement. You can override it if you
	 * customize "piece" message.
	 * @param engine GameEngine
	 * @param message Message array
	 */
	internal fun netRecvPieceMovement(engine:GameEngine, message:List<String>) {
		val id = message[4].toInt()

		if(id>=0) {
			val pieceX = message[5].toInt()
			val pieceY = message[6].toInt()
			val pieceDir = message[7].toInt()
			//int pieceBottomY = message[8].toInt();
			val pieceColor = message[9].toInt()
			val pieceSkin = message[10].toInt()
			val pieceBig = message.size>11&&message[11].toBoolean()

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
		if(receiver.isStickySkin(engine)||netAlwaysSendFieldAttributes) {
			// Send with attributes
			val strSrcFieldData = engine.field.attrFieldToString()
			val nocompSize = strSrcFieldData.length

			val strCompFieldData = NetUtil.compressString(strSrcFieldData)
			val compSize = strCompFieldData.length

			var strFieldData = strSrcFieldData
			var isCompressed = false
			if(compSize<nocompSize) {
				strFieldData = strCompFieldData
				isCompressed = true
			}

			netLobby?.netPlayerClient?.send("game\tfieldattr\t${engine.blkSkin}\t$strFieldData\t$isCompressed\n")
		} else {
			// Send without attributes
			val strSrcFieldData = engine.field.fieldToString()
			val nocompSize = strSrcFieldData.length

			val strCompFieldData = NetUtil.compressString(strSrcFieldData)
			val compSize = strCompFieldData.length

			var strFieldData = strSrcFieldData
			var isCompressed = false
			if(compSize<nocompSize) {
				strFieldData = strCompFieldData
				isCompressed = true
			}

			netLobby?.netPlayerClient?.send(
				"game\tfield\t${engine.blkSkin}\t${engine.field.heightWithoutHurryupFloor}\t$strFieldData\t$isCompressed\n"
			)
		}
	}

	/** NET: Receive field message
	 * @param engine GameEngine
	 * @param message Message array
	 */
	protected open fun netRecvField(engine:GameEngine, message:List<String>) {
		if(message[3]=="fieldattr") {
			// With attributes
			if(message.size>4) {
				engine.nowPieceObject = null
				engine.holdDisable = false
				if(engine.stat==GameEngine.Status.SETTING) engine.stat = GameEngine.Status.MOVE
				val skin = message[4].toInt()
				netPlayerSkin = skin
				if(message.size>6) {
					val isCompressed = message[6].toBoolean()
					var strFieldData = message[5]
					if(isCompressed) strFieldData = NetUtil.decompressString(strFieldData)
					engine.field.attrStringToField(strFieldData, skin)
				}
			}
		} else // Without attributes
			if(message.size>5) {
				engine.nowPieceObject = null
				engine.holdDisable = false
				if(engine.stat==GameEngine.Status.SETTING) engine.stat = GameEngine.Status.MOVE
				val skin = message[4].toInt()
				val highestWallY = message[5].toInt()
				netPlayerSkin = skin
				if(message.size>7) {
					var strFieldData = message[6]
					val isCompressed = message[7].toBoolean()
					if(isCompressed) strFieldData = NetUtil.decompressString(strFieldData)
					engine.field.stringToField(strFieldData, skin, highestWallY, highestWallY)
				} else
					engine.field.reset()
			}
	}

	/** NET: Send next and hold piece information to all spectators
	 * @param engine GameEngine
	 */
	protected open fun netSendNextAndHold(engine:GameEngine) {
		var holdID = Piece.PIECE_NONE
		var holdDirection = Piece.DIRECTION_UP
		var holdColor = Block.COLOR_WHITE
		engine.holdPieceObject?.let {
			holdID = it.id
			holdDirection = it.direction
			holdColor = engine.ruleOpt.pieceColor[it.id]
		}

		val msg = StringBuilder("game\tnext\t${engine.ruleOpt.nextDisplay}\t${engine.holdDisable}\t")

		for(i in -1..<engine.ruleOpt.nextDisplay) {
			if(i<0)
				msg.append(holdID).append(";").append(holdDirection).append(";").append(holdColor)
			else {
				val nextObj = engine.getNextObject(engine.nextPieceCount+i)
				msg.append(nextObj!!.id).append(";").append(nextObj.direction).append(";")
					.append(engine.ruleOpt.pieceColor[nextObj.id])
			}

			if(i<engine.ruleOpt.nextDisplay-1) msg.append("\t")
		}

		msg.append("\n")
		netLobby?.netPlayerClient?.send("$msg")
	}

	/** NET: Receive next and hold piece information
	 * @param engine GameEngine
	 * @param message Message array
	 */
	internal fun netRecvNextAndHold(engine:GameEngine, message:List<String>) {
		val maxNext = message[4].toInt()
		engine.ruleOpt.nextDisplay = maxNext
		engine.holdDisable = message[5].toBoolean()

		for(i in 0..<maxNext+1)
			if(i+6<message.size) {
				val strPieceData = message[i+6].split(Regex(";")).dropLastWhile {it.isEmpty()}
				val pieceID = strPieceData[0].toInt()
				val pieceDirection = strPieceData[1].toInt()
				val pieceColor = strPieceData[2].toInt()

				if(i==0) {
					if(pieceID==Piece.PIECE_NONE)
						engine.holdPieceObject = null
					else {
						engine.holdPieceObject = Piece(pieceID).apply {
							direction = pieceDirection
							setColor(pieceColor)
							setSkin(netPlayerSkin)
							updateConnectData()
						}
					}
				} else {
					if(engine.nextPieceArrayObject.size<maxNext)
						engine.nextPieceArrayObject = List(maxNext) {
							//engine.nextPieceArrayObject[i-1] =
							Piece(pieceID).apply {
								direction = pieceDirection
								setColor(pieceColor)
								setSkin(netPlayerSkin)
								updateConnectData()
							}
						}
				}
			}

		engine.isNextVisible = true
		engine.isHoldVisible = true
	}

	/** Menu routine for 1P NetPlay online ranking screen.
	 * Usually called from [onSetting].
	 * @param goalType Goal Type
	 */
	protected fun netOnUpdateNetPlayRanking(engine:GameEngine, goalType:Int) {
		if(netIsNetRankingDisplayMode) {
			val d = netRankingView

			if(!netRankingNoDataFlag[d]&&netRankingReady[d]) {
				// Up
				if(engine.ctrl.isMenuRepeatKey(Controller.BUTTON_UP)) {
					netRankingCursor[d]--
					if(netRankingCursor[d]<0) netRankingCursor[d] = netRankingPlace[d].size-1
					engine.playSE("cursor")
				}
				// Down
				if(engine.ctrl.isMenuRepeatKey(Controller.BUTTON_DOWN)) {
					netRankingCursor[d]++
					if(netRankingCursor[d]>netRankingPlace[d].size-1) netRankingCursor[d] = 0
					engine.playSE("cursor")
				}
				// Download
				if(engine.ctrl.isPush(Controller.BUTTON_A)) {
					engine.playSE("decide")

					netLobby?.netPlayerClient?.send(
						"spdownload\t${NetUtil.urlEncode(netCurrentRoomInfo!!.ruleName)}\t${
							NetUtil.urlEncode(id)
						}\t$goalType\t${netRankingView!=0}\t${NetUtil.urlEncode(netRankingName[d][netRankingCursor[d]])}\n"
					)
					netIsNetRankingDisplayMode = false
					owner.menuOnly = false
				}
			}

			// Left/Right
			if(engine.ctrl.isPush(Controller.BUTTON_LEFT)||engine.ctrl.isPush(Controller.BUTTON_RIGHT)) {
				netRankingView = if(netRankingView==0) 1 else 0
				engine.playSE("change")
			}

			// Exit
			if(engine.ctrl.isPush(Controller.BUTTON_B)) {
				netIsNetRankingDisplayMode = false
				owner.menuOnly = false
			}
		}
	}

	/** Render 1P NetPlay online ranking screen.
	 * Usually called from [renderSetting].*/
	protected fun netOnRenderNetPlayRanking(engine:GameEngine, receiver:EventReceiver) {
		if(netIsNetRankingDisplayMode) {
			val strBtnA = receiver.getKeyNameByButtonID(engine, Controller.BUTTON_A)
			val strBtnB = receiver.getKeyNameByButtonID(engine, Controller.BUTTON_B)

			val d = netRankingView

			if(!netRankingNoDataFlag[d]&&netRankingReady[d]) {
				receiver.drawMenu(engine, 0, 1, "<<", BASE, COLOR.ORANGE)
				receiver.drawMenu(engine, 38, 1, ">>", BASE, COLOR.ORANGE)
				receiver.drawMenu(
					engine, 3, 1,
					"${if(d!=0) "DAILY" else "ALL-TIME"} RANKING (${netRankingCursor[d]+1}/${netRankingPlace[d].size})",
					BASE,
					COLOR.GREEN
				)

				val startIndex = netRankingCursor[d]/20*20
				var endIndex = startIndex+20
				if(endIndex>netRankingPlace[d].size) endIndex = netRankingPlace[d].size

				when(netRankingType) {
					NetSPRecord.RANKINGTYPE_GENERIC_SCORE -> receiver.drawMenu(
						engine, 1, 3, "    SCORE   LINE TIME     NAME",
						BASE,
						COLOR.BLUE
					)
					NetSPRecord.RANKINGTYPE_GENERIC_TIME -> receiver.drawMenu(
						engine, 1, 3, "    TIME     PIECE PPS    NAME",
						BASE,
						COLOR.BLUE
					)
					NetSPRecord.RANKINGTYPE_SCORERACE -> receiver.drawMenu(
						engine, 1, 3, "    TIME     LINE SPL    NAME", BASE,
						COLOR.BLUE
					)
					NetSPRecord.RANKINGTYPE_DIGRACE -> receiver.drawMenu(
						engine, 1, 3, "    TIME     LINE PIECE  NAME", BASE,
						COLOR.BLUE
					)
					NetSPRecord.RANKINGTYPE_ULTRA -> receiver.drawMenu(
						engine, 1, 3, "    SCORE   LINE PIECE    NAME", BASE,
						COLOR.BLUE
					)
					NetSPRecord.RANKINGTYPE_COMBORACE -> receiver.drawMenu(
						engine, 1, 3, "    COMBO TIME     PPS    NAME", BASE,
						COLOR.BLUE
					)
					NetSPRecord.RANKINGTYPE_DIGCHALLENGE -> receiver.drawMenu(
						engine, 1, 3, "    SCORE   LINE TIME     NAME",
						BASE,
						COLOR.BLUE
					)
					NetSPRecord.RANKINGTYPE_TIMEATTACK -> receiver.drawMenu(
						engine, 1, 3, "    LINE  TIME     PPS    NAME",
						BASE,
						COLOR.BLUE
					)
				}

				for((c, i) in (startIndex..<endIndex).withIndex()) {
					if(i==netRankingCursor[d])
						receiver.drawMenu(engine, 0, 4+c, BaseFont.CURSOR, BASE, COLOR.RED)

					val rankColor = if(i==netRankingMyRank[d])
						COLOR.PINK else COLOR.YELLOW
					if(netRankingPlace[d][i]==-1)
						receiver.drawMenu(engine, 1, 4+c, "N/A", BASE, rankColor)
					else
						receiver.drawMenu(engine, 1, 4+c, "%3d".format(netRankingPlace[d][i]+1), NUM, rankColor)

					when(netRankingType) {
						NetSPRecord.RANKINGTYPE_GENERIC_SCORE -> {
							receiver.drawMenu(engine, 5, 4+c, "${netRankingScore[d][i]}", NUM, i==netRankingCursor[d])
							receiver.drawMenu(engine, 13, 4+c, "${netRankingLines[d][i]}", NUM, i==netRankingCursor[d])
							receiver.drawMenu(
								engine, 18, 4+c, netRankingTime[d][i].toTimeStr,
								NUM, i==netRankingCursor[d]
							)
							receiver.drawMenu(engine, 27, 4+c, netRankingName[d][i], TTF, i==netRankingCursor[d])
						}
						NetSPRecord.RANKINGTYPE_GENERIC_TIME -> {
							receiver.drawMenu(
								engine, 5, 4+c, netRankingTime[d][i].toTimeStr,
								NUM, i==netRankingCursor[d]
							)
							receiver.drawMenu(engine, 14, 4+c, "${netRankingPiece[d][i]}", NUM, i==netRankingCursor[d])
							receiver.drawMenu(
								engine, 20, 4+c, "%.5g".format(netRankingPPS[d][i]),
								NUM, i==netRankingCursor[d]
							)
							receiver.drawMenu(engine, 27, 4+c, netRankingName[d][i], TTF, i==netRankingCursor[d])
						}
						NetSPRecord.RANKINGTYPE_SCORERACE -> {
							receiver.drawMenu(
								engine, 5, 4+c, netRankingTime[d][i].toTimeStr,
								NUM, i==netRankingCursor[d]
							)
							receiver.drawMenu(engine, 14, 4+c, "${netRankingLines[d][i]}", NUM, i==netRankingCursor[d])
							receiver.drawMenu(
								engine, 19, 4+c, "%.5g".format(netRankingSPL[d][i]),
								NUM, i==netRankingCursor[d]
							)
							receiver.drawMenu(engine, 26, 4+c, netRankingName[d][i], TTF, i==netRankingCursor[d])
						}
						NetSPRecord.RANKINGTYPE_DIGRACE -> {
							receiver.drawMenu(
								engine, 5, 4+c, netRankingTime[d][i].toTimeStr,
								NUM, i==netRankingCursor[d]
							)
							receiver.drawMenu(engine, 14, 4+c, "${netRankingLines[d][i]}", NUM, i==netRankingCursor[d])
							receiver.drawMenu(engine, 19, 4+c, "${netRankingPiece[d][i]}", NUM, i==netRankingCursor[d])
							receiver.drawMenu(engine, 26, 4+c, netRankingName[d][i], TTF, i==netRankingCursor[d])
						}
						NetSPRecord.RANKINGTYPE_ULTRA -> {
							receiver.drawMenu(engine, 5, 4+c, "${netRankingScore[d][i]}", NUM, i==netRankingCursor[d])
							receiver.drawMenu(engine, 13, 4+c, "${netRankingLines[d][i]}", NUM, i==netRankingCursor[d])
							receiver.drawMenu(engine, 18, 4+c, "${netRankingPiece[d][i]}", NUM, i==netRankingCursor[d])
							receiver.drawMenu(
								engine, 27, 4+c, netRankingName[d][i],
								TTF, i==netRankingCursor[d]
							)
						}
						NetSPRecord.RANKINGTYPE_COMBORACE -> {
							receiver.drawMenu(
								engine, 5, 4+c,
								"${(netRankingScore[d][i]-1)}", NUM, i==netRankingCursor[d]
							)
							receiver.drawMenu(
								engine, 11, 4+c,
								netRankingTime[d][i].toTimeStr, NUM, i==netRankingCursor[d]
							)
							receiver.drawMenu(
								engine, 20, 4+c,
								"%.4g".format(netRankingPPS[d][i]), NUM, i==netRankingCursor[d]
							)
							receiver.drawMenu(
								engine, 27, 4+c, netRankingName[d][i],
								TTF, i==netRankingCursor[d]
							)
						}
						NetSPRecord.RANKINGTYPE_DIGCHALLENGE -> {
							receiver.drawMenu(
								engine, 5, 4+c,
								"${netRankingScore[d][i]}", NUM, i==netRankingCursor[d]
							)
							receiver.drawMenu(
								engine, 13, 4+c,
								"${netRankingLines[d][i]}", NUM, i==netRankingCursor[d]
							)
							receiver.drawMenu(
								engine, 18, 4+c,
								"${netRankingDepth[d][i]}", NUM, i==netRankingCursor[d]
							)
							receiver.drawMenu(engine, 27, 4+c, netRankingName[d][i], TTF, i==netRankingCursor[d])
						}
						NetSPRecord.RANKINGTYPE_TIMEATTACK -> {
							var fontcolor = COLOR.WHITE
							if(netRankingRollclear[d][i]==1) fontcolor = COLOR.GREEN
							if(netRankingRollclear[d][i]==2) fontcolor = COLOR.ORANGE
							receiver.drawMenu(engine, 5, 4+c, "${netRankingLines[d][i]}", NUM, fontcolor)
							receiver.drawMenu(
								engine, 11, 4+c, netRankingTime[d][i].toTimeStr,
								NUM, i==netRankingCursor[d]
							)
							receiver.drawMenu(
								engine, 20, 4+c, "%.4g".format(netRankingPPS[d][i]),
								NUM, i==netRankingCursor[d]
							)
							receiver.drawMenu(engine, 27, 4+c, netRankingName[d][i], TTF, i==netRankingCursor[d])
						}
					}
				}

				if(netRankingCursor[d]>=0&&netRankingCursor[d]<netRankingDate[d].size) {
					val calendar = netRankingDate[d][netRankingCursor[d]]
					val strDate = calendar.strDateTime(TimeZone.getDefault())
					receiver.drawMenu(engine, 1, 25, "DATE:$strDate", BASE, COLOR.CYAN)

					val gamerate = netRankingGamerate[d][netRankingCursor[d]]
					receiver.drawMenu(
						engine, 1, 26, "GAMERATE:"+if(gamerate==0f) "UNKNOWN" else "${(100*gamerate)}%",
						BASE,
						COLOR.CYAN
					)
				}

				receiver.drawMenu(
					engine, 1, 27,
					"A($strBtnA KEY):DOWNLOAD\nB($strBtnB KEY):BACK LEFT/RIGHT:${if(d==0) "DAILY" else "ALL-TIME"}",
					BASE,
					COLOR.ORANGE
				)
			} else if(netRankingNoDataFlag[d]) {
				receiver.drawMenu(engine, 0, 1, "<<", BASE, COLOR.ORANGE)
				receiver.drawMenu(engine, 38, 1, ">>", BASE, COLOR.ORANGE)
				receiver.drawMenu(engine, 3, 1, (if(d!=0) "DAILY" else "ALL-TIME")+" RANKING", BASE, COLOR.GREEN)

				receiver.drawMenu(engine, 1, 3, "NO DATA", BASE, COLOR.COBALT)

				receiver.drawMenu(
					engine, 1, 28, "B($strBtnB KEY):BACK LEFT/RIGHT:${if(d==0) "DAILY" else "ALL-TIME"}",
					BASE,
					COLOR.ORANGE
				)
			} else if(!netRankingReady[d]) {
				receiver.drawMenu(engine, 0, 1, "<<", BASE, COLOR.ORANGE)
				receiver.drawMenu(engine, 38, 1, ">>", BASE, COLOR.ORANGE)
				receiver.drawMenu(engine, 3, 1, "${if(d!=0) "DAILY" else "ALL-TIME"} RANKING", BASE, COLOR.GREEN)

				receiver.drawMenu(engine, 1, 3, "LOADING...", BASE, COLOR.CYAN)

				receiver.drawMenu(
					engine, 1, 28, "B($strBtnB KEY):BACK LEFT/RIGHT:${if(d==0) "DAILY" else "ALL-TIME"}",
					BASE,
					COLOR.ORANGE
				)
			}
		}
	}

	/** Enter the netplay ranking screen
	 * @param goalType Game Type
	 */
	protected fun netEnterNetPlayRankingScreen(goalType:Int) {
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
		netCurrentRoomInfo?.let {
			val rule = if(it.rated) it.ruleName else "all"
			netLobby?.netPlayerClient?.send(
				"spranking\t${NetUtil.urlEncode(rule)}\t${NetUtil.urlEncode(id)}\t$goalType\tfalse\n")
			netLobby?.netPlayerClient?.send("spranking\t${NetUtil.urlEncode(rule)}\t${NetUtil.urlEncode(id)}\t$goalType\ttrue\n")
		}
	}

	/** Receive 1P NetPlay ranking.
	 * @param message Message array
	 */
	private fun netRecvNetPlayRanking(message:List<String>) {
		val strDebugTemp = StringBuilder()
		for(element in message)
			strDebugTemp.append(element).append(" ")
		log.debug("{}", strDebugTemp)

		if(message.size>7) {
			val isDaily = message[4].toBoolean()
			val d = if(isDaily) 1 else 0

			netRankingType = message[5].toInt()
			var maxRecords = message[6].toInt()
			val arrayRow = message[7].split(Regex(";")).dropLastWhile {it.isEmpty()}
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

			for(i in 0..<maxRecords) {
				val arrayData = arrayRow[i].split(Regex(",")).dropLastWhile {it.isEmpty()}
				netRankingPlace[d].add(arrayData[0].toInt())
				val pName = NetUtil.urlDecode(arrayData[1])
				netRankingName[d].add(pName)
				Util.importCalendarString(arrayData[2])?.let {netRankingDate[d].add(it)}
				netRankingGamerate[d].add(arrayData[3].toFloat())

				when(netRankingType) {
					NetSPRecord.RANKINGTYPE_GENERIC_SCORE -> {
						netRankingScore[d].add(arrayData[4].toInt())
						netRankingLines[d].add(arrayData[5].toInt())
						netRankingTime[d].add(arrayData[6].toInt())
					}
					NetSPRecord.RANKINGTYPE_GENERIC_TIME -> {
						netRankingTime[d].add(arrayData[4].toInt())
						netRankingPiece[d].add(arrayData[5].toInt())
						netRankingPPS[d].add(arrayData[6].toFloat())
					}
					NetSPRecord.RANKINGTYPE_SCORERACE -> {
						netRankingTime[d].add(arrayData[4].toInt())
						netRankingLines[d].add(arrayData[5].toInt())
						netRankingSPL[d].add(arrayData[6].toDouble())
					}
					NetSPRecord.RANKINGTYPE_DIGRACE -> {
						netRankingTime[d].add(arrayData[4].toInt())
						netRankingLines[d].add(arrayData[5].toInt())
						netRankingPiece[d].add(arrayData[6].toInt())
					}
					NetSPRecord.RANKINGTYPE_ULTRA -> {
						netRankingScore[d].add(arrayData[4].toInt())
						netRankingLines[d].add(arrayData[5].toInt())
						netRankingPiece[d].add(arrayData[6].toInt())
					}
					NetSPRecord.RANKINGTYPE_COMBORACE -> {
						netRankingScore[d].add(arrayData[4].toInt())
						netRankingTime[d].add(arrayData[5].toInt())
						netRankingPPS[d].add(arrayData[6].toFloat())
					}
					NetSPRecord.RANKINGTYPE_DIGCHALLENGE -> {
						netRankingScore[d].add(arrayData[4].toInt())
						netRankingLines[d].add(arrayData[5].toInt())
						netRankingDepth[d].add(arrayData[6].toInt())
					}
					NetSPRecord.RANKINGTYPE_TIMEATTACK -> {
						netRankingLines[d].add(arrayData[4].toInt())
						netRankingTime[d].add(arrayData[5].toInt())
						netRankingPPS[d].add(arrayData[6].toFloat())
						netRankingRollclear[d].add(arrayData[7].toInt())
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
			val isDaily = message[4].toBoolean()
			val d = if(isDaily) 1 else 0
			netRankingNoDataFlag[d] = true
			netRankingReady[d] = false
		}
	}

	/** NET: Send various in-game stats (as well as goalType)<br></br>
	 * Game modes should implement this.
	 * @param engine GameEngine
	 */
	protected open fun netSendStats(engine:GameEngine) {

		val msg = "game\tstats\t"+engine.run {
			statistics.run {
				"${scoreLine}\t${scoreSD}\t${scoreHD}\t${scoreBonus}\t${lines}\t${totalPieceLocked}\t${time}\t${level}\t"
			}+"${gameActive}\t${timerActive}\t${lastEvent}\t\t${if(owner.bgMan.fadeSW) owner.bgMan.nextBg else engine.owner.bgMan.bg}\n"
		}
		netLobby?.netPlayerClient?.send(msg)
	}

	/** NET: Receive various in-game stats (as well as goalType)<br></br>
	 * Game modes should implement this.
	 * @param engine GameEngine
	 * @param message Message array
	 */
	protected open fun netRecvStats(engine:GameEngine, message:List<String>) {
		listOf<(String)->Unit>({}, {}, {}, {},
			{engine.statistics.scoreLine = it.toInt()},
			{engine.statistics.scoreSD = it.toInt()},
			{engine.statistics.scoreHD = it.toInt()},
			{engine.statistics.scoreBonus = it.toInt()},
			{engine.statistics.lines = it.toInt()},
			{engine.statistics.totalPieceLocked = it.toInt()},
			{engine.statistics.time = it.toInt()},
			{engine.statistics.level = it.toInt()},
			{engine.gameActive = it.toBoolean()},
			{engine.timerActive = it.toBoolean()},
			{engine.lastEvent = ScoreEvent.parseStr(it)},
			{engine.owner.bgMan.bg = it.toInt()}).zip(message).forEach {(x, y) ->
			x(y)
		}

		// Meter
		engine.meterValue = engine.statistics.lines%10/9f
		engine.meterColor = GameEngine.METER_COLOR_LEVEL
	}

	/** NET: Send end-of-game stats<br></br>
	 * Game modes should implement this.
	 * @param engine GameEngine
	 */
	protected open fun netSendEndGameStats(engine:GameEngine) {
		val subMsg = engine.statistics.run {
			"SCORE;${score}\tLINE;${lines}\tLEVEL;${level+levelDispAdd}\tTIME;${time.toTimeStr}\tSCORE/LINE;${spl}\tLINE/MIN;${lpm}\t"
		}
		netLobby?.netPlayerClient?.send("gstat1p	${NetUtil.urlEncode(subMsg)}\n")
	}

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
	protected open fun netRecvOptions(engine:GameEngine, message:List<String>) {}

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
			record.gameType = netGetGoalType

			val strData = NetUtil.compressString(record.exportString())

			val checksumObj = Adler32()
			checksumObj.update(NetUtil.stringToBytes(strData))
			val sChecksum = checksumObj.value

			netLobby?.netPlayerClient?.send("spsend\t$sChecksum\t$strData\n")
		} else
			netReplaySendStatus = 2
	}

	/** NET: Get goal type (used from the default implementation of
	 * netSendReplay)<br></br>
	 * Game modes should implement this, unless there is only 1 goal type.
	 * @return Goal type (default implementation will return 0)
	 */
	protected open val netGetGoalType get() = 0

	/** NET: It returns true when the current settings don't prevent leaderboard screen from showing.
	 * Game modes should implement this. By default, this always returns false.
	 * @param engine GameEngine
	 * @return `true` when the current settings don't prevent leaderboard screen from showing.
	 */
	protected open fun netIsNetRankingViewOK(engine:GameEngine):Boolean = engine.ai==null

	/** NET: It returns true when the current settings don't prevent leaderboard screen from showing.
	 * By default, it just calls netIsNetRankingViewOK, but you should override
	 * it if you make "race" modes.
	 * @param engine GameEngine
	 * @return `true` when the current settings don't prevent replay data from sending.
	 */
	protected open fun netIsNetRankingSendOK(engine:GameEngine):Boolean = netIsNetRankingViewOK(engine)

	companion object {
		/** Log (Declared in NetDummyMode) */
		internal val log = LogManager.getLogger()
	}
}
