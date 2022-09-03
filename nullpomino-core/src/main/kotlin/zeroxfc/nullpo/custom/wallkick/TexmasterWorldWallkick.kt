/*
 * Copyright (c) 2021-2021,
 * This library class was created by 0xFC963F18DC21 / Shots243
 * It is part of an extension library for the game NullpoMino (copyright 2021-2021)
 *
 * Kotlin converted and modified by Venom=Nhelv
 *
 * Herewith shall the term "Library Creator" be given to 0xFC963F18DC21.
 * Herewith shall the term "Game Creator" be given to the original creator of NullpoMino, NullNoname.
 *
 * THIS LIBRARY AND MODE PACK WAS NOT MADE IN ASSOCIATION WITH THE GAME CREATOR.
 *
 * Repository: https://github.com/Shots243/ModePile
 *
 * When using this library in a mode / library pack of your own, the following
 * conditions must be satisfied:
 *     - This license must remain visible at the top of the document, unmodified.
 *     - You are allowed to use this library for any modding purpose.
 *         - If this is the case, the Library Creator must be credited somewhere.
 *             - Source comments only are fine, but in a README is recommended.
 *     - Modification of this library is allowed, but only in the condition that a
 *       pull request is made to merge the changes to the repository.
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

import mu.nu.nullpo.game.component.Controller
import mu.nu.nullpo.game.component.Field
import mu.nu.nullpo.game.component.Piece
import mu.nu.nullpo.game.subsystem.wallkick.BaseStandardWallkick

/**
 * SRS with symmetric I-piece kicks
 */
class TexmasterWorldWallkick:BaseStandardWallkick() {
	/*
     * Get kick table
     */
	override fun getKickTable(x:Int, y:Int, rtDir:Int, rtOld:Int, rtNew:Int, allowUpward:Boolean, piece:Piece, field:Field,
		ctrl:Controller?):Array<Array<IntArray>>? {
		var kicktable:Array<Array<IntArray>>? = null
		when(rtDir) {
			2 -> { // 180-degree rotation
				kicktable = when(piece.id) {
					Piece.PIECE_I -> WALLKICK_I_180
					else -> WALLKICK_NORMAL_180
				}
			}
			-1 -> { // Left rotation
				kicktable = when(piece.id) {
					Piece.PIECE_I -> WALLKICK_I_L
					Piece.PIECE_I2 -> WALLKICK_I2_L
					Piece.PIECE_I3 -> WALLKICK_I3_L
					Piece.PIECE_L3 -> WALLKICK_L3_L
					else -> WALLKICK_NORMAL_L
				}
			}
			1 -> { // Right rotation
				kicktable = when(piece.id) {
					Piece.PIECE_I -> WALLKICK_I_R
					Piece.PIECE_I2 -> WALLKICK_I2_R
					Piece.PIECE_I3 -> WALLKICK_I3_R
					Piece.PIECE_L3 -> WALLKICK_L3_R
					else -> WALLKICK_NORMAL_R
				}
			}
		}
		return kicktable
	}

	companion object {
		// Wallkick data
		private val WALLKICK_NORMAL_L = arrayOf(
			arrayOf(intArrayOf(-1, 0), intArrayOf(-1, -1), intArrayOf(0, 2), intArrayOf(-1, 2)),
			arrayOf(intArrayOf(-1, 0), intArrayOf(-1, 1), intArrayOf(0, -2), intArrayOf(-1, -2)),
			arrayOf(intArrayOf(1, 0), intArrayOf(1, -1), intArrayOf(0, 2), intArrayOf(1, 2)),
			arrayOf(intArrayOf(1, 0), intArrayOf(1, 1), intArrayOf(0, -2), intArrayOf(1, -2))
		)
		private val WALLKICK_NORMAL_R = arrayOf(
			arrayOf(intArrayOf(-1, 0), intArrayOf(-1, -1), intArrayOf(0, 2), intArrayOf(-1, 2)),
			arrayOf(intArrayOf(1, 0), intArrayOf(1, 1), intArrayOf(0, -2), intArrayOf(1, -2)),
			arrayOf(intArrayOf(1, 0), intArrayOf(1, -1), intArrayOf(0, 2), intArrayOf(1, 2)),
			arrayOf(intArrayOf(-1, 0), intArrayOf(-1, 1), intArrayOf(0, -2), intArrayOf(-1, -2))
		)
		private val WALLKICK_I_L = arrayOf(
			arrayOf(intArrayOf(-2, 0), intArrayOf(1, 0), intArrayOf(1, -2), intArrayOf(-2, 1)),
			arrayOf(intArrayOf(-2, 0), intArrayOf(1, 0), intArrayOf(-2, -1), intArrayOf(1, 2)),
			arrayOf(intArrayOf(2, 0), intArrayOf(-1, 0), intArrayOf(2, -1), intArrayOf(-1, 1)),
			arrayOf(intArrayOf(-1, 0), intArrayOf(2, 0), intArrayOf(-1, -2), intArrayOf(2, 1))
		)
		private val WALLKICK_I_R = arrayOf(
			arrayOf(intArrayOf(-2, 0), intArrayOf(1, 0), intArrayOf(1, -2), intArrayOf(-2, 1)),
			arrayOf(intArrayOf(-1, 0), intArrayOf(2, 0), intArrayOf(-1, -2), intArrayOf(2, 1)),
			arrayOf(intArrayOf(2, 0), intArrayOf(-1, 0), intArrayOf(2, -1), intArrayOf(-1, 1)),
			arrayOf(intArrayOf(-2, 0), intArrayOf(1, 0), intArrayOf(-2, -1), intArrayOf(1, 2))
		)
		private val WALLKICK_I2_L = arrayOf(
			arrayOf(intArrayOf(-1, 0), intArrayOf(-0, -1), intArrayOf(-1, -2)),
			arrayOf(intArrayOf(-0, 1), intArrayOf(-1, 0), intArrayOf(-1, 1)),
			arrayOf(intArrayOf(1, 0), intArrayOf(-0, 1), intArrayOf(1, 0)),
			arrayOf(intArrayOf(-0, -1), intArrayOf(1, 0), intArrayOf(1, 1))
		)
		private val WALLKICK_I2_R = arrayOf(
			arrayOf(intArrayOf(0, -1), intArrayOf(-1, 0), intArrayOf(-1, -1)),
			arrayOf(intArrayOf(1, 0), intArrayOf(0, -1), intArrayOf(1, 0)),
			arrayOf(intArrayOf(0, 1), intArrayOf(1, 0), intArrayOf(1, -1)),
			arrayOf(intArrayOf(-1, 0), intArrayOf(0, 1), intArrayOf(-1, 2))
		)
		private val WALLKICK_I3_L = arrayOf(
			arrayOf(intArrayOf(-1, 0), intArrayOf(1, 0), intArrayOf(0, 0), intArrayOf(0, 0)),
			arrayOf(intArrayOf(1, 0), intArrayOf(-1, 0), intArrayOf(0, -1), intArrayOf(0, 1)),
			arrayOf(intArrayOf(1, 0), intArrayOf(-1, 0), intArrayOf(0, 2), intArrayOf(0, -2)),
			arrayOf(intArrayOf(-1, 0), intArrayOf(1, 0), intArrayOf(0, -1), intArrayOf(0, 1))
		)
		private val WALLKICK_I3_R = arrayOf(
			arrayOf(intArrayOf(1, 0), intArrayOf(-1, 0), intArrayOf(0, 1), intArrayOf(0, -1)),
			arrayOf(intArrayOf(1, 0), intArrayOf(-1, 0), intArrayOf(0, -2), intArrayOf(0, 2)),
			arrayOf(intArrayOf(-1, 0), intArrayOf(1, 0), intArrayOf(0, 1), intArrayOf(0, -1)),
			arrayOf(intArrayOf(-1, 0), intArrayOf(1, 0), intArrayOf(0, 0), intArrayOf(0, 0))
		)
		private val WALLKICK_L3_L = arrayOf(
			arrayOf(intArrayOf(-0, -1), intArrayOf(-0, 1)),
			arrayOf(intArrayOf(-1, 0), intArrayOf(1, 0)), arrayOf(intArrayOf(-0, 1), intArrayOf(-0, -1)),
			arrayOf(intArrayOf(1, 0), intArrayOf(-1, 0))
		)
		private val WALLKICK_L3_R = arrayOf(
			arrayOf(intArrayOf(-1, 0), intArrayOf(1, 0)),
			arrayOf(intArrayOf(0, -1), intArrayOf(0, 1)), arrayOf(intArrayOf(1, 0), intArrayOf(-1, 0)),
			arrayOf(intArrayOf(0, 1), intArrayOf(0, -1))
		)
		// 180-degree rotation wallkick data
		private val WALLKICK_NORMAL_180 = arrayOf(
			arrayOf(
				intArrayOf(1, 0), intArrayOf(2, 0), intArrayOf(1, 1), intArrayOf(2, 1), intArrayOf(-1, 0), intArrayOf(-2, 0),
				intArrayOf(-1, 1), intArrayOf(-2, 1), intArrayOf(0, -1), intArrayOf(3, 0), intArrayOf(-3, 0)
			),
			arrayOf(
				intArrayOf(0, 1), intArrayOf(0, 2), intArrayOf(-1, 1), intArrayOf(-1, 2), intArrayOf(0, -1), intArrayOf(0, -2),
				intArrayOf(-1, -1), intArrayOf(-1, -2), intArrayOf(1, 0), intArrayOf(0, 3), intArrayOf(0, -3)
			),
			arrayOf(
				intArrayOf(-1, 0), intArrayOf(-2, 0), intArrayOf(-1, -1), intArrayOf(-2, -1), intArrayOf(1, 0), intArrayOf(2, 0),
				intArrayOf(1, -1), intArrayOf(2, -1), intArrayOf(0, 1), intArrayOf(-3, 0), intArrayOf(3, 0)
			),
			arrayOf(
				intArrayOf(0, 1), intArrayOf(0, 2), intArrayOf(1, 1), intArrayOf(1, 2), intArrayOf(0, -1), intArrayOf(0, -2),
				intArrayOf(1, -1), intArrayOf(1, -2), intArrayOf(-1, 0), intArrayOf(0, 3), intArrayOf(0, -3)
			)
		)
		private val WALLKICK_I_180 = arrayOf(
			arrayOf(intArrayOf(-1, 0), intArrayOf(-2, 0), intArrayOf(1, 0), intArrayOf(2, 0), intArrayOf(0, 1)),
			arrayOf(intArrayOf(0, 1), intArrayOf(0, 2), intArrayOf(0, -1), intArrayOf(0, -2), intArrayOf(-1, 0)),
			arrayOf(intArrayOf(1, 0), intArrayOf(2, 0), intArrayOf(-1, 0), intArrayOf(-2, 0), intArrayOf(0, -1)),
			arrayOf(intArrayOf(0, 1), intArrayOf(0, 2), intArrayOf(0, -1), intArrayOf(0, -2), intArrayOf(1, 0))
		)
	}
}
