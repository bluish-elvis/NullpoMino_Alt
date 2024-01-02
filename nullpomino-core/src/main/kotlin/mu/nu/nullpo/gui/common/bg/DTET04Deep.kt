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
import mu.nu.nullpo.gui.common.libs.Vector
import zeroxfc.nullpo.custom.libs.Interpolation
import kotlin.math.sin
import kotlin.random.Random

class DTET04Deep<T>(bg:ResourceImage<T>):AbstractBG<T>(bg) {
	/*'（オーロラ）
Crs = Rnd * 360
CrsF = Rnd * 640
CrsFF = Rnd * 360*/
	private var a = Random.nextFloat()*360
	private var b = Random.nextFloat()*640
	override var tick = Random.nextInt(7200)
	private var spdN = 0f

	override fun update() {
		a += 2.1f+spdN
		while(a<0) a += 360
		while(a>=360) a -= 360

		val c = tick*.2f
		b += sin(c*RG)*maxOf(-1f, spdN-1.1f)
		while(b<0) b += 640
		while(b>=640) b -= 640
		tick++
		while(tick>=7200) tick -= 7200
		/*
Crs = Crs + 3.5: If Crs >= 360 Then Crs = Crs - 360
If TrM >= 2 Then
	CrsF = CrsF + Sin(CrsFF * Rg) * 2
	If CrsF < 0 Then CrsF = CrsF + 640
	If CrsF >= 640 Then CrsF = CrsF - 640
	CrsFF = CrsFF + 0.2: If CrsFF >= 360 Then CrsFF = CrsFF - 360
End If*/
		if(spdN!=speed)
			spdN = if(Vector.almostEqual(spdN, speed, .001f/100)) speed else
				Interpolation.lerp(spdN, speed, .05f)
	}

	override fun reset() {
		tick = 0
		a = Random.nextFloat()*360
		b = Random.nextFloat()*640
		tick = Random.nextInt(7200)
		spdN = 0f
	}

	override fun draw(render: AbstractRenderer) {
		for(i in 0..79) {
			for(j in 0..7) {
				val ww = 5+3*(.5f+spdN)
				val x = (b+sin((a+i*17f)*RG)*ww*if(j%2==0) 1 else -1)%640
				val y = i*8f+j
				img.draw(0f, y, x, y, 640f, y+1)
				img.draw(640-x, y, 0f, y, x, y+1)
			}
		}
	}
}
/*
Case 4 '（オーロラ）
For I = 0 To 79
R = CrsF + Sin((Crs + I * 17) * Rg) * 7 * (TrM + 1)
If R > 640 Then R = R - 640 Else If R < 0 Then R = R + 640
With Src
	.Left = R: .Top = I * 8: .Right = 640: .Bottom = .Top + 8
End With
If Not FS Then BBSf.BltFast 0, I * 8, BGSf, Src, DDBLTFAST_WAIT
With Src
	.Left = 0: .Top = I * 8: .Right = R: .Bottom = .Top + 8
End With
If Not FS Then BBSf.BltFast 640 - R, I * 8, BGSf, Src, DDBLTFAST_WAIT
Next I
*/
