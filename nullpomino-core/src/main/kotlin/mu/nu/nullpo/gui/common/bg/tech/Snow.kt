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

class Snow():mu.nu.nullpo.gui.common.bg.AbstractBG<Nothing?>(mu.nu.nullpo.gui.common.ResourceImage.Blank) {
	private class Flake(x:Float, vx:Float, vy:Float, var rx:Float, var ry:Float) {
		var pos = Vector(x, 0f)
		var vel = Vector(vx, vy)
		val x get() = pos.x
		val y get() = pos.y

		fun update(spd:Float):Boolean {
			pos += vel*spd*.4f
			vel.x += (.02f-Random.nextFloat()*.04f)*spd*.4f
			rx = (rx+Random.nextFloat()-.5f).coerceIn(2f..4f)
			ry = (ry+Random.nextFloat()-.5f).coerceIn(3f..5f)
			return pos.y>480f-ry

		}
	}

	private val children = mutableListOf<Flake>()

	override fun update() {
		tick++
		if(tick%(if(tick%626>260) 3 else 6)==0)
			children.add(Flake(Random.nextFloat()*640, Random.nextFloat()*2-1, 1+Random.nextFloat()*.6f,
				2+Random.nextFloat()*2, 2+Random.nextFloat()*2))
		children.removeIf {it.update(spdN)}
		tick %= 1878
		super.update()
	}
	/** Resets the background to its base state.*/
	override fun reset() {
		children.clear()
		tick = 0
	}

	/** Draws the background to the game screen.*/
	override fun draw(render:mu.nu.nullpo.gui.common.AbstractRenderer, bg:Boolean) {
		if(bg) render.drawBlackBG()
		render.resources.imgFrags.let {img ->
			render.drawBlendAdd {
				children.forEach {
					img[2].draw(it.x-it.rx, it.y-it.ry, it.x+it.rx, it.y+it.ry, .7f)
				}
			}
		}
	}

	companion object {
		private const val tau = (Math.PI*2).toFloat()
	}
}
