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
package mu.nu.nullpo.util

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import mu.nu.nullpo.game.component.RuleOptions
import mu.nu.nullpo.game.subsystem.ai.AIPlayer
import mu.nu.nullpo.game.subsystem.wallkick.Wallkick
import mu.nu.nullpo.tool.ruleeditor.RuleEditor
import net.omegaboshi.nullpomino.game.subsystem.randomizer.MemorylessRandomizer
import net.omegaboshi.nullpomino.game.subsystem.randomizer.Randomizer
import org.apache.logging.log4j.LogManager
import java.io.FileInputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.GZIPInputStream
import java.util.zip.ZipException

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

	/** @return true -> 1 , false -> 0 */
	fun Boolean.toInt() = if(this) 1 else 0

	operator fun Int.plus(x:Boolean) = if(x) this+1 else this
	operator fun Float.plus(x:Boolean) = if(x) this+1 else this
	operator fun Boolean.plus(x:Int) = if(this) x+1 else x
	operator fun Boolean.minus(x:Int) = if(this) x-1 else x
	operator fun Boolean.times(x:Int) = if(this) x else 0

	fun Triple<Float, Float, Float>.HSBtoRGB():Triple<Float, Float, Float> {
		val (h, s, b) = this
		val i = (h*6).toInt()
		val f = h*6-i
		val p = b*(1-s)
		val q = b*(1-f*s)
		val t = b*(1-(1-f)*s)
		return when(i%6) {
			0 -> Triple(b, t, p)
			1 -> Triple(q, b, p)
			2 -> Triple(p, b, t)
			3 -> Triple(p, q, b)
			4 -> Triple(t, p, b)
			else -> Triple(b, p, q)
		}
	}
	/** Convert as play time into a String
	 * @return String for play time
	 */
	val Float.toTimeStr
		get() = if(this<0) "--:--.--" else
			"%02d:%02d.%02d".format(this.toInt()/3600, this.toInt()/60%60, (this%60*5f/3f).toInt())
	@Deprecated("Float extended", ReplaceWith("t.toTimeStr", "mu.nu.nullpo.util.GeneralUtil.getTime"))
	fun getTime(t:Float):String = t.toTimeStr

	val Int.toTimeStr
		get() = if(this<0) "--:--.--" else
			"%02d:%02d.%02d".format(this/3600, this/60%60, (this%60*5f/3f).toInt())
	@Deprecated("Int extended", ReplaceWith("t.toTimeStr", "mu.nu.nullpo.util.GeneralUtil.getTime"))
	fun getTime(t:Int):String = t.toTimeStr
	val Long.toTimeStr
		get() = if(this<0) "--:--.--" else
			"%02d:%02d.%02d".format(this/3600, this/60%60, (this%60*5f/3f).toInt())

	/** @return true -> ON , false -> OFF */
	@JvmOverloads
	fun Boolean.getONorOFF(isLong:Boolean = false):String =
		if(this) "\u0083 ${if(isLong) "ENABLE" else "ON"}" else "\u0085 ${if(isLong) "DISABLE" else "OFF"}"

	/** @return true-> ○ , false -> ×*/
	val Boolean.getOX:String get() = if(this) "\u0083" else "\u0085"

	@Deprecated("Bool extended", ReplaceWith("b.getOX", "mu.nu.nullpo.util.GeneralUtil.getOX"))
	fun getOX(b:Boolean) = b.getOX

	/** @return Date and Time String */
	val Calendar.strDateTime:String get() = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(time)

	/** @return Date and Time String in [z] TimeZone */
	fun Calendar.strDateTime(z:TimeZone):String = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
		.apply {timeZone = z}.format(this)

	/** @return Date String */
	val Calendar.strDate:String get() = SimpleDateFormat("yyyy-MM-dd").format(time)

	/** @return Date and Time String in [z] TimeZone */
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
	 * @return Calendar String, "yyyy-MM-dd HH:mm:ss"
	 */
	val nowGMT get() = Calendar.getInstance(TimeZone.getTimeZone("GMT")).strDateTime
	@Deprecated("extended", ReplaceWith("nowGMT"))
	fun exportCalendarString():String = nowGMT

	/** Export a Calendar to a String for saving/sending.
	 * TimeZone is always GMT.
	 * @return Calendar String, "yyyy-MM-dd HH:mm:ss"
	 */
	val Calendar.strGMT:String
		get() = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").apply {timeZone = TimeZone.getTimeZone("GMT")}
			.format(time)

	fun exportCalendarString(c:Calendar):String = c.strGMT

	/** Create a Calendar by using a String that came from exportCalendarString.
	 * TimeZone is always GMT.
	 * @return Calendar, null if fails
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
	fun loadRule(filename:String?):RuleOptions {
		if(filename.isNullOrEmpty()) return RuleOptions()
		log.info("Load rule options from $filename")
		return try {
			val rf = try {
				GZIPInputStream(FileInputStream(filename))
			} catch(_:ZipException) {
				FileInputStream(filename)
			}
			Json.decodeFromString<RuleOptions>(rf.bufferedReader().use {it.readText()})
		} catch(_:Exception) {
			val rf = try {
				GZIPInputStream(FileInputStream(filename))
			} catch(_:ZipException) {
				FileInputStream(filename)
			}
			val prop = CustomProperties()
			prop.load(rf)
			rf.close()

			val ruleOpt = RuleOptions()
			ruleOpt.readProperty(prop)

			RuleEditor.log.debug("Loaded rule file from $filename")
			RuleEditor.log.debug(Json.encodeToString(ruleOpt))

			ruleOpt
		}
	}

	/** Load Randomizer
	 * @param filename Classpath of the randomizer
	 * @return Randomizer (null if something fails)
	 */
	fun loadRandomizer(filename:String):Randomizer {
		var randomizerObject:Randomizer = MemorylessRandomizer()

		try {
			val randomizerClass = Class.forName(filename)
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
		x<=0 -> (1..<digits).fold("0") {b, _ -> "${b}0"}
		"$x".length>digits -> {
			val y = (1..<digits).fold(x) {b, _ -> b.div(10)}
			val z = (1..<digits).fold(minOf(y, 35)) {b, _ -> b.times(10)}
			"${('A'.code+minOf(y.toInt()-10, 25)).toChar()}${if(digits>1) capsNum(x-z, digits-1) else ""}"
		}
		digits>0 -> "%0${digits}d".format(x)
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
			code>='a'.code -> code-('a'.code)+36
			code>='A'.code -> code-('A'.code)+10
			else -> code-'0'.code
		}

	/** Returns a list containing all elements that are not `null`.*/
	fun <T:Any> Collection<T?>.filterNotNullIndexed():List<IndexedValue<T>> =
		this.mapIndexedNotNull {i, it -> it?.let {IndexedValue(i, it)}}

	/** Returns a list containing all elements that are not `null`.*/
	fun <T:Any> Array<T?>.filterNotNullIndexed():List<IndexedValue<T>> =
		this.mapIndexedNotNull {i, it -> it?.let {IndexedValue(i, it)}}

	@OptIn(ExperimentalSerializationApi::class)
	val Json = Json {
		coerceInputValues = true
		encodeDefaults = false
		explicitNulls = false
		ignoreUnknownKeys = true
		decodeEnumsCaseInsensitive = true
		/*	serializersModule = SerializersModule {
					polymorphic(Any::class) {
						PolymorphicModuleBuilder.subclass((Rankable.ScoreRow)::class)
					}
				}*/
	}

//	fun <T> listSerializer(serializerForT:KSerializer<T>):KSerializer<List<T>> = listOf(serializerForT).serializer()

}
