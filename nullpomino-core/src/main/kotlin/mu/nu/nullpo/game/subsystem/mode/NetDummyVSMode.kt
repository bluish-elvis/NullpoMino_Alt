/*
 * Copyright (c) 2010-2021, NullNoname
 * Kotlin converted and modified by Venom=Nhelv
 * All rights reserved.
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
open class NetDummyVSMode:NetDummyMode() {

	/* -------------------- Variables -------------------- */
	/** NET-VS: Local player's seat ID (-1:Spectator) */
	private var netvsMySeatID = 0

	/** NET-VS: Number of players */
	private var netvsNumPlayers = 0

	/** NET-VS: Number of players in current game */
	private var netvsNumNowPlayers = 0

	/** NET-VS: Number of players still alive in current game */
	internal var netvsNumAlivePlayers = 0

	/** NET-VS: Player exist flag */
	internal var netvsPlayerExist = BooleanArray(0)

	/** NET-VS: Player ready flag */
	private var netvsPlayerReady = BooleanArray(0)

	/** NET-VS: Player dead flag */
	internal var netvsPlayerDead = BooleanArray(0)

	/** NET-VS: Player active flag (false if newcomer) */
	private var netvsPlayerActive = BooleanArray(0)

	/** NET-VS: Player's Seat ID array (-1:No Player) */
	internal var netvsPlayerSeatID = IntArray(0)

	/** NET-VS: Player's UID array (-1:No Player) */
	internal var netvsPlayerUID = IntArray(0)

	/** NET-VS: Player's place */
	internal var netvsPlayerPlace = IntArray(0)

	/** NET-VS: Player's win count */
	internal var netvsPlayerWinCount = IntArray(0)

	/** NET-VS: Player's game count */
	internal var netvsPlayerPlayCount = IntArray(0)

	/** NET-VS: Player's team colors */
	private var netvsPlayerTeamColor = IntArray(0)

	/** NET-VS: Player names */
	private var netvsPlayerName:Array<String> = emptyArray()

	/** NET-VS: Player team names */
	private var netvsPlayerTeam:Array<String> = emptyArray()

	/** NET-VS: Player's skins */
	internal var netvsPlayerSkin = IntArray(0)

	/** NET-VS: true if it's ready to show player's result */
	internal var netvsPlayerResultReceived = BooleanArray(0)

	/** NET-VS: true if automatic start timer is activated */
	private var netvsAutoStartTimerActive = false

	/** NET-VS: Time left until the game starts automatically */
	private var netvsAutoStartTimer = 0

	/** NET-VS: true if room game is in progress */
	internal var netvsIsGameActive = false

	/** NET-VS: true if room game is finished */
	private var netvsIsGameFinished = false

	/** NET-VS: true if waiting for ready status change */
	private var netvsIsReadyChangePending = false

	/** NET-VS: true if waiting for dead status change */
	private var netvsIsDeadPending = false

	/** NET-VS: true if local player joined game in progress */
	internal var netvsIsNewcomer = false

	/** NET-VS: Elapsed timer active flag */
	internal var netvsPlayTimerActive = false

	/** NET-VS: Elapsed time */
	internal var netvsPlayTimer = 0

	/** NET-VS: true if practice mode */
	internal var netvsIsPractice = false

	/** NET-VS: true if can exit from practice game */
	internal var netvsIsPracticeExitAllowed = false

	/** NET-VS: How long current piece is active */
	private var netvsPieceMoveTimer = 0

	/** NET-VS: Time before forced piece lock */
	private var netvsPieceMoveTimerMax = 0

	/** NET-VS: Map number to use */
	private var netvsMapNo = 0

	/** NET-VS: Random for selecting values in Practice mode */
	private var netvsRandMap:Random? = null

	/** NET-VS: Practice mode last used values number */
	private var netvsMapPreviousPracticeMap = 0

	/** NET-VS: UID of player who attacked local player last (-1: Suicide or
	 * Unknown) */
	internal var netvsLastAttackerUID = 0

	/* Mode Name */
	override val name = "NET-VS-DUMMY"

	override val isVSMode:Boolean
		get() = true

	/** NET-VS: Number of players */
	override val players:Int
		get() = NETVS_MAX_PLAYERS

	/** NET-VS: This is netplay-only mode */
	override val isNetplayMode:Boolean
		get() = true

	/** NET-VS: Mode Initialization */
	override fun modeInit(manager:GameManager) {
		super.modeInit(manager)
		log.debug("modeInit() on NetDummyVSMode")
		netForceSendMovements = true
		netvsMySeatID = -1
		netvsNumPlayers = 0
		netvsNumNowPlayers = 0
		netvsNumAlivePlayers = 0
		netvsPlayerExist = BooleanArray(NETVS_MAX_PLAYERS)
		netvsPlayerReady = BooleanArray(NETVS_MAX_PLAYERS)
		netvsPlayerActive = BooleanArray(NETVS_MAX_PLAYERS)
		netvsPlayerSeatID = IntArray(NETVS_MAX_PLAYERS)
		netvsPlayerUID = IntArray(NETVS_MAX_PLAYERS)
		netvsPlayerWinCount = IntArray(NETVS_MAX_PLAYERS)
		netvsPlayerPlayCount = IntArray(NETVS_MAX_PLAYERS)
		netvsPlayerTeamColor = IntArray(NETVS_MAX_PLAYERS)
		netvsPlayerName = Array(NETVS_MAX_PLAYERS) {""}
		netvsPlayerTeam = Array(NETVS_MAX_PLAYERS) {""}
		netvsPlayerSkin = IntArray(NETVS_MAX_PLAYERS) {-1}
		netvsAutoStartTimerActive = false
		netvsAutoStartTimer = 0
		netvsPieceMoveTimerMax = NETVS_PIECE_AUTO_LOCK_TIME
		netvsMapPreviousPracticeMap = -1
		netvsResetFlags()
	}

	/** NET-VS: Init some variables */
	private fun netvsResetFlags() {
		netvsPlayerResultReceived = BooleanArray(NETVS_MAX_PLAYERS)
		netvsPlayerDead = BooleanArray(NETVS_MAX_PLAYERS)
		netvsPlayerPlace = IntArray(NETVS_MAX_PLAYERS)
		netvsIsGameActive = false
		netvsIsGameFinished = false
		netvsIsReadyChangePending = false
		netvsIsDeadPending = false
		netvsIsNewcomer = false
		netvsPlayTimerActive = false
		netvsPlayTimer = 0
		netvsIsPractice = false
		netvsIsPracticeExitAllowed = false
		netvsPieceMoveTimer = 0
	}

	/** NET-VS: Initialization for each player */
	override fun playerInit(engine:GameEngine, playerID:Int) {
		netPlayerInit(engine, playerID)
	}

	/** @return true if watch mode
	 */
	internal fun netvsIsWatch():Boolean {
		try {
			return netLobby!!.netPlayerClient!!.yourPlayerInfo!!.seatID==-1
		} catch(e:Exception) {
		}

		return false
	}

	/** NET-VS: Update player variables */
	override fun netUpdatePlayerExist() {
		netvsMySeatID = netLobby!!.netPlayerClient!!.yourPlayerInfo!!.seatID
		netvsNumPlayers = 0
		netNumSpectators = 0
		netPlayerName = netLobby!!.netPlayerClient!!.playerName
		netIsWatch = netvsIsWatch()

		for(i in 0 until NETVS_MAX_PLAYERS) {
			netvsPlayerExist[i] = false
			netvsPlayerReady[i] = false
			netvsPlayerActive[i] = false
			netvsPlayerSeatID[i] = -1
			netvsPlayerUID[i] = -1
			netvsPlayerWinCount[i] = 0
			netvsPlayerPlayCount[i] = 0
			netvsPlayerName[i] = ""
			netvsPlayerTeam[i] = ""
			owner.engine[i].framecolor = GameEngine.FRAME_COLOR_GRAY
		}

		val pList = netLobby!!.updateSameRoomPlayerInfoList()
		val teamList = mutableListOf<String>()

		for(pInfo in pList)
			if(pInfo.roomID==netCurrentRoomInfo!!.roomID)
				if(pInfo.seatID==-1)
					netNumSpectators++
				else {
					netvsNumPlayers++

					val playerID = netvsGetPlayerIDbySeatID(pInfo.seatID)
					netvsPlayerExist[playerID] = true
					netvsPlayerReady[playerID] = pInfo.ready
					netvsPlayerActive[playerID] = pInfo.playing
					netvsPlayerSeatID[playerID] = pInfo.seatID
					netvsPlayerUID[playerID] = pInfo.uid
					netvsPlayerWinCount[playerID] = pInfo.winCountNow
					netvsPlayerPlayCount[playerID] = pInfo.playCountNow
					netvsPlayerName[playerID] = pInfo.strName
					netvsPlayerTeam[playerID] = pInfo.strTeam

					// Set frame cint
					if(pInfo.seatID<NETVS_PLAYER_COLOR_FRAME.size)
						owner.engine[playerID].framecolor = NETVS_PLAYER_COLOR_FRAME[pInfo.seatID]

					// Set team cint
					if(netvsPlayerTeam[playerID].isNotEmpty())
						if(!teamList.contains(netvsPlayerTeam[playerID])) {
							teamList.add(netvsPlayerTeam[playerID])
							netvsPlayerTeamColor[playerID] = teamList.size
						} else
							netvsPlayerTeamColor[playerID] = teamList.indexOf(netvsPlayerTeam[playerID])+1
				}
	}

	/** NET-VS: When you join the room */
	override fun netOnJoin(lobby:NetLobbyFrame, client:NetPlayerClient?, roomInfo:NetRoomInfo?) {
		log.debug("netOnJoin() on NetDummyVSMode")

		netCurrentRoomInfo = roomInfo
		netIsNetPlay = true
		netvsIsNewcomer = netCurrentRoomInfo!!.playing

		netUpdatePlayerExist()
		netvsSetLockedRule()
		netvsSetGameScreenLayout()

		if(netvsIsNewcomer) netvsNumNowPlayers = netvsNumPlayers
	}

	/** NET-VS: Initialize various NetPlay variables. Usually called from
	 * playerInit. */
	override fun netPlayerInit(engine:GameEngine, playerID:Int) {
		log.debug("netPlayerInit(engine, $playerID) on NetDummyVSMode")

		super.netPlayerInit(engine, playerID)

		// Misc. variables
		engine.fieldWidth = 10
		engine.fieldHeight = 20
		engine.gameoverAll = false
		engine.allowTextRenderByReceiver = true
	}

	/** NET-VS: Draw player's name */
	override fun netDrawPlayerName(engine:GameEngine) {
		val playerID = engine.playerID
		val x = owner.receiver.fieldX(engine, playerID)
		val y = owner.receiver.fieldY(engine, playerID)

		if(netvsPlayerName[playerID].isNotEmpty()) {
			var name = netvsPlayerName[playerID]
			var fontcolorNum = netvsPlayerTeamColor[playerID]
			if(fontcolorNum<0) fontcolorNum = 0
			if(fontcolorNum>NETVS_TEAM_FONT_COLORS.size-1) fontcolorNum = NETVS_TEAM_FONT_COLORS.size-1
			val fontcolor = NETVS_TEAM_FONT_COLORS[fontcolorNum]

			when {
				engine.displaysize==-1 -> {
					if(name.length>7) name = name.take(7)+".."
					owner.receiver.drawDirectTTF(x, y-16, name, fontcolor)
				}
				playerID==0 -> {
					if(name.length>14) name = name.take(14)+".."
					owner.receiver.drawDirectTTF(x, y-20, name, fontcolor)
				}
				else -> owner.receiver.drawDirectTTF(x, y-20, name, fontcolor)
			}
		}
	}

	/** NET-VS: Send field to everyone. It won't do anything in practice
	 * game. */
	override fun netSendField(engine:GameEngine) {
		if(!netvsIsPractice&&engine.playerID==0&&!netIsWatch) super.netSendField(engine)
	}

	/** NET-VS: Send next and hold piece informations to everyone. It won't do
	 * anything in practice game. */
	override fun netSendNextAndHold(engine:GameEngine) {
		if(!netvsIsPractice&&engine.playerID==0&&!netIsWatch) super.netSendNextAndHold(engine)
	}

	/** NET-VS: Send the current piece's movement to everyone. It won't do
	 * anything in practice game. */
	override fun netSendPieceMovement(engine:GameEngine, forceSend:Boolean):Boolean =
		if(!netvsIsPractice&&engine.playerID==0&&!netIsWatch) super.netSendPieceMovement(engine, forceSend) else false

	/** NET-VS: Set locked rule/Revert to user rule */
	private fun netvsSetLockedRule() {
		if(netCurrentRoomInfo!=null&&netCurrentRoomInfo!!.ruleLock) {
			// Set to locked rule
			if(netLobby!=null&&netLobby!!.ruleOptLock!=null) {
				val randomizer = GeneralUtil.loadRandomizer(netLobby!!.ruleOptLock!!.strRandomizer)
				val wallkick = GeneralUtil.loadWallkick(netLobby!!.ruleOptLock!!.strWallkick)
				for(i in 0 until players) {
					owner.engine[i].ruleOpt.copy(netLobby!!.ruleOptLock)
					owner.engine[i].randomizer = randomizer
					owner.engine[i].wallkick = wallkick
				}
			} else
				log.warn("Tried to set locked rule, but rule was not received yet!")
		} else if(!netvsIsWatch()) {
			// Revert rules
			owner.engine[0].ruleOpt.copy(netLobby!!.ruleOptPlayer)
			owner.engine[0].randomizer = GeneralUtil.loadRandomizer(owner.engine[0].ruleOpt.strRandomizer)
			owner.engine[0].wallkick = GeneralUtil.loadWallkick(owner.engine[0].ruleOpt.strWallkick)
		}
	}

	/** Set game screen layout */
	private fun netvsSetGameScreenLayout() {
		for(i in 0 until players)
			netvsSetGameScreenLayout(owner.engine[i])
	}

	/** Set game screen layout
	 * @param engine GameEngine
	 */
	private fun netvsSetGameScreenLayout(engine:GameEngine) {
		// Set display size
		if(engine.playerID==0&&!netvsIsWatch()||netCurrentRoomInfo!=null&&netCurrentRoomInfo!!.maxPlayers==2&&engine.playerID<=1) {
			engine.displaysize = 0
			engine.enableSE = true
		} else {
			engine.displaysize = -1
			engine.enableSE = false
		}

		// Set visible flag
		if(netCurrentRoomInfo!=null&&engine.playerID>=netCurrentRoomInfo!!.maxPlayers) engine.isVisible = false

		// Set frame cint
		val seatID = netvsPlayerSeatID[engine.playerID]
		if(seatID>=0&&seatID<NETVS_PLAYER_COLOR_FRAME.size)
			engine.framecolor = NETVS_PLAYER_COLOR_FRAME[seatID]
		else
			engine.framecolor = GameEngine.FRAME_COLOR_GRAY
	}

	/** NET-VS: Apply room's settings (such as gravity) to all GameEngine */
	protected fun netvsApplyRoomSettings() {
		for(i in 0 until players)
			netvsApplyRoomSettings(owner.engine[i])
	}

	/** NET-VS: Apply room's settings (such as gravity) to the specific
	 * GameEngine
	 * @param engine GameEngine to apply settings
	 */
	internal open fun netvsApplyRoomSettings(engine:GameEngine) {
		if(netCurrentRoomInfo!=null) {
			engine.speed.gravity = netCurrentRoomInfo!!.gravity
			engine.speed.denominator = netCurrentRoomInfo!!.denominator
			engine.speed.are = netCurrentRoomInfo!!.are
			engine.speed.areLine = netCurrentRoomInfo!!.areLine
			engine.speed.lineDelay = netCurrentRoomInfo!!.lineDelay
			engine.speed.lockDelay = netCurrentRoomInfo!!.lockDelay
			engine.speed.das = netCurrentRoomInfo!!.das

			engine.b2bEnable = netCurrentRoomInfo!!.b2b
			engine.splitb2b = netCurrentRoomInfo!!.splitb2b

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
	internal fun netvsGetPlayerIDbySeatID(seat:Int):Int = netvsGetPlayerIDbySeatID(seat, netvsMySeatID)

	/** NET-VS: Get player field number by seat ID
	 * @param seat The seat ID want to know
	 * @param myseat Your seat number (-1 if spectator)
	 * @return Player number
	 */
	private fun netvsGetPlayerIDbySeatID(seat:Int, myseat:Int):Int {
		var myseat2 = myseat
		if(myseat2<0) myseat2 = 0
		return NETVS_GAME_SEAT_NUMBERS[myseat2][seat]
	}

	/** NET-VS: Start a practice game
	 * @param engine GameEngine
	 */
	private fun netvsStartPractice(engine:GameEngine) {
		netvsIsPractice = true
		netvsIsPracticeExitAllowed = false

		engine.init()
		engine.stat = GameEngine.Status.READY
		engine.resetStatc()
		netUpdatePlayerExist()
		netvsSetGameScreenLayout()

		// Map
		if(netCurrentRoomInfo!!.useMap&&netLobby!!.mapList.size>0) {
			if(netvsRandMap==null) netvsRandMap = Random.Default

			var map:Int
			val maxMap = netLobby!!.mapList.size
			do
				map = netvsRandMap!!.nextInt(maxMap)
			while(map==netvsMapPreviousPracticeMap&&maxMap>=2)
			netvsMapPreviousPracticeMap = map

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
	internal open fun netvsRecvEndGameStats(message:Array<String>) {
		val seatID = message[2].toInt()
		val playerID = netvsGetPlayerIDbySeatID(seatID)

		if(playerID!=0||netvsIsWatch()) netvsPlayerResultReceived[playerID] = true
	}

	/** Get number of teams alive (Each independence player will also count as a
	 * team)
	 * @return Number of teams alive
	 */
	internal fun netvsGetNumberOfTeamsAlive():Int {
		val listTeamName = mutableListOf<String>()
		var noTeamCount = 0

		for(i in 0 until players)
			if(netvsPlayerExist[i]&&!netvsPlayerDead[i]&&owner.engine[i].gameActive)
				if(netvsPlayerTeam[i].isNotEmpty()) {
					if(!listTeamName.contains(netvsPlayerTeam[i])) listTeamName.add(netvsPlayerTeam[i])
				} else
					noTeamCount++

		return noTeamCount+listTeamName.size
	}

	/** Check if the given playerID can be attacked
	 * @param playerID Player ID (to attack)
	 * @return true if playerID can be attacked
	 */
	internal fun netvsIsAttackable(playerID:Int):Boolean {
		// Can't attack self
		if(playerID<=0) return false

		// Doesn't exist?
		if(!netvsPlayerExist[playerID]||netvsPlayerDead[playerID]||!netvsPlayerActive[playerID]) return false

		// Is teammate?
		val myTeam = netvsPlayerTeam[0]
		val thisTeam = netvsPlayerTeam[playerID]
		return myTeam.isEmpty()||thisTeam.isEmpty()||myTeam!=thisTeam
	}

	/** Draw room info box (number of players, number of spectators, etc) to
	 * somewhere on the screen
	 * @param x X position
	 * @param y Y position
	 */
	private fun netvsDrawRoomInfoBox(x:Int, y:Int) {
		if(netCurrentRoomInfo!=null) {
			owner.receiver.drawDirectFont(x, y, "PLAYERS", COLOR.CYAN, .5f)
			owner.receiver.drawDirectFont(x, y+8, "$netvsNumPlayers", COLOR.WHITE, .5f)
			owner.receiver.drawDirectFont(x, y+16, "SPECTATORS", COLOR.CYAN, .5f)
			owner.receiver.drawDirectFont(x, y+24, "$netNumSpectators", COLOR.WHITE, .5f)

			if(!netvsIsWatch()) {
				owner.receiver.drawDirectFont(x, y+32, "MATCHES", COLOR.CYAN, .5f)
				owner.receiver.drawDirectFont(x, y+40, "${netvsPlayerPlayCount[0]}", COLOR.WHITE, .5f)
				owner.receiver.drawDirectFont(x, y+48, "WINS", COLOR.CYAN, .5f)
				owner.receiver.drawDirectFont(x, y+56, "${netvsPlayerWinCount[0]}", COLOR.WHITE, .5f)
			}
		}
		owner.receiver.drawDirectFont(x, y+72, "ALL ROOMS", COLOR.GREEN, .5f)
		owner.receiver.drawDirectFont(x, y+80, "${netLobby!!.netPlayerClient!!.roomInfoList.size}", COLOR.WHITE, .5f)
	}

	/** NET-VS: Settings screen */
	override fun onSetting(engine:GameEngine, playerID:Int):Boolean {
		if(netCurrentRoomInfo!=null&&playerID==0&&!netvsIsWatch()) {
			netvsPlayerExist[0] = true

			engine.displaysize = 0
			engine.enableSE = true
			engine.isVisible = true

			if(!netvsIsReadyChangePending&&netvsNumPlayers>=2&&!netvsIsNewcomer&&menuTime>=5) {
				// Ready ON
				if(engine.ctrl.isPush(Controller.BUTTON_A)&&!netvsPlayerReady[0]) {
					engine.playSE("decide")
					netvsIsReadyChangePending = true
					netLobby!!.netPlayerClient!!.send("ready\ttrue\n")
				}
				// Ready OFF
				if(engine.ctrl.isPush(Controller.BUTTON_B)&&netvsPlayerReady[0]) {
					engine.playSE("decide")
					netvsIsReadyChangePending = true
					netLobby!!.netPlayerClient!!.send("ready\tfalse\n")
				}
			}

			// Practice Mode
			if(engine.ctrl.isPush(Controller.BUTTON_F)&&menuTime>=5) {
				engine.playSE("decide")
				netvsStartPractice(engine)
				return true
			}
		}

		// Random Map Preview
		if(netCurrentRoomInfo!=null&&netCurrentRoomInfo!!.useMap
			&&!netLobby!!.mapList.isEmpty())
			if(netvsPlayerExist[playerID]) {
				if(menuTime%30==0) {
					engine.statc[5]++
					if(engine.statc[5]>=netLobby!!.mapList.size) engine.statc[5] = 0
					engine.createFieldIfNeeded()
					engine.field.stringToField(netLobby!!.mapList[engine.statc[5]])
					engine.field.setAllSkin(engine.skin)
					engine.field.setAllAttribute(true, Block.ATTRIBUTE.VISIBLE, Block.ATTRIBUTE.OUTLINE)
					engine.field.setAllAttribute(false, Block.ATTRIBUTE.SELF_PLACED)
				}
			} else if(engine.field!=null&&!engine.field.isEmpty) engine.field.reset()

		menuTime++

		return true
	}

	/** NET-VS: Render the settings screen */
	override fun renderSetting(engine:GameEngine, playerID:Int) {
		if(!engine.isVisible) return

		val x = owner.receiver.fieldX(engine, playerID)
		val y = owner.receiver.fieldY(engine, playerID)

		if(netCurrentRoomInfo!=null) {
			if(netvsPlayerReady[playerID]&&netvsPlayerExist[playerID])
				if(engine.displaysize!=-1)
					owner.receiver.drawDirectFont(x+68, y+204, "OK", COLOR.YELLOW)
				else
					owner.receiver.drawDirectFont(x+36, y+80, "OK", COLOR.YELLOW, .5f)

			if(playerID==0&&!netvsIsWatch()&&!netvsIsReadyChangePending&&netvsNumPlayers>=2
				&&!netvsIsNewcomer)
				if(!netvsPlayerReady[playerID]) {
					var strTemp = "A(${owner.receiver.getKeyNameByButtonID(engine, Controller.BUTTON_A)} KEY):"
					if(strTemp.length>10) strTemp = strTemp.take(10)
					owner.receiver.drawMenuFont(engine, playerID, 0, 16, strTemp, COLOR.CYAN)
					owner.receiver.drawMenuFont(engine, playerID, 1, 17, "READY", COLOR.CYAN)
				} else {
					var strTemp = "B(${owner.receiver.getKeyNameByButtonID(engine, Controller.BUTTON_B)} KEY):"
					if(strTemp.length>10) strTemp = strTemp.take(10)
					owner.receiver.drawMenuFont(engine, playerID, 0, 16, strTemp, COLOR.BLUE)
					owner.receiver.drawMenuFont(engine, playerID, 1, 17, "CANCEL", COLOR.BLUE)
				}
		}

		if(playerID==0&&!netvsIsWatch()&&menuTime>=5) {
			var strTemp = "F(${owner.receiver.getKeyNameByButtonID(engine, Controller.BUTTON_F)} KEY):"
			if(strTemp.length>10) strTemp = strTemp.take(10)
			strTemp = strTemp.uppercase()
			owner.receiver.drawMenuFont(engine, playerID, 0, 18, strTemp, COLOR.PURPLE)
			owner.receiver.drawMenuFont(engine, playerID, 1, 19, "PRACTICE", COLOR.PURPLE)
		}
	}

	/** NET-VS: Ready */
	override fun onReady(engine:GameEngine, playerID:Int):Boolean {
		if(engine.statc[0]==0)
		// Map
			if(netCurrentRoomInfo!!.useMap&&netvsMapNo<netLobby!!.mapList.size&&!netvsIsPractice) {
				engine.createFieldIfNeeded()
				engine.field.stringToField(netLobby!!.mapList[netvsMapNo])
				if(playerID==0&&!netvsIsWatch())
					engine.field.setAllSkin(engine.skin)
				else if(netCurrentRoomInfo!!.ruleLock&&netLobby!!.ruleOptLock!=null)
					engine.field.setAllSkin(netLobby!!.ruleOptLock!!.skin)
				else if(netvsPlayerSkin[playerID]>=0) engine.field.setAllSkin(netvsPlayerSkin[playerID])
				engine.field.setAllAttribute(true, Block.ATTRIBUTE.VISIBLE, Block.ATTRIBUTE.OUTLINE)
				engine.field.setAllAttribute(false, Block.ATTRIBUTE.SELF_PLACED)
			}

		if(netvsIsPractice&&engine.statc[0]>=10) netvsIsPracticeExitAllowed = true

		return false
	}

	/** NET-VS: Executed after Ready->Go, before the first piece appears. */
	override fun startGame(engine:GameEngine, playerID:Int) {
		netvsApplyRoomSettings(engine)

		if(playerID==0) {
			// Set BGM
			if(netvsIsPractice)
				owner.bgmStatus.bgm = BGM.Silent
			else {
				owner.bgmStatus.bgm = BGM.Extra(0)
				owner.bgmStatus.fadesw = false
			}

			// Init Variables
			netvsPieceMoveTimer = 0
		}
	}

	/** NET-VS: When the pieces can move */
	override fun onMove(engine:GameEngine, playerID:Int):Boolean {
		// Stop game for remote players
		if(playerID!=0||netvsIsWatch()) return true

		// Timer start
		if(engine.ending==0&&engine.statc[0]==0&&!engine.holdDisable&&!netvsIsPractice) netvsPlayTimerActive = true

		// Send movements
		super.onMove(engine, playerID)

		// Auto lock
		if(engine.ending==0&&engine.nowPieceObject!=null&&netvsPieceMoveTimerMax>0) {
			netvsPieceMoveTimer++
			if(netvsPieceMoveTimer>=netvsPieceMoveTimerMax) {
				engine.nowPieceY = engine.nowPieceBottomY
				engine.lockDelayNow = engine.lockDelay
				netvsPieceMoveTimer = 0
			}
		}

		return false
	}

	/** NET-VS: When the piece locked */
	override fun pieceLocked(engine:GameEngine, playerID:Int, lines:Int) {
		super.pieceLocked(engine, playerID, lines)
		netvsPieceMoveTimer = 0
	}

	/** NET-VS: Executed at the end of each frame */
	override fun onLast(engine:GameEngine, playerID:Int) {
		super.onLast(engine, playerID)

		// Play Timer
		if(playerID==0&&netvsPlayTimerActive) netvsPlayTimer++

		// Automatic start timer
		if(playerID==0&&netCurrentRoomInfo!=null&&netvsAutoStartTimerActive
			&&!netvsIsGameActive)
			when {
				netvsNumPlayers<=1 -> netvsAutoStartTimerActive = false
				netvsAutoStartTimer>0 -> netvsAutoStartTimer--
				else -> {
					if(!netvsIsWatch()) netLobby!!.netPlayerClient!!.send("autostart\n")
					netvsAutoStartTimer = 0
					netvsAutoStartTimerActive = false
				}
			}

		// End practice mode
		if(playerID==0&&netvsIsPractice&&netvsIsPracticeExitAllowed&&engine.ctrl.isPush(Controller.BUTTON_F)) {
			netvsIsPractice = false
			netvsIsPracticeExitAllowed = false
			owner.bgmStatus.bgm = BGM.Silent
			engine.field.reset()
			engine.gameEnded()
			engine.stat = GameEngine.Status.SETTING
			engine.resetStatc()
		}
	}

	/** NET-VS: Render something such as HUD */
	override fun renderLast(engine:GameEngine, playerID:Int) {
		// Player count
		if(playerID==players-1) super.renderLast(engine, playerID)

		// Room info box
		if(playerID==players-1) {
			var x2 = if(owner.receiver.nextDisplayType==2) 544 else 503
			if(owner.receiver.nextDisplayType==2&&netCurrentRoomInfo!!.maxPlayers==2) x2 = 321
			if(owner.receiver.nextDisplayType!=2&&netCurrentRoomInfo!!.maxPlayers==2) x2 = 351

			netvsDrawRoomInfoBox(x2, 286)
		}

		// Elapsed time
		if(playerID==0) {
			owner.receiver.drawDirectFont(256, 16, netvsPlayTimer.toTimeStr)

			if(netvsIsPractice)
				owner.receiver.drawDirectFont(256, 32, engine.statistics.time.toTimeStr,
					COLOR.PURPLE)
		}

		// Automatic start timer
		if(playerID==0&&netCurrentRoomInfo!=null&&netvsAutoStartTimerActive
			&&!netvsIsGameActive)
			owner.receiver.drawDirectFont(496, 16,
				netvsAutoStartTimer.toTimeStr,
				if(netCurrentRoomInfo!!.autoStartTNET2) COLOR.RED else COLOR.YELLOW)

	}

	/** NET-VS: Game Over */
	override fun onGameOver(engine:GameEngine, playerID:Int):Boolean {
		if(engine.statc[0]==0) engine.gameEnded()
		engine.allowTextRenderByReceiver = false
		owner.bgmStatus.bgm = BGM.Silent
		engine.resetFieldVisible()

		// Practice
		if(playerID==0&&netvsIsPractice)
			return if(engine.statc[0]<engine.field.height+1)
				false
			else {
				engine.field.reset()
				engine.stat = GameEngine.Status.RESULT
				engine.resetStatc()
				true
			}

		// 1P died
		if(playerID==0&&!netvsPlayerDead[playerID]&&!netvsIsDeadPending&&!netvsIsWatch()) {
			netSendField(engine)
			netSendNextAndHold(engine)
			netSendStats(engine)

			netLobby!!.netPlayerClient!!.send("dead\t$netvsLastAttackerUID\n")

			netvsPlayerResultReceived[playerID] = true
			netvsIsDeadPending = true
			return true
		}

		// Player/Opponent died
		if(netvsPlayerDead[playerID]) {
			if(engine.field==null) {
				engine.stat = GameEngine.Status.SETTING
				engine.resetStatc()
				return true
			}
			return engine.statc[0]>=engine.field.height+1&&!netvsPlayerResultReceived[playerID]
		}

		return true
	}

	/** NET-VS: Draw Game Over screen */
	override fun renderGameOver(engine:GameEngine, playerID:Int) {
		if(playerID==0&&netvsIsPractice) return
		if(!engine.isVisible) return

		val x = owner.receiver.fieldX(engine, playerID)
		val y = owner.receiver.fieldY(engine, playerID)
		val place = netvsPlayerPlace[playerID]

		if(engine.displaysize!=-1) {
			if(netvsPlayerReady[playerID]&&!netvsIsGameActive)
				owner.receiver.drawDirectFont(x+68, y+204, "OK", COLOR.YELLOW)
			else if(netvsNumNowPlayers==2||netCurrentRoomInfo!!.maxPlayers==2)
				owner.receiver.drawDirectFont(x+20, y+204,
					"YOU LOSE", COLOR.WHITE)
			else if(place==1)
				owner.receiver.drawDirectFont(x+28, y+80,
					"YOU WIN", COLOR.WHITE, .5f)
			else if(place==2)
				owner.receiver.drawDirectFont(x+12, y+204,
					"2ND PLACE", COLOR.WHITE)
			else if(place==3)
				owner.receiver.drawDirectFont(x+12, y+204,
					"3RD PLACE", COLOR.RED)
			else if(place==4)
				owner.receiver.drawDirectFont(x+12, y+204,
					"4TH PLACE", COLOR.GREEN)
			else if(place==5)
				owner.receiver.drawDirectFont(x+12, y+204,
					"5TH PLACE", COLOR.BLUE)
			else if(place==6)
				owner.receiver.drawDirectFont(x+12, y+204,
					"6TH PLACE", COLOR.PURPLE)
		} else if(netvsPlayerReady[playerID]&&!netvsIsGameActive)
			owner.receiver.drawDirectFont(x+36, y+80,
				"OK", COLOR.YELLOW, .5f)
		else if(netvsNumNowPlayers==2||netCurrentRoomInfo!!.maxPlayers==2)
			owner.receiver.drawDirectFont(x+20, y+80,
				"YOU LOSE", COLOR.WHITE, .5f)
		else if(place==1)
			owner.receiver.drawDirectFont(x+28, y+80,
				"YOU WIN", COLOR.WHITE, .5f)
		else if(place==2)
			owner.receiver.drawDirectFont(x+8, y+80,
				"2ND PLACE", COLOR.WHITE, .5f)
		else if(place==3)
			owner.receiver.drawDirectFont(x+8, y+80,
				"3RD PLACE", COLOR.RED, .5f)
		else if(place==4)
			owner.receiver.drawDirectFont(x+8, y+80,
				"4TH PLACE", COLOR.GREEN, .5f)
		else if(place==5)
			owner.receiver.drawDirectFont(x+8, y+80,
				"5TH PLACE", COLOR.BLUE, .5f)
		else if(place==6)
			owner.receiver.drawDirectFont(x+8, y+80,
				"6TH PLACE", COLOR.PURPLE, .5f)
	}

	/** NET-VS: Excellent screen */
	override fun onExcellent(engine:GameEngine, playerID:Int):Boolean {
		engine.allowTextRenderByReceiver = false
		if(playerID==0) netvsPlayerResultReceived[playerID] = true

		if(engine.statc[0]==0) {
			engine.gameEnded()
			owner.bgmStatus.bgm = BGM.Silent
			engine.resetFieldVisible()
			engine.playSE("excellent")
		}

		if(engine.statc[0]>=120&&engine.ctrl.isPush(Controller.BUTTON_A)) engine.statc[0] = engine.field.height+1+180

		if(engine.statc[0]>=engine.field.height+1+180) {
			if(!netvsIsGameActive&&netvsPlayerResultReceived[playerID]) {
				if(engine.field!=null) engine.field.reset()
				engine.resetStatc()
				engine.stat = GameEngine.Status.RESULT
			}
		} else
			engine.statc[0]++

		return true
	}

	/** NET-VS: Draw Excellent screen */
	override fun renderExcellent(engine:GameEngine, playerID:Int) {
		if(!engine.isVisible) return

		val x = owner.receiver.fieldX(engine, playerID)
		val y = owner.receiver.fieldY(engine, playerID)

		if(engine.displaysize!=-1) {
			if(playerID==0&&netvsIsPractice&&!netvsIsWatch())
				owner.receiver.drawDirectFont(x+4, y+204, "EXCELLENT!", COLOR.YELLOW)
			else if(netvsPlayerReady[playerID]&&!netvsIsGameActive)
				owner.receiver.drawDirectFont(x+68, y+204, "OK", COLOR.YELLOW)
			else if(netvsNumNowPlayers==2||netCurrentRoomInfo!!.maxPlayers==2)
				owner.receiver.drawDirectFont(x+52, y+204, "WIN!", COLOR.YELLOW)
			else
				owner.receiver.drawDirectFont(x+4, y+204, "1ST PLACE!", COLOR.YELLOW)
		} else if(playerID==0&&netvsIsPractice&&!netvsIsWatch())
			owner.receiver.drawDirectFont(x+4, y+80, "EXCELLENT!", COLOR.YELLOW, .5f)
		else if(netvsPlayerReady[playerID]&&!netvsIsGameActive)
			owner.receiver.drawDirectFont(x+36, y+80, "OK", COLOR.YELLOW, .5f)
		else if(netvsNumNowPlayers==2||netCurrentRoomInfo!!.maxPlayers==2)
			owner.receiver.drawDirectFont(x+28, y+80, "WIN!", COLOR.YELLOW, .5f)
		else
			owner.receiver.drawDirectFont(x+4, y+80, "1ST PLACE!", COLOR.YELLOW, .5f)
	}

	/** NET-VS: Results screen */
	override fun onResult(engine:GameEngine, playerID:Int):Boolean {
		engine.allowTextRenderByReceiver = false

		if(playerID==0&&!netvsIsWatch()) {
			// To the settings screen
			if(engine.ctrl.isPush(Controller.BUTTON_A)) {
				engine.playSE("decide")
				netvsIsPractice = false
				engine.stat = GameEngine.Status.SETTING
				engine.resetStatc()
				return true
			}
			// Start Practice
			if(engine.ctrl.isPush(Controller.BUTTON_F)) {
				engine.playSE("decide")
				netvsStartPractice(engine)
				return true
			}
		}

		return true
	}

	/** NET-VS: Draw results screen */
	override fun renderResult(engine:GameEngine, playerID:Int) {
		var scale = 1f
		if(engine.displaysize==-1) scale = .5f

		// Place
		if(!netvsIsPractice||playerID!=0) {
			owner.receiver.drawMenuFont(engine, playerID, 0, 0, "RESULT", COLOR.ORANGE, scale)
			if(netvsPlayerPlace[playerID]==1) {
				if(netvsNumNowPlayers==2)
					owner.receiver.drawMenuFont(engine, playerID, 6, 1, "WIN!", COLOR.YELLOW, scale)
				else
					owner.receiver.drawMenuFont(engine, playerID, 6, 1, "1ST!", COLOR.YELLOW, scale)
			} else if(netvsPlayerPlace[playerID]==2) {
				if(netvsNumNowPlayers==2)
					owner.receiver.drawMenuFont(engine, playerID, 6, 1, "LOSE", COLOR.WHITE, scale)
				else
					owner.receiver.drawMenuFont(engine, playerID, 7, 1, "2ND", COLOR.WHITE, scale)
			} else if(netvsPlayerPlace[playerID]==3)
				owner.receiver.drawMenuFont(engine, playerID, 7, 1, "3RD", COLOR.RED, scale)
			else if(netvsPlayerPlace[playerID]==4)
				owner.receiver.drawMenuFont(engine, playerID, 7, 1, "4TH", COLOR.GREEN, scale)
			else if(netvsPlayerPlace[playerID]==5)
				owner.receiver.drawMenuFont(engine, playerID, 7, 1, "5TH", COLOR.BLUE, scale)
			else if(netvsPlayerPlace[playerID]==6)
				owner.receiver.drawMenuFont(engine, playerID, 7, 1, "6TH", COLOR.COBALT, scale)
		} else
			owner.receiver.drawMenuFont(engine, playerID, 0, 0, "PRACTICE", COLOR.PINK, scale)

		if(playerID==0&&!netvsIsWatch()) {
			// Restart/Practice
			var strTemp = "A(${owner.receiver.getKeyNameByButtonID(engine, Controller.BUTTON_A)} KEY):"
			if(strTemp.length>10) strTemp = strTemp.take(10)
			owner.receiver.drawMenuFont(engine, playerID, 0, 18, strTemp, COLOR.RED)
			owner.receiver.drawMenuFont(engine, playerID, 1, 19, "RESTART", COLOR.RED)

			var strTempF = "F(${owner.receiver.getKeyNameByButtonID(engine, Controller.BUTTON_F)} KEY):"
			if(strTempF.length>10) strTempF = strTempF.take(10)
			owner.receiver.drawMenuFont(engine, playerID, 0, 20, strTempF, COLOR.PURPLE)
			if(!netvsIsPractice)
				owner.receiver.drawMenuFont(engine, playerID, 1, 21, "PRACTICE", COLOR.PURPLE)
			else
				owner.receiver.drawMenuFont(engine, playerID, 1, 21, "RETRY", COLOR.PURPLE)
		} else if(netvsPlayerReady[playerID]&&netvsPlayerExist[playerID]) {
			// Player Ready
			val x = owner.receiver.fieldX(engine, playerID)
			val y = owner.receiver.fieldY(engine, playerID)

			if(engine.displaysize!=-1)
				owner.receiver.drawDirectFont(x+68, y+356, "OK", COLOR.YELLOW)
			else
				owner.receiver.drawDirectFont(x+36, y+156, "OK", COLOR.YELLOW, .5f)
		}
	}

	/** NET-VS: No retry key. */
	override fun netplayOnRetryKey(engine:GameEngine, playerID:Int) {}

	/** NET-VS: Disconnected */
	override fun netlobbyOnDisconnect(lobby:NetLobbyFrame, client:NetPlayerClient, ex:Throwable?) {
		for(i in 0 until players)
			owner.engine[i].stat = GameEngine.Status.NOTHING
	}

	/** NET-VS: Message received */
	@Throws(IOException::class)
	override fun netlobbyOnMessage(lobby:NetLobbyFrame, client:NetPlayerClient, message:Array<String>) {
		// Player status update
		if(message[0]=="playerupdate") {
			val pInfo = NetPlayerInfo(message[1])

			// Ready status change
			if(pInfo.roomID==netCurrentRoomInfo!!.roomID&&pInfo.seatID!=-1) {
				val playerID = netvsGetPlayerIDbySeatID(pInfo.seatID)

				if(netvsPlayerReady[playerID]!=pInfo.ready) {
					netvsPlayerReady[playerID] = pInfo.ready

					if(playerID==0&&!netvsIsWatch())
						netvsIsReadyChangePending = false
					else if(pInfo.ready) owner.receiver.playSE("decide")
					else if(!pInfo.playing) owner.receiver.playSE("change")
				}
			}

			netUpdatePlayerExist()
		}
		// When someone logout
		if(message[0]=="playerlogout") {
			val pInfo = NetPlayerInfo(message[1])

			if(pInfo.roomID==netCurrentRoomInfo!!.roomID&&pInfo.seatID!=-1) netUpdatePlayerExist()
		}
		// Player status change (Join/Watch)
		if(message[0]=="changestatus") {
			val uid = message[2].toInt()

			netUpdatePlayerExist()
			netvsSetGameScreenLayout()

			if(uid==netLobby!!.netPlayerClient!!.playerUID) {
				netvsIsPractice = false
				if(netvsIsGameActive&&!netvsIsWatch()) netvsIsNewcomer = true

				owner.engine[0].stat = GameEngine.Status.SETTING

				for(i in 0 until players) {
					if(owner.engine[i].field!=null) owner.engine[i].field.reset()
					owner.engine[i].nowPieceObject = null

					if(owner.engine[i].stat==GameEngine.Status.NOTHING||!netvsIsGameActive)
						owner.engine[i].stat = GameEngine.Status.SETTING
					owner.engine[i].resetStatc()
				}
			}
		}
		// Someone entered here
		if(message[0]=="playerenter") {
			val seatID = message[3].toInt()
			if(seatID!=-1&&netvsNumPlayers<2) owner.receiver.playSE("levelstop")
		}
		// Someone leave here
		if(message[0]=="playerleave") {
			netUpdatePlayerExist()

			if(netvsNumPlayers<2) netvsAutoStartTimerActive = false
		}
		// Automatic timer start
		if(message[0]=="autostartbegin")
			if(netvsNumPlayers>=2) {
				val seconds = message[1].toInt()
				netvsAutoStartTimer = seconds*60
				netvsAutoStartTimerActive = true
			}
		// Automatic timer stop
		if(message[0]=="autostartstop") netvsAutoStartTimerActive = false
		// Game Started
		if(message[0]=="start") {
			val randseed = message[1].toLong(16)
			netvsNumNowPlayers = message[2].toInt()
			netvsNumAlivePlayers = netvsNumNowPlayers
			netvsMapNo = message[3].toInt()

			netvsResetFlags()
			netUpdatePlayerExist()

			owner.menuOnly = false
			owner.bgmStatus.reset()
			owner.backgroundStatus.reset()
			owner.replayProp.clear()
			for(i in 0 until players)
				if(netvsPlayerExist[i]) {
					owner.engine[i].init()
					netvsSetGameScreenLayout(owner.engine[i])
				}

			netvsAutoStartTimerActive = false
			netvsIsGameActive = true
			netvsIsGameFinished = false
			netvsPlayTimer = 0

			netvsSetLockedRule() // Set locked rule/Restore rule

			for(i in 0 until players) {
				val engine = owner.engine[i]
				engine.resetStatc()

				if(netvsPlayerExist[i]) {
					netvsPlayerActive[i] = true
					engine.stat = GameEngine.Status.READY
					engine.randSeed = randseed
					engine.random = Random(randseed)

					if(netCurrentRoomInfo!!.maxPlayers==2&&netvsNumPlayers==2) {
						engine.isVisible = true
						engine.displaysize = 0

						if(netCurrentRoomInfo!!.ruleLock||i==0&&!netvsIsWatch()) {
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

					if(netCurrentRoomInfo!!.maxPlayers==2&&netvsNumPlayers==2) engine.isVisible = false
				} else {
					engine.stat = GameEngine.Status.SETTING
					engine.isVisible = false
				}

				netvsPlayerResultReceived[i] = false
				netvsPlayerDead[i] = false
				netvsPlayerReady[i] = false
			}
		}
		// Dead
		if(message[0]=="dead") {
			val seatID = message[3].toInt()
			val playerID = netvsGetPlayerIDbySeatID(seatID)

			if(!netvsPlayerDead[playerID]) {
				netvsPlayerDead[playerID] = true
				netvsPlayerPlace[playerID] = message[4].toInt()
				owner.engine[playerID].stat = GameEngine.Status.GAMEOVER
				owner.engine[playerID].resetStatc()
				netvsNumAlivePlayers--

				if(seatID==netLobby!!.netPlayerClient!!.yourPlayerInfo!!.seatID) {
					if(!netvsIsDeadPending) {
						// Forced death
						netSendField(owner.engine[0])
						netSendNextAndHold(owner.engine[0])
						netSendStats(owner.engine[0])
						netvsPlayerResultReceived[0] = true
					}

					// Send end game stats
					netSendEndGameStats(owner.engine[0])
				}
			}
		}
		// End-of-game Stats
		if(message[0]=="gstat") netvsRecvEndGameStats(message)
		// Game Finished
		if(message[0]=="finish") {
			netvsIsGameActive = false
			netvsIsGameFinished = true
			netvsPlayTimerActive = false
			netvsIsNewcomer = false

			// Stop practice game
			if(netvsIsPractice) {
				netvsIsPractice = false
				netvsIsPracticeExitAllowed = false
				owner.bgmStatus.bgm = BGM.Silent
				owner.engine[0].gameEnded()
				owner.engine[0].stat = GameEngine.Status.SETTING
				owner.engine[0].resetStatc()
			}

			val flagTeamWin = message[4].toBoolean()

			if(flagTeamWin) {
				// Team won
				for(i in 0 until players)
					if(netvsPlayerExist[i]&&!netvsPlayerDead[i]) {
						netvsPlayerPlace[i] = 1
						owner.engine[i].gameEnded()
						owner.engine[i].stat = GameEngine.Status.EXCELLENT
						owner.engine[i].resetStatc()
						owner.engine[i].statistics.time = netvsPlayTimer
						netvsNumAlivePlayers--

						if(i==0&&!netvsIsWatch()) netSendEndGameStats(owner.engine[0])
					}
			} else {
				// Normal player won
				val seatID = message[2].toInt()
				if(seatID!=-1) {
					val playerID = netvsGetPlayerIDbySeatID(seatID)
					if(netvsPlayerExist[playerID]) {
						netvsPlayerPlace[playerID] = 1
						owner.engine[playerID].gameEnded()
						owner.engine[playerID].stat = GameEngine.Status.EXCELLENT
						owner.engine[playerID].resetStatc()
						owner.engine[playerID].statistics.time = netvsPlayTimer
						netvsNumAlivePlayers--

						if(seatID==netLobby!!.netPlayerClient!!.yourPlayerInfo!!.seatID&&!netvsIsWatch())
							netSendEndGameStats(owner.engine[0])
					}
				}
			}

			if(netvsIsWatch()||netvsPlayerPlace[0]>=3) owner.receiver.playSE("matchend")

			netUpdatePlayerExist()
		}
		// Game messages
		if(message[0]=="game") {
			//int uid = message[1].toInt();
			val seatID = message[2].toInt()
			val playerID = netvsGetPlayerIDbySeatID(seatID)
			val engine = owner.engine[playerID]

			if(engine.field==null) engine.createFieldIfNeeded()

			// Field
			if(message[3]=="field"||message[3]=="fieldattr") netRecvField(engine, message)
			// Stats
			if(message[3]=="stats") netRecvStats(engine, message)
			// Current Piece
			if(message[3]=="piece") {
				netRecvPieceMovement(engine, message)

				// Play timer start
				if(netvsIsWatch()&&!netvsIsNewcomer&&!netvsPlayTimerActive&&!netvsIsGameFinished) {
					netvsPlayTimerActive = true
					netvsPlayTimer = 0
				}

				// Force start
				if(!netvsIsWatch()&&netvsPlayTimerActive&&!netvsIsPractice&&
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
		internal const val NETVS_MAX_PLAYERS = 6

		/** NET-VS: Numbers of seats numbers corresponding to frames on player's
		 * screen */
		private val NETVS_GAME_SEAT_NUMBERS =
			arrayOf(intArrayOf(0, 1, 2, 3, 4, 5), intArrayOf(1, 0, 2, 3, 4, 5), intArrayOf(1, 2, 0, 3, 4, 5),
				intArrayOf(1, 2, 3, 0, 4, 5), intArrayOf(1, 2, 3, 4, 0, 5), intArrayOf(1, 2, 3, 4, 5, 0))

		/** NET-VS: Each player's garbage block cint */
		internal val NETVS_PLAYER_COLOR_BLOCK =
			arrayOf(Block.COLOR.RED, Block.COLOR.BLUE, Block.COLOR.GREEN, Block.COLOR.YELLOW,
				Block.COLOR.PURPLE, Block.COLOR.CYAN)

		/** NET-VS: Each player's frame cint */
		private val NETVS_PLAYER_COLOR_FRAME =
			intArrayOf(GameEngine.FRAME_COLOR_RED, GameEngine.FRAME_COLOR_BLUE, GameEngine.FRAME_COLOR_GREEN,
				GameEngine.FRAME_COLOR_BRONZE, GameEngine.FRAME_COLOR_PURPLE, GameEngine.FRAME_COLOR_CYAN)

		/** NET-VS: Team font colors */
		private val NETVS_TEAM_FONT_COLORS =
			arrayOf(COLOR.WHITE, COLOR.RED, COLOR.GREEN, COLOR.BLUE, COLOR.YELLOW, COLOR.PURPLE, COLOR.CYAN)

		/** NET-VS: Default time before forced piece lock */
		private const val NETVS_PIECE_AUTO_LOCK_TIME = 30*60
	}
}
