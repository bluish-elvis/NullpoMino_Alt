/*
 * This library class was created by 0xFC963F18DC21 / Shots243
 * It is part of an extension library for the game NullpoMino (copyright 2010)
 *
 * Herewith shall the term "Library Creator" be given to 0xFC963F18DC21.
 * Herewith shall the term "Game Creator" be given to the original creator of NullpoMino.
 *
 * THIS LIBRARY AND MODE PACK WAS NOT MADE IN ASSOCIATION WITH THE GAME CREATOR.
 *
 * Repository: https://github.com/Shots243/ModePile
 *
 * When using this library in a mode / library pack of your own, the following
 * conditions must be satisfied:
 *     - This license must remain visible at the top of the document, unmodified.
 *     - You are allowed to use this library for any modding purpose.
 *         - If this is the case, the Library Creator must be credited somewhere.
 *             - Source comments only are fine, but in a README is recommended.
 *     - Modification of this library is allowed, but only in the condition that a
 *       pull request is made to merge the changes to the repository.
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
package zeroxfc.nullpo.custom.libs

import mu.nu.nullpo.game.event.EventReceiver
import mu.nu.nullpo.game.play.GameEngine
import kotlin.random.Random

object GameTextUtilities {
	/**
	 * Rainbow color order
	 */
	private val RAINBOW_ORDER = arrayOf(
		EventReceiver.COLOR.RED,
		EventReceiver.COLOR.ORANGE,
		EventReceiver.COLOR.YELLOW,
		EventReceiver.COLOR.WHITE,
		EventReceiver.COLOR.GREEN,
		EventReceiver.COLOR.CYAN,
		EventReceiver.COLOR.BLUE,
		EventReceiver.COLOR.COBALT,
		EventReceiver.COLOR.PURPLE,
		EventReceiver.COLOR.PINK)
	/**
	 * Text alignment option
	 */
	const val ALIGN_TOP_LEFT = 0
	const val ALIGN_TOP_MIDDLE = 1
	private const val ALIGN_TOP_RIGHT = 2
	private const val ALIGN_MIDDLE_LEFT = 3
	const val ALIGN_MIDDLE_MIDDLE = 4
	private const val ALIGN_MIDDLE_RIGHT = 5
	private const val ALIGN_BOTTOM_LEFT = 6
	private const val ALIGN_BOTTOM_MIDDLE = 7
	private const val ALIGN_BOTTOM_RIGHT = 8
	/**
	 * Rainbow color count
	 */
	private const val RAINBOW_COLORS = 10
	/**
	 * Valid characters
	 */
	private const val CHARACTERS = " !\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopq"
	/**
	 * Sequential Character Phase
	 */
	private var CharacterPhase = 0

// region Character Phase Functions
	/**
	 * Get current character in sequence.
	 *
	 * @return Character at phase.
	 */
	val currentCharacter:Char
		get() = CHARACTERS[CharacterPhase]
	/**
	 * Gets current character in sequence with offset.
	 *
	 * @param offset Character offset.
	 * @return Offset sequence character.
	 */
	fun getCurrentCharacter(offset:Int):Char {
		var i = CharacterPhase+offset
		i = MathHelper.pythonModulo(i, CHARACTERS.length)
		return CHARACTERS[i]
	}
	/**
	 * Increments character phase by x.
	 *
	 * @param x Amount to increment by.
	 */
	@JvmOverloads fun updatePhase(x:Int = 1) {
		CharacterPhase = MathHelper.pythonModulo(CharacterPhase+x, CHARACTERS.length)
	}
	/**
	 * Resets phase to 0.
	 */
	fun resetPhase() {
		CharacterPhase = 0
	}
	/**
	 * Sets the character phase.
	 *
	 * @param x Integer to set phase to.
	 */
	fun setPhase(x:Int) {
		if(CharacterPhase<0) CharacterPhase += CHARACTERS.length
		CharacterPhase = x%CHARACTERS.length
	} // endregion Character Phase Functions
	// region String Utilities
	/**
	 * Generates a completely random string.
	 *
	 * @param length       Length of string
	 * @param randomEngine Random instance to use
	 * @return Random string, with all characters either being visible or a space.
	 */
	fun randomString(length:Int, randomEngine:Random):String {
		val sb = StringBuilder()
		for(i in 0 until length) {
			sb.append(CHARACTERS[randomEngine.nextInt(CHARACTERS.length)])
		}
		return "$sb"
	}
	/**
	 * Completely obfuscates a string.
	 *
	 * @param str          String to obfuscate
	 * @param randomEngine Random instance to use
	 * @return Obfuscated string.
	 */
	fun obfuscateString(str:String?, randomEngine:Random):String? = obfuscateString(str, 1.0, randomEngine)
	/**
	 * Obfuscates a string with random characters.
	 *
	 * @param str          String to obfuscate
	 * @param chance       Chance of character obfuscation (0 < chance <= 1)
	 * @param randomEngine Random instance to use
	 * @return Obfuscated string.
	 */
	private fun obfuscateString(str:String?, chance:Double, randomEngine:Random):String? {
		if(chance<=0) return str
		val sb = StringBuilder(str)
		for(i in sb.indices) {
			val c = randomEngine.nextDouble()
			if(c<chance) {
				sb.setCharAt(i, CHARACTERS[randomEngine.nextInt(CHARACTERS.length)])
			}
		}
		return "$sb"
	}
	// endregion String Utilities
	// region Aligned Text
	/**
	 * Draws an aligned string using `drawDirectFont`.
	 *
	 * @param receiver  EventReceiver used to draw
	 * @param engine    Current GameEngine
	 * @param playerID  Player ID (1P = 0)
	 * @param x         X coordinate of top-left corner of text
	 * @param y         Y coordinate of top-left corner of text
	 * @param alignment Alignment of string relative to string's area
	 * @param str       String to draw
	 * @param color     Color of string
	 * @param scale     Scale of string
	 */
	fun drawDirectTextAlign(receiver:EventReceiver, engine:GameEngine, playerID:Int, x:Int, y:Int, alignment:Int, str:String,
		color:EventReceiver.COLOR = EventReceiver.COLOR.WHITE, scale:Float = 1f) {
		val offsetX:Int = when(alignment) {
			ALIGN_TOP_MIDDLE, ALIGN_MIDDLE_MIDDLE, ALIGN_BOTTOM_MIDDLE -> (8*str.length*scale).toInt()
			ALIGN_TOP_RIGHT, ALIGN_MIDDLE_RIGHT, ALIGN_BOTTOM_RIGHT -> (16*str.length*scale).toInt()
			else -> 0
		}
		val offsetY:Int = when(alignment) {
			ALIGN_MIDDLE_LEFT, ALIGN_MIDDLE_MIDDLE, ALIGN_MIDDLE_RIGHT -> (8*scale).toInt()
			ALIGN_BOTTOM_LEFT, ALIGN_BOTTOM_MIDDLE, ALIGN_BOTTOM_RIGHT -> (16*scale).toInt()
			else -> 0
		}
		receiver.drawDirectFont(x-offsetX, y-offsetY, str, color, scale)
	}
	/**
	 * Draws an aligned string using `drawScoreFont`.
	 *
	 * @param receiver  EventReceiver used to draw
	 * @param engine    Current GameEngine
	 * @param playerID  Player ID (1P = 0)
	 * @param x         X coordinate of top-left corner of text
	 * @param y         Y coordinate of top-left corner of text
	 * @param alignment Alignment of string relative to string's area
	 * @param str       String to draw
	 * @param color     Color of string
	 * @param scale     Scale of string
	 */
	fun drawScoreTextAlign(receiver:EventReceiver, engine:GameEngine, playerID:Int, x:Int, y:Int, alignment:Int, str:String,
		color:EventReceiver.COLOR = EventReceiver.COLOR.WHITE, scale:Float = 1f) {
		val offsetX:Int = when(alignment) {
			ALIGN_TOP_MIDDLE, ALIGN_MIDDLE_MIDDLE, ALIGN_BOTTOM_MIDDLE -> str.length/2
			ALIGN_TOP_RIGHT, ALIGN_MIDDLE_RIGHT, ALIGN_BOTTOM_RIGHT -> str.length
			else -> 0
		}
		val offsetY:Int = when(alignment) {
			ALIGN_MIDDLE_LEFT, ALIGN_MIDDLE_MIDDLE, ALIGN_MIDDLE_RIGHT -> (scale/2).toInt()
			ALIGN_BOTTOM_LEFT, ALIGN_BOTTOM_MIDDLE, ALIGN_BOTTOM_RIGHT -> (scale*1).toInt()
			else -> 0
		}
		receiver.drawScoreFont(engine, playerID, x-offsetX, y-offsetY, str, color, scale)
	}
	/**
	 * Draws an aligned string using `drawMenuFont`.
	 *
	 * @param receiver  EventReceiver used to draw
	 * @param engine    Current GameEngine
	 * @param playerID  Player ID (1P = 0)
	 * @param x         X coordinate of top-left corner of text
	 * @param y         Y coordinate of top-left corner of text
	 * @param alignment Alignment of string relative to string's area
	 * @param str       String to draw
	 * @param color     Color of string
	 * @param scale     Scale of string
	 */
	fun drawMenuTextAlign(receiver:EventReceiver, engine:GameEngine, playerID:Int, x:Int, y:Int, alignment:Int, str:String?,
		color:EventReceiver.COLOR = EventReceiver.COLOR.WHITE, scale:Float = 1f) {
		str ?: return
		val offsetX:Int = when(alignment) {
			ALIGN_TOP_MIDDLE, ALIGN_MIDDLE_MIDDLE, ALIGN_BOTTOM_MIDDLE -> (str.length*scale/2).toInt()
			ALIGN_TOP_RIGHT, ALIGN_MIDDLE_RIGHT, ALIGN_BOTTOM_RIGHT -> (str.length*scale).toInt()
			else -> 0
		}
		receiver.drawMenuFont(engine, playerID, x-offsetX, y, str, color, scale)
	}
	/**
	 * Draws a rainbow string using `drawDirectFont`.
	 *
	 * @param receiver    EventReceiver used to draw
	 * @param x           X coordinate of top-left corner of text
	 * @param y           Y coordinate of top-left corner of text
	 * @param str         String to draw
	 * @param startColor Starting color of text
	 * @param scale       Scale of text
	 * @param reverse     Reverse order or not
	 */
	@JvmOverloads fun drawRainbowDirectString(receiver:EventReceiver, x:Int, y:Int, str:String,
		startColor:EventReceiver.COLOR, scale:Float, reverse:Boolean = false) {
		var offset = 0
		for(i in str.indices) {
			if(str[i]==' ') {
				offset++
			} else {
				var j = (listOf(*RAINBOW_ORDER)
					.indexOf(startColor)+i*if(reverse) -1 else 1-offset*if(reverse) -1 else 1)%RAINBOW_COLORS
				if(j<0) j = RAINBOW_COLORS-j
				receiver.drawDirectFont(x+(i*16*scale).toInt(), y, str.substring(i, i+1), RAINBOW_ORDER[j])
			}
		}
	}
	/**
	 * Draws a rainbow string using `drawDirectFont`.
	 *
	 * @param receiver     EventReceiver used to draw
	 * @param x            X coordinate of top-left corner of text
	 * @param y            Y coordinate of top-left corner of text
	 * @param str          String to draw
	 * @param randomEngine Random instance to use
	 * @param scale        Scale of text
	 */
	fun drawRandomRainbowDirectString(receiver:EventReceiver, x:Int, y:Int, str:String,
		randomEngine:Random, scale:Float) {
		var offset = 0
		for(i in str.indices) {
			if(str[i]==' ') {
				offset++
			} else {
				receiver.drawDirectFont(x+(i*16*scale).toInt(), y, str.substring(i, i+1),
					RAINBOW_ORDER[randomEngine.nextInt(RAINBOW_COLORS)])
			}
		}
	}
	/**
	 * Draws a rainbow string using `drawScoreFont`.
	 *
	 * @param receiver     EventReceiver used to draw
	 * @param engine       Current GameEngine
	 * @param playerID     Player ID (1P = 0)
	 * @param x            X coordinate of top-left corner of text
	 * @param y            Y coordinate of top-left corner of text
	 * @param str          String to draw
	 * @param randomEngine Random instance to use
	 * @param scale        Scale of text
	 */
	fun drawRandomRainbowScoreString(receiver:EventReceiver, engine:GameEngine, playerID:Int, x:Int, y:Int, str:String,
		randomEngine:Random, scale:Float) {
		var offset = 0
		for(i in str.indices) {
			if(str[i]==' ') {
				offset++
			} else {
				receiver.drawScoreFont(engine, playerID, x+i, y, str.substring(i, i+1), RAINBOW_ORDER[randomEngine.nextInt(
					RAINBOW_COLORS)])
			}
		}
	}
	/**
	 * Draws a rainbow string using `drawMenuFont`.
	 *
	 * @param receiver     EventReceiver used to draw
	 * @param engine       Current GameEngine
	 * @param playerID     Player ID (1P = 0)
	 * @param x            X coordinate of top-left corner of text
	 * @param y            Y coordinate of top-left corner of text
	 * @param str          String to draw
	 * @param randomEngine Random instance to use
	 * @param scale        Scale of text
	 */
	fun drawRandomRainbowMenuString(receiver:EventReceiver, engine:GameEngine, playerID:Int, x:Int, y:Int, str:String,
		randomEngine:Random, scale:Float) {
		var offset = 0
		for(i in str.indices) {
			if(str[i]==' ') {
				offset++
			} else {
				receiver.drawMenuFont(engine, playerID, x+i, y, str.substring(i, i+1), RAINBOW_ORDER[randomEngine.nextInt(
					RAINBOW_COLORS)])
			}
		}
	}
	// endregion Rainbow Text
	// region Mixed Color Text
	/**
	 * Draws a mixed-color string to a location using `drawDirectFont`.
	 *
	 *
	 * Uses `String[] and int[]` for the "text String" and "color int" data in this format:
	 *
	 * For newlines, insert a -1 for color.
	 * By default, all text is left-aligned.
	 *
	 * @param engine       GameEngine to draw with.
	 * @param playerID     Player to draw next to (0 = 1P).
	 * @param stringData   String[] containing text data.
	 * @param colorData   int[] containing color data.
	 * @param destinationX X of destination (uses drawDirectFont(...)).
	 * @param destinationY Y of destination (uses drawDirectFont(...)).
	 * @param scale        Text scale (0.5f, 1.0f, 2.0f).
	 */
	fun drawMixedColorDirectString(receiver:EventReceiver, engine:GameEngine, playerID:Int, stringData:Array<String>,
		colorData:Array<EventReceiver.COLOR>, destinationX:Int, destinationY:Int, scale:Float) {
		if(stringData.size!=colorData.size||stringData.isEmpty()) return
		var counterX = 0
		var counterY = 0
		for(i in stringData.indices) {
			if(colorData[i]!=EventReceiver.COLOR.RAINBOW) {
				receiver.drawDirectFont(destinationX+(counterX*16*scale).toInt(),
					destinationY+(counterY*16*scale).toInt(), stringData[i], colorData[i], scale)
				counterX += stringData[i].length
			} else {
				counterX = 0
				counterY++
			}
		}
	}
	/**
	 * Draws a mixed-color string to a location using `drawScoreFont`.
	 *
	 *
	 * Uses `String[] and int[]` for the "text String" and "color int" data in this format:
	 *
	 *
	 * By default, all text is left-aligned.
	 *
	 * @param engine       GameEngine to draw with.
	 * @param playerID     Player to draw next to (0 = 1P).
	 * @param stringData   String[] containing text data.
	 * @param colorData   int[] containing color data.
	 * @param destinationX X of destination (uses drawScoreFont(...)).
	 * @param destinationY Y of destination (uses drawScoreFont(...)).
	 * @param scale        Text scale (0.5f, 1.0f, 2.0f).
	 */
	fun drawMixedColorScoreString(receiver:EventReceiver, engine:GameEngine, playerID:Int, stringData:Array<String>,
		colorData:Array<EventReceiver.COLOR>, destinationX:Int, destinationY:Int, scale:Float) {
		if(stringData.size!=colorData.size||stringData.isEmpty()) return
		var counterX = 0
		val counterY = 0
		for(i in stringData.indices) {
			receiver.drawScoreFont(engine, playerID, destinationX+counterX, destinationY+counterY,
				stringData[i], colorData[i], scale)
			counterX += stringData[i].length
		}
	}
	/**
	 * Draws a mixed-color string to a location using `drawMenuFont`.
	 *
	 *
	 * Uses `String[] and int[]` for the "text String" and "color int" data in this format:
	 *
	 *
	 * By default, all text is left-aligned.
	 *
	 * @param engine       GameEngine to draw with.
	 * @param playerID     Player to draw next to (0 = 1P).
	 * @param stringData   String[] containing text data.
	 * @param colorData   int[] containing color data.
	 * @param destinationX X of destination (uses drawMenuFont(...)).
	 * @param destinationY Y of destination (uses drawMenuFont(...)).
	 * @param scale        Text scale (0.5f, 1.0f, 2.0f).
	 */
	fun drawMixedColorMenuString(receiver:EventReceiver, engine:GameEngine, playerID:Int, stringData:Array<String>,
		colorData:Array<EventReceiver.COLOR>, destinationX:Int, destinationY:Int, scale:Float) {
		if(stringData.size!=colorData.size||stringData.isEmpty()) return
		var counterX = 0
		val counterY = 0
		for(i in stringData.indices) {
			receiver.drawMenuFont(engine, playerID, destinationX+counterX, destinationY+counterY,
				stringData[i], colorData[i], scale)
			counterX += stringData[i].length
		}
	}
//endregion Mixed Color Text
// region Color Alternator Text
/**
 * Draws an alternating-color string to a location using `drawDirectFont`.
 *
 * @param engine     GameEngine to draw with.
 * @param playerID   Player to draw next to (0 = 1P).
 * @param string     Text.
 * @param colorData int[] containing color data.
 * @param offset     Start offset of color array.
 * @param x          X of destination (uses drawDirectFont(...)).
 * @param y          Y of destination (uses drawDirectFont(...)).
 * @param scale      Text scale (0.5f, 1.0f, 2.0f).
 */
fun drawAlternatorColorDirectString(receiver:EventReceiver, engine:GameEngine, playerID:Int, string:String,
	colorData:Array<EventReceiver.COLOR>, offset:Int, x:Int, y:Int, scale:Float) {
	var offset = offset
	if(colorData.isEmpty()) return
	offset %= colorData.size
	if(offset<0) offset += colorData.size
	for(i in string.indices) {
		receiver.drawDirectFont(x+(16*i*scale).toInt(), y, string.substring(i, i+1),
			colorData[(offset+i)%colorData.size], scale)
	}
}
/**
 * Draws an alternating-color string to a location using `drawScoreFont`.
 *
 * @param engine     GameEngine to draw with.
 * @param playerID   Player to draw next to (0 = 1P).
 * @param string     Text.
 * @param colorData int[] containing color data.
 * @param offset     Start offset of color array.
 * @param x          X of destination (uses drawScoreFont(...)).
 * @param y          Y of destination (uses drawScoreFont(...)).
 * @param scale      Text scale (0.5f, 1.0f, 2.0f).
 */
fun drawAlternatorColorScoreString(receiver:EventReceiver, engine:GameEngine, playerID:Int, string:String,
	colorData:Array<EventReceiver.COLOR>, offset:Int, x:Int, y:Int, scale:Float) {
	var offset = offset
	if(colorData.isEmpty()) return
	offset %= colorData.size
	if(offset<0) offset += colorData.size
	for(i in string.indices) {
		receiver.drawScoreFont(engine, playerID, x, y, string.substring(i, i+1), colorData[(offset+i)%colorData.size], scale)
	}

	/**
	 * Draws an alternating-color string to a location using `drawMenuFont`.
	 *
	 * @param engine     GameEngine to draw with.
	 * @param playerID   Player to draw next to (0 = 1P).
	 * @param string     Text.
	 * @param colorData int[] containing color data.
	 * @param offset     Start offset of color array.
	 * @param x          X of destination (uses drawMenuFont(...)).
	 * @param y          Y of destination (uses drawMenuFont(...)).
	 * @param scale      Text scale (0.5f, 1.0f, 2.0f).
	 */
	fun drawAlternatorColorMenuString(receiver:EventReceiver, engine:GameEngine, playerID:Int, string:String,
		colorData:Array<EventReceiver.COLOR>, offset:Int, x:Int, y:Int, scale:Float) {
		var offset = offset
		if(colorData.isEmpty()) return
		offset %= colorData.size
		if(offset<0) offset += colorData.size
		for(i in string.indices) {
			receiver.drawScoreFont(engine, playerID, x, y, string.substring(i, i+1), colorData[(offset+i)%colorData.size], scale)
		}

	}
// endregion Color Alternator Text
}
}