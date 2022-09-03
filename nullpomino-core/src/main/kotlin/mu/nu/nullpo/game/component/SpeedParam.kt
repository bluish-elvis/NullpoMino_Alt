/*
 * Copyright (c) 2010-2022, NullNoname
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
package mu.nu.nullpo.game.component

import java.io.Serializable
import kotlin.math.sqrt

/** Blockピースの落下速度や出現待ち timeなどの data */
data class SpeedParam(
	/** 落下速度 */
	var gravity:Int = 4,
	/** 落下速度の分母 (gravity==denominatorなら1Gになる) */
	var denominator:Int = 256,
	/** 出現待ち time */
	var are:Int = 24,
	/** Line clear後の出現待ち time */
	var areLine:Int = 24,
	/** Line clear time */
	var lineDelay:Int = 40,
	/** 固定 time */
	var lockDelay:Int = 30,
	/** 横移動 time */
	var das:Int = 14
):Serializable {

	val rank:Float get() = if(gravity<0) 1f else (sqrt((gravity.toFloat()/denominator).toDouble())/sqrt(20.0)).toFloat()
	/** Constructor */
	constructor():this(4, 256, 24, 24, 40, 30, 14)

	/** Copy constructor
	 * @param s Copy source
	 */
	constructor(s:SpeedParam?):this(
		s?.gravity ?: 4, s?.denominator ?: 256, s?.are ?: 24, s?.areLine ?: 24,
		s?.lineDelay ?: 40, s?.lockDelay ?: 30, s?.das ?: 14
	)

	constructor(l:LevelData, i:Int):this(l[i])

	/** Reset to defaults */
	fun reset() {
		gravity = 4
		denominator = 256
		are = 24
		areLine = 24
		lineDelay = 40
		lockDelay = 30
		das = 14
	}

	/** 設定を[s]からコピー */
	fun replace(s:SpeedParam?) {
		s?.let {b ->
			gravity = b.gravity
			denominator = b.denominator
			are = b.are
			areLine = b.areLine
			lineDelay = b.lineDelay
			lockDelay = b.lockDelay
			das = b.das
		} ?: reset()
	}

	companion object {
		/** Serial version ID */
		private const val serialVersionUID = -955934100998757270L
		val SDS_FIXED = floatArrayOf(0.5f, 1f, 2f, 3f, 4f, 5f, 20f)
	}
}
