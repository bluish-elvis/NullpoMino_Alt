package mu.nu.nullpo.game.subsystem.wallkick

import mu.nu.nullpo.game.component.*

/** Base class for all Standard (SRS) wallkicks */
open class BaseStandardWallkick:Wallkick {
	/** Get wallkick table. Used from executeWallkick.
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param rtDir Rotation button used (-1: left rotation, 1: right rotation,
	 * 2: 180-degree rotation)
	 * @param rtOld Direction before rotation
	 * @param rtNew Direction after rotation
	 * @param allowUpward If true, upward wallkicks are allowed.
	 * @param piece Current piece
	 * @param field Current field
	 * @param ctrl Button input status (it may be null, when controlled by an
	 * AI)
	 * @return Wallkick Table. You may return null if you don't want to execute
	 * a kick.
	 */
	protected open fun getKickTable(x:Int, y:Int, rtDir:Int, rtOld:Int, rtNew:Int, allowUpward:Boolean, piece:Piece,
		field:Field, ctrl:Controller?):Array<Array<IntArray>>? = null

	/* Wallkick */
	@Suppress("UNREACHABLE_CODE") override fun executeWallkick(x:Int, y:Int, rtDir:Int, rtOld:Int, rtNew:Int, allowUpward:Boolean, piece:Piece,
		field:Field, ctrl:Controller?):WallkickResult? {
		getKickTable(x, y, rtDir, rtOld, rtNew, allowUpward, piece, field, ctrl)?.let {kicktable ->
			for(i in 0 until kicktable[rtOld].size) {
				var x2 = kicktable[rtOld][i][0]
				var y2 = kicktable[rtOld][i][1]

				if(piece.big) {
					x2 *= 2
					y2 *= 2
				}

				if(y2>=0||allowUpward)
					if(!piece.checkCollision(x+x2, y+y2, rtNew, field))
						return WallkickResult(x2, y2, rtNew)
			}
		}
		return null
	}
}