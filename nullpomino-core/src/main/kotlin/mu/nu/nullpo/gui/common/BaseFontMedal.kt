/*
 * Copyright (c) 2021-2023, NullNoname
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

import mu.nu.nullpo.game.event.EventReceiver.COLOR
import mu.nu.nullpo.gui.slick.img.FontMedal

abstract class BaseFontMedal:BaseFont {
	companion object {
		const val w = 9
		const val h = 24
		/** paddingLeft */
		const val pl = 5
		/** paddingTop */
		const val pt = 4
		/** marginedBottom */
		const val mb = h-pt
	}

	abstract val img:ResourceImage<*>
	final override fun getImg(i:Int):ResourceImage<*> = img

	protected fun processTxt(x:Float, y:Float, str:String, tier:Int, scale:Float,
		draw:(x:Float, y:Float, dx:Float, dy:Float, sx:Int, sy:Int, sw:Int, sh:Int)->Unit) {
		val sy = maxOf(0, 4-tier)*h
		val ww = str.length*9
		val bx = x-(ww+10)/2f*(1-scale)
		val by = y-h/2f*(1-scale)

		draw(bx-pl*scale, by-pt*scale, bx, by+mb*scale, 0, sy, 4, sy+h)
		draw(bx-scale, by-pt*scale, bx+(1+ww)*scale, by+mb*scale, 4, sy, 6, sy+h)
		draw(bx+(1+ww)*scale, by-pt*scale, bx+(5+ww)*scale, by+mb*scale, 6, sy, 10, sy+h)
		str.forEachIndexed {i, c ->
			val stringChar = c.code-0x41
			if(stringChar in 0x00..0x1B) {// Character output
				val dx = bx+i*9f
				val sx = stringChar*9+10
				draw(dx, by*scale, dx+9*scale, by+16*scale, sx, sy+4, sx+9, sy+20)
			}
		}
	}

	/** 文字列を描画
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param str 文字列
	 * @param tier 文字色
	 * @param scale 拡大率
	 */
	fun printFont(x:Float, y:Float, str:String, tier:Int, scale:Float = 1f, alpha:Float = if(tier==0) 0.5f else 1f,
		darkness:Float = 0f) =
		processTxt(x, y, str, tier, scale)
		{dx:Float, dy:Float, w:Float, h:Float, sx:Int, sy:Int, sw:Int, sh:Int ->
			FontMedal.img.draw(
				dx, dy, w, h, sx, sy, sw, sh,
				alpha, (minOf(1f, maxOf(0f, 1f-darkness))).let {brit -> Triple(brit, brit, brit)}
			)
		}

	fun printFont(x:Int, y:Int, str:String, tier:Int, scale:Float = 1f, alpha:Float = if(tier==0) 0.5f else 1f, darkness:Float = 0f) =
		printFont(x.toFloat(), y.toFloat(), str, tier, scale, alpha, darkness)
	/** 文字列を描画
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param str 文字列
	 * @param tier 文字色
	 * @param scale 拡大率
	 */
	fun printFontGrid(x:Int, y:Int, str:String, tier:Int = 0, scale:Float = 1f, alpha:Float = if(tier==0) 0.5f else 1f,
		darkness:Float = 0f) = FontMedal.printFont(x*16, y*16, str, tier, scale, alpha, darkness)

	private fun col(color:COLOR) = when(color) {
		COLOR.RED -> 2
		else -> 1
	}

	override fun processTxt(x:Float, y:Float, str:String, color:COLOR, scale:Float, alpha:Float, rainbow:Int,
		draw:(i:Int, dx:Float, dy:Float, scale:Float, sx:Int, sy:Int, sw:Int, sh:Int, a:Float)->Unit) =
		printFont(x, y, str, col(color), scale, alpha)
	//x:Float, y:Float, str:String, tier:Int, scale:Float,
	/** Draws the string
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param str String
	 * @param color Letter cint
	 * @param scale Enlargement factor
	 */
	override fun printFont(x:Float, y:Float, str:String, color:COLOR, scale:Float, alpha:Float, rainbow:Int) =
		printFont(x, y, str, col(color), scale, alpha)
}
