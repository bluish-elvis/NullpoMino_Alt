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

import mu.nu.nullpo.game.event.EventReceiver.COLOR
import mu.nu.nullpo.gui.slick.ResourceHolder
import mu.nu.nullpo.gui.slick.drawImage
import org.apache.logging.log4j.LogManager
import org.newdawn.slick.Color
import org.newdawn.slick.Graphics
import org.newdawn.slick.Image
import java.io.BufferedReader
import java.io.FileReader
import java.io.IOException

object RenderStaffRoll {
	/** Log */
	internal val log = LogManager.getLogger()
	lateinit var img:Image; private set
	val BG:Graphics get() = img.graphics
	val height get() = img.height

	private fun cmp(c:Char):Float = when(c) {
		' ', ':' -> .5f
		'(', ')', '.' -> .33f
		'/', 'I' -> .25f
		else -> 0f
	}

	/** Folder names list */
	init {
		try {
			val strList = BufferedReader(FileReader("config/list/staff.lst")).readLines()
			val scale = .5f
			val w = 12f*scale
			val h = 14f*scale

			img = Image(160, (strList.size*20*scale).toInt())
			var dy = 0f
			strList.forEach lit@{line ->
				line.trim {it<=' '}.let {str ->
					if(!str.startsWith('#')&&!str.startsWith("//")) { // Commment-line. Ignore it.
						var dx = img.width/2f-(str.length-str.sumOf {cmp(it).toDouble()}.toFloat())*.5f*w
						str.forEachIndexed {i, it ->
							if(i>0||it!=':') {
								val fontColor = when(str.first()) {
									':' -> if(it.isUpperCase()) COLOR.BLUE else COLOR.GREEN
									else -> if(it.isUpperCase()) COLOR.ORANGE else COLOR.WHITE
								}
								val chr = it.uppercaseChar().code
								var sx = (chr-32)%32
								var sy = (chr-32)/32+fontColor.ordinal*3
								sx *= 12
								sy *= 14
								dx -= w*cmp(it)/2
								BG.drawImage(ResourceHolder.imgFontNano, dx, dy, dx+w, dy+h, sx.toFloat(), sy.toFloat(), sx+12f, sy+14f)
								dx += w*(1f-cmp(it)/2)
							}
						}
						dy += 20*scale
					}
				}
			}
		} catch(e:IOException) {
			log.error("Failed to load Staffroll list file", e)
		}
	}

	fun draw() = img.draw()
	fun draw(x:Float, y:Float, sy:Float, h:Float, filter:Color = Color.white) =
		img.draw(x, y, x+img.width.toFloat(), y+h, 0f, sy, img.width.toFloat(), sy+h, filter)

}
