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

abstract class BaseFontGrade:BaseFont {

	override fun processTxt(x:Float, y:Float, str:String, color:EventReceiver.COLOR, scale:Float, rainbow:Int,
		draw:(i:Int, dx:Float, dy:Float, scale:Float, sx:Int, sy:Int, sw:Int, sh:Int)->Unit) =
		if(scale>=5f/3f) {
			//processBigFont
			var dx = x
			var i = 0
			while(i<str.length) {
				val col = (if(color==EventReceiver.COLOR.RAINBOW) EventReceiver.getRainbowColor(rainbow+i) else color).ordinal
				var sC = str[i].code
				var nX = -1
				var nY = -1
				if(i<str.length-1) nX = str[i+1].code
				if(i<str.length-2) nY = str[i+2].code
				when(sC) {
					in 0x31..0x39 -> sC -= 0x31
					0x53, 0x73 -> // S
						if(nX in 0x31..0x39) {
							if(nX==0x31&&nY>=0x30&&nY<=0x33) {
								sC = 25+nY-0x30
								i++
							} else sC = 16+nX-0x31
							i++
						} else sC = 9
					0x6D, 0x4D ->//m|M
						if(nX in 0x31..0x39) {
							sC = 29+nX-0x31
							i++
						} else sC = if(sC==0x6D) 10 else 14
					0x4B, 0x6B -> sC = 11//K
					0x56, 0x76 -> sC = 12//V
					0x4F, 0x6F -> sC = 13//O
					0x47, 67 -> //G
						sC = if(nX==0x6D||nX==0x4D) {
							i++
							if(nX==0x6D) 38 else 39
						} else 15
					else -> sC = -1
				}
				dx += if(sC in 0..41) { // 文字出力

					val sz = if(sC<=15) 48 else if(sC>=40) 128 else 64
					val sx = (if(sC<16) sC else (sC-16)%12)*sz
					val sy = ((if(sC>=16) if(sC>=28) if(sC>=40) 3 else 2 else 1 else 0)+col*4)*48
					val scale = scale/2
					draw(1, dx-4*scale, y-4*scale, scale, sx, sy, sz, 48)
					(sz*scale).toInt()
				} else (24*scale).toInt()
				i++
			}
		} else {
//processMiniFont
			var dx = x
			var i = 0
			while(i<str.length) {
				val col = (if(color==EventReceiver.COLOR.RAINBOW) EventReceiver.getRainbowColor(rainbow+i) else color).ordinal
				var sC = str[i].code
				when(sC) {
					in 0x31..0x39 -> if(sC==0x31&&i<str.length-1) {
						val next = str[i+1].code
						if(next in 0x30..0x33) {
							sC = 9+next-0x30
							i++
						}
					} else sC -= 0x31
					0x53, 0x73 -> sC = 13//S
					0x6D -> sC = 14//m
					0x4B, 0x6B -> sC = 15//K
					0x56, 0x76 -> sC = 16//V
					0x4F, 0x6F -> sC = 17//O
					0x4D -> sC = 18//M
					0x47, 67 -> sC = 19//G
					else -> sC = -1
				}
				if(sC in 0..19) { // 文字出力
					val sx = sC%10*32
					val sy = (sC/10+col*2)*32
					val scale = scale/2
					draw(0, dx-2*scale, y-2*scale, scale, sx, sy, 32, 32)
					dx += (32*scale).toInt()
				}
				i++
			}
		}

	abstract override val rainbowCount:Int
	abstract override fun printFont(x:Int, y:Int, str:String, color:EventReceiver.COLOR, scale:Float, alpha:Float, rainbow:Int)

}