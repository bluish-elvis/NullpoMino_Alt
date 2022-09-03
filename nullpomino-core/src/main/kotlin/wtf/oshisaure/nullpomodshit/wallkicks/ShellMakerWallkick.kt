//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//
package wtf.oshisaure.nullpomodshit.wallkicks

import mu.nu.nullpo.game.component.Block
import mu.nu.nullpo.game.component.Controller
import mu.nu.nullpo.game.component.Field
import mu.nu.nullpo.game.component.Piece
import mu.nu.nullpo.game.component.WallkickResult
import mu.nu.nullpo.game.subsystem.wallkick.Wallkick

class ShellMakerWallkick:Wallkick {
	override fun executeWallkick(x:Int, y:Int, rtDir:Int, rtOld:Int, rtNew:Int, allowUpward:Boolean, piece:Piece, field:Field,
		ctrl:Controller?):WallkickResult? {
		if(checkCollisionKick(piece, x, y, rtNew, field)) {
			val mino = Piece(7).apply {
				setBlock(piece.block[0])
				setColor(8)
			}
			for(cur_y in 2 until field.height) for(cur_x in 0 until field.width) {
				mino.placeToField(cur_x, cur_y, field)
				if(piece.checkCollision(x, y, field)) field.setBlock(cur_x, cur_y, null as Block?)
			}
		}
		return null
	}

	private fun checkCollisionKick(piece:Piece, x:Int, y:Int, rt:Int, fld:Field):Boolean = true
}
