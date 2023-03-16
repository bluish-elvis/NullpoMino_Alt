/*
 Copyright (c) 2023,
 This library class was created by 0xFC963F18DC21 / Shots243
 It is part of an extension library for the game NullpoMino (copyright 2010-2023)

 Kotlin converted and modified by Venom=Nhelv

 Herewith shall the term "Library Creator" be given to 0xFC963F18DC21.
 Herewith shall the term "Game Creator" be given to the original creator of NullpoMino, NullNoname.

 THIS LIBRARY AND MODE PACK WAS NOT MADE IN ASSOCIATION WITH THE GAME CREATOR.

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
import mu.nu.nullpo.gui.slick.ResourceHolderCustomAssetExtension
import zeroxfc.nullpo.custom.libs.Interpolation
import zeroxfc.nullpo.custom.libs.MathHelper.almostEqual
import kotlin.math.abs
import kotlin.random.Random

class BackgroundTGM3StyleNoRotation:AnimatedBackgroundHook {
	private var holderType = 0
	private val sizeX:Double
	private val sizeY:Double

	/**
	 * Inside each instance:
	 *
	 * @param angle - angle  (current / limit)
	 * @param scale - scale (current / limit)
	 * @param frame - frame (timer / limit)
	 */
	private data class ValueWrapper(var angle:Double = MIN_ANGLE, var scale:Float = MIN_SCALE, var frame:Int = MIN_TRAVEL_TIME) {
		fun replace(it:ValueWrapper) {
			angle = it.angle
			scale = it.scale
			frame = it.frame
		}
	}

	private var lastValues = ValueWrapper()
	private var currentValues = ValueWrapper()
	private var targetValues = ValueWrapper()
	private var valueRandomizer:Random
	/**
	 * Panning amount variables.
	 */
	private var lastPan:IntArray
	private var currentPan:IntArray
	private var targetPan:IntArray
	private var dimTimer = 0
	private var localPath:String
	private var hasUpdated = false

	constructor(filePath:String, valueRandomizer:Random) {
		customHolder = ResourceHolderCustomAssetExtension()
		customHolder.loadImage(filePath, imageName)
		customHolder.loadImage("res/graphics/blank_black.png", "blackBG")
		localPath = filePath
		this.valueRandomizer = valueRandomizer
		val imgDim = customHolder.getImageDimensions(imageName)
		// if (holderType == HOLDER_SLICK) customHolder.setRotationCenter(imageName,(float)imgDim[0] / 2, (float)imgDim[1] / 2);
		sizeX = imgDim[0].toDouble()
		sizeY = imgDim[1].toDouble()
		reset()
		log.debug("TGM3-Style background created (File Path: $filePath).")
	}

	constructor(filePath:String, seed:Long) {
		customHolder = ResourceHolderCustomAssetExtension()
		customHolder.loadImage(filePath, imageName)
		customHolder.loadImage("res/graphics/blank_black.png", "blackBG")
		localPath = filePath
		valueRandomizer = Random(seed)
		val imgDim = customHolder.getImageDimensions(imageName)
		// if (holderType == HOLDER_SLICK) customHolder.setRotationCenter(imageName,(float)imgDim[0] / 2, (float)imgDim[1] / 2);
		sizeX = imgDim[0].toDouble()
		sizeY = imgDim[1].toDouble()
		reset()
		log.debug("TGM3-Style background created (File Path: $filePath).")
	}
	/**
	 * Sets a new panning location for the background image.
	 * Rather complex and somewhat heavy.
	 */
	private fun setNewTarget() {
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
		val imgDim = customHolder.getImageDimensions(imageName)
		targetValues.angle = (valueRandomizer.nextDouble()*(MAX_ANGLE-MIN_ANGLE))+MIN_ANGLE
		// Set new scale
		var ns:Float
		do {
			ns = (valueRandomizer.nextDouble()*(MAX_SCALE-MIN_SCALE)).toFloat()+MIN_SCALE
		} while(!almostEqual(ns.toDouble(), currentValues.scale.toDouble(), 1.0))
		targetValues.scale = ns

		// Find max pan from center
		// int[] imgDim = customHolder.getImageDimensions(imageName);

		// if (holderType == HOLDER_SLICK) {
		// 	differences = new int[] { (int)minOf(imgDim[0] * ns, imgDim[1] * ns) - 640, (int)minOf(imgDim[0] * ns, imgDim[1] * ns) - 480 };
		// } else {
		val differences = listOf((imgDim[0]*ns-640).toInt()/2, (imgDim[1]*ns-480).toInt()/2)
		// }

		// Set new target pan
		// double r = (differences[0] * differences[1]) / Math.sqrt( (differences[0] * differences[0] * Math.sin(targetValues.angle) * Math.sin(targetValues.angle)) + (differences[1] * differences[1] * Math.cos(targetValues.angle) * Math.cos(targetValues.angle)) );
		// do {
		val coefficientX:Double = (valueRandomizer.nextDouble()-0.5)*2
		val coefficientY:Double = (valueRandomizer.nextDouble()-0.5)*2
		targetPan[0] = (differences[0]*coefficientX).toInt()
		targetPan[1] = (differences[1]*coefficientY).toInt()
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
			val t = currentValues.frame.toDouble()/targetValues.frame.toDouble()

			/* if (holderType == HOLDER_SLICK) {
			// 	currentValues.angle = Interpolation.sineStep(lastValues.angle, targetValues.angle, t);
			// 	customHolder.setRotation(imageName, currentValues.angle.floatValue());
			 }*/
			currentValues.scale = Interpolation.sineStep(
				lastValues.scale.toDouble(),
				targetValues.scale.toDouble(), t
			)
				.toFloat()

			// int[] imgDim = customHolder.getImageDimensions(imageName);
			// size = (imgDim[1] * Math.sin(Math.toRadians(currentValues.angle))) + (imgDim[0] * Math.cos(Math.toRadians(currentValues.angle)));
			// sizeY = (imgDim[1] * Math.cos(Math.toRadians(currentValues.angle))) + (imgDim[0] * Math.sin(Math.toRadians(currentValues.angle)));
			// size *= currentValues.scale;
			// sizeY *= currentValues.scale;
			currentPan[0] = Interpolation.sineStep(lastPan[0].toDouble(), targetPan[0].toDouble(), t).toInt()
			currentPan[1] = Interpolation.sineStep(lastPan[1].toDouble(), targetPan[1].toDouble(), t).toInt()
		}
		if(dimTimer>0) changeImage()
	}

	private fun changeImage() {
		dimTimer--
		if(dimTimer==15) {
			val dim = customHolder.getImageDimensions("transitory")
			customHolder.copyImage("transitory", imageName)
			customHolder.setRotationCenter(imageName, dim[0]/2f, dim[1]/2f)
			reset()
		}
	}

	override fun reset() {
		if(hasUpdated) {
			lastValues = ValueWrapper()
			currentValues = ValueWrapper()
			currentValues.scale = MIN_SCALE
			targetValues = ValueWrapper()
			lastValues.scale = 1f
			currentValues.scale = MIN_SCALE
			targetValues.scale = 1f
			lastPan = IntArray(2)
			currentPan = IntArray(2)
			targetPan = IntArray(2)
			customHolder.setRotation(imageName, 0f)
			setNewTarget()
			hasUpdated = false
		}
	}

	override fun draw(engine:GameEngine) {
		customHolder.drawImage("blackBG", 0, 0)
		val rawImgDim = customHolder.getImageDimensions(imageName)
		val imgDim = customHolder.getImageDimensions(imageName).map {it*(currentValues.scale).toInt()}

		// log.debug("%d, %d".format(imgDim[0], imgDim[1]));
		var v = 255
		if(dimTimer>0) {
			val t = dimTimer-15
			v = Interpolation.lerp(0, 255, abs(t).toDouble()/15.0)
		}

		// Calculate the new "size" where it is basically the size of the smallest non-spined rectangle that can inscribe the new image

		customHolder.drawImage(
			imageName, currentPan[0]+320-imgDim[0]/2, currentPan[1]+240-imgDim[1]/2, imgDim[0],
			imgDim[1], 0, 0, rawImgDim[0], rawImgDim[1], v, v, v, 255
		)
	}

	override fun setBG(bg:Int) {
		log.warn(
			"TGM3-Style backgrounds do not support the default backgrounds due to their small size."
		)
		log.info("Minimum recommended size: 1024 x 1024.")
	}

	override fun setBG(filePath:String) {
		if(filePath!=localPath) {
			val dimOld = customHolder.getImageDimensions(imageName)
			customHolder.loadImage(filePath, "transitory")
			val dim = customHolder.getImageDimensions("transitory")
			if(dimOld[0]!=dim[0]||dimOld[1]!=dim[1]) {
				log.warn(
					"Using differently-sized backgrounds stop seamless transitions from occurring."
				)
			}
			if(dim[0]<1024||dim[1]<1024) {
				// Too small.
				log.warn("Background size is smaller than recommended minimum size.")
				log.info("Minimum recommended size: 1024 x 1024.")
			} else {
				// Successful.
				dimTimer = 30
				localPath = filePath
				log.debug("TGM3-Sytle background modified (New File Path: $filePath).")
			}
		}
	}
	/**
	 * Allows the hot-swapping of preloaded BGs from a storage instance of a `ResourceHolderCustomAssetExtension`.
	 *
	 * @param holder Storage instance
	 * @param name   Image name
	 */
	override fun setBGFromHolder(holder:ResourceHolderCustomAssetExtension, name:String) {
		val image = holder.getImageAt(name) ?: return
		if(name!=localPath) {
			val dimOld = customHolder.getImageDimensions(imageName)
			customHolder.putImageAt(image, "transitory")
			val dim = customHolder.getImageDimensions("transitory")
			if(dimOld[0]!=dim[0]||dimOld[1]!=dim[1]) {
				log.warn(
					"Using differently-sized backgrounds stop seamless transitions from occurring."
				)
			}
			if(dim[0]<1024||dim[1]<1024) {
				// Too small.
				log.warn("Background size is smaller than recommended minimum size.")
				log.info("Minimum recommended size: 1024 x 1024.")
			} else {
				// Successful.
				dimTimer = 30
				localPath = name
				log.debug("TGM3-Sytle background modified (New Image Name: $name).")
			}
		}
	}
	/**
	 * This last one is important. In the case that any of the child types are used, it allows identification.
	 * The identification can be used to allow casting during operations.
	 *
	 * @return Identification number of child class.
	 */
	override val id:Int = ANIMATION_TGM3TI_STYLE

	companion object {
		/*
     * note: screw slick's image rotation function.
     */
		// private static final int MAX_ROTATED_SCREEN_REQUIREMENT = (int)Math.ceil(Math.sin(45) * (640 + 480));
		private const val MIN_ANGLE = -60.0
		private const val MAX_ANGLE = 60.0
		private const val MIN_TRAVEL_TIME = 600
		private const val MAX_TRAVEL_TIME = 1800
		private const val MIN_SCALE = 1.5f
		private const val MAX_SCALE = 4f
	}

	init {
		imageName = ("localBG")
		holderType = resourceHook
		lastValues = ValueWrapper()
		currentValues = ValueWrapper()
		currentValues.scale = MIN_SCALE
		targetValues = ValueWrapper()
		lastPan = IntArray(2)
		currentPan = IntArray(2)
		targetPan = IntArray(2)
		dimTimer = 0
		hasUpdated = true
	}
}
