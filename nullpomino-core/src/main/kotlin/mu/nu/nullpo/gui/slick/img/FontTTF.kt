/*
 * Copyright (c) 2021, NullNoname
 * Kotlin converted and modified by Venom=Nhelv
 * All rights reserved.
 *
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

package mu.nu.nullpo.gui.slick.img

import mu.nu.nullpo.game.event.EventReceiver
import mu.nu.nullpo.gui.common.BaseFontTTF
import mu.nu.nullpo.gui.slick.ResourceHolder
import org.newdawn.slick.Color

object FontTTF:BaseFontTTF{
	/** TTF font Drawing a string using the
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param str String
	 * @param color Letter cint
	 */
	override fun print(x:Int, y:Int, str:String, color:EventReceiver.COLOR, alpha:Float) {
		ResourceHolder.ttfFont?.drawString(x.toFloat(), y.toFloat(), str, when(color) {
			EventReceiver.COLOR.BLUE -> Color(0, 0, 255)
			EventReceiver.COLOR.RED -> Color(255, 0, 0)
			EventReceiver.COLOR.PINK -> Color(255, 128, 128)
			EventReceiver.COLOR.GREEN -> Color(0, 255, 0)
			EventReceiver.COLOR.YELLOW -> Color(255, 255, 0)
			EventReceiver.COLOR.CYAN -> Color(0, 255, 255)
			EventReceiver.COLOR.ORANGE -> Color(255, 128, 0)
			EventReceiver.COLOR.PURPLE -> Color(255, 0, 255)
			EventReceiver.COLOR.COBALT -> Color(0, 0, 128)
			else -> Color(255, 255, 255)
		}.apply {a = alpha})
	}
}