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

import mu.nu.nullpo.util.CustomProperties
import java.io.Serializable

/** Scoreなどの情報 */
class Statistics:Serializable {

	/** Total score */
	val score:Int get() = scoreLine+scoreHD+scoreSD+scoreBonus
	/** Line clear score */
	var scoreLine:Int = 0
	/** Soft drop score */
	var scoreSD:Int = 0
	/** Hard drop score */
	var scoreHD:Int = 0
	/** その他の方法で手に入れたScore */
	var scoreBonus:Int = 0

	/** Total line count */
	var lines:Int = 0

	/** Cleared Garbage Lines */
	var garbageLines:Int = 0
	/** Total Cleared block count */
	var blocks:Int = 0

	/** 経過 time */
	var time:Int = 0

	/** Level */
	var level:Int = 0

	/** Levelの表示に加算する数値 (表示 levelが内部の値と異なる場合に使用) */
	var levelDispAdd:Int = 0

	/** 置いたピースのcount */
	var totalPieceLocked:Int = 0

	/** ピースを操作していた合計 time */
	var totalPieceActiveTime:Int = 0

	/** ピースを移動させた合計 count */
	var totalPieceMove:Int = 0

	/** ピースをrotationさせた合計 count */
	var totalPieceRotate:Int = 0

	/** 1-line clear count */
	var totalSingle:Int = 0
	/** 2-line clear count */
	var totalDouble:Int = 0
	/** Split 2-line clear count */
	var totalSplitDouble:Int = 0
	/** 3-line clear count */
	var totalTriple:Int = 0
	/** One-Two-3-line clear count */
	var totalSplitTriple:Int = 0
	/** 4-line clear count */
	var totalQuadruple:Int = 0

	/** Twister but no-line (with wallkick) count */
	var totalTwistZeroMini:Int = 0
	/** Twister but no-line (without wallkick) count */
	var totalTwistZero:Int = 0
	/** Twister 1-line (with wallkick) count */
	var totalTwistSingleMini:Int = 0
	/** Twister 1-line (without wallkick) count */
	var totalTwistSingle:Int = 0
	/** Twister 2-line (with wallkick) count */
	var totalTwistDoubleMini:Int = 0
	/** Twister 2-line (without wallkick) count */
	var totalTwistDouble:Int = 0
	/** Twister Split 2-line count */
	var totalTwistSplitDouble:Int = 0
	/** Twister 3 line count */
	var totalTwistTriple:Int = 0
	/** Twister One-Two-3-line count */
	var totalTwistSplitTriple:Int = 0

	/** Back to Back 4-line clear count */
	var totalB2BQuad:Int = 0
	/** Back to Back Split clear count */
	var totalB2BSplit:Int = 0
	/** Back to Back Twister clear count */
	var totalB2BTwist:Int = 0

	/** Hold use count */
	var totalHoldUsed:Int = 0

	/** Longest combo */
	var maxCombo:Int = 0
	/** Longest Back to Back Chain */
	var maxB2B:Int = 0

	/** 1Linesあたりの得点 (Score Per Line) */
	val spl:Double get() = if(lines>0) score.toDouble()/lines.toDouble() else 0.0
	/** 1分間あたりの得点 (Score Per Minute) */
	val spm:Double get() = if(time>0) score*3600.0/time else 0.0
	/** 1秒間あたりの得点 (Score Per Second) */
	val sps:Double get() = if(time>0) score*60.0/time else 0.0

	/** 1分間あたりのLinescount (Lines Per Minute) */
	val lpm:Float get() = if(time>0) lines*3600f/time else 0f
	/** 1秒間あたりのLinescount (Lines Per Second) */
	val lps:Float get() = if(time>0) lines*60f/time else 0f
	/** 1分間あたりのピースcount (Pieces Per Minute) */
	val ppm:Float get() = if(time>0) totalPieceLocked*3600f/time else 0f
	/** 1秒間あたりのピースcount (Pieces Per Second) */
	val pps:Float get() = if(time>0) totalPieceLocked*60f/time else 0f

	/** TAS detection: slowdown rate */
	var gamerate:Float = 0f
	/** Longest Avalanche-chain */
	var maxChain:Int = 0

	/** Roll cleared flag (0=Not Reached 1=Reached 2=Fully Survived) */
	var rollclear:Int = 0
		set(it) {
			if(field==it || !(0..2).contains(it)) return
			when(it) {
				1 -> rollReached++
				2 -> {
					if(field<1) rollReached++
					rollSurvived++
				}
			}
			field = it
			rollclearHistory = rollclearHistory.plus(it).takeLast(historyMax).toIntArray()
		}
	/** Roll cleared history */
	var rollclearHistory:IntArray = IntArray(historyMax)

	/** Roll Reached Count */
	var rollReached:Int = 0
	/** Roll Survived Count */
	var rollSurvived:Int = 0

	var pieces:IntArray = IntArray(Piece.PIECE_COUNT)

	/** Constructor */
	constructor() {
		reset()
	}

	/** Copy constructor
	 * @param s Copy source
	 */
	constructor(s:Statistics?) {
		copy(s)
	}

	/** Constructor that imports data from a String Array
	 * @param s String Array (String[37])
	 */
	constructor(s:Array<String>) {
		importStringArray(s)
	}

	/** Constructor that imports data from a String
	 * @param s String (Split by ;)
	 */
	constructor(s:String) {
		importString(s)
	}

	fun scoreReset() {
		scoreLine = 0
		scoreSD = 0
		scoreHD = 0
		scoreBonus = 0
	}

	/** Reset to defaults */
	fun reset() {
		scoreReset()
		lines = 0
		blocks = 0
		time = 0
		level = 0
		levelDispAdd = 0
		totalPieceLocked = 0
		totalPieceActiveTime = 0
		totalPieceMove = 0
		totalPieceRotate = 0
		totalSingle = 0
		totalDouble = 0
		totalSplitDouble = 0
		totalTriple = 0
		totalSplitTriple = 0
		totalQuadruple = 0
		totalTwistZeroMini = 0
		totalTwistZero = 0
		totalTwistSingleMini = 0
		totalTwistSingle = 0
		totalTwistDoubleMini = 0
		totalTwistDouble = 0
		totalTwistSplitDouble = 0
		totalTwistTriple = 0
		totalTwistSplitTriple = 0
		totalB2BQuad = 0
		totalB2BSplit = 0
		totalB2BTwist = 0
		totalHoldUsed = 0
		maxCombo = 0
		gamerate = 0f
		maxChain = 0
		rollclear = 0

		pieces = IntArray(Piece.PIECE_COUNT)
	}

	/** 他のStatisticsの値をコピー
	 * @param s Copy source
	 */
	fun copy(s:Statistics?) {
		s?.let {b ->
			scoreLine = b.scoreLine
			scoreSD = b.scoreSD
			scoreHD = b.scoreHD
			scoreBonus = b.scoreBonus
			lines = b.lines
			blocks = b.blocks
			time = b.time
			level = b.level
			levelDispAdd = b.levelDispAdd
			totalPieceLocked = b.totalPieceLocked
			totalPieceActiveTime = b.totalPieceActiveTime
			totalPieceMove = b.totalPieceMove
			totalPieceRotate = b.totalPieceRotate
			totalHoldUsed = b.totalHoldUsed
			totalSingle = b.totalSingle
			totalDouble = b.totalDouble
			totalSplitDouble = b.totalSplitDouble
			totalTriple = b.totalTriple
			totalSplitTriple = b.totalSplitTriple
			totalQuadruple = b.totalQuadruple
			totalTwistZeroMini = b.totalTwistZeroMini
			totalTwistZero = b.totalTwistZero
			totalTwistSingleMini = b.totalTwistSingleMini
			totalTwistSingle = b.totalTwistSingle
			totalTwistDoubleMini = b.totalTwistDoubleMini
			totalTwistDouble = b.totalTwistDouble
			totalTwistSplitDouble = b.totalTwistSplitDouble
			totalTwistTriple = b.totalTwistTriple
			totalTwistSplitTriple = b.totalTwistSplitTriple
			totalB2BQuad = b.totalB2BQuad
			totalB2BSplit = b.totalB2BSplit
			totalB2BTwist = b.totalB2BTwist
			maxCombo = b.maxCombo
			maxB2B = b.maxB2B
			gamerate = b.gamerate
			maxChain = b.maxChain
			rollclear = b.rollclear
			rollclearHistory=
				b.rollclearHistory
			garbageLines = b.garbageLines

			pieces = b.pieces
		} ?: reset()
	}

	/** 他のStatisticsの値を合成
	 * @param s Copy source
	 */
	fun combine(s:Statistics?) {
		s?.let {b ->
			scoreLine += b.scoreLine
			scoreSD += b.scoreSD
			scoreHD += b.scoreHD
			scoreBonus += b.scoreBonus
			lines += b.lines
			blocks += b.blocks
			time += b.time
			level += b.level
			levelDispAdd += b.levelDispAdd
			totalPieceLocked += b.totalPieceLocked
			totalPieceActiveTime += b.totalPieceActiveTime
			totalPieceMove += b.totalPieceMove
			totalPieceRotate += b.totalPieceRotate
			totalHoldUsed += b.totalHoldUsed
			totalSingle += b.totalSingle
			totalDouble += b.totalDouble
			totalSplitDouble += b.totalSplitDouble
			totalTriple += b.totalTriple
			totalSplitTriple += b.totalSplitTriple
			totalQuadruple += b.totalQuadruple
			totalTwistZeroMini += b.totalTwistZeroMini
			totalTwistZero += b.totalTwistZero
			totalTwistSingleMini += b.totalTwistSingleMini
			totalTwistSingle += b.totalTwistSingle
			totalTwistDoubleMini += b.totalTwistDoubleMini
			totalTwistDouble += b.totalTwistDouble
			totalTwistSplitDouble += b.totalTwistSplitDouble
			totalTwistTriple += b.totalTwistTriple
			totalTwistSplitTriple += b.totalTwistSplitTriple
			totalB2BQuad += b.totalB2BQuad
			totalB2BSplit += b.totalB2BSplit
			totalB2BTwist += b.totalB2BTwist
			maxCombo = maxOf(maxCombo, b.maxCombo)
			maxB2B = maxOf(maxB2B, b.maxB2B)
			gamerate = (gamerate+b.gamerate)/2f
			maxChain = maxOf(maxCombo, b.maxChain)
			rollclear = b.rollclear
			garbageLines += b.garbageLines

			pieces = pieces.mapIndexed {it, i -> it+b.pieces[i]}.toIntArray()
		}
	}

	/** プロパティセットに保存
	 * @param p プロパティセット
	 * @param id 任意のID (Player IDなど）
	 */
	fun writeProperty(p:CustomProperties, id:Int) =
		mapOf<String, Comparable<*>>("$id.statistics.scoreFromLineClear" to scoreLine,
			"$id.statistics.scoreSD" to scoreSD,
			"$id.statistics.scoreHD" to scoreHD,
			"$id.statistics.scoreBonus" to scoreBonus,
			"$id.statistics.lines" to lines,
			"$id.statistics.blocks" to blocks,
			"$id.statistics.time" to time,
			"$id.statistics.level" to level,
			"$id.statistics.levelDispAdd" to levelDispAdd,
			"$id.statistics.totalPieceLocked" to totalPieceLocked,
			"$id.statistics.totalPieceActiveTime" to totalPieceActiveTime,
			"$id.statistics.totalPieceMove" to totalPieceMove,
			"$id.statistics.totalPieceRotate" to totalPieceRotate,
			"$id.statistics.totalSingle" to totalSingle,
			"$id.statistics.totalDouble" to totalDouble,
			"$id.statistics.totalSplitDouble" to totalSplitDouble,
			"$id.statistics.totalTriple" to totalTriple,
			"$id.statistics.totalSplitTriple" to totalSplitTriple,
			"$id.statistics.totalFour" to totalQuadruple,
			"$id.statistics.totalTwistZeroMini" to totalTwistZeroMini,
			"$id.statistics.totalTwistZero" to totalTwistZero,
			"$id.statistics.totalTwistSingleMini" to totalTwistSingleMini,
			"$id.statistics.totalTwistSingle" to totalTwistSingle,
			"$id.statistics.totalTwistDoubleMini" to totalTwistDoubleMini,
			"$id.statistics.totalTwistDouble" to totalTwistDouble,
			"$id.statistics.totalTwistSplitDouble" to totalTwistSplitDouble,
			"$id.statistics.totalTwistTriple" to totalTwistTriple,
			"$id.statistics.totalTwistSplitTriple" to totalTwistSplitTriple,
			"$id.statistics.totalB2BFour" to totalB2BQuad,
			"$id.statistics.totalB2BSplit" to totalB2BSplit,
			"$id.statistics.totalB2BTwist" to totalB2BTwist,
			"$id.statistics.totalHoldUsed" to totalHoldUsed,
			"$id.statistics.maxCombo" to maxCombo,
			"$id.statistics.maxB2B" to maxB2B,
			"$id.statistics.gamerate" to gamerate,
			"$id.statistics.maxChain" to maxChain,
			"$id.statistics.rollclear" to rollclear).plus((0 until pieces.size-1).associate {
			"$id.statistics.pieces.$it" to pieces[it]
		}).forEach {(key, it) ->
			p.setProperty(key, it)
		}

	/** プロパティセットから読み込み
	 * @param p プロパティセット
	 * @param id 任意のID (Player IDなど）
	 */
	fun readProperty(p:CustomProperties, id:Int) {
		scoreLine = p.getProperty("$id.statistics.scoreLine", 0)
		scoreSD = p.getProperty("$id.statistics.scoreSD", 0)
		scoreHD = p.getProperty("$id.statistics.scoreHD", 0)
		scoreBonus = p.getProperty("$id.statistics.scoreBonus", 0)
		lines = p.getProperty("$id.statistics.lines", 0)
		lines = p.getProperty("$id.statistics.blocks", 0)
		time = p.getProperty("$id.statistics.time", 0)
		level = p.getProperty("$id.statistics.level", 0)
		levelDispAdd = p.getProperty("$id.statistics.levelDispAdd", 0)
		totalPieceLocked = p.getProperty("$id.statistics.totalPieceLocked", 0)
		totalPieceActiveTime = p.getProperty("$id.statistics.totalPieceActiveTime", 0)
		totalPieceMove = p.getProperty("$id.statistics.totalPieceMove", 0)
		totalPieceRotate = p.getProperty("$id.statistics.totalPieceRotate", 0)
		totalSingle = p.getProperty("$id.statistics.totalSingle", 0)
		totalDouble = p.getProperty("$id.statistics.totalDouble", 0)
		totalSplitDouble = p.getProperty("$id.statistics.totalSplitDouble", 0)
		totalTriple = p.getProperty("$id.statistics.totalTriple", 0)
		totalSplitTriple = p.getProperty("$id.statistics.totalSplitTriple", 0)
		totalQuadruple = p.getProperty("$id.statistics.totalFour", 0)
		totalTwistZeroMini = p.getProperty("$id.statistics.totalTwistZeroMini", 0)
		totalTwistZero = p.getProperty("$id.statistics.totalTwistZero", 0)
		totalTwistSingleMini = p.getProperty("$id.statistics.totalTwistSingleMini", 0)
		totalTwistSingle = p.getProperty("$id.statistics.totalTwistSingle", 0)
		totalTwistDoubleMini = p.getProperty("$id.statistics.totalTwistDoubleMini", 0)
		totalTwistDouble = p.getProperty("$id.statistics.totalTwistDouble", 0)
		totalTwistSplitDouble = p.getProperty("$id.statistics.totalTwistSplitDouble", 0)
		totalTwistTriple = p.getProperty("$id.statistics.totalTwistSplitTriple", 0)
		totalTwistSplitTriple = p.getProperty("$id.statistics.totalTwistTriple", 0)
		totalB2BQuad = p.getProperty("$id.statistics.totalB2BFour", 0)
		totalB2BSplit = p.getProperty("$id.statistics.totalB2BSplit", 0)
		totalB2BTwist = p.getProperty("$id.statistics.totalB2BTwist", 0)
		totalHoldUsed = p.getProperty("$id.statistics.totalHoldUsed", 0)
		maxCombo = p.getProperty("$id.statistics.maxCombo", 0)
		maxB2B = p.getProperty("$id.statistics.maxB2B", 0)
		gamerate = p.getProperty("$id.statistics.gamerate", 0f)
		maxChain = p.getProperty("$id.statistics.maxChain", 0)
		rollclear = p.getProperty("$id.statistics.rollclear", 0)
		for(i in 0 until pieces.size-1)
			pieces[i] = p.getProperty("$id.statistics.pieces.$i", 0)
	}

	/** Import from String Array
	 * @param s String Array (String[42])
	 */
	fun importStringArray(s:Array<String>) = listOf<(String)->Unit>(
		{scoreLine = Integer.parseInt(it)},
		{scoreSD = Integer.parseInt(it)},
		{scoreHD = Integer.parseInt(it)},
		{scoreBonus = Integer.parseInt(it)},
		{lines = Integer.parseInt(it)},
		{blocks = Integer.parseInt(it)},
		{time = Integer.parseInt(it)},
		{level = Integer.parseInt(it)},
		{levelDispAdd = Integer.parseInt(it)},
		{totalPieceLocked = Integer.parseInt(it)},
		{totalPieceActiveTime = Integer.parseInt(it)},
		{totalPieceMove = Integer.parseInt(it)},
		{totalPieceRotate = Integer.parseInt(it)},
		{totalSingle = Integer.parseInt(it)},
		{totalDouble = Integer.parseInt(it)},
		{totalSplitDouble = Integer.parseInt(it)},
		{totalTriple = Integer.parseInt(it)},
		{totalSplitTriple = Integer.parseInt(it)},
		{totalQuadruple = Integer.parseInt(it)},
		{totalTwistZeroMini = Integer.parseInt(it)},
		{totalTwistZero = Integer.parseInt(it)},
		{totalTwistSingleMini = Integer.parseInt(it)},
		{totalTwistSingle = Integer.parseInt(it)},
		{totalTwistDoubleMini = Integer.parseInt(it)},
		{totalTwistDouble = Integer.parseInt(it)},
		{totalTwistSplitDouble = Integer.parseInt(it)},
		{totalTwistTriple = Integer.parseInt(it)},
		{totalTwistSplitTriple = Integer.parseInt(it)},
		{totalB2BQuad = Integer.parseInt(it)},
		{totalB2BSplit = Integer.parseInt(it)},
		{totalB2BTwist = Integer.parseInt(it)},
		{totalHoldUsed = Integer.parseInt(it)},
		{maxCombo = Integer.parseInt(it)},
		{maxB2B = Integer.parseInt(it)},
		{gamerate = java.lang.Float.parseFloat(it)},
		{maxChain = Integer.parseInt(it)},
		{rollclear = Integer.parseInt(it)}).plus(
		(0 until pieces.size-1).map {i:Int ->
			{it:String -> pieces[i] = Integer.parseInt(it)}
		}).zip(s).forEach {(m, st) -> m(st)}

	/** Import from String
	 * @param s String (Split by ;)
	 */
	fun importString(s:String) {
		importStringArray(s.split(";".toRegex()).dropLastWhile {it.isEmpty()}.toTypedArray())
	}

	/** Export to String Array
	 * @return String Array (String[38])
	 */
	fun exportStringArray():Array<String> = arrayOf(
		"$scoreLine"
		, "$scoreSD"
		, "$scoreHD"
		, "$scoreBonus"
		, "$lines"
		, "$blocks"
		, "$time"
		, "$level"
		, "$levelDispAdd"
		, "$totalPieceLocked"
		, "$totalPieceActiveTime"
		, "$totalPieceMove"
		, "$totalPieceRotate"
		, "$totalSingle"
		, "$totalDouble"
		, "$totalSplitDouble"
		, "$totalTriple"
		, "$totalSplitTriple"
		, "$totalQuadruple"
		, "$totalTwistZeroMini"
		, "$totalTwistZero"
		, "$totalTwistSingleMini"
		, "$totalTwistSingle"
		, "$totalTwistDoubleMini"
		, "$totalTwistDouble"
		, "$totalTwistSplitDouble"
		, "$totalTwistTriple"
		, "$totalTwistSplitTriple"
		, "$totalB2BQuad"
		, "$totalB2BSplit"
		, "$totalB2BTwist"
		, "$totalHoldUsed"
		, "$maxCombo"
		, "$maxB2B"
		, "$gamerate"
		, "$maxChain"
		, "$rollclear")+(pieces.map {"$it"})

	/** Export to String
	 * @return String (Split by ;)
	 */
	fun exportString():String {
		val array = exportStringArray()
		val result = StringBuilder()
		for(i in array.indices) {
			if(i>0) result.append(";")
			result.append(array[i])
		}
		return "$result"
	}

	companion object {
		/** Serial version ID */
		private const val serialVersionUID = -499640168205398295L
		const val historyMax = 100
	}
}

