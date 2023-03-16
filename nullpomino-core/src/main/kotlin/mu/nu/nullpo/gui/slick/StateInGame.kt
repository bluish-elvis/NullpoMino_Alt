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
package mu.nu.nullpo.gui.slick

import mu.nu.nullpo.game.component.RuleOptions
import mu.nu.nullpo.game.event.EventReceiver.COLOR
import mu.nu.nullpo.game.play.GameManager
import mu.nu.nullpo.game.play.GameStyle
import mu.nu.nullpo.gui.common.BaseFont
import mu.nu.nullpo.gui.common.GameKeyDummy
import mu.nu.nullpo.gui.slick.img.FontNormal
import mu.nu.nullpo.util.CustomProperties
import mu.nu.nullpo.util.GeneralUtil
import org.apache.logging.log4j.LogManager
import org.newdawn.slick.AppGameContainer
import org.newdawn.slick.GameContainer
import org.newdawn.slick.Graphics
import org.newdawn.slick.SlickException
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
	private var enableFrameStep = false

	/** Show background flag */
	var showBg = true; private set

	/** 倍速Mode */
	private var fastForward = 0

	/** Pause menuのCursor position */
	private var cursor = 0

	/** Number of frames remaining until pause key can be used */
	private var pauseFrame = 0

	/** Screenshot撮影 flag */
	private var ssFlag = false

	/** AppGameContainer (これを使ってタイトルバーを変える) */
	private var appContainer:AppGameContainer? = null

	/** Previous ingame flag (Used by title-bar text change) */
	private var prevInGameFlag = false

	/** Current game mode name */
	private var modeName = ""

	/** Fetch this state's ID */
	override fun getID():Int = ID

	/** State initialization */
	override fun init(container:GameContainer, game:StateBasedGame) {
		appContainer = container as AppGameContainer
	}

	/** Called when entering this state */
	override fun enter(container:GameContainer?, game:StateBasedGame?) {
		enableFrameStep = NullpoMinoSlick.propConfig.getProperty("option.enableFrameStep", false)
		showBg = NullpoMinoSlick.propConfig.getProperty("option.showBg", true)
		fastForward = 0
		cursor = 0
		prevInGameFlag = false

		container?.setClearEachFrame(!showBg) // Clear each frame when there is no BG
	}

	/** Start a new game
	 * @param strRulePath Rule file path (null if you want to use user-selected
	 * one)
	 */
	@JvmOverloads
	fun startNewGame(strRulePath:String? = null) {
		modeName = NullpoMinoSlick.propGlobal.getProperty("name.mode", "")
		val modeObj = NullpoMinoSlick.modeManager[modeName]
		if(modeObj==null) log.error("Couldn't find mode:$modeName")
		gameManager = GameManager(RendererSlick(appContainer!!.graphics), modeObj).also {
			pause = false

			// Initialization for each player
			for(i in 0 until it.players) it.engine[i].let {e ->
				// チューニング設定
				e.owSpinDirection = NullpoMinoSlick.propGlobal.getProperty("$i.tuning.owRotateButtonDefaultRight", -1)
				e.owSkin = NullpoMinoSlick.propGlobal.getProperty("$i.tuning.owSkin", -1)
				e.owMinDAS = NullpoMinoSlick.propGlobal.getProperty("$i.tuning.owMinDAS", -1)
				e.owMaxDAS = NullpoMinoSlick.propGlobal.getProperty("$i.tuning.owMaxDAS", -1)
				e.owARR = NullpoMinoSlick.propGlobal.getProperty("$i.tuning.owDasDelay", -1)
				e.owSDSpd = NullpoMinoSlick.propGlobal.getProperty("$i.tuning.owSDSpd", -1)
				e.owReverseUpDown = NullpoMinoSlick.propGlobal.getProperty("$i.tuning.owReverseUpDown", false)
				e.owMoveDiagonal = NullpoMinoSlick.propGlobal.getProperty("$i.tuning.owMoveDiagonal", -1)
				e.owBlockOutlineType = NullpoMinoSlick.propGlobal.getProperty("$i.tuning.owBlockOutlineType", -1)
				e.owBlockShowOutlineOnly = NullpoMinoSlick.propGlobal.getProperty("$i.tuning.owBlockShowOutlineOnly", -1)
				e.owDelayCancel = NullpoMinoSlick.propGlobal.getProperty("$i.tuning.owDelayCancel", -1)

				// ルール
				val ruleName = strRulePath ?: NullpoMinoSlick.propGlobal.getProperty(
					if(it.mode?.gameStyle!=null&&it.mode?.gameStyle!=GameStyle.TETROMINO) "$i.rule.${it.mode!!.gameStyle.ordinal}"
					else "$i.rule", ""
				)

				val ruleOpt:RuleOptions = if(!ruleName.isNullOrEmpty()) {
					log.info("Load rule options from $ruleName")
					GeneralUtil.loadRule(ruleName)
				} else {
					log.info("Load rule options from setting file")
					RuleOptions().apply {
						readProperty(NullpoMinoSlick.propGlobal, i)
					}
				}
				e.ruleOpt = ruleOpt

				// NEXT順生成アルゴリズム
				if(ruleOpt.strRandomizer.isNotEmpty()) e.randomizer = GeneralUtil.loadRandomizer(ruleOpt.strRandomizer)
				// Wallkick
				if(ruleOpt.strWallkick.isNotEmpty()) e.wallkick = GeneralUtil.loadWallkick(ruleOpt.strWallkick)

				// AI
				val aiName = NullpoMinoSlick.propGlobal.getProperty("$i.ai", "")
				if(aiName.isNotEmpty()) {
					e.ai = GeneralUtil.loadAIPlayer(aiName)
					e.aiMoveDelay = NullpoMinoSlick.propGlobal.getProperty("$i.aiMoveDelay", 0)
					e.aiThinkDelay = NullpoMinoSlick.propGlobal.getProperty("$i.aiThinkDelay", 0)
					e.aiUseThread = NullpoMinoSlick.propGlobal.getProperty("$i.aiUseThread", true)
					e.aiShowHint = NullpoMinoSlick.propGlobal.getProperty("$i.aiShowHint", false)
					e.aiPreThink = NullpoMinoSlick.propGlobal.getProperty("$i.aiPreThink", false)
					e.aiShowState = NullpoMinoSlick.propGlobal.getProperty("$i.aiShowState", false)
				}
				it.showInput = NullpoMinoSlick.propConfig.getProperty("option.showInput", false)

				// Called at initialization
				e.init()
			}
		}
		updateTitleBarCaption()
	}

	/** リプレイ[prop]を読み込んで再生 */
	fun startReplayGame(prop:CustomProperties) {
		// Mode
		modeName = prop.getProperty("name.mode", "")
		val modeObj = NullpoMinoSlick.modeManager[modeName]
		if(modeObj==null) log.error("Couldn't find mode:$modeName")

		gameManager = GameManager(RendererSlick(appContainer!!.graphics), modeObj).also {
			it.replayMode = true
			it.replayProp = prop
			pause = false

			// Initialization for each player
			for(i in 0 until it.players) {
				// ルール
				val ruleOpt = RuleOptions()
				ruleOpt.readProperty(prop, i)
				it.engine[i].ruleOpt = ruleOpt

				// NEXT順生成アルゴリズム
				if(ruleOpt.strRandomizer.isNotEmpty()) it.engine[i].randomizer = GeneralUtil.loadRandomizer(ruleOpt.strRandomizer)

				// Wallkick
				if(ruleOpt.strWallkick.isNotEmpty()) it.engine[i].wallkick = GeneralUtil.loadWallkick(ruleOpt.strWallkick)

				// AI (リプレイ追記用）
				val aiName = NullpoMinoSlick.propGlobal.getProperty("$i.ai", "")
				if(aiName.isNotEmpty()) {
					it.engine[i].ai = GeneralUtil.loadAIPlayer(aiName)
					it.engine[i].aiMoveDelay = NullpoMinoSlick.propGlobal.getProperty("$i.aiMoveDelay", 0)
					it.engine[i].aiThinkDelay = NullpoMinoSlick.propGlobal.getProperty("$i.aiThinkDelay", 0)
					it.engine[i].aiUseThread = NullpoMinoSlick.propGlobal.getProperty("$i.aiUseThread", true)
					it.engine[i].aiShowHint = NullpoMinoSlick.propGlobal.getProperty("$i.aiShowHint", false)
					it.engine[i].aiPreThink = NullpoMinoSlick.propGlobal.getProperty("$i.aiPreThink", false)
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
					pause&&!enableFrameStep -> "[PAUSE]"
					it.replayMode -> if(it.replayRerecord) "[RERECORD]" else "[REPLAY]"
					it.engine[0].isInGame&&!it.replayMode&&!it.replayRerecord -> "[PLAY]"
					else -> "[Menu]"
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
				val offsetX = it.receiver.fieldX(it.engine[0])
				val offsetY = it.receiver.fieldY(it.engine[0])

				// Pause menu
				if(pause&&!enableFrameStep&&!pauseMessageHide) {
					FontNormal.printFont(offsetX+12, offsetY+188+cursor*16, BaseFont.CURSOR, COLOR.RAINBOW)

					FontNormal.printFont(offsetX+28, offsetY+188, "Continue", cursor==0)
					FontNormal.printFont(offsetX+28, offsetY+204, "Restart", cursor==1)
					FontNormal.printFont(offsetX+28, offsetY+220, "Exit", cursor==2)
					if(it.replayMode&&!it.replayRerecord)
						FontNormal.printFont(offsetX+28, offsetY+236, "RERECORD", cursor==3)
				}

				// Fast-forward
				if(fastForward!=0)
					FontNormal.printFont(offsetX, offsetY+376, "e${fastForward+1}", COLOR.ORANGE)
				if(it.replayShowInvisible)
					FontNormal.printFont(offsetX, offsetY+392, "SHOW INVIS", COLOR.ORANGE)
			}
		}

		NullpoMinoSlick.drawFPS() // FPS
		NullpoMinoSlick.drawObserverClient() // Observer
		if(ssFlag) {
			NullpoMinoSlick.saveScreenShot(container, g) // Screenshot
			ssFlag = false
		}

		if(!NullpoMinoSlick.alternateFPSTiming) NullpoMinoSlick.alternateFPSSleep(true)
	}

	/** ゲーム stateを更新 */
	@Throws(SlickException::class)
	override fun update(container:GameContainer, game:StateBasedGame, delta:Int) {
		if(!container.hasFocus()) {
			GameKey.gameKey.forEach {it.clear()}
			if(NullpoMinoSlick.alternateFPSTiming) NullpoMinoSlick.alternateFPSSleep()
			return
		}

		// TTF font 描画
		ResourceHolder.ttfFont?.loadGlyphs()
		// Update key input states
		GameKey.gameKey.forEachIndexed {i, it ->
			it.update(
				container.input,
				(!pause||enableFrameStep)&&i<(gameManager?.engine?.size ?: 0)&&gameManager?.engine?.get(i)?.isInGame==true
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
		if(GameKey.gameKey.any {it.isPushKey(GameKeyDummy.BUTTON_PAUSE)}) {
			if(!pause) {
				if(gameManager?.isGameActive==true&&pauseFrame<=0) {
					ResourceHolder.soundManager.play("pause")
					if(!ResourceHolder.bgmIsLooping) ResourceHolder.bgmPause()
					pause = true
					cursor = 0
				}
			} else {// Unpause by pause key
				ResourceHolder.soundManager.play("pause")
				ResourceHolder.bgmResume()
				pause = false
				pauseFrame = 0
			}
			updateTitleBarCaption()
		} else if(pause&&!pauseMessageHide) {
			// Cursor movement
			if(GameKey.gameKey[0].isMenuRepeatKey(GameKeyDummy.BUTTON_UP)) {
				cursor--
				if(cursor<0)
					cursor = if(gameManager?.replayMode==true&&gameManager?.replayRerecord==false)
						3 else 2

				ResourceHolder.soundManager.play("cursor")
			}
			if(GameKey.gameKey[0].isMenuRepeatKey(GameKeyDummy.BUTTON_DOWN)) {
				cursor++
				if(cursor>3) cursor = 0

				if((gameManager?.replayMode==false||gameManager?.replayRerecord==true)&&cursor>2) cursor = 0

				ResourceHolder.soundManager.play("cursor")
			}

			// Confirm
			if(GameKey.gameKey[0].isPushKey(GameKeyDummy.BUTTON_A)) {
				ResourceHolder.soundManager.play("decide0")
				when(cursor) {
					0 -> { // Continue
						pause = false
						pauseFrame = 0
						GameKey.gameKey[0].clear()
						ResourceHolder.bgmResume()
					}
					1 -> { // Retry
						ResourceHolder.bgmStop()
						pause = false
						gameManager?.reset()
					}
					2 -> { // End
						ResourceHolder.bgmStop()
						ResourceHolder.soundManager.stop("danger")
						gameManager?.reset()
						game.enterState(StateTitle.ID)
						return
					}
					3 -> { // Replay re-record
						gameManager?.replayRerecord = true
						ResourceHolder.soundManager.play("twist")
						cursor = 0
					}
				}
				updateTitleBarCaption()
			} else if(GameKey.gameKey[0].isPushKey(GameKeyDummy.BUTTON_B)&&pauseFrame<=0) {
				ResourceHolder.soundManager.play("pause")
				// Unpause by cancel key
				pause = false
				pauseFrame = 5
				GameKey.gameKey[0].clear()
				ResourceHolder.bgmResume()
				updateTitleBarCaption()
			}
		}// Pause menu
		if(pauseFrame>0) pauseFrame--

		// Hide pause menu
		pauseMessageHide = GameKey.gameKey[0].isPressKey(GameKeyDummy.BUTTON_C)


		gameManager?.let {m ->
			if(m.replayMode&&!m.replayRerecord&&m.engine[0].gameActive) {
				// Replay speed
				if(GameKey.gameKey[0].isMenuRepeatKey(GameKeyDummy.BUTTON_LEFT)) if(fastForward>0) fastForward--
				if(GameKey.gameKey[0].isMenuRepeatKey(GameKeyDummy.BUTTON_RIGHT)) if(fastForward<98) fastForward++

				// Replay re-record
				if(GameKey.gameKey[0].isPushKey(GameKeyDummy.BUTTON_D)) {
					m.replayRerecord = true
					ResourceHolder.soundManager.play("twist")
					cursor = 0
				}
				// Show invisible blocks during replays
				if(GameKey.gameKey[0].isPushKey(GameKeyDummy.BUTTON_E)) {
					m.replayShowInvisible = !m.replayShowInvisible
					ResourceHolder.soundManager.play("twist")
					cursor = 0
				}
			} else fastForward = 0

			// BGM
			if(ResourceHolder.bgmPlaying!=m.musMan.bgm&&!m.musMan.fadeSW)
				ResourceHolder.bgmStart(m.musMan.bgm)
			if(ResourceHolder.bgmIsPlaying) {
				val baseVolume = NullpoMinoSlick.propConfig.getProperty("option.bgmVolume", 128)
				val newVolume = maxOf(0f, minOf(m.musMan.volume*baseVolume/128f, 1f))
				container.musicVolume = newVolume
				if(newVolume<=0f) ResourceHolder.bgmStop()
			}

			// ゲームの処理を実行
			if(!pause||GameKey.gameKey[0].isPushKey(GameKeyDummy.BUTTON_FRAMESTEP)&&enableFrameStep) {
				for(i in 0 until minOf(m.players, GameKey.gameKey.size))
					if(!m.engine[i].gameActive||((m.engine[i].ai==null||m.engine[i].aiShowHint)&&(!m.replayMode||m.replayRerecord)))
						GameKey.gameKey[i].inputStatusUpdate(m.engine[i].ctrl)

				for(i in 0..fastForward) gameManager?.updateAll()
			}
			// Retry button
			if(GameKey.gameKey.any {it.isPushKey(GameKeyDummy.BUTTON_RETRY)}) {
				ResourceHolder.bgmStop()
				pause = false
				m.reset()
			}

			// Return to title
			if(m.quitFlag||GameKey.gameKey.any {it.isPushKey(GameKeyDummy.BUTTON_GIVEUP)}) {
				ResourceHolder.bgmStop()
				game.enterState(StateTitle.ID)
				return@update
			}
		}

		// Screenshot button
		if(GameKey.gameKey.any {it.isPushKey(GameKeyDummy.BUTTON_SCREENSHOT)})
			ssFlag = true

		// Exit button
		if(GameKey.gameKey.any {it.isPushKey(GameKeyDummy.BUTTON_QUIT)}) {
			shutdown()
			container.exit()
		}

		if(NullpoMinoSlick.alternateFPSTiming) NullpoMinoSlick.alternateFPSSleep(true)
	}

	companion object {
		/** This state's ID */
		const val ID = 2

		/** Log */
		internal val log = LogManager.getLogger()
	}
}
