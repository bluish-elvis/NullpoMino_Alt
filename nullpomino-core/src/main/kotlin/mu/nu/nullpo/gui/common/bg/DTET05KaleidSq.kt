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
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class DTET05KaleidSq<T>(bg:ResourceImage<T>):AbstractBG<T>(bg) {
	/*（ダイヤ）
With Dia
DiaR = Rnd * 360: .X = Rnd * 640: .Y = Rnd * 480
End With*/
	override var speed:Float = 1f
		set(value) {
			field = value
			r = speed*3
		}
	private var r = Random.nextFloat()*360
	private var x = Random.nextFloat()*640
	private var y = Random.nextFloat()*480
	override fun update() {
		val vel = 6.6f*(1+speed/2)
		x += sin(r)*vel
		y += cos(r)*vel
		while(x<0) x += 320
		while(x>=320) x -= 320
		while(y<0) y += 240
		while(y>=240) y -= 240
		if(speed>1) {
			r += .0025f+(speed-1)*RG/12
			while(r>=360) r -= 360
		}
	}

	override fun reset() {
		r = Random.nextFloat()*360
		x = Random.nextFloat()*640
		y = Random.nextFloat()*480
	}

	override fun draw(render: AbstractRenderer) {
		img.draw(0f, 0f, x, y, 320f, 240f)
		img.draw(320-x, 0f, 0f, y, x, 240f)
		img.draw(0f, 240-y, x, 0f, 320f, y)
		img.draw(320-x, 240-y, 0f, 0f, x, y)

		img.draw(320f, 0f, 640-x, y, 640f, 240f)
		img.draw(320+x, 0f, 320f, y, 640-x, 240f)
		img.draw(320f, 240-y, 640-x, 0f, 640f, y)
		img.draw(320+x, 240-y, 320f, 0f, 640-x, y)

		img.draw(0f, 240f, x, 480-y, 320f, 480f)
		img.draw(320-x, 240f, 0f, 480-y, x, 480f)
		img.draw(0f, 240+y, x, 240f, 320f, 480-y)
		img.draw(320-x, 240+y, 0f, 240f, x, 480-y)

		img.draw(320f, 240f, 640-x, 480-y, 640f, 480f)
		img.draw(320+x, 240f, 320f, 480-y, 640-x, 480f)
		img.draw(320f, 240+y, 640-x, 240f, 640f, 480-y)
		img.draw(320+x, 240+y, 320f, 240f, 640-x, 480-y)
	}
}
/*Case 5 '（ダイヤ）
With Dia
	If TrM = 0 Then .X = .X - 8: .Y = .Y + 8
	If TrM = 1 Then .X = .X + 12: .Y = .Y - 12
	If TrM = 2 Then .X = .X + Sin(DiaR * Rg) * 10: .Y = .Y - Cos(DiaR * Rg) * 10
	If .X < 0 Then .X = .X + 320
	If .X >= 320 Then .X = .X - 320
	If .Y < 0 Then .Y = .Y + 240
	If .Y >= 240 Then .Y = .Y - 240
	If TrM = 2 Then DiaR = DiaR + 0.2: If DiaR >= 360 Then DiaR = DiaR - 360
End With
With Src
.Left = Dia.X: .Top = Dia.Y: .Right = 320: .Bottom = 240
End With
If Not FS Then BBSf.BltFast 0, 0, BGSf, Src, DDBLTFAST_WAIT
With Src
.Left = 0: .Top = Dia.Y: .Right = Dia.X: .Bottom = 240
End With
If Not FS Then BBSf.BltFast 320 - Dia.X, 0, BGSf, Src, DDBLTFAST_WAIT
With Src
.Left = Dia.X: .Top = 0: .Right = 320: .Bottom = Dia.Y
End With
If Not FS Then BBSf.BltFast 0, 240 - Dia.Y, BGSf, Src, DDBLTFAST_WAIT
With Src
.Left = 0: .Top = 0: .Right = Dia.X: .Bottom = Dia.Y
End With
If Not FS Then BBSf.BltFast 320 - Dia.X, 240 - Dia.Y, BGSf, Src, DDBLTFAST_WAIT

With Src
.Left = 640 - Dia.X: .Top = Dia.Y: .Right = 640: .Bottom = 240
End With
If Not FS Then BBSf.BltFast 320, 0, BGSf, Src, DDBLTFAST_WAIT
With Src
.Left = 320: .Top = Dia.Y: .Right = 640 - Dia.X: .Bottom = 240
End With
If Not FS Then BBSf.BltFast 320 + Dia.X, 0, BGSf, Src, DDBLTFAST_WAIT
With Src
.Left = 640 - Dia.X: .Top = 0: .Right = 640: .Bottom = Dia.Y
End With
If Not FS Then BBSf.BltFast 320, 240 - Dia.Y, BGSf, Src, DDBLTFAST_WAIT
With Src
.Left = 320: .Top = 0: .Right = 640 - Dia.X: .Bottom = Dia.Y
End With
If Not FS Then BBSf.BltFast 320 + Dia.X, 240 - Dia.Y, BGSf, Src, DDBLTFAST_WAIT

With Src
.Left = Dia.X: .Top = 480 - Dia.Y: .Right = 320: .Bottom = 480
End With
If Not FS Then BBSf.BltFast 0, 240, BGSf, Src, DDBLTFAST_WAIT
With Src
.Left = 0: .Top = 480 - Dia.Y: .Right = Dia.X: .Bottom = 480
End With
If Not FS Then BBSf.BltFast 320 - Dia.X, 240, BGSf, Src, DDBLTFAST_WAIT
With Src
.Left = Dia.X: .Top = 240: .Right = 320: .Bottom = 480 - Dia.Y
End With
If Not FS Then BBSf.BltFast 0, 240 + Dia.Y, BGSf, Src, DDBLTFAST_WAIT
With Src
.Left = 0: .Top = 240: .Right = Dia.X: .Bottom = 480 - Dia.Y
End With
If Not FS Then BBSf.BltFast 320 - Dia.X, 240 + Dia.Y, BGSf, Src, DDBLTFAST_WAIT

With Src
.Left = 640 - Dia.X: .Top = 480 - Dia.Y: .Right = 640: .Bottom = 480
End With
If Not FS Then BBSf.BltFast 320, 240, BGSf, Src, DDBLTFAST_WAIT
With Src
.Left = 320: .Top = 480 - Dia.Y: .Right = 640 - Dia.X: .Bottom = 480
End With
If Not FS Then BBSf.BltFast 320 + Dia.X, 240, BGSf, Src, DDBLTFAST_WAIT
With Src
.Left = 640 - Dia.X: .Top = 240: .Right = 640: .Bottom = 480 - Dia.Y
End With
If Not FS Then BBSf.BltFast 320, 240 + Dia.Y, BGSf, Src, DDBLTFAST_WAIT
With Src
.Left = 320: .Top = 240: .Right = 640 - Dia.X: .Bottom = 480 - Dia.Y
End With
If Not FS Then BBSf.BltFast 320 + Dia.X, 240 + Dia.Y, BGSf, Src, DDBLTFAST_WAIT
*/
