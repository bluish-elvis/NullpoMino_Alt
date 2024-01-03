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

package mu.nu.nullpo.gui.common.bg.dtet

import kotlin.random.Random

class BGAIMist<T>(bg:mu.nu.nullpo.gui.common.ResourceImage<T>):mu.nu.nullpo.gui.common.bg.AbstractBG<T>(bg) {
	/*'（スターダスト）
For I = 0 To 59: StD(I) = Rnd * 152: Next I*/
	private val py = MutableList(60) {Random.nextFloat()*152}
	override fun update() {
		py.forEachIndexed {i, _ ->
			py[i] += (i*.1f-2.95f)*(.5f+speed)
			if(py[i]<0) py[i] += 152f
			if(py[i]>=152) py[i] -= 152f
		}
	}

	override fun reset() {
		py.forEachIndexed {i, _ -> py[i] = Random.nextFloat()*152}
	}

	override fun draw(render: mu.nu.nullpo.gui.common.AbstractRenderer) {
		py.forEachIndexed {i, it ->
			val sy = (i%3)*160+it
			img.draw(0f, i*8f, 0f, sy, 640f, sy+8)
		}
	}
}

/*Case 8 '（スターダスト）
For I = 0 To 59
StD(I) = StD(I) + (I * 0.1 - 2.95) * (1 + (TrM >= 2) * 2)
If StD(I) < 0 Then StD(I) = StD(I) + 152
If StD(I) >= 152 Then StD(I) = StD(I) - 152
With Src
.Left = 0: .Top = (I Mod 3) * 160 + StD(I): .Right = .Left + 640: .Bottom = .Top + 8
End With
If Not FS Then BBSf.BltFast 0, I * 8, BGSf, Src, DDBLTFAST_WAIT
Next I

'With Src
'.Left = 0: .Top = 0: .Right = .Left + 640: .Bottom = .Top + 480
'End With
'If Not FS Then BBSf.BltFast 0, 0, BGSf, Src, DDBLTFAST_WAIT*/
