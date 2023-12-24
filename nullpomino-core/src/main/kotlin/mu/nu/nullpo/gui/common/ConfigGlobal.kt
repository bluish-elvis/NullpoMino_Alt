/*
 Copyright (c) 2024, NullNoname
 Kotlin converted and modified by Venom=Nhelv.

 THIS WAS NOT MADE IN ASSOCIATION WITH THE GAME CREATOR.
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

package mu.nu.nullpo.gui.common

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import mu.nu.nullpo.game.play.GameStyle
import mu.nu.nullpo.gui.common.GameKeyDummy.Companion.MAX_PLAYERS
import mu.nu.nullpo.gui.net.UpdateChecker.Config as UpdaterConf

@Serializable
data class ConfigGlobal(
	val tuning:MutableList<TuneConf>,
	val rule:MutableList<MutableList<RuleConf>>,
	val ai:MutableList<AIConf>,
	val updateChecker:UpdaterConf,
	var custom:CustomConf = CustomConf(),
	var lastMode:MutableMap<String, String> = mutableMapOf(),
	var lastModeFolder:String = "",
	var lastRule:MutableMap<String, String> = mutableMapOf(),
	var didFirstRun:Boolean = false
) {
	constructor():this(MutableList(MAX_PLAYERS) {TuneConf()},
		MutableList(MAX_PLAYERS) {MutableList(GameStyle.entries.size) {RuleConf()}},
		MutableList(MAX_PLAYERS) {AIConf()}, UpdaterConf()
	)
	@Serializable
	data class VisualConf(
		/** Background display */
		var showBG:Boolean,
		/** 回転・パーティクルなどの重い演出を使う
		 * 1:Line clearエフェクト表示 2:背景のアニメとフェード 3:半透明パーティクル */
		var heavyEffect:Int,
		/** 縁線を太くする */
		var edgeBold:Boolean,
		/** fieldBackgroundの明るさ */
		var fieldBgBright:Int,
		/** Show field BG grid */
		var showFieldBgGrid:Boolean,
		/** Show Meter on right field */
		var showMeter:Boolean,
		/** Show ARE meter */
		var showSpeed:Boolean,
		/** NEXT欄を暗くする */
		var darkNextArea:Boolean,
		/** ghost ピースの上にNEXT表示 */
		var nextShadow:Boolean,
		/** Line clear effect speed */
		var lineEffectSpeed:Int,
		/** Show ghost piece as Outline */
		var outlineGhost:Boolean,
		/** Get type of piece preview
		 * @return 0=Above 1=Side Small 2=Side Big
		 */
		var nextDisplayType:Int,
		/** 操作ブロック降下を滑らかにする */
		var smoothFall:Boolean,
		/** 高速落下時の軌道を表示する */
		var showLocus:Boolean,
		/** 回転軸を表示する */
		var showCenter:Boolean,
	) {
		constructor():this(
			true, 0, false, 128, true, true, true,
			true, false, 0, false, 0, false, false, false
		)
	}

	@Serializable data class GeneralConf(
		var showFPS:Boolean,
		/** Show input */
		var showInput:Boolean,
		/** frame ステップ is enabled */
		var enableFrameStep:Boolean
	) {
		constructor():this(false, false, false)
	}
	@Serializable
	data class TuneConf(
		/** A button rotation -1=Auto 0=Always CCW 1=Always CW */
		var spinDir:Int,
		/** Block Skin -2=random -1=Auto 0 or above=Fixed */
		var skin:Int,
		/** Min/Max DAS -1=Auto 0 or above=Fixed */
		var minDAS:Int,
		/** Min/Max DAS -1=Auto 0 or above=Fixed */
		var maxDAS:Int,
		/** ARE Canceling
		 * (-1:Rule 1:Move 2:Spin 4:Hold)*/
		var delayCancel:Int,
		/** DAS Repeat Rate -1=Auto 0 or above=Fixed */
		var owARR:Int,
		/** SoftDrop Speed -1=Auto =Fixed Factor above 6=Always x5-20 Speed */
		var owSDSpd:Int,
		/** Reverse the roles of up/down keys in-game */
		var reverseUpDown:Boolean,
		/** Diagonal move (-1=Auto 0=Disable 1=Enable) */
		var moveDiagonal:Int,
		/** Outline type (-1:Auto 0orAbove:Fixed) */
		var blockOutlineType:Int,
		/** Show outline only flag (-1:Auto 0:Always Normal 1:Always Outline Only) */
		var blockShowOutlineOnly:Int,

		) {
		constructor():this(
			-1, -1, -1, -1, -1, -1, -1,
			false, -1, -1, -1,
		)
	}

	@Serializable
	data class RuleConf(var path:String = "", var file:String = "", var name:String = "",
		/** Game style */
		@Transient var style:Int = 0)
	@Serializable
	data class AIConf(
		/** AIPlayer: AI for autoplaying */
		var name:String,
		var moveDelay:Int,
		/** AI think delay (Only when using thread) */
		var thinkDelay:Int,
		/** AIをスレッドで処理 */
		var useThread:Boolean,
		/** use AI for hints player */
		var showHint:Boolean,
		/** Pre-think with AI */
		var preThink:Boolean,
		/** Show internal state of AI */
		var showState:Boolean
	) {
		constructor():this("", 0, 0, false, false, false, false)
	}

	@Serializable
	data class GameKeyMaps(
		/** Key code (in game) */
		val keymap:MutableList<MutableList<Int>>,
		/** Key code (in menu) */
		val keymapNav:MutableList<MutableList<Int>>,
		/** Joystick button number */
		val buttonmap:MutableList<MutableList<Int>>,
		/** Joystick direction key border */
		var joyBorder:Int
	) {
		constructor():this(blank, blank, blank, 0)

		companion object {
			private val blank get() = MutableList(GameKeyDummy.MAX_BUTTON) {mutableListOf<Int>()}
		}
	}
	@Serializable
	data class GamePadConf(
		/** 各Playerが使用するJoystick の number (-1:none)*/
		var controllerID:MutableList<Int>,
		/** Joystick direction key が反応する閾値 (一部検出法では使えない) */
		var border:MutableList<Int>,
		/** アナログスティック無視 */
		var ignoreAxis:MutableList<Boolean>,
		/** ハットスイッチ無視 */
		var ignorePOV:MutableList<Boolean>
	) {
		constructor():this(
			MutableList(MAX_PLAYERS) {-1},
			MutableList(MAX_PLAYERS) {0},
			MutableList(MAX_PLAYERS) {false},
			MutableList(MAX_PLAYERS) {false})
	}

	@Serializable
	data class CustomConf(
		var skinDir:String = "res",
		var ssDir:String = "ss",
		var replayDir:String = "replay",
	)
	@Serializable
	data class RenderConf(
		var screenWidth:Int,
		var screenHeight:Int,
		var fullScreen:Boolean = false,
		/** Target FPS */
		var maxFPS:Int = 60,
		/** 垂直同期を待つ */
		var vsync:Boolean = false,
		/** Timing of alternate FPS sleep (false=render true=update) */
		var alternateFPSTiming:Boolean = false,
		/** Allow dynamic adjust of target FPS (as seen in Swing version) */
		var alternateFPSDynamicAdjust:Boolean = true,
		/** Perfect FPS mode (more accurate, eats more CPU) */
		var alternateFPSPerfectMode:Boolean = false,
		/** Execute Thread.yield() during Perfect FPS mode */
		var alternateFPSPerfectYield:Boolean = false,
	) {
		constructor():this(640, 480, false, 60, false, false, true, false, false)

	}
	@Serializable
	data class AudioConf(
		var se:Boolean = true,
		var bgm:Boolean = false,
		var seVolume:Int = 64,
		var bgmVolume:Int = 64,
		var bgmPreload:Boolean = false,
		var bgmStreaming:Boolean = true,
	) {
		constructor():this(true, false, 64, 64, false, true)
	}
	@Serializable
	data class CtrlConf(
		var keymaps:MutableList<GameKeyMaps>,
		var joyMethod:Int = 0,
		val joyPadConf:GamePadConf = GamePadConf(),
	)
}
