package mu.nu.nullpo.gui.common

import java.util.*

abstract class ResourceHolder {

	abstract val imgBlockListSize:Int

	fun getBlockIsSticky(skin:Int):Boolean = skin in 0 until imgBlockListSize&&blockStickyFlagList[skin]

	/** Block sticky flag */
	open var blockStickyFlagList:LinkedList<Boolean> = LinkedList()

	/** BackgroundOfcount */
	internal open val BACKGROUND_MAX = 20

	/** Number of image splits for block spatter animation during line clears */
	abstract val BLOCK_BREAK_SEGMENTS:Int

	/** Number of images for block spatter animation during line clears */
	internal val BLOCK_BREAK_MAX = 8

	/** Number of gem block clear effects */
	internal val PERASE_MAX = 7
	internal val HANABI_MAX = 7

}
