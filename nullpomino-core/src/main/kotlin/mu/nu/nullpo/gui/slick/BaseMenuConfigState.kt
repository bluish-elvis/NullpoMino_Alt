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
import mu.nu.nullpo.gui.slick.img.FontNormal
import mu.nu.nullpo.gui.slick.img.FontTTF
import org.newdawn.slick.GameContainer
import org.newdawn.slick.Graphics
import org.newdawn.slick.state.StateBasedGame

abstract class BaseMenuConfigState:BaseMenuChooseState() {
	abstract val title:String
	abstract val columns:List<Pair<String, List<Column?>>>
	val allLine get() = columns.flatMap {it.second}
	/** Max cursor value */
	override val numChoice:Int get() = allLine.size

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
				c?.let {FontNormal.printFontGrid(2, 3+i, it.show(), cursor==i+cf, rainbow = it.rainbow() ?: FontNormal.rainbowCount)}
			}
		}
		super.renderImpl(container, game, g)
		if(!allLine[cursor]?.uiText.isNullOrEmpty()) FontTTF.print(16, 432, NullpoMinoSlick.getUIText(allLine[cursor]?.uiText?:""))
	}
override  fun onCursor(container:GameContainer, game:StateBasedGame, delta:Int, change:Int) {
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

}
