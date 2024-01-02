/*
 * Copyright (c) 2010-2024, NullNoname
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
import mu.nu.nullpo.gui.common.fx.FragAnim
import mu.nu.nullpo.gui.common.fx.FragAnim.ANIM
import mu.nu.nullpo.gui.common.fx.PopupAward
import mu.nu.nullpo.gui.common.fx.PopupBravo
import mu.nu.nullpo.gui.common.fx.PopupCombo
import mu.nu.nullpo.gui.common.fx.PopupPoint
import mu.nu.nullpo.gui.common.fx.particles.BlockParticle
import mu.nu.nullpo.gui.common.fx.particles.Fireworks
import mu.nu.nullpo.gui.common.fx.particles.LandingParticles
import mu.nu.nullpo.util.GeneralUtil.toInt

abstract class AbstractRenderer:EventReceiver() {
	internal abstract val resources:ResourceHolder

	protected val showLineEffect get() = heavyEffect>0
	protected val animBG get() = heavyEffect>1
	protected val particle get() = heavyEffect>2

	/** Background display */
	protected val showBG get() = conf.showBG
	/** Show Meter on right field */
	protected val showMeter get() = conf.showMeter
	/** Show ARE meter */
	protected val showSpeed get() = conf.showSpeed

	/** Show ghost piece as Outline */
	protected val outlineGhost get() = conf.outlineGhost

	/** fieldBackgroundの明るさ */
	protected val fieldBgBright get() = conf.fieldBgBright/255f

	/** 縁線を太くする */
	protected val edgeBold get() = conf.edgeBold

	/** Show field BG grid */
	protected val showFieldBgGrid get() = conf.showFieldBgGrid

	/** NEXT欄を暗くする */
	protected val darkNextArea get() = conf.darkNextArea

	/** ghost ピースの上にNEXT表示 */
	protected val nextShadow get() = conf.nextShadow

	/** Line clear effect speed */
	protected val lineEffectSpeed get() = conf.lineEffectSpeed

	/** 回転軸を表示する */
	protected val showCenter get() = conf.showCenter

	/** 操作ブロック降下を滑らかにする */
	protected val smoothFall get() = conf.smoothFall

	/** 高速落下時の軌道を表示する */
	protected val showLocus get() = conf.showLocus

	override fun drawFont(x:Float, y:Float, str:String, font:BaseFont.FONT, color:COLOR, scale:Float, alpha:Float) {
		if(font==BaseFont.FONT.TTF) printTTFSpecific(x, y, str, color, scale, alpha)
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
		val sk = skin%resources.imgBlockListSize

		if(color<0) return

		val isSpecialBlocks = color>=Block.COLOR.COUNT
		val isSticky = resources.getBlockIsSticky(sk)

		val sx = if(isSticky) if(isSpecialBlocks)
			color-Block.COLOR.COUNT
		else
			(attr and Block.ATTRIBUTE.CONNECT_UP.bit>0).toInt()+
				(attr and Block.ATTRIBUTE.CONNECT_DOWN.bit>0).toInt()*2+
				(attr and Block.ATTRIBUTE.CONNECT_LEFT.bit>0).toInt()*4+
				(attr and Block.ATTRIBUTE.CONNECT_RIGHT.bit>0).toInt()*8
		else color+(bone).toInt()*9-(Block.COLOR.COUNT+18)*isSpecialBlocks.toInt()
		val sy = if(isSticky) if(isSpecialBlocks) 18 else color+(bone).toInt()*9 else 0

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
	override fun drawBadges(x:Float, y:Float, width:Int, nums:Int, scale:Float) {
		var n = nums
		var nx = x
		var ny = y
		var z:Int
		val b = FontBadge.b
		var mh = 0
		while(n>0) {
			z = 0
			while(z<b.size-1&&n>=b[z+1]) z++
			val w = FontBadge(z).w*scale
			val h = FontBadge(z).h
			if(h>mh) mh = h
			drawBadgesSpecific(nx, ny, z, scale)
			n -= b[z]
			nx += w
			if(nx>x+width) {
				nx = x
				ny += mh*scale
				mh = 0
			}
		}
	}

	class FontBadge(type:Int) {
		val type = maxOf(0, minOf(b.size-1, type))
		val sx = listOf(0, 10, 20, 30, 0, 10, 20, 30, 0, 20, 40, 0)[type]
		val sy = listOf(0, 0, 0, 0, 14, 14, 14, 14, 24, 24, 0, 44)[type]
		val w = listOf(10, 10, 10, 10, 10, 10, 10, 10, 20, 20, 32, 64)[type]
		val h = listOf(10, 10, 14, 14, 14, 14, 15, 15, 15, 15, 32, 48)[type]

		companion object {
			val b = listOf(1, 5, 10, 50, 100, 500, 1000, 5000, 10000, 50000, 100000, 500000)
		}
	}

	protected abstract fun drawBG(engine:GameEngine)
	protected abstract fun drawFrameSpecific(x:Float, y:Float, engine:GameEngine)
	private fun drawFrame(x:Float, y:Float, engine:GameEngine) {
		// Upと下
		val s = engine.blockSize
		val fieldW = engine.field.width// ?: Field.DEFAULT_WIDTH
		val fieldH = engine.field.height//?: Field.DEFAULT_HEIGHT
		val fW = fieldW*s
		val fH = fieldH*s
		val lX = x+engine.frameX
		val rX = lX+fW
		val tY = y+engine.frameY
		val bY = tY+fH
		// NEXT area background
		if(showBG&&darkNextArea) {
			when(nextDisplayType) {
				0 -> {
					val w = fW+15f
					fillRectSpecific(lX+20, tY, w-40f, nextHeight*-1f, 0)
					for(i in 20 downTo 0)
						drawRectSpecific(lX+20-i, tY, w-40f+i*2, nextHeight*-1f-i, 0, 1-i/20f, 1f)
				}
				1 -> {//side small
					val x2 = lX+s+fieldW*s
					val maxNext = if(engine.isNextVisible) engine.ruleOpt.nextDisplay else 0

					// HOLD area
					if(engine.ruleOpt.holdEnable&&engine.isHoldVisible) {
						fillRectSpecific(lX-32, tY, 32f, 64f, 0)
						for(i in 8 downTo 0)
							drawRectSpecific(lX-32-i, tY-i, 33f+i, 64f+i*2, 0, 1-i/8f, 1f)
					}

					// NEXT area
					if(maxNext>0) {
						fillRectSpecific(x2, tY+8, 32f, 32f*maxNext-16, 0)
						for(i in 8 downTo 0)
							drawRectSpecific(x2-1, tY+8-i, 33f+i, 32f*maxNext-16+i*20, 0, 1-i/8f, 1f)
					}
				}
				2 -> {//side big
					val x2 = lX+s+fW
					val maxNext = if(engine.isNextVisible) engine.ruleOpt.nextDisplay else 0

					// HOLD area
					if(engine.ruleOpt.holdEnable&&engine.isHoldVisible) {
						fillRectSpecific(lX-64, tY, 64f, nextHeight*1f, 0)
						for(i in 8 downTo 0)
							drawRectSpecific(lX-64-i, tY-i, 65f+i, nextHeight+i*2f, 0, i/8f, 1f)
					}
					// NEXT area
					if(maxNext>0) {
						drawRect(x2, tY+8, 64f, 64f*maxNext-16, 0)
						for(i in 8 downTo 0)
							drawRectSpecific(x2-1, tY+8-i, 65f+i, 64f*maxNext-16+i*2, 0, i/8f, 1f)
					}
				}
			}
		}
// Field Background
		if(fieldBgBright>0)
			if(fieldW<=10&&fieldH<=20) {
				var img = resources.imgFieldBG[1]
				if(engine.displaySize==-1) img = resources.imgFieldBG[0]
				if(engine.displaySize==1) img = resources.imgFieldBG[2]
				img.draw(lX, tY, rX, bY, 0f, 0f, 0f+fW, 0f+fH, fieldBgBright)
			} else if(showBG) drawRect(lX, tY, 0f+fW, 0f+fH, 0, fieldBgBright)

		drawFrameSpecific(x, y, engine)

		when(engine.frameColor) {
			GameEngine.FRAME_SKIN_GB -> {
				drawRect(x.toInt(), y.toInt(), fW, fH, 0x888888)
				val fi = resources.imgFrameOld[0]
				for(i in 0..fieldH) {
					val zx = x.toInt()-s
					val zy = y.toInt()+i*s
					fi.draw(zx, zy, zx+s, zy+s, 0, 0, 16, 16)
					fi.draw(zx+fW+s, zy, zx+fW+s*2, zy+s, 0, 0, 16, 16)
					if(i==fieldH)
						for(z in 1..fieldW)
							fi.draw((zx+z*s), zy, zx+(z+1)*s, zy+s, 0, 0, 16, 16)
				}
			}
			GameEngine.FRAME_SKIN_SG -> {
				drawRect(x.toInt(), y.toInt(), fW, fH, 0)

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
			val mH = fH+s
			val mainH = mH*maxOf(0f, minOf(1f, engine.meterValue))
			val subH = mH*maxOf(0f, minOf(1f, engine.meterValueSub))
			val mW = maxOf(2f, s/4f)
			val zx = rX+s/2f-mW/2f
			val zy = tY-s/2f
			drawRect(zx, zy, mW, mH.toFloat(), 0)

			if(engine.meterValueSub>engine.meterValue) {
				drawRect(
					zx+mW/2, zy+mH-subH, mW/2, subH,
					getMeterColorHex(engine.meterColorSub, engine.meterValueSub)
				)
			}
			if(engine.meterValue>0) {
				drawRect(
					zx, zy+mH-mainH, mW, mainH,
					getMeterColorHex(engine.meterColor, engine.meterValue)
				)
			}
			if(engine.meterValueSub>0&&engine.meterValueSub<=engine.meterValue) {
				val ov = minOf(mainH, subH)
				drawRect(
					zx+mW/2f, zy+mH-ov, mW, ov,
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
//			engine.playerID
			val g = spd.gravity
			val d = spd.denominator
			drawMenuNum(engine, 0, zy, if(g<0||d<0) fieldH*1f else g*1f/d, 3 to 4)
			drawMenuSpeed(engine, 1, zy, g, d, 5)

			for(i in 0..1) {
				val show = if(i==0) "ARE" to spd.are else "LINE" to spd.areLine

				drawMenuNum(engine, 6+i*3, zy, String.format(if(i==0) "%2d/" else "%2d", show.second))
				drawMenuNano(engine, 5+i*2.5f, zy+.5f, show.first, COLOR.WHITE, .5f)
			}
			for(i in 0..2) {
				val show = when(i) {
					0 -> "LINE" to spd.lineDelay
					1 -> "LOCK" to spd.lockDelay
					else -> "DAS" to spd.das
				}
				drawMenuNum(engine, 8-i*3, zy+1, String.format(if(i==1) "%2d+" else "%2d", show.second))
				drawMenuNano(engine, 6.5f-i*3, zy+1f, show.first, COLOR.WHITE, .5f)
			}
			drawMenuNano(engine, 0f, zy+1.5f, "DELAYS", COLOR.WHITE, .5f)
		}

		for(i in 0..engine.lives)
			drawDia(
				lX-.5f*s, bY-(1+i+.5f)*s, s*2f/3, s*2f/3,
				engine.statc[0]/(14f-engine.speed.rank*10f), alpha = .75f,
				outlineColor = getColorByID(COLOR.all.let {it[(i+1)%(it.size-1)]}), outlineW = 2f
			)
	}

	/** Currently working onBlockDraw a piece
	 * (Y-coordinateThe0MoreBlockDisplay only)
	 * @param x X-coordinate of base field
	 * @param y Y-coordinate of base field
	 * @param engine GameEngineInstance of
	 * @param scale Display magnification
	 */
	protected fun drawCurrentPiece(x:Float, y:Float, engine:GameEngine, scale:Float) {
		val blkSize = (engine.blockSize*scale)
		val bx = engine.nowPieceX
		val by = engine.nowPieceY
		var g = engine.fpf

		val isRetro = engine.frameColor in GameEngine.FRAME_SKIN_SG..GameEngine.FRAME_SKIN_GB
		val ys = if(!smoothFall||by>=engine.nowPieceBottomY||isRetro) 0f else engine.gCount*blkSize/engine.speed.denominator%blkSize
		//if(engine.harddropFall>0)g+=engine.harddropFall;
		if(!showLocus||isRetro) g = 0

		engine.nowPieceObject?.let {p ->
			p.dataX[p.direction].zip(p.dataY[p.direction].withIndex()).groupBy({it.first}, {it.second})
				.forEach {(x2, c) ->
					val (i, y2) = c.minBy {it.value}
					val b = Block(p.block[i]).apply {darkness = 0f}
					for(z in 0..g) drawBlock(
						x+((x2+bx)*blkSize), y+((y2+by-z)*blkSize), b,
						-.1f, .4f, scale*if(engine.big) 2 else 1
					)
				}
			drawPiece(
				x+bx*blkSize, y+by*blkSize+ys, p, scale*if(engine.big) 2 else 1, -.25f,
				ow = if(engine.statc[0]%2==0||engine.holdDisable) 2f else 0f
			)
			if(engine.nowPieceSteps<10) drawDirectNano(
				x+(bx+p.spinCX-.1f)*blkSize, y+by*blkSize+ys,
				"${engine.nowPieceSteps}/${p.finesseLimit(engine.nowPieceX)}", COLOR.WHITE, .5f
			)
			if(showCenter) drawDia(
				x+(bx+p.spinCX+.5f)*blkSize, y+(by+p.spinCY+.5f)*blkSize+ys, blkSize*2/3, blkSize*2/3,
				engine.statc[0]/(14f-engine.speed.rank*10f), alpha = .75f,
				outlineColor = getColorByID(p.block[engine.statc[0]%p.block.size].run {
					if(getAttribute(Block.ATTRIBUTE.BONE)) Block.COLOR.WHITE else color
				}), outlineW = 2f
			)
		}
	}

	/** fieldのBlockを描画
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param engine GameEngineのインスタンス
	 */
	protected fun drawField(x:Float, y:Float, engine:GameEngine, size:Int, scale:Float = 1f) {
		var blkSize = engine.blockSize
		var zoom = scale
		if(size==-1) {
			blkSize /= 2
			zoom /= 2
		} else if(size==1) {
			blkSize *= 2
			zoom *= 2
		}

		val field = engine.field
		val width = field.width
		val height = field.height
		var viewHeight = field.height

		if(engine.heboHiddenEnable&&engine.gameActive) viewHeight -= engine.heboHiddenYNow

		val outlineType = if(engine.owBlockOutlineType==-1) engine.blockOutlineType else engine.owBlockOutlineType

		for(i in -field.hiddenHeight..<viewHeight)
			for(j in 0..<width) {
				val x2 = x+j*blkSize
				val y2 = y+i*blkSize

				field.getBlock(j, i)?.also {
					if(it.getAttribute(Block.ATTRIBUTE.WALL))
						drawBlock(x2, y2, 0, it.skin, it.getAttribute(Block.ATTRIBUTE.BONE), it.darkness, it.alpha, zoom, it.aint)
					else if(it.color!=null) {
						if(engine.owner.replayMode&&engine.owner.replayShowInvisible)
							drawBlockForceVisible(x2, y2, it, scale)
						else if(it.getAttribute(Block.ATTRIBUTE.VISIBLE)) drawBlock(x2, y2, it, scale = scale)

						if(it.getAttribute(Block.ATTRIBUTE.OUTLINE)&&!it.getAttribute(Block.ATTRIBUTE.BONE)) {
							val ls = blkSize-1
							val w = if(edgeBold) 2f else 1f
							when(outlineType) {
								GameEngine.BLOCK_OUTLINE_NORMAL -> {
									if(field.getBlockEmpty(j, i-1)) drawLineSpecific(x2, y2, x2+ls, y2, w = w)
									if(field.getBlockEmpty(j, i+1)) drawLineSpecific(x2, y2+ls, x2+ls, y2+ls, w = w)
									if(field.getBlockEmpty(j-1, i)) drawLineSpecific(x2, y2, x2, y2+ls, w = w)
									if(field.getBlockEmpty(j+1, i)) drawLineSpecific(x2+ls, y2, x2+ls, y2+ls, w = w)
								}
								GameEngine.BLOCK_OUTLINE_CONNECT -> {
									if(!it.getAttribute(Block.ATTRIBUTE.CONNECT_UP)) drawLineSpecific(x2, y2, x2+ls, y2, w = w)
									if(!it.getAttribute(Block.ATTRIBUTE.CONNECT_DOWN)) drawLineSpecific(x2, y2+ls, x2+ls, y2+ls, w = w)
									if(!it.getAttribute(Block.ATTRIBUTE.CONNECT_LEFT)) drawLineSpecific(x2, y2, x2, y2+ls, w = w)
									if(!it.getAttribute(Block.ATTRIBUTE.CONNECT_RIGHT)) drawLineSpecific(x2+ls, y2, x2+ls, y2+ls, w = w)
								}
								GameEngine.BLOCK_OUTLINE_SAMECOLOR -> {
									val color = getColorByID(it.color ?: Block.COLOR.WHITE)
									if(field.getBlockColor(j, i-1)!=it.color) drawLineSpecific(x2, y2, x2+ls, y2, color, w = w)
									if(field.getBlockColor(j, i+1)!=it.color) drawLineSpecific(x2, y2+ls, x2+ls, y2+ls, color, w = w)
									if(field.getBlockColor(j-1, i)!=it.color) drawLineSpecific(x2, y2, x2, y2+ls, color, w = w)
									if(field.getBlockColor(j+1, i)!=it.color) drawLineSpecific(x2+ls, y2, x2+ls, y2+ls, color, w = w)
								}
							}
						}
					}
				}
			}
		drawFieldSpecific(x, y, width, viewHeight, blkSize, zoom, outlineType)

		// BunglerHIDDEN
		field.let {
			if(engine.heboHiddenEnable&&engine.gameActive) {
				var maxY = engine.heboHiddenYNow
				if(maxY>height) maxY = height
				for(i in 0..<maxY)
					for(j in 0..<width)
						drawBlock(x+j*blkSize, y+(height-1-i)*blkSize, (x+y).toInt()%2, 0, false, 0f, 1f, zoom)
			}
		}
	}

	/** NEXTを描画
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param engine GameEngineのインスタンス
	 */
	private fun drawNext(x:Float, y:Float, engine:GameEngine) {
		val fldWidth = engine.field.width+1
		val fbs = engine.blockSize
		val cps = fldWidth*fbs
		val cX = x+cps/2
		val rX = x+cps
		if(engine.isNextVisible&&engine.ruleOpt.nextDisplay>=1) {
			val pid = engine.nextPieceCount
			drawFont(x, y-fbs, "${engine.nextPieceArraySize}", BaseFont.FONT.NANO, COLOR.ORANGE, .5f)
			printFontSpecific(x, y-fbs/2, "${engine.statistics.randSeed}", BaseFont.FONT.NANO, COLOR.WHITE, .5f, .5f)
			engine.getNextObject(pid)?.let {
				//int x2 = x + 4 + ((-1 + (engine.field.getWidth() - piece.getWidth() + 1) / 2) * 16);
				val x2 = x+engine.getSpawnPosX(it, engine.field)*fbs //Rules with spawn x modified were misaligned.
				val y2 = y+engine.getSpawnPosY(it, engine.field)*fbs
				if(engine.ruleOpt.fieldCeiling||!engine.ruleOpt.pieceEnterAboveField)
					drawPieceOutline(x2, y2, it, 1f, .5f)
				else drawPiece(x2, y2, it)
				drawFont(cX-45, y-26, "%3d".format(pid), BaseFont.FONT.NANO, if(pid%7==0) COLOR.YELLOW else COLOR.WHITE, .75f)
			}
			if(engine.ruleOpt.nextDisplay>1) when(nextDisplayType) {
				2 -> {
					drawFont(rX, y-fbs/2, "QUEUE", BaseFont.FONT.NANO, COLOR.ORANGE)
					for(i in 0..<minOf(engine.ruleOpt.nextDisplay, 14)) {
						engine.getNextObject(pid+i)?.let {
							val centerX = ((4-it.width-1)*fbs)/2-it.minimumBlockX*fbs
							val centerY = ((4-it.height-1)*fbs)/2-it.minimumBlockY*fbs
							val pY = y+i*3*fbs
							drawPiece(rX+centerX, pY+centerY, it, 1f)
							(pid+i+1).let {n -> if(n%7==0) drawFont(rX, pY, "$n", BaseFont.FONT.NANO, COLOR.YELLOW, .5f)}
						}
					}
				}
				1 -> {
					drawFont(rX, y, "QUEUE", BaseFont.FONT.NANO, COLOR.ORANGE)
					for(i in 0..<minOf(engine.ruleOpt.nextDisplay, 14)) {
						engine.getNextObject(pid+i)?.let {
							val centerX = ((4-it.width-1)*fbs)/4-it.minimumBlockX*fbs/2
							val centerY = ((4-it.height-1)*fbs)/4-it.minimumBlockY*fbs/2
							val pY = y+i*2*fbs
							drawPiece(rX+centerX, pY+centerY, it, .5f)
							(pid+i+1).let {n -> if(n%7==0) drawFont(rX, pY, "$n", BaseFont.FONT.NANO, COLOR.YELLOW, .5f)}
						}
					}
				}
				else -> {
					drawFont(cX-16, y-nextHeight, "NEXT", BaseFont.FONT.NANO, COLOR.ORANGE)
					// NEXT1~4
					for(i in (0..<minOf(3, engine.ruleOpt.nextDisplay-1)).reversed())
						engine.getNextObject(pid+i+1)?.let {
							val pX = rX+(-2+i)*fbs*2
							val cY = (i-2)*fbs/2
							drawPiece(pX+((4-it.width-1)*fbs)/4-it.minimumBlockX*fbs/2, y+cY-(it.maximumBlockY+1)*fbs/2, it, .75f)
							(pid+i+1).let {n -> if(n%7==0) drawFont(pX, y+cY-6, "$n", BaseFont.FONT.NANO, COLOR.YELLOW, .75f)}
						}
					if(engine.ruleOpt.nextDisplay>=5) {
						// NEXT5~
						for(i in (0..<engine.ruleOpt.nextDisplay-3).reversed())
							engine.getNextObject(pid+i+4)?.let {
								val pY = (1+i)*24
								drawPiece(rX+((4-it.width-1)*fbs)/4-it.minimumBlockX*fbs/2, y+pY-(it.maximumBlockY+1)*8, it, .5f)
								(pid+i+4).let {n -> if(n%7==0) drawFont(rX, y+pY-6, "$n", BaseFont.FONT.NANO, COLOR.YELLOW, .5f)}
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
				drawFont(x2, y2, str, BaseFont.FONT.NANO, tempColor, .75f)

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
		val px = (engine.nowPieceX*blksize+x)
		val py = (engine.nowPieceBottomY*blksize+y)
		engine.nowPieceObject?.let {
			drawPiece(px, py, it, scale, .3f, .7f)
			if(outlineGhost) drawPieceOutline(px, py, it, blksize, 1f, true)
		}
	}

	protected open fun drawHintPiece(x:Float, y:Float, engine:GameEngine, scale:Float) {
		val ai = engine.ai ?: return
		engine.aiHintPiece?.let {
			val blkSize = BS*scale
			val px = (ai.bestX*blkSize+x)
			val py = (ai.bestY*blkSize+y)
			it.direction = ai.bestRt
			drawPieceOutline(px, py, it, blkSize)
		}
	}

	/** Draw shadow nexts
	 * @param x X coord
	 * @param y Y coord
	 * @param engine GameEngine
	 */
	protected fun drawShadowNexts(x:Float, y:Float, engine:GameEngine) {
		val blkSize = engine.blockSize.toFloat()
		engine.getNextObject(engine.nextPieceCount)?.let {next ->
			val sx = engine.getSpawnPosX(next)
			val sy = next.getBottom(sx, engine.getSpawnPosY(next), engine.field)
			drawPieceOutline(x+(blkSize*sx).toInt(), y+(blkSize*sy).toInt(), next, blkSize, .5f, true)
		}
		engine.holdPieceObject?.let {hold ->
			val sx = engine.getSpawnPosX(hold)
			val sy = hold.getBottom(sx, engine.getSpawnPosY(hold), engine.field)
			drawPieceOutline(x+(blkSize*sx).toInt(), y+(blkSize*sy).toInt(), hold, blkSize, .5f/3)
		}
		engine.nowPieceObject?.let {piece ->
			val shadowX = engine.nowPieceX
			val shadowY = engine.nowPieceBottomY+piece.minimumBlockY
			val size = if(piece.big||engine.displaySize==1) 2 else 1
			val shadowCenter = blkSize*(piece.minimumBlockX*2+(piece.width+size))/2

			for(i in 0..<engine.ruleOpt.nextDisplay-1) {
				if(i>=3) break

				engine.getNextObject(engine.nextPieceCount+i)?.let {next ->
					val vPos = (blkSize*shadowY).toInt()-(i+1)*24-8
					val nextCenter = blkSize*(next.minimumBlockX*2+(next.width+1))/4

					if(vPos>=-blkSize/2)
						drawPiece(x+(blkSize*shadowX+shadowCenter-nextCenter).toInt(), y+vPos, next, blkSize/2f/BS, .25f, .75f)
				}
			}
			engine.holdPieceObject?.let {hold ->
				val nextCenter = blkSize*(hold.minimumBlockX*2+(hold.width+1))/4

				drawPiece(x+(blkSize*shadowX-nextCenter).toInt(), y+(blkSize*shadowY).toInt(), hold, blkSize/2f/BS, .25f, .25f)
			}
		}
	}

	/* Ready画面の描画処理 */
	override fun renderReady(engine:GameEngine) {
		if(!engine.allowTextRenderByReceiver) return
		//if(engine.isVisible == false) return;

		if(engine.statc[0]>0) {
			val cy = (engine.fieldHeight-2)/2
			if(engine.statc[0] in engine.readyStart..<engine.readyEnd)
				drawMenuFont(engine, 0, cy, "READY", COLOR.WHITE, 2f)
			else if(engine.statc[0] in engine.goStart..<engine.goEnd)
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
			val scale = getScaleF(engine.displaySize)
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

	private fun locusParticle(engine:GameEngine, fall:Int, alpha:Int = 225, lifetime:IntRange, vel:Float = alpha*0.0072f) {
		if(particle) engine.nowPieceObject?.let {p ->
			p.dataX[p.direction].zip(p.dataY[p.direction].withIndex()).groupBy({it.first}, {it.second})
				.mapValues {it.value.filterNot {(i, _) -> p.block[i].getAttribute(Block.ATTRIBUTE.BONE)}}
				.filterNot {it.value.isEmpty()}.forEach {(x2, c) ->
					val (i, y2) = c.minBy {it.value}
					val col = Fireworks.colorBy(getBlockColor(p.block[i]))
					val x = engine.fX+(engine.nowPieceX+x2+.5f)*engine.blockSize
					for(z in 0..fall) {
						val y = engine.fY+(engine.nowPieceY+y2-z+.5f)*engine.blockSize
						efxFG.addAll(
							Fireworks(
								x, y, col[0], col[1], col[2], alpha, 32, -.1f, .95f, lifetime.first, lifetime.last,
								vel, 0..0
							).particles
						)
					}
				}
		}
	}

	override fun afterPieceFall(engine:GameEngine, fpf:Int) {
		locusParticle(engine, fpf, 128, 8..16)
		super.afterPieceFall(engine, fpf)
	}

	override fun afterSoftDropFall(engine:GameEngine, fall:Int) {
		locusParticle(engine, fall, 160, 16..26)
		super.afterSoftDropFall(engine, fall)
	}

	override fun afterHardDropFall(engine:GameEngine, fall:Int) {
		locusParticle(engine, fall, 200, 24..36)
		super.afterHardDropFall(engine, fall)
	}

	override fun pieceLocked(engine:GameEngine, pX:Int, pY:Int, p:Piece, lines:Int, finesse:Boolean) {
		if(particle) efxFG.addAll(
			p.dataX[p.direction].zip(p.dataY[p.direction].withIndex()).groupBy({it.first}, {it.second})
				.mapValues {it.value.filterNot {(i, _) -> p.block[i].getAttribute(Block.ATTRIBUTE.BONE)}}
				.filterNot {it.value.isEmpty()}.flatMap {(x2, c) ->
					val x = engine.fX+(pX+x2+.5f)*engine.blockSize
					val fw = c.flatMap {(i, y2) ->
						val col = Fireworks.colorBy(getBlockColor(p.block[i]))
						val y = engine.fY+(pY+y2+.5f)*engine.blockSize
						Fireworks(
							x, y, col[0], col[1], col[2], 255, col[if(finesse) 3 else 4], -.05f, .95f,
							27, 45, 4.1f, 2..4
						).let {if(finesse) it.particles+it else listOf(it)}
					}
					val (i, y2) = c.maxBy {it.value}
					val col = Fireworks.colorBy(getBlockColor(p.block[i]))
					val px = engine.fX+(pX+x2)*engine.blockSize
					val prx = engine.fX+(pX+x2+1)*engine.blockSize
					val py = engine.fY+(pY+y2+1)*engine.blockSize
					val lp = LandingParticles(
						3, px, prx, py, 0f, col[0], col[1], col[2],
						235, 45, .44f, .66f
					).particles
					fw+lp
				}
		)
	}

	override fun lineClear(engine:GameEngine, y:Collection<Int>) {
		val s = engine.blockSize
		efxFG.addAll(y.map {Beam(engine.fX, engine.fY+it*s, s*engine.fieldWidth, s, if(particle) .5f else 1f)})
	}

	override fun blockBreak(engine:GameEngine, blk:Map<Int, Map<Int, Block>>) {
		if(showLineEffect&&engine.displaySize!=-1&&engine.skin!=GameEngine.FRAME_SKIN_GB) {
			val s = engine.blockSize
			efxFG.addAll(blk.flatMap {(y, row) ->
				row.flatMap a@{(x, blk) ->
					val sx = engine.fX+x*s
					val sy = engine.fY+y*s
					if(particle) {
						if(blk.getAttribute(Block.ATTRIBUTE.BONE)) Fireworks.colorBy(COLOR.GREEN).let {col ->
							LandingParticles(
								5, sx, sx+s, sy+s/2f, s*.5f, col[0], col[1], col[2],
								235, 25, .44f, .66f
							).particles
						} else Fireworks.colorBy(getBlockColor(blk)).let {col ->
							Fireworks(
								sx+s/2f, sy+s/2f, col[0], col[1], col[2], 255, 32, .1f, .95f, 25, 45,
								4.5f, 4..10
							).let {it.particles+it}
						}
					} else {
						val color = blk.drawColor
						val r = resources
						listOf(
							if(blk.isGemBlock) // 宝石Block
								FragAnim(ANIM.GEM, sx, sy, (color-Block.COLOR_GEM_RED)%r.pEraseMax, lineEffectSpeed)
							else // 通常Block
								FragAnim(
									if(blk.getAttribute(Block.ATTRIBUTE.LAST_COMMIT)) ANIM.SPARK else ANIM.BLOCK,
									sx, sy, maxOf(0, color-Block.COLOR_WHITE)%r.blockBreakMax, lineEffectSpeed
								)
						)
					}
				}
			})
//			if(engine.skin==)
			val btype = engine.skin in GameEngine.FRAME_SKIN_HEBO..GameEngine.FRAME_SKIN_SG||(engine.owner.mode?.gameIntensity ?: 0)<0
			efxBG.addAll(
				BlockParticle.Mapper(
					engine, this, blk, if(btype) BlockParticle.Type.TGM else BlockParticle.Type.DTET,
					blk.size>=4, if(btype) 4f else 3.2f+blk.size
				).particles
			)
		}
	}

	/* ラインを消す演出の処理 */
	override fun calcScore(engine:GameEngine, event:ScoreEvent?) {
		event ?: return
		val w = engine.fieldWidth*engine.blockSize/2
		val sx = engine.fX+w

		val dir = maxOf(-w*2f, minOf(w*2f, (320-sx)*w/128))
		val sy = engine.fY+engine.blockSize/2*
			when {
				event.lines==0 -> engine.nowPieceBottomY*2
				event.split -> engine.field.lastLinesTop*2
				else -> (engine.field.lastLinesTop+engine.field.lastLinesBottom)
			}
		efxFG.add(PopupAward(sx, sy, event, if(event.lines==0) engine.speed.are else engine.speed.lineDelay, dir))
	}

	override fun addScore(x:Int, y:Int, pts:Int, color:COLOR) {
		if(pts!=0) efxFG.add(PopupPoint(x, y, pts, color.ordinal))
	}

	override fun addCombo(x:Int, y:Int, pts:Int, type:PopupCombo.CHAIN, ex:Int) {
		if(pts>0) efxFG.add(PopupCombo(x, y, pts, type, ex))
	}

	override fun shootFireworks(engine:GameEngine, x:Float, y:Float, color:COLOR) {
		if(particle) {
			val col = Fireworks.colorBy(color)
			efxFG.addAll(Fireworks(x, y, col[0], col[1], col[2], 255, 128).let {it.particles+it})
		} else efxFG.add(FragAnim(ANIM.HANABI, x, y, color.ordinal))
		super.shootFireworks(engine, x, y, color)
	}

	override fun bravo(engine:GameEngine) {
		val s = engine.blockSize
		efxFG.add(PopupBravo(engine.fX, engine.fY, s*engine.fieldWidth, s*engine.fieldHeight))
		super.bravo(engine)
	}

	/* EXCELLENT画面の描画処理 */
	override fun renderExcellent(engine:GameEngine) {
		if(!engine.allowTextRenderByReceiver) return
		if(!engine.isVisible) return

		val cY = (engine.fieldHeight-1) // 19 : cY/3f = 6.3
		if(engine.owner.players<=1) {
			drawMenuFont(engine, 0f, cY/3f, "EXCELLENT!", COLOR.RAINBOW, 1f)
			engine.owner.mode?.name?.let {
				drawMenuNano(engine, -30f, cY/2f-.25f, it, COLOR.RAINBOW, .5f)
				drawMenuNano(engine, -30f, cY/2f+.25f, "MODE COMPLETED", COLOR.RAINBOW, .5f)
			}
		} else drawMenuFont(engine, -3f, cY/2f, "You WIN!", COLOR.ORANGE, 1f)
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
					drawMenuFont(engine, -3f, engine.fieldHeight/2f, "GAME OVER", COLOR.RED, 1f)
				else drawMenuFont(engine, -3f, engine.fieldHeight/2f, "THE END", COLOR.WHITE, 1f)
				engine.owner.winner==-2 -> drawMenuFont(engine, -3f, engine.fieldHeight/2f, "DRAW", COLOR.PURPLE, 1f)
				engine.owner.players<3 -> drawMenuFont(engine, -3f, engine.fieldHeight/2f, "You Lost", COLOR.RED, 1f)
			}
	}

	/* Render results screen処理 */
	override fun renderResult(engine:GameEngine) {
		if(!engine.allowTextRenderByReceiver) return
		if(!engine.isVisible) return

		drawMenuFont(
			engine, .75f, 0f+engine.fieldHeight, "RETRY",
			if(engine.statc[0]==0) COLOR.RAINBOW else COLOR.WHITE, 1f
		)
		drawMenuFont(
			engine, 6.75f, 0f+engine.fieldHeight, "END",
			if(engine.statc[0]==1) COLOR.RAINBOW else COLOR.WHITE, 1f
		)
	}

	/* fieldエディット画面の描画処理 */
	override fun renderFieldEdit(engine:GameEngine) {
		val x = engine.fX+engine.mapEditX*engine.blockSize
		val y = engine.fY+engine.mapEditY*engine.blockSize
		val bright = if(engine.mapEditFrames%6>=3) -.5f else -.2f
		drawBlock(x, y, engine.mapEditColor, engine.skin, false, bright, 1f, 1f)
	}
	/* 各 frame 最初の描画処理 */
	override fun renderFirst(engine:GameEngine) {
		if(engine.playerID==0)
			drawBG(engine)

		// NEXTなど
		if(!engine.owner.menuOnly&&engine.isVisible) {
			val offsetX = engine.fX
			val offsetY = engine.fY

			drawFrame(offsetX, offsetY, engine)
			engine.statc.forEachIndexed {i, it ->
				printFontSpecific(offsetX-20, offsetY+i*8, "%3d".format(it), BaseFont.FONT.NANO, COLOR.WHITE, .5f, .5f)
			}
			efxBG.forEachIndexed {i, it -> it.draw(i, this)}
			if(engine.displaySize!=-1) drawNext(offsetX+engine.fieldXOffset, offsetY, engine)
			drawField(offsetX, offsetY, engine, engine.displaySize)

			if(nextShadow&&engine.ghost&&engine.ruleOpt.ghost&&engine.gameActive)
				drawShadowNexts(offsetX, offsetY, engine)

		} else
			efxBG.forEachIndexed {i, it -> it.draw(i, this)}
	}
	/* 各 frame の最後に行われる処理 */
	override fun onLast(engine:GameEngine) {
		if(engine.playerID==engine.owner.players-1) effectUpdate()
	}

	/** Update effects */
	private fun effectUpdate() {
		efxBG.removeAll {it.update(this)}
		efxFG.removeAll {it.update(this)}
	}
	/* 各 frame の最後に行われる描画処理 */
	override fun renderLast(engine:GameEngine) {
		if(engine.playerID==engine.owner.players-1)
			efxFG.forEachIndexed {i, it -> it.draw(i, this)}
	}

	open fun drawBlendAdd(unit:()->Unit) {
		unit()
	}

	protected abstract fun printFontSpecific(x:Float, y:Float, str:String, font:BaseFont.FONT, color:COLOR, scale:Float, alpha:Float)
	protected fun printFontSpecific(x:Number, y:Number, str:String, font:BaseFont.FONT, color:COLOR, scale:Float, alpha:Float) =
		printFontSpecific(x.toFloat(), y.toFloat(), str, font, color, scale, alpha)

	protected abstract fun printTTFSpecific(x:Float, y:Float, str:String, color:COLOR, scale:Float, alpha:Float)
	protected fun printTTFSpecific(x:Number, y:Number, str:String, color:COLOR, size:Int, alpha:Float) =
		printTTFSpecific(x.toFloat(), y.toFloat(), str, color, size.toFloat()/BaseFontTTF.FONT_SIZE, alpha)

	/***
	 * @param sx BlockSkin grid-X
	 * @param sy BlockSkin grid-Y
	 * @param sk BlockSkin number
	 */
	protected abstract fun drawBlockSpecific(x:Float, y:Float, sx:Int, sy:Int, sk:Int, size:Float, darkness:Float, alpha:Float)

	abstract fun drawLineSpecific(x:Float, y:Float, sx:Float, sy:Float, color:Int = 0xFFFFFF, alpha:Float = 1f, w:Float = 1f)

	fun drawLineSpecific(x:Number, y:Number, sx:Number, sy:Number, color:Int = 0xFFFFFF, alpha:Float = 1f, w:Int = 1) =
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
		x:Number, y:Number, w:Number, h:Number, color:Int = 0xFFFFFF, alpha:Float = 1f, outlineW:Int = 0,
		outlineColor:Int = 0x000000
	) =
		drawRect(x.toFloat(), y.toFloat(), w.toFloat(), h.toFloat(), color, alpha, outlineW.toFloat(), outlineColor)

	/** Draw Rectangle Outline*/
	abstract fun drawRectSpecific(
		x:Float, y:Float, w:Float, h:Float, color:Int = 0xFFFFFF, alpha:Float = 1f, bold:Float = 1f
	)

	fun drawRectSpecific(x:Number, y:Number, w:Number, h:Number, color:Int = 0xFFFFFF, alpha:Float = 1f, bold:Int = 0) =
		drawRectSpecific(x.toFloat(), y.toFloat(), w.toFloat(), h.toFloat(), color, alpha, bold.toFloat())
	/** Fiil Rectangle Solid*/
	abstract fun fillRectSpecific(x:Float, y:Float, w:Float, h:Float, color:Int = 0xFFFFFF, alpha:Float = 1f)

	fun fillRectSpecific(x:Number, y:Number, w:Number, h:Number, color:Int = 0xFFFFFF, alpha:Float = 1f) =
		fillRectSpecific(x.toFloat(), y.toFloat(), w.toFloat(), h.toFloat(), color, alpha)

	/** draw and Fill Rectangle */
	fun drawDia(
		x:Float, y:Float, w:Float, h:Float, angle:Float, color:Int = 0xFFFFFF, alpha:Float = 1f,
		outlineW:Float = 0f, outlineColor:Int = 0x000000
	) {
		fillDiaSpecific(x, y, w, h, angle, color, alpha)
		if(outlineW>0)
			drawDiaSpecific(x, y, w, h, angle, outlineColor, alpha, outlineW)
	}

	fun drawDia(
		x:Number, y:Number, w:Number, h:Number, angle:Float, color:Int = 0xFFFFFF, alpha:Float = 1f, outlineW:Int = 0,
		outlineColor:Int = 0x000000
	) =
		drawDia(x.toFloat(), y.toFloat(), w.toFloat(), h.toFloat(), angle, color, alpha, outlineW.toFloat(), outlineColor)

	/** Draw Rectangle Outline*/
	protected abstract fun drawDiaSpecific(
		x:Float, y:Float, w:Float, h:Float, angle:Float, color:Int = 0xFFFFFF, alpha:Float = 1f, bold:Float = 1f
	)

	private fun drawDiaSpecific(x:Number, y:Number, w:Number, h:Number, angle:Float, color:Int = 0xFFFFFF, alpha:Float = 1f, bold:Int = 0) =
		drawDiaSpecific(x.toFloat(), y.toFloat(), w.toFloat(), h.toFloat(), angle, color, alpha, bold.toFloat())
	/** Fiil Diaangle Solid*/
	protected abstract fun fillDiaSpecific(x:Float, y:Float, w:Float, h:Float, angle:Float, color:Int = 0xFFFFFF, alpha:Float = 1f)

	private fun fillDiaSpecific(x:Number, y:Number, w:Number, h:Number, angle:Float, color:Int = 0xFFFFFF, alpha:Float = 1f) =
		fillDiaSpecific(x.toFloat(), y.toFloat(), w.toFloat(), h.toFloat(), angle, color, alpha)

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
		x:Number, y:Number, w:Number, h:Number, color:Int = 0xFFFFFF, alpha:Float = 1f, outlineW:Int = 0,
		outlineColor:Int = 0x000000
	) =
		drawOval(x.toFloat(), y.toFloat(), w.toFloat(), h.toFloat(), color, alpha, outlineW.toFloat(), outlineColor)

	/** Draw Oval Outline*/
	protected abstract fun drawOvalSpecific(
		x:Float, y:Float, w:Float, h:Float, color:Int = 0xFFFFFF, alpha:Float = 1f,
		bold:Float = 1f
	)

	private fun drawOvalSpecific(x:Number, y:Number, w:Number, h:Number, color:Int = 0xFFFFFF, alpha:Float = 1f, bold:Int = 0) =
		drawOvalSpecific(x.toFloat(), y.toFloat(), w.toFloat(), h.toFloat(), color, alpha, bold.toFloat())
	/** Fill Oval Solid*/
	protected abstract fun fillOvalSpecific(x:Float, y:Float, w:Float, h:Float, color:Int = 0xFFFFFF, alpha:Float = 1f)

	private fun fillOvalSpecific(x:Number, y:Number, w:Number, h:Number, color:Int = 0xFFFFFF, alpha:Float = 1f) =
		fillOvalSpecific(x.toFloat(), y.toFloat(), w.toFloat(), h.toFloat(), color, alpha)

	/**
	 * draw outline Piece
	 */
	protected open fun drawPieceOutline(x:Float, y:Float, piece:Piece, blksize:Float, alpha:Float = 1f, whiteLine:Boolean = false) {
		piece.let {
			val bigmul = blksize*(1+it.big.toInt())
			it.block.forEachIndexed {i, blk ->
				val x2 = x+(it.dataX[it.direction][i]*bigmul).toInt()
				val y2 = y+(it.dataY[it.direction][i]*bigmul).toInt()
				if(y2>=0) drawBlockOutline(x2, y2, blk, bigmul.toInt(), alpha, whiteLine)
			}
		}
	}

	protected open fun drawBlockOutline(x:Float, y:Float, blk:Block, blksize:Int, alpha:Float = 1f, whiteLine:Boolean = false) {
		blk.let {
			val x3 = (x)
			val y3 = (y)
			val ls = blksize-1

			val color = getColorByID(if(it.getAttribute(Block.ATTRIBUTE.BONE)) Block.COLOR.WHITE else it.color ?: Block.COLOR.WHITE)
			val lC = if(whiteLine) 0xFFFFFF else color
			fillRectSpecific(x3, y3, blksize.toFloat(), blksize.toFloat(), color, alpha*.5f)

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
				fillRectSpecific(x3, y3, 2f, 2f, lC, alpha*.5f)
			if(it.getAttribute(Block.ATTRIBUTE.CONNECT_LEFT, Block.ATTRIBUTE.CONNECT_DOWN))
				fillRectSpecific(x3, (y3+blksize-2), 2f, 2f, lC, alpha*.5f)
			if(it.getAttribute(Block.ATTRIBUTE.CONNECT_RIGHT, Block.ATTRIBUTE.CONNECT_UP))
				fillRectSpecific(x3+blksize-2, y3, 2f, 2f, lC, alpha*.5f)
			if(it.getAttribute(Block.ATTRIBUTE.CONNECT_RIGHT, Block.ATTRIBUTE.CONNECT_DOWN))
				fillRectSpecific(x3+blksize-2, y3+blksize-2, 2f, 2f, lC, alpha*.5f)
		}
	}

	protected abstract fun drawBadgesSpecific(x:Float, y:Float, type:Int, scale:Float)
	protected abstract fun drawFieldSpecific(
		x:Float, y:Float, width:Int, viewHeight:Int, blksize:Int, scale:Float,
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

	fun drawBlackBG() = fillRectSpecific(0, 0, 640, 480, 0)

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
		fun getMeterColorHex(meterColor:Int, value:Float):Int =
			when(meterColor) {
				//			var r = meterColor/65536
				//			var g = meterColor/256%256
				//			var b = meterColor%256
				GameEngine.METER_COLOR_LEVEL ->
					(0xFF*maxOf(0f, minOf(value*3-1, 1f))).toInt()*0x10000+
						(0xFF*maxOf(0f, minOf(3-value*3f, 1f))).toInt()*0x100+
						(0xFF*maxOf(0f, minOf((1-value*3), 1f))).toInt()
				GameEngine.METER_COLOR_LIMIT -> //red<yellow<green<cyan
					(0xFF*maxOf(0f, minOf((2-value*3), 1f))).toInt()*0x10000+
						(0xFF*maxOf(0f, minOf(value*3, 1f))).toInt()*0x100+
						(0xFF*maxOf(0f, minOf((value*3f-2f), 1f))).toInt()
				else -> meterColor
			}
	}
}
