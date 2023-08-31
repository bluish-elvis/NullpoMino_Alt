/*
 * Copyright (c) 2010-2023, NullNoname
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
package mu.nu.nullpo.tool.ruleeditor

/*

/** ルールエディター */
class RuleEditorFX:Application(null) {
	private var stage:Stage = Stage()
	private var scene:Scene? = null
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
	private val tabPane:TabPane = TabPane()

	//----------------------------------------------------------------------
	/* 基本設定パネル */

	/** Rule name */
	private val txtfldRuleName:TextField = TextField()

	/** NEXT表示countのテキストfield */
	private val txtfldNextDisplay:TextField = TextField()

	/** Game style combobox */
	private var comboboxStyle:ComboBox<*>? = null

	/** 絵柄のComboボックス */
	private var comboboxSkin:ComboBox<*>? = null

	/** ghost is enabled */
	private val chkboxGhost:CheckBox = CheckBox()

	/** Blockピースがfield枠外から出現 */
	private val chkboxEnterAboveField:CheckBox = CheckBox()

	/** 出現予定地が埋まっているときにY-coordinateを上にずらすMaximum count */
	private val txtfldEnterMaxDistanceY:TextField = TextField()

	/** NEXT順生成アルゴリズム */
	private var comboboxRandomizer:ComboBox<*>? = null

	/** NEXT順生成アルゴリズムのリスト */
	private var vectorRandomizer:Vector<String> = Vector()

	/** NEXT順生成アルゴリズムのリセット button
	 * private JButton btnResetRandomizer; */

	//----------------------------------------------------------------------
	/* field設定パネル */

	/** fieldの幅 */
	private val txtfldFieldWidth:TextField = TextField()

	/** Field height */
	private val txtfldFieldHeight:TextField = TextField()

	/** fieldの見えない部分の高さ */
	private val txtfldFieldHiddenHeight:TextField = TextField()

	/** fieldの天井 */
	private val chkboxFieldCeiling:CheckBox = CheckBox()

	/** field枠内に置けないと死亡 */
	private val chkboxFieldLockoutDeath:CheckBox = CheckBox()

	/** field枠外にはみ出しただけで死亡 */
	private val chkboxFieldPartialLockoutDeath:CheckBox = CheckBox()

	//----------------------------------------------------------------------
	/* ホールド設定パネル */

	/** ホールド is enabled */
	private val chkboxHoldEnable:CheckBox = CheckBox()

	/** 先行ホールド */
	private val chkboxHoldInitial:CheckBox = CheckBox()

	/** 先行ホールド連続使用不可 */
	private val chkboxHoldInitialLimit:CheckBox = CheckBox()

	/** ホールドを使ったときにBlockピースの向きを初期状態に戻す */
	private val chkboxHoldResetDirection:CheckBox = CheckBox()

	/** ホールドできる count (-1:無制限) */
	private val txtfldHoldLimit:TextField = TextField()

	//----------------------------------------------------------------------
	/* ドロップ設定パネル */

	/** Hard drop使用可否 */
	private val chkboxDropHardDropEnable:CheckBox = CheckBox()

	/** Hard dropで即固定 */
	private val chkboxDropHardDropLock:CheckBox = CheckBox()

	/** Hard drop連続使用不可 */
	private val chkboxDropHardDropLimit:CheckBox = CheckBox()

	/** Soft drop使用可否 */
	private val chkboxDropSoftDropEnable:CheckBox = CheckBox()

	/** Soft dropで即固定 */
	private val chkboxDropSoftDropLock:CheckBox = CheckBox()

	/** Soft drop連続使用不可 */
	private val chkboxDropSoftDropLimit:CheckBox = CheckBox()

	/** 接地状態でSoft dropすると即固定 */
	private val chkboxDropSoftDropSurfaceLock:CheckBox = CheckBox()

	/** Soft drop速度 */
	private val txtfldDropSoftDropSpeed:TextField = TextField()

	/** Soft drop速度をCurrent 通常速度×n倍にする */
	private val chkboxDropSoftDropMultiplyNativeSpeed:CheckBox = CheckBox()

	/** Use new soft drop codes */
	private val chkboxDropSoftDropGravitySpeedLimit:CheckBox = CheckBox()

	//----------------------------------------------------------------------
	/* rotation設定パネル */

	/** 先行rotation */
	private val chkboxRotateInitial:CheckBox = CheckBox()

	/** 先行rotation連続使用不可 */
	private val chkboxRotateInitialLimit:CheckBox = CheckBox()

	/** Wallkick */
	private val chkboxRotateWallkick:CheckBox = CheckBox()

	/** 先行rotationでもWallkickする */
	private val chkboxRotateInitialWallkick:CheckBox = CheckBox()

	/** 上DirectionへのWallkickができる count (-1:無限) */
	private val txtfldRotateMaxUpwardWallkick:TextField = TextField()

	/** falseなら左が正rotation, When true,右が正rotation */
	private val chkboxRotateButtonDefaultRight:CheckBox = CheckBox()

	/** 逆rotationを許可 (falseなら正rotationと同じ) */
	private val chkboxRotateButtonAllowReverse:CheckBox = CheckBox()

	/** 2rotationを許可 (falseなら正rotationと同じ) */
	private val chkboxRotateButtonAllowDouble:CheckBox = CheckBox()

	/** Wallkickアルゴリズム */
	private var comboboxWallkickSystem:ComboBox<*>? = null

	/** Wallkickアルゴリズムのリスト */
	private var vectorWallkickSystem:Vector<String>? = null

	/** Wallkickアルゴリズムのリセット button */
	private val btnResetWallkickSystem:Button = Button()

	//----------------------------------------------------------------------
	/* 固定 time設定パネル */

	/** 最低固定 time */
	private val txtfldLockDelayMin:TextField = TextField()

	/** 最高固定 time */
	private val txtfldLockDelayMax:TextField = TextField()

	/** 落下で固定 timeリセット */
	private val chkboxLockDelayLockResetFall:CheckBox = CheckBox()

	/** 移動で固定 timeリセット */
	private val chkboxLockDelayLockResetMove:CheckBox = CheckBox()

	/** rotationで固定 timeリセット */
	private val chkboxLockDelayLockResetRotate:CheckBox = CheckBox()

	/** Lock delay reset by wallkick */
	private val chkboxLockDelayLockResetWallkick:CheckBox = CheckBox()

	/** 横移動 counterとrotation counterを共有 (横移動 counterだけ使う) */
	private val chkboxLockDelayLockResetLimitShareCount:CheckBox = CheckBox()

	/** 横移動 count制限 */
	private val txtfldLockDelayLockResetLimitMove:TextField = TextField()

	/** rotation count制限 */
	private val txtfldLockDelayLockResetLimitRotate:TextField = TextField()

	/** 横移動 counterかrotation counterが超過したら固定 timeリセットを無効にする */
	private val radioLockDelayLockResetLimitOverNoReset:RadioButton = RadioButton()

	/** 横移動 counterかrotation counterが超過したら即座に固定する */
	private val radioLockDelayLockResetLimitOverInstant:RadioButton = RadioButton()

	/** 横移動 counterかrotation counterが超過したらWallkick無効にする */
	private val radioLockDelayLockResetLimitOverNoWallkick:RadioButton = RadioButton()

	//----------------------------------------------------------------------
	/* ARE設定パネル */

	/** 最低ARE */
	private val txtfldAREMin:TextField = TextField()

	/** 最高ARE */
	private val txtfldAREMax:TextField = TextField()

	/** 最低ARE after line clear */
	private val txtfldARELineMin:TextField = TextField()

	/** 最高ARE after line clear */
	private val txtfldARELineMax:TextField = TextField()

	/** 固定した瞬間に光る frame count */
	private val txtfldARELockFlash:TextField = TextField()

	/** Blockが光る専用 frame を入れる */
	private val chkboxARELockFlashOnlyFrame:CheckBox = CheckBox()

	/** Line clear前にBlockが光る frame を入れる */
	private val chkboxARELockFlashBeforeLineClear:CheckBox = CheckBox()

	/** ARE cancel on move checkbox */
	private val chkboxARECancelMove:CheckBox = CheckBox()

	/** ARE cancel on rotate checkbox */
	private val chkboxARECancelRotate:CheckBox = CheckBox()

	/** ARE cancel on hold checkbox */
	private val chkboxARECancelHold:CheckBox = CheckBox()

	//----------------------------------------------------------------------
	/* Line clear設定パネル */

	/** 最低Line clear time */
	private val txtfldLineDelayMin:TextField = TextField()

	/** 最高Line clear time */
	private val txtfldLineDelayMax:TextField = TextField()

	/** 落下アニメ */
	private val chkboxLineFallAnim:CheckBox = CheckBox()

	/** Line delay cancel on move checkbox */
	private val chkboxLineCancelMove:CheckBox = CheckBox()

	/** Line delay cancel on rotate checkbox */
	private val chkboxLineCancelRotate:CheckBox = CheckBox()

	/** Line delay cancel on hold checkbox */
	private val chkboxLineCancelHold:CheckBox = CheckBox()

	//----------------------------------------------------------------------
	/* 移動設定パネル */

	/** 最低横溜め time */
	private val txtfldMoveDASMin:TextField = TextField()

	/** 最高横溜め time */
	private val txtfldMoveDASMax:TextField = TextField()

	/** 横移動間隔 */
	private val txtfldMoveDASDelay:TextField = TextField()

	/** Ready画面で横溜め可能 */
	private val chkboxMoveDASInReady:CheckBox = CheckBox()

	/** 最初の frame で横溜め可能 */
	private val chkboxMoveDASInMoveFirstFrame:CheckBox = CheckBox()

	/** Blockが光った瞬間に横溜め可能 */
	private val chkboxMoveDASInLockFlash:CheckBox = CheckBox()

	/** Line clear中に横溜め可能 */
	private val chkboxMoveDASInLineClear:CheckBox = CheckBox()

	/** ARE中に横溜め可能 */
	private val chkboxMoveDASInARE:CheckBox = CheckBox()

	/** AREの最後の frame で横溜め可能 */
	private val chkboxMoveDASInARELastFrame:CheckBox = CheckBox()

	/** Ending突入画面で横溜め可能 */
	private val chkboxMoveDASInEndingStart:CheckBox = CheckBox()

	/** DAS charge on blocked move checkbox */
	private val chkboxMoveDASChargeOnBlockedMove:CheckBox = CheckBox()

	/** Store DAS Charge on neutral checkbox */
	private val chkboxMoveDASStoreChargeOnNeutral:CheckBox = CheckBox()

	/** Redirect in delay checkbox */
	private val chkboxMoveDASRedirectInDelay:CheckBox = CheckBox()

	/** 最初の frame に移動可能 */
	private val chkboxMoveFirstFrame:CheckBox = CheckBox()

	/** 斜め移動 */
	private val chkboxMoveDiagonal:CheckBox = CheckBox()

	/** 上下同時押し可能 */
	private val chkboxMoveUpAndDown:CheckBox = CheckBox()

	/** 左右同時押し可能 */
	private val chkboxMoveLeftAndRightAllow:CheckBox = CheckBox()

	/** 左右同時押ししたときに前の frame の input Directionを優先する */
	private val chkboxMoveLeftAndRightUsePreviousInput:CheckBox = CheckBox()

	/** Shift lock checkbox */
	private val chkboxMoveShiftLockEnable:CheckBox = CheckBox()

	//----------------------------------------------------------------------
	/* rotationパターン補正パネル */

	/** プリセット選択Comboボックス */
	private var comboboxPieceOffset:ComboBox<*>? = null

	/** rotationパターン補正タブ */
	private var tabPieceOffset:TabPane? = null

	/** rotationパターン補正(X) input 欄 */
	private var txtfldPieceOffsetX:Array<Array<TextField>> = emptyArray()

	/** rotationパターン補正(Y) input 欄 */
	private var txtfldPieceOffsetY:Array<Array<TextField>> = emptyArray()

	//----------------------------------------------------------------------
	/* rotationパターン補正パネル */

	/** rotationパターン補正タブ */
	private var tabPieceSpawn:TabPane? = null

	/** 出現位置補正(X) input 欄 */
	private var txtfldPieceSpawnX:Array<Array<TextField>> = emptyArray()

	/** 出現位置補正(Y) input 欄 */
	private var txtfldPieceSpawnY:Array<Array<TextField>> = emptyArray()

	/** Big時出現位置補正(X) input 欄 */
	private var txtfldPieceSpawnBigX:Array<Array<TextField>> = emptyArray()

	/** Big時出現位置補正(Y) input 欄 */
	private var txtfldPieceSpawnBigY:Array<Array<TextField>> = emptyArray()

	//----------------------------------------------------------------------
	/* 色設定パネル */

	/** 色選択Comboボックス */
	private var comboboxPieceColor:Array<ComboBox<*>>? = null

	//----------------------------------------------------------------------
	/* 初期Direction設定パネル */

	/** 初期Direction選択Comboボックス */
	private var comboboxPieceDirection:Array<ComboBox<*>>? = null

	//----------------------------------------------------------------------
	/** Block画像 */
	private var imgBlockSkins:Array<Image>? = null
	override fun init() {
		log.info(ParametersImpl.getParameters(this))
		// 設定ファイル読み込み
		try {
			val `in` = FileInputStream("config/setting/swing.properties")
			propConfig.load(`in`)
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
		strNowFile = null
		loadBlockSkins()
	}

	override fun start(p0:Stage?) {
		stage = p0 ?: return
		stage.onCloseRequest = EventHandler<WindowEvent> {
			log.info("CLOSING")
		}

		// Look&Feel設定
		/*if(propConfig.getProperty("option.usenativelookandfeel", true))
			try {
				UIManager.getInstalledLookAndFeels()
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
			} catch(e:Exception) {
				log.warn("Failed to set native look&feel", e)
			}*/


		stage.title = getUIText("Title_RuleEditor")

		var ruleOpt = RuleOptions()

		readRuleToUI(RuleOptions())
		initUI()
		stage.sizeToScene()
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
				stage.title = "${getUIText("Title_RuleEditor")}:$strNowFile"
			} catch(e:IOException) {
				log.error("Failed to load rule data from $filename", e)
				Alert(
					Alert.AlertType.ERROR, "${getUIText("Message_FileLoadFailed")}\n$e",
				).apply {
					title = getUIText("Title_FileLoadFailed")
				}
			}

		readRuleToUI(ruleOpt)

		stage.show()
		initUI()
	}

	/** 画面のInitialization */
	private fun initUI() {
		contentPane.layout = BorderLayout()

		// Menuバー --------------------------------------------------
		val menuBar = MenuBar()

		// ファイルMenu
		val mFile = Menu(getUIText("JMenu_File"))
		mFile.setMnemonic('F')
		menuBar.add(mFile)

		// 新規作成
		mFile.add(MenuItem(getUIText("JMenuItem_New")).also {
			it.setMnemonic('N')
			it.accelerator = KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK)
			it.actionCommand = "New"
			it.addActionListener(this)
		})

		// 開く
		mFile.add(MenuItem(getUIText("JMenuItem_Open")).also {
			it.setMnemonic('O')
			it.accelerator = KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK)
			it.actionCommand = "Open"
			it.addActionListener(this)
		})

		// Up書き保存
		mFile.add(MenuItem(getUIText("JMenuItem_Save")).also {
			it.setMnemonic('S')
			it.accelerator = KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK)
			it.actionCommand = "Save"
			it.addActionListener(this)
		})

		// Nameを付けて保存
		mFile.add(MenuItem(getUIText("JMenuItem_SaveAs")).also {
			it.setMnemonic('A')
			it.accelerator = KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK or InputEvent.ALT_DOWN_MASK)
			it.actionCommand = "SaveAs"
			it.addActionListener(this)
		})

		// 終了
		mFile.add(MenuItem(getUIText("JMenuItem_Exit")).also {
			it.setMnemonic('X')
			it.actionCommand = "Exit"
			it.addActionListener(this)
		})

		// タブ全体 --------------------------------------------------
		contentPane.add(tabPane, BorderLayout.NORTH)

		// 基本設定タブ --------------------------------------------------
		val panelBasic = VBox()
		tabPane.addTab(getUIText("TabName_Basic"), panelBasic)

		// Rule name
		txtfldRuleName.prefColumnCount = 15
		panelBasic.children.addAll(listOf(Pane().apply {
			add(Label(getUIText("Basic_RuleName")))
			add(txtfldRuleName)
		}))

		// NEXT表示count
		panelBasic.add(Pane().apply {
			add(Label(getUIText("Basic_NextDisplay")))
			txtfldNextDisplay.prefColumnCount = 5
			add(txtfldNextDisplay)
		})

		// Game style
		val pStyle = Pane()
		pStyle.add(Label(getUIText("Basic_Style")))
		comboboxStyle = ComboBox(GameEngine.GAMESTYLE_NAMES).apply {
			preferredSize = Dimension(100, 30)
		}
		pStyle.add(comboboxStyle)
		panelBasic.add(pStyle)

		// 絵柄
		val pSkin = Pane()
		panelBasic.add(pSkin)

		pSkin.add(Label(getUIText("Basic_Skin")))

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
		val pEnterMaxDistanceY = Pane()
		panelBasic.add(pEnterMaxDistanceY)

		pEnterMaxDistanceY.add(Label(getUIText("Basic_EnterMaxDistanceY")))

		txtfldEnterMaxDistanceY.prefColumnCount = 5
		pEnterMaxDistanceY.add(txtfldEnterMaxDistanceY)

		// NEXT順生成アルゴリズム
		val pRandomizer = Pane()
		panelBasic.add(pRandomizer)

		pRandomizer.add(Label(getUIText("Basic_Randomizer")))

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
		val panelField = Pane()
		panelField.layout = BoxLayout(panelField, BoxLayout.Y_AXIS)
		tabPane.addTab(getUIText("TabName_Field"), panelField)

		// fieldの幅
		panelField.add(Pane().apply {
			add(Label(getUIText("Field_FieldWidth")))
			add(txtfldFieldWidth)
		})

		// Field height
		panelField.add(Pane().apply {
			add(Label(getUIText("Field_FieldHeight")))
			add(txtfldFieldHeight)
		})

		// fieldの見えない部分の高さ
		panelField.add(Pane().apply {
			add(Label(getUIText("Field_FieldHiddenHeight")))
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
		val panelHold = Pane()
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
		chkboxHoldResetDirection.text = getUIText("Hold_HoldResetDirection")
		panelHold.add(chkboxHoldResetDirection)

		// ホールドできる count
		txtfldHoldLimit.prefColumnCount = 5
		val pHoldLimit = Pane().apply {
			add(Label(getUIText("Hold_HoldLimit")))
			add(txtfldHoldLimit)
		}
		panelHold.add(pHoldLimit)

		// ドロップタブ --------------------------------------------------
		val panelDrop = Pane()
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
		val pDropSoftDropSpeed = Pane()
		panelDrop.add(pDropSoftDropSpeed)
		pDropSoftDropSpeed.add(Label(getUIText("Drop_SoftDropSpeed")))

		txtfldDropSoftDropSpeed.prefColumnCount = 5
		pDropSoftDropSpeed.add(txtfldDropSoftDropSpeed)

		// rotationタブ --------------------------------------------------
		val panelRotate = Pane()
		panelRotate.layout = BoxLayout(panelRotate, BoxLayout.Y_AXIS)
		tabPane.addTab(getUIText("TabName_Rotate"), panelRotate)

		// 先行rotation
		chkboxRotateInitial.text = getUIText("Rotate_RotateInitial")
		panelRotate.add(chkboxRotateInitial)

		// 先行rotation連続使用不可
		chkboxRotateInitialLimit.text = getUIText("Rotate_RotateInitialLimit")
		panelRotate.add(chkboxRotateInitialLimit)

		// Wallkick
		chkboxRotateWallkick.text = getUIText("Rotate_RotateWallkick")
		panelRotate.add(chkboxRotateWallkick)

		// 先行rotationでもWallkickする
		chkboxRotateInitialWallkick.text = getUIText("Rotate_RotateInitialWallkick")
		panelRotate.add(chkboxRotateInitialWallkick)

		// Aで右rotation
		chkboxRotateButtonDefaultRight.text = getUIText("Rotate_RotateButtonDefaultRight")
		panelRotate.add(chkboxRotateButtonDefaultRight)

		// 逆rotation許可
		chkboxRotateButtonAllowReverse.text = getUIText("Rotate_RotateButtonAllowReverse")
		panelRotate.add(chkboxRotateButtonAllowReverse)

		// 2rotation許可
		chkboxRotateButtonAllowDouble.text = getUIText("Rotate_RotateButtonAllowDouble")
		panelRotate.add(chkboxRotateButtonAllowDouble)

		// UpDirectionへWallkickできる count
		val pRotateMaxUpwardWallkick = Pane()
		panelRotate.add(pRotateMaxUpwardWallkick)
		pRotateMaxUpwardWallkick.add(Label(getUIText("Rotate_RotateMaxUpwardWallkick")))

		txtfldRotateMaxUpwardWallkick.prefColumnCount = 5
		pRotateMaxUpwardWallkick.add(txtfldRotateMaxUpwardWallkick)

		// Wallkickアルゴリズム
		val pWallkickSystem = Pane()
		panelRotate.add(pWallkickSystem)

		pWallkickSystem.add(Label(getUIText("Rotate_WallkickSystem")))

		vectorWallkickSystem = getTextFileVector("config/list/wallkick.lst")
		comboboxWallkickSystem = JComboBox(createShortStringVector(vectorWallkickSystem)).apply {
			preferredSize = Dimension(200, 30)
			pWallkickSystem.add(this)
		}

		btnResetWallkickSystem.also {
			it.text = getUIText("Rotate_ResetWallkickSystem")
			it.setMnemonic('R')
			it.actionCommand = "ResetWallkickSystem"
			it.setOnAction(this)
			pWallkickSystem.add(it)
		}

		// 固定 timeタブ --------------------------------------------------
		val panelLockDelay = Pane()
		panelLockDelay.layout = BoxLayout(panelLockDelay, BoxLayout.Y_AXIS)
		tabPane.addTab(getUIText("TabName_LockDelay"), panelLockDelay)

		// 最低固定 timeと最高固定 time
		panelLockDelay.add(Label(getUIText("LockDelay_LockDelayMinMax")))

		val pLockDelayMinMax = Pane()
		panelLockDelay.add(pLockDelayMinMax)

		txtfldLockDelayMin.prefColumnCount = 5
		pLockDelayMinMax.add(txtfldLockDelayMin)
		txtfldLockDelayMax.prefColumnCount = 5
		pLockDelayMinMax.add(txtfldLockDelayMax)

		// 落下で固定 timeリセット
		chkboxLockDelayLockResetFall.text = getUIText("LockDelay_LockResetFall")
		panelLockDelay.add(chkboxLockDelayLockResetFall)

		// 移動で固定 timeリセット
		chkboxLockDelayLockResetMove.text = getUIText("LockDelay_LockResetMove")
		panelLockDelay.add(chkboxLockDelayLockResetMove)

		// rotationで固定 timeリセット
		chkboxLockDelayLockResetRotate.text = getUIText("LockDelay_LockResetRotate")
		panelLockDelay.add(chkboxLockDelayLockResetRotate)

		// Lock delay reset by wallkick
		chkboxLockDelayLockResetWallkick.text = getUIText("LockDelay_LockResetWallkick")
		panelLockDelay.add(chkboxLockDelayLockResetWallkick)

		// 横移動 counterとrotation counterを共有 (横移動 counterだけ使う）
		chkboxLockDelayLockResetLimitShareCount.text = getUIText("LockDelay_LockDelayLockResetLimitShareCount")
		panelLockDelay.add(chkboxLockDelayLockResetLimitShareCount)

		// 横移動 count制限
		val pLockDelayLockResetLimitMove = Pane()
		panelLockDelay.add(pLockDelayLockResetLimitMove)
		pLockDelayLockResetLimitMove.add(Label(getUIText("LockDelay_LockDelayLockResetLimitMove")))

		txtfldLockDelayLockResetLimitMove.prefColumnCount = 5
		pLockDelayLockResetLimitMove.add(txtfldLockDelayLockResetLimitMove)

		// rotation count制限
		val pLockDelayLockResetLimitRotate = Pane()
		panelLockDelay.add(pLockDelayLockResetLimitRotate)

		pLockDelayLockResetLimitRotate.add(Label(getUIText("LockDelay_LockDelayLockResetLimitRotate")))

		txtfldLockDelayLockResetLimitRotate.prefColumnCount = 5
		pLockDelayLockResetLimitRotate.add(txtfldLockDelayLockResetLimitRotate)

		// 移動またはrotation count制限が超過した時の設定
		val pLockDelayLockResetLimitOver = Pane()
		pLockDelayLockResetLimitOver.layout = BoxLayout(pLockDelayLockResetLimitOver, BoxLayout.Y_AXIS)
		panelLockDelay.add(pLockDelayLockResetLimitOver)

		pLockDelayLockResetLimitOver.add(Label(getUIText("LockDelay_LockDelayLockResetLimitOver")))

		val gLockDelayLockResetLimitOver = ButtonGroup()

		radioLockDelayLockResetLimitOverNoReset.text = getUIText("LockDelay_LockDelayLockResetLimitOverNoReset")
		pLockDelayLockResetLimitOver.add(radioLockDelayLockResetLimitOverNoReset)
		gLockDelayLockResetLimitOver.add(radioLockDelayLockResetLimitOverNoReset)

		radioLockDelayLockResetLimitOverInstant.text = getUIText("LockDelay_LockDelayLockResetLimitOverInstant")
		pLockDelayLockResetLimitOver.add(radioLockDelayLockResetLimitOverInstant)
		gLockDelayLockResetLimitOver.add(radioLockDelayLockResetLimitOverInstant)

		radioLockDelayLockResetLimitOverNoWallkick.text = getUIText("LockDelay_LockDelayLockResetLimitOverNoWallkick")
		pLockDelayLockResetLimitOver.add(radioLockDelayLockResetLimitOverNoWallkick)
		gLockDelayLockResetLimitOver.add(radioLockDelayLockResetLimitOverNoWallkick)

		// AREタブ --------------------------------------------------
		val panelARE = Pane()
		panelARE.layout = BoxLayout(panelARE, BoxLayout.Y_AXIS)
		tabPane.addTab(getUIText("TabName_ARE"), panelARE)

		// 最低AREと最高ARE
		panelARE.add(Label(getUIText("ARE_MinMax")))

		val pAREMinMax = Pane()
		panelARE.add(pAREMinMax)

		txtfldAREMin.prefColumnCount = 5
		pAREMinMax.add(txtfldAREMin)
		txtfldAREMax.prefColumnCount = 5
		pAREMinMax.add(txtfldAREMax)

		// 最低ARE after line clearと最高ARE after line clear
		panelARE.add(Label(getUIText("ARE_LineMinMax")))

		val pARELineMinMax = Pane()
		panelARE.add(pARELineMinMax)

		txtfldARELineMin.prefColumnCount = 5
		pARELineMinMax.add(txtfldARELineMin)
		txtfldARELineMax.prefColumnCount = 5
		pARELineMinMax.add(txtfldARELineMax)

		// 固定した瞬間に光る frame count
		panelARE.add(Label(getUIText("ARE_LockFlash")))

		val pARELockFlash = Pane()
		panelARE.add(pARELockFlash)

		txtfldARELockFlash.prefColumnCount = 5
		pARELockFlash.add(txtfldARELockFlash)

		// Blockが光る専用 frame を入れる
		chkboxARELockFlashOnlyFrame.text = getUIText("ARE_LockFlashOnlyFrame")
		panelARE.add(chkboxARELockFlashOnlyFrame)

		// Line clear前にBlockが光る frame を入れる
		chkboxARELockFlashBeforeLineClear.text = getUIText("ARE_LockFlashBeforeLineClear")
		panelARE.add(chkboxARELockFlashBeforeLineClear)

		// ARE cancel on move
		chkboxARECancelMove.text = getUIText("ARE_CancelMove")
		panelARE.add(chkboxARECancelMove)

		// ARE cancel on move
		chkboxARECancelRotate.text = getUIText("ARE_CancelRotate")
		panelARE.add(chkboxARECancelRotate)

		// ARE cancel on move
		chkboxARECancelHold.text = getUIText("ARE_CancelHold")
		panelARE.add(chkboxARECancelHold)

		// Line clearタブ --------------------------------------------------
		val panelLine = Pane()
		panelLine.layout = BoxLayout(panelLine, BoxLayout.Y_AXIS)
		tabPane.addTab(getUIText("TabName_Line"), panelLine)

		// 最低Line clear timeと最高Line clear time
		panelLine.add(Label(getUIText("Line_MinMax")))

		val pLineMinMax = Pane()
		panelLine.add(pLineMinMax)

		txtfldLineDelayMin.prefColumnCount = 5
		pLineMinMax.add(txtfldLineDelayMin)
		txtfldLineDelayMax.prefColumnCount = 5
		pLineMinMax.add(txtfldLineDelayMax)

		// 落下アニメ
		chkboxLineFallAnim.text = getUIText("Line_FallAnim")
		panelLine.add(chkboxLineFallAnim)

		// Line delay cancel on move
		chkboxLineCancelMove.text = getUIText("Line_CancelMove")
		panelLine.add(chkboxLineCancelMove)

		// Line delay cancel on rotate
		chkboxLineCancelRotate.text = getUIText("Line_CancelRotate")
		panelLine.add(chkboxLineCancelRotate)

		// Line delay cancel on hold
		chkboxLineCancelHold.text = getUIText("Line_CancelHold")
		panelLine.add(chkboxLineCancelHold)

		// 移動タブ --------------------------------------------------
		val panelMove = Pane()
		panelMove.layout = BoxLayout(panelMove, BoxLayout.Y_AXIS)
		tabPane.addTab(getUIText("TabName_Move"), panelMove)

		// 最低横溜め timeと最高横溜め time
		panelMove.add(Label(getUIText("Move_DASMinMax")))

		val pMoveDASMinMax = Pane()
		panelMove.add(pMoveDASMinMax)

		txtfldMoveDASMin.prefColumnCount = 5
		pMoveDASMinMax.add(txtfldMoveDASMin)
		txtfldMoveDASMax.prefColumnCount = 5
		pMoveDASMinMax.add(txtfldMoveDASMax)

		// 横移動間隔
		val pMoveDASDelay = Pane()
		panelMove.add(pMoveDASDelay)

		pMoveDASDelay.add(Label(getUIText("Move_DASDelay1")))

		txtfldMoveDASDelay.prefColumnCount = 5
		pMoveDASDelay.add(txtfldMoveDASDelay)

		pMoveDASDelay.add(Label(getUIText("Move_DASDelay2")))

		// ○○のとき横溜め可能
		chkboxMoveDASInReady.text = getUIText("Move_DASInReady")
		panelMove.add(chkboxMoveDASInReady)
		chkboxMoveDASInMoveFirstFrame.text = getUIText("Move_DASInMoveFirstFrame")
		panelMove.add(chkboxMoveDASInMoveFirstFrame)
		chkboxMoveDASInLockFlash.text = getUIText("Move_DASInLockFlash")
		panelMove.add(chkboxMoveDASInLockFlash)
		chkboxMoveDASInLineClear.text = getUIText("Move_DASInLineClear")
		panelMove.add(chkboxMoveDASInLineClear)
		chkboxMoveDASInARE.text = getUIText("Move_DASInARE")
		panelMove.add(chkboxMoveDASInARE)
		chkboxMoveDASInARELastFrame.text = getUIText("Move_DASInARELastFrame")
		panelMove.add(chkboxMoveDASInARELastFrame)
		chkboxMoveDASInEndingStart.text = getUIText("Move_DASInEndingStart")
		panelMove.add(chkboxMoveDASInEndingStart)
		chkboxMoveDASChargeOnBlockedMove.text = getUIText("Move_DASChargeOnBlockedMove")
		panelMove.add(chkboxMoveDASChargeOnBlockedMove)
		chkboxMoveDASStoreChargeOnNeutral.text = getUIText("Move_DASStoreChargeOnNeutral")
		panelMove.add(chkboxMoveDASStoreChargeOnNeutral)
		chkboxMoveDASRedirectInDelay.text = getUIText("Move_DASRedirectInDelay")
		panelMove.add(chkboxMoveDASRedirectInDelay)

		// 最初の frame に移動可能
		chkboxMoveFirstFrame.text = getUIText("Move_FirstFrame")
		panelMove.add(chkboxMoveFirstFrame)

		// 斜め移動
		chkboxMoveDiagonal.text = getUIText("Move_Diagonal")
		panelMove.add(chkboxMoveDiagonal)

		// Up下同時押し
		chkboxMoveUpAndDown.text = getUIText("Move_UpAndDown")
		panelMove.add(chkboxMoveUpAndDown)

		// 左右同時押し
		chkboxMoveLeftAndRightAllow.text = getUIText("Move_LeftAndRightAllow")
		panelMove.add(chkboxMoveLeftAndRightAllow)

		// 左右同時押ししたときに前 frame の input を優先
		chkboxMoveLeftAndRightUsePreviousInput.text = getUIText("Move_LeftAndRightUsePreviousInput")
		panelMove.add(chkboxMoveLeftAndRightUsePreviousInput)

		// Shift lock
		chkboxMoveShiftLockEnable.text = getUIText("Move_ShiftLock")
		panelMove.add(chkboxMoveShiftLockEnable)

		// rotationパターン補正タブ ------------------------------------------------
		val panelPieceOffset = Pane()
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
		val panelPieceOffsetX = Pane()
		panelPieceOffsetX.layout = BoxLayout(panelPieceOffsetX, BoxLayout.Y_AXIS)
		tabPieceOffset?.addTab(getUIText("TabName_PieceOffsetX"), panelPieceOffsetX)

		val pPieceOffsetX = Array(Piece.PIECE_COUNT) {
			Pane().apply {
				panelPieceOffsetX.add(this)
				add(Label(getUIText("PieceName$it")))
			}
		}

		txtfldPieceOffsetX = Array(Piece.PIECE_COUNT) {i ->
			Array(Piece.DIRECTION_COUNT) {
				JTextField("", 5).apply {pPieceOffsetX[i].add(this)}
			}
		}
		// rotationパターン補正(Y)タブ --------------------------------------------------
		val panelPieceOffsetY = Pane().apply {
			layout = BoxLayout(this, BoxLayout.Y_AXIS)
			tabPieceOffset?.addTab(getUIText("TabName_PieceOffsetY"), this)
		}

		val pPieceOffsetY = Array(Piece.PIECE_COUNT) {
			Pane().apply {
				panelPieceOffsetY.add(this)
				add(Label(getUIText("PieceName$it")))
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
		val panelPieceSpawn = Pane().apply {
			layout = BoxLayout(this, BoxLayout.Y_AXIS)
			tabPane.addTab(getUIText("TabName_PieceSpawn"), this)
		}

		tabPieceSpawn = JTabbedPane()
		panelPieceSpawn.add(tabPieceSpawn)

		// 出現位置補正(X)タブ --------------------------------------------------
		val panelPieceSpawnX = Pane()
		panelPieceSpawnX.layout = BoxLayout(panelPieceSpawnX, BoxLayout.Y_AXIS)
		tabPieceSpawn?.addTab(getUIText("TabName_PieceSpawnX"), panelPieceSpawnX)

		val pPieceSpawnX = Array(Piece.PIECE_COUNT) {
			Pane().apply {
				add(Label(getUIText("PieceName$it")))
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
		val panelPieceSpawnY = Pane().apply {
			layout = BoxLayout(this, BoxLayout.Y_AXIS)
			tabPieceSpawn?.addTab(getUIText("TabName_PieceSpawnY"), this)
		}

		val pPieceSpawnY = Array(Piece.PIECE_COUNT) {
			Pane().apply {
				panelPieceSpawnY.add(this)
				add(Label(getUIText("PieceName$it")))
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
		val panelPieceSpawnBigX = Pane().apply {
			layout = BoxLayout(this, BoxLayout.Y_AXIS)
			tabPieceSpawn?.addTab(getUIText("TabName_PieceSpawnBigX"), this)
		}

		val pPieceSpawnBigX = Array(Piece.PIECE_COUNT) {
			Pane().apply {
				panelPieceSpawnBigX.add(this)
				add(Label(getUIText("PieceName$it")))
			}
		}

		txtfldPieceSpawnBigX = Array(Piece.PIECE_COUNT) {i ->
			Array(Piece.DIRECTION_COUNT) {
				JTextField("", 5).apply {pPieceSpawnBigX[i].add(this)}
			}
		}
		// Big時出現位置補正(Y)タブ --------------------------------------------------
		val panelPieceSpawnBigY = Pane().apply {
			layout = BoxLayout(this, BoxLayout.Y_AXIS)
			tabPieceSpawn?.addTab(getUIText("TabName_PieceSpawnBigY"), this)
		}

		val pPieceSpawnBigY = Array(Piece.PIECE_COUNT) {
			Pane().apply {
				panelPieceSpawnBigY.add(this)
				add(Label(getUIText("PieceName$it")))
			}
		}

		txtfldPieceSpawnBigY = Array(Piece.PIECE_COUNT) {i ->
			Array(Piece.DIRECTION_COUNT) {
				JTextField("", 5).apply {pPieceSpawnBigY[i].add(this)}
			}
		}

		// 色設定タブ --------------------------------------------------
		val panelPieceColor = Pane().apply {
			layout = BoxLayout(this, BoxLayout.X_AXIS)
			tabPane.addTab(getUIText("TabName_PieceColor"), this)
		}

		val strColorNames = Array(Block.COLOR.COUNT-1) {getUIText("ColorName$it")}

		val pColorRow = Array(2) {
			Pane().apply {
				layout = BoxLayout(this, BoxLayout.Y_AXIS)
				panelPieceColor.add(this)
			}		}
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
			Pane().apply {
				pColorRow[0].add(this)
				add(Label(getUIText("PieceName$it")))
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
		val panelPieceDirection = Pane()
		panelPieceDirection.layout = BoxLayout(panelPieceDirection, BoxLayout.X_AXIS)
		tabPane.addTab(getUIText("TabName_PieceDirection"), panelPieceDirection)

		val pDirectRow = Array(2) {
			Pane().apply {
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
			Pane().apply {
				pDirectRow[0].add(this)
				add(Label(getUIText("PieceName$it")))
			}
		}

		comboboxPieceDirection = Array(Piece.PIECE_COUNT) {
			JComboBox(strDirectionNames).apply {
				preferredSize = Dimension(150, 30)
				maximumRowCount = strDirectionNames.size
				pPieceDirection[it].add(this)
			}
		}}

	/** Block画像を読み込み */
	private fun loadBlockSkins() {
		val skinDir = propGlobal.custom.skinDir

		var numBlocks = 0
		while(File("$skinDir/graphics/blockskin/normal/n$numBlocks.png").canRead()) numBlocks++
		log.debug("$numBlocks block skins found")

		imgBlockSkins = Array(numBlocks) {i ->
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
		var bufI:BufferedImage? = null
		try {
			bufI = ImageIO.read(url!!)
			log.debug("Loaded image from $url")
		} catch(e:IOException) {
			log.error("Failed to load image from ${url ?: ""}", e)
		}

		return bufI
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
			RuleOptions.LOCKRESET_LIMIT_OVER_NoReset -> radioLockDelayLockResetLimitOverNoReset?.isSelected = true
			RuleOptions.LOCKRESET_LIMIT_OVER_INSTANT -> radioLockDelayLockResetLimitOverInstant?.isSelected = true
			RuleOptions.LOCKRESET_LIMIT_OVER_NoKick -> radioLockDelayLockResetLimitOverNoWallkick?.isSelected = true
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
			r.lockresetLimitOver = RuleOptions.LOCKRESET_LIMIT_OVER_NoReset
		if(radioLockDelayLockResetLimitOverInstant!!.isSelected)
			r.lockresetLimitOver = RuleOptions.LOCKRESET_LIMIT_OVER_INSTANT
		if(radioLockDelayLockResetLimitOverNoWallkick!!.isSelected)
			r.lockresetLimitOver = RuleOptions.LOCKRESET_LIMIT_OVER_NoKick

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
						log.error("Failed to load rule data from $strNowFile", e2)
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
					}				} else {
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
	private inner class ComboLabel(
		var text:String = "",
		var icon:Icon? = null
	)

	/** 画像表示ComboボックスのListCellRenderer<br></br>
	 * [出典](http://www.javadrive.jp/tutorial/jcombobox/index20.html) */
	private inner class ComboLabelCellRenderer:Label(), ListCellRenderer<Any> {
		fun getListCellRendererComponent(list:JList<out Nothing>?, value:Nothing?, index:Int, isSelected:Boolean,
			cellHasFocus:Boolean):Component {
			TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
		}

		init {
			isOpaque = true
		}

		override fun getListCellRendererComponent(list:ListView<*>, value:Any, index:Int, isSelected:Boolean,
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
		}}

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

			launch(RuleEditorFX::class.java, *args)
		}
	}
}
*/
