//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//
package wtf.oshisaure.nullpomodshit.wallkicks

import mu.nu.nullpo.game.component.*
import mu.nu.nullpo.game.subsystem.wallkick.BaseStandardWallkick
import kotlin.math.abs

class HoldFStandardWallkick:BaseStandardWallkick() {
	override fun executeWallkick(
		x:Int, y:Int, rtDir:Int, rtOld:Int, rtNew:Int, allowUpward:Boolean, piece:Piece, field:Field, ctrl:Controller?
	):WallkickResult? {
		return if(!ctrl!!.isPress(9)) {
			super.executeWallkick(x, y, rtDir, rtOld, rtNew, allowUpward, piece, field, ctrl)
		} else {
			for(i in 0 until field.width) {
				for(j in -field.hiddenHeight until field.height) {
					field.setBlock(i, j, Block((i+abs(j))%7+2, piece.block[0].skin, 129))
				}
			}
			WallkickResult(field.width+10-x, 0, rtNew)
		}
	}
	/* Get kick table */
	override fun getKickTable(x:Int, y:Int, rtDir:Int, rtOld:Int, rtNew:Int, allowUpward:Boolean,
		piece:Piece, field:Field, ctrl:Controller?):Array<Array<IntArray>>? {
		var kicktable:Array<Array<IntArray>>? = null

		when(rtDir) {
			2
				// 180-degree rotation
			-> kicktable = when(piece.id) {
				Piece.PIECE_I -> WALLKICK_I_180
				else -> WALLKICK_NORMAL_180
			}
			-1
				// Left rotation
			-> kicktable = when(piece.id) {
				Piece.PIECE_I -> WALLKICK_I_L
				Piece.PIECE_I2 -> WALLKICK_I2_L
				Piece.PIECE_I3 -> WALLKICK_I3_L
				Piece.PIECE_L3 -> WALLKICK_L3_L
				else -> WALLKICK_NORMAL_L
			}
			1
				// Right rotation
			-> kicktable = when(piece.id) {
				Piece.PIECE_I -> WALLKICK_I_R
				Piece.PIECE_I2 -> WALLKICK_I2_R
				Piece.PIECE_I3 -> WALLKICK_I3_R
				Piece.PIECE_L3 -> WALLKICK_L3_R
				else -> WALLKICK_NORMAL_R
			}
		}

		return kicktable
	}

	companion object {
		// Wallkick data
		private val WALLKICK_NORMAL_L = arrayOf(
			arrayOf(intArrayOf(1, 0), intArrayOf(1, -1), intArrayOf(0, 2), intArrayOf(1, 2), intArrayOf(0, -1)), // 0>>3
			arrayOf(intArrayOf(1, 0), intArrayOf(1, 1), intArrayOf(0, -2), intArrayOf(1, -2), intArrayOf(0, -1)), // 1>>0
			arrayOf(intArrayOf(-1, 0), intArrayOf(-1, -1), intArrayOf(0, 2), intArrayOf(-1, 2), intArrayOf(0, -1)), // 2>>1
			arrayOf(intArrayOf(-1, 0), intArrayOf(-1, 1), intArrayOf(0, -2), intArrayOf(-1, -2), intArrayOf(0, -1)))// 3>>2
		private val WALLKICK_NORMAL_R = arrayOf(
			arrayOf(intArrayOf(-1, 0), intArrayOf(-1, -1), intArrayOf(0, 2), intArrayOf(-1, 2), intArrayOf(0, -1)), // 0>>1
			arrayOf(intArrayOf(1, 0), intArrayOf(1, 1), intArrayOf(0, -2), intArrayOf(1, -2), intArrayOf(0, -1)), // 1>>2
			arrayOf(intArrayOf(1, 0), intArrayOf(1, -1), intArrayOf(0, 2), intArrayOf(1, 2), intArrayOf(0, -1)), // 2>>3
			arrayOf(intArrayOf(-1, 0), intArrayOf(-1, 1), intArrayOf(0, -2), intArrayOf(-1, -2), intArrayOf(0, -1)))// 3>>0
		private val WALLKICK_I_L = arrayOf(
			arrayOf(intArrayOf(-1, 0), intArrayOf(2, 0), intArrayOf(-1, -2), intArrayOf(2, 1)), // 0>>3
			arrayOf(intArrayOf(2, 0), intArrayOf(-1, 0), intArrayOf(2, -1), intArrayOf(-1, 2)), // 1>>0
			arrayOf(intArrayOf(1, 0), intArrayOf(-2, 0), intArrayOf(1, 2), intArrayOf(-2, -1)), // 2>>1
			arrayOf(intArrayOf(-2, 0), intArrayOf(1, 0), intArrayOf(-2, 1), intArrayOf(1, -2)))// 3>>2
		private val WALLKICK_I_R = arrayOf(
			arrayOf(intArrayOf(-2, 0), intArrayOf(1, 0), intArrayOf(-2, 1), intArrayOf(1, -2)), // 0>>1
			arrayOf(intArrayOf(-1, 0), intArrayOf(2, 0), intArrayOf(-1, -2), intArrayOf(2, 1)), // 1>>2
			arrayOf(intArrayOf(2, 0), intArrayOf(-1, 0), intArrayOf(2, -1), intArrayOf(-1, 2)), // 2>>3
			arrayOf(intArrayOf(1, 0), intArrayOf(-2, 0), intArrayOf(1, 2), intArrayOf(-2, -1)))// 3>>0
		private val WALLKICK_I2_L = arrayOf(arrayOf(intArrayOf(1, 0), intArrayOf(0, -1), intArrayOf(1, -2)), // 0>>3
			arrayOf(intArrayOf(0, 1), intArrayOf(1, 0), intArrayOf(1, 1)), // 1>>0
			arrayOf(intArrayOf(-1, 0), intArrayOf(0, 1), intArrayOf(-1, 0)), // 2>>1
			arrayOf(intArrayOf(0, -1), intArrayOf(-1, 0), intArrayOf(-1, 1)))// 3>>2
		private val WALLKICK_I2_R = arrayOf(arrayOf(intArrayOf(0, -1), intArrayOf(-1, 0), intArrayOf(-1, -1)), // 0>>1
			arrayOf(intArrayOf(1, 0), intArrayOf(0, -1), intArrayOf(1, 0)), // 1>>2
			arrayOf(intArrayOf(0, 1), intArrayOf(1, 0), intArrayOf(1, -1)), // 2>>3
			arrayOf(intArrayOf(-1, 0), intArrayOf(0, 1), intArrayOf(-1, 2)))// 3>>0
		private val WALLKICK_I3_L = arrayOf(
			arrayOf(intArrayOf(1, 0), intArrayOf(-1, 0), intArrayOf(0, 0), intArrayOf(0, 0)), // 0>>3
			arrayOf(intArrayOf(-1, 0), intArrayOf(1, 0), intArrayOf(0, -1), intArrayOf(0, 1)), // 1>>0
			arrayOf(intArrayOf(-1, 0), intArrayOf(1, 0), intArrayOf(0, 2), intArrayOf(0, -2)), // 2>>1
			arrayOf(intArrayOf(1, 0), intArrayOf(-1, 0), intArrayOf(0, -1), intArrayOf(0, 1)))// 3>>2
		private val WALLKICK_I3_R = arrayOf(
			arrayOf(intArrayOf(1, 0), intArrayOf(-1, 0), intArrayOf(0, 1), intArrayOf(0, -1)), // 0>>1
			arrayOf(intArrayOf(1, 0), intArrayOf(-1, 0), intArrayOf(0, -2), intArrayOf(0, 2)), // 1>>2
			arrayOf(intArrayOf(-1, 0), intArrayOf(1, 0), intArrayOf(0, 1), intArrayOf(0, -1)), // 2>>3
			arrayOf(intArrayOf(-1, 0), intArrayOf(1, 0), intArrayOf(0, 0), intArrayOf(0, 0)))// 3>>0
		private val WALLKICK_L3_L = arrayOf(arrayOf(intArrayOf(0, -1), intArrayOf(0, 1)), // 0>>3
			arrayOf(intArrayOf(1, 0), intArrayOf(-1, 0)), // 1>>0
			arrayOf(intArrayOf(0, 1), intArrayOf(0, -1)), // 2>>1
			arrayOf(intArrayOf(-1, 0), intArrayOf(1, 0)))// 3>>2
		private val WALLKICK_L3_R = arrayOf(arrayOf(intArrayOf(-1, 0), intArrayOf(1, 0)), // 0>>1
			arrayOf(intArrayOf(0, -1), intArrayOf(0, 1)), // 1>>2
			arrayOf(intArrayOf(1, 0), intArrayOf(-1, 0)), // 2>>3
			arrayOf(intArrayOf(0, 1), intArrayOf(0, -1)))// 3>>0

		// 180-degree rotation wallkick data
		private val WALLKICK_NORMAL_180 = arrayOf(
			arrayOf(intArrayOf(1, 0), intArrayOf(2, 0), intArrayOf(1, 1), intArrayOf(2, 1), intArrayOf(-1, 0), intArrayOf(-2, 0),
				intArrayOf(-1, 1), intArrayOf(-2, 1), intArrayOf(0, -1), intArrayOf(3, 0), intArrayOf(-3, 0)), // 0>>2─┐
			arrayOf(intArrayOf(0, 1), intArrayOf(0, 2), intArrayOf(-1, 1), intArrayOf(-1, 2), intArrayOf(0, -1), intArrayOf(0, -2),
				intArrayOf(-1, -1), intArrayOf(-1, -2), intArrayOf(1, 0), intArrayOf(0, 3), intArrayOf(0, -3)), // 1>>3─┼┐
			arrayOf(intArrayOf(-1, 0), intArrayOf(-2, 0), intArrayOf(-1, -1), intArrayOf(-2, -1), intArrayOf(1, 0), intArrayOf(2, 0),
				intArrayOf(1, -1), intArrayOf(2, -1), intArrayOf(0, 1), intArrayOf(-3, 0), intArrayOf(3, 0)), // 2>>0─┘│
			arrayOf(intArrayOf(0, 1), intArrayOf(0, 2), intArrayOf(1, 1), intArrayOf(1, 2), intArrayOf(0, -1), intArrayOf(0, -2),
				intArrayOf(1, -1), intArrayOf(1, -2), intArrayOf(-1, 0), intArrayOf(0, 3), intArrayOf(0, -3)))// 3>>1──┘
		private val WALLKICK_I_180 = arrayOf(
			arrayOf(intArrayOf(-1, 0), intArrayOf(-2, 0), intArrayOf(1, 0), intArrayOf(2, 0), intArrayOf(0, 1)), // 0>>2─┐
			arrayOf(intArrayOf(0, 1), intArrayOf(0, 2), intArrayOf(0, -1), intArrayOf(0, -2), intArrayOf(-1, 0)), // 1>>3─┼┐
			arrayOf(intArrayOf(1, 0), intArrayOf(2, 0), intArrayOf(-1, 0), intArrayOf(-2, 0), intArrayOf(0, -1)), // 2>>0─┘│
			arrayOf(intArrayOf(0, 1), intArrayOf(0, 2), intArrayOf(0, -1), intArrayOf(0, -2), intArrayOf(1, 0)))// 3>>1──┘
	}
}
