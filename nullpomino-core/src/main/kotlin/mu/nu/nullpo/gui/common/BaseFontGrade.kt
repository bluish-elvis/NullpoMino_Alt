/*
 * Copyright (c) 2021-2022, NullNoname
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

abstract class BaseFontGrade:BaseFont {
	override fun processTxt(x:Float, y:Float, str:String, color:EventReceiver.COLOR, scale:Float, alpha:Float,
		rainbow:Int,
		draw:(i:Int, dx:Float, dy:Float, scale:Float, sx:Int, sy:Int, sw:Int, sh:Int, a:Float)->Unit) =
		if(scale>=5f/3f) {
			//processBigFont
			var dx = x
			var i = 0
			while(i<str.length) {
				val col = (if(color==EventReceiver.COLOR.RAINBOW) EventReceiver.getRainbowColor(rainbow+i) else color).ordinal
				var cd = str[i].code
				var nX = -1
				var nY = -1
				if(i<str.length-1) nX = str[i+1].code
				if(i<str.length-2) nY = str[i+2].code
				when(cd) {
					in 0x31..0x39 -> cd -= 0x31
					0x53, 0x73 -> // S
						if(nX in 0x31..0x39) {
							if(nX==0x31&&nY>=0x30&&nY<=0x33) {
								cd = 25+nY-0x30
								i++
							} else cd = 16+nX-0x31
							i++
						} else cd = 9
					0x6D, 0x4D ->//m|M
						if(nX in 0x31..0x39) {
							cd = 29+nX-0x31
							i++
						} else cd = if(cd==0x6D) 10 else 14
					0x4B, 0x6B -> cd = 11//K
					0x56, 0x76 -> cd = 12//V
					0x4F, 0x6F -> cd = 13//O
					0x47, 67 -> //G
						cd = if(nX==0x6D||nX==0x4D) {
							i++
							if(nX==0x6D) 38 else 39
						} else 15
					else -> cd = -1
				}
				dx += if(cd in 0..41) { // 文字出力

					val sz = if(cd<=15) 48 else if(cd>=40) 128 else 64
					val sx = (if(cd<16) cd else (cd-16)%12)*sz
					val sy = ((if(cd>=16) if(cd>=28) if(cd>=40) 3 else 2 else 1 else 0)+col*4)*48
					val sc = scale/2
					draw(1, dx-4*sc, y-4*sc, sc, sx, sy, sz, 48, alpha)
					(sz*sc).toInt()
				} else (24*scale).toInt()
				i++
			}
		} else {
//processMiniFont
			var dx = x
			var i = 0
			while(i<str.length) {
				val col = (if(color==EventReceiver.COLOR.RAINBOW) EventReceiver.getRainbowColor(rainbow+i) else color).ordinal
				var cd = str[i].code
				when(cd) {
					in 0x31..0x39 -> if(cd==0x31&&i<str.length-1) {
						val next = str[i+1].code
						if(next in 0x30..0x33) {
							cd = 9+next-0x30
							i++
						}
					} else cd -= 0x31
					0x53, 0x73 -> cd = 13//S
					0x6D -> cd = 14//m
					0x4B, 0x6B -> cd = 15//K
					0x56, 0x76 -> cd = 16//V
					0x4F, 0x6F -> cd = 17//O
					0x4D -> cd = 18//M
					0x47, 67 -> cd = 19//G
					else -> cd = -1
				}
				if(cd in 0..19) { // 文字出力
					val sx = cd%10*32
					val sy = (cd/10+col*2)*32
					val sc = scale/2
					draw(0, dx-2*sc, y-2*sc, sc, sx, sy, 32, 32, alpha)
					dx += (32*sc).toInt()
				}
				i++
			}
		}

	override fun printFont(x:Int, y:Int, str:String, color:EventReceiver.COLOR, scale:Float, alpha:Float, rainbow:Int) =
		processTxt(
			x.toFloat(), y.toFloat(), str, color, scale, alpha, rainbow,
		) {i:Int, dx:Float, dy:Float, s:Float, sx:Int, sy:Int, sw:Int, sh:Int, a:Float ->
			getImg(i).draw(dx, dy, dx+sw*s, dy+sh*s, sx, sy, sx+sw, sy+sh, a)
		}

}
