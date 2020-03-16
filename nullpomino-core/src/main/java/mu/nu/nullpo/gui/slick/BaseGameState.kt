package mu.nu.nullpo.gui.slick

import mu.nu.nullpo.game.event.EventReceiver
import mu.nu.nullpo.gui.GameKeyDummy
import mu.nu.nullpo.gui.slick.img.FontNano
import mu.nu.nullpo.gui.slick.img.FontNormal
import org.newdawn.slick.*
import org.newdawn.slick.state.BasicGameState
import org.newdawn.slick.state.StateBasedGame

/** Base state */
abstract class BaseGameState:BasicGameState() {
	/** Screen Shot flag (Declared in BaseGameState; Don't override it!) */
	protected var screenShotFlag = false

	/* Fetch this state's ID */
	override fun getID():Int = 0

	/* State initialization */
	override fun init(container:GameContainer, game:StateBasedGame) {

	}

	/** Draw the screen. BaseGameState will do the common things (such as
	 * Framerate Cap or Screen Shot) here.
	 * Your code will be in renderImpl, unless if you want do something
	 * special. */
	override fun render(container:GameContainer, game:StateBasedGame, g:Graphics) {
		// Lost the focus
		if(!container.hasFocus()) {
			if(!NullpoMinoSlick.alternateFPSTiming) NullpoMinoSlick.alternateFPSSleep()
			return
		}

		// Do user's code
		renderImpl(container, game, g)

		// Do common things
		NullpoMinoSlick.drawFPS() // FPS counter
		NullpoMinoSlick.drawObserverClient() // Observer
		if(screenShotFlag) {
			// Create a screenshot
			NullpoMinoSlick.saveScreenShot(container, g)
			screenShotFlag = false
		}

		// Framerate Cap
		if(!NullpoMinoSlick.alternateFPSTiming) NullpoMinoSlick.alternateFPSSleep()
	}

	/** Update the game. BaseGameState will do the common things (such as
	 * Framerate Cap or Screen Shot) here.
	 * Your code will be in updateImpl, unless if you want do something
	 * special. */
	@Throws(SlickException::class)
	override fun update(container:GameContainer, game:StateBasedGame, delta:Int) {
		// Lost the focus
		if(!container.hasFocus()) {
			GameKey.gamekey[0].clear()
			GameKey.gamekey[1].clear()
			if(NullpoMinoSlick.alternateFPSTiming) NullpoMinoSlick.alternateFPSSleep()
			return
		}

		// Do user's code
		updateImpl(container, game, delta)

		// Screenshot button
		if(GameKey.gamekey[0].isPushKey(GameKeyDummy.BUTTON_SCREENSHOT)) screenShotFlag = true
		// Exit button
		if(GameKey.gamekey[0].isPushKey(GameKeyDummy.BUTTON_QUIT)) container.exit()

		// Framerate Cap
		if(NullpoMinoSlick.alternateFPSTiming) NullpoMinoSlick.alternateFPSSleep()
	}

	/** Draw the screen. Your code will be here, unless if you want do something
	 * special.
	 * @param container GameContainer
	 * @param game StateBasedGame
	 * @param g Graphics
	 */
	protected open fun renderImpl(container:GameContainer, game:StateBasedGame, g:Graphics) {

		if(MouseInput.isMousePressed) {
			val x = MouseInput.mouseX shr 4
			val y = MouseInput.mouseY shr 4
			val z = minOf(MouseInput.mouseHold+1,MouseInput.das)*8/(MouseInput.das+1)
			ResourceHolder.imgCursor.draw(
				x*16f-8, y*16f-8, x*16f+24, y*16f+24, 32f*(z%4), 32f*(z/4), 32f*(1+(z%4)), 32f*(1+z/4))
			FontNano.printFont(/*x*16-8, y*16-8*/0,0,"$x\n$y\n${MouseInput.mouseHold}",scale=.5f)
		}
	}

	/** Update the game. Your code will be here, unless if you want do something
	 * special.
	 * @param container GameContainer
	 * @param game StateBasedGame
	 * @param delta Time passed since the last execution
	 * @throws SlickException Something failed
	 */
	@Throws(SlickException::class)
	protected open fun updateImpl(container:GameContainer, game:StateBasedGame, delta:Int) {
	}
}
