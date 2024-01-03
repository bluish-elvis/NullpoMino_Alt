/*
 Copyright (c) 2023-2024, NullNoname
 All rights reserved.

 Converted to Kotlin and modified by Venom_Nhelv as bluish-elvis
 THIS IS NOT MADE IN ASSOCIATION WITH THE GAME CREATOR.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions are met:
     * Redistributions of source code must retain the above copyright
       notice, this list of conditions and the following disclaimer.
     * Redistributions in binary form must reproduce the above copyright
       notice, this list of conditions and the following disclaimer in the
       documentation and/or other materials provided with the distribution.
     * Neither the name of NullNoname nor the names of its
       contributors may be used to endorse or promote products derived from
       this software without specific prior written permission.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 POSSIBILITY OF SUCH DAMAGE.
 */

package mu.nu.nullpo.gui.common.bg

import mu.nu.nullpo.gui.common.AbstractRenderer
import mu.nu.nullpo.gui.common.ResourceImage
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class DTET07Beams<T>(res:ResourceImage<T>, private val bg:Boolean = true):AbstractBG<T>(res) {
	/*'（レーザー）
LsrSY = Rnd * 240
For I = 0 To 59
If (I Mod 3) = 0 Then
With Lsr(I)
.C = (Int(I / 2) Mod 2): .X = Rnd * 704 - 32: .Y = Rnd * 544 - 32: .r = I * 2 + 1: .RR = 1 - (I Mod 2) * 2
End With
Else
Lsr(I) = Lsr(Int(I / 3) * 3)
End If
Next I*/
	private class Beam(val id:Int, var speed:Float) {

		var x = Random.nextFloat()*704-32; private set
		var y = Random.nextFloat()*544-32; private set
		var r = id*2+1f; private set

		var c = id/2%2; private set
		var vr = (id%5-2)/5f; private set

		data class Stat(var x:Float, var y:Float, var r:Float) {
			fun replace(v:Stat) {
				x = v.x;y = v.y;r = v.r
			}
		}

		var t = List(2) {Stat(x, y, r)}
		fun update() {
			t[1].replace(t[0])
			t[0].let {it.x = x;it.y = y;it.r = r}
			val vel = 10+4*speed
			x += sin(r*3*RG)*vel
			y -= cos(r*3*RG)*vel
			r += vr*speed*minOf(1+speed/10, speed)
			while(r<0) r += 360
			while(r>=360) r -= 360
			fun refl() {
				vr = -vr;c = 1-c
			}
			while(x<-32) {
				x += 672;refl()
			};while(x>=640) {
				x -= 672;refl()
			}
			while(y<-32) {
				y += 512;refl()
			};while(y>=480) {
				y -= 512;refl()
			}
			/*With Lsr(I)
	If TrM = 2 Then .r = .r + .RR
	If .r < 0 Then .r = .r + 360
	If .r >= 360 Then .r = .r - 360
	.X = .X + Sin(.r * 3 * Rg) * 18: .Y = .Y - Cos(.r * 3 * Rg) * 18
	If .X < -32 Then .X = .X + 672: .RR = -.RR: .C = 1 - .C
	If .X >= 640 Then .X = .X - 672: .RR = -.RR: .C = 1 - .C
	If .Y < -32 Then .Y = .Y + 512: .RR = -.RR: .C = 1 - .C
	If .Y >= 480 Then .Y = .Y - 512: .RR = -.RR: .C = 1 - .C
	End With*/
		}

		fun reset() {
			x = Random.nextFloat()*704-32
			y = Random.nextFloat()*544-32
			r = id*2+1f
			c = id/2%2
			vr = (id%5-2)/5f
			t.forEach {it.x = x;it.y = y;it.r = r}
		}
	}

	override var speed:Float = 1f
		set(value) {
			field = value
			children.forEach {it.speed = value}
		}
	private val children = List(20) {Beam(it, speed)}
	private var by = Random.nextFloat()*240
	override var tick
		get() = (240-by).toInt()
		set(value) {
			by = (240-value%240).toFloat()
		}

	override fun update() {
		by -= 4+speed*3
		if(by<0) by += 240
		children.forEach {it.update()}
	}

	override fun reset() {
		by = Random.nextFloat()*240
		children.forEach {it.reset()}
	}

	override fun draw(render:AbstractRenderer) {
		if(bg) {
			img.draw(0f, 0f, 0f, 480-by, 640f, 480f)
			img.draw(0f, by, 0f, 240f, 640f, 480f)
			img.draw(0f, 240+by, 0f, 240f, 640f, 480-by)
		}
		/*LsrSY = LsrSY - 4 - TrM * 3: If LsrSY < 0 Then LsrSY = LsrSY + 240
		With Src
			.Left = 0: .Top = 480 - LsrSY: .Right = 640: .Bottom = 480
		End With
		If Not FS Then BBSf.BltFast 0, 0, BGSf, Src, DDBLTFAST_WAIT
		With Src
			.Left = 0: .Top = 240: .Right = 640: .Bottom = 480
		End With
		If Not FS Then BBSf.BltFast 0, LsrSY, BGSf, Src, DDBLTFAST_WAIT
		With Src
			.Left = 0: .Top = 240: .Right = 640: .Bottom = 480 - LsrSY
		End With
		If Not FS Then BBSf.BltFast 0, 240 + LsrSY, BGSf, Src, DDBLTFAST_WAIT
		*/
		children.forEach {
			val x = it.x
			val y = it.y
			val r = it.r.toInt()
			val sx = (r%20)*32f
			val sy = it.c*96f+(r/20%3)*32
			it.t.reversed().forEach {(x1, y1, r1) ->
				val tr = r1.toInt()
				val tsx = (tr%20)*32f
				val tsy = it.c*96f+(tr/20%3)*32
				img.draw(x1, y1, tsx, tsy, tsx+32, tsy+32)
			}
			img.draw(x, y, sx, sy, sx+32, sy+32)
		}
	}

	override fun drawLite() {
		img.draw(0f, 0f, 0f, 240f, 640f, 480f)
		img.draw(0f, 240f, 0f, 240f, 640f, 480f)
	}
}
/*Case 7 '（レーザー）

For I = 0 To 59
	If (I Mod 3) < 2 Then
	Lsr(I) = Lsr(I + 1)
	Else
	With Lsr(I)
	If TrM = 2 Then .r = .r + .RR
	If .r < 0 Then .r = .r + 360
	If .r >= 360 Then .r = .r - 360
	.X = .X + Sin(.r * 3 * Rg) * 18: .Y = .Y - Cos(.r * 3 * Rg) * 18
	If .X < -32 Then .X = .X + 672: .RR = -.RR: .C = 1 - .C
	If .X >= 640 Then .X = .X - 672: .RR = -.RR: .C = 1 - .C
	If .Y < -32 Then .Y = .Y + 512: .RR = -.RR: .C = 1 - .C
	If .Y >= 480 Then .Y = .Y - 512: .RR = -.RR: .C = 1 - .C
	End With
	End If
	With Src
		.Left = (Lsr(I).r Mod 20) * 32: .Top = Lsr(I).C * 96 + (Int(Lsr(I).r / 20) Mod 3) * 32: .Right = .Left + 32: .Bottom = .Top + 32
	End With
	BltClip Lsr(I).X, Lsr(I).Y, BGSf, Src, DDBLTFAST_SRCCOLORKEY Or DDBLTFAST_WAIT
Next I
*/
