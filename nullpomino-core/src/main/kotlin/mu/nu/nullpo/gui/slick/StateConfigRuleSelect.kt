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

import mu.nu.nullpo.game.component.RuleOptions
import mu.nu.nullpo.game.event.EventReceiver.COLOR
import mu.nu.nullpo.gui.common.ConfigGlobal.RuleConf
import mu.nu.nullpo.gui.slick.img.FontNormal
import mu.nu.nullpo.util.GeneralUtil.Json
import org.newdawn.slick.GameContainer
import org.newdawn.slick.Graphics
import org.newdawn.slick.state.StateBasedGame
import java.io.File
import java.io.FileInputStream
import java.util.*
import java.util.zip.GZIPInputStream
import java.util.zip.ZipException

/** Rule selector state */
internal class StateConfigRuleSelect:BaseMenuScrollState() {
	/** Player ID */
	var player = 0
	/** Game style ID */
	var style = 0

	/** Rule file list (for loading) */
	private var strFileList:Array<String> = emptyArray()
	/** Rule name list */
	private var strRuleNameList:List<String> = emptyList()
	/** Rule file list (for list display) */
	private var strRuleFileList:List<String> = emptyList()
	/** Current Rule File name */
	private var strCurrentFileName = ""
	/** Current Rule name */
	private var strCurrentRuleName = ""
	/** Rule entries */
	private var ruleEntries:LinkedList<RuleConf> = LinkedList()

	init {
		minChoiceY = 3
	}

	/** Get rule file list
	 * @return Rule file list. null if directory doesn't exist.
	 */
	private val ruleFileList:Array<String>?
		get() {
			val dir = File("config/rule")

			val list = dir.list {_, name -> name.endsWith(".rul")||name.endsWith(".rul.gz")}

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
			val entry = RuleConf()

			val file = File("config/rule/$element")
			entry.file = element
			entry.path = file.path
			try {
				val rf = try {
					GZIPInputStream(FileInputStream(file))
				}catch(_:ZipException){
					FileInputStream(file)
				}
				val ret = Json.decodeFromString<RuleOptions>(rf.bufferedReader().use {it.readText()})
				entry.name = ret.strRuleName
				entry.style = ret.style
			} catch(e:Exception) {
				entry.name = ""
				entry.style = -1
			}

			if(entry.style==currentStyle) ruleEntries.add(entry)
		}
	}

	/** Get rule name list as String[]
	 * @return Rule name list
	 */
	private fun extractRuleNameListFromRuleEntries():List<String> =
		List(ruleEntries.size) {ruleEntries[it].name}

	/** Get rule file name list as String[]
	 * @return Rule name list
	 */
	private fun extractFileNameListFromRuleEntries():List<String> =
		List(ruleEntries.size) {ruleEntries[it].file}

	/* Called when entering this state */
	override fun enter(container:GameContainer?, game:StateBasedGame?) {
		super.enter(container, game)
		strFileList = ruleFileList ?: emptyArray()
		createRuleEntries(strFileList, style)
		strRuleNameList = extractRuleNameListFromRuleEntries()
		strRuleFileList = extractFileNameListFromRuleEntries()
		list = strRuleNameList
		NullpoMinoSlick.propGlobal.rule[player][style].let {
			strCurrentFileName = it.file
			strCurrentRuleName = it.name
		}

		cursor = 0
		ruleEntries.indexOfFirst {it.file==strCurrentFileName}.let {
			if(it>=0) {
				cursor = it
				emitGrid(it+minChoiceY)
			}
		}
	}

	/* State initialization */
	override fun init(container:GameContainer, game:StateBasedGame) {}

	/* Draw the screen */
	override fun onRenderSuccess(container:GameContainer, game:StateBasedGame, graphics:Graphics) {
		val title = "SELECT ${player+1}P RULE (${cursor+1}/${list.size})"
		FontNormal.printFontGrid(1, 1, title, COLOR.ORANGE)

		FontNormal.printFontGrid(1, 25, "CURRENT:${strCurrentRuleName}", COLOR.BLUE)
		FontNormal.printFontGrid(9, 26, strCurrentFileName, COLOR.BLUE)

		FontNormal.printFontGrid(1, 28, "A:OK B:CANCEL D:TOGGLE-VIEW", COLOR.GREEN)
	}

	/* Decide */
	override fun onDecide(container:GameContainer, game:StateBasedGame, delta:Int):Boolean {
		ResourceHolder.soundManager.play("decide0")

		val entry = ruleEntries[cursor]
		NullpoMinoSlick.propGlobal.rule[player][style] = entry

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
		list = if(list==strRuleNameList) strRuleFileList else strRuleNameList
		return false
	}

	companion object {
		/** This state's ID */
		const val ID = 7

		/** Number of rules shown at a time */
		const val PAGE_HEIGHT = 21
	}
}
