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
	var score:Int = 0

	/** Line clear score */
	var scoreFromLineClear:Int = 0
	/** Soft drop score */
	var scoreFromSoftDrop:Int = 0
	/** Hard drop score */
	var scoreFromHardDrop:Int = 0
	/** その他の方法で手に入れたScore */
	var scoreFromOtherBonus:Int = 0

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

	/** T-Spin 0 lines (with wallkick) count */
	var totalTSpinZeroMini:Int = 0
	/** T-Spin 0 lines (without wallkick) count */
	var totalTSpinZero:Int = 0
	/** T-Spin 1 line (with wallkick) count */
	var totalTSpinSingleMini:Int = 0
	/** T-Spin 1 line (without wallkick) count */
	var totalTSpinSingle:Int = 0
	/** T-Spin 2 line (with wallkick) count */
	var totalTSpinDoubleMini:Int = 0
	/** T-Spin 2 line (without wallkick) count */
	var totalTSpinDouble:Int = 0
	/** T-Spin 3 line count */
	var totalTSpinTriple:Int = 0

	/** Back to Back 4-line clear count */
	var totalB2BFour:Int = 0
	/** Back to Back Split clear count */
	var totalB2BSplit:Int = 0
	/** Back to Back T-Spin clear count */
	var totalB2BTSpin:Int = 0

	/** Hold use count */
	var totalHoldUsed:Int = 0

	/** Largest combo */
	var maxCombo:Int = 0

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
	/** Max chain */
	var maxChain:Int = 0

	/** Roll cleared flag (0=Not Reached 1=Reached 2=Fully Survived) */
	var rollclear:Int = 0

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

	/** Reset to defaults */
	fun reset() {
		score = 0
		scoreFromLineClear = 0
		scoreFromSoftDrop = 0
		scoreFromHardDrop = 0
		scoreFromOtherBonus = 0
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
		totalTSpinZeroMini = 0
		totalTSpinZero = 0
		totalTSpinSingleMini = 0
		totalTSpinSingle = 0
		totalTSpinDoubleMini = 0
		totalTSpinDouble = 0
		totalTSpinTriple = 0
		totalB2BFour = 0
		totalB2BSplit = 0
		totalB2BTSpin = 0
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
			score = b.score
			scoreFromLineClear = b.scoreFromLineClear
			scoreFromSoftDrop = b.scoreFromSoftDrop
			scoreFromHardDrop = b.scoreFromHardDrop
			scoreFromOtherBonus = b.scoreFromOtherBonus
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
			totalTSpinZeroMini = b.totalTSpinZeroMini
			totalTSpinZero = b.totalTSpinZero
			totalTSpinSingleMini = b.totalTSpinSingleMini
			totalTSpinSingle = b.totalTSpinSingle
			totalTSpinDoubleMini = b.totalTSpinDoubleMini
			totalTSpinDouble = b.totalTSpinDouble
			totalTSpinTriple = b.totalTSpinTriple
			totalB2BFour = b.totalB2BFour
			totalB2BSplit = b.totalB2BSplit
			totalB2BTSpin = b.totalB2BTSpin
			maxCombo = b.maxCombo
			gamerate = b.gamerate
			maxChain = b.maxChain
			rollclear = b.rollclear
			garbageLines = b.garbageLines

			pieces = b.pieces
		} ?: reset()
	}

	/** プロパティセットに保存
	 * @param p プロパティセット
	 * @param id 任意のID (Player IDなど）
	 */
	fun writeProperty(p:CustomProperties, id:Int) {
		p.setProperty("$id.statistics.score", score)
		p.setProperty("$id.statistics.scoreFromLineClear", scoreFromLineClear)
		p.setProperty("$id.statistics.scoreFromSoftDrop", scoreFromSoftDrop)
		p.setProperty("$id.statistics.scoreFromHardDrop", scoreFromHardDrop)
		p.setProperty("$id.statistics.scoreFromOtherBonus", scoreFromOtherBonus)
		p.setProperty("$id.statistics.lines", lines)
		p.setProperty("$id.statistics.blocks", blocks)
		p.setProperty("$id.statistics.time", time)
		p.setProperty("$id.statistics.level", level)
		p.setProperty("$id.statistics.levelDispAdd", levelDispAdd)
		p.setProperty("$id.statistics.totalPieceLocked", totalPieceLocked)
		p.setProperty("$id.statistics.totalPieceActiveTime", totalPieceActiveTime)
		p.setProperty("$id.statistics.totalPieceMove", totalPieceMove)
		p.setProperty("$id.statistics.totalPieceRotate", totalPieceRotate)
		p.setProperty("$id.statistics.totalSingle", totalSingle)
		p.setProperty("$id.statistics.totalDouble", totalDouble)
		p.setProperty("$id.statistics.totalSplitDouble", totalSplitDouble)
		p.setProperty("$id.statistics.totalTriple", totalTriple)
		p.setProperty("$id.statistics.totalSplitTriple", totalSplitTriple)
		p.setProperty("$id.statistics.totalFour", totalQuadruple)
		p.setProperty("$id.statistics.totalTSpinZeroMini", totalTSpinZeroMini)
		p.setProperty("$id.statistics.totalTSpinZero", totalTSpinZero)
		p.setProperty("$id.statistics.totalTSpinSingleMini", totalTSpinSingleMini)
		p.setProperty("$id.statistics.totalTSpinSingle", totalTSpinSingle)
		p.setProperty("$id.statistics.totalTSpinDoubleMini", totalTSpinDoubleMini)
		p.setProperty("$id.statistics.totalTSpinDouble", totalTSpinDouble)
		p.setProperty("$id.statistics.totalTSpinTriple", totalTSpinTriple)
		p.setProperty("$id.statistics.totalB2BFour", totalB2BFour)
		p.setProperty("$id.statistics.totalB2BSplit", totalB2BSplit)
		p.setProperty("$id.statistics.totalB2BTSpin", totalB2BTSpin)
		p.setProperty("$id.statistics.totalHoldUsed", totalHoldUsed)
		p.setProperty("$id.statistics.maxCombo", maxCombo)
		p.setProperty("$id.statistics.spl", spl)
		p.setProperty("$id.statistics.spm", spm)
		p.setProperty("$id.statistics.sps", sps)
		p.setProperty("$id.statistics.lpm", lpm)
		p.setProperty("$id.statistics.lps", lps)
		p.setProperty("$id.statistics.ppm", ppm)
		p.setProperty("$id.statistics.pps", pps)
		p.setProperty("$id.statistics.gamerate", gamerate)
		p.setProperty("$id.statistics.maxChain", maxChain)
		p.setProperty("$id.statistics.rollclear", rollclear)

		for(i in 0 until pieces.size-1)
			p.setProperty("$id.statistics.pieces.$i", pieces[i])
	}

	/** プロパティセットから読み込み
	 * @param p プロパティセット
	 * @param id 任意のID (Player IDなど）
	 */
	fun readProperty(p:CustomProperties, id:Int) {
		score = p.getProperty("$id.statistics.score", 0)
		scoreFromLineClear = p.getProperty("$id.statistics.scoreFromLineClear", 0)
		scoreFromSoftDrop = p.getProperty("$id.statistics.scoreFromSoftDrop", 0)
		scoreFromHardDrop = p.getProperty("$id.statistics.scoreFromHardDrop", 0)
		scoreFromOtherBonus = p.getProperty("$id.statistics.scoreFromOtherBonus", 0)
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
		totalTSpinZeroMini = p.getProperty("$id.statistics.totalTSpinZeroMini", 0)
		totalTSpinZero = p.getProperty("$id.statistics.totalTSpinZero", 0)
		totalTSpinSingleMini = p.getProperty("$id.statistics.totalTSpinSingleMini", 0)
		totalTSpinSingle = p.getProperty("$id.statistics.totalTSpinSingle", 0)
		totalTSpinDoubleMini = p.getProperty("$id.statistics.totalTSpinDoubleMini", 0)
		totalTSpinDouble = p.getProperty("$id.statistics.totalTSpinDouble", 0)
		totalTSpinTriple = p.getProperty("$id.statistics.totalTSpinTriple", 0)
		totalB2BFour = p.getProperty("$id.statistics.totalB2BFour", 0)
		totalB2BSplit = p.getProperty("$id.statistics.totalB2BSplit", 0)
		totalB2BTSpin = p.getProperty("$id.statistics.totalB2BTSpin", 0)
		totalHoldUsed = p.getProperty("$id.statistics.totalHoldUsed", 0)
		maxCombo = p.getProperty("$id.statistics.maxCombo", 0)
		gamerate = p.getProperty("$id.statistics.gamerate", 0f)
		maxChain = p.getProperty("$id.statistics.maxChain", 0)
		rollclear = p.getProperty("$id.statistics.rollclear", 0)
		for(i in 0 until pieces.size-1)
			pieces[i] = p.getProperty("$id.statistics.pieces.$i", 0)
	}

	/** Import from String Array
	 * @param s String Array (String[42])
	 */
	fun importStringArray(s:Array<String>) {
		score = Integer.parseInt(s[0])
		scoreFromLineClear = Integer.parseInt(s[1])
		scoreFromSoftDrop = Integer.parseInt(s[2])
		scoreFromHardDrop = Integer.parseInt(s[3])
		scoreFromOtherBonus = Integer.parseInt(s[4])
		lines = Integer.parseInt(s[5])
		blocks = Integer.parseInt(s[6])
		time = Integer.parseInt(s[7])
		level = Integer.parseInt(s[8])
		levelDispAdd = Integer.parseInt(s[9])
		totalPieceLocked = Integer.parseInt(s[10])
		totalPieceActiveTime = Integer.parseInt(s[11])
		totalPieceMove = Integer.parseInt(s[12])
		totalPieceRotate = Integer.parseInt(s[13])
		totalSingle = Integer.parseInt(s[14])
		totalDouble = Integer.parseInt(s[15])
		totalSplitDouble = Integer.parseInt(s[16])
		totalTriple = Integer.parseInt(s[17])
		totalSplitTriple = Integer.parseInt(s[18])
		totalQuadruple = Integer.parseInt(s[19])
		totalTSpinZeroMini = Integer.parseInt(s[20])
		totalTSpinZero = Integer.parseInt(s[21])
		totalTSpinSingleMini = Integer.parseInt(s[22])
		totalTSpinSingle = Integer.parseInt(s[23])
		totalTSpinDoubleMini = Integer.parseInt(s[24])
		totalTSpinDouble = Integer.parseInt(s[25])
		totalTSpinTriple = Integer.parseInt(s[26])
		totalB2BFour = Integer.parseInt(s[27])
		totalB2BSplit = Integer.parseInt(s[28])
		totalB2BTSpin = Integer.parseInt(s[29])
		totalHoldUsed = Integer.parseInt(s[30])
		maxCombo = Integer.parseInt(s[31])
		gamerate = java.lang.Float.parseFloat(s[39])
		maxChain = Integer.parseInt(s[40])
		if(s.size>40) rollclear = Integer.parseInt(s[41])
		for(i in 0 until pieces.size-1)
			if(s.size>41+i)
				pieces[i] = Integer.parseInt(s[42+i])
			else
				break
	}

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
		"$score"
		, "$scoreFromLineClear"
		, "$scoreFromSoftDrop"
		, "$scoreFromHardDrop"
		, "$scoreFromOtherBonus"
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
		, "$totalTSpinZeroMini"
		, "$totalTSpinZero"
		, "$totalTSpinSingleMini"
		, "$totalTSpinSingle"
		, "$totalTSpinDoubleMini"
		, "$totalTSpinDouble"
		, "$totalTSpinTriple"
		, "$totalB2BFour"
		, "$totalB2BSplit"
		, "$totalB2BTSpin"
		, "$totalHoldUsed"
		, "$maxCombo"
		, "$spl"
		, "$spm"
		, "$sps"
		, "$lpm"
		, "$lps"
		, "$ppm"
		, "$pps"
		, "$gamerate"
		, "$maxChain"
		, "$rollclear").plus(pieces.map {"$it"})

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
	}
}

