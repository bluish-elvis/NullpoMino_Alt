package mu.nu.nullpo.gui.common

import mu.nu.nullpo.game.component.Piece
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

	internal val SE_LIST = arrayListOf("cursor", "change", "decide", "cancel", "pause",
		"hold", "initialhold", "holdfail", "move", "movefail",
		"rotate", "wallkick", "initialrotate", "rotfail",
		"harddrop", "softdrop", "step", "lock",
		"erase", "linefall", "linefall1", "cheer", "twist", "twister",
		"combo","combo_power", "b2b_start", "b2b_combo", "b2b_end",

		"danger", "dead", "dead_last", "shutter",
		"gradeup", "levelstop", "levelup", "levelup_section",
		"endingstart", "excellent",
		"bravo", "cool", "regret",

		"countdown", "hurryup", "timeout",
		"gamelost", "gamewon", "stageclear", "stagefail", "matchend",
		"gem", "bomb", "square_s", "square_g")
		.also {a ->
			a.addAll(listOf(
				(0..1).flatMap {listOf("start$it", "garbage$it", "crowd$it")},
				(0..2).flatMap {listOf("decide$it", "erase$it", "firecracker$it")},
				(0..4).map {"grade$it"}, (0..5).map {"applause$it"},
				Piece.Shape.names.map {"piece_${it.lowercase(Locale.getDefault())}"},
				(1..3).map {"medal$it"},
				(1..4).map {"line$it"}
			).flatten())
		}

}
