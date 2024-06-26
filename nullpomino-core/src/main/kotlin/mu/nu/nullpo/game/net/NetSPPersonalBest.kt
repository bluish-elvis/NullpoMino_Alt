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

package mu.nu.nullpo.game.net

import mu.nu.nullpo.util.CustomProperties
import java.io.Serializable
import java.util.LinkedList

/** Single player personal record manager */
class NetSPPersonalBest:Serializable {
	/** Player Name */
	var strPlayerName = ""

	/** Records */
	var listRecord:LinkedList<NetSPRecord> = LinkedList()

	/** Constructor */
	constructor() {
		reset()
	}

	/** Copy Constructor
	 * @param s Source
	 */
	constructor(s:NetSPPersonalBest) {
		replace(s)
	}

	/** Constructor that imports data from a String List
	 * @param s String List (String[2])
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
		listRecord = LinkedList()
	}

	/** Copy from [s] other NetSPPersonalBest*/
	fun replace(s:NetSPPersonalBest) {
		strPlayerName = s.strPlayerName
		listRecord = LinkedList()
		for(i in s.listRecord.indices)
			listRecord.add(NetSPRecord(s.listRecord[i]))
	}

	/** Get specific NetSPRecord
	 * @param rule Rule Name
	 * @param mode Mode Name
	 * @param gtype Game Type
	 * @return NetSPRecord (null if not found)
	 */
	fun getRecord(rule:String, mode:String, gtype:Int):NetSPRecord? {
		for(r in listRecord) {
			if(r.strRuleName==rule&&r.strModeName==mode&&r.gameType==gtype) return r
		}
		return null
	}

	/** Checks if r1 is a new record.
	 * @param rtype Ranking Type
	 * @param r1 Newer Record
	 * @return Returns `true` if there are no previous record of this
	 * player, or if the newer record (r1) is better than old one.
	 */
	fun isNewRecord(rtype:Int, r1:NetSPRecord):Boolean {
		val r2 = getRecord(r1.strRuleName, r1.strModeName, r1.gameType) ?: return true
		return r1.compare(rtype, r2)
	}

	/** Register a record.
	 * @param rtype Ranking Type
	 * @param r1 Newer Record
	 * @return Returns `true` if the newer record (r1) is
	 * registered.
	 */
	fun registerRecord(rtype:Int, r1:NetSPRecord):Boolean {
		val r2 = getRecord(r1.strRuleName, r1.strModeName, r1.gameType)

		if(r2!=null) {
			if(r1.compare(rtype, r2))
			// Replace with a new record
				r2.replace(r1)
			else
				return false
		} else
		// Register a new record
			listRecord.add(r1)

		return true
	}

	/** Write CustomProperties to [prop] */
	fun writeProperty(prop:CustomProperties) {
		val strKey = "sppersonal.$strPlayerName."
		prop.setProperty(strKey+"numRecords", listRecord.size)

		for(i in listRecord.indices) {
			val record = listRecord[i]
			val strRecordCompressed = NetUtil.compressString(record.exportString())
			prop.setProperty(strKey+i, strRecordCompressed)
		}
	}

	/** Read CustomProperties from [prop] */
	fun readProperty(prop:CustomProperties) {
		val strKey = "sppersonal.$strPlayerName."
		val numRecords = prop.getProperty(strKey+"numRecords", 0)

		listRecord.clear()
		for(i in 0..<numRecords) {
			val strRecordCompressed = prop.getProperty(strKey+i)
			if(strRecordCompressed!=null) {
				val strRecord = NetUtil.decompressString(strRecordCompressed)
				val record = NetSPRecord(strRecord)
				listRecord.add(record)
			}
		}
	}

	/** Export the records to a String
	 * @return String (Split by ;)
	 */
	fun exportListRecord():String {
		val strResult = StringBuilder()
		for(i in listRecord.indices) {
			if(i>0) strResult.append(";")
			strResult.append(NetUtil.compressString(listRecord[i].exportString()))
		}
		return "$strResult"
	}

	/** Import the record from a String
	 * @param s String (Split by ;)
	 */
	fun importListRecord(s:String) {
		listRecord.clear()

		val array = s.split(Regex(";")).dropLastWhile {it.isEmpty()}
		for(element in array) {
			val strTemp = NetUtil.decompressString(element)
			val record = NetSPRecord(strTemp)
			listRecord.add(record)
		}
	}

	/** Export to a String List
	 * @return String List (String[2])
	 */
	fun exportStringArray():Array<String> =
		arrayOf(
			NetUtil.urlEncode(strPlayerName), exportListRecord()
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
	 * @param s String List (String[8])
	 */
	fun importStringArray(s:List<String>) {
		if(s.isNotEmpty()) strPlayerName = NetUtil.urlDecode(s[0])
		if(s.size>1) importListRecord(s[1])
	}

	/** Import from a String
	 * @param s String (Split by ;)
	 */
	fun importString(s:String) {
		importStringArray(s.split(Regex(";")).dropLastWhile {it.isEmpty()})
	}

}
