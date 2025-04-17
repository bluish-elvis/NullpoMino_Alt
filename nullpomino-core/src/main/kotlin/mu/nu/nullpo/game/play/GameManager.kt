/*
 Copyright (c) 2010-2024, NullNoname
 All rights reserved.

 Converted to Kotlin and modified by Venom_Nhelv as bluish-elvis
 THIS IS NOT MADE IN ASSOCIATION WITH THE GAME CREATOR.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions are met:
     * Redistributions of source code must retain the above copyright
       notice, this list of conditions and the following disclaimer.
     * Redistributions in binary form must reproduce the above copyright
       notice, this list of conditions and the following disclaimer in the
       documentation and/or other materials provided with the distribution.
     * Neither the name of NullNoname nor the names of its
       contributors may be used to endorse or promote products derived from
       this software without specific prior written permission.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 POSSIBILITY OF SUCH DAMAGE.
 */
package mu.nu.nullpo.game.play

import mu.nu.nullpo.game.component.BGMStatus
import mu.nu.nullpo.game.component.BackgroundStatus
import mu.nu.nullpo.game.event.EventReceiver
import mu.nu.nullpo.game.subsystem.mode.GameMode
import mu.nu.nullpo.util.CustomProperties
import org.apache.logging.log4j.LogManager

/** GameManager: The container of the game */
class GameManager(
	/** EventReceiver: Manages various events, and renders everything to the screen */
	val receiver:EventReceiver = EventReceiver(),
	/** Game Mode */
	mode:GameMode? = null
) {
	/** Game Mode */
	var mode = mode
		set(value) {
			value?.modeInit(this)
			log.debug("GameManager: Mode injected = ${value?.name}")
			field = value
		}
	/** Properties used by game mode
	 * contains setting & Presets */
	var modeConfig = CustomProperties(cfgMode)

	private val cfgMode get() = "config/setting/mode/${mode?.id ?: "_common"}.cfg"

	/** Properties used by statistics */
	var statsProp = CustomProperties(statsFile)
	val statsFile get() = "scores/stats"

	/** Properties for Ranking/Records game mode */
	var recordProp = CustomProperties(recorder()+".rec")

	fun recorder(ruleName:String? = null):String =
		"scores/"+(ruleName?.let {"$it/"} ?: "")+(mode?.id ?: "")
	//fun recorder():String = "scores/${mode?.name ?: "mode"}.rec"

	/** Properties for replay file */
	var replayProp = CustomProperties()
	/** True if replay mode */
	var replayMode = false
	/** True if replay Overwriting */
	var replayRerecord = false

	/** True if display menus only (No game screens) */
	var menuOnly = false

	/** BGMStatus: Manages the status of background music */
	val musMan = BGMStatus()
	/** BackgroundStatus: Manages the status of background image */
	val bgMan = BackgroundStatus()
	/** GameEngine: This is where the most action takes place */
	val engine = mutableListOf<GameEngine>()

	/** True to show invisible blocks in replay */
	var replayShowInvisible = false
	/** Show input */
	var showInput = false

	/** @return Number of players*/
	val players get() = engine.size
	/** @return true if the game should quit in any GameEngine object*/
	val quitFlag get() = engine.any {it.quitFlag}
	/** @return true if there is an active GameEngine*/
	val isGameActive get() = engine.any {it.gameActive}
	/** @return Player ID of last survivor.
	 * -2 in single player game.
	 * -1 in draw game.
	 */
	val winner get() = if(players<2) -2 else engine.indexOfLast {it.stat!=GameEngine.Status.GAMEOVER}

	init {
		log.debug("GameManager constructor called")
		init()
	}

	/** Initialize the game */
	fun init() {
		log.debug("GameManager init()")

		if(replayProp.isEmpty) replayMode = false

		replayRerecord = false
		menuOnly = false

		bgMan.fadeEnabled = receiver.heavyEffect>0
		receiver.modeInit(this)
		val players = mode?.let {
			modeConfig.load(cfgMode)
			statsProp.load(statsFile)
			it.modeInit(this)
			it.players
		} ?: 1
		for(i in 0..<players)
			engine.add(GameEngine(this, i))
	}

	/** Save properties to "config/setting/mode.cfg"*/
	fun saveModeConfig() = modeConfig.save(cfgMode)

	/** Reset the game */
	fun reset() {
		log.debug("GameManager reset()")

		menuOnly = false
		musMan.reset()
		bgMan.reset()
		if(!replayMode) replayProp.clear()
		engine.forEach {it.init()}
	}
	/** shut down the game */
	fun shutdown() {
		log.debug("GameManager shutdown()")

		try {
			engine.forEach {it.shutdown()}
			engine.removeAll {true}
			mode = null
			modeConfig.clear()
			replayProp.clear()
			musMan.reset()
			bgMan.reset()
		} catch(e:Throwable) {
			log.debug("Caught Throwable on shutdown", e)
		}
	}

	/** Update every GameEngine */
	fun updateAll() {
		engine.forEach {it.update()}
		musMan.fadeUpdate()
		bgMan.fadeUpdate()

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
		internal val log = LogManager.getLogger()

		/** Major version */
		const val versionMajor = 7.7f
		/** Minor version */
		const val versionMinor = 2025
		/** Development-build flag (false:Release-build true:Dev-build) */
		const val isDevBuild = true
		/** Get minor version (For compatibility with old replays)
		 * @return Minor version
		 */
		val versionMinorOld get() = versionMinor.toFloat()
		/** Get version information as String
		 * @return Version information
		 */
		val versionString get() = "$versionMajor.$versionMinor${if(isDevBuild) "DEV" else ""}"
		/** Get build type as string
		 * @return Build type as String
		 */
		val buildTypeString get() = if(isDevBuild) "Development" else "Release"
		/** Get build type name
		 * @param type Build type (false:Release true:Development)
		 * @return Build type as String
		 */
		fun getBuildTypeString(type:Boolean) = if(type) "Development" else "Release"
	}
}
