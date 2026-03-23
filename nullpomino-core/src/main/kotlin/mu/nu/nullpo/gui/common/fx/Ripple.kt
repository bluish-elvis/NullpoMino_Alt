/*
 Copyright (c) 2026, NullNoname
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

package mu.nu.nullpo.gui.common.fx

import mu.nu.nullpo.gui.common.AbstractRenderer
import mu.nu.nullpo.util.GeneralUtil.times
import mu.nu.nullpo.util.GeneralUtil.toHEXColor
import zeroxfc.nullpo.custom.libs.Interpolation.cosStep
import zeroxfc.nullpo.custom.libs.Interpolation.lerp

class Ripple(override var x:Float, override var y:Float, red:Int, green:Int, blue:Int, alpha:Int):Effect {
	private var ticks = 0
	private val cColor = Triple(
		lerp(red, 255, .2f)/255f, lerp(green, 255, .2f)/255f, lerp(blue, 255, .2f)/255f
	)
	private val cAlpha = lerp(alpha, 255, .2f)/255f
	override fun draw(i:Int, r:AbstractRenderer) {
		r.drawBlendAdd {
			if(ticks<40) {
				val c = cosStep(0f, 64f, ticks/40f)
				r.drawOval(x-c/2, y-c/2, c, c, 0, lerp(cAlpha, 0f, ticks/40f), 1f, (cColor*cAlpha).toHEXColor)
			}
			val s = cosStep(32f, 0f, ticks/60f)
			r.resources.imgFrags[1].draw(x-s, y-s, x+s, y+s, lerp(cAlpha, 0f, ticks/60f), cColor*cAlpha)
		}
	}

	override fun update(r:AbstractRenderer):Boolean = ++ticks>=60

}
