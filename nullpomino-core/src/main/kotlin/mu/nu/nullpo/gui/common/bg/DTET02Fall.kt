/*
 * Copyright (c) 2023, NullNoname
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

import mu.nu.nullpo.gui.common.ResourceImage
import kotlin.random.Random

class DTET02Fall<T>(bg:ResourceImage<T>):AbstractBG<T>(bg) {
	/*'（紫空）
PSkD(0) = 0: PSkD(1) = 24: PSkD(2) = 72: PSkD(3) = 144
PSkD(4) = 240: PSkD(5) = 336: PSkD(6) = 408: PSkD(7) = 456
PSkS1(0) = 0: PSkS2(0) = 24
PSkS1(1) = 48: PSkS2(1) = 48
PSkS1(2) = 144: PSkS2(2) = 72
PSkS1(3) = 288: PSkS2(3) = 96
PSkS1(4) = 288: PSkS2(4) = 96
PSkS1(5) = 144: PSkS2(5) = 72
PSkS1(6) = 48: PSkS2(6) = 48
PSkS1(7) = 0: PSkS2(7) = 24
PSkP = Rnd*/
	private val dy = listOf(0, 24, 72, 144, 240, 336, 408, 456)
	private val dh = listOf(0 to 24, 48 to 48, 144 to 72, 288 to 96, 288 to 96, 144 to 72, 48 to 48, 0 to 24)
	private var py = Random.nextFloat()
	override var tick:Int
		get() = (py*100).toInt()
		set(value) {
			py = value/100f
		}

	override fun update() {
		py += .01f+speed*.03f*if(speed>=1.5f) -1 else 1
		if(py<0) py += 1
		if(py>=1) py -= 1
	}

	override fun reset() {
		py = Random.nextFloat()
	}

	override fun draw() {
		dy.zip(dh).forEachIndexed {i, (d, second) ->
			val r = ((i%2)+py*2).let {it+if(it<0) 2 else 0}%2
			val s1 = second.first
			val s2 = second.second
			val sy = s1+r*s2
			if(r<1) img.draw(0f, 0f+d, 0f, sy, 640f, sy+s2)
			else {
				img.draw(0f, 0f+d, 0f, sy, 640f, s1+s2*2f)
				img.draw(0f, d+s2*(2-r), 0f, 0f+s1, 640f, s1+s2*(r-1))
			}
		}
	}
}
/*Case 2 '（紫空）
For I = 0 To 7
R = (I Mod 2) + PSkP * 2: If R >= 2 Then R = R - 2
If R < 1 Then
	With Src
		.Left = 0: .Top = PSkS1(I) + R * PSkS2(I): .Right = .Left + 640: .Bottom = .Top + PSkS2(I)
	End With
	If Not FS Then BBSf.BltFast 0, PSkD(I), BGSf, Src, DDBLTFAST_WAIT
Else
	With Src
		.Left = 0: .Top = PSkS1(I) + R * PSkS2(I): .Right = .Left + 640: .Bottom = PSkS1(I) + PSkS2(I) * 2
	End With
	If Not FS Then BBSf.BltFast 0, PSkD(I), BGSf, Src, DDBLTFAST_WAIT
	With Src
		.Left = 0: .Top = PSkS1(I): .Right = .Left + 640: .Bottom = .Top + PSkS2(I) * (R - 1)
	End With
	If Not FS Then BBSf.BltFast 0, PSkD(I) + PSkS2(I) * (2 - R), BGSf, Src, DDBLTFAST_WAIT
End If
Next I
If TrM = 0 Then PSkP = PSkP - 0.01: If PSkP < 0 Then PSkP = PSkP + 1
If TrM = 1 Then PSkP = PSkP - 0.03: If PSkP < 0 Then PSkP = PSkP + 1
If TrM = 2 Then PSkP = PSkP + 0.05: If PSkP >= 1 Then PSkP = PSkP - 1
*/
