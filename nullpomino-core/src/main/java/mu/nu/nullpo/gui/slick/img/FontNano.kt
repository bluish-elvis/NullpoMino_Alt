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
package mu.nu.nullpo.gui.slick.img

import mu.nu.nullpo.game.event.EventReceiver
import mu.nu.nullpo.game.event.EventReceiver.COLOR
import mu.nu.nullpo.gui.slick.NullpoMinoSlick
import mu.nu.nullpo.gui.slick.ResourceHolder
import org.newdawn.slick.Color

/** 普通の文字列の表示クラス */
object FontNano {

	/** 文字列を描画
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param str 文字列
	 * @param color 文字色
	 * @param scale 拡大率
	 */
	fun printFont(x:Int, y:Int, str:String, color:COLOR = COLOR.WHITE, scale:Float = 1f, alpha:Float =1f ,
		rainbow:Int = NullpoMinoSlick.rainbow) {
		var dx = x.toFloat()
		var dy = y.toFloat()
		val filter = Color(Color.white).apply {
			a = alpha
		}

		str.forEachIndexed { i, char ->
			val stringChar = char.toInt()

			if(stringChar==0x0A) {
				// 改行 (\n）
				dy = (dy+16*scale)
				dx = x.toFloat()
			} else {// 文字出力
				val col = (if(color==COLOR.RAINBOW) EventReceiver.getRainbowColor(rainbow+i) else color).ordinal
				val c = stringChar-32// Character output
				var sx = c%32
				var sy = c/32+col*3
				val w = 12f*scale
				val h = 14f*scale
				sx *= 12
				sy *= 14
				ResourceHolder.imgFontNano.draw(dx, dy, dx+w, dy+h,
					sx.toFloat(), sy.toFloat(), sx+12f, sy+14f, filter)

				dx += w
			}
		}
	}

	/** Draws the string (16x16Grid units)
	 * @param fontX X-coordinate
	 * @param fontY Y-coordinate
	 * @param fontStr String
	 * @param fontColor Letter cint
	 */
	fun printFontGrid(fontX:Int, fontY:Int, fontStr:String, fontColor:COLOR = COLOR.WHITE, scale:Float = 1f,
		alpha:Float = 1f) =
		printFont(fontX*16, fontY*16, fontStr, fontColor, scale, alpha)

	/** flagThefalseIf it&#39;s the casefontColorTrue cint, trueIf it&#39;s the
	 * casefontColorTrue colorDraws the string in (16x16Grid units)
	 * @param fontX X-coordinate
	 * @param fontY Y-coordinate
	 * @param fontStr String
	 * @param flag Conditional expression
	 * @param fontColorFalse flagThefalseText cint in the case of
	 * @param fontColorTrue flagThetrueText cint in the case of
	 */
	fun printFontGrid(fontX:Int, fontY:Int, fontStr:String, flag:Boolean,
		fontColorFalse:COLOR = COLOR.WHITE, fontColorTrue:COLOR = COLOR.RED, alpha:Float = 1f) =
		printFont(fontX*16, fontY*16, fontStr, color = if(flag) fontColorTrue else fontColorFalse, alpha = alpha)
}

