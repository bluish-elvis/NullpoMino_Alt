/*
 * Copyright (c) 2022-2023, NullNoname
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

package mu.nu.nullpo.gui.slick.img.bg

import mu.nu.nullpo.gui.common.ResourceImage
import mu.nu.nullpo.gui.common.libs.Vector
import org.newdawn.slick.Image
import kotlin.math.sin

class SpinBG(bg:ResourceImage<Image>):AbstractBG(bg) {
	val sc get() = ((1+sin(this.bg.rotation*Rg)/3)*1024f/minOf(this.bg.width, this.bg.height))
	val cx get() = this.bg.width/2*sc
	val cy get() = this.bg.height/2*sc
	var a = 0f
	override var speed:Float = 1f
	override var tick:Int
		get() = a.toInt()
		set(value) {
			a = value.toFloat()
		}

	override fun update() {
		val fact = speed*.3f
		if(!Vector.almostEqual(speed, 0f, Float.MIN_VALUE)) {
			a += fact
			bg.rotation = a
			bg.setCenterOfRotation(cx, cy)
			while(a>=360) a -= 360
			while(a<0) a += 360
		}
	}

	override fun reset() {
		a = 0f
		bg.rotation = 0f
	}

	override fun draw() {
		bg.draw(320-bg.centerOfRotationX, 240-bg.centerOfRotationY, sc)
	}

}
