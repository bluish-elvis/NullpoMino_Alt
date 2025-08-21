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
package zeroxfc.nullpo.custom.libs

import mu.nu.nullpo.game.event.EventReceiver
import mu.nu.nullpo.gui.common.BaseFont.FONT.BASE

class ScrollingMarqueeText(  // Strings that make up the headings
	headings:Array<String>,   // Strings that fill the info under the headings
	texts:Array<String>,   // Main color
	private val headingColor:EventReceiver.COLOR,   // Text color
	private val textColor:EventReceiver.COLOR) {
	// Whole string
	private var mainHeadingString = ""
	// Whole text string
	private var mainTextString = ""
	/**
	 * Automatically draw the roll at a certain Y value.
	 *
	 * @param receiver Current renderer
	 * @param y        Y-coordinate to draw on
	 * @param size     Size of text to draw with
	 * @param progress Progress of the roll (0: start, 1: end)
	 */
	fun drawAtY(receiver:EventReceiver, y:Double, size:Int, progress:Double) {
		val mainOffset1 =
			(40*SIZES[size]/SCALES_FLOAT[size]).toInt()-(progress*(40*SIZES[size]+(mainHeadingString.length+EXCESS_LENGTH)*SIZES[size])).toInt()
		val mainOffset2 =
			(40*SIZES[size]/SCALES_FLOAT[size]).toInt()-(progress*(40*SIZES[size]+(mainTextString.length+EXCESS_LENGTH)*SIZES[size])).toInt()
		receiver.drawFont(
			mainOffset1, (y*SIZES[size]).toInt(),
			mainHeadingString, BASE, headingColor, SCALES_FLOAT[size]
		)
		receiver.drawFont(
			mainOffset2, (y*SIZES[size]).toInt(),
			mainTextString, BASE, textColor, SCALES_FLOAT[size]
		)
	}

	companion object {
		// Text size index names
		const val SIZE_SMALL = 0
		const val SIZE_NORMAL = 1
		const val SIZE_LARGE = 2
		// Sizes of texts
		private val SIZES = intArrayOf(8, 16, 32)
		// Float sizes
		private val SCALES_FLOAT = floatArrayOf(0.5f, 1f, 2f)
		// Excess length
		private const val EXCESS_LENGTH = 5
	}
	/*
     * Create a staff roll.
     */
	init {
		val mHS = StringBuilder(mainHeadingString)
		val mTS = StringBuilder(mainTextString)
		for(i in headings.indices) {
			mHS.append(String(CharArray(headings[i].length)).replace("\u0000", " ")).append(" ").append(
				texts[i]
			).append(if(i<headings.size-1) " / " else "")
			mTS.append(headings[i]).append(" ").append(
				String(
					CharArray(
						texts[i].length
					)
				).replace("\u0000", " ")
			).append(if(i<headings.size-1) " / " else "")
		}
		mainHeadingString = "$mHS"
		mainTextString = "$mTS"
	}
}
