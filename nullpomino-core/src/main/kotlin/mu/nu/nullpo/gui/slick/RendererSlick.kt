/*
 * Copyright (c) 2010-2023, NullNoname
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
package mu.nu.nullpo.gui.slick

import mu.nu.nullpo.game.component.Block
import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.game.play.GameEngine.Companion.FRAME_SKIN_GRADE
import mu.nu.nullpo.game.play.GameManager
import mu.nu.nullpo.gui.common.AbstractRenderer
import mu.nu.nullpo.gui.common.BaseFont
import mu.nu.nullpo.gui.common.ResourceImage
import mu.nu.nullpo.gui.common.bg.*
import mu.nu.nullpo.gui.common.libs.Vector
import mu.nu.nullpo.gui.slick.img.FontGrade
import mu.nu.nullpo.gui.slick.img.FontMedal
import mu.nu.nullpo.gui.slick.img.FontNano
import mu.nu.nullpo.gui.slick.img.FontNormal
import mu.nu.nullpo.gui.slick.img.FontNumber
import mu.nu.nullpo.gui.slick.img.FontTTF
import mu.nu.nullpo.gui.slick.img.RenderStaffRoll
import mu.nu.nullpo.gui.slick.img.bg.SpinBG
import mu.nu.nullpo.util.CustomProperties
import org.newdawn.slick.Color
import org.newdawn.slick.Graphics
import org.newdawn.slick.Image
import org.newdawn.slick.geom.Polygon
import kotlin.math.PI
import kotlin.math.absoluteValue

/** ゲームの event 処理と描画処理 (Slick版） */
class RendererSlick(
	/** 描画先サーフェイス */
	internal var graphics:Graphics? = null
):AbstractRenderer() {

	override val resources = ResourceHolder

	/* TTF使用可能 */
	override val isTTFSupport:Boolean
		get() = resources.ttfFont!=null

	override val skinMax:Int
		get() = resources.imgBigBlockList.size

	/** Constructor */
	init {
		showBg = NullpoMinoSlick.propConfig.getProperty("option.showBg", true)
		showLineEffect = NullpoMinoSlick.propConfig.getProperty("option.showLineEffect", true)
		heavyEffect = NullpoMinoSlick.propConfig.getProperty("option.heavyEffect", false)
		edgeBold = NullpoMinoSlick.propConfig.getProperty("option.edgeBold", false)
		fieldBgBright = NullpoMinoSlick.propConfig.getProperty("option.fieldBgBright", 128)/255f
		showFieldBgGrid = NullpoMinoSlick.propConfig.getProperty("option.showFieldBgGrid", true)
		showMeter = NullpoMinoSlick.propConfig.getProperty("option.showMeter", true)
		showSpeed = NullpoMinoSlick.propConfig.getProperty("option.showMeter", true)
		darkNextArea = NullpoMinoSlick.propConfig.getProperty("option.darkNextArea", true)
		nextShadow = NullpoMinoSlick.propConfig.getProperty("option.nextShadow", false)
		lineEffectSpeed = NullpoMinoSlick.propConfig.getProperty("option.lineEffectSpeed", 0)
		outlineGhost = NullpoMinoSlick.propConfig.getProperty("option.outlineGhost", false)
		sideNext = NullpoMinoSlick.propConfig.getProperty("option.sideNext", false)
		bigSideNext = NullpoMinoSlick.propConfig.getProperty("option.bigSideNext", false)
		smoothFall = NullpoMinoSlick.propConfig.getProperty("option.smoothFall", false)
		showLocus = NullpoMinoSlick.propConfig.getProperty("option.showLocus", false)
		showCenter = NullpoMinoSlick.propConfig.getProperty("option.showCenter", false)
	}

	override fun drawBlendAdd(unit:()->Unit) {
		val g = graphics ?: return super.drawBlendAdd(unit)
		g.setDrawMode(Graphics.MODE_ADD)
		unit()
		g.setDrawMode(Graphics.MODE_NORMAL)
	}

	override fun printFontSpecific(x:Float, y:Float, str:String, font:BaseFont.FONT, color:COLOR, scale:Float, alpha:Float) {
		when(font) {
			BaseFont.FONT.NANO -> FontNano.printFont(x, y, str, color, scale, alpha)
			BaseFont.FONT.NUM -> FontNumber.printFont(x, y, str, color, scale, alpha)
			BaseFont.FONT.TTF -> printTTFSpecific(x, y, str, color, scale, alpha)
			BaseFont.FONT.GRADE -> FontGrade.printFont(x, y, str, color, scale, alpha)
			else -> FontNormal.printFont(x, y, str, color, scale, alpha)
		}
	}

	override fun printTTFSpecific(x:Float, y:Float, str:String, color:COLOR, scale:Float, alpha:Float) =
		FontTTF.print(x, y, str, color, alpha, scale)

	override val doesGraphicsExist get() = graphics!=null

	/* 勲章を描画 */
	override fun drawMedal(x:Int, y:Int, str:String, tier:Int, scale:Float) {
		FontMedal.printFont(x, y, str, tier, scale)
	}

	/* Get key name by button ID */
	override fun getKeyNameByButtonID(playerID:Int, inGame:Boolean, btnID:Int):String =
		GameKey.getKeyName(playerID, inGame, btnID)

	/* Is the skin sticky? */
	override fun isStickySkin(skin:Int):Boolean =
		(skin>=0&&skin<resources.blockStickyFlagList.size&&resources.blockStickyFlagList[skin])

	/* Sound effects再生 */
	override fun playSE(name:String, freq:Float, vol:Float) {
		resources.soundManager.play(name, false, freq, vol)
	}

	override fun loopSE(name:String, freq:Float, vol:Float) {
		resources.soundManager.play(name, true, freq, vol)
	}

	override fun stopSE(name:String) {
		resources.soundManager.stop(name)
	}

	/* 描画先のサーフェイスを設定 */
	override fun setGraphics(g:Any?) {
		if(g is Graphics&&g!=graphics) this.graphics = g
	}

	/* リプレイを保存 */
	override fun saveReplay(owner:GameManager, prop:CustomProperties, folderName:String) {
		if(owner.mode?.isOnlineMode!=false) return

		super.saveReplay(owner, prop, NullpoMinoSlick.propGlobal.getProperty("custom.replay.directory", folderName))
	}

	override fun drawBlockSpecific(x:Float, y:Float, sx:Int, sy:Int, sk:Int, size:Float, darkness:Float, alpha:Float) {
		val g = graphics ?: return
		val img:Image = when {
			size*2<=BS -> resources.imgSmallBlockList[sk]
			size>=BS*2 -> resources.imgBigBlockList[sk]
			else -> resources.imgNormalBlockList[sk]
		}.res
		val si = when {
			size*2<=BS -> BS/2
			size>=BS*2 -> BS*2
			else -> BS
		}
		val filter = (minOf(1f, maxOf(0f, 1f-darkness))).let {brit -> Color(brit, brit, brit, alpha)}

		val imageWidth = img.width
		if(sx>=imageWidth&&imageWidth!=-1) return
		val imageHeight = img.height
		if(sy>=imageHeight&&imageHeight!=-1) return
		g.drawImage(img, x, y, (x+size), (y+size), sx*si.toFloat(), sy*si.toFloat(), (sx+1f)*si, (sy+1f)*si, filter)

		if(darkness<0) {
			g.color = Color(1f, 1f, 1f, -darkness)
			g.fillRect(x, y, size, size)
		}
	}

	override fun drawLineSpecific(x:Float, y:Float, sx:Float, sy:Float, color:Int, alpha:Float, w:Float) {
		val g = graphics ?: return
		val lw = g.lineWidth
		g.lineWidth = w
		g.color = Color(color).apply {a = alpha}
		g.drawLine(x, y, sx, sy)
		g.lineWidth = lw
	}

	override fun drawRectSpecific(x:Float, y:Float, w:Float, h:Float, color:Int, alpha:Float, bold:Float) {
		if(w>0f) {
			val g = graphics ?: return
			val c = g.color
			g.color = Color(color).apply {a = alpha}
			val lw = g.lineWidth
			g.lineWidth = bold
			g.drawRect(x-bold/2f, y-bold/2f, w+bold/2f, h+bold/2f)
			g.color = c
			g.lineWidth = lw
		}
	}

	override fun fillRectSpecific(x:Float, y:Float, w:Float, h:Float, color:Int, alpha:Float) {
		val g = graphics ?: return
		val c = g.color
		g.color = Color(color).apply {a = alpha}
		g.fillRect(x, y, w, h)
		g.color = c
	}

	override fun drawDiaSpecific(x:Float, y:Float, w:Float, h:Float, angle:Float, color:Int, alpha:Float, bold:Float) {
		if(w>0f) {
			val g = graphics ?: return
			val c = g.color
			g.color = Color(color).apply {a = alpha}
			val lw = g.lineWidth
			g.lineWidth = bold
			val p = PI.toFloat()
			val rad = angle%p
			val va = Vector(w/2, rad, true)
			val vb = Vector(h/2, p/2+rad, true)
			g.draw(Polygon(floatArrayOf(x+va.x, y+va.y, x+vb.x, y+vb.y, x-va.x, y-va.y, x-vb.x, y-vb.y)))
			g.color = c
			g.lineWidth = lw
		}
	}

	override fun fillDiaSpecific(x:Float, y:Float, w:Float, h:Float, angle:Float, color:Int, alpha:Float) {
		val g = graphics ?: return
		val c = g.color
		g.color = Color(color).apply {a = alpha}
		val pi = PI.toFloat()
		val rad = angle%pi
		val va = Vector(w/2, rad, true)
		val vb = Vector(h/2, pi/2+rad, true)
		g.fill(Polygon(floatArrayOf(x+va.x, y+va.y, x+vb.x, y+vb.y, x-va.x, y-va.y, x-vb.x, y-vb.y)))
		g.color = c
	}

	override fun drawOvalSpecific(x:Float, y:Float, w:Float, h:Float, color:Int, alpha:Float, bold:Float) {
		if(w>0f) {
			val g = graphics ?: return
			val c = g.color
			g.color = Color(color).apply {a = alpha}
			val lw = g.lineWidth
			g.lineWidth = bold
			g.drawOval(x-bold/2f, y-bold/2f, w+bold/2f, h+bold/2f)
			g.color = c
			g.lineWidth = lw
		}
	}

	override fun fillOvalSpecific(x:Float, y:Float, w:Float, h:Float, color:Int, alpha:Float) {
		val g = graphics ?: return
		val c = g.color
		g.color = Color(color).apply {a = alpha}
		g.fillOval(x, y, w, h)
		g.color = c
	}

	override fun drawBadgesSpecific(x:Float, y:Float, type:Int, scale:Float) {
		val b = FontBadge(type)
		graphics?.drawImage(
			resources.imgBadges,
			x, y, x+b.w*scale, y+b.h*scale,
			b.sx.toFloat(), b.sy.toFloat(), (b.sx+b.w).toFloat(), (b.sy+b.h).toFloat()
		)
	}

	override fun drawFieldSpecific(x:Int, y:Int, width:Int, viewHeight:Int, blksize:Int, scale:Float, outlineType:Int) {
		//TODO:("Not yet implemented")

		val g = graphics ?: return
//		val blksize = (BS*scale).toInt()
		/*
				engine.nowPieceObject?.let {
					for(i in 0 until it.maxBlock)
						if(!it.big) {
							val x2 = engine.nowPieceX+it.dataX[it.direction][i]
							val y2 = engine.nowPieceBottomY+it.dataY[it.direction][i]

							if(y2>=0)
								if(outlineGhost) {
									val blkTemp = it.block[i]
									val x3:Float = (x+x2*blksize).toFloat()
									val y3:Float = (y+y2*blksize).toFloat()
									val ls = blksize-1

									var colorID = blkTemp.drawColor
									if(blkTemp.getAttribute(Block.ATTRIBUTE.BONE)) colorID = -1
									var color = getColorByID(colorID)
									if(showBg) color.a = .5f
									else color = color.darker(.5f)
									g.color = color
									g.fillRect(x3, y3, blksize.toFloat(), blksize.toFloat())
									g.color = Color.white

									if(!blkTemp.getAttribute(Block.ATTRIBUTE.CONNECT_UP)) {
										g.drawLine(x3, y3, (x3+ls), y3)
										g.drawLine(x3, (y3+1), (x3+ls), (y3+1))
									}
									if(!blkTemp.getAttribute(Block.ATTRIBUTE.CONNECT_DOWN)) {
										g.drawLine(x3, (y3+ls), (x3+ls), (y3+ls))
										g.drawLine(x3, (y3-1+ls), (x3+ls), (y3-1+ls))
									}
									if(!blkTemp.getAttribute(Block.ATTRIBUTE.CONNECT_LEFT)) {
										g.drawLine(x3, y3, x3, (y3+ls))
										g.drawLine((x3+1), y3, (x3+1), (y3+ls))
									}
									if(!blkTemp.getAttribute(Block.ATTRIBUTE.CONNECT_RIGHT)) {
										g.drawLine((x3+ls), y3, (x3+ls), (y3+ls))
										g.drawLine((x3-1+ls), y3, (x3-1+ls), (y3+ls))
									}
									if(blkTemp.getAttribute(Block.ATTRIBUTE.CONNECT_LEFT, Block.ATTRIBUTE.CONNECT_UP))
										g.fillRect(x3, y3, 2f, 2f)
									if(blkTemp.getAttribute(Block.ATTRIBUTE.CONNECT_LEFT, Block.ATTRIBUTE.CONNECT_DOWN))
										g.fillRect(x3, (y3+blksize-2), 2f, 2f)
									if(blkTemp.getAttribute(Block.ATTRIBUTE.CONNECT_RIGHT, Block.ATTRIBUTE.CONNECT_UP))
										g.fillRect((x3+blksize-2), y3, 2f, 2f)
									if(blkTemp.getAttribute(Block.ATTRIBUTE.CONNECT_RIGHT, Block.ATTRIBUTE.CONNECT_DOWN))
										g.fillRect((x3+blksize-2), (y3+blksize-2), 2f, 2f)
								} else {
									val blkTemp = Block(it.block[i])
									blkTemp.darkness = .3f
									if(engine.nowPieceColorOverride>=0) blkTemp.cint = engine.nowPieceColorOverride
									drawBlock(x+x2*blksize, y+y2*blksize, blkTemp, scale)
								}
						} else {
							val x2 = engine.nowPieceX+it.dataX[it.direction][i]*2
							val y2 = engine.nowPieceBottomY+it.dataY[it.direction][i]*2

							if(outlineGhost) {
								val blkTemp = it.block[i]
								val x3:Float = (x+x2*blksize).toFloat()
								val y3:Float = (y+y2*blksize).toFloat()
								val ls = blksize*2-1

								var colorID = blkTemp.drawColor
								if(blkTemp.getAttribute(Block.ATTRIBUTE.BONE)) colorID = -1
								var color = getColorByID(colorID)
								if(showBg) color.a = .5f
								else color = color.darker(.5f)
								g.color = color
								g.fillRect(x3, y3, (blksize*2).toFloat(), (blksize*2).toFloat())
								g.color = Color.white

								if(!blkTemp.getAttribute(Block.ATTRIBUTE.CONNECT_UP)) {
									g.drawLine(x3, y3, (x3+ls), y3)
									g.drawLine(x3, (y3+1), (x3+ls), (y3+1))
								}
								if(!blkTemp.getAttribute(Block.ATTRIBUTE.CONNECT_DOWN)) {
									g.drawLine(x3, (y3+ls), (x3+ls), (y3+ls))
									g.drawLine(x3, (y3-1+ls), (x3+ls), (y3-1+ls))
								}
								if(!blkTemp.getAttribute(Block.ATTRIBUTE.CONNECT_LEFT)) {
									g.drawLine(x3, y3, x3, (y3+ls))
									g.drawLine((x3+1), y3, (x3+1), (y3+ls))
								}
								if(!blkTemp.getAttribute(Block.ATTRIBUTE.CONNECT_RIGHT)) {
									g.drawLine((x3+ls), y3, (x3+ls), (y3+ls))
									g.drawLine((x3-1+ls), y3, (x3-1+ls), (y3+ls))
								}
								if(blkTemp.getAttribute(Block.ATTRIBUTE.CONNECT_LEFT, Block.ATTRIBUTE.CONNECT_UP))
									g.fillRect(x3, y3, 2f, 2f)
								if(blkTemp.getAttribute(Block.ATTRIBUTE.CONNECT_LEFT, Block.ATTRIBUTE.CONNECT_DOWN))
									g.fillRect(x3, (y3+blksize*2-2), 2f, 2f)
								if(blkTemp.getAttribute(Block.ATTRIBUTE.CONNECT_RIGHT, Block.ATTRIBUTE.CONNECT_UP))
									g.fillRect((x3+blksize*2-2), y3, 2f, 2f)
								if(blkTemp.getAttribute(Block.ATTRIBUTE.CONNECT_RIGHT, Block.ATTRIBUTE.CONNECT_DOWN))
									g.fillRect((x3+blksize*2-2), (y3+blksize*2-2), 2f, 2f)
							} else {
								val blkTemp = Block(it.block[i])
								blkTemp.darkness = .3f
								if(engine.nowPieceColorOverride>=0) blkTemp.cint = engine.nowPieceColorOverride
								drawBlock(x+x2*blksize, y+y2*blksize, blkTemp, scale*2f)
							}
						}
				}*/
	}

	/*verride fun drawGhostPiece(x:Float, y:Float, engine:GameEngine, scale:Float) {
		val g = graphics ?: return
		val blksize = (BS*scale).toInt()

		engine.nowPieceObject?.let {
			for(i in 0 until it.maxBlock)
				if(!it.big) {
					val x2 = engine.nowPieceX+it.dataX[it.direction][i]
					val y2 = engine.nowPieceBottomY+it.dataY[it.direction][i]

					if(y2>=0)
						if(outlineGhost) {
							val blkTemp = it.block[i]
							val x3:Float = (x+x2*blksize)
							val y3:Float = (y+y2*blksize)
							val ls = blksize-1

							var colorID = blkTemp.drawColor
							if(blkTemp.getAttribute(Block.ATTRIBUTE.BONE)) colorID = -1
							var color = getColorByID(colorID)
							if(showBg) color.a = .5f
							else color = color.darker(.5f)
							g.color = color
							g.fillRect(x3, y3, blksize.toFloat(), blksize.toFloat())
							g.color = Color.white

							if(!blkTemp.getAttribute(Block.ATTRIBUTE.CONNECT_UP)) {
								g.drawLine(x3, y3, (x3+ls), y3)
								g.drawLine(x3, (y3+1), (x3+ls), (y3+1))
							}
							if(!blkTemp.getAttribute(Block.ATTRIBUTE.CONNECT_DOWN)) {
								g.drawLine(x3, (y3+ls), (x3+ls), (y3+ls))
								g.drawLine(x3, (y3-1+ls), (x3+ls), (y3-1+ls))
							}
							if(!blkTemp.getAttribute(Block.ATTRIBUTE.CONNECT_LEFT)) {
								g.drawLine(x3, y3, x3, (y3+ls))
								g.drawLine((x3+1), y3, (x3+1), (y3+ls))
							}
							if(!blkTemp.getAttribute(Block.ATTRIBUTE.CONNECT_RIGHT)) {
								g.drawLine((x3+ls), y3, (x3+ls), (y3+ls))
								g.drawLine((x3-1+ls), y3, (x3-1+ls), (y3+ls))
							}
							if(blkTemp.getAttribute(Block.ATTRIBUTE.CONNECT_LEFT, Block.ATTRIBUTE.CONNECT_UP))
								g.fillRect(x3, y3, 2f, 2f)
							if(blkTemp.getAttribute(Block.ATTRIBUTE.CONNECT_LEFT, Block.ATTRIBUTE.CONNECT_DOWN))
								g.fillRect(x3, (y3+blksize-2), 2f, 2f)
							if(blkTemp.getAttribute(Block.ATTRIBUTE.CONNECT_RIGHT, Block.ATTRIBUTE.CONNECT_UP))
								g.fillRect((x3+blksize-2), y3, 2f, 2f)
							if(blkTemp.getAttribute(Block.ATTRIBUTE.CONNECT_RIGHT, Block.ATTRIBUTE.CONNECT_DOWN))
								g.fillRect((x3+blksize-2), (y3+blksize-2), 2f, 2f)
						} else {
							val blkTemp = Block(it.block[i]).apply {
								darkness = .3f
								engine.nowPieceColorOverride?.let {color = it}
							}
							drawBlock(x+x2*blksize, y+y2*blksize, blkTemp, scale = scale)
						}
				} else {
					val x2 = engine.nowPieceX+it.dataX[it.direction][i]*2
					val y2 = engine.nowPieceBottomY+it.dataY[it.direction][i]*2

					if(outlineGhost) {
						val blkTemp = it.block[i]
						val x3:Float = (x+x2*blksize)
						val y3:Float = (y+y2*blksize)
						val ls = blksize*2-1

						var colorID = blkTemp.drawColor
						if(blkTemp.getAttribute(Block.ATTRIBUTE.BONE)) colorID = -1
						var color = getColorByID(colorID)
						if(showBg) color.a = .5f
						else color = color.darker(.5f)
						g.color = color
						g.fillRect(x3, y3, (blksize*2).toFloat(), (blksize*2).toFloat())
						g.color = Color.white

						if(!blkTemp.getAttribute(Block.ATTRIBUTE.CONNECT_UP)) {
							g.drawLine(x3, y3, (x3+ls), y3)
							g.drawLine(x3, (y3+1), (x3+ls), (y3+1))
						}
						if(!blkTemp.getAttribute(Block.ATTRIBUTE.CONNECT_DOWN)) {
							g.drawLine(x3, (y3+ls), (x3+ls), (y3+ls))
							g.drawLine(x3, (y3-1+ls), (x3+ls), (y3-1+ls))
						}
						if(!blkTemp.getAttribute(Block.ATTRIBUTE.CONNECT_LEFT)) {
							g.drawLine(x3, y3, x3, (y3+ls))
							g.drawLine((x3+1), y3, (x3+1), (y3+ls))
						}
						if(!blkTemp.getAttribute(Block.ATTRIBUTE.CONNECT_RIGHT)) {
							g.drawLine((x3+ls), y3, (x3+ls), (y3+ls))
							g.drawLine((x3-1+ls), y3, (x3-1+ls), (y3+ls))
						}
						if(blkTemp.getAttribute(Block.ATTRIBUTE.CONNECT_LEFT, Block.ATTRIBUTE.CONNECT_UP))
							g.fillRect(x3, y3, 2f, 2f)
						if(blkTemp.getAttribute(Block.ATTRIBUTE.CONNECT_LEFT, Block.ATTRIBUTE.CONNECT_DOWN))
							g.fillRect(x3, (y3+blksize*2-2), 2f, 2f)
						if(blkTemp.getAttribute(Block.ATTRIBUTE.CONNECT_RIGHT, Block.ATTRIBUTE.CONNECT_UP))
							g.fillRect((x3+blksize*2-2), y3, 2f, 2f)
						if(blkTemp.getAttribute(Block.ATTRIBUTE.CONNECT_RIGHT, Block.ATTRIBUTE.CONNECT_DOWN))
							g.fillRect((x3+blksize*2-2), (y3+blksize*2-2), 2f, 2f)
					} else {
						val blkTemp = Block(it.block[i]).apply {
							darkness = .3f
							engine.nowPieceColorOverride?.let {color = it}
						}
						drawBlock(x+x2*blksize, y+y2*blksize, blkTemp, scale = scale*2f)
					}
				}
		}
	}

	override fun drawHintPiece(x:Int, y:Int, engine:GameEngine, scale:Float) {
		val g = graphics ?: return
		engine.aiHintPiece?.let {
			it.direction = engine.ai!!.bestRt
			it.updateConnectData()
			val blksize = (16*scale).toInt()

			for(i in 0 until it.maxBlock)
				if(!it.big) {
					val x2 = engine.ai!!.bestX+it.dataX[engine.ai!!.bestRt][i]
					val y2 = engine.ai!!.bestY+it.dataY[engine.ai!!.bestRt][i]

					if(y2>=0) {
						val blkTemp = it.block[i]
						val x3:Float = (x+x2*blksize).toFloat()
						val y3:Float = (y+y2*blksize).toFloat()
						val ls = blksize-1

						var colorID = blkTemp.drawColor
						if(blkTemp.getAttribute(Block.ATTRIBUTE.BONE)) colorID = -1
						val color = getColorByID(colorID)
						g.color = color

						if(!blkTemp.getAttribute(Block.ATTRIBUTE.CONNECT_UP)) g.fillRect(x3, y3, ls.toFloat(), 2f)
						if(!blkTemp.getAttribute(Block.ATTRIBUTE.CONNECT_DOWN))
							g.fillRect(x3, (y3+ls-1), ls.toFloat(), 2f)
						if(!blkTemp.getAttribute(Block.ATTRIBUTE.CONNECT_LEFT)) g.fillRect(x3, y3, 2f, ls.toFloat())
						if(!blkTemp.getAttribute(Block.ATTRIBUTE.CONNECT_RIGHT))
							g.fillRect((x3+ls-1), y3, 2f, ls.toFloat())
						if(blkTemp.getAttribute(Block.ATTRIBUTE.CONNECT_LEFT, Block.ATTRIBUTE.CONNECT_UP))
							g.fillRect(x3, y3, 2f, 2f)
						if(blkTemp.getAttribute(Block.ATTRIBUTE.CONNECT_LEFT, Block.ATTRIBUTE.CONNECT_DOWN))
							g.fillRect(x3, (y3+blksize-2), 2f, 2f)
						if(blkTemp.getAttribute(Block.ATTRIBUTE.CONNECT_RIGHT, Block.ATTRIBUTE.CONNECT_UP))
							g.fillRect((x3+blksize-2), y3, 2f, 2f)
						if(blkTemp.getAttribute(Block.ATTRIBUTE.CONNECT_RIGHT, Block.ATTRIBUTE.CONNECT_DOWN))
							g.fillRect((x3+blksize-2), (y3+blksize-2), 2f, 2f)
					}
				} else {
					val x2 = engine.ai!!.bestX+it.dataX[engine.ai!!.bestRt][i]*2
					val y2 = engine.ai!!.bestY+it.dataY[engine.ai!!.bestRt][i]*2

					val blkTemp = it.block[i]
					val x3:Float = (x+x2*blksize).toFloat()
					val y3:Float = (y+y2*blksize).toFloat()
					val ls = blksize*2-1

					var colorID = blkTemp.drawColor
					if(blkTemp.getAttribute(Block.ATTRIBUTE.BONE)) colorID = -1
					var color = getColorByID(colorID)
					if(showBg) color.a = .5f
					else color = color.darker(.5f)
					g.color = color
					//graphics.fillRect(x3, y3, blksize * 2, blksize * 2);
					g.color = Color.white

					if(!blkTemp.getAttribute(Block.ATTRIBUTE.CONNECT_UP)) {
						g.drawLine(x3, y3, (x3+ls), y3)
						g.drawLine(x3, (y3+1), (x3+ls), (y3+1))
					}
					if(!blkTemp.getAttribute(Block.ATTRIBUTE.CONNECT_DOWN)) {
						g.drawLine(x3, (y3+ls), (x3+ls), (y3+ls))
						g.drawLine(x3, (y3-1+ls), (x3+ls), (y3-1+ls))
					}
					if(!blkTemp.getAttribute(Block.ATTRIBUTE.CONNECT_LEFT)) {
						g.drawLine(x3, y3, x3, (y3+ls))
						g.drawLine((x3+1), y3, (x3+1), (y3+ls))
					}
					if(!blkTemp.getAttribute(Block.ATTRIBUTE.CONNECT_RIGHT)) {
						g.drawLine((x3+ls), y3, (x3+ls), (y3+ls))
						g.drawLine((x3-1+ls), y3, (x3-1+ls), (y3+ls))
					}
					if(blkTemp.getAttribute(Block.ATTRIBUTE.CONNECT_LEFT, Block.ATTRIBUTE.CONNECT_UP))
						g.fillRect(x3, y3, 2f, 2f)
					if(blkTemp.getAttribute(Block.ATTRIBUTE.CONNECT_LEFT, Block.ATTRIBUTE.CONNECT_DOWN))
						g.fillRect(x3, (y3+blksize*2-2), 2f, 2f)
					if(blkTemp.getAttribute(Block.ATTRIBUTE.CONNECT_RIGHT, Block.ATTRIBUTE.CONNECT_UP))
						g.fillRect((x3+blksize*2-2), y3, 2f, 2f)
					if(blkTemp.getAttribute(Block.ATTRIBUTE.CONNECT_RIGHT, Block.ATTRIBUTE.CONNECT_DOWN))
						g.fillRect((x3+blksize*2-2), (y3+blksize*2-2), 2f, 2f)
				}
		}
	}*/

	/** Field frameを描画
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param engine GameEngineのインスタンス
	 */
	override fun drawFrameSpecific(x:Int, y:Int, engine:GameEngine) {
		val g = graphics ?: return
		val size = engine.blockSize
		val width = engine.field.width//?: Field.DEFAULT_WIDTH
		val height = engine.field.height//?: Field.DEFAULT_HEIGHT
//		val oX = 0

		RenderStaffRoll.draw(x+width/2f, y.toFloat(), 0f, height.toFloat(), Color(200, 233, 255, 200))


		if(engine.frameColor>=0) {
			val fi = resources.imgFrame[engine.frameColor].res

			val lX = x.toFloat()
			val rX = lX+width*size
			val tY = y.toFloat()
			val bY = tY+height*size

			g.color = Color.white
			//top edge
//			g.drawImage(fi, fi.height.toFloat()+1, 0f)
			fi.getSubImage(16, 0, 16, 32).also {
				it.setCenterOfRotation(0f, 0f)
				it.rotation = -90f
				it.draw(lX-size, tY, size.toFloat(), rX-lX+size*2f)
			}
			//bottom edge
			fi.getSubImage(48, 96, 16, 32).also {
				it.setCenterOfRotation(0f, 0f)
				it.rotation = -90f
				it.draw(lX-size, bY+size, size.toFloat(), rX-lX+size*2f)
			}
			//left edge
			g.texture(
				Polygon(floatArrayOf(lX-size, tY-size, lX, tY, lX, bY, lX-size, bY+size)),
				fi.getSubImage(16, 0, 16, 32),
				1f, 1f, true
			)
			//right edge
			g.texture(
				Polygon(floatArrayOf(rX+size, tY-size, rX, tY, rX, bY, rX+size, bY+size)),
				fi.getSubImage(16, 96, 16, 32),
				1f, 1f, true
			)
		} else if(engine.frameColor==FRAME_SKIN_GRADE) {
			val fi = resources.imgFrameOld[3]
		}
	}

	val bgType:List<AbstractBG<Image>> by lazy {
		resources.imgPlayBG.map {it:ResourceImage<Image> -> SpinBG(it)}
	}

	val bgaType:List<AbstractBG<Image>> by lazy {
		resources.imgPlayBGA.mapIndexed {i, it:ResourceImage<Image> ->
			when {
				it.name.endsWith("_o") -> DTET00Ocean(it)
				it.name.endsWith("_c") -> DTET01CircleLoop(it)
				it.name.endsWith("_f") -> DTET02Fall(it)
				it.name.endsWith("_n") -> DTET03NightClock(it)
				it.name.endsWith("_d") -> DTET04Deep(it)
				it.name.endsWith("_k") -> DTET05KaleidSq(it)
				it.name.endsWith("_t") -> DTET06Texture(it)
				it.name.endsWith("_b") -> DTET07Beams(it)
				it.name.endsWith("_m") -> DTET08Mist(it)
				it.name.endsWith("_p") -> DTET09Prism(it)
				it.name.endsWith("_x") -> DTET11ExTrans(it)
				it.name.endsWith("_r") -> DTET12Rush(it)
				else -> DTET10VWave(it)
			}
		}
	}

	override fun modeInit(manager:GameManager) {
		super.modeInit(manager)
		bgType.forEach {it.reset()}
		bgaType.forEach {it.reset()}
//		engine.owner.bgMan.fadeEnabled=heavyEffect
	}

	override fun onFirst(engine:GameEngine) {
		super.onFirst(engine)
		val bgMax = resources.bgMax
		val bgaMax = resources.bgaMax
		val bg = engine.owner.bgMan.bg

		if(!engine.owner.menuOnly&&showBg&&heavyEffect) {
			if(bg in 0 until bgMax)
				bgType[bg].update()
			else if(bg<0&&bg.absoluteValue in 1..bgaMax+1)
				bgaType[bg.absoluteValue-1].update()
		}
	}

	override fun drawBG(engine:GameEngine) {
		val graphics = graphics ?: return
		if(engine.owner.menuOnly) {
			graphics.color = Color.white
			graphics.drawImage(resources.imgMenuBG[1], 0f, 0f)
		} else if(showBg) {
			val bgMax = resources.bgMax
			val bgaMax = resources.bgaMax
			val bg = engine.owner.bgMan.bg


			graphics.color = Color.white
			if(heavyEffect) {
				if(bg in 0 until bgMax)
					bgType[bg].draw()
				else if(bg<0&&bg.absoluteValue in 1..bgaMax+1)
					bgaType[bg.absoluteValue-1].draw()

				if(engine.owner.bgMan.fadeSW) {
					val filter = Color(Color.black).apply {
						a = engine.owner.bgMan.let {
							if(!it.fadeStat) it.fadeCount/100f else (100f-it.fadeCount)/100
						}
					}
					graphics.color = filter
					graphics.fillRect(0, 0, 640, 480)
				}
			} else {

				if(bg in 0 until bgMax)
					bgType[bg].drawLite()
				else if(bg<0&&bg.absoluteValue in 1..bgaMax+1)
					bgaType[bg.absoluteValue-1].drawLite()
				val bgi = resources.imgPlayBG[bg].res
				bgi.rotation = 0f
				graphics.drawImage(bgi, 0, 0, 640, 480, 0, 0, bgi.width, bgi.height)
				/*
If FOC < 30 Then
FOC = FOC + 1
RR = FAC
RR1 = FOC + (FOC > 20) * (FOC - 20)
For I = 0 To 11: For U = 0 To 1
With Src
.Left = 0: .Top = 232 - RR1 * 2: .Right = 40 * (8 - ((RR * 3) Mod 8)): .Bottom = 232
End With
If Not FS Then BBSf.BltFast U * 320 + 320 - 40 * (8 - ((RR * 3) Mod 8)), I * 40 + 40 - RR1 * 2, SpSf, Src, DDBLTFAST_WAIT
With Src
.Left = 40 * (8 - ((RR * 3) Mod 8)): .Top = 232 - RR1 * 2: .Right = 320: .Bottom = 232
End With
If Not FS Then BBSf.BltFast U * 320, I * 40 + 40 - RR1 * 2, SpSf, Src, DDBLTFAST_WAIT
Next U, I
Else
If FIC = 0 Then
Stt = Int(Lev / 5)
If Lev >= 50 And Lev < 200 Then Stt = 10
If Lev >= 30 - (TrM >= 1) * 20 - (TrM = 2) * 150 Then Stt = 11 - (Not (Fnl Or TA)) * 2
BGCr Stt
End If
FIC = FIC + 1
RR = FAC
RR1 = (FIC - 10) * -(FIC >= 10)
For I = 0 To 11: For U = 0 To 1
With Src
.Left = 0: .Top = 192: .Right = 40 * (8 - ((RR * 3) Mod 8)): .Bottom = 232 - RR1 * 2
End With
If Not FS Then BBSf.BltFast U * 320 + 320 - 40 * (8 - ((RR * 3) Mod 8)), I * 40, SpSf, Src, DDBLTFAST_WAIT
With Src
.Left = 40 * (8 - ((RR * 3) Mod 8)): .Top = 192: .Right = 320: .Bottom = 232 - RR1 * 2
End With
If Not FS Then BBSf.BltFast U * 320, I * 40, SpSf, Src, DDBLTFAST_WAIT
Next U, I
If FIC >= 30 Then FOC = 0: FIC = 0: FF = False
End If
Exit Sub
*/
			}
		}
	}

	override fun setBGSpd(owner:GameManager, spd:Float, id:Int?) {
		val bgMax = resources.bgMax
		val bgaMax = resources.bgaMax

		if(!owner.menuOnly&&showBg&&heavyEffect)
			if(id==null) {
				bgType.forEach {it.speed = spd}
				bgaType.forEach {it.speed = spd}
			} else if(id in 0 until bgMax)
				bgType[id].speed = spd
			else if(id<0&&id.absoluteValue in 1..bgaMax+1)
				bgaType[id.absoluteValue-1].speed = spd
	}
	/*override fun effectRender() {
	_	effects.forEachIndexed {i, it ->
			when(it) {
				is Particle -> {
					val g = graphics ?: return it.draw(i, this)
					when(it.shape) {
						Particle.ParticleShape.ARect -> {
							g.setDrawMode(Graphics.MODE_ADD)
							drawRect(
								it.pos.x-it.us/2, it.pos.y-it.us/2, it.us, it.us,
								it.ur*0x10000+it.ug*0x100+it.ub, it.ua/255f, 0f
							)
							g.setDrawMode(Graphics.MODE_NORMAL)
						}
						Particle.ParticleShape.AOval -> {
							g.setDrawMode(Graphics.MODE_ADD)
							drawOval(
								it.pos.x-it.us/2, it.pos.y-it.us/2, it.us, it.us,
								it.ur*0x10000+it.ug*0x100+it.ub, it.ua/255f, 0f
							)
							g.setDrawMode(Graphics.MODE_NORMAL)
						}
						else -> it.draw(i, this)
					}
				}
				else -> it.draw(i, this)
			}
		}
	}*/

	companion object {
		/** Block colorIDに応じてSlick用Colorオブジェクトを作成・取得
		 * @param colorID Block colorID
		 * @return Slick用Colorオブジェクト
		 */
		fun getColorByID(colorID:Int):Color = when(colorID) {
			Block.COLOR_WHITE -> Color(Color.gray)
			Block.COLOR_RED -> Color(Color.red)
			Block.COLOR_ORANGE -> Color(Color.orange)
			Block.COLOR_YELLOW -> Color(Color.yellow)
			Block.COLOR_GREEN -> Color(Color.green)
			Block.COLOR_CYAN -> Color(Color.cyan)
			Block.COLOR_BLUE -> Color(Color.blue)
			Block.COLOR_PURPLE -> Color(Color.magenta)
			else -> Color(Color.black)
		}

		fun getColorByID(color:Block.COLOR):Color = getColorByID(Block.colorNumber(color, Block.TYPE.BLOCK))

		fun getMeterColorAsColor(meterColor:Int, value:Float):Color =
			Color(getMeterColorHex(meterColor, value)).apply {a = 1f}
	}
}
