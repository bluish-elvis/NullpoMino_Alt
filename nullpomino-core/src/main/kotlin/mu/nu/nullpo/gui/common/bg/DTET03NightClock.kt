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

class DTET03NightClock<T>(img:ResourceImage<T>, private val bg:Boolean = true):AbstractBG<T>(img) {
	/*'（キラキラ振り子）
FSX = Rnd * 640
FC = 140: FX = 0: FY = 0
KrI = 0
For I = 0 To 35
With Kr(I)
.V = False
End With
Next I*/
	private class Frag(val id:Int) {
		var x = Random.nextFloat()*704-32; private set
		var y = Random.nextFloat()*544-32; private set
		private var vx = 0f
		private var vy = 0f
		var tick = -1; private set
		fun update() {
			if(tick<0) return
			tick++
			if(tick>=6) tick -= 6
			vy -= .1f
			x += vx
			y += vy
			/* For I = 0 To 35
			 * With Kr(I)
			 * If .V Then
			 * .A = .A + 1 + (.A >= 5) * 6
			 * .YY = .YY - 0.1
			 * .X = .X + .XX: .Y = .Y + .YY
			 * End If
			 * End With
			 * Next I*/
		}

		fun reset(fx:Float, fy:Float) {
			tick = 0
			val r1 = Random.nextFloat()*360*RG
			val r2 = Random.nextFloat()*64
			x = 312+fx*540+sin(r1)*r2
			y = -188+fy*540+cos(r1)*r2
			vx = Random.nextFloat()*8-4
			vy = Random.nextFloat()*8-4
			/*
			KrI = KrI + 1 + (KrI = 35) * 36
	With Kr(KrI)
	.V = True: .A = 0: R1 = Rnd * 360: R2 = Rnd * 64
	.X = 312 + FX * 540 + Sin(R1 * Rg) * R2: .Y = -188 + FY * 540 + Cos(R1 * Rg) * R2
	.XX = -4 + Rnd * 8: .YY = -4 + Rnd * 8
	End With
			*/
		}
	}

	private val children = List(36) {Frag(it)}
	private var bx = Random.nextFloat()*640
	override var tick = 140
	private var fx = 0f; private set
	private var fy = 0f; private set

	override fun reset() {
		bx = Random.nextFloat()*640
		tick = 0
	}

	override fun update() {
		bx -= 4+speed
		tick++
		if(bx<0) bx += 640
		if(tick>=180) tick -= 180
		val swing = sin(tick*2*RG)
		fx = sin(swing*35*RG)
		fy = cos(swing*35*RG)
		children.forEach {it.update()}
		children[tick%children.size].reset(fx, fy)
		/*FC = FC + 2 + (FC >= 358) * 360
FX = Sin(Sin(FC * Rg) * 35 * Rg): FY = Cos(Sin(FC * Rg) * 35 * Rg)
KrI = KrI + 1 + (KrI = 35) * 36*/
	}

	override fun draw(render:AbstractRenderer) {
		if(bg) {
			img.draw(0f, 0f, bx, 0f, 640f, 240f)
			img.draw(0f, 240f, bx, 0f, 640f, 240f)
			img.draw(640-bx, 0f, 0f, 0f, bx, 240f)
			img.draw(640-bx, 240f, 0f, 0f, bx, 240f)
		}
		children.filter {it.tick>=0}.forEach {
			val sx = 16f+it.tick*16
			img.draw(it.x+Random.nextFloat()*20-10, it.y+Random.nextFloat()*20-10, sx, 368f, sx+16, 384f)
			/*
			 * For I = 0 To 35
			 * If Kr(I).V Then
			 * With Src
			 * .Left = 16 + Kr(I).A * 16: .Top = 368: .Right = .Left + 16: .Bottom = .Top + 16
			 * End With
			 * BltClip Kr(I).X + (Rnd * 20 - 10) * TrM, Kr(I).Y + (Rnd * 20 - 10) * TrM, BGSf, Src, DDBLTFAST_SRCCOLORKEY Or DDBLTFAST_WAIT
			 * End If
			 */

		}
		for(it in (11..30)+(38..50)) {
			val ch = it*16-bx/4%16
			img.draw(312+fx*ch, -188+fy*ch, 0f, 368f, 16f, 384f)
		}
		img.draw(256+fx*540, -244+fy*540, 0f, 240f, 128f, 368f)
		/*For I = 11 To 46 + (TrM < 2) * 4
If I < 31 Or I > 37 Then
With Src
.Left = 0: .Top = 368: .Right = .Left + 16: .Bottom = .Top + 16
End With
BltClip 312 + FX * (I * 16 + (TrM = 2) * (FC Mod 15)), -188 + FY * (I * 16 + (TrM = 2) * (FC Mod 15)), BGSf, Src, DDBLTFAST_SRCCOLORKEY Or DDBLTFAST_WAIT
End If
Next I
With Src
.Left = 0: .Top = 240: .Right = .Left + 128: .Bottom = .Top + 128
End With
BltClip 256 + FX * 540, -244 + FY * 540, BGSf, Src, DDBLTFAST_SRCCOLORKEY Or DDBLTFAST_WAIT*/

	}

	override fun drawLite() {
		img.draw(0f, 0f, 0f, 0f, 640f, 240f)
		img.draw(0f, 240f, 0f, 0f, 640f, 240f)
	}
}

/*Case 3 '（キラキラ振り子）
FSX = FSX - 5: If FSX < 0 Then FSX = FSX + 640
With Src
.Left = FSX: .Top = 0: .Right = 640: .Bottom = 240
End With
If Not FS Then BBSf.BltFast 0, 0, BGSf, Src, DDBLTFAST_WAIT
If Not FS Then BBSf.BltFast 0, 240, BGSf, Src, DDBLTFAST_WAIT
With Src
.Left = 0: .Top = 0: .Right = FSX: .Bottom = 240
End With
If Not FS Then BBSf.BltFast 640 - FSX, 0, BGSf, Src, DDBLTFAST_WAIT
If Not FS Then BBSf.BltFast 640 - FSX, 240, BGSf, Src, DDBLTFAST_WAIT
For I = 0 To 35
With Kr(I)
If .V Then
.A = .A + 1 + (.A >= 5) * 6
.YY = .YY - 0.1
.X = .X + .XX: .Y = .Y + .YY
End If
End With
Next I
For I = 0 To 35
If Kr(I).V Then
With Src
.Left = 16 + Kr(I).A * 16: .Top = 368: .Right = .Left + 16: .Bottom = .Top + 16
End With
BltClip Kr(I).X + (Rnd * 20 - 10) * TrM, Kr(I).Y + (Rnd * 20 - 10) * TrM, BGSf, Src, DDBLTFAST_SRCCOLORKEY Or DDBLTFAST_WAIT
End If
Next I
FC = FC + 2 + (FC >= 358) * 360
FX = Sin(Sin(FC * Rg) * 35 * Rg): FY = Cos(Sin(FC * Rg) * 35 * Rg)

For I = 11 To 46 + (TrM < 2) * 4
If I < 31 Or I > 37 Then
With Src
.Left = 0: .Top = 368: .Right = .Left + 16: .Bottom = .Top + 16
End With
BltClip 312 + FX * (I * 16 + (TrM = 2) * (FC Mod 15)), -188 + FY * (I * 16 + (TrM = 2) * (FC Mod 15)), BGSf, Src, DDBLTFAST_SRCCOLORKEY Or DDBLTFAST_WAIT
End If
Next I
With Src
.Left = 0: .Top = 240: .Right = .Left + 128: .Bottom = .Top + 128
End With
BltClip 256 + FX * 540, -244 + FY * 540, BGSf, Src, DDBLTFAST_SRCCOLORKEY Or DDBLTFAST_WAIT*/
