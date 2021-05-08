package zeroxfc.nullpo.custom.wallkick

import mu.nu.nullpo.game.component.*
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
		var flags = 0
		when(piece.id) {
			Piece.PIECE_I -> flags = flags or FLAG_FLEXIBLE
			Piece.PIECE_O -> {
			}
			else -> {
				flags = flags or FLAG_FLEXIBLE
				flags = flags or FLAG_SPECIAL
			}
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
			val testField:Field = Field(41, 41, 0)
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
					test[0]*multiplier, test[1]*multiplier, rtNew)
			}
		}
		return null
	}

	companion object {
		private val T_KICK_TABLE_CCW = arrayOf(arrayOf(intArrayOf(-1, 0), intArrayOf(0, 2), intArrayOf(-1, 2)),
			arrayOf(intArrayOf(1, 0)), arrayOf(intArrayOf(1, 0), intArrayOf(0, 1), intArrayOf(1, 1), intArrayOf(1, 3)),
			arrayOf(intArrayOf(-1, 0), intArrayOf(-1, 1)))
		private val T_KICK_TABLE_CW = arrayOf(arrayOf(intArrayOf(1, 0), intArrayOf(0, 2), intArrayOf(1, 2)),
			arrayOf(intArrayOf(-1, 0)), arrayOf(intArrayOf(-1, 0), intArrayOf(0, 1), intArrayOf(-1, 1), intArrayOf(-1, 3)),
			arrayOf(intArrayOf(1, 0), intArrayOf(1, 1)))
		private val JL_KICK_TABLE_CCW = arrayOf(arrayOf(intArrayOf(0, 0), intArrayOf(-1, 0), intArrayOf(0, 2), intArrayOf(-1, 2)),
			arrayOf(intArrayOf(0, 0), intArrayOf(1, 0)),
			arrayOf(intArrayOf(0, 0), intArrayOf(1, 0), intArrayOf(0, 1), intArrayOf(1, 1), intArrayOf(0, 3), intArrayOf(1, 3)),
			arrayOf(intArrayOf(0, 0), intArrayOf(-1, 0), intArrayOf(-1, 1)))
		private val JL_KICK_TABLE_CW = arrayOf(arrayOf(intArrayOf(0, 0), intArrayOf(1, 0), intArrayOf(0, 2), intArrayOf(1, 2)),
			arrayOf(intArrayOf(0, 0), intArrayOf(-1, 0)),
			arrayOf(intArrayOf(0, 0), intArrayOf(-1, 0), intArrayOf(0, 1), intArrayOf(-1, 1), intArrayOf(0, 3), intArrayOf(-1, 3)),
			arrayOf(intArrayOf(0, 0), intArrayOf(1, 0), intArrayOf(1, 1)))
		private val Z_KICK_TABLE_CCW = arrayOf(
			arrayOf(intArrayOf(0, 0), intArrayOf(-1, 0), intArrayOf(0, 1), intArrayOf(-1, 1), intArrayOf(0, 2), intArrayOf(-1, 2),
				intArrayOf(0, 3), intArrayOf(-1, 3)), arrayOf(intArrayOf(0, 0), intArrayOf(1, 0), intArrayOf(0, 1), intArrayOf(1, 1)),
			arrayOf(intArrayOf(0, 0), intArrayOf(-1, 0), intArrayOf(0, 1), intArrayOf(-1, 1), intArrayOf(0, 2), intArrayOf(-1, 2),
				intArrayOf(0, 3), intArrayOf(-1, 3)), arrayOf(intArrayOf(0, 0), intArrayOf(1, 0), intArrayOf(0, 1), intArrayOf(1, 1)))
		private val Z_KICK_TABLE_CW = arrayOf(
			arrayOf(intArrayOf(0, 0), intArrayOf(-1, 0), intArrayOf(0, 1), intArrayOf(-1, 1), intArrayOf(0, 2), intArrayOf(-1, 2),
				intArrayOf(0, 3), intArrayOf(-1, 3)), arrayOf(intArrayOf(0, 0), intArrayOf(1, 0), intArrayOf(0, 1), intArrayOf(1, 1)),
			arrayOf(intArrayOf(0, 0), intArrayOf(-1, 0), intArrayOf(0, 1), intArrayOf(-1, 1), intArrayOf(0, 2), intArrayOf(-1, 2),
				intArrayOf(0, 3), intArrayOf(-1, 3)), arrayOf(intArrayOf(0, 0), intArrayOf(1, 0), intArrayOf(0, 1), intArrayOf(1, 1)))
		private val S_KICK_TABLE_CCW = arrayOf(
			arrayOf(intArrayOf(0, 0), intArrayOf(1, 0), intArrayOf(0, 1), intArrayOf(1, 1), intArrayOf(0, 2), intArrayOf(1, 2),
				intArrayOf(0, 3), intArrayOf(1, 3)), arrayOf(intArrayOf(0, 0), intArrayOf(-1, 0), intArrayOf(0, 1), intArrayOf(-1, 1)),
			arrayOf(intArrayOf(0, 0), intArrayOf(1, 0), intArrayOf(0, 1), intArrayOf(1, 1), intArrayOf(0, 2), intArrayOf(1, 2),
				intArrayOf(0, 3), intArrayOf(1, 3)), arrayOf(intArrayOf(0, 0), intArrayOf(-1, 0), intArrayOf(0, 1), intArrayOf(-1, 1)))
		private val S_KICK_TABLE_CW = arrayOf(
			arrayOf(intArrayOf(0, 0), intArrayOf(1, 0), intArrayOf(0, 1), intArrayOf(1, 1), intArrayOf(0, 2), intArrayOf(1, 2),
				intArrayOf(0, 3), intArrayOf(1, 3)), arrayOf(intArrayOf(0, 0), intArrayOf(-1, 0), intArrayOf(0, 1), intArrayOf(-1, 1)),
			arrayOf(intArrayOf(0, 0), intArrayOf(1, 0), intArrayOf(0, 1), intArrayOf(1, 1), intArrayOf(0, 2), intArrayOf(1, 2),
				intArrayOf(0, 3), intArrayOf(1, 3)), arrayOf(intArrayOf(0, 0), intArrayOf(-1, 0), intArrayOf(0, 1), intArrayOf(-1, 1)))
		private val ORDER = arrayOf(intArrayOf(3, 2, 1, 0, 4, 5, 6), intArrayOf(3, 4, 5, 6, 2, 1, 0),
			intArrayOf(3, 2, 4, 1, 5, 0, 6), intArrayOf(3, 4, 2, 5, 1, 6, 0))
		private const val FLAG_FLEXIBLE = 1
		private const val FLAG_SPECIAL = 2
		private fun checkFlag(num:Int, flag:Int):Boolean = num and flag>0
	}

}