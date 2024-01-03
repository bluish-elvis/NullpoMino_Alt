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
package zeroxfc.nullpo.custom.libs.backgroundtypes

import zeroxfc.nullpo.custom.libs.AnchorPoint

class ImageChunk @JvmOverloads constructor(anchorType:AnchorPoint = AnchorPoint.TL, anchorLocation:List<Int> = listOf(0, 0),
	val sourceLocation:List<Int> = listOf(0, 0), sourceDimensions:List<Int> = listOf(1, 1), scale:List<Float> = listOf(1f, 1f)) {
	var drawLocation = emptyList<Int>()
		private set
	private var anchorType:AnchorPoint = anchorType
		set(value) {
			field = value
			calibrateDrawLocation()
		}
	var anchorLocation = anchorLocation
		set(value) {
			field = value
			calibrateDrawLocation()
		}
	var sourceDimensions = sourceDimensions
		set(value) {
			field = value
			calibrateDrawLocation()
		}
	var scale = scale
		set(value) {
			field = value
			calibrateDrawLocation()
		}

	val drawDimensions
		get() = listOf((sourceDimensions[0]*scale[0]).toInt(), (sourceDimensions[1]*scale[1]).toInt())

	init {
		calibrateDrawLocation()
	}

	private fun calibrateDrawLocation() {
		val ddim = drawDimensions
		drawLocation = when(anchorType) {
			AnchorPoint.TM -> listOf(anchorLocation[0]-ddim[0]/2, anchorLocation[1])
			AnchorPoint.TR -> listOf(anchorLocation[0]-ddim[0], anchorLocation[1])
			AnchorPoint.ML -> listOf(anchorLocation[0], anchorLocation[1]-ddim[1]/2)
			AnchorPoint.MM -> listOf(anchorLocation[0]-ddim[0]/2, anchorLocation[1]-ddim[1]/2)
			AnchorPoint.MR -> listOf(anchorLocation[0]-ddim[0], anchorLocation[1]-ddim[1]/2)
			AnchorPoint.LL -> listOf(anchorLocation[0], anchorLocation[1]-ddim[1])
			AnchorPoint.LM -> listOf(anchorLocation[0]-ddim[0]/2, anchorLocation[1]-ddim[1])
			AnchorPoint.LR -> listOf(anchorLocation[0]-ddim[0], anchorLocation[1]-ddim[1])
			else -> listOf(anchorLocation[0], anchorLocation[1])
		}
	}
}
