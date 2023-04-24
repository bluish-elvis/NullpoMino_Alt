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

	/** Sound effectsON/OFF */
	private var se = false

	/** BGMのON/OFF */
	private var bgm = false

	/** BGMの事前読み込み */
	private var bgmPreload = false

	/** BGMストリーミングのON/OFF */
	private var bgmStreaming = false

	/** Background表示 */
	private var showBg = false

	/** FPS表示 */
	private var showFps = false

	/** frame ステップ is enabled */
	private var enableFrameStep = false

	/** MaximumFPS */
	private var maxFps = 0

	/** Line clearエフェクト表示 */
	private var showLineEffect = false

	/** Line clear effect speed */
	private var lineEffectSpeed = 0

	/** 重い演出を使う */
	private var heavyEffect = false

	/** 操作ブロック降下を滑らかにする */
	private var smoothFall = false

	/** 高速落下時の軌道を表示する */
	private var showLocus = false

	/** fieldBackgroundの明るさ */
	private var fieldBgBright = 0

	/** 縁線を太くする */
	protected var edgeBold = false

	/** Show field BG grid */
	private var showFieldBgGrid = false

	/** NEXT欄を暗くする */
	private var darkNextArea = false

	/** Sound effects volume */
	private var seVolume = 0

	/** BGM volume */
	private var bgmVolume = 0

	/** field右側にMeterを表示 */
	private var showMeter = false

	/** 垂直同期を待つ */
	private var vsync = false

	/** ghost ピースの上にNEXT表示 */
	private var nextShadow = false

	/** 枠線型ghost ピース */
	private var outlineGhost = false

	/** Piece preview type (0=Top 1=Side small 2=Side big) */
	private var nexttype = 0

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

	/** Show player input */
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
		showBg = prop.getProperty("option.showBg", true)
		showFps = prop.getProperty("option.showFps", true)
		enableFrameStep = prop.getProperty("option.enableFrameStep", false)
		maxFps = prop.getProperty("option.maxFps", 60)
		showLineEffect = prop.getProperty("option.showLineEffect", true)
		lineEffectSpeed = prop.getProperty("option.lineEffectSpeed", 0)
		heavyEffect = prop.getProperty("option.heavyEffect", false)
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
		nexttype = 0
		if(prop.getProperty("option.sideNext", false)&&!prop.getProperty("option.bigSideNext", false))
			nexttype = 1
		else if(prop.getProperty("option.sideNext", false)&&prop.getProperty("option.bigSideNext", false))
			nexttype = 2
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
		prop.setProperty("option.showBg", showBg)
		prop.setProperty("option.showFps", showFps)
		prop.setProperty("option.enableFrameStep", enableFrameStep)
		prop.setProperty("option.maxFps", maxFps)
		prop.setProperty("option.showLineEffect", showLineEffect)
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
		when(nexttype) {
			0 -> {
				prop.setProperty("option.sideNext", false)
				prop.setProperty("option.bigSideNext", false)
			}
			1 -> {
				prop.setProperty("option.sideNext", true)
				prop.setProperty("option.bigSideNext", false)
			}
			2 -> {
				prop.setProperty("option.sideNext", true)
				prop.setProperty("option.bigSideNext", true)
			}
		}
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
					(if(cursor<=4) 3 else if(cursor<=12) 4 else if(cursor<=15) 5 else 6)+cursor, "\u0082",
					COLOR.RAINBOW
				)

				FontNormal.printFontGrid(2, 3, "Sound Effects:"+se.getOX, cursor==0)
				FontNormal.printFontGrid(2, 4, "BGM:"+bgm.getOX, cursor==1)
				FontNormal.printFontGrid(2, 5, "BGM Preload:"+bgmPreload.getOX, cursor==2)
				FontNormal.printFontGrid(2, 6, "SE Volume:$seVolume(${seVolume*100/128}%)", cursor==3)
				FontNormal.printFontGrid(2, 7, "BGM Volume:$bgmVolume(${bgmVolume*100/128}%)", cursor==4)

				FontNormal.printFontGrid(2, 9, "Show Background:"+showBg.getOX, cursor==5)
				FontNormal.printFontGrid(2, 10, "Use Explosions:"+heavyEffect.getOX, cursor==6)
				FontNormal.printFontGrid(2, 11, "Show BG Fields Grid:"+showFieldBgGrid.getOX, cursor==7)
				FontNormal.printFontGrid(2, 12, "Field BG Bright:$fieldBgBright(${fieldBgBright*100/255}%)", cursor==8)
				FontNormal.printFontGrid(2, 13, "Blocks Edge Thickness:${1+edgeBold.toInt()}", cursor==9)
				FontNormal.printFontGrid(2, 14, "Show Line Effect:"+showLineEffect.getOX, cursor==10)
				FontNormal.printFontGrid(2, 15, "Line Effect Speed:${BaseFont.CROSS}"+(lineEffectSpeed+1), cursor==11)
				FontNormal.printFontGrid(2, 16, "Show Meter:"+showMeter.getOX, cursor==12)

				FontNormal.printFontGrid(2, 18, "Dark Next Area:"+darkNextArea.getOX, cursor==13)
				FontNormal.printFontGrid(2, 19, "Show NextPiece above Shadow :"+nextShadow.getOX, cursor==14)
				FontNormal.printFontGrid(2, 20, "NEXT Layout type:"+NEXTTYPE_OPTIONS[nexttype], cursor==15)

				FontNormal.printFontGrid(2, 22, "Outline Ghost Piece:"+outlineGhost.getOX, cursor==16)
				FontNormal.printFontGrid(2, 23, "CurrentPiece Smooth fall:"+smoothFall.getOX, cursor==17)
				FontNormal.printFontGrid(2, 24, "Show CurrentPieces blur:"+showLocus.getOX, cursor==18)
				FontNormal.printFontGrid(2, 25, "Show Input:"+showInput.getOX, cursor==19)
			}
			else -> {
				FontNormal.printFontGrid(1, 1, "GENERAL OPTIONS: GRAPHICS (2/2)", COLOR.ORANGE)
				FontNormal.printFontGrid(1, 3+cursor-20+(cursor>25).toInt(), "\u0082", COLOR.RAINBOW)

				FontNormal.printFontGrid(2, 3, "FullScreen:"+fullscreen.getOX, cursor==20)
				FontNormal.printFontGrid(2, 4, "Show FPS:"+showFps.getOX, cursor==21)
				FontNormal.printFontGrid(2, 5, "MAX FPS:$maxFps", cursor==22)
				FontNormal.printFontGrid(2, 6, "Enable Frame Step:"+enableFrameStep.getOX, cursor==23)
				FontNormal.printFontGrid(2, 7, "FPS perfect mode:"+alternateFPSPerfectMode.getOX, cursor==24)
				FontNormal.printFontGrid(2, 8, "FPS perfect yield:"+alternateFPSPerfectYield.getOX, cursor==25)

				FontNormal.printFontGrid(2, 10, "BGM STREAMING:"+bgmStreaming.getOX, cursor==26)
				FontNormal.printFontGrid(2, 11, "VSYNC:"+vsync.getOX, cursor==27)
				FontNormal.printFontGrid(2, 12, "FPS SLEEP TIMING:"+if(alternateFPSTiming) "UPDATE" else "RENDER", cursor==28)
				FontNormal.printFontGrid(2, 13, "FPS DYNAMIC ADJUST:"+alternateFPSDynamicAdjust.getOX, cursor==29)
				FontNormal.printFontGrid(
					2, 14,
					"SCREEN SIZE:${SCREENSIZE_TABLE[screenSizeType][0]}${BaseFont.CROSS}"+SCREENSIZE_TABLE[screenSizeType][1], cursor==30
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
				5 -> showBg = !showBg
				6 -> heavyEffect = !heavyEffect
				7 -> showFieldBgGrid = !showFieldBgGrid
				8 -> {
					fieldBgBright += change
					if(fieldBgBright<0) fieldBgBright = 255
					if(fieldBgBright>255) fieldBgBright = 0
				}
				9 -> edgeBold = !edgeBold
				10 -> showLineEffect = !showLineEffect
				11 -> {
					lineEffectSpeed += change
					if(lineEffectSpeed<0) lineEffectSpeed = 9
					if(lineEffectSpeed>9) lineEffectSpeed = 0
				}
				12 -> showMeter = !showMeter
				13 -> darkNextArea = !darkNextArea
				14 -> nextShadow = !nextShadow
				15 -> {
					nexttype += change
					if(nexttype<0) nexttype = 2
					if(nexttype>2) nexttype = 0
				}
				16 -> outlineGhost = !outlineGhost
				17 -> smoothFall = !smoothFall
				18 -> showLocus = !showLocus
				19 -> showInput = !showInput
				20 -> fullscreen = !fullscreen
				21 -> showFps = !showFps
				22 -> {
					maxFps += change
					if(maxFps<0) maxFps = 99
					if(maxFps>99) maxFps = 0
				}
				23 -> enableFrameStep = !enableFrameStep
				24 -> alternateFPSPerfectMode = !alternateFPSPerfectMode
				25 -> alternateFPSPerfectYield = !alternateFPSPerfectYield
				26 -> bgmStreaming = !bgmStreaming
				27 -> vsync = !vsync
				28 -> alternateFPSTiming = !alternateFPSTiming
				29 -> alternateFPSDynamicAdjust = !alternateFPSDynamicAdjust
				30 -> {
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
			if(showLineEffect) ResourceHolder.loadLineClearEffectImages()
			if(showBg) ResourceHolder.loadBackgroundImages()
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
				"ConfigGeneral_UseBackgroundFade",
				"ConfigGeneral_ShowFieldBGGrid",
				"ConfigGeneral_FieldBGBright",
				"ConfigGeneral_EdgeBold",
				"ConfigGeneral_ShowLineEffect",
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
