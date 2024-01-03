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
import kotlin.math.ceil
import kotlin.math.floor

abstract class BaseStaffRoll {

	enum class LineType {
		Plane, Header,
	}

	abstract val img:ResourceImage<*>

	/** @param scr scroll amount
	 * @param dh draw height*/
	fun draw(x:Float, y:Float, scr:Float, dh:Float, alpha:Float = 1f) {
		fun cmp(c:Char) = when(c) {
			' ', ':' -> .5f
			'(', ')', '.' -> .33f
			'/', 'I' -> .25f
			else -> 0f
		}
		strList.drop(maxOf(0, floor(scr/lh).toInt())).take(1+ceil(maxOf(0f, minOf(dh, dh+scr))/lh).toInt()).let {list ->
			list.forEachIndexed {l, (type, str) ->
				str.fold(x+width/2f-(str.length-str.sumOf {cmp(it).toDouble()}.toFloat())*.5f*fw) {cx, it ->
					val fontColor = when(type) {
						LineType.Header -> if(it.isUpperCase()) BLUE else GREEN
						LineType.Plane -> if(it.isUpperCase()) ORANGE else WHITE
					}
					val chr = it.uppercaseChar().code
					val sx = BW*((chr-32)%32)
					val sy = BH*((chr-32)/32+fontColor.ordinal*3)
					val dx = cx-fw*cmp(it)/2
					val dy = y+l*lh-if(scr<0) scr else scr%lh
					img.draw(
						dx, maxOf(y, dy), cx+fw, maxOf(y, minOf(y+dh, dy+fh)),
						sx.toFloat(), if(dy<y) sy+minOf(14f, (y-dy)/scale) else sy.toFloat(), sx+BW.toFloat(),
						if(dy+fh>y+dh) sy+14f-minOf(14f, (dy+fh-y-dh)/scale) else sy+14f, alpha
					)
					cx+fw-fw*cmp(it)/2
				}
			}
		}
	}

	companion object {
		/** Log */
		internal val log = LogManager.getLogger()
		const val scale = .5f
		const val width = 160
		private const val BW = BaseFontNano.W
		private const val BH = BaseFontNano.H
		private const val fw = BW*scale
		private const val fh = BH*scale
		private const val lh = fh+2
		private val strList by lazy {
			try {
				this::class.java.getResource("/staff.lst")?.path?.let {t ->
					FileInputStream(t).bufferedReader().use {it.readLines()}
				}?.map {s -> s.trim {it<=' '}}?.filterNot {it.startsWith('#')||it.startsWith("//")} // Commment-line. Ignore it.
					?.map {(if(it.startsWith(':')) LineType.Header else LineType.Plane) to it} ?: emptyList()
			} catch(e:IOException) {
				log.error("Failed to load Staffroll list file", e)
				emptyList()
			}
		}
		val height get() = (strList.size*lh)
	}
}
