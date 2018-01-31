package mu.nu.nullpo.game.subsystem.mode.menu

import mu.nu.nullpo.game.event.EventReceiver.COLOR
import mu.nu.nullpo.util.CustomProperties

abstract class AbstractMenuItem<T>(val name:String, val displayName:String, val color:COLOR,
	val DEFAULT_VALUE:T) {
	var value:T = DEFAULT_VALUE

	abstract val valueString:String

	/** Change the aint.
	 * @param dir Direction pressed: -1 = left, 1 = right.
	 * If 0, update without changing any settings.
	 * @param fast 0 by default, +1 if E held, +2 if F held.
	 */
	abstract fun change(dir:Int, fast:Int)

	abstract fun save(playerID:Int, prop:CustomProperties, modeName:String)

	abstract fun load(playerID:Int, prop:CustomProperties, modeName:String)
}
