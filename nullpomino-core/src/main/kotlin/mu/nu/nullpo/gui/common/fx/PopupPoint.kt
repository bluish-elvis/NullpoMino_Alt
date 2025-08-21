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

import mu.nu.nullpo.game.event.EventReceiver
import mu.nu.nullpo.game.event.EventReceiver.COLOR
import mu.nu.nullpo.gui.common.AbstractRenderer
import mu.nu.nullpo.gui.common.BaseFont.FONT.NUM

class PopupPoint(x:Int, y:Int, val pts:Int, private val c:Int):SpriteSheet(x, y) {
	val color get() = if(c==COLOR.RAINBOW.ordinal) EventReceiver.getRainbowColor(ticks).ordinal else c
	override var alpha
		get() = minOf(1f, 2f-ticks/36f)
		set(v) {}

	override fun update(r:AbstractRenderer):Boolean {
		y += vel.y
		vel.y = maxOf(-.75f, .5f-ticks*0.03f)
		return ++ticks>=72||pts==0
	}

	override fun draw(i:Int, r:AbstractRenderer) {
		if(pts>0) r.drawFont(
			dx.toInt(), dy.toInt(), "+${pts}",
			NUM,
			if(ticks/2%2==0) COLOR.WHITE else COLOR.all[color], alpha = alpha
		)
		else if(pts<0) r.drawFont(dx.toInt(), dy.toInt(), "$pts", NUM, COLOR.RED)
	}

	override val dx:Float get() = x-"$pts".length*6

	//

}
