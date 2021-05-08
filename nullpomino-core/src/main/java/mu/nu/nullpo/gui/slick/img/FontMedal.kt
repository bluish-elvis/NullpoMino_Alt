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

import mu.nu.nullpo.gui.slick.ResourceHolder
import org.newdawn.slick.Color

/** 普通の文字列の表示クラス */
object FontMedal {

	/** 文字列を描画
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param str 文字列
	 * @param tier 文字色
	 * @param scale 拡大率
	 */
	fun printFont(x:Int, y:Int, str:String, tier:Int, scale:Float = 1f, alpha:Float = 1f, darkness:Float = 0f) {
		val filter = Color(Color.white).apply {
			a = alpha
		}.darker(maxOf(0f,darkness))
		val sy:Float = maxOf(0, 4-tier)*24f
		ResourceHolder.imgFontMedal.draw(x-5*scale, y-4*scale, x-scale, y+20*scale,
			0f, sy, 4f, sy+24f)
		ResourceHolder.imgFontMedal.draw(x-scale, y-4*scale, x+(1+9*str.length)*scale, y+20*scale,
			4f, sy, 6f, sy+24f)
		ResourceHolder.imgFontMedal.draw(x+(1+9*str.length)*scale, y-4*scale, x+(5+9*str.length)*scale, y+20*scale,
			6f, sy, 10f, sy+24f)
		for(i in str.indices) {
			val stringChar = str[i].code- 0x41
			if(stringChar in 0x00..0x1B) {// Character output
				val dx:Float = x+i*9f
				val sx:Float = stringChar*9f+10f
				ResourceHolder.imgFontMedal.draw(dx, y*scale, dx+9*scale, y+16*scale,
					sx, sy+4f, sx+9f, sy+20f, filter)
			}
		}
	}

	/** Draws the string (16x16Grid units)
	 * @param fontX X-coordinate
	 * @param fontY Y-coordinate
	 * @param str String
	 * @param tier Letter cint
	 */
	fun printFontGrid(fontX:Int, fontY:Int, str:String, tier:Int= 0, scale:Float = 1f, alpha:Float = 1f, darkness:Float = 0f) =
		printFont(fontX*16, fontY*16, str, tier, scale, alpha, darkness)
}
