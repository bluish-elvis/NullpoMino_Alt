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

import mu.nu.nullpo.game.component.Block
import mu.nu.nullpo.game.component.Piece
import mu.nu.nullpo.game.event.EventReceiver
import mu.nu.nullpo.game.event.EventReceiver.COLOR
import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.gui.common.ResourceImage
import org.apache.logging.log4j.LogManager
import org.jetbrains.kotlin.utils.keysToMap
import zeroxfc.nullpo.custom.libs.MathHelper.almostEqual

abstract class ExamSpinnerBase private constructor(private val gradeText:String, private val selectedOutcome:Int,
	private val close:Boolean, private val custom:Boolean) {
	companion object {
		/**
		 * Default asset coordinates and sizes.
		 */
		private val SOURCE_DETAILS = listOf(
			listOf(listOf(0f, 0f), listOf(128f, 32f)),
			listOf(listOf(0f, 32f), listOf(128f, 32f)), listOf(listOf(32f, 64f), listOf(64f, 32f)),
			listOf(listOf(0f, 64f), listOf(96f, 32f))
		)
		private val startXs = listOf(
			0, 80, 160, 240 // P F P F
		)
		private const val TravelClose = 320*32
		private val endXs = listOf(
			startXs[0]+TravelClose,
			startXs[1]+TravelClose,
			startXs[2]+TravelClose,
			startXs[3]+TravelClose
		)
		private const val spinDuration = 360
		private var HUGE_O:Piece = Piece(Piece.PIECE_O).apply {
			big = true
			direction = 0
			setColor(Block.COLOR.YELLOW)
		}
		private val log = LogManager.getLogger()
	}

	private val endXs = Companion.endXs.toMutableList()
	abstract var res:ResourceImage<*>
	private var header:String? = null
	private var subheading:String? = null
	private var possibilities:List<String> = emptyList()
	private var clickedBefore = false
	private var lifeTime = 0

	/**
	 * Create a new promo exam graphic.
	 *
	 * @param gLabel       What grades to display
	 * @param result 0 = pass, 1 = fail
	 * @param _close           Was it a close one?
	 */
	constructor(gLabel:String, result:Int, _close:Boolean):this(gLabel, result, _close, false) {
		//resourceImage.load()
		log.debug("Non-custom ExamSpinner object created.")
	}
	/**
	 * Creates a custom spinner. Make sure to fill in all fields. Note: use lowercase "\n" for newlines.
	 *
	 * @param hText          Heading text
	 * @param subLabel      Subheading text
	 * @param gLabel       Grade Qualification text
	 * @param opt   How many possibilities? (Should be length 2; positive in 0, negative in 1)
	 * @param result 0 for first outcome, 1 for second.
	 * @param _close           Was it a close one?
	 */
	constructor(hText:String?, subLabel:String?, gLabel:String, opt:List<String>?, result:Int, _close:Boolean)
		:this(gLabel, result, _close, true) {
		log.debug("Custom ExamSpinner object created.")
		header = hText ?: "PROMOTION\nEXAM"
		subheading = subLabel ?: "QUALIFY\nGRADE"
		possibilities = opt?.let {
			it.takeIf {it.size>=2}?.take(2)
		} ?: listOf("PASS", "FAIL")
	}
	/**
	 * Draws the spinner to the screen.
	 *
	 * @param receiver Renderer to draw with
	 * @param engine   Current `GameEngine` instance
	 * @param flag     Yellow text?
	 */
	@JvmOverloads fun draw(receiver:EventReceiver, engine:GameEngine, flag:Boolean = lifeTime/2%2==0) {
		val baseX = receiver.fieldX(engine)+4f
		val baseY = receiver.fieldY(engine)+52f
		val size = 16
		HUGE_O.setSkin(engine.skin)
		val b = if(flag) 0f else 1f
		val color = if(flag) COLOR.YELLOW else COLOR.WHITE
		if(custom) {
			val splitHeadingText = header!!.split(Regex("\n"))
			val splitSubheadingText = subheading!!.split(Regex("\n"))
			val splitGradeText = gradeText.split(Regex("\n"))
			val splitPossibilityText = List(possibilities.size) {i ->
				possibilities[i].split(Regex("\n"))
			}

			// region MAIN HEADING
			val hbx = baseX+80
			splitHeadingText.forEachIndexed {i, it ->
				GameTextUtilities.drawDirectTextAlign(
					receiver, hbx, baseY+size*i, GameTextUtilities.ALIGN_TOP_MIDDLE, it, color, 1f
				)
			}
			// endregion MAIN HEADING

			// region SUBHEADING
			val shbx = baseY+size*4
			splitSubheadingText.forEachIndexed {i, it ->
				GameTextUtilities.drawDirectTextAlign(
					receiver, hbx, shbx+size*i, GameTextUtilities.ALIGN_TOP_MIDDLE, it, color, 1f
				)
			}
			// endregion SUBHEADING

			// region GRADE
			val gby = baseY+size*9
			splitGradeText.forEachIndexed {i, it ->
				GameTextUtilities.drawDirectTextAlign(
					receiver, hbx, gby+size*i, GameTextUtilities.ALIGN_TOP_MIDDLE, it, color,
					if(splitGradeText.size<=2) 2f else 1f
				)
			}
			// endregion GRADE
			val pby = baseY+size*16
			if(close) {
				if(lifeTime<spinDuration) endXs.forEachIndexed {i, it ->
					splitPossibilityText[i%2].forEachIndexed {x, c ->
						if(it%320<=160) GameTextUtilities.drawDirectTextAlign(
							receiver, baseX+it%320, pby+size*x, GameTextUtilities.ALIGN_MIDDLE_MIDDLE,
							c, if(i%2==0)color else COLOR.COBALT, 1f
						)
					}
				} else if(lifeTime<spinDuration+120) {
					val offset = lifeTime%3-1
					// FailShake
					splitPossibilityText[(selectedOutcome+1)%2].forEachIndexed {i ,it->
						GameTextUtilities.drawDirectTextAlign(
							receiver, hbx, pby+size*i, GameTextUtilities.ALIGN_MIDDLE_MIDDLE, it,
							if(selectedOutcome==0) color else COLOR.COBALT, 1f
						)
					}
					if(lifeTime>=spinDuration+112&&selectedOutcome==0) {
						val height = (lifeTime-spinDuration-113)*2-1
						val width = 3
						receiver.drawPiece(baseX+width*16, baseY+height*16, HUGE_O, 2f, 0f)
					}
				} else {
					if(lifeTime==spinDuration+120)
						receiver.blockBreak(engine, (13..17).keysToMap {(3..7).keysToMap {HUGE_O.block.first()}})

						splitPossibilityText[selectedOutcome].forEachIndexed {i ,it->
							GameTextUtilities.drawDirectTextAlign(
								receiver, hbx, pby+size*i, GameTextUtilities.ALIGN_MIDDLE_MIDDLE, it,
								if(selectedOutcome==0) color else COLOR.COBALT, 1f
							)
						}
				}
			} else if(lifeTime>=60) splitPossibilityText[selectedOutcome].forEachIndexed {i ,it->
				GameTextUtilities.drawDirectTextAlign(
					receiver, hbx, pby+size*i, GameTextUtilities.ALIGN_MIDDLE_MIDDLE, it,
					if(selectedOutcome==0) color else COLOR.COBALT, 1f
				)
			}
		} else {
			val alphas = endXs.map {
				val l = it%320
				val diff = maxOf(0, if(l<=80) l else 80-(l-80))
				Interpolation.sineStep(0f, 1f, diff/80f)
			}
			res.draw(
				baseX+80-SOURCE_DETAILS[0][1][0]/2, baseY, SOURCE_DETAILS[0][1][0],
				SOURCE_DETAILS[0][1][1], SOURCE_DETAILS[0][0][0], SOURCE_DETAILS[0][0][1], SOURCE_DETAILS[0][1][0],
				SOURCE_DETAILS[0][1][1], 1f, Triple(1f, 1f, b)
			)
			res.draw(
				baseX+80-SOURCE_DETAILS[1][1][0]/2, baseY+size*4, SOURCE_DETAILS[1][1][0],
				SOURCE_DETAILS[1][1][1], SOURCE_DETAILS[1][0][0], SOURCE_DETAILS[1][0][1], SOURCE_DETAILS[1][1][0],
				SOURCE_DETAILS[1][1][1], 1f, Triple(1f, 1f, b)
			)
			receiver.drawMenuGrade(engine, 5-gradeText.length, 9, gradeText, color, 2f)

			// NEW CODE GOES HERE.
			if(close) {
				if(lifeTime<spinDuration) {
					res.draw(
						baseX+endXs[0]%320-SOURCE_DETAILS[2][1][0]/2, baseY+size*14,
						SOURCE_DETAILS[2][1][0], SOURCE_DETAILS[2][1][1], SOURCE_DETAILS[2][0][0], SOURCE_DETAILS[2][0][1],
						SOURCE_DETAILS[2][1][0], SOURCE_DETAILS[2][1][1], alphas[0], Triple(1f, 1f, b)
					)
					res.draw(
						baseX+endXs[1]%320-SOURCE_DETAILS[3][1][0]/2, baseY+size*14,
						SOURCE_DETAILS[3][1][0], SOURCE_DETAILS[3][1][1], SOURCE_DETAILS[3][0][0], SOURCE_DETAILS[3][0][1],
						SOURCE_DETAILS[3][1][0], SOURCE_DETAILS[3][1][1], alphas[1], Triple(80/255f, 80/255f, 160/255f)
					)
					res.draw(
						baseX+endXs[2]%320-SOURCE_DETAILS[2][1][0]/2, baseY+size*14,
						SOURCE_DETAILS[2][1][0], SOURCE_DETAILS[2][1][1], SOURCE_DETAILS[2][0][0], SOURCE_DETAILS[2][0][1],
						SOURCE_DETAILS[2][1][0], SOURCE_DETAILS[2][1][1], alphas[2], Triple(255/255f, 255/255f, b)
					)
					res.draw(
						baseX+endXs[3]%320-SOURCE_DETAILS[3][1][0]/2, baseY+size*14,
						SOURCE_DETAILS[3][1][0], SOURCE_DETAILS[3][1][1], SOURCE_DETAILS[3][0][0], SOURCE_DETAILS[3][0][1],
						SOURCE_DETAILS[3][1][0], SOURCE_DETAILS[3][1][1], alphas[3], Triple(80/255f, 80/255f, 160/255f)
					)
				} else if(lifeTime<spinDuration+120) {
					val offset = lifeTime%3-1
					res.draw(
						baseX+80+offset-SOURCE_DETAILS[3][1][0]/2, baseY+size*14,
						SOURCE_DETAILS[3][1][0], SOURCE_DETAILS[3][1][1], SOURCE_DETAILS[3][0][0], SOURCE_DETAILS[3][0][1],
						SOURCE_DETAILS[3][1][0], SOURCE_DETAILS[3][1][1], 1f, Triple(80/255f, 80/255f, 160/255f)
					)
					if(lifeTime>=spinDuration+112) {
						val height = (lifeTime-spinDuration-113)*2-1
						val width = 3
						receiver.drawPiece(baseX+width*16, baseY+height*16, HUGE_O, 1f, 0f)
					}
				} else {
					if(lifeTime==spinDuration+120)
						receiver.blockBreak(engine, (13..17).keysToMap {(3..7).keysToMap {HUGE_O.block.first()}})

					if(selectedOutcome==0) {
						// PASS
						res.draw(
							baseX+80-SOURCE_DETAILS[2][1][0]/2, baseY+size*14,
							SOURCE_DETAILS[2][1][0], SOURCE_DETAILS[2][1][1], SOURCE_DETAILS[2][0][0], SOURCE_DETAILS[2][0][1],
							SOURCE_DETAILS[2][1][0], SOURCE_DETAILS[2][1][1], 1f, Triple(1f, 1f, b)
						)
					} else {
						res.draw(
							baseX+80-SOURCE_DETAILS[3][1][0]/2, baseY+size*14,
							SOURCE_DETAILS[3][1][0], SOURCE_DETAILS[3][1][1], SOURCE_DETAILS[3][0][0], SOURCE_DETAILS[3][0][1],
							SOURCE_DETAILS[3][1][0], SOURCE_DETAILS[3][1][1], 1f, Triple(80/255f, 80/255f, 160/255f)
						)
					}
				}
			} else if(lifeTime>=60) {
				if(selectedOutcome==0) {
					// PASS
					res.draw(
						baseX+80-SOURCE_DETAILS[2][1][0]/2, baseY+size*14,
						SOURCE_DETAILS[2][1][0], SOURCE_DETAILS[2][1][1], SOURCE_DETAILS[2][0][0], SOURCE_DETAILS[2][0][1],
						SOURCE_DETAILS[2][1][0], SOURCE_DETAILS[2][1][1], 1f, Triple(1f, 1f, b)
					)
				} else {
					res.draw(
						baseX+80-SOURCE_DETAILS[3][1][0]/2, baseY+size*14,
						SOURCE_DETAILS[3][1][0], SOURCE_DETAILS[3][1][1], SOURCE_DETAILS[3][0][0], SOURCE_DETAILS[3][0][1],
						SOURCE_DETAILS[3][1][0], SOURCE_DETAILS[3][1][1], 1f, Triple(80/255f, 80/255f, 160/255f)
					)
				}
			}
		}
	}
	/**
	 * Updates the state of the ExamSpinner instance
	 *
	 * @param engine GameEngine to play SE with
	 */
	fun update(engine:GameEngine) {
		lifeTime++
		if(close) when {
			lifeTime==spinDuration+12 -> engine.playSE("shutter")
			lifeTime==spinDuration+120 -> {
				engine.playSE("linefall0")
				engine.playSE(if(selectedOutcome==0) "excellent" else "regret")
			}
			lifeTime<=spinDuration -> {
				val j = lifeTime.toFloat()/spinDuration
				// StringBuilder sb = new StringBuilder();
				// sb.append("[");
				endXs.zip(startXs).forEachIndexed { i, (e,s) ->
					val res = Interpolation.smoothStep(e*1f, s*1f, j, 64f)
					// sb.append(res).append(", ");
					endXs[i] = res.toInt()
				}
				// sb.append("]");
				// log.debug(lifeTime + ": (LOC) " + Arrays.toString(endXs) + ", " + j);
				// log.debug(lifeTime + ": (RAW) " + sb.toString());
				for(i in endXs.indices) {
					if((endXs[i]%320).toFloat().almostEqual(80f, 24f)) {
						if(!clickedBefore) {
							clickedBefore = true
							engine.playSE("change")
						}
						break
					}
					if(i==endXs.size-1) clickedBefore = false
				}
			}
		} else if(lifeTime==60) {
			engine.playSE("linefall0")
			engine.playSE(if(selectedOutcome==0) "excellent" else "regret")
		}

	}

}
