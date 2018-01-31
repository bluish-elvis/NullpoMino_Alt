package mu.nu.nullpo.gui.common

abstract class ResourceHolder {

	abstract val imgBlockListSize:Int

	abstract fun getImgNormalBlock(skin:Int):AbstractImage

	abstract fun getImgSmallBlock(skin:Int):AbstractImage

	abstract fun getImgBigBlock(skin:Int):AbstractImage

	fun getBlockIsSticky(skin:Int):Boolean = skin in 0..(imgBlockListSize-1)&&blockStickyFlagList!![skin]

	companion object {

		/** BackgroundOfcount */
		const val BACKGROUND_MAX = 20

		/** Number of images for block spatter animation during line clears */
		const val BLOCK_BREAK_MAX = 8

		/** Number of image splits for block spatter animation during line clears */
		const val BLOCK_BREAK_SEGMENTS = 2

		/** Number of gem block clear effects */
		const val PERASE_MAX = 7

		/** Block sticky flag */
		var blockStickyFlagList:List<Boolean>? = null
	}
}
