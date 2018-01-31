package mu.nu.nullpo.game.subsystem.mode.menu

import mu.nu.nullpo.game.event.EventReceiver.COLOR

abstract class EnumMenuItem(name:String, displayName:String, color:COLOR, defaultValue:Int, val CHOICE_NAMES:Array<String>):
	IntegerMenuItem(name, displayName, color, defaultValue, 0, CHOICE_NAMES.size-1) {

	override val valueString:String
		get() = CHOICE_NAMES[value]
}
