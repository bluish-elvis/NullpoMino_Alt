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

import java.io.*
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.*

/** String以外も格納できるプロパティセット */
class CustomProperties:Properties() {

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
	 * @param key キー
	 * @param value keyに対応する変数
	 * @return プロパティリストの指定されたキーの前の値。それがない場合は null
	 */
	@Synchronized
	fun <T:Comparable<T>> setProperty(key:String, value:Comparable<T>):Any? = when(value) {
		is Byte -> setProperty(key, "$value")
		is Int -> setProperty(key, "$value")
		is Long -> setProperty(key, "$value")
		is Float -> setProperty(key, "$value")
		is Double -> setProperty(key, "$value")
		is Char -> setProperty(key, "$value")
		is Boolean -> setProperty(key, "$value")
		else -> setProperty(key, "$value")
	}

	/** byte型のプロパティを取得
	 * @param key キー
	 * @param defaultValue keyが見つからない場合に返す変数
	 * @return 指定されたキーに対応する整数 (見つからなかったらdefaultValue）
	 */
	fun getProperty(key:String, defaultValue:Byte):Byte = try {
		getProperty(key, "$defaultValue").toByte()
	} catch(e:NumberFormatException) {
		defaultValue
	}

	/** short型のプロパティを取得
	 * @param key キー
	 * @param defaultValue keyが見つからない場合に返す変数
	 * @return 指定されたキーに対応する整数 (見つからなかったらdefaultValue）
	 */
	fun getProperty(key:String, defaultValue:Short):Short = try {
		getProperty(key, "$defaultValue").toShort()
	} catch(e:NumberFormatException) {
		defaultValue
	}

	/** int型のプロパティを取得
	 * @param key キー
	 * @param defaultValue keyが見つからない場合に返す変数
	 * @return 指定されたキーに対応する整数 (見つからなかったらdefaultValue）
	 */
	fun getProperty(key:String, defaultValue:Int):Int = try {
		getProperty(key, "$defaultValue").toInt()
	} catch(e:NumberFormatException) {
		defaultValue
	}

	/** intArray型のプロパティを取得
	 * @param key キー
	 * @param defaultValue keyが見つからない場合に返す変数
	 * @return 指定されたキーに対応する整数 (見つからなかったらdefaultValue）
	 */
	fun getProperty(key:String, defaultValue:IntArray):IntArray = try {
		getProperty(key, "$defaultValue").split(',').map {it.toInt()}.toIntArray()
	} catch(e:NumberFormatException) {
		defaultValue
	}

	fun setProperty(key:String, value:IntArray):Any =
		setProperty(key, value.joinToString(separator = ","))

	/** long型のプロパティを取得
	 * @param key キー
	 * @param defaultValue keyが見つからない場合に返す変数
	 * @return 指定されたキーに対応する整数 (見つからなかったらdefaultValue）
	 */
	fun getProperty(key:String, defaultValue:Long):Long = try {
		getProperty(key, "$defaultValue").toLong()
	} catch(e:NumberFormatException) {
		defaultValue
	}

	/** float型のプロパティを取得
	 * @param key キー
	 * @param defaultValue keyが見つからない場合に返す変数
	 * @return 指定されたキーに対応する整数 (見つからなかったらdefaultValue）
	 */
	fun getProperty(key:String, defaultValue:Float):Float = try {
		getProperty(key, "$defaultValue").toFloat()
	} catch(e:NumberFormatException) {
		defaultValue
	}

	/** double型のプロパティを取得
	 * @param key キー
	 * @param defaultValue keyが見つからない場合に返す変数
	 * @return 指定されたキーに対応する整数 (見つからなかったらdefaultValue）
	 */
	fun getProperty(key:String, defaultValue:Double):Double = try {
		getProperty(key, "$defaultValue").toDouble()
	} catch(e:NumberFormatException) {
		defaultValue
	}

	/** char型のプロパティを取得
	 * @param key キー
	 * @param defaultValue keyが見つからない場合に返す変数
	 * @return 指定されたキーに対応する整数 (見つからなかったらdefaultValue）
	 */
	fun getProperty(key:String, defaultValue:Char):Char = try {
		getProperty(key, "$defaultValue")[0]
	} catch(e:Exception) {
		defaultValue
	}

	/** boolean型のプロパティを取得
	 * @param key キー
	 * @param defaultValue keyが見つからない場合に返す変数
	 * @return 指定されたキーに対応するboolean型変数 (見つからなかったらdefaultValue）
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

		/** Serial version */
		private const val serialVersionUID = 2L

		private val code = StandardCharsets.UTF_8
	}
}
