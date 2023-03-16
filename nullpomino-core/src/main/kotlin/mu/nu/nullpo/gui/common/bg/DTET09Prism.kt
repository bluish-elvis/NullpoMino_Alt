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
import kotlin.math.sign
import kotlin.random.Random

class DTET09Prism<T>(bg:ResourceImage<T>):AbstractBG<T>(bg) {
	private val children = List(15) {Prism(it, speed)}
	override var speed:Float = 1f
		set(value) {
			field = value
			children.forEach {it.speed = value}
		}
	var by = Random.nextFloat()*240
	override var tick
		get() = (240-by).toInt()
		set(value) {
			by = (240-value%240).toFloat()
		}

	override fun update() {
		by--
		if(by<0) by += 240
		children.forEach {it.update()}
	}

	override fun reset() {
		by = Random.nextFloat()*240
		children.forEach {it.init()}
	}

	override fun draw() {
		img.draw(0f, 0f, 0f, 240-by, 640f, 240f)
		img.draw(0f, 0+by, 0f, 0f, 640f, 240f)
		img.draw(0f, 240+by, 0f, 0f, 640f, 240-by)
		/*
With Src
	.Left = 0: .Top = 240 - CrysSY: .Right = 640: .Bottom = 240
End With
If Not FS Then BBSf.BltFast 0, 0, BGSf, Src, DDBLTFAST_WAIT
With Src
	.Left = 0: .Top = 0: .Right = 640: .Bottom = 240
End With
If Not FS Then BBSf.BltFast 0, CrysSY, BGSf, Src, DDBLTFAST_WAIT
With Src
	.Left = 0: .Top = 0: .Right = 640: .Bottom = 240 - CrysSY
End With
If Not FS Then BBSf.BltFast 0, 240 + CrysSY, BGSf, Src, DDBLTFAST_WAIT*/
		children.forEach {
			val sx = it.tick%8*80f
			val sy = 240f+(it.tick/8*80)
			img.draw(it.x, it.y, sx, sy, sx+80, sy+80)
		}
	}

	private class Prism(val id:Int, var speed:Float) {
		var x = 0f
		var y = 0f
		var vy = 0f
		var tick = 0
		var vt = 0

		init {
			init()
		}

		fun init() {
			val spd = 1+speed*(id%2)
			tick = Random.nextInt(20)
			val dir = Random.nextInt(2)
			x = Random.nextFloat()*800-80
			y = dir*639f-80
			vt = Random.nextInt(1, (2+spd).toInt())*if(dir>0) -1 else 1
			vy = vt*4f+(Random.nextFloat()*3)*vt.sign
		}

		fun update() {
			tick += vt
			y += vy
			if(tick>=20) tick -= 20
			if(tick<=0) tick += 20
			if(y<-80||y>=560) init()

		}
	}
}

/*
'（水晶）
CrysSY = Rnd * 240
For I = 0 To 15
With Crys(I)
	.A = Int(Rnd * 20)
	.AA = (1 + Int(Rnd * 2)) * (1 + (TrM = 2) * (I Mod 2) * 2)
	.X = Int(Rnd * 800) - 80
	.Y = Rnd * 640 - 80
	.YY = .AA * 4 + Rnd * 4 * (1 + (TrM = 2) * (I Mod 2) * 2)
End With
Next I

Case 9 '（水晶）
CrysSY = CrysSY - 1: If CrysSY < 0 Then CrysSY = CrysSY + 240
With Src
	.Left = 0: .Top = 240 - CrysSY: .Right = 640: .Bottom = 240
End With
If Not FS Then BBSf.BltFast 0, 0, BGSf, Src, DDBLTFAST_WAIT
With Src
	.Left = 0: .Top = 0: .Right = 640: .Bottom = 240
End With
If Not FS Then BBSf.BltFast 0, CrysSY, BGSf, Src, DDBLTFAST_WAIT
With Src
	.Left = 0: .Top = 0: .Right = 640: .Bottom = 240 - CrysSY
End With
If Not FS Then BBSf.BltFast 0, 240 + CrysSY, BGSf, Src, DDBLTFAST_WAIT
For I = 0 To 15
	With Crys(I)
		.A = .A + .AA
		If .A >= 20 Then .A = .A - 20
		If .A < 0 Then .A = .A + 20
		.Y = .Y + .YY
		If .Y < -80 Or .Y >= 560 Then
			.A = Int(Rnd * 20)
			.AA = (1 + Int(Rnd * 2)) * (1 + (TrM = 2) * (I Mod 2) * 2)
			.X = Int(Rnd * 800) - 80
			.Y = Rnd * 80 - 160 - (TrM = 2) * (I Mod 2) * 640
			.YY = .AA * 4 + Rnd * 4 * (1 + (TrM = 2) * (I Mod 2) * 2)
		End If
	End With
	With Src
		.Left = (Crys(I).A Mod 8) * 80: .Top = 240 + Int(Crys(I).A / 8) * 80: .Right = .Left + 80: .Bottom = .Top + 80
	End With
	BltClip Crys(I).X, Crys(I).Y, BGSf, Src, DDBLTFAST_SRCCOLORKEY Or DDBLTFAST_WAIT
Next I*/
