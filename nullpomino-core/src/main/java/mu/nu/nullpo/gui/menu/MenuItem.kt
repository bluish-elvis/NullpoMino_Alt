package mu.nu.nullpo.gui.menu

abstract class MenuItem constructor(var name:String, val description:String = "") {
	var color:Int = 0
	var state:Int = 0

	/** Changes the state of the MenuItem.
	 * @param change the amount to change the internal state.
	 */
	abstract fun changeState(change:Int)

}
