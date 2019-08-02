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
import mu.nu.nullpo.gui.slick.img.FontNormal
import org.newdawn.slick.*
import org.newdawn.slick.state.BasicGameState
import org.newdawn.slick.state.StateBasedGame

/** Joystick button設定画面のステート */
class StateConfigJoystickButton:BasicGameState() {

	/** Player number */
	var player = 0

	/** StateBasedGame */
	private lateinit var gameObj:StateBasedGame

	/** 使用するJoystick の number */
	private var joyNumber:Int = 0

	/** Number of button currently being configured */
	private var keynum:Int = 0

	/** 経過 frame count */
	private var frame:Int = 0

	/** Button settings */
	private var buttonmap:IntArray = IntArray(GameKeyDummy.MAX_BUTTON)

	/* Fetch this state's ID */
	override fun getID():Int = ID

	/** Button settings initialization */
	private fun reset() {
		keynum = 4
		frame = 0

		buttonmap = IntArray(GameKeyDummy.MAX_BUTTON)

		joyNumber = ControllerManager.controllerID[player]

		System.arraycopy(GameKey.gamekey[player].buttonmap, 0, buttonmap, 0, GameKeyDummy.MAX_BUTTON)
	}

	/* State initialization */
	override fun init(container:GameContainer, game:StateBasedGame) {
		gameObj = game
	}

	/* Draw the screen */
	override fun render(container:GameContainer, game:StateBasedGame, g:Graphics) {
		if(!container.hasFocus()) {
			if(!NullpoMinoSlick.alternateFPSTiming) NullpoMinoSlick.alternateFPSSleep()
			return
		}

		g.drawImage(ResourceHolder.imgMenuBG[0], 0f, 0f)

		FontNormal.printFontGrid(1, 1, "JOYSTICK SETTING (${player+1}P)", COLOR.ORANGE)


			FontNormal.printFontGrid(1, 3, if(joyNumber<0)"NO JOYSTICK" else "JOYSTICK NUMBER:$joyNumber", COLOR.RED)

		//FontNormal.printFontGrid(2, 3, "UP             : " + String.valueOf(buttonmap[GameKey.BUTTON_UP]), (keynum == 0));
		//FontNormal.printFontGrid(2, 4, "DOWN           : " + String.valueOf(buttonmap[GameKey.BUTTON_DOWN]), (keynum == 1));
		//FontNormal.printFontGrid(2, 5, "LEFT           : " + String.valueOf(buttonmap[GameKey.BUTTON_LEFT]), (keynum == 2));
		//FontNormal.printFontGrid(2, 6, "RIGHT          : " + String.valueOf(buttonmap[GameKey.BUTTON_RIGHT]), (keynum == 3));
		FontNormal.printFontGrid(2, 5, "A (L/R-ROT)    : ${buttonmap[GameKeyDummy.BUTTON_A]}", keynum==4)
		FontNormal.printFontGrid(2, 6, "B (R/L-ROT)    : ${buttonmap[GameKeyDummy.BUTTON_B]}", keynum==5)
		FontNormal.printFontGrid(2, 7, "C (L/R-ROT)    : ${buttonmap[GameKeyDummy.BUTTON_C]}", keynum==6)
		FontNormal.printFontGrid(2, 8, "D (HOLD)       : ${buttonmap[GameKeyDummy.BUTTON_D]}", keynum==7)
		FontNormal.printFontGrid(2, 9, "E (180-ROT)    : ${buttonmap[GameKeyDummy.BUTTON_E]}", keynum==8)
		FontNormal.printFontGrid(2, 10, "F              : ${buttonmap[GameKeyDummy.BUTTON_F]}", keynum==9)
		FontNormal.printFontGrid(2, 11, "QUIT           : ${buttonmap[GameKeyDummy.BUTTON_QUIT]}", keynum==10)
		FontNormal.printFontGrid(2, 12, "PAUSE          : ${buttonmap[GameKeyDummy.BUTTON_PAUSE]}", keynum==11)
		FontNormal.printFontGrid(2, 13, "GIVEUP         : ${buttonmap[GameKeyDummy.BUTTON_GIVEUP]}", keynum==12)
		FontNormal.printFontGrid(2, 14, "RETRY          : ${buttonmap[GameKeyDummy.BUTTON_RETRY]}", keynum==13)
		FontNormal.printFontGrid(2, 15, "FRAME STEP     : ${buttonmap[GameKeyDummy.BUTTON_FRAMESTEP]}", keynum==14)
		FontNormal.printFontGrid(2, 16, "SCREEN SHOT    : ${buttonmap[GameKeyDummy.BUTTON_SCREENSHOT]}", keynum==15)

		FontNormal.printFontGrid(1, 5+keynum-4, "b", COLOR.RED)
		if(frame>=KEYACCEPTFRAME) {
			FontNormal.printFontGrid(1, 20, "UP/DOWN:   MOVE CURSOR", COLOR.GREEN)
			FontNormal.printFontGrid(1, 21, "ENTER:     OK", COLOR.GREEN)
			FontNormal.printFontGrid(1, 22, "DELETE:    NO SET", COLOR.GREEN)
			FontNormal.printFontGrid(1, 23, "BACKSPACE: CANCEL", COLOR.GREEN)
		}

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
			for(i in 0 until ControllerManager.MAX_BUTTONS)
				try {
					if(ControllerManager.isControllerButton(player, container.input, i)) {
						ResourceHolder.soundManager.play("change")
						buttonmap[keynum] = i
						frame = 0
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
						frame = KEYACCEPTFRAME/2
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
		// Up
			if(key==Input.KEY_UP) {
				ResourceHolder.soundManager.play("cursor")
				keynum--
				if(keynum<4) keynum = 15
			} else if(key==Input.KEY_DOWN) {
				ResourceHolder.soundManager.play("cursor")
				keynum++
				if(keynum>15) keynum = 4
			} else if(key==Input.KEY_DELETE) {
				ResourceHolder.soundManager.play("change")
				buttonmap[keynum] = -1
			} else if(key==Input.KEY_BACK) {
				gameObj.enterState(StateConfigJoystickMain.ID)
				return
			} else if(key==Input.KEY_ENTER) {
				ResourceHolder.soundManager.play("decide1")

				System.arraycopy(buttonmap, 0, GameKey.gamekey[player].buttonmap, 0, GameKeyDummy.MAX_BUTTON)
				GameKey.gamekey[player].saveConfig(NullpoMinoSlick.propConfig)
				NullpoMinoSlick.saveConfig()

				gameObj.enterState(StateConfigJoystickMain.ID)
				return
			}// Enter/Return
		// Backspace
		// Delete
		// Down
	}

	/** Called when entering this state */
	override fun enter(container:GameContainer?, game:StateBasedGame?) {
		reset()
	}

	/** Called when leaving this state */
	override fun leave(container:GameContainer?, game:StateBasedGame?) {
		reset()
	}

	companion object {
		/** This state's ID */
		const val ID = 10

		/** Key input を受付可能になるまでの frame count */
		const val KEYACCEPTFRAME = 20
	}
}
