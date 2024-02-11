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

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import mu.nu.nullpo.game.event.Rankable
import org.apache.logging.log4j.LogManager
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.UnsupportedEncodingException
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Enumeration
import java.util.Properties
import java.util.Vector
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import java.util.zip.ZipException

/** String以外も格納できるプロパティセット */
class CustomProperties(name:String = ""):Properties() {
	var fileName = name
		private set
	/** Load from [file].
	 * @return This Properties data you specified, or null if the file doesn't exist.
	 */
	fun load(file:String = fileName):CustomProperties? {
		fileName = file
		log.debug(" $file")
		try {
			val f = GZIPInputStream(FileInputStream(file))
			load(f)
			f.close()
		} catch(e:ZipException) {
			val f = FileInputStream(file)
			load(f)
			f.close()
		} catch(e:FileNotFoundException) {
			log.debug("Not found custom property file: $file")
			return null
		} catch(e:IOException) {
			log.debug("Failed to load custom property file from $file", e)
			return null
		}

		return this
	}
	/** Load from [file].
	 * @return This Properties data you specified, or null if the file doesn't exist.
	 */
	fun loadXML(file:String = fileName, gzip:Boolean = false):CustomProperties? {
		fileName = file
		log.debug("XML $file")
		try {
			FileInputStream(file).let {if(gzip) GZIPInputStream(it) else it}.let {
				loadFromXML(it)
				it.close()
			}
		} catch(e:FileNotFoundException) {
			log.debug("Not found prop XML: $file")
			return null
		} catch(e:IOException) {
			log.debug("Failed to load prop from XML $file", e)
			return null
		}

		return this
	}

	/** Save to [file].
	 * @return true if success
	 */
	fun save(file:String = fileName, xml:Boolean = false):Boolean {
		try {
			val repFolder = File(file).parentFile
			if(!repFolder.exists())
				if(repFolder.mkdirs()) log.info("Created folder: ${repFolder.name}")
				else log.error("Couldn't create folder at ${repFolder.name}")
			val out = GZIPOutputStream(FileOutputStream(file))
			if(xml) storeToXML(out, "NullpoMino Custom Property File", "UTF-8")
			else store(out, "NullpoMino Custom Property File")
			log.debug("Saving custom property file to $file")
			out.close()
		} catch(e:IOException) {
			log.debug("Failed to save custom property file to $file", e)
			return false
		}
		return true
	}
	@Synchronized
	override fun keys():Enumeration<Any> {
		val keysEnum = super.keys()
		val keyList = Vector<String>()
		while(keysEnum.hasMoreElements()) {
			keyList.add(keysEnum.nextElement().toString())
		}
		keyList.sort()
		return elements()
	}

	/** プロパティを設定
	 * @return 指定された[key]に設定されていた値。それがない場合は null
	 */
	inline fun <reified T> setProperty(key:String, value:T):T? = setProperty(key,
		if(value is Rankable) Json.encodeToString(value) else  "$value") as? T
	/** プロパティを設定
	 * @return 指定された[key]に対応する値 (見つからなかったら[def]）
	 */
	@Suppress("IMPLICIT_CAST_TO_ANY")
	inline fun <reified T> getProperty(key:String, def:T):T = try {
		when(def) {
			is Byte -> getProperty(key, def)
			is Int -> getProperty(key, def)
			is Long -> getProperty(key, def)
			is Float -> getProperty(key, def)
			is Double -> getProperty(key, def)
			is Char -> getProperty(key, def)
			is Boolean -> getProperty(key, def)
			is Rankable -> Json.decodeFromString<T>(getProperty(key))
			is List<*> -> if(def is MutableList<*>) getPropertiesMutable(key, def) else getProperties(key, def)
			else -> getProperty(key, "$def")
		} as? T ?: def
	} catch(e:Exception) {
		def
	}

	/** byte型のプロパティを取得
	 * @return 指定された[key]に対応する値 (見つからなかったら[defaultValue]）
	 */
	fun getProperty(key:String, defaultValue:Byte):Byte = try {
		getProperty(key, "$defaultValue").toByte()
	} catch(e:NumberFormatException) {
		defaultValue
	}

	/** short型のプロパティを取得
	 * @return 指定された[key]に対応する値 (見つからなかったら[defaultValue]）
	 */
	fun getProperty(key:String, defaultValue:Short):Short = try {
		getProperty(key, "$defaultValue").toShort()
	} catch(e:NumberFormatException) {
		defaultValue
	}

	/** int型のプロパティを取得
	 * @return 指定された[key]に対応する値 (見つからなかったら[defaultValue]）
	 */
	fun getProperty(key:String, defaultValue:Int):Int = try {
		getProperty(key, "$defaultValue").toInt()
	} catch(e:NumberFormatException) {
		defaultValue
	}

	/** List型のプロパティを取得
	 * @return 指定された[key]に対応するListの値 空Listであってはならない (見つからなかったら[defaultValues]）
	 */
	inline fun <reified T> getProperties(key:String, defaultValues:List<T>) = try {
		getProperty(key, "${defaultValues.joinToString(",")}}").split(',')
			.mapIndexedNotNull {i, it ->
				when(T::class) {
					Byte::class -> it.toByte()
					Int::class -> it.toInt()
					Long::class -> it.toLong()
					Float::class -> it.toFloat()
					Double::class -> it.toDouble()
					Char::class -> it[0]
					Boolean::class -> it.toBoolean()
					else -> it
				} as? T ?: defaultValues.getOrNull(minOf(i, defaultValues.size-1))
			}
	} catch(e:Exception) {
		defaultValues
	}

	inline fun <reified T> getPropertiesMutable(key:String, defaultValues:List<T>) =
		getProperties(key, defaultValues).toMutableList()

	inline fun <reified T> getProperties(key:String, defaultValues:T) = try {
		getProperty(key, "$defaultValues").split(',').map {it as? T ?: defaultValues}
	} catch(e:Exception) {
		listOf(defaultValues)
	}

	inline fun <reified T> getPropertiesMutable(key:String, defaultValues:T) =
		getProperties(key, defaultValues).toMutableList()

	inline fun <reified T> setProperties(key:String, value:List<T>):List<T> =
		setProperty(key, value.joinToString(","))?.let {
			if(it is String) it.split(',').filterIsInstance<T>()
			else null
		} ?: emptyList()

	/** intArray型のプロパティを取得
	 * @return 指定された[key]に対応する値 (見つからなかったら[defaultValue]）
	 */
	fun getProperties(key:String, defaultValue:IntArray):IntArray = try {
		getProperty(key, "$defaultValue").split(',').map {it.toInt()}.toIntArray()
	} catch(e:NumberFormatException) {
		defaultValue
	}

	fun setProperties(key:String, value:IntArray):IntArray? =
		setProperty(key, value.joinToString(","))?.let {
			if(it is String) it.split(',').map {s -> s.toInt()}.toIntArray()
			else null
		}

	/** long型のプロパティを取得
	 * @return 指定された[key]に対応する値 (見つからなかったら[defaultValue]）
	 */
	fun getProperty(key:String, defaultValue:Long):Long = try {
		getProperty(key, "$defaultValue").toLong()
	} catch(e:NumberFormatException) {
		defaultValue
	}

	/** float型のプロパティを取得
	 * @return 指定された[key]に対応する値 (見つからなかったら[defaultValue]）
	 */
	fun getProperty(key:String, defaultValue:Float):Float = try {
		getProperty(key, "$defaultValue").toFloat()
	} catch(e:NumberFormatException) {
		defaultValue
	}

	/** double型のプロパティを取得
	 * @return 指定された[key]に対応する値 (見つからなかったら[defaultValue]）
	 */
	fun getProperty(key:String, defaultValue:Double):Double = try {
		getProperty(key, "$defaultValue").toDouble()
	} catch(e:NumberFormatException) {
		defaultValue
	}

	/** char型のプロパティを取得
	 * @return 指定された[key]に対応する値 (見つからなかったら[defaultValue]）
	 */
	fun getProperty(key:String, defaultValue:Char):Char = try {
		getProperty(key, "$defaultValue")[0]
	} catch(e:Exception) {
		defaultValue
	}

	/** boolean型のプロパティを取得
	 * @return 指定された[key]に対応する値 (見つからなかったら[defaultValue]）
	 */
	fun getProperty(key:String, defaultValue:Boolean):Boolean =
		getProperty(key, "$defaultValue").toBoolean()

	/** このプロパティセットを文字列に変換する(URLEncoderでエンコード)
	 * @param comments 識別コメント
	 * @return URLEncoderでエンコードされたプロパティセット文字列
	 */
	fun encode(comments:String):String? = try {
		val out = ByteArrayOutputStream()
		store(out, comments)
		URLEncoder.encode(out.toString(code.name()), code.name())
	} catch(e:UnsupportedEncodingException) {
		throw Error("UTF-8 not supported", e)
	} catch(e:Exception) {
		e.printStackTrace()
		null
	}

	/** encode(String)でエンコードしたStringからプロパティセットを復元
	 * @param source encode(String)でエンコードしたString
	 * @return 成功するとtrue
	 */
	fun decode(source:String):Boolean = try {
		val decodedString = URLDecoder.decode(source, code.name())
		val prop = ByteArrayInputStream(decodedString.toByteArray(code))
		load(prop)
		true
	} catch(e:UnsupportedEncodingException) {
		throw Error("UTF-8 not supported", e)
	} catch(e:Exception) {
		e.printStackTrace()
		false
	}

	companion object {
		private val log = LogManager.getLogger()
		private val code = StandardCharsets.UTF_8
	}
}
