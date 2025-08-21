/*
 Copyright (c) 2010-2024, NullNoname
 All rights reserved.

 Converted to Kotlin and modified by Venom_Nhelv as bluish-elvis
 THIS IS NOT MADE IN ASSOCIATION WITH THE GAME CREATOR.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions are met:
     * Redistributions of source code must retain the above copyright
       notice, this list of conditions and the following disclaimer.
     * Redistributions in binary form must reproduce the above copyright
       notice, this list of conditions and the following disclaimer in the
       documentation and/or other materials provided with the distribution.
     * Neither the name of NullNoname nor the names of its
       contributors may be used to endorse or promote products derived from
       this software without specific prior written permission.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 POSSIBILITY OF SUCH DAMAGE.
 */
package mu.nu.nullpo.gui.slick

import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.game.play.GameEngine.Companion.FRAME_SKIN_GRADE
import mu.nu.nullpo.game.play.GameManager
import mu.nu.nullpo.gui.common.AbstractRenderer
import mu.nu.nullpo.gui.common.BaseFont.FONT
import mu.nu.nullpo.gui.common.ResourceImage
import mu.nu.nullpo.gui.slick.img.*
import mu.nu.nullpo.gui.slick.img.bg.AbstractBG
import mu.nu.nullpo.gui.slick.img.bg.SpinBG
import mu.nu.nullpo.util.CustomProperties
import org.apache.logging.log4j.LogManager
import org.newdawn.slick.Color
import org.newdawn.slick.Graphics
import org.newdawn.slick.geom.Polygon
import zeroxfc.nullpo.custom.libs.Vector
import kotlin.math.PI
import kotlin.math.absoluteValue

/** ゲームの event 処理と描画処理 (Slick版） */
class RendererSlick(
	/** 描画先サーフェイス */
	internal var graphics:Graphics? = null
):AbstractRenderer() {

	override val resources = ResourceHolder

	/* TTF使用可能 */
	override val isTTFSupport get() = resources.ttfFont!=null

	override val skinMax get() = resources.imgBigBlockList.size

	override val rainbowCount get() = NullpoMinoSlick.rainbow
	/** Constructor */
	init {
		conf = NullpoMinoSlick.propConfig.visual
	}

	override fun drawBlendAdd(unit:()->Unit) {
		val g = graphics?:return super.drawBlendAdd(unit)
		g.setDrawMode(Graphics.MODE_ADD)
		unit()
		g.setDrawMode(Graphics.MODE_NORMAL)
	}

	override fun printFontSpecific(x:Float, y:Float, str:String, font:FONT, color:COLOR, scale:Float, alpha:Float) {
		when(font) {
			FONT.NANO -> FontNano.printFont(x, y, str, color, scale, alpha)
			FONT.NUM -> FontNumber.printFont(x, y, str, color, scale, alpha)
			FONT.NUM_T -> FontNumTall.printFont(x, y, str, color, scale, alpha)
			FONT.TTF -> printTTFSpecific(x, y, str, color, scale, alpha)
			FONT.GRADE -> FontGrade.printFont(x, y, str, color, scale, alpha)
			else -> FontNormal.printFont(x, y, str, color, scale, alpha)
		}
	}

	override fun printTTFSpecific(x:Float, y:Float, str:String, color:COLOR, scale:Float, alpha:Float) =
		FontTTF.print(x, y, str, color, alpha, scale)

	override fun drawStaffRoll(x:Number, y:Number, scr:Number, height:Number, alpha:Float) {
		RenderStaffRoll.draw(x.toFloat(), y.toFloat(), scr.toFloat(), height.toFloat(), alpha)
	}

	override val doesGraphicsExist get() = graphics!=null

	/* 勲章を描画 */
	override fun drawMedal(x:Float, y:Float, str:String, tier:Int, scale:Float) {
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
		super.saveReplay(owner, prop, NullpoMinoSlick.propGlobal.custom.replayDir)
	}

	override fun drawBlockSpecific(x:Float, y:Float, sx:Int, sy:Int, sk:Int, size:Float, darkness:Float, alpha:Float) {
		val g = graphics?:return
		val img = when {
			size*2<=BS -> resources.imgSmallBlockList[sk]
			size>=BS*2 -> resources.imgBigBlockList[sk]
			else -> resources.imgNormalBlockList[sk]
		}.res
		val si = when {
			size*2<=BS -> BS/2
			size>=BS*2 -> BS*2
			else -> BS
		}.toFloat()
		val isSticky = resources.getBlockIsSticky(sk)
		val bone = (if(isSticky) sy else sx) in 9..17
		val filter = (1f-darkness).coerceIn(0f, 1f).let {Color(it, it, it, alpha)}

		val imageWidth = img.width
		if(sx*si>=imageWidth&&imageWidth!=-1) return
		val imageHeight = img.height
		if(sy*si>=imageHeight&&imageHeight!=-1) return
		g.drawImage(img, x, y, (x+size), (y+size), sx*si, sy*si, (sx+1)*si, (sy+1)*si, filter)

		if(darkness<0) {
			g.color = Color(1f, 1f, 1f, -darkness*alpha)
			g.fillRect(x, y, size, size)

			if(heavyEffect>0&&!bone) {
				g.setDrawMode(Graphics.MODE_COLOR_MULTIPLY)
				val shf = Color(1f, 1f, 1f, (-darkness*alpha).coerceIn(0f, 1f))
				val shx = if(isSticky) sx else 1
				val shy = if(isSticky) 1 else 0
				g.drawImage(img, x, y, (x+size), (y+size), shx*si, shy*si, (shx+1)*si, (shy+1)*si, shf)
				g.setDrawMode(Graphics.MODE_NORMAL)

			}
		} else if(heavyEffect>0&&darkness>0&&!bone) {
			g.setDrawMode(Graphics.MODE_ADD)
			val shf = (darkness*alpha/2).coerceIn(0f, 1f).let {Color(it, it, it, it)}
			val shx = if(isSticky) sx else 0
			val shy = 0
			g.drawImage(img, x, y, (x+size), (y+size), shx*si, shy*si, (shx+1)*si, (shy+1)*si, shf)
			g.setDrawMode(Graphics.MODE_NORMAL)
		}
	}

	override fun drawLineSpecific(x:Float, y:Float, sx:Float, sy:Float, color:Int, alpha:Float, w:Float) {
		val g = graphics?:return
		val lw = g.lineWidth
		g.lineWidth = w
		g.color = Color(color).apply {a = alpha}
		g.drawLine(x, y, sx, sy)
		g.lineWidth = lw
	}

	override fun drawRectSpecific(x:Float, y:Float, w:Float, h:Float, color:Int, alpha:Float, bold:Float) {
		if(w>0f) {
			val g = graphics?:return
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
		val g = graphics?:return
		val c = g.color
		g.color = Color(color).apply {a = alpha}
		g.fillRect(x, y, w, h)
		g.color = c
	}

	override fun drawDiaSpecific(x:Float, y:Float, w:Float, h:Float, angle:Float, color:Int, alpha:Float, bold:Float) {
		if(w>0f) {
			val g = graphics?:return
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
		val g = graphics?:return
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
			val g = graphics?:return
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
		val g = graphics?:return
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

	override fun drawFieldSpecific(x:Float, y:Float, width:Int, viewHeight:Int, blksize:Int, scale:Float, outlineType:Int) {
		val g = graphics?:return
	}

	/** Field frameを描画
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param engine GameEngineのインスタンス
	 */
	override fun drawFrameSpecific(x:Float, y:Float, engine:GameEngine) {
		val g = graphics?:return
		val size = engine.blockSize
		val width = engine.field.width//?: Field.DEFAULT_WIDTH
		val height = engine.field.height//?: Field.DEFAULT_HEIGHT

		if(engine.frameSkin>=0) {
			val fi = resources.imgFrame[engine.frameSkin].res
			val rX = x+width*size
			val bY = y+height*size

			g.color = Color.white
			//top edge
//			g.drawImage(fi, fi.height.toFloat()+1, 0f)
			fi.getSubImage(16, 0, 16, 32).also {
				it.setCenterOfRotation(0f, 0f)
				it.rotation = -90f
				it.draw(x-size, y, size.toFloat(), rX-x+size*2f)
			}
			//bottom edge
			fi.getSubImage(48, 96, 16, 32).also {
				it.setCenterOfRotation(0f, 0f)
				it.rotation = -90f
				it.draw(x-size, bY+size, size.toFloat(), rX-x+size*2f)
			}
			//left edge
			g.texture(
				Polygon(floatArrayOf(x-size, y-size, x, y, x, bY, x-size, bY+size)),
				fi.getSubImage(16, 0, 16, 32), 1f, 1f, true
			)
			//right edge
			g.texture(
				Polygon(floatArrayOf(rX+size, y-size, rX, y, rX, bY, rX+size, bY+size)),
				fi.getSubImage(16, 96, 16, 32), 1f, 1f, true
			)
		} else if(engine.frameSkin==FRAME_SKIN_GRADE) {
			val fi = resources.imgFrameOld[3]
		}
	}

	override val bgType by lazy {
		super.bgType.map {
			if(it is mu.nu.nullpo.gui.common.bg.SpinBG)
				SpinBG(ResourceImageSlick(it.img, true), it.addBGFX)
			else it::class.java.getDeclaredConstructor(ResourceImage::class.java)
				.newInstance(ResourceImageSlick(it.img)) as AbstractBG
		}
	}

	/*@Suppress("UNCHECKED_CAST")
	override val bgaType:List<AbstractBG<Image>> by lazy {
		super.bgaType.map{
			log.debug(it::class.java.name)
			log.debug(it::class.java.getDeclaredConstructor(ResourceImage::class.java))
			it::class.java.getDeclaredConstructor(ResourceImage::class.java).newInstance(ResourceImageSlick(it.img)) as
				AbstractBG<Image>
		}
	}*/

	override fun onFirst(engine:GameEngine) {
		super.onFirst(engine)
		if(!engine.owner.menuOnly&&showBG&&animBG) {
			val bgMax = resources.bgMax
			val bgaMax = resources.bgaMax
			val bg = engine.owner.bgMan.bg.let {if(it>=bgMax) it%bgMax else it}

			if(bg in 0..<bgMax)
				bgType[bg].update()
			else if(bg<0&&bg.absoluteValue in 1..bgaMax+1)
				bgaType[bg.absoluteValue-1].update()
		}
	}

	override fun drawBG(engine:GameEngine) {
		val graphics = graphics?:return
		if(engine.owner.menuOnly) {
			graphics.color = Color.white
			graphics.drawImage(resources.imgMenuBG[1], 0f, 0f)
		} else if(showBG) {
			val bgMax = resources.bgMax
			val bgaMax = resources.bgaMax
			val bg = engine.owner.bgMan.bg.let {if(it>=bgMax) it%bgMax else it}


			graphics.color = Color.white
			if(animBG) {
				if(bg in 0..<bgMax)
					bgType[bg].draw(this)
				else if(bg<0&&bg.absoluteValue in 1..bgaMax+1)
					bgaType[bg.absoluteValue-1].draw(this)

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
				if(bg in 0..<bgMax)
					bgType[bg].drawLite()
				else if(bg<0&&bg.absoluteValue in 1..bgaMax+1)
					bgaType[bg.absoluteValue-1].drawLite()
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
		/** Log */
		internal val log = LogManager.getLogger()
	}
}
