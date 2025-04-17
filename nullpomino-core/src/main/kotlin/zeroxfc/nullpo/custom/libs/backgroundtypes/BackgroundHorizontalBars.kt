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

import mu.nu.nullpo.gui.common.AbstractRenderer
import mu.nu.nullpo.gui.common.ResourceImage
import mu.nu.nullpo.gui.common.bg.AbstractBG
import zeroxfc.nullpo.custom.libs.AnchorPoint
import zeroxfc.nullpo.custom.libs.MathHelper.almostEqual
import kotlin.math.PI
import kotlin.math.sin

class BackgroundHorizontalBars<T>(img:ResourceImage<T>, pulseFrames:Int, sliceSize:Int? = null,
	pulseBaseScale:Float? = BASE_SCALE, pulseScaleVariance:Float? = SCALE_VARIANCE, reverse:Boolean = false):AbstractBG<T>(img) {
	// private ResourceHolderCustomAssetExtension customHolder;
	private var chunks:Array<ImageChunk> = emptyArray()
	private var pulsePhaseMax = 0
	private var currentPulsePhase = 0
	private var pulseBaseScale:Float? = null
	private var pulseScaleVariance:Float? = null
	private var reverse = false

	init {
		setup(pulseFrames, sliceSize, pulseBaseScale, pulseScaleVariance, reverse)
	}

	private fun setup(pulseFrames:Int, sliceSize:Int?, baseScale:Float?, scaleVariance:Float?, _reverse:Boolean) {
		if(baseScale==null||scaleVariance==null||sliceSize==null) {
			chunks = Array(AMT) {i ->
				ImageChunk(
					AnchorPoint.ML, listOf(0, 480/AMT*i+480/AMT/2),
					listOf(0, 480/AMT*i), listOf(640, 480/AMT), listOf(1f, BASE_SCALE)
				)
			}
			reverse = _reverse
			pulsePhaseMax = pulseFrames
			currentPulsePhase = pulsePhaseMax
		} else {
			pulseBaseScale = baseScale
			pulseScaleVariance = scaleVariance
			chunks = Array(sliceSize) {i ->
				ImageChunk(
					AnchorPoint.ML, listOf(0, 480/sliceSize*i+480/sliceSize/2),
					listOf(0, 480/sliceSize*i), listOf(640, 480/sliceSize), listOf(1f, baseScale)
				)
			}
			reverse = _reverse
			pulsePhaseMax = pulseFrames
			currentPulsePhase = pulsePhaseMax
		}
	}

	fun modifyValues(pulseFrames:Int, baseScale:Float?, scaleVariance:Float?, _reverse:Boolean) {
		reverse = _reverse
		pulsePhaseMax = pulseFrames
		if(baseScale!=null) pulseBaseScale = baseScale
		if(scaleVariance!=null) pulseScaleVariance = scaleVariance
		if(currentPulsePhase>pulsePhaseMax) currentPulsePhase = pulsePhaseMax
	}

	fun resetPulseScaleValues() {
		if(pulseBaseScale!=null) pulseBaseScale = null
		if(pulseScaleVariance!=null) pulseScaleVariance = null
	}

	override fun update() {
		currentPulsePhase = (currentPulsePhase+1)%pulsePhaseMax
		for(i in chunks.indices) {
			val j = if(reverse) chunks.size-i-1 else i
			val ppu = (currentPulsePhase+i)%pulsePhaseMax
			val baseScale = pulseBaseScale ?: BASE_SCALE
			val scaleVariance = pulseScaleVariance ?: SCALE_VARIANCE
			val newScale = (baseScale+sin(TWO_PI*(ppu.toDouble()/pulsePhaseMax))*scaleVariance).coerceAtLeast(1.0)
			chunks[j].scale = listOf(1f, newScale.toFloat())
		}
	}

	override fun reset() {
		currentPulsePhase = pulsePhaseMax
		update()
	}

	override fun draw(render:AbstractRenderer, bg:Boolean) {
		val priorityList = chunks.sortedBy {it.scale[1]}.toMutableList()
		val baseScale = pulseBaseScale ?: BASE_SCALE
		if(baseScale.toDouble().almostEqual(1.0, 0.005)) {
			img.draw()
			priorityList.removeAll {it.scale[0].toDouble().almostEqual(1.0, 0.005)}
		}
		priorityList.forEach {i ->
			val pos = i.drawLocation
			val ddim = i.drawDimensions
			val sloc = i.sourceLocation
			val sdim = i.sourceDimensions
			img.draw(pos[0], pos[1], ddim[0], ddim[1], sloc[0], sloc[1], sdim[0], sdim[1])
		}
	}

	companion object {
		private const val AMT = 480/3
		private const val TWO_PI = PI*2
		private const val BASE_SCALE = 1f
		private const val SCALE_VARIANCE = 1f
	}

}
