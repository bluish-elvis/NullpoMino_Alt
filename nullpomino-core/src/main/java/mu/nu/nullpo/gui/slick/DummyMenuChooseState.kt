/* Copyright (c) 2010, NullNoname
 * All rights reserved.
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * Neither the name of NullNoname nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE. */
package mu.nu.nullpo.gui.slick

import mu.nu.nullpo.game.event.EventReceiver.COLOR
import mu.nu.nullpo.gui.GameKeyDummy
import org.newdawn.slick.*
import org.newdawn.slick.state.StateBasedGame

/** Dummy class for menus where the player picks from a list of options */
abstract class DummyMenuChooseState:BaseGameState() {
	/** Cursor position */
	protected var cursor = 0

	/** Max cursor value */
	abstract val maxCursor:Int

	/** Top choice's y-coordinate */
	protected open var minChoiceY = 3

	/** Set to false to ignore mouse input */
	protected open val mouseEnabled:Boolean = true


	@Throws(SlickException::class)
	override fun updateImpl(container:GameContainer, game:StateBasedGame, delta:Int) {
		// TTF font load
		ResourceHolder.ttfFont?.loadGlyphs()

		// Update key input states
		GameKey.gamekey[0].update(container.input)

		// Mouse
		var mouseConfirm = false
		if(mouseEnabled) mouseConfirm = updateMouseInput(container.input)

		if(maxCursor>=0) {
			// Cursor movement
			if(GameKey.gamekey[0].isMenuRepeatKey(GameKeyDummy.BUTTON_UP)) {
				cursor--
				if(cursor<0) cursor = maxCursor-1
				ResourceHolder.soundManager.play("cursor")
			}
			if(GameKey.gamekey[0].isMenuRepeatKey(GameKeyDummy.BUTTON_DOWN)) {
				cursor++
				if(cursor>=maxCursor) cursor = 0
				ResourceHolder.soundManager.play("cursor")
			}

			var change = 0
			if(GameKey.gamekey[0].isMenuRepeatKey(GameKeyDummy.BUTTON_LEFT)) change = -1
			if(GameKey.gamekey[0].isMenuRepeatKey(GameKeyDummy.BUTTON_RIGHT)) change = 1

			if(change!=0) onChange(container, game, delta, change)

			// Confirm button
			if(GameKey.gamekey[0].isPushKey(GameKeyDummy.BUTTON_A)||mouseConfirm)
				if(onDecide(container, game, delta)) return

		}
		if(GameKey.gamekey[0].isPushKey(GameKeyDummy.BUTTON_D)) {
			if(onPushButtonD(container, game, delta))
			return
		}

		// Cancel button
		if(GameKey.gamekey[0].isPushKey(GameKeyDummy.BUTTON_B)||MouseInput.isMouseRightClicked) {
			if(onCancel(container, game, delta))
			return
		}
	}

	protected open fun updateMouseInput(input:Input):Boolean {
		MouseInput.update(input)
		if(MouseInput.isMouseClicked) {
			val y = MouseInput.mouseY shr 4
			val newCursor = y-minChoiceY
			if(newCursor in 0 until maxCursor) {
				if(newCursor==cursor) return true
				ResourceHolder.soundManager.play("cursor")
				cursor = newCursor
			}
		}
		return false
	}

	protected fun renderChoices(x:Int, choices:Array<String>) {
		renderChoices(x, minChoiceY, choices)
	}

	protected fun renderChoices(x:Int, y:Int, choices:Array<String>) {
		FontNormal.printFontGrid(x-1, y+cursor, "b", COLOR.RED)
		choices.forEachIndexed {i,z->
			FontNormal.printFontGrid(x, y+i, z, cursor==i)
		}
	}

	/** Called when left or right is pressed. */
	protected open fun onChange(container:GameContainer, game:StateBasedGame, delta:Int, change:Int) {}

	/** Called on a decide operation (left click on highlighted entry or select
	 * button).
	 * @return True to skip all further update processing, false otherwise.
	 */
	protected open fun onDecide(container:GameContainer, game:StateBasedGame, delta:Int):Boolean = false

	/** Called on a cancel operation (right click or cancel button).
	 * @return True to skip all further update processing, false otherwise.
	 */
	protected open fun onCancel(container:GameContainer, game:StateBasedGame, delta:Int):Boolean = false

	/** Called when D button is pushed.
	 * Currently, this is the only one needed; methods for other buttons can be
	 * added if needed.
	 * @return True to skip all further update processing, false otherwise.
	 */
	protected open fun onPushButtonD(container:GameContainer, game:StateBasedGame, delta:Int):Boolean = false
}
