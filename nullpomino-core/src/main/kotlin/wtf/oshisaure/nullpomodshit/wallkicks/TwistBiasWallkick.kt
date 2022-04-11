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

class TwistBiasWallkick:Wallkick {
	override fun executeWallkick(
		x:Int, y:Int, rtDir:Int, rtOld:Int, rtNew:Int, allowUpward:Boolean, piece:Piece, field:Field, ctrl:Controller?
	):WallkickResult? {
		if(!checkCollisionKick(piece, x, y, rtNew, field)&&!piece.checkCollision(x, y+1, field)) {
			return null
		}
		var xOffs = 0
		var yOffs = 20
		var foundSpace = false
		while(true) {
			while(!foundSpace) {
				foundSpace = true
				if(!piece.checkCollision(x, y+yOffs, rtNew, field)) xOffs = 0
				else if(!piece.checkCollision(x+1, y+yOffs, rtNew, field)) xOffs = 1
				else if(!piece.checkCollision(x-1, y+yOffs, rtNew, field)) xOffs = -1
				else if(piece.big&&!piece.checkCollision(x+2, y+yOffs, rtNew, field)) xOffs = 2
				else if(piece.big&&!piece.checkCollision(x-2, y+yOffs, rtNew, field)) xOffs = -2
				else {
					if(yOffs<-20) return null
					foundSpace = false
					--yOffs
				}
			}
			return WallkickResult(xOffs, yOffs, rtNew)
		}
	}

	private fun checkCollisionKick(piece:Piece, x:Int, y:Int, rt:Int, fld:Field):Boolean {
		return if(piece.big) checkCollisionKickBig(piece, x, y, rt, fld) else {
			for(i in 0 until piece.maxBlock) {
				if(piece.dataX[rt][i]!=1+piece.dataOffsetX[rt]) {
					val x2 = x+piece.dataX[rt][i]
					val y2 = y+piece.dataY[rt][i]
					if(x2>=fld.width||y2>=fld.height||fld.getCoordAttribute(x2, y2)==3||
						fld.getCoordAttribute(x2, y2)!=2&&fld.getBlockColor(x2, y2)!=null
					) return true
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
						if(x3>=fld.width||y3>=fld.height||fld.getCoordAttribute(x3, y3)==3||
							fld.getCoordAttribute(x3, y3)!=2&&fld.getBlockColor(x3, y3)!=null
						) return true
					}
				}
			}
		}
		return false
	}
}
