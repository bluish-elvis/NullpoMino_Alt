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
package mu.nu.nullpo.gui.net

import mu.nu.nullpo.game.net.NetPlayerClient
import mu.nu.nullpo.game.net.NetRoomInfo
import java.io.IOException

/** Lobby event interface (also used by netplay modes) */
interface NetLobbyListener {
	/** Initialization Completed
	 * @param lobby NetLobbyFrame
	 */
	fun netlobbyOnInit(lobby:NetLobbyFrame)

	/** Login completed
	 * @param lobby NetLobbyFrame
	 * @param client NetClient
	 */
	fun netlobbyOnLoginOK(lobby:NetLobbyFrame, client:NetPlayerClient)

	/** When you enter a room
	 * @param lobby NetLobbyFrame
	 * @param client NetClient
	 * @param roomInfo NetRoomInfo
	 */
	fun netlobbyOnRoomJoin(lobby:NetLobbyFrame, client:NetPlayerClient, roomInfo:NetRoomInfo)

	/** When you returned to lobby
	 * @param lobby NetLobbyFrame
	 * @param client NetClient
	 */
	fun netlobbyOnRoomLeave(lobby:NetLobbyFrame, client:NetPlayerClient)

	/** When disconnected
	 * @param lobby NetLobbyFrame
	 * @param client NetClient
	 * @param ex A Throwable that caused disconnection (null if unknown or
	 * normal termination)
	 */
	fun netlobbyOnDisconnect(lobby:NetLobbyFrame, client:NetPlayerClient, ex:Throwable?)

	/** Message received
	 * @param lobby NetLobbyFrame
	 * @param client NetClient
	 * @param message Message (Already sepatated by tabs)
	 * @throws IOException When something bad occurs
	 */
	@Throws(IOException::class)
	fun netlobbyOnMessage(lobby:NetLobbyFrame, client:NetPlayerClient, message:List<String>)

	@Deprecated("message should use List")
	@Throws(IOException::class)
	fun netlobbyOnMessage(lobby:NetLobbyFrame, client:NetPlayerClient, message:Array<String>) =
		netlobbyOnMessage(lobby, client, message.toList())

	/** When the lobby window is closed
	 * @param lobby NetLobbyFrame
	 */
	fun netlobbyOnExit(lobby:NetLobbyFrame)
}
