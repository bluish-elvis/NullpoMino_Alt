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

import org.apache.logging.log4j.LogManager
import org.lwjgl.input.Controller
import org.lwjgl.input.Controllers
import org.newdawn.slick.Input

/** Joystick 関連の処理 */
object ControllerManager {
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
	var controllers:ArrayList<Controller> = ArrayList()

	/** 各Playerが使用するJoystick の number */
	var controllerID = IntArray(0)

	/** Joystick direction key が反応する閾値 (一部検出法では使えない) */
	var border = FloatArray(0)

	/** アナログスティック無視 */
	var ignoreAxis = BooleanArray(0)

	/** ハットスイッチ無視 */
	var ignorePOV = BooleanArray(0)

	/** Joystick のcountを取得
	 * @return Joystick のcount
	 */
	val controllerCount:Int
		get() = controllers.size

	/** Initialization */
	fun initControllers() {
		controllers = ArrayList()
		controllerID = IntArray(2) {-1}
		border = FloatArray(2) {0f}
		ignoreAxis = BooleanArray(2)
		ignorePOV = BooleanArray(2)
		Controllers.destroy()
		for(i in 0 until Controllers.getControllerCount()) {
			val c = Controllers.getController(i)

			if(c.buttonCount in MIN_BUTTONS until MAX_BUTTONS) controllers.add(c)
		}

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
			val controller = controllerID[player]

			if(controller<0) return false

			if(method==CONTROLLER_METHOD_SLICK_DEFAULT)
				return input.isControllerUp(controller)
			else if(method==CONTROLLER_METHOD_SLICK_ALTERNATE)
				return input.isControllerUp(controller)||!ignoreAxis[player]&&input.getAxisValue(controller, 1)<-border[player]
			else if(method==CONTROLLER_METHOD_LWJGL)
				if(controller>=0&&controller<controllers.size) {
					val axisValue = controllers[controller].yAxisValue
					val povValue = controllers[controller].povY
					return !ignoreAxis[player]&&axisValue<-border[player]||!ignorePOV[player]&&povValue<-border[player]
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
			val controller = controllerID[player]

			if(controller<0) return false

			if(method==CONTROLLER_METHOD_SLICK_DEFAULT)
				return input.isControllerDown(controller)
			else if(method==CONTROLLER_METHOD_SLICK_ALTERNATE)
				return input.isControllerDown(controller)||!ignoreAxis[player]&&input.getAxisValue(controller, 1)>border[player]
			else if(method==CONTROLLER_METHOD_LWJGL)
				if(controller>=0&&controller<controllers.size) {
					val axisValue = controllers[controller].yAxisValue
					val povValue = controllers[controller].povY
					return !ignoreAxis[player]&&axisValue>border[player]||!ignorePOV[player]&&povValue>border[player]
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
			val controller = controllerID[player]

			if(controller<0) return false

			if(method==CONTROLLER_METHOD_SLICK_DEFAULT)
				return input.isControllerLeft(controller)
			else if(method==CONTROLLER_METHOD_SLICK_ALTERNATE)
				return input.isControllerLeft(controller)||!ignoreAxis[player]&&input.getAxisValue(controller, 0)<-border[player]
			else if(method==CONTROLLER_METHOD_LWJGL)
				if(controller>=0&&controller<controllers.size) {
					val axisValue = controllers[controller].xAxisValue
					val povValue = controllers[controller].povX
					return !ignoreAxis[player]&&axisValue<-border[player]||!ignorePOV[player]&&povValue<-border[player]
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
			val controller = controllerID[player]

			if(controller<0) return false

			if(method==CONTROLLER_METHOD_SLICK_DEFAULT)
				return input.isControllerRight(controller)
			else if(method==CONTROLLER_METHOD_SLICK_ALTERNATE)
				return input.isControllerRight(controller)||!ignoreAxis[player]&&input.getAxisValue(controller, 0)>border[player]
			else if(method==CONTROLLER_METHOD_LWJGL)
				if(controller>=0&&controller<controllers.size) {
					val axisValue = controllers[controller].xAxisValue
					val povValue = controllers[controller].povX
					return !ignoreAxis[player]&&axisValue>border[player]||!ignorePOV[player]&&povValue>border[player]
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
			val controller = controllerID[player]

			if(controller<0) return false
			if(button<0) return false

			if(method==CONTROLLER_METHOD_SLICK_DEFAULT||method==CONTROLLER_METHOD_SLICK_ALTERNATE)
				return input.isButtonPressed(button, controller)
			else if(method==CONTROLLER_METHOD_LWJGL)
				if(controller>=0&&controller<controllers.size) {
					val c = controllers[controller]
					if(button<c.buttonCount) return c.isButtonPressed(button)
				}
		} catch(e:ArrayIndexOutOfBoundsException) {
			// Invalid button
		} catch(e:Throwable) {
			log.debug("Exception on isControllerButton (button:$button)", e)
		}

		return false
	}
}
