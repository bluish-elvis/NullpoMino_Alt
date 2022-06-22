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

package mu.nu.nullpo.gui.common

import mu.nu.nullpo.game.component.Block
import mu.nu.nullpo.game.component.Piece
import mu.nu.nullpo.game.event.EventReceiver
import mu.nu.nullpo.game.event.ScoreEvent
import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.gui.common.FragAnim.ANIM
import mu.nu.nullpo.util.GeneralUtil.toInt

abstract class AbstractRenderer:EventReceiver() {

	internal abstract val resources:ResourceHolder

	/** 演出オブジェクト */
	internal val effects:ArrayList<EffectObject> = ArrayList(10*4)

	/** Line clearエフェクト表示 */
	protected var showlineeffect = false

	/** 重い演出を使う */
	protected var heavyeffect = false

	/** fieldBackgroundの明るさ */
	protected var fieldbgbright:Float = .5f

	/** Show field BG grid */
	protected var showfieldbggrid = false

	/** NEXT欄を暗くする */
	protected var darknextarea = false

	/** ghost ピースの上にNEXT表示 */
	protected var nextshadow = false

	/** Line clear effect speed */
	protected var lineeffectspeed = 1

	/** 回転軸を表示する */
	protected var showCenter = false

	/** 操作ブロック降下を滑らかにする */
	protected var smoothfall = false

	/** 高速落下時の軌道を表示する */
	protected var showLocus = false

	override fun drawFont(x:Int, y:Int, str:String, font:FONT, color:COLOR, scale:Float, alpha:Float) {
		if(font==FONT.TTF) printTTFSpecific(x, y, str, color, alpha)
		else printFontSpecific(x, y, str, font, color, scale, alpha)
	}

	var rainbow = 0

	/** Draw a block
	 * @param x X pos
	 * @param y Y pos
	 * @param color Color number for render
	 * @param skin Skin
	 * @param bone true to use bone block ([][][][])
	 * @param darkness Darkness or brightness
	 * @param alpha Alpha
	 * @param scale Size (.5f, 1f, 2f)
	 * @param attr Attribute
	 */
	/* 1マスBlockを描画 */
	override fun drawBlock(
		x:Float, y:Float, color:Int, skin:Int, bone:Boolean, darkness:Float, alpha:Float, scale:Float,
		attr:Int, outline:Float
	) {
		var sk = skin

		if(!doesGraphicsExist()) return

		if(color<0) return
		if(sk>=resources.imgBlockListSize) sk = 0

		val isSpecialBlocks = color>=Block.COLOR.COUNT
		val isSticky = resources.getBlockIsSticky(sk)

		var sx = color
		if(bone) sx += 9
		var sy = 0
		if(isSpecialBlocks) sx = (color-Block.COLOR.COUNT+18)

		if(isSticky)
			if(isSpecialBlocks) {
				sx = (color-Block.COLOR.COUNT)
				sy = 18
			} else {
				sx = 0
				if(attr and Block.ATTRIBUTE.CONNECT_UP.bit>0) sx = sx or 0x1
				if(attr and Block.ATTRIBUTE.CONNECT_DOWN.bit>0) sx = sx or 0x2
				if(attr and Block.ATTRIBUTE.CONNECT_LEFT.bit>0) sx = sx or 0x4
				if(attr and Block.ATTRIBUTE.CONNECT_RIGHT.bit>0) sx = sx or 0x8
				sy = color
				if(bone) sy += 9
			}
		val ls = BS*scale
		drawBlockSpecific(x, y, sx, sy, sk, ls, darkness, alpha)
		if(outline>0) {
			if(attr and Block.ATTRIBUTE.CONNECT_UP.bit==0)
				drawLineSpecific(x, y, x+ls, y, w = outline)
			if(attr and Block.ATTRIBUTE.CONNECT_DOWN.bit==0)
				drawLineSpecific(x, y+ls, x+ls, y+ls, w = outline)
			if(attr and Block.ATTRIBUTE.CONNECT_LEFT.bit==0)
				drawLineSpecific(x, y, x, y+ls, w = outline)
			if(attr and Block.ATTRIBUTE.CONNECT_RIGHT.bit==0)
				drawLineSpecific(x+ls, y, x+ls, y+ls, w = outline)
		}
	}

	/* 勲章を描画 */
	override fun drawBadges(x:Int, y:Int, width:Int, nums:Int, scale:Float) {
		var n = nums
		var nx = x.toFloat()
		var ny = y.toFloat()
		var z:Int
		val b = FontBadge.b
		var mh = 0
		while(n>0) {
			z = 0
			while(z<b.size-1&&n>=b[z+1]) z++
			val w = FontBadge(z).w*scale
			val h = FontBadge(z).h
			if(nx+w>width) {
				nx = x.toFloat()
				ny += mh*scale
				mh = 0
			}
			if(h>mh) mh = h
			drawBadgesSpecific(nx, ny, z, scale)
			n -= b[z]
			nx += w
		}
	}

	class FontBadge(type:Int) {
		val type = maxOf(0, minOf(b.size-1, type))
		val sx = intArrayOf(0, 10, 20, 30, 0, 10, 20, 30, 0, 20, 40, 0)[type]
		val sy = intArrayOf(0, 0, 0, 0, 14, 14, 14, 14, 24, 24, 0, 44)[type]
		val w = intArrayOf(10, 10, 10, 10, 10, 10, 10, 10, 20, 20, 32, 64)[type]
		val h = intArrayOf(10, 10, 14, 14, 14, 14, 15, 15, 15, 15, 32, 48)[type]

		companion object {
			val b = intArrayOf(1, 5, 10, 50, 100, 500, 1000, 5000, 10000, 50000, 100000, 500000)
		}
	}

	protected abstract fun drawBG(engine:GameEngine)
	protected abstract fun drawFrameSpecific(x:Int, y:Int, engine:GameEngine, displaysize:Int)
	private fun drawFrame(x:Int, y:Int, engine:GameEngine, displaysize:Int) {
		// Upと下
		var tmpX = x
		var tmpY = y

		val s = 4*when(displaysize) {
			-1 -> 2
			1 -> 8
			else -> 4
		}

		val fieldW = engine.field.width// ?: Field.DEFAULT_WIDTH
		val fieldH = engine.field.height//?: Field.DEFAULT_HEIGHT
		val maxWidth = fieldW*s
		val maxHeight = fieldH*s
		val lX = x+4
		val rX = lX+maxWidth
		val tY = y+4
		val bY = tY+maxHeight
		// NEXT area background
		if(showbg&&darknextarea) {
			when(nextDisplayType) {
				0 -> {
					val w = maxWidth+15
					fillRectSpecific(x+20, y, w-40, -48, 0)
					for(i in 20 downTo 0)
						drawRectSpecific(x+20-i, y, w-40+i*2, -48-i, 0, 1-i/20f, 1)
				}
				1 -> {//side small
					val x2 = x+s+fieldW*s
					val maxNext = if(engine.isNextVisible) engine.ruleOpt.nextDisplay else 0

					// HOLD area
					if(engine.ruleOpt.holdEnable&&engine.isHoldVisible) {
						fillRectSpecific(x-32, y, 32, 64, 0)
						for(i in 8 downTo 0)
							drawRectSpecific(x-32-i, y-i, 33+i, 64+i*2, 0, 1-i/8f, 1)
					}

					// NEXT area
					if(maxNext>0) {
						fillRectSpecific(x2, y+8, 32, 32*maxNext-16, 0)
						for(i in 8 downTo 0)
							drawRectSpecific(x2-1, y+8-i, 33+i, 32*maxNext-16+i*20, 0, 1-i/8f, 1)
					}
				}
				2 -> {//side big
					val x2 = x+s+maxWidth
					val maxNext = if(engine.isNextVisible) engine.ruleOpt.nextDisplay else 0

					// HOLD area
					if(engine.ruleOpt.holdEnable&&engine.isHoldVisible) {
						fillRectSpecific(x-64, y, 64, 48, 0)
						for(i in 8 downTo 0)
							drawRectSpecific(x-64-i, y-i, 65+i, 48+i*2, 0, i/8f, 1)
					}
					// NEXT area
					if(maxNext>0) {
						drawRect(x2, y+8, 64, 64*maxNext-16, 0)
						for(i in 8 downTo 0)
							drawRectSpecific(x2-1, y+8-i, 65+i, 64*maxNext-16+i*2, 0, i/8f, 1)
					}
				}
			}
		}
// Field Background
		if(fieldbgbright>0)
			if(fieldW<=10&&fieldH<=20) {
				var img = resources.imgFieldBG[1]
				if(displaysize==-1) img = resources.imgFieldBG[0]
				if(displaysize==1) img = resources.imgFieldBG[2]
				img.draw(lX, tY, rX, bY, 0, 0, maxWidth, maxHeight, fieldbgbright)
			} else if(showbg) drawRect(lX, tY, maxWidth, maxHeight, 0, fieldbgbright)

		drawFrameSpecific(x, y, engine, displaysize)

		when(engine.framecolor) {
			GameEngine.FRAME_SKIN_GB -> {
				drawRect(x+4, y+4, fieldW*s, fieldH*s, 0x888888)
				val fi = resources.imgFrameOld[0]
				tmpX -= 12
				for(i in 0..fieldH) {

					tmpY = y+i*s+4
					fi.draw(tmpX, tmpY, (tmpX+s), (tmpY+s), 0, 0, 16, 16)
					fi.draw((tmpX+maxWidth+s), tmpY, (tmpX+maxWidth+s*2), (tmpY+s), 0, 0, 16, 16)
					if(i==fieldH)
						for(z in 1..fieldW)
							fi.draw((tmpX+z*s), tmpY, (tmpX+(z+1)*s), (tmpY+s), 0, 0, 16, 16)
				}
			}
			GameEngine.FRAME_SKIN_SG -> {
				drawRect(x+4, y+4, fieldW*s, fieldH*s, 0)

				val fi = resources.imgFrameOld[1]
				val mW = maxWidth+s*2
				fi.draw(x-12, y-12, x-12+mW/2, y-12+(fieldH+2)*s, 0, 0, 96, 352)
				fi.draw(x-12+mW, y-12, x-12+mW/2, y-12+(fieldH+2)*s, 0, 0, 96, 352)
			}
			GameEngine.FRAME_SKIN_HEBO -> {
				val fi = resources.imgFrameOld[2]

				//Top

				fi.draw(lX-s, tY-s, lX+s, tY, 0, 0, 32, 16)
				fi.draw(rX+s, tY-s, rX-s, tY, 0, 0, 32, 16)

				fi.draw(lX+s, tY-s, rX-s, tY, 32, 0, 48, 16)

				//Side Wall
				fi.draw(lX-s, tY, lX, bY, 0, 15, 16, 16)
				fi.draw(rX+s, tY, rX, bY, 0, 15, 16, 16)

				//Bottom
				fi.draw(lX-s, bY, lX, bY+s*3, 0, 16, 16, 48)
				fi.draw(lX, bY, rX, bY+s*3, 14, 16, 15, 48)
				fi.draw(rX+s, bY, rX, bY+s*3, 0, 16, 16, 48)
			}
			/*else -> if(engine.framecolor>0) {
				val mW = maxWidth+s
				val fi = resources.imgFrame[engine.framecolor]
				val oX = 0
			}*/
		}

		if(showMeter) {
			// 右Meter
			val mainH = engine.meterValue
			val subH = engine.meterValueSub
			val maxH = maxHeight
			tmpX = x+fieldW*s+8
			tmpY = y+4

			drawRect(tmpX, tmpY, 4, maxH, 0)

			if(engine.meterValue>0) {
				drawRect(
					tmpX, tmpY+maxHeight-mainH, 4, maxHeight,
					getMeterColorHex(engine.meterColor, engine.meterValue, maxH), 1f
				)
			}
		}
		if(true) {
			//下Meter 残りlockdelayとARE
			val tmpX = x+8f
			val tmpY = y+fieldH*s+8f
			val mW = maxWidth-8f
			fillRectSpecific(tmpX, tmpY, mW, 4f, 0)
			when(engine.stat) {
				GameEngine.Status.MOVE -> if(engine.lockDelay>0) {
					val value = maxOf(0f, mW-engine.lockDelayNow*mW/engine.lockDelay)
					fillRectSpecific(
						tmpX+(mW-value)/2f, tmpY, value, 4f,
						if(engine.lockDelayNow>0) 0xFFFF00 else 0x00FF00
					)
				} else {
					fillRectSpecific(tmpX, tmpY, mW, 4f, 0xFF0000)
				}
				GameEngine.Status.LOCKFLASH -> {
					fillRectSpecific(tmpX, tmpY, mW, 4f, 0xFFFFFF)
					if(engine.ruleOpt.lockflash>0) {
						val value = engine.statc[0]*mW/engine.ruleOpt.lockflash
						fillRectSpecific(tmpX+(mW-value)/2f, tmpY, value, 4f, 0x808080)
					}
				}
				GameEngine.Status.LINECLEAR -> if(engine.lineDelay>0) {
					val value = mW-engine.statc[0]*mW/engine.lineDelay
					fillRectSpecific(tmpX+(mW-value)/2f, tmpY, value, 4f, 0x00FFFF)
				}
				GameEngine.Status.ARE -> if(engine.statc[1]>0) {
					fillRectSpecific(
						tmpX, tmpY, mW, 4f,
						if(engine.ruleOpt.areCancelMove||engine.ruleOpt.areCancelRotate||engine.ruleOpt.areCancelHold) 0xFF8000 else 0xFFFF00
					)
					val value = maxOf(0f, mW-engine.statc[0]*mW/engine.statc[1])
					fillRectSpecific(tmpX+(mW-value)/2f, tmpY, value, 4f, 0)
				}
				else -> {
				}
			}
			val y = fieldH+1
			val spd = engine.speed
			val playerID = engine.playerID
			val g = spd.gravity
			val d = spd.denominator
			drawSpeedMeter(engine, playerID, -13, y, g, d, 5)
			drawMenuNum(engine, playerID, 0, y, String.format("%3f", if(g<0||d<0) fieldH*1f else g*1f/d))

			for(i in 0..1) {
				val show = if(i==0) "ARE" to spd.are else "LINE" to spd.areLine

				drawMenuNum(engine, playerID, 6+i*3, y, String.format(if(i==0) "%2d/" else "%2d", show.second))
				drawMenuNano(engine, playerID, 10+i*5, y*2+1, show.first, COLOR.WHITE, .5f)
			}
			for(i in 0..2) {
				val show = when(i) {
					0 -> "LINE" to spd.lineDelay
					1 -> "LOCK" to spd.lockDelay
					else -> "DAS" to spd.das
				}
				drawMenuNum(engine, playerID, 8-i*3, y+1, String.format(if(i==1) "%2d+" else "%2d", show.second))
				drawMenuNano(engine, playerID, 13-i*6, y*2+2, show.first, COLOR.WHITE, .5f)
			}
			drawMenuNano(engine, playerID, 0, y*2+3, "DELAYS", COLOR.WHITE, .5f)

		}
	}

	/** Currently working onBlockDraw a piece
	 * (Y-coordinateThe0MoreBlockDisplay only)
	 * @param x X-coordinate of base field
	 * @param y Y-coordinate of base field
	 * @param engine GameEngineInstance of
	 * @param scale Display magnification
	 */
	protected fun drawCurrentPiece(x:Int, y:Int, engine:GameEngine, scale:Float) {
		val blksize = (getBlockSize(engine)*scale).toInt()
		val bx = engine.nowPieceX
		val by = engine.nowPieceY
		var g = engine.fpf

		val isRetro = engine.framecolor in GameEngine.FRAME_SKIN_SG..GameEngine.FRAME_SKIN_GB
		val ys = if(!smoothfall||by>=engine.nowPieceBottomY||isRetro) 0 else engine.gcount*blksize/engine.speed.denominator%blksize
		//if(engine.harddropFall>0)g+=engine.harddropFall;
		if(!showLocus||isRetro) g = 0

		engine.nowPieceObject?.let {
			for(z in 0..g) {
				//if(g>0)
				var i = 0
				var x2:Int
				var y2:Int
				while(i<it.maxBlock) {
					x2 = it.dataX[it.direction][i]
					y2 = it.dataY[it.direction][i]
					while(i<it.maxBlock-1) {
						if(x2!=it.dataX[it.direction][i+1]) break
						i++
						if(y2>it.dataY[it.direction][i]) y2 = it.dataY[it.direction][i]
					}
					val b = it.block[i]
					drawBlock(
						x+((x2+bx)*16f*scale), y+((y2+by-z)*16f*scale), b,
						-.1f, .4f, scale*if(engine.big) 2 else 1
					)
					i++
				}
				if(z==0) drawPiece(
					x+bx*blksize, y+by*blksize+ys, it, scale*if(engine.big) 2 else 1, -.25f,
					ow = if(engine.statc[0]%2==0||engine.holdDisable) 2f else 0f
				)
			}
		}
	}

	/** fieldのBlockを描画
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param engine GameEngineのインスタンス
	 */
	protected fun drawField(x:Int, y:Int, engine:GameEngine, size:Int, scale:Float = 1f) {
		if(!doesGraphicsExist()) return

		var blksize = getBlockSize(engine)
		var zoom = scale
		if(size==-1) {
			blksize /= 2
			zoom /= 2
		} else if(size==1) {
			blksize *= 2
			zoom *= 2
		}

		val field = engine.field
		val width = field.width
		val height = field.height
		var viewHeight = field.height

		if(engine.heboHiddenEnable&&engine.gameActive) viewHeight -= engine.heboHiddenYNow

		val outlineType = if(engine.owBlockOutlineType==-1) engine.blockOutlineType else engine.owBlockOutlineType

		for(i in -field.hiddenHeight until viewHeight)
			for(j in 0 until width) {
				val x2 = (x+j*blksize).toFloat()
				val y2 = (y+i*blksize).toFloat()

				field.getBlock(j, i)?.also {
					if(it.getAttribute(Block.ATTRIBUTE.WALL))
						drawBlock(x2, y2, 0, it.skin, it.getAttribute(Block.ATTRIBUTE.BONE), it.darkness, it.alpha, zoom, it.aint)
					else if(it.color!=null) {
						if(engine.owner.replayMode&&engine.owner.replayShowInvisible)
							drawBlockForceVisible(x2, y2, it, scale)
						else if(it.getAttribute(Block.ATTRIBUTE.VISIBLE)) drawBlock(x2, y2, it, scale = scale)

						if(it.getAttribute(Block.ATTRIBUTE.OUTLINE)&&!it.getAttribute(Block.ATTRIBUTE.BONE)) {
							val ls = blksize-1
							when(outlineType) {
								GameEngine.BLOCK_OUTLINE_NORMAL -> {
									if(field.getBlockEmpty(j, i-1)) drawLineSpecific(x2, y2, (x2+ls), y2)
									if(field.getBlockEmpty(j, i+1)) drawLineSpecific(x2, (y2+ls), (x2+ls), (y2+ls))
									if(field.getBlockEmpty(j-1, i)) drawLineSpecific(x2, y2, x2, (y2+ls))
									if(field.getBlockEmpty(j+1, i)) drawLineSpecific((x2+ls), y2, (x2+ls), (y2+ls))
								}
								GameEngine.BLOCK_OUTLINE_CONNECT -> {
									if(!it.getAttribute(Block.ATTRIBUTE.CONNECT_UP)) drawLineSpecific(x2, y2, (x2+ls), y2)
									if(!it.getAttribute(Block.ATTRIBUTE.CONNECT_DOWN)) drawLineSpecific(x2, (y2+ls), (x2+ls), (y2+ls))
									if(!it.getAttribute(Block.ATTRIBUTE.CONNECT_LEFT)) drawLineSpecific(x2, y2, x2, (y2+ls))
									if(!it.getAttribute(Block.ATTRIBUTE.CONNECT_RIGHT)) drawLineSpecific((x2+ls), y2, (x2+ls), (y2+ls))
								}
								GameEngine.BLOCK_OUTLINE_SAMECOLOR -> {
									val color = getColorByID(it.color ?: Block.COLOR.WHITE)
									if(field.getBlockColor(j, i-1)!=it.color) drawLineSpecific(x2, y2, (x2+ls), y2, color)
									if(field.getBlockColor(j, i+1)!=it.color) drawLineSpecific(x2, (y2+ls), (x2+ls), (y2+ls), color)
									if(field.getBlockColor(j-1, i)!=it.color) drawLineSpecific(x2, y2, x2, (y2+ls), color)
									if(field.getBlockColor(j+1, i)!=it.color) drawLineSpecific((x2+ls), y2, (x2+ls), (y2+ls), color)
								}
							}
						}

					}
				}

			}
		drawFieldSpecific(x, y, width, viewHeight, blksize, zoom, outlineType)

		// BunglerHIDDEN
		field.let {
			if(engine.heboHiddenEnable&&engine.gameActive) {
				var maxY = engine.heboHiddenYNow
				if(maxY>height) maxY = height
				for(i in 0 until maxY)
					for(j in 0 until width)
						drawBlock(x+j*blksize, y+(height-1-i)*blksize, (x+y)%2, 0, false, 0.0f, 1f, zoom)
			}
		}
	}

	/** NEXTを描画
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param engine GameEngineのインスタンス
	 */
	private fun drawNext(x:Int, y:Int, engine:GameEngine) {
		val fldWidth = engine.fieldWidth+1
		val fldBlkSize = getBlockSize(engine)
		if(engine.isNextVisible&&engine.ruleOpt.nextDisplay>=1) {
			val pid = engine.nextPieceCount
			drawFont(x+60, y, "NEXT", FONT.NANO, COLOR.ORANGE)
			drawFont(x, y, "${engine.nextPieceArraySize}", FONT.NANO, COLOR.ORANGE, .5f)
			engine.getNextObject(pid)?.let {
				//int x2 = x + 4 + ((-1 + (engine.field.getWidth() - piece.getWidth() + 1) / 2) * 16);
				val x2 = x+4+engine.getSpawnPosX(engine.field, it)*fldBlkSize //Rules with spawn x modified were misaligned.
				val y2 = y+4+nextHeight+engine.getSpawnPosY(engine.field, it)*fldBlkSize
				drawPiece(x2, y2, it)
				drawFont(x+60, y+16, "$pid", FONT.NANO, if(pid%7==0) COLOR.YELLOW else COLOR.WHITE, .75f)
				if(engine.ruleOpt.fieldCeiling||!engine.ruleOpt.pieceEnterAboveField)
					drawPieceOutline(x2, y2, it, 1f, .5f)
			}
			if(engine.ruleOpt.nextDisplay>1) when(nextDisplayType) {
				2 -> {
					val x2 = x+8+fldWidth*fldBlkSize
					drawFont(x2+16, y+40, "NEXT", FONT.NANO, COLOR.ORANGE)
					for(i in 0 until minOf(engine.ruleOpt.nextDisplay, 7)) {
						engine.getNextObject(pid+i)?.let {
							val centerX = (64-(it.width+1)*16)/2-it.minimumBlockX*16
							val centerY = (64-(it.height+1)*16)/2-it.minimumBlockY*16
							drawPiece(x2+centerX, y+48+i*64+centerY, it, 1f)
							(pid+i+1).let {n -> if(n%7==0) drawFont(x2, y+48+i*64, "$n", FONT.NANO, COLOR.YELLOW, .5f)}
						}
					}
				}
				1 -> {
					val x2 = x+8+fldWidth*fldBlkSize
					drawFont(x2, y+40, "NEXT", FONT.NANO, COLOR.ORANGE)
					for(i in 0 until minOf(engine.ruleOpt.nextDisplay, 14)) {
						engine.getNextObject(pid+i)?.let {
							val centerX = (32-(it.width+1)*8)/2-it.minimumBlockX*8
							val centerY = (32-(it.height+1)*8)/2-it.minimumBlockY*8
							drawPiece(x2+centerX, y+48+i*32+centerY, it, .75f)
							(pid+i+1).let {n -> if(n%7==0) drawFont(x2, y+48+i*32, "$n", FONT.NANO, COLOR.YELLOW, .5f)}
						}
					}
				}
				else -> {
					// NEXT1~3
					for(i in 0 until minOf(2, engine.ruleOpt.nextDisplay-1))
						engine.getNextObject(pid+i+1)?.let {
							drawPiece(x+124+i*40, y+48-(it.maximumBlockY+1)*8, it, .5f)
							(pid+i+1).let {n -> if(n%7==0) drawFont(x+124+i*40, y+48, "$n", FONT.NANO, COLOR.YELLOW, .5f)}
						}

					if(engine.ruleOpt.nextDisplay>=4) {
						// NEXT4~
						for(i in 0 until engine.ruleOpt.nextDisplay-3) engine.getNextObject(pid+i+3)?.let {
							drawPiece(x+182, y+i*40+88-(it.maximumBlockY+1)*8, it, .5f)
							(pid+i+3).let {n -> if(n%7==0) drawFont(x+182, y+i*40+88, "$n", FONT.NANO, COLOR.YELLOW, .5f)}
						}
					}
				}
			}
		}

		if(engine.isHoldVisible) {
			// HOLD
			val holdRemain = engine.ruleOpt.holdLimit-engine.holdUsedCount
			val x2 = if(sidenext) x-32 else x+if(nextDisplayType==2) -48 else 0
			val y2 = if(sidenext) y+40 else y+if(nextDisplayType==0) 16 else 0

			if(engine.ruleOpt.holdEnable&&(engine.ruleOpt.holdLimit<0||holdRemain>0)) {
				var str = "SWAP"
				var tempColor = if(engine.holdDisable) COLOR.WHITE else COLOR.GREEN
				if(engine.ruleOpt.holdLimit>=0) {
					str += "\ne$holdRemain"
					if(!engine.holdDisable&&holdRemain>0&&holdRemain<=10)
						tempColor = if(holdRemain<=5) COLOR.RED else COLOR.YELLOW
				}
				drawFont(x2, y2, str, FONT.NANO, tempColor, .75f)

				engine.holdPieceObject?.let {
					val dark = if(engine.holdDisable) .3f else 0f
					it.resetOffsetArray()
					it.setDarkness(0f)

					when(nextDisplayType) {
						2 -> {
							val centerX = (64-(it.width+1)*16)/2-it.minimumBlockX*16
							val centerY = (64-(it.height+1)*16)/2-it.minimumBlockY*16
							drawPiece(x-64+centerX, y+48+centerY, it, 1f, dark, ow = 1f)
						}
						1 -> {
							val centerX = (32-(it.width+1)*8)/2-it.minimumBlockX*8
							val centerY = (32-(it.height+1)*8)/2-it.minimumBlockY*8
							drawPiece(x2+centerX, y+48+centerY, it, .5f, dark, ow = 1f)
						}
						else -> drawPiece(x2, y+48-(it.maximumBlockY+1)*8, it, .5f, dark, ow = 1f)
					}
				}
			}
		}
	}

	/** 現在操作中のBlockピースのghost を描画
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param engine GameEngineのインスタンス
	 * @param scale 表示倍率
	 */
	protected open fun drawGhostPiece(x:Float, y:Float, engine:GameEngine, scale:Float) {
		val blksize = (BS*scale)
		val px = (engine.nowPieceX*blksize+x).toInt()
		val py = (engine.nowPieceBottomY*blksize+y).toInt()
		engine.nowPieceObject?.let {
			drawPiece(px, py, it, scale, .3f, .7f)
			if(outlineghost) drawPieceOutline(px, py, it, blksize, 1f, true)
		}
	}

	protected open fun drawHintPiece(x:Int, y:Int, engine:GameEngine, scale:Float) {
		val ai = engine.ai ?: return
		engine.aiHintPiece?.let {
			val blksize = (BS*scale)
			val px = (ai.bestX*blksize+x).toInt()
			val py = (ai.bestY*blksize+y).toInt()
			it.direction = ai.bestRt
			drawPieceOutline(px, py, it, blksize)
		}
	}

	/** Draw shadow nexts
	 * @param x X coord
	 * @param y Y coord
	 * @param engine GameEngine
	 * @param scale Display size of piece
	 */
	protected fun drawShadowNexts(x:Int, y:Int, engine:GameEngine, scale:Float) {
		val blksize = 16*scale
		engine.getNextObject(engine.nextPieceCount)?.let {next ->
			val sx = engine.getSpawnPosX(next)
			val sy = next.getBottom(sx, engine.getSpawnPosY(next), engine.field)
			drawPieceOutline(x+(blksize*sx).toInt(), y+(blksize*sy).toInt(), next, blksize, .5f, true)
		}
		engine.holdPieceObject?.let {hold ->
			val sx = engine.getSpawnPosX(hold)
			val sy = hold.getBottom(sx, engine.getSpawnPosY(hold), engine.field)
			drawPieceOutline(x+(blksize*sx).toInt(), y+(blksize*sy).toInt(), hold, blksize, .5f/3)
		}
		engine.nowPieceObject?.let {piece ->
			val shadowX = engine.nowPieceX
			val shadowY = engine.nowPieceBottomY+piece.minimumBlockY
			val size = if(piece.big||engine.displaysize==1) 2 else 1
			val shadowCenter = blksize*(piece.minimumBlockX*2+(piece.width+size))/2

			for(i in 0 until engine.ruleOpt.nextDisplay-1) {
				if(i>=3) break

				engine.getNextObject(engine.nextPieceCount+i)?.let {next ->
					val vPos = (blksize*shadowY).toInt()-(i+1)*24-8
					val nextCenter = blksize*(next.minimumBlockX*2+(next.width+1))/4

					if(vPos>=-blksize/2)
						drawPiece(x+(blksize*shadowX+shadowCenter-nextCenter).toInt(), y+vPos, next, .5f*scale, .25f, .75f)
				}
			}
			engine.holdPieceObject?.let {hold ->
				val nextCenter = blksize*(hold.minimumBlockX*2+(hold.width+1))/4

				drawPiece(x+(blksize*shadowX-nextCenter).toInt(), y+(blksize*shadowY).toInt(), hold, .5f*scale, .25f, .25f)
			}
		}
	}

	/* Ready画面の描画処理 */
	override fun renderReady(engine:GameEngine, playerID:Int) {
		if(!engine.allowTextRenderByReceiver) return
		//if(engine.isVisible == false) return;

		if(engine.statc[0]>0) {
			val offsetX = fieldX(engine, playerID)
			val offsetY = fieldY(engine, playerID)

			if(engine.statc[0]>0)
				if(engine.displaysize!=-1) {
					if(engine.statc[0]>=engine.readyStart&&engine.statc[0]<engine.readyEnd)
						drawDirectFont(offsetX+4, offsetY+196, "READY", COLOR.WHITE, 2f)
					else if(engine.statc[0]>=engine.goStart&&engine.statc[0]<engine.goEnd)
						drawDirectFont(offsetX+36, offsetY+196, "GO!", COLOR.WHITE, 2f)
				} else if(engine.statc[0]>=engine.readyStart&&engine.statc[0]<engine.readyEnd)
					drawDirectFont(offsetX+20, offsetY+80, "READY", COLOR.WHITE)
				else if(engine.statc[0]>=engine.goStart&&engine.statc[0]<engine.goEnd)
					drawDirectFont(offsetX+32, offsetY+30, "GO!", COLOR.WHITE)

			drawDirectNano(offsetX+4, offsetY+230, "TODAY SEEDS:", COLOR.WHITE)
			drawDirectNano(offsetX+4, offsetY+245, "${engine.statistics.randSeed}", COLOR.WHITE, 0.75f)
		}
	}

	/* Blockピース移動時の処理 */
	override fun renderMove(engine:GameEngine, playerID:Int) {
		if(!engine.isVisible) return

		val offsetX = fieldX(engine, playerID)
		val offsetY = fieldY(engine, playerID)

		if(engine.statc[0]>1||engine.ruleOpt.moveFirstFrame)
			when(engine.displaysize) {
				1 -> {
					if(engine.ghost&&engine.ruleOpt.ghost) drawGhostPiece(offsetX+4f, offsetY+52f, engine, 2f)
					if(engine.ai!=null&&engine.aiShowHint&&engine.aiHintReady) drawHintPiece(offsetX+4, offsetY+52, engine, 2f)
					drawCurrentPiece(offsetX+4, offsetY+52, engine, 2f)
				}
				0 -> {
					if(engine.ghost&&engine.ruleOpt.ghost) drawGhostPiece(offsetX+4f, offsetY+52f, engine, 1f)
					if(engine.ai!=null&&engine.aiShowHint&&engine.aiHintReady) drawHintPiece(offsetX+4, offsetY+52, engine, 1f)
					drawCurrentPiece(offsetX+4, offsetY+52, engine, 1f)
				}
				else -> {
					if(engine.ghost&&engine.ruleOpt.ghost) drawGhostPiece(offsetX+4f, offsetY+4f, engine, .5f)
					if(engine.ai!=null&&engine.aiShowHint&&engine.aiHintReady) drawHintPiece(offsetX+4, offsetY+4, engine, .5f)
					drawCurrentPiece(offsetX+4, offsetY+4, engine, .5f)
				}
			}
	}

	override fun renderLockFlash(engine:GameEngine, playerID:Int) {
		/*if(engine.fpf>0)*/ renderMove(engine, playerID)
	}

	override fun renderLineClear(engine:GameEngine, playerID:Int) {
		if(engine.fpf>0) renderMove(engine, playerID)
	}

	override fun renderARE(engine:GameEngine, playerID:Int) {
		if(engine.fpf>0) renderMove(engine, playerID)
	}

	override fun lineClear(engine:GameEngine, playerID:Int, y:Int) {
		val s = getBlockSize(engine)
		effects.add(
			BeamH(
				fieldX(engine, playerID)+4, fieldY(engine, playerID)+52+y*s,
				getBlockSize(engine)*engine.fieldWidth, s
			)
		)
	}

	/* Blockを消す演出を出すときの処理 */
	override fun blockBreak(engine:GameEngine, x:Int, y:Int, blk:Block) {
		resources
		if(showlineeffect&&engine.displaysize!=-1) {
			val color = blk.drawColor
			val sx = fieldX(engine)+4+x*getBlockSize(engine)
			val sy = fieldY(engine)+52+y*getBlockSize(engine)
			// 通常Block
			if(blk.isGemBlock)
				effects.add(
					FragAnim(ANIM.GEM, sx, sy, (color-Block.COLOR_GEM_RED)%resources.pEraseMax, lineeffectspeed)
				)// 宝石Block
			else if(!blk.getAttribute(Block.ATTRIBUTE.BONE))
				effects.add(
					FragAnim(
						if(blk.getAttribute(Block.ATTRIBUTE.LAST_COMMIT)) ANIM.SPARK else ANIM.BLOCK,
						sx, sy, maxOf(0, color-Block.COLOR_WHITE)%resources.blockBreakMax, lineeffectspeed
					)
				)
			//blockParticles.addBlock(engine, receiver, playerID, blk, j, i, 10, 90, li>=4, localRandom)
			//blockParticles.addBlock(engine, receiver, playerID, blk, j, i, engine.field.width, cY, li, 120)
		}
	}

	/* ラインを消す演出の処理 */
	override fun calcScore(engine:GameEngine, event:ScoreEvent?) {
		event ?: return
		val w = engine.fieldWidth*getBlockSize(engine)/2
		val sx = fieldX(engine)+4+w

		val dir = maxOf(-w*2, minOf(w*2, (320-sx)*w/128))
		val sy = fieldY(engine)+52+getBlockSize(engine)/2*
			when {
				event.lines==0 -> engine.nowPieceBottomY*2
				event.split -> engine.field.lastLinesTop*2
				else -> (engine.field.lastLinesTop+engine.field.lastLinesBottom)
			}
		effects.add(PopupAward(sx, sy, event, if(event.lines==0) engine.speed.are else engine.speed.lineDelay, dir))
	}

	override fun addScore(x:Int, y:Int, pts:Int, color:COLOR) {
		effects.add(PopupPoint(x, y, pts, color.ordinal))
	}

	override fun addCombo(x:Int, y:Int, pts:Int, type:PopupCombo.CHAIN, ex:Int) {
		if(pts>0) effects.add(PopupCombo(x, y, pts, type, ex))
	}

	override fun shootFireworks(engine:GameEngine, x:Int, y:Int, color:COLOR) {
		effects.add(FragAnim(ANIM.HANABI, x, y, color.ordinal))
		super.shootFireworks(engine, x, y, color)
	}

	override fun bravo(engine:GameEngine) {
		effects.add(PopupBravo(fieldX(engine), fieldY(engine)))
		super.bravo(engine)
	}

	/* EXCELLENT画面の描画処理 */
	override fun renderExcellent(engine:GameEngine, playerID:Int) {
		if(!engine.allowTextRenderByReceiver) return
		if(!engine.isVisible) return

		val offsetX = fieldX(engine, playerID)
		val offsetY = fieldY(engine, playerID)

		if(engine.displaysize!=-1) {
			if(engine.owner.players<=1)
				drawDirectFont(offsetX+4, offsetY+204, "EXCELLENT!", COLOR.RAINBOW, 1f)
			else drawDirectFont(offsetX+36, offsetY+204, "You WIN!", COLOR.ORANGE, 1f)
		} else if(engine.owner.players<=1)
			drawDirectFont(offsetX+4, offsetY+80, "EXCELLENT!", COLOR.RAINBOW, .5f)
		else drawDirectFont(offsetX+20, offsetY+80, "You WIN!", COLOR.ORANGE, .5f)
	}

	/* game over画面の描画処理 */
	override fun renderGameOver(engine:GameEngine, playerID:Int) {
		if(!engine.allowTextRenderByReceiver||!engine.isVisible) return
		val offsetX = fieldX(engine, playerID)
		val offsetY = fieldY(engine, playerID)
		if(engine.lives>0&&engine.gameActive) {
			drawDirectFont(offsetX+4, offsetY+204, "LEFT", COLOR.WHITE, 1f)
			drawDirectFont(offsetX+132, offsetY+196, ((engine.lives-1)%10).toString(), COLOR.WHITE, 2f)
		} else if(engine.statc[0]>=engine.statc[1])
			when {
				engine.displaysize!=-1 ->
					when {
						engine.owner.players<2 -> if(engine.ending==0)
							drawDirectFont(offsetX+12, offsetY+204, "GAME OVER", COLOR.RED, 1f)
						else drawDirectFont(offsetX+28, offsetY+204, "THE END", COLOR.WHITE, 1f)
						engine.owner.winner==-2 -> drawDirectFont(offsetX+52, offsetY+204, "DRAW", COLOR.PURPLE, 1f)
						engine.owner.players<3 -> drawDirectFont(offsetX+20, offsetY+80, "You Lost", COLOR.RED, 1f)
					}
				engine.owner.players<2 -> if(engine.ending==0)
					drawDirectFont(offsetX+4, offsetY+204, "GAME OVER", COLOR.RED, 1f)
				else drawDirectFont(offsetX+20, offsetY+204, "THE END", COLOR.WHITE, 1f)
				engine.owner.winner==-2 -> drawDirectFont(offsetX+28, offsetY+80, "DRAW", COLOR.PURPLE, .5f)
				engine.owner.players<3 -> drawDirectFont(offsetX+12, offsetY+80, "You Lost", COLOR.RED, .5f)
			}
	}

	/* Render results screen処理 */
	override fun renderResult(engine:GameEngine, playerID:Int) {
		if(!engine.allowTextRenderByReceiver) return
		if(!engine.isVisible) return

		var tempColor:COLOR = if(engine.statc[0]==0) COLOR.RED else COLOR.WHITE

		drawDirectFont(
			fieldX(engine, playerID)+12,
			fieldY(engine, playerID)+340, "RETRY", tempColor, 1f
		)

		tempColor = if(engine.statc[0]==1) COLOR.RED else COLOR.WHITE
		drawDirectFont(
			fieldX(engine, playerID)+108,
			fieldY(engine, playerID)+340, "END", tempColor, 1f
		)
	}

	/* fieldエディット画面の描画処理 */
	override fun renderFieldEdit(engine:GameEngine, playerID:Int) {
		val x = fieldX(engine, playerID)+4f+engine.fldeditX*getBlockSize(engine)
		val y = fieldY(engine, playerID)+52f+engine.fldeditY*getBlockSize(engine)
		val bright = if(engine.fldeditFrames%60>=30) -.5f else -.2f
		drawBlock(x, y, engine.fldeditColor, engine.skin, false, bright, 1f, 1f)
	}
	/* 各 frame 最初の描画処理 */
	override fun renderFirst(engine:GameEngine, playerID:Int) {
		if(engine.playerID==0) drawBG(engine)

		// NEXTなど
		if(!engine.owner.menuOnly&&engine.isVisible) {
			val offsetX = fieldX(engine, playerID)
			val offsetY = fieldY(engine, playerID)

			if(engine.displaysize!=-1) {
				drawFrame(offsetX, offsetY+48, engine, engine.displaysize)
				drawNext(offsetX-fieldXOffset(engine), offsetY, engine)
				drawField(offsetX+4, offsetY+4+nextHeight, engine, engine.displaysize)
			} else {
				drawFrame(offsetX, offsetY, engine, -1)
				drawField(offsetX+4, offsetY+4, engine, -1)
			}
			printFontSpecific(offsetX, offsetY-15, "${engine.statistics.randSeed}", FONT.NANO, COLOR.WHITE, 0.7f, 0.75f)
			engine.statc.forEachIndexed {i, it ->
				printFontSpecific(offsetX-41, offsetY+i*10, String.format("%3d", it), FONT.NANO, COLOR.WHITE, 0.7f, 0.75f)
			}

			if(nextshadow) drawShadowNexts(
				offsetX+4, offsetY+52, engine, when(engine.displaysize) {
					1 -> 2f
					0 -> 1f
					else -> .5f
				}
			)

		}
	}
	/* 各 frame の最後に行われる処理 */
	override fun onLast(engine:GameEngine, playerID:Int) {
		if(playerID==engine.owner.players-1) effectUpdate()
	}

	/** Update effects */
	private fun effectUpdate() {
		effects.forEach {
			it.update()
		}
		effects.removeIf {it.isExpired}
	}
	/* 各 frame の最後に行われる描画処理 */
	override fun renderLast(engine:GameEngine, playerID:Int) {
		if(playerID==engine.owner.players-1) effectRender()
	}

	fun drawAward(x:Int, y:Int, ev:ScoreEvent, anim:Int, alpha:Float = 1f) {
		val strPieceName = ev.piece?.id?.let {Piece.Shape.names[it]} ?: ""

		when {
			ev.lines==1 -> drawDirectFont(x-48, y, "SINGLE", color = if(ev.twist==null) COLOR.COBALT else COLOR.BLUE, alpha = alpha)
			ev.lines==2 -> {
				if(!ev.split)
					drawDirectFont(x-48, y, "DOUBLE", color = if(ev.twist==null) COLOR.BLUE else COLOR.CYAN, alpha = alpha)
				else drawDirectFont(x-80, y, "SPLIT TWIN", color = COLOR.PURPLE, alpha = alpha)
			}
			ev.lines==3 -> {
				if(!ev.split)
					drawDirectFont(x-48, y, "TRIPLE", color = COLOR.GREEN, alpha = alpha)
				else drawDirectFont(x-80, y, "1.2.TRIPLE", color = COLOR.CYAN, alpha = alpha)
			}
			ev.lines>=4 -> drawDirectFont(x-72, y, "QUADRUPLE", color = getRainbowColor(anim), alpha = alpha)
		}
		if(ev.twist!=null) when {
			ev.twist.isMini -> {
				drawDirectFont(x-80, y-16, "MINI", color = if(ev.b2b) COLOR.CYAN else COLOR.BLUE, alpha = alpha)
				ev.piece?.let {drawPiece(x-32, y, it, 0.5f, alpha = alpha)}
				drawDirectFont(x-16, y, "$strPieceName-TWIST", color = if(ev.b2b) COLOR.PINK else COLOR.PURPLE, alpha = alpha)
			}
			ev.twist==ScoreEvent.Twister.IMMOBILE_EZ -> {
				ev.piece?.let {drawPiece(x-16, y, it, 0.5f, alpha = alpha)}
				drawDirectFont(x-54, y-8, "EZ", color = COLOR.ORANGE, alpha = alpha)
				drawDirectFont(x+54, y-8, "TRICK", color = COLOR.ORANGE, alpha = alpha)
			}
			else -> {
				ev.piece?.let {drawPiece(x-64, y, it, 0.5f, alpha = alpha)}
				drawDirectFont(
					x-32, y-8, "-TWISTER",
					color = if(ev.lines==3) getRainbowColor(anim) else if(ev.b2b) COLOR.PINK else COLOR.PURPLE, alpha = alpha
				)
			}
		}
	}

	@JvmOverloads fun drawCombo(x:Int, y:Int, pts:Int, type:PopupCombo.CHAIN, alpha:Float = 1f) {
		when(type) {
			PopupCombo.CHAIN.B2B -> {
				drawFont(x-18, y-15, "SKILL", FONT.NANO, COLOR.RED, .75f, alpha)
				drawDirectNum(x-18, y, String.format("%2d", pts), COLOR.YELLOW, 1.5f, alpha)
				drawFont(x-18, y+20, "CHAIN!", FONT.NANO, COLOR.ORANGE, .75f, alpha)
			}
			PopupCombo.CHAIN.COMBO -> {
				drawDirectNum(x-18, y-0, String.format("%2d", pts), COLOR.CYAN, 1.5f, alpha)
				drawFont(x+18, y+8, "REN", FONT.NANO, COLOR.BLUE, .5f, alpha)
				drawFont(x-18, y+20, "COMBO!", FONT.NANO, COLOR.BLUE, .75f, alpha)
			}
		}
	}

	protected fun effectRender() {
		effects.forEachIndexed {i, it ->
			drawEffectSpecific(i, it)
		}
	}

	protected abstract fun printFontSpecific(x:Int, y:Int, str:String, font:FONT, color:COLOR, scale:Float, alpha:Float)

	protected abstract fun printTTFSpecific(x:Int, y:Int, str:String, color:COLOR, alpha:Float)

	protected abstract fun doesGraphicsExist():Boolean

	protected abstract fun drawBlockSpecific(
		x:Float, y:Float, sx:Int, sy:Int, sk:Int,
		size:Float, darkness:Float, alpha:Float
	)

	protected abstract fun drawLineSpecific(
		x:Float, y:Float, sx:Float, sy:Float, color:Int = 0xFFFFFF, alpha:Float = 1f,
		w:Float = 1f
	)

	protected fun drawLineSpecific(x:Int, y:Int, sx:Int, sy:Int, color:Int = 0xFFFFFF, alpha:Float = 1f, w:Int = 1) =
		drawLineSpecific(x.toFloat(), y.toFloat(), sx.toFloat(), sy.toFloat(), color, alpha, w.toFloat())

	/** draw and Fill Rectangle */
	private fun drawRect(
		x:Float, y:Float, w:Float, h:Float, color:Int = 0xFFFFFF, alpha:Float = 1f,
		outlineW:Float = 0f, outlineColor:Int = 0x000000
	) {
		fillRectSpecific(x-outlineW/2f, y-outlineW/2f, w+outlineW/2f, h+outlineW/2f, color, alpha)
		if(outlineW>0)
			drawRectSpecific(x-outlineW/2f, y-outlineW/2f, w+outlineW/2f, h+outlineW/2f, outlineColor, alpha, outlineW)

	}

	private fun drawRect(
		x:Int, y:Int, w:Int, h:Int, color:Int = 0xFFFFFF, alpha:Float = 1f, outlineW:Int = 0,
		outlineColor:Int = 0x000000
	) =
		drawRect(x.toFloat(), y.toFloat(), w.toFloat(), h.toFloat(), color, alpha, outlineW.toFloat(), outlineColor)

	/** Draw Rectangle Outline*/
	protected abstract fun drawRectSpecific(
		x:Float, y:Float, w:Float, h:Float, color:Int = 0xFFFFFF, alpha:Float = 1f,
		bold:Float = 1f
	)

	private fun drawRectSpecific(x:Int, y:Int, w:Int, h:Int, color:Int = 0xFFFFFF, alpha:Float = 1f, bold:Int = 0) =
		drawRectSpecific(x.toFloat(), y.toFloat(), w.toFloat(), h.toFloat(), color, alpha, bold.toFloat())
	/** Fiil Rectangle Solid*/
	protected abstract fun fillRectSpecific(x:Float, y:Float, w:Float, h:Float, color:Int = 0xFFFFFF, alpha:Float = 1f)

	private fun fillRectSpecific(x:Int, y:Int, w:Int, h:Int, color:Int = 0xFFFFFF, alpha:Float = 1f) =
		fillRectSpecific(x.toFloat(), y.toFloat(), w.toFloat(), h.toFloat(), color, alpha)

	/**
	 * draw outline Piece
	 */
	protected open fun drawPieceOutline(x:Int, y:Int, piece:Piece, blksize:Float, alpha:Float = 1f, whiteLine:Boolean = false) {
		piece.let {
			val bigmul = blksize*(1+it.big.toInt())
			it.block.forEachIndexed {i, blk ->
				val x2 = x+(it.dataX[it.direction][i]*bigmul).toInt()
				val y2 = y+(it.dataY[it.direction][i]*bigmul).toInt()
				if(y2>=0) drawBlockOutline(x2, y2, blk, bigmul.toInt(), alpha, whiteLine)
			}
		}
	}

	protected open fun drawBlockOutline(x:Int, y:Int, blk:Block, blksize:Int, alpha:Float = 1f, whiteLine:Boolean = false) {
		blk.let {
			val x3 = (x)
			val y3 = (y)
			val ls = blksize-1

			val color = getColorByID(if(it.getAttribute(Block.ATTRIBUTE.BONE)) Block.COLOR.WHITE else it.color ?: Block.COLOR.WHITE)
			val lC = if(whiteLine) 0xFFFFFF else color
			fillRectSpecific(x3, y3, blksize, blksize, color, alpha*.5f)

			if(!it.getAttribute(Block.ATTRIBUTE.CONNECT_UP)) {
				drawLineSpecific(x3, y3, (x3+ls), y3, lC, alpha*.5f)
				drawLineSpecific(x3, (y3+1), (x3+ls), (y3+1), lC, alpha*.5f)
			}
			if(!it.getAttribute(Block.ATTRIBUTE.CONNECT_DOWN)) {
				drawLineSpecific(x3, (y3+ls), (x3+ls), (y3+ls), lC, alpha*.5f)
				drawLineSpecific(x3, (y3-1+ls), (x3+ls), (y3-1+ls), lC, alpha*.5f)
			}
			if(!it.getAttribute(Block.ATTRIBUTE.CONNECT_LEFT)) {
				drawLineSpecific(x3, y3, x3, (y3+ls), lC, alpha*.5f)
				drawLineSpecific((x3+1), y3, (x3+1), (y3+ls), lC, alpha*.5f)
			}
			if(!it.getAttribute(Block.ATTRIBUTE.CONNECT_RIGHT)) {
				drawLineSpecific((x3+ls), y3, (x3+ls), (y3+ls), lC, alpha*.5f)
				drawLineSpecific((x3-1+ls), y3, (x3-1+ls), (y3+ls), lC, alpha*.5f)
			}
			if(it.getAttribute(Block.ATTRIBUTE.CONNECT_LEFT, Block.ATTRIBUTE.CONNECT_UP))
				fillRectSpecific(x3, y3, 2, 2, lC, alpha*.5f)
			if(it.getAttribute(Block.ATTRIBUTE.CONNECT_LEFT, Block.ATTRIBUTE.CONNECT_DOWN))
				fillRectSpecific(x3, (y3+blksize-2), 2, 2, lC, alpha*.5f)
			if(it.getAttribute(Block.ATTRIBUTE.CONNECT_RIGHT, Block.ATTRIBUTE.CONNECT_UP))
				fillRectSpecific(x3+blksize-2, y3, 2, 2, lC, alpha*.5f)
			if(it.getAttribute(Block.ATTRIBUTE.CONNECT_RIGHT, Block.ATTRIBUTE.CONNECT_DOWN))
				fillRectSpecific(x3+blksize-2, y3+blksize-2, 2, 2, lC, alpha*.5f)
		}
	}

	protected abstract fun drawBadgesSpecific(x:Float, y:Float, type:Int, scale:Float)
	protected abstract fun drawFieldSpecific(
		x:Int, y:Int, width:Int, viewHeight:Int, blksize:Int, scale:Float,
		outlineType:Int
	)

	protected open fun drawEffectSpecific(i:Int, it:EffectObject) {

		when(it) {
			/*is FragAnim -> {
				val flip = (i%2==0)!=(i%10==0)
				when(it.type) {
					ANIM.GEM
						// Gems frag
					-> ResourceHolder.imgPErase[it.color]
					// TI Block frag
					ANIM.SPARK
					-> ResourceHolder.imgBreak[it.color][0]
					// TAP Block frag
					ANIM.BLOCK
					-> ResourceHolder.imgBreak[it.color][1]
					//Fireworks
					ANIM.HANABI
					-> ResourceHolder.imgHanabi[it.color]
				}.draw(if(flip) it.dx else it.dx2, it.dy, if(flip) it.dx else x, y+sq,
					srcx, srcy, srcx+sq, srcy+sq)
			}
			is BeamH //Line Cleaned
			-> {
				srcy = (it.anim/2-1)*8
				if(it.anim%2==1) graphics.setDrawMode(Graphics.MODE_ADD)
				ResourceHolder.imgLine[0].draw(x, y, x+it.w, y+it.h, 0, srcy, 80, srcy+8)
				graphics.setDrawMode(Graphics.MODE_NORMAL)
			}*/
			is PopupAward -> drawAward(it.x.toInt(), it.y.toInt(), it.event, it.anim, it.alpha)
			is PopupCombo -> drawCombo(it.x.toInt(), it.y.toInt(), it.pts, it.type, it.alpha)
			is PopupPoint -> if(it.pts>0) drawDirectNum(
				it.dx.toInt(), it.dy.toInt(), "+${it.pts}", COLOR.values()[it.color],
				alpha = it.alpha
			)
			else if(it.pts<0) drawDirectNum(it.dx.toInt(), it.dy.toInt(), "${it.pts}", COLOR.RED)
			is PopupBravo //Field Cleaned
			-> {
				drawDirectFont(it.x.toInt()+20, it.y.toInt()+204, "BRAVO!", getRainbowColor((it.anim+4)%9), 1.5f)
				drawDirectFont(it.x.toInt()+52, it.y.toInt()+236, "PERFECT", getRainbowColor(it.anim%9), 1f)
			}
		}
	}

	override fun drawSpeedMeter(x:Float, y:Float, sp:Float, len:Float) {
		val s = if(sp<0) 1f else sp
		val w = maxOf(1f, len-1)*BS
		drawRectSpecific((x-1), (y-1), w+1, 3f, 0)
		fillRectSpecific(
			x, y, w, 2f, getColorByID(
				when {
					s<0.25f -> COLOR.CYAN
					s<0.5f -> COLOR.GREEN
					s<0.75f -> COLOR.YELLOW
					s<1f -> COLOR.ORANGE
					else -> COLOR.RED
				}
			)
		)
		if(s<.25f) fillRectSpecific(x, y, s*w*4, 2f, 0xFF00)
		if(s<.5f) fillRectSpecific(x, y, s*w*2, 2f, 0xFFFF00)
		if(s<0.75f) fillRectSpecific(x, y, s*w/.75f, 2f, 0xFF8000)

		if(s<1f) fillRectSpecific(x, y, s*w, 2f, 0xFF0000)
		else fillRectSpecific(x, y, minOf(1f, s-1)*w, 2f, 0xFFFFFF)
	}

	companion object {

		/** Block colorIDに応じてColor Hexを作成
		 * @param color Block colorID
		 * @return color Hex
		 */
		fun getColorByID(color:COLOR?):Int = when(color) {
			COLOR.WHITE -> 0xFFFFFF
			COLOR.RED -> 0xFF0000
			COLOR.ORANGE -> 0xFF8000
			COLOR.YELLOW -> 0xFFFF00
			COLOR.GREEN -> 0x00FF00
			COLOR.CYAN -> 0x00FFFF
			COLOR.BLUE -> 0x0040FF
			COLOR.PURPLE -> 0xAA00FF
			else -> 0
		}
		/** Block colorIDに応じてColor Hexを作成
		 * @param color Block colorID
		 * @return color Hex
		 */
		fun getColorByID(color:Block.COLOR?):Int = getColorByID(getBlockColor(color))
		fun getMeterColorHex(meterColor:Int, value:Int, max:Int):Int {
			var r = meterColor/65536
			var g = meterColor/256%256
			var b = meterColor%256
			when(meterColor) {
				GameEngine.METER_COLOR_LEVEL -> {
					r = (maxOf(0f, 255*minOf((value*3f-max)/max, 1f))).toInt()
					g = (maxOf(0f, 255*minOf((max-value)*3f/max, 1f))).toInt()
					b = (maxOf(0f, 255*minOf((max-value*3f)/max, 1f))).toInt()
				}
				GameEngine.METER_COLOR_LIMIT -> {//red<yellow<green<cyan
					r = (maxOf(0f, 255*minOf((max*2f-value*3f)/max, 1f))).toInt()
					g = (maxOf(0f, 255*minOf(value*3f/max, 1f))).toInt()
					b = (maxOf(0f, 255*minOf((value*3f-max*2f)/max, 1f))).toInt()
				}
				else -> return meterColor
			}
			return r*65536+g*256+b
		}

		const val nextHeight = 48
	}
}
