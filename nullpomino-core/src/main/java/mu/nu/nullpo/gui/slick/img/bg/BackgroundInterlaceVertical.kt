package mu.nu.nullpo.gui.slick.img.bg

import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.gui.slick.ResourceHolderCustomAssetExtension
import kotlin.math.sin

class BackgroundInterlaceVertical:AnimatedBackgroundHook {
	private var chunks:Array<ImageChunk> = emptyArray()
	private var pulseTimer = 0
	private var pulseTimerMax = 0
	private var columnWidth = 0
	private var baseScale = 0f
	private var scaleVariance = 0f
	private var upOdd = false
	private var reverse = false

	constructor(bgNumber:Int, columnWidth:Int, pulseTimerFrames:Int, pulseBaseScale:Float, pulseScaleVariance:Float,
		upOdd:Boolean, reverse:Boolean) {
		var bgNumber = bgNumber
		if(bgNumber<0||bgNumber>19) bgNumber = 0
		customHolder.loadImage("res/graphics/back$bgNumber.png", imageName)
		setup(columnWidth, pulseTimerFrames, pulseBaseScale, pulseScaleVariance, upOdd, reverse)
		log.debug("Non-custom vertical interlace background ($bgNumber) created.")
	}

	constructor(filePath:String, columnWidth:Int, pulseTimerFrames:Int, pulseBaseScale:Float, pulseScaleVariance:Float,
		upOdd:Boolean, reverse:Boolean) {
		customHolder.loadImage(filePath, imageName)
		setup(columnWidth, pulseTimerFrames, pulseBaseScale, pulseScaleVariance, upOdd, reverse)
		log.debug("Custom vertical interlace background created (File Path: $filePath).")
	}

	private fun setup(columnWidth:Int = DEFAULT_COLUMN_WIDTH, pulseTimerFrames:Int = DEFAULT_TIMER_MAX,
		pulseBaseScale:Float = BASE_SCALE, pulseScaleVariance:Float = SCALE_VARIANCE, upOdd:Boolean = UP_ODD_DEFAULT,
		reverse:Boolean = false) {
		this.upOdd = !upOdd
		pulseTimerMax = pulseTimerFrames
		baseScale = pulseBaseScale
		scaleVariance = pulseScaleVariance
		pulseTimer = pulseTimerFrames
		this.reverse = reverse
		this.columnWidth = if(480%columnWidth!=0) DEFAULT_COLUMN_WIDTH else columnWidth
		chunks = Array(SCREEN_WIDTH/columnWidth) {i ->
			val up = upOdd&&i%2==1
			val anchorType:Int = if(up) ImageChunk.ANCHOR_POINT_LL else ImageChunk.ANCHOR_POINT_TL
			val anchorLocation = intArrayOf(i*columnWidth, if(up) SCREEN_HEIGHT else 0)
			val srcLocation = intArrayOf(i*columnWidth, 0)
			ImageChunk(anchorType, anchorLocation, srcLocation, intArrayOf(columnWidth, 480), floatArrayOf(1f, baseScale))
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
			var j = i
			if(reverse) j = chunks.size-1-i
			val ppu = (pulseTimer+i)%pulseTimerMax
			val s = sin(Math.PI*(ppu.toDouble()/pulseTimerMax))
			var scale = baseScale+scaleVariance*s
			if(scale<1.0) scale = 1.0
			chunks[j].scale = floatArrayOf(1f, scale.toFloat())
		}
	}

	fun modifyValues(pulseFrames:Int, pulseBaseScale:Float, pulseScaleVariance:Float, upOdd:Boolean) {
		this.upOdd = upOdd
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
	/**
	 * Draws the background to the game screen.
	 *
	 * @param engine   Current GameEngine instance
	 * @param playerID Current player ID (1P = 0)
	 */
	override fun draw(engine:GameEngine, playerID:Int) {
		for(i in chunks) {
			val pos = i.drawLocation
			val ddim = i.drawDimensions
			val sloc = i.sourceLocation
			val sdim = i.sourceDimensions
			customHolder.drawImage(imageName, pos[0], pos[1], ddim[0], ddim[1], sloc[0], sloc[1], sdim[0],
				sdim[1], 255, 255, 255, 255)
		}
	}
	/**
	 * Change BG to one of the default ones.
	 *
	 * @param bg New BG number
	 */
	override fun setBG(bg:Int) {
		customHolder.loadImage("res/graphics/back$bg.png", imageName)
		log.debug("Non-custom vertical interlace background modified (New BG: $bg).")
	}
	/**
	 * Change BG to a custom BG using its file path.
	 *
	 * @param filePath File path of new background
	 */
	override fun setBG(filePath:String) {
		customHolder.loadImage(filePath, imageName)
		log.debug("Custom vertical interlace background modified (New File Path: $filePath).")
	}
	/**
	 * Allows the hot-swapping of pre-loaded BGs from a storage instance of a `ResourceHolderCustomAssetExtension`.
	 *
	 * @param holder Storage instance
	 * @param name   Image name
	 */
	override fun setBGFromHolder(holder:ResourceHolderCustomAssetExtension, name:String) {
		customHolder.putImageAt(holder.getImageAt(name), imageName)
		log.debug(
			"Custom vertical interlace background modified (New Image Reference: $name).")
	}
	/**
	 * This last one is important. In the case that any of the child types are used, it allows identification.
	 * The identification can be used to allow casting during operations.
	 *
	 * @return Identification number of child class.
	 */
	override val ID = ANIMATION_INTERLACE_VERTICAL

	companion object {
		private const val SCREEN_WIDTH = 640
		private const val SCREEN_HEIGHT = 480
		private const val DEFAULT_COLUMN_WIDTH = 1
		private const val DEFAULT_TIMER_MAX = 30
		private const val UP_ODD_DEFAULT = true
		private const val BASE_SCALE = 1f
		private const val SCALE_VARIANCE = 0.1f
	}

	init {
		customHolder = ResourceHolderCustomAssetExtension()
		imageName = ("localBG")
	}
}