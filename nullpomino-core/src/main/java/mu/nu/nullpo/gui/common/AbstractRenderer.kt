/*
 * Copyright (c) 2010-2021, NullNoname
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
import mu.nu.nullpo.game.component.Field
import mu.nu.nullpo.game.component.Piece
import mu.nu.nullpo.game.event.EventReceiver
import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.gui.common.FragAnim.ANIM
import mu.nu.nullpo.gui.common.PopupCombo.CHAIN

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
	 * @param cint Color number for render
	 * @param skin Skin
	 * @param bone true to use bone block ([][][][])
	 * @param darkness Darkness or brightness
	 * @param alpha Alpha
	 * @param scale Size (.5f, 1f, 2f)
	 * @param attr Attribute
	 */
	/* 1マスBlockを描画 */
	override fun drawBlock(x:Float, y:Float, cint:Int, skin:Int, bone:Boolean, darkness:Float, alpha:Float, scale:Float,
		attr:Int, outline:Float) {
		var sk = skin

		if(!doesGraphicsExist()) return

		if(cint<0) return
		if(sk>=resources.imgBlockListSize) sk = 0

		val isSpecialBlocks = cint>=Block.COLOR.COUNT
		val isSticky = resources.getBlockIsSticky(sk)

		var sx = cint
		if(bone) sx += 9
		var sy = 0
		if(isSpecialBlocks) sx = (cint-Block.COLOR.COUNT+18)

		if(isSticky)
			if(isSpecialBlocks) {
				sx = (cint-Block.COLOR.COUNT)
				sy = 18
			} else {
				sx = 0
				if(attr and Block.ATTRIBUTE.CONNECT_UP.bit>0) sx = sx or 0x1
				if(attr and Block.ATTRIBUTE.CONNECT_DOWN.bit>0) sx = sx or 0x2
				if(attr and Block.ATTRIBUTE.CONNECT_LEFT.bit>0) sx = sx or 0x4
				if(attr and Block.ATTRIBUTE.CONNECT_RIGHT.bit>0) sx = sx or 0x8
				sy = cint
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
	protected open fun drawFrame(x:Int, y:Int, engine:GameEngine, displaysize:Int) {
		// Upと下
		var tmpX = x
		var tmpY = y

		val s = 4*when(displaysize) {
			-1 -> 2
			1 -> 8
			else -> 4
		}

		val fieldW = engine.field.width ?: Field.DEFAULT_WIDTH
		val fieldH = engine.field.height ?: Field.DEFAULT_HEIGHT
		val maxWidth = fieldW*s
		val maxHeight = fieldH*s
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
				tmpX -= 12
				tmpY -= 12
				fi.draw(tmpX, tmpY, tmpX+mW/2, tmpY+(fieldH+2)*s, 0, 0, 96, 352)
				fi.draw(tmpX+mW, tmpY, tmpX+mW/2, tmpY+(fieldH+2)*s, 0, 0, 96, 352)
			}
			GameEngine.FRAME_SKIN_HEBO -> {
				val fi = resources.imgFrameOld[2]

				fi.draw(tmpX, tmpY, (tmpX+s), (tmpY+s), 0, 0, 16, 16)
				tmpX += 16+(fieldW+2)*s
				fi.draw(tmpX, tmpY, (tmpX), (tmpY+s), 32, 0, 48, 16)
				tmpX = x
				tmpY += 16
				fi.draw(tmpX, tmpY, (tmpX+16), (tmpY+fieldH*s), 0, 15, 16, 1)
				fi.draw((tmpX+maxWidth), tmpY, (tmpX+16), (tmpY+fieldH*s), 0, 15, 16, 1)
			}
			else -> if(engine.framecolor>0) {
				val mW = maxWidth+if(showMeter) 8 else 0
				val fi = resources.imgFrame[engine.framecolor]
				val oX = 0
				tmpX = x+4
				tmpY = y
				fi.draw(tmpX, tmpY, tmpX+mW, tmpY+4, (oX+4), 0, (oX+4+4), 4)
				tmpY = y+fieldH*s+4
				fi.draw(tmpX, tmpY, (tmpX+mW), (tmpY+4), (oX+4), 8, (oX+4+4), (8+4))

				// 左と右
				tmpX = x
				tmpY = y+4
				fi.draw(tmpX, tmpY, (tmpX+4), (tmpY+fieldH*s), oX, 4, (oX+4), (4+4))

				tmpX = x+fieldW*s+if(showMeter) 12 else 4
				fi.draw(tmpX, tmpY, (tmpX+4), (tmpY+fieldH*s), (oX+8), 4, (oX+8+4), 4+4)

				// 左上
				tmpX = x
				tmpY = y
				fi.draw(tmpX, tmpY, (tmpX+4), (tmpY+4), oX, 0, (oX+4), 4)

				// 左下
				tmpX = x
				tmpY = y+fieldH*s+4
				fi.draw(tmpX, tmpY, (tmpX+4), (tmpY+4), oX, 8, (oX+4), (8+4))

				if(showMeter) {
					// MeterONのときの右上
					tmpX = x+fieldW*s+12
					tmpY = y
					fi.draw(tmpX, tmpY, (tmpX+4), (tmpY+4), (oX+8), 0, (oX+8+4), 4)

					// MeterONのときの右下
					tmpX = x+fieldW*s+12
					tmpY = y+fieldH*s+4
					fi.draw(tmpX, tmpY, (tmpX+4), (tmpY+4), (oX+8), 8, (oX+8+4), (8+4))

					// 右Meterの枠
					tmpX = x+fieldW*s+4
					tmpY = y+4
					fi.draw(tmpX, tmpY, (tmpX+4), (tmpY+fieldH*s), (oX+12), 4, (oX+12+4), (4+4))

					tmpX = x+fieldW*s+4
					tmpY = y
					fi.draw(tmpX, tmpY, (tmpX+4), (tmpY+4), (oX+12), 0, (oX+12+4), 4)

					tmpX = x+fieldW*s+4
					tmpY = y+fieldH*s+4
					fi.draw(tmpX, tmpY, (tmpX+4), (tmpY+4), (oX+12), 8, (oX+12+4), (8+4))

				} else {
					// MeterOFFのときの右上
					tmpX = x+fieldW*s+4
					tmpY = y
					fi.draw(tmpX, tmpY, (tmpX+4), (tmpY+4), (oX+8), 0, (oX+8+4), 4)

					// MeterOFFのときの右下
					tmpX = x+fieldW*s+4
					tmpY = y+fieldH*s+4
					fi.draw(tmpX, tmpY, (tmpX+4), (tmpY+4), (oX+8), 8, (oX+8+4), (8+4))
				}
			}
		}

		if(showMeter) {
			// 右Meter
			val mH = maxHeight+if(engine.meterValueSub>0||engine.meterValue>0)
				-maxOf(engine.meterValue, engine.meterValueSub) else 0

			tmpX = x+fieldW*s+8
			tmpY = y+4

			if(mH>0) drawRect(tmpX, tmpY, 4, mH, 0)

			val value = minOf(engine.meterValueSub, fieldH*s)
			if(value>0) {

				if(engine.meterValueSub>maxOf(engine.meterValue, 0)) {
					val tmpY = y+fieldH*s+3-(value-1)
					drawRect(tmpX, tmpY, 4, value, getMeterColorAsColor(engine.meterColorSub, value, mH))
				}
				if(engine.meterValue>0) {
					val tmpY = y+fieldH*s+3-(value-1)
					drawRect(tmpX, tmpY, 4, value, getMeterColorAsColor(engine.meterColor, value, mH))
				}
			}
		}
		if(showSpeed||true) {
			//下Meter 残りlockdelayとARE
			val tmpX = x+8f
			val tmpY = y+fieldH*s+8f
			val mW = maxWidth-8f
			fillRectSpecific(tmpX, tmpY, mW, 4f, 0)
			when(engine.stat) {
				GameEngine.Status.MOVE -> if(engine.lockDelay>0) {
					val value = maxOf(0f, mW-engine.lockDelayNow*mW/engine.lockDelay)
					fillRectSpecific(tmpX+(mW-value)/2f, tmpY, value, 4f,
						if(engine.lockDelayNow>0) 0xFFFF00 else 0x00FF00)
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
					fillRectSpecific(tmpX, tmpY, mW, 4f,
						if(engine.ruleOpt.areCancelMove||engine.ruleOpt.areCancelRotate||engine.ruleOpt.areCancelHold) 0xFF8000 else 0xFFFF00)
					val value = maxOf(0f, mW-engine.statc[0]*mW/engine.statc[1])
					fillRectSpecific(tmpX+(mW-value)/2f, tmpY, value, 4f, 0)
				}
				else -> {
				}
			}

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
					drawBlock(x+((x2+bx)*16f*scale), y+((y2+by-z)*16f*scale), b,
						-.1f, .4f, scale*if(engine.big) 2 else 1)
					i++
				}
				if(z==0) drawPiece(x+bx*blksize, y+by*blksize+ys, it, scale*if(engine.big) 2 else 1, -.25f,
					ow = if(engine.statc[0]%2==0) 2f else 0f)
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
	protected fun drawNext(x:Int, y:Int, engine:GameEngine) {
		val fldWidth = engine.fieldWidth+1
		val fldBlkSize = getBlockSize(engine)
		if(engine.isNextVisible)
			when(nextDisplayType) {
				2 -> if(engine.ruleOpt.nextDisplay>=1) {
					val x2 = x+8+fldWidth*fldBlkSize
					//FontNormal.printFont(x2+16,y+40,NullpoMinoSlick.getUIText("InGame_Next"),COLOR_ORANGE,.5f);
					drawFont(x2+16, y+40, "NEXT", FONT.NANO, COLOR.ORANGE)
					for(i in 0 until engine.ruleOpt.nextDisplay) {
						engine.getNextObject(engine.nextPieceCount+i)?.let {piece ->
							val centerX = (64-(piece.width+1)*16)/2-piece.minimumBlockX*16
							val centerY = (64-(piece.height+1)*16)/2-piece.minimumBlockY*16
							drawPiece(x2+centerX, y+48+i*64+centerY, piece, 1f)
						}
					}
				}
				1 -> if(engine.ruleOpt.nextDisplay>=1) {
					val x2 = x+8+fldWidth*fldBlkSize
					//FontNormal.printFont(x2,y+40,NullpoMinoSlick.getUIText("InGame_Next"),COLOR_ORANGE,.5f);
					drawFont(x2, y+40, "NEXT", FONT.NANO, COLOR.ORANGE)
					for(i in 0 until engine.ruleOpt.nextDisplay) {
						engine.getNextObject(engine.nextPieceCount+i)?.let {
							val centerX = (32-(it.width+1)*8)/2-it.minimumBlockX*8
							val centerY = (32-(it.height+1)*8)/2-it.minimumBlockY*8
							drawPiece(x2+centerX, y+48+i*32+centerY, it, .75f)
						}
					}
				}
				else -> {
					// NEXT1
					if(engine.ruleOpt.nextDisplay>=1) {
						//FontNormal.printFont(x+60,y,NullpoMinoSlick.getUIText("InGame_Next"),COLOR_ORANGE,.5f);
						drawFont(x+60, y, "NEXT", FONT.NANO, COLOR.ORANGE)
						engine.getNextObject(engine.nextPieceCount)?.let {
							//int x2 = x + 4 + ((-1 + (engine.field.getWidth() - piece.getWidth() + 1) / 2) * 16);
							val x2 = x+4+engine.getSpawnPosX(engine.field, it)*fldBlkSize //Rules with spawn x modified were misaligned.
							val y2 = y+48-(it.maximumBlockY+1)*16
							drawPiece(x2, y2, it)
						}
					}

					// NEXT2・3
					for(i in 0 until minOf(2, engine.ruleOpt.nextDisplay-1))
						engine.getNextObject(engine.nextPieceCount+i+1)?.let {
							drawPiece(x+124+i*40, y+48-(it.maximumBlockY+1)*8, it, .5f)
						}

					// NEXT4～
					for(i in 0 until engine.ruleOpt.nextDisplay-3) engine.getNextObject(engine.nextPieceCount+i+3)?.let {
						if(showMeter) drawPiece(x+176, y+i*40+88-(it.maximumBlockY+1)*8, it, .5f)
						else drawPiece(x+168, y+i*40+88-(it.maximumBlockY+1)*8, it, .5f)
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
				drawFont(x2, y2, str, FONT.NANO, tempColor)

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

	/** Currently working onBlockOf Peaceghost Draw a
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param engine GameEngineInstance of
	 * @param scale Display magnification
	 */
	protected abstract fun drawGhostPiece(x:Float, y:Float, engine:GameEngine, scale:Float)

	protected abstract fun drawHintPiece(x:Int, y:Int, engine:GameEngine, scale:Float)

	/** Draw shadow nexts
	 * @param x X coord
	 * @param y Y coord
	 * @param engine GameEngine
	 * @param scale Display size of piece
	 */
	protected fun drawShadowNexts(x:Int, y:Int, engine:GameEngine, scale:Float) {
		val blksize = (16*scale).toInt()

		engine.nowPieceObject?.let {piece ->
			val shadowX = engine.nowPieceX
			val shadowY = engine.nowPieceBottomY+piece.minimumBlockY

			for(i in 0 until engine.ruleOpt.nextDisplay-1) {
				if(i>=3) break

				engine.getNextObject(engine.nextPieceCount+i)?.let {next ->
					val size = if(piece.big||engine.displaysize==1) 2 else 1
					val shadowCenter = blksize*piece.minimumBlockX+blksize*(piece.width+size)/2
					val nextCenter = blksize/2*next.minimumBlockX+blksize/2*(next.width+1)/2
					val vPos = blksize*shadowY-(i+1)*24-8

					if(vPos>=-blksize/2)
						drawPiece(x+blksize*shadowX+shadowCenter-nextCenter, y+vPos, next, .5f*scale, .25f, .75f)
				}
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
					if(nextshadow) drawShadowNexts(offsetX+4, offsetY+52, engine, 2f)
					if(engine.ghost&&engine.ruleOpt.ghost) drawGhostPiece(offsetX+4f, offsetY+52f, engine, 2f)
					if(engine.ai!=null&&engine.aiShowHint&&engine.aiHintReady) drawHintPiece(offsetX+4, offsetY+52, engine, 2f)
					drawCurrentPiece(offsetX+4, offsetY+52, engine, 2f)
				}
				0 -> {
					if(nextshadow) drawShadowNexts(offsetX+4, offsetY+52, engine, 1f)
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
		if(engine.fpf>0) renderMove(engine, playerID)
	}

	override fun renderLineClear(engine:GameEngine, playerID:Int) {
		if(engine.fpf>0) renderMove(engine, playerID)
	}

	override fun renderARE(engine:GameEngine, playerID:Int) {
		if(engine.fpf>0) renderMove(engine, playerID)
	}

	override fun lineClear(engine:GameEngine, playerID:Int, y:Int) {
		val s = getBlockSize(engine)
		effects.add(BeamH(fieldX(engine, playerID)+4, fieldY(engine, playerID)+52+y*s,
			getBlockSize(engine)*engine.fieldWidth, s))
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
					FragAnim(ANIM.GEM, sx, sy, (color-Block.COLOR_GEM_RED)%resources.pEraseMax, lineeffectspeed))// 宝石Block
			else if(!blk.getAttribute(Block.ATTRIBUTE.BONE))
				effects.add(FragAnim(if(blk.getAttribute(Block.ATTRIBUTE.LAST_COMMIT)) ANIM.SPARK else ANIM.BLOCK,
					sx, sy, maxOf(0, color-Block.COLOR_WHITE)%resources.blockBreakMax, lineeffectspeed))
			//blockParticles.addBlock(engine, receiver, playerID, blk, j, i, 10, 90, li>=4, localRandom)
			//blockParticles.addBlock(engine, receiver, playerID, blk, j, i, engine.field.width, cY, li, 120)
		}
	}

	/* ラインを消す演出の処理 */
	override fun calcScore(engine:GameEngine, event:GameEngine.ScoreEvent?) {
		event ?: return
		val w = engine.fieldWidth*getBlockSize(engine)/2
		val sx = fieldX(engine)+4+w

		val sy = fieldY(engine)+52+getBlockSize(engine)/2*
			when {
				event.lines==0 -> engine.nowPieceBottomY*2
				event.split -> engine.field.lastLinesTop*2
				else -> (engine.field.lastLinesTop+engine.field.lastLinesBottom)
			}
		effects.add(PopupAward(sx, sy, event, if(event.lines==0) engine.speed.are else engine.speed.lineDelay, w*2))
	}

	override fun addScore(x:Int, y:Int, pts:Int, color:COLOR) {
		effects.add(PopupPoint(x, y, pts, color.ordinal))
	}

	override fun addCombo(x:Int, y:Int, pts:Int, type:CHAIN, ex:Int) {
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
				drawDirectFont(offsetX+4, offsetY+204, "EXCELLENT!", COLOR.ORANGE, 1f)
			else drawDirectFont(offsetX+36, offsetY+204, "You WIN!", COLOR.ORANGE, 1f)
		} else if(engine.owner.players<=1)
			drawDirectFont(offsetX+4, offsetY+80, "EXCELLENT!", COLOR.ORANGE, .5f)
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
							drawDirectFont(offsetX+12, offsetY+204, "GAME OVER", COLOR.WHITE, 1f)
						else drawDirectFont(offsetX+28, offsetY+204, "THE END", COLOR.WHITE, 1f)
						engine.owner.winner==-2 -> drawDirectFont(offsetX+52, offsetY+204, "DRAW", COLOR.GREEN, 1f)
						engine.owner.players<3 -> drawDirectFont(offsetX+20, offsetY+80, "You Lost", COLOR.WHITE, 1f)
					}
				engine.owner.players<2 -> if(engine.ending==0)
					drawDirectFont(offsetX+4, offsetY+204, "GAME OVER", COLOR.WHITE, 1f)
				else drawDirectFont(offsetX+20, offsetY+204, "THE END", COLOR.WHITE, 1f)
				engine.owner.winner==-2 -> drawDirectFont(offsetX+28, offsetY+80, "DRAW", COLOR.GREEN, .5f)
				engine.owner.players<3 -> drawDirectFont(offsetX+12, offsetY+80, "You Lost", COLOR.WHITE, .5f)
			}
	}

	/* Render results screen処理 */
	override fun renderResult(engine:GameEngine, playerID:Int) {
		if(!engine.allowTextRenderByReceiver) return
		if(!engine.isVisible) return

		var tempColor:COLOR = if(engine.statc[0]==0) COLOR.RED else COLOR.WHITE

		drawDirectFont(fieldX(engine, playerID)+12,
			fieldY(engine, playerID)+340, "RETRY", tempColor, 1f)

		tempColor = if(engine.statc[0]==1) COLOR.RED else COLOR.WHITE
		drawDirectFont(fieldX(engine, playerID)+108,
			fieldY(engine, playerID)+340, "END", tempColor, 1f)
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
				drawField(offsetX+4, offsetY+52, engine, engine.displaysize)
			} else {
				drawFrame(offsetX, offsetY, engine, -1)
				drawField(offsetX+4, offsetY+4, engine, -1)
			}
			printFontSpecific(offsetX, offsetY-15, "${engine.statistics.randSeed}", FONT.NANO, COLOR.WHITE, 0.7f, 0.75f)
			engine.statc.forEachIndexed {i, it ->
				printFontSpecific(offsetX-25, offsetY+i*10, String.format("%3d", it), FONT.NANO, COLOR.WHITE, 0.7f, 0.75f)
			}
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

	fun drawAward(x:Int, y:Int, ev:GameEngine.ScoreEvent, anim:Int, alpha:Float = 1f) {
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
			ev.twist==GameEngine.Twister.IMMOBILE_EZ -> {
				ev.piece?.let {drawPiece(x-16, y, it, 0.5f, alpha = alpha)}
				drawDirectFont(x-54, y-8, "EZ", color = COLOR.ORANGE, alpha = alpha)
				drawDirectFont(x+54, y-8, "TRICK", color = COLOR.ORANGE, alpha = alpha)
			}
			else -> {
				ev.piece?.let {drawPiece(x-64, y, it, 0.5f, alpha = alpha)}
				drawDirectFont(x-32, y-8, "-TWISTER",
					color = if(ev.lines==3) getRainbowColor(anim) else if(ev.b2b) COLOR.PINK else COLOR.PURPLE, alpha = alpha)
			}
		}
	}

	fun drawCombo(x:Int, y:Int, pts:Int, type:CHAIN, alpha:Float = 1f) {
		when(type) {
			CHAIN.B2B -> {
				drawFont(x-18, y-15, "SKILL", FONT.NANO, COLOR.RED, .75f, alpha)
				drawDirectNum(x-18, y, String.format("%2d", pts), COLOR.YELLOW, 1.5f, alpha)
				drawFont(x-18, y+20, "CHAIN!", FONT.NANO, COLOR.ORANGE, .75f, alpha)
			}
			CHAIN.COMBO -> {
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

	protected abstract fun drawBlockSpecific(x:Float, y:Float, sx:Int, sy:Int, sk:Int,
		size:Float, darkness:Float, alpha:Float)

	protected abstract fun drawLineSpecific(x:Float, y:Float, sx:Float, sy:Float, color:Int = 0xFFFFFF, alpha:Float = 1f,
		w:Float = 1f)

	protected fun drawLineSpecific(x:Int, y:Int, sx:Int, sy:Int, color:Int = 0xFFFFFF, alpha:Float = 1f, w:Int = 1) =
		drawLineSpecific(x.toFloat(), y.toFloat(), sx.toFloat(), sy.toFloat(), color, alpha, w.toFloat())

	protected fun drawRect(x:Float, y:Float, w:Float, h:Float, color:Int = 0xFFFFFF, alpha:Float = 1f,
		outlineW:Float = 0f, outlineColor:Int = 0x000000) {
		fillRectSpecific(x-outlineW/2f, y-outlineW/2f, w+outlineW/2f, h+outlineW/2f, color, alpha)
		if(outlineW>0)
			drawRectSpecific(x-outlineW/2f, y-outlineW/2f, w+outlineW/2f, h+outlineW/2f, outlineColor, alpha, outlineW)

	}

	protected fun drawRect(x:Int, y:Int, w:Int, h:Int, color:Int = 0xFFFFFF, alpha:Float = 1f, outlineW:Int = 0,
		outlineColor:Int = 0x000000) =
		drawRect(x.toFloat(), y.toFloat(), w.toFloat(), h.toFloat(), color, alpha, outlineW.toFloat(), outlineColor)

	/**
	 * Draw Rectangle Outline
	 */
	protected abstract fun drawRectSpecific(x:Float, y:Float, w:Float, h:Float, color:Int = 0xFFFFFF, alpha:Float = 1f,
		bold:Float = 1f)

	protected fun drawRectSpecific(x:Int, y:Int, w:Int, h:Int, color:Int = 0xFFFFFF, alpha:Float = 1f, bold:Int = 0) =
		drawRectSpecific(x.toFloat(), y.toFloat(), w.toFloat(), h.toFloat(), color, alpha, bold.toFloat())
	/**
	 * Fiil Rectangle Solid
	 */
	protected abstract fun fillRectSpecific(x:Float, y:Float, w:Float, h:Float, color:Int = 0xFFFFFF, alpha:Float = 1f)

	protected fun fillRectSpecific(x:Int, y:Int, w:Int, h:Int, color:Int = 0xFFFFFF, alpha:Float = 1f) =
		fillRectSpecific(x.toFloat(), y.toFloat(), w.toFloat(), h.toFloat(), color, alpha)

	protected open fun drawBlockOutline(i:Int, j:Int, x:Int, y:Int, blksize:Int, blk:Block, outlineType:Int) {
		blk.let {
			val x3 = (x+0*blksize)
			val y3 = (y+0*blksize)
			val ls = blksize-1

			val colorID = getColorByID(it.color)

			fillRectSpecific(x3, y3, blksize, blksize,
				getColorByID(if(it.getAttribute(Block.ATTRIBUTE.BONE)) Block.COLOR.WHITE else it.color ?: Block.COLOR.WHITE), .5f)

			if(!it.getAttribute(Block.ATTRIBUTE.CONNECT_UP)) {
				drawLineSpecific(x3, y3, (x3+ls), y3)
				drawLineSpecific(x3, (y3+1), (x3+ls), (y3+1))
			}
			if(!it.getAttribute(Block.ATTRIBUTE.CONNECT_DOWN)) {
				drawLineSpecific(x3, (y3+ls), (x3+ls), (y3+ls))
				drawLineSpecific(x3, (y3-1+ls), (x3+ls), (y3-1+ls))
			}
			if(!it.getAttribute(Block.ATTRIBUTE.CONNECT_LEFT)) {
				drawLineSpecific(x3, y3, x3, (y3+ls))
				drawLineSpecific((x3+1), y3, (x3+1), (y3+ls))
			}
			if(!it.getAttribute(Block.ATTRIBUTE.CONNECT_RIGHT)) {
				drawLineSpecific((x3+ls), y3, (x3+ls), (y3+ls))
				drawLineSpecific((x3-1+ls), y3, (x3-1+ls), (y3+ls))
			}
			if(it.getAttribute(Block.ATTRIBUTE.CONNECT_LEFT, Block.ATTRIBUTE.CONNECT_UP))
				fillRectSpecific(x3, y3, 2, 2)
			if(it.getAttribute(Block.ATTRIBUTE.CONNECT_LEFT, Block.ATTRIBUTE.CONNECT_DOWN))
				fillRectSpecific(x3, (y3+blksize-2), 2, 2)
			if(it.getAttribute(Block.ATTRIBUTE.CONNECT_RIGHT, Block.ATTRIBUTE.CONNECT_UP))
				fillRectSpecific(x3+blksize-2, y3, 2, 2)
			if(it.getAttribute(Block.ATTRIBUTE.CONNECT_RIGHT, Block.ATTRIBUTE.CONNECT_DOWN))
				fillRectSpecific(x3+blksize-2, y3+blksize-2, 2, 2)
		}
	}

	protected abstract fun drawBadgesSpecific(x:Float, y:Float, type:Int, scale:Float)
	protected abstract fun drawFieldSpecific(x:Int, y:Int, width:Int, viewHeight:Int, blksize:Int, scale:Float,
		outlineType:Int)

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
			is PopupPoint -> if(it.pts>0) drawDirectNum(it.dx.toInt(), it.dy.toInt(), "+${it.pts}", COLOR.values()[it.color],
				alpha = it.alpha)
			else if(it.pts<0) drawDirectNum(it.dx.toInt(), it.dy.toInt(), "${it.pts}", COLOR.RED)
			is PopupBravo //Field Cleaned
			-> {
				drawDirectFont(it.x.toInt()+20, it.y.toInt()+204, "BRAVO!", getRainbowColor((it.anim+4)%9), 1.5f)
				drawDirectFont(it.x.toInt()+52, it.y.toInt()+236, "PERFECT", getRainbowColor(it.anim%9), 1f)
			}
		}
	}

	companion object {

		/** Block colorIDに応じてColor Hexを作成
		 * @param color Block colorID
		 * @return color Hex
		 */
		fun getColorByID(color:EventReceiver.COLOR?):Int = when(color) {
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
		fun getColorByID(color:Block.COLOR?):Int = when(color) {
			Block.COLOR.WHITE -> 0xFFFFFF
			Block.COLOR.RED -> 0xFF0000
			Block.COLOR.ORANGE -> 0xFF8000
			Block.COLOR.YELLOW -> 0xFFFF00
			Block.COLOR.GREEN -> 0x00FF00
			Block.COLOR.CYAN -> 0x00FFFF
			Block.COLOR.BLUE -> 0x0040FF
			Block.COLOR.PURPLE -> 0xAA00FF
			else -> 0
		}

		fun getMeterColorAsColor(meterColor:Int, value:Int, max:Int):Int {
			var r = 0
			var g = 0
			var b = 0
			when(meterColor) {
				GameEngine.METER_COLOR_LEVEL -> {
					r = (maxOf(0f, minOf((value*3f-max)/max, 1f))*255).toInt()
					g = (maxOf(0f, minOf((max-value)*3f/max, 1f))*255).toInt()
					b = (maxOf(0f, minOf((max-value*3f)/max, 1f))*255).toInt()
				}
				GameEngine.METER_COLOR_LIMIT -> {//red<yellow<green<cyan
					r = (maxOf(0f, minOf((max*2f-value*3f)/max, 1f))*255).toInt()
					g = (minOf(value*3f/max, 1f)*255).toInt()
					b = (maxOf(0f, minOf((value*3f-max*2f)/max, 1f))*255).toInt()
				}
				else -> return meterColor
			}
			return r*0x10000+g*0x100+b
		}
	}
}