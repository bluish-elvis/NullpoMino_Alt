/*
 Copyright (c) 2023,
 This library class was created by 0xFC963F18DC21 / Shots243
 It is part of an extension library for the game NullpoMino (copyright 2010-2023)

 Kotlin converted and modified by Venom=Nhelv

 Herewith shall the term "Library Creator" be given to 0xFC963F18DC21.
 Herewith shall the term "Game Creator" be given to the original creator of NullpoMino, NullNoname.

 THIS LIBRARY AND MODE PACK WAS NOT MADE IN ASSOCIATION WITH THE GAME CREATOR.

 Original Repository: https://github.com/Shots243/ModePile

 When using this library in a mode / library pack of your own, the following
 conditions must be satisfied:
     - This license must remain visible at the top of the document, unmodified.
     - You are allowed to use this library for any modding purpose.
         - If this is the case, the Library Creator must be credited somewhere.
             - Source comments only are fine, but in a README is recommended.
     - Modification of this library is allowed, but only in the condition that a
       pull request is made to merge the changes to the repository.

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
package zeroxfc.nullpo.custom.libs

import mu.nu.nullpo.game.component.Controller
import mu.nu.nullpo.game.event.EventReceiver
import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.gui.common.BaseFont
import mu.nu.nullpo.gui.common.BaseFont.Companion.NAME_END
import mu.nu.nullpo.gui.common.BaseFont.Companion.NAME_REV
import mu.nu.nullpo.util.CustomProperties
import org.apache.logging.log4j.LogManager
import zeroxfc.nullpo.custom.libs.ProfileProperties.LoginScreen.Companion.State.Init
import zeroxfc.nullpo.custom.libs.ProfileProperties.LoginScreen.Companion.State.Name
import zeroxfc.nullpo.custom.libs.ProfileProperties.LoginScreen.Companion.State.Pin
import zeroxfc.nullpo.custom.libs.ProfileProperties.LoginScreen.Companion.State.Result
import java.io.BufferedWriter
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStreamWriter
import java.io.Writer
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import java.util.Locale

/**
 * Create a new profile loader. Use this constructor in a mode.
 *
 * @param colorHeading Color of heading. Use values from [EventReceiver] class.
 */
class ProfileProperties @JvmOverloads constructor(colorHeading:EventReceiver.COLOR = EventReceiver.COLOR.CYAN) {
	/** Login screen */
	val loginScreen = LoginScreen(this, colorHeading)
	/** Profile cfg file */
	val propProfile = CustomProperties("config/setting/profile.cfg")
	/** Get profile name.
	 * @return Profile name
	 */
	/** Username */
	var nameDisplay = ""
		private set

	private var nameProp = ""
	/** Is it logged in */
	var isLoggedIn = false
		private set

	init {
		try {
			propProfile.load()
			log.info("Profile file \"config/setting/profile.cfg\" loaded and ready.")
		} catch(e:IOException) {
			if(e is FileNotFoundException) {
				log.error("Profile file \"config/setting/profile.cfg\" not found. Creating new.\n", e)
				val fileWriter:Writer
				val outputWriter:BufferedWriter
				try {
					fileWriter = OutputStreamWriter(FileOutputStream("config/setting/profile.cfg"), StandardCharsets.UTF_8)
					outputWriter = BufferedWriter(fileWriter)
					outputWriter.write('\u0000'.code)
					outputWriter.close()
					fileWriter.close()
					log.info("Blank profile file \"config/setting/profile.cfg\" created.\n", e)
				} catch(e2:Exception) {
					log.error("Profile file creation failed.\n", e2)
				}
			} else {
				log.error("Profile file \"config/setting/profile.cfg\" is not loadable.\n", e)
			}
		}
	}
	/** Gets the internal property version of a name.
	 * @param name Raw name
	 * @return Property name
	 */
	private fun getStorageName(name:String):String {
		return name.uppercase(Locale.getDefault()).replace(' ', '_')
			.replace('.', 'd')
			.replace('?', 'k')
			.replace('!', 'e')
	}
	/**
	 * Tests to see if a name has already been taken on the local machine.
	 *
	 * @param name Name to test
	 * @return Available?
	 */
	private fun testUsernameTaken(name:String, number:Long):Boolean {
		val nCap = getStorageName(name)
		return propProfile.getProperty("$PREFIX_NAME$nCap.$number", false)
	}
	/**
	 * Tests to see if a name has not already been taken on the local machine.
	 *
	 * @param name Name to test
	 * @return Available?
	 */
	private fun testUsernameAvailability(name:String):Boolean {
		val nCap = getStorageName(name)
		return !propProfile.getProperty("$PREFIX_NAME$nCap.0", false)
	}
	/**
	 * Tests to see if a password conflict arises.
	 *
	 * @param name Name to test
	 * @return Available?
	 */
	private fun testPasswordCrash(name:String, buttonPresses:IntArray):Boolean {
		val nCap = getStorageName(name)
		var crash = false
		var number = 0L
		while(testUsernameTaken(name, number)) {
			if(!propProfile.getProperty("$PREFIX_NAME$nCap.$number", false)) return false
			crash = true
			val pass = propProfile.getProperty("$PREFIX_PASS$nCap.$number", 0)
			for(i in buttonPresses.indices) {
				val j = 4*(buttonPresses.size-i-1)
				if(buttonPresses[i] shl j and pass==0) {
					crash = false
					break
				}
			}
			number++
		}
		return crash
	}
	/**
	 * Attempt to log into an account with a name and password.
	 *
	 * @param name          Account name
	 * @param buttonPresses Password input sequence (exp. length 6)
	 * @return Was the attempt to log into the account successful?
	 */
	private fun attemptLogIn(name:String, buttonPresses:IntArray):Boolean {
		val nCap = getStorageName(name)
		val nCapDisplay = name.uppercase(Locale.getDefault())
		if(testUsernameAvailability(nCap)) {
			log.warn("Login to $nCapDisplay failed. Account with that name does not exist.")
			return false // If username does not exist, fail login.
		}
		var login = false
		var number = 0L
		while(testUsernameTaken(nCap, number)) {
			val pass = propProfile.getProperty("$PREFIX_PASS$nCap.$number", 0)
			for(i in buttonPresses.indices) {
				login = true
				val j = 4*(buttonPresses.size-i-1)
				if(buttonPresses[i] shl j and pass==0) {
					log.warn("Login to $nCapDisplay $number failed. Password mismatch.")
					login = false
					break
				}
			}
			if(login) {
				break
			}
			number++
		}
		nameDisplay = nCapDisplay
		nameProp = "$nCap.$number"
		isLoggedIn = true
		log.info("Login to $nCapDisplay $number successful!")
		return true
	}
	/**
	 * Attempt to create an account with a name and password.
	 *
	 * @param name          Account name
	 * @param buttonPresses Password input sequence (exp. length 6)
	 * @return Was the attempt to create an account successful?
	 */
	private fun createAccount(name:String, buttonPresses:IntArray):Boolean {
		val nCap = getStorageName(name)
		val nCapDisplay = name.uppercase()
		var number = 0L
		while(testUsernameTaken(nCap, number)) {
			log.warn("Creation of $nCapDisplay $number failed. Name and number taken.")
			number++
		}
		nameDisplay = nCapDisplay
		nameProp = "$nCap.$number"
		var password = SecureRandom().nextInt(128)
		password = password shl 4
		for(buttonPress in buttonPresses) {
			password = password shl 4
			password = password or buttonPress
		}
		propProfile.setProperty(PREFIX_NAME+nameProp, true)
		propProfile.setProperty(PREFIX_PASS+nameProp, password)
		isLoggedIn = true
		log.info("Account $nameDisplay $number created!")
		saveProfileConfig()
		return true
	}
	//region getProperty() methods
	/**
	 * Gets a `byte` property.
	 *
	 * @param path Property path
	 * @param def  Default value
	 * @return Value of property. Returns `def` if undefined or not logged in.
	 */
	fun getProperty(path:String, def:Byte):Byte = if(isLoggedIn) propProfile.getProperty("$nameProp.$path", def) else def
	/**
	 * Gets a `short` property.
	 *
	 * @param path Property path
	 * @param def  Default value
	 * @return Value of property. Returns `def` if undefined or not logged in.
	 */
	fun getProperty(path:String, def:Short):Short = if(isLoggedIn) propProfile.getProperty("$nameProp.$path", def) else def
	/**
	 * Gets an `int` property.
	 *
	 * @param path Property path
	 * @param def  Default value
	 * @return Value of property. Returns `def` if undefined or not logged in.
	 */
	fun getProperty(path:String, def:Int):Int = if(isLoggedIn) propProfile.getProperty("$nameProp.$path", def) else def
	/**
	 * Gets a `long` property.
	 *
	 * @param path Property path
	 * @param def  Default value
	 * @return Value of property. Returns `def` if undefined or not logged in.
	 */
	fun getProperty(path:String, def:Long):Long = if(isLoggedIn) propProfile.getProperty("$nameProp.$path", def) else def
	/**
	 * Gets a `float` property.
	 *
	 * @param path Property path
	 * @param def  Default value
	 * @return Value of property. Returns `def` if undefined or not logged in.
	 */
	fun getProperty(path:String, def:Float):Float = if(isLoggedIn) propProfile.getProperty("$nameProp.$path", def) else def
	/**
	 * Gets a `double` property.
	 *
	 * @param path Property path
	 * @param def  Default value
	 * @return Value of property. Returns `def` if undefined or not logged in.
	 */
	fun getProperty(path:String, def:Double):Double = if(isLoggedIn) propProfile.getProperty("$nameProp.$path", def) else def
	/**
	 * Gets a `char` property.
	 *
	 * @param path Property path
	 * @param def  Default value
	 * @return Value of property. Returns `def` if undefined or not logged in.
	 */
	fun getProperty(path:String, def:Char):Char = if(isLoggedIn) propProfile.getProperty("$nameProp.$path", def) else def
	/**
	 * Gets a `String` property.
	 *
	 * @param path Property path
	 * @param def  Default value
	 * @return Value of property. Returns `def` if undefined or not logged in.
	 */
	fun getProperty(path:String, def:String):String = if(isLoggedIn) propProfile.getProperty("$nameProp.$path", def) else def

	/**
	 * Gets an `int` property.
	 *
	 * @param path Property path
	 * @param def  Default value
	 * @return Value of property. Returns `def` if undefined or not logged in.
	 */
	fun getProperty(path:String, def:Boolean):Boolean = if(isLoggedIn) propProfile.getProperty("$nameProp.$path", def) else def
	//endregion
	//region setProperty() methods
	/**
	 * Sets a `byte` property.
	 *
	 * @param path Property path
	 * @param val  New value
	 */
	fun setProperty(path:String, `val`:Byte) {
		if(isLoggedIn) propProfile.setProperty("$nameProp.$path", `val`)
	}
	/**
	 * Sets a `byte` property.
	 *
	 * @param path Property path
	 * @param val  New value
	 */
	fun setProperty(path:String, `val`:Short) {
		if(isLoggedIn) propProfile.setProperty("$nameProp.$path", `val`)
	}
	/**
	 * Sets a `byte` property.
	 *
	 * @param path Property path
	 * @param val  New value
	 */
	fun setProperty(path:String, `val`:Int) {
		if(isLoggedIn) propProfile.setProperty("$nameProp.$path", `val`)
	}
	/**
	 * Sets a `byte` property.
	 *
	 * @param path Property path
	 * @param val  New value
	 */
	fun setProperty(path:String, `val`:Long) {
		if(isLoggedIn) propProfile.setProperty("$nameProp.$path", `val`)
	}
	/**
	 * Sets a `byte` property.
	 *
	 * @param path Property path
	 * @param val  New value
	 */
	fun setProperty(path:String, `val`:Float) {
		if(isLoggedIn) propProfile.setProperty("$nameProp.$path", `val`)
	}
	/**
	 * Sets a `byte` property.
	 *
	 * @param path Property path
	 * @param val  New value
	 */
	fun setProperty(path:String, `val`:Double) {
		if(isLoggedIn) propProfile.setProperty("$nameProp.$path", `val`)
	}
	/**
	 * Sets a `byte` property.
	 *
	 * @param path Property path
	 * @param val  New value
	 */
	fun setProperty(path:String, `val`:Char) {
		if(isLoggedIn) propProfile.setProperty("$nameProp.$path", `val`)
	}
	/**
	 * Sets a `String` property.
	 *
	 * @param path Property path
	 * @param val  New value
	 */
	fun setProperty(path:String, `val`:String?) {
		if(isLoggedIn) propProfile.setProperty("$nameProp.$path", `val`)
	}
	/**
	 * Sets a `byte` property.
	 *
	 * @param path Property path
	 * @param val  New value
	 */
	fun setProperty(path:String, `val`:Boolean) {
		if(isLoggedIn) {
			propProfile.setProperty("$nameProp.$path", `val`)
		}
	}
	//endregion
	/**
	 * Save properties to "config/setting/profile.cfg"
	 */
	fun saveProfileConfig():Boolean = try {
		propProfile.save("config/setting/profile.cfg")
		true
	} catch(e:IOException) {
		log.error("Failed to save mode config", e)
		false
	}

	fun reset() {
		nameDisplay = ""
		nameProp = ""
		isLoggedIn = false
	}
	/**
	 * Creates a new login screen with a custom heading color for a `ProfileProperties` instance.
	 *
	 * @param playerProperties `ProfileProperties` instance that is using this login screen.
	 * @param colorHeading    Text color. Get from [EventReceiver] class.
	 */
	class LoginScreen @JvmOverloads internal constructor(
		private val playerProperties:ProfileProperties,
		private val colorHeading:EventReceiver.COLOR = EventReceiver.COLOR.CYAN
	) {
		private var nameEntry = ""
		private var buttonPresses = IntArray(6)
		private var secondButtonPresses = IntArray(6)
		private var currentChar = 0
		private var state = Init
		private var login = false
		private var signup = false
		private var success = false
		/**
		 * Updates the screen data. Used for inputting data into the parent `ProfileProperty` instance.
		 *
		 * @param engine   Current GameEngine instance
		 * @return True to override onCustom routine
		 */
		fun updateScreen(engine:GameEngine):Boolean {
			val update = when(state) {
				Init -> onInitialScreen(engine)
				Name -> onNameInput(engine)
				Pin -> onPasswordInput(engine)
				Result -> onSuccessScreen(engine)
			}
			if(update) engine.statc[0]++
			return true
		}

		private fun onInitialScreen(engine:GameEngine):Boolean {
			/*
             * A: Log-in
             * B: Sign-up
             * E: Play as Guest
             */
			login = false
			signup = false
			nameEntry = ""
			success = false
			buttonPresses = IntArray(6)
			secondButtonPresses = IntArray(6)
			currentChar = 0
			when {
				engine.ctrl.isPush(Controller.BUTTON_A) -> {
					login = true
					state = Name
					engine.playSE("decide")
					engine.resetStatc()
					return false
				}
				engine.ctrl.isPush(Controller.BUTTON_B) -> {
					signup = true
					state = Name
					engine.playSE("decide")
					engine.resetStatc()
					return false
				}
				engine.ctrl.isPush(Controller.BUTTON_E) -> {
					engine.stat = GameEngine.Status.SETTING
					engine.playSE("decide")
					engine.resetStatc()
					return false
				}
				else -> return false
			}
		}

		private fun onNameInput(engine:GameEngine):Boolean {
			/*
             * DOWN - next letter
             * UP - prev. letter
             * A - confirm selection
             * B - backspace
             * E - go back
             */
			if(nameEntry.length==3) currentChar = ENTRY_CHARS.length-1
			when {
				engine.ctrl.isPress(Controller.BUTTON_RIGHT) -> {
					if(engine.statc[1]%6==0) {
						engine.playSE("change")
						currentChar++
					}
					engine.statc[1]++
				}
				engine.ctrl.isPress(Controller.BUTTON_LEFT) -> {
					if(engine.statc[1]%6==0) {
						engine.playSE("change")
						currentChar--
					}
					engine.statc[1]++
				}
				engine.ctrl.isPress(Controller.BUTTON_DOWN) -> {
					if(engine.statc[1]%10==0) {
						engine.playSE("change")
						currentChar += 10
					}
					engine.statc[1]++
				}
				engine.ctrl.isPress(Controller.BUTTON_UP) -> {
					if(engine.statc[1]%6==0) {
						engine.playSE("change")
						currentChar -= 10
					}
					engine.statc[1]++
				}
				engine.ctrl.isPush(Controller.BUTTON_A) -> {
					when(val s = getCharAt(currentChar)) {
						"p" -> {
							if(nameEntry.isNotEmpty()) nameEntry = nameEntry.substring(0, nameEntry.length-1)
							engine.playSE("change")
							currentChar = 0
						}
						"q" -> {
							if(nameEntry.length<3) nameEntry = "%-3s".format(nameEntry)
							engine.playSE("decide")
							currentChar = 0
							state = Pin
							engine.resetStatc()
						}
						else -> {
							nameEntry += s
							engine.playSE("decide")
						}
					}
				}
				engine.ctrl.isPush(Controller.BUTTON_B) -> {
					if(nameEntry.isNotEmpty()) {
						currentChar = ENTRY_CHARS.indexOf(nameEntry[nameEntry.length-1])
						nameEntry = nameEntry.substring(0, nameEntry.length-1)
					}
					engine.playSE("change")
				}
				engine.ctrl.isPush(Controller.BUTTON_E) -> {
					login = false
					signup = false
					state = Init
					engine.playSE("decide")
					engine.resetStatc()
					return false
				}
				else -> {
					engine.statc[1] = 0
				}
			}
			while(currentChar>=ENTRY_CHARS.length) currentChar -= ENTRY_CHARS.length
			while(currentChar<0) currentChar += ENTRY_CHARS.length
			return true
		}

		private fun onPasswordInput(engine:GameEngine):Boolean {
			when {
				engine.ctrl.isPush(Controller.BUTTON_A) -> {
					if(engine.statc[2]==0) buttonPresses[engine.statc[1]] = VALUE_BT_A else secondButtonPresses[engine.statc[1]] =
						VALUE_BT_A
					engine.playSE("change")
					engine.statc[1]++
				}
				engine.ctrl.isPush(Controller.BUTTON_B) -> {
					if(engine.statc[2]==0) buttonPresses[engine.statc[1]] = VALUE_BT_B else secondButtonPresses[engine.statc[1]] =
						VALUE_BT_B
					engine.playSE("change")
					engine.statc[1]++
				}
				engine.ctrl.isPush(Controller.BUTTON_C) -> {
					if(engine.statc[2]==0) buttonPresses[engine.statc[1]] = VALUE_BT_C else secondButtonPresses[engine.statc[1]] =
						VALUE_BT_C
					engine.playSE("change")
					engine.statc[1]++
				}
				engine.ctrl.isPush(Controller.BUTTON_D) -> {
					if(engine.statc[2]==0) buttonPresses[engine.statc[1]] = VALUE_BT_D else secondButtonPresses[engine.statc[1]] =
						VALUE_BT_D
					engine.playSE("change")
					engine.statc[1]++
				}
				engine.ctrl.isPush(Controller.BUTTON_E) -> {
					nameEntry = ""
					state = Name
					engine.playSE("decide")
					engine.resetStatc()
					return false
				}
			}
			if(engine.statc[1]==6&&engine.statc[2]==0&&signup) {
				engine.statc[1] = 0
				engine.statc[2] = 1
			} else if(engine.statc[1]==6) {
				if(login&&!signup) {
					success = playerProperties.attemptLogIn(nameEntry, buttonPresses)
				} else if(signup) {
					val adequate = isAdequate(buttonPresses, secondButtonPresses)&&!playerProperties.testPasswordCrash(
						nameEntry,
						buttonPresses
					)
					if(adequate) success = playerProperties.createAccount(nameEntry, buttonPresses)
				}
				if(success) engine.playSE("decide") else engine.playSE("regret")
				state = Result
				engine.resetStatc()
				return false
			}
			return true
		}

		/** Screen to draw. Use it inside onCustom and renderLast.*/

		private fun onSuccessScreen(engine:GameEngine):Boolean {
			if(engine.statc[0]>=180) {
				if(success) engine.stat = GameEngine.Status.SETTING else state = Init
				engine.resetStatc()
				return false
			}
			return true
		}
		/** Render the current login screen.*/
		fun renderScreen(receiver:EventReceiver, engine:GameEngine) {
			val pid = engine.playerID
			when(state) {
				Init -> {
					// region INITIAL SCREEN
					GameTextUtilities.drawMenuTextAlign(
						receiver, engine, 5, 0, GameTextUtilities.ALIGN_TOP_MIDDLE,
						"Player", colorHeading, 2f
					)
					GameTextUtilities.drawMenuTextAlign(
						receiver, engine, 5, 2, GameTextUtilities.ALIGN_TOP_MIDDLE,
						"Data", colorHeading, 2f
					)
					receiver.drawMenuFont(engine, 0, 8, "A: Login ", EventReceiver.COLOR.YELLOW)
					receiver.drawMenuFont(engine, 0, 9, "B: New SignUp", EventReceiver.COLOR.YELLOW)
					receiver.drawMenuFont(engine, 0, 11, "E: Play as Guest", EventReceiver.COLOR.YELLOW)
					receiver.drawMenuNano(engine, 0, 18, "SELECT NEXT ACTION.", scale = .75f)
				}
				Name -> {
					// region NAME INPUT
					GameTextUtilities.drawMenuTextAlign(
						receiver, engine, 5, 0, GameTextUtilities.ALIGN_TOP_MIDDLE,
						"Name", colorHeading, 2f
					)
					GameTextUtilities.drawMenuTextAlign(
						receiver, engine, 5, 2, GameTextUtilities.ALIGN_TOP_MIDDLE,
						"Entry", colorHeading, 2f
					)
					receiver.drawMenuFont(engine, 2, 8, nameEntry, scale = 2f)
					val c = if(engine.statc[0]/6%2==0) EventReceiver.COLOR.RAINBOW else EventReceiver.COLOR.WHITE
					receiver.drawMenuFont(engine, 2+nameEntry.length*2, 8, getCharAt(currentChar), c, 2f)
					ENTRY_CHARS.forEachIndexed {i, c ->
						receiver.drawMenuFont(engine, i%10, 11+i/10, "$c", i==currentChar)
					}
					receiver.drawMenuFont(engine, 0, 18, "Input Your ID name.", scale = .75f)
				}
				Pin -> {
					// region PASSWORD INPUT
					GameTextUtilities.drawMenuTextAlign(
						receiver, engine, 5, 0, GameTextUtilities.ALIGN_TOP_MIDDLE,
						"Pass Phrase", colorHeading, .75f
					)
					receiver.drawMenuFont(engine, 2, 8, nameEntry, scale = 2f)
					var x = 0
					while(x<6) {
						val chr = if(x<engine.statc[1]||x==engine.statc[1]&&engine.statc[0]/2%2==0) BaseFont.CIRCLE_S else BaseFont.CIRCLE_L
						receiver.drawMenuFont(engine, x+2, 12, chr, colorHeading)
						x++
					}
					receiver.drawMenuNano(
						engine, 0, 18,
						if(signup&&engine.statc[2]==1) "Input your PIN again" else "Input your PIN code"
					)
					receiver.drawMenuNano(engine, 0, 9, "With your ABCD Buttons")
				}
				Result -> {
					// region SUCCESS SCREEN
					val s = if(success) "OK" else "ERROR"
					val col = if(engine.statc[0]/6%2==0)
						if(success) EventReceiver.COLOR.RAINBOW else EventReceiver.COLOR.RED
					else EventReceiver.COLOR.WHITE

					GameTextUtilities.drawMenuTextAlign(
						receiver, engine, 5, 9, GameTextUtilities.ALIGN_TOP_MIDDLE,
						s, col, 2f
					)
				}
			}
		}

		companion object {
			/** Screen states */
			enum class State { Init, Name, Pin, Result }
		}
	}

	companion object {
		/**
		 * Button password values
		 */
		const val VALUE_BT_A = 1
		const val VALUE_BT_B = 2
		const val VALUE_BT_C = 4
		const val VALUE_BT_D = 8
		/**
		 * Valid characters in a name selector:<br></br>
		 * Please note that "p" is backspace and "q" is end entry.
		 */
		const val ENTRY_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789?!. ${NAME_REV}${NAME_END}"
		/**
		 * Debug logger
		 */
		private val log = LogManager.getLogger()
		/**
		 * Profile prefixes.
		 */
		private const val PREFIX_NAME = "profile.name."
		private const val PREFIX_PASS = "profile.password."
		/**
		 * Gets the valid name character at the index.
		 *
		 * @param index Character index
		 * @return Character at index
		 */
		fun getCharAt(index:Int):String {
			val i = MathHelper.pythonModulo(index, ENTRY_CHARS.length)
			return ENTRY_CHARS.substring(i, i+1)
		}
		/**
		 * Test two button sequences to see if they are the same and each contain 6 presses.<br></br>
		 * This is used outside in a mode during its password entry sequence.
		 *
		 * @param pass1 Button press sequence (exp. length 6)
		 * @param pass2 Button press sequence (exp. length 6)
		 * @return Same?
		 */
		private fun isAdequate(pass1:IntArray, pass2:IntArray):Boolean {
			if(pass1.size!=pass2.size) return false
			if(pass1.size!=6) return false
			for(i in pass1.indices) {
				if(pass1[i]!=pass2[i]) return false
			}
			return true
		}
	}

}
