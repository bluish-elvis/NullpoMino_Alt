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
package mu.nu.nullpo.gui.slick.img

import mu.nu.nullpo.game.event.EventReceiver
import mu.nu.nullpo.game.event.EventReceiver.COLOR
import mu.nu.nullpo.gui.slick.NullpoMinoSlick
import mu.nu.nullpo.gui.slick.ResourceHolder
import org.newdawn.slick.Color

/** 普通の文字列の表示クラス */
object FontNumber {
	/** 文字列を描画
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param str 文字列
	 * @param color 文字色
	 * @param scale 拡大率
	 */
	fun printFont(x:Int, y:Int, str:String, color:COLOR = COLOR.WHITE, scale:Float = 1f, alpha:Float = 1f,
		rainbow:Int = NullpoMinoSlick.rainbow) {
		var dx = x.toFloat()
		var dy = y.toFloat()
		val fontBig = scale>=1.5f
		val filter = Color(Color.white).apply {
			a = alpha
		}
		str.forEachIndexed {i, c ->
			var stringChar = c.code
			// 文字出力
			when(stringChar) {
				0x0A -> {
					// 改行 (\n）
					dy += 16*scale
					dx = x.toFloat()

				}
				0x20 -> dx += 12*scale
				0x3f -> stringChar = 0x3b
				0x2d -> stringChar = 0x3c
				0x2b -> stringChar = 0x3d
				0x2f -> stringChar = 0x3e
				0x2e -> stringChar = 0x3f
				0x25 -> stringChar = 0x40
			}

			if(stringChar in 0x30..0x40) { // 文字出力
				var sx = (stringChar-48)%32
				var sy = (if(color==COLOR.RAINBOW) EventReceiver.getRainbowColor(rainbow+i) else color).ordinal

				if(fontBig) {
					sx *= 24
					sy *= 32
					ResourceHolder.imgNum[1].draw(dx, dy, dx+12*scale, dy+16*scale, sx, sy, sx+24, sy+32, filter)
				} else {
					sx *= 12
					sy *= 16
					ResourceHolder.imgNum[0].draw(dx, dy-1, dx+12*scale, dy+16*scale, sx, sy, sx+12, sy+16, filter)
				}
				dx += 12*scale
			}
		}
	}

	/** flagThefalseIf it&#39;s the casefontColorTrue cint, trueIf it&#39;s the
	 * casefontColorTrue colorDraws the string in
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param str String
	 * @param flag Conditional expression
	 * @param fontColorFalse flagThefalseText cint in the case of
	 * @param fontColorTrue flagThetrueText cint in the case of
	 * @param scale Enlargement factor
	 */
	fun printFont(x:Int, y:Int, str:String, flag:Boolean, fontColorFalse:COLOR = COLOR.WHITE,
		fontColorTrue:COLOR = COLOR.RED, scale:Float = 1f, alpha:Float = 1f) =
		printFont(x, y, str, if(flag) fontColorTrue else fontColorFalse, scale, alpha)

	/** Draws the string (16x16Grid units)
	 * @param fontX X-coordinate
	 * @param fontY Y-coordinate
	 * @param fontStr String
	 * @param fontColor Letter cint
	 */
	fun printFontGrid(fontX:Int, fontY:Int, fontStr:String, fontColor:COLOR = COLOR.WHITE, scale:Float = 1f, alpha:Float = 1f) =
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
