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

class ClassicOAvalancheWallkick:Wallkick {
	override fun executeWallkick(
		x:Int, y:Int, rtDir:Int, rtOld:Int, rtNew:Int, allowUpward:Boolean, piece:Piece, field:Field, ctrl:Controller?
	):WallkickResult? {
		var check = 0
		if(piece.big) {
			check = 1
		}
		var i:Int
		var cur_x:Int
		if(piece.id!=0) {
			if(piece.id==2) {
				when(rtDir) {
					-1 -> field.moveLeft()
					0 -> {}
					1 -> field.moveRight()
					2 -> {
						i = field.highestBlockY
						cur_x = 0
						while(cur_x<field.width) {
							var cur_cell = field.height-1
							var cur_y = field.height-1
							while(cur_y>=i) {
								val bl = field.getBlock(cur_x, cur_y)
								if(bl!=null&&!bl.isEmpty) {
									if(cur_cell!=cur_y) {
										field.setBlock(cur_x, cur_cell, Block(bl))
										field.setBlock(cur_x, cur_y, Block())
									}
									--cur_cell
								}
								--cur_y
							}
							++cur_x
						}
					}
					else -> {}
				}
				return WallkickResult(0, 0, rtOld)
			}
			if(checkCollisionKick(piece, x, y, rtNew, field)||piece.id==8||piece.id==10) {
				i = 0
				if(!piece.checkCollision(x-1-check, y, rtNew, field)) i = -1-check
				if(!piece.checkCollision(x+1+check, y, rtNew, field)) i = 1+check
				if(i!=0) return WallkickResult(i, 0, rtNew)
			}
		}
		return if(piece.id==4&&allowUpward&&rtNew==0&&!piece.checkCollision(x, y-1-check, rtNew, field)) {
			WallkickResult(0, -1-check, rtNew)
		} else {
			if(piece.id==0&&(rtNew==0||rtNew==2)) {
				i = check
				while(i<=check*2) {
					cur_x = 0
					if(!piece.checkCollision(x-1-i, y, rtNew, field)) cur_x = -1-i
					else if(!piece.checkCollision(x+1+i, y, rtNew, field)) cur_x = 1+i
					else if(!piece.checkCollision(x+2+i, y, rtNew, field)) cur_x = 2+i
					if(cur_x!=0) return WallkickResult(cur_x, 0, rtNew)
					++i
				}
			}
			if(piece.id==0&&allowUpward&&(rtNew==3||rtNew==1)&&piece.checkCollision(x, y+1, field)) {
				i = check
				while(i<=check*2) {
					cur_x = 0
					if(!piece.checkCollision(x, y-1-i, rtNew, field)) cur_x = -1-i
					else if(!piece.checkCollision(x, y-2-i, rtNew, field)) cur_x = -2-i
					if(cur_x!=0) return WallkickResult(0, cur_x, rtNew)
					++i
				}
			}
			null
		}
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
