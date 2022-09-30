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

package mu.nu.nullpo.gui.common

import mu.nu.nullpo.game.component.Controller
import mu.nu.nullpo.util.CustomProperties

open class GameKeyDummy
/** Player numberを指定できるConstructor
 * @param player Player number
 */
@JvmOverloads protected constructor(
	/** Player ID */
	val player:Int = 0
) {

	/** Key code (ingame) */
	val keymap:Array<IntArray>

	/** Key code (menu) */
	val keymapNav:Array<IntArray>

	/** Joystick button number */
	val buttonmap:Array<IntArray>

	/** Joystick direction key border */
	var joyBorder = 0

	/** Button input flag and length */
	protected val inputState:IntArray

	/** Button input flag */
	protected val pressState:BooleanArray

	private val empty get() = IntArray(0)

	init {
		keymap = Array(MAX_BUTTON) {empty}
		keymapNav = Array(MAX_BUTTON) {empty}
		buttonmap = Array(MAX_BUTTON) {empty}
		joyBorder = 0
		inputState = IntArray(MAX_BUTTON)
		pressState = BooleanArray(MAX_BUTTON)
	}

	/** Clear button input state */
	fun clear() {
		for(i in 0 until MAX_BUTTON)
			inputState[i] = 0
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
	fun isPressKey(key:Int):Boolean = inputState[key]>=1

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
	open fun loadConfig(prop:CustomProperties) {
		// Keyboard - ingame
		keymap[BUTTON_UP] = prop.getProperties("key.p$player.up", empty)
		keymap[BUTTON_DOWN] = prop.getProperties("key.p$player.down", empty)
		keymap[BUTTON_LEFT] = prop.getProperties("key.p$player.left", empty)
		keymap[BUTTON_RIGHT] = prop.getProperties("key.p$player.right", empty)
		keymap[BUTTON_A] = prop.getProperties("key.p$player.a", empty)
		keymap[BUTTON_B] = prop.getProperties("key.p$player.b", empty)
		keymap[BUTTON_C] = prop.getProperties("key.p$player.c", empty)
		keymap[BUTTON_D] = prop.getProperties("key.p$player.d", empty)
		keymap[BUTTON_E] = prop.getProperties("key.p$player.e", empty)
		keymap[BUTTON_F] = prop.getProperties("key.p$player.f", empty)
		keymap[BUTTON_QUIT] = prop.getProperties("key.p$player.quit", empty)
		keymap[BUTTON_PAUSE] = prop.getProperties("key.p$player.pause", empty)
		keymap[BUTTON_GIVEUP] = prop.getProperties("key.p$player.giveup", empty)
		keymap[BUTTON_RETRY] = prop.getProperties("key.p$player.retry", empty)
		keymap[BUTTON_FRAMESTEP] = prop.getProperties("key.p$player.framestep", empty)
		keymap[BUTTON_SCREENSHOT] = prop.getProperties("key.p$player.screenshot", empty)

		// Keyboard - menu
		keymapNav[BUTTON_UP] = prop.getProperties("keynav.p$player.up", keymap[BUTTON_UP])
		keymapNav[BUTTON_DOWN] = prop.getProperties("keynav.p$player.down", keymap[BUTTON_DOWN])
		keymapNav[BUTTON_LEFT] = prop.getProperties("keynav.p$player.left", keymap[BUTTON_LEFT])
		keymapNav[BUTTON_RIGHT] = prop.getProperties("keynav.p$player.right", keymap[BUTTON_RIGHT])
		keymapNav[BUTTON_A] = prop.getProperties("keynav.p$player.a", keymap[BUTTON_A])
		keymapNav[BUTTON_B] = prop.getProperties("keynav.p$player.b", keymap[BUTTON_B])
		keymapNav[BUTTON_C] = prop.getProperties("keynav.p$player.c", keymap[BUTTON_C])
		keymapNav[BUTTON_D] = prop.getProperties("keynav.p$player.d", keymap[BUTTON_D])
		keymapNav[BUTTON_E] = prop.getProperties("keynav.p$player.e", keymap[BUTTON_E])
		keymapNav[BUTTON_F] = prop.getProperties("keynav.p$player.f", keymap[BUTTON_F])
		keymapNav[BUTTON_QUIT] = prop.getProperties("keynav.p$player.quit", keymap[BUTTON_QUIT])
		keymapNav[BUTTON_PAUSE] = prop.getProperties("keynav.p$player.pause", keymap[BUTTON_PAUSE])
		keymapNav[BUTTON_GIVEUP] = prop.getProperties("keynav.p$player.giveup", keymap[BUTTON_GIVEUP])
		keymapNav[BUTTON_RETRY] = prop.getProperties("keynav.p$player.retry", keymap[BUTTON_RETRY])
		keymapNav[BUTTON_FRAMESTEP] = prop.getProperties("keynav.p$player.framestep", keymap[BUTTON_FRAMESTEP])
		keymapNav[BUTTON_SCREENSHOT] = prop.getProperties("keynav.p$player.screenshot", keymap[BUTTON_SCREENSHOT])

		// Joystick
		//buttonmap[BUTTON_UP] = prop.getProperty("button.p" + player + ".up", 0);
		//buttonmap[BUTTON_DOWN] = prop.getProperty("button.p" + player + ".down", 0);
		//buttonmap[BUTTON_LEFT] = prop.getProperty("button.p" + player + ".left", 0);
		//buttonmap[BUTTON_RIGHT] = prop.getProperty("button.p" + player + ".right", 0);
		buttonmap[BUTTON_A] = prop.getProperties("button.p$player.a", empty)
		buttonmap[BUTTON_B] = prop.getProperties("button.p$player.b", empty)
		buttonmap[BUTTON_C] = prop.getProperties("button.p$player.c", empty)
		buttonmap[BUTTON_D] = prop.getProperties("button.p$player.d", empty)
		buttonmap[BUTTON_E] = prop.getProperties("button.p$player.e", empty)
		buttonmap[BUTTON_F] = prop.getProperties("button.p$player.f", empty)
		buttonmap[BUTTON_QUIT] = prop.getProperties("button.p$player.quit", empty)
		buttonmap[BUTTON_PAUSE] = prop.getProperties("button.p$player.pause", empty)
		buttonmap[BUTTON_GIVEUP] = prop.getProperties("button.p$player.giveup", empty)
		buttonmap[BUTTON_RETRY] = prop.getProperties("button.p$player.retry", empty)
		buttonmap[BUTTON_FRAMESTEP] = prop.getProperties("button.p$player.framestep", empty)
		buttonmap[BUTTON_SCREENSHOT] = prop.getProperties("button.p$player.screenshot", empty)

		joyBorder = prop.getProperty("joyBorder.p$player", 0)
	}

	/** Save settings to [prop] */
	fun saveConfig(prop:CustomProperties) {
		// Keyboard - ingame
		prop.setProperty("key.p$player.up", keymap[BUTTON_UP])
		prop.setProperty("key.p$player.down", keymap[BUTTON_DOWN])
		prop.setProperty("key.p$player.left", keymap[BUTTON_LEFT])
		prop.setProperty("key.p$player.right", keymap[BUTTON_RIGHT])
		prop.setProperty("key.p$player.a", keymap[BUTTON_A])
		prop.setProperty("key.p$player.b", keymap[BUTTON_B])
		prop.setProperty("key.p$player.c", keymap[BUTTON_C])
		prop.setProperty("key.p$player.d", keymap[BUTTON_D])
		prop.setProperty("key.p$player.e", keymap[BUTTON_E])
		prop.setProperty("key.p$player.f", keymap[BUTTON_F])
		prop.setProperty("key.p$player.quit", keymap[BUTTON_QUIT])
		prop.setProperty("key.p$player.pause", keymap[BUTTON_PAUSE])
		prop.setProperty("key.p$player.giveup", keymap[BUTTON_GIVEUP])
		prop.setProperty("key.p$player.retry", keymap[BUTTON_RETRY])
		prop.setProperty("key.p$player.framestep", keymap[BUTTON_FRAMESTEP])
		prop.setProperty("key.p$player.screenshot", keymap[BUTTON_SCREENSHOT])

		// Keyboard - menu
		prop.setProperty("keynav.p$player.up", keymapNav[BUTTON_UP])
		prop.setProperty("keynav.p$player.down", keymapNav[BUTTON_DOWN])
		prop.setProperty("keynav.p$player.left", keymapNav[BUTTON_LEFT])
		prop.setProperty("keynav.p$player.right", keymapNav[BUTTON_RIGHT])
		prop.setProperty("keynav.p$player.a", keymapNav[BUTTON_A])
		prop.setProperty("keynav.p$player.b", keymapNav[BUTTON_B])
		prop.setProperty("keynav.p$player.c", keymapNav[BUTTON_C])
		prop.setProperty("keynav.p$player.d", keymapNav[BUTTON_D])
		prop.setProperty("keynav.p$player.e", keymapNav[BUTTON_E])
		prop.setProperty("keynav.p$player.f", keymapNav[BUTTON_F])
		prop.setProperty("keynav.p$player.quit", keymapNav[BUTTON_QUIT])
		prop.setProperty("keynav.p$player.pause", keymapNav[BUTTON_PAUSE])
		prop.setProperty("keynav.p$player.giveup", keymapNav[BUTTON_GIVEUP])
		prop.setProperty("keynav.p$player.retry", keymapNav[BUTTON_RETRY])
		prop.setProperty("keynav.p$player.framestep", keymapNav[BUTTON_FRAMESTEP])
		prop.setProperty("keynav.p$player.screenshot", keymapNav[BUTTON_SCREENSHOT])

		// Joystick
		//prop.setProperty("button.p" + player + ".up", buttonmap[BUTTON_UP]);
		//prop.setProperty("button.p" + player + ".down", buttonmap[BUTTON_DOWN]);
		//prop.setProperty("button.p" + player + ".left", buttonmap[BUTTON_LEFT]);
		//prop.setProperty("button.p" + player + ".right", buttonmap[BUTTON_RIGHT]);
		prop.setProperty("button.p$player.a", buttonmap[BUTTON_A])
		prop.setProperty("button.p$player.b", buttonmap[BUTTON_B])
		prop.setProperty("button.p$player.c", buttonmap[BUTTON_C])
		prop.setProperty("button.p$player.d", buttonmap[BUTTON_D])
		prop.setProperty("button.p$player.e", buttonmap[BUTTON_E])
		prop.setProperty("button.p$player.f", buttonmap[BUTTON_F])
		prop.setProperty("button.p$player.quit", buttonmap[BUTTON_QUIT])
		prop.setProperty("button.p$player.pause", buttonmap[BUTTON_PAUSE])
		prop.setProperty("button.p$player.giveup", buttonmap[BUTTON_GIVEUP])
		prop.setProperty("button.p$player.retry", buttonmap[BUTTON_RETRY])
		prop.setProperty("button.p$player.framestep", buttonmap[BUTTON_FRAMESTEP])
		prop.setProperty("button.p$player.screenshot", buttonmap[BUTTON_SCREENSHOT])

		prop.setProperty("joyBorder.p$player", joyBorder)

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

		private val DIR_NAME = listOf("UP", "DOWN", "LEFT", "RIGHT")
		private val NAV_KEYS = listOf("SELECT", "CANCEL", "C", "D", "E", "F")
		private val PLAY_KEYS = listOf("CW Spin", "CCW Spin", "CW Spin", "Swap Hold", "180 Spin", "F")
		private val SYS_KEYS = listOf("AppQuit", "Pause", "BackToMenu", "Retry", "FrameStep", "ScreenShot")

		fun arrayKeyName(isNav:Boolean):List<String> = DIR_NAME+(if(isNav) NAV_KEYS else PLAY_KEYS)+SYS_KEYS
		fun isNavKey(key:Int):Boolean =//return (key >= BUTTON_NAV_UP) && (key <= BUTTON_NAV_CANCEL);
			false
	}

}
/** Default constructor */
