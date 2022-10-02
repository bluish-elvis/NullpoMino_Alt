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

package mu.nu.nullpo.game.net

import mu.nu.nullpo.game.component.Statistics
import mu.nu.nullpo.util.CustomProperties
import java.io.Serializable
import java.util.LinkedList

/** Single player mode record */
class NetSPRecord:Serializable {

	/** Player Name */
	var strPlayerName = ""

	/** Game Mode Name */
	var strModeName = ""

	/** Rule Name */
	var strRuleName = ""

	/** Main Stats */
	var stats:Statistics? = null

	/** List of custom stats (Each String is NAME;VALUE format) */
	var listCustomStats:LinkedList<String> = LinkedList()

	/** Replay data (Compressed) */
	var strReplayProp = ""

	/** Time stamp (GMT) */
	var strTimeStamp = ""

	/** Game Type ID */
	var gameType = 0

	/** Game Style ID */
	var style = 0

	/** Replay data as CustomProperties that contains replay data */
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
		replace(s)
	}

	/** Constructor that imports data from a String List
	 * @param s String List (String[6])
	 */
	constructor(s:List<String>) {
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

	/** Copy from [s] other NetSPRecord*/
	fun replace(s:NetSPRecord) {
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
		if(s.isNullOrEmpty()) return

		val array = s.split(Regex(",")).dropLastWhile {it.isEmpty()}
		listCustomStats.addAll(array)
	}

	/** Export to a String List
	 * @return String List (String[9])
	 */
	fun exportStringArray():Array<String> = arrayOf(
		NetUtil.urlEncode(strPlayerName), NetUtil.urlEncode(strModeName), NetUtil.urlEncode(strRuleName),
		if(stats==null) "" else NetUtil.compressString(stats!!.exportString()),
		if(listCustomStats.isEmpty()) "" else NetUtil.compressString(exportCustomStats()), strReplayProp, "$gameType",
		"$style", strTimeStamp
	)

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

	/** Import from a String List
	 * @param s String List (String[9])
	 */
	fun importStringArray(s:List<String>) {
		strPlayerName = NetUtil.urlDecode(s[0])
		strModeName = NetUtil.urlDecode(s[1])
		strRuleName = NetUtil.urlDecode(s[2])
		stats = if(s[3].isEmpty()) null else Statistics(NetUtil.decompressString(s[3]))
		if(s[4].isEmpty()) listCustomStats = LinkedList()
		else importCustomStats(NetUtil.decompressString(s[4]))
		strReplayProp = s[5]
		gameType = s[6].toInt()
		style = s[7].toInt()
		strTimeStamp = if(s.size>8) s[8] else ""
	}

	/** Import from a String
	 * @param s String (Split by ;)
	 */
	fun importString(s:String) {
		importStringArray(s.split(Regex(";")).dropLastWhile {it.isEmpty()})
	}

	/** Compare to other NetSPRecord
	 * @param type Ranking Type
	 * @param r2 The other NetSPRecord
	 * @return `true` if this record is better than r2
	 */
	fun compare(type:Int, r2:NetSPRecord):Boolean = compareRecords(type, this, r2)

	/** Set String value of specific custom stat
	 * @param name Custom stat name
	 * @param value Value
	 */
	fun setCustomStat(name:String, value:String) {
		for(i in listCustomStats.indices) {
			val strTemp = listCustomStats[i]
			val strArray = strTemp.split(Regex(";")).dropLastWhile {it.isEmpty()}

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
			val strArray = strTemp.split(Regex(";")).dropLastWhile {it.isEmpty()}

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
		stats?.also {
			if(type!=RANKINGTYPE_GENERIC_SCORE) {
				when(type) {
					RANKINGTYPE_GENERIC_TIME -> {
						strRow += "${it.time},${it.totalPieceLocked},${it.pps}"
					}
					RANKINGTYPE_SCORERACE -> {
						strRow += "${it.time},${it.lines},${it.spl}"
					}
					RANKINGTYPE_DIGRACE -> {
						strRow += "${it.time},${it.lines},${it.totalPieceLocked}"
					}
					RANKINGTYPE_ULTRA -> {
						strRow += "${it.score},${it.lines},${it.totalPieceLocked}"
					}
					RANKINGTYPE_COMBORACE -> {
						strRow += "${it.maxCombo},${it.time},${it.pps}"
					}
					RANKINGTYPE_DIGCHALLENGE -> {
						strRow += "${it.score},${it.lines},${it.time}"
					}
					RANKINGTYPE_TIMEATTACK -> {
						strRow += "${it.lines},${it.time},${it.pps},${it.rollclear}"
					}
				}
			} else {
				strRow += "${it.score},${it.lines},${it.time}"
			}
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
			val s1 = r1.stats ?: return false
			val s2 = r2.stats ?: return false

			return when(type) {
				RANKINGTYPE_GENERIC_SCORE -> (s1.score>s2.score)||
					(s1.score==s2.score&&s1.lines>s2.lines)
					||s1.score==s2.score&&s1.lines==s2.lines&&s1.time<s2.time
				RANKINGTYPE_GENERIC_TIME -> (s1.time<s2.time)
					||(s1.time==s2.time&&s1.totalPieceLocked<s2.totalPieceLocked)
					||s1.time==s2.time&&s1.totalPieceLocked==s2.totalPieceLocked&&s1.pps>s2.pps
				RANKINGTYPE_SCORERACE -> (s1.time<s2.time)
					||(s1.time==s2.time&&s1.lines<s2.lines)
					||s1.time==s2.time&&s1.lines==s2.lines&&s1.spl>s2.spl
				RANKINGTYPE_DIGRACE -> (s1.time<s2.time)
					||(s1.time==s2.time&&s1.lines<s2.lines)
					||s1.time==s2.time&&s1.lines==s2.lines&&s1.totalPieceLocked<s2.totalPieceLocked
				RANKINGTYPE_ULTRA -> (s1.score>s2.score)
					||(s1.score==s2.score&&s1.lines>s2.lines)
					||s1.score==s2.score&&s1.lines==s2.lines&&s1.totalPieceLocked<s2.totalPieceLocked
				RANKINGTYPE_COMBORACE -> (s1.maxCombo>s2.maxCombo)
					||(s1.maxCombo==s2.maxCombo&&s1.time<s2.time)
					||s1.maxCombo==s2.maxCombo&&s1.time==s2.time&&s1.pps>s2.pps
				RANKINGTYPE_DIGCHALLENGE -> (s1.score>s2.score)
					||(s1.score==s2.score&&s1.lines>s2.lines)
					||s1.score==s2.score&&s1.lines==s2.lines&&s1.time>s2.time
				RANKINGTYPE_TIMEATTACK -> {
					// Cap the line count at 150 or 200
					val maxLines = if(r1.gameType>=5) 200 else 150
					val l1 = minOf(s1.lines, maxLines)
					val l2 = minOf(s2.lines, maxLines)

					(s1.rollclear>s2.rollclear)
						||(s1.rollclear==s2.rollclear&&l1>l2)
						||(s1.rollclear==s2.rollclear&&l1==l2&&s1.time<s2.time)
						||s1.rollclear==s2.rollclear&&l1==l2&&s1.time==s2.time&&s1.pps>s2.pps
				}
				else -> return false
			}

		}
	}
}
