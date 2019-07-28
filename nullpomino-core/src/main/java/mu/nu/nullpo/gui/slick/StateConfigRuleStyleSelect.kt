package mu.nu.nullpo.gui.slick

import mu.nu.nullpo.game.event.EventReceiver.COLOR
import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.gui.slick.img.FontNormal
import org.newdawn.slick.GameContainer
import org.newdawn.slick.Graphics
import org.newdawn.slick.state.StateBasedGame

/** Style select menu */
class StateConfigRuleStyleSelect:DummyMenuChooseState() {

	/** Player number */
	var player = 0

	override val maxCursor = GameEngine.MAX_GAMESTYLE-1
	init {
		minChoiceY = 3
	}

	/* Fetch this state's ID */
	override fun getID():Int = ID

	/* State initialization */
	override fun init(container:GameContainer, game:StateBasedGame) {}

	/* Draw the screen */
	override fun renderImpl(container:GameContainer, game:StateBasedGame, g:Graphics) {
		// Background
		g.drawImage(ResourceHolder.imgMenuBG[0], 0f, 0f)

		// Menu
		FontNormal.printFontGrid(1, 1, "SELECT ${player+1}P STYLE", COLOR.ORANGE)

		FontNormal.printFontGrid(1, 3+cursor, "b", COLOR.RED)

		for(i in 0 until GameEngine.MAX_GAMESTYLE)
			FontNormal.printFontGrid(2, 3+i, GameEngine.GAMESTYLE_NAMES[i], cursor==i)
	}

	/* Decide */
	override fun onDecide(container:GameContainer, game:StateBasedGame, delta:Int):Boolean {
		ResourceHolder.soundManager.play("decide0")
		NullpoMinoSlick.stateConfigRuleSelect.player = player
		NullpoMinoSlick.stateConfigRuleSelect.style = cursor
		game.enterState(StateConfigRuleSelect.ID)
		return false
	}

	/* Cancel */
	override fun onCancel(container:GameContainer, game:StateBasedGame, delta:Int):Boolean {
		game.enterState(StateConfigMainMenu.ID)
		return false
	}

	companion object {
		/** This state's ID */
		const val ID = 15
	}
}
