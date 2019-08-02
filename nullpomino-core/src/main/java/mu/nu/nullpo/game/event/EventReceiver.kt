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
package mu.nu.nullpo.game.event

import mu.nu.nullpo.game.component.Block
import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.game.play.GameManager
import mu.nu.nullpo.util.CustomProperties
import mu.nu.nullpo.util.GeneralUtil
import org.apache.log4j.Logger
import java.io.*
import java.util.*
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import kotlin.math.sqrt

/** Drawing and event handling EventReceiver */
open class EventReceiver {

	/** Font cint constants */
	enum class FONT { NORMAL, NANO, NUM, GRADE, GRADE_BIG, MEDAL, TTF; }

	/** Font cint constants */
	enum class COLOR { WHITE, BLUE, RED, PINK, GREEN, YELLOW, CYAN, ORANGE, PURPLE, COBALT; }

	/** Background display */
	protected var showbg:Boolean = false

	/** Show meter */
	protected var showmeter:Boolean = false

	/** Outline ghost piece */
	protected var outlineghost:Boolean = false

	/** Piece previews on sides */
	protected var sidenext:Boolean = false

	/** Use bigger side previews */
	protected var bigsidenext:Boolean = false

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

	/** It will be called when a fireworks shoots.
	 * @param engine GameEngine
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param color Color
	 */
	open fun shootFireworks(engine:GameEngine, x:Int, y:Int, color:COLOR) {
		playSE("firecracker"+(engine.random.nextInt(2)+1))
	}

	/** It will be called when a fireworks shoots
	 * @param engine GameEngine
	 * @param playerID Player ID
	 */
	fun shootFireworks(engine:GameEngine, playerID:Int) {
		val mx = engine.fieldWidth*16
		val my = engine.fieldHeight/2*16
		val x = fieldX(engine, playerID)+engine.random.nextInt(mx)
		var y = fieldY(engine, playerID)+engine.random.nextInt(my)+50
		engine.field?.let {if(my<it.highestBlockY) y += my}
		shootFireworks(engine, x, y, COLOR.values()[engine.random.nextInt(7)])

	}

	open fun bravo(gameEngine:GameEngine, playerID:Int) {
		playSE("bravo")
	}

	/** Draw String to any location.
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param str String to draw
	 * @param color Font cint
	 * @param scale Font size (.5f, 1f, 2f)
	 */
	open fun drawDirect(x:Int, y:Int, str:String, font:FONT = FONT.NORMAL,
		color:COLOR = COLOR.WHITE, scale:Float = 1f) {
	}

	fun drawDirectFont(x:Int, y:Int, str:String, color:COLOR = COLOR.WHITE, scale:Float = 1f) =
		drawDirect(x, y, str, FONT.NORMAL, color, scale)

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
		drawDirectFont(x, y, str, color = if(flag) (if(playerID%2==0) COLOR.RED else COLOR.BLUE) else COLOR.WHITE, scale = scale)

	/** [You don't have to override this]
	 * Draw Number to any location.
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param str String to draw
	 * @param color Font cint
	 * @param scale Font size (.5f, 1f, 2f)
	 */
	fun drawDirectNum(x:Int, y:Int, str:String, color:COLOR = COLOR.WHITE, scale:Float = 1f) =
		drawDirect(x, y, str, FONT.NUM, color, scale)

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
		drawDirectNum(x, y, str, if(flag) if(playerID%2==0) COLOR.RED else COLOR.BLUE else COLOR.WHITE, scale)

	/** [You don't have to override this]
	 * Draw Grade to any location.
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param str String to draw
	 * @param color Font cint
	 * @param scale Font size (.5f, 1f, 2f)
	 */
	fun drawDirectGrade(x:Int, y:Int, str:String, color:COLOR = COLOR.WHITE, scale:Float = 1f) =
		drawDirect(x, y, str, FONT.GRADE, color, scale)

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
		drawDirectGrade(x, y, str, if(flag) if(playerID%2==0) COLOR.RED else COLOR.BLUE else COLOR.WHITE, scale)

	/** [YOu don't have to override this]
	 * Draw TTF String to any location.
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param str String to draw
	 * @param color Font cint
	 */
	open fun drawDirectTTF(x:Int, y:Int, str:String, color:COLOR = COLOR.WHITE) {
		drawDirect(x, y, str, FONT.TTF, color)
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
		drawDirectTTF(x, y, str, if(flag) if(playerID%2==0) COLOR.RED else COLOR.BLUE else COLOR.WHITE)
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
	open fun drawMenu(engine:GameEngine, playerID:Int, x:Int, y:Int, str:String,
		font:FONT = FONT.NORMAL, color:COLOR = COLOR.WHITE, scale:Float = 1f) {
	}

	fun drawMenuFont(engine:GameEngine, playerID:Int, x:Int, y:Int, str:String, color:COLOR = COLOR.WHITE,
		scale:Float = 1f) =
		drawMenu(engine, playerID, x, y, str, FONT.NORMAL, color, scale)

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
		drawMenuFont(engine, playerID, x, y, str, color = if(flag) if(playerID%2==0) COLOR.RED else COLOR.BLUE else COLOR.WHITE, scale = scale)

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
		drawMenuNum(engine, playerID, x, y, str, if(flag) if(playerID%2==0) COLOR.RED else COLOR.BLUE else COLOR.WHITE, scale)

	fun drawMenuNano(engine:GameEngine, playerID:Int, x:Int, y:Int, str:String, color:COLOR = COLOR.WHITE,
		scale:Float = 1f) =
		drawMenu(engine, playerID, x, y, str, FONT.NANO, color, scale)

	fun drawMenuNano(engine:GameEngine, playerID:Int, x:Int, y:Int, str:String, flag:Boolean, scale:Float = 1f) =
		drawMenuNano(engine, playerID, x, y, str, if(flag) if(playerID%2==0) COLOR.YELLOW else COLOR.ORANGE else COLOR.WHITE, scale)

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
		drawMenuGrade(engine, playerID, x, y, str, if(flag) if(playerID%2==0) COLOR.RED else COLOR.BLUE else COLOR.WHITE, scale)

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
		drawMenuTTF(engine, playerID, x, y, str, if(flag) if(playerID%2==0) COLOR.RED else COLOR.BLUE else COLOR.WHITE)

	/** Draw String to score display area.
	 * @param engine GameEngine
	 * @param playerID Player ID
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param str String to draw
	 * @param color Font cint
	 * @param scale Font size (.5f, 1f, 2f)
	 */
	open fun drawScore(engine:GameEngine, playerID:Int, x:Int, y:Int, str:String,
		font:FONT = FONT.NORMAL, color:COLOR = COLOR.WHITE, scale:Float = 1f) {
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
	fun drawScoreFont(engine:GameEngine, playerID:Int, x:Int, y:Int, str:String, color:COLOR = COLOR.WHITE,
		scale:Float = 1f) =
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
		drawScore(engine, playerID, x, y, str, font, color = if(flag) (if(playerID%2==0) COLOR.YELLOW else COLOR.ORANGE) else COLOR.WHITE, scale = scale)

	fun drawScoreFont(engine:GameEngine, playerID:Int, x:Int, y:Int, str:String, flag:Boolean, scale:Float = 1f) =
		drawScore(engine, playerID, x, y, str, FONT.NORMAL, flag = flag, scale = scale)

	fun drawScoreNum(engine:GameEngine, playerID:Int, x:Int, y:Int, str:String, color:COLOR = COLOR.WHITE,
		scale:Float = 1f) =
		drawScore(engine, playerID, x, y, str, FONT.NUM, color, scale)

	fun drawScoreNum(engine:GameEngine, playerID:Int, x:Int, y:Int, str:String, scale:Float = 1f) =
		drawScore(engine, playerID, x, y, str, FONT.NUM, scale = scale)

	fun drawScoreNum(engine:GameEngine, playerID:Int, x:Int, y:Int, str:String, flag:Boolean, scale:Float = 1f) =
		drawScoreNum(engine, playerID, x, y, str, if(flag) if(playerID%2==0) COLOR.YELLOW else COLOR.ORANGE else COLOR.WHITE, scale)

	fun drawScoreNano(engine:GameEngine, playerID:Int, x:Int, y:Int, str:String, color:COLOR = COLOR.WHITE,
		scale:Float = 1f) =
		drawScore(engine, playerID, x, y, str, FONT.NANO, color, scale)

	fun drawScoreNano(engine:GameEngine, playerID:Int, x:Int, y:Int, str:String, flag:Boolean, scale:Float = 1f) =
		drawScoreNano(engine, playerID, x, y, str, if(flag) if(playerID%2==0) COLOR.YELLOW else COLOR.ORANGE else COLOR.WHITE, scale)

	fun drawScoreGrade(engine:GameEngine, playerID:Int, x:Int, y:Int, str:String, color:COLOR = COLOR.WHITE,
		scale:Float = 1f) =
		drawScore(engine, playerID, x, y, str, FONT.GRADE, color, scale)

	fun drawScoreGrade(engine:GameEngine, playerID:Int, x:Int, y:Int, str:String, flag:Boolean, scale:Float = 1f) =
		drawScoreGrade(engine, playerID, x, y, str, if(flag) if(playerID%2==0) COLOR.YELLOW else COLOR.ORANGE else COLOR.WHITE, scale)

	fun drawScoreTTF(engine:GameEngine, playerID:Int, x:Int, y:Int, str:String, color:COLOR = COLOR.WHITE) =
		drawScore(engine, playerID, x, y, str, FONT.TTF, color)

	fun drawScoreTTF(engine:GameEngine, playerID:Int, x:Int, y:Int, str:String, flag:Boolean) =
		drawScoreTTF(engine, playerID, x, y, str, if(flag) if(playerID%2==0) COLOR.RED else COLOR.BLUE else COLOR.WHITE)

	/** Draw a block
	 * @param engine GameEngine
	 * @param playerID Player ID
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param color Block cint
	 * @param skin Block skin
	 * @param bone When true, it will use [] (bone) blocks
	 * @param darkness Brightness
	 * @param alpha Alpha-blending
	 * @param scale Size (.5f, 1f, 2f)
	 */
	open fun drawSingleBlock(engine:GameEngine, playerID:Int, x:Int, y:Int, color:Int, skin:Int, bone:Boolean,
		darkness:Float, alpha:Float, scale:Float) {
	}

	/** Get key name by button ID
	 * @param engine GameEngine
	 * @param btnID Button ID
	 * @return Key name
	 */
	open fun getKeyNameByButtonID(engine:GameEngine, btnID:Int):String = ""

	/** Draw speed meter.
	 * @param engine GameEngine
	 * @param playerID Player ID
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param s Speed (float:0.0~1.0 int:0~40)
	 */
	open fun drawSpeedMeter(engine:GameEngine, playerID:Int, x:Int, y:Int, s:Float) {}

	fun drawSpeedMeter(engine:GameEngine, playerID:Int, x:Int, y:Int, s:Int) = drawSpeedMeter(engine, playerID, x, y, s/40f)

	fun drawSpeedMeter(engine:GameEngine, playerID:Int, x:Int, y:Int, g:Int, d:Int) {
		var s = if(g<=0) 1f else 0f
		if(g>0&&d>0) s = (sqrt((g.toFloat()/d).toDouble())/sqrt(20.0)).toFloat()
		drawSpeedMeter(engine, playerID, x, y, s)
	}

	/** Get maximum length of the meter.
	 * @param engine GameEngine
	 * @return Maximum length of the meter
	 */
	fun getMeterMax(engine:GameEngine):Int {
		if(!showmeter) return 0
		val blksize = if(engine.displaysize==1) 32 else if(engine.displaysize==-1) 8 else 16

		return engine.fieldHeight*blksize
	}

	/** Draw Decorations at score pos
	 * @param engine GameEngine
	 * @param playerID Player ID
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param scale size
	 */
	open fun drawScoreBadges(engine:GameEngine, playerID:Int, x:Int, y:Int,
		width:Int = 0, nums:Int, scale:Float = 1f) {
	}

	/** Draw Decorations at menu pos
	 * @param engine GameEngine
	 * @param playerID Player ID
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param scale size
	 */
	open fun drawMenuBadges(engine:GameEngine, playerID:Int, x:Int, y:Int, nums:Int, scale:Float = 1f) {}

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
	open fun drawScoreMedal(engine:GameEngine, playerID:Int, x:Int, y:Int, str:String, tier:Int, scale:Float = 1f) {
	}

	/** Draw Medal at menu pos
	 * @param engine GameEngine
	 * @param playerID Player ID
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param scale size
	 */
	open fun drawMenuMedal(engine:GameEngine, playerID:Int, x:Int, y:Int, str:String, tier:Int, scale:Float = 1f) {
	}

	/** Draw Medal
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param tier medal Tier
	 * @param scale size
	 */
	open fun drawMedal(x:Int, y:Int, str:String, tier:Int, scale:Float = 1f) {}

	/** Get width of block image.
	 * @param engine GameEngine
	 * @return Width of block image
	 */
	fun getBlockGraphicsWidth(engine:GameEngine):Int = when {
		engine.displaysize==-1 -> 8
		engine.displaysize==1 -> 32
		else -> 16
	}

	/** Get height of block image.
	 * @param engine GameEngine
	 * @return Height of block image
	 */
	fun getBlockGraphicsHeight(engine:GameEngine):Int = when {
		engine.displaysize==-1 -> 8
		engine.displaysize==1 -> 32
		else -> 16
	}

	/** Get X position of field
	 * @param engine GameEngine
	 * @param playerID Player ID
	 * @return X position of field
	 */
	fun fieldX(engine:GameEngine, playerID:Int):Int {
		engine.owner.mode?.let {
			return if(nextDisplayType==2) NEW_FIELD_OFFSET_X_BSP[it.gameStyle][engine.displaysize+1][playerID]
			else NEW_FIELD_OFFSET_X[it.gameStyle][engine.displaysize+1][playerID]
		} ?: return 0
	}

	/** Get Y position of field
	 * @param engine GameEngine
	 * @param playerID Player ID
	 * @return Y position of field
	 */
	fun fieldY(engine:GameEngine, playerID:Int):Int {
		engine.owner.mode?.let {
			return if(nextDisplayType==2) NEW_FIELD_OFFSET_Y_BSP[it.gameStyle][engine.displaysize+1][playerID]
			else NEW_FIELD_OFFSET_Y[it.gameStyle][engine.displaysize+1][playerID]
		} ?: return 0
	}

	/** Get X position of score display area
	 * @param engine GameEngine
	 * @param playerID Player ID
	 * @return X position of score display area
	 */
	fun scoreX(engine:GameEngine, playerID:Int):Int =
		fieldX(engine, playerID)+(if(nextDisplayType==2) 256 else 216) + if(engine.displaysize==1) 32 else 0


	/** Get Y position of score display area
	 * @param engine GameEngine
	 * @param playerID Player ID
	 * @return Y position of score display area
	 */
	fun scoreY(engine:GameEngine, playerID:Int):Int = fieldY(engine, playerID)+48

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
	open fun playSE(name:String) {}

	open fun playSE(name:String, freq:Float) {}
	open fun loopSE(name:String) {}
	open fun stopSE(name:String) {}

	/** Set Graphics context
	 * @param g Graphics context
	 */
	open fun setGraphics(g:Any) {}

	/** Load properties from "config/setting/mode.cfg"
	 * @return Properties from "config/setting/mode.cfg". null if load fails.
	 */
	fun loadModeConfig():CustomProperties? {
		val propModeConfig = CustomProperties()

		try {
			val `in` = GZIPInputStream(FileInputStream("config/setting/mode.cfg"))
			propModeConfig.load(`in`)
			`in`.close()
		} catch(e:IOException) {
			return null
		}

		return propModeConfig
	}

	/** Save properties to "config/setting/mode.cfg"
	 * @param modeConfig Properties you want to save
	 */
	fun saveModeConfig(modeConfig:CustomProperties) {
		try {
			val out = GZIPOutputStream(FileOutputStream("config/setting/mode.cfg"))
			modeConfig.store(out, "NullpoMino Mode Config")
			out.close()
		} catch(e:IOException) {
			log.error("Failed to save mode config", e)
		}

	}

	/** Load any properties from any location.
	 * @param filename Filename
	 * @return Properties you specified, or null if the file doesn't exist.
	 */
	fun loadProperties(filename:String):CustomProperties? {
		val prop = CustomProperties()

		try {
			val `in` = GZIPInputStream(FileInputStream(filename))
			prop.load(`in`)
			`in`.close()
		} catch(e:IOException) {
			log.debug("Failed to load custom property file from $filename", e)
			return null
		}

		return prop
	}

	/** Save any properties to any location.
	 * @param filename Filename
	 * @param prop Properties you want to save
	 * @return true if success
	 */
	fun saveProperties(filename:String, prop:CustomProperties):Boolean {
		try {
			val out = GZIPOutputStream(FileOutputStream(filename))
			prop.store(out, "NullpoMino Custom Property File")
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

	/** It will be called during the ARE.
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

	/** It will be called during the ARE. (For rendering)
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
	 * @param playerID Player ID
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param blk Block
	 */
	open fun blockBreak(engine:GameEngine, playerID:Int, x:Int, y:Int, blk:Block) {}

	/** It will be called when the game mode is going to calculate score.
	 * Please note it will be called even if no lines are cleared.
	 * @param engine GameEngine
	 * @param playerID Player ID
	 * @param lines Number of lines cleared (0 if no line clear happened)
	 */
	open fun calcScore(engine:GameEngine, playerID:Int, lines:Int) {}

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

	/** Called when saving replay
	 * @param owner GameManager
	 * @param prop CustomProperties where the replay is going to stored
	 */
	open fun saveReplay(owner:GameManager, prop:CustomProperties) {
		saveReplay(owner, prop, "replay")
	}

	/** Called when saving replay (This is main body)
	 * @param owner GameManager
	 * @param prop CustomProperties where the replay is going to stored
	 * @param foldername Replay folder name
	 */
	fun saveReplay(owner:GameManager, prop:CustomProperties, foldername:String) {
		var foldername = foldername
		if(owner.mode?.isNetplayMode!=false) return
		foldername = foldername+"/"+owner.mode?.javaClass!!.simpleName
		val filename =
			foldername+"/"+GeneralUtil.getReplayFilename(prop.getProperty("name.rule")).toLowerCase(Locale.ROOT).replace("[\\s-]".toRegex(), "_")
		try {
			val repfolder = File(foldername)
			if(!repfolder.exists())
				if(repfolder.mkdirs())
					log.info("Created replay folder: $foldername")
				else
					log.info("Couldn't create replay folder at $foldername")

			val out = GZIPOutputStream(FileOutputStream(filename))
			prop.store(out, "NullpoMino Replay")
			out.close()
			log.info("Saved replay file: $filename")
		} catch(e:IOException) {
			log.error("Couldn't save replay file to $filename", e)
		}

	}

	companion object {
		/** Log */
		internal val log = Logger.getLogger(EventReceiver::class.java)

		/** Field X position */
		val NEW_FIELD_OFFSET_X = arrayOf(arrayOf(// TETROMINO
			intArrayOf(119, 247, 375, 503, 247, 375), // Small
			intArrayOf(32, 432, 432, 432, 432, 432), // Normal
			intArrayOf(16, 416, 416, 416, 416, 416))// Big
			, arrayOf(// AVALANCHE
			intArrayOf(119, 247, 375, 503, 247, 375), // Small
			intArrayOf(32, 432, 432, 432, 432, 432), // Normal
			intArrayOf(16, 352, 352, 352, 352, 352))// Big
			, arrayOf(// PHYSICIAN
			intArrayOf(119, 247, 375, 503, 247, 375), // Small
			intArrayOf(32, 432, 432, 432, 432, 432), // Normal
			intArrayOf(16, 416, 416, 416, 416, 416))// Big
			, arrayOf(// SPF
			intArrayOf(119, 247, 375, 503, 247, 375), // Small
			intArrayOf(32, 432, 432, 432, 432, 432), // Normal
			intArrayOf(16, 352, 352, 352, 352, 352))// Big
		)
		/** Field Y position */
		val NEW_FIELD_OFFSET_Y = arrayOf(arrayOf(// TETROMINO
			intArrayOf(80, 80, 80, 80, 286, 286), // Small
			intArrayOf(32, 32, 32, 32, 32, 32), // Normal
			intArrayOf(8, 8, 8, 8, 8, 8))// Big
			, arrayOf(// AVALANCHE
			intArrayOf(80, 80, 80, 80, 286, 286), // Small
			intArrayOf(32, 32, 32, 32, 32, 32), // Normal
			intArrayOf(8, 8, 8, 8, 8, 8))// Big
			, arrayOf(// PHYSICIAN
			intArrayOf(80, 80, 80, 80, 286, 286), // Small
			intArrayOf(32, 32, 32, 32, 32, 32), // Normal
			intArrayOf(8, 8, 8, 8, 8, 8))// Big
			, arrayOf(// SPF
			intArrayOf(80, 80, 80, 80, 286, 286), // Small
			intArrayOf(32, 32, 32, 32, 32, 32), // Normal
			intArrayOf(-8, -8, -8, -8, -8, -8))// Big
		)

		/** Field X position (Big side preview) */
		val NEW_FIELD_OFFSET_X_BSP = arrayOf(arrayOf(// TETROMINO
			intArrayOf(208, 320, 432, 544, 320, 432), // Small
			intArrayOf(64, 400, 400, 400, 400, 400), // Normal
			intArrayOf(16, 352, 352, 352, 352, 352))// Big
			, arrayOf(// AVALANCHE
			intArrayOf(208, 320, 432, 544, 320, 432), // Small
			intArrayOf(64, 400, 400, 400, 400, 400), // Normal
			intArrayOf(16, 352, 352, 352, 352, 352))// Big
			, arrayOf(// PHYSICIAN
			intArrayOf(208, 320, 432, 544, 320, 432), // Small
			intArrayOf(64, 400, 400, 400, 400, 400), // Normal
			intArrayOf(16, 352, 352, 352, 352, 352))// Big
			, arrayOf(// SPF
			intArrayOf(208, 320, 432, 544, 320, 432), // Small
			intArrayOf(64, 400, 400, 400, 400, 400), // Normal
			intArrayOf(16, 352, 352, 352, 352, 352))// Big
		)
		/** Field Y position (Big side preview) */
		val NEW_FIELD_OFFSET_Y_BSP = arrayOf(arrayOf(// TETROMINO
			intArrayOf(80, 80, 80, 80, 286, 286), // Small
			intArrayOf(32, 32, 32, 32, 32, 32), // Normal
			intArrayOf(8, 8, 8, 8, 8, 8))// Big
			, arrayOf(// AVALANCHE
			intArrayOf(80, 80, 80, 80, 286, 286), // Small
			intArrayOf(32, 32, 32, 32, 32, 32), // Normal
			intArrayOf(8, 8, 8, 8, 8, 8))// Big
			, arrayOf(// PHYSICIAN
			intArrayOf(80, 80, 80, 80, 286, 286), // Small
			intArrayOf(32, 32, 32, 32, 32, 32), // Normal
			intArrayOf(8, 8, 8, 8, 8, 8))// Big
			, arrayOf(// SPF
			intArrayOf(80, 80, 80, 80, 286, 286), // Small
			intArrayOf(32, 32, 32, 32, 32, 32), // Normal
			intArrayOf(-16, -16, -16, -16, -16, -16))// Big
		)

	}

}