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
import kotlin.reflect.full.isSubclassOf

/** 音楽の再生状況を管理するクラス */
class BGMStatus:Serializable {

	/** Current BGM */
	var bgm:BGM = BGM.SILENT

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
	sealed class BGM(val nums:Int = 1, ln:String = "", vararg sn:String) {
		constructor(ln:String = "", vararg sn:String):this(1, ln, *sn)

		open val idx:Int = 0

		val id:Int get() = BGM::class.java.declaredClasses.indexOfFirst { it == this::class.java }

		val longName:String = if(ln.isEmpty()) BGM::class.simpleName ?: "" else ln
		val subName = sn.toList()
		fun fullName(idx:Int) = longName+subName[idx]

		val name = this::class.simpleName?:""

		//	override fun toString():String = name.replace('_', ' ')

		var filename:Array<String> = Array(maxOf(1, nums)) {""}

		operator fun compareTo(o:BGM):Int = if(id==o.id) idx-o.idx else id-o.id

		object SILENT:BGM("Silent")
		data class GENERIC(override val idx:Int = 0):BGM(6, "Guidelines Modes", *Array(6) {"Level:${it+1}"})
		data class RUSH(override val idx:Int = 0):BGM(3, "TimeAttack Rush", *Array(3) {"Level:${it+1}"})
		data class EXTRA(override val idx:Int = 0):BGM(3, "Extra Modes")
		data class RETRO_N(override val idx:Int = 0):BGM(3, "Retro Classic:N.")
		object RETRO_A:BGM("Retro Marathon:TG")
		data class RETRO_S(override val idx:Int = 0):BGM(3, "Retro Mania:S","Marathon","Deadlock","Modern")
		data class PUZZLE(override val idx:Int = 0):BGM("Grand Blossom", "SAKURA", "TOMOYO", "CELBERUS")
		data class GM_1(override val idx:Int = 0):BGM(2, "Grand Marathon", "NORMAL", "20G")
		data class GM_2(override val idx:Int = 0):BGM(4, "Grand Mania", "NORMAL", "20G 500", "Storm 300/700", "Storm 500/900")
		data class GM_3(override val idx:Int = 0):BGM(6, "Grand Mastery",
			"NORMAL", "20G", "Blitz", "Blitz 500", "Lightning 700", "Lightning 1k")

		data class MENU(override val idx:Int = 0):BGM(3, "Menu Screen BGM","Mode Select","General Config", "Mode Config")
		data class ENDING(override val idx:Int = 0):BGM(4, "Ending Challenge","Marathon","Mania (60sec)","Mastery (55sec)","Modern (200Sec)")
		data class RESULT(override val idx:Int = 0):BGM(4, "Play Result", "Failure", "Done Sprint", "Done Enduro", "Cleared Game")
		data class FINALE(override val idx:Int = 0):BGM(3,"Grand Finale","Genuine","Joker","Further")

		//operator fun get(index: Int): BGM = if(this.idx)
		companion object {
			val GENERIC_1 = GENERIC(0)
			val GENERIC_2 = GENERIC(1)
			val GENERIC_3 = GENERIC(2)
			val GENERIC_4 = GENERIC(3)
			val GENERIC_5 = GENERIC(4)
			val GENERIC_6 = GENERIC(5)
			val RUSH_1 = RUSH(0)
			val RUSH_2 = RUSH(1)
			val RUSH_3 = RUSH(2)
			val EXTRA_1 = EXTRA(0)
			val EXTRA_2 = EXTRA(1)
			val EXTRA_3 = EXTRA(2)
			val PUZZLE_1 = PUZZLE(0)
			val PUZZLE_2 = PUZZLE(1)
			val PUZZLE_3 = PUZZLE(2)
			val GM_20G_1 = GM_1(1)
			val GM_20G_2 = GM_2(1)
			val GM_20G_3 = GM_3(1)
			val STORM_1 = GM_2(2)
			val STORM_2 = GM_2(3)
			val BLITZ_1 = GM_3(2)
			val BLITZ_2 = GM_3(3)
			val BLITZ_3 = GM_3(4)
			val BLITZ_4 = GM_3(5)
			val FAILED = RESULT(0)
			val RESULT_1 = RESULT(1)
			val RESULT_2 = RESULT(2)
			val CLEARED = RESULT(3)
			val MENU_1=MENU(0)
			val MENU_2=MENU(1)
			val MENU_3=MENU(2)
			val ENDING_1=ENDING(0)
			val ENDING_2=ENDING(1)
			val ENDING_3=ENDING(2)
			val ENDING_4=ENDING(3)
			val FINALE_1=FINALE(0)
			val FINALE_2=FINALE(1)
			val FINALE_3=FINALE(2)

			@JvmStatic val map = BGM::class.nestedClasses
				.filter {it.isSubclassOf(BGM::class) }
				.map {it.objectInstance }
				.filterIsInstance<BGM>()
				.associateBy { it.name }

			@JvmStatic fun valueOf(value:String) = requireNotNull(map[value]) {
				"No enum constant ${BGM::class.java.name}.$value"
			}

			@JvmStatic fun values() = map.values.toTypedArray()
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
		/** 音楽のMaximumcount */
		private val BGM_TOTAL:Int get() = BGM.values().size
		/** 選択できない音楽のcount */
		val BGM_UNSELECTABLE:Int get() = BGM.values().count {it.nums<=0}

		val count:Int get() = BGM.values().count {it.nums>0}

		@Deprecated("", ReplaceWith("get(no).id", "mu.nu.nullpo.game.component.BGMStatus.Companion.count", "mu.nu.nullpo.game.component.BGMStatus.Companion.count"))
		fun getCount(no:Int):Int = if(no<=count) no else count-no

		operator fun get(no:Int):BGM {
			var i = no
			if(i<0) i = count-i
			if(i>=BGM.values().size) i = 0
			return BGM.values()[i]
		}

		@Deprecated("", ReplaceWith("get(no).id", "mu.nu.nullpo.game.component.BGMStatus.Companion.count"))
		fun getInt(no:Int):Int = if(no<0) count-no else no
	}
}
