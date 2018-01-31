package mu.nu.nullpo.game.subsystem.mode.menu

import mu.nu.nullpo.game.event.EventReceiver.COLOR
import mu.nu.nullpo.util.GeneralUtil

class TimeMenuItem constructor(name:String, displayName:String, color:COLOR, defaultValue:Int, min:Int, max:Int, val increment:Int = 60)
	:IntegerMenuItem(name, displayName, color, defaultValue, min, max) {

	override val valueString:String
		get() = GeneralUtil.getTime(value.toFloat())

	override fun change(dir:Int, fast:Int) {
		val delta = dir*increment
		value += delta
		if(value<min) value = max
		if(value>max) value = min
	}
}
