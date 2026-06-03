/*
 Copyright (c) 2022-2024, NullNoname
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

package mu.nu.nullpo.gui.common.fx

import mu.nu.nullpo.gui.common.AbstractRenderer

/** imgLine : del_h.png del_v.png */
sealed class Beam(x:Float, y:Float, val w:Int, val h:Int, alpha:Float = 1f, var angle:Float = 0f):
	SpriteSheet(x, y, alpha = alpha) {
	protected abstract val isV:Boolean

	class H(x:Float, y:Float, w:Int, h:Int, alpha:Float = 1f, angle:Float = 0f):Beam(x, y, w, h, alpha, angle) {
		override val isV:Boolean get() = false
	}

	class V(x:Float, y:Float, w:Int, h:Int, alpha:Float = 1f, angle:Float = 0f):Beam(x, y, w, h, alpha, angle) {
		override val isV:Boolean get() = true
	}
	override fun update(r:AbstractRenderer):Boolean = ++ticks>=16

	override fun draw(i:Int, r:AbstractRenderer) {
		r.drawBlendAdd {
			r.resources.imgLine[if(isV) 1 else 0].draw(x, y, dx2, dy2, srcX, srcY, srcX2, srcY2, alpha)
		}
	}

	override val dx2:Float get() = x+w
	override val dy2:Float get() = y+h
	override val srcX:Int get() = if(isV) (ticks-1)*8 else 0
	override val srcX2:Int get() = srcX+if(isV) 16 else 80
	override val srcY:Int get() = if(isV) 0 else (ticks/2-1)*8
	override val srcY2:Int get() = srcY+if(isV) 160 else 8
}
