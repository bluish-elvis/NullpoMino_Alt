/*
 * Copyright (c) 2022-2024, NullNoname
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

import mu.nu.nullpo.game.event.EventReceiver.COLOR
import mu.nu.nullpo.gui.common.AbstractRenderer
import mu.nu.nullpo.gui.common.BaseFont.FONT
import mu.nu.nullpo.gui.common.libs.Vector

class PopupCombo(x:Int, y:Int, val pts:Int, val type:CHAIN, val ex:Int = 0):SpriteSheet(
	x, y, vel = Vector(-1, 0)
) {
	val ox = x
	override var alpha
		get() = minOf(1f, 3f-ticks/20f)
		set(v) {}

	override fun update(r:AbstractRenderer):Boolean {
		x += vel.x
		vel.x = (ox+ex-x)*0.1f
		ticks++
		return ticks>=60
	}

	override fun draw(i:Int, r:AbstractRenderer) {
		val x = x.toInt()
		val y = y.toInt()
		when(type) {
			CHAIN.B2B -> {
				r.drawFont(x-18, y-15, "SKILL", FONT.NANO, COLOR.RED, .75f, alpha)
				r.drawDirectNum(x-18, y, "%2d".format(pts), COLOR.YELLOW, 1.5f, alpha)
				r.drawFont(x-18, y+20, "Rush!", FONT.NANO, COLOR.ORANGE, .75f, alpha)
			}
			CHAIN.COMBO -> {
				r.drawDirectNum(x-18, y-0, "%2d".format(pts), COLOR.CYAN, 1.5f, alpha)
				r.drawFont(x+18, y+8, "REN", FONT.NANO, COLOR.BLUE, .5f, alpha)
				r.drawFont(x-18, y+20, "Combo!", FONT.NANO, COLOR.BLUE, .75f, alpha)
			}
			CHAIN.CHAIN -> {
				r.drawDirectNum(x-18, y-0, "%2d".format(pts), COLOR.YELLOW, 1.5f, alpha)
				r.drawFont(x+18, y+8, "Hits", FONT.NANO, COLOR.ORANGE, .5f, alpha)
				r.drawFont(x-18, y+20, "Chain!", FONT.NANO, COLOR.ORANGE, .75f, alpha)
			}
		}
	}

	enum class CHAIN {
		COMBO, B2B, CHAIN
	}
}
