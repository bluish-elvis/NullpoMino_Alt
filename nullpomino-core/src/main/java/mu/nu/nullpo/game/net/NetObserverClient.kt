/* Copyright (c) 2010, NullNoname
 * All rights reserved.
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * Neither the name of NullNoname nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
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
 * POSSIBILITY OF SUCH DAMAGE. */
package mu.nu.nullpo.game.net

import mu.nu.nullpo.game.play.GameManager
import org.apache.log4j.Logger
import java.io.IOException

/** クライアント(Observer用) */
class NetObserverClient:NetBaseClient {

	/** サーバーVersion */
	@Volatile
	var serverVersion = 0f
		private set

	/** Number of players */
	@Volatile
	var playerCount = 0
		private set

	/** Observercount */
	@Volatile
	var observerCount = 0
		private set

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

	/* 受信したメッセージに応じていろいろ処理をする */
	@Throws(IOException::class)
	override fun processPacket(fullMessage:String) {
		val message = fullMessage.split("\t".toRegex()).dropLastWhile {it.isEmpty()}.toTypedArray() // タブ区切り

		// 接続完了
		if(message[0]=="welcome") {
			//welcome\t[VERSION]\t[PLAYERS]\t[OBSERVERS]\t[VERSION MINOR]\t[VERSION STRING]\t[PING INTERVAL]
			serverVersion = java.lang.Float.parseFloat(message[1])
			playerCount = Integer.parseInt(message[2])
			observerCount = Integer.parseInt(message[3])

			val pingInterval:Long = if(message.size>6) java.lang.Long.parseLong(message[6]) else PING_INTERVAL
			if(pingInterval!=PING_INTERVAL) startPingTask(pingInterval)

			send("observerlogin\t${GameManager.versionMajor}\t${GameManager.isDevBuild}\n")
		}
		// 人count更新
		if(message[0]=="observerupdate") {
			//observerupdate\t[PLAYERS]\t[OBSERVERS]
			playerCount = Integer.parseInt(message[1])
			observerCount = Integer.parseInt(message[2])
		}

		super.processPacket(fullMessage)
	}

	companion object {
		/** Log */
		internal val log = Logger.getLogger(NetObserverClient::class.java)
	}
}
