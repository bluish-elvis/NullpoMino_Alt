/*
 Copyright (c) 2021-2024, NullNoname
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

package mu.nu.nullpo.gui.slick.img

import mu.nu.nullpo.game.event.EventReceiver.COLOR
import mu.nu.nullpo.gui.common.BaseFontTTF
import mu.nu.nullpo.gui.slick.ResourceHolder
import org.newdawn.slick.Color

object FontTTF:BaseFontTTF {
	override fun print(x:Float, y:Float, str:String, color:COLOR, alpha:Float, size:Float) {
		ResourceHolder.ttfFont?.let {

			it.drawString(
				x, y, str, when(color) {
					COLOR.BLUE -> Color(0, 0, 255)
					COLOR.RED -> Color(255, 0, 0)
					COLOR.PINK -> Color(255, 128, 128)
					COLOR.GREEN -> Color(0, 255, 0)
					COLOR.YELLOW -> Color(255, 255, 0)
					COLOR.CYAN -> Color(0, 255, 255)
					COLOR.ORANGE -> Color(255, 128, 0)
					COLOR.PURPLE -> Color(255, 0, 255)
					COLOR.COBALT -> Color(0, 0, 128)
					else -> Color(255, 255, 255)
				}.apply {a = alpha})
		}
	}
}
