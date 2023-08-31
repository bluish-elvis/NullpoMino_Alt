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

import mu.nu.nullpo.game.event.EventReceiver.COLOR
import mu.nu.nullpo.gui.common.BaseFont
import mu.nu.nullpo.gui.slick.img.FontNormal
import org.newdawn.slick.GameContainer
import org.newdawn.slick.Graphics
import org.newdawn.slick.state.StateBasedGame

/** Keyboard Reset menu */
internal class StateConfigKeyboardReset:BaseMenuChooseState() {
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
	override fun init(container:GameContainer, game:StateBasedGame) {}

	/* Draw the screen */
	override fun renderImpl(container:GameContainer, game:StateBasedGame, g:Graphics) {
		// Background
		g.drawImage(ResourceHolder.imgMenuBG[1], 0f, 0f)

		// Menu
		FontNormal.printFontGrid(1, 1, "KEYBOARD RESET (${player+1}P)", COLOR.ORANGE)

		FontNormal.printFontGrid(2, 3, "RESET SETTINGS TO...", COLOR.GREEN)

		FontNormal.printFontGrid(1, 4+cursor, BaseFont.CURSOR, COLOR.RAINBOW)

		FontNormal.printFontGrid(2, 4, "BLOCKBOX STYLE (DEFAULT)", cursor==0)
		FontNormal.printFontGrid(2, 5, "GUIDELINE STYLE", cursor==1)
		FontNormal.printFontGrid(2, 6, "NULLPOMINO CLASSIC STYLE", cursor==2)
	}

	/* Decide */
	override fun onDecide(container:GameContainer, game:StateBasedGame, delta:Int):Boolean {
		ResourceHolder.soundManager.play("decide1")
		GameKey.gameKey[player].loadDefaultKeymap(cursor)
		GameKey.gameKey[player].saveConfig(NullpoMinoSlick.propConfig.ctrl.keymaps[player])
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
