package mu.nu.nullpo.game.subsystem.wallkick

import mu.nu.nullpo.game.component.Controller
import mu.nu.nullpo.game.component.Field
import mu.nu.nullpo.game.component.Piece
import mu.nu.nullpo.game.component.WallkickResult

/** GBCWallkick */
class GBCWallkick:Wallkick {

	override fun executeWallkick(x:Int, y:Int, rtDir:Int, rtOld:Int, rtNew:Int, allowUpward:Boolean, piece:Piece,
		field:Field, ctrl:Controller?):WallkickResult? {
		if(piece.id!=Piece.PIECE_I&&piece.id!=Piece.PIECE_I2&&piece.id!=Piece.PIECE_I3) {
			var kicktable = KICKTABLE_L
			if(rtDir>=0) kicktable = KICKTABLE_R

			var x2 = kicktable[rtOld][0]
			var y2 = kicktable[rtOld][1]
			if(piece.big) {
				x2 *= 2
				y2 *= 2
			}

			if((y2>=0||allowUpward)&&y+y2>-2)
				if(!piece.checkCollision(x+x2, y+y2, rtNew, field))
					return WallkickResult(x2, y2, rtNew)
		}

		return null
	}

	companion object {
		private val KICKTABLE_L = arrayOf(intArrayOf(1, -1), intArrayOf(1, 1), intArrayOf(-1, 1), intArrayOf(-1, -1))
		private val KICKTABLE_R = arrayOf(intArrayOf(-1, -1), intArrayOf(1, -1), intArrayOf(1, 1), intArrayOf(-1, 1))
	}
}
