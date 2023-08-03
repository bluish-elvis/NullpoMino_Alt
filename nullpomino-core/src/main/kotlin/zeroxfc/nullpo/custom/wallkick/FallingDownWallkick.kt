/*
 Copyright (c) 2021-2023,
 This library class was created by 0xFC963F18DC21 / Shots243
 It is part of an extension library for the game NullpoMino (copyright 2010-2023)

 Kotlin converted and modified by Venom=Nhelv

 Herewith shall the term "Library Creator" be given to 0xFC963F18DC21.
 Herewith shall the term "Game Creator" be given to the original creator of NullpoMino, NullNoname.

 THIS LIBRARY AND MODE PACK WAS NOT MADE IN ASSOCIATION WITH THE GAME CREATOR.

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

package zeroxfc.nullpo.custom.wallkick

import mu.nu.nullpo.game.component.Controller
import mu.nu.nullpo.game.component.Field
import mu.nu.nullpo.game.component.Piece
import mu.nu.nullpo.game.component.WallkickResult
import mu.nu.nullpo.game.subsystem.wallkick.Wallkick

class FallingDownWallkick:Wallkick {
	/**
	 * Execute a wallkick using Falling Down's wallkick system.
	 *
	 * @param x           X-coordinate
	 * @param y           Y-coordinate
	 * @param rtDir       Rotation button used (-1: left rotation, 1: right rotation, 2: 180-degree rotation)
	 * @param rtOld       Direction before rotation
	 * @param rtNew       Direction after rotation
	 * @param allowUpward If true, upward wallkicks are allowed.
	 * @param piece       Current piece
	 * @param field       Current field
	 * @param ctrl        Button input status (it may be null, when controlled by an AI)
	 * @return WallkickResult object, or null if you don't want a kick
	 */
	override fun executeWallkick(x:Int, y:Int, rtDir:Int, rtOld:Int, rtNew:Int, allowUpward:Boolean, piece:Piece,
		field:Field, ctrl:Controller?):WallkickResult? {
		val multiplier = if(piece.big) 2 else 1
		val convRtOld:Int = (rtOld+2)%4
		val convRtNew:Int = (rtNew+2)%4

		// There are 2 sections: Flexible and Special.
		// Flexible is procedural.
		// Special uses the tables defined above.

		// Set kick type used.
		val flags = when(piece.id) {
			Piece.PIECE_I -> FLAG_FLEXIBLE
			Piece.PIECE_O -> 0
			else -> FLAG_FLEXIBLE or FLAG_SPECIAL
		}

		// FLEXIBLE KICK
		if(checkFlag(flags, FLAG_FLEXIBLE)) {
			val ordNum:Int = when {
				ctrl?.isPress(Controller.BUTTON_LEFT)==true -> 0
				ctrl?.isPress(Controller.BUTTON_RIGHT)==true -> 1
				rtDir!=1 -> 2
				else -> 3
			}

			// Intersection testing environment setup.
			val testField = Field(41, 41, 0)
			val initX:Int = 41/2
			val initY:Int = 41/2

			// New piece objects as to not mess up in-game piece.
			val testPieceOld = Piece(piece)
			testPieceOld.big = false
			val testPieceNew = Piece(piece)
			testPieceNew.big = false
			testPieceNew.placeToField(initX, initY, rtNew, testField)
			for(yOffset in 0..3) {
				val dodge = booleanArrayOf(false, false, false, false, false, false, false)

				// Intersection testing
				for(xOffset in -3..3) {
					if(testPieceOld.checkCollision(initX-xOffset, initY-yOffset, rtOld, testField)) dodge[xOffset+3] = true
				}

				// Blockage testing
				for(xOffset in -3..3) {
					if(piece.checkCollision(x+xOffset*multiplier, y+yOffset*multiplier, rtNew, field)) dodge[xOffset+3] = false
				}

				// Valid kick testing
				for(i in 0..6) {
					if(dodge[ORDER[ordNum][i]]) {
						return WallkickResult(
							(ORDER[ordNum][i]-3)*multiplier,
							yOffset*multiplier,
							rtNew
						)
					}
				}
			}
		}

		// SPECIAL KICK
		if(checkFlag(flags, FLAG_SPECIAL)) {
			// Get piece kick table.
			val masterKickTable:Array<Array<IntArray>> = when(piece.id) {
				Piece.PIECE_T -> if(rtDir==1) T_KICK_TABLE_CW else T_KICK_TABLE_CCW
				Piece.PIECE_J, Piece.PIECE_L -> if(rtDir==1) JL_KICK_TABLE_CW else JL_KICK_TABLE_CCW
				Piece.PIECE_Z -> if(rtDir==1) Z_KICK_TABLE_CW else Z_KICK_TABLE_CCW
				Piece.PIECE_S -> if(rtDir==1) S_KICK_TABLE_CW else S_KICK_TABLE_CCW
				else -> return null
			}

			// Get specific rotation kick table.
			val kicktable:Array<IntArray>
			val index = if(rtDir==1) convRtOld else 3-convRtNew
			kicktable = masterKickTable[index]

			// Do kick tests
			for(test in kicktable) {
				if(!piece.checkCollision(x+test[0]*multiplier, y+test[1]*multiplier, rtNew, field)) return WallkickResult(
					test[0]*multiplier, test[1]*multiplier, rtNew
				)
			}
		}
		return null
	}

	companion object {
		private val T_KICK_TABLE_CCW = arrayOf(
			arrayOf(intArrayOf(-1, 0), intArrayOf(0, 2), intArrayOf(-1, 2)),
			arrayOf(intArrayOf(1, 0)), arrayOf(intArrayOf(1, 0), intArrayOf(0, 1), intArrayOf(1, 1), intArrayOf(1, 3)),
			arrayOf(intArrayOf(-1, 0), intArrayOf(-1, 1))
		)
		private val T_KICK_TABLE_CW = arrayOf(
			arrayOf(intArrayOf(1, 0), intArrayOf(0, 2), intArrayOf(1, 2)),
			arrayOf(intArrayOf(-1, 0)), arrayOf(intArrayOf(-1, 0), intArrayOf(0, 1), intArrayOf(-1, 1), intArrayOf(-1, 3)),
			arrayOf(intArrayOf(1, 0), intArrayOf(1, 1))
		)
		private val JL_KICK_TABLE_CCW = arrayOf(
			arrayOf(intArrayOf(0, 0), intArrayOf(-1, 0), intArrayOf(0, 2), intArrayOf(-1, 2)),
			arrayOf(intArrayOf(0, 0), intArrayOf(1, 0)),
			arrayOf(intArrayOf(0, 0), intArrayOf(1, 0), intArrayOf(0, 1), intArrayOf(1, 1), intArrayOf(0, 3), intArrayOf(1, 3)),
			arrayOf(intArrayOf(0, 0), intArrayOf(-1, 0), intArrayOf(-1, 1))
		)
		private val JL_KICK_TABLE_CW = arrayOf(
			arrayOf(intArrayOf(0, 0), intArrayOf(1, 0), intArrayOf(0, 2), intArrayOf(1, 2)),
			arrayOf(intArrayOf(0, 0), intArrayOf(-1, 0)),
			arrayOf(intArrayOf(0, 0), intArrayOf(-1, 0), intArrayOf(0, 1), intArrayOf(-1, 1), intArrayOf(0, 3), intArrayOf(-1, 3)),
			arrayOf(intArrayOf(0, 0), intArrayOf(1, 0), intArrayOf(1, 1))
		)
		private val Z_KICK_TABLE_CCW = arrayOf(
			arrayOf(
				intArrayOf(0, 0), intArrayOf(-1, 0), intArrayOf(0, 1), intArrayOf(-1, 1), intArrayOf(0, 2), intArrayOf(-1, 2),
				intArrayOf(0, 3), intArrayOf(-1, 3)
			), arrayOf(intArrayOf(0, 0), intArrayOf(1, 0), intArrayOf(0, 1), intArrayOf(1, 1)),
			arrayOf(
				intArrayOf(0, 0), intArrayOf(-1, 0), intArrayOf(0, 1), intArrayOf(-1, 1), intArrayOf(0, 2), intArrayOf(-1, 2),
				intArrayOf(0, 3), intArrayOf(-1, 3)
			), arrayOf(intArrayOf(0, 0), intArrayOf(1, 0), intArrayOf(0, 1), intArrayOf(1, 1))
		)
		private val Z_KICK_TABLE_CW = arrayOf(
			arrayOf(
				intArrayOf(0, 0), intArrayOf(-1, 0), intArrayOf(0, 1), intArrayOf(-1, 1), intArrayOf(0, 2), intArrayOf(-1, 2),
				intArrayOf(0, 3), intArrayOf(-1, 3)
			), arrayOf(intArrayOf(0, 0), intArrayOf(1, 0), intArrayOf(0, 1), intArrayOf(1, 1)),
			arrayOf(
				intArrayOf(0, 0), intArrayOf(-1, 0), intArrayOf(0, 1), intArrayOf(-1, 1), intArrayOf(0, 2), intArrayOf(-1, 2),
				intArrayOf(0, 3), intArrayOf(-1, 3)
			), arrayOf(intArrayOf(0, 0), intArrayOf(1, 0), intArrayOf(0, 1), intArrayOf(1, 1))
		)
		private val S_KICK_TABLE_CCW = arrayOf(
			arrayOf(
				intArrayOf(0, 0), intArrayOf(1, 0), intArrayOf(0, 1), intArrayOf(1, 1), intArrayOf(0, 2), intArrayOf(1, 2),
				intArrayOf(0, 3), intArrayOf(1, 3)
			), arrayOf(intArrayOf(0, 0), intArrayOf(-1, 0), intArrayOf(0, 1), intArrayOf(-1, 1)),
			arrayOf(
				intArrayOf(0, 0), intArrayOf(1, 0), intArrayOf(0, 1), intArrayOf(1, 1), intArrayOf(0, 2), intArrayOf(1, 2),
				intArrayOf(0, 3), intArrayOf(1, 3)
			), arrayOf(intArrayOf(0, 0), intArrayOf(-1, 0), intArrayOf(0, 1), intArrayOf(-1, 1))
		)
		private val S_KICK_TABLE_CW = arrayOf(
			arrayOf(
				intArrayOf(0, 0), intArrayOf(1, 0), intArrayOf(0, 1), intArrayOf(1, 1), intArrayOf(0, 2), intArrayOf(1, 2),
				intArrayOf(0, 3), intArrayOf(1, 3)
			), arrayOf(intArrayOf(0, 0), intArrayOf(-1, 0), intArrayOf(0, 1), intArrayOf(-1, 1)),
			arrayOf(
				intArrayOf(0, 0), intArrayOf(1, 0), intArrayOf(0, 1), intArrayOf(1, 1), intArrayOf(0, 2), intArrayOf(1, 2),
				intArrayOf(0, 3), intArrayOf(1, 3)
			), arrayOf(intArrayOf(0, 0), intArrayOf(-1, 0), intArrayOf(0, 1), intArrayOf(-1, 1))
		)
		private val ORDER = arrayOf(
			intArrayOf(3, 2, 1, 0, 4, 5, 6), intArrayOf(3, 4, 5, 6, 2, 1, 0),
			intArrayOf(3, 2, 4, 1, 5, 0, 6), intArrayOf(3, 4, 2, 5, 1, 6, 0)
		)
		private const val FLAG_FLEXIBLE = 1
		private const val FLAG_SPECIAL = 2
		private fun checkFlag(num:Int, flag:Int):Boolean = num and flag>0
	}

}
