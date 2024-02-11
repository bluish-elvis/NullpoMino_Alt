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
package mu.nu.nullpo.game.subsystem.wallkick

import mu.nu.nullpo.game.component.Controller
import mu.nu.nullpo.game.component.Field
import mu.nu.nullpo.game.component.Piece
import mu.nu.nullpo.game.event.WallkickResult

/** ClassicWallkick - クラシックルールなWallkick (旧VersionのCLASSIC1と2相当） */
class ClassicWallkick:Wallkick {
	/* Wallkick */
	override fun executeWallkick(x:Int, y:Int, rtDir:Int, rtOld:Int, rtNew:Int, allowUpward:Boolean, piece:Piece,
		field:Field, ctrl:Controller?): WallkickResult? {
		var check = 0
		if(piece.big) check = 1

		// 通常のWallkick (I以外）
		if(piece.id!=Piece.PIECE_I)
			if(checkCollisionKick(piece, x, y, rtNew, field)||piece.type==Piece.Shape.I2
				||piece.type==Piece.Shape.L3) {
				var temp = 0

				if(!piece.checkCollision(x-1-check, y, rtNew, field)) temp = -1-check
				if(!piece.checkCollision(x+1+check, y, rtNew, field)) temp = 1+check

				if(temp!=0) return WallkickResult(temp, 0, rtNew)
			}

		return null
	}

	/** Wallkick可能かどうか調べる
	 * @param piece Blockピース
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param rt Direction
	 * @param fld field
	 * @return Wallkick可能ならtrue
	 */
	private fun checkCollisionKick(piece:Piece, x:Int, y:Int, rt:Int, fld:Field):Boolean {
		// Bigでは専用処理
		if(piece.big) return checkCollisionKickBig(piece, x, y, rt, fld)

		for(i in 0..<piece.maxBlock)
			if(piece.dataX[rt][i]!=1+piece.dataOffsetX[rt]) {
				val x2 = x+piece.dataX[rt][i]
				val y2 = y+piece.dataY[rt][i]

				if(x2>=fld.width) return true
				if(y2>=fld.height) return true
				if(fld.getCoordAttribute(x2, y2)==Field.COORD_WALL) return true
				if(fld.getCoordAttribute(x2, y2)!=Field.COORD_VANISH&&!fld.getBlockEmpty(x2, y2))
					return true
			}

		return false
	}

	/** Wallkick可能かどうか調べる (Big用）
	 * @param piece Blockピース
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param rt Direction
	 * @param fld field
	 * @return Wallkick可能ならtrue
	 */
	private fun checkCollisionKickBig(piece:Piece, x:Int, y:Int, rt:Int, fld:Field):Boolean {
		for(i in 0..<piece.maxBlock)
			if(piece.dataX[rt][i]!=1+piece.dataOffsetX[rt]) {
				val x2 = x+piece.dataX[rt][i]*2
				val y2 = y+piece.dataY[rt][i]*2

				// 4Block分調べる
				for(k in 0..1)
					for(l in 0..1) {
						val x3 = x2+k
						val y3 = y2+l

						if(x3>=fld.width) return true
						if(y3>=fld.height) return true
						if(fld.getCoordAttribute(x3, y3)==Field.COORD_WALL) return true
						if(fld.getCoordAttribute(x3, y3)!=Field.COORD_VANISH&&!fld.getBlockEmpty(x3, y3))
							return true
					}
			}

		return false
	}
}
