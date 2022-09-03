/*
 * Copyright (c) 2010-2022, NullNoname
 * Kotlin converted and modified by Venom=Nhelv
 * All rights reserved.
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

package mu.nu.nullpo.gui.slick.img

import mu.nu.nullpo.gui.slick.ResourceHolder
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow

object RenderBG {

	fun spinBG(bg:Int) {
		val bgmax = ResourceHolder.backgroundMax
		val bg = bg%bgmax
		if(bg in 0 until bgmax) {
			val bgi = ResourceHolder.imgPlayBG[bg].res
			val sc = ((1-cos(bgi.rotation/PI.pow(3.0))/PI)*1024f/minOf(bgi.width, bgi.height)).toFloat()
			val cx = bgi.width/2*sc
			val cy = bgi.height/2*sc
			bgi.setCenterOfRotation(cx, cy)
			bgi.rotate(0.04f)
			bgi.draw(320-cx, 240-cy, sc)
		}
	}

	fun kaleidoSquare() {
		TODO()
	}

	fun waterFall() {
		TODO()
	}

	fun abyss() {
		TODO()
	}
}
