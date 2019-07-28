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

import mu.nu.nullpo.game.event.EventReceiver.COLOR
import mu.nu.nullpo.gui.slick.ResourceHolder
import org.newdawn.slick.Color

/** Normal display class string */
object FontNormal {
	/** Specified font ColorSlickUseColorObtained as
	 * @param fontColor font Color
	 * @return font ColorColor
	 */
	private fun getFontColorAsColor(fontColor:COLOR):Color = when(fontColor) {
		COLOR.BLUE -> Color(0, 0, 255)
		COLOR.RED -> Color(255, 0, 0)
		COLOR.PINK -> Color(255, 128, 128)
		COLOR.GREEN -> Color(0, 255, 0)
		COLOR.YELLOW -> Color(255, 255, 0)
		COLOR.CYAN -> Color(0, 255, 255)
		COLOR.ORANGE -> Color(255, 128, 0)
		COLOR.PURPLE -> Color(255, 0, 255)
		COLOR.COBALT -> Color(0, 0, 128)
		else -> Color(255, 255, 255)
	}

	/** TTF font Drawing a string using the
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param str String
	 * @param color Letter cint
	 */
	fun printTTF(x:Int, y:Int, str:String, color:COLOR = COLOR.WHITE) {
		ResourceHolder.ttfFont?.run {drawString(x.toFloat(), y.toFloat(), str, getFontColorAsColor(color))}
	}

	/** Draws the string
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param str String
	 * @param color Letter cint
	 * @param scale Enlargement factor
	 */
	fun printFont(x:Int, y:Int, str:String, color:COLOR = COLOR.WHITE, scale:Float = 1f) {
		var dx = x
		var dy = y
		val fontColor = color.ordinal
		for(i in 0 until str.length) {
			val stringChar = str[i].toInt()

			if(stringChar==0x0A) {
				// New line (\n)
				if(scale==1f) {
					dy = (dy+16*scale).toInt()
					dx = x
				} else {
					dy += 8
					dx = x
				}
			} else {
				val c = stringChar-32// Character output
				if(scale==.5f) {
					val sx = c%32*8
					val sy = c/32*8+fontColor*24
					ResourceHolder.imgFontSmall.draw(dx.toFloat(), dy.toFloat(), (dx+8).toFloat(), (dy+8).toFloat(),
						sx.toFloat(), sy.toFloat(), (sx+8).toFloat(), (sy+8).toFloat())
					dx += 8
				} else {
					val sx = c%32*16
					val sy = c/32*16+fontColor*48
					//SDLRect rectSrc = new SDLRect(sx, sy, 16, 16);
					//SDLRect rectDst = new SDLRect(dx, dy, 16, 16);
					//ResourceHolderSDL.imgFont.blitSurface(rectSrc, dest, rectDst);
					ResourceHolder.imgFont.draw(dx.toFloat(), dy.toFloat(), dx+16*scale, dy+16*scale,
						sx.toFloat(), sy.toFloat(), (sx+16).toFloat(), (sy+16).toFloat())
					dx = (dx+16*scale).toInt()
				}
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
		fontColorTrue:COLOR = COLOR.RED, scale:Float = 1f) =
		printFont(x, y, str, if(flag) fontColorTrue else fontColorFalse, scale)

	/** Draws the string (16x16Grid units)
	 * @param fontX X-coordinate
	 * @param fontY Y-coordinate
	 * @param fontStr String
	 * @param fontColor Letter cint
	 */
	fun printFontGrid(fontX:Int, fontY:Int, fontStr:String, fontColor:COLOR = COLOR.WHITE, scale:Float = 1f) =
		printFont(fontX*16, fontY*16, fontStr, fontColor, scale)

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
		fontColorFalse:COLOR = COLOR.WHITE, fontColorTrue:COLOR = COLOR.RED) =
		printFont(fontX*16, fontY*16, fontStr, if(flag) fontColorTrue else fontColorFalse)
}