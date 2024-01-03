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

class DTET12Rush<T>(bg:ResourceImage<T>):AbstractBG<T>(bg) {
	private var tickY = 0f
	override fun update() {
		tick -= 73
		if(tick<0) tick += 640
		tickY -= 72+(speed)
		if(tickY<0) tickY += 480
	}

	override fun reset() {
		tick = 0
		tickY = 0f
	}

	override fun draw(render: AbstractRenderer) {
		val tickX = tick.toFloat()
		img.draw(0f, 0f, tickX, tickY, 640f, 480f)
		img.draw(640f-tickX, 0f, 0f, tickY, tickX, 480f)
		img.draw(0f, 480f-tickY, tickX, 0f, 640f, tickY)
		img.draw(640f-tickX, 480f-tickY, 0f, 0f, tickX, tickY)
	}
}
/*Case 11 'レベル200（電流）
With EdG
.X = .X - 73: If .X < 0 Then .X = .X + 640
.Y = .Y - 72 - TA * 6: If .Y < 0 Then .Y = .Y + 480
End With
With Src
.Left = EdG.X: .Top = EdG.Y: .Right = 640: .Bottom = 480
End With
If Not FS Then BBSf.BltFast 0, 0, BGSf, Src, DDBLTFAST_WAIT
With Src
.Left = 0: .Top = EdG.Y: .Right = EdG.X: .Bottom = 480
End With
If Not FS Then BBSf.BltFast 640 - EdG.X, 0, BGSf, Src, DDBLTFAST_WAIT
With Src
.Left = EdG.X: .Top = 0: .Right = 640: .Bottom = EdG.Y
End With
If Not FS Then BBSf.BltFast 0, 480 - EdG.Y, BGSf, Src, DDBLTFAST_WAIT
With Src
.Left = 0: .Top = 0: .Right = EdG.X: .Bottom = EdG.Y
End With
If Not FS Then BBSf.BltFast 640 - EdG.X, 480 - EdG.Y, BGSf, Src, DDBLTFAST_WAIT
*/
