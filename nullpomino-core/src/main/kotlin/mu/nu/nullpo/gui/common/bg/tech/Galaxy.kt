/*
 Copyright (c) 2025, NullNoname
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

package mu.nu.nullpo.gui.common.bg.tech

import mu.nu.nullpo.util.GeneralUtil.HSBtoRGB
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class Galaxy():mu.nu.nullpo.gui.common.bg.AbstractBG<Nothing?>(mu.nu.nullpo.gui.common.ResourceImage.Blank, Space()) {
	private class Star() {
		var dist:Float = 0f
		var rev:Float = 0f
		fun update(tick:Float) {
			rev += tick/(dist+1)
			rev %= tau
		}
	}

	private val children = List(20) {List(16*(it+1)) {Star()}}
	/** Performs an update tick on the background. Advisably used in onLast.*/
	override fun update() {
		children.flatten().forEach {i -> i.update(spdN*.02f)}
		super.update()
	}

	/** Resets the background to its base state.*/
	override fun reset() {
		addBGFX?.reset()
		tick = 0
		children.forEachIndexed {i, t ->
			t.forEachIndexed {j, it ->
				it.dist = i+Random.nextFloat()
				it.rev = (tau*j-tau*Random.nextFloat())/t.size
			}
		}
	}

	/** Draws the background to the game screen.*/
	override fun draw(render:mu.nu.nullpo.gui.common.AbstractRenderer, bg:Boolean) {
		if(bg) render.drawBlackBG()
		addBGFX?.draw(render, false)
		render.resources.imgFrags.let {img ->
//			img[1].draw(120f, 40f, 520f, 440f, .2f, Triple(0f, 0f, 0f))
			render.drawBlendAdd {
				val rr = tau/6
				children.flatten().forEach {
					val gx = 6*it.dist*cos(it.rev)
					val gy = 18*it.dist*sin(it.rev)
					val x = 320+gx*cos(rr)-gy*sin(rr)
					val y = 240+gy*cos(rr)+gx*sin(rr)
					val sc = 16+(y-240)/40f-it.dist/2
					img[0].draw(
						x-sc/2, y-sc/2, x+sc/2, y+sc/2,
						.2f,
						if(it.dist<5) Triple(.088f, it.dist-2/7, 1f).HSBtoRGB()
						else Triple(.572f, it.dist/70+.1f, (22-it.dist)/12).HSBtoRGB(),
					)
				}
			}
		}
	}

	companion object {
		private const val tau = (Math.PI*2).toFloat()
	}
}
