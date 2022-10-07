/*
 * Copyright (c) 2010-2021, NullNoname
 * Kotlin converted and modified by Venom=Nhelv
 * All rights reserved.
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
package mu.nu.nullpo.tool.musiclisteditor

import mu.nu.nullpo.game.component.BGMStatus.BGM
import mu.nu.nullpo.util.CustomProperties
import org.apache.logging.log4j.LogManager
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Dimension
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.Locale
import javax.swing.AbstractAction
import javax.swing.Action
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JCheckBox
import javax.swing.JFileChooser
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JOptionPane
import javax.swing.JPanel
import javax.swing.JPopupMenu
import javax.swing.JTabbedPane
import javax.swing.JTextField
import javax.swing.UIManager
import javax.swing.WindowConstants

/** MusicListEditor (音楽リスト編集ツール) */
class MusicListEditor:JFrame(), ActionListener {
	/** Swing版のSave settings用Property file */
	private val propConfig = CustomProperties()

	/** Default language file */
	private val propLangDefault = CustomProperties()

	/** UI翻訳用Property file */
	private val propLang = CustomProperties()

	/** 音楽リストが含まれるProperty file */
	private val propMusic = CustomProperties()

	/** タブ */
	private var tabPane:JTabbedPane = JTabbedPane()

	/** 音楽のFilename用テキストボックス */
	private var txtfldMusicFileNames:Array<JTextField> = emptyArray()

	/** ループなし check ボックス */
	private var chkboxNoLoop:Array<JCheckBox> = emptyArray()

	/** ファイル選択ダイアログ */
	private var fileChooser:JFileChooser? = null

	/** ファイルフィルタのHashMap */
	private var hashmapFileFilters:HashMap<String, SimpleFileFilter>? = null

	/** Constructor */
	init {
		init()
		isVisible = true
	}

	/** Initialization */
	private fun init() {
		// 設定ファイル読み込み
		try {
			val `in` = FileInputStream("config/setting/swing.xml")
			propConfig.loadFromXML(`in`)
			`in`.close()
		} catch(e:IOException) {
		}

		// 言語ファイル読み込み
		try {
			val `in` = FileInputStream("config/lang/musiclisteditor_default.xml")
			propLangDefault.loadFromXML(`in`)
			`in`.close()
		} catch(e:IOException) {
			log.error("Couldn't load default UI language file", e)
		}

		try {
			val `in` = FileInputStream(
				"config/lang/musiclisteditor_${Locale.getDefault().country}.xml"
			)
			propLang.loadFromXML(`in`)
			`in`.close()
		} catch(e:IOException) {
		}

		// 音楽リスト読み込み
		loadMusicList()

		// Look&Feel設定
		if(propConfig.getProperty("option.usenativelookandfeel", true))
			try {
				UIManager.getInstalledLookAndFeels()
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
			} catch(e:Exception) {
				log.warn("Failed to set native look&feel", e)
			}

		defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
		title = getUIText("Title_MusicListEditor")

		initUI()
		pack()
	}

	/** 画面のInitialization */
	private fun initUI() {
		contentPane.layout = BoxLayout(contentPane, BoxLayout.Y_AXIS)

		// メイン画面
		val pMusicSetting = JPanel()
		pMusicSetting.layout = BoxLayout(pMusicSetting, BoxLayout.Y_AXIS)
		pMusicSetting.alignmentX = Component.LEFT_ALIGNMENT
		this.add(pMusicSetting)

		// タブ全体 --------------------------------------------------
		tabPane = JTabbedPane()
		this.add(tabPane, BorderLayout.NORTH)
		val num = BGM.countAll-1
		val panels:Array<JPanel> = Array(BGM.countCategory-1) {
			JPanel().apply {
				layout = BoxLayout(this, BoxLayout.Y_AXIS)
				alignmentY = Component.TOP_ALIGNMENT
			}
		}
		txtfldMusicFileNames = Array(num) {JTextField(45)}
		chkboxNoLoop = Array(num) {JCheckBox()}
		//TODO:Tab : BGM. : Category
		BGM.values.forEachIndexed {i, Track ->
			if(Track!=BGM.Silent) {
				val i = i-1
				val name = "${Track.name}.${Track.idx}"
				val id = Track.id
				panels[Track.id-1].let {
					if(Track.idx==0) tabPane.addTab(Track.longName, it)

					val pMusicTemp = JPanel(BorderLayout())
					it.add(pMusicTemp)

					val pMusicTempLabels = JPanel(BorderLayout())
					pMusicTemp.add(pMusicTempLabels, BorderLayout.WEST)
					pMusicTempLabels.add(JLabel("${Track.id}-${Track.idx} ${Track.fullName}"), BorderLayout.WEST)

					val pMusicTempTexts = JPanel(BorderLayout())
					pMusicTemp.add(pMusicTempTexts, BorderLayout.EAST)

					txtfldMusicFileNames[i].componentPopupMenu = TextFieldPopupMenu(txtfldMusicFileNames[i])
					txtfldMusicFileNames[i].text = propMusic.getProperty("music.filename.$name", "")
					pMusicTempTexts.add(txtfldMusicFileNames[i], BorderLayout.CENTER)

					val pMusicTempTextsButtons = JPanel(BorderLayout())
					pMusicTempTexts.add(pMusicTempTextsButtons, BorderLayout.EAST)

					chkboxNoLoop[i].toolTipText = getUIText("MusicListEditor_NoLoop_Tip")
					chkboxNoLoop[i].isSelected = propMusic.getProperty("music.noloop.$name", false)
					pMusicTempTextsButtons.add(chkboxNoLoop[i], BorderLayout.WEST)

					val btnClear = JButton(getUIText("MusicListEditor_Clear"))
					btnClear.toolTipText = getUIText("MusicListEditor_Clear_Tip")
					btnClear.actionCommand = "Clear$i"
					btnClear.addActionListener(this)
					pMusicTempTextsButtons.add(btnClear, BorderLayout.CENTER)

					val btnOpen = JButton(getUIText("MusicListEditor_OpenFileDialog"))
					btnOpen.toolTipText = getUIText("MusicListEditor_OpenFileDialog_Tip")
					btnOpen.actionCommand = "OpenFileDialog$i"
					btnOpen.addActionListener(this)
					pMusicTempTextsButtons.add(btnOpen, BorderLayout.EAST)
				}
			}
		}

		// 画面下の button類
		val pButtons = JPanel()
		pButtons.layout = BoxLayout(pButtons, BoxLayout.X_AXIS)
		pButtons.alignmentX = Component.LEFT_ALIGNMENT
		this.add(pButtons)

		val btnOK = JButton(getUIText("MusicListEditor_OK"))
		btnOK.addActionListener(this)
		btnOK.actionCommand = "OK"
		btnOK.maximumSize = Dimension(java.lang.Short.MAX_VALUE.toInt(), 30)
		btnOK.setMnemonic('O')
		pButtons.add(btnOK)

		val btnCancel = JButton(getUIText("MusicListEditor_Cancel"))
		btnCancel.addActionListener(this)
		btnCancel.actionCommand = "Cancel"
		btnCancel.maximumSize = Dimension(java.lang.Short.MAX_VALUE.toInt(), 30)
		btnCancel.setMnemonic('C')
		pButtons.add(btnCancel)

		// ファイルフィルタ
		hashmapFileFilters = HashMap()
		hashmapFileFilters?.also {
			it[".wav"] = SimpleFileFilter(".wav", getUIText("FileChooser_wav"))
			it[".xm"] = SimpleFileFilter(".xm", getUIText("FileChooser_xm"))
			it[".mod"] = SimpleFileFilter(".mod", getUIText("FileChooser_mod"))
			it[".aif"] = SimpleFileFilter(".aif", getUIText("FileChooser_aif"))
			it[".aiff"] = SimpleFileFilter(".aif", getUIText("FileChooser_aiff"))
			it[".ogg"] = SimpleFileFilter(".ogg", getUIText("FileChooser_ogg"))
		}
		// ファイル選択ダイアログ
		fileChooser = JFileChooser().apply {
			hashmapFileFilters?.forEach {(_, filter) ->
				addChoosableFileFilter(filter)
			}
		}
	}

	/** 翻訳後のUIの文字列を取得
	 * @param str 文字列
	 * @return 翻訳後のUIの文字列 (無いならそのままstrを返す）
	 */
	private fun getUIText(str:String):String = propLang.getProperty(str) ?: propLangDefault.getProperty(str, str)

	/** 音楽リスト読み込み */
	private fun loadMusicList() {
		propMusic.clear()
		try {
			val `in` = FileInputStream("config/setting/music.xml")
			propMusic.loadFromXML(`in`)
			`in`.close()
		} catch(e:IOException) {
		}
	}

	/** 音楽リストを保存
	 * @throws IOException 保存失敗
	 */
	@Throws(IOException::class)
	private fun saveMusicList(prop:CustomProperties) {
		try {
			val out = FileOutputStream("config/setting/music.xml")
			prop.storeToXML(out, "NullpoMino Music List")
			out.close()
		} catch(e:IOException) {
			log.error("Failed to save music list file", e)
			throw e
		}
	}

	/* Menu 実行時の処理 */
	override fun actionPerformed(e:ActionEvent) {
		if(e.actionCommand.startsWith("OpenFileDialog")) {
			// Button number取得
			var number = 0
			try {
				val strNum = e.actionCommand.replaceFirst(Regex("OpenFileDialog"), "")
				number = strNum.toInt()
			} catch(e2:Exception) {
				log.error("OpenFileDialog: Failed to get button number", e2)
				return
			}

			// カレントディレクトリ
			val currentDirectory = System.getProperty("user.dir")

			//  default ディレクトリを設定
			var defaultDirectory = txtfldMusicFileNames[number].text
			if(defaultDirectory.isEmpty()) defaultDirectory = "$currentDirectory/res/bgm"

			val file = File(defaultDirectory)
			fileChooser?.currentDirectory = file

			// ファイル選択ダイアログの default 拡張子を設定
			if(file.isFile)
				try {
					val strName = file.name
					val lastPeriod = strName.lastIndexOf('.')
					if(lastPeriod!=-1) {
						val strExt = strName.substring(lastPeriod, strName.length)
						fileChooser?.fileFilter = hashmapFileFilters!![strExt]
					}
				} catch(e2:Exception) {
				}

			// ファイル選択ダイアログを表示
			if(fileChooser?.showOpenDialog(this)==JFileChooser.APPROVE_OPTION) {
				val strPath = fileChooser!!.selectedFile.path
				txtfldMusicFileNames[number].text = strPath
			}
		} else if(e.actionCommand.startsWith("Clear")) {
			var number = 0
			try {
				val strNum = e.actionCommand.replaceFirst(Regex("Clear"), "")
				number = strNum.toInt()
			} catch(e2:Exception) {
				log.error("Clear: Failed to get button number", e2)
				return
			}

			txtfldMusicFileNames[number].text = ""
		} else if(e.actionCommand=="OK") {
			val prop = CustomProperties()
			txtfldMusicFileNames.forEachIndexed {i, it ->
				val track = BGM.values[i+1].let {"${it.name}.${it.idx}"}
				prop.setProperty("music.filename.$track", it.text)
				prop.setProperty("music.noloop.$track", chkboxNoLoop[i].isSelected)
			}

			try {
				saveMusicList(prop)
			} catch(e2:IOException) {
				JOptionPane.showMessageDialog(
					this,
					getUIText("Message_FileSaveFailed")+"\n"+e2.localizedMessage,
					getUIText("Title_FileSaveFailed"), JOptionPane.ERROR_MESSAGE
				)
			}

			dispose()
		} else if(e.actionCommand=="Cancel") dispose()
	}

	/** ポップアップMenu
	 * [出展](http://terai.xrea.jp/Swing/DefaultEditorKit.html) */
	private inner class TextFieldPopupMenu(field:JTextField):JPopupMenu() {
		private val cutAction:Action = object:AbstractAction(getUIText("Popup_Cut")) {
			private val serialVersionUID = 1L

			override fun actionPerformed(evt:ActionEvent) {
				field.cut()
			}
		}
		private val copyAction:Action = object:AbstractAction(getUIText("Popup_Copy")) {
			private val serialVersionUID = 1L

			override fun actionPerformed(evt:ActionEvent) {
				field.copy()
			}
		}
		private val pasteAction:Action = object:AbstractAction(getUIText("Popup_Paste")) {
			private val serialVersionUID = 1L

			override fun actionPerformed(evt:ActionEvent) {
				field.paste()
			}
		}
		private val deleteAction:Action = object:AbstractAction(getUIText("Popup_Delete")) {
			private val serialVersionUID = 1L

			override fun actionPerformed(evt:ActionEvent) {
				field.replaceSelection(null)
			}
		}
		private val selectAllAction:Action = object:AbstractAction(getUIText("Popup_SelectAll")) {
			private val serialVersionUID = 1L

			override fun actionPerformed(evt:ActionEvent) {
				field.selectAll()
			}
		}

		init {
			add(cutAction)
			add(copyAction)
			add(pasteAction)
			add(deleteAction)
			add(selectAllAction)
		}

		override fun show(c:Component, x:Int, y:Int) {
			val field = c as JTextField
			val flg = field.selectedText!=null
			cutAction.isEnabled = flg
			copyAction.isEnabled = flg
			deleteAction.isEnabled = flg
			selectAllAction.isEnabled = field.isFocusOwner
			super.show(c, x, y)
		}

		private val serialVersionUID = 1L
	}

	companion object {
		/** Serial version ID */
		private const val serialVersionUID = -6480034324392568869L

		/** Log */
		internal val log = LogManager.getLogger()

		/** メイン関数
		 * @param args コマンドLines引数
		 */
		@JvmStatic
		fun main(args:Array<String>) {
			org.apache.logging.log4j.core.config.Configurator.initialize(log.name, "config/etc/log.xml")
			log.debug("MusicListEditor start")
			MusicListEditor()
		}
	}
}
