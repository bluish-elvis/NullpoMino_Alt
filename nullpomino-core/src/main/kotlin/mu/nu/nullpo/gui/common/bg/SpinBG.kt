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

package mu.nu.nullpo.gui.common.bg

import mu.nu.nullpo.gui.common.AbstractRenderer

open class SpinBG<T:Any?>(bgi:mu.nu.nullpo.gui.common.ResourceImage<T>, addBGFX:AbstractBG<*>? = null)
	:AbstractBG<T>(bgi,addBGFX) {
//	val sc get() = ((1+sin(res.rotation*RG*2).absoluteValue/3)*640/minOf(res.width, res.height))
//	val cx get() = res.width/2*sc
//	val cy get() = res.height/2*sc
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
		super.update()
	}

	override fun reset() {
		a = 0f
		log.debug("SpinBG reset")
		addBGFX?.reset()
	}

	override fun draw(render:AbstractRenderer, bg:Boolean) {
		render.drawBlackBG()
		img.draw()
		addBGFX?.draw(render, true)
	}
}
