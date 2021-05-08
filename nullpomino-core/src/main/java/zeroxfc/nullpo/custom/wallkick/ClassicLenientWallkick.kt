package zeroxfc.nullpo.custom.wallkick

import mu.nu.nullpo.game.component.*
import mu.nu.nullpo.game.subsystem.wallkick.Wallkick

class ClassicLenientWallkick:Wallkick {
	/**
	 * Execute a wallkick
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
	override fun executeWallkick(x:Int, y:Int, rtDir:Int, rtOld:Int, rtNew:Int, allowUpward:Boolean, piece:Piece, field:Field,
		ctrl:Controller?):WallkickResult? {
		var x2:Int
		var y2:Int
		val wallkick = if(piece.id==Piece.PIECE_I) I_WALLKICK else BASE_WALLKICK
		for(i in wallkick.indices) {
			x2 = if(rtDir<0||rtDir==2) {
				wallkick[i][0]
			} else {
				-wallkick[i][0]
			}
			y2 = wallkick[i][1]
			if(piece.big) {
				x2 *= 2
				y2 *= 2
			}
			if(!piece.checkCollision(x+x2, y+y2, rtNew, field)) {
				return WallkickResult(x2, y2, rtNew)
			}
		}
		return null
	}

	companion object {
		private val BASE_WALLKICK = arrayOf(intArrayOf(-1, 0), intArrayOf(1, 0), intArrayOf(0, 1), intArrayOf(0, -1))
		private val I_WALLKICK = arrayOf(intArrayOf(-1, 0), intArrayOf(1, 0), intArrayOf(-2, 0), intArrayOf(2, 0), intArrayOf(0, 1),
			intArrayOf(0, -1))
	}
}