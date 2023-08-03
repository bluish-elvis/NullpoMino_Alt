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
import java.io.Serializable
import java.util.LinkedList

/** ルーム情報 */
class NetRoomInfo:Serializable {
	/** 識別 number */
	var roomID = -1

	/** ルーム名 */
	var strName = ""

	/** 参加可能なMaximum人count */
	var maxPlayers = 6

	/** 自動開始までの待機 time */
	var autoStartSeconds = 0

	/** 落下速度(分子) */
	var gravity = 1

	/** 落下速度(分母) */
	var denominator = 60

	/** ARE */
	var are = 30

	/** ARE after line clear */
	var areLine = 30

	/** Line clear time */
	var lineDelay = 40

	/** 固定 time */
	var lockDelay = 30

	/** DAS */
	var das = 14

	/** Flag for types of Twisters allowed (0=none, 1=normal, 2=all spin) */
	var twistEnableType = 1

	/** Allow EZ-spins in spinCheckType 2 */
	var twistEnableEZ = false

	/** Flag for enabling B2B */
	var b2b = true

	var splitb2b = false

	/** b2b adds as a separate garbage chunk */
	var b2bChunk = false

	/** Flag for enabling combos */
	var combo = true

	/** Allow Rensa/Combo Block */
	var rensaBlock = true

	/** Allow garbage countering */
	var counter = true

	/** Enable bravo bonus */
	var bravo = true

	/** ルール固定 flag */
	var ruleLock = false

	/** Rule name */
	var ruleName = ""

	/** ルール */
	var ruleOpt:RuleOptions = RuleOptions()

	/** 参加しているNumber of players */
	var playerSeatedCount = 0

	/** 観戦中の人のcount */
	var spectatorCount = 0

	/** ルームにいる人全員のカウント(参戦中+観戦中) */
	var playerListCount = 0

	/** ゲーム中 flag */
	var playing = false

	/** Start game直後のNumber of players */
	var startPlayers = 0

	/** 死亡カウント */
	var deaths = 0

	/** Automatically start timerが動いているときはtrue */
	var autoStartActive = false

	/** 誰かOK表示を出したあとCancelしたらtrue */
	var isSomeoneCancelled = false

	/** 3人以上生きている場合に Attack 力を減らす */
	var reduceLineSend = false

	/** Rate of change of garbage holes */
	var messiness = 100

	/** Hole change style (false=line true=attack) */
	var garbageChangePerAttack = true

	/** Divide change rate by number of live players/teams to mimic feel of
	 * 1v1 */
	var divideChangeRateByPlayers = false

	/** Garbage send type (false=Send to all, true=Target) */
	var isTarget = false

	/** Targeting time */
	var targetTimer = 60

	//public boolean useTankMode = false;

	/** Hurryup開始までの秒count(-1でHurryupなし) */
	var hurryUpSeconds = -1

	/** Hurryup後に何回Blockを置くたびに床をせり上げるか */
	var hurryUpInterval = 5

	/** Automatically start timer type(false=NullpoMino true=TNET2) */
	var autoStartTNET2 = false

	/** 誰かOK表示を出したあとCancelしたらTimer無効化 */
	var disableTimerAfterSomeoneCancelled = false

	/** Map is enabled */
	var useMap = false

	/** 前回のMap */
	var mapPrevious = -1

	/** 新しい断片的garbage blockシステムを使う */
	var useFractionalGarbage = false

	/** Mode name */
	var strMode = ""

	/** Single player flag */
	var singleplayer = false

	/** Rated-game flag */
	var rated = false

	/** Custom rated-game flag */
	var customRated = false

	/** Game style */
	var style = 0

	/** マップリスト */
	val mapList = LinkedList<String>()

	/** ルームにいる人のリスト */
	val playerList = LinkedList<NetPlayerInfo>()

	/** ゲーム席 */
	val playerSeat = LinkedList<NetPlayerInfo>()

	/** ゲーム席(Start game時にのみ更新・新しい人が入ってきたり誰かが出ていったりしても変わりません) */
	val playerSeatNowPlaying = LinkedList<NetPlayerInfo>()

	/** 待ち行列 */
	val playerQueue = LinkedList<NetPlayerInfo>()

	/** Dead player list (Pushed from front, winner will be the first entry) */
	val playerSeatDead = LinkedList<NetPlayerInfo>()

	/** Chat messages */
	val chatList = LinkedList<NetChatMessage>()

	/** 今ゲーム席にいる人のcountをcountえる(null席はカウントしない)
	 * @return 今ゲーム席にいる人のcount
	 */
	val numberOfPlayerSeated:Int
		get() = playerSeat.size

	/** 何人のPlayerが準備完了したかcountえる
	 * @return 準備完了したNumber of players
	 */
	val howManyPlayersReady:Int
		get() = playerSeat.count {it.ready}

	/** 何人のPlayerがプレイ中かcountえる(死んだ人とまだ部屋に来た直後の人は含みません)
	 * @return プレイ中のNumber of players
	 */
	val howManyPlayersPlaying:Int
		get() = playerSeatNowPlaying.count {it.playing&&playerSeat.contains(it)}

	/** 最後に生き残ったPlayerの情報を取得
	 * @return 最後に生き残ったPlayerの情報(まだ2人以上生きている場合や,そもそもゲームが始まっていない場合はnull)
	 */
	val winner:NetPlayerInfo?
		get() = if(startPlayers>=2&&howManyPlayersPlaying<2&&playing)
			playerSeatNowPlaying.first {it.playing&&it.connected&&playerSeat.contains(it)}
		else null

	/** 最後に生き残ったTeam nameを取得
	 * @return 最後に生き残ったTeam name
	 */
	val winnerTeam:String?
		get() = if(startPlayers>=2&&howManyPlayersPlaying>=2&&playing)
			playerSeatNowPlaying.first {it.playing&&it.connected&&playerSeat.contains(it)}.strTeam
		else null

	/** @return 1つのチームだけが生き残っている場合にtrue
	 */
	val isTeamWin:Boolean
		get() = winnerTeam!=null

	/** @return true if it's a team game
	 */
	val isTeamGame:Boolean
		get() {
			val teamList = LinkedList<String>()

			if(startPlayers>=2)
				playerSeatNowPlaying
					.filter {it.strTeam.isNotEmpty()}
					.forEach {
						if(teamList.contains(it.strTeam))
							return true
						teamList.add(it.strTeam)
					}

			return false
		}

	/** Constructor */
	constructor()

	/** Copy constructor
	 * @param n Copy source
	 */
	constructor(n:NetRoomInfo) {
		replace(n)
	}

	/** Stringの配列から data代入するConstructor
	 * @param rdata Stringの配列(String[7])
	 */
	constructor(rdata:List<String>) {
		importStringArray(rdata)
	}

	/** Stringから data代入するConstructor
	 * @param str String
	 */
	constructor(str:String) {
		importString(str)
	}

	/** 他のNetRoomInfo[n]からコピー*/
	fun replace(n:NetRoomInfo) {
		roomID = n.roomID
		strName = n.strName
		maxPlayers = n.maxPlayers
		autoStartSeconds = n.autoStartSeconds
		gravity = n.gravity
		denominator = n.denominator
		are = n.are
		areLine = n.areLine
		lineDelay = n.lineDelay
		lockDelay = n.lockDelay
		das = n.das
		twistEnableType = n.twistEnableType
		twistEnableEZ = n.twistEnableEZ
		b2b = n.b2b
		b2bChunk = n.b2bChunk
		combo = n.combo
		rensaBlock = n.rensaBlock
		counter = n.counter
		bravo = n.bravo

		ruleLock = n.ruleLock
		ruleName = n.ruleName
		ruleOpt = RuleOptions(n.ruleOpt)

		playerSeatedCount = n.playerSeatedCount
		spectatorCount = n.spectatorCount
		playerListCount = n.playerListCount
		playing = n.playing
		startPlayers = n.startPlayers
		deaths = n.deaths
		autoStartActive = n.autoStartActive
		isSomeoneCancelled = n.isSomeoneCancelled
		reduceLineSend = n.reduceLineSend
		hurryUpSeconds = n.hurryUpSeconds
		hurryUpInterval = n.hurryUpInterval
		autoStartTNET2 = n.autoStartTNET2
		disableTimerAfterSomeoneCancelled = n.disableTimerAfterSomeoneCancelled
		useMap = n.useMap
		mapPrevious = n.mapPrevious
		useFractionalGarbage = n.useFractionalGarbage
		garbageChangePerAttack = n.garbageChangePerAttack
		messiness = n.messiness
		divideChangeRateByPlayers = n.divideChangeRateByPlayers
		isTarget = n.isTarget
		targetTimer = n.targetTimer
		//useTankMode = n.useTankMode;
		strMode = n.strMode
		singleplayer = n.singleplayer
		rated = n.rated
		customRated = n.customRated
		style = n.style

		mapList.clear()
		mapList.addAll(n.mapList)
		playerList.clear()
		playerList.addAll(n.playerList)
		playerSeat.clear()
		playerSeat.addAll(n.playerSeat)
		playerSeatNowPlaying.clear()
		playerSeatNowPlaying.addAll(n.playerSeatNowPlaying)
		playerQueue.clear()
		playerQueue.addAll(n.playerQueue)
		playerSeatDead.clear()
		playerSeatDead.addAll(n.playerSeatDead)
		chatList.clear()
		chatList.addAll(n.chatList)
	}

	/** Stringの配列から data代入(Playerリスト除く)
	 * @param rdata Stringの配列(String[43])
	 */
	private fun importStringArray(rdata:List<String>) {
		roomID = rdata[0].toInt()
		strName = NetUtil.urlDecode(rdata[1])
		maxPlayers = rdata[2].toInt()
		playerSeatedCount = rdata[3].toInt()
		spectatorCount = rdata[4].toInt()
		playerListCount = rdata[5].toInt()
		playing = rdata[6].toBoolean()
		ruleLock = rdata[7].toBoolean()
		ruleName = NetUtil.urlDecode(rdata[8])
		autoStartSeconds = rdata[9].toInt()
		gravity = rdata[10].toInt()
		denominator = rdata[11].toInt()
		are = rdata[12].toInt()
		areLine = rdata[13].toInt()
		lineDelay = rdata[14].toInt()
		lockDelay = rdata[15].toInt()
		das = rdata[16].toInt()
		twistEnableType = rdata[17].toInt()
		b2b = rdata[18].toBoolean()
		combo = rdata[19].toBoolean()
		rensaBlock = rdata[20].toBoolean()
		counter = rdata[21].toBoolean()
		bravo = rdata[22].toBoolean()
		reduceLineSend = rdata[23].toBoolean()
		hurryUpSeconds = rdata[24].toInt()
		hurryUpInterval = rdata[25].toInt()
		autoStartTNET2 = rdata[26].toBoolean()
		disableTimerAfterSomeoneCancelled = rdata[27].toBoolean()
		useMap = rdata[28].toBoolean()
		useFractionalGarbage = rdata[29].toBoolean()
		garbageChangePerAttack = rdata[30].toBoolean()
		messiness = rdata[31].toInt()
		twistEnableEZ = rdata[33].toBoolean()
		b2bChunk = rdata[34].toBoolean()
		strMode = NetUtil.urlDecode(rdata[35])
		singleplayer = rdata[36].toBoolean()
		rated = rdata[37].toBoolean()
		customRated = rdata[38].toBoolean()
		style = rdata[39].toInt()
		divideChangeRateByPlayers = rdata[40].toBoolean()
		if(rdata.size>41) isTarget = rdata[41].toBoolean()
		if(rdata.size>42) targetTimer = rdata[42].toInt()
		//useTankMode = Boolean.parseBoolean(rdata[43]);
	}

	/** String(;で区切り)から data代入(Playerリスト除く)
	 * @param str String
	 */
	fun importString(str:String) {
		importStringArray(str.split(Regex(";")).dropLastWhile {it.isEmpty()})
	}

	/** Stringの配列に変換(Playerリスト除く)
	 * @return Stringの配列(String[40])
	 */
	private fun exportStringArray():List<String> =
		listOf(
			"$roomID", NetUtil.urlEncode(strName), "$maxPlayers", "$playerSeatedCount", "$spectatorCount", "$playerListCount",
			"$playing", "$ruleLock", NetUtil.urlEncode(ruleName), "$autoStartSeconds", "$gravity", "$denominator", "$are", "$areLine",
			"$lineDelay", "$lockDelay", "$das", "$twistEnableType", "$b2b", "$combo", "$rensaBlock", "$counter", "$bravo",
			"$reduceLineSend", "$hurryUpSeconds", "$hurryUpInterval", "$autoStartTNET2", "$disableTimerAfterSomeoneCancelled",
			"$useMap", "$useFractionalGarbage", "$garbageChangePerAttack", "$messiness", "", "$twistEnableEZ",
			"$b2bChunk", NetUtil.urlEncode(strMode), "$singleplayer", "$rated", "$customRated", "$style",
			"$divideChangeRateByPlayers"//,"$useTankMode"
		)

	/** Stringに変換(;で区切り)(Playerリスト除く)
	 * @return String
	 */
	fun exportString():String {
		val data = exportStringArray()
		val strResult = StringBuilder()

		data.indices.forEach {i ->
			strResult.append(data[i])
			if(i<data.size-1) strResult.append(";")
		}

		return "$strResult"
	}

	/** Number of playersカウントを更新 */
	fun updatePlayerCount() {
		playerSeatedCount = numberOfPlayerSeated
		playerListCount = playerList.size
		spectatorCount = playerListCount-playerSeatedCount
	}

	/** 指定したPlayerがゲーム席にいるかどうか調べる
	 * @param pInfo Player
	 * @return 指定したPlayerがゲーム席にいるならtrue
	 */
	fun isPlayerInSeat(pInfo:NetPlayerInfo):Boolean = playerSeat.contains(pInfo)

	/** 指定したPlayerがどの numberのゲーム席にいるか調べる
	 * @param pInfo Player
	 * @return ゲーム席 number(いないなら-1)
	 */
	fun getPlayerSeatNumber(pInfo:NetPlayerInfo):Int = playerSeat.indices.firstOrNull {playerSeat[it]===pInfo} ?: -1

	/** @return 順番待ちなしですぐにゲーム席に入れるならtrue
	 */
	fun canJoinSeat():Boolean = numberOfPlayerSeated<maxPlayers

	/** ゲーム席に入る
	 * @param pInfo Player
	 * @return ゲーム席の number(満員だったら-1)
	 */
	fun joinSeat(pInfo:NetPlayerInfo):Int {
		if(canJoinSeat()) {
			exitQueue(pInfo)
			playerSeat.add(pInfo)
			return playerSeat.size-1
		}
		return -1
	}

	/** 指定したPlayerをゲーム席から外す
	 * @param pInfo Player
	 */
	fun exitSeat(pInfo:NetPlayerInfo) {
		playerSeat.remove(pInfo)
	}

	/** 順番待ちに入る
	 * @param pInfo Player
	 * @return 順番待ち number
	 */
	fun joinQueue(pInfo:NetPlayerInfo):Int {
		if(playerQueue.contains(pInfo)) return playerQueue.indexOf(pInfo)
		playerQueue.add(pInfo)
		return playerQueue.size-1
	}

	/** 指定したPlayerを順番待ちから外す
	 * @param pInfo Player
	 */
	fun exitQueue(pInfo:NetPlayerInfo) {
		playerQueue.remove(pInfo)
	}

	/** @return true if 2 or more people have same IP
	 */
	fun hasSameIPPlayers():Boolean {
		val ipList = LinkedList<String>()

		if(startPlayers>=2)
			playerSeatNowPlaying
				.filter {it.strRealIP.isNotEmpty()}
				.forEach {
					if(ipList.contains(it.strRealIP))
						return@hasSameIPPlayers true
					ipList.add(it.strRealIP)
				}

		return false
	}

	/** Start game時に呼び出す処理 */
	fun gameStart() {
		updatePlayerCount()
		playerSeatNowPlaying.clear()
		playerSeatNowPlaying.addAll(playerSeat)
		playerSeatDead.clear()
		chatList.clear()
		startPlayers = playerSeatedCount
		deaths = 0
		autoStartActive = false
		isSomeoneCancelled = false
	}

	/** ルーム消去時の処理 */
	fun delete() {
		ruleOpt.reset()
		mapList.clear()
		playerList.clear()
		playerSeat.clear()
		playerSeatNowPlaying.clear()
		playerQueue.clear()
		playerSeatDead.clear()
		chatList.clear()
	}

	companion object {
		/** Spin detection type */
		const val SPINTYPE_4POINT = 0
		const val SPINTYPE_IMMOBILE = 1
	}
}
