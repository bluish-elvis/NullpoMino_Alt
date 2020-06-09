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
package mu.nu.nullpo.game.subsystem.mode

import mu.nu.nullpo.game.component.Block
import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.game.play.GameManager
import mu.nu.nullpo.gui.net.NetLobbyFrame
import mu.nu.nullpo.util.CustomProperties

/** Game mode interface */
interface GameMode {

	/** Get mode identifier.
	 * @return Mode identifier
	 */
	val id:String
	/** Get mode name.
	 * @return Mode name
	 */
	val name:String

	/** Get (max) number of players.
	 * @return Number of players
	 */
	val players:Int

	/** Get game style.
	 * @return Game style of this mode (0:Tetromino, 1:Avalanche, 2:Physician, 3:SPF)
	 */
	val gameStyle:Int

	/** Is netplay-only mode?
	 * @return true if this is netplay-only mode.
	 */
	val isNetplayMode:Boolean

	/** Is VS mode?
	 * @return true if this is multiplayer mode.
	 */
	val isVSMode:Boolean

	/** Initialization of game mode. Executed before the game screen appears.
	 * @param manager GameManager that owns this mode
	 */
	fun modeInit(manager:GameManager)

	/** Initialization for each player.
	 * @param engine GameEngine
	 * @param playerID Player ID
	 */
	fun playerInit(engine:GameEngine, playerID:Int)

	/** Executed after Ready->Go, before the first piece appears.
	 * @param engine GameEngine
	 * @param playerID Player ID
	 */
	fun startGame(engine:GameEngine, playerID:Int)

	/** Executed at the start of each frame.
	 * @param engine GameEngine
	 * @param playerID Player ID
	 */
	fun onFirst(engine:GameEngine, playerID:Int)

	/** Executed at the end of each frame. You can update your own timers here.
	 * @param engine GameEngine
	 * @param playerID Player ID
	 */
	fun onLast(engine:GameEngine, playerID:Int)

	/** Settings screen.
	 * @param engine GameEngine
	 * @param playerID Player ID
	 * @return true if you don't want to start the game yet. false if settings
	 * are done.
	 */
	fun onSetting(engine:GameEngine, playerID:Int):Boolean

	/** Ready->Go screen.
	 * @param engine GameEngine
	 * @param playerID Player ID
	 * @return true if you override everything of this screen (skips default behavior)
	 */
	fun onReady(engine:GameEngine, playerID:Int):Boolean

	/** Piece movement screen. This is where the player can move/rotate/drop
	 * current piece.
	 * @param engine GameEngine
	 * @param playerID Player ID
	 * @return true if you override everything of this screen (skips default behavior)
	 */
	fun onMove(engine:GameEngine, playerID:Int):Boolean

	/** "Lock flash" screen. Certain rules may skip this screen.
	 * @param engine GameEngine
	 * @param playerID Player ID
	 * @return true if you override everything of this screen (skips default behavior)
	 */
	fun onLockFlash(engine:GameEngine, playerID:Int):Boolean

	/** During line clear.
	 * @param engine GameEngine
	 * @param playerID Player ID
	 * @return true if you override everything of this screen (skips default behavior)
	 */
	fun onLineClear(engine:GameEngine, playerID:Int):Boolean

	/** During ARE.
	 * @param engine GameEngine
	 * @param playerID Player ID
	 * @return true if you override everything of this screen (skips default behavior)
	 */
	fun onARE(engine:GameEngine, playerID:Int):Boolean

	/** During ending-start sequence.
	 * @param engine GameEngine
	 * @param playerID Player ID
	 * @return true if you override everything of this screen (skips default behavior)
	 */
	fun onEndingStart(engine:GameEngine, playerID:Int):Boolean

	/** "Custom" screen. Any game mode can use this screen freely.
	 * @param engine GameEngine
	 * @param playerID Player ID
	 * @return This is ignored.
	 */
	fun onCustom(engine:GameEngine, playerID:Int):Boolean

	/** "Excellent!" screen.
	 * @param engine GameEngine
	 * @param playerID Player ID
	 * @return true if you override everything of this screen (skips default behavior)
	 */
	fun onExcellent(engine:GameEngine, playerID:Int):Boolean

	/** "Game Over" screen.
	 * @param engine GameEngine
	 * @param playerID Player ID
	 * @return true if you override everything of this screen (skips default behavior)
	 */
	fun onGameOver(engine:GameEngine, playerID:Int):Boolean

	/** End-of-game results screen.
	 * @param engine GameEngine
	 * @param playerID Player ID
	 * @return true if you override everything of this screen (skips default behavior)
	 */
	fun onResult(engine:GameEngine, playerID:Int):Boolean

	/** Field editor screen.
	 * @param engine GameEngine
	 * @param playerID Player ID
	 * @return true if you override everything of this screen (skips default behavior)
	 */
	fun onFieldEdit(engine:GameEngine, playerID:Int):Boolean

	/** Executed at the start of each frame.
	 * @param engine GameEngine
	 * @param playerID Player ID
	 */
	fun renderFirst(engine:GameEngine, playerID:Int)

	/** Executed at the end of each frame. You can render HUD here.
	 * @param engine GameEngine
	 * @param playerID Player ID
	 */
	fun renderLast(engine:GameEngine, playerID:Int)

	/** Render settings screen.
	 * @param engine GameEngine
	 * @param playerID Player ID
	 */
	fun renderSetting(engine:GameEngine, playerID:Int)

	/** Render Ready->Go screen.
	 * @param engine GameEngine
	 * @param playerID Player ID
	 */
	fun renderReady(engine:GameEngine, playerID:Int)

	/** Render piece movement screen.
	 * @param engine GameEngine
	 * @param playerID Player ID
	 */
	fun renderMove(engine:GameEngine, playerID:Int)

	/** Render "Lock flash" screen.
	 * @param engine GameEngine
	 * @param playerID Player ID
	 */
	fun renderLockFlash(engine:GameEngine, playerID:Int)

	/** Render line clear screen.
	 * @param engine GameEngine
	 * @param playerID Player ID
	 */
	fun renderLineClear(engine:GameEngine, playerID:Int)

	/** Render ARE screen.
	 * @param engine GameEngine
	 * @param playerID Player ID
	 */
	fun renderARE(engine:GameEngine, playerID:Int)

	/** Render "ending start sequence" screen.
	 * @param engine GameEngine
	 * @param playerID Player ID
	 */
	fun renderEndingStart(engine:GameEngine, playerID:Int)

	/** Render "Custom" screen.
	 * @param engine GameEngine
	 * @param playerID Player ID
	 */
	fun renderCustom(engine:GameEngine, playerID:Int)

	/** Render "Excellent!" screen.
	 * @param engine GameEngine
	 * @param playerID Player ID
	 */
	fun renderExcellent(engine:GameEngine, playerID:Int)

	/** Render "Game Over" screen.
	 * @param engine GameEngine
	 * @param playerID Player ID
	 */
	fun renderGameOver(engine:GameEngine, playerID:Int)

	/** Render results screen.
	 * @param engine GameEngine
	 * @param playerID Player ID
	 */
	fun renderResult(engine:GameEngine, playerID:Int)

	/** Render field editor screen.
	 * @param engine GameEngine
	 * @param playerID Player ID
	 */
	fun renderFieldEdit(engine:GameEngine, playerID:Int)

	/** Render player input.
	 * @param engine GameEngine
	 * @param playerID Player ID
	 */
	fun renderInput(engine:GameEngine, playerID:Int)

	/** Executed when a block gets destroyed in line-clear screen.
	 * @param engine GameEngine
	 * @param playerID Player ID
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param blk Block
	 */
	fun blockBreak(engine:GameEngine, playerID:Int, x:Int, y:Int, blk:Block)

	fun lineClear(gameEngine:GameEngine, playerID:Int, i:Int)

	/** Calculate score. Executed before pieceLocked.
	 *  Please note this event will be called even if no lines are cleared!
	 * @param engine GameEngine
	 * @param playerID Player ID
	 * @param lines Number of lines. Can be zero.
	 */
	fun calcScore(engine:GameEngine, playerID:Int, lines:Int):Int

	/** After soft drop is used
	 * @param engine GameEngine
	 * @param playerID Player ID
	 * @param fall Number of rows
	 */
	fun afterSoftDropFall(engine:GameEngine, playerID:Int, fall:Int)

	/** After hard drop is used
	 * @param engine GameEngine
	 * @param playerID Player ID
	 * @param fall Number of rows
	 */
	fun afterHardDropFall(engine:GameEngine, playerID:Int, fall:Int)

	/** Executed after the player exits field-editor screen.
	 * @param engine GameEngine
	 * @param playerID Player ID
	 */
	fun fieldEditExit(engine:GameEngine, playerID:Int)

	/** When the current piece locked (Executed befotre calcScore)
	 * @param engine GameEngine
	 * @param playerID Player ID
	 * @param lines Number of lines. Can be zero.
	 */
	fun pieceLocked(engine:GameEngine, playerID:Int, lines:Int)

	/** When line clear ends
	 * @param engine GameEngine
	 * @param playerID Player ID
	 * @return true if you override everything of this screen (skips default behavior)
	 */
	fun lineClearEnd(engine:GameEngine, playerID:Int):Boolean

	/** Read rankings from property file.
	 *  This is used in playerInit or from netOnJoin.
	 * @param prop Property file
	 * @param ruleName Rule name
	 */
	fun loadRanking(prop:CustomProperties, ruleName:String)

	/** Called when saving replay
	 * @param engine GameEngine
	 * @param playerID Player ID
	 * @param prop CustomProperties of replay file (You can write additional settings here)
	 */
	fun saveReplay(engine:GameEngine, playerID:Int, prop:CustomProperties)

	/** Called when a replay file is loaded
	 * @param engine GameEngine
	 * @param playerID Player ID
	 * @param prop CustomProperties of replay file (You can read additional settings here)
	 */
	fun loadReplay(engine:GameEngine, playerID:Int, prop:CustomProperties)

	/** Initialization for netplay.
	 * @param obj Any object (Currently NetLobbyFrame)
	 */
	fun netplayInit(obj:NetLobbyFrame)

	/** When the mode unloads during netplay (Called when mode change happens)
	 * @param obj Any object (Currently NetLobbyFrame)
	 */
	fun netplayUnload(obj:NetLobbyFrame)

	/** Called when retry key is pressed during netplay
	 * @param engine GameEngine
	 * @param playerID Player ID
	 */
	fun netplayOnRetryKey(engine:GameEngine, playerID:Int)
}
