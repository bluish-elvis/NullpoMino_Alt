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

import mu.nu.nullpo.game.event.EventReceiver.COLOR
import mu.nu.nullpo.gui.GameKeyDummy
import mu.nu.nullpo.gui.slick.img.FontNormal
import org.newdawn.slick.GameContainer
import org.newdawn.slick.Graphics
import org.newdawn.slick.state.StateBasedGame

/** キーボード設定画面のステート */
class StateConfigKeyboardNavi:DummyMenuChooseState() {

	/** Player number */
	var player = 0

	/** StateBasedGame */
	private lateinit var gameObj:StateBasedGame
	override val maxCursor = 1
	init {
		minChoiceY = 1
	}

	/* Fetch this state's ID */
	override fun getID():Int = ID

	/* State initialization */
	override fun init(container:GameContainer, game:StateBasedGame) {
		gameObj = game
	}

	/** Get key name
	 * @param key Keycode
	 * @return Key name
	 */
	private fun getKeyName(key:Int):String {
		val str = org.lwjgl.input.Keyboard.getKeyName(key)
		return str?.toUpperCase() ?: key.toString()
	}

	/* Draw the screen */
	override fun renderImpl(container:GameContainer, game:StateBasedGame, g:Graphics) {
		g.drawImage(ResourceHolder.imgMenuBG[0], 0f, 0f)

		FontNormal.printFontGrid(1, 1, "KEYBOARD NAVIGATION SETTING ("+(player+1)+"P)", COLOR.ORANGE)

		FontNormal.printFontGrid(1, 3+cursor, "b", COLOR.RED)

		FontNormal.printFontGrid(2, 3, "COPY FROM GAME KEYS", cursor==0)
		FontNormal.printFontGrid(2, 4, "CUSTOMIZE", cursor==1)
	}

	override fun onDecide(container:GameContainer, game:StateBasedGame, delta:Int):Boolean {
		if(cursor==0)
			for(i in 0 until GameKeyDummy.MAX_BUTTON) {
				ResourceHolder.soundManager.play("decide1")
				GameKey.gamekey[player].keymapNav[i] = GameKey.gamekey[player].keymap[i]
			}
		else if(cursor==1) {
			ResourceHolder.soundManager.play("decide2")
			NullpoMinoSlick.stateConfigKeyboard.player = player
			NullpoMinoSlick.stateConfigKeyboard.isNavSetting = true
			game.enterState(StateConfigKeyboard.ID)
			return true
		}

		GameKey.gamekey[player].saveConfig(NullpoMinoSlick.propConfig)
		NullpoMinoSlick.saveConfig()

		gameObj.enterState(StateConfigMainMenu.ID)
		return true
	}

	override fun onCancel(container:GameContainer, game:StateBasedGame, delta:Int):Boolean {
		game.enterState(StateConfigMainMenu.ID)
		return false
	}

	/** Called when entering this state */
	override fun enter(container:GameContainer?, game:StateBasedGame?) {}

	/** Called when leaving this state */
	override fun leave(container:GameContainer?, game:StateBasedGame?) {}

	companion object {
		/** This state's ID */
		const val ID = 16
	}
}
