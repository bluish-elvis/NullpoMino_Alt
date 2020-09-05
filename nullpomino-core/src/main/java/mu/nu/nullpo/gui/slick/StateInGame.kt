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
import mu.nu.nullpo.game.event.EventReceiver.COLOR
import mu.nu.nullpo.game.play.GameManager
import mu.nu.nullpo.gui.GameKeyDummy
import mu.nu.nullpo.gui.slick.img.FontNormal
import mu.nu.nullpo.util.CustomProperties
import mu.nu.nullpo.util.GeneralUtil
import org.apache.log4j.Logger
import org.newdawn.slick.*
import org.newdawn.slick.state.BasicGameState
import org.newdawn.slick.state.StateBasedGame

/** ゲーム画面のステート */
class StateInGame:BasicGameState() {

	/** ゲームのメインクラス */
	var gameManager:GameManager? = null

	/** ポーズ flag */
	private var pause = false

	/** ポーズメッセージ非表示 */
	private var pauseMessageHide = false

	/** frame ステップ is enabled flag */
	private var enableframestep = false

	/** Show background flag */
	private var showbg = true

	/** 倍速Mode */
	private var fastforward = 0

	/** Pause menuのCursor position */
	private var cursor = 0

	/** Number of frames remaining until pause key can be used */
	private var pauseFrame = 0

	/** Screenshot撮影 flag */
	private var ssflag = false

	/** AppGameContainer (これを使ってタイトルバーを変える) */
	private var appContainer:AppGameContainer? = null

	/** Previous ingame flag (Used by title-bar text change) */
	private var prevInGameFlag = false

	/** Current game mode name */
	private var modeName:String = ""

	/** Fetch this state's ID */
	override fun getID():Int = ID

	/** State initialization */
	override fun init(container:GameContainer, game:StateBasedGame) {
		appContainer = container as AppGameContainer
	}

	/** Called when entering this state */
	override fun enter(container:GameContainer?, game:StateBasedGame?) {
		enableframestep = NullpoMinoSlick.propConfig.getProperty("option.enableframestep", false)
		showbg = NullpoMinoSlick.propConfig.getProperty("option.showbg", true)
		fastforward = 0
		cursor = 0
		prevInGameFlag = false

		container?.setClearEachFrame(!showbg) // Clear each frame when there is no BG
	}

	/** Start a new game
	 * @param strRulePath Rule file path (null if you want to use user-selected
	 * one)
	 */
	@JvmOverloads
	fun startNewGame(strRulePath:String? = null) {
		gameManager = GameManager(RendererSlick()).also {
			pause = false

			it.receiver.setGraphics(appContainer!!.graphics)

			modeName = NullpoMinoSlick.propGlobal.getProperty("name.mode", "")
			val modeObj = NullpoMinoSlick.modeManager.getMode(modeName)
			if(modeObj==null)
				log.error("Couldn't find mode:$modeName")
			else
				it.mode = modeObj

			it.init()

			// Initialization for each player
			for(i in 0 until it.players)it.engine[i].let{e->
				// チューニング設定
				e.owRotateButtonDefaultRight = NullpoMinoSlick.propGlobal.getProperty("$i.tuning.owRotateButtonDefaultRight", -1)
				e.owSkin = NullpoMinoSlick.propGlobal.getProperty("$i.tuning.owSkin", -1)
				e.owMinDAS = NullpoMinoSlick.propGlobal.getProperty("$i.tuning.owMinDAS", -1)
				e.owMaxDAS = NullpoMinoSlick.propGlobal.getProperty("$i.tuning.owMaxDAS", -1)
				e.owARR = NullpoMinoSlick.propGlobal.getProperty("$i.tuning.owDasDelay", -1)
				e.owSDSpd = NullpoMinoSlick.propGlobal.getProperty("$i.tuning.owSDSpd", -1)
				e.owReverseUpDown = NullpoMinoSlick.propGlobal.getProperty("$i.tuning.owReverseUpDown", false)
				e.owMoveDiagonal = NullpoMinoSlick.propGlobal.getProperty("$i.tuning.owMoveDiagonal", -1)
				e.owBlockOutlineType = NullpoMinoSlick.propGlobal.getProperty("$i.tuning.owBlockOutlineType", -1)
				e.owBlockShowOutlineOnly = NullpoMinoSlick.propGlobal.getProperty("$i.tuning.owBlockShowOutlineOnly", -1)

				// ルール
				val ruleopt:RuleOptions
				var rulename = strRulePath
				if(rulename==null) {
					rulename = NullpoMinoSlick.propGlobal.getProperty("$i.rule", "")
					if(it.mode!!.gameStyle>0)
						rulename = NullpoMinoSlick.propGlobal.getProperty("$i"+".rule."
							+it.mode!!.gameStyle, "")
				}
				if(rulename!=null&&rulename.isNotEmpty()) {
					log.info("Load rule options from $rulename")
					ruleopt = GeneralUtil.loadRule(rulename)
				} else {
					log.info("Load rule options from setting file")
					ruleopt = RuleOptions()
					ruleopt.readProperty(NullpoMinoSlick.propGlobal, i)
				}
				e.ruleopt = ruleopt

				// NEXT順生成アルゴリズム
				if(ruleopt.strRandomizer.isNotEmpty()) {
					e.randomizer = GeneralUtil.loadRandomizer(ruleopt.strRandomizer)
				}

				// Wallkick
				if(ruleopt.strWallkick.isNotEmpty()) {
					e.wallkick = GeneralUtil.loadWallkick(ruleopt.strWallkick)
				}

				// AI
				val aiName = NullpoMinoSlick.propGlobal.getProperty("$i.ai", "")
				if(aiName.isNotEmpty()) {
					e.ai = GeneralUtil.loadAIPlayer(aiName)
					e.aiMoveDelay = NullpoMinoSlick.propGlobal.getProperty("$i.aiMoveDelay", 0)
					e.aiThinkDelay = NullpoMinoSlick.propGlobal.getProperty("$i.aiThinkDelay", 0)
					e.aiUseThread = NullpoMinoSlick.propGlobal.getProperty("$i.aiUseThread", true)
					e.aiShowHint = NullpoMinoSlick.propGlobal.getProperty("$i.aiShowHint", false)
					e.aiPrethink = NullpoMinoSlick.propGlobal.getProperty("$i.aiPrethink", false)
					e.aiShowState = NullpoMinoSlick.propGlobal.getProperty("$i.aiShowState", false)
				}
				it.showInput = NullpoMinoSlick.propConfig.getProperty("option.showInput", false)

				// Called at initialization
				e.init()
			}
		}
		updateTitleBarCaption()
	}

	/** リプレイを読み込んで再生
	 * @param prop リプレイ dataの入ったプロパティセット
	 */
	fun startReplayGame(prop:CustomProperties) {
		gameManager = GameManager(RendererSlick()).also {
			it.replayMode = true
			it.replayProp = prop
			pause = false

			it.receiver.setGraphics(appContainer!!.graphics)

			// Mode
			modeName = prop.getProperty("name.mode", "")
			val modeObj = NullpoMinoSlick.modeManager.getMode(modeName)
			if(modeObj==null) log.error("Couldn't find mode:$modeName")
			else it.mode = modeObj

			it.init()

			// Initialization for each player
			for(i in 0 until it.players) {
				// ルール
				val ruleopt = RuleOptions()
				ruleopt.readProperty(prop, i)
				it.engine[i].ruleopt = ruleopt

				// NEXT順生成アルゴリズム
				if(ruleopt.strRandomizer.isNotEmpty()) it.engine[i].randomizer = GeneralUtil.loadRandomizer(ruleopt.strRandomizer)

				// Wallkick
				if(ruleopt.strWallkick.isNotEmpty()) it.engine[i].wallkick = GeneralUtil.loadWallkick(ruleopt.strWallkick)

				// AI (リプレイ追記用）
				val aiName = NullpoMinoSlick.propGlobal.getProperty("$i.ai", "")
				if(aiName.isNotEmpty()) {
					it.engine[i].ai = GeneralUtil.loadAIPlayer(aiName)
					it.engine[i].aiMoveDelay = NullpoMinoSlick.propGlobal.getProperty("$i.aiMoveDelay", 0)
					it.engine[i].aiThinkDelay = NullpoMinoSlick.propGlobal.getProperty("$i.aiThinkDelay", 0)
					it.engine[i].aiUseThread = NullpoMinoSlick.propGlobal.getProperty("$i.aiUseThread", true)
					it.engine[i].aiShowHint = NullpoMinoSlick.propGlobal.getProperty("$i.aiShowHint", false)
					it.engine[i].aiPrethink = NullpoMinoSlick.propGlobal.getProperty("$i.aiPrethink", false)
					it.engine[i].aiShowState = NullpoMinoSlick.propGlobal.getProperty("$i.aiShowState", false)
				}
				it.showInput = NullpoMinoSlick.propConfig.getProperty("option.showInput", false)

				// Called at initialization
				it.engine[i].init()
			}
		}
		updateTitleBarCaption()
	}

	/** Update title bar text */
	fun updateTitleBarCaption() {
		var strTitle = "NullpoMino_Alt - $modeName"
		gameManager?.let {
			if(it.engine.isNotEmpty())
				strTitle = when {
					pause&&!enableframestep -> "[PAUSE]"
					it.replayMode -> if(it.replayRerecord) "[RERECORD]" else "[REPLAY]"
					it.engine[0].isInGame&&!it.replayMode&&!it.replayRerecord -> "[PLAY]"
					else -> "[MENU]"
				}+strTitle
		}
		appContainer?.setTitle(strTitle)
	}

	/** 終了時の処理 */
	fun shutdown() {
		gameManager?.shutdown()
		gameManager = null
		ResourceHolder.bgmUnloadAll()
	}

	/** Called when leaving this state */
	override fun leave(container:GameContainer?, game:StateBasedGame?) {
		container?.setClearEachFrame(false)
		shutdown()
	}

	/** Draw the screen */
	override fun render(container:GameContainer, game:StateBasedGame, g:Graphics) {
		if(!container.hasFocus()) {
			if(!NullpoMinoSlick.alternateFPSTiming) NullpoMinoSlick.alternateFPSSleep(true)
			return
		}

		// ゲーム画面
		gameManager?.let {
			it.renderAll()

			if(it.engine.isNotEmpty()) {
				val offsetX = it.receiver.fieldX(it.engine[0], 0)
				val offsetY = it.receiver.fieldY(it.engine[0], 0)

				// Pause menu
				if(pause&&!enableframestep&&!pauseMessageHide) {
					FontNormal.printFont(offsetX+12, offsetY+188+cursor*16, "\u0082", COLOR.RAINBOW)

					FontNormal.printFont(offsetX+28, offsetY+188, "Continue", cursor==0)
					FontNormal.printFont(offsetX+28, offsetY+204, "Restart", cursor==1)
					FontNormal.printFont(offsetX+28, offsetY+220, "Exit", cursor==2)
					if(it.replayMode&&!it.replayRerecord)
						FontNormal.printFont(offsetX+28, offsetY+236, "RERECORD", cursor==3)
				}

				// Fast forward
				if(fastforward!=0)
					FontNormal.printFont(offsetX, offsetY+376, "e${fastforward+1}", COLOR.ORANGE)
				if(it.replayShowInvisible)
					FontNormal.printFont(offsetX, offsetY+392, "SHOW INVIS", COLOR.ORANGE)
			}
		}

		NullpoMinoSlick.drawFPS() // FPS
		NullpoMinoSlick.drawObserverClient() // Observer
		if(ssflag) {
			NullpoMinoSlick.saveScreenShot(container, g) // Screenshot
			ssflag = false
		}

		if(!NullpoMinoSlick.alternateFPSTiming) NullpoMinoSlick.alternateFPSSleep(true)
	}

	/** ゲーム stateを更新 */
	@Throws(SlickException::class)
	override fun update(container:GameContainer, game:StateBasedGame, delta:Int) {
		if(!container.hasFocus()) {
			GameKey.gamekey.forEach {it.clear()}
			if(NullpoMinoSlick.alternateFPSTiming) NullpoMinoSlick.alternateFPSSleep()
			return
		}

		// TTF font 描画
		ResourceHolder.ttfFont?.loadGlyphs()
		// Update key input states
		GameKey.gamekey.forEachIndexed {i, it ->
			it.update(container.input,
				(!pause||enableframestep)&&i<(gameManager?.engine?.size ?: 0)&&gameManager?.engine?.get(i)?.isInGame==true
			)
		}
		// Title bar update
		if(!gameManager?.engine.isNullOrEmpty()) {
			val nowInGame = gameManager?.engine?.get(0)?.isInGame==true
			if(prevInGameFlag!=nowInGame) {
				prevInGameFlag = nowInGame
				updateTitleBarCaption()
			}
		}

		// Pause
		if(GameKey.gamekey.any {it.isPushKey(GameKeyDummy.BUTTON_PAUSE)}) {
			if(!pause) {
				if(gameManager?.isGameActive==true&&pauseFrame<=0) {
					ResourceHolder.soundManager.play("pause")
					pause = true
					cursor = 0
				}
			} else {
				ResourceHolder.soundManager.play("pause")
				pause = false
				pauseFrame = 0
			}
			updateTitleBarCaption()
		} else if(pause&&!pauseMessageHide) {
			// Cursor movement
			if(GameKey.gamekey[0].isMenuRepeatKey(GameKeyDummy.BUTTON_UP)) {
				cursor--

				if(cursor<0)
					cursor = if(gameManager?.replayMode==true&&gameManager?.replayRerecord==false)
						3 else 2

				ResourceHolder.soundManager.play("cursor")
			}
			if(GameKey.gamekey[0].isMenuRepeatKey(GameKeyDummy.BUTTON_DOWN)) {
				cursor++
				if(cursor>3) cursor = 0

				if((gameManager?.replayMode==false||gameManager?.replayRerecord==true)&&cursor>2) cursor = 0

				ResourceHolder.soundManager.play("cursor")
			}

			// Confirm
			if(GameKey.gamekey[0].isPushKey(GameKeyDummy.BUTTON_A)) {
				ResourceHolder.soundManager.play("decide0")
				when(cursor) {
					0 // Continue
					-> {
						pause = false
						pauseFrame = 0
						GameKey.gamekey[0].clear()
						ResourceHolder.bgmResume()
					}
					1 // Retry
					-> {
						ResourceHolder.bgmStop()
						pause = false
						gameManager?.reset()
					}
					2 // End
					-> {
						ResourceHolder.bgmStop()
						game.enterState(StateTitle.ID)
						return
					}
					3 // Replay re-record
					-> {
						gameManager?.replayRerecord = true
						ResourceHolder.soundManager.play("twist")
						cursor = 0
					}
				}
				updateTitleBarCaption()
			} else if(GameKey.gamekey[0].isPushKey(GameKeyDummy.BUTTON_B)&&pauseFrame<=0) {
				ResourceHolder.soundManager.play("pause")
				pause = false
				pauseFrame = 5
				GameKey.gamekey[0].clear()
				ResourceHolder.bgmResume()
				updateTitleBarCaption()
			}// Unpause by cancel key
		}// Pause menu
		if(pauseFrame>0) pauseFrame--

		// Hide pause menu
		pauseMessageHide = GameKey.gamekey[0].isPressKey(GameKeyDummy.BUTTON_C)


		gameManager?.let {
			if(it.replayMode&&!it.replayRerecord&&it.engine[0].gameActive) {
				// Replay speed
				if(GameKey.gamekey[0].isMenuRepeatKey(GameKeyDummy.BUTTON_LEFT)) if(fastforward>0) fastforward--
				if(GameKey.gamekey[0].isMenuRepeatKey(GameKeyDummy.BUTTON_RIGHT)) if(fastforward<98) fastforward++

				// Replay re-record
				if(GameKey.gamekey[0].isPushKey(GameKeyDummy.BUTTON_D)) {
					it.replayRerecord = true
					ResourceHolder.soundManager.play("twist")
					cursor = 0
				}
				// Show invisible blocks during replays
				if(GameKey.gamekey[0].isPushKey(GameKeyDummy.BUTTON_E)) {
					it.replayShowInvisible = !it.replayShowInvisible
					ResourceHolder.soundManager.play("twist")
					cursor = 0
				}
			} else fastforward = 0

			// BGM
			if(ResourceHolder.bgmPlaying!=it.bgmStatus.bgm&&!it.bgmStatus.fadesw)
				ResourceHolder.bgmStart(it.bgmStatus.bgm)
			if(ResourceHolder.bgmIsPlaying()) {
				val basevolume = NullpoMinoSlick.propConfig.getProperty("option.bgmvolume", 128)
				val newvolume = maxOf(0f, minOf(it.bgmStatus.volume*basevolume/128f, 1f))
				container.musicVolume = newvolume
				if(newvolume<=0f) ResourceHolder.bgmStop()
			}

			// ゲームの処理を実行
			if(!pause||GameKey.gamekey[0].isPushKey(GameKeyDummy.BUTTON_FRAMESTEP)&&enableframestep) {
				for(i in 0 until minOf(it.players, GameKey.gamekey.size))
					if(!it.replayMode||it.replayRerecord||!it.engine[i].gameActive)
						GameKey.gamekey[i].inputStatusUpdate(it.engine[i].ctrl)

				for(i in 0..fastforward) gameManager?.updateAll()
			}
			// Retry button
			if(GameKey.gamekey.any {it.isPushKey(GameKeyDummy.BUTTON_RETRY)}) {
				ResourceHolder.bgmStop()
				pause = false
				it.reset()
			}

			// Return to title
			if(it.quitFlag||GameKey.gamekey.any {it.isPushKey(GameKeyDummy.BUTTON_GIVEUP)}) {
				ResourceHolder.bgmStop()
				game.enterState(StateTitle.ID)
				return
			}
		}

		// Screenshot button
		if(GameKey.gamekey.any {it.isPushKey(GameKeyDummy.BUTTON_SCREENSHOT)})
			ssflag = true

		// Exit button
		if(GameKey.gamekey.any {it.isPushKey(GameKeyDummy.BUTTON_QUIT)}) {
			shutdown()
			container.exit()
		}

		if(NullpoMinoSlick.alternateFPSTiming) NullpoMinoSlick.alternateFPSSleep(true)
	}

	companion object {
		/** This state's ID */
		const val ID = 2

		/** Log */
		internal val log = Logger.getLogger(StateInGame::class.java)
	}
}
