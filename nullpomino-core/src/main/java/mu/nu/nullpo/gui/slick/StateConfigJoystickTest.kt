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
import mu.nu.nullpo.gui.slick.img.FontNormal
import org.newdawn.slick.*
import org.newdawn.slick.state.BasicGameState
import org.newdawn.slick.state.StateBasedGame

/** Joystick テスト画面のステート */
class StateConfigJoystickTest:BasicGameState() {

	/** Player number */
	var player = 0

	/** Screenshot撮影 flag */
	private var ssflag = false

	/** 使用するJoystick の number */
	private var joyNumber:Int = 0

	/** 最後に押された button */
	private var lastPressButton:Int = 0

	/** 経過 frame count */
	private var frame:Int = 0

	/** Buttoncount */
	private var buttonCount:Int = 0

	/** StateBasedGame */
	private lateinit var gameObj:StateBasedGame

	/* Fetch this state's ID */
	override fun getID():Int = ID

	/* State initialization */
	override fun init(container:GameContainer, game:StateBasedGame) {
		gameObj = game
	}

	/** いろいろリセット */
	private fun reset() {
		joyNumber = ControllerManager.controllerID[player]
		lastPressButton = -1
		frame = 0
		buttonCount = 0

		if(joyNumber>=0) buttonCount = ControllerManager.controllers!![joyNumber].buttonCount
	}

	/* Draw the screen */
	override fun render(container:GameContainer, game:StateBasedGame, g:Graphics) {
		if(!container.hasFocus()) {
			if(!NullpoMinoSlick.alternateFPSTiming) NullpoMinoSlick.alternateFPSSleep()
			return
		}

		ResourceHolder.imgMenuBG[0].draw(0f, 0f)

		FontNormal.printFontGrid(1, 1, "JOYSTICK INPUT TEST ("+(player+1)+"P)", COLOR.ORANGE)

		if(joyNumber<0)
			FontNormal.printFontGrid(1, 3, "NO JOYSTICK", COLOR.RED)
		else if(frame>=KEYACCEPTFRAME) {
			FontNormal.printFontGrid(1, 3, "JOYSTICK NUMBER:$joyNumber", COLOR.RED)

			FontNormal.printFontGrid(1, 5, "LAST PRESSED BUTTON:"+if(lastPressButton==-1) "NONE" else lastPressButton.toString())

			val controller = ControllerManager.controllers!![joyNumber]

			FontNormal.printFontGrid(1, 7, "AXIS X:"+controller.xAxisValue)
			FontNormal.printFontGrid(1, 8, "AXIS Y:"+controller.yAxisValue)

			FontNormal.printFontGrid(1, 10, "POV X:"+controller.povX)
			FontNormal.printFontGrid(1, 11, "POV Y:"+controller.povY)
		}

		if(frame>=KEYACCEPTFRAME) FontNormal.printFontGrid(1, 23, "ENTER/BACKSPACE: EXIT", COLOR.GREEN)

		// FPS
		NullpoMinoSlick.drawFPS()
		// Observer
		NullpoMinoSlick.drawObserverClient()
		if(!NullpoMinoSlick.alternateFPSTiming) NullpoMinoSlick.alternateFPSSleep()
	}

	/* Update game state */
	override fun update(container:GameContainer, game:StateBasedGame, delta:Int) {
		if(!container.hasFocus()) {
			if(NullpoMinoSlick.alternateFPSTiming) NullpoMinoSlick.alternateFPSSleep()
			return
		}

		frame++

		// Joystick button
		if(frame>=KEYACCEPTFRAME)
			for(i in 0 until buttonCount)
				try {
					if(ControllerManager.isControllerButton(player, container.input, i)) {
						ResourceHolder.soundManager.play("change")
						lastPressButton = i
					}
				} catch(e:Throwable) {
				}

		// JInput
		if(NullpoMinoSlick.useJInputKeyboard) {
			JInputManager.poll()

			if(frame>=KEYACCEPTFRAME)
				for(i in 0 until JInputManager.MAX_SLICK_KEY)
					if(JInputManager.isKeyDown(i)) {
						onKey(i)
						break
					}
		}

		if(NullpoMinoSlick.alternateFPSTiming) NullpoMinoSlick.alternateFPSSleep()
	}

	/* Called when a key is pressed (Slick native) */
	override fun keyPressed(key:Int, c:Char) {
		if(!NullpoMinoSlick.useJInputKeyboard) onKey(key)
	}

	/** When a key is pressed
	 * @param key Keycode
	 */
	private fun onKey(key:Int) {
		if(frame>=KEYACCEPTFRAME)
		// Backspace & Enter/Return
			if(key==Input.KEY_BACK||key==Input.KEY_RETURN) gameObj.enterState(StateConfigJoystickMain.ID)
	}

	/* Called when entering this state */
	override fun enter(container:GameContainer?, game:StateBasedGame?) {
		reset()
	}

	/* Called when leaving this state */
	override fun leave(container:GameContainer?, game:StateBasedGame?) {
		reset()
	}

	companion object {
		/** This state's ID */
		const val ID = 13

		/** Key input を受付可能になるまでの frame count */
		const val KEYACCEPTFRAME = 20
	}
}
