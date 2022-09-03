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
package mu.nu.nullpo.game.subsystem.mode

import mu.nu.nullpo.game.component.BGMStatus.BGM
import mu.nu.nullpo.game.component.Block
import mu.nu.nullpo.game.component.Controller
import mu.nu.nullpo.game.component.Piece
import mu.nu.nullpo.game.component.SpeedParam
import mu.nu.nullpo.game.event.EventReceiver
import mu.nu.nullpo.game.event.EventReceiver.COLOR
import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.game.play.GameManager
import mu.nu.nullpo.game.play.GameStyle
import mu.nu.nullpo.game.subsystem.mode.menu.MenuList
import mu.nu.nullpo.gui.net.NetLobbyFrame
import mu.nu.nullpo.util.CustomProperties
import mu.nu.nullpo.util.GeneralUtil.getONorOFF
import mu.nu.nullpo.util.GeneralUtil.getOX
import mu.nu.nullpo.util.GeneralUtil.toInt
import mu.nu.nullpo.util.GeneralUtil.toTimeStr
import zeroxfc.nullpo.custom.libs.ProfileProperties
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.ln1p

/** Dummy implementation of game mode. Used as a base of most game modes. */
abstract class AbstractMode:GameMode {
	final override val id:String
		get() = this::class.simpleName ?: "noName"

	/** GameManager that owns this mode */
	protected var owner = GameManager()

	/** Drawing and event handling EventReceiver */
	protected val receiver:EventReceiver get() = owner.receiver

	/** Used by [loadRanking], [saveRanking]
	 * @sample GrandMarathon.rankMap
	 * @sample Marathon.rankMap  */
	/*abstract */override val rankMap:Map<String, IntArray> = emptyMap()

	/** Used by [loadRankingPlayer], [saveRankingPlayer]
	 * @sample zeroxfc.nullpo.custom.modes.MissionMode.rankPersMap  */
	override val rankPersMap:Map<String, IntArray> = emptyMap()
	protected var menuColor = COLOR.WHITE
	/*abstract */override val menu:MenuList = MenuList(id)
	protected var statcMenu
		get() = menu.statcMenu
		set(x) {
			menu.statcMenu = x
		}
	protected var menuY
		get() = menu.menuY
		set(x) {
			menu.menuY = x
		}
	/** Name of mode in properties file */
	protected open val propName:String = ""
	/** Position of cursor in menu */
	protected var menuCursor
		get() = menu.menuCursor
		set(x) {
			menu.menuCursor = x
		}
	/** Number of frames spent in menu */
	protected var menuTime = 0
	override val name = this::class.simpleName ?: "No Name"
	override val players:Int = 1
	override val gameStyle = GameStyle.TETROMINO
	override val gameIntensity = 0
	override val isNetplayMode = false
	override val isVSMode = false
	/** Show Player profile */
	protected var showPlayerStats = false
	protected val playerProperties:List<ProfileProperties> = owner.engine.map {it.playerProp}

	protected fun playerProperties(playerID:Int):ProfileProperties = owner.engine[playerID].playerProp

	override fun loadSetting(prop:CustomProperties, ruleName:String, playerID:Int) =
		menu.load(prop, ruleName, if(players<=1) -1 else playerID)

	fun loadSetting(engine:GameEngine) = loadSetting(owner.modeConfig, engine)

	@Deprecated("Set Parameter from GameEngine", ReplaceWith("loadSetting(prop, engine)"))
	protected fun loadSetting(prop:CustomProperties) = loadSetting(prop, owner.engine.first())

	override fun saveSetting(prop:CustomProperties, ruleName:String, playerID:Int) {
		menu.save(prop, ruleName, if(players<=1) -1 else playerID)
		owner.saveModeConfig()
	}

	@Deprecated("Set Parameter from GameEngine", ReplaceWith("saveSetting(prop, engine)"))
	fun saveSetting(prop:CustomProperties) = saveSetting(prop, owner.engine.first())

	/** Read rankings from [prop]
	 * This should be called from Only [GameEngine] */
	override fun loadRanking(prop:CustomProperties, ruleName:String) {
		rankMap.forEach {rec ->
			rec.value.forEachIndexed {i, _ ->
				rec.value[i] = prop.getProperty("${rec.key}.$i", rec.value[i])
			}
		}
	}

	override fun loadRankingPlayer(prof:ProfileProperties, ruleName:String) {
		if(prof.isLoggedIn) {
			rankPersMap.forEach {rec ->
				rec.value.forEachIndexed {i, _ ->
					rec.value[i] = prof.propProfile.getProperty("$id.${rec.key}.$i", rec.value[i])
				}
			}
		}
	}
	/** Save rankings [map] to [prop] */
	internal fun <T:Comparable<T>> saveRanking(map:List<Pair<String, Comparable<T>>>) =
		saveRanking(map.toMap())
	/** Save rankings [map] */
	internal fun <T:Comparable<T>> saveRanking(map:Map<String, Comparable<T>>) {
		map.forEach {(key, it) ->
			owner.recordProp.setProperty(key, it)
		}
	}

	override fun saveRanking() {
		rankMap.forEach {(key, value) ->
			value.forEachIndexed {i, rec -> owner.recordProp.setProperty("$key.$i", rec)}
		}
	}

	override fun saveRankingPlayer(prof:ProfileProperties) {
		if(prof.isLoggedIn) {
			rankPersMap.forEach {rec ->
				rec.value.forEachIndexed {i, v ->
					prof.propProfile.setProperty("$id.${rec.key}.$i", v)
				}
			}

		}
	}

	override fun pieceLocked(engine:GameEngine, lines:Int) {}
	override fun lineClearEnd(engine:GameEngine):Boolean = false
	override fun afterHardDropFall(engine:GameEngine, fall:Int) {}
	override fun afterSoftDropFall(engine:GameEngine, fall:Int) {}
	override fun blockBreak(engine:GameEngine, blk:Map<Int, Map<Int, Block>>) {}
	override fun lineClear(gameEngine:GameEngine, i:Collection<Int>) {}
	override fun fieldEditExit(engine:GameEngine) {}
	override fun loadReplay(engine:GameEngine, prop:CustomProperties) {}
	override fun modeInit(manager:GameManager) {
		menuTime = 0
	}

	override fun onARE(engine:GameEngine):Boolean = false
	override fun onCustom(engine:GameEngine):Boolean = false
	override fun onEndingStart(engine:GameEngine):Boolean = false
	override fun onExcellent(engine:GameEngine):Boolean = false
	override fun onFirst(engine:GameEngine) {}
	override fun onGameOver(engine:GameEngine):Boolean = false
	/** This function will be called when the game timer updates */
	override fun onLast(engine:GameEngine) {
		if(scDisp<engine.statistics.score&&!engine.lagStop) scDisp += ceil(((engine.statistics.score-scDisp)/10f).toDouble()).toInt()
	}

	override fun onLineClear(engine:GameEngine):Boolean = false
	/** Calculates line-clear score
	 * (This function will be called even if no lines are cleared)
	 * @param engine GameEngine
	 * @param lines Cleared line
	 * */
	override fun calcScore(engine:GameEngine, lines:Int):Int {
		val spd = maxOf(0, engine.lockDelay-engine.lockDelayNow)+if(engine.manualLock) 1 else 0
		val pts = calcScoreBase(engine, lines)
		val get = calcScoreCombo(
			pts, if(engine.combo>0&&lines>=1) engine.combo else 0,
			engine.statistics.level, spd
		)
		if(lines>=1) engine.statistics.scoreLine += get
		else engine.statistics.scoreBonus += get
		scDisp += minOf(get, spd)
		lastscore = get
		return if(pts>0||gameIntensity==2) get else 0
	}

	var renSum = 0; private set

	/** Time to display the most recent increase in score */
	var scDisp = 0//; private set

	/** Most recent increase in score */
	var lastscore = 0

	/** npmAlt style Scoring: base Point */
	fun calcScoreBase(engine:GameEngine, lines:Int):Int {
		val pts = when {
			engine.twist -> when {
				lines<=0&&!engine.twistEZ -> if(engine.twistMini) 5 else 8// Twister 0 lines
				engine.twistEZ -> (if(engine.b2b) 11+lines else 0)+when(lines) {
					1 -> 20
					2 -> 40
					else -> lines*25
				}
// Immobile Spin
				lines==1 -> if(engine.twistMini) (if(engine.b2b) 32 else 24)//Mini Twister 1 lines
				else if(engine.b2b) 75 else 50// Twister 1 line
				lines==2 -> if(engine.twistMini&&engine.useAllSpinBonus) (if(engine.b2b) 100 else 75)//Mini Twister 2 lines
				else if(engine.b2b) 128 else 100// Twister 2 lines
				lines==3 -> if(engine.b2b) 210 else 175// Twister 3 lines
				else -> if(engine.b2b) 256 else 192// 180Twister Quads
			}
			lines==1 -> 16 // Single
			lines==2 -> if(engine.split) (if(engine.b2b) 77 else 64) else 48 // Double
			lines==3 -> if(engine.split) (if(engine.b2b) 128 else 102) else 85 // Triple
			lines>=4 -> if(engine.b2b) 192 else 160 // Quads
			else -> 0
		}
		// All clear
		val btb = if(engine.b2b) 99+engine.b2bCount else 100
		return if(lines>=1&&engine.field.isEmpty) pts*(1000+btb*7)/70+2048 else pts*btb/10
	}
	/** npmAlt style Scoring: level&combo bonus at Marathon*/
	fun calcScoreCombo(pts:Int, cmb:Int, lv:Int, spd:Int):Int =
		// Add to score
		if(pts+cmb>0) {
			val mul = if(lv>40) 55+lv/10 else 10+lv
			val get = pts*mul/10+spd
			if(cmb>0) {
				val b = renSum*(4+cmb)/5
				renSum += get
				renSum = renSum*(5+cmb)/5-b
			} else renSum = get
			renSum
		} else 0

	/**Tetris World Style Goal*/
	fun calcPoint(engine:GameEngine, lines:Int):Int = when {
		engine.twist -> when {
			lines<=0&&!engine.twistEZ -> 1 // Twister 0 lines
			engine.twistEZ&&lines>0 -> lines*2+(engine.b2b.toInt()) // Immobile EZ Spin
			lines==1 -> if(engine.twistMini) if(engine.b2b) 3 else 2
			else if(engine.b2b) 5 else 3 // Twister 1 line
			lines==2 -> if(engine.twistMini&&engine.useAllSpinBonus) (if(engine.b2b) 6 else 4)
			else if(engine.b2b) 8 else 6 // Twister 2 lines
			else -> if(engine.b2b) 12 else 8// Twister 3 lines
		}
		lines==1 -> 1 // Single
		lines==2 -> if(engine.split) 4 else 3 // Double
		lines==3 -> if(engine.split) if(engine.b2b) 7 else 6 else 5 // Triple
		lines>=4 -> if(engine.b2b) 12 else 8 // Quads
		else -> 0
	}+when(val it = engine.combo) {
		if(it<=1) it else 0 -> 0
		in 2..4 -> 1
		in 5..8 -> 2
		else -> 3
	}+if(lines>=1&&engine.field.isEmpty) 18 else 0 // All clear

	/**VS Attack lines based tetr.io garbaging by osk, g3ner1c, emmachase*/
	fun calcPower(engine:GameEngine, lines:Int):Int = if(lines<=0) 0 else (when {
		engine.twist -> (if(engine.lastEventShape==Piece.Shape.T) lines*2 else lines+1)
		lines<=3 -> maxOf(0, lines-1)
		else -> lines
	}+engine.b2bCount.let {b ->
		if(b>0) (floor(1+ln1p(b*.8f))+if(b>2) floor(ln1p(b*.8f)%1/3) else 0f).toInt()
		else 0
	}).let {if(it<=0) floor(ln1p(engine.combo*1.25f)).toInt() else engine.combo*it/4+it}+
		if(engine.field.isEmpty) 10 else 0

	override fun onLockFlash(engine:GameEngine):Boolean = false
	override fun onMove(engine:GameEngine):Boolean = false
	override fun onReady(engine:GameEngine):Boolean {
		menuTime = 0
		return false
	}

	override fun onResult(engine:GameEngine):Boolean = false
	override fun onSetting(engine:GameEngine):Boolean {
		// Menu
		if(!engine.owner.replayMode) {

			// Configuration changes val change = updateCursor(engine, 5)
			updateMenu(engine)

			// 決定
			if(menuTime<5) menuTime++ else if(engine.ctrl.isPush(Controller.BUTTON_A)) {
				engine.playSE("decide")
				saveSetting(owner.modeConfig, engine)
				owner.saveModeConfig()
				return false
			}
			// Cancel
			if(engine.ctrl.isPush(Controller.BUTTON_B)) engine.quitFlag = true

		} else {
			menuTime++
			menuCursor = -1
			return menuTime<60
		}
		return true
	}

	override fun onProfile(engine:GameEngine):Boolean {
		showPlayerStats = false
		return false
	}

	override fun onFieldEdit(engine:GameEngine):Boolean = false
	override fun playerInit(engine:GameEngine) {
		owner = engine.owner
		menuTime = 0
		scDisp = 0
		loadSetting(if(owner.replayMode) owner.replayProp else owner.modeConfig, engine.ruleOpt.strRuleName, engine.playerID)
	}

	override fun renderARE(engine:GameEngine) {}
	override fun renderCustom(engine:GameEngine) {}
	override fun renderEndingStart(engine:GameEngine) {}
	override fun renderExcellent(engine:GameEngine) {}
	override fun renderFirst(engine:GameEngine) {}
	override fun renderGameOver(engine:GameEngine) {}
	override fun renderLast(engine:GameEngine) {}
	override fun renderLineClear(engine:GameEngine) {}
	/** medal の文字色を取得
	 * @param medalColor medal 状態
	 * @return medal の文字色
	 */
	fun getMedalFontColor(medalColor:Int):COLOR? = when(medalColor) {
		1 -> COLOR.RED
		2 -> COLOR.WHITE
		3 -> COLOR.YELLOW
		4 -> COLOR.CYAN
		else -> null
	}

	fun getTimeFontColor(time:Int):COLOR = when {
		time<600 -> if(time/10%2==0) COLOR.RED else COLOR.WHITE
		time in 600 until 1200 -> COLOR.ORANGE
		time in 1200 until 1800 -> COLOR.YELLOW
		else -> if(time/5%12==0) COLOR.GREEN else COLOR.WHITE
	}

	override fun renderLockFlash(engine:GameEngine) {}
	override fun renderMove(engine:GameEngine) {}
	override fun renderReady(engine:GameEngine) {}
	override fun renderResult(engine:GameEngine) {}
	override fun renderSetting(engine:GameEngine) {
		//TODO: Custom page breaks

		menu.drawMenu(engine, engine.playerID, receiver, 0)
	}

	override fun renderProfile(engine:GameEngine) {}
	override fun renderFieldEdit(engine:GameEngine) {}
	override fun saveReplay(engine:GameEngine, prop:CustomProperties):Boolean = false
	override fun startGame(engine:GameEngine) {}
	override fun netplayInit(obj:NetLobbyFrame) {}
	override fun netplayUnload(obj:NetLobbyFrame) {}
	override fun netplayOnRetryKey(engine:GameEngine) {}
	/** Update menu cursor
	 * @param engine GameEngine
	 * @param maxCursor Max value of cursor position
	 * @return -1 if Left key is pressed, 1 if Right key is pressed, 0 otherwise
	 */
	protected open fun updateCursor(engine:GameEngine, maxCursor:Int):Int {
		// Up
		if(engine.ctrl.isMenuRepeatKey(Controller.BUTTON_UP)) {
			menuCursor--
			if(menuCursor<0) menuCursor = maxCursor
			engine.playSE("cursor")
		}
		// Down
		if(engine.ctrl.isMenuRepeatKey(Controller.BUTTON_DOWN)) {
			menuCursor++
			if(menuCursor>maxCursor) menuCursor = 0
			engine.playSE("cursor")
		}

		// Configuration changes
		if(engine.ctrl.isMenuRepeatKey(Controller.BUTTON_LEFT)) return -1
		return if(engine.ctrl.isMenuRepeatKey(Controller.BUTTON_RIGHT)) 1 else 0
	}

	protected fun rangeCursor(x:Int, min:Int, max:Int):Int = if(x>max) min else if(x<min) max else x
	// Configuration changes
	protected fun updateMenu(engine:GameEngine):Int = updateCursor(engine, menu.size-1).apply {
		if(this!=0) {
			engine.playSE("change")
			val fast = engine.ctrl.isPush(Controller.BUTTON_C).toInt()+engine.ctrl.isPush(Controller.BUTTON_D).toInt()*2
			menu.change(menuCursor, this, fast)
		}
	}

	protected fun initMenu(y:Int = 0, color:COLOR = menuColor, statc:Int = statcMenu) {
		menuY = y
		menuColor = color
		statcMenu = statc
	}

	protected fun drawMenu(engine:GameEngine, receiver:EventReceiver, vararg value:Pair<String, Any>) {
		value.forEachIndexed {y, it ->
			val str = it.second.let {
				when(it) {
					is String -> it
					is Boolean -> it.getONorOFF(true)
					else -> "$it"
				}
			}
			receiver.drawMenuFont(engine, 0, menuY+y*2, it.first, color = menuColor)
			if(menuCursor==statcMenu+y&&!engine.owner.replayMode)
				receiver.drawMenuFont(engine, 0, menuY+1+y*2, "\u0082${str}", true)
			else receiver.drawMenuFont(engine, 1, menuY+1+y*2, str)
		}

		statcMenu += value.size
		menuY += value.size*2
	}

	protected fun drawMenu(engine:GameEngine, receiver:EventReceiver, color:COLOR = menuColor, vararg value:Pair<String, Any>) {
		menuColor = color
		drawMenu(engine, receiver, *value)
	}

	protected fun drawMenu(
		engine:GameEngine, receiver:EventReceiver, y:Int = menuY,
		color:COLOR = menuColor, statc:Int = statcMenu, vararg value:Pair<String, Any>
	) {
		menuY = y
		menuColor = color
		statcMenu = statc
		drawMenu(engine, receiver, *value)
	}

	protected fun drawMenuBGM(engine:GameEngine, receiver:EventReceiver, bgmno:Int, color:COLOR = menuColor) {
		val cur = menuCursor==statcMenu&&!engine.owner.replayMode
		receiver.drawMenuFont(engine, 0, menuY, "BGM", color)
		receiver.drawMenuFont(engine, 0, menuY+1, BGM.values[bgmno].drawName, cur)
		receiver.drawMenuNum(engine, 8, menuY, String.format("%2d", bgmno), cur)
		if(cur) receiver.drawMenuFont(engine, 7, menuY, "\u0082", true)
		statcMenu++
		menuY += 2
	}

	protected fun drawMenuCompact(engine:GameEngine, receiver:EventReceiver, vararg value:Pair<String, Any>) {
		for(it in value) {
			val label = it.first
			val str:String = it.second.let {
				when(it) {
					is String -> it
					is Boolean -> if(label.length<6) it.getONorOFF(false)
					else it.getOX
					else -> "$it"
				}
			}
			receiver.drawMenuFont(engine, 1, menuY, "${label}:", color = menuColor)
			if(menuCursor==statcMenu&&!engine.owner.replayMode) {
				receiver.drawMenuFont(engine, 0, menuY, "\u0082", true)
				receiver.drawMenuFont(engine, label.length+2, menuY, str, true)
			} else
				receiver.drawMenuFont(engine, label.length+2, menuY, str)
			statcMenu++
			menuY++
		}
	}

	protected fun drawMenuCompact(
		engine:GameEngine, receiver:EventReceiver, color:COLOR = menuColor, vararg value:Pair<String, Any>
	) {
		menuColor = color
		drawMenuCompact(engine, receiver, *value)
	}

	protected fun drawMenuCompact(
		engine:GameEngine, receiver:EventReceiver, y:Int = menuY,
		color:COLOR = menuColor, statc:Int = statcMenu, vararg value:Pair<String, Any>
	) {
		menuY = y
		menuColor = color
		statcMenu = statc
		drawMenuCompact(engine, receiver, *value)
	}

	protected fun drawGravity(
		engine:GameEngine, receiver:EventReceiver,
		g:Int = engine.speed.gravity, d:Int = engine.speed.denominator
	) {
		receiver.drawMenuFont(engine, 0, menuY, "SPEED", color = menuColor)
		receiver.drawMenuSpeed(engine, 5, menuY+1, g, d, 5)
		for(i in 0..1) {
			if(menuCursor==statcMenu&&!engine.owner.replayMode) {
				receiver.drawMenuFont(engine, 5, menuY, "\u0082", true)
				receiver.drawMenuNum(engine, 6, menuY, String.format("%5d", if(i==0) g else d), true)
			} else receiver.drawMenuNum(engine, 6, menuY, String.format("%5d", if(i==0) g else d))
			statcMenu++
			menuY++
		}
	}

	protected fun drawGravity(
		engine:GameEngine, receiver:EventReceiver, y:Int, color:COLOR, statc:Int,
		g:Int = engine.speed.gravity, d:Int = engine.speed.denominator
	) {
		menuY = y
		menuColor = color
		statcMenu = statc
		drawGravity(engine, receiver, g, d)
	}

	protected fun drawMenuSpeeds(
		engine:GameEngine, receiver:EventReceiver, y:Int, color:COLOR, statc:Int, g:Int,
		d:Int, are:Int, aline:Int, lined:Int, lock:Int, das:Int
	) {
		menuY = y
		menuColor = color
		statcMenu = statc
		drawMenuSpeeds(engine, receiver, g, d, are, aline, lined, lock, das)
	}

	protected fun drawMenuSpeeds(
		engine:GameEngine, receiver:EventReceiver, y:Int, color:COLOR, statc:Int, speed:SpeedParam = engine.speed,
		showG:Boolean = true
	) {
		menuY = y
		menuColor = color
		statcMenu = statc
		drawMenuSpeeds(engine, receiver, speed, showG)
	}

	protected fun drawMenuSpeeds(
		engine:GameEngine, receiver:EventReceiver, spd:SpeedParam = engine.speed,
		showG:Boolean = true
	) =
		if(showG) drawMenuSpeeds(
			engine, receiver, spd.gravity, spd.denominator,
			spd.are, spd.areLine, spd.lineDelay, spd.lockDelay, spd.das
		)
		else drawMenuSpeeds(engine, receiver, spd.are, spd.areLine, spd.lineDelay, spd.lockDelay, spd.das)

	protected fun drawMenuSpeeds(
		engine:GameEngine, receiver:EventReceiver,
		g:Int, d:Int, are:Int, aline:Int, lined:Int, lock:Int, das:Int
	) {
		drawGravity(engine, receiver, g, d)
		drawMenuSpeeds(engine, receiver, are, aline, lined, lock, das)
	}

	protected fun drawMenuSpeeds(
		engine:GameEngine, receiver:EventReceiver,
		are:Int, aline:Int, lined:Int, lock:Int, das:Int
	) {

		for(i in 0..1) {
			val cur = menuCursor==statcMenu&&!engine.owner.replayMode
			val show = if(i==0) "ARE" to are else "LINE" to aline
			if(cur) receiver.drawMenuFont(engine, 3+i*3, menuY, "\u0082", true)

			receiver.drawMenuNum(
				engine, 4+i*3, menuY,
				String.format(if(i==0) "%2d/" else "%2d", show.second), cur
			)
			receiver.drawMenuNano(engine, 6+i*6, menuY*2+1, show.first, menuColor, .5f)
			statcMenu++
		}
		menuY++
		for(i in 0..2) {
			val cur = menuCursor==statcMenu&&!engine.owner.replayMode
			val show = when(i) {
				0 -> "LINE" to lined
				1 -> "LOCK" to lock
				else -> "DAS" to das
			}
			if(cur) receiver.drawMenuFont(engine, 7-i*3, menuY, "\u0082", true)

			receiver.drawMenuNum(
				engine, 8-i*3, menuY,
				String.format(if(i==1) "%2d+" else "%2d", show.second), cur
			)
			receiver.drawMenuNano(engine, 14-i*6, menuY*2+1, show.first, menuColor, .5f)
			statcMenu++
		}
		receiver.drawMenuNano(engine, 0, menuY*2-2, "DELAYS", menuColor, .5f)
		//receiver.drawMenuFont(engine, 0, menuY-1, show, color = menuColor)
		menuY++
	}

	protected fun drawScoreSpeeds(
		engine:GameEngine, receiver:EventReceiver,
		x:Int, y:Int, g:Int = engine.speed.gravity, d:Int = engine.speed.denominator,
		are:Int = engine.speed.are, aline:Int = engine.speed.areLine, lined:Int = engine.speed.lineDelay,
		lock:Int = engine.speed.lockDelay, das:Int = engine.speed.das
	) {
		receiver.drawScoreSpeed(engine, x, 0, g, d, 4)
		receiver.drawMenuNum(engine, x, y+1, "${g/1.0/d}")

		listOf(are, aline).forEachIndexed {i, _ ->
			receiver.drawScoreNum(
				engine, x+4+i*3, y, String.format(if(i==0) "%2d/" else "%2d", if(i==0) are else aline)
			)
		}

		for(i in 0..2) {
			receiver.drawScoreNum(
				engine, x+8-i*3, y+1, String.format(if(i==1) "%2d+" else "%2d", if(i==0) lined else if(i==1) lock else das)
			)
		}
	}

	protected fun drawResult(engine:GameEngine, receiver:EventReceiver, y:Int, color:COLOR, str:String, num:Int) {
		receiver.drawMenuFont(engine, 0, y, str, color = color)
		receiver.drawMenuNum(engine, 0, y+1, String.format("%13d", num), color = COLOR.WHITE)
	}

	protected fun drawResult(engine:GameEngine, receiver:EventReceiver, y:Int, color:COLOR, str:String, num:Float) {
		receiver.drawMenuFont(engine, 0, y, str, color = color)
		receiver.drawMenuNum(engine, 0, y+1, String.format("%13g", num), color = COLOR.WHITE)
	}

	protected fun drawResult(engine:GameEngine, receiver:EventReceiver, y:Int, color:COLOR, vararg str:String) =
		drawResultScale(engine, receiver, y, color, 1f, *str)

	protected fun drawResultScale(
		engine:GameEngine, receiver:EventReceiver, y:Int,
		color:COLOR = COLOR.WHITE, scale:Float,
		vararg str:String
	) {
		for(i in str.indices)
			receiver.drawMenuFont(engine, 0, y+i, str[i], if(i and 1==0) color else COLOR.WHITE, scale)
	}

	protected fun drawResultRank(
		engine:GameEngine, receiver:EventReceiver, y:Int, color:COLOR, scale:Float,
		rank:Int, str:String
	) {
		if(rank!=-1) {
			val postfix = when {
				rank%10==0&&rank%100!=10 -> "st"
				rank%10==1&&rank%100!=11 -> "nd"
				rank%10==2&&rank%100!=12 -> "rd"
				else -> "th"
			}
			receiver.drawMenuFont(engine, 5, y, postfix, color, scale)
			receiver.drawMenuFont(engine, 5, y+1, str, color, scale*.8f)
			receiver.drawMenuNum(engine, 0, y, String.format("%3d", rank+1), scale = scale*2)
		} else {
			receiver.drawMenuFont(engine, 0, y, "OUT OF THE", color, scale)
			receiver.drawMenuFont(engine, 0, y+1, String.format("%12s", str), color, scale*.8f)
		}
	}

	protected fun drawResultRank(engine:GameEngine, receiver:EventReceiver, y:Int, color:COLOR, rank:Int) {
		drawResultRankScale(engine, receiver, y, color, 1f, rank)
	}

	protected fun drawResultRankScale(engine:GameEngine, receiver:EventReceiver, y:Int, color:COLOR, scale:Float, rank:Int) {
		drawResultRank(engine, receiver, y, color, scale, rank, "RANK")
	}

	protected fun drawResultNetRank(engine:GameEngine, receiver:EventReceiver, y:Int, color:COLOR, rank:Int) {
		drawResultNetRankScale(engine, receiver, y, color, 1f, rank)
	}

	protected fun drawResultNetRankScale(engine:GameEngine, receiver:EventReceiver, y:Int, color:COLOR, scale:Float, rank:Int) {
		drawResultRank(engine, receiver, y, color, scale, rank, "ONLINE")
	}

	protected fun drawResultNetRankDaily(engine:GameEngine, receiver:EventReceiver, y:Int, color:COLOR, rank:Int) {
		drawResultNetRankDailyScale(engine, receiver, y, color, 1f, rank)
	}

	protected fun drawResultNetRankDailyScale(engine:GameEngine, receiver:EventReceiver, y:Int, color:COLOR, scale:Float, rank:Int) {
		drawResultRank(engine, receiver, y, color, scale, rank, "OF DAILY")
	}

	protected fun drawResultStats(engine:GameEngine, receiver:EventReceiver, y:Int, color:COLOR, vararg stats:Statistic) {
		drawResultStatsScale(engine, receiver, y, color, 1f, *stats)
	}

	protected fun drawResultStatsScale(engine:GameEngine, receiver:EventReceiver, startY:Int, color:COLOR, scale:Float, vararg stats:Statistic) {
		var y = startY
		for(stat in stats) {
			when(stat) {
				Statistic.SCORE -> {
					receiver.drawMenuFont(engine, 0, y, "Score", color, scale*.75f)
					receiver.drawMenuNum(engine, 0, y, String.format("%7d", engine.statistics.score), scale = scale*1.9f)
				}
				Statistic.ATTACKS -> {
					receiver.drawMenuFont(engine, 6, y, "Lines", color, scale*.8f)
					receiver.drawMenuFont(engine, 6, y+1, "Sent", color, scale*.8f)
					receiver.drawMenuNum(engine, 0, y, String.format("%4d", engine.statistics.attacks), scale = 2f)
				}
				Statistic.TIME -> {
					receiver.drawMenuFont(engine, 0, y, "Elapsed Time", color, scale*.8f)
					receiver.drawMenuNum(
						engine, 0, y, String.format("%8s", engine.statistics.time.toTimeStr), scale = scale*1.7f
					)
				}
				Statistic.LEVEL -> {
					receiver.drawMenuFont(engine, 0, y+1, "Level", color, scale)
					receiver.drawMenuNum(engine, 5, y, String.format("%03d", engine.statistics.level+1), scale = scale*2f)
				}
				Statistic.LEVEL_ADD_DISP -> {
					receiver.drawMenuFont(engine, 0, y, "Level", color, scale)
					receiver.drawMenuNum(
						engine, 0, y+1, String.format("%10d", engine.statistics.level+engine.statistics.levelDispAdd),
						scale = scale
					)
				}
				Statistic.LEVEL_MANIA -> {
					receiver.drawMenuFont(engine, 0, y+1, "Level", color, scale)
					receiver.drawMenuNum(engine, 5, y, String.format("%03d", engine.statistics.level), scale = scale*2f)
				}
				Statistic.LINES -> {
					receiver.drawMenuFont(engine, 6, y, "Lines", color, scale*.8f)
					receiver.drawMenuFont(engine, 6, y+1, "clear", color, scale*.8f)
					receiver.drawMenuNum(engine, 0, y, String.format("%4d", engine.statistics.lines), scale = scale*2f)
				}
				Statistic.PIECE -> {
					receiver.drawMenuNum(
						engine, 0, y, String.format("%4d", engine.statistics.totalPieceLocked), scale = scale*2f
					)
					receiver.drawMenuFont(engine, 6, y, "Pieces", color, scale*.8f)
					receiver.drawMenuFont(engine, 6, y+1, "set", color, scale*.8f)
				}
				Statistic.MAXCOMBO -> {
					receiver.drawMenuFont(engine, 0, y, "Best", color, scale*.8f)
					receiver.drawMenuFont(engine, 6, y, "Combo", color, scale*.8f)
					receiver.drawMenuFont(engine, 6, y+1, "HITS", color, scale)
					receiver.drawMenuNum(engine, 0, y, String.format("%4d", engine.statistics.maxCombo), scale = scale*2f)
				}
				Statistic.MAXB2B -> {
					receiver.drawMenuFont(engine, 0, y, "Back2Back Peak", color, scale*.75f)
					receiver.drawMenuFont(engine, 6, y+1, "Chain", color, scale*.8f)
					receiver.drawMenuNum(engine, 0, y, String.format("%4d", engine.statistics.maxB2B), scale = scale*2f)
				}
				Statistic.MAXCHAIN -> {
					receiver.drawMenuFont(engine, 0, y, "Longest", color, scale*.8f)
					receiver.drawMenuFont(engine, 6, y, "Chain", color, scale*.8f)
					receiver.drawMenuFont(engine, 6, y+1, "HITS", color, scale)
					receiver.drawMenuNum(
						engine, 0, y, String.format("%4d", engine.statistics.maxChain-1), scale = scale*2f
					)
				}
				Statistic.SPL -> {
					receiver.drawMenuFont(engine, 0, y, "Score/Line", color, scale)
					receiver.drawMenuNum(engine, 0, y+1, String.format("%13f", engine.statistics.spl), scale = scale)
				}
				Statistic.SPM -> {
					receiver.drawMenuFont(engine, 0, y, "Score/min", color, scale)
					receiver.drawMenuNum(engine, 0, y+1, String.format("%13f", engine.statistics.spm), scale = scale)
				}
				Statistic.SPS -> {
					receiver.drawMenuFont(engine, 0, y, "Score/sec", color, scale)
					receiver.drawMenuNum(engine, 0, y+1, String.format("%13f", engine.statistics.sps), scale = scale)
				}
				Statistic.LPM -> {
					receiver.drawMenuFont(engine, 0, y, "Lines/min", color, scale)
					receiver.drawMenuNum(engine, 0, y+1, String.format("%13f", engine.statistics.lpm), scale = scale)
				}
				Statistic.LPS -> {
					receiver.drawMenuFont(engine, 0, y, "Lines/sec", color, scale)
					receiver.drawMenuNum(engine, 0, y+1, String.format("%13f", engine.statistics.lps), scale = scale)
				}
				Statistic.PPM -> {
					receiver.drawMenuFont(engine, 0, y, "Pieces/min", color, scale)
					receiver.drawMenuNum(engine, 0, y+1, String.format("%13f", engine.statistics.ppm), scale = scale)
				}
				Statistic.PPS -> {
					receiver.drawMenuFont(engine, 5, y, "Pieces", color, scale*.8f)
					receiver.drawMenuNum(
						engine, 0, y, String.format("%4d", engine.statistics.totalPieceLocked), scale = scale*1.5f
					)
					receiver.drawMenuFont(engine, 0, y+1, "/sec", color, scale)
					receiver.drawMenuNum(engine, 4, y+1, String.format("%8f", engine.statistics.pps), scale = scale)
				}
				Statistic.APM -> {
					receiver.drawMenuFont(engine, 0, y, "Spikes/min", color, scale)
					receiver.drawMenuNum(engine, 0, y+1, String.format("%13f", engine.statistics.apm), scale = scale)
				}
				Statistic.APL -> {
					receiver.drawMenuFont(engine, 0, y, "Spikes/Line", color, scale)
					receiver.drawMenuNum(engine, 0, y+1, String.format("%13f", engine.statistics.apl), scale = scale)
				}
				Statistic.VS -> {
					receiver.drawMenuFont(engine, 0, y, "Rank Point", color = color, scale = scale*.8f)
					receiver.drawMenuNum(engine, 0, y, String.format("%7f", engine.statistics.vs), scale = scale*1.7f)
				}
			}
			y += 2
		}
	}
	/** Default method to render controller input display
	 * @param engine GameEngine
	 */
	override fun renderInput(engine:GameEngine) {
		val receiver = engine.owner.receiver
		val pid = engine.playerID
		val x = 170*(pid+1)-110
		val y = 464
		val col = EventReceiver.getPlayerColor(pid)
		val ctrl = engine.ctrl
		receiver.drawDirectFont(x+0*16, y, "<", if(ctrl.isPress(Controller.BUTTON_LEFT)) col else COLOR.WHITE, 1f)
		receiver.drawDirectFont(x+1*16, y, "\u008e", if(ctrl.isPress(Controller.BUTTON_DOWN)) col else COLOR.WHITE)
		receiver.drawDirectFont(x+2*16, y, "\u008b", if(ctrl.isPress(Controller.BUTTON_UP)) col else COLOR.WHITE)
		receiver.drawDirectFont(x+3*16, y, ">", if(ctrl.isPress(Controller.BUTTON_RIGHT)) col else COLOR.WHITE)
		receiver.drawDirectFont(x+4*16, y, "A", if(ctrl.isPress(Controller.BUTTON_A)) col else COLOR.WHITE)
		receiver.drawDirectFont(x+5*16, y, "B", if(ctrl.isPress(Controller.BUTTON_B)) col else COLOR.WHITE)
		receiver.drawDirectFont(x+6*16, y, "C", if(ctrl.isPress(Controller.BUTTON_C)) col else COLOR.WHITE)
		receiver.drawDirectFont(x+7*16, y, "D", if(ctrl.isPress(Controller.BUTTON_D)) col else COLOR.WHITE)
		receiver.drawDirectFont(x+8*16, y, "E", if(ctrl.isPress(Controller.BUTTON_E)) col else COLOR.WHITE)
		receiver.drawDirectFont(x+9*16, y, "F", if(ctrl.isPress(Controller.BUTTON_F)) col else COLOR.WHITE)
		receiver.drawDirectNano(x, y, String.format("%1dP:%04d", pid+1, engine.ctrl.buttonBit), col, 0.5f)
	}
	/** Total score */
	enum class Statistic {
		SCORE, ATTACKS, LINES, TIME, VS,
		LEVEL, LEVEL_MANIA, PIECE,
		MAXCOMBO, MAXB2B, SPL, SPM, SPS,
		LPM, LPS, PPM, PPS, APL, APM,
		MAXCHAIN, LEVEL_ADD_DISP
	}

}
