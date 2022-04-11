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

class ClassicTransformWallkick:Wallkick {
	override fun executeWallkick(
		x:Int, y:Int, rtDir:Int, rtOld:Int, rtNew:Int, allowUpward:Boolean, piece:Piece, field:Field, ctrl:Controller?
	):WallkickResult? {
		var check = 0
		if(piece.big) {
			check = 1
		}
		if(checkCollisionKick(piece, x, y, rtNew, field)||piece.id==8||piece.id==10) {
			val oldpiece = Piece(piece)
			var loop = piece.id
			for(i in 0..5) {
				loop = if(i==5) 2 else idLoop(loop)
				piece.id=loop
				piece.setBlock(oldpiece.block[0])
//				piece.id = oldpiece.id
				piece.setColor(oldpiece.colors)
				if(!piece.checkCollision(x, y+2, rtNew, field)) return WallkickResult(0, 2, rtNew)
				else if(!piece.checkCollision(x+1+check, y+2, rtNew, field)) return WallkickResult(1+check, 2, rtNew)
				if(!piece.checkCollision(x-1-check, y+2, rtNew, field)) return WallkickResult(-1-check, 2, rtNew)
				if(!piece.checkCollision(x, y+1, rtNew, field)) return WallkickResult(0, 1, rtNew)
				if(!piece.checkCollision(x+1+check, y+1, rtNew, field)) return WallkickResult(1+check, 1, rtNew)
				if(!piece.checkCollision(x-1-check, y+1, rtNew, field)) return WallkickResult(-1-check, 1, rtNew)
				if(!piece.checkCollision(x, y, rtNew, field)) return WallkickResult(0, 0, rtNew)
				if(!piece.checkCollision(x+1+check, y, rtNew, field)) return WallkickResult(1+check, 0, rtNew)
				if(!piece.checkCollision(x-1-check, y, rtNew, field)) return WallkickResult(-1-check, 0, rtNew)
				if(!piece.checkCollision(x, y-1, rtNew, field)) return WallkickResult(0, -1, rtNew)
			}
			piece.copy(oldpiece)
		}
		return null
	}

	private fun idLoop(id:Int):Int = when(id) {
		0 -> 1
		1 -> 4
		2 -> 0
		3 -> 5
		4 -> 3
		5 -> 6
		6 -> 0
		else -> 0
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
						if(x3>=fld.width||y3>=fld.height||fld.getCoordAttribute(x3, y3)==3
							||fld.getCoordAttribute(x3, y3)!=2&&fld.getBlockColor(x3, y3)!=null
						) return true
					}
				}
			}
		}
		return false
	}
}
