/*
 * Copyright (c) 2010-2023, NullNoname
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
package mu.nu.nullpo.game.component

import java.io.Serializable
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.primaryConstructor

/** 音楽の再生状況を管理するクラス */
//@kotlinx.serialization.Serializable
class BGMStatus {
	/** Current BGM */
	var bgm:BGM = BGM.Silent
	var track = 0

	/** 音量 (1f=100%, .5f=50%) */
	var volume:Float = 1f; private set

	/** BGM fadeoutスイッチ */
	var fadeSW:Boolean
		get() = fadeSpd>0f
		set(sw) {
			fadeSpd = if(sw) .01f else 0f
		}

	/** BGM fadeout速度 */
	var fadeSpd = 0f

	/** 音楽の定数 */
	sealed class BGM(
		val id:Int, idx:Int = 0, val hidden:Boolean = false, nums:Int = 1, ln:String = "",
		sn:List<String> = emptyList()
	) {
		constructor(id:Int, idx:Int, nums:Int, ln:String):this(id, idx, false, nums, ln)
		constructor(id:Int, idx:Int, ln:String, hidden:Boolean = false, sn:List<String>):this(id, idx, hidden, sn.size, ln, sn)
		constructor(id:Int, idx:Int, ln:String, vararg sn:String = emptyArray(), hidden:Boolean = false)
			:this(id, idx, hidden, sn.size, ln, sn.toList())

		//		val id:Int = BGM::class.java.declaredClasses.indexOfFirst {it==this::class.java}
		val idx:Int = minOf(maxOf(0, idx), nums-1)
		val nums = maxOf(1, nums, sn.size)

		val name = this::class.simpleName ?: ""
		val longName:String = ln.ifEmpty {name}
		val subName:String = if(sn.isEmpty()) "" else sn[maxOf(minOf(this.idx, minOf(sn.size, nums)-1), 0)]
		val drawName = "#$id-${this.idx} ${name.replace('_', ' ')}"
		val fullName = "$longName $subName"
		//var filename:List<String> = List(maxOf(1, nums)) {""}

		override fun equals(other:Any?):Boolean =
			super.equals(other)||if(other is BGM) id==other.id&&idx==other.idx else false

		operator fun compareTo(o:BGM):Int = if(id==o.id) idx-o.idx else id-o.id
		override fun hashCode():Int {
			var result = hidden.hashCode()
			result = 31*result+id
			result = 31*result+idx
			result = 31*result+nums
			result = 31*result+name.hashCode()
			result = 31*result+longName.hashCode()
			result = 31*result+subName.hashCode()
			result = 31*result+drawName.hashCode()
			result = 31*result+fullName.hashCode()
			return result
		}

		override fun toString():String = fullName

		object Silent:BGM(0, ln = "Silent")
		class Generic(idx:Int = 0):BGM(1, idx, "Guidelines Modes", sn = List(10) {"Level:${it+1}"})
		class Rush(idx:Int = 0):BGM(2, idx, "Trial Rush", sn = List(4) {"Level:${it+1}"})
		class Extra(idx:Int = 0):BGM(3, idx, 5, "Extra Modes")
		class Puzzle(idx:Int = 0):BGM(4, idx, "Strategy Mode/Grand Blossom", "SAKURA", "SAKURA", "TOMOYO", "CELBERUS", "EXTRA")
		class RetroN(idx:Int = 0):BGM(5, idx, 4, "Retro Classic:N.")
		class RetroA(idx:Int = 0):BGM(6, idx, 5, "Retro Marathon:AT")
		class RetroS(idx:Int = 0):BGM(7, idx, 6, "Retro Mania:S")

		class GrandM(idx:Int = 0):BGM(8, idx, "Grand Marathon", "Lv 0", "Lv 500")
		class GrandA(idx:Int = 0):BGM(
			9, idx, "Grand Mania", "Lv 0", "Lv200", "Lv500", "Lv500 mRoll/SLv0",
			"Lv700/SLv300", "Lv700 with mRoll/SLv500", "Lv900/SLv800"
		)

		class GrandT(idx:Int = 0):BGM(
			10, idx,
			"Grand Mastery", "NORMAL", "rank 200", "rank 500", "rank 800 on NORMAL",
			"rank 800 on Lv400-500", "rank 1000 on NORMAL", "rank 1000 on Lv500-700", "Lv900 mRoll"
		)

		class GrandTS(idx:Int = 0):BGM(
			11, idx,
			"Grand Lightning", "LLv0", "LLv500", "LLv700", "LLv 1000"
		)

		class Menu(idx:Int = 0):BGM(
			12, idx,
			"Select BGM", "Title Menu/Replay", "Mode Select", "General Config",
			"Mode Config(Retro/Puzzle)", "Mode Config(Generic)",
			"Mode Config(Unique)", "Mode Config(Trial)", "Mode Config(Grand 20G)", hidden = true
		)

		class Ending(idx:Int = 0):BGM(
			13, idx, "Ending Challenge",
			"Marathon", "Mania (60sec)", "Mastery (55sec)", "Modern (200Sec)", "Modern-Hard (200Sec)",
			hidden = true
		)

		class Result(idx:Int = 0):BGM(
			14, idx, "Play Result",
			"Failure", "Done Sprint", "Done Enduro", "Cleared Game", hidden = true
		)

		class Finale(idx:Int = 0):BGM(15, idx, "Grand Finale", "Genuine", "Joker", "Further", hidden = true)
		class Blitz(idx:Int = 0):BGM(16, idx, "Blitz", "3-min", "5-min", "3-min EXTREME", "5-min EXTREME", hidden = true)

		//operator fun get(index: Int): BGM = if(this.idx)
		companion object {
			val all:List<List<BGM>>
				get() = BGM::class.sealedSubclasses.map {bg ->
					bg.objectInstance?.let {listOf(it)}
						?: List(bg.createInstance().nums) {i -> bg.primaryConstructor?.call(i)}.filterNotNull()
				}.filter {it.isNotEmpty()}.sortedBy {it.first().id}
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
		replace(b)
	}

	/** Reset to defaults */
	fun reset() {
		bgm = BGM.Silent
		volume = 1f
		fadeSW = false
	}

	/** 設定を[b]からコピー */
	fun replace(b:BGMStatus) {
		bgm = b.bgm
		volume = b.volume
		fadeSW = b.fadeSW
	}

	/** BGM fade状態と音量の更新 */
	fun fadeUpdate() {
		if(fadeSW) {
			if(volume>0f) volume -= fadeSpd
			else if(volume<0f) volume = 0f
		} else if(volume!=1f) volume = 1f
	}
}
