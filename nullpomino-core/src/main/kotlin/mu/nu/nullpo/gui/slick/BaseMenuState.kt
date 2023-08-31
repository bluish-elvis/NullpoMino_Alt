/*
 * Copyright (c) 2010-2022, NullNoname
 * Kotlin converted and modified by Venom=Nhelv.
 * THIS WAS NOT MADE IN ASSOCIATION WITH THE GAME CREATOR.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of NullNoname nor the names of its
 *       contributors may be used to endorse or promote products derived from
 *       this software without specific prior written permission.
 *
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
 * POSSIBILITY OF SUCH DAMAGE.
 */

package mu.nu.nullpo.gui.slick

import mu.nu.nullpo.gui.common.GameKeyDummy
import mu.nu.nullpo.gui.slick.img.FontNano
import org.newdawn.slick.GameContainer
import org.newdawn.slick.Graphics
import org.newdawn.slick.Image
import org.newdawn.slick.SlickException
import org.newdawn.slick.state.BasicGameState
import org.newdawn.slick.state.StateBasedGame

/** Base state */
abstract class BaseMenuState:BasicGameState() {
	/** Screen Shot flag (Declared in BaseGameState; Don't override it!) */
	protected var screenShotFlag = false

	/* Fetch this state's ID */
	override fun getID() = 0
	private var beamY = 0f
	private var beamT = 32
	/** Set to false for ignore mouse input */
	protected open val mouseEnabled = true
	fun emit(y:Float) {
		beamY = y
		beamT = 0
	}

	fun emit(y:Number) = emit(y.toFloat())
	fun emitGrid(y:Number) = emit(y.toFloat()*16)

	/* State initialization */
	override fun init(container:GameContainer, game:StateBasedGame) {
	}

	override fun enter(container:GameContainer?, game:StateBasedGame?) {
		val i = ResourceHolder.imgLine[1].copy()
		ig =
			Image(i.height, i.width).apply {
				graphics.clear()
				i.rotate(270f)
				i.setCenterOfRotation(i.width/2f, i.width/2f)
				graphics.drawImage(i, 0f, 0f)
			}.getFlippedCopy(true, false)
	}

	override fun leave(container:GameContainer?, game:StateBasedGame?) {
		ig.destroy()
	}

	/** Draw the screen. BaseGameState will do the common things here.
	 *  (such as Framerate Cap or Screen Shot)
	 * Your code will be in renderImpl, unless if you want to do something special. */
	override fun render(container:GameContainer, game:StateBasedGame, g:Graphics) {
		// Lost the focus
		if(!container.hasFocus()) {
			if(!NullpoMinoSlick.alternateFPSTiming) NullpoMinoSlick.alternateFPSSleep()
			return
		}

		g.isAntiAlias = false
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

	/** Update the game. BaseGameState will do the common things here.
	 *  (such as Framerate Cap or Screen Shot)
	 * Your code will be in updateImpl, unless if you want to do something special. */
	@Throws(SlickException::class)
	override fun update(container:GameContainer, game:StateBasedGame, delta:Int) {
		// Lost the focus
		if(!container.hasFocus()) {
			GameKey.gameKey[0].clear()
			GameKey.gameKey[1].clear()
			if(NullpoMinoSlick.alternateFPSTiming) NullpoMinoSlick.alternateFPSSleep()
			return
		}

		// Do user's code
		updateImpl(container, game, delta)

		// Screenshot button
		if(GameKey.gameKey[0].isPushKey(GameKeyDummy.BUTTON_SCREENSHOT)) screenShotFlag = true
		// Exit button
		if(GameKey.gameKey[0].isPushKey(GameKeyDummy.BUTTON_QUIT)) container.exit()

		// Framerate Cap
		if(NullpoMinoSlick.alternateFPSTiming) NullpoMinoSlick.alternateFPSSleep()
	}

	/** Draw the screen. Your code will be here, unless if you want to do something special.
	 * @param container GameContainer
	 * @param game StateBasedGame
	 * @param g Graphics
	 */
	protected open fun renderImpl(container:GameContainer, game:StateBasedGame, g:Graphics) {
		if(mouseEnabled&&MouseInput.isMousePressed) {
			val x = MouseInput.mouseX shr 4
			val y = MouseInput.mouseY shr 4
			val z = minOf(MouseInput.mouseHold+1, MouseInput.das)*8/(MouseInput.das+1)
			ResourceHolder.imgCursor.draw(
				x*16f-8, y*16f-8, x*16f+24, y*16f+24,
				32f*(z%4), 32f*(z/4), 32f*(1+(z%4)), 32f*(1+z/4)
			)
			FontNano.printFont(/*x*16-8, y*16-8*/0, 0, "$x\n$y\n${MouseInput.mouseHold}", scale = .5f)
		}    // Menu
		if(beamT in 0..<32) {
			val i = beamT/3
			val j = beamT/2
			val y = beamY
			g.setDrawMode(Graphics.MODE_ADD)
			if(i<8)
				ResourceHolder.imgLine[0].draw(
					0f, y, 640f, y+16, 0f, 8f*i, 80f, 8f*(1+i)
				)
//			if(j<16)
			ig.draw(
				0f, y, 640f, y+16, 0f, 16f*j, 160f, 16f*(1+j)
			)

			g.setDrawMode(Graphics.MODE_NORMAL)
			beamT++
		}
	}

	/** Update the game. Your code will be here, unless if you want to do something special.
	 * @param container GameContainer
	 * @param game StateBasedGame
	 * @param delta Time passed since the last execution
	 * @throws SlickException Something failed
	 */
	@Throws(SlickException::class)
	protected open fun updateImpl(container:GameContainer, game:StateBasedGame, delta:Int) {
	}

	companion object {
		private var ig = Image(1, 1)
	}
}
