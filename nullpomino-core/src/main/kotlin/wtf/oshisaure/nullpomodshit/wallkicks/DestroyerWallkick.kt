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

class DestroyerWallkick:Wallkick {
	override fun executeWallkick(
		x:Int, y:Int, rtDir:Int, rtOld:Int, rtNew:Int, allowUpward:Boolean, piece:Piece, field:Field, ctrl:Controller?
	):WallkickResult {
		val kick = movePieceInBounds(piece, x, y, rtNew, field)
		destroyBlocksKick(piece, x, y, rtNew, field)
		return kick
	}

	private fun destroyBlocksKick(piece:Piece, x:Int, y:Int, rt:Int, fld:Field) {
		if(piece.big) {
			destroyBlocksKickBig(piece, x, y, rt, fld)
		} else {
			for(i in 0 until piece.maxBlock) {
				val x2 = x+piece.dataX[rt][i]
				val y2 = y+piece.dataY[rt][i]
				fld.setBlock(x2, y2, Block())
			}
		}
	}

	private fun destroyBlocksKickBig(piece:Piece, x:Int, y:Int, rt:Int, fld:Field) {
		for(i in 0 until piece.maxBlock) {
			val x2 = x+piece.dataX[rt][i]*2
			val y2 = y+piece.dataY[rt][i]*2
			for(k in 0..1) {
				for(l in 0..1) {
					val x3 = x2+k
					val y3 = y2+l
					val bl = fld.getBlock(x3, y3)
					if(bl!=null&&!bl.isEmpty) {
						fld.setBlock(x3, y3, Block())
					}
				}
			}
		}
	}

	private fun movePieceInBounds(piece:Piece, x:Int, y:Int, rt:Int, fld:Field):WallkickResult {
		return if(piece.big) {
			movePieceInBoundsBig(piece, x, y, rt, fld)
		} else {
			var kick_x = 0
			var kick_y = 0
			var kick_dir = checkInBoundsKick(piece, x+kick_x, y+kick_y, rt, fld)
			while(kick_dir!=-1) {
				when(kick_dir) {
					0 -> --kick_x
					1 -> ++kick_x
					2 -> --kick_y
					3 -> ++kick_y
				}
				kick_dir = checkInBoundsKick(piece, x+kick_x, y+kick_y, rt, fld)
			}
			WallkickResult(kick_x, kick_y, rt)
		}
	}

	private fun movePieceInBoundsBig(piece:Piece, x:Int, y:Int, rt:Int, fld:Field):WallkickResult {
		var kick_x = 0
		var kick_y = 0
		var kick_dir = checkInBoundsKickBig(piece, x+kick_x, y+kick_y, rt, fld)
		while(kick_dir!=-1) {
			when(kick_dir) {
				0 -> --kick_x
				1 -> ++kick_x
				2 -> --kick_y
				3 -> ++kick_y
			}
			kick_dir = checkInBoundsKickBig(piece, x+kick_x, y+kick_y, rt, fld)
		}
		return WallkickResult(kick_x, kick_y, rt)
	}

	private fun checkInBoundsKick(piece:Piece, x:Int, y:Int, rt:Int, fld:Field):Int {
		return if(piece.big) {
			checkInBoundsKickBig(piece, x, y, rt, fld)
		} else {
			for(i in 0 until piece.maxBlock) {
				val x2 = x+piece.dataX[rt][i]
				val y2 = y+piece.dataY[rt][i]
				if(x2>=fld.width) return 0 else
					if(x2<0) return 1 else
						if(y2>=fld.height) return 2 else
							if(y2<0&&fld.ceiling) return 3
			}
			-1
		}
	}

	private fun checkInBoundsKickBig(piece:Piece, x:Int, y:Int, rt:Int, fld:Field):Int {
		for(i in 0 until piece.maxBlock) {
			val x2 = x+piece.dataX[rt][i]*2
			val y2 = y+piece.dataY[rt][i]*2
			for(k in 0..1) {
				for(l in 0..1) {
					val x3 = x2+k
					val y3 = y2+l
					when {
						x3>=fld.width -> return 0
						x3<0 -> return 1
						y3>=fld.height -> return 2
						y3<0&&fld.ceiling -> return 3
					}
				}
			}
		}
		return -1
	}
}
