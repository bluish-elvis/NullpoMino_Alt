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
	private var bgmpreload = false

	/** BGMストリーミングのON/OFF */
	private var bgmstreaming = false

	/** Background表示 */
	private var showbg = false

	/** FPS表示 */
	private var showfps = false

	/** frame ステップ is enabled */
	private var enableframestep = false

	/** MaximumFPS */
	private var maxfps = 0

	/** Line clearエフェクト表示 */
	private var showlineeffect = false

	/** Line clear effect speed */
	private var lineeffectspeed = 0

	/** 重い演出を使う */
	private var heavyeffect = false

	/** 操作ブロック降下を滑らかにする */
	private var smoothfall = false

	/** 高速落下時の軌道を表示する */
	private var showLocus = false

	/** fieldBackgroundの明るさ */
	private var fieldbgbright = 0

	/** Show field BG grid */
	private var showfieldbggrid = false

	/** NEXT欄を暗くする */
	private var darknextarea = false

	/** Sound effects volume */
	private var sevolume = 0

	/** BGM volume */
	private var bgmvolume = 0

	/** field右側にMeterを表示 */
	private var showmeter = false

	/** 垂直同期を待つ */
	private var vsync = false

	/** ghost ピースの上にNEXT表示 */
	private var nextshadow = false

	/** 枠線型ghost ピース */
	private var outlineghost = false

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
		bgmpreload = prop.getProperty("option.bgmpreload", false)
		bgmstreaming = prop.getProperty("option.bgmstreaming", true)
		showbg = prop.getProperty("option.showbg", true)
		showfps = prop.getProperty("option.showfps", true)
		enableframestep = prop.getProperty("option.enableframestep", false)
		maxfps = prop.getProperty("option.maxfps", 60)
		showlineeffect = prop.getProperty("option.showlineeffect", true)
		lineeffectspeed = prop.getProperty("option.lineeffectspeed", 0)
		heavyeffect = prop.getProperty("option.heavyeffect", false)
		if(prop.getProperty("option.fieldbgbright2")!=null)
			fieldbgbright = prop.getProperty("option.fieldbgbright2", 128)
		else {
			fieldbgbright = prop.getProperty("option.fieldbgbright", 64)*2
			if(fieldbgbright>255) fieldbgbright = 255
		}
		showfieldbggrid = prop.getProperty("option.showfieldbggrid", true)
		darknextarea = prop.getProperty("option.darknextarea", true)
		sevolume = prop.getProperty("option.sevolume", 128)
		bgmvolume = prop.getProperty("option.bgmvolume", 128)
		showmeter = prop.getProperty("option.showmeter", true)
		vsync = prop.getProperty("option.vsync", false)
		nextshadow = prop.getProperty("option.nextshadow", false)
		outlineghost = prop.getProperty("option.outlineghost", false)
		smoothfall = prop.getProperty("option.smoothfall", false)
		showLocus = prop.getProperty("option.showLocus", false)
		showInput = prop.getProperty("option.showInput", false)
		nexttype = 0
		if(prop.getProperty("option.sidenext", false)&&!prop.getProperty("option.bigsidenext", false))
			nexttype = 1
		else if(prop.getProperty("option.sidenext", false)&&prop.getProperty("option.bigsidenext", false))
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
		prop.setProperty("option.bgmpreload", bgmpreload)
		prop.setProperty("option.bgmstreaming", bgmstreaming)
		prop.setProperty("option.showbg", showbg)
		prop.setProperty("option.showfps", showfps)
		prop.setProperty("option.enableframestep", enableframestep)
		prop.setProperty("option.maxfps", maxfps)
		prop.setProperty("option.showlineeffect", showlineeffect)
		prop.setProperty("option.lineeffectspeed", lineeffectspeed)
		prop.setProperty("option.heavyeffect", heavyeffect)
		prop.setProperty("option.fieldbgbright2", fieldbgbright)
		prop.setProperty("option.showfieldbggrid", showfieldbggrid)
		prop.setProperty("option.darknextarea", darknextarea)
		prop.setProperty("option.sevolume", sevolume)
		prop.setProperty("option.bgmvolume", bgmvolume)
		prop.setProperty("option.showmeter", showmeter)
		prop.setProperty("option.vsync", vsync)
		prop.setProperty("option.nextshadow", nextshadow)
		prop.setProperty("option.outlineghost", outlineghost)
		prop.setProperty("option.showInput", showInput)
		prop.setProperty("option.smoothfall", smoothfall)
		prop.setProperty("option.showLocus", showLocus)
		when(nexttype) {
			0 -> {
				prop.setProperty("option.sidenext", false)
				prop.setProperty("option.bigsidenext", false)
			}
			1 -> {
				prop.setProperty("option.sidenext", true)
				prop.setProperty("option.bigsidenext", false)
			}
			2 -> {
				prop.setProperty("option.sidenext", true)
				prop.setProperty("option.bigsidenext", true)
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
		ResourceHolder.imgMenuBG[0].draw()

		// Basic Options
		when {
			cursor<=18 -> {
				FontNormal.printFontGrid(1, 1, "GENERAL OPTIONS: APPERANCE (1/3)", COLOR.ORANGE)
				FontNormal.printFontGrid(
					1,
					(if(cursor<=4) 3 else if(cursor<=8) 4 else if(cursor<=11) 5 else if(cursor<=14) 6 else 7)+cursor, "\u0082",
					COLOR.RAINBOW
				)

				FontNormal.printFontGrid(2, 3, "Sound Effects:"+se.getOX, cursor==0)
				FontNormal.printFontGrid(2, 4, "BGM:"+bgm.getOX, cursor==1)
				FontNormal.printFontGrid(2, 5, "BGM Preload:"+bgmpreload.getOX, cursor==2)
				FontNormal.printFontGrid(2, 6, "SE Volume:$sevolume(${sevolume*100/128}%)", cursor==3)
				FontNormal.printFontGrid(2, 7, "BGM Volume:$bgmvolume(${bgmvolume*100/128}%)", cursor==4)

				FontNormal.printFontGrid(2, 9, "Show Background:"+showbg.getOX, cursor==5)
				FontNormal.printFontGrid(2, 10, "Use Explosions:"+heavyeffect.getOX, cursor==6)
				FontNormal.printFontGrid(2, 11, "Show BG Fields Grid:"+showfieldbggrid.getOX, cursor==7)
				FontNormal.printFontGrid(2, 12, "Field BG Bright:$fieldbgbright(${fieldbgbright*100/255}%)", cursor==8)

				FontNormal.printFontGrid(2, 14, "Show Line Effect:"+showlineeffect.getOX, cursor==9)
				FontNormal.printFontGrid(2, 15, "Line Effect Speed:"+"X "+(lineeffectspeed+1), cursor==10)
				FontNormal.printFontGrid(2, 16, "Show Meter:"+showmeter.getOX, cursor==11)

				FontNormal.printFontGrid(2, 18, "Dark Next Area:"+darknextarea.getOX, cursor==12)
				FontNormal.printFontGrid(2, 19, "Show NextPiece above Shadow :"+nextshadow.getOX, cursor==13)
				FontNormal.printFontGrid(2, 20, "NEXT Layout type:"+NEXTTYPE_OPTIONS[nexttype], cursor==14)

				FontNormal.printFontGrid(2, 22, "Outline Ghost Piece:"+outlineghost.getOX, cursor==15)
				FontNormal.printFontGrid(2, 23, "CurrentPiece Smooth fall:"+smoothfall.getOX, cursor==16)
				FontNormal.printFontGrid(2, 24, "Show CurrentPieces blur:"+showLocus.getOX, cursor==17)
				FontNormal.printFontGrid(2, 25, "Show Input:"+showInput.getOX, cursor==18)

			}
			cursor<=24 -> {
				FontNormal.printFontGrid(1, 1, "GENERAL OPTIONS: GRAPHICS (2/3)", COLOR.ORANGE)
				FontNormal.printFontGrid(1, 3+cursor-19, "\u0082", COLOR.RAINBOW)

				FontNormal.printFontGrid(2, 3, "FullScreen:"+fullscreen.getOX, cursor==19)
				FontNormal.printFontGrid(2, 4, "Show FPS:"+showfps.getOX, cursor==20)
				FontNormal.printFontGrid(2, 5, "MAX FPS:$maxfps", cursor==21)
				FontNormal.printFontGrid(2, 6, "Enable Frame Step:"+enableframestep.getOX, cursor==22)
				FontNormal.printFontGrid(2, 7, "FPS perfect mode:"+alternateFPSPerfectMode.getOX, cursor==23)
				FontNormal.printFontGrid(2, 8, "FPS perfect yield:"+alternateFPSPerfectYield.getOX, cursor==24)
			}
			else -> {
				FontNormal.printFontGrid(1, 1, "GENERAL OPTIONS: SLICK (3/3)", COLOR.ORANGE)
				FontNormal.printFontGrid(1, 3+cursor-25, "\u0082", COLOR.RAINBOW)

				FontNormal.printFontGrid(2, 3, "BGM STREAMING:"+bgmstreaming.getOX, cursor==25)
				FontNormal.printFontGrid(2, 4, "VSYNC:"+vsync.getOX, cursor==26)
				FontNormal.printFontGrid(2, 5, "FPS SLEEP TIMING:"+if(alternateFPSTiming) "UPDATE" else "RENDER", cursor==27)
				FontNormal.printFontGrid(2, 6, "FPS DYNAMIC ADJUST:"+alternateFPSDynamicAdjust.getOX, cursor==28)
				FontNormal.printFontGrid(
					2, 7,
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
		GameKey.gamekey[0].update(container.input)

		// Cursor movement
		if(GameKey.gamekey[0].isMenuRepeatKey(GameKeyDummy.BUTTON_UP)) {
			cursor--
			if(cursor<0) cursor = 29
			ResourceHolder.soundManager.play("cursor")
		}
		if(GameKey.gamekey[0].isMenuRepeatKey(GameKeyDummy.BUTTON_DOWN)) {
			cursor++
			if(cursor>29) cursor = 0
			ResourceHolder.soundManager.play("cursor")
		}

		// Configuration changes
		var change = 0
		if(GameKey.gamekey[0].isMenuRepeatKey(GameKeyDummy.BUTTON_LEFT)) change = -1
		if(GameKey.gamekey[0].isMenuRepeatKey(GameKeyDummy.BUTTON_RIGHT)) change = 1

		if(change!=0) {
			ResourceHolder.soundManager.play("change")

			when(cursor) {
				0 -> se = !se
				1 -> bgm = !bgm
				2 -> bgmpreload = !bgmpreload
				3 -> {
					sevolume += change
					if(sevolume<0) sevolume = 128
					if(sevolume>128) sevolume = 0
				}
				4 -> {
					bgmvolume += change
					if(bgmvolume<0) bgmvolume = 128
					if(bgmvolume>128) bgmvolume = 0
				}
				5 -> showbg = !showbg
				6 -> heavyeffect = !heavyeffect
				7 -> showfieldbggrid = !showfieldbggrid
				8 -> {
					fieldbgbright += change
					if(fieldbgbright<0) fieldbgbright = 255
					if(fieldbgbright>255) fieldbgbright = 0
				}
				9 -> showlineeffect = !showlineeffect
				10 -> {
					lineeffectspeed += change
					if(lineeffectspeed<0) lineeffectspeed = 9
					if(lineeffectspeed>9) lineeffectspeed = 0
				}
				11 -> showmeter = !showmeter
				12 -> darknextarea = !darknextarea
				13 -> nextshadow = !nextshadow
				14 -> {
					nexttype += change
					if(nexttype<0) nexttype = 2
					if(nexttype>2) nexttype = 0
				}
				15 -> outlineghost = !outlineghost
				16 -> smoothfall = !smoothfall
				17 -> showLocus = !showLocus
				18 -> showInput = !showInput
				19 -> fullscreen = !fullscreen
				20 -> showfps = !showfps
				21 -> {
					maxfps += change
					if(maxfps<0) maxfps = 99
					if(maxfps>99) maxfps = 0
				}
				22 -> enableframestep = !enableframestep
				23 -> alternateFPSPerfectMode = !alternateFPSPerfectMode
				24 -> alternateFPSPerfectYield = !alternateFPSPerfectYield
				25 -> bgmstreaming = !bgmstreaming
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
		if(GameKey.gamekey[0].isPushKey(GameKeyDummy.BUTTON_A)) {
			ResourceHolder.soundManager.play("decide2")
			saveConfig(NullpoMinoSlick.propConfig)
			NullpoMinoSlick.saveConfig()
			NullpoMinoSlick.setGeneralConfig()
			if(showlineeffect) ResourceHolder.loadLineClearEffectImages()
			if(showbg) ResourceHolder.loadBackgroundImages()
			game.enterState(StateConfigMainMenu.ID)
		}

		// Cancel button
		if(GameKey.gamekey[0].isPushKey(GameKeyDummy.BUTTON_B)) {
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
				"ConfigGeneral_SE", "ConfigGeneral_BGM", "ConfigGeneral_BGMPreload", "ConfigGeneral_SEVolume",
				"ConfigGeneral_BGMVolume", "ConfigGeneral_Background", "ConfigGeneral_UseBackgroundFade",
				"ConfigGeneral_ShowFieldBGGrid", "ConfigGeneral_FieldBGBright", "ConfigGeneral_ShowLineEffect",
				"ConfigGeneral_LineEffectSpeed", "ConfigGeneral_ShowMeter", "ConfigGeneral_DarkNextArea", "ConfigGeneral_NextShadow",
				"ConfigGeneral_NextType", "ConfigGeneral_OutlineGhost", "ConfigGeneral_SmoothFall", "ConfigGeneral_ShowLocus",
				"ConfigGeneral_ShowInput", "ConfigGeneral_Fullscreen", "ConfigGeneral_ShowFPS", "ConfigGeneral_MaxFPS",
				"ConfigGeneral_FrameStep", "ConfigGeneral_AlternateFPSPerfectMode", "ConfigGeneral_AlternateFPSPerfectYield",
				"ConfigGeneral_BGMStreaming", "ConfigGeneral_VSync", "ConfigGeneral_AlternateFPSTiming",
				"ConfigGeneral_AlternateFPSDynamicAdjust", "ConfigGeneral_ScreenSizeType"
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
