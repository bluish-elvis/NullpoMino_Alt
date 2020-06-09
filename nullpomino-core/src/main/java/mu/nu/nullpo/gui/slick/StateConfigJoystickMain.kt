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
import mu.nu.nullpo.util.CustomProperties
import mu.nu.nullpo.util.GeneralUtil
import org.newdawn.slick.*
import org.newdawn.slick.state.StateBasedGame

/** Joystick 設定メインMenu のステート */
class StateConfigJoystickMain:BaseGameState() {

	/** Player number */
	var player = 0

	/** Cursor position */
	private var cursor = 0

	/** 使用するJoystick の number */
	private var joyUseNumber:Int = 0

	/** Joystick direction key が反応する閾値 */
	private var joyBorder:Int = 0

	/** アナログスティック無視 */
	private var joyIgnoreAxis:Boolean = false

	/** ハットスイッチ無視 */
	private var joyIgnorePOV:Boolean = false

	/** Joystick input method */
	private var joyMethod:Int = 0

	/* Fetch this state's ID */
	override fun getID():Int = ID

	/** Load settings
	 * @param prop Property file to read from
	 */
	private fun loadConfig(prop:CustomProperties) {
		joyUseNumber = prop.getProperty("joyUseNumber.p$player", -1)
		joyBorder = prop.getProperty("joyBorder.p$player", 0)
		joyIgnoreAxis = prop.getProperty("joyIgnoreAxis.p$player", false)
		joyIgnorePOV = prop.getProperty("joyIgnorePOV.p$player", false)
		joyMethod = prop.getProperty("option.joymethod", ControllerManager.CONTROLLER_METHOD_SLICK_DEFAULT)
	}

	/** Save settings
	 * @param prop Property file to save to
	 */
	private fun saveConfig(prop:CustomProperties) {
		prop.setProperty("joyUseNumber.p$player", joyUseNumber)
		prop.setProperty("joyBorder.p$player", joyBorder)
		prop.setProperty("joyIgnoreAxis.p$player", joyIgnoreAxis)
		prop.setProperty("joyIgnorePOV.p$player", joyIgnorePOV)
		prop.setProperty("option.joymethod", joyMethod)
	}

	/* Called when entering this state */
	override fun enter(container:GameContainer?, game:StateBasedGame?) {
		loadConfig(NullpoMinoSlick.propConfig)
	}

	/* State initialization */
	override fun init(container:GameContainer, game:StateBasedGame) {}

	/* Draw the game screen */
	override fun renderImpl(container:GameContainer, game:StateBasedGame, g:Graphics) {
		// Menu
		g.drawImage(ResourceHolder.imgMenuBG[0], 0f, 0f)

		FontNormal.printFontGrid(1, 1, "JOYSTICK SETTING (${player+1}P)", COLOR.ORANGE)

		FontNormal.printFontGrid(1, 3+cursor, "\u0082", COLOR.RAINBOW)

		FontNormal.printFontGrid(2, 3, "[BUTTON SETTING]", cursor==0)
		FontNormal.printFontGrid(2, 4, "[INPUT TEST]", cursor==1)
		FontNormal.printFontGrid(2, 5, "JOYSTICK NUMBER:"+if(joyUseNumber==-1)
			"NOTHING"
		else
			"$joyUseNumber", cursor==2)
		FontNormal.printFontGrid(2, 6, "JOYSTICK BORDER:$joyBorder", cursor==3)
		FontNormal.printFontGrid(2, 7, "AXIS INPUT:"+GeneralUtil.getONorOFF(!joyIgnoreAxis), cursor==4)
		FontNormal.printFontGrid(2, 8, "POV INPUT:"+GeneralUtil.getONorOFF(!joyIgnorePOV), cursor==5)
		FontNormal.printFontGrid(2, 9, "JOYSTICK METHOD:"+JOYSTICK_METHOD_STRINGS[joyMethod], cursor==6)

		if(cursor<UI_TEXT.size) FontNormal.printTTF(16, 432, NullpoMinoSlick.getUIText(UI_TEXT[cursor]))
	}

	/* Update game state */
	@Throws(SlickException::class)
	override fun updateImpl(container:GameContainer, game:StateBasedGame, delta:Int) {
		// TTF font load
		if(ResourceHolder.ttfFont!=null) ResourceHolder.ttfFont!!.loadGlyphs()

		// Update key input states
		GameKey.gamekey[0].update(container.input)

		// Cursor movement
		if(GameKey.gamekey[0].isMenuRepeatKey(GameKeyDummy.BUTTON_UP)) {
			cursor--
			if(cursor<0) cursor = 6
			ResourceHolder.soundManager.play("cursor")
		}
		if(GameKey.gamekey[0].isMenuRepeatKey(GameKeyDummy.BUTTON_DOWN)) {
			cursor++
			if(cursor>6) cursor = 0
			ResourceHolder.soundManager.play("cursor")
		}

		// Configuration changes
		var change = 0
		if(GameKey.gamekey[0].isMenuRepeatKey(GameKeyDummy.BUTTON_LEFT)) change = -1
		if(GameKey.gamekey[0].isMenuRepeatKey(GameKeyDummy.BUTTON_RIGHT)) change = 1

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
		if(GameKey.gamekey[0].isPushKey(GameKeyDummy.BUTTON_A)) {
			saveConfig(NullpoMinoSlick.propConfig)
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
		if(GameKey.gamekey[0].isPushKey(GameKeyDummy.BUTTON_B)) {
			loadConfig(NullpoMinoSlick.propConfig)
			game.enterState(StateConfigMainMenu.ID)
		}
	}

	companion object {
		/** This state's ID */
		const val ID = 12

		/** Key input を受付可能になるまでの frame count */
		const val KEYACCEPTFRAME = 20

		/** Joystick method names */
		private val JOYSTICK_METHOD_STRINGS = arrayOf("NONE", "SLICK DEFAULT", "SLICK ALTERNATE", "LWJGL")

		/** UI Text identifier Strings */
		private val UI_TEXT =
			arrayOf("ConfigJoystickMain_ButtonSetting", "ConfigJoystickMain_InputTest", "ConfigJoystickMain_JoyUseNumber", "ConfigJoystickMain_JoyBorder", "ConfigJoystickMain_JoyIgnoreAxis", "ConfigJoystickMain_JoyIgnorePOV", "ConfigJoystickMain_JoyMethod")
	}
}
