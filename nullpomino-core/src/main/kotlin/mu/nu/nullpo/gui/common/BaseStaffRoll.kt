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

import mu.nu.nullpo.game.event.EventReceiver.COLOR.BLUE
import mu.nu.nullpo.game.event.EventReceiver.COLOR.GREEN
import mu.nu.nullpo.game.event.EventReceiver.COLOR.ORANGE
import mu.nu.nullpo.game.event.EventReceiver.COLOR.WHITE
import org.apache.logging.log4j.LogManager
import java.io.FileInputStream
import java.io.IOException

abstract class BaseStaffRoll<T> {
	/** Log */
	internal val log = LogManager.getLogger()
	abstract val bufI:ResourceImage<T>
	val scale = .5f
	open val width:Int = 160
	open val height:Int get() = strList.size*14
	protected val strList by lazy {
		try {
			this::class.java.getResource("../staff.lst")?.path?.let {t ->
				FileInputStream(t).bufferedReader().use {it.readLines()}
			}?.map {s -> s.trim {it<=' '}}?.filterNot {it.startsWith('#')||it.startsWith("//")} // Commment-line. Ignore it.
				?: emptyList()
		} catch(e:IOException) {
			log.error("Failed to load Staffroll list file", e)
			emptyList()
		}
	}

	protected fun cmp(c:Char):Float = when(c) {
		' ', ':' -> .5f
		'(', ')', '.' -> .33f
		'/', 'I' -> .25f
		else -> 0f
	}

	abstract fun drawBuf(dx:Float, dy:Float, dw:Float, dh:Float, sx:Float, sy:Float, sw:Float, sh:Float)

	/** Folder names list */
	protected fun init() {
		val w = BaseFontNano.W*scale
		val h = BaseFontNano.H*scale

		var dy = 0f
		strList.forEach {str ->
			var dx = width/2f-(str.length-str.sumOf {cmp(it).toDouble()}.toFloat())*.5f*w
			str.forEachIndexed {i, it ->
				if(i>0||it!=':') {
					val fontColor = when(str.first()) {
						':' -> if(it.isUpperCase()) BLUE else GREEN
						else -> if(it.isUpperCase()) ORANGE else WHITE
					}
					val chr = it.uppercaseChar().code
					val sx = BaseFontNano.W*((chr-32)%32)
					val sy = BaseFontNano.H*((chr-32)/32+fontColor.ordinal*3)
					dx -= w*cmp(it)/2
					drawBuf(dx, dy, dx+w, dy+h, sx.toFloat(), sy.toFloat(), sx+12f, sy+14f)
					dx += w*(1f-cmp(it)/2)
				}
			}
			dy += 20*scale
		}
	}

	fun draw() = bufI.draw()
	fun draw(x:Float, y:Float, sy:Float, h:Float, alpha:Float = 1f,
		color:Triple<Float, Float, Float> = Triple(1f, 1f, 1f)) =
		bufI.draw(
			x, y-minOf(0f, sy), x+width, y+h, 0f, maxOf(0f, sy), 0f+width, minOf(sy+h, 0f+height),
			alpha, color
		)
}
