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
import kotlin.math.abs
import kotlin.math.sin
import kotlin.math.sqrt

class BackgroundCircularRipple<T>(img:ResourceImage<T>, cellWidth:Int? = DEF_GRID_WIDTH, cellHeight:Int? = DEF_GRID_HEIGHT,
	var centerX:Int = DEF_PULSE_CENTER_X, var centerY:Int = DEF_PULSE_CENTER_Y,
	var wavelength:Float = DEF_WAVELENGTH, var waveSpeed:Int = DEF_WAVESPEED,
	var pulseTimerMax:Int = 200, var pulseBaseScale:Float = BASE_SCALE,
	var pulseScaleVariance:Float = SCALE_VARIANCE):AbstractBG<T>(img) {
	private var chunkGrid:List<List<ImageChunk>> = emptyList()
	private var currentPulseTimer = 0
	private var pulseRadii:ArrayList<Int> = ArrayList(0)
	private var pulseCenters:ArrayList<List<Int>> = ArrayList(0)

	init {
		setup(
			cellWidth, cellHeight, centerX, centerY, wavelength, waveSpeed, pulseTimerMax, pulseBaseScale,
			pulseScaleVariance
		)
	}

	fun modifyValues(waveSpeed:Int?, pulseTimerFrames:Int, pulseCenterX:Int?, pulseCenterY:Int?, wavelength:Float?,
		pulseBaseScale:Float?, pulseScaleVariance:Float?) {
		pulseTimerMax = pulseTimerFrames
		if(pulseBaseScale!=null) this.pulseBaseScale = pulseBaseScale
		if(pulseScaleVariance!=null) this.pulseScaleVariance = pulseScaleVariance
		if(wavelength!=null) this.wavelength = wavelength
		if(pulseCenterX!=null) centerX = pulseCenterX
		if(pulseCenterY!=null) centerY = pulseCenterY
		if(waveSpeed!=null) this.waveSpeed = waveSpeed
		if(currentPulseTimer>pulseTimerMax) currentPulseTimer = pulseTimerMax
	}

	fun resetPulseScaleValues() {
		pulseBaseScale = BASE_SCALE
		pulseScaleVariance = SCALE_VARIANCE
	}

	private fun setup(cellWidth:Int?, cellHeight:Int?, pulseCenterX:Int?, pulseCenterY:Int?, waveLength:Float, waveSpeed:Int,
		pulseFrames:Int, pulseBaseScale:Float?, pulseScaleVariance:Float?) {
		var wL = waveLength
		var wS = waveSpeed
		pulseTimerMax = pulseFrames
		currentPulseTimer = pulseTimerMax
		if(pulseBaseScale==null||pulseScaleVariance==null||pulseCenterX==null||pulseCenterY==null||cellWidth==null||cellHeight==null) {
			chunkGrid = List(DEF_GRID_HEIGHT) {y ->
				List(DEF_GRID_WIDTH) {x ->
					ImageChunk(
						AnchorPoint.MM, listOf(DEF_FIELD_DIM*x+DEF_FIELD_DIM/2, DEF_FIELD_DIM*y+DEF_FIELD_DIM/2),
						listOf(DEF_FIELD_DIM*x, DEF_FIELD_DIM*y), listOf(DEF_FIELD_DIM, DEF_FIELD_DIM), listOf(BASE_SCALE, BASE_SCALE)
					)
				}
			}
		} else {
			if(wL<=0) wL = DEF_WAVELENGTH
			if(wS<=0) wS = DEF_WAVESPEED
			this.pulseBaseScale = pulseBaseScale
			this.pulseScaleVariance = pulseScaleVariance
			this.wavelength = wL
			centerX = pulseCenterX
			centerY = pulseCenterY
			this.waveSpeed = wS
			val w:Int = if(640%cellWidth!=0) 8 else 640/cellWidth
			val h:Int = if(480%cellHeight!=0) 8 else 480/cellHeight
			chunkGrid = List(h) {y ->
				List(w) {x ->
					ImageChunk(
						AnchorPoint.MM,
						listOf(cellWidth*x+cellWidth/2, cellHeight*y+cellHeight/2),
						listOf(cellWidth*x, cellHeight*y), listOf(cellWidth, cellHeight), listOf(pulseBaseScale, pulseBaseScale)
					)
				}
			}
		}
	}

	override fun update() {
		if(pulseTimerMax>=0) currentPulseTimer++
		if(pulseTimerMax in 0..currentPulseTimer) {
			currentPulseTimer = 0
			pulseRadii.add(0)
			pulseCenters.add(
				listOf(
					(centerX), (centerY)
				)
			)
		}
		val ws:Int = waveSpeed
		val baseScale = pulseBaseScale
		val scaleVariance = pulseScaleVariance
		val wl = wavelength
		if(pulseRadii.size>0) {
			for(i in pulseRadii.indices) {
				pulseRadii[i] = pulseRadii[i]+ws
				val cx = pulseCenters[i][0]
				val cy = pulseCenters[i][1]
				val cr = pulseRadii[i]
				chunkGrid.forEach {imageChunks ->
					imageChunks.forEach {
						val anch = it.anchorLocation
						val cellAnchorX = anch[0]
						val cellAnchorY = anch[1]
						val distanceX = abs(cellAnchorX-cx).toDouble()
						val distanceY = abs(cellAnchorY-cy).toDouble()
						val dTotal = sqrt(distanceX*distanceX+distanceY*distanceY)
						if(almostEqual(dTotal, cr.toDouble(), wl.toDouble())&&dTotal>=0) {
							val usedDistance = dTotal-cr
							val sinVal = sin(PI*(usedDistance/wl))
							var newScale = it.scale[0]+sinVal*scaleVariance
							if(newScale<1.0) newScale = 1.0
							it.scale = listOf(newScale.toFloat(), newScale.toFloat())
						} else if(pulseRadii.size<=1) it.scale = listOf(baseScale, baseScale)
					}
				}
			}
		}

		// pulseRadii.removeAll(integer -> (integer > MAX_RADIUS));
		pulseRadii.indices.reversed().forEach {i ->
			if(pulseRadii[i]>MAX_RADIUS) {
				pulseRadii.removeAt(i)
				pulseCenters.removeAt(i)
			}
		}
	}

	fun manualRipple(x:Int, y:Int) {
		pulseRadii.add(0)
		pulseCenters.add(listOf(x, y))
	}

	override fun reset() {
		pulseRadii = ArrayList()
		pulseCenters = ArrayList()
		currentPulseTimer = pulseTimerMax
		update()
	}

	override fun draw(render: AbstractRenderer) {
		val priorityList = chunkGrid.flatten().sortedBy {it.scale[0]}.toMutableList()
		if(almostEqual(pulseBaseScale.toDouble(), 1.0, 0.005)) {
			img.draw()
			priorityList.removeAll {almostEqual(it.scale[0].toDouble(), 1.0, 0.005)}
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
		private const val DEF_FIELD_DIM = 8
		private const val DEF_GRID_WIDTH = 640/DEF_FIELD_DIM
		private const val DEF_GRID_HEIGHT = 480/DEF_FIELD_DIM
		private const val DEF_PULSE_CENTER_X = 640/2
		private const val DEF_PULSE_CENTER_Y = 480/2
		private const val DEF_WAVESPEED = 8
		private const val MAX_RADIUS = 960
		private const val DEF_WAVELENGTH = 80f
		private const val BASE_SCALE = 1f
		private const val SCALE_VARIANCE = 1f
	}

}
