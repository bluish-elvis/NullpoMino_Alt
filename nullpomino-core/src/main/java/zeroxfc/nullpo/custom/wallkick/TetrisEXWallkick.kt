package zeroxfc.nullpo.custom.wallkick

import mu.nu.nullpo.game.component.*
import mu.nu.nullpo.game.subsystem.wallkick.Wallkick

class TetrisEXWallkick:Wallkick {
	private var currentDASCharge = 0
	private var maxDASCharge = 0
	private var dasDirection = 1
	override fun executeWallkick(x:Int, y:Int, rtDir:Int, rtOld:Int, rtNew:Int, allowUpward:Boolean,
		piece:Piece, field:Field, ctrl:Controller?):WallkickResult? {

		/*
         * Note about the selection of wallkicks:
         *
         * then comes to the hard part: the condition to use the "moving" tables.
         * it's not simply "the moving button is pushed" (at least both may be pushed while in the DAS system only one can be functioning).
         * the condition should be "the DAS is functioning but blocked (by mino or wall) (in the direction it's functioning)", like saying "there's pressure against the wall."
         * this is some fundamental difference and the "failed-to-move" information from DAS system needs to be passed to the wallkick subsystem.
         *
         * > as of right now, there is no implemented way of passing DAS information to the wallkick system, so button press will have to do for now.
         * > (kinda scared to modify the vanilla code directly; there might be a way to do it via reflection / mid-processing casting though)
         *
         * !! ignore the lines with an > above, the new system with mid-processing casting works.
         */

		/*
    	 * LEGACY:
    	 *
    	if (rtDir < 0 || rtDir == 2) {
			if (ctrl.isPress(Controller.BUTTON_LEFT) && (currentDASCharge >= maxDASCharge)) {
				table = KickTableLeftCCW;
			} else if (ctrl.isPress(Controller.BUTTON_RIGHT) && (currentDASCharge >= maxDASCharge)) {
				table = KickTableRightCCW;
			} else {
				table = KickTableCCW;
			}
		} else {
			if (ctrl.isPress(Controller.BUTTON_LEFT) && (currentDASCharge >= maxDASCharge)) {
				table = KickTableLeftCW;
			} else if (ctrl.isPress(Controller.BUTTON_RIGHT) && (currentDASCharge >= maxDASCharge)) {
				table = KickTableRightCW;
			} else {
				table = KickTableCW;
			}
		}
    	 */
		val table:Array<IntArray> = if(rtDir<0||rtDir==2) when {
			dasDirection==-1&&currentDASCharge>=maxDASCharge -> KickTableLeftCCW
			dasDirection==1&&currentDASCharge>=maxDASCharge -> KickTableRightCCW
			else -> KickTableCCW
		} else when {
			dasDirection==-1&&currentDASCharge>=maxDASCharge -> KickTableLeftCW
			dasDirection==1&&currentDASCharge>=maxDASCharge -> KickTableRightCW
			else -> KickTableCW
		}
		currentDASCharge = 0
		dasDirection = 0
		maxDASCharge = 1
		for(ints in table) {
			var x2 = ints[0]
			var y2 = ints[1]
			if(piece.big) {
				x2 *= 2
				y2 *= 2
			}
			if(!piece.checkCollision(x+x2, y+y2, rtNew, field)) return WallkickResult(x2, y2, rtNew)
		}
		return null
	}

	companion object {
		// Mirror X to get CW table.
		private val KickTableCCW = arrayOf(intArrayOf(0, 0), intArrayOf(-1, 0), intArrayOf(-1, 1), intArrayOf(0, 1),
			intArrayOf(1, 1), intArrayOf(1, 0), intArrayOf(0, -1))
		private val KickTableCW = arrayOf(intArrayOf(0, 0), intArrayOf(1, 0), intArrayOf(1, 1), intArrayOf(0, 1), intArrayOf(-1, 1),
			intArrayOf(-1, 0), intArrayOf(0, -1))
		// Mirror X to get CW Right table.
		private val KickTableLeftCCW = arrayOf(intArrayOf(-1, 0), intArrayOf(-1, 1), intArrayOf(-2, 0), intArrayOf(0, 0),
			intArrayOf(0, 1), intArrayOf(0, -1), intArrayOf(1, 1), intArrayOf(1, 0))
		private val KickTableRightCCW = arrayOf(intArrayOf(1, 0), intArrayOf(1, 1), intArrayOf(0, 1), intArrayOf(1, 2),
			intArrayOf(0, 0), intArrayOf(0, -1), intArrayOf(0, 1), intArrayOf(-1, 1))
		// Mirror X to get CCW Right table.
		private val KickTableLeftCW = arrayOf(intArrayOf(-1, 0), intArrayOf(-1, 1), intArrayOf(0, 1), intArrayOf(-1, 2),
			intArrayOf(0, 0), intArrayOf(0, -1), intArrayOf(0, 1), intArrayOf(1, 1))
		private val KickTableRightCW = arrayOf(intArrayOf(1, 0), intArrayOf(1, 1), intArrayOf(2, 0), intArrayOf(0, 0),
			intArrayOf(0, 1), intArrayOf(0, -1), intArrayOf(-1, 1), intArrayOf(-1, 0))
	}
}