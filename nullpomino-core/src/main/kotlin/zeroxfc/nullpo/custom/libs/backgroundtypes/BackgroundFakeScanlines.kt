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

import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.gui.common.AbstractRenderer
import mu.nu.nullpo.gui.common.ResourceImage
import mu.nu.nullpo.gui.common.bg.AbstractBG
import zeroxfc.nullpo.custom.libs.AnchorPoint
import kotlin.random.Random

class BackgroundFakeScanlines<T>(img:ResourceImage<T>):AbstractBG<T>(img) {
	// private ResourceHolderCustomAssetExtension customHolder;
	private var colorRandom:Random = Random.Default
	private var chunks:Array<ImageChunk> = emptyArray()
	private var phase = 0

	init{
		setup()
	}

	private fun setup() {
		// Generate chunks
		chunks = Array(AMT) {i ->
			ImageChunk(
				AnchorPoint.TL, listOf(0, 480/AMT*i+480/AMT/2), listOf(0, 480/AMT*i),
				listOf(640, 480/AMT), listOf(1f, 1f)
			)
		}
		phase = 0
	}

	override fun update() {
		chunks.forEach {
			val newScale = (.01f*colorRandom.nextFloat())+.995f
			it.scale = listOf(newScale, 1f)
		}
		phase = (phase+1)%PERIOD
	}

	override fun reset() {
		phase = 0
		update()
	}

	override fun draw(render:AbstractRenderer) {
		for(id in chunks.indices) {
			var col = 1f-BASE_LUMINANCE_OFFSET
			if(id and 2==0) col -= BASE_LUMINANCE_OFFSET
			if(phase>=PERIOD/2&&(id==phase-PERIOD/2||id==1+phase-PERIOD/2||id==-1+phase-PERIOD/2))
				col += BASE_LUMINANCE_OFFSET


			// Randomness offset
			col -= (.025f*colorRandom.nextFloat())
			val pos = chunks[id].drawLocation.map{it.toFloat()}
			val ddim = chunks[id].drawDimensions.map{it.toFloat()}
			val sloc = chunks[id].sourceLocation.map{it.toFloat()}
			val sdim = chunks[id].sourceDimensions.map{it.toFloat()}
			img.draw( pos[0], pos[1], ddim[0], ddim[1], sloc[0], sloc[1], sdim[0], sdim[1], 1f, Triple(col, col, col))
		}
	}
	companion object {
		private const val AMT = 480/2
		private const val PERIOD = 480 // Frames
		private const val BASE_LUMINANCE_OFFSET = 0.25f
	}
}
