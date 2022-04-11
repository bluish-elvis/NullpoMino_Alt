/*
 * Copyright (c) 2010-2021, NullNoname
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

import mu.nu.nullpo.util.GeneralUtil
import mu.nu.nullpo.util.GeneralUtil.strGMT
import org.apache.logging.log4j.LogManager
import java.io.Serializable
import java.util.Calendar

/** Chat message */
class NetChatMessage:Serializable {

	/** User ID */
	var uid = 0

	/** Username */
	var strUserName = ""

	/** Hostname */
	var strHost = ""

	/** Room ID (-1:Lobby) */
	var roomID = 0

	/** Room Name */
	var strRoomName = ""

	/** Timestamp Calendar */
	var timestamp:Calendar? = null

	/** Message Body */
	var strMessage = ""

	/** Default Constructor */
	constructor() {
		reset()
	}

	/** Constructor
	 * @param msg Message
	 */
	constructor(msg:String) {
		reset()
		strMessage = msg
	}

	/** Constructor
	 * @param msg Message
	 * @param pInfo Player Info
	 */
	constructor(msg:String, pInfo:NetPlayerInfo) {
		reset()
		strMessage = msg
		uid = pInfo.uid
		strUserName = pInfo.strName
		strHost = pInfo.strRealHost
	}

	/** Constructor
	 * @param msg Message
	 * @param pInfo Player Info
	 * @param roomInfo Room Info
	 */
	constructor(msg:String, pInfo:NetPlayerInfo, roomInfo:NetRoomInfo) {
		reset()
		strMessage = msg
		uid = pInfo.uid
		strUserName = pInfo.strName
		strHost = pInfo.strRealHost
		roomID = roomInfo.roomID
		strRoomName = roomInfo.strName
	}

	/** Reset to default values */
	fun reset() {
		uid = -1
		strUserName = ""
		strHost = ""
		roomID = -1
		strRoomName = ""
		timestamp = Calendar.getInstance()
		strMessage = ""
	}

	/** Output to Logger */
	fun outputLog() {
		if(roomID==-1)
			log.info("LobbyChat UID:$uid Name:$strUserName Msg:$strMessage")
		else
			log.info("RoomChat Room:$strRoomName UID:$uid Name:$strUserName Msg:$strMessage")
	}

	/** Import from String array
	 * @param s String array (String[7])
	 */
	fun importStringArray(s:Array<String>) {
		uid = s[0].toInt()
		strUserName = NetUtil.urlDecode(s[1])
		strHost = NetUtil.urlDecode(s[2])
		roomID = s[3].toInt()
		strRoomName = NetUtil.urlDecode(s[4])
		timestamp = GeneralUtil.importCalendarString(s[5])
		strMessage = NetUtil.urlDecode(s[6])
	}

	/** Import from String (Divided by ;)
	 * @param str String
	 */
	fun importString(str:String) {
		importStringArray(str.split(";".toRegex()).dropLastWhile {it.isEmpty()}.toTypedArray())
	}

	/** Export to String array
	 * @return String array (String[7])
	 */
	fun exportStringArray():Array<String> = arrayOf(
		"$uid"
		, NetUtil.urlEncode(strUserName)
		, NetUtil.urlEncode(strHost)
		, "$roomID"
		, NetUtil.urlEncode(strRoomName)
		, timestamp!!.strGMT
		, NetUtil.urlEncode(strMessage))

	/** Export to String (Divided by ;)
	 * @return String
	 */
	fun exportString():String {
		val data = exportStringArray()
		val strResult = StringBuilder()

		for(i in data.indices) {
			strResult.append(data[i])
			if(i<data.size-1) strResult.append(";")
		}

		return "$strResult"
	}

	/** Delete this NetChatMessage */
	fun delete() {
		uid = -1
		strUserName = ""
		strHost = ""
		roomID = -1
		strRoomName = ""
		timestamp = null
		strMessage = ""
	}

	companion object {
		/** Serial version */
		private const val serialVersionUID = 1L

		/** Log */
		internal val log = LogManager.getLogger()
	}
}
