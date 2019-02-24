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
package mu.nu.nullpo.game.component

import java.io.Serializable
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.primaryConstructor

/** 音楽の再生状況を管理するクラス */
class BGMStatus:Serializable {

	/** Current BGM */
	var bgm:BGM = BGM.SILENT
	var track:Int = 0

	/** 音量 (1f=100%, .5f=50%) */
	var volume:Float = 1f

	/** BGM fadeoutスイッチ */
	var fadesw:Boolean
		get() = fadespd>0f
		set(sw) {
			fadespd = if(sw) 0.01f else 0f
		}

	/** BGM fadeout速度 */
	var fadespd:Float = 0f

	/** 音楽の定数 */
	sealed class BGM(idx:Int = 0, val hidden:Boolean = false, nums:Int = 1, ln:String = "",
		vararg sn:String = emptyArray()) {
		constructor(idx:Int, nums:Int, ln:String, vararg sn:String):this(idx, false, nums, ln, *sn)

		val id:Int = BGM::class.java.declaredClasses.indexOfFirst {it==this::class.java}
		val idx:Int = minOf(maxOf(0, idx), nums-1)
		val nums:Int = maxOf(1, nums)

		val name = this::class.simpleName ?: ""
		val longName:String = if(ln.isEmpty()) name else ln
		val subName:String = if(sn.isEmpty()) "" else sn[maxOf(minOf(this.idx, minOf(sn.size, nums)-1), 0)]
		val drawName:String = "#$id-${this.idx} ${name.replace('_', ' ')}"
		val fullName:String = "$longName $subName"
		//var filename:Array<String> = Array(maxOf(1, nums)) {""}

		override fun equals(other:Any?):Boolean =
			super.equals(other)||if(other is BGM) id==other.id&&idx==other.idx else false
		operator fun compareTo(o:BGM):Int = if(id==o.id) idx-o.idx else id-o.id
		override fun hashCode():Int = super.hashCode()

		override fun toString():String = fullName


		object SILENT:BGM(ln = "Silent")
		class GENERIC(idx:Int = 0):BGM(idx, 6, "Guidelines Modes", *Array(6) {"Level:${it+1}"})
		class RUSH(idx:Int = 0):BGM(idx, 3, "TimeAttack Rush", *Array(3) {"Level:${it+1}"})
		class EXTRA(idx:Int = 0):BGM(idx, 3, "Extra Modes")
		class RETRO_N(idx:Int = 0):BGM(idx, 3, "Retro Classic:N.")
		class RETRO_A(idx:Int = 0):BGM(idx, 5, ln = "Retro Marathon:AT")
		class RETRO_S(idx:Int = 0):BGM(idx, 3, "Retro Mania:S", "Marathon", "Deadlock", "Modern")
		class PUZZLE(idx:Int = 0):BGM(idx, 3, "Grand Blossom", "SAKURA", "TOMOYO", "CELBERUS")
		class GM_1(idx:Int = 0):BGM(idx, 2, "Grand Marathon", "NORMAL", "20G")
		class GM_2(idx:Int = 0):BGM(idx, 4, "Grand Mania", "NORMAL", "20G 500", "Storm 300/700", "Storm 500/900")
		class GM_3(idx:Int = 0):BGM(idx, 6, "Grand Mastery",
			"NORMAL", "20G", "Blitz", "Blitz 500", "Lightning 700", "Lightning 1k")

		class MENU(idx:Int = 0):BGM(idx, true, 3, "Menu Screen BGM",
			"Mode Select", "General Config", "Mode Config")

		class ENDING(idx:Int = 0):BGM(idx, true, 4, "Ending Challenge",
			"Marathon", "Mania (60sec)", "Mastery (55sec)", "Modern (200Sec)")

		class RESULT(idx:Int = 0):BGM(idx, true, 4, "Play Result",
			"Failure", "Done Sprint", "Done Enduro", "Cleared Game")

		class FINALE(idx:Int = 0):BGM(idx, true, 3, "Grand Finale", "Genuine", "Joker", "Further")

		//operator fun get(index: Int): BGM = if(this.idx)
		companion object {
			val all:List<List<BGM>>
				get() = BGM::class.sealedSubclasses.map {bg ->
					bg.objectInstance?.let {listOf(it)}
						?: List(bg.createInstance().nums) {i -> bg.primaryConstructor?.call(i)}.filterNotNull()
				}
			val values:List<BGM> get() = all.flatten()
			val listStr:List<String> get() = values.map {it.fullName}
			val count:Int get() = values.count {!it.hidden}
			/** 音楽のMaximumcount */
			val countAll:Int get() = values.size
			/** 選択できない音楽のcount */
			val countUnselectable:Int get() = values.count {it.hidden}
			val countCategory:Int get() = values.size
		}
	}

	/** Constructor */
	constructor() {
		reset()

	}

	/** Copy constructor
	 * @param b Copy source
	 */
	constructor(b:BGMStatus) {
		copy(b)
	}

	/** Reset to defaults */
	fun reset() {
		bgm = BGM.SILENT
		volume = 1f
		fadesw = false
	}

	/** 他のBGMStatusからコピー
	 * @param b Copy source
	 */
	fun copy(b:BGMStatus) {
		bgm = b.bgm
		volume = b.volume
		fadesw = b.fadesw
	}

	/** BGM fade状態と音量の更新 */
	fun fadeUpdate() {
		if(fadesw) {
			if(volume>0f)
				volume -= fadespd
			else if(volume<0f) volume = 0f
		} else if(volume!=1f) volume = 1f
	}

	companion object {
		/** Serial version ID */
		private const val serialVersionUID = -1003092972570497408L

	}
}
