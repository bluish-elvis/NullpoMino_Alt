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

import mu.nu.nullpo.util.CustomProperties
import java.io.Serializable
import java.util.*

/** Single player mode ranking */
class NetSPRanking:Serializable {

	/** Game Mode Name */
	var strModeName = ""

	/** Rule Name */
	var strRuleName = ""

	/** Game Type ID */
	var gameType = 0

	/** Game Style ID */
	var style = 0

	/** Ranking Type */
	var rankingType = 0

	/** Max number of records (-1:Unlimited) */
	var maxRecords = 0

	/** Records */
	var listRecord:LinkedList<NetSPRecord> = LinkedList()

	/** Default Constructor */
	constructor() {
		reset()
	}

	/** Copy Constructor
	 * @param s Source
	 */
	constructor(s:NetSPRanking) {
		replace(s)
	}

	/** Constructor
	 * @param modename Game Mode Name
	 * @param rulename Rule Name
	 * @param gtype Game Type ID
	 * @param style Game Style ID
	 * @param rtype Ranking Type
	 * @param max Max number of records
	 */
	constructor(modename:String, rulename:String, gtype:Int, style:Int, rtype:Int, max:Int) {
		reset()
		strModeName = modename
		strRuleName = rulename
		gameType = gtype
		this.style = style
		rankingType = rtype
		maxRecords = max
	}

	/** Initialization */
	fun reset() {
		strModeName = ""
		strRuleName = ""
		gameType = 0
		style = 0
		rankingType = 0
		maxRecords = 100
		listRecord = LinkedList()
	}

	/** Copy from [s] other NetSPRankingData*/
	fun replace(s:NetSPRanking) {
		strModeName = s.strModeName
		strRuleName = s.strRuleName
		gameType = s.gameType
		style = s.style
		rankingType = s.rankingType
		maxRecords = s.maxRecords
		listRecord = LinkedList()
		for(i in s.listRecord.indices)
			listRecord.add(NetSPRecord(s.listRecord[i]))
	}

	/** Get specific player's record
	 * @param strPlayerName Player Name
	 * @return NetSPRecord (null if not found)
	 */
	fun getRecord(strPlayerName:String):NetSPRecord? {
		val index = indexOf(strPlayerName)
		return if(index==-1) null else listRecord[index]
	}

	/** Get specific player's record
	 * @param pInfo NetPlayerInfo
	 * @return NetSPRecord (null if not found)
	 */
	fun getRecord(pInfo:NetPlayerInfo):NetSPRecord? = getRecord(pInfo.strName)

	/** Get specific player's index
	 * @param strPlayerName Player Name
	 * @return Index (-1 if not found)
	 */
	fun indexOf(strPlayerName:String):Int {
		for(i in listRecord.indices) {
			val r = listRecord[i]
			if(r.strPlayerName==strPlayerName) return i
		}
		return -1
	}

	/** Get specific player's index
	 * @param pInfo NetPlayerInfo
	 * @return Index (-1 if not found)
	 */
	fun indexOf(pInfo:NetPlayerInfo):Int = indexOf(pInfo.strName)

	/** Remove specific player's record
	 * @param strPlayerName Player Name
	 * @return Number of records removed (0 if not found)
	 */
	fun removeRecord(strPlayerName:String):Int {
		var count = 0

		val list = LinkedList(listRecord)
		for(i in list.indices) {
			val r = list[i]

			if(r.strPlayerName==strPlayerName) {
				listRecord.removeAt(i)
				count++
			}
		}

		return count
	}

	/** Remove specific player's record
	 * @param pInfo NetPlayerInfo
	 * @return Number of records removed (0 if not found)
	 */
	fun removeRecord(pInfo:NetPlayerInfo):Int = removeRecord(pInfo.strName)

	/** Checks if r1 is a new record.
	 * @param r1 Newer Record
	 * @return Returns `true` if there are no previous record of this
	 * player, or if the newer record (r1) is better than old one.
	 */
	fun isNewRecord(r1:NetSPRecord):Boolean {
		val r2 = getRecord(r1.strPlayerName) ?: return true
		return r1.compare(rankingType, r2)
	}

	/** Register a new record
	 * @param r1 Record
	 * @return Rank (-1 if out of rank)
	 */
	fun registerRecord(r1:NetSPRecord):Int {
		if(!isNewRecord(r1)) return -1

		// Remove older records
		removeRecord(r1.strPlayerName)

		// Insert new record
		val list = LinkedList(listRecord)
		var rank = -1

		for(i in list.indices)
			if(r1.compare(rankingType, list[i])) {
				listRecord.add(i, r1)
				rank = i
				break
			}

		// Couldn't rank in? Add to last.
		if(rank==-1) {
			listRecord.add(r1)
			rank = listRecord.size-1
		}

		// Remove anything after maxRecords
		while(listRecord.size>=maxRecords)
			listRecord.removeLast()

		// Done
		return if(rank>=maxRecords) -1 else rank
	}

	/** Write CustomProperties to [prop] */
	fun writeProperty(prop:CustomProperties) {
		val strKey = "spranking.$strRuleName.$strModeName.$gameType."
		prop.setProperty(strKey+"numRecords", listRecord.size)

		for(i in listRecord.indices) {
			val record = listRecord[i]
			val strRecordCompressed = NetUtil.compressString(record.exportString())
			prop.setProperty(strKey+i, strRecordCompressed)
		}
	}

	/** Read CustomProperties from [prop] */
	fun readProperty(prop:CustomProperties) {
		val strKey = "spranking.$strRuleName.$strModeName.$gameType."
		var numRecords = prop.getProperty(strKey+"numRecords", 0)
		if(numRecords>maxRecords) numRecords = maxRecords

		listRecord.clear()
		for(i in 0 until numRecords) {
			val strRecordCompressed = prop.getProperty(strKey+i)
			if(strRecordCompressed!=null) {
				val strRecord = NetUtil.decompressString(strRecordCompressed)
				val record = NetSPRecord(strRecord)
				listRecord.add(record)
			}
		}
	}

	companion object {
		/** serialVersionUID for Serialize */
		private const val serialVersionUID = 1L

		/** Condense a list of rankings into a single ranking file.
		 * @param s The list of rankings.
		 * @return A ranking that is the combination of all the rankings.
		 */
		fun mergeRankings(s:LinkedList<NetSPRanking>?):NetSPRanking? {
			if(s.isNullOrEmpty()) return null
			val acc = NetSPRanking(s[0])
			for(r in s)
				for(i in r.listRecord.indices)
					acc.registerRecord(NetSPRecord(r.listRecord[i]))
			return acc
		}
	}
}
