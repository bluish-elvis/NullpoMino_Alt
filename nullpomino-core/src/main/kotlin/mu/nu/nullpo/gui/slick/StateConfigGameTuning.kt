/*
 * Copyright (c) 2010-2023, NullNoname
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

import mu.nu.nullpo.game.component.Controller
import mu.nu.nullpo.game.component.RuleOptions
import mu.nu.nullpo.game.component.SpeedParam.Companion.SDS_FIXED
import mu.nu.nullpo.game.event.EventReceiver.COLOR
import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.game.play.GameManager
import mu.nu.nullpo.game.play.GameStyle
import mu.nu.nullpo.game.subsystem.mode.Preview
import mu.nu.nullpo.gui.common.BaseFont
import mu.nu.nullpo.gui.common.GameKeyDummy
import mu.nu.nullpo.gui.slick.img.FontNano
import mu.nu.nullpo.gui.slick.img.FontNormal
import mu.nu.nullpo.gui.slick.img.FontTTF
import mu.nu.nullpo.util.CustomProperties
import mu.nu.nullpo.util.GeneralUtil
import mu.nu.nullpo.util.GeneralUtil.getOX
import org.apache.logging.log4j.LogManager
import org.newdawn.slick.AppGameContainer
import org.newdawn.slick.GameContainer
import org.newdawn.slick.Graphics
import org.newdawn.slick.SlickException
import org.newdawn.slick.state.StateBasedGame
import kotlin.random.Random

/** Game Tuning menu state */
class StateConfigGameTuning:BaseGameState() {
	/** Player number */
	var player = 0

	/** Preview flag */
	private var isPreview = false

	/** AppGameContainer (Used by preview) */
	private lateinit var appContainer:AppGameContainer

	/** Game Manager for preview */
	private var gameManager:GameManager? = null

	/** Cursor position */
	private var cursor = 0

	/** A button rotation -1=Auto 0=Always CCW 1=Always CW */
	private var owSpinDirection = 0

	/** Block Skin -1=Auto 0 or above=Fixed */
	private var owSkin = 0

	/** Min/Max DAS -1=Auto 0 or above=Fixed */
	private var owMinDAS = 0
	private var owMaxDAS = 0

	/** DAS Repeat Rate -1=Auto 0 or above=Fixed */
	private var owDASRate = 0

	/** SoftDrop Speed -1=Auto =Fixed Factor above 6=Always x5-20 Speed */
	private var owSDSpd = 0

	/** Reverse the roles of up/down keys in-game */
	private var owReverseUpDown = false

	/** Diagonal move (-1=Auto 0=Disable 1=Enable) */
	private var owMoveDiagonal = 0

	/** Outline type (-1:Auto 0orAbove:Fixed) */
	private var owBlockOutlineType = 0

	/** Show outline only flag (-1:Auto 0:Always Normal 1:Always Outline Only) */
	private var owBlockShowOutlineOnly = 0

	private var sk = 0

	private var spdpv = 0f

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
		owSpinDirection = prop.getProperty("$player.tuning.owRotateButtonDefaultRight", -1)
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
		prop.setProperty("$player.tuning.owRotateButtonDefaultRight", owSpinDirection)
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
		gameManager = GameManager(RendererSlick(this.appContainer.graphics), Preview()).also {
			it.bgMan.bg = -999 // Force no BG

			// Initialization for each player
			it.engine.forEachIndexed {i, e ->
				// Tuning
				e.owSpinDirection = owSpinDirection
				e.owSkin = owSkin
				e.owMinDAS = owMinDAS
				e.owMaxDAS = owMaxDAS
				e.owARR = owDASRate
				e.owSDSpd = owSDSpd
				e.owReverseUpDown = owReverseUpDown
				e.owMoveDiagonal = owMoveDiagonal
				e.owBlockOutlineType = owBlockOutlineType
				e.owBlockShowOutlineOnly = owBlockShowOutlineOnly
				e.comboType = GameEngine.COMBO_TYPE_NORMAL
				e.b2bEnable = true
				e.splitB2B = true
				e.lives = 99
				// Rule
				val ruleOpt:RuleOptions
				val ruleName = NullpoMinoSlick.propGlobal.getProperty(
					if(it.mode?.gameStyle==GameStyle.TETROMINO) "$i.rule" else "$i.rule.${it.mode!!.gameStyle.ordinal}", ""
				)

				if(ruleName.isNotEmpty()) {
					log.info("Load rule options from $ruleName")
					ruleOpt = GeneralUtil.loadRule(ruleName)
				} else {
					log.info("Load rule options from setting file")
					ruleOpt = RuleOptions()
					ruleOpt.readProperty(NullpoMinoSlick.propGlobal, i)
				}
				e.ruleOpt = ruleOpt

				// Randomizer
				if(ruleOpt.strRandomizer.isNotEmpty())
					e.randomizer = GeneralUtil.loadRandomizer(ruleOpt.strRandomizer)

				// Wallkick
				if(ruleOpt.strWallkick.isNotEmpty())
					e.wallkick = GeneralUtil.loadWallkick(ruleOpt.strWallkick)

				// AI
				val aiName = NullpoMinoSlick.propGlobal.getProperty("$i.ai", "")
				if(aiName.isNotEmpty()) {
					e.ai = GeneralUtil.loadAIPlayer(aiName)
					e.aiMoveDelay = NullpoMinoSlick.propGlobal.getProperty("$i.aiMoveDelay", 0)
					e.aiThinkDelay = NullpoMinoSlick.propGlobal.getProperty("$i.aiThinkDelay", 0)
					e.aiUseThread = NullpoMinoSlick.propGlobal.getProperty("$i.aiUseThread", true)
					e.aiShowHint = NullpoMinoSlick.propGlobal.getProperty("$i.aiShowHint", false)
					e.aiPreThink = NullpoMinoSlick.propGlobal.getProperty("$i.aiPreThink", false)
					e.aiShowState = NullpoMinoSlick.propGlobal.getProperty("$i.aiShowState", false)
				}
				it.showInput = NullpoMinoSlick.propConfig.getProperty("option.showInput", false)

				// Init
				e.init()
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

		if(isPreview)
		// Preview
			try {
				g.drawImage(ResourceHolder.imgMenuBG[0], 0f, 0f)
				gameManager?.let {
					it.renderAll()
					val engine = it.engine.first()
					val fontX = when(it.receiver.nextDisplayType) {
						0 -> 16
						1 -> 17
						else -> 18
					}
					FontNormal.printFontGrid(fontX, 14, "PUSH F BUTTON TO EXIT", COLOR.YELLOW)
					val strButtonF = GameKey.getKeyName(player, true, Controller.BUTTON_F)
					FontNormal.printFontGrid(fontX, 15, "(ASSIGNED: ${strButtonF.uppercase()})", COLOR.YELLOW)
					val spd = engine.speed
					val ow = engine.softDropSpd
					FontNano.printFontGrid(fontX, 13, "${spd.gravity}>$ow/${spd.denominator} ${engine.gcount}")
				}
			} catch(e:Exception) {
				log.error("Render fail", e)
			}
		else {
			// Menu
			g.drawImage(ResourceHolder.imgMenuBG[1], 0f, 0f)

			FontNormal.printFontGrid(1, 1, "GAME TUNING (${player+1}P)", COLOR.ORANGE)
			FontNormal.printFontGrid(1, 3+cursor, BaseFont.CURSOR, COLOR.RAINBOW)

			FontNormal.printFontGrid(
				2, 3, "A BUTTON SPIN:${
					when(owSpinDirection) {
						0 -> "LEFT"
						1 -> "RIGHT"
						else -> "AUTO"
					}
				}", cursor==0
			)

			val skinMax = ResourceHolder.imgNormalBlockList.size
			sk = when(owSkin) {
				-1 -> (sk+1)%skinMax
				-2 -> Random.Default.nextInt(skinMax)
				else -> owSkin
			}
			val imgBlock = ResourceHolder.imgNormalBlockList[sk]
			if(ResourceHolder.blockStickyFlagList[sk])
				for(j in 0..8) imgBlock.draw(
					(160+j*16).toFloat(), 64f, (160+j*16+16).toFloat(), (64+16).toFloat(),
					0f, (j*16).toFloat(), 16f, (j*16+16).toFloat()
				)
			else imgBlock.draw(160f, 64f, (160+144).toFloat(), (64+16).toFloat(), 0f, 0f, 144f, 16f)
			FontNormal.printFontGrid(
				2, 4, "SKIN:${"%02d".format(owSkin)}:", cursor==1,
				COLOR.WHITE,
				if(ResourceHolder.blockStickyFlagList[sk]) COLOR.BLUE else COLOR.RED
			)
			FontNormal.printFontGrid(
				19, 4, when(owSkin) {
					-1 -> "AUTO"
					-2 -> "RANDOM"
					else -> NullpoMinoSlick.propSkins.getProperty("Skin$owSkin", "").uppercase()
				}, cursor==1, COLOR.WHITE, if(ResourceHolder.blockStickyFlagList[sk]) COLOR.BLUE else COLOR.RED
			)

			FontNormal.printFontGrid(
				2, 5, "min DAS:"+if(owMinDAS==-1) "AUTO" else "$owMinDAS", cursor==2,
				rainbow = (spdpv/2).toInt()
			)
			FontNormal.printFontGrid(
				2, 6, "max DAS:"+if(owMaxDAS==-1) "AUTO" else "$owMaxDAS", cursor==3,
				rainbow = (spdpv/2).toInt()
			)
			FontNormal.printFontGrid(
				2, 7, "DAS delay:"+if(owDASRate==-1) "AUTO" else "$owDASRate", cursor==4,
				rainbow = (spdpv/2).toInt()
			)
			FontNormal.printFontGrid(
				2, 8, "SoftDrop Speed:"+when {
					owSDSpd==-1 -> "AUTO"
					owSDSpd<SDS_FIXED.size -> "${SDS_FIXED[owSDSpd]}G"
					else -> "*${owSDSpd-SDS_FIXED.size+5}"
				},
				cursor==5, rainbow = (spdpv/4).toInt()
			)
			FontNormal.printFontGrid(2, 9, "Reverse UP/DOWN:"+owReverseUpDown.getOX, cursor==6)


			FontNormal.printFontGrid(
				2, 10, "Diagonal Move:${
					when(owMoveDiagonal) {
						0 -> "\u0085"
						1 -> "\u0083"
						else -> "AUTO"
					}
				}", cursor==7
			)

			FontNormal.printFontGrid(2, 11, "OUTLINE TYPE:"+OUTLINE_TYPE_NAMES[owBlockOutlineType+1], cursor==8)

			FontNormal.printFontGrid(
				2, 12, "SHOW Outline Only:${
					when(owBlockShowOutlineOnly) {
						0 -> "\u0085"
						1 -> "\u0083"
						else -> "AUTO"
					}
				}", cursor==9
			)

			FontNormal.printFontGrid(2, 13, "[PREVIEW]", cursor==10)
			FontNano.printFontGrid(13, 13, "HOTKEY : D BUTTON")
			val strButtonD = GameKey.getKeyName(player, false, Controller.BUTTON_D)
			FontNano.printFontGrid(13, 14, "(ASSIGNED: ${strButtonD.uppercase()})")

			if(cursor>=0&&cursor<UI_TEXT.size)
				FontTTF.print(16, 432, NullpoMinoSlick.getUIText(UI_TEXT[cursor]))
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
				GameKey.gameKey[0].update(container.input, true)

				// Execute game loops
				GameKey.gameKey[0].inputStatusUpdate(gameManager!!.engine[0].ctrl)
				gameManager?.updateAll()

				// Retry button
				if(GameKey.gameKey[0].isMenuRepeatKey(GameKeyDummy.BUTTON_RETRY)) {
					gameManager?.reset()
					gameManager?.bgMan?.bg = -999 // Force no BG
				}

				// Exit
				if(GameKey.gameKey[0].isMenuRepeatKey(GameKeyDummy.BUTTON_F)
					||GameKey.gameKey[0].isMenuRepeatKey(GameKeyDummy.BUTTON_GIVEUP)||
					gameManager?.quitFlag==true)
					stopPreviewGame()
			} catch(e:Exception) {
				log.error("Update fail", e)
			}
		else {
			// Menu screen
			GameKey.gameKey[0].update(container.input, false)

			// TTF font
			if(ResourceHolder.ttfFont!=null) ResourceHolder.ttfFont!!.loadGlyphs()

			// Cursor movement
			if(GameKey.gameKey[0].isMenuRepeatKey(GameKeyDummy.BUTTON_UP)) {
				cursor--
				if(cursor<0) cursor = 10
				ResourceHolder.soundManager.play("cursor")
			}
			if(GameKey.gameKey[0].isMenuRepeatKey(GameKeyDummy.BUTTON_DOWN)) {
				cursor++
				if(cursor>10) cursor = 0
				ResourceHolder.soundManager.play("cursor")
			}

			// Configuration changes
			var change = 0
			if(GameKey.gameKey[0].isMenuRepeatKey(GameKeyDummy.BUTTON_LEFT)) change = -1
			if(GameKey.gameKey[0].isMenuRepeatKey(GameKeyDummy.BUTTON_RIGHT)) change = 1

			if(change!=0) {
				ResourceHolder.soundManager.play("change")

				when(cursor) {
					0 -> {
						owSpinDirection += change
						if(owSpinDirection<-1) owSpinDirection = 1
						if(owSpinDirection>1) owSpinDirection = -1
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
			if(GameKey.gameKey[0].isPushKey(GameKeyDummy.BUTTON_D)) {
				ResourceHolder.soundManager.play("decide")
				startPreviewGame()
				return
			}

			// Confirm button
			if(GameKey.gameKey[0].isPushKey(GameKeyDummy.BUTTON_A)) {

				if(cursor==10) {
					// Preview
					ResourceHolder.soundManager.play("decide")
					startPreviewGame()
					return
				}
				ResourceHolder.soundManager.play("decide2")
				// Save
				saveConfig(NullpoMinoSlick.propGlobal)
				NullpoMinoSlick.saveConfig()
				game.enterState(StateConfigMainMenu.ID)
			}

			// Cancel button
			if(GameKey.gameKey[0].isPushKey(GameKeyDummy.BUTTON_B)) {
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
			listOf(
				"GameTuning_RotateButtonDefaultRight",
				"GameTuning_Skin",
				"GameTuning_MinDAS",
				"GameTuning_MaxDAS",
				"GameTuning_DasDelay",
				"GameTuning_ReverseUpDown",
				"GameTuning_MoveDiagonal",
				"GameTuning_BlockOutlineType",
				"GameTuning_BlockShowOutlineOnly",
				"GameTuning_Preview"
			)

		/** Log */
		internal val log = LogManager.getLogger()

		/** Outline type names */
		private val OUTLINE_TYPE_NAMES = listOf("AUTO", "NONE", "NORMAL", "CONNECT", "SAMECOLOR")
	}
}
