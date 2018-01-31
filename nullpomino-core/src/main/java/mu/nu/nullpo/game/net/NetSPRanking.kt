package mu.nu.nullpo.game.net

import mu.nu.nullpo.util.CustomProperties
import java.io.Serializable
import java.util.*

/** Single player mode ranking */
class NetSPRanking:Serializable {

	/** Game Mode Name */
	var strModeName:String = ""

	/** Rule Name */
	var strRuleName:String = ""

	/** Game Type ID */
	var gameType:Int = 0

	/** Game Style ID */
	var style:Int = 0

	/** Ranking Type */
	var rankingType:Int = 0

	/** Max number of records (-1:Unlimited) */
	var maxRecords:Int = 0

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
		copy(s)
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

	/** Copy from other NetSPRankingData
	 * @param s Source
	 */
	fun copy(s:NetSPRanking) {
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

	/** Write to a CustomProperties
	 * @param prop CustomProperties
	 */
	fun writeProperty(prop:CustomProperties) {
		val strKey = "spranking.$strRuleName.$strModeName.$gameType."
		prop.setProperty(strKey+"numRecords", listRecord.size)

		for(i in listRecord.indices) {
			val record = listRecord[i]
			val strRecordCompressed = NetUtil.compressString(record.exportString())
			prop.setProperty(strKey+i, strRecordCompressed)
		}
	}

	/** Read from a CustomProperties
	 * @param prop CustomProperties
	 */
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
		 * @return A ranking that is the combination of all of the rankings.
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
