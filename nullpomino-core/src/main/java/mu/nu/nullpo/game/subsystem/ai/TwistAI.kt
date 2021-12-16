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
package mu.nu.nullpo.game.subsystem.ai

import mu.nu.nullpo.game.component.Field
import mu.nu.nullpo.game.component.Piece
import mu.nu.nullpo.game.play.GameEngine

/** TwisterHaveAI (WIP) */
class TwistAI:BasicAI() {
	override val name = "TWISTER"

	override fun thinkMain(engine:GameEngine, x:Int, y:Int, rt:Int, rtOld:Int, fld:Field, piece:Piece,
		nextpiece:Piece?, holdpiece:Piece?, depth:Int):Int {
		var pts = 0

		// Add points for being adjacent to other blocks
		if(piece.checkCollision(x-1, y, fld)) pts += 1
		if(piece.checkCollision(x+1, y, fld)) pts += 1
		if(piece.checkCollision(x, y-1, fld)) pts += 100

		// Number of holes and valleys needing an I piece (before placement)
		val holeBefore = fld.howManyHoles
		val lidBefore = fld.howManyLidAboveHoles
		val needIValleyBefore = fld.totalValleyNeedIPiece
		// Field height (before placement)
		val heightBefore = fld.highestBlockY
		// Twister flag
		var twist = false
		if(piece.type==Piece.Shape.T&&rtOld!=-1&&fld.isTwistSpot(x, y, piece.big)) twist = true
		// TwisterHolecount (before placement)
		val tslotBefore:Int = fld.getTSlotLineClearAll(false)
		//if( (nextpiece.id == Piece.PIECE_T) || ((holdpiece != null) && (holdpiece.id == Piece.PIECE_T)) ) {
		//}

		// Place the piece
		if(!piece.placeToField(x, y, rt, fld)) return 0

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

		// Danger flag
		val danger = heightAfter<=12

		// Additional points for lower placements
		pts += if(!danger&&depth==0)
			y*10
		else
			y*20

		// LinescountAdditional points in
		if(lines==1&&!danger&&depth==0&&heightAfter>=16&&holeBefore<3&&!twist&&engine.combo<1) return 0
		if(!danger&&depth==0) {
			if(lines==1) pts += 10
			if(lines==2) pts += 50
			if(lines==3) pts += 100
			if(lines>=4) pts += 100000
		} else {
			if(lines==1) pts += 5000
			if(lines==2) pts += 10000
			if(lines==3) pts += 30000
			if(lines>=4) pts += 100000
		}

		if(lines<4&&!allclear) {
			// Number of holes and valleys needing an I piece (after placement)
			val holeAfter = fld.howManyHoles
			val lidAfter = fld.howManyLidAboveHoles
			val needIValleyAfter = fld.totalValleyNeedIPiece
			// TwisterHolecount (after placement)
			val tslotAfter:Int = fld.getTSlotLineClearAll(false)
			//if( (nextpiece.id == Piece.PIECE_T) || ((holdpiece != null) && (holdpiece.id == Piece.PIECE_T)) ) {
			//}
			var newtslot = false

			if(!danger&&tslotAfter>tslotBefore&&tslotAfter==1&&holeAfter==holeBefore+1) {
				// NewlyTwisterHole and additional points can be
				pts += 100000
				newtslot = true

				// HoldTBe sure to give
				if(nextpiece!!.id!=Piece.PIECE_T&&holdpiece!=null&&holdpiece.type==Piece.Shape.T) forceHold = true
			} else if(tslotAfter<tslotBefore&&!twist&&!danger)
			// TwisterBreaking holeNG
				return 0
			else if(holeAfter>holeBefore) {
				// Demerits for new holes
				pts -= (holeAfter-holeBefore)*10
				if(depth==0) return 0
			} else if(holeAfter<holeBefore)
			// Add points for reduction in number of holes
				pts += if(!danger)
					(holeBefore-holeAfter)*5
				else
					(holeBefore-holeAfter)*10

			if(lidAfter>lidBefore&&!newtslot) {
				// Is riding on top of the holeBlockIncreasing the deduction
				pts -= if(!danger)
					(lidAfter-lidBefore)*10
				else
					(lidAfter-lidBefore)*20
			} else if(lidAfter<lidBefore)
			// Add points for reduction in number blocks above holes
				pts += if(!danger)
					(lidBefore-lidAfter)*10
				else
					(lidBefore-lidAfter)*20

			if(twist&&lines>=1&&holeAfter<holeBefore)
			// Twister bonus
				pts += 100000*lines

			if(needIValleyAfter>needIValleyBefore&&needIValleyAfter>=2) {
				// 2One or moreIDeduction and make a hole type is required
				pts -= (needIValleyAfter-needIValleyBefore)*10
				if(depth==0) return 0
			} else if(needIValleyAfter<needIValleyBefore)
			// Add points for reduction in number of holes
				pts += if(depth==0&&!danger)
					(needIValleyBefore-needIValleyAfter)*10
				else
					(needIValleyBefore-needIValleyAfter)*20

			if(heightBefore<heightAfter) {
				// Add points for reducing the height
				pts += if(depth==0&&!danger)
					(heightAfter-heightBefore)*10
				else
					(heightAfter-heightBefore)*20
			} else if(heightBefore>heightAfter)
			// Demerits for increase in height
				if(depth>0||danger) pts -= (heightBefore-heightAfter)*4

			// Combo bonus
			if(lines>=1&&engine.comboType!=GameEngine.COMBO_TYPE_DISABLE) pts += lines*engine.combo*50
		}

		return pts
	}
}
