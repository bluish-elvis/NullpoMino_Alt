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
package mu.nu.nullpo.game.net

import biz.source_code.base64Coder.Base64Coder
import org.cacas.java.gnu.tools.Crypt
import java.io.ByteArrayOutputStream
import java.io.UnsupportedEncodingException
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.zip.*

/** Network utils */
object NetUtil {
	/** Convert byte[] to String (with UTF-8 encoding)
	 * @param bytes Byte array (byte[])
	 * @return String
	 */
	fun bytesToString(bytes:ByteArray):String {
		try {
			return String(bytes, StandardCharsets.UTF_8)
		} catch(e:UnsupportedEncodingException) {
			throw Error("UTF-8 Not Supported", e)
		}

	}

	/** Convert String to byte[] (with UTF-8 encoding)
	 * @param str String
	 * @return Byte array (byte[])
	 */
	fun stringToBytes(str:String):ByteArray {
		try {
			return str.toByteArray(StandardCharsets.UTF_8)
		} catch(e:UnsupportedEncodingException) {
			throw Error("UTF-8 Not Supported", e)
		}

	}

	/** Encode non-URL-safe characters with using URLEncoder
	 * @param str String
	 * @return URLEncoder-encoded String
	 */
	fun urlEncode(str:String):String {
		try {
			return URLEncoder.encode(str, StandardCharsets.UTF_8.name())
		} catch(e:UnsupportedEncodingException) {
			throw Error("UTF-8 Not Supported", e)
		}

	}

	/** Decode URL-safe characters with using URLDecoder
	 * @param str URLEncoder-encoded String
	 * @return Decoded String
	 */
	fun urlDecode(str:String):String {
		try {
			return URLDecoder.decode(str, StandardCharsets.UTF_8.name())
		} catch(e:UnsupportedEncodingException) {
			throw Error("UTF-8 Not Supported", e)
		}

	}

	/** Convert String to byte[] with Shift_JIS encoding
	 * @param s UTF-8 String
	 * @return Shift_JIS encoded byte array (byte[])
	 */
	fun stringToShiftJIS(s:String):ByteArray {

		return try {
			s.toByteArray(charset("Shift_JIS"))
		} catch(e:UnsupportedEncodingException) {
			s.toByteArray()
		}
	}

	/** Convert Shift_JIS byte array (byte[]) to String
	 * @param b Shift_JIS encoded byte array (byte[])
	 * @return UTF-8 String
	 */
	fun shiftJIStoString(b:ByteArray):String {
		return try {
			String(b, charset("Shift_JIS"))
		} catch(e:UnsupportedEncodingException) {
			String(b)
		}
	}

	/** Create Tripcode
	 * @param tripkey Password
	 * @param maxlen Tripcode Length (Usually 10)
	 * @return String of Tripcode
	 */
	fun createTripCode(tripkey:String, maxlen:Int):String {
		val bTripKey = stringToShiftJIS(tripkey)
		val bSaltTemp = ByteArray(bTripKey.size+3)
		System.arraycopy(bTripKey, 0, bSaltTemp, 0, bTripKey.size)
		bSaltTemp[bTripKey.size] = 'H'.code.toByte()
		bSaltTemp[bTripKey.size+1] = '.'.code.toByte()
		bSaltTemp[bTripKey.size+2] = '.'.code.toByte()
		val bSalt = ByteArray(2)
		bSalt[0] = bSaltTemp[1]
		bSalt[1] = bSaltTemp[2]

		for(i in bSalt.indices) {
			if(bSalt[i]<'.'.code.toByte()||bSalt[i]>'z'.code.toByte()) bSalt[i] = '.'.code.toByte()
			if(bSalt[i]==':'.code.toByte()) bSalt[i] = 'A'.code.toByte()
			if(bSalt[i]==';'.code.toByte()) bSalt[i] = 'B'.code.toByte()
			if(bSalt[i]=='<'.code.toByte()) bSalt[i] = 'C'.code.toByte()
			if(bSalt[i]=='='.code.toByte()) bSalt[i] = 'D'.code.toByte()
			if(bSalt[i]=='>'.code.toByte()) bSalt[i] = 'E'.code.toByte()
			if(bSalt[i]=='?'.code.toByte()) bSalt[i] = 'F'.code.toByte()
			if(bSalt[i]=='@'.code.toByte()) bSalt[i] = 'G'.code.toByte()
			if(bSalt[i]=='['.code.toByte()) bSalt[i] = 'a'.code.toByte()
			if(bSalt[i]=='\\'.code.toByte()) bSalt[i] = 'b'.code.toByte()
			if(bSalt[i]==']'.code.toByte()) bSalt[i] = 'c'.code.toByte()
			if(bSalt[i]=='^'.code.toByte()) bSalt[i] = 'd'.code.toByte()
			if(bSalt[i]=='_'.code.toByte()) bSalt[i] = 'e'.code.toByte()
			if(bSalt[i]=='`'.code.toByte()) bSalt[i] = 'f'.code.toByte()
		}

		var strTripCode = Crypt.crypt(bSalt, bTripKey)
		if(strTripCode.length>maxlen) strTripCode = strTripCode.substring(strTripCode.length-maxlen)

		return strTripCode
	}

	/** Compress a byte array (byte[]).<br></br>
	 * [
 * Source](http://www.exampledepot.com/egs/java.util.zip/CompArray.html)
	 * @param input Raw byte array (byte[])
	 * @param level Compression level (0-9)
	 * @return Compressed byte array (byte[])
	 */
	@JvmOverloads
	fun compressByteArray(input:ByteArray, level:Int = Deflater.BEST_COMPRESSION):ByteArray {
		// Create the compressor with highest level of compression
		val compressor = Deflater(level)

		// Give the compressor the data to compress
		compressor.setInput(input)
		compressor.finish()

		// Create an expandable byte array to hold the compressed data.
		// You cannot use an array that's the same size as the orginal because
		// there is no guarantee that the compressed data will be smaller than
		// the uncompressed data.
		val bos = ByteArrayOutputStream(input.size)

		// Compress the data
		val buf = ByteArray(1024)
		while(!compressor.finished()) {
			val count = compressor.deflate(buf)
			bos.write(buf, 0, count)
		}

		// Get the compressed data
		return bos.toByteArray()
	}

	/** Decompress a byte array (byte[])
	 * @param compressedData Compressed byte array (byte[])
	 * @return Raw byte array (byte[])
	 */
	fun decompressByteArray(compressedData:ByteArray):ByteArray {
		// Create the decompressor and give it the data to compress
		val decompressor = Inflater()
		decompressor.setInput(compressedData)

		// Create an expandable byte array to hold the decompressed data
		val bos = ByteArrayOutputStream(compressedData.size)

		// Decompress the data
		val buf = ByteArray(1024)
		while(!decompressor.finished())
			try {
				val count = decompressor.inflate(buf)
				bos.write(buf, 0, count)
			} catch(e:DataFormatException) {
				throw RuntimeException("This byte array is not a valid compressed data", e)
			}

		// Get the decompressed data
		return bos.toByteArray()
	}

	/** Compress a String then encode with Base64.
	 * @param input String you want to compress
	 * @param level Compression level (0-9)
	 * @return Compressed + Base64 encoded String
	 */
	@JvmOverloads
	fun compressString(input:String?, level:Int = Deflater.BEST_COMPRESSION):String {
		val bCompressed = compressByteArray(stringToBytes(input?:""), level)
		val cCompressed = Base64Coder.encode(bCompressed)
		return String(cCompressed)
	}

	/** Decompress a Base64 encoded String
	 * @param input Compressed + Base64 encoded String
	 * @return Raw String
	 */
	fun decompressString(input:String):String {
		val bCompressed = Base64Coder.decode(input)
		val bDecompressed = decompressByteArray(bCompressed)
		return bytesToString(bDecompressed)
	}
}