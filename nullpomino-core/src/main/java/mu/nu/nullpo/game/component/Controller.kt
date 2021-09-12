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
package mu.nu.nullpo.game.component

import java.io.Serializable

/** button input状態を管理するクラス */
class Controller:Serializable {

	/** Buttonを押した状態ならtrue */
	val buttonPress = MutableList(BUTTON_COUNT){false}

	/** Buttonを押しっぱなしにしている time */
	val buttonTime = MutableList(BUTTON_COUNT){0}

	/** button input状態をビット flagで返す
	 * @return button input状態のビット flag
	 */
	var buttonBit:Int
		get() {
			var input = 0

			if(buttonPress[BUTTON_UP]) input = input or BUTTON_BIT_UP
			if(buttonPress[BUTTON_DOWN]) input = input or BUTTON_BIT_DOWN
			if(buttonPress[BUTTON_LEFT]) input = input or BUTTON_BIT_LEFT
			if(buttonPress[BUTTON_RIGHT]) input = input or BUTTON_BIT_RIGHT
			if(buttonPress[BUTTON_A]) input = input or BUTTON_BIT_A
			if(buttonPress[BUTTON_B]) input = input or BUTTON_BIT_B
			if(buttonPress[BUTTON_C]) input = input or BUTTON_BIT_C
			if(buttonPress[BUTTON_D]) input = input or BUTTON_BIT_D
			if(buttonPress[BUTTON_E]) input = input or BUTTON_BIT_E
			if(buttonPress[BUTTON_F]) input = input or BUTTON_BIT_F

			return input
		}
		set(input) {
			clearButtonState()

			if(input and BUTTON_BIT_UP>0) buttonPress[BUTTON_UP] = true
			if(input and BUTTON_BIT_DOWN>0) buttonPress[BUTTON_DOWN] = true
			if(input and BUTTON_BIT_LEFT>0) buttonPress[BUTTON_LEFT] = true
			if(input and BUTTON_BIT_RIGHT>0) buttonPress[BUTTON_RIGHT] = true
			if(input and BUTTON_BIT_A>0) buttonPress[BUTTON_A] = true
			if(input and BUTTON_BIT_B>0) buttonPress[BUTTON_B] = true
			if(input and BUTTON_BIT_C>0) buttonPress[BUTTON_C] = true
			if(input and BUTTON_BIT_D>0) buttonPress[BUTTON_D] = true
			if(input and BUTTON_BIT_E>0) buttonPress[BUTTON_E] = true
			if(input and BUTTON_BIT_F>0) buttonPress[BUTTON_F] = true
		}

	/** Constructor */
	constructor() {
		reset()
	}

	/** Copy constructor
	 * @param c Copy source
	 */
	constructor(c:Controller) {
		copy(c)
	}

	/** 初期状態に戻す */
	fun reset() {
		clearButtonState()
		clearButtonTime()
	}

	/** 他のController stateをコピー
	 * @param c Copy source
	 */
	fun copy(c:Controller) {
		for(i in 0 until BUTTON_COUNT) {
			buttonPress[i] = c.buttonPress[i]
			buttonTime[i] = c.buttonTime[i]
		}
	}

	/** buttonをすべて押していない状態にする */
	fun clearButtonState() {
		buttonPress.fill(false)
	}

	/** buttonを1 frame だけ押した状態かどうか判定
	 * @param btn Button number
	 * @return buttonを1 frame だけ押した状態ならtrue
	 */
	fun isPush(btn:Int):Boolean = buttonTime[btn]==1

	/** buttonを押している状態かどうか判定
	 * @param btn Button number
	 * @return buttonを押している状態ならtrue
	 */
	fun isPress(btn:Int):Boolean = buttonTime[btn]>=1
	fun isPressAll(vararg btn:Int):Boolean = btn.all {isPress(it)}
	fun isPressAny(vararg btn:Int):Boolean = btn.any {isPress(it)}

	/** Menu でカーソルが動くかどうか判定
	 * @param key Button number
	 * @param enableCButton C buttonでの高速移動許可
	 * @return カーソルが動くならtrue
	 */
	@JvmOverloads
	fun isMenuRepeatKey(key:Int, enableCButton:Boolean = true):Boolean {
		return buttonTime[key]==1||buttonTime[key]>=25&&buttonTime[key]%3==0||
			buttonTime[key]>=1&&isPress(BUTTON_C)&&enableCButton

	}

	/** buttonを押した状態にする
	 * @param key Button number
	 */
	fun setButtonPressed(key:Int) {
		if(key>=0&&key<buttonPress.size) buttonPress[key] = true
	}

	/** buttonを押してない状態にする
	 * @param key Button number
	 */
	fun setButtonUnpressed(key:Int) {
		if(key>=0&&key<buttonPress.size) buttonPress[key] = false
	}

	/** buttonを押した状態を設定
	 * @param key Button number
	 * @param pressed When true,押した, falseなら押してない
	 */
	fun setButtonState(key:Int, pressed:Boolean) {
		if(key>=0&&key<buttonPress.size) buttonPress[key] = pressed
	}

	/** button input timeを更新 */
	fun updateButtonTime() {
		for(i in 0 until BUTTON_COUNT)
			if(buttonPress[i]) buttonTime[i]++ else buttonTime[i] = 0
	}

	/** button input状態をリセット */
	fun clearButtonTime() {
		buttonTime.fill(0)
	}

	companion object {
		/** Serial version ID */
		private const val serialVersionUID = -4855072501928533723L

		/** ↑ (Hard drop) button */
		const val BUTTON_UP = 0

		/** ↓ (Soft drop) button */
		const val BUTTON_DOWN = 1

		/** ← (Left movement) button */
		const val BUTTON_LEFT = 2

		/** → (Right movement) button */
		const val BUTTON_RIGHT = 3

		/** A (Regular rotation) button */
		const val BUTTON_A = 4

		/** B (Reverse rotation) button */
		const val BUTTON_B = 5

		/** C (Regular rotation) button */
		const val BUTTON_C = 6

		/** D (Hold) button */
		const val BUTTON_D = 7

		/** E (180-degree rotation) button */
		const val BUTTON_E = 8

		/** F (Use inum, staff roll fast-forward, etc.) button */
		const val BUTTON_F = 9

		/** Number of buttons */
		const val BUTTON_COUNT = 10

		/** ビット演算用定数 */
		const val BUTTON_BIT_UP = 1
		const val BUTTON_BIT_DOWN = 2
		const val BUTTON_BIT_LEFT = 4
		const val BUTTON_BIT_RIGHT = 8
		const val BUTTON_BIT_A = 16
		const val BUTTON_BIT_B = 32
		const val BUTTON_BIT_C = 64
		const val BUTTON_BIT_D = 128
		const val BUTTON_BIT_E = 256
		const val BUTTON_BIT_F = 512
	}
}