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

import mu.nu.nullpo.game.event.EventReceiver.COLOR
import mu.nu.nullpo.gui.common.BaseFont
import mu.nu.nullpo.gui.common.GameKeyDummy
import mu.nu.nullpo.gui.slick.img.FontNormal
import mu.nu.nullpo.util.GeneralUtil.toTimeStr
import org.newdawn.slick.GameContainer
import org.newdawn.slick.Graphics
import org.newdawn.slick.Input
import org.newdawn.slick.state.BasicGameState
import org.newdawn.slick.state.StateBasedGame

/** Keyboard config screen state */
class StateConfigKeyboard:BasicGameState() {
	/** Player number */
	var player = 0

	/** True if navigation key setting mode */
	var isNavSetting = false

	/** StateBasedGame */
	private lateinit var gameObj:StateBasedGame

	/** True if no key is pressed now (for JInput mode) */
	private var noPressedKey = false

	/** Number of button currently being configured */
	private var keynum = 0
	private var keypos = 0

	/** Frame counter */
	private var frame = 0

	/** Nunber of frames left in key-set mode */
	private var keyConfigRestFrame = 0

	/** Button settings */
	private var keymap = MutableList(NUM_KEYS) {MutableList(MAX_KEYS) {0}}

	/* Fetch this state's ID */
	override fun getID():Int = ID

	/** Button settings initialization */
	private fun reset() {
		noPressedKey = true

		keynum = 0
		frame = 0
		keyConfigRestFrame = 0

		keymap = MutableList(NUM_KEYS) {
			if(!isNavSetting) GameKey.gameKey[player].keymap[it]
			else GameKey.gameKey[player].keymapNav[it]
		}
	}

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
		return str?.uppercase() ?: "$key"
	}

	/* Draw the screen */
	override fun render(container:GameContainer, game:StateBasedGame, g:Graphics) {
		if(!container.hasFocus()) {
			if(!NullpoMinoSlick.alternateFPSTiming) NullpoMinoSlick.alternateFPSSleep()
			return
		}

		g.drawImage(ResourceHolder.imgMenuBG[1], 0f, 0f)

		if(!isNavSetting)
			FontNormal.printFontGrid(1, 1, "KEYBOARD Assign (${player+1}P)", COLOR.ORANGE)
		else
			FontNormal.printFontGrid(1, 1, "KEYBOARD Navigation Assign (${player+1}P)", COLOR.ORANGE)
		if(!NullpoMinoSlick.useJInputKeyboard)
			FontNormal.printFontGrid(1, 2, "Slick native mode", COLOR.CYAN)
		else
			FontNormal.printFontGrid(1, 2, "Jinput mode", COLOR.PINK)

		for(it in 0 until GameKeyDummy.MAX_BUTTON) {
			val flag = keynum==it
			FontNormal.printFontGrid(2, it+4, GameKeyDummy.arrayKeyName(isNavSetting)[it], flag)
			FontNormal.printFontGrid(13, it+4, ":", flag)
			keymap[it].forEachIndexed {i, key -> FontNormal.printFontGrid(15+i*10, it+4, getKeyName(key), flag)}
		}
		FontNormal.printFontGrid(2, 20, "SAVE & EXIT", keynum==16)

		FontNormal.printFontGrid(
			14+if(keynum!=16) keypos*10 else 0,
			4+keynum,
			if(keynum==16) "?" else BaseFont.CURSOR,
			COLOR.RAINBOW
		)

		if(frame>=KEYACCEPTFRAME)
			when {
				keyConfigRestFrame>0 -> FontNormal.printFontGrid(1, 22, "PUSH KEY... "+keyConfigRestFrame.toTimeStr, COLOR.PINK)
				keynum<NUM_KEYS -> {
					FontNormal.printFontGrid(1, 22, "<\u008B\u008E>:   MOVE CURSOR", COLOR.GREEN)
					FontNormal.printFontGrid(1, 23, "ENTER:     SET KEY", COLOR.GREEN)
					FontNormal.printFontGrid(1, 24, "DELETE:    SET TO NONE", COLOR.GREEN)
					FontNormal.printFontGrid(1, 25, "BACKSPACE: CANCEL", COLOR.GREEN)
				}
				else -> {
					FontNormal.printFontGrid(1, 22, "<\u008B\u008E>:   MOVE CURSOR", COLOR.GREEN)
					FontNormal.printFontGrid(1, 23, "ENTER:     SAVE & EXIT", COLOR.GREEN)
					FontNormal.printFontGrid(1, 24, "BACKSPACE: CANCEL", COLOR.GREEN)
				}
			}

		// FPS
		NullpoMinoSlick.drawFPS()
		// Observer
		NullpoMinoSlick.drawObserverClient()
		if(!NullpoMinoSlick.alternateFPSTiming) NullpoMinoSlick.alternateFPSSleep()
	}

	/* Update game state */
	override fun update(container:GameContainer, game:StateBasedGame, delta:Int) {
		if(!container.hasFocus()) {
			if(NullpoMinoSlick.alternateFPSTiming) NullpoMinoSlick.alternateFPSSleep()
			return
		}

		frame++
		if(keyConfigRestFrame>0) keyConfigRestFrame--

		// JInput
		if(NullpoMinoSlick.useJInputKeyboard) {
			JInputManager.poll()

			if(frame>=KEYACCEPTFRAME)
				for(i in 0 until JInputManager.MAX_SLICK_KEY)
					if(JInputManager.isKeyDown(i)) {
						onKey(i)
						frame = 0
						break
					}
		}

		if(NullpoMinoSlick.alternateFPSTiming) NullpoMinoSlick.alternateFPSSleep()
	}

	/* When a key is released (Slick native) */
	override fun keyReleased(key:Int, c:Char) {
		if(!NullpoMinoSlick.useJInputKeyboard) onKey(key)
	}

	/** When a key is released
	 * @param key Keycode
	 */
	private fun onKey(key:Int) {
		if(frame>=KEYACCEPTFRAME)
			if(keyConfigRestFrame>0) {
				// Key-set mode
				ResourceHolder.soundManager.play("move")
				if(keymap[keynum].size<=keypos) keymap[keynum] = (keymap[keynum]+key).toMutableList() else keymap[keynum][keypos] = key
				keyConfigRestFrame = 0
			} else {
				// Menu mode
				if(key==Input.KEY_UP) {
					ResourceHolder.soundManager.play("cursor")
					keynum--
					if(keynum<0) keynum = NUM_KEYS
				}
				if(key==Input.KEY_DOWN) {
					ResourceHolder.soundManager.play("cursor")
					keynum++
					if(keynum>NUM_KEYS) keynum = 0
				}

				if(key==Input.KEY_LEFT) {
					ResourceHolder.soundManager.play("change")
					keypos--
					if(keypos<0) keypos = MAX_KEYS-1
				}
				if(key==Input.KEY_RIGHT) {
					ResourceHolder.soundManager.play("change")
					keypos++
					if(keypos>=MAX_KEYS) keypos = 0
				}
				// Enter
				if(key==Input.KEY_ENTER) {
					if(keynum>=NUM_KEYS) {
						ResourceHolder.soundManager.play("decide2")
						// Save & Exit
						for(i in 0 until NUM_KEYS)
							if(!isNavSetting)
								GameKey.gameKey[player].keymap[i] = keymap[i]
							else
								GameKey.gameKey[player].keymapNav[i] = keymap[i]
						GameKey.gameKey[player].saveConfig(NullpoMinoSlick.propConfig)
						NullpoMinoSlick.saveConfig()
						gameObj.enterState(StateConfigMainMenu.ID)
					} else {
						ResourceHolder.soundManager.play("decide1")
						// Set key
						keyConfigRestFrame = 60*5
					}
					return
				}

				// Delete
				if(key==Input.KEY_DELETE)
					if(keynum<NUM_KEYS&&keymap[keynum][keypos]>0) {
						ResourceHolder.soundManager.play("change")
						keymap[keynum] = (keymap[keynum]-keymap[keynum][keypos]).toMutableList()
					}

				// Backspace
				if(key==Input.KEY_BACK)
					gameObj.enterState(if(isNavSetting) StateConfigKeyboardNavi.ID else StateConfigMainMenu.ID)
			}
	}

	/** Called when entering this state */
	override fun enter(container:GameContainer?, game:StateBasedGame?) {
		reset()
	}

	/** Called when leaving this state */
	override fun leave(container:GameContainer?, game:StateBasedGame?) {
		reset()
	}

	companion object {
		/** This state's ID */
		const val ID = 9

		/** Number of frames you have to wait */
		const val KEYACCEPTFRAME = 15

		/** Number of keys to set */
		const val NUM_KEYS = 16
		const val MAX_KEYS = 3
	}
}
