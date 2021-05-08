package mu.nu.nullpo.gui.slick.img.bg

import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.gui.slick.ResourceHolderCustomAssetExtension

class BackgroundImageSequenceAnim(filePaths:Array<String>, frameTime:Int, pingPong:Boolean):AnimatedBackgroundHook() {
	private val frameTime:Int
	private val frameCount:Int
	private val pingPong:Boolean
	private var currentTick = 0
	private var currentFrame = 0
	private var forward = false
	private fun setup() {
		forward = true
		currentFrame = 0
		currentTick = 0
	}
	/**
	 * Performs an update tick on the background. Advisably used in onLast.
	 */
	override fun update() {
		currentTick++
		if(currentTick>=frameTime) {
			currentTick = 0
			if(pingPong) {
				if(forward) currentFrame++ else currentFrame--
				if(currentFrame>=frameCount) {
					currentFrame -= 2
					forward = false
				} else if(currentFrame<0) {
					currentFrame++
					forward = true
				}
			} else {
				currentFrame = (currentFrame+1)%frameCount
			}
		}
	}
	/**
	 * Resets the background to its base state.
	 */
	override fun reset() {
		setup()
	}
	/**
	 * Draws the background to the game screen.
	 *
	 * @param engine   Current GameEngine instance
	 * @param playerID Current player ID (1P = 0)
	 */
	override fun draw(engine:GameEngine, playerID:Int) {
		val i = "frame$currentFrame"
		customHolder.drawImage(i, 0, 0, 640, 480, 0, 0, 640, 480, 255, 255, 255, 255)
	}
	/**
	 * Change BG to one of the default ones.
	 *
	 * @param bg New BG number
	 */
	override fun setBG(bg:Int) {
		log.warn(
			"Image Sequence animation backgrounds do not support this operation. Please create a new instance.")
	}
	/**
	 * Change BG to a custom BG using its file path.
	 *
	 * @param filePath File path of new background
	 */
	override fun setBG(filePath:String) {
		log.warn(
			"Image Sequence animation backgrounds do not support this operation. Please create a new instance.")
	}
	/**
	 * Allows the hot-swapping of pre-loaded BGs from a storage instance of a `ResourceHolderCustomAssetExtension`.
	 *
	 * @param holder Storage instance
	 * @param name   Image name
	 */
	override fun setBGFromHolder(holder:ResourceHolderCustomAssetExtension, name:String) {
		log.warn(
			"Image Sequence animation backgrounds do not support this operation. Please create a new instance.")
	}
	/**
	 * This last one is important. In the case that any of the child types are used, it allows identification.
	 * The identification can be used to allow casting during operations.
	 *
	 * @return Identification number of child class.
	 */
	override val ID:Int = ANIMATION_IMAGE_SEQUENCE_ANIM

	init {
		customHolder = ResourceHolderCustomAssetExtension()
		for(i in filePaths.indices) {
			customHolder.loadImage(filePaths[i], "frame$i")
			val dim = customHolder.getImageDimensions("frame$i")
			if(dim[0]!=640||dim[1]!=480) log.warn(
				"Image at "+filePaths[i]+" is not 640x480. It may not render correctly.")
		}
		this.frameTime = frameTime
		this.pingPong = pingPong
		frameCount = filePaths.size
		setup()
		log.debug("Sequence frame animation background created (Frames: "+filePaths.size+").")
	}
}