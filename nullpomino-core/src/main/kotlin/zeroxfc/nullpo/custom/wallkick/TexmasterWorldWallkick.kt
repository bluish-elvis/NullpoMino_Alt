/*
 Copyright (c) 2019-2024,
 This library class was created by 0xFC963F18DC21 / Shots243
 It is part of an extension library for the game NullpoMino (copyright 2010-2024)

 Converted to Kotlin and modified by Venom_Nhelv as bluish-elvis

 Herewith shall the term "Library Creator" be given to 0xFC963F18DC21.
 Herewith shall the term "Game Creator" be given to the original creator of NullpoMino, NullNoname.

 THIS LIBRARY AND MODE PACK WAS NOT MADE IN ASSOCIATION WITH THE GAME CREATOR.
 THIS KOTLIN VERSION WAS NOT MADE IN ASSOCIATION WITH THE LIBRARY CREATOR.

 Original Repository: https://github.com/Shots243/ModePile

 When using this library in a mode / library pack of your own, the following
 conditions must be satisfied:
     - This license must remain visible at the top of the document, unmodified.
     - You are allowed to use this library for any modding purpose.
         - If this is the case, the Library Creator must be credited somewhere.
             - Source comments only are fine, but in a README is recommended.
     - Modification of this library is allowed, but only in the condition that a
       pull request is made to merge the changes to the repository.

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

package zeroxfc.nullpo.custom.wallkick

import mu.nu.nullpo.game.subsystem.wallkick.StandardWallkick

/** SRS with symmetric & rotate direction biased I-piece kicks
 */
class TexmasterWorldWallkick:StandardWallkick() {
	// Wallkick data
	override val WALLKICK_NORMAL_L = listOf(
		listOf(-1 to 0, -1 to -1, 0 to 2, -1 to 2),
		listOf(-1 to 0, -1 to 1, 0 to -2, -1 to -2),
		listOf(1 to 0, 1 to -1, 0 to 2, 1 to 2),
		listOf(1 to 0, 1 to 1, 0 to -2, 1 to -2)
	)
	override val WALLKICK_NORMAL_R = listOf(
		listOf(-1 to 0, -1 to -1, 0 to 2, -1 to 2),
		listOf(1 to 0, 1 to 1, 0 to -2, 1 to -2),
		listOf(1 to 0, 1 to -1, 0 to 2, 1 to 2),
		listOf(-1 to 0, -1 to 1, 0 to -2, -1 to -2)
	)
	override val WALLKICK_I_L = listOf(
		listOf(-2 to 0, 1 to 0, 1 to -2, -2 to 1),
		listOf(-2 to 0, 1 to 0, -2 to -1, 1 to 2),
		listOf(2 to 0, -1 to 0, 2 to -1, -1 to 1),
		listOf(-1 to 0, 2 to 0, -1 to -2, 2 to 1)
	)
	override val WALLKICK_I_R = listOf(
		listOf(-2 to 0, 1 to 0, 1 to -2, -2 to 1),
		listOf(-1 to 0, 2 to 0, -1 to -2, 2 to 1),
		listOf(2 to 0, -1 to 0, 2 to -1, -1 to 1),
		listOf(-2 to 0, 1 to 0, -2 to -1, 1 to 2)
	)
	override val WALLKICK_I2_L = listOf(
		listOf(-1 to 0, -0 to -1, -1 to -2),
		listOf(-0 to 1, -1 to 0, -1 to 1),
		listOf(1 to 0, -0 to 1, 1 to 0),
		listOf(-0 to -1, 1 to 0, 1 to 1)
	)
	override val WALLKICK_I3_L = listOf(
		listOf(-1 to 0, 1 to 0, 0 to 0, 0 to 0),
		listOf(1 to 0, -1 to 0, 0 to -1, 0 to 1),
		listOf(1 to 0, -1 to 0, 0 to 2, 0 to -2),
		listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1)
	)
	override val WALLKICK_L3_L = listOf(
		listOf(-0 to -1, -0 to 1),
		listOf(-1 to 0, 1 to 0), listOf(-0 to 1, -0 to -1),
		listOf(1 to 0, -1 to 0)
	)
	override val WALLKICK_L3_R = listOf(
		listOf(-1 to 0, 1 to 0),
		listOf(0 to -1, 0 to 1), listOf(1 to 0, -1 to 0),
		listOf(0 to 1, 0 to -1)
	)
}
