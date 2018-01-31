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

import mu.nu.nullpo.game.play.GameManager
import org.apache.log4j.Logger
import org.newdawn.slick.*
import org.newdawn.slick.state.BasicGameState
import org.newdawn.slick.state.StateBasedGame

/** ロード画面のステート */
class StateLoading:BasicGameState() {

	/** プリロード進行度 */
	private var preloadCount:Int = 0
	private var preloadSet:Int = 0

	/* Fetch this state's ID */
	override fun getID():Int = ID

	/* State initialization */
	override fun init(container:GameContainer, game:StateBasedGame) {}

	/* Called when entering this state */
	override fun enter(container:GameContainer?, game:StateBasedGame?) {
		preloadCount = 0
		preloadSet = 0

		//  input 関連をInitialization
		GameKey.initGlobalGameKey()
		GameKey.gamekey[0].loadConfig(NullpoMinoSlick.propConfig)
		GameKey.gamekey[1].loadConfig(NullpoMinoSlick.propConfig)

		// 設定を反映させる
		NullpoMinoSlick.setGeneralConfig()

		// 画像などを読み込み
		try {
			ResourceHolder.load()
		} catch(e:Throwable) {
			log.error("Resource load failed", e)
		}

	}

	/* Draw the screen */
	override fun render(container:GameContainer, game:StateBasedGame, g:Graphics) {
		// 巨大な画像をあらかじめ画面に描画することでメモリにキャッシュさせる
		if(preloadSet==0)
			if(preloadCount<ResourceHolder.BLOCK_BREAK_MAX) {
				try {
					ResourceHolder.imgBreak[preloadCount][0].draw(0f, 0f)
				} catch(e:Exception) {
				}

				preloadCount++
			} else {
				preloadCount = 0
				preloadSet++
			}
		if(preloadSet==1)
			if(preloadCount<ResourceHolder.BLOCK_BREAK_MAX) {
				try {
					ResourceHolder.imgBreak[preloadCount][1].draw(0f, 0f)
				} catch(e:Exception) {
				}

				preloadCount++
			} else {
				preloadCount = 0
				preloadSet++
			}
		if(preloadSet==2)
			if(preloadCount<ResourceHolder.PERASE_MAX) {
				try {
					ResourceHolder.imgPErase[preloadCount].draw(0f, 0f)
				} catch(e:Exception) {
				}

				preloadCount++
			} else {
				preloadCount = 0
				preloadSet++
			}
		if(preloadSet==3) {
			ResourceHolder.imgFont.draw(0f, 0f)
			preloadSet++
		}

		g.color = Color.black
		g.fillRect(0f, 0f, 640f, 480f)
	}

	/* Update game */
	override fun update(container:GameContainer, game:StateBasedGame, delta:Int) {
		if(preloadSet>2) {
			// Change title bar caption
			if(container is AppGameContainer) {
				container.setTitle("NullpoMino_Alt version"+GameManager.versionString)
				container.setUpdateOnlyWhenVisible(true)
			}

			// First run
			if(NullpoMinoSlick.propConfig.getProperty("option.firstSetupMode", true)) {
				// Set various default settings here
				GameKey.gamekey[0].loadDefaultKeymap()
				GameKey.gamekey[0].saveConfig(NullpoMinoSlick.propConfig)
				NullpoMinoSlick.propConfig.setProperty("option.firstSetupMode", false)

				// Set default rotation button setting (only for first run)
				if(NullpoMinoSlick.propGlobal.getProperty("global.firstSetupMode", true)) {
					for(pl in 0..1)
						if(NullpoMinoSlick.propGlobal.getProperty(pl.toString()+".tuning.owRotateButtonDefaultRight")==null)
							NullpoMinoSlick.propGlobal.setProperty(pl.toString()+".tuning.owRotateButtonDefaultRight", 0)
					NullpoMinoSlick.propGlobal.setProperty("global.firstSetupMode", false)
				}

				// Save settings
				NullpoMinoSlick.saveConfig()

				// Go to title screen
				game.enterState(StateTitle.ID)
			} else
				game.enterState(StateTitle.ID)
		}
	}

	companion object {
		/** This state's ID */
		const val ID = 0
		/** Log */
		internal val log = Logger.getLogger(StateLoading::class.java)
	}
}
