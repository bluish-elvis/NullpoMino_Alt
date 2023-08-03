/*
 * Copyright (c) 2022, NullNoname
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

package edu.cuhk.cse.fyp.tetrisai.lspi

import kotlin.math.abs
import kotlin.math.pow

class TwoPlayerBasisFunction {
	/* A and b are important matrices in the LSPI algorithm. Every move the "player" makes, A and b are updated.
	 *
	 * A += current_features * (current_features - DISCOUNT*future_features)
	 * 						 ^
	 * 						 -------This is a matrix multiplication with the transpose of features,
	 * 								 so for k features, a k x k matrix is produced.
	 * b += reward*current_features
	 *
	 * reward = DIFF_ROWS_COMPLETED
	 */
	var a = Array(FEATURE_COUNT) {DoubleArray(FEATURE_COUNT)}
	var b = Array(FEATURE_COUNT) {DoubleArray(1)}
	var z = Array(FEATURE_COUNT) {DoubleArray(1)}
	var weight = DoubleArray(FEATURE_COUNT)

	init {
		for(i in 0..<FEATURE_COUNT) {
			a[i][i] = 0.00001
		}
	}

	private val features = DoubleArray(FEATURE_COUNT)
	private val past = DoubleArray(FEATURE_COUNT)
	/** Function to get future array for current state.*/
	fun getFutureArray(s1:State, fs1:FutureState, s2:State, fs2:FutureState):DoubleArray {
		//simple features

		//features[ROWS_COMPLETED] = fs1.getRowsCleared();
		features[DIFF_LINES_SENT] = fs1.linesSent.toDouble()
		features[DIFF_ROWS_COMPLETED] = (fs1.rowsCleared-s1.rowsCleared).toDouble()
		features[LINES_STACK] = (-s1.linesStack-1).toDouble()
		//features[DIFF_LINES_STACK] = features[LINES_STACK] - past[LINES_STACK];

		//compute height features
		val currentTurn1 = s1.turnNumber
		val currentPiece1 = s1.nextPiece
		heightFeatures(s1, past, currentPiece1, currentTurn1)
		heightFeatures(fs1, features, currentPiece1, currentTurn1)
		features[DIFF_AVG_HEIGHT] = features[DIFF_AVG_HEIGHT]-past[DIFF_AVG_HEIGHT]
		features[DIFF_COVERED_GAPS] = features[COVERED_GAPS]-past[COVERED_GAPS]
		features[DIFF_CONSECUTIVE_HOLES] = features[CONSECUTIVE_HOLES]-past[CONSECUTIVE_HOLES]

		//features[DIFF_MAX_HEIGHT] = 	features[MAX_HEIGHT]-past[MAX_HEIGHT];
		//features[DIFF_SUM_ADJ_DIFF] = 	features[SUM_ADJ_DIFF]-past[SUM_ADJ_DIFF];
		//features[DIFF_TOTAL_WELL_DEPTH] =	features[TOTAL_WELL_DEPTH]-past[TOTAL_WELL_DEPTH];

		//features[CENTER_DEV] = Math.abs(move[State.SLOT] - State.COLS/2.0);
		//features[WEIGHTED_ERODED_PIECE_CELLS] = (fs.getRowsCleared()- s.getRowsCleared())*features[ERODED_PIECE_CELLS];
		//features[LANDING_HEIGHT] = s.getTop()[move[State.SLOT]];

		//simple features

		//features[OPPO_DIFF_LINES_SENT] = fs2.getLinesSent();
		features[OPPO_LINES_STACK] = (-s2.linesStack-1).toDouble()
		//features[OPPO_DIFF_LINES_STACK] = features[OPPO_LINES_STACK] - past[OPPO_LINES_STACK];

		//compute height features
		val currentTurn2 = s2.turnNumber
		val currentPiece2 = s2.nextPiece
		oppoHeightFeatures(s2, past, currentPiece2, currentTurn2)
		oppoHeightFeatures(fs2, features, currentPiece2, currentTurn2)
		//features[OPPO_DIFF_AVG_HEIGHT] 	=		features[OPPO_DIFF_AVG_HEIGHT] - past[OPPO_DIFF_AVG_HEIGHT];
		//features[OPPO_DIFF_COVERED_GAPS] =		features[OPPO_COVERED_GAPS] - past[OPPO_COVERED_GAPS];
		//features[OPPO_DIFF_CONSECUTIVE_HOLES] =	features[OPPO_CONSECUTIVE_HOLES] - past[OPPO_CONSECUTIVE_HOLES];

		//features[OPPO_DIFF_MAX_HEIGHT] = 	features[OPPO_MAX_HEIGHT]-past[OPPO_MAX_HEIGHT];
		//features[OPPO_DIFF_SUM_ADJ_DIFF] = 	features[OPPO_SUM_ADJ_DIFF]-past[OPPO_SUM_ADJ_DIFF];
		//features[OPPO_DIFF_TOTAL_WELL_DEPTH] =	features[OPPO_TOTAL_WELL_DEPTH]-past[OPPO_TOTAL_WELL_DEPTH];

		//features[OPPO_CENTER_DEV] = Math.abs(move[State.SLOT] - State.COLS/2.0);
		//features[OPPO_WEIGHTED_ERODED_PIECE_CELLS] = (fs2.getRowsCleared()- s2.getRowsCleared())*features[OPPO_ERODED_PIECE_CELLS];
		//features[OPPO_LANDING_HEIGHT] = s2.getTop()[move[State.SLOT]];
		features[OPPO_DIE] = ((features[OPPO_MAX_HEIGHT]+s2.linesStack).pow(2.0)-features[OPPO_MAX_HEIGHT].pow(2.0)
			-((features[MAX_HEIGHT]+s1.linesStack).pow(2.0)-features[MAX_HEIGHT].pow(2.0)))
		return features
	}

	fun cellOperations(top:IntArray?, field:Array<IntArray?>?, vals:DoubleArray, turnNo:Int) {
		var rowTrans = 0
		var colTrans = 0
		var coveredGaps = 0
		var totalBlocks = 0
		var currentPieceCells = 0
		for(i in 0..<State.ROWS-1) {
			if(field!![i]!![0]==0) rowTrans++
			if(field[i]!![State.COLS-1]==0) rowTrans++
			for(j in 0..<State.COLS) {
				if(j>0&&field[i]!![j]==0!=(field[i]!![j-1]==0)) rowTrans++
				if(field[i]!![j]==0!=(field[i+1]!![j]==0)) colTrans++
				if(i<top!![j]&&field[i]!![j]==0) coveredGaps++
				if(field[i]!![j]!=0) totalBlocks++
				if(field[i]!![j]==turnNo) currentPieceCells++
			}
		}
		//		vals[ERODED_PIECE_CELLS] = 4 - currentPieceCells;
		vals[COL_TRANS] = colTrans.toDouble()
		vals[ROW_TRANS] = rowTrans.toDouble()
		vals[COVERED_GAPS] = coveredGaps.toDouble()
		vals[TOTAL_BLOCKS] = totalBlocks.toDouble()
	}

	fun oppoCellOperations(top:IntArray?, field:Array<IntArray?>?, vals:DoubleArray, turnNo:Int) {
		var rowTrans = 0
		var colTrans = 0
		var coveredGaps = 0
		var totalBlocks = 0
		var currentPieceCells = 0
		for(i in 0..<State.ROWS-1) {
			if(field!![i]!![0]==0) rowTrans++
			if(field[i]!![State.COLS-1]==0) rowTrans++
			for(j in 0..<State.COLS) {
				if(j>0&&field[i]!![j]==0!=(field[i]!![j-1]==0)) rowTrans++
				if(field[i]!![j]==0!=(field[i+1]!![j]==0)) colTrans++
				if(i<top!![j]&&field[i]!![j]==0) coveredGaps++
				if(field[i]!![j]!=0) totalBlocks++
				if(field[i]!![j]==turnNo) currentPieceCells++
			}
		}
		//		vals[ERODED_PIECE_CELLS] = 4 - currentPieceCells;
		vals[OPPO_COL_TRANS] = colTrans.toDouble()
		vals[OPPO_ROW_TRANS] = rowTrans.toDouble()
		vals[OPPO_COVERED_GAPS] = coveredGaps.toDouble()
		vals[OPPO_TOTAL_BLOCKS] = totalBlocks.toDouble()
	}
	/**
	 * Shared method for obtaining features about the height of the tetris wall.
	 * It's pretty messy, but I didn't want to split extracting these features into
	 * separate methods, or it might end up doing redundant stuff.
	 *
	 * @param s
	 * @param vals
	 */
	fun heightFeatures(s:State, vals:DoubleArray, currentPiece:Int, currentTurn:Int) {
		val field = s.field
		val top = s.top
		val c:Int = State.COLS-1
		var maxWellDepth = 0.0
		var totalWellDepth = 0.0
		var totalWeightedWellDepth = 0.0
		var maxHeight = 0.0
		var minHeight = Int.MAX_VALUE.toDouble()
		var total = 0.0
		var totalHeightSquared = 0.0
		var diffTotal = 0.0
		var squaredDiffTotal = 0.0
		var conHoles = 0.0
		for(j in 0..<State.COLS) { //by column
			total += top[j].toDouble()
			totalHeightSquared += top[j].toDouble().pow(2.0)
			diffTotal += (if(j>0) abs(top[j-1]-top[j]) else 0.0).toDouble()
			squaredDiffTotal += if(j>0) abs(top[j-1].toDouble().pow(2.0)-top[j].toDouble().pow(2.0)) else 0.0
			maxHeight = maxOf(maxHeight, top[j].toDouble())
			minHeight = minOf(minHeight, top[j].toDouble())
			if((j==0||top[j-1]>top[j])&&(j==c||top[j+1]>top[j])) {
				val wellDepth =
					if(j==0) top[1]-top[0] else if(j==c) top[j-1]-top[j] else minOf(top[j-1], top[j+1])-top[j]
				maxWellDepth = maxOf(wellDepth.toDouble(), maxWellDepth)
				totalWellDepth += maxWellDepth
				totalWeightedWellDepth += (wellDepth*(wellDepth+1)/2).toDouble()
			}
			var consecutiveHoles = 0
			for(i in top[j]..<State.ROWS-1) {
				var full = true
				for(k in 0..<State.COLS) {
					if(k!=j&&field[i]!![k]==0) {
						full = false
						break
					}
				}
				if(full) consecutiveHoles++
			}
			//			if(consecutiveHoles>1&&consecutiveHoles<4) conHoles += consecutiveHoles;
//			else conHoles -= consecutiveHoles;
			if(consecutiveHoles>0) conHoles = (4-abs(4-consecutiveHoles)).toDouble()
		}
		cellOperations(top, field, vals, currentTurn)
		vals[MAX_WELL_DEPTH] = maxWellDepth
		vals[TOTAL_WELL_DEPTH] = totalWellDepth
		vals[WEIGHTED_WELL_DEPTH] = totalWeightedWellDepth
		vals[DIFF_AVG_HEIGHT] = total/State.COLS
		vals[SUM_ADJ_DIFF] = diffTotal
		//vals[SUM_ADJ_DIFF_SQUARED] = squaredDiffTotal;
		vals[MAX_MIN_DIFF] = maxHeight-minHeight
		vals[MAX_HEIGHT] = maxHeight
		vals[COL_STD_DEV] = (totalHeightSquared-total*total/State.COLS)/(State.COLS-1).toDouble()
		vals[CONSECUTIVE_HOLES] = conHoles
	}

	fun oppoHeightFeatures(s:State, vals:DoubleArray, currentPiece:Int, currentTurn:Int) {
		val field = s.field
		val top = s.top
		val c:Int = State.COLS-1
		var maxWellDepth = 0.0
		var totalWellDepth = 0.0
		var totalWeightedWellDepth = 0.0
		var maxHeight = 0.0
		var minHeight = Int.MAX_VALUE.toDouble()
		var total = 0.0
		var totalHeightSquared = 0.0
		var diffTotal = 0.0
		var squaredDiffTotal = 0.0
		var conHoles = 0.0
		for(j in 0..<State.COLS) { //by column
			total += top[j].toDouble()
			totalHeightSquared += top[j].toDouble().pow(2.0)
			diffTotal += if(j>0) abs(top[j-1]-top[j]).toDouble() else 0.0
			squaredDiffTotal += if(j>0) abs(top[j-1].toDouble().pow(2.0)-top[j].toDouble().pow(2.0)) else 0.0
			maxHeight = maxOf(maxHeight, top[j].toDouble())
			minHeight = minOf(minHeight, top[j].toDouble())
			if((j==0||top[j-1]>top[j])&&(j==c||top[j+1]>top[j])) {
				val wellDepth =
					if(j==0) top[1]-top[0] else if(j==c) top[j-1]-top[j] else minOf(top[j-1], top[j+1])-top[j]
				maxWellDepth = maxOf(wellDepth.toDouble(), maxWellDepth)
				totalWellDepth += maxWellDepth
				totalWeightedWellDepth += (wellDepth*(wellDepth+1)/2).toDouble()
			}
			var consecutiveHoles = 0
			for(i in top[j]..<State.ROWS-1) {
				var full = true
				for(k in 0..<State.COLS) {
					if(k!=j&&field[i]!![k]==0) {
						full = false
						break
					}
				}
				if(full) consecutiveHoles++
			}
			if(consecutiveHoles in 2..3) conHoles += consecutiveHoles.toDouble() else conHoles -= consecutiveHoles.toDouble()
		}
		oppoCellOperations(top, field, vals, currentTurn)
		vals[OPPO_MAX_WELL_DEPTH] = maxWellDepth
		vals[OPPO_TOTAL_WELL_DEPTH] = totalWellDepth
		vals[OPPO_WEIGHTED_WELL_DEPTH] = totalWeightedWellDepth
		//vals[OPPO_DIFF_AVG_HEIGHT] = ((double)total)/State.COLS;
		vals[OPPO_SUM_ADJ_DIFF] = diffTotal
		//vals[SUM_ADJ_DIFF_SQUARED] = squaredDiffTotal;
		vals[OPPO_MAX_MIN_DIFF] = maxHeight-minHeight
		vals[OPPO_MAX_HEIGHT] = maxHeight
		vals[OPPO_COL_STD_DEV] = (totalHeightSquared-total*total/State.COLS)/(State.COLS-1).toDouble()
		vals[OPPO_CONSECUTIVE_HOLES] = conHoles
	}

	private val tmpA = Array(FEATURE_COUNT) {DoubleArray(FEATURE_COUNT)}
	private val mWeight = Array(FEATURE_COUNT) {DoubleArray(1)}
	private val mFeatures = Array(FEATURE_COUNT) {DoubleArray(1)}
	private val mFutureFeatures = Array(1) {DoubleArray(FEATURE_COUNT)}
	private val mRowFeatures = Array(1) {DoubleArray(FEATURE_COUNT)}
	private val changeToA = Array(FEATURE_COUNT) {DoubleArray(FEATURE_COUNT)}
	private val zz = Array(FEATURE_COUNT) {DoubleArray(1)}
	/**
	 * Matrix update function. See above for descriptions.
	 *
	 * ( I know there are many arrays all over the place,
	 * but I'm trying to _NOT_ create any new arrays to reduce compute time and memory
	 * that's why you see all the System.arraycopy everywhere )
	 * @param s
	 * @param features
	 * @param futureFeatures
	 */
	fun updateMatrices(s:State?, features:DoubleArray, futureFeatures:DoubleArray) {
		//preprocessing
		Matrix.arrayToCol(features, mFeatures)
		Matrix.multiply(DISCOUNT*LAMBDA, z)
		Matrix.sum(z, mFeatures)
		Matrix.copy(z, zz)
		Matrix.arrayToRow(futureFeatures, mFutureFeatures)
		Matrix.arrayToRow(features, mRowFeatures)
		Matrix.multiply(-1*DISCOUNT, mFutureFeatures)
		Matrix.sum(mRowFeatures, mFutureFeatures)
		Matrix.product(z, mRowFeatures, changeToA)
		Matrix.sum(a, changeToA)
		//		Matrix.multiply(features[OPPO_DIFF_DIE], mFeatures);
		Matrix.multiply(features[DIFF_LINES_SENT], zz)
		//		Matrix.multiply(features[DIFF_ROWS_COMPLETED], mFeatures);
		Matrix.sum(b, zz)
	}
	/**
	 * The computation of the weights can be separate from the updating of matrix A & b.
	 *
	 * weights = A^(-1)b
	 *
	 * The way I'm doing this in the back-end is running the Gauss-Jordan Elimination algo alongside the b matrix.
	 * This saves computing inverse of A and then multiplying it with b.
	 */
	fun computeWeights() {
		if(Matrix.premultiplyInverse(a, b, mWeight, tmpA)==null) return
		Matrix.colToArray(mWeight, weight)
		//printField(mWeight);
	}

	fun printField(field:Array<DoubleArray>) {
		for(i in field.indices) {
			for(j in field[0].indices) {
				print(" "+field[i][j])
			}
			println()
		}
	}

	companion object {
		// discount value, part of the LRQ algo.
		private const val DISCOUNT = 0.96
		private const val LAMBDA = 0.7
		/**
		 * Below are features being used for scoring.
		 * These are based on the _next_ state.
		 * This means these numbers represent the values that are reported if the attempted action is taken.
		 * The values starting with DIFF_ represent the differnece between the _next_ state and the current state
		 *
		 * Notes:
		 * -	SUM_ADJ_DIFF seems to work better squared.
		 * -	A well is a whole starting at the top of the wall. Both sides of a well are blocks.
		 *
		 */
		private var count = 0
		// player
		private val MAX_HEIGHT = count++
		private val COVERED_GAPS = count++ //(PD)holes in the tetris wall which are inaccessible from the top
		private val DIFF_LINES_SENT = count++ //(ESTR FYP)
		private val DIFF_ROWS_COMPLETED = count++ //(LSPI paper)
		private val MAX_MIN_DIFF = count++ //(Novel)
		private val MAX_WELL_DEPTH = count++ //(Novel)maximum well depth
		private val TOTAL_WELL_DEPTH = count++ //(PD)total depth of all wells on the tetris wall.
		private val TOTAL_BLOCKS = count++ //(CF)total number of blocks in the wall
		private val COL_TRANS = count++ //(PD)
		private val ROW_TRANS = count++ //(PD)
		private val DIFF_AVG_HEIGHT = count++ //(LSPI paper)
		private val SUM_ADJ_DIFF = count++ //(Handout)
		private val DIFF_COVERED_GAPS = count++ //(Novel)
		private val WEIGHTED_WELL_DEPTH = count++ //(CF)the deeper the well is, the heavier the "weightage".
		//	final private static int LANDING_HEIGHT					= count++;	//(PD)
		private val COL_STD_DEV = count++ //(Novel)
		//	final private static int ERODED_PIECE_CELLS				= count++;	//Intemediary step for WEIGHTED_ERODED_PIECE_CELLS
		//	final private static int WEIGHTED_ERODED_PIECE_CELLS	= count++;	//(PD)
		//	final private static int CENTER_DEV						= count++;	//(PD) priority value used to break tie in PD
		//	final private static int SUM_ADJ_DIFF_SQUARED			= count++;	//(Novel)(sum of the difference between adjacent columns)^2
		private val CONSECUTIVE_HOLES = count++ //(ESTR FYP) leave a vertical empty column
		private val DIFF_CONSECUTIVE_HOLES = count++ //(ESTR FYP)
		private val LINES_STACK = count++ //(ESTR FYP) record line stack
		//	final private static int DIFF_LINES_STACK				= count++;	//(ESTR FYP)
		private val single_player_features = count
		// opponent
		private val OPPO_MAX_HEIGHT = count++
		private val OPPO_COVERED_GAPS = count++ //(PD)holes in the tetris wall which are inaccessible from the top
		//	final private static int OPPO_DIFF_LINES_SENT			= count++;	//(ESTR FYP)
		//final private static int OPPO_DIFF_ROWS_COMPLETED		= count++;	//(LSPI paper)
		private val OPPO_MAX_MIN_DIFF = count++ //(Novel)
		private val OPPO_MAX_WELL_DEPTH = count++ //(Novel)maximum well depth
		private val OPPO_TOTAL_WELL_DEPTH = count++ //(PD)total depth of all wells on the tetris wall.
		private val OPPO_TOTAL_BLOCKS = count++ //(CF)total number of blocks in the wall
		private val OPPO_COL_TRANS = count++ //(PD)
		private val OPPO_ROW_TRANS = count++ //(PD)
		//	final private static int OPPO_DIFF_AVG_HEIGHT			= count++;	//(LSPI paper)
		private val OPPO_SUM_ADJ_DIFF = count++ //(Handout)
		//	final private static int OPPO_DIFF_COVERED_GAPS			= count++;	//(Novel)
		private val OPPO_WEIGHTED_WELL_DEPTH = count++ //(CF)the deeper the well is, the heavier the "weightage".
		//	final private static int OPPO_LANDING_HEIGHT			= count++;	//(PD)
		private val OPPO_COL_STD_DEV = count++ //(Novel)
		//	final private static int OPPO_ERODED_PIECE_CELLS		= count++;	//Intemediary step for WEIGHTED_ERODED_PIECE_CELLS
		//	final private static int OPPO_WEIGHTED_ERODED_PIECE_CELLS= count++;	//(PD)
		//	final private static int OPPO_CENTER_DEV				= count++;	//(PD) priority value used to break tie in PD
		//	final private static int OPPO_SUM_ADJ_DIFF_SQUARED		= count++;	//(Novel)(sum of the difference between adjacent columns)^2
		private val OPPO_CONSECUTIVE_HOLES = count++ //(ESTR FYP) leave a vertical empty column
		//	final private static int OPPO_DIFF_CONSECUTIVE_HOLES	= count++;	//(ESTR FYP)
		private val OPPO_LINES_STACK = count++ //(ESTR FYP) record line stack
		//	final private static int OPPO_DIFF_LINES_STACK			= count++;	//(ESTR FYP)
		private val OPPO_DIE = count++ //(ESTR FYP)
		val FEATURE_COUNT = count
	}
}
