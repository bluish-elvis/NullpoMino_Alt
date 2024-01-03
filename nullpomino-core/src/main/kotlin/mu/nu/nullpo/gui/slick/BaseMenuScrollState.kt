/*
 Copyright (c) 2010-2024, NullNoname
 All rights reserved.

 Converted to Kotlin and modified by Venom_Nhelv as bluish-elvis
 THIS IS NOT MADE IN ASSOCIATION WITH THE GAME CREATOR.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions are met:
     * Redistributions of source code must retain the above copyright
       notice, this list of conditions and the following disclaimer.
     * Redistributions in binary form must reproduce the above copyright
       notice, this list of conditions and the following disclaimer in the
       documentation and/or other materials provided with the distribution.
     * Neither the name of NullNoname nor the names of its
       contributors may be used to endorse or promote products derived from
       this software without specific prior written permission.

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

package mu.nu.nullpo.gui.slick

import mu.nu.nullpo.game.event.EventReceiver.COLOR
import mu.nu.nullpo.gui.common.BaseFont
import mu.nu.nullpo.gui.slick.img.FontNormal
import org.newdawn.slick.Color
import org.newdawn.slick.GameContainer
import org.newdawn.slick.Graphics
import org.newdawn.slick.Input
import org.newdawn.slick.state.StateBasedGame

/** Dummy class for menus with a scroll bar */
abstract class BaseMenuScrollState:BaseMenuChooseState() {
	/** ID number of file at top of currently displayed section */
	private var minEntry = 0

	/** Maximum number of entries to display at a time */
	protected var pageHeight = 0

	/** List of entries */
	protected open var list:List<String> = emptyList()
	override val numChoice:Int get() = list.size

	protected var emptyError = ""

	/** Y-coordinates of dark sections of scroll bar */
	private var pUpMinY = 0
	private var pUpMaxY = 0
	private var pDownMinY = 0
	private var pDownMaxY = 0
	private val sbHeight get() = 16*(pageHeight-1)-(LINE_WIDTH shl 1)

	override fun updateImpl(container:GameContainer, game:StateBasedGame, delta:Int) {
		super.updateImpl(container, game, delta)
		if(cursor>=list.size) cursor = 0
		if(cursor<minEntry) minEntry = cursor
		var maxEntry = minEntry+pageHeight-1
		if(cursor>=maxEntry) {
			maxEntry = cursor
			minEntry = maxEntry-pageHeight+1
		}
	}
	/* Draw the screen */
	override fun renderImpl(container:GameContainer, game:StateBasedGame, g:Graphics) {
		// Background
		g.drawImage(ResourceHolder.imgMenuBG[0], 0f, 0f)

		// Menu
		when {
			list.isEmpty() -> FontNormal.printFontGrid(2, 10, emptyError, COLOR.RED)
			else -> {

				drawMenuList(g)
				onRenderSuccess(container, game, g)
			}
		}
		super.renderImpl(container, game, g)
	}

	protected open fun onRenderSuccess(container:GameContainer, game:StateBasedGame, graphics:Graphics) {}

	public override fun updateMouseInput(input:Input):Boolean {
		// Mouse
		MouseInput.update(input)
		val clicked = MouseInput.isMouseClicked
		val x = MouseInput.mouseX shr 4
		val y = MouseInput.mouseY shr 4
		if(x>=SB_TEXT_X-1&&(clicked||MouseInput.isMenuRepeatLeft)) {
			var maxEntry = minEntry+pageHeight-1
			when {
				y<=2&&minEntry>0 -> {
					ResourceHolder.soundManager.play("cursor")
					//Scroll up
					minEntry--
					maxEntry--
				}
				y>=2+pageHeight&&maxEntry<list.size -> {
					ResourceHolder.soundManager.play("cursor")
					//Down arrow
					minEntry++
				}
				numChoice>pageHeight ->
					maxOf(0, (MouseInput.mouseY-32)*(numChoice+1-pageHeight)/sbHeight).let {
						if(it!=minEntry) {
							ResourceHolder.soundManager.play("cursor")
							minEntry = it
						}
					}
			}
			if(cursor>=maxEntry) cursor = maxEntry-1
			if(cursor<minEntry) cursor = minEntry
		} else if(clicked&&y in 3..<2+pageHeight) {
			val newCursor = y-3+minEntry
			when {
				newCursor==cursor -> return true
				newCursor>=list.size -> return false
				else -> {
					ResourceHolder.soundManager.play("cursor")
					cursor = newCursor
					emitGrid(newCursor-minEntry+minChoiceY)
				}
			}
		}
		return false
	}

	private fun drawMenuList(graphics:Graphics) {
		val maxEntry = minOf(minEntry+pageHeight-1, list.size)

		for((y, i) in (minEntry..<maxEntry).withIndex()) {
			FontNormal.printFontGrid(2, 3+y, list[i], cursor==i)
			if(cursor==i) FontNormal.printFontGrid(1, 3+y, BaseFont.CURSOR, COLOR.RAINBOW)
		}

		//Draw scroll bar
		FontNormal.printFontGrid(SB_TEXT_X, 2, "\u008b", SB_TEXT_COLOR)
		FontNormal.printFontGrid(SB_TEXT_X, 2+pageHeight, "\u008e", SB_TEXT_COLOR)
		//Draw shadow
		graphics.color = SB_SHADOW_COLOR
		graphics.fillRect((SB_MIN_X+SB_WIDTH).toFloat(), (SB_MIN_Y+LINE_WIDTH).toFloat(), LINE_WIDTH.toFloat(), sbHeight.toFloat())
		graphics.fillRect((SB_MIN_X+LINE_WIDTH).toFloat(), (SB_MIN_Y+sbHeight).toFloat(), SB_WIDTH.toFloat(), LINE_WIDTH.toFloat())
		//Draw border
		graphics.color = SB_BORDER_COLOR
		graphics.fillRect(SB_MIN_X.toFloat(), SB_MIN_Y.toFloat(), SB_WIDTH.toFloat(), sbHeight.toFloat())
		//Draw inside
		val insideHeight = sbHeight-(LINE_WIDTH shl 1)
		val insideWidth = SB_WIDTH-(LINE_WIDTH shl 1)
		var fillMinY = insideHeight*minEntry/list.size
		var fillHeight = ((maxEntry-minEntry)*insideHeight+list.size)/list.size
		if(fillHeight<LINE_WIDTH) {
			fillHeight = LINE_WIDTH
			fillMinY = (insideHeight-fillHeight)*minEntry/(list.size-pageHeight)
		}
		graphics.color = SB_BACK_COLOR
		graphics.fillRect(
			(SB_MIN_X+LINE_WIDTH).toFloat(),
			(SB_MIN_Y+LINE_WIDTH).toFloat(),
			insideWidth.toFloat(),
			insideHeight.toFloat()
		)
		graphics.color = SB_FILL_COLOR
		graphics.fillRect(
			(SB_MIN_X+LINE_WIDTH).toFloat(),
			(SB_MIN_Y+LINE_WIDTH+fillMinY).toFloat(),
			insideWidth.toFloat(),
			fillHeight.toFloat()
		)
		graphics.color = Color.white

		//Update coordinates
		pUpMinY = SB_MIN_Y+LINE_WIDTH
		pUpMaxY = pUpMinY+fillMinY
		pDownMinY = pUpMaxY+fillHeight
		pDownMaxY = SB_MIN_Y+LINE_WIDTH+insideHeight
	}

	override fun onCursor(container:GameContainer, game:StateBasedGame, delta:Int, change:Int) {
		super.onCursor(container, game, delta, change)
		emitGrid(cursor-minEntry+minChoiceY)
	}

	override fun onChange(container:GameContainer, game:StateBasedGame, delta:Int, change:Int) {
		ResourceHolder.soundManager.play("cursor")
		if(change==1) pageDown()
		else if(change==-1) pageUp()
		emitGrid(cursor-minEntry+minChoiceY)
	}

	private fun pageDown() {
		val max = numChoice-pageHeight
		if(minEntry>=max) cursor = numChoice
		else {
			cursor += pageHeight
			minEntry += pageHeight
			if(minEntry>max) {
				cursor -= minEntry-max
				minEntry = max
			}
		}
	}

	private fun pageUp() {
		if(minEntry==0) cursor = 0
		else {
			cursor -= pageHeight
			minEntry -= pageHeight
			if(minEntry<0) {
				cursor -= minEntry
				minEntry = 0
			}
		}
	}

	companion object {
		/** Scroll bar attributes */
		protected const val SB_TEXT_X = 38
		protected val SB_TEXT_COLOR = COLOR.BLUE
		protected const val SB_MIN_X = SB_TEXT_X shl 4
		protected const val SB_MIN_Y = 49
		protected const val LINE_WIDTH = 2
		protected const val SB_WIDTH = 14

		/** Scroll bar colors */
		protected val SB_SHADOW_COLOR = Color(12, 78, 156)
		protected val SB_BORDER_COLOR = Color(52, 150, 252)
		protected val SB_FILL_COLOR:Color = Color.white
		protected val SB_BACK_COLOR:Color = Color.black
	}
}
