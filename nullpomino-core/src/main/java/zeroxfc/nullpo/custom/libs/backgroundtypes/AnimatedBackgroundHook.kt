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
import mu.nu.nullpo.game.play.GameManager
import mu.nu.nullpo.gui.slick.ResourceHolderCustomAssetExtension
import org.apache.log4j.Logger
import kotlin.math.abs

abstract class AnimatedBackgroundHook {
	protected abstract val id:Int
	protected var customHolder:ResourceHolderCustomAssetExtension = ResourceHolderCustomAssetExtension()
	protected var imageName = ""

	/**
	 * Performs an update tick on the background. Advisably used in onLast.
	 */
	abstract fun update()
	/**
	 * Resets the background to its base state.
	 */
	abstract fun reset()
	/**
	 * Draws the background to the game screen.
	 *
	 * @param engine   Current GameEngine instance
	 * @param playerID Current player ID (1P = 0)
	 */
	abstract fun draw(engine:GameEngine, playerID:Int)
	/**
	 * Change BG to one of the default ones.
	 *
	 * @param bg New BG number
	 */
	abstract fun setBG(bg:Int)
	/**
	 * Change BG to a custom BG using its file path.
	 *
	 * @param filePath File path of new background
	 */
	abstract fun setBG(filePath:String)
	/**
	 * Allows the hot-swapping of pre-loaded BGs from a storage instance of a `ResourceHolderCustomAssetExtension`.
	 *
	 * @param holder Storage instance
	 * @param name   Image name
	 */
	abstract fun setBGFromHolder(holder:ResourceHolderCustomAssetExtension, name:String)

	companion object {
		/**
		 * Animation type ID
		 *
		 *
		 * Please add a new one for every new background type made.
		 */
		const val ANIMATION_NONE = 0
		// No animation. Essentially a quick-switchable custom BG.
		const val ANIMATION_FRAME_ANIM = 1
		// Full frame 640x480 animation at up to 60 FPS. Uses a single image.
		const val ANIMATION_IMAGE_SEQUENCE_ANIM = 2
		// Full frame 640x480 animation at up to 60 FPS. Uses a sequence of images.
		const val ANIMATION_PULSE_HORIZONTAL_BARS = 3
		// Pulsating horizontal bars, like waves across a pseudo-fluid.
		const val ANIMATION_PULSE_VERTICAL_BARS = 4
		// Pulsating vertical bars, like waves across a pseudo-fluid.
		const val ANIMATION_CIRCULAR_RIPPLE = 5
		// Droplets on a smooth psudo-liquid.
		const val ANIMATION_DIAGONAL_RIPPLE = 6
		// Droplets on a smooth psudo-liquid.
		const val ANIMATION_SLIDING_TILES = 7
		// NOTE: SDL window handling is gross.
		const val ANIMATION_TGM3TI_STYLE = 8
		// NOTE: Swing and SDL will not be able to use rotations.
		const val ANIMATION_INTERLACE_HORIZONTAL = 9
		// I hope you like Earthbound.
		const val ANIMATION_INTERLACE_VERTICAL = 10
		// I hope you like Earthbound.
		const val ANIMATION_FAKE_SCANLINES = 11 // Fake CRT Scanlines.
		/**
		 * ResourceHolder--- types
		 */
		const val HOLDER_SLICK = 0
		internal val log:Logger = Logger.getLogger(AnimatedBackgroundHook::class.java)
		private var LAST_BG = -1
		private var LAST_FADE_BG = -1
		/**
		 * Gets the current resource holder type.<br></br>
		 * Useful for selecting different renderers, sound engines or input handlers.
		 *
		 * @return Integer that represents the holder type.
		 */
		const val resourceHook = HOLDER_SLICK
		/**
		 * Gets the last valid background number stored in a GameManager instance.
		 *
		 * @param owner GameManager instance to check
		 * @return Background number (0 <= bg < 19)
		 */
		private fun getBGState(owner:GameManager):Int {
			val bg = owner.backgroundStatus.bg
			return if(bg<0||bg>19) LAST_BG else {
				LAST_BG = bg
				bg
			}
		}
		/**
		 * Gets the last valid background number stored in a GameManager instance.
		 *
		 * @param owner GameManager instance to check
		 * @return Background number (0 <= bg < 19)
		 */
		private fun getFadeBGState(owner:GameManager):Int {
			val bg = owner.backgroundStatus.fadebg
			return if(bg<0||bg>19) LAST_FADE_BG else {
				LAST_FADE_BG = bg
				bg
			}
		}
		/**
		 * Disables the current background.
		 *
		 * @param owner GameManager to disable BG in.
		 */
		fun disableDefaultBG(owner:GameManager) {
			getBGState(owner)
			getFadeBGState(owner)
			owner.backgroundStatus.bg = -1
			owner.backgroundStatus.fadebg = -1
		}
		/**
		 * Re-enables the current background.
		 *
		 * @param owner GameManager to re-enable BG in.
		 */
		fun enableDefaultBG(owner:GameManager) {
			owner.backgroundStatus.bg = LAST_BG
			owner.backgroundStatus.fadebg = LAST_FADE_BG
		}
		// Fuzzy equals.
		internal fun almostEqual(a:Double, b:Double, eps:Double):Boolean = abs(a-b)<eps
	}
}