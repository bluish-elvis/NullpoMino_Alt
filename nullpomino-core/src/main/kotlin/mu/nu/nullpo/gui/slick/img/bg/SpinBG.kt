/*
 Copyright (c) 2022-2024, NullNoname
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

package mu.nu.nullpo.gui.slick.img.bg

import mu.nu.nullpo.gui.common.AbstractRenderer
import mu.nu.nullpo.gui.common.bg.AbstractBG as BaseBG
import mu.nu.nullpo.gui.slick.ResourceImageSlick
import kotlin.math.absoluteValue
import kotlin.math.sin

class SpinBG(bgi:ResourceImageSlick, private val addBGFX:BaseBG<*>? = null):AbstractBG(bgi) {
	val sc get() = ((1+sin(this.res.rotation*RG*2).absoluteValue/3)*640/minOf(this.res.width, this.res.height))
	val cx get() = this.res.width/2*sc
	val cy get() = this.res.height/2*sc
	var a = 0f
	override var speed:Float = 1f
	override var tick:Int
		get() = a.toInt()
		set(value) {
			a = value.toFloat()
		}

	override fun update() {
		val fact = speed*.1f
		a += fact
		a %= 360f
		res.setCenterOfRotation(cx, cy)
	}

	override fun reset() {
		a = 0f
		logger.debug("SpinBG reset")
		addBGFX?.reset()
	}

	override fun draw(render:AbstractRenderer) {
		render.drawBlackBG()
		res.rotation = a
		res.draw(320f-cx, 240f-cy, sc)
		/*render.drawFont(0, 0, "${res.width}x${res.height} $sc", BaseFont.FONT.NANO, scale = .5f)
		render.drawFont(
			0,
			8,
			"${640f/minOf(this.res.width, this.res.height)} x ${sin(PI+this.res.rotation*RG*4)}",
			BaseFont.FONT.NANO,
			scale = .5f
		)*/
		addBGFX?.draw(render)
	}

}
