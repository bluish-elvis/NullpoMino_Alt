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

import kotlinx.serialization.encodeToString
import mu.nu.nullpo.util.GeneralUtil.Json
import mu.nu.nullpo.game.play.GameManager
import mu.nu.nullpo.gui.common.ConfigGlobal
import mu.nu.nullpo.gui.common.GameKeyDummy.Companion.MAX_PLAYERS
import org.apache.logging.log4j.LogManager
import org.newdawn.slick.AppGameContainer
import org.newdawn.slick.GameContainer
import org.newdawn.slick.Graphics
import org.newdawn.slick.Image
import org.newdawn.slick.Sound
import org.newdawn.slick.state.BasicGameState
import org.newdawn.slick.state.StateBasedGame
import mu.nu.nullpo.gui.slick.NullpoMinoSlick.Companion.propConfig as pCo
import mu.nu.nullpo.gui.slick.NullpoMinoSlick.Companion.propGlobal as pGl

/** ロード画面のステート */
internal class StateLoading:BasicGameState() {
	/** プリロード進行度 */
	private var preloadSet = -2

	private var loadBG = Image(640, 480)

	private val skinDir = pGl.custom.skinDir
	/* Fetch this state's ID */
	override fun getID():Int = ID

	/* State initialization */
	override fun init(container:GameContainer, game:StateBasedGame) {}

	/* Called when entering this state */
	override fun enter(container:GameContainer?, game:StateBasedGame?) {
		//  input 関連をInitialization
		GameKey.initGlobalGameKey()
		GameKey.gameKey.forEachIndexed {i, t -> pCo.ctrl.keymaps.getOrNull(i)?.let {t.loadConfig(it)}}

		// 設定を反映させる
		NullpoMinoSlick.setGeneralConfig()

		if(pCo.audio.se)
			try {
				val clip = Sound("$skinDir/jingle/welcome.ogg")
				clip.play()
				loadBG = Image("$skinDir/graphics/${ResourceHolder.imgTitleBG.name}.png")
				container?.graphics?.drawImage(loadBG, 0f, 0f)
			} catch(_:Throwable) {
			}
	}

	/* Draw the screen */
	override fun render(container:GameContainer, game:StateBasedGame, g:Graphics) {
		g.drawImage(loadBG, 0f, 0f)
		//	if(preloadSet<=-2) preloadSet = -1
	}

	/* Update game */
	override fun update(c:GameContainer, game:StateBasedGame, delta:Int) {
		when {
			preloadSet==-2 -> preloadSet = -1
			preloadSet==-1 -> // 画像などを読み込み
				preloadSet = try {
					ResourceHolder.load()
					0
				} catch(e:Throwable) {
					log.error("Resource load failed", e)
					-3
				}
			preloadSet in 0..3 -> cacheImg()
			preloadSet>3 -> {
				// Change title bar caption
				if(c is AppGameContainer) {
					c.setTitle("NullpoMino_Alt version${GameManager.versionString}")
					c.setUpdateOnlyWhenVisible(true)
				}

				// First run
				if(!pCo.didFirstRun) {
					// Set various default settings here
					GameKey.gameKey[0].loadDefaultKeymap()
					if(pCo.ctrl.keymaps.isEmpty()) pCo.ctrl.keymaps += List(MAX_PLAYERS) {ConfigGlobal.GameKeyMaps()}
					GameKey.gameKey.forEachIndexed {i, it -> pCo.ctrl.keymaps[i] = it.map}
					pCo.didFirstRun = true

					// Set default rotation button setting (only for first run)
					if(!pGl.didFirstRun) {
						if(pGl.tuning.isEmpty()) pGl.tuning += List(MAX_PLAYERS) {ConfigGlobal.TuneConf()}
						pGl.tuning.forEach {it.spinDir = 0}
						pGl.didFirstRun = true
					}

					// Save settings
					NullpoMinoSlick.saveConfig()
				}
				GameKey.gameKey.forEach {log.debug(Json.encodeToString(it.map))}
				log.debug(Json.encodeToString(ControllerManager.config))
				// Go to title screen
				game.enterState(StateTitle.ID)
			}
		}
	}

	private fun cacheImg() {
		// 巨大な画像をあらかじめ画面に描画することでメモリにキャッシュさせる
		when(preloadSet) {
			0 -> {
				for(i in 0..<ResourceHolder.blockBreakMax)
					try {
						ResourceHolder.imgBreak[i][0].draw()
					} catch(_:Exception) {
					}

				preloadSet++
			}
			1 -> {
				for(i in 0..<ResourceHolder.blockBreakMax)
					try {
						ResourceHolder.imgBreak[i][1].draw()
					} catch(_:Exception) {
					}

				preloadSet++
			}
			2 -> {
				for(i in 0..<ResourceHolder.pEraseMax)
					try {
						ResourceHolder.imgPErase[i].draw()
					} catch(_:Exception) {
					}

				preloadSet++
			}
			3 -> {
				ResourceHolder.let {
					try {
						listOf(it.imgFont, it.imgNum).flatten().forEach {i ->
							i.draw()
						}
						it.imgFontNano.draw()
					} catch(_:Exception) {
					}
				}
				preloadSet++
			}
		}
	}

	companion object {
		/** This state's ID */
		const val ID = 0
		/** Log */
		internal val log = LogManager.getLogger()
	}
}
