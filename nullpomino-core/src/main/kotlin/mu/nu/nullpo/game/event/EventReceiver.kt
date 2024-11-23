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
package mu.nu.nullpo.game.event

import mu.nu.nullpo.game.component.Block
import mu.nu.nullpo.game.component.Controller
import mu.nu.nullpo.game.component.Piece
import mu.nu.nullpo.game.component.SpeedParam.Companion.spdRank
import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.game.play.GameManager
import mu.nu.nullpo.gui.common.BaseFont
import mu.nu.nullpo.gui.common.BaseFont.FONT
import mu.nu.nullpo.gui.common.BaseStaffRoll
import mu.nu.nullpo.gui.common.ConfigGlobal.VisualConf
import mu.nu.nullpo.gui.common.fx.Effect
import mu.nu.nullpo.gui.common.fx.PopupCombo
import mu.nu.nullpo.util.CustomProperties
import mu.nu.nullpo.util.GeneralUtil.toInt
import mu.nu.nullpo.util.GeneralUtil.toReplayFilename
import org.apache.logging.log4j.LogManager
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import kotlin.math.absoluteValue
import kotlin.math.sign
import kotlin.random.Random

/** Drawing and event handling EventReceiver */
open class EventReceiver {
	var conf = VisualConf(); protected set
	val heavyEffect get() = conf.heavyEffect
	protected val random = Random.Default

	/** Font cint constants */
	enum class COLOR {
		WHITE, BLUE, RED, PINK, GREEN, YELLOW, CYAN, ORANGE, PURPLE, COBALT, RAINBOW;

		companion object {
			val all = entries
		}
	}

	/** Get type of piece preview
	 * @return 0=Above 1=Side Small 2=Side Big
	 */
	var nextDisplayType:Int = 0; protected set

	/** Piece previews on sides */
	protected val sideNext get() = nextDisplayType>0

	/** Use bigger side previews */
	protected val bigSideNext get() = nextDisplayType>1

	/** @return True if you can use TTF font routines */
	open val isTTFSupport get() = false

	/** Get Number of Block Skins */
	open val skinMax get() = 0

	/** @return X position of field */
	fun fieldX(e:GameEngine, pos:Number = 0) = e.fX+pos.toFloat()*BS
	/** @return X position of field */
	val GameEngine.fX
		get() = if(owner.menuOnly) 0f else (owner.mode?.let {m ->
			frameX+fieldXOffset+if(nextDisplayType==2) NEW_FIELD_OFFSET_X_BSP[m.gameStyle.ordinal][displaySize+1].let {
				it[minOf(it.lastIndex, playerID)]
			}
			else NEW_FIELD_OFFSET_X[m.gameStyle.ordinal][displaySize+1].let {
				it[minOf(it.lastIndex, playerID)]+if(nextDisplayType==1) blockSize else 0
			}
		}) ?: 0f

	/** @return Left Margin of field for Centering w/width<10 */
	val GameEngine.fieldXOffset get() = maxOf(0, blockSize*(10-field.width)/2)

	/** @return Y position of field*/
	fun fieldY(e:GameEngine, pos:Number = 0) = e.fY+pos.toFloat()*BS
	/** @return Y position of field*/
	val GameEngine.fY
		get() = if(owner.menuOnly) 0f else (owner.mode?.let {m ->
			frameY+if(nextDisplayType==2) NEW_FIELD_OFFSET_Y_BSP[m.gameStyle.ordinal][displaySize+1].let {
				it[minOf(it.lastIndex, playerID)]
			}
			else NEW_FIELD_OFFSET_Y[m.gameStyle.ordinal][displaySize+1].let {it[minOf(it.lastIndex, playerID)]}+
				if(nextDisplayType==0) nextHeight else 0
		}) ?: 0f

	/** @return X position of score display area*/
	fun scoreX(e:GameEngine, pos:Int = 0) = e.sX+pos*BS
	/** @return X position of score display area*/
	val GameEngine.sX
		get() = (if(owner.menuOnly) 0 else
			owner.mode?.let {m ->
				fieldXOffset+if(nextDisplayType==2) NEW_FIELD_OFFSET_X_BSP[m.gameStyle.ordinal][displaySize+1].let {
					it[minOf(it.lastIndex, playerID)]
				}
				else NEW_FIELD_OFFSET_X[m.gameStyle.ordinal][displaySize+1].let {it[minOf(it.lastIndex, playerID)]}
			} ?: 0)+fieldXOffset+blockSize*(field.width+3+nextDisplayType)

	/** @return Y position of score display area */
//	@Deprecated("toExtend", ReplaceWith("e.sY"))
	fun scoreY(e:GameEngine, pos:Int = 0) = e.sY+pos*BS
	/** @return Y position of score display area */
	val GameEngine.sY
		get() = if(owner.menuOnly) 0 else owner.mode?.let {m ->
			if(nextDisplayType==2) NEW_FIELD_OFFSET_Y_BSP[m.gameStyle.ordinal].let {it[minOf(it.lastIndex, displaySize+1)]}
				.let {it[minOf(it.lastIndex, playerID)]}
			else NEW_FIELD_OFFSET_Y[m.gameStyle.ordinal].let {it[minOf(it.lastIndex, displaySize+1)]}.let {
				it[minOf(
					it.lastIndex,
					playerID
				)]
			}
		} ?: 0

	/** It will be called when a line is cleared.*/
	open fun lineClear(engine:GameEngine, y:Collection<Int>) {}

	/** It will be called when score gained.
	 * @param pts Number of points last gained*/
	open fun addScore(x:Int, y:Int, pts:Int, color:COLOR) {}

	/** After Score Gained, to show points.
	 * @param engine GameEngine
	 * @param pts Number of points last gained
	 */
	fun addScore(engine:GameEngine, x:Int, y:Int, pts:Int, color:COLOR = getPlayerColor(engine.playerID)) =
		addScore(engine.fX.toInt()+x*engine.blockSize, engine.fY.toInt()+y*engine.blockSize, pts, color)

	open fun addCombo(x:Int, y:Int, pts:Int, type:PopupCombo.CHAIN, ex:Int) {}

	/** After Score Gained, to show combo.
	 * @param engine GameEngine
	 * @param pts Number of points last gained
	 */
	fun addCombo(engine:GameEngine, x:Int, y:Int, pts:Int, type:PopupCombo.CHAIN) = addCombo(
		engine.fX.toInt()+x*engine.blockSize, engine.fY.toInt()+y*engine.blockSize, pts, type, -(x+2)*engine.blockSize+18
	)

	/** It will be called when a fireworks shoot.
	 * @param engine GameEngine
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param color Color
	 */
	open fun shootFireworks(engine:GameEngine, x:Float, y:Float, color:COLOR) {
		playSE("firecracker"+(random.nextInt(2)+1))
	}

	/** It will be called when a fireworks shoot
	 * @param engine GameEngine
	 */
	fun shootFireworks(engine:GameEngine) {
		val mx = engine.fieldWidth*engine.blockSize
		val my = engine.fieldHeight*8
		val x = engine.fX+random.nextInt(mx)
		val y = (engine.fY+random.nextInt(my)+50).let {
			if(my<engine.field.highestBlockY) it+my else it
		}
		shootFireworks(engine, x, y, COLOR.all[random.nextInt(7)])
	}

	open fun bravo(engine:GameEngine) {
		playSE("bravo")
	}

	/** Draw String to any location.
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param str String to draw
	 * @param color Font cint
	 * @param scale Font size (.5f, 1f, 2f)
	 */
	open fun drawFont(x:Float, y:Float, str:String, font:FONT = FONT.NORMAL, color:COLOR = COLOR.WHITE, scale:Float = 1f, alpha:Float = 1f) {
	}

	fun drawFont(x:Number, y:Number, str:String, font:FONT = FONT.NORMAL, color:COLOR = COLOR.WHITE, scale:Float = 1f, alpha:Float = 1f) =
		drawFont(x.toFloat(), y.toFloat(), str, font, color, scale, alpha)

	fun drawDirectFont(x:Number, y:Number, str:String, color:COLOR = COLOR.WHITE, scale:Float = 1f, alpha:Float = 1f) =
		drawFont(x, y, str, FONT.NORMAL, color, scale, alpha)

	/** [You don't have to override this]
	 * Draw String to any location.
	 * If flag is false, it will use white font cint. If flag is true, it will
	 * use red instead.
	 * @param playerID Player ID
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param str String to draw
	 * @param flag Any boolean variable
	 */
	fun drawDirectFont(playerID:Int, x:Number, y:Number, str:String, flag:Boolean, scale:Float = 1f, alpha:Float = 1f) =
		drawDirectFont(x, y, str, if(flag) getPlayerColor(playerID) else COLOR.WHITE, scale, alpha)

	fun drawDirectNano(x:Number, y:Number, str:String, color:COLOR = COLOR.WHITE, scale:Float = 1f, alpha:Float = 1f) =
		drawFont(x, y, str, FONT.NANO, color, scale, alpha)

	/** [You don't have to override this]
	 * Draw String to any location.
	 * If flag is false, it will use white font cint. If flag is true, it will
	 * use red instead.
	 * @param playerID Player ID
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param str String to draw
	 * @param flag Any boolean variable
	 */
	fun drawDirectNano(playerID:Int, x:Number, y:Number, str:String, flag:Boolean, scale:Float = 1f, alpha:Float = 1f) =
		drawDirectNano(x, y, str, if(flag) getPlayerColor(playerID) else COLOR.WHITE, scale, alpha)

	fun drawDirectNum(x:Float, y:Float, num:Number, precise:Pair<Int?, Int?> = null to null,
		color:COLOR = COLOR.WHITE, scale:Float = 1f, alpha:Float = 1f) =
		"%${precise.first ?: ""}${precise.second?.let {".$it"} ?: ""}f".format(num).split('.').let {
			val f = "${it[0]}."
			drawFont(x, y, f, FONT.NUM, color, scale, alpha)
			if(it.size>1) {
				val ns = scale/2
				val isBig = FONT.NANO.h*ns>=FONT.NUM.h*.75f
				drawFont(
					x+f.length*FONT.NUM.w*scale, y+FONT.NUM.h*(scale-ns),
					it[1], if(isBig) FONT.NUM else FONT.NANO, color, if(isBig) ns else ns*FONT.NUM.h/FONT.NANO.h, alpha
				)
			}
		}

	fun drawDirectNum(x:Number, y:Number, num:Number, precise:Pair<Int?, Int?> = null to null,
		color:COLOR = COLOR.WHITE, scale:Float = 1f, alpha:Float = 1f) =
		drawDirectNum(x.toFloat(), y.toFloat(), num, precise, color, scale, alpha)

	/** [You don't have to override this]
	 * Draw Number to any location.
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param str String to draw
	 * @param color Font cint
	 * @param scale Font size (.5f, 1f, 2f)
	 */
	fun drawDirectNum(x:Number, y:Number, str:String, color:COLOR = COLOR.WHITE, scale:Float = 1f, alpha:Float = 1f) =
		drawFont(x, y, str, FONT.NUM, color, scale, alpha)

	/** [You don't have to override this]
	 * Draw Grade to any location.
	 * If flag is false, it will use white font cint.
	 * If flag is true, it will use red or blue instead.
	 * @param playerID Player ID
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param str String to draw
	 * @param flag Any boolean variable
	 */
	fun drawDirectNum(playerID:Int, x:Number, y:Number, str:String, flag:Boolean, scale:Float = 1f, alpha:Float = 1f) =
		drawDirectNum(x, y, str, if(flag) getPlayerColor(playerID) else COLOR.WHITE, scale, alpha)

	/** [You don't have to override this]
	 * Draw Grade to any location.
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param str String to draw
	 * @param color Font cint
	 * @param scale Font size (.5f, 1f, 2f)
	 */
	fun drawDirectGrade(x:Number, y:Number, str:String, color:COLOR = COLOR.WHITE, scale:Float = 1f, alpha:Float = 1f) =
		drawFont(x, y, str, FONT.GRADE, color, scale, alpha)

	/** [You don't have to override this]
	 * Draw Grade to any location.
	 * If flag is false, it will use white font cint.
	 * If flag is true, it will use red or blue instead.
	 * @param playerID Player ID
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param str String to draw
	 * @param flag Any boolean variable
	 */
	fun drawDirectGrade(playerID:Int, x:Number, y:Number, str:String, flag:Boolean, scale:Float = 1f, alpha:Float = 1f) =
		drawDirectGrade(x, y, str, if(flag) getPlayerColor(playerID) else COLOR.WHITE, scale, alpha)

	/** [YOu don't have to override this]
	 * Draw TTF String to any location.
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param str String to draw
	 * @param color Font cint
	 */
	fun drawDirectTTF(x:Number, y:Number, str:String, color:COLOR = COLOR.WHITE) =
		drawFont(x, y, str, FONT.TTF, color)

	/** [You don't have to override this]
	 * Draw String to any location.
	 * If flag is false, it will use white font cint. If flag is true, it will
	 * use red instead.
	 * @param playerID Player ID
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param str String to draw
	 * @param flag Any boolean variable
	 */
	fun drawDirectTTF(playerID:Int, x:Number, y:Number, str:String, flag:Boolean) =
		drawDirectTTF(x, y, str, if(flag) getPlayerColor(playerID) else COLOR.WHITE)

	open fun drawStaffRoll(x:Number, y:Number, scr:Number, height:Number, alpha:Float = 1f) {}
	fun drawStaffRoll(engine:GameEngine, scr:Float, height:Number = if(!engine.owner.menuOnly) engine.fieldHeight*BS else 480f, alpha:Float = 1f) =
		(if(!engine.owner.menuOnly) (engine.fX to engine.fY) else (320f to 0f)).let {
			val _h = height.toFloat()
			drawStaffRoll(
				it.first, it.second, scr*(_h+BaseStaffRoll.height+256)-_h, _h, alpha
			)
		}

	private fun menuPos(engine:GameEngine, x:Float, y:Float, str:String, font:FONT, scale:Float):Pair<Float, Float> {
		var sx = if(x<-2) engine.fieldWidth*BS/2-str.length*font.w*scale/2 else BS*x
		var sy = y*BS
		if(!engine.owner.menuOnly) {
			sx += engine.fX
			sy += engine.fY
		}
		return sx to sy
	}

	private fun menuPos(engine:GameEngine, x:Number, y:Number, str:String, font:FONT, scale:Float) =
		menuPos(engine, x.toFloat(), y.toFloat(), str, font, scale)
	/** Draw String inside the field.
	 * @param engine GameEngine
	 * @param x X-coordinate
	 * @param y Y-coordinate if x < -2 Center Align
	 * @param str String to draw
	 * @param font Fomt specifies
	 * @param color Font cint
	 * @param scale Font size (.5f, 1f, 2f)
	 */
	fun drawMenu(engine:GameEngine, x:Number, y:Number, str:String, font:FONT = FONT.NORMAL, color:COLOR = COLOR.WHITE, scale:Float = 1f, alpha:Float = 1f) =
		menuPos(engine, x, y, str, font, scale).let {(sx, sy) ->
			drawFont(sx, sy, str, font, color, scale, alpha)
		}

	/** Draw String inside the field.*/
	fun drawMenuFont(engine:GameEngine, x:Number, y:Number, str:String, color:COLOR = COLOR.WHITE, scale:Float = 1f, alpha:Float = 1f) =
		drawMenu(engine, x, y, str, FONT.NORMAL, color, scale, alpha)

	fun drawMenuFont(engine:GameEngine, x:Number, y:Number, str:String, scale:Float = 1f, alpha:Float = 1f) =
		drawMenu(engine, x, y, str, FONT.NORMAL, scale = scale, alpha = alpha)

	/** [You don't have to override this]
	 * Draw String inside the field.
	 * If [flag] is false, it will use white font cint.
	 * If [flag] is true, it will use red or blue instead.*/
	fun drawMenuFont(engine:GameEngine, x:Number, y:Number, str:String, flag:Boolean, scale:Float = 1f) =
		drawMenuFont(engine, x, y, str, color = if(flag) getPlayerColor(engine.playerID) else COLOR.WHITE, scale = scale)

	/** [You don't have to override this]
	 * Draw Number inside the field.*/
	fun drawMenuNum(engine:GameEngine, x:Number, y:Number, str:String, color:COLOR = COLOR.WHITE, scale:Float = 1f, alpha:Float = 1f) =
		drawMenu(engine, x, y, str, FONT.NUM, color, scale, alpha)

	/** [You don't have to override this]
	 * Draw Number inside the field.*/
	fun drawMenuNum(engine:GameEngine, x:Number, y:Number, str:String, scale:Float = 1f, alpha:Float = 1f) =
		drawMenu(engine, x, y, str, FONT.NUM, scale = scale, alpha = alpha)

	/** [You don't have to override this]
	 * Draw Number inside the field.
	 * If [flag] is false, it will use white font cint.
	 * If [flag] is true, it will use red or blue instead.*/
	fun drawMenuNum(engine:GameEngine, x:Number, y:Number, str:String, flag:Boolean, scale:Float = 1f, alpha:Float = 1f) =
		drawMenuNum(engine, x, y, str, if(flag) getPlayerColor(engine.playerID) else COLOR.WHITE, scale, alpha)

	fun drawMenuNum(engine:GameEngine, x:Number, y:Number, num:Number, precise:Pair<Int?, Int?> = null to null,
		color:COLOR = COLOR.WHITE, scale:Float = 1f, alpha:Float = 1f) =
		menuPos(engine, x, y, "$num", FONT.NUM, scale).let {(sx, sy) ->
			drawDirectNum(sx, sy, num, precise, color, scale, alpha)
		}

	fun drawMenuNum(engine:GameEngine, x:Number, y:Number, num:Number, precise:Pair<Int?, Int?> = null to null,
		flag:Boolean, scale:Float = 1f, alpha:Float = 1f) =
		drawMenuNum(engine, x, y, num, precise, if(flag) COLOR.RAINBOW else COLOR.WHITE, scale, alpha)

	/** @see drawMenu*/
	fun drawMenuNano(engine:GameEngine, x:Number, y:Number, str:String, color:COLOR = COLOR.WHITE, scale:Float = 1f, alpha:Float = 1f) =
		drawMenu(engine, x, y, str, FONT.NANO, color, scale, alpha)

	/** [You don't have to override this]
	 * Draw Small Font inside the field.
	 * If [flag] is false, it will use white font cint.
	 * If [flag] is true, it will use red or blue instead.*/
	fun drawMenuNano(engine:GameEngine, x:Number, y:Number, str:String, flag:Boolean, scale:Float = 1f, alpha:Float = 1f) =
		drawMenuNano(
			engine, x, y, str, if(flag) if(engine.playerID%2==0) COLOR.YELLOW else COLOR.ORANGE else COLOR.WHITE, scale, alpha
		)

	/** [You don't have to override this]
	 * Draw Grade inside the field.*/
	fun drawMenuGrade(engine:GameEngine, x:Number, y:Number, str:String, color:COLOR = COLOR.WHITE, scale:Float = 1f, alpha:Float = 1f) =
		drawMenu(engine, x, y, str, FONT.GRADE, color, scale, alpha)

	/** [You don't have to override this]
	 * Draw Grade inside the field.
	 * If [flag] is false, it will use white font cint.
	 * If [flag] is true, it will use red or blue instead.*/
	fun drawMenuGrade(engine:GameEngine, x:Number, y:Number, str:String, flag:Boolean, scale:Float = 1f, alpha:Float = 1f) =
		drawMenuGrade(engine, x, y, str, if(flag) getPlayerColor(engine.playerID) else COLOR.WHITE, scale, alpha)

	/** [You don't have to override this]
	 * Draw String inside the field.*/
	fun drawMenuTTF(engine:GameEngine, x:Number, y:Number, str:String, color:COLOR = COLOR.WHITE) =
		drawMenu(engine, x, y, str, font = FONT.TTF, color = color)

	/** [You don't have to override this]
	 * Draw String inside the field.
	 * If [flag] is false, it will use white font cint.
	 * If [flag] is true, it will use red or blue instead.*/
	fun drawMenuTTF(engine:GameEngine, x:Number, y:Number, str:String, flag:Boolean) =
		drawMenuTTF(engine, x, y, str, if(flag) getPlayerColor(engine.playerID) else COLOR.WHITE)

	/** Draw String to score display area.*/
	fun drawScore(engine:GameEngine, x:Float, y:Float, str:String, font:FONT = FONT.NORMAL, color:COLOR = COLOR.WHITE, scale:Float = 1f, alpha:Float = 1f) {
		if(!engine.owner.menuOnly) drawFont(engine.sX+x*BS, engine.sY+y*BS, str, font, color, scale, alpha)
	}

	fun drawScore(engine:GameEngine, x:Number, y:Number, str:String, font:FONT = FONT.NORMAL, color:COLOR = COLOR.WHITE, scale:Float = 1f, alpha:Float = 1f) =
		drawScore(engine, x.toFloat(), y.toFloat(), str, font, color, scale, alpha)

	/** [You don't have to override this]
	 *  Draw String to score display area. */
	fun drawScoreFont(engine:GameEngine, x:Number, y:Number, str:String, color:COLOR = COLOR.WHITE, scale:Float = 1f, alpha:Float = 1f) =
		drawScore(engine, x, y, str, FONT.NORMAL, color, scale, alpha)

	/** [You don't have to override this]
	 *  Draw String to score display area. */
	fun drawScoreFont(engine:GameEngine, x:Number, y:Number, str:String, scale:Float = 1f, alpha:Float = 1f) =
		drawScore(engine, x, y, str, FONT.NORMAL, scale = scale, alpha = alpha)

	/** [You don't have to override this]
	 * Draw String to score display area.
	 * If [flag] is false, it will use white font cint.
	 * If [flag] is true, it will use rainbow instead.*/
	fun drawScore(engine:GameEngine, x:Number, y:Number, str:String, font:FONT = FONT.NORMAL, flag:Boolean, scale:Float = 1f, alpha:Float = 1f) =
		drawScore(engine, x, y, str, font, if(flag) COLOR.RAINBOW else COLOR.WHITE, scale, alpha)

	fun drawScoreFont(engine:GameEngine, x:Number, y:Number, str:String, flag:Boolean, scale:Float = 1f, alpha:Float = 1f) =
		drawScore(engine, x, y, str, FONT.NORMAL, flag, scale, alpha)

	fun drawScoreNum(engine:GameEngine, x:Number, y:Number, str:String, color:COLOR = COLOR.WHITE, scale:Float = 1f, alpha:Float = 1f) =
		drawScore(engine, x, y, str, FONT.NUM, color, scale, alpha)

	fun drawScoreNum(engine:GameEngine, x:Number, y:Number, str:String, scale:Float = 1f, alpha:Float = 1f) =
		drawScore(engine, x, y, str, FONT.NUM, scale = scale, alpha = alpha)

	fun drawScoreNum(engine:GameEngine, x:Number, y:Number, str:String, flag:Boolean, scale:Float = 1f, alpha:Float = 1f) =
		drawScoreNum(engine, x, y, str, if(flag) COLOR.RAINBOW else COLOR.WHITE, scale, alpha)

	fun drawScoreNum(engine:GameEngine, x:Float, y:Float, num:Number, precise:Pair<Int?, Int?> = null to null,
		color:COLOR = COLOR.WHITE, scale:Float = 1f, alpha:Float = 1f) {
		if(!engine.owner.menuOnly) drawDirectNum(engine.sX+x*BS, engine.sY+y*BS, num, precise, color, scale, alpha)
	}

	fun drawScoreNum(engine:GameEngine, x:Number, y:Number, num:Number, precise:Pair<Int?, Int?> = null to null,
		color:COLOR = COLOR.WHITE, scale:Float = 1f, alpha:Float = 1f) =
		drawScoreNum(engine, x.toFloat(), y.toFloat(), num, precise, color, scale, alpha)

	fun drawScoreNum(engine:GameEngine, x:Number, y:Number, num:Number, precise:Pair<Int?, Int?> = null to null,
		flag:Boolean, scale:Float = 1f, alpha:Float = 1f) =
		drawScoreNum(engine, x, y, num, precise, if(flag) COLOR.RAINBOW else COLOR.WHITE, scale, alpha)

	fun drawScoreNano(engine:GameEngine, x:Number, y:Number, str:String, color:COLOR = COLOR.WHITE, scale:Float = 1f, alpha:Float = 1f) =
		drawScore(engine, x, y, str, FONT.NANO, color, scale, alpha)

	fun drawScoreNano(engine:GameEngine, x:Number, y:Number, str:String, flag:Boolean, scale:Float = 1f, alpha:Float = 1f) =
		drawScoreNano(engine, x, y, str, if(flag) COLOR.RAINBOW else COLOR.WHITE, scale, alpha)

	fun drawScoreGrade(engine:GameEngine, x:Number, y:Number, str:String, color:COLOR = COLOR.WHITE, scale:Float = 1f, alpha:Float = 1f) =
		drawScore(engine, x, y, str, FONT.GRADE, color, scale, alpha)

	fun drawScoreGrade(engine:GameEngine, x:Number, y:Number, str:String, flag:Boolean, scale:Float = 1f, alpha:Float = 1f) =
		drawScoreGrade(engine, x, y, str, if(flag) COLOR.RAINBOW else COLOR.WHITE, scale, alpha)

	fun drawScoreTTF(engine:GameEngine, x:Number, y:Number, str:String, color:COLOR = COLOR.WHITE) =
		drawScore(engine, x, y, str, FONT.TTF, color)

	fun drawScoreTTF(engine:GameEngine, x:Number, y:Number, str:String, flag:Boolean) =
		drawScoreTTF(engine, x, y, str, if(flag) getPlayerColor(engine.playerID) else COLOR.WHITE)

	/** Draw speed meter.
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param sp Speed (float:0.0~1.0)
	 * @param len Meter Width Grid
	 */
	open fun drawSpeedMeter(x:Float, y:Float, sp:Float, len:Float) {}

	/** Draw speed meter on Field Area.
	 * @param x X-coordinate grid
	 * @param y Y-coordinate grid
	 * @param sp Speed (float:0.0~1.0 int:0~40)
	 * @param len Meter Width Grid
	 */
	fun drawMenuSpeed(engine:GameEngine, x:Float, y:Float, sp:Float, len:Float = 3f) {
		var sx = x*BS+minOf(len.absoluteValue, BS/2f)*len.sign
		var sy = y*BS.toFloat()
		if(!engine.owner.menuOnly) {
			sx += engine.fX
			sy += engine.fY
		}
		drawSpeedMeter(sx, sy, sp, len)
	}

	/** Draw speed meter on Field Area.
	 * @param x X-coordinate grid
	 * @param y Y-coordinate grid
	 * @param sp Speed (float:0.0~1.0 int:0~40)
	 * @param len Meter Width Grid
	 */
	fun drawMenuSpeed(engine:GameEngine, x:Number, y:Number, sp:Float, len:Float = 3f) =
		drawMenuSpeed(engine, x.toFloat(), y.toFloat(), sp, len)

	/** Draw speed meter on Field.
	 * @param x X-coordinate grid
	 * @param y Y-coordinate grid
	 * @param g gravity Value
	 * @param d gravity Denominator
	 * @param len Meter Width Grid
	 */
	fun drawMenuSpeed(engine:GameEngine, x:Float, y:Float, g:Int, d:Int, len:Float = 3f) =
		drawMenuSpeed(engine, x, y, spdRank(g, d), len)

	/** Draw speed meter on Field.
	 * @param x X-coordinate grid
	 * @param y Y-coordinate grid
	 * @param g gravity Value
	 * @param d gravity Denominator
	 * @param len Meter Width Grid
	 */
	fun drawMenuSpeed(engine:GameEngine, x:Number, y:Number, g:Int, d:Int, len:Int = 3) =
		drawMenuSpeed(engine, x, y, spdRank(g, d), len.toFloat())

	/** Draw speed meter on Score Area.
	 * @param x X-coordinate grid
	 * @param y Y-coordinate grid
	 * @param sp Speed (float:0.0~1.0 int:0~40)
	 * @param len Meter Width Grid
	 */
	fun drawScoreSpeed(engine:GameEngine, x:Float, y:Float, sp:Float, len:Float = 3f) {
		val dx1 = engine.sX+x*BS+minOf(len.absoluteValue, BS/2f)*len.sign
		val dy1 = engine.sY+y*BS+BS/2f
		//if(engine.owner.menuOnly) return
		drawSpeedMeter(dx1, dy1, sp, len)
	}

	fun drawScoreSpeed(engine:GameEngine, x:Number, y:Number, sp:Float, len:Number = 3f) =
		drawScoreSpeed(engine, x.toFloat(), y.toFloat(), sp, len.toFloat())

	/** Draw speed meter on Score Area.
	 * @param x X-coordinate grid
	 * @param y Y-coordinate grid
	 * @param sp Speed (float:0.0~1.0 int:0~40)
	 * @param len Meter Width Grid
	 */
	fun drawScoreSpeed(engine:GameEngine, x:Number, y:Number, sp:Int, len:Number = 3) =
		drawScoreSpeed(engine, x, y, sp/40f, len)

	/** Draw speed meter on Score Area.
	 * @param x X-coordinate grid
	 * @param y Y-coordinate grid
	 * @param g gravity Value
	 * @param d gravity Denominator
	 */
	fun drawScoreSpeed(engine:GameEngine, x:Number, y:Number, g:Int, d:Int, len:Number = 3f) =
		drawScoreSpeed(engine, x, y, spdRank(g, d), len)

	/** Draw Decorations*/
	open fun drawBadges(x:Float, y:Float, width:Int = 0, nums:Int, scale:Float = 1f) {}
	fun drawBadges(x:Number, y:Number, width:Int = 0, nums:Int, scale:Float = 1f) =
		drawBadges(x.toFloat(), y.toFloat(), width, nums, scale)

	/** Draw Decorations at score pos*/
	fun drawScoreBadges(engine:GameEngine, x:Number, y:Number, width:Int, nums:Int, scale:Float = 1f) =
		drawBadges(engine.sX+x.toFloat()*BS, engine.sY+y.toFloat()*BS, width, nums, scale)

	/** Draw Decorations at in-field pos*/
	fun drawMenuBadges(engine:GameEngine, x:Number, y:Number, nums:Int, scale:Float = 1f) =
		drawBadges(x.toFloat()*BS+engine.fX, y.toFloat()*BS+engine.fY, BS*10, nums, scale)

	/** Draw Medal
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param tier medal Tier
	 * @param scale size
	 */
	open fun drawMedal(x:Float, y:Float, str:String, tier:Int, scale:Float = 1f) {}
	fun drawMedal(x:Number, y:Number, str:String, tier:Int, scale:Float = 1f) =
		drawMedal(x.toFloat(), y.toFloat(), str, tier, scale)

	/** Draw Medal at score pos
	 * @param tier color
	 */
	fun drawScoreMedal(engine:GameEngine, x:Number, y:Number, str:String, tier:Int, scale:Float = 1f) =
		drawMedal(engine.sX+x.toFloat()*BS, engine.sY+y.toFloat()*BS, str, tier, scale)

	/** Draw Medal at menu pos
	 * @param tier color
	 */
	fun drawMenuMedal(engine:GameEngine, x:Number, y:Number, str:String, tier:Int, scale:Float = 1f) =
		drawMedal(x.toFloat()*BS+engine.fX, y.toFloat()*BS+engine.fY, str, tier, scale)

	/** Draw a block
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param color Block cint
	 * @param skin Block skin
	 * @param bone When true, it will use [] (bone) blocks
	 * @param darkness Brightness
	 * @param alpha Alpha-blending
	 * @param scale Size (.5f, 1f, 2f)
	 */
	@JvmOverloads
	open fun drawBlock(x:Float, y:Float, color:Int, skin:Int, bone:Boolean = false, darkness:Float = 0f, alpha:Float = 1f, scale:Float = 1f, attr:Int = 0, outline:Float = 0f) {
	}

	@JvmOverloads
	fun drawBlock(x:Number, y:Number, color:Int, skin:Int, bone:Boolean, darkness:Float, alpha:Float, scale:Float, attr:Int = 0, outline:Float = 0f) =
		drawBlock(x.toFloat(), y.toFloat(), color, skin, bone, darkness, alpha, scale, attr, outline)

	/** Blockクラスのインスタンスを使用してBlockを描画 (拡大率と暗さ指定可能）
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param block Blockクラスのインスタンス
	 * @param scale 拡大率
	 * @param darkness 暗さもしくは明るさ
	 */
	@JvmOverloads
	fun drawBlock(x:Number, y:Number, block:Block, darkness:Float = 0f, alpha:Float = 1f, scale:Float = 1f, outline:Float = 0f) =
		drawBlock(
			x, y, block.drawColor, block.skin, block.getAttribute(Block.ATTRIBUTE.BONE), block.darkness+darkness,
			block.alpha*alpha, scale, block.aint, outline
		)

	@JvmOverloads fun drawBlockForceVisible(x:Number, y:Number, blk:Block, scale:Float = 1f) = drawBlock(
		x,
		y,
		blk.drawColor,
		blk.skin,
		blk.getAttribute(Block.ATTRIBUTE.BONE),
		blk.darkness/2,
		.25f*blk.alpha+.25f,
		scale,
		blk.aint
	)

	/** Blockピースを描画 (暗さもしくは明るさの指定可能）
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param piece 描画するピース
	 * @param scale 拡大率
	 * @param darkness 暗さもしくは明るさ
	 */
	@JvmOverloads
	fun drawPiece(x:Float, y:Float, piece:Piece, scale:Float = 1f, darkness:Float = 0f, alpha:Float = 1f, ow:Float = 0f) =
		piece.block.forEachIndexed {i, blk ->
			val ls = scale*getBlockSize(piece.big.toInt())
			drawBlock(
				x+(piece.dataX[piece.direction][i].toFloat()*ls), y+(piece.dataY[piece.direction][i].toFloat()*ls),
				blk, darkness, alpha, scale, ow
			)
		}

	fun drawPiece(x:Number, y:Number, piece:Piece, scale:Float = 1f, darkness:Float = 0f, alpha:Float = 1f, ow:Float = 0f) =
		drawPiece(x.toFloat(), y.toFloat(), piece, scale, darkness, alpha, ow)

	/** Get key name by button ID
	 * @param playerID Player ID
	 * @param inGame When false, it will use Navigation Keymap
	 * @param btnID Button ID
	 * @return Key name
	 */
	open fun getKeyNameByButtonID(playerID:Int, inGame:Boolean, btnID:Int):String = ""

	/** Get key name by button ID
	 * @param engine GameEngine
	 * @param btnID Button ID
	 * @return Key name
	 */
	open fun getKeyNameByButtonID(engine:GameEngine, btnID:Int):String =
		getKeyNameByButtonID(engine.playerID, engine.isInGame, btnID)

	/** Check if the skin is sticky type
	 * @param skin Skin ID
	 * @return true if the skin is sticky type
	 */
	open fun isStickySkin(skin:Int):Boolean = false

	/** [You don't have to override this]
	 * Check if the current skin is sticky type
	 * @param engine GameEngine
	 * @return true if the current skin is sticky type
	 */
	fun isStickySkin(engine:GameEngine):Boolean = isStickySkin(engine.skin)

	/** Play sound effects
	 * @param name Name of SFX
	 */
	open fun playSE(name:String, freq:Float = 1f, vol:Float = 1f) {}
	open fun loopSE(name:String, freq:Float = 1f, vol:Float = 1f) {}
	open fun stopSE(name:String) {}

	/** Set Graphics context
	 * @param g Graphics context
	 */
	open fun setGraphics(g:Any?) {}

	/** Load CustomProperties from [filename].
	 * @param filename Filename
	 * @return Properties you specified, or null if the file doesn't exist.
	 */
	fun loadProperties(filename:String):CustomProperties? {
		val prop = CustomProperties()

		log.debug("load custom property file from $filename")
		try {
			val file = GZIPInputStream(FileInputStream(filename))
			prop.load(file)
			file.close()
		} catch(e:IOException) {
			log.debug("Failed to load custom property file from $filename", e)
			return null
		}

		return prop
	}

	/** Save [prop] to [filename].
	 * @return true if success
	 */
	@Deprecated("Use from CustomProperties", ReplaceWith("prop.save(filename)"))
	fun saveProperties(prop:CustomProperties, filename:String = prop.fileName):Boolean {
		try {
			val repFolder = File(filename).parentFile
			if(!repFolder.exists()) if(repFolder.mkdirs()) log.info("Created folder: ${repFolder.name}")
			else log.info("Couldn't create folder at ${repFolder.name}")
			val out = GZIPOutputStream(FileOutputStream(filename))
			prop.store(out, "NullpoMino Custom Property File")
			log.debug("Saving custom property file to $filename")
			out.close()
		} catch(e:IOException) {
			log.debug("Failed to save custom property file to $filename", e)
			return false
		}

		return true
	}

	/** It will be called before game screen appears.
	 * @param manager GameManager that owns this mode
	 */
	open fun modeInit(manager:GameManager) {
		setBGSpd(manager, 1f)
	}

	/**change BGA Speed multiplier to [spd]
	 * @param spd Recommended : 0f-2f*/
	open fun setBGSpd(owner:GameManager, spd:Number, id:Int? = null) {}

	/** It will be called at the end of initialization for each player.*/
	open fun playerInit(engine:GameEngine) {}

	/** It will be called when Ready->Go is about to end, before first piece appears.*/
	open fun startGame(engine:GameEngine) {}

	/** It will be called at the start of each frame.*/
	open fun onFirst(engine:GameEngine) {}

	/** It will be called at the end of each frame.*/
	open fun onLast(engine:GameEngine) {}

	/** It will be called at the settings screen.*/
	open fun onSetting(engine:GameEngine) {}

	/** It will be called at the settings screen.*/
	open fun onProfile(engine:GameEngine) {}

	/** It will be called during the "Ready->Go" screen.*/
	open fun onReady(engine:GameEngine) {}

	/** It will be called during the piece movement.*/
	open fun onMove(engine:GameEngine) {}

	/** It will be called during the "Lock flash".*/
	open fun onLockFlash(engine:GameEngine) {}

	/** It will be called during the line clear.*/
	open fun onLineClear(engine:GameEngine) {}

	/** It will be called during the "ARE".*/
	open fun onARE(engine:GameEngine) {}

	/** It will be called during the "Ending start" screen.*/
	open fun onEndingStart(engine:GameEngine) {}

	/** It will be called during the "Custom" screen.*/
	open fun onCustom(engine:GameEngine) {}

	/** It will be called during the "EXCELLENT!" screen.*/
	open fun onExcellent(engine:GameEngine) {}

	/** It will be called during the Game Over screen.*/
	open fun onGameOver(engine:GameEngine) {}

	/** It will be called during the end-of-game stats screen.*/
	open fun onResult(engine:GameEngine) {}

	/** It will be called during the field editor screen.*/
	open fun onFieldEdit(engine:GameEngine) {}

	/** It will be called at the start of each frame. (For rendering
	 * @param inside Draw method inside frame*/
	open fun renderFirst(engine:GameEngine, inside:()->Unit = {}) {}

	/** It will be called at the end of each frame. (For rendering)*/
	open fun renderLast(engine:GameEngine) {}

	/** It will be called at the settings screen. (For rendering)*/
	open fun renderSetting(engine:GameEngine) {}

	/** It will be called at the settings screen. (For rendering)*/
	open fun renderProfile(engine:GameEngine) {
		engine.playerProp.loginScreen.renderScreen(this, engine)
	}
	/** It will be called during the "Ready->Go" screen. (For rendering)*/
	open fun renderReady(engine:GameEngine) {}

	/** It will be called during the piece movement. (For rendering)*/
	open fun renderMove(engine:GameEngine) {}

	/** It will be called during the "ARE". (For rendering)*/
	open fun renderARE(engine:GameEngine) {}

	/** It will be called during the "Ending start" screen. (For rendering)*/
	open fun renderEndingStart(engine:GameEngine) {}

	/** It will be called during the "Custom" screen. (For rendering)*/
	open fun renderCustom(engine:GameEngine) {}

	/** It will be called during the "EXCELLENT!" screen. (For rendering)*/
	open fun renderExcellent(engine:GameEngine) {}

	/** It will be called during the Game Over screen. (For rendering)*/
	open fun renderGameOver(engine:GameEngine) {}

	/** It will be called during the end-of-game stats screen. (For rendering)*/
	open fun renderResult(engine:GameEngine) {}

	/** It will be called during the field editor screen. (For rendering)*/
	open fun renderFieldEdit(engine:GameEngine) {}

	/** It will be called if the player's input is being displayed. (For rendering)*/
	open fun renderInput(engine:GameEngine) {
		val pid = engine.playerID
		val x = 170*(pid+1)-110
		val y = 464
		val col = getPlayerColor(pid)
		val ctrl = engine.ctrl
		drawDirectFont(x+0*16, y, "<", if(ctrl.isPress(Controller.BUTTON_LEFT)) col else COLOR.WHITE, 1f)
		drawDirectFont(x+1*16, y, BaseFont.DOWN_S, if(ctrl.isPress(Controller.BUTTON_DOWN)) col else COLOR.WHITE)
		drawDirectFont(x+2*16, y, BaseFont.UP_S, if(ctrl.isPress(Controller.BUTTON_UP)) col else COLOR.WHITE)
		drawDirectFont(x+3*16, y, ">", if(ctrl.isPress(Controller.BUTTON_RIGHT)) col else COLOR.WHITE)
		drawDirectFont(x+4*16, y, "A", if(ctrl.isPress(Controller.BUTTON_A)) col else COLOR.WHITE)
		drawDirectFont(x+5*16, y, "B", if(ctrl.isPress(Controller.BUTTON_B)) col else COLOR.WHITE)
		drawDirectFont(x+6*16, y, "C", if(ctrl.isPress(Controller.BUTTON_C)) col else COLOR.WHITE)
		drawDirectFont(x+7*16, y, "D", if(ctrl.isPress(Controller.BUTTON_D)) col else COLOR.WHITE)
		drawDirectFont(x+8*16, y, "E", if(ctrl.isPress(Controller.BUTTON_E)) col else COLOR.WHITE)
		drawDirectFont(x+9*16, y, "F", if(ctrl.isPress(Controller.BUTTON_F)) col else COLOR.WHITE)
		drawDirectNano(x, y, "%1dP:%04d".format(pid+1, engine.ctrl.buttonBit), col, 0.5f)
	}

	/** It will be called during the line clear. (For rendering)*/
	open fun renderLineClear(engine:GameEngine) {}

	/** It will be called during the "Lock flash". (For rendering)*/
	open fun renderLockFlash(engine:GameEngine) {}

	/** It will be called before blocks are destroyed.
	 * @param engine GameEngine
	 * @param blk Indexed Iterable (listOf(y:listOf(x:Block))
	 */
	open fun blockBreak(engine:GameEngine, blk:Map<Int, Map<Int, Block>>) {}
	/** It will be called before a block is destroyed.
	 * @param engine GameEngine
	 * @param blk Indexed Iterable (listOf(y:listOf(x:Block))
	 * @param x X-coordinate grid
	 * @param y Y-coordinate grid
	 */
	fun blockBreak(engine:GameEngine, x:Int, y:Int, blk:Block) = blockBreak(engine, mapOf(y to mapOf(x to blk)))

	/** It will be called when the game mode is going to calculate score.
	 * Please note it will be called even if no lines are cleared.
	 * @param engine GameEngine
	 * @param event Event the score gained
	 */
	open fun calcScore(engine:GameEngine, event:ScoreEvent?) {}

	/** Every Current Piece falls
	 * @param engine GameEngine
	 * @param fpf Number of rows the piece fell
	 */
	open fun afterPieceFall(engine:GameEngine, fpf:Int) {}

	/** After Soft Drop is used
	 * @param engine GameEngine
	 * @param fall Number of rows the piece fell by Soft Drop
	 */
	open fun afterSoftDropFall(engine:GameEngine, fall:Int) {}

	/** After Hard Drop is used
	 * @param engine GameEngine
	 * @param fall Number of rows the piece fell by Hard Drop
	 */
	open fun afterHardDropFall(engine:GameEngine, fall:Int) {}

	/** It will be called when the player exit the field editor.*/
	open fun fieldEditExit(engine:GameEngine) {}

	/** It will be called when the p has locked. (after calcScore)
	 * @param engine GameEngine
	 * @param pX pieceX
	 * @param pY pieceY
	 * @param p piece
	 * @param lines Number of lines to be cleared (can be 0)
	 */
	open fun pieceLocked(engine:GameEngine, pX:Int, pY:Int, p:Piece, lines:Int, finesse:Boolean) {}

	/** It will be called at the end of line-clear phase.*/
	open fun lineClearEnd(engine:GameEngine) {}

	/** Called when saving replay into [prop]
	 * @param owner GameManager
	 * @param folderName Replay folder name
	 */
	open fun saveReplay(owner:GameManager, prop:CustomProperties, folderName:String = "replay") {
		if(owner.mode?.isOnlineMode!=false) return
		val folder = "$folderName/${owner.mode?.javaClass?.simpleName ?: ""}"
		val filename = "$folder/"+prop.getProperty("name.rule").lowercase().toReplayFilename.replace(Regex("[\\s-]"), "_")
		try {
			val repFolder = File(folder)
			if(!repFolder.exists()) if(repFolder.mkdirs()) log.info("Created replay folder: $folder")
			else log.error("Couldn't create replay folder at $folder")

			val out = GZIPOutputStream(FileOutputStream(filename))
			prop.store(out, "NullpoMino Replay")
			out.close()
			log.info("Saved replay file: $filename")
		} catch(e:IOException) {
			log.error("Couldn't save replay file to $filename", e)
		}
	}

	open val doesGraphicsExist:Boolean = false

	/** 演出オブジェクト */
	internal val efxBG:MutableList<Effect> = mutableListOf()
	/** 演出オブジェクト */
	internal val efxFG:MutableList<Effect> = mutableListOf()

	companion object {
		/** cell and block size(block,font) */
		const val BS = 16
		/** @return Width&Height of block image*/
		fun getScaleF(displaySize:Int):Float = when(displaySize) {
			-1 -> .5f
			1 -> 2f
			else -> 1f
		}
		/** @return Width&Height of block image*/
		fun getBlockSize(displaySize:Int):Int = when(displaySize) {
			-1 -> BS/2
			1 -> BS*2
			else -> BS
		}
		/** Log */
		internal val log = LogManager.getLogger()

		fun getRainbowColor(i:Int, o:Int = 0):COLOR = when((i-o%10)%10) {
			1, -9 -> COLOR.RED
			2, -8 -> COLOR.ORANGE
			3, -7 -> COLOR.YELLOW
			4, -6 -> COLOR.GREEN
			5, -5 -> COLOR.CYAN
			6, -4 -> COLOR.BLUE
			7, -3 -> COLOR.COBALT
			8, -2 -> COLOR.PURPLE
			9, -1 -> COLOR.PINK
			else -> COLOR.WHITE
		}

		fun getPlayerColor(playerID:Int):COLOR = when(playerID) {
			0 -> COLOR.BLUE
			1 -> COLOR.RED
			2 -> COLOR.GREEN
			3 -> COLOR.PURPLE
			4 -> COLOR.ORANGE
			5 -> COLOR.WHITE
			else -> COLOR.all.let {it.getOrElse((playerID-5)%it.size) {COLOR.WHITE}}
		}

		fun getBlockColor(engine:GameEngine, piece:Piece.Shape):COLOR =
			getBlockColor(Block.intToColor(engine.ruleOpt.pieceColor[piece.ordinal]))

		fun getBlockColor(b:Block):COLOR = getBlockColor(b.color, b.type)
		fun getBlockColor(it:Pair<Block.COLOR?, Block.TYPE>):COLOR = getBlockColor(it.first, it.second)
		fun getBlockColor(color:Block.COLOR?, type:Block.TYPE? = Block.TYPE.BLOCK):COLOR = when(type) {
			Block.TYPE.SQUARE_GOLD -> COLOR.YELLOW
			Block.TYPE.SQUARE_SILVER -> COLOR.BLUE
			else -> when(color) {
				Block.COLOR.BLACK -> COLOR.WHITE
				Block.COLOR.WHITE -> COLOR.WHITE
				Block.COLOR.RED -> COLOR.RED
				Block.COLOR.ORANGE -> COLOR.ORANGE
				Block.COLOR.YELLOW -> COLOR.YELLOW
				Block.COLOR.GREEN -> COLOR.GREEN
				Block.COLOR.CYAN -> COLOR.CYAN
				Block.COLOR.BLUE -> if(type==Block.TYPE.GEM) COLOR.BLUE else COLOR.COBALT
				Block.COLOR.PURPLE -> if(type==Block.TYPE.GEM) COLOR.PINK else COLOR.PURPLE
				Block.COLOR.RAINBOW -> COLOR.RAINBOW
				else -> COLOR.WHITE
			}
		}

		/** Field X position */
		val NEW_FIELD_OFFSET_X = listOf(
			listOf(// TETROMINO
				listOf(119, 247, 375, 503, 247, 375, 375), // Small
				listOf(32, 432, 432, 432, 432, 432, 432), // Normal
				listOf(16, 416, 416, 416, 416, 416, 416)
			)// Big
			, listOf(// AVALANCHE
				listOf(119, 247, 375, 503, 247, 375, 375), // Small
				listOf(32, 432, 432, 432, 432, 432, 432), // Normal
				listOf(16, 352, 352, 352, 352, 352, 352)
			)// Big
			, listOf(// PHYSICIAN
				listOf(119, 247, 375, 503, 247, 375, 375), // Small
				listOf(32, 432, 432, 432, 432, 432, 432), // Normal
				listOf(16, 416, 416, 416, 416, 416, 416)
			)// Big
			, listOf(// SPF
				listOf(119, 247, 375, 503, 247, 375, 375), // Small
				listOf(32, 432, 432, 432, 432, 432, 432), // Normal
				listOf(16, 352, 352, 352, 352, 352, 416)
			)// Big
		)

		/** Field Y position */
		val NEW_FIELD_OFFSET_Y = listOf(
			listOf(// TETROMINO
				listOf(80, 80, 80, 80, 286, 286, 286), // Small
				listOf(32), // Normal
				listOf(8)
			)// Big
			, listOf(// AVALANCHE
				listOf(80, 80, 80, 80, 286, 286, 286), // Small
				listOf(32), // Normal
				listOf(8)
			)// Big
			, listOf(// PHYSICIAN
				listOf(80, 80, 80, 80, 286, 286, 286), // Small
				listOf(32), // Normal
				listOf(8)
			)// Big
			, listOf(// SPF
				listOf(80, 80, 80, 80, 286, 286, 286), // Small
				listOf(32), // Normal
				listOf(8)
			)// Big
		)

		const val nextHeight = 48

		/** Field X position (Big side preview) */
		val NEW_FIELD_OFFSET_X_BSP = listOf(
			listOf(// TETROMINO
				listOf(208, 320, 432, 544, 320, 432, 544), // Small
				listOf(64, 400), // Normal
				listOf(16, 352)
			)// Big
			, listOf(// AVALANCHE
				listOf(208, 320, 432, 544, 320, 432, 544), // Small
				listOf(64, 400), // Normal
				listOf(16, 352)
			)// Big
			, listOf(// PHYSICIAN
				listOf(208, 320, 432, 544, 320, 432, 544), // Small
				listOf(64, 400), // Normal
				listOf(16, 352)
			)// Big
			, listOf(// SPF
				listOf(208, 320, 432, 544, 320, 432, 544), // Small
				listOf(64, 400), // Normal
				listOf(16, 352)
			)// Big
		)

		/** Field Y position (Big side preview) */
		val NEW_FIELD_OFFSET_Y_BSP = listOf(
			listOf(// TETROMINO
				listOf(80, 80, 80, 80, 286, 286, 286), // Small
				listOf(32), // Normal
				listOf(8)
			)// Big
			, listOf(// AVALANCHE
				listOf(80, 80, 80, 80, 286, 286, 286), // Small
				listOf(32), // Normal
				listOf(8)
			)// Big
			, listOf(// PHYSICIAN
				listOf(80, 80, 80, 80, 286, 286, 286), // Small
				listOf(32), // Normal
				listOf(8)
			)// Big
			, listOf(// SPF
				listOf(80, 80, 80, 80, 286, 286, 286), // Small
				listOf(32), // Normal
				listOf(8)
			)// Big
		)
	}

}
