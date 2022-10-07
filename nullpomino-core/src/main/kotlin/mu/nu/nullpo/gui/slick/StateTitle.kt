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

import mu.nu.nullpo.game.component.BGMStatus
import mu.nu.nullpo.game.event.EventReceiver.COLOR
import mu.nu.nullpo.game.play.GameManager
import mu.nu.nullpo.gui.net.UpdateChecker
import mu.nu.nullpo.gui.slick.img.FontMedal
import mu.nu.nullpo.gui.slick.img.FontNano
import mu.nu.nullpo.gui.slick.img.FontTTF
import mu.nu.nullpo.gui.slick.img.RenderStaffRoll
import org.apache.logging.log4j.LogManager
import org.newdawn.slick.AppGameContainer
import org.newdawn.slick.Color
import org.newdawn.slick.GameContainer
import org.newdawn.slick.Graphics
import org.newdawn.slick.state.StateBasedGame

/** Title screen state */
class StateTitle internal constructor():DummyMenuChooseState() {
	/** True when new version is already checked */
	private var isNewVersionChecked = false
	override val numChoice:Int get() = CHOICES.size
	private var rollY = 0f

	init {
		minChoiceY = 20-CHOICES.size
	}

	/* Fetch this state's ID */
	override fun getID():Int = ID

	/* State initialization */
	override fun init(container:GameContainer, game:StateBasedGame) {}

	/* Called when entering this state */
	override fun enter(container:GameContainer?, game:StateBasedGame?) {
		super.enter(container, game)
		// Observer start
		NullpoMinoSlick.startObserverClient()
		// Call GC
		System.gc()

		// Update title bar
		if(container is AppGameContainer) {
			container.setTitle("NullpoMino version${GameManager.versionString}")
			container.setUpdateOnlyWhenVisible(true)
		}

		// New Version check
		if(!isNewVersionChecked&&NullpoMinoSlick.propGlobal.getProperty("updatechecker.enable", true)) {
			isNewVersionChecked = true

			var startupCount = NullpoMinoSlick.propGlobal.getProperty("updatechecker.startupCount", 0)
			val startupMax = NullpoMinoSlick.propGlobal.getProperty("updatechecker.startupMax", 20)

			if(startupCount>=startupMax) {
				val strURL = NullpoMinoSlick.propGlobal.getProperty("updatechecker.url", "")
				UpdateChecker.startCheckForUpdates(strURL)
				startupCount = 0
			} else
				startupCount++

			if(startupMax>=1) {
				NullpoMinoSlick.propGlobal.setProperty("updatechecker.startupCount", startupCount)
				NullpoMinoSlick.saveConfig()
			}
		}
		if(ResourceHolder.bgmPlaying!=BGMStatus.BGM.Menu(0)) ResourceHolder.bgmStart(BGMStatus.BGM.Menu(0))
	}

	override fun updateImpl(container:GameContainer, game:StateBasedGame, delta:Int) {
		super.updateImpl(container, game, delta)
		val mY = container.screenHeight*RenderStaffRoll.img.textureHeight
		rollY += delta/32f
		if(rollY>mY) rollY -= mY
	}

	/* Draw the screen */
	override fun renderImpl(container:GameContainer, game:StateBasedGame, g:Graphics) {
		// Background
		g.drawImage(ResourceHolder.imgTitleBG, 0f, 0f)
		// Menu
		FontNano.printFont(
			0, 0, "NULLPOMINO VERSION ${GameManager.versionString}",
			COLOR.ORANGE, 0.5f
		)
		FontNano.printFont(
			0, 8, "${container.width}*${container.height} SCR:${container.screenWidth}*${container.screenHeight}",
			COLOR.WHITE, 0.5f
		)

		FontMedal.printFont(600, 432, "ALT", 2)
		renderChoices(2, minChoiceY, CHOICES)

		FontTTF.print(16, 432, NullpoMinoSlick.getUIText(UI_TEXT[cursor]))

		FontNano.printFont(300, 0, "$rollY")
		FontNano.printFont(300, 10, "${container.screenHeight*RenderStaffRoll.img.textureHeight}")
		RenderStaffRoll.draw(600-RenderStaffRoll.img.width.toFloat(), 0f, rollY, 480f, Color(200, 233, 255, 200))
		super.renderImpl(container, game, g)

		if(UpdateChecker.isNewVersionAvailable(GameManager.versionMajor, GameManager.versionMinor)) {
			val strTemp = String.format(
				NullpoMinoSlick.getUIText("Title_NewVersion"),
				UpdateChecker.latestVersionFullString, UpdateChecker.strReleaseDate
			)
			FontTTF.print(16, 416, strTemp)
		}
	}

	override fun onDecide(container:GameContainer, game:StateBasedGame, delta:Int):Boolean {
		ResourceHolder.soundManager.play("decide1")

		StateSelectMode.isTopLevel = true
		if(cursor==CHOICEID.size-1) container.exit()
		else game.enterState(CHOICEID[cursor])

		return false
	}

	companion object {
		/** This state's ID */
		const val ID = 1

		/** Strings for menu choices */
		private val CHOICES = listOf("Game Start", "Watch Replay", "Online Game", "Configurations", "Exit")
		private val CHOICEID = listOf(StateSelectMode.ID, StateReplaySelect.ID, StateNetGame.ID, StateConfigMainMenu.ID, -1)

		/** UI Text identifier Strings */
		private val UI_TEXT = listOf("Title_Start", "Title_Replay", "Title_NetPlay", "Title_Config", "Title_Exit")

		/** Log */
		internal var log = LogManager.getLogger()
	}
}
