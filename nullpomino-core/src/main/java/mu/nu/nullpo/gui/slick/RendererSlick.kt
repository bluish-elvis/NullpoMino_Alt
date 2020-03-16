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
package mu.nu.nullpo.gui.slick

import mu.nu.nullpo.game.component.Block
import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.game.play.GameManager
import mu.nu.nullpo.gui.common.*
import mu.nu.nullpo.gui.common.FragAnim.ANIM
import mu.nu.nullpo.gui.slick.img.*
import mu.nu.nullpo.util.CustomProperties
import org.lwjgl.input.Keyboard
import org.newdawn.slick.*
import kotlin.math.cos
import kotlin.math.pow

/** ゲームの event 処理と描画処理 (Slick版） */
internal open class RendererSlick:AbstractRenderer() {
	/** 描画先サーフェイス */
	private var graphics:Graphics? = null

	override var resources:mu.nu.nullpo.gui.common.ResourceHolder?
		get() = ResourceHolder
		set(value) {}

	/* TTF使用可能 */
	override val isTTFSupport:Boolean
		get() = ResourceHolder.ttfFont!=null

	override val skinMax:Int
		get() = ResourceHolder.imgBigBlockList.size

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
		showmeter = NullpoMinoSlick.propConfig.getProperty("option.showmeter", true)
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
			FONT.TTF -> FontNormal.printTTF(x, y, str, color, alpha)
			FONT.GRADE -> if(scale>=5f/3f) FontGrade.printBigFont(x, y, str, color, scale/3f)
			else FontGrade.printMiniFont(x, y, str, color, scale/2f)
			else -> FontNormal.printFont(x, y, str, color, scale, alpha)
		}
	}

	override fun printTTFSpecific(x:Int, y:Int, str:String, color:COLOR, alpha:Float) =
		FontNormal.printTTF(x, y, str, color, alpha)

	override fun doesGraphicsExist():Boolean = graphics!=null

	/* スピードMeterを描画 */
	override fun drawSpeedMeter(engine:GameEngine, playerID:Int, x:Int, y:Int, sp:Float) {
		val s = if(sp<0||sp>1) 1f else sp
		val graphics = graphics ?: return
		if(engine.owner.menuOnly) return

		val dx1 = scoreX(engine, playerID)+6+x*BS
		val dy1 = scoreY(engine, playerID)+6+y*BS

		graphics.color = Color.black
		graphics.drawRect(dx1.toFloat(), dy1.toFloat(), 41f, 3f)


		graphics.color = if(s<=0.5) Color.green else if(s<0.75) Color.yellow else Color.orange
		graphics.fillRect((dx1+1).toFloat(), (dy1+1).toFloat(), 40f, 2f)
		if(s<.5f) {
			graphics.color = Color.yellow
			graphics.fillRect((dx1+1).toFloat(), (dy1+1).toFloat(), s*80, 2f)
		}
		if(s<0.75f) {
			graphics.color = Color.orange
			graphics.fillRect((dx1+1).toFloat(), (dy1+1).toFloat(), (s*40f*3f/4f).toInt().toFloat(), 2f)
		}
		graphics.color = Color.red
		graphics.fillRect((dx1+1).toFloat(), (dy1+1).toFloat(), s*40, 2f)

		graphics.color = Color.white
	}

	/* 勲章を描画 */
	override fun drawMedal(x:Int, y:Int, str:String, tier:Int, scale:Float) {
		FontMedal.printFont(x, y, str, tier, scale)
	}

	/* Get key name by button ID */
	override fun getKeyNameByButtonID(playerID:Int, inGame:Boolean, btnID:Int):String {
		val keymap = if(inGame) GameKey.gamekey[playerID].keymap else GameKey.gamekey[playerID].keymapNav

		if(btnID>=0&&btnID<keymap.size) {
			val keycode = keymap[btnID]
			return Keyboard.getKeyName(keycode) ?: "($keycode)"
		}

		return ""
	}

	/* Is the skin sticky? */
	override fun isStickySkin(skin:Int):Boolean {
		return (skin>=0&&skin<ResourceHolder.blockStickyFlagList.size
			&&ResourceHolder.blockStickyFlagList[skin])
	}

	/* Sound effects再生 */
	override fun playSE(name:String, freq:Float, vol:Float) {
		ResourceHolder.soundManager.play(name, false, freq, vol)
	}

	override fun loopSE(name:String, freq:Float, vol:Float) {
		ResourceHolder.soundManager.play(name, true, freq, vol)
	}

	override fun stopSE(name:String) {
		ResourceHolder.soundManager.stop(name)
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

	override fun drawBlockSpecific(x:Int, y:Int, sx:Int, sy:Int, sk:Int,
		size:Int, darkness:Float, alpha:Float) {
		val graphics = graphics ?: return
		val img:Image = when {
			size*2<=BS -> ResourceHolder.imgSmallBlockList[sk]
			size>=BS*2 -> ResourceHolder.imgBigBlockList[sk]
			else -> ResourceHolder.imgNormalBlockList[sk]
		} ?: return
		val filter = Color(Color.white).apply {
			a = alpha
		}.darker(maxOf(0f, darkness))

		val imageWidth = img.width
		if(sx>=imageWidth&&imageWidth!=-1) return
		val imageHeight = img.height
		if(sy>=imageHeight&&imageHeight!=-1) return
		graphics.drawImage(img, x.toFloat(), y.toFloat(), (x+size).toFloat(), (y+size).toFloat(), sx.toFloat(), sy.toFloat(), (sx+size).toFloat(), (sy+size).toFloat(), filter)

		if(darkness<0) {
			val brightfilter = Color(Color.white)
			brightfilter.a = -darkness
			graphics.color = brightfilter
			graphics.fillRect(x.toFloat(), y.toFloat(), size.toFloat(), size.toFloat())
		}
	}

	override fun drawLineSpecific(x:Int, y:Int, sx:Int, sy:Int, color:Int, alpha:Float, w:Float) {
		graphics?.lineWidth = w
		graphics?.color = Color(color+(maxOf(0f, minOf(1f, alpha))*255).toInt() shl 24)
		graphics?.drawLine(x, y, sx, sy)
		graphics?.lineWidth = 1f
	}

	override fun drawOutlineSpecific(i:Int, j:Int, x:Int, y:Int, blksize:Int, blk:Block, outlineType:Int) {
		//TODO:("Not yet implemented")

		val g = graphics ?: return
		val blksize = BS
		/*blk.let {

					val x2 = x+it.dataX[it.direction][i]
					val y2 = y+it.dataY[it.direction][i]

					if(y2>=0) {
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
					}
				}*/
	}

	override fun drawBadgesSpecific(x:Int, y:Int, sx:Int, sy:Int, w:Int, h:Int) {
		graphics?.drawImage(ResourceHolder.imgBadges,
			x.toFloat(), y.toFloat(), (x+w).toFloat(), (y+h).toFloat(),
			sx.toFloat(), sy.toFloat(), (sx+w).toFloat(), (sy+h).toFloat())
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

	/** 現在操作中のBlockピースのghost を描画
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param engine GameEngineのインスタンス
	 * @param scale 表示倍率
	 */
	override fun drawGhostPiece(x:Int, y:Int, engine:GameEngine, scale:Float) {
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
	}

	/** Field frameを描画
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param engine GameEngineのインスタンス
	 */
	private fun drawFrame(x:Int, y:Int, engine:GameEngine, displaysize:Int) {
		val graphics = graphics ?: return
		val size = when(displaysize) {
			-1 -> {
				2
			}
			1 -> 8
			else -> 4
		}
		var width = 10
		var height = 20
		val oX = 0


		engine.field?.let {
			width = it.width
			height = it.height
		}

		// Field Background
		if(fieldbgbright>0)
			if(width<=10&&height<=20) {
				val filter = Color(Color.white)
				filter.a = fieldbgbright

				var img = ResourceHolder.imgFieldbg2
				if(displaysize==-1) img = ResourceHolder.imgFieldbg2Small
				if(displaysize==1) img = ResourceHolder.imgFieldbg2Big

				graphics.drawImage(img, x+4, y+4, x+4+width*size*4, y+4+height*size*4, 0, 0, (width*size*4), (height*size
					*4), filter)
			} else if(showbg) {
				val filter = Color(Color.black)
				filter.a = fieldbgbright
				graphics.color = filter
				graphics.fillRect((x+4), (y+4), (width*size*4), (height*size*4))
				graphics.color = Color.white
			}
		//graphics.drawImage(RenderStaffRoll.BG, x+4, y+4, x+4+width*size*4, y+4+height*size*4, 0, 0, width*size*4, height*size*4);
		// Upと下

		var tmpX = x
		var tmpY = y
		val s = size*4
		var maxWidth = width*s
		var maxHeight = height*size*4
		// NEXT area background
		if(showbg&&darknextarea) {
			val filter = Color(Color.black)
			graphics.color = filter

			when(nextDisplayType) {
				2 -> {//side big
					val x2 = x+s+width*s
					val maxNext = if(engine.isNextVisible) engine.ruleopt.nextDisplay else 0

					// HOLD area
					if(engine.ruleopt.holdEnable&&engine.isHoldVisible) {
						graphics.fillRect(x-64, y, 64, 48)
						for(i in 8 downTo 0) {
							val filter2 = Color(Color.black)
							filter2.a = i.toFloat()/8f
							graphics.color = filter2
							graphics.lineWidth = 1f
							graphics.drawRect(x-64-i, y-i, 65+i, 48+i*2)
						}
					}
					// NEXT area
					if(maxNext>0) {
						graphics.fillRect(x2, y+8, 64, 64*maxNext-16)
						for(i in 8 downTo 0) {
							val filter2 = Color(Color.black)
							filter2.a = i.toFloat()/8f
							graphics.color = filter2
							graphics.lineWidth = 1f
							graphics.drawRect(x2-1, y+8-i, 65+i, 64*maxNext-16+i*2)
						}
					}
				}
				1 -> {//side small
					val x2 = x+s+width*s
					val maxNext = if(engine.isNextVisible) engine.ruleopt.nextDisplay else 0

					// HOLD area
					if(engine.ruleopt.holdEnable&&engine.isHoldVisible) {
						graphics.fillRect(x-32, y, 32, 64)
						for(i in 8 downTo 0) {
							val filter2 = Color(Color.black)
							filter2.a = 1-i.toFloat()/8f
							graphics.color = filter2
							graphics.lineWidth = 1f
							graphics.drawRect(x-32-i, y-i, 33+i, 64+i*2)
						}
					}

					// NEXT area
					if(maxNext>0) {
						graphics.fillRect(x2, y+8, 32, 32*maxNext-16)
						for(i in 8 downTo 0) {
							val filter2 = Color(Color.black)
							filter2.a = 1-i.toFloat()/8f
							graphics.color = filter2
							graphics.lineWidth = 1F
							graphics.drawRect(x2-1, y+8-i, 33+i, 32*maxNext-16+i*2)
						}
					}
				}
				else -> {
					val w = width*s+15

					graphics.fillRect(x+20, y, w-40, -48)

					for(i in 20 downTo 0) {
						val filter2 = Color(Color.black)
						filter2.a = 1-i.toFloat()/20f
						graphics.color = filter2
						graphics.lineWidth = 1f
						graphics.drawRect(x+20-i, y, w-40+i*2, -48-i)
					}
				}
			}

			graphics.color = Color.white
		}

		when(engine.framecolor) {
			GameEngine.FRAME_SKIN_GB -> {
				graphics.color = Color.gray
				graphics.fillRect((x+4), (y+4), (width*size*4), (height*size*4))
				val fi = ResourceHolder.imgFrameOld[0]
				tmpX -= 12
				for(i in 0..height) {

					tmpY = y+i*s+4
					graphics.drawImage(fi, tmpX, tmpY, (tmpX+s), (tmpY+s), 0, 0, 16, 16)
					graphics.drawImage(fi, (tmpX+maxWidth+s), tmpY, (tmpX+maxWidth+s*2), (tmpY+s), 0, 0, 16, 16)
					if(i==height)
						for(z in 1..width)
							graphics.drawImage(fi, (tmpX+z*s), tmpY, (tmpX+(z+1)*s), (tmpY+s), 0, 0, 16, 16)
				}
			}
			GameEngine.FRAME_SKIN_SG -> {
				graphics.color = Color.black
				graphics.fillRect((x+4), (y+4), (width*size*4), (height*size*4))

				val fi = ResourceHolder.imgFrameOld[1]
				maxWidth += s*2
				tmpX -= 12
				tmpY -= 12
				graphics.drawImage(fi, tmpX, tmpY, tmpX+maxWidth/2, tmpY+(height+2)*s, 0, 0, 96, 352)
				graphics.drawImage(fi, tmpX+maxWidth, tmpY, tmpX+maxWidth/2, tmpY+(height+2)*s, 0, 0, 96, 352)
			}
			GameEngine.FRAME_SKIN_HEBO -> {
				val fi = ResourceHolder.imgFrameOld[2]

				graphics.drawImage(fi, tmpX, tmpY, (tmpX+s), (tmpY+s), 0, 0, 16, 16)
				tmpX += 16+(width+2)*s
				graphics.drawImage(fi, tmpX, tmpY, (tmpX), (tmpY+s), 32, 0, 48, 16)
				tmpX = x
				tmpY += 16
				graphics.drawImage(fi, tmpX, tmpY, (tmpX+16), (tmpY+height*s), 0, 15, 16, 1)
				graphics.drawImage(fi, (tmpX+maxWidth), tmpY, (tmpX+16), (tmpY+height*s), 0, 15, 16, 1)
			}
			else -> {
				if(showmeter) maxWidth += 8
				val fi = ResourceHolder.imgFrame[engine.framecolor]
				tmpX = x+4
				tmpY = y
				graphics.drawImage(fi, tmpX, tmpY, (tmpX+maxWidth), (tmpY+4), (oX+4), 0, (oX+4+4), 4)
				tmpY = y+height*size*4+4
				graphics.drawImage(fi, tmpX, tmpY, (tmpX+maxWidth), (tmpY+4), (oX+4), 8, (oX+4+4), (8+4))

				// 左と右
				tmpX = x
				tmpY = y+4
				graphics.drawImage(fi, tmpX, tmpY, (tmpX+4), (tmpY+height*size*4), oX, 4, (oX+4), (4+4))

				tmpX = x+width*size*4+if(showmeter) 12 else 4
				graphics.drawImage(fi, tmpX, tmpY, (tmpX+4), (tmpY+height*size*4), (oX+8), 4, (oX+8+4), 4+4)

				// 左上
				tmpX = x
				tmpY = y
				graphics.drawImage(fi, tmpX, tmpY, (tmpX+4), (tmpY+4), oX, 0, (oX+4), 4)

				// 左下
				tmpX = x
				tmpY = y+height*size*4+4
				graphics.drawImage(fi, tmpX, tmpY, (tmpX+4), (tmpY+4), oX, 8, (oX+4), (8+4))

				if(showmeter) {
					// MeterONのときの右上
					tmpX = x+width*size*4+12
					tmpY = y
					graphics.drawImage(fi, tmpX, tmpY, (tmpX+4), (tmpY+4), (oX+8), 0, (oX+8+4), 4)

					// MeterONのときの右下
					tmpX = x+width*size*4+12
					tmpY = y+height*size*4+4
					graphics.drawImage(fi, tmpX, tmpY, (tmpX+4), (tmpY+4), (oX+8), 8, (oX+8+4), (8+4))

					// 右Meterの枠
					tmpX = x+width*size*4+4
					tmpY = y+4
					graphics.drawImage(fi, tmpX, tmpY, (tmpX+4), (tmpY+height*size*4), (oX+12), 4, (oX+12+4), (4+4))

					tmpX = x+width*size*4+4
					tmpY = y
					graphics.drawImage(fi, tmpX, tmpY, (tmpX+4), (tmpY+4), (oX+12), 0, (oX+12+4), 4)

					tmpX = x+width*size*4+4
					tmpY = y+height*size*4+4
					graphics.drawImage(fi, tmpX, tmpY, (tmpX+4), (tmpY+4), (oX+12), 8, (oX+12+4), (8+4))

				} else {
					// MeterOFFのときの右上
					tmpX = x+width*size*4+4
					tmpY = y
					graphics.drawImage(fi, tmpX, tmpY, (tmpX+4), (tmpY+4), (oX+8), 0, (oX+8+4), 4)

					// MeterOFFのときの右下
					tmpX = x+width*size*4+4
					tmpY = y+height*size*4+4
					graphics.drawImage(fi, tmpX, tmpY, (tmpX+4), (tmpY+4), (oX+8), 8, (oX+8+4), (8+4))
				}
			}
		}
		if(showmeter) {

			// 右Meter
			if(engine.meterValueSub>0||engine.meterValue>0)
				maxHeight -= maxOf(engine.meterValue, engine.meterValueSub)

			tmpX = x+width*size*4+8
			tmpY = y+4

			if(maxHeight>0) {
				graphics.color = Color.black
				graphics.fillRect(tmpX, tmpY, 4, maxHeight)
				graphics.color = Color.white
			}

			if(engine.meterValueSub>maxOf(engine.meterValue, 0)) {
				var value = engine.meterValueSub
				if(value>height*size*4) value = height*size*4

				if(value>0) {
					tmpX = x+width*size*4+8
					tmpY = y+height*size*4+3-(value-1)

					graphics.color = getMeterColorAsColor(engine.meterColorSub, value, maxHeight)
					graphics.fillRect(tmpX, tmpY, 4, value)
					graphics.color = Color.white
				}
			}
			if(engine.meterValue>0) {
				var value = engine.meterValue
				if(value>height*size*4) value = height*size*4

				if(value>0) {
					tmpX = x+width*size*4+8
					tmpY = y+height*size*4+3-(value-1)

					graphics.color = getMeterColorAsColor(engine.meterColor, value, maxHeight)
					graphics.fillRect(tmpX, tmpY, 4, value)
					graphics.color = Color.white
				}
			}
		}

	}

	/** 各 frame 最初の描画処理
	 * @param engine GameEngine
	 * @param playerID Player ID
	 */
	override fun renderFirst(engine:GameEngine, playerID:Int) {
		val graphics = graphics ?: return

		if(engine.playerID==0)
			if(engine.owner.menuOnly) {
				graphics.color = Color.white
				graphics.drawImage(ResourceHolder.imgMenuBG[1], 0f, 0f)
			} else {
				val bgmax = ResourceHolder.bGmax
				var bg = engine.owner.backgroundStatus.bg%bgmax
				if(engine.owner.backgroundStatus.fadesw&&!heavyeffect) bg = engine.owner.backgroundStatus.fadebg

				if(bg in 0 until bgmax&&showbg) {
					graphics.color = Color.white
					val bgi = ResourceHolder.imgPlayBG[bg]
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
		// NEXTなど
		if(!engine.owner.menuOnly&&engine.isVisible) {
			val offsetX = fieldX(engine, playerID)
			val offsetY = fieldY(engine, playerID)

			if(engine.displaysize!=-1) {
				drawFrame(offsetX, offsetY+48, engine, engine.displaysize)
				drawNext(offsetX, offsetY, engine)
				drawField(offsetX+4, offsetY+52, engine, engine.displaysize)
			} else {
				drawFrame(offsetX, offsetY, engine, -1)
				drawField(offsetX+4, offsetY+4, engine, -1)
			}
		}
	}

	/** Render effects */
	override fun effectRender() {
		val graphics = graphics ?: return
		effectlist.forEachIndexed {i, it ->
			var x = it.x.toInt()
			var y = it.y.toInt()
			val srcx:Int
			val srcy:Int
			val sq:Int

			when {
				it is FragAnim
				->
					when(it.type) {
						ANIM.GEM// Gems frag
						-> {
							x -= 16
							y -= 16
							sq = 64
							srcx = (it.anim-1)%10*sq
							srcy = (it.anim-1)/10*sq
							graphics.drawImage(ResourceHolder.imgPErase[it.color], x, y, x+sq, y+sq, srcx, srcy, srcx+sq, srcy+sq)
						}
						ANIM.SPARK // TI Block frag
						-> {
							x -= 88
							y -= 38
							sq = 192
							srcx = (it.anim-1)%6*sq
							srcy = (it.anim-1)/6*sq
							val flip = (i%3==0)!=(x%3==0)
							ResourceHolder.imgBreak[it.color][0].draw(
								if(flip) x else x+sq, y, if(flip) x+sq else x, y+sq,
								srcx, srcy, srcx+sq, srcy+sq)
						}

						ANIM.BLOCK // TAP Block frag
						-> {
							x -= 88
							y -= 86
							sq = 192
							srcx = (it.anim-1)%8*sq
							srcy = (it.anim-1)/8*sq
							val flip = (i%3==0)!=(x.toInt()%3==0)
							ResourceHolder.imgBreak[it.color][1].draw(if(flip) x else x+sq, y, if(flip) x+sq else x, y+sq,
								srcx, srcy, srcx+sq, srcy+sq)
						}
						ANIM.HANABI //Fireworks
						-> {
							sq = 192
							x -= sq/2
							y -= sq/2
							srcx = (it.anim-1)%6*sq
							srcy = (it.anim-1)/8*sq
							ResourceHolder.imgHanabi[it.color].draw(x, y, x+sq, y+sq, srcx, srcy, srcx+sq, srcy+sq)
						}
					}
				it is BeamH //Line Cleaned
				-> {
					srcy = (it.anim/2-1)*8
					if(it.anim%2==1) graphics.setDrawMode(Graphics.MODE_ADD)
					ResourceHolder.imgLine[0].draw(x, y, x+it.w, y+it.h, 0, srcy, 80, srcy+8)
					graphics.setDrawMode(Graphics.MODE_NORMAL)
				}
				it is PopupAward ->
					drawAward(x, y, it.event, it.anim)
				it is PopupCombo ->
					drawCombo(x, y, it.pts, it.type)
				it is PopupPoint ->
					drawDirectNum(x-(it.pts.toString().length*6), y, "+${it.pts}", COLOR.values()[it.color])

				it is PopupBravo //Field Cleaned
				-> {
					drawDirectFont(x+20, y+204, "BRAVO!", getRainbowColor((it.anim+4)%9), 1.5f)
					drawDirectFont(x+52, y+236, "PERFECT", getRainbowColor(it.anim%9), 1f)
				}
			}
		}
	}

	companion object {

		/** Block colorIDに応じてSlick用Colorオブジェクトを作成・取得
		 * @param colorID Block colorID
		 * @return Slick用Colorオブジェクト
		 */
		fun getColorByID(colorID:Int):Color = when(colorID) {
			Block.BLOCK_COLOR_GRAY -> Color(Color.gray)
			Block.BLOCK_COLOR_RED -> Color(Color.red)
			Block.BLOCK_COLOR_ORANGE -> Color(Color.orange)
			Block.BLOCK_COLOR_YELLOW -> Color(Color.yellow)
			Block.BLOCK_COLOR_GREEN -> Color(Color.green)
			Block.BLOCK_COLOR_CYAN -> Color(Color.cyan)
			Block.BLOCK_COLOR_BLUE -> Color(Color.blue)
			Block.BLOCK_COLOR_PURPLE -> Color(Color.magenta)
			else -> Color(Color.black)
		}

		fun getColorByID(color:Block.COLOR):Color = when(color) {
			Block.COLOR.WHITE -> Color(Color.gray)
			Block.COLOR.RED -> Color(Color.red)
			Block.COLOR.ORANGE -> Color(Color.orange)
			Block.COLOR.YELLOW -> Color(Color.yellow)
			Block.COLOR.GREEN -> Color(Color.green)
			Block.COLOR.CYAN -> Color(Color.cyan)
			Block.COLOR.BLUE -> Color(Color.blue)
			Block.COLOR.PURPLE -> Color(Color.magenta)
			else -> Color(Color.black)
		}

		fun getMeterColorAsColor(meterColor:Int, value:Int, max:Int):Color {
			var color = Color(0, 0, 0)
			when(meterColor) {
				GameEngine.METER_COLOR_LEVEL -> {
					color.r = maxOf(0f, minOf((value*3f-max)/max, 1f))
					color.g = maxOf(0f, minOf((max-value)*3f/max, 1f))
					color.b = maxOf(0f, minOf((max-value*3f)/max, 1f))
				}
				GameEngine.METER_COLOR_LIMIT -> {//red<yellow<green<cyan
					color.r = maxOf(0f, minOf((max*2f-value*3f)/max, 1f))
					color.g = value*3f/max
					color.b = maxOf(0f, minOf((value*3f-max*2f)/max, 1f))
				}
				else -> color = Color(meterColor)
			}
			return color
		}
	}
}