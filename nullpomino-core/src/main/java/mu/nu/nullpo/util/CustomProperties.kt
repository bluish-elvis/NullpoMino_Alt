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

	/** byte型のプロパティを設定
	 * @param key キー
	 * @param value keyに対応する変数
	 * @return プロパティリストの指定されたキーの前の値。それがない場合は null
	 */
	@Synchronized
	fun setProperty(key:String, value:Byte):Any? = setProperty(key, "$value")

	/** short型のプロパティを設定
	 * @param key キー
	 * @param value keyに対応する変数
	 * @return プロパティリストの指定されたキーの前の値。それがない場合は null
	 */
	@Synchronized
	fun setProperty(key:String, value:Short):Any? = setProperty(key, "$value")

	/** int型のプロパティを設定
	 * @param key キー
	 * @param value keyに対応する変数
	 * @return プロパティリストの指定されたキーの前の値。それがない場合は null
	 */
	@Synchronized
	fun setProperty(key:String, value:Int):Any? = setProperty(key, "$value")

	/** long型のプロパティを設定
	 * @param key キー
	 * @param value keyに対応する変数
	 * @return プロパティリストの指定されたキーの前の値。それがない場合は null
	 */
	@Synchronized
	fun setProperty(key:String, value:Long):Any? = setProperty(key, "$value")

	/** float型のプロパティを設定
	 * @param key キー
	 * @param value keyに対応する変数
	 * @return プロパティリストの指定されたキーの前の値。それがない場合は null
	 */
	@Synchronized
	fun setProperty(key:String, value:Float):Any? = setProperty(key, "$value")

	/** double型のプロパティを設定
	 * @param key キー
	 * @param value keyに対応する変数
	 * @return プロパティリストの指定されたキーの前の値。それがない場合は null
	 */
	@Synchronized
	fun setProperty(key:String, value:Double):Any? = setProperty(key, "$value")

	/** char型のプロパティを設定
	 * @param key キー
	 * @param value keyに対応する変数
	 * @return プロパティリストの指定されたキーの前の値。それがない場合は null
	 */
	@Synchronized
	fun setProperty(key:String, value:Char):Any? = setProperty(key, "$value")

	/** boolean型のプロパティを設定
	 * @param key キー
	 * @param value keyに対応する変数
	 * @return プロパティリストの指定されたキーの前の値。それがない場合は null
	 */
	@Synchronized
	fun setProperty(key:String, value:Boolean):Any? = setProperty(key, "$value")

	/** byte型のプロパティを取得
	 * @param key キー
	 * @param defaultValue keyが見つからない場合に返す変数
	 * @return 指定されたキーに対応する整数 (見つからなかったらdefaultValue）
	 */
	fun getProperty(key:String, defaultValue:Byte):Byte = try {
		java.lang.Byte.parseByte(getProperty(key, defaultValue.toString()))
	} catch(e:NumberFormatException) {
		defaultValue
	}

	/** short型のプロパティを取得
	 * @param key キー
	 * @param defaultValue keyが見つからない場合に返す変数
	 * @return 指定されたキーに対応する整数 (見つからなかったらdefaultValue）
	 */
	fun getProperty(key:String, defaultValue:Short):Short = try {
		java.lang.Short.parseShort(getProperty(key, defaultValue.toString()))
	} catch(e:NumberFormatException) {
		defaultValue
	}

	/** int型のプロパティを取得
	 * @param key キー
	 * @param defaultValue keyが見つからない場合に返す変数
	 * @return 指定されたキーに対応する整数 (見つからなかったらdefaultValue）
	 */
	fun getProperty(key:String, defaultValue:Int):Int = try {
		Integer.parseInt(getProperty(key, defaultValue.toString()))
	} catch(e:NumberFormatException) {
		defaultValue
	}

	/** long型のプロパティを取得
	 * @param key キー
	 * @param defaultValue keyが見つからない場合に返す変数
	 * @return 指定されたキーに対応する整数 (見つからなかったらdefaultValue）
	 */
	fun getProperty(key:String, defaultValue:Long):Long = try {
		java.lang.Long.parseLong(getProperty(key, defaultValue.toString()))
	} catch(e:NumberFormatException) {
		defaultValue
	}

	/** float型のプロパティを取得
	 * @param key キー
	 * @param defaultValue keyが見つからない場合に返す変数
	 * @return 指定されたキーに対応する整数 (見つからなかったらdefaultValue）
	 */
	fun getProperty(key:String, defaultValue:Float):Float = try {
		java.lang.Float.parseFloat(getProperty(key, defaultValue.toString()))
	} catch(e:NumberFormatException) {
		defaultValue
	}

	/** double型のプロパティを取得
	 * @param key キー
	 * @param defaultValue keyが見つからない場合に返す変数
	 * @return 指定されたキーに対応する整数 (見つからなかったらdefaultValue）
	 */
	fun getProperty(key:String, defaultValue:Double):Double {

		return try {
			java.lang.Double.parseDouble(getProperty(key, defaultValue.toString()))
		} catch(e:NumberFormatException) {
			defaultValue
		}
	}

	/** char型のプロパティを取得
	 * @param key キー
	 * @param defaultValue keyが見つからない場合に返す変数
	 * @return 指定されたキーに対応する整数 (見つからなかったらdefaultValue）
	 */
	fun getProperty(key:String, defaultValue:Char):Char = try {
		getProperty(key, defaultValue.toString())[0]
	} catch(e:Exception) {
		defaultValue
	}

	/** boolean型のプロパティを取得
	 * @param key キー
	 * @param defaultValue keyが見つからない場合に返す変数
	 * @return 指定されたキーに対応するboolean型変数 (見つからなかったらdefaultValue）
	 */
	fun getProperty(key:String, defaultValue:Boolean):Boolean =
		java.lang.Boolean.valueOf(getProperty(key, java.lang.Boolean.toString(defaultValue)))

	/** このプロパティセットを文字列に変換する(URLEncoderでエンコード)
	 * @param comments 識別コメント
	 * @return URLEncoderでエンコードされたプロパティセット文字列
	 */
	fun encode(comments:String):String? {
		var result:String? = null

		try {
			val out = ByteArrayOutputStream()
			store(out, comments)
			result = URLEncoder.encode(out.toString(code.name()), code.name())
		} catch(e:UnsupportedEncodingException) {
			throw Error("UTF-8 not supported", e)
		} catch(e:Exception) {
			e.printStackTrace()
		}

		return result
	}

	/** encode(String)でエンコードしたStringからプロパティセットを復元
	 * @param source encode(String)でエンコードしたString
	 * @return 成功するとtrue
	 */
	fun decode(source:String):Boolean {
		try {
			val decodedString = URLDecoder.decode(source, code.name())
			val `in` = ByteArrayInputStream(decodedString.toByteArray(code))
			load(`in`)
		} catch(e:UnsupportedEncodingException) {
			throw Error("UTF-8 not supported", e)
		} catch(e:Exception) {
			e.printStackTrace()
			return false
		}

		return true
	}

	companion object {

		/** Serial version */
		private const val serialVersionUID = 2L

		private val code = StandardCharsets.UTF_8
	}
}
