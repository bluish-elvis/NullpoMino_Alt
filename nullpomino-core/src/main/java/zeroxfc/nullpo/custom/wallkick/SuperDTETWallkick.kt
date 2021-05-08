/*
    Copyright (c) 2010, NullNoname
    All rights reserved.

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
package zeroxfc.nullpo.custom.wallkick

import mu.nu.nullpo.game.component.*
import mu.nu.nullpo.game.subsystem.wallkick.Wallkick

/**
 * DTET Wallkick - An extension of the Classic Wallkick system for DRS by Zircean
 * Modified by 0xFC963F18DC21 to add a 1 up floorkick
 */
class SuperDTETWallkick:Wallkick {
	/*
     * Wallkick main method
     */
	override fun executeWallkick(x:Int, y:Int, rtDir:Int, rtOld:Int, rtNew:Int, allowUpward:Boolean, piece:Piece, field:Field,
		ctrl:Controller?):WallkickResult? {
		var x2:Int
		var y2:Int
		for(i in WALLKICK.indices) {
			x2 = if(rtDir<0||rtDir==2) WALLKICK[i][0] else -WALLKICK[i][0]
			y2 = WALLKICK[i][1]

			if(piece.big) {
				x2 *= 2
				y2 *= 2
			}
			if(!piece.checkCollision(x+x2, y+y2, rtNew, field))
				return WallkickResult(x2, y2, rtNew)

		}
		return null
	}

	companion object {
		/**
		 * Wallkick table
		 */
		private val WALLKICK = arrayOf(intArrayOf(-1, 0), intArrayOf(1, 0), intArrayOf(0, 1), intArrayOf(-1, 1), intArrayOf(1, 1),
			intArrayOf(0, -1))
	}
}