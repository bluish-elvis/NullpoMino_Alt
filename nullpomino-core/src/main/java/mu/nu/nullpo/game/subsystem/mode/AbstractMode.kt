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

import mu.nu.nullpo.game.component.*
import mu.nu.nullpo.game.component.BGMStatus.BGM
import mu.nu.nullpo.game.event.EventReceiver
import mu.nu.nullpo.game.event.EventReceiver.COLOR
import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.game.play.GameManager
import mu.nu.nullpo.game.subsystem.mode.menu.AbstractMenuItem
import mu.nu.nullpo.gui.net.NetLobbyFrame
import mu.nu.nullpo.util.CustomProperties
import mu.nu.nullpo.util.GeneralUtil
import java.util.*

/** Dummy implementation of game mode. Used as a base of most game modes. */
abstract class AbstractMode:GameMode {

	/** GameManager that owns this mode */
	protected lateinit var owner:GameManager

	/** Drawing and event handling EventReceiver */
	protected lateinit var receiver:EventReceiver

	/** Current state of menu for drawMenu */
	protected var statcMenu:Int = 0
	protected var menuColor:COLOR = COLOR.WHITE
	protected var menuY:Int = 0

	protected val menu:ArrayList<AbstractMenuItem<*>>

	/** Name of mode in properties file */
	protected val propName:String

	/** Position of cursor in menu */
	protected var menuCursor:Int = 0

	/** Number of frames spent in menu */
	protected var menuTime = 0

	override val name:String
		get() = "DUMMY"

	override val players:Int
		get() = 1

	override val gameStyle:Int
		get() = GameEngine.GAMESTYLE_TETROMINO

	override val isNetplayMode:Boolean
		get() = false

	override val isVSMode:Boolean
		get() = false

	/** Total score */
	enum class Statistic {
		SCORE, LINES, TIME,
		LEVEL, LEVEL_MANIA, PIECE,
		MAXCOMBO, SPL, SPM, SPS,
		LPM, LPS, PPM, PPS,
		MAXCHAIN, LEVEL_ADD_DISP
	}

	init {
		statcMenu = 0
		menuCursor = 0
		menuTime = 0
		menuColor = COLOR.WHITE
		menuY = 0
		menu = ArrayList()
		propName = "dummy"
	}

	protected open fun loadSetting(prop:CustomProperties) {
		for(item in menu)
			item.load(-1, prop, propName)
	}

	protected open fun saveSetting(prop:CustomProperties) {
		for(item in menu)
			item.save(-1, prop, propName)
	}

	override fun pieceLocked(engine:GameEngine, playerID:Int, lines:Int) {}

	override fun lineClearEnd(engine:GameEngine, playerID:Int):Boolean = false

	override fun afterHardDropFall(engine:GameEngine, playerID:Int, fall:Int) {}

	override fun afterSoftDropFall(engine:GameEngine, playerID:Int, fall:Int) {}

	override fun blockBreak(engine:GameEngine, playerID:Int, x:Int, y:Int, blk:Block) {}
	override fun lineClear(gameEngine:GameEngine, playerID:Int, i:Int) {}
	override fun calcScore(engine:GameEngine, playerID:Int, lines:Int) {}

	override fun fieldEditExit(engine:GameEngine, playerID:Int) {}

	override fun loadReplay(engine:GameEngine, playerID:Int, prop:CustomProperties) {}

	override fun modeInit(manager:GameManager) {
		menuTime = 0
	}

	override fun onARE(engine:GameEngine, playerID:Int):Boolean = false

	override fun onCustom(engine:GameEngine, playerID:Int):Boolean = false

	override fun onEndingStart(engine:GameEngine, playerID:Int):Boolean = false

	override fun onExcellent(engine:GameEngine, playerID:Int):Boolean = false

	override fun onFirst(engine:GameEngine, playerID:Int) {}

	override fun onGameOver(engine:GameEngine, playerID:Int):Boolean = false

	override fun onLast(engine:GameEngine, playerID:Int) {}

	override fun onLineClear(engine:GameEngine, playerID:Int):Boolean = false

	open fun calcScore(engine:GameEngine, lines:Int):Int {
		menuTime = 0
		var pts = menuTime
		if(engine.tspin) {
			if(lines==0&&!engine.tspinez)
				pts = if(engine.tspinmini) 5 else 7// T-Spin 0 lines
			else if(engine.tspinez&&lines>0)
				pts = 12*lines*lines+(if(engine.b2b) 15 else 0)// Immobile EZ Spin
			else if(lines==1)
				pts += if(engine.tspinmini) if(engine.b2b) 40 else 25 else if(engine.b2b) 50 else 32// T-Spin 1 line
			else if(lines==2)
				pts += if(engine.tspinmini&&engine.useAllSpinBonus) if(engine.b2b) 100 else 64 else if(engine.b2b) 111 else 72// T-Spin 2 lines
			else if(lines>=3) pts += if(engine.b2b) 270 else 180// Twister 3 lines
		} else if(lines==1)
			pts = 10 // 1列
		else if(lines==2)
			pts = if(engine.split) if(engine.b2b) 72 else 64 else 45 // 2列
		else if(lines==3)
			pts = if(engine.split) if(engine.b2b) 160 else 128 else 96 // 3列
		else if(lines>=4) pts = if(engine.b2b) 256 else 192
		// All clear
		if(lines>=1&&engine.field!!.isEmpty) pts += pts*10/7+256
		return pts*10
	}

	override fun onLockFlash(engine:GameEngine, playerID:Int):Boolean = false

	override fun onMove(engine:GameEngine, playerID:Int):Boolean = false

	override fun onReady(engine:GameEngine, playerID:Int):Boolean {
		menuTime = 0
		return false
	}

	override fun onResult(engine:GameEngine, playerID:Int):Boolean = false

	override fun onSetting(engine:GameEngine, playerID:Int):Boolean = false

	override fun onFieldEdit(engine:GameEngine, playerID:Int):Boolean = false

	override fun playerInit(engine:GameEngine, playerID:Int) {
		owner = engine.owner
		receiver = engine.owner.receiver
		menuTime = 0
	}

	override fun renderARE(engine:GameEngine, playerID:Int) {}

	override fun renderCustom(engine:GameEngine, playerID:Int) {}

	override fun renderEndingStart(engine:GameEngine, playerID:Int) {}

	override fun renderExcellent(engine:GameEngine, playerID:Int) {}

	override fun renderFirst(engine:GameEngine, playerID:Int) {}

	override fun renderGameOver(engine:GameEngine, playerID:Int) {}

	override fun renderLast(engine:GameEngine, playerID:Int) {}

	override fun renderLineClear(engine:GameEngine, playerID:Int) {}

	fun renderLineAlert(engine:GameEngine, playerID:Int, receiver:EventReceiver) {
		val lastb2b = engine.b2bcount>1
		if(engine.lastevent!=GameEngine.EVENT_NONE) {
			val strPieceName = Piece.Shape.names[engine.lasteventpiece]

			if(lastb2b) {
				receiver.drawMenuFont(engine, playerID, 0, 20, "BACK 2 BACK", color = COLOR.RED)
				receiver.drawMenuNum(engine, playerID, 0, 22, String.format("%2d", engine.b2bbuf), color = COLOR.YELLOW, scale = 1.5f)
				receiver.drawMenuFont(engine, playerID, 2, 23, "COMBO!", color = COLOR.ORANGE, scale = .75f)
			}
			if(engine.combobuf>=2) {
				receiver.drawMenuNum(engine, playerID, 4, 22, String.format("%2d", engine.combobuf), color = COLOR.CYAN, scale = 1.5f)
				receiver.drawMenuFont(engine, playerID, 6, 23, "HITS CHAIN!", color = COLOR.BLUE, scale = .75f)
			}

			when(engine.lastevent) {
				GameEngine.EVENT_TSPIN_ZERO_MINI -> {
					receiver.drawMenuFont(engine, playerID, -2, 21, "MINI", color = COLOR.PURPLE)
					receiver.drawMenuFont(engine, playerID, 2, 21, "$strPieceName-SPIN", color = COLOR.PINK)
				}
				GameEngine.EVENT_TSPIN_ZERO -> receiver.drawMenuFont(engine, playerID, 2, 21, "$strPieceName-SPIN", color = COLOR.PINK)
				GameEngine.EVENT_SINGLE -> receiver.drawMenuFont(engine, playerID, 2, 21, "SINGLE", color = COLOR.COBALT)
				GameEngine.EVENT_TSPIN_SINGLE_MINI -> {
					receiver.drawMenuFont(engine, playerID, -2, 21, "MINI", color = if(lastb2b) COLOR.CYAN else COLOR.BLUE)
					receiver.drawMenuFont(engine, playerID, 2, 21, "$strPieceName-SPIN", color = if(lastb2b) COLOR.PINK else COLOR.PURPLE)
					receiver.drawMenuFont(engine, playerID, 2, 22, "SINGLE", color = COLOR.BLUE)
				}
				GameEngine.EVENT_TSPIN_SINGLE -> {
					receiver.drawMenuFont(engine, playerID, 2, 21, "$strPieceName-SPIN", color = if(lastb2b) COLOR.PINK else COLOR.PURPLE)
					receiver.drawMenuFont(engine, playerID, 2, 22, "SINGLE", color = COLOR.BLUE)
				}
				GameEngine.EVENT_DOUBLE -> receiver.drawMenuFont(engine, playerID, 2, 21, "DOUBLE", color = COLOR.BLUE)
				GameEngine.EVENT_TSPIN_DOUBLE_MINI -> {
					receiver.drawMenuFont(engine, playerID, -2, 21, "MINI", color = if(lastb2b) COLOR.GREEN else COLOR.CYAN)
					receiver.drawMenuFont(engine, playerID, 2, 20, "$strPieceName-SPIN", color = if(lastb2b) COLOR.PINK else COLOR.PURPLE)
					receiver.drawMenuFont(engine, playerID, 2, 22, "DOUBLE", color = COLOR.CYAN)
				}
				GameEngine.EVENT_TSPIN_DOUBLE -> {
					receiver.drawMenuFont(engine, playerID, 2, 20, "$strPieceName-SPIN", color = if(lastb2b) COLOR.PINK else COLOR.PURPLE)
					receiver.drawMenuFont(engine, playerID, 2, 22, "DOUBLE", color = COLOR.CYAN)
				}
				GameEngine.EVENT_SPLIT_DOUBLE -> receiver.drawMenuFont(engine, playerID, 0, 21, "SPLIT TWIN", color = COLOR.PURPLE)
				GameEngine.EVENT_TRIPLE -> receiver.drawMenuFont(engine, playerID, 2, 21, "TRIPLE", color = COLOR.GREEN)
				GameEngine.EVENT_SPLIT_TRIPLE -> receiver.drawMenuFont(engine, playerID, 0, 21, "1.2.TRIPLE", color = COLOR.CYAN)
				GameEngine.EVENT_TSPIN_TRIPLE -> {
					receiver.drawMenuFont(engine, playerID, 2, 20, "$strPieceName-SPIN", color = if(lastb2b) COLOR.YELLOW else COLOR.ORANGE)
					receiver.drawMenuFont(engine, playerID, 2, 22, "TRIPLE", color = COLOR.GREEN)
				}
				GameEngine.EVENT_QUADRUPLE -> receiver.drawMenuFont(engine, playerID, 1, 21, "QUADRUPLE", color = COLOR.ORANGE)
				GameEngine.EVENT_TSPIN_EZ -> receiver.drawMenuFont(engine, playerID, 0, 21, "EZ-"+strPieceName
					+"-SPIN", color = COLOR.ORANGE)
			}
		}
	}

	/** medal の文字色を取得
	 * @param medalColor medal 状態
	 * @return medal の文字色
	 */
	fun getMedalFontColor(medalColor:Int):EventReceiver.COLOR? = when(medalColor) {
		1 -> COLOR.RED
		2 -> COLOR.WHITE
		3 -> COLOR.YELLOW
		4 -> COLOR.CYAN
		else -> null
	}
	override fun renderLockFlash(engine:GameEngine, playerID:Int) {}

	override fun renderMove(engine:GameEngine, playerID:Int) {}

	override fun renderReady(engine:GameEngine, playerID:Int) {}

	override fun renderResult(engine:GameEngine, playerID:Int) {}

	override fun renderSetting(engine:GameEngine, playerID:Int) {
		//TODO: Custom page breaks
		var menuItem:AbstractMenuItem<*>
		val pageNum = menuCursor/10
		val pageStart = pageNum*10
		val endPage = minOf(menu.size, pageStart+10)
		for(i in pageStart until endPage) {
			menuItem = menu[i]
			receiver.drawMenuFont(engine, playerID, 0, i shl 1, menuItem.displayName, menuItem.color)
			if(menuCursor==i&&!engine.owner.replayMode)
				receiver.drawMenuFont(engine, playerID, 0, (i shl 1)+1,
					"b"+menuItem.valueString, true)
			else
				receiver.drawMenuFont(engine, playerID, 1, (i shl 1)+1, menuItem.valueString)
		}
	}

	override fun renderFieldEdit(engine:GameEngine, playerID:Int) = Unit

	override fun saveReplay(engine:GameEngine, playerID:Int, prop:CustomProperties) = Unit

	override fun startGame(engine:GameEngine, playerID:Int) = Unit

	override fun netplayInit(obj:NetLobbyFrame) = Unit

	override fun netplayUnload(obj:NetLobbyFrame) = Unit

	override fun netplayOnRetryKey(engine:GameEngine, playerID:Int) = Unit

	/** Update menu cursor
	 * @param engine GameEngine
	 * @param maxCursor Max value of cursor position
	 * @return -1 if Left key is pressed, 1 if Right key is pressed, 0
	 * otherwise
	 */
	protected fun updateCursor(engine:GameEngine, maxCursor:Int):Int = updateCursor(engine, maxCursor, 0)

	/** Update menu cursor
	 * @param engine GameEngine
	 * @param maxCursor Max value of cursor position
	 * @param playerID Player ID (unused)
	 * @return -1 if Left key is pressed, 1 if Right key is pressed,
	 * 0 otherwise
	 */
	protected open fun updateCursor(engine:GameEngine, maxCursor:Int, playerID:Int):Int {
		// Up
		if(engine.ctrl!!.isMenuRepeatKey(Controller.BUTTON_UP)) {
			menuCursor--
			if(menuCursor<0) menuCursor = maxCursor
			engine.playSE("cursor")
		}
		// Down
		if(engine.ctrl!!.isMenuRepeatKey(Controller.BUTTON_DOWN)) {
			menuCursor++
			if(menuCursor>maxCursor) menuCursor = 0
			engine.playSE("cursor")
		}

		// Configuration changes
		if(engine.ctrl!!.isMenuRepeatKey(Controller.BUTTON_LEFT)) return -1
		return if(engine.ctrl!!.isMenuRepeatKey(Controller.BUTTON_RIGHT)) 1 else 0
	}

	protected fun updateMenu(engine:GameEngine) {
		// Configuration changes
		val change = updateCursor(engine, menu.size-1)

		if(change!=0) {
			engine.playSE("change")
			var fast = 0
			if(engine.ctrl!!.isPush(Controller.BUTTON_E)) fast++
			if(engine.ctrl!!.isPush(Controller.BUTTON_F)) fast += 2
			menu[menuCursor].change(change, fast)
		}
	}

	protected fun initMenu(y:Int, color:COLOR, statc:Int) {
		menuY = y
		menuColor = color
		statcMenu = statc
	}

	protected fun initMenu(color:COLOR, statc:Int) {
		menuY = 0
		statcMenu = statc
		menuColor = color
	}

	protected fun drawMenu(engine:GameEngine, playerID:Int, receiver:EventReceiver, vararg str:String) {
		for(i in str.indices) {
			if(i and 1==0)
				receiver.drawMenuFont(engine, playerID, 0, menuY, str[i], color = menuColor)
			else {
				if(menuCursor==statcMenu&&!engine.owner.replayMode)
					receiver.drawMenuFont(engine, playerID, 0, menuY, "b"+str[i], true)
				else
					receiver.drawMenuFont(engine, playerID, 1, menuY, str[i])
				statcMenu++
			}
			menuY++
		}
	}

	protected fun drawMenuBGM(engine:GameEngine, playerID:Int, receiver:EventReceiver, bgmno:Int) {
		val cur = menuCursor==statcMenu&&!engine.owner.replayMode
		receiver.drawMenuFont(engine, playerID, 0, menuY, "BGM", color = menuColor)
		receiver.drawMenuFont(engine, playerID, 0, menuY+1, BGM.values[bgmno].drawName, cur)
		receiver.drawMenuNum(engine, playerID, 8, menuY, String.format("%2d", bgmno), cur)
		if(cur) receiver.drawMenuFont(engine, playerID, 7, menuY, "b", true)
		statcMenu++
		menuY += 2

	}

	protected fun drawMenu(engine:GameEngine, playerID:Int, receiver:EventReceiver,
		y:Int, color:COLOR, statc:Int, vararg str:String) {
		menuY = y
		menuColor = color
		statcMenu = statc
		drawMenu(engine, playerID, receiver, *str)
	}

	protected fun drawMenuCompact(engine:GameEngine, playerID:Int, receiver:EventReceiver, vararg str:String) {
		var i = 0
		while(i<str.size-1) {
			receiver.drawMenuFont(engine, playerID, 1, menuY, str[i]+":", color = menuColor)
			if(menuCursor==statcMenu&&!engine.owner.replayMode) {
				receiver.drawMenuFont(engine, playerID, 0, menuY, "b", true)
				receiver.drawMenuFont(engine, playerID, str[i].length+2, menuY, str[i+1], true)
			} else
				receiver.drawMenuFont(engine, playerID, str[i].length+2, menuY, str[i+1])
			statcMenu++
			menuY++
			i += 2
		}
	}

	protected fun drawMenuCompact(engine:GameEngine, playerID:Int, receiver:EventReceiver,
		y:Int, color:COLOR, statc:Int, vararg str:String) {
		menuY = y
		menuColor = color
		statcMenu = statc
		drawMenuCompact(engine, playerID, receiver, *str)
	}

	protected fun drawGravity(engine:GameEngine, playerID:Int, receiver:EventReceiver, g:Int, d:Int) {
		receiver.drawMenuFont(engine, playerID, 0, menuY, "SPEED", color = menuColor)
		receiver.drawSpeedMeter(engine, playerID, -12, menuY+1, g, d)
		for(i in 0..1) {
			if(menuCursor==statcMenu&&!engine.owner.replayMode) {
				receiver.drawMenuFont(engine, playerID, 5, menuY, "b", true)
				receiver.drawMenuNum(engine, playerID, 6, menuY, String.format("%5d", if(i==0) g else d), true)
			} else
				receiver.drawMenuNum(engine, playerID, 6, menuY, String.format("%5d", if(i==0) g else d))
			statcMenu++
			menuY++
		}
	}

	protected fun drawGravity(engine:GameEngine, playerID:Int, receiver:EventReceiver, y:Int, color:COLOR, statc:Int, g:Int, d:Int) {
		menuY = y
		menuColor = color
		statcMenu = statc
		drawGravity(engine, playerID, receiver, g, d)
	}

	protected fun drawMenuSpeeds(engine:GameEngine, playerID:Int, receiver:EventReceiver, y:Int, color:COLOR, statc:Int,
		g:Int, d:Int, are:Int, aline:Int, lined:Int, lock:Int, das:Int) {
		menuY = y
		menuColor = color
		statcMenu = statc
		drawMenuSpeeds(engine, playerID, receiver, g, d, are, aline, lined, lock, das)
	}

	protected fun drawMenuSpeeds(engine:GameEngine, playerID:Int, receiver:EventReceiver,
		g:Int, d:Int, are:Int, aline:Int, lined:Int, lock:Int, das:Int) {
		drawGravity(engine, playerID, receiver, g, d)

		var wait = "ARE"
		for(i in 0..1) {
			val cur = menuCursor==statcMenu&&!engine.owner.replayMode
			if(cur) {
				receiver.drawMenuFont(engine, playerID, 3+i*3, menuY, "b", true)
				if(i==1) wait = "LINE"
			}
			receiver.drawMenuNum(engine, playerID, 4+i*3, menuY, String.format(if(i==0) "%2d/" else "%2d", if(i==0) are else aline), cur)
			statcMenu++
		}
		menuY++
		for(i in 0..2) {
			val cur = menuCursor==statcMenu&&!engine.owner.replayMode
			if(cur) {
				wait = if(i==0) "LINE" else if(i==1) "LOCK" else "DAS"
				receiver.drawMenuFont(engine, playerID, 7-i*3, menuY, "b", true)
			}
			receiver.drawMenuNum(engine, playerID, 8-i*3, menuY, String.format(if(i==1) "%2d+" else "%2d", if(i==0) lined else if(i==1) lock else das), cur)
			statcMenu++
		}
		receiver.drawMenuFont(engine, playerID, 0, menuY-1, wait, color = menuColor)
		menuY++
	}

	protected fun drawResult(engine:GameEngine, playerID:Int, receiver:EventReceiver, y:Int, color:COLOR, str:String,
		num:Int) {
		receiver.drawMenuFont(engine, playerID, 0, y, str, color = color)
		receiver.drawMenuNum(engine, playerID, 0, y+1, String.format("%13d", num), color = COLOR.WHITE)
	}

	protected fun drawResult(engine:GameEngine, playerID:Int, receiver:EventReceiver, y:Int, color:COLOR, vararg str:String) {
		drawResultScale(engine, playerID, receiver, y, color, 1f, *str)
	}

	protected fun drawResultScale(engine:GameEngine, playerID:Int, receiver:EventReceiver, y:Int,
		color:COLOR = COLOR.WHITE, scale:Float,
		vararg str:String) {
		for(i in str.indices)
			receiver.drawMenuFont(engine, playerID, 0, y+i, str[i],
				if(i and 1==0) color else COLOR.WHITE, scale)
	}

	protected fun drawResultRank(engine:GameEngine, playerID:Int, receiver:EventReceiver, y:Int, color:COLOR,
		scale:Float, rank:Int, str:String) {
		if(rank!=-1) {
			var postfix = when {
				rank%10==0&&rank%100!=10 -> "ST"
				rank%10==1&&rank%100!=11 -> "ND"
				rank%10==2&&rank%100!=12 -> "RD"
				else -> "TH"
			}
			receiver.drawMenuFont(engine, playerID, 5, y, postfix, color, scale)
			receiver.drawMenuFont(engine, playerID, 5, y+1, str, color, scale*.8f)
			receiver.drawMenuNum(engine, playerID, 0, y, String.format("%3d", rank+1), scale = scale*2)
		} else {
			receiver.drawMenuFont(engine, playerID, 0, y, "OUT OF THE", color, scale)
			receiver.drawMenuFont(engine, playerID, 0, y+1, String.format("%12s", str), color, scale*.8f)
		}
	}

	protected fun drawResultRank(engine:GameEngine, playerID:Int, receiver:EventReceiver, y:Int, color:COLOR, rank:Int) {
		drawResultRankScale(engine, playerID, receiver, y, color, 1f, rank)
	}

	protected fun drawResultRankScale(engine:GameEngine, playerID:Int, receiver:EventReceiver, y:Int, color:COLOR,
		scale:Float, rank:Int) {
		drawResultRank(engine, playerID, receiver, y, color, scale, rank, "RANK")
	}

	protected fun drawResultNetRank(engine:GameEngine, playerID:Int, receiver:EventReceiver, y:Int, color:COLOR, rank:Int) {
		drawResultNetRankScale(engine, playerID, receiver, y, color, 1f, rank)
	}

	protected fun drawResultNetRankScale(engine:GameEngine, playerID:Int, receiver:EventReceiver, y:Int, color:COLOR,
		scale:Float, rank:Int) {
		drawResultRank(engine, playerID, receiver, y, color, scale, rank, "ONLINE")
	}

	protected fun drawResultNetRankDaily(engine:GameEngine, playerID:Int, receiver:EventReceiver, y:Int, color:COLOR,
		rank:Int) {
		drawResultNetRankDailyScale(engine, playerID, receiver, y, color, 1f, rank)
	}

	protected fun drawResultNetRankDailyScale(engine:GameEngine, playerID:Int, receiver:EventReceiver, y:Int, color:COLOR,
		scale:Float, rank:Int) {
		drawResultRank(engine, playerID, receiver, y, color, scale, rank, "OF DAILY")
	}

	protected fun drawResultStats(engine:GameEngine, playerID:Int, receiver:EventReceiver, y:Int, color:COLOR,
		vararg stats:Statistic) {
		drawResultStatsScale(engine, playerID, receiver, y, color, 1f, *stats)
	}

	protected fun drawResultStatsScale(engine:GameEngine, playerID:Int, receiver:EventReceiver, y:Int, color:COLOR,
		scale:Float, vararg stats:Statistic) {
		var y = y
		for(stat in stats) {
			when(stat) {
				AbstractMode.Statistic.SCORE -> {
					receiver.drawMenuFont(engine, playerID, 0, y, "SCORE", color = color, scale = scale*.75f)
					receiver.drawMenuNum(engine, playerID, 0, y, String.format("%7d", engine.statistics.score), scale = scale*1.9f)
				}
				AbstractMode.Statistic.TIME -> {
					receiver.drawMenuFont(engine, playerID, 0, y, "TIME", color = color, scale = scale*.8f)
					receiver.drawMenuNum(engine, playerID, 0, y, String.format("%8s", GeneralUtil.getTime(engine.statistics.time.toFloat())),
						scale = scale*1.7f)
				}
				AbstractMode.Statistic.LEVEL -> {
					receiver.drawMenuFont(engine, playerID, 0, y+1, "LEVEL", color, scale)
					receiver.drawMenuNum(engine, playerID, 5, y, String.format("%03d", engine.statistics.level+1), scale = scale*2f)
				}
				AbstractMode.Statistic.LEVEL_MANIA -> {
					receiver.drawMenuFont(engine, playerID, 0, y+1, "LEVEL", color, scale)
					receiver.drawMenuNum(engine, playerID, 5, y, String.format("%03d", engine.statistics.level), scale = scale*2f)
				}
				AbstractMode.Statistic.LINES -> {
					receiver.drawMenuFont(engine, playerID, 6, y+1, "LINES", color, scale*.8f)
					receiver.drawMenuNum(engine, playerID, 0, y, String.format("%4d", engine.statistics.lines), scale = scale*2f)
				}
				AbstractMode.Statistic.PIECE -> {
					receiver.drawMenuNum(engine, playerID, 0, y, String.format("%4d", engine.statistics.totalPieceLocked), scale = scale*2f)
					receiver.drawMenuFont(engine, playerID, 6, y+1, "PIECE", color, scale*.75f)
				}
				AbstractMode.Statistic.MAXCOMBO -> {
					receiver.drawMenuFont(engine, playerID, 0, y, "MAX COMBO", color, scale)
					receiver.drawMenuNum(engine, playerID, 0, y+1, String.format("%5d", engine.statistics.maxCombo-1), scale = scale)
				}
				AbstractMode.Statistic.MAXCHAIN -> {
					receiver.drawMenuFont(engine, playerID, 0, y, "MAX COMBO", color, scale)
					receiver.drawMenuNum(engine, playerID, 0, y+1, String.format("%5d", engine.statistics.maxChain-1), scale = scale)
				}
				AbstractMode.Statistic.SPL -> {
					receiver.drawMenuFont(engine, playerID, 0, y, "SCORE/LINE", color, scale)
					receiver.drawMenuNum(engine, playerID, 0, y+1, String.format("%13g", engine.statistics.spl), scale = scale)
				}
				AbstractMode.Statistic.SPM -> {
					receiver.drawMenuFont(engine, playerID, 0, y, "SCORE/MIN", color, scale)
					receiver.drawMenuNum(engine, playerID, 0, y+1, String.format("%13g", engine.statistics.spm), scale = scale)
				}
				AbstractMode.Statistic.SPS -> {
					receiver.drawMenuFont(engine, playerID, 0, y, "SCORE/SEC", color, scale)
					receiver.drawMenuNum(engine, playerID, 0, y+1, String.format("%13g", engine.statistics.sps), scale = scale)
				}
				AbstractMode.Statistic.LPM -> {
					receiver.drawMenuFont(engine, playerID, 0, y, "LINE/MIN", color, scale)
					receiver.drawMenuNum(engine, playerID, 0, y+1, String.format("%13g", engine.statistics.lpm), scale = scale)
				}
				AbstractMode.Statistic.LPS -> {
					receiver.drawMenuFont(engine, playerID, 0, y, "LINE/SEC", color, scale)
					receiver.drawMenuNum(engine, playerID, 0, y+1, String.format("%13g", engine.statistics.lps), scale = scale)
				}
				AbstractMode.Statistic.PPM -> {
					receiver.drawMenuFont(engine, playerID, 0, y, "PIECE/MIN", color, scale)
					receiver.drawMenuNum(engine, playerID, 0, y+1, String.format("%13g", engine.statistics.ppm), scale = scale)
				}
				AbstractMode.Statistic.PPS -> {
					receiver.drawMenuFont(engine, playerID, 0, y, "PIECE/SEC", color, scale)
					receiver.drawMenuNum(engine, playerID, 0, y+1, String.format("%13g", engine.statistics.pps), scale = scale)
				}
				AbstractMode.Statistic.LEVEL_ADD_DISP -> {
					receiver.drawMenuFont(engine, playerID, 0, y, "LEVEL", color, scale)
					receiver.drawMenuNum(engine, playerID, 0, y+1,
						String.format("%10d", engine.statistics.level+engine.statistics.levelDispAdd), scale = scale)
				}
			}
			y += 2
		}
	}

	/** Default method to render controller input display
	 * @param engine GameEngine
	 * @param playerID Player ID
	 */
	override fun renderInput(engine:GameEngine, playerID:Int) {
		val receiver = engine.owner.receiver
		var y = 24
		if(isVSMode&&!isNetplayMode) {
			var color = COLOR.BLUE
			if(playerID==0) {
				color = COLOR.RED
				y--
			}
			receiver.drawScoreFont(engine, 0, -9, y, (playerID+1).toString()+"P INPUT:", color)
		} else
			receiver.drawScoreFont(engine, 0, -6, y, "INPUT:", COLOR.BLUE)
		val ctrl = engine.ctrl
		if(ctrl!!.isPress(Controller.BUTTON_LEFT)) receiver.drawScoreFont(engine, 0, 0, y, "<")
		if(ctrl.isPress(Controller.BUTTON_DOWN)) receiver.drawScoreFont(engine, 0, 1, y, "n")
		if(ctrl.isPress(Controller.BUTTON_UP)) receiver.drawScoreFont(engine, 0, 2, y, "k")
		if(ctrl.isPress(Controller.BUTTON_RIGHT)) receiver.drawScoreFont(engine, 0, 3, y, ">")
		if(ctrl.isPress(Controller.BUTTON_A)) receiver.drawScoreFont(engine, 0, 4, y, "A")
		if(ctrl.isPress(Controller.BUTTON_B)) receiver.drawScoreFont(engine, 0, 5, y, "B")
		if(ctrl.isPress(Controller.BUTTON_C)) receiver.drawScoreFont(engine, 0, 6, y, "C")
		if(ctrl.isPress(Controller.BUTTON_D)) receiver.drawScoreFont(engine, 0, 7, y, "D")
		if(ctrl.isPress(Controller.BUTTON_E)) receiver.drawScoreFont(engine, 0, 8, y, "E")
		if(ctrl.isPress(Controller.BUTTON_F)) receiver.drawScoreFont(engine, 0, 9, y, "F")
	}
}