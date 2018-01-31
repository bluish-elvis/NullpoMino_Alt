package mu.nu.nullpo.gui.menu

import java.util.*

open class Menu @JvmOverloads constructor(
	val title:String,
	val subTitle:String,
	private val menuItems:Vector<MenuItem> = Vector()) {
	var selectedIndex:Int = 0

	init {
		selectedIndex = 0
	}

	constructor(title:String, menuItems:Vector<MenuItem>):this(title, "", menuItems)

	fun addMenuItem(menuItem:MenuItem) {
		menuItems.add(menuItem)

	}

	fun incIndex() {
		if(selectedIndex<=menuItems.size-2) selectedIndex++
	}

	fun decIndex() {
		if(selectedIndex>=1) selectedIndex--
	}

}
