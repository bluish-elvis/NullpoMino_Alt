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

import mu.nu.nullpo.gui.common.AbstractRenderer
import mu.nu.nullpo.gui.common.fx.FragAnim.ANIM.BLOCK
import mu.nu.nullpo.gui.common.fx.FragAnim.ANIM.GEM
import mu.nu.nullpo.gui.common.fx.FragAnim.ANIM.HANABI
import mu.nu.nullpo.gui.common.fx.FragAnim.ANIM.SPARK
import mu.nu.nullpo.gui.common.libs.Vector

class FragAnim(val type:ANIM, x:Float, y:Float, val color:Int, val spd:Int = 0, scale:Float = 1f, alpha:Float = 1f,
	vel:Vector = Vector(0f, 0f)):SpriteSheet(x, y, vel, scale, alpha) {
	constructor(type:ANIM, x:Int, y:Int, color:Int, spd:Int = 0, scale:Float = 1f, alpha:Float = 1f,
		vel:Vector = Vector(0f, 0f)):this(type, x.toFloat(), y.toFloat(), color, spd, scale, alpha, vel)

	override fun update(r:AbstractRenderer):Boolean {
		ticks += if(type==HANABI) 1 else maxOf(1, spd)
		if(type==HANABI) vel.y += 0.3f
		return (ticks>=when(type) {
			GEM -> 60
			HANABI -> 48
			else -> 36
		})
	}

	override fun draw(i:Int, r:AbstractRenderer) {
		val flip = (i%2==0)!=(i%10==0)
		when(type) {
			// Gems frag
			GEM -> r.resources.imgPErase[color]
			// TI Block frag
			SPARK -> r.resources.imgBreak[color][0]
			// TAP Block frag
			BLOCK -> r.resources.imgBreak[color][1]
			//Fireworks
			HANABI -> r.resources.imgHanabi[color]
		}.draw(
			if(flip) dx2 else dx, dy, if(flip) dx else dx2, dy2, srcX, srcY, srcX2, srcY2, alpha
		)
	}

	val offset = when(type) {
		BLOCK -> Vector(88, 86)
		SPARK -> Vector(88, 38)
		GEM -> Vector(16, 16)
		HANABI -> Vector(96, 96)
	}
	val table = when(type) {
		BLOCK -> 8 to 8
		SPARK -> 6 to 6
		GEM -> 10 to 10
		HANABI -> 6 to 8
	}
	val sq = when(type) {
		BLOCK -> 192
		SPARK -> 192
		GEM -> 64
		HANABI -> 192
	}

	override val dx:Float get() = x-offset.x
	override val dy:Float get() = y-offset.y
	override val dx2:Float get() = dx+sq
	override val dy2:Float get() = dy+sq
	override val srcX:Int get() = (ticks-1)%table.first*sq
	override val srcY:Int get() = (ticks-1)/table.second*sq
	override val srcX2:Int get() = srcX+sq
	override val srcY2:Int get() = srcY+sq
	//flip = (type==BLOCK||type==SPARK)
	//draw(if(flip) x else x+sq, y, if(flip) x+sq else x, y+sq,srcX, srcy, srcX+sq, srcy+sq)

	enum class ANIM {
		// Normal Block, TA Block Frag
		BLOCK,

		// Controlled Block, TI Block Frag
		SPARK,

		// Gem Block
		GEM,

		// Fireworks
		HANABI
	}
}
