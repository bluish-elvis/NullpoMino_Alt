package mu.nu.nullpo.game.net

import mu.nu.nullpo.game.component.Statistics
import mu.nu.nullpo.util.CustomProperties
import java.io.Serializable
import java.util.*

/** Single player mode record */
class NetSPRecord:Serializable {

	/** Player Name */
	var strPlayerName:String=""

	/** Game Mode Name */
	var strModeName:String=""

	/** Rule Name */
	var strRuleName:String=""

	/** Main Stats */
	var stats:Statistics? = null

	/** List of custom stats (Each String is NAME;VALUE format) */
	var listCustomStats:LinkedList<String> = LinkedList()

	/** Replay data (Compressed) */
	var strReplayProp:String=""

	/** Time stamp (GMT) */
	var strTimeStamp:String=""

	/** Game Type ID */
	var gameType:Int = 0

	/** Game Style ID */
	var style:Int = 0

	/** Get replay data as CustomProperties
	 * @return CustomProperties that contains replay data
	 */
	/** Set replay data from CustomProperties
	 * @param p CustomProperties that contains replay data
	 */
	var replayProp:CustomProperties
		get() {
			val strEncode = NetUtil.decompressString(strReplayProp)
			val p = CustomProperties()
			p.decode(strEncode)
			return p
		}
		set(p) {
			val strEncode = p.encode("NullpoMino Net Single Player Replay ($strPlayerName)")
			strReplayProp = NetUtil.compressString(strEncode)
		}

	/** Default Constructor */
	constructor() {
		reset()
	}

	/** Copy Constructor
	 * @param s Source
	 */
	constructor(s:NetSPRecord) {
		copy(s)
	}

	/** Constructor that imports data from a String Array
	 * @param s String Array (String[6])
	 */
	constructor(s:Array<String>) {
		importStringArray(s)
	}

	/** Constructor that imports data from a String
	 * @param s String (Split by ;)
	 */
	constructor(s:String) {
		importString(s)
	}

	/** Initialization */
	fun reset() {
		strPlayerName = ""
		strModeName = ""
		strRuleName = ""
		stats = null
		listCustomStats = LinkedList()
		strReplayProp = ""
		strTimeStamp = ""
		gameType = 0
		style = 0
	}

	/** Copy from other NetSPRecord
	 * @param s Source
	 */
	fun copy(s:NetSPRecord) {
		strPlayerName = s.strPlayerName
		strModeName = s.strModeName
		strRuleName = s.strRuleName

		stats = Statistics(s.stats)

		listCustomStats = LinkedList(s.listCustomStats)

		strReplayProp = s.strReplayProp
		strTimeStamp = s.strTimeStamp
		gameType = s.gameType
		style = s.style
	}

	/** Export custom stats to a String
	 * @return String (Split by ,)
	 */
	fun exportCustomStats():String {
		if(listCustomStats.isNotEmpty()) {
			val strResult = StringBuilder()
			for(i in listCustomStats.indices) {
				if(i>0) strResult.append(",")
				strResult.append(listCustomStats[i])
			}
			return "$strResult"
		}
		return ""
	}

	/** Import custom stats from a String
	 * @param s String (Split by ,)
	 */
	fun importCustomStats(s:String?) {
		listCustomStats.clear()
		if(s==null||s.isEmpty()) return

		val array = s.split(",".toRegex()).dropLastWhile {it.isEmpty()}.toTypedArray()
		Collections.addAll(listCustomStats, *array)
	}

	/** Export to a String Array
	 * @return String Array (String[9])
	 */
	fun exportStringArray():Array<String> = arrayOf(
	NetUtil.urlEncode(strPlayerName)
	,NetUtil.urlEncode(strModeName)
	,NetUtil.urlEncode(strRuleName)
	,if(stats==null) "" else NetUtil.compressString(stats!!.exportString())
	,if(listCustomStats.isNullOrEmpty()) "" else NetUtil.compressString(exportCustomStats())
	, strReplayProp
	, "$gameType"
	, "$style"
	, strTimeStamp)

	/** Export to a String
	 * @return String (Split by ;)
	 */
	fun exportString():String {
		val array = exportStringArray()
		val result = StringBuilder()

		for(i in array.indices) {
			if(i>0) result.append(";")
			result.append(array[i])
		}

		return "$result"
	}

	/** Import from a String Array
	 * @param s String Array (String[9])
	 */
	fun importStringArray(s:Array<String>) {
		strPlayerName = NetUtil.urlDecode(s[0])
		strModeName = NetUtil.urlDecode(s[1])
		strRuleName = NetUtil.urlDecode(s[2])
		stats = if(s[3].isEmpty()) null else Statistics(NetUtil.decompressString(s[3]))
		if(s[4].isEmpty()) listCustomStats = LinkedList()
		else importCustomStats(NetUtil.decompressString(s[4]))
		strReplayProp = s[5]
		gameType = Integer.parseInt(s[6])
		style = Integer.parseInt(s[7])
		strTimeStamp = if(s.size>8) s[8] else ""
	}

	/** Import from a String
	 * @param s String (Split by ;)
	 */
	fun importString(s:String) {
		importStringArray(s.split(";".toRegex()).dropLastWhile {it.isEmpty()}.toTypedArray())
	}

	/** Compare to other NetSPRecord
	 * @param type Ranking Type
	 * @param r2 The other NetSPRecord
	 * @return `true` if this this record is better than r2
	 */
	fun compare(type:Int, r2:NetSPRecord):Boolean = compareRecords(type, this, r2)

	/** Set String value of specific custom stat
	 * @param name Custom stat name
	 * @param value Value
	 */
	fun setCustomStat(name:String, value:String) {
		for(i in listCustomStats.indices) {
			val strTemp = listCustomStats[i]
			val strArray = strTemp.split(";".toRegex()).dropLastWhile {it.isEmpty()}.toTypedArray()

			if(strArray[0]==name) {
				listCustomStats[i] = "$name;$value"
				return
			}
		}
		listCustomStats.add("$name;$value")
	}

	/** Get String value of specific custom stat
	 * @param name Custom stat name
	 * @return Value (null if not found)
	 */
	fun getCustomStat(name:String):String? {
		for(strTemp in listCustomStats) {
			val strArray = strTemp.split(";".toRegex()).dropLastWhile {it.isEmpty()}.toTypedArray()

			if(strArray[0]==name) return strArray[1]
		}
		return null
	}

	/** Get String value of specific custom stat
	 * @param name Custom stat name
	 * @param strDefault Default value (used when the name is not found)
	 * @return Value (strDefault if not found)
	 */
	fun getCustomStat(name:String, strDefault:String):String {
		val strResult = getCustomStat(name)
		return strResult ?: strDefault
	}

	/** Get a short String of stats of the record (used by NetServer)
	 * @param type Ranking Type
	 * @return Short String of stats of the record
	 */
	fun getStatRow(type:Int):String {
		var strRow = ""

		if(type!=RANKINGTYPE_GENERIC_SCORE) {
			if(type==RANKINGTYPE_GENERIC_TIME) {
				strRow += stats!!.time.toString()+","
				strRow += stats!!.totalPieceLocked.toString()+","
				strRow += stats!!.pps
			} else if(type==RANKINGTYPE_SCORERACE) {
				strRow += stats!!.time.toString()+","
				strRow += stats!!.lines.toString()+","
				strRow += stats!!.spl
			} else if(type==RANKINGTYPE_DIGRACE) {
				strRow += stats!!.time.toString()+","
				strRow += stats!!.lines.toString()+","
				strRow += stats!!.totalPieceLocked
			} else if(type==RANKINGTYPE_ULTRA) {
				strRow += stats!!.score.toString()+","
				strRow += stats!!.lines.toString()+","
				strRow += stats!!.totalPieceLocked
			} else if(type==RANKINGTYPE_COMBORACE) {
				strRow += stats!!.maxCombo.toString()+","
				strRow += stats!!.time.toString()+","
				strRow += stats!!.pps
			} else if(type==RANKINGTYPE_DIGCHALLENGE) {
				strRow += stats!!.score.toString()+","
				strRow += stats!!.lines.toString()+","
				strRow += stats!!.time
			} else if(type==RANKINGTYPE_TIMEATTACK) {
				strRow += stats!!.lines.toString()+","
				strRow += stats!!.time.toString()+","
				strRow += stats!!.pps.toString()+","
				strRow += stats!!.rollclear
			}
		} else {
			strRow += stats!!.score.toString()+","
			strRow += stats!!.lines.toString()+","
			strRow += stats!!.time
		}

		return strRow
	}

	companion object {
		/** serialVersionUID for Serialize */
		private const val serialVersionUID = 1L

		/** Ranking type constants */
		const val RANKINGTYPE_GENERIC_SCORE = 0
		const val RANKINGTYPE_GENERIC_TIME = 1
		const val RANKINGTYPE_SCORERACE = 2
		const val RANKINGTYPE_DIGRACE = 3
		const val RANKINGTYPE_ULTRA = 4
		const val RANKINGTYPE_COMBORACE = 5
		const val RANKINGTYPE_DIGCHALLENGE = 6
		const val RANKINGTYPE_TIMEATTACK = 7

		/** Compare 2 records
		 * @param type Ranking Type
		 * @param r1 Record 1
		 * @param r2 Record 2
		 * @return `true` if r1 is better than r2
		 */
		fun compareRecords(type:Int, r1:NetSPRecord, r2:NetSPRecord):Boolean {
			val s1 = r1.stats
			val s2 = r2.stats

			if(type==RANKINGTYPE_GENERIC_SCORE) {
				return if(s1!!.score>s2!!.score)
					true
				else if(s1.score==s2.score&&s1.lines>s2.lines)
					true
				else
					s1.score==s2.score&&s1.lines==s2.lines&&s1.time<s2.time
			} else if(type==RANKINGTYPE_GENERIC_TIME) {
				return if(s1!!.time<s2!!.time)
					true
				else if(s1.time==s2.time&&s1.totalPieceLocked<s2.totalPieceLocked)
					true
				else
					s1.time==s2.time&&s1.totalPieceLocked==s2.totalPieceLocked&&s1.pps>s2.pps
			} else if(type==RANKINGTYPE_SCORERACE) {
				return if(s1!!.time<s2!!.time)
					true
				else if(s1.time==s2.time&&s1.lines<s2.lines)
					true
				else
					s1.time==s2.time&&s1.lines==s2.lines&&s1.spl>s2.spl
			} else if(type==RANKINGTYPE_DIGRACE) {
				return if(s1!!.time<s2!!.time)
					true
				else if(s1.time==s2.time&&s1.lines<s2.lines)
					true
				else
					s1.time==s2.time&&s1.lines==s2.lines&&s1.totalPieceLocked<s2.totalPieceLocked
			} else if(type==RANKINGTYPE_ULTRA) {
				return if(s1!!.score>s2!!.score)
					true
				else if(s1.score==s2.score&&s1.lines>s2.lines)
					true
				else
					s1.score==s2.score&&s1.lines==s2.lines&&s1.totalPieceLocked<s2.totalPieceLocked
			} else if(type==RANKINGTYPE_COMBORACE) {
				return if(s1!!.maxCombo>s2!!.maxCombo)
					true
				else if(s1.maxCombo==s2.maxCombo&&s1.time<s2.time)
					true
				else
					s1.maxCombo==s2.maxCombo&&s1.time==s2.time&&s1.pps>s2.pps
			} else if(type==RANKINGTYPE_DIGCHALLENGE) {
				return if(s1!!.score>s2!!.score)
					true
				else if(s1.score==s2.score&&s1.lines>s2.lines)
					true
				else
					s1.score==s2.score&&s1.lines==s2.lines&&s1.time>s2.time
			} else if(type==RANKINGTYPE_TIMEATTACK) {
				// Cap the line count at 150 or 200
				val maxLines = if(r1.gameType>=5) 200 else 150
				val l1 = minOf(s1!!.lines, maxLines)
				val l2 = minOf(s2!!.lines, maxLines)

				return if(s1.rollclear>s2.rollclear)
					true
				else if(s1.rollclear==s2.rollclear&&l1>l2)
					true
				else if(s1.rollclear==s2.rollclear&&l1==l2&&s1.time<s2.time)
					true
				else
					s1.rollclear==s2.rollclear&&l1==l2&&s1.time==s2.time&&s1.pps>s2.pps
			}

			return false
		}
	}
}
