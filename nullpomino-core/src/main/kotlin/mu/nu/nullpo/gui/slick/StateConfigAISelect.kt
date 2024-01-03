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
import mu.nu.nullpo.game.subsystem.ai.AIPlayer
import mu.nu.nullpo.gui.common.ConfigGlobal.AIConf
import mu.nu.nullpo.gui.slick.img.FontNormal
import mu.nu.nullpo.util.GeneralUtil.getONorOFF
import org.apache.logging.log4j.LogManager
import org.newdawn.slick.GameContainer
import org.newdawn.slick.Graphics
import org.newdawn.slick.state.StateBasedGame
import java.io.BufferedReader
import java.io.FileReader
import java.io.IOException

/** AI config screen state */
internal class StateConfigAISelect:BaseMenuConfigState() {
	/** Player ID */
	var player = 0
	override val title:String get() = "${(player+1)}P AI setting"
	/** AIのクラス一覧 */
	private var aiPathList:List<String> = emptyList()

	/** AIのName一覧 */
	private var aiNameList:List<String> = emptyList()

	/** AIのID */
	private var aiID = 0

	private var ai = AIConf()

	/* Fetch this state's ID */
	override fun getID():Int = ID

	/* State initialization */
	override fun init(container:GameContainer, game:StateBasedGame) {
		try {
			this::class.java.getResource("/ai.lst")?.file?.let {
				val bf = BufferedReader(FileReader(it))
				aiPathList = loadAIList(bf)
				aiNameList = loadAINames(aiPathList)
				bf.close()
			}
		} catch(e:IOException) {
			log.error("Failed to load AI list", e)
		}
	}

	/* Called when entering this state */
	override fun enter(container:GameContainer?, game:StateBasedGame?) {
		ai = NullpoMinoSlick.propGlobal.ai.getOrElse(player) {AIConf()}
		aiID = -1
		for(i in aiPathList.indices)
			if(ai.name==aiPathList[i]) aiID = i
	}

	/** AI一覧を読み込み
	 * @param bf 読み込み元のテキストファイル
	 * @return AI一覧
	 */
	private fun loadAIList(bf:BufferedReader):List<String> {
		val aiArrayList = mutableListOf<String>()

		while(true) {
			val name:String?
			try {
				name = bf.readLine()
			} catch(e:Exception) {
				break
			}

			if(name.isNullOrEmpty()) break

			if(!name.startsWith("#")) aiArrayList.add(name)
		}

		return aiArrayList.toList()
	}

	/** AIのName一覧を作成
	 * @param aiPath AIのクラスのリスト
	 * @return AIのName一覧
	 */
	private fun loadAINames(aiPath:List<String>):List<String> = List(aiPath.size) {
		val aiClass:Class<*>
		val aiObj:AIPlayer
		try {
			aiClass = Class.forName(aiPath[it])
			aiObj = aiClass.getDeclaredConstructor().newInstance() as AIPlayer
			return@List aiObj.name
		} catch(e:ClassNotFoundException) {
			log.error("AI class ${aiPath[it]} not found", e)
		} catch(e:Throwable) {
			log.error("AI class ${aiPath[it]} load failed", e)
		}
		return@List "(INVALID)"
	}

	/* Draw the screen */
	override fun renderImpl(container:GameContainer, game:StateBasedGame, g:Graphics) {
		super.renderImpl(container, game, g)
		FontNormal.printFontGrid(1, 28, "A:OK B:CANCEL", COLOR.GREEN)
	}

	override fun onDecide(container:GameContainer, game:StateBasedGame, delta:Int):Boolean {
		ResourceHolder.soundManager.play("decide1")

		if(player !in NullpoMinoSlick.propGlobal.ai.indices) NullpoMinoSlick.propGlobal.ai.add(player, ai)
		else NullpoMinoSlick.propGlobal.ai[player] = ai
		NullpoMinoSlick.saveConfig()

		game.enterState(StateConfigMainMenu.ID)
		return true
	}

	override fun onCancel(container:GameContainer, game:StateBasedGame, delta:Int):Boolean {
		game.enterState(StateConfigMainMenu.ID)
		return true
	}

	override val columns:List<Pair<String, List<Column>>>
		get() = listOf(
			"" to listOf(
				Column({"AI type:"+if(aiID<0) "(disable)" else aiNameList[aiID]}, {
					aiID += it
					if(aiID<-1) aiID = aiNameList.size-1
					if(aiID>aiNameList.size-1) aiID = -1
					ai.name = if(aiID>=0) aiPathList[aiID] else ""
				}),
				Column({"AI move delay:"+ai.moveDelay}, {
					ai.moveDelay += it
					if(ai.moveDelay<-1) ai.moveDelay = 99
					if(ai.moveDelay>99) ai.moveDelay = -1
				}),
				Column({"AI think delay:"+ai.thinkDelay}, {
					ai.thinkDelay += it*10
					if(ai.thinkDelay<0) ai.thinkDelay = 1000
					if(ai.thinkDelay>1000) ai.thinkDelay = 0
				}),
				Column({"AI use thread:"+ai.useThread.getONorOFF()}, {!ai.useThread}),
				Column({"AI show hint:"+ai.showHint.getONorOFF()}, {!ai.showHint}),
				Column({"AI pre-think:"+ai.preThink.getONorOFF()}, {!ai.preThink}),
				Column({"AI show info:"+ai.showState.getONorOFF()}, {!ai.showState}),
			)
		)

	companion object {
		/** This state's ID */
		const val ID = 8

		/** 1画面に表示するMaximumAIcount */
		const val MAX_AI_IN_ONE_PAGE = 20

		/** Log */
		internal val log = LogManager.getLogger()
	}
}
