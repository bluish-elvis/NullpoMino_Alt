package mu.nu.nullpo.game.subsystem.mode.menu

import mu.nu.nullpo.game.event.EventReceiver.COLOR
import mu.nu.nullpo.util.CustomProperties

open class IntegerMenuItem(name:String, displayName:String, color:COLOR, defaultValue:Int, val min:Int, val max:Int)
	:AbstractMenuItem<Int>(name, displayName, color, defaultValue) {

	override val valueString:String
		get() = value.toString()

	override fun change(dir:Int, fast:Int) {
		value += dir
		if(value<min) value = max
		if(value>max) value = min
	}

	override fun save(playerID:Int, prop:CustomProperties, modeName:String) {
		prop.setProperty(modeName+"."+name
			+if(playerID<0) "" else ".p$playerID", value)
	}

	override fun load(playerID:Int, prop:CustomProperties, modeName:String) {
		value = prop.getProperty(modeName+"."+name
			+if(playerID<0) "" else ".p$playerID", DEFAULT_VALUE)
	}
}
