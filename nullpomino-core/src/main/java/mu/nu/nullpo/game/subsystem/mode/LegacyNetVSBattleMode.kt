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

import mu.nu.nullpo.game.component.*
import mu.nu.nullpo.game.event.EventReceiver.COLOR
import mu.nu.nullpo.game.net.*
import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.game.play.GameManager
import mu.nu.nullpo.gui.net.NetLobbyFrame
import mu.nu.nullpo.util.GeneralUtil
import org.apache.log4j.Logger
import java.util.*

/** The old version of NET-VS-BATTLE Mode
 */
@Deprecated("Replaced with the current NetVSBattleMode which uses\n"+
	"  NetDummyVSMode. ")
class LegacyNetVSBattleMode:NetDummyMode() {

	/** Current room ID */
	private var currentRoomID:Int = 0

	/** Current room informations */
	private var currentRoomInfo:NetRoomInfo? = null

	/** true if rule is locked */
	private var rulelockFlag:Boolean = false

	/** Use reduced attack tables if 3 or more players are alive */
	private var reduceLineSend:Boolean = false

	/** Use fractional garbage system */
	private var useFractionalGarbage:Boolean = false

	/** Garbage hole change probability */
	private var garbagePercent:Int = 0

	/** Change garbage on attack */
	private var garbageChangePerAttack:Boolean = false

	/** Divide hole change rate by number of remaining opposing players/teams */
	private var divideChangeRateByPlayers:Boolean = false

	/** Column number of hole in most recent garbage line */
	private var lastHole = -1

	/** Tank mode */
	//private boolean useTankMode = false;

	/** Time in seconds before hurry up (-1 if hurry up disabled) */
	private var hurryupSeconds:Int = 0

	/** Number of pieces to be placed between adding Hurry Up lines */
	private var hurryupInterval:Int = 0

	/** true if Hurry Up has been started */
	private var hurryupStarted:Boolean = false

	/** Number of frames left to show "HURRY UP!" text */
	private var hurryupShowFrames:Int = 0

	/** Seat number of local player (-1: spectator) */
	private var playerSeatNumber:Int = 0

	/** Number of games since joining this room */
	private var numGames:Int = 0

	/** Local player wins count */
	private var numWins:Int = 0

	/** Number of players */
	private var numPlayers:Int = 0

	/** Number of spectators */
	private var numSpectators:Int = 0

	/** Number of players in current game */
	private var numNowPlayers:Int = 0

	/** Maximum number of players in this room */
	private var numMaxPlayers:Int = 0

	/** Number of alive players */
	private var numAlivePlayers:Int = 0

	/** Seat numbers of players */
	private var allPlayerSeatNumbers:IntArray = IntArray(0)

	/** true if player field exists */
	private var isPlayerExist:BooleanArray = BooleanArray(0)

	/** true if player is ready */
	private var isReady:BooleanArray = BooleanArray(0)

	/** Dead flag */
	private var isDead:BooleanArray = BooleanArray(0)

	/** Place */
	private var playerPlace:IntArray = IntArray(0)

	/** true if you KO'd player */
	private var playerKObyYou:BooleanArray = BooleanArray(0)

	/** Player active flag (false if newcomer) */
	private var playerActive:BooleanArray = BooleanArray(0)

	/** Block skin used */
	private var playerSkin:IntArray = IntArray(0)

	/** Player name */
	private var playerNames:Array<String> = emptyArray()

	/** Team name */
	private var playerTeams:Array<String> = emptyArray()

	/** Team colors */
	private var playerTeamColors:IntArray = IntArray(0)

	/** Number of games played */
	private var playerGamesCount:IntArray = IntArray(0)

	/** Number of wins */
	private var playerWinCount:IntArray = IntArray(0)

	//	private boolean[] playerTeamsIsTank;
	//
	//	private boolean isTank;

	/** true if room game is in progress */
	private var isNetGameActive:Boolean = false

	/** true if room game is finished */
	private var isNetGameFinished:Boolean = false

	/** true if local player joined game in progress */
	private var isNewcomer:Boolean = false

	/** true if waiting for ready status change */
	private var isReadyChangePending:Boolean = false

	/** true if practice mode */
	private var isPractice:Boolean = false

	/** Automatic start timer is enabled */
	private var autoStartActive:Boolean = false

	/** Time left until automatic start */
	private var autoStartTimer:Int = 0

	/** true is time elapsed counter is enabled */
	private var netPlayTimerActive:Boolean = false

	/** Elapsed time */
	private var netPlayTimer:Int = 0

	/** How long current piece is active */
	private var pieceMoveTimer:Int = 0

	/** Previous state of active piece */
	private var prevPieceID:Int = Piece.PIECE_NONE
	private var prevPieceX:Int = 0
	private var prevPieceY:Int = 0
	private var prevPieceDir:Int = 0

	/** Time to display the most recent increase in score */
	private var scgettime:IntArray = IntArray(0)

	/** Most recent scoring event type */
	private var lastevent:IntArray = IntArray(0)

	/** true if most recent scoring event was B2B */
	private var lastb2b:BooleanArray = BooleanArray(0)

	/** Most recent scoring event Combo count */
	private var lastcombo:IntArray = IntArray(0)

	/** Most recent scoring event piece type */
	private var lastpiece:IntArray = IntArray(0)

	/** Count of garbage lines send */
	private var garbageSent:IntArray = IntArray(0)

	/** Amount of garbage in garbage queue */
	private var garbage:IntArray = IntArray(0)

	/** Recieved garbage entries */
	private var garbageEntries:LinkedList<GarbageEntry>? = null

	/** APL (Attack Per Line) */
	private var playerAPL:FloatArray = FloatArray(0)

	/** APM (Attack Per Minute) */
	private var playerAPM:FloatArray = FloatArray(0)

	/** true if results are received and ready to display */
	private var isPlayerResultReceived:BooleanArray = BooleanArray(0)

	/** Number of pieces placed after Hurry Up has started */
	private var hurryupCount:Int = 0

	/** Map number to use */
	private var mapNo:Int = 0

	/** Random for selecting map in Practice mode */
	private var randMap:Random? = null

	/** Practice mode last used map number */
	private var mapPreviousPracticeMap:Int = 0

	/** UID of player who attacked local player last */
	private var lastAttackerUID:Int = 0

	/** KO count */
	private var currentKO:Int = 0

	/** true if can exit from practice game */
	private var isPracticeExitAllowed:Boolean = false

	/** Target ID (-1:All) */
	private var targetID:Int = 0

	/** Target Timer */
	private var targetTimer:Int = 0

	/** Surviving teamcountReturns(No teamPlayerAs well1Team and
	 * onecountObtained)
	 * @return Surviving teamcount
	 */
	private val numberOfTeamsAlive:Int
		get() {
			val listTeamName = LinkedList<String>()
			var noTeamCount = 0

			for(i in 0 until MAX_PLAYERS)
				if(isPlayerExist[i]&&!isDead[i]&&owner.engine[i].gameActive)
					if(playerTeams[i].isNotEmpty()) {
						if(!listTeamName.contains(playerTeams[i])) listTeamName.add(playerTeams[i])
					} else
						noTeamCount++

			return noTeamCount+listTeamName.size
		}

	/** Get number of possible targets (number of opponents)
	 * @return Number of possible targets (number of opponents)
	 */
	private val numberOfPossibleTargets:Int
		get() {
			var count = 0
			for(i in 1 until MAX_PLAYERS)
				if(isTargetable(i)) count++
			return count
		}

	/** State of war in this room nowNumber of playersReturns
	 * @return State of warNumber of players
	 */
	/* private int getCurrentNumberOfPlayers() {
	 * int count = 0;
	 * for(int i = 0; i < MAX_PLAYERS; i++) {
	 * if(isPlayerExist[i]) count++;
	 * }
	 * return count;
	 * } */

	/* Mode name */
	override val name:String get() = "NET-VS-BATTLE"

	override val isVSMode:Boolean get() = true

	/* Maximum players count */
	override val players:Int get() = MAX_PLAYERS

	/* NetPlay */
	override val isNetplayMode:Boolean get() = true

	/** I have now accumulatedgarbage blockOfcountReturns
	 * @return I have now accumulatedgarbage blockOfcount
	 */
	private val totalGarbageLines:Int
		get() {
			var count = 0
			for(garbageEntry in garbageEntries!!)
				count += garbageEntry.lines
			return count
		}

	/** Game seat numberBased on thefield numberReturns
	 * @param seat Game seat number
	 * @return Correspondingfield number
	 */
	private fun getPlayerIDbySeatID(seat:Int):Int {
		var myseat = playerSeatNumber
		if(myseat<0) myseat = 0
		return GAME_SEAT_NUMBERS[myseat][seat]
	}

	/** PlayerPresence flagAnd humancountUpdate */
	private fun updatePlayerExist() {
		numPlayers = 0
		numSpectators = 0

		for(i in 0 until MAX_PLAYERS) {
			isPlayerExist[i] = false
			isReady[i] = false
			allPlayerSeatNumbers[i] = -1
			owner.engine[i].framecolor = GameEngine.FRAME_COLOR_GRAY
		}

		if(currentRoomID!=-1&&netLobby!=null)
			for(pInfo in netLobby!!.updateSameRoomPlayerInfoList())
				if(pInfo.roomID==currentRoomID)
					if(pInfo.seatID!=-1) {
						val playerID = getPlayerIDbySeatID(pInfo.seatID)
						isPlayerExist[playerID] = true
						isReady[playerID] = pInfo.ready
						allPlayerSeatNumbers[playerID] = pInfo.seatID
						numPlayers++

						if(pInfo.seatID<PLAYER_COLOR_FRAME.size)
							owner.engine[playerID].framecolor = PLAYER_COLOR_FRAME[pInfo.seatID]
					} else
						numSpectators++
	}

	/** Check if the given playerID can be targeted
	 * @param playerID Player ID (to target)
	 * @return true if playerID can be targeted
	 */
	private fun isTargetable(playerID:Int):Boolean {
		// Can't target self
		if(playerID<=0) return false

		// Doesn't exist?
		if(!isPlayerExist[playerID]||isDead[playerID]||!playerActive[playerID]) return false

		// Is teammate?
		val myTeam = playerTeams[0]
		val thisTeam = playerTeams[playerID]
		return myTeam.isEmpty()||thisTeam.isEmpty()||myTeam!=thisTeam
	}

	/** Set new target */
	private fun setNewTarget() {
		if(numberOfPossibleTargets>=1&&currentRoomInfo!=null&&currentRoomInfo!!.isTarget&&
			playerSeatNumber>=0&&!isPractice)
			do {
				targetID++
				if(targetID>=MAX_PLAYERS) targetID = 1
			} while(!isTargetable(targetID))
		else
			targetID = -1
	}

	/* Mode Initialization */
	override fun modeInit(manager:GameManager) {
		super.modeInit(manager)
		receiver = owner.receiver
		currentRoomID = -1
		playerSeatNumber = -1
		numGames = 0
		numWins = 0
		numPlayers = 0
		numSpectators = 0
		numNowPlayers = 0
		numMaxPlayers = 0
		autoStartActive = false
		autoStartTimer = 0
		garbagePercent = 100
		garbageChangePerAttack = true
		divideChangeRateByPlayers = false
		//useTankMode = false;
		isReady = BooleanArray(MAX_PLAYERS)
		playerActive = BooleanArray(MAX_PLAYERS)
		playerNames = Array(MAX_PLAYERS) {""}
		playerTeams = Array(MAX_PLAYERS) {""}
		playerTeamColors = IntArray(MAX_PLAYERS)
		playerGamesCount = IntArray(MAX_PLAYERS)
		playerWinCount = IntArray(MAX_PLAYERS)
		//		playerTeamsIsTank = new boolean[MAX_PLAYERS];
		scgettime = IntArray(MAX_PLAYERS)
		lastevent = IntArray(MAX_PLAYERS)
		lastb2b = BooleanArray(MAX_PLAYERS)
		lastcombo = IntArray(MAX_PLAYERS)
		lastpiece = IntArray(MAX_PLAYERS)
		garbageSent = IntArray(MAX_PLAYERS)
		garbage = IntArray(MAX_PLAYERS)
		playerAPL = FloatArray(MAX_PLAYERS)
		playerAPM = FloatArray(MAX_PLAYERS)
		isPlayerResultReceived = BooleanArray(MAX_PLAYERS)
		mapPreviousPracticeMap = -1
		playerSkin = IntArray(MAX_PLAYERS)
		for(i in 0 until MAX_PLAYERS)
			playerSkin[i] = -1
		resetFlags()
	}

	/** Various reset */
	private fun resetFlags() {
		//		isTank = false;
		isPractice = false
		allPlayerSeatNumbers = IntArray(MAX_PLAYERS)
		isPlayerExist = BooleanArray(MAX_PLAYERS)
		isDead = BooleanArray(MAX_PLAYERS)
		playerPlace = IntArray(MAX_PLAYERS)
		playerKObyYou = BooleanArray(MAX_PLAYERS)
		playerActive = BooleanArray(MAX_PLAYERS)
		isNetGameActive = false
		isNetGameFinished = false
		isNewcomer = false
		isReadyChangePending = false
		netPlayTimerActive = false
		netPlayTimer = 0
		lastAttackerUID = -1
		currentKO = 0
		targetID = -1
		targetTimer = 0
	}

	/** When you join the room
	 * @param lobby NetLobbyFrame
	 * @param client NetPlayerClient
	 * @param roomInfo NetRoomInfo
	 */
	override fun netOnJoin(lobby:NetLobbyFrame, client:NetPlayerClient?, roomInfo:NetRoomInfo?) {
		log.debug("onJoin on NetVSBattleMode")

		resetFlags()
		owner.reset()

		isReady = BooleanArray(MAX_PLAYERS)
		if(currentRoomInfo!=null) {
			currentRoomInfo!!.delete()
			currentRoomInfo = null
		}

		playerSeatNumber = client!!.yourPlayerInfo!!.seatID
		currentRoomID = client.yourPlayerInfo!!.roomID
		currentRoomInfo = roomInfo
		autoStartActive = false

		numMaxPlayers = roomInfo?.maxPlayers ?: 0

		if(roomInfo!=null) {
			rulelockFlag = roomInfo.ruleLock
			reduceLineSend = roomInfo.reduceLineSend
			hurryupSeconds = roomInfo.hurryupSeconds
			hurryupInterval = roomInfo.hurryupInterval
			useFractionalGarbage = roomInfo.useFractionalGarbage
			garbagePercent = roomInfo.garbagePercent
			garbageChangePerAttack = roomInfo.garbageChangePerAttack
			divideChangeRateByPlayers = roomInfo.divideChangeRateByPlayers
			//useTankMode = roomInfo.useTankMode;

			for(i in 0 until players) owner.engine[i].apply {
				speed.gravity = roomInfo.gravity
				speed.denominator = roomInfo.denominator
				speed.are = roomInfo.are
				speed.areLine = roomInfo.areLine
				speed.lineDelay = roomInfo.lineDelay
				speed.lockDelay = roomInfo.lockDelay
				speed.das = roomInfo.das
				b2bEnable = roomInfo.b2b
				comboType = if(roomInfo.combo) GameEngine.COMBO_TYPE_NORMAL else GameEngine.COMBO_TYPE_DISABLE

				when {
					roomInfo.tspinEnableType==0 -> {
						tspinEnable = false
						useAllSpinBonus = false
					}
					roomInfo.tspinEnableType==1 -> {
						tspinEnable = true
						useAllSpinBonus = false
					}
					roomInfo.tspinEnableType==2 -> {
						tspinEnable = true
						useAllSpinBonus = true
					}
				}
			}

			isNewcomer = roomInfo.playing

			if(!rulelockFlag) {
				// Revert rules
				owner.engine[0].ruleopt.copy(netLobby!!.ruleOptPlayer)
				owner.engine[0].randomizer = GeneralUtil.loadRandomizer(owner.engine[0].ruleopt.strRandomizer)
				owner.engine[0].wallkick = GeneralUtil.loadWallkick(owner.engine[0].ruleopt.strWallkick)
			} else // Set to locked rule
				if(netLobby!=null&&netLobby!!.ruleOptLock!=null) {
					log.info("Set locked rule")
					val randomizer = GeneralUtil.loadRandomizer(netLobby!!.ruleOptLock!!.strRandomizer)
					val wallkick = GeneralUtil.loadWallkick(netLobby!!.ruleOptLock!!.strWallkick)
					for(i in 0 until players) {
						owner.engine[i].ruleopt.copy(netLobby!!.ruleOptLock)
						owner.engine[i].randomizer = randomizer
						owner.engine[i].wallkick = wallkick
					}
				} else
					log.warn("Tried to set locked rule, but rule was not received yet!")
		}

		numGames = 0
		numWins = 0

		for(i in 0 until players) {
			owner.engine[i].enableSE = false
			owner.engine[i].isVisible = i<numMaxPlayers
		}
		if(playerSeatNumber>=0) {
			owner.engine[0].displaysize = 0
			owner.engine[0].enableSE = true
		} else {
			owner.engine[0].displaysize = -1
			owner.engine[0].enableSE = false
		}

		// Apply 1vs1 layout
		if(roomInfo!=null&&roomInfo.maxPlayers==2) {
			owner.engine[0].displaysize = 0
			owner.engine[1].displaysize = 0
		}

		updatePlayerExist()
		updatePlayerNames()
	}

	/** Update player names */
	private fun updatePlayerNames() {
		val pList = netLobby!!.sameRoomPlayerInfoList
		val teamList = LinkedList<String>()

		for(i in 0 until MAX_PLAYERS) {
			playerNames[i] = ""
			playerTeams[i] = ""
			playerTeamColors[i] = 0
			playerGamesCount[i] = 0
			playerWinCount[i] = 0

			for(pInfo in pList)
				if(pInfo.seatID!=-1&&getPlayerIDbySeatID(pInfo.seatID)==i) {
					playerNames[i] = pInfo.strName
					playerTeams[i] = pInfo.strTeam
					playerGamesCount[i] = pInfo.playCountNow
					playerWinCount[i] = pInfo.winCountNow

					// Set team cint
					if(playerTeams[i].isNotEmpty())
						if(!teamList.contains(playerTeams[i])) {
							teamList.add(playerTeams[i])
							playerTeamColors[i] = teamList.size
						} else
							playerTeamColors[i] = teamList.indexOf(playerTeams[i])+1
				}
		}
	}

	/** Send field state
	 * @param engine GameEngine
	 */
	private fun sendField(engine:GameEngine) {
		if(isPractice) return
		if(numPlayers+numSpectators<2) return

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

			garbage[engine.playerID] = totalGarbageLines

			var msg = "game\tfieldattr\t"+garbage[engine.playerID]+"\t"+engine.skin+"\t"
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
			//log.debug("nocompSize:" + nocompSize + " compSize:" + compSize + " isCompressed:" + isCompressed);

			garbage[engine.playerID] = totalGarbageLines

			var msg = ("game\tfield\t"+garbage[engine.playerID]+"\t"+engine.skin+"\t"
				+engine.field!!.highestGarbageBlockY+"\t")
			msg += engine.field!!.heightWithoutHurryupFloor.toString()+"\t"
			msg += strFieldData+"\t"+isCompressed+"\n"
			netLobby!!.netPlayerClient!!.send(msg)
		}
	}

	/** Start practice mode
	 * @param engine GameEngine
	 */
	private fun startPractice(engine:GameEngine) {
		isPractice = true
		isPracticeExitAllowed = false
		engine.init()
		engine.stat = GameEngine.Status.READY
		engine.resetStatc()

		// map
		if(currentRoomInfo!=null&&currentRoomInfo!!.useMap&&netLobby!!.mapList.size>0) {
			if(randMap==null) randMap = Random()

			var map = 0
			val maxMap = netLobby!!.mapList.size
			do
				map = randMap!!.nextInt(maxMap)
			while(map==mapPreviousPracticeMap&&maxMap>=2)
			mapPreviousPracticeMap = map

			engine.createFieldIfNeeded()
			engine.field!!.stringToField(netLobby!!.mapList[map])
			engine.field!!.setAllSkin(engine.skin)
			engine.field!!.setAllAttribute(Block.BLOCK_ATTRIBUTE_VISIBLE, true)
			engine.field!!.setAllAttribute(Block.BLOCK_ATTRIBUTE_OUTLINE, true)
			engine.field!!.setAllAttribute(Block.BLOCK_ATTRIBUTE_SELFPLACED, false)
		}
	}

	/** Send game results
	 * @param engine GameEngine
	 * @param playerID Player ID
	 */
	private fun sendGameStat(engine:GameEngine, playerID:Int) {
		var msg = "gstat\t"
		msg += playerPlace[playerID].toString()+"\t"
		msg += (garbageSent[playerID].toFloat()/GARBAGE_DENOMINATOR).toString()+"\t"+playerAPL[0]+"\t"+playerAPM[0]+"\t"
		msg += engine.statistics.lines.toString()+"\t"+engine.statistics.lpm+"\t"
		msg += engine.statistics.totalPieceLocked.toString()+"\t"+engine.statistics.pps+"\t"
		msg += netPlayTimer.toString()+"\t"+currentKO+"\t"+numWins+"\t"+numGames
		msg += "\n"
		netLobby!!.netPlayerClient!!.send(msg)
	}

	/** Receive game results
	 * @param message Message
	 */
	private fun recvGameStat(message:Array<String>) {
		//int uid = Integer.parseInt(message[1]);
		val seatID = Integer.parseInt(message[2])
		val playerID = getPlayerIDbySeatID(seatID)

		if(playerID!=0||playerSeatNumber<0) {
			val engine = owner.engine[playerID]

			val tempGarbageSend = java.lang.Float.parseFloat(message[5])
			garbageSent[playerID] = (tempGarbageSend*GARBAGE_DENOMINATOR).toInt()

			playerAPL[playerID] = java.lang.Float.parseFloat(message[6])
			playerAPM[playerID] = java.lang.Float.parseFloat(message[7])
			engine.statistics.lines = Integer.parseInt(message[8])
			//engine.statistics.lpm = java.lang.Float.parseFloat(message[9])
			engine.statistics.totalPieceLocked = Integer.parseInt(message[10])
			//engine.statistics.pps = java.lang.Float.parseFloat(message[11])
			engine.statistics.time = Integer.parseInt(message[12])

			isPlayerResultReceived[playerID] = true
		}
	}

	/* Initialization for each player */
	override fun playerInit(engine:GameEngine, playerID:Int) {
		if(playerID>=1||playerSeatNumber==-1) {
			engine.displaysize = -1
			engine.enableSE = false
		} else {
			engine.displaysize = 0
			engine.enableSE = true
		}
		engine.fieldWidth = 10
		engine.fieldHeight = 20
		engine.gameoverAll = false
		engine.allowTextRenderByReceiver = true

		garbage[playerID] = 0
		garbageSent[playerID] = 0

		playerAPL[playerID] = 0f
		playerAPM[playerID] = 0f
		isPlayerResultReceived[playerID] = playerID==0&&playerSeatNumber>=0

		//		playerTeamsIsTank[playerID] = true;

		if(playerID==0) {
			prevPieceID = Piece.PIECE_NONE
			prevPieceX = 0
			prevPieceY = 0
			prevPieceDir = 0

			if(garbageEntries==null)
				garbageEntries = LinkedList()
			else
				garbageEntries!!.clear()

			if(playerSeatNumber>=0) engine.framecolor = PLAYER_COLOR_FRAME[playerSeatNumber]
		}

		if(playerID>=numMaxPlayers) engine.isVisible = false
		if(playerID==players-1) updatePlayerExist()
	}

	//	@Override
	//	public void onFirst(GameEngine engine, int playerID) {
	//		if( (useTankMode == true) && (!isPractice) && (isNetGameActive) ){
	//			if(engine.ctrl.isPush(Controller.BUTTON_F)) {
	//				if((!playerTeamsIsTank[0]) || (!isTank)) {
	//					playerTeamsIsTank[0] = true;
	//					isTank = true;
	//					netLobby.netPlayerClient.send("game\ttank\n");
	//				}
	//			}
	//		}
	//	}

	/* Called at settings screen */
	override fun onSetting(engine:GameEngine, playerID:Int):Boolean {
		if(playerID==0&&playerSeatNumber>=0) {
			isPlayerExist[0] = true
			engine.framecolor = PLAYER_COLOR_FRAME[playerSeatNumber]

			engine.displaysize = 0
			engine.enableSE = true

			// Apply 1vs1 layout
			if(currentRoomInfo!=null&&currentRoomInfo!!.maxPlayers==2) {
				owner.engine[0].displaysize = 0
				owner.engine[1].displaysize = 0
			}

			if(netLobby!=null&&netLobby!!.netPlayerClient!=null) {
				if(!isReadyChangePending&&numPlayers>=2) {
					// Ready ON
					if(engine.ctrl!!.isPush(Controller.BUTTON_A)&&menuTime>=5&&!isReady[0]&&!currentRoomInfo!!.playing) {
						engine.playSE("decide")
						isReadyChangePending = true
						netLobby!!.netPlayerClient!!.send("ready\ttrue\n")
					}
					// Ready OFF
					if(engine.ctrl!!.isPush(Controller.BUTTON_B)&&menuTime>=5&&isReady[0]&&!currentRoomInfo!!.playing) {
						engine.playSE("change")
						isReadyChangePending = true
						netLobby!!.netPlayerClient!!.send("ready\tfalse\n")
					}
				}

				// Random map preview
				if(currentRoomInfo!=null&&currentRoomInfo!!.useMap&&!netLobby!!.mapList.isEmpty())
					if(menuTime%30==0) {
						engine.statc[5]++
						if(engine.statc[5]>=netLobby!!.mapList.size) engine.statc[5] = 0
						engine.createFieldIfNeeded()
						engine.field!!.stringToField(netLobby!!.mapList[engine.statc[5]])
						engine.field!!.setAllSkin(engine.skin)
						engine.field!!.setAllAttribute(Block.BLOCK_ATTRIBUTE_VISIBLE, true)
						engine.field!!.setAllAttribute(Block.BLOCK_ATTRIBUTE_OUTLINE, true)
						engine.field!!.setAllAttribute(Block.BLOCK_ATTRIBUTE_SELFPLACED, false)
					}

				// Practice mode
				if(engine.ctrl!!.isPush(Controller.BUTTON_F)&&menuTime>=5) {
					engine.playSE("decide")
					startPractice(engine)
				}
			}

			// GC呼び出し
			if(menuTime==0) System.gc()

			menuTime++
		}

		return true
	}

	/* Render the settings screen */
	override fun renderSetting(engine:GameEngine, playerID:Int) {
		if(netLobby==null||netLobby!!.netPlayerClient==null) return
		if(!engine.isVisible) return

		if(currentRoomInfo!=null&&!currentRoomInfo!!.playing) {
			val x = receiver.getFieldDisplayPositionX(engine, playerID)
			val y = receiver.getFieldDisplayPositionY(engine, playerID)

			if(isReady[playerID]&&isPlayerExist[playerID])
				if(engine.displaysize!=-1)
					receiver.drawDirectFont(x+68, y+204, "OK", COLOR.YELLOW)
				else
					receiver.drawDirectFont(x+36, y+80, "OK", COLOR.YELLOW, .5f)

			if(playerID==0&&playerSeatNumber>=0&&!isReadyChangePending&&numPlayers>=2)
				if(!isReady[playerID]) {
					var strTemp = "A("+receiver.getKeyNameByButtonID(engine, Controller.BUTTON_A)+" KEY):"
					if(strTemp.length>10) strTemp = strTemp.substring(0, 10)
					receiver.drawMenuFont(engine, playerID, 0, 16, strTemp, COLOR.CYAN)
					receiver.drawMenuFont(engine, playerID, 1, 17, "READY", COLOR.CYAN)
				} else {
					var strTemp = "B("+receiver.getKeyNameByButtonID(engine, Controller.BUTTON_B)+" KEY):"
					if(strTemp.length>10) strTemp = strTemp.substring(0, 10)
					receiver.drawMenuFont(engine, playerID, 0, 16, strTemp, COLOR.BLUE)
					receiver.drawMenuFont(engine, playerID, 1, 17, "CANCEL", COLOR.BLUE)
				}
		}

		if(playerID==0&&playerSeatNumber>=0) {
			var strTemp = "F("+receiver.getKeyNameByButtonID(engine, Controller.BUTTON_F)+" KEY):"
			if(strTemp.length>10) strTemp = strTemp.substring(0, 10)
			receiver.drawMenuFont(engine, playerID, 0, 18, strTemp, COLOR.PURPLE)
			receiver.drawMenuFont(engine, playerID, 1, 19, "PRACTICE", COLOR.PURPLE)
		}
	}

	/* Ready */
	override fun onReady(engine:GameEngine, playerID:Int):Boolean {
		if(engine.statc[0]==0)
		// Map
			if(currentRoomInfo!!.useMap&&mapNo<netLobby!!.mapList.size&&!isPractice) {
				engine.createFieldIfNeeded()
				engine.field!!.stringToField(netLobby!!.mapList[mapNo])
				if(playerID==0&&playerSeatNumber>=0)
					engine.field!!.setAllSkin(engine.skin)
				else if(rulelockFlag&&netLobby!!.ruleOptLock!=null)
					engine.field!!.setAllSkin(netLobby!!.ruleOptLock!!.skin)
				else if(playerSkin[playerID]>=0) engine.field!!.setAllSkin(playerSkin[playerID])
				engine.field!!.setAllAttribute(Block.BLOCK_ATTRIBUTE_VISIBLE, true)
				engine.field!!.setAllAttribute(Block.BLOCK_ATTRIBUTE_OUTLINE, true)
				engine.field!!.setAllAttribute(Block.BLOCK_ATTRIBUTE_SELFPLACED, false)
			}

		if(isPractice&&engine.statc[0]>=10) isPracticeExitAllowed = true

		return false
	}

	/* Start game */
	override fun startGame(engine:GameEngine, playerID:Int) {
		currentRoomInfo?.let {
			engine.apply {
				speed.gravity = it.gravity
				speed.denominator = it.denominator
				speed.are = it.are
				speed.areLine = it.areLine
				speed.lineDelay = it.lineDelay
				speed.lockDelay = it.lockDelay
				speed.das = it.das
				b2bEnable = it.b2b
				comboType = if(it.combo) GameEngine.COMBO_TYPE_NORMAL else GameEngine.COMBO_TYPE_DISABLE

				spinCheckType = it.spinCheckType
				tspinEnableEZ = it.tspinEnableEZ

				when {
					it.tspinEnableType==0 -> {
						tspinEnable = false
						useAllSpinBonus = false
					}
					it.tspinEnableType==1 -> {
						tspinEnable = true
						useAllSpinBonus = false
					}
					it.tspinEnableType==2 -> {
						tspinEnable = true
						useAllSpinBonus = true
					}
				}
			}
			setNewTarget()
			targetTimer = 0
		}
		if(isPractice)
			owner.bgmStatus.bgm = BGMStatus.BGM.SILENT
		else {
			owner.bgmStatus.bgm = BGMStatus.BGM.EXTRA_1
			owner.bgmStatus.fadesw = false
		}
		pieceMoveTimer = 0
		hurryupCount = 0
		hurryupShowFrames = 0
		hurryupStarted = false
	}

	/* Processing on the move */
	override fun onMove(engine:GameEngine, playerID:Int):Boolean {
		// Start gameImmediately after the occurrence of a new piece
		if(engine.ending==0&&engine.statc[0]==0&&!engine.holdDisable&&
			playerID==0&&playerSeatNumber>=0&&!isPractice) {
			netPlayTimerActive = true
			sendField(engine)
		}

		// Move
		if(engine.ending==0&&playerID==0&&playerSeatNumber>=0&&engine.nowPieceObject!=null&&!isPractice&&
			numPlayers+numSpectators>=2)
			if(engine.nowPieceObject==null&&prevPieceID!=Piece.PIECE_NONE||engine.manualLock) {
				prevPieceID = Piece.PIECE_NONE
				netLobby!!.netPlayerClient!!.send("game\tpiece\t"+prevPieceID+"\t"+prevPieceX+"\t"+prevPieceY+"\t"+prevPieceDir
					+"\t"+
					0+"\t"+engine.skin+"\t"+false+"\n")

				if(numNowPlayers==2&&numMaxPlayers==2) netSendNextAndHold(engine)
			} else if(engine.nowPieceObject!!.id!=prevPieceID||engine.nowPieceX!=prevPieceX||
				engine.nowPieceY!=prevPieceY||engine.nowPieceObject!!.direction!=prevPieceDir) {
				prevPieceID = engine.nowPieceObject!!.id
				prevPieceX = engine.nowPieceX
				prevPieceY = engine.nowPieceY
				prevPieceDir = engine.nowPieceObject!!.direction

				val x = prevPieceX+engine.nowPieceObject!!.dataOffsetX[prevPieceDir]
				val y = prevPieceY+engine.nowPieceObject!!.dataOffsetY[prevPieceDir]
				netLobby!!.netPlayerClient!!.send("game\tpiece\t"+prevPieceID+"\t"+x+"\t"+y+"\t"+prevPieceDir+"\t"+
					engine.nowPieceBottomY+"\t"+engine.ruleopt.pieceColor[prevPieceID]+"\t"+engine.skin+"\t"+
					engine.nowPieceObject!!.big+"\n")

				if(numNowPlayers==2&&numMaxPlayers==2) netSendNextAndHold(engine)
			}

		// Fixed Force
		if(engine.ending==0&&playerID==0&&playerSeatNumber>=0&&engine.nowPieceObject!=null) {
			pieceMoveTimer++
			if(pieceMoveTimer>=PIECE_AUTO_LOCK_TIME) {
				engine.nowPieceY = engine.nowPieceBottomY
				engine.lockDelayNow = engine.lockDelay
			}
		}

		return playerID!=0||playerSeatNumber==-1

	}

	/* Called whenever a piece is locked */
	override fun pieceLocked(engine:GameEngine, playerID:Int, lines:Int) {
		if(engine.ending==0&&playerID==0&&playerSeatNumber>=0) {
			sendField(engine)
			pieceMoveTimer = 0
		}
	}

	/* Line clear */
	override fun onLineClear(engine:GameEngine, playerID:Int):Boolean {
		if(engine.statc[0]==1&&engine.ending==0&&playerID==0&&playerSeatNumber>=0) sendField(engine)
		return false
	}

	/* Calculate score */
	override fun calcScore(engine:GameEngine, playerID:Int, lines:Int) {
		// Attack
		if(lines>0) {
			//int pts = 0;
			val pts = IntArray(ATTACK_CATEGORIES)

			scgettime[playerID] = 0

			val numAliveTeams = numberOfTeamsAlive
			var attackNumPlayerIndex = numAliveTeams-2
			if(isPractice||!reduceLineSend) attackNumPlayerIndex = 0
			if(attackNumPlayerIndex<0) attackNumPlayerIndex = 0
			if(attackNumPlayerIndex>4) attackNumPlayerIndex = 4

			var attackLineIndex = LINE_ATTACK_INDEX_SINGLE
			var mainAttackCategory = ATTACK_CATEGORY_NORMAL

			if(engine.tspin) {
				mainAttackCategory = ATTACK_CATEGORY_SPIN
				if(engine.tspinez) {
					attackLineIndex = LINE_ATTACK_INDEX_EZ_T
					lastevent[playerID] = EVENT_TSPIN_EZ
				} else if(lines==1) {
					if(engine.tspinmini) {
						attackLineIndex = LINE_ATTACK_INDEX_TMINI
						lastevent[playerID] = EVENT_TSPIN_SINGLE_MINI
					} else {
						attackLineIndex = LINE_ATTACK_INDEX_TSINGLE
						lastevent[playerID] = EVENT_TSPIN_SINGLE
					}
				} else if(lines==2) {
					if(engine.tspinmini&&engine.useAllSpinBonus) {
						attackLineIndex = LINE_ATTACK_INDEX_TMINI_D
						lastevent[playerID] = EVENT_TSPIN_DOUBLE_MINI
					} else {
						attackLineIndex = LINE_ATTACK_INDEX_TDOUBLE
						lastevent[playerID] = EVENT_TSPIN_DOUBLE
					}
				} else if(lines>=3) {
					attackLineIndex = LINE_ATTACK_INDEX_TTRIPLE
					lastevent[playerID] = EVENT_TSPIN_TRIPLE
				}// T-Spin 3 lines
				// T-Spin 2 lines
				// T-Spin 1 line
			} else if(lines==1) {
				// 1Column
				attackLineIndex = LINE_ATTACK_INDEX_SINGLE
				lastevent[playerID] = EVENT_SINGLE
			} else if(lines==2) {
				// 2Column
				attackLineIndex = LINE_ATTACK_INDEX_DOUBLE
				lastevent[playerID] = EVENT_DOUBLE
			} else if(lines==3) {
				// 3Column
				attackLineIndex = LINE_ATTACK_INDEX_TRIPLE
				lastevent[playerID] = EVENT_TRIPLE
			} else if(lines>=4) {
				// 4 lines
				attackLineIndex = LINE_ATTACK_INDEX_FOUR
				lastevent[playerID] = EVENT_FOUR
			}

			// Attack force calculation
			//log.debug("attackNumPlayerIndex:" + attackNumPlayerIndex + ", attackLineIndex:" + attackLineIndex);
			if(engine.useAllSpinBonus)
				pts[mainAttackCategory] += LINE_ATTACK_TABLE_ALLSPIN[attackLineIndex][attackNumPlayerIndex]
			else
				pts[mainAttackCategory] += LINE_ATTACK_TABLE[attackLineIndex][attackNumPlayerIndex]

			// B2B
			if(engine.b2b) {
				lastb2b[playerID] = true

				if(pts[mainAttackCategory]>0)
					if(attackLineIndex==LINE_ATTACK_INDEX_TTRIPLE&&!engine.useAllSpinBonus)
						pts[ATTACK_CATEGORY_B2B] += 2
					else
						pts[ATTACK_CATEGORY_B2B] += 1
			} else
				lastb2b[playerID] = false

			// Combo
			if(engine.comboType!=GameEngine.COMBO_TYPE_DISABLE) {
				var cmbindex = engine.combo-1
				if(cmbindex<0) cmbindex = 0
				if(cmbindex>=COMBO_ATTACK_TABLE[attackNumPlayerIndex].size)
					cmbindex = COMBO_ATTACK_TABLE[attackNumPlayerIndex].size-1
				pts[ATTACK_CATEGORY_COMBO] += COMBO_ATTACK_TABLE[attackNumPlayerIndex][cmbindex]
				lastcombo[playerID] = engine.combo
			}

			// All clear
			if(lines>=1&&engine.field!!.isEmpty&&currentRoomInfo!!.bravo)
				pts[ATTACK_CATEGORY_BRAVO] += 6

			// gem block attack
			pts[ATTACK_CATEGORY_GEM] += engine.field!!.howManyGemClears

			lastpiece[playerID] = engine.nowPieceObject!!.id

			for(i in pts.indices)
				pts[i] *= GARBAGE_DENOMINATOR
			if(useFractionalGarbage&&!isPractice)
				if(numAliveTeams>=3)
					for(i in pts.indices)
						pts[i] = pts[i]/(numAliveTeams-1)

			// Attack lines count
			for(i in pts)
				garbageSent[playerID] += i

			// Offset
			garbage[playerID] = totalGarbageLines
			for(i in pts.indices)
				if(pts[i]>0&&garbage[playerID]>0&&currentRoomInfo!!.counter)
					while((!useFractionalGarbage
							&&!garbageEntries!!.isEmpty()&&pts[i]>0)||useFractionalGarbage&&!garbageEntries!!.isEmpty()&&pts[i]>=GARBAGE_DENOMINATOR) {
						val garbageEntry = garbageEntries!!.first
						garbageEntry.lines -= pts[i]

						if(garbageEntry.lines<=0) {
							pts[i] = Math.abs(garbageEntry.lines)
							garbageEntries!!.removeFirst()
						} else
							pts[i] = 0
					}

			//  Attack
			if(!isPractice&&numPlayers+numSpectators>=2) {
				garbage[playerID] = totalGarbageLines

				val stringPts = StringBuilder()
				for(i in pts)
					stringPts.append(i).append("\t")

				if(targetID!=-1&&!isTargetable(targetID)) setNewTarget()
				val targetSeatID = if(targetID==-1) -1 else allPlayerSeatNumbers[targetID]

				netLobby!!.netPlayerClient!!.send("game\tattack\t"+stringPts+"\t"+lastevent[playerID]+"\t"+lastb2b[playerID]
					+"\t"+
					lastcombo[playerID]+"\t"+garbage[playerID]+"\t"+lastpiece[playerID]+"\t"+targetSeatID+"\n")
			}
		}

		// Rising auction
		if((lines==0||!currentRoomInfo!!.rensaBlock)&&totalGarbageLines>=GARBAGE_DENOMINATOR&&!isPractice) {
			engine.playSE("garbage")

			var smallGarbageCount = 0 // 10ptsLess thangarbage blockcountThe totalcount(Overcall together later)
			var hole = lastHole
			var newHole:Int
			if(hole==-1) hole = engine.random.nextInt(engine.field!!.width)

			var finalGarbagePercent = garbagePercent
			if(divideChangeRateByPlayers) finalGarbagePercent /= numberOfTeamsAlive-1

			while(!garbageEntries!!.isEmpty()) {
				val garbageEntry = garbageEntries!!.poll()
				smallGarbageCount += garbageEntry.lines%GARBAGE_DENOMINATOR

				if(garbageEntry.lines/GARBAGE_DENOMINATOR>0) {
					val seatFrom = allPlayerSeatNumbers[garbageEntry.playerID]
					val garbageColor = if(seatFrom<0) Block.BLOCK_COLOR_GRAY else PLAYER_COLOR_BLOCK[seatFrom]
					lastAttackerUID = garbageEntry.uid
					if(garbageChangePerAttack) {
						if(engine.random.nextInt(100)<finalGarbagePercent) {
							newHole = engine.random.nextInt(engine.field!!.width-1)
							if(newHole>=hole) newHole++
							hole = newHole
						}
						engine.field!!.addSingleHoleGarbage(hole, garbageColor, engine.skin, garbageEntry.lines/GARBAGE_DENOMINATOR)
					} else
						for(i in garbageEntry.lines/GARBAGE_DENOMINATOR downTo 1) {
							if(engine.random.nextInt(100)<finalGarbagePercent) {
								newHole = engine.random.nextInt(engine.field!!.width-1)
								if(newHole>=hole) newHole++
								hole = newHole
							}

							engine.field!!.addSingleHoleGarbage(hole, garbageColor, engine.skin, 1)
						}
				}
			}

			if(smallGarbageCount>0) {
				// 10ptsAll overcall or more parts

				//int hole = engine.random.nextInt(engine.field.getWidth());
				//engine.field.addSingleHoleGarbage(hole, Block.BLOCK_COLOR_GRAY, engine.getSkin(),
				//		  Block.BLOCK_ATTRIBUTE_GARBAGE | Block.BLOCK_ATTRIBUTE_VISIBLE | Block.BLOCK_ATTRIBUTE_OUTLINE,
				//		  smallGarbageCount / GARBAGE_DENOMINATOR);

				if(smallGarbageCount/GARBAGE_DENOMINATOR>0) {
					lastAttackerUID = -1

					if(garbageChangePerAttack) {
						if(engine.random.nextInt(100)<finalGarbagePercent) {
							newHole = engine.random.nextInt(engine.field!!.width-1)
							if(newHole>=hole) newHole++
							hole = newHole
						}
						engine.field!!.addSingleHoleGarbage(hole, Block.BLOCK_COLOR_GRAY, engine.skin, smallGarbageCount/GARBAGE_DENOMINATOR)
					} else
						for(i in smallGarbageCount/GARBAGE_DENOMINATOR downTo 1) {
							if(engine.random.nextInt(100)<finalGarbagePercent) {
								newHole = engine.random.nextInt(engine.field!!.width-1)
								if(newHole>=hole) newHole++
								hole = newHole
							}

							engine.field!!.addSingleHoleGarbage(hole, Block.BLOCK_COLOR_GRAY, engine.skin, 1)
						}

				}
				// 10ptsBrought forward under the next
				if(smallGarbageCount%GARBAGE_DENOMINATOR>0) {
					val smallGarbageEntry = GarbageEntry(smallGarbageCount%GARBAGE_DENOMINATOR, -1)
					garbageEntries!!.add(smallGarbageEntry)
				}
			}

			lastHole = hole
		}

		// HURRY UP!
		if(hurryupSeconds>=0&&engine.timerActive&&!isPractice)
			if(hurryupStarted) {
				hurryupCount++

				if(hurryupCount%hurryupInterval==0) engine.field!!.addHurryupFloor(1, engine.skin)
			} else
				hurryupCount = hurryupInterval-1
	}

	/* ARE */
	override fun onARE(engine:GameEngine, playerID:Int):Boolean {
		if(engine.statc[0]==0&&engine.ending==0&&playerID==0&&playerSeatNumber>=0) {
			sendField(engine)
			if(numNowPlayers==2&&numMaxPlayers==2) netSendNextAndHold(engine)
		}
		return false
	}

	/* Called after every frame */
	override fun onLast(engine:GameEngine, playerID:Int) {
		scgettime[playerID]++
		if(playerID==0&&hurryupShowFrames>0) hurryupShowFrames--

		// HURRY UP!
		if(playerID==0&&engine.timerActive&&hurryupSeconds>=0&&engine.statistics.time==hurryupSeconds*60&&
			!isPractice&&!hurryupStarted) {
			netLobby!!.netPlayerClient!!.send("game\thurryup\n")
			owner.receiver.playSE("hurryup")
			hurryupStarted = true
			hurryupShowFrames = 60*5
		}

		// Rising auctionMeter
		val tempGarbage = garbage[playerID]/GARBAGE_DENOMINATOR
		val tempGarbageF = garbage[playerID].toFloat()/GARBAGE_DENOMINATOR
		val newMeterValue = (tempGarbageF*receiver.getBlockGraphicsHeight(engine)).toInt()
		if(playerID==0&&playerSeatNumber!=-1) {
			if(newMeterValue>engine.meterValue) {
				engine.meterValue += receiver.getBlockGraphicsHeight(engine)/2
				if(engine.meterValue>newMeterValue) engine.meterValue = newMeterValue
			} else if(newMeterValue<engine.meterValue) engine.meterValue--
		} else
			engine.meterValue = newMeterValue
		if(tempGarbage>=4)
			engine.meterColor = GameEngine.METER_COLOR_RED
		else if(tempGarbage>=3)
			engine.meterColor = GameEngine.METER_COLOR_ORANGE
		else if(tempGarbage>=1)
			engine.meterColor = GameEngine.METER_COLOR_YELLOW
		else
			engine.meterColor = GameEngine.METER_COLOR_GREEN

		// APL & APM
		if(playerID==0&&engine.gameActive&&engine.timerActive) {
			val tempGarbageSent = garbageSent[playerID].toFloat()/GARBAGE_DENOMINATOR
			playerAPM[0] = tempGarbageSent*3600/engine.statistics.time

			if(engine.statistics.lines>0)
				playerAPL[0] = tempGarbageSent/engine.statistics.lines
			else
				playerAPL[0] = 0f
		}

		// Timer
		if(playerID==0&&netPlayTimerActive) netPlayTimer++

		// Target
		if(playerID==0&&playerSeatNumber>=0&&netPlayTimerActive&&engine.gameActive&&engine.timerActive&&
			numberOfPossibleTargets>=1&&currentRoomInfo!=null&&currentRoomInfo!!.isTarget) {
			targetTimer++

			if(targetTimer>=currentRoomInfo!!.targetTimer||!isTargetable(targetID)) {
				targetTimer = 0
				setNewTarget()
			}
		}

		// Automatically start timer
		if(playerID==0&&currentRoomInfo!=null&&autoStartActive&&!isNetGameActive)
			if(numPlayers<=1)
				autoStartActive = false
			else if(autoStartTimer>0)
				autoStartTimer--
			else {
				if(playerSeatNumber!=-1) netLobby!!.netPlayerClient!!.send("autostart\n")
				autoStartTimer = 0
				autoStartActive = false
			}

		// End practice mode
		if(playerID==0&&isPractice&&isPracticeExitAllowed&&engine.ctrl!!.isPush(Controller.BUTTON_F)) {
			engine.timerActive = false
			owner.bgmStatus.bgm = BGMStatus.BGM.SILENT
			isPracticeExitAllowed = false

			if(isPractice) {
				isPractice = false
				engine.field!!.reset()
				engine.gameEnded()
				engine.stat = GameEngine.Status.SETTING
				engine.resetStatc()
			} else {
				engine.stat = GameEngine.Status.GAMEOVER
				engine.resetStatc()
			}
		}

		/* if(currentRoomID == -1) {
		 * engine.isVisible = false;
		 * } else if((netLobby.netClient != null) && (currentRoomID != -1)) {
		 * NetRoomInfo roomInfo = netLobby.netClient.getRoomInfo(currentRoomID);
		 * if((roomInfo == null) || (playerID >= roomInfo.maxPlayers)) {
		 * engine.isVisible = false;
		 * }
		 * } */
	}

	/* Drawing processing at the end of every frame */
	override fun renderLast(engine:GameEngine, playerID:Int) {
		// Number of players
		if(playerID==players-1&&netLobby!=null&&netLobby!!.netPlayerClient!=null&&netLobby!!.netPlayerClient!!.isConnected
			&&
			(!owner.engine[1].isVisible||owner.engine[1].displaysize==-1||!isNetGameActive)) {
			var x = if(owner.receiver.nextDisplayType==2) 544 else 503
			if(owner.receiver.nextDisplayType==2&&numMaxPlayers==2) x = 321

			if(currentRoomID!=-1) {
				receiver.drawDirectFont(x, 286, "PLAYERS", COLOR.CYAN, .5f)
				receiver.drawDirectFont(x, 294, (""+numPlayers), COLOR.WHITE, .5f)
				receiver.drawDirectFont(x, 302, "SPECTATORS", COLOR.CYAN, .5f)
				receiver.drawDirectFont(x, 310, (""+numSpectators), COLOR.WHITE, .5f)
				receiver.drawDirectFont(x, 318, "MATCHES", COLOR.CYAN, .5f)
				receiver.drawDirectFont(x, 326, (""+numGames), COLOR.WHITE, .5f)
				receiver.drawDirectFont(x, 334, "WINS", COLOR.CYAN, .5f)
				receiver.drawDirectFont(x, 342, (""+numWins), COLOR.WHITE, .5f)
			}
			receiver.drawDirectFont(x, 358, "ALL ROOMS", COLOR.GREEN, .5f)
			receiver.drawDirectFont(x, 366, (""+netLobby!!.netPlayerClient!!.roomInfoList.size), COLOR.WHITE, .5f)
		}

		// All number of players
		if(playerID==players-1) super.renderLast(engine, playerID)

		// Course time
		if(playerID==0&&currentRoomID!=-1) {
			receiver.drawDirectFont(256, 16, GeneralUtil.getTime(netPlayTimer.toFloat()))

			if(hurryupSeconds>=0&&hurryupShowFrames>0&&!isPractice&&hurryupStarted)
				receiver.drawDirectFont(playerID, 256-8, 32, "HURRY UP!", hurryupShowFrames%2==0)
		}

		if(isPlayerExist[playerID]&&engine.isVisible) {
			val x = receiver.getFieldDisplayPositionX(engine, playerID)
			val y = receiver.getFieldDisplayPositionY(engine, playerID)

			// Name
			if(playerNames!=null&&playerNames[playerID]!=null&&playerNames[playerID].isNotEmpty()) {
				var name = playerNames[playerID]
				var fontcolorNum = playerTeamColors[playerID]
				if(fontcolorNum<0) fontcolorNum = 0
				if(fontcolorNum>TEAM_FONT_COLORS.size-1) fontcolorNum = TEAM_FONT_COLORS.size-1
				val fontcolor = TEAM_FONT_COLORS[fontcolorNum]

				if(engine.displaysize==-1) {
					if(name.length>7) name = name.substring(0, 7)+".."
					receiver.drawDirectTTF(x, y-16, name, fontcolor)
				} else if(playerID==0) {
					if(name.length>14) name = name.substring(0, 14)+".."
					receiver.drawDirectTTF(x, y-20, name, fontcolor)
				} else
					receiver.drawDirectTTF(x, y-20, name, fontcolor)
			}

			// garbage blockcount
			if(garbage[playerID]>0&&useFractionalGarbage&&engine.stat!=GameEngine.Status.RESULT) {
				val strTempGarbage:String

				var fontColor = COLOR.WHITE
				if(garbage[playerID]>=GARBAGE_DENOMINATOR) fontColor = COLOR.YELLOW
				if(garbage[playerID]>=GARBAGE_DENOMINATOR*3) fontColor = COLOR.ORANGE
				if(garbage[playerID]>=GARBAGE_DENOMINATOR*4) fontColor = COLOR.RED

				if(engine.displaysize!=-1) {
					//strTempGarbage = String.format(Locale.ROOT, "%5.2f", (float)garbage[playerID] / GARBAGE_DENOMINATOR);
					strTempGarbage = String.format(Locale.US, "%5.2f", garbage[playerID].toFloat()/GARBAGE_DENOMINATOR)
					receiver.drawDirectFont(x+96, y+372, strTempGarbage, fontColor, 1f)
				} else {
					//strTempGarbage = String.format(Locale.ROOT, "%4.1f", (float)garbage[playerID] / GARBAGE_DENOMINATOR);
					strTempGarbage = String.format(Locale.US, "%4.1f", garbage[playerID].toFloat()/GARBAGE_DENOMINATOR)
					receiver.drawDirectFont(x+64, y+168, strTempGarbage, fontColor, .5f)
				}
			}
		}

		// Practice mode
		if(playerID==0&&(isPractice||numNowPlayers==1)&&isPracticeExitAllowed) {
			if(lastevent[playerID]==EVENT_NONE||scgettime[playerID]>=120)
				receiver.drawMenuFont(engine, 0, 0, 21, "F("
					+receiver.getKeyNameByButtonID(engine, Controller.BUTTON_F)
					+" KEY):\n END GAME", COLOR.PURPLE)

			if(isPractice&&engine.timerActive)
				receiver.drawDirectFont(256, 32, GeneralUtil.getTime(engine.statistics.time.toFloat()), COLOR.PURPLE)
		}

		// Automatically start timer
		if(playerID==0&&currentRoomInfo!=null&&autoStartActive
			&&!isNetGameActive)
			receiver.drawDirectFont(496, 16, GeneralUtil.getTime(autoStartTimer.toFloat()),
				if(currentRoomInfo!!.autoStartTNET2) COLOR.RED else COLOR.YELLOW)

		// Target
		if(playerID==targetID&&currentRoomInfo!=null&&currentRoomInfo!!.isTarget&&numAlivePlayers>=3&&
			isNetGameActive&&!isDead[playerID]) {
			val x = receiver.getFieldDisplayPositionX(engine, playerID)
			val y = receiver.getFieldDisplayPositionY(engine, playerID)
			var fontcolor = COLOR.GREEN
			if(targetTimer>=currentRoomInfo!!.targetTimer-20&&targetTimer%2==0)
				fontcolor = COLOR.WHITE

			if(engine.displaysize!=-1)
				receiver.drawMenuFont(engine, playerID, 2, 12, "TARGET", fontcolor)
			else
				receiver.drawDirectFont(x+4+16, y+80, "TARGET", fontcolor, .5f)
		}

		// Line clear event
		if(lastevent[playerID]!=EVENT_NONE&&scgettime[playerID]<120) {
			val strPieceName = Piece.Shape.names[lastpiece[playerID]]

			if(engine.displaysize!=-1) {
				when(lastevent[playerID]) {
					EVENT_SINGLE -> receiver.drawMenuFont(engine, playerID, 2, 21, "SINGLE", COLOR.COBALT)
					EVENT_DOUBLE -> receiver.drawMenuFont(engine, playerID, 2, 21, "DOUBLE", COLOR.BLUE)
					EVENT_TRIPLE -> receiver.drawMenuFont(engine, playerID, 2, 21, "TRIPLE", COLOR.GREEN)
					EVENT_FOUR -> if(lastb2b[playerID])
						receiver.drawMenuFont(engine, playerID, 3, 21, "FOUR", COLOR.RED)
					else
						receiver.drawMenuFont(engine, playerID, 3, 21, "FOUR", COLOR.ORANGE)
					EVENT_TSPIN_SINGLE_MINI -> if(lastb2b[playerID])
						receiver.drawMenuFont(engine, playerID, 1, 21, "$strPieceName-MINI-S", COLOR.RED)
					else
						receiver.drawMenuFont(engine, playerID, 1, 21, "$strPieceName-MINI-S", COLOR.ORANGE)
					EVENT_TSPIN_SINGLE -> if(lastb2b[playerID])
						receiver.drawMenuFont(engine, playerID, 1, 21, "$strPieceName-SINGLE", COLOR.RED)
					else
						receiver.drawMenuFont(engine, playerID, 1, 21, "$strPieceName-SINGLE", COLOR.ORANGE)
					EVENT_TSPIN_DOUBLE_MINI -> if(lastb2b[playerID])
						receiver.drawMenuFont(engine, playerID, 1, 21, "$strPieceName-MINI-D", COLOR.RED)
					else
						receiver.drawMenuFont(engine, playerID, 1, 21, "$strPieceName-MINI-D", COLOR.ORANGE)
					EVENT_TSPIN_DOUBLE -> if(lastb2b[playerID])
						receiver.drawMenuFont(engine, playerID, 1, 21, "$strPieceName-DOUBLE", COLOR.RED)
					else
						receiver.drawMenuFont(engine, playerID, 1, 21, "$strPieceName-DOUBLE", COLOR.ORANGE)
					EVENT_TSPIN_TRIPLE -> if(lastb2b[playerID])
						receiver.drawMenuFont(engine, playerID, 1, 21, "$strPieceName-TRIPLE", COLOR.RED)
					else
						receiver.drawMenuFont(engine, playerID, 1, 21, "$strPieceName-TRIPLE", COLOR.ORANGE)
					EVENT_TSPIN_EZ -> if(lastb2b[playerID])
						receiver.drawMenuFont(engine, playerID, 3, 21, "EZ-$strPieceName", COLOR.RED)
					else
						receiver.drawMenuFont(engine, playerID, 3, 21, "EZ-$strPieceName", COLOR.ORANGE)
				}

				if(lastcombo[playerID]>=2)
					receiver.drawMenuFont(engine, playerID, 2, 22, (lastcombo[playerID]-1).toString()+"COMBO", COLOR.CYAN)
			} else {
				val x = receiver.getFieldDisplayPositionX(engine, playerID)
				val y = receiver.getFieldDisplayPositionY(engine, playerID)
				var x2 = 8
				if(useFractionalGarbage&&garbage[playerID]>0) x2 = 0

				when(lastevent[playerID]) {
					EVENT_SINGLE -> receiver.drawDirectFont(x+4+16, y+168, "SINGLE", COLOR.COBALT, .5f)
					EVENT_DOUBLE -> receiver.drawDirectFont(x+4+16, y+168, "DOUBLE", COLOR.BLUE, .5f)
					EVENT_TRIPLE -> receiver.drawDirectFont(x+4+16, y+168, "TRIPLE", COLOR.GREEN, .5f)
					EVENT_FOUR -> if(lastb2b[playerID])
						receiver.drawDirectFont(x+4+24, y+168, "FOUR", COLOR.RED, .5f)
					else
						receiver.drawDirectFont(x+4+24, y+168, "FOUR", COLOR.ORANGE, .5f)
					EVENT_TSPIN_SINGLE_MINI -> if(lastb2b[playerID])
						receiver.drawDirectFont(x+4+x2, y+168, "$strPieceName-MINI-S", COLOR.RED, .5f)
					else
						receiver.drawDirectFont(x+4+x2, y+168, "$strPieceName-MINI-S", COLOR.ORANGE, .5f)
					EVENT_TSPIN_SINGLE -> if(lastb2b[playerID])
						receiver.drawDirectFont(x+4+x2, y+168, "$strPieceName-SINGLE", COLOR.RED, .5f)
					else
						receiver.drawDirectFont(x+4+x2, y+168, "$strPieceName-SINGLE", COLOR.ORANGE, .5f)
					EVENT_TSPIN_DOUBLE_MINI -> if(lastb2b[playerID])
						receiver.drawDirectFont(x+4+x2, y+168, "$strPieceName-MINI-D", COLOR.RED, .5f)
					else
						receiver.drawDirectFont(x+4+x2, y+168, "$strPieceName-MINI-D", COLOR.ORANGE, .5f)
					EVENT_TSPIN_DOUBLE -> if(lastb2b[playerID])
						receiver.drawDirectFont(x+4+x2, y+168, "$strPieceName-DOUBLE", COLOR.RED, .5f)
					else
						receiver.drawDirectFont(x+4+x2, y+168, "$strPieceName-DOUBLE", COLOR.ORANGE, .5f)
					EVENT_TSPIN_TRIPLE -> if(lastb2b[playerID])
						receiver.drawDirectFont(x+4+x2, y+168, "$strPieceName-TRIPLE", COLOR.RED, .5f)
					else
						receiver.drawDirectFont(x+4+x2, y+168, "$strPieceName-TRIPLE", COLOR.ORANGE, .5f)
					EVENT_TSPIN_EZ -> if(lastb2b[playerID])
						receiver.drawDirectFont(x+4+24, y+168, "EZ-$strPieceName", COLOR.RED, .5f)
					else
						receiver.drawDirectFont(x+4+24, y+168, "EZ-$strPieceName", COLOR.ORANGE, .5f)
				}

				if(lastcombo[playerID]>=2)
					receiver.drawDirectFont(x+4+16, y+176, ((lastcombo[playerID]-1).toString()+"COMBO"), COLOR.CYAN, .5f)
			}
		} else if(isPlayerExist[playerID]&&engine.isVisible&&!isPractice) {
			val strTemp = playerWinCount[playerID].toString()+"/"+playerGamesCount[playerID]

			if(engine.displaysize!=-1) {
				var y = 21
				if(engine.stat==GameEngine.Status.RESULT) y = 22
				receiver.drawMenuFont(engine, playerID, 0, y, strTemp, COLOR.WHITE)
			} else {
				val x = receiver.getFieldDisplayPositionX(engine, playerID)
				val y = receiver.getFieldDisplayPositionY(engine, playerID)
				receiver.drawDirectFont(x+4, y+168, strTemp, COLOR.WHITE, .5f)
			}
		}// Games count
	}

	/* game over */
	override fun onGameOver(engine:GameEngine, playerID:Int):Boolean {
		engine.gameEnded()
		engine.allowTextRenderByReceiver = false
		isPracticeExitAllowed = false

		if(playerID==0&&isPractice)
			return if(engine.statc[0]<engine.field!!.height+1)
				false
			else {
				engine.field!!.reset()
				engine.stat = GameEngine.Status.RESULT
				engine.resetStatc()
				true
			}

		if(playerID==0&&!isDead[playerID]) {
			owner.bgmStatus.bgm = BGMStatus.BGM.SILENT
			engine.resetFieldVisible()

			sendField(engine)
			if(numNowPlayers==2&&numMaxPlayers==2) netSendNextAndHold(engine)
			netLobby!!.netPlayerClient!!.send("dead\t$lastAttackerUID\n")

			engine.stat = GameEngine.Status.CUSTOM
			engine.resetStatc()
			return true
		}

		if(isDead[playerID]) {
			if(playerPlace[playerID]<=2&&playerID==0&&playerSeatNumber>=0) engine.statistics.time = netPlayTimer
			if(engine.field==null) {
				engine.stat = GameEngine.Status.SETTING
				engine.resetStatc()
				return true
			}
			return engine.statc[0]>=engine.field!!.height+1&&!isPlayerResultReceived[playerID]
		}

		return true
	}

	/* game overDraw the screen */
	override fun renderGameOver(engine:GameEngine, playerID:Int) {
		if(playerID==0&&isPractice) return
		if(!engine.isVisible) return

		val x = receiver.getFieldDisplayPositionX(engine, playerID)
		val y = receiver.getFieldDisplayPositionY(engine, playerID)
		val place = playerPlace[playerID]

		if(engine.displaysize!=-1) {
			if(isReady[playerID]&&!isNetGameActive)
				receiver.drawDirectFont(x+68, y+204, "OK", COLOR.YELLOW)
			else if(numNowPlayers==2&&isDead[playerID])
				receiver.drawDirectFont(x+52, y+204, "LOSE", COLOR.WHITE)
			else if(place==1) {
				//receiver.drawDirect(x + 12, y + 204, "GAME OVER", EventReceiver.COLOR.WHITE);
			} else if(place==2)
				receiver.drawDirectFont(x+12, y+204, "2ND PLACE", COLOR.WHITE)
			else if(place==3)
				receiver.drawDirectFont(x+12, y+204, "3RD PLACE", COLOR.RED)
			else if(place==4)
				receiver.drawDirectFont(x+12, y+204, "4TH PLACE", COLOR.GREEN)
			else if(place==5)
				receiver.drawDirectFont(x+12, y+204, "5TH PLACE", COLOR.BLUE)
			else if(place==6)
				receiver.drawDirectFont(x+12, y+204, "6TH PLACE", COLOR.PURPLE)

			if(playerKObyYou[playerID])
				receiver.drawDirectFont(x+52, y+236, "K.O.", COLOR.PINK)
		} else {
			if(isReady[playerID]&&!isNetGameActive)
				receiver.drawDirectFont(x+36, y+80, "OK", COLOR.YELLOW, .5f)
			else if(numNowPlayers==2||currentRoomInfo!!.maxPlayers==2)
				receiver.drawDirectFont(x+28, y+80, "LOSE", COLOR.WHITE, .5f)
			else if(place==1) {
				//receiver.drawDirect(x + 8, y + 80, "GAME OVER", EventReceiver.COLOR.WHITE, .5f);
			} else if(place==2)
				receiver.drawDirectFont(x+8, y+80, "2ND PLACE", COLOR.WHITE, .5f)
			else if(place==3)
				receiver.drawDirectFont(x+8, y+80, "3RD PLACE", COLOR.RED, .5f)
			else if(place==4)
				receiver.drawDirectFont(x+8, y+80, "4TH PLACE", COLOR.GREEN, .5f)
			else if(place==5)
				receiver.drawDirectFont(x+8, y+80, "5TH PLACE", COLOR.BLUE, .5f)
			else if(place==6)
				receiver.drawDirectFont(x+8, y+80, "6TH PLACE", COLOR.PURPLE, .5f)

			if(playerKObyYou[playerID])
				receiver.drawDirectFont(x+28,
					y+96, "K.O.", COLOR.PINK, .5f)
		}
	}

	/* After being defeated */
	override fun onCustom(engine:GameEngine, playerID:Int):Boolean {
		if(!isNetGameActive) {
			isDead[playerID] = true
			engine.stat = GameEngine.Status.GAMEOVER
			engine.resetStatc()
		}
		return false
	}

	/* EXCELLENTScreen processing */
	override fun onExcellent(engine:GameEngine, playerID:Int):Boolean {
		engine.gameEnded()
		engine.allowTextRenderByReceiver = false

		if(engine.statc[0]==0) {
			//if((playerID == 0) && (playerSeatNumber != -1)) numWins++;
			owner.bgmStatus.bgm = BGMStatus.BGM.SILENT
			if(engine.ai!=null) engine.ai!!.shutdown(engine, playerID)
			engine.resetFieldVisible()
			engine.playSE("excellent")
		}

		if(engine.statc[0]>=120&&engine.ctrl!!.isPush(Controller.BUTTON_A)) engine.statc[0] = engine.field!!.height+1+180

		if(engine.statc[0]>=engine.field!!.height+1+180&&!isNetGameActive&&isPlayerResultReceived[playerID]) {
			if(engine.field!=null) engine.field!!.reset()
			engine.resetStatc()
			engine.stat = GameEngine.Status.RESULT
		} else
			engine.statc[0]++

		return true
	}

	/* EXCELLENTProcess of drawing the screen */
	override fun renderExcellent(engine:GameEngine, playerID:Int) {
		if(!engine.isVisible) return

		val x = receiver.getFieldDisplayPositionX(engine, playerID)
		val y = receiver.getFieldDisplayPositionY(engine, playerID)

		if(engine.displaysize!=-1) {
			if(isReady[playerID]&&!isNetGameActive)
				receiver.drawDirectFont(x+68, y+204, "OK", COLOR.YELLOW)
			else if(numNowPlayers==2||currentRoomInfo!!.maxPlayers==2)
				receiver.drawDirectFont(x+52, y+204, "WIN!", COLOR.YELLOW)
			else
				receiver.drawDirectFont(x+4, y+204, "1ST PLACE!", COLOR.YELLOW)
		} else if(isReady[playerID]&&!isNetGameActive)
			receiver.drawDirectFont(x+36,
				y+80, "OK", COLOR.YELLOW, .5f)
		else if(numNowPlayers==2||currentRoomInfo!!.maxPlayers==2)
			receiver.drawDirectFont(x+28,
				y+80, "WIN!", COLOR.YELLOW, .5f)
		else
			receiver.drawDirectFont(x+4, y+80, "1ST PLACE!", COLOR.YELLOW, .5f)
	}

	/* Processing of the results screen */
	override fun onResult(engine:GameEngine, playerID:Int):Boolean {
		engine.allowTextRenderByReceiver = false

		// To the setting screen
		if(engine.ctrl!!.isPush(Controller.BUTTON_A)&&!isNetGameActive&&playerID==0) {
			engine.playSE("decide")
			resetFlags()
			owner.reset()
		}
		// Practice mode
		if(engine.ctrl!!.isPush(Controller.BUTTON_F)&&playerID==0) {
			engine.playSE("decide")
			startPractice(engine)
		}

		return true
	}

	/* Render results screenProcessing */
	override fun renderResult(engine:GameEngine, playerID:Int) {
		var scale = 1f
		if(engine.displaysize==-1) scale = .5f

		if(!isPractice) {
			receiver.drawMenuFont(engine, playerID, 0, 0, "RESULT", COLOR.ORANGE, scale)
			if(playerPlace[playerID]==1) {
				if(numNowPlayers==2)
					receiver.drawMenuFont(engine, playerID, 6, 1, "WIN!", COLOR.YELLOW, scale)
				else if(numNowPlayers>2) receiver.drawMenuFont(engine, playerID, 6, 1, "1ST!", COLOR.YELLOW, scale)
			} else if(playerPlace[playerID]==2) {
				if(numNowPlayers==2)
					receiver.drawMenuFont(engine, playerID, 6, 1, "LOSE", COLOR.WHITE, scale)
				else
					receiver.drawMenuFont(engine, playerID, 7, 1, "2ND", COLOR.WHITE, scale)
			} else if(playerPlace[playerID]==3)
				receiver.drawMenuFont(engine, playerID, 7, 1, "3RD", COLOR.RED, scale)
			else if(playerPlace[playerID]==4)
				receiver.drawMenuFont(engine, playerID, 7, 1, "4TH", COLOR.GREEN, scale)
			else if(playerPlace[playerID]==5)
				receiver.drawMenuFont(engine, playerID, 7, 1, "5TH", COLOR.BLUE, scale)
			else if(playerPlace[playerID]==6)
				receiver.drawMenuFont(engine, playerID, 7, 1, "6TH", COLOR.COBALT, scale)
		} else
			receiver.drawMenuFont(engine, playerID, 0, 0, "PRACTICE", COLOR.PINK, scale)

		drawResultScale(engine, playerID, receiver, 2, COLOR.ORANGE, scale, "ATTACK", String.format("%10g",
			garbageSent[playerID].toFloat()/GARBAGE_DENOMINATOR), "LINE", String.format("%10d", engine.statistics.lines), "PIECE", String.format("%10d", engine.statistics.totalPieceLocked), "ATK/LINE", String.format("%10g", playerAPL[playerID]), "ATTACK/MIN", String.format("%10g", playerAPM[playerID]), "LINE/MIN", String.format("%10g", engine.statistics.lpm), "PIECE/SEC", String.format("%10g", engine.statistics.pps), "TIME", String.format("%10s", GeneralUtil.getTime(engine.statistics.time.toFloat())))

		if(!isNetGameActive&&playerSeatNumber>=0&&playerID==0) {
			var strTemp = "A("+receiver.getKeyNameByButtonID(engine, Controller.BUTTON_A)+" KEY):"
			if(strTemp.length>10) strTemp = strTemp.substring(0, 10)
			receiver.drawMenuFont(engine, playerID, 0, 18, strTemp, COLOR.RED)
			receiver.drawMenuFont(engine, playerID, 1, 19, "RESTART", COLOR.RED)
		}

		if(playerSeatNumber>=0&&playerID==0) {
			var strTempF = "F("+receiver.getKeyNameByButtonID(engine, Controller.BUTTON_F)+" KEY):"
			if(strTempF.length>10) strTempF = strTempF.substring(0, 10)
			receiver.drawMenuFont(engine, playerID, 0, 20, strTempF, COLOR.PURPLE)
			if(!isPractice)
				receiver.drawMenuFont(engine, playerID, 1, 21, "PRACTICE", COLOR.PURPLE)
			else
				receiver.drawMenuFont(engine, playerID, 1, 21, "RETRY", COLOR.PURPLE)
		}
	}

	/** No retry key. */
	override fun netplayOnRetryKey(engine:GameEngine, playerID:Int) {}

	override fun netlobbyOnDisconnect(lobby:NetLobbyFrame, client:NetPlayerClient, ex:Throwable?) {
		for(i in 0 until players)
			owner.engine[i].stat = GameEngine.Status.NOTHING
	}

	override fun netlobbyOnMessage(lobby:NetLobbyFrame, client:NetPlayerClient, message:Array<String>) {
		// PlayerState change
		if(message[0]=="playerupdate") {
			val pInfo = NetPlayerInfo(message[1])

			if(pInfo.roomID==currentRoomID&&pInfo.seatID!=-1) {
				val playerID = getPlayerIDbySeatID(pInfo.seatID)

				if(isReady[playerID]!=pInfo.ready) {
					isReady[playerID] = pInfo.ready

					if(playerID==0&&playerSeatNumber!=-1)
						isReadyChangePending = false
					else if(pInfo.ready)
						receiver.playSE("decide")
					else if(!pInfo.playing) receiver.playSE("change")
				}
			}

			updatePlayerExist()
			updatePlayerNames()
		}
		// PlayerCut
		if(message[0]=="playerlogout") {
			val pInfo = NetPlayerInfo(message[1])

			if(pInfo.roomID==currentRoomID&&pInfo.seatID!=-1) {
				updatePlayerExist()
				updatePlayerNames()
			}
		}
		// Participation status change
		if(message[0]=="changestatus") {
			val uid = Integer.parseInt(message[2])

			if(uid==netLobby!!.netPlayerClient!!.playerUID) {
				playerSeatNumber = client.yourPlayerInfo!!.seatID
				isReady[0] = false

				updatePlayerExist()
				updatePlayerNames()

				if(playerSeatNumber>=0) {
					// Participation in a war
					owner.engine[0].displaysize = 0
					owner.engine[0].enableSE = true
					for(i in 1 until players)
						owner.engine[i].displaysize = -1
				} else
				// Spectator
					for(i in 0 until players) {
						owner.engine[i].displaysize = -1
						owner.engine[i].enableSE = false
					}

				// Apply 1vs1 layout
				if(currentRoomInfo!=null&&currentRoomInfo!!.maxPlayers==2) {
					owner.engine[0].displaysize = 0
					owner.engine[1].displaysize = 0
				}

				isPractice = false
				owner.engine[0].stat = GameEngine.Status.SETTING

				for(i in 0 until players) {
					if(owner.engine[i].field!=null) owner.engine[i].field!!.reset()
					owner.engine[i].nowPieceObject = null
					garbage[i] = 0

					if(owner.engine[i].stat==GameEngine.Status.NOTHING||isNetGameFinished)
						owner.engine[i].stat = GameEngine.Status.SETTING
					owner.engine[i].resetStatc()
				}
			} else if(message[1]=="watchonly") {
				val seatID = Integer.parseInt(message[4])
				val playerID = getPlayerIDbySeatID(seatID)
				isPlayerExist[playerID] = false
				isReady[playerID] = false
				garbage[playerID] = 0
			}
		}
		// I came someone
		if(message[0]=="playerenter") {
			val seatID = Integer.parseInt(message[3])
			if(seatID!=-1&&numPlayers<2) owner.receiver.playSE("levelstop")
		}
		// I went out someone
		if(message[0]=="playerleave") {
			val seatID = Integer.parseInt(message[3])

			if(seatID!=-1) {
				val playerID = getPlayerIDbySeatID(seatID)
				isPlayerExist[playerID] = false
				isReady[playerID] = false
				garbage[playerID] = 0

				numPlayers--
				if(numPlayers<2) {
					isReady[0] = false
					autoStartActive = false
				}
			}
		}
		// Automatic timer start
		if(message[0]=="autostartbegin")
			if(numPlayers>=2) {
				val seconds = Integer.parseInt(message[1])
				autoStartTimer = seconds*60
				autoStartActive = true
			}
		// Automatic timer stop
		if(message[0]=="autostartstop") autoStartActive = false
		// game start
		if(message[0]=="start") {
			val randseed = java.lang.Long.parseLong(message[1], 16)
			numNowPlayers = Integer.parseInt(message[2])
			if(numNowPlayers>=2&&playerSeatNumber!=-1) numGames++
			numAlivePlayers = numNowPlayers
			mapNo = Integer.parseInt(message[3])

			resetFlags()
			owner.reset()

			autoStartActive = false
			isNetGameActive = true
			netPlayTimer = 0

			if(currentRoomInfo!=null) currentRoomInfo!!.playing = true

			if(playerSeatNumber!=-1&&!rulelockFlag
				&&netLobby!!.ruleOptPlayer!=null)
				owner.engine[0].ruleopt.copy(netLobby!!.ruleOptPlayer) // Restore rules

			updatePlayerExist()
			updatePlayerNames()

			log.debug("Game Started numNowPlayers:$numNowPlayers numMaxPlayers:$numMaxPlayers mapNo:$mapNo")

			for(i in 0 until players) {
				val engine = owner.engine[i]
				engine.resetStatc()

				if(isPlayerExist[i]) {
					playerActive[i] = true
					engine.stat = GameEngine.Status.READY
					engine.randSeed = randseed
					engine.random = Random(randseed)

					if(numMaxPlayers==2&&numNowPlayers==2) {
						engine.isVisible = true
						engine.displaysize = 0

						if(rulelockFlag||i==0&&playerSeatNumber!=-1) {
							engine.isNextVisible = true
							engine.isHoldVisible = true

							if(i!=0) engine.randomizer = owner.engine[0].randomizer
						} else {
							engine.isNextVisible = false
							engine.isHoldVisible = false
						}
					}
				} else if(i<numMaxPlayers) {
					engine.stat = GameEngine.Status.SETTING
					engine.isVisible = true
					engine.isNextVisible = false
					engine.isHoldVisible = false

					if(numMaxPlayers==2&&numNowPlayers==2) engine.isVisible = false
				} else {
					engine.stat = GameEngine.Status.SETTING
					engine.isVisible = false
				}

				isDead[i] = false
				isReady[i] = false
			}
		}
		// Death
		if(message[0]=="dead") {
			val seatID = Integer.parseInt(message[3])
			val playerID = getPlayerIDbySeatID(seatID)
			var koUID = -1
			if(message.size>5) koUID = Integer.parseInt(message[5])

			//			if((useTankMode == true) && (playerTeamsIsTank[playerID] == true)) {
			//				String teamName = playerTeams[playerID];
			//				for(int i = 0; i < MAX_PLAYERS; i++ ){
			//					if((playerTeams[i].length() > 0) && (isPlayerExist[i]) && (playerTeams[i].equals(teamName))) {
			//						if(i != playerID) {
			//							playerTeamsIsTank[i] = true;
			//						}
			//					}
			//
			//				}
			//				isTank = true;
			//			}

			if(!isDead[playerID]) {
				isDead[playerID] = true
				playerPlace[playerID] = Integer.parseInt(message[4])
				owner.engine[playerID].gameEnded()
				owner.engine[playerID].stat = GameEngine.Status.GAMEOVER
				owner.engine[playerID].resetStatc()
				numAlivePlayers--

				if(koUID==netLobby!!.netPlayerClient!!.playerUID) {
					playerKObyYou[playerID] = true
					currentKO++
				}
				if(seatID==playerSeatNumber&&playerSeatNumber!=-1) sendGameStat(owner.engine[playerID], playerID)
			}
		}
		// Game Stats
		if(message[0]=="gstat") recvGameStat(message)
		// game finished
		if(message[0]=="finish") {
			log.debug("Game Finished")

			isNetGameActive = false
			isNetGameFinished = true
			isNewcomer = false
			netPlayTimerActive = false

			if(currentRoomInfo!=null) currentRoomInfo!!.playing = false

			if(isPractice) {
				isPractice = false
				owner.bgmStatus.bgm = BGMStatus.BGM.SILENT
				owner.engine[0].gameEnded()
				owner.engine[0].stat = GameEngine.Status.SETTING
				owner.engine[0].resetStatc()
			}

			val flagTeamWin = java.lang.Boolean.parseBoolean(message[4])

			if(flagTeamWin) {
				//String strTeam = NetUtil.urlDecode(message[3]);
				for(i in 0 until MAX_PLAYERS)
					if(isPlayerExist[i]&&!isDead[i]) {
						playerPlace[i] = 1
						owner.engine[i].gameEnded()
						owner.engine[i].stat = GameEngine.Status.EXCELLENT
						owner.engine[i].resetStatc()
						owner.engine[i].statistics.time = netPlayTimer
						numAlivePlayers--

						if(i==0&&playerSeatNumber!=-1) {
							numWins++
							sendGameStat(owner.engine[i], i)
						}
					}
			} else {
				val seatID = Integer.parseInt(message[2])
				if(seatID!=-1) {
					val playerID = getPlayerIDbySeatID(seatID)
					if(isPlayerExist[playerID]) {
						playerPlace[playerID] = 1
						owner.engine[playerID].gameEnded()
						owner.engine[playerID].stat = GameEngine.Status.EXCELLENT
						owner.engine[playerID].resetStatc()
						owner.engine[playerID].statistics.time = netPlayTimer
						numAlivePlayers--

						if(seatID==playerSeatNumber&&playerSeatNumber!=-1) {
							numWins++
							sendGameStat(owner.engine[playerID], playerID)
						}
					}
				}
			}

			if(playerSeatNumber==-1||playerPlace[0]>=3) owner.receiver.playSE("matchend")

			updatePlayerExist()
			updatePlayerNames()
		}
		// game messages
		if(message[0]=="game") {
			val uid = Integer.parseInt(message[1])
			val seatID = Integer.parseInt(message[2])
			val playerID = getPlayerIDbySeatID(seatID)

			if(owner.engine[playerID].field==null) owner.engine[playerID].field = Field()

			// Field without attributes
			if(message[3]=="field")
				if(message.size>7) {
					owner.engine[playerID].nowPieceObject = null
					owner.engine[playerID].holdDisable = false
					garbage[playerID] = Integer.parseInt(message[4])
					val skin = Integer.parseInt(message[5])
					val highestGarbageY = Integer.parseInt(message[6])
					val highestWallY = Integer.parseInt(message[7])
					playerSkin[playerID] = skin
					if(message.size>9) {
						var strFieldData = message[8]
						val isCompressed = java.lang.Boolean.parseBoolean(message[9])
						if(isCompressed) strFieldData = NetUtil.decompressString(strFieldData)
						owner.engine[playerID].field!!.stringToField(strFieldData, skin, highestGarbageY, highestWallY)
					} else
						owner.engine[playerID].field!!.reset()
				}
			// Field with attributes
			if(message[3]=="fieldattr")
				if(message.size>5) {
					owner.engine[playerID].nowPieceObject = null
					owner.engine[playerID].holdDisable = false
					garbage[playerID] = Integer.parseInt(message[4])
					val skin = Integer.parseInt(message[5])
					playerSkin[playerID] = skin
					if(message.size>7) {
						var strFieldData = message[6]
						val isCompressed = java.lang.Boolean.parseBoolean(message[7])
						if(isCompressed) strFieldData = NetUtil.decompressString(strFieldData)
						owner.engine[playerID].field!!.attrStringToField(strFieldData, skin)
					} else
						owner.engine[playerID].field!!.reset()
				}
			// During operationBlock
			if(message[3]=="piece") {
				val id = Integer.parseInt(message[4])

				if(id>=0) {
					val pieceX = Integer.parseInt(message[5])
					val pieceY = Integer.parseInt(message[6])
					val pieceDir = Integer.parseInt(message[7])
					//int pieceBottomY = Integer.parseInt(message[8]);
					val pieceColor = Integer.parseInt(message[9])
					val pieceSkin = Integer.parseInt(message[10])
					val pieceBig = message.size>11&&java.lang.Boolean.parseBoolean(message[11])

					owner.engine[playerID].nowPieceObject = Piece(id)
					owner.engine[playerID].nowPieceObject!!.direction = pieceDir
					owner.engine[playerID].nowPieceObject!!.setAttribute(Block.BLOCK_ATTRIBUTE_VISIBLE, true)
					owner.engine[playerID].nowPieceObject!!.setColor(pieceColor)
					owner.engine[playerID].nowPieceObject!!.setSkin(pieceSkin)
					owner.engine[playerID].nowPieceX = pieceX
					owner.engine[playerID].nowPieceY = pieceY
					//owner.engine[playerID].nowPieceBottomY = pieceBottomY;
					owner.engine[playerID].nowPieceObject!!.big = pieceBig
					owner.engine[playerID].nowPieceObject!!.updateConnectData()
					owner.engine[playerID].nowPieceBottomY =
						owner.engine[playerID].nowPieceObject!!.getBottom(pieceX, pieceY, owner.engine[playerID].field)

					if(owner.engine[playerID].stat!=GameEngine.Status.EXCELLENT) {
						owner.engine[playerID].stat = GameEngine.Status.MOVE
						owner.engine[playerID].statc[0] = 2
					}

					playerSkin[playerID] = pieceSkin
				} else
					owner.engine[playerID].nowPieceObject = null

				if(playerSeatNumber==-1&&!netPlayTimerActive&&!isNetGameFinished&&!isNewcomer) {
					netPlayTimerActive = true
					netPlayTimer = 0
				}

				if(playerSeatNumber!=-1&&netPlayTimerActive&&!isPractice&&
					owner.engine[0].stat==GameEngine.Status.READY
					&&owner.engine[0].statc[0]<owner.engine[0].goEnd)
					owner.engine[0].statc[0] = owner.engine[0].goEnd
			}
			//			if((message[3].equals("tank")) && (useTankMode == true)){
			//				String teamName = playerTeams[playerID];
			//				for(int i = 0; i < MAX_PLAYERS; i++ ){
			//					if((playerTeams[i].length() > 0) && (isPlayerExist[i]) && (playerTeams[i].equals(teamName))) {
			//						if(i != playerID) {
			//							playerTeamsIsTank[i] = false;
			//						}
			//					}
			//
			//				}
			//				isTank = true;
			//				playerTeamsIsTank[playerID] = true;
			//			}
			//  Attack
			if(message[3]=="attack") {
				//int pts = Integer.parseInt(message[4]);
				val pts = IntArray(ATTACK_CATEGORIES)
				var sumPts = 0

				for(i in 0 until ATTACK_CATEGORIES) {
					pts[i] = Integer.parseInt(message[4+i])
					sumPts += pts[i]
				}

				lastevent[playerID] = Integer.parseInt(message[ATTACK_CATEGORIES+5])
				lastb2b[playerID] = java.lang.Boolean.parseBoolean(message[ATTACK_CATEGORIES+6])
				lastcombo[playerID] = Integer.parseInt(message[ATTACK_CATEGORIES+7])
				garbage[playerID] = Integer.parseInt(message[ATTACK_CATEGORIES+8])
				lastpiece[playerID] = Integer.parseInt(message[ATTACK_CATEGORIES+9])
				scgettime[playerID] = 0
				val targetSeatID = Integer.parseInt(message[ATTACK_CATEGORIES+10])

				if(playerSeatNumber!=-1&&owner.engine[0].timerActive&&sumPts>0&&!isPractice&&!isNewcomer&&
					(targetSeatID==-1||playerSeatNumber==targetSeatID||!currentRoomInfo!!.isTarget)&&
					(playerTeams[0].isEmpty()||playerTeams[playerID].isEmpty()
						||!playerTeams[0].equals(playerTeams[playerID], ignoreCase = true)))
				//				if( (playerSeatNumber != -1) && (owner.engine[0].timerActive) && (sumPts > 0) && (!isPractice) && (!isNewcomer) &&
				//					((targetSeatID == -1) || (playerSeatNumber == targetSeatID) || (!currentRoomInfo.isTarget)) &&
				//				    ((playerTeams[0].length() <= 0) || (playerTeams[playerID].length() <= 0) || (!playerTeams[0].equalsIgnoreCase(playerTeams[playerID]))) &&
				//				    (playerTeamsIsTank[0]) )
				{
					var secondAdd = 0 //TODO: Allow for chunking of attack types other than b2b.
					if(currentRoomInfo!!.b2bChunk) secondAdd = pts[ATTACK_CATEGORY_B2B]

					var garbageEntry = GarbageEntry(sumPts-secondAdd, playerID, uid)
					garbageEntries!!.add(garbageEntry)

					if(secondAdd>0) {
						garbageEntry = GarbageEntry(secondAdd, playerID, uid)
						garbageEntries!!.add(garbageEntry)
					}

					garbage[0] = totalGarbageLines
					if(garbage[0]>=4*GARBAGE_DENOMINATOR) owner.engine[0].playSE("danger")
					netLobby!!.netPlayerClient!!.send("game\tgarbageupdate\t"+garbage[0]+"\n")
				}
			}
			// Update bar rising auction
			if(message[3]=="garbageupdate") garbage[playerID] = Integer.parseInt(message[4])
			// NEXT and HOLD
			if(message[3]=="next") {
				val maxNext = Integer.parseInt(message[4])
				owner.engine[playerID].ruleopt.nextDisplay = maxNext
				owner.engine[playerID].holdDisable = java.lang.Boolean.parseBoolean(message[5])

				for(i in 0 until maxNext+1)
					if(i+6<message.size) {
						val strPieceData = message[i+6].split(";".toRegex()).dropLastWhile {it.isEmpty()}.toTypedArray()
						val pieceID = Integer.parseInt(strPieceData[0])
						val pieceDirection = Integer.parseInt(strPieceData[1])
						val pieceColor = Integer.parseInt(strPieceData[2])

						if(i==0) {
							if(pieceID==Piece.PIECE_NONE)
								owner.engine[playerID].holdPieceObject = null
							else {
								owner.engine[playerID].holdPieceObject = Piece(pieceID).apply {
									direction = pieceDirection
									setColor(pieceColor)
									setSkin(playerSkin[playerID])
									updateConnectData()
								}
							}
						} else {
							if(owner.engine[playerID].nextPieceArrayObject==null||owner.engine[playerID].nextPieceArrayObject.size<maxNext)
								owner.engine[playerID].nextPieceArrayObject = arrayOfNulls(maxNext)
							owner.engine[playerID].nextPieceArrayObject[i-1] = Piece(pieceID).apply {
								direction = pieceDirection
								setColor(pieceColor)
								setSkin(playerSkin[playerID])
								updateConnectData()
							}
						}
					}

				owner.engine[playerID].isNextVisible = true
				owner.engine[playerID].isHoldVisible = true
			}
			// HurryUp
			if(message[3]=="hurryup")
				if(!hurryupStarted&&hurryupSeconds>0) {
					if(playerSeatNumber!=-1&&owner.engine[0].timerActive) owner.receiver.playSE("hurryup")
					hurryupStarted = true
					hurryupShowFrames = 60*5
				}
		}
	}

	/** I was sent from the enemygarbage blockOf data */
	private inner class GarbageEntry {
		/** garbage blockcount */
		var lines = 0

		/** Source(For gamesPlayer number) */
		var playerID = 0

		/** Source(For non-gamePlayer number) */
		var uid = 0

		/** Constructor */
		constructor()

		/** With parametersConstructor
		 * @param g garbage blockcount
		 */
		constructor(g:Int) {
			lines = g
		}

		/** With parametersConstructor
		 * @param g garbage blockcount
		 * @param p Source(For gamesPlayer number)
		 */
		constructor(g:Int, p:Int) {
			lines = g
			playerID = p
		}

		/** With parametersConstructor
		 * @param g garbage blockcount
		 * @param p Source(For gamesPlayer number)
		 * @param s Source(For non-gamePlayer number)
		 */
		constructor(g:Int, p:Int, s:Int) {
			lines = g
			playerID = p
			uid = s
		}
	}

	companion object {
		/** Log */
		internal val log = Logger.getLogger(LegacyNetVSBattleMode::class.java)

		/** Maximum number of players */
		private const val MAX_PLAYERS = 6

		/** Most recent scoring event type constants */
		private const val EVENT_NONE = 0
		private const val EVENT_SINGLE = 1
		private const val EVENT_DOUBLE = 2
		private const val EVENT_TRIPLE = 3
		private const val EVENT_FOUR = 4
		private const val EVENT_TSPIN_SINGLE_MINI = 5
		private const val EVENT_TSPIN_SINGLE = 6
		private const val EVENT_TSPIN_DOUBLE = 7
		private const val EVENT_TSPIN_TRIPLE = 8
		private const val EVENT_TSPIN_DOUBLE_MINI = 9
		private const val EVENT_TSPIN_EZ = 10

		/** Type of attack performed */
		private const val ATTACK_CATEGORY_NORMAL = 0
		private const val ATTACK_CATEGORY_B2B = 1
		private const val ATTACK_CATEGORY_SPIN = 2
		private const val ATTACK_CATEGORY_COMBO = 3
		private const val ATTACK_CATEGORY_BRAVO = 4
		private const val ATTACK_CATEGORY_GEM = 5
		private const val ATTACK_CATEGORIES = 6

		/** Numbers of seats numbers corresponding to frames on player's screen */
		private val GAME_SEAT_NUMBERS =
			arrayOf(intArrayOf(0, 1, 2, 3, 4, 5), intArrayOf(1, 0, 2, 3, 4, 5), intArrayOf(1, 2, 0, 3, 4, 5), intArrayOf(1, 2, 3, 0, 4, 5), intArrayOf(1, 2, 3, 4, 0, 5), intArrayOf(1, 2, 3, 4, 5, 0))

		/** Each player's garbage block cint */
		private val PLAYER_COLOR_BLOCK =
			intArrayOf(Block.BLOCK_COLOR_RED, Block.BLOCK_COLOR_BLUE, Block.BLOCK_COLOR_GREEN, Block.BLOCK_COLOR_YELLOW, Block.BLOCK_COLOR_PURPLE, Block.BLOCK_COLOR_CYAN)

		/** Each player's frame cint */
		private val PLAYER_COLOR_FRAME =
			intArrayOf(GameEngine.FRAME_COLOR_RED, GameEngine.FRAME_COLOR_BLUE, GameEngine.FRAME_COLOR_GREEN, GameEngine.FRAME_COLOR_BRONZE, GameEngine.FRAME_COLOR_PURPLE, GameEngine.FRAME_COLOR_CYAN)

		/** Team font colors */
		private val TEAM_FONT_COLORS =
			arrayOf(COLOR.WHITE, COLOR.RED, COLOR.GREEN, COLOR.BLUE, COLOR.YELLOW, COLOR.PURPLE, COLOR.CYAN)

		/** Time before forced piece lock */
		private const val PIECE_AUTO_LOCK_TIME = 60*60

		/** Attack table (for T-Spin only) */
		private val LINE_ATTACK_TABLE = arrayOf(
			// 1-2P, 3P, 4P, 5P, 6P
			intArrayOf(0, 0, 0, 0, 0), // Single
			intArrayOf(1, 1, 0, 0, 0), // Double
			intArrayOf(2, 2, 1, 1, 1), // Triple
			intArrayOf(4, 3, 2, 2, 2), // Four
			intArrayOf(1, 1, 0, 0, 0), // T-Mini-S
			intArrayOf(2, 2, 1, 1, 1), // T-Single
			intArrayOf(4, 3, 2, 2, 2), // T-Double
			intArrayOf(6, 4, 3, 3, 3), // T-Triple
			intArrayOf(4, 3, 2, 2, 2), // T-Mini-D
			intArrayOf(1, 1, 0, 0, 0))// EZ-T

		/** Attack table(for All Spin) */
		private val LINE_ATTACK_TABLE_ALLSPIN = arrayOf(
			// 1-2P, 3P, 4P, 5P, 6P
			intArrayOf(0, 0, 0, 0, 0), // Single
			intArrayOf(1, 1, 0, 0, 0), // Double
			intArrayOf(2, 2, 1, 1, 1), // Triple
			intArrayOf(4, 3, 2, 2, 2), // Four
			intArrayOf(0, 0, 0, 0, 0), // T-Mini-S
			intArrayOf(2, 2, 1, 1, 1), // T-Single
			intArrayOf(4, 3, 2, 2, 2), // T-Double
			intArrayOf(6, 4, 3, 3, 3), // T-Triple
			intArrayOf(3, 2, 1, 1, 1), // T-Mini-D
			intArrayOf(0, 0, 0, 0, 0))// EZ-T

		/** Indexes of attack types in attack table */
		private const val LINE_ATTACK_INDEX_SINGLE = 0
		private const val LINE_ATTACK_INDEX_DOUBLE = 1
		private const val LINE_ATTACK_INDEX_TRIPLE = 2
		private const val LINE_ATTACK_INDEX_FOUR = 3
		private const val LINE_ATTACK_INDEX_TMINI = 4
		private const val LINE_ATTACK_INDEX_TSINGLE = 5
		private const val LINE_ATTACK_INDEX_TDOUBLE = 6
		private const val LINE_ATTACK_INDEX_TTRIPLE = 7
		private const val LINE_ATTACK_INDEX_TMINI_D = 8
		private const val LINE_ATTACK_INDEX_EZ_T = 9

		/** Combo attack table */
		private val COMBO_ATTACK_TABLE = arrayOf(intArrayOf(0, 0, 1, 1, 2, 2, 3, 3, 4, 4, 4, 5), // 1-2 Player(s)
			intArrayOf(0, 0, 1, 1, 1, 2, 2, 3, 3, 4, 4, 4), // 3 Player
			intArrayOf(0, 0, 0, 1, 1, 1, 2, 2, 3, 3, 4, 4), // 4 Player
			intArrayOf(0, 0, 0, 1, 1, 1, 1, 2, 2, 3, 3, 4), // 5 Player
			intArrayOf(0, 0, 0, 0, 1, 1, 1, 1, 2, 2, 3, 3))// 6 Payers

		private const val GARBAGE_DENOMINATOR = 60 // can be divided by 2,3,4,5
	}
}
