package zeroxfc.nullpo.custom.wallkick

import mu.nu.nullpo.game.component.Piece
import mu.nu.nullpo.game.component.WallkickResult
import mu.nu.nullpo.game.subsystem.wallkick.BaseStandardWallkick
import zeroxfc.nullpo.custom.libs.ArrayRandomizer

class SuperRandomWallkick:BaseStandardWallkick() {
	/*
     * Wallkick
     */
	override fun executeWallkick(x:Int, y:Int, rtDir:Int, rtOld:Int, rtNew:Int, allowUpward:Boolean, piece:Piece,
		field:mu.nu.nullpo.game.component.Field, ctrl:mu.nu.nullpo.game.component.Controller?):WallkickResult? {
		val kicktable:Array<Array<IntArray>> = getKickTable(x, y, rtDir, rtOld, rtNew, allowUpward, piece, field, ctrl)
			?: return null
		var arr = IntArray(kicktable[rtOld].size)
		for(i in arr.indices) {
			arr[i] = i
		}
		var v = 0
		for(i in ctrl!!.buttonTime) {
			v += i
		}
		val randomizer = ArrayRandomizer((rtOld+rtNew+piece.id+field.highestBlockY+v).toLong())
		arr = randomizer.permute(arr)
		for(i in arr) {
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
		return null
	}
}