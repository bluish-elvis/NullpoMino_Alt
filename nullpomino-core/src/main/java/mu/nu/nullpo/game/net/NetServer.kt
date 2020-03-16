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

import biz.source_code.base64Coder.Base64Coder
import mu.nu.nullpo.game.component.RuleOptions
import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.game.play.GameManager
import mu.nu.nullpo.util.CustomProperties
import mu.nu.nullpo.util.GeneralUtil
import net.clarenceho.crypto.RC4
import org.apache.log4j.Logger
import org.apache.log4j.PropertyConfigurator
import org.cacas.java.gnu.tools.Crypt
import java.io.*
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.*
import java.nio.channels.spi.SelectorProvider
import java.util.*
import java.util.zip.*
import kotlin.math.pow

/** NullpoMino NetServer<br></br>
 * The code is based on
 * [James Greenfield's The
 * Rox Java NIO Tutorial](http://rox-xmlrpc.sourceforge.net/niotut/) */
@Suppress("RemoveExplicitTypeArguments") class NetServer {

	/** List of SocketChannel */
	private val channelList = LinkedList<SocketChannel>()

	/** Last communication time */
	private val lastCommTimeMap = HashMap<SocketChannel, Long>()

	/** Incomplete packet buffer */
	private val notCompletePacketMap = HashMap<SocketChannel, StringBuilder>()

	/** Player info */
	private val playerInfoMap = HashMap<SocketChannel, NetPlayerInfo>()

	/** Room info list */
	private val roomInfoList = LinkedList<NetRoomInfo>()

	/** Observer list */
	private val observerList = LinkedList<SocketChannel>()

	/** Admin list */
	private val adminList = LinkedList<SocketChannel>()

	/** Number of players connected so far (Used for assigning player ID) */
	private var playerCount = 0

	/** Number of rooms created so far (Used for room ID) */
	private var roomCount = 0

	/** RNG for values selection */
	private val rand = Random()

	/** true if shutdown is requested by the admin */
	private var shutdownRequested = false

	/** The port to listen on */
	private var port:Int = 0

	/** The channel on which we'll accept connections */
	private var serverChannel:ServerSocketChannel? = null

	/** The selector we'll be monitoring */
	private var selector:Selector? = null

	/** The buffer into which we'll read data when it's available */
	private var readBuffer:ByteBuffer? = null

	/** A list of ChangeRequest instances */
	private val pendingChanges = LinkedList<ChangeRequest>()

	/** Maps a SocketChannel to a list of ByteBuffer instances */
	private val pendingData = HashMap<SocketChannel, List<ByteBuffer>>()

	/** Constructor */
	constructor() {
		init(DEFAULT_PORT)
	}

	/** Constructor
	 * @param port The port to listen on
	 */
	constructor(port:Int) {
		init(port)
	}

	/** Initialize
	 * @param port The port to listen on
	 */
	private fun init(port:Int) {
		this.port = port

		// Load player data file
		propPlayerData = CustomProperties()
		try {
			val `in` = GZIPInputStream(FileInputStream("config/setting/netserver_playerdata"))
			propPlayerData.loadFromXML(`in`)
			`in`.close()
		} catch(e:IOException) {
		}

		// Load multiplayer leaderboard file
		propMPRanking = CustomProperties()
		try {
			val `in` = GZIPInputStream(FileInputStream("config/setting/netserver_mpranking"))
			propMPRanking.loadFromXML(`in`)
			`in`.close()
		} catch(e:IOException) {
		}

		// Load single player leaderboard file
		propSPRankingAlltime = CustomProperties()
		try {
			val `in` = GZIPInputStream(FileInputStream("config/setting/netserver_spranking"))
			propSPRankingAlltime.loadFromXML(`in`)
			`in`.close()
		} catch(e:IOException) {
		}

		propSPRankingDaily = CustomProperties()
		try {
			val `in` = GZIPInputStream(FileInputStream("config/setting/netserver_spranking_daily"))
			propSPRankingDaily.loadFromXML(`in`)
			`in`.close()
		} catch(e:IOException) {
		}

		// Load single player personal best
		propSPPersonalBest = CustomProperties()
		try {
			val `in` = GZIPInputStream(FileInputStream("config/setting/netserver_sppersonalbest"))
			propSPPersonalBest.loadFromXML(`in`)
			`in`.close()
		} catch(e:IOException) {
		}

		// Load settings
		allowDNSAccess = propServer.getProperty("netserver.allowDNSAccess", true)
		timeoutTime = propServer.getProperty("netserver.timeoutTime", DEFAULT_TIMEOUT_TIME)
		clientPingInterval = propServer.getProperty("netserver.clientPingInterval", (5*1000).toLong())
		ratingDefault = propServer.getProperty("netserver.ratingDefault", NetPlayerInfo.DEFAULT_MULTIPLAYER_RATING)
		ratingNormalMaxDiff = propServer.getProperty("netserver.ratingNormalMaxDiff", NORMAL_MAX_DIFF)
		ratingProvisionalGames = propServer.getProperty("netserver.ratingProvisionalGames", PROVISIONAL_GAMES)
		ratingMin = propServer.getProperty("netserver.ratingMin", 0)
		ratingMax = propServer.getProperty("netserver.ratingMax", 99999)
		ratingAllowSameIP = propServer.getProperty("netserver.ratingAllowSameIP", true)
		maxMPRanking = propServer.getProperty("netserver.maxMPRanking", DEFAULT_MAX_MPRANKING)
		maxSPRanking = propServer.getProperty("netserver.maxSPRanking", DEFAULT_MAX_SPRANKING)
		spDailyTimeZone = propServer.getProperty("netserver.spDailyTimeZone", "")
		spMinGameRate = propServer.getProperty("netserver.spMinGameRate", DEFAULT_MIN_GAMERATE)
		maxLobbyChatHistory = propServer.getProperty("netserver.maxLobbyChatHistory", DEFAULT_MAX_LOBBYCHAT_HISTORY)
		maxRoomChatHistory = propServer.getProperty("netserver.maxRoomChatHistory", DEFAULT_MAX_ROOMCHAT_HISTORY)

		// Load rules for rated game
		loadRuleList()

		// Load room info presets for rated multiplayer games
		loadPresetList()

		// Load multiplayer leaderboard
		loadMPRankingList()
		propMPRanking.clear() // Clear all entries in order to reduce file size

		// Load single player leaderboard
		loadSPRankingList()
		propSPRankingAlltime.clear() // Clear all entries in order to reduce file size
		propSPRankingDaily.clear()

		// Load ban list
		loadBanList()

		// Load lobby chat history
		loadLobbyChatHistory()
	}

	/** Initialize the selector
	 * @return The selector we'll be monitoring
	 * @throws IOException When the selector can't be created (Usually when the
	 * port is already in use)
	 */
	@Throws(IOException::class)
	private fun initSelector():Selector {
		// Create a new selector
		val socketSelector = SelectorProvider.provider().openSelector()

		// Create a new non-blocking server socket channel
		serverChannel = ServerSocketChannel.open()?.also {
			it.configureBlocking(false)

			// Bind the server socket to the specified address and port
			val isa = InetSocketAddress(port)
			it.socket().bind(isa)

			// Register the server socket channel, indicating an interest in
			// accepting new connections
			it.register(socketSelector, SelectionKey.OP_ACCEPT)
		}
		log.info("Listening on port $port...")

		return socketSelector
	}

	/** Server mainloop */
	fun run() {
		// Startup
		try {
			selector = initSelector()
		} catch(e:IOException) {
			log.fatal("Failed to startup the server", e)
			return
		}

		// Mainloop
		while(!shutdownRequested)
			try {
				try {
					// Process any pending changes
					synchronized(pendingChanges) {
						val changes = pendingChanges.iterator()
						while(changes.hasNext()) {
							val change = changes.next()
							val key = change.socket.keyFor(selector)

							if(key.isValid)
								when(change.type) {
									ChangeRequest.DISCONNECT -> {
										// Delayed disconnect
										val queue = pendingData[change.socket]
										if(queue==null||queue.isEmpty())
											try {
												changes.remove()
												logout(key)
											} catch(e:ConcurrentModificationException) {
												log.debug("ConcurrentModificationException on delayed disconnect", e)
											}

									}
									ChangeRequest.CHANGEOPS -> {
										// interestOps Change
										key.interestOps(change.ops)
										changes.remove()
									}
								}
							else
								changes.remove()
						}
						//this.pendingChanges.clear();
					}

					// Wait for an event one of the registered channels
					selector!!.select()

					// Iterate over the set of keys for which events are available
					val selectedKeys = selector!!.selectedKeys().iterator()
					while(selectedKeys.hasNext()) {
						val key = selectedKeys.next()
						selectedKeys.remove()

						if(!key.isValid) continue

						try {
							// Check what event is available and deal with it
							when {
								key.isAcceptable -> doAccept(key)
								key.isReadable -> doRead(key)
								key.isWritable -> doWrite(key)
							}
						} catch(e:NetServerDisconnectRequestedException) {
							// Intended Disconnect
							log.debug("Socket disconnected by NetServerDisconnectRequestedException")
							logout(key)
						} catch(e:IOException) {
							// Disconnect when something bad happens
							log.info("Socket disconnected by IOException", e)
							logout(key)
						} catch(e:Exception) {
							log.warn("Socket disconnected by Non-IOException", e)
							logout(key)
						}

					}
				} catch(e:ConcurrentModificationException) {
					log.debug("ConcurrentModificationException on server mainloop", e)
				}

			} catch(e:IOException) {
				log.fatal("IOException on server mainloop", e)
			} catch(e:Throwable) {
				log.fatal("Non-IOException throwed on server mainloop", e)
			}

		log.warn("Server Shutdown!")
	}

	/** Accept a new client
	 * @param key SelectionKey
	 * @throws IOException When something bad happens
	 */
	@Throws(IOException::class)
	private fun doAccept(key:SelectionKey) {
		// For an accept to be pending the channel must be a server socket channel.
		val serverSocketChannel = key.channel() as ServerSocketChannel

		// Accept the connection and make it non-blocking
		val socketChannel = serverSocketChannel.accept()
		socketChannel.configureBlocking(false)

		// Register the new SocketChannel with our Selector, indicating
		// we'd like to be notified when there's data waiting to be read
		socketChannel.register(selector, SelectionKey.OP_READ)

		// Add to list
		channelList.add(socketChannel)
		lastCommTimeMap[socketChannel] = System.currentTimeMillis()
		adminSendClientList()

		val ban = getBan(socketChannel)
		if(ban!=null) {
			// Banned
			log.info("Connection is banned:"+getHostName(socketChannel))
			val endDate = ban.endDate
			val strStart = GeneralUtil.exportCalendarString(ban.startDate)
			val strExpire = if(endDate==null) "" else GeneralUtil.exportCalendarString(endDate)
			send(socketChannel, "banned\t$strStart\t$strExpire\n")
			synchronized(pendingChanges) {
				pendingChanges.add(ChangeRequest(socketChannel, ChangeRequest.DISCONNECT, 0))
			}
		} else {
			// Send welcome message
			log.debug("Accept:"+getHostName(socketChannel))
			send(socketChannel,
				"welcome\t${GameManager.versionMajor}\t${playerInfoMap.size}\t${observerList.size}\t"
					+GameManager.versionMinor+"\t${GameManager.versionString}\t$clientPingInterval\t"
					+GameManager.isDevBuild+"\n")
		}
	}

	/** Receive message(s) from client
	 * @param key SelectionKey
	 * @throws IOException When something bad happens
	 */
	@Throws(IOException::class)
	private fun doRead(key:SelectionKey) {
		val socketChannel = key.channel() as SocketChannel

		// Clear out our read buffer so it's ready for new data
		if(readBuffer==null)
			readBuffer = ByteBuffer.allocate(BUF_SIZE)
		else
			readBuffer!!.clear()

		// Attempt to read off the channel
		val numRead:Int
		numRead = socketChannel.read(readBuffer)

		if(numRead==-1)
		// Remote entity shut the socket down cleanly. Do the
		// same from our end and cancel the channel.
			throw NetServerDisconnectRequestedException("Connection is closed (numBytesRead is -1)")

		// Process the packet
		readBuffer!!.flip()

		val bytes = ByteArray(readBuffer!!.limit())
		readBuffer!!.get(bytes)

		val message = NetUtil.bytesToString(bytes)

		// Previous incomplete packet buffer (null if none are present)
		val notCompletePacketBuffer = notCompletePacketMap.remove(socketChannel)

		// The new packet buffer
		var packetBuffer = StringBuilder()
		if(notCompletePacketBuffer!=null) packetBuffer.append(notCompletePacketBuffer)
		packetBuffer.append(message)

		var index:Int = packetBuffer.indexOf("\n")
		while(index!=-1) {
			val msgNow = packetBuffer.substring(0, index)
			processPacket(socketChannel, msgNow)
			packetBuffer = packetBuffer.replace(0, index+1, "")
			index = packetBuffer.indexOf("\n")
		}

		// Place new incomplete packet buffer
		if(packetBuffer.isNotEmpty()) notCompletePacketMap[socketChannel] = packetBuffer
	}

	/** Write message(s) to client
	 * @param key SelectionKey
	 * @throws IOException When something bad happens
	 */
	@Throws(IOException::class)
	private fun doWrite(key:SelectionKey) {
		val socketChannel = key.channel() as SocketChannel

		synchronized(pendingData) {
			val queue = pendingData[socketChannel]

			// Write until there's not more data ...
			if(queue!=null) {
				while(queue.isNotEmpty()) {
					val buf = queue[0]
					socketChannel.write(buf)
					if(buf.remaining()>0)
					// ... or the socket's buffer fills up
						break
					queue.drop(0)
				}

				if(queue.isNullOrEmpty())
				// We wrote away all data, so we're no longer interested
				// in writing on this socket. Switch back to waiting for
				// data.
					key.interestOps(SelectionKey.OP_READ)
			}
		}
	}

	/** Logout
	 * @param key SelectionKey
	 */
	private fun logout(key:SelectionKey) {
		key.cancel()

		val ch = key.channel()
		if(ch is SocketChannel) logout(ch)
	}

	/** Logout
	 * @param channel SocketChannel
	 */
	private fun logout(channel:SocketChannel?) {
		if(channel==null) return

		val remoteAddr = getHostFull(channel)
		log.info("Logout: $remoteAddr")

		try {
			channel.register(selector, 0)
		} catch(e:CancelledKeyException) {
			// CancelledKeyException. This is normal
		} catch(e:Exception) {
			log.debug("Exception throwed on logout (channel.register)", e)
		}

		try {
			channel.finishConnect()
		} catch(e:Exception) {
			log.debug("Exception throwed on logout (channel.finishConnect)", e)
		}

		try {
			channel.close()
		} catch(e:Exception) {
			log.debug("Exception throwed on logout (channel.close)", e)
		}

		try {
			channelList.remove(channel)
			lastCommTimeMap.remove(channel)
			notCompletePacketMap.remove(channel)

			val pInfo = playerInfoMap.remove(channel)
			if(pInfo!=null) {
				log.info(pInfo.strName+" has logged out")

				playerDead(pInfo)
				pInfo.connected = false
				pInfo.ready = false

				val deleteList = LinkedList<NetRoomInfo>() // Room delete check list

				for(roomInfo in roomInfoList)
					if(roomInfo.playerList.contains(pInfo)) {
						roomInfo.playerList.remove(pInfo)
						roomInfo.playerQueue.remove(pInfo)
						roomInfo.exitSeat(pInfo)
						deleteList.add(roomInfo)
					}

				for(roomInfo in deleteList)
					if(!deleteRoom(roomInfo)) {
						joinAllQueuePlayers(roomInfo)

						if(!gameFinished(roomInfo))
							if(!gameStartIfPossible(roomInfo)) {
								autoStartTimerCheck(roomInfo)
								broadcastRoomInfoUpdate(roomInfo)
							}
					}
			}
			if(observerList.remove(channel)) log.info("Observer logout ($remoteAddr)")
			if(adminList.remove(channel)) log.info("Admin logout ($remoteAddr)")

			if(pInfo!=null) {
				broadcastPlayerInfoUpdate(pInfo, "playerlogout")
				pInfo.delete()
			}
			broadcastUserCountToAll()
			adminSendClientList()

			log.debug("Channel close success")
		} catch(e:Exception) {
			log.warn("Exception throwed on logout", e)
		}

		if(channelList.isEmpty())
			cleanup()
		else if(playerInfoMap.isEmpty()) roomInfoList.clear()
	}

	/** Cleanup (after all clients are disconnected) */
	private fun cleanup() {
		log.info("Cleanup")

		channelList.clear()
		lastCommTimeMap.clear()
		notCompletePacketMap.clear()
		observerList.clear()
		adminList.clear()
		playerInfoMap.clear()
		roomInfoList.clear()
		synchronized(pendingData) {
			pendingData.clear()
		}
		if(readBuffer!=null) readBuffer!!.clear()

		System.gc()
	}

	/** Kill timeout (dead) connections
	 * @param timeout Timeout in millsecond
	 * @return Number of connections killed
	 */
	private fun killTimeoutConnections(timeout:Long):Int {
		if(timeout<=0) return 0

		val clients = LinkedList(channelList)
		var killCount = 0

		for(client in clients) {
			val lasttimeL = lastCommTimeMap[client]

			if(lasttimeL!=null) {
				val nowtime = System.currentTimeMillis()

				if(nowtime-lasttimeL>=timeout) {
					logout(client)
					killCount++
				}
			}
		}

		if(killCount>0) log.info("Killed $killCount dead connections")

		return killCount
	}

	/** Send a message
	 * @param client SocketChannel
	 * @param bytes Message to send (byte[])
	 */
	fun send(client:SocketChannel, bytes:ByteArray) {
		synchronized(pendingChanges) {
			// Indicate we want the interest ops set changed
			pendingChanges.add(ChangeRequest(client, ChangeRequest.CHANGEOPS, SelectionKey.OP_WRITE))

			// And queue the data we want written
			synchronized(pendingData) {
				var queue = (pendingData as Map<SocketChannel, List<ByteBuffer>>).getOrDefault(client, emptyList())
				queue += ByteBuffer.wrap(bytes)
			}
		}

		// Finally, wake up our selecting thread so it can make the required changes
		selector!!.wakeup()
	}

	/** Send a message
	 * @param client SocketChannel
	 * @param msg Message to send (String)
	 */
	fun send(client:SocketChannel?, msg:String) {
		client?.let {send(it, NetUtil.stringToBytes(msg))}
	}

	/** Send a message
	 * @param pInfo NetPlayerInfo
	 * @param bytes Message to send (byte[])
	 */
	fun send(pInfo:NetPlayerInfo, bytes:ByteArray) {
		val ch = getSocketChannelByPlayer(pInfo) ?: return
		send(ch, bytes)
	}

	/** Send a message
	 * @param pInfo NetPlayerInfo
	 * @param msg Message to send (String)
	 */
	fun send(pInfo:NetPlayerInfo, msg:String) {
		val ch = getSocketChannelByPlayer(pInfo) ?: return
		send(ch, NetUtil.stringToBytes(msg))
	}

	/** Broadcast a message to all players
	 * @param msg Message to send (String)
	 */
	fun broadcast(msg:String) {
		synchronized(channelList) {
			for(ch in channelList) {
				val p = playerInfoMap[ch]

				if(p!=null) send(ch, msg)
			}
		}
	}

	/** Broadcast a message to all players in specific room
	 * @param msg Message to send (String)
	 * @param roomID Room ID (-1:Lobby)
	 */
	fun broadcast(msg:String, roomID:Int) {
		synchronized(channelList) {
			for(ch in channelList) {
				val p = playerInfoMap[ch]

				if(p!=null&&roomID==p.roomID) send(ch, msg)
			}
		}
	}

	/** Broadcast a message to all players in specific room, except for the
	 * specified player
	 * @param msg Message to send (String)
	 * @param roomID Room ID (-1:Lobby)
	 * @param pInfo The player to avoid sending message
	 */
	fun broadcast(msg:String, roomID:Int, pInfo:NetPlayerInfo) {
		synchronized(channelList) {
			for(ch in channelList) {
				val p = playerInfoMap[ch]

				if(p!=null&&p.uid!=pInfo.uid&&roomID==p.roomID) send(ch, msg)
			}
		}
	}

	/** Broadcast a message to all observers
	 * @param msg Message to send (String)
	 */
	fun broadcastObserver(msg:String) {
		for(ch in observerList)
			send(ch, msg)
	}

	/** Broadcast client count (observers and players) to everyone */
	fun broadcastUserCountToAll() {
		val msg = "observerupdate\t${playerInfoMap.size}\t${observerList.size}\n"
		broadcast(msg)
		broadcastObserver(msg)
		writeServerStatusFile()
	}

	/** Broadcast a message to all admins
	 * @param msg Message to send (String)
	 */
	fun broadcastAdmin(msg:String) {
		for(ch in adminList)
			send(ch, msg)
	}

	/** Get SocketChannel from NetPlayerInfo
	 * @param pInfo Player
	 * @return SocketChannel (null if not found)
	 */
	fun getSocketChannelByPlayer(pInfo:NetPlayerInfo):SocketChannel? {
		synchronized(channelList) {
			for(ch in channelList) {
				val p = playerInfoMap[ch]

				if(p!=null&&p.uid==pInfo.uid) return ch
			}
		}
		return null
	}

	/** Find longest matching player name matching a word boundary in msg.
	 * e.g. msg = "this is a test"
	 * player = "this is" will succeed, "this i" will fail
	 * "this is" will be returned if a player named "this" exists
	 * Returns SocketChannel of found player or null.
	 * @param msg Message to send (String)
	 */
	fun findPlayerByMsg(msg:String):SocketChannel? {
		// Added to support temporary private messaging code, but might be useful even so?
		synchronized(channelList) {
			var maxLen = 0
			var len = 0
			var player = ""
			var chMatch:SocketChannel? = null
			for(ch in channelList) {
				playerInfoMap[ch]?.let {p ->
					len = p.strName.length
					player = p.strName
					if(p.isTripUse) {
						len -= 12
						player = player.substring(0, len)
					}
				}
				if(len+1<msg.length)
					if(msg.substring(0, len+1)=="$player "&&len>maxLen) {
						chMatch = ch
						maxLen = len
					}
			}
			return chMatch
		}
	}

	/** Process a packet.
	 * @param client The SocketChannel who sent this packet
	 * @param fullMessage The string of packet
	 * @throws IOException When something bad happens
	 */
	@Throws(IOException::class)
	private fun processPacket(client:SocketChannel, fullMessage:String) {
		// Check ban
		if(checkConnectionOnBanlist(client)) throw NetServerDisconnectRequestedException("Connection banned")

		// Setup Variables
		val message = fullMessage.split("\t".toRegex()).dropLastWhile {it.isEmpty()}.toTypedArray() // Split by \t
		var pInfo:NetPlayerInfo? = playerInfoMap[client] // NetPlayerInfo of this client. null if not logged in.

		// Update last communication time
		lastCommTimeMap[client] = System.currentTimeMillis()

		// Get information of this server.
		if(message[0]=="getinfo") {
			val loggedInUsersCount = playerInfoMap.size
			val observerCount = observerList.size
			send(client, "getinfo\t${GameManager.versionMajor}\t$loggedInUsersCount\t$observerCount\n")
			return
		}
		// Disconnect request.
		if(message[0]=="disconnect")
			throw NetServerDisconnectRequestedException("Disconnect requested by the client (this is normal)")
		// Ping
		if(message[0]=="ping") {
			//ping\t[ID]
			if(message.size>1) {
				val id = Integer.parseInt(message[1])
				send(client, "pong\t$id\n")
			} else
				send(client, "pong\n")

			// Kill dead connections
			killTimeoutConnections(timeoutTime)

			return
		}
		// Observer login
		if(message[0]=="observerlogin") {
			//observer\t[MAJOR VERSION]\t[MINOR VERSION]\t[DEV BUILD]

			// Ignore it if already logged in
			if(observerList.contains(client)) return
			if(adminList.contains(client)) return
			if(playerInfoMap.containsKey(client)) return

			// Version check
			val serverVer = GameManager.versionMajor
			val clientVer = java.lang.Float.parseFloat(message[1])
			if(serverVer!=clientVer) {
				send(client, "observerloginfail\tDIFFERENT_VERSION\t$serverVer\n")
				//logout(client);
				synchronized(pendingChanges) {
					pendingChanges.add(ChangeRequest(client, ChangeRequest.DISCONNECT, 0))
				}
				return
			}

			// Build type check
			val serverBuildType = GameManager.isDevBuild
			val clientBuildType = java.lang.Boolean.parseBoolean(message[3])
			if(serverBuildType!=clientBuildType) {
				send(client, "observerloginfail\tDIFFERENT_BUILD\t$serverBuildType\n")
				synchronized(pendingChanges) {
					pendingChanges.add(ChangeRequest(client, ChangeRequest.DISCONNECT, 0))
				}
				return
			}

			// Kill dead connections
			killTimeoutConnections(timeoutTime)

			// Success
			observerList.add(client)
			send(client, "observerloginsuccess\n")
			broadcastUserCountToAll()
			adminSendClientList()

			log.info("New observer has logged in ($client)")
			return
		}
		// Player login
		if(message[0]=="login") {
			//login\t[MAJOR VERSION]\t[NAME]\t[COUNTRY]\t[TEAM]\t[MINOR VERSION]\t[DEV BUILD]

			// Ignore it if already logged in
			if(observerList.contains(client)) return
			if(adminList.contains(client)) return
			if(playerInfoMap.containsKey(client)) return

			// Version check
			val serverVer = GameManager.versionMajor
			val clientVer = java.lang.Float.parseFloat(message[1])
			if(serverVer!=clientVer) {
				send(client, "loginfail\tDIFFERENT_VERSION\t$serverVer\n")
				//logout(client);
				synchronized(pendingChanges) {
					pendingChanges.add(ChangeRequest(client, ChangeRequest.DISCONNECT, 0))
				}
				return
			}

			// Build type check
			val serverBuildType = GameManager.isDevBuild
			val clientBuildType = java.lang.Boolean.parseBoolean(message[6])
			if(serverBuildType!=clientBuildType) {
				send(client, "observerloginfail\tDIFFERENT_BUILD\t${GameManager.buildTypeString}\n")
				synchronized(pendingChanges) {
					pendingChanges.add(ChangeRequest(client, ChangeRequest.DISCONNECT, 0))
				}
				return
			}

			// Kill dead connections
			killTimeoutConnections(timeoutTime)

			// Tripcode
			var originalName = NetUtil.urlDecode(message[2])
			val sharpIndex = originalName.indexOf('#')
			var isTripUse = false

			if(sharpIndex!=-1) {
				val strTripKey = originalName.substring(sharpIndex+1)
				val strTripCode = NetUtil.createTripCode(strTripKey, propServer.getProperty("netserver.tripcodemax", 10))

				originalName = if(sharpIndex>0) {
					val strTemp = originalName.substring(0, sharpIndex)
					strTemp.replace('!', '?')+" !"+strTripCode
				} else
					"!$strTripCode"

				isTripUse = true
			} else
				originalName = originalName.replace('!', '?')

			// Decide name (change to something else if needed)
			if(originalName.isEmpty()) originalName = "noname"
			var name = originalName

			if(isTripUse) {
				// Kill the connection of the same name player
				val pInfo2 = searchPlayerByName(name)
				if(pInfo2?.channel!=null) logout(pInfo2.channel)
			} else {
				// Change to "Name(n)" if the player of the same name exists
				var nameCount = 0
				while(searchPlayerByName(name)!=null) {
					name = "$originalName($nameCount)"
					nameCount++
				}
			}

			// Set variables
			pInfo = NetPlayerInfo()
			pInfo.strName = name
			if(message.size>3) pInfo.strCountry = message[3]
			if(message.size>4) pInfo.strTeam = NetUtil.urlDecode(message[4])
			pInfo.uid = playerCount
			pInfo.connected = true
			pInfo.isTripUse = isTripUse

			pInfo.strRealHost = getHostName(client)
			pInfo.strRealIP = getHostAddress(client)
			pInfo.channel = client

			val showhosttype = propServer.getProperty("netserver.showhosttype", 0)
			if(showhosttype==1)
				pInfo.strHost = getHostAddress(client)
			else if(showhosttype==2)
				pInfo.strHost = getHostName(client)
			else if(showhosttype==3) {
				pInfo.strHost = Crypt.crypt(propServer.getProperty("netserver.hostsalt", "AA"), getHostAddress(client))

				val maxlen = propServer.getProperty("netserver.hostcryptmax", 8)
				if(pInfo.strHost.length>maxlen) pInfo.strHost = pInfo.strHost.substring(pInfo.strHost.length-maxlen)
			} else if(showhosttype==4) {
				pInfo.strHost = Crypt.crypt(propServer.getProperty("netserver.hostsalt", "AA"), getHostName(client))

				val maxlen = propServer.getProperty("netserver.hostcryptmax", 8)
				if(pInfo.strHost.length>maxlen) pInfo.strHost = pInfo.strHost.substring(pInfo.strHost.length-maxlen)
			}

			// Load rating
			getPlayerDataFromProperty(pInfo)
			//log.info("Play:" + pInfo.playCount[0] + " Win:" + pInfo.winCount[0]);

			// Success
			playerInfoMap[client] = pInfo
			playerCount++
			send(client, "loginsuccess\t${NetUtil.urlEncode(pInfo.strName)}\t${pInfo.uid}\n")
			log.info(pInfo.strName+" has logged in (Host:${getHostName(client)} Team:${pInfo.strTeam})")

			sendRatedRuleList(client)
			sendPlayerList(client)
			sendRoomList(client)

			broadcastPlayerInfoUpdate(pInfo, "playernew")
			broadcastUserCountToAll()
			adminSendClientList()

			// Send lobby chat history
			while(lobbyChatList!!.size>maxLobbyChatHistory) lobbyChatList!!.removeFirst()
			for(chat in lobbyChatList!!)
				send(client,
					"lobbychath\t${NetUtil.urlEncode(chat.strUserName)}\t${GeneralUtil.exportCalendarString(chat.timestamp!!)}\t"
						+NetUtil.urlEncode(chat.strMessage)+"\n")

			return
		}
		// Send rated presets to client
		if(message[0]=="getpresets") {
			val str = StringBuilder("ratedpresets")

			for(aRatedInfoList in ratedInfoList!!) str.append("\t").append(aRatedInfoList)

			str.append("\n")

			send(client, "$str")

			//log.info("Sent preset message: " + str);

			return
		}
		// Send rule data to server (Client->Server)
		if(message[0]=="ruledata") {
			//ruledata\t[ADLER32CHECKSUM]\t[RULEDATA]

			if(pInfo!=null) {
				val strData = message[2]

				// Is checksum correct?
				val checksumObj = Adler32()
				checksumObj.update(NetUtil.stringToBytes(strData))
				val sChecksum = checksumObj.value
				val cChecksum = java.lang.Long.parseLong(message[1])

				// OK
				if(sChecksum==cChecksum) {
					val strRuleData = NetUtil.decompressString(strData)

					val prop = CustomProperties()
					prop.decode(strRuleData)
					pInfo.ruleOpt = RuleOptions()
					pInfo.ruleOpt!!.readProperty(prop, 0)
					send(client, "ruledatasuccess\n")
				} else
					send(client, "ruledatafail\t$sChecksum\n")
			}
			return
		}
		// Get rule data from server (Server->Client)
		if(message[0]=="ruleget") {
			//ruleget\t[UID]

			if(pInfo!=null) {
				val uid = Integer.parseInt(message[1])
				val p = searchPlayerByUID(uid)

				if(p!=null) {
					if(p.ruleOpt==null) p.ruleOpt = RuleOptions()

					val prop = CustomProperties()
					p.ruleOpt!!.writeProperty(prop, 0)
					val strRuleTemp = prop.encode("RuleData "+p.strName)
					val strRuleData = NetUtil.compressString(strRuleTemp)

					// Checksum
					val checksumObj = Adler32()
					checksumObj.update(NetUtil.stringToBytes(strRuleData))
					val sChecksum = checksumObj.value

					send(client, "rulegetsuccess\t$uid\t$sChecksum\t$strRuleData\n")
				} else
					send(client, "rulegetfail\t$uid\n")
			}
			return
		}
		// Send rated-game rule data (Server->Client)
		if(message[0]=="rulegetrated")
			if(pInfo!=null) {
				val style = Integer.parseInt(message[1])
				val name = message[2]
				val rule = getRatedRule(style, name)

				if(rule!=null) {
					val prop = CustomProperties()
					rule.writeProperty(prop, 0)
					val strRuleTemp = prop.encode("Rated RuleData "+rule.strRuleName)
					val strRuleData = NetUtil.compressString(strRuleTemp)

					// Checksum
					val checksumObj = Adler32()
					checksumObj.update(NetUtil.stringToBytes(strRuleData))
					val sChecksum = checksumObj.value

					send(client, "rulegetratedsuccess\t$style\t$name\t$sChecksum\t$strRuleData\n")
				} else
					send(client, "rulegetratedfail\t$style\t$name\n")
			}
		// Lobby chat
		if(message[0]=="lobbychat") {
			//lobbychat\t[MESSAGE]

			//			String[] message = fullMessage.split("\t");	// Split by \t
			//			NetPlayerInfo pInfo = playerInfoMap.get(client);	// NetPlayerInfo of this client. null if not logged in.

			if(pInfo!=null) {
				val chat = NetChatMessage(NetUtil.urlDecode(message[1]), pInfo)

				// Begin temporary private message code here
				var msg = chat.strMessage
				if(msg.length>5&&msg.substring(0, 5)=="/msg ") {
					val ch = findPlayerByMsg(msg.substring(5))
					if(ch==null)
						send(pInfo.channel,
							"lobbychat\t${chat.uid}\t${NetUtil.urlEncode(chat.strUserName)}\t${GeneralUtil.exportCalendarString(chat.timestamp!!)}\t${NetUtil.urlEncode("(private) Cannot find user")}\n")
					else {
						var playerName = ""
						var len = 0
						playerInfoMap[ch]?.let {p ->
							playerName = p.strName
							len = playerName.length
							if(p.isTripUse) len -= 12
						}
						msg = chat.strMessage.substring(len+6)
						send(pInfo.channel,
							"lobbychat\t${chat.uid}\t${NetUtil.urlEncode(chat.strUserName)}\t${GeneralUtil.exportCalendarString(chat.timestamp!!)}\t${NetUtil.urlEncode("-> *"+playerName.substring(0, len)+"* "+msg)}\n")
						send(ch,
							"lobbychat\t${chat.uid}\t${NetUtil.urlEncode(chat.strUserName)}\t${GeneralUtil.exportCalendarString(chat.timestamp!!)}\t${NetUtil.urlEncode("(private) $msg")}\n")

					}
				} else {
					// End here
					chat.outputLog()
					lobbyChatList!!.add(chat)
					while(lobbyChatList!!.size>maxLobbyChatHistory) lobbyChatList!!.removeFirst()
					saveLobbyChatHistory()

					broadcast("lobbychat\t${chat.uid}\t${NetUtil.urlEncode(chat.strUserName)}\t${GeneralUtil.exportCalendarString(chat.timestamp!!)}\t${NetUtil.urlEncode(chat.strMessage)}\n")
				}
			}
			return
		}
		// Room chat
		if(message[0]=="chat") {
			//chat\t[MESSAGE]

			if(pInfo!=null&&pInfo.roomID!=-1) {
				val roomInfo = getRoomInfo(pInfo.roomID)
				if(roomInfo!=null) {
					val chat = NetChatMessage(NetUtil.urlDecode(message[1]), pInfo, roomInfo)
					chat.outputLog()
					roomInfo.chatList.add(chat)
					while(roomInfo.chatList.size>maxRoomChatHistory) roomInfo.chatList.removeFirst()

					broadcast("chat\t${chat.uid}\t${NetUtil.urlEncode(chat.strUserName)}\t${GeneralUtil.exportCalendarString(chat.timestamp!!)}\t${NetUtil.urlEncode(chat.strMessage)}\n", pInfo.roomID)
				}
			}
			return
		}
		// Get multiplayer leaderboard
		if(message[0]=="mpranking") {
			//mpranking\t[STYLE]

			val style = Integer.parseInt(message[1])
			val myRank = mpRankingIndexOf(style, pInfo)

			val strPData = StringBuilder()
			var prevRating = -1
			var nowRank = 0
			for(i in 0 until mpRankingList!![style].size) {
				val p = mpRankingList!![style][i]
				if(i==0||p.rating[style]<prevRating) {
					prevRating = p.rating[style]
					nowRank = i
				}
				strPData.append(nowRank).append(";").append(NetUtil.urlEncode(p.strName)).append(";").append(p.rating[style])
					.append(";").append(p.playCount[style]).append(";").append(p.winCount[style]).append("\t")
			}
			if(myRank==-1&&pInfo!=null) {
				strPData.append((-1).toString()+";").append(NetUtil.urlEncode(pInfo.strName)).append(";")
					.append(pInfo.rating[style]).append(";").append(pInfo.playCount[style]).append(";").append(pInfo.winCount[style])
					.append("\t")
			}
			val strPDataC = NetUtil.compressString("$strPData")

			val strMsg = "mpranking\t$style\t$myRank\t$strPDataC\n"
			send(client, strMsg)
		}
		// Single player room
		if(message[0]=="singleroomcreate")
		//singleroomcreate\t[roomName]\t[mode]\t[rule]
			if(pInfo!=null&&pInfo.roomID==-1) {
				val roomInfo = NetRoomInfo()

				roomInfo.singleplayer = true
				roomInfo.strMode = NetUtil.urlDecode(message[2])

				roomInfo.strName = NetUtil.urlDecode(message[1])
				if(roomInfo.strName.isEmpty()) roomInfo.strName = "Single (${pInfo.strName})"

				roomInfo.maxPlayers = 1

				if(message.size>3) {
					roomInfo.ruleName = NetUtil.urlDecode(message[3])
					roomInfo.ruleOpt = RuleOptions(getRatedRule(0, roomInfo.ruleName))
					roomInfo.ruleLock = true
					roomInfo.rated = true
				} else {
					roomInfo.ruleName = pInfo.ruleOpt!!.strRuleName
					roomInfo.ruleOpt = RuleOptions(pInfo.ruleOpt)
					roomInfo.ruleLock = false
					roomInfo.rated = false
				}

				roomInfo.roomID = roomCount

				roomCount++
				if(roomCount==-1) roomCount = 0

				roomInfoList.add(roomInfo)

				pInfo.roomID = roomInfo.roomID
				pInfo.resetPlayState()
				pInfo.playCountNow = 0
				pInfo.winCountNow = 0

				roomInfo.playerList.add(pInfo)
				pInfo.seatID = roomInfo.joinSeat(pInfo)

				// Send rule data if rated room
				if(roomInfo.rated) {
					val prop = CustomProperties()
					roomInfo.ruleOpt!!.writeProperty(prop, 0)
					val strRuleTemp = prop.encode("RuleData")
					val strRuleData = NetUtil.compressString(strRuleTemp)
					send(client, "rulelock\t$strRuleData\n")
				}

				broadcastPlayerInfoUpdate(pInfo)
				broadcastRoomInfoUpdate(roomInfo, "roomcreate")
				send(client, "roomcreatesuccess\t${roomInfo.roomID}\t0\t-1\n")

				log.info("NewSingleRoom ID:${roomInfo.roomID} Title:"+roomInfo.strName)
			}
		// Multiplayer room
		if(message[0]=="roomcreate") {
			if(pInfo!=null&&pInfo.roomID==-1) {
				val strRoomInfo = NetUtil.urlDecode(message[2])
				val roomInfo = NetRoomInfo(strRoomInfo)

				roomInfo.strName = NetUtil.urlDecode(message[1])
				if(roomInfo.strName.isEmpty()) roomInfo.strName = "No Title"

				if(roomInfo.maxPlayers<1) roomInfo.maxPlayers = 1
				if(roomInfo.maxPlayers>6) roomInfo.maxPlayers = 6

				if(roomInfo.ruleLock) {
					roomInfo.ruleName = pInfo.ruleOpt!!.strRuleName
					roomInfo.ruleOpt = RuleOptions(pInfo.ruleOpt)
				}

				if(roomInfo.strMode.isEmpty()) roomInfo.strMode = NetUtil.urlDecode(message[3])

				// Set values
				if(roomInfo.useMap&&message.size>4) {
					val strDecompressed = NetUtil.decompressString(message[4])
					val strMaps = strDecompressed.split("\t".toRegex()).dropLastWhile {it.isEmpty()}.toTypedArray()

					val maxMap = strMaps.size

					Collections.addAll(roomInfo.mapList, *strMaps)

					if(roomInfo.mapList.isEmpty()) {
						log.debug("Room${roomInfo.roomID}: No maps")
						roomInfo.useMap = false
					} else
						log.debug("Room${roomInfo.roomID}: Received ${roomInfo.mapList.size} maps")
				}

				roomInfo.roomID = roomCount

				roomCount++
				if(roomCount==-1) roomCount = 0

				roomInfoList.add(roomInfo)

				pInfo.roomID = roomInfo.roomID
				pInfo.resetPlayState()
				pInfo.playCountNow = 0
				pInfo.winCountNow = 0

				roomInfo.playerList.add(pInfo)
				pInfo.seatID = roomInfo.joinSeat(pInfo)

				// Send rule data if rule-lock is enabled
				if(roomInfo.ruleLock) {
					val prop = CustomProperties()
					roomInfo.ruleOpt!!.writeProperty(prop, 0)
					val strRuleTemp = prop.encode("RuleData")
					val strRuleData = NetUtil.compressString(strRuleTemp)
					send(client, "rulelock\t$strRuleData\n")
					//log.info("rulelock\t" + strRuleData);
				}

				broadcastPlayerInfoUpdate(pInfo)
				broadcastRoomInfoUpdate(roomInfo, "roomcreate")
				send(client, "roomcreatesuccess\t${roomInfo.roomID}\t${pInfo.seatID}\t-1\n")

				log.info(
					"NewRoom ID:${roomInfo.roomID} Title:${roomInfo.strName} RuleLock:${roomInfo.ruleLock} Map:${roomInfo.useMap} Mode:${roomInfo.strMode}")
			}
			return
		}
		if(message[0]=="ratedroomcreate") {
			if(pInfo!=null&&pInfo.roomID==-1) {
				val i = Integer.parseInt(message[3])
				val strPreset = NetUtil.decompressString(ratedInfoList!![i])
				val roomInfo = NetRoomInfo(strPreset)

				roomInfo.strName = NetUtil.urlDecode(message[1])
				if(roomInfo.strName.isEmpty()) roomInfo.strName = "No Title"

				roomInfo.maxPlayers = Integer.parseInt(message[2])
				if(roomInfo.maxPlayers<1) roomInfo.maxPlayers = 1
				if(roomInfo.maxPlayers>6) roomInfo.maxPlayers = 6

				roomInfo.strMode = NetUtil.urlDecode(message[4])

				roomInfo.rated = true
				roomInfo.ruleLock = false //TODO: implement rule whitelists or rule locks in presets where it is relevant

				roomInfo.roomID = roomCount //TODO: fix copy-paste code

				roomCount++
				if(roomCount==-1) roomCount = 0

				roomInfoList.add(roomInfo)

				pInfo.roomID = roomInfo.roomID
				pInfo.resetPlayState()
				pInfo.playCountNow = 0
				pInfo.winCountNow = 0

				roomInfo.playerList.add(pInfo)
				pInfo.seatID = roomInfo.joinSeat(pInfo)

				broadcastPlayerInfoUpdate(pInfo)
				broadcastRoomInfoUpdate(roomInfo, "roomcreate")
				send(client, "roomcreatesuccess\t${roomInfo.roomID}\t${pInfo.seatID}\t-1\n")

				log.info("NewRatedRoom ID:${roomInfo.roomID} Title:${roomInfo.strName} RuleLock:${roomInfo.ruleLock} Map:${roomInfo.useMap} Mode:${roomInfo.strMode}")
			}
			return
		}
		// Join room (If roomID is -1, the player will return to lobby)
		if(message[0]=="roomjoin") {
			//roomjoin\t[ROOMID]\t[WATCH]

			if(pInfo!=null) {
				val roomID = Integer.parseInt(message[1])
				val watch = java.lang.Boolean.parseBoolean(message[2])
				val prevRoom = getRoomInfo(pInfo.roomID)
				val newRoom = getRoomInfo(roomID)

				if(roomID<0) {
					// Return to lobby
					if(prevRoom!=null) {
						val seatID = pInfo.seatID
						broadcast(
							"playerleave\t${pInfo.uid}\t${NetUtil.urlEncode(pInfo.strName)}\t$seatID\n", prevRoom.roomID, pInfo)
						playerDead(pInfo)
						pInfo.ready = false
						prevRoom.exitSeat(pInfo)
						prevRoom.exitQueue(pInfo)
						prevRoom.playerList.remove(pInfo)
						if(!deleteRoom(prevRoom)) {
							joinAllQueuePlayers(prevRoom)

							if(!gameFinished(prevRoom))
								if(!gameStartIfPossible(prevRoom)) {
									autoStartTimerCheck(prevRoom)
									broadcastRoomInfoUpdate(prevRoom)
								}
						}
					}
					pInfo.roomID = -1
					pInfo.seatID = -1
					pInfo.queueID = -1
					pInfo.resetPlayState()
					pInfo.playCountNow = 0
					pInfo.winCountNow = 0

					broadcastPlayerInfoUpdate(pInfo)
					send(client, "roomjoinsuccess\t-1\t-1\t-1\n")
				} else if(newRoom!=null) {
					// Enter a room
					if(prevRoom!=null) {
						val seatID = pInfo.seatID
						broadcast(
							"playerleave\t${pInfo.uid}\t${NetUtil.urlEncode(pInfo.strName)}\t$seatID\n", prevRoom.roomID, pInfo)
						playerDead(pInfo)
						pInfo.ready = false
						prevRoom.exitSeat(pInfo)
						prevRoom.exitQueue(pInfo)
						prevRoom.playerList.remove(pInfo)
						if(!deleteRoom(prevRoom)) {
							joinAllQueuePlayers(prevRoom)

							if(!gameFinished(prevRoom))
								if(!gameStartIfPossible(prevRoom)) {
									autoStartTimerCheck(prevRoom)
									broadcastRoomInfoUpdate(prevRoom)
								}
						}
					}
					pInfo.roomID = newRoom.roomID
					pInfo.resetPlayState()
					pInfo.playCountNow = 0
					pInfo.winCountNow = 0

					newRoom.playerList.add(pInfo)

					pInfo.seatID = -1
					if(!watch&&!newRoom.singleplayer) {
						pInfo.seatID = newRoom.joinSeat(pInfo)

						if(pInfo.seatID==-1) pInfo.queueID = newRoom.joinQueue(pInfo)
					}

					// Send rule data if rule-lock is enabled
					if(newRoom.ruleLock) {
						val prop = CustomProperties()
						newRoom.ruleOpt!!.writeProperty(prop, 0)
						val strRuleTemp = prop.encode("RuleData")
						val strRuleData = NetUtil.compressString(strRuleTemp)
						send(client, "rulelock\t$strRuleData\n")
						//log.info("rulelock\t" + strRuleData);
					}//	|| newRoom.rated //XXX: This breaks the new Rated with room info preset system, as there is no Rule Lock for Rated now.

					// Map send
					if(newRoom.useMap&&!newRoom.mapList.isEmpty()) {
						val strMapTemp = StringBuilder()
						val maxMap = newRoom.mapList.size
						for(i in 0 until maxMap) {
							strMapTemp.append(newRoom.mapList[i])
							if(i<maxMap-1) strMapTemp.append("\t")
						}
						val strCompressed = NetUtil.compressString("$strMapTemp")
						send(client, "values\t$strCompressed\n")
					}

					broadcast(
						"playerenter\t${pInfo.uid}\t${NetUtil.urlEncode(pInfo.strName)}\t${pInfo.seatID}\n", newRoom.roomID, pInfo)
					broadcastRoomInfoUpdate(newRoom)
					broadcastPlayerInfoUpdate(pInfo)
					send(client, "roomjoinsuccess\t${newRoom.roomID}\t${pInfo.seatID}\t${pInfo.queueID}\n")

					// Send chat history
					for(chat in newRoom.chatList)
						send(client,
							"chath\t${NetUtil.urlEncode(chat.strUserName)}\t${GeneralUtil.exportCalendarString(chat.timestamp!!)}\t${NetUtil.urlEncode(chat.strMessage)}\n")
				} else
				// No such a room
					send(client, "roomjoinfail\n")
			}
			return
		}
		// Change team
		if(message[0]=="changeteam")
		//changeteam\t[TEAM]
			if(pInfo!=null&&!pInfo.playing) {
				var strTeam = ""
				if(message.size>1) strTeam = NetUtil.urlDecode(message[1])

				if(strTeam!=pInfo.strTeam) {
					pInfo.strTeam = strTeam
					broadcastPlayerInfoUpdate(pInfo)

					broadcast("changeteam\t${pInfo.uid}\t${NetUtil.urlEncode(pInfo.strName)}\t${NetUtil.urlEncode(pInfo.strTeam)}\n", pInfo.roomID)
				}
			}
		// Change Player/Spectator status
		if(message[0]=="changestatus")
		//changestatus\t[WATCH]
			if(pInfo!=null&&!pInfo.playing&&pInfo.roomID!=-1) {
				val roomInfo = getRoomInfo(pInfo.roomID)
				val watch = java.lang.Boolean.parseBoolean(message[1])

				if(!roomInfo!!.singleplayer) {
					when {
						watch -> {
							// Change to spectator
							val prevSeatID = pInfo.seatID
							roomInfo.exitSeat(pInfo)
							roomInfo.exitQueue(pInfo)
							pInfo.ready = false
							pInfo.seatID = -1
							pInfo.queueID = -1
							//send(client, "changestatus\twatchonly\t-1\n");
							broadcast("changestatus\twatchonly\t${pInfo.uid}\t${NetUtil.urlEncode(pInfo.strName)}\t$prevSeatID\n", pInfo.roomID)

							joinAllQueuePlayers(roomInfo) // Let the queue-player to join
						} // Change to player
						roomInfo.canJoinSeat() -> {
							pInfo.seatID = roomInfo.joinSeat(pInfo)
							pInfo.queueID = -1
							pInfo.ready = false
							//send(client, "changestatus\tjoinseat\t" + pInfo.seatID + "\n");
							broadcast("changestatus\tjoinseat\t${pInfo.uid}\t${NetUtil.urlEncode(pInfo.strName)}\t${pInfo.seatID}\n", pInfo.roomID)
						}
						else -> {
							pInfo.seatID = -1
							pInfo.queueID = roomInfo.joinQueue(pInfo)
							pInfo.ready = false
							//send(client, "changestatus\tjoinqueue\t" + pInfo.queueID + "\n");
							broadcast("changestatus\tjoinqueue\t${pInfo.uid}\t${NetUtil.urlEncode(pInfo.strName)}\t${pInfo.queueID}\n", pInfo.roomID)
						}
					}
					broadcastPlayerInfoUpdate(pInfo)
					if(!gameStartIfPossible(roomInfo)) autoStartTimerCheck(roomInfo)
					broadcastRoomInfoUpdate(roomInfo)
				}
			}
		// Start game (Single player)
		if(message[0]=="start1p")
			if(pInfo!=null) {
				log.info("Starting single player game")

				val roomInfo = getRoomInfo(pInfo.roomID)
				val seat = roomInfo!!.getPlayerSeatNumber(pInfo)

				if(seat!=-1&&roomInfo.singleplayer) gameStart(roomInfo)
			}
		// Ready state change
		if(message[0]=="ready")
		//ready\t[STATE]
			if(pInfo!=null) {
				val roomInfo = getRoomInfo(pInfo.roomID)
				val seat = roomInfo!!.getPlayerSeatNumber(pInfo)

				if(seat!=-1&&!roomInfo.singleplayer) {
					pInfo.ready = java.lang.Boolean.parseBoolean(message[1])
					broadcastPlayerInfoUpdate(pInfo)

					if(!pInfo.ready) roomInfo.isSomeoneCancelled = true

					// Start a game if possible
					if(!gameStartIfPossible(roomInfo)) autoStartTimerCheck(roomInfo)
				}
			}
		// Autostart
		if(message[0]=="autostart")
			if(pInfo!=null) {
				val roomInfo = getRoomInfo(pInfo.roomID)
				val seat = roomInfo!!.getPlayerSeatNumber(pInfo)

				if(seat!=-1&&roomInfo.autoStartActive&&!roomInfo.singleplayer) {
					if(roomInfo.autoStartTNET2) {
						// Move all non-ready players to spectators
						val pList = LinkedList<NetPlayerInfo>()
						pList.addAll(roomInfo.playerSeat)

						for(p in pList)
							if(!p.ready) {
								val prevSeatID = p.seatID
								roomInfo.exitSeat(p)
								roomInfo.exitQueue(p)
								p.ready = false
								p.seatID = -1
								p.queueID = -1
								broadcast("changestatus\twatchonly\t${p.uid}\t${NetUtil.urlEncode(p.strName)}\t$prevSeatID\n", p.roomID)
							}

						joinAllQueuePlayers(roomInfo)
					}

					gameStart(roomInfo)
				}
			}
		// Dead
		if(message[0]=="dead")
			if(pInfo!=null)
				if(message.size>1) {
					val koUID = Integer.parseInt(message[1])
					val koPlayerInfo = searchPlayerByUID(koUID)
					playerDead(pInfo, koPlayerInfo)
				} else
					playerDead(pInfo)
		// Race mode win (TODO: Replace with something cheat-proof)
		if(message[0]=="racewin")
			if(pInfo!=null&&pInfo.roomID!=-1&&pInfo.seatID!=-1) {
				val roomInfo = getRoomInfo(pInfo.roomID)
				val modeIndex = mpModeList!![roomInfo!!.style].indexOf(roomInfo.strMode)
				val isRace = if(modeIndex==-1) false else mpModeIsRace!![roomInfo.style][modeIndex]

				if(roomInfo.playing&&isRace)
					for(i in message.size-1 downTo 2) {
						val koUID = Integer.parseInt(message[i])
						if(koUID!=pInfo.uid) {
							val koPlayerInfo = searchPlayerByUID(koUID)

							if(koPlayerInfo!=null&&koPlayerInfo.roomID==roomInfo.roomID) playerDead(koPlayerInfo, pInfo)
						}
					}
			}
		// Multiplayer end-of-game stats
		if(message[0]=="gstat")
			if(pInfo!=null&&pInfo.roomID!=-1&&pInfo.seatID!=-1) {
				val roomInfo = getRoomInfo(pInfo.roomID)

				if(!roomInfo!!.singleplayer) {
					val msg = StringBuilder("gstat\t${pInfo.uid}\t${pInfo.seatID}\t${NetUtil.urlEncode(pInfo.strName)}\t")
					for(i in 1 until message.size) {
						msg.append(message[i])
						if(i<message.size-1) msg.append("\t")
					}
					msg.append("\n")

					broadcast("$msg", roomInfo.roomID)
				}
			}
		// Single player end-of-game stats
		if(message[0]=="gstat1p")
			if(pInfo!=null&&pInfo.roomID!=-1&&pInfo.seatID!=-1) {
				val roomInfo = getRoomInfo(pInfo.roomID)

				if(roomInfo!!.singleplayer) {
					val msg = "gstat1p\t${message[1]}\n"
					broadcast(msg, roomInfo.roomID)
				}
			}
		// Single player replay send
		if(message[0]=="spsend")
		//spsend\t[CHECKSUM]\t[DATA]
			if(pInfo!=null&&pInfo.roomID!=-1&&pInfo.seatID!=-1) {
				val roomInfo = getRoomInfo(pInfo.roomID)
				if(!pInfo.isTripUse)
					broadcast("spsendok\t-1\tfalse\t-1\n", pInfo.roomID)
				else if(roomInfo!!.singleplayer) {
					val sChecksum = java.lang.Long.parseLong(message[1])
					val checksumObj = Adler32()
					checksumObj.update(NetUtil.stringToBytes(message[2]))
					log.info("Checksums are: $sChecksum and ${checksumObj.value}")

					if(sChecksum==checksumObj.value) {
						val strData = NetUtil.decompressString(message[2])
						val record = NetSPRecord(strData)
						val rule = if(roomInfo.rated) roomInfo.ruleName else "any" // "any" for unrated rules
						record.strPlayerName = pInfo.strName
						record.strModeName = roomInfo.strMode
						record.strRuleName = rule
						record.style = roomInfo.style
						record.strTimeStamp = GeneralUtil.exportCalendarString()

						val gamerate = record.stats!!.gamerate*100f

						val isDailyWiped = updateSPDailyRanking()
						var rank = -1
						var rankDaily = -1

						val ranking = getSPRanking(rule, record.strModeName, record.gameType)
						val rankingDaily = getSPRanking(rule, record.strModeName, record.gameType, true)
						if(ranking==null) log.warn("All-time ranking not found:${record.strModeName}")
						if(rankingDaily==null) log.warn("Daily ranking not found:${record.strModeName}")

						if((ranking!=null||rankingDaily!=null)&&gamerate>=spMinGameRate) {
							if(ranking!=null) rank = ranking.registerRecord(record)
							if(rankingDaily!=null) rankDaily = rankingDaily.registerRecord(record)

							if(rank!=-1||rankDaily!=-1||isDailyWiped) writeSPRankingToFile()

							var isPB = false
							if(ranking!=null) {
								isPB = pInfo.spPersonalBest.registerRecord(ranking.rankingType, record)
								if(isPB) {
									setPlayerDataToProperty(pInfo)
									writePlayerDataToFile()
								}
							}

							log.info("Name:${pInfo.strName} Mode:${record.strModeName} AllTime:$rank Daily:$rankDaily")
							broadcast("spsendok\t$rank\t$isPB\t$rankDaily\n", pInfo.roomID)
						} else
							broadcast("spsendok\t-1\tfalse\t-1\n", pInfo.roomID)
					} else
						send(client, "spsendng\n")
				}
			}
		// Single player leaderboard
		if(message[0]=="spranking") {
			//spranking\t[RULE]\t[MODE]\t[GAMETYPE]\t[DAILY]
			val strRule = NetUtil.urlDecode(message[1])
			val strMode = NetUtil.urlDecode(message[2])
			val gameType = Integer.parseInt(message[3])
			val isDaily = java.lang.Boolean.parseBoolean(message[4])

			if(isDaily) if(updateSPDailyRanking()) writeSPRankingToFile()

			var myRank = -1
			val ranking = getSPRanking(strRule, strMode, gameType, isDaily)

			if(ranking!=null) {
				var maxRecord = ranking.listRecord.size

				val strData = StringBuilder()

				for(i in 0 until maxRecord) {
					var strRow = ""
					if(i>0) strRow = ";"

					val record = ranking.listRecord[i]
					strRow += "$i${","+NetUtil.urlEncode(record.strPlayerName)},"
					strRow += record.strTimeStamp+",${record.stats!!.gamerate},"
					strRow += record.getStatRow(ranking.rankingType)

					if(pInfo!=null&&pInfo.strName==record.strPlayerName) myRank = i

					strData.append(strRow)
				}
				if(myRank==-1&&pInfo!=null&&!isDaily) {
					val record = pInfo.spPersonalBest.getRecord(strRule, strMode, gameType)

					if(record!=null) {
						var strRow = ""
						if(maxRecord>0) strRow += ","

						maxRecord++
						strRow += (-1).toString()+",${NetUtil.urlEncode(record.strPlayerName)},"
						strRow += record.strTimeStamp+",${record.stats!!.gamerate},"
						strRow += record.getStatRow(ranking.rankingType)

						strData.append(strRow)
					}
				}

				var strMsg = "spranking\t$strRule\t$strMode\t$gameType\t$isDaily\t"
				strMsg += "${ranking.rankingType}\t$maxRecord\t$strData\n"
				send(client, strMsg)
			} else {
				var strMsg = "spranking\t$strRule\t$strMode\t$gameType\t$isDaily\t"
				strMsg += "0\t0\n"
				send(client, strMsg)
			}
		}
		// Single player replay download
		if(message[0]=="spdownload") {
			//spdownload\t[RULE]\t[MODE]\t[GAMETYPE]\t[DAILY]\t[NAME]
			var strRule = NetUtil.urlDecode(message[1])
			val strMode = NetUtil.urlDecode(message[2])
			val gameType = Integer.parseInt(message[3])
			val isDaily = java.lang.Boolean.parseBoolean(message[4])
			val strName = NetUtil.urlDecode(message[5])

			// Is any rule room?
			if(pInfo!=null&&pInfo.roomID!=-1) {
				val roomInfo = getRoomInfo(pInfo.roomID)
				if(roomInfo!=null&&!roomInfo.rated) strRule = "any"
			}

			if(isDaily) if(updateSPDailyRanking()) writeSPRankingToFile()

			val ranking = getSPRanking(strRule, strMode, gameType, isDaily)
			if(ranking!=null) {
				// Get from leaderboard...
				var record = ranking.getRecord(strName)
				// or from Personal Best when not found in the leaderboard.
				if(record==null&&!isDaily) record = pInfo!!.spPersonalBest.getRecord(strRule, strMode, gameType)

				if(record!=null) {
					val checksumObj = Adler32()
					checksumObj.update(NetUtil.stringToBytes(record.strReplayProp))
					val sChecksum = checksumObj.value

					val strMsg = "spdownload\t$sChecksum\t${record.strReplayProp}\n"
					send(client, strMsg)
				} else
					log.warn("Record not found (Mode:$strMode, Rule:$strRule, Type:$gameType Name:$strName)")
			} else if(!isDaily)
				log.warn("All-time ranking not found (Mode:$strMode, Rule:$strRule, Type:$gameType)")
			else
				log.warn("Daily ranking not found (Mode:$strMode, Rule:$strRule, Type:$gameType)")
		}
		// Single player mode reset
		if(message[0]=="reset1p")
			if(pInfo!=null) {
				val roomInfo = getRoomInfo(pInfo.roomID)
				if(roomInfo!=null) {
					val seat = roomInfo.getPlayerSeatNumber(pInfo)

					if(seat!=-1) {
						pInfo.resetPlayState()
						broadcastPlayerInfoUpdate(pInfo)
						gameFinished(roomInfo)
						broadcast("reset1p\n", roomInfo.roomID, pInfo)
					}
				}
			}
		// Game messages (NetServer will deliver them to other players but won't modify it)
		if(message[0]=="game")
			if(pInfo!=null) {
				val roomInfo = getRoomInfo(pInfo.roomID)
				if(roomInfo!=null) {
					val seat = roomInfo.getPlayerSeatNumber(pInfo)

					if(seat!=-1) {
						val msg = StringBuilder("game\t${pInfo.uid}\t$seat\t")
						for(i in 1 until message.size) {
							msg.append(message[i])
							if(i<message.size-1) msg.append("\t")
						}
						msg.append("\n")
						broadcast("$msg", roomInfo.roomID, pInfo)
					}
				}
			}
		// ADMIN: Admin Login
		if(message[0]=="adminlogin") {
			// Ignore it if already logged in
			if(observerList.contains(client)) return
			if(adminList.contains(client)) return
			if(playerInfoMap.containsKey(client)) return

			val strRemoteAddr = getHostFull(client)

			// Check version
			val serverVer = GameManager.versionMajor
			val clientVer = java.lang.Float.parseFloat(message[1])
			if(serverVer!=clientVer) {
				val strLogMsg = "$strRemoteAddr has tried to access admin, but client version is different ($clientVer)"
				log.warn(strLogMsg)
				throw NetServerDisconnectRequestedException(strLogMsg)
			}

			// Build type check
			val serverBuildType = GameManager.isDevBuild
			val clientBuildType = java.lang.Boolean.parseBoolean(message[4])
			if(serverBuildType!=clientBuildType) {
				val strLogMsg =
					"$strRemoteAddr has tried to access admin, but build type is different (IsDevBuild:$clientBuildType)"
				log.warn(strLogMsg)
				throw NetServerDisconnectRequestedException(strLogMsg)
			}

			// Check username and password
			val strServerUsername = propServer.getProperty("netserver.admin.username", "")
			val strServerPassword = propServer.getProperty("netserver.admin.password", "")
			if(strServerUsername.isEmpty()||strServerPassword.isEmpty()) {
				log.warn("$strRemoteAddr has tried to access admin, but admin is disabled")
				send(client, "adminloginfail\tDISABLE\n")
				return
			}

			val strClientUsername = message[2]
			if(strClientUsername!=strServerUsername) {
				log.warn("$strRemoteAddr has tried to access admin with incorrect username ($strClientUsername)")
				send(client, "adminloginfail\tFAIL\n")
				return
			}

			val rc4 = RC4(strServerPassword)
			val bPass = Base64Coder.decode(message[3])
			val bPass2 = rc4.rc4(bPass)
			val strClientPasswordCheckData = NetUtil.bytesToString(bPass2)
			if(strClientPasswordCheckData!=strServerUsername) {
				log.warn("$strRemoteAddr has tried to access admin with incorrect password (Username:$strClientUsername)")
				send(client, "adminloginfail\tFAIL\n")
				return
			}

			// Kill dead connections
			killTimeoutConnections(timeoutTime)

			// Login successful
			adminList.add(client)
			send(client, "adminloginsuccess\t${getHostAddress(client)}\t${getHostName(client)}\n")
			adminSendClientList()
			sendRoomList(client)
			log.info("Admin has logged in ($strRemoteAddr)")
		}
		// ADMIN: Admin commands
		if(message[0]=="admin")
			if(adminList.contains(client)) {
				val strAdminCommandTemp = NetUtil.decompressString(message[1])
				val strAdminCommandArray = strAdminCommandTemp.split("\t".toRegex()).dropLastWhile {it.isEmpty()}.toTypedArray()
				processAdminCommand(client, strAdminCommandArray)
			} else {
				log.warn(getHostFull(client)+" has tried to access admin command without login")
				logout(client)
				return
			}
	}

	/** Process admin command
	 * @param client The SocketChannel who sent this packet
	 * @param message The String array of the command
	 * @throws IOException When something bad happens
	 */
	@Throws(IOException::class)
	private fun processAdminCommand(client:SocketChannel, message:Array<String>) {
		// Client list (force update)
		if(message[0]=="clientlist") adminSendClientList(client)
		// Ban
		if(message[0]=="ban") {
			// ban\t[IP]\t(Length)
			val kickCount:Int

			var banLength = -1
			if(message.size>2) banLength = Integer.parseInt(message[2])

			kickCount = ban(message[1], banLength)
			saveBanList()

			sendAdminResult(client, "ban\t${message[1]}\t$banLength\t$kickCount")
		}
		// Un-Ban
		if(message[0]=="unban") {
			// unban\t[IP]
			var count = 0

			if(message[1].equals("ALL", ignoreCase = true)) {
				count = banList!!.size
				banList!!.clear()
			} else {
				val tempList = LinkedList<NetServerBan>()
				banList?.let {tempList += it}

				for(ban in tempList)
					if(ban.addr==message[1]) {
						banList!!.remove(ban)
						count++
					}
			}
			saveBanList()

			sendAdminResult(client, "unban\t${message[1]}\t$count")
		}
		// Ban List
		if(message[0]=="banlist") {
			// Cleanup expired bans
			val tempList = LinkedList<NetServerBan>()
			banList?.let {
				tempList += it

				for(ban in tempList)
					if(ban.isExpired) it.remove(ban)

				// Create list
				val strResult = StringBuilder()
				for(ban in it)
					strResult.append("\t").append(ban.exportString())
				sendAdminResult(client, "banlist$strResult")
			}
		}
		// Player delete
		if(message[0]=="playerdelete") {
			// playerdelete\t<Name>

			val strName = message[1]
			val pInfo = searchPlayerByName(strName)

			var playerDataChange = false
			var mpRankingDataChange = false
			var spRankingDataChange = false

			for(i in 0 until GameEngine.MAX_GAMESTYLE) {
				if(propPlayerData.getProperty("p.rating.$i.$strName")!=null) {
					propPlayerData.setProperty("p.rating.$i.$strName", ratingDefault)
					propPlayerData.setProperty("p.playCount.$i.$strName", 0)
					propPlayerData.setProperty("p.winCount.$i.$strName", 0)
					playerDataChange = true
				}
				if(propPlayerData.getProperty("sppersonal.$strName.numRecords")!=null) {
					propPlayerData.setProperty("sppersonal.$strName.numRecords", 0)
					playerDataChange = true
				}

				if(pInfo!=null) {
					pInfo.rating[i] = ratingDefault
					pInfo.playCount[i] = 0
					pInfo.winCount[i] = 0
					pInfo.spPersonalBest.listRecord.clear()
				}

				val mpIndex = mpRankingIndexOf(i, strName)
				if(mpIndex!=-1) {
					mpRankingList!![i].removeAt(mpIndex)
					mpRankingDataChange = true
				}

				for(ranking in spRankingListAlltime!!) {
					val record = ranking.getRecord(strName)
					if(record!=null) {
						ranking.listRecord.remove(record)
						spRankingDataChange = true
					}
				}
				for(ranking in spRankingListDaily!!) {
					val record = ranking.getRecord(strName)
					if(record!=null) {
						ranking.listRecord.remove(record)
						spRankingDataChange = true
					}
				}
			}

			sendAdminResult(client, "playerdelete\t$strName")

			if(playerDataChange) writePlayerDataToFile()
			if(mpRankingDataChange) writeMPRankingToFile()
			if(spRankingDataChange) writeSPRankingToFile()
		}
		// Room delete
		if(message[0]=="roomdelete") {
			// roomdelete\t[ID]
			val roomID = Integer.parseInt(message[1])
			val roomInfo = getRoomInfo(roomID)

			if(roomInfo!=null) {
				val strRoomName = roomInfo.strName
				forceDeleteRoom(roomInfo)
				sendAdminResult(client, "roomdeletesuccess\t$roomID\t$strRoomName")
			} else
				sendAdminResult(client, "roomdeletefail\t$roomID")
		}
		// Shutdown
		if(message[0]=="shutdown") {
			log.warn("Shutdown requested by the admin (${getHostFull(client)})")
			shutdownRequested = true
			selector!!.wakeup()
		}
		// Announce
		if(message[0]=="announce")
		// announce\t[Message]
			broadcast("announce\t${message[1]}\n")
	}

	/** Send admin command result
	 * @param client The admin
	 * @param msg Message to send
	 */
	private fun sendAdminResult(client:SocketChannel, msg:String) {
		send(client, "adminresult\t${NetUtil.compressString(msg)}\n")
	}

	/** Broadcast admin command result to all admins
	 * @param msg Message to send
	 */
	private fun broadcastAdminResult(msg:String) {
		broadcastAdmin("adminresult\t${NetUtil.compressString(msg)}\n")
	}

	/** Send client list to admin
	 * @param client The admin. If null, it will broadcast to all admins.
	 */
	private fun adminSendClientList(client:SocketChannel? = null) {
		val strMsg = StringBuilder("clientlist")

		for(ch in channelList) {
			val strIP = getHostAddress(ch)
			val strHost = getHostName(ch)
			val pInfo = playerInfoMap[ch]

			var type = 0 // Type of client. 0:Not logged in
			if(pInfo!=null)
				type = 1 // 1:Player
			else if(observerList.contains(ch))
				type = 2 // 2:Observer
			else if(adminList.contains(ch)) type = 3 // 3:Admin

			var strClientData = "$strIP|$strHost|$type"
			if(pInfo!=null) strClientData += "|${pInfo.exportString()}"

			strMsg.append("\t").append(strClientData)
		}

		if(client==null)
			broadcastAdminResult("$strMsg")
		else
			sendAdminResult(client, "$strMsg")
	}

	/** Get NetRoomInfo by using roomID
	 * @param roomID Room ID
	 * @return NetRoomInfo (null if not found)
	 */
	private fun getRoomInfo(roomID:Int):NetRoomInfo? {
		if(roomID==-1) return null

		for(roomInfo in roomInfoList)
			if(roomID==roomInfo.roomID) return roomInfo

		return null
	}

	/** Send room list to specified client
	 * @param client Client to send
	 */
	private fun sendRoomList(client:SocketChannel) {
		val msg = StringBuilder("roomlist\t"+roomInfoList.size)

		for(roomInfo in roomInfoList) {
			msg.append("\t")
			msg.append(roomInfo.exportString())
		}

		msg.append("\n")
		send(client, "$msg")
	}

	/** Delete a room
	 * @param roomInfo Room to delete
	 * @return true if success, false if fails (room not empty)
	 */
	private fun deleteRoom(roomInfo:NetRoomInfo?):Boolean {
		if(roomInfo!=null&&roomInfo.playerList.isEmpty()) {
			log.info("RoomDelete ID:${roomInfo.roomID} Title:${roomInfo.strName}")
			broadcastRoomInfoUpdate(roomInfo, "roomdelete")
			roomInfoList.remove(roomInfo)
			roomInfo.delete()
			return true
		}
		return false
	}

	/** Force delete a room
	 * @param roomInfo Room to delete
	 * @throws IOException If something bad happens
	 */
	@Throws(IOException::class)
	private fun forceDeleteRoom(roomInfo:NetRoomInfo?) {
		if(roomInfo!=null) {
			if(!roomInfo.playerList.isEmpty()) {
				val tempList = LinkedList(roomInfo.playerList)
				for(pInfo in tempList) {
					val client = getSocketChannelByPlayer(pInfo)
					if(client!=null) {
						// Packet simulation :p
						processPacket(client, "roomjoin\t-1\tfalse")
						// Send message to the kicked player
						send(client, "roomkicked\t0\t${roomInfo.roomID}\t${NetUtil.urlEncode(roomInfo.strName)}\n")
					}
				}
				roomInfo.playerList.clear()
			}
			deleteRoom(roomInfo)
		}
	}

	/** Start/Stop auto start timer. It also turn-off the Ready status if there
	 * is only 1 player.
	 * @param roomInfo The room
	 */
	private fun autoStartTimerCheck(roomInfo:NetRoomInfo) {
		if(roomInfo.autoStartSeconds<=0) return

		val minPlayers = if(roomInfo.autoStartTNET2) 2 else 1

		// Stop
		if(roomInfo.numberOfPlayerSeated<=1||roomInfo.isSomeoneCancelled&&roomInfo.disableTimerAfterSomeoneCancelled
			||roomInfo.howManyPlayersReady<minPlayers
			||roomInfo.howManyPlayersReady<roomInfo.numberOfPlayerSeated/2) {
			if(roomInfo.autoStartActive) broadcast("autostartstop\n", roomInfo.roomID)
			roomInfo.autoStartActive = false
		} else if(!roomInfo.autoStartActive&&(!roomInfo.isSomeoneCancelled||!roomInfo.disableTimerAfterSomeoneCancelled)
			&&roomInfo.howManyPlayersReady>=minPlayers
			&&roomInfo.howManyPlayersReady>=roomInfo.numberOfPlayerSeated/2) {
			broadcast("autostartbegin\t${roomInfo.autoStartSeconds}\n", roomInfo.roomID)
			roomInfo.autoStartActive = true
		}// Start

		// Turn-off ready status if there is only 1 player
		if(roomInfo.numberOfPlayerSeated==1)
			for(p in roomInfo.playerSeat)
				if(p.ready) {
					p.ready = false
					broadcastPlayerInfoUpdate(p)
				}
	}

	/** Start a game if possible
	 * @param roomInfo The room
	 * @return true if started, false if not
	 */
	private fun gameStartIfPossible(roomInfo:NetRoomInfo):Boolean {
		if(roomInfo.howManyPlayersReady==roomInfo.numberOfPlayerSeated&&roomInfo.numberOfPlayerSeated>=2) {
			gameStart(roomInfo)
			return true
		}
		return false
	}

	/** Start a game (force start)
	 * @param roomInfo The room
	 */
	private fun gameStart(roomInfo:NetRoomInfo?) {
		if(roomInfo==null) return
		if(roomInfo.numberOfPlayerSeated<=0) return
		if(roomInfo.numberOfPlayerSeated<=1&&!roomInfo.singleplayer) return
		if(roomInfo.playing) return

		roomInfo.gameStart()

		var mapNo = 0
		val mapMax = roomInfo.mapList.size
		if(roomInfo.useMap&&mapMax>0) {
			do mapNo = rand.nextInt(mapMax) while(mapNo==roomInfo.mapPrevious&&mapMax>=2)

			roomInfo.mapPrevious = mapNo
		}
		val msg = "start\t${rand.nextLong().toString(16)}\t${roomInfo.startPlayers}\t$mapNo\n"
		broadcast(msg, roomInfo.roomID)

		for(p in roomInfo.playerSeat) {
			p.ready = false
			p.playing = true
			p.playCountNow++

			// If ranked room
			if(roomInfo.rated&&!roomInfo.isTeamGame&&(!roomInfo.hasSameIPPlayers()||ratingAllowSameIP)) {
				p.playCount[roomInfo.style]++
				p.ratingBefore[roomInfo.style] = p.rating[roomInfo.style]
			}

			broadcastPlayerInfoUpdate(p)
		}
		roomInfo.playing = true
		roomInfo.autoStartActive = false
		broadcastRoomInfoUpdate(roomInfo)
	}

	/** Check if the game is finished. If finished, it will notify players.
	 * @param roomInfo The room
	 * @return true if finished
	 */
	private fun gameFinished(roomInfo:NetRoomInfo):Boolean {
		val startPlayers = roomInfo.startPlayers
		val nowPlaying = roomInfo.howManyPlayersPlaying
		val isTeamWin = roomInfo.isTeamWin

		if(roomInfo.playing&&(nowPlaying<1||startPlayers>=2&&nowPlaying<2||isTeamWin)) {
			// Game finished
			val winner = roomInfo.winner
			var msg = "finish\t"

			if(isTeamWin) {
				// Winner is a team
				var teamName = roomInfo.winnerTeam
				if(teamName==null) teamName = ""
				msg += "${(-1)}\t${-1}\t${NetUtil.urlEncode(teamName)}\t$isTeamWin"

				for(pInfo in roomInfo.playerSeat)
					if(pInfo.playing) {
						pInfo.resetPlayState()
						pInfo.winCountNow++
						broadcastPlayerInfoUpdate(pInfo)
						roomInfo.playerSeatDead.addFirst(pInfo)

						// Rated game
						/* if(roomInfo.rated) {
						 * // TODO: Update ratings?
						 * pInfo.winCount[roomInfo.style]++;
						 * setPlayerDataToProperty(pInfo);
						 * } */
					}

				/* if(roomInfo.rated) {
				 * writePlayerDataToFile();
				 * } */
			} else if(winner!=null&&!roomInfo.singleplayer) {
				// Winner is a player
				roomInfo.playerSeatDead.addFirst(winner)

				// Rated game
				if(roomInfo.rated&&!roomInfo.isTeamGame&&(!roomInfo.hasSameIPPlayers()||ratingAllowSameIP)) {
					// Update win count
					winner.winCount[roomInfo.style]++

					// Update rating
					val style = roomInfo.style
					val n = roomInfo.playerSeatDead.size
					for(w in 0 until n-1)
						for(l in w+1 until n) {
							val wp = roomInfo.playerSeatDead[w]
							val lp = roomInfo.playerSeatDead[l]

							wp.rating[style] += (rankDelta(wp.playCount[style], wp.rating[style].toDouble(), lp.rating[style].toDouble(), 1.0)/(n-1)).toInt()
							lp.rating[style] += (rankDelta(lp.playCount[style], lp.rating[style].toDouble(), wp.rating[style].toDouble(), 0.0)/(n-1)).toInt()

							if(wp.rating[style]<ratingMin) wp.rating[style] = ratingMin
							if(lp.rating[style]<ratingMin) lp.rating[style] = ratingMin
							if(wp.rating[style]>ratingMax) wp.rating[style] = ratingMax
							if(lp.rating[style]>ratingMax) lp.rating[style] = ratingMax
						}

					// Notify/Save
					for(i in 0 until n) {
						val p = roomInfo.playerSeatDead[i]
						val change = p.rating[style]-p.ratingBefore[style]
						log.debug("#${i+1} Name:${p.strName} Rating:${p.rating[style]} ($change)")
						setPlayerDataToProperty(p)

						val msgRatingChange =
							"rating\t${p.uid}\t${p.seatID}\t${NetUtil.urlEncode(p.strName)}\t${p.rating[style]}\t$change\n"
						broadcast(msgRatingChange, winner.roomID)
					}
					writePlayerDataToFile()

					// Leaderboard update
					for(i in 0 until n) {
						val p = roomInfo.playerSeatDead[i]
						if(p.isTripUse) mpRankingUpdate(style, p)
					}
					writeMPRankingToFile()
				}

				msg += "${winner.uid}\t${winner.seatID}\t${NetUtil.urlEncode(winner.strName)}\t$isTeamWin"
				winner.resetPlayState()
				winner.winCountNow++
				broadcastPlayerInfoUpdate(winner)
			} else
			// No winner(s)
				msg += "${(-1)}\t${-1}\t\t$isTeamWin"
			msg += "\n"
			broadcast(msg, roomInfo.roomID)

			roomInfo.playing = false
			roomInfo.autoStartActive = false
			broadcastRoomInfoUpdate(roomInfo)

			return true
		}

		return false
	}

	/** Broadcast a room update information
	 * @param roomInfo The room
	 * @param command Command
	 */
	private fun broadcastRoomInfoUpdate(roomInfo:NetRoomInfo, command:String = "roomupdate") {
		roomInfo.updatePlayerCount()
		var msg = command+"\t"
		msg += roomInfo.exportString()
		msg += "\n"
		broadcast(msg)
		broadcastAdmin(msg)
	}

	/** Send player list to specified client
	 * @param client Client to send
	 */
	private fun sendPlayerList(client:SocketChannel) {
		val msg = StringBuilder("playerlist\t${playerInfoMap.size}")

		channelList.forEach {ch ->
			val pInfo = playerInfoMap[ch]

			if(pInfo!=null) {
				msg.append("\t${pInfo.exportString()}")
			}
		}

		msg.append("\n")
		send(client, "$msg")
	}

	/** Broadcast a player update information
	 * @param pInfo The player
	 * @param command Command
	 */
	private fun broadcastPlayerInfoUpdate(pInfo:NetPlayerInfo, command:String = "playerupdate") {
		broadcast("command$\t${pInfo.exportString()}\n")
	}

	/** Get NetPlayerInfo by player's name
	 * @param name Name
	 * @return NetPlayerInfo (null if not found)
	 */
	private fun searchPlayerByName(name:String):NetPlayerInfo? =
		channelList.map {playerInfoMap[it]}
			.firstOrNull {it?.strName==name}

	/** Get NetPlayerInfo by player's ID
	 * @param uid ID
	 * @return NetPlayerInfo (null if not found)
	 */
	private fun searchPlayerByUID(uid:Int):NetPlayerInfo? =
		channelList.map {playerInfoMap[it]}.firstOrNull {it?.uid==uid}

	/** Move queue player(s) to the game seat if possible
	 * @param roomInfo The room
	 * @return Number of players moved to the game seat
	 */
	private fun joinAllQueuePlayers(roomInfo:NetRoomInfo):Int {
		var playerJoinedCount = 0

		while(roomInfo.canJoinSeat()&&!roomInfo.playerQueue.isEmpty()) {
			val pInfo = roomInfo.playerQueue.poll()
			pInfo.seatID = roomInfo.joinSeat(pInfo)
			pInfo.queueID = -1
			pInfo.ready = false
			broadcast(
				"changestatus\tjoinseat\t${pInfo.uid}\t${NetUtil.urlEncode(pInfo.strName)}\t${pInfo.seatID}\n", pInfo.roomID)
			broadcastPlayerInfoUpdate(pInfo)
			playerJoinedCount++
		}

		if(playerJoinedCount>0) broadcastRoomInfoUpdate(roomInfo)

		return playerJoinedCount
	}

	/** Signal player-dead
	 * @param pInfo Player
	 * @param pKOInfo Assailant (can be null)
	 */
	private fun playerDead(pInfo:NetPlayerInfo, pKOInfo:NetPlayerInfo? = null) {
		getRoomInfo(pInfo.roomID)?.let {roomInfo ->
			if(pInfo.seatID!=-1&&pInfo.playing&&roomInfo.playing) {
				pInfo.resetPlayState()

				val place = roomInfo.startPlayers-roomInfo.deadCount
				var msg = "dead\t${pInfo.uid}\t${NetUtil.urlEncode(pInfo.strName)}\t${pInfo.seatID}\t$place\t"
				msg += if(pKOInfo==null) "${(-1)}\t"
				else "${pKOInfo.uid}\t${NetUtil.urlEncode(pKOInfo.strName)}"
				msg += "\n"
				broadcast(msg, pInfo.roomID)

				roomInfo.deadCount++
				roomInfo.playerSeatDead.addFirst(pInfo)
				gameFinished(roomInfo)

				broadcastPlayerInfoUpdate(pInfo)
			}
		}
	}

	/** Sets a ban by IP address.
	 * @param strIP IP address
	 * @param banLength The length of the ban. (-1: Kick only, not ban)
	 * @return Number of players kicked
	 */
	private fun ban(strIP:String, banLength:Int):Int {
		val banChannels = LinkedList<SocketChannel>()

		for(ch in channelList) {
			val ip = getHostAddress(ch)
			if(ip==strIP) banChannels.add(ch)
		}
		for(ch in banChannels)
			ban(ch, banLength)
		if(banChannels.isEmpty()&&banLength>=0)
		// Add ban entry manually
			banList?.add(NetServerBan(strIP, banLength))

		return banChannels.size
	}

	/** Sets a ban.
	 * @param client The remote address to ban.
	 * @param banLength The length of the ban. (-1: Kick only, not ban)
	 * @return Number of players kicked (always 1 in this routine)
	 */
	private fun ban(client:SocketChannel, banLength:Int):Int {
		val remoteAddr = getHostAddress(client)

		if(banLength<0)
			log.info("Kicked player: $remoteAddr")
		else {
			banList?.add(NetServerBan(remoteAddr, banLength))
			log.info("Banned player: $remoteAddr")
		}

		logout(client)
		return 1
	}

	/** Checks whether a connection is banned.
	 * @param client The remote address to check.
	 * @return true if the connection is banned, false if it is not banned or if
	 * the ban
	 * is expired.
	 */
	private fun checkConnectionOnBanlist(client:SocketChannel):Boolean = getBan(client)!=null

	/** Get ban data of the connection.
	 * @param client The remote address to check.
	 * @return An instance of NetServerBan is the connection is banned, null
	 * otherwise.
	 */
	private fun getBan(client:SocketChannel):NetServerBan? {
		val remoteAddr = getHostAddress(client)

		val i = banList!!.iterator()
		var ban:NetServerBan

		while(i.hasNext()) {
			ban = i.next()
			if(ban.addr==remoteAddr)
				if(ban.isExpired) i.remove()
				else return ban
		}

		return null
	}

	/** Send rated-game rule list
	 * @param client Client
	 */
	private fun sendRatedRuleList(client:SocketChannel) {
		for(style in 0 until GameEngine.MAX_GAMESTYLE) {
			val msg = StringBuilder("rulelist\t$style")

			for(i in 0 until ruleList!![style].size) {
				val tempObj = ruleList!![style][i]

				msg.append("\t").append(NetUtil.urlEncode(tempObj.strRuleName))

			}

			msg.append("\n")
			send(client, "$msg")
		}
		//send(client, "rulelistend\n");
	}

	/** Get rated-game rule
	 * @param style Style ID
	 * @param name Rule Name
	 * @return Rated-game rule (null if not found)
	 */
	private fun getRatedRule(style:Int, name:String) = ruleList?.get(style)?.firstOrNull {it.strRuleName==name}

	/*
	/** Get rated-game rule index
	 * @param style Style ID
	 * @param name Rule Name
	 * @return Index (-1 if not found)
	 */private int getRatedRuleIndex(int style, String name) {
	 * for(int i = 0; i < ruleList[style].size(); i++) {
	 * RuleOptions rule = (RuleOptions)ruleList[style].get(i);
	 * if(name.equals(rule.strRuleName)) {
	 * return i;
	 * }
	 * }
	 * return -1;
	 * } */

	/** Get new rating
	 * @param playedGames Number of games played by the player
	 * @param myRank Player's rating
	 * @param oppRank Opponent's rating
	 * @param myScore 0:Loss, 1:Win
	 * @return New rating
	 */
	private fun rankDelta(playedGames:Int, myRank:Double, oppRank:Double, myScore:Double):Double =
		maxDelta(playedGames)*(myScore-expectedScore(myRank, oppRank))

	/** Subroutine of rankDelta; Returns expected score.
	 * @param myRank Player's rating
	 * @param oppRank Opponent's rating
	 * @return Expected score
	 */
	private fun expectedScore(myRank:Double, oppRank:Double):Double = 1.0/(1+10.0.pow((oppRank-myRank)/400.0))

	/** Subroutine of rankDelta; Returns multiplier of rating change
	 * @param playedGames Number of games played by the player
	 * @return Multiplier of rating change
	 */
	private fun maxDelta(playedGames:Int):Double =
		if(playedGames>ratingProvisionalGames) ratingNormalMaxDiff else ratingNormalMaxDiff+400/(playedGames+3)

	/** Write server-status file */
	private fun writeServerStatusFile() {
		if(!propServer.getProperty("netserver.writestatusfile", false)) return

		var status = propServer.getProperty("netserver.statusformat", "\$observers/\$players")

		status = status.replace("\\\$version".toRegex(), GameManager.versionMajor.toString())
		status = status.replace("\\\$observers".toRegex(), observerList.size.toString())
		status = status.replace("\\\$players".toRegex(), playerInfoMap.size.toString())
		status = status.replace("\\\$clients".toRegex(), (observerList.size+playerInfoMap.size).toString())
		status = status.replace("\\\$rooms".toRegex(), roomInfoList.size.toString())

		try {
			val outFile = FileWriter(propServer.getProperty("netserver.statusfilename", "status.txt"))
			val out = PrintWriter(outFile)
			out.println(status)
			out.close()
		} catch(e:IOException) {
			e.printStackTrace()
		}

	}

	/** Pending changes */
	private class ChangeRequest(val socket:SocketChannel, val type:Int, val ops:Int) {
		companion object {
			/** Delayed disconnect action */
			const val DISCONNECT = 1

			/** interestOps change action */
			const val CHANGEOPS = 2
		}
	}

	companion object {
		/** Log */
		internal val log = Logger.getLogger(NetServer::class.java)

		/** Default port number */
		const val DEFAULT_PORT = 9200

		/** Read buffer size */
		const val BUF_SIZE = 8192

		/** Rule data send buffer size */
		const val RULE_BUF_SIZE = 512

		/** Default value of ratingNormalMaxDiff */
		const val NORMAL_MAX_DIFF = 16.0

		/** Default value of ratingProvisionalGames */
		const val PROVISIONAL_GAMES = 50

		/** Default value of maxMPRanking */
		const val DEFAULT_MAX_MPRANKING = 100

		/** Default value of maxSPRanking */
		const val DEFAULT_MAX_SPRANKING = 100

		/** Default minimum gamerate */
		const val DEFAULT_MIN_GAMERATE = 80f

		/** Default time of timeout */
		const val DEFAULT_TIMEOUT_TIME = (1000*60).toLong()

		/** Default number of lobby chat histories */
		const val DEFAULT_MAX_LOBBYCHAT_HISTORY = 10

		/** Default number of room chat histories */
		const val DEFAULT_MAX_ROOMCHAT_HISTORY = 10

		/** Server config file */
		private var propServer:CustomProperties = CustomProperties()

		/** Server Rated presets file */
		private var propPresets:CustomProperties = CustomProperties()

		/** Properties of player data list (mainly for rating) */
		private var propPlayerData:CustomProperties = CustomProperties()

		/** Properties of multiplayer leaderboard */
		private var propMPRanking:CustomProperties = CustomProperties()

		/** Properties of single player all-time leaderboard */
		private var propSPRankingAlltime:CustomProperties = CustomProperties()

		/** Properties of single player daily leaderboard */
		private var propSPRankingDaily:CustomProperties = CustomProperties()

		/** Properties of single player personal best */
		private var propSPPersonalBest:CustomProperties = CustomProperties()

		/** True to allow hostname display (If false, it will display IP only) */
		private var allowDNSAccess:Boolean = false

		/** Timeout time (0=Disable) */
		private var timeoutTime:Long = 0

		/** Client's ping interval */
		private var clientPingInterval:Long = 0

		/** Default rating */
		private var ratingDefault:Int = 0

		/** The maximum possible adjustment per game. (K-value) */
		private var ratingNormalMaxDiff:Double = 0.toDouble()

		/** After playing this number of games, the rating logic will take account
		 * of number of games played. */
		private var ratingProvisionalGames:Int = 0

		/** Min/Max range of rating */
		private var ratingMin:Int = 0
		private var ratingMax:Int = 0

		/** Allow same IP player for rating change */
		private var ratingAllowSameIP:Boolean = false

		/** Max entry of multiplayer leaderboard */
		private var maxMPRanking:Int = 0

		/** Max entry of singleplayer leaderboard */
		private var maxSPRanking:Int = 0

		/** TimeZone of daily single player leaderboard */
		private var spDailyTimeZone:String? = null

		/** Minimum game rate of single player leaderboard */
		private var spMinGameRate:Float = 0f

		/** Max entry of lobby chat history */
		private var maxLobbyChatHistory:Int = 0

		/** Max entry of room chat history */
		private var maxRoomChatHistory:Int = 0

		/** Rated room info presets (compressed NetRoomInfo Strings) */
		private var ratedInfoList:LinkedList<String>? = null

		/** Rule list for rated game. */
		private var ruleList:Array<LinkedList<RuleOptions>>? = null

		/** Setting ID list for rated game. */
		private var ruleSettingIDList:Array<LinkedList<Int>>? = null

		/** Multiplayer leaderboard list. */
		private var mpRankingList:Array<LinkedList<NetPlayerInfo>>? = null

		/** Multiplayer mode list */
		private var mpModeList:Array<LinkedList<String>>? = null

		/** Multiplayer race mode flag */
		private var mpModeIsRace:Array<LinkedList<Boolean>>? = null

		/** Single player mode list. */
		private var spModeList:Array<LinkedList<String>>? = null

		/** Single player all-time leaderboard list */
		private var spRankingListAlltime:LinkedList<NetSPRanking>? = null

		/** Single player daily leaderboard list */
		private var spRankingListDaily:LinkedList<NetSPRanking>? = null

		/** Last-update time of single player daily leaderboard */
		private var spDailyLastUpdate:Calendar? = null

		/** Ban list */
		private var banList:LinkedList<NetServerBan>? = null

		/** Lobby chat message history */
		private var lobbyChatList:LinkedList<NetChatMessage>? = LinkedList()

		/** Load rated-game room presets from the server config */
		private fun loadPresetList() {
			propPresets = CustomProperties()
			try {
				val `in` = FileInputStream("config/etc/netserver_presets.cfg")
				propPresets.load(`in`)
				`in`.close()
			} catch(e:IOException) {
				log.warn("Failed to load config file", e)
			}

			ratedInfoList = LinkedList()

			var strInfo:String? = ""
			var i = 0
			while(strInfo!=null) { //Iterate over the available presets in the server config
				strInfo = propPresets.getProperty("0.preset."+i++)
				if(strInfo!=null) ratedInfoList!!.add(strInfo)
			}
			log.info("Loaded ${ratedInfoList!!.size} presets.")
		}

		/** Load rated-game rule list */
		private fun loadRuleList() {
			log.info("Loading Rule List...")

			ruleList = Array(GameEngine.MAX_GAMESTYLE) {LinkedList<RuleOptions>()}
			ruleSettingIDList = Array(GameEngine.MAX_GAMESTYLE) {LinkedList<Int>()}

			try {
				val txtRuleList = BufferedReader(FileReader("config/etc/netserver_rulelist.lst"))
				var style = 0

				txtRuleList.readLines().forEach {str ->
					if(str.isEmpty()||str.startsWith("#")) {
						// Empty or a comment line. Do nothing.
					} else if(str.startsWith(":")) {
						// Game style
						val strStyle = str.substring(1)

						style = -1
						for(i in 0 until GameEngine.MAX_GAMESTYLE)
							if(strStyle.equals(GameEngine.GAMESTYLE_NAMES[i], ignoreCase = true)) {
								style = i
								break
							}

						if(style==-1) {
							log.warn("{StyleChange} Unknown Style:$str")
							style = 0
						} else
							log.debug("{StyleChange} StyleID:$style StyleName:$strStyle")
					} else
					// Rule file
						try {
							var settingID = 0
							val strTempArray = str.split(";".toRegex()).dropLastWhile {it.isEmpty()}.toTypedArray()
							if(strTempArray.size>1) settingID = Integer.parseInt(strTempArray[1])

							log.debug("{RuleLoad} StyleID:$style RuleFile:${strTempArray[0]} SettingID:"+settingID)

							val `in` = FileInputStream(strTempArray[0])
							val prop = CustomProperties()
							prop.load(`in`)
							`in`.close()

							val rule = RuleOptions()
							rule.readProperty(prop, 0)

							ruleList!![style].add(rule)
							ruleSettingIDList!![style].add(settingID)
						} catch(e2:Exception) {
							log.warn("Failed to load rule file", e2)
						}
				}

				txtRuleList.close()
			} catch(e:Exception) {
				log.warn("Failed to load rule list", e)
			}

		}

		/** Load multiplayer leaderboard */
		private fun loadMPRankingList() {
			// Load mode list
			mpModeList = Array(GameEngine.MAX_GAMESTYLE) {LinkedList<String>()}
			mpModeIsRace = Array(GameEngine.MAX_GAMESTYLE) {LinkedList<Boolean>()}

			try {
				val `in` = BufferedReader(FileReader("config/list/netlobby_multimode.lst"))

				var style = 0

				`in`.readLines().forEach {str ->
					if(str.isEmpty()||str.startsWith("#")) {
						// Empty line or comment line. Ignore it.
					} else if(str.startsWith(":")) {
						// Game style tag
						val strStyle = str.substring(1)

						style = -1
						for(i in 0 until GameEngine.MAX_GAMESTYLE)
							if(strStyle.equals(GameEngine.GAMESTYLE_NAMES[i], ignoreCase = true)) {
								style = i
								break
							}

						if(style==-1) style = 0
					} else {
						// Game mode name
						val strSplit = str.split(",".toRegex()).dropLastWhile {it.isEmpty()}.toTypedArray()
						val strModeName = strSplit[0]
						var isRace = false
						if(strSplit.size>1) isRace = java.lang.Boolean.parseBoolean(strSplit[1])

						mpModeList!![style].add(strModeName)
						mpModeIsRace!![style].add(isRace)
					}
				}

				`in`.close()
			} catch(e:Exception) {
				log.warn("Failed to load multiplayer mode list", e)
			}

			// Load leaderboard
			log.info("Loading Multiplayer Ranking...")
			mpRankingList = Array(GameEngine.MAX_GAMESTYLE) {LinkedList<NetPlayerInfo>()}

			for(style in 0 until GameEngine.MAX_GAMESTYLE) {
				var count = propMPRanking.getProperty("$style.mpranking.count", 0)
				if(count>maxMPRanking) count = maxMPRanking

				for(i in 0 until count) {
					val p = NetPlayerInfo().apply {
						strName = propMPRanking.getProperty("$style.mpranking.strName.$i", "")
						rating[style] = propMPRanking.getProperty("$style.mpranking.rating.$i", ratingDefault)
						playCount[style] = propMPRanking.getProperty("$style.mpranking.playCount.$i", 0)
						winCount[style] = propMPRanking.getProperty("$style.mpranking.winCount.$i", 0)
					}
					mpRankingList!![style].add(p)
				}
			}
		}

		/** Find a player in multiplayer leaderboard.
		 * @param style Game Style
		 * @param p NetPlayerInfo (can be null, returns -1 if so)
		 * @return Index in mpRankingList[style] (-1 if not found)
		 */
		private fun mpRankingIndexOf(style:Int, p:NetPlayerInfo?):Int = if(p==null) -1 else mpRankingIndexOf(style, p.strName)

		/** Find a player in multiplayer leaderboard.
		 * @param style Game Style
		 * @param name Player name in String (can be null, returns -1 if so)
		 * @return Index in mpRankingList[style] (-1 if not found)
		 */
		private fun mpRankingIndexOf(style:Int, name:String?):Int {
			if(name==null) return -1
			for(i in 0 until mpRankingList!![style].size) {
				val p2 = mpRankingList!![style][i]
				if(name==p2.strName) return i
			}
			return -1
		}

		/** Update multiplayer leaderboard.
		 * @param style Game Style
		 * @param p NetPlayerInfo
		 * @return New place (-1 if not ranked)
		 */
		private fun mpRankingUpdate(style:Int, p:NetPlayerInfo):Int {
			// Remove existing record
			val prevRecord = mpRankingIndexOf(style, p)
			if(prevRecord!=-1) mpRankingList!![style].removeAt(prevRecord)

			// Insert new record
			var place = -1
			var rankin = false
			for(i in 0 until mpRankingList!![style].size) {
				val p2 = mpRankingList!![style][i]
				if(p.rating[style]>p2.rating[style]) {
					mpRankingList!![style].add(i, p)
					place = i
					rankin = true
					break
				}
			}

			// Couldn't rank in? Add to last.
			if(!rankin) {
				mpRankingList!![style].addLast(p)
				place = mpRankingList!![style].size-1
			}

			// Remove anything after maxMPRanking
			while(mpRankingList!![style].size>=maxMPRanking) mpRankingList!![style].removeLast()

			// Done
			return if(place>=maxMPRanking) -1 else place
		}

		/** Write player data properties (propPlayerData) to a file */
		private fun writeMPRankingToFile() {
			for(style in 0 until GameEngine.MAX_GAMESTYLE) {
				var count = mpRankingList!![style].size
				if(count>maxMPRanking) count = maxMPRanking
				propMPRanking.setProperty("$style.mpranking.count", count)

				for(i in 0 until count) {
					val p = mpRankingList!![style][i]
					propMPRanking.setProperty("$style.mpranking.strName.$i", p.strName)
					propMPRanking.setProperty("$style.mpranking.rating.$i", p.rating[style])
					propMPRanking.setProperty("$style.mpranking.playCount.$i", p.playCount[style])
					propMPRanking.setProperty("$style.mpranking.winCount.$i", p.winCount[style])
				}
			}

			try {
				val out = GZIPOutputStream(FileOutputStream("config/setting/netserver_mpranking"))
				propMPRanking.storeToXML(out, "NullpoMino NetServer Multiplayer Leaderboard")
				out.close()
			} catch(e:IOException) {
				log.error("Failed to write multiplayer ranking data", e)
			}

		}

		/** Load single player leaderboard */
		private fun loadSPRankingList() {
			log.info("Loading Single Player Ranking...")

			spRankingListAlltime = LinkedList()
			spRankingListDaily = LinkedList()

			// Load mode list
			spModeList = Array(GameEngine.MAX_GAMESTYLE) {LinkedList<String>()}

			// Daily last-update
			val z = if(spDailyTimeZone!!.isNotEmpty()) TimeZone.getTimeZone(spDailyTimeZone) else TimeZone.getDefault()
			spDailyLastUpdate = GeneralUtil.importCalendarString(propSPRankingDaily.getProperty("daily.lastupdate", ""))
			if(spDailyLastUpdate!=null) spDailyLastUpdate!!.timeZone = z

			try {
				val `in` = BufferedReader(FileReader("config/list/netlobby_singlemode.lst"))

				var style = 0

				`in`.readLines().forEach {str ->
					if(str.isEmpty()||str.startsWith("#")) {
						// Empty line or comment line. Ignore it.
					} else if(str.startsWith(":")) {
						// Game style tag
						val strStyle = str.substring(1)

						style = -1
						for(i in 0 until GameEngine.MAX_GAMESTYLE)
							if(strStyle.equals(GameEngine.GAMESTYLE_NAMES[i], true)) {
								style = i
								break
							}

						if(style==-1) {
							log.warn("{StyleChange} Unknown Style:$str")
							style = 0
						} else log.debug("{StyleChange} StyleID:$style StyleName:$strStyle")
					} else {
						// Game mode name
						val strSplit = str.split(",".toRegex()).dropLastWhile {it.isEmpty()}.toTypedArray()
						val strModeName = strSplit[0]
						var rankingType = 0
						var maxGameType = 0
						if(strSplit.size>1) rankingType = Integer.parseInt(strSplit[1])
						if(strSplit.size>2) maxGameType = Integer.parseInt(strSplit[2])

						log.debug("{Mode} Name:$strModeName RankingType:$rankingType MaxGameType:$maxGameType")

						spModeList!![style].add(strModeName)

						for(i in 0 until ruleList!![style].size+1) {
							val ruleName:String
							ruleName = if(i<ruleList!![style].size) {
								val ruleOpt = ruleList!![style][i]
								ruleOpt.strRuleName
							} else "any"

							for(j in 0 until maxGameType+1)
								for(k in 0..1) {
									val rankingData = NetSPRanking()
									rankingData.strModeName = strModeName
									rankingData.strRuleName = ruleName
									rankingData.gameType = j
									rankingData.rankingType = rankingType
									rankingData.style = style
									rankingData.maxRecords = maxSPRanking

									if(k==0) {
										rankingData.readProperty(propSPRankingAlltime)
										spRankingListAlltime!!.add(rankingData)
										log.debug(rankingData.strRuleName+",${rankingData.strModeName},"+rankingData.gameType)
									} else {
										rankingData.readProperty(propSPRankingDaily)
										spRankingListDaily!!.add(rankingData)
									}
								}
						}
					}
				}

				`in`.close()
			} catch(e:Exception) {
				log.warn("Failed to load single player mode list", e)
			}

		}

		/** Get specific NetSPRanking
		 * @param rule Rule Name ("all" to get a merged table)
		 * @param mode Mode Name
		 * @param gtype Game Type
		 * @param isDaily `true` to get daily ranking, `false`
		 * to get all-time ranking
		 * @return NetSPRanking (null if not found)
		 */
		private fun getSPRanking(rule:String, mode:String, gtype:Int, isDaily:Boolean = false):NetSPRanking? {
			if(rule=="all") return getSPRankingAllRules(mode, gtype, isDaily)
			val list = if(isDaily) spRankingListDaily else spRankingListAlltime
			for(r in list!!)
				if(r.strRuleName==rule&&r.strModeName==mode&&r.gameType==gtype) return r
			return null
		}

		/** Get NetSPRanking for all rule types
		 * @param mode Mode Name
		 * @param gtype Game Type
		 * @param isDaily `true` to get daily ranking, `false`
		 * to get all-time ranking
		 * @return NetSPRanking (null if not found or there are none)
		 */
		private fun getSPRankingAllRules(mode:String, gtype:Int, isDaily:Boolean):NetSPRanking {
			val list = if(isDaily) spRankingListDaily else spRankingListAlltime
			val allRanks = LinkedList<NetSPRanking>()
			for(r in list!!)
				if(r.strModeName==mode&&r.gameType==gtype) allRanks.add(r)
			val merged = NetSPRanking.mergeRankings(allRanks)
			merged!!.strRuleName = "all"
			return merged
		}

		/** Update the last-update variable of daily ranking, and wipe the records
		 * if needed
		 * @return `true` if the records are wiped
		 */
		private fun updateSPDailyRanking():Boolean {
			val z = if(spDailyTimeZone!!.isNotEmpty()) TimeZone.getTimeZone(spDailyTimeZone) else TimeZone.getDefault()
			val c = Calendar.getInstance(z)
			val oldLastUpdate = spDailyLastUpdate

			spDailyLastUpdate = c
			propSPRankingDaily.setProperty("daily.lastupdate", GeneralUtil.exportCalendarString(spDailyLastUpdate!!))

			if(oldLastUpdate!=null) log.debug("SP daily ranking previous-update:"+GeneralUtil.getCalendarString(oldLastUpdate))
			log.debug("SP daily ranking last-update:"+GeneralUtil.getCalendarString(c))

			if(oldLastUpdate==null||c.get(Calendar.DATE)==oldLastUpdate.get(Calendar.DATE)) return false

			for(r in spRankingListDaily!!)
				r.listRecord.clear()
			log.info("SP daily ranking wiped")

			return true
		}

		/** Write single player ranking to a file */
		private fun writeSPRankingToFile() {
			// All-time
			for(r in spRankingListAlltime!!)
				r.writeProperty(propSPRankingAlltime)
			try {
				val out = GZIPOutputStream(FileOutputStream("config/setting/netserver_spranking"))
				propSPRankingAlltime.storeToXML(out, "NullpoMino NetServer Single Player All-time Leaderboard")
				out.close()
			} catch(e:IOException) {
				log.error("Failed to write single player all-time ranking data", e)
			}

			// Daily
			for(r in spRankingListDaily!!)
				r.writeProperty(propSPRankingDaily)
			try {
				val out = GZIPOutputStream(FileOutputStream("config/setting/netserver_spranking_daily"))
				propSPRankingDaily.storeToXML(out, "NullpoMino NetServer Single Player Daily Leaderboard")
				out.close()
			} catch(e:IOException) {
				log.error("Failed to write single player daily ranking data", e)
			}

		}

		/** Get player data from propPlayerData
		 * @param pInfo NetPlayerInfo
		 */
		private fun getPlayerDataFromProperty(pInfo:NetPlayerInfo) {
			if(pInfo.isTripUse) {
				for(i in 0 until GameEngine.MAX_GAMESTYLE) {
					pInfo.rating[i] = propPlayerData.getProperty("p.rating.$i."+pInfo.strName, ratingDefault)
					pInfo.playCount[i] = propPlayerData.getProperty("p.playCount.$i."+pInfo.strName, 0)
					pInfo.winCount[i] = propPlayerData.getProperty("p.winCount.$i."+pInfo.strName, 0)
				}
				pInfo.spPersonalBest.strPlayerName = pInfo.strName
				pInfo.spPersonalBest.readProperty(propPlayerData)
			} else {
				for(i in 0 until GameEngine.MAX_GAMESTYLE) {
					pInfo.rating[i] = ratingDefault
					pInfo.playCount[i] = 0
					pInfo.winCount[i] = 0
				}
				pInfo.spPersonalBest.strPlayerName = pInfo.strName
			}
		}

		/** Set player data to propPlayerData
		 * @param pInfo NetPlayerInfo
		 */
		private fun setPlayerDataToProperty(pInfo:NetPlayerInfo) {
			if(pInfo.isTripUse) {
				for(i in 0 until GameEngine.MAX_GAMESTYLE) {
					propPlayerData.setProperty("p.rating.$i."+pInfo.strName, pInfo.rating[i])
					propPlayerData.setProperty("p.playCount.$i."+pInfo.strName, pInfo.playCount[i])
					propPlayerData.setProperty("p.winCount.$i."+pInfo.strName, pInfo.winCount[i])
				}
				pInfo.spPersonalBest.strPlayerName = pInfo.strName
				pInfo.spPersonalBest.writeProperty(propPlayerData)
			}
		}

		/** Write player data properties (propPlayerData) to a file */
		private fun writePlayerDataToFile() {
			try {
				val out = GZIPOutputStream(FileOutputStream("config/setting/netserver_playerdata"))
				propPlayerData.storeToXML(out, "NullpoMino NetServer PlayerData")
				out.close()
			} catch(e:IOException) {
				log.error("Failed to write player data", e)
			}

		}

		/** Load ban list from a file */
		private fun loadBanList() {
			banList = LinkedList()

			try {
				val txtBanList = BufferedReader(FileReader("config/setting/netserver_banned.lst"))

				txtBanList.readLines().forEach {str ->
					if(str.isNotEmpty()) {
						val ban = NetServerBan()
						ban.importString(str)
						if(!ban.isExpired) banList!!.add(ban)
					}
				}
			} catch(e:IOException) {
				log.debug("Ban list file doesn't exist")
			} catch(e:Exception) {
				log.warn("Failed to load ban list", e)
			}

		}

		/** Write ban list to a file */
		private fun saveBanList() {
			try {
				val outFile = FileWriter("config/setting/netserver_banned.lst")
				val out = PrintWriter(outFile)

				for(ban in banList!!)
					out.println(ban.exportString())

				out.flush()
				out.close()

				log.info("Ban list saved")
			} catch(e:Exception) {
				log.error("Failed to save ban list", e)
			}

		}

		/** Load lobby chat history file */
		private fun loadLobbyChatHistory() {
			if(lobbyChatList==null)
				lobbyChatList = LinkedList()
			else
				lobbyChatList!!.clear()

			try {
				val txtLobbyChat = BufferedReader(FileReader("config/setting/netserver_lobbychat.log"))

				txtLobbyChat.readLines().forEach {str ->
					if(str.isNotEmpty()) {
						val chat = NetChatMessage()
						chat.importString(str)
						lobbyChatList!!.add(chat)
					}
				}
			} catch(e:IOException) {
				log.debug("Lobby chat history doesn't exist")
			} catch(e:Exception) {
				log.info("Failed to load lobby chat history", e)
			}

			while(lobbyChatList!!.size>maxLobbyChatHistory) lobbyChatList!!.removeFirst()
		}

		/** Save lobby chat history file */
		private fun saveLobbyChatHistory() {
			try {
				val outFile = FileWriter("config/setting/netserver_lobbychat.log")
				val out = PrintWriter(outFile)

				while(lobbyChatList!!.size>maxLobbyChatHistory) lobbyChatList!!.removeFirst()

				for(chat in lobbyChatList!!)
					out.println(chat.exportString())

				out.flush()
				out.close()

				log.debug("Lobby chat history saved")
			} catch(e:Exception) {
				log.error("Failed to save lobby chat history file", e)
			}

		}

		/** Get IP address
		 * @param client SocketChannel
		 * @return IP address
		 */
		private fun getHostAddress(client:SocketChannel):String {
			try {
				return client.socket().inetAddress.hostAddress
			} catch(e:Exception) {
			}

			return ""
		}

		/** Get Hostname
		 * @param client SocketChannel
		 * @return Hostname
		 */
		private fun getHostName(client:SocketChannel):String {
			if(!allowDNSAccess) return getHostAddress(client)
			try {
				return client.socket().inetAddress.hostName
			} catch(e:Exception) {
			}

			return ""
		}

		/** Get both Hostname and IP address
		 * @param client SocketChannel
		 * @return Hostname and IP address
		 */
		private fun getHostFull(client:SocketChannel):String {
			if(!allowDNSAccess) return getHostAddress(client)
			try {
				return getHostName(client)+" (${getHostAddress(client)})"
			} catch(e:Exception) {
			}

			return ""
		}

		/** Main (Entry point)
		 * @param args optional command-line arguments (0: server port 1:
		 * netserver.cfg path)
		 */
		@JvmStatic fun main(args:Array<String>) {
			// Init log system (should be first!)
			PropertyConfigurator.configure("config/etc/log_server.cfg")

			// get netserver.cfg file path from 2nd command-line argument, if specified
			var servcfg = "config/etc/netserver.cfg" // default location
			if(args.size>=2) servcfg = args[1]

			// Load server config file
			propServer = CustomProperties()
			try {
				val `in` = FileInputStream(servcfg)
				propServer.load(`in`)
				`in`.close()
			} catch(e:IOException) {
				log.warn("Failed to load config file", e)
			}

			// Fetch port number from config file
			var port = propServer.getProperty("netserver.port", DEFAULT_PORT)

			if(args.isNotEmpty())
			// If command-line option is used, change port number to the new one
				try {
					port = Integer.parseInt(args[0])
				} catch(e:NumberFormatException) {
				}

			// Run
			NetServer(port).run()
		}
	}
}