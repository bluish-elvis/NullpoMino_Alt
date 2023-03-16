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
import mu.nu.nullpo.gui.common.libs.Vector
import kotlin.random.Random

class DTET06Texture<T>(bg:ResourceImage<T>):AbstractBG<T>(bg) {
	var x = Random.nextFloat()*640
	var y = Random.nextFloat()*480
	override fun update() {
		val v = Vector(16f, speed, true)
		x += v.x
		y += v.y
		if(x<0) x += 640
		if(x>=640) x -= 640
		if(y<0) y += 480
		if(y>=480) y -= 480
	}

	override fun reset() {
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

/*
Case 6 '（カーペット）
With CptG
If TrM = 0 Then .X = .X - 16: .Y = .Y + 1
If TrM = 1 Then .Y = .Y - 15
If TrM = 2 Then .X = .X - 16: .Y = .Y + 15
If .X < 0 Then .X = .X + 640
If .X >= 640 Then .X = .X - 640
If .Y < 0 Then .Y = .Y + 480
If .Y >= 480 Then .Y = .Y - 480
End With
With Src
.Left = CptG.X: .Top = CptG.Y: .Right = 640: .Bottom = 480
End With
If Not FS Then BBSf.BltFast 0, 0, BGSf, Src, DDBLTFAST_WAIT
With Src
.Left = 0: .Top = CptG.Y: .Right = CptG.X: .Bottom = 480
End With
If Not FS Then BBSf.BltFast 640 - CptG.X, 0, BGSf, Src, DDBLTFAST_WAIT
With Src
.Left = CptG.X: .Top = 0: .Right = 640: .Bottom = CptG.Y
End With
If Not FS Then BBSf.BltFast 0, 480 - CptG.Y, BGSf, Src, DDBLTFAST_WAIT
With Src
.Left = 0: .Top = 0: .Right = CptG.X: .Bottom = CptG.Y
End With
If Not FS Then BBSf.BltFast 640 - CptG.X, 480 - CptG.Y, BGSf, Src, DDBLTFAST_WAIT
*/
