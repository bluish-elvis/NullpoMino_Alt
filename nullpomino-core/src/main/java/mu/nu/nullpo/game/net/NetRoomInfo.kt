/* Copyright (c) 2010, NullNoname
 * All rights reserved. */
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

	var spinCheckType = SPINTYPE_4POINT

	/** Allow EZ-spins in spinCheckType 2 */
	var twistEnableEZ = false

	/** Flag for enabling B2B */
	var b2b = true

	/** b2b adds as a separate garbage chunk */
	var b2bChunk:Boolean = false

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
	var ruleOpt:RuleOptions? = null

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
	var deadCount = 0

	/** Automatically start timerが動いているときはtrue */
	var autoStartActive = false

	/** 誰かOK表示を出したあとCancelしたらtrue */
	var isSomeoneCancelled = false

	/** 3人以上生きている場合に Attack 力を減らす */
	var reduceLineSend = false

	/** Rate of change of garbage holes */
	var garbagePercent = 100

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
	var hurryupSeconds = -1

	/** Hurryup後に何回Blockを置くたびに床をせり上げるか */
	var hurryupInterval = 5

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
						else teamList.add(it.strTeam)
					}

			return false
		}

	/** Constructor */
	constructor()

	/** Copy constructor
	 * @param n Copy source
	 */
	constructor(n:NetRoomInfo) {
		copy(n)
	}

	/** Stringの配列から data代入するConstructor
	 * @param rdata Stringの配列(String[7])
	 */
	constructor(rdata:Array<String>) {
		importStringArray(rdata)
	}

	/** Stringから data代入するConstructor
	 * @param str String
	 */
	constructor(str:String) {
		importString(str)
	}

	/** 他のNetRoomInfoからコピー
	 * @param n Copy source
	 */
	fun copy(n:NetRoomInfo) {
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
		spinCheckType = n.spinCheckType
		twistEnableEZ = n.twistEnableEZ
		b2b = n.b2b
		b2bChunk = n.b2bChunk
		combo = n.combo
		rensaBlock = n.rensaBlock
		counter = n.counter
		bravo = n.bravo

		ruleLock = n.ruleLock
		ruleName = n.ruleName
		ruleOpt = if(n.ruleOpt!=null)
			RuleOptions(n.ruleOpt)
		else
			null

		playerSeatedCount = n.playerSeatedCount
		spectatorCount = n.spectatorCount
		playerListCount = n.playerListCount
		playing = n.playing
		startPlayers = n.startPlayers
		deadCount = n.deadCount
		autoStartActive = n.autoStartActive
		isSomeoneCancelled = n.isSomeoneCancelled
		reduceLineSend = n.reduceLineSend
		hurryupSeconds = n.hurryupSeconds
		hurryupInterval = n.hurryupInterval
		autoStartTNET2 = n.autoStartTNET2
		disableTimerAfterSomeoneCancelled = n.disableTimerAfterSomeoneCancelled
		useMap = n.useMap
		mapPrevious = n.mapPrevious
		useFractionalGarbage = n.useFractionalGarbage
		garbageChangePerAttack = n.garbageChangePerAttack
		garbagePercent = n.garbagePercent
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
	fun importStringArray(rdata:Array<String>) {
		roomID = Integer.parseInt(rdata[0])
		strName = NetUtil.urlDecode(rdata[1])
		maxPlayers = Integer.parseInt(rdata[2])
		playerSeatedCount = Integer.parseInt(rdata[3])
		spectatorCount = Integer.parseInt(rdata[4])
		playerListCount = Integer.parseInt(rdata[5])
		playing = java.lang.Boolean.parseBoolean(rdata[6])
		ruleLock = java.lang.Boolean.parseBoolean(rdata[7])
		ruleName = NetUtil.urlDecode(rdata[8])
		autoStartSeconds = Integer.parseInt(rdata[9])
		gravity = Integer.parseInt(rdata[10])
		denominator = Integer.parseInt(rdata[11])
		are = Integer.parseInt(rdata[12])
		areLine = Integer.parseInt(rdata[13])
		lineDelay = Integer.parseInt(rdata[14])
		lockDelay = Integer.parseInt(rdata[15])
		das = Integer.parseInt(rdata[16])
		twistEnableType = Integer.parseInt(rdata[17])
		b2b = java.lang.Boolean.parseBoolean(rdata[18])
		combo = java.lang.Boolean.parseBoolean(rdata[19])
		rensaBlock = java.lang.Boolean.parseBoolean(rdata[20])
		counter = java.lang.Boolean.parseBoolean(rdata[21])
		bravo = java.lang.Boolean.parseBoolean(rdata[22])
		reduceLineSend = java.lang.Boolean.parseBoolean(rdata[23])
		hurryupSeconds = Integer.parseInt(rdata[24])
		hurryupInterval = Integer.parseInt(rdata[25])
		autoStartTNET2 = java.lang.Boolean.parseBoolean(rdata[26])
		disableTimerAfterSomeoneCancelled = java.lang.Boolean.parseBoolean(rdata[27])
		useMap = java.lang.Boolean.parseBoolean(rdata[28])
		useFractionalGarbage = java.lang.Boolean.parseBoolean(rdata[29])
		garbageChangePerAttack = java.lang.Boolean.parseBoolean(rdata[30])
		garbagePercent = Integer.parseInt(rdata[31])
		spinCheckType = Integer.parseInt(rdata[32])
		twistEnableEZ = java.lang.Boolean.parseBoolean(rdata[33])
		b2bChunk = java.lang.Boolean.parseBoolean(rdata[34])
		strMode = NetUtil.urlDecode(rdata[35])
		singleplayer = java.lang.Boolean.parseBoolean(rdata[36])
		rated = java.lang.Boolean.parseBoolean(rdata[37])
		customRated = java.lang.Boolean.parseBoolean(rdata[38])
		style = Integer.parseInt(rdata[39])
		divideChangeRateByPlayers = java.lang.Boolean.parseBoolean(rdata[40])
		if(rdata.size>41) isTarget = java.lang.Boolean.parseBoolean(rdata[41])
		if(rdata.size>42) targetTimer = Integer.parseInt(rdata[42])
		//useTankMode = Boolean.parseBoolean(rdata[43]);
	}

	/** String(;で区切り)から data代入(Playerリスト除く)
	 * @param str String
	 */
	fun importString(str:String) {
		importStringArray(str.split(";".toRegex()).dropLastWhile {it.isEmpty()}.toTypedArray())
	}

	/** Stringの配列に変換(Playerリスト除く)
	 * @return Stringの配列(String[40])
	 */
	fun exportStringArray():Array<String> = arrayOf("$roomID"
		, NetUtil.urlEncode(strName)
		, "$maxPlayers"
		, "$playerSeatedCount"
		, "$spectatorCount"
		, "$playerListCount"
		, java.lang.Boolean.toString(playing)
		, java.lang.Boolean.toString(ruleLock)
		, NetUtil.urlEncode(ruleName)
		, "$autoStartSeconds"
		, "$gravity"
		, "$denominator"
		, "$are"
		, "$areLine"
		, "$lineDelay"
		, "$lockDelay"
		, "$das"
		, "$twistEnableType"
		, java.lang.Boolean.toString(b2b)
		, java.lang.Boolean.toString(combo)
		, java.lang.Boolean.toString(rensaBlock)
		, java.lang.Boolean.toString(counter)
		, java.lang.Boolean.toString(bravo)
		, java.lang.Boolean.toString(reduceLineSend)
		, "$hurryupSeconds"
		, "$hurryupInterval"
		, java.lang.Boolean.toString(autoStartTNET2)
		, java.lang.Boolean.toString(disableTimerAfterSomeoneCancelled)
		, java.lang.Boolean.toString(useMap)
		, java.lang.Boolean.toString(useFractionalGarbage)
		, java.lang.Boolean.toString(garbageChangePerAttack)
		, "$garbagePercent"
		, "$spinCheckType"
		, java.lang.Boolean.toString(twistEnableEZ)
		, java.lang.Boolean.toString(b2bChunk)
		, NetUtil.urlEncode(strMode)
		, java.lang.Boolean.toString(singleplayer)
		, java.lang.Boolean.toString(rated)
		, java.lang.Boolean.toString(customRated)
		, "$style"
		, java.lang.Boolean.toString(divideChangeRateByPlayers))//rdata[41] = Boolean.toString(useTankMode);

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
	fun getPlayerSeatNumber(pInfo:NetPlayerInfo):Int {
		return playerSeat.indices.firstOrNull {playerSeat[it]===pInfo}
			?: -1
	}

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

			for(i in playerSeat.indices)
				if(playerSeat[i]==null) {
					playerSeat[i] = pInfo
					return i
				}

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
						return true
					else
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
		deadCount = 0
		autoStartActive = false
		isSomeoneCancelled = false
	}

	/** ルーム消去時の処理 */
	fun delete() {
		ruleOpt = null
		mapList.clear()
		playerList.clear()
		playerSeat.clear()
		playerSeatNowPlaying.clear()
		playerQueue.clear()
		playerSeatDead.clear()
		chatList.clear()
	}

	companion object {
		/** Serial version */
		private const val serialVersionUID = 1L

		/** Spin detection type */
		const val SPINTYPE_4POINT = 0
		const val SPINTYPE_IMMOBILE = 1
	}
}
