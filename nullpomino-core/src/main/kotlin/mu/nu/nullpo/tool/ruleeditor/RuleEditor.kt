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
package mu.nu.nullpo.tool.ruleeditor

import mu.nu.nullpo.game.component.Block
import mu.nu.nullpo.game.component.Piece
import mu.nu.nullpo.game.component.RuleOptions
import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.util.CustomProperties
import org.apache.logging.log4j.LogManager
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Component
import java.awt.Dimension
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import java.awt.image.BufferedImage
import java.io.*
import java.net.MalformedURLException
import java.net.URL
import java.util.Locale
import java.util.Vector
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import javax.imageio.ImageIO
import javax.swing.*
import javax.swing.filechooser.FileFilter

/** ルールエディター */
class RuleEditor:JFrame, ActionListener {

	/** Swing版のSave settings用Property file */
	val propConfig = CustomProperties()

	/** Default language file */
	private val propLangDefault = CustomProperties()

	/** UI翻訳用Property file */
	private val propLang = CustomProperties()

	//----------------------------------------------------------------------
	/** 今開いているFilename (null:なし) */
	private var strNowFile:String? = null

	/** タブ */
	private val tabPane:JTabbedPane = JTabbedPane()

	//----------------------------------------------------------------------
	/* 基本設定パネル */

	/** Rule name */
	private val txtfldRuleName:JTextField = JTextField("", 15)

	/** NEXT表示countのテキストfield */
	private val txtfldNextDisplay:JTextField = JTextField("", 5)

	/** Game style combobox */
	private var comboboxStyle:JComboBox<*>? = null

	/** 絵柄のComboボックス */
	private var comboboxSkin:JComboBox<*>? = null

	/** ghost is enabled */
	private val chkboxGhost:JCheckBox = JCheckBox()

	/** Blockピースがfield枠外から出現 */
	private val chkboxEnterAboveField:JCheckBox = JCheckBox()

	/** 出現予定地が埋まっているときにY-coordinateを上にずらすMaximum count */
	private val txtfldEnterMaxDistanceY:JTextField = JTextField("", 5)

	/** NEXT順生成アルゴリズム */
	private var comboboxRandomizer:JComboBox<*>? = null

	/** NEXT順生成アルゴリズムのリスト */
	private var vectorRandomizer:Vector<String>? = null

	/** NEXT順生成アルゴリズムのリセット button
	 * private JButton btnResetRandomizer; */

	//----------------------------------------------------------------------
	/* field設定パネル */

	/** fieldの幅 */
	private val txtfldFieldWidth:JTextField = JTextField("", 5)

	/** Field height */
	private val txtfldFieldHeight:JTextField = JTextField("", 5)

	/** fieldの見えない部分の高さ */
	private val txtfldFieldHiddenHeight:JTextField = JTextField("", 5)

	/** fieldの天井 */
	private val chkboxFieldCeiling:JCheckBox = JCheckBox()

	/** field枠内に置けないと死亡 */
	private val chkboxFieldLockoutDeath:JCheckBox = JCheckBox()

	/** field枠外にはみ出しただけで死亡 */
	private val chkboxFieldPartialLockoutDeath:JCheckBox = JCheckBox()

	//----------------------------------------------------------------------
	/* ホールド設定パネル */

	/** ホールド is enabled */
	private val chkboxHoldEnable:JCheckBox = JCheckBox()

	/** 先行ホールド */
	private val chkboxHoldInitial:JCheckBox = JCheckBox()

	/** 先行ホールド連続使用不可 */
	private val chkboxHoldInitialLimit:JCheckBox = JCheckBox()

	/** ホールドを使ったときにBlockピースの向きを初期状態に戻す */
	private val chkboxHoldResetDirection:JCheckBox = JCheckBox()

	/** ホールドできる count (-1:無制限) */
	private val txtfldHoldLimit:JTextField = JTextField("", 5)

	//----------------------------------------------------------------------
	/* ドロップ設定パネル */

	/** Hard drop使用可否 */
	private val chkboxDropHardDropEnable:JCheckBox = JCheckBox()

	/** Hard dropで即固定 */
	private val chkboxDropHardDropLock:JCheckBox = JCheckBox()

	/** Hard drop連続使用不可 */
	private val chkboxDropHardDropLimit:JCheckBox = JCheckBox()

	/** Soft drop使用可否 */
	private val chkboxDropSoftDropEnable:JCheckBox = JCheckBox()

	/** Soft dropで即固定 */
	private val chkboxDropSoftDropLock:JCheckBox = JCheckBox()

	/** Soft drop連続使用不可 */
	private val chkboxDropSoftDropLimit:JCheckBox = JCheckBox()

	/** 接地状態でSoft dropすると即固定 */
	private val chkboxDropSoftDropSurfaceLock:JCheckBox = JCheckBox()

	/** Soft drop速度 */
	private val txtfldDropSoftDropSpeed:JTextField = JTextField("", 5)

	/** Soft drop速度をCurrent 通常速度×n倍にする */
	private val chkboxDropSoftDropMultiplyNativeSpeed:JCheckBox = JCheckBox()

	/** Use new soft drop codes */
	private val chkboxDropSoftDropGravitySpeedLimit:JCheckBox = JCheckBox()

	//----------------------------------------------------------------------
	/* rotation設定パネル */

	/** 先行rotation */
	private val chkboxRotateInitial:JCheckBox = JCheckBox()

	/** 先行rotation連続使用不可 */
	private val chkboxRotateInitialLimit:JCheckBox = JCheckBox()

	/** Wallkick */
	private val chkboxRotateWallkick:JCheckBox = JCheckBox()

	/** 先行rotationでもWallkickする */
	private val chkboxRotateInitialWallkick:JCheckBox = JCheckBox()

	/** 上DirectionへのWallkickができる count (-1:無限) */
	private val txtfldRotateMaxUpwardWallkick:JTextField = JTextField("", 5)

	/** falseなら左が正rotation, When true,右が正rotation */
	private val chkboxRotateButtonDefaultRight:JCheckBox = JCheckBox()

	/** 逆rotationを許可 (falseなら正rotationと同じ) */
	private val chkboxRotateButtonAllowReverse:JCheckBox = JCheckBox()

	/** 2rotationを許可 (falseなら正rotationと同じ) */
	private val chkboxRotateButtonAllowDouble:JCheckBox = JCheckBox()

	/** Wallkickアルゴリズム */
	private var comboboxWallkickSystem:JComboBox<*>? = null

	/** Wallkickアルゴリズムのリスト */
	private var vectorWallkickSystem:Vector<String>? = null

	/** Wallkickアルゴリズムのリセット button */
	private var btnResetWallkickSystem:JButton? = null

	//----------------------------------------------------------------------
	/* 固定 time設定パネル */

	/** 最低固定 time */
	private val txtfldLockDelayMin:JTextField = JTextField("", 5)

	/** 最高固定 time */
	private val txtfldLockDelayMax:JTextField = JTextField("", 5)

	/** 落下で固定 timeリセット */
	private val chkboxLockDelayLockResetFall:JCheckBox = JCheckBox()

	/** 移動で固定 timeリセット */
	private val chkboxLockDelayLockResetMove:JCheckBox = JCheckBox()

	/** rotationで固定 timeリセット */
	private val chkboxLockDelayLockResetRotate:JCheckBox = JCheckBox()

	/** Lock delay reset by wallkick */
	private val chkboxLockDelayLockResetWallkick:JCheckBox = JCheckBox()

	/** 横移動 counterとrotation counterを共有 (横移動 counterだけ使う) */
	private val chkboxLockDelayLockResetLimitShareCount:JCheckBox = JCheckBox()

	/** 横移動 count制限 */
	private val txtfldLockDelayLockResetLimitMove:JTextField = JTextField("", 5)

	/** rotation count制限 */
	private val txtfldLockDelayLockResetLimitRotate:JTextField = JTextField("", 5)

	/** 横移動 counterかrotation counterが超過したら固定 timeリセットを無効にする */
	private var radioLockDelayLockResetLimitOverNoReset:JRadioButton? = null

	/** 横移動 counterかrotation counterが超過したら即座に固定する */
	private var radioLockDelayLockResetLimitOverInstant:JRadioButton? = null

	/** 横移動 counterかrotation counterが超過したらWallkick無効にする */
	private var radioLockDelayLockResetLimitOverNoWallkick:JRadioButton? = null

	//----------------------------------------------------------------------
	/* ARE設定パネル */

	/** 最低ARE */
	private val txtfldAREMin:JTextField = JTextField("", 5)

	/** 最高ARE */
	private val txtfldAREMax:JTextField = JTextField("", 5)

	/** 最低ARE after line clear */
	private val txtfldARELineMin:JTextField = JTextField("", 5)

	/** 最高ARE after line clear */
	private val txtfldARELineMax:JTextField = JTextField("", 5)

	/** 固定した瞬間に光る frame count */
	private val txtfldARELockFlash:JTextField = JTextField("", 5)

	/** Blockが光る専用 frame を入れる */
	private val chkboxARELockFlashOnlyFrame:JCheckBox = JCheckBox()

	/** Line clear前にBlockが光る frame を入れる */
	private val chkboxARELockFlashBeforeLineClear:JCheckBox = JCheckBox()

	/** ARE cancel on move checkbox */
	private val chkboxARECancelMove:JCheckBox = JCheckBox()

	/** ARE cancel on rotate checkbox */
	private val chkboxARECancelRotate:JCheckBox = JCheckBox()

	/** ARE cancel on hold checkbox */
	private val chkboxARECancelHold:JCheckBox = JCheckBox()

	//----------------------------------------------------------------------
	/* Line clear設定パネル */

	/** 最低Line clear time */
	private val txtfldLineDelayMin:JTextField = JTextField("", 5)

	/** 最高Line clear time */
	private val txtfldLineDelayMax:JTextField = JTextField("", 5)

	/** 落下アニメ */
	private val chkboxLineFallAnim:JCheckBox = JCheckBox()

	/** Line delay cancel on move checkbox */
	private val chkboxLineCancelMove:JCheckBox = JCheckBox()

	/** Line delay cancel on rotate checkbox */
	private val chkboxLineCancelRotate:JCheckBox = JCheckBox()

	/** Line delay cancel on hold checkbox */
	private val chkboxLineCancelHold:JCheckBox = JCheckBox()

	//----------------------------------------------------------------------
	/* 移動設定パネル */

	/** 最低横溜め time */
	private val txtfldMoveDASMin:JTextField = JTextField("", 5)

	/** 最高横溜め time */
	private val txtfldMoveDASMax:JTextField = JTextField("", 5)

	/** 横移動間隔 */
	private val txtfldMoveDASDelay:JTextField = JTextField("", 5)

	/** Ready画面で横溜め可能 */
	private val chkboxMoveDASInReady:JCheckBox = JCheckBox()

	/** 最初の frame で横溜め可能 */
	private val chkboxMoveDASInMoveFirstFrame:JCheckBox = JCheckBox()

	/** Blockが光った瞬間に横溜め可能 */
	private val chkboxMoveDASInLockFlash:JCheckBox = JCheckBox()

	/** Line clear中に横溜め可能 */
	private val chkboxMoveDASInLineClear:JCheckBox = JCheckBox()

	/** ARE中に横溜め可能 */
	private val chkboxMoveDASInARE:JCheckBox = JCheckBox()

	/** AREの最後の frame で横溜め可能 */
	private val chkboxMoveDASInARELastFrame:JCheckBox = JCheckBox()

	/** Ending突入画面で横溜め可能 */
	private val chkboxMoveDASInEndingStart:JCheckBox = JCheckBox()

	/** DAS charge on blocked move checkbox */
	private val chkboxMoveDASChargeOnBlockedMove:JCheckBox = JCheckBox()

	/** Store DAS Charge on neutral checkbox */
	private val chkboxMoveDASStoreChargeOnNeutral:JCheckBox = JCheckBox()

	/** Redirect in delay checkbox */
	private val chkboxMoveDASRedirectInDelay:JCheckBox = JCheckBox()

	/** 最初の frame に移動可能 */
	private val chkboxMoveFirstFrame:JCheckBox = JCheckBox()

	/** 斜め移動 */
	private val chkboxMoveDiagonal:JCheckBox = JCheckBox()

	/** 上下同時押し可能 */
	private val chkboxMoveUpAndDown:JCheckBox = JCheckBox()

	/** 左右同時押し可能 */
	private val chkboxMoveLeftAndRightAllow:JCheckBox = JCheckBox()

	/** 左右同時押ししたときに前の frame の input Directionを優先する */
	private val chkboxMoveLeftAndRightUsePreviousInput:JCheckBox = JCheckBox()

	/** Shift lock checkbox */
	private val chkboxMoveShiftLockEnable:JCheckBox = JCheckBox()

	//----------------------------------------------------------------------
	/* rotationパターン補正パネル */

	/** プリセット選択Comboボックス */
	private var comboboxPieceOffset:JComboBox<*>? = null

	/** rotationパターン補正タブ */
	private var tabPieceOffset:JTabbedPane? = null

	/** rotationパターン補正(X) input 欄 */
	private var txtfldPieceOffsetX:Array<Array<JTextField>>? = null

	/** rotationパターン補正(Y) input 欄 */
	private var txtfldPieceOffsetY:Array<Array<JTextField>>? = null

	//----------------------------------------------------------------------
	/* rotationパターン補正パネル */

	/** rotationパターン補正タブ */
	private var tabPieceSpawn:JTabbedPane? = null

	/** 出現位置補正(X) input 欄 */
	private var txtfldPieceSpawnX:Array<Array<JTextField>>? = null

	/** 出現位置補正(Y) input 欄 */
	private var txtfldPieceSpawnY:Array<Array<JTextField>>? = null

	/** Big時出現位置補正(X) input 欄 */
	private var txtfldPieceSpawnBigX:Array<Array<JTextField>>? = null

	/** Big時出現位置補正(Y) input 欄 */
	private var txtfldPieceSpawnBigY:Array<Array<JTextField>>? = null

	//----------------------------------------------------------------------
	/* 色設定パネル */

	/** 色選択Comboボックス */
	private var comboboxPieceColor:Array<JComboBox<*>>? = null

	//----------------------------------------------------------------------
	/* 初期Direction設定パネル */

	/** 初期Direction選択Comboボックス */
	private var comboboxPieceDirection:Array<JComboBox<*>>? = null

	//----------------------------------------------------------------------
	/** Block画像 */
	private var imgBlockSkins:Array<BufferedImage>? = null

	/** Constructor */
	constructor():super() {

		init()
		readRuleToUI(RuleOptions())

		isVisible = true
	}

	/** 特定のファイルを読み込むConstructor
	 * @param filename Filename (空文字列かnullにするとパラメータなしConstructorと同じ動作）
	 */
	constructor(filename:String?):super() {

		init()

		var ruleOpt = RuleOptions()

		if(filename!=null&&filename.isNotEmpty())
			try {
				ruleOpt = load(filename)
				strNowFile = filename
				title = "${getUIText("Title_RuleEditor")}:$strNowFile"
			} catch(e:IOException) {
				log.error("Failed to load rule data from $filename", e)
				JOptionPane.showMessageDialog(this, "${getUIText("Message_FileLoadFailed")}\n$e",
					getUIText("Title_FileLoadFailed"), JOptionPane.ERROR_MESSAGE)
			}

		readRuleToUI(ruleOpt)

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
			val `in` = FileInputStream("config/lang/ruleeditor_default.xml")
			propLangDefault.loadFromXML(`in`)
			`in`.close()
		} catch(e:IOException) {
			log.error("Couldn't load default UI language file", e)
		}

		try {
			val `in` = FileInputStream("config/lang/ruleeditor_${Locale.getDefault().country}.xml")
			propLang.loadFromXML(`in`)
			`in`.close()
		} catch(e:IOException) {
		}

		// Look&Feel設定
		if(propConfig.getProperty("option.usenativelookandfeel", true))
			try {
				UIManager.getInstalledLookAndFeels()
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
			} catch(e:Exception) {
				log.warn("Failed to set native look&feel", e)
			}

		strNowFile = null

		title = getUIText("Title_RuleEditor")
		defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE

		loadBlockSkins()

		initUI()
		pack()
	}

	/** 画面のInitialization */
	private fun initUI() {
		contentPane.layout = BorderLayout()

		// Menuバー --------------------------------------------------
		val menuBar = JMenuBar()
		jMenuBar = menuBar

		// ファイルMenu
		val mFile = JMenu(getUIText("JMenu_File"))
		mFile.setMnemonic('F')
		menuBar.add(mFile)

		// 新規作成
		mFile.add(JMenuItem(getUIText("JMenuItem_New")).also {
			it.setMnemonic('N')
			it.accelerator = KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK)
			it.actionCommand = "New"
			it.addActionListener(this)
		})

		// 開く
		mFile.add(JMenuItem(getUIText("JMenuItem_Open")).also {
			it.setMnemonic('O')
			it.accelerator = KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK)
			it.actionCommand = "Open"
			it.addActionListener(this)
		})

		// Up書き保存
		mFile.add(JMenuItem(getUIText("JMenuItem_Save")).also {
			it.setMnemonic('S')
			it.accelerator = KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK)
			it.actionCommand = "Save"
			it.addActionListener(this)
		})

		// Nameを付けて保存
		mFile.add(JMenuItem(getUIText("JMenuItem_SaveAs")).also {
			it.setMnemonic('A')
			it.accelerator = KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK or InputEvent.ALT_DOWN_MASK)
			it.actionCommand = "SaveAs"
			it.addActionListener(this)
		})

		// 終了
		mFile.add(JMenuItem(getUIText("JMenuItem_Exit")).also {
			it.setMnemonic('X')
			it.actionCommand = "Exit"
			it.addActionListener(this)
		})

		// タブ全体 --------------------------------------------------
		contentPane.add(tabPane, BorderLayout.NORTH)

		// 基本設定タブ --------------------------------------------------
		val panelBasic = JPanel()
		panelBasic.layout = BoxLayout(panelBasic, BoxLayout.Y_AXIS)
		tabPane.addTab(getUIText("TabName_Basic"), panelBasic)

		// Rule name
		txtfldRuleName.columns = 15
		panelBasic.add(JPanel().apply {
			add(JLabel(getUIText("Basic_RuleName")))
			add(txtfldRuleName)
		})

		// NEXT表示count
		panelBasic.add(JPanel().apply {
			add(JLabel(getUIText("Basic_NextDisplay")))
			txtfldNextDisplay.columns = 5
			add(txtfldNextDisplay)
		})

		// Game style
		val pStyle = JPanel()
		pStyle.add(JLabel(getUIText("Basic_Style")))
		comboboxStyle = JComboBox(GameEngine.GAMESTYLE_NAMES).apply {
			preferredSize = Dimension(100, 30)
		}
		pStyle.add(comboboxStyle)
		panelBasic.add(pStyle)

		// 絵柄
		val pSkin = JPanel()
		panelBasic.add(pSkin)

		pSkin.add(JLabel(getUIText("Basic_Skin")))

		val model = DefaultComboBoxModel<ComboLabel>()
		imgBlockSkins?.forEachIndexed {i, it ->
			model.addElement(ComboLabel("$i", ImageIcon(it)))
		}
		comboboxSkin = JComboBox(model).apply {
			renderer = ComboLabelCellRenderer()
			preferredSize = Dimension(190, 30)
		}
		pSkin.add(comboboxSkin)

		// ghost
		chkboxGhost.text = getUIText("Basic_Ghost")
		panelBasic.add(chkboxGhost)

		// field枠外から出現
		chkboxEnterAboveField.text = getUIText("Basic_EnterAboveField")
		panelBasic.add(chkboxEnterAboveField)

		// 出現予定地が埋まっているときにY-coordinateを上にずらすMaximum count
		val pEnterMaxDistanceY = JPanel()
		panelBasic.add(pEnterMaxDistanceY)

		pEnterMaxDistanceY.add(JLabel(getUIText("Basic_EnterMaxDistanceY")))

		txtfldEnterMaxDistanceY.columns = 5
		pEnterMaxDistanceY.add(txtfldEnterMaxDistanceY)

		// NEXT順生成アルゴリズム
		val pRandomizer = JPanel()
		panelBasic.add(pRandomizer)

		pRandomizer.add(JLabel(getUIText("Basic_Randomizer")))

		vectorRandomizer = getTextFileVector("config/list/randomizer.lst")
		comboboxRandomizer = JComboBox(createShortStringVector(vectorRandomizer)).apply {
			preferredSize = Dimension(200, 30)
			pRandomizer.add(this)
		}

		/*val btnResetRandomizer = JButton(getUIText("Basic_Reset")).also {
			it.setMnemonic('R')
			it.actionCommand = "ResetRandomizer"
			it.addActionListener(this)
			pRandomizer.add(it)
		}*/

		// fieldタブ --------------------------------------------------
		val panelField = JPanel()
		panelField.layout = BoxLayout(panelField, BoxLayout.Y_AXIS)
		tabPane.addTab(getUIText("TabName_Field"), panelField)

		// fieldの幅
		panelField.add(JPanel().apply {
			add(JLabel(getUIText("Field_FieldWidth")))
			add(txtfldFieldWidth)
		})

		// Field height
		panelField.add(JPanel().apply {
			add(JLabel(getUIText("Field_FieldHeight")))
			add(txtfldFieldHeight)
		})

		// fieldの見えない部分の高さ
		panelField.add(JPanel().apply {
			add(JLabel(getUIText("Field_FieldHiddenHeight")))
			add(txtfldFieldHiddenHeight)
		})

		// fieldの天井
		chkboxFieldCeiling.text = getUIText("Field_FieldCeiling")
		panelField.add(chkboxFieldCeiling)

		// field枠内に置けないと死亡
		chkboxFieldLockoutDeath.text = getUIText("Field_FieldLockoutDeath")
		panelField.add(chkboxFieldLockoutDeath)

		// field枠外にはみ出しただけで死亡
		chkboxFieldPartialLockoutDeath.text = getUIText("Field_FieldPartialLockoutDeath")
		panelField.add(chkboxFieldPartialLockoutDeath)

		// ホールドタブ --------------------------------------------------
		val panelHold = JPanel()
		panelHold.layout = BoxLayout(panelHold, BoxLayout.Y_AXIS)
		tabPane.addTab(getUIText("TabName_Hold"), panelHold)

		// ホールド is enabled
		chkboxHoldEnable.text = getUIText("Hold_HoldEnable")
		panelHold.add(chkboxHoldEnable)

		// 先行ホールド
		chkboxHoldInitial.text = getUIText("Hold_HoldInitial")
		panelHold.add(chkboxHoldInitial)

		// 先行ホールド連続使用不可
		chkboxHoldInitialLimit.text = getUIText("Hold_HoldInitialLimit")
		panelHold.add(chkboxHoldInitialLimit)

		// ホールドを使ったときにBlockピースの向きを初期状態に戻す
		chkboxHoldResetDirection.text = (getUIText("Hold_HoldResetDirection"))
		panelHold.add(chkboxHoldResetDirection)

		// ホールドできる count
		txtfldHoldLimit.columns = 5
		val pHoldLimit = JPanel().apply {
			add(JLabel(getUIText("Hold_HoldLimit")))
			add(txtfldHoldLimit)
		}
		panelHold.add(pHoldLimit)

		// ドロップタブ --------------------------------------------------
		val panelDrop = JPanel()
		panelDrop.layout = BoxLayout(panelDrop, BoxLayout.Y_AXIS)
		tabPane.addTab(getUIText("TabName_Drop"), panelDrop)

		// Hard drop使用可否
		chkboxDropHardDropEnable.text = getUIText("Drop_HardDropEnable")
		panelDrop.add(chkboxDropHardDropEnable)

		// Hard dropで即固定
		chkboxDropHardDropLock.text = getUIText("Drop_HardDropLock")
		panelDrop.add(chkboxDropHardDropLock)

		// Hard drop連続使用不可
		chkboxDropHardDropLimit.text = getUIText("Drop_HardDropLimit")
		panelDrop.add(chkboxDropHardDropLimit)

		// Soft drop使用可否
		chkboxDropSoftDropEnable.text = getUIText("Drop_SoftDropEnable")
		panelDrop.add(chkboxDropSoftDropEnable)

		// Soft dropで即固定
		chkboxDropSoftDropLock.text = getUIText("Drop_SoftDropLock")
		panelDrop.add(chkboxDropSoftDropLock)

		// Soft drop連続使用不可
		chkboxDropSoftDropLimit.text = getUIText("Drop_SoftDropLimit")
		panelDrop.add(chkboxDropSoftDropLimit)

		// 接地状態でSoft dropすると即固定
		chkboxDropSoftDropSurfaceLock.text = getUIText("Drop_SoftDropSurfaceLock")
		panelDrop.add(chkboxDropSoftDropSurfaceLock)

		// Soft drop速度をCurrent 通常速度×n倍にする
		chkboxDropSoftDropMultiplyNativeSpeed.text = getUIText("Drop_SoftDropMultiplyNativeSpeed")
		panelDrop.add(chkboxDropSoftDropMultiplyNativeSpeed)

		// Use new soft drop codes
		chkboxDropSoftDropGravitySpeedLimit.text = getUIText("Drop_SoftDropGravitySpeedLimit")
		panelDrop.add(chkboxDropSoftDropGravitySpeedLimit)

		// Soft drop速度
		val pDropSoftDropSpeed = JPanel()
		panelDrop.add(pDropSoftDropSpeed)
		pDropSoftDropSpeed.add(JLabel(getUIText("Drop_SoftDropSpeed")))

		txtfldDropSoftDropSpeed.columns = (5)
		pDropSoftDropSpeed.add(txtfldDropSoftDropSpeed)

		// rotationタブ --------------------------------------------------
		val panelRotate = JPanel()
		panelRotate.layout = BoxLayout(panelRotate, BoxLayout.Y_AXIS)
		tabPane.addTab(getUIText("TabName_Rotate"), panelRotate)

		// 先行rotation
		chkboxRotateInitial.text = (getUIText("Rotate_RotateInitial"))
		panelRotate.add(chkboxRotateInitial)

		// 先行rotation連続使用不可
		chkboxRotateInitialLimit.text = (getUIText("Rotate_RotateInitialLimit"))
		panelRotate.add(chkboxRotateInitialLimit)

		// Wallkick
		chkboxRotateWallkick.text = (getUIText("Rotate_RotateWallkick"))
		panelRotate.add(chkboxRotateWallkick)

		// 先行rotationでもWallkickする
		chkboxRotateInitialWallkick.text = (getUIText("Rotate_RotateInitialWallkick"))
		panelRotate.add(chkboxRotateInitialWallkick)

		// Aで右rotation
		chkboxRotateButtonDefaultRight.text = (getUIText("Rotate_RotateButtonDefaultRight"))
		panelRotate.add(chkboxRotateButtonDefaultRight)

		// 逆rotation許可
		chkboxRotateButtonAllowReverse.text = (getUIText("Rotate_RotateButtonAllowReverse"))
		panelRotate.add(chkboxRotateButtonAllowReverse)

		// 2rotation許可
		chkboxRotateButtonAllowDouble.text = (getUIText("Rotate_RotateButtonAllowDouble"))
		panelRotate.add(chkboxRotateButtonAllowDouble)

		// UpDirectionへWallkickできる count
		val pRotateMaxUpwardWallkick = JPanel()
		panelRotate.add(pRotateMaxUpwardWallkick)
		pRotateMaxUpwardWallkick.add(JLabel(getUIText("Rotate_RotateMaxUpwardWallkick")))

		txtfldRotateMaxUpwardWallkick.columns = (5)
		pRotateMaxUpwardWallkick.add(txtfldRotateMaxUpwardWallkick)

		// Wallkickアルゴリズム
		val pWallkickSystem = JPanel()
		panelRotate.add(pWallkickSystem)

		pWallkickSystem.add(JLabel(getUIText("Rotate_WallkickSystem")))

		vectorWallkickSystem = getTextFileVector("config/list/wallkick.lst")
		comboboxWallkickSystem = JComboBox(createShortStringVector(vectorWallkickSystem)).apply {
			preferredSize = Dimension(200, 30)
			pWallkickSystem.add(this)
		}

		btnResetWallkickSystem = JButton(getUIText("Rotate_ResetWallkickSystem")).also {
			it.setMnemonic('R')
			it.actionCommand = "ResetWallkickSystem"
			it.addActionListener(this)
			pWallkickSystem.add(it)
		}

		// 固定 timeタブ --------------------------------------------------
		val panelLockDelay = JPanel()
		panelLockDelay.layout = BoxLayout(panelLockDelay, BoxLayout.Y_AXIS)
		tabPane.addTab(getUIText("TabName_LockDelay"), panelLockDelay)

		// 最低固定 timeと最高固定 time
		panelLockDelay.add(JLabel(getUIText("LockDelay_LockDelayMinMax")))

		val pLockDelayMinMax = JPanel()
		panelLockDelay.add(pLockDelayMinMax)

		txtfldLockDelayMin.columns = 5
		pLockDelayMinMax.add(txtfldLockDelayMin)
		txtfldLockDelayMax.columns = 5
		pLockDelayMinMax.add(txtfldLockDelayMax)

		// 落下で固定 timeリセット
		chkboxLockDelayLockResetFall.text = (getUIText("LockDelay_LockResetFall"))
		panelLockDelay.add(chkboxLockDelayLockResetFall)

		// 移動で固定 timeリセット
		chkboxLockDelayLockResetMove.text = (getUIText("LockDelay_LockResetMove"))
		panelLockDelay.add(chkboxLockDelayLockResetMove)

		// rotationで固定 timeリセット
		chkboxLockDelayLockResetRotate.text = (getUIText("LockDelay_LockResetRotate"))
		panelLockDelay.add(chkboxLockDelayLockResetRotate)

		// Lock delay reset by wallkick
		chkboxLockDelayLockResetWallkick.text = (getUIText("LockDelay_LockResetWallkick"))
		panelLockDelay.add(chkboxLockDelayLockResetWallkick)

		// 横移動 counterとrotation counterを共有 (横移動 counterだけ使う）
		chkboxLockDelayLockResetLimitShareCount.text = (getUIText("LockDelay_LockDelayLockResetLimitShareCount"))
		panelLockDelay.add(chkboxLockDelayLockResetLimitShareCount)

		// 横移動 count制限
		val pLockDelayLockResetLimitMove = JPanel()
		panelLockDelay.add(pLockDelayLockResetLimitMove)
		pLockDelayLockResetLimitMove.add(JLabel(getUIText("LockDelay_LockDelayLockResetLimitMove")))

		txtfldLockDelayLockResetLimitMove.columns = 5
		pLockDelayLockResetLimitMove.add(txtfldLockDelayLockResetLimitMove)

		// rotation count制限
		val pLockDelayLockResetLimitRotate = JPanel()
		panelLockDelay.add(pLockDelayLockResetLimitRotate)

		pLockDelayLockResetLimitRotate.add(JLabel(getUIText("LockDelay_LockDelayLockResetLimitRotate")))

		txtfldLockDelayLockResetLimitRotate.columns = 5
		pLockDelayLockResetLimitRotate.add(txtfldLockDelayLockResetLimitRotate)

		// 移動またはrotation count制限が超過した時の設定
		val pLockDelayLockResetLimitOver = JPanel()
		pLockDelayLockResetLimitOver.layout = BoxLayout(pLockDelayLockResetLimitOver, BoxLayout.Y_AXIS)
		panelLockDelay.add(pLockDelayLockResetLimitOver)

		pLockDelayLockResetLimitOver.add(JLabel(getUIText("LockDelay_LockDelayLockResetLimitOver")))

		val gLockDelayLockResetLimitOver = ButtonGroup()

		radioLockDelayLockResetLimitOverNoReset = JRadioButton(getUIText("LockDelay_LockDelayLockResetLimitOverNoReset"))
		pLockDelayLockResetLimitOver.add(radioLockDelayLockResetLimitOverNoReset)
		gLockDelayLockResetLimitOver.add(radioLockDelayLockResetLimitOverNoReset)

		radioLockDelayLockResetLimitOverInstant = JRadioButton(getUIText("LockDelay_LockDelayLockResetLimitOverInstant"))
		pLockDelayLockResetLimitOver.add(radioLockDelayLockResetLimitOverInstant)
		gLockDelayLockResetLimitOver.add(radioLockDelayLockResetLimitOverInstant)

		radioLockDelayLockResetLimitOverNoWallkick = JRadioButton(getUIText("LockDelay_LockDelayLockResetLimitOverNoWallkick"))
		pLockDelayLockResetLimitOver.add(radioLockDelayLockResetLimitOverNoWallkick)
		gLockDelayLockResetLimitOver.add(radioLockDelayLockResetLimitOverNoWallkick)

		// AREタブ --------------------------------------------------
		val panelARE = JPanel()
		panelARE.layout = BoxLayout(panelARE, BoxLayout.Y_AXIS)
		tabPane.addTab(getUIText("TabName_ARE"), panelARE)

		// 最低AREと最高ARE
		panelARE.add(JLabel(getUIText("ARE_MinMax")))

		val pAREMinMax = JPanel()
		panelARE.add(pAREMinMax)

		txtfldAREMin.columns = 5
		pAREMinMax.add(txtfldAREMin)
		txtfldAREMax.columns = 5
		pAREMinMax.add(txtfldAREMax)

		// 最低ARE after line clearと最高ARE after line clear
		panelARE.add(JLabel(getUIText("ARE_LineMinMax")))

		val pARELineMinMax = JPanel()
		panelARE.add(pARELineMinMax)

		txtfldARELineMin.columns = 5
		pARELineMinMax.add(txtfldARELineMin)
		txtfldARELineMax.columns = 5
		pARELineMinMax.add(txtfldARELineMax)

		// 固定した瞬間に光る frame count
		panelARE.add(JLabel(getUIText("ARE_LockFlash")))

		val pARELockFlash = JPanel()
		panelARE.add(pARELockFlash)

		txtfldARELockFlash.columns = 5
		pARELockFlash.add(txtfldARELockFlash)

		// Blockが光る専用 frame を入れる
		chkboxARELockFlashOnlyFrame.text = (getUIText("ARE_LockFlashOnlyFrame"))
		panelARE.add(chkboxARELockFlashOnlyFrame)

		// Line clear前にBlockが光る frame を入れる
		chkboxARELockFlashBeforeLineClear.text = (getUIText("ARE_LockFlashBeforeLineClear"))
		panelARE.add(chkboxARELockFlashBeforeLineClear)

		// ARE cancel on move
		chkboxARECancelMove.text = (getUIText("ARE_CancelMove"))
		panelARE.add(chkboxARECancelMove)

		// ARE cancel on move
		chkboxARECancelRotate.text = (getUIText("ARE_CancelRotate"))
		panelARE.add(chkboxARECancelRotate)

		// ARE cancel on move
		chkboxARECancelHold.text = (getUIText("ARE_CancelHold"))
		panelARE.add(chkboxARECancelHold)

		// Line clearタブ --------------------------------------------------
		val panelLine = JPanel()
		panelLine.layout = BoxLayout(panelLine, BoxLayout.Y_AXIS)
		tabPane.addTab(getUIText("TabName_Line"), panelLine)

		// 最低Line clear timeと最高Line clear time
		panelLine.add(JLabel(getUIText("Line_MinMax")))

		val pLineMinMax = JPanel()
		panelLine.add(pLineMinMax)

		txtfldLineDelayMin.columns = (5)
		pLineMinMax.add(txtfldLineDelayMin)
		txtfldLineDelayMax.columns = (5)
		pLineMinMax.add(txtfldLineDelayMax)

		// 落下アニメ
		chkboxLineFallAnim.text = (getUIText("Line_FallAnim"))
		panelLine.add(chkboxLineFallAnim)

		// Line delay cancel on move
		chkboxLineCancelMove.text = (getUIText("Line_CancelMove"))
		panelLine.add(chkboxLineCancelMove)

		// Line delay cancel on rotate
		chkboxLineCancelRotate.text = (getUIText("Line_CancelRotate"))
		panelLine.add(chkboxLineCancelRotate)

		// Line delay cancel on hold
		chkboxLineCancelHold.text = (getUIText("Line_CancelHold"))
		panelLine.add(chkboxLineCancelHold)

		// 移動タブ --------------------------------------------------
		val panelMove = JPanel()
		panelMove.layout = BoxLayout(panelMove, BoxLayout.Y_AXIS)
		tabPane.addTab(getUIText("TabName_Move"), panelMove)

		// 最低横溜め timeと最高横溜め time
		panelMove.add(JLabel(getUIText("Move_DASMinMax")))

		val pMoveDASMinMax = JPanel()
		panelMove.add(pMoveDASMinMax)

		txtfldMoveDASMin.columns = (5)
		pMoveDASMinMax.add(txtfldMoveDASMin)
		txtfldMoveDASMax.columns = (5)
		pMoveDASMinMax.add(txtfldMoveDASMax)

		// 横移動間隔
		val pMoveDASDelay = JPanel()
		panelMove.add(pMoveDASDelay)

		pMoveDASDelay.add(JLabel(getUIText("Move_DASDelay1")))

		txtfldMoveDASDelay.columns = (5)
		pMoveDASDelay.add(txtfldMoveDASDelay)

		pMoveDASDelay.add(JLabel(getUIText("Move_DASDelay2")))

		// ○○のとき横溜め可能
		chkboxMoveDASInReady.text = (getUIText("Move_DASInReady"))
		panelMove.add(chkboxMoveDASInReady)
		chkboxMoveDASInMoveFirstFrame.text = (getUIText("Move_DASInMoveFirstFrame"))
		panelMove.add(chkboxMoveDASInMoveFirstFrame)
		chkboxMoveDASInLockFlash.text = (getUIText("Move_DASInLockFlash"))
		panelMove.add(chkboxMoveDASInLockFlash)
		chkboxMoveDASInLineClear.text = (getUIText("Move_DASInLineClear"))
		panelMove.add(chkboxMoveDASInLineClear)
		chkboxMoveDASInARE.text = (getUIText("Move_DASInARE"))
		panelMove.add(chkboxMoveDASInARE)
		chkboxMoveDASInARELastFrame.text = (getUIText("Move_DASInARELastFrame"))
		panelMove.add(chkboxMoveDASInARELastFrame)
		chkboxMoveDASInEndingStart.text = (getUIText("Move_DASInEndingStart"))
		panelMove.add(chkboxMoveDASInEndingStart)
		chkboxMoveDASChargeOnBlockedMove.text = (getUIText("Move_DASChargeOnBlockedMove"))
		panelMove.add(chkboxMoveDASChargeOnBlockedMove)
		chkboxMoveDASStoreChargeOnNeutral.text = (getUIText("Move_DASStoreChargeOnNeutral"))
		panelMove.add(chkboxMoveDASStoreChargeOnNeutral)
		chkboxMoveDASRedirectInDelay.text = (getUIText("Move_DASRedirectInDelay"))
		panelMove.add(chkboxMoveDASRedirectInDelay)

		// 最初の frame に移動可能
		chkboxMoveFirstFrame.text = (getUIText("Move_FirstFrame"))
		panelMove.add(chkboxMoveFirstFrame)

		// 斜め移動
		chkboxMoveDiagonal.text = (getUIText("Move_Diagonal"))
		panelMove.add(chkboxMoveDiagonal)

		// Up下同時押し
		chkboxMoveUpAndDown.text = (getUIText("Move_UpAndDown"))
		panelMove.add(chkboxMoveUpAndDown)

		// 左右同時押し
		chkboxMoveLeftAndRightAllow.text = (getUIText("Move_LeftAndRightAllow"))
		panelMove.add(chkboxMoveLeftAndRightAllow)

		// 左右同時押ししたときに前 frame の input を優先
		chkboxMoveLeftAndRightUsePreviousInput.text = (getUIText("Move_LeftAndRightUsePreviousInput"))
		panelMove.add(chkboxMoveLeftAndRightUsePreviousInput)

		// Shift lock
		chkboxMoveShiftLockEnable.text = (getUIText("Move_ShiftLock"))
		panelMove.add(chkboxMoveShiftLockEnable)

		// rotationパターン補正タブ ------------------------------------------------
		val panelPieceOffset = JPanel()
		panelPieceOffset.layout = BoxLayout(panelPieceOffset, BoxLayout.Y_AXIS)
		tabPane.addTab(getUIText("TabName_PieceOffset"), panelPieceOffset)
		comboboxPieceOffset = JComboBox(RuleOptions.PIECEOFFSET_NAME.map {getUIText(it)}.toTypedArray()).apply {
			actionCommand = "OffsetPreset"
		}
		comboboxPieceOffset?.addActionListener(this)
		panelPieceOffset.add(comboboxPieceOffset)

		tabPieceOffset = JTabbedPane()
		panelPieceOffset.add(tabPieceOffset)

		// rotationパターン補正(X)タブ --------------------------------------------------
		val panelPieceOffsetX = JPanel()
		panelPieceOffsetX.layout = BoxLayout(panelPieceOffsetX, BoxLayout.Y_AXIS)
		tabPieceOffset?.addTab(getUIText("TabName_PieceOffsetX"), panelPieceOffsetX)

		val pPieceOffsetX = Array(Piece.PIECE_COUNT) {
			JPanel().apply {
				panelPieceOffsetX.add(this)
				add(JLabel(getUIText("PieceName$it")))
			}
		}

		txtfldPieceOffsetX = Array(Piece.PIECE_COUNT) {i ->
			Array(Piece.DIRECTION_COUNT) {
				JTextField("", 5).apply {pPieceOffsetX[i].add(this)}
			}
		}
		// rotationパターン補正(Y)タブ --------------------------------------------------
		val panelPieceOffsetY = JPanel().apply {
			layout = BoxLayout(this, BoxLayout.Y_AXIS)
			tabPieceOffset?.addTab(getUIText("TabName_PieceOffsetY"), this)
		}

		val pPieceOffsetY = Array(Piece.PIECE_COUNT) {
			JPanel().apply {
				panelPieceOffsetY.add(this)
				add(JLabel(getUIText("PieceName$it")))
			}
		}

		txtfldPieceOffsetY = Array(Piece.PIECE_COUNT) {i ->
			Array(Piece.DIRECTION_COUNT) {
				JTextField("", 5).apply {
					pPieceOffsetY[i].add(this)
				}
			}
		}

		// 出現位置補正タブ ------------------------------------------------
		val panelPieceSpawn = JPanel().apply {
			layout = BoxLayout(this, BoxLayout.Y_AXIS)
			tabPane.addTab(getUIText("TabName_PieceSpawn"), this)
		}

		tabPieceSpawn = JTabbedPane()
		panelPieceSpawn.add(tabPieceSpawn)

		// 出現位置補正(X)タブ --------------------------------------------------
		val panelPieceSpawnX = JPanel()
		panelPieceSpawnX.layout = BoxLayout(panelPieceSpawnX, BoxLayout.Y_AXIS)
		tabPieceSpawn?.addTab(getUIText("TabName_PieceSpawnX"), panelPieceSpawnX)

		val pPieceSpawnX = Array(Piece.PIECE_COUNT) {
			JPanel().apply {
				add(JLabel(getUIText("PieceName$it")))
			}
		}

		txtfldPieceSpawnX = Array(Piece.PIECE_COUNT) {i ->
			panelPieceSpawnX.add(pPieceSpawnX[i])
			Array(Piece.DIRECTION_COUNT) {
				JTextField("", 5).apply {
					pPieceSpawnX[i].add(this)
				}
			}
		}

		// 出現位置補正(Y)タブ --------------------------------------------------
		val panelPieceSpawnY = JPanel().apply {
			layout = BoxLayout(this, BoxLayout.Y_AXIS)
			tabPieceSpawn?.addTab(getUIText("TabName_PieceSpawnY"), this)
		}

		val pPieceSpawnY = Array(Piece.PIECE_COUNT) {
			JPanel().apply {
				panelPieceSpawnY.add(this)
				add(JLabel(getUIText("PieceName$it")))
			}
		}

		txtfldPieceSpawnY = Array(Piece.PIECE_COUNT) {i ->
			Array(Piece.DIRECTION_COUNT) {
				JTextField("", 5).apply {
					pPieceSpawnY[i].add(this)
				}
			}
		}

		// Big時出現位置補正(X)タブ --------------------------------------------------
		val panelPieceSpawnBigX = JPanel().apply {
			layout = BoxLayout(this, BoxLayout.Y_AXIS)
			tabPieceSpawn?.addTab(getUIText("TabName_PieceSpawnBigX"), this)
		}

		val pPieceSpawnBigX = Array(Piece.PIECE_COUNT) {
			JPanel().apply {
				panelPieceSpawnBigX.add(this)
				add(JLabel(getUIText("PieceName$it")))
			}
		}

		txtfldPieceSpawnBigX = Array(Piece.PIECE_COUNT) {i ->
			Array(Piece.DIRECTION_COUNT) {
				JTextField("", 5).apply {pPieceSpawnBigX[i].add(this)}
			}
		}
		// Big時出現位置補正(Y)タブ --------------------------------------------------
		val panelPieceSpawnBigY = JPanel().apply {
			layout = BoxLayout(this, BoxLayout.Y_AXIS)
			tabPieceSpawn?.addTab(getUIText("TabName_PieceSpawnBigY"), this)
		}

		val pPieceSpawnBigY = Array(Piece.PIECE_COUNT) {
			JPanel().apply {
				panelPieceSpawnBigY.add(this)
				add(JLabel(getUIText("PieceName$it")))
			}
		}

		txtfldPieceSpawnBigY = Array(Piece.PIECE_COUNT) {i ->
			Array(Piece.DIRECTION_COUNT) {
				JTextField("", 5).apply {pPieceSpawnBigY[i].add(this)}
			}
		}

		// 色設定タブ --------------------------------------------------
		val panelPieceColor = JPanel().apply {
			layout = BoxLayout(this, BoxLayout.X_AXIS)
			tabPane.addTab(getUIText("TabName_PieceColor"), this)
		}

		val strColorNames = Array(Block.COLOR.COUNT-1) {getUIText("ColorName$it")}

		val pColorRow = Array(2) {
			JPanel().apply {
				layout = BoxLayout(this, BoxLayout.Y_AXIS)
				panelPieceColor.add(this)
			}

		}
		val bResetColor = arrayOf(JButton(getUIText("Basic_Reset")+" SRS").apply {
			setMnemonic('S')
			actionCommand = "PresetColors_SRS"
		}, JButton("${getUIText("Basic_Reset")} ARS").apply {
			setMnemonic('A')
			actionCommand = "PresetColors_ARS"
		})
		bResetColor.forEach {
			it.addActionListener(this)
			pColorRow[1].add(it)
		}

		val pPieceColor = Array(Piece.PIECE_COUNT) {
			JPanel().apply {
				pColorRow[0].add(this)
				add(JLabel(getUIText("PieceName$it")))
			}
		}
		comboboxPieceColor = Array(Piece.PIECE_COUNT) {i ->
			JComboBox(strColorNames).apply {
				preferredSize = Dimension(100, 30)
				maximumRowCount = strColorNames.size
				pPieceColor[i].add(this)
			}
		}

		// 初期Direction設定タブ --------------------------------------------------
		val panelPieceDirection = JPanel()
		panelPieceDirection.layout = BoxLayout(panelPieceDirection, BoxLayout.X_AXIS)
		tabPane.addTab(getUIText("TabName_PieceDirection"), panelPieceDirection)

		val pDirectRow = Array(2) {
			JPanel().apply {
				this@apply.layout = BoxLayout(this, BoxLayout.Y_AXIS)
				panelPieceDirection.add(this)
			}
		}
		val bResetDirect = arrayOf(JButton("${getUIText("Basic_Reset")} SRS").apply {
			setMnemonic('S')
			actionCommand = "ResetDirection_SRS"
		}, JButton("${getUIText("Basic_Reset")} ARS").apply {
			setMnemonic('A')
			actionCommand = "ResetDirection_ARS"
		})
		bResetDirect.forEach {
			it.addActionListener(this)
			pDirectRow[1].add(it)
		}

		val strDirectionNames = Array(Piece.DIRECTION_COUNT+1) {
			getUIText("DirectionName$it")
		}

		val pPieceDirection = Array(Piece.PIECE_COUNT) {
			JPanel().apply {
				pDirectRow[0].add(this)
				add(JLabel(getUIText("PieceName$it")))
			}
		}

		comboboxPieceDirection = Array(Piece.PIECE_COUNT) {
			JComboBox(strDirectionNames).apply {
				preferredSize = Dimension(150, 30)
				maximumRowCount = strDirectionNames.size
				pPieceDirection[it].add(this)
			}
		}

	}

	/** Block画像を読み込み */
	private fun loadBlockSkins() {
		val skindir = propConfig.getProperty("custom.skin.directory", "res")

		var numBlocks = 0
		while(File("$skindir/graphics/blockskin/normal/n$numBlocks.png").canRead()) numBlocks++
		log.debug("$numBlocks block skins found")

		imgBlockSkins = Array(numBlocks) {i ->
			val imgBlock = loadImage(getURL("$skindir/graphics/blockskin/normal/n$i.png"))
			val isSticky = imgBlock!=null&&imgBlock.width>=400&&imgBlock.height>=304

			BufferedImage(144, 16, BufferedImage.TYPE_INT_RGB).apply {

				if(isSticky) for(j in 0..8)
					graphics.drawImage(imgBlock, j*16, 0, j*16+16, 16, 0, j*16, 16, j*16+16, null)
				else
					graphics.drawImage(imgBlock, 0, 0, 144, 16, 0, 0, 144, 16, null)
			}
		}
	}

	/** 画像を読み込み
	 * @param url 画像ファイルのURL
	 * @return 画像ファイル (失敗するとnull）
	 */
	private fun loadImage(url:URL?):BufferedImage? {
		var img:BufferedImage? = null
		try {
			img = ImageIO.read(url!!)
			log.debug("Loaded image from $url")
		} catch(e:IOException) {
			log.error("Failed to load image from ${url ?: ""}", e)
		}

		return img
	}

	/** リソースファイルのURLを返す
	 * @param str Filename
	 * @return リソースファイルのURL
	 */
	private fun getURL(str:String):URL? {
		val url:URL

		try {
			val sep = File.separator[0]
			var file = str.replace(sep, '/')

			// 参考：http://www.asahi-net.or.jp/~DP8T-ASM/java/tips/HowToMakeURL.html
			if(file[0]!='/') {
				var dir = System.getProperty("user.dir")
				dir = dir.replace(sep, '/')+'/'
				if(dir[0]!='/') dir = "/$dir"
				file = dir+file
			}
			url = URL("file", "", file)
		} catch(e:MalformedURLException) {
			log.warn("Invalid URL:$str", e)
			return null
		}

		return url
	}

	/** テキストファイルを読み込んでVector&lt;String&gt;に入れる
	 * @param filename Filename
	 * @return テキストファイルを読み込んだVector&lt;String&gt;
	 */
	private fun getTextFileVector(filename:String):Vector<String> {
		val vec = Vector<String>()

		try {
			val `in` = BufferedReader(FileReader(filename))

			while(true) {
				val str = `in`.readLine()
				if(str==null||str.isEmpty()) break
				vec.add(str)
			}
		} catch(e:IOException) {
		}

		return vec
	}

	/** 特定のVector&lt;String&gt;の最後のドット記号から先だけを取り出したVector&lt;String&gt;を作成
	 * @param vecSrc 元のVector&lt;String&gt;
	 * @return 加工したVector&lt;String&gt;
	 */
	private fun createShortStringVector(vecSrc:Vector<String>?):Vector<String> {
		val vec = Vector<String>()

		for(str in vecSrc!!) {
			val last = str.lastIndexOf('.')

			val newStr:String = if(last!=-1) str.substring(last+1) else str

			vec.add(newStr)
		}

		return vec
	}

	/** ルール設定をUIに反映させる
	 * @param r ルール設定
	 */
	private fun readRuleToUI(r:RuleOptions) {
		txtfldRuleName.text = r.strRuleName
		txtfldNextDisplay.text = r.nextDisplay.toString()
		comboboxStyle?.selectedIndex = r.style
		comboboxSkin?.selectedIndex = r.skin
		chkboxGhost.isSelected = r.ghost
		chkboxEnterAboveField.isSelected = r.pieceEnterAboveField
		txtfldEnterMaxDistanceY.text = r.pieceEnterMaxDistanceY.toString()
		comboboxRandomizer?.selectedIndex = vectorRandomizer?.indexOf(r.strRandomizer) ?: 0

		txtfldFieldWidth.text = r.fieldWidth.toString()
		txtfldFieldHeight.text = r.fieldHeight.toString()
		txtfldFieldHiddenHeight.text = r.fieldHiddenHeight.toString()
		chkboxFieldCeiling.isSelected = r.fieldCeiling
		chkboxFieldLockoutDeath.isSelected = r.fieldLockoutDeath
		chkboxFieldPartialLockoutDeath.isSelected = r.fieldPartialLockoutDeath

		chkboxHoldEnable.isSelected = r.holdEnable
		chkboxHoldInitial.isSelected = r.holdInitial
		chkboxHoldInitialLimit.isSelected = r.holdInitialLimit
		chkboxHoldResetDirection.isSelected = r.holdResetDirection
		txtfldHoldLimit.text = r.holdLimit.toString()

		chkboxDropHardDropEnable.isSelected = r.harddropEnable
		chkboxDropHardDropLock.isSelected = r.harddropLock
		chkboxDropHardDropLimit.isSelected = r.harddropLimit
		chkboxDropSoftDropEnable.isSelected = r.softdropEnable
		chkboxDropSoftDropLock.isSelected = r.softdropLock
		chkboxDropSoftDropLimit.isSelected = r.softdropLimit
		chkboxDropSoftDropSurfaceLock.isSelected = r.softdropSurfaceLock
		txtfldDropSoftDropSpeed.text = r.softdropSpeed.toString()
		chkboxDropSoftDropMultiplyNativeSpeed.isSelected = r.softdropMultiplyNativeSpeed
		chkboxDropSoftDropGravitySpeedLimit.isSelected = r.softdropGravitySpeedLimit

		chkboxRotateInitial.isSelected = r.rotateInitial
		chkboxRotateInitialLimit.isSelected = r.rotateInitialLimit
		chkboxRotateWallkick.isSelected = r.rotateWallkick
		chkboxRotateInitialWallkick.isSelected = r.rotateInitialWallkick
		txtfldRotateMaxUpwardWallkick.text = r.rotateMaxUpwardWallkick.toString()
		chkboxRotateButtonDefaultRight.isSelected = r.rotateButtonDefaultRight
		chkboxRotateButtonAllowReverse.isSelected = r.rotateButtonAllowReverse
		chkboxRotateButtonAllowDouble.isSelected = r.rotateButtonAllowDouble
		comboboxWallkickSystem?.selectedIndex = vectorWallkickSystem?.indexOf(r.strWallkick) ?: 0
		txtfldLockDelayMin.text = r.minLockDelay.toString()
		txtfldLockDelayMax.text = r.maxLockDelay.toString()
		chkboxLockDelayLockResetFall.isSelected = r.lockresetFall
		chkboxLockDelayLockResetMove.isSelected = r.lockresetMove
		chkboxLockDelayLockResetRotate.isSelected = r.lockresetRotate
		chkboxLockDelayLockResetWallkick.isSelected = r.lockresetWallkick
		chkboxLockDelayLockResetLimitShareCount.isSelected = r.lockresetLimitShareCount
		txtfldLockDelayLockResetLimitMove.text = r.lockresetLimitMove.toString()
		txtfldLockDelayLockResetLimitRotate.text = r.lockresetLimitRotate.toString()
		when(r.lockresetLimitOver) {
			RuleOptions.LOCKRESET_LIMIT_OVER_NORESET -> radioLockDelayLockResetLimitOverNoReset?.isSelected = true
			RuleOptions.LOCKRESET_LIMIT_OVER_INSTANT -> radioLockDelayLockResetLimitOverInstant?.isSelected = true
			RuleOptions.LOCKRESET_LIMIT_OVER_NOWALLKICK -> radioLockDelayLockResetLimitOverNoWallkick?.isSelected = true
		}

		txtfldAREMin.text = r.minARE.toString()
		txtfldAREMax.text = r.maxARE.toString()
		txtfldARELineMin.text = r.minARELine.toString()
		txtfldARELineMax.text = r.maxARELine.toString()
		txtfldARELockFlash.text = r.lockflash.toString()
		chkboxARELockFlashOnlyFrame.isSelected = r.lockflashOnlyFrame
		chkboxARELockFlashBeforeLineClear.isSelected = r.lockflashBeforeLineClear
		chkboxARECancelMove.isSelected = r.areCancelMove
		chkboxARECancelRotate.isSelected = r.areCancelRotate
		chkboxARECancelHold.isSelected = r.areCancelHold

		txtfldLineDelayMin.text = r.minLineDelay.toString()
		txtfldLineDelayMax.text = r.maxLineDelay.toString()
		chkboxLineFallAnim.isSelected = r.lineFallAnim
		chkboxLineCancelMove.isSelected = r.lineCancelMove
		chkboxLineCancelRotate.isSelected = r.lineCancelRotate
		chkboxLineCancelHold.isSelected = r.lineCancelHold

		txtfldMoveDASMin.text = r.minDAS.toString()
		txtfldMoveDASMax.text = r.maxDAS.toString()
		txtfldMoveDASDelay.text = r.dasARR.toString()
		chkboxMoveDASInReady.isSelected = r.dasInReady
		chkboxMoveDASInMoveFirstFrame.isSelected = r.dasInMoveFirstFrame
		chkboxMoveDASInLockFlash.isSelected = r.dasInLockFlash
		chkboxMoveDASInLineClear.isSelected = r.dasInLineClear
		chkboxMoveDASInARE.isSelected = r.dasInARE
		chkboxMoveDASInARELastFrame.isSelected = r.dasInARELastFrame
		chkboxMoveDASInEndingStart.isSelected = r.dasInEndingStart
		chkboxMoveDASChargeOnBlockedMove.isSelected = r.dasChargeOnBlockedMove
		chkboxMoveDASStoreChargeOnNeutral.isSelected = r.dasStoreChargeOnNeutral
		chkboxMoveDASRedirectInDelay.isSelected = r.dasRedirectInDelay
		chkboxMoveFirstFrame.isSelected = r.moveFirstFrame
		chkboxMoveDiagonal.isSelected = r.moveDiagonal
		chkboxMoveUpAndDown.isSelected = r.moveUpAndDown
		chkboxMoveLeftAndRightAllow.isSelected = r.moveLeftAndRightAllow
		chkboxMoveLeftAndRightUsePreviousInput.isSelected = r.moveLeftAndRightUsePreviousInput
		chkboxMoveShiftLockEnable.isSelected = r.shiftLockEnable
		comboboxPieceOffset?.selectedIndex = r.pieceOffset
		for(i in 0 until Piece.PIECE_COUNT) {
			for(j in 0 until Piece.DIRECTION_COUNT) {
				txtfldPieceOffsetX?.let {it[i][j].text = "${r.pieceOffsetX[i][j]}"}
				txtfldPieceOffsetY?.let {it[i][j].text = "${r.pieceOffsetY[i][j]}"}
				txtfldPieceSpawnX?.let {it[i][j].text = "${r.pieceSpawnX[i][j]}"}
				txtfldPieceSpawnY?.let {it[i][j].text = "${r.pieceSpawnY[i][j]}"}
				txtfldPieceSpawnBigX?.let {it[i][j].text = "${r.pieceSpawnXBig[i][j]}"}
				txtfldPieceSpawnBigY?.let {it[i][j].text = "${r.pieceSpawnYBig[i][j]}"}
			}
			comboboxPieceColor?.get(i)?.selectedIndex = r.pieceColor[i]-1
			comboboxPieceDirection?.get(i)?.selectedIndex = r.pieceDefaultDirection[i]
		}
	}

	/** ルール設定をUIから書き込む
	 * @param r ルール設定
	 */
	private fun writeRuleFromUI(r:RuleOptions) {
		r.strRuleName = txtfldRuleName.text.uppercase()
		r.nextDisplay = getIntTextField(txtfldNextDisplay)
		r.style = comboboxStyle!!.selectedIndex
		r.skin = comboboxSkin!!.selectedIndex
		r.ghost = chkboxGhost.isSelected
		r.pieceEnterAboveField = chkboxEnterAboveField.isSelected
		r.pieceEnterMaxDistanceY = getIntTextField(txtfldEnterMaxDistanceY)
		val indexRandomizer = comboboxRandomizer!!.selectedIndex
		if(indexRandomizer>=0)
			r.strRandomizer = vectorRandomizer!![indexRandomizer]
		else
			r.strRandomizer = ""

		r.fieldWidth = getIntTextField(txtfldFieldWidth)
		r.fieldHeight = getIntTextField(txtfldFieldHeight)
		r.fieldHiddenHeight = getIntTextField(txtfldFieldHiddenHeight)
		r.fieldCeiling = chkboxFieldCeiling.isSelected
		r.fieldLockoutDeath = chkboxFieldLockoutDeath.isSelected
		r.fieldPartialLockoutDeath = chkboxFieldPartialLockoutDeath.isSelected

		r.holdEnable = chkboxHoldEnable.isSelected
		r.holdInitial = chkboxHoldInitial.isSelected
		r.holdInitialLimit = chkboxHoldInitialLimit.isSelected
		r.holdResetDirection = chkboxHoldResetDirection.isSelected
		r.holdLimit = getIntTextField(txtfldHoldLimit)

		r.harddropEnable = chkboxDropHardDropEnable.isSelected
		r.harddropLock = chkboxDropHardDropLock.isSelected
		r.harddropLimit = chkboxDropHardDropLimit.isSelected
		r.softdropEnable = chkboxDropSoftDropEnable.isSelected
		r.softdropLock = chkboxDropSoftDropLock.isSelected
		r.softdropLimit = chkboxDropSoftDropLimit.isSelected
		r.softdropSurfaceLock = chkboxDropSoftDropSurfaceLock.isSelected
		r.softdropSpeed = getFloatTextField(txtfldDropSoftDropSpeed)
		r.softdropMultiplyNativeSpeed = chkboxDropSoftDropMultiplyNativeSpeed.isSelected
		r.softdropGravitySpeedLimit = chkboxDropSoftDropGravitySpeedLimit.isSelected

		r.rotateInitial = chkboxRotateInitial.isSelected
		r.rotateInitialLimit = chkboxRotateInitialLimit.isSelected
		r.rotateWallkick = chkboxRotateWallkick.isSelected
		r.rotateInitialWallkick = chkboxRotateInitialWallkick.isSelected
		r.rotateMaxUpwardWallkick = getIntTextField(txtfldRotateMaxUpwardWallkick)
		r.rotateButtonDefaultRight = chkboxRotateButtonDefaultRight.isSelected
		r.rotateButtonAllowReverse = chkboxRotateButtonAllowReverse.isSelected
		r.rotateButtonAllowDouble = chkboxRotateButtonAllowDouble.isSelected
		val indexWallkick = comboboxWallkickSystem!!.selectedIndex
		if(indexWallkick>=0)
			r.strWallkick = vectorWallkickSystem!![indexWallkick]
		else
			r.strWallkick = ""

		r.minLockDelay = getIntTextField(txtfldLockDelayMin)
		r.maxLockDelay = getIntTextField(txtfldLockDelayMax)
		r.lockresetFall = chkboxLockDelayLockResetFall.isSelected
		r.lockresetMove = chkboxLockDelayLockResetMove.isSelected
		r.lockresetRotate = chkboxLockDelayLockResetRotate.isSelected
		r.lockresetWallkick = chkboxLockDelayLockResetWallkick.isSelected
		r.lockresetLimitShareCount = chkboxLockDelayLockResetLimitShareCount.isSelected
		r.lockresetLimitMove = getIntTextField(txtfldLockDelayLockResetLimitMove)
		r.lockresetLimitRotate = getIntTextField(txtfldLockDelayLockResetLimitRotate)
		if(radioLockDelayLockResetLimitOverNoReset!!.isSelected)
			r.lockresetLimitOver = RuleOptions.LOCKRESET_LIMIT_OVER_NORESET
		if(radioLockDelayLockResetLimitOverInstant!!.isSelected)
			r.lockresetLimitOver = RuleOptions.LOCKRESET_LIMIT_OVER_INSTANT
		if(radioLockDelayLockResetLimitOverNoWallkick!!.isSelected)
			r.lockresetLimitOver = RuleOptions.LOCKRESET_LIMIT_OVER_NOWALLKICK

		r.minARE = getIntTextField(txtfldAREMin)
		r.maxARE = getIntTextField(txtfldAREMax)
		r.minARELine = getIntTextField(txtfldARELineMin)
		r.maxARELine = getIntTextField(txtfldARELineMax)
		r.lockflash = getIntTextField(txtfldARELockFlash)
		r.lockflashOnlyFrame = chkboxARELockFlashOnlyFrame.isSelected
		r.lockflashBeforeLineClear = chkboxARELockFlashBeforeLineClear.isSelected
		r.areCancelMove = chkboxARECancelMove.isSelected
		r.areCancelRotate = chkboxARECancelRotate.isSelected
		r.areCancelHold = chkboxARECancelHold.isSelected

		r.minLineDelay = getIntTextField(txtfldLineDelayMin)
		r.maxLineDelay = getIntTextField(txtfldLineDelayMax)
		r.lineFallAnim = chkboxLineFallAnim.isSelected
		r.lineCancelMove = chkboxLineCancelMove.isSelected
		r.lineCancelRotate = chkboxLineCancelRotate.isSelected
		r.lineCancelHold = chkboxLineCancelHold.isSelected

		r.minDAS = getIntTextField(txtfldMoveDASMin)
		r.maxDAS = getIntTextField(txtfldMoveDASMax)
		r.dasARR = getIntTextField(txtfldMoveDASDelay)
		r.dasInReady = chkboxMoveDASInReady.isSelected
		r.dasInMoveFirstFrame = chkboxMoveDASInMoveFirstFrame.isSelected
		r.dasInLockFlash = chkboxMoveDASInLockFlash.isSelected
		r.dasInLineClear = chkboxMoveDASInLineClear.isSelected
		r.dasInARE = chkboxMoveDASInARE.isSelected
		r.dasInARELastFrame = chkboxMoveDASInARELastFrame.isSelected
		r.dasInEndingStart = chkboxMoveDASInEndingStart.isSelected
		r.dasChargeOnBlockedMove = chkboxMoveDASChargeOnBlockedMove.isSelected
		r.dasStoreChargeOnNeutral = chkboxMoveDASStoreChargeOnNeutral.isSelected
		r.dasRedirectInDelay = chkboxMoveDASRedirectInDelay.isSelected
		r.moveFirstFrame = chkboxMoveFirstFrame.isSelected
		r.moveDiagonal = chkboxMoveDiagonal.isSelected
		r.moveUpAndDown = chkboxMoveUpAndDown.isSelected
		r.moveLeftAndRightAllow = chkboxMoveLeftAndRightAllow.isSelected
		r.moveLeftAndRightUsePreviousInput = chkboxMoveLeftAndRightUsePreviousInput.isSelected
		r.shiftLockEnable = chkboxMoveShiftLockEnable.isSelected
		r.pieceOffset = comboboxPieceOffset!!.selectedIndex
		for(i in 0 until Piece.PIECE_COUNT) {
			for(j in 0 until Piece.DIRECTION_COUNT) {
				r.pieceOffsetX[i][j] = getIntTextField(txtfldPieceOffsetX!![i][j])
				r.pieceOffsetY[i][j] = getIntTextField(txtfldPieceOffsetY!![i][j])
				r.pieceSpawnX[i][j] = getIntTextField(txtfldPieceSpawnX!![i][j])
				r.pieceSpawnY[i][j] = getIntTextField(txtfldPieceSpawnY!![i][j])
				r.pieceSpawnXBig[i][j] = getIntTextField(txtfldPieceSpawnBigX!![i][j])
				r.pieceSpawnYBig[i][j] = getIntTextField(txtfldPieceSpawnBigY!![i][j])
			}
			r.pieceColor[i] = comboboxPieceColor!![i].selectedIndex+1
			r.pieceDefaultDirection[i] = comboboxPieceDirection!![i].selectedIndex
		}
	}

	/** ルールをファイルに保存
	 * @param filename Filename
	 * @throws IOException 保存に失敗したとき
	 */
	@Throws(IOException::class)
	fun save(filename:String) {
		val ruleOpt = RuleOptions()
		writeRuleFromUI(ruleOpt)

		val prop = CustomProperties()
		ruleOpt.writeProperty(prop, 0)

		val out = GZIPOutputStream(FileOutputStream(filename))
		prop.store(out, "NullpoMino RuleData")
		out.close()

		log.debug("Saved rule file to $filename")
	}

	/** ルールをファイルから読み込み
	 * @param filename Filename
	 * @return ルール data
	 * @throws IOException Failed to loadしたとき
	 */
	@Throws(IOException::class)
	fun load(filename:String):RuleOptions {
		val prop = CustomProperties()

		val `in` = GZIPInputStream(FileInputStream(filename))
		prop.load(`in`)
		`in`.close()

		val ruleOpt = RuleOptions()
		ruleOpt.readProperty(prop, 0, true)

		log.debug("Loaded rule file from $filename")

		return ruleOpt
	}

	/** 翻訳後のUIの文字列を取得
	 * @param str 文字列
	 * @return 翻訳後のUIの文字列 (無いならそのままstrを返す）
	 */
	fun getUIText(str:String):String = propLang.getProperty(str, propLangDefault.getProperty(str, str))

	/** テキストfieldからint型の値を取得
	 * @param txtfld テキストfield
	 * @return テキストfieldから値を取得できた場合はその値, 失敗したら0
	 */
	private fun getIntTextField(txtfld:JTextField):Int = txtfld.text.toIntOrNull() ?: 0

	/** テキストfieldからfloat型の値を取得
	 * @param txtfld テキストfield
	 * @return テキストfieldから値を取得できた場合はその値, 失敗したら0f
	 */
	private fun getFloatTextField(txtfld:JTextField):Float = txtfld.text.toFloatOrNull() ?: 0f

	/** アクション発生時の処理 */
	override fun actionPerformed(e:ActionEvent) {
		val a:String = e.actionCommand
		val b:Int
		val c:JFileChooser
		when(a) {
			"New" -> {
				// 新規作成
				strNowFile = null
				title = getUIText("Title_RuleEditor")
				readRuleToUI(RuleOptions())
			}
			"Open" -> {
				// 開く
				c = JFileChooser(System.getProperty("user.dir")+"/config/rule")
				c.fileFilter = FileFilterRUL()

				if(c.showOpenDialog(this)==JFileChooser.APPROVE_OPTION) {
					val file = c.selectedFile
					var ruleOpt = RuleOptions()

					strNowFile = file.path
					title = "${getUIText("Title_RuleEditor")}:$strNowFile"

					try {
						ruleOpt = load(file.path)
					} catch(e2:IOException) {
						log.error("Failed to load rule data from ${strNowFile!!}", e2)
						JOptionPane.showMessageDialog(this, "${getUIText("Message_FileLoadFailed")}\n$e2",
							getUIText("Title_FileLoadFailed"), JOptionPane.ERROR_MESSAGE)
						return
					}

					readRuleToUI(ruleOpt)
				}
			}
			"Save" -> {
				if(strNowFile!=null) { // Up書き保存
					try {
						save(strNowFile!!)
					} catch(e2:IOException) {
						log.error("Failed to save rule data to ${strNowFile!!}", e2)
						JOptionPane.showMessageDialog(this, "${getUIText("Message_FileSaveFailed")}\n$e2",
							getUIText("Title_FileSaveFailed"), JOptionPane.ERROR_MESSAGE)
					}

				} else {
					// Nameを付けて保存
					c = JFileChooser("${System.getProperty("user.dir")}/config/rule")
					c.fileFilter = FileFilterRUL()

					if(c.showSaveDialog(this)==JFileChooser.APPROVE_OPTION) {
						val file = c.selectedFile
						var filename = file.path
						if(!filename.endsWith(".rul")) filename = "$filename.rul"

						try {
							save(filename)
						} catch(e2:Exception) {
							log.error("Failed to save rule data to $filename", e2)
							JOptionPane.showMessageDialog(this, "${getUIText("Message_FileSaveFailed")}\n$e2",
								getUIText("Title_FileSaveFailed"), JOptionPane.ERROR_MESSAGE)
							return
						}

						strNowFile = filename
						title = "${getUIText("Title_RuleEditor")}:$strNowFile"
					}
				}
			}
			"Save", "SaveAs" -> {
				c = JFileChooser("${System.getProperty("user.dir")}/config/rule")
				c.fileFilter = FileFilterRUL()
				if(c.showSaveDialog(this)==JFileChooser.APPROVE_OPTION) {
					val file = c.selectedFile
					var filename = file.path
					if(!filename.endsWith(".rul")) filename = "$filename.rul"
					try {
						save(filename)
					} catch(e2:Exception) {
						log.error("Failed to save rule data to $filename", e2)
						JOptionPane.showMessageDialog(this, "${getUIText("Message_FileSaveFailed")}\n$e2",
							getUIText("Title_FileSaveFailed"), JOptionPane.ERROR_MESSAGE)
						return
					}

					strNowFile = filename
					title = "${getUIText("Title_RuleEditor")}:$strNowFile"
				}
			}
			"OffsetPreset" -> {
				val tog = comboboxPieceOffset!!.selectedIndex==RuleOptions.PIECEOFFSET_ASSIGN
				for(i in txtfldPieceOffsetX!!)
					for(j in i) j.isEditable = tog
				for(i in txtfldPieceOffsetY!!)
					for(j in i) j.isEditable = tog
				for(i in txtfldPieceSpawnX!!)
					for(j in i) j.isEditable = tog
				for(i in txtfldPieceSpawnY!!)
					for(j in i) j.isEditable = tog
				for(i in txtfldPieceSpawnBigX!!)
					for(j in i) j.isEditable = tog
				for(i in txtfldPieceSpawnBigY!!)
					for(j in i) j.isEditable = tog
			}
			"PresetColors_SRS", "PresetColors_ARS" -> {
				b = if(a=="PresetColors_SRS") 1 else 0
				for(i in comboboxPieceColor!!.indices)
					comboboxPieceColor!![i].selectedIndex = RuleOptions.PIECECOLOR_PRESET[b][i]
			}
			"ResetDirection_SRS", "ResetDirection_ARS" -> {
				var i = 0
				while(i<comboboxPieceDirection!!.size-1&&i<RuleOptions.PIECEDIRECTION_ARSPRESET.size-1) {
					comboboxPieceDirection!![i].selectedIndex = if(a=="ResetDirection_ARS")
						RuleOptions.PIECEDIRECTION_ARSPRESET[i]
					else 0
					i++
				}
			}
			"ResetRandomizer" // NEXT順生成アルゴリズムの選択リセット
			-> comboboxRandomizer!!.setSelectedItem(null)
			"Exit" // 終了
			-> dispose()
		}
	}

	/** ファイル選択画面のフィルタ */
	private inner class FileFilterRUL:FileFilter() {
		override fun accept(f:File):Boolean = if(f.isDirectory) true else f.name.endsWith(".rul")

		override fun getDescription():String = getUIText("FileChooser_RuleFile")
	}

	/** 画像表示Comboボックスの項目<br></br>
	 * [出典](http://www.javadrive.jp/tutorial/jcombobox/index20.html) */
	private class ComboLabel(
		var text:String = "",
		var icon:Icon? = null
	)

	/** 画像表示ComboボックスのListCellRenderer<br></br>
	 * [出典](http://www.javadrive.jp/tutorial/jcombobox/index20.html) */
	private inner class ComboLabelCellRenderer:JLabel(), ListCellRenderer<Any> {
		fun getListCellRendererComponent(list:JList<out Nothing>?, value:Nothing?, index:Int, isSelected:Boolean,
			cellHasFocus:Boolean):Component {
			TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
		}

		init {
			isOpaque = true
		}

		override fun getListCellRendererComponent(list:JList<*>, value:Any, index:Int, isSelected:Boolean,
			cellHasFocus:Boolean):Component {
			val data = value as ComboLabel
			text = data.text
			icon = data.icon

			if(isSelected) {
				foreground = Color.white
				background = Color.black
			} else {
				foreground = Color.black
				background = Color.white
			}

			return this
		}

	}

	companion object {
		/** Serial version */
		private const val serialVersionUID = 1L

		/** Log */
		internal val log = LogManager.getLogger()

		/** メイン関数
		 * @param args コマンドLines引数
		 */
		@JvmStatic
		fun main(args:Array<String>) {
			org.apache.logging.log4j.core.config.Configurator.initialize(log.name, "config/etc/log.xml")
			log.debug("RuleEditor start")

			if(args.isNotEmpty()) RuleEditor(args[0])
			else RuleEditor()
		}
	}
}
