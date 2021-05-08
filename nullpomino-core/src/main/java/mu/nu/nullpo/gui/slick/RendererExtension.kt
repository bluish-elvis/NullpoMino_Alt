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
package mu.nu.nullpo.gui.slick

import mu.nu.nullpo.game.component.Block
import mu.nu.nullpo.game.component.Piece
import mu.nu.nullpo.game.event.EventReceiver
import mu.nu.nullpo.gui.common.EffectObject
import mu.nu.nullpo.gui.slick.*
import org.apache.log4j.Logger
import java.lang.reflect.Field
import kotlin.math.max

object RendererExtension {
	/**
	 * Block break effect types used in addBlockBreakEffect()
	 */
	const val TYPE_NORMAL_BREAK = 1
	const val TYPE_GEM_BREAK = 2
	/**
	 * Text alignment option
	 */
	const val ALIGN_TOP_LEFT = 0
	private const val ALIGN_TOP_MIDDLE = 1
	private const val ALIGN_TOP_RIGHT = 2
	private const val ALIGN_MIDDLE_LEFT = 3
	private const val ALIGN_MIDDLE_MIDDLE = 4
	private const val ALIGN_MIDDLE_RIGHT = 5
	private const val ALIGN_BOTTOM_LEFT = 6
	private const val ALIGN_BOTTOM_MIDDLE = 7
	private const val ALIGN_BOTTOM_RIGHT = 8
	/**
	 * Debug logger
	 */
	private val log = Logger.getLogger(RendererExtension::class.java)
	/**
	 * Run class in debug mode?
	 */
	private const val DEBUG = true
	/**
	 * Draw a custom-scaled piece that does not have to have a scale of 0.5f, 1f or 2f.<br></br>
	 * <br></br>
	 * It can be aligned to any corner, side midpoint or centre of its bounding box.
	 *
	 * @param receiver  Renderer to draw with
	 * @param x         X-coordinate of piece's top-left corner
	 * @param y         Y-coordinate of piece's top-left corner
	 * @param alignment Alignment setting ID (use the ones in this class)
	 * @param piece     The piece to draw
	 * @param scale     Scale factor at which the piece is drawn in
	 * @param darkness  Darkness value (0f = None, negative = lighter, positive = darker)
	 */
	fun drawAlignedScaledPiece(receiver:EventReceiver?, x:Int, y:Int, alignment:Int, piece:Piece, scale:Float, darkness:Float) {
		val baseSize:Int = 16*max(piece.width, piece.height)
		var offsetX:Int = when(alignment) {
			ALIGN_TOP_MIDDLE, ALIGN_MIDDLE_MIDDLE, ALIGN_BOTTOM_MIDDLE -> (baseSize*0.5f*scale).toInt()
			ALIGN_TOP_RIGHT, ALIGN_MIDDLE_RIGHT, ALIGN_BOTTOM_RIGHT -> (baseSize*scale).toInt()
			else -> 0
		}
		var offsetY:Int = when(alignment) {
			ALIGN_MIDDLE_LEFT, ALIGN_MIDDLE_MIDDLE, ALIGN_MIDDLE_RIGHT -> (baseSize*0.5f*scale).toInt()
			ALIGN_BOTTOM_LEFT, ALIGN_BOTTOM_MIDDLE, ALIGN_BOTTOM_RIGHT -> (baseSize*scale).toInt()
			else -> 0
		}
		if(piece.big) {
			offsetX *= 2
			offsetY *= 2
		}
		receiver?.drawPiece(x-offsetX, y-offsetY, piece, scale, darkness)
	}
	/**
	 * Add block break effect at custom location given
	 *
	 * @param receiver Current renderer in game
	 * @param x        X-Coordinate of top left corner of 16x16 block
	 * @param y        Y-Coordinate of top left corner of 16x16 block
	 * @param blk      Block to break
	 */
	fun addBlockBreakEffect(receiver:EventReceiver?, x:Int, y:Int, blk:Block?) {
		if(receiver==null||blk==null) return
		addBlockBreakEffect(receiver, if(blk.isGemBlock) 2 else 1, x, y, blk.drawColor)
	}
	/**
	 * Draw a block break effect at a custom position where coordinates stood for the left-hand corner of a block.
	 *
	 * @param receiver   Current renderer in game
	 * @param effectType Block break effect type
	 * @param x          X-Coordinate of top left corner of 16x16 block
	 * @param y          Y-Coordinate of top left corner of 16x16 block
	 * @param color      Effect color
	 */
	private fun addBlockBreakEffect(receiver:EventReceiver?, effectType:Int, x:Int, y:Int, color:Int) {
		if(receiver==null) return
		val local:Class<*> = RendererSlick::class.java
		val effectList:Field
		val list:ArrayList<EffectExtra>
		val show:Boolean = NullpoMinoSlick.propConfig.getProperty("option.showlineeffect", true)

		try {
			if(show) {
				effectList = local.getDeclaredField("effectlist")
				effectList.isAccessible = true

				/*
                 * This should not return anything other than ArrayList<EffectObject>,
                 * as verified in the source code (see RendererSlick.java, RendererSwing.java
                 * and RendererSDL.java).
                 *
                 * Use @SuppressWarnings("unchecked").
                 */
				list = effectList[receiver] as ArrayList<EffectExtra>
				list.add(EffectExtra(effectType, x, y, color))
				effectList[receiver] = list
			}
		} catch(e:Exception) {
			if(DEBUG) log.error("Failed to extract, modify and place back effectList.")
		}
	}

	class EffectExtra(val effectType:Int, x:Int, y:Int, val color:Int):EffectObject(x = x, y = y) {
		override fun update() {
		}

	}
}