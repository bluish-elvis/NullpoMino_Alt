/*
 * Copyright (c) 2010-2021, NullNoname
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

data class LevelData(
	/** 落下速度 */
	var gravity:IntArray = intArrayOf(4),
	/** 落下速度の分母 (gravity==denominatorなら1Gになる) */
	var denominator:IntArray = intArrayOf(256),
	/** 出現待ち time */
	var are:IntArray = intArrayOf(24),
	/** Line clear後の出現待ち time */
	var areLine:IntArray = intArrayOf(24),
	/** Line clear time */
	var lineDelay:IntArray = intArrayOf(40),
	/** 固定 time */
	var lockDelay:IntArray = intArrayOf(30),
	/** 横移動 time */
	var das:IntArray = intArrayOf(14)
) {
	constructor(gravity:IntArray, denominator:IntArray, are:Int, areLine:Int, lineDelay:Int, lockDelay:Int, das:Int):
		this(gravity, denominator,
			intArrayOf(are), intArrayOf(areLine), intArrayOf(lineDelay), intArrayOf(lockDelay), intArrayOf(das))

	constructor(gravity:Int, denominator:Int,
		are:IntArray, areLine:IntArray, lineDelay:IntArray, lockDelay:IntArray, das:IntArray):
		this(intArrayOf(gravity), intArrayOf(denominator), are, areLine, lineDelay, lockDelay, das)

	constructor(are:IntArray, areLine:IntArray, lineDelay:IntArray, lockDelay:IntArray, das:IntArray):
		this(-1, 256, are, areLine, lineDelay, lockDelay, das)

	constructor(are:Int, areLine:Int, lineDelay:Int, lockDelay:Int, das:Int):
		this(intArrayOf(-1), intArrayOf(256), are, areLine, lineDelay, lockDelay, das)

	operator fun get(i:Int) =
		SpeedParam(lv(gravity, i), lv(denominator, i), lv(are, i), lv(areLine, i), lv(lineDelay, i), lv(lockDelay, i), lv(das, i))

	override fun equals(other:Any?):Boolean {
		if(this===other) return true
		if(javaClass!=other?.javaClass) return false

		other as LevelData

		if(!gravity.contentEquals(other.gravity)) return false
		if(!denominator.contentEquals(other.denominator)) return false
		if(!are.contentEquals(other.are)) return false
		if(!areLine.contentEquals(other.areLine)) return false
		if(!lineDelay.contentEquals(other.lineDelay)) return false
		if(!lockDelay.contentEquals(other.lockDelay)) return false
		if(!das.contentEquals(other.das)) return false

		return true
	}

	override fun hashCode():Int {
		var result = gravity.contentHashCode()
		result = 31*result+denominator.contentHashCode()
		result = 31*result+are.contentHashCode()
		result = 31*result+areLine.contentHashCode()
		result = 31*result+lineDelay.contentHashCode()
		result = 31*result+lockDelay.contentHashCode()
		result = 31*result+das.contentHashCode()
		return result
	}

	companion object {
		fun lv(arr:IntArray, i:Int):Int = arr[maxOf(0, minOf(i, arr.size-1))]
	}

}
