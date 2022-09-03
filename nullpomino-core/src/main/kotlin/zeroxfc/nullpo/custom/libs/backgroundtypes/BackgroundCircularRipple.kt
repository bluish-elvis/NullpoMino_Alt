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

import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.gui.slick.ResourceHolderCustomAssetExtension
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlin.math.sqrt

class BackgroundCircularRipple:AnimatedBackgroundHook {
	private var chunkGrid:Array<Array<ImageChunk>> = emptyArray()
	private var pulseTimerMax = 0
	private var currentPulseTimer = 0
	private var pulseRadii:ArrayList<Int> = ArrayList(0)
	private var pulseCentres:ArrayList<IntArray> = ArrayList(0)
	private var pulseBaseScale = 0f
	private var pulseScaleVariance = 0f
	private var wavelength = 0f
	private var centreX = 0
	private var centreY = 0
	private var waveSpeed = 0

	constructor(bgNumber:Int, cellWidth:Int?, cellHeight:Int?, pulseCentreX:Int?, pulseCentreY:Int?, wavelength:Float,
		waveSpeed:Int, pulseTimerFrames:Int, pulseBaseScale:Float?, pulseScaleVariance:Float?) {
		val num = if(bgNumber in 1..19) bgNumber else 0
		customHolder = ResourceHolderCustomAssetExtension()
		customHolder.loadImage("res/graphics/back$num.png", imageName)
		setup(
			cellWidth, cellHeight, pulseCentreX, pulseCentreY, wavelength, waveSpeed, pulseTimerFrames, pulseBaseScale,
			pulseScaleVariance
		)
		log.debug("Non-custom circular ripple background ($num) created.")
	}

	constructor(filePath:String, cellWidth:Int?, cellHeight:Int?, pulseCentreX:Int?, pulseCentreY:Int?, wavelength:Float,
		waveSpeed:Int, pulseTimerFrames:Int, pulseBaseScale:Float?, pulseScaleVariance:Float?) {
		customHolder = ResourceHolderCustomAssetExtension()
		customHolder.loadImage(filePath, imageName)
		setup(
			cellWidth, cellHeight, pulseCentreX, pulseCentreY, wavelength, waveSpeed, pulseTimerFrames, pulseBaseScale,
			pulseScaleVariance
		)
		log.debug("Custom circular ripple background created (File Path: $filePath).")
	}

	fun modifyValues(waveSpeed:Int?, pulseTimerFrames:Int, pulseCentreX:Int?, pulseCentreY:Int?, wavelength:Float?,
		pulseBaseScale:Float?, pulseScaleVariance:Float?) {
		pulseTimerMax = pulseTimerFrames
		if(pulseBaseScale!=null) this.pulseBaseScale = pulseBaseScale
		if(pulseScaleVariance!=null) this.pulseScaleVariance = pulseScaleVariance
		if(wavelength!=null) this.wavelength = wavelength
		if(pulseCentreX!=null) centreX = pulseCentreX
		if(pulseCentreY!=null) centreY = pulseCentreY
		if(waveSpeed!=null) this.waveSpeed = waveSpeed
		if(currentPulseTimer>pulseTimerMax) currentPulseTimer = pulseTimerMax
	}

	fun resetPulseScaleValues() {
		pulseBaseScale = BASE_SCALE
		pulseScaleVariance = SCALE_VARIANCE
	}

	private fun setup(cellWidth:Int?, cellHeight:Int?, pulseCentreX:Int?, pulseCentreY:Int?, wavelength:Float, waveSpeed:Int,
		pulseFrames:Int, pulseBaseScale:Float?, pulseScaleVariance:Float?) {
		var wavelength:Float? = wavelength
		var waveSpeed:Int? = waveSpeed
		pulseTimerMax = pulseFrames
		currentPulseTimer = pulseTimerMax
		if(pulseBaseScale==null||pulseScaleVariance==null||pulseCentreX==null||pulseCentreY==null||wavelength==null||waveSpeed==null||cellWidth==null||cellHeight==null) {
			chunkGrid = Array(DEF_GRID_HEIGHT) {y ->
				Array(DEF_GRID_WIDTH) {x ->
					ImageChunk(
						ImageChunk.ANCHOR_POINT_MM, intArrayOf(
							DEF_FIELD_DIM*x+DEF_FIELD_DIM/2, DEF_FIELD_DIM*y+DEF_FIELD_DIM/2
						), intArrayOf(
							DEF_FIELD_DIM*x, DEF_FIELD_DIM*y
						), intArrayOf(DEF_FIELD_DIM, DEF_FIELD_DIM), floatArrayOf(BASE_SCALE, BASE_SCALE)
					)
				}
			}
		} else {
			if(wavelength<=0) wavelength = DEF_WAVELENGTH
			if(waveSpeed<=0) waveSpeed = DEF_WAVESPEED
			this.pulseBaseScale = pulseBaseScale
			this.pulseScaleVariance = pulseScaleVariance
			this.wavelength = wavelength
			centreX = pulseCentreX
			centreY = pulseCentreY
			this.waveSpeed = waveSpeed
			val w:Int = if(640%cellWidth!=0) 8 else 640/cellWidth
			val h:Int = if(480%cellHeight!=0) 8 else 480/cellHeight
			chunkGrid = Array(h) {y ->
				Array(w) {x ->
					ImageChunk(
						ImageChunk.ANCHOR_POINT_MM,
						intArrayOf(cellWidth*x+cellWidth/2, cellHeight*y+cellHeight/2), intArrayOf(
							cellWidth*x, cellHeight*y
						), intArrayOf(cellWidth, cellHeight), floatArrayOf(pulseBaseScale, pulseBaseScale)
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
			pulseCentres.add(
				intArrayOf(
					(centreX), (centreY)
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
				val cx = pulseCentres[i][0]
				val cy = pulseCentres[i][1]
				val cr = pulseRadii[i]
				for(imageChunks in chunkGrid) {
					for(imageChunk in imageChunks) {
						val anch = imageChunk.anchorLocation
						val cellAnchorX = anch[0]
						val cellAnchorY = anch[1]
						val distanceX = abs(cellAnchorX-cx).toDouble()
						val distanceY = abs(cellAnchorY-cy).toDouble()
						val dTotal = sqrt(distanceX*distanceX+distanceY*distanceY)
						if(almostEqual(dTotal, cr.toDouble(), wl.toDouble())&&dTotal>=0) {
							val usedDistance = dTotal-cr
							val sinVal = sin(PI*(usedDistance/wl))
							var newScale = imageChunk.scale[0]+sinVal*scaleVariance
							if(newScale<1.0) newScale = 1.0
							imageChunk.scale = floatArrayOf(newScale.toFloat(), newScale.toFloat())
						} else if(pulseRadii.size<=1) {
							imageChunk.scale = floatArrayOf(baseScale, baseScale)
						}
					}
				}
			}
		}

		// pulseRadii.removeAll(integer -> (integer > MAX_RADIUS));
		for(i in pulseRadii.indices.reversed()) {
			if(pulseRadii[i]>MAX_RADIUS) {
				pulseRadii.removeAt(i)
				pulseCentres.removeAt(i)
			}
		}
	}

	fun manualRipple(x:Int, y:Int) {
		pulseRadii.add(0)
		pulseCentres.add(intArrayOf(x, y))
	}

	override fun reset() {
		pulseRadii = ArrayList()
		pulseCentres = ArrayList()
		currentPulseTimer = pulseTimerMax
		update()
	}

	override fun draw(engine:GameEngine) {
		val priorityList = ArrayList<ImageChunk>()
		for(imageChunks in chunkGrid) {
			priorityList.addAll(imageChunks)
		}
		priorityList.sortWith {c1:ImageChunk, c2:ImageChunk ->
			c1.scale[0].compareTo(c2.scale[0])
		}
		if(almostEqual(pulseBaseScale.toDouble(), 1.0, 0.005)) {
			customHolder.drawImage(imageName, 0, 0)
			priorityList.removeAll {imageChunk:ImageChunk ->
				almostEqual(
					imageChunk.scale[0].toDouble(), 1.0, 0.005
				)
			}
		}
		for(i in priorityList) {
			val pos = i.drawLocation
			val ddim = i.drawDimensions
			val sloc = i.sourceLocation
			val sdim = i.sourceDimensions
			customHolder.drawImage(
				imageName, pos[0], pos[1], ddim[0], ddim[1], sloc[0], sloc[1], sdim[0], sdim[1],
				255, 255, 255, 255
			)
		}
	}

	override fun setBG(bg:Int) {
		customHolder.loadImage("res/graphics/back$bg.png", imageName)
		log.debug("Non-custom circular ripple background modified (New BG: $bg).")
	}

	override fun setBG(filePath:String) {
		customHolder.loadImage(filePath, imageName)
		log.debug("Custom circular ripple background modified (New File Path: $filePath).")
	}
	/**
	 * Allows the hot-swapping of preloaded BGs from a storage instance of a `ResourceHolderCustomAssetExtension`.
	 *
	 * @param holder Storage instance
	 * @param name   Image name
	 */
	override fun setBGFromHolder(holder:ResourceHolderCustomAssetExtension, name:String) {
		customHolder.putImageAt(holder.getImageAt(name), imageName)
		log.debug("Custom circular ripple background modified (New Image Reference: $name).")
	}

	companion object {
		private const val DEF_FIELD_DIM = 8
		private const val DEF_GRID_WIDTH = 640/DEF_FIELD_DIM
		private const val DEF_GRID_HEIGHT = 480/DEF_FIELD_DIM
		private const val DEF_PULSE_CENTRE_X = 640/2
		private const val DEF_PULSE_CENTRE_Y = 480/2
		private const val DEF_WAVESPEED = 8
		private const val MAX_RADIUS = 960
		private const val DEF_WAVELENGTH = 80f
		private const val BASE_SCALE = 1f
		private const val SCALE_VARIANCE = 1f
	}

	override val id:Int = ANIMATION_CIRCULAR_RIPPLE

	init {
		pulseRadii = ArrayList()
		pulseCentres = ArrayList()
		imageName = "localBG"
	}
}
