//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//
package wtf.oshisaure.nullpomodshit.wallkicks

import mu.nu.nullpo.game.component.Controller
import mu.nu.nullpo.game.component.Field
import mu.nu.nullpo.game.component.Piece
import mu.nu.nullpo.game.subsystem.wallkick.BaseStandardWallkick

class BlockdropWallkick:BaseStandardWallkick() {
	override fun getKickTable(x:Int, y:Int, rtDir:Int, rtOld:Int, rtNew:Int, allowUpward:Boolean, piece:Piece, field:Field,
		ctrl:Controller?):Array<Array<IntArray>>? = when(rtDir) {
		-1 -> kicktableL
		0 -> null
		1 -> kicktableR
		else -> null
	}

	companion object {
		private val kicktableL = arrayOf(
			arrayOf(intArrayOf(-1, 0), intArrayOf(1, 0), intArrayOf(0, 1), intArrayOf(-1, 1), intArrayOf(1, 1), intArrayOf(0, -1)),
			arrayOf(intArrayOf(-1, 0), intArrayOf(1, 0), intArrayOf(0, 1), intArrayOf(-1, 1), intArrayOf(1, 1), intArrayOf(0, -1)),
			arrayOf(intArrayOf(-1, 0), intArrayOf(1, 0), intArrayOf(0, 1), intArrayOf(-1, 1), intArrayOf(1, 1), intArrayOf(0, -1)),
			arrayOf(intArrayOf(-1, 0), intArrayOf(1, 0), intArrayOf(0, 1), intArrayOf(-1, 1), intArrayOf(1, 1), intArrayOf(0, -1)))
		private val kicktableR = arrayOf(
			arrayOf(intArrayOf(1, 0), intArrayOf(-1, 0), intArrayOf(0, 1), intArrayOf(1, 1), intArrayOf(-1, 1), intArrayOf(0, -1)),
			arrayOf(intArrayOf(1, 0), intArrayOf(-1, 0), intArrayOf(0, 1), intArrayOf(1, 1), intArrayOf(-1, 1), intArrayOf(0, -1)),
			arrayOf(intArrayOf(1, 0), intArrayOf(-1, 0), intArrayOf(0, 1), intArrayOf(1, 1), intArrayOf(-1, 1), intArrayOf(0, -1)),
			arrayOf(intArrayOf(1, 0), intArrayOf(-1, 0), intArrayOf(0, 1), intArrayOf(1, 1), intArrayOf(-1, 1), intArrayOf(0, -1)))
	}
}