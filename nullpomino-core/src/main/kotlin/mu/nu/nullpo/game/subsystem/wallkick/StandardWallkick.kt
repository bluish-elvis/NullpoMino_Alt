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
package mu.nu.nullpo.game.subsystem.wallkick

import mu.nu.nullpo.game.component.Controller
import mu.nu.nullpo.game.component.Field
import mu.nu.nullpo.game.component.Piece

/** SRS */
class StandardWallkick:BaseStandardWallkick() {
	/* Get kick table */
	override fun getKickTable(x:Int, y:Int, rtDir:Int, rtOld:Int, rtNew:Int, allowUpward:Boolean,
		piece:Piece, field:Field, ctrl:Controller?) = when(rtDir) {
		// 180-degree rotation
		2 -> when(piece.id) {
			Piece.PIECE_I -> WALLKICK_I_180
			else -> WALLKICK_NORMAL_180
		}
		// Left rotation
		-1 -> when(piece.id) {
			Piece.PIECE_I -> WALLKICK_I_L
			Piece.PIECE_I2 -> WALLKICK_I2_L
			Piece.PIECE_I3 -> WALLKICK_I3_L
			Piece.PIECE_L3 -> WALLKICK_L3_L
			else -> WALLKICK_NORMAL_L
		}
		// Right rotation
		1 -> when(piece.id) {
			Piece.PIECE_I -> WALLKICK_I_R
			Piece.PIECE_I2 -> WALLKICK_I2_R
			Piece.PIECE_I3 -> WALLKICK_I3_R
			Piece.PIECE_L3 -> WALLKICK_L3_R
			else -> WALLKICK_NORMAL_R
		}
		else -> null
	}

	companion object {
		// Wallkick data
		private val WALLKICK_NORMAL_L = listOf(
			listOf(listOf(1, 0), listOf(1, -1), listOf(0, 2), listOf(1, 2), listOf(0, -1)), // 0>>3
			listOf(listOf(1, 0), listOf(1, 1), listOf(0, -2), listOf(1, -2), listOf(0, -1)), // 1>>0
			listOf(listOf(-1, 0), listOf(-1, -1), listOf(0, 2), listOf(-1, 2), listOf(0, -1)), // 2>>1
			listOf(listOf(-1, 0), listOf(-1, 1), listOf(0, -2), listOf(-1, -2), listOf(0, -1))
		)// 3>>2
		private val WALLKICK_NORMAL_R = listOf(
			listOf(listOf(-1, 0), listOf(-1, -1), listOf(0, 2), listOf(-1, 2), listOf(0, -1)), // 0>>1
			listOf(listOf(1, 0), listOf(1, 1), listOf(0, -2), listOf(1, -2), listOf(0, -1)), // 1>>2
			listOf(listOf(1, 0), listOf(1, -1), listOf(0, 2), listOf(1, 2), listOf(0, -1)), // 2>>3
			listOf(listOf(-1, 0), listOf(-1, 1), listOf(0, -2), listOf(-1, -2), listOf(0, -1))
		)// 3>>0
		private val WALLKICK_I_L = listOf(
			listOf(listOf(-1, 0), listOf(2, 0), listOf(-1, -2), listOf(2, 1)), // 0>>3
			listOf(listOf(2, 0), listOf(-1, 0), listOf(2, -1), listOf(-1, 2)), // 1>>0
			listOf(listOf(1, 0), listOf(-2, 0), listOf(1, 2), listOf(-2, -1)), // 2>>1
			listOf(listOf(-2, 0), listOf(1, 0), listOf(-2, 1), listOf(1, -2))
		)// 3>>2
		private val WALLKICK_I_R = listOf(
			listOf(listOf(-2, 0), listOf(1, 0), listOf(-2, 1), listOf(1, -2)), // 0>>1
			listOf(listOf(-1, 0), listOf(2, 0), listOf(-1, -2), listOf(2, 1)), // 1>>2
			listOf(listOf(2, 0), listOf(-1, 0), listOf(2, -1), listOf(-1, 2)), // 2>>3
			listOf(listOf(1, 0), listOf(-2, 0), listOf(1, 2), listOf(-2, -1))
		)// 3>>0
		private val WALLKICK_I2_L = listOf(
			listOf(listOf(1, 0), listOf(0, -1), listOf(1, -2)), // 0>>3
			listOf(listOf(0, 1), listOf(1, 0), listOf(1, 1)), // 1>>0
			listOf(listOf(-1, 0), listOf(0, 1), listOf(-1, 0)), // 2>>1
			listOf(listOf(0, -1), listOf(-1, 0), listOf(-1, 1))
		)// 3>>2
		private val WALLKICK_I2_R = listOf(
			listOf(listOf(0, -1), listOf(-1, 0), listOf(-1, -1)), // 0>>1
			listOf(listOf(1, 0), listOf(0, -1), listOf(1, 0)), // 1>>2
			listOf(listOf(0, 1), listOf(1, 0), listOf(1, -1)), // 2>>3
			listOf(listOf(-1, 0), listOf(0, 1), listOf(-1, 2))
		)// 3>>0
		private val WALLKICK_I3_L = listOf(
			listOf(listOf(1, 0), listOf(-1, 0), listOf(0, 0), listOf(0, 0)), // 0>>3
			listOf(listOf(-1, 0), listOf(1, 0), listOf(0, -1), listOf(0, 1)), // 1>>0
			listOf(listOf(-1, 0), listOf(1, 0), listOf(0, 2), listOf(0, -2)), // 2>>1
			listOf(listOf(1, 0), listOf(-1, 0), listOf(0, -1), listOf(0, 1))
		)// 3>>2
		private val WALLKICK_I3_R = listOf(
			listOf(listOf(1, 0), listOf(-1, 0), listOf(0, 1), listOf(0, -1)), // 0>>1
			listOf(listOf(1, 0), listOf(-1, 0), listOf(0, -2), listOf(0, 2)), // 1>>2
			listOf(listOf(-1, 0), listOf(1, 0), listOf(0, 1), listOf(0, -1)), // 2>>3
			listOf(listOf(-1, 0), listOf(1, 0), listOf(0, 0), listOf(0, 0))
		)// 3>>0
		private val WALLKICK_L3_L = listOf(
			listOf(listOf(0, -1), listOf(0, 1)), // 0>>3
			listOf(listOf(1, 0), listOf(-1, 0)), // 1>>0
			listOf(listOf(0, 1), listOf(0, -1)), // 2>>1
			listOf(listOf(-1, 0), listOf(1, 0))
		)// 3>>2
		private val WALLKICK_L3_R = listOf(
			listOf(listOf(-1, 0), listOf(1, 0)), // 0>>1
			listOf(listOf(0, -1), listOf(0, 1)), // 1>>2
			listOf(listOf(1, 0), listOf(-1, 0)), // 2>>3
			listOf(listOf(0, 1), listOf(0, -1))
		)// 3>>0

		// 180-degree rotation wallkick data
		private val WALLKICK_NORMAL_180 = listOf(
			listOf(
				listOf(1, 0), listOf(2, 0), listOf(1, 1), listOf(2, 1), listOf(-1, 0), listOf(-2, 0),
				listOf(-1, 1), listOf(-2, 1), listOf(0, -1), listOf(3, 0), listOf(-3, 0)
			), // 0>>2─┐
			listOf(
				listOf(0, 1), listOf(0, 2), listOf(-1, 1), listOf(-1, 2), listOf(0, -1), listOf(0, -2),
				listOf(-1, -1), listOf(-1, -2), listOf(1, 0), listOf(0, 3), listOf(0, -3)
			), // 1>>3─┼┐
			listOf(
				listOf(-1, 0), listOf(-2, 0), listOf(-1, -1), listOf(-2, -1), listOf(1, 0), listOf(2, 0),
				listOf(1, -1), listOf(2, -1), listOf(0, 1), listOf(-3, 0), listOf(3, 0)
			), // 2>>0─┘│
			listOf(
				listOf(0, 1), listOf(0, 2), listOf(1, 1), listOf(1, 2), listOf(0, -1), listOf(0, -2),
				listOf(1, -1), listOf(1, -2), listOf(-1, 0), listOf(0, 3), listOf(0, -3)
			)
		)// 3>>1──┘
		private val WALLKICK_I_180 = listOf(
			listOf(listOf(-1, 0), listOf(-2, 0), listOf(1, 0), listOf(2, 0), listOf(0, 1)), // 0>>2─┐
			listOf(listOf(0, 1), listOf(0, 2), listOf(0, -1), listOf(0, -2), listOf(-1, 0)), // 1>>3─┼┐
			listOf(listOf(1, 0), listOf(2, 0), listOf(-1, 0), listOf(-2, 0), listOf(0, -1)), // 2>>0─┘│
			listOf(listOf(0, 1), listOf(0, 2), listOf(0, -1), listOf(0, -2), listOf(1, 0))
		)// 3>>1──┘
	}
}
