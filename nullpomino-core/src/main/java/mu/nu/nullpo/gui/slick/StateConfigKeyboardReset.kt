package mu.nu.nullpo.gui.slick

import mu.nu.nullpo.game.event.EventReceiver.COLOR
import mu.nu.nullpo.gui.slick.img.FontNormal
import org.newdawn.slick.GameContainer
import org.newdawn.slick.Graphics
import org.newdawn.slick.state.StateBasedGame

/** Keyboard Reset menu */
class StateConfigKeyboardReset:DummyMenuChooseState() {

	/** Player number */
	var player = 0
	override val numChoice = 3

	/** Constructor */
	init {
		minChoiceY = 4
	}

	/* Fetch this state's ID */
	override fun getID():Int = ID

	/* State initialization */
	override fun init(arg0:GameContainer, arg1:StateBasedGame) {}

	/* Draw the screen */
	override fun renderImpl(container:GameContainer, game:StateBasedGame, g:Graphics) {
		// Background
		g.drawImage(ResourceHolder.imgMenuBG[0], 0f, 0f)

		// Menu
		FontNormal.printFontGrid(1, 1, "KEYBOARD RESET (${player+1}P)", COLOR.ORANGE)

		FontNormal.printFontGrid(1, 3, "RESET SETTINGS TO...", COLOR.GREEN)

		FontNormal.printFontGrid(1, 4+cursor, "\u0082", COLOR.RAINBOW)

		FontNormal.printFontGrid(2, 4, "BLOCKBOX STYLE (DEFAULT)", cursor==0)
		FontNormal.printFontGrid(2, 5, "GUIDELINE STYLE", cursor==1)
		FontNormal.printFontGrid(2, 6, "NULLPOMINO CLASSIC STYLE", cursor==2)
	}

	/* Decide */
	override fun onDecide(container:GameContainer, game:StateBasedGame, delta:Int):Boolean {
		ResourceHolder.soundManager.play("decide1")
		GameKey.gamekey[player].loadDefaultKeymap(cursor)
		GameKey.gamekey[player].saveConfig(NullpoMinoSlick.propConfig)
		NullpoMinoSlick.saveConfig()
		game.enterState(StateConfigMainMenu.ID)
		return true
	}

	/* Cancel */
	override fun onCancel(container:GameContainer, game:StateBasedGame, delta:Int):Boolean {
		game.enterState(StateConfigMainMenu.ID)
		return true
	}

	companion object {
		/** This state's ID */
		const val ID = 17
	}
}
