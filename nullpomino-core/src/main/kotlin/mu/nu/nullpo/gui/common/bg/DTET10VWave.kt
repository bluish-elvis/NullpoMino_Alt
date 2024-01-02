/*
 * Copyright (c) 2024, NullNoname
 * Kotlin converted and modified by Venom=Nhelv.
 *
 * THIS WAS NOT MADE IN ASSOCIATION WITH THE GAME CREATOR.
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

package mu.nu.nullpo.gui.common.bg

import mu.nu.nullpo.gui.common.AbstractRenderer
import mu.nu.nullpo.gui.common.ResourceImage
import kotlin.math.sin

class DTET10VWave<T>(bg:ResourceImage<T>):AbstractBG<T>(bg) {
	//	val sc get() = ((1+sin(this.bg.rotation*PI/180)/3)*1024f/minOf(this.bg.width, this.bg.height)).toFloat()
//	val cx get() = this.bg.width/2*sc
//	val cy get() = this.bg.height/2*sc
	private var t = 0f
	override var tick:Int
		get() = t.toInt()
		set(value) {
			t = value.toFloat()
		}

	override fun update() {
		t += 1+speed
//		bg.rotation = t*.04f
//		bg.setCenterOfRotation(cx, cy)
		while(t>=360/.04f) t -= 360
	}

	override fun reset() {
		tick = 0
	}

	override fun draw(render: AbstractRenderer) {
		for(i in 0..59) {
			val y = 30f+i*7+sin((t*1.7f+i*6)*RG)*28
			img.draw(0f, i*8f, 0f, y, 640f, y+8)
		}
	}
}
/*
For I = 0 To 59
With Src
.Left = 0: .Top = 30 + I * 7 + Sin((Spa + I * 6) * Rg) * 28: .Right = .Left + 640: .Bottom = .Top + 8
End With
If Not FS Then BBSf.BltFast 0, I * 8, BGSf, Src, DDBLTFAST_WAIT
Next I
Spa = Spa + 1 + Lev * 0.03: If Spa >= 360 Then Spa = Spa - 360
*/
