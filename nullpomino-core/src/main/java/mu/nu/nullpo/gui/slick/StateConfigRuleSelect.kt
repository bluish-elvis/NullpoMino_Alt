/* Copyright (c) 2010, NullNoname
 * All rights reserved.
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * Neither the name of NullNoname nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
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
 * POSSIBILITY OF SUCH DAMAGE. */
package mu.nu.nullpo.gui.slick

import mu.nu.nullpo.game.event.EventReceiver.COLOR
import mu.nu.nullpo.gui.slick.img.FontNormal
import mu.nu.nullpo.util.CustomProperties
import org.newdawn.slick.GameContainer
import org.newdawn.slick.Graphics
import org.newdawn.slick.state.StateBasedGame
import java.io.File
import java.io.FileInputStream
import java.util.*

/** Rule selector state */
class StateConfigRuleSelect:DummyMenuScrollState() {

	/** Player ID */
	var player = 0

	/** Game style ID */
	var style = 0

	/** Rule file list (for loading) */
	private var strFileList:Array<String> = emptyArray()

	/** Rule name list */
	private var strRuleNameList:Array<String> = emptyArray()

	/** Rule file list (for list display) */
	private var strRuleFileList:Array<String> = emptyArray()

	/** Current Rule File name */
	private var strCurrentFileName:String = ""

	/** Current Rule name */
	private var strCurrentRuleName:String = ""

	/** Rule entries */
	private var ruleEntries:LinkedList<RuleEntry> = LinkedList()

	/** Get rule file list
	 * @return Rule file list. null if directory doesn't exist.
	 */
	private// Sort if not windows
	val ruleFileList:Array<String>?
		get() {
			val dir = File("config/rule")

			val list = dir.list {_, name -> name.endsWith(".rul")}

			if(!System.getProperty("os.name").startsWith("Windows"))
				list?.sort()

			return list
		}

	/** Constructor */
	init {
		pageHeight = PAGE_HEIGHT
		emptyError = "NO RULE FILE"
	}

	/* Fetch this state's ID */
	override fun getID():Int = ID

	/** Create rule entries
	 * @param filelist Rule file list
	 * @param currentStyle Current style
	 */
	private fun createRuleEntries(filelist:Array<String>, currentStyle:Int) {
		ruleEntries = LinkedList()

		for(element in filelist) {
			val entry = RuleEntry()

			val file = File("config/rule/$element")
			entry.filename = element
			entry.filepath = file.path

			val prop = CustomProperties()
			try {
				val `in` = FileInputStream("config/rule/$element")
				prop.load(`in`)
				`in`.close()
				entry.rulename = prop.getProperty("0.ruleopt.strRuleName", "")
				entry.style = prop.getProperty("0.ruleopt.style", 0)
			} catch(e:Exception) {
				entry.rulename = ""
				entry.style = -1
			}

			if(entry.style==currentStyle) ruleEntries.add(entry)
		}
	}

	/** Get rule name list as String[]
	 * @return Rule name list
	 */
	private fun extractRuleNameListFromRuleEntries():Array<String> =
		Array(ruleEntries.size){ruleEntries[it].rulename}

	/** Get rule file name list as String[]
	 * @return Rule name list
	 */
	private fun extractFileNameListFromRuleEntries():Array<String> =
		Array(ruleEntries.size) {ruleEntries[it].filename}

	/* Called when entering this state */
	override fun enter(container:GameContainer?, game:StateBasedGame?) {
		super.enter(container, game)
		strFileList = ruleFileList?: emptyArray()
		createRuleEntries(strFileList, style)
		strRuleNameList = extractRuleNameListFromRuleEntries()
		strRuleFileList = extractFileNameListFromRuleEntries()
		list = strRuleNameList

		if(style==0) {
			strCurrentFileName = NullpoMinoSlick.propGlobal.getProperty("$player.rulefile", "")
			strCurrentRuleName = NullpoMinoSlick.propGlobal.getProperty("$player.rulename", "")
		} else {
			strCurrentFileName = NullpoMinoSlick.propGlobal.getProperty("$player.rulefile.$style", "")
			strCurrentRuleName = NullpoMinoSlick.propGlobal.getProperty("$player.rulename.$style", "")
		}

		cursor = 0
		for(i in ruleEntries.indices)
			if(ruleEntries[i].filename==strCurrentFileName) cursor = i
	}

	/* State initialization */
	override fun init(container:GameContainer, game:StateBasedGame) {}

	/* Draw the screen */
	override fun onRenderSuccess(container:GameContainer, game:StateBasedGame, graphics:Graphics) {
		val title = "SELECT ${player+1}P RULE (${cursor+1}/${list.size})"
		FontNormal.printFontGrid(1, 1, title, COLOR.ORANGE)

		FontNormal.printFontGrid(1, 25, "CURRENT:${strCurrentRuleName.uppercase()}", COLOR.BLUE)
		FontNormal.printFontGrid(9, 26, strCurrentFileName.uppercase(), COLOR.BLUE)

		FontNormal.printFontGrid(1, 28, "A:OK B:CANCEL D:TOGGLE-VIEW", COLOR.GREEN)
	}

	/* Decide */
	override fun onDecide(container:GameContainer, game:StateBasedGame, delta:Int):Boolean {
		ResourceHolder.soundManager.play("decide0")

		val entry = ruleEntries[cursor]
		if(style==0) {
			NullpoMinoSlick.propGlobal.setProperty("$player.rule", entry.filepath)
			NullpoMinoSlick.propGlobal.setProperty("$player.rulefile", entry.filename)
			NullpoMinoSlick.propGlobal.setProperty("$player.rulename", entry.rulename)
		} else {
			NullpoMinoSlick.propGlobal.setProperty("$player.rule.$style", entry.filepath)
			NullpoMinoSlick.propGlobal.setProperty("$player.rulefile.$style", entry.filename)
			NullpoMinoSlick.propGlobal.setProperty("$player.rulename.$style", entry.rulename)
		}

		NullpoMinoSlick.saveConfig()

		game.enterState(StateConfigRuleStyleSelect.ID)
		return true
	}

	/* Cancel */
	override fun onCancel(container:GameContainer, game:StateBasedGame, delta:Int):Boolean {
		game.enterState(StateConfigRuleStyleSelect.ID)
		return true
	}

	/* D button */
	override fun onPushButtonD(container:GameContainer, game:StateBasedGame, delta:Int):Boolean {
		ResourceHolder.soundManager.play("change")
		list = if(list.contentEquals(strRuleNameList))
			strRuleFileList
		else
			strRuleNameList
		return false
	}

	/** Rule entry */
	private inner class RuleEntry {
		/** File name */
		var filename:String = ""
		/** File path */
		var filepath:String = ""
		/** Rule name */
		var rulename:String = ""
		/** Game style */
		var style:Int = 0
	}

	companion object {
		/** This state's ID */
		const val ID = 7

		/** Number of rules shown at a time */
		const val PAGE_HEIGHT = 21
	}
}
