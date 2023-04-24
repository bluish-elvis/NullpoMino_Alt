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
package mu.nu.nullpo.game.component

import mu.nu.nullpo.util.CustomProperties
import java.io.Serializable

/** Scoreなどの情報 */
class Statistics:Serializable {
	/** Total score */
	val score:Long get() = 0L+scoreLine+scoreHD+scoreSD+scoreBonus
	/** Line clear score */
	var scoreLine = 0
	/** Soft drop score */
	var scoreSD = 0
	/** Hard drop score */
	var scoreHD = 0
	/** その他の方法で手に入れたScore */
	var scoreBonus = 0

	/** Total line count */
	var lines = 0

	/** Total sent garbage */
	val attacks get() = attacksLine+attacksTwist+attacksBonus
	/** Multi Lines clear garbage */
	var attacksLine = 0
	/** Twister clear garbage */
	var attacksTwist = 0
	/** Combo/Chain/B2B garbage */
	var attacksBonus = 0

	/** Cleared Garbage Lines */
	var garbageLines = 0
	/** Total Cleared block count */
	var blocks = 0

	/** 経過 time */
	var time = 0

	/** Level */
	var level = 0

	/** Levelの表示に加算する数値 (表示 levelが内部の値と異なる場合に使用) */
	var levelDispAdd = 0

	/** 置いたピースのcount */
	var totalPieceLocked = 0

	/** ピースを操作していた合計 time */
	var totalPieceActiveTime = 0

	/** ピースを移動させた合計 count */
	var totalPieceMove = 0

	/** ピースを回転させた合計 count */
	var totalPieceSpin = 0

	/** 1-line clear count */
	var totalSingle = 0
	/** 2-line clear count */
	var totalDouble = 0
	/** Split 2-line clear count */
	var totalSplitDouble = 0
	/** 3-line clear count */
	var totalTriple = 0
	/** One-Two-3-line clear count */
	var totalSplitTriple = 0
	/** 4-line clear count */
	var totalQuadruple = 0

	/** Twister but no-line (with wallkick) count */
	var totalTwistZeroMini = 0
	/** Twister but no-line (without wallkick) count */
	var totalTwistZero = 0
	/** Twister 1-line (with wallkick) count */
	var totalTwistSingleMini = 0
	/** Twister 1-line (without wallkick) count */
	var totalTwistSingle = 0
	/** Twister 2-line (with wallkick) count */
	var totalTwistDoubleMini = 0
	/** Twister 2-line (without wallkick) count */
	var totalTwistDouble = 0
	/** Twister Split 2-line count */
	var totalTwistSplitDouble = 0
	/** Twister 3 line count */
	var totalTwistTriple = 0
	/** Twister One-Two-3-line count */
	var totalTwistSplitTriple = 0

	/** Back to Back 4-line clear count */
	var totalB2BQuad = 0
	/** Back to Back Split clear count */
	var totalB2BSplit = 0
	/** Back to Back Twister clear count */
	var totalB2BTwist = 0

	/** Hold use count */
	var totalHoldUsed = 0

	/** Longest combo */
	var maxCombo = 0
	/** Longest Avalanche-chain */
	var maxChain = 0
	/** Longest Back to Back Chain */
	var maxB2B = 0

	/** 1Linesあたりの得点 (Score Per Line) */
	val spl:Double get() = if(lines>0) score.toDouble()/lines.toDouble() else .0
	/** 1Pieceあたりの得点 (Score Per Piece) */
	val spp:Double get() = if(totalPieceLocked>0) score.toDouble()/totalPieceLocked.toDouble() else .0
	/** 1分間あたりの得点 (Score Per Minute) */
	val spm:Double get() = if(time>0) score*3600.0/time else .0
	/** 1秒間あたりの得点 (Score Per Second) */
	val sps:Double get() = if(time>0) score*60.0/time else .0

	/** 1分間あたりのLines (Lines Per Minute) */
	val lpm:Float get() = if(time>0) lines*3600f/time else 0f
	/** 1秒間あたりのLines (Lines Per Second) */
	val lps:Float get() = if(time>0) lines*60f/time else 0f
	/** 1分間あたりのピースcount (Pieces Per Minute) */
	val ppm:Float get() = if(time>0) totalPieceLocked*3600f/time else 0f
	/** 1秒間あたりのピースcount (Pieces Per Second) */
	val pps:Float get() = if(time>0) totalPieceLocked*60f/time else 0f

	/** 1Linesあたりの攻撃 (Attack Per Line) */
	val apl:Float get() = if(lines>0) attacks.toFloat()/lines.toFloat() else 0f
	/** 1Pieceあたりの攻撃 (Attack Per Piece) */
	val app:Float get() = if(totalPieceLocked>0) attacks.toFloat()/totalPieceLocked.toFloat() else 0f
	/** 1分間あたりの攻撃 (Attack Per Minutes) */
	val apm:Float get() = if(time>0) attacks*3600f/time else 0f
	/** 対戦評価 (VS Score)*/
	val vs:Float get() = if(time>0) (attacks+garbageLines)*6000f/time else 0f

	/** TAS detection: slowdown rate */
	var gamerate = 0f

	/** Roll cleared flag (0=Not Reached 1=Reached 2=Fully Survived) */
	var rollClear = 0
		set(it) {
			if(field==it||!(0..2).contains(it)) return
			when(it) {
				1 -> rollReached++
				2 -> {
					if(field<1) rollReached++
					rollSurvived++
				}
			}
			field = it
			rollclearHistory = listOf(it)+rollclearHistory.takeLast(historyMax)
		}
	/** Roll cleared history */
	var rollclearHistory = List(historyMax) {0}

	/** Roll Reached Count */
	var rollReached = 0
	/** Roll Survived Count */
	var rollSurvived = 0

	var pieces = List(Piece.PIECE_COUNT) {0}

	var randSeed:Long = 0L
	/** Constructor */
	constructor() {
		reset()
	}

	/** Copy constructor
	 * @param s Copy source
	 */
	constructor(s:Statistics?) {
		replace(s)
	}

	/** Constructor that imports data from a String List
	 * @param s String List (String[37])
	 */
	constructor(s:List<String>) {
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
		attacksLine = 0
		attacksTwist = 0
		attacksBonus = 0
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
		totalPieceSpin = 0
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
		rollClear = 0

		pieces = List(Piece.PIECE_COUNT) {0}
		randSeed = 0L
	}

	/** 設定を[s]からコピー */
	fun replace(s:Statistics?) {
		s?.let {b ->
			scoreLine = b.scoreLine
			scoreSD = b.scoreSD
			scoreHD = b.scoreHD
			scoreBonus = b.scoreBonus
			lines = b.lines
			attacksLine = b.attacksLine
			attacksTwist = b.attacksTwist
			attacksBonus = b.attacksBonus
			blocks = b.blocks
			time = b.time
			level = b.level
			levelDispAdd = b.levelDispAdd
			totalPieceLocked = b.totalPieceLocked
			totalPieceActiveTime = b.totalPieceActiveTime
			totalPieceMove = b.totalPieceMove
			totalPieceSpin = b.totalPieceSpin
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
			rollClear = b.rollClear
			rollclearHistory =
				b.rollclearHistory
			garbageLines = b.garbageLines

			pieces = b.pieces
			randSeed = b.randSeed
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
			attacksLine += b.attacksLine
			attacksTwist += b.attacksTwist
			attacksBonus += b.attacksBonus
			blocks += b.blocks
			time += b.time
			level += b.level
			levelDispAdd += b.levelDispAdd
			totalPieceLocked += b.totalPieceLocked
			totalPieceActiveTime += b.totalPieceActiveTime
			totalPieceMove += b.totalPieceMove
			totalPieceSpin += b.totalPieceSpin
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
			rollClear = b.rollClear
			garbageLines += b.garbageLines

			pieces = pieces.mapIndexed {it, i -> it+b.pieces[i]}
			randSeed = b.randSeed
		}
	}

	/** プロパティセットに保存
	 * @param p プロパティセット
	 * @param id 任意のID (Player IDなど）
	 */
	fun writeProperty(p:CustomProperties, id:Int) =
		mapOf<String, Comparable<*>>(
			"$id.statistics.scoreLine" to scoreLine,
			"$id.statistics.scoreSD" to scoreSD,
			"$id.statistics.scoreHD" to scoreHD,
			"$id.statistics.scoreBonus" to scoreBonus,
			"$id.statistics.lines" to lines,
			"$id.statistics.attacksLine" to attacksLine,
			"$id.statistics.attacksTwist" to attacksTwist,
			"$id.statistics.attacksBonus" to attacksBonus,
			"$id.statistics.blocks" to blocks,
			"$id.statistics.time" to time,
			"$id.statistics.level" to level,
			"$id.statistics.levelDispAdd" to levelDispAdd,
			"$id.statistics.totalPieceLocked" to totalPieceLocked,
			"$id.statistics.totalPieceActiveTime" to totalPieceActiveTime,
			"$id.statistics.totalPieceMove" to totalPieceMove,
			"$id.statistics.totalPieceRotate" to totalPieceSpin,
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
			"$id.statistics.rollClear" to rollClear,
			"$id.statistics.randSeed" to randSeed,
		).plus((0 until pieces.size-1).associate {
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
		attacksLine = p.getProperty("$id.statistics.attacksLine", 0)
		attacksTwist = p.getProperty("$id.statistics.attacksTwist", 0)
		attacksBonus = p.getProperty("$id.statistics.attacksBonus", 0)
		blocks = p.getProperty("$id.statistics.blocks", 0)
		time = p.getProperty("$id.statistics.time", 0)
		level = p.getProperty("$id.statistics.level", 0)
		levelDispAdd = p.getProperty("$id.statistics.levelDispAdd", 0)
		totalPieceLocked = p.getProperty("$id.statistics.totalPieceLocked", 0)
		totalPieceActiveTime = p.getProperty("$id.statistics.totalPieceActiveTime", 0)
		totalPieceMove = p.getProperty("$id.statistics.totalPieceMove", 0)
		totalPieceSpin = p.getProperty("$id.statistics.totalPieceRotate", 0)
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
		rollClear = p.getProperty("$id.statistics.rollClear", 0)
		randSeed = p.getProperty("$id.statistics.randSeed", 0L)
		pieces = List(pieces.size) {p.getProperty("$id.statistics.pieces.$it", 0)}
	}

	/** Import from String List
	 * @param s String List (String[42])
	 */
	fun importStringArray(s:List<String>) {
		val pi = MutableList(pieces.size) {0}
		listOf<(String)->Unit>(
			{scoreLine = it.toInt()},
			{scoreSD = it.toInt()},
			{scoreHD = it.toInt()},
			{scoreBonus = it.toInt()},
			{attacksLine = it.toInt()},
			{attacksTwist = it.toInt()},
			{attacksBonus = it.toInt()},
			{lines = it.toInt()},
			{blocks = it.toInt()},
			{time = it.toInt()},
			{level = it.toInt()},
			{levelDispAdd = it.toInt()},
			{totalPieceLocked = it.toInt()},
			{totalPieceActiveTime = it.toInt()},
			{totalPieceMove = it.toInt()},
			{totalPieceSpin = it.toInt()},
			{totalSingle = it.toInt()},
			{totalDouble = it.toInt()},
			{totalSplitDouble = it.toInt()},
			{totalTriple = it.toInt()},
			{totalSplitTriple = it.toInt()},
			{totalQuadruple = it.toInt()},
			{totalTwistZeroMini = it.toInt()},
			{totalTwistZero = it.toInt()},
			{totalTwistSingleMini = it.toInt()},
			{totalTwistSingle = it.toInt()},
			{totalTwistDoubleMini = it.toInt()},
			{totalTwistDouble = it.toInt()},
			{totalTwistSplitDouble = it.toInt()},
			{totalTwistTriple = it.toInt()},
			{totalTwistSplitTriple = it.toInt()},
			{totalB2BQuad = it.toInt()},
			{totalB2BSplit = it.toInt()},
			{totalB2BTwist = it.toInt()},
			{totalHoldUsed = it.toInt()},
			{maxCombo = it.toInt()},
			{maxB2B = it.toInt()},
			{gamerate = it.toFloat()},
			{maxChain = it.toInt()},
			{rollClear = it.toInt()},
			{randSeed = it.toLong()}).plus(
			(0 until pieces.size-1).map {i:Int ->
				{pi[i] = it.toInt()}
			}).zip(s).forEach {(m, st) -> m(st)}
		pieces = pi.toList()
	}

	/** Import from String
	 * @param s String (Split by ;)
	 */
	fun importString(s:String) {
		importStringArray(s.split(Regex(";")).dropLastWhile {it.isEmpty()})
	}

	/** Export to String List
	 * @return String List (String[38])
	 */
	fun exportStringArray():List<String> = listOf(
		"$scoreLine", "$scoreSD", "$scoreHD", "$scoreBonus", "$attacksLine", "$attacksTwist", "$attacksBonus",
		"$lines", "$blocks", "$time", "$level", "$levelDispAdd", "$totalPieceLocked", "$totalPieceActiveTime",
		"$totalPieceMove", "$totalPieceSpin", "$totalSingle", "$totalDouble", "$totalSplitDouble",
		"$totalTriple", "$totalSplitTriple", "$totalQuadruple", "$totalTwistZeroMini", "$totalTwistZero",
		"$totalTwistSingleMini", "$totalTwistSingle", "$totalTwistDoubleMini", "$totalTwistDouble", "$totalTwistSplitDouble",
		"$totalTwistTriple", "$totalTwistSplitTriple", "$totalB2BQuad", "$totalB2BSplit", "$totalB2BTwist", "$totalHoldUsed",
		"$maxCombo", "$maxB2B", "$gamerate", "$maxChain", "$rollClear", "$randSeed"
	)+(pieces.map {"$it"})

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
