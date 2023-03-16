/*
 * Copyright (c) 2010-2023, NullNoname
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
import mu.nu.nullpo.game.component.Block
import mu.nu.nullpo.game.component.Controller
import mu.nu.nullpo.game.event.EventReceiver.COLOR
import mu.nu.nullpo.game.net.NetPlayerClient
import mu.nu.nullpo.game.net.NetPlayerInfo
import mu.nu.nullpo.game.net.NetRoomInfo
import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.game.play.GameManager
import mu.nu.nullpo.gui.net.NetLobbyFrame
import mu.nu.nullpo.util.GeneralUtil
import mu.nu.nullpo.util.GeneralUtil.toTimeStr
import java.io.IOException
import kotlin.random.Random

/** Special base class for netplay VS modes. Up to 6 players supported. */
abstract class NetDummyVSMode:NetDummyMode() {
	/* -------------------- Variables -------------------- */
	/** NET-VS: Local player's seat ID (-1:Spectator) */
	private var netVSMySeatID = 0

	/** NET-VS: Number of players */
	private var netVSNumPlayers = 0

	/** NET-VS: Number of players in current game */
	private var netVSNumNowPlayers = 0

	/** NET-VS: Number of players still alive in current game */
	internal var netVSNumAlivePlayers = 0

	/** NET-VS: Player exist flag */
	internal var netVSPlayerExist = BooleanArray(0)

	/** NET-VS: Player ready flag */
	private var netVSPlayerReady = BooleanArray(0)

	/** NET-VS: Player dead flag */
	internal var netVSPlayerDead = BooleanArray(0)

	/** NET-VS: Player active flag (false if newcomer) */
	private var netVSPlayerActive = BooleanArray(0)

	/** NET-VS: Player's Seat ID array (-1:No Player) */
	internal var netVSPlayerSeatID = IntArray(0)

	/** NET-VS: Player's UID array (-1:No Player) */
	internal var netVSPlayerUID = IntArray(0)

	/** NET-VS: Player's place */
	internal var netVSPlayerPlace = IntArray(0)

	/** NET-VS: Player's win count */
	internal var netVSPlayerWinCount = IntArray(0)

	/** NET-VS: Player's game count */
	internal var netVSPlayerPlayCount = IntArray(0)

	/** NET-VS: Player's team colors */
	private var netVSPlayerTeamColor = IntArray(0)

	/** NET-VS: Player names */
	private var netVSPlayerName:Array<String> = emptyArray()

	/** NET-VS: Player team names */
	private var netVSPlayerTeam:Array<String> = emptyArray()

	/** NET-VS: Player's skins */
	internal var netVSPlayerSkin = IntArray(0)

	/** NET-VS: true if it's ready to show player's result */
	internal var netVSPlayerResultReceived = BooleanArray(0)

	/** NET-VS: true if automatic start timer is activated */
	private var netVSAutoStartTimerActive = false

	/** NET-VS: Time left until the game starts automatically */
	private var netVSAutoStartTimer = 0

	/** NET-VS: true if room game is in progress */
	internal var netVSIsGameActive = false

	/** NET-VS: true if room game is finished */
	private var netVSIsGameFinished = false

	/** NET-VS: true if waiting for ready status change */
	private var netVSIsReadyChangePending = false

	/** NET-VS: true if waiting for dead status change */
	private var netVSIsDeadPending = false

	/** NET-VS: true if local player joined game in progress */
	internal var netVSIsNewcomer = false

	/** NET-VS: Elapsed timer active flag */
	internal var netVSPlayTimerActive = false

	/** NET-VS: Elapsed time */
	internal var netVSPlayTimer = 0

	/** NET-VS: true if practice mode */
	internal var netVSIsPractice = false

	/** NET-VS: true if you can exit from practice game */
	internal var netVSIsPracticeExitAllowed = false

	/** NET-VS: How long current piece is active */
	private var netVSPieceMoveTimer = 0

	/** NET-VS: Time before forced piece lock */
	private var netVSPieceMoveTimerMax = 0

	/** NET-VS: Map number to use */
	private var netVSMapNo = 0

	/** NET-VS: Random for selecting values in Practice mode */
	private var netVSRandMap:Random? = null

	/** NET-VS: Practice mode last used values number */
	private var netVSMapPreviousPracticeMap = 0

	/** NET-VS: UID of player who attacked local player last (-1: Suicide or
	 * Unknown) */
	internal var netVSLastAttackerUID = 0

	/* Mode Name */
	override val name = "NET-VS-DUMMY"

	override val isVSMode:Boolean
		get() = true

	/** NET-VS: Number of players */
	override val players:Int
		get() = NET_MAX_PLAYERS

	/** NET-VS: This is netplay-only mode */
	override val isOnlineMode:Boolean
		get() = true

	/** NET-VS: Mode Initialization */
	override fun modeInit(manager:GameManager) {
		super.modeInit(manager)
		log.debug("modeInit() on NetDummyVSMode")
		netForceSendMovements = true
		netVSMySeatID = -1
		netVSNumPlayers = 0
		netVSNumNowPlayers = 0
		netVSNumAlivePlayers = 0
		netVSPlayerExist = BooleanArray(NET_MAX_PLAYERS)
		netVSPlayerReady = BooleanArray(NET_MAX_PLAYERS)
		netVSPlayerActive = BooleanArray(NET_MAX_PLAYERS)
		netVSPlayerSeatID = IntArray(NET_MAX_PLAYERS)
		netVSPlayerUID = IntArray(NET_MAX_PLAYERS)
		netVSPlayerWinCount = IntArray(NET_MAX_PLAYERS)
		netVSPlayerPlayCount = IntArray(NET_MAX_PLAYERS)
		netVSPlayerTeamColor = IntArray(NET_MAX_PLAYERS)
		netVSPlayerName = Array(NET_MAX_PLAYERS) {""}
		netVSPlayerTeam = Array(NET_MAX_PLAYERS) {""}
		netVSPlayerSkin = IntArray(NET_MAX_PLAYERS) {-1}
		netVSAutoStartTimerActive = false
		netVSAutoStartTimer = 0
		netVSPieceMoveTimerMax = NET_PIECE_AUTO_LOCK_TIME
		netVSMapPreviousPracticeMap = -1
		netVSResetFlags()
	}

	/** NET-VS: Init some variables */
	private fun netVSResetFlags() {
		netVSPlayerResultReceived = BooleanArray(NET_MAX_PLAYERS)
		netVSPlayerDead = BooleanArray(NET_MAX_PLAYERS)
		netVSPlayerPlace = IntArray(NET_MAX_PLAYERS)
		netVSIsGameActive = false
		netVSIsGameFinished = false
		netVSIsReadyChangePending = false
		netVSIsDeadPending = false
		netVSIsNewcomer = false
		netVSPlayTimerActive = false
		netVSPlayTimer = 0
		netVSIsPractice = false
		netVSIsPracticeExitAllowed = false
		netVSPieceMoveTimer = 0
	}

	/** NET-VS: Initialization for each player */
	override fun playerInit(engine:GameEngine) {
		netPlayerInit(engine)
	}

	/** @return true if watch mode
	 */
	internal fun netVSIsWatch():Boolean {
		return try {
			netLobby!!.netPlayerClient!!.yourPlayerInfo!!.seatID==-1
		} catch(e:Exception) {
			false
		}
	}

	/** NET-VS: Update player variables */
	override fun netUpdatePlayerExist() {
		netVSMySeatID = netLobby!!.netPlayerClient!!.yourPlayerInfo!!.seatID
		netVSNumPlayers = 0
		netNumSpectators = 0
		netPlayerName = netLobby!!.netPlayerClient!!.playerName
		netIsWatch = netVSIsWatch()

		for(i in 0 until NET_MAX_PLAYERS) {
			netVSPlayerExist[i] = false
			netVSPlayerReady[i] = false
			netVSPlayerActive[i] = false
			netVSPlayerSeatID[i] = -1
			netVSPlayerUID[i] = -1
			netVSPlayerWinCount[i] = 0
			netVSPlayerPlayCount[i] = 0
			netVSPlayerName[i] = ""
			netVSPlayerTeam[i] = ""
			owner.engine[i].frameColor = GameEngine.FRAME_COLOR_GRAY
		}

		val pList = netLobby!!.updateSameRoomPlayerInfoList()
		val teamList = mutableListOf<String>()

		for(pInfo in pList)
			if(pInfo.roomID==netCurrentRoomInfo!!.roomID)
				if(pInfo.seatID==-1)
					netNumSpectators++
				else {
					netVSNumPlayers++

					val playerID = netVSGetPlayerIDbySeatID(pInfo.seatID)
					netVSPlayerExist[playerID] = true
					netVSPlayerReady[playerID] = pInfo.ready
					netVSPlayerActive[playerID] = pInfo.playing
					netVSPlayerSeatID[playerID] = pInfo.seatID
					netVSPlayerUID[playerID] = pInfo.uid
					netVSPlayerWinCount[playerID] = pInfo.winCountNow
					netVSPlayerPlayCount[playerID] = pInfo.playCountNow
					netVSPlayerName[playerID] = pInfo.strName
					netVSPlayerTeam[playerID] = pInfo.strTeam

					// Set frame cint
					if(pInfo.seatID<NET_PLAYER_COLOR_FRAME.size)
						owner.engine[playerID].frameColor = NET_PLAYER_COLOR_FRAME[pInfo.seatID]

					// Set team cint
					if(netVSPlayerTeam[playerID].isNotEmpty())
						if(!teamList.contains(netVSPlayerTeam[playerID])) {
							teamList.add(netVSPlayerTeam[playerID])
							netVSPlayerTeamColor[playerID] = teamList.size
						} else
							netVSPlayerTeamColor[playerID] = teamList.indexOf(netVSPlayerTeam[playerID])+1
				}
	}

	/** NET-VS: When you join the room */
	override fun netOnJoin(lobby:NetLobbyFrame, client:NetPlayerClient?, roomInfo:NetRoomInfo?) {
		log.debug("netOnJoin() on NetDummyVSMode")

		netCurrentRoomInfo = roomInfo
		netIsNetPlay = true
		netVSIsNewcomer = netCurrentRoomInfo!!.playing

		netUpdatePlayerExist()
		netVSSetLockedRule()
		netVSSetGameScreenLayout()

		if(netVSIsNewcomer) netVSNumNowPlayers = netVSNumPlayers
	}

	/** NET-VS: Initialize various NetPlay variables.
	 * Usually called from [playerInit]. */
	override fun netPlayerInit(engine:GameEngine) {
		log.debug("netPlayerInit(engine#${engine.playerID}) on NetDummyVSMode")

		super.netPlayerInit(engine)

		// Misc. variables
		engine.fieldWidth = 10
		engine.fieldHeight = 20
		engine.dieAll = false
		engine.allowTextRenderByReceiver = true
	}

	/** NET-VS: Draw player's name */
	override fun netDrawPlayerName(engine:GameEngine) {
		val playerID = engine.playerID
		val x = receiver.fieldX(engine)
		val y = receiver.fieldY(engine)

		if(netVSPlayerName[playerID].isNotEmpty()) {
			var name = netVSPlayerName[playerID]
			var fontcolorNum = netVSPlayerTeamColor[playerID]
			if(fontcolorNum<0) fontcolorNum = 0
			if(fontcolorNum>NET_TEAM_FONT_COLORS.size-1) fontcolorNum = NET_TEAM_FONT_COLORS.size-1
			val fontcolor = NET_TEAM_FONT_COLORS[fontcolorNum]

			when {
				engine.displaySize==-1 -> {
					if(name.length>7) name = name.take(7)+".."
					receiver.drawDirectTTF(x, y-16, name, fontcolor)
				}
				playerID==0 -> {
					if(name.length>14) name = name.take(14)+".."
					receiver.drawDirectTTF(x, y-20, name, fontcolor)
				}
				else -> receiver.drawDirectTTF(x, y-20, name, fontcolor)
			}
		}
	}

	/** NET-VS: Send field to everyone. It won't do anything in practice
	 * game. */
	override fun netSendField(engine:GameEngine) {
		if(!netVSIsPractice&&engine.playerID==0&&!netIsWatch) super.netSendField(engine)
	}

	/** NET-VS: Send next and hold piece information to everyone.
	 * It won't do anything in practice game. */
	override fun netSendNextAndHold(engine:GameEngine) {
		if(!netVSIsPractice&&engine.playerID==0&&!netIsWatch) super.netSendNextAndHold(engine)
	}

	/** NET-VS: Send the current piece's movement to everyone.
	 * It won't do anything in practice game. */
	override fun netSendPieceMovement(engine:GameEngine, forceSend:Boolean):Boolean =
		if(!netVSIsPractice&&engine.playerID==0&&!netIsWatch) super.netSendPieceMovement(engine, forceSend) else false

	/** NET-VS: Set locked rule/Revert to user rule */
	private fun netVSSetLockedRule() {
		if(netCurrentRoomInfo!=null&&netCurrentRoomInfo!!.ruleLock) {
			// Set to locked rule
			if(netLobby!=null&&netLobby!!.ruleOptLock!=null) {
				val randomizer = GeneralUtil.loadRandomizer(netLobby!!.ruleOptLock!!.strRandomizer)
				val wallkick = GeneralUtil.loadWallkick(netLobby!!.ruleOptLock!!.strWallkick)
				for(i in 0 until players) {
					owner.engine[i].ruleOpt.replace(netLobby!!.ruleOptLock)
					owner.engine[i].randomizer = randomizer
					owner.engine[i].wallkick = wallkick
				}
			} else
				log.warn("Tried to set locked rule, but rule was not received yet!")
		} else if(!netVSIsWatch()) {
			// Revert rules
			owner.engine[0].ruleOpt.replace(netLobby!!.ruleOptPlayer)
			owner.engine[0].randomizer = GeneralUtil.loadRandomizer(owner.engine[0].ruleOpt.strRandomizer)
			owner.engine[0].wallkick = GeneralUtil.loadWallkick(owner.engine[0].ruleOpt.strWallkick)
		}
	}

	/** Set game screen layout */
	private fun netVSSetGameScreenLayout() {
		for(i in 0 until players)
			netVSSetGameScreenLayout(owner.engine[i])
	}

	/** Set game screen layout
	 * @param engine GameEngine
	 */
	private fun netVSSetGameScreenLayout(engine:GameEngine) {
		// Set display size
		if(engine.playerID==0&&!netVSIsWatch()||
			netCurrentRoomInfo!=null&&netCurrentRoomInfo!!.maxPlayers==2&&engine.playerID<=1) {
			engine.displaySize = 0
			engine.enableSE = true
		} else {
			engine.displaySize = -1
			engine.enableSE = false
		}

		// Set visible flag
		if(netCurrentRoomInfo!=null&&engine.playerID>=netCurrentRoomInfo!!.maxPlayers) engine.isVisible = false

		// Set frame cint
		val seatID = netVSPlayerSeatID[engine.playerID]
		engine.frameColor = if(seatID>=0&&seatID<NET_PLAYER_COLOR_FRAME.size)
			NET_PLAYER_COLOR_FRAME[seatID] else GameEngine.FRAME_COLOR_GRAY
	}

	/** NET-VS: Apply room's settings (such as gravity) to all GameEngine */
	protected fun netVSApplyRoomSettings() {
		for(i in 0 until players)
			netVSApplyRoomSettings(owner.engine[i])
	}

	/** NET-VS: Apply room's settings (such as gravity) to the specific
	 * GameEngine
	 * @param engine GameEngine to apply settings
	 */
	internal open fun netVSApplyRoomSettings(engine:GameEngine) {
		if(netCurrentRoomInfo!=null) {
			engine.speed.gravity = netCurrentRoomInfo!!.gravity
			engine.speed.denominator = netCurrentRoomInfo!!.denominator
			engine.speed.are = netCurrentRoomInfo!!.are
			engine.speed.areLine = netCurrentRoomInfo!!.areLine
			engine.speed.lineDelay = netCurrentRoomInfo!!.lineDelay
			engine.speed.lockDelay = netCurrentRoomInfo!!.lockDelay
			engine.speed.das = netCurrentRoomInfo!!.das

			engine.b2bEnable = netCurrentRoomInfo!!.b2b
			engine.splitB2B = netCurrentRoomInfo!!.splitb2b

			engine.comboType = if(netCurrentRoomInfo!!.combo) GameEngine.COMBO_TYPE_NORMAL else GameEngine.COMBO_TYPE_DISABLE

			when(netCurrentRoomInfo!!.twistEnableType) {
				0 -> {
					engine.twistEnable = false
					engine.useAllSpinBonus = false
				}
				1 -> {
					engine.twistEnable = true
					engine.useAllSpinBonus = false
				}
				2 -> {
					engine.twistEnable = true
					engine.useAllSpinBonus = true
				}
			}
		}
	}

	/** NET-VS: Get player field number by seat ID
	 * @param seat The seat ID want to know
	 * @return Player number
	 */
	internal fun netVSGetPlayerIDbySeatID(seat:Int):Int = netVSGetPlayerIDbySeatID(seat, netVSMySeatID)

	/** NET-VS: Get player field number by seat ID
	 * @param seat The seat ID want to know
	 * @param myseat Your seat number (-1 if spectator)
	 * @return Player number
	 */
	private fun netVSGetPlayerIDbySeatID(seat:Int, myseat:Int):Int {
		var myseat2 = myseat
		if(myseat2<0) myseat2 = 0
		return NET_GAME_SEAT_NUMBERS[myseat2][seat]
	}

	/** NET-VS: Start a practice game
	 * @param engine GameEngine
	 */
	private fun netVSStartPractice(engine:GameEngine) {
		netVSIsPractice = true
		netVSIsPracticeExitAllowed = false

		engine.init()
		engine.stat = GameEngine.Status.READY
		engine.resetStatc()
		netUpdatePlayerExist()
		netVSSetGameScreenLayout()

		// Map
		if(netCurrentRoomInfo!!.useMap&&netLobby!!.mapList.size>0) {
			if(netVSRandMap==null) netVSRandMap = Random.Default

			var map:Int
			val maxMap = netLobby!!.mapList.size
			do
				map = netVSRandMap!!.nextInt(maxMap)
			while(map==netVSMapPreviousPracticeMap&&maxMap>=2)
			netVSMapPreviousPracticeMap = map

			engine.createFieldIfNeeded()
			engine.field.stringToField(netLobby!!.mapList[map])
			engine.field.setAllSkin(engine.skin)
			engine.field.setAllAttribute(true, Block.ATTRIBUTE.VISIBLE, Block.ATTRIBUTE.OUTLINE)
			engine.field.setAllAttribute(false, Block.ATTRIBUTE.SELF_PLACED)
		}
	}

	/** NET-VS: Receive end-of-game stats.<br></br>
	 * Game modes should implement this. However, there are some sample codes in
	 * NetDummyVSMode.
	 * @param message Message
	 */
	internal open fun netVSRecvEndGameStats(message:List<String>) {
		val seatID = message[2].toInt()
		val playerID = netVSGetPlayerIDbySeatID(seatID)

		if(playerID!=0||netVSIsWatch()) netVSPlayerResultReceived[playerID] = true
	}

	/** Get number of teams alive (Each independence player will also count as a
	 * team)
	 * @return Number of teams alive
	 */
	internal fun netVSGetNumberOfTeamsAlive():Int {
		val listTeamName = mutableListOf<String>()
		var noTeamCount = 0

		for(i in 0 until players)
			if(netVSPlayerExist[i]&&!netVSPlayerDead[i]&&owner.engine[i].gameActive)
				if(netVSPlayerTeam[i].isNotEmpty()) {
					if(!listTeamName.contains(netVSPlayerTeam[i])) listTeamName.add(netVSPlayerTeam[i])
				} else
					noTeamCount++

		return noTeamCount+listTeamName.size
	}

	/** Check if the given playerID can be attacked
	 * @param playerID Player ID (to attack)
	 * @return true if playerID can be attacked
	 */
	internal fun netVSIsAttackable(playerID:Int):Boolean {
		// Can't attack self
		if(playerID<=0) return false

		// Doesn't exist?
		if(!netVSPlayerExist[playerID]||netVSPlayerDead[playerID]||!netVSPlayerActive[playerID]) return false

		// Is teammate?
		val myTeam = netVSPlayerTeam[0]
		val thisTeam = netVSPlayerTeam[playerID]
		return myTeam.isEmpty()||thisTeam.isEmpty()||myTeam!=thisTeam
	}

	/** Draw room info box (number of players, number of spectators, etc.) to
	 * somewhere on the screen
	 * @param x X position
	 * @param y Y position
	 */
	private fun netVSDrawRoomInfoBox(x:Int, y:Int) {
		if(netCurrentRoomInfo!=null) {
			receiver.drawDirectFont(x, y, "PLAYERS", COLOR.CYAN, .5f)
			receiver.drawDirectFont(x, y+8, "$netVSNumPlayers", COLOR.WHITE, .5f)
			receiver.drawDirectFont(x, y+16, "SPECTATORS", COLOR.CYAN, .5f)
			receiver.drawDirectFont(x, y+24, "$netNumSpectators", COLOR.WHITE, .5f)

			if(!netVSIsWatch()) {
				receiver.drawDirectFont(x, y+32, "MATCHES", COLOR.CYAN, .5f)
				receiver.drawDirectFont(x, y+40, "${netVSPlayerPlayCount[0]}", COLOR.WHITE, .5f)
				receiver.drawDirectFont(x, y+48, "WINS", COLOR.CYAN, .5f)
				receiver.drawDirectFont(x, y+56, "${netVSPlayerWinCount[0]}", COLOR.WHITE, .5f)
			}
		}
		receiver.drawDirectFont(x, y+72, "ALL ROOMS", COLOR.GREEN, .5f)
		receiver.drawDirectFont(x, y+80, "${netLobby!!.netPlayerClient!!.roomInfoList.size}", COLOR.WHITE, .5f)
	}

	/** NET-VS: Settings screen */
	override fun onSetting(engine:GameEngine):Boolean {
		if(netCurrentRoomInfo!=null&&engine.playerID==0&&!netVSIsWatch()) {
			netVSPlayerExist[0] = true

			engine.displaySize = 0
			engine.enableSE = true
			engine.isVisible = true

			if(menuTime>=5) if(!netVSIsReadyChangePending&&netVSNumPlayers>=2&&!netVSIsNewcomer) {
				// Ready ON
				if(engine.ctrl.isPush(Controller.BUTTON_A)&&!netVSPlayerReady[0]) {
					engine.playSE("decide")
					netVSIsReadyChangePending = true
					netLobby!!.netPlayerClient!!.send("ready\ttrue\n")
				}
				// Ready OFF
				if(engine.ctrl.isPush(Controller.BUTTON_B)&&netVSPlayerReady[0]) {
					engine.playSE("decide")
					netVSIsReadyChangePending = true
					netLobby!!.netPlayerClient!!.send("ready\tfalse\n")
				}
			} else if(engine.ctrl.isPush(Controller.BUTTON_F)) {
				// Practice Mode
				engine.playSE("decide")
				netVSStartPractice(engine)
				return true
			}
		}

		// Random Map Preview
		if(netCurrentRoomInfo!=null&&netCurrentRoomInfo!!.useMap&&!netLobby!!.mapList.isEmpty())
			if(netVSPlayerExist[engine.playerID]) {
				if(menuTime>=35) {
					engine.statc[5]++
					if(engine.statc[5]>=netLobby!!.mapList.size) engine.statc[5] = 0
					engine.createFieldIfNeeded()
					engine.field.stringToField(netLobby!!.mapList[engine.statc[5]])
					engine.field.setAllSkin(engine.skin)
					engine.field.setAllAttribute(true, Block.ATTRIBUTE.VISIBLE, Block.ATTRIBUTE.OUTLINE)
					engine.field.setAllAttribute(false, Block.ATTRIBUTE.SELF_PLACED)
					menuTime -= 30
				}
			} else if(!engine.field.isEmpty) engine.field.reset()

		if(menuTime<35) menuTime++

		return true
	}

	/** NET-VS: Render the settings screen */
	override fun renderSetting(engine:GameEngine) {
		if(!engine.isVisible) return

		val x = receiver.fieldX(engine)
		val y = receiver.fieldY(engine)

		val pid = engine.playerID
		if(netCurrentRoomInfo!=null) {
			if(netVSPlayerReady[pid]&&netVSPlayerExist[pid])
				if(engine.displaySize!=-1)
					receiver.drawDirectFont(x+68, y+204, "OK", COLOR.YELLOW)
				else
					receiver.drawDirectFont(x+36, y+80, "OK", COLOR.YELLOW, .5f)

			if(pid==0&&!netVSIsWatch()&&!netVSIsReadyChangePending&&netVSNumPlayers>=2
				&&!netVSIsNewcomer)
				if(!netVSPlayerReady[pid]) {
					var strTemp = "A(${receiver.getKeyNameByButtonID(engine, Controller.BUTTON_A)} KEY):"
					if(strTemp.length>10) strTemp = strTemp.take(10)
					receiver.drawMenuFont(engine, 0, 16, strTemp, COLOR.CYAN)
					receiver.drawMenuFont(engine, 1, 17, "READY", COLOR.CYAN)
				} else {
					var strTemp = "B(${receiver.getKeyNameByButtonID(engine, Controller.BUTTON_B)} KEY):"
					if(strTemp.length>10) strTemp = strTemp.take(10)
					receiver.drawMenuFont(engine, 0, 16, strTemp, COLOR.BLUE)
					receiver.drawMenuFont(engine, 1, 17, "CANCEL", COLOR.BLUE)
				}
		}

		if(pid==0&&!netVSIsWatch()&&menuTime>=5) {
			var strTemp = "F(${receiver.getKeyNameByButtonID(engine, Controller.BUTTON_F)} KEY):"
			if(strTemp.length>10) strTemp = strTemp.take(10)
			strTemp = strTemp.uppercase()
			receiver.drawMenuFont(engine, 0, 18, strTemp, COLOR.PURPLE)
			receiver.drawMenuFont(engine, 1, 19, "PRACTICE", COLOR.PURPLE)
		}
	}

	/** NET-VS: Ready */
	override fun onReady(engine:GameEngine):Boolean {
		if(engine.statc[0]==0)
		// Map
			if(netCurrentRoomInfo!!.useMap&&netVSMapNo<netLobby!!.mapList.size&&!netVSIsPractice) {
				engine.createFieldIfNeeded()
				engine.field.stringToField(netLobby!!.mapList[netVSMapNo])
				if(engine.playerID==0&&!netVSIsWatch())
					engine.field.setAllSkin(engine.skin)
				else if(netCurrentRoomInfo!!.ruleLock&&netLobby!!.ruleOptLock!=null)
					engine.field.setAllSkin(netLobby!!.ruleOptLock!!.skin)
				else if(netVSPlayerSkin[engine.playerID]>=0) engine.field.setAllSkin(netVSPlayerSkin[engine.playerID])
				engine.field.setAllAttribute(true, Block.ATTRIBUTE.VISIBLE, Block.ATTRIBUTE.OUTLINE)
				engine.field.setAllAttribute(false, Block.ATTRIBUTE.SELF_PLACED)
			}

		if(netVSIsPractice&&engine.statc[0]>=10) netVSIsPracticeExitAllowed = true

		return false
	}

	/** NET-VS: Executed after Ready->Go, before the first piece appears. */
	override fun startGame(engine:GameEngine) {
		netVSApplyRoomSettings(engine)

		if(engine.playerID==0) {
			// Set BGM
			if(netVSIsPractice)
				owner.musMan.bgm = BGM.Silent
			else {
				owner.musMan.bgm = BGM.Extra(4)
				owner.musMan.fadeSW = false
			}

			// Init Variables
			netVSPieceMoveTimer = 0
		}
	}

	/** NET-VS: When the pieces can move */
	override fun onMove(engine:GameEngine):Boolean {
		// Stop game for remote players
		if(engine.playerID!=0||netVSIsWatch()) return true

		// Timer start
		if(engine.ending==0&&engine.statc[0]==0&&!engine.holdDisable&&!netVSIsPractice) netVSPlayTimerActive = true

		// Send movements
		super.onMove(engine)

		// Auto lock
		if(engine.ending==0&&engine.nowPieceObject!=null&&netVSPieceMoveTimerMax>0) {
			netVSPieceMoveTimer++
			if(netVSPieceMoveTimer>=netVSPieceMoveTimerMax) {
				engine.nowPieceY = engine.nowPieceBottomY
				engine.lockDelayNow = engine.lockDelay
				netVSPieceMoveTimer = 0
			}
		}

		return false
	}

	/** NET-VS: When the piece locked */
	override fun pieceLocked(engine:GameEngine, lines:Int) {
		netVSPieceMoveTimer = 0
	}

	/** NET-VS: Executed at the end of each frame */
	override fun onLast(engine:GameEngine) {
		val pid = engine.playerID
		super.onLast(engine)

		// Play Timer
		if(pid==0&&netVSPlayTimerActive) netVSPlayTimer++

		// Automatic start timer
		if(pid==0&&netCurrentRoomInfo!=null&&netVSAutoStartTimerActive&&!netVSIsGameActive)
			when {
				netVSNumPlayers<=1 -> netVSAutoStartTimerActive = false
				netVSAutoStartTimer>0 -> netVSAutoStartTimer--
				else -> {
					if(!netVSIsWatch()) netLobby!!.netPlayerClient!!.send("autostart\n")
					netVSAutoStartTimer = 0
					netVSAutoStartTimerActive = false
				}
			}

		// End practice mode
		if(pid==0&&netVSIsPractice&&netVSIsPracticeExitAllowed&&engine.ctrl.isPush(Controller.BUTTON_F)) {
			netVSIsPractice = false
			netVSIsPracticeExitAllowed = false
			owner.musMan.bgm = BGM.Silent
			engine.field.reset()
			engine.gameEnded()
			engine.stat = GameEngine.Status.SETTING
			engine.resetStatc()
		}
	}

	/** NET-VS: Render something such as HUD */
	override fun renderLast(engine:GameEngine) {
		// Player count
		val pid = engine.playerID
		if(pid==players-1) super.renderLast(engine)

		// Room info box
		if(pid==players-1) {
			var x2 = if(receiver.nextDisplayType==2) 544 else 503
			if(receiver.nextDisplayType==2&&netCurrentRoomInfo!!.maxPlayers==2) x2 = 321
			if(receiver.nextDisplayType!=2&&netCurrentRoomInfo!!.maxPlayers==2) x2 = 351

			netVSDrawRoomInfoBox(x2, 286)
		}

		// Elapsed time
		if(pid==0) {
			receiver.drawDirectFont(256, 16, netVSPlayTimer.toTimeStr)

			if(netVSIsPractice)
				receiver.drawDirectFont(256, 32, engine.statistics.time.toTimeStr, COLOR.PURPLE)
		}

		// Automatic start timer
		if(pid==0&&netCurrentRoomInfo!=null&&netVSAutoStartTimerActive&&!netVSIsGameActive)
			receiver.drawDirectFont(
				496, 16, netVSAutoStartTimer.toTimeStr,
				if(netCurrentRoomInfo!!.autoStartTNET2) COLOR.RED else COLOR.YELLOW
			)
	}

	/** NET-VS: Game Over */
	override fun onGameOver(engine:GameEngine):Boolean {
		if(engine.statc[0]==0) engine.gameEnded()
		engine.allowTextRenderByReceiver = false
		owner.musMan.bgm = BGM.Silent
		engine.resetFieldVisible()

		val pid = engine.playerID
		// Practice
		if(pid==0&&netVSIsPractice)
			return if(engine.statc[0]<engine.field.height+1)
				false
			else {
				engine.field.reset()
				engine.stat = GameEngine.Status.RESULT
				engine.resetStatc()
				true
			}

		// 1P died
		if(pid==0&&!netVSPlayerDead[pid]&&!netVSIsDeadPending&&!netVSIsWatch()) {
			netSendField(engine)
			netSendNextAndHold(engine)
			netSendStats(engine)

			netLobby!!.netPlayerClient!!.send("dead\t$netVSLastAttackerUID\n")

			netVSPlayerResultReceived[pid] = true
			netVSIsDeadPending = true
			return true
		}

		// Player/Opponent died
		if(netVSPlayerDead[pid]) {
			if(engine.field.isEmpty) {
				engine.stat = GameEngine.Status.SETTING
				engine.resetStatc()
				return true
			}
			return engine.statc[0]>=engine.field.height+1&&!netVSPlayerResultReceived[pid]
		}

		return true
	}

	/** NET-VS: Draw Game Over screen */
	override fun renderGameOver(engine:GameEngine) {
		val pid = engine.playerID
		if(pid==0&&netVSIsPractice) return
		if(!engine.isVisible) return

		val x = receiver.fieldX(engine)
		val y = receiver.fieldY(engine)
		val place = netVSPlayerPlace[pid]

		if(engine.displaySize!=-1) {
			if(netVSPlayerReady[pid]&&!netVSIsGameActive)
				owner.receiver.drawDirectFont(x+68, y+204, "OK", COLOR.YELLOW)
			else if(netVSNumNowPlayers==2||netCurrentRoomInfo!!.maxPlayers==2)
				owner.receiver.drawDirectFont(x+20, y+204, "YOU LOSE", COLOR.WHITE)
			else if(place==1) receiver.drawDirectFont(x+28, y+80, "YOU WIN", COLOR.WHITE, .5f)
			else if(place==2) receiver.drawDirectFont(x+12, y+204, "2ND PLACE", COLOR.WHITE)
			else if(place==3) receiver.drawDirectFont(x+12, y+204, "3RD PLACE", COLOR.RED)
			else if(place==4) receiver.drawDirectFont(x+12, y+204, "4TH PLACE", COLOR.GREEN)
			else if(place==5) receiver.drawDirectFont(x+12, y+204, "5TH PLACE", COLOR.BLUE)
			else if(place==6) receiver.drawDirectFont(x+12, y+204, "6TH PLACE", COLOR.PURPLE)
		} else if(netVSPlayerReady[pid]&&!netVSIsGameActive)
			owner.receiver.drawDirectFont(x+36, y+80, "OK", COLOR.YELLOW, .5f)
		else if(netVSNumNowPlayers==2||netCurrentRoomInfo!!.maxPlayers==2)
			owner.receiver.drawDirectFont(x+20, y+80, "YOU LOSE", COLOR.WHITE, .5f)
		else if(place==1) receiver.drawDirectFont(x+28, y+80, "YOU WIN", COLOR.WHITE, .5f)
		else if(place==2) receiver.drawDirectFont(x+8, y+80, "2ND PLACE", COLOR.WHITE, .5f)
		else if(place==3) receiver.drawDirectFont(x+8, y+80, "3RD PLACE", COLOR.RED, .5f)
		else if(place==4) receiver.drawDirectFont(x+8, y+80, "4TH PLACE", COLOR.GREEN, .5f)
		else if(place==5) receiver.drawDirectFont(x+8, y+80, "5TH PLACE", COLOR.BLUE, .5f)
		else if(place==6) receiver.drawDirectFont(x+8, y+80, "6TH PLACE", COLOR.PURPLE, .5f)
	}

	/** NET-VS: Excellent screen */
	override fun onExcellent(engine:GameEngine):Boolean {
		engine.allowTextRenderByReceiver = false
		val pid = engine.playerID
		if(pid==0) netVSPlayerResultReceived[pid] = true

		if(engine.statc[0]==0) {
			engine.gameEnded()
			owner.musMan.bgm = BGM.Silent
			engine.resetFieldVisible()
			engine.playSE("excellent")
		}

		if(engine.statc[0]>=120&&engine.ctrl.isPush(Controller.BUTTON_A)) engine.statc[0] = engine.field.height+1+180

		if(engine.statc[0]>=engine.field.height+1+180) {
			if(!netVSIsGameActive&&netVSPlayerResultReceived[pid]) {
				engine.field.reset()
				engine.resetStatc()
				engine.stat = GameEngine.Status.RESULT
			}
		} else
			engine.statc[0]++

		return true
	}

	/** NET-VS: Draw Excellent screen */
	override fun renderExcellent(engine:GameEngine) {
		if(!engine.isVisible) return

		val pid = engine.playerID
		val cY = (engine.fieldHeight-1)/2f

		if(pid==0&&netVSIsPractice&&!netVSIsWatch())
			owner.receiver.drawMenuFont(engine, 0f, cY/2, "EXCELLENT!", COLOR.RAINBOW)
		else if(netVSPlayerReady[pid]&&!netVSIsGameActive)
			owner.receiver.drawMenuFont(engine, 0f, cY/2, "OK", COLOR.WHITE)
		else if(netVSNumNowPlayers==2||netCurrentRoomInfo!!.maxPlayers==2)
			owner.receiver.drawMenuFont(engine, 0f, cY/2, "WIN!", COLOR.YELLOW)
		else
			owner.receiver.drawMenuFont(engine, 0f, cY/2, "1ST PLACE!", COLOR.YELLOW)
	}

	/** NET-VS: Results screen */
	override fun onResult(engine:GameEngine):Boolean {
		engine.allowTextRenderByReceiver = false

		if(engine.playerID==0&&!netVSIsWatch()) {
			// To the settings screen
			if(engine.ctrl.isPush(Controller.BUTTON_A)) {
				engine.playSE("decide")
				netVSIsPractice = false
				engine.stat = GameEngine.Status.SETTING
				engine.resetStatc()
				return true
			}
			// Start Practice
			if(engine.ctrl.isPush(Controller.BUTTON_F)) {
				engine.playSE("decide")
				netVSStartPractice(engine)
				return true
			}
		}

		return true
	}

	/** NET-VS: Draw results screen */
	override fun renderResult(engine:GameEngine) {
		var scale = 1f
		if(engine.displaySize==-1) scale = .5f

		// Place
		val pid = engine.playerID
		if(!netVSIsPractice||pid!=0) {
			owner.receiver.drawMenuFont(engine, 0, 0, "RESULT", COLOR.ORANGE, scale)
			if(netVSPlayerPlace[pid]==1) {
				if(netVSNumNowPlayers==2)
					owner.receiver.drawMenuFont(engine, 6, 1, "WIN!", COLOR.YELLOW, scale)
				else
					owner.receiver.drawMenuFont(engine, 6, 1, "1ST!", COLOR.YELLOW, scale)
			} else if(netVSPlayerPlace[pid]==2) {
				if(netVSNumNowPlayers==2)
					owner.receiver.drawMenuFont(engine, 6, 1, "LOSE", COLOR.WHITE, scale)
				else
					owner.receiver.drawMenuFont(engine, 7, 1, "2ND", COLOR.WHITE, scale)
			} else if(netVSPlayerPlace[pid]==3)
				owner.receiver.drawMenuFont(engine, 7, 1, "3RD", COLOR.RED, scale)
			else if(netVSPlayerPlace[pid]==4)
				owner.receiver.drawMenuFont(engine, 7, 1, "4TH", COLOR.GREEN, scale)
			else if(netVSPlayerPlace[pid]==5)
				owner.receiver.drawMenuFont(engine, 7, 1, "5TH", COLOR.BLUE, scale)
			else if(netVSPlayerPlace[pid]==6)
				owner.receiver.drawMenuFont(engine, 7, 1, "6TH", COLOR.COBALT, scale)
		} else
			owner.receiver.drawMenuFont(engine, 0, 0, "PRACTICE", COLOR.PINK, scale)

		if(pid==0&&!netVSIsWatch()) {
			// Restart/Practice
			var strTemp = "A(${owner.receiver.getKeyNameByButtonID(engine, Controller.BUTTON_A)} KEY):"
			if(strTemp.length>10) strTemp = strTemp.take(10)
			owner.receiver.drawMenuFont(engine, 0, 18, strTemp, COLOR.RED)
			owner.receiver.drawMenuFont(engine, 1, 19, "RESTART", COLOR.RED)

			var strTempF = "F(${owner.receiver.getKeyNameByButtonID(engine, Controller.BUTTON_F)} KEY):"
			if(strTempF.length>10) strTempF = strTempF.take(10)
			owner.receiver.drawMenuFont(engine, 0, 20, strTempF, COLOR.PURPLE)
			if(!netVSIsPractice)
				owner.receiver.drawMenuFont(engine, 1, 21, "PRACTICE", COLOR.PURPLE)
			else
				owner.receiver.drawMenuFont(engine, 1, 21, "RETRY", COLOR.PURPLE)
		} else if(netVSPlayerReady[pid]&&netVSPlayerExist[pid]) {
			// Player Ready
			val x = receiver.fieldX(engine)
			val y = receiver.fieldY(engine)

			if(engine.displaySize!=-1)
				owner.receiver.drawDirectFont(x+68, y+356, "OK", COLOR.YELLOW)
			else
				owner.receiver.drawDirectFont(x+36, y+156, "OK", COLOR.YELLOW, .5f)
		}
	}

	/** NET-VS: No retry key. */
	override fun netplayOnRetryKey(engine:GameEngine) {}

	/** NET-VS: Disconnected */
	override fun netlobbyOnDisconnect(lobby:NetLobbyFrame, client:NetPlayerClient, ex:Throwable?) {
		for(i in 0 until players)
			owner.engine[i].stat = GameEngine.Status.NOTHING
	}

	/** NET-VS: Message received */
	@Throws(IOException::class)
	override fun netlobbyOnMessage(lobby:NetLobbyFrame, client:NetPlayerClient, message:List<String>) {
		// Player status update
		if(message[0]=="playerupdate") {
			val pInfo = NetPlayerInfo(message[1])

			// Ready status change
			if(pInfo.roomID==netCurrentRoomInfo!!.roomID&&pInfo.seatID!=-1) {
				val playerID = netVSGetPlayerIDbySeatID(pInfo.seatID)

				if(netVSPlayerReady[playerID]!=pInfo.ready) {
					netVSPlayerReady[playerID] = pInfo.ready

					if(playerID==0&&!netVSIsWatch())
						netVSIsReadyChangePending = false
					else if(pInfo.ready) receiver.playSE("decide")
					else if(!pInfo.playing) receiver.playSE("change")
				}
			}

			netUpdatePlayerExist()
		}
		// When someone log out
		if(message[0]=="playerlogout") {
			val pInfo = NetPlayerInfo(message[1])

			if(pInfo.roomID==netCurrentRoomInfo!!.roomID&&pInfo.seatID!=-1) netUpdatePlayerExist()
		}
		// Player status change (Join/Watch)
		if(message[0]=="changestatus") {
			val uid = message[2].toInt()

			netUpdatePlayerExist()
			netVSSetGameScreenLayout()

			if(uid==netLobby!!.netPlayerClient!!.playerUID) {
				netVSIsPractice = false
				if(netVSIsGameActive&&!netVSIsWatch()) netVSIsNewcomer = true

				owner.engine[0].stat = GameEngine.Status.SETTING

				for(i in 0 until players) {
					owner.engine[i].field.reset()
					owner.engine[i].nowPieceObject = null

					if(owner.engine[i].stat==GameEngine.Status.NOTHING||!netVSIsGameActive)
						owner.engine[i].stat = GameEngine.Status.SETTING
					owner.engine[i].resetStatc()
				}
			}
		}
		// Someone entered here
		if(message[0]=="playerenter") {
			val seatID = message[3].toInt()
			if(seatID!=-1&&netVSNumPlayers<2) receiver.playSE("levelstop")
		}
		// Someone leave here
		if(message[0]=="playerleave") {
			netUpdatePlayerExist()

			if(netVSNumPlayers<2) netVSAutoStartTimerActive = false
		}
		// Automatic timer start
		if(message[0]=="autostartbegin")
			if(netVSNumPlayers>=2) {
				val seconds = message[1].toInt()
				netVSAutoStartTimer = seconds*60
				netVSAutoStartTimerActive = true
			}
		// Automatic timer stop
		if(message[0]=="autostartstop") netVSAutoStartTimerActive = false
		// Game Started
		if(message[0]=="start") {
			val randseed = message[1].toLong(16)
			netVSNumNowPlayers = message[2].toInt()
			netVSNumAlivePlayers = netVSNumNowPlayers
			netVSMapNo = message[3].toInt()

			netVSResetFlags()
			netUpdatePlayerExist()

			owner.menuOnly = false
			owner.musMan.reset()
			owner.bgMan.reset()
			owner.replayProp.clear()
			for(i in 0 until players)
				if(netVSPlayerExist[i]) {
					owner.engine[i].init()
					netVSSetGameScreenLayout(owner.engine[i])
				}

			netVSAutoStartTimerActive = false
			netVSIsGameActive = true
			netVSIsGameFinished = false
			netVSPlayTimer = 0

			netVSSetLockedRule() // Set locked rule/Restore rule

			for(i in 0 until players) {
				val engine = owner.engine[i]
				engine.resetStatc()

				if(netVSPlayerExist[i]) {
					netVSPlayerActive[i] = true
					engine.stat = GameEngine.Status.READY
					engine.randSeed = randseed
					engine.random = Random(randseed)

					if(netCurrentRoomInfo!!.maxPlayers==2&&netVSNumPlayers==2) {
						engine.isVisible = true
						engine.displaySize = 0

						if(netCurrentRoomInfo!!.ruleLock||i==0&&!netVSIsWatch()) {
							engine.isNextVisible = true
							engine.isHoldVisible = true

							if(i!=0) engine.randomizer = owner.engine[0].randomizer
						} else {
							engine.isNextVisible = false
							engine.isHoldVisible = false
						}
					}
				} else if(i<netCurrentRoomInfo!!.maxPlayers) {
					engine.stat = GameEngine.Status.SETTING
					engine.isVisible = true
					engine.isNextVisible = false
					engine.isHoldVisible = false

					if(netCurrentRoomInfo!!.maxPlayers==2&&netVSNumPlayers==2) engine.isVisible = false
				} else {
					engine.stat = GameEngine.Status.SETTING
					engine.isVisible = false
				}

				netVSPlayerResultReceived[i] = false
				netVSPlayerDead[i] = false
				netVSPlayerReady[i] = false
			}
		}
		// Dead
		if(message[0]=="dead") {
			val seatID = message[3].toInt()
			val playerID = netVSGetPlayerIDbySeatID(seatID)

			if(!netVSPlayerDead[playerID]) {
				netVSPlayerDead[playerID] = true
				netVSPlayerPlace[playerID] = message[4].toInt()
				owner.engine[playerID].stat = GameEngine.Status.GAMEOVER
				owner.engine[playerID].resetStatc()
				netVSNumAlivePlayers--

				if(seatID==netLobby!!.netPlayerClient!!.yourPlayerInfo!!.seatID) {
					if(!netVSIsDeadPending) {
						// Forced death
						netSendField(owner.engine[0])
						netSendNextAndHold(owner.engine[0])
						netSendStats(owner.engine[0])
						netVSPlayerResultReceived[0] = true
					}

					// Send end game stats
					netSendEndGameStats(owner.engine[0])
				}
			}
		}
		// End-of-game Stats
		if(message[0]=="gstat") netVSRecvEndGameStats(message)
		// Game Finished
		if(message[0]=="finish") {
			netVSIsGameActive = false
			netVSIsGameFinished = true
			netVSPlayTimerActive = false
			netVSIsNewcomer = false

			// Stop practice game
			if(netVSIsPractice) {
				netVSIsPractice = false
				netVSIsPracticeExitAllowed = false
				owner.musMan.bgm = BGM.Silent
				owner.engine[0].gameEnded()
				owner.engine[0].stat = GameEngine.Status.SETTING
				owner.engine[0].resetStatc()
			}

			val flagTeamWin = message[4].toBoolean()

			if(flagTeamWin) {
				// Team won
				for(i in 0 until players)
					if(netVSPlayerExist[i]&&!netVSPlayerDead[i]) {
						netVSPlayerPlace[i] = 1
						owner.engine[i].gameEnded()
						owner.engine[i].stat = GameEngine.Status.EXCELLENT
						owner.engine[i].resetStatc()
						owner.engine[i].statistics.time = netVSPlayTimer
						netVSNumAlivePlayers--

						if(i==0&&!netVSIsWatch()) netSendEndGameStats(owner.engine[0])
					}
			} else {
				// Normal player won
				val seatID = message[2].toInt()
				if(seatID!=-1) {
					val playerID = netVSGetPlayerIDbySeatID(seatID)
					if(netVSPlayerExist[playerID]) {
						netVSPlayerPlace[playerID] = 1
						owner.engine[playerID].gameEnded()
						owner.engine[playerID].stat = GameEngine.Status.EXCELLENT
						owner.engine[playerID].resetStatc()
						owner.engine[playerID].statistics.time = netVSPlayTimer
						netVSNumAlivePlayers--

						if(seatID==netLobby!!.netPlayerClient!!.yourPlayerInfo!!.seatID&&!netVSIsWatch())
							netSendEndGameStats(owner.engine[0])
					}
				}
			}

			if(netVSIsWatch()||netVSPlayerPlace[0]>=3) receiver.playSE("matchend")

			netUpdatePlayerExist()
		}
		// Game messages
		if(message[0]=="game") {
			//int uid = message[1].toInt();
			val seatID = message[2].toInt()
			val playerID = netVSGetPlayerIDbySeatID(seatID)
			val engine = owner.engine[playerID]

			engine.createFieldIfNeeded()

			// Field
			if(message[3]=="field"||message[3]=="fieldattr") netRecvField(engine, message)
			// Stats
			if(message[3]=="stats") netRecvStats(engine, message)
			// Current Piece
			if(message[3]=="piece") {
				netRecvPieceMovement(engine, message)

				// Play timer start
				if(netVSIsWatch()&&!netVSIsNewcomer&&!netVSPlayTimerActive&&!netVSIsGameFinished) {
					netVSPlayTimerActive = true
					netVSPlayTimer = 0
				}

				// Force start
				if(!netVSIsWatch()&&netVSPlayTimerActive&&!netVSIsPractice&&
					engine.stat==GameEngine.Status.READY&&engine.statc[0]<engine.goEnd)
					engine.statc[0] = engine.goEnd
			}
			// Next and Hold
			if(message[3]=="next") netRecvNextAndHold(engine, message)
		}
	}

	companion object {
		/* -------------------- Constants -------------------- */
		/** NET-VS: Max number of players */
		internal const val NET_MAX_PLAYERS = 6

		/** NET-VS: Numbers of seats numbers corresponding to frames on player's
		 * screen */
		private val NET_GAME_SEAT_NUMBERS =
			listOf(
				listOf(0, 1, 2, 3, 4, 5), listOf(1, 0, 2, 3, 4, 5), listOf(1, 2, 0, 3, 4, 5),
				listOf(1, 2, 3, 0, 4, 5), listOf(1, 2, 3, 4, 0, 5), listOf(1, 2, 3, 4, 5, 0)
			)

		/** NET-VS: Each player's garbage block cint */
		internal val NET_PLAYER_COLOR_BLOCK =
			listOf(
				Block.COLOR.RED, Block.COLOR.BLUE, Block.COLOR.GREEN, Block.COLOR.YELLOW,
				Block.COLOR.PURPLE, Block.COLOR.CYAN
			)

		/** NET-VS: Each player's frame cint */
		private val NET_PLAYER_COLOR_FRAME =
			listOf(
				GameEngine.FRAME_COLOR_RED, GameEngine.FRAME_COLOR_BLUE, GameEngine.FRAME_COLOR_GREEN,
				GameEngine.FRAME_COLOR_BRONZE, GameEngine.FRAME_COLOR_PURPLE, GameEngine.FRAME_COLOR_CYAN
			)

		/** NET-VS: Team font colors */
		private val NET_TEAM_FONT_COLORS =
			listOf(COLOR.WHITE, COLOR.RED, COLOR.GREEN, COLOR.BLUE, COLOR.YELLOW, COLOR.PURPLE, COLOR.CYAN)

		/** NET-VS: Default time before forced piece lock */
		private const val NET_PIECE_AUTO_LOCK_TIME = 30*60
	}
}
