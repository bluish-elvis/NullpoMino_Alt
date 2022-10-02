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
package mu.nu.nullpo.util

import mu.nu.nullpo.game.component.Piece
import mu.nu.nullpo.game.component.RuleOptions
import mu.nu.nullpo.game.subsystem.ai.AIPlayer
import mu.nu.nullpo.game.subsystem.wallkick.Wallkick
import net.omegaboshi.nullpomino.game.subsystem.randomizer.MemorylessRandomizer
import net.omegaboshi.nullpomino.game.subsystem.randomizer.Randomizer
import org.apache.logging.log4j.LogManager
import java.io.FileInputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.TimeZone
import java.util.zip.GZIPInputStream

/** Generic static utils */
object GeneralUtil {
	/** Log */
	internal val log = LogManager.getLogger()
	@Suppress("UNCHECKED_CAST")
	fun <T:Any?> flattenList(nestList:Collection<*>, flatList:MutableList<T> = mutableListOf()):List<T> {
		for(e in nestList)
			if(e is Collection<*>)
			// using unchecked cast here as can't check for instance of 'erased' generic type
				flattenList(e as List<T>, flatList)
			else
				flatList.add(e as T)

		return flatList.toList()
	}

	/** Fetches the filename for a replay
	 * @return Replay's filename
	 */
	val replayFilename:String
		get() = "".toReplayFilename

	val String.toReplayFilename:String
		get() {
			val c = Calendar.getInstance()
			val dfm = SimpleDateFormat("yyyyMMddHHmm_")
			return "${dfm.format(c.time)}$this.rep"
		}

	fun Boolean.toInt() = if(this) 1 else 0

	/** Convert as play time into a String
	 * @return String for play time
	 */
	val Float.toTimeStr
		get() =
			if(this<0) "--:--.--" else String.format("%02d:%02d.%02d", this.toInt()/3600, this.toInt()/60%60, (this%60*5f/3f).toInt())
	@Deprecated("Float extended", ReplaceWith("t.toTimeStr", "mu.nu.nullpo.util.GeneralUtil.getTime"))
	fun getTime(t:Float):String = t.toTimeStr

	val Int.toTimeStr
		get() =
			if(this<0) "--:--.--" else String.format("%02d:%02d.%02d", this/3600, this/60%60, (this%60*5f/3f).toInt())
	@Deprecated("Int extended", ReplaceWith("t.toTimeStr", "mu.nu.nullpo.util.GeneralUtil.getTime"))
	fun getTime(t:Int):String = t.toTimeStr
	val Long.toTimeStr
		get() =
			if(this<0) "--:--.--" else String.format("%02d:%02d.%02d", this/3600, this/60%60, (this%60*5f/3f).toInt())

	/** Returns ON if b is true, OFF if b is false
	 * @return ON if b is true, OFF if b is false
	 */
	@JvmOverloads
	fun Boolean.getONorOFF(islong:Boolean = false):String =
		if(this) "\u0083 ${if(islong) "ENABLE" else "ON"}" else "\u0085 ${if(islong) "DISABLE" else "OFF"}"

	/** Returns ○ if b is true, × if b is false
	 * @return ○ if b is true, × if b is false
	 */
	val Boolean.getOX:String get() = if(this) "\u0083" else "\u0085"

	@Deprecated("Bool extended", ReplaceWith("b.getOX", "mu.nu.nullpo.util.GeneralUtil.getOX"))
	fun getOX(b:Boolean) = b.getOX

	/** Get date and time from a Calendar
	 * @return Date and Time String
	 */
	val Calendar.strDateTime:String get() = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(time)

	/** Get date and time from a Calendar with specific TimeZone
	 * @param z TimeZone
	 * @return Date and Time String
	 */
	fun Calendar.strDateTime(z:TimeZone):String = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
		.apply {timeZone = z}.format(this)

	/** Get date from a Calendar
	 * @return Date String
	 */
	val Calendar.strDate:String get() = SimpleDateFormat("yyyy-MM-dd").format(time)

	/** Get date from a Calendar with specific TimeZone
	 * @param z TimeZone
	 * @return Date String
	 */
	fun Calendar.strDate(z:TimeZone):String = SimpleDateFormat("yyyy-MM-dd").apply {timeZone = z}.format(this)

	/** Get time from a Calendar
	 * @return Time String
	 */
	val Calendar.strTime:String get() = SimpleDateFormat("HH:mm:ss").format(time)

	/** Get time from a Calendar with specific TimeZone
	 * @param z TimeZone
	 * @return Time String
	 */
	fun Calendar.strTime(z:TimeZone):String = SimpleDateFormat("HH:mm:ss").apply {timeZone = z}.format(this)

	/** Export a Calendar to a String for saving/sending.
	 * TimeZone is always GMT. Time is based on current time.
	 * @return Calendar String (Each field is separated with a hyphen '-' )
	 */
	val nowGMT get() = Calendar.getInstance(TimeZone.getTimeZone("GMT")).strDateTime
	@Deprecated("extended", ReplaceWith("nowGMT"))
	fun exportCalendarString():String = nowGMT

	/** Export a Calendar to a String for saving/sending.
	 * TimeZone is always GMT.
	 * @return Calendar String (Each field is separated with a hyphen '-' )
	 */
	val Calendar.strGMT:String
		get() = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").apply {timeZone = TimeZone.getTimeZone("GMT")}
			.format(time)

	fun exportCalendarString(c:Calendar):String = c.strGMT

	/** Create a Calendar by using a String that came from exportCalendarString.
	 * TimeZone is always GMT.
	 * @sample String (Each field is separated with a hyphen '-' )
	 * @return Calendar (null if fails)
	 */
	val String.GMTtoDate:Calendar?
		get() {
			val dfm = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").apply {timeZone = TimeZone.getTimeZone("GMT")}

			val c = Calendar.getInstance(TimeZone.getTimeZone("GMT"))

			try {
				val date = dfm.parse(this)
				c.time = date
			} catch(e:Exception) {
				return null
			}

			return c
		}

	fun importCalendarString(s:String):Calendar? = s.GMTtoDate

	/** Get the number of piece types can appear
	 * @param pieceEnable Piece enable flags
	 * @return Number of piece types can appear (In the normal Tetromino games,
	 * it returns 7)
	 */
	fun getNumberOfPiecesCanAppear(pieceEnable:List<Boolean>?):Int {
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
	fun isPieceSZOOnly(pieceEnable:List<Boolean>?):Boolean =
		pieceEnable?.let {it[Piece.PIECE_S]&&it[Piece.PIECE_Z]&&it[Piece.PIECE_O]} ?: false

	/** Create piece ID array from a String
	 * @param strSrc String
	 * @return Piece ID array
	 */
	fun createNextPieceArrayFromNumberString(strSrc:String):List<Int> {
		val len = strSrc.length
		return if(len<1) emptyList() else List(len) {
			var pieceID = Piece.PIECE_I

			try {
				pieceID = Character.getNumericValue(strSrc[it])%Piece.PIECE_STANDARD_COUNT
			} catch(_:NumberFormatException) {
			}

			if(pieceID<0||pieceID>=Piece.PIECE_STANDARD_COUNT) pieceID = Piece.PIECE_I
			pieceID
		}
	}

	/*fun <T:Any?> Class<T>.resource(path:String, fallback:String? = null):String {
		try {
			val f = File(path)
			val originLog = File((this::class.java.getResource(f.name)?.path ?: "").toString())
			if(!f.parentFile.exists()) f.parentFile.mkdirs()
			else if(f.isDirectory) f.deleteRecursively()
			if(!f.exists()) {
				originLog.copyTo(f)
				log.warn("regenerated $path")
			}
		} catch(e:Exception) {
			log.error("resource error about $path", e)
		}
		path
	}*/

	/** Load rule file
	 * @param filename Filename
	 * @return RuleOptions
	 */
	fun loadRule(filename:String):RuleOptions {
		val prop = CustomProperties()

		try {
			val `in` = GZIPInputStream(FileInputStream(filename))
			prop.load(`in`)
			`in`.close()
		} catch(e:Exception) {
			log.warn("Failed to load rule from $filename", e)
		}

		val ruleOpt = RuleOptions()
		ruleOpt.readProperty(prop, 0)

		return ruleOpt
	}

	/** Load Randomizer
	 * @param filename Classpath of the randomizer
	 * @return Randomizer (null if something fails)
	 */
	fun loadRandomizer(filename:String):Randomizer {
		val randomizerClass:Class<*>
		var randomizerObject:Randomizer = MemorylessRandomizer()

		try {
			randomizerClass = Class.forName(filename)
			randomizerObject = randomizerClass.getDeclaredConstructor().newInstance() as Randomizer
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
			wallkickObject = wallkickClass.getDeclaredConstructor().newInstance() as Wallkick
		} catch(e:Exception) {
			log.warn("Failed to load Wallkick from $filename", e)
		}

		return wallkickObject
	}

	/** Load AI
	 * @param filename Classpath of the AI
	 * @return The instance of AI (null if something fails)
	 */
	fun loadAIPlayer(filename:String):AIPlayer? {
		val aiClass:Class<*>
		var aiObject:AIPlayer? = null

		try {
			aiClass = Class.forName(filename)
			aiObject = aiClass.getDeclaredConstructor().newInstance() as AIPlayer
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
	@Deprecated("Kotlin has collecton method", ReplaceWith("strings.drop(startIndex).joinToString(separator)"))
	fun stringCombine(strings:List<String>, separator:String, startIndex:Int):String = strings.drop(1).joinToString(" ")

	fun capsNum(x:Long, digits:Int):String = when {
		digits<=0 -> ""
		x<=0 -> (1 until digits).fold("0") {b, _ -> "${b}0"}
		"$x".length>digits -> {
			val y = (1 until digits).fold(x) {b, _ -> b.div(10)}
			val z = (1 until digits).fold(minOf(y, 35)) {b, _ -> b.times(10)}
			"${('A'.code+minOf(y-10, 25)).toChar()}${if(digits>1) capsNum(x-z, digits-1) else ""}"
		}
		digits>0 -> String.format("%0${digits}d", x)
		else -> ""
	}

	fun capsNum(x:Int, digits:Int):String = capsNum(x.toLong(), digits)
	/**'0'-'9','A'-'Z' represent colors 0-36.
	 * Colors beyond that would follow the ASCII table starting at '['.
	 * */
	val Int.toAlphaNum:Char
		get() = when(this) {
			in 0..9 -> ('0'.code+maxOf(0, this))
			in 10..36 -> ('A'.code+(this-10))
			else -> ('a'.code+(this-37))
		}.toChar()

	/**
	With a radix of 36, the digits encompass '0'-'9','A'-'Z'.
	With a radix higher than 36, we can also have characters 'a'-'z' represent digits.

	Given the current implementation of other functions, I assumed that
	if we needed additional BLOCK_COLOR values, it would follow from 'Z'->'['
	in the ASCII chart.
	 */
	val Char.aNum:Int
		get() = when {
			code>='0'.code -> code-'0'.code
			code>='A'.code -> code-'A'.code
			else -> code-'a'.code
		}

	/** Returns a list containing all elements that are not `null`.*/
	fun <T:Any> Collection<T?>.filterNotNullIndexed():List<IndexedValue<T>> =
		this.mapIndexedNotNull {i, it -> it?.let {IndexedValue(i, it)}}

	/** Returns a list containing all elements that are not `null`.*/
	fun <T:Any> Array<T?>.filterNotNullIndexed():List<IndexedValue<T>> =
		this.mapIndexedNotNull {i, it -> it?.let {IndexedValue(i, it)}}
}
