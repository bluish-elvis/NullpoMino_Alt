/*
 Copyright (c) 2010-2024, NullNoname
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

package mu.nu.nullpo.game.component

@ConsistentCopyVisibility data class LevelData internal constructor(
	/** Fall velocity table (numerators) 落下速度 */
	val gravity:IntArray,
	/** Fall velocity table (denominators) 落下速度の分母 (gravity==denominatorなら1Gになる) */
	val denominator:IntArray,
	/** 出現待ち time */
	val are:IntArray,
	/** Line clear後の出現待ち time */
	val areLine:IntArray = are,
	/** Line clear time */
	val lineDelay:IntArray,
	/** 固定 time */
	val lockDelay:IntArray,
	/** 横移動 time */
	val das:IntArray
) {
	// areLine = are if not specified
	constructor(gravity:IntArray, denominator:IntArray,
		are:IntArray, lineDelay:IntArray, lockDelay:IntArray, das:IntArray):
		this(gravity, denominator, are, are, lineDelay, lockDelay, das)

	constructor(
		gravity:List<Int> = listOf(4), denominator:List<Int> = listOf(256),
		are:List<Int> = listOf(24), areLine:List<Int> = are, lineDelay:List<Int> = listOf(40),
		lockDelay:List<Int> = listOf(30), das:List<Int> = listOf(14)
	):this(gravity.toIntArray(), denominator.toIntArray(), are.toIntArray(), areLine.toIntArray(),
		lineDelay.toIntArray(), lockDelay.toIntArray(), das.toIntArray())
	// areLine = are if not specified
	constructor(gravity:List<Int>, denominator:List<Int>,
		are:List<Int>, lineDelay:List<Int>, lockDelay:List<Int>, das:List<Int>):
		this(gravity, denominator, are, are, lineDelay, lockDelay, das)

	//if are & delays are fixed
	constructor(gravity:List<Int>, denominator:List<Int>, are:Int = 24, areLine:Int = 24, lineDelay:Int = 40,
		lockDelay:Int = 30, das:Int = 14):
		this(
			gravity, denominator,
			listOf(are), listOf(areLine), listOf(lineDelay), listOf(lockDelay), listOf(das)
		)

	// gravity & denominator = -1 / 256 if not specified
	constructor(are:List<Int>, areLine:List<Int>, lineDelay:List<Int>, lockDelay:List<Int>, das:List<Int>):
		this(listOf(-1), listOf(256), are, areLine, lineDelay, lockDelay, das)

	// gravity & denominator = -1 / 256, areLine = are if not specified
	constructor(are:List<Int>, lineDelay:List<Int>, lockDelay:List<Int> = listOf(30), das:List<Int> = listOf(14)):
		this(are, are, lineDelay, lockDelay, das)

	// gravity & denominator = -1 / 256 if not specified, are & delays are fixed
	constructor(are:Int, areLine:Int, lineDelay:Int, lockDelay:Int = 30, das:Int = 14):
		this(listOf(-1), listOf(256), are, areLine, lineDelay, lockDelay, das)

	operator fun get(i:Int) =
		SpeedParam(lv(gravity, i), lv(denominator, i), lv(are, i), lv(areLine, i), lv(lineDelay, i), lv(lockDelay, i),
			lv(das, i))

	val size get() = maxOf(gravity.size, denominator.size, are.size, areLine.size, lineDelay.size, lockDelay.size, das.size)

	companion object {
		fun <T> lv(arr:List<T>, i:Int):T = arr[i.coerceIn(arr.indices)]
		fun <T> lv(arr:Array<T>, i:Int):T = arr[i.coerceIn(arr.indices)]
		fun lv(arr:IntArray, i:Int):Int = arr[i.coerceIn(arr.indices)]
	}

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
		if(size!=other.size) return false

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
		result = 31*result+size
		return result
	}

}
