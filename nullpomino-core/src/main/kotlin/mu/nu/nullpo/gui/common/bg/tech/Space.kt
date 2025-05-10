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

import zeroxfc.nullpo.custom.libs.Vector
import kotlin.random.Random

class Space():mu.nu.nullpo.gui.common.bg.AbstractBG<Nothing?>(mu.nu.nullpo.gui.common.ResourceImage.Blank) {
	private class Star() {
		var pos = Vector()
		var vel = Vector()
		val x get() = pos.x
		val y get() = pos.y
		var sc = 0f
		fun update(spd:Float) {
			pos += vel*spd
			pos.x %= 640+sc
			pos.y %= 480+sc
		}
	}

	private val children = List(256) {Star()}
	/** Performs an update tick on the background. Advisably used in onLast.*/
	override fun update() {
		children.forEach {it.update(spdN)}
		super.update()
	}

	/** Resets the background to its base state.*/
	override fun reset() {
		tick = 0
		children.forEach {
			val s = Random.nextInt(26, 41)*.1f
			it.sc = s*2.5f
			it.pos.set(Random.nextFloat()*640 to Random.nextFloat()*480)
			it.vel.set((Random.nextFloat()-.5f)*.01f*s to (Random.nextFloat()-.5f)*.01f*s)
		}
	}

	/** Draws the background to the game screen.*/
	override fun draw(render:mu.nu.nullpo.gui.common.AbstractRenderer, bg:Boolean) {
		if(bg) render.drawBlackBG()
		render.resources.imgFrags.let {img ->
			render.drawBlendAdd {
				children.forEach {
					img[0].draw(it.x-it.sc/2, it.y-it.sc/2, it.x+it.sc/2, it.y+it.sc/2, .6f)
				}
			}
		}
	}

	companion object {
		private const val tau = (Math.PI*2).toFloat()
	}
}
