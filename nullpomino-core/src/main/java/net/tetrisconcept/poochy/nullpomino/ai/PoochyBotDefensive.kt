/*
 * Copyright (c) 2010-2021, NullNoname
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

package net.tetrisconcept.poochy.nullpomino.ai

import mu.nu.nullpo.game.component.Field
import mu.nu.nullpo.game.component.Piece

class PoochyBotDefensive:PoochyBot() {
	/* AI's name */
	override val name = "${super.name} (Defensive)"

	/**
	 * Think routine
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param rt Direction
	 * @param rtOld Direction before rotation (-1: None)
	 * @param fld Field (Can be modified without problems)
	 * @param piece Piece
	 * @param depth Compromise level (ranges from 0 through getMaxThinkDepth-1)
	 * @return Evaluation score
	 */
	override fun thinkMain(x:Int, y:Int, rt:Int, rtOld:Int, fld:Field, piece:Piece?, depth:Int):Int {
		var pts = 0

		// Add points for being adjacent to other blocks
		if(piece!!.checkCollision(x-1, y, fld)) pts += 1
		if(piece.checkCollision(x+1, y, fld)) pts += 1
		if(piece.checkCollision(x, y-1, fld)) pts += 1000

		val width = fld.width
		//int height = fld.getHeight();

		val xMin = piece.minimumBlockX+x
		val xMax = piece.maximumBlockX+x

		// Number of holes and valleys needing an I piece (before placement)
		val holeBefore = fld.howManyHoles
		//int lidBefore = fld.getHowManyLidAboveHoles();

		//Fetch depths.
		val depthsBefore = getColumnDepths(fld)
		var deepestY = -1
		//int deepestX = -1;
		for(i in 0 until width-1)
			if(depthsBefore[i]>deepestY) {
				deepestY = depthsBefore[i]
				//deepestX = i;
			}

		//Find valleys that need an I, J, or L.
		var needIValleyBefore = 0
		var needJValleyBefore = 0
		var needLValleyBefore = 0
		if(depthsBefore[0]>depthsBefore[1]) needIValleyBefore = (depthsBefore[0]-depthsBefore[1])/3
		if(depthsBefore[width-1]>depthsBefore[width-2]) needIValleyBefore = (depthsBefore[width-1]-depthsBefore[width-2])/3
		for(i in 1 until width-1) {
			val left = depthsBefore[i-1]
			val right = depthsBefore[i+1]
			val lowerSide = maxOf(left, right)
			val diff = depthsBefore[i]-lowerSide
			if(diff>=3) needIValleyBefore += diff/3
			if(left==right) {
				if(left==depthsBefore[i]+2) {
					needIValleyBefore++
					needLValleyBefore--
					needJValleyBefore--
				} else if(left==depthsBefore[i]+1) {
					needLValleyBefore++
					needJValleyBefore++
				}
			}
			if(diff%4==2) {
				if(left>right)
					needLValleyBefore += 2
				else if(left<right)
					needJValleyBefore += 2
				else {
					needJValleyBefore++
					needLValleyBefore++
				}
			}
		}
		if((depthsBefore[0]-depthsBefore[1])%4==2) needJValleyBefore += 2
		if((depthsBefore[width-1]-depthsBefore[width-2])%4==2) needLValleyBefore += 2

		needJValleyBefore = needJValleyBefore shr 1
		needLValleyBefore = needLValleyBefore shr 1

		// Field height (before placement)
		val heightBefore = fld.highestBlockY
		// Twister flag
		var twist = false
		if(piece.id==Piece.PIECE_T&&rtOld!=-1&&fld.isTwistSpot(x, y, piece.big)) {
			twist = true
		}

		//Does move fill in valley with an I piece?
		var valley = 0
		if(piece.id==Piece.PIECE_I) {
			if(xMin==xMax&&0<=xMin&&xMin<width) {
				//if (DEBUG_ALL) log.debug("actualX = " + xMin);
				val xDepth = depthsBefore[xMin]
				var sideDepth = -1
				if(xMin>0) sideDepth = depthsBefore[xMin-1]
				if(xMin<width-1) sideDepth = maxOf(sideDepth, depthsBefore[xMin+1])
				valley = xDepth-sideDepth
				//if (DEBUG_ALL) log.debug("valley = " + valley);
			}
		}

		// Place the piece
		if(!piece.placeToField(x, y, rt, fld)) {
			if(DEBUG_ALL)
				log.debug("End of thinkMain($x, $y, $rt, $rtOld, fld, piece ${piece.id}, $depth). pts = 0 (Cannot place piece)")
			return Integer.MIN_VALUE
		}

		// Line clear
		val lines = fld.checkLine()
		if(lines>0) {
			fld.clearLine()
			fld.downFloatingBlocks()
		}

		// All clear
		val allclear = fld.isEmpty
		if(allclear) pts += 500000

		// Field height (after clears)
		val heightAfter = fld.highestBlockY

		val depthsAfter = getColumnDepths(fld)

		// Danger flag
		//boolean danger = (heightBefore <= 8);
		//Flag for really dangerously high stacks
		val peril = heightBefore<=4

		// Additional points for lower placements
		pts += y*20

		val holeAfter = fld.howManyHoles

		//Bonus points for filling in valley with an I piece
		var valleyBonus = 0
		if(valley==3&&xMax<width-1)
			valleyBonus = 40000
		else if(valley>=4) valleyBonus = 400000
		if(xMax==0) valleyBonus *= 2
		if(valley>0)
			if(DEBUG_ALL) log.debug("I piece xMax = $xMax, valley depth = $valley, valley bonus = $valleyBonus")
		pts += valleyBonus

		//Points for line clears
		if(peril) {
			if(lines==1) pts += 500000
			if(lines==2) pts += 1000000
			if(lines==3) pts += 30000000
			if(lines>=4) pts += 100000000
		} else {
			if(lines==1) pts += 50000
			if(lines==2) pts += 100000
			if(lines==3) pts += 300000
			if(lines>=4) pts += 1000000
		}

		if(lines<4&&!allclear) {
			// Number of holes and valleys needing an I piece (after placement)
			//int lidAfter = fld.getHowManyLidAboveHoles();

			//Find valleys that need an I, J, or L.
			var needIValleyAfter = 0
			var needJValleyAfter = 0
			var needLValleyAfter = 0
			if(depthsAfter[0]>depthsAfter[1]) needIValleyAfter = (depthsAfter[0]-depthsAfter[1])/3
			if(depthsAfter[width-1]>depthsAfter[width-2]) needIValleyAfter = (depthsAfter[width-1]-depthsAfter[width-2])/3
			for(i in 1 until width-1) {
				val left = depthsAfter[i-1]
				val right = depthsAfter[i+1]
				val lowerSide = maxOf(left, right)
				val diff = depthsAfter[i]-lowerSide
				if(diff>=3) needIValleyAfter += diff/3
				if(left==right) {
					if(left==depthsAfter[i]+2) {
						needIValleyAfter++
						needLValleyAfter--
						needJValleyAfter--
					} else if(left==depthsAfter[i]+1) {
						needLValleyAfter++
						needJValleyAfter++
					}
				}
				if(diff%4==2) {
					if(left>right)
						needLValleyAfter += 2
					else if(left<right)
						needJValleyAfter += 2
					else {
						needJValleyAfter++
						needLValleyAfter++
					}
				}
			}
			if((depthsAfter[0]-depthsAfter[1])%4==2) needJValleyAfter += 2
			if((depthsAfter[width-1]-depthsAfter[width-2])%4==2) needLValleyAfter += 2

			needJValleyAfter = needJValleyAfter shr 1
			needLValleyAfter = needLValleyAfter shr 1

			if(holeAfter>holeBefore) {
				// Demerits for new holes
				if(depth==0) return Integer.MIN_VALUE
				pts -= (holeAfter-holeBefore)*400
			} else if(holeAfter<holeBefore) {
				// Add points for reduction in number of holes
				pts += (holeBefore-holeAfter)*400+10000
			}

			/* if(lidAfter < lidBefore) {
			 * // Add points for reduction in number blocks above holes
			 * pts += (lidAfter - lidBefore) * 500;
			 * } */

			if(twist&&lines>=1) {
				// Twister Bonus - retained from Basic AI, but should never actually trigger
				pts += 100000*lines
			}

			//Bonuses and penalties for valleys that need I, J, or L.
			var needIValleyDiffScore = 0
			if(needIValleyBefore>0) needIValleyDiffScore = 1 shl needIValleyBefore
			if(needIValleyAfter>0) needIValleyDiffScore -= 1 shl needIValleyAfter

			var needLJValleyDiffScore = 0

			if(needJValleyBefore>1) needLJValleyDiffScore += 1 shl needJValleyBefore
			if(needJValleyAfter>1) needLJValleyDiffScore -= 1 shl needJValleyAfter
			if(needLValleyBefore>1) needLJValleyDiffScore += 1 shl needLValleyBefore
			if(needLValleyAfter>1) needLJValleyDiffScore -= 1 shl needLValleyAfter

			if(needIValleyDiffScore<0&&holeAfter>=holeBefore) {
				if(depth==0) return Integer.MIN_VALUE
				pts += needIValleyDiffScore*200
			} else if(needIValleyDiffScore>0) {
				pts += needIValleyDiffScore*200
			}
			if(needLJValleyDiffScore<0&&holeAfter>=holeBefore) {
				if(depth==0) return Integer.MIN_VALUE
				pts += needLJValleyDiffScore*40
			} else if(needLJValleyDiffScore>0) {
				pts += needLJValleyDiffScore*40
			}

			//Bonus for pyramidal stack
			val mid = width/2-1
			var d:Int
			for(i in 0 until mid-1) {
				d = depthsAfter[i]-depthsAfter[i+1]
				pts += if(d>=0) 10 else d
			}
			for(i in mid+2 until width) {
				d = depthsAfter[i]-depthsAfter[i-1]
				pts += if(d>=0) 10 else d
			}
			d = depthsAfter[mid-1]-depthsAfter[mid]
			pts += if(d>=0) 5 else d
			d = depthsAfter[mid+1]-depthsAfter[mid]
			pts += if(d>=0) 5 else d

			// Add points for reducing the height
			if(heightBefore<heightAfter)
				pts += (heightAfter-heightBefore)*20
			else if(heightBefore>heightAfter) pts -= (heightBefore-heightAfter)*4// Demerits for increase in height

			//Penalty for dangerous placements
			if(heightAfter<2) {
				val spawnMinX = width/2-2
				val spawnMaxX = width/2+1
				for(i in spawnMinX..spawnMaxX)
					if(depthsAfter[i]<2&&depthsAfter[i]<depthsBefore[i]) pts -= 2000000*(depthsBefore[i]-depthsAfter[i])
				if(heightBefore>=2&&depth==0) pts -= 2000000*(heightBefore-heightAfter)
			}
		}
		if(DEBUG_ALL)
			log.debug("End of thinkMain($x, $y, $rt, $rtOld, fld, piece ${piece.id}, $depth). pts = $pts")
		return pts
	}
}
