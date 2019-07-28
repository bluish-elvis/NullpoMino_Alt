/* Copyright (c) 2010, NullNoname
 * All rights reserved.
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * Neither the name of NullNoname nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
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
 * POSSIBILITY OF SUCH DAMAGE. */
package mu.nu.nullpo.tool.ruleeditor

import mu.nu.nullpo.game.component.*
import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.util.CustomProperties
import org.apache.log4j.Logger
import org.apache.log4j.PropertyConfigurator
import java.awt.*
import java.awt.event.*
import java.awt.image.BufferedImage
import java.io.*
import java.net.MalformedURLException
import java.net.URL
import java.util.*
import javax.imageio.ImageIO
import javax.swing.*
import javax.swing.filechooser.FileFilter

/** ルールエディター */
class RuleEditor:JFrame, ActionListener {

	/** Swing版のSave settings用Property file */
	val propConfig:CustomProperties = CustomProperties()

	/** Default language file */
	val propLangDefault:CustomProperties = CustomProperties()

	/** UI翻訳用Property file */
	val propLang:CustomProperties = CustomProperties()

	//----------------------------------------------------------------------
	/** 今開いているFilename (null:なし) */
	private var strNowFile:String? = null

	/** タブ */
	private var tabPane:JTabbedPane = JTabbedPane()

	//----------------------------------------------------------------------
	/* 基本設定パネル */

	/** Rule name */
	private var txtfldRuleName:JTextField? = null

	/** NEXT表示countのテキストfield */
	private var txtfldNextDisplay:JTextField? = null

	/** Game style combobox */
	private var comboboxStyle:JComboBox<*>? = null

	/** 絵柄のComboボックス */
	private var comboboxSkin:JComboBox<*>? = null

	/** ghost is enabled */
	private var chkboxGhost:JCheckBox? = null

	/** Blockピースがfield枠外から出現 */
	private var chkboxEnterAboveField:JCheckBox? = null

	/** 出現予定地が埋まっているときにY-coordinateを上にずらすMaximum count */
	private var txtfldEnterMaxDistanceY:JTextField? = null

	/** NEXT順生成アルゴリズム */
	private var comboboxRandomizer:JComboBox<*>? = null

	/** NEXT順生成アルゴリズムのリスト */
	private var vectorRandomizer:Vector<String>? = null

	/** NEXT順生成アルゴリズムのリセット button
	 * private JButton btnResetRandomizer; */

	//----------------------------------------------------------------------
	/* field設定パネル */

	/** fieldの幅 */
	private var txtfldFieldWidth:JTextField? = null

	/** Field height */
	private var txtfldFieldHeight:JTextField? = null

	/** fieldの見えない部分の高さ */
	private var txtfldFieldHiddenHeight:JTextField? = null

	/** fieldの天井 */
	private var chkboxFieldCeiling:JCheckBox? = null

	/** field枠内に置けないと死亡 */
	private var chkboxFieldLockoutDeath:JCheckBox? = null

	/** field枠外にはみ出しただけで死亡 */
	private var chkboxFieldPartialLockoutDeath:JCheckBox? = null

	//----------------------------------------------------------------------
	/* ホールド設定パネル */

	/** ホールド is enabled */
	private var chkboxHoldEnable:JCheckBox? = null

	/** 先行ホールド */
	private var chkboxHoldInitial:JCheckBox? = null

	/** 先行ホールド連続使用不可 */
	private var chkboxHoldInitialLimit:JCheckBox? = null

	/** ホールドを使ったときにBlockピースの向きを初期状態に戻す */
	private var chkboxHoldResetDirection:JCheckBox? = null

	/** ホールドできる count (-1:無制限) */
	private var txtfldHoldLimit:JTextField? = null

	//----------------------------------------------------------------------
	/* ドロップ設定パネル */

	/** Hard drop使用可否 */
	private var chkboxDropHardDropEnable:JCheckBox? = null

	/** Hard dropで即固定 */
	private var chkboxDropHardDropLock:JCheckBox? = null

	/** Hard drop連続使用不可 */
	private var chkboxDropHardDropLimit:JCheckBox? = null

	/** Soft drop使用可否 */
	private var chkboxDropSoftDropEnable:JCheckBox? = null

	/** Soft dropで即固定 */
	private var chkboxDropSoftDropLock:JCheckBox? = null

	/** Soft drop連続使用不可 */
	private var chkboxDropSoftDropLimit:JCheckBox? = null

	/** 接地状態でSoft dropすると即固定 */
	private var chkboxDropSoftDropSurfaceLock:JCheckBox? = null

	/** Soft drop速度 */
	private var txtfldDropSoftDropSpeed:JTextField? = null

	/** Soft drop速度をCurrent 通常速度×n倍にする */
	private var chkboxDropSoftDropMultiplyNativeSpeed:JCheckBox? = null

	/** Use new soft drop codes */
	private var chkboxDropSoftDropGravitySpeedLimit:JCheckBox? = null

	//----------------------------------------------------------------------
	/* rotation設定パネル */

	/** 先行rotation */
	private var chkboxRotateInitial:JCheckBox? = null

	/** 先行rotation連続使用不可 */
	private var chkboxRotateInitialLimit:JCheckBox? = null

	/** Wallkick */
	private var chkboxRotateWallkick:JCheckBox? = null

	/** 先行rotationでもWallkickする */
	private var chkboxRotateInitialWallkick:JCheckBox? = null

	/** 上DirectionへのWallkickができる count (-1:無限) */
	private var txtfldRotateMaxUpwardWallkick:JTextField? = null

	/** falseなら左が正rotation, When true,右が正rotation */
	private var chkboxRotateButtonDefaultRight:JCheckBox? = null

	/** 逆rotationを許可 (falseなら正rotationと同じ) */
	private var chkboxRotateButtonAllowReverse:JCheckBox? = null

	/** 2rotationを許可 (falseなら正rotationと同じ) */
	private var chkboxRotateButtonAllowDouble:JCheckBox? = null

	/** Wallkickアルゴリズム */
	private var comboboxWallkickSystem:JComboBox<*>? = null

	/** Wallkickアルゴリズムのリスト */
	private var vectorWallkickSystem:Vector<String>? = null

	/** Wallkickアルゴリズムのリセット button */
	private var btnResetWallkickSystem:JButton? = null

	//----------------------------------------------------------------------
	/* 固定 time設定パネル */

	/** 最低固定 time */
	private var txtfldLockDelayMin:JTextField? = null

	/** 最高固定 time */
	private var txtfldLockDelayMax:JTextField? = null

	/** 落下で固定 timeリセット */
	private var chkboxLockDelayLockResetFall:JCheckBox? = null

	/** 移動で固定 timeリセット */
	private var chkboxLockDelayLockResetMove:JCheckBox? = null

	/** rotationで固定 timeリセット */
	private var chkboxLockDelayLockResetRotate:JCheckBox? = null

	/** Lock delay reset by wallkick */
	private var chkboxLockDelayLockResetWallkick:JCheckBox? = null

	/** 横移動 counterとrotation counterを共有 (横移動 counterだけ使う) */
	private var chkboxLockDelayLockResetLimitShareCount:JCheckBox? = null

	/** 横移動 count制限 */
	private var txtfldLockDelayLockResetLimitMove:JTextField? = null

	/** rotation count制限 */
	private var txtfldLockDelayLockResetLimitRotate:JTextField? = null

	/** 横移動 counterかrotation counterが超過したら固定 timeリセットを無効にする */
	private var radioLockDelayLockResetLimitOverNoReset:JRadioButton? = null

	/** 横移動 counterかrotation counterが超過したら即座に固定する */
	private var radioLockDelayLockResetLimitOverInstant:JRadioButton? = null

	/** 横移動 counterかrotation counterが超過したらWallkick無効にする */
	private var radioLockDelayLockResetLimitOverNoWallkick:JRadioButton? = null

	//----------------------------------------------------------------------
	/* ARE設定パネル */

	/** 最低ARE */
	private var txtfldAREMin:JTextField? = null

	/** 最高ARE */
	private var txtfldAREMax:JTextField? = null

	/** 最低ARE after line clear */
	private var txtfldARELineMin:JTextField? = null

	/** 最高ARE after line clear */
	private var txtfldARELineMax:JTextField? = null

	/** 固定した瞬間に光る frame count */
	private var txtfldARELockFlash:JTextField? = null

	/** Blockが光る専用 frame を入れる */
	private var chkboxARELockFlashOnlyFrame:JCheckBox? = null

	/** Line clear前にBlockが光る frame を入れる */
	private var chkboxARELockFlashBeforeLineClear:JCheckBox? = null

	/** ARE cancel on move checkbox */
	private var chkboxARECancelMove:JCheckBox? = null

	/** ARE cancel on rotate checkbox */
	private var chkboxARECancelRotate:JCheckBox? = null

	/** ARE cancel on hold checkbox */
	private var chkboxARECancelHold:JCheckBox? = null

	//----------------------------------------------------------------------
	/* Line clear設定パネル */

	/** 最低Line clear time */
	private var txtfldLineDelayMin:JTextField? = null

	/** 最高Line clear time */
	private var txtfldLineDelayMax:JTextField? = null

	/** 落下アニメ */
	private var chkboxLineFallAnim:JCheckBox? = null

	/** Line delay cancel on move checkbox */
	private var chkboxLineCancelMove:JCheckBox? = null

	/** Line delay cancel on rotate checkbox */
	private var chkboxLineCancelRotate:JCheckBox? = null

	/** Line delay cancel on hold checkbox */
	private var chkboxLineCancelHold:JCheckBox? = null

	//----------------------------------------------------------------------
	/* 移動設定パネル */

	/** 最低横溜め time */
	private var txtfldMoveDASMin:JTextField? = null

	/** 最高横溜め time */
	private var txtfldMoveDASMax:JTextField? = null

	/** 横移動間隔 */
	private var txtfldMoveDASDelay:JTextField? = null

	/** Ready画面で横溜め可能 */
	private var chkboxMoveDASInReady:JCheckBox? = null

	/** 最初の frame で横溜め可能 */
	private var chkboxMoveDASInMoveFirstFrame:JCheckBox? = null

	/** Blockが光った瞬間に横溜め可能 */
	private var chkboxMoveDASInLockFlash:JCheckBox? = null

	/** Line clear中に横溜め可能 */
	private var chkboxMoveDASInLineClear:JCheckBox? = null

	/** ARE中に横溜め可能 */
	private var chkboxMoveDASInARE:JCheckBox? = null

	/** AREの最後の frame で横溜め可能 */
	private var chkboxMoveDASInARELastFrame:JCheckBox? = null

	/** Ending突入画面で横溜め可能 */
	private var chkboxMoveDASInEndingStart:JCheckBox? = null

	/** DAS charge on blocked move checkbox */
	private var chkboxMoveDASChargeOnBlockedMove:JCheckBox? = null

	/** Store DAS Charge on neutral checkbox */
	private var chkboxMoveDASStoreChargeOnNeutral:JCheckBox? = null

	/** Redirect in delay checkbox */
	private var chkboxMoveDASRedirectInDelay:JCheckBox? = null

	/** 最初の frame に移動可能 */
	private var chkboxMoveFirstFrame:JCheckBox? = null

	/** 斜め移動 */
	private var chkboxMoveDiagonal:JCheckBox? = null

	/** 上下同時押し可能 */
	private var chkboxMoveUpAndDown:JCheckBox? = null

	/** 左右同時押し可能 */
	private var chkboxMoveLeftAndRightAllow:JCheckBox? = null

	/** 左右同時押ししたときに前の frame の input Directionを優先する */
	private var chkboxMoveLeftAndRightUsePreviousInput:JCheckBox? = null

	/** Shift lock checkbox */
	private var chkboxMoveShiftLockEnable:JCheckBox? = null

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

		var ruleopt = RuleOptions()

		if(filename!=null&&filename.isNotEmpty())
			try {
				ruleopt = load(filename)
				strNowFile = filename
				title = "${getUIText("Title_RuleEditor")}:$strNowFile"
			} catch(e:IOException) {
				log.error("Failed to load rule data from $filename", e)
				JOptionPane.showMessageDialog(this, "${getUIText("Message_FileLoadFailed")}\n$e",
					getUIText("Title_FileLoadFailed"), JOptionPane.ERROR_MESSAGE)
			}

		readRuleToUI(ruleopt)

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
		val miNew = JMenuItem(getUIText("JMenuItem_New"))
		miNew.setMnemonic('N')
		miNew.accelerator = KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK)
		miNew.actionCommand = "New"
		miNew.addActionListener(this)
		mFile.add(miNew)

		// 開く
		val miOpen = JMenuItem(getUIText("JMenuItem_Open"))
		miOpen.setMnemonic('O')
		miOpen.accelerator = KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK)
		miOpen.actionCommand = "Open"
		miOpen.addActionListener(this)
		mFile.add(miOpen)

		// Up書き保存
		val miSave = JMenuItem(getUIText("JMenuItem_Save"))
		miSave.setMnemonic('S')
		miSave.accelerator = KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK)
		miSave.actionCommand = "Save"
		miSave.addActionListener(this)
		mFile.add(miSave)

		// Nameを付けて保存
		val miSaveAs = JMenuItem(getUIText("JMenuItem_SaveAs"))
		miSaveAs.setMnemonic('A')
		miSaveAs.accelerator = KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK or InputEvent.ALT_DOWN_MASK)
		miSaveAs.actionCommand = "SaveAs"
		miSaveAs.addActionListener(this)
		mFile.add(miSaveAs)

		// 終了
		val miExit = JMenuItem(getUIText("JMenuItem_Exit"))
		miExit.setMnemonic('X')
		miExit.actionCommand = "Exit"
		miExit.addActionListener(this)
		mFile.add(miExit)

		// タブ全体 --------------------------------------------------
		tabPane = JTabbedPane()
		contentPane.add(tabPane, BorderLayout.NORTH)

		// 基本設定タブ --------------------------------------------------
		val panelBasic = JPanel()
		panelBasic.layout = BoxLayout(panelBasic, BoxLayout.Y_AXIS)
		tabPane.addTab(getUIText("TabName_Basic"), panelBasic)

		// Rule name
		val pRuleName = JPanel()
		panelBasic.add(pRuleName)

		val lRuleName = JLabel(getUIText("Basic_RuleName"))
		pRuleName.add(lRuleName)

		txtfldRuleName = JTextField("", 15)
		pRuleName.add(txtfldRuleName)

		// NEXT表示count
		val pNextDisplay = JPanel()
		panelBasic.add(pNextDisplay)

		val lNextDisplay = JLabel(getUIText("Basic_NextDisplay"))
		pNextDisplay.add(lNextDisplay)

		txtfldNextDisplay = JTextField("", 5)
		pNextDisplay.add(txtfldNextDisplay)

		// Game style
		val pStyle = JPanel()
		panelBasic.add(pStyle)

		val lStyle = JLabel(getUIText("Basic_Style"))
		pStyle.add(lStyle)

		comboboxStyle = JComboBox(GameEngine.GAMESTYLE_NAMES)
		comboboxStyle!!.preferredSize = Dimension(100, 30)
		pStyle.add(comboboxStyle)

		// 絵柄
		val pSkin = JPanel()
		panelBasic.add(pSkin)

		val lSkin = JLabel(getUIText("Basic_Skin"))
		pSkin.add(lSkin)

		val model = DefaultComboBoxModel<ComboLabel>()
		imgBlockSkins!!.indices.forEach {i ->
			model.addElement(ComboLabel("$i", ImageIcon(imgBlockSkins!![i])))
		}
		comboboxSkin = JComboBox(model).apply {
			renderer = ComboLabelCellRenderer()
			preferredSize = Dimension(190, 30)
		}
		pSkin.add(comboboxSkin)

		// ghost
		chkboxGhost = JCheckBox(getUIText("Basic_Ghost"))
		panelBasic.add(chkboxGhost)

		// field枠外から出現
		chkboxEnterAboveField = JCheckBox(getUIText("Basic_EnterAboveField"))
		panelBasic.add(chkboxEnterAboveField)

		// 出現予定地が埋まっているときにY-coordinateを上にずらすMaximum count
		val pEnterMaxDistanceY = JPanel()
		panelBasic.add(pEnterMaxDistanceY)

		val lEnterMaxDistanceY = JLabel(getUIText("Basic_EnterMaxDistanceY"))
		pEnterMaxDistanceY.add(lEnterMaxDistanceY)

		txtfldEnterMaxDistanceY = JTextField("", 5)
		pEnterMaxDistanceY.add(txtfldEnterMaxDistanceY)

		// NEXT順生成アルゴリズム
		val pRandomizer = JPanel()
		panelBasic.add(pRandomizer)

		val lRandomizer = JLabel(getUIText("Basic_Randomizer"))
		pRandomizer.add(lRandomizer)

		vectorRandomizer = getTextFileVector("config/list/randomizer.lst")
		comboboxRandomizer = JComboBox(createShortStringVector(vectorRandomizer)).apply{
		preferredSize = Dimension(200, 30)
		pRandomizer.add(this)}

		val btnResetRandomizer = JButton(getUIText("Basic_Reset")).also{
		it.setMnemonic('R')
		it.actionCommand = "ResetRandomizer"
		it.addActionListener(this)
		pRandomizer.add(it)}

		// fieldタブ --------------------------------------------------
		val panelField = JPanel()
		panelField.layout = BoxLayout(panelField, BoxLayout.Y_AXIS)
		tabPane.addTab(getUIText("TabName_Field"), panelField)

		// fieldの幅
		val pFieldWidth = JPanel()
		panelField.add(pFieldWidth)

		val lFieldWidth = JLabel(getUIText("Field_FieldWidth"))
		pFieldWidth.add(lFieldWidth)

		txtfldFieldWidth = JTextField("", 5)
		pFieldWidth.add(txtfldFieldWidth)

		// Field height
		val pFieldHeight = JPanel()
		panelField.add(pFieldHeight)

		val lFieldHeight = JLabel(getUIText("Field_FieldHeight"))
		pFieldHeight.add(lFieldHeight)

		txtfldFieldHeight = JTextField("", 5)
		pFieldHeight.add(txtfldFieldHeight)

		// fieldの見えない部分の高さ
		val pFieldHiddenHeight = JPanel()
		panelField.add(pFieldHiddenHeight)

		val lFieldHiddenHeight = JLabel(getUIText("Field_FieldHiddenHeight"))
		pFieldHiddenHeight.add(lFieldHiddenHeight)

		txtfldFieldHiddenHeight = JTextField("", 5)
		pFieldHiddenHeight.add(txtfldFieldHiddenHeight)

		// fieldの天井
		chkboxFieldCeiling = JCheckBox(getUIText("Field_FieldCeiling"))
		panelField.add(chkboxFieldCeiling)

		// field枠内に置けないと死亡
		chkboxFieldLockoutDeath = JCheckBox(getUIText("Field_FieldLockoutDeath"))
		panelField.add(chkboxFieldLockoutDeath)

		// field枠外にはみ出しただけで死亡
		chkboxFieldPartialLockoutDeath = JCheckBox(getUIText("Field_FieldPartialLockoutDeath"))
		panelField.add(chkboxFieldPartialLockoutDeath)

		// ホールドタブ --------------------------------------------------
		val panelHold = JPanel()
		panelHold.layout = BoxLayout(panelHold, BoxLayout.Y_AXIS)
		tabPane.addTab(getUIText("TabName_Hold"), panelHold)

		// ホールド is enabled
		chkboxHoldEnable = JCheckBox(getUIText("Hold_HoldEnable"))
		panelHold.add(chkboxHoldEnable)

		// 先行ホールド
		chkboxHoldInitial = JCheckBox(getUIText("Hold_HoldInitial"))
		panelHold.add(chkboxHoldInitial)

		// 先行ホールド連続使用不可
		chkboxHoldInitialLimit = JCheckBox(getUIText("Hold_HoldInitialLimit"))
		panelHold.add(chkboxHoldInitialLimit)

		// ホールドを使ったときにBlockピースの向きを初期状態に戻す
		chkboxHoldResetDirection = JCheckBox(getUIText("Hold_HoldResetDirection"))
		panelHold.add(chkboxHoldResetDirection)

		// ホールドできる count
		val pHoldLimit = JPanel()
		panelHold.add(pHoldLimit)

		val lHoldLimit = JLabel(getUIText("Hold_HoldLimit"))
		pHoldLimit.add(lHoldLimit)

		txtfldHoldLimit = JTextField("", 5)
		pHoldLimit.add(txtfldHoldLimit)

		// ドロップタブ --------------------------------------------------
		val panelDrop = JPanel()
		panelDrop.layout = BoxLayout(panelDrop, BoxLayout.Y_AXIS)
		tabPane.addTab(getUIText("TabName_Drop"), panelDrop)

		// Hard drop使用可否
		chkboxDropHardDropEnable = JCheckBox(getUIText("Drop_HardDropEnable"))
		panelDrop.add(chkboxDropHardDropEnable)

		// Hard dropで即固定
		chkboxDropHardDropLock = JCheckBox(getUIText("Drop_HardDropLock"))
		panelDrop.add(chkboxDropHardDropLock)

		// Hard drop連続使用不可
		chkboxDropHardDropLimit = JCheckBox(getUIText("Drop_HardDropLimit"))
		panelDrop.add(chkboxDropHardDropLimit)

		// Soft drop使用可否
		chkboxDropSoftDropEnable = JCheckBox(getUIText("Drop_SoftDropEnable"))
		panelDrop.add(chkboxDropSoftDropEnable)

		// Soft dropで即固定
		chkboxDropSoftDropLock = JCheckBox(getUIText("Drop_SoftDropLock"))
		panelDrop.add(chkboxDropSoftDropLock)

		// Soft drop連続使用不可
		chkboxDropSoftDropLimit = JCheckBox(getUIText("Drop_SoftDropLimit"))
		panelDrop.add(chkboxDropSoftDropLimit)

		// 接地状態でSoft dropすると即固定
		chkboxDropSoftDropSurfaceLock = JCheckBox(getUIText("Drop_SoftDropSurfaceLock"))
		panelDrop.add(chkboxDropSoftDropSurfaceLock)

		// Soft drop速度をCurrent 通常速度×n倍にする
		chkboxDropSoftDropMultiplyNativeSpeed = JCheckBox(getUIText("Drop_SoftDropMultiplyNativeSpeed"))
		panelDrop.add(chkboxDropSoftDropMultiplyNativeSpeed)

		// Use new soft drop codes
		chkboxDropSoftDropGravitySpeedLimit = JCheckBox(getUIText("Drop_SoftDropGravitySpeedLimit"))
		panelDrop.add(chkboxDropSoftDropGravitySpeedLimit)

		// Soft drop速度
		val pDropSoftDropSpeed = JPanel()
		panelDrop.add(pDropSoftDropSpeed)
		val lDropSoftDropSpeed = JLabel(getUIText("Drop_SoftDropSpeed"))
		pDropSoftDropSpeed.add(lDropSoftDropSpeed)

		txtfldDropSoftDropSpeed = JTextField("", 5)
		pDropSoftDropSpeed.add(txtfldDropSoftDropSpeed)

		// rotationタブ --------------------------------------------------
		val panelRotate = JPanel()
		panelRotate.layout = BoxLayout(panelRotate, BoxLayout.Y_AXIS)
		tabPane.addTab(getUIText("TabName_Rotate"), panelRotate)

		// 先行rotation
		chkboxRotateInitial = JCheckBox(getUIText("Rotate_RotateInitial"))
		panelRotate.add(chkboxRotateInitial)

		// 先行rotation連続使用不可
		chkboxRotateInitialLimit = JCheckBox(getUIText("Rotate_RotateInitialLimit"))
		panelRotate.add(chkboxRotateInitialLimit)

		// Wallkick
		chkboxRotateWallkick = JCheckBox(getUIText("Rotate_RotateWallkick"))
		panelRotate.add(chkboxRotateWallkick)

		// 先行rotationでもWallkickする
		chkboxRotateInitialWallkick = JCheckBox(getUIText("Rotate_RotateInitialWallkick"))
		panelRotate.add(chkboxRotateInitialWallkick)

		// Aで右rotation
		chkboxRotateButtonDefaultRight = JCheckBox(getUIText("Rotate_RotateButtonDefaultRight"))
		panelRotate.add(chkboxRotateButtonDefaultRight)

		// 逆rotation許可
		chkboxRotateButtonAllowReverse = JCheckBox(getUIText("Rotate_RotateButtonAllowReverse"))
		panelRotate.add(chkboxRotateButtonAllowReverse)

		// 2rotation許可
		chkboxRotateButtonAllowDouble = JCheckBox(getUIText("Rotate_RotateButtonAllowDouble"))
		panelRotate.add(chkboxRotateButtonAllowDouble)

		// UpDirectionへWallkickできる count
		val pRotateMaxUpwardWallkick = JPanel()
		panelRotate.add(pRotateMaxUpwardWallkick)
		val lRotateMaxUpwardWallkick = JLabel(getUIText("Rotate_RotateMaxUpwardWallkick"))
		pRotateMaxUpwardWallkick.add(lRotateMaxUpwardWallkick)

		txtfldRotateMaxUpwardWallkick = JTextField("", 5)
		pRotateMaxUpwardWallkick.add(txtfldRotateMaxUpwardWallkick)

		// Wallkickアルゴリズム
		val pWallkickSystem = JPanel()
		panelRotate.add(pWallkickSystem)

		val lWallkickSystem = JLabel(getUIText("Rotate_WallkickSystem"))
		pWallkickSystem.add(lWallkickSystem)

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
		val lLockDelayMin = JLabel(getUIText("LockDelay_LockDelayMinMax"))
		panelLockDelay.add(lLockDelayMin)

		val pLockDelayMinMax = JPanel()
		panelLockDelay.add(pLockDelayMinMax)

		txtfldLockDelayMin = JTextField("", 5)
		pLockDelayMinMax.add(txtfldLockDelayMin)
		txtfldLockDelayMax = JTextField("", 5)
		pLockDelayMinMax.add(txtfldLockDelayMax)

		// 落下で固定 timeリセット
		chkboxLockDelayLockResetFall = JCheckBox(getUIText("LockDelay_LockResetFall"))
		panelLockDelay.add(chkboxLockDelayLockResetFall)

		// 移動で固定 timeリセット
		chkboxLockDelayLockResetMove = JCheckBox(getUIText("LockDelay_LockResetMove"))
		panelLockDelay.add(chkboxLockDelayLockResetMove)

		// rotationで固定 timeリセット
		chkboxLockDelayLockResetRotate = JCheckBox(getUIText("LockDelay_LockResetRotate"))
		panelLockDelay.add(chkboxLockDelayLockResetRotate)

		// Lock delay reset by wallkick
		chkboxLockDelayLockResetWallkick = JCheckBox(getUIText("LockDelay_LockResetWallkick"))
		panelLockDelay.add(chkboxLockDelayLockResetWallkick)

		// 横移動 counterとrotation counterを共有 (横移動 counterだけ使う）
		chkboxLockDelayLockResetLimitShareCount = JCheckBox(getUIText("LockDelay_LockDelayLockResetLimitShareCount"))
		panelLockDelay.add(chkboxLockDelayLockResetLimitShareCount)

		// 横移動 count制限
		val pLockDelayLockResetLimitMove = JPanel()
		panelLockDelay.add(pLockDelayLockResetLimitMove)
		val lLockDelayLockResetLimitMove = JLabel(getUIText("LockDelay_LockDelayLockResetLimitMove"))
		pLockDelayLockResetLimitMove.add(lLockDelayLockResetLimitMove)

		txtfldLockDelayLockResetLimitMove = JTextField("", 5)
		pLockDelayLockResetLimitMove.add(txtfldLockDelayLockResetLimitMove)

		// rotation count制限
		val pLockDelayLockResetLimitRotate = JPanel()
		panelLockDelay.add(pLockDelayLockResetLimitRotate)
		val lLockDelayLockResetLimitRotate = JLabel(getUIText("LockDelay_LockDelayLockResetLimitRotate"))
		pLockDelayLockResetLimitRotate.add(lLockDelayLockResetLimitRotate)

		txtfldLockDelayLockResetLimitRotate = JTextField("", 5)
		pLockDelayLockResetLimitRotate.add(txtfldLockDelayLockResetLimitRotate)

		// 移動またはrotation count制限が超過した時の設定
		val pLockDelayLockResetLimitOver = JPanel()
		pLockDelayLockResetLimitOver.layout = BoxLayout(pLockDelayLockResetLimitOver, BoxLayout.Y_AXIS)
		panelLockDelay.add(pLockDelayLockResetLimitOver)

		val lLockDelayLockResetLimitOver = JLabel(getUIText("LockDelay_LockDelayLockResetLimitOver"))
		pLockDelayLockResetLimitOver.add(lLockDelayLockResetLimitOver)

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
		val lAREMin = JLabel(getUIText("ARE_MinMax"))
		panelARE.add(lAREMin)

		val pAREMinMax = JPanel()
		panelARE.add(pAREMinMax)

		txtfldAREMin = JTextField("", 5)
		pAREMinMax.add(txtfldAREMin)
		txtfldAREMax = JTextField("", 5)
		pAREMinMax.add(txtfldAREMax)

		// 最低ARE after line clearと最高ARE after line clear
		val lARELineMin = JLabel(getUIText("ARE_LineMinMax"))
		panelARE.add(lARELineMin)

		val pARELineMinMax = JPanel()
		panelARE.add(pARELineMinMax)

		txtfldARELineMin = JTextField("", 5)
		pARELineMinMax.add(txtfldARELineMin)
		txtfldARELineMax = JTextField("", 5)
		pARELineMinMax.add(txtfldARELineMax)

		// 固定した瞬間に光る frame count
		val lARELockFlash = JLabel(getUIText("ARE_LockFlash"))
		panelARE.add(lARELockFlash)

		val pARELockFlash = JPanel()
		panelARE.add(pARELockFlash)

		txtfldARELockFlash = JTextField("", 5)
		pARELockFlash.add(txtfldARELockFlash)

		// Blockが光る専用 frame を入れる
		chkboxARELockFlashOnlyFrame = JCheckBox(getUIText("ARE_LockFlashOnlyFrame"))
		panelARE.add(chkboxARELockFlashOnlyFrame)

		// Line clear前にBlockが光る frame を入れる
		chkboxARELockFlashBeforeLineClear = JCheckBox(getUIText("ARE_LockFlashBeforeLineClear"))
		panelARE.add(chkboxARELockFlashBeforeLineClear)

		// ARE cancel on move
		chkboxARECancelMove = JCheckBox(getUIText("ARE_CancelMove"))
		panelARE.add(chkboxARECancelMove)

		// ARE cancel on move
		chkboxARECancelRotate = JCheckBox(getUIText("ARE_CancelRotate"))
		panelARE.add(chkboxARECancelRotate)

		// ARE cancel on move
		chkboxARECancelHold = JCheckBox(getUIText("ARE_CancelHold"))
		panelARE.add(chkboxARECancelHold)

		// Line clearタブ --------------------------------------------------
		val panelLine = JPanel()
		panelLine.layout = BoxLayout(panelLine, BoxLayout.Y_AXIS)
		tabPane.addTab(getUIText("TabName_Line"), panelLine)

		// 最低Line clear timeと最高Line clear time
		val lLineMin = JLabel(getUIText("Line_MinMax"))
		panelLine.add(lLineMin)

		val pLineMinMax = JPanel()
		panelLine.add(pLineMinMax)

		txtfldLineDelayMin = JTextField("", 5)
		pLineMinMax.add(txtfldLineDelayMin)
		txtfldLineDelayMax = JTextField("", 5)
		pLineMinMax.add(txtfldLineDelayMax)

		// 落下アニメ
		chkboxLineFallAnim = JCheckBox(getUIText("Line_FallAnim"))
		panelLine.add(chkboxLineFallAnim)

		// Line delay cancel on move
		chkboxLineCancelMove = JCheckBox(getUIText("Line_CancelMove"))
		panelLine.add(chkboxLineCancelMove)

		// Line delay cancel on rotate
		chkboxLineCancelRotate = JCheckBox(getUIText("Line_CancelRotate"))
		panelLine.add(chkboxLineCancelRotate)

		// Line delay cancel on hold
		chkboxLineCancelHold = JCheckBox(getUIText("Line_CancelHold"))
		panelLine.add(chkboxLineCancelHold)

		// 移動タブ --------------------------------------------------
		val panelMove = JPanel()
		panelMove.layout = BoxLayout(panelMove, BoxLayout.Y_AXIS)
		tabPane.addTab(getUIText("TabName_Move"), panelMove)

		// 最低横溜め timeと最高横溜め time
		val lMoveDASMin = JLabel(getUIText("Move_DASMinMax"))
		panelMove.add(lMoveDASMin)

		val pMoveDASMinMax = JPanel()
		panelMove.add(pMoveDASMinMax)

		txtfldMoveDASMin = JTextField("", 5)
		pMoveDASMinMax.add(txtfldMoveDASMin)
		txtfldMoveDASMax = JTextField("", 5)
		pMoveDASMinMax.add(txtfldMoveDASMax)

		// 横移動間隔
		val pMoveDASDelay = JPanel()
		panelMove.add(pMoveDASDelay)

		val lMoveDASDelay1 = JLabel(getUIText("Move_DASDelay1"))
		pMoveDASDelay.add(lMoveDASDelay1)

		txtfldMoveDASDelay = JTextField("", 5)
		pMoveDASDelay.add(txtfldMoveDASDelay)

		val lMoveDASDelay2 = JLabel(getUIText("Move_DASDelay2"))
		pMoveDASDelay.add(lMoveDASDelay2)

		// ○○のとき横溜め可能
		chkboxMoveDASInReady = JCheckBox(getUIText("Move_DASInReady"))
		panelMove.add(chkboxMoveDASInReady)
		chkboxMoveDASInMoveFirstFrame = JCheckBox(getUIText("Move_DASInMoveFirstFrame"))
		panelMove.add(chkboxMoveDASInMoveFirstFrame)
		chkboxMoveDASInLockFlash = JCheckBox(getUIText("Move_DASInLockFlash"))
		panelMove.add(chkboxMoveDASInLockFlash)
		chkboxMoveDASInLineClear = JCheckBox(getUIText("Move_DASInLineClear"))
		panelMove.add(chkboxMoveDASInLineClear)
		chkboxMoveDASInARE = JCheckBox(getUIText("Move_DASInARE"))
		panelMove.add(chkboxMoveDASInARE)
		chkboxMoveDASInARELastFrame = JCheckBox(getUIText("Move_DASInARELastFrame"))
		panelMove.add(chkboxMoveDASInARELastFrame)
		chkboxMoveDASInEndingStart = JCheckBox(getUIText("Move_DASInEndingStart"))
		panelMove.add(chkboxMoveDASInEndingStart)
		chkboxMoveDASChargeOnBlockedMove = JCheckBox(getUIText("Move_DASChargeOnBlockedMove"))
		panelMove.add(chkboxMoveDASChargeOnBlockedMove)
		chkboxMoveDASStoreChargeOnNeutral = JCheckBox(getUIText("Move_DASStoreChargeOnNeutral"))
		panelMove.add(chkboxMoveDASStoreChargeOnNeutral)
		chkboxMoveDASRedirectInDelay = JCheckBox(getUIText("Move_DASRedirectInDelay"))
		panelMove.add(chkboxMoveDASRedirectInDelay)

		// 最初の frame に移動可能
		chkboxMoveFirstFrame = JCheckBox(getUIText("Move_FirstFrame"))
		panelMove.add(chkboxMoveFirstFrame)

		// 斜め移動
		chkboxMoveDiagonal = JCheckBox(getUIText("Move_Diagonal"))
		panelMove.add(chkboxMoveDiagonal)

		// Up下同時押し
		chkboxMoveUpAndDown = JCheckBox(getUIText("Move_UpAndDown"))
		panelMove.add(chkboxMoveUpAndDown)

		// 左右同時押し
		chkboxMoveLeftAndRightAllow = JCheckBox(getUIText("Move_LeftAndRightAllow"))
		panelMove.add(chkboxMoveLeftAndRightAllow)

		// 左右同時押ししたときに前 frame の input を優先
		chkboxMoveLeftAndRightUsePreviousInput = JCheckBox(getUIText("Move_LeftAndRightUsePreviousInput"))
		panelMove.add(chkboxMoveLeftAndRightUsePreviousInput)

		// Shift lock
		chkboxMoveShiftLockEnable = JCheckBox(getUIText("Move_ShiftLock"))
		panelMove.add(chkboxMoveShiftLockEnable)

		// rotationパターン補正タブ ------------------------------------------------
		val panelPieceOffset = JPanel()
		panelPieceOffset.layout = BoxLayout(panelPieceOffset, BoxLayout.Y_AXIS)
		tabPane.addTab(getUIText("TabName_PieceOffset"), panelPieceOffset)
		comboboxPieceOffset = JComboBox(RuleOptions.PIECEOFFSET_NAME.map{getUIText(it)}.toTypedArray()).apply {
			actionCommand = "OffsetPreset"
		}
		comboboxPieceOffset!!.addActionListener(this)
		panelPieceOffset.add(comboboxPieceOffset)

		tabPieceOffset = JTabbedPane()
		panelPieceOffset.add(tabPieceOffset)

		// rotationパターン補正(X)タブ --------------------------------------------------
		val panelPieceOffsetX = JPanel()
		panelPieceOffsetX.layout = BoxLayout(panelPieceOffsetX, BoxLayout.Y_AXIS)
		tabPieceOffset!!.addTab(getUIText("TabName_PieceOffsetX"), panelPieceOffsetX)

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
			tabPieceOffset!!.addTab(getUIText("TabName_PieceOffsetY"), this)
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
		tabPieceSpawn!!.addTab(getUIText("TabName_PieceSpawnX"), panelPieceSpawnX)

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
			tabPieceSpawn!!.addTab(getUIText("TabName_PieceSpawnY"), this)
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
			tabPieceSpawn!!.addTab(getUIText("TabName_PieceSpawnBigX"), this)
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
			tabPieceSpawn!!.addTab(getUIText("TabName_PieceSpawnBigY"), this)
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

		val strColorNames = Array(Block.BLOCK_COLOR_COUNT-1) {getUIText("ColorName$it")}

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
				layout = BoxLayout(this, BoxLayout.Y_AXIS)
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
	fun loadImage(url:URL?):BufferedImage? {
		var img:BufferedImage? = null
		try {
			img = ImageIO.read(url!!)
			log.debug("Loaded image from $url")
		} catch(e:IOException) {
			log.error("Failed to load image from ${url!!}", e)
		}

		return img
	}

	/** リソースファイルのURLを返す
	 * @param str Filename
	 * @return リソースファイルのURL
	 */
	fun getURL(str:String):URL? {
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

			val newStr:String
			newStr = if(last!=-1)
				str.substring(last+1)
			else
				str

			vec.add(newStr)
		}

		return vec
	}

	/** ルール設定をUIに反映させる
	 * @param r ルール設定
	 */
	fun readRuleToUI(r:RuleOptions) {
		txtfldRuleName!!.text = r.strRuleName
		txtfldNextDisplay!!.text = r.nextDisplay.toString()
		comboboxStyle!!.selectedIndex = r.style
		comboboxSkin!!.selectedIndex = r.skin
		chkboxGhost!!.isSelected = r.ghost
		chkboxEnterAboveField!!.isSelected = r.pieceEnterAboveField
		txtfldEnterMaxDistanceY!!.text = r.pieceEnterMaxDistanceY.toString()
		val indexRandomizer = vectorRandomizer!!.indexOf(r.strRandomizer)
		comboboxRandomizer!!.selectedIndex = indexRandomizer

		txtfldFieldWidth!!.text = r.fieldWidth.toString()
		txtfldFieldHeight!!.text = r.fieldHeight.toString()
		txtfldFieldHiddenHeight!!.text = r.fieldHiddenHeight.toString()
		chkboxFieldCeiling!!.isSelected = r.fieldCeiling
		chkboxFieldLockoutDeath!!.isSelected = r.fieldLockoutDeath
		chkboxFieldPartialLockoutDeath!!.isSelected = r.fieldPartialLockoutDeath

		chkboxHoldEnable!!.isSelected = r.holdEnable
		chkboxHoldInitial!!.isSelected = r.holdInitial
		chkboxHoldInitialLimit!!.isSelected = r.holdInitialLimit
		chkboxHoldResetDirection!!.isSelected = r.holdResetDirection
		txtfldHoldLimit!!.text = r.holdLimit.toString()

		chkboxDropHardDropEnable!!.isSelected = r.harddropEnable
		chkboxDropHardDropLock!!.isSelected = r.harddropLock
		chkboxDropHardDropLimit!!.isSelected = r.harddropLimit
		chkboxDropSoftDropEnable!!.isSelected = r.softdropEnable
		chkboxDropSoftDropLock!!.isSelected = r.softdropLock
		chkboxDropSoftDropLimit!!.isSelected = r.softdropLimit
		chkboxDropSoftDropSurfaceLock!!.isSelected = r.softdropSurfaceLock
		txtfldDropSoftDropSpeed!!.text = r.softdropSpeed.toString()
		chkboxDropSoftDropMultiplyNativeSpeed!!.isSelected = r.softdropMultiplyNativeSpeed
		chkboxDropSoftDropGravitySpeedLimit!!.isSelected = r.softdropGravitySpeedLimit

		chkboxRotateInitial!!.isSelected = r.rotateInitial
		chkboxRotateInitialLimit!!.isSelected = r.rotateInitialLimit
		chkboxRotateWallkick!!.isSelected = r.rotateWallkick
		chkboxRotateInitialWallkick!!.isSelected = r.rotateInitialWallkick
		txtfldRotateMaxUpwardWallkick!!.text = r.rotateMaxUpwardWallkick.toString()
		chkboxRotateButtonDefaultRight!!.isSelected = r.rotateButtonDefaultRight
		chkboxRotateButtonAllowReverse!!.isSelected = r.rotateButtonAllowReverse
		chkboxRotateButtonAllowDouble!!.isSelected = r.rotateButtonAllowDouble
		val indexWallkick = vectorWallkickSystem!!.indexOf(r.strWallkick)
		comboboxWallkickSystem!!.selectedIndex = indexWallkick

		txtfldLockDelayMin!!.text = r.minLockDelay.toString()
		txtfldLockDelayMax!!.text = r.maxLockDelay.toString()
		chkboxLockDelayLockResetFall!!.isSelected = r.lockresetFall
		chkboxLockDelayLockResetMove!!.isSelected = r.lockresetMove
		chkboxLockDelayLockResetRotate!!.isSelected = r.lockresetRotate
		chkboxLockDelayLockResetWallkick!!.isSelected = r.lockresetWallkick
		chkboxLockDelayLockResetLimitShareCount!!.isSelected = r.lockresetLimitShareCount
		txtfldLockDelayLockResetLimitMove!!.text = r.lockresetLimitMove.toString()
		txtfldLockDelayLockResetLimitRotate!!.text = r.lockresetLimitRotate.toString()
		if(r.lockresetLimitOver==RuleOptions.LOCKRESET_LIMIT_OVER_NORESET)
			radioLockDelayLockResetLimitOverNoReset!!.isSelected = true
		else if(r.lockresetLimitOver==RuleOptions.LOCKRESET_LIMIT_OVER_INSTANT)
			radioLockDelayLockResetLimitOverInstant!!.isSelected = true
		else if(r.lockresetLimitOver==RuleOptions.LOCKRESET_LIMIT_OVER_NOWALLKICK)
			radioLockDelayLockResetLimitOverNoWallkick!!.isSelected = true

		txtfldAREMin!!.text = r.minARE.toString()
		txtfldAREMax!!.text = r.maxARE.toString()
		txtfldARELineMin!!.text = r.minARELine.toString()
		txtfldARELineMax!!.text = r.maxARELine.toString()
		txtfldARELockFlash!!.text = r.lockflash.toString()
		chkboxARELockFlashOnlyFrame!!.isSelected = r.lockflashOnlyFrame
		chkboxARELockFlashBeforeLineClear!!.isSelected = r.lockflashBeforeLineClear
		chkboxARECancelMove!!.isSelected = r.areCancelMove
		chkboxARECancelRotate!!.isSelected = r.areCancelRotate
		chkboxARECancelHold!!.isSelected = r.areCancelHold

		txtfldLineDelayMin!!.text = r.minLineDelay.toString()
		txtfldLineDelayMax!!.text = r.maxLineDelay.toString()
		chkboxLineFallAnim!!.isSelected = r.lineFallAnim
		chkboxLineCancelMove!!.isSelected = r.lineCancelMove
		chkboxLineCancelRotate!!.isSelected = r.lineCancelRotate
		chkboxLineCancelHold!!.isSelected = r.lineCancelHold

		txtfldMoveDASMin!!.text = r.minDAS.toString()
		txtfldMoveDASMax!!.text = r.maxDAS.toString()
		txtfldMoveDASDelay!!.text = r.dasDelay.toString()
		chkboxMoveDASInReady!!.isSelected = r.dasInReady
		chkboxMoveDASInMoveFirstFrame!!.isSelected = r.dasInMoveFirstFrame
		chkboxMoveDASInLockFlash!!.isSelected = r.dasInLockFlash
		chkboxMoveDASInLineClear!!.isSelected = r.dasInLineClear
		chkboxMoveDASInARE!!.isSelected = r.dasInARE
		chkboxMoveDASInARELastFrame!!.isSelected = r.dasInARELastFrame
		chkboxMoveDASInEndingStart!!.isSelected = r.dasInEndingStart
		chkboxMoveDASChargeOnBlockedMove!!.isSelected = r.dasChargeOnBlockedMove
		chkboxMoveDASStoreChargeOnNeutral!!.isSelected = r.dasStoreChargeOnNeutral
		chkboxMoveDASRedirectInDelay!!.isSelected = r.dasRedirectInDelay
		chkboxMoveFirstFrame!!.isSelected = r.moveFirstFrame
		chkboxMoveDiagonal!!.isSelected = r.moveDiagonal
		chkboxMoveUpAndDown!!.isSelected = r.moveUpAndDown
		chkboxMoveLeftAndRightAllow!!.isSelected = r.moveLeftAndRightAllow
		chkboxMoveLeftAndRightUsePreviousInput!!.isSelected = r.moveLeftAndRightUsePreviousInput
		chkboxMoveShiftLockEnable!!.isSelected = r.shiftLockEnable
		comboboxPieceOffset!!.selectedIndex = r.pieceOffset
		for(i in 0 until Piece.PIECE_COUNT) {
			for(j in 0 until Piece.DIRECTION_COUNT) {
				txtfldPieceOffsetX!![i][j].text = "$r.pieceOffsetX[i][j]"
				txtfldPieceOffsetY!![i][j].text = "$r.pieceOffsetY[i][j]"
				txtfldPieceSpawnX!![i][j].text = "$r.pieceSpawnX[i][j]"
				txtfldPieceSpawnY!![i][j].text = "$r.pieceSpawnY[i][j]"
				txtfldPieceSpawnBigX!![i][j].text = "$r.pieceSpawnXBig[i][j]"
				txtfldPieceSpawnBigY!![i][j].text = "$r.pieceSpawnYBig[i][j]"
			}
			comboboxPieceColor!![i].selectedIndex = r.pieceColor[i]-1
			comboboxPieceDirection!![i].selectedIndex = r.pieceDefaultDirection[i]
		}
	}

	/** ルール設定をUIから書き込む
	 * @param r ルール設定
	 */
	fun writeRuleFromUI(r:RuleOptions) {
		r.strRuleName = txtfldRuleName!!.text.toUpperCase()
		r.nextDisplay = getIntTextField(txtfldNextDisplay)
		r.style = comboboxStyle!!.selectedIndex
		r.skin = comboboxSkin!!.selectedIndex
		r.ghost = chkboxGhost!!.isSelected
		r.pieceEnterAboveField = chkboxEnterAboveField!!.isSelected
		r.pieceEnterMaxDistanceY = getIntTextField(txtfldEnterMaxDistanceY)
		val indexRandomizer = comboboxRandomizer!!.selectedIndex
		if(indexRandomizer>=0)
			r.strRandomizer = vectorRandomizer!![indexRandomizer]
		else
			r.strRandomizer = ""

		r.fieldWidth = getIntTextField(txtfldFieldWidth)
		r.fieldHeight = getIntTextField(txtfldFieldHeight)
		r.fieldHiddenHeight = getIntTextField(txtfldFieldHiddenHeight)
		r.fieldCeiling = chkboxFieldCeiling!!.isSelected
		r.fieldLockoutDeath = chkboxFieldLockoutDeath!!.isSelected
		r.fieldPartialLockoutDeath = chkboxFieldPartialLockoutDeath!!.isSelected

		r.holdEnable = chkboxHoldEnable!!.isSelected
		r.holdInitial = chkboxHoldInitial!!.isSelected
		r.holdInitialLimit = chkboxHoldInitialLimit!!.isSelected
		r.holdResetDirection = chkboxHoldResetDirection!!.isSelected
		r.holdLimit = getIntTextField(txtfldHoldLimit)

		r.harddropEnable = chkboxDropHardDropEnable!!.isSelected
		r.harddropLock = chkboxDropHardDropLock!!.isSelected
		r.harddropLimit = chkboxDropHardDropLimit!!.isSelected
		r.softdropEnable = chkboxDropSoftDropEnable!!.isSelected
		r.softdropLock = chkboxDropSoftDropLock!!.isSelected
		r.softdropLimit = chkboxDropSoftDropLimit!!.isSelected
		r.softdropSurfaceLock = chkboxDropSoftDropSurfaceLock!!.isSelected
		r.softdropSpeed = getFloatTextField(txtfldDropSoftDropSpeed)
		r.softdropMultiplyNativeSpeed = chkboxDropSoftDropMultiplyNativeSpeed!!.isSelected
		r.softdropGravitySpeedLimit = chkboxDropSoftDropGravitySpeedLimit!!.isSelected

		r.rotateInitial = chkboxRotateInitial!!.isSelected
		r.rotateInitialLimit = chkboxRotateInitialLimit!!.isSelected
		r.rotateWallkick = chkboxRotateWallkick!!.isSelected
		r.rotateInitialWallkick = chkboxRotateInitialWallkick!!.isSelected
		r.rotateMaxUpwardWallkick = getIntTextField(txtfldRotateMaxUpwardWallkick)
		r.rotateButtonDefaultRight = chkboxRotateButtonDefaultRight!!.isSelected
		r.rotateButtonAllowReverse = chkboxRotateButtonAllowReverse!!.isSelected
		r.rotateButtonAllowDouble = chkboxRotateButtonAllowDouble!!.isSelected
		val indexWallkick = comboboxWallkickSystem!!.selectedIndex
		if(indexWallkick>=0)
			r.strWallkick = vectorWallkickSystem!![indexWallkick]
		else
			r.strWallkick = ""

		r.minLockDelay = getIntTextField(txtfldLockDelayMin)
		r.maxLockDelay = getIntTextField(txtfldLockDelayMax)
		r.lockresetFall = chkboxLockDelayLockResetFall!!.isSelected
		r.lockresetMove = chkboxLockDelayLockResetMove!!.isSelected
		r.lockresetRotate = chkboxLockDelayLockResetRotate!!.isSelected
		r.lockresetWallkick = chkboxLockDelayLockResetWallkick!!.isSelected
		r.lockresetLimitShareCount = chkboxLockDelayLockResetLimitShareCount!!.isSelected
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
		r.lockflashOnlyFrame = chkboxARELockFlashOnlyFrame!!.isSelected
		r.lockflashBeforeLineClear = chkboxARELockFlashBeforeLineClear!!.isSelected
		r.areCancelMove = chkboxARECancelMove!!.isSelected
		r.areCancelRotate = chkboxARECancelRotate!!.isSelected
		r.areCancelHold = chkboxARECancelHold!!.isSelected

		r.minLineDelay = getIntTextField(txtfldLineDelayMin)
		r.maxLineDelay = getIntTextField(txtfldLineDelayMax)
		r.lineFallAnim = chkboxLineFallAnim!!.isSelected
		r.lineCancelMove = chkboxLineCancelMove!!.isSelected
		r.lineCancelRotate = chkboxLineCancelRotate!!.isSelected
		r.lineCancelHold = chkboxLineCancelHold!!.isSelected

		r.minDAS = getIntTextField(txtfldMoveDASMin)
		r.maxDAS = getIntTextField(txtfldMoveDASMax)
		r.dasDelay = getIntTextField(txtfldMoveDASDelay)
		r.dasInReady = chkboxMoveDASInReady!!.isSelected
		r.dasInMoveFirstFrame = chkboxMoveDASInMoveFirstFrame!!.isSelected
		r.dasInLockFlash = chkboxMoveDASInLockFlash!!.isSelected
		r.dasInLineClear = chkboxMoveDASInLineClear!!.isSelected
		r.dasInARE = chkboxMoveDASInARE!!.isSelected
		r.dasInARELastFrame = chkboxMoveDASInARELastFrame!!.isSelected
		r.dasInEndingStart = chkboxMoveDASInEndingStart!!.isSelected
		r.dasChargeOnBlockedMove = chkboxMoveDASChargeOnBlockedMove!!.isSelected
		r.dasStoreChargeOnNeutral = chkboxMoveDASStoreChargeOnNeutral!!.isSelected
		r.dasRedirectInDelay = chkboxMoveDASRedirectInDelay!!.isSelected
		r.moveFirstFrame = chkboxMoveFirstFrame!!.isSelected
		r.moveDiagonal = chkboxMoveDiagonal!!.isSelected
		r.moveUpAndDown = chkboxMoveUpAndDown!!.isSelected
		r.moveLeftAndRightAllow = chkboxMoveLeftAndRightAllow!!.isSelected
		r.moveLeftAndRightUsePreviousInput = chkboxMoveLeftAndRightUsePreviousInput!!.isSelected
		r.shiftLockEnable = chkboxMoveShiftLockEnable!!.isSelected
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
		val ruleopt = RuleOptions()
		writeRuleFromUI(ruleopt)

		val prop = CustomProperties()
		ruleopt.writeProperty(prop, 0)

		val out = FileOutputStream(filename)
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

		val `in` = FileInputStream(filename)
		prop.load(`in`)
		`in`.close()

		val ruleopt = RuleOptions()
		ruleopt.readProperty(prop, 0, true)

		log.debug("Loaded rule file from $filename")

		return ruleopt
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
	fun getIntTextField(txtfld:JTextField?):Int {
		var v = 0

		try {
			v = Integer.parseInt(txtfld!!.text)
		} catch(e:Exception) {
		}

		return v
	}

	/** テキストfieldからfloat型の値を取得
	 * @param txtfld テキストfield
	 * @return テキストfieldから値を取得できた場合はその値, 失敗したら0f
	 */
	fun getFloatTextField(txtfld:JTextField?):Float {
		var v = 0f

		try {
			v = java.lang.Float.parseFloat(txtfld!!.text)
		} catch(e:Exception) {
		}

		return v
	}

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
					var ruleopt = RuleOptions()

					strNowFile = file.path
					title = "${getUIText("Title_RuleEditor")}:$strNowFile"

					try {
						ruleopt = load(file.path)
					} catch(e2:IOException) {
						log.error("Failed to load rule data from ${strNowFile!!}", e2)
						JOptionPane.showMessageDialog(this, "${getUIText("Message_FileLoadFailed")}\n$e2",
							getUIText("Title_FileLoadFailed"), JOptionPane.ERROR_MESSAGE)
						return
					}

					readRuleToUI(ruleopt)
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

				}else {
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
			"Save","SaveAs" -> {
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
	protected inner class FileFilterRUL:FileFilter() {
		override fun accept(f:File):Boolean = if(f.isDirectory) true else f.name.endsWith(".rul")

		override fun getDescription():String = getUIText("FileChooser_RuleFile")
	}

	/** 画像表示Comboボックスの項目<br></br>
	 * [出典](http://www.javadrive.jp/tutorial/jcombobox/index20.html) */
	protected inner class ComboLabel(
		var text:String = "",
		var icon:Icon? = null
	) {
	}

	/** 画像表示ComboボックスのListCellRenderer<br></br>
	 * [出典](http://www.javadrive.jp/tutorial/jcombobox/index20.html) */
	protected inner class ComboLabelCellRenderer:JLabel(), ListCellRenderer<Any> {
		fun getListCellRendererComponent(list:JList<out Nothing>?, value:Nothing?, index:Int, isSelected:Boolean, cellHasFocus:Boolean):Component {
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
		internal val log = Logger.getLogger(RuleEditor::class.java)

		/** メイン関数
		 * @param args コマンドLines引数
		 */
		@JvmStatic
		fun main(args:Array<String>) {
			PropertyConfigurator.configure("config/etc/log.cfg")
			log.debug("RuleEditor start")

			if(args.isNotEmpty())
				RuleEditor(args[0])
			else
				RuleEditor()
		}
	}
}
