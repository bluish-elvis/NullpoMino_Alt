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

import kotlin.collections.List
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.primaryConstructor

/** 音楽の定数
 * @param _idx index of Sub Track
 * @param _num number of Sub Tracks
 * @param hidden -flag for excluding from selection */
sealed class BGM(
	val id:Int, _idx:Int = 0, val hidden:Boolean = false, _num:Int = 1, _long:String = "",
	sn:List<String> = emptyList()
) {
	constructor(id:Int, idx:Int, _num:Int, ln:String):this(id, idx, false, _num, ln)
	/** @param sn list of Sub Track Names */
	constructor(id:Int, idx:Int, ln:String, hidden:Boolean = false, sn:List<String>):this(id, idx, hidden, sn.size, ln, sn)
	/** @param sn Sub Track Names */
	constructor(id:Int, idx:Int, ln:String, vararg sn:String = emptyArray(), hidden:Boolean = false)
		:this(id, idx, hidden, sn.size, ln, sn.toList())

	//		val id:Int = BGM::class.java.declaredClasses.indexOfFirst {it==this::class.java}
	val idx:Int = (_num-1).coerceIn(0, maxOf(0, _idx))
	val nums = maxOf(1, _num, sn.size)

	val name = this::class.simpleName ?: ""
	val longName:String = _long.ifEmpty {name}
	val subName:String = if(sn.isEmpty()) "" else sn[maxOf(minOf(idx, minOf(sn.size, _num)-1), 0)]
	val drawName = "#$id-$idx ${name.replace('_', ' ')}"
	val fullName = "$longName $subName"
	//var filename:List<String> = List(maxOf(1, _num)) {""}

	override fun equals(other:Any?):Boolean =
		super.equals(other)||if(other is BGM) id==other.id&&idx==other.idx else false

	operator fun compareTo(o:BGM):Int = if(id==o.id) idx-o.idx else id-o.id
	override fun hashCode():Int = hidden.hashCode().let {31*it+id}
		.let {31*it+idx}
		.let {31*it+nums}
		.let {31*it+name.hashCode()}
		.let {31*it+longName.hashCode()}
		.let {31*it+subName.hashCode()}
		.let {31*it+drawName.hashCode()}
		.let {31*it+fullName.hashCode()}

	override fun toString():String = fullName

	object Silent:BGM(0, _long = "Silent")
	class Generic(idx:Int = 0):BGM(1, idx, "Guidelines Modes", sn = List(7) {"Level:${it+1}"})
	class Rush(idx:Int = 0):BGM(2, idx, "Trial Rush", sn = List(5) {"Level:${it+1}"})
	class Puzzle(idx:Int = 0):BGM(3, idx, "Strategy Mode/Grand Blossom", "SAKURA", "TOMOYO", "CELBERUS", "KONOHA")
	class Zen(idx:Int = 0):BGM(4, idx,  "Zen/Low Speeds Modes", sn = List(7) {"Level:${it+1}"})
	class Extra(idx:Int = 0):BGM(5, idx, 3, "Extra Modes")

	class RetroN(idx:Int = 0):BGM(6, idx, 4, "Retro Classic:N.")
	class RetroA(idx:Int = 0):BGM(7, idx, 5, "Retro Marathon:AT")
	class RetroS(idx:Int = 0):BGM(8, idx, 6, "Retro Mania:S")

	class GrandM(idx:Int = 0):BGM(9, idx, "Grand Marathon", "Lv 0", "Lv 500")
	class GrandA(idx:Int = 0):BGM(
		10, idx, "Grand Mania", "Lv 0", "Lv200", "Lv500", "Lv500 mRoll/SLv0",
		"Lv700/SLv300", "Lv700 with mRoll/SLv500", "Lv900/SLv800"
	)

	class GrandT(idx:Int = 0):BGM(
		11, idx,
		"Grand Mastery", "NORMAL", "rank 200", "rank 500", "rank 800 on NORMAL",
		"rank 800 on Lv400-500", "rank 1000 on NORMAL", "rank 1000 on Lv500-700", "Lv900 mRoll"
	)

	class GrandTS(idx:Int = 0):BGM(
		12, idx,
		"Grand Lightning", "LLv0", "LLv500", "LLv700", "LLv 1000"
	)

	class Menu(idx:Int = 0):BGM(
		13, idx,
		"Select BGM", "Title Menu/Replay", "Mode Select", "General Config",
		"Mode Config(Retro/Puzzle)", "Mode Config(Generic)",
		"Mode Config(Unique)", "Mode Config(Trial)", "Mode Config(Grand 20G)", hidden = true
	)

	class Ending(idx:Int = 0):BGM(
		14, idx, "Ending Challenge",
		"Marathon", "Mania (60sec)", "Mastery (55sec)", "Modern-Easy (200Sec)","Modern-Medium (200Sec)", "Modern-Hard " +
			"(200Sec)",
		hidden = true
	)

	class Result(idx:Int = 0):BGM(
		15, idx, "Play Result",
		"Failure", "Done Sprint", "Done Enduro", "Cleared Game", hidden = true
	)

	class Finale(idx:Int = 0):BGM(16, idx, "Grand Finale", "Genuine", "Joker", "Further", hidden = true)
	class Blitz(idx:Int = 0):BGM(17, idx, "Blitz", "3-min", "5-min", "3-min EXTREME", "5-min EXTREME", hidden = true)

	//operator fun get(index: Int): BGM = if(this._idx)
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
