package mu.nu.nullpo.game.net

import biz.source_code.base64Coder.Base64Coder
import mu.nu.nullpo.util.GeneralUtil
import org.apache.log4j.Logger
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.util.Calendar
import java.util.TimeZone

class NetServerBan {

	var addr:String = ""

	var startDate:Calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT"))
	var banLength:Int = 0

	/** Returns the end date of the ban, or null if no such date exists (i.e.
	 * permanent).
	 * @return the end date or null
	 */
	val endDate:Calendar?
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

	/** Returns a boolean representing whether or not this NetServerBan is
	 * expired.
	 * @return true if the ban is expired.
	 */
	val isExpired:Boolean
		get() {
			val endDate = endDate
			return if(endDate==null)
				false
			else
				Calendar.getInstance(TimeZone.getTimeZone("GMT")).after(endDate)
		}

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
	fun exportString():String {
		//String strStartDate = exportStartDate();
		val strTemp = GeneralUtil.exportCalendarString(startDate)
		var strStartDate = ""
		if(strTemp!=null) strStartDate = "GMT$strTemp"
		return "$addr;$banLength;$strStartDate"
	}

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
		internal val log = Logger.getLogger(NetServerBan::class.java)

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
/** Creates a new NetServerBan object representing a permanent ban starting
 * now.
 * @param addr the remote address this NetServerBan affects.
 */
