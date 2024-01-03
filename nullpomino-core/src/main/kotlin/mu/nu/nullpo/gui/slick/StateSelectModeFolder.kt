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
import mu.nu.nullpo.gui.slick.img.FontNormal
import mu.nu.nullpo.gui.slick.img.FontTTF
import org.apache.logging.log4j.LogManager
import org.newdawn.slick.GameContainer
import org.newdawn.slick.Graphics
import org.newdawn.slick.state.GameState
import org.newdawn.slick.state.StateBasedGame
import org.newdawn.slick.state.transition.EmptyTransition
import org.newdawn.slick.state.transition.HorizontalSplitTransition
import java.io.FileReader
import java.io.IOException
import java.util.LinkedList

/** Mode folder select */
internal class StateSelectModeFolder:BaseMenuScrollState() {
	/** Constructor */
	init {
		pageHeight = PAGE_HEIGHT
	}

	/* Fetch this state's ID */
	override fun getID():Int = ID

	/* State initialization */
	override fun init(container:GameContainer, game:StateBasedGame) {
		loadFolderListFile()
		prepareFolderList()
	}

	/** Prepare folder list */
	private fun prepareFolderList() {
		list = List(listFolder.size) {
			if(strCurrentFolder==listFolder[it]) {
				cursor = it
				emitGrid(cursor+minChoiceY)
			}
			listFolder[it]
		}+"[ALL MODES]"
	}

	/** Get folder description
	 * @param str Folder name
	 * @return Description
	 */
	private fun getFolderDesc(str:String):String {
		val str2 = str.replace(' ', '_').replace('(', 'l')
			.replace(')', 'r')
		return NullpoMinoSlick.propModeDesc.getProperty("_$str2")
			?: NullpoMinoSlick.propDefaultModeDesc.getProperty("_$str2", "_$str2") ?: str2
	}

	/* Render screen */
	override fun onRenderSuccess(container:GameContainer, game:StateBasedGame, graphics:Graphics) {
		FontNormal.printFontGrid(1, 1, "Pick a Modes Folder (${cursor+1}/${list.size})", COLOR.ORANGE)
		FontTTF.print(16, 440, getFolderDesc(list[cursor]))
	}

	/* Decide */
	override fun onDecide(container:GameContainer, game:StateBasedGame, delta:Int):Boolean {
		ResourceHolder.soundManager.play("decide1")
		strCurrentFolder = if(cursor==list.lastIndex) "" else list[cursor]
		NullpoMinoSlick.propGlobal.lastModeFolder = strCurrentFolder
//		NullpoMinoSlick.saveConfig()
		game.enterState(StateSelectMode.ID, TransDecideFolder(strCurrentFolder), HorizontalSplitTransition())
		return false
	}

	/* Cancel */
	override fun onCancel(container:GameContainer, game:StateBasedGame, delta:Int):Boolean {
		game.enterState(StateSelectMode.ID, TransDecideFolder(), HorizontalSplitTransition())
		return true
	}

	internal class TransDecideFolder(private val folderName:String? = null):EmptyTransition() {
		override fun init(firstState:GameState?, secondState:GameState?) {
			super.init(firstState, secondState)
			if(secondState is StateSelectMode) {
				secondState.strCurrentFolder = folderName ?: ""
				secondState.isTopLevel = folderName==null
			}
		}
	}

	companion object {
		/** Log */
		internal val log = LogManager.getLogger()

		/** This state's ID */
		const val ID = 19

		/** Number of folders in one page */
		const val PAGE_HEIGHT = 24

		/** Top-level mode list */
		var listTopLevelModes:LinkedList<String> = LinkedList()

		/** Folder names list */
		var listFolder:LinkedList<String> = LinkedList()

		/** HashMap of mode folder (FolderName->ModeNames) */
		var mapFolder:HashMap<String, LinkedList<String>> = HashMap()

		/** Selected folder name */
		var strCurrentFolder = ""

		/** Load folder list file */
		private fun loadFolderListFile() {
			listTopLevelModes.clear()
			listFolder.clear()
			mapFolder.clear()
			strCurrentFolder = NullpoMinoSlick.propGlobal.lastModeFolder

			try {
				var strFolder = ""
				FileReader("config/list/modefolder.lst").buffered().use {b ->
					b.forEachLine {s ->
						val str = s.trim {it<=' '} // Trim the space

						if(str.startsWith("#")) {
							// Commment-line. Ignore it.
						} else if(str.startsWith(":")) {
							// New folder
							strFolder = str.substring(1)
							if(!listFolder.contains(strFolder)) {
								listFolder.add(strFolder)
								val listMode = LinkedList<String>()
								mapFolder[strFolder] = listMode
							}
						} else if(str.isNotEmpty())
						// Mode name
							if(strFolder.isEmpty()) {
								log.debug("(top-level).$str")
								listTopLevelModes.add(str)
							} else {
								val listMode = mapFolder[strFolder]
								if(listMode!=null&&!listMode.contains(str)) {
									log.debug("$strFolder.$str")
									listMode.add(str)
									mapFolder[strFolder] = listMode
								}
							}
					}
				}
			} catch(e:IOException) {
				log.error("Failed to load mode folder list file", e)
			}
		}
	}
}
