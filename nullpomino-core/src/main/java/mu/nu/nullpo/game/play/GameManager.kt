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
package mu.nu.nullpo.game.play

import mu.nu.nullpo.game.component.BGMStatus
import mu.nu.nullpo.game.component.BackgroundStatus
import mu.nu.nullpo.game.event.EventReceiver
import mu.nu.nullpo.game.subsystem.mode.GameMode
import mu.nu.nullpo.util.CustomProperties
import org.apache.log4j.Logger

/** GameManager: The container of the game */
class GameManager
/** Default constructor */
(
	/** EventReceiver: Manages various events, and renders everything to the screen */
	val receiver:EventReceiver = EventReceiver()) {

	/** Game Mode */
	var mode:GameMode? = null

	/** Properties used by game mode */
	var modeConfig:CustomProperties = receiver.loadModeConfig()?:CustomProperties()

	/** Properties for replay file */
	var replayProp:CustomProperties = CustomProperties()

	/** true if replay mode */
	var replayMode:Boolean = false

	/** true if replay rerecording */
	var replayRerecord:Boolean = false

	/** true if display menus only (No game screens) */
	var menuOnly:Boolean = false

	/** BGMStatus: Manages the status of background music */
	val bgmStatus:BGMStatus = BGMStatus()

	/** BackgroundStatus: Manages the status of background image */
	val backgroundStatus:BackgroundStatus = BackgroundStatus()

	/** GameEngine: This is where the most action takes place */
	val engine:MutableList<GameEngine> = mutableListOf()

	/** true to show invisible blocks in replay */
	var replayShowInvisible:Boolean = false

	/** Show input */
	var showInput:Boolean = false

	/** Get number of players
	 * @return Number of players
	 */
	val players:Int
		get() = engine.size

	/** Check if quit flag is true in any GameEngine object
	 * @return true if the game should quit
	 */
	val quitFlag:Boolean
		get() = engine.any {it.quitflag}

	/** Check if at least 1 game is active
	 * @return true if there is a active GameEngine
	 */
	val isGameActive:Boolean
		get() = engine.any {it.gameActive}

	/** Get winner ID
	 * @return Player ID of last survivor. -1 in single player game. -2 in tied
	 * game.
	 */
	val winner:Int
		get() = if(players<2) -1 else engine.indexOfLast {it.stat!=GameEngine.Status.GAMEOVER}

	init {
		log.debug("GameManager constructor called")
	}

	/** Initialize the game */
	fun init() {
		log.debug("GameManager init()")

		if(replayProp.isEmpty) replayMode = false

		replayRerecord = false
		menuOnly = false

		var players = 1
		mode?.let {
			it.modeInit(this)
			players = it.players
		}
		for(i in 0 until players)
			engine.add(GameEngine(this, i))
	}

	/** Reset the game */
	fun reset() {
		log.debug("GameManager reset()")

		menuOnly = false
		bgmStatus.reset()
		backgroundStatus.reset()
		if(!replayMode) replayProp.clear()
		engine.forEach{it.init()}
	}

	/** Shutdown the game */
	fun shutdown() {
		log.debug("GameManager shutdown()")

		try {
			engine.forEach {it.shutdown()}
			engine.removeAll {true}
			mode = null
			modeConfig.clear()
			replayProp.clear()
			bgmStatus.reset()
			backgroundStatus.reset()
		} catch(e:Throwable) {
			log.debug("Caught Throwable on shutdown", e)
		}

	}

	/** Update every GameEngine */
	fun updateAll() {
		engine.forEach {it.update()}
		bgmStatus.fadeUpdate()
		backgroundStatus.fadeUpdate()
	}

	/** Dispatches all render events to EventReceiver */
	fun renderAll() {
		engine.forEach {it.render()}
	}

	/** Replay save routine */
	fun saveReplay() {
		replayProp.clear()
		engine.forEach {it.saveReplay()}
		receiver.saveReplay(this, replayProp)
	}

	companion object {
		/** Log (Apache log4j) */
		internal val log = Logger.getLogger(GameManager::class.java)

		/** Major version */
		const val versionMajor = 7.6f
		/** Minor version */
		const val versionMinor = 417

		/** Development-build flag (false:Release-build true:Dev-build) */
		const val isDevBuild = true

		/** Get minor version (For compatibility with old replays)
		 * @return Minor version
		 */
		val versionMinorOld:Float
			get() = versionMinor.toFloat()

		/** Get version information as String
		 * @return Version information
		 */
		val versionString:String
			get() = versionMajor.toString()+"."+versionMinor+if(isDevBuild) "DEV" else ""

		/** Get build type as string
		 * @return Build type as String
		 */
		val buildTypeString:String
			get() = if(isDevBuild) "Development" else "Release"

		/** Get build type name
		 * @param type Build type (false:Release true:Development)
		 * @return Build type as String
		 */
		fun getBuildTypeString(type:Boolean):String = if(type) "Development" else "Release"
	}
}
