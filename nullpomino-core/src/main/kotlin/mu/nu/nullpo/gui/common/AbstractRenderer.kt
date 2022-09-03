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

package mu.nu.nullpo.gui.common

import mu.nu.nullpo.game.component.Block
import mu.nu.nullpo.game.component.Piece
import mu.nu.nullpo.game.event.EventReceiver
import mu.nu.nullpo.game.event.ScoreEvent
import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.gui.common.fx.Beam
import mu.nu.nullpo.gui.common.fx.Effect
import mu.nu.nullpo.gui.common.fx.FragAnim
import mu.nu.nullpo.gui.common.fx.FragAnim.ANIM
import mu.nu.nullpo.gui.common.fx.PopupAward
import mu.nu.nullpo.gui.common.fx.PopupBravo
import mu.nu.nullpo.gui.common.fx.PopupCombo
import mu.nu.nullpo.gui.common.fx.PopupPoint
import mu.nu.nullpo.gui.common.fx.particles.Fireworks
import mu.nu.nullpo.gui.common.fx.particles.ParticleEmitterBase
import mu.nu.nullpo.util.GeneralUtil.toInt

abstract class AbstractRenderer:EventReceiver() {
	internal abstract val resources:ResourceHolder

	/** 演出オブジェクト */
	internal val effects:MutableList<Effect> = mutableListOf()

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
		if(font==FONT.TTF) printTTFSpecific(x, y, str, color, scale, alpha)
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
	protected abstract fun drawFrameSpecific(x:Int, y:Int, engine:GameEngine)
	private fun drawFrame(x:Int, y:Int, engine:GameEngine) {
		// Upと下

		val s = engine.blockSize

		val fieldW = engine.field.width// ?: Field.DEFAULT_WIDTH
		val fieldH = engine.field.height//?: Field.DEFAULT_HEIGHT
		val fW = fieldW*s
		val fH = fieldH*s
		val lX = x
		val rX = lX+fW
		val tY = y
		val bY = tY+fH
		// NEXT area background
		if(showBg&&darknextarea) {
			when(nextDisplayType) {
				0 -> {
					val w = fW+15
					fillRectSpecific(x+20, y, w-40, -nextHeight, 0)
					for(i in 20 downTo 0)
						drawRectSpecific(x+20-i, y, w-40+i*2, -nextHeight-i, 0, 1-i/20f, 1)
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
					val x2 = x+s+fW
					val maxNext = if(engine.isNextVisible) engine.ruleOpt.nextDisplay else 0

					// HOLD area
					if(engine.ruleOpt.holdEnable&&engine.isHoldVisible) {
						fillRectSpecific(x-64, y, 64, nextHeight, 0)
						for(i in 8 downTo 0)
							drawRectSpecific(x-64-i, y-i, 65+i, nextHeight+i*2, 0, i/8f, 1)
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
				if(engine.displaySize==-1) img = resources.imgFieldBG[0]
				if(engine.displaySize==1) img = resources.imgFieldBG[2]
				img.draw(lX, tY, rX, bY, 0, 0, fW, fH, fieldbgbright)
			} else if(showBg) drawRect(lX, tY, fW, fH, 0, fieldbgbright)

		drawFrameSpecific(x, y, engine)

		when(engine.frameColor) {
			GameEngine.FRAME_SKIN_GB -> {
				drawRect(x, y, fW, fH, 0x888888)
				val fi = resources.imgFrameOld[0]
				for(i in 0..fieldH) {
					val zx = x-s
					val zy = y+i*s
					fi.draw(zx, zy, zx+s, zy+s, 0, 0, 16, 16)
					fi.draw(zx+fW+s, zy, zx+fW+s*2, zy+s, 0, 0, 16, 16)
					if(i==fieldH)
						for(z in 1..fieldW)
							fi.draw((zx+z*s), zy, zx+(z+1)*s, zy+s, 0, 0, 16, 16)
				}
			}
			GameEngine.FRAME_SKIN_SG -> {
				drawRect(x, y, fW, fH, 0)

				val fi = resources.imgFrameOld[1]
				val mW = fW+s*2
				fi.draw(x-s, y-s, x-s+mW/2, y-s+(fieldH+2)*s, 0, 0, 96, 352)
				fi.draw(x-s+mW, y-s, x-s+mW/2, y-s+(fieldH+2)*s, 0, 0, 96, 352)
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
			val mainH = fH*maxOf(0f, minOf(1f, engine.meterValue))
			val subH = fH*maxOf(0f, minOf(1f, engine.meterValueSub))
			val mW = maxOf(2f, s/4f)
			val zx = rX+s/2-mW/2
			val zy = tY.toFloat()
			drawRect(zx, zy, mW, fH.toFloat(), 0)


			if(engine.meterValueSub>engine.meterValue) {
				drawRect(
					zx+mW/2, zy+fH-subH, mW/2, subH,
					getMeterColorHex(engine.meterColorSub, engine.meterValueSub)
				)
			}
			if(engine.meterValue>0) {
				drawRect(
					zx, zy+fH-mainH, mW, mainH,
					getMeterColorHex(engine.meterColor, engine.meterValue)
				)
			}
			if(engine.meterValueSub>0&&engine.meterValueSub<=engine.meterValue) {
				val ov = minOf(mainH, subH)
				drawRect(
					zx+mW/2f, zy+fH-ov, mW, ov,
					getMeterColorHex(engine.meterColorSub, engine.meterValueSub), .5f
				)
			}
		}
		if(showSpeed) {
			//下Meter 残りlockdelayとARE
			val tmpX = lX+s/2f
			val tmpY = bY+s/2f
			val mW = fW-8f
			val mH = maxOf(2f, s/4f)
			fillRectSpecific(tmpX, tmpY, mW, mH, 0)
			when(engine.stat) {
				GameEngine.Status.MOVE -> if(engine.lockDelay>0) {
					val value = maxOf(0f, mW-engine.lockDelayNow*mW/engine.lockDelay)
					fillRectSpecific(
						tmpX+(mW-value)/2f, tmpY, value, mH,
						if(engine.lockDelayNow>0) 0xFFFF00 else 0x00FF00
					)
				} else {
					fillRectSpecific(tmpX, tmpY, mW, mH, 0xFF0000)
				}
				GameEngine.Status.LOCKFLASH -> {
					fillRectSpecific(tmpX, tmpY, mW, mH, 0xFFFFFF)
					if(engine.ruleOpt.lockFlash>0) {
						val value = engine.statc[0]*mW/engine.ruleOpt.lockFlash
						fillRectSpecific(tmpX+(mW-value)/2f, tmpY, value, mH, 0x808080)
					}
				}
				GameEngine.Status.LINECLEAR -> if(engine.lineDelay>0) {
					val value = mW-engine.statc[0]*mW/engine.lineDelay
					fillRectSpecific(tmpX+(mW-value)/2f, tmpY, value, mH, 0x00FFFF)
				}
				GameEngine.Status.ARE -> if(engine.statc[1]>0) {
					fillRectSpecific(
						tmpX, tmpY, mW, mH,
						if(engine.ruleOpt.areCancelMove||engine.ruleOpt.areCancelSpin||engine.ruleOpt.areCancelHold) 0xFF8000 else 0xFFFF00
					)
					val value = maxOf(0f, mW-engine.statc[0]*mW/engine.statc[1])
					fillRectSpecific(tmpX+(mW-value)/2f, tmpY, value, mH, 0)
				}
				else -> {
				}
			}
			val zy = fieldH+1
			val spd = engine.speed
			engine.playerID
			val g = spd.gravity
			val d = spd.denominator
			drawMenuSpeed(engine, 1, zy, g, d, 5)
			drawMenuNum(engine, 0, zy, String.format("%3f", if(g<0||d<0) fieldH*1f else g*1f/d))

			for(i in 0..1) {
				val show = if(i==0) "ARE" to spd.are else "LINE" to spd.areLine

				drawMenuNum(engine, 6+i*3, zy, String.format(if(i==0) "%2d/" else "%2d", show.second))
				drawMenuNano(engine, 10+i*5, zy*2+1, show.first, COLOR.WHITE, .5f)
			}
			for(i in 0..2) {
				val show = when(i) {
					0 -> "LINE" to spd.lineDelay
					1 -> "LOCK" to spd.lockDelay
					else -> "DAS" to spd.das
				}
				drawMenuNum(engine, 8-i*3, zy+1, String.format(if(i==1) "%2d+" else "%2d", show.second))
				drawMenuNano(engine, 13-i*6, zy*2+2, show.first, COLOR.WHITE, .5f)
			}
			drawMenuNano(engine, 0, zy*2+3, "DELAYS", COLOR.WHITE, .5f)

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

		val isRetro = engine.frameColor in GameEngine.FRAME_SKIN_SG..GameEngine.FRAME_SKIN_GB
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
		val fldWidth = engine.field.width+1
		val fbs = engine.blockSize
		val cps = fldWidth*fbs
		val cX = x+cps/2
		val rX = x+cps
		if(engine.isNextVisible&&engine.ruleOpt.nextDisplay>=1) {
			val pid = engine.nextPieceCount
			drawFont(x, y-fbs, "${engine.nextPieceArraySize}", FONT.NANO, COLOR.ORANGE, .5f)
			printFontSpecific(x, y-fbs/2, "${engine.statistics.randSeed}", FONT.NANO, COLOR.WHITE, 0.7f, 0.75f)
			engine.getNextObject(pid)?.let {
				//int x2 = x + 4 + ((-1 + (engine.field.getWidth() - piece.getWidth() + 1) / 2) * 16);
				val x2 = x+engine.getSpawnPosX(engine.field, it)*fbs //Rules with spawn x modified were misaligned.
				val y2 = y+engine.getSpawnPosY(engine.field, it)*fbs
				if(engine.ruleOpt.fieldCeiling||!engine.ruleOpt.pieceEnterAboveField)
					drawPieceOutline(x2, y2, it, 1f, .5f)
				else drawPiece(x2, y2, it)
				drawFont(x2, y-10, "$pid", FONT.NANO, if(pid%7==0) COLOR.YELLOW else COLOR.WHITE, .75f)
			}
			if(engine.ruleOpt.nextDisplay>1) when(nextDisplayType) {
				2 -> {
					drawFont(rX, y-fbs/2, "QUEUE", FONT.NANO, COLOR.ORANGE)
					for(i in 0 until minOf(engine.ruleOpt.nextDisplay, 14)) {
						engine.getNextObject(pid+i)?.let {
							val centerX = ((4-it.width-1)*fbs)/2-it.minimumBlockX*fbs
							val centerY = ((4-it.height-1)*fbs)/2-it.minimumBlockY*fbs
							val pY = y+i*3*fbs
							drawPiece(rX+centerX, pY+centerY, it, 1f)
							(pid+i+1).let {n -> if(n%7==0) drawFont(rX, pY, "$n", FONT.NANO, COLOR.YELLOW, .5f)}
						}
					}
				}
				1 -> {
					drawFont(rX, y, "QUEUE", FONT.NANO, COLOR.ORANGE)
					for(i in 0 until minOf(engine.ruleOpt.nextDisplay, 14)) {
						engine.getNextObject(pid+i)?.let {
							val centerX = ((4-it.width-1)*fbs)/4-it.minimumBlockX*fbs/2
							val centerY = ((4-it.height-1)*fbs)/4-it.minimumBlockY*fbs/2
							val pY = y+i*2*fbs
							drawPiece(rX+centerX, pY+centerY, it, .5f)
							(pid+i+1).let {n -> if(n%7==0) drawFont(rX, pY, "$n", FONT.NANO, COLOR.YELLOW, .5f)}
						}
					}
				}
				else -> {
					drawFont(cX-16, y-nextHeight, "NEXT", FONT.NANO, COLOR.ORANGE)
					// NEXT2~5
					for(i in 0 until minOf(3, engine.ruleOpt.nextDisplay-1))
						engine.getNextObject(pid+i+1)?.let {
							val pX = rX+(-2+i)*fbs*2
							val cY = (i-2)*fbs/2
							drawPiece(pX+((4-it.width-1)*fbs)/4-it.minimumBlockX*fbs/2, y+cY-(it.maximumBlockY+1)*fbs/2, it, .5f)
							(pid+i+1).let {n -> if(n%7==0) drawFont(pX, y+cY-6, "$n", FONT.NANO, COLOR.YELLOW, .5f)}
						}
					if(engine.ruleOpt.nextDisplay>=5) {
						// NEXT6~
						for(i in 0 until engine.ruleOpt.nextDisplay-3)
							engine.getNextObject(pid+i+4)?.let {

								val pY = (1+i)*32
								drawPiece(rX+((4-it.width-1)*fbs)/4-it.minimumBlockX*fbs/2, y+pY-(it.maximumBlockY+1)*8, it, .5f)
								(pid+i+4).let {n -> if(n%7==0) drawFont(rX, y+pY-6, "$n", FONT.NANO, COLOR.YELLOW, .5f)}
							}
					}
				}
			}
		}

		if(engine.isHoldVisible) {
			// HOLD
			val holdRemain = engine.ruleOpt.holdLimit-engine.holdUsedCount
			val x2 = if(sideNext) x-fbs*3 else x+fbs
			val y2 = if(sideNext) y else y-fbs*3

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
							val centerX = ((4-it.width-1)*fbs)/2-it.minimumBlockX*fbs
							val centerY = ((4-it.height-1)*fbs)/2-it.minimumBlockY*fbs
							drawPiece(x-64+centerX, y+centerY, it, 1f, dark, ow = 1f)
						}
						1 -> {
							val centerX = ((4-it.width-1)*fbs)/4-it.minimumBlockX*fbs/2
							val centerY = ((4-it.height-1)*fbs)/4-it.minimumBlockY*fbs/2
							drawPiece(x2+centerX, y+centerY, it, .5f, dark, ow = 1f)
						}
						else -> drawPiece(x2, y-fbs-(it.maximumBlockY+1)*8, it, .5f, dark, ow = 1f)
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
			if(outlineGhost) drawPieceOutline(px, py, it, blksize, 1f, true)
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
	protected fun drawShadowNexts(x:Int, y:Int, engine:GameEngine) {
		val blksize = engine.blockSize.toFloat()
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
			val size = if(piece.big||engine.displaySize==1) 2 else 1
			val shadowCenter = blksize*(piece.minimumBlockX*2+(piece.width+size))/2

			for(i in 0 until engine.ruleOpt.nextDisplay-1) {
				if(i>=3) break

				engine.getNextObject(engine.nextPieceCount+i)?.let {next ->
					val vPos = (blksize*shadowY).toInt()-(i+1)*24-8
					val nextCenter = blksize*(next.minimumBlockX*2+(next.width+1))/4

					if(vPos>=-blksize/2)
						drawPiece(x+(blksize*shadowX+shadowCenter-nextCenter).toInt(), y+vPos, next, blksize/2f/BS, .25f, .75f)
				}
			}
			engine.holdPieceObject?.let {hold ->
				val nextCenter = blksize*(hold.minimumBlockX*2+(hold.width+1))/4

				drawPiece(x+(blksize*shadowX-nextCenter).toInt(), y+(blksize*shadowY).toInt(), hold, blksize/2f/BS, .25f, .25f)
			}
		}
	}

	/* Ready画面の描画処理 */
	override fun renderReady(engine:GameEngine) {
		if(!engine.allowTextRenderByReceiver) return
		//if(engine.isVisible == false) return;

		if(engine.statc[0]>0) {
			val cy = (engine.fieldHeight-2)/2
			if(engine.statc[0] in engine.readyStart until engine.readyEnd)
				drawMenuFont(engine, 0, cy, "READY", COLOR.WHITE, 2f)
			else if(engine.statc[0] in engine.goStart until engine.goEnd)
				drawMenuFont(engine, 2, cy, "GO!", COLOR.WHITE, 2f)

			drawMenuNano(engine, 0f, cy+2.25f, "TODAY SEEDS:", COLOR.WHITE)
			drawMenuNano(engine, 0f, cy+3f, "${engine.statistics.randSeed}", COLOR.WHITE, 0.75f)
		}
	}

	/* Blockピース移動時の処理 */
	override fun renderMove(engine:GameEngine) {
		if(!engine.isVisible) return

		val offsetX = engine.fX
		val offsetY = engine.fY

		if(engine.statc[0]>1||engine.ruleOpt.moveFirstFrame) {
			val scale = when(engine.displaySize) {
				-1 -> .5f
				0 -> 1f
				1 -> 2f
				else -> 1f
			}
			if(engine.ai!=null&&engine.aiShowHint&&engine.aiHintReady) drawHintPiece(offsetX, offsetY, engine, scale)
			if(engine.ghost&&engine.ruleOpt.ghost) drawGhostPiece(offsetX*1f, offsetY*1f, engine, scale)
			drawCurrentPiece(offsetX, offsetY, engine, scale)
		}
	}

	override fun renderLockFlash(engine:GameEngine) {
		/*if(engine.fpf>0)*/ renderMove(engine)
	}

	override fun renderLineClear(engine:GameEngine) {
		if(engine.fpf>0) renderMove(engine)
	}

	override fun renderARE(engine:GameEngine) {
		if(engine.fpf>0) renderMove(engine)
	}

	override fun lineClear(engine:GameEngine, y:Collection<Int>) {
		val s = engine.blockSize
		y.forEach {
			effects.add(Beam(engine.fX, engine.fY+it*s, s*engine.fieldWidth, s))
		}
	}

	override fun blockBreak(engine:GameEngine, blk:Map<Int, Map<Int, Block>>) {
		if(showlineeffect&&engine.displaySize!=-1) {
			blk.forEach {(y, row) ->
				row.forEach {(x, blk) ->
					val color = blk.drawColor
					val sx = engine.fX+x*engine.blockSize
					val sy = engine.fY+y*engine.blockSize
					// 通常Block
					val r = resources
					if(blk.isGemBlock)
						effects.add(FragAnim(ANIM.GEM, sx, sy, (color-Block.COLOR_GEM_RED)%r.pEraseMax, lineeffectspeed))
					// 宝石Block
					else if(!blk.getAttribute(Block.ATTRIBUTE.BONE))
						effects.add(
							FragAnim(
								if(blk.getAttribute(Block.ATTRIBUTE.LAST_COMMIT)) ANIM.SPARK else ANIM.BLOCK,
								sx, sy, maxOf(0, color-Block.COLOR_WHITE)%r.blockBreakMax, lineeffectspeed
							)
						)
					//blockParticles.addBlock(engine, receiver, playerID, blk, j, i, 10, 90, li>=4, localRandom)
					//blockParticles.addBlock(engine, receiver, playerID, blk, j, i, engine.field.width, cY, li, 120)
				}
			}
		}
	}

	/* ラインを消す演出の処理 */
	override fun calcScore(engine:GameEngine, event:ScoreEvent?) {
		event ?: return
		val w = engine.fieldWidth*getBlockSize(engine)/2
		val sx = engine.fX+w

		val dir = maxOf(-w*2, minOf(w*2, (320-sx)*w/128))
		val sy = engine.fY+getBlockSize(engine)/2*
			when {
				event.lines==0 -> engine.nowPieceBottomY*2
				event.split -> engine.field.lastLinesTop*2
				else -> (engine.field.lastLinesTop+engine.field.lastLinesBottom)
			}
		effects.add(PopupAward(sx, sy, event, if(event.lines==0) engine.speed.are else engine.speed.lineDelay, dir))
	}

	override fun addScore(x:Int, y:Int, pts:Int, color:COLOR) {
		if(pts!=0) effects.add(PopupPoint(x, y, pts, color.ordinal))
	}

	override fun addCombo(x:Int, y:Int, pts:Int, type:PopupCombo.CHAIN, ex:Int) {
		if(pts>0) effects.add(PopupCombo(x, y, pts, type, ex))
	}

	override fun shootFireworks(engine:GameEngine, x:Int, y:Int, color:COLOR) {
		if(heavyeffect) {
			val col = ParticleEmitterBase.colorBy(color)
			effects.add(Fireworks(x, y, col[0], col[1], col[2], 255, col[3]))
		} else effects.add(FragAnim(ANIM.HANABI, x, y, color.ordinal))
		super.shootFireworks(engine, x, y, color)
	}

	override fun bravo(engine:GameEngine) {
		effects.add(PopupBravo(engine.fX, engine.fY))
		super.bravo(engine)
	}

	/* EXCELLENT画面の描画処理 */
	override fun renderExcellent(engine:GameEngine) {
		if(!engine.allowTextRenderByReceiver) return
		if(!engine.isVisible) return

		val cY = (engine.fieldHeight-1)
		if(engine.owner.players<=1)
			drawMenuFont(engine, 0f, cY/3f, "EXCELLENT!", COLOR.RAINBOW, 1f)
		else drawMenuFont(engine, 0f, cY/2f, "You WIN!", COLOR.ORANGE, 1f)

	}

	/* game over画面の描画処理 */
	override fun renderGameOver(engine:GameEngine) {
		if(!engine.allowTextRenderByReceiver||!engine.isVisible) return
		val offsetX = engine.fX
		val offsetY = engine.fY
		if(engine.lives>0&&engine.gameActive) {
			drawDirectFont(offsetX+4, offsetY+156, "LEFT", COLOR.WHITE, 1f)
			drawDirectFont(offsetX+128, offsetY+148, ((engine.lives-1)%10).toString(), COLOR.WHITE, 2f)
		} else if(engine.statc[0]>=engine.statc[1])
			when {
				engine.owner.players<2 -> if(engine.ending==0)
					drawDirectFont(offsetX+12, offsetY+156, "GAME OVER", COLOR.RED, 1f)
				else drawDirectFont(offsetX+28, offsetY+156, "THE END", COLOR.WHITE, 1f)
				engine.owner.winner==-2 -> drawDirectFont(offsetX+48, offsetY+156, "DRAW", COLOR.PURPLE, 1f)
				engine.owner.players<3 -> drawDirectFont(offsetX+16, offsetY+156, "You Lost", COLOR.RED, 1f)
			}
	}

	/* Render results screen処理 */
	override fun renderResult(engine:GameEngine) {
		if(!engine.allowTextRenderByReceiver) return
		if(!engine.isVisible) return

		var tempColor:COLOR = if(engine.statc[0]==0) COLOR.RED else COLOR.WHITE

		drawDirectFont(
			engine.fX+12,
			engine.fY+340, "RETRY", tempColor, 1f
		)

		tempColor = if(engine.statc[0]==1) COLOR.RED else COLOR.WHITE
		drawDirectFont(
			engine.fX+108,
			engine.fY+340, "END", tempColor, 1f
		)
	}

	/* fieldエディット画面の描画処理 */
	override fun renderFieldEdit(engine:GameEngine) {
		val x = engine.fX+4f+engine.mapEditX*getBlockSize(engine)
		val y = engine.fY+52f+engine.mapEditY*getBlockSize(engine)
		val bright = if(engine.mapEditFrames%60>=30) -.5f else -.2f
		drawBlock(x, y, engine.mapEditColor, engine.skin, false, bright, 1f, 1f)
	}
	/* 各 frame 最初の描画処理 */
	override fun renderFirst(engine:GameEngine) {
		if(engine.playerID==0) drawBG(engine)

		// NEXTなど
		if(!engine.owner.menuOnly&&engine.isVisible) {
			val offsetX = engine.fX
			val offsetY = engine.fY

			drawFrame(offsetX, offsetY, engine)
			if(engine.displaySize!=-1) drawNext(offsetX+engine.fieldXOffset, offsetY, engine)
			drawField(offsetX, offsetY, engine, engine.displaySize)
			engine.statc.forEachIndexed {i, it ->
				printFontSpecific(offsetX-32, offsetY+i*10, String.format("%3d", it), FONT.NANO, COLOR.WHITE, 0.7f, 0.75f)
			}

			if(nextshadow&&engine.ghost&&engine.ruleOpt.ghost&&engine.gameActive)
				drawShadowNexts(offsetX, offsetY, engine)

		}
	}
	/* 各 frame の最後に行われる処理 */
	override fun onLast(engine:GameEngine) {
		if(engine.playerID==engine.owner.players-1) effectUpdate()
	}

	/** Update effects */
	private fun effectUpdate() {
		effects.removeAll {it.update(this)}
	}
	/* 各 frame の最後に行われる描画処理 */
	override fun renderLast(engine:GameEngine) {
		if(engine.playerID==engine.owner.players-1) effectRender()
	}

	protected open fun effectRender() {
		effects.forEachIndexed {i, it -> it.draw(i, this)}
	}

	open fun drawBlendAdd(unit:()->Unit) {
		unit()
	}

	protected abstract fun printFontSpecific(x:Int, y:Int, str:String, font:FONT, color:COLOR, scale:Float, alpha:Float)

	protected abstract fun printTTFSpecific(x:Int, y:Int, str:String, color:COLOR, size:Int, alpha:Float)
	protected fun printTTFSpecific(x:Int, y:Int, str:String, color:COLOR, scale:Float, alpha:Float) =
		printTTFSpecific(x, y, str, color, (scale*BaseFontTTF.FONT_SIZE).toInt(), alpha)

	protected abstract fun doesGraphicsExist():Boolean

	protected abstract fun drawBlockSpecific(x:Float, y:Float, sx:Int, sy:Int, sk:Int, size:Float, darkness:Float, alpha:Float)

	protected abstract fun drawLineSpecific(x:Float, y:Float, sx:Float, sy:Float, color:Int = 0xFFFFFF, alpha:Float = 1f, w:Float = 1f)

	protected fun drawLineSpecific(x:Int, y:Int, sx:Int, sy:Int, color:Int = 0xFFFFFF, alpha:Float = 1f, w:Int = 1) =
		drawLineSpecific(x.toFloat(), y.toFloat(), sx.toFloat(), sy.toFloat(), color, alpha, w.toFloat())

	/** draw and Fill Rectangle */
	fun drawRect(
		x:Float, y:Float, w:Float, h:Float, color:Int = 0xFFFFFF, alpha:Float = 1f,
		outlineW:Float = 0f, outlineColor:Int = 0x000000
	) {
		fillRectSpecific(x-outlineW/2f, y-outlineW/2f, w+outlineW/2f, h+outlineW/2f, color, alpha)
		if(outlineW>0)
			drawRectSpecific(x-outlineW/2f, y-outlineW/2f, w+outlineW/2f, h+outlineW/2f, outlineColor, alpha, outlineW)

	}

	fun drawRect(
		x:Int, y:Int, w:Int, h:Int, color:Int = 0xFFFFFF, alpha:Float = 1f, outlineW:Int = 0,
		outlineColor:Int = 0x000000
	) =
		drawRect(x.toFloat(), y.toFloat(), w.toFloat(), h.toFloat(), color, alpha, outlineW.toFloat(), outlineColor)

	/** Draw Rectangle Outline*/
	protected abstract fun drawRectSpecific(
		x:Float, y:Float, w:Float, h:Float, color:Int = 0xFFFFFF, alpha:Float = 1f, bold:Float = 1f
	)

	private fun drawRectSpecific(x:Int, y:Int, w:Int, h:Int, color:Int = 0xFFFFFF, alpha:Float = 1f, bold:Int = 0) =
		drawRectSpecific(x.toFloat(), y.toFloat(), w.toFloat(), h.toFloat(), color, alpha, bold.toFloat())
	/** Fiil Rectangle Solid*/
	protected abstract fun fillRectSpecific(x:Float, y:Float, w:Float, h:Float, color:Int = 0xFFFFFF, alpha:Float = 1f)

	private fun fillRectSpecific(x:Int, y:Int, w:Int, h:Int, color:Int = 0xFFFFFF, alpha:Float = 1f) =
		fillRectSpecific(x.toFloat(), y.toFloat(), w.toFloat(), h.toFloat(), color, alpha)

	/** draw and Fill Oval */
	fun drawOval(
		x:Float, y:Float, w:Float, h:Float, color:Int = 0xFFFFFF, alpha:Float = 1f,
		outlineW:Float = 0f, outlineColor:Int = 0x000000
	) {
		fillOvalSpecific(x-outlineW/2f, y-outlineW/2f, w+outlineW/2f, h+outlineW/2f, color, alpha)
		if(outlineW>0)
			drawOvalSpecific(x-outlineW/2f, y-outlineW/2f, w+outlineW/2f, h+outlineW/2f, outlineColor, alpha, outlineW)

	}

	fun drawOval(
		x:Int, y:Int, w:Int, h:Int, color:Int = 0xFFFFFF, alpha:Float = 1f, outlineW:Int = 0,
		outlineColor:Int = 0x000000
	) =
		drawOval(x.toFloat(), y.toFloat(), w.toFloat(), h.toFloat(), color, alpha, outlineW.toFloat(), outlineColor)

	/** Draw Oval Outline*/
	protected abstract fun drawOvalSpecific(
		x:Float, y:Float, w:Float, h:Float, color:Int = 0xFFFFFF, alpha:Float = 1f,
		bold:Float = 1f
	)

	private fun drawOvalSpecific(x:Int, y:Int, w:Int, h:Int, color:Int = 0xFFFFFF, alpha:Float = 1f, bold:Int = 0) =
		drawOvalSpecific(x.toFloat(), y.toFloat(), w.toFloat(), h.toFloat(), color, alpha, bold.toFloat())
	/** Fiil Oval Solid*/
	protected abstract fun fillOvalSpecific(x:Float, y:Float, w:Float, h:Float, color:Int = 0xFFFFFF, alpha:Float = 1f)

	private fun fillOvalSpecific(x:Int, y:Int, w:Int, h:Int, color:Int = 0xFFFFFF, alpha:Float = 1f) =
		fillOvalSpecific(x.toFloat(), y.toFloat(), w.toFloat(), h.toFloat(), color, alpha)

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
		fun getMeterColorHex(meterColor:Int, value:Float):Int {
			var r = meterColor/65536
			var g = meterColor/256%256
			var b = meterColor%256
			when(meterColor) {
				GameEngine.METER_COLOR_LEVEL -> {
					r = (0xFF*maxOf(0f, minOf(value*3-1, 1f))).toInt()
					g = (0xFF*maxOf(0f, minOf(3-value*3f, 1f))).toInt()
					b = (0xFF*maxOf(0f, minOf((1-value*3), 1f))).toInt()
				}
				GameEngine.METER_COLOR_LIMIT -> {//red<yellow<green<cyan
					r = (0xFF*maxOf(0f, minOf((2-value*3), 1f))).toInt()
					g = (0xFF*maxOf(0f, minOf(value*3, 1f))).toInt()
					b = (0xFF*maxOf(0f, minOf((value*3f-2f), 1f))).toInt()
				}
				else -> return meterColor
			}
			return r*0x10000+g*0x100+b
		}

	}
}
