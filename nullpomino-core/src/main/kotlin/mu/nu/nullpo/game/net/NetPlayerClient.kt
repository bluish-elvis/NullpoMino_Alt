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

import mu.nu.nullpo.game.play.GameManager
import org.apache.logging.log4j.LogManager
import java.io.IOException
import java.util.LinkedList
import java.util.Locale

/** クライアント(Player用) */
class NetPlayerClient:NetBaseClient {
	/** Player情報 */
	/** @return Player情報のリスト
	 */
	val playerInfoList = LinkedList<NetPlayerInfo>()

	/** ルーム情報 */
	/** @return ルーム情報のリスト
	 */
	val roomInfoList = LinkedList<NetRoomInfo>()

	/** 自分のPlayer名 */
	/** @return Current Player名
	 */
	var playerName:String = ""
		private set

	/** 自分のTeam name */
	private var playerTeam:String = ""

	/** 自分のPlayer識別 number */
	/** @return Current Playerの識別 number
	 */
	var playerUID = 0
		private set

	/** サーバーVersion */
	/** @return サーバーVersion
	 */
	val serverVersion = -1f

	/** Number of players */
	/** @return Number of players
	 */
	var playerCount = -1
		private set

	var observerCount = -1
		private set

	/** 自分自身の情報を取得
	 * @return 自分自身の情報
	 */
	val yourPlayerInfo:NetPlayerInfo?
		get() = getPlayerInfoByUID(playerUID)

	/** @return Current room ID
	 */
	val currentRoomID:Int
		get() = yourPlayerInfo?.roomID ?: -1

	/** @return Current room info
	 */
	val currentRoomInfo:NetRoomInfo?
		get() = getRoomInfo(currentRoomID)

	/** Default constructor */
	constructor():super()

	/** Constructor
	 * @param host 接続先ホスト
	 */
	constructor(host:String):super(host)

	/** Constructor
	 * @param host 接続先ホスト
	 * @param port 接続先ポート number
	 */
	constructor(host:String, port:Int):super(host, port)

	/** Constructor
	 * @param host 接続先ホスト
	 * @param port 接続先ポート number
	 * @param name PlayerのName
	 */
	constructor(host:String, port:Int, name:String):super() {
		this.host = host
		this.port = port
		playerName = name
		playerTeam = ""
	}

	/** Constructor
	 * @param host 接続先ホスト
	 * @param port 接続先ポート number
	 * @param name PlayerのName
	 * @param team 所属するTeam name
	 */
	constructor(host:String, port:Int, name:String, team:String):super() {
		this.host = host
		this.port = port
		playerName = name
		playerTeam = team
	}

	/* 受信したメッセージに応じていろいろ処理をする */
	@Throws(IOException::class)
	override fun processPacket(fullMessage:String) {
		val message = fullMessage.split(Regex("\t")).dropLastWhile {it.isEmpty()} // タブ区切り

		// 接続完了
		if(message[0]=="welcome") {
			//welcome\t[VERSION]\t[PLAYERS]\t[OBSERVERS]\t[VERSION MINOR]\t[VERSION STRING]\t[PING INTERVAL]
			playerCount = message[2].toInt()
			observerCount = message[3].toInt()

			val pingInterval:Long = if(message.size>6) message[6].toLong() else PING_INTERVAL
			if(pingInterval!=PING_INTERVAL) startPingTask(pingInterval)

			send(
				"login\t${GameManager.versionMajor}\t${NetUtil.urlEncode(playerName)}\t${Locale.getDefault().country}\t${
					NetUtil.urlEncode(
						playerTeam
					)
				}\n"
			)
		}
		// 人count更新
		if(message[0]=="observerupdate") {
			//observerupdate\t[PLAYERS]\t[OBSERVERS]
			playerCount = message[1].toInt()
			observerCount = message[2].toInt()
		}
		// ログイン成功
		if(message[0]=="loginsuccess") {
			//loginsuccess\t[NAME]\t[UID]
			playerName = NetUtil.urlDecode(message[1])
			playerUID = message[2].toInt()
		}
		// Playerリスト
		if(message[0]=="playerlist") {
			//playerlist\t[PLAYERS]\t[PLAYERDATA...]

			val numPlayers = message[1].toInt()

			for(i in 0 until numPlayers) {
				val p = NetPlayerInfo(message[2+i])
				playerInfoList.add(p)
			}
		}
		// Player情報更新/新規Player
		if(message[0]=="playerupdate"||message[0]=="playernew") {
			//playerupdate\t[PLAYERDATA]

			val p = NetPlayerInfo(message[1])
			val p2 = getPlayerInfoByUID(p.uid)

			if(p2==null)
				playerInfoList.add(p)
			else {
				val index = playerInfoList.indexOf(p2)
				playerInfoList[index] = p
			}
		}
		// Player切断
		if(message[0]=="playerlogout") {
			//playerlogout\t[PLAYERDATA]

			val p = NetPlayerInfo(message[1])
			val p2 = getPlayerInfoByUID(p.uid)

			if(p2!=null) {
				playerInfoList.remove(p2)
				p2.delete()
			}
		}
		// ルームリスト
		if(message[0]=="roomlist") {
			//roomlist\t[ROOMS]\t[ROOMDATA...]

			val numRooms = message[1].toInt()

			for(i in 0 until numRooms) {
				val r = NetRoomInfo(message[2+i])
				roomInfoList.add(r)
			}
		}
		// ルーム情報更新/新規ルーム出現
		if(message[0]=="roomupdate"||message[0]=="roomcreate") {
			//roomupdate\t[ROOMDATA]

			val r = NetRoomInfo(message[1])
			val r2 = getRoomInfo(r.roomID)

			if(r2==null)
				roomInfoList.add(r)
			else {
				val index = roomInfoList.indexOf(r2)
				roomInfoList[index] = r
			}
		}
		// ルーム消滅
		if(message[0]=="roomdelete") {
			//roomdelete\t[ROOMDATA]

			val r = NetRoomInfo(message[1])
			val r2 = getRoomInfo(r.roomID)

			if(r2!=null) {
				roomInfoList.remove(r2)
				r2.delete()
			}
		}
		// 参戦状態変更
		if(message[0]=="changestatus") {
			val p = getPlayerInfoByUID(message[2].toInt())

			if(p!=null)
				when {
					message[1]=="watchonly" -> {
						p.seatID = -1
						p.queueID = -1
					}
					message[1]=="joinqueue" -> {
						p.seatID = -1
						p.queueID = message[4].toInt()
					}
					message[1]=="joinseat" -> {
						p.seatID = message[4].toInt()
						p.queueID = -1
					}
				}
		}

		// Listener呼び出し
		super.processPacket(fullMessage)
	}

	/** 指定されたIDのルーム情報を返す
	 * @param roomID ルームID
	 * @return ルーム情報(存在しないならnull)
	 */
	fun getRoomInfo(roomID:Int):NetRoomInfo? {
		if(roomID<0) return null

		for(roomInfo in roomInfoList)
			if(roomID==roomInfo.roomID) return roomInfo

		return null
	}

	/** 指定したNameのPlayerを取得
	 * @param name Name
	 * @return 指定したNameのPlayer情報(いなかったらnull)
	 */
	fun getPlayerInfoByName(name:String):NetPlayerInfo? {
		for(pInfo in playerInfoList)
			if(pInfo.strName==name) return pInfo
		return null
	}

	/** 指定したIDのPlayerを取得
	 * @param uid ID
	 * @return 指定したIDのPlayer情報(いなかったらnull)
	 */
	fun getPlayerInfoByUID(uid:Int):NetPlayerInfo? {
		for(pInfo in playerInfoList)
			if(pInfo.uid==uid) return pInfo
		return null
	}

	companion object {
		/** Log */
		internal val log = LogManager.getLogger()
	}
}
