package mu.nu.nullpo.gui.slick

import net.java.games.input.*
import org.apache.log4j.Logger
import org.newdawn.slick.Input
import java.util.*

/** JInput keyboard input manager */
object JInputManager {
	/** Logger */
	internal val log = Logger.getLogger(JInputManager::class.java)

	/** Number of keycodes found in Slick */
	const val MAX_SLICK_KEY = 224

	/** ControllerEnvironment: Main object of JInput */
	var controllerEnvironment:ControllerEnvironment = ControllerEnvironment.getDefaultEnvironment()

	/** All available controllers */
	var controllers:Array<Controller> = emptyArray()

	/** JInput keyboard */
	var keyboard:Keyboard? = null

	/** Keysym map */
	var keyMap:HashMap<Int, Component.Identifier.Key> = HashMap()

	/** Init keysym mappings */
	fun initKeymap() {
		keyMap.clear()
		keyMap[0] = Component.Identifier.Key.VOID // Most likely zero
		keyMap[Input.KEY_ESCAPE] = Component.Identifier.Key.ESCAPE
		keyMap[Input.KEY_1] = Component.Identifier.Key._1
		keyMap[Input.KEY_2] = Component.Identifier.Key._2
		keyMap[Input.KEY_3] = Component.Identifier.Key._3
		keyMap[Input.KEY_4] = Component.Identifier.Key._4
		keyMap[Input.KEY_5] = Component.Identifier.Key._5
		keyMap[Input.KEY_6] = Component.Identifier.Key._6
		keyMap[Input.KEY_7] = Component.Identifier.Key._7
		keyMap[Input.KEY_8] = Component.Identifier.Key._8
		keyMap[Input.KEY_9] = Component.Identifier.Key._9
		keyMap[Input.KEY_0] = Component.Identifier.Key._0
		keyMap[Input.KEY_MINUS] = Component.Identifier.Key.MINUS
		keyMap[Input.KEY_EQUALS] = Component.Identifier.Key.EQUALS
		keyMap[Input.KEY_BACK] = Component.Identifier.Key.BACK
		keyMap[Input.KEY_TAB] = Component.Identifier.Key.TAB
		keyMap[Input.KEY_Q] = Component.Identifier.Key.Q
		keyMap[Input.KEY_W] = Component.Identifier.Key.W
		keyMap[Input.KEY_E] = Component.Identifier.Key.E
		keyMap[Input.KEY_R] = Component.Identifier.Key.R
		keyMap[Input.KEY_T] = Component.Identifier.Key.T
		keyMap[Input.KEY_Y] = Component.Identifier.Key.Y
		keyMap[Input.KEY_U] = Component.Identifier.Key.U
		keyMap[Input.KEY_I] = Component.Identifier.Key.I
		keyMap[Input.KEY_O] = Component.Identifier.Key.O
		keyMap[Input.KEY_P] = Component.Identifier.Key.P
		keyMap[Input.KEY_LBRACKET] = Component.Identifier.Key.LBRACKET
		keyMap[Input.KEY_RBRACKET] = Component.Identifier.Key.RBRACKET
		keyMap[Input.KEY_RETURN] = Component.Identifier.Key.RETURN
		keyMap[Input.KEY_LCONTROL] = Component.Identifier.Key.LCONTROL
		keyMap[Input.KEY_A] = Component.Identifier.Key.A
		keyMap[Input.KEY_S] = Component.Identifier.Key.S
		keyMap[Input.KEY_D] = Component.Identifier.Key.D
		keyMap[Input.KEY_F] = Component.Identifier.Key.F
		keyMap[Input.KEY_G] = Component.Identifier.Key.G
		keyMap[Input.KEY_H] = Component.Identifier.Key.H
		keyMap[Input.KEY_J] = Component.Identifier.Key.J
		keyMap[Input.KEY_K] = Component.Identifier.Key.K
		keyMap[Input.KEY_L] = Component.Identifier.Key.L
		keyMap[Input.KEY_SEMICOLON] = Component.Identifier.Key.SEMICOLON
		keyMap[Input.KEY_APOSTROPHE] = Component.Identifier.Key.APOSTROPHE
		keyMap[Input.KEY_GRAVE] = Component.Identifier.Key.GRAVE
		keyMap[Input.KEY_LSHIFT] = Component.Identifier.Key.LSHIFT
		keyMap[Input.KEY_BACKSLASH] = Component.Identifier.Key.BACKSLASH
		keyMap[Input.KEY_Z] = Component.Identifier.Key.Z
		keyMap[Input.KEY_X] = Component.Identifier.Key.X
		keyMap[Input.KEY_C] = Component.Identifier.Key.C
		keyMap[Input.KEY_V] = Component.Identifier.Key.V
		keyMap[Input.KEY_B] = Component.Identifier.Key.B
		keyMap[Input.KEY_N] = Component.Identifier.Key.N
		keyMap[Input.KEY_M] = Component.Identifier.Key.M
		keyMap[Input.KEY_COMMA] = Component.Identifier.Key.COMMA
		keyMap[Input.KEY_PERIOD] = Component.Identifier.Key.PERIOD
		keyMap[Input.KEY_SLASH] = Component.Identifier.Key.SLASH
		keyMap[Input.KEY_RSHIFT] = Component.Identifier.Key.RSHIFT
		keyMap[Input.KEY_MULTIPLY] = Component.Identifier.Key.MULTIPLY
		keyMap[Input.KEY_LALT] = Component.Identifier.Key.LALT
		keyMap[Input.KEY_SPACE] = Component.Identifier.Key.SPACE
		keyMap[Input.KEY_CAPITAL] = Component.Identifier.Key.CAPITAL
		keyMap[Input.KEY_F1] = Component.Identifier.Key.F1
		keyMap[Input.KEY_F2] = Component.Identifier.Key.F2
		keyMap[Input.KEY_F3] = Component.Identifier.Key.F3
		keyMap[Input.KEY_F4] = Component.Identifier.Key.F4
		keyMap[Input.KEY_F5] = Component.Identifier.Key.F5
		keyMap[Input.KEY_F6] = Component.Identifier.Key.F6
		keyMap[Input.KEY_F7] = Component.Identifier.Key.F7
		keyMap[Input.KEY_F8] = Component.Identifier.Key.F8
		keyMap[Input.KEY_F9] = Component.Identifier.Key.F9
		keyMap[Input.KEY_F10] = Component.Identifier.Key.F10
		keyMap[Input.KEY_NUMLOCK] = Component.Identifier.Key.NUMLOCK
		keyMap[Input.KEY_SCROLL] = Component.Identifier.Key.SCROLL
		keyMap[Input.KEY_NUMPAD7] = Component.Identifier.Key.NUMPAD7
		keyMap[Input.KEY_NUMPAD8] = Component.Identifier.Key.NUMPAD8
		keyMap[Input.KEY_NUMPAD9] = Component.Identifier.Key.NUMPAD9
		keyMap[Input.KEY_SUBTRACT] = Component.Identifier.Key.SUBTRACT
		keyMap[Input.KEY_NUMPAD4] = Component.Identifier.Key.NUMPAD4
		keyMap[Input.KEY_NUMPAD5] = Component.Identifier.Key.NUMPAD5
		keyMap[Input.KEY_NUMPAD6] = Component.Identifier.Key.NUMPAD6
		keyMap[Input.KEY_ADD] = Component.Identifier.Key.ADD
		keyMap[Input.KEY_NUMPAD1] = Component.Identifier.Key.NUMPAD1
		keyMap[Input.KEY_NUMPAD2] = Component.Identifier.Key.NUMPAD2
		keyMap[Input.KEY_NUMPAD3] = Component.Identifier.Key.NUMPAD3
		keyMap[Input.KEY_NUMPAD0] = Component.Identifier.Key.NUMPAD0
		keyMap[Input.KEY_F11] = Component.Identifier.Key.F11
		keyMap[Input.KEY_F12] = Component.Identifier.Key.F12
		keyMap[Input.KEY_F13] = Component.Identifier.Key.F13
		keyMap[Input.KEY_F14] = Component.Identifier.Key.F14
		keyMap[Input.KEY_F15] = Component.Identifier.Key.F15
		keyMap[Input.KEY_KANA] = Component.Identifier.Key.KANA
		keyMap[Input.KEY_CONVERT] = Component.Identifier.Key.CONVERT
		keyMap[Input.KEY_NOCONVERT] = Component.Identifier.Key.NOCONVERT
		keyMap[Input.KEY_YEN] = Component.Identifier.Key.YEN
		keyMap[Input.KEY_NUMPADEQUALS] = Component.Identifier.Key.NUMPADEQUAL // Different name
		keyMap[Input.KEY_CIRCUMFLEX] = Component.Identifier.Key.CIRCUMFLEX
		keyMap[Input.KEY_AT] = Component.Identifier.Key.AT
		keyMap[Input.KEY_COLON] = Component.Identifier.Key.COLON
		keyMap[Input.KEY_UNDERLINE] = Component.Identifier.Key.UNDERLINE
		keyMap[Input.KEY_KANJI] = Component.Identifier.Key.KANJI
		keyMap[Input.KEY_STOP] = Component.Identifier.Key.STOP
		keyMap[Input.KEY_AX] = Component.Identifier.Key.AX
		keyMap[Input.KEY_UNLABELED] = Component.Identifier.Key.UNLABELED
		keyMap[Input.KEY_NUMPADENTER] = Component.Identifier.Key.NUMPADENTER
		keyMap[Input.KEY_RCONTROL] = Component.Identifier.Key.RCONTROL
		keyMap[Input.KEY_NUMPADCOMMA] = Component.Identifier.Key.NUMPADCOMMA
		keyMap[Input.KEY_DIVIDE] = Component.Identifier.Key.DIVIDE
		keyMap[Input.KEY_SYSRQ] = Component.Identifier.Key.SYSRQ
		keyMap[Input.KEY_RALT] = Component.Identifier.Key.RALT
		keyMap[Input.KEY_PAUSE] = Component.Identifier.Key.PAUSE
		keyMap[Input.KEY_HOME] = Component.Identifier.Key.HOME
		keyMap[Input.KEY_UP] = Component.Identifier.Key.UP
		keyMap[Input.KEY_PRIOR] = Component.Identifier.Key.PAGEUP // Different name
		keyMap[Input.KEY_LEFT] = Component.Identifier.Key.LEFT
		keyMap[Input.KEY_RIGHT] = Component.Identifier.Key.RIGHT
		keyMap[Input.KEY_END] = Component.Identifier.Key.END
		keyMap[Input.KEY_DOWN] = Component.Identifier.Key.DOWN
		keyMap[Input.KEY_NEXT] = Component.Identifier.Key.PAGEDOWN // Different name
		keyMap[Input.KEY_INSERT] = Component.Identifier.Key.INSERT
		keyMap[Input.KEY_DELETE] = Component.Identifier.Key.DELETE
		keyMap[Input.KEY_LWIN] = Component.Identifier.Key.LWIN
		keyMap[Input.KEY_RWIN] = Component.Identifier.Key.RWIN
		keyMap[Input.KEY_APPS] = Component.Identifier.Key.APPS
		keyMap[Input.KEY_POWER] = Component.Identifier.Key.POWER
		keyMap[Input.KEY_SLEEP] = Component.Identifier.Key.SLEEP
		//keyMap.put(0, Component.Identifier.Key.KEY_UNKNOWN);	// Most likely zero
	}

	/** Convert Slick's keycode to JInput's key identifier.
	 * @param key Slick's keycode
	 * @return JInput's key identifier
	 */
	fun slickToJInputKey(key:Int):Component.Identifier.Key = keyMap[key]?:Component.Identifier.Key.VOID

	/** Init */
	fun initKeyboard() {
		controllerEnvironment = ControllerEnvironment.getDefaultEnvironment()
		controllers = controllerEnvironment.controllers

		for(c in controllers) {
			if(c.type===Controller.Type.KEYBOARD&&c is Keyboard) {
				if(NullpoMinoSlick.useJInputKeyboard) log.debug("initKeyboard: Keyboard found")
				keyboard = c
			}
		}

		if(keyboard==null&&NullpoMinoSlick.useJInputKeyboard) {
			log.error("initKeyboard: Keyboard NOT FOUND")

			// Linux
			if(System.getProperty("os.name").startsWith("Linux")) {
				log.error("If you can use sudo, try the following command and start NullpoMino again:")
				log.error("sudo chmod go+r /dev/input/*")
			}
		}
	}

	/** Polls keyboard input. */
	fun poll() {
		if(keyboard!=null) keyboard!!.poll()
	}

	/** Check if specific key is down or not.
	 * @param key SLICK keycode
	 * @return true if the key is down
	 */
	fun isKeyDown(key:Int):Boolean {
		if(keyboard!=null) {
			val jinputKey = keyMap[key]
			return keyboard!!.isKeyDown(jinputKey)
		}
		return false
	}
}
