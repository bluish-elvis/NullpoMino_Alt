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

import mu.nu.nullpo.game.component.RuleOptions
import mu.nu.nullpo.game.play.GameEngine
import java.io.Serializable
import java.nio.channels.SocketChannel

/** Player information */
class NetPlayerInfo:Serializable {

	/** Name */
	var strName = ""

	/** Country code */
	var strCountry = ""

	/** Host */
	var strHost = ""

	/** Team name */
	var strTeam = ""

	/** Rules in use */
	var ruleOpt:RuleOptions? = null

	/** Multiplayer rating */
	val rating = IntArray(GameEngine.MAX_GAMESTYLE)

	/** Rating backup (internal use) */
	val ratingBefore = IntArray(GameEngine.MAX_GAMESTYLE)

	/** Number of rated multiplayer games played */
	val playCount = IntArray(GameEngine.MAX_GAMESTYLE)

	/** Number of games played in current room */
	var playCountNow = 0

	/** Number of rated multiplayer games win */
	val winCount = IntArray(GameEngine.MAX_GAMESTYLE)

	/** Number of wins in current room */
	var winCountNow = 0

	/** Single player personal records */
	var spPersonalBest = NetSPPersonalBest()

	/** User ID */
	var uid = -1

	/** Current room ID */
	var roomID = -1

	/** Game seat number (-1 if spectator) */
	var seatID = -1

	/** Join queue number (-1 if not in queue) */
	var queueID = -1

	/** True if "Ready" sign */
	var ready = false

	/** True if playing now */
	var playing = false

	/** True if connected */
	var connected = false

	/** True if this player is using tripcode */
	var isTripUse = false

	/** Real host name (for internal use) */
	var strRealHost = ""

	/** Real IP (for internal use) */
	var strRealIP = ""

	/** SocketChannel of this player (for internal use) */
	var channel:SocketChannel? = null

	/** Constructor */
	constructor()

	/** Copy constructor
	 * @param n Copy source
	 */
	constructor(n:NetPlayerInfo) {
		replace(n)
	}

	/** String list constructor (Uses importStringArray)
	 * @param pdata String list (String[12])
	 */
	constructor(pdata:List<String>) {
		importStringArray(pdata)
	}

	/** String constructor (Uses importString)
	 * @param str String(Divided by ;)
	 */
	constructor(str:String) {
		importString(str)
	}

	/** Copy from [n] other NetPlayerInfo */
	fun replace(n:NetPlayerInfo) {
		strName = n.strName
		strCountry = n.strCountry
		strHost = n.strHost
		strTeam = n.strTeam

		ruleOpt = if(n.ruleOpt!=null)
			RuleOptions(n.ruleOpt)
		else
			null

		for(i in 0 until GameEngine.MAX_GAMESTYLE) {
			rating[i] = n.rating[i]
			ratingBefore[i] = n.ratingBefore[i]
			playCount[i] = n.playCount[i]
			winCount[i] = n.winCount[i]
		}
		spPersonalBest = NetSPPersonalBest(n.spPersonalBest)

		playCountNow = n.playCountNow
		winCountNow = n.winCountNow

		uid = n.uid
		roomID = n.roomID
		seatID = n.seatID
		queueID = n.queueID
		ready = n.ready
		playing = n.playing
		connected = n.connected
		isTripUse = n.isTripUse
		strRealHost = n.strRealHost
		strRealIP = n.strRealIP
		channel = n.channel
	}

	/** Import from String list
	 * @param pdata String list (String[27])
	 */
	fun importStringArray(pdata:List<String>) {
		strName = NetUtil.urlDecode(pdata[0])
		strCountry = NetUtil.urlDecode(pdata[1])
		strHost = NetUtil.urlDecode(pdata[2])
		strTeam = NetUtil.urlDecode(pdata[3])
		roomID = pdata[4].toInt()
		uid = pdata[5].toInt()
		seatID = pdata[6].toInt()
		queueID = pdata[7].toInt()
		ready = pdata[8].toBoolean()
		playing = (pdata[9]).toBoolean()
		connected = (pdata[10]).toBoolean()
		isTripUse = (pdata[11]).toBoolean()
		rating[0] = pdata[12].toInt()
		rating[1] = pdata[13].toInt()
		rating[2] = pdata[14].toInt()
		rating[3] = pdata[15].toInt()
		playCount[0] = pdata[16].toInt()
		playCount[1] = pdata[17].toInt()
		playCount[2] = pdata[18].toInt()
		playCount[3] = pdata[19].toInt()
		winCount[0] = pdata[20].toInt()
		winCount[1] = pdata[21].toInt()
		winCount[2] = pdata[22].toInt()
		winCount[3] = pdata[23].toInt()
		if(pdata.size>24) spPersonalBest.importString(NetUtil.decompressString(pdata[24]))
		if(pdata.size>25) playCountNow = pdata[25].toInt()
		if(pdata.size>26) winCountNow = pdata[26].toInt()
	}

	/** Import from String (Divided by ;)
	 * @param str String
	 */
	fun importString(str:String) {
		importStringArray(str.split(Regex(";")).dropLastWhile {it.isEmpty()})
	}

	/** Export to String list
	 * @return String list (String[27])
	 */
	fun exportStringArray():List<String> = listOf(
		NetUtil.urlEncode(strName),
		NetUtil.urlEncode(strCountry),
		NetUtil.urlEncode(strHost),
		NetUtil.urlEncode(strTeam),
		"$roomID",
		"$uid",
		"$seatID",
		"$queueID",
		"$ready",
		"$playing",
		"$connected",
		"$isTripUse",
		"${rating[0]}",
		"${rating[1]}",
		"${rating[2]}",
		"${rating[3]}",
		"${playCount[0]}",
		"${playCount[1]}",
		"${playCount[2]}",
		"${playCount[3]}",
		"${winCount[0]}",
		"${winCount[1]}",
		"${winCount[2]}",
		"${winCount[3]}",
		NetUtil.compressString(spPersonalBest.exportString()),
		"$playCountNow",
		"$winCountNow"
	)

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

	/** Reset play flags */
	fun resetPlayState() {
		ready = false
		playing = false
	}

	/** Delete this player */
	fun delete() {
		ruleOpt = null
	}

	companion object {
		/** Serial version */
		private const val serialVersionUID = 1L

		/** Default rating for multiplayer games */
		const val DEFAULT_MULTIPLAYER_RATING = 1500
	}
}
