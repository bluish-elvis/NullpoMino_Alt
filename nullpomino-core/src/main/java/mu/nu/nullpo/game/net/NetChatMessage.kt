package mu.nu.nullpo.game.net

import mu.nu.nullpo.util.GeneralUtil
import org.apache.log4j.Logger
import java.io.Serializable
import java.util.*

/** Chat message */
class NetChatMessage:Serializable {

	/** User ID */
	var uid:Int = 0

	/** Username */
	var strUserName:String = ""

	/** Hostname */
	var strHost:String = ""

	/** Room ID (-1:Lobby) */
	var roomID:Int = 0

	/** Room Name */
	var strRoomName:String = ""

	/** Timestamp Calendar */
	var timestamp:Calendar? = null

	/** Message Body */
	var strMessage:String = ""

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
		uid = Integer.parseInt(s[0])
		strUserName = NetUtil.urlDecode(s[1])
		strHost = NetUtil.urlDecode(s[2])
		roomID = Integer.parseInt(s[3])
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
		, GeneralUtil.exportCalendarString(timestamp!!)
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
		internal val log = Logger.getLogger(NetChatMessage::class.java)
	}
}
