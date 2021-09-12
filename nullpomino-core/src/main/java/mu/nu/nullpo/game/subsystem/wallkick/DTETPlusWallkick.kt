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
package mu.nu.nullpo.game.subsystem.wallkick

import mu.nu.nullpo.game.component.Controller
import mu.nu.nullpo.game.component.Field
import mu.nu.nullpo.game.component.Piece
import mu.nu.nullpo.game.component.WallkickResult

/** DTETPlus Wallkick - An extension of the ClassicPlusWallkick */
class DTETPlusWallkick:DTETWallkick() {

	/* Wallkick main method */
	override fun executeWallkick(x:Int, y:Int, rtDir:Int, rtOld:Int, rtNew:Int, allowUpward:Boolean,
		piece:Piece,
		field:Field, ctrl:Controller?):WallkickResult? {
		var x2:Int
		var y2:Int

		for(aWALLKICK in WALLKICK) {
			x2 = if(rtDir<0||rtDir==2)
				aWALLKICK[0]
			else
				-aWALLKICK[0]
			y2 = aWALLKICK[1]

			if(piece.big) {
				x2 *= 2
				y2 *= 2
			}

			if(!piece.checkCollision(x+x2, y+y2, rtNew, field)) return WallkickResult(x2, y2, rtNew)
		}
		var check = 0
		if(piece.big) check = 1
		// IのWallkick
		if(piece.id==Piece.PIECE_I&&(rtNew==Piece.DIRECTION_UP||rtNew==Piece.DIRECTION_DOWN))
			for(i in check..check*2) {
				var temp = 0

				if(!piece.checkCollision(x-1-i, y, rtNew, field))
					temp = -1-i
				else if(!piece.checkCollision(x+1+i, y, rtNew, field))
					temp = 1+i
				else if(!piece.checkCollision(x+2+i, y, rtNew, field)) temp = 2+i

				if(temp!=0) return WallkickResult(temp, 0, rtNew)
			}
		// Iの床蹴り (接地している場合のみ）
		if(piece.id==Piece.PIECE_I&&allowUpward&&(rtNew==Piece.DIRECTION_LEFT||rtNew==Piece.DIRECTION_RIGHT)&&
			piece.checkCollision(x, y+1, field))

			for(i in check..check*2) {
				var temp = 0

				if(!piece.checkCollision(x, y-1-i, rtNew, field))
					temp = -1-i
				else if(!piece.checkCollision(x, y-2-i, rtNew, field)) temp = -2-i

				if(temp!=0) return WallkickResult(0, temp, rtNew)

			}
		return null
	}

	companion object {
		/** Wallkick table */
		private val WALLKICK = arrayOf(intArrayOf(-1, 0), intArrayOf(1, 0), intArrayOf(0, 1), intArrayOf(-1, 1), intArrayOf(1, 1),
			intArrayOf(0, -1), intArrayOf(-1, -1), intArrayOf(1, -1))
	}
}
