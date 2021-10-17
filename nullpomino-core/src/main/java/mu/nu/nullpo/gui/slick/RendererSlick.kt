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
package mu.nu.nullpo.gui.slick

import mu.nu.nullpo.game.component.Block
import mu.nu.nullpo.game.component.Field
import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.game.play.GameManager
import mu.nu.nullpo.gui.common.AbstractRenderer
import mu.nu.nullpo.gui.common.BeamH
import mu.nu.nullpo.gui.common.EffectObject
import mu.nu.nullpo.gui.common.FragAnim
import mu.nu.nullpo.gui.common.FragAnim.ANIM
import mu.nu.nullpo.gui.slick.img.*
import mu.nu.nullpo.util.CustomProperties
import org.lwjgl.input.Keyboard
import org.newdawn.slick.Color
import org.newdawn.slick.Graphics
import org.newdawn.slick.Image
import kotlin.math.cos
import kotlin.math.pow

/** ゲームの event 処理と描画処理 (Slick版） */
class RendererSlick:AbstractRenderer() {
	/** 描画先サーフェイス */
	internal var graphics:Graphics? = null

	override val resources = ResourceHolder

	/* TTF使用可能 */
	override val isTTFSupport:Boolean
		get() = resources.ttfFont!=null

	override val skinMax:Int
		get() = resources.imgBigBlockList.size

	/** Constructor */
	init {
		showbg = NullpoMinoSlick.propConfig.getProperty("option.showbg", true)
		showlineeffect = NullpoMinoSlick.propConfig.getProperty("option.showlineeffect", true)
		heavyeffect = NullpoMinoSlick.propConfig.getProperty("option.heavyeffect", false)
		var bright = NullpoMinoSlick.propConfig.getProperty("option.fieldbgbright", 64)*2
		if(NullpoMinoSlick.propConfig.getProperty("option.fieldbgbright2")!=null)
			bright = NullpoMinoSlick.propConfig.getProperty("option.fieldbgbright2", 128)
		if(bright>255) bright = 255
		fieldbgbright = bright/255.toFloat()
		showfieldbggrid = NullpoMinoSlick.propConfig.getProperty("option.showfieldbggrid", true)
		showMeter = NullpoMinoSlick.propConfig.getProperty("option.showmeter", true)
		showSpeed = NullpoMinoSlick.propConfig.getProperty("option.showmeter", true)
		darknextarea = NullpoMinoSlick.propConfig.getProperty("option.darknextarea", true)
		nextshadow = NullpoMinoSlick.propConfig.getProperty("option.nextshadow", false)
		lineeffectspeed = NullpoMinoSlick.propConfig.getProperty("option.lineeffectspeed", 0)
		outlineghost = NullpoMinoSlick.propConfig.getProperty("option.outlineghost", false)
		sidenext = NullpoMinoSlick.propConfig.getProperty("option.sidenext", false)
		bigsidenext = NullpoMinoSlick.propConfig.getProperty("option.bigsidenext", false)
		smoothfall = NullpoMinoSlick.propConfig.getProperty("option.smoothfall", false)
		showLocus = NullpoMinoSlick.propConfig.getProperty("option.showLocus", false)
		showCenter = NullpoMinoSlick.propConfig.getProperty("option.showCenter", false)
	}

	override fun printFontSpecific(x:Int, y:Int, str:String, font:FONT, color:COLOR, scale:Float, alpha:Float) {
		when(font) {
			FONT.NANO -> FontNano.printFont(x, y, str, color, scale, alpha)
			FONT.NUM -> FontNumber.printFont(x, y, str, color, scale, alpha)
			FONT.TTF -> printTTFSpecific(x, y, str, color, alpha)
			FONT.GRADE -> FontGrade.printFont(x, y, str, color, scale, alpha)
			else -> FontNormal.printFont(x, y, str, color, scale, alpha)
		}
	}

	override fun printTTFSpecific(x:Int, y:Int, str:String, color:COLOR, alpha:Float) =
		FontTTF.print(x, y, str, color, alpha)

	override fun doesGraphicsExist():Boolean = graphics!=null

	/* 勲章を描画 */
	override fun drawMedal(x:Int, y:Int, str:String, tier:Int, scale:Float) {
		FontMedal.printFont(x, y, str, tier, scale)
	}

	/* Get key name by button ID */
	override fun getKeyNameByButtonID(playerID:Int, inGame:Boolean, btnID:Int):String {
		val keymap = if(inGame) GameKey.gamekey[playerID].keymap else GameKey.gamekey[playerID].keymapNav

		if(btnID>=0&&btnID<keymap.size) {
			val keycode = keymap[btnID]
			return keycode.joinToString {Keyboard.getKeyName(it) ?: "($it)"}
		}

		return ""
	}

	/* Is the skin sticky? */
	override fun isStickySkin(skin:Int):Boolean {
		return (skin>=0&&skin<resources.blockStickyFlagList.size
			&&resources.blockStickyFlagList[skin])
	}

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
	override fun setGraphics(g:Any) {
		if(g is Graphics) graphics = g
	}

	/* リプレイを保存 */
	override fun saveReplay(owner:GameManager, prop:CustomProperties, foldername:String) {
		if(owner.mode?.isNetplayMode!=false) return

		super.saveReplay(owner, prop, NullpoMinoSlick.propGlobal.getProperty("custom.replay.directory", foldername))
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
		val filter = Color(Color.white).apply {
			a = alpha
		}.darker(maxOf(0f, darkness))

		val imageWidth = img.width
		if(sx>=imageWidth&&imageWidth!=-1) return
		val imageHeight = img.height
		if(sy>=imageHeight&&imageHeight!=-1) return
		g.drawImage(img, x, y, (x+size), (y+size), sx*si.toFloat(), sy*si.toFloat(), (sx+1f)*si, (sy+1f)*si, filter)

		if(darkness<0) {
			val brightfilter = Color(Color.white)
			brightfilter.a = -darkness
			g.color = brightfilter
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
		g.color = Color(color).apply {a = alpha}
		g.fillRect(x, y, w, h)
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
		val blksize = (BS*scale).toInt()
/*
		engine.nowPieceObject?.let {
			for(i in 0 until it.maxBlock)
				if(!it.big) {
					val x2 = engine.nowPieceX+it.dataX[it.direction][i]
					val y2 = engine.nowPieceBottomY+it.dataY[it.direction][i]

					if(y2>=0)
						if(outlineghost) {
							val blkTemp = it.block[i]
							val x3:Float = (x+x2*blksize).toFloat()
							val y3:Float = (y+y2*blksize).toFloat()
							val ls = blksize-1

							var colorID = blkTemp.drawColor
							if(blkTemp.getAttribute(Block.ATTRIBUTE.BONE)) colorID = -1
							var color = getColorByID(colorID)
							if(showbg) color.a = .5f
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

					if(outlineghost) {
						val blkTemp = it.block[i]
						val x3:Float = (x+x2*blksize).toFloat()
						val y3:Float = (y+y2*blksize).toFloat()
						val ls = blksize*2-1

						var colorID = blkTemp.drawColor
						if(blkTemp.getAttribute(Block.ATTRIBUTE.BONE)) colorID = -1
						var color = getColorByID(colorID)
						if(showbg) color.a = .5f
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
						if(outlineghost) {
							val blkTemp = it.block[i]
							val x3:Float = (x+x2*blksize)
							val y3:Float = (y+y2*blksize)
							val ls = blksize-1

							var colorID = blkTemp.drawColor
							if(blkTemp.getAttribute(Block.ATTRIBUTE.BONE)) colorID = -1
							var color = getColorByID(colorID)
							if(showbg) color.a = .5f
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

					if(outlineghost) {
						val blkTemp = it.block[i]
						val x3:Float = (x+x2*blksize)
						val y3:Float = (y+y2*blksize)
						val ls = blksize*2-1

						var colorID = blkTemp.drawColor
						if(blkTemp.getAttribute(Block.ATTRIBUTE.BONE)) colorID = -1
						var color = getColorByID(colorID)
						if(showbg) color.a = .5f
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
					if(showbg) color.a = .5f
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
	override fun drawFrame(x:Int, y:Int, engine:GameEngine, displaysize:Int) {
		val graphics = graphics ?: return
		val size = when(displaysize) {
			-1 -> {
				2
			}
			1 -> 8
			else -> 4
		}
		val width = engine.field.width ?: Field.DEFAULT_WIDTH
		val height = engine.field.height ?: Field.DEFAULT_HEIGHT
//		val oX = 0

		// Field Background
		if(fieldbgbright>0)
			if(width<=10&&height<=20) {
				val filter = Color(Color.white)
				filter.a = fieldbgbright

				var img = resources.imgFieldBG[1]
				if(displaysize==-1) img = resources.imgFieldBG[0]
				if(displaysize==1) img = resources.imgFieldBG[2]

				graphics.drawImage(
					img, x+4, y+4, x+4+width*size*4, y+4+height*size*4,
					0, 0, (width*size*4), (height*size
						*4), filter
				)
			} else if(showbg) {
				val filter = Color(Color.black)
				filter.a = fieldbgbright
				graphics.color = filter
				graphics.fillRect((x+4), (y+4), (width*size*4), (height*size*4))
				graphics.color = Color.white
			}
		RenderStaffRoll.draw(x+width/2f, y.toFloat(), 0f, height.toFloat(), Color(200, 233, 255, 200))
		super.drawFrame(x, y, engine, displaysize)

	}

	override fun drawBG(engine:GameEngine) {
		val graphics = graphics ?: return
		if(engine.owner.menuOnly) {
			graphics.color = Color.white
			graphics.drawImage(resources.imgMenuBG[1], 0f, 0f)
		} else {
			val bgmax = resources.backgroundMax
			var bg = engine.owner.backgroundStatus.bg%bgmax
			if(engine.owner.backgroundStatus.fadesw&&!heavyeffect) bg = engine.owner.backgroundStatus.fadebg

			if(bg in 0 until bgmax&&showbg) {
				graphics.color = Color.white
				val bgi = resources.imgPlayBG[bg].res
				if(heavyeffect) {
					val sc = (1-cos(bgi.rotation/Math.PI.pow(3.0))/Math.PI).toFloat()*1024f/minOf(bgi.width, bgi.height)
					val cx = bgi.width/2*sc
					val cy = bgi.height/2*sc
					bgi.setCenterOfRotation(cx, cy)
					bgi.rotate(0.04f)
					bgi.draw(320-cx, 240-cy, sc)
					if(engine.owner.backgroundStatus.fadesw) {
						val filter = Color(Color.black)
						filter.a = if(!engine.owner.backgroundStatus.fadestat)
							engine.owner.backgroundStatus.fadecount.toFloat()/100
						else
							(100-engine.owner.backgroundStatus.fadecount).toFloat()/100
						graphics.color = filter
						graphics.fillRect(0, 0, 640, 480)
					}
				} else {
					bgi.rotation = 0f
					graphics.drawImage(bgi, 0, 0, 640, 480, 0, 0, bgi.width, bgi.height)
				}
			}
		}
	}

	fun FragAnim.draw(x:Float, y:Float, x2:Float, y2:Float, srcx:Float, srcy:Float, srcx2:Float, srcy2:Float) {
		when(type) {
			ANIM.BLOCK -> resources.imgBreak[color][1]
			ANIM.SPARK -> resources.imgBreak[color][0]
			ANIM.GEM -> resources.imgPErase[color]
			ANIM.HANABI -> resources.imgHanabi[color]
		}.draw(x, y, x2, y2, srcx, srcy, srcx2, srcy2)
	}

	/** Render effects */
	override fun drawEffectSpecific(i:Int, it:EffectObject) {
		val graphics = graphics ?: return

		when(it) {
			is FragAnim -> {
				val flip = (i%2==0)!=(i%10==0)
				when(it.type) {
					// Gems frag
					ANIM.GEM -> resources.imgPErase[it.color]
					// TI Block frag
					ANIM.SPARK -> resources.imgBreak[it.color][0]
					// TAP Block frag
					ANIM.BLOCK -> resources.imgBreak[it.color][1]
					//Fireworks
					ANIM.HANABI -> resources.imgHanabi[it.color]
				}.draw(
					if(flip) it.dx2 else it.dx, it.dy, if(flip) it.dx else it.dx2, it.dy2,
					it.srcx, it.srcy, it.srcx2, it.srcy2
				)
			}
			is BeamH //Line Cleaned
			-> {
				//if(it.anim%2==1)
				graphics.setDrawMode(Graphics.MODE_ADD)
				resources.imgLine[0].draw(it.x, it.y, it.dx2, it.dy2, it.srcx, it.srcy, it.srcx2, it.srcy2)
				graphics.setDrawMode(Graphics.MODE_NORMAL)
			}
			else -> super.drawEffectSpecific(i, it)

		}
	}

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

		fun getMeterColorAsColor(meterColor:Int, value:Int, max:Int):Color =
			Color(AbstractRenderer.getMeterColorAsColor(meterColor, value, max))
	}
}
