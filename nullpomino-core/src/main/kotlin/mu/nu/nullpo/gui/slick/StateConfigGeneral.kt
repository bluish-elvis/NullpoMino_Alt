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

import mu.nu.nullpo.gui.common.BaseFont
import mu.nu.nullpo.gui.common.ConfigGlobal
import mu.nu.nullpo.util.GeneralUtil.getOX
import mu.nu.nullpo.util.GeneralUtil.toInt
import org.apache.logging.log4j.LogManager
import org.newdawn.slick.GameContainer
import org.newdawn.slick.state.StateBasedGame

/** 全般の設定画面のステート */
internal class StateConfigGeneral:BaseMenuConfigState() {
	override val title = "GENERAL OPTIONS"
	override val mouseEnabled = false
	private var confGeneral = ConfigGlobal.GeneralConf()
	private var confVisual = ConfigGlobal.VisualConf()
	private var confRender = ConfigGlobal.RenderConf()
	private var confAudio = ConfigGlobal.AudioConf()

	/** FPS表示 */
	private var showFps
		get() = confGeneral.showFPS
		set(value) {
			confGeneral.showFPS = value
		}
	/** @see mu.nu.nullpo.game.play.GameManager.showInput */
	private var showInput
		get() = confGeneral.showInput
		set(value) {
			confGeneral.showInput = value
		}
	/** frame ステップ is enabled */
	private var enableFrameStep
		get() = confGeneral.enableFrameStep
		set(value) {
			confGeneral.enableFrameStep = value
		}

	/** Sound effects ON/OFF */
	private var se
		get() = confAudio.se
		set(value) {
			confAudio.se = value
		}

	/** BGMのON/OFF */
	private var bgm
		get() = confAudio.bgm
		set(value) {
			confAudio.bgm = value
		}

	/** BGMの事前読み込み */
	private var bgmPreload
		get() = confAudio.bgmPreload
		set(value) {
			confAudio.bgmPreload = value
		}
	/** BGMストリーミングのON/OFF */
	private var bgmStreaming
		get() = confAudio.bgmStreaming
		set(value) {
			confAudio.bgmStreaming = value
		}
	/** @see mu.nu.nullpo.gui.common.AbstractRenderer.showBG */
	private var showBG
		get() = confVisual.showBG
		set(value) {
			confVisual.showBG = value
		}
	/** MaximumFPS */
	private var maxFps
		get() = confRender.maxFPS
		set(value) {
			confRender.maxFPS = value
		}
	private var lineEffectSpeed
		get() = confVisual.lineEffectSpeed
		set(value) {
			confVisual.lineEffectSpeed = value
		}
	/** @see mu.nu.nullpo.gui.common.AbstractRenderer.heavyEffect*/
	private var heavyEffect
		get() = confVisual.heavyEffect
		set(value) {
			confVisual.heavyEffect = value
		}
	/** @see mu.nu.nullpo.gui.common.AbstractRenderer.smoothFall */
	private var smoothFall
		get() = confVisual.smoothFall
		set(value) {
			confVisual.smoothFall = value
		}
	/** @see mu.nu.nullpo.gui.common.AbstractRenderer.showLocus */
	private var showLocus
		get() = confVisual.showLocus
		set(value) {
			confVisual.showLocus = value
		}
	/** @see mu.nu.nullpo.gui.common.AbstractRenderer.showCenter */
	private var showCenter
		get() = confVisual.showCenter
		set(value) {
			confVisual.showCenter = value
		}
	/** @see mu.nu.nullpo.gui.common.AbstractRenderer.fieldBgBright */
	private var fieldBgBright
		get() = confVisual.fieldBgBright
		set(value) {
			confVisual.fieldBgBright = value
		}
	/** @see mu.nu.nullpo.gui.common.AbstractRenderer.edgeBold */
	private var edgeBold
		get() = confVisual.edgeBold
		set(value) {
			confVisual.edgeBold = value
		}
	/** @see mu.nu.nullpo.gui.common.AbstractRenderer.showFieldBgGrid */
	private var showFieldBgGrid
		get() = confVisual.showFieldBgGrid
		set(value) {
			confVisual.showFieldBgGrid = value
		}
	/** @see mu.nu.nullpo.gui.common.AbstractRenderer.darkNextArea */
	private var darkNextArea
		get() = confVisual.darkNextArea
		set(value) {
			confVisual.darkNextArea = value
		}
	/** Sound effects volume */
	private var seVolume
		get() = confAudio.seVolume
		set(value) {
			confAudio.seVolume = value
		}
	/** BGM volume */
	private var bgmVolume
		get() = confAudio.bgmVolume
		set(value) {
			confAudio.bgmVolume = value
		}
	/** @see mu.nu.nullpo.gui.common.AbstractRenderer.showMeter */
	private var showMeter
		get() = confVisual.showMeter
		set(value) {
			confVisual.showMeter = value
		}
	/** @see mu.nu.nullpo.gui.common.AbstractRenderer.nextShadow */
	private var nextShadow
		get() = confVisual.nextShadow
		set(value) {
			confVisual.nextShadow = value
		}
	/** @see mu.nu.nullpo.gui.common.AbstractRenderer.outlineGhost */
	private var outlineGhost
		get() = confVisual.outlineGhost
		set(value) {
			confVisual.outlineGhost = value
		}
	/** @see mu.nu.nullpo.game.event.EventReceiver.nextDisplayType */
	private var nextDisplayType
		get() = confVisual.nextDisplayType
		set(value) {
			confVisual.nextDisplayType = value
		}

	/** フルスクリーン flag */
	private var fullScreen
		get() = confRender.fullScreen
		set(value) {
			confRender.fullScreen = value
		}
	/** 垂直同期を待つ */
	private var vsync
		get() = confRender.vsync
		set(value) {
			confRender.vsync = value
		}
	/** Timing of alternate FPS sleep (false=render true=update) */
	private var alternateFPSTiming
		get() = confRender.alternateFPSTiming
		set(value) {
			confRender.alternateFPSTiming = value
		}
	/** Allow dynamic adjust of target FPS (as seen in Swing version) */
	private var alternateFPSDynamicAdjust
		get() = confRender.alternateFPSDynamicAdjust
		set(value) {
			confRender.alternateFPSDynamicAdjust = value
		}
	/** Perfect FPS mode */
	private var alternateFPSPerfectMode
		get() = confRender.alternateFPSPerfectMode
		set(value) {
			confRender.alternateFPSPerfectMode = value
		}
	/** Execute Thread.yield() during Perfect FPS mode */
	private var alternateFPSPerfectYield
		get() = confRender.alternateFPSPerfectYield
		set(value) {
			confRender.alternateFPSPerfectYield = value
		}

	/** Screen size type */
	private var screenSizeType = 0
		set(value) {
			field = value
			if(screenSizeType in SCREENSIZE_TABLE.indices) {
				SCREENSIZE_TABLE[value].let {(x, y) ->
					confRender.screenWidth = x
					confRender.screenHeight = y
				}
			}
		}

	/* Fetch this state's ID */
	override fun getID() = ID

	/* State initialization */
	override fun init(container:GameContainer, game:StateBasedGame) {
		loadConfig()
	}

	/** Load settings
	 * @param prop Property file to read from
	 */
	private fun loadConfig() {
		NullpoMinoSlick.loadSlickConfig()
		val propConfig = NullpoMinoSlick.propConfig
		confGeneral = propConfig.general
		confVisual = propConfig.visual
		confAudio = propConfig.audio
		confRender = propConfig.render

		screenSizeType =
			SCREENSIZE_TABLE.indexOfFirst {(x, y) ->
				x==confRender.screenWidth&&y==confRender.screenHeight
			}.let {if(it<0) 4 else it} // Default to 640x480

	}

	/** Save settings
	 * @param prop Property file to save to
	 */
	private fun saveConfig() {
		val prop = NullpoMinoSlick.propConfig
		prop.general = confGeneral
		prop.visual = confVisual
		prop.render = confRender
		prop.audio = confAudio
		NullpoMinoSlick.saveConfig()
	}

	override fun onDecide(container:GameContainer, game:StateBasedGame, delta:Int):Boolean {
		ResourceHolder.soundManager.play("decide2")
		saveConfig()
		NullpoMinoSlick.setGeneralConfig()
		if(heavyEffect>0) ResourceHolder.loadLineClearEffectImages()
		if(showBG) ResourceHolder.loadBackgroundImages()
		game.enterState(StateConfigMainMenu.ID)
		return true
	}

	override fun onCancel(container:GameContainer, game:StateBasedGame, delta:Int):Boolean {
		loadConfig()
		game.enterState(StateConfigMainMenu.ID)
		return true
	}

	override val columns:List<Pair<String, List<Column>>> = listOf(
		"APPEARANCE" to listOf(
			Column({"Sound Effects:"+se.getOX}, {se = !se}, "ConfigGeneral_SE"),
			Column({"BGM:"+bgm.getOX}, {bgm = !bgm}, "ConfigGeneral_BGM"),
			Column({"BGM Preload:"+bgmPreload.getOX}, {bgmPreload = !bgmPreload}, "ConfigGeneral_BGMPreload"),
			Column({"SE Volume:$seVolume(${seVolume*100/128}%)"}, {
				seVolume += it
				if(seVolume<0) seVolume = 128
				if(seVolume>128) seVolume = 0
			}, "ConfigGeneral_SEVolume"),
			Column({"BGM Volume:$bgmVolume(${bgmVolume*100/128}%)"}, {
				bgmVolume += it
				if(bgmVolume<0) bgmVolume = 128
				if(bgmVolume>128) bgmVolume = 0
			}, "ConfigGeneral_BGMVolume"),
			Column({"Show Background:"+showBG.getOX}, {showBG = !showBG}, "ConfigGeneral_Background"),
			Column({"Effect Level:$heavyEffect"}, {
				heavyEffect += it
				if(heavyEffect<0) heavyEffect = 3
				if(heavyEffect>3) heavyEffect = 0
			}, "ConfigGeneral_UseHeavyEffect"),
			Column(
				{"Show BG Fields Grid:"+showFieldBgGrid.getOX},
				{showFieldBgGrid = !showFieldBgGrid},
				"ConfigGeneral_ShowFieldBGGrid"
			),
			Column({"Field BG Bright:$fieldBgBright(${fieldBgBright*100/255}%)"}, {
				fieldBgBright += it
				if(fieldBgBright<0) fieldBgBright = 255
				if(fieldBgBright>255) fieldBgBright = 0
			}, "ConfigGeneral_FieldBGBright"),
			Column({"Blocks Edge Thickness:${1+edgeBold.toInt()}"}, {edgeBold = !edgeBold}, "ConfigGeneral_EdgeBold"),
			Column({"Line Effect Speed:${BaseFont.CROSS}"+(lineEffectSpeed+1)}, {
				lineEffectSpeed += it
				if(lineEffectSpeed<0) lineEffectSpeed = 9
				if(lineEffectSpeed>9) lineEffectSpeed = 0
			}, "ConfigGeneral_LineEffectSpeed"),
			Column({"Show Meter:"+showMeter.getOX}, {showMeter = !showMeter}, "ConfigGeneral_ShowMeter"),
			Column({"Dark Next Area:"+darkNextArea.getOX}, {darkNextArea = !darkNextArea}, "ConfigGeneral_DarkNextArea"),
			Column({"Show NextPiece above Shadow :"+nextShadow.getOX}, {nextShadow = !nextShadow}, "ConfigGeneral_NextShadow"),
			Column({"NEXT Layout type:"+NEXTTYPE_OPTIONS[nextDisplayType]}, {
				nextDisplayType += it
				if(nextDisplayType<0) nextDisplayType = 2
				if(nextDisplayType>2) nextDisplayType = 0
			}, "ConfigGeneral_NextType"),
			Column({"Outline Ghost Piece:"+outlineGhost.getOX}, {outlineGhost = !outlineGhost}, "ConfigGeneral_OutlineGhost"),
			Column({"CurrentPiece Smooth fall:"+smoothFall.getOX}, {smoothFall = !smoothFall}, "ConfigGeneral_SmoothFall"),
			Column({"Show CurrentPieces blur:"+showLocus.getOX}, {showLocus = !showLocus}, "ConfigGeneral_ShowLocus"),
			Column({"Show CurrentPieces center:"+showCenter.getOX}, {showCenter = !showCenter}, "ConfigGeneral_ShowLocus"),
			Column({"Show Input:"+showInput.getOX}, {showInput = !showInput}, "ConfigGeneral_ShowInput"),
		), "RENDERING" to listOf(
			Column({"FullScreen:"+fullScreen.getOX}, {fullScreen = !fullScreen}, "ConfigGeneral_Fullscreen"),
			Column({"Show FPS:"+showFps.getOX}, {showFps = !showFps}, "ConfigGeneral_ShowFPS"),
			Column({"MAX FPS:$maxFps"}, {
				maxFps += it
				if(maxFps<0) maxFps = 99
				if(maxFps>99) maxFps = 0
			}, "ConfigGeneral_MaxFPS"),
			Column({"Enable Frame Step:"+enableFrameStep.getOX}, {enableFrameStep = !enableFrameStep}, "ConfigGeneral_FrameStep"),
			Column(
				{"FPS perfect mode:"+alternateFPSPerfectMode.getOX},
				{alternateFPSPerfectMode = !alternateFPSPerfectMode},
				"ConfigGeneral_AlternateFPSPerfectMode"
			),
			Column(
				{"FPS perfect yield:"+alternateFPSPerfectYield.getOX},
				{alternateFPSPerfectYield = !alternateFPSPerfectYield},
				"ConfigGeneral_AlternateFPSPerfectYield"
			),

			Column({"BGM STREAMING:"+bgmStreaming.getOX}, {bgmStreaming = !bgmStreaming}, "ConfigGeneral_BGMStreaming"),
			Column({"VSYNC:"+vsync.getOX}, {vsync = !vsync}, "ConfigGeneral_VSync"),
			Column(
				{"FPS SLEEP TIMING:"+if(alternateFPSTiming) "UPDATE" else "RENDER"},
				{alternateFPSTiming = !alternateFPSTiming},
				"ConfigGeneral_AlternateFPSTiming"
			),
			Column(
				{"FPS DYNAMIC ADJUST:"+alternateFPSDynamicAdjust.getOX},
				{alternateFPSDynamicAdjust = !alternateFPSDynamicAdjust},
				"ConfigGeneral_AlternateFPSDynamicAdjust"
			),
			Column(
				{"SCREEN SIZE:${SCREENSIZE_TABLE[screenSizeType].first}${BaseFont.CROSS}"+SCREENSIZE_TABLE[screenSizeType].second},
				{
					screenSizeType += it
					if(screenSizeType<0) screenSizeType = SCREENSIZE_TABLE.size-1
					if(screenSizeType>SCREENSIZE_TABLE.size-1) screenSizeType = 0
				},
				"ConfigGeneral_ScreenSizeType"
			)
		)
	)

	companion object {
		/** This state's ID */
		const val ID = 6

		/** Piece preview type options */
		private val NEXTTYPE_OPTIONS = listOf("TOP", "SIDE(SMALL)", "SIDE(BIG)")

		/** Screen size table */
		private val SCREENSIZE_TABLE = listOf(
			320 to 240,
			400 to 300,
			480 to 360,
			512 to 384,
			640 to 480,
			800 to 600,
			1024 to 768,
			1152 to 864,
			1280 to 960
		)
		/** Log */
		internal val log = LogManager.getLogger()
	}
}
