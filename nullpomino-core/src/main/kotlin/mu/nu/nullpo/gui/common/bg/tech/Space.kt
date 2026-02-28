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

package mu.nu.nullpo.gui.common.bg.tech

import mu.nu.nullpo.gui.common.bg.AbstractBG
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

class Space(addBGFX:AbstractBG<*>? = null):AbstractBG<Nothing?>(mu.nu.nullpo.gui.common.ResourceImage.Blank, addBGFX) {
	private sealed class Chunks {
		operator fun component1() = x
		operator fun component2() = y
		operator fun component3() = sc*size

		var vr = 0f
		var d = 0f
		var vel = 0f
		var ang = 0f
		val x get() = 320+d*cos(ang)
		val y get() = 240+d*sin(ang)
		val sc get() = (d/rad).pow(1.6f)
		abstract val size:Int
		fun update(spd:Float) {
			d += vel*spd
			//ang+=vr
			if(d>rad*1.05f&&(x !in 0f..640f||y !in 0f..480f)) reset() else {
				vel *= 1+d/rad*.05f
			}
		}

		open fun reset(pos:Boolean = false) {
			d = if(pos) Random.nextFloat()*rad else 0f
			ang = Random.nextFloat()*tau
			vel = (Random.nextFloat()+.2f)
			vr = (Random.nextFloat()-.5f)*.1f
		}

		init {
			reset(true)
		}

		class Star:Chunks() {
			override val size = 40
		}

		class Block(var skin:Int = 0, var color:Int = 0):Chunks() {
			override val size = 8
			override fun reset(pos:Boolean) {
				super.reset(pos)
				color = Random.nextInt(mu.nu.nullpo.game.component.Block.COLOR.ALL_COLOR_NUM)
				skin = Random.nextInt()
			}
		}
	}

	var vr = 0f
	private val children = List(256) {if(it%2==0) Chunks.Star() else Chunks.Block(0, 0)}
	/** Performs an update tick on the background. Advisably used in onLast.*/
	override fun update() {
		super.update()
		children.forEach {it.ang += vr; it.update(spdN)}
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
	override fun draw(render:mu.nu.nullpo.gui.common.AbstractRenderer, bg:Boolean) {
		if(bg) render.drawBlackBG()
		children.forEach {
			val (x, y, sc) = it
			if(it is Chunks.Block) render.drawBlock(x, y, it.color, it.skin, false, 0f, 1f, sc)
			else render.resources.imgFrags.let {img ->
				render.drawBlendAdd {
					img[0].draw(x-sc/2, y-sc/2, x+sc/2, y+sc/2, .6f)
				}
			}
		}
	}

	companion object {
		private const val tau = (Math.PI*2).toFloat()
		private val rad = (640f.pow(2)+480f.pow(2)).pow(.5f)

	}
}
