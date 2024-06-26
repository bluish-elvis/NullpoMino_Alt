/*
 Copyright (c) 2010-2024, NullNoname
 All rights reserved.

 Converted to Kotlin and modified by Venom_Nhelv as bluish-elvis
 THIS IS NOT MADE IN ASSOCIATION WITH THE GAME CREATOR.

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
package mu.nu.nullpo.gui.slick

import kotlinx.serialization.encodeToString
import mu.nu.nullpo.util.GeneralUtil.Json
import mu.nu.nullpo.game.event.EventReceiver.COLOR
import mu.nu.nullpo.game.net.NetObserverClient
import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.gui.common.ConfigGlobal
import mu.nu.nullpo.gui.slick.img.FontNano
import mu.nu.nullpo.util.CustomProperties
import mu.nu.nullpo.util.ModeManager
import org.apache.logging.log4j.LogManager
import org.lwjgl.opengl.Display
import org.newdawn.slick.AppGameContainer
import org.newdawn.slick.GameContainer
import org.newdawn.slick.Graphics
import org.newdawn.slick.Image
import org.newdawn.slick.ScalableGame
import org.newdawn.slick.SlickException
import org.newdawn.slick.state.StateBasedGame
import org.newdawn.slick.util.Log
import java.awt.image.BufferedImage
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.swing.JOptionPane
import kotlin.system.exitProcess

/** NullpoMino SlickVersion */
internal class NullpoMinoSlick:StateBasedGame("NullpoMino (Now Loading...)") {
	/* ステート (タイトルとかゲームとかのシーンのことね）を追加 */
	override fun initStatesList(container:GameContainer) {
		stateLoading = StateLoading()
		stateTitle = StateTitle()
		stateInGame = StateInGame()
		stateSelectMode = StateSelectMode()
		stateReplaySelect = StateReplaySelect()
		stateConfigMainMenu = StateConfigMainMenu()
		stateConfigGeneral = StateConfigGeneral()
		stateConfigRuleSelect = StateConfigRuleSelect()
		stateConfigAISelect = StateConfigAISelect()
		stateConfigKeyboard = StateConfigKeyboard()
		stateConfigJoystickButton = StateConfigJoystickButton()
		stateNetGame = StateNetGame()
		stateConfigJoystickMain = StateConfigJoystickMain()
		stateConfigJoystickTest = StateConfigJoystickTest()
		stateConfigGameTuning = StateConfigGameTuning()
		stateConfigRuleStyleSelect = StateConfigRuleStyleSelect()
		stateConfigKeyboardNavi = StateConfigKeyboardNavi()
		stateConfigKeyboardReset = StateConfigKeyboardReset()
		stateSelectRuleFromList = StateSelectRuleFromList()
		stateSelectModeFolder = StateSelectModeFolder()

		addState(stateLoading)
		addState(stateTitle)
		addState(stateInGame)
		addState(stateSelectMode)
		addState(stateReplaySelect)
		addState(stateConfigMainMenu)
		addState(stateConfigGeneral)
		addState(stateConfigRuleSelect)
		addState(stateConfigAISelect)
		addState(stateConfigKeyboard)
		addState(stateConfigJoystickButton)
		addState(stateNetGame)
		addState(stateConfigJoystickMain)
		addState(stateConfigJoystickTest)
		addState(stateConfigGameTuning)
		addState(stateConfigRuleStyleSelect)
		addState(stateConfigKeyboardNavi)
		addState(stateConfigKeyboardReset)
		addState(stateSelectRuleFromList)
		addState(stateSelectModeFolder)
	}

	companion object {
		/** Log */
		internal val log = LogManager.getLogger()

		/** Save settings用Property file */
		var propConfig = ConfigSlick(); private set
		/** Save settings用Property file (全Version共通) */
		var propGlobal = ConfigGlobal(); private set
		/** 音楽リストProperty file */
		internal val propMusic = CustomProperties()
		/** Observer機能用Property file */
		private val propObserver = CustomProperties()
		/** Default language file */
		private val propLangDefault = CustomProperties()
		/** 言語ファイル */
		private val propLang = CustomProperties()
		/** Default game mode description file */
		internal val propDefaultModeDesc = CustomProperties()
		/** Game mode description file */
		internal val propModeDesc = CustomProperties()
		/** Skin description file */
		internal val propSkins = CustomProperties()

		/** Mode 管理 */
		internal val modeManager:ModeManager = ModeManager()

		/** AppGameContainer */
		internal lateinit var appGameContainer:AppGameContainer
		/** ロード画面のステート */
		internal lateinit var stateLoading:StateLoading
		/** タイトル画面のステート */
		internal lateinit var stateTitle:StateTitle
		/** ゲーム画面のステート */
		internal lateinit var stateInGame:StateInGame
		/** Mode 選択画面のステート */
		internal lateinit var stateSelectMode:StateSelectMode
		/** リプレイ選択画面のステート */
		internal lateinit var stateReplaySelect:StateReplaySelect
		/** 設定画面のステート */
		internal lateinit var stateConfigMainMenu:StateConfigMainMenu
		/** 全般の設定画面のステート */
		internal lateinit var stateConfigGeneral:StateConfigGeneral
		/** ルール選択画面のステート */
		internal lateinit var stateConfigRuleSelect:StateConfigRuleSelect
		/** AI選択画面のステート */
		internal lateinit var stateConfigAISelect:StateConfigAISelect
		/** キーボード設定画面のステート */
		internal lateinit var stateConfigKeyboard:StateConfigKeyboard
		/** Joystick button設定画面のステート */
		internal lateinit var stateConfigJoystickButton:StateConfigJoystickButton
		/** ネットプレイ画面のステート */
		private lateinit var stateNetGame:StateNetGame
		/** Joystick 設定メインMenu のステート */
		internal lateinit var stateConfigJoystickMain:StateConfigJoystickMain
		/** Joystick テスト画面のステート */
		internal lateinit var stateConfigJoystickTest:StateConfigJoystickTest
		/** チューニング設定画面のステート */
		internal lateinit var stateConfigGameTuning:StateConfigGameTuning
		/** Style select state */
		internal lateinit var stateConfigRuleStyleSelect:StateConfigRuleStyleSelect
		/** Keyboard menu navigation settings state */
		internal lateinit var stateConfigKeyboardNavi:StateConfigKeyboardNavi
		/** Keyboard Reset menu state */
		internal lateinit var stateConfigKeyboardReset:StateConfigKeyboardReset
		/** Rule select (after mode selection) */
		internal lateinit var stateSelectRuleFromList:StateSelectRuleFromList
		/** Mode folder select */
		internal lateinit var stateSelectModeFolder:StateSelectModeFolder

		/** Timing of alternate FPS sleep (false=render true=update) */
		internal var alternateFPSTiming = false
		/** Allow dynamic adjust of target FPS (as seen in Swing version) */
		private var alternateFPSDynamicAdjust = false
		/** Perfect FPS mode (more accurate, eats more CPU) */
		private var alternateFPSPerfectMode = false
		/** Execute Thread.yield() during Perfect FPS mode */
		private var alternateFPSPerfectYield = false
		/** Target FPS */
		internal var altMaxFPS = 0
		/** Current max FPS */
		private var altMaxFPSCurrent = 0
		/** Used for FPS calculation */
		private var periodCurrent:Long = 0L
		/** FPS維持用 */
		private var beforeTime = 0L
		/** FPS維持用 */
		private var overSleepTime = 0L
		/** FPS維持用 */
		private var noDelays = 0
		/** FPS計算用 */
		private var calcInterval = 0L
		/** FPS計算用 */
		private var prevCalcTime = 0L
		/** frame count */
		var frameCount = 0L; private set
		/** upTime by frame */
		var upTimeFrame = 0L; private set
		/** rainbow 9 counter */
		val rainbow get() = (upTimeFrame%20).toInt()/2
		/** 実際のFPS */
		private var actualFPS = .0
		/** FPS表示用DecimalFormat */
		private val df = DecimalFormat("0.0")
		/** Used by perfect fps mode */
		private var perfectFPSDelay = 0L

		/** Observerクライアント */
		private var netObserverClient:NetObserverClient? = null

		/** True if read keyboard input from JInput */
		internal var useJInputKeyboard = false
		/** True to use safer texture loading
		 * (Use BigImage instead of regular Image) */
		internal var useBigImageTextureLoad = false

		/** メイン関数
		 * @param args プログラムに渡されたコマンドLines引数
		 */
		@JvmStatic
		fun main(args:Array<String>) {
			org.apache.logging.log4j.core.config.Configurator.initialize(log.name, "config/etc/log.xml")
			Log.setLogSystem(SlickLog4j())
			log.info("NullpoMinoSlick Start")
			log.info(NullpoMinoSlick::class.java.getResource("/log4j2.xml")?.path ?: "")
			// 設定ファイル読み込み
			loadGlobalConfig()
			loadSlickConfig()

			try {
				val fin = FileInputStream("config/setting/music.xml")
				propMusic.loadFromXML(fin)
				fin.close()
			} catch(_:IOException) {
			}

			// 言語ファイル読み込み
			try {
				val fin = FileInputStream("config/lang/slick_default.xml")
				propLangDefault.loadFromXML(fin)
				fin.close()
			} catch(e:IOException) {
				log.error("Couldn't load default UI language file", e)
			}

			try {
				val fin = FileInputStream("config/lang/slick_${Locale.getDefault().country}.xml")
				propLang.loadFromXML(fin)
				fin.close()

				val out = FileOutputStream("config/lang/slick_${Locale.getDefault().country}.xml")
				propLang.storeToXML(out, "Slick language file - ${Locale.getDefault().displayCountry}")
				out.close()
			} catch(_:IOException) {
			}

			// Game mode description
			try {
				val fis = FileInputStream("config/lang/modedesc_default.xml")
				propDefaultModeDesc.loadFromXML(fis)
			} catch(e:IOException) {
				log.error("Couldn't load default mode description file", e)
			}

			try {
				val fis = FileInputStream("config/lang/modedesc_${Locale.getDefault().country}.xml")
				propModeDesc.loadFromXML(fis)
				fis.close()
			} catch(_:IOException) {
			}

			// 設定ファイル読み込み
			try {
				val fis = FileInputStream("config/lang/blockskin.xml")
				propSkins.loadFromXML(fis)
				fis.close()
			} catch(_:IOException) {
			}

			// Mode読み込み
			try {
				this::class.java.getResource("/mode.lst")?.file?.let {f ->
					FileInputStream(f).bufferedReader().use {
						modeManager.loadGameModes(it)
					}
				}
			} catch(e:IOException) {
				log.error("Mode list load failed", e)
			}

			// Set default rule selections
			try {
				val propDefaultRule = CustomProperties()
				val fis = FileInputStream("config/list/global_defaultrule.lst")
				propDefaultRule.load(fis)
				fis.close()

				for(pl in 0..1) {
					if(propGlobal.rule.getOrNull(pl).isNullOrEmpty())
						propGlobal.rule[pl] = mutableListOf()
					for(i in 0..<GameEngine.MAX_GAMESTYLE) {
						val pos = if(i==0) "" else ".$i"
						if(propGlobal.rule.getOrNull(pl)?.getOrNull(i)?.path.isNullOrEmpty()) {
							propGlobal.rule[pl][i] = ConfigGlobal.RuleConf(
								propDefaultRule.getProperty("default.rule$pos", ""),
								propDefaultRule.getProperty("default.rulefile$pos", ""),
								propDefaultRule.getProperty("default.rulename$pos", "")
							)
						}
					}
				}

			} catch(_:Exception) {
			}

			// Command line options
			useJInputKeyboard = false
			useBigImageTextureLoad = false

			for(str in args) {
				if(str=="-j"||str=="/j") {
					useJInputKeyboard = true
					log.info("-j option is used. Use JInput to read keyboard input.")
				} else if(str=="-b"||str=="/b") {
					useBigImageTextureLoad = true
					log.info("-b option is used. Use BigImage instead of normal Image.")
				}
			}

			perfectFPSDelay = System.nanoTime()

			// Get driver name and version
			var strDriverName:String?
			var strDriverVersion:String?
			try {
				strDriverName = Display.getAdapter()
				strDriverVersion = Display.getVersion()
				log.info("Driver adapter:$strDriverName, Driver version:$strDriverVersion")
			} catch(e:Throwable) {
				log.fatal("LWJGL load failed", e)

				// LWJGL Load failed! Do the file of LWJGL exist?
				val fileLWJGL:File = when {
					System.getProperty("os.name").contains("Windows") -> if(System.getProperty("os.arch").contains("64"))
						File("lib/lwjgl64.dll") else File("lib/lwjgl.dll")
					System.getProperty("os.name").contains("Mac OS") -> File("lib/liblwjgl.jnilib")
					else -> if(System.getProperty("os.arch").contains("64")) File("lib/liblwjgl64.so")
					else File("lib/liblwjgl.so")
				}

				if(fileLWJGL.isFile&&fileLWJGL.canRead()) {
					// File exists but incompatible with your OS
					val strErrorTitle = getUIText("LWJGLLoadFailedMessage_Title")
					val strErrorMessage = String.format(getUIText("LWJGLLoadFailedMessage_Body"), "$e")
					JOptionPane.showMessageDialog(null, strErrorMessage, strErrorTitle, JOptionPane.ERROR_MESSAGE)
				} else {
					// Not found
					val strErrorTitle = getUIText("LWJGLNotFoundMessage_Title")
					val strErrorMessage = String.format(getUIText("LWJGLNotFoundMessage_Body"), "$e")
					JOptionPane.showMessageDialog(null, strErrorMessage, strErrorTitle, JOptionPane.ERROR_MESSAGE)
				}

				// Exit
				exitProcess(-3)
			}

			if(strDriverName==null) strDriverName = "(Unknown)"
			if(strDriverVersion==null) strDriverVersion = "(Unknown)"

			// ゲーム画面などの初期化
			try {
				val sWidth = propConfig.render.screenWidth
				val sHeight = propConfig.render.screenHeight

				val obj = NullpoMinoSlick()

				appGameContainer = if(sWidth!=640||sHeight!=480) {
					val sObj = ScalableGame(obj, 640, 480, true)
					AppGameContainer(sObj)
				} else
					AppGameContainer(obj)
				appGameContainer.setShowFPS(false)
				appGameContainer.setClearEachFrame(false)
				appGameContainer.setMinimumLogicUpdateInterval(0)
				appGameContainer.setMaximumLogicUpdateInterval(0)
				appGameContainer.setUpdateOnlyWhenVisible(false)
				appGameContainer.setForceExit(false)
				appGameContainer.setDisplayMode(sWidth, sHeight, propConfig.render.fullScreen)
				appGameContainer.start()
			} catch(e:SlickException) {
				log.fatal("Game initialize failed (SlickException)", e)

				// Display an error dialog
				val strErrorTitle = getUIText("InitFailedMessage_Title")
				val strErrorMessage = String.format(getUIText("InitFailedMessage_Body"), strDriverName, strDriverVersion, "$e")
				JOptionPane.showMessageDialog(null, strErrorMessage, strErrorTitle, JOptionPane.ERROR_MESSAGE)

				// Exit
				exitProcess(-1)
			} catch(e:Throwable) {
				log.fatal("Game initialize failed (NON-SlickException)", e)

				// Display an error dialog
				val strErrorTitle = getUIText("InitFailedMessageGeneral_Title")
				val strErrorMessage = String.format(getUIText("InitFailedMessageGeneral_Body"), strDriverName, strDriverVersion, "$e")
				JOptionPane.showMessageDialog(null, strErrorMessage, strErrorTitle, JOptionPane.ERROR_MESSAGE)

				// Exit
				exitProcess(-2)
			}

			stopObserverClient()

			log.debug("Calling netLobby shutdown routine")
			stateNetGame.netLobby.shutdown()

			exitProcess(0)
		}

		/** 設定ファイルを保存 */
		fun saveConfig() {
			try {
				FileOutputStream("config/setting/slick.json", false).bufferedWriter().use {
					it.write(Json.encodeToString(propConfig))
				}
			} catch(e:IOException) {
				log.error("Failed to save Slick-specific config", e)
			}

			try {
				FileOutputStream("config/setting/global.json", false).bufferedWriter().use {
					it.write(Json.encodeToString(propGlobal))
				}
			} catch(e:Exception) {
				log.error("Failed to save global config", e)
			}
		}

		/** (Re-)Load global config file */
		internal fun loadGlobalConfig() {
			try {
				propGlobal = Json.decodeFromString(FileInputStream("config/setting/global.json").bufferedReader().use {it.readText()})
			} catch(_:Exception) {
			}
		}
		/** (Re-)Load slick config file */
		internal fun loadSlickConfig() {
			try {
				propConfig = Json.decodeFromString(FileInputStream("config/setting/slick.json").bufferedReader().use {it.readText()})
			} catch(_:Exception) {
			}
		}
		/** いろいろな設定を反映させる */
		internal fun setGeneralConfig() {
			appGameContainer.setTargetFrameRate(-1)
			beforeTime = System.nanoTime()
			overSleepTime = 0L
			noDelays = 0

			alternateFPSTiming = propConfig.render.alternateFPSTiming
			alternateFPSDynamicAdjust = propConfig.render.alternateFPSDynamicAdjust
			alternateFPSPerfectMode = propConfig.render.alternateFPSPerfectMode
			alternateFPSPerfectYield = propConfig.render.alternateFPSPerfectYield
			altMaxFPS = propConfig.render.maxFPS
			altMaxFPSCurrent = altMaxFPS
			periodCurrent = (1.0/altMaxFPSCurrent*1000000000).toLong()

			appGameContainer.setVSync(propConfig.render.vsync)
			appGameContainer.alwaysRender = !alternateFPSTiming


			appGameContainer.soundVolume = propConfig.audio.seVolume/128f

			ControllerManager.run {
				method = propConfig.ctrl.joyMethod
				config = propConfig.ctrl.joyPadConf
			}

			// useJInputKeyboard = propConfig.getProperty("option.useJInputKeyboard", true);
		}

		/** Screenshot保存
		 * @param container GameContainer
		 * @param g Graphics
		 */
		internal fun saveScreenShot(container:GameContainer, g:Graphics) {
			// Filenameを決める
			val dir = propGlobal.custom.ssDir
			val c = Calendar.getInstance()
			val dfm = SimpleDateFormat("yyyy_MM_dd_HH_mm_ss")
			val filename = "$dir/${dfm.format(c.time)}.png"
			log.info("Saving screenshot to $filename")

			g.isAntiAlias = false
			// Screenshot作成
			try {
				val ssFolder = File(dir)
				if(!ssFolder.exists())
					if(ssFolder.mkdirs())
						log.info("Created screenshot folder: $dir")
					else
						log.info("Couldn't create screenshot folder at $dir")

				val screenWidth = container.width
				val screenHeight = container.height

				val screenImage = Image(screenWidth, screenHeight)
				g.copyArea(screenImage, 0, 0)

				// 以下の方法だと上下さかさま
				// ImageOut.write(screenImage, filename);

				// なので自前で画面をコピーする
				BufferedImage(screenWidth, screenHeight, BufferedImage.TYPE_INT_RGB).also {ssImage ->

					for(i in 0..<screenWidth)
						for(j in 0..<screenHeight) {
							val color = screenImage.getColor(i, j+1) // SomehowY-coordinateThe+1I seem not to deviate

							val rgb = color.red and 0x000000FF shl 16 or (
								color.green and 0x000000FF shl 8) or
								(color.blue and 0x000000FF)

							ssImage.setRGB(i, j, rgb)
						}

					// ファイルに保存
					javax.imageio.ImageIO.write(ssImage, "png", File(filename))
				}
			} catch(e:Throwable) {
				log.error("Failed to create screen shot", e)
			}
		}

		/** 翻訳後のUIの文字列を取得
		 * @param str 文字列
		 * @return 翻訳後のUIの文字列 (無いならそのままstrを返す）
		 */
		fun getUIText(str:String):String =
			propLang.getProperty(str) ?: propLangDefault.getProperty(str, str) ?: str

		/** FPS cap routine
		 * @param inGame `true` if during the gameplay
		 */
		@JvmOverloads
		internal fun alternateFPSSleep(inGame:Boolean = false) {
			val maxFps = altMaxFPSCurrent

			if(maxFps>0) {
				var sleepFlag = false
				val afterTime:Long = System.nanoTime()

				val timeDiff:Long = afterTime-beforeTime

				val sleepTime:Long = periodCurrent-timeDiff-overSleepTime
				val sleepTimeInMillis:Long = sleepTime/1000000L

				if(sleepTimeInMillis>=10&&(!alternateFPSPerfectMode||!inGame)) {
					// If it is possible to use sleep
					try {
						Thread.sleep(sleepTimeInMillis)
					} catch(_:InterruptedException) {
					}

					// sleep() oversleep
					overSleepTime = System.nanoTime()-afterTime-sleepTime
					perfectFPSDelay = System.nanoTime()
					sleepFlag = true
				} else if(alternateFPSPerfectMode&&inGame||sleepTime>0) {
					// Perfect FPS
					overSleepTime = 0L
					if(altMaxFPSCurrent>altMaxFPS+5) altMaxFPSCurrent = altMaxFPS+5
					if(alternateFPSPerfectYield)
						while(System.nanoTime()<perfectFPSDelay+1000000000/altMaxFPS)
							Thread.yield()
					else
						@Suppress("ControlFlowWithEmptyBody")
						while(System.nanoTime()<perfectFPSDelay+1000000000/altMaxFPS) {
						}
					perfectFPSDelay += (1000000000/altMaxFPS).toLong()

					// Don't run in superFast after the heavy slowdown
					if(System.nanoTime()>perfectFPSDelay+2000000000/altMaxFPS) perfectFPSDelay = System.nanoTime()

					sleepFlag = true
				}

				if(!sleepFlag) {
					// Impossible to sleep!
					overSleepTime = 0L
					if(++noDelays>=16) {
						Thread.yield()
						noDelays = 0
					}
					perfectFPSDelay = System.nanoTime()
				}

				beforeTime = System.nanoTime()
				calcFPS(inGame, periodCurrent)
			} else {
				periodCurrent = (1.0/60*1000000000).toLong()
				calcFPS(inGame, periodCurrent)
			}
		}

		/** FPSの計算
		 * @param period FPSを計算する間隔
		 */
		private fun calcFPS(inGame:Boolean, period:Long) {
			frameCount++
			upTimeFrame++
			calcInterval += period

			// 1秒おきにFPSを再計算する
			if(calcInterval>=1000000000L) {
				val timeNow = System.nanoTime()

				// 実際の経過 timeを測定
				val realElapsedTime = timeNow-prevCalcTime // 単位: ns

				// FPSを計算
				// realElapsedTimeの単位はnsなのでsに変換する
				actualFPS = frameCount.toDouble()/realElapsedTime*1000000000L

				frameCount = 0L
				calcInterval = 0L
				prevCalcTime = timeNow

				// Set new target fps
				if(altMaxFPS>0&&alternateFPSDynamicAdjust&&!alternateFPSPerfectMode) {
					if(inGame) {
						// Too Slow
						if(actualFPS<altMaxFPS-1) altMaxFPSCurrent = minOf(altMaxFPS+20, altMaxFPSCurrent+1)
						// Too Fast
						else if(actualFPS>altMaxFPS+1) altMaxFPSCurrent = maxOf(1, altMaxFPS, altMaxFPSCurrent-1)

					} else if(altMaxFPSCurrent!=altMaxFPS) altMaxFPSCurrent = altMaxFPS
					periodCurrent = (1.0/altMaxFPSCurrent*1000000000).toLong()
				}
			}
		}

		/** FPS display */
		internal fun drawFPS() {
			if(propConfig.general.showFPS)
				FontNano.printFont(320-42, 0, "${df.format(actualFPS)}FPS", COLOR.WHITE, .5f)
		}

		/** Observerクライアントを開始 */
		internal fun startObserverClient() {
			log.debug("startObserverClient called")

			try {
				val fin = FileInputStream("config/setting/netobserver.xml")
				propObserver.loadFromXML(fin)
				fin.close()
			} catch(_:IOException) {
			}

			if(!propObserver.getProperty("observer.enable", false)) return
			if(netObserverClient?.isConnected==true) return

			val host = propObserver.getProperty("observer.host", "")
			val port = propObserver.getProperty("observer.port", mu.nu.nullpo.game.net.NetBaseClient.DEFAULT_PORT)

			if(host.isNotEmpty()&&port>0) {
				netObserverClient = NetObserverClient(host, port)
				netObserverClient?.start()
			}
		}

		/** Observerクライアントを停止 */
		internal fun stopObserverClient() {
			log.debug("stopObserverClient called")

			netObserverClient?.run {
				if(isConnected) send("disconnect\n")
				threadRunning = false
				connectedFlag = false
			}
			netObserverClient = null
			propObserver.clear()
		}

		/** Observerクライアントからの情報を描画 */
		internal fun drawObserverClient() {
			netObserverClient?.let {
				if(it.isConnected)
					FontNano.printFont(
						0,
						480-16,
						"%40s".format("%d/%d".format(it.observerCount, it.playerCount)),
						if(it.observerCount>0&&it.playerCount>0) COLOR.RED
						else if(it.observerCount>1) COLOR.GREEN else COLOR.BLUE
					)

			}
		}
	}
}
/** FPS cap routine */
