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
package zeroxfc.nullpo.custom.libs

import mu.nu.nullpo.game.component.*
import mu.nu.nullpo.game.event.EventReceiver
import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.gui.slick.RendererExtension
import mu.nu.nullpo.gui.slick.ResourceHolderCustomAssetExtension
import org.apache.log4j.Logger

class ExamSpinner {
	companion object {
		/**
		 * Default asset coordinates and sizes.
		 */
		private val SOURCE_DETAILS = arrayOf(arrayOf(intArrayOf(0, 0), intArrayOf(128, 32)),
			arrayOf(intArrayOf(0, 32), intArrayOf(128, 32)), arrayOf(intArrayOf(32, 64), intArrayOf(64, 32)),
			arrayOf(intArrayOf(0, 64), intArrayOf(96, 32)))
		private val startXs = intArrayOf(
			0, 80, 160, 240 // P F P F
		)
		private const val TravelClose = 320*32
		private val endXs = intArrayOf(
			startXs[0]+TravelClose,
			startXs[1]+TravelClose,
			startXs[2]+TravelClose,
			startXs[3]+TravelClose
		)
		private const val spinDuration = 360
		private var HUGE_O:Piece = Piece(Piece.PIECE_O).apply {
			big = true
			setSkin(0)
			direction = 0
			setColor(Block.BLOCK_COLOR_YELLOW)
		}
		private val log = Logger.getLogger(ExamSpinner::class.java)

	}

	private val gradeText:String
	private val selectedOutcome:Int
	private val close:Boolean
	private val custom:Boolean
	private val locations:IntArray
	private var customHolder:ResourceHolderCustomAssetExtension? = null
	private var header:String? = null
	private var subheading:String? = null
	private var possibilities:Array<String> = emptyArray()
	private var clickedBefore = false
	private var lifeTime = 0
	/**
	 * Create a new promo exam graphic.
	 *
	 * @param gradeText       What grade to display
	 * @param selectedOutcome 0 = pass, 1 = fail
	 * @param close           Was it a close one?
	 */
	constructor(gradeText:String?, selectedOutcome:Int, close:Boolean) {
		var gradeText = gradeText
		custom = false
		if(gradeText==null) gradeText = "UNDEFINED"
		customHolder = ResourceHolderCustomAssetExtension()
		customHolder!!.loadImage("res/graphics/examResultText.png", "default")
		log.debug("Non-custom ExamSpinner object created.")
		this.gradeText = gradeText
		this.selectedOutcome = selectedOutcome
		this.close = close
	}
	/**
	 * Creates a custom spinner. Make sure to fill in all fields. Note: use lowercase "\n" for newlines.
	 *
	 * @param header          Heading text
	 * @param subheading      Subheading text
	 * @param gradeText       Grade Qualification text
	 * @param possibilities   How many possibilities? (Should be length 2; positive in 0, negative in 1)
	 * @param selectedOutcome 0 for first outcome, 1 for second.
	 * @param close           Was it a close one?
	 */
	constructor(header:String?, subheading:String?, gradeText:String?, possibilities:Array<String>?, selectedOutcome:Int,
		close:Boolean) {
		var header = header
		var subheading = subheading
		var gradeText = gradeText
		var possibilities = possibilities
		custom = true
		if(header==null) header = "PROMOTION\nEXAM"
		if(subheading==null) subheading = "EXAM\nGRADE"
		if(gradeText==null) gradeText = "UNDEFINED"
		if(possibilities==null) possibilities = arrayOf("PASS", "FAIL")
		if(possibilities.size<2) possibilities = arrayOf("PASS", "FAIL")
		if(possibilities.size>2) possibilities = arrayOf(possibilities[0], possibilities[1])
		log.debug("Custom ExamSpinner object created.")
		this.header = header
		this.subheading = subheading
		this.gradeText = gradeText
		this.possibilities = possibilities
		this.selectedOutcome = selectedOutcome
		this.close = close
	}
	/**
	 * Draws the spinner to the screen.
	 *
	 * @param receiver Renderer to draw with
	 * @param engine   Current `GameEngine` instance
	 * @param playerID Current Player ID (0 = 1P)
	 * @param flag     Yellow text?
	 */
	@JvmOverloads fun draw(receiver:EventReceiver, engine:GameEngine, playerID:Int, flag:Boolean = lifeTime/2%2==0) {
		val baseX:Int = receiver.fieldX(engine, playerID)+4
		val baseY:Int = receiver.fieldY(engine, playerID)+52
		val size = 16
		HUGE_O.setSkin(engine.skin)
		var b = 255
		if(flag) b = 0
		var color:EventReceiver.COLOR = EventReceiver.COLOR.WHITE
		if(flag) color = EventReceiver.COLOR.YELLOW
		if(custom) {
			val splitHeadingText = header!!.split("\n".toRegex()).toTypedArray()
			val splitSubheadingText = subheading!!.split("\n".toRegex()).toTypedArray()
			val splitGradeText = gradeText.split("\n".toRegex()).toTypedArray()
			val splitPossibilityText:Array<Array<String>> = Array(possibilities.size) {i ->
				possibilities[i].split("\n".toRegex()).toTypedArray()
			}

			// region MAIN HEADING
			val HBX = baseX+80
			for(i in splitHeadingText.indices) {
				GameTextUtilities.drawDirectTextAlign(receiver, engine, playerID, HBX, baseY+size*i, GameTextUtilities.ALIGN_TOP_MIDDLE,
					splitHeadingText[i], color, 1f)
			}
			// endregion MAIN HEADING

			// region SUBHEADING
			val SHBY = baseY+size*4
			for(i in splitSubheadingText.indices) {
				GameTextUtilities.drawDirectTextAlign(receiver, engine, playerID, HBX, SHBY+size*i, GameTextUtilities.ALIGN_TOP_MIDDLE,
					splitSubheadingText[i], color, 1f)
			}
			// endregion SUBHEADING

			// region GRADE
			val GBY = baseY+size*9
			for(i in splitGradeText.indices) {
				GameTextUtilities.drawDirectTextAlign(receiver, engine, playerID, HBX, GBY+size*i, GameTextUtilities.ALIGN_TOP_MIDDLE,
					splitGradeText[i], color, if(splitGradeText.size==1) 2f else 1f)
			}
			// endregion GRADE
			val PBY = baseY+size*16
			if(close) {
				if(lifeTime<spinDuration) {
					// Pass1
					for(i in 0 until splitPossibilityText[0].size) {
						if(locations[0]%320<=160) GameTextUtilities.drawDirectTextAlign(receiver, engine, playerID, baseX+locations[0]%320,
							PBY+size*i, GameTextUtilities.ALIGN_MIDDLE_MIDDLE,
							splitPossibilityText[0][i], color, 1f)
					}

					// Fail1
					for(i in 0 until splitPossibilityText[1].size) {
						if(locations[1]%320<=160) GameTextUtilities.drawDirectTextAlign(receiver, engine, playerID, baseX+locations[1]%320,
							PBY+size*i, GameTextUtilities.ALIGN_MIDDLE_MIDDLE,
							splitPossibilityText[1][i], EventReceiver.COLOR.COBALT, 1f)
					}

					// Pass1
					for(i in 0 until splitPossibilityText[0].size) {
						if(locations[2]%320<=160) GameTextUtilities.drawDirectTextAlign(receiver, engine, playerID, baseX+locations[2]%320,
							PBY+size*i, GameTextUtilities.ALIGN_MIDDLE_MIDDLE,
							splitPossibilityText[0][i], color, 1f)
					}

					// Fail1
					for(i in 0 until splitPossibilityText[1].size) {
						if(locations[3]%320<=160) GameTextUtilities.drawDirectTextAlign(receiver, engine, playerID, baseX+locations[3]%320,
							PBY+size*i, GameTextUtilities.ALIGN_MIDDLE_MIDDLE,
							splitPossibilityText[1][i], EventReceiver.COLOR.COBALT, 1f)
					}
				} else if(lifeTime<spinDuration+120) {
					val offset = lifeTime%3-1
					// FailShake
					for(i in 0 until splitPossibilityText[1].size) {
						GameTextUtilities.drawDirectTextAlign(receiver, engine, playerID, HBX+offset, PBY+size*i,
							GameTextUtilities.ALIGN_MIDDLE_MIDDLE,
							splitPossibilityText[1][i], EventReceiver.COLOR.COBALT, 1f)
					}
					if(lifeTime>=spinDuration+112&&selectedOutcome==0) {
						val height = (lifeTime-spinDuration-113)*2-1
						val width = 3
						receiver.drawPiece(baseX+width*16, baseY+height*16, HUGE_O, 1f, 0f)
					}
				} else {
					if(lifeTime==spinDuration+120) {
						val blk = Block(Block.BLOCK_COLOR_YELLOW)
						for(y in 13..17) {
							for(x in 3..7) {
								val x2 = x*16+baseX
								val y2 = y*16+baseY
								RendererExtension.addBlockBreakEffect(receiver, x2, y2, blk)
							}
						}
					}
					if(selectedOutcome==0) {
						// PASS
						for(i in 0 until splitPossibilityText[0].size) {
							GameTextUtilities.drawDirectTextAlign(receiver, engine, playerID, HBX, PBY+size*i,
								GameTextUtilities.ALIGN_MIDDLE_MIDDLE,
								splitPossibilityText[0][i], color, 1f)
						}
					} else {
						for(i in 0 until splitPossibilityText[1].size) {
							GameTextUtilities.drawDirectTextAlign(receiver, engine, playerID, HBX, PBY+size*i,
								GameTextUtilities.ALIGN_MIDDLE_MIDDLE,
								splitPossibilityText[1][i], EventReceiver.COLOR.COBALT, 1f)
						}
					}
				}
			} else if(lifeTime>=60) {
				if(selectedOutcome==0) {
					// PASS
					for(i in 0 until splitPossibilityText[0].size) {
						GameTextUtilities.drawDirectTextAlign(receiver, engine, playerID, HBX, PBY+size*i,
							GameTextUtilities.ALIGN_MIDDLE_MIDDLE,
							splitPossibilityText[0][i], color, 1f)
					}
				} else {
					for(i in 0 until splitPossibilityText[1].size) {
						GameTextUtilities.drawDirectTextAlign(receiver, engine, playerID, HBX, PBY+size*i,
							GameTextUtilities.ALIGN_MIDDLE_MIDDLE,
							splitPossibilityText[1][i], EventReceiver.COLOR.COBALT, 1f)
					}
				}
			}
		} else {
			val alphas = IntArray(locations.size)
			for(i in locations.indices) {
				var diff:Int
				val l = locations[i]%320
				diff = if(l<=80) l else 80-(l-80)
				if(diff<0) diff = 0
				val alpha = Interpolation.sineStep(0.0, 255.0, diff.toDouble()/80.0).toInt()
				alphas[i] = alpha
			}
			customHolder!!.drawImage("default", baseX+80-SOURCE_DETAILS[0][1][0]/2, baseY, SOURCE_DETAILS[0][1][0],
				SOURCE_DETAILS[0][1][1], SOURCE_DETAILS[0][0][0], SOURCE_DETAILS[0][0][1], SOURCE_DETAILS[0][1][0],
				SOURCE_DETAILS[0][1][1], 255, 255, b, 255)
			customHolder!!.drawImage("default", baseX+80-SOURCE_DETAILS[1][1][0]/2, baseY+size*4, SOURCE_DETAILS[1][1][0],
				SOURCE_DETAILS[1][1][1], SOURCE_DETAILS[1][0][0], SOURCE_DETAILS[1][0][1], SOURCE_DETAILS[1][1][0],
				SOURCE_DETAILS[1][1][1], 255, 255, b, 255)
			receiver.drawMenuFont(engine, playerID, 5-gradeText.length, 9, gradeText, color, 2.0f)

			// NEW CODE GOES HERE.
			if(close) {
				if(lifeTime<spinDuration) {
					customHolder!!.drawImage("default", baseX+locations[0]%320-SOURCE_DETAILS[2][1][0]/2, baseY+size*14,
						SOURCE_DETAILS[2][1][0], SOURCE_DETAILS[2][1][1], SOURCE_DETAILS[2][0][0], SOURCE_DETAILS[2][0][1],
						SOURCE_DETAILS[2][1][0], SOURCE_DETAILS[2][1][1], 255, 255, b, alphas[0])
					customHolder!!.drawImage("default", baseX+locations[1]%320-SOURCE_DETAILS[3][1][0]/2, baseY+size*14,
						SOURCE_DETAILS[3][1][0], SOURCE_DETAILS[3][1][1], SOURCE_DETAILS[3][0][0], SOURCE_DETAILS[3][0][1],
						SOURCE_DETAILS[3][1][0], SOURCE_DETAILS[3][1][1], 80, 80, 160, alphas[1])
					customHolder!!.drawImage("default", baseX+locations[2]%320-SOURCE_DETAILS[2][1][0]/2, baseY+size*14,
						SOURCE_DETAILS[2][1][0], SOURCE_DETAILS[2][1][1], SOURCE_DETAILS[2][0][0], SOURCE_DETAILS[2][0][1],
						SOURCE_DETAILS[2][1][0], SOURCE_DETAILS[2][1][1], 255, 255, b, alphas[2])
					customHolder!!.drawImage("default", baseX+locations[3]%320-SOURCE_DETAILS[3][1][0]/2, baseY+size*14,
						SOURCE_DETAILS[3][1][0], SOURCE_DETAILS[3][1][1], SOURCE_DETAILS[3][0][0], SOURCE_DETAILS[3][0][1],
						SOURCE_DETAILS[3][1][0], SOURCE_DETAILS[3][1][1], 80, 80, 160, alphas[3])
				} else if(lifeTime<spinDuration+120) {
					val offset = lifeTime%3-1
					customHolder!!.drawImage("default", baseX+80+offset-SOURCE_DETAILS[3][1][0]/2, baseY+size*14,
						SOURCE_DETAILS[3][1][0], SOURCE_DETAILS[3][1][1], SOURCE_DETAILS[3][0][0], SOURCE_DETAILS[3][0][1],
						SOURCE_DETAILS[3][1][0], SOURCE_DETAILS[3][1][1], 80, 80, 160, 255)
					if(lifeTime>=spinDuration+112) {
						val height = (lifeTime-spinDuration-113)*2-1
						val width = 3
						receiver.drawPiece(baseX+width*16, baseY+height*16, HUGE_O, 1f, 0f)
					}
				} else {
					if(lifeTime==spinDuration+120) {
						val blk = Block(Block.BLOCK_COLOR_YELLOW)
						for(y in 13..17) {
							for(x in 3..7) {
								val x2 = x*16+baseX
								val y2 = y*16+baseY
								RendererExtension.addBlockBreakEffect(receiver, x2, y2, blk)
							}
						}
					}
					if(selectedOutcome==0) {
						// PASS
						customHolder!!.drawImage("default", baseX+80-SOURCE_DETAILS[2][1][0]/2, baseY+size*14,
							SOURCE_DETAILS[2][1][0], SOURCE_DETAILS[2][1][1], SOURCE_DETAILS[2][0][0], SOURCE_DETAILS[2][0][1],
							SOURCE_DETAILS[2][1][0], SOURCE_DETAILS[2][1][1], 255, 255, b, 255)
					} else {
						customHolder!!.drawImage("default", baseX+80-SOURCE_DETAILS[3][1][0]/2, baseY+size*14,
							SOURCE_DETAILS[3][1][0], SOURCE_DETAILS[3][1][1], SOURCE_DETAILS[3][0][0], SOURCE_DETAILS[3][0][1],
							SOURCE_DETAILS[3][1][0], SOURCE_DETAILS[3][1][1], 80, 80, 160, 255)
					}
				}
			} else if(lifeTime>=60) {
				if(selectedOutcome==0) {
					// PASS
					customHolder!!.drawImage("default", baseX+80-SOURCE_DETAILS[2][1][0]/2, baseY+size*14,
						SOURCE_DETAILS[2][1][0], SOURCE_DETAILS[2][1][1], SOURCE_DETAILS[2][0][0], SOURCE_DETAILS[2][0][1],
						SOURCE_DETAILS[2][1][0], SOURCE_DETAILS[2][1][1], 255, 255, b, 255)
				} else {
					customHolder!!.drawImage("default", baseX+80-SOURCE_DETAILS[3][1][0]/2, baseY+size*14,
						SOURCE_DETAILS[3][1][0], SOURCE_DETAILS[3][1][1], SOURCE_DETAILS[3][0][0], SOURCE_DETAILS[3][0][1],
						SOURCE_DETAILS[3][1][0], SOURCE_DETAILS[3][1][1], 80, 80, 160, 255)
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
		if(lifeTime==60&&!close) {
			engine.playSE("linefall")
			if(selectedOutcome==0) {
				engine.playSE("excellent")
			} else {
				engine.playSE("regret")
			}
		} else if(lifeTime==spinDuration+120&&close) {
			engine.playSE("linefall")
			if(selectedOutcome==0) {
				engine.playSE("excellent")
			} else {
				engine.playSE("regret")
			}
		}
		if(close&&lifeTime<=spinDuration) {
			val j = lifeTime.toDouble()/spinDuration.toDouble()
			// StringBuilder sb = new StringBuilder();
			// sb.append("[");
			for(i in locations.indices) {
				val res = Interpolation.smoothStep(
					endXs[i].toDouble(), startXs[i].toDouble(), 64.0, j)
				// sb.append(res).append(", ");
				locations[i] = res.toInt()
			}
			// sb.append("]");
			// log.debug(lifeTime + ": (LOC) " + Arrays.toString(locations) + ", " + j);
			// log.debug(lifeTime + ": (RAW) " + sb.toString());
			for(i in locations.indices) {
				if(MathHelper.almostEqual((locations[i]%320).toDouble(), 80.0, 24.0)) {
					if(!clickedBefore) {
						clickedBefore = true
						engine.playSE("change")
					}
					break
				}
				if(i==locations.size-1) {
					clickedBefore = false
				}
			}
		}
	}

	init {
		lifeTime = 0

		// locations = endXs;
		locations = endXs.clone()
		clickedBefore = false
	}
}