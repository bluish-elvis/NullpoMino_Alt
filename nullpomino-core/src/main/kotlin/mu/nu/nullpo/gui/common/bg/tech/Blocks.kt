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

import mu.nu.nullpo.gui.common.AbstractRenderer
import mu.nu.nullpo.gui.common.ResourceImage
import mu.nu.nullpo.gui.common.bg.AbstractBG
import zeroxfc.nullpo.custom.libs.Vector
import kotlin.random.Random

class Blocks(addBGFX:AbstractBG<*>? = null):AbstractBG<Nothing?>(ResourceImage.Blank, addBGFX) {
	private class Frag {
		operator fun component1() = x
		operator fun component2() = y
		operator fun component3() = sc
		operator fun component4() = skin
		operator fun component5() = color

		var pos = Vector()
		var vel = Vector()
		val x get() = pos.x
		val y get() = pos.y
		var sc = 0f
		var skin = 0
		var color = 0
		fun update(spd:Float) {
			pos += vel*spd
			//pos.set((pos+(sc to sc)).mod(640f+sc*2, 480f+sc*2)-(sc to sc))
			if(pos.x !in -sc*8..640+sc*8||pos.y !in -sc*8..480+sc*8) reset()
		}

		fun reset(pp:Boolean = false) {
			sc = Random.nextInt(8, 32)/16f
			val (p, v) = when(Random.nextInt(4)) {
				0 -> (Random.nextFloat()*640 to sc*-8) to (0f to sc*(.2f+Random.nextFloat()))
				1 -> (Random.nextFloat()*640 to 480+sc*8) to (0f to -sc*(.2f+Random.nextFloat()))
				2 -> (sc*-8f to Random.nextFloat()*480) to (sc*(.2f+Random.nextFloat()) to 0f)
				else -> (640+sc*8 to Random.nextFloat()*480) to (-sc*(.2f+Random.nextFloat()) to 0f)
			}
			pos.set(if(pp) Random.nextFloat()*640 to Random.nextFloat()*480 else p)
			vel.set(v)
			color = Random.nextInt(mu.nu.nullpo.game.component.Block.COLOR.ALL_COLOR_NUM)
			skin = Random.nextInt()
		}
	}

	var vel = Vector()
	private val children = List(64) {Frag()}
	/** Performs an update tick on the background. Advisably used in onLast.*/
	override fun update() {
		children.forEach {it.pos += vel*it.sc; it.update(spdN)}
		super.update()
	}

	/** Resets the background to its base state.*/
	override fun reset() {
		super.reset()
		tick = 0
		children.forEach {
			it.reset(true)
		}
	}

	/** Draws the background to the game screen.*/
	override fun draw(render:AbstractRenderer, bg:Boolean) {
		if(bg) render.drawBlackBG()
		children.forEach {(x, y, sc, skin, color) ->
			render.drawBlock(x-sc*8f, y-sc*8f, color, skin, false, 0f, .6f, sc)
		}

	}
}
