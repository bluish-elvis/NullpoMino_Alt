package mu.nu.nullpo.gui.menu

class ToggleMenuItem @JvmOverloads constructor(name:String, color:Int, state:Int = 0, val drawStyle:Int = DRAWSTYLE_OX)
	:MenuItem(name) {

	init {
		this.name = name
		this.color = color
		this.state = state
	}

	override fun changeState(change:Int) {
		state = 1-state
	}


	companion object {

		const val DRAWSTYLE_OX = 0
		const val DRAWSTYLE_ONOFF = 1
	}
}
