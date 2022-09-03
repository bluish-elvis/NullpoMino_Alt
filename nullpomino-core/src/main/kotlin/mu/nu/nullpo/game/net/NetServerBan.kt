/*
 * Copyright (c) 2010-2022, NullNoname
 * Kotlin converted and modified by Venom=Nhelv
 * All rights reserved.
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

package mu.nu.nullpo.game.net

import biz.source_code.base64Coder.Base64Coder
import mu.nu.nullpo.util.GeneralUtil
import mu.nu.nullpo.util.GeneralUtil.strGMT
import org.apache.logging.log4j.LogManager
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.util.Calendar
import java.util.TimeZone

class NetServerBan {

	var addr = ""

	var startDate:Calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT"))
	var banLength = 0

	/** Returns the end date of the ban, or null if no such date exists (i.e.
	 * permanent).
	 * @return the end date or null
	 */
	val endDate:Calendar
		get() {
			return (startDate.clone() as Calendar).also {
				when(banLength) {
					BANLENGTH_1HOUR -> it.add(Calendar.HOUR, 1)
					BANLENGTH_6HOURS -> it.add(Calendar.HOUR, 6)
					BANLENGTH_24HOURS -> it.add(Calendar.HOUR, 24)
					BANLENGTH_1WEEK -> it.add(Calendar.WEEK_OF_MONTH, 1)
					BANLENGTH_1MONTH -> it.add(Calendar.MONTH, 1)
					BANLENGTH_1YEAR -> it.add(Calendar.YEAR, 1)
				}
			}
		}

	/** Returns a boolean representing whether this NetServerBan is expired.
	 * @return true if the ban is expired.
	 */
	val isExpired:Boolean
		get() = Calendar.getInstance(TimeZone.getTimeZone("GMT")).after(endDate)

	/** Empty Constructor */
	constructor()

	/** Creates a new NetServerBan object representing a ban starting now.
	 * @param addr the remote address this NetServerBan affects.
	 * @param banLength an integer representing the length of the ban.
	 */
	@JvmOverloads
	constructor(addr:String, banLength:Int = BANLENGTH_PERMANENT) {
		this.addr = addr
		startDate = Calendar.getInstance(TimeZone.getTimeZone("GMT"))
		this.banLength = banLength
	}

	/** Export startDate to String
	 * @return String (null if fails)
	 */
	fun exportStartDate():String? {
		try {
			val bout = ByteArrayOutputStream()
			val oos = ObjectOutputStream(bout)
			oos.writeObject(startDate)
			val bTemp = NetUtil.compressByteArray(bout.toByteArray())
			val cTemp = Base64Coder.encode(bTemp)
			return String(cTemp)
		} catch(e:Exception) {
			log.error("Failed to export startDate", e)
		}

		return null
	}

	/** Import startDate from String
	 * @param strInput String
	 * @return true if success
	 */
	fun importStartDate(strInput:String):Boolean {
		try {
			if(strInput.startsWith("GMT")) {
				// GMT String
				val c = GeneralUtil.importCalendarString(strInput.substring(3))
				if(c!=null) {
					startDate = c
					return true
				}
			} else {
				// Object Stream
				val bTemp = Base64Coder.decode(strInput)
				val bTemp2 = NetUtil.decompressByteArray(bTemp)
				val bin = ByteArrayInputStream(bTemp2)
				val oin = ObjectInputStream(bin)
				startDate = oin.readObject() as Calendar
				return true
			}
		} catch(e:Exception) {
			log.error("Failed to import startDate", e)
		}

		return false
	}

	/** Export to String
	 * @return String
	 */
	fun exportString():String = //String strStartDate = exportStartDate();
		"$addr;$banLength;GMT${startDate.strGMT}"

	/** Import from String
	 * @param strInput String
	 */
	fun importString(strInput:String) {
		val strArray = strInput.split(";".toRegex()).dropLastWhile {it.isEmpty()}.toTypedArray()
		addr = strArray[0]
		banLength = strArray[1].toInt()
		importStartDate(strArray[2])
	}

	companion object {
		internal val log = LogManager.getLogger()

		const val BANLENGTH_1HOUR = 0
		const val BANLENGTH_6HOURS = 1
		const val BANLENGTH_24HOURS = 2
		const val BANLENGTH_1WEEK = 3
		const val BANLENGTH_1MONTH = 4
		const val BANLENGTH_1YEAR = 5
		const val BANLENGTH_PERMANENT = 6

		const val BANLENGTH_TOTAL = 7
	}
}
