/*
 * Copyright (c) 2010-2022, NullNoname
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

package mu.nu.nullpo.game.component

data class LevelData(
	/** 落下速度 */
	var gravity:List<Int> = listOf(4),
	/** 落下速度の分母 (gravity==denominatorなら1Gになる) */
	var denominator:List<Int> = listOf(256),
	/** 出現待ち time */
	var are:List<Int> = listOf(24),
	/** Line clear後の出現待ち time */
	var areLine:List<Int> = are,
	/** Line clear time */
	var lineDelay:List<Int> = listOf(40),
	/** 固定 time */
	var lockDelay:List<Int> = listOf(30),
	/** 横移動 time */
	var das:List<Int> = listOf(14)
) {
	constructor(gravity:List<Int>, denominator:List<Int>, are:Int, areLine:Int, lineDelay:Int, lockDelay:Int, das:Int):
		this(
			gravity, denominator,
			listOf(are), listOf(areLine), listOf(lineDelay), listOf(lockDelay), listOf(das)
		)

	constructor(gravity:List<Int>, denominator:List<Int>,
		are:List<Int>,  lineDelay:List<Int>, lockDelay:List<Int>, das:List<Int>):
		this(gravity, denominator, are, are, lineDelay, lockDelay, das)

	constructor(are:List<Int>, lineDelay:List<Int>, lockDelay:List<Int>, das:List<Int>):
		this(listOf(-1), listOf(256), are, are, lineDelay, lockDelay, das)

	constructor(are:Int, areLine:Int, lineDelay:Int, lockDelay:Int, das:Int):
		this(listOf(-1), listOf(256), are, areLine, lineDelay, lockDelay, das)

	operator fun get(i:Int) =
		SpeedParam(lv(gravity, i), lv(denominator, i), lv(are, i), lv(areLine, i), lv(lineDelay, i), lv(lockDelay, i), lv(das, i))

	companion object {
		fun lv(arr:List<Int>, i:Int):Int = arr[maxOf(0, minOf(i, arr.size-1))]
	}

}
