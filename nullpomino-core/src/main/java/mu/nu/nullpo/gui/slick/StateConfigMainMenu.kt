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

import mu.nu.nullpo.game.component.BGMStatus.BGM
import mu.nu.nullpo.game.event.EventReceiver.COLOR
import mu.nu.nullpo.gui.slick.img.FontNormal
import org.newdawn.slick.GameContainer
import org.newdawn.slick.Graphics
import org.newdawn.slick.state.StateBasedGame

/** Options screen */
class StateConfigMainMenu:DummyMenuChooseState() {

	/** Player number */
	private var player = 0

	override val maxCursor = 7
	init {
		minChoiceY = 3
	}

	/* Fetch this state's ID */
	override fun getID():Int = ID

	/* State initialization */
	override fun init(container:GameContainer, game:StateBasedGame) {}

	override fun enter(container:GameContainer?, game:StateBasedGame?) {
		if(!ResourceHolder.bgmIsPlaying()) ResourceHolder.bgmStart(BGM.MENU(1))
	}

	/* Draw the screen */
	override fun renderImpl(container:GameContainer, game:StateBasedGame, g:Graphics) {
		// Background
		g.drawImage(ResourceHolder.imgMenuBG[0], 0f, 0f)

		// Menu
		FontNormal.printFontGrid(1, 1, "OPTIONS", COLOR.ORANGE)

		FontNormal.printFontGrid(1, 3+cursor, "b", COLOR.RED)

		FontNormal.printFontGrid(2, 3, "[GENERAL OPTIONS]", cursor==0)
		FontNormal.printFontGrid(2, 4, "[RULE SELECT]:"+(player+1)+"P", cursor==1)
		FontNormal.printFontGrid(2, 5, "[GAME TUNING]:"+(player+1)+"P", cursor==2)
		FontNormal.printFontGrid(2, 6, "[AI SETTING]:"+(player+1)+"P", cursor==3)
		FontNormal.printFontGrid(2, 7, "[KEYBOARD SETTING]:"+(player+1)+"P", cursor==4)
		FontNormal.printFontGrid(2, 8, "[KEYBOARD NAVIGATION SETTING]:"+(player+1)+"P", cursor==5)
		FontNormal.printFontGrid(2, 9, "[KEYBOARD RESET]:"+(player+1)+"P", cursor==6)
		FontNormal.printFontGrid(2, 10, "[JOYSTICK SETTING]:"+(player+1)+"P", cursor==7)

		FontNormal.printTTF(16, 432, NullpoMinoSlick.getUIText(UI_TEXT[cursor]))
	}

	override fun onChange(container:GameContainer, game:StateBasedGame, delta:Int, change:Int) {
		player += change
		if(player<0) player = 1
		if(player>1) player = 0
		ResourceHolder.soundManager.play("change")
	}

	override fun onDecide(container:GameContainer, game:StateBasedGame, delta:Int):Boolean {
		ResourceHolder.soundManager.play("decide1")
		when(cursor) {
			0 -> game.enterState(StateConfigGeneral.ID)
			1 -> {
				NullpoMinoSlick.stateConfigRuleStyleSelect.player = player
				game.enterState(StateConfigRuleStyleSelect.ID)
			}
			2 -> {
				NullpoMinoSlick.stateConfigGameTuning.player = player
				game.enterState(StateConfigGameTuning.ID)
			}
			3 -> {
				NullpoMinoSlick.stateConfigAISelect.player = player
				game.enterState(StateConfigAISelect.ID)
			}
			4 -> {
				NullpoMinoSlick.stateConfigKeyboard.player = player
				NullpoMinoSlick.stateConfigKeyboard.isNavSetting = false
				game.enterState(StateConfigKeyboard.ID)
			}
			5 -> {
				NullpoMinoSlick.stateConfigKeyboardNavi.player = player
				game.enterState(StateConfigKeyboardNavi.ID)
			}
			6 -> {
				NullpoMinoSlick.stateConfigKeyboardReset.player = player
				game.enterState(StateConfigKeyboardReset.ID)
			}
			7 -> {
				NullpoMinoSlick.stateConfigJoystickMain.player = player
				game.enterState(StateConfigJoystickMain.ID)
			}
		}

		return false
	}

	override fun onCancel(container:GameContainer, game:StateBasedGame, delta:Int):Boolean {
		game.enterState(StateTitle.ID)
		return false
	}

	companion object {
		/** This state's ID */
		const val ID = 5

		/** UI Text identifier Strings */
		private val UI_TEXT =
			arrayOf("ConfigMainMenu_General", "ConfigMainMenu_Rule", "ConfigMainMenu_GameTuning", "ConfigMainMenu_AI", "ConfigMainMenu_Keyboard", "ConfigMainMenu_KeyboardNavi", "ConfigMainMenu_KeyboardReset", "ConfigMainMenu_Joystick")
	}
}
