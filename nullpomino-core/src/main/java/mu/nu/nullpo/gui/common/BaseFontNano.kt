/*
 * Copyright (c) 2021, NullNoname
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

package mu.nu.nullpo.gui.common

import mu.nu.nullpo.game.event.EventReceiver
import java.util.Locale

abstract class BaseFontNano:BaseFont {

	val w = 12
	val h = 14
	abstract override val rainbowCount:Int
	override fun processTxt(x:Float, y:Float, str:String, color:EventReceiver.COLOR, scale:Float, rainbow:Int,
		draw:(i:Int, dx:Float, dy:Float, scale:Float, sx:Int, sy:Int, sw:Int, sh:Int)->Unit) {
		var dx = x
		var dy = y

		str.uppercase(Locale.getDefault()).forEachIndexed {i, char ->
			val stringChar = char.code

			if(stringChar==0x0A) {
				// 改行 (\n）
				dy = (dy+16*scale)
				dx = x
			} else {// 文字出力
				val col = (if(color==EventReceiver.COLOR.RAINBOW) EventReceiver.getRainbowColor(rainbow+i) else color).ordinal
				val c = stringChar-32// Character output
				val sx = (c%32)*w
				val sy = (c/32+col*3)*h

				draw(0,dx,dy,scale,sx,sy,w,h)
				dx += w*scale
			}
		}
	}

	abstract override fun printFont(x:Int, y:Int, str:String, color:EventReceiver.COLOR, scale:Float, alpha:Float, rainbow:Int)
}
