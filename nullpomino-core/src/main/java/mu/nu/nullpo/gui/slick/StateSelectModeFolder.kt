package mu.nu.nullpo.gui.slick

import mu.nu.nullpo.game.event.EventReceiver.COLOR
import mu.nu.nullpo.gui.slick.img.FontNormal
import org.apache.log4j.Logger
import org.newdawn.slick.GameContainer
import org.newdawn.slick.Graphics
import org.newdawn.slick.state.StateBasedGame
import java.io.*
import java.util.*

/** Mode folder select */
class StateSelectModeFolder:DummyMenuScrollState() {
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
		list = Array(listFolder.size) {
			if(strCurrentFolder==listFolder[it]) cursor = it
			listFolder[it]
		}
		list += "[ALL MODES]"
	}

	/** Get folder description
	 * @param str Folder name
	 * @return Description
	 */
	private fun getFolderDesc(str:String):String {
		var str2 = str.replace(' ', '_')
		str2 = str2.replace('(', 'l')
		str2 = str2.replace(')', 'r')
		return NullpoMinoSlick.propModeDesc.getProperty("_$str2")
			?: NullpoMinoSlick.propDefaultModeDesc.getProperty("_$str2", "_$str2") ?: str2
	}

	/* Render screen */
	override fun onRenderSuccess(container:GameContainer, game:StateBasedGame, graphics:Graphics) {
		FontNormal.printFontGrid(1, 1, "PICK A MODES FOLDER (${cursor+1}/${list.size})", COLOR.ORANGE)
		FontNormal.printTTF(16, 440, getFolderDesc(list[cursor]))
	}

	/* Decide */
	override fun onDecide(container:GameContainer, game:StateBasedGame, delta:Int):Boolean {
		ResourceHolder.soundManager.play("decide1")
		strCurrentFolder = if(cursor==list.lastIndex) "" else list[cursor]
		NullpoMinoSlick.propGlobal.setProperty("name.folder", strCurrentFolder)
		NullpoMinoSlick.saveConfig()
		StateSelectMode.isTopLevel = false
		game.enterState(StateSelectMode.ID)
		return false
	}

	/* Cancel */
	override fun onCancel(container:GameContainer, game:StateBasedGame, delta:Int):Boolean {
		StateSelectMode.isTopLevel = true
		game.enterState(StateSelectMode.ID)
		return false
	}

	companion object {
		/** Log */
		internal val log = Logger.getLogger(StateSelectModeFolder::class.java)

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

		/** Current folder name */
		var strCurrentFolder:String = ""

		/** Load folder list file */
		private fun loadFolderListFile() {
			listTopLevelModes.clear()

			listFolder.clear()

			mapFolder.clear()

			strCurrentFolder = NullpoMinoSlick.propGlobal.getProperty("name.folder", "")

			try {
				val `in` = BufferedReader(FileReader("config/list/modefolder.lst"))
				var strFolder = ""

				`in`.readLines().forEach {s ->
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

				`in`.close()
			} catch(e:IOException) {
				log.error("Failed to load mode folder list file", e)
			}

		}
	}
}
