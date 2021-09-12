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

import mu.nu.nullpo.game.event.EventReceiver.COLOR
import mu.nu.nullpo.gui.slick.img.FontNano
import mu.nu.nullpo.gui.slick.img.FontNormal
import mu.nu.nullpo.util.CustomProperties
import org.apache.log4j.Logger
import org.newdawn.slick.GameContainer
import org.newdawn.slick.Graphics
import org.newdawn.slick.state.StateBasedGame
import java.io.*
import java.util.LinkedList
import java.util.Locale
import java.util.zip.GZIPInputStream

/** Rule select (after mode selection) */
class StateSelectRuleFromList:DummyMenuScrollState() {

	/** HashMap of rules (ModeName->RuleEntry) */
	private var mapRuleEntries:HashMap<String, RuleEntry> = HashMap()

	/** Current mode */
	private var strCurrentMode = ""

	/** Constructor */
	init {
		pageHeight = PAGE_HEIGHT
	}

	/* Fetch this state's ID */
	override fun getID():Int = ID

	/* State initialization */
	override fun init(container:GameContainer, game:StateBasedGame) {
		loadRecommendedRuleList()
	}

	/** Load list file */
	private fun loadRecommendedRuleList() {
		mapRuleEntries = HashMap()

		try {
			val reader = BufferedReader(FileReader("config/list/recommended_rules.lst"))
			var strMode = ""

			reader.readLines().forEach {str ->
				val r = str.trim {it<=' '} // Trim the space

				if(r.startsWith("#")) {
					// Commment-line. Ignore it.
				} else if(r.startsWith(":")) // Mode change
					strMode = r.substring(1)
				else {
					// File Path
					val file = File(r)
					if(file.exists()&&file.isFile)
						try {
							val ruleIn = GZIPInputStream(FileInputStream(file))
							val propRule = CustomProperties()
							propRule.load(ruleIn)
							ruleIn.close()

							val strRuleName = propRule.getProperty("0.ruleOpt.strRuleName", "")
							if(strRuleName.isNotEmpty()) {
								var entry:RuleEntry? = mapRuleEntries[strMode]
								if(entry==null) {
									entry = RuleEntry()
									mapRuleEntries[strMode] = entry
								}

							}
						} catch(e2:IOException) {
							log.error("File $r doesn't exist", e2)
						}

				}
			}
			reader.close()
		} catch(e:IOException) {
			log.error("Failed to load recommended rules list", e)
		}

	}

	/** Prepare rule list */
	private fun prepareRuleList() {
		strCurrentMode = NullpoMinoSlick.propGlobal.getProperty("name.mode", "")
		if(strCurrentMode.isEmpty()) {
			val entry = mapRuleEntries[""]
			val modeRule = mapRuleEntries[strCurrentMode]
			val ne:Int = entry?.listName?.size ?: 0
			val nm:Int = modeRule?.listName?.size ?: 0


			list = Array(1+ne+nm) {
				when {
					(it in 1..ne) -> entry!!.listName[it-1]
					(it in ne+1..ne+nm) -> modeRule!!.listName[it-ne-1]
					else -> "(Current Rule)"
				}
			}

		} else {
			list = arrayOf("(Current Rule)")
		}

		var defaultCursor = 0
		val strLastRule = NullpoMinoSlick.propGlobal.getProperty("lastrule.${strCurrentMode.lowercase(Locale.getDefault())}")
		if(strLastRule!=null&&strLastRule.isNotEmpty())
			for(i in list.indices)
				if(list[i]==strLastRule) defaultCursor = i
		cursor = defaultCursor
	}

	/* When the player enters this state */
	override fun enter(container:GameContainer?, game:StateBasedGame?) {
		super.enter(container, game)
		prepareRuleList()
	}

	/* Render screen */
	override fun onRenderSuccess(container:GameContainer, game:StateBasedGame, graphics:Graphics) {
		FontNormal.printFontGrid(1, 1, "Choose your Style (${cursor+1}/${list.size})", COLOR.ORANGE)
		FontNano.printFont(8, 36, "FOR ${strCurrentMode.uppercase()}", COLOR.ORANGE, .5f)
	}

	/* Decide */
	override fun onDecide(container:GameContainer, game:StateBasedGame, delta:Int):Boolean {
		ResourceHolder.soundManager.play("decide0")
		NullpoMinoSlick.propGlobal.setProperty("lastrule.${strCurrentMode.lowercase(Locale.getDefault())}",
			if(cursor>=1) list[cursor] else "")
		NullpoMinoSlick.saveConfig()

		var strRulePath:String? = null
		if(cursor>=1) strRulePath = mapRuleEntries[strCurrentMode]?.listPath?.get(cursor-1)

		NullpoMinoSlick.stateInGame.startNewGame(strRulePath)
		game.enterState(StateInGame.ID)
		return false
	}

	/* Cancel */
	override fun onCancel(container:GameContainer, game:StateBasedGame, delta:Int):Boolean {
		game.enterState(StateSelectMode.ID)
		return true
	}

	/** RuleEntry */
	private class RuleEntry {
		val listPath = LinkedList<String>()
		val listName = LinkedList<String>()
	}

	companion object {
		/** Log */
		internal val log = Logger.getLogger(StateSelectRuleFromList::class.java)

		/** This state's ID */
		const val ID = 18

		/** Number of rules in one page */
		const val PAGE_HEIGHT = 24
	}
}
