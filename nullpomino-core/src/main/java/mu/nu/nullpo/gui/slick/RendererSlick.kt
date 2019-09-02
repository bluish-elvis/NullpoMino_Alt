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
import mu.nu.nullpo.game.component.Piece
import mu.nu.nullpo.game.event.EventReceiver
import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.game.play.GameManager
import mu.nu.nullpo.gui.EffectObject
import mu.nu.nullpo.gui.slick.img.*
import mu.nu.nullpo.util.CustomProperties
import org.lwjgl.input.Keyboard
import org.newdawn.slick.*
import java.util.*
import kotlin.math.cos
import kotlin.math.pow

/** ゲームの event 処理と描画処理 (Slick版） */
open class RendererSlick:EventReceiver() {
	enum class GFX {
		BLOCK, SPARK, GEM, HANABI, LINE, SPLASH, BRAVO
	}
	/** Log */
	//static Logger log = Logger.getLogger(RendererSlick.class);

	/** 描画先サーフェイス */
	private var graphics:Graphics? = null

	/** 演出オブジェクト */
	private val effectlist:ArrayList<EffectObject>

	/** Line clearエフェクト表示 */
	private val showlineeffect:Boolean

	/** 重い演出を使う */
	private val heavyeffect:Boolean

	/** fieldBackgroundの明るさ */
	private val fieldbgbright:Float

	/** Show field BG grid */
	private val showfieldbggrid:Boolean

	/** NEXT欄を暗くする */
	private val darknextarea:Boolean

	/** ghost ピースの上にNEXT表示 */
	private val nextshadow:Boolean

	/** Line clear effect speed */
	private val lineeffectspeed:Int

	/** 操作ブロック降下を滑らかにする */
	private val smoothfall:Boolean
	/** 高速落下時の軌道を表示する */
	private val showLocus:Boolean
	/** 回転軸を表示する */
	private val showCenter:Boolean

	/* TTF使用可能 */
	override val isTTFSupport:Boolean
		get() = ResourceHolder.ttfFont!=null

	override val skinMax:Int
		get() = ResourceHolder.imgBigBlockList.size

	/** Constructor */
	init {
		graphics = null
		effectlist = ArrayList(10*4)

		showbg = NullpoMinoSlick.propConfig.getProperty("option.showbg", true)
		if(ResourceHolder.bGmax==0) showbg = false
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

	/* Menu 用の文字列を描画 */
	override fun drawMenu(engine:GameEngine, playerID:Int, x:Int, y:Int, str:String, font:FONT, color:COLOR,
		scale:Float) {
		val s = if(scale<=.5f) BS/2 else BS
		var x = x*s
		var y = y*s
		if(!engine.owner.menuOnly) {
			x += fieldX(engine, playerID)+4
			y += fieldY(engine, playerID)+52
		}
		drawDirect(x, y, str, font, color, scale)
	}

	/* Render score用の font を描画 */
	override fun drawScore(engine:GameEngine, playerID:Int, x:Int, y:Int, str:String, font:FONT, color:COLOR,
		scale:Float) {
		if(engine.owner.menuOnly) return
		val s = if(scale<=.5f) BS/2 else BS
		drawDirect(scoreX(engine, playerID)+x*s,
			scoreY(engine, playerID)+y*s, str, font, color, scale)
	}

	/* 直接指定した座標へ文字列を描画 */
	override fun drawDirect(x:Int, y:Int, str:String, font:FONT, color:COLOR, scale:Float) {
		when(font) {
			FONT.NANO -> FontNano.printFont(x, y, str, color, scale)
			FONT.NUM -> FontNumber.printFont(x, y, str, color, scale)
			FONT.TTF -> FontNormal.printTTF(x, y, str, color)
			FONT.GRADE -> if(scale>=5f/3f) FontGrade.printBigFont(x, y, str, color, scale/3f)
			else FontGrade.printMiniFont(x, y, str, color, scale/2f)
			else -> FontNormal.printFont(x, y, str, color, scale)
		}

	}

	/* スピードMeterを描画 */
	override fun drawSpeedMeter(engine:GameEngine, playerID:Int, x:Int, y:Int, s:Float) {
		var s = s
		val graphics = graphics ?: return
		if(engine.owner.menuOnly) return

		val dx1 = scoreX(engine, playerID)+6+x*BS
		val dy1 = scoreY(engine, playerID)+6+y*BS

		graphics.color = Color.black
		graphics.drawRect(dx1.toFloat(), dy1.toFloat(), 41f, 3f)

		if(s<0||s>1) s = 1f

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

	override fun drawMenuBadges(engine:GameEngine, playerID:Int, x:Int, y:Int, nums:Int, scale:Float) {
		var x = x*BS
		var y = y*BS
		if(!engine.owner.menuOnly) {
			x += fieldX(engine, playerID)+4
			y += fieldY(engine, playerID)+52
		}
		drawBadges(x, y, BS*10, nums, scale)
	}

	override fun drawScoreBadges(engine:GameEngine, playerID:Int, x:Int, y:Int, width:Int, nums:Int,
		scale:Float) {
		drawBadges(scoreX(engine, playerID)+x*BS, scoreY(engine, playerID)+y*BS, width, nums, scale)
	}

	/* 勲章を描画 */
	override fun drawBadges(x:Int, y:Int, width:Int, nums:Int, scale:Float) {
		var nums = nums
		var nx = x
		var ny = y
		var z:Int
		var mh = 0
		val img = ResourceHolder.imgBadges
		val sx = intArrayOf(0, 10, 20, 30, 0, 10, 20, 30, 0, 20, 40, 0)
		val sy = intArrayOf(0, 0, 0, 0, 14, 14, 14, 14, 24, 24, 0, 44)
		val w = intArrayOf(10, 10, 10, 10, 10, 10, 10, 10, 20, 20, 32, 64)
		val h = intArrayOf(10, 10, 14, 14, 14, 14, 15, 15, 15, 15, 32, 48)
		val b = intArrayOf(1, 5, 10, 50, 100, 500, 1000, 5000, 10000, 50000, 100000, 500000)
		while(nums>0) {
			z = 0
			while(z<b.size-1&&nums>=b[z+1]) {
				if(mh<h[z]) mh = h[z]
				z++
			}
			graphics?.drawImage(img, nx.toFloat(), ny.toFloat(), nx+w[z]*scale, ny+h[z]*scale,
				sx[z].toFloat(), sy[z].toFloat(), (sx[z]+w[z]).toFloat(), (sy[z]+h[z]).toFloat())
			nums -= b[z]
			nx += (w[z]*scale).toInt()
			if(width>w[0]*scale&&nx>=x+width) {
				nx = x
				ny += (mh*scale).toInt()
				mh = 0
			}
		}
	}

	override fun drawMenuMedal(engine:GameEngine, playerID:Int, x:Int, y:Int, str:String, tier:Int, scale:Float) {
		var x = x
		var y = y
		x *= BS
		y *= BS
		if(!engine.owner.menuOnly) {
			x += fieldX(engine, playerID)+4
			y += fieldY(engine, playerID)+52
		}
		drawMedal(x, y, str, tier, scale)
	}

	override fun drawScoreMedal(engine:GameEngine, playerID:Int, x:Int, y:Int, str:String, tier:Int,
		scale:Float) {
		drawMedal(scoreX(engine, playerID)+x*BS, scoreY(engine, playerID)+y*BS, str, tier, scale)
	}

	/* 勲章を描画 */
	override fun drawMedal(x:Int, y:Int, str:String, tier:Int, scale:Float) {
		FontMedal.printFont(x, y, str, tier, scale)
	}

	/* Get key name by button ID */
	override fun getKeyNameByButtonID(engine:GameEngine, btnID:Int):String {
		val keymap = if(engine.isInGame) GameKey.gamekey[engine.playerID].keymap else GameKey.gamekey[engine.playerID].keymapNav

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
	override fun playSE(name:String) {
		ResourceHolder.soundManager.play(name)
	}

	override fun playSE(name:String, freq:Float) {
		ResourceHolder.soundManager.play(name, false, freq, 1f)
	}

	override fun loopSE(name:String) {
		ResourceHolder.soundManager.play(name, true)
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
		if(owner.mode!!.isNetplayMode) return

		super.saveReplay(owner, prop, NullpoMinoSlick.propGlobal.getProperty("custom.replay.directory", foldername))
	}

	/* 1マスBlockを描画 */
	override fun drawSingleBlock(engine:GameEngine, playerID:Int, x:Int, y:Int, color:Int, skin:Int, bone:Boolean,
		darkness:Float, alpha:Float, scale:Float) {
		drawBlock(x, y, color, skin, bone, darkness, alpha, scale)
	}

	/** Draw a block
	 * @param x X pos
	 * @param y Y pos
	 * @param color Color
	 * @param skin Skin
	 * @param bone true to use bone block ([][][][])
	 * @param darkness Darkness or brightness
	 * @param alpha Alpha
	 * @param scale Size (.5f, 1f, 2f)
	 * @param attr Attribute
	 */
	protected fun drawBlock(x:Int, y:Int, color:Int, skin:Int, bone:Boolean, darkness:Float, alpha:Float, scale:Float,
		attr:Int = 0) {
		val skin = skin%ResourceHolder.imgNormalBlockList.size
		val graphics = graphics ?: return

		if(color<=Block.BLOCK_COLOR_INVALID) return

		val isSpecialBlocks = color>=Block.BLOCK_COLOR_GEM_RED
		val isSticky = ResourceHolder.blockStickyFlagList[skin]

		val size = (16*scale).toInt()
		val img:Image = when(scale) {
			.5f -> ResourceHolder.imgSmallBlockList[skin]
			2f -> ResourceHolder.imgBigBlockList[skin]
			else -> ResourceHolder.imgNormalBlockList[skin]
		}

		var sx = color*size
		if(bone) sx += 9*size
		var sy = 0
		if(isSpecialBlocks) sx = (color-Block.BLOCK_COLOR_GEM_RED+18)*size

		if(isSticky)
			if(isSpecialBlocks) {
				sx = (color-Block.BLOCK_COLOR_COUNT)*size
				sy = 18*size
			} else {
				sx = 0
				if(attr and Block.ATTRIBUTE.CONNECT_UP.bit!=0) sx = sx or 0x1
				if(attr and Block.ATTRIBUTE.CONNECT_DOWN.bit!=0) sx = sx or 0x2
				if(attr and Block.ATTRIBUTE.CONNECT_LEFT.bit!=0) sx = sx or 0x4
				if(attr and Block.ATTRIBUTE.CONNECT_RIGHT.bit!=0) sx = sx or 0x8
				sx *= size
				sy = color*size
				if(bone) sy += 9*size
			}

		val imageWidth = img.width
		if(sx>=imageWidth&&imageWidth!=-1) sx = 0
		val imageHeight = img.height
		if(sy>=imageHeight&&imageHeight!=-1) sy = 0

		var filter = Color(Color.white)
		filter.a = alpha
		if(darkness>0) filter = filter.darker(darkness)

		graphics.drawImage(img, x, y, (x+size), (y+size), sx, sy, (sx+size), (sy+size), filter)

		if(isSticky&&!isSpecialBlocks) {
			val d = 16*size
			val h = size/2

			if(attr and Block.ATTRIBUTE.CONNECT_UP.bit!=0&&attr and Block.ATTRIBUTE.CONNECT_LEFT.bit!=0)
				graphics.drawImage(img, x, y, (x+h), (y+h), d, sy, (d+h), (sy+h), filter)
			if(attr and Block.ATTRIBUTE.CONNECT_UP.bit!=0&&attr and Block.ATTRIBUTE.CONNECT_RIGHT.bit!=0)
				graphics.drawImage(img, (x+h), y, (x+h+h), (y+h), (d+h), sy, (d+h+h), (sy+h), filter)
			if(attr and Block.ATTRIBUTE.CONNECT_DOWN.bit!=0&&attr and Block.ATTRIBUTE.CONNECT_LEFT.bit!=0)
				graphics.drawImage(img, x, (y+h), (x+h), (y+h+h), d, (sy+h), (d+h), (sy+h+h), filter)
			if(attr and Block.ATTRIBUTE.CONNECT_DOWN.bit!=0&&attr and Block.ATTRIBUTE.CONNECT_RIGHT.bit!=0)
				graphics.drawImage(img, (x+h), (y+h), (x+h+h), (y+h+h), (d+h), (sy+h), (d+h+h), (sy+h+h), filter)
		}

		if(darkness<0) {
			val brightfilter = Color(Color.white)
			brightfilter.a = -darkness
			graphics.color = brightfilter
			graphics.fillRect(x.toFloat(), y.toFloat(), size.toFloat(), size.toFloat())
		}
	}

	/** Blockクラスのインスタンスを使用してBlockを描画 (拡大率と暗さ指定可能）
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param blk Blockクラスのインスタンス
	 * @param scale 拡大率
	 * @param darkness 暗さもしくは明るさ
	 */
	protected fun drawBlock(x:Int, y:Int, blk:Block, scale:Float = 1f, darkness:Float = blk.darkness,
		alpha:Float = blk.alpha) =
		drawBlock(x, y, blk.drawColor, blk.skin, blk.getAttribute(Block.ATTRIBUTE.BONE), darkness, alpha, scale, blk.aint)

	protected fun drawBlockForceVisible(x:Int, y:Int, blk:Block, scale:Float) =
		drawBlock(x, y, blk.drawColor, blk.skin, blk.getAttribute(Block.ATTRIBUTE.BONE), blk.darkness, .5f*blk.alpha+.5f, scale, blk.aint)

	/** Blockピースを描画 (暗さもしくは明るさの指定可能）
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param piece 描画するピース
	 * @param scale 拡大率
	 * @param darkness 暗さもしくは明るさ
	 */
	private fun drawPiece(x:Int, y:Int, piece:Piece, scale:Float = 1f, darkness:Float = 0f, alpha:Float = 1f) {
		for(i in 0 until piece.maxBlock) {
			val ls = 16f*scale
			val x2 = x+(piece.dataX[piece.direction][i].toFloat()*ls)
			val y2 = y+(piece.dataY[piece.direction][i].toFloat()*ls)

			val blkTemp = Block(piece.block[i])
			blkTemp.darkness += darkness
			blkTemp.alpha = blkTemp.alpha*alpha
			drawBlock(x2.toInt(), y2.toInt(), blkTemp, scale)
			graphics?.let {
				it.resetLineWidth()
				val filter = Color(Color.white)
				it.color = filter

				if(!blkTemp.getAttribute(Block.ATTRIBUTE.CONNECT_UP)) {
					it.drawLine(x2, y2, (x2+ls), y2)
					it.drawLine(x2, (y2+1), (x2+ls), (y2+1))
				}
				if(!blkTemp.getAttribute(Block.ATTRIBUTE.CONNECT_DOWN)) {
					it.drawLine(x2, (y2+ls), (x2+ls), (y2+ls))
					it.drawLine(x2, (y2-1+ls), (x2+ls), (y2-1+ls))
				}
				if(!blkTemp.getAttribute(Block.ATTRIBUTE.CONNECT_LEFT)) {
					it.drawLine(x2, y2, x2, (y2+ls))
					it.drawLine((x2+1), y2, (x2+1), (y2+ls))
				}
				if(!blkTemp.getAttribute(Block.ATTRIBUTE.CONNECT_RIGHT)) {
					it.drawLine((x2+ls), y2, (x2+ls), (y2+ls))
					it.drawLine((x2-1+ls), y2, (x2-1+ls), (y2+ls))
				}
			}
		}
	}

	/** 現在操作中のBlockピースを描画 (Y-coordinateが0以上のBlockだけ表示）
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param engine GameEngineのインスタンス
	 * @param scale 表示倍率
	 */
	private fun drawCurrentPiece(x:Int, y:Int, engine:GameEngine, scale:Float) {
		val blksize = (16*scale).toInt()
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
					drawBlock(x+((x2+bx).toFloat()*16f*scale).toInt(), y+((y2+by-z).toFloat()*16f*scale).toInt(), b, scale*if(engine.big) 2 else 1, -.1f, .4f)
					i++
				}
				if(z==0) drawPiece(x+bx*blksize, y+by*blksize+ys, it, scale*if(engine.big) 2 else 1)
			}
		}
	}

	/** 現在操作中のBlockピースのghost を描画
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param engine GameEngineのインスタンス
	 * @param scale 表示倍率
	 */
	private fun drawGhostPiece(x:Int, y:Int, engine:GameEngine, scale:Float) {
		val g = graphics ?: return
		val blksize = (16*scale).toInt()

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

	private fun drawHintPiece(x:Int, y:Int, engine:GameEngine, scale:Float) {

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

	/** fieldのBlockを描画
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param engine GameEngineのインスタンス
	 */
	private fun drawField(x:Int, y:Int, engine:GameEngine, size:Int) {
		val g = graphics ?: return

		var blksize = 16
		var scale = 1f
		if(size==-1) {
			blksize = 8
			scale = .5f
		} else if(size==1) {
			blksize = 32
			scale = 2f
		}

		val field = engine.field ?: return
		val width = field.width
		val height = field.height
		val viewHeight = height-
			if(engine.heboHiddenEnable&&engine.gameActive) engine.heboHiddenYNow
			else 0

		var outlineType = engine.blockOutlineType
		if(engine.owBlockOutlineType!=-1) outlineType = engine.owBlockOutlineType

		for(i in -field.hiddenHeight until viewHeight)
			for(j in 0 until width) {
				val x2 = x+j*blksize
				val y2 = y+i*blksize

				field.getBlock(j, i)?.also {blk ->
					if(blk.color!=null) {
						if(blk.getAttribute(Block.ATTRIBUTE.WALL))
							drawBlock(x2, y2, 0, blk.skin, blk.getAttribute(Block.ATTRIBUTE.BONE), blk.darkness, blk.alpha, scale, blk.aint)
						else if(engine.owner.replayMode&&engine.owner.replayShowInvisible)
							drawBlockForceVisible(x2, y2, blk, scale)
						else if(blk.getAttribute(Block.ATTRIBUTE.VISIBLE)) drawBlock(x2, y2, blk, scale)

						if(blk.getAttribute(Block.ATTRIBUTE.OUTLINE)&&!blk.getAttribute(Block.ATTRIBUTE.BONE)) {
							val filter = Color(Color.white)
							filter.a = blk.alpha
							g.color = filter
							val ls = blksize-1
							when(outlineType) {
								GameEngine.BLOCK_OUTLINE_NORMAL -> {
									if(field.getBlockColor(j, i-1)==Block.BLOCK_COLOR_NONE) g.drawLine(x2, y2, (x2+ls), y2)
									if(field.getBlockColor(j, i+1)==Block.BLOCK_COLOR_NONE) g.drawLine(x2, (y2+ls), (x2+ls), (y2+ls))
									if(field.getBlockColor(j-1, i)==Block.BLOCK_COLOR_NONE) g.drawLine(x2, y2, x2, (y2+ls))
									if(field.getBlockColor(j+1, i)==Block.BLOCK_COLOR_NONE) g.drawLine((x2+ls), y2, (x2+ls), (y2+ls))
								}
								GameEngine.BLOCK_OUTLINE_CONNECT -> {
									if(!blk.getAttribute(Block.ATTRIBUTE.CONNECT_UP)) g.drawLine(x2, y2, (x2+ls), y2)
									if(!blk.getAttribute(Block.ATTRIBUTE.CONNECT_DOWN)) g.drawLine(x2, (y2+ls), (x2+ls), (y2+ls))
									if(!blk.getAttribute(Block.ATTRIBUTE.CONNECT_LEFT)) g.drawLine(x2, y2, x2, (y2+ls))
									if(!blk.getAttribute(Block.ATTRIBUTE.CONNECT_RIGHT)) g.drawLine((x2+ls), y2, (x2+ls), (y2+ls))
								}
								GameEngine.BLOCK_OUTLINE_SAMECOLOR -> {
									g.color = getColorByID(blk.cint)
									if(field.getBlockColor(j, i-1)!=blk.cint) g.drawLine(x2, y2, (x2+ls), y2)
									if(field.getBlockColor(j, i+1)!=blk.cint) g.drawLine(x2, (y2+ls), (x2+ls), (y2+ls))
									if(field.getBlockColor(j-1, i)!=blk.cint) g.drawLine(x2, y2, x2, (y2+ls))
									if(field.getBlockColor(j+1, i)!=blk.cint) g.drawLine((x2+ls), y2, (x2+ls), (y2+ls))
								}
							}
						}

						g.color = Color.white
					}
				}

			}

		// ヘボHIDDEN
		if(engine.heboHiddenEnable&&engine.gameActive) {
			var maxY = engine.heboHiddenYNow
			if(maxY>height) maxY = height
			for(i in 0 until maxY)
				for(j in 0 until width)
					drawBlock(x+j*blksize, y+(height-1-i)*blksize, 0, 0, false, .3f, 1f, scale)
		}
	}

	/** Field frameを描画
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param engine GameEngineのインスタンス
	 */
	private fun drawFrame(x:Int, y:Int, engine:GameEngine, displaysize:Int) {
		val graphics = graphics ?: return
		var next = engine.isNextVisible||engine.isHoldVisible
		val size = when(displaysize) {
			-1 -> {
				next = false
				2
			}
			1 -> 8
			else -> 4
		}
		var width:Int = 10
		var height:Int = 20
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

		when {
			engine.framecolor==GameEngine.FRAME_SKIN_GB -> {
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
			engine.framecolor==GameEngine.FRAME_SKIN_SG -> {
				graphics.color = Color.black
				graphics.fillRect((x+4), (y+4), (width*size*4), (height*size*4))

				val fi = ResourceHolder.imgFrameOld[1]
				maxWidth += s*2
				tmpX -= 12
				tmpY -= 12
				graphics.drawImage(fi, tmpX, tmpY, tmpX+maxWidth/2, tmpY+(height+2)*s, 0, 0, 96, 352)
				graphics.drawImage(fi, tmpX+maxWidth, tmpY, tmpX+maxWidth/2, tmpY+(height+2)*s, 0, 0, 96, 352)
			}
			engine.framecolor==GameEngine.FRAME_SKIN_HEBO -> {
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

	/** NEXTを描画
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param engine GameEngineのインスタンス
	 */
	private fun drawNext(x:Int, y:Int, engine:GameEngine) {
		graphics ?: return

		var fldWidth = 11
		var fldBlkSize = 16
		engine.field?.let {
			fldWidth = it.width+1
			if(engine.displaysize==1) fldBlkSize = 32
		}
		if(engine.isNextVisible)
			when(nextDisplayType) {
				2 -> if(engine.ruleopt.nextDisplay>=1) {
					val x2 = x+8+fldWidth*fldBlkSize
					//FontNormal.printFont(x2+16,y+40,NullpoMinoSlick.getUIText("InGame_Next"),COLOR_ORANGE,.5f);
					FontNano.printFont(x2+16, y+40, "NEXT", COLOR.ORANGE)
					for(i in 0 until engine.ruleopt.nextDisplay) {
						engine.getNextObject(engine.nextPieceCount+i)?.let {piece ->
							val centerX = (64-(piece.width+1)*16)/2-piece.minimumBlockX*16
							val centerY = (64-(piece.height+1)*16)/2-piece.minimumBlockY*16
							drawPiece(x2+centerX, y+48+i*64+centerY, piece, 1f)
						}
					}
				}
				1 -> if(engine.ruleopt.nextDisplay>=1) {
					val x2 = x+8+fldWidth*fldBlkSize
					//FontNormal.printFont(x2,y+40,NullpoMinoSlick.getUIText("InGame_Next"),COLOR_ORANGE,.5f);
					FontNano.printFont(x2, y+40, "NEXT", COLOR.ORANGE)
					for(i in 0 until engine.ruleopt.nextDisplay) {
						engine.getNextObject(engine.nextPieceCount+i)?.let {
							val centerX = (32-(it.width+1)*8)/2-it.minimumBlockX*8
							val centerY = (32-(it.height+1)*8)/2-it.minimumBlockY*8
							drawPiece(x2+centerX, y+48+i*32+centerY, it, .5f)
						}
					}
				}
				else -> {
					// NEXT1
					if(engine.ruleopt.nextDisplay>=1) {
						//FontNormal.printFont(x+60,y,NullpoMinoSlick.getUIText("InGame_Next"),COLOR_ORANGE,.5f);
						FontNano.printFont(x+60, y, "NEXT", COLOR.ORANGE)
						engine.getNextObject(engine.nextPieceCount)?.let {
							//int x2 = x + 4 + ((-1 + (engine.field.getWidth() - piece.getWidth() + 1) / 2) * 16);
							val x2 = x+4+engine.getSpawnPosX(engine.field, it)*fldBlkSize //Rules with spawn x modified were misaligned.
							val y2 = y+48-(it.maximumBlockY+1)*16
							drawPiece(x2, y2, it)
						}
					}

					// NEXT2・3
					for(i in 0 until minOf(2, engine.ruleopt.nextDisplay-1))
						engine.getNextObject(engine.nextPieceCount+i+1)?.let {
							drawPiece(x+124+i*40, y+48-(it.maximumBlockY+1)*8, it, .5f)
						}

					// NEXT4～
					for(i in 0 until engine.ruleopt.nextDisplay-3) engine.getNextObject(engine.nextPieceCount+i+3)?.let {
						if(showmeter) drawPiece(x+176, y+i*40+88-(it.maximumBlockY+1)*8, it, .5f)
						else drawPiece(x+168, y+i*40+88-(it.maximumBlockY+1)*8, it, .5f)
					}
				}
			}

		if(engine.isHoldVisible) {
			// HOLD
			val holdRemain = engine.ruleopt.holdLimit-engine.holdUsedCount
			val x2 = if(sidenext) x-32 else x + if(nextDisplayType==2) -48 else 0
			val y2 = if(sidenext) y+40 else y + if(nextDisplayType==0) 16 else 0

			if(engine.ruleopt.holdEnable&&(engine.ruleopt.holdLimit<0||holdRemain>0)) {
				var str = "SWAP"
				var tempColor = if(engine.holdDisable) COLOR.WHITE else COLOR.GREEN
				if(engine.ruleopt.holdLimit>=0) {
					str += "\ne$holdRemain"
					if(!engine.holdDisable&&holdRemain>0&&holdRemain<=10)
						tempColor = if(holdRemain<=5) COLOR.RED else COLOR.YELLOW
				}
				FontNano.printFont(x2, y2, str, tempColor)

				engine.holdPieceObject?.let {
					var dark = 0f
					if(engine.holdDisable) dark = .3f
					it.resetOffsetArray()
					it.setDarkness(0f)

					when(nextDisplayType) {
						2 -> {
							val centerX = (64-(it.width+1)*16)/2-it.minimumBlockX*16
							val centerY = (64-(it.height+1)*16)/2-it.minimumBlockY*16
							drawPiece(x-64+centerX, y+48+centerY, it, 1f, dark)
						}
						1 -> {
							val centerX = (32-(it.width+1)*8)/2-it.minimumBlockX*8
							val centerY = (32-(it.height+1)*8)/2-it.minimumBlockY*8
							drawPiece(x2+centerX, y+48+centerY, it, .5f, dark)
						}
						else -> drawPiece(x2, y+48-(it.maximumBlockY+1)*8, it, .5f, dark)
					}
				}
			}
		}
	}

	/** Draw shadow nexts
	 * @param x X coord
	 * @param y Y coord
	 * @param engine GameEngine
	 * @param scale Display size of piece
	 */
	private fun drawShadowNexts(x:Int, y:Int, engine:GameEngine, scale:Float) {
		val blksize = (16*scale).toInt()

		engine.nowPieceObject?.let {piece ->
			val shadowX = engine.nowPieceX
			val shadowY = engine.nowPieceBottomY+piece.minimumBlockY

			for(i in 0 until engine.ruleopt.nextDisplay-1) {
				if(i>=3) break

				engine.getNextObject(engine.nextPieceCount+i)?.let {next ->
					val size = if(piece.big||engine.displaysize==1) 2 else 1
					val shadowCenter = blksize*piece.minimumBlockX+blksize*(piece.width+size)/2
					val nextCenter = blksize/2*next.minimumBlockX+blksize/2*(next.width+1)/2
					val vPos = blksize*shadowY-(i+1)*24-8

					if(vPos>=-blksize/2)
						drawPiece(x+blksize*shadowX+shadowCenter-nextCenter, y+vPos, next, .5f*scale, .1f)
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

	/* Ready画面の描画処理 */
	override fun renderReady(engine:GameEngine, playerID:Int) {
		graphics ?: return
		if(!engine.allowTextRenderByReceiver) return
		//if(engine.isVisible == false) return;

		if(engine.statc[0]>0) {
			val offsetX = fieldX(engine, playerID)
			val offsetY = fieldY(engine, playerID)

			if(engine.statc[0]>0)
				if(engine.displaysize!=-1) {
					if(engine.statc[0]>=engine.readyStart&&engine.statc[0]<engine.readyEnd)
						FontNormal.printFont(offsetX+4, offsetY+196, "READY", COLOR.WHITE, 2f)
					else if(engine.statc[0]>=engine.goStart&&engine.statc[0]<engine.goEnd)
						FontNormal.printFont(offsetX+36, offsetY+196, "GO!", COLOR.WHITE, 2f)
				} else if(engine.statc[0]>=engine.readyStart&&engine.statc[0]<engine.readyEnd)
					FontNormal.printFont(offsetX+20, offsetY+80, "READY", COLOR.WHITE)
				else if(engine.statc[0]>=engine.goStart&&engine.statc[0]<engine.goEnd)
					FontNormal.printFont(offsetX+32, offsetY+30, "GO!", COLOR.WHITE)
		}
	}

	/* Blockピース移動時の処理 */
	override fun renderMove(engine:GameEngine, playerID:Int) {
		if(!engine.isVisible) return

		val offsetX = fieldX(engine, playerID)
		val offsetY = fieldY(engine, playerID)

		if(engine.statc[0]>1||engine.ruleopt.moveFirstFrame)
			when(engine.displaysize) {
				1 -> {
					if(nextshadow) drawShadowNexts(offsetX+4, offsetY+52, engine, 2f)
					if(engine.ghost&&engine.ruleopt.ghost) drawGhostPiece(offsetX+4, offsetY+52, engine, 2f)
					if(engine.ai!=null&&engine.aiShowHint&&engine.aiHintReady) drawHintPiece(offsetX+4, offsetY+52, engine, 2f)
					drawCurrentPiece(offsetX+4, offsetY+52, engine, 2f)
				}
				0 -> {
					if(nextshadow) drawShadowNexts(offsetX+4, offsetY+52, engine, 1f)
					if(engine.ghost&&engine.ruleopt.ghost) drawGhostPiece(offsetX+4, offsetY+52, engine, 1f)
					if(engine.ai!=null&&engine.aiShowHint&&engine.aiHintReady) drawHintPiece(offsetX+4, offsetY+52, engine, 1f)
					drawCurrentPiece(offsetX+4, offsetY+52, engine, 1f)
				}
				else -> {
					if(engine.ghost&&engine.ruleopt.ghost) drawGhostPiece(offsetX+4, offsetY+4, engine, .5f)
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
		val s = engine.displaysize*16+if(engine.displaysize==-1) 8 else 16
		effectlist.add(EffectObject(GFX.LINE, fieldX(engine, playerID)+4, fieldY(engine, playerID)+52+y*s,
			s, engine.fieldWidth))
	}

	/* Blockを消す演出を出すときの処理 */
	override fun blockBreak(engine:GameEngine, playerID:Int, x:Int, y:Int, blk:Block) {
		if(showlineeffect&&engine.displaysize!=-1) {
			val color = blk.drawColor
			val x = fieldX(engine, playerID)+4+x*16
			val y = fieldY(engine, playerID)+52+y*16
			// 通常Block
			if(color>=Block.BLOCK_COLOR_GRAY&&color<=Block.BLOCK_COLOR_PURPLE
				&&!blk.getAttribute(Block.ATTRIBUTE.BONE))
				effectlist.add(EffectObject(if(blk.getAttribute(Block.ATTRIBUTE.LAST_COMMIT)) GFX.SPARK else GFX.BLOCK,
					x, y, (color-Block.BLOCK_COLOR_GRAY)%ResourceHolder.imgBreak.size))
			else if(blk.isGemBlock)
				effectlist.add(EffectObject(GFX.GEM, x, y, (color-Block.BLOCK_COLOR_GEM_RED)%ResourceHolder.imgPErase.size))// 宝石Block

		}
	}

	/* Blockを消す演出を出すときの処理 */
	override fun shootFireworks(engine:GameEngine, x:Int, y:Int, color:COLOR) {
		val cint:Int = color.ordinal
		val obj = EffectObject(GFX.HANABI, x, y, cint)
		effectlist.add(obj)
		super.shootFireworks(engine, x, y, color)
	}

	override fun bravo(engine:GameEngine, playerID:Int) {
		effectlist.add(EffectObject(GFX.BRAVO, fieldX(engine, playerID), fieldY(engine, playerID), 0f))
		super.bravo(engine, playerID)
	}

	/* EXCELLENT画面の描画処理 */
	override fun renderExcellent(engine:GameEngine, playerID:Int) {
		graphics ?: return
		if(!engine.allowTextRenderByReceiver) return
		if(!engine.isVisible) return

		val offsetX = fieldX(engine, playerID)
		val offsetY = fieldY(engine, playerID)

		if(engine.displaysize!=-1) {
			if(engine.owner.players<=1)
				FontNormal.printFont(offsetX+4, offsetY+204, "EXCELLENT!", COLOR.ORANGE, 1f)
			else FontNormal.printFont(offsetX+36, offsetY+204, "YOU WIN!", COLOR.ORANGE, 1f)
		} else if(engine.owner.players<=1)
			FontNormal.printFont(offsetX+4, offsetY+80, "EXCELLENT!", COLOR.ORANGE, .5f)
		else FontNormal.printFont(offsetX+20, offsetY+80, "YOU WIN!", COLOR.ORANGE, .5f)
	}

	/* game over画面の描画処理 */
	override fun renderGameOver(engine:GameEngine, playerID:Int) {
		graphics ?: return
		if(!engine.allowTextRenderByReceiver||!engine.isVisible) return
		val offsetX = fieldX(engine, playerID)
		val offsetY = fieldY(engine, playerID)
		if(engine.lives>0&&engine.gameActive) {
			FontNormal.printFont(offsetX+4, offsetY+204, "LEFT", COLOR.WHITE, 1f)
			FontNumber.printFont(offsetX+132, offsetY+196, ((engine.lives-1)%10).toString(), COLOR.WHITE, 2f)
		} else if(engine.statc[0]>=engine.statc[1])
			when {
				engine.displaysize!=-1 ->
					when {
						engine.owner.players<2 -> if(engine.ending==0)
							FontNormal.printFont(offsetX+12, offsetY+204, "GAME OVER", COLOR.WHITE, 1f)
						else FontNormal.printFont(offsetX+28, offsetY+204, "THE END", COLOR.WHITE, 1f)
						engine.owner.winner==-2 -> FontNormal.printFont(offsetX+52, offsetY+204, "DRAW", COLOR.GREEN, 1f)
						engine.owner.players<3 -> FontNormal.printFont(offsetX+52, offsetY+204, "LOSE", COLOR.WHITE, 1f)
					}
				engine.owner.players<2 -> if(engine.ending==0)
					FontNormal.printFont(offsetX+4, offsetY+204, "GAME OVER", COLOR.WHITE, 1f)
				else FontNormal.printFont(offsetX+20, offsetY+204, "THE END", COLOR.WHITE, 1f)
				engine.owner.winner==-2 -> FontNormal.printFont(offsetX+28, offsetY+80, "DRAW", COLOR.GREEN, .5f)
				engine.owner.players<3 -> FontNormal.printFont(offsetX+28, offsetY+80, "LOSE", COLOR.WHITE, .5f)
			}
	}

	/* Render results screen処理 */
	override fun renderResult(engine:GameEngine, playerID:Int) {
		graphics ?: return
		if(!engine.allowTextRenderByReceiver) return
		if(!engine.isVisible) return

		var tempColor:COLOR = if(engine.statc[0]==0) COLOR.RED else COLOR.WHITE

		FontNormal.printFont(fieldX(engine, playerID)+12,
			fieldY(engine, playerID)+340, "RETRY", tempColor, 1f)

		tempColor = if(engine.statc[0]==1) COLOR.RED else COLOR.WHITE
		FontNormal.printFont(fieldX(engine, playerID)+108,
			fieldY(engine, playerID)+340, "END", tempColor, 1f)
	}

	/* fieldエディット画面の描画処理 */
	override fun renderFieldEdit(engine:GameEngine, playerID:Int) {
		graphics ?: return
		val x = fieldX(engine, playerID)+4+engine.fldeditX*16
		val y = fieldY(engine, playerID)+52+engine.fldeditY*16
		val bright = if(engine.fldeditFrames%60>=30) -.5f else -.2f
		drawBlock(x, y, engine.fldeditColor, engine.skin, false, bright, 1f, 1f)
	}

	/* 各 frame の最後に行われる処理 */
	override fun onLast(engine:GameEngine, playerID:Int) {
		if(playerID==engine.owner.players-1) effectUpdate()
	}

	/* 各 frame の最後に行われる描画処理 */
	override fun renderLast(engine:GameEngine, playerID:Int) {
		if(playerID==engine.owner.players-1) effectRender()
	}

	/** Update effects */
	private fun effectUpdate() {
		effectlist.forEach {

			it.anim++
			when(it.effect) {
				GFX.LINE//Lines
				-> {
					it.anim += lineeffectspeed/2
					if(it.anim>=16) it.effect = null
				}
				GFX.BLOCK //Normal Block
					, GFX.SPARK // Controlled Block
					, GFX.GEM // Gem Block
				-> {
					it.anim += lineeffectspeed
					if(it.anim>=(if(it.effect==GFX.GEM) 60 else 36)) it.effect = null
				}
				GFX.HANABI // Fireworks
				-> if(it.anim>=48) it.effect = null
				GFX.BRAVO//Bravo
				-> if(it.anim>=100) it.effect = null
				GFX.SPLASH
				-> if(it.x<-BS) it.effect = null
			}
		}
		effectlist.removeIf {it.effect==null}
	}

	/** Render effects */
	private fun effectRender() {
		val graphics = graphics ?: return
		effectlist.forEachIndexed {i, it ->
			var x = it.x
			var y = it.y
			var srcx:Int
			var srcy:Int
			val color:Int = if(it.param.isNotEmpty()) it.param[0] else 0
			var sq:Int
			when(it.effect) {
				GFX.GEM // Gems frag
				-> {
					x -= 16
					y -= 16
					sq = 64
					srcx = (it.anim-1)%10*sq
					srcy = (it.anim-1)/10*sq
					graphics.drawImage(ResourceHolder.imgPErase[color], x, y, x+sq, y+sq, srcx, srcy, srcx+sq, srcy+sq)
				}
				GFX.SPARK // TI Block frag
				-> {
					x -= 88
					y -= 38
					sq = 192
					srcx = (it.anim-1)%6*sq
					srcy = (it.anim-1)/6*sq
					val flip = (i%3==0)!=(x%3==0)
					ResourceHolder.imgBreak[color][0].draw(
						if(flip) x else x+sq, y, if(flip) x+sq else x, y+sq,
						srcx, srcy, srcx+sq, srcy+sq)
				}

				GFX.BLOCK // TAP Block frag
				-> {
					x -= 88
					y -= 86
					sq = 192
					srcx = (it.anim-1)%8*sq
					srcy = (it.anim-1)/8*sq
					val flip = (i%3==0)!=(x%3==0)
					ResourceHolder.imgBreak[color][1].draw(if(flip) x else x+sq, y, if(flip) x+sq else x, y+sq,
						srcx, srcy, srcx+sq, srcy+sq)
				}
				GFX.HANABI //Fireworks
				-> {
					sq = 192
					x -= sq/2
					y -= sq/2
					srcx = (it.anim-1)%6*sq
					srcy = (it.anim-1)/8*sq
					ResourceHolder.imgHanabi[color].draw(x, y, x+sq, y+sq, srcx, srcy, srcx+sq, srcy+sq)
				}
				GFX.LINE //Line Cleaned
				-> {
					sq = it.param[1]*color
					srcy = (it.anim/2-1)*color
					if(it.anim%2==1) graphics.setDrawMode(Graphics.MODE_ADD)
					ResourceHolder.imgLine[0].draw(x, y, x+sq, y+color, 0, srcy, 80, srcy+8)
					graphics.setDrawMode(Graphics.MODE_NORMAL)
				}
				GFX.BRAVO //Field Cleaned
				-> {
					FontNormal.printFont(it.x+20, it.x+204, "BRAVO!", COLOR.values()[(it.anim+5)%10], 1.5f)
					FontNormal.printFont(it.x+52, it.x+236, "CLEANED", COLOR.values()[it.anim%10], 1f)
				}
				GFX.SPLASH -> {

				}
			}
		}
	}

	companion object {
		/** block size */
		const val BS = 16

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