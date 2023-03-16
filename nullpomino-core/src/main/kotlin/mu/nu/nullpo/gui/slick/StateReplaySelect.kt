/*
 * Copyright (c) 2010-2022, NullNoname
 * Kotlin converted and modified by Venom=Nhelv.
 * THIS WAS NOT MADE IN ASSOCIATION WITH THE GAME CREATOR.
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

import mu.nu.nullpo.game.component.Statistics
import mu.nu.nullpo.game.event.EventReceiver.COLOR
import mu.nu.nullpo.gui.slick.NullpoMinoSlick.Companion.modeManager
import mu.nu.nullpo.gui.slick.img.FontNano
import mu.nu.nullpo.gui.slick.img.FontNormal
import mu.nu.nullpo.util.CustomProperties
import mu.nu.nullpo.util.GeneralUtil.toTimeStr
import org.apache.logging.log4j.LogManager
import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.library.impl.javaFile
import org.newdawn.slick.GameContainer
import org.newdawn.slick.Graphics
import org.newdawn.slick.state.StateBasedGame
import java.io.FileInputStream
import java.io.IOException
import java.util.zip.GZIPInputStream

/** リプレイ選択画面のステート */
class StateReplaySelect:BaseMenuScrollState() {
	private var directoryPlace = mutableListOf<String>()
	private var cursorHistory = mutableListOf<Int>()
	private fun dirName(it:String) = modeManager[it]?.name ?: it
	private var listInternal:List<ReplayCol> = emptyList()
	override var list:List<String>
		get() = listInternal.map {(if(it.dir) dirName(it.name) else null) ?: it.name}
		set(value) {}

	private data class ReplayCol(val file:File, val name:String,
		/** Mode name */
		val mode:String = "",
		/** Rule name */
		val rule:String = "",
		/** Scoreなどの情報 */
		val stats:Statistics? = null, val dir:Boolean = false)

	init {
		pageHeight = PAGE_HEIGHT
		emptyError = "NO REPLAY FILE"
	}

	/* Fetch this state's ID */
	override fun getID():Int = ID

	/* State initialization */
	//override fun init(container:GameContainer, game:StateBasedGame) {}

	/* Called when entering this state */
	override fun enter(container:GameContainer?, game:StateBasedGame?) {
		super.enter(container, game)
		getReplayFileList()
	}

	/** リプレイファイル一覧を取得
	 * @param dir ディレクトリ指定、null/空で
	 * @return リプレイファイルのFilenameの配列。
	 */
	private fun getReplayFileList(dir:String? = null) {
		if(!dir.isNullOrEmpty()) {
			directoryPlace.add(dir)
			cursorHistory.add(cursor)
			cursor = 0
		} else if(directoryPlace.isNotEmpty()) {
			directoryPlace.removeLast()
			cursor = cursorHistory.removeLast()
		}

		val d = NullpoMinoSlick.propGlobal.getProperty("custom.replay.directory", "replay")+
			directoryPlace.joinToString("${File.separatorChar}", "${File.separatorChar}")

		val fd = File(d)
		listInternal = fd.listFiles.sortedBy {it.name}.sortedByDescending {it.isDirectory}.map {
			if(it.isDirectory) ReplayCol(it, it.name, stats = Statistics().apply {
				lines = it.listFilesOrEmpty.count {lf -> isReplay(lf)}
			}, dir = true)
			else if(isReplay(it)) try {
				val gis = GZIPInputStream(FileInputStream(it.javaFile()))
				val prop = CustomProperties().apply {
					load(gis)
				}
				gis.close()

				ReplayCol(it, it.name, prop.getProperty("name.dir", ""), prop.getProperty("name.rule", ""),
					Statistics().apply {
						readProperty(prop, 0)
					})
			} catch(e:IOException) {
				log.error("Failed to load replay file ($it)", e)
				ReplayCol(it, it.name)
			} else ReplayCol(it, it.name)

		}
	}

	private fun isReplay(it:File) = it.isFile&&it.name.endsWith(".rep")

	override fun onRenderSuccess(container:GameContainer, game:StateBasedGame, graphics:Graphics) {
		FontNormal.printFontGrid(1, 1, "SELECT REPLAY FILE (${cursor+1}/${list.size})", COLOR.ORANGE)
		if(directoryPlace.isNotEmpty()) FontNano.printFont(
			8, 36, directoryPlace.joinToString(">", ">") {dirName(it)}, COLOR.ORANGE
		)

		listInternal[cursor].let {
			if(it.dir) {
				FontNormal.printFontGrid(1, 24, "REPLAYS:${it.stats?.lines}", COLOR.CYAN)
			} else if(it.stats!=null) {
				val s = it.stats
				FontNormal.printFontGrid(1, 24, "MODE:${it.mode} RULE:${it.rule}", COLOR.CYAN)
				FontNormal.printFontGrid(1, 25, "SCORE:${s.score} LINE:${s.lines}", COLOR.CYAN)
				FontNormal.printFontGrid(1, 26, "LEVEL:${s.level+s.levelDispAdd} TIME:${s.time.toTimeStr}", COLOR.CYAN)
				FontNormal.printFontGrid(1, 27, "GAME RATE:${if(s.gamerate==0f) "UNKNOWN" else "${100*s.gamerate}%"}", COLOR.CYAN)
				FontNano.printFontGrid(1, 28, "SEED:${s.randSeed}", COLOR.CYAN)
			} else FontNormal.printFontGrid(1, 25, "*INVALID FILE*", COLOR.RED)
		}
	}

	override fun onDecide(container:GameContainer, game:StateBasedGame, delta:Int):Boolean {
		if(list.isEmpty()) return false
		if(listInternal[cursor].dir) {
			ResourceHolder.soundManager.play("decide1")
			getReplayFileList(listInternal[cursor].name)
		} else if(listInternal[cursor].stats!=null) {
			val prop = CustomProperties()
			try {
				ResourceHolder.soundManager.play("twist")
				val gis = GZIPInputStream(
					FileInputStream(
						NullpoMinoSlick.propGlobal.getProperty("custom.replay.directory", "replay")+
							directoryPlace.joinToString("${File.separatorChar}", "${File.separatorChar}", "${File.separatorChar}")+
							listInternal[cursor].name
					)
				)
				prop.load(gis)
				gis.close()
			} catch(e:IOException) {
				ResourceHolder.soundManager.play("rotfail")
				log.error("Failed to load replay file from ${list[cursor]}", e)
				return true
			}
			ResourceHolder.soundManager.play("decide0")
			NullpoMinoSlick.stateInGame.startReplayGame(prop)
			game.enterState(StateInGame.ID)
		} else ResourceHolder.soundManager.play("rotfail")
		return false
	}

	override fun onCancel(container:GameContainer, game:StateBasedGame, delta:Int):Boolean {
		ResourceHolder.soundManager.play("cancel")
		if(directoryPlace.isEmpty())
			game.enterState(StateTitle.ID)
		else getReplayFileList(null)
		return true
	}

	companion object {
		/** This state's ID */
		const val ID = 4

		/** 1画面に表示するMaximumファイルcount */
		const val PAGE_HEIGHT = 20

		/** Log */
		internal val log = LogManager.getLogger()
	}
}
