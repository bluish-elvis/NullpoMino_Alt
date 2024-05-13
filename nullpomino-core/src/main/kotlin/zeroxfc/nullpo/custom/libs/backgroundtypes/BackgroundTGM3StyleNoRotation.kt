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
import zeroxfc.nullpo.custom.libs.Interpolation
import zeroxfc.nullpo.custom.libs.MathHelper.almostEqual
import kotlin.random.Random

open class BackgroundTGM3StyleNoRotation<T>(img:ResourceImage<T>):AbstractBG<T>(img){
	private val sizeX=img.width
	private val sizeY=img.width

	/**
	 * Inside each instance:
	 *
	 * @param angle - angle  (current / limit)
	 * @param scale - scale (current / limit)
	 * @param frame - frame (timer / limit)
	 */
	private data class ValueWrapper(var angle:Float = 0f, var scale:Float = 1f, var frame:Int = MIN_TRAVEL_TIME) {
		fun replace(it:ValueWrapper) {
			angle = it.angle
			scale = it.scale
			frame = it.frame
		}
		fun reset(){
			angle = 0f
			scale = 1f
			frame = 0
		}
	}

	private val lastValues = ValueWrapper()
	private val currentValues = ValueWrapper()
	private val targetValues = ValueWrapper()
	private var valueRandomizer:Random=Random.Default
	/** Panning amount variables.*/
	private val lastPan=MutableList(2){0f}
	private val currentPan=MutableList(2){0f}
	private val targetPan=MutableList(2){0f}
	private var hasUpdated = false

	init {
		hasUpdated = true
		reset()
	}
	final override fun reset() {
		if(hasUpdated) {
			lastValues.reset()
			currentValues.reset()
			targetValues.reset()
			lastPan.fill(0f)
			currentPan.fill(0f)
			targetPan.fill(0f)
			setNewTarget()
			hasUpdated = false
		}
	}
	/**
	 * Sets a new panning location for the background image.
	 * Rather complex and somewhat heavy.
	 */
	protected open fun setNewTarget() {
		// Set current as last for LERP.
		lastPan[0] = currentPan[0]
		lastPan[1] = currentPan[1]
		lastValues.replace(currentValues)

		// Reset frame timer
		currentValues.frame = 0

		// Set new time limit
		targetValues.frame = valueRandomizer.nextInt(MAX_TRAVEL_TIME-MIN_TRAVEL_TIME+1)+MIN_TRAVEL_TIME

		//  (holderType == HOLDER_SLICK) {
		// 	// Set new rotation
		//
		targetValues.angle = (valueRandomizer.nextFloat()*(MAX_ANGLE-MIN_ANGLE))+MIN_ANGLE
		// Set new scale
		var ns:Float
		do {
			ns = (valueRandomizer.nextFloat()*(MAX_SCALE-MIN_SCALE))+MIN_SCALE
		} while(!almostEqual(ns, currentValues.scale, 1f))
		targetValues.scale = ns

		// Find max pan from center
		// int[] imgDim = customHolder.getImageDimensions(imageName);

		// if (holderType == HOLDER_SLICK) {
		// 	val differences =
		//			listOf(minOf(imgDim.get(0)*ns, imgDim.get(1)*ns) as Int-640, minOf(imgDim.get(0)*ns, imgDim.get(1)*ns) as Int-480)
		// } else {
		val differences = listOf((sizeX*ns-640).toInt()/2, (sizeY*ns-480).toInt()/2)
		// }

		// Set new target pan
		// double r = (differences[0] * differences[1]) / Math.sqrt( (differences[0] * differences[0] * Math.sin(targetValues.angle) * Math.sin(targetValues.angle)) + (differences[1] * differences[1] * Math.cos(targetValues.angle) * Math.cos(targetValues.angle)) );
		// do {
		val coefficientX = (valueRandomizer.nextFloat()-.5f)*2
		val coefficientY = (valueRandomizer.nextFloat()-.5f)*2
		targetPan[0] = (differences[0]*coefficientX)
		targetPan[1] = (differences[1]*coefficientY)
		// } while (
		// 		Math.sqrt(Math.pow(targetPan[0], 2) + Math.pow(targetPan[1], 2)) > r
		// );
	}

	fun setSeed(seed:Long) {
		valueRandomizer = Random(seed)
		reset()
	}

	override fun update() {
		hasUpdated = true
		currentValues.frame++
		if(currentValues.frame>targetValues.frame) {
			currentValues.frame = 0
			setNewTarget()
		} else {
			val t = currentValues.frame.toFloat()/targetValues.frame

			/*if (img == HOLDER_SLICK) {
			 	currentValues.angle = Interpolation.sineStep(lastValues.angle, targetValues.angle, t)
			 	img.setRotation(imageName, currentValues.angle.floatValue())
			 }*/
			currentValues.scale = Interpolation.sineStep(lastValues.scale, targetValues.scale, t)

			// int[] imgDim = customHolder.getImageDimensions(imageName);
			// size = (imgDim[1] * Math.sin(Math.toRadians(currentValues.angle))) + (imgDim[0] * Math.cos(Math.toRadians(currentValues.angle)));
			// sizeY = (imgDim[1] * Math.cos(Math.toRadians(currentValues.angle))) + (imgDim[0] * Math.sin(Math.toRadians(currentValues.angle)));
			// size *= currentValues.scale;
			// sizeY *= currentValues.scale;
			currentPan[0] = Interpolation.sineStep(lastPan[0], targetPan[0], t)
			currentPan[1] = Interpolation.sineStep(lastPan[1], targetPan[1], t)
		}
//		if(dimTimer>0) changeImage()
	}


	override fun draw(render:AbstractRenderer) {
		//customHolder.drawImage("blackBG", 0, 0)
		val imgDim = listOf(sizeX,sizeY).map {it*currentValues.scale}

		// Calculate the new "size" where it is basically the size of the smallest non-spined rectangle that can inscribe the new image

		img.draw(
			 currentPan[0]+320-imgDim[0]/2, currentPan[1]+240-imgDim[1]/2, imgDim[0],
			imgDim[1], 0, 0, sizeX, sizeY
		)
	}

	companion object {
		/*
     * note: screw slick's image rotation function.
     */
		// private static final int MAX_ROTATED_SCREEN_REQUIREMENT = (int)Math.ceil(Math.sin(45) * (640 + 480));
		private const val MIN_ANGLE = -60f
		private const val MAX_ANGLE = 60f
		private const val MIN_TRAVEL_TIME = 600
		private const val MAX_TRAVEL_TIME = 1800
		private const val MIN_SCALE = 1.5f
		private const val MAX_SCALE = 4f
	}

}
