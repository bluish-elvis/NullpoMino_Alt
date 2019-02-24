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

import mu.nu.nullpo.game.component.Statistics
import mu.nu.nullpo.game.event.EventReceiver.COLOR
import mu.nu.nullpo.gui.slick.img.FontNormal
import mu.nu.nullpo.util.CustomProperties
import mu.nu.nullpo.util.GeneralUtil
import org.apache.log4j.Logger
import org.newdawn.slick.GameContainer
import org.newdawn.slick.Graphics
import org.newdawn.slick.state.StateBasedGame
import java.io.*
import java.nio.file.Paths
import java.util.*
import java.util.zip.GZIPInputStream

/** リプレイ選択画面のステート */
class StateReplaySelect:DummyMenuScrollState() {
	private var strCurrentFolder:String = ""
	private var strPrevFolder:String = ""
	private var fileList:Array<File> = emptyArray()
	/** Mode name */
	private var modenameList:Array<String> = emptyArray()

	/** Rule name */
	private var rulenameList:Array<String> = emptyArray()

	/** Scoreなどの情報 */
	private var statsList:Array<Statistics?> = emptyArray()

	init {
		pageHeight = PAGE_HEIGHT
		emptyError = "NO REPLAY FILE"
	}

	/* Fetch this state's ID */
	override fun getID():Int = ID

	/* State initialization */
	override fun init(container:GameContainer, game:StateBasedGame) {}

	/* Called when entering this state */
	override fun enter(container:GameContainer?, game:StateBasedGame?) {
		strPrevFolder = ""
		getReplayFileList()
	}

	/** リプレイファイル一覧を取得
	 * @return リプレイファイルのFilenameの配列。
	 */
	private fun getReplayFileList(mode:String?):Array<Statistics?> {
		var d = NullpoMinoSlick.propGlobal.getProperty("custom.replay.directory", "replay")
		if(mode!=null) {
			strCurrentFolder = mode
			d += strCurrentFolder
			try {
				strPrevFolder = Paths.get(strCurrentFolder).parent.normalize().toString()
			} catch(e:Exception) {
				strPrevFolder = ""
				strCurrentFolder = strPrevFolder
			}

		} else {
			strPrevFolder = ""
			strCurrentFolder = strPrevFolder
		}

		val dir = File(d)
		val fold = dir.listFiles(FileFilter {it.isDirectory})
		val reps = dir.listFiles {i -> i.name.endsWith(".rep")}

		val sorter = Comparator.comparing<File, String>{it.name}
		var nF = 0
		var nR = 0
		if(fold!=null) {
			java.util.Arrays.sort(fold, sorter)
			nF = fold.size
		}
		if(reps!=null) {
			java.util.Arrays.sort(reps, sorter)
			nR = reps.size
		}

		val nT = nF+nR
		list = Array(nT) {reps!![it].name}
		modenameList = Array(nT) {""}
		rulenameList = Array(nT) {""}
		statsList = arrayOfNulls(nT)

		for(i in 0 until nT) {
			if(i<nR) {
				val prop = CustomProperties()

				try {
					val `in` = GZIPInputStream(FileInputStream(reps[i]))
					prop.load(`in`)
					`in`.close()
				} catch(e:IOException) {
					log.error("Failed to load replay file ("+list[i]+")", e)
				}

				modenameList[i] = prop.getProperty("name.mode", "")
				rulenameList[i] = prop.getProperty("name.rule", "")

				statsList[i] = Statistics().apply {
					readProperty(prop, 0)
				}
			} else {
				list[i] = fold!![i-nR].name
				statsList[i] = null
			}
		}
		return statsList
	}

	private fun getReplayFileList() {
		getReplayFileList(null)
	}

	override fun onRenderSuccess(container:GameContainer, game:StateBasedGame, graphics:Graphics) {
		var title = "SELECT REPLAY FILE"
		title += " ("+(cursor+1)+"/"+list.size+")"
		if(!strCurrentFolder.isEmpty()) title += "\n"+strCurrentFolder.replace(File.separatorChar, 'b')
		FontNormal.printFontGrid(1, 1, title, COLOR.ORANGE)

		statsList[cursor]?.let{
			FontNormal.printFontGrid(1, 24, "MODE:"+modenameList[cursor]+" RULE:"+rulenameList[cursor], COLOR.CYAN)
			FontNormal.printFontGrid(1, 25, "SCORE:"+it.score+" LINE:"+it.lines, COLOR.CYAN)
			FontNormal.printFontGrid(1, 26, "LEVEL:"+(it.level+it.levelDispAdd)+" TIME:"+GeneralUtil.getTime(it.time.toFloat()), COLOR.CYAN)
			FontNormal.printFontGrid(1, 27, "GAME RATE:"+if(it.gamerate==0f) "UNKNOWN" else (100*it.gamerate).toString()+"%", COLOR.CYAN)
		}
	}

	override fun onDecide(container:GameContainer, game:StateBasedGame, delta:Int):Boolean {
		if(list.isNullOrEmpty()) return false
		ResourceHolder.soundManager.play("decide0")
		if(statsList[cursor]==null) {
			getReplayFileList(GeneralUtil.nulltoEmpty(strCurrentFolder)+File.separator+list[cursor])
		} else {
			val prop = CustomProperties()

			try {
				val `in` = GZIPInputStream(FileInputStream(
					NullpoMinoSlick.propGlobal.getProperty("custom.replay.directory", "replay")
						+strCurrentFolder+"/"+list[cursor]))
				prop.load(`in`)
				`in`.close()
			} catch(e:IOException) {
				log.error("Failed to load replay file from "+list[cursor], e)
				return true
			}

			NullpoMinoSlick.stateInGame.startReplayGame(prop)

			game.enterState(StateInGame.ID)
		}
		return false
	}

	override fun onCancel(container:GameContainer, game:StateBasedGame, delta:Int):Boolean {
		if(strCurrentFolder.isEmpty()||strPrevFolder.isEmpty())
			game.enterState(StateTitle.ID)
		else
			getReplayFileList(strPrevFolder)
		return false
	}

	companion object {
		/** This state's ID */
		const val ID = 4

		/** 1画面に表示するMaximumファイルcount */
		const val PAGE_HEIGHT = 20

		/** Log */
		internal val log = Logger.getLogger(StateReplaySelect::class.java)
	}
}
