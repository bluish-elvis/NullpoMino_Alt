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
import mu.nu.nullpo.util.GeneralUtil
import org.newdawn.slick.*
import org.newdawn.slick.state.BasicGameState
import org.newdawn.slick.state.StateBasedGame

/** Keyboard config screen state */
class StateConfigKeyboard:BasicGameState() {

	/** Player number */
	var player = 0

	/** true if navigation key setting mode */
	var isNavSetting = false

	/** StateBasedGame */
	private lateinit var gameObj:StateBasedGame

	/** true if no key is pressed now (for JInput mode) */
	private var noPressedKey:Boolean = false

	/** Number of button currently being configured */
	private var keynum:Int = 0

	/** Frame counter */
	private var frame:Int = 0

	/** Nunber of frames left in key-set mode */
	private var keyConfigRestFrame:Int = 0

	/** Button settings */
	private var keymap:IntArray = IntArray(NUM_KEYS)

	/* Fetch this state's ID */
	override fun getID():Int = ID

	/** Button settings initialization */
	private fun reset() {
		noPressedKey = true

		keynum = 0
		frame = 0
		keyConfigRestFrame = 0

		keymap = IntArray(NUM_KEYS) {
			if(!isNavSetting) GameKey.gamekey[player].keymap[it]
			else GameKey.gamekey[player].keymapNav[it]
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
		return str?.toUpperCase() ?: "$key"
	}

	/* Draw the screen */
	override fun render(container:GameContainer, game:StateBasedGame, g:Graphics) {
		if(!container.hasFocus()) {
			if(!NullpoMinoSlick.alternateFPSTiming) NullpoMinoSlick.alternateFPSSleep()
			return
		}

		g.drawImage(ResourceHolder.imgMenuBG[0], 0f, 0f)

		if(!isNavSetting)
			FontNormal.printFontGrid(1, 1, "KEYBOARD SETTING (${player+1}P)", COLOR.ORANGE)
		else
			FontNormal.printFontGrid(1, 1, "KEYBOARD NAVIGATION SETTING (${player+1}P)", COLOR.ORANGE)
		if(!NullpoMinoSlick.useJInputKeyboard)
			FontNormal.printFontGrid(1, 2, "SLICK NATIVE MODE", COLOR.CYAN)
		else
			FontNormal.printFontGrid(1, 2, "JINPUT MODE", COLOR.PINK)

		FontNormal.printFontGrid(2, 4, "UP          : ${getKeyName(keymap[GameKeyDummy.BUTTON_UP])}", keynum==0)
		FontNormal.printFontGrid(2, 5, "DOWN        : ${getKeyName(keymap[GameKeyDummy.BUTTON_DOWN])}", keynum==1)
		FontNormal.printFontGrid(2, 6, "LEFT        : ${getKeyName(keymap[GameKeyDummy.BUTTON_LEFT])}", keynum==2)
		FontNormal.printFontGrid(2, 7, "RIGHT       : ${getKeyName(keymap[GameKeyDummy.BUTTON_RIGHT])}", keynum==3)
		if(!isNavSetting) {
			FontNormal.printFontGrid(2, 8, "A (L/R-ROT) : ${getKeyName(keymap[GameKeyDummy.BUTTON_A])}", keynum==4)
			FontNormal.printFontGrid(2, 9, "B (R/L-ROT) : ${getKeyName(keymap[GameKeyDummy.BUTTON_B])}", keynum==5)
			FontNormal.printFontGrid(2, 10, "C (L/R-ROT) : ${getKeyName(keymap[GameKeyDummy.BUTTON_C])}", keynum==6)
			FontNormal.printFontGrid(2, 11, "D (HOLD)    : ${getKeyName(keymap[GameKeyDummy.BUTTON_D])}", keynum==7)
			FontNormal.printFontGrid(2, 12, "E (180-ROT) : ${getKeyName(keymap[GameKeyDummy.BUTTON_E])}", keynum==8)
		} else {
			FontNormal.printFontGrid(2, 8, "A (SELECT)  : ${getKeyName(keymap[GameKeyDummy.BUTTON_A])}", keynum==4)
			FontNormal.printFontGrid(2, 9, "B (CANCEL)  : ${getKeyName(keymap[GameKeyDummy.BUTTON_B])}", keynum==5)
			FontNormal.printFontGrid(2, 10, "C           : ${getKeyName(keymap[GameKeyDummy.BUTTON_C])}", keynum==6)
			FontNormal.printFontGrid(2, 11, "D           : ${getKeyName(keymap[GameKeyDummy.BUTTON_D])}", keynum==7)
			FontNormal.printFontGrid(2, 12, "E           : ${getKeyName(keymap[GameKeyDummy.BUTTON_E])}", keynum==8)
		}
		FontNormal.printFontGrid(2, 13, "F           : ${getKeyName(keymap[GameKeyDummy.BUTTON_F])}", keynum==9)
		FontNormal.printFontGrid(2, 14, "QUIT        : ${getKeyName(keymap[GameKeyDummy.BUTTON_QUIT])}", keynum==10)
		FontNormal.printFontGrid(2, 15, "PAUSE       : ${getKeyName(keymap[GameKeyDummy.BUTTON_PAUSE])}", keynum==11)
		FontNormal.printFontGrid(2, 16, "GIVEUP      : ${getKeyName(keymap[GameKeyDummy.BUTTON_GIVEUP])}", keynum==12)
		FontNormal.printFontGrid(2, 17, "RETRY       : ${getKeyName(keymap[GameKeyDummy.BUTTON_RETRY])}", keynum==13)
		FontNormal.printFontGrid(2, 18, "FRAME STEP  : ${getKeyName(keymap[GameKeyDummy.BUTTON_FRAMESTEP])}", keynum==14)
		FontNormal.printFontGrid(2, 19, "SCREEN SHOT : ${getKeyName(keymap[GameKeyDummy.BUTTON_SCREENSHOT])}", keynum==15)
		FontNormal.printFontGrid(2, 20, "[SAVE & EXIT]", keynum==16)

		FontNormal.printFontGrid(1, 4+keynum, "\u0082", COLOR.RAINBOW)

		if(frame>=KEYACCEPTFRAME)
			when {
				keyConfigRestFrame>0 -> FontNormal.printFontGrid(1, 22, "PUSH KEY... "+GeneralUtil.getTime(keyConfigRestFrame), COLOR.PINK)
				keynum<NUM_KEYS -> {
					FontNormal.printFontGrid(1, 22, "UP/DOWN:   MOVE CURSOR", COLOR.GREEN)
					FontNormal.printFontGrid(1, 23, "ENTER:     SET KEY", COLOR.GREEN)
					FontNormal.printFontGrid(1, 24, "DELETE:    SET TO NONE", COLOR.GREEN)
					FontNormal.printFontGrid(1, 25, "BACKSPACE: CANCEL", COLOR.GREEN)
				}
				else -> {
					FontNormal.printFontGrid(1, 22, "UP/DOWN:   MOVE CURSOR", COLOR.GREEN)
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
				keymap[keynum] = key
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

				// Enter
				if(key==Input.KEY_ENTER) {

					if(keynum>=NUM_KEYS) {
						ResourceHolder.soundManager.play("decide2")
						// Save & Exit
						for(i in 0 until NUM_KEYS)
							if(!isNavSetting)
								GameKey.gamekey[player].keymap[i] = keymap[i]
							else
								GameKey.gamekey[player].keymapNav[i] = keymap[i]
						GameKey.gamekey[player].saveConfig(NullpoMinoSlick.propConfig)
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
					if(keynum<NUM_KEYS&&keymap[keynum]!=0) {
						ResourceHolder.soundManager.play("change")
						keymap[keynum] = 0
					}

				// Backspace
				if(key==Input.KEY_BACK)
					if(isNavSetting)
						gameObj.enterState(StateConfigKeyboardNavi.ID)
					else
						gameObj.enterState(StateConfigMainMenu.ID)
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
	}
}
