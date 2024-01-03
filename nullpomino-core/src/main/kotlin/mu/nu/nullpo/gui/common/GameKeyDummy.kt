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

package mu.nu.nullpo.gui.common

import mu.nu.nullpo.game.component.Controller
import mu.nu.nullpo.gui.common.ConfigGlobal.GameKeyMaps

abstract class GameKeyDummy(
	/** Player ID */
	val player:Int = 0, var map:GameKeyMaps = GameKeyMaps()) {

	val keymap get() = map.keymap
	val keymapNav get() = map.keymapNav
	val buttonmap get() = map.buttonmap
	var joyBorder:Int = 0
	/** Button input flag and length */
	@Transient protected val inputState = MutableList(MAX_BUTTON) {0}

	/** Button input flag */
	protected val pressState get() = inputState.map {it>0}

	/** Clear button input state */
	fun clear() {
		inputState.fill(0)
	}

	/** buttonが1 frame だけ押されているか判定
	 * @param key Button number
	 * @return 押されていたらtrue
	 */
	fun isPushKey(key:Int):Boolean = inputState[key]==1

	/** buttonが押されているか判定
	 * @param key Button number
	 * @return 押されていたらtrue
	 */
	fun isPressKey(key:Int):Boolean = pressState[key]

	/** Menu でカーソルが動くかどうか判定
	 * @param key Button number
	 * @return カーソルが動くならtrue
	 */
	fun isMenuRepeatKey(key:Int):Boolean = (inputState[key]==1||inputState[key]>=25&&inputState[key]%3==0
		||inputState[key]>=1&&isPressKey(BUTTON_C))

	/** buttonを押している timeを取得
	 * @param key Button number
	 * @return buttonを押している time (0なら押してない）
	 */
	fun getInputState(key:Int):Int = inputState[key]

	/** buttonを押している timeを強制変更
	 * @param key Button number
	 * @param state buttonを押している time
	 */
	fun setInputState(key:Int, state:Int) {
		inputState[key] = state
	}

	/** Load settings from [prop] */
	open fun loadConfig(prop:GameKeyMaps) {
		// Keyboard - ingame

		keymap.forEachIndexed {i, _ -> keymap[i] = prop.keymap[i]}
		keymapNav.forEachIndexed {i, _ -> keymapNav[i] = prop.keymap[i]}
		buttonmap.forEachIndexed {i, _ -> buttonmap[i] = prop.buttonmap[i]}

		// Joystick
		joyBorder = prop.joyBorder
	}

	/** Save settings to [prop] */
	fun saveConfig(prop:GameKeyMaps) {
		// Keyboard - ingame
		keymap.forEachIndexed {i, it -> prop.keymap[i] = it}
		keymapNav.forEachIndexed {i, it -> prop.keymapNav[i] = it}
		buttonmap.forEachIndexed {i, it -> prop.buttonmap[i] = it}

		// Joystick
		prop.joyBorder = joyBorder
	}

	/** Controllerに input 状況を伝える
	 * @param ctrl input 状況を伝えるControllerのインスタンス
	 */
	fun inputStatusUpdate(ctrl:Controller?) {
		val c = ctrl ?: return
		c.setButtonBit(inputState.mapIndexed {i, b -> if(b>0) (1 shl i) else 0}.sum())
		inputState.forEachIndexed {i, v ->
			if(i<Controller.BUTTON_COUNT) {
				c.buttonTime[i] = v-1
			}
		}
	}

	companion object {
		/** Button number constants */
		const val BUTTON_UP = 0
		const val BUTTON_DOWN = 1
		const val BUTTON_LEFT = 2
		const val BUTTON_RIGHT = 3
		const val BUTTON_A = 4
		const val BUTTON_B = 5
		const val BUTTON_C = 6
		const val BUTTON_D = 7
		const val BUTTON_E = 8
		const val BUTTON_F = 9
		const val BUTTON_QUIT = 10
		const val BUTTON_PAUSE = 11
		const val BUTTON_GIVEUP = 12
		const val BUTTON_RETRY = 13
		const val BUTTON_FRAMESTEP = 14
		const val BUTTON_SCREENSHOT = 15

		/** Max button number */
		const val MAX_BUTTON = 16
		const val MAX_PLAYERS = 2

		private val DIR_NAME = listOf("UP", "DOWN", "LEFT", "RIGHT")
		private val NAV_KEYS = listOf("SELECT", "CANCEL", "C", "D", "E", "F")
		private val PLAY_KEYS = listOf("CW Spin", "CCW Spin", "CW Spin", "Swap Hold", "180 Spin", "F")
		private val SYS_KEYS = listOf("AppQuit", "Pause", "BackToMenu", "Retry", "FrameStep", "ScreenShot")

		fun arrayKeyName(isNav:Boolean):List<String> = DIR_NAME+(if(isNav) NAV_KEYS else PLAY_KEYS)+SYS_KEYS
	}

}
