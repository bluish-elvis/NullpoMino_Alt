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
package mu.nu.nullpo.tool.ruleeditor

import kotlinx.serialization.encodeToString
import mu.nu.nullpo.util.GeneralUtil.Json
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
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.FileReader
import java.io.IOException
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
	private val txtFldRuleName = JTextField("", 15)

	/** NEXT表示countのテキストfield */
	private val txtFldNextDisplay = JTextField("", 5)

	/** Game style comboBox */
	private var comboBoxStyle = JComboBox<String>()

	/** 絵柄のComboボックス */
	private var comboBoxSkin = JComboBox<ComboLabel>()

	/** ghost is enabled */
	private val chkBoxGhost = JCheckBox()

	/** Blockピースがfield枠外から出現 */
	private val chkBoxEnterAboveField = JCheckBox()

	/** 出現予定地が埋まっているときにY-coordinateを上にずらすMaximum count */
	private val txtFldEnterMaxDistanceY = JTextField("", 5)

	/** NEXT順生成アルゴリズム */
	private var comboBoxRandomizer:JComboBox<*>? = null

	/** NEXT順生成アルゴリズムのリスト */
	private var vectorRandomizer:Vector<String>? = null

	/** NEXT順生成アルゴリズムのリセット button
	 * private JButton btnResetRandomizer; */

	//----------------------------------------------------------------------
	/* field設定パネル */

	/** fieldの幅 */
	private val txtFldFieldWidth = JTextField("", 5)

	/** Field height */
	private val txtFldFieldHeight = JTextField("", 5)

	/** fieldの見えない部分の高さ */
	private val txtFldFieldHiddenHeight = JTextField("", 5)

	/** fieldの天井 */
	private val chkBoxFieldCeiling = JCheckBox()

	/** field枠内に置けないと死亡 */
	private val chkBoxFieldLockoutDeath = JCheckBox()

	/** field枠外にはみ出しただけで死亡 */
	private val chkBoxFieldPartialLockoutDeath = JCheckBox()

	//----------------------------------------------------------------------
	/* ホールド設定パネル */

	/** ホールド is enabled */
	private val chkBoxHoldEnable = JCheckBox()

	/** 先行ホールド */
	private val chkBoxHoldInitial = JCheckBox()

	/** 先行ホールド連続使用不可 */
	private val chkBoxHoldInitialLimit = JCheckBox()

	/** ホールドを使ったときにBlockピースの向きを初期状態に戻す */
	private val chkBoxHoldResetDirection = JCheckBox()

	/** ホールドできる count (-1:無制限) */
	private val txtFldHoldLimit = JTextField("", 5)

	//----------------------------------------------------------------------
	/* ドロップ設定パネル */

	/** Hard drop使用可否 */
	private val chkBoxDropHardDropEnable = JCheckBox()

	/** Hard dropで即固定 */
	private val chkBoxDropHardDropLock = JCheckBox()

	/** Hard drop連続使用不可 */
	private val txtFldDropHardDropLimit = JTextField("", 3)

	/** Soft drop使用可否 */
	private val chkBoxDropSoftDropEnable = JCheckBox()

	/** Soft dropで即固定 */
	private val chkBoxDropSoftDropLock = JCheckBox()

	/** Soft drop連続使用不可 */
	private val txtFldDropSoftDropLimit = JTextField("", 3)

	/** 接地状態でSoft dropすると即固定 */
	private val chkBoxDropSoftDropSurfaceLock = JCheckBox()

	/** Soft drop速度 */
	private val txtFldDropSoftDropSpeed = JTextField("", 5)

	/** Soft drop速度をCurrent 通常速度×n倍にする */
	private val chkBoxDropSoftDropMultiplyNativeSpeed = JCheckBox()

	/** Use new soft drop codes */
	private val chkBoxDropSoftDropGravitySpeedLimit = JCheckBox()

	//----------------------------------------------------------------------
	/* rotation設定パネル */

	/** 先行rotation */
	private val chkBoxSpinInitial = JCheckBox()

	/** 先行rotation連続使用不可 */
	private val chkBoxSpinInitialLimit = JCheckBox()

	/** Wallkick */
	private val chkBoxSpinWallkick = JCheckBox()

	/** 先行rotationでもWallkickする */
	private val chkBoxSpinInitialWallkick = JCheckBox()

	/** 上DirectionへのWallkickができる count (-1:無限) */
	private val txtFldSpinWallkickRise = JTextField("", 5)

	/** falseなら左が正rotation, When true,右が正rotation */
	private val chkBoxSpinToRight = JCheckBox()

	/** 逆rotationを許可 (falseなら正rotationと同じ) */
	private val chkBoxSpinReverseKey = JCheckBox()

	/** 180rotationを許可 (falseなら正rotationと同じ) */
	private val chkBoxSpinDoubleKey = JCheckBox()

	/** Wallkickアルゴリズム */
	private var comboBoxWallkickSystem:JComboBox<String> = JComboBox<String>()

	/** Wallkickアルゴリズムのリスト */
	private var vectorWallkickSystem:Vector<String>? = null

	/** Wallkickアルゴリズムのリセット button */
	private var btnResetWallkickSystem:JButton? = null

	//----------------------------------------------------------------------
	/* 固定 time設定パネル */

	/** 最低固定 time */
	private val txtFldLockDelayMin = JTextField("", 5)

	/** 最高固定 time */
	private val txtFldLockDelayMax = JTextField("", 5)

	/** 落下で固定 timeリセット */
	private val chkBoxLockDelayLockResetFall = JCheckBox()

	/** 移動で固定 timeリセット */
	private val chkBoxLockDelayLockResetMove = JCheckBox()

	/** rotationで固定 timeリセット */
	private val chkBoxLockDelayLockResetSpin = JCheckBox()

	/** Lock delay reset by wallkick */
	private val chkBoxLockDelayLockResetWallkick = JCheckBox()

	/** 横移動 counterとrotation counterを共有 (横移動 counterだけ使う) */
	private val chkBoxLockDelayLockResetLimitShareCount = JCheckBox()

	/** 横移動 count制限 */
	private val txtFldLockDelayLockResetLimitMove = JTextField("", 5)

	/** rotation count制限 */
	private val txtFldLockDelayLockResetLimitSpin = JTextField("", 5)

	/** 横移動 counterかrotation counterが超過したら固定 timeリセットを無効にする */
	private var radioLockDelayLockResetLimitOverNoReset:JRadioButton? = null

	/** 横移動 counterかrotation counterが超過したら即座に固定する */
	private var radioLockDelayLockResetLimitOverInstant:JRadioButton? = null

	/** 横移動 counterかrotation counterが超過したらWallkick無効にする */
	private var radioLockDelayLockResetLimitOverNoWallkick:JRadioButton? = null

	//----------------------------------------------------------------------
	/* ARE設定パネル */

	/** 最低ARE */
	private val txtFldAREMin = JTextField("", 5)

	/** 最高ARE */
	private val txtFldAREMax = JTextField("", 5)

	/** 最低ARE after line clear */
	private val txtFldARELineMin = JTextField("", 5)

	/** 最高ARE after line clear */
	private val txtFldARELineMax = JTextField("", 5)

	/** 固定した瞬間に光る frame count */
	private val txtFldARELockFlash = JTextField("", 5)

	/** Blockが光る専用 frame を入れる */
	private val chkBoxARELockFlashOnlyFrame = JCheckBox()

	/** Line clear前にBlockが光る frame を入れる */
	private val chkBoxARELockFlashBeforeLineClear = JCheckBox()

	/** ARE cancel on move checkbox */
	private val chkBoxARECancelMove = JCheckBox()

	/** ARE cancel on spin checkbox */
	private val chkBoxARECancelSpin = JCheckBox()

	/** ARE cancel on hold checkbox */
	private val chkBoxARECancelHold = JCheckBox()

	//----------------------------------------------------------------------
	/* Line clear設定パネル */

	/** 最低Line clear time */
	private val txtFldLineDelayMin = JTextField("", 5)

	/** 最高Line clear time */
	private val txtFldLineDelayMax = JTextField("", 5)

	/** 落下アニメ */
	private val chkBoxLineFallAnim = JCheckBox()

	/** Line delay cancel on move checkbox */
	private val chkBoxLineCancelMove = JCheckBox()

	/** Line delay cancel on spin checkbox */
	private val chkBoxLineCancelSpin = JCheckBox()

	/** Line delay cancel on hold checkbox */
	private val chkBoxLineCancelHold = JCheckBox()

	//----------------------------------------------------------------------
	/* 移動設定パネル */

	/** 最低横溜め time */
	private val txtFldMoveDASMin = JTextField("", 5)

	/** 最高横溜め time */
	private val txtFldMoveDASMax = JTextField("", 5)

	/** 横移動間隔 */
	private val txtFldMoveDASDelay = JTextField("", 5)

	/** Ready画面で横溜め可能 */
	private val chkBoxMoveDASInReady = JCheckBox()

	/** 最初の frame で横溜め可能 */
	private val chkBoxMoveDASInMoveFirstFrame = JCheckBox()

	/** Blockが光った瞬間に横溜め可能 */
	private val chkBoxMoveDASInLockFlash = JCheckBox()

	/** Line clear中に横溜め可能 */
	private val chkBoxMoveDASInLineClear = JCheckBox()

	/** ARE中に横溜め可能 */
	private val chkBoxMoveDASInARE = JCheckBox()

	/** AREの最後の frame で横溜め可能 */
	private val chkBoxMoveDASInARELastFrame = JCheckBox()

	/** Ending突入画面で横溜め可能 */
	private val chkBoxMoveDASInEndingStart = JCheckBox()

	/** DAS charge on blocked move checkbox */
	private val chkBoxMoveDASChargeOnBlockedMove = JCheckBox()

	/** Store DAS Charge on neutral checkbox */
	private val chkBoxMoveDASStoreChargeOnNeutral = JCheckBox()

	/** Redirect in delay checkbox */
	private val chkBoxMoveDASRedirectInDelay = JCheckBox()

	/** 最初の frame に移動可能 */
	private val chkBoxMoveFirstFrame = JCheckBox()

	/** 斜め移動 */
	private val chkBoxMoveDiagonal = JCheckBox()

	/** 上下同時押し可能 */
	private val chkBoxMoveUpAndDown = JCheckBox()

	/** 左右同時押し可能 */
	private val chkBoxMoveLeftAndRightAllow = JCheckBox()

	/** 左右同時押ししたときに前の frame の input Directionを優先する */
	private val chkBoxMoveLeftAndRightUsePreviousInput = JCheckBox()

	/** Shift lock checkbox */
	private val chkBoxMoveShiftLockEnable = JCheckBox()

	//----------------------------------------------------------------------
	/* rotationパターン補正パネル */

	/** プリセット選択Comboボックス */
	private var comboBoxPieceOffset = JComboBox<String>()

	/** rotationパターン補正タブ */
	private var tabPieceOffset = JTabbedPane()

	/** rotationパターン補正(X) input 欄 */
	private var txtFldPieceOffsetX:List<List<JTextField>> = emptyList()

	/** rotationパターン補正(Y) input 欄 */
	private var txtFldPieceOffsetY:List<List<JTextField>> = emptyList()

	//----------------------------------------------------------------------
	/* rotationパターン補正パネル */

	/** rotationパターン補正タブ */
	private var tabPieceSpawn = JTabbedPane()

	/** 出現位置補正(X) input 欄 */
	private var txtFldPieceSpawnX:List<List<JTextField>> = emptyList()

	/** 出現位置補正(Y) input 欄 */
	private var txtFldPieceSpawnY:List<List<JTextField>> = emptyList()

	/** Big時出現位置補正(X) input 欄 */
	private var txtFldPieceSpawnBigX:List<List<JTextField>> = emptyList()

	/** Big時出現位置補正(Y) input 欄 */
	private var txtFldPieceSpawnBigY:List<List<JTextField>> = emptyList()

	//----------------------------------------------------------------------
	/* 色設定パネル */

	/** 色選択Comboボックス */
	private var comboBoxPieceColor:List<JComboBox<*>> = emptyList()

	//----------------------------------------------------------------------
	/* 初期Direction設定パネル */

	/** 初期Direction選択Comboボックス */
	private var comboBoxPieceDirection:List<JComboBox<*>> = emptyList()

	//----------------------------------------------------------------------
	/** Block画像 */
	private var imgBlockSkins:List<BufferedImage> = emptyList()

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

		if(!filename.isNullOrEmpty())
			try {
				ruleOpt = load(filename)
				strNowFile = filename
				title = "${getUIText("Title_RuleEditor")}:$strNowFile"
			} catch(e:IOException) {
				log.error("Failed to load rule data from $filename", e)
				JOptionPane.showMessageDialog(
					this, "${getUIText("Message_FileLoadFailed")}\n$e",
					getUIText("Title_FileLoadFailed"), JOptionPane.ERROR_MESSAGE
				)
			}

		readRuleToUI(ruleOpt)

		isVisible = true
	}

	/** Initialization */
	private fun init() {
		// 設定ファイル読み込み
		propConfig.load("config/setting/swing.properties")

		// 言語ファイル読み込み
		if(propLangDefault.loadXML("config/lang/ruleeditor_default.xml")==null) log.error(
			"Couldn't load default UI language file")

		propLang.loadXML("config/lang/ruleeditor_${Locale.getDefault().country}.xml")

		// Look&Feel設定
		if(propConfig.getProperty("option.usenativelookandfeel", false))
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
		txtFldRuleName.columns = 15
		panelBasic.add(JPanel().apply {
			add(JLabel(getUIText("Basic_RuleName")))
			add(txtFldRuleName)
		})

		// NEXT表示count
		panelBasic.add(JPanel().apply {
			add(JLabel(getUIText("Basic_NextDisplay")))
			txtFldNextDisplay.columns = 5
			add(txtFldNextDisplay)
		})

		// Game style
		val pStyle = JPanel()
		pStyle.add(JLabel(getUIText("Basic_Style")))
		comboBoxStyle = JComboBox(GameEngine.GAMESTYLE_NAMES.toTypedArray()).apply {
			preferredSize = Dimension(100, 30)
		}
		pStyle.add(comboBoxStyle)
		panelBasic.add(pStyle)

		// 絵柄
		val pSkin = JPanel()
		panelBasic.add(pSkin)

		pSkin.add(JLabel(getUIText("Basic_Skin")))

		val model = DefaultComboBoxModel<ComboLabel>()
		imgBlockSkins.forEachIndexed {i, it ->
			model.addElement(ComboLabel("$i", ImageIcon(it)))
		}
		comboBoxSkin = JComboBox(model).apply {
			renderer = ComboLabelCellRenderer()
			preferredSize = Dimension(190, 30)
		}
		pSkin.add(comboBoxSkin)

		// ghost
		chkBoxGhost.text = getUIText("Basic_Ghost")
		panelBasic.add(chkBoxGhost)

		// field枠外から出現
		chkBoxEnterAboveField.text = getUIText("Basic_EnterAboveField")
		panelBasic.add(chkBoxEnterAboveField)

		// 出現予定地が埋まっているときにY-coordinateを上にずらすMaximum count
		val pEnterMaxDistanceY = JPanel()
		panelBasic.add(pEnterMaxDistanceY)

		pEnterMaxDistanceY.add(JLabel(getUIText("Basic_EnterMaxDistanceY")))

		txtFldEnterMaxDistanceY.columns = 5
		pEnterMaxDistanceY.add(txtFldEnterMaxDistanceY)

		// NEXT順生成アルゴリズム
		val pRandomizer = JPanel()
		panelBasic.add(pRandomizer)

		pRandomizer.add(JLabel(getUIText("Basic_Randomizer")))

		vectorRandomizer = this::class.java.getResource("../randomizer.lst")?.path?.let {getTextFileVector(it)}
		comboBoxRandomizer = JComboBox(createShortStringVector(vectorRandomizer)).apply {
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
			add(txtFldFieldWidth)
		})

		// Field height
		panelField.add(JPanel().apply {
			add(JLabel(getUIText("Field_FieldHeight")))
			add(txtFldFieldHeight)
		})

		// fieldの見えない部分の高さ
		panelField.add(JPanel().apply {
			add(JLabel(getUIText("Field_FieldHiddenHeight")))
			add(txtFldFieldHiddenHeight)
		})

		// fieldの天井
		chkBoxFieldCeiling.text = getUIText("Field_FieldCeiling")
		panelField.add(chkBoxFieldCeiling)

		// field枠内に置けないと死亡
		chkBoxFieldLockoutDeath.text = getUIText("Field_FieldLockoutDeath")
		panelField.add(chkBoxFieldLockoutDeath)

		// field枠外にはみ出しただけで死亡
		chkBoxFieldPartialLockoutDeath.text = getUIText("Field_FieldPartialLockoutDeath")
		panelField.add(chkBoxFieldPartialLockoutDeath)

		// ホールドタブ --------------------------------------------------
		val panelHold = JPanel()
		panelHold.layout = BoxLayout(panelHold, BoxLayout.Y_AXIS)
		tabPane.addTab(getUIText("TabName_Hold"), panelHold)

		// ホールド is enabled
		chkBoxHoldEnable.text = getUIText("Hold_HoldEnable")
		panelHold.add(chkBoxHoldEnable)

		// 先行ホールド
		chkBoxHoldInitial.text = getUIText("Hold_HoldInitial")
		panelHold.add(chkBoxHoldInitial)

		// 先行ホールド連続使用不可
		chkBoxHoldInitialLimit.text = getUIText("Hold_HoldInitialLimit")
		panelHold.add(chkBoxHoldInitialLimit)

		// ホールドを使ったときにBlockピースの向きを初期状態に戻す
		chkBoxHoldResetDirection.text = (getUIText("Hold_HoldResetDirection"))
		panelHold.add(chkBoxHoldResetDirection)

		// ホールドできる count
		txtFldHoldLimit.columns = 5
		val pHoldLimit = JPanel().apply {
			add(JLabel(getUIText("Hold_HoldLimit")))
			add(txtFldHoldLimit)
		}
		panelHold.add(pHoldLimit)

		// ドロップタブ --------------------------------------------------
		val panelDrop = JPanel()
		panelDrop.layout = BoxLayout(panelDrop, BoxLayout.Y_AXIS)
		tabPane.addTab(getUIText("TabName_Drop"), panelDrop)

		// Hard drop使用可否
		chkBoxDropHardDropEnable.text = getUIText("Drop_HardDropEnable")
		panelDrop.add(chkBoxDropHardDropEnable)

		// Hard dropで即固定
		chkBoxDropHardDropLock.text = getUIText("Drop_HardDropLock")
		panelDrop.add(chkBoxDropHardDropLock)

		// Hard drop連続使用不可
		txtFldDropHardDropLimit.columns = 3
		panelDrop.add(JPanel().apply{
			add(JLabel(getUIText("Drop_HardDropLimit")))
			add(txtFldDropHardDropLimit)
		})

		// Soft drop使用可否
		chkBoxDropSoftDropEnable.text = getUIText("Drop_SoftDropEnable")
		panelDrop.add(chkBoxDropSoftDropEnable)

		// Soft dropで即固定
		chkBoxDropSoftDropLock.text = getUIText("Drop_SoftDropLock")
		panelDrop.add(chkBoxDropSoftDropLock)

		// Soft drop連続使用不可
		txtFldDropSoftDropLimit.columns = 3
		panelDrop.add(JPanel().apply{
			add(JLabel(getUIText("Drop_SoftDropLimit")))
			add(txtFldDropSoftDropLimit)
		})

		// 接地状態でSoft dropすると即固定
		chkBoxDropSoftDropSurfaceLock.text = getUIText("Drop_SoftDropSurfaceLock")
		panelDrop.add(chkBoxDropSoftDropSurfaceLock)

		// Soft drop速度をCurrent 通常速度×n倍にする
		chkBoxDropSoftDropMultiplyNativeSpeed.text = getUIText("Drop_SoftDropMultiplyNativeSpeed")
		panelDrop.add(chkBoxDropSoftDropMultiplyNativeSpeed)

		// Use new soft drop codes
		chkBoxDropSoftDropGravitySpeedLimit.text = getUIText("Drop_SoftDropGravitySpeedLimit")
		panelDrop.add(chkBoxDropSoftDropGravitySpeedLimit)

		// Soft drop速度
		txtFldDropSoftDropSpeed.columns = 5
		panelDrop.add(JPanel().apply{
			add(JLabel(getUIText("Drop_SoftDropSpeed")))
			add(txtFldDropSoftDropSpeed)
		})


		// rotationタブ --------------------------------------------------
		val panelSpin = JPanel()
		panelSpin.layout = BoxLayout(panelSpin, BoxLayout.Y_AXIS)
		tabPane.addTab(getUIText("TabName_Rotate"), panelSpin)

		// 先行rotation
		chkBoxSpinInitial.text = (getUIText("Rotate_RotateInitial"))
		panelSpin.add(chkBoxSpinInitial)

		// 先行rotation連続使用不可
		chkBoxSpinInitialLimit.text = (getUIText("Rotate_RotateInitialLimit"))
		panelSpin.add(chkBoxSpinInitialLimit)

		// Wallkick
		chkBoxSpinWallkick.text = (getUIText("Rotate_RotateWallkick"))
		panelSpin.add(chkBoxSpinWallkick)

		// 先行rotationでもWallkickする
		chkBoxSpinInitialWallkick.text = (getUIText("Rotate_RotateInitialWallkick"))
		panelSpin.add(chkBoxSpinInitialWallkick)

		// Aで右rotation
		chkBoxSpinToRight.text = (getUIText("Rotate_RotateButtonDefaultRight"))
		panelSpin.add(chkBoxSpinToRight)

		// 逆rotation許可
		chkBoxSpinReverseKey.text = (getUIText("Rotate_RotateButtonAllowReverse"))
		panelSpin.add(chkBoxSpinReverseKey)

		// 2rotation許可
		chkBoxSpinDoubleKey.text = (getUIText("Rotate_RotateButtonAllowDouble"))
		panelSpin.add(chkBoxSpinDoubleKey)

		// UpDirectionへWallkickできる count
		val pSpinMaxUpwardWallkick = JPanel()
		panelSpin.add(pSpinMaxUpwardWallkick)
		pSpinMaxUpwardWallkick.add(JLabel(getUIText("Rotate_RotateMaxUpwardWallkick")))

		txtFldSpinWallkickRise.columns = (5)
		pSpinMaxUpwardWallkick.add(txtFldSpinWallkickRise)

		// Wallkickアルゴリズム
		val pWallkickSystem = JPanel()
		panelSpin.add(pWallkickSystem)

		pWallkickSystem.add(JLabel(getUIText("Rotate_WallkickSystem")))

		vectorWallkickSystem = this::class.java.getResource("../wallkick.lst")?.path?.let {getTextFileVector(it)}
		comboBoxWallkickSystem = JComboBox(createShortStringVector(vectorWallkickSystem)).apply {
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

		txtFldLockDelayMin.columns = 5
		pLockDelayMinMax.add(txtFldLockDelayMin)
		txtFldLockDelayMax.columns = 5
		pLockDelayMinMax.add(txtFldLockDelayMax)

		// 落下で固定 timeリセット
		chkBoxLockDelayLockResetFall.text = (getUIText("LockDelay_LockResetFall"))
		panelLockDelay.add(chkBoxLockDelayLockResetFall)

		// 移動で固定 timeリセット
		chkBoxLockDelayLockResetMove.text = (getUIText("LockDelay_LockResetMove"))
		panelLockDelay.add(chkBoxLockDelayLockResetMove)

		// rotationで固定 timeリセット
		chkBoxLockDelayLockResetSpin.text = (getUIText("LockDelay_LockResetRotate"))
		panelLockDelay.add(chkBoxLockDelayLockResetSpin)

		// Lock delay reset by wallkick
		chkBoxLockDelayLockResetWallkick.text = (getUIText("LockDelay_LockResetWallkick"))
		panelLockDelay.add(chkBoxLockDelayLockResetWallkick)

		// 横移動 counterとrotation counterを共有 (横移動 counterだけ使う）
		chkBoxLockDelayLockResetLimitShareCount.text = (getUIText("LockDelay_LockDelayLockResetLimitShareCount"))
		panelLockDelay.add(chkBoxLockDelayLockResetLimitShareCount)

		// 横移動 count制限
		val pLockDelayLockResetLimitMove = JPanel()
		panelLockDelay.add(pLockDelayLockResetLimitMove)
		pLockDelayLockResetLimitMove.add(JLabel(getUIText("LockDelay_LockDelayLockResetLimitMove")))

		txtFldLockDelayLockResetLimitMove.columns = 5
		pLockDelayLockResetLimitMove.add(txtFldLockDelayLockResetLimitMove)

		// rotation count制限
		val pLockDelayLockResetLimitSpin = JPanel()
		panelLockDelay.add(pLockDelayLockResetLimitSpin)

		pLockDelayLockResetLimitSpin.add(JLabel(getUIText("LockDelay_LockDelayLockResetLimitRotate")))

		txtFldLockDelayLockResetLimitSpin.columns = 5
		pLockDelayLockResetLimitSpin.add(txtFldLockDelayLockResetLimitSpin)

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

		txtFldAREMin.columns = 5
		pAREMinMax.add(txtFldAREMin)
		txtFldAREMax.columns = 5
		pAREMinMax.add(txtFldAREMax)

		// 最低ARE after line clearと最高ARE after line clear
		panelARE.add(JLabel(getUIText("ARE_LineMinMax")))

		val pARELineMinMax = JPanel()
		panelARE.add(pARELineMinMax)

		txtFldARELineMin.columns = 5
		pARELineMinMax.add(txtFldARELineMin)
		txtFldARELineMax.columns = 5
		pARELineMinMax.add(txtFldARELineMax)

		// 固定した瞬間に光る frame count
		panelARE.add(JLabel(getUIText("ARE_LockFlash")))

		val pARELockFlash = JPanel()
		panelARE.add(pARELockFlash)

		txtFldARELockFlash.columns = 5
		pARELockFlash.add(txtFldARELockFlash)

		// Blockが光る専用 frame を入れる
		chkBoxARELockFlashOnlyFrame.text = (getUIText("ARE_LockFlashOnlyFrame"))
		panelARE.add(chkBoxARELockFlashOnlyFrame)

		// Line clear前にBlockが光る frame を入れる
		chkBoxARELockFlashBeforeLineClear.text = (getUIText("ARE_LockFlashBeforeLineClear"))
		panelARE.add(chkBoxARELockFlashBeforeLineClear)

		// ARE cancel on move
		chkBoxARECancelMove.text = (getUIText("ARE_CancelMove"))
		panelARE.add(chkBoxARECancelMove)

		// ARE cancel on move
		chkBoxARECancelSpin.text = (getUIText("ARE_CancelRotate"))
		panelARE.add(chkBoxARECancelSpin)

		// ARE cancel on move
		chkBoxARECancelHold.text = (getUIText("ARE_CancelHold"))
		panelARE.add(chkBoxARECancelHold)

		// Line clearタブ --------------------------------------------------
		val panelLine = JPanel()
		panelLine.layout = BoxLayout(panelLine, BoxLayout.Y_AXIS)
		tabPane.addTab(getUIText("TabName_Line"), panelLine)

		// 最低Line clear timeと最高Line clear time
		panelLine.add(JLabel(getUIText("Line_MinMax")))

		val pLineMinMax = JPanel()
		panelLine.add(pLineMinMax)

		txtFldLineDelayMin.columns = (5)
		pLineMinMax.add(txtFldLineDelayMin)
		txtFldLineDelayMax.columns = (5)
		pLineMinMax.add(txtFldLineDelayMax)

		// 落下アニメ
		chkBoxLineFallAnim.text = (getUIText("Line_FallAnim"))
		panelLine.add(chkBoxLineFallAnim)

		// Line delay cancel on move
		chkBoxLineCancelMove.text = (getUIText("Line_CancelMove"))
		panelLine.add(chkBoxLineCancelMove)

		// Line delay cancel on spin
		chkBoxLineCancelSpin.text = (getUIText("Line_CancelRotate"))
		panelLine.add(chkBoxLineCancelSpin)

		// Line delay cancel on hold
		chkBoxLineCancelHold.text = (getUIText("Line_CancelHold"))
		panelLine.add(chkBoxLineCancelHold)

		// 移動タブ --------------------------------------------------
		val panelMove = JPanel()
		panelMove.layout = BoxLayout(panelMove, BoxLayout.Y_AXIS)
		tabPane.addTab(getUIText("TabName_Move"), panelMove)

		// 最低横溜め timeと最高横溜め time
		panelMove.add(JLabel(getUIText("Move_DASMinMax")))

		val pMoveDASMinMax = JPanel()
		panelMove.add(pMoveDASMinMax)

		txtFldMoveDASMin.columns = (5)
		pMoveDASMinMax.add(txtFldMoveDASMin)
		txtFldMoveDASMax.columns = (5)
		pMoveDASMinMax.add(txtFldMoveDASMax)

		// 横移動間隔
		val pMoveDASDelay = JPanel()
		panelMove.add(pMoveDASDelay)

		pMoveDASDelay.add(JLabel(getUIText("Move_DASDelay1")))

		txtFldMoveDASDelay.columns = (5)
		pMoveDASDelay.add(txtFldMoveDASDelay)

		pMoveDASDelay.add(JLabel(getUIText("Move_DASDelay2")))

		// ○○のとき横溜め可能
		chkBoxMoveDASInReady.text = (getUIText("Move_DASInReady"))
		panelMove.add(chkBoxMoveDASInReady)
		chkBoxMoveDASInMoveFirstFrame.text = (getUIText("Move_DASInMoveFirstFrame"))
		panelMove.add(chkBoxMoveDASInMoveFirstFrame)
		chkBoxMoveDASInLockFlash.text = (getUIText("Move_DASInLockFlash"))
		panelMove.add(chkBoxMoveDASInLockFlash)
		chkBoxMoveDASInLineClear.text = (getUIText("Move_DASInLineClear"))
		panelMove.add(chkBoxMoveDASInLineClear)
		chkBoxMoveDASInARE.text = (getUIText("Move_DASInARE"))
		panelMove.add(chkBoxMoveDASInARE)
		chkBoxMoveDASInARELastFrame.text = (getUIText("Move_DASInARELastFrame"))
		panelMove.add(chkBoxMoveDASInARELastFrame)
		chkBoxMoveDASInEndingStart.text = (getUIText("Move_DASInEndingStart"))
		panelMove.add(chkBoxMoveDASInEndingStart)
		chkBoxMoveDASChargeOnBlockedMove.text = (getUIText("Move_DASChargeOnBlockedMove"))
		panelMove.add(chkBoxMoveDASChargeOnBlockedMove)
		chkBoxMoveDASStoreChargeOnNeutral.text = (getUIText("Move_DASStoreChargeOnNeutral"))
		panelMove.add(chkBoxMoveDASStoreChargeOnNeutral)
		chkBoxMoveDASRedirectInDelay.text = (getUIText("Move_DASRedirectInDelay"))
		panelMove.add(chkBoxMoveDASRedirectInDelay)

		// 最初の frame に移動可能
		chkBoxMoveFirstFrame.text = (getUIText("Move_FirstFrame"))
		panelMove.add(chkBoxMoveFirstFrame)

		// 斜め移動
		chkBoxMoveDiagonal.text = (getUIText("Move_Diagonal"))
		panelMove.add(chkBoxMoveDiagonal)

		// Up下同時押し
		chkBoxMoveUpAndDown.text = (getUIText("Move_UpAndDown"))
		panelMove.add(chkBoxMoveUpAndDown)

		// 左右同時押し
		chkBoxMoveLeftAndRightAllow.text = (getUIText("Move_LeftAndRightAllow"))
		panelMove.add(chkBoxMoveLeftAndRightAllow)

		// 左右同時押ししたときに前 frame の input を優先
		chkBoxMoveLeftAndRightUsePreviousInput.text = (getUIText("Move_LeftAndRightUsePreviousInput"))
		panelMove.add(chkBoxMoveLeftAndRightUsePreviousInput)

		// Shift lock
		chkBoxMoveShiftLockEnable.text = (getUIText("Move_ShiftLock"))
		panelMove.add(chkBoxMoveShiftLockEnable)

		// rotationパターン補正タブ ------------------------------------------------
		val panelPieceOffset = JPanel().also {
			it.layout = BoxLayout(it, BoxLayout.Y_AXIS)
			tabPane.addTab(getUIText("TabName_PieceOffset"), it)
		}
		val pPresetOffset = JPanel().also {
			it.layout = BoxLayout(it, BoxLayout.X_AXIS)
			panelPieceOffset.add(it)
		}
		comboBoxPieceOffset = JComboBox(RuleOptions.PIECEOFFSET_NAME.map {getUIText(it)}.toTypedArray()).also {
			it.actionCommand = "OffsetPreset"
			it.addActionListener(this)
			pPresetOffset.add(it)
		}
		JButton(getUIText("Apply")).also {
			it.setMnemonic('A')
			it.actionCommand = "OffsetApply"
			it.addActionListener(this)
			pPresetOffset.add(it)
		}
		tabPieceOffset = JTabbedPane()
		panelPieceOffset.add(tabPieceOffset)

		// rotationパターン補正(X)タブ --------------------------------------------------
		val panelPieceOffsetX = JPanel()
		panelPieceOffsetX.layout = BoxLayout(panelPieceOffsetX, BoxLayout.Y_AXIS)
		tabPieceOffset.addTab(getUIText("TabName_PieceOffsetX"), panelPieceOffsetX)

		val pPieceOffsetX = List(Piece.PIECE_COUNT) {
			JPanel().apply {
				panelPieceOffsetX.add(this)
				add(JLabel(getUIText("PieceName$it")))
			}
		}

		txtFldPieceOffsetX = List(Piece.PIECE_COUNT) {i ->
			List(Piece.DIRECTION_COUNT) {
				JTextField("", 5).also {pPieceOffsetX[i].add(it)}
			}
		}
		// rotationパターン補正(Y)タブ --------------------------------------------------
		val panelPieceOffsetY = JPanel().apply {
			layout = BoxLayout(this, BoxLayout.Y_AXIS)
			tabPieceOffset.addTab(getUIText("TabName_PieceOffsetY"), this)
		}

		val pPieceOffsetY = List(Piece.PIECE_COUNT) {
			JPanel().apply {
				panelPieceOffsetY.add(this)
				add(JLabel(getUIText("PieceName$it")))
			}
		}

		txtFldPieceOffsetY = List(Piece.PIECE_COUNT) {i ->
			List(Piece.DIRECTION_COUNT) {
				JTextField("", 5).also {pPieceOffsetY[i].add(it)}
			}
		}

		// 出現位置補正タブ ------------------------------------------------
		val panelPieceSpawn = JPanel().also {
			it.layout = BoxLayout(it, BoxLayout.Y_AXIS)
			tabPane.addTab(getUIText("TabName_PieceSpawn"), it)
		}
		val pResetSpawn = JPanel().also {
			it.layout = BoxLayout(it, BoxLayout.X_AXIS)
			panelPieceSpawn.add(it)
		}
		val bResetSpawn = listOf(JButton("${getUIText("Basic_Reset")} SRS").apply {
			setMnemonic('S')
			actionCommand = "ResetSpawn_SRS"
		}, JButton("${getUIText("Basic_Reset")} ARS").apply {
			setMnemonic('A')
			actionCommand = "ResetSpawn_ARS"
		})
		bResetSpawn.forEach {
			it.addActionListener(this)
			pResetSpawn.add(it)
		}

		tabPieceSpawn = JTabbedPane()
		panelPieceSpawn.add(tabPieceSpawn)

		// 出現位置補正(X)タブ --------------------------------------------------
		val panelPieceSpawnX = JPanel()
		panelPieceSpawnX.layout = BoxLayout(panelPieceSpawnX, BoxLayout.Y_AXIS)
		tabPieceSpawn.addTab(getUIText("TabName_PieceSpawnX"), panelPieceSpawnX)

		val pPieceSpawnX = List(Piece.PIECE_COUNT) {
			JPanel().apply {
				add(JLabel(getUIText("PieceName$it")))
			}
		}

		txtFldPieceSpawnX = List(Piece.PIECE_COUNT) {i ->
			panelPieceSpawnX.add(pPieceSpawnX[i])
			List(Piece.DIRECTION_COUNT) {
				JTextField("", 5).apply {
					pPieceSpawnX[i].add(this)
				}
			}
		}

		// 出現位置補正(Y)タブ --------------------------------------------------
		val panelPieceSpawnY = JPanel().apply {
			layout = BoxLayout(this, BoxLayout.Y_AXIS)
			tabPieceSpawn.addTab(getUIText("TabName_PieceSpawnY"), this)
		}

		val pPieceSpawnY = List(Piece.PIECE_COUNT) {
			JPanel().apply {
				panelPieceSpawnY.add(this)
				add(JLabel(getUIText("PieceName$it")))
			}
		}

		txtFldPieceSpawnY = List(Piece.PIECE_COUNT) {i ->
			List(Piece.DIRECTION_COUNT) {
				JTextField("", 5).apply {
					pPieceSpawnY[i].add(this)
				}
			}
		}

		// Big時出現位置補正(X)タブ --------------------------------------------------
		val panelPieceSpawnBigX = JPanel().apply {
			layout = BoxLayout(this, BoxLayout.Y_AXIS)
			tabPieceSpawn.addTab(getUIText("TabName_PieceSpawnBigX"), this)
		}

		val pPieceSpawnBigX = List(Piece.PIECE_COUNT) {
			JPanel().apply {
				panelPieceSpawnBigX.add(this)
				add(JLabel(getUIText("PieceName$it")))
			}
		}

		txtFldPieceSpawnBigX = List(Piece.PIECE_COUNT) {i ->
			List(Piece.DIRECTION_COUNT) {
				JTextField("", 5).apply {pPieceSpawnBigX[i].add(this)}
			}
		}
		// Big時出現位置補正(Y)タブ --------------------------------------------------
		val panelPieceSpawnBigY = JPanel().apply {
			layout = BoxLayout(this, BoxLayout.Y_AXIS)
			tabPieceSpawn.addTab(getUIText("TabName_PieceSpawnBigY"), this)
		}

		val pPieceSpawnBigY = List(Piece.PIECE_COUNT) {
			JPanel().apply {
				panelPieceSpawnBigY.add(this)
				add(JLabel(getUIText("PieceName$it")))
			}
		}

		txtFldPieceSpawnBigY = List(Piece.PIECE_COUNT) {i ->
			List(Piece.DIRECTION_COUNT) {
				JTextField("", 5).apply {pPieceSpawnBigY[i].add(this)}
			}
		}

		// 色設定タブ --------------------------------------------------
		val panelPieceColor = JPanel().apply {
			layout = BoxLayout(this, BoxLayout.X_AXIS)
			tabPane.addTab(getUIText("TabName_PieceColor"), this)
		}

		val pColorRow = List(2) {
			JPanel().apply {
				layout = BoxLayout(this, BoxLayout.Y_AXIS)
				panelPieceColor.add(this)
			}
		}
		val bResetColor = listOf(JButton(getUIText("Basic_Reset")+" SRS").apply {
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

		val strColorNames = Array(Block.COLOR.COUNT-2) {getUIText("ColorName$it")}
		val pPieceColor = List(Piece.PIECE_COUNT) {
			JPanel().apply {
				pColorRow[0].add(this)
				add(JLabel(getUIText("PieceName$it")))
			}
		}
		comboBoxPieceColor = List(Piece.PIECE_COUNT) {i ->
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

		val pDirectRow = List(2) {
			JPanel().apply {
				this@apply.layout = BoxLayout(this, BoxLayout.Y_AXIS)
				panelPieceDirection.add(this)
			}
		}
		val bResetDirect = listOf(JButton("${getUIText("Basic_Reset")} SRS").apply {
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

		val pPieceDirection = List(Piece.PIECE_COUNT) {
			JPanel().apply {
				pDirectRow[0].add(this)
				add(JLabel(getUIText("PieceName$it")))
			}
		}

		val strDirectionNames = Array(Piece.DIRECTION_COUNT+1) {
			getUIText("DirectionName$it")
		}

		comboBoxPieceDirection = List(Piece.PIECE_COUNT) {
			JComboBox(strDirectionNames).apply {
				preferredSize = Dimension(150, 30)
				maximumRowCount = strDirectionNames.size
				pPieceDirection[it].add(this)
			}
		}
	}

	/** Block画像を読み込み */
	private fun loadBlockSkins() {
		val skinDir = propConfig.getProperty("custom.skin.directory", "res")

		var numBlocks = 0
		while(File("$skinDir/graphics/blockskin/normal/n$numBlocks.png").canRead()) numBlocks++
		log.debug("$numBlocks block skins found")

		imgBlockSkins = List(numBlocks) {i ->
			val imgBlock = loadImage(getURL("$skinDir/graphics/blockskin/normal/n$i.png"))
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
			log.debug("Loaded image from {}", url)
		} catch(e:IOException) {
			log.error("Failed to load image from ${url?:""}", e)
		}

		return img
	}

	/** リソースファイルのURLを返す
	 * @param str Filename
	 * @return リソースファイルのURL
	 */
	private fun getURL(str:String):URL? = try {
		File(str).toURI().toURL()
	} catch(e:MalformedURLException) {
		log.warn("Invalid URL:$str", e)
		null
	}

	/** テキストファイルを読み込んでVector&lt;String&gt;に入れる
	 * @param filename Filename
	 * @return テキストファイルを読み込んだVector&lt;String&gt;
	 */
	private fun getTextFileVector(filename:String):Vector<String> {
		val vec = Vector<String>()

		try {
			FileReader(filename).buffered().use {
				it.forEachLine {s -> vec.add(s)}
			}
		} catch(_:IOException) {
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
		txtFldRuleName.text = r.strRuleName
		txtFldNextDisplay.text = r.nextDisplay.toString()
		comboBoxStyle.selectedIndex = r.style
		comboBoxSkin.selectedIndex = r.skin
		chkBoxGhost.isSelected = r.ghost
		chkBoxEnterAboveField.isSelected = r.pieceEnterAboveField
		txtFldEnterMaxDistanceY.text = r.pieceEnterMaxDistanceY.toString()
		comboBoxRandomizer?.selectedIndex = vectorRandomizer?.indexOf(r.strRandomizer)?:0

		txtFldFieldWidth.text = r.fieldWidth.toString()
		txtFldFieldHeight.text = r.fieldHeight.toString()
		txtFldFieldHiddenHeight.text = r.fieldHiddenHeight.toString()
		chkBoxFieldCeiling.isSelected = r.fieldCeiling
		chkBoxFieldLockoutDeath.isSelected = r.fieldLockoutDeath
		chkBoxFieldPartialLockoutDeath.isSelected = r.fieldPartialLockoutDeath

		chkBoxHoldEnable.isSelected = r.holdEnable
		chkBoxHoldInitial.isSelected = r.holdInitial
		chkBoxHoldInitialLimit.isSelected = r.holdInitialLimit
		chkBoxHoldResetDirection.isSelected = r.holdResetDirection
		txtFldHoldLimit.text = r.holdLimit.toString()

		chkBoxDropHardDropEnable.isSelected = r.harddropEnable
		chkBoxDropHardDropLock.isSelected = r.harddropLock
		txtFldDropHardDropLimit.text = r.harddropLimit.toString()
		chkBoxDropSoftDropEnable.isSelected = r.softdropEnable
		chkBoxDropSoftDropLock.isSelected = r.softdropLock
		txtFldDropSoftDropLimit.text = r.softdropLimit.toString()
		chkBoxDropSoftDropSurfaceLock.isSelected = r.softdropSurfaceLock
		txtFldDropSoftDropSpeed.text = r.softdropSpeed.toString()
		chkBoxDropSoftDropMultiplyNativeSpeed.isSelected = r.softdropMultiplyNativeSpeed
		chkBoxDropSoftDropGravitySpeedLimit.isSelected = r.softdropGravitySpeedLimit

		chkBoxSpinInitial.isSelected = r.spinInitial
		chkBoxSpinInitialLimit.isSelected = r.spinInitialLimit
		chkBoxSpinWallkick.isSelected = r.spinWallkick
		chkBoxSpinInitialWallkick.isSelected = r.spinInitialWallkick
		txtFldSpinWallkickRise.text = r.spinWallkickMaxRise.toString()
		chkBoxSpinToRight.isSelected = r.spinToRight
		chkBoxSpinReverseKey.isSelected = r.spinReverseKey
		chkBoxSpinDoubleKey.isSelected = r.spinDoubleKey
		comboBoxWallkickSystem.selectedIndex = vectorWallkickSystem?.indexOf(r.strWallkick)?:0
		txtFldLockDelayMin.text = r.minLockDelay.toString()
		txtFldLockDelayMax.text = r.maxLockDelay.toString()
		chkBoxLockDelayLockResetFall.isSelected = r.lockResetFall
		chkBoxLockDelayLockResetMove.isSelected = r.lockResetMove
		chkBoxLockDelayLockResetSpin.isSelected = r.lockResetSpin
		chkBoxLockDelayLockResetWallkick.isSelected = r.lockResetWallkick
		chkBoxLockDelayLockResetLimitShareCount.isSelected = r.lockResetLimitShareCount
		txtFldLockDelayLockResetLimitMove.text = r.lockResetMoveLimit.toString()
		txtFldLockDelayLockResetLimitSpin.text = r.lockResetSpinLimit.toString()
		when(r.lockResetLimitOver) {
			RuleOptions.LOCKRESET_LIMIT_OVER_NO_RESET -> radioLockDelayLockResetLimitOverNoReset?.isSelected = true
			RuleOptions.LOCKRESET_LIMIT_OVER_INSTANT -> radioLockDelayLockResetLimitOverInstant?.isSelected = true
			RuleOptions.LOCKRESET_LIMIT_OVER_NO_KICK -> radioLockDelayLockResetLimitOverNoWallkick?.isSelected = true
		}

		txtFldAREMin.text = r.minARE.toString()
		txtFldAREMax.text = r.maxARE.toString()
		txtFldARELineMin.text = r.minARELine.toString()
		txtFldARELineMax.text = r.maxARELine.toString()
		txtFldARELockFlash.text = r.lockFlash.toString()
		chkBoxARELockFlashOnlyFrame.isSelected = r.lockFlashOnlyFrame
		chkBoxARELockFlashBeforeLineClear.isSelected = r.lockFlashBeforeLineClear
		chkBoxARECancelMove.isSelected = r.areCancelMove
		chkBoxARECancelSpin.isSelected = r.areCancelSpin
		chkBoxARECancelHold.isSelected = r.areCancelHold

		txtFldLineDelayMin.text = r.minLineDelay.toString()
		txtFldLineDelayMax.text = r.maxLineDelay.toString()
		chkBoxLineFallAnim.isSelected = r.lineFallAnim
		chkBoxLineCancelMove.isSelected = r.lineCancelMove
		chkBoxLineCancelSpin.isSelected = r.lineCancelSpin
		chkBoxLineCancelHold.isSelected = r.lineCancelHold

		txtFldMoveDASMin.text = r.minDAS.toString()
		txtFldMoveDASMax.text = r.maxDAS.toString()
		txtFldMoveDASDelay.text = r.dasARR.toString()
		chkBoxMoveDASInReady.isSelected = r.dasInReady
		chkBoxMoveDASInMoveFirstFrame.isSelected = r.dasInMoveFirstFrame
		chkBoxMoveDASInLockFlash.isSelected = r.dasInLockFlash
		chkBoxMoveDASInLineClear.isSelected = r.dasInLineClear
		chkBoxMoveDASInARE.isSelected = r.dasInARE
		chkBoxMoveDASInARELastFrame.isSelected = r.dasInARELastFrame
		chkBoxMoveDASInEndingStart.isSelected = r.dasInEndingStart
		chkBoxMoveDASChargeOnBlockedMove.isSelected = r.dasChargeOnBlockedMove
		chkBoxMoveDASStoreChargeOnNeutral.isSelected = r.dasStoreChargeOnNeutral
		chkBoxMoveDASRedirectInDelay.isSelected = r.dasRedirectInDelay
		chkBoxMoveFirstFrame.isSelected = r.moveFirstFrame
		chkBoxMoveDiagonal.isSelected = r.moveDiagonal
		chkBoxMoveUpAndDown.isSelected = r.moveUpAndDown
		chkBoxMoveLeftAndRightAllow.isSelected = r.moveLeftAndRightAllow
		chkBoxMoveLeftAndRightUsePreviousInput.isSelected = r.moveLeftAndRightUsePreviousInput
		chkBoxMoveShiftLockEnable.isSelected = r.shiftLockEnable
		comboBoxPieceOffset.selectedIndex = r.pieceOffset
		r.pieceOffsetX.forEachIndexed {x, i -> i.forEachIndexed {y, j -> txtFldPieceOffsetX[x][y].text = "$j"}}
		r.pieceOffsetY.forEachIndexed {x, i -> i.forEachIndexed {y, j -> txtFldPieceOffsetY[x][y].text = "$j"}}
		r.pieceSpawnX.forEachIndexed {x, i -> i.forEachIndexed {y, j -> txtFldPieceSpawnX[x][y].text = "$j"}}
		r.pieceSpawnY.forEachIndexed {x, i -> i.forEachIndexed {y, j -> txtFldPieceSpawnY[x][y].text = "$j"}}
		r.pieceSpawnXBig.forEachIndexed {x, i -> i.forEachIndexed {y, j -> txtFldPieceSpawnBigX[x][y].text = "$j"}}
		r.pieceSpawnYBig.forEachIndexed {x, i -> i.forEachIndexed {y, j -> txtFldPieceSpawnBigY[x][y].text = "$j"}}
		comboBoxPieceColor.forEachIndexed {i, it -> it.selectedIndex = r.pieceColor[i]-1}
		comboBoxPieceDirection.forEachIndexed {i, it -> it.selectedIndex = r.pieceDefaultDirection[i]}
	}

	/** ルール設定をUIから書き込む
	 * @param r ルール設定
	 */
	private fun writeRuleFromUI(r:RuleOptions) {
		r.strRuleName = txtFldRuleName.text.uppercase()
		r.nextDisplay = getIntTextField(txtFldNextDisplay)
		r.style = comboBoxStyle.selectedIndex
		r.skin = comboBoxSkin.selectedIndex
		r.ghost = chkBoxGhost.isSelected
		r.pieceEnterAboveField = chkBoxEnterAboveField.isSelected
		r.pieceEnterMaxDistanceY = getIntTextField(txtFldEnterMaxDistanceY)
		val indexRandomizer = comboBoxRandomizer!!.selectedIndex
		r.strRandomizer = if(indexRandomizer>=0) vectorRandomizer!![indexRandomizer] else ""

		r.fieldWidth = getIntTextField(txtFldFieldWidth)
		r.fieldHeight = getIntTextField(txtFldFieldHeight)
		r.fieldHiddenHeight = getIntTextField(txtFldFieldHiddenHeight)
		r.fieldCeiling = chkBoxFieldCeiling.isSelected
		r.fieldLockoutDeath = chkBoxFieldLockoutDeath.isSelected
		r.fieldPartialLockoutDeath = chkBoxFieldPartialLockoutDeath.isSelected

		r.holdEnable = chkBoxHoldEnable.isSelected
		r.holdInitial = chkBoxHoldInitial.isSelected
		r.holdInitialLimit = chkBoxHoldInitialLimit.isSelected
		r.holdResetDirection = chkBoxHoldResetDirection.isSelected
		r.holdLimit = getIntTextField(txtFldHoldLimit)

		r.harddropEnable = chkBoxDropHardDropEnable.isSelected
		r.harddropLock = chkBoxDropHardDropLock.isSelected
		r.harddropLimit = getIntTextField(txtFldDropHardDropLimit)
		r.softdropEnable = chkBoxDropSoftDropEnable.isSelected
		r.softdropLock = chkBoxDropSoftDropLock.isSelected
		r.softdropLimit = getIntTextField(txtFldDropSoftDropLimit)
		r.softdropSurfaceLock = chkBoxDropSoftDropSurfaceLock.isSelected
		r.softdropSpeed = getFloatTextField(txtFldDropSoftDropSpeed)
		r.softdropMultiplyNativeSpeed = chkBoxDropSoftDropMultiplyNativeSpeed.isSelected
		r.softdropGravitySpeedLimit = chkBoxDropSoftDropGravitySpeedLimit.isSelected

		r.spinInitial = chkBoxSpinInitial.isSelected
		r.spinInitialLimit = chkBoxSpinInitialLimit.isSelected
		r.spinWallkick = chkBoxSpinWallkick.isSelected
		r.spinInitialWallkick = chkBoxSpinInitialWallkick.isSelected
		r.spinWallkickMaxRise = getIntTextField(txtFldSpinWallkickRise)
		r.spinToRight = chkBoxSpinToRight.isSelected
		r.spinReverseKey = chkBoxSpinReverseKey.isSelected
		r.spinDoubleKey = chkBoxSpinDoubleKey.isSelected
		val indexWallkick = comboBoxWallkickSystem.selectedIndex
		r.strWallkick = if(indexWallkick>=0) vectorWallkickSystem!![indexWallkick] else ""

		r.minLockDelay = getIntTextField(txtFldLockDelayMin)
		r.maxLockDelay = getIntTextField(txtFldLockDelayMax)
		r.lockResetFall = chkBoxLockDelayLockResetFall.isSelected
		r.lockResetMove = chkBoxLockDelayLockResetMove.isSelected
		r.lockResetSpin = chkBoxLockDelayLockResetSpin.isSelected
		r.lockResetWallkick = chkBoxLockDelayLockResetWallkick.isSelected
		r.lockResetLimitShareCount = chkBoxLockDelayLockResetLimitShareCount.isSelected
		r.lockResetMoveLimit = getIntTextField(txtFldLockDelayLockResetLimitMove)
		r.lockResetSpinLimit = getIntTextField(txtFldLockDelayLockResetLimitSpin)
		if(radioLockDelayLockResetLimitOverNoReset!!.isSelected)
			r.lockResetLimitOver = RuleOptions.LOCKRESET_LIMIT_OVER_NO_RESET
		if(radioLockDelayLockResetLimitOverInstant!!.isSelected)
			r.lockResetLimitOver = RuleOptions.LOCKRESET_LIMIT_OVER_INSTANT
		if(radioLockDelayLockResetLimitOverNoWallkick!!.isSelected)
			r.lockResetLimitOver = RuleOptions.LOCKRESET_LIMIT_OVER_NO_KICK

		r.minARE = getIntTextField(txtFldAREMin)
		r.maxARE = getIntTextField(txtFldAREMax)
		r.minARELine = getIntTextField(txtFldARELineMin)
		r.maxARELine = getIntTextField(txtFldARELineMax)
		r.lockFlash = getIntTextField(txtFldARELockFlash)
		r.lockFlashOnlyFrame = chkBoxARELockFlashOnlyFrame.isSelected
		r.lockFlashBeforeLineClear = chkBoxARELockFlashBeforeLineClear.isSelected
		r.areCancelMove = chkBoxARECancelMove.isSelected
		r.areCancelSpin = chkBoxARECancelSpin.isSelected
		r.areCancelHold = chkBoxARECancelHold.isSelected

		r.minLineDelay = getIntTextField(txtFldLineDelayMin)
		r.maxLineDelay = getIntTextField(txtFldLineDelayMax)
		r.lineFallAnim = chkBoxLineFallAnim.isSelected
		r.lineCancelMove = chkBoxLineCancelMove.isSelected
		r.lineCancelSpin = chkBoxLineCancelSpin.isSelected
		r.lineCancelHold = chkBoxLineCancelHold.isSelected

		r.minDAS = getIntTextField(txtFldMoveDASMin)
		r.maxDAS = getIntTextField(txtFldMoveDASMax)
		r.dasARR = getIntTextField(txtFldMoveDASDelay)
		r.dasInReady = chkBoxMoveDASInReady.isSelected
		r.dasInMoveFirstFrame = chkBoxMoveDASInMoveFirstFrame.isSelected
		r.dasInLockFlash = chkBoxMoveDASInLockFlash.isSelected
		r.dasInLineClear = chkBoxMoveDASInLineClear.isSelected
		r.dasInARE = chkBoxMoveDASInARE.isSelected
		r.dasInARELastFrame = chkBoxMoveDASInARELastFrame.isSelected
		r.dasInEndingStart = chkBoxMoveDASInEndingStart.isSelected
		r.dasChargeOnBlockedMove = chkBoxMoveDASChargeOnBlockedMove.isSelected
		r.dasStoreChargeOnNeutral = chkBoxMoveDASStoreChargeOnNeutral.isSelected
		r.dasRedirectInDelay = chkBoxMoveDASRedirectInDelay.isSelected
		r.moveFirstFrame = chkBoxMoveFirstFrame.isSelected
		r.moveDiagonal = chkBoxMoveDiagonal.isSelected
		r.moveUpAndDown = chkBoxMoveUpAndDown.isSelected
		r.moveLeftAndRightAllow = chkBoxMoveLeftAndRightAllow.isSelected
		r.moveLeftAndRightUsePreviousInput = chkBoxMoveLeftAndRightUsePreviousInput.isSelected
		r.shiftLockEnable = chkBoxMoveShiftLockEnable.isSelected
		r.pieceOffset = comboBoxPieceOffset.selectedIndex
		offsetApply()
		r.pieceOffsetX = txtFldPieceOffsetX.map {x -> x.map {j -> getIntTextField(j)}}
		r.pieceOffsetY = txtFldPieceOffsetY.map {x -> x.map {j -> getIntTextField(j)}}
		r.pieceSpawnX = txtFldPieceSpawnX.map {x -> x.map {j -> getIntTextField(j)}.toMutableList()}
		r.pieceSpawnY = txtFldPieceSpawnY.map {x -> x.map {j -> getIntTextField(j)}.toMutableList()}
		r.pieceSpawnXBig = txtFldPieceSpawnBigX.map {x -> x.map {j -> getIntTextField(j)}.toMutableList()}
		r.pieceSpawnYBig = txtFldPieceSpawnBigY.map {x -> x.map {j -> getIntTextField(j)}.toMutableList()}
		r.pieceColor = comboBoxPieceColor.map {it.selectedIndex+1}
		r.pieceDefaultDirection = comboBoxPieceDirection.map {it.selectedIndex}
	}

	private fun offsetApply() {
		val idx = comboBoxPieceOffset.selectedIndex
		if(idx==RuleOptions.PIECEOFFSET_ASSIGN) return
		txtFldPieceOffsetX.forEachIndexed {i, l ->
			l.forEachIndexed {j, it ->
				it.text = if(idx==RuleOptions.PIECEOFFSET_BIASED) "${RuleOptions.PIECEOFFSET_ARSPRESET[0][i][j]}"
				else "0"
			}
		}
		txtFldPieceOffsetY.forEachIndexed {i, l ->
			l.forEachIndexed {j, it ->
				it.text = if(idx==RuleOptions.PIECEOFFSET_BOTTOM||idx==RuleOptions.PIECEOFFSET_BIASED)
					"${RuleOptions.PIECEOFFSET_ARSPRESET[1][i][j]}" else "0"
			}
		}
	}

	/** ルールをファイルに保存
	 * @param filename Filename
	 * @throws IOException 保存に失敗したとき
	 */
	@Throws(IOException::class)
	fun save(filename:String) {
		val ruleOpt = RuleOptions().apply {
			writeRuleFromUI(this)
		}
		if(filename.endsWith(".gz"))
			GZIPOutputStream(FileOutputStream(filename, false)).bufferedWriter().use {
				it.write(Json.encodeToString(ruleOpt))
			}
		else FileOutputStream(filename, false).bufferedWriter().use {
			it.write(Json.encodeToString(ruleOpt))
		}

		log.debug("Saved rule file to $filename")
	}

	/** ルールをファイルから読み込み
	 * @param filename Filename
	 * @return ルール data
	 * @throws IOException Failed to loadしたとき
	 */
	@Throws(IOException::class)
	fun load(filename:String):RuleOptions {
		return try {
			val rf = GZIPInputStream(FileInputStream(filename))
			val ret = Json.decodeFromString<RuleOptions>(rf.bufferedReader().use {it.readText()})
			log.debug("Loaded rule Jsons from $filename")
			ret
		} catch(_:Exception) {
			val rf = GZIPInputStream(FileInputStream(filename))
			val prop = CustomProperties()
			prop.load(rf)
			rf.close()

			val ruleOpt = RuleOptions()
			ruleOpt.readProperty(prop)

			log.debug("Loaded rule Properties from $filename")
			log.debug(Json.encodeToString(ruleOpt))

			ruleOpt
		}
	}

	/** 翻訳後のUIの文字列を取得
	 * @param str 文字列
	 * @return 翻訳後のUIの文字列 (無いならそのまま[str]を返す）
	 */
	fun getUIText(str:String):String = propLang.getProperty(str, propLangDefault.getProperty(str, str))

	/** テキストfieldからint型の値を取得
	 * @param txtFld テキストfield
	 * @return テキストfieldから値を取得できた場合はその値, 失敗したら0
	 */
	private fun getIntTextField(txtFld:JTextField):Int = txtFld.text.toIntOrNull()?:0

	/** テキストfieldからfloat型の値を取得
	 * @param txtFld テキストfield
	 * @return テキストfieldから値を取得できた場合はその値, 失敗したら0f
	 */
	private fun getFloatTextField(txtFld:JTextField):Float = txtFld.text.toFloatOrNull()?:0f

	/** アクション発生時の処理 */
	override fun actionPerformed(e:ActionEvent) {
		fun saveAs() {
			val c1 = JFileChooser(strNowFile?:"${System.getProperty("user.dir")}/config/rule")
			c1.fileFilter = FileFilterRUL()
			c1.selectedFile = strNowFile?.let {File(it)}
			if(c1.showSaveDialog(this)==JFileChooser.APPROVE_OPTION) {
				val file = c1.selectedFile
				val filename = //if(!filename.endsWith(".rul")) "${file.path}.rul" else
					file.path
				try {
					save(filename)
				} catch(e2:Exception) {
					log.error("Failed to save rule data to $filename", e2)
					JOptionPane.showMessageDialog(
						this, "${getUIText("Message_FileSaveFailed")}\n$e2",
						getUIText("Title_FileSaveFailed"), JOptionPane.ERROR_MESSAGE
					)
					return
				}

				strNowFile = filename
				title = "${getUIText("Title_RuleEditor")}:$strNowFile"
			}
		}
		when(val a = e.actionCommand) {
			"New" -> {
				// 新規作成
				strNowFile = null
				title = getUIText("Title_RuleEditor")
				readRuleToUI(RuleOptions())
			}
			"Open" -> {
				// 開く
				val c = JFileChooser(strNowFile?:"${System.getProperty("user.dir")}/config/rule")
				c.fileFilter = FileFilterRUL()

				if(c.showOpenDialog(this)==JFileChooser.APPROVE_OPTION) {
					val file = c.selectedFile

					strNowFile = file.path
					title = "${getUIText("Title_RuleEditor")}:$strNowFile"

					try {
						readRuleToUI(load(file.path))
					} catch(e2:IOException) {
						log.error("Failed to load rule data from ${strNowFile!!}", e2)
						JOptionPane.showMessageDialog(
							this, "${getUIText("Message_FileLoadFailed")}\n$e2",
							getUIText("Title_FileLoadFailed"), JOptionPane.ERROR_MESSAGE
						)
						return
					}
				}
			}
			"Save" ->
				if(strNowFile!=null) {
					try {
						save(strNowFile!!)
					} catch(e2:IOException) {
						log.error("Failed to save rule data to ${strNowFile!!}", e2)
						JOptionPane.showMessageDialog(
							this, "${getUIText("Message_FileSaveFailed")}\n$e2",
							getUIText("Title_FileSaveFailed"), JOptionPane.ERROR_MESSAGE
						)
					}
				} else saveAs()

			"SaveAs" -> saveAs()

			"OffsetPreset" -> {
				val tog = comboBoxPieceOffset.selectedIndex==RuleOptions.PIECEOFFSET_ASSIGN
				txtFldPieceOffsetX.forEach {i -> for(j in i) j.isEditable = tog}
				txtFldPieceOffsetY.forEach {i -> for(j in i) j.isEditable = tog}
			}
			"OffsetApply" -> offsetApply()
			"PresetColors_SRS", "PresetColors_ARS" -> {
				val b = if(a=="PresetColors_SRS") 1 else 0
				for(i in comboBoxPieceColor.indices)
					comboBoxPieceColor[i].selectedIndex = RuleOptions.PIECECOLOR_PRESET[b][i]-1
			}
			"ResetSpawn_SRS", "ResetSpawn_ARS" -> {
				txtFldPieceSpawnX.forEach {i -> for(j in i) j.text = "0"}
				txtFldPieceSpawnY.forEach {i -> for(j in i) j.text = "0"}
				txtFldPieceSpawnBigX.forEachIndexed {i, l ->
					l.forEachIndexed {j, it ->
						it.text = if(a=="ResetSpawn_ARS")
							"${RuleOptions.PIECESPAWNXBIG_ARSPRESET[i][j]}" else "0"
					}
				}
				txtFldPieceSpawnBigY.forEach {i -> for(j in i) j.text = "0"}
			}
			"ResetDirection_SRS", "ResetDirection_ARS" -> {
				comboBoxPieceDirection.forEachIndexed {i, it ->
					it.selectedIndex = if(a=="ResetDirection_ARS") RuleOptions.PIECEDIRECTION_ARSPRESET[i] else 0
				}
			}
			"ResetRandomizer" // NEXT順生成アルゴリズムの選択リセット
				-> comboBoxRandomizer!!.setSelectedItem(null)
			"Exit" // 終了
				-> dispose()
		}
	}

	/** ファイル選択画面のフィルタ */
	private inner class FileFilterRUL:FileFilter() {
		override fun accept(f:File):Boolean = if(f.isDirectory) true else f.name.endsWith(".rul")||f.name.endsWith(".rul.gz")

		override fun getDescription():String = getUIText("FileChooser_RuleFile")
	}

	/** 画像表示Comboボックスの項目<br></br>
	 * [出典](http://www.javadrive.jp/tutorial/jcomboBox/index20.html) */
	private class ComboLabel(
		var text:String = "",
		var icon:Icon? = null
	)

	/** 画像表示ComboボックスのListCellRenderer<br></br>
	 * [出典](http://www.javadrive.jp/tutorial/jcomboBox/index20.html) */
	private inner class ComboLabelCellRenderer:JLabel(), ListCellRenderer<Any> {
		/*fun getListCellRendererComponent(list:JList<out Nothing>?, value:Nothing?, index:Int, isSelected:Boolean,
			cellHasFocus:Boolean):Component {
			TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
		}*/

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
