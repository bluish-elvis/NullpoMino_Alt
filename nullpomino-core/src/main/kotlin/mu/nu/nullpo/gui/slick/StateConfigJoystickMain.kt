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

import mu.nu.nullpo.game.event.EventReceiver.COLOR
import mu.nu.nullpo.gui.common.BaseFont
import mu.nu.nullpo.gui.common.GameKeyDummy
import mu.nu.nullpo.gui.common.ConfigGlobal.CtrlConf
import mu.nu.nullpo.gui.slick.img.FontNormal
import mu.nu.nullpo.gui.slick.img.FontTTF
import mu.nu.nullpo.util.GeneralUtil.getONorOFF
import org.newdawn.slick.GameContainer
import org.newdawn.slick.Graphics
import org.newdawn.slick.SlickException
import org.newdawn.slick.state.StateBasedGame

/** Joystick 設定メインMenu のステート */
internal class StateConfigJoystickMain:BaseMenuState() {
	/** Player number */
	var player = 0

	/** Cursor position */
	private var cursor = 0

	/** 使用するJoystick の number */
	private var joyUseNumber = 0

	/** Joystick direction key が反応する閾値 */
	private var joyBorder = 0

	/** アナログスティック無視 */
	private var joyIgnoreAxis = false

	/** ハットスイッチ無視 */
	private var joyIgnorePOV = false

	/** Joystick input method */
	private var joyMethod = 0

	/* Fetch this state's ID */
	override fun getID():Int = ID

	/** Load settings
	 * @param prop Property file to read from
	 */
	private fun loadConfig(prop:CtrlConf) {
		prop.joyPadConf.let {
			joyUseNumber = it.controllerID[player]
			joyBorder = it.border[player]
			joyIgnoreAxis = it.ignoreAxis[player]
			joyIgnorePOV = it.ignorePOV[player]
		}
		joyMethod = prop.joyMethod
	}

	/** Save settings
	 * @param prop Property file to save to
	 */
	private fun saveConfig(prop:CtrlConf) {
		prop.joyPadConf.apply {
			controllerID[player] = joyUseNumber
			border[player] = joyBorder
			ignoreAxis[player] = joyIgnoreAxis
			ignorePOV[player] = joyIgnorePOV
		}
		prop.joyMethod = joyMethod
	}

	/* Called when entering this state */
	override fun enter(container:GameContainer?, game:StateBasedGame?) {
		loadConfig(NullpoMinoSlick.propConfig.ctrl)
	}

	/* State initialization */
	override fun init(container:GameContainer, game:StateBasedGame) {}

	/* Draw the game screen */
	override fun renderImpl(container:GameContainer, game:StateBasedGame, g:Graphics) {
		// Menu
		g.drawImage(ResourceHolder.imgMenuBG[1], 0f, 0f)

		FontNormal.printFontGrid(1, 1, "JOYSTICK SETTING (${player+1}P)", COLOR.ORANGE)

		FontNormal.printFontGrid(1, 3+cursor, BaseFont.CURSOR, COLOR.RAINBOW)

		FontNormal.printFontGrid(2, 3, "[BUTTON SETTING]", cursor==0)
		FontNormal.printFontGrid(2, 4, "[INPUT TEST]", cursor==1)
		FontNormal.printFontGrid(
			2, 5, "JOYSTICK NUMBER:"+if(joyUseNumber==-1)
				"NOTHING"
			else
				"$joyUseNumber", cursor==2
		)
		FontNormal.printFontGrid(2, 6, "JOYSTICK BORDER:$joyBorder", cursor==3)
		FontNormal.printFontGrid(2, 7, "AXIS INPUT:"+(!joyIgnoreAxis).getONorOFF(), cursor==4)
		FontNormal.printFontGrid(2, 8, "POV INPUT:"+(!joyIgnorePOV).getONorOFF(), cursor==5)
		FontNormal.printFontGrid(2, 9, "JOYSTICK METHOD:"+JOYSTICK_METHOD_STRINGS[joyMethod], cursor==6)

		if(cursor<UI_TEXT.size) FontTTF.print(16, 432, NullpoMinoSlick.getUIText(UI_TEXT[cursor]))
	}

	/* Update game state */
	@Throws(SlickException::class)
	override fun updateImpl(container:GameContainer, game:StateBasedGame, delta:Int) {
		// TTF font load
		if(ResourceHolder.ttfFont!=null) ResourceHolder.ttfFont!!.loadGlyphs()

		// Update key input states
		GameKey.gameKey[0].update(container.input)

		// Cursor movement
		if(GameKey.gameKey[0].isMenuRepeatKey(GameKeyDummy.BUTTON_UP)) {
			cursor--
			if(cursor<0) cursor = 6
			ResourceHolder.soundManager.play("cursor")
		}
		if(GameKey.gameKey[0].isMenuRepeatKey(GameKeyDummy.BUTTON_DOWN)) {
			cursor++
			if(cursor>6) cursor = 0
			ResourceHolder.soundManager.play("cursor")
		}

		// Configuration changes
		var change = 0
		if(GameKey.gameKey[0].isMenuRepeatKey(GameKeyDummy.BUTTON_LEFT)) change = -1
		if(GameKey.gameKey[0].isMenuRepeatKey(GameKeyDummy.BUTTON_RIGHT)) change = 1

		if(change!=0) {
			ResourceHolder.soundManager.play("change")

			when(cursor) {
				2 -> {
					joyUseNumber += change
					if(joyUseNumber<-1) joyUseNumber = ControllerManager.controllerCount-1
					if(joyUseNumber>ControllerManager.controllerCount-1) joyUseNumber = -1
				}
				3 -> {
					joyBorder += change
					if(joyBorder<0) joyBorder = 32768
					if(joyBorder>32768) joyBorder = 0
				}
				4 -> joyIgnoreAxis = !joyIgnoreAxis
				5 -> joyIgnorePOV = !joyIgnorePOV
				6 -> {
					joyMethod += change
					if(joyMethod<0) joyMethod = ControllerManager.CONTROLLER_METHOD_MAX-1
					if(joyMethod>ControllerManager.CONTROLLER_METHOD_MAX-1) joyMethod = 0
				}
			}
		}

		// Confirm button
		if(GameKey.gameKey[0].isPushKey(GameKeyDummy.BUTTON_A)) {
			saveConfig(NullpoMinoSlick.propConfig.ctrl)
			NullpoMinoSlick.saveConfig()
			NullpoMinoSlick.setGeneralConfig()

			when(cursor) {
				0 -> {
					//[BUTTON SETTING]
					ResourceHolder.soundManager.play("decide1")

					NullpoMinoSlick.stateConfigJoystickButton.player = player
					game.enterState(StateConfigJoystickButton.ID)
				}
				1 -> {
					//[INPUT TEST]
					ResourceHolder.soundManager.play("decide2")

					NullpoMinoSlick.stateConfigJoystickTest.player = player
					game.enterState(StateConfigJoystickTest.ID)
				}
				else -> game.enterState(StateConfigMainMenu.ID)
			}
		}

		// Cancel button
		if(GameKey.gameKey[0].isPushKey(GameKeyDummy.BUTTON_B)) {
			loadConfig(NullpoMinoSlick.propConfig.ctrl)
			game.enterState(StateConfigMainMenu.ID)
		}
	}

	companion object {
		/** This state's ID */
		const val ID = 12

		/** Key input を受付可能になるまでの frame count */
		const val KEYACCEPTFRAME = 20

		/** Joystick method names */
		private val JOYSTICK_METHOD_STRINGS = listOf("NONE", "SLICK DEFAULT", "SLICK ALTERNATE", "LWJGL")

		/** UI Text identifier Strings */
		private val UI_TEXT =
			listOf(
				"ConfigJoystickMain_ButtonSetting", "ConfigJoystickMain_InputTest", "ConfigJoystickMain_JoyUseNumber",
				"ConfigJoystickMain_JoyBorder", "ConfigJoystickMain_JoyIgnoreAxis", "ConfigJoystickMain_JoyIgnorePOV",
				"ConfigJoystickMain_JoyMethod"
			)
	}
}
