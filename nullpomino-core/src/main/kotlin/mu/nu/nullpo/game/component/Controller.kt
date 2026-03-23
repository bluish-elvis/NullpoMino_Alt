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
package mu.nu.nullpo.game.component

import mu.nu.nullpo.util.GeneralUtil.us

/** button input状態を管理するクラス */
@kotlinx.serialization.Serializable data class Controller(
	/** Buttonを押した状態ならtrue */
	private val buttonPress:MutableList<Boolean>,
	/** Buttonを押しっぱなしにしている time */
	val buttonTime:MutableList<Int>
) {

	/** button input状態をビット flagで返す*/
	val buttonBit:UShort
		get() = buttonPress.mapIndexed {i, b -> if(b) (1 shl i) else 0}.sum().us

	/** Constructor */
	constructor():this(MutableList(BUTTON_COUNT) {false}, MutableList(BUTTON_COUNT) {0})
	/** Copy constructor from [c] */
	constructor(c:Controller):this(c.buttonPress, c.buttonTime)
	constructor(bit:Int, time:MutableList<Int>):this((0..<BUTTON_COUNT).map {(bit and (1 shl it))>0}.toMutableList(), time)
	/** 初期状態に戻す */
	fun reset() {
		clearButtonState()
		clearButtonTime()
	}

	/** 設定を[c]からコピー */
	fun replace(c:Controller) {
		for(i in 0..<BUTTON_COUNT) {
			buttonPress[i] = c.buttonPress[i]
			buttonTime[i] = c.buttonTime[i]
		}
	}

	/** buttonをすべて押していない状態にする */
	private fun clearButtonState() {
		buttonPress.fill(false)
	}

	/** buttonを押し始めた瞬間の状態かどうか判定
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
	fun isPressCount(vararg btn:Int):Int = btn.count {isPress(it)}
	/** Menu でカーソルが動くかどうか判定
	 * @param key Button number
	 * @param enableCButton C buttonでの高速移動許可
	 * @return カーソルが動くならtrue
	 */
	@JvmOverloads fun isMenuRepeatKey(key:Int, enableCButton:Boolean = true):Boolean = buttonTime[key]==1||
		buttonTime[key]>=25&&buttonTime[key]%3==0||
		buttonTime[key]>=1&&isPress(BUTTON_C)&&enableCButton

	/** buttonを押した状態にする
	 * @param key Button number
	 */
	fun setButtonPressed(key:Int) {
		if(key in buttonPress.indices) buttonPress[key] = true
	}

	/** buttonを押してない状態にする
	 * @param key Button number
	 */
	fun setButtonUnpressed(key:Int) {
		if(key in buttonPress.indices) buttonPress[key] = false
	}

	fun setButtonState(key:Int, pressed:Boolean) {
		if(key in buttonPress.indices) buttonPress[key] = pressed
	}

	fun setButtonBit(input:UShort) {
		for(i in buttonPress.indices) buttonPress[i] = (input and (1 shl i).us)>0u
	}

	/** button input timeを更新 */
	fun updateButtonTime() {
		for(i in 0..<BUTTON_COUNT)
			if(buttonPress[i]) buttonTime[i]++ else buttonTime[i] = 0
	}

	/** button input状態をリセット */
	private fun clearButtonTime() {
		buttonTime.fill(0)
	}

	companion object {
		/** ↑ (Hard drop) button */
		const val BUTTON_UP = 0

		/** ↓ (Soft drop) button */
		const val BUTTON_DOWN = 1

		/** ← (Left movement) button */
		const val BUTTON_LEFT = 2

		/** → (Right movement) button */
		const val BUTTON_RIGHT = 3

		/** A (Regular spin) button */
		const val BUTTON_A = 4

		/** B (Reverse spin) button */
		const val BUTTON_B = 5

		/** C (Regular spin) button */
		const val BUTTON_C = 6

		/** D (Hold) button */
		const val BUTTON_D = 7

		/** E (180-degree spin) button */
		const val BUTTON_E = 8

		/** F (Reverse spin, Use item, staff roll fast-forward, etc.) button */
		const val BUTTON_F = 9

		/** Number of buttons */
		const val BUTTON_COUNT = 10

		/** ビット演算用定数 */
		const val BUTTON_BIT_UP:UShort = 1u
		const val BUTTON_BIT_DOWN:UShort = 2u
		const val BUTTON_BIT_LEFT:UShort = 4u
		const val BUTTON_BIT_RIGHT:UShort = 8u
		const val BUTTON_BIT_A:UShort = 16u
		const val BUTTON_BIT_B:UShort = 32u
		const val BUTTON_BIT_C:UShort = 64u
		const val BUTTON_BIT_D:UShort = 128u
		const val BUTTON_BIT_E:UShort = 256u
		const val BUTTON_BIT_F:UShort = 512u
	}
}
