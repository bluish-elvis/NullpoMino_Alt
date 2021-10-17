package mu.nu.nullpo.game.subsystem.mode.menu

import mu.nu.nullpo.game.event.EventReceiver

open class StringsMenuItem(name:String, displayName:String, color:EventReceiver.COLOR, defaultValue:Int,
	val CHOICE_NAMES:Array<String>, compact:Boolean = false, perRule:Boolean = false):
	IntegerMenuItem(name, displayName, color, defaultValue, CHOICE_NAMES.indices, compact, perRule) {

	override val valueString:String
		get() = CHOICE_NAMES[value]
}