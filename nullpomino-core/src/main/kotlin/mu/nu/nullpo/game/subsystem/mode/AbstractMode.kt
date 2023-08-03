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
import mu.nu.nullpo.game.event.EventReceiver.Companion.getScaleF
import mu.nu.nullpo.game.event.ScoreEvent
import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.game.play.GameManager
import mu.nu.nullpo.game.play.GameStyle
import mu.nu.nullpo.game.subsystem.mode.menu.MenuList
import mu.nu.nullpo.game.subsystem.mode.menu.PresetItem
import mu.nu.nullpo.gui.common.BaseFont
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

	override val rankMap:rankMapType = emptyMap()
	override val rankPersMap:rankMapType = emptyMap()

	protected fun rankMapOf(list:List<Pair<String, MutableList<*>>>):rankMapType =
		list.filterIsInstance<Pair<String, rankMapChild>>()
			.toMap()

	protected fun rankMapOf(vararg array:Pair<String, MutableList<*>>):rankMapType = rankMapOf(listOf(*array))

	protected var menuColor = COLOR.WHITE
	/*abstract */override val menu = MenuList(id)
	protected var statcMenu
		get() = menu.menuSubPos
		set(x) {
			menu.menuSubPos = x
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
	override val isOnlineMode = false
	override val isVSMode = false
	/** Show Player profile */
	protected var showPlayerStats = false
	protected val playerProperties:List<ProfileProperties> = owner.engine.map {it.playerProp}

	protected fun playerProperties(playerID:Int):ProfileProperties = owner.engine[playerID].playerProp

	override fun loadSetting(engine:GameEngine, prop:CustomProperties, ruleName:String, playerID:Int) =
		menu.load(prop, engine, ruleName, if(players<=1) -1 else playerID)

	fun loadSetting(engine:GameEngine) = loadSetting(engine, owner.modeConfig)

	@Deprecated("Set Parameter from GameEngine", ReplaceWith("loadSetting(prop, engine)"))
	protected fun loadSetting(prop:CustomProperties) = loadSetting(owner.engine.first(), prop)

	override fun saveSetting(engine:GameEngine, prop:CustomProperties, ruleName:String, playerID:Int) {
		menu.save(prop,engine,ruleName,  if(players<=1) -1 else playerID)
		owner.saveModeConfig()
	}

	@Deprecated("Set Parameter from GameEngine", ReplaceWith("saveSetting(prop, engine)"))
	fun saveSetting(prop:CustomProperties) = saveSetting(owner.engine.first(), prop)

	/** Read rankings from [prop]
	 * This should be called from Only [GameEngine] */
	override fun loadRanking(prop:CustomProperties) {
		if(rankMap.isNotEmpty()) rankMap.forEach {(key, value) ->
			value.forEachIndexed {i, n ->
				value[i] = prop.getProperty("${key}.$i", n)
			}
		}
	}

	override fun saveRanking() {
		if(rankMap.isNotEmpty()) rankMap.forEach {(key, value) ->
			value.forEachIndexed {i, rec ->
				owner.recordProp.setProperty("$key.$i", rec)
			}
		}
	}

	override fun loadRankingPlayer(prof:ProfileProperties) {
		if(rankPersMap.isNotEmpty()&&prof.isLoggedIn) rankPersMap.forEach {(key, value) ->
			value.forEachIndexed {i, n ->
				value[i] = prof.propProfile.getProperty("$id.${key}.$i", n)
			}
		}
	}

	override fun saveRankingPlayer(prof:ProfileProperties) {
		if(rankPersMap.isNotEmpty()&&prof.isLoggedIn) rankPersMap.forEach {(key, value) ->
			value.forEachIndexed {i, v ->
				prof.propProfile.setProperty("$id.${key}.$i", v)
			}
		}
	}

	/** Save rankings [map] to prop */
	internal inline fun <reified T:Comparable<T>> saveRanking(map:List<Pair<String, T>>) =
		saveRanking(map.toMap())
	/** Save rankings [map] */
	internal inline fun <reified T:Comparable<T>> saveRanking(map:Map<String, T>) {
		map.forEach {(key, it) ->
			owner.recordProp.setProperty(key, it)
		}
	}

	override fun pieceLocked(engine:GameEngine, lines:Int, finesse:Boolean) {}
	override fun lineClearEnd(engine:GameEngine):Boolean = false
	override fun afterHardDropFall(engine:GameEngine, fall:Int) {}
	override fun afterSoftDropFall(engine:GameEngine, fall:Int) {}
	override fun blockBreak(engine:GameEngine, blk:Map<Int, Map<Int, Block>>):Boolean = false
	override fun lineClear(gameEngine:GameEngine, i:Collection<Int>) {}
	override fun fieldEditExit(engine:GameEngine) {}
	override fun loadReplay(engine:GameEngine, prop:CustomProperties) {}
	override fun modeInit(manager:GameManager) {
		menuTime = 0
		owner = manager
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
	 */
	@Deprecated(
		"use ScoreEvent instead lines",
		ReplaceWith(
			"calcScore(engine, ScoreEvent(engine.nowPieceObject, lines, engine.b2bCount, engine.combo, engine.twistType, engine.split))",
			"mu.nu.nullpo.game.event.ScoreEvent"
		)
	)
	fun calcScore(engine:GameEngine, lines:Int):Int =
		calcScore(engine, ScoreEvent(engine.nowPieceObject, lines, engine.b2bCount, engine.combo, engine.twistType, engine.split))
	/** Calculates line-clear score
	 * (This function will be called even if no lines are cleared)
	 * @param engine GameEngine
	 * @param ev Cleared Lines Data
	 */
	override fun calcScore(engine:GameEngine, ev:ScoreEvent):Int {
		val spd = maxOf(0, engine.lockDelay-engine.lockDelayNow)+if(engine.manualLock) 1 else 0
		val pts = calcScoreBase(engine, ev)
		val get = calcScoreCombo(
			pts, ev.combo,
			engine.statistics.level, spd
		)
		if(ev.lines>=1) engine.statistics.scoreLine += get
		else engine.statistics.scoreBonus += get
		scDisp += minOf(get, spd)
		lastScore = get
		return if(pts>0||gameIntensity==2) get else 0
	}

	var renSum = 0; private set

	/** Time to display the most recent increase in score */
	var scDisp = 0L//; private set

	/** Most recent increase in score */
	var lastScore = 0

	/** npmAlt style Scoring: base Point */
	fun calcScoreBase(engine:GameEngine, ev:ScoreEvent):Int {
		val ln = ev.lines
		val pts = when {
			ev.twist -> when {
				ln<=0&&!ev.twistEZ -> if(ev.twistMini) 5 else 8// Twister 0 lines
				ev.twistEZ -> (if(engine.b2b) 11+ln else 0)+when(ln) {
					1 -> 20
					2 -> 40
					else -> ln*25
				}
// Immobile Spin
				ln==1 -> if(ev.twistMini) (if(ev.b2b>0) 32 else 24)//Mini Twister 1 lines
				else if(ev.piece?.type==Piece.Shape.T) if(ev.b2b>0) 75 else 50 else if(ev.b2b>0) 50 else 25// Twister 1 line
				ln==2 -> if(ev.twistMini) (if(ev.b2b>0) 100 else 75)//Mini Twister 2 lines
				else if(ev.piece?.type==Piece.Shape.T) if(ev.b2b>0) 160 else 128 else if(ev.b2b>0) 125 else 100// Twister 2 lines
				ln==3 -> if(ev.piece?.type==Piece.Shape.T) if(ev.b2b>0) 205 else 172 else if(ev.b2b>0) 175 else 150// Twister 3 lines
				else -> if(ev.b2b>0) 256 else 192// 180Twister Quads
			}
			ln==1 -> 16 // Single
			ln==2 -> if(ev.split) (if(ev.b2b>0) 77 else 64) else 48 // Double
			ln==3 -> if(ev.split) (if(ev.b2b>0) 128 else 102) else 85 // Triple
			ln>=4 -> if(ev.b2b>0) 192 else 160 // Quads
			else -> 0
		}
		// All clear
		val btb = 100+maxOf(0, engine.b2bCount)
		return if(ln>=1&&engine.field.isEmpty) pts*(1000+btb*7)/70+2048 else pts*btb/10
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
	fun calcPoint(engine:GameEngine, ev:ScoreEvent):Int = ev.lines.let {li ->
		when {
			ev.twist -> when {
				li<=0&&!ev.twistEZ -> 1 // Twister 0 lines
				ev.twistEZ&&li>0 -> li*2+(ev.b2b>0).toInt() // Immobile EZ Spin
				li==1 -> if(ev.twistMini) if(ev.b2b>0) 3 else 2
				else if(ev.piece?.type==Piece.Shape.T) if(ev.b2b>0) 5 else 3 else if(ev.b2b>0) 4 else 3  // Twister 1 line
				li==2 -> if(ev.twistMini) (if(ev.b2b>0) 6 else 4)
				else if(ev.piece?.type==Piece.Shape.T) if(ev.b2b>0) 9 else 7 else if(ev.b2b>0) 8 else 6 // Twister 2 lines
				else -> if(ev.b2b>0) 12 else 8 // Twister 3 lines
			}
			li==1 -> 1 // Single
			li==2 -> if(ev.split) 4 else 3 // Double
			li==3 -> if(ev.split) if(ev.b2b>0) 7 else 6 else 5 // Triple
			li>=4 -> if(ev.b2b>0) 12 else 8 // Quad
			else -> 0
		}+when(val it = ev.combo) {
			if(it<=1) it else 0 -> 0
			in 2..4 -> 1
			in 5..8 -> 2
			else -> 3
		}+if(li>=1&&engine.field.isEmpty) 18 else 0 // All clear
	}

	/**VS Attack lines based tetr.io garbage by osk, g3ner1c, emmachase
	 * @param statC if true increase to statistics attacks
	 * @return total,line/twist,bonus*/
	fun calcPower(engine:GameEngine, ev:ScoreEvent, statC:Boolean = false):Int =
		calcPower(engine, ev).also {
			if(statC) {
				if(ev.twist) engine.statistics.attacksTwist += it.base
				else engine.statistics.attacksLine += it.base
				engine.statistics.attacksBonus += it.bonus
			}
		}.total

	fun calcPower(engine:GameEngine, ev:ScoreEvent):PowData {
		val lines = ev.lines
		if(lines<=0) return PowData()
		val base = when {
			ev.twist -> (if(ev.piece?.type==Piece.Shape.T) lines*2 else lines+1)
			lines<=3 -> maxOf(0, lines-1)
			else -> lines
		}
		val it = (ev.b2b.let {c ->
			if(c>0) (floor(1+ln1p(c*.8f))+if(c>2) floor(ln1p(c*.8f)%1/3) else 0f).toInt()
			else 0
		}+base).let {
			val combo = maxOf(0, ev.combo)
			if(it<=0) floor(ln1p(combo*1.25f)).toInt() else it+combo*it/4+engine.field.isEmpty.toInt()*10
		}

		return PowData(base, it-base)
	}

	override fun onLockFlash(engine:GameEngine):Boolean = false
	override fun onMove(engine:GameEngine):Boolean = false
	override fun onReady(engine:GameEngine):Boolean {
		menuTime = 0
		return false
	}

	override fun onResult(engine:GameEngine):Boolean = false
	override fun onSetting(engine:GameEngine):Boolean {
		// Menu
		if(!owner.replayMode&&menu.size>0) {
			// Configuration changes val change = updateCursor(engine, 5)
			updateMenu(engine)

			// 決定
			if(menuTime<5) menuTime++ else if(engine.ctrl.isPush(Controller.BUTTON_A)) {
				engine.playSE("decide")
				saveSetting(engine, owner.modeConfig)
				owner.saveModeConfig()
				onSettingChanged(engine)
				return false
			}
			// Cancel
			if(engine.ctrl.isPush(Controller.BUTTON_B)) engine.quitFlag = true
		} else {
			menuTime++
			menuCursor = -1
			return menuTime<60*(1+(menu.loc.maxOrNull() ?: 0)/engine.fieldHeight)
		}
		return true
	}

	protected open fun onSettingChanged(engine:GameEngine) {}

	override fun onProfile(engine:GameEngine):Boolean {
		showPlayerStats = false
		return false
	}

	override fun onFieldEdit(engine:GameEngine):Boolean = false
	override fun playerInit(engine:GameEngine) {
		menuTime = 0
		scDisp = 0
		loadSetting(engine, if(owner.replayMode) owner.replayProp else owner.modeConfig)
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
		time in 600..<1200 -> COLOR.ORANGE
		time in 1200..<1800 -> COLOR.YELLOW
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
	protected fun updateMenu(engine:GameEngine):Int = updateCursor(engine, menu.menus.size-1).also {sw ->
		if(sw!=0) {
			engine.playSE("change")
			val fast = engine.ctrl.isPush(Controller.BUTTON_C).toInt()+engine.ctrl.isPush(Controller.BUTTON_D).toInt()*2
			menu.change(menuCursor, sw, fast)
			onSettingChanged(engine)
		}
		if(engine.ctrl.isPush(Controller.BUTTON_A))
			menu.menus[menuCursor].let {(x, y) ->
				menu[x].let {
					if(it is PresetItem&&it.value is Int) {
						menuTime = -6
						when(y) {
							0 -> it.presetLoad(engine, owner.modeConfig, menu.propName, it.value as Int)
							1 -> {
								it.presetSave(engine, owner.modeConfig, menu.propName, it.value as Int)
								owner.saveModeConfig()
							}
						}
						onSettingChanged(engine)
					}
				}
			}
		sw
	}

	protected fun initMenu(y:Int = 0, color:COLOR = menuColor, statc:Int = statcMenu) {
		menuY = y
		menuColor = color
		statcMenu = statc
	}

	protected fun drawMenu(engine:GameEngine, receiver:EventReceiver, vararg value:Pair<String, Any>) {
		value.forEachIndexed {y, (key, v) ->
			val str = v.let {
				when(it) {
					is String -> it
					is Boolean -> it.getONorOFF(true)
					else -> "$it"
				}
			}
			receiver.drawMenuFont(engine, 0, menuY+y*2, key, color = menuColor)
			if(menuCursor==statcMenu+y&&!owner.replayMode)
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

	protected fun drawMenuBGM(engine:GameEngine, receiver:EventReceiver, bgmId:Int, color:COLOR = menuColor) {
		val cur = menuCursor==statcMenu&&!owner.replayMode
		receiver.drawMenuFont(engine, 0, menuY, "BGM", color)
		receiver.drawMenuFont(engine, 0, menuY+1, BGM.values[bgmId].drawName, cur)
		receiver.drawMenuNum(engine, 8, menuY, "%2d".format(bgmId), cur)
		if(cur) receiver.drawMenuFont(engine, 7, menuY, BaseFont.CURSOR, true)
		statcMenu++
		menuY += 2
	}

	protected fun drawMenuCompact(engine:GameEngine, receiver:EventReceiver, vararg value:Pair<String, Any>) {
		for((label, second) in value) {
			val str:String = second.let {
				when(it) {
					is String -> it
					is Boolean -> if(label.length<6) it.getONorOFF(false)
					else it.getOX
					else -> "$it"
				}
			}
			receiver.drawMenuFont(engine, 1, menuY, "${label}:", color = menuColor)
			if(menuCursor==statcMenu&&!owner.replayMode) {
				receiver.drawMenuFont(engine, 0, menuY, BaseFont.CURSOR, true)
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
			if(menuCursor==statcMenu&&!owner.replayMode) {
				receiver.drawMenuFont(engine, 5, menuY, BaseFont.CURSOR, true)
				receiver.drawMenuNum(engine, 6, menuY, "%5d".format(if(i==0) g else d), true)
			} else receiver.drawMenuNum(engine, 6, menuY, "%5d".format(if(i==0) g else d))
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
			val cur = menuCursor==statcMenu&&!owner.replayMode
			val show = if(i==0) "ARE" to are else "LINE" to aline
			if(cur) receiver.drawMenuFont(engine, 3+i*3, menuY, BaseFont.CURSOR, true)

			receiver.drawMenuNum(
				engine, 4+i*3, menuY,
				String.format(if(i==0) "%2d/" else "%2d", show.second), cur
			)
			receiver.drawMenuNano(engine, 6+i*6, menuY*2+1, show.first, menuColor, .5f)
			statcMenu++
		}
		menuY++
		for(i in 0..2) {
			val cur = menuCursor==statcMenu&&!owner.replayMode
			val show = when(i) {
				0 -> "LINE" to lined
				1 -> "LOCK" to lock
				else -> "DAS" to das
			}
			if(cur) receiver.drawMenuFont(engine, 7-i*3, menuY, BaseFont.CURSOR, true)

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
		receiver.drawMenuNum(engine, 0, y+1, num, 13 to null, color = COLOR.WHITE)
	}

	protected fun drawResult(engine:GameEngine, receiver:EventReceiver, y:Int, color:COLOR, str:String, num:Float) {
		receiver.drawMenuFont(engine, 0, y, str, color = color)
		receiver.drawMenuNum(engine, 0, y+1, num, 13 to null, color = COLOR.WHITE)
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
			receiver.drawMenuNum(engine, 0, y, "%3d".format(rank+1), scale = scale*2)
		} else {
			receiver.drawMenuFont(engine, 0, y, "OUT OF THE", color, scale)
			receiver.drawMenuFont(engine, 0, y+1, "%12s".format(str), color, scale*.8f)
		}
	}

	protected fun drawResultRank(engine:GameEngine, receiver:EventReceiver, y:Int, color:COLOR, rank:Int) {
		drawResultRankScale(engine, receiver, y, color, getScaleF(engine.displaySize), rank)
	}

	protected fun drawResultRankScale(engine:GameEngine, receiver:EventReceiver, y:Int, color:COLOR, scale:Float, rank:Int) {
		drawResultRank(engine, receiver, y, color, scale, rank, "RANK")
	}

	protected fun drawResultNetRank(engine:GameEngine, receiver:EventReceiver, y:Int, color:COLOR, rank:Int) {
		drawResultNetRankScale(engine, receiver, y, color, getScaleF(engine.displaySize), rank)
	}

	protected fun drawResultNetRankScale(engine:GameEngine, receiver:EventReceiver, y:Int, color:COLOR, scale:Float, rank:Int) {
		drawResultRank(engine, receiver, y, color, scale, rank, "ONLINE")
	}

	protected fun drawResultNetRankDaily(engine:GameEngine, receiver:EventReceiver, y:Int, color:COLOR, rank:Int) {
		drawResultNetRankDailyScale(engine, receiver, y, color, getScaleF(engine.displaySize), rank)
	}

	protected fun drawResultNetRankDailyScale(engine:GameEngine, receiver:EventReceiver, y:Int, color:COLOR, scale:Float, rank:Int) {
		drawResultRank(engine, receiver, y, color, scale, rank, "OF DAILY")
	}

	protected fun drawResultStats(engine:GameEngine, receiver:EventReceiver, y:Int, color:COLOR, vararg stats:Statistic) {
		drawResultStatsScale(engine, receiver, y, color, getScaleF(engine.displaySize), *stats)
	}

	protected fun drawResultStatsScale(engine:GameEngine, receiver:EventReceiver, startY:Int, color:COLOR, scale:Float, vararg stats:Statistic) {
		var y = startY
		for(stat in stats) {
			when(stat) {
				Statistic.SCORE -> {
					receiver.drawMenuFont(engine, 0, y, "Score", color, scale*.75f)
					receiver.drawMenuNum(engine, 0, y, "%7d".format(engine.statistics.score), scale = scale*1.9f)
					receiver.drawMenuNano(engine, 7f, y+1.25f, "Score", color = color, scale = scale*.75f)
				}
				Statistic.ATTACKS -> {
					receiver.drawMenuFont(engine, 6, y, "Lines", color, scale*.8f)
					receiver.drawMenuFont(engine, 6, y+1, "Sent", color, scale*.8f)
					receiver.drawMenuNum(engine, 0, y, "%4d".format(engine.statistics.attacks), scale = scale*2)
				}
				Statistic.TIME -> {
					receiver.drawMenuNum(
						engine, 0f, y+0.33f, "%8s".format(engine.statistics.time.toTimeStr), scale = scale*5/3
					)
					receiver.drawMenuNano(engine, 0, y, "Elapsed Time", color, scale*.75f)
				}
				Statistic.LEVEL -> {
					receiver.drawMenuFont(engine, 0, y+1, "Level", color, scale)
					receiver.drawMenuNum(engine, 5, y, "%03d".format(engine.statistics.level+1), scale = scale*2)
				}
				Statistic.LEVEL_ADD_DISP -> {
					receiver.drawMenuFont(engine, 0, y, "Level", color, scale)
					receiver.drawMenuNum(
						engine, 0, y+1, "%10d".format(engine.statistics.level+engine.statistics.levelDispAdd),
						scale = scale
					)
				}
				Statistic.LEVEL_MANIA -> {
					receiver.drawMenuFont(engine, 0, y+1, "Level", color, scale)
					receiver.drawMenuNum(engine, 5, y, "%03d".format(engine.statistics.level), scale = scale*2)
				}
				Statistic.LINES -> {
					receiver.drawMenuFont(engine, 6, y, "Lines", color, scale*.8f)
					receiver.drawMenuFont(engine, 6, y+1, "clear", color, scale*.8f)
					receiver.drawMenuNum(engine, 0, y, "%4d".format(engine.statistics.lines), scale = scale*2)
				}
				Statistic.PIECE -> {
					receiver.drawMenuNum(
						engine, 0, y, "%4d".format(engine.statistics.totalPieceLocked), scale = scale*2
					)
					receiver.drawMenuFont(engine, 6, y, "Pieces", color, scale*.8f)
					receiver.drawMenuFont(engine, 6, y+1, "set", color, scale*.8f)
				}
				Statistic.MAXCOMBO -> {
					receiver.drawMenuFont(engine, 0, y, "Best", color, scale*.8f)
					receiver.drawMenuFont(engine, 6, y, "Combo", color, scale*.8f)
					receiver.drawMenuFont(engine, 6, y+1, "HITS", color, scale)
					receiver.drawMenuNum(engine, 0, y, "%4d".format(engine.statistics.maxCombo), scale = scale*2)
				}
				Statistic.MAXB2B -> {
					receiver.drawMenuFont(engine, 0, y, "Back2Back Peak", color, scale*.75f)
					receiver.drawMenuFont(engine, 6, y+1, "Chain", color, scale*.8f)
					receiver.drawMenuNum(engine, 0, y, "%4d".format(engine.statistics.maxB2B), scale = scale*2)
				}
				Statistic.MAXCHAIN -> {
					receiver.drawMenuFont(engine, 0, y, "Longest", color, scale*.8f)
					receiver.drawMenuFont(engine, 6, y, "Chain", color, scale*.8f)
					receiver.drawMenuFont(engine, 6, y+1, "HITS", color, scale)
					receiver.drawMenuNum(
						engine, 0, y, "%4d".format(engine.statistics.maxChain-1), scale = scale*2
					)
				}
				Statistic.SPL -> {
					receiver.drawMenuFont(engine, 0, y, "Score/Line", color, scale)
					receiver.drawMenuNum(engine, 0, y+1, engine.statistics.spl, 13 to null, scale = scale)
				}
				Statistic.SPM -> {
					receiver.drawMenuFont(engine, 0, y, "Score/min", color, scale)
					receiver.drawMenuNum(engine, 0, y+1, engine.statistics.spm, 13 to null, scale = scale)
				}
				Statistic.SPS -> {
					receiver.drawMenuFont(engine, 0, y, "Score/sec", color, scale)
					receiver.drawMenuNum(engine, 0, y+1, engine.statistics.sps, 13 to null, scale = scale)
				}
				Statistic.SPP -> {
					receiver.drawMenuFont(engine, 0, y, "Score/Mino", color, scale)
					receiver.drawMenuNum(engine, 0, y+1, engine.statistics.spp, 13 to null, scale = scale)
				}
				Statistic.LPM -> {
					receiver.drawMenuFont(engine, 0, y, "Lines/min", color, scale)
					receiver.drawMenuNum(engine, 0, y+1, engine.statistics.lpm, 13 to null, scale = scale)
				}
				Statistic.LPS -> {
					receiver.drawMenuFont(engine, 0, y, "Lines/sec", color, scale)
					receiver.drawMenuNum(engine, 0, y+1, engine.statistics.lps, 13 to null, scale = scale)
				}
				Statistic.PPM -> {
					receiver.drawMenuFont(engine, 0, y, "Pieces/min", color, scale)
					receiver.drawMenuNum(engine, 0, y+1, engine.statistics.ppm, 13 to null, scale = scale)
				}
				Statistic.PPS -> {
					receiver.drawMenuFont(engine, 5, y, "Pieces", color, scale*.8f)
					receiver.drawMenuNum(
						engine, 0, y, "%4d".format(engine.statistics.totalPieceLocked), scale = scale*1.5f
					)
					receiver.drawMenuFont(engine, 0, y+1, "/sec", color, scale)
					receiver.drawMenuNum(engine, 4, y+1, (engine.statistics.pps), 8 to null, scale = scale)
				}
				Statistic.APM -> {
					receiver.drawMenuFont(engine, 0, y, "Spikes/min", color, scale)
					receiver.drawMenuNum(engine, 0, y+1, (engine.statistics.apm), 13 to null, scale = scale)
				}
				Statistic.APL -> {
					receiver.drawMenuFont(engine, 0, y, "Spikes/Line", color, scale)
					receiver.drawMenuNum(engine, 0, y+1, (engine.statistics.apl), 13 to null, scale = scale)
				}
				Statistic.APP -> {
					receiver.drawMenuFont(engine, 0, y, "Spikes/Mino", color, scale)
					receiver.drawMenuNum(engine, 0, y+1, (engine.statistics.app), 13 to null, scale = scale)
				}
				Statistic.VS -> {
					receiver.drawMenuFont(engine, 0, y, "Rank Point", color = color, scale = scale*.8f)
					receiver.drawMenuNum(engine, 0, y, (engine.statistics.vs), 6 to null, scale = scale*1.7f)
					receiver.drawMenuNano(engine, 4.25f, y+1.25f, "Rank Point", color = color, scale = scale*.75f)
				}
			}
			y += 2
		}
	}
	/** Default method to render controller input display
	 * @param engine GameEngine
	 */
	override fun renderInput(engine:GameEngine) {
		val receiver = owner.receiver
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
		receiver.drawDirectNano(x, y, "%1dP:%04d".format(pid+1, engine.ctrl.buttonBit), col, 0.5f)
	}
	/** Total score */
	enum class Statistic {
		SCORE, ATTACKS, LINES, TIME, VS,
		LEVEL, LEVEL_MANIA, PIECE,
		MAXCOMBO, MAXB2B, SPL, SPM, SPS, SPP,
		LPM, LPS, PPM, PPS, APL, APM, APP,
		MAXCHAIN, LEVEL_ADD_DISP
	}

	companion object {
		data class PowData(val base:Int = 0, val bonus:Int = 0) {
			val total = base+bonus
		}
	}

}
