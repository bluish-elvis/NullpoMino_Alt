/*
 * Copyright (c) 2010-2021, NullNoname
 * Kotlin converted and modified by Venom=Nhelv
 * All rights reserved.
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

import mu.nu.nullpo.game.event.EventReceiver.COLOR
import mu.nu.nullpo.gui.common.BaseFont
import mu.nu.nullpo.gui.common.GameKeyDummy
import mu.nu.nullpo.gui.slick.img.FontNormal
import org.newdawn.slick.GameContainer
import org.newdawn.slick.Graphics
import org.newdawn.slick.Input
import org.newdawn.slick.state.BasicGameState
import org.newdawn.slick.state.StateBasedGame

/** Joystick button設定画面のステート */
class StateConfigJoystickButton:BasicGameState() {
	/** Player number */
	var player = 0

	/** StateBasedGame */
	private lateinit var gameObj:StateBasedGame

	/** 使用するJoystick の number */
	private var joyNumber = 0

	/** Number of button currently being configured */
	private var keynum = 0
	private var keypos = 0

	/** 経過 frame count */
	private var frame = 0

	/** Button settings */
	private var buttonmap:Array<IntArray> = Array(GameKeyDummy.MAX_BUTTON) {IntArray(MAX_KEYS) {-1}}

	/* Fetch this state's ID */
	override fun getID():Int = ID

	/** Button settings initialization */
	private fun reset() {
		keynum = 4
		frame = 0

		buttonmap = Array(GameKeyDummy.MAX_BUTTON) {IntArray(MAX_KEYS)}

		joyNumber = ControllerManager.controllerID[player]

		System.arraycopy(GameKey.gameKey[player].buttonmap, 0, buttonmap, 0, GameKeyDummy.MAX_BUTTON)
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

		g.drawImage(ResourceHolder.imgMenuBG[1], 0f, 0f)

		FontNormal.printFontGrid(1, 1, "JOYSTICK setting (${player+1}P)", COLOR.ORANGE)

		FontNormal.printFontGrid(1, 3, if(joyNumber<0) "Not Found Joystick" else "Joystick Number:$joyNumber", COLOR.RED)
		for(it in 4..<GameKeyDummy.MAX_BUTTON) {
			val flag = keynum==it
			FontNormal.printFontGrid(2, it+1, GameKeyDummy.arrayKeyName(false)[it], flag)
			FontNormal.printFontGrid(13, it+1, ":", flag)
			buttonmap[it].forEachIndexed {i, key -> FontNormal.printFontGrid(15+i*3, it+1, "$key", flag)}
		}

		FontNormal.printFontGrid(14+keypos*3, 1+keynum, BaseFont.CURSOR, COLOR.RAINBOW)
		FontNormal.printFontGrid(1, 5+keynum-4, BaseFont.CURSOR, COLOR.RAINBOW)
		if(frame>=KEYACCEPTFRAME) {
			FontNormal.printFontGrid(1, 22, "<\u008B\u008E>:   MOVE CURSOR", COLOR.GREEN)
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
			for(i in 0..<ControllerManager.MAX_BUTTONS)
				try {
					if(ControllerManager.isControllerButton(player, container.input, i)) {
						ResourceHolder.soundManager.play("change")
						buttonmap[keynum][keypos] = i
						frame = 0
					}
				} catch(_:Throwable) {
				}

		// JInput
		if(NullpoMinoSlick.useJInputKeyboard) {
			JInputManager.poll()

			if(frame>=KEYACCEPTFRAME)
				for(i in 0..<JInputManager.MAX_SLICK_KEY)
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
			} else if(key==Input.KEY_LEFT) {
				ResourceHolder.soundManager.play("change")
				keypos--
				if(keypos<0) keypos = StateConfigKeyboard.MAX_KEYS-1
			} else if(key==Input.KEY_RIGHT) {
				ResourceHolder.soundManager.play("change")
				keypos++
				if(keypos>=StateConfigKeyboard.MAX_KEYS) keypos = 0
			} else if(key==Input.KEY_DELETE) {
				ResourceHolder.soundManager.play("change")
				buttonmap[keynum][keypos] = -1
			} else if(key==Input.KEY_BACK) {
				gameObj.enterState(StateConfigJoystickMain.ID)
				return
			} else if(key==Input.KEY_ENTER) {
				ResourceHolder.soundManager.play("decide1")

				System.arraycopy(buttonmap, 0, GameKey.gameKey[player].buttonmap, 0, GameKeyDummy.MAX_BUTTON)
				GameKey.gameKey[player].saveConfig(NullpoMinoSlick.propConfig)
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

		const val MAX_KEYS = 3
	}
}
