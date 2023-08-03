/*
 * Copyright (c) 2022-2022, NullNoname
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

package mu.nu.nullpo.gui.common.fx

import mu.nu.nullpo.game.event.EventReceiver.Companion.getRainbowColor
import mu.nu.nullpo.gui.common.AbstractRenderer
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class PopupBravo(x:Float, y:Float, val w:Int, val h:Int):SpriteSheet(x, y) {

	override fun update(r:AbstractRenderer):Boolean {
		ticks++
		return ticks>=100
	}

	override val dx2:Float get() = x+w
	override val dy2:Float get() = y+h
	override fun draw(i:Int, r:AbstractRenderer) {
		r.drawBlendAdd {
			if(ticks<24) {
				val srcX = 0
				val srcX2 = srcX+80
				val srcY = (ticks/3-1)*8
				val srcY2 = srcY+8
				r.resources.imgLine[0].draw(x, y, dx2, dy2, srcX, srcY, srcX2, srcY2)
			}
			if(ticks<32) {
				val srcX = (ticks/2-1)*8
				val srcX2 = srcX+16
				val srcY = 0
				val srcY2 = srcY+160
				r.resources.imgLine[1].draw(x-w*.25f, y, dx2+w*.25f, dy2, srcX, srcY, srcX2, srcY2)
			}
		}
		for(i in listOf(1, 2, 4))
			if(ticks<(25*i)) {
				val s = sin(ticks*PI.toFloat()/(50*i))
				alpha = cos(ticks*PI.toFloat()/(50*i))
				r.drawRectSpecific(x-s*16, y-s*16, w+s*32, h+s*32, alpha = alpha)
				r.drawRectSpecific(x-s*32, y-s*32, w+s*64, h+s*64, alpha = alpha)
			}
		r.drawDirectFont(x, y+h/2-20, "PERFECT!", getRainbowColor(ticks%9), 1.25f)
		r.drawDirectFont(x-8, y+h/2+16, "ALL CLEARED", getRainbowColor((ticks+4)%9), 1f)
	}
}
