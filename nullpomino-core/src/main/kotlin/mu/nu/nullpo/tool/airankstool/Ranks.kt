/*
 * Copyright (c) 2010-2022, NullNoname
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

package mu.nu.nullpo.tool.airankstool

import mu.nu.nullpo.game.component.Piece
import java.util.Arrays
import kotlin.math.abs
import kotlin.math.pow

@kotlinx.serialization.Serializable
class Ranks:java.io.Serializable {
	private val ranks:IntArray

	val stackWidth:Int
	val size:Int
	internal var ranksFrom:Ranks? = null
	val maxJump:Int
	var error = 0
		private set
	private var maxError = 0
	private var rankMin = 0
	private var rankMax = 0

	private var completion = 0
	private val base:Int
	private val surfaceWidth:Int

	val completionPercentage:Int
		get() {
			var completionPercentage:Long = (completion.toLong()+1)*100
			completionPercentage /= size.toLong()
			return completionPercentage.toInt()

		}
	val errorPercentage:Float
		get() {
			val errorLong = (error/completion).toLong()
			val maxErrorPossible = Integer.MAX_VALUE.toLong()-Integer.MIN_VALUE.toLong()
			var errorPercentage = errorLong.toFloat()/maxErrorPossible.toFloat()
			errorPercentage *= 100f
			return errorPercentage
		}

	fun getMaxError():Float = maxError.toFloat()

	fun completionPercentageIncrease():Boolean = 0==completion%(size/100)&&completion!=0

	constructor(maxJump:Int, stackWidth:Int) {

		this.maxJump = maxJump
		base = 2*maxJump+1

		this.stackWidth = stackWidth

		surfaceWidth = stackWidth-1

		size = base.toDouble().pow(surfaceWidth.toDouble()).toInt()
		ranks = IntArray(size)
		completion = 0
		error = 0
		maxError = 0
		rankMin = Integer.MAX_VALUE
		rankMax = 0
		Arrays.fill(ranks, Integer.MAX_VALUE)

	}

	constructor(rankFrom:Ranks?) {

		ranksFrom = rankFrom
		maxJump = ranksFrom!!.maxJump
		base = 2*maxJump+1

		stackWidth = ranksFrom!!.stackWidth
		surfaceWidth = stackWidth-1
		size = (2*maxJump+1).toDouble().pow((stackWidth-1).toDouble()).toInt()
		ranks = IntArray(size)
		completion = 0
		error = 0
		maxError = 0
		rankMin = Integer.MAX_VALUE
		rankMax = 0

	}

	fun setRanksFrom(ranksfrom:Ranks) {
		ranksFrom = ranksfrom
		error = 0
		maxError = 0
		completion = 0
		rankMin = Integer.MAX_VALUE
		rankMax = 0

	}

	fun freeRanksFrom() {
		ranksFrom = null
	}

	fun getRankValue(surface:Int):Int = ranks[surface]

	fun encode(surface:IntArray):Int {
		var surfaceNum = 0
		var factor = 1
		for(i in 0 until surfaceWidth) {
			surfaceNum += (surface[i]+maxJump)*factor
			factor *= base
		}
		return surfaceNum
	}

	private fun setRankValue(surface:Int, value:Int) {
		ranks[surface] = value
	}

	fun setRank(surface:IntArray, surfaceDecodedWork:IntArray) {
		val currentSurfaceNum = encode(surface)
		setRankValue(currentSurfaceNum, getRank(surface, surfaceDecodedWork))
		synchronized(this) {
			completion++
			var errorCurrent = abs(ranks[currentSurfaceNum]-ranksFrom!!.getRankValue(currentSurfaceNum))
			if(errorCurrent==0) errorCurrent = 0
			if(errorCurrent>maxError) maxError = errorCurrent
			error += errorCurrent
		}
	}

	fun scaleRanks() {
		/* int pas =(Integer.MAX_VALUE-rankMin)/4;
 * int n1=0;
 * int n2=0;
 * int n3=0;
 * int n4=0; */

		for(i in 0 until size) {
			var newValue = (ranks[i]-rankMin).toLong()*(Integer.MAX_VALUE-rankMin).toLong()
			newValue /= (rankMax-rankMin)

			newValue = maxOf(minOf(newValue, (Integer.MAX_VALUE-rankMin).toLong()), 0)
			newValue += rankMin
			/* if (newValue/pas==0){
 * n1++;
 * }
 * if (newValue/pas==1){
 * n2++;
 * }
 * if (newValue/pas==2){
 * n3++;
 * }
 * if (newValue/pas==3){
 * n4++;
 * } */
			ranks[i] = newValue.toInt()
		}
		//System.out.println("n1 = "+n1+" n2 = "+n2+" n3 = "+n3+" n4 = "+n4);
	}

	fun decode(surfaceNum:Int, surface:IntArray) {

		var surfaceNumWork = surfaceNum
		for(i in 0 until surfaceWidth) {
			surface[i] = surfaceNumWork%base-maxJump
			surfaceNumWork /= base
		}

	}

	fun iterateSurface(surface:IntArray, surfaceDecodedWork:IntArray) {
		setRank(surface, surfaceDecodedWork)

		var retenue = 1
		for(i in 0 until surfaceWidth)
			if(retenue==0)
				break
			else if(surface[i]<maxJump) {
				surface[i]++
				surfaceDecodedWork[i]++
				retenue = 0
			} else {
				surface[i] = -maxJump
				surfaceDecodedWork[i] = -maxJump
				retenue = 1
			}

	}

	private fun getRank(surface:IntArray, surfaceDecodedWork:IntArray):Int {
		var sum = 0L

		for(p in 0 until Piece.PIECE_STANDARD_COUNT) {
			val rankForPiece = getRankPiece(surface, surfaceDecodedWork, p)
			//System.out.println("piece :"+p+" rank : "+rankForPiece);
			sum += rankForPiece.toLong()
		}
		var result:Int
		result = (sum/Piece.PIECE_STANDARD_COUNT).toInt()
		synchronized(this) {
			if(result>rankMax) rankMax = result
			if(result<rankMin) rankMin = result
		}

		if(result<0) {
			result = 0
			println("ahhhhhh")
		}
		return result
	}

	private fun getRankPiece(surface:IntArray, surfaceDecodedWork:IntArray, piece:Int):Int {
		var bestRank = 0
		for(r in 0 until PIECES_NUM_ROTATIONS[piece]) {
			val rank = getRankPieceRotation(surface, surfaceDecodedWork, piece, r)
			if(rank>bestRank) bestRank = rank

		}
		return bestRank
	}

	fun surfaceFitsPiece(surface:IntArray, piece:Int, rotation:Int, x:Int):Boolean {
		var fits = true

		for(x1 in 0 until PIECES_WIDTHS[piece][rotation]-1)
			if(surface[x+x1]!=PIECES_LOWESTS[piece][rotation][x1]-PIECES_LOWESTS[piece][rotation][x1+1]) {
				fits = false
				break
			}
		return fits
	}

	fun surfaceAddPossible(surfaceDecodedWork:IntArray, piece:Int, rotation:Int, x:Int):Boolean {
		val addPossible = true
		if(x>0) {
			surfaceDecodedWork[x-1] += PIECES_HEIGHTS[piece][rotation][0]
			if(surfaceDecodedWork[x-1]>maxJump)
				return false
			else if(surfaceDecodedWork[x-1]<-maxJump) return false

		}

		for(x1 in 0 until PIECES_WIDTHS[piece][rotation]-1) {
			surfaceDecodedWork[x+x1] += PIECES_HEIGHTS[piece][rotation][x1+1]-PIECES_HEIGHTS[piece][rotation][x1]
			if(surfaceDecodedWork[x+x1]>maxJump)
				return false
			else if(surfaceDecodedWork[x+x1]<-maxJump) return false
		}

		if(x<surfaceWidth-(PIECES_WIDTHS[piece][rotation]-1)) {
			surfaceDecodedWork[x+PIECES_WIDTHS[piece][rotation]-1] -= PIECES_HEIGHTS[piece][rotation][PIECES_WIDTHS[piece][rotation]-1]
			if(surfaceDecodedWork[x+PIECES_WIDTHS[piece][rotation]-1]>maxJump)
				return false
			else if(surfaceDecodedWork[x+PIECES_WIDTHS[piece][rotation]-1]<-maxJump) return false
		}
		return addPossible
	}

	fun addToHeights(heights:IntArray, piece:Int, rotation:Int, x:Int) {
		for(x1 in 0 until PIECES_WIDTHS[piece][rotation])
			heights[x+x1] += PIECES_HEIGHTS[piece][rotation][x1]

	}

	fun heightsToSurface(heights:IntArray):IntArray {

		val surface = IntArray(stackWidth-1)
		for(i in 0 until stackWidth-1) {
			var diff = heights[i+1]-heights[i]
			if(diff>maxJump) diff = maxJump
			if(diff<-maxJump) diff = -maxJump
			surface[i] = diff
		}
		return surface

	}

	private fun getRankPieceRotation(surface:IntArray, surfaceDecodedWork:IntArray, piece:Int, rotation:Int):Int {

		var bestRank = 0
		if(piece==Piece.PIECE_I) {
			val rank = ranksFrom!!.getRankValue(encode(surface))
			if(rank>bestRank) bestRank = rank

		}

		for(x in 0 until stackWidth-(PIECES_WIDTHS[piece][rotation]-1)) {
			val fits = surfaceFitsPiece(surfaceDecodedWork, piece, rotation, x)

			if(fits) {
				val addPossible = surfaceAddPossible(surfaceDecodedWork, piece, rotation, x)
				if(addPossible) {
					val newSurface = encode(surfaceDecodedWork)

					val rank = ranksFrom!!.getRankValue(newSurface)
					if(rank>bestRank) bestRank = rank

				}

			}
			//Reinit work surface
			if(x>0) surfaceDecodedWork[x-1] = surface[x-1]
			System.arraycopy(surface, x, surfaceDecodedWork, x, PIECES_WIDTHS[piece][rotation]-1)
			if(x<surfaceWidth-(PIECES_WIDTHS[piece][rotation]-1))
				surfaceDecodedWork[x+PIECES_WIDTHS[piece][rotation]-1] = surface[x+PIECES_WIDTHS[piece][rotation]-1]

		}

		return bestRank
	}

	companion object {
		/** */

		private const val serialVersionUID = 1L

		//Number of different orientations a piece can have (this is used to save computing time)
		val PIECES_NUM_ROTATIONS = intArrayOf(
			2, // I
			4, // L
			1, // O
			2, // Z
			4, // T
			4, // J
			2, // S
			1, // I1
			2, // I2
			2, // I3
			4 // L3
		)

		val PIECES_LEFTMOSTS = arrayOf(
			intArrayOf(0, 2, 0, 1), // I
			intArrayOf(0, 1, 0, 0), // L
			intArrayOf(0, 0, 0, 0), // O
			intArrayOf(0, 1, 0, 0), // Z
			intArrayOf(0, 1, 0, 0), // T
			intArrayOf(0, 1, 0, 0), // J
			intArrayOf(0, 1, 0, 0), // S
			intArrayOf(0, 0, 0, 0), // I1
			intArrayOf(0, 1, 0, 0), // I2
			intArrayOf(0, 1, 0, 1), // I3
			intArrayOf(0, 0, 0, 0) // L3
		)
		val PIECES_RIGHTMOSTS = arrayOf(
			intArrayOf(3, 2, 3, 1), //I
			intArrayOf(2, 2, 2, 1), //L
			intArrayOf(1, 1, 1, 1), //O
			intArrayOf(2, 2, 2, 1), //Z
			intArrayOf(2, 2, 2, 1), //T
			intArrayOf(2, 2, 2, 1), //J
			intArrayOf(2, 2, 2, 1), //S
			intArrayOf(0, 0, 0, 0), //I1
			intArrayOf(1, 1, 1, 0), //I2
			intArrayOf(2, 1, 2, 1), //I3
			intArrayOf(1, 1, 1, 1)
		)//L3

		val PIECES_WIDTHS = arrayOf(
			intArrayOf(4, 1, 4, 1), //I
			intArrayOf(3, 2, 3, 2), //L
			intArrayOf(2, 2, 2, 2), //O
			intArrayOf(3, 2, 3, 2), //Z
			intArrayOf(3, 2, 3, 2), //T
			intArrayOf(3, 2, 3, 2), //J
			intArrayOf(3, 2, 3, 2), //S
			intArrayOf(1, 1, 1, 1), //I1
			intArrayOf(2, 1, 2, 1), //I2
			intArrayOf(3, 1, 3, 1), //I3
			intArrayOf(2, 2, 2, 2) //L3
		)

		val PIECES_HEIGHTS = arrayOf(
			arrayOf(intArrayOf(1, 1, 1, 1), intArrayOf(4), intArrayOf(1, 1, 1, 1), intArrayOf(4)), //I
			arrayOf(intArrayOf(1, 1, 2), intArrayOf(3, 1), intArrayOf(2, 1, 1), intArrayOf(1, 3)), //L
			arrayOf(intArrayOf(2, 2), intArrayOf(2, 2), intArrayOf(2, 2), intArrayOf(2, 2)), //O
			arrayOf(intArrayOf(1, 2, 1), intArrayOf(2, 2), intArrayOf(1, 2, 1), intArrayOf(2, 2)), //Z
			arrayOf(intArrayOf(1, 2, 1), intArrayOf(3, 1), intArrayOf(1, 2, 1), intArrayOf(1, 3)), //T
			arrayOf(intArrayOf(2, 1, 1), intArrayOf(3, 1), intArrayOf(1, 1, 2), intArrayOf(1, 3)), //J
			arrayOf(intArrayOf(1, 2, 1), intArrayOf(2, 2), intArrayOf(1, 2, 1), intArrayOf(2, 2)), //S
			arrayOf(intArrayOf(1), intArrayOf(1), intArrayOf(1), intArrayOf(1)), //I1
			arrayOf(intArrayOf(1, 1), intArrayOf(2), intArrayOf(1, 1), intArrayOf(2)), //I2
			arrayOf(intArrayOf(1, 1, 1), intArrayOf(3), intArrayOf(1, 1, 1), intArrayOf(3)), //I3
			arrayOf(intArrayOf(2, 1), intArrayOf(2, 1), intArrayOf(1, 2), intArrayOf(1, 2)) //L3
		)
		val PIECES_LOWESTS = arrayOf(
			arrayOf(intArrayOf(1, 1, 1, 1), intArrayOf(3), intArrayOf(2, 2, 2, 2), intArrayOf(3)), //I
			arrayOf(intArrayOf(1, 1, 1), intArrayOf(2, 2), intArrayOf(2, 1, 1), intArrayOf(0, 2)), //L
			arrayOf(intArrayOf(1, 1), intArrayOf(1, 1), intArrayOf(1, 1), intArrayOf(1, 1)), //O
			arrayOf(intArrayOf(0, 1, 1), intArrayOf(2, 1), intArrayOf(1, 2, 2), intArrayOf(2, 1)), //Z
			arrayOf(intArrayOf(1, 1, 1), intArrayOf(2, 1), intArrayOf(1, 2, 1), intArrayOf(1, 2)), //T
			arrayOf(intArrayOf(1, 1, 1), intArrayOf(2, 0), intArrayOf(1, 1, 2), intArrayOf(2, 2)), //J
			arrayOf(intArrayOf(1, 1, 0), intArrayOf(1, 2), intArrayOf(2, 2, 1), intArrayOf(1, 2)), //S
			arrayOf(intArrayOf(0), intArrayOf(0), intArrayOf(0), intArrayOf(0)), //I1
			arrayOf(intArrayOf(0, 0), intArrayOf(1), intArrayOf(1, 1), intArrayOf(1)), //I2
			arrayOf(intArrayOf(1, 1, 1), intArrayOf(2), intArrayOf(1, 1, 1), intArrayOf(2)), //I3
			arrayOf(intArrayOf(1, 1), intArrayOf(1, 0), intArrayOf(0, 1), intArrayOf(1, 1)) //L3
		)
	}
}
