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
package mu.nu.nullpo.util

import mu.nu.nullpo.game.component.Piece
import mu.nu.nullpo.game.component.RuleOptions
import mu.nu.nullpo.game.subsystem.ai.DummyAI
import mu.nu.nullpo.game.subsystem.wallkick.Wallkick
import net.omegaboshi.nullpomino.game.subsystem.randomizer.Randomizer
import org.apache.log4j.Logger
import java.io.FileInputStream
import java.text.SimpleDateFormat
import java.util.*

/** Generic static utils */
object GeneralUtil {
	/** Log */
	internal val log = Logger.getLogger(GeneralUtil::class.java)

	/** Fetches the filename for a replay
	 * @return Replay's filename
	 */
	val replayFilename:String
		get() = getReplayFilename("")

	/** Converts play time into a String
	 * @param t Play time
	 * @return String for play time
	 */
	fun getTime(t:Float):String = if(t<0) "--:--.--" else String.format("%02d:%02d.%02d", t.toInt()/3600, t.toInt()/60%60, (t%60*5f/3f).toInt())

	/** Returns ON if b is true, OFF if b is false
	 * @param b Boolean variable to be checked
	 * @return ON if b is true, OFF if b is false
	 */

	@JvmOverloads
	fun getONorOFF(b:Boolean, islong:Boolean = false):String = if(b) if(islong) "c ENABLE" else "c ON" else if(islong) "c DISABLE" else "e OFF"

	/** Returns ○ if b is true, × if b is false
	 * @param b Boolean variable to be checked
	 * @return ○ if b is true, × if b is false
	 */
	fun getOorX(b:Boolean):String = if(b) "c" else "e"

	fun getReplayFilename(name:String):String {
		val c = Calendar.getInstance()
		val dfm = SimpleDateFormat("yyyyMMddHHmm_")
		return dfm.format(c.time)+name+".rep"
	}

	/** Get date and time from a Calendar
	 * @param c Calendar
	 * @return Date and Time String
	 */
	fun getCalendarString(c:Calendar):String {
		val dfm = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
		return dfm.format(c.time)
	}

	/** Get date and time from a Calendar with specific TimeZone
	 * @param c Calendar
	 * @param z TimeZone
	 * @return Date and Time String
	 */
	fun getCalendarString(c:Calendar, z:TimeZone):String {
		val dfm = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
		dfm.timeZone = z
		return dfm.format(c.time)
	}

	/** Get date from a Calendar
	 * @param c Calendar
	 * @return Date String
	 */
	fun getCalendarStringDate(c:Calendar):String {
		val dfm = SimpleDateFormat("yyyy-MM-dd")
		return dfm.format(c.time)
	}

	/** Get date from a Calendar with specific TimeZone
	 * @param c Calendar
	 * @param z TimeZone
	 * @return Date String
	 */
	fun getCalendarStringDate(c:Calendar, z:TimeZone):String {
		val dfm = SimpleDateFormat("yyyy-MM-dd")
		dfm.timeZone = z
		return dfm.format(c.time)
	}

	/** Get time from a Calendar
	 * @param c Calendar
	 * @return Time String
	 */
	fun getCalendarStringTime(c:Calendar):String {
		val dfm = SimpleDateFormat("HH:mm:ss")
		return dfm.format(c.time)
	}

	/** Get time from a Calendar with specific TimeZone
	 * @param c Calendar
	 * @param z TimeZone
	 * @return Time String
	 */
	fun getCalendarStringTime(c:Calendar, z:TimeZone):String {
		val dfm = SimpleDateFormat("HH:mm:ss")
		dfm.timeZone = z
		return dfm.format(c.time)
	}

	/** Export a Calendar to a String for saving/sending. TimeZone is always
	 * GMT. Time is based on current time.
	 * @return Calendar String (Each field is separated with a hyphen '-')
	 */
	fun exportCalendarString():String {
		val c = Calendar.getInstance(TimeZone.getTimeZone("GMT"))
		return exportCalendarString(c)
	}

	/** Export a Calendar to a String for saving/sending. TimeZone is always
	 * GMT.
	 * @param c Calendar
	 * @return Calendar String (Each field is separated with a hyphen '-')
	 */
	fun exportCalendarString(c:Calendar):String {
		val dfm = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss")
		dfm.timeZone = TimeZone.getTimeZone("GMT")
		return dfm.format(c.time)
	}

	/** Create a Calendar by using a String that came from exportCalendarString.
	 * TimeZone is always GMT.
	 * @param s String (Each field is separated with a hyphen '-')
	 * @return Calendar (null if fails)
	 */
	fun importCalendarString(s:String):Calendar? {
		val dfm = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss")
		dfm.timeZone = TimeZone.getTimeZone("GMT")

		val c = Calendar.getInstance(TimeZone.getTimeZone("GMT"))

		try {
			val date = dfm.parse(s)
			c.time = date
		} catch(e:Exception) {
			return null
		}

		return c
	}

	/** Get the number of piece types can appear
	 * @param pieceEnable Piece enable flags
	 * @return Number of piece types can appear (In the normal Tetromino games,
	 * it returns 7)
	 */
	fun getNumberOfPiecesCanAppear(pieceEnable:BooleanArray?):Int {
		if(pieceEnable==null) return Piece.PIECE_COUNT

		var count = 0

		for(element in pieceEnable)
			if(element) count++

		return count
	}

	/** Returns true if enabled piece types are S,Z,O only.
	 * @param pieceEnable Piece enable flags
	 * @return `true` if enabled piece types are S,Z,O only.
	 */
	fun isPieceSZOOnly(pieceEnable:BooleanArray?):Boolean =
		pieceEnable?.let {it[Piece.PIECE_S]&&it[Piece.PIECE_Z]&&it[Piece.PIECE_O]} ?: false

	/** Create piece ID array from a String
	 * @param strSrc String
	 * @return Piece ID array
	 */
	fun createNextPieceArrayFromNumberString(strSrc:String):IntArray {
		val len = strSrc.length
		if(len<1) return IntArray(0)

		val nextArray = IntArray(len)
		for(i in 0 until len) {
			var pieceID = Piece.PIECE_I

			try {
				pieceID = Character.getNumericValue(strSrc[i])%Piece.PIECE_STANDARD_COUNT
			} catch(e:NumberFormatException) {
			}

			if(pieceID<0||pieceID>=Piece.PIECE_STANDARD_COUNT) pieceID = Piece.PIECE_I

			nextArray[i] = pieceID
		}

		return nextArray
	}

	/** Load rule file
	 * @param filename Filename
	 * @return RuleOptions
	 */
	fun loadRule(filename:String):RuleOptions {
		val prop = CustomProperties()

		try {
			val `in` = FileInputStream(filename)
			prop.load(`in`)
			`in`.close()
		} catch(e:Exception) {
			log.warn("Failed to load rule from $filename", e)
		}

		val ruleopt = RuleOptions()
		ruleopt.readProperty(prop, 0)

		return ruleopt
	}

	/** Load Randomizer
	 * @param filename Classpath of the randomizer
	 * @return Randomizer (null if something fails)
	 */
	fun loadRandomizer(filename:String):Randomizer? {
		val randomizerClass:Class<*>
		var randomizerObject:Randomizer? = null

		try {
			randomizerClass = Class.forName(filename)
			randomizerObject = randomizerClass.newInstance() as Randomizer
		} catch(e:Exception) {
			log.warn("Failed to load Randomizer from $filename", e)
		}

		return randomizerObject
	}

	/** Load Wallkick
	 * @param filename Classpath of the wallkick
	 * @return Wallkick (null if something fails)
	 */
	fun loadWallkick(filename:String):Wallkick? {
		val wallkickClass:Class<*>
		var wallkickObject:Wallkick? = null

		try {
			wallkickClass = Class.forName(filename)
			wallkickObject = wallkickClass.newInstance() as Wallkick
		} catch(e:Exception) {
			log.warn("Failed to load Wallkick from $filename", e)
		}

		return wallkickObject
	}

	/** Load AI
	 * @param filename Classpath of the AI
	 * @return The instance of AI (null if something fails)
	 */
	fun loadAIPlayer(filename:String):DummyAI? {
		val aiClass:Class<*>
		var aiObject:DummyAI? = null

		try {
			aiClass = Class.forName(filename)
			aiObject = aiClass.newInstance() as DummyAI
		} catch(e:Exception) {
			log.warn("Failed to load AIPlayer from $filename", e)
		}

		return aiObject
	}

	/** Combine array of strings
	 * @param strings Array of strings
	 * @param separator Separator used for combine
	 * @param startIndex First element which will be combined
	 * @return Combined string
	 */
	fun stringCombine(strings:Array<String>, separator:String,
		startIndex:Int):String {
		val res = StringBuilder()
		for(i in startIndex until strings.size) {
			res.append(strings[i])
			if(i!=strings.size-1) res.append(separator)
		}

		return res.toString()

	}

	inline fun nulltoEmpty(a:String?):String = a ?: ""

	fun capsInteger(x:Int, digits:Int):String = when {
		digits<=0 -> ""
		x<=0 -> (1 until digits).fold("0"){b,_->b+"0"}
		x.toString().length>digits -> {
			val y = (1 until digits).fold(x){b,_->b.div(10)}
			val z = (1 until digits).fold(minOf(y,35)){b,_->b.times(10)}
			('A'.toInt()+minOf(y-10,25)).toChar().toString()+
				if(digits>1)capsInteger(x-z, digits-1)else ""
		}
		digits>0 -> String.format("%0${digits}d", x)
		else -> ""
	}

}
