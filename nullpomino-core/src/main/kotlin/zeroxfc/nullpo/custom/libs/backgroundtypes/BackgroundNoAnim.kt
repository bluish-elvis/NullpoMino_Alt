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

class BackgroundNoAnim:AnimatedBackgroundHook {
	/**
	 * Almost redundant background.
	 */
	constructor(bgNumber:Int) {
		var bgNumber = bgNumber
		if(bgNumber<0||bgNumber>19) bgNumber = 0
		customHolder = ResourceHolderCustomAssetExtension()
		customHolder.loadImage("res/graphics/back$bgNumber.png", imageName)
		log.debug("Non-custom static background ($bgNumber) created.")
	}

	constructor(filePath:String) {
		customHolder = ResourceHolderCustomAssetExtension()
		customHolder.loadImage(filePath, imageName)
		log.debug("Custom static background created (File Path: $filePath).")
	}

	override fun update() {
		// EMPTY
	}

	override fun reset() {
		// EMPTY
	}

	override fun draw(engine:GameEngine) {
		customHolder.drawImage(imageName, 0, 0)
	}

	override fun setBG(bg:Int) {
		customHolder.loadImage("res/graphics/back$bg.png", imageName)
		log.debug("Non-custom static background modified (New BG: $bg).")
	}

	override fun setBG(filePath:String) {
		customHolder.loadImage(filePath, imageName)
		log.debug("Custom static background modified (New File Path: $filePath).")
	}
	/**
	 * Allows the hot-swapping of preloaded BGs from a storage instance of a `ResourceHolderCustomAssetExtension`.
	 *
	 * @param holder Storage instance
	 * @param name   Image name
	 */
	override fun setBGFromHolder(holder:ResourceHolderCustomAssetExtension, name:String) {
		customHolder.putImageAt(holder.getImageAt(name), imageName)
		log.debug("Custom static background modified (New Image Reference: $name).")
	}
	/**
	 * This last one is important. In the case that any of the child types are used, it allows identification.
	 * The identification can be used to allow casting during operations.
	 *
	 * @return Identification number of child class.
	 */
	override val id = ANIMATION_NONE
	// private ResourceHolderCustomAssetExtension customHolder;
	init {
		imageName = ("localBG")
	}
}
