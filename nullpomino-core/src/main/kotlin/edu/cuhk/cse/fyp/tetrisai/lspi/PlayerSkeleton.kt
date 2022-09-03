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

import java.text.DecimalFormat

open class PlayerSkeleton {
	//implement this function to have a working system
	protected var fs:FutureState = FutureState()
	protected var fs2:FutureState = FutureState()
	protected val formatter = DecimalFormat("#00")
	protected var future:DoubleArray = DoubleArray(BasisFunction.FUTURE_COUNT)
	protected var past:DoubleArray = DoubleArray(BasisFunction.FUTURE_COUNT)
	open var basisFunctions = BasisFunction()
		protected set
	open var twoPlayerBasisFunctions = TwoPlayerBasisFunction()
		protected set
	var learns = false
	var count = 0

	//*************************************************************************************************************
	// single player
	open fun pickMove(s:State, legalMoves:Array<IntArray>):Int {
		fs.resetToCurrentState(s) //set the state to the current to prepare to simulate next move.
		val maxMove = pickBestMove(s, legalMoves, future) //pick best move returns the highest scoring weighted features
		fs.makeMove(maxMove) //simulate next step
		if(learns) {
			basisFunctions.updateMatrices(s, past, future)
		}
		val tmp = future //swap the past and present around - reuse, reduce and recycle arrays.:)
		future = past
		past = tmp
		count++
		//if(count%1000==0) System.out.println(Arrays.toString(test));
		return maxMove
	}
	/**
	 * Given the current state, and the set of legal moves, decide the index of the move to be taken.
	 * Score is essentially does multiply each feature with its corresponding weight, and sums it all up.
	 *
	 * @param s Current state
	 * @param legalMoves Available moves to be made
	 * @param feature Temporary array for storing features
	 * @return
	 */
	open fun pickBestMove(s:State, legalMoves:Array<IntArray>, feature:DoubleArray?):Int {
		var score:Double
		var maxScore = Double.NEGATIVE_INFINITY
		val d = legalMoves.size
		val init =
			(Math.random()*d).toInt() //randomise the starting point to look at so that 0 is not always the first highest score
		var m = (init+1)%d
		var maxMove = m
		while(m!=init) {
			fs.makeMove(m)
			if(!fs.lost) {
				val f = basisFunctions.getFeatureArray(s, fs)
				score = score(f)
				if(maxScore<score) {
					maxScore = score
					maxMove = m
					if(learns) System.arraycopy(f, 0, feature, 0, f.size)
				}
			}
			fs.resetToCurrentState(s)
			m = (m+1)%d
		}
		return maxMove
	}

	private fun score(features:DoubleArray?):Double {
		var total = 0.0
		for(i in features!!.indices) {
			val score = features[i]*basisFunctions.weight[i]
			total += score
		}
		return total
	}
	// ********************************************************************************************************************
	// double player
	open fun pickMove(s1:State, s2:State, legalMoves:Array<IntArray>):Int {
		fs.resetToCurrentState(s1) //set the state to the current to prepare to simulate next move.
		fs2.resetToCurrentState(s2)
		val maxMove = pickBestMove(s1, s2, legalMoves, future) //pick best move returns the highest scoring weighted features
		fs.makeMove(maxMove) //simulate next step
		//updates the matrices - adds the current "instance" into its training data
		if(learns) twoPlayerBasisFunctions.updateMatrices(s1, past, future)

		val tmp = future //swap the past and present around - reuse, reduce and recycle arrays.:)
		future = past
		past = tmp
		count++
		//if(count%1000==0) System.out.println(Arrays.toString(test));
		return maxMove
	}
	// ********************************************************************************************************************
	// double player consider opponent one move
	open fun pickMove(s1:State, s2:State, legalMoves:Array<IntArray>, oppLegalMoves:Array<IntArray>):Int {
		fs.resetToCurrentState(s1) //set the state to the current to prepare to simulate next move.
		fs2.resetToCurrentState(s2)
		val maxMove =
			pickBestMove(s1, s2, legalMoves, oppLegalMoves, future) //pick best move returns the highest scoring weighted features
		fs.makeMove(maxMove) //simulate next step
		//updates the matrices - adds the current "instance" into its training data
		if(learns) twoPlayerBasisFunctions.updateMatrices(s1, past, future)

		val tmp = future //swap the past and present around - reuse, reduce and recycle arrays.:)
		future = past
		past = tmp
		count++
		//if(count%1000==0) System.out.println(Arrays.toString(test));
		return maxMove
	}
	/**
	 * Given the current state, and the set of legal moves, decide the index of the move to be taken.
	 * Score is essentially does multiply each feature with its corresponding weight, and sums it all up.
	 *
	 * @param s1 Current state
	 * @param s2 Current state
	 * @param legalMoves Available moves to be made
	 * @param feature Temporary array for storing features
	 * @return
	 */
	// double player
	open fun pickBestMove(s1:State, s2:State, legalMoves:Array<IntArray>, feature:DoubleArray?):Int {
		var score:Double
		var maxScore = Double.NEGATIVE_INFINITY
		val d = legalMoves.size
		val init =
			(Math.random()*d).toInt() //randomise the starting point to look at so that 0 is not always the first highest score
		var m = (init+1)%d
		var maxMove = m
		while(m!=init) {
			fs.makeMove(m)
			fs2.addLines(fs.linesSent)
			if(!fs.lost) {
				val f = twoPlayerBasisFunctions.getFutureArray(s1, fs, s2, fs2)
				score = 0.0
				for(i in f.indices) score += f[i]*twoPlayerBasisFunctions.weight[i]
				if(maxScore<score) {
					maxScore = score
					maxMove = m
					if(learns) System.arraycopy(f, 0, feature, 0, f.size)
				}
			}
			fs.resetToCurrentState(s1)
			fs2.resetToCurrentState(s2)
			m = (m+1)%d
		}
		return maxMove
	}
	/**
	 * Given the current state, and the set of legal moves, decide the index of the move to be taken.
	 * Score is essentially does multiply each future with its corresponding weight, and sums it all up.
	 *
	 * @param s Current state
	 * @param legalMoves Available moves to be made
	 * @param future Temporary array for storing features
	 * @return
	 */
	// double player consider oppo
	open fun pickBestMove(
		s1:State, s2:State, legalMoves:Array<IntArray>, oppLegalMoves:Array<IntArray>, future:DoubleArray?
	):Int {
		var score:Double
		var maxScore = Double.NEGATIVE_INFINITY
		val d = legalMoves.size
		val od = oppLegalMoves.size
		val init =
			(Math.random()*d).toInt() //randomise the starting point to look at so that 0 is not always the first highest score
		var m = (init+1)%d
		var maxMove = m
		while(m!=init) {
			fs.makeMove(m)
			fs2.addLinesStack(fs.linesSent)
			if(!fs.lost) {
				val oinit = (Math.random()*od).toInt()
				var om = (oinit+1)%od
				var oscore:Double
				var oMaxScore = Double.NEGATIVE_INFINITY
				var oMaxMove = om
				while(om!=oinit) {
					fs2.makeMove(om)
					fs.addLinesStack(fs2.linesSent)
					if(!fs2.lost) {
						val of = twoPlayerBasisFunctions.getFutureArray(s2, fs2, s1, fs)
						oscore = 0.0
						for(i in of.indices) oscore += of[i]*twoPlayerBasisFunctions.weight[i]
						if(oMaxScore<oscore) {
							oMaxScore = oscore
							oMaxMove = om
						}
					}
					om = (om+1)%od
				}
				fs2.makeMove(oMaxMove)
				val f = twoPlayerBasisFunctions.getFutureArray(s1, fs, s2, fs2)
				score = 0.0
				for(i in f.indices) score += f[i]*twoPlayerBasisFunctions.weight[i]
				if(maxScore<score) {
					maxScore = score
					maxMove = m
					if(learns) System.arraycopy(f, 0, future, 0, f.size)
				}
			}
			fs.resetToCurrentState(s1)
			fs2.resetToCurrentState(s2)
			m = (m+1)%d
		}
		return maxMove
	}
	// *************************************************************************************************************
	open fun printField(field:Array<IntArray>) {
		for(i in field.indices) {
			for(j in field[0].indices) {
				print(" ${formatter.format(field[i][j].toLong())}")
			}
			println()
		}
	}

	companion object {
		@JvmStatic fun main(args:Array<String>) {
			val p = PlayerSkeleton()
			val s = State()
			val t = TFrame(s)
			while(!s.lost) {
				s.makeMove(p.pickMove(s, s.legalMoves()))
				s.draw()
				s.drawNext(0, 0)
				try {
					Thread.sleep(10)
				} catch(e:InterruptedException) {
					e.printStackTrace()
				}
				println(s.linesSent)
				//BasisFunction.computeWeights();
			}
			print(s.totalLinesSent)
			print("/")
			println(s.turnNumber)
		}
	}
}
