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

import mu.nu.nullpo.game.component.Piece
import mu.nu.nullpo.game.event.EventReceiver.COLOR
import mu.nu.nullpo.game.event.ScoreEvent
import mu.nu.nullpo.gui.common.AbstractRenderer
import mu.nu.nullpo.gui.common.BaseFont.FONT.BASE

class PopupAward(x:Float, y:Float, val event:ScoreEvent, val moveTime:Int, val ex:Float = 0f):SpriteSheet(x, y) {
	val ox = x
	override var alpha
		get() = minOf(1f, 3f-ticks/22f)
		set(v) {}

	override fun update(r:AbstractRenderer):Boolean {
		ticks++
		x += vel.x

		vel.x = (ox+ex-x)*0.1f
		if(ticks>moveTime) {
			y -= 0.5f
		}
		event.piece?.setDarkness(0f)
		return ticks>=66
	}

	override fun draw(i:Int, r:AbstractRenderer) {
		val ev = event
		val x = x.toInt()
		val y = y.toInt()
		val strPieceName = ev.piece?.id?.let {Piece.Shape.names[it]}?:""

		tuple(ev.lines, ev.split).let {
			r.drawFont(x-it.length*8, y, it, BASE, color = when(ev.lines) {
				1 -> if(ev.twistType==null) COLOR.COBALT else COLOR.BLUE
				2 -> if(ev.twistType==null) if(ev.split) COLOR.PURPLE else COLOR.BLUE else COLOR.CYAN
				3 -> if(ev.twistType==null) if(ev.split) COLOR.CYAN else COLOR.GREEN else
					if((ticks/2)%2==0) COLOR.YELLOW else COLOR.GREEN
				else -> COLOR.getRainbowColor(ticks)
			}, alpha = alpha)
		}
		if(ev.twistType!=null) when {
			ev.twistType.mini -> {
				r.drawFont(x-80, y-16, "MINI", BASE, color = if(ev.b2b>0) COLOR.CYAN else COLOR.BLUE, alpha = alpha)
				ev.piece?.let {r.drawPiece(x-32, y, it, 0.5f, alpha = alpha)}
				r.drawFont(x-16, y, "$strPieceName-TWIST", BASE, color = if(ev.b2b>0) COLOR.PINK else COLOR.PURPLE, alpha = alpha)
			}
			ev.twistType.ez -> {
				ev.piece?.let {r.drawPiece(x-16, y, it, 0.5f, alpha = alpha)}
				r.drawFont(x-54, y-8, "EZ", BASE, color = COLOR.ORANGE, alpha = alpha)
				r.drawFont(x+54, y-8, "TWIST", BASE, color = COLOR.ORANGE, alpha = alpha)
			}
			else -> {
				ev.piece?.let {r.drawPiece(x-64, y, it, 0.5f, alpha = alpha)}
				r.drawFont(
					x-32,
					y-8,
					"-TWISTER",
					BASE,
					color = if(ev.lines==3) COLOR.getRainbowColor(ticks) else if(ev.b2b>0) COLOR.PINK else COLOR.PURPLE,
					alpha = alpha
				)
			}
		}
	}

	companion object {
		//		private val log = mu.nu.nullpo.util.log.getLogger(PopupAward::class)
		fun tuple(i:Int, s:Boolean = false) = when(i) {
			1 -> "Single"
			2 -> "Double"
			3 -> "Triple"
			4 -> if(!s) "QUADruple" else "SP. QUAD"
			5 -> if(!s) "PENTuple" else "SP. QUINT"
			6 -> if(!s) "HEXTuple" else "SP. HEXA"
			12 -> "DOZEN"
			13 -> "Baker's DOZEN"
			20 -> "ViginTUPLE"
			21 -> "UniginTUPLE"
			22 -> "DUOLIUS"
			else -> "$i-LINE"
		}
	}
}
