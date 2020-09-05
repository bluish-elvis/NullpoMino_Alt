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
package mu.nu.nullpo.gui.slick

import mu.nu.nullpo.game.component.RuleOptions
import mu.nu.nullpo.game.net.NetPlayerClient
import mu.nu.nullpo.game.net.NetRoomInfo
import mu.nu.nullpo.game.play.GameManager
import mu.nu.nullpo.game.subsystem.mode.NetDummyMode
import mu.nu.nullpo.gui.GameKeyDummy
import mu.nu.nullpo.gui.net.NetLobbyFrame
import mu.nu.nullpo.gui.net.NetLobbyListener
import mu.nu.nullpo.util.GeneralUtil
import org.apache.log4j.Logger
import org.newdawn.slick.*
import org.newdawn.slick.state.BasicGameState
import org.newdawn.slick.state.StateBasedGame

/** ネットゲーム画面のステート */
class StateNetGame:BasicGameState(), NetLobbyListener {

	/** ゲームのメインクラス */
	var gameManager:GameManager? = null

	/** ロビー画面 */
	var netLobby:NetLobbyFrame = NetLobbyFrame()

	/** FPS表示 */
	private var showfps = true

	/** Screenshot撮影 flag */
	private var ssflag = false

	/** Show background flag */
	private var showbg = true

	/** Previous ingame flag (Used by title-bar text change) */
	private var prevInGameFlag = false

	/** AppGameContainer (これを使ってタイトルバーを変える) */
	private var appContainer:AppGameContainer? = null

	/** Mode name to enter (null=Exit) */
	private var strModeToEnter:String = ""

	/** Current game mode name */
	private var modeName:String = ""

	/* Fetch this state's ID */
	override fun getID():Int = ID

	/* State initialization */
	override fun init(container:GameContainer, game:StateBasedGame) {
		appContainer = container as AppGameContainer
	}

	/* Called when entering this state */
	override fun enter(container:GameContainer?, game:StateBasedGame?) {
		// Init variables
		showbg = NullpoMinoSlick.propConfig.getProperty("option.showbg", true)
		prevInGameFlag = false

		// Observer stop
		NullpoMinoSlick.stopObserverClient()

		// 60FPS
		NullpoMinoSlick.altMaxFPS = 60
		appContainer?.alwaysRender = true
		appContainer?.setUpdateOnlyWhenVisible(false)

		// Clear each frame
		appContainer?.setClearEachFrame(!showbg)

		// gameManager initialization
		gameManager = GameManager(RendererSlick()).apply {
			receiver.setGraphics(appContainer!!.graphics)
		}

		// Lobby initialization
		netLobby.addListener(this)

		// Mode initialization
		enterNewMode(null)

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
			if(ssflag) {
				NullpoMinoSlick.saveScreenShot(container, g)
				ssflag = false
			}

			if(!NullpoMinoSlick.alternateFPSTiming) NullpoMinoSlick.alternateFPSSleep(true)
		} catch(e:NullPointerException) {
			try {
				if(gameManager==null||!gameManager!!.quitFlag) log.error("render NPE", e)
			} catch(e2:Throwable) {
			}

		} catch(e:Exception) {
			try {
				if(gameManager==null||!gameManager!!.quitFlag) log.error("render fail", e)
			} catch(e2:Throwable) {
			}

		}

	}

	/* Update game state */
	override fun update(container:GameContainer, game:StateBasedGame, delta:Int) {
		try {
			// Clear input states if game window does not have focus
			if(!container.hasFocus()||netLobby.isFocused) GameKey.gamekey[0].clear()

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
					GameKey.gamekey[0].update(container.input, gameManager.engine.size>0&&gameManager.engine[0].isInGame)

					gameManager.mode?.let {mode->
						// BGM
						if(ResourceHolder.bgmPlaying!=gameManager.bgmStatus.bgm) ResourceHolder.bgmStart(gameManager.bgmStatus.bgm)
						if(ResourceHolder.bgmIsPlaying()) {
							val basevolume = NullpoMinoSlick.propConfig.getProperty("option.bgmvolume", 128)
							val basevolume2 = basevolume/128.toFloat()
							var newvolume = gameManager.bgmStatus.volume*basevolume2
							if(newvolume<0f) newvolume = 0f
							if(newvolume>1f) newvolume = 1f
							container.musicVolume = newvolume
							if(newvolume<=0f) ResourceHolder.bgmStop()
						}

						// ゲームの処理を実行
						GameKey.gamekey[0].inputStatusUpdate(gameManager.engine[0].ctrl)
						gameManager.updateAll()

						if(gameManager.quitFlag) {
							ResourceHolder.bgmStop()
							game.enterState(StateTitle.ID)
							return
						}

						// Retry button
						if(GameKey.gamekey[0].isPushKey(GameKeyDummy.BUTTON_RETRY))
							mode.netplayOnRetryKey(gameManager.engine[0], 0)
					}
				}
			// Screenshot button
			if(GameKey.gamekey[0].isPushKey(GameKeyDummy.BUTTON_SCREENSHOT)||GameKey.gamekey[1].isPushKey(GameKeyDummy.BUTTON_SCREENSHOT))
				ssflag = true

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
			} catch(e2:Throwable) {
			}

		} catch(e:Exception) {
			try {
				if(gameManager?.quitFlag==true) {
					game.enterState(StateTitle.ID)
					return
				}
				log.error("update fail", e)
			} catch(e2:Throwable) {
			}

		}

	}

	/** Enter to a new mode
	 * @param newModeName Mode name
	 */
	private fun enterNewMode(newModeName:String?) {
		NullpoMinoSlick.loadGlobalConfig() // Reload global config file

		val previousMode = gameManager!!.mode

		when(val newModeTemp = if(newModeName==null) NetDummyMode() else NullpoMinoSlick.modeManager.getMode(newModeName)) {
			null -> log.error("Cannot find a mode:"+newModeName!!)
			is NetDummyMode -> {
				log.info("Enter new mode:"+newModeTemp.id)

				modeName = newModeTemp.name

				if(previousMode!=null) {
					if(gameManager!!.engine[0].ai!=null) gameManager!!.engine[0].ai!!.shutdown(gameManager!!.engine[0], 0)
					previousMode.netplayUnload(netLobby)
				}
				gameManager!!.mode = newModeTemp
				gameManager!!.init()

				// Tuning
				gameManager!!.engine[0].owRotateButtonDefaultRight = NullpoMinoSlick.propGlobal.getProperty(0.toString()+".tuning.owRotateButtonDefaultRight", -1)
				gameManager!!.engine[0].owSkin = NullpoMinoSlick.propGlobal.getProperty(0.toString()+".tuning.owSkin", -1)
				gameManager!!.engine[0].owMinDAS = NullpoMinoSlick.propGlobal.getProperty(0.toString()+".tuning.owMinDAS", -1)
				gameManager!!.engine[0].owMaxDAS = NullpoMinoSlick.propGlobal.getProperty(0.toString()+".tuning.owMaxDAS", -1)
				gameManager!!.engine[0].owARR = NullpoMinoSlick.propGlobal.getProperty(0.toString()+".tuning.owDasDelay", -1)
				gameManager!!.engine[0].owReverseUpDown = NullpoMinoSlick.propGlobal.getProperty(0.toString()+".tuning.owReverseUpDown", false)
				gameManager!!.engine[0].owMoveDiagonal = NullpoMinoSlick.propGlobal.getProperty(0.toString()+".tuning.owMoveDiagonal", -1)
				gameManager!!.engine[0].owBlockOutlineType = NullpoMinoSlick.propGlobal.getProperty(0.toString()+".tuning.owBlockOutlineType", -1)
				gameManager!!.engine[0].owBlockShowOutlineOnly = NullpoMinoSlick.propGlobal.getProperty(0.toString()+".tuning.owBlockShowOutlineOnly", -1)

				// Rule
				val ruleopt:RuleOptions
				var rulename:String? = NullpoMinoSlick.propGlobal.getProperty(0.toString()+".rule", "")
				if(gameManager!!.mode!!.gameStyle>0)
					rulename = NullpoMinoSlick.propGlobal.getProperty(0.toString()+".rule."
						+gameManager!!.mode!!.gameStyle, "")
				if(rulename!=null&&rulename.isNotEmpty()) {
					log.info("Load rule options from $rulename")
					ruleopt = GeneralUtil.loadRule(rulename)
				} else {
					log.info("Load rule options from setting file")
					ruleopt = RuleOptions()
					ruleopt.readProperty(NullpoMinoSlick.propGlobal, 0)
				}
				gameManager!!.engine[0].ruleopt = ruleopt

				// Randomizer
				if(ruleopt.strRandomizer.isNotEmpty())
					gameManager!!.engine[0].randomizer = GeneralUtil.loadRandomizer(ruleopt.strRandomizer)


				// Wallkick
				if(ruleopt.strWallkick.isNotEmpty())
					gameManager!!.engine[0].wallkick = GeneralUtil.loadWallkick(ruleopt.strWallkick)


				// AI
				val aiName = NullpoMinoSlick.propGlobal.getProperty(0.toString()+".ai", "")
				if(aiName.isNotEmpty()) {
					gameManager!!.engine[0].ai = GeneralUtil.loadAIPlayer(aiName)
					gameManager!!.engine[0].aiMoveDelay = NullpoMinoSlick.propGlobal.getProperty(0.toString()+".aiMoveDelay", 0)
					gameManager!!.engine[0].aiThinkDelay = NullpoMinoSlick.propGlobal.getProperty(0.toString()+".aiThinkDelay", 0)
					gameManager!!.engine[0].aiUseThread = NullpoMinoSlick.propGlobal.getProperty(0.toString()+".aiUseThread", true)
					gameManager!!.engine[0].aiShowHint = NullpoMinoSlick.propGlobal.getProperty(0.toString()+".aiShowHint", false)
					gameManager!!.engine[0].aiPrethink = NullpoMinoSlick.propGlobal.getProperty(0.toString()+".aiPrethink", false)
					gameManager!!.engine[0].aiShowState = NullpoMinoSlick.propGlobal.getProperty(0.toString()+".aiShowState", false)
				}
				gameManager!!.showInput = NullpoMinoSlick.propConfig.getProperty("option.showInput", false)

				// Initialization for each player
				for(i in 0 until gameManager!!.players)
					gameManager!!.engine[i].init()

				newModeTemp.netplayInit(netLobby)
			}
			else -> log.error("This mode does not support netplay:"+newModeName!!)
		}

		updateTitleBarCaption()
	}

	/** Update title bar text */
	fun updateTitleBarCaption() {
		var strTitle = "NullpoMino Netplay"

		gameManager?.also {
			if(modeName!=="NET-DUMMY" && it.engine.isNotEmpty())
				strTitle = "[${if(it.engine[0].isInGame&&!it.replayMode&&!it.replayRerecord)
					"PLAY" else "MENU"
				}] NullpoMino Netplay - $modeName"
		}
		appContainer!!.setTitle(strTitle)
	}

	override fun netlobbyOnDisconnect(lobby:NetLobbyFrame, client:NetPlayerClient, ex:Throwable?) {
		strModeToEnter = ""
	}

	override fun netlobbyOnExit(lobby:NetLobbyFrame) {
		gameManager?.also{
			if(it.engine.isNotEmpty()) it.engine[0].quitflag = true
		}
	}

	override fun netlobbyOnInit(lobby:NetLobbyFrame) {}

	override fun netlobbyOnLoginOK(lobby:NetLobbyFrame, client:NetPlayerClient) {}

	override fun netlobbyOnMessage(lobby:NetLobbyFrame, client:NetPlayerClient, message:Array<String>) {}

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
		internal val log = Logger.getLogger(StateNetGame::class.java)

		/** This state's ID */
		const val ID = 11
	}
}
