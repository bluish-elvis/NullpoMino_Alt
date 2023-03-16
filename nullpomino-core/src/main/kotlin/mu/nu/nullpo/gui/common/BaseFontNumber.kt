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

import mu.nu.nullpo.game.event.EventReceiver

abstract class BaseFontNumber:BaseFont {
	companion object {
		const val w:Int = 12
		const val h:Int = 16
	}

	abstract override val rainbowCount:Int
	override fun processTxt(x:Float, y:Float, str:String, color:EventReceiver.COLOR, scale:Float, alpha:Float, rainbow:Int,
		draw:(i:Int, dx:Float, dy:Float, scale:Float, sx:Int, sy:Int, sw:Int, sh:Int, a:Float)->Unit) {
		var dx = x
		var dy = y
		val fontBig = scale>=1.5f
		str.forEachIndexed {i, c ->
			// 文字出力
			val stringChar = c.code.let {
				when(it) {
					0x0A -> {
						// 改行 (\n）
						dy += h*scale
						dx = x
						0
					}
					0x20 -> 0x30//dx += 12*scale
					0x3f -> 0x3b
					0x2d -> 0x3c
					0x2b -> 0x3d
					0x2f -> 0x3e
					0x2e -> 0x3f
					0x25 -> 0x40
					in 0x30..0x40 -> it
					else -> 0
				}
			}

			if(stringChar in 0x30..0x40) { // 文字出力
				val sx = if(c.code==0x20) 0 else (stringChar-48)%16
				val sy = (if(color==EventReceiver.COLOR.RAINBOW) EventReceiver.getRainbowColor(rainbow, i) else color).ordinal
				val a = if(c.code==0x20) alpha*.4f else alpha
				if(fontBig) draw(1, dx, dy, scale/2, sx*24, sy*32, 24, 32, a)
				else draw(0, dx, dy-1, scale, sx*12, sy*16, 12, 16, a)

				dx += w*scale
			}
		}
	}

	override fun printFont(x:Int, y:Int, str:String, color:EventReceiver.COLOR, scale:Float, alpha:Float, rainbow:Int) =
		processTxt(x.toFloat(), y.toFloat(), str, color, scale, alpha, rainbow)
		{i:Int, dx:Float, dy:Float, s:Float, sx:Int, sy:Int, w:Int, h:Int, a:Float ->
			getImg(i).draw(dx, dy, dx+w*s, dy+h*s, sx, sy, sx+w, sy+h, alpha = a)
		}

}
