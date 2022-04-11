/*
 * Copyright (c) 2010-2021, NullNoname
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

import mu.nu.nullpo.gui.common.GameKeyDummy
import org.newdawn.slick.Input

/** Key input state manager (Only use with Slick. Don't use inside game
 * modes!) */
class GameKey:GameKeyDummy {

	/** Default constructor */
	constructor():super()

	/** Constructor with player number param
	 * @param pl Player number
	 */
	constructor(pl:Int):super(pl)

	/** Update button input status
	 * @param input Slick's Input class (You can get it with container.getInput())
	 * @param ingame true if ingame
	 */
	@JvmOverloads
	fun update(input:Input, ingame:Boolean = false) {
		if(player==0&&NullpoMinoSlick.useJInputKeyboard) JInputManager.poll()

		for(i in 0 until MAX_BUTTON) {
			val kmap = if(ingame) keymap else keymapNav

			val flag = kmap[i].any {
				if(NullpoMinoSlick.useJInputKeyboard)
					JInputManager.isKeyDown(it)
				else
					input.isKeyDown(it)
			} or when(i) {
				BUTTON_UP -> ControllerManager.isControllerUp(player, input)
				BUTTON_DOWN -> ControllerManager.isControllerDown(player, input)
				BUTTON_LEFT -> ControllerManager.isControllerLeft(player, input)
				BUTTON_RIGHT -> ControllerManager.isControllerRight(player, input)
				else -> buttonmap[i].any {ControllerManager.isControllerButton(player, input, it)}
			}

			if(flag) inputState[i]++
			else inputState[i] = 0
		}
	}

	/** Reset keyboard settings to default
	 * @param type Settings type (0=Blockbox 1=Guideline 2=NullpoMino-Classic)
	 */
	@JvmOverloads
	fun loadDefaultKeymap(type:Int = 0) {
		loadDefaultGameKeymap(type)
		loadDefaultMenuKeymap(type)
	}

	/** Reset in-game keyboard settings to default. Menu keys are unchanged.
	 * @param type Settings type (0=Blockbox 1=Guideline
	 * 2=NullpoMino-Classic)
	 */
	fun loadDefaultGameKeymap(type:Int) {
		System.arraycopy(DEFAULTKEYS[0][type], 0, keymap, 0, keymap.size)
	}

	/** Reset menu keyboard settings to default. In-game keys are unchanged.
	 * @param type Settings type (0=Blockbox 1=Guideline
	 * 2=NullpoMino-Classic)
	 */
	fun loadDefaultMenuKeymap(type:Int) {
		System.arraycopy(DEFAULTKEYS[1][type], 0, keymapNav, 0, keymapNav.size)
	}

	companion object {
		/** Key input state (Used by all game states) */
		var gamekey:Array<GameKey> = emptyArray()

		/** Default key mappings */
		val DEFAULTKEYS = arrayOf(
			// Ingame
			arrayOf(
				// Blockbox type
				intArrayOf(Input.KEY_UP, Input.KEY_DOWN, Input.KEY_LEFT, Input.KEY_RIGHT, Input.KEY_Z, Input.KEY_X, Input.KEY_A, Input.KEY_SPACE, Input.KEY_D, Input.KEY_S, Input.KEY_F12, Input.KEY_ESCAPE, Input.KEY_F11, Input.KEY_F10, Input.KEY_N, Input.KEY_F5),
				// Guideline games type
				intArrayOf(Input.KEY_SPACE, Input.KEY_DOWN, Input.KEY_LEFT, Input.KEY_RIGHT, Input.KEY_Z, Input.KEY_UP, Input.KEY_C, Input.KEY_LSHIFT, Input.KEY_X, Input.KEY_V, Input.KEY_F12, Input.KEY_ESCAPE, Input.KEY_F11, Input.KEY_F10, Input.KEY_N, Input.KEY_F5),
				// NullpoMino classic type
				intArrayOf(Input.KEY_UP, Input.KEY_DOWN, Input.KEY_LEFT, Input.KEY_RIGHT, Input.KEY_A, Input.KEY_S, Input.KEY_D, Input.KEY_Z, Input.KEY_X, Input.KEY_C, Input.KEY_ESCAPE, Input.KEY_F1, Input.KEY_F12, Input.KEY_F11, Input.KEY_N, Input.KEY_F10)),
			// Menu
			arrayOf(
				// Blockbox type
				intArrayOf(Input.KEY_UP, Input.KEY_DOWN, Input.KEY_LEFT, Input.KEY_RIGHT, Input.KEY_ENTER, Input.KEY_ESCAPE, Input.KEY_A, Input.KEY_SPACE, Input.KEY_D, Input.KEY_S, Input.KEY_F12, Input.KEY_F1, Input.KEY_F11, Input.KEY_F10, Input.KEY_N, Input.KEY_F5),
				// Guideline games type
				intArrayOf(Input.KEY_UP, Input.KEY_DOWN, Input.KEY_LEFT, Input.KEY_RIGHT, Input.KEY_ENTER, Input.KEY_ESCAPE, Input.KEY_C, Input.KEY_LSHIFT, Input.KEY_X, Input.KEY_V, Input.KEY_F12, Input.KEY_F1, Input.KEY_F11, Input.KEY_F10, Input.KEY_N, Input.KEY_F5),
				// NullpoMino classic type
				intArrayOf(Input.KEY_UP, Input.KEY_DOWN, Input.KEY_LEFT, Input.KEY_RIGHT, Input.KEY_A, Input.KEY_S, Input.KEY_D, Input.KEY_Z, Input.KEY_X, Input.KEY_C, Input.KEY_ESCAPE, Input.KEY_F1, Input.KEY_F12, Input.KEY_F11, Input.KEY_N, Input.KEY_F10)))

		/** Init everything */
		fun initGlobalGameKey() {
			ControllerManager.initControllers()
			JInputManager.initKeymap()
			JInputManager.initKeyboard()
			gamekey = arrayOf(GameKey(0), GameKey(1))
		}
	}
}