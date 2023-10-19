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
import mu.nu.nullpo.game.subsystem.mode.Preview
import mu.nu.nullpo.gui.common.ConfigGlobal.AIConf
import mu.nu.nullpo.gui.common.ConfigGlobal.TuneConf
import mu.nu.nullpo.gui.common.GameKeyDummy
import mu.nu.nullpo.gui.slick.img.FontNano
import mu.nu.nullpo.gui.slick.img.FontNormal
import mu.nu.nullpo.util.GeneralUtil.getOX
import org.apache.logging.log4j.LogManager
import org.newdawn.slick.AppGameContainer
import org.newdawn.slick.GameContainer
import org.newdawn.slick.Graphics
import org.newdawn.slick.SlickException
import org.newdawn.slick.state.StateBasedGame
import kotlin.random.Random
import mu.nu.nullpo.gui.slick.NullpoMinoSlick.Companion.propGlobal as pGl
import mu.nu.nullpo.util.GeneralUtil as Util

/** Game Tuning menu state */
internal class StateConfigGameTuning:BaseMenuConfigState() {
	/** Player number */
	var player = 0
	override val title:String
		get() = "GAME TUNING (${player+1}P)"

	/** Preview flag */
	private var isPreview = false

	/** AppGameContainer (Used by preview) */
	private lateinit var appContainer:AppGameContainer

	/** Game Manager for preview */
	private var gameManager:GameManager? = null

	private var conf:TuneConf = TuneConf()

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

	/* Called when entering this state */
	override fun enter(container:GameContainer?, game:StateBasedGame?) {
		super.enter(container, game)
		isPreview = false
		conf = pGl.tuning[player]
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
				e.owSpinDir = conf.spinDir
				e.owSkin = conf.skin
				e.owMinDAS = conf.minDAS
				e.owMaxDAS = conf.maxDAS
				e.owARR = conf.owARR
				e.owSDSpd = conf.owSDSpd
				e.owReverseUpDown = conf.reverseUpDown
				e.owMoveDiagonal = conf.moveDiagonal
				e.owBlockOutlineType = conf.blockOutlineType
				e.owBlockShowOutlineOnly = conf.blockShowOutlineOnly
				e.comboType = GameEngine.COMBO_TYPE_NORMAL
				e.b2bEnable = true
				e.splitB2B = true
				e.lives = 99
				// Rule
				val ruleName = pGl.rule[i][it.mode!!.gameStyle.ordinal].path
				val ruleOpt:RuleOptions = if(ruleName.isNotEmpty()) {
					log.info("Load rule options from $ruleName")
					Util.loadRule(ruleName)
				} else {
					RuleOptions()
				}
				e.ruleOpt = ruleOpt

				// Randomizer
				if(ruleOpt.strRandomizer.isNotEmpty())
					e.randomizer = Util.loadRandomizer(ruleOpt.strRandomizer)

				// Wallkick
				if(ruleOpt.strWallkick.isNotEmpty())
					e.wallkick = Util.loadWallkick(ruleOpt.strWallkick)

				// AI
				pGl.ai.getOrElse(i) {AIConf()}.let {ai ->
					if(ai.name.isNotEmpty()) {
						e.ai = Util.loadAIPlayer(ai.name)
						e.aiConf = ai
					}
				}
				it.showInput = NullpoMinoSlick.propConfig.general.showInput

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
					FontNano.printFontGrid(fontX, 13, "${spd.gravity}>$ow/${spd.denominator} ${engine.gCount}")
				}
			} catch(e:Exception) {
				log.error("Render fail", e)
			}
		else {
			// Menu
			super.renderImpl(container, game, g)

			val skinMax = ResourceHolder.imgNormalBlockList.size
			sk = when(conf.skin) {
				-1 -> (sk+1)%skinMax
				-2 -> Random.Default.nextInt(skinMax)
				else -> conf.skin
			}
			val imgBlock = ResourceHolder.imgNormalBlockList[sk]
			if(ResourceHolder.blockStickyFlagList[sk])
				for(j in 0..8) imgBlock.draw(
					(160+j*16).toFloat(), 64f, (160+j*16+16).toFloat(), (64+16).toFloat(),
					0f, (j*16).toFloat(), 16f, (j*16+16).toFloat()
				)
			else imgBlock.draw(160f, 64f, (160+144).toFloat(), (64+16).toFloat(), 0f, 0f, 144f, 16f)

			FontNano.printFontGrid(
				19, 4, when(conf.skin) {
					-1 -> "AUTO"
					-2 -> "RANDOM"
					else -> skinStrs[conf.skin]
				}, cursor==1, COLOR.WHITE, if(ResourceHolder.blockStickyFlagList[sk]) COLOR.BLUE else COLOR.RED
			)
			FontNano.printFontGrid(13, 13, "HOTKEY : D BUTTON")
			val strButtonD = GameKey.getKeyName(player, false, Controller.BUTTON_D)
			FontNano.printFontGrid(13, 14, "(ASSIGNED: ${strButtonD.uppercase()})")
		}
	}

	/* Update game state */
	@Throws(SlickException::class)
	override fun updateImpl(container:GameContainer, game:StateBasedGame, delta:Int) {
		spdpv += when(cursor) {
			2, 3, 4 -> {
				when(cursor) {
					2 -> conf.minDAS
					3 -> conf.maxDAS
					4 -> conf.owARR
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
				conf.owSDSpd.let {
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
		else super.updateImpl(container, game, delta)

	}

	override fun onDecide(container:GameContainer, game:StateBasedGame, delta:Int):Boolean {
		if(cursor==10) {
			// Preview
			ResourceHolder.soundManager.play("decide")
			startPreviewGame()
			return true
		}
		ResourceHolder.soundManager.play("decide2")
		// Save
		if(player !in pGl.tuning.indices) pGl.tuning.add(player, conf)
		else pGl.tuning[player] = conf
		NullpoMinoSlick.saveConfig()
		game.enterState(StateConfigMainMenu.ID)
		return true
	}

	override fun onCancel(container:GameContainer, game:StateBasedGame, delta:Int):Boolean {
		ResourceHolder.soundManager.play("cancel")
		conf = pGl.tuning[player]
		game.enterState(StateConfigMainMenu.ID)
		return true
	}

	override fun onPushButtonD(container:GameContainer, game:StateBasedGame, delta:Int):Boolean {
		// Preview by D button
		ResourceHolder.soundManager.play("decide")
		startPreviewGame()
		return true
	}

	private val skinStrs by lazy {
		ResourceHolder.imgNormalBlockList.indices.map {
			NullpoMinoSlick.propSkins.getProperty("Skin$it", "").uppercase()
		}
	}
	override val columns = listOf(
		"" to listOf(
			Column({
				"A BUTTON SPIN:${
					when(conf.spinDir) {
						0 -> "LEFT"
						1 -> "RIGHT"
						else -> "AUTO"
					}
				}"
			}, {
				conf.spinDir += it
				if(conf.spinDir<-1) conf.spinDir = 1
				if(conf.spinDir>1) conf.spinDir = -1
			}, "GameTuning_RotateButtonDefaultRight"),
			Column({"SKIN:${"%02d".format(conf.skin)}:"}, {
				conf.skin += it
				if(conf.skin<-2) conf.skin = ResourceHolder.imgNormalBlockList.size-1
				if(conf.skin>ResourceHolder.imgNormalBlockList.size-1) conf.skin = -2
			}, "GameTuning_Skin"),
			Column({"min DAS:"+conf.minDAS.let {if(it==-1) "AUTO" else "$it"}}, {
				conf.minDAS += it
				if(conf.minDAS<-1) conf.minDAS = 99
				if(conf.minDAS>99) conf.minDAS = -1
			}, "GameTuning_MinDAS") {(spdpv/2).toInt()},
			Column({"max DAS:"+conf.maxDAS.let {if(it==-1) "AUTO" else "$it"}}, {
				conf.maxDAS += it
				if(conf.maxDAS<-1) conf.maxDAS = 99
				if(conf.maxDAS>99) conf.maxDAS = -1
			}, "GameTuning_MaxDAS") {(spdpv/2).toInt()},
			Column({"DAS delay:"+conf.owARR.let {if(it==-1) "AUTO" else "$it"}}, {
				conf.owARR += it
				if(conf.owARR<-1) conf.owARR = 99
				if(conf.owARR>99) conf.owARR = -1
			}, "GameTuning_DasDelay") {(spdpv/2).toInt()},
			Column({
				"SoftDrop Speed:"+conf.owSDSpd.let {
					when {
						it==-1 -> "AUTO"
						it<SDS_FIXED.size -> "${SDS_FIXED[it]}G"
						else -> "*${it-SDS_FIXED.size+5}"
					}
				}
			}, {
				conf.owSDSpd += it
				if(conf.owSDSpd<-1) conf.owSDSpd = SDS_FIXED.size+15
				if(conf.owSDSpd>SDS_FIXED.size+15) conf.owSDSpd = -1
			}, "GameTuning_SDSpd") {(spdpv/4).toInt()},
			Column({"Reverse UP/DOWN:"+conf.reverseUpDown.getOX}, {
				conf.reverseUpDown = !conf.reverseUpDown
			}, "GameTuning_ReverseUpDown"),
			Column({
				"Diagonal Move:${
					when(conf.moveDiagonal) {
						0 -> "\u0085"
						1 -> "\u0083"
						else -> "AUTO"
					}
				}"
			}, {
				conf.moveDiagonal += it
				if(conf.moveDiagonal<-1) conf.moveDiagonal = 1
				if(conf.moveDiagonal>1) conf.moveDiagonal = -1
			}, "GameTuning_MoveDiagonal"),
			Column({"OUTLINE TYPE:"+OUTLINE_TYPE_NAMES[conf.blockOutlineType+1]}, {
				conf.blockOutlineType += it
				if(conf.blockOutlineType<-1) conf.blockOutlineType = 3
				if(conf.blockOutlineType>3) conf.blockOutlineType = -1
			}, "GameTuning_BlockOutlineType"),
			Column({
				"SHOW Outline Only:${
					when(conf.blockShowOutlineOnly) {
						0 -> "\u0085"
						1 -> "\u0083"
						else -> "AUTO"
					}
				}"
			}, {
				conf.blockShowOutlineOnly += it
				if(conf.blockShowOutlineOnly<-1) conf.blockShowOutlineOnly = 1
				if(conf.blockShowOutlineOnly>1) conf.blockShowOutlineOnly = -1
			}, "GameTuning_BlockShowOutlineOnly"),
			Column({"[PREVIEW]"}, {}, "GameTuning_Preview"),
		)
	)

	companion object {
		/** This state's ID */
		const val ID = 14

		/** Log */
		internal val log = LogManager.getLogger()

		/** Outline type names */
		private val OUTLINE_TYPE_NAMES = listOf("AUTO", "NONE", "NORMAL", "CONNECT", "SAMECOLOR")
	}
}
