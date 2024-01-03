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

import mu.nu.nullpo.gui.common.ConfigGlobal.GamePadConf
import mu.nu.nullpo.gui.common.GameKeyDummy.Companion.MAX_PLAYERS
import org.apache.logging.log4j.LogManager
import org.lwjgl.input.Controller
import org.lwjgl.input.Controllers
import org.newdawn.slick.Input

/** Joystick 関連の処理 */
internal object ControllerManager {
	/** Log */
	internal val log = LogManager.getLogger()

	/** 最小/Maximum buttoncount */
	const val MIN_BUTTONS = 3
	const val MAX_BUTTONS = 100

	/** Joystick 状態検出法の定数 */
	const val CONTROLLER_METHOD_NONE = 0
	const val CONTROLLER_METHOD_SLICK_DEFAULT = 1
	const val CONTROLLER_METHOD_SLICK_ALTERNATE = 2
	const val CONTROLLER_METHOD_LWJGL = 3
	const val CONTROLLER_METHOD_MAX = 4

	/** Joystick 状態検出法 */
	var method = CONTROLLER_METHOD_SLICK_DEFAULT

	/** Joystick state */
	var controllers = emptyList<Controller>()
	var config = GamePadConf(
		mutableListOf(-1), mutableListOf(), mutableListOf(),mutableListOf())
	/** 各Playerが使用するJoystick の number */
	val controllerID get() = config.controllerID

	/** Joystick direction key が反応する閾値 (一部検出法では使えない) */
	val border get() = config.border.map {it/32768f}

	/** アナログスティック無視 */
	val ignoreAxis get() = config.ignoreAxis

	/** ハットスイッチ無視 */
	val ignorePOV get() = config.ignorePOV

	/** Joystick のcountを取得
	 * @return Joystick のcount
	 */
	val controllerCount:Int
		get() = controllers.size

	/** Initialization */
	fun initControllers() {
		Controllers.destroy()
		config.controllerID = MutableList(MAX_PLAYERS) {-1}
		config.border = MutableList(MAX_PLAYERS) {0}
		config.ignoreAxis = MutableList(MAX_PLAYERS) {false}
		config.ignorePOV = MutableList(MAX_PLAYERS) {false}
		controllers = List(Controllers.getControllerCount()) {i ->
			Controllers.getController(i).takeIf {it.buttonCount in MIN_BUTTONS..<MAX_BUTTONS}
		}.filterNotNull()

		log.info("Found ${controllers.size} controllers from NullpoMinoSlick app")

		controllers.forEachIndexed {i, c ->
			log.debug("ID:$i, AxisCount:${c.axisCount}, ButtonCount:"+c.buttonCount)
		}
	}

	/** Joystick の上を押しているとtrue
	 * @param player Player number
	 * @param input Inputクラス (container.getInput()で取得可能）
	 * @return 上を押しているとtrue
	 */
	fun isControllerUp(player:Int, input:Input):Boolean {
		try {
			val controller = controllerID.getOrElse(player){-1}

			return when {
				controller<0 -> false
				method==CONTROLLER_METHOD_SLICK_DEFAULT -> input.isControllerUp(controller)
				method==CONTROLLER_METHOD_SLICK_ALTERNATE -> input.isControllerUp(controller)||
					!ignoreAxis[player]&&input.getAxisValue(controller, 1)<-border[player]
				method==CONTROLLER_METHOD_LWJGL&&controller<controllers.size -> !ignoreAxis[player]&&controllers[controller].yAxisValue<-border[player]||
					!ignorePOV[player]&&controllers[controller].povY<-border[player]
				else -> false
			}
		} catch(e:Throwable) {
			log.debug("Exception on isControllerUp", e)
		}

		return false
	}

	/** Joystick の下を押しているとtrue
	 * @param player Player number
	 * @param input Inputクラス (container.getInput()で取得可能）
	 * @return 下を押しているとtrue
	 */
	fun isControllerDown(player:Int, input:Input):Boolean {
		try {
			val controller = controllerID.getOrElse(player){-1}

			return when {
				controller<0 -> false
				method==CONTROLLER_METHOD_SLICK_DEFAULT -> input.isControllerDown(controller)
				method==CONTROLLER_METHOD_SLICK_ALTERNATE -> input.isControllerDown(controller)||
					!ignoreAxis[player]&&input.getAxisValue(controller, 1)>border[player]
				method==CONTROLLER_METHOD_LWJGL&&controller<controllers.size ->
					!ignoreAxis[player]&&controllers[controller].yAxisValue>border[player]||
						!ignorePOV[player]&&controllers[controller].povY>border[player]
				else -> false
			}
		} catch(e:Throwable) {
			log.debug("Exception on isControllerDown", e)
		}

		return false
	}

	/** Joystick の左を押しているとtrue
	 * @param player Player number
	 * @param input Inputクラス (container.getInput()で取得可能）
	 * @return 左を押しているとtrue
	 */
	fun isControllerLeft(player:Int, input:Input):Boolean {
		try {
			val controller = controllerID.getOrElse(player){-1}
			return when {
				controller<0 -> false
				method==CONTROLLER_METHOD_SLICK_DEFAULT ->
					input.isControllerLeft(controller)
				method==CONTROLLER_METHOD_SLICK_ALTERNATE ->
					input.isControllerLeft(controller)||!ignoreAxis[player]&&input.getAxisValue(controller, 0)<-border[player]
				method==CONTROLLER_METHOD_LWJGL&&controller<controllers.size ->
					!ignoreAxis[player]&&controllers[controller].xAxisValue<-border[player]||
						!ignorePOV[player]&&controllers[controller].povX<-border[player]
				else -> false
			}
		} catch(e:Throwable) {
			log.debug("Exception on isControllerLeft", e)
		}

		return false
	}

	/** Joystick の右を押しているとtrue
	 * @param player Player number
	 * @param input Inputクラス (container.getInput()で取得可能）
	 * @return 右を押しているとtrue
	 */
	fun isControllerRight(player:Int, input:Input):Boolean {
		try {
			val controller = controllerID.getOrElse(player){-1}

			return when {
				controller<0 -> false
				method==CONTROLLER_METHOD_SLICK_DEFAULT -> input.isControllerRight(controller)
				method==CONTROLLER_METHOD_SLICK_ALTERNATE ->
					input.isControllerRight(controller)||!ignoreAxis[player]&&input.getAxisValue(controller, 0)>border[player]
				method==CONTROLLER_METHOD_LWJGL&&controller<controllers.size ->
					!ignoreAxis[player]&&controllers[controller].xAxisValue>border[player]||
						!ignorePOV[player]&&controllers[controller].povX>border[player]
				else -> false
			}
		} catch(e:Throwable) {
			log.debug("Exception on isControllerRight", e)
		}

		return false
	}

	/** Joystick の特定の buttonが押されているならtrue
	 * @param player Player number
	 * @param input Inputクラス (container.getInput()で取得可能）
	 * @param button Button number
	 * @return 指定した buttonが押されているとtrue
	 */
	fun isControllerButton(player:Int, input:Input, button:Int):Boolean {
		try {
			val controller = controllerID.getOrElse(player){-1}
			return when {
				player>=controllerID.size||controller<0||button<0 -> false
				method==CONTROLLER_METHOD_SLICK_DEFAULT||method==CONTROLLER_METHOD_SLICK_ALTERNATE ->
					input.isButtonPressed(button, controller)
				method==CONTROLLER_METHOD_LWJGL&&controller<controllers.size ->
					controllers[controller].let {c -> button<c.buttonCount&&c.isButtonPressed(button)}
				else -> false
			}
		} catch(e:ArrayIndexOutOfBoundsException) {
			// Invalid button
		} catch(e:Throwable) {
			log.debug("Exception on isControllerButton (button:$button)", e)
		}

		return false
	}
}
