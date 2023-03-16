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
package mu.nu.nullpo.gui.slick

import mu.nu.nullpo.game.component.BGMStatus.BGM
import mu.nu.nullpo.game.component.RuleOptions
import mu.nu.nullpo.game.net.NetPlayerClient
import mu.nu.nullpo.game.net.NetRoomInfo
import mu.nu.nullpo.game.play.GameManager
import mu.nu.nullpo.game.play.GameStyle
import mu.nu.nullpo.game.subsystem.mode.NetDummyMode
import mu.nu.nullpo.gui.common.GameKeyDummy
import mu.nu.nullpo.gui.net.NetLobbyFrame
import mu.nu.nullpo.gui.net.NetLobbyListener
import mu.nu.nullpo.util.GeneralUtil
import org.apache.logging.log4j.LogManager
import org.newdawn.slick.AppGameContainer
import org.newdawn.slick.GameContainer
import org.newdawn.slick.Graphics
import org.newdawn.slick.state.BasicGameState
import org.newdawn.slick.state.StateBasedGame

/** ネットゲーム画面のステート */
class StateNetGame:BasicGameState(), NetLobbyListener {
	/** ゲームのメインクラス */
	var gameManager:GameManager? = null

	/** ロビー画面 */
	var netLobby:NetLobbyFrame = NetLobbyFrame()

	/** FPS表示 */
	private var showFPS = true

	/** Screenshot撮影 flag */
	private var ssFlag = false

	/** Show background flag */
	private var showBG = true

	/** Previous ingame flag (Used by title-bar text change) */
	private var prevInGameFlag = false

	/** AppGameContainer (これを使ってタイトルバーを変える) */
	private var appContainer:AppGameContainer? = null

	/** Mode name to enter (null=Exit) */
	private var strModeToEnter = ""

	/** Current game mode name */
	private var modeName = ""

	/* Fetch this state's ID */
	override fun getID():Int = ID

	/* State initialization */
	override fun init(container:GameContainer, game:StateBasedGame) {
		appContainer = container as AppGameContainer
	}

	/* Called when entering this state */
	override fun enter(container:GameContainer?, game:StateBasedGame?) {
		// Init variables
		showBG = NullpoMinoSlick.propConfig.getProperty("option.showbg", true)
		prevInGameFlag = false

		// Observer stop
		NullpoMinoSlick.stopObserverClient()

		// 60FPS
		NullpoMinoSlick.altMaxFPS = 60
		appContainer?.alwaysRender = true
		appContainer?.setUpdateOnlyWhenVisible(false)

		// Clear each frame
		appContainer?.setClearEachFrame(!showBG)

		// gameManager initialization
		gameManager = GameManager(RendererSlick(appContainer!!.graphics))

		// Lobby initialization
		netLobby.addListener(this)

		// Mode initialization
		enterNewMode(null)

		if(ResourceHolder.bgmPlaying!=BGM.Menu(1)) ResourceHolder.bgmStart(BGM.Menu(1))
		// Lobby start
		netLobby.init()
		netLobby.isVisible = true
	}

	/* Called when leaving this state */
	override fun leave(container:GameContainer?, game:StateBasedGame?) {
		gameManager?.shutdown()
		gameManager = null

		netLobby.shutdown()
		ResourceHolder.bgmStop()
		container?.setClearEachFrame(false)

		// FPS restore
		NullpoMinoSlick.altMaxFPS = NullpoMinoSlick.propConfig.getProperty("option.maxfps", 60)
		appContainer?.alwaysRender = !NullpoMinoSlick.alternateFPSTiming
		appContainer?.setUpdateOnlyWhenVisible(true)

		// Reload global config (because it can change rules)
		NullpoMinoSlick.loadGlobalConfig()
	}

	/* Draw the game screen */
	override fun render(container:GameContainer, game:StateBasedGame, g:Graphics) {
		try {
			// ゲーム画面
			if(gameManager?.mode!=null) gameManager?.renderAll()

			// FPS
			NullpoMinoSlick.drawFPS()
			// Screenshot
			if(ssFlag) {
				NullpoMinoSlick.saveScreenShot(container, g)
				ssFlag = false
			}

			if(!NullpoMinoSlick.alternateFPSTiming) NullpoMinoSlick.alternateFPSSleep(true)
		} catch(e:NullPointerException) {
			try {
				if(gameManager==null||gameManager?.quitFlag==false) log.error("render NPE", e)
			} catch(_:Throwable) {
			}
		} catch(e:Exception) {
			try {
				if(gameManager==null||gameManager?.quitFlag==false) log.error("render fail", e)
			} catch(_:Throwable) {
			}
		}
	}

	/* Update game state */
	override fun update(container:GameContainer, game:StateBasedGame, delta:Int) {
		try {
			// Clear input states if game window does not have focus
			if(!container.hasFocus()||netLobby.isFocused) GameKey.gameKey[0].clear()

			// TTF font 描画
			ResourceHolder.ttfFont?.loadGlyphs()

			// Title bar update

			gameManager?.also {gameManager ->
				if(gameManager.engine.isNotEmpty()) {
					val nowInGame = gameManager.engine[0].isInGame
					if(prevInGameFlag!=nowInGame) {
						prevInGameFlag = nowInGame
						updateTitleBarCaption()
					}
				}
			}

			// Update key input states
			if(container.hasFocus()&&!netLobby.isFocused)
				gameManager?.also {gameManager ->
					GameKey.gameKey[0].update(container.input, gameManager.engine.size>0&&gameManager.engine[0].isInGame)

					gameManager.mode?.let {mode ->
						// BGM
						if(ResourceHolder.bgmPlaying!=gameManager.musMan.bgm) ResourceHolder.bgmStart(gameManager.musMan.bgm)
						if(ResourceHolder.bgmIsPlaying) {
							val basevolume = NullpoMinoSlick.propConfig.getProperty("option.bgmvolume", 128)
							val basevolume2 = basevolume/128.toFloat()
							var newvolume = gameManager.musMan.volume*basevolume2
							if(newvolume<0f) newvolume = 0f
							if(newvolume>1f) newvolume = 1f
							container.musicVolume = newvolume
							if(newvolume<=0f) ResourceHolder.bgmStop()
						}

						// ゲームの処理を実行
						GameKey.gameKey[0].inputStatusUpdate(gameManager.engine[0].ctrl)
						gameManager.updateAll()

						if(gameManager.quitFlag) {
							ResourceHolder.bgmStop()
							game.enterState(StateTitle.ID)
							return@update
						}

						// Retry button
						if(GameKey.gameKey[0].isPushKey(GameKeyDummy.BUTTON_RETRY))
							mode.netplayOnRetryKey(gameManager.engine[0])
					}
				}
			// Screenshot button
			if(GameKey.gameKey[0].isPushKey(GameKeyDummy.BUTTON_SCREENSHOT)||
				GameKey.gameKey[1].isPushKey(GameKeyDummy.BUTTON_SCREENSHOT)
			)
				ssFlag = true

			// Enter to new mode
			if(strModeToEnter.isNotEmpty()) {
				enterNewMode(strModeToEnter)
				strModeToEnter = ""
			}

			if(NullpoMinoSlick.alternateFPSTiming) NullpoMinoSlick.alternateFPSSleep(true)
		} catch(e:NullPointerException) {
			try {
				if(gameManager?.quitFlag==true) {
					game.enterState(StateTitle.ID)
					return
				}
				log.error("update NPE", e)
			} catch(_:Throwable) {
			}
		} catch(e:Exception) {
			try {
				if(gameManager?.quitFlag==true) {
					game.enterState(StateTitle.ID)
					return
				}
				log.error("update fail", e)
			} catch(_:Throwable) {
			}
		}
	}

	/** Enter to a new mode
	 * @param newModeName Mode name
	 */
	private fun enterNewMode(newModeName:String?) {
		NullpoMinoSlick.loadGlobalConfig() // Reload global config file
		val gm = gameManager ?: return
		val previousMode = gm.mode

		when(val newModeTemp = NullpoMinoSlick.modeManager[newModeName]) {
			null -> log.error("Cannot find a mode:$newModeName")
			is NetDummyMode -> {
				log.info("Enter new mode:${newModeTemp.id}")

				modeName = newModeTemp.name
				gm.engine.forEach {it.ai?.shutdown()}
				previousMode?.netplayUnload(netLobby)

				gm.mode = newModeTemp
				gm.init()
				gm.engine[0].let {
					// Tuning
					it.owSpinDirection = NullpoMinoSlick.propGlobal.getProperty(
						"0.tuning.owRotateButtonDefaultRight", -1
					)
					it.owSkin = NullpoMinoSlick.propGlobal.getProperty("0.tuning.owSkin", -1)
					it.owMinDAS = NullpoMinoSlick.propGlobal.getProperty("0.tuning.owMinDAS", -1)
					it.owMaxDAS = NullpoMinoSlick.propGlobal.getProperty("0.tuning.owMaxDAS", -1)
					it.owARR = NullpoMinoSlick.propGlobal.getProperty("0.tuning.owDasDelay", -1)
					it.owSDSpd = NullpoMinoSlick.propGlobal.getProperty("0.tuning.owSDSpd", -1)
					it.owReverseUpDown = NullpoMinoSlick.propGlobal.getProperty("0.tuning.owReverseUpDown", false)
					it.owMoveDiagonal = NullpoMinoSlick.propGlobal.getProperty("0.tuning.owMoveDiagonal", -1)
					it.owBlockOutlineType = NullpoMinoSlick.propGlobal.getProperty("0.tuning.owBlockOutlineType", -1)
					it.owBlockShowOutlineOnly = NullpoMinoSlick.propGlobal.getProperty("0.tuning.owBlockShowOutlineOnly", -1)
					it.owDelayCancel = NullpoMinoSlick.propGlobal.getProperty("0.tuning.owDelayCancel", -1)
					// Rule

					val ruleName = NullpoMinoSlick.propGlobal.getProperty(
						if(gm.mode?.gameStyle==GameStyle.TETROMINO) "0.rule" else "0.rule.${gm.mode?.gameStyle}", ""
					)
					val ruleOpt:RuleOptions = if(ruleName.isNotEmpty()) {
						log.info("Load rule options from $ruleName")
						GeneralUtil.loadRule(ruleName)
					} else {
						log.info("Load rule options from setting file")
						RuleOptions().apply {
							readProperty(NullpoMinoSlick.propGlobal, 0)
						}
					}
					it.ruleOpt = ruleOpt

					// Randomizer
					if(ruleOpt.strRandomizer.isNotEmpty()) it.randomizer = GeneralUtil.loadRandomizer(ruleOpt.strRandomizer)
					// Wallkick
					if(ruleOpt.strWallkick.isNotEmpty()) it.wallkick = GeneralUtil.loadWallkick(ruleOpt.strWallkick)

					// AI
					val aiName = NullpoMinoSlick.propGlobal.getProperty("0.ai", "")
					if(aiName.isNotEmpty()) {
						it.ai = GeneralUtil.loadAIPlayer(aiName)
						it.aiMoveDelay = NullpoMinoSlick.propGlobal.getProperty("0.aiMoveDelay", 0)
						it.aiThinkDelay = NullpoMinoSlick.propGlobal.getProperty("0.aiThinkDelay", 0)
						it.aiUseThread = NullpoMinoSlick.propGlobal.getProperty("0.aiUseThread", true)
						it.aiShowHint = NullpoMinoSlick.propGlobal.getProperty("0.aiShowHint", false)
						it.aiPreThink = NullpoMinoSlick.propGlobal.getProperty("0.aiPreThink", false)
						it.aiShowState = NullpoMinoSlick.propGlobal.getProperty("0.aiShowState", false)
					}
				}
				gm.showInput = NullpoMinoSlick.propConfig.getProperty("option.showInput", false)

				// Initialization for each player
				for(i in 0 until gm.players)
					gm.engine[i].init()

				newModeTemp.netplayInit(netLobby)
			}
			else -> log.error("This mode does not support netplay:$newModeName")
		}

		updateTitleBarCaption()
	}

	/** Update title bar text */
	fun updateTitleBarCaption() {
		var strTitle = "NullpoMino Netplay"

		gameManager?.also {
			if(modeName!=="NET-DUMMY"&&it.engine.isNotEmpty())
				strTitle = "[${
					if(it.engine[0].isInGame&&!it.replayMode&&!it.replayRerecord)
						"PLAY" else "Menu"
				}] NullpoMino NetPlay - $modeName"
		}
		appContainer?.setTitle(strTitle)
	}

	override fun netlobbyOnDisconnect(lobby:NetLobbyFrame, client:NetPlayerClient, ex:Throwable?) {
		strModeToEnter = ""
	}

	override fun netlobbyOnExit(lobby:NetLobbyFrame) {
		gameManager?.also {
			if(it.engine.isNotEmpty()) it.engine[0].quitFlag = true
		}
	}

	override fun netlobbyOnInit(lobby:NetLobbyFrame) {}

	override fun netlobbyOnLoginOK(lobby:NetLobbyFrame, client:NetPlayerClient) {}

	override fun netlobbyOnMessage(lobby:NetLobbyFrame, client:NetPlayerClient, message:List<String>) {}

	override fun netlobbyOnRoomJoin(lobby:NetLobbyFrame, client:NetPlayerClient, roomInfo:NetRoomInfo) {
		//enterNewMode(roomInfo.strMode);
		strModeToEnter = roomInfo.strMode
	}

	override fun netlobbyOnRoomLeave(lobby:NetLobbyFrame, client:NetPlayerClient) {
		//enterNewMode(null);
		strModeToEnter = ""
	}

	companion object {
		/** Log */
		internal val log = LogManager.getLogger()

		/** This state's ID */
		const val ID = 11
	}
}
