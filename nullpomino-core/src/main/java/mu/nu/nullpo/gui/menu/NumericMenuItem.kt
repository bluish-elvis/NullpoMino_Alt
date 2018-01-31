package mu.nu.nullpo.gui.menu

open class NumericMenuItem
constructor(
	name:String,
	color:Int,
	state:Int = 50,
	private val minValue:Int = 0,
	private val maxValue:Int = 100,
	private val step:Int = 1,
	private val arithmeticStyle:Int = ARITHSTYLE_MODULAR
):MenuItem(name) {

	init {
		this.color = color
		this.state = state
	}

	override fun changeState(change:Int) {
		state += step*change
		val range = maxValue-minValue
		if(state>maxValue)
			when(arithmeticStyle) {
				ARITHSTYLE_MODULAR -> do
					state -= range
				while(state>maxValue)
				ARITHSTYLE_SATURATE -> state = maxValue
			}
		else if(state<minValue)
			when(arithmeticStyle) {
				ARITHSTYLE_MODULAR -> do
					state += range
				while(state<maxValue)
				ARITHSTYLE_SATURATE -> state = minValue
			}
	}


	companion object {

		const val ARITHSTYLE_MODULAR = 0
		const val ARITHSTYLE_SATURATE = 1
	}
}
