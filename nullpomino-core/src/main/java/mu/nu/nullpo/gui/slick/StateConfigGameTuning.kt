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

import mu.nu.nullpo.game.component.*
import mu.nu.nullpo.game.component.SpeedParam.Companion.SDS_FIXED
import mu.nu.nullpo.game.event.EventReceiver.COLOR
import mu.nu.nullpo.game.play.GameManager
import mu.nu.nullpo.game.subsystem.mode.PreviewMode
import mu.nu.nullpo.gui.GameKeyDummy
import mu.nu.nullpo.gui.slick.img.FontNano
import mu.nu.nullpo.gui.slick.img.FontNormal
import mu.nu.nullpo.util.CustomProperties
import mu.nu.nullpo.util.GeneralUtil
import org.apache.log4j.Logger
import org.newdawn.slick.*
import org.newdawn.slick.state.StateBasedGame
import java.util.*

/** Game Tuning menu state */
class StateConfigGameTuning:BaseGameState() {

	/** Player number */
	var player = 0

	/** Preview flag */
	private var isPreview:Boolean = false

	/** AppGameContainer (Used by preview) */
	private lateinit var appContainer:AppGameContainer

	/** Game Manager for preview */
	private var gameManager:GameManager? = null

	/** Cursor position */
	private var cursor = 0

	/** A button rotation -1=Auto 0=Always CCW 1=Always CW */
	private var owRotateButtonDefaultRight:Int = 0

	/** Block Skin -1=Auto 0 or above=Fixed */
	private var owSkin:Int = 0

	/** Min/Max DAS -1=Auto 0 or above=Fixed */
	private var owMinDAS:Int = 0
	private var owMaxDAS:Int = 0

	/** DAS Repeat Rate -1=Auto 0 or above=Fixed */
	private var owDASRate:Int = 0

	/** SoftDrop Speed -1=Auto =Fixed Factor above 6=Always x5-20 Speed */
	private var owSDSpd:Int = 0

	/** Reverse the roles of up/down keys in-game */
	private var owReverseUpDown:Boolean = false

	/** Diagonal move (-1=Auto 0=Disable 1=Enable) */
	private var owMoveDiagonal:Int = 0

	/** Outline type (-1:Auto 0orAbove:Fixed) */
	private var owBlockOutlineType:Int = 0

	/** Show outline only flag (-1:Auto 0:Always Normal 1:Always Outline Only) */
	private var owBlockShowOutlineOnly:Int = 0

	private var sk:Int = 0

	private var spdpv:Float = 0f

	/* Fetch this state's ID */
	override fun getID():Int = ID

	/* State initialization */
	override fun init(container:GameContainer, game:StateBasedGame) {

		if(container is AppGameContainer)
			appContainer = container
		else
			log.error("This container isn't AppGameContainer")
	}

	/** Load settings
	 * @param prop Property file to read from
	 */
	private fun loadConfig(prop:CustomProperties) {
		owRotateButtonDefaultRight = prop.getProperty("$player.tuning.owRotateButtonDefaultRight", -1)
		owSkin = prop.getProperty("$player.tuning.owSkin", -1)
		owMinDAS = prop.getProperty("$player.tuning.owMinDAS", -1)
		owMaxDAS = prop.getProperty("$player.tuning.owMaxDAS", -1)
		owDASRate = prop.getProperty("$player.tuning.owDasDelay", -1)
		owSDSpd = prop.getProperty("$player.tuning.owSDSpd", -1)
		owReverseUpDown = prop.getProperty("$player.tuning.owReverseUpDown", false)
		owMoveDiagonal = prop.getProperty("$player.tuning.owMoveDiagonal", -1)
		owBlockOutlineType = prop.getProperty("$player.tuning.owBlockOutlineType", -1)
		owBlockShowOutlineOnly = prop.getProperty("$player.tuning.owBlockShowOutlineOnly", -1)
	}

	/** Save settings
	 * @param prop Property file to save to
	 */
	private fun saveConfig(prop:CustomProperties) {
		prop.setProperty("$player.tuning.owRotateButtonDefaultRight", owRotateButtonDefaultRight)
		prop.setProperty("$player.tuning.owSkin", owSkin)
		prop.setProperty("$player.tuning.owMinDAS", owMinDAS)
		prop.setProperty("$player.tuning.owMaxDAS", owMaxDAS)
		prop.setProperty("$player.tuning.owDasDelay", owDASRate)
		prop.setProperty("$player.tuning.owSDSpd", owSDSpd)
		prop.setProperty("$player.tuning.owReverseUpDown", owReverseUpDown)
		prop.setProperty("$player.tuning.owMoveDiagonal", owMoveDiagonal)
		prop.setProperty("$player.tuning.owBlockOutlineType", owBlockOutlineType)
		prop.setProperty("$player.tuning.owBlockShowOutlineOnly", owBlockShowOutlineOnly)
	}

	/* Called when entering this state */
	override fun enter(container:GameContainer?, game:StateBasedGame?) {
		super.enter(container, game)
		isPreview = false
		loadConfig(NullpoMinoSlick.propGlobal)
	}

	/* Called when leaving the state */
	override fun leave(container:GameContainer?, game:StateBasedGame?) {
		super.leave(container, game)
		stopPreviewGame()
	}

	/** Start the preview game */
	private fun startPreviewGame() {
		gameManager = GameManager(RendererSlick()).also {
			it.receiver.setGraphics(appContainer.graphics)

			it.mode = PreviewMode()
			it.init()

			it.backgroundStatus.bg = -1 // Force no BG

			// Initialization for each player
			for(i in 0 until it.players) {
				// Tuning
				it.engine[i].owRotateButtonDefaultRight = owRotateButtonDefaultRight
				it.engine[i].owSkin = owSkin
				it.engine[i].owMinDAS = owMinDAS
				it.engine[i].owMaxDAS = owMaxDAS
				it.engine[i].owDASDelay = owDASRate
				it.engine[i].owSDSpd = owSDSpd
				it.engine[i].owReverseUpDown = owReverseUpDown
				it.engine[i].owMoveDiagonal = owMoveDiagonal
				it.engine[i].owBlockOutlineType = owBlockOutlineType
				it.engine[i].owBlockShowOutlineOnly = owBlockShowOutlineOnly
				it.engine[i].lives = 99
				// Rule
				val ruleopt:RuleOptions
				var rulename:String? = NullpoMinoSlick.propGlobal.getProperty("$i.rule", "")
				if(it.mode!!.gameStyle>0)
					rulename = NullpoMinoSlick.propGlobal.getProperty("$i"+".rule."
						+it.mode!!.gameStyle, "")
				if(rulename!=null&&rulename.isNotEmpty()) {
					log.info("Load rule options from $rulename")
					ruleopt = GeneralUtil.loadRule(rulename)
				} else {
					log.info("Load rule options from setting file")
					ruleopt = RuleOptions()
					ruleopt.readProperty(NullpoMinoSlick.propGlobal, i)
				}
				it.engine[i].ruleopt = ruleopt

				// Randomizer
				if(ruleopt.strRandomizer.isNotEmpty())
					it.engine[i].randomizer = GeneralUtil.loadRandomizer(ruleopt.strRandomizer)

				// Wallkick
				if(ruleopt.strWallkick.isNotEmpty())
					it.engine[i].wallkick = GeneralUtil.loadWallkick(ruleopt.strWallkick)

				// AI
				val aiName = NullpoMinoSlick.propGlobal.getProperty("$i.ai", "")
				if(aiName.isNotEmpty()) {
					it.engine[i].ai = GeneralUtil.loadAIPlayer(aiName)
					it.engine[i].aiMoveDelay = NullpoMinoSlick.propGlobal.getProperty("$i.aiMoveDelay", 0)
					it.engine[i].aiThinkDelay = NullpoMinoSlick.propGlobal.getProperty("$i.aiThinkDelay", 0)
					it.engine[i].aiUseThread = NullpoMinoSlick.propGlobal.getProperty("$i.aiUseThread", true)
					it.engine[i].aiShowHint = NullpoMinoSlick.propGlobal.getProperty("$i.aiShowHint", false)
					it.engine[i].aiPrethink = NullpoMinoSlick.propGlobal.getProperty("$i.aiPrethink", false)
					it.engine[i].aiShowState = NullpoMinoSlick.propGlobal.getProperty("$i.aiShowState", false)
				}
				it.showInput = NullpoMinoSlick.propConfig.getProperty("option.showInput", false)

				// Init
				it.engine[i].init()
			}
		}
		isPreview = true
	}

	/** Stop the preview game */
	private fun stopPreviewGame() {
		if(isPreview) {
			isPreview = false
			gameManager?.shutdown()
			gameManager = null

		}
	}

	/* Draw the game screen */
	override fun renderImpl(container:GameContainer, game:StateBasedGame, g:Graphics) {
		g.drawImage(ResourceHolder.imgMenuBG[0], 0f, 0f)

		if(isPreview)
		// Preview
			try {
				gameManager?.let {
					val engine = it.engine.first()
					val strButtonF = it.receiver.getKeyNameByButtonID(engine, Controller.BUTTON_F)
					val fontY = if(it.receiver.nextDisplayType==2) 1 else 27
					FontNormal.printFontGrid(1, fontY,
						"PUSH F BUTTON (ASSIGNED: ${strButtonF.toUpperCase()}) TO EXIT", COLOR.YELLOW)
					val spd = engine.speed
					val ow = engine.softDropSpd
					FontNano.printFontGrid(16,13,"${spd.gravity}>$ow/${spd.denominator} ${engine.gcount}")
					it.renderAll()
				}
			} catch(e:Exception) {
				log.error("Render fail", e)
			}
		else {
			// Menu
			var strTemp = ""

			FontNormal.printFontGrid(1, 1, "GAME TUNING (${player+1}P)", COLOR.ORANGE)
			FontNormal.printFontGrid(1, 3+cursor, "\u0082", COLOR.RAINBOW)

			if(owRotateButtonDefaultRight==-1) strTemp = "AUTO"
			if(owRotateButtonDefaultRight==0) strTemp = "LEFT"
			if(owRotateButtonDefaultRight==1) strTemp = "RIGHT"
			FontNormal.printFontGrid(2, 3, "A BUTTON ROTATE:$strTemp", cursor==0)

			val skinmax = ResourceHolder.imgNormalBlockList.size
			sk = when(owSkin) {
				-1 -> (sk+1)%skinmax
				-2 -> Random().nextInt(skinmax)
				else -> owSkin
			}
			val imgBlock = ResourceHolder.imgNormalBlockList[sk]
			if(ResourceHolder.blockStickyFlagList[sk])
				for(j in 0..8) imgBlock.draw((160+j*16).toFloat(), 64f, (160+j*16+16).toFloat(), (64+16).toFloat(), 0f, (j*16).toFloat(), 16f, (j*16+16).toFloat())
			else
				imgBlock.draw(160f, 64f, (160+144).toFloat(), (64+16).toFloat(), 0f, 0f, 144f, 16f)
			FontNormal.printFontGrid(2, 4, "SKIN:${String.format("%02d", owSkin)}:", cursor==1
				, COLOR.WHITE, if(ResourceHolder.blockStickyFlagList[sk]) COLOR.BLUE else COLOR.RED)
			FontNormal.printFontGrid(19, 4, when(owSkin) {
				-1 -> "AUTO"
				-2 -> "RANDOM"
				else -> NullpoMinoSlick.propSkins.getProperty("Skin$owSkin", "").toUpperCase()
			}, cursor==1, COLOR.WHITE, if(ResourceHolder.blockStickyFlagList[sk]) COLOR.BLUE else COLOR.RED)

			FontNormal.printFontGrid(2, 5, "min DAS:"+if(owMinDAS==-1) "AUTO" else "$owMinDAS", cursor==2, rainbow = (spdpv/2).toInt())
			FontNormal.printFontGrid(2, 6, "max DAS:"+if(owMaxDAS==-1) "AUTO" else "$owMaxDAS", cursor==3, rainbow = (spdpv/2).toInt())
			FontNormal.printFontGrid(2, 7, "DAS delay:"+if(owDASRate==-1) "AUTO" else "$owDASRate", cursor==4, rainbow = (spdpv/2).toInt())
			FontNormal.printFontGrid(2, 8, "SoftDrop Speed:"+if(owSDSpd==-1) "AUTO" else
				if(owSDSpd<SDS_FIXED.size) "${SDS_FIXED[owSDSpd]}G" else "*${owSDSpd-SDS_FIXED.size+5}", cursor==5, rainbow = (spdpv/4).toInt())
			FontNormal.printFontGrid(2, 9, "Reverse UP/DOWN:"+GeneralUtil.getOorX(owReverseUpDown), cursor==6)

			if(owMoveDiagonal==-1) strTemp = "AUTO"
			if(owMoveDiagonal==0) strTemp = "\u0085"
			if(owMoveDiagonal==1) strTemp = "\u0083"
			FontNormal.printFontGrid(2, 10, "Diagonal Move:$strTemp", cursor==7)

			FontNormal.printFontGrid(2, 11, "OUTLINE TYPE:"+OUTLINE_TYPE_NAMES[owBlockOutlineType+1], cursor==8)

			if(owBlockShowOutlineOnly==-1) strTemp = "AUTO"
			if(owBlockShowOutlineOnly==0) strTemp = "\u0085"
			if(owBlockShowOutlineOnly==1) strTemp = "\u0083"
			FontNormal.printFontGrid(2, 12, "SHOW Outline Only:$strTemp", cursor==9)

			FontNormal.printFontGrid(2, 13, "[PREVIEW]", cursor==10)
			FontNano.printFontGrid(13, 13, "HOTKEY : D BUTTON")

			if(cursor>=0&&cursor<UI_TEXT.size)
				FontNormal.printTTF(16, 432, NullpoMinoSlick.getUIText(UI_TEXT[cursor]))
		}
	}

	/* Update game state */
	@Throws(SlickException::class)
	override fun updateImpl(container:GameContainer, game:StateBasedGame, delta:Int) {
		spdpv += when(cursor) {
			2, 3, 4 -> {
				when(cursor) {
					2 -> owMinDAS
					3 -> owMaxDAS
					4 -> owDASRate
					else -> -1
				}.let {
					when {
						it<0 -> 1f
						it==0 -> 0f
						else -> 20f/(it*2+1)
					}
				}-if(spdpv>18) 18f else 0f
			}
			5 -> {
				owSDSpd.let {
					when(it) {
						in SDS_FIXED.indices ->
							if(it==SDS_FIXED.indices.last) 0f else (SDS_FIXED[it]*3)
						in SDS_FIXED.size..SDS_FIXED.size+15 ->
							(it-SDS_FIXED.size+5f)/3f
						else -> 2f
					}
				}-if(spdpv>36) 36f else 0f
			}
			else -> 0f
		}

		if(isPreview)
		// Preview
			try {
				GameKey.gamekey[0].update(container.input, true)

				// Execute game loops
				GameKey.gamekey[0].inputStatusUpdate(gameManager!!.engine[0].ctrl)
				gameManager!!.updateAll()

				// Retry button
				if(GameKey.gamekey[0].isMenuRepeatKey(GameKeyDummy.BUTTON_RETRY)) {
					gameManager!!.reset()
					gameManager!!.backgroundStatus.bg = -1 // Force no BG
				}

				// Exit
				if(GameKey.gamekey[0].isMenuRepeatKey(GameKeyDummy.BUTTON_F)
					||GameKey.gamekey[0].isMenuRepeatKey(GameKeyDummy.BUTTON_GIVEUP)||
					gameManager!!.quitFlag)
					stopPreviewGame()
			} catch(e:Exception) {
				log.error("Update fail", e)
			}
		else {
			// Menu screen
			GameKey.gamekey[0].update(container.input, false)

			// TTF font
			if(ResourceHolder.ttfFont!=null) ResourceHolder.ttfFont!!.loadGlyphs()

			// Cursor movement
			if(GameKey.gamekey[0].isMenuRepeatKey(GameKeyDummy.BUTTON_UP)) {
				cursor--
				if(cursor<0) cursor = 10
				ResourceHolder.soundManager.play("cursor")
			}
			if(GameKey.gamekey[0].isMenuRepeatKey(GameKeyDummy.BUTTON_DOWN)) {
				cursor++
				if(cursor>10) cursor = 0
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
						owRotateButtonDefaultRight += change
						if(owRotateButtonDefaultRight<-1) owRotateButtonDefaultRight = 1
						if(owRotateButtonDefaultRight>1) owRotateButtonDefaultRight = -1
					}
					1 -> {
						owSkin += change
						if(owSkin<-2) owSkin = ResourceHolder.imgNormalBlockList.size-1
						if(owSkin>ResourceHolder.imgNormalBlockList.size-1) owSkin = -2
					}
					2 -> {
						owMinDAS += change
						if(owMinDAS<-1) owMinDAS = 99
						if(owMinDAS>99) owMinDAS = -1
					}
					3 -> {
						owMaxDAS += change
						if(owMaxDAS<-1) owMaxDAS = 99
						if(owMaxDAS>99) owMaxDAS = -1
					}
					4 -> {
						owDASRate += change
						if(owDASRate<-1) owDASRate = 99
						if(owDASRate>99) owDASRate = -1
					}
					5 -> {
						owSDSpd += change
						if(owSDSpd<-1) owSDSpd = SDS_FIXED.size+15
						if(owSDSpd>SDS_FIXED.size+15) owSDSpd = -1
					}
					6 -> owReverseUpDown = owReverseUpDown xor true
					7 -> {
						owMoveDiagonal += change
						if(owMoveDiagonal<-1) owMoveDiagonal = 1
						if(owMoveDiagonal>1) owMoveDiagonal = -1
					}
					8 -> {
						owBlockOutlineType += change
						if(owBlockOutlineType<-1) owBlockOutlineType = 3
						if(owBlockOutlineType>3) owBlockOutlineType = -1
					}
					9 -> {
						owBlockShowOutlineOnly += change
						if(owBlockShowOutlineOnly<-1) owBlockShowOutlineOnly = 1
						if(owBlockShowOutlineOnly>1) owBlockShowOutlineOnly = -1
					}
					10 -> {
					}
				}
			}

			// Preview by D button
			if(GameKey.gamekey[0].isPushKey(GameKeyDummy.BUTTON_D)) {
				ResourceHolder.soundManager.play("decide")
				startPreviewGame()
				return
			}

			// Confirm button
			if(GameKey.gamekey[0].isPushKey(GameKeyDummy.BUTTON_A)) {
				ResourceHolder.soundManager.play("decide2")

				if(cursor==10) {
					// Preview
					startPreviewGame()
					return
				}
				// Save
				saveConfig(NullpoMinoSlick.propGlobal)
				NullpoMinoSlick.saveConfig()
				game.enterState(StateConfigMainMenu.ID)
			}

			// Cancel button
			if(GameKey.gamekey[0].isPushKey(GameKeyDummy.BUTTON_B)) {
				ResourceHolder.soundManager.play("cancel")
				loadConfig(NullpoMinoSlick.propGlobal)
				game.enterState(StateConfigMainMenu.ID)
			}
		}
	}

	companion object {
		/** This state's ID */
		const val ID = 14

		/** UI Text identifier Strings */
		private val UI_TEXT =
			arrayOf("GameTuning_RotateButtonDefaultRight", "GameTuning_Skin", "GameTuning_MinDAS", "GameTuning_MaxDAS", "GameTuning_DasDelay", "GameTuning_ReverseUpDown", "GameTuning_MoveDiagonal", "GameTuning_BlockOutlineType", "GameTuning_BlockShowOutlineOnly", "GameTuning_Preview")

		/** Log */
		internal val log = Logger.getLogger(StateConfigGameTuning::class.java)

		/** Outline type names */
		private val OUTLINE_TYPE_NAMES = arrayOf("AUTO", "NONE", "NORMAL", "CONNECT", "SAMECOLOR")
	}
}
