//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//
package wtf.oshisaure.nullpomodshit.wallkicks

import mu.nu.nullpo.game.component.*
import mu.nu.nullpo.game.subsystem.wallkick.Wallkick
import org.apache.logging.log4j.LogManager

class PressEWallkick:Wallkick {
	override fun executeWallkick(
		x:Int, y:Int, rtDir:Int, rtOld:Int, rtNew:Int, allowUpward:Boolean, piece:Piece, field:Field,
		ctrl:Controller?
	):WallkickResult? {
		return if(!ctrl!!.isPress(8)) {
			null
		} else {
			val mino = Piece(7).apply {
				setBlock(piece.block[0])
				for(cur_y in -field.hiddenHeight until field.height) {
					for(cur_x in 0 until field.width) {
						setColor(2+(cur_x-cur_y+field.height)%7)
						placeToField(cur_x, cur_y, field)
						if(checkCollisionKick(piece, x, y, rtOld, field)) field.delBlock(cur_x, cur_y)
					}
				}
			}
			ctrl.setButtonPressed(0)
			ctrl.setButtonPressed(1)
			WallkickResult(0, -field.hiddenHeight-field.height, rtOld)
		}
	}

	private fun checkCollisionKick(piece:Piece, x:Int, y:Int, rt:Int, fld:Field):Boolean =
		if(piece.big) checkCollisionKickBig(piece, x, y, rt, fld)
		else piece.block.withIndex().any {
			val i = it.index
			if(piece.dataX[rt][i]!=1+piece.dataOffsetX[rt]) {
				val x2 = x+piece.dataX[rt][i]
				val y2 = y+piece.dataY[rt][i]
				x2>=fld.width||y2>=fld.height||fld.getCoordAttribute(x2, y2)==3||
					fld.getCoordAttribute(x2, y2)!=2&&fld.getBlockColor(x2, y2)!=null
			} else false
		}

	private fun checkCollisionKickBig(piece:Piece, x:Int, y:Int, rt:Int, fld:Field):Boolean {
		for(i in 0 until piece.maxBlock) {
			if(piece.dataX[rt][i]!=1+piece.dataOffsetX[rt]) {
				val x2 = x+piece.dataX[rt][i]*2
				val y2 = y+piece.dataY[rt][i]*2
				for(k in 0..1) {
					for(l in 0..1) {
						val x3 = x2+k
						val y3 = y2+l
						if(x3>=fld.width||y3>=fld.height||fld.getCoordAttribute(x3, y3)==3||
							fld.getCoordAttribute(x3, y3)!=2&&fld.getBlockColor(x3, y3)!=null
						) return true
					}
				}
			}
		}
		return false
	}

	companion object {
		var log = LogManager.getLogger()
	}
}
