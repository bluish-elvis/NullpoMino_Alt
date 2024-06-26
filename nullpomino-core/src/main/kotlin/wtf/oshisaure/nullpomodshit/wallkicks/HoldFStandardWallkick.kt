/*
 Copyright (c) 2022-2024,
 This library class was created by 0xFC963F18DC21 / Shots243
 It is part of an extension library for the game NullpoMino (copyright 2010-2024)

 Converted to Kotlin and modified by Venom_Nhelv as bluish-elvis

 Herewith shall the term "Library Creator" be given to 0xFC963F18DC21.
 Herewith shall the term "Game Creator" be given to the original creator of NullpoMino, NullNoname.

 THIS LIBRARY AND MODE PACK WAS NOT MADE IN ASSOCIATION WITH THE GAME CREATOR.
 THIS KOTLIN VERSION WAS NOT MADE IN ASSOCIATION WITH THE LIBRARY CREATOR.

 Original Repository: https://github.com/Shots243/ModePile

 When using this library in a mode / library pack of your own, the following
 conditions must be satisfied:
     - This license must remain visible at the top of the document, unmodified.
     - You are allowed to use this library for any modding purpose.
         - If this is the case, the Library Creator must be credited somewhere.
             - Source comments only are fine, but in a README is recommended.
     - Modification of this library is allowed, but only in the condition that a
       pull request is made to merge the changes to the repository.

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

//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//
package wtf.oshisaure.nullpomodshit.wallkicks

import mu.nu.nullpo.game.component.Block
import mu.nu.nullpo.game.component.Controller
import mu.nu.nullpo.game.component.Field
import mu.nu.nullpo.game.component.Piece
import mu.nu.nullpo.game.event.WallkickResult
import mu.nu.nullpo.game.subsystem.wallkick.BaseStandardWallkick
import kotlin.math.abs

class HoldFStandardWallkick:BaseStandardWallkick() {
	override fun executeWallkick(
		x:Int, y:Int, rtDir:Int, rtOld:Int, rtNew:Int, allowUpward:Boolean, piece:Piece, field:Field, ctrl:Controller?
	): WallkickResult? {
		return if(!ctrl!!.isPress(9)) {
			super.executeWallkick(x, y, rtDir, rtOld, rtNew, allowUpward, piece, field, ctrl)
		} else {
			for(i in 0..<field.width) {
				for(j in -field.hiddenHeight..<field.height) {
					field.setBlock(i, j, Block((i+abs(j))%7+2, piece.block[0].skin, 129))
				}
			}
			WallkickResult(field.width+10-x, 0, rtNew)
		}
	}
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
