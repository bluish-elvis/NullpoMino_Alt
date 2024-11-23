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
import kotlin.math.PI
import kotlin.math.sin

class BackgroundInterlaceVertical<T>(img:ResourceImage<T>, columnWidth:Int = DEFAULT_COLUMN_WIDTH,
	pulseTimerFrames:Int = DEFAULT_TIMER_MAX, pulseBaseScale:Float = BASE_SCALE, pulseScaleVariance:Float = SCALE_VARIANCE,
	upOdd:Boolean = UP_ODD_DEFAULT, reverse:Boolean = false):AbstractBG<T>(img) {
	private var chunks:List<ImageChunk> = emptyList()
	private var pulseTimer = 0
	private var pulseTimerMax = 0
	private var columnWidth = 0
	private var baseScale = 0f
	private var scaleVariance = 0f
	private var upOdd = false
	private var reverse = false

	init {
		setup(columnWidth, pulseTimerFrames, pulseBaseScale, pulseScaleVariance, upOdd, reverse)
	}

	private fun setup(width:Int = DEFAULT_COLUMN_WIDTH, pulseTimerFrames:Int = DEFAULT_TIMER_MAX,
		pulseBaseScale:Float = BASE_SCALE, pulseScaleVariance:Float = SCALE_VARIANCE, _upOdd:Boolean = UP_ODD_DEFAULT,
		_reverse:Boolean = false) {
		upOdd = !_upOdd
		pulseTimerMax = pulseTimerFrames
		baseScale = pulseBaseScale
		scaleVariance = pulseScaleVariance
		pulseTimer = pulseTimerFrames
		reverse = _reverse
		columnWidth = if(480%width!=0) DEFAULT_COLUMN_WIDTH else width
		chunks = List(SCREEN_WIDTH/width) {i ->
			val up = _upOdd&&i%2==1
			val anchorType = if(up) AnchorPoint.LL else AnchorPoint.TL
			val anchorLocation = listOf(i*width, if(up) SCREEN_HEIGHT else 0)
			val srcLocation = listOf(i*width, 0)
			ImageChunk(anchorType, anchorLocation, srcLocation, listOf(width, 480), listOf(1f, baseScale))
		}
	}
	/**
	 * Performs an update tick on the background. Advisably used in onLast.
	 */
	override fun update() {
		pulseTimer++
		if(pulseTimer>=pulseTimerMax) {
			pulseTimer = 0
		}
		for(i in chunks.indices) {
			val j = if(reverse) chunks.size-1-i else i
			val ppu = (pulseTimer+i)%pulseTimerMax
			val s = sin(PI*(ppu.toDouble()/pulseTimerMax))
			val scale = (baseScale+scaleVariance*s).coerceAtLeast(1.0)
			chunks[j].scale = listOf(1f, scale.toFloat())
		}
	}

	fun modifyValues(pulseFrames:Int, pulseBaseScale:Float, pulseScaleVariance:Float, _upOdd:Boolean) {
		upOdd = _upOdd
		pulseTimerMax = pulseFrames
		baseScale = pulseBaseScale
		scaleVariance = pulseScaleVariance
		if(pulseTimer>pulseTimerMax) pulseTimer = pulseTimerMax
	}
	/**
	 * Resets the background to its base state.
	 */
	override fun reset() {
		pulseTimer = pulseTimerMax
		update()
	}

	override fun draw(render:AbstractRenderer) {
		chunks.forEach {i ->
			val pos = i.drawLocation
			val ddim = i.drawDimensions
			val sloc = i.sourceLocation
			val sdim = i.sourceDimensions
			img.draw(pos[0], pos[1], ddim[0], ddim[1], sloc[0], sloc[1], sdim[0], sdim[1])
		}
	}

	companion object {
		private const val SCREEN_WIDTH = 640
		private const val SCREEN_HEIGHT = 480
		private const val DEFAULT_COLUMN_WIDTH = 1
		private const val DEFAULT_TIMER_MAX = 30
		private const val UP_ODD_DEFAULT = true
		private const val BASE_SCALE = 1f
		private const val SCALE_VARIANCE = 0.1f
	}

}
