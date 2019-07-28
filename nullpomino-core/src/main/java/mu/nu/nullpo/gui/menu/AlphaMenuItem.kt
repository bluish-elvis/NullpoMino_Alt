package mu.nu.nullpo.gui.menu

import java.util.Arrays
import java.util.Vector

class AlphaMenuItem(name:String, color:Int, choiceList:Vector<String>)
	:NumericMenuItem(name, color, 0, 0, choiceList.size, -1, ARITHSTYLE_MODULAR) {
	init {
		state = 0
	}
	constructor(name:String, color:Int, choiceList:Array<String>):this(name, color, Vector<String>(listOf<String>(*choiceList)))

}
