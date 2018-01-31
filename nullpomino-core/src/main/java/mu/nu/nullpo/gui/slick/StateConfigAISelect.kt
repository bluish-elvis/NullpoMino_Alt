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
import mu.nu.nullpo.game.subsystem.ai.AIPlayer
import mu.nu.nullpo.gui.GameKeyDummy
import mu.nu.nullpo.util.GeneralUtil
import org.apache.log4j.Logger
import org.newdawn.slick.GameContainer
import org.newdawn.slick.Graphics
import org.newdawn.slick.state.StateBasedGame
import java.io.*
import java.util.*

/** AI config screen state */
class StateConfigAISelect:BaseGameState() {

	/** Player ID */
	var player = 0

	/** AIのクラス一覧 */
	private var aiPathList:Array<String> = emptyArray()

	/** AIのName一覧 */
	private var aiNameList:Array<String> = emptyArray()

	/** Current AIのクラス */
	private var currentAI:String = ""

	/** AIのID */
	private var aiID = 0

	/** AIの移動間隔 */
	private var aiMoveDelay = 0

	/** AIの思考の待ち time */
	private var aiThinkDelay = 0

	/** AIでスレッドを使う */
	private var aiUseThread = false

	private var aiShowHint = false

	private var aiPrethink = false

	private var aiShowState = false

	/** Cursor position */
	private var cursor = 0

	/* Fetch this state's ID */
	override fun getID():Int = ID

	/* State initialization */
	override fun init(container:GameContainer, game:StateBasedGame) {
		try {
			val `in` = BufferedReader(FileReader("config/list/ai.lst"))
			aiPathList = loadAIList(`in`)
			aiNameList = loadAINames(aiPathList)
			`in`.close()
		} catch(e:IOException) {
			log.error("Failed to load AI list", e)
		}

	}

	/* Called when entering this state */
	override fun enter(container:GameContainer?, game:StateBasedGame?) {
		currentAI = NullpoMinoSlick.propGlobal.getProperty(player.toString()+".ai", "")
		aiMoveDelay = NullpoMinoSlick.propGlobal.getProperty(player.toString()+".aiMoveDelay", 0)
		aiThinkDelay = NullpoMinoSlick.propGlobal.getProperty(player.toString()+".aiThinkDelay", 0)
		aiUseThread = NullpoMinoSlick.propGlobal.getProperty(player.toString()+".aiUseThread", true)
		aiShowHint = NullpoMinoSlick.propGlobal.getProperty(player.toString()+".aiShowHint", false)
		aiPrethink = NullpoMinoSlick.propGlobal.getProperty(player.toString()+".aiPrethink", false)
		aiShowState = NullpoMinoSlick.propGlobal.getProperty(player.toString()+".aiShowState", false)

		aiID = -1
		for(i in aiPathList.indices)
			if(currentAI==aiPathList[i]) aiID = i
	}

	/** AI一覧を読み込み
	 * @param bf 読み込み元のテキストファイル
	 * @return AI一覧
	 */
	fun loadAIList(bf:BufferedReader):Array<String> {
		val aiArrayList = ArrayList<String>()

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

		return Array(aiArrayList.size) {aiArrayList[it]}
	}

	/** AIのName一覧を作成
	 * @param aiPath AIのクラスのリスト
	 * @return AIのName一覧
	 */
	fun loadAINames(aiPath:Array<String>):Array<String> = Array(aiPath.size){
		val aiClass:Class<*>
		val aiObj:AIPlayer
		try {
			aiClass = Class.forName(aiPath[it])
			aiObj = aiClass.newInstance() as AIPlayer
			return@Array aiObj.name
		} catch(e:ClassNotFoundException) {
			log.error("AI class "+aiPath[it]+" not found", e)
		} catch(e:Throwable) {
			log.error("AI class "+aiPath[it]+" load failed", e)
		}
		return@Array "(INVALID)"
	}

	/* Draw the screen */
	override fun renderImpl(container:GameContainer, game:StateBasedGame, g:Graphics) {
		// Background
		g.drawImage(ResourceHolder.imgMenuBG[0], 0f, 0f)

		// Menu
		FontNormal.printFontGrid(1, 1, (player+1).toString()+"P AI SETTING", COLOR.ORANGE)

		FontNormal.printFontGrid(1, 3+cursor, "b", COLOR.RED)

		val aiName:String = if(aiID<0)
			"(DISABLE)"
		else
			aiNameList[aiID].toUpperCase()
		FontNormal.printFontGrid(2, 3, "AI TYPE:$aiName", cursor==0)
		FontNormal.printFontGrid(2, 4, "AI MOVE DELAY:$aiMoveDelay", cursor==1)
		FontNormal.printFontGrid(2, 5, "AI THINK DELAY:$aiThinkDelay", cursor==2)
		FontNormal.printFontGrid(2, 6, "AI USE THREAD:"+GeneralUtil.getONorOFF(aiUseThread), cursor==3)
		FontNormal.printFontGrid(2, 7, "AI SHOW HINT:"+GeneralUtil.getONorOFF(aiShowHint), cursor==4)
		FontNormal.printFontGrid(2, 8, "AI PRE-THINK:"+GeneralUtil.getONorOFF(aiPrethink), cursor==5)
		FontNormal.printFontGrid(2, 9, "AI SHOW INFO:"+GeneralUtil.getONorOFF(aiShowState), cursor==6)

		FontNormal.printFontGrid(1, 28, "A:OK B:CANCEL", COLOR.GREEN)
	}

	/* Update game state */
	override fun updateImpl(container:GameContainer, game:StateBasedGame, delta:Int) {
		// Update key input states
		GameKey.gamekey[0].update(container.input)

		// Cursor movement
		if(GameKey.gamekey[0].isMenuRepeatKey(GameKeyDummy.BUTTON_UP)) {
			cursor--
			if(cursor<0) cursor = 5
			ResourceHolder.soundManager.play("cursor")
		}
		if(GameKey.gamekey[0].isMenuRepeatKey(GameKeyDummy.BUTTON_DOWN)) {
			cursor++
			if(cursor>5) cursor = 0
			ResourceHolder.soundManager.play("cursor")
		}

		// Configuration changes
		var change = 0
		if(GameKey.gamekey[0].isMenuRepeatKey(GameKeyDummy.BUTTON_LEFT)) change = -1
		if(GameKey.gamekey[0].isMenuRepeatKey(GameKeyDummy.BUTTON_RIGHT)) change = 1

		if(change!=0) {
			ResourceHolder.soundManager.play("change")

			when(cursor) {
				0 -> {
					aiID += change
					if(aiID<-1) aiID = aiNameList.size-1
					if(aiID>aiNameList.size-1) aiID = -1
				}
				1 -> {
					aiMoveDelay += change
					if(aiMoveDelay<-1) aiMoveDelay = 99
					if(aiMoveDelay>99) aiMoveDelay = -1
				}
				2 -> {
					aiThinkDelay += change*10
					if(aiThinkDelay<0) aiThinkDelay = 1000
					if(aiThinkDelay>1000) aiThinkDelay = 0
				}
				3 -> aiUseThread = !aiUseThread
				4 -> aiShowHint = !aiShowHint
				5 -> aiPrethink = !aiPrethink
				6 -> aiShowState = !aiShowState
			}
		}

		// Confirm button
		if(GameKey.gamekey[0].isPushKey(GameKeyDummy.BUTTON_A)) {
			ResourceHolder.soundManager.play("decide1")


				NullpoMinoSlick.propGlobal.setProperty(player.toString()+".ai", if(aiID>=0)aiPathList[aiID] else "")
			NullpoMinoSlick.propGlobal.setProperty(player.toString()+".aiMoveDelay", aiMoveDelay)
			NullpoMinoSlick.propGlobal.setProperty(player.toString()+".aiThinkDelay", aiThinkDelay)
			NullpoMinoSlick.propGlobal.setProperty(player.toString()+".aiUseThread", aiUseThread)
			NullpoMinoSlick.propGlobal.setProperty(player.toString()+".aiShowHint", aiShowHint)
			NullpoMinoSlick.propGlobal.setProperty(player.toString()+".aiPrethink", aiPrethink)
			NullpoMinoSlick.propGlobal.setProperty(player.toString()+".aiShowState", aiShowState)
			NullpoMinoSlick.saveConfig()

			game.enterState(StateConfigMainMenu.ID)
			return
		}

		// Cancel button
		if(GameKey.gamekey[0].isPushKey(GameKeyDummy.BUTTON_B)) {
			game.enterState(StateConfigMainMenu.ID)
			return
		}
	}

	companion object {
		/** This state's ID */
		const val ID = 8

		/** 1画面に表示するMaximumAIcount */
		const val MAX_AI_IN_ONE_PAGE = 20

		/** Log */
		internal val log = Logger.getLogger(StateConfigAISelect::class.java)
	}
}
