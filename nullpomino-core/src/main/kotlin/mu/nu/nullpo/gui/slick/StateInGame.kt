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
package mu.nu.nullpo.gui.slick

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import mu.nu.nullpo.util.GeneralUtil.Json
import mu.nu.nullpo.game.component.RuleOptions
import mu.nu.nullpo.game.event.EventReceiver.COLOR
import mu.nu.nullpo.game.play.GameManager
import mu.nu.nullpo.gui.common.BaseFont
import mu.nu.nullpo.gui.common.ConfigGlobal.AIConf
import mu.nu.nullpo.gui.common.GameKeyDummy
import mu.nu.nullpo.gui.slick.img.FontNormal
import mu.nu.nullpo.util.CustomProperties
import org.apache.logging.log4j.LogManager
import org.newdawn.slick.AppGameContainer
import org.newdawn.slick.GameContainer
import org.newdawn.slick.Graphics
import org.newdawn.slick.SlickException
import org.newdawn.slick.state.BasicGameState
import org.newdawn.slick.state.StateBasedGame
import mu.nu.nullpo.gui.slick.NullpoMinoSlick.Companion.propConfig as pCo
import mu.nu.nullpo.gui.slick.NullpoMinoSlick.Companion.propGlobal as pGl
import mu.nu.nullpo.gui.slick.ResourceHolder as Res
import mu.nu.nullpo.util.GeneralUtil as Util

/** ゲーム画面のステート */
internal class StateInGame:BasicGameState() {
	/** ゲームのメインクラス */
	private var gameManager:GameManager? = null

	/** ポーズ flag */
	private var pause = false

	/** ポーズメッセージ非表示 */
	private var pauseMessageHide = false

	/** frame ステップ is enabled flag */
	private var enableFrameStep = false

	/** Show background flag */
	private var showBG = true

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

	/** Previous in-game flag (Used by title-bar text change) */
	private var prevInGameFlag = false

	/** Current game mode name */
	private var modeName = ""

	/** Fetch this state's ID */
	override fun getID():Int = ID

	/** State initialization */
	override fun init(c:GameContainer, game:StateBasedGame) {
		appContainer = c as AppGameContainer
	}

	/** Called when entering this state */
	override fun enter(c:GameContainer?, game:StateBasedGame?) {
		enableFrameStep = pCo.general.enableFrameStep
		showBG = pCo.visual.showBG
		fastForward = 0
		cursor = 0
		prevInGameFlag = false

		c?.setClearEachFrame(!showBG) // Clear each frame when there is no BG
	}

	/** Start a new game
	 * @param strRulePath Rule file path (null if you want to use user-selected one)
	 */
	@JvmOverloads
	fun startNewGame(mode:String = pGl.lastMode[""] ?: "", strRulePath:String? = null) {
		modeName = mode
		val modeObj = NullpoMinoSlick.modeManager[mode]
		if(modeObj==null) log.error("Couldn't find mode:$mode")
		gameManager = GameManager(RendererSlick(appContainer!!.graphics), modeObj).also {
			pause = false

			// Initialization for each player
			it.engine.forEachIndexed {i, e ->
				// チューニング設定
				try {
					pGl.tuning.getOrNull(i)
				} catch(ex:Exception) {
					log.warn(ex)
					null
				}?.also {t -> e.owTune = t.copy()}

				// ルール
				val ruleName =
					strRulePath ?: pGl.rule.getOrNull(i)?.getOrNull(it.mode?.gameStyle?.ordinal ?: 0)?.path
				val ruleOpt = Util.loadRule(ruleName)

				e.ruleOpt = ruleOpt

				// NEXT順生成アルゴリズム
				if(ruleOpt.strRandomizer.isNotEmpty()) e.randomizer = Util.loadRandomizer(ruleOpt.strRandomizer)
				// Wallkick
				if(ruleOpt.strWallkick.isNotEmpty()) e.wallkick = Util.loadWallkick(ruleOpt.strWallkick)

				// AI
				pGl.ai.getOrElse(i) {AIConf()}.let {ai ->
					if(ai.name.isNotEmpty()) {
						e.ai = Util.loadAIPlayer(ai.name)
						e.aiConf = ai
					}
				}
				it.showInput = pCo.general.showInput

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
			it.engine.forEachIndexed {i, e ->
				// ルール
				val ruleOpt = try{
					Json.decodeFromString(prop.getProperty("$i.rule"))
				}catch (_:Exception){
					RuleOptions().apply {readProperty(prop, i)}
				}
				e.ruleOpt = ruleOpt
				// NEXT順生成アルゴリズム
				if(ruleOpt.strRandomizer.isNotEmpty()) e.randomizer = Util.loadRandomizer(ruleOpt.strRandomizer)

				// Wallkick
				if(ruleOpt.strWallkick.isNotEmpty()) e.wallkick = Util.loadWallkick(ruleOpt.strWallkick)

				// AI (リプレイ追記用）
				pGl.ai.getOrElse(i) {AIConf()}.let {ai ->
					if(ai.name.isNotEmpty()) {
						e.ai = Util.loadAIPlayer(ai.name)
						e.aiConf = ai
					}
				}
				it.showInput = pCo.general.showInput
				// Called at initialization
				e.init()
			}
		}
		updateTitleBarCaption()
	}

	/** Update title bar text */
	private fun updateTitleBarCaption() {
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
		Res.bgmUnloadAll()
	}

	/** Called when leaving this state */
	override fun leave(c:GameContainer?, game:StateBasedGame?) {
		c?.setClearEachFrame(false)
		shutdown()
	}

	/** Draw the screen */
	override fun render(c:GameContainer, game:StateBasedGame, g:Graphics) {
		if(!c.hasFocus()) {
			if(!NullpoMinoSlick.alternateFPSTiming) NullpoMinoSlick.alternateFPSSleep(true)
			return
		}

		// ゲーム画面
		gameManager?.let {
			it.renderAll()

			if(it.engine.isNotEmpty()) {
				val offsetX = it.receiver.fieldX(it.engine[0]).toInt()
				val offsetY = it.receiver.fieldY(it.engine[0]).toInt()

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
			NullpoMinoSlick.saveScreenShot(c, g) // Screenshot
			ssFlag = false
		}

		if(!NullpoMinoSlick.alternateFPSTiming) NullpoMinoSlick.alternateFPSSleep(true)
	}

	/** ゲーム stateを更新 */
	@Throws(SlickException::class)
	override fun update(c:GameContainer, game:StateBasedGame, delta:Int) {
		if(!c.hasFocus()) {
			GameKey.gameKey.forEach {it.clear()}
			if(NullpoMinoSlick.alternateFPSTiming) NullpoMinoSlick.alternateFPSSleep()
			return
		}

		// TTF font 描画
		Res.ttfFont?.loadGlyphs()
		// Update key input states
		GameKey.gameKey.forEachIndexed {i, it ->
			it.update(
				c.input,
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
					Res.soundManager.play("pause")
					if(!Res.bgmIsLooping) Res.bgmPause()
					pause = true
					cursor = 0
				}
			} else {// Unpause by pause key
				Res.soundManager.play("pause")
				Res.bgmResume()
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

				Res.soundManager.play("cursor")
			}
			if(GameKey.gameKey[0].isMenuRepeatKey(GameKeyDummy.BUTTON_DOWN)) {
				cursor++
				if(cursor>3) cursor = 0

				if((gameManager?.replayMode==false||gameManager?.replayRerecord==true)&&cursor>2) cursor = 0

				Res.soundManager.play("cursor")
			}

			// Confirm
			if(GameKey.gameKey[0].isPushKey(GameKeyDummy.BUTTON_A)) {
				Res.soundManager.play("decide0")
				when(cursor) {
					0 -> { // Continue
						pause = false
						pauseFrame = 0
						GameKey.gameKey[0].clear()
						Res.bgmResume()
					}
					1 -> { // Retry
						Res.bgmStop()
						pause = false
						gameManager?.reset()
					}
					2 -> { // End
						Res.bgmStop()
						Res.soundManager.stop("danger")
						gameManager?.reset()
						game.enterState(StateTitle.ID)
						return
					}
					3 -> { // Replay re-record
						gameManager?.replayRerecord = true
						Res.soundManager.play("twist")
						cursor = 0
					}
				}
				updateTitleBarCaption()
			} else if(GameKey.gameKey[0].isPushKey(GameKeyDummy.BUTTON_B)&&pauseFrame<=0) {
				Res.soundManager.play("pause")
				// Unpause by cancel key
				pause = false
				pauseFrame = 5
				GameKey.gameKey[0].clear()
				Res.bgmResume()
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
					Res.soundManager.play("twist")
					cursor = 0
				}
				// Show invisible blocks during replays
				if(GameKey.gameKey[0].isPushKey(GameKeyDummy.BUTTON_E)) {
					m.replayShowInvisible = !m.replayShowInvisible
					Res.soundManager.play("twist")
					cursor = 0
				}
			} else fastForward = 0

			// BGM
			if(Res.bgmPlaying!=m.musMan.bgm&&!m.musMan.fadeSW)
				Res.bgmStart(m.musMan.bgm)
			if(Res.bgmIsPlaying) c.musicVolume =
				maxOf(0f, minOf(m.musMan.volume*pCo.audio.bgmVolume/128f, 1f)).also {
					if(it<=0f) Res.bgmStop()
				}

			// ゲームの処理を実行
			if(!pause||GameKey.gameKey[0].isPushKey(GameKeyDummy.BUTTON_FRAMESTEP)&&enableFrameStep) {
				for(i in 0..<minOf(m.players, GameKey.gameKey.size))
					if(!m.engine[i].gameActive||((m.engine[i].ai==null||m.engine[i].aiShowHint)&&(!m.replayMode||m.replayRerecord)))
						GameKey.gameKey[i].inputStatusUpdate(m.engine[i].ctrl)

				for(i in 0..fastForward) gameManager?.updateAll()
			}
			// Retry button
			if(GameKey.gameKey.any {it.isPushKey(GameKeyDummy.BUTTON_RETRY)}) {
				Res.bgmStop()
				pause = false
				m.reset()
			}

			// Return to title
			if(m.quitFlag||GameKey.gameKey.any {it.isPushKey(GameKeyDummy.BUTTON_GIVEUP)}) {
				Res.bgmStop()
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
			c.exit()
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
