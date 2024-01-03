/*
 Copyright (c) 2021-2024, NullNoname
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

package mu.nu.nullpo.game.subsystem.mode.menu

import mu.nu.nullpo.game.event.EventReceiver
import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.util.CustomProperties

class MenuList(val propName:String = "", vararg items:AbstractMenuItem<*>) {
	val items = items.toList()
	/** [(item.index: item.columnNum to item.showPosYBot)]*/
	val menus = items.foldIndexed(emptyList<Pair<Int, Int>>()) {id, list, item ->
		list+List(item.colMax) {id to it}
	}
	/** map of [items] drawed Y-coordinate grids*/
	val loc = items.fold(emptyList<Int>()) {buf, it -> buf+((buf.lastOrNull() ?: 0)+it.showHeight)}
	private fun locPage(page:Int, height:Int) = loc.indexOfFirst {it>=page*height}.let {if(it<0) loc.size else it}
	val size get() = items.size
	operator fun get(index:Int) = items[index]

	var menuCursor = 0
	var menuSubPos = 0
	var menuY = 0

	fun change(cur:Int, dir:Int, fast:Int) {
		val it = menus[cur]
		items[it.first].change(dir, fast, it.second)
	}

	fun drawMenu(engine:GameEngine, playerID:Int, receiver:EventReceiver, y:Int = menuY, cur:Int = menuCursor) =
		drawMenu(engine, playerID, receiver, y, page = loc.getOrElse(cur) {0}/engine.field.height)

	fun drawMenu(engine:GameEngine, playerID:Int, receiver:EventReceiver, y:Int = menuY, page:Int, offset:Int = 0) {
		var menuY = y
		val range = locPage(page, engine.field.height)+offset..<locPage(page+1, engine.field.height)+offset
		receiver.drawMenuNano(engine, 3f, -.5f, "$menuCursor / ${menus.size-1}, ${range.first}", scale = .5f)
		items.slice(range).forEachIndexed {i, it ->
			for(z in 0..<it.colMax) receiver.drawMenuNano(engine, -.4f, .5f+menuY+z*.5f, "${range.first+i+z}", scale = .5f)
			val i1 = menuCursor-range.first
			it.draw(
				engine, playerID, receiver, menuY, if(engine.owner.replayMode||menus[i1].first!=i) -1 else menus[i1].second
			)
			menuY += it.showHeight
			menuSubPos += if(it is SpeedPresets) (if(it.showG) 2 else 0+if(it.showD) 5 else 0) else 1
		}
	}

	fun load(prop:CustomProperties, engine:GameEngine, ruleName:String = "", playerID:Int = -1) {
		items.forEach {
			it.load(prop, it.propName(propName, ruleName, playerID))
			if(it is PresetItem)
				it.presetLoad(engine, prop, ruleName, playerID)
		}
	}

	fun save(prop:CustomProperties, engine:GameEngine, ruleName:String = "", playerID:Int = -1) {
		items.forEach {
			it.save(prop, it.propName(propName, ruleName, playerID))
			if(it is PresetItem)
				it.presetSave(engine, prop, ruleName, playerID)
		}
	}
}
