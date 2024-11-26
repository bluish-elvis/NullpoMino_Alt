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
package mu.nu.nullpo.game.subsystem.mode

import mu.nu.nullpo.game.component.Block
import mu.nu.nullpo.game.event.Leaderboard
import mu.nu.nullpo.game.event.ScoreEvent
import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.game.play.GameEngine.GameStyle
import mu.nu.nullpo.game.play.GameManager
import mu.nu.nullpo.game.subsystem.mode.menu.MenuList
import mu.nu.nullpo.gui.net.NetLobbyFrame
import mu.nu.nullpo.util.CustomProperties
import zeroxfc.nullpo.custom.libs.ProfileProperties

typealias rankMapType = Map<String, rankMapChild>
typealias rankMapChild = MutableList<Comparable<Any>>

/** Game mode interface */
interface GameMode {
	/** @return Mode identifier */
	val id:String
	/** @return Mode name*/
	val name:String
	/** @return Maximum number of players*/
	val players:Int
	/** @return Game style of this mode (0:Tetromino, 1:Avalanche, 2:Physician, 3:SPF)*/
	val gameStyle:GameStyle
	/** @return Game genre of this mode
	 *  (-1:Retro/Puzzle, 0: Generic/Guideline,
	 *  1: Various Unique, 2:Rush Trial, 3:Grand 20G Challenge)*/
	val gameIntensity:Int
	/** @return true if this is net-play only mode.*/
	val isOnlineMode:Boolean
	/** @return true if this is a multiplayer mode.*/
	val isVSMode:Boolean

	/** State of Setting menu */
	val menu:MenuList
	/** Mapping of Ranking Properties
	 * get()= [AbstractMode.rankMapOf] (List<String to Score>)
	 * @sample Marathon.rankMapOf
	 */
	val propRank:rankMapType
	/** Used by [loadRankingPlayer], [saveRankingPlayer]
	 * get()= [AbstractMode.rankMapOf] (List<String to Score>)
	 * @sample zeroxfc.nullpo.custom.modes.MissionMode.propPB  */
	val propPB:rankMapType
	val ranking:Leaderboard<*>

	/** Initialization of game mode. Executed before the game screen appears.
	 * @param manager GameManager that owns this mode
	 */
	fun modeInit(manager:GameManager)
	/** Initialization for each player. */
	fun playerInit(engine:GameEngine)
	/** Executed after Ready->Go, before the first piece appears. */
	fun startGame(engine:GameEngine)
	/** Executed at the start of each frame.*/
	fun onFirst(engine:GameEngine)
	/** Executed at the end of each frame. You can update your own timers here.*/
	fun onLast(engine:GameEngine)
	/** Settings screen.
	 * @return true if you don't want to start the game yet. false if settings are done.*/
	fun onSetting(engine:GameEngine):Boolean
	/** Profile screen.
	 * @return true if you don't want to start the game yet. false if settings are done.*/
	fun onProfile(engine:GameEngine):Boolean
	/** Ready->Go screen.
	 * @return true if you override everything of this screen (skips default behavior)*/
	fun onReady(engine:GameEngine):Boolean
	/** Piece movement screen.
	 * This is where the player can move/spin/drop the current piece.
	 * @return true if you override everything of this screen (skips default behavior)*/
	fun onMove(engine:GameEngine):Boolean
	/** "Lock flash" screen. Certain rules may skip this screen.
	 * @return true if you override everything of this screen (skips default behavior)*/
	fun onLockFlash(engine:GameEngine):Boolean
	/** During line clear.
	 * @return true if you override everything of this screen (skips default behavior)*/
	fun onLineClear(engine:GameEngine):Boolean
	/** During ARE.
	 * @return true if you override everything of this screen (skips default behavior)*/
	fun onARE(engine:GameEngine):Boolean
	/** During ending-start sequence.
	 * @return true if you override everything of this screen (skips default behavior)*/
	fun onEndingStart(engine:GameEngine):Boolean
	/** "Custom" screen. Any game mode can use this screen freely.
	 * @return This is ignored.*/
	fun onCustom(engine:GameEngine):Boolean
	/** "Excellent!" screen.
	 * @return true if you override everything of this screen (skips default behavior)*/
	fun onExcellent(engine:GameEngine):Boolean
	/** "Game Over" screen.
	 * @return true if you override everything of this screen (skips default behavior)*/
	fun onGameOver(engine:GameEngine):Boolean
	/** End-of-game results screen.
	 * @return true if you override everything of this screen (skips default behavior)*/
	fun onResult(engine:GameEngine):Boolean
	/** Field editor screen.
	 * @return true if you override everything of this screen (skips default behavior)*/
	fun onFieldEdit(engine:GameEngine):Boolean

	/** Executed at the start of each frame.*/
	fun renderFirst(engine:GameEngine)
	/** Executed at the end of each frame.
	 *  You can render HUD here.*/
	fun renderLast(engine:GameEngine)
	/** Render settings screen.*/
	fun renderSetting(engine:GameEngine)
	/** Render profile screen.*/
	fun renderProfile(engine:GameEngine)
	/** Render Ready->Go screen.*/
	fun renderReady(engine:GameEngine)
	/** Render piece movement screen.*/
	fun renderMove(engine:GameEngine)
	/** Render "Lock flash" screen.*/
	fun renderLockFlash(engine:GameEngine)
	/** Render line clear screen.*/
	fun renderLineClear(engine:GameEngine)
	/** Render ARE screen.*/
	fun renderARE(engine:GameEngine)
	/** Render "ending start sequence" screen.*/
	fun renderEndingStart(engine:GameEngine)
	/** Render "Custom" screen. */
	fun renderCustom(engine:GameEngine)
	/** Render "Excellent!" screen. */
	fun renderExcellent(engine:GameEngine)
	/** Render "Game Over" screen. */
	fun renderGameOver(engine:GameEngine)
	/** Render results screen. */
	fun renderResult(engine:GameEngine)
	/** Render field editor screen. */
	fun renderFieldEdit(engine:GameEngine)
	/** Render player input. */
	fun renderInput(engine:GameEngine)

	/** Executed when a block gets destroyed in line-clear screen.
	 * @param blk Block
	 * @return if true, skip default behavior
	 */
	fun blockBreak(engine:GameEngine, blk:Map<Int, Map<Int, Block>>):Boolean

	fun lineClear(gameEngine:GameEngine, i:Collection<Int>)

	/** Calculate score. Executed before pieceLocked.
	 *  Please note this event will be called even If no lines are cleared!
	 * @param ev Number of lines. Can be zero.
	 */
	fun calcScore(engine:GameEngine, ev:ScoreEvent):Int

	/** After soft drop is used
	 * @param fall Number of rows
	 */
	fun afterSoftDropFall(engine:GameEngine, fall:Int)

	/** After hard drop is used
	 * @param fall Number of rows
	 */
	fun afterHardDropFall(engine:GameEngine, fall:Int)

	/** Executed after the player exits field-editor screen. */
	fun fieldEditExit(engine:GameEngine)

	/** When the current piece locked (Executed before calcScore)
	 *  @param lines Number of lines. Can be zero.
	 */
	fun pieceLocked(engine:GameEngine, lines:Int, finesse:Boolean)

	/** When line clear ends
	 * @return true if you override everything of this screen (skips default behavior)
	 */
	fun lineClearEnd(engine:GameEngine):Boolean

	fun loadSetting(engine:GameEngine, prop:CustomProperties, ruleName:String = engine.ruleOpt.strRuleName, playerID:Int = engine.playerID)

	fun loadSetting(prof:ProfileProperties, engine:GameEngine) =
		if(prof.isLoggedIn) loadSetting(engine, prof.propProfile, engine.ruleOpt.strRuleName, engine.playerID)
		else Unit

	/** Read rankings from [prop].
	 *  This is used in playerInit or from netOnJoin.
	 */
	fun loadRanking(prop:CustomProperties)
	/** Read rankings from [prof].
	 *  This is used in playerInit or from netOnJoin.
	 */
	fun loadRankingPlayer(prof:ProfileProperties)

	fun saveSetting(engine:GameEngine, prop:CustomProperties, ruleName:String = engine.ruleOpt.strRuleName, playerID:Int = engine.playerID)

	fun saveRanking()
	fun saveRankingPlayer(prof:ProfileProperties)

	/** Called when saving replay to [prop] */
	fun saveReplay(engine:GameEngine, prop:CustomProperties):Boolean
	/** Called when a replay file is loaded from [prop] */
	fun loadReplay(engine:GameEngine, prop:CustomProperties)

	/** Initialization for netplay.
	 * @param obj Any object (Currently NetLobbyFrame)
	 */
	fun netplayInit(obj:NetLobbyFrame)
	/** When the mode unloads during netplay (Called when mode change happens)
	 * @param obj Any object (Currently NetLobbyFrame)
	 */
	fun netplayUnload(obj:NetLobbyFrame)
	/** Called when retry key is pressed during netplay */
	fun netplayOnRetryKey(engine:GameEngine)
}
