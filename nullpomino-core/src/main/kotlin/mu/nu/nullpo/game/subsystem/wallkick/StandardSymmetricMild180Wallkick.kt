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

package mu.nu.nullpo.game.subsystem.wallkick

/** SRS with symmetric I-piece kicks and saner 180 kicks */
class StandardSymmetricMild180Wallkick:StandardWallkick() {
	// Wallkick data
	override val WALLKICK_I_L = listOf(
		listOf(2 to 0, -1 to 0, -1 to -2, 2 to 1), // 0>>3
		listOf(2 to 0, -1 to 0, 2 to -1, -1 to 2), // 1>>0
		listOf(-2 to 0, 1 to 0, -2 to -1, 1 to 1), // 2>>1
		listOf(1 to 0, -2 to 0, 1 to -2, -2 to 1), // 3>>2
	)
	override val WALLKICK_I_R = listOf(
		listOf(-2 to 0, 1 to 0, 1 to -2, -2 to 1), // 0>>1
		listOf(-1 to 0, 2 to 0, -1 to -2, 2 to 1), // 1>>2
		listOf(2 to 0, -1 to 0, 2 to -1, -1 to 1), // 2>>3
		listOf(-2 to 0, 1 to 0, -2 to -1, 1 to 2), // 3>>0
	)

	override val WALLKICK_L3_L = listOf(
		listOf(0 to -1, 0 to 1),  // 0>>3
		listOf(1 to 0, -1 to 0),  // 1>>0
		listOf(0 to 1, 0 to -1),  // 2>>1
		listOf(-1 to 0, 1 to 0),  // 3>>2
	)
	override val WALLKICK_L3_R = listOf(
		listOf(-1 to 0, 1 to 0),  // 0>>1
		listOf(0 to -1, 0 to 1),  // 1>>2
		listOf(1 to 0, -1 to 0),  // 2>>3
		listOf(0 to 1, 0 to -1),  // 3>>0
	)

	// 180-degree rotation wallkick data
	override val WALLKICK_NORMAL_180 = listOf(
		listOf(1 to 0, -1 to 0, 0 to -1, 0 to 1, 0 to -2,
			0 to 2),  // 0>>2─ ┐
		listOf(0 to 1, 0 to -1, 0 to -2, 0 to 2, -1 to 0,
			1 to 0),  // 1>>3─ ┼ ┐
		listOf(-1 to 0, 1 to 0, 0 to 1, 0 to -1, 0 to 2,
			0 to -2),  // 2>>0─ ┘ │
		listOf(0 to 1, 0 to -1, 0 to -2, 0 to 2, 1 to 0,
			-1 to 0) // 3>>1─ ─ ┘
		//{{ 1, 0},{ 2, 0},{ 1, 1},{ 2, 1},{-1, 0},{-2, 0},{-1, 1},{-2, 1},{ 0,-1},{ 3, 0},{-3, 0}},	// 0>>2─ ┐
		//{{ 0, 1},{ 0, 2},{-1, 1},{-1, 2},{ 0,-1},{ 0,-2},{-1,-1},{-1,-2},{ 1, 0},{ 0, 3},{ 0,-3}},	// 1>>3─ ┼ ┐
		//{{-1, 0},{-2, 0},{-1,-1},{-2,-1},{ 1, 0},{ 2, 0},{ 1,-1},{ 2,-1},{ 0, 1},{-3, 0},{ 3, 0}},	// 2>>0─ ┘ │
		//{{ 0, 1},{ 0, 2},{ 1, 1},{ 1, 2},{ 0,-1},{ 0,-2},{ 1,-1},{ 1,-2},{-1, 0},{ 0, 3},{ 0,-3}},	// 3>>1─ ─ ┘
	)
	override val WALLKICK_I_180 = listOf(
		listOf(-1 to 0, -2 to 0, 1 to 0, 2 to 0),  // 0>>2─ ┐
		listOf(0 to 1, 0 to -1, 0 to -2, 0 to 2),  // 1>>3─ ┼ ┐
		listOf(1 to 0, 2 to 0, -1 to 0, -2 to 0),  // 2>>0─ ┘ │
		listOf(0 to 1, 0 to -1, 0 to -2, 0 to 2),  // 3>>1─ ─ ┘
		//{{-1, 0},{-2, 0},{ 1, 0},{ 2, 0},{ 0, 1}},													// 0>>2─ ┐
		//{{ 0, 1},{ 0, 2},{ 0,-1},{ 0,-2},{-1, 0}},													// 1>>3─ ┼ ┐
		//{{ 1, 0},{ 2, 0},{-1, 0},{-2, 0},{ 0,-1}},													// 2>>0─ ┘ │
		//{{ 0, 1},{ 0, 2},{ 0,-1},{ 0,-2},{ 1, 0}},													// 3>>1─ ─ ┘
	)

}
