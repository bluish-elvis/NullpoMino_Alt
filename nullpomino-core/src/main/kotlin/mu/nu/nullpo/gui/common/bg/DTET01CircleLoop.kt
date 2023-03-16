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
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class DTET01CircleLoop<T>(bg:ResourceImage<T>):AbstractBG<T>(bg) {
	/* (アルファベット)
	With ABG
	.R = Rnd * 360: .R2 = Rnd * 360: .X = Rnd * 640: .Y = Rnd * 480
	End With*/
	var r = Random.nextFloat()*360
	var r2 = Random.nextFloat()*360
	var x = Random.nextFloat()*640
	var y = Random.nextFloat()*480
	override fun update() {
		x += sin(r*Rg)*(2+speed*2.7f)
		y -= cos(r*Rg)*(2+speed*2.7f)
		if(x<0) x += 640
		if(x>=640) x -= 640
		if(y<0) y += 480
		if(y>=480) y -= 480
		r += .075f+speed*.007f
		if(speed>1) {
			r -= minOf(1f, speed-1)*.5f*(1+sin(r2*Rg)*.6f)
			if(r<0) r += 360
			if(r>=360) r -= 360
			r2 += (speed-1)*1.3f
			if(r2<0) r2 += 360
			if(r2>=360) r2 -= 360
		}

	}

	override fun reset() {
		r = Random.nextFloat()*360
		r2 = Random.nextFloat()*360
		x = Random.nextFloat()*640
		y = Random.nextFloat()*480
	}

	override fun draw() {
		img.draw(0f, 0f, x, y, 640f, 480f)
		img.draw(640-x, 0f, 0f, y, x, 480f)
		img.draw(0f, 480-y, x, 0f, 640f, y)
		img.draw(640-x, 480-y, 0f, 0f, x, y)

	}
}
/*Case 1 '（アルファベット）
With ABG
.X = .X + Sin(.R * Rg) * (3 + TrM * 5): .Y = .Y - Cos(.R * Rg) * (3 + TrM * 5)
If .X < 0 Then .X = .X + 640
If .X >= 640 Then .X = .X - 640
If .Y < 0 Then .Y = .Y + 480
If .Y >= 480 Then .Y = .Y - 480
.R = .R + 0.06 + TrM * 0.02 - (TrM >= 2) * (1 + Sin(.R2 * Rg)) * 0.6
If .R >= 360 Then .R = .R - 360
If .R < 0 Then .R = .R + 360
If TrM >= 2 Then .R2 = .R2 + 2.2: If .R2 >= 360 Then .R2 = .R2 - 360
End With
With Src
.Left = ABG.X: .Top = ABG.Y: .Right = 640: .Bottom = 480
End With
If Not FS Then BBSf.BltFast 0, 0, BGSf, Src, DDBLTFAST_WAIT
With Src
.Left = 0: .Top = ABG.Y: .Right = ABG.X: .Bottom = 480
End With
If Not FS Then BBSf.BltFast 640 - ABG.X, 0, BGSf, Src, DDBLTFAST_WAIT
With Src
.Left = ABG.X: .Top = 0: .Right = 640: .Bottom = ABG.Y
End With
If Not FS Then BBSf.BltFast 0, 480 - ABG.Y, BGSf, Src, DDBLTFAST_WAIT
With Src
.Left = 0: .Top = 0: .Right = ABG.X: .Bottom = ABG.Y
End With
If Not FS Then BBSf.BltFast 640 - ABG.X, 480 - ABG.Y, BGSf, Src, DDBLTFAST_WAIT

'For I = 0 To 77
'With A(I)
'If .X < 100 Then .XXX = 0.1
'If .X > 524 Then .XXX = -0.1
'If .Y < 100 Then .YYY = 0.1
'If .Y > 364 Then .YYY = -0.1
'.XX = .XX + .XXX: .YY = .YY + .YYY
'If Abs(.XX) > .MXX Then .XX = .MXX * Sgn(.XX): .XXX = 0
'If Abs(.YY) > .MYY Then .YY = .MYY * Sgn(.YY): .YYY = 0
'.X = .X + .XX: .Y = .Y + .YY
'End With
'With Src
'.Left = 256 + (I Mod 8) * 16: .Top = Int(I / 8) * 16: .Right = .Left + 16: .Bottom = .Top + 16
'End With
'BltClip A(I).X, A(I).Y, SpSf, Src
'Next I
*/
