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

import mu.nu.nullpo.game.event.EventReceiver.COLOR
import mu.nu.nullpo.gui.common.BaseFont
import mu.nu.nullpo.gui.common.GameKeyDummy
import mu.nu.nullpo.gui.slick.img.FontNormal
import mu.nu.nullpo.gui.slick.img.FontTTF
import mu.nu.nullpo.util.CustomProperties
import mu.nu.nullpo.util.GeneralUtil.getOX
import mu.nu.nullpo.util.GeneralUtil.toInt
import org.newdawn.slick.GameContainer
import org.newdawn.slick.Graphics
import org.newdawn.slick.SlickException
import org.newdawn.slick.state.StateBasedGame

/** 全般の設定画面のステート */
class StateConfigGeneral:BaseGameState() {
	/** Cursor position */
	private var cursor = 0

	/** フルスクリーン flag */
	private var fullscreen = false

	/** Sound effects ON/OFF */
	private var se = false

	/** BGMのON/OFF */
	private var bgm = false

	/** BGMの事前読み込み */
	private var bgmPreload = false

	/** BGMストリーミングのON/OFF */
	private var bgmStreaming = false

	/** @see mu.nu.nullpo.game.event.EventReceiver.showBG */
	private var showBG = false

	/** FPS表示 */
	private var showFps = false

	/** frame ステップ is enabled */
	private var enableFrameStep = false

	/** MaximumFPS */
	private var maxFps = 0

	/**  @see mu.nu.nullpo.gui.common.AbstractRenderer.lineEffectSpeed */
	private var lineEffectSpeed = 0

	/** @see mu.nu.nullpo.gui.common.AbstractRenderer.heavyEffect*/
	private var heavyEffect = 0

	/** @see mu.nu.nullpo.gui.common.AbstractRenderer.smoothFall */
	private var smoothFall = false

	/** @see mu.nu.nullpo.gui.common.AbstractRenderer.showLocus */
	private var showLocus = false

	/** @see mu.nu.nullpo.gui.common.AbstractRenderer.fieldBgBright */
	private var fieldBgBright = 0

	/** @see mu.nu.nullpo.gui.common.AbstractRenderer.edgeBold */
	private var edgeBold = false

	/** @see mu.nu.nullpo.gui.common.AbstractRenderer.showFieldBgGrid */
	private var showFieldBgGrid = false

	/** @see mu.nu.nullpo.gui.common.AbstractRenderer.darkNextArea */
	private var darkNextArea = false

	/** Sound effects volume */
	private var seVolume = 0

	/** BGM volume */
	private var bgmVolume = 0

	/** @see mu.nu.nullpo.game.event.EventReceiver.showMeter */
	private var showMeter = false

	/** 垂直同期を待つ */
	private var vsync = false

	/** @see mu.nu.nullpo.gui.common.AbstractRenderer.nextShadow */
	private var nextShadow = false

	/** @see mu.nu.nullpo.game.event.EventReceiver.outlineGhost */
	private var outlineGhost = false

	/** @see mu.nu.nullpo.game.event.EventReceiver.nextDisplayType */
	private var nextDisplayType = 0

	/** Timing of alternate FPS sleep (false=render true=update) */
	private var alternateFPSTiming = false

	/** Allow dynamic adjust of target FPS (as seen in Swing version) */
	private var alternateFPSDynamicAdjust = false

	/** Perfect FPS mode */
	private var alternateFPSPerfectMode = false

	/** Execute Thread.yield() during Perfect FPS mode */
	private var alternateFPSPerfectYield = false

	/** Screen size type */
	private var screenSizeType = 0

	/** @see mu.nu.nullpo.game.play.GameManager.showInput */
	private var showInput = false

	/* Fetch this state's ID */
	override fun getID() = ID

	/* State initialization */
	override fun init(container:GameContainer, game:StateBasedGame) {
		loadConfig(NullpoMinoSlick.propConfig)
	}

	/** Load settings
	 * @param prop Property file to read from
	 */
	private fun loadConfig(prop:CustomProperties) {
		fullscreen = prop.getProperty("option.fullscreen", false)
		se = prop.getProperty("option.se", true)
		bgm = prop.getProperty("option.bgm", false)
		bgmPreload = prop.getProperty("option.bgmPreload", false)
		bgmStreaming = prop.getProperty("option.bgmStreaming", true)
		showBG = prop.getProperty("option.showBG", true)
		showFps = prop.getProperty("option.showFps", true)
		enableFrameStep = prop.getProperty("option.enableFrameStep", false)
		maxFps = prop.getProperty("option.maxFps", 60)
		lineEffectSpeed = prop.getProperty("option.lineEffectSpeed", 0)
		heavyEffect = prop.getProperty("option.heavyEffect", 0)
		edgeBold = NullpoMinoSlick.propConfig.getProperty("option.edgeBold", false)
		fieldBgBright = minOf(255, prop.getProperty("option.fieldBgBright", 128))
		showFieldBgGrid = prop.getProperty("option.showFieldBgGrid", true)
		darkNextArea = prop.getProperty("option.darkNextArea", true)
		seVolume = prop.getProperty("option.seVolume", 128)
		bgmVolume = prop.getProperty("option.bgmVolume", 128)
		showMeter = prop.getProperty("option.showMeter", true)
		vsync = prop.getProperty("option.vsync", false)
		nextShadow = prop.getProperty("option.nextShadow", false)
		outlineGhost = prop.getProperty("option.outlineGhost", false)
		smoothFall = prop.getProperty("option.smoothFall", false)
		showLocus = prop.getProperty("option.showLocus", false)
		showInput = prop.getProperty("option.showInput", false)
		nextDisplayType = prop.getProperty("option.nextDisplayType", 0)
		alternateFPSTiming = prop.getProperty("option.alternateFPSTiming", false)
		alternateFPSDynamicAdjust = prop.getProperty("option.alternateFPSDynamicAdjust", false)
		alternateFPSPerfectMode = prop.getProperty("option.alternateFPSPerfectMode", false)
		alternateFPSPerfectYield = prop.getProperty("option.alternateFPSPerfectYield", false)

		screenSizeType = 5 // Default to 800x600
		val sWidth = prop.getProperty("option.screenwidth", -1)
		val sHeight = prop.getProperty("option.screenheight", -1)
		for(i in SCREENSIZE_TABLE.indices)
			if(sWidth==SCREENSIZE_TABLE[i][0]&&sHeight==SCREENSIZE_TABLE[i][1]) {
				screenSizeType = i
				break
			}
	}

	/** Save settings
	 * @param prop Property file to save to
	 */
	private fun saveConfig(prop:CustomProperties) {
		prop.setProperty("option.fullscreen", fullscreen)
		prop.setProperty("option.se", se)
		prop.setProperty("option.bgm", bgm)
		prop.setProperty("option.bgmPreload", bgmPreload)
		prop.setProperty("option.bgmStreaming", bgmStreaming)
		prop.setProperty("option.showBG", showBG)
		prop.setProperty("option.showFps", showFps)
		prop.setProperty("option.enableFrameStep", enableFrameStep)
		prop.setProperty("option.maxFps", maxFps)
		prop.setProperty("option.lineEffectSpeed", lineEffectSpeed)
		prop.setProperty("option.heavyEffect", heavyEffect)
		prop.setProperty("option.edgeBold", edgeBold)
		prop.setProperty("option.fieldBgBright", fieldBgBright)
		prop.setProperty("option.showFieldBgGrid", showFieldBgGrid)
		prop.setProperty("option.darkNextArea", darkNextArea)
		prop.setProperty("option.seVolume", seVolume)
		prop.setProperty("option.bgmVolume", bgmVolume)
		prop.setProperty("option.showMeter", showMeter)
		prop.setProperty("option.vsync", vsync)
		prop.setProperty("option.nextShadow", nextShadow)
		prop.setProperty("option.outlineGhost", outlineGhost)
		prop.setProperty("option.showInput", showInput)
		prop.setProperty("option.smoothFall", smoothFall)
		prop.setProperty("option.showLocus", showLocus)
		prop.setProperty("option.nextDisplayType", nextDisplayType)
		prop.setProperty("option.alternateFPSTiming", alternateFPSTiming)
		prop.setProperty("option.alternateFPSDynamicAdjust", alternateFPSDynamicAdjust)
		prop.setProperty("option.alternateFPSPerfectMode", alternateFPSPerfectMode)
		prop.setProperty("option.alternateFPSPerfectYield", alternateFPSPerfectYield)

		if(screenSizeType>=0&&screenSizeType<SCREENSIZE_TABLE.size) {
			prop.setProperty("option.screenwidth", SCREENSIZE_TABLE[screenSizeType][0])
			prop.setProperty("option.screenheight", SCREENSIZE_TABLE[screenSizeType][1])
		}
	}

	/* Draw the screen */
	override fun renderImpl(container:GameContainer, game:StateBasedGame, g:Graphics) {
		// Background
		ResourceHolder.imgMenuBG[1].draw()

		// Basic Options
		when {
			cursor<=19 -> {
				FontNormal.printFontGrid(1, 1, "GENERAL OPTIONS: APPERANCE (1/2)", COLOR.ORANGE)
				FontNormal.printFontGrid(
					1,
					(if(cursor<=4) 3 else if(cursor<=12) 4 else if(cursor<=15) 5 else 6)+cursor, BaseFont.CURSOR,
					COLOR.RAINBOW
				)

				FontNormal.printFontGrid(2, 3, "Sound Effects:"+se.getOX, cursor==0)
				FontNormal.printFontGrid(2, 4, "BGM:"+bgm.getOX, cursor==1)
				FontNormal.printFontGrid(2, 5, "BGM Preload:"+bgmPreload.getOX, cursor==2)
				FontNormal.printFontGrid(2, 6, "SE Volume:$seVolume(${seVolume*100/128}%)", cursor==3)
				FontNormal.printFontGrid(2, 7, "BGM Volume:$bgmVolume(${bgmVolume*100/128}%)", cursor==4)

				FontNormal.printFontGrid(2, 9, "Show Background:"+showBG.getOX, cursor==5)
				FontNormal.printFontGrid(2, 10, "Effect Level:$heavyEffect", cursor==6)
				FontNormal.printFontGrid(2, 11, "Show BG Fields Grid:"+showFieldBgGrid.getOX, cursor==7)
				FontNormal.printFontGrid(2, 12, "Field BG Bright:$fieldBgBright(${fieldBgBright*100/255}%)", cursor==8)
				FontNormal.printFontGrid(2, 13, "Blocks Edge Thickness:${1+edgeBold.toInt()}", cursor==9)
				FontNormal.printFontGrid(2, 15, "Line Effect Speed:${BaseFont.CROSS}"+(lineEffectSpeed+1), cursor==10)
				FontNormal.printFontGrid(2, 16, "Show Meter:"+showMeter.getOX, cursor==11)

				FontNormal.printFontGrid(2, 18, "Dark Next Area:"+darkNextArea.getOX, cursor==12)
				FontNormal.printFontGrid(2, 19, "Show NextPiece above Shadow :"+nextShadow.getOX, cursor==13)
				FontNormal.printFontGrid(2, 20, "NEXT Layout type:"+NEXTTYPE_OPTIONS[nextDisplayType], cursor==14)

				FontNormal.printFontGrid(2, 22, "Outline Ghost Piece:"+outlineGhost.getOX, cursor==15)
				FontNormal.printFontGrid(2, 23, "CurrentPiece Smooth fall:"+smoothFall.getOX, cursor==16)
				FontNormal.printFontGrid(2, 24, "Show CurrentPieces blur:"+showLocus.getOX, cursor==17)
				FontNormal.printFontGrid(2, 25, "Show Input:"+showInput.getOX, cursor==18)
			}
			else -> {
				FontNormal.printFontGrid(1, 1, "GENERAL OPTIONS: GRAPHICS (2/2)", COLOR.ORANGE)
				FontNormal.printFontGrid(1, 3+cursor-20+(cursor>25).toInt(), BaseFont.CURSOR, COLOR.RAINBOW)

				FontNormal.printFontGrid(2, 3, "FullScreen:"+fullscreen.getOX, cursor==19)
				FontNormal.printFontGrid(2, 4, "Show FPS:"+showFps.getOX, cursor==20)
				FontNormal.printFontGrid(2, 5, "MAX FPS:$maxFps", cursor==21)
				FontNormal.printFontGrid(2, 6, "Enable Frame Step:"+enableFrameStep.getOX, cursor==22)
				FontNormal.printFontGrid(2, 7, "FPS perfect mode:"+alternateFPSPerfectMode.getOX, cursor==23)
				FontNormal.printFontGrid(2, 8, "FPS perfect yield:"+alternateFPSPerfectYield.getOX, cursor==24)

				FontNormal.printFontGrid(2, 10, "BGM STREAMING:"+bgmStreaming.getOX, cursor==25)
				FontNormal.printFontGrid(2, 11, "VSYNC:"+vsync.getOX, cursor==26)
				FontNormal.printFontGrid(2, 12, "FPS SLEEP TIMING:"+if(alternateFPSTiming) "UPDATE" else "RENDER", cursor==27)
				FontNormal.printFontGrid(2, 13, "FPS DYNAMIC ADJUST:"+alternateFPSDynamicAdjust.getOX, cursor==28)
				FontNormal.printFontGrid(
					2, 14,
					"SCREEN SIZE:${SCREENSIZE_TABLE[screenSizeType][0]}${BaseFont.CROSS}"+SCREENSIZE_TABLE[screenSizeType][1], cursor==29
				)
			}
		}// Slick Options
		// Advanced Options

		if(cursor>=0&&cursor<UI_TEXT.size) FontTTF.print(16, 432, NullpoMinoSlick.getUIText(UI_TEXT[cursor]))
	}

	/* Update game state */
	@Throws(SlickException::class)
	override fun updateImpl(container:GameContainer, game:StateBasedGame, delta:Int) {
		// TTF font
		if(ResourceHolder.ttfFont!=null) ResourceHolder.ttfFont!!.loadGlyphs()

		// Update key input states
		GameKey.gameKey[0].update(container.input)

		// Cursor movement
		if(GameKey.gameKey[0].isMenuRepeatKey(GameKeyDummy.BUTTON_UP)) {
			cursor--
			if(cursor<0) cursor = 30
			ResourceHolder.soundManager.play("cursor")
		}
		if(GameKey.gameKey[0].isMenuRepeatKey(GameKeyDummy.BUTTON_DOWN)) {
			cursor++
			if(cursor>30) cursor = 0
			ResourceHolder.soundManager.play("cursor")
		}

		// Configuration changes
		var change = 0
		if(GameKey.gameKey[0].isMenuRepeatKey(GameKeyDummy.BUTTON_LEFT)) change = -1
		if(GameKey.gameKey[0].isMenuRepeatKey(GameKeyDummy.BUTTON_RIGHT)) change = 1

		if(change!=0) {
			ResourceHolder.soundManager.play("change")

			when(cursor) {
				0 -> se = !se
				1 -> bgm = !bgm
				2 -> bgmPreload = !bgmPreload
				3 -> {
					seVolume += change
					if(seVolume<0) seVolume = 128
					if(seVolume>128) seVolume = 0
				}
				4 -> {
					bgmVolume += change
					if(bgmVolume<0) bgmVolume = 128
					if(bgmVolume>128) bgmVolume = 0
				}
				5 -> showBG = !showBG
				6 -> {
					heavyEffect += change
					if(heavyEffect<0) heavyEffect = 3
					if(heavyEffect>3) heavyEffect = 0
				}
				7 -> showFieldBgGrid = !showFieldBgGrid
				8 -> {
					fieldBgBright += change
					if(fieldBgBright<0) fieldBgBright = 255
					if(fieldBgBright>255) fieldBgBright = 0
				}
				9 -> edgeBold = !edgeBold
				10 -> {
					lineEffectSpeed += change
					if(lineEffectSpeed<0) lineEffectSpeed = 9
					if(lineEffectSpeed>9) lineEffectSpeed = 0
				}
				11 -> showMeter = !showMeter
				12 -> darkNextArea = !darkNextArea
				13 -> nextShadow = !nextShadow
				14 -> {
					nextDisplayType += change
					if(nextDisplayType<0) nextDisplayType = 2
					if(nextDisplayType>2) nextDisplayType = 0
				}
				15 -> outlineGhost = !outlineGhost
				16 -> smoothFall = !smoothFall
				17 -> showLocus = !showLocus
				18 -> showInput = !showInput
				19 -> fullscreen = !fullscreen
				20 -> showFps = !showFps
				21 -> {
					maxFps += change
					if(maxFps<0) maxFps = 99
					if(maxFps>99) maxFps = 0
				}
				22 -> enableFrameStep = !enableFrameStep
				23 -> alternateFPSPerfectMode = !alternateFPSPerfectMode
				24 -> alternateFPSPerfectYield = !alternateFPSPerfectYield
				25 -> bgmStreaming = !bgmStreaming
				26 -> vsync = !vsync
				27 -> alternateFPSTiming = !alternateFPSTiming
				28 -> alternateFPSDynamicAdjust = !alternateFPSDynamicAdjust
				29 -> {
					screenSizeType += change
					if(screenSizeType<0) screenSizeType = SCREENSIZE_TABLE.size-1
					if(screenSizeType>SCREENSIZE_TABLE.size-1) screenSizeType = 0
				}
			}
		}

		// Confirm button
		if(GameKey.gameKey[0].isPushKey(GameKeyDummy.BUTTON_A)) {
			ResourceHolder.soundManager.play("decide2")
			saveConfig(NullpoMinoSlick.propConfig)
			NullpoMinoSlick.saveConfig()
			NullpoMinoSlick.setGeneralConfig()
			if(heavyEffect>0) ResourceHolder.loadLineClearEffectImages()
			if(showBG) ResourceHolder.loadBackgroundImages()
			game.enterState(StateConfigMainMenu.ID)
		}

		// Cancel button
		if(GameKey.gameKey[0].isPushKey(GameKeyDummy.BUTTON_B)) {
			loadConfig(NullpoMinoSlick.propConfig)
			game.enterState(StateConfigMainMenu.ID)
		}
	}

	companion object {
		/** This state's ID */
		const val ID = 6

		/** UI Text identifier Strings */
		private val UI_TEXT =
			listOf(
				"ConfigGeneral_SE",
				"ConfigGeneral_BGM",
				"ConfigGeneral_BGMPreload",
				"ConfigGeneral_SEVolume",
				"ConfigGeneral_BGMVolume",
				"ConfigGeneral_Background",
				"ConfigGeneral_UseHeavyEffect",
				"ConfigGeneral_ShowFieldBGGrid",
				"ConfigGeneral_FieldBGBright",
				"ConfigGeneral_EdgeBold",
				"ConfigGeneral_LineEffectSpeed",
				"ConfigGeneral_ShowMeter",
				"ConfigGeneral_DarkNextArea",
				"ConfigGeneral_NextShadow",
				"ConfigGeneral_NextType",
				"ConfigGeneral_OutlineGhost",
				"ConfigGeneral_SmoothFall",
				"ConfigGeneral_ShowLocus",
				"ConfigGeneral_ShowInput",
				"ConfigGeneral_Fullscreen",
				"ConfigGeneral_ShowFPS",
				"ConfigGeneral_MaxFPS",
				"ConfigGeneral_FrameStep",
				"ConfigGeneral_AlternateFPSPerfectMode",
				"ConfigGeneral_AlternateFPSPerfectYield",
				"ConfigGeneral_BGMStreaming",
				"ConfigGeneral_VSync",
				"ConfigGeneral_AlternateFPSTiming",
				"ConfigGeneral_AlternateFPSDynamicAdjust",
				"ConfigGeneral_ScreenSizeType"
			)

		/** Piece preview type options */
		private val NEXTTYPE_OPTIONS = listOf("TOP", "SIDE(SMALL)", "SIDE(BIG)")

		/** Screen size table */
		private val SCREENSIZE_TABLE =
			listOf(
				listOf(320, 240), listOf(400, 300), listOf(480, 360), listOf(512, 384), listOf(640, 480),
				listOf(800, 600), listOf(1024, 768), listOf(1152, 864), listOf(1280, 960)
			)
	}
}
