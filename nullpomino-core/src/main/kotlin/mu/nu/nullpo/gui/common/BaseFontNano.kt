/*
 Copyright (c) 2021-2024, NullNoname
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

package mu.nu.nullpo.gui.common

import mu.nu.nullpo.game.event.EventReceiver.COLOR

abstract class BaseFontNano:BaseFont {
	companion object {
		const val W = 12
		const val H = 14
		const val Wh = 6
		const val Hh = 7

		private fun ofs(c:Char, size:Boolean) = if(!size) when(c.code) {
			0x31 -> -1 to 0
			0x27, 0x2c, 0x2e, 0x49, 0x6c -> 0 to -1
			0x3a, 0x3b -> -1 to -1
			0x69 -> -1 to -2
			0x23, 0x24, 0x25, 0x2a, 0x2f, 0x5c, 0x4d, 0x6d, 0x57, 0x77, 0x5e, 0x7e, 0x7f -> 1 to 0
			else -> 0 to 0
		} else when(c.code) {
			0x23, 0x40, 0x4d, 0x6d, 0x7e, 0x7f -> 0 to 0
			0x78 -> 0 to -1
			0x24, 0x25, 0x2a, 0x2b, 0x2d, 0x3d, 0x59 -> -1 to -1
			0x5a, 0x7a -> -1 to -2
			0x26 -> -2 to -1
			0x49, 0x69 -> -2 to -3
			0x31 -> -4 to -3
			else -> -2 to -2
		}

		fun calcWidths(str:String, scale:Float) = str.lines().map {
			it.fold(0f) {acc, c ->
				acc+if(scale<=2f/3f) {
					val (sl, sr) = ofs(c, false)
					(Wh-1+sl+sr)*scale*2
				} else {
					val (sl, sr) = ofs(c, true)
					(W+sl+sr)*scale
				}
			}
		}
	}

	override fun getWidths(str:String, scale:Float):List<Float> = calcWidths(str, scale)

	abstract override val rainbowCount:Int
	override fun processTxt(x:Float, y:Float, str:String, colors:(Char, Int)->COLOR, scale:Float, alpha:Float, rainbow:Int,
		draw:(i:Int, li:Int, dx:Float, dy:Float, scale:Float, sx:Int, sy:Int, sw:Int, sh:Int, a:Float)->Unit):List<Float> = str.lines().mapIndexed {li, ls ->
		var dx = x-2*scale
		val dy = y+16*li*scale
		ls.forEachIndexed {i, char ->
			val stringChar = char.code
			val col = colors(char, i).let {(if(it==COLOR.RAINBOW) COLOR.getRainbowColor(rainbow, i) else it).ordinal}
			val c = stringChar-32// Character output
			val sx = c%32
			val sy = c/32+col*3

			dx += if(scale<=2f/3f) {
				val (sl, sr) = ofs(char, false)
				val hs = scale*2
				draw(0, li,dx+sl*hs, dy, hs, sx*6, sy*7, 6, 7, alpha)
				(Wh-1+sl+sr)*hs
			} else {
				val (sl, sr) = ofs(char, true)
				draw(1, li, dx+sl*scale, dy, scale, sx*12, sy*14, 12, 14, alpha)
				(W+sl+sr)*scale
			}
		}
		dx-x+2
	}

	/*override fun printFont(x:Float, y:Float, str:String, color:COLOR, scale:Float, alpha:Float, rainbow:Int) =
		processTxt(
			x, y, str, color, scale, alpha, rainbow
		) {i:Int, dx:Float, dy:Float, s:Float, sx:Int, sy:Int, w:Int, h:Int, a:Float ->
			getImg(i).draw(dx, dy, dx+w*s, dy+h*s, sx, sy, sx+w, sy+h, alpha = a)
		}*/

}
