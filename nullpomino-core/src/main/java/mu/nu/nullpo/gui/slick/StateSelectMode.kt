/*
 * Copyright (c) 2010-2021, NullNoname
 * Kotlin converted and modified by Venom=Nhelv
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of NullNoname nor the names of its
 *       contributors may be used to endorse or promote products derived from
 *       this software without specific prior written permission.
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

import mu.nu.nullpo.game.component.BGMStatus.BGM
import mu.nu.nullpo.game.event.EventReceiver.COLOR
import mu.nu.nullpo.gui.slick.img.FontNano
import mu.nu.nullpo.gui.slick.img.FontNormal
import mu.nu.nullpo.gui.slick.img.FontTTF
import org.apache.log4j.Logger
import org.newdawn.slick.GameContainer
import org.newdawn.slick.Graphics
import org.newdawn.slick.state.StateBasedGame
import java.util.LinkedList

/** Mode select screen */
class StateSelectMode:DummyMenuScrollState() {

	/** Current folder name */
	private var strCurrentFolder = ""

	/** Constructor */
	init {
		pageHeight = PAGE_HEIGHT
	}

	/* Fetch this state's ID */
	override fun getID():Int = ID

	/* State initialization */
	override fun init(container:GameContainer, game:StateBasedGame) {}

	/** Prepare mode list */
	private fun prepareModeList() {
		strCurrentFolder = StateSelectModeFolder.strCurrentFolder
		// Get mode list
		val listMode:LinkedList<String>?
		if(isTopLevel) {
			listMode = StateSelectModeFolder.listTopLevelModes
			list = Array(listMode.size) {convModeName(listMode[it], true)}
			list += "[more...]"
		} else {
			listMode = StateSelectModeFolder.mapFolder[strCurrentFolder]
			list = if(strCurrentFolder.isNotBlank()&&listMode?.isNotEmpty()==true)
				Array(listMode.size) {convModeName(listMode[it], true)}
			else
				NullpoMinoSlick.modeManager.getModeNames(false)

		}

		// Set cursor postion

		cursor = getIDbyName(when {
			isTopLevel -> NullpoMinoSlick.propGlobal.getProperty("name.mode.toplevel", null)
			strCurrentFolder.isNotEmpty() -> NullpoMinoSlick.propGlobal.getProperty("name.mode.$strCurrentFolder", null)
			else -> NullpoMinoSlick.propGlobal.getProperty("name.mode", null)
		})
		if(cursor<0) cursor = 0
		if(cursor>list.size-1) cursor = list.size-1
	}

	/** Get mode ID (not including netplay modes)
	 * @param name Name of mode
	 * @return ID (-1 if not found)
	 */
	private fun getIDbyName(name:String?):Int =
		if(name.isNullOrEmpty()||list.isEmpty()) -1 else list.indexOfFirst {it==name}

	/** Get game mode description
	 * @param str Mode name
	 * @return Description
	 */
	private fun getModeDesc(str:String):String {
		var str2 = str.replace(' ', '_')
		str2 = str2.replace('(', 'l')
		str2 = str2.replace(')', 'r')

		return NullpoMinoSlick.propModeDesc.getProperty(str2)
			?: NullpoMinoSlick.propDefaultModeDesc.getProperty(str2, str2) ?: str2
	}

	private fun convModeName(str:String, sw:Boolean):String {
		val mm = NullpoMinoSlick.modeManager
		val i = mm.getNum(str)
		return if(i==-1) str else if(sw) mm.getName(i) else mm.getID(i)
	}

	/* Enter */
	override fun enter(container:GameContainer?, game:StateBasedGame?) {
		super.enter(container, game)
		prepareModeList()
		if(ResourceHolder.bgmPlaying!=BGM.Menu(0)) ResourceHolder.bgmStart(BGM.Menu(0))
	}

	/* Render screen */
	public override fun onRenderSuccess(container:GameContainer, game:StateBasedGame, graphics:Graphics) {

		FontNormal.printFontGrid(1, 1, "Select a Game Mode (${cursor+1}/${list.size})", COLOR.ORANGE)
		if(!isTopLevel)
			FontNano.printFont(8, 36,
				if(strCurrentFolder.isNotEmpty()) ">${strCurrentFolder.uppercase()}" else ">[All modes]", COLOR.ORANGE, .5f)

		FontTTF.print(16, 440, getModeDesc(convModeName(list[cursor], false)))
	}

	/* Decide */
	override fun onDecide(container:GameContainer, game:StateBasedGame, delta:Int):Boolean {

		if(isTopLevel&&cursor==list.lastIndex) {
			// More...
			NullpoMinoSlick.propGlobal.setProperty("name.mode.toplevel", list[cursor])
			ResourceHolder.soundManager.play("decide1")
			game.enterState(StateSelectModeFolder.ID)
		} else {
			// Go to rule selector
			if(isTopLevel) NullpoMinoSlick.propGlobal.setProperty("name.mode.toplevel", list[cursor])
			ResourceHolder.soundManager.play("decide2")
			if(strCurrentFolder.isNotEmpty())
				NullpoMinoSlick.propGlobal.setProperty("name.mode.$strCurrentFolder", list[cursor])

			NullpoMinoSlick.propGlobal.setProperty("name.mode", list[cursor])
			//NullpoMinoSlick.saveConfig();
			game.enterState(StateSelectRuleFromList.ID)
		}

		return false
	}

	/* Cancel */
	override fun onCancel(container:GameContainer, game:StateBasedGame, delta:Int):Boolean {
		if(isTopLevel) game.enterState(StateTitle.ID)
		else game.enterState(StateSelectModeFolder.ID)

		return true
	}

	companion object {
		/** Logger */
		internal var log = Logger.getLogger(StateSelectMode::class.java)

		/** This state's ID */
		const val ID = 3

		const val PAGE_HEIGHT = 24

		/** True if top-level folder */
		var isTopLevel = false
	}
}
