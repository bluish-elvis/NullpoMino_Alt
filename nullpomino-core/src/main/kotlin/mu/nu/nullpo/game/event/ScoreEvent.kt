/*
 * Copyright (c) 2021-2022, NullNoname
 * Kotlin converted and modified by Venom=Nhelv.
 * THIS WAS NOT MADE IN ASSOCIATION WITH THE GAME CREATOR.
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

data class ScoreEvent(val piece:Piece? = null, val lines:Int = 0, val b2b:Int = -1,
	val combo:Int = -1, val twistType:Twister? = null, val split:Boolean = false) {
	/** True if Twister */
	val twist:Boolean get() = twistType!=null
	/** True if Twister Mini */
	val twistMini:Boolean get() = twistType?.mini==true
	/** EZ Twister */
	val twistEZ:Boolean get() = twistType?.ez==true

	override fun equals(other:Any?):Boolean {
		if(this===other) return true
		if(javaClass!=other?.javaClass) return false

		other as ScoreEvent

		return !(lines!=other.lines||split!=other.split||piece!=other.piece||twistType!=other.twistType||combo!=other.combo||b2b!=other.b2b)
	}

	override fun toString():String = "sc<${piece?.type?.name},$lines,$b2b,$combo,${twistType?.name},${split.toInt()}>"
	override fun hashCode():Int {
		var result = lines
		result = 31*result+split.hashCode()
		result = 31*result+(piece?.hashCode() ?: 0)
		result = 31*result+(twistType?.hashCode() ?: 0)
		result = 31*result+combo
		result = 31*result+b2b
		return result
	}

	companion object {
		fun parseStr(s:String) =
			Regex("""^sc<(?<piece>\w+),(?<lines>\d+),(?<b2b>[-\d]+),(?<combo>[-\d]+),(?<twist>\w+),(?<split>\d)>$""")
				.matchEntire(s)?.destructured?.toList()?.let {i ->
					ScoreEvent(
						Piece(Piece.Shape.names.indexOf(i[0])), i[1].toInt(), i[2].toInt(), i[3].toInt(),
						Twister.all.find {it.name==i[4]}, i[5]=="1"
					)
				}
	}

	enum class Twister {
		IMMOBILE_EZ, IMMOBILE_MINI, POINT_MINI, IMMOBILE, POINT;

		val mini:Boolean get() = this==POINT_MINI||this==IMMOBILE_MINI
		val ez:Boolean get() = this==IMMOBILE_EZ

		companion object {
			val all = Twister.values()
		}
	}
}
