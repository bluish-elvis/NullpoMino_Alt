/*
 * Copyright (c) 2021-2022, NullNoname
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

package mu.nu.nullpo.game.event

import mu.nu.nullpo.game.component.Piece
import mu.nu.nullpo.util.GeneralUtil.toInt

data class ScoreEvent(val lines:Int = 0, val split:Boolean = false, val piece:Piece? = null, val twist:Twister? = null,
	val b2b:Boolean = false) {

	override fun equals(other:Any?):Boolean {
		if(this===other) return true
		if(javaClass!=other?.javaClass) return false

		other as ScoreEvent

		return !(lines!=other.lines||split!=other.split||piece!=other.piece||twist!=other.twist||b2b!=other.b2b)
	}

	override fun hashCode():Int {
		var result = piece.hashCode()
		result = (Twister.all.size+1)*result+1+(twist?.ordinal ?: -1)
		result = 10*result+minOf(maxOf(0, lines), 9)
		result = 4*result+split.toInt()+b2b.toInt()*2
		return result
	}

	companion object {
		fun parseInt(i:Int):ScoreEvent? = if(i<0) null
		else {
			val lines = (i shr 2)%10
			val twist = (i shr 2)/10%(Twister.all.size+1)
			val piece = (i shr 2)/10/(Twister.all.size+1)%Piece.Shape.all.size
			ScoreEvent(
				lines, i%2==1, if(piece<=0) null else Piece(piece-1).apply {},
				Twister.all.getOrNull(twist-1), (i shr 1)%2==1
			)
		}

		fun parseInt(i:String):ScoreEvent? = parseInt(i.toInt())
	}

	enum class Twister {
		IMMOBILE_EZ, IMMOBILE_MINI, POINT_MINI, IMMOBILE, POINT;

		val isMini:Boolean get() = this==POINT_MINI||this==IMMOBILE_MINI

		companion object {
			val all = Twister.values()
		}
	}
}
