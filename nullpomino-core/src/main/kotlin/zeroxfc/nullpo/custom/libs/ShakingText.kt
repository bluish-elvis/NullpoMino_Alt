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
package zeroxfc.nullpo.custom.libs

import mu.nu.nullpo.game.event.EventReceiver
import mu.nu.nullpo.game.play.GameEngine
import kotlin.random.Random

/**
Generates a text shaker object.

@param textPositionRandomizer `Random` instance to use.
 */

class ShakingText
/**
Generates a text shaker object with an auto-generated randomizer.
 */(private val textPositionRandomizer:Random = Random.Default) {
	/**
	 * Draws some shaken text at some pixel coordinates.
	 *
	 * @param receiver     `EventReceiver` instance to draw on
	 * @param x            X-coordinate (pixel) of the top-left of the text anchor
	 * @param y            Y-coordinate (pixel) of the top-left of the text anchor
	 * @param maxDevianceX Maximum x-pixel-coordinate deviance in text
	 * @param maxDevianceY Maximum y-pixel-coordinate deviance in text
	 * @param text         Text to draw
	 * @param color       Color of text. Use colors from `EventReceiver`
	 * @param scale        Scale of drawn text.
	 */
	private fun drawDirectText(receiver:EventReceiver, x:Int, y:Int, maxDevianceX:Int, maxDevianceY:Int, text:String,
		color:EventReceiver.COLOR, scale:Float) {
		val mDX = if(maxDevianceX<0) -maxDevianceX else maxDevianceX
		val mDY = if(maxDevianceY<0) -maxDevianceY else maxDevianceY
		val offset = 16f*scale
		text.forEachIndexed { i,it ->
			val offsetUsed = (offset*i).toInt()
			var xDiff = 0
			var yDiff = 0
			if(mDX>0) xDiff = textPositionRandomizer.nextInt(mDX*2+1)-mDX
			if(mDY>0) yDiff = textPositionRandomizer.nextInt(mDY*2+1)-mDY
			receiver.drawDirectFont(x+offsetUsed+xDiff, y+yDiff, "$it", color, scale)
		}
	}
	/**
	 * Draws some shaken text relative to the score display position.
	 *
	 * @param receiver     `EventReceiver` instance to draw on
	 * @param engine       Current `GameEngine` instance
	 * @param x            X-coordinate of the top-left of the text anchor
	 * @param y            Y-coordinate of the top-left of the text anchor
	 * @param maxDevianceX Maximum x-pixel-coordinate deviance in text
	 * @param maxDevianceY Maximum y-pixel-coordinate deviance in text
	 * @param text         Text to draw
	 * @param color       Color of text. Use colors from `EventReceiver`
	 * @param scale        Scale of drawn text.
	 */
	fun drawScoreText(receiver:EventReceiver, engine:GameEngine, x:Int, y:Int, maxDevianceX:Int, maxDevianceY:Int, text:String,
		color:EventReceiver.COLOR, scale:Float) {
		val nx = receiver.scoreX(engine)+x*if(scale<=0.5f) 8 else 16
		val ny = receiver.scoreY(engine)+y*if(scale<=0.5f) 8 else 16
		drawDirectText(receiver, nx, ny, maxDevianceX, maxDevianceY, text, color, scale)
	}
	/**
	 * Draws some shaken text relative to the field display position.
	 *
	 * @param receiver     `EventReceiver` instance to draw on
	 * @param engine       Current `GameEngine` instance
	 * @param x            X-coordinate of the top-left of the text anchor
	 * @param y            Y-coordinate of the top-left of the text anchor
	 * @param maxDevianceX Maximum x-pixel-coordinate deviance in text
	 * @param maxDevianceY Maximum y-pixel-coordinate deviance in text
	 * @param text         Text to draw
	 * @param color       Color of text. Use colors from `EventReceiver`
	 * @param scale        Scale of drawn text.
	 */
	fun drawMenuText(receiver:EventReceiver, engine:GameEngine, x:Int, y:Int, maxDevianceX:Int, maxDevianceY:Int, text:String,
		color:EventReceiver.COLOR, scale:Float) {
		val nx = receiver.fieldX(engine).toInt()+x*if(scale<=0.5f) 8 else 16+4
		val ny = receiver.fieldY(engine).toInt()+y*if(scale<=0.5f) 8 else 16+52
		drawDirectText(receiver, nx, ny, maxDevianceX, maxDevianceY, text, color, scale)
	}
}
