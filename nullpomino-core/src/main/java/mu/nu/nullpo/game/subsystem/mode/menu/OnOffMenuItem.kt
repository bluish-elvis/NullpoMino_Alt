package mu.nu.nullpo.game.subsystem.mode.menu

import mu.nu.nullpo.game.event.EventReceiver.COLOR
import mu.nu.nullpo.util.GeneralUtil

class OnOffMenuItem(name:String, displayName:String, color:COLOR,
	defaultValue:Boolean):BooleanMenuItem(name, displayName, color, defaultValue) {

	override val valueString:String
		get() = GeneralUtil.getONorOFF(value)
}
