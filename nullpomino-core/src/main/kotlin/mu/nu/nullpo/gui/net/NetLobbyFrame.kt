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
package mu.nu.nullpo.gui.net

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import mu.nu.nullpo.game.component.RuleOptions
import mu.nu.nullpo.game.net.NetBaseClient
import mu.nu.nullpo.game.net.NetMessageListener
import mu.nu.nullpo.game.net.NetPlayerClient
import mu.nu.nullpo.game.net.NetPlayerInfo
import mu.nu.nullpo.game.net.NetRoomInfo
import mu.nu.nullpo.game.net.NetUtil
import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.game.play.GameManager
import mu.nu.nullpo.game.play.GameStyle
import mu.nu.nullpo.game.subsystem.mode.NetDummyMode
import mu.nu.nullpo.gui.common.ConfigGlobal
import mu.nu.nullpo.gui.common.ConfigGlobal.RuleConf
import mu.nu.nullpo.tool.ruleeditor.RuleEditor
import mu.nu.nullpo.util.CustomProperties
import mu.nu.nullpo.util.GeneralUtil
import mu.nu.nullpo.util.GeneralUtil.strDateTime
import mu.nu.nullpo.util.GeneralUtil.toTimeStr
import org.apache.logging.log4j.LogManager
import java.awt.BorderLayout
import java.awt.CardLayout
import java.awt.Color
import java.awt.Component
import java.awt.Dimension
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.awt.image.BufferedImage
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.FileReader
import java.io.IOException
import java.io.PrintWriter
import java.net.MalformedURLException
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.GregorianCalendar
import java.util.LinkedList
import java.util.Locale
import java.util.Vector
import java.util.zip.Adler32
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import javax.imageio.ImageIO
import javax.swing.*
import javax.swing.table.DefaultTableModel
import javax.swing.text.JTextComponent
import javax.swing.text.SimpleAttributeSet
import javax.swing.text.StyleConstants

/** NullpoMino NetLobby */
/** Constructor */
class NetLobbyFrame:JFrame(), ActionListener, NetMessageListener {
	/** NetPlayerClient */
	var netPlayerClient:NetPlayerClient? = null

	/** Rule data */
	var ruleOptPlayer:RuleOptions? = null
	var ruleOptLock:RuleOptions? = null

	/** Map list */
	val mapList:LinkedList<String> = LinkedList()

	/** Event listeners */
	private var listeners:LinkedList<NetLobbyListener>? = LinkedList()

	/** Preset info */
	private val presets = LinkedList<NetRoomInfo>()

	/** Current game mode (act as special NetLobbyListener) */
	var netDummyMode:NetDummyMode? = null

	/** Property file for lobby settings */
	private val propConfig = CustomProperties()

	/** Property file for global settings */
	private var propGlobal = ConfigGlobal()

	/** Property file for swing settings */
	private val propSwingConfig = CustomProperties()

	/** Property file for observer ("Watch") settings */
	private val propObserver = CustomProperties()

	/** Default game mode description file */
	private val propDefaultModeDesc = CustomProperties()

	/** Game mode description file */
	private val propModeDesc = CustomProperties()

	/** Default language file */
	private val propLangDefault = CustomProperties()

	/** Property file for GUI translations */
	private val propLang = CustomProperties()

	/** Current screen-card number */
	private var currentScreenCardNumber = 0

	/** Current room ID (for View Detail) */
	private var currentViewDetailRoomID = -1

	/** NetRoomInfo for settings backup */
	private var backupRoomInfo:NetRoomInfo? = null

	/** NetRoomInfo for settings backup (1P) */
	private var backupRoomInfo1P:NetRoomInfo? = null

	/** PrintWriter for lobby log */
	private var writerLobbyLog:PrintWriter? = null

	/** PrintWriter for room log */
	private var writerRoomLog:PrintWriter? = null

	/** Rated-game rule name list */
	private val listRatedRuleName:Array<LinkedList<String>> = Array(GameEngine.MAX_GAMESTYLE) {LinkedList<String>()}

	/** Layout manager for main screen */
	private val contentPaneCardLayout:CardLayout = CardLayout()

	/** Menu bars (all screens) */
	private val menuBar:Array<JMenuBar> = Array(SCREENCARD_NAMES.size) {JMenuBar()}

	/** Text field of player name (Server select screen) */
	private val txtfldPlayerName:JTextField = JTextField()

	/** Text field of team name (Server select screen) */
	private val txtfldPlayerTeam:JTextField = JTextField()

	/** Listbox for servers (Server select screen) */
	private val listboxServerList:JList<String> = JList()

	/** Listbox data for servers (Server select screen) */
	private var listModelServerList:DefaultListModel<String> = DefaultListModel()

	/** Connect button (Server select screen) */
	private val btnServerConnect:JButton = JButton()

	/** Lobby/Room Tab */
	private val tabLobbyAndRoom:JTabbedPane = JTabbedPane()

	/** JSplitPane (Lobby screen) */
	private val splitLobby:JSplitPane = JSplitPane()

	/** At the top of the lobby screen layout manager */
	private val roomListTopBarCardLayout:CardLayout = CardLayout()

	/** Panel at the top of the lobby screen */
	private val subpanelRoomListTopBar:JPanel = JPanel()

	/** Lobby popup menu (Lobby screen) */
	private val popupLobbyOptions:JPopupMenu = JPopupMenu()

	/** Rule change menu inum (Lobby screen) */
	private val itemLobbyMenuRuleChange:JMenuItem = JMenuItem()

	/** Team change menu inum (Lobby screen) */
	private val itemLobbyMenuTeamChange:JMenuItem = JMenuItem()

	/** Leaderboard menu inum (Lobby screen) */
	private val itemLobbyMenuRanking:JMenuItem = JMenuItem()

	/** Quick Start button(Lobby screen) */
	private val btnRoomListQuickStart:JButton = JButton()

	/** Create a Room button(Lobby screen) */
	private val btnRoomListRoomCreate:JButton = JButton()

	/** Create Room 1P (Lobby screen) */
	private val btnRoomListRoomCreate1P:JButton = JButton()

	/** Options menu button (Lobby screen) */
	private val btnRoomListOptions:JButton = JButton()

	/** Team name input Column(Lobby screen) */
	private val txtfldRoomListTeam:JTextField = JTextField()

	/** Room list table */
	private val tableRoomList:JTable = JTable()

	/** Room list tableの data */
	private var tablemodelRoomList:DefaultTableModel = DefaultTableModel()

	/** Chat logAndPlayerPartition line of the list(Lobby screen) */
	private val splitLobbyChat:JSplitPane = JSplitPane()

	/** Chat log(Lobby screen) */
	private val txtpaneLobbyChatLog:JTextPane = JTextPane()

	/** PlayerList(Lobby screen) */
	private var listboxLobbyChatPlayerList:JList<String> = JList()

	/** PlayerList(Lobby screen)Of data */
	private var listmodelLobbyChatPlayerList:DefaultListModel<String> = DefaultListModel()

	/** Chat input Column(Lobby screen) */
	private val txtfldLobbyChatInput:JTextField = JTextField()

	/** Submit chat button(Lobby screen) */
	private val btnLobbyChatSend:JButton = JButton()

	/** Participation in a war button(Room screen) */
	private val btnRoomButtonsJoin:JButton = JButton()

	/** Withdrawal button(Room screen) */
	private val btnRoomButtonsSitOut:JButton = JButton()

	/** Change team button(Room screen) */
	private val btnRoomButtonsTeamChange:JButton = JButton()

	/** View Settings button (Room screen) */
	private val btnRoomButtonsViewSetting:JButton = JButton()

	/** Leaderboard button (Room screen) */
	private val btnRoomButtonsRanking:JButton = JButton()

	/** Partition line separating the upper and lower(Room screen) */
	private val splitRoom:JSplitPane = JSplitPane()

	/** Room at the top of the screen layout manager */
	private val roomTopBarCardLayout:CardLayout = CardLayout()

	/** Top panel room screen */
	private val subPanelRoomTopBar:JPanel = JPanel()

	/** Game stats panel */
	private val subPanelGameStat:JPanel = JPanel()

	/** CardLayout for game stats */
	private var gameStatCardLayout:CardLayout = CardLayout()

	/** Multiplayer game stats table */
	private var tableGameStat:JTable = JTable()

	/** Multiplayer game stats table data */
	private val tableModelGameStat:DefaultTableModel = DefaultTableModel()

	/** Multiplayer game stats table */
	private val tableGameStat1P:JTable = JTable()

	/** Multiplayer game stats table data */
	private val tableModelGameStat1P:DefaultTableModel = DefaultTableModel()

	/** Chat logAndPlayerPartition line of the list(Room screen) */
	private val splitRoomChat:JSplitPane = JSplitPane()

	/** Chat log(Room screen) */
	private val txtPaneRoomChatLog:JTextPane = JTextPane()

	/** PlayerList(Room screen) */
	private val listboxRoomChatPlayerList:JList<String> = JList()

	/** PlayerList(Room screen)Of data */
	private val listModelRoomChatPlayerList:DefaultListModel<String> = DefaultListModel()

	/** The same roomPlayerInformation */
	/** Being in the same roomPlayerReturns a list(The update does not)
	 * @return Being in the same roomPlayerList
	 */
	private val sameRoomPlayerInfoList:LinkedList<NetPlayerInfo> = LinkedList()

	/** Chat input Column(Room screen) */
	private val txtfldRoomChatInput:JTextField = JTextField()

	/** Submit chat button(Room screen) */
	private val btnRoomChatSend:JButton = JButton()
	/** Team name input Column(Room screen) */
	private val txtfldRoomTeam:JTextField = JTextField()

	/** Host name input Column(Server add screen) */
	private val txtfldServerAddHost:JTextField = JTextField()

	/** OK button(Server add screen) */
	private val btnServerAddOK:JButton = JButton()

	private val txtfldCreateRatedName:JTextField = JTextField()

	/** Cancel button (Created rated waiting screen) */
	private val btnCreateRatedWaitingCancel:JButton = JButton()

	/** Presets box (Create rated screen) */
	private val comboboxCreateRatedPresets:JComboBox<String> = JComboBox()

	/** People participatecount(Create rated screen) */
	private val spinnerCreateRatedMaxPlayers:JSpinner = JSpinner()

	/** OK button (Create rated screen) */
	private val btnCreateRatedOK:JButton = JButton()

	/** Custom button (Create rated screen) */
	private val btnCreateRatedCustom:JButton = JButton()

	/** Cancel button (Created rated screen) */
	private val btnCreateRatedCancel:JButton = JButton()

	/** ルーム名(Create room screen) */
	private val txtfldCreateRoomName:JTextField = JTextField()

	/** Game Mode (Create room screen) */
	private val comboboxCreateRoomMode:JComboBox<String> = JComboBox()

	/** People participatecount(Create room screen) */
	private val spinnerCreateRoomMaxPlayers:JSpinner = JSpinner()

	/** To wait before auto-start time(Create room screen) */
	private val spinnerCreateRoomAutoStartSeconds:JSpinner = JSpinner()

	/** Molecular fall velocity(Create room screen) */
	private val spinnerCreateRoomGravity:JSpinner = JSpinner()

	/** Denominator-fall velocity(Create room screen) */
	private val spinnerCreateRoomDenominator:JSpinner = JSpinner()

	/** ARE(Create room screen) */
	private val spinnerCreateRoomARE:JSpinner = JSpinner()

	/** ARE after line clear(Create room screen) */
	private val spinnerCreateRoomARELine:JSpinner = JSpinner()

	/** Line clear time(Create room screen) */
	private val spinnerCreateRoomLineDelay:JSpinner = JSpinner()

	/** Fixation time(Create room screen) */
	private val spinnerCreateRoomLockDelay:JSpinner = JSpinner()

	/** Horizontal reservoir(Create room screen) */
	private val spinnerCreateRoomDAS:JSpinner = JSpinner()

	/** HurryupSeconds before the startcount(Create room screen) */
	private val spinnerCreateRoomHurryupSeconds:JSpinner = JSpinner()

	/** HurryupTimes afterBlockDo you run up the floor every time you put
	 * the(Create room screen) */
	private val spinnerCreateRoomHurryupInterval:JSpinner = JSpinner()

	/** MapSetID(Create room screen) */
	private val spinnerCreateRoomMapSetID:JSpinner = JSpinner()

	/** Rate of change of garbage holes */
	private val spinnerCreateRoomGarbagePercent:JSpinner = JSpinner()

	/** Map is enabled(Create room screen) */
	private val chkboxCreateRoomUseMap:JCheckBox = JCheckBox()

	/** Of all fixed rules(Create room screen) */
	private val chkboxCreateRoomRuleLock:JCheckBox = JCheckBox()

	/** Spin bonusType(Create room screen) */
	private val comboboxCreateRoomTWISTEnableType:JComboBox<String> = JComboBox()

	/** Flag for enabling B2B(Create room screen) */
	private val chkboxCreateRoomB2B:JCheckBox = JCheckBox()

	/** Flag for enabling combos(Create room screen) */
	private val chkboxCreateRoomCombo:JCheckBox = JCheckBox()

	/** Allow Rensa/Combo Block */
	private val chkboxCreateRoomRensaBlock:JCheckBox = JCheckBox()

	/** Allow countering */
	private val chkboxCreateRoomCounter:JCheckBox = JCheckBox()

	/** Enable bravo bonus */
	private val chkboxCreateRoomBravo:JCheckBox = JCheckBox()

	/** Allow EZ spins */
	private val chkboxCreateRoomTWISTEnableEZ:JCheckBox = JCheckBox()

	/** 3If I live more than Attack Reduce the force(Create room screen) */
	private val chkboxCreateRoomReduceLineSend:JCheckBox = JCheckBox()

	/** Set garbage type */
	private val chkboxCreateRoomGarbageChangePerAttack:JCheckBox = JCheckBox()

	/** Set garbage type */
	private val chkboxCreateRoomDivideChangeRateByPlayers:JCheckBox = JCheckBox()

	/** B2B chunk type */
	private val chkboxCreateRoomB2BChunk:JCheckBox = JCheckBox()

	/** Fragmentarygarbage blockUsing the system(Create room screen) */
	private val chkboxCreateRoomUseFractionalGarbage:JCheckBox = JCheckBox()

	/** Use Target System (Create room screen) */
	private val chkboxCreateRoomIsTarget:JCheckBox = JCheckBox()

	/** Spinner for Targeting time (Create room screen) */
	private val spinnerCreateRoomTargetTimer:JSpinner = JSpinner()

	/** TNET2TypeAutomatically start timerI use(Create room screen) */
	private val chkboxCreateRoomAutoStartTNET2:JCheckBox = JCheckBox()

	/** SomeoneCancelWasTimerInvalidation(Create room screen) */
	private val chkboxCreateRoomDisableTimerAfterSomeoneCancelled:JCheckBox = JCheckBox()

	/** Preset number (Create room screen) */
	private val spinnerCreateRoomPresetID:JSpinner = JSpinner()

	/** Preset code (Create room screen) */
	private val txtfldCreateRoomPresetCode:JTextField = JTextField()

	/** OK button(Create room screen) */
	private val btnCreateRoomOK:JButton = JButton()

	/** Participation in a war button(Create room screen) */
	private val btnCreateRoomJoin:JButton = JButton()

	/** Spectator button(Create room screen) */
	private val btnCreateRoomWatch:JButton = JButton()

	/** Cancel Button (Create room screen) */
	private val btnCreateRoomCancel:JButton = JButton()

	/** Game mode label (Create room 1P screen) */
	private val labelCreateRoom1PGameMode:JLabel = JLabel()

	/** Game mode listbox (Create room 1P screen) */
	private val listboxCreateRoom1PModeList:JList<String> = JList()

	/** Game mode list data (Create room 1P screen) */
	private var listmodelCreateRoom1PModeList:DefaultListModel<String> = DefaultListModel()

	/** Rule-list listbox (Create room 1P screen) */
	private val listboxCreateRoom1PRuleList:JList<String> = JList()

	/** Rule-list list data (Create room 1P screen) */
	private var listmodelCreateRoom1PRuleList:DefaultListModel<String> = DefaultListModel()

	/** OK button (Create room 1P screen) */
	private val btnCreateRoom1POK:JButton = JButton()

	/** Cancel button (Create room 1P screen) */
	private val btnCreateRoom1PCancel:JButton = JButton()

	/** Tab (MPRanking screen) */
	private val tabMPRanking:JTabbedPane = JTabbedPane()

	/** Table of multiplayer leaderboard (MPRanking screen) */
	private val tableMPRanking:Array<JTable> = Array(GameEngine.MAX_GAMESTYLE) {
		JTable()
	}

	/** Table data of multiplayer leaderboard (MPRanking screen) */
	private var tablemodelMPRanking:Array<DefaultTableModel> = Array(GameEngine.MAX_GAMESTYLE) {
		DefaultTableModel(MPRANKING_COLUMNNAMES.map {getUIText(it)}.toTypedArray(), 0)
	}

	/** OK button (MPRanking screen) */
	private val btnMPRankingOK:JButton = JButton()

	/** Tab (Rule change screen) */
	private val tabRuleChange:JTabbedPane = JTabbedPane()

	/** Rule list listbox (Rule change screen) */
	private var listboxRuleChangeRuleList:Array<JList<String>> = emptyArray()

	/** OK button (Rule change screen) */
	private val btnRuleChangeOK:JButton = JButton()

	/** Cancel button (Rule change screen) */
	private val btnRuleChangeCancel:JButton = JButton()

	/** Rule entries (Rule change screen) */
	private var ruleEntries:LinkedList<RuleConf> = LinkedList()

	/** Tuning: A button rotation Combobox */
	private var comboboxTuningSpinDirection:JComboBox<String> = JComboBox()
	/** Tuning: Diagonal move Combobox */
	private var comboboxTuningMoveDiagonal:JComboBox<String> = JComboBox()
	/** Tuning: Show Outline Only Combobox */
	private var comboboxTuningBlockShowOutlineOnly:JComboBox<String> = JComboBox()
	/** Tuning: Skin Combobox */
	private val comboboxTuningSkin:JComboBox<ComboLabel> = JComboBox()
	/** Tuning: Skin Images */
	private var imgTuningBlockSkins:Array<BufferedImage> = Array(0) {BufferedImage(0, 0, 0)}
	/** Tuning: Outline type combobox */
	private var comboboxTuningBlockOutlineType:JComboBox<String> = JComboBox()
	/** Tuning: Minimum DAS */
	private val txtfldTuningMinDAS:JTextField = JTextField()
	/** Tuning: Maximum DAS */
	private val txtfldTuningMaxDAS:JTextField = JTextField()
	/** Tuning: DAS dealy */
	private val txtfldTuningDasDelay:JTextField = JTextField()
	/** Tuning: Checkbox to enable swapping the roles of up/down buttons
	 * in-game */
	private val chkboxTuningReverseUpDown:JCheckBox = JCheckBox()

	/** @return Current ScreenChat log
	 */
	private val currentChatLogTextPane:JTextPane
		get() = if(tabLobbyAndRoom.selectedIndex!=0) txtPaneRoomChatLog else txtpaneLobbyChatLog

	/** Get current time as String (for chat log)
	 * @return Current time as String
	 */
	private val currentTimeAsString:String
		get() = SimpleDateFormat("HH:mm:ss").format(GregorianCalendar().time)

	/** Get currenlty selected values set ID
	 * @return Map set ID
	 */
	private val currentSelectedMapSetID:Int
		get() = spinnerCreateRoomMapSetID.let {it.value as Int}

	/** Get rule file list (for rule change screen)
	 * @return Rule file list. null if directory doesn't exist.
	 */
	// Sort if not windows
	private val ruleFileList:Array<String>?
		get() {
			val dir = File("config/rule")

			val list = dir.list {_, name -> name.endsWith(".rul")}

			if(!System.getProperty("os.name").startsWith("Windows"))
				list?.sort()

			return list
		}

	/** Initialization */
	fun init() {
		// Read configuration file
		try {
			val `in` = FileInputStream("config/setting/netlobby.cfg")
			propConfig.load(`in`)
			`in`.close()
		} catch(_:IOException) {
		}

		// Load global settings
		try {
			propGlobal = Json.decodeFromString(FileInputStream("config/setting/global.json").bufferedReader().use {it.readText()})
		} catch(_:Exception) {
		}

		// SwingRead version of the configuration file
		try {
			val `in` = FileInputStream("config/setting/swing.cfg")
			propSwingConfig.load(`in`)
			`in`.close()
		} catch(_:IOException) {
		}

		// ObserverFunction read configuration file
		try {
			val `in` = FileInputStream("config/setting/netobserver.cfg")
			propObserver.load(`in`)
			`in`.close()
		} catch(_:IOException) {
		}

		// Game mode description
		try {
			val `in` = FileInputStream("config/lang/modedesc_default.xml")
			propDefaultModeDesc.loadFromXML(`in`)
			`in`.close()
		} catch(e:IOException) {
			log.error("Couldn't load default mode description file", e)
		}

		try {
			val `in` = FileInputStream("config/lang/modedesc_${Locale.getDefault().country}.xml")
			propModeDesc.loadFromXML(`in`)
			`in`.close()
		} catch(_:IOException) {
		}

		// Read language file
		try {
			val `in` = FileInputStream("config/lang/netlobby_default.xml")
			propLangDefault.loadFromXML(`in`)
			`in`.close()
		} catch(e:Exception) {
			log.error("Couldn't load default UI language file", e)
		}

		try {
			val `in` = FileInputStream("config/lang/netlobby_${Locale.getDefault().country}.xml")
			propLang.loadFromXML(`in`)
			`in`.close()
		} catch(_:IOException) {
		}

		// Look&FeelSetting
		if(propSwingConfig.getProperty("option.usenativelookandfeel", true))
			try {
				UIManager.getInstalledLookAndFeels()
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
			} catch(e:Exception) {
				log.warn("Failed to set native look&feel", e)
			}

		// WindowListenerSign up
		addWindowListener(object:WindowAdapter() {
			override fun windowClosing(e:WindowEvent?) {
				shutdown()
			}
		})

		// Rated-game rule name list
		listRatedRuleName
		// Map list
		mapList.clear()

		// Rule files
		val strRuleFileList = ruleFileList
		if(strRuleFileList==null)
			log.error("Rule file directory not found")
		else
			createRuleEntries(strRuleFileList)

		// Block skins
		loadBlockSkins()

		// GUI Init
		defaultCloseOperation = WindowConstants.DO_NOTHING_ON_CLOSE
		title = getUIText("Title_NetLobby")

		initUI()

		this.setSize(propConfig.getProperty("mainwindow.width", 500), propConfig.getProperty("mainwindow.height", 450))
		this.setLocation(propConfig.getProperty("mainwindow.x", 0), propConfig.getProperty("mainwindow.y", 0))

		// ListenerCall
		if(listeners!=null)
			for(l in listeners!!)
				l.onLobbyInit(this)
		if(netDummyMode!=null) netDummyMode!!.onLobbyInit(this)
	}

	/** GUI Initialization */
	private fun initUI() {
		contentPane.layout = contentPaneCardLayout

		initServerSelectUI()
		initLobbyUI()
		initServerAddUI()
		initCreateRatedWaitingUI()
		initCreateRatedUI()
		initCreateRoomUI()
		initCreateRoom1PUI()
		initMPRankingUI()
		initRuleChangeUI()

		changeCurrentScreenCard(SCREENCARD_SERVERSELECT)
	}

	/** Server select screen initialization */
	private fun initServerSelectUI() {
		// And server selectionName input Screen
		val mainpanelServerSelect = JPanel(BorderLayout())
		contentPane.add(mainpanelServerSelect, SCREENCARD_NAMES[SCREENCARD_SERVERSELECT])

		// * NameAndTeam name input Panel
		val subpanelNames = JPanel()
		subpanelNames.layout = BoxLayout(subpanelNames, BoxLayout.Y_AXIS)
		mainpanelServerSelect.add(subpanelNames, BorderLayout.NORTH)

		// ** Name input Panel
		val subpanelNameEntry = JPanel(BorderLayout())
		subpanelNames.add(subpanelNameEntry)

		// *** &#39;Name:&quot;Label
		subpanelNameEntry.add(JLabel(getUIText("ServerSelect_LabelName")), BorderLayout.WEST)

		// *** Name input Column
		txtfldPlayerName.componentPopupMenu = TextComponentPopupMenu(txtfldPlayerName)
		txtfldPlayerName.text = propConfig.getProperty("serverselect.txtfldPlayerName.text", "")
		subpanelNameEntry.add(txtfldPlayerName, BorderLayout.CENTER)

		// ** Team name input Panel
		val subpanelTeamEntry = JPanel(BorderLayout())
		subpanelNames.add(subpanelTeamEntry)

		// *** &#39;Team name:&quot;Label
		subpanelTeamEntry.add(JLabel(getUIText("ServerSelect_LabelTeam")), BorderLayout.WEST)

		// *** Team name input Column
		txtfldPlayerTeam.componentPopupMenu = TextComponentPopupMenu(txtfldPlayerTeam)
		txtfldPlayerTeam.text = propConfig.getProperty("serverselect.txtfldPlayerTeam.text", "")
		subpanelTeamEntry.add(txtfldPlayerTeam, BorderLayout.CENTER)

		// * Server selection list box
		if(GameManager.isDevBuild) {
			if(!loadListToDefaultListModel(listModelServerList, "config/setting/netlobby_serverlist_dev.cfg")) {
				loadListToDefaultListModel(listModelServerList, "config/list/netlobby_serverlist_default_dev.lst")
				saveListFromDefaultListModel(listModelServerList, "config/setting/netlobby_serverlist_dev.cfg")
			}
		} else if(!loadListToDefaultListModel(listModelServerList, "config/setting/netlobby_serverlist.cfg")) {
			loadListToDefaultListModel(listModelServerList, "config/list/netlobby_serverlist_default.lst")
			saveListFromDefaultListModel(listModelServerList, "config/setting/netlobby_serverlist.cfg")
		}
		val spListboxServerSelect = JScrollPane(listboxServerList.apply {
			model = listModelServerList
			componentPopupMenu = ServerSelectListBoxPopupMenu()
			addMouseListener(ServerSelectListBoxMouseAdapter())
			setSelectedValue(propConfig.getProperty("serverselect.listboxServerList.value", ""), true)
		})
		mainpanelServerSelect.add(spListboxServerSelect, BorderLayout.CENTER)

		// * Panel add or remove server
		val subpanelServerAdd = JPanel()
		subpanelServerAdd.layout = BoxLayout(subpanelServerAdd, BoxLayout.Y_AXIS)
		mainpanelServerSelect.add(subpanelServerAdd, BorderLayout.EAST)

		// ** Add Server button
		subpanelServerAdd.add(JButton(getUIText("ServerSelect_ServerAdd")).also {
			it.maximumSize = Dimension(Short.MAX_VALUE.toInt(), it.maximumSize.height)
			it.addActionListener(this)
			it.actionCommand = "ServerSelect_ServerAdd"
			it.setMnemonic('A')
		})

		// ** Delete server button
		subpanelServerAdd.add(JButton(getUIText("ServerSelect_ServerDelete")).also {
			it.maximumSize = Dimension(Short.MAX_VALUE.toInt(), it.maximumSize.height)
			it.addActionListener(this)
			it.actionCommand = "ServerSelect_ServerDelete"
			it.setMnemonic('D')
		})

		// ** Monitoring settings button
		subpanelServerAdd.add(JButton(getUIText("ServerSelect_SetObserver")).also {
			it.maximumSize = Dimension(Short.MAX_VALUE.toInt(), it.maximumSize.height)
			it.addActionListener(this)
			it.actionCommand = "ServerSelect_SetObserver"
			it.setMnemonic('S')
		})

		// ** Unmonitor button
		subpanelServerAdd.add(JButton(getUIText("ServerSelect_UnsetObserver")).also {
			it.maximumSize = Dimension(Short.MAX_VALUE.toInt(), it.maximumSize.height)
			it.addActionListener(this)
			it.actionCommand = "ServerSelect_UnsetObserver"
			it.setMnemonic('U')
		})

		// * Connection button·Exit buttonPanel
		val subpanelServerSelectButtons = JPanel()
		subpanelServerSelectButtons.layout = BoxLayout(subpanelServerSelectButtons, BoxLayout.X_AXIS)
		mainpanelServerSelect.add(subpanelServerSelectButtons, BorderLayout.SOUTH)

		// ** Connection button
		btnServerConnect.text = getUIText("ServerSelect_Connect")
		btnServerConnect.maximumSize = Dimension(Short.MAX_VALUE.toInt(), btnServerConnect.maximumSize.height)
		btnServerConnect.addActionListener(this)
		btnServerConnect.actionCommand = "ServerSelect_Connect"
		btnServerConnect.setMnemonic('C')
		subpanelServerSelectButtons.add(btnServerConnect)

		// ** Exit button
		subpanelServerSelectButtons.add(JButton(getUIText("ServerSelect_Exit")).also {
			it.maximumSize = Dimension(Short.MAX_VALUE.toInt(), it.maximumSize.height)
			it.addActionListener(this)
			it.actionCommand = "ServerSelect_Exit"
			it.setMnemonic('X')
		})
	}

	/** Lobby screen initialization */
	private fun initLobbyUI() {
		contentPane.add(tabLobbyAndRoom, SCREENCARD_NAMES[SCREENCARD_LOBBY])

		// === Popup Menu ===

		// * Popup Menu

		// ** Rule change
		popupLobbyOptions.add(itemLobbyMenuRuleChange.also {
			it.text = getUIText("Lobby_RuleChange")
			it.addActionListener(this)
			it.actionCommand = "Lobby_RuleChange"
			it.setMnemonic('R')
			it.toolTipText = getUIText("Lobby_RuleChange_Tip")
		})

		// ** Team change
		popupLobbyOptions.add(itemLobbyMenuTeamChange.also {
			it.text = getUIText("Lobby_TeamChange")
			it.addActionListener(this)
			it.actionCommand = "Lobby_TeamChange"
			it.setMnemonic('T')
			it.toolTipText = getUIText("Lobby_TeamChange_Tip")
		})

		// ** Leaderboard
		popupLobbyOptions.add(itemLobbyMenuRanking.also {
			it.text = getUIText("Lobby_Ranking")
			it.addActionListener(this)
			it.actionCommand = "Lobby_Ranking"
			it.setMnemonic('K')
			it.toolTipText = getUIText("Lobby_Ranking_Tip")
		})

		// === Lobby Tab ===
		val mainpanelLobby = JPanel(BorderLayout())
		//this.getContentPane().add(mainpanelLobby, SCREENCARD_NAMES[SCREENCARD_LOBBY]);
		tabLobbyAndRoom.addTab(getUIText("Lobby_Tab_Lobby"), mainpanelLobby)
		tabLobbyAndRoom.setMnemonicAt(0, 'Y'.code)

		// * Partition line separating the upper and lower
		mainpanelLobby.add(splitLobby.apply {
			orientation = JSplitPane.VERTICAL_SPLIT
			dividerLocation = propConfig.getProperty("lobby.splitLobby.location", 200)
		}, BorderLayout.CENTER)

		// ** Room list(Top)
		val subpanelRoomList = JPanel(BorderLayout()).apply {
			minimumSize = Dimension(0, 0)
		}
		splitLobby.topComponent = subpanelRoomList
		// *** Top of the screen panel lobby
		subpanelRoomListTopBar.layout = roomListTopBarCardLayout
		subpanelRoomList.add(subpanelRoomListTopBar, BorderLayout.NORTH)

		// **** Room list buttonKind
		val subpanelRoomListButtons = JPanel()
		subpanelRoomListTopBar.add(subpanelRoomListButtons, "Buttons")
		//subpanelRoomList.add(subpanelRoomListButtons, BorderLayout.NORTH);

		// ***** TODO:Quick Start button
		subpanelRoomListButtons.add(btnRoomListQuickStart.also {
			it.text = getUIText("Lobby_QuickStart")
			it.addActionListener(this)
			it.actionCommand = "Lobby_QuickStart"
			it.setMnemonic('Q')
			it.toolTipText = getUIText("Lobby_QuickStart_Tip")
			it.isVisible = false
		})

		// ***** Create a Room button
		subpanelRoomListButtons.add(btnRoomListRoomCreate.also {
			it.text = getUIText("Lobby_RoomCreate")
			it.addActionListener(this)
			it.actionCommand = "Lobby_RoomCreate"
			it.setMnemonic('N')
			it.toolTipText = getUIText("Lobby_RoomCreate_Tip")
		})

		// ***** Create Room (1P) button
		subpanelRoomListButtons.add(btnRoomListRoomCreate1P.also {
			it.text = getUIText("Lobby_RoomCreate1P")
			it.addActionListener(this)
			it.actionCommand = "Lobby_RoomCreate1P"
			it.setMnemonic('1')
			it.toolTipText = getUIText("Lobby_RoomCreate1P_Tip")
		})

		// ***** Options menu button
		subpanelRoomListButtons.add(btnRoomListOptions.also {
			it.text = getUIText("Lobby_Options")
			it.addActionListener(this)
			it.actionCommand = "Lobby_Options"
			it.setMnemonic('O')
			it.toolTipText = getUIText("Lobby_Options_Tip")
		})

		// ***** Cut button
		subpanelRoomListButtons.add(JButton(getUIText("Lobby_Disconnect")).also {
			it.addActionListener(this)
			it.actionCommand = "Lobby_Disconnect"
			it.setMnemonic('L')
			it.toolTipText = getUIText("Lobby_Disconnect_Tip")
		})

		// **** Panel change team
		val subpanelRoomListTeam = JPanel(BorderLayout())
		subpanelRoomListTopBar.add(subpanelRoomListTeam, "Team")

		// ***** Team name input Column
		txtfldRoomListTeam
		subpanelRoomListTeam.add(txtfldRoomListTeam, BorderLayout.CENTER)

		// ***** Team nameChange buttonPanel
		val subpanelRoomListTeamButtons = JPanel()
		subpanelRoomListTeam.add(subpanelRoomListTeamButtons, BorderLayout.EAST)

		// ****** Team nameChangeOK
		subpanelRoomListTeamButtons.add(JButton(getUIText("Lobby_TeamChange_OK")).also {
			it.addActionListener(this)
			it.actionCommand = "Lobby_TeamChange_OK"
			it.setMnemonic('O')
		})

		// ****** Team nameChangeCancel
		subpanelRoomListTeamButtons.add(JButton(getUIText("Lobby_TeamChange_Cancel")).also {
			it.addActionListener(this)
			it.actionCommand = "Lobby_TeamChange_Cancel"
			it.setMnemonic('C')
		})

		// *** Room list

		/** Room list tableのカラム名(翻訳後) */
		tablemodelRoomList.setColumnIdentifiers(Vector(ROOMTABLE_COLUMNNAMES.map {getUIText(it)}))
		tableRoomList.apply {
			model = tablemodelRoomList
			setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
			setDefaultEditor(Any::class.java, null)
			autoResizeMode = JTable.AUTO_RESIZE_OFF
			tableHeader.reorderingAllowed = false
			componentPopupMenu = RoomTablePopupMenu()
			addMouseListener(RoomTableMouseAdapter())
			addKeyListener(RoomTableKeyAdapter())
		}
		tableRoomList.columnModel.run {
			getColumn(0).preferredWidth = propConfig.getProperty("tableRoomList.width.id", 35) // ID
			getColumn(1).preferredWidth = propConfig.getProperty("tableRoomList.width.name", 155) // Name
			getColumn(2).preferredWidth = propConfig.getProperty("tableRoomList.width.rated", 50) // Rated
			getColumn(3).preferredWidth = propConfig.getProperty("tableRoomList.width.rulename", 105) // Rule name
			getColumn(4).preferredWidth = propConfig.getProperty("tableRoomList.width.modename", 105) // Mode name
			getColumn(5).preferredWidth = propConfig.getProperty("tableRoomList.width.status", 55) // Status
			getColumn(6).preferredWidth = propConfig.getProperty("tableRoomList.width.players", 65) // Players
			getColumn(7).preferredWidth = propConfig.getProperty("tableRoomList.width.spectators", 65) // Spectators
		}
		subpanelRoomList.add(JScrollPane(tableRoomList), BorderLayout.CENTER)

		// ** Chat(Under)
		val subpanelLobbyChat = JPanel(BorderLayout()).apply {
			minimumSize = Dimension(0, 0)
		}
		splitLobby.bottomComponent = subpanelLobbyChat

		// *** Chat logAndPlayerPartition line of the list
		splitLobbyChat.dividerLocation = propConfig.getProperty("lobby.splitLobbyChat.location", 350)

		subpanelLobbyChat.add(splitLobbyChat, BorderLayout.CENTER)

		// **** Chat log(Lobby screen)
		txtpaneLobbyChatLog.componentPopupMenu = LogPopupMenu(txtpaneLobbyChatLog)
		txtpaneLobbyChatLog.addKeyListener(LogKeyAdapter())
		val spTxtpaneLobbyChatLog = JScrollPane(txtpaneLobbyChatLog)
		spTxtpaneLobbyChatLog.minimumSize = Dimension(0, 0)
		splitLobbyChat.leftComponent = spTxtpaneLobbyChatLog

		// **** PlayerList(Lobby screen)
		listboxLobbyChatPlayerList = JList(listmodelLobbyChatPlayerList)
		listboxLobbyChatPlayerList.componentPopupMenu = ListBoxPopupMenu(listboxLobbyChatPlayerList)
		splitLobbyChat.rightComponent = JScrollPane(listboxLobbyChatPlayerList).apply {
			minimumSize = Dimension(0, 0)
		}

		// *** Chat input Column panel(Lobby screen)
		val subpanelLobbyChatInputArea = JPanel(BorderLayout())
		subpanelLobbyChat.add(subpanelLobbyChatInputArea, BorderLayout.SOUTH)

		// **** Chat input Column(Lobby screen)
		txtfldLobbyChatInput.componentPopupMenu = TextComponentPopupMenu(txtfldLobbyChatInput)
		subpanelLobbyChatInputArea.add(txtfldLobbyChatInput, BorderLayout.CENTER)

		// **** Submit chat button(Lobby screen)
		subpanelLobbyChatInputArea.add(btnLobbyChatSend.also {
			it.text = getUIText("Lobby_ChatSend")
			it.addActionListener(this)
			it.actionCommand = "Lobby_ChatSend"
			it.setMnemonic('S')
		}, BorderLayout.EAST)

		// === Room Tab ===
		val mainpanelRoom = JPanel(BorderLayout())
		//this.getContentPane().add(mainpanelRoom, SCREENCARD_NAMES[SCREENCARD_ROOM]);
		tabLobbyAndRoom.addTab(getUIText("Lobby_Tab_NoRoom"), mainpanelRoom)
		tabLobbyAndRoom.setMnemonicAt(1, 'R'.code)
		tabLobbyAndRoom.setEnabledAt(1, false)

		// * Partition line separating the upper and lower

		mainpanelRoom.add(splitRoom.apply {
			orientation = JSplitPane.VERTICAL_SPLIT
			dividerLocation = propConfig.getProperty("room.splitRoom.location", 200)
		}, BorderLayout.CENTER)

		// ** List of game results(Top)
		val subpanelRoomTop = JPanel(BorderLayout())
		subpanelRoomTop.minimumSize = Dimension(0, 0)
		splitRoom.topComponent = subpanelRoomTop

		// *** Top panel room screen
		subPanelRoomTopBar.layout = roomTopBarCardLayout
		subpanelRoomTop.add(subPanelRoomTopBar, BorderLayout.NORTH)

		// ****  buttonPanel type
		val subpanelRoomButtons = JPanel()
		subPanelRoomTopBar.add(subpanelRoomButtons, "Buttons")

		// ***** Withdrawal button
		subpanelRoomButtons.add(JButton(getUIText("Room_Leave")).also {
			it.addActionListener(this)
			it.actionCommand = "Room_Leave"
			it.setMnemonic('L')
			it.toolTipText = getUIText("Room_Leave_Tip")
		})

		// ***** Participation in a game button
		subpanelRoomButtons.add(btnRoomButtonsJoin.also {
			it.text = getUIText("Room_Join")
			it.addActionListener(this)
			it.actionCommand = "Room_Join"
			it.setMnemonic('J')
			it.toolTipText = getUIText("Room_Join_Tip")
			it.isVisible = false
		})

		// ***** Withdrawal button
		subpanelRoomButtons.add(btnRoomButtonsSitOut.also {
			it.text = getUIText("Room_SitOut")
			it.addActionListener(this)
			it.actionCommand = "Room_SitOut"
			it.setMnemonic('W')
			it.toolTipText = getUIText("Room_SitOut_Tip")
			it.isVisible = false
		})

		// ***** Change team button
		subpanelRoomButtons.add(btnRoomButtonsTeamChange.also {
			it.text = getUIText("Room_TeamChange")
			it.addActionListener(this)
			it.actionCommand = "Room_TeamChange"
			it.setMnemonic('T')
			it.toolTipText = getUIText("Room_TeamChange_Tip")
		})

		// **** Panel change team
		val subpanelRoomTeam = JPanel(BorderLayout())
		subPanelRoomTopBar.add(subpanelRoomTeam, "Team")

		// ***** Team name input Column

		subpanelRoomTeam.add(txtfldRoomTeam, BorderLayout.CENTER)

		// ***** Team nameChange buttonPanel
		val subpanelRoomTeamButtons = JPanel()
		subpanelRoomTeam.add(subpanelRoomTeamButtons, BorderLayout.EAST)

		// ****** Team nameChangeOK
		subpanelRoomTeamButtons.add(JButton(getUIText("Room_TeamChange_OK")).also {
			it.addActionListener(this)
			it.actionCommand = "Room_TeamChange_OK"
			it.setMnemonic('O')
		})

		// ****** Team nameChangeCancel
		subpanelRoomTeamButtons.add(JButton(getUIText("Room_TeamChange_Cancel")).also {
			it.addActionListener(this)
			it.actionCommand = "Room_TeamChange_Cancel"
			it.setMnemonic('C')
		})

		// ***** Setting confirmation button
		subpanelRoomButtons.add(btnRoomButtonsViewSetting.also {
			it.text = getUIText("Room_ViewSetting")
			it.addActionListener(this)
			it.actionCommand = "Room_ViewSetting"
			it.setMnemonic('V')
			it.toolTipText = getUIText("Room_ViewSetting_Tip")
		})

		// ***** Leaderboard button
		subpanelRoomButtons.add(btnRoomButtonsRanking.also {
			it.text = getUIText("Room_Ranking")
			it.addActionListener(this)
			it.actionCommand = "Room_Ranking"
			it.setMnemonic('K')
			it.toolTipText = getUIText("Room_Ranking_Tip")
			it.isVisible = false
		})

		// *** Game stats area
		subPanelGameStat.layout = gameStatCardLayout
		subpanelRoomTop.add(subPanelGameStat, BorderLayout.CENTER)

		// **** Multiplayer game stats table
		tableModelGameStat.setColumnIdentifiers(Vector(STATTABLE_COLUMNNAMES.map {getUIText(it)}))
		tableGameStat.apply {
			model = tableModelGameStat
			setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
			setDefaultEditor(Any::class.java, null)
			autoResizeMode = JTable.AUTO_RESIZE_OFF
			tableHeader.reorderingAllowed = false
			componentPopupMenu = TablePopupMenu(tableGameStat)
		}

		tableGameStat.columnModel.run {
			getColumn(0).preferredWidth = propConfig.getProperty("tableGameStat.width.rank", 30) // Rank
			getColumn(1).preferredWidth = propConfig.getProperty("tableGameStat.width.name", 100) // Name
			getColumn(2).preferredWidth = propConfig.getProperty("tableGameStat.width.attack", 55) // Attack count
			getColumn(3).preferredWidth = propConfig.getProperty("tableGameStat.width.apl", 55) // APL
			getColumn(4).preferredWidth = propConfig.getProperty("tableGameStat.width.apm", 55) // APM
			getColumn(5).preferredWidth = propConfig.getProperty("tableGameStat.width.lines", 55) // Line count
			getColumn(6).preferredWidth = propConfig.getProperty("tableGameStat.width.lpm", 55) // LPM
			getColumn(7).preferredWidth = propConfig.getProperty("tableGameStat.width.piece", 55) // Piece count
			getColumn(8).preferredWidth = propConfig.getProperty("tableGameStat.width.pps", 55) // PPS
			getColumn(9).preferredWidth = propConfig.getProperty("tableGameStat.width.time", 65) // Time
			getColumn(10).preferredWidth = propConfig.getProperty("tableGameStat.width.ko", 40) // KO
			getColumn(11).preferredWidth = propConfig.getProperty("tableGameStat.width.wins", 55) // Win
			getColumn(12).preferredWidth = propConfig.getProperty("tableGameStat.width.games", 55) // Games
		}

		subPanelGameStat.add(JScrollPane(tableGameStat).apply {
			minimumSize = Dimension(0, 0)
		}, "GameStatMP")

		// **** Single player game stats table
		tableModelGameStat1P.setColumnIdentifiers(Vector(STATTABLE1P_COLUMNNAMES.map {getUIText(it)}))
		tableGameStat1P.model = tableModelGameStat1P
		tableGameStat1P.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
		tableGameStat1P.setDefaultEditor(Any::class.java, null)
		tableGameStat1P.autoResizeMode = JTable.AUTO_RESIZE_OFF
		tableGameStat1P.tableHeader.reorderingAllowed = false
		tableGameStat1P.componentPopupMenu = TablePopupMenu(tableGameStat1P)

		tableGameStat1P.columnModel.run {
			getColumn(0).preferredWidth = propConfig.getProperty("tableGameStat1P.width.description", 100) // Description
			getColumn(1).preferredWidth = propConfig.getProperty("tableGameStat1P.width.value", 100) // Value
		}

		subPanelGameStat.add(JScrollPane(tableGameStat1P).apply {
			minimumSize = Dimension(0, 0)
		}, "GameStat1P")

		// ** Chat panel(Under)
		val subpanelRoomChat = JPanel(BorderLayout())
		subpanelRoomChat.minimumSize = Dimension(0, 0)
		splitRoom.bottomComponent = subpanelRoomChat

		// *** Chat logAndPlayerPartition line of the list(Room screen)
		subpanelRoomChat.add(splitRoomChat.apply {
			dividerLocation = propConfig.getProperty("room.splitRoomChat.location", 350)
		}, BorderLayout.CENTER)

		// **** Chat log(Room screen)
		txtPaneRoomChatLog.componentPopupMenu = LogPopupMenu(txtPaneRoomChatLog)
		txtPaneRoomChatLog.addKeyListener(LogKeyAdapter())
		splitRoomChat.leftComponent = JScrollPane(txtPaneRoomChatLog).apply {
			minimumSize = Dimension(0, 0)
		}

		// **** PlayerList(Room screen)
		sameRoomPlayerInfoList.clear()
		listModelRoomChatPlayerList.clear()
		listboxRoomChatPlayerList.model = listModelRoomChatPlayerList
		listboxRoomChatPlayerList.componentPopupMenu = ListBoxPopupMenu(listboxRoomChatPlayerList)
		splitRoomChat.rightComponent = JScrollPane(listboxRoomChatPlayerList).apply {
			minimumSize = Dimension(0, 0)
		}

		// *** Chat input Column panel(Room screen)
		val subpanelRoomChatInputArea = JPanel(BorderLayout())
		subpanelRoomChat.add(subpanelRoomChatInputArea, BorderLayout.SOUTH)

		// **** Chat input Column(Room screen)
		txtfldRoomChatInput.componentPopupMenu = TextComponentPopupMenu(txtfldRoomChatInput)
		subpanelRoomChatInputArea.add(txtfldRoomChatInput, BorderLayout.CENTER)

		// **** Submit chat button(Room screen)
		btnRoomChatSend.text = getUIText("Room_ChatSend")
		btnRoomChatSend.addActionListener(this)
		btnRoomChatSend.actionCommand = "Room_ChatSend"
		btnRoomChatSend.setMnemonic('S')
		subpanelRoomChatInputArea.add(btnRoomChatSend, BorderLayout.EAST)
	}

	/** Server-add screen initialization */
	private fun initServerAddUI() {
		// Add Server screen
		val mainpanelServerAdd = JPanel(BorderLayout())
		contentPane.add(mainpanelServerAdd, SCREENCARD_NAMES[SCREENCARD_SERVERADD])

		// * Add Server screen panel(Another panel because it would have been stretched vertically and simply added1I use sheet)
		val containerpanelServerAdd = JPanel()
		containerpanelServerAdd.layout = BoxLayout(containerpanelServerAdd, BoxLayout.Y_AXIS)
		mainpanelServerAdd.add(containerpanelServerAdd, BorderLayout.NORTH)

		// ** Panel host name
		val subpanelHost = JPanel(BorderLayout())
		containerpanelServerAdd.add(subpanelHost)

		// *** Name or &quot;hostIPAddress:&quot;Label
		subpanelHost.add(JLabel(getUIText("ServerAdd_Host")), BorderLayout.WEST)

		// *** Host name input Column
		txtfldServerAddHost.componentPopupMenu = TextComponentPopupMenu(txtfldServerAddHost)
		subpanelHost.add(txtfldServerAddHost, BorderLayout.CENTER)

		// **  buttonPanel type
		val subpanelButtons = JPanel()
		subpanelButtons.layout = BoxLayout(subpanelButtons, BoxLayout.X_AXIS)
		containerpanelServerAdd.add(subpanelButtons)

		// *** OK button
		btnServerAddOK.text = getUIText("ServerAdd_OK")
		btnServerAddOK.addActionListener(this)
		btnServerAddOK.actionCommand = "ServerAdd_OK"
		btnServerAddOK.setMnemonic('O')
		btnServerAddOK.maximumSize = Dimension(Short.MAX_VALUE.toInt(), btnServerAddOK.maximumSize.height)
		subpanelButtons.add(btnServerAddOK)

		// *** Cancel button
		subpanelButtons.add(JButton(getUIText("ServerAdd_Cancel")).also {
			it.addActionListener(this)
			it.actionCommand = "ServerAdd_Cancel"
			it.setMnemonic('C')
			it.maximumSize = Dimension(Short.MAX_VALUE.toInt(), it.maximumSize.height)
		})
	}

	/** Create rated screen card while waiting for presets to arrive from
	 * server */
	private fun initCreateRatedWaitingUI() {
		// Main panel
		val mainpanelCreateRatedWaiting = JPanel(BorderLayout())
		contentPane.add(mainpanelCreateRatedWaiting, SCREENCARD_NAMES[SCREENCARD_CREATERATED_WAITING])

		// * Container panel
		val containerpanelCreateRatedWaiting = JPanel()
		containerpanelCreateRatedWaiting.layout = BoxLayout(containerpanelCreateRatedWaiting, BoxLayout.Y_AXIS)
		mainpanelCreateRatedWaiting.add(containerpanelCreateRatedWaiting, BorderLayout.NORTH)

		// ** Subpanel for label
		val subpanelText = JPanel(BorderLayout())
		containerpanelCreateRatedWaiting.add(subpanelText, BorderLayout.CENTER)

		// *** "Please wait while preset information is retrieved from the server" label
		subpanelText.add(JLabel(getUIText("CreateRated_Waiting_Text")), BorderLayout.CENTER)

		// ** Subpanel for cancel button
		val subpanelButtons = JPanel()
		mainpanelCreateRatedWaiting.add(subpanelButtons, BorderLayout.SOUTH)

		// *** Cancel Button
		btnCreateRatedWaitingCancel.text = getUIText("CreateRated_Waiting_Cancel")
		btnCreateRatedWaitingCancel.addActionListener(this)
		btnCreateRatedWaitingCancel.actionCommand = "CreateRated_Waiting_Cancel"
		btnCreateRatedWaitingCancel.setMnemonic('C')
		btnCreateRatedWaitingCancel.maximumSize =
			Dimension(Short.MAX_VALUE.toInt(), btnCreateRatedWaitingCancel.maximumSize.height)
		subpanelButtons.add(btnCreateRatedWaitingCancel, BorderLayout.SOUTH)
	}

	private fun initCreateRatedUI() {
		// Main panel
		val mainpanelCreateRated = JPanel(BorderLayout())
		contentPane.add(mainpanelCreateRated, SCREENCARD_NAMES[SCREENCARD_CREATERATED_WAITING])

		// * Container panel
		val containerpanelCreateRated = JPanel()
		containerpanelCreateRated.layout = BoxLayout(containerpanelCreateRated, BoxLayout.Y_AXIS)
		mainpanelCreateRated.add(containerpanelCreateRated, BorderLayout.NORTH)

		// ** Subpanel for preset selection
		val subpanelName = JPanel(BorderLayout())
		containerpanelCreateRated.add(subpanelName)

		// *** "Room Name:" label
		subpanelName.add(JLabel(getUIText("CreateRated_Name")), BorderLayout.WEST)

		// *** Room name textfield
		txtfldCreateRatedName.componentPopupMenu = TextComponentPopupMenu(txtfldCreateRatedName)
		txtfldCreateRatedName.toolTipText = getUIText("CreateRated_Name_Tip")
		subpanelName.add(txtfldCreateRatedName, BorderLayout.CENTER)

		// ** Subpanel for preset selection
		val subpanelPresetSelect = JPanel(BorderLayout())
		containerpanelCreateRated.add(subpanelPresetSelect)

		// *** "Preset:" label
		subpanelPresetSelect.add(JLabel(getUIText("CreateRated_Preset")), BorderLayout.WEST)

		// *** Presets
		comboboxCreateRatedPresets.model = DefaultComboBoxModel(arrayOf("Select..."))
		comboboxCreateRatedPresets.selectedIndex = propConfig.getProperty("createrated.defaultPreset", 0)
		comboboxCreateRatedPresets.preferredSize = Dimension(200, 20)
		comboboxCreateRatedPresets.toolTipText = getUIText("CreateRated_Preset_Tip")
		subpanelPresetSelect.add(comboboxCreateRatedPresets, BorderLayout.EAST)

		// ** Number of players panel
		val subpanelMaxPlayers = JPanel(BorderLayout())
		containerpanelCreateRated.add(subpanelMaxPlayers)

		// *** Number of players label
		subpanelMaxPlayers.add(JLabel(getUIText("CreateRated_MaxPlayers")), BorderLayout.WEST)

		// *** Number of players textfield
		val defaultMaxPlayers = propConfig.getProperty("createrated.defaultMaxPlayers", 6)
		spinnerCreateRatedMaxPlayers.model = (SpinnerNumberModel(defaultMaxPlayers, 2, 6, 1))
		spinnerCreateRatedMaxPlayers.preferredSize = Dimension(200, 20)
		spinnerCreateRatedMaxPlayers.toolTipText = getUIText("CreateRated_MaxPlayers_Tip")
		subpanelMaxPlayers.add(spinnerCreateRatedMaxPlayers, BorderLayout.EAST)

		// ** Subpanel for buttons
		val subpanelButtons = JPanel()
		mainpanelCreateRated.add(subpanelButtons, BorderLayout.SOUTH)

		// *** OK button
		btnCreateRatedOK.text = getUIText("CreateRated_OK")
		btnCreateRatedOK.addActionListener(this)
		btnCreateRatedOK.actionCommand = "CreateRated_OK"
		btnCreateRatedOK.setMnemonic('O')
		btnCreateRatedOK.maximumSize = Dimension(Short.MAX_VALUE.toInt(), btnCreateRatedOK.maximumSize.height)
		subpanelButtons.add(btnCreateRatedOK)

		// *** Custom button
		btnCreateRatedCustom.text = getUIText("CreateRated_Custom")
		btnCreateRatedCustom.addActionListener(this)
		btnCreateRatedCustom.actionCommand = "CreateRated_Custom"
		btnCreateRatedCustom.setMnemonic('U')
		btnCreateRatedCustom.maximumSize =
			Dimension(Short.MAX_VALUE.toInt(), btnCreateRatedCustom.maximumSize.height)
		subpanelButtons.add(btnCreateRatedCustom)

		// *** Cancel Button
		btnCreateRatedCancel.text = getUIText("CreateRated_Cancel")
		btnCreateRatedCancel.addActionListener(this)
		btnCreateRatedCancel.actionCommand = "CreateRated_Cancel"
		btnCreateRatedCancel.setMnemonic('C')
		btnCreateRatedCancel.maximumSize =
			Dimension(Short.MAX_VALUE.toInt(), btnCreateRatedCancel.maximumSize.height)
		subpanelButtons.add(btnCreateRatedCancel)
	}

	/** Create room scren initialization */
	private fun initCreateRoomUI() {
		// Create Screen Room
		val mainpanelCreateRoom = JPanel(BorderLayout())
		contentPane.add(mainpanelCreateRoom, SCREENCARD_NAMES[SCREENCARD_CREATEROOM])

		// Tab
		val tabbedPane = JTabbedPane()
		mainpanelCreateRoom.add(tabbedPane, BorderLayout.CENTER)

		// tabs

		// * Basic Settings panel
		val containerpanelCreateRoomMainOwner = JPanel(BorderLayout())
		tabbedPane.addTab(getUIText("CreateRoom_Tab_Main"), containerpanelCreateRoomMainOwner)

		// * Speed setting panel(Stretching for prevention)
		val containerpanelCreateRoomSpeedOwner = JPanel(BorderLayout())
		tabbedPane.addTab(getUIText("CreateRoom_Tab_Speed"), containerpanelCreateRoomSpeedOwner)

		// * Bonus tab
		val containerpanelCreateRoomBonusOwner = JPanel(BorderLayout())
		tabbedPane.addTab(getUIText("CreateRoom_Tab_Bonus"), containerpanelCreateRoomBonusOwner)

		// * Garbage tab
		val containerpanelCreateRoomGarbageOwner = JPanel(BorderLayout())
		tabbedPane.addTab(getUIText("CreateRoom_Tab_Garbage"), containerpanelCreateRoomGarbageOwner)

		// * Miscellaneous tab
		val containerpanelCreateRoomMiscOwner = JPanel(BorderLayout())
		tabbedPane.addTab(getUIText("CreateRoom_Tab_Misc"), containerpanelCreateRoomMiscOwner)

		// * Preset tab
		val containerpanelCreateRoomPresetOwner = JPanel(BorderLayout())
		tabbedPane.addTab(getUIText("CreateRoom_Tab_Preset"), containerpanelCreateRoomPresetOwner)

		// general tab

		// * Speed setting panel(Body)
		val containerpanelCreateRoomMain = JPanel()
		containerpanelCreateRoomMain.layout = BoxLayout(containerpanelCreateRoomMain, BoxLayout.Y_AXIS)
		containerpanelCreateRoomMainOwner.add(containerpanelCreateRoomMain, BorderLayout.NORTH)

		// ** Panel Room name
		val subpanelName = JPanel(BorderLayout())
		containerpanelCreateRoomMain.add(subpanelName)

		// *** Name &quot;Room:&quot;Label
		subpanelName.add(JLabel(getUIText("CreateRoom_Name")), BorderLayout.WEST)

		// *** Room name input Column
		txtfldCreateRoomName.componentPopupMenu = TextComponentPopupMenu(txtfldCreateRoomName)
		txtfldCreateRoomName.toolTipText = getUIText("CreateRoom_Name_Tip")
		subpanelName.add(txtfldCreateRoomName, BorderLayout.CENTER)

		// ** Game Mode panel
		val subpanelMode = JPanel(BorderLayout())
		containerpanelCreateRoomMain.add(subpanelMode)

		// *** Mode label
		subpanelMode.add(JLabel(getUIText("CreateRoom_Mode")), BorderLayout.WEST)

		// *** Mode Combobox
		val modelMode = DefaultComboBoxModel<String>()
		loadModeList(modelMode, "config/list/netlobby_multimode.lst")
		comboboxCreateRoomMode.apply {
			model = modelMode
			preferredSize = Dimension(200, 20)
			toolTipText = getUIText("CreateRoom_Mode_Tip")
		}
		subpanelMode.add(comboboxCreateRoomMode, BorderLayout.EAST)

		// ** People participatecountPanel
		val subpanelMaxPlayers = JPanel(BorderLayout())
		containerpanelCreateRoomMain.add(subpanelMaxPlayers)

		// *** Human participation &quot;count:&quot;Label
		subpanelMaxPlayers.add(JLabel(getUIText("CreateRoom_MaxPlayers")), BorderLayout.WEST)

		// *** People participatecountSelection
		val defaultMaxPlayers = propConfig.getProperty("createroom.defaultMaxPlayers", 6)
		spinnerCreateRoomMaxPlayers.model = SpinnerNumberModel(defaultMaxPlayers, 2, 6, 1)
		spinnerCreateRoomMaxPlayers.preferredSize = Dimension(200, 20)
		spinnerCreateRoomMaxPlayers.toolTipText = getUIText("CreateRoom_MaxPlayers_Tip")
		subpanelMaxPlayers.add(spinnerCreateRoomMaxPlayers, BorderLayout.EAST)

		// ** HurryupSecondcountPanel
		val subpanelHurryupSeconds = JPanel(BorderLayout())
		containerpanelCreateRoomMain.add(subpanelHurryupSeconds)

		// *** &#39;HURRY UPSeconds before the startcount:&quot;Label
		subpanelHurryupSeconds.add(JLabel(getUIText("CreateRoom_HurryupSeconds")), BorderLayout.WEST)

		// *** HurryupSecondcount
		val defaultHurryupSeconds = propConfig.getProperty("createroom.defaultHurryupSeconds", 180)
		spinnerCreateRoomHurryupSeconds.model = (SpinnerNumberModel(defaultHurryupSeconds, -1, 999, 1))
		spinnerCreateRoomHurryupSeconds.preferredSize = Dimension(200, 20)
		spinnerCreateRoomHurryupSeconds.toolTipText = getUIText("CreateRoom_HurryupSeconds_Tip")
		subpanelHurryupSeconds.add(spinnerCreateRoomHurryupSeconds, BorderLayout.EAST)

		// ** HurryupPanel spacing
		val subpanelHurryupInterval = JPanel(BorderLayout())
		containerpanelCreateRoomMain.add(subpanelHurryupInterval)

		// *** &#39;HURRY UPLater, Interval overcall the floor:&quot;Label
		subpanelHurryupInterval.add(JLabel(getUIText("CreateRoom_HurryupInterval")), BorderLayout.WEST)

		// *** HurryupInterval
		val defaultHurryupInterval = propConfig.getProperty("createroom.defaultHurryupInterval", 5)
		spinnerCreateRoomHurryupInterval.model = (SpinnerNumberModel(defaultHurryupInterval, 1, 99, 1))
		spinnerCreateRoomHurryupInterval.preferredSize = Dimension(200, 20)
		spinnerCreateRoomHurryupInterval.toolTipText = getUIText("CreateRoom_HurryupInterval_Tip")
		subpanelHurryupInterval.add(spinnerCreateRoomHurryupInterval, BorderLayout.EAST)

		// ** MapSetIDPanel
		val subpanelMapSetID = JPanel(BorderLayout())
		containerpanelCreateRoomMain.add(subpanelMapSetID)

		// *** &#39;MapSetID:&quot;Label
		subpanelMapSetID.add(JLabel(getUIText("CreateRoom_MapSetID")), BorderLayout.WEST)

		// *** MapSetID
		val defaultMapSetID = propConfig.getProperty("createroom.defaultMapSetID", 0)
		spinnerCreateRoomMapSetID.model = (SpinnerNumberModel(defaultMapSetID, 0, 99, 1))
		spinnerCreateRoomMapSetID.preferredSize = Dimension(200, 20)
		spinnerCreateRoomMapSetID.toolTipText = getUIText("CreateRoom_MapSetID_Tip")
		subpanelMapSetID.add(spinnerCreateRoomMapSetID, BorderLayout.EAST)

		// ** Map is enabled
		chkboxCreateRoomUseMap.text = getUIText("CreateRoom_UseMap")
		chkboxCreateRoomUseMap.setMnemonic('P')
		chkboxCreateRoomUseMap.isSelected = propConfig.getProperty("createroom.defaultUseMap", false)
		chkboxCreateRoomUseMap.toolTipText = getUIText("CreateRoom_UseMap_Tip")
		containerpanelCreateRoomMain.add(chkboxCreateRoomUseMap)

		// ** Of all fixed rules
		chkboxCreateRoomRuleLock.text = getUIText("CreateRoom_RuleLock")
		chkboxCreateRoomRuleLock.setMnemonic('L')
		chkboxCreateRoomRuleLock.isSelected = propConfig.getProperty("createroom.defaultRuleLock", false)
		chkboxCreateRoomRuleLock.toolTipText = getUIText("CreateRoom_RuleLock_Tip")
		containerpanelCreateRoomMain.add(chkboxCreateRoomRuleLock)

		// speed tab

		// * Speed setting panel(Body)
		val containerpanelCreateRoomSpeed = JPanel()
		containerpanelCreateRoomSpeed.layout = BoxLayout(containerpanelCreateRoomSpeed, BoxLayout.Y_AXIS)
		containerpanelCreateRoomSpeedOwner.add(containerpanelCreateRoomSpeed, BorderLayout.NORTH)

		// ** Fall velocity(Molecule)Panel
		val subpanelGravity = JPanel(BorderLayout())
		containerpanelCreateRoomSpeed.add(subpanelGravity)

		// *** Fall velocity &quot;(Molecule):&quot;Label
		subpanelGravity.add(JLabel(getUIText("CreateRoom_Gravity")), BorderLayout.WEST)

		// *** Fall velocity(Molecule)
		subpanelGravity.add(spinnerCreateRoomGravity.apply {
			model = (SpinnerNumberModel(propConfig.getProperty("createroom.defaultGravity", 1), -1, 99999, 1))
			preferredSize = Dimension(200, 20)
		}, BorderLayout.EAST)

		// ** Fall velocity(Denominator)Panel
		val subpanelDenominator = JPanel(BorderLayout())
		containerpanelCreateRoomSpeed.add(subpanelDenominator)

		// *** Fall velocity &quot;(Denominator):&quot;Label
		subpanelDenominator.add(JLabel(getUIText("CreateRoom_Denominator")), BorderLayout.WEST)

		// *** Fall velocity(Denominator)
		val defaultDenominator = propConfig.getProperty("createroom.defaultDenominator", 60)
		spinnerCreateRoomDenominator.model = (SpinnerNumberModel(defaultDenominator, 0, 99999, 1))
		spinnerCreateRoomDenominator.preferredSize = Dimension(200, 20)
		subpanelDenominator.add(spinnerCreateRoomDenominator, BorderLayout.EAST)

		// ** AREPanel
		val subpanelARE = JPanel(BorderLayout())
		containerpanelCreateRoomSpeed.add(subpanelARE)

		// *** &#39;ARE:&quot;Label
		subpanelARE.add(JLabel(getUIText("CreateRoom_ARE")), BorderLayout.WEST)

		// ***
		spinnerCreateRoomARE.model = (SpinnerNumberModel(
			propConfig.getProperty("createroom.defaultARE", 0), 0, 99, 1
		))
		spinnerCreateRoomARE.preferredSize = Dimension(200, 20)
		subpanelARE.add(spinnerCreateRoomARE, BorderLayout.EAST)

		// ** ARE after line clearPanel
		val subpanelARELine = JPanel(BorderLayout())
		containerpanelCreateRoomSpeed.add(subpanelARELine)

		// *** &#39;ARE after line clear:&quot;Label
		subpanelARELine.add(JLabel(getUIText("CreateRoom_ARELine")), BorderLayout.WEST)

		// *** ARE after line clear
		val defaultARELine = propConfig.getProperty("createroom.defaultARELine", 0)
		spinnerCreateRoomARELine.model = (SpinnerNumberModel(defaultARELine, 0, 99, 1))
		spinnerCreateRoomARELine.preferredSize = Dimension(200, 20)
		subpanelARELine.add(spinnerCreateRoomARELine, BorderLayout.EAST)

		// ** Line clear timePanel
		val subpanelLineDelay = JPanel(BorderLayout())
		containerpanelCreateRoomSpeed.add(subpanelLineDelay)

		// *** &#39;Line clear time:&quot;Label
		subpanelLineDelay.add(JLabel(getUIText("CreateRoom_LineDelay")), BorderLayout.WEST)

		// *** Line clear time
		val defaultLineDelay = propConfig.getProperty("createroom.defaultLineDelay", 0)
		spinnerCreateRoomLineDelay.model = (SpinnerNumberModel(defaultLineDelay, 0, 99, 1))
		spinnerCreateRoomLineDelay.preferredSize = Dimension(200, 20)
		subpanelLineDelay.add(spinnerCreateRoomLineDelay, BorderLayout.EAST)

		// ** Fixation timePanel
		val subpanelLockDelay = JPanel(BorderLayout())
		containerpanelCreateRoomSpeed.add(subpanelLockDelay)

		// *** &quot;Fixed time:&quot;Label
		subpanelLockDelay.add(JLabel(getUIText("CreateRoom_LockDelay")), BorderLayout.WEST)

		// *** Fixation time
		val defaultLockDelay = propConfig.getProperty("createroom.defaultLockDelay", 30)
		spinnerCreateRoomLockDelay.model = (SpinnerNumberModel(defaultLockDelay, 0, 98, 1))
		spinnerCreateRoomLockDelay.preferredSize = Dimension(200, 20)
		subpanelLockDelay.add(spinnerCreateRoomLockDelay, BorderLayout.EAST)

		// ** Panel horizontal reservoir
		val subpanelDAS = JPanel(BorderLayout())
		containerpanelCreateRoomSpeed.add(subpanelDAS)

		// *** Horizontal reservoir &quot;:&quot;Label
		subpanelDAS.add(JLabel(getUIText("CreateRoom_DAS")), BorderLayout.WEST)

		// *** Horizontal reservoir
		val defaultDAS = propConfig.getProperty("createroom.defaultDAS", 11)
		spinnerCreateRoomDAS.model = (SpinnerNumberModel(defaultDAS, 0, 99, 1))
		spinnerCreateRoomDAS.preferredSize = Dimension(200, 20)
		subpanelDAS.add(spinnerCreateRoomDAS, BorderLayout.EAST)

		// bonus tab

		// bonus panel
		val containerpanelCreateRoomBonus = JPanel()
		containerpanelCreateRoomBonus.layout = BoxLayout(containerpanelCreateRoomBonus, BoxLayout.Y_AXIS)
		containerpanelCreateRoomBonusOwner.add(containerpanelCreateRoomBonus, BorderLayout.NORTH)

		// ** Spin bonusPanel
		val subpanelTWISTEnableType = JPanel(BorderLayout())
		containerpanelCreateRoomBonus.add(subpanelTWISTEnableType)

		// *** &quot;Spin bonus:&quot;Label
		subpanelTWISTEnableType.add(JLabel(getUIText("CreateRoom_TwistEnableType")), BorderLayout.WEST)

		// *** Spin bonus
		comboboxCreateRoomTWISTEnableType.model = DefaultComboBoxModel(COMBOBOX_SPINBONUS_NAMES.map {getUIText(it)}.toTypedArray())
		comboboxCreateRoomTWISTEnableType.selectedIndex = propConfig.getProperty("createroom.defaultTwistEnableType", 2)
		comboboxCreateRoomTWISTEnableType.preferredSize = Dimension(200, 20)
		comboboxCreateRoomTWISTEnableType.toolTipText = getUIText("CreateRoom_TwistEnableType_Tip")
		subpanelTWISTEnableType.add(comboboxCreateRoomTWISTEnableType, BorderLayout.EAST)

		// ** EZ Spin checkbox
		chkboxCreateRoomTWISTEnableEZ.text = getUIText("CreateRoom_TwistEnableEZ")
		chkboxCreateRoomTWISTEnableEZ.setMnemonic('E')
		chkboxCreateRoomTWISTEnableEZ.isSelected = propConfig.getProperty("createroom.defaultTwistEnableEZ", false)
		chkboxCreateRoomTWISTEnableEZ.toolTipText = getUIText("CreateRoom_TwistEnableEZ_Tip")
		containerpanelCreateRoomBonus.add(chkboxCreateRoomTWISTEnableEZ)

		// ** Flag for enabling B2B
		chkboxCreateRoomB2B.text = getUIText("CreateRoom_B2B")
		chkboxCreateRoomB2B.setMnemonic('B')
		chkboxCreateRoomB2B.isSelected = propConfig.getProperty("createroom.defaultB2B", true)
		chkboxCreateRoomB2B.toolTipText = getUIText("CreateRoom_B2B_Tip")
		containerpanelCreateRoomBonus.add(chkboxCreateRoomB2B)

		// ** Flag for enabling combos
		chkboxCreateRoomCombo.text = getUIText("CreateRoom_Combo")
		chkboxCreateRoomCombo.setMnemonic('M')
		chkboxCreateRoomCombo.isSelected = propConfig.getProperty("createroom.defaultCombo", true)
		chkboxCreateRoomCombo.toolTipText = getUIText("CreateRoom_Combo_Tip")
		containerpanelCreateRoomBonus.add(chkboxCreateRoomCombo)

		// ** Bravo bonus
		chkboxCreateRoomBravo.text = getUIText("CreateRoom_Bravo")
		chkboxCreateRoomBravo.setMnemonic('A')
		chkboxCreateRoomBravo.isSelected = propConfig.getProperty("createroom.defaultBravo", true)
		chkboxCreateRoomBravo.toolTipText = getUIText("CreateRoom_Bravo_Tip")
		containerpanelCreateRoomBonus.add(chkboxCreateRoomBravo)

		// garbage tab

		// garbage panel
		val containerpanelCreateRoomGarbage = JPanel()
		containerpanelCreateRoomGarbage.layout = BoxLayout(containerpanelCreateRoomGarbage, BoxLayout.Y_AXIS)
		containerpanelCreateRoomGarbageOwner.add(containerpanelCreateRoomGarbage, BorderLayout.NORTH)

		// ** Garbage change rate panel
		val subpanelGarbagePercent = JPanel(BorderLayout())
		containerpanelCreateRoomGarbage.add(subpanelGarbagePercent)

		// ** Label for garbage change rate
		subpanelGarbagePercent.add(JLabel(getUIText("CreateRoom_GarbagePercent")), BorderLayout.WEST)

		// ** Spinner for garbage change rate
		val defaultGarbagePercent = propConfig.getProperty("createroom.defaultGarbagePercent", 90)
		spinnerCreateRoomGarbagePercent.model = (SpinnerNumberModel(defaultGarbagePercent, 0, 100, 10))
		spinnerCreateRoomGarbagePercent.preferredSize = Dimension(200, 20)
		spinnerCreateRoomGarbagePercent.toolTipText = getUIText("CreateRoom_GarbagePercent_Tip")
		subpanelGarbagePercent.add(spinnerCreateRoomGarbagePercent, BorderLayout.EAST)

		// ** Target timer panel
		val subpanelTargetTimer = JPanel(BorderLayout())
		containerpanelCreateRoomGarbage.add(subpanelTargetTimer)

		// ** Label for target timer
		subpanelTargetTimer.add(JLabel(getUIText("CreateRoom_TargetTimer")), BorderLayout.WEST)

		// ** Spinner for target timer
		subpanelTargetTimer.add(spinnerCreateRoomTargetTimer.apply {
			model = (SpinnerNumberModel(propConfig.getProperty("createroom.defaultTargetTimer", 60), 0, 3600, 1))
			preferredSize = Dimension(200, 20)
			toolTipText = getUIText("CreateRoom_TargetTimer_Tip")
		}, BorderLayout.EAST)

		// ** Set garbage type
		chkboxCreateRoomGarbageChangePerAttack.text = getUIText("CreateRoom_GarbageChangePerAttack")
		chkboxCreateRoomGarbageChangePerAttack.setMnemonic('G')
		chkboxCreateRoomGarbageChangePerAttack.isSelected =
			propConfig.getProperty("createroom.defaultGarbageChangePerAttack", true)
		chkboxCreateRoomGarbageChangePerAttack.toolTipText = getUIText("CreateRoom_GarbageChangePerAttack_Tip")
		containerpanelCreateRoomGarbage.add(chkboxCreateRoomGarbageChangePerAttack)

		// ** Divide change rate by live players/teams
		containerpanelCreateRoomGarbage.add(chkboxCreateRoomDivideChangeRateByPlayers.apply {
			text = getUIText("CreateRoom_DivideChangeRateByPlayers")
			isSelected = propConfig.getProperty("createroom.defaultDivideChangeRateByPlayers", false)
			toolTipText = getUIText("CreateRoom_DivideChangeRateByPlayers_Tip")
		})

		// ** B2B chunk
		containerpanelCreateRoomGarbage.add(chkboxCreateRoomB2BChunk.apply {
			text = getUIText("CreateRoom_B2BChunk")
			setMnemonic('B')
			isSelected = propConfig.getProperty("createroom.defaultB2BChunk", false)
			toolTipText = getUIText("CreateRoom_B2BChunk_Tip")
		})

		// ** Rensa/Combo Block
		containerpanelCreateRoomGarbage.add(chkboxCreateRoomRensaBlock.apply {
			text = getUIText("CreateRoom_RensaBlock")
			setMnemonic('E')
			isSelected = propConfig.getProperty("createroom.defaultRensaBlock", true)
			toolTipText = getUIText("CreateRoom_RensaBlock_Tip")
		})

		// ** Garbage countering

		containerpanelCreateRoomGarbage.add(chkboxCreateRoomCounter.apply {
			text = getUIText("CreateRoom_Counter")
			setMnemonic('C')
			isSelected = propConfig.getProperty("createroom.defaultCounter", true)
			toolTipText = getUIText("CreateRoom_Counter_Tip")
		})

		// ** 3If I live more than Attack Reduce the force
		containerpanelCreateRoomGarbage.add(chkboxCreateRoomReduceLineSend.apply {
			text = getUIText("CreateRoom_ReduceLineSend")
			setMnemonic('R')
			isSelected = propConfig.getProperty("createroom.defaultReduceLineSend", true)
			toolTipText = getUIText("CreateRoom_ReduceLineSend_Tip")
		})

		// ** Fragmentarygarbage blockUsing the system
		containerpanelCreateRoomGarbage.add(chkboxCreateRoomUseFractionalGarbage.apply {
			text = getUIText("CreateRoom_UseFractionalGarbage")
			setMnemonic('F')
			isSelected = propConfig.getProperty("createroom.defaultUseFractionalGarbage", false)
			toolTipText = getUIText("CreateRoom_UseFractionalGarbage_Tip")
		})

		// *** Use target system
		chkboxCreateRoomIsTarget.text = getUIText("CreateRoom_IsTarget")
		chkboxCreateRoomIsTarget.setMnemonic('T')
		chkboxCreateRoomIsTarget.isSelected = propConfig.getProperty("createroom.defaultIsTarget", false)
		chkboxCreateRoomIsTarget.toolTipText = getUIText("CreateRoom_IsTarget_Tip")
		containerpanelCreateRoomGarbage.add(chkboxCreateRoomIsTarget)

		// misc tab

		// misc panel
		val containerpanelCreateRoomMisc = JPanel().apply {
			layout = BoxLayout(this, BoxLayout.Y_AXIS)
		}
		containerpanelCreateRoomMiscOwner.add(containerpanelCreateRoomMisc, BorderLayout.NORTH)

		// ** To wait before auto-start timePanel
		val subpanelAutoStartSeconds = JPanel(BorderLayout())
		containerpanelCreateRoomMisc.add(subpanelAutoStartSeconds)

		// *** To wait before auto-start &quot; time:&quot;Label
		subpanelAutoStartSeconds.add(JLabel(getUIText("CreateRoom_AutoStartSeconds")), BorderLayout.WEST)

		// *** To wait before auto-start time
		subpanelAutoStartSeconds.add(spinnerCreateRoomAutoStartSeconds.apply {
			model = (SpinnerNumberModel(
				propConfig.getProperty("createroom.defaultAutoStartSeconds", 15),
				0, 999, 1
			))
			preferredSize = Dimension(200, 20)
			toolTipText = getUIText("CreateRoom_AutoStartSeconds_Tip")
		}, BorderLayout.EAST)

		// ** TNET2TypeAutomatically start timerI use
		containerpanelCreateRoomMisc.add(chkboxCreateRoomAutoStartTNET2.apply {
			text = getUIText("CreateRoom_AutoStartTNET2")
			setMnemonic('A')
			isSelected = propConfig.getProperty("createroom.defaultAutoStartTNET2", false)
			toolTipText = getUIText("CreateRoom_AutoStartTNET2_Tip")
		})

		// ** SomeoneCancelWasTimerInvalidation
		containerpanelCreateRoomMisc.add(chkboxCreateRoomDisableTimerAfterSomeoneCancelled.apply {
			text = getUIText("CreateRoom_DisableTimerAfterSomeoneCancelled")
			setMnemonic('D')
			isSelected = propConfig.getProperty("createroom.defaultDisableTimerAfterSomeoneCancelled", false)
			toolTipText = getUIText("CreateRoom_DisableTimerAfterSomeoneCancelled_Tip")
		})

		// Preset tab

		// * Preset panel
		val containerpanelCreateRoomPreset = JPanel()
		containerpanelCreateRoomPreset.layout = BoxLayout(containerpanelCreateRoomPreset, BoxLayout.Y_AXIS)
		containerpanelCreateRoomPresetOwner.add(containerpanelCreateRoomPreset, BorderLayout.NORTH)

		// ** Preset number panel
		val subpanelPresetID = JPanel(BorderLayout())
		subpanelPresetID.alignmentX = 0f
		containerpanelCreateRoomPreset.add(subpanelPresetID)

		// *** "Preset number:" Label
		subpanelPresetID.add(JLabel(getUIText("CreateRoom_PresetID")), BorderLayout.WEST)

		// *** Preset number selector
		val defaultPresetID = propConfig.getProperty("createroom.defaultPresetID", 0)
		spinnerCreateRoomPresetID.model = (SpinnerNumberModel(defaultPresetID, 0, 999, 1))
		spinnerCreateRoomPresetID.preferredSize = Dimension(200, 20)
		subpanelPresetID.add(spinnerCreateRoomPresetID, BorderLayout.EAST)

		// ** Save button
		val btnPresetSave = JButton(getUIText("CreateRoom_PresetSave"))
		btnPresetSave.alignmentX = 0f
		btnPresetSave.addActionListener(this)
		btnPresetSave.actionCommand = "CreateRoom_PresetSave"
		btnPresetSave.setMnemonic('S')
		btnPresetSave.maximumSize = Dimension(Short.MAX_VALUE.toInt(), btnPresetSave.maximumSize.height)
		containerpanelCreateRoomPreset.add(btnPresetSave)

		// ** Load button
		val btnPresetLoad = JButton(getUIText("CreateRoom_PresetLoad"))
		btnPresetLoad.alignmentX = 0f
		btnPresetLoad.addActionListener(this)
		btnPresetLoad.actionCommand = "CreateRoom_PresetLoad"
		btnPresetLoad.setMnemonic('L')
		btnPresetLoad.maximumSize = Dimension(Short.MAX_VALUE.toInt(), btnPresetLoad.maximumSize.height)
		containerpanelCreateRoomPreset.add(btnPresetLoad)

		// ** Preset code panel
		val subpanelPresetCode = JPanel(BorderLayout())
		subpanelPresetCode.alignmentX = 0f
		containerpanelCreateRoomPreset.add(subpanelPresetCode)

		// *** "Preset code:" Label
		subpanelPresetCode.add(JLabel(getUIText("CreateRoom_PresetCode")), BorderLayout.WEST)

		// *** Preset code textfield
		txtfldCreateRoomPresetCode.componentPopupMenu = TextComponentPopupMenu(txtfldCreateRoomPresetCode)
		subpanelPresetCode.add(txtfldCreateRoomPresetCode, BorderLayout.CENTER)

		// *** Preset code export
		val btnPresetCodeExport = JButton(getUIText("CreateRoom_PresetCodeExport"))
		btnPresetCodeExport.alignmentX = 0f
		btnPresetCodeExport.addActionListener(this)
		btnPresetCodeExport.actionCommand = "CreateRoom_PresetCodeExport"
		btnPresetCodeExport.setMnemonic('E')
		btnPresetCodeExport.maximumSize = Dimension(Short.MAX_VALUE.toInt(), btnPresetCodeExport.maximumSize.height)
		containerpanelCreateRoomPreset.add(btnPresetCodeExport)

		// *** Preset code import
		val btnPresetCodeImport = JButton(getUIText("CreateRoom_PresetCodeImport"))
		btnPresetCodeImport.alignmentX = 0f
		btnPresetCodeImport.addActionListener(this)
		btnPresetCodeImport.actionCommand = "CreateRoom_PresetCodeImport"
		btnPresetCodeImport.setMnemonic('I')
		btnPresetCodeImport.maximumSize = Dimension(Short.MAX_VALUE.toInt(), btnPresetCodeImport.maximumSize.height)
		containerpanelCreateRoomPreset.add(btnPresetCodeImport)

		// buttons

		// **  buttonPanel type
		val subpanelButtons = JPanel()
		subpanelButtons.layout = BoxLayout(subpanelButtons, BoxLayout.X_AXIS)
		//containerpanelCreateRoom.add(subpanelButtons);
		mainpanelCreateRoom.add(subpanelButtons, BorderLayout.SOUTH)

		// *** OK button
		btnCreateRoomOK.text = getUIText("CreateRoom_OK")
		btnCreateRoomOK.addActionListener(this)
		btnCreateRoomOK.actionCommand = "CreateRoom_OK"
		btnCreateRoomOK.setMnemonic('O')
		btnCreateRoomOK.maximumSize = Dimension(Short.MAX_VALUE.toInt(), btnCreateRoomOK.maximumSize.height)
		subpanelButtons.add(btnCreateRoomOK)

		// *** Participation in a war button
		btnCreateRoomJoin.text = getUIText("CreateRoom_Join")
		btnCreateRoomJoin.addActionListener(this)
		btnCreateRoomJoin.actionCommand = "CreateRoom_Join"
		btnCreateRoomJoin.setMnemonic('J')
		btnCreateRoomJoin.maximumSize = Dimension(Short.MAX_VALUE.toInt(), btnCreateRoomJoin.maximumSize.height)
		subpanelButtons.add(btnCreateRoomJoin)

		// *** Participation in a war button
		btnCreateRoomWatch.text = getUIText("CreateRoom_Watch")
		btnCreateRoomWatch.addActionListener(this)
		btnCreateRoomWatch.actionCommand = "CreateRoom_Watch"
		btnCreateRoomWatch.setMnemonic('W')
		btnCreateRoomWatch.maximumSize = Dimension(Short.MAX_VALUE.toInt(), btnCreateRoomWatch.maximumSize.height)
		subpanelButtons.add(btnCreateRoomWatch)

		// *** Cancel Button
		btnCreateRoomCancel.text = getUIText("CreateRoom_Cancel")
		btnCreateRoomCancel.addActionListener(this)
		btnCreateRoomCancel.actionCommand = "CreateRoom_Cancel"
		btnCreateRoomCancel.setMnemonic('C')
		btnCreateRoomCancel.maximumSize = Dimension(Short.MAX_VALUE.toInt(), btnCreateRoomCancel.maximumSize.height)
		subpanelButtons.add(btnCreateRoomCancel)
	}

	/** Create room (1P) screen initialization */
	private fun initCreateRoom1PUI() {
		// Main panel for Create room 1P
		val mainpanelCreateRoom1P = JPanel()
		mainpanelCreateRoom1P.layout = BoxLayout(mainpanelCreateRoom1P, BoxLayout.Y_AXIS)
		contentPane.add(mainpanelCreateRoom1P, SCREENCARD_NAMES[SCREENCARD_CREATEROOM1P])

		// * Game mode panel
		val pModeList = JPanel(BorderLayout())
		mainpanelCreateRoom1P.add(pModeList)

		labelCreateRoom1PGameMode.text = getUIText("CreateRoom1P_Mode_Label")
		pModeList.add(labelCreateRoom1PGameMode, BorderLayout.NORTH)

		// ** Game mode listbox
		listmodelCreateRoom1PModeList.clear()
		loadModeList(listmodelCreateRoom1PModeList, "config/list/netlobby_singlemode.lst")


		pModeList.add(JScrollPane(listboxCreateRoom1PModeList.apply {
			model = (listmodelCreateRoom1PModeList)
			addListSelectionListener {labelCreateRoom1PGameMode.text = getModeDesc("$it")}
			setSelectedValue(propConfig.getProperty("createroom1p.listboxCreateRoom1PModeList.value", ""), true)
		}), BorderLayout.CENTER)

		// * Rule list panel
		val pRuleList = JPanel(BorderLayout())
		mainpanelCreateRoom1P.add(pRuleList)

		// ** "Rule:" label
		pRuleList.add(JLabel(getUIText("CreateRoom1P_Rule_Label")), BorderLayout.NORTH)

		// ** Rule list listbox
		listmodelCreateRoom1PRuleList.clear()
		listboxCreateRoom1PRuleList.model = (listmodelCreateRoom1PRuleList)
		pRuleList.add(JScrollPane(listboxCreateRoom1PRuleList), BorderLayout.CENTER)

		// * Buttons panel
		val subpanelButtons = JPanel()
		subpanelButtons.layout = BoxLayout(subpanelButtons, BoxLayout.X_AXIS)
		mainpanelCreateRoom1P.add(subpanelButtons)

		// ** OK button
		btnCreateRoom1POK.text = getUIText("CreateRoom1P_OK")
		btnCreateRoom1POK.addActionListener(this)
		btnCreateRoom1POK.actionCommand = "CreateRoom1P_OK"
		btnCreateRoom1POK.setMnemonic('O')
		btnCreateRoom1POK.maximumSize = Dimension(Short.MAX_VALUE.toInt(), btnCreateRoom1POK.maximumSize.height)
		subpanelButtons.add(btnCreateRoom1POK)

		// ** Cancel button
		btnCreateRoom1PCancel.text = getUIText("CreateRoom1P_Cancel")
		btnCreateRoom1PCancel.addActionListener(this)
		btnCreateRoom1PCancel.actionCommand = "CreateRoom1P_Cancel"
		btnCreateRoom1PCancel.setMnemonic('C')
		btnCreateRoom1PCancel.maximumSize =
			Dimension(Short.MAX_VALUE.toInt(), btnCreateRoom1PCancel.maximumSize.height)
		subpanelButtons.add(btnCreateRoom1PCancel)
	}

	/** MPRanking screen initialization */
	private fun initMPRankingUI() {
		// Main panel for MPRanking
		val mainpanelMPRanking = JPanel(BorderLayout())
		contentPane.add(mainpanelMPRanking, SCREENCARD_NAMES[SCREENCARD_MPRANKING])

		// * Tab
		mainpanelMPRanking.add(tabMPRanking, BorderLayout.CENTER)

		// ** Leaderboard Table

		tablemodelMPRanking
		tableMPRanking.forEachIndexed {i, it ->
			it.apply {
				model = tablemodelMPRanking[i]
				setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
				setDefaultEditor(Any::class.java, null)
				autoResizeMode = JTable.AUTO_RESIZE_OFF
				tableHeader.reorderingAllowed = false
				componentPopupMenu = TablePopupMenu(tableMPRanking[i])
			}
		}

		for(i in 0..<GameEngine.MAX_GAMESTYLE) {
			val tm = tableMPRanking[i].columnModel
			tm.getColumn(0).preferredWidth = propConfig.getProperty("tableMPRanking.width.rank", 30) // Rank
			tm.getColumn(1).preferredWidth = propConfig.getProperty("tableMPRanking.width.name", 200) // Name
			tm.getColumn(2).preferredWidth = propConfig.getProperty("tableMPRanking.width.rating", 60) // Rating
			tm.getColumn(3).preferredWidth = propConfig.getProperty("tableMPRanking.width.play", 60) // Play
			tm.getColumn(4).preferredWidth = propConfig.getProperty("tableMPRanking.width.win", 60) // Win

			val spMPRanking = JScrollPane(tableMPRanking[i])
			tabMPRanking.addTab(GameEngine.GAMESTYLE_NAMES[i], spMPRanking)

			if(i!=GameStyle.TETROMINO.ordinal) tabMPRanking.setEnabledAt(i, false) // TODO: Add non-tetromino leaderboard
		}

		// * OK Button

		mainpanelMPRanking.add(btnMPRankingOK.also {
			it.text = getUIText("MPRanking_OK")
			it.addActionListener(this)
			it.actionCommand = "MPRanking_OK"
			it.setMnemonic('O')
		}, BorderLayout.SOUTH)
	}

	/** Rule change screen initialization */
	private fun initRuleChangeUI() {
		// Main panel for RuleChange
		val mainpanelRuleChange = JPanel(BorderLayout())
		contentPane.add(mainpanelRuleChange, SCREENCARD_NAMES[SCREENCARD_RULECHANGE])

		// * Tab
		mainpanelRuleChange.add(tabRuleChange, BorderLayout.CENTER)

		// ** Rule Listboxes
		listboxRuleChangeRuleList = Array(GameEngine.MAX_GAMESTYLE) {JList(extractRuleListFromRuleEntries(it))}
		GameEngine.GAMESTYLE_NAMES.forEachIndexed {i, it ->
			tabRuleChange.addTab(it, JScrollPane(listboxRuleChangeRuleList[i]))
		}

		// ** Tuning Tab
		val subpanelTuning = JPanel()
		subpanelTuning.layout = BoxLayout(subpanelTuning, BoxLayout.Y_AXIS)
		tabRuleChange.addTab(getUIText("RuleChange_Tab_Tuning"), subpanelTuning)

		// *** A button spin
		val pTuningSpinDefaultRight = JPanel()
		//pTuningRotateButtonDefaultRight.setLayout(new BoxLayout(pTuningRotateButtonDefaultRight, BoxLayout.Y_AXIS));
		pTuningSpinDefaultRight.alignmentX = Component.LEFT_ALIGNMENT
		subpanelTuning.add(pTuningSpinDefaultRight)

		pTuningSpinDefaultRight.add(JLabel(getUIText("GameTuning_RotateButtonDefaultRight_Label")))

		comboboxTuningSpinDirection.model = DefaultComboBoxModel(
			TUNING_ABUTTON_SPIN.map {getUIText(it)}.toTypedArray()
		)
		pTuningSpinDefaultRight.add(comboboxTuningSpinDirection)

		// *** Diagonal move
		val pTuningMoveDiagonal = JPanel()
		pTuningMoveDiagonal.alignmentX = Component.LEFT_ALIGNMENT
		subpanelTuning.add(pTuningMoveDiagonal)

		pTuningMoveDiagonal.add(JLabel(getUIText("GameTuning_MoveDiagonal_Label")))
		comboboxTuningMoveDiagonal.model = DefaultComboBoxModel(TUNING_COMBOBOX_GENERIC.map {getUIText(it)}.toTypedArray())
		pTuningMoveDiagonal.add(comboboxTuningMoveDiagonal)

		// *** Show Outline Only
		val pTuningBlockShowOutlineOnly = JPanel()
		pTuningBlockShowOutlineOnly.alignmentX = Component.LEFT_ALIGNMENT
		subpanelTuning.add(pTuningBlockShowOutlineOnly)

		pTuningBlockShowOutlineOnly.add(JLabel(getUIText("GameTuning_BlockShowOutlineOnly_Label")))

		comboboxTuningBlockShowOutlineOnly.model = DefaultComboBoxModel(TUNING_COMBOBOX_GENERIC.map {getUIText(it)}.toTypedArray())
		pTuningBlockShowOutlineOnly.add(comboboxTuningBlockShowOutlineOnly)

		// *** Outline Type
		val pTuningOutlineType = JPanel()
		pTuningOutlineType.alignmentX = Component.LEFT_ALIGNMENT
		subpanelTuning.add(pTuningOutlineType)

		pTuningOutlineType.add(JLabel(getUIText("GameTuning_OutlineType_Label")))

		comboboxTuningBlockOutlineType.model = DefaultComboBoxModel(TUNING_OUTLINE_TYPE_NAMES.map {getUIText(it)}.toTypedArray())
		pTuningOutlineType.add(comboboxTuningBlockOutlineType)

		// *** Skin
		val pTuningSkin = JPanel()
		pTuningSkin.alignmentX = Component.LEFT_ALIGNMENT
		subpanelTuning.add(pTuningSkin)

		pTuningSkin.add(JLabel(getUIText("GameTuning_Skin_Label")))

		val model = DefaultComboBoxModel<ComboLabel>()
		model.addElement(ComboLabel(getUIText("GameTuning_Skin_Random")))
		model.addElement(ComboLabel(getUIText("GameTuning_Skin_Auto")))
		for(i in imgTuningBlockSkins.indices)
			model.addElement(ComboLabel("$i", ImageIcon(imgTuningBlockSkins[i])))

		comboboxTuningSkin.model = model
		comboboxTuningSkin.renderer = ComboLabelCellRenderer()
		comboboxTuningSkin.preferredSize = Dimension(190, 30)
		pTuningSkin.add(comboboxTuningSkin)

		// *** Minimum DAS
		val pTuningMinDAS = JPanel()
		pTuningMinDAS.alignmentX = Component.LEFT_ALIGNMENT
		subpanelTuning.add(pTuningMinDAS)

		pTuningMinDAS.add(JLabel(getUIText("GameTuning_MinDAS_Label")))

		txtfldTuningMinDAS.columns = 5
		pTuningMinDAS.add(txtfldTuningMinDAS)

		// *** Maximum DAS
		val pTuningMaxDAS = JPanel()
		pTuningMaxDAS.alignmentX = Component.LEFT_ALIGNMENT
		subpanelTuning.add(pTuningMaxDAS)

		pTuningMaxDAS.add(JLabel(getUIText("GameTuning_MaxDAS_Label")))

		txtfldTuningMaxDAS.columns = 5
		pTuningMaxDAS.add(txtfldTuningMaxDAS)

		// *** DAS delay
		val pTuningDasDelay = JPanel()
		pTuningDasDelay.alignmentX = Component.LEFT_ALIGNMENT
		subpanelTuning.add(pTuningDasDelay)

		pTuningDasDelay.add(JLabel(getUIText("GameTuning_DasDelay_Label")))

		txtfldTuningDasDelay.columns = 5
		pTuningDasDelay.add(txtfldTuningDasDelay)

		// *** Reverse Up/Down
		val pTuningReverseUpDown = JPanel()
		pTuningReverseUpDown.alignmentX = Component.LEFT_ALIGNMENT
		subpanelTuning.add(pTuningReverseUpDown)

		pTuningReverseUpDown.add(JLabel(getUIText("GameTuning_ReverseUpDown_Label")))
		pTuningReverseUpDown.add(chkboxTuningReverseUpDown)

		// * Buttons panel
		val subpanelButtons = JPanel()
		subpanelButtons.layout = BoxLayout(subpanelButtons, BoxLayout.X_AXIS)
		mainpanelRuleChange.add(subpanelButtons, BorderLayout.SOUTH)

		// ** OK button
		btnRuleChangeOK.text = getUIText("RuleChange_OK")
		btnRuleChangeOK.addActionListener(this)
		btnRuleChangeOK.actionCommand = "RuleChange_OK"
		btnRuleChangeOK.setMnemonic('O')
		btnRuleChangeOK.maximumSize = Dimension(Short.MAX_VALUE.toInt(), btnRuleChangeOK.maximumSize.height)
		subpanelButtons.add(btnRuleChangeOK)

		// ** Cancel button
		btnRuleChangeCancel.text = getUIText("RuleChange_Cancel")
		btnRuleChangeCancel.addActionListener(this)
		btnRuleChangeCancel.actionCommand = "RuleChange_Cancel"
		btnRuleChangeCancel.setMnemonic('C')
		btnRuleChangeCancel.maximumSize = Dimension(Short.MAX_VALUE.toInt(), btnRuleChangeCancel.maximumSize.height)
		subpanelButtons.add(btnRuleChangeCancel)
	}

	/** Load block skins */
	private fun loadBlockSkins() {
		val skinDir = propGlobal.custom.skinDir

		var numSkins = 0
		while(File("$skinDir/graphics/blockskin/normal/n$numSkins.png").canRead()) {
			numSkins++
		}
		log.debug("$numSkins block skins found")

		imgTuningBlockSkins = Array(numSkins) {i ->
			val imgBlock = loadImage(getURL("$skinDir/graphics/blockskin/normal/n$i.png"))
			val isSticky = imgBlock!=null&&imgBlock.width>=400&&imgBlock.height>=304
			return@Array BufferedImage(144, 16, BufferedImage.TYPE_INT_RGB).apply {
				if(isSticky)
					for(j in 0..8)
						graphics.drawImage(imgBlock, j*16, 0, j*16+16, 16, 0, j*16, 16, j*16+16, null)
				else
					graphics.drawImage(imgBlock, 0, 0, 144, 16, 0, 0, 144, 16, null)
			}
		}
	}

	/** Load image file
	 * @param url Image file URL
	 * @return Image data (or null when load fails)
	 */
	private fun loadImage(url:URL?):BufferedImage? {
		var img:BufferedImage? = null
		try {
			img = ImageIO.read(url!!)
			log.debug("Loaded image from {}", url)
		} catch(e:IOException) {
			log.error("Failed to load image from "+url!!, e)
		}

		return img
	}

	/** Get the URL of filename
	 * @param str Filename
	 * @return URL of the filename
	 */
	private fun getURL(str:String):URL? = try {
		File(str).toURI().toURL()
	} catch(e:MalformedURLException) {
		RuleEditor.log.warn("Invalid URL:$str", e)
		null
	}

	/** PosttranslationalUIGets a string of
	 * @param str String
	 * @return PosttranslationalUIString (If you do not acceptstrReturns)
	 */
	fun getUIText(str:String):String = propLang.getProperty(str) ?: propLangDefault.getProperty(str, str) ?: str

	/** Get game mode description
	 * @param str Mode name
	 * @return Description
	 */
	private fun getModeDesc(str:String):String {
		val str2 = str.replace(' ', '_')
			.replace('(', 'l')
			.replace(')', 'r')
		return propModeDesc.getProperty(str2) ?: propDefaultModeDesc.getProperty(str2, str2) ?: str2
	}

	/** Screen switching
	 * @param cardNumber Card switching destination screen number
	 */
	private fun changeCurrentScreenCard(cardNumber:Int) {
		try {
			contentPaneCardLayout.show(contentPane, SCREENCARD_NAMES[cardNumber])
			currentScreenCardNumber = cardNumber

			// Set menu bar
			jMenuBar = menuBar[cardNumber]

			// Set default button
			var defaultButton:JButton? = null
			when(currentScreenCardNumber) {
				SCREENCARD_SERVERSELECT -> defaultButton = btnServerConnect
				SCREENCARD_LOBBY -> defaultButton = if(tabLobbyAndRoom.selectedIndex==0)
					btnLobbyChatSend
				else
					btnRoomChatSend
				SCREENCARD_SERVERADD -> defaultButton = btnServerAddOK
				SCREENCARD_CREATERATED_WAITING -> defaultButton = btnCreateRatedWaitingCancel
				SCREENCARD_CREATERATED -> defaultButton = btnCreateRatedOK
				SCREENCARD_CREATEROOM -> defaultButton = if(btnCreateRoomOK.isVisible)
					btnCreateRoomOK
				else
					btnCreateRoomCancel
				SCREENCARD_CREATEROOM1P -> defaultButton = if(btnCreateRoom1POK.isVisible)
					btnCreateRoom1POK
				else
					btnCreateRoom1PCancel
				SCREENCARD_MPRANKING -> defaultButton = btnMPRankingOK
				SCREENCARD_RULECHANGE -> defaultButton = btnRuleChangeOK
			}

			if(defaultButton!=null) getRootPane().defaultButton = defaultButton
		} catch(e:Exception) {
			// TODO: There are some threading issue here
			log.debug("changeCurrentScreenCard failed; Possible threading issue", e)
		}
	}

	/** Create String from a Calendar (for chat log)
	 * @param cal Calendar
	 * @param showDate true to show date
	 * @return String created from Calendar
	 */
	fun getTimeAsString(cal:Calendar?, showDate:Boolean = false):String {
		if(cal==null) return if(showDate) "????-??-?? ??:??:??" else "??:??:??"
		val strFormat = if(showDate) "yyyy-MM-dd HH:mm:ss" else "HH:mm:ss"
		val dfm = SimpleDateFormat(strFormat)
		return dfm.format(cal.time)
	}

	/** PlayerOfNameObtained by converting symbol trip
	 * @param pInfo PlayerInformation
	 * @return PlayerOfName(Translated symbol trip)
	 */
	private fun getPlayerNameWithTripCode(pInfo:NetPlayerInfo?):String = convTripCode(pInfo!!.strName)

	/** Convert the symbol trip
	 * @param s String to be converted(MostName)
	 * @return The converted string
	 */
	private fun convTripCode(s:String):String {
		if(!propLang.getProperty("TripSeparator_EnableConvert", false)) return s
		var strName = s
		strName = strName.replace(getUIText("TripSeparator_True"), getUIText("TripSeparator_False"))
		strName = strName.replace("!", getUIText("TripSeparator_True"))
		strName = strName.replace("?", getUIText("TripSeparator_False"))
		return strName
	}

	/** Chat logAdd a new line to the(System Messages)
	 * @param txtpane Chat log
	 * @param str The string to add
	 * @param fgcolor Letter cint(nullYes)
	 */
	fun addSystemChatLog(txtpane:JTextPane, str:String?, fgcolor:Color? = null) {
		val strTime = currentTimeAsString

		var sas:SimpleAttributeSet? = null
		if(fgcolor!=null) {
			sas = SimpleAttributeSet()
			StyleConstants.setForeground(sas, fgcolor)
		}
		try {
			val doc = txtpane.document
			doc.insertString(doc.length, str!!+"\n", sas)
			txtpane.caretPosition = doc.length

			if(txtpane===txtPaneRoomChatLog) {
				if(writerRoomLog!=null) {
					writerRoomLog!!.println("[$strTime] $str")
					writerRoomLog!!.flush()
				}
			} else if(writerLobbyLog!=null) {
				writerLobbyLog!!.println("[$strTime] $str")
				writerLobbyLog!!.flush()
			}
		} catch(_:Exception) {
		}
	}

	/** Chat logAdd a new line to the(System calls from a different thread for
	 * message)
	 * @param txtpane Chat log
	 * @param str The string to add
	 */
	fun addSystemChatLogLater(txtpane:JTextPane, str:String) {
		SwingUtilities.invokeLater {addSystemChatLog(txtpane, str)}
	}

	/** Chat logAdd a new line to the(System calls from a different thread for
	 * message)
	 * @param txtpane Chat log
	 * @param str The string to add
	 * @param fgcolor Letter cint(nullYes)
	 */
	private fun addSystemChatLogLater(txtpane:JTextPane, str:String?, fgcolor:Color) {
		SwingUtilities.invokeLater {addSystemChatLog(txtpane, str, fgcolor)}
	}

	/** Add a user chat to log pane
	 * @param txtpane JTextPane to add this chat log
	 * @param username User name
	 * @param calendar Time
	 * @param str Message
	 */
	private fun addUserChatLog(txtpane:JTextPane, username:String, calendar:Calendar?, str:String) {
		val sasTime = SimpleAttributeSet()
		StyleConstants.setForeground(sasTime, Color.gray)
		val strTime = getTimeAsString(calendar)

		val sas = SimpleAttributeSet()
		StyleConstants.setBold(sas, true)
		StyleConstants.setUnderline(sas, true)

		try {
			val doc = txtpane.document
			doc.insertString(doc.length, "[$strTime]", sasTime)
			doc.insertString(doc.length, "<$username>", sas)
			doc.insertString(doc.length, " $str\n", null)
			txtpane.caretPosition = doc.length

			if(txtpane===txtPaneRoomChatLog) {
				if(writerRoomLog!=null) {
					writerRoomLog!!.println("[$strTime]<$username> $str")
					writerRoomLog!!.flush()
				}
			} else if(writerLobbyLog!=null) {
				writerLobbyLog!!.println("[$strTime]<$username> $str")
				writerLobbyLog!!.flush()
			}
		} catch(_:Exception) {
		}
	}

	/** Add a user chat to log pane (for multi threading)
	 * @param txtpane JTextPane to add this chat log
	 * @param username User name
	 * @param calendar Time
	 * @param str Message
	 */
	private fun addUserChatLogLater(txtpane:JTextPane, username:String, calendar:Calendar?,
		str:String) {
		SwingUtilities.invokeLater {addUserChatLog(txtpane, username, calendar, str)}
	}

	/** Add a recorded user chat to log pane
	 * @param txtpane JTextPane to add this chat log
	 * @param username User name
	 * @param calendar Time
	 * @param str Message
	 */
	private fun addRecordedUserChatLog(txtpane:JTextPane, username:String, calendar:Calendar?, str:String) {
		val sasTime = SimpleAttributeSet()
		StyleConstants.setForeground(sasTime, Color.gray)
		val strTime = getTimeAsString(calendar, true)

		val sasUserName = SimpleAttributeSet()
		StyleConstants.setBold(sasUserName, true)
		StyleConstants.setUnderline(sasUserName, true)
		StyleConstants.setForeground(sasUserName, Color.gray)

		val sasMessage = SimpleAttributeSet()
		StyleConstants.setForeground(sasMessage, Color.darkGray)

		try {
			val doc = txtpane.document
			doc.insertString(doc.length, "[$strTime]", sasTime)
			doc.insertString(doc.length, "<$username>", sasUserName)
			doc.insertString(doc.length, " $str\n", sasMessage)
			txtpane.caretPosition = doc.length

			if(txtpane===txtPaneRoomChatLog) {
				if(writerRoomLog!=null) {
					writerRoomLog!!.println("[$strTime]<$username> $str")
					writerRoomLog!!.flush()
				}
			} else if(writerLobbyLog!=null) {
				writerLobbyLog!!.println("[$strTime]<$username> $str")
				writerLobbyLog!!.flush()
			}
		} catch(_:Exception) {
		}
	}

	/** Add a recorded user chat to log pane (for multi threading)
	 * @param txtpane JTextPane to add this chat log
	 * @param username User name
	 * @param calendar Time
	 * @param str Message
	 */
	private fun addRecordedUserChatLogLater(txtpane:JTextPane, username:String, calendar:Calendar?,
		str:String) {
		SwingUtilities.invokeLater {addRecordedUserChatLog(txtpane, username, calendar, str)}
	}

	/** FileDefaultListModelRead on
	 * @param listModel Which to readDefaultListModel
	 * @param filename Filename
	 * @return The successtrue
	 */
	private fun loadListToDefaultListModel(listModel:DefaultListModel<String>, filename:String):Boolean {
		try {
			listModel.clear()
			FileReader(filename).buffered().use {
				it.forEachLine {str ->
					if(str.isNotEmpty()) listModel.addElement(str)
				}
			}
		} catch(e:IOException) {
			log.debug("Failed to load list from $filename", e)
			return false
		}

		return true
	}

	/** Load game mode list to a DefaultListModel
	 * @param listModel DefaultListModel
	 * @param filename Filename of mode list
	 * @return `true` if success
	 */
	private fun loadModeList(listModel:DefaultListModel<String>, filename:String):Boolean {
		try {
			listModel.clear()
			FileReader(filename).buffered().use {
				it.forEachLine {str ->
					if(str.isEmpty()||str.startsWith("#")) {
						// Empty line or comment line. Ignore it.
					} else if(str.startsWith(":")) {
						// Game style tag. Currently unused.
					} else {
						// Game mode name
						val commaIndex = str.indexOf(',')
						if(commaIndex!=-1) listModel.addElement(str.take(commaIndex))
					}
				}
			}
		} catch(e:IOException) {
			log.debug("Failed to load list from $filename", e)
			return false
		}

		return true
	}

	/** Load game mode list to a DefaultComboBoxModel
	 * @param listModel DefaultComboBoxModel
	 * @param filename Filename of mode list
	 * @return `true` if success
	 */
	private fun loadModeList(listModel:DefaultComboBoxModel<String>, filename:String):Boolean {
		try {
			listModel.removeAllElements()
			FileReader(filename).buffered().use {
				it.forEachLine {str ->
					if(str.isEmpty()||str.startsWith("#")) {
						// Empty line or comment line. Ignore it.
					} else if(str.startsWith(":")) {
						// Game style tag. Currently unused.
					} else {
						// Game mode name
						val commaIndex = str.indexOf(',')
						if(commaIndex!=-1) listModel.addElement(str.take(commaIndex))
					}
				}
			}
		} catch(e:IOException) {
			log.debug("Failed to load list from $filename", e)
			return false
		}

		return true
	}

	/** DefaultListModelSave it to a file from
	 * @param listModel Preservation of the originalDefaultListModel
	 * @param filename Filename
	 * @return The successtrue
	 */
	private fun saveListFromDefaultListModel(listModel:DefaultListModel<*>, filename:String):Boolean {
		try {
			PrintWriter(filename).use {
				for(i in 0..<listModel.size())
					it.println(listModel.get(i))
				it.flush()
			}
		} catch(e:IOException) {
			log.debug("Failed to save server list", e)
			return false
		}

		return true
	}

	/** Set enabled state of lobby buttons
	 * @param mode 0=Disable all 1=Full Lobby 2=Inside Room
	 */
	private fun setLobbyButtonsEnabled(mode:Int) {
		btnRoomListQuickStart.isEnabled = mode==1
		btnRoomListRoomCreate.isEnabled = mode==1
		btnRoomListRoomCreate1P.isEnabled = mode==1

		itemLobbyMenuRuleChange.isEnabled = mode==1
		itemLobbyMenuTeamChange.isEnabled = mode>=1
		itemLobbyMenuRanking.isEnabled = mode>=1

		btnLobbyChatSend.isEnabled = mode>=1
		txtfldLobbyChatInput.isEnabled = mode>=1
	}

	/** Screen Room buttonOf is enabledChange the state
	 * @param b When true, is enabled, falseInvalid if
	 */
	private fun setRoomButtonsEnabled(b:Boolean) {
		btnRoomButtonsTeamChange.isEnabled = b
		btnRoomButtonsJoin.isEnabled = b
		btnRoomButtonsSitOut.isEnabled = b
		btnRoomButtonsViewSetting.isEnabled = b
		btnRoomButtonsRanking.isEnabled = b
		btnRoomChatSend.isEnabled = b
		txtfldRoomChatInput.isEnabled = b
	}

	/** Participation in the screen room buttonAnd withdrawal buttonSwitching
	 * @param b trueWhen the war button, falseWhen the withdrawal buttonShow
	 */
	private fun setRoomJoinButtonVisible(b:Boolean) {
		btnRoomButtonsJoin.isVisible = b
		btnRoomButtonsJoin.isEnabled = true
		btnRoomButtonsSitOut.isVisible = !b
		btnRoomButtonsSitOut.isEnabled = true
	}

	/** Room list tableRow dataCreate
	 * @param r Room Information
	 * @return Line data
	 */
	private fun createRoomListRowData(r:NetRoomInfo):Array<String> = arrayOf(
		r.roomID.toString(), r.strName, if(r.rated) getUIText("RoomTable_Rated_True") else getUIText("RoomTable_Rated_False"),
		if(r.ruleLock) r.ruleName.uppercase() else getUIText("RoomTable_RuleName_Any"), r.strMode,
		if(r.playing) getUIText("RoomTable_Status_Playing") else getUIText("RoomTable_Status_Waiting"),
		"${r.playerSeatedCount}/${r.maxPlayers}", r.spectatorCount.toString()
	)

	/** Entered the room that you specify
	 * @param roomID RoomID
	 * @param watch When true,Watching only
	 */
	fun joinRoom(roomID:Int, watch:Boolean) {
		tabLobbyAndRoom.setEnabledAt(1, true)
		tabLobbyAndRoom.selectedIndex = 1

		if(netPlayerClient!!.yourPlayerInfo!!.roomID!=roomID) {
			txtPaneRoomChatLog.text = ""
			setRoomButtonsEnabled(false)
			netPlayerClient!!.send("roomjoin\t$roomID\t$watch\n")
		}

		changeCurrentScreenCard(SCREENCARD_LOBBY)
	}

	/** Change UI type of multiplayer room create screen
	 * @param isDetailMode true=View Detail, false=Create Room
	 * @param roomInfo Room Information (only used when isDetailMode is true)
	 */
	private fun setCreateRoomUIType(isDetailMode:Boolean, roomInfo:NetRoomInfo?) {
		val r:NetRoomInfo?

		if(isDetailMode) {
			btnCreateRoomOK.isVisible = false
			btnCreateRoomJoin.isVisible = true
			btnCreateRoomWatch.isVisible = true
			r = roomInfo

			if(netPlayerClient!!.yourPlayerInfo!!.roomID==r!!.roomID) {
				btnCreateRoomJoin.isVisible = false
				btnCreateRoomWatch.isVisible = false
			}
		} else {
			btnCreateRoomOK.isVisible = true
			btnCreateRoomJoin.isVisible = false
			btnCreateRoomWatch.isVisible = false

			if(backupRoomInfo!=null)
				r = backupRoomInfo
			else {
				r = NetRoomInfo().apply {
					maxPlayers = propConfig.getProperty("createroom.defaultMaxPlayers", 6)
					autoStartSeconds = propConfig.getProperty("createroom.defaultAutoStartSeconds", 15)
					gravity = propConfig.getProperty("createroom.defaultGravity", 1)
					denominator = propConfig.getProperty("createroom.defaultDenominator", 60)
					are = propConfig.getProperty("createroom.defaultARE", 0)
					areLine = propConfig.getProperty("createroom.defaultARELine", 0)
					lineDelay = propConfig.getProperty("createroom.defaultLineDelay", 0)
					lockDelay = propConfig.getProperty("createroom.defaultLockDelay", 30)
					das = propConfig.getProperty("createroom.defaultDAS", 11)
					hurryUpSeconds = propConfig.getProperty("createroom.defaultHurryupSeconds", 180)
					hurryUpInterval = propConfig.getProperty("createroom.defaultHurryupInterval", 5)
					messiness = propConfig.getProperty("createroom.defaultGarbagePercent", 90)
					ruleLock = propConfig.getProperty("createroom.defaultRuleLock", false)
					twistEnableType = propConfig.getProperty("createroom.defaultTwistEnableType", 2)
					twistEnableEZ = propConfig.getProperty("createroom.defaultTwistEnableEZ", true)
					b2b = propConfig.getProperty("createroom.defaultB2B", true)
					combo = propConfig.getProperty("createroom.defaultCombo", true)
					rensaBlock = propConfig.getProperty("createroom.defaultRensaBlock", true)
					counter = propConfig.getProperty("createroom.defaultCounter", true)
					bravo = propConfig.getProperty("createroom.defaultBravo", true)
					reduceLineSend = propConfig.getProperty("createroom.defaultReduceLineSend", true)
					garbageChangePerAttack = propConfig.getProperty("createroom.defaultGarbageChangePerAttack", true)
					b2bChunk = propConfig.getProperty("createroom.defaultB2BChunk", false)
					useFractionalGarbage = propConfig.getProperty("createroom.defaultUseFractionalGarbage", false)
					autoStartTNET2 = propConfig.getProperty("createroom.defaultAutoStartTNET2", false)
					disableTimerAfterSomeoneCancelled =
						propConfig.getProperty("createroom.defaultDisableTimerAfterSomeoneCancelled", false)
					useMap = propConfig.getProperty("createroom.defaultUseMap", false)
					//propConfig.getProperty("createroom.defaultMapSetID", 0);
					ruleName = propConfig.getProperty("createroom.ruleName", "")
				}
			}
		}

		importRoomInfoToCreateRoomScreen(r)
	}

	/** Change UI type of single player room create screen
	 * @param isDetailMode true=View Detail, false=Create Room
	 * @param roomInfo Room Information (only used when isDetailMode is true)
	 */
	private fun setCreateRoom1PUIType(isDetailMode:Boolean, roomInfo:NetRoomInfo?) {
		val r:NetRoomInfo?

		if(isDetailMode) {
			r = roomInfo

			if(netPlayerClient!!.yourPlayerInfo!!.roomID==r!!.roomID)
				btnCreateRoom1POK.isVisible = false
			else {
				btnCreateRoom1POK.isVisible = true
				btnCreateRoom1POK.text = getUIText("CreateRoom1P_OK_Watch")
				btnCreateRoom1POK.setMnemonic('W')
			}
		} else {
			btnCreateRoom1POK.isVisible = true
			btnCreateRoom1POK.text = getUIText("CreateRoom1P_OK")
			btnCreateRoom1POK.setMnemonic('O')

			if(backupRoomInfo1P!=null)
				r = backupRoomInfo1P
			else {
				r = NetRoomInfo().apply {
					maxPlayers = 1
					singleplayer = true
					strMode = propConfig.getProperty("createroom1p.strMode", "")
					ruleName = propConfig.getProperty("createroom1p.ruleName", "")
				}
			}
		}

		if(r!=null) {
			//listboxCreateRoom1PModeList.setSelectedIndex(0);
			//listboxCreateRoom1PRuleList.setSelectedIndex(0);
			listboxCreateRoom1PModeList.setSelectedValue(r.strMode, true)
			listboxCreateRoom1PRuleList.setSelectedValue(r.ruleName, true)
		}
	}

	/** Switch to room detail screen
	 * @param roomID Room ID
	 */
	fun viewRoomDetail(roomID:Int) {
		val roomInfo = netPlayerClient!!.getRoomInfo(roomID)

		if(roomInfo!=null) {
			currentViewDetailRoomID = roomID

			if(roomInfo.singleplayer) {
				setCreateRoom1PUIType(true, roomInfo)
				changeCurrentScreenCard(SCREENCARD_CREATEROOM1P)
			} else {
				setCreateRoomUIType(true, roomInfo)
				changeCurrentScreenCard(SCREENCARD_CREATEROOM)
			}
		}
	}

	/** Lobby screenPlayerList update */
	private fun updateLobbyUserList() {
		val pList = LinkedList(netPlayerClient!!.playerInfoList)

		if(!pList.isEmpty()) {
			listmodelLobbyChatPlayerList.clear()

			for(pInfo in pList) {
				// Name
				var name = getPlayerNameWithTripCode(pInfo)
				if(pInfo.uid==netPlayerClient!!.playerUID) name = "*"+getPlayerNameWithTripCode(pInfo)

				// Team
				if(pInfo.strTeam.isNotEmpty()) {
					name = getPlayerNameWithTripCode(pInfo)+" - "+pInfo.strTeam
					if(pInfo.uid==netPlayerClient!!.playerUID) name = "*${getPlayerNameWithTripCode(pInfo)} - "+pInfo.strTeam
				}

				// Rating
				name += " |${pInfo.rating[0]}|"
				/* name += " |T:" + pInfo.rating[0] + "|";
				 * name += "A:" + pInfo.rating[1] + "|";
				 * name += "P:" + pInfo.rating[2] + "|";
				 * name += "S:" + pInfo.rating[3] + "|"; */

				// Country code
				if(pInfo.strCountry.isNotEmpty()) name += " (${pInfo.strCountry})"

				/* XXX Hostname
				 * if(pInfo.strHost.length() > 0) {
				 * name += " {" + pInfo.strHost + "}";
				 * } */

				if(pInfo.roomID==-1)
					listmodelLobbyChatPlayerList.addElement(name)
				else
					listmodelLobbyChatPlayerList.addElement("{${pInfo.roomID}} "+name)
			}
		}
	}

	/** Screen RoomPlayerList update */
	private fun updateRoomUserList() {
		val roomInfo = netPlayerClient!!.getRoomInfo(netPlayerClient!!.yourPlayerInfo!!.roomID) ?: return

		val pList = LinkedList(netPlayerClient!!.playerInfoList)

		if(!pList.isEmpty()) {
			listModelRoomChatPlayerList.clear()

			for(i in 0..<roomInfo.maxPlayers)
				listModelRoomChatPlayerList.addElement("[${(i+1)}]")

			for(pInfo in pList) {
				if(pInfo.roomID==roomInfo.roomID) {
					// Name
					var name = getPlayerNameWithTripCode(pInfo)
					if(pInfo.uid==netPlayerClient!!.playerUID) name = "*"+getPlayerNameWithTripCode(pInfo)

					// Team
					if(pInfo.strTeam.isNotEmpty()) {
						name = getPlayerNameWithTripCode(pInfo)+" - "+pInfo.strTeam
						if(pInfo.uid==netPlayerClient!!.playerUID) name = "*${getPlayerNameWithTripCode(pInfo)} - "+pInfo.strTeam
					}

					// Rating
					name += " |${pInfo.rating[roomInfo.style]}|"

					// Country code
					if(pInfo.strCountry.isNotEmpty()) name += " (${pInfo.strCountry})"

					/* XXX Hostname
					 * if(pInfo.strHost.length() > 0) {
					 * name += " {" + pInfo.strHost + "}";
					 * } */

					// Status
					if(pInfo.playing)
						name += getUIText("RoomUserList_Playing")
					else if(pInfo.ready) name += getUIText("RoomUserList_Ready")

					if(pInfo.seatID>=0&&pInfo.seatID<roomInfo.maxPlayers)
						listModelRoomChatPlayerList.set(pInfo.seatID, "[${(pInfo.seatID+1)}] "+name)
					else if(pInfo.queueID!=-1)
						listModelRoomChatPlayerList.addElement("${(pInfo.queueID+1)}. $name")
					else
						listModelRoomChatPlayerList.addElement(name)
				}
			}
		}
	}

	/** Being in the same roomPlayerUpdate the list
	 * @return Being in the same roomPlayerList
	 */
	fun updateSameRoomPlayerInfoList():LinkedList<NetPlayerInfo> {
		val pList = LinkedList(netPlayerClient!!.playerInfoList)
		val roomID = netPlayerClient!!.yourPlayerInfo!!.roomID
		sameRoomPlayerInfoList.clear()

		for(pInfo in pList)
			if(pInfo.roomID==roomID) sameRoomPlayerInfoList.add(pInfo)

		return sameRoomPlayerInfoList
	}

	/** Rule dataTransmission */
	private fun sendMyRuleDataToServer() {
		if(ruleOptPlayer==null) ruleOptPlayer = RuleOptions()

		val prop = CustomProperties()
		ruleOptPlayer!!.writeProperty(prop, 0)
		val strRuleTemp = prop.encode("RuleData") ?: ""
		val strRuleData = NetUtil.compressString(strRuleTemp)
		log.debug("RuleData uncompressed:${strRuleTemp.length} compressed:"+strRuleData.length)

		// checkSam calculation
		val checksumObj = Adler32()
		checksumObj.update(NetUtil.stringToBytes(strRuleData))
		val sChecksum = checksumObj.value

		// Transmission
		netPlayerClient!!.send("ruledata\t$sChecksum\t$strRuleData\n")
	}

	/** To save the settings in the lobby */
	fun saveConfig() {
		propConfig.setProperty("mainwindow.width", size.width)
		propConfig.setProperty("mainwindow.height", size.height)
		propConfig.setProperty("mainwindow.x", location.x)
		propConfig.setProperty("mainwindow.y", location.y)
		propConfig.setProperty("lobby.splitLobby.location", splitLobby.dividerLocation)
		propConfig.setProperty("lobby.splitLobbyChat.location", splitLobbyChat.dividerLocation)
		propConfig.setProperty("room.splitRoom.location", splitRoom.dividerLocation)
		propConfig.setProperty("room.splitRoomChat.location", splitRoomChat.dividerLocation)
		propConfig.setProperty("serverselect.txtfldPlayerName.text", txtfldPlayerName.text)
		propConfig.setProperty("serverselect.txtfldPlayerTeam.text", txtfldPlayerTeam.text)

		propConfig.setProperty("serverselect.listboxServerList.value", listboxServerList.selectedValue ?: "")
		tableRoomList.columnModel.let {
			if(it.columnCount==8) {
				propConfig.setProperty("tableRoomList.width.id", it.getColumn(0).width)
				propConfig.setProperty("tableRoomList.width.name", it.getColumn(1).width)
				propConfig.setProperty("tableRoomList.width.rated", it.getColumn(2).width)
				propConfig.setProperty("tableRoomList.width.rulename", it.getColumn(3).width)
				propConfig.setProperty("tableRoomList.width.modename", it.getColumn(4).width)
				propConfig.setProperty("tableRoomList.width.status", it.getColumn(5).width)
				propConfig.setProperty("tableRoomList.width.players", it.getColumn(6).width)
				propConfig.setProperty("tableRoomList.width.spectators", it.getColumn(7).width)
			}
		}

		tableGameStat.columnModel.let {
			if(it.columnCount==13) {
				propConfig.setProperty("tableGameStat.width.rank", it.getColumn(0).width)
				propConfig.setProperty("tableGameStat.width.name", it.getColumn(1).width)
				propConfig.setProperty("tableGameStat.width.attack", it.getColumn(2).width)
				propConfig.setProperty("tableGameStat.width.apl", it.getColumn(3).width)
				propConfig.setProperty("tableGameStat.width.apm", it.getColumn(4).width)
				propConfig.setProperty("tableGameStat.width.lines", it.getColumn(5).width)
				propConfig.setProperty("tableGameStat.width.lpm", it.getColumn(6).width)
				propConfig.setProperty("tableGameStat.width.piece", it.getColumn(7).width)
				propConfig.setProperty("tableGameStat.width.pps", it.getColumn(8).width)
				propConfig.setProperty("tableGameStat.width.time", it.getColumn(9).width)
				propConfig.setProperty("tableGameStat.width.ko", it.getColumn(10).width)
				propConfig.setProperty("tableGameStat.width.wins", it.getColumn(11).width)
				propConfig.setProperty("tableGameStat.width.games", it.getColumn(12).width)
			}
		}
		tableGameStat1P.columnModel.let {
			if(it.columnCount==2) {
				propConfig.setProperty("tableGameStat1P.width.description", it.getColumn(0).width)
				propConfig.setProperty("tableGameStat1P.width.value", it.getColumn(1).width)
			}
		}
		tableMPRanking[0].columnModel.let {
			if(it.columnCount==5) {
				propConfig.setProperty("tableMPRanking.width.rank", it.getColumn(0).width)
				propConfig.setProperty("tableMPRanking.width.name", it.getColumn(1).width)
				propConfig.setProperty("tableMPRanking.width.rating", it.getColumn(2).width)
				propConfig.setProperty("tableMPRanking.width.play", it.getColumn(3).width)
				propConfig.setProperty("tableMPRanking.width.win", it.getColumn(4).width)
			}
		}
		backupRoomInfo?.let {
			propConfig.setProperty("createroom.defaultMaxPlayers", it.maxPlayers)
			propConfig.setProperty("createroom.defaultAutoStartSeconds", it.autoStartSeconds)
			propConfig.setProperty("createroom.defaultGravity", it.gravity)
			propConfig.setProperty("createroom.defaultDenominator", it.denominator)
			propConfig.setProperty("createroom.defaultARE", it.are)
			propConfig.setProperty("createroom.defaultARELine", it.areLine)
			propConfig.setProperty("createroom.defaultLineDelay", it.lineDelay)
			propConfig.setProperty("createroom.defaultLockDelay", it.lockDelay)
			propConfig.setProperty("createroom.defaultDAS", it.das)
			propConfig.setProperty("createroom.defaultGarbagePercent", it.messiness)
			propConfig.setProperty("createroom.defaultTargetTimer", it.targetTimer)
			propConfig.setProperty("createroom.defaultHurryupSeconds", it.hurryUpSeconds)
			propConfig.setProperty("createroom.defaultHurryupInterval", it.hurryUpInterval)
			propConfig.setProperty("createroom.defaultRuleLock", it.ruleLock)
			propConfig.setProperty("createroom.defaultTwistEnableType", it.twistEnableType)
			propConfig.setProperty("createroom.defaultTwistEnableEZ", it.twistEnableEZ)
			propConfig.setProperty("createroom.defaultB2B", it.b2b)
			propConfig.setProperty("createroom.defaultCombo", it.combo)
			propConfig.setProperty("createroom.defaultRensaBlock", it.rensaBlock)
			propConfig.setProperty("createroom.defaultCounter", it.counter)
			propConfig.setProperty("createroom.defaultBravo", it.bravo)
			propConfig.setProperty("createroom.defaultReduceLineSend", it.reduceLineSend)
			propConfig.setProperty("createroom.defaultGarbageChangePerAttack", it.garbageChangePerAttack)
			propConfig.setProperty("createroom.defaultB2BChunk", it.b2bChunk)
			propConfig.setProperty("createroom.defaultUseFractionalGarbage", it.useFractionalGarbage)
			propConfig.setProperty("createroom.defaultIsTarget", it.isTarget)
			propConfig.setProperty("createroom.defaultAutoStartTNET2", it.autoStartTNET2)
			propConfig.setProperty("createroom.defaultDisableTimerAfterSomeoneCancelled", it.disableTimerAfterSomeoneCancelled)
			propConfig.setProperty("createroom.defaultUseMap", it.useMap)
			propConfig.setProperty("createroom.defaultMapSetID", spinnerCreateRoomMapSetID.value as Int)
		}

		val listboxCreateRoom1PModeListSelectedValue = listboxCreateRoom1PModeList.selectedValue
		if(listboxCreateRoom1PModeListSelectedValue!=null)
			propConfig.setProperty("createroom1p.listboxCreateRoom1PModeList.value", listboxCreateRoom1PModeListSelectedValue)
		else
			propConfig.setProperty("createroom1p.listboxCreateRoom1PModeList.value", "")

		val listboxCreateRoom1PRuleListSelectedValue = listboxCreateRoom1PRuleList.selectedValue
		if(listboxCreateRoom1PRuleListSelectedValue!=null&&listboxCreateRoom1PRuleList.selectedIndex>=1)
			propConfig.setProperty("createroom1p.listboxCreateRoom1PRuleList.value", listboxCreateRoom1PRuleListSelectedValue)
		else
			propConfig.setProperty("createroom1p.listboxCreateRoom1PRuleList.value", "")

		propConfig.setProperty("createroom.defaultPresetID", spinnerCreateRoomPresetID.value as Int)

		try {
			val out = FileOutputStream("config/setting/netlobby.cfg")
			propConfig.store(out, "NullpoMino NetLobby Config")
			out.close()
		} catch(e:IOException) {
			log.warn("Failed to save netlobby config file", e)
		}
	}

	/** Save global config file */
	private fun saveGlobalConfig() {
		try {
			propGlobal = Json.decodeFromString(FileInputStream("config/setting/global.json").bufferedReader().use {it.readText()})
		} catch(e:Exception) {
			log.warn("Failed to save global config file", e)
		}
	}

	/** End processing */
	fun shutdown() {
		saveConfig()

		writerLobbyLog?.flush()
		writerLobbyLog?.close()
		writerLobbyLog = null

		writerRoomLog?.flush()
		writerRoomLog?.close()
		writerRoomLog = null

		// Cut
		if(netPlayerClient?.isConnected==true) netPlayerClient?.send("disconnect\n")
		netPlayerClient?.threadRunning = false
		netPlayerClient?.interrupt()
		netPlayerClient = null

		// ListenerCall
		if(listeners!=null) {
			for(l in listeners!!)
				l.onLobbyExit(this)
			listeners = null
		}
		netDummyMode?.onLobbyExit(this)
		netDummyMode = null

		dispose()
	}

	/** Delete server buttonWhen processing is pressed */
	fun serverSelectDeleteButtonClicked() {
		val index = listboxServerList.selectedIndex
		if(index!=-1) {
			val server = listboxServerList.selectedValue as String
			val answer = JOptionPane.showConfirmDialog(
				this, getUIText("MessageBody_ServerDelete")+"\n"
					+server, getUIText("MessageTitle_ServerDelete"), JOptionPane.YES_NO_OPTION
			)
			if(answer==JOptionPane.YES_OPTION) {
				listModelServerList.remove(index)
				saveListFromDefaultListModel(listModelServerList, "config/setting/netlobby_serverlist.cfg")
			}
		}
	}

	/** Server connection buttonWhen processing is pressed */
	fun serverSelectConnectButtonClicked() {
		val index = listboxServerList.selectedIndex
		if(index!=-1) {
			val strServer = listboxServerList.selectedValue as String
			var portSplitter = strServer.indexOf(":")
			if(portSplitter==-1) portSplitter = strServer.length

			val strHost = strServer.take(portSplitter)
			log.debug("Host:$strHost")

			var port = NetBaseClient.DEFAULT_PORT
			try {
				val strPort = strServer.substring(portSplitter+1, strServer.length)
				port = strPort.toInt()
			} catch(e2:Exception) {
				log.debug("Failed to get port number; Try to use default port")
			}

			log.debug("Port:$port")

			netPlayerClient = NetPlayerClient(strHost, port, txtfldPlayerName.text, txtfldPlayerTeam.text.trim {it<=' '}).also {
				it.isDaemon = true
				it.addListener(this)
				it.start()
			}

			txtpaneLobbyChatLog.text = ""
			setLobbyButtonsEnabled(0)
			tablemodelRoomList.rowCount = 0

			changeCurrentScreenCard(SCREENCARD_LOBBY)
		}
	}

	/** Monitoring settings buttonWhen processing is pressed */
	fun serverSelectSetObserverButtonClicked() {
		val index = listboxServerList.selectedIndex
		if(index!=-1) {
			val strServer = listboxServerList.selectedValue as String
			var portSpliter = strServer.indexOf(":")
			if(portSpliter==-1) portSpliter = strServer.length

			val strHost = strServer.take(portSpliter)
			log.debug("Host:$strHost")

			var port = NetBaseClient.DEFAULT_PORT
			try {
				val strPort = strServer.substring(portSpliter+1, strServer.length)
				port = strPort.toInt()
			} catch(e2:Exception) {
				log.debug("Failed to get port number; Try to use default port")
			}

			log.debug("Port:$port")

			val answer = JOptionPane.showConfirmDialog(
				this, getUIText("MessageBody_SetObserver")+"\n"
					+strServer, getUIText("MessageTitle_SetObserver"), JOptionPane.YES_NO_OPTION
			)

			if(answer==JOptionPane.YES_OPTION) {
				propObserver.setProperty("observer.enable", true)
				propObserver.setProperty("observer.host", strHost)
				propObserver.setProperty("observer.port", port)

				try {
					val out = FileOutputStream("config/setting/netobserver.cfg")
					propObserver.store(out, "NullpoMino NetObserver Config")
					out.close()
				} catch(e:IOException) {
					log.warn("Failed to save NetObserver config file", e)
				}
			}
		}
	}

	/** Send a chat message
	 * @param roomchat `true` if room chat
	 * @param strMsg Message to send
	 */
	private fun sendChat(roomchat:Boolean, strMsg:String) {
		var msg = strMsg
		when {
			msg.startsWith("/team") -> {
				msg = msg.replaceFirst(Regex("/team"), "")
				msg = msg.trim {it<=' '}
				netPlayerClient?.send("changeteam\t${NetUtil.urlEncode(msg)}\n")
			}
			roomchat -> netPlayerClient?.send("chat\t${NetUtil.urlEncode(msg)}\n")
			else -> netPlayerClient?.send("lobbychat\t${NetUtil.urlEncode(msg)}\n")
		}
	}

	/** Creates NetRoomInfo from Create Room screen
	 * @return NetRoomInfo
	 */
	private fun exportRoomInfoFromCreateRoomScreen():NetRoomInfo? {
		try {
			val roomInfo = NetRoomInfo()

			val roomName = txtfldCreateRoomName.text
			val modeName = comboboxCreateRoomMode.selectedItem as String
			val integerMaxPlayers = spinnerCreateRoomMaxPlayers.value as Int
			val integerAutoStartSeconds = spinnerCreateRoomAutoStartSeconds.value as Int
			val integerGravity = spinnerCreateRoomGravity.value as Int
			val integerDenominator = spinnerCreateRoomDenominator.value as Int
			val integerARE = spinnerCreateRoomARE.value as Int
			val integerARELine = spinnerCreateRoomARELine.value as Int
			val integerLineDelay = spinnerCreateRoomLineDelay.value as Int
			val integerLockDelay = spinnerCreateRoomLockDelay.value as Int
			val integerDAS = spinnerCreateRoomDAS.value as Int
			val integerHurryupSeconds = spinnerCreateRoomHurryupSeconds.value as Int
			val integerHurryupInterval = spinnerCreateRoomHurryupInterval.value as Int
			val rulelock = chkboxCreateRoomRuleLock.isSelected
			val twistEnableType = comboboxCreateRoomTWISTEnableType.selectedIndex
			val twistEnableEZ = chkboxCreateRoomTWISTEnableEZ.isSelected
			val b2b = chkboxCreateRoomB2B.isSelected
			val combo = chkboxCreateRoomCombo.isSelected
			val rensaBlock = chkboxCreateRoomRensaBlock.isSelected
			val counter = chkboxCreateRoomCounter.isSelected
			val bravo = chkboxCreateRoomBravo.isSelected
			val reduceLineSend = chkboxCreateRoomReduceLineSend.isSelected
			val autoStartTNET2 = chkboxCreateRoomAutoStartTNET2.isSelected
			val disableTimerAfterSomeoneCancelled = chkboxCreateRoomDisableTimerAfterSomeoneCancelled.isSelected
			val useMap = chkboxCreateRoomUseMap.isSelected
			val useFractionalGarbage = chkboxCreateRoomUseFractionalGarbage.isSelected
			val garbageChangePerAttack = chkboxCreateRoomGarbageChangePerAttack.isSelected
			val divideChangeRateByPlayers = chkboxCreateRoomDivideChangeRateByPlayers.isSelected
			val integerGarbagePercent = spinnerCreateRoomGarbagePercent.value as Int
			val b2bChunk = chkboxCreateRoomB2BChunk.isSelected
			val isTarget = chkboxCreateRoomIsTarget.isSelected
			val integerTargetTimer = spinnerCreateRoomTargetTimer.value as Int

			roomInfo.strName = roomName
			roomInfo.strMode = modeName
			roomInfo.maxPlayers = integerMaxPlayers
			roomInfo.autoStartSeconds = integerAutoStartSeconds
			roomInfo.gravity = integerGravity
			roomInfo.denominator = integerDenominator
			roomInfo.are = integerARE
			roomInfo.areLine = integerARELine
			roomInfo.lineDelay = integerLineDelay
			roomInfo.lockDelay = integerLockDelay
			roomInfo.das = integerDAS
			roomInfo.hurryUpSeconds = integerHurryupSeconds
			roomInfo.hurryUpInterval = integerHurryupInterval
			roomInfo.ruleLock = rulelock
			roomInfo.twistEnableType = twistEnableType
			roomInfo.twistEnableEZ = twistEnableEZ
			roomInfo.b2b = b2b
			roomInfo.combo = combo
			roomInfo.rensaBlock = rensaBlock
			roomInfo.counter = counter
			roomInfo.bravo = bravo
			roomInfo.reduceLineSend = reduceLineSend
			roomInfo.autoStartTNET2 = autoStartTNET2
			roomInfo.disableTimerAfterSomeoneCancelled = disableTimerAfterSomeoneCancelled
			roomInfo.useMap = useMap
			roomInfo.useFractionalGarbage = useFractionalGarbage
			roomInfo.garbageChangePerAttack = garbageChangePerAttack
			roomInfo.divideChangeRateByPlayers = divideChangeRateByPlayers
			roomInfo.messiness = integerGarbagePercent
			roomInfo.b2bChunk = b2bChunk
			roomInfo.isTarget = isTarget
			roomInfo.targetTimer = integerTargetTimer

			return roomInfo
		} catch(e:Exception) {
			log.error("Exception on exportRoomInfoFromCreateRoomScreen", e)
		}

		return null
	}

	/** Import NetRoomInfo to Create Room screen
	 * @param r NetRoomInfo
	 */
	private fun importRoomInfoToCreateRoomScreen(r:NetRoomInfo?) {
		if(r!=null) {
			txtfldCreateRoomName.text = r.strName
			comboboxCreateRoomMode.selectedIndex = 0
			if(r.strMode.isNotEmpty()) comboboxCreateRoomMode.selectedItem = r.strMode
			spinnerCreateRoomMaxPlayers.value = r.maxPlayers
			spinnerCreateRoomAutoStartSeconds.value = r.autoStartSeconds
			spinnerCreateRoomGravity.value = r.gravity
			spinnerCreateRoomDenominator.value = r.denominator
			spinnerCreateRoomARE.value = r.are
			spinnerCreateRoomARELine.value = r.areLine
			spinnerCreateRoomLineDelay.value = r.lineDelay
			spinnerCreateRoomLockDelay.value = r.lockDelay
			spinnerCreateRoomDAS.value = r.das
			spinnerCreateRoomHurryupSeconds.value = r.hurryUpSeconds
			spinnerCreateRoomHurryupInterval.value = r.hurryUpInterval
			spinnerCreateRoomGarbagePercent.value = r.messiness
			spinnerCreateRoomTargetTimer.value = r.targetTimer
			chkboxCreateRoomUseMap.isSelected = r.useMap
			chkboxCreateRoomRuleLock.isSelected = r.ruleLock
			comboboxCreateRoomTWISTEnableType.selectedIndex = r.twistEnableType
			chkboxCreateRoomTWISTEnableEZ.isSelected = r.twistEnableEZ
			chkboxCreateRoomB2B.isSelected = r.b2b
			chkboxCreateRoomCombo.isSelected = r.combo
			chkboxCreateRoomRensaBlock.isSelected = r.rensaBlock
			chkboxCreateRoomCounter.isSelected = r.counter
			chkboxCreateRoomBravo.isSelected = r.bravo
			chkboxCreateRoomReduceLineSend.isSelected = r.reduceLineSend
			chkboxCreateRoomGarbageChangePerAttack.isSelected = r.garbageChangePerAttack
			chkboxCreateRoomDivideChangeRateByPlayers.isSelected = r.divideChangeRateByPlayers
			chkboxCreateRoomB2BChunk.isSelected = r.b2bChunk
			chkboxCreateRoomUseFractionalGarbage.isSelected = r.useFractionalGarbage
			chkboxCreateRoomIsTarget.isSelected = r.isTarget
			chkboxCreateRoomAutoStartTNET2.isSelected = r.autoStartTNET2
			chkboxCreateRoomDisableTimerAfterSomeoneCancelled.isSelected = r.disableTimerAfterSomeoneCancelled
		}
	}

	/** Create rule entries (for rule change screen)
	 * @param filelist Rule file list
	 */
	private fun createRuleEntries(filelist:Array<String>) {
		ruleEntries.clear()
		for(element in filelist) {
			val entry = RuleConf()

			val file = File("config/rule/$element")
			entry.file = element
			entry.path = file.path

			val prop = CustomProperties()
			try {
				val `in` = GZIPInputStream(FileInputStream("config/rule/$element"))
				prop.load(`in`)
				`in`.close()
				entry.name = prop.getProperty("0.ruleOpt.strRuleName", "")
				entry.style = prop.getProperty("0.ruleOpt.style", 0)
			} catch(e:Exception) {
				entry.name = ""
				entry.style = -1
			}

			ruleEntries.add(entry)
		}
	}

	/** Get subset of rule entries (for rule change screen)
	 * @param currentStyle Current style
	 * @return Subset of rule entries
	 */
	private fun getSubsetEntries(currentStyle:Int):LinkedList<RuleConf> {
		val subEntries = LinkedList<RuleConf>()
		if(ruleEntries.size>0) ruleEntries.forEach {ruleEntry ->
			if(ruleEntry.style==currentStyle) subEntries.add(ruleEntry)
		}
		return subEntries
	}

	/** Get rule name + file name list as String[] (for rule change screen)
	 * @param currentStyle Current style
	 * @return Rule name + file name list
	 */
	private fun extractRuleListFromRuleEntries(currentStyle:Int):Array<String> {
		val subEntries = getSubsetEntries(currentStyle)

		return Array(subEntries.size) {
			val entry = subEntries[it]
			return@Array entry.name+" (${entry.file})"
		}
	}

	/** Enter rule change screen */
	private fun enterRuleChangeScreen() {
		// Set rule selections
		val strCurrentFileName = Array(GameEngine.MAX_GAMESTYLE) {
			propGlobal.rule[0][it].file
		}

		for(i in 0..<GameEngine.MAX_GAMESTYLE) {
			val subEntries = getSubsetEntries(i)
			for(j in subEntries.indices)
				if(subEntries[j].file==strCurrentFileName[i]) listboxRuleChangeRuleList[i].selectedIndex = j
		}

		// Tuning
		(propGlobal.tuning.firstOrNull() ?: ConfigGlobal.TuneConf()).let {
			comboboxTuningSpinDirection.selectedIndex = it.spinDir+1
			comboboxTuningMoveDiagonal.selectedIndex = it.moveDiagonal+1
			comboboxTuningBlockShowOutlineOnly.selectedIndex = it.blockShowOutlineOnly+1
			comboboxTuningSkin.selectedIndex = it.skin+2
			comboboxTuningBlockOutlineType.selectedIndex = it.blockOutlineType+1

			txtfldTuningMinDAS.text = "${it.minDAS}"
			txtfldTuningMaxDAS.text = "${it.maxDAS}"
			txtfldTuningDasDelay.text = "${it.owARR}"
			chkboxTuningReverseUpDown.isSelected = it.reverseUpDown
		}
		// Change screen
		changeCurrentScreenCard(SCREENCARD_RULECHANGE)
	}

	/* Menu What Happens at Runtime */
	override fun actionPerformed(e:ActionEvent) {
		//addSystemChatLog(getCurrentChatLogTextPane(), e.getActionCommand(), Color.magenta);

		// Add Server
		if(e.actionCommand=="ServerSelect_ServerAdd") changeCurrentScreenCard(SCREENCARD_SERVERADD)
		// Delete server
		if(e.actionCommand=="ServerSelect_ServerDelete") serverSelectDeleteButtonClicked()
		// Server connection
		if(e.actionCommand=="ServerSelect_Connect") serverSelectConnectButtonClicked()
		// Monitoring settings
		if(e.actionCommand=="ServerSelect_SetObserver") serverSelectSetObserverButtonClicked()
		// Unmonitor
		if(e.actionCommand=="ServerSelect_UnsetObserver")
			if(propObserver.getProperty("observer.enable", false)) {
				val strCurrentHost = propObserver.getProperty("observer.host", "")
				val currentPort = propObserver.getProperty("observer.port", 0)
				val strMessageBox = String.format(getUIText("MessageBody_UnsetObserver"), strCurrentHost, currentPort)

				val answer =
					JOptionPane.showConfirmDialog(this, strMessageBox, getUIText("MessageTitle_UnsetObserver"), JOptionPane.YES_NO_OPTION)

				if(answer==JOptionPane.YES_OPTION) {
					propObserver.setProperty("observer.enable", false)
					try {
						val out = FileOutputStream("config/setting/netobserver.cfg")
						propObserver.store(out, "NullpoMino NetObserver Config")
						out.close()
					} catch(e2:IOException) {
						log.warn("Failed to save NetObserver config file", e2)
					}
				}
			}
		// End
		if(e.actionCommand=="ServerSelect_Exit") shutdown()
		// Quick Start
		if(e.actionCommand=="Lobby_QuickStart") {
			// TODO: Quick Start
		}
		// Create Room 1P
		if(e.actionCommand=="Lobby_RoomCreate1P") {
			currentViewDetailRoomID = -1
			setCreateRoom1PUIType(false, null)
			changeCurrentScreenCard(SCREENCARD_CREATEROOM1P)
		}
		// Create Room Multiplayer
		if(e.actionCommand=="Lobby_RoomCreate") {
			currentViewDetailRoomID = -1
			// setCreateRoomUIType(false, null);
			changeCurrentScreenCard(SCREENCARD_CREATERATED_WAITING)
			netPlayerClient!!.send("getpresets\n")
		}
		// Lobby Options
		if(e.actionCommand=="Lobby_Options") popupLobbyOptions.show(btnRoomListOptions, 0, 0)
		// Rule Change
		if(e.actionCommand=="Lobby_RuleChange") enterRuleChangeScreen()
		// Change team(Lobby screen)
		if(e.actionCommand=="Lobby_TeamChange")
			if(netPlayerClient!=null&&netPlayerClient!!.isConnected) {
				txtfldRoomListTeam.text = netPlayerClient!!.yourPlayerInfo!!.strTeam
				roomListTopBarCardLayout.next(subpanelRoomListTopBar)
			}
		// Cut
		if(e.actionCommand=="Lobby_Disconnect") {
			if(netPlayerClient!=null&&netPlayerClient!!.isConnected) {
				netPlayerClient!!.send("disconnect\n")
				netPlayerClient!!.threadRunning = false
				netPlayerClient!!.interrupt()
				netPlayerClient = null
			}
			tabLobbyAndRoom.selectedIndex = 0
			tabLobbyAndRoom.setEnabledAt(1, false)
			tabLobbyAndRoom.setTitleAt(1, getUIText("Lobby_Tab_NoRoom"))
			setLobbyButtonsEnabled(1)
			title = getUIText("Title_NetLobby")
			changeCurrentScreenCard(SCREENCARD_SERVERSELECT)
		}
		// Multiplayer Leaderboard
		if(e.actionCommand=="Lobby_Ranking"||e.actionCommand=="Room_Ranking")
			if(netPlayerClient!=null&&netPlayerClient!!.isConnected) {
				tablemodelMPRanking[0].rowCount = 0
				netPlayerClient!!.send("mpranking\t0\n")
				changeCurrentScreenCard(SCREENCARD_MPRANKING)
			}
		// Submit chat
		if(e.actionCommand=="Lobby_ChatSend"||e.actionCommand=="Room_ChatSend") {
			if(txtfldLobbyChatInput.text.isNotEmpty()&&netPlayerClient!=null&&netPlayerClient!!.isConnected) {
				sendChat(false, txtfldLobbyChatInput.text)
				txtfldLobbyChatInput.text = ""
			}

			if(netPlayerClient!=null&&netPlayerClient!!.isConnected)
				if(tabLobbyAndRoom.selectedIndex==0) {
					if(txtfldLobbyChatInput.text.isNotEmpty()) {
						sendChat(false, txtfldLobbyChatInput.text)
						txtfldLobbyChatInput.text = ""
					}
				} else if(txtfldRoomChatInput.text.isNotEmpty()) {
					sendChat(true, txtfldRoomChatInput.text)
					txtfldRoomChatInput.text = ""
				}
		}
		// Change teamOK(Lobby screen)
		if(e.actionCommand=="Lobby_TeamChange_OK")
			if(netPlayerClient!=null&&netPlayerClient!!.isConnected) {
				netPlayerClient!!.send("changeteam\t${NetUtil.urlEncode(txtfldRoomListTeam.text)}\n")
				roomListTopBarCardLayout.first(subpanelRoomListTopBar)
			}
		// Change teamCancel(Lobby screen)
		if(e.actionCommand=="Lobby_TeamChange_Cancel")
			roomListTopBarCardLayout.first(subpanelRoomListTopBar)
		// Withdrawal button
		if(e.actionCommand=="Room_Leave") {
			if(netPlayerClient!=null&&netPlayerClient!!.isConnected) netPlayerClient!!.send("roomjoin\t-1\tfalse\n")

			tableModelGameStat.rowCount = 0
			tableModelGameStat1P.rowCount = 0

			tabLobbyAndRoom.selectedIndex = 0
			tabLobbyAndRoom.setEnabledAt(1, false)
			tabLobbyAndRoom.setTitleAt(1, getUIText("Lobby_Tab_NoRoom"))

			changeCurrentScreenCard(SCREENCARD_LOBBY)

			// Listener call
			for(l in listeners!!)
				l.onRoomLeave(this, netPlayerClient!!)
			if(netDummyMode!=null) netDummyMode!!.onRoomLeave(this, netPlayerClient!!)
		}
		// Participation in a war button
		if(e.actionCommand=="Room_Join") {
			netPlayerClient!!.send("changestatus\tfalse\n")
			btnRoomButtonsJoin.isEnabled = false
		}
		// Withdrawal(Watching only) button
		if(e.actionCommand=="Room_SitOut") {
			netPlayerClient!!.send("changestatus\ttrue\n")
			btnRoomButtonsSitOut.isEnabled = false
		}
		// Change team(Room screen)
		if(e.actionCommand=="Room_TeamChange")
			if(netPlayerClient!=null&&netPlayerClient!!.isConnected) {
				txtfldRoomTeam.text = netPlayerClient!!.yourPlayerInfo!!.strTeam
				roomTopBarCardLayout.next(subPanelRoomTopBar)
			}
		// Change teamOK(Room screen)
		if(e.actionCommand=="Room_TeamChange_OK")
			if(netPlayerClient!=null&&netPlayerClient!!.isConnected) {
				netPlayerClient!!.send("changeteam\t${NetUtil.urlEncode(txtfldRoomTeam.text)}\n")
				roomTopBarCardLayout.first(subPanelRoomTopBar)
			}
		// Change teamCancel(Room screen)
		if(e.actionCommand=="Room_TeamChange_Cancel") roomTopBarCardLayout.first(subPanelRoomTopBar)
		// Confirmation rule(Room screen)
		if(e.actionCommand=="Room_ViewSetting") viewRoomDetail(netPlayerClient!!.yourPlayerInfo!!.roomID)
		// In the Add Server screenOK button
		if(e.actionCommand=="ServerAdd_OK") {
			if(txtfldServerAddHost.text.isNotEmpty()) {
				listModelServerList.addElement(txtfldServerAddHost.text)
				saveListFromDefaultListModel(listModelServerList, "config/setting/netlobby_serverlist.cfg")
				txtfldServerAddHost.text = ""
			}
			changeCurrentScreenCard(SCREENCARD_SERVERSELECT)
		}
		// In the Add Server screenCancel button
		if(e.actionCommand=="ServerAdd_Cancel") {
			txtfldServerAddHost.text = ""
			changeCurrentScreenCard(SCREENCARD_SERVERSELECT)
		}
		// Create rated cancel from waiting card
		if(e.actionCommand=="CreateRated_Waiting_Cancel") {
			currentViewDetailRoomID = -1
			changeCurrentScreenCard(SCREENCARD_LOBBY)
		}
		// Create rated OK
		if(e.actionCommand=="CreateRated_OK")
			try {
				val presetIndex = comboboxCreateRatedPresets.selectedIndex
				val r = presets[presetIndex]
				r.strName = txtfldCreateRatedName.text
				backupRoomInfo = r

				val msg = ("ratedroomcreate\t${NetUtil.urlEncode(r.strName)}\t"
					+spinnerCreateRatedMaxPlayers.value+"\t$presetIndex\t"
					+NetUtil.urlEncode("NET-VS-BATTLE")+"\n")

				txtPaneRoomChatLog.text = ""
				setRoomButtonsEnabled(false)
				tabLobbyAndRoom.setEnabledAt(1, true)
				tabLobbyAndRoom.selectedIndex = 1
				changeCurrentScreenCard(SCREENCARD_LOBBY)

				netPlayerClient!!.send(msg)
			} catch(e2:Exception) {
				log.error("Error on CreateRated_OK", e2)
			}

		// Create rated - go to custom settings
		if(e.actionCommand=="CreateRated_Custom") {
			// Load preset into field
			val r = presets[comboboxCreateRatedPresets.selectedIndex]
			setCreateRoomUIType(false, null)
			importRoomInfoToCreateRoomScreen(r)
			// Copy name and number of players
			txtfldCreateRoomName.text = txtfldCreateRatedName.text
			spinnerCreateRoomMaxPlayers.value = spinnerCreateRatedMaxPlayers.value
			// Change screen card
			changeCurrentScreenCard(SCREENCARD_CREATEROOM)
		}
		// Create rated cancel
		if(e.actionCommand=="CreateRated_Cancel") {
			currentViewDetailRoomID = -1
			changeCurrentScreenCard(SCREENCARD_LOBBY)
		}
		// Room-created screensOK button
		if(e.actionCommand=="CreateRoom_OK")
			try {
				val r = exportRoomInfoFromCreateRoomScreen()
				backupRoomInfo = r

				var msg = "roomcreate\t${NetUtil.urlEncode(r!!.strName)}\t"
				msg += NetUtil.urlEncode(r.exportString())+"\t"
				msg += NetUtil.urlEncode(r.strMode)+"\t"

				// Map send
				if(r.useMap) {
					val setID = currentSelectedMapSetID
					log.debug("MapSetID:$setID")

					mapList.clear()
					val propMap = CustomProperties()
					try {
						val `in` = FileInputStream("config/map/vsbattle/$setID.map")
						propMap.load(`in`)
						`in`.close()
					} catch(e2:IOException) {
						log.error("Map set $setID not found", e2)
					}

					val maxMap = propMap.getProperty("values.maxMapNumber", 0)
					log.debug("Number of maps:$maxMap")

					val strMap = StringBuilder()

					for(i in 0..<maxMap) {
						val strMapTemp = propMap.getProperty("values.$i", "")
						mapList.add(strMapTemp)
						strMap.append(strMapTemp)
						if(i<maxMap-1) strMap.append("\t")
					}

					val strCompressed = NetUtil.compressString("$strMap")
					log.debug("Map uncompressed:${strMap.length} compressed:"+strCompressed.length)

					msg += strCompressed
				}

				msg += "\n"

				txtPaneRoomChatLog.text = ""
				setRoomButtonsEnabled(false)
				tabLobbyAndRoom.setEnabledAt(1, true)
				tabLobbyAndRoom.selectedIndex = 1
				changeCurrentScreenCard(SCREENCARD_LOBBY)

				netPlayerClient!!.send(msg)
			} catch(e2:Exception) {
				log.error("Error on CreateRoom_OK", e2)
			}

		// Save Preset (Create Room)
		if(e.actionCommand=="CreateRoom_PresetSave") {
			val r = exportRoomInfoFromCreateRoomScreen()
			val id = spinnerCreateRoomPresetID.value as Int
			propConfig.setProperty("0.preset.$id", NetUtil.compressString(r!!.exportString()))
		}
		// Load Preset (Create Room)
		if(e.actionCommand=="CreateRoom_PresetLoad") {
			val id = spinnerCreateRoomPresetID.value as Int
			val strPresetC = propConfig.getProperty("0.preset.$id")
			if(strPresetC!=null) {
				val strPreset = NetUtil.decompressString(strPresetC)
				val r = NetRoomInfo(strPreset)
				importRoomInfoToCreateRoomScreen(r)
			}
		}
		// Preset code export (Create Room)
		if(e.actionCommand=="CreateRoom_PresetCodeExport") {
			val r = exportRoomInfoFromCreateRoomScreen()
			if(r==null)
				txtfldCreateRoomPresetCode.text = ""
			else
				txtfldCreateRoomPresetCode.text = NetUtil.compressString(r.exportString())
		}
		// Preset code import (Create Room)
		if(e.actionCommand=="CreateRoom_PresetCodeImport")
			try {
				var strPresetCode = txtfldCreateRoomPresetCode.text
				strPresetCode = strPresetCode.replace(Regex("[^a-zA-Z0-9+/=]"), "")
				if(strPresetCode.isNotEmpty()) {
					val strPresetCodeD = NetUtil.decompressString(strPresetCode)
					val r = NetRoomInfo(strPresetCodeD)
					importRoomInfoToCreateRoomScreen(r)
				}
			} catch(e2:Exception) {
				log.error("Failed to import preset code", e2)
			}

		// Participated in the creation screen room button
		if(e.actionCommand=="CreateRoom_Join") joinRoom(currentViewDetailRoomID, false)
		// Watch-created screens Room button
		if(e.actionCommand=="CreateRoom_Watch") joinRoom(currentViewDetailRoomID, true)
		// Room-created screensCancel button
		if(e.actionCommand=="CreateRoom_Cancel") {
			currentViewDetailRoomID = -1
			changeCurrentScreenCard(SCREENCARD_LOBBY)
		}
		// OK button (Create Room 1P)
		if(e.actionCommand=="CreateRoom1P_OK")
		//singleroomcreate\t[roomName]\t[mode]
			try {
				if(currentViewDetailRoomID!=-1)
					joinRoom(currentViewDetailRoomID, true)
				else if(listboxCreateRoom1PModeList.selectedIndex!=-1) {
					val strMode = listboxCreateRoom1PModeList.selectedValue as String
					var strRule = ""
					if(listboxCreateRoom1PRuleList.selectedIndex>=1)
						strRule = listboxCreateRoom1PRuleList.selectedValue as String

					txtPaneRoomChatLog.text = ""
					setRoomButtonsEnabled(false)
					tabLobbyAndRoom.setEnabledAt(1, true)
					tabLobbyAndRoom.selectedIndex = 1
					changeCurrentScreenCard(SCREENCARD_LOBBY)

					netPlayerClient!!.send("singleroomcreate\t${"\t${NetUtil.urlEncode(strMode)}\t"+NetUtil.urlEncode(strRule)}\n")
				}
			} catch(e2:Exception) {
				log.error("Error on CreateRoom1P_OK", e2)
			}

		// Cancel button (Create Room 1P)
		if(e.actionCommand=="CreateRoom1P_Cancel") {
			currentViewDetailRoomID = -1
			changeCurrentScreenCard(SCREENCARD_LOBBY)
		}
		// OK button (MPRanking)
		if(e.actionCommand=="MPRanking_OK") changeCurrentScreenCard(SCREENCARD_LOBBY)
		// Cancel button (Rule change)
		if(e.actionCommand=="RuleChange_Cancel") changeCurrentScreenCard(SCREENCARD_LOBBY)
		// OK button (Rule change)
		if(e.actionCommand=="RuleChange_OK") {
			// Set rules
			val strPrevTetrominoRuleFilename = propGlobal.rule[0][0].path

			for(i in 0..<GameEngine.MAX_GAMESTYLE) {
				val id = listboxRuleChangeRuleList[i].selectedIndex
				val subEntries = getSubsetEntries(i)
				val entry = if(id>=0) subEntries[id] else null
				propGlobal.rule[0][i] = entry?.let {RuleConf(it.path, it.file, it.name)} ?: RuleConf()
			}

			// Tuning
			propGlobal.tuning.firstOrNull()?.apply {
				spinDir = comboboxTuningSpinDirection.selectedIndex-1
				moveDiagonal = comboboxTuningMoveDiagonal.selectedIndex-1
				blockShowOutlineOnly = comboboxTuningBlockShowOutlineOnly.selectedIndex-1
				skin = comboboxTuningSkin.selectedIndex-2

				blockOutlineType = comboboxTuningBlockOutlineType.selectedIndex-1

				minDAS = getIntTextField(-1, txtfldTuningMinDAS)
				maxDAS = getIntTextField(-1, txtfldTuningMaxDAS)
				owARR = getIntTextField(-1, txtfldTuningDasDelay)
				reverseUpDown = chkboxTuningReverseUpDown.isSelected
			}

			// Save
			saveGlobalConfig()

			// Load rule
			val strFileName = propGlobal.rule[0][0].path
			if(strPrevTetrominoRuleFilename!=strFileName) {
				val propRule = CustomProperties().apply {
					try {
						val `in` = GZIPInputStream(FileInputStream(strFileName))
						load(`in`)
						`in`.close()
					} catch(_:Exception) {
					}
				}

				ruleOptPlayer = RuleOptions().apply {
					readProperty(propRule, 0)
				}

				// Send rule
				if(netPlayerClient!=null&&netPlayerClient!!.isConnected) sendMyRuleDataToServer()
			}

			changeCurrentScreenCard(SCREENCARD_LOBBY)
		}
	}

	/* Message reception */
	@Throws(IOException::class)
	override fun netOnMessage(client:NetBaseClient, message:List<String>) {
		//addSystemChatLog(getCurrentChatLogTextPane(), message[0], Color.green);

		// Connection completion
		if(message[0]=="welcome") {
			//welcome\t[VERSION]\t[PLAYERS]
			// Chat logFile creation
			if(writerLobbyLog==null)
				try {
					val currentTime = GregorianCalendar()
					val month = currentTime.get(Calendar.MONTH)+1
					val filename =
						String.format(
							"log/lobby_%04d_%02d_%02d_%02d_%02d_%02d.txt", currentTime.get(Calendar.YEAR), month,
							currentTime.get(Calendar.DATE), currentTime.get(Calendar.HOUR_OF_DAY), currentTime.get(Calendar.MINUTE),
							currentTime.get(Calendar.SECOND)
						)
					writerLobbyLog = PrintWriter(filename)
				} catch(e:Exception) {
					log.warn("Failed to create lobby log file", e)
				}

			if(writerRoomLog==null)
				try {
					val currentTime = GregorianCalendar()
					val month = currentTime.get(Calendar.MONTH)+1
					val filename =
						String.format(
							"log/room_%04d_%02d_%02d_%02d_%02d_%02d.txt", currentTime.get(Calendar.YEAR), month,
							currentTime.get(Calendar.DATE), currentTime.get(Calendar.HOUR_OF_DAY), currentTime.get(Calendar.MINUTE),
							currentTime.get(Calendar.SECOND)
						)
					writerRoomLog = PrintWriter(filename)
				} catch(e:Exception) {
					log.warn("Failed to create room log file", e)
				}

			val strTemp = String.format(getUIText("SysMsg_ServerConnected"), netPlayerClient!!.host, netPlayerClient!!.port)
			addSystemChatLogLater(txtpaneLobbyChatLog, strTemp, Color.blue)

			addSystemChatLogLater(txtpaneLobbyChatLog, getUIText("SysMsg_ServerVersion")+message[1], Color.blue)
			addSystemChatLogLater(txtpaneLobbyChatLog, getUIText("SysMsg_NumberOfPlayers")+message[2], Color.blue)
		}
		// Successful login
		if(message[0]=="loginsuccess") {
			addSystemChatLogLater(txtpaneLobbyChatLog, getUIText("SysMsg_LoginOK"), Color.blue)
			addSystemChatLogLater(
				txtpaneLobbyChatLog, getUIText("SysMsg_YourNickname")+convTripCode(NetUtil.urlDecode(message[1])),
				Color.blue
			)
			addSystemChatLogLater(txtpaneLobbyChatLog, getUIText("SysMsg_YourUID")+netPlayerClient!!.playerUID, Color.blue)

			addSystemChatLogLater(txtpaneLobbyChatLog, getUIText("SysMsg_SendRuleDataStart"), Color.blue)
			sendMyRuleDataToServer()
		}
		// Login failure
		if(message[0]=="loginfail") {
			setLobbyButtonsEnabled(0)

			if(message.size>1&&message[1]=="DIFFERENT_VERSION") {
				val strClientVer = GameManager.versionMajor.toString()
				val strServerVer = message[2]
				val strErrorMsg = String.format(getUIText("SysMsg_LoginFailDifferentVersion"), strClientVer, strServerVer)
				addSystemChatLogLater(txtpaneLobbyChatLog, strErrorMsg, Color.red)
			} else if(message.size>1&&message[1]=="DIFFERENT_BUILD") {
				val strClientBuildType = GameManager.buildTypeString
				val strServerBuildType = message[2]
				val strErrorMsg =
					String.format(getUIText("SysMsg_LoginFailDifferentBuild"), strClientBuildType, strServerBuildType)
				addSystemChatLogLater(txtpaneLobbyChatLog, strErrorMsg, Color.red)
			} else {
				val reason = StringBuilder()
				for(i in 1..<message.size)
					reason.append(message[i]).append(" ")
				addSystemChatLogLater(txtpaneLobbyChatLog, getUIText("SysMsg_LoginFail")+reason, Color.red)
			}
		}
		// Banned
		if(message[0]=="banned") {
			setLobbyButtonsEnabled(0)

			val cStart = GeneralUtil.importCalendarString(message[1])
			val cExpire = if(message.size>2&&message[2].isNotEmpty()) GeneralUtil.importCalendarString(message[2]) else null

			val strStart = cStart?.strDateTime ?: "???"
			val strExpire = cExpire?.strDateTime ?: getUIText("SysMsg_Banned_Permanent")

			addSystemChatLogLater(txtpaneLobbyChatLog, String.format(getUIText("SysMsg_Banned"), strStart, strExpire), Color.red)
		}
		// Rule dataTransmission success
		if(message[0]=="ruledatasuccess") {
			addSystemChatLogLater(txtpaneLobbyChatLog, getUIText("SysMsg_SendRuleDataOK"), Color.blue)

			// ListenerCall
			for(l in listeners!!)
				l.onLoginOK(this, netPlayerClient!!)
			if(netDummyMode!=null) netDummyMode!!.onLoginOK(this, netPlayerClient!!)

			setLobbyButtonsEnabled(1)
		}
		// Rule dataTransmission failure
		if(message[0]=="ruledatafail") sendMyRuleDataToServer()
		// Rule receive (for rule-locked games)
		if(message[0]=="rulelock") {
			if(ruleOptLock==null) ruleOptLock = RuleOptions()

			val strRuleData = NetUtil.decompressString(message[1])

			val prop = CustomProperties()
			prop.decode(strRuleData)
			ruleOptLock!!.readProperty(prop, 0)

			log.info("Received rule data (${ruleOptLock!!.strRuleName})")
		}
		// Rated-game rule list
		if(message[0]=="rulelist") {
			val style = message[1].toInt()

			if(style<listRatedRuleName.size) {
				listRatedRuleName[style].clear()

				for(i in 0..<message.size-2) {
					val name = NetUtil.urlDecode(message[2+i])
					listRatedRuleName[style].add(name)
				}
			}

			if(style==0) {
				listmodelCreateRoom1PRuleList.clear()
				listmodelCreateRoom1PRuleList.addElement(getUIText("CreateRoom1P_YourRule"))
				listboxCreateRoom1PRuleList.selectedIndex = 0

				for(i in 0..<listRatedRuleName[style].size) {
					val name = listRatedRuleName[style][i]
					listmodelCreateRoom1PRuleList.addElement(name)
				}

				listboxCreateRoom1PRuleList.setSelectedValue(
					propConfig.getProperty("createroom1p.listboxCreateRoom1PRuleList.value", ""), true
				)
			}
		}
		// PlayerList
		if(message[0]=="playerlist"||message[0]=="playerupdate"||
			message[0]=="playernew"||message[0]=="playerlogout") {
			SwingUtilities.invokeLater {updateLobbyUserList()}

			if(tabLobbyAndRoom.isEnabledAt(1)) {
				SwingUtilities.invokeLater {updateRoomUserList()}

				if(message[0]=="playerlogout") {
					val p = NetPlayerInfo(message[1])
					val p2 = netPlayerClient?.yourPlayerInfo
					if(p2!=null&&p.roomID==p2.roomID) {
						val strTemp:String = if(p.strHost.isNotEmpty())
							String.format(getUIText("SysMsg_LeaveRoomWithHost"), getPlayerNameWithTripCode(p), p.strHost)
						else
							String.format(getUIText("SysMsg_LeaveRoom"), getPlayerNameWithTripCode(p))
						addSystemChatLogLater(txtPaneRoomChatLog, strTemp, Color.blue)
					}
				}
			}
		}
		// PlayerEntering a room
		if(message[0]=="playerenter") {
			val uid = message[1].toInt()
			val pInfo = netPlayerClient?.getPlayerInfoByUID(uid)

			if(pInfo!=null) {
				val strTemp:String = if(pInfo.strHost.isNotEmpty())
					String.format(getUIText("SysMsg_EnterRoomWithHost"), getPlayerNameWithTripCode(pInfo), pInfo.strHost)
				else
					String.format(getUIText("SysMsg_EnterRoom"), getPlayerNameWithTripCode(pInfo))
				addSystemChatLogLater(txtPaneRoomChatLog, strTemp, Color.blue)
			}
		}
		// PlayerWithdrawal
		if(message[0]=="playerleave") {
			val uid = message[1].toInt()
			val pInfo = netPlayerClient?.getPlayerInfoByUID(uid)

			if(pInfo!=null) {
				val strTemp:String = if(pInfo.strHost.isNotEmpty())
					String.format(getUIText("SysMsg_LeaveRoomWithHost"), getPlayerNameWithTripCode(pInfo), pInfo.strHost)
				else
					String.format(getUIText("SysMsg_LeaveRoom"), getPlayerNameWithTripCode(pInfo))
				addSystemChatLogLater(txtPaneRoomChatLog, strTemp, Color.blue)
			}
		}
		// Change team
		if(message[0]=="changeteam") {
			val uid = message[1].toInt()
			val pInfo = netPlayerClient!!.getPlayerInfoByUID(uid)

			if(pInfo!=null) {
				val strTeam:String
				val strTemp:String

				if(message.size>3) {
					strTeam = NetUtil.urlDecode(message[3])
					strTemp = String.format(getUIText("SysMsg_ChangeTeam"), getPlayerNameWithTripCode(pInfo), strTeam)
				} else
					strTemp = String.format(getUIText("SysMsg_ChangeTeam_None"), getPlayerNameWithTripCode(pInfo))

				addSystemChatLogLater(currentChatLogTextPane, strTemp, Color.blue)
			}
		}
		// Room list
		if(message[0]=="roomlist") {
			val size = message[1].toInt()

			tablemodelRoomList.rowCount = 0
			for(i in 0..<size) {
				val r = NetRoomInfo(message[2+i])
				tablemodelRoomList.addRow(createRoomListRowData(r))
			}
		}
		// Receive presets
		if(message[0]=="ratedpresets")
			if(currentScreenCardNumber==SCREENCARD_CREATERATED_WAITING)
				if(message.size==1) {
					currentViewDetailRoomID = -1
					setCreateRoomUIType(false, null)
					changeCurrentScreenCard(SCREENCARD_CREATEROOM)
				} else {
					comboboxCreateRatedPresets.removeAllItems()
					var preset:String
					for(i in 1..<message.size) {
						preset = NetUtil.decompressString(message[i])
						val r = NetRoomInfo(preset)
						presets.add(r)
						comboboxCreateRatedPresets.addItem(r.strName)
					}
					changeCurrentScreenCard(SCREENCARD_CREATERATED)
				}
		// New room appearance
		if(message[0]=="roomcreate") {
			val r = NetRoomInfo(message[1])
			tablemodelRoomList.addRow(createRoomListRowData(r))
		}
		// Room information update
		if(message[0]=="roomupdate") {
			val r = NetRoomInfo(message[1])
			val columnID = tablemodelRoomList.findColumn(getUIText(ROOMTABLE_COLUMNNAMES[0]))

			for(i in 0..<tablemodelRoomList.rowCount) {
				val strID = tablemodelRoomList.getValueAt(i, columnID) as String
				val roomID = strID.toInt()

				if(roomID==r.roomID) {
					val rowData = createRoomListRowData(r)
					for(j in rowData.indices)
						tablemodelRoomList.setValueAt(rowData[j], i, j)
					break
				}
			}
		}
		// Annihilation Room
		if(message[0]=="roomdelete") {
			val r = NetRoomInfo(message[1])
			val columnID = tablemodelRoomList.findColumn(getUIText(ROOMTABLE_COLUMNNAMES[0]))

			for(i in 0..<tablemodelRoomList.rowCount) {
				val strID = tablemodelRoomList.getValueAt(i, columnID) as String
				val roomID = strID.toInt()

				if(roomID==r.roomID) {
					tablemodelRoomList.removeRow(i)
					break
				}
			}

			if(r.roomID==currentViewDetailRoomID&&currentScreenCardNumber==SCREENCARD_CREATEROOM)
				changeCurrentScreenCard(SCREENCARD_LOBBY)
		}
		// Successfully create and enter Room
		if(message[0]=="roomcreatesuccess"||message[0]=="roomjoinsuccess") {
			val roomID = message[1].toInt()
			val seatID = message[2].toInt()
			val queueID = message[3].toInt()

			netPlayerClient!!.yourPlayerInfo!!.roomID = roomID
			netPlayerClient!!.yourPlayerInfo!!.seatID = seatID
			netPlayerClient!!.yourPlayerInfo!!.queueID = queueID

			if(roomID!=-1) {
				val roomInfo = netPlayerClient!!.getRoomInfo(roomID)
				val pInfo = netPlayerClient!!.yourPlayerInfo

				if(seatID==-1&&queueID==-1) {
					val strTemp = String.format(getUIText("SysMsg_StatusChange_Spectator"), getPlayerNameWithTripCode(pInfo))
					addSystemChatLogLater(txtPaneRoomChatLog, strTemp, Color.blue)
					setRoomJoinButtonVisible(true)
				} else if(seatID==-1) {
					val strTemp = String.format(getUIText("SysMsg_StatusChange_Queue"), getPlayerNameWithTripCode(pInfo))
					addSystemChatLogLater(txtPaneRoomChatLog, strTemp, Color.blue)
					setRoomJoinButtonVisible(false)
				} else {
					val strTemp = String.format(getUIText("SysMsg_StatusChange_Joined"), getPlayerNameWithTripCode(pInfo))
					addSystemChatLogLater(txtPaneRoomChatLog, strTemp, Color.blue)
					setRoomJoinButtonVisible(false)
				}

				if(netPlayerClient!=null&&netPlayerClient!!.getRoomInfo(roomID)!=null)
					if(netPlayerClient!!.getRoomInfo(roomID)!!.singleplayer) {
						btnRoomButtonsJoin.isVisible = false
						btnRoomButtonsSitOut.isVisible = false
						btnRoomButtonsRanking.isVisible = false
						gameStatCardLayout.show(subPanelGameStat, "GameStat1P")
					} else {
						btnRoomButtonsRanking.isVisible = netPlayerClient!!.getRoomInfo(roomID)!!.rated
						gameStatCardLayout.show(subPanelGameStat, "GameStatMP")
					}

				SwingUtilities.invokeLater {
					setRoomButtonsEnabled(true)
					updateRoomUserList()
				}

				val strTitle = roomInfo!!.strName
				title = getUIText("Title_NetLobby")+" - "+strTitle
				tabLobbyAndRoom.setTitleAt(1, getUIText("Lobby_Tab_Room")+strTitle)

				addSystemChatLogLater(txtPaneRoomChatLog, getUIText("SysMsg_RoomJoin_Title")+strTitle, Color.blue)
				addSystemChatLogLater(txtPaneRoomChatLog, getUIText("SysMsg_RoomJoin_ID")+roomInfo.roomID, Color.blue)
				if(roomInfo.ruleLock)
					addSystemChatLogLater(txtPaneRoomChatLog, getUIText("SysMsg_RoomJoin_Rule")+roomInfo.ruleName, Color.blue)

				setLobbyButtonsEnabled(2)
				changeCurrentScreenCard(SCREENCARD_LOBBY)

				// ListenerCall
				for(l in listeners!!)
					l.onRoomJoin(this, netPlayerClient!!, roomInfo)
				if(netDummyMode!=null) netDummyMode!!.onRoomJoin(this, netPlayerClient!!, roomInfo)
			} else {
				addSystemChatLogLater(txtPaneRoomChatLog, getUIText("SysMsg_RoomJoin_Lobby"), Color.blue)

				title = getUIText("Title_NetLobby")
				tabLobbyAndRoom.selectedIndex = 0
				tabLobbyAndRoom.setEnabledAt(1, false)
				tabLobbyAndRoom.setTitleAt(1, getUIText("Lobby_Tab_NoRoom"))

				setLobbyButtonsEnabled(1)
				changeCurrentScreenCard(SCREENCARD_LOBBY)

				// ListenerCall
				for(l in listeners!!)
					l.onRoomLeave(this, netPlayerClient!!)
				if(netDummyMode!=null) netDummyMode!!.onRoomLeave(this, netPlayerClient!!)
			}
		}
		// Entry Room failure
		if(message[0]=="roomjoinfail")
			addSystemChatLogLater(txtPaneRoomChatLog, getUIText("SysMsg_RoomJoinFail"), Color.red)
		// Kicked from a room
		if(message[0]=="roomkicked") {
			val strKickMsg = String.format(getUIText("SysMsg_Kicked_"+message[1]), NetUtil.urlDecode(message[3]), message[2])
			addSystemChatLogLater(txtpaneLobbyChatLog, strKickMsg, Color.red)
		}
		// Map receive
		if(message[0]=="values") {
			val strDecompressed = NetUtil.decompressString(message[1])
			val strMaps = strDecompressed.split(Regex("\t")).dropLastWhile {it.isEmpty()}
			mapList.clear()
			mapList.addAll(strMaps)

			log.debug("Received ${mapList.size} maps")
		}
		// Lobby chat
		if(message[0]=="lobbychat") {
			val uid = message[1].toInt()
			val pInfo = netPlayerClient?.getPlayerInfoByUID(uid)

			if(pInfo!=null) {
				val calendar = GeneralUtil.importCalendarString(message[3])
				val strMsgBody = NetUtil.urlDecode(message[4])
				addUserChatLogLater(txtpaneLobbyChatLog, getPlayerNameWithTripCode(pInfo), calendar, strMsgBody)
			}
		}
		// Room chat
		if(message[0]=="chat") {
			val uid = message[1].toInt()
			val pInfo = netPlayerClient?.getPlayerInfoByUID(uid)

			if(pInfo!=null) {
				val calendar = GeneralUtil.importCalendarString(message[3])
				val strMsgBody = NetUtil.urlDecode(message[4])
				addUserChatLogLater(txtPaneRoomChatLog, getPlayerNameWithTripCode(pInfo), calendar, strMsgBody)
			}
		}
		// Lobby chat/Room chat (history)
		if(message[0]=="lobbychath"||message[0]=="chath") {
			val strUsername = convTripCode(NetUtil.urlDecode(message[1]))
			val calendar = GeneralUtil.importCalendarString(message[2])
			val strMsgBody = NetUtil.urlDecode(message[3])
			val txtpane = if(message[0]=="lobbychath") txtpaneLobbyChatLog else txtPaneRoomChatLog
			addRecordedUserChatLogLater(txtpane, strUsername, calendar, strMsgBody)
		}
		// Participation status change
		if(message[0]=="changestatus") {
			val uid = message[2].toInt()
			val pInfo = netPlayerClient!!.getPlayerInfoByUID(uid)

			if(pInfo!=null)
				if(message[1]=="watchonly") {
					val strTemp = String.format(getUIText("SysMsg_StatusChange_Spectator"), getPlayerNameWithTripCode(pInfo))
					addSystemChatLogLater(txtPaneRoomChatLog, strTemp, Color.blue)
					if(uid==netPlayerClient!!.playerUID) setRoomJoinButtonVisible(true)
				} else if(message[1]=="joinqueue") {
					val strTemp = String.format(getUIText("SysMsg_StatusChange_Queue"), getPlayerNameWithTripCode(pInfo))
					addSystemChatLogLater(txtPaneRoomChatLog, strTemp, Color.blue)
					if(uid==netPlayerClient!!.playerUID) setRoomJoinButtonVisible(false)
				} else if(message[1]=="joinseat") {
					val strTemp = String.format(getUIText("SysMsg_StatusChange_Joined"), getPlayerNameWithTripCode(pInfo))
					addSystemChatLogLater(txtPaneRoomChatLog, strTemp, Color.blue)
					if(uid==netPlayerClient!!.playerUID) setRoomJoinButtonVisible(false)
				}

			SwingUtilities.invokeLater {updateRoomUserList()}
		}
		// Automatically start timerStart
		if(message[0]=="autostartbegin") {
			val strTemp = String.format(getUIText("SysMsg_AutoStartBegin"), message[1])
			addSystemChatLogLater(txtPaneRoomChatLog, strTemp, Color(64, 128, 0))
		}
		// game start
		if(message[0]=="start") {
			addSystemChatLogLater(txtPaneRoomChatLog, getUIText("SysMsg_GameStart"), Color(0, 128, 0))
			tableModelGameStat.rowCount = 0
			tableModelGameStat1P.rowCount = 0

			if(netPlayerClient!!.yourPlayerInfo!!.seatID!=-1) {
				btnRoomButtonsSitOut.isEnabled = false
				btnRoomButtonsTeamChange.isEnabled = false
				itemLobbyMenuTeamChange.isEnabled = false
				roomTopBarCardLayout.first(subPanelRoomTopBar)
			}
		}
		// Death
		if(message[0]=="dead") {
			val uid = message[1].toInt()
			val name = convTripCode(NetUtil.urlDecode(message[2]))

			if(message.size>6) {
				val strTemp = String.format(getUIText("SysMsg_KO"), convTripCode(NetUtil.urlDecode(message[6])), name)
				addSystemChatLogLater(txtPaneRoomChatLog, strTemp, Color(0, 128, 0))
			}

			if(uid==netPlayerClient!!.playerUID) {
				btnRoomButtonsSitOut.isEnabled = true
				btnRoomButtonsTeamChange.isEnabled = true
				itemLobbyMenuTeamChange.isEnabled = true
			}
		}
		// Game stats (Multiplayer)
		if(message[0]=="gstat") {
			val rowdata = arrayOfNulls<String>(13)
			val myRank = message[4].toInt()

			rowdata[0] = "$myRank" // Rank
			rowdata[1] = convTripCode(NetUtil.urlDecode(message[3])) // Name
			rowdata[2] = message[5] // Attack count
			rowdata[3] = message[6] // APL
			rowdata[4] = message[7] // APM
			rowdata[5] = message[8] // Line count
			rowdata[6] = message[9] // LPM
			rowdata[7] = message[10] // Piece count
			rowdata[8] = message[11] // PPS
			rowdata[9] = message[12].toInt().toTimeStr //  Time
			rowdata[10] = message[13] // KO
			rowdata[11] = message[14] // Win
			rowdata[12] = message[15] // Games

			var insertPos = 0
			for(i in 0..<tableModelGameStat.rowCount) {
				val strRank = tableModelGameStat.getValueAt(i, 0) as String
				val rank = strRank.toInt()

				if(myRank>rank) insertPos = i+1
			}

			tableModelGameStat.insertRow(insertPos, rowdata)

			if(writerRoomLog!=null) {
				writerRoomLog!!.print("[$currentTimeAsString] ")

				for(i in rowdata.indices) {
					writerRoomLog!!.print(rowdata[i])
					if(i<rowdata.size-1)
						writerRoomLog!!.print(",")
					else
						writerRoomLog!!.print("\n")
				}

				writerRoomLog!!.flush()
			}
		}
		// Game stats (Single player)
		if(message[0]=="gstat1p") {
			val strRowData = NetUtil.urlDecode(message[1])
			val rowData = strRowData.split(Regex("\t")).dropLastWhile {it.isEmpty()}

			if(writerRoomLog!=null) writerRoomLog!!.print("[$currentTimeAsString]\n")

			tableModelGameStat1P.rowCount = 0
			for(element in rowData) {
				val strTempArray = element.split(Regex(";")).dropLastWhile {it.isEmpty()}
				tableModelGameStat1P.addRow(strTempArray.toTypedArray())

				if(writerRoomLog!=null&&strTempArray.size>1)
					writerRoomLog!!.print(
						" ${strTempArray[0]}:${strTempArray[1]}\n"
					)
			}

			if(writerRoomLog!=null) writerRoomLog!!.flush()
		}
		// game finished
		if(message[0]=="finish") {
			addSystemChatLogLater(txtPaneRoomChatLog, getUIText("SysMsg_GameEnd"), Color(0, 128, 0))

			if(message.size>3&&message[3].isNotEmpty()) {
				var flagTeamWin = false
				if(message.size>4) flagTeamWin = message[4].toBoolean()

				val strWinner:String = if(flagTeamWin)
					String.format(getUIText("SysMsg_WinnerTeam"), NetUtil.urlDecode(message[3]))
				else
					String.format(getUIText("SysMsg_Winner"), convTripCode(NetUtil.urlDecode(message[3])))
				addSystemChatLogLater(txtPaneRoomChatLog, strWinner, Color(0, 128, 0))
			}

			btnRoomButtonsSitOut.isEnabled = true
			btnRoomButtonsTeamChange.isEnabled = true
			itemLobbyMenuTeamChange.isEnabled = true
		}
		// Rating change
		if(message[0]=="rating") {
			val strPlayerName = convTripCode(NetUtil.urlDecode(message[3]))
			val ratingNow = message[4].toInt()
			val ratingChange = message[5].toInt()
			val strTemp = String.format(getUIText("SysMsg_Rating"), strPlayerName, ratingNow, ratingChange)
			addSystemChatLogLater(txtPaneRoomChatLog, strTemp, Color(0, 128, 0))
		}
		// Multiplayer Leaderboard
		if(message[0]=="mpranking") {
			val style = message[1].toInt()
			val myRank = message[2].toInt()

			tablemodelMPRanking[style].rowCount = 0

			val strPData = NetUtil.decompressString(message[3])
			val strPDataA = strPData.split(Regex("\t")).dropLastWhile {it.isEmpty()}

			for(element in strPDataA) {
				val strRankData = element.split(Regex(";")).dropLastWhile {it.isEmpty()}
				val strRowData = arrayOfNulls<String>(MPRANKING_COLUMNNAMES.size)
				val rank = strRankData[0].toInt()
				if(rank==-1)
					strRowData[0] = "N/A"
				else
					strRowData[0] = (rank+1).toString()
				strRowData[1] = convTripCode(NetUtil.urlDecode(strRankData[1]))
				strRowData[2] = strRankData[2]
				strRowData[3] = strRankData[3]
				strRowData[4] = strRankData[4]
				tablemodelMPRanking[style].addRow(strRowData)
			}

			if(myRank==-1) {
				val tableRowMax = tablemodelMPRanking[style].rowCount
				tableMPRanking[style].selectionModel.setSelectionInterval(tableRowMax-1, tableRowMax-1)
			} else
				tableMPRanking[style].selectionModel.setSelectionInterval(myRank, myRank)
		}
		// Announcement from the admin
		if(message[0]=="announce") {
			val strTime = currentTimeAsString
			val strMessage = "[$strTime]<ADMIN>:"+NetUtil.urlDecode(message[1])
			addSystemChatLogLater(currentChatLogTextPane, strMessage, Color(255, 32, 0))
		}
		// Single player replay download
		if(message[0]=="spdownload") {
			val sChecksum = message[1].toLong()
			val checksumObj = Adler32()
			checksumObj.update(NetUtil.stringToBytes(message[2]))

			if(checksumObj.value==sChecksum) {
				val strReplay = NetUtil.decompressString(message[2])
				val prop = CustomProperties()
				prop.decode(strReplay)

				try {
					val out = GZIPOutputStream(FileOutputStream("replay/netreplay.rep"))
					prop.store(out, "NullpoMino NetReplay from "+netPlayerClient!!.host!!)
					addSystemChatLog(currentChatLogTextPane, getUIText("SysMsg_ReplaySaved"), Color.magenta)
				} catch(e:IOException) {
					log.error("Failed to write replay to replay/netreplay.rep", e)
				}
			}
		}

		// ListenerCall
		if(listeners!=null)
			for(l in listeners!!)
				l.onMessage(this, netPlayerClient!!, message)
		if(netDummyMode!=null) netDummyMode!!.onMessage(this, netPlayerClient!!, message)
	}

	/* When it is cut */
	override fun netOnDisconnect(client:NetBaseClient, ex:Throwable?) {
		SwingUtilities.invokeLater {
			setLobbyButtonsEnabled(0)
			setRoomButtonsEnabled(false)
			tablemodelRoomList.rowCount = 0
		}

		if(ex!=null) {
			addSystemChatLogLater(
				currentChatLogTextPane, getUIText("SysMsg_DisconnectedError")+"\n"
					+ex.localizedMessage, Color.red
			)
			log.info("Server Disconnected", ex)
		} else {
			addSystemChatLogLater(currentChatLogTextPane, getUIText("SysMsg_DisconnectedOK"), Color(128, 0, 0))
			log.info("Server Disconnected (null)")
		}

		// ListenerCall
		if(listeners!=null)
			for(l in listeners!!)
				l.onDisconnect(this, netPlayerClient!!, ex)
		if(netDummyMode!=null) netDummyMode!!.onDisconnect(this, netPlayerClient!!, ex)
	}

	/** Add a new NetLobbyListener, but don't add NetDummyMode!
	 * @param l A NetLobbyListener to add
	 */
	fun addListener(l:NetLobbyListener) {
		listeners!!.add(l)
	}

	/** Remove a NetLobbyListener from the listeners list
	 * @param l NetLobbyListener to remove
	 * @return true if removed, false if not found or already removed
	 */
	fun removeListener(l:NetLobbyListener):Boolean = listeners!!.remove(l)

	/** Text input Pop-up for the fieldMenu
	 * [Exhibit](http://terai.xrea.jp/Swing/DefaultEditorKit.html) */
	private inner class TextComponentPopupMenu(field:JTextComponent):JPopupMenu() {
		private val cutAction:Action = object:AbstractAction(getUIText("Popup_Cut")) {
			override fun actionPerformed(evt:ActionEvent) {
				field.cut()
			}
		}
		private val copyAction:Action = object:AbstractAction(getUIText("Popup_Copy")) {
			override fun actionPerformed(evt:ActionEvent) {
				field.copy()
			}
		}
		private val pasteAction:Action = object:AbstractAction(getUIText("Popup_Paste")) {
			override fun actionPerformed(evt:ActionEvent) {
				field.paste()
			}
		}
		private val deleteAction:Action = object:AbstractAction(getUIText("Popup_Delete")) {
			override fun actionPerformed(evt:ActionEvent) {
				field.replaceSelection(null)
			}
		}
		private val selectAllAction:Action = object:AbstractAction(getUIText("Popup_SelectAll")) {
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
			val field = c as JTextComponent
			val flg = field.selectedText!=null
			cutAction.isEnabled = flg
			copyAction.isEnabled = flg
			deleteAction.isEnabled = flg
			selectAllAction.isEnabled = field.isFocusOwner
			super.show(c, x, y)
		}
	}

	/** Pop-up box for the listMenu */
	private inner class ListBoxPopupMenu(private val listbox:JList<*>?):JPopupMenu() {
		private val copyAction:Action = object:AbstractAction(getUIText("Popup_Copy")) {
			override fun actionPerformed(e:ActionEvent) {
				if(listbox==null) return
				val selectedObj = listbox.selectedValue

				if(selectedObj!=null&&selectedObj is String) {
					val ss = StringSelection(selectedObj)
					val clipboard = Toolkit.getDefaultToolkit().systemClipboard
					clipboard.setContents(ss, ss)
				}
			}
		}

		init {
			add(copyAction)
		}

		override fun show(c:Component, x:Int, y:Int) {
			if(listbox!!.selectedIndex!=-1) {
				copyAction.isEnabled = true
				super.show(c, x, y)
			}
		}
	}

	/** Pop-up list box for server selectionMenu */
	private inner class ServerSelectListBoxPopupMenu:JPopupMenu() {
		private val connectAction:Action = object:AbstractAction(getUIText("Popup_ServerSelect_Connect")) {
			override fun actionPerformed(e:ActionEvent) {
				serverSelectConnectButtonClicked()
			}
		}
		private val deleteAction:Action = object:AbstractAction(getUIText("Popup_ServerSelect_Delete")) {
			override fun actionPerformed(e:ActionEvent) {
				serverSelectDeleteButtonClicked()
			}
		}
		private val setObserverAction:Action = object:AbstractAction(getUIText("Popup_ServerSelect_SetObserver")) {
			override fun actionPerformed(e:ActionEvent) {
				serverSelectSetObserverButtonClicked()
			}
		}

		init {
			add(connectAction)
			add(deleteAction)
			add(setObserverAction)
		}

		override fun show(c:Component, x:Int, y:Int) {
			if(listboxServerList.selectedIndex!=-1) super.show(c, x, y)
		}
	}

	/** For server selection list boxMouseAdapter */
	private inner class ServerSelectListBoxMouseAdapter:MouseAdapter() {
		override fun mouseClicked(e:MouseEvent?) {
			if(e!!.clickCount==2&&e.button==MouseEvent.BUTTON1) serverSelectConnectButtonClicked()
		}
	}

	/** Room list tablePop-up forMenu */
	private inner class RoomTablePopupMenu:JPopupMenu() {
		private val joinAction:Action = object:AbstractAction(getUIText("Popup_RoomTable_Join")) {
			override fun actionPerformed(evt:ActionEvent) {
				val row = tableRoomList.selectedRow
				if(row!=-1) {
					val columnID = tablemodelRoomList.findColumn(getUIText(ROOMTABLE_COLUMNNAMES[0]))
					val strRoomID = tablemodelRoomList.getValueAt(row, columnID) as String
					val roomID = strRoomID.toInt()
					joinRoom(roomID, false)
				}
			}
		}
		private val watchAction:Action = object:AbstractAction(getUIText("Popup_RoomTable_Watch")) {
			override fun actionPerformed(evt:ActionEvent) {
				val row = tableRoomList.selectedRow
				if(row!=-1) {
					val columnID = tablemodelRoomList.findColumn(getUIText(ROOMTABLE_COLUMNNAMES[0]))
					val strRoomID = tablemodelRoomList.getValueAt(row, columnID) as String
					val roomID = strRoomID.toInt()
					joinRoom(roomID, true)
				}
			}
		}
		private val detailAction:Action = object:AbstractAction(getUIText("Popup_RoomTable_Detail")) {
			override fun actionPerformed(evt:ActionEvent) {
				val row = tableRoomList.selectedRow
				if(row!=-1) {
					val columnID = tablemodelRoomList.findColumn(getUIText(ROOMTABLE_COLUMNNAMES[0]))
					val strRoomID = tablemodelRoomList.getValueAt(row, columnID) as String
					val roomID = strRoomID.toInt()
					viewRoomDetail(roomID)
				}
			}
		}

		init {
			add(joinAction)
			add(watchAction)
			add(detailAction)
		}

		override fun show(c:Component, x:Int, y:Int) {
			if(tableRoomList.selectedRow!=-1) {
				joinAction.isEnabled = true
				watchAction.isEnabled = true
				detailAction.isEnabled = true
				super.show(c, x, y)
			}
		}
	}

	/** Room list tableUseMouseAdapter */
	private inner class RoomTableMouseAdapter:MouseAdapter() {
		override fun mouseClicked(e:MouseEvent?) {
			if(e!!.clickCount==2&&e.button==MouseEvent.BUTTON1) {
				val pt = e.point
				val row = tableRoomList.rowAtPoint(pt)

				if(row!=-1) {
					val columnID = tablemodelRoomList.findColumn(getUIText(ROOMTABLE_COLUMNNAMES[0]))
					val strRoomID = tablemodelRoomList.getValueAt(row, columnID) as String
					val roomID = strRoomID.toInt()
					joinRoom(roomID, false)
				}
			}
		}
	}

	/** Room list tableUseKeyAdapter */
	private inner class RoomTableKeyAdapter:KeyAdapter() {
		override fun keyPressed(e:KeyEvent?) {
			if(e!!.keyCode==KeyEvent.VK_ENTER) e.consume()
		}

		override fun keyReleased(e:KeyEvent?) {
			if(e!!.keyCode==KeyEvent.VK_ENTER) {
				val row = tableRoomList.selectedRow
				if(row!=-1) {
					val columnID = tablemodelRoomList.findColumn(getUIText(ROOMTABLE_COLUMNNAMES[0]))
					val strRoomID = tablemodelRoomList.getValueAt(row, columnID) as String
					val roomID = strRoomID.toInt()
					joinRoom(roomID, false)
				}
				e.consume()
			}
		}
	}

	/** Pop-up display field for logMenu */
	private inner class LogPopupMenu(field:JTextComponent):JPopupMenu() {
		private val copyAction:Action = object:AbstractAction(getUIText("Popup_Copy")) {
			override fun actionPerformed(evt:ActionEvent) {
				field.copy()
			}
		}
		private val selectAllAction:Action = object:AbstractAction(getUIText("Popup_SelectAll")) {
			override fun actionPerformed(evt:ActionEvent) {
				field.selectAll()
			}
		}
		private val clearAction:Action = object:AbstractAction(getUIText("Popup_Clear")) {
			override fun actionPerformed(evt:ActionEvent) {
				field.text = null
			}
		}

		init {
			add(copyAction)
			add(selectAllAction)
			add(clearAction)
		}

		override fun show(c:Component, x:Int, y:Int) {
			val field = c as JTextComponent
			val flg = field.selectedText!=null
			copyAction.isEnabled = flg
			selectAllAction.isEnabled = field.isFocusOwner
			super.show(c, x, y)
		}
	}

	/** Display field for logKeyAdapter */
	private class LogKeyAdapter:KeyAdapter() {
		override fun keyPressed(e:KeyEvent?) {
			if(e!!.keyCode!=KeyEvent.VK_UP&&e.keyCode!=KeyEvent.VK_DOWN&&
				e.keyCode!=KeyEvent.VK_LEFT&&e.keyCode!=KeyEvent.VK_RIGHT&&
				e.keyCode!=KeyEvent.VK_HOME&&e.keyCode!=KeyEvent.VK_END&&
				e.keyCode!=KeyEvent.VK_PAGE_UP&&e.keyCode!=KeyEvent.VK_PAGE_DOWN&&
				(e.keyCode!=KeyEvent.VK_A||!e.isControlDown)&&
				(e.keyCode!=KeyEvent.VK_C||!e.isControlDown)&&
				!e.isAltDown)
				e.consume()
		}

		override fun keyTyped(e:KeyEvent?) {
			e!!.consume()
		}
	}

	/** Popup menu for any table */
	private inner class TablePopupMenu(table:JTable):JPopupMenu() {
		private val copyAction:Action = object:AbstractAction(getUIText("Popup_Copy")) {
			override fun actionPerformed(e:ActionEvent) {
				val row = table.selectedRow

				if(row!=-1) {
					val strCopy = StringBuilder()

					for(column in 0..<table.columnCount) {
						val selectedObject = table.getValueAt(row, column)
						if(selectedObject is String)
							if(column==0)
								strCopy.append(selectedObject)
							else
								strCopy.append(",").append(selectedObject)
					}

					val ss = StringSelection("$strCopy")
					val clipboard = Toolkit.getDefaultToolkit().systemClipboard
					clipboard.setContents(ss, ss)
				}
			}
		}

		init {
			add(copyAction)
		}

		override fun show(c:Component, x:Int, y:Int) {
			val table = c as JTable
			val flg = table.selectedRow!=-1
			copyAction.isEnabled = flg
			super.show(c, x, y)
		}
	}

	/** Each label of Image Combobox<br></br>
	 * [Source](http://www.javadrive.jp/tutorial/jcombobox/index20.html) */
	private class ComboLabel(var text:String = "", var icon:Icon? = null)

	/** ListCellRenderer for Image Combobox<br></br>
	 * [Source](http://www.javadrive.jp/tutorial/jcombobox/index20.html) */
	private inner class ComboLabelCellRenderer:JLabel(), ListCellRenderer<Any> {
		init {
			isOpaque = true
		}

		override fun getListCellRendererComponent(list:JList<out Any>?, value:Any?, index:Int, isSelected:Boolean,
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
		/** Room-table column names. These strings will be passed to
		 * getUIText(String) subroutine. */
		val ROOMTABLE_COLUMNNAMES =
			arrayOf(
				"RoomTable_ID", "RoomTable_Name", "RoomTable_Rated", "RoomTable_RuleName", "RoomTable_ModeName",
				"RoomTable_Status", "RoomTable_Players", "RoomTable_Spectators"
			)

		/** End-of-game statistics column names. These strings will be passed to
		 * getUIText(String) subroutine. */
		val STATTABLE_COLUMNNAMES =
			arrayOf(
				"StatTable_Rank", "StatTable_Name", "StatTable_Attack", "StatTable_APL", "StatTable_APM", "StatTable_Lines",
				"StatTable_LPM", "StatTable_Piece", "StatTable_PPS", "StatTable_Time", "StatTable_KO", "StatTable_Wins",
				"StatTable_Games"
			)

		/** 1P end-of-game statistics column names. These strings will be passed to
		 * getUIText(String) subroutine. */
		val STATTABLE1P_COLUMNNAMES = arrayOf("StatTable1P_Description", "StatTable1P_Value")

		/** Multiplayer leaderboard column names. These strings will be passed to
		 * getUIText(String) subroutine. */
		val MPRANKING_COLUMNNAMES =
			arrayOf("MPRanking_Rank", "MPRanking_Name", "MPRanking_Rating", "MPRanking_PlayCount", "MPRanking_WinCount")

		/** Tuning: Generic Auto/Disable/Enable labels (before translation) */
		val TUNING_COMBOBOX_GENERIC =
			arrayOf("GameTuning_ComboboxGeneric_Auto", "GameTuning_ComboboxGeneric_Disable", "GameTuning_ComboboxGeneric_Enable")

		/** Tuning: A button rotation (before translation) */
		val TUNING_ABUTTON_SPIN =
			arrayOf(
				"GameTuning_RotateButtonDefaultRight_Auto", "GameTuning_RotateButtonDefaultRight_Left",
				"GameTuning_RotateButtonDefaultRight_Right"
			)

		/** Tuning: Outline type names (before translation) */
		val TUNING_OUTLINE_TYPE_NAMES =
			arrayOf(
				"GameTuning_OutlineType_Auto", "GameTuning_OutlineType_None", "GameTuning_OutlineType_Normal",
				"GameTuning_OutlineType_Connect", "GameTuning_OutlineType_SameColor"
			)

		/** Spin bonus names */
		val COMBOBOX_SPINBONUS_NAMES = arrayOf("CreateRoom_Twist_Disable", "CreateRoom_Twist_TOnly", "CreateRoom_Twist_All")

		/** Names for spin check types */
		val COMBOBOX_SPINCHECKTYPE_NAMES = arrayOf("CreateRoom_SpinCheck_4Point", "CreateRoom_SpinCheck_Immobile")

		/** Constants for each screen-card */
		const val SCREENCARD_SERVERSELECT = 0
		private const val SCREENCARD_LOBBY = 1
		const val SCREENCARD_SERVERADD = 2
		const val SCREENCARD_CREATERATED_WAITING = 3
		const val SCREENCARD_CREATERATED = 4
		const val SCREENCARD_CREATEROOM = 5
		const val SCREENCARD_CREATEROOM1P = 6
		const val SCREENCARD_MPRANKING = 7
		const val SCREENCARD_RULECHANGE = 8

		/** Names for each screen-card */
		val SCREENCARD_NAMES =
			arrayOf(
				"ServerSelect", "Lobby", "ServerAdd", "CreateRatedWaiting", "CreateRated", "CreateRoom", "CreateRoom1P",
				"MPRanking", "RuleChange"
			)

		/** Log */
		internal val log = LogManager.getLogger()

		/** Get int value from JTextField[field]
		 * @param value Default Value (used if conversion fails)
		 * @return int value (or default value if fails)
		 */
		fun getIntTextField(value:Int, field:JTextField):Int = try {
			field.text.toInt()
		} catch(e:NumberFormatException) {
			value
		}

		/** Main function count
		 * @param args CommandLines Argument count
		 */
		@JvmStatic fun main(args:Array<String>) {
			org.apache.logging.log4j.core.config.Configurator.initialize(log.name, "config/etc/log.xml")
			val frame = NetLobbyFrame()
			frame.init()
			frame.isVisible = true
		}
	}
}
