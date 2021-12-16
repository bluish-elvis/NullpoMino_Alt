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
import org.apache.logging.log4j.LogManager

class PressFToFillTheScreenWallkick:Wallkick {
	override fun executeWallkick(
		x:Int, y:Int, rtDir:Int, rtOld:Int, rtNew:Int, allowUpward:Boolean, piece:Piece, field:Field, ctrl:Controller?
	):WallkickResult? {
		return if(!ctrl!!.isPress(9)) {
			null
		} else {
			piece.big = false
			var blockCount = 0
			var xi:Int
			run {
				var xi = 0
				while(xi<field.width) {
					xi = -field.hiddenHeight
					while(xi<field.height) {
						if(!this.isABlockOnTheField(xi, xi, field)) {
							++blockCount
						}
						++xi
					}
					++xi
				}
			}
			piece.block = Array(blockCount) {piece.block[0]}
			piece.dataX = Array(4) {IntArray(blockCount)}
			piece.dataY = Array(4) {IntArray(blockCount)}
			blockCount = 0
			xi = 0
			while(xi<field.width) {
				for(yi in -field.hiddenHeight until field.height) {
					if(!isABlockOnTheField(xi, yi, field)) {
						piece.dataX[0][blockCount] = xi
						piece.dataX[1][blockCount] = xi
						piece.dataX[2][blockCount] = xi
						piece.dataX[3][blockCount] = xi
						piece.dataY[0][blockCount] = yi
						piece.dataY[1][blockCount] = yi
						piece.dataY[2][blockCount] = yi
						piece.dataY[3][blockCount] = yi
						++blockCount
					}
				}
				++xi
			}
			WallkickResult(-x, -y, 0)
		}
	}

	fun isABlockOnTheField(x:Int, y:Int, fld:Field):Boolean =
		x>=fld.width||y>=fld.height||fld.getCoordAttribute(x, y)==3||fld.getCoordAttribute(x, y)!=2&&fld.getBlockColor(x, y)!=null

	companion object {
		var log = LogManager.getLogger()
	}
}
