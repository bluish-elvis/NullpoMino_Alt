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
import mu.nu.nullpo.gui.common.libs.Vector.Companion.almostEqual
import zeroxfc.nullpo.custom.libs.Interpolation
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

class DTET00Ocean<T>(bg:ResourceImage<T>):AbstractBG<T>(bg) {
	/*SeaA = 0
For I = 0 To 29: Sea(I) = Rnd * 640: Next I*/
	private val rasterX = MutableList(31) {Random.nextFloat()*640}
	private val waveX = MutableList(31) {0f}
	/** Speed 0f-2f*/
	private var spdN = 0f
	private var waveH = 0f; private set
	private fun waveMag(i:Int) = (1.15f.pow(i)-spdN/2)*(1.4f-spdN*.2f)
	override fun update() {
		tick++
		if(tick>=180) tick = 0
		rasterX.forEachIndexed {i, it ->
			if(i==0) {
				rasterX[i] += spdN
				if(waveX[i]!=0f) waveX[i] = 0f
			} else {
				waveX[i] = sin((tick*2+(1.28f+spdN*0.01f).pow(32-i))*RG)*waveMag(i)
// R = Sea(I) + Sin((SeaA + 1.28 ^ (32 - I)) * Rg) * (1.15 ^ I) * (1 + (TrM = 0) * 0.4)
				rasterX[i] += (i-2.5f)*(spdN*.09f-.05f)
//				Sea(I) = Sea(I) + (I - 2.5) * (0.03 - (TrM > 0) * 0.08)
			}
			if(rasterX[i]>640) rasterX[i] -= 640f
			if(rasterX[i]<0) rasterX[i] += 640f
		}
		if(spdN!=speed)
			spdN = if(almostEqual(spdN, speed, .001f/100)) speed else Interpolation.lerp(spdN, speed, .05f)
	}

	override fun reset() {
		tick = 0
		rasterX.forEachIndexed {i, _ -> rasterX[i] = Random.nextFloat()*640}
		waveX.fill(0f)
		waveH = 0f
	}

	override fun draw() {
		rasterX.zip(waveX).forEachIndexed {i, (x, s) ->
			if(i==0) {
				//sky
				img.draw(0f, 0f, x, 0f, 640f, 240f)
				img.draw(640-x, 0f, 0f, 0f, x, 240f)
			} else {
				//ocean
				val i = i-1
				val waveH = minOf(maxOf(0f, spdN-.5f), 2f)
				val wy = i*8+((s/waveMag(i))-1)*i*waveH
				val x1 = (x+s).let {it+if(it<0) 640 else 0}%640
				img.draw(0f, 240f+i*8, x1, 240+wy, 640f, minOf(248+wy, 480f))
				img.draw(640-x1, 240f+i*8, 0f, 240+wy, x1, minOf(248+wy, 480f))
				/*
		With Src
			.Left = Sea(I): .Top = 240 + I * 5 + R: .Right = 640: .Bottom = .Top + 8
		End With
		If Not FS Then BBSf.BltFast 0, 240 + I * 8, BGSf, Src, DDBLTFAST_WAIT
		With Src
			.Left = 0: .Top = 240 + I * 5 + R: .Right = Sea(I): .Bottom = .Top + 8
		End With
		If Not FS Then BBSf.BltFast 640 - Sea(I), 240 + I * 8, BGSf, Src, DDBLTFAST_WAIT*/
			}
		}
	}

}
/*Case 0 '（海）
R = Sea(0)
If R > 640 Then R = R - 640 Else If R < 0 Then R = R + 640
With Src
	.Left = R: .Top = 0: .Right = 640: .Bottom = .Top + 240
End With
If Not FS Then BBSf.BltFast 0, 0, BGSf, Src, DDBLTFAST_WAIT
With Src
	.Left = 0: .Top = 0: .Right = R: .Bottom = .Top + 240
End With
If Not FS Then BBSf.BltFast 640 - R, 0, BGSf, Src, DDBLTFAST_WAIT
For I = 0 To 29
	If TrM < 2 Then
		R = Sea(I) + Sin((SeaA + 1.28 ^ (32 - I)) * Rg) * (1.15 ^ I) * (1 + (TrM = 0) * 0.4)
		If R > 640 Then R = R - 640 Else If R < 0 Then R = R + 640
		With Src
			.Left = R: .Top = 240 + I * 8: .Right = 640: .Bottom = .Top + 8
		End With
		If Not FS Then BBSf.BltFast 0, 240 + I * 8, BGSf, Src, DDBLTFAST_WAIT
		With Src
			.Left = 0: .Top = 240 + I * 8: .Right = R: .Bottom = .Top + 8
		End With
		If Not FS Then BBSf.BltFast 640 - R, 240 + I * 8, BGSf, Src, DDBLTFAST_WAIT
	Else
		R = Sin((SeaA + 1.3 ^ (32 - I)) * Rg) * (1.15 ^ I - 1)
		With Src
			.Left = Sea(I): .Top = 240 + I * 5 + R: .Right = 640: .Bottom = .Top + 8
		End With
		If Not FS Then BBSf.BltFast 0, 240 + I * 8, BGSf, Src, DDBLTFAST_WAIT
		With Src
			.Left = 0: .Top = 240 + I * 5 + R: .Right = Sea(I): .Bottom = .Top + 8
		End With
		If Not FS Then BBSf.BltFast 640 - Sea(I), 240 + I * 8, BGSf, Src, DDBLTFAST_WAIT
	End If
	Sea(I) = Sea(I) + (I - 2.5) * (0.03 - (TrM > 0) * 0.08)
	If Sea(I) > 640 Then Sea(I) = Sea(I) - 640 Else If Sea(I) < 0 Then Sea(I) = Sea(I) + 640
Next I
SeaA = SeaA + 2 + (SeaA >= 358) * 360
*/
