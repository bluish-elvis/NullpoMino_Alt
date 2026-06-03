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

class Hitbox(override var x:Float, override var y:Float, var w:Float, var h:Float, val lifetime:Int = 60,
	red:Int = 255, green:Int = 0, blue:Int = 0, alpha:Int = 255):Effect {
	constructor(x:Number, y:Number, w:Number, h:Number, lifetime:Int = 60, red:Int = 255, green:Int = 0, blue:Int = 0,
		alpha:Int = 255):
		this(x.toFloat(), y.toFloat(), w.toFloat(), h.toFloat(), lifetime, red, green, blue, alpha)

	private var ticks = 0
	private val color = Triple(red/255f, green/255f, blue/255f)
	private val cColor = Triple(
		lerp(red, 255, .2f)/255f, lerp(green, 255, .2f)/255f, lerp(blue, 255, .2f)/255f
	)
	private val cAlpha = lerp(alpha, 255, .2f)/255f
	private val alpha = alpha/255f
	private val maxTime = lifetime.let {it*2f/3 to (it.toFloat())}
	override fun draw(i:Int, r:AbstractRenderer) {
		r.drawBlendAdd {

			if(ticks<maxTime.first) {
				val lA = lerp(alpha, 0f, ticks/maxTime.first)
				r.drawRect(x, y, w, h, (color*lA).toHEXColor, lA)
			}
			val s = cosStep(1f, 0f, ticks/maxTime.second)
			r.drawRect(x, y, w, h, 0, 1f, 1f, (cColor*s).toHEXColor)
		}
	}

	override fun update(r:AbstractRenderer):Boolean = ++ticks>=maxTime.second

}
