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

import mu.nu.nullpo.game.event.EventReceiver.COLOR
import mu.nu.nullpo.gui.common.AbstractRenderer
import mu.nu.nullpo.gui.common.BaseFont.FONT
import mu.nu.nullpo.gui.common.BaseFontNano

class PopupPoint(x:Int, y:Int, val pts:Int, val str:String, private val c:Int, private val big:Boolean = false)
	:SpriteSheet(x, y) {
	val color get() = if(c==COLOR.RAINBOW.ordinal) COLOR.getRainbowColor(ticks).ordinal else c
	override var alpha
		get() = minOf(1f, 2f-ticks/36f)
		set(v) {}

	override fun update(r:AbstractRenderer):Boolean {
		if(dx<20) x += -(dx-20)*.1f
		y += vel.y
		vel.y = maxOf(-.75f, .5f-ticks*0.03f)
		return ++ticks>=72||pts==0
	}

	override fun draw(i:Int, r:AbstractRenderer) {
		val (sPtr, col) =
			if(pts>0) ("+${pts}" to if(ticks/2%2==0) COLOR.WHITE else COLOR.all[color]) else ("-${str}" to COLOR.RED)
		r.drawFont(dx.toInt(), dy.toInt(), sPtr, FONT.NUM, col, if(big) 1.5f else 1f, alpha)
		if(str.isNotEmpty())
			r.drawFont(dx.toInt(), (dy-BaseFontNano.H).toInt(), str, FONT.NANO, col, if(big) 1f else .5f, alpha)
	}

	override val dx:Float get() = x-"$pts".length*FONT.NUM.w*if(big) 0.75f else 0.5f

	//

}
