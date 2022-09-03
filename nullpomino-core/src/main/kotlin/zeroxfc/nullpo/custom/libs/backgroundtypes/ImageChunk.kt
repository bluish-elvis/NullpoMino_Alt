/*
 * This library class was created by 0xFC963F18DC21 / Shots243
 * It is part of an extension library for the game NullpoMino (copyright 2010)
 *
 * Herewith shall the term "Library Creator" be given to 0xFC963F18DC21.
 * Herewith shall the term "Game Creator" be given to the original creator of NullpoMino.
 *
 * THIS LIBRARY AND MODE PACK WAS NOT MADE IN ASSOCIATION WITH THE GAME CREATOR.
 *
 * Repository: https://github.com/Shots243/ModePile
 *
 * When using this library in a mode / library pack of your own, the following
 * conditions must be satisfied:
 *     - This license must remain visible at the top of the document, unmodified.
 *     - You are allowed to use this library for any modding purpose.
 *         - If this is the case, the Library Creator must be credited somewhere.
 *             - Source comments only are fine, but in a README is recommended.
 *     - Modification of this library is allowed, but only in the condition that a
 *       pull request is made to merge the changes to the repository.
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
package zeroxfc.nullpo.custom.libs.backgroundtypes

class ImageChunk @JvmOverloads constructor(anchorType:Int = 0, anchorLocation:IntArray = intArrayOf(0, 0),
	val sourceLocation:IntArray = intArrayOf(0, 0),
	sourceDimensions:IntArray = intArrayOf(1, 1), scale:FloatArray = floatArrayOf(1f, 1f)) {
	var drawLocation = intArrayOf()
		private set
	private var anchorType:Int = anchorType
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

	private fun calibrateDrawLocation() {
		val ddim = drawDimensions
		drawLocation = when(anchorType) {
			ANCHOR_POINT_TM -> intArrayOf(anchorLocation[0]-ddim[0]/2, anchorLocation[1])
			ANCHOR_POINT_TR -> intArrayOf(anchorLocation[0]-ddim[0], anchorLocation[1])
			ANCHOR_POINT_ML -> intArrayOf(anchorLocation[0], anchorLocation[1]-ddim[1]/2)
			ANCHOR_POINT_MM -> intArrayOf(anchorLocation[0]-ddim[0]/2, anchorLocation[1]-ddim[1]/2)
			ANCHOR_POINT_MR -> intArrayOf(anchorLocation[0]-ddim[0], anchorLocation[1]-ddim[1]/2)
			ANCHOR_POINT_LL -> intArrayOf(anchorLocation[0], anchorLocation[1]-ddim[1])
			ANCHOR_POINT_LM -> intArrayOf(anchorLocation[0]-ddim[0]/2, anchorLocation[1]-ddim[1])
			ANCHOR_POINT_LR -> intArrayOf(anchorLocation[0]-ddim[0], anchorLocation[1]-ddim[1])
			else -> intArrayOf(anchorLocation[0], anchorLocation[1])
		}
	}

	val drawDimensions:IntArray
		get() = intArrayOf((sourceDimensions[0]*scale[0]).toInt(), (sourceDimensions[1]*scale[1]).toInt())

	companion object {
		const val ANCHOR_POINT_TL = 0
		const val ANCHOR_POINT_TM = 1
		const val ANCHOR_POINT_TR = 2
		const val ANCHOR_POINT_ML = 3
		const val ANCHOR_POINT_MM = 4
		const val ANCHOR_POINT_MR = 5
		const val ANCHOR_POINT_LL = 6
		const val ANCHOR_POINT_LM = 7
		const val ANCHOR_POINT_LR = 8
	}

	init {
		calibrateDrawLocation()
	}
}
