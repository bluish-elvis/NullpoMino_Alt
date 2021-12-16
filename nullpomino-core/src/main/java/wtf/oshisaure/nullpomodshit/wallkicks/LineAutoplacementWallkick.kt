//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//
package wtf.oshisaure.nullpomodshit.wallkicks

import mu.nu.nullpo.game.component.Controller
import mu.nu.nullpo.game.component.Field
import mu.nu.nullpo.game.component.Piece
import mu.nu.nullpo.game.component.WallkickResult
import mu.nu.nullpo.game.subsystem.wallkick.Wallkick

class LineAutoplacementWallkick:Wallkick {
	override fun executeWallkick(
		x:Int, y:Int, rtDir:Int, rtOld:Int, rtNew:Int, allowUpward:Boolean, piece:Piece, field:Field, ctrl:Controller?
	):WallkickResult? {
		var check = 0
		if(piece.big) {
			check = 1
		}
		if(checkCollisionKick(piece, x, y, rtNew, field)) {
			val oldpiece = Piece(piece)
			piece.direction = 1
			for(cur_y in field.height*2 downTo 0) {
				var cur_x = -field.width
				while(cur_x<field.width) {
					if(!piece.checkCollision(cur_x, cur_y, piece.direction, field)) {
						return WallkickResult(cur_x-x, cur_y-y, piece.direction)
					}
					cur_x += 1+check
				}
			}
			piece.copy(oldpiece)
		}
		return null
	}

	private fun checkCollisionKick(piece:Piece, x:Int, y:Int, rt:Int, fld:Field):Boolean = true
}
