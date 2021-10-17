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

class MagicalChallengeWallkick:Wallkick {
	override fun executeWallkick(
		x:Int, y:Int, rtDir:Int, rtOld:Int, rtNew:Int, allowUpward:Boolean, piece:Piece, field:Field, ctrl:Controller?
	):WallkickResult? = when(piece.id) {
		0 -> when(rtNew) {
			0, 2 -> if(checkCell(x+2, y+1, field)) testWallkick(piece, x, y, -2, 0, rtNew, field)
			else if(checkCell(x+3, y+1, field)) testWallkick(piece, x, y, -1, 0, rtNew, field)
			else if(checkCell(x, y+1, field)) testWallkick(piece, x, y, 1, 0, rtNew, field)
			else null
			1, 3 -> if(checkCell(x+1, y+2, field)) testWallkick(piece, x, y, 0, -2, rtNew, field)
			else if(checkCell(x+1, y+3, field)) testWallkick(piece, x, y, 0, -1, rtNew, field)
			else if(checkCell(x+1, y, field)) testWallkick(piece, x, y, 0, 1, rtNew, field)
			else null
			else -> null
		}
		1 -> when(rtDir) {
			1 -> when(rtNew) {
				0 -> if(checkCell(x+1, y+2, field)) testWallkick(piece, x, y, 0, -1, rtNew, field)
				else if(checkCell(x, y+2, field)) testWallkick(piece, x, y, 1, 0, rtNew, field) else null
				1 -> if(checkCell(x, y+1, field)) testWallkick(piece, x, y, 1, 0, rtNew, field)
				else if(checkCell(x, y, field)) testWallkick(piece, x, y, -1, 0, rtNew, field)
				else null
				2 -> if(checkCell(x+1, y, field)) testWallkick(piece, x, y, 0, 1, rtNew, field)
				else if(checkCell(x+2, y, field)) testWallkick(piece, x, y, -1, 0, rtNew, field)
				else null
				3 -> if(checkCell(x+2, y+1, field)) testWallkick(piece, x, y, -1, 0, rtNew, field)
				else if(checkCell(x+2, y+2, field)) testWallkick(piece, x, y, 1, 0, rtNew, field)
				else null
				else -> null
			}
			-1 -> when(rtNew) {
				0 -> if(checkCell(x+2, y+1, field)) testWallkick(piece, x, y, -1, 0, rtNew, field)
				else if(checkCell(x+2, y+2, field)) testWallkick(piece, x, y, -1, 0, rtNew, field)
				else null
				1 -> if(checkCell(x+1, y+2, field)) testWallkick(piece, x, y, 0, -1, rtNew, field) else null
				2 -> if(checkCell(x, y, field)) testWallkick(piece, x, y, 1, 0, rtNew, field)
				else if(checkCell(x, y+1, field)) testWallkick(piece, x, y, 1, 0, rtNew, field) else null
				3 -> if(checkCell(x+1, y, field)) testWallkick(piece, x, y, 0, 1, rtNew, field) else null
				else -> null
			}
			else -> null
		}
		5 -> when(rtDir) {
			-1 -> when(rtNew) {
				0 -> if(checkCell(x+1, y+2, field)) testWallkick(piece, x, y, 0, -1, rtNew, field)
				else if(checkCell(x+2, y+2, field)) testWallkick(piece, x, y, -1, 0, rtNew, field)
				else null
				1 -> if(checkCell(x, y+1, field)) testWallkick(piece, x, y, 1, 0, rtNew, field)
				else if(checkCell(x, y+2, field)) testWallkick(piece, x, y, -1, 0, rtNew, field)
				else null
				2 -> if(checkCell(x+1, y, field)) testWallkick(piece, x, y, 0, 1, rtNew, field)
				else if(checkCell(x, y, field)) testWallkick(piece, x, y, 1, 0, rtNew, field)
				else null
				3 -> if(checkCell(x+2, y+1, field)) testWallkick(piece, x, y, -1, 0, rtNew, field)
				else if(checkCell(x+2, y, field)) testWallkick(piece, x, y, 1, 0, rtNew, field)
				else null
				else -> null
			}
			1 -> when(rtNew) {
				0 -> if(checkCell(x, y+1, field)) testWallkick(piece, x, y, 1, 0, rtNew, field)
				else if(checkCell(x, y+2, field)) testWallkick(piece, x, y, 1, 0, rtNew, field)
				else null
				1 -> if(checkCell(x+1, y, field)) testWallkick(piece, x, y, 0, -1, rtNew, field)
				else null
				2 -> if(checkCell(x+1, y, field)) testWallkick(piece, x, y, -1, 0, rtNew, field)
				else if(checkCell(x+1, y+1, field)) testWallkick(piece, x, y, -1, 0, rtNew, field)
				else null
				3 -> if(checkCell(x+1, y+2, field)) testWallkick(piece, x, y, 0, 1, rtNew, field)
				else null
				else -> null
			}
			else -> null
		}
		6 -> when(rtDir) {
			1 -> when(rtNew) {
				0 -> if(checkCell(x+1, y, field)) testWallkick(piece, x, y, 0, 1, rtNew, field)
				else if(checkCell(x+2, y, field)) testWallkick(piece, x, y, -1, 0, rtNew, field)
				else null
				1 -> if(checkCell(x+2, y+1, field)) testWallkick(piece, x, y, -1, 0, rtNew, field)
				else if(checkCell(x+2, y+2, field)) testWallkick(piece, x, y, 1, 0, rtNew, field)
				else null
				2 -> if(checkCell(x+1, y+2, field)) testWallkick(piece, x, y, 0, -1, rtNew, field)
				else if(checkCell(x+2, y+2, field)) testWallkick(piece, x, y, 1, 0, rtNew, field)
				else null
				3 -> if(checkCell(x, y+1, field)) testWallkick(piece, x, y, 1, 0, rtNew, field)
				else if(checkCell(x, y, field)) testWallkick(piece, x, y, -1, 0, rtNew, field)
				else null
				else -> null
			}
			-1 -> when(rtNew) {
				0 -> if(checkCell(x, y+1, field)) testWallkick(piece, x, y, 1, 0, rtNew, field)
				else null
				1 -> if(checkCell(x+2, y+2, field)) testWallkick(piece, x, y, -1, 0, rtNew, field)
				else if(checkCell(x+1, y, field)) testWallkick(piece, x, y, 0, 1, rtNew, field)
				else null
				2 -> if(checkCell(x+2, y+1, field)) testWallkick(piece, x, y, -1, 0, rtNew, field)
				else null
				3 -> if(checkCell(x, y, field)) testWallkick(piece, x, y, 1, 0, rtNew, field)
				else if(checkCell(x+1, y+2, field)) testWallkick(piece, x, y, 0, -1, rtNew, field)
				else null
				else -> null
			}
			else -> null
		}
		3 -> when(rtDir) {
			-1 -> when(rtNew) {
				0 -> if(checkCell(x+1, y, field)) testWallkick(piece, x, y, 0, 1, rtNew, field)
				else if(checkCell(x, y, field)) testWallkick(piece, x, y, 1, 0, rtNew, field)
				else null
				1 -> if(checkCell(x+2, y+1, field)) testWallkick(piece, x, y, -1, 0, rtNew, field)
				else if(checkCell(x+2, y+2, field)) testWallkick(piece, x, y, 1, 0, rtNew, field)
				else null
				2 -> if(checkCell(x+1, y, field)) testWallkick(piece, x, y, 0, -1, rtNew, field)
				else if(checkCell(x+2, y, field)) testWallkick(piece, x, y, -1, 0, rtNew, field)
				else null
				3 -> if(checkCell(x, y+1, field)) testWallkick(piece, x, y, 1, 0, rtNew, field)
				else if(checkCell(x, y+2, field)) testWallkick(piece, x, y, -1, 0, rtNew, field)
				else null
				else -> null
			}
			1 -> when(rtNew) {
				0 -> if(checkCell(x+2, y+1, field)) testWallkick(piece, x, y, -1, 0, rtNew, field) else null
				1 -> if(checkCell(x+2, y, field)) testWallkick(piece, x, y, -1, 0, rtNew, field)
				else if(checkCell(x, y+2, field)) testWallkick(piece, x, y, 0, -1, rtNew, field) else null
				2 -> if(checkCell(x, y+1, field)) testWallkick(piece, x, y, 1, 0, rtNew, field) else null
				3 -> if(checkCell(x, y+2, field)) testWallkick(piece, x, y, 1, 0, rtNew, field)
				else if(checkCell(x+1, y, field)) testWallkick(piece, x, y, 0, 1, rtNew, field) else null
				else -> null
			}
			else -> null
		}
		4 -> when(rtOld) {
			0 -> if(checkCell(x+1, y+2, field)) testWallkick(piece, x, y, 0, -1, rtNew, field) else null
			1 -> if(checkCell(x, y+1, field)) testWallkick(piece, x, y, 1, 0, rtNew, field) else null
			2 -> if(checkCell(x+1, y, field)) testWallkick(piece, x, y, 0, 1, rtNew, field) else null
			3 -> if(checkCell(x+2, y+1, field)) testWallkick(piece, x, y, -1, 0, rtNew, field) else null
			else -> null
		}
		9 -> when(rtNew) {
			0, 2 -> if(checkCell(x+2, y+1, field)) testWallkick(piece, x, y, -1, 0, rtNew, field)
			else if(checkCell(x, y+1, field)) testWallkick(piece, x, y, 1, 0, rtNew, field) else null
			1, 3 -> if(checkCell(x+1, y+2, field)) testWallkick(piece, x, y, 0, -1, rtNew, field)
			else if(checkCell(x+1, y, field)) testWallkick(piece, x, y, 0, 1, rtNew, field) else null
			else -> null
		}
		8 -> when(rtNew) {
			0, 2 -> if(checkCell(x+1, y, field)) testWallkick(piece, x, y, -1, 0, rtNew, field) else null
			1, 3 -> if(checkCell(x, y+1, field)) testWallkick(piece, x, y, 0, -1, rtNew, field) else null
			else -> null
		}
		10 -> when(rtDir) {
			1 -> when(rtNew) {
				0 -> if(checkCell(x, y, field)) testWallkick(piece, x, y, 1, 0, rtNew, field) else null
				1 -> if(checkCell(x+1, y, field)) testWallkick(piece, x, y, -1, 0, rtNew, field) else null
				2 -> if(checkCell(x+1, y+1, field)) testWallkick(piece, x, y, 1, 0, rtNew, field) else null
				3 -> if(checkCell(x, y+1, field)) testWallkick(piece, x, y, 0, -1, rtNew, field) else null
				else -> null
			}
			-1 -> when(rtNew) {
				0 -> if(checkCell(x+1, y+1, field)) testWallkick(piece, x, y, -1, 0, rtNew, field) else null
				1 -> if(checkCell(x, y+1, field)) testWallkick(piece, x, y, 0, -1, rtNew, field) else null
				2 -> null
				3 -> if(checkCell(x+1, y, field)) testWallkick(piece, x, y, -1, 0, rtNew, field) else null
				else -> null
			}
			else -> null
		}
		else -> null
	}

	private fun testWallkick(piece:Piece, x:Int, y:Int, i:Int, j:Int, rtNew:Int, field:Field):WallkickResult? =
		if(piece.checkCollision(x+i, y+j, rtNew, field)) null else WallkickResult(i, j, rtNew)

	private fun checkCell(x:Int, y:Int, fld:Field):Boolean =
		x>=fld.width||y>=fld.height||fld.getCoordAttribute(x, y)==3||fld.getCoordAttribute(x, y)!=2&&fld.getBlockColor(x, y)!=null
}
