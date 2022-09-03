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
import java.util.Random

class ClassicSecretGMWallkick:Wallkick {
	private val random = Random()
	override fun executeWallkick(
		x:Int, y:Int, rtDir:Int, rtOld:Int, rtNew:Int, allowUpward:Boolean, piece:Piece, field:Field, ctrl:Controller?
	):WallkickResult {
		var check = 0
		if(piece.big) {
			check = 1
		}
		if(piece.id!=0&&(checkCollisionKick(piece, x, y, rtNew, field)||piece.id==8||piece.id==10)) {
			var temp = 0
			if(!piece.checkCollision(x-1-check, y, rtNew, field)) temp = -1-check
			if(!piece.checkCollision(x+1+check, y, rtNew, field)) temp = 1+check
			if(temp!=0) return WallkickResult(temp, 0, rtNew)
		}
		val n_skin = 29
		val w_field = field.width
		val h_field = field.height
		for(sg_y in 0 until w_field*2) {
			var hole = if(sg_y<w_field) sg_y else w_field*2-2-sg_y
			hole = if(hole<0) w_field-1 else hole
			for(sg_x in 0 until w_field) {
				if(sg_x==hole) field.setBlock(sg_x, h_field-sg_y-1, Block())
				else field.setBlock(sg_x, h_field-sg_y-1, Block(1+random.nextInt(15), random.nextInt(n_skin), 129))
			}
		}
		return WallkickResult(0, -y-h_field-4, rtNew)
	}

	private fun checkCollisionKick(piece:Piece, x:Int, y:Int, rt:Int, fld:Field):Boolean {
		return if(piece.big) {
			checkCollisionKickBig(piece, x, y, rt, fld)
		} else {
			for(i in 0 until piece.maxBlock) {
				if(piece.dataX[rt][i]!=1+piece.dataOffsetX[rt]) {
					val x2 = x+piece.dataX[rt][i]
					val y2 = y+piece.dataY[rt][i]
					if(x2>=fld.width) return true else
						if(y2>=fld.height) return true else
							if(fld.getCoordAttribute(x2, y2)==3) return true else
								if(fld.getCoordAttribute(x2, y2)!=2&&fld.getBlockColor(x2, y2)!=null) return true
				}
			}
			false
		}
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
						if(x3>=fld.width) return true else
							if(y3>=fld.height) return true else
								if(fld.getCoordAttribute(x3, y3)==3) return true else
									if(fld.getCoordAttribute(x3, y3)!=2&&fld.getBlockColor(x3, y3)!=null) return true
					}
				}
			}
		}
		return false
	}
}
