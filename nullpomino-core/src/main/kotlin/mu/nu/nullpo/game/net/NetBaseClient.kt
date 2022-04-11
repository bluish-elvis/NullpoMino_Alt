/*
 * Copyright (c) 2010-2021, NullNoname
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

import org.apache.logging.log4j.LogManager
import java.io.IOException
import java.io.UnsupportedEncodingException
import java.net.Socket
import java.nio.charset.StandardCharsets
import java.util.LinkedList
import java.util.Timer
import java.util.TimerTask

/** Client(Basic part) */
open class NetBaseClient:Thread {

	/** True if Thread moves between */
	@Volatile
	var threadRunning = false

	/** Regular always While you are connectedtrue */
	@Volatile
	var connectedFlag = false

	/** Socket for connection */
	protected var socket:Socket? = null

	/** Destination host */
	/** @return Destination host
	 */
	var host:String? = null
		protected set

	/** Destination port number */
	/** @return Destination port number
	 */
	var port = 0
		protected set

	/** IP address */
	/** @return Server's IP address
	 */
	var ip = ""
		protected set

	/** Previous incomplete packet */
	protected var notCompletePacketBuffer:StringBuilder? = null

	/** Interface receiving messages */
	protected val listeners = LinkedList<NetMessageListener>()

	/** pingHit count(From serverpongReset When a message is received) */
	protected var pingCount = 0

	/** Ping task */
	protected lateinit var taskPing:TimerTask

	/** AutomaticpingHitTimer */
	protected lateinit var timerPing:Timer

	/** @return Regular always And are connectedtrue
	 */
	val isConnected:Boolean
		get() = connectedFlag&&socket?.isConnected ?: false

	/** Default constructor */
	constructor():super() {
		host = null
		port = DEFAULT_PORT
	}

	/** Constructor
	 * @param host Destination host
	 */
	constructor(host:String):super("NET_$host") {
		this.host = host
		port = DEFAULT_PORT
	}

	/** Constructor
	 * @param host Destination host
	 * @param port Destination port number
	 */
	constructor(host:String, port:Int):super("NET_$host:$port") {
		this.host = host
		this.port = port
	}

	/* Processing of the thread */
	override fun run() {
		threadRunning = true
		connectedFlag = false
		log.info("Connecting to $host:$port")

		var exDisconnectReason:Throwable? = null

		try {
			// Connection
			socket = Socket(host, port).also {
				ip = it.inetAddress.hostAddress
			}
			connectedFlag = true

			// pingHitTimerPreparation
			startPingTask()

			// Message reception
			val buf = ByteArray(BUF_SIZE)
			val size:Int = socket!!.getInputStream().read(buf)

			while(threadRunning&&(size)>0) {
				val message = String(buf, 0, size, StandardCharsets.UTF_8)
				//log.debug(message);

				// The various processing depending on the received message
				var packetBuffer = StringBuilder()
				if(notCompletePacketBuffer!=null) packetBuffer.append(notCompletePacketBuffer)
				packetBuffer.append(message)

				var index:Int = packetBuffer.indexOf("\n")
				while((index)!=-1) {
					val msgNow = packetBuffer.substring(0, index)
					processPacket(msgNow)
					packetBuffer = packetBuffer.replace(0, index+1, "")
					index = packetBuffer.indexOf("\n")
				}

				// If there is an incomplete packet
				notCompletePacketBuffer = packetBuffer.ifEmpty {null}
			}
		} catch(e:UnsupportedEncodingException) {
			throw Error("UTF-8 Not Supported", e)
		} catch(e:Exception) {
			log.info("Socket disconnected", e)
			exDisconnectReason = e
		}

		timerPing.cancel()
		connectedFlag = false
		threadRunning = false

		// Listener
		for(i in listeners.indices)
			try {
				listeners[i].netOnDisconnect(this, exDisconnectReason)
			} catch(e2:Exception) {
				log.debug("Uncaught Exception on NetMessageListener #$i (disconnect event)", e2)
			}

	}

	/** The various processing depending on the received message
	 * @param fullMessage Received Messages
	 * @throws IOException If there are any errors
	 */
	@Throws(IOException::class)
	protected open fun processPacket(fullMessage:String) {
		val message = fullMessage.split("\t".toRegex()).dropLastWhile {it.isEmpty()}.toTypedArray() // Tab delimited

		// pingReply
		if(message[0]=="pong") {
			if(pingCount>=PING_AUTO_DISCONNECT_COUNT/2) log.debug("pong $pingCount")
			pingCount = 0
		}

		// ListenerCall
		for(i in listeners.indices)
			try {
				listeners[i].netOnMessage(this, message)
			} catch(e:Exception) {
				log.error("Uncaught Exception on NetMessageListener #$i (message event)", e)
			}

	}

	/** Send a message to the server
	 * @param bytes Message to be sent
	 * @return true if successful
	 */
	fun send(bytes:ByteArray):Boolean {
		try {
			socket!!.getOutputStream().write(bytes)
		} catch(e:Exception) {
			log.error("Failed to send message", e)
			return false
		}

		return true
	}

	/** Send a message to the server
	 * @param msg Message to be sent
	 * @return true if successful
	 */
	fun send(msg:String):Boolean {
		try {
			socket!!.getOutputStream().write(NetUtil.stringToBytes(msg))
		} catch(e:Exception) {
			log.error("Failed to send message ($msg)", e)
			return false
		}

		return true
	}

	/** NewNetMessageListenerAdd
	 * @param l AddNetMessageListener
	 */
	fun addListener(l:NetMessageListener) {
		if(!listeners.contains(l)) listeners.add(l)
	}

	/** SpecifiedNetMessageListenerDelete the
	 * @param l RemoveNetMessageListener
	 * @return Actually been removedtrue, I has not been added
	 * originallyfalse
	 */
	fun removeListener(l:NetMessageListener):Boolean = listeners.remove(l)

	/** Start Ping timer task
	 * @param interval Interval
	 */
	@JvmOverloads
	fun startPingTask(interval:Long = PING_INTERVAL) {
		log.debug("Ping interval:$interval")
		timerPing.cancel()
		if(interval<=0) return
		pingCount = 0
		taskPing = PingTask()
		timerPing = Timer(true)
		timerPing.schedule(taskPing, interval, interval)
	}

	/** Stop the Ping timer task */
	fun stopPingTask() {
		timerPing.cancel()
	}

	/** Ping task */
	protected inner class PingTask:TimerTask() {
		override fun run() {
			try {
				if(isConnected) {
					if(pingCount>=PING_AUTO_DISCONNECT_COUNT) {
						log.error("Ping timeout")
						threadRunning = false
						connectedFlag = false
						timerPing.cancel()
					} else {
						send("ping\n")
						pingCount++

						if(pingCount>=PING_AUTO_DISCONNECT_COUNT/2)
							log.debug("Ping $pingCount/$PING_AUTO_DISCONNECT_COUNT")
					}
				} else {
					log.info("Ping Timer Cancelled")
					timerPing.cancel()
				}
			} catch(e:Exception) {
				log.error("Exception in Ping Timer. Stopping the task.", e)
				timerPing.cancel()
			}

		}
	}

	companion object {
		/** Log */
		internal val log = LogManager.getLogger()

		/** default Port of number */
		const val DEFAULT_PORT = 9200

		/** The size of the read buffer */
		const val BUF_SIZE = 2048

		/** Default ping interval (1000=1s) */
		const val PING_INTERVAL = 5L*1000

		/** This countOnlypingIf there is no reaction even hit the automatic
		 * disconnection */
		const val PING_AUTO_DISCONNECT_COUNT = 6
	}
}
/** Start Ping timer task */
