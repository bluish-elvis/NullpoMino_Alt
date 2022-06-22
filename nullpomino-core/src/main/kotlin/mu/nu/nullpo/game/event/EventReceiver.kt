/*
 * Copyright (c) 2010-2022, NullNoname
 * Kotlin converted and modified by Venom=Nhelv
 * All rights reserved.
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
package mu.nu.nullpo.game.event

import mu.nu.nullpo.game.component.Block
import mu.nu.nullpo.game.component.Piece
import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.game.play.GameManager
import mu.nu.nullpo.gui.common.PopupCombo
import mu.nu.nullpo.util.CustomProperties
import mu.nu.nullpo.util.GeneralUtil.toReplayFilename
import org.apache.logging.log4j.LogManager
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import kotlin.math.sqrt
import kotlin.random.Random

/** Drawing and event handling EventReceiver */
open class EventReceiver {

	protected val random = Random.Default

	/** Font cint constants */
	enum class FONT { NORMAL, NANO, NUM, GRADE, GRADE_BIG, MEDAL, TTF; }

	/** Font cint constants */
	enum class COLOR { WHITE, BLUE, RED, PINK, GREEN, YELLOW, CYAN, ORANGE, PURPLE, COBALT, RAINBOW; }

	/** Background display */
	protected var showbg = false

	/** Show meter */
	protected var showMeter = false

	/** Show ARE meter */
	protected var showSpeed = false

	/** Outline ghost piece */
	protected var outlineghost = false

	/** Piece previews on sides */
	protected var sidenext = false

	/** Use bigger side previews */
	protected var bigsidenext = false

	/** Is TTF font available?
	 * @return true if you can use TTF font routines.
	 */
	open val isTTFSupport:Boolean
		get() = false

	/** Get type of piece preview
	 * @return 0=Above 1=Side Small 2=Side Big
	 */
	val nextDisplayType:Int
		get() = if(sidenext) if(bigsidenext) 2 else 1 else 0

	/** Get Number of Block Skins
	 * @return int blockskins
	 */
	open val skinMax:Int
		get() = 0

	/** It will be called when a line is cleared.
	 * @param engine GameEngine
	 * @param playerID Player ID
	 * @param y Y-coordinate
	 */
	open fun lineClear(engine:GameEngine, playerID:Int, y:Int) {}

	open fun addScore(x:Int, y:Int, pts:Int, color:COLOR) {}

	/** After Score Gained, to show points.
	 * @param engine GameEngine
	 * @param pts Number of points last gained
	 */
	fun addScore(engine:GameEngine, x:Int, y:Int, pts:Int, color:COLOR = getPlayerColor(engine.playerID)) =
		addScore(fieldX(engine)+x*getBlockSize(engine), fieldY(engine)+y*getBlockSize(engine), pts, color)

	open fun addCombo(x:Int, y:Int, pts:Int, type:PopupCombo.CHAIN, ex:Int) {}

	/** After Score Gained, to show combo.
	 * @param engine GameEngine
	 * @param pts Number of points last gained
	 */
	fun addCombo(engine:GameEngine, x:Int, y:Int, pts:Int, type:PopupCombo.CHAIN) =
		addCombo(
			fieldX(engine)+x*getBlockSize(engine), fieldY(engine)+y*getBlockSize(engine),
			pts, type, -(x+2)*getBlockSize(engine)+18
		)

	/** It will be called when a fireworks shoot.
	 * @param engine GameEngine
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param color Color
	 */
	open fun shootFireworks(engine:GameEngine, x:Int, y:Int, color:COLOR) {
		playSE("firecracker"+(random.nextInt(2)+1))
	}

	/** It will be called when a fireworks shoot
	 * @param engine GameEngine
	 */
	fun shootFireworks(engine:GameEngine) {
		val mx = engine.fieldWidth*getBlockSize(engine)
		val my = engine.fieldHeight*8
		val x = fieldX(engine)+random.nextInt(mx)
		var y = fieldY(engine)+random.nextInt(my)+50
		if(my<engine.field.highestBlockY) y += my
		shootFireworks(engine, x, y, COLOR.values()[random.nextInt(7)])
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
	open fun drawFont(x:Int, y:Int, str:String, font:FONT = FONT.NORMAL,
		color:COLOR = COLOR.WHITE, scale:Float = 1f, alpha:Float = 1f) {
	}

	fun drawDirectFont(x:Int, y:Int, str:String, color:COLOR = COLOR.WHITE, scale:Float = 1f, alpha:Float = 1f) =
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
	fun drawDirectFont(playerID:Int, x:Int, y:Int, str:String, flag:Boolean, scale:Float = 1f) =
		drawDirectFont(x, y, str, if(flag) getPlayerColor(playerID) else COLOR.WHITE, scale)

	fun drawDirectNano(x:Int, y:Int, str:String, color:COLOR = COLOR.WHITE, scale:Float = 1f, alpha:Float = 1f) =
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
	fun drawDirectNano(playerID:Int, x:Int, y:Int, str:String, flag:Boolean, scale:Float = 1f) =
		drawDirectNano(x, y, str, if(flag) getPlayerColor(playerID) else COLOR.WHITE, scale)

	/** [You don't have to override this]
	 * Draw Number to any location.
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param str String to draw
	 * @param color Font cint
	 * @param scale Font size (.5f, 1f, 2f)
	 */
	fun drawDirectNum(x:Int, y:Int, str:String, color:COLOR = COLOR.WHITE, scale:Float = 1f, alpha:Float = 1f) =
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
	fun drawDirectNum(playerID:Int, x:Int, y:Int, str:String, flag:Boolean, scale:Float = 1f) =
		drawDirectNum(x, y, str, if(flag) getPlayerColor(playerID) else COLOR.WHITE, scale)

	/** [You don't have to override this]
	 * Draw Grade to any location.
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param str String to draw
	 * @param color Font cint
	 * @param scale Font size (.5f, 1f, 2f)
	 */
	fun drawDirectGrade(x:Int, y:Int, str:String, color:COLOR = COLOR.WHITE, scale:Float = 1f) =
		drawFont(x, y, str, FONT.GRADE, color, scale)

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
	fun drawDirectGrade(playerID:Int, x:Int, y:Int, str:String, flag:Boolean, scale:Float = 1f) =
		drawDirectGrade(x, y, str, if(flag) getPlayerColor(playerID) else COLOR.WHITE, scale)

	/** [YOu don't have to override this]
	 * Draw TTF String to any location.
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param str String to draw
	 * @param color Font cint
	 */
	fun drawDirectTTF(x:Int, y:Int, str:String, color:COLOR = COLOR.WHITE) {
		drawFont(x, y, str, FONT.TTF, color)
	}

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
	fun drawDirectTTF(playerID:Int, x:Int, y:Int, str:String, flag:Boolean) {
		drawDirectTTF(x, y, str, if(flag) getPlayerColor(playerID) else COLOR.WHITE)
	}

	/** Draw String inside the field.
	 * @param engine GameEngine
	 * @param playerID Player ID
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param str String to draw
	 * @param font Fomt specifies
	 * @param color Font cint
	 * @param scale Font size (.5f, 1f, 2f)
	 */
	fun drawMenu(engine:GameEngine, playerID:Int, x:Int, y:Int, str:String,
		font:FONT = FONT.NORMAL, color:COLOR = COLOR.WHITE, scale:Float = 1f) {
		val s = if(scale<=.5f) BS/2 else BS
		var sx = x*s
		var sy = y*s
		if(!engine.owner.menuOnly) {
			sx += fieldX(engine, playerID)+4
			sy += fieldY(engine, playerID)+52
		}
		drawFont(sx, sy, str, font, color, scale)
	}

	fun drawMenuFont(engine:GameEngine, playerID:Int, x:Int, y:Int, str:String, color:COLOR = COLOR.WHITE,
		scale:Float = 1f) =
		drawMenu(engine, playerID, x, y, str, FONT.NORMAL, color, scale)

	fun drawMenuFont(engine:GameEngine, playerID:Int, x:Int, y:Int, str:String, scale:Float = 1f) =
		drawMenu(engine, playerID, x, y, str, FONT.NORMAL, scale = scale)

	/** [You don't have to override this]
	 * Draw String inside the field.
	 * If flag is false, it will use white font cint.
	 * If flag is true, it will use red or blue instead.
	 * @param engine GameEngine
	 * @param playerID Player ID
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param str String to draw
	 * @param flag Any boolean variable
	 * @param scale Font size
	 */
	fun drawMenuFont(engine:GameEngine, playerID:Int, x:Int, y:Int, str:String, flag:Boolean, scale:Float = 1f) =
		drawMenuFont(engine, playerID, x, y, str, color = if(flag) getPlayerColor(playerID) else COLOR.WHITE, scale = scale)

	/** [You don't have to override this]
	 * Draw Number inside the field.
	 * @param engine GameEngine
	 * @param playerID Player ID
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param str String to draw
	 * @param color Font cint
	 * @param scale Font size
	 */
	fun drawMenuNum(engine:GameEngine, playerID:Int, x:Int, y:Int, str:String, color:COLOR = COLOR.WHITE, scale:Float = 1f) =
		drawMenu(engine, playerID, x, y, str, FONT.NUM, color, scale)

	/** [You don't have to override this]
	 * Draw Number inside the field.
	 * @param engine GameEngine
	 * @param playerID Player ID
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param str String to draw
	 * @param scale Font size
	 */
	fun drawMenuNum(engine:GameEngine, playerID:Int, x:Int, y:Int, str:String, scale:Float = 1f) =
		drawMenu(engine, playerID, x, y, str, FONT.NUM, scale = scale)

	/** [You don't have to override this]
	 * Draw Number inside the field.
	 * If flag is false, it will use white font cint.
	 * If flag is true, it will use red or blue instead.
	 * @param engine GameEngine
	 * @param playerID Player ID
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param str String to draw
	 * @param flag Any boolean variable
	 * @param scale Font size
	 */
	fun drawMenuNum(engine:GameEngine, playerID:Int, x:Int, y:Int, str:String, flag:Boolean, scale:Float = 1f) =
		drawMenuNum(engine, playerID, x, y, str, if(flag) getPlayerColor(playerID) else COLOR.WHITE, scale)

	fun drawMenuNano(engine:GameEngine, playerID:Int, x:Int, y:Int, str:String, color:COLOR = COLOR.WHITE,
		scale:Float = 1f) =
		drawMenu(engine, playerID, x, y, str, FONT.NANO, color, scale)

	fun drawMenuNano(engine:GameEngine, playerID:Int, x:Int, y:Int, str:String, flag:Boolean, scale:Float = 1f) =
		drawMenuNano(
			engine, playerID, x, y, str, if(flag) if(playerID%2==0) COLOR.YELLOW else COLOR.ORANGE else COLOR.WHITE,
			scale
		)

	/** [You don't have to override this]
	 * Draw Grade inside the field.
	 * @param engine GameEngine
	 * @param playerID Player ID
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param str String to draw
	 * @param color Font cint
	 * @param scale Font size
	 */
	fun drawMenuGrade(engine:GameEngine, playerID:Int, x:Int, y:Int, str:String, color:COLOR = COLOR.WHITE,
		scale:Float = 1f) =
		drawMenu(engine, playerID, x, y, str, FONT.GRADE, color, scale)

	/** [You don't have to override this]
	 * Draw Grade inside the field.
	 * If flag is false, it will use white font cint.
	 * If flag is true, it will use red or blue instead.
	 * @param engine GameEngine
	 * @param playerID Player ID
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param str String to draw
	 * @param flag Any boolean variable
	 * @param scale Font size
	 */
	fun drawMenuGrade(engine:GameEngine, playerID:Int, x:Int, y:Int, str:String, flag:Boolean, scale:Float = 1f) =
		drawMenuGrade(engine, playerID, x, y, str, if(flag) getPlayerColor(playerID) else COLOR.WHITE, scale)

	/** [You don't have to override this]
	 * Draw String inside the field.
	 * @param engine GameEngine
	 * @param playerID Player ID
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param str String to draw
	 * @param color Font cint
	 */
	fun drawMenuTTF(engine:GameEngine, playerID:Int, x:Int, y:Int, str:String, color:COLOR = COLOR.WHITE) =
		drawMenu(engine, playerID, x, y, str, font = FONT.TTF, color = color)

	/** [You don't have to override this]
	 * Draw String inside the field.
	 * If flag is false, it will use white font cint.
	 * If flag is true, it will use red or blue instead.
	 * @param engine GameEngine
	 * @param playerID Player ID
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param str String to draw
	 * @param flag Any boolean variable
	 */
	fun drawMenuTTF(engine:GameEngine, playerID:Int, x:Int, y:Int, str:String, flag:Boolean) =
		drawMenuTTF(engine, playerID, x, y, str, if(flag) getPlayerColor(playerID) else COLOR.WHITE)

	/** Draw String to score display area.
	 * @param engine GameEngine
	 * @param playerID Player ID
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param str String to draw
	 * @param color Font cint
	 * @param scale Font size (.5f, 1f, 2f)
	 */
	fun drawScore(engine:GameEngine, playerID:Int, x:Int, y:Int, str:String,
		font:FONT = FONT.NORMAL, color:COLOR = COLOR.WHITE, scale:Float = 1f) {
		if(engine.owner.menuOnly) return
		val s = if(scale<=.5f) BS/2 else BS
		drawFont(scoreX(engine, playerID)+x*s, scoreY(engine, playerID)+y*s, str, font, color, scale)
	}

	/** [You don't have to override this]
	 * Draw String to score display area.
	 * If flag is false, it will use white font cint. If flag is true, it will
	 * use red instead.
	 * @param engine GameEngine
	 * @param playerID Player ID
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param str String to draw
	 * @param color Font cint
	 * @param scale Font size
	 */
	fun drawScoreFont(engine:GameEngine, playerID:Int, x:Int, y:Int, str:String, color:COLOR = COLOR.WHITE, scale:Float = 1f) =
		drawScore(engine, playerID, x, y, str, FONT.NORMAL, color, scale)

	fun drawScoreFont(engine:GameEngine, playerID:Int, x:Int, y:Int, str:String, scale:Float = 1f) =
		drawScore(engine, playerID, x, y, str, FONT.NORMAL, scale = scale)

	/** [You don't have to override this]
	 * Draw String to score display area.
	 * If flag is false, it will use white font cint. If flag is true, it will
	 * use red instead.
	 * @param engine GameEngine
	 * @param playerID Player ID
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param str String to draw
	 * @param flag Any boolean variable
	 * @param scale Font size
	 */
	fun drawScore(engine:GameEngine, playerID:Int, x:Int, y:Int, str:String, font:FONT = FONT.NORMAL, flag:Boolean,
		scale:Float = 1f) =
		drawScore(engine, playerID, x, y, str, font, color = if(flag) COLOR.RAINBOW else COLOR.WHITE, scale = scale)

	fun drawScoreFont(engine:GameEngine, playerID:Int, x:Int, y:Int, str:String, flag:Boolean, scale:Float = 1f) =
		drawScore(engine, playerID, x, y, str, FONT.NORMAL, flag = flag, scale = scale)

	fun drawScoreNum(engine:GameEngine, playerID:Int, x:Int, y:Int, str:String, color:COLOR = COLOR.WHITE,
		scale:Float = 1f) =
		drawScore(engine, playerID, x, y, str, FONT.NUM, color, scale)

	fun drawScoreNum(engine:GameEngine, playerID:Int, x:Int, y:Int, str:String, scale:Float = 1f) =
		drawScore(engine, playerID, x, y, str, FONT.NUM, scale = scale)

	fun drawScoreNum(engine:GameEngine, playerID:Int, x:Int, y:Int, str:String, flag:Boolean, scale:Float = 1f) =
		drawScoreNum(engine, playerID, x, y, str, if(flag) COLOR.RAINBOW else COLOR.WHITE, scale)

	fun drawScoreNano(engine:GameEngine, playerID:Int, x:Int, y:Int, str:String, color:COLOR = COLOR.WHITE,
		scale:Float = 1f) =
		drawScore(engine, playerID, x, y, str, FONT.NANO, color, scale)

	fun drawScoreNano(engine:GameEngine, playerID:Int, x:Int, y:Int, str:String, flag:Boolean, scale:Float = 1f) =
		drawScoreNano(engine, playerID, x, y, str, if(flag) COLOR.RAINBOW else COLOR.WHITE, scale)

	fun drawScoreGrade(engine:GameEngine, playerID:Int, x:Int, y:Int, str:String, color:COLOR = COLOR.WHITE,
		scale:Float = 1f) =
		drawScore(engine, playerID, x, y, str, FONT.GRADE, color, scale)

	fun drawScoreGrade(engine:GameEngine, playerID:Int, x:Int, y:Int, str:String, flag:Boolean, scale:Float = 1f) =
		drawScoreGrade(engine, playerID, x, y, str, if(flag) COLOR.RAINBOW else COLOR.WHITE, scale)

	fun drawScoreTTF(engine:GameEngine, playerID:Int, x:Int, y:Int, str:String, color:COLOR = COLOR.WHITE) =
		drawScore(engine, playerID, x, y, str, FONT.TTF, color)

	fun drawScoreTTF(engine:GameEngine, playerID:Int, x:Int, y:Int, str:String, flag:Boolean) =
		drawScoreTTF(engine, playerID, x, y, str, if(flag) getPlayerColor(playerID) else COLOR.WHITE)

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
	open fun drawBlock(x:Float, y:Float, color:Int, skin:Int, bone:Boolean = false, darkness:Float = 0f, alpha:Float = 1f,
		scale:Float = 1f, attr:Int = 0, outline:Float = 0f) {
	}

	@JvmOverloads
	fun drawBlock(x:Int, y:Int, color:Int, skin:Int, bone:Boolean, darkness:Float, alpha:Float, scale:Float, attr:Int = 0,
		outline:Float = 0f) =
		drawBlock(x.toFloat(), y.toFloat(), color, skin, bone, darkness, alpha, scale, attr, outline)

	/** Blockクラスのインスタンスを使用してBlockを描画 (拡大率と暗さ指定可能）
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param block Blockクラスのインスタンス
	 * @param scale 拡大率
	 * @param darkness 暗さもしくは明るさ
	 */
	@JvmOverloads
	fun drawBlock(x:Float, y:Float, block:Block, darkness:Float = block.darkness, alpha:Float = block.alpha,
		scale:Float = 1f, outline:Float = 0f) =
		drawBlock(
			x, y, block.drawColor, block.skin, block.getAttribute(Block.ATTRIBUTE.BONE), darkness, alpha, scale,
			block.aint, outline
		)

	@JvmOverloads
	fun drawBlockForceVisible(x:Float, y:Float, blk:Block, scale:Float = 1f) =
		drawBlock(
			x, y, blk.drawColor, blk.skin, blk.getAttribute(Block.ATTRIBUTE.BONE),
			blk.darkness/2, .5f*blk.alpha+.5f, scale, blk.aint
		)

	/** Blockピースを描画 (暗さもしくは明るさの指定可能）
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param piece 描画するピース
	 * @param scale 拡大率
	 * @param darkness 暗さもしくは明るさ
	 */
	@JvmOverloads
	fun drawPiece(x:Int, y:Int, piece:Piece, scale:Float = 1f, darkness:Float = 0f, alpha:Float = 1f, ow:Float = 0f) =
		piece.block.forEachIndexed {i, blk ->
			val ls = scale*if(piece.big) 32 else 16
			drawBlock(
				x+(piece.dataX[piece.direction][i].toFloat()*ls), y+(piece.dataY[piece.direction][i].toFloat()*ls),
				blk, blk.darkness+darkness, blk.alpha*alpha, scale, ow
			)
		}

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

	/** Draw speed meter.
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param sp Speed (float:0.0~1.0)
	 * @param len Meter Width
	 */
	open fun drawSpeedMeter(x:Float, y:Float, sp:Float, len:Float) {}

	/** Draw speed meter.
	 * @param engine GameEngine
	 * @param playerID Player ID
	 * @param x X-coordinate grid
	 * @param y Y-coordinate grid
	 * @param sp Speed (float:0.0~1.0 int:0~40)
	 * @param len Meter Width Grid
	 */
	fun drawSpeedMeter(engine:GameEngine, playerID:Int, x:Int, y:Int, sp:Float, len:Float = 3f) {
		val dx1 = scoreX(engine, playerID)+x*BS+maxOf(minOf(len, BS/2f), 0f)
		val dy1 = scoreY(engine, playerID)+y*BS+BS/2f
		//if(engine.owner.menuOnly) return
		drawSpeedMeter(dx1, dy1, sp, len)
	}

	/** Draw speed meter.
	 * @param engine GameEngine
	 * @param playerID Player ID
	 * @param x X-coordinate grid
	 * @param y Y-coordinate grid
	 * @param sp Speed (float:0.0~1.0 int:0~40)
	 * @param len Meter Width Grid
	 */
	fun drawSpeedMeter(engine:GameEngine, playerID:Int, x:Int, y:Int, sp:Int, len:Int = 3) =
		drawSpeedMeter(engine, playerID, x, y, sp/40f, len.toFloat())

	/** Draw speed meter.
	 * @param engine GameEngine
	 * @param playerID Player ID
	 * @param x X-coordinate grid
	 * @param y Y-coordinate grid
	 * @param g gravity Value
	 * @param d gravity Denominator
	 */
	fun drawSpeedMeter(engine:GameEngine, playerID:Int, x:Int, y:Int, g:Int, d:Int, len:Int = 3) {
		var s = if(g<=0) 1f else 0f
		if(g>0&&d>0) s = (sqrt((g.toFloat()/d).toDouble())/sqrt(20.0)).toFloat()
		drawSpeedMeter(engine, playerID, x, y, s, len.toFloat())
	}

	/** Get maximum length of the meter.
	 * @param engine GameEngine
	 * @return Maximum length of the meter
	 */
	fun getMeterMax(engine:GameEngine):Int = if(!showMeter) 0 else engine.field.height*getBlockSize(engine)

	/** Draw Decorations at score pos
	 * @param engine GameEngine
	 * @param playerID Player ID
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param scale size
	 */
	fun drawScoreBadges(engine:GameEngine, playerID:Int, x:Int, y:Int, width:Int, nums:Int,
		scale:Float = 1f) =
		drawBadges(scoreX(engine, playerID)+x*BS, scoreY(engine, playerID)+y*BS, width, nums, scale)

	/** Draw Decorations at menu pos
	 * @param engine GameEngine
	 * @param playerID Player ID
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param scale size
	 */
	fun drawMenuBadges(engine:GameEngine, playerID:Int, x:Int, y:Int, nums:Int, scale:Float = 1f) {
		var sx = x*BS
		var sy = y*BS
		if(!engine.owner.menuOnly) {
			sx += fieldX(engine, playerID)+4
			sy += fieldY(engine, playerID)+52
		}
		drawBadges(sx, sy, BS*10, nums, scale)
	}

	/** Draw Decorations
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param width linebreak width
	 * @param scale size
	 */
	open fun drawBadges(x:Int, y:Int, width:Int = 0, nums:Int, scale:Float = 1f) {}

	/** Draw Medal at score pos
	 * @param engine GameEngine
	 * @param playerID Player ID
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param scale size
	 */
	fun drawScoreMedal(engine:GameEngine, playerID:Int, x:Int, y:Int, str:String, tier:Int, scale:Float = 1f) =
		drawMedal(scoreX(engine, playerID)+x*BS, scoreY(engine, playerID)+y*BS, str, tier, scale)

	/** Draw Medal at menu pos
	 * @param engine GameEngine
	 * @param playerID Player ID
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param scale size
	 */
	fun drawMenuMedal(engine:GameEngine, playerID:Int, x:Int, y:Int, str:String, tier:Int, scale:Float = 1f) {
		var sx = x*BS
		var sy = y*BS
		if(!engine.owner.menuOnly) {
			sx += fieldX(engine, playerID)+4
			sy += fieldY(engine, playerID)+52
		}
		drawMedal(sx, sy, str, tier, scale)
	}

	/** Draw Medal
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param tier medal Tier
	 * @param scale size
	 */
	open fun drawMedal(x:Int, y:Int, str:String, tier:Int, scale:Float = 1f) {}

	/** Get width and Height of block image.
	 * @param engine GameEngine
	 * @return Width of block image
	 */
	fun getBlockSize(engine:GameEngine):Int = when(engine.displaysize) {
		-1 -> BS/2
		1 -> BS*2
		else -> BS
	}

	/** Get X position of field
	 * @param engine GameEngine
	 * @param playerID Player ID
	 * @return X position of field
	 */
	fun fieldX(engine:GameEngine, playerID:Int = engine.playerID):Int {
		engine.owner.mode?.let {
			return@fieldX fieldXOffset(engine)+if(nextDisplayType==2)
				NEW_FIELD_OFFSET_X_BSP[it.gameStyle.ordinal][engine.displaysize+1][playerID]
			else NEW_FIELD_OFFSET_X[it.gameStyle.ordinal][engine.displaysize+1][playerID]
		} ?: return 0
	}

	fun fieldXOffset(engine:GameEngine):Int = maxOf(0, getBlockSize(engine)*(10-engine.field.width)/2)
	/** Get Y position of field
	 * @param engine GameEngine
	 * @param playerID Player ID
	 * @return Y position of field
	 */
	fun fieldY(engine:GameEngine, playerID:Int = engine.playerID):Int {
		engine.owner.mode?.let {
			return@fieldY if(nextDisplayType==2) NEW_FIELD_OFFSET_Y_BSP[it.gameStyle.ordinal][engine.displaysize+1][playerID]
			else NEW_FIELD_OFFSET_Y[it.gameStyle.ordinal][engine.displaysize+1][playerID]
		} ?: return 0
	}

	/** Get X position of score display area
	 * @param engine GameEngine
	 * @param playerID Player ID
	 * @return X position of score display area
	 */
	fun scoreX(engine:GameEngine, playerID:Int):Int =
		fieldX(engine, playerID)-fieldXOffset(engine)+(if(nextDisplayType==2) 256 else 216)+if(engine.displaysize==1) 32 else 0

	/** Get Y position of score display area
	 * @param engine GameEngine
	 * @param playerID Player ID
	 * @return Y position of score display area
	 */
	fun scoreY(engine:GameEngine, playerID:Int):Int = fieldY(engine, playerID)

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
	open fun setGraphics(g:Any) {}

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
			if(!repFolder.exists())
				if(repFolder.mkdirs()) log.info("Created folder: ${repFolder.name}")
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
	open fun modeInit(manager:GameManager) {}

	/** It will be called at the end of initialization for each player.
	 * @param engine GameEngine
	 * @param playerID Player ID
	 */
	open fun playerInit(engine:GameEngine, playerID:Int) {}

	/** It will be called when Ready->Go is about to end, before first piece
	 * appears.
	 * @param engine GameEngine
	 * @param playerID Player ID
	 */
	open fun startGame(engine:GameEngine, playerID:Int) {}

	/** It will be called at the start of each frame.
	 * @param engine GameEngine
	 * @param playerID Player ID
	 */
	open fun onFirst(engine:GameEngine, playerID:Int) {}

	/** It will be called at the end of each frame.
	 * @param engine GameEngine
	 * @param playerID Player ID
	 */
	open fun onLast(engine:GameEngine, playerID:Int) {}

	/** It will be called at the settings screen.
	 * @param engine GameEngine
	 * @param playerID Player ID
	 */
	open fun onSetting(engine:GameEngine, playerID:Int) {}

	/** It will be called at the settings screen.
	 * @param engine GameEngine
	 * @param playerID Player ID
	 */
	open fun onProfile(engine:GameEngine, playerID:Int) {}

	/** It will be called during the "Ready->Go" screen.
	 * @param engine GameEngine
	 * @param playerID Player ID
	 */
	open fun onReady(engine:GameEngine, playerID:Int) {}

	/** It will be called during the piece movement.
	 * @param engine GameEngine
	 * @param playerID Player ID
	 */
	open fun onMove(engine:GameEngine, playerID:Int) {}

	/** It will be called during the "Lock flash".
	 * @param engine GameEngine
	 * @param playerID Player ID
	 */
	open fun onLockFlash(engine:GameEngine, playerID:Int) {}

	/** It will be called during the line clear.
	 * @param engine GameEngine
	 * @param playerID Player ID
	 */
	open fun onLineClear(engine:GameEngine, playerID:Int) {}

	/** It will be called during the "ARE".
	 * @param engine GameEngine
	 * @param playerID Player ID
	 */
	open fun onARE(engine:GameEngine, playerID:Int) {}

	/** It will be called during the "Ending start" screen.
	 * @param engine GameEngine
	 * @param playerID Player ID
	 */
	open fun onEndingStart(engine:GameEngine, playerID:Int) {}

	/** It will be called during the "Custom" screen.
	 * @param engine GameEngine
	 * @param playerID Player ID
	 */
	open fun onCustom(engine:GameEngine, playerID:Int) {}

	/** It will be called during the "EXCELLENT!" screen.
	 * @param engine GameEngine
	 * @param playerID Player ID
	 */
	open fun onExcellent(engine:GameEngine, playerID:Int) {}

	/** It will be called during the Game Over screen.
	 * @param engine GameEngine
	 * @param playerID Player ID
	 */
	open fun onGameOver(engine:GameEngine, playerID:Int) {}

	/** It will be called during the end-of-game stats screen.
	 * @param engine GameEngine
	 * @param playerID Player ID
	 */
	open fun onResult(engine:GameEngine, playerID:Int) {}

	/** It will be called during the field editor screen.
	 * @param engine GameEngine
	 * @param playerID Player ID
	 */
	open fun onFieldEdit(engine:GameEngine, playerID:Int) {}

	/** It will be called at the start of each frame. (For rendering)
	 * @param engine GameEngine
	 * @param playerID Player ID
	 */
	open fun renderFirst(engine:GameEngine, playerID:Int) {}

	/** It will be called at the end of each frame. (For rendering)
	 * @param engine GameEngine
	 * @param playerID Player ID
	 */
	open fun renderLast(engine:GameEngine, playerID:Int) {}

	/** It will be called at the settings screen. (For rendering)
	 * @param engine GameEngine
	 * @param playerID Player ID
	 */
	open fun renderSetting(engine:GameEngine, playerID:Int) {}

	/** It will be called at the settings screen. (For rendering)
	 * @param engine GameEngine
	 * @param playerID Player ID
	 */
	open fun renderProfile(engine:GameEngine, playerID:Int) {
		engine.playerProp.loginScreen.renderScreen(this, engine, playerID)
	}
	/** It will be called during the "Ready->Go" screen. (For rendering)
	 * @param engine GameEngine
	 * @param playerID Player ID
	 */
	open fun renderReady(engine:GameEngine, playerID:Int) {}

	/** It will be called during the piece movement. (For rendering)
	 * @param engine GameEngine
	 * @param playerID Player ID
	 */
	open fun renderMove(engine:GameEngine, playerID:Int) {}

	/** It will be called during the "ARE". (For rendering)
	 * @param engine GameEngine
	 * @param playerID Player ID
	 */
	open fun renderARE(engine:GameEngine, playerID:Int) {}

	/** It will be called during the "Ending start" screen. (For rendering)
	 * @param engine GameEngine
	 * @param playerID Player ID
	 */
	open fun renderEndingStart(engine:GameEngine, playerID:Int) {}

	/** It will be called during the "Custom" screen. (For rendering)
	 * @param engine GameEngine
	 * @param playerID Player ID
	 */
	open fun renderCustom(engine:GameEngine, playerID:Int) {}

	/** It will be called during the "EXCELLENT!" screen. (For rendering)
	 * @param engine GameEngine
	 * @param playerID Player ID
	 */
	open fun renderExcellent(engine:GameEngine, playerID:Int) {}

	/** It will be called during the Game Over screen. (For rendering)
	 * @param engine GameEngine
	 * @param playerID Player ID
	 */
	open fun renderGameOver(engine:GameEngine, playerID:Int) {}

	/** It will be called during the end-of-game stats screen. (For rendering)
	 * @param engine GameEngine
	 * @param playerID Player ID
	 */
	open fun renderResult(engine:GameEngine, playerID:Int) {}

	/** It will be called during the field editor screen. (For rendering)
	 * @param engine GameEngine
	 * @param playerID Player ID
	 */
	open fun renderFieldEdit(engine:GameEngine, playerID:Int) {}

	/** It will be called if the player's input is being displayed. (For
	 * rendering)
	 * @param engine GameEngine
	 * @param playerID Player ID
	 */
	open fun renderInput(engine:GameEngine, playerID:Int) {}

	/** It will be called during the line clear. (For rendering)
	 * @param engine GameEngine
	 * @param playerID Player ID
	 */
	open fun renderLineClear(engine:GameEngine, playerID:Int) {}

	/** It will be called during the "Lock flash". (For rendering)
	 * @param engine GameEngine
	 * @param playerID Player ID
	 */
	open fun renderLockFlash(engine:GameEngine, playerID:Int) {}

	/** It will be called when a block is cleared.
	 * @param engine GameEngine
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param blk Block
	 */
	open fun blockBreak(engine:GameEngine, x:Int, y:Int, blk:Block) {}

	@Deprecated("playerID noEffect", ReplaceWith("blockBreak(engine,x,y,blk)"))
	fun blockBreak(engine:GameEngine, playerID:Int = engine.playerID, x:Int, y:Int, blk:Block) = blockBreak(engine, x, y, blk)
	/** It will be called when the game mode is going to calculate score.
	 * Please note it will be called even if no lines are cleared.
	 * @param engine GameEngine
	 * @param event Event the score gained
	 */
	open fun calcScore(engine:GameEngine, event:ScoreEvent?) {}

	/** After Soft Drop is used
	 * @param engine GameEngine
	 * @param playerID Player ID
	 * @param fall Number of rows the piece falled by Soft Drop
	 */
	open fun afterSoftDropFall(engine:GameEngine, playerID:Int, fall:Int) {}

	/** After Hard Drop is used
	 * @param engine GameEngine
	 * @param playerID Player ID
	 * @param fall Number of rows the piece falled by Hard Drop
	 */
	open fun afterHardDropFall(engine:GameEngine, playerID:Int, fall:Int) {}

	/** It will be called when the player exit the field editor.
	 * @param engine GameEngine
	 * @param playerID Player ID
	 */
	open fun fieldEditExit(engine:GameEngine, playerID:Int) {}

	/** It will be called when the piece has locked. (after calcScore)
	 * @param engine GameEngine
	 * @param playerID Player ID
	 * @param lines Number of lines to be cleared (can be 0)
	 */
	open fun pieceLocked(engine:GameEngine, playerID:Int, lines:Int) {}

	/** It will be called at the end of line-clear phase.
	 * @param engine GameEngine
	 * @param playerID Player ID
	 */
	open fun lineClearEnd(engine:GameEngine, playerID:Int) {}

	/** Called when saving replay into [prop]
	 * @param owner GameManager
	 * @param foldername Replay folder name
	 */
	open fun saveReplay(owner:GameManager, prop:CustomProperties, foldername:String = "replay") {
		if(owner.mode?.isNetplayMode!=false) return
		val folder = "$foldername/${owner.mode?.javaClass?.simpleName ?: ""}"
		val filename = "$folder/"+
			prop.getProperty("name.rule").lowercase().toReplayFilename
				.replace("[\\s-]".toRegex(), "_")
		try {
			val repFolder = File(folder)
			if(!repFolder.exists())
				if(repFolder.mkdirs()) log.info("Created replay folder: $folder")
				else log.error("Couldn't create replay folder at $folder")

			val out = GZIPOutputStream(FileOutputStream(filename))
			prop.store(out, "NullpoMino Replay")
			out.close()
			log.info("Saved replay file: $filename")
		} catch(e:IOException) {
			log.error("Couldn't save replay file to $filename", e)
		}

	}

	companion object {
		/** cell and block size(block,font) */
		const val BS = 16

		/** Log */
		internal val log = LogManager.getLogger()

		fun getRainbowColor(i:Int):COLOR = when(i%9) {
			0 -> COLOR.RED
			1 -> COLOR.ORANGE
			2 -> COLOR.YELLOW
			3 -> COLOR.GREEN
			4 -> COLOR.CYAN
			5 -> COLOR.BLUE
			6 -> COLOR.COBALT
			7 -> COLOR.PURPLE
			8 -> COLOR.PINK
			else -> COLOR.WHITE
		}

		fun getPlayerColor(playerID:Int):COLOR = when(playerID) {
			0 -> COLOR.BLUE
			1 -> COLOR.RED
			2 -> COLOR.GREEN
			3 -> COLOR.PURPLE
			4 -> COLOR.ORANGE
			5 -> COLOR.WHITE
			else -> COLOR.values().let {it.getOrElse((playerID-5)%it.size) {COLOR.WHITE}}
		}

		fun getBlockColor(engine:GameEngine, piece:Piece.Shape):COLOR =
			getBlockColor(Block.intToColor(engine.ruleOpt.pieceColor[piece.ordinal]))

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
		val NEW_FIELD_OFFSET_X = arrayOf(
			arrayOf(// TETROMINO
				intArrayOf(119, 247, 375, 503, 247, 375), // Small
				intArrayOf(32, 432, 432, 432, 432, 432), // Normal
				intArrayOf(16, 416, 416, 416, 416, 416)
			)// Big
			, arrayOf(// AVALANCHE
				intArrayOf(119, 247, 375, 503, 247, 375), // Small
				intArrayOf(32, 432, 432, 432, 432, 432), // Normal
				intArrayOf(16, 352, 352, 352, 352, 352)
			)// Big
			, arrayOf(// PHYSICIAN
				intArrayOf(119, 247, 375, 503, 247, 375), // Small
				intArrayOf(32, 432, 432, 432, 432, 432), // Normal
				intArrayOf(16, 416, 416, 416, 416, 416)
			)// Big
			, arrayOf(// SPF
				intArrayOf(119, 247, 375, 503, 247, 375), // Small
				intArrayOf(32, 432, 432, 432, 432, 432), // Normal
				intArrayOf(16, 352, 352, 352, 352, 352)
			)// Big
		)

		/** Field Y position */
		val NEW_FIELD_OFFSET_Y = arrayOf(
			arrayOf(// TETROMINO
				intArrayOf(80, 80, 80, 80, 286, 286), // Small
				intArrayOf(32, 32, 32, 32, 32, 32), // Normal
				intArrayOf(8, 8, 8, 8, 8, 8)
			)// Big
			, arrayOf(// AVALANCHE
				intArrayOf(80, 80, 80, 80, 286, 286), // Small
				intArrayOf(32, 32, 32, 32, 32, 32), // Normal
				intArrayOf(8, 8, 8, 8, 8, 8)
			)// Big
			, arrayOf(// PHYSICIAN
				intArrayOf(80, 80, 80, 80, 286, 286), // Small
				intArrayOf(32, 32, 32, 32, 32, 32), // Normal
				intArrayOf(8, 8, 8, 8, 8, 8)
			)// Big
			, arrayOf(// SPF
				intArrayOf(80, 80, 80, 80, 286, 286), // Small
				intArrayOf(32, 32, 32, 32, 32, 32), // Normal
				intArrayOf(-8, -8, -8, -8, -8, -8)
			)// Big
		)

		/** Field X position (Big side preview) */
		val NEW_FIELD_OFFSET_X_BSP = arrayOf(
			arrayOf(// TETROMINO
				intArrayOf(208, 320, 432, 544, 320, 432), // Small
				intArrayOf(64, 400, 400, 400, 400, 400), // Normal
				intArrayOf(16, 352, 352, 352, 352, 352)
			)// Big
			, arrayOf(// AVALANCHE
				intArrayOf(208, 320, 432, 544, 320, 432), // Small
				intArrayOf(64, 400, 400, 400, 400, 400), // Normal
				intArrayOf(16, 352, 352, 352, 352, 352)
			)// Big
			, arrayOf(// PHYSICIAN
				intArrayOf(208, 320, 432, 544, 320, 432), // Small
				intArrayOf(64, 400, 400, 400, 400, 400), // Normal
				intArrayOf(16, 352, 352, 352, 352, 352)
			)// Big
			, arrayOf(// SPF
				intArrayOf(208, 320, 432, 544, 320, 432), // Small
				intArrayOf(64, 400, 400, 400, 400, 400), // Normal
				intArrayOf(16, 352, 352, 352, 352, 352)
			)// Big
		)

		/** Field Y position (Big side preview) */
		val NEW_FIELD_OFFSET_Y_BSP = arrayOf(
			arrayOf(// TETROMINO
				intArrayOf(80, 80, 80, 80, 286, 286), // Small
				intArrayOf(32, 32, 32, 32, 32, 32), // Normal
				intArrayOf(8, 8, 8, 8, 8, 8)
			)// Big
			, arrayOf(// AVALANCHE
				intArrayOf(80, 80, 80, 80, 286, 286), // Small
				intArrayOf(32, 32, 32, 32, 32, 32), // Normal
				intArrayOf(8, 8, 8, 8, 8, 8)
			)// Big
			, arrayOf(// PHYSICIAN
				intArrayOf(80, 80, 80, 80, 286, 286), // Small
				intArrayOf(32, 32, 32, 32, 32, 32), // Normal
				intArrayOf(8, 8, 8, 8, 8, 8)
			)// Big
			, arrayOf(// SPF
				intArrayOf(80, 80, 80, 80, 286, 286), // Small
				intArrayOf(32, 32, 32, 32, 32, 32), // Normal
				intArrayOf(-16, -16, -16, -16, -16, -16)
			)// Big
		)

	}

}
