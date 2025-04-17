/*
 Copyright (c) 2023-2024, NullNoname
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
import mu.nu.nullpo.gui.slick.BaseMenuScrollState.Companion.LINE_D
import mu.nu.nullpo.gui.slick.BaseMenuScrollState.Companion.LINE_WIDTH
import mu.nu.nullpo.gui.slick.BaseMenuScrollState.Companion.SB_BACK_COLOR
import mu.nu.nullpo.gui.slick.BaseMenuScrollState.Companion.SB_BORDER_COLOR
import mu.nu.nullpo.gui.slick.BaseMenuScrollState.Companion.SB_FILL_COLOR
import mu.nu.nullpo.gui.slick.BaseMenuScrollState.Companion.SB_MIN_X
import mu.nu.nullpo.gui.slick.BaseMenuScrollState.Companion.SB_MIN_Y
import mu.nu.nullpo.gui.slick.BaseMenuScrollState.Companion.SB_SHADOW_COLOR
import mu.nu.nullpo.gui.slick.BaseMenuScrollState.Companion.SB_TEXT_COLOR
import mu.nu.nullpo.gui.slick.BaseMenuScrollState.Companion.SB_TEXT_X
import mu.nu.nullpo.gui.slick.BaseMenuScrollState.Companion.SB_WIDTH
import mu.nu.nullpo.gui.slick.img.FontNormal
import mu.nu.nullpo.gui.slick.img.FontTTF
import org.newdawn.slick.Color
import org.newdawn.slick.GameContainer
import org.newdawn.slick.Graphics
import org.newdawn.slick.Input
import org.newdawn.slick.state.StateBasedGame

abstract class BaseMenuConfigState:BaseMenuChooseState() {
	abstract val title:String
	abstract val columns:List<Pair<String, List<Column?>>>
	val allLine get() = columns.flatMap {it.second}
	/** Max cursor value */
	override val numChoice get() = allLine.size

	/** Maximum number of entries to display at a time */
	protected var pageHeight = 24
	private val sbHeight get() = 16f*(pageHeight-1)-LINE_D

	class Column(val show:()->String, val onChange:(change:Int)->Unit, val uiText:String = "", val rainbow:()->Int? = {null})
	/* Draw the screen */
	override fun renderImpl(container:GameContainer, game:StateBasedGame, g:Graphics) {
		// Background
		ResourceHolder.imgMenuBG[1].draw()
		columns.withIndex().firstOrNull {it.value.second.contains(allLine[cursor])}?.let {(id, col) ->
			val (subtitle, array) = col
			val cf = columns.take(id).sumOf {it.second.size}
			FontNormal.printFontGrid(
				1, 1, title+if(columns.size>1) ": $subtitle (${id+1}/${columns.size})" else "", COLOR.ORANGE
			)
			FontNormal.printFontGrid(1, 3+cursor-cf, BaseFont.CURSOR, COLOR.RAINBOW)
			array.forEachIndexed {i, c ->
				c?.let {FontNormal.printFontGrid(2, 3+i, it.show(), cursor==i+cf, rainbow = it.rainbow()?:FontNormal.rainbowCount)}
			}

			//Draw scroll bar
			FontNormal.printFontGrid(SB_TEXT_X, 2, "\u008b", SB_TEXT_COLOR)
			FontNormal.printFontGrid(SB_TEXT_X, 2+pageHeight, "\u008e", SB_TEXT_COLOR)
			//Draw shadow
			g.color = SB_SHADOW_COLOR
			g.fillRect((SB_MIN_X+SB_WIDTH), (SB_MIN_Y+LINE_WIDTH), LINE_WIDTH, sbHeight)
			g.fillRect((SB_MIN_X+LINE_WIDTH), (SB_MIN_Y+sbHeight), SB_WIDTH, LINE_WIDTH)
			//Draw border
			g.color = SB_BORDER_COLOR
			g.fillRect(SB_MIN_X.toFloat(), SB_MIN_Y.toFloat(), SB_WIDTH, sbHeight)
			//Draw inside
			val insideHeight = sbHeight-LINE_D
			val insideWidth = SB_WIDTH-LINE_D
			val fillHeight = maxOf(LINE_WIDTH,insideHeight*1f*columns[id].second.size/allLine.size)
			val fillMinY = minOf(insideHeight*cf/allLine.size,insideHeight-fillHeight)
			val curHeight = maxOf(LINE_WIDTH,insideHeight/allLine.size)
			val curMinY = minOf(insideHeight*cursor/allLine.size,insideHeight-curHeight)
			g.color = SB_BACK_COLOR
			g.fillRect((SB_MIN_X+LINE_WIDTH), (SB_MIN_Y+LINE_WIDTH), insideWidth, insideHeight)
			g.color = SB_FILL_COLOR
			g.fillRect((SB_MIN_X+LINE_WIDTH), (SB_MIN_Y+LINE_WIDTH+fillMinY), insideWidth, fillHeight)
			g.color = SB_BORDER_COLOR
			g.fillRect((SB_MIN_X+LINE_WIDTH), (SB_MIN_Y+LINE_WIDTH+curMinY), insideWidth, curHeight)
			g.color = Color.white

		}

		super.renderImpl(container, game, g)
		if(!allLine[cursor]?.uiText.isNullOrEmpty()) FontTTF.print(16, 432,
			NullpoMinoSlick.getUIText(allLine[cursor]?.uiText?:""))
	}

	override fun onCursor(container:GameContainer, game:StateBasedGame, delta:Int, change:Int) {
		ResourceHolder.soundManager.play("cursor")
		columns.withIndex().firstOrNull {it.value.second.contains(allLine[cursor])}?.let {(id, _) ->
			val cf = columns.take(id).sumOf {it.second.size}
			emitGrid(cursor+minChoiceY-cf)
		}
	}

	override fun onChange(container:GameContainer, game:StateBasedGame, delta:Int, change:Int) {
		ResourceHolder.soundManager.play("change")
		allLine[cursor]?.onChange?.let {it(change)}
	}

	override fun updateMouseInput(input:Input):Boolean {
		MouseInput.update(input)
		if(MouseInput.isMouseClicked) {
			val y = MouseInput.mouseY shr 4
			val newCursor = y-minChoiceY
			ResourceHolder.soundManager.play("cursor")
			if(newCursor in 0..<numChoice) {
				if(newCursor==cursor) return true
				cursor = newCursor
				emitGrid(cursor+minChoiceY)
			}
		}
		return false
	}
}
