/*
 * Copyright (c) 2021-2024, NullNoname
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
import mu.nu.nullpo.game.event.EventReceiver.COLOR

abstract class BaseFontNormal:BaseFont {
	companion object {
		const val W:Int = 16
		const val H:Int = 16
	}

	override fun processTxt(x:Float, y:Float, str:String, color:COLOR, scale:Float, alpha:Float, rainbow:Int,
		draw:(i:Int, dx:Float, dy:Float, scale:Float, sx:Int, sy:Int, sw:Int, sh:Int, a:Float)->Unit) {
		var dx = x
		var dy = y
		str.forEachIndexed {i, char ->

			if(char.code==0x0A) {
				// New line (\n)
				dy += H*scale
				dx = x
			} else {
				val c = if(char.code==0x20) 96 else char.code-32// Character output
				val a = if(char.code==0x20) alpha/3f else alpha
				val fontColor = (if(color==COLOR.RAINBOW) EventReceiver.getRainbowColor(rainbow, i) else color).ordinal
				val wy = dy+if(char.isLowerCase()) 3f*scale else 0f

				when {
					scale<=.5f -> draw(0, dx, wy, scale*2, c%32*8, (c/32+fontColor*4)*8, 8, 8, a)
					scale>=(5f/3f) -> draw(2, dx, wy, scale/2, c%32*32, (c/32+fontColor*4)*32, 32, 32, a)
					else -> draw(1, dx, wy, scale, c%32*W, (c/32+fontColor*4)*16, 16, 16, a)
				}
				dx += W*scale
			}
		}
	}

	override fun printFont(x:Float, y:Float, str:String, color:COLOR, scale:Float, alpha:Float, rainbow:Int) =
		processTxt(x, y, str, color, scale, alpha, rainbow)
		{i:Int, dx:Float, dy:Float, s:Float, sx:Int, sy:Int, sw:Int, sh:Int, a:Float ->
			getImg(i).draw(dx, dy, dx+sw*s, dy+sh*s, sx, sy, sx+sw, sy+sh, alpha = a)
		}

}
