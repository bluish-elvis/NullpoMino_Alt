/*
 Copyright (c) 2010-2024, NullNoname
 All rights reserved.

 Converted to Kotlin and modified by Venom_Nhelv as bluish-elvis
 THIS IS NOT MADE IN ASSOCIATION WITH THE GAME CREATOR.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions are met:
     * Redistributions of source code must retain the above copyright
       notice, this list of conditions and the following disclaimer.
     * Redistributions in binary form must reproduce the above copyright
       notice, this list of conditions and the following disclaimer in the
       documentation and/or other materials provided with the distribution.
     * Neither the name of NullNoname nor the names of its
       contributors may be used to endorse or promote products derived from
       this software without specific prior written permission.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 POSSIBILITY OF SUCH DAMAGE.
 */
package mu.nu.nullpo.gui.slick

import mu.nu.nullpo.game.component.BGM
import mu.nu.nullpo.game.event.EventReceiver.COLOR
import mu.nu.nullpo.gui.common.BaseFont
import mu.nu.nullpo.gui.slick.img.FontNano
import mu.nu.nullpo.gui.slick.img.FontNormal
import mu.nu.nullpo.gui.slick.img.FontTTF
import org.newdawn.slick.GameContainer
import org.newdawn.slick.Graphics
import org.newdawn.slick.state.StateBasedGame

/** Options screen */
internal class StateConfigMainMenu:BaseMenuChooseState() {
	/** Player number */
	private var player = 0

	override val numChoice = CHOICES.size

	init {
		minChoiceY = 3
	}

	/* Fetch this state's ID */
	override fun getID():Int = ID

	/* State initialization */
	//override fun init(container:GameContainer, game:StateBasedGame) {}

	override fun enter(container:GameContainer?, game:StateBasedGame?) {
		super.enter(container, game)
		if(ResourceHolder.bgmPlaying!= BGM.Menu(2)) ResourceHolder.bgmStart(BGM.Menu(2))
	}

	/* Draw the screen */
	override fun renderImpl(container:GameContainer, game:StateBasedGame, g:Graphics) {
		// Background
		g.drawImage(ResourceHolder.imgMenuBG[1], 0f, 0f)

		// Menu
		FontNormal.printFontGrid(1, 1, "OPTIONS", player==0, COLOR.ORANGE, COLOR.CYAN)
		FontNano.printFontGrid(8, 1, "FOR ${player+1}P", COLOR.ORANGE)

		FontNormal.printFontGrid(1, 3+cursor, BaseFont.CURSOR, player==0, COLOR.RED, COLOR.BLUE)

		CHOICES.forEachIndexed {i, (first) ->
			FontNormal.printFontGrid(
				2, 3+i, "[$first]", cursor==i, COLOR.WHITE,
				if(player==0) COLOR.BLUE else COLOR.RED
			)
		}
		super.renderImpl(container, game, g)

		FontTTF.print(16, 432, NullpoMinoSlick.getUIText("ConfigMainMenu_${CHOICES[cursor].second}"))
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
		return true
	}

	companion object {
		/** This state's ID */
		const val ID = 5

		/** Text identifier Strings */
		private val CHOICES =
			listOf(
				"General Settings" to "General",
				"RULE Select" to "Rule", "Tweaks Handling" to "GameTuning",
				"AI Setting" to "AI",
				"In-Game Keyboard Assign" to "Keyboard",
				"Keyboard Assign" to "KeyboardNavi",
				"RESET Keyboard Assign" to "KeyboardReset",
				"Joystick SETTING" to "Joystick"
			)
	}
}
