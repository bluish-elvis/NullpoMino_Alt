/*
 * Copyright (c) 2010-2022, NullNoname
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
package mu.nu.nullpo.gui.slick

import mu.nu.nullpo.game.play.GameManager
import org.apache.logging.log4j.LogManager
import org.newdawn.slick.AppGameContainer
import org.newdawn.slick.GameContainer
import org.newdawn.slick.Graphics
import org.newdawn.slick.Image
import org.newdawn.slick.Sound
import org.newdawn.slick.state.BasicGameState
import org.newdawn.slick.state.StateBasedGame

/** ロード画面のステート */
class StateLoading:BasicGameState() {

	/** プリロード進行度 */
	private var preloadSet:Int = -2

	private var loadBG:Image = Image(640, 480)

	private val skindir:String = NullpoMinoSlick.propConfig.getProperty("custom.skin.directory", "res")
	/* Fetch this state's ID */
	override fun getID():Int = ID

	/* State initialization */
	override fun init(container:GameContainer, game:StateBasedGame) {}

	/* Called when entering this state */
	override fun enter(container:GameContainer?, game:StateBasedGame?) {

		//  input 関連をInitialization
		GameKey.initGlobalGameKey()
		GameKey.gamekey[0].loadConfig(NullpoMinoSlick.propConfig)
		GameKey.gamekey[1].loadConfig(NullpoMinoSlick.propConfig)

		// 設定を反映させる
		NullpoMinoSlick.setGeneralConfig()

		if(NullpoMinoSlick.propConfig.getProperty("option.se", true))
			try {
				val clip = Sound("$skindir/jingle/welcome.ogg")
				clip.play()
				loadBG = Image("$skindir/graphics/${ResourceHolder.imgTitleBG.name}.png")
				container?.graphics?.drawImage(loadBG, 0f, 0f)
			} catch(e:Throwable) {
			}

	}

	/* Draw the screen */
	override fun render(container:GameContainer, game:StateBasedGame, g:Graphics) {
		g.drawImage(loadBG, 0f, 0f)
		//	if(preloadSet<=-2) preloadSet = -1
	}

	/* Update game */
	override fun update(container:GameContainer, game:StateBasedGame, delta:Int) {
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
				if(container is AppGameContainer) {
					container.setTitle("NullpoMino_Alt version${GameManager.versionString}")
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
							if(NullpoMinoSlick.propGlobal.getProperty("$pl.tuning.owRotateButtonDefaultRight")==null)
								NullpoMinoSlick.propGlobal.setProperty("$pl.tuning.owRotateButtonDefaultRight", 0)
						NullpoMinoSlick.propGlobal.setProperty("global.firstSetupMode", false)
					}

					// Save settings
					NullpoMinoSlick.saveConfig()
				}
				// Go to title screen
				game.enterState(StateTitle.ID)
			}
		}
	}

	private fun cacheImg() {
		// 巨大な画像をあらかじめ画面に描画することでメモリにキャッシュさせる
		when(preloadSet) {
			0 -> {
				for(i in 0 until ResourceHolder.blockBreakMax)
					try {
						ResourceHolder.imgBreak[i][0].draw()
					} catch(e:Exception) {
					}

				preloadSet++
			}
			1 -> {
				for(i in 0 until ResourceHolder.blockBreakMax)
					try {
						ResourceHolder.imgBreak[i][1].draw()
					} catch(e:Exception) {
					}

				preloadSet++
			}
			2 -> {
				for(i in 0 until ResourceHolder.pEraseMax)
					try {
						ResourceHolder.imgPErase[i].draw()
					} catch(e:Exception) {
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
					} catch(e:Exception) {
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
