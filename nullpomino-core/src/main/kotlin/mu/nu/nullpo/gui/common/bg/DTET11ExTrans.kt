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

class DTET11ExTrans<T>(bg:ResourceImage<T>):AbstractBG<T>(bg) {
	companion object {
		var TTick = 0
			set(value) {
				field = value%80
			}
	}

	override fun update() {
		tick = ++TTick
	}

	override fun reset() {
		tick = 0
	}

	override fun draw(render: AbstractRenderer) {
		val x = minOf(640f, (speed.toInt()*40f*((8-tick*3)%8)).let {it+if(it<0) 640 else 0}%640)
		val y = minOf(480f, ((1-speed)*tick*5).let {it+if(it<0) 480 else 0}%480)
		/*With LdG
.X = TrM * 40 * (8 - ((FAC * 3) Mod 8))
.Y = (1 - TrM) * FAC * 5: If .Y < 0 Then .Y = .Y + 480
End With*/
		img.draw(0f, -80f, x, y, 640f, 320f)
		img.draw(0f, 240f, x, y, 640f, 320f)
		img.draw(640-x, -80f, 0f, y, x, 320f)
		img.draw(640-x, 240f, 0f, y, x, 320f)
		img.draw(0f, 240-y, x, 0f, 640f, y)
		img.draw(0f, 560-y, x, 0f, 640f, y)
		img.draw(640-x, 240-y, 0f, 0f, x, y)
		img.draw(640-x, 560-y, 0f, 0f, x, y)
	}
}
/*Case 13 'レベルMAX
With Src
.Left = LdG.X: .Top = LdG.Y: .Right = 640: .Bottom = 320
End With
BltClip 0, -80, BGSf, Src, DDBLTFAST_WAIT
BltClip 0, 240, BGSf, Src, DDBLTFAST_WAIT
With Src
.Left = 0: .Top = LdG.Y: .Right = LdG.X: .Bottom = 320
End With
BltClip 640 - LdG.X, -80, BGSf, Src, DDBLTFAST_WAIT
BltClip 640 - LdG.X, 240, BGSf, Src, DDBLTFAST_WAIT
With Src
.Left = LdG.X: .Top = 0: .Right = 640: .Bottom = LdG.Y
End With
BltClip 0, 240 - LdG.Y, BGSf, Src, DDBLTFAST_WAIT
BltClip 0, 560 - LdG.Y, BGSf, Src, DDBLTFAST_WAIT
With Src
.Left = 0: .Top = 0: .Right = LdG.X: .Bottom = LdG.Y
End With
BltClip 640 - LdG.X, 240 - LdG.Y, BGSf, Src, DDBLTFAST_WAIT
BltClip 640 - LdG.X, 560 - LdG.Y, BGSf, Src, DDBLTFAST_WAIT
*/
