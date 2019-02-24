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
package mu.nu.nullpo.gui.net

import mu.nu.nullpo.game.component.RuleOptions
import mu.nu.nullpo.game.net.*
import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.game.play.GameManager
import mu.nu.nullpo.game.subsystem.mode.NetDummyMode
import mu.nu.nullpo.util.CustomProperties
import mu.nu.nullpo.util.GeneralUtil
import org.apache.log4j.Logger
import org.apache.log4j.PropertyConfigurator
import java.awt.*
import java.awt.datatransfer.StringSelection
import java.awt.event.*
import java.awt.image.BufferedImage
import java.io.*
import java.net.MalformedURLException
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.Adler32
import java.util.zip.GZIPOutputStream
import javax.imageio.ImageIO
import javax.swing.*
import javax.swing.table.DefaultTableModel
import javax.swing.text.*

/** NullpoMino NetLobby */
/** Constructor */
class NetLobbyFrame:JFrame(), ActionListener, NetMessageListener {

	/** NetPlayerClient */
	var netPlayerClient:NetPlayerClient? = null

	/** Rule data */
	var ruleOptPlayer:RuleOptions? = null
	var ruleOptLock:RuleOptions? = null

	/** Map list */
	var mapList:LinkedList<String> = LinkedList()

	/** Event listeners */
	private var listeners:LinkedList<NetLobbyListener>? = LinkedList()

	/** Preset info */
	private val presets = LinkedList<NetRoomInfo>()

	/** Current game mode (act as special NetLobbyListener) */
	var netDummyMode:NetDummyMode? = null

	/** Property file for lobby settings */
	private val propConfig:CustomProperties = CustomProperties()

	/** Property file for global settings */
	private val propGlobal:CustomProperties = CustomProperties()

	/** Property file for swing settings */
	private val propSwingConfig:CustomProperties = CustomProperties()

	/** Property file for observer ("Watch") settings */
	private val propObserver:CustomProperties = CustomProperties()

	/** Default game mode description file */
	private val propDefaultModeDesc:CustomProperties = CustomProperties()

	/** Game mode description file */
	private val propModeDesc:CustomProperties = CustomProperties()

	/** Default language file */
	private val propLangDefault:CustomProperties = CustomProperties()

	/** Property file for GUI translations */
	private val propLang:CustomProperties = CustomProperties()

	/** Current screen-card number */
	private var currentScreenCardNumber:Int = 0

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
	private lateinit var listRatedRuleName:Array<LinkedList<String>>

	/** Layout manager for main screen */
	private lateinit var contentPaneCardLayout:CardLayout

	/** Menu bars (all screens) */
	private lateinit var menuBar:Array<JMenuBar>

	/** Text field of player name (Server select screen) */
	private lateinit var txtfldPlayerName:JTextField

	/** Text field of team name (Server select screen) */
	private lateinit var txtfldPlayerTeam:JTextField

	/** Listbox for servers (Server select screen) */
	private lateinit var listboxServerList:JList<*>

	/** Listbox data for servers (Server select screen) */
	private lateinit var listmodelServerList:DefaultListModel<String>

	/** Connect button (Server select screen) */
	private lateinit var btnServerConnect:JButton

	/** Lobby/Room Tab */
	private lateinit var tabLobbyAndRoom:JTabbedPane

	/** JSplitPane (Lobby screen) */
	private var splitLobby:JSplitPane? = null

	/** At the top of the lobby screen layout manager */
	private lateinit var roomListTopBarCardLayout:CardLayout

	/** Panel at the top of the lobby screen */
	private lateinit var subpanelRoomListTopBar:JPanel

	/** Lobby popup menu (Lobby screen) */
	private lateinit var popupLobbyOptions:JPopupMenu

	/** Rule change menu item (Lobby screen) */
	private lateinit var itemLobbyMenuRuleChange:JMenuItem

	/** Team change menu item (Lobby screen) */
	private lateinit var itemLobbyMenuTeamChange:JMenuItem

	/** Leaderboard menu item (Lobby screen) */
	private lateinit var itemLobbyMenuRanking:JMenuItem

	/** Quick Start button(Lobby screen) */
	private lateinit var btnRoomListQuickStart:JButton

	/** Create a Room button(Lobby screen) */
	private lateinit var btnRoomListRoomCreate:JButton

	/** Create Room 1P (Lobby screen) */
	private lateinit var btnRoomListRoomCreate1P:JButton

	/** Options menu button (Lobby screen) */
	private lateinit var btnRoomListOptions:JButton

	/** Team name input Column(Lobby screen) */
	private lateinit var txtfldRoomListTeam:JTextField

	/** Room list table */
	private lateinit var tableRoomList:JTable

	/** Room list tableのカラム名(翻訳後) */
	private lateinit var strTableColumnNames:Array<String>

	/** Room list tableの data */
	private lateinit var tablemodelRoomList:DefaultTableModel

	/** Chat logAndPlayerPartition line of the list(Lobby screen) */
	private var splitLobbyChat:JSplitPane? = null

	/** Chat log(Lobby screen) */
	private lateinit var txtpaneLobbyChatLog:JTextPane

	/** PlayerList(Lobby screen) */
	private lateinit var listboxLobbyChatPlayerList:JList<*>

	/** PlayerList(Lobby screen)Of data */
	private lateinit var listmodelLobbyChatPlayerList:DefaultListModel<String>

	/** Chat input Column(Lobby screen) */
	private lateinit var txtfldLobbyChatInput:JTextField

	/** Submit chat button(Lobby screen) */
	private lateinit var btnLobbyChatSend:JButton

	/** Participation in a war button(Room screen) */
	private lateinit var btnRoomButtonsJoin:JButton

	/** Withdrawal button(Room screen) */
	private lateinit var btnRoomButtonsSitOut:JButton

	/** Change team button(Room screen) */
	private lateinit var btnRoomButtonsTeamChange:JButton

	/** View Settings button (Room screen) */
	private lateinit var btnRoomButtonsViewSetting:JButton

	/** Leaderboard button (Room screen) */
	private lateinit var btnRoomButtonsRanking:JButton

	/** Partition line separating the upper and lower(Room screen) */
	private var splitRoom:JSplitPane? = null

	/** Room at the top of the screen layout manager */
	private lateinit var roomTopBarCardLayout:CardLayout

	/** Top panel room screen */
	private lateinit var subpanelRoomTopBar:JPanel

	/** Game stats panel */
	private lateinit var subpanelGameStat:JPanel

	/** CardLayout for game stats */
	private lateinit var gameStatCardLayout:CardLayout

	/** Multiplayer game stats table */
	private lateinit var tableGameStat:JTable

	/** Multiplayer game stats table column names */
	private lateinit var strGameStatTableColumnNames:Array<String>

	/** Multiplayer game stats table data */
	private lateinit var tablemodelGameStat:DefaultTableModel

	/** Multiplayer game stats table */
	private lateinit var tableGameStat1P:JTable

	/** Multiplayer game stats table column names */
	private lateinit var strGameStatTableColumnNames1P:Array<String>

	/** Multiplayer game stats table data */
	private lateinit var tablemodelGameStat1P:DefaultTableModel

	/** Chat logAndPlayerPartition line of the list(Room screen) */
	private var splitRoomChat:JSplitPane? = null

	/** Chat log(Room screen) */
	private lateinit var txtpaneRoomChatLog:JTextPane

	/** PlayerList(Room screen) */
	private lateinit var listboxRoomChatPlayerList:JList<*>

	/** PlayerList(Room screen)Of data */
	private lateinit var listmodelRoomChatPlayerList:DefaultListModel<String>

	/** The same roomPlayerInformation */
	/** Being in the same roomPlayerReturns a list(The update does not)
	 * @return Being in the same roomPlayerList
	 */
	lateinit var sameRoomPlayerInfoList:LinkedList<NetPlayerInfo>
		private set

	/** Chat input Column(Room screen) */
	private lateinit var txtfldRoomChatInput:JTextField

	/** Submit chat button(Room screen) */
	private lateinit var btnRoomChatSend:JButton

	/** Team name input Column(Room screen) */
	private lateinit var txtfldRoomTeam:JTextField

	/** Host name input Column(Server add screen) */
	private lateinit var txtfldServerAddHost:JTextField

	/** OK button(Server add screen) */
	private lateinit var btnServerAddOK:JButton

	private lateinit var txtfldCreateRatedName:JTextField

	/** Cancel button (Created rated waiting screen) */
	private lateinit var btnCreateRatedWaitingCancel:JButton

	/** Presets box (Create rated screen) */
	private lateinit var comboboxCreateRatedPresets:JComboBox<String>

	/** People participatecount(Create rated screen) */
	private lateinit var spinnerCreateRatedMaxPlayers:JSpinner

	/** OK button (Create rated screen) */
	private lateinit var btnCreateRatedOK:JButton

	/** Custom button (Create rated screen) */
	private lateinit var btnCreateRatedCustom:JButton

	/** Cancel button (Created rated screen) */
	private lateinit var btnCreateRatedCancel:JButton

	/** ルーム名(Create room screen) */
	private lateinit var txtfldCreateRoomName:JTextField

	/** Game Mode (Create room screen) */
	private lateinit var comboboxCreateRoomMode:JComboBox<*>

	/** People participatecount(Create room screen) */
	private lateinit var spinnerCreateRoomMaxPlayers:JSpinner

	/** To wait before auto-start time(Create room screen) */
	private lateinit var spinnerCreateRoomAutoStartSeconds:JSpinner

	/** Molecular fall velocity(Create room screen) */
	private lateinit var spinnerCreateRoomGravity:JSpinner

	/** Denominator-fall velocity(Create room screen) */
	private lateinit var spinnerCreateRoomDenominator:JSpinner

	/** ARE(Create room screen) */
	private lateinit var spinnerCreateRoomARE:JSpinner

	/** ARE after line clear(Create room screen) */
	private lateinit var spinnerCreateRoomARELine:JSpinner

	/** Line clear time(Create room screen) */
	private lateinit var spinnerCreateRoomLineDelay:JSpinner

	/** Fixation time(Create room screen) */
	private lateinit var spinnerCreateRoomLockDelay:JSpinner

	/** Horizontal reservoir(Create room screen) */
	private lateinit var spinnerCreateRoomDAS:JSpinner

	/** HurryupSeconds before the startcount(Create room screen) */
	private lateinit var spinnerCreateRoomHurryupSeconds:JSpinner

	/** HurryupTimes afterBlockDo you run up the floor every time you put
	 * the(Create room screen) */
	private lateinit var spinnerCreateRoomHurryupInterval:JSpinner

	/** MapSetID(Create room screen) */
	private var spinnerCreateRoomMapSetID:JSpinner? = null

	/** Rate of change of garbage holes */
	private lateinit var spinnerCreateRoomGarbagePercent:JSpinner

	/** Map is enabled(Create room screen) */
	private lateinit var chkboxCreateRoomUseMap:JCheckBox

	/** Of all fixed rules(Create room screen) */
	private lateinit var chkboxCreateRoomRuleLock:JCheckBox

	/** Spin bonusType(Create room screen) */
	private lateinit var comboboxCreateRoomTSpinEnableType:JComboBox<*>

	/** Spin recognition type (4-point, immobile, etc.) */
	private lateinit var comboboxCreateRoomSpinCheckType:JComboBox<*>

	/** Flag for enabling B2B(Create room screen) */
	private lateinit var chkboxCreateRoomB2B:JCheckBox

	/** Flag for enabling combos(Create room screen) */
	private lateinit var chkboxCreateRoomCombo:JCheckBox

	/** Allow Rensa/Combo Block */
	private lateinit var chkboxCreateRoomRensaBlock:JCheckBox

	/** Allow countering */
	private lateinit var chkboxCreateRoomCounter:JCheckBox

	/** Enable bravo bonus */
	private lateinit var chkboxCreateRoomBravo:JCheckBox

	/** Allow EZ spins */
	private lateinit var chkboxCreateRoomTSpinEnableEZ:JCheckBox

	/** 3If I live more than Attack Reduce the force(Create room screen) */
	private lateinit var chkboxCreateRoomReduceLineSend:JCheckBox

	/** Set garbage type */
	private lateinit var chkboxCreateRoomGarbageChangePerAttack:JCheckBox

	/** Set garbage type */
	private lateinit var chkboxCreateRoomDivideChangeRateByPlayers:JCheckBox

	/** B2B chunk type */
	private lateinit var chkboxCreateRoomB2BChunk:JCheckBox

	/** Fragmentarygarbage blockUsing the system(Create room screen) */
	private lateinit var chkboxCreateRoomUseFractionalGarbage:JCheckBox

	/** Use Target System (Create room screen) */
	private lateinit var chkboxCreateRoomIsTarget:JCheckBox

	/** Spinner for Targeting time (Create room screen) */
	private lateinit var spinnerCreateRoomTargetTimer:JSpinner

	/** TNET2TypeAutomatically start timerI use(Create room screen) */
	private lateinit var chkboxCreateRoomAutoStartTNET2:JCheckBox

	/** SomeoneCancelWasTimerInvalidation(Create room screen) */
	private lateinit var chkboxCreateRoomDisableTimerAfterSomeoneCancelled:JCheckBox

	/** Preset number (Create room screen) */
	private lateinit var spinnerCreateRoomPresetID:JSpinner

	/** Preset code (Create room screen) */
	private lateinit var txtfldCreateRoomPresetCode:JTextField

	/** OK button(Create room screen) */
	private lateinit var btnCreateRoomOK:JButton

	/** Participation in a war button(Create room screen) */
	private lateinit var btnCreateRoomJoin:JButton

	/** Spectator button(Create room screen) */
	private lateinit var btnCreateRoomWatch:JButton

	/** Cancel Button (Create room screen) */
	private lateinit var btnCreateRoomCancel:JButton

	/** Game mode label (Create room 1P screen) */
	private lateinit var labelCreateRoom1PGameMode:JLabel

	/** Game mode listbox (Create room 1P screen) */
	private lateinit var listboxCreateRoom1PModeList:JList<*>

	/** Game mode list data (Create room 1P screen) */
	private lateinit var listmodelCreateRoom1PModeList:DefaultListModel<String>

	/** Rule list listbox (Create room 1P screen) */
	private lateinit var listboxCreateRoom1PRuleList:JList<*>

	/** Rule list list data (Create room 1P screen) */
	private lateinit var listmodelCreateRoom1PRuleList:DefaultListModel<String>

	/** OK button (Create room 1P screen) */
	private lateinit var btnCreateRoom1POK:JButton

	/** Cancel button (Create room 1P screen) */
	private lateinit var btnCreateRoom1PCancel:JButton

	/** Tab (MPRanking screen) */
	private lateinit var tabMPRanking:JTabbedPane

	/** Column names of multiplayer leaderboard (MPRanking screen) */
	private lateinit var strMPRankingTableColumnNames:Array<String>

	/** Table of multiplayer leaderboard (MPRanking screen) */
	private lateinit var tableMPRanking:Array<JTable>

	/** Table data of multiplayer leaderboard (MPRanking screen) */
	private lateinit var tablemodelMPRanking:Array<DefaultTableModel>

	/** OK button (MPRanking screen) */
	private lateinit var btnMPRankingOK:JButton

	/** Tab (Rule change screen) */
	private lateinit var tabRuleChange:JTabbedPane

	/** Rule list listbox (Rule change screen) */
	private lateinit var listboxRuleChangeRuleList:Array<JList<*>>

	/** OK button (Rule change screen) */
	private lateinit var btnRuleChangeOK:JButton

	/** Cancel button (Rule change screen) */
	private lateinit var btnRuleChangeCancel:JButton

	/** Rule entries (Rule change screen) */
	private lateinit var ruleEntries:LinkedList<RuleEntry>

	/** Tuning: A button rotation Combobox */
	private lateinit var comboboxTuningRotateButtonDefaultRight:JComboBox<*>
	/** Tuning: Diagonal move Combobox */
	private lateinit var comboboxTuningMoveDiagonal:JComboBox<*>
	/** Tuning: Show Outline Only Combobox */
	private lateinit var comboboxTuningBlockShowOutlineOnly:JComboBox<*>
	/** Tuning: Skin Combobox */
	private lateinit var comboboxTuningSkin:JComboBox<ComboLabel>
	/** Tuning: Skin Images */
	private lateinit var imgTuningBlockSkins:Array<BufferedImage>
	/** Tuning: Outline type combobox */
	private lateinit var comboboxTuningBlockOutlineType:JComboBox<*>
	/** Tuning: Minimum DAS */
	private lateinit var txtfldTuningMinDAS:JTextField
	/** Tuning: Maximum DAS */
	private lateinit var txtfldTuningMaxDAS:JTextField
	/** Tuning: DAS dealy */
	private lateinit var txtfldTuningDasDelay:JTextField
	/** Tuning: Checkbox to enable swapping the roles of up/down buttons
	 * in-game */
	private lateinit var chkboxTuningReverseUpDown:JCheckBox

	/** @return Current ScreenChat log
	 */
	val currentChatLogTextPane:JTextPane
		get() = if(tabLobbyAndRoom.selectedIndex!=0) txtpaneRoomChatLog else txtpaneLobbyChatLog

	/** Get current time as String (for chat log)
	 * @return Current time as String
	 */
	val currentTimeAsString:String
		get() = SimpleDateFormat("HH:mm:ss").format(GregorianCalendar().time)

	/** Get currenlty selected values set ID
	 * @return Map set ID
	 */
	val currentSelectedMapSetID:Int
		get() = spinnerCreateRoomMapSetID?.let {it.value as Int} ?: 0

	/** Get rule file list (for rule change screen)
	 * @return Rule file list. null if directory doesn't exist.
	 */
	// Sort if not windows
	val ruleFileList:Array<String>?
		get() {
			val dir = File("config/rule")

			val list = dir.list {dir1, name -> name.endsWith(".rul")}

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
		} catch(e:IOException) {
		}

		// Load global settings
		try {
			val `in` = FileInputStream("config/setting/global.cfg")
			propGlobal.load(`in`)
			`in`.close()
		} catch(e:IOException) {
		}

		// SwingRead version of the configuration file
		try {
			val `in` = FileInputStream("config/setting/swing.cfg")
			propSwingConfig.load(`in`)
			`in`.close()
		} catch(e:IOException) {
		}

		// ObserverFunction read configuration file
		try {
			val `in` = FileInputStream("config/setting/netobserver.cfg")
			propObserver.load(`in`)
			`in`.close()
		} catch(e:IOException) {
		}

		// Game mode description
		try {
			val `in` = FileInputStream("config/lang/modedesc_default.xml")
			propDefaultModeDesc.load(`in`)
			`in`.close()
		} catch(e:IOException) {
			log.error("Couldn't load default mode description file", e)
		}

		try {
			val `in` = FileInputStream("config/lang/modedesc_"+Locale.getDefault().country+".xml")
			propModeDesc.load(`in`)
			`in`.close()
		} catch(e:IOException) {
		}

		// Read language file
		try {
			val `in` = FileInputStream("config/lang/netlobby_default.xml")
			propLangDefault.load(`in`)
			`in`.close()
		} catch(e:Exception) {
			log.error("Couldn't load default UI language file", e)
		}

		try {
			val `in` = FileInputStream("config/lang/netlobby_"+Locale.getDefault().country+".xml")
			propLang.load(`in`)
			`in`.close()
		} catch(e:IOException) {
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
		listRatedRuleName = Array(GameEngine.MAX_GAMESTYLE) {LinkedList<String>()}
		// Map list
		mapList = LinkedList()

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
				l.netlobbyOnInit(this)
		if(netDummyMode!=null) netDummyMode!!.netlobbyOnInit(this)
	}

	/** GUI Initialization */
	private fun initUI() {
		contentPaneCardLayout = CardLayout()
		contentPane.layout = contentPaneCardLayout

		menuBar = Array(SCREENCARD_NAMES.size) {JMenuBar()}

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
		val labelNameEntry = JLabel(getUIText("ServerSelect_LabelName"))
		subpanelNameEntry.add(labelNameEntry, BorderLayout.WEST)

		// *** Name input Column
		txtfldPlayerName = JTextField()
		txtfldPlayerName.componentPopupMenu = TextComponentPopupMenu(txtfldPlayerName)
		txtfldPlayerName.text = propConfig.getProperty("serverselect.txtfldPlayerName.text", "")
		subpanelNameEntry.add(txtfldPlayerName, BorderLayout.CENTER)

		// ** Team name input Panel
		val subpanelTeamEntry = JPanel(BorderLayout())
		subpanelNames.add(subpanelTeamEntry)

		// *** &#39;Team name:&quot;Label
		val labelTeamEntry = JLabel(getUIText("ServerSelect_LabelTeam"))
		subpanelTeamEntry.add(labelTeamEntry, BorderLayout.WEST)

		// *** Team name input Column
		txtfldPlayerTeam = JTextField()
		txtfldPlayerTeam.componentPopupMenu = TextComponentPopupMenu(txtfldPlayerTeam)
		txtfldPlayerTeam.text = propConfig.getProperty("serverselect.txtfldPlayerTeam.text", "")
		subpanelTeamEntry.add(txtfldPlayerTeam, BorderLayout.CENTER)

		// * Server selection list box
		listmodelServerList = DefaultListModel<String>()
		if(GameManager.isDevBuild) {
			if(!loadListToDefaultListModel(listmodelServerList, "config/setting/netlobby_serverlist_dev.cfg")) {
				loadListToDefaultListModel(listmodelServerList, "config/list/netlobby_serverlist_default_dev.lst")
				saveListFromDefaultListModel(listmodelServerList, "config/setting/netlobby_serverlist_dev.cfg")
			}
		} else if(!loadListToDefaultListModel(listmodelServerList, "config/setting/netlobby_serverlist.cfg")) {
			loadListToDefaultListModel(listmodelServerList, "config/list/netlobby_serverlist_default.lst")
			saveListFromDefaultListModel(listmodelServerList, "config/setting/netlobby_serverlist.cfg")
		}
		listboxServerList = JList(listmodelServerList)
		listboxServerList.componentPopupMenu = ServerSelectListBoxPopupMenu()
		listboxServerList.addMouseListener(ServerSelectListBoxMouseAdapter())
		listboxServerList.setSelectedValue(propConfig.getProperty("serverselect.listboxServerList.value", ""), true)
		val spListboxServerSelect = JScrollPane(listboxServerList)
		mainpanelServerSelect.add(spListboxServerSelect, BorderLayout.CENTER)

		// * Panel add or remove server
		val subpanelServerAdd = JPanel()
		subpanelServerAdd.layout = BoxLayout(subpanelServerAdd, BoxLayout.Y_AXIS)
		mainpanelServerSelect.add(subpanelServerAdd, BorderLayout.EAST)

		// ** Add Server button
		val btnServerAdd = JButton(getUIText("ServerSelect_ServerAdd"))
		btnServerAdd.maximumSize = Dimension(java.lang.Short.MAX_VALUE.toInt(), btnServerAdd.maximumSize.height)
		btnServerAdd.addActionListener(this)
		btnServerAdd.actionCommand = "ServerSelect_ServerAdd"
		btnServerAdd.setMnemonic('A')
		subpanelServerAdd.add(btnServerAdd)

		// ** Delete server button
		val btnServerDelete = JButton(getUIText("ServerSelect_ServerDelete"))
		btnServerDelete.maximumSize = Dimension(java.lang.Short.MAX_VALUE.toInt(), btnServerDelete.maximumSize.height)
		btnServerDelete.addActionListener(this)
		btnServerDelete.actionCommand = "ServerSelect_ServerDelete"
		btnServerDelete.setMnemonic('D')
		subpanelServerAdd.add(btnServerDelete)

		// ** Monitoring settings button
		val btnSetObserver = JButton(getUIText("ServerSelect_SetObserver"))
		btnSetObserver.maximumSize = Dimension(java.lang.Short.MAX_VALUE.toInt(), btnSetObserver.maximumSize.height)
		btnSetObserver.addActionListener(this)
		btnSetObserver.actionCommand = "ServerSelect_SetObserver"
		btnSetObserver.setMnemonic('S')
		subpanelServerAdd.add(btnSetObserver)

		// ** Unmonitor button
		val btnUnsetObserver = JButton(getUIText("ServerSelect_UnsetObserver"))
		btnUnsetObserver.maximumSize = Dimension(java.lang.Short.MAX_VALUE.toInt(), btnUnsetObserver.maximumSize.height)
		btnUnsetObserver.addActionListener(this)
		btnUnsetObserver.actionCommand = "ServerSelect_UnsetObserver"
		btnUnsetObserver.setMnemonic('U')
		subpanelServerAdd.add(btnUnsetObserver)

		// * Connection button·Exit buttonPanel
		val subpanelServerSelectButtons = JPanel()
		subpanelServerSelectButtons.layout = BoxLayout(subpanelServerSelectButtons, BoxLayout.X_AXIS)
		mainpanelServerSelect.add(subpanelServerSelectButtons, BorderLayout.SOUTH)

		// ** Connection button
		btnServerConnect = JButton(getUIText("ServerSelect_Connect"))
		btnServerConnect.maximumSize = Dimension(java.lang.Short.MAX_VALUE.toInt(), btnServerConnect.maximumSize.height)
		btnServerConnect.addActionListener(this)
		btnServerConnect.actionCommand = "ServerSelect_Connect"
		btnServerConnect.setMnemonic('C')
		subpanelServerSelectButtons.add(btnServerConnect)

		// ** Exit button
		val btnServerExit = JButton(getUIText("ServerSelect_Exit"))
		btnServerExit.maximumSize = Dimension(java.lang.Short.MAX_VALUE.toInt(), btnServerExit.maximumSize.height)
		btnServerExit.addActionListener(this)
		btnServerExit.actionCommand = "ServerSelect_Exit"
		btnServerExit.setMnemonic('X')
		subpanelServerSelectButtons.add(btnServerExit)
	}

	/** Lobby screen initialization */
	private fun initLobbyUI() {
		tabLobbyAndRoom = JTabbedPane()
		contentPane.add(tabLobbyAndRoom, SCREENCARD_NAMES[SCREENCARD_LOBBY])

		// === Popup Menu ===

		// * Popup Menu
		popupLobbyOptions = JPopupMenu()

		// ** Rule change
		itemLobbyMenuRuleChange = JMenuItem(getUIText("Lobby_RuleChange"))
		itemLobbyMenuRuleChange.addActionListener(this)
		itemLobbyMenuRuleChange.actionCommand = "Lobby_RuleChange"
		itemLobbyMenuRuleChange.setMnemonic('R')
		itemLobbyMenuRuleChange.toolTipText = getUIText("Lobby_RuleChange_Tip")
		popupLobbyOptions.add(itemLobbyMenuRuleChange)

		// ** Team change
		itemLobbyMenuTeamChange = JMenuItem(getUIText("Lobby_TeamChange"))
		itemLobbyMenuTeamChange.addActionListener(this)
		itemLobbyMenuTeamChange.actionCommand = "Lobby_TeamChange"
		itemLobbyMenuTeamChange.setMnemonic('T')
		itemLobbyMenuTeamChange.toolTipText = getUIText("Lobby_TeamChange_Tip")
		popupLobbyOptions.add(itemLobbyMenuTeamChange)

		// ** Leaderboard
		itemLobbyMenuRanking = JMenuItem(getUIText("Lobby_Ranking"))
		itemLobbyMenuRanking.addActionListener(this)
		itemLobbyMenuRanking.actionCommand = "Lobby_Ranking"
		itemLobbyMenuRanking.setMnemonic('K')
		itemLobbyMenuRanking.toolTipText = getUIText("Lobby_Ranking_Tip")
		popupLobbyOptions.add(itemLobbyMenuRanking)

		// === Lobby Tab ===
		val mainpanelLobby = JPanel(BorderLayout())
		//this.getContentPane().add(mainpanelLobby, SCREENCARD_NAMES[SCREENCARD_LOBBY]);
		tabLobbyAndRoom.addTab(getUIText("Lobby_Tab_Lobby"), mainpanelLobby)
		tabLobbyAndRoom.setMnemonicAt(0, 'Y'.toInt())

		// * Partition line separating the upper and lower
		splitLobby = JSplitPane(JSplitPane.VERTICAL_SPLIT).apply {
			dividerLocation = propConfig.getProperty("lobby.splitLobby.location", 200)

		}
		mainpanelLobby.add(splitLobby, BorderLayout.CENTER)

		// ** Room list(Top)
		val subpanelRoomList = JPanel(BorderLayout()).apply {
			minimumSize = Dimension(0, 0)
		}
		splitLobby?.topComponent = subpanelRoomList
		// *** Top of the screen panel lobby
		roomListTopBarCardLayout = CardLayout()
		subpanelRoomListTopBar = JPanel(roomListTopBarCardLayout)
		subpanelRoomList.add(subpanelRoomListTopBar, BorderLayout.NORTH)

		// **** Room list buttonKind
		val subpanelRoomListButtons = JPanel()
		subpanelRoomListTopBar.add(subpanelRoomListButtons, "Buttons")
		//subpanelRoomList.add(subpanelRoomListButtons, BorderLayout.NORTH);

		// ***** TODO:Quick Start button
		btnRoomListQuickStart = JButton(getUIText("Lobby_QuickStart"))
		btnRoomListQuickStart.addActionListener(this)
		btnRoomListQuickStart.actionCommand = "Lobby_QuickStart"
		btnRoomListQuickStart.setMnemonic('Q')
		btnRoomListQuickStart.toolTipText = getUIText("Lobby_QuickStart_Tip")
		btnRoomListQuickStart.isVisible = false
		subpanelRoomListButtons.add(btnRoomListQuickStart)

		// ***** Create a Room button
		btnRoomListRoomCreate = JButton(getUIText("Lobby_RoomCreate"))
		btnRoomListRoomCreate.addActionListener(this)
		btnRoomListRoomCreate.actionCommand = "Lobby_RoomCreate"
		btnRoomListRoomCreate.setMnemonic('N')
		btnRoomListRoomCreate.toolTipText = getUIText("Lobby_RoomCreate_Tip")
		subpanelRoomListButtons.add(btnRoomListRoomCreate)

		// ***** Create Room (1P) button
		btnRoomListRoomCreate1P = JButton(getUIText("Lobby_RoomCreate1P"))
		btnRoomListRoomCreate1P.addActionListener(this)
		btnRoomListRoomCreate1P.actionCommand = "Lobby_RoomCreate1P"
		btnRoomListRoomCreate1P.setMnemonic('1')
		btnRoomListRoomCreate1P.toolTipText = getUIText("Lobby_RoomCreate1P_Tip")
		subpanelRoomListButtons.add(btnRoomListRoomCreate1P)

		// ***** Options menu button
		btnRoomListOptions = JButton(getUIText("Lobby_Options"))
		btnRoomListOptions.addActionListener(this)
		btnRoomListOptions.actionCommand = "Lobby_Options"
		btnRoomListOptions.setMnemonic('O')
		btnRoomListOptions.toolTipText = getUIText("Lobby_Options_Tip")
		subpanelRoomListButtons.add(btnRoomListOptions)

		// ***** Cut button
		val btnRoomListDisconnect = JButton(getUIText("Lobby_Disconnect"))
		btnRoomListDisconnect.addActionListener(this)
		btnRoomListDisconnect.actionCommand = "Lobby_Disconnect"
		btnRoomListDisconnect.setMnemonic('L')
		btnRoomListDisconnect.toolTipText = getUIText("Lobby_Disconnect_Tip")
		subpanelRoomListButtons.add(btnRoomListDisconnect)

		// **** Panel change team
		val subpanelRoomListTeam = JPanel(BorderLayout())
		subpanelRoomListTopBar.add(subpanelRoomListTeam, "Team")

		// ***** Team name input Column
		txtfldRoomListTeam = JTextField()
		subpanelRoomListTeam.add(txtfldRoomListTeam, BorderLayout.CENTER)

		// ***** Team nameChange buttonPanel
		val subpanelRoomListTeamButtons = JPanel()
		subpanelRoomListTeam.add(subpanelRoomListTeamButtons, BorderLayout.EAST)

		// ****** Team nameChangeOK
		val btnRoomListTeamOK = JButton(getUIText("Lobby_TeamChange_OK"))
		btnRoomListTeamOK.addActionListener(this)
		btnRoomListTeamOK.actionCommand = "Lobby_TeamChange_OK"
		btnRoomListTeamOK.setMnemonic('O')
		subpanelRoomListTeamButtons.add(btnRoomListTeamOK)

		// ****** Team nameChangeCancel
		val btnRoomListTeamCancel = JButton(getUIText("Lobby_TeamChange_Cancel"))
		btnRoomListTeamCancel.addActionListener(this)
		btnRoomListTeamCancel.actionCommand = "Lobby_TeamChange_Cancel"
		btnRoomListTeamCancel.setMnemonic('C')
		subpanelRoomListTeamButtons.add(btnRoomListTeamCancel)

		// *** Room list table
		strTableColumnNames = Array(ROOMTABLE_COLUMNNAMES.size) {getUIText(ROOMTABLE_COLUMNNAMES[it])}
		tablemodelRoomList = DefaultTableModel(strTableColumnNames, 0)
		tableRoomList = JTable(tablemodelRoomList).apply {
			setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
			setDefaultEditor(Any::class.java, null)
			autoResizeMode = JTable.AUTO_RESIZE_OFF
			tableHeader.reorderingAllowed = false
			componentPopupMenu = RoomTablePopupMenu()
			addMouseListener(RoomTableMouseAdapter())
			addKeyListener(RoomTableKeyAdapter())
		}
		val tm = tableRoomList.columnModel
		tm.getColumn(0).preferredWidth = propConfig.getProperty("tableRoomList.width.id", 35) // ID
		tm.getColumn(1).preferredWidth = propConfig.getProperty("tableRoomList.width.name", 155) // Name
		tm.getColumn(2).preferredWidth = propConfig.getProperty("tableRoomList.width.rated", 50) // Rated
		tm.getColumn(3).preferredWidth = propConfig.getProperty("tableRoomList.width.rulename", 105) // Rule name
		tm.getColumn(4).preferredWidth = propConfig.getProperty("tableRoomList.width.modename", 105) // Mode name
		tm.getColumn(5).preferredWidth = propConfig.getProperty("tableRoomList.width.status", 55) // Status
		tm.getColumn(6).preferredWidth = propConfig.getProperty("tableRoomList.width.players", 65) // Players
		tm.getColumn(7).preferredWidth = propConfig.getProperty("tableRoomList.width.spectators", 65) // Spectators

		val spTableRoomList = JScrollPane(tableRoomList)
		subpanelRoomList.add(spTableRoomList, BorderLayout.CENTER)

		// ** Chat(Under)
		val subpanelLobbyChat = JPanel(BorderLayout()).apply {
			minimumSize = Dimension(0, 0)
		}
		splitLobby?.bottomComponent = subpanelLobbyChat

		// *** Chat logAndPlayerPartition line of the list
		splitLobbyChat = JSplitPane().apply {
			dividerLocation = propConfig.getProperty("lobby.splitLobbyChat.location", 350)
		}
		subpanelLobbyChat.add(splitLobbyChat, BorderLayout.CENTER)

		// **** Chat log(Lobby screen)
		txtpaneLobbyChatLog = JTextPane()
		txtpaneLobbyChatLog.componentPopupMenu = LogPopupMenu(txtpaneLobbyChatLog)
		txtpaneLobbyChatLog.addKeyListener(LogKeyAdapter())
		val spTxtpaneLobbyChatLog = JScrollPane(txtpaneLobbyChatLog)
		spTxtpaneLobbyChatLog.minimumSize = Dimension(0, 0)
		splitLobbyChat?.leftComponent = spTxtpaneLobbyChatLog

		// **** PlayerList(Lobby screen)
		listmodelLobbyChatPlayerList = DefaultListModel()
		listboxLobbyChatPlayerList = JList(listmodelLobbyChatPlayerList)
		listboxLobbyChatPlayerList.componentPopupMenu = ListBoxPopupMenu(listboxLobbyChatPlayerList)
		val spListboxLobbyChatPlayerList = JScrollPane(listboxLobbyChatPlayerList)
		spListboxLobbyChatPlayerList.minimumSize = Dimension(0, 0)
		splitLobbyChat?.rightComponent = spListboxLobbyChatPlayerList

		// *** Chat input Column panel(Lobby screen)
		val subpanelLobbyChatInputArea = JPanel(BorderLayout())
		subpanelLobbyChat.add(subpanelLobbyChatInputArea, BorderLayout.SOUTH)

		// **** Chat input Column(Lobby screen)
		txtfldLobbyChatInput = JTextField()
		txtfldLobbyChatInput.componentPopupMenu = TextComponentPopupMenu(txtfldLobbyChatInput)
		subpanelLobbyChatInputArea.add(txtfldLobbyChatInput, BorderLayout.CENTER)

		// **** Submit chat button(Lobby screen)
		btnLobbyChatSend = JButton(getUIText("Lobby_ChatSend"))
		btnLobbyChatSend.addActionListener(this)
		btnLobbyChatSend.actionCommand = "Lobby_ChatSend"
		btnLobbyChatSend.setMnemonic('S')
		subpanelLobbyChatInputArea.add(btnLobbyChatSend, BorderLayout.EAST)

		// === Room Tab ===
		val mainpanelRoom = JPanel(BorderLayout())
		//this.getContentPane().add(mainpanelRoom, SCREENCARD_NAMES[SCREENCARD_ROOM]);
		tabLobbyAndRoom.addTab(getUIText("Lobby_Tab_NoRoom"), mainpanelRoom)
		tabLobbyAndRoom.setMnemonicAt(1, 'R'.toInt())
		tabLobbyAndRoom.setEnabledAt(1, false)

		// * Partition line separating the upper and lower
		splitRoom = JSplitPane(JSplitPane.VERTICAL_SPLIT).apply {
			dividerLocation = propConfig.getProperty("room.splitRoom.location", 200)
		}
		mainpanelRoom.add(splitRoom, BorderLayout.CENTER)

		// ** List of game results(Top)
		val subpanelRoomTop = JPanel(BorderLayout())
		subpanelRoomTop.minimumSize = Dimension(0, 0)
		splitRoom?.topComponent = subpanelRoomTop

		// *** Top panel room screen
		roomTopBarCardLayout = CardLayout()
		subpanelRoomTopBar = JPanel(roomTopBarCardLayout)
		subpanelRoomTop.add(subpanelRoomTopBar, BorderLayout.NORTH)

		// ****  buttonPanel type
		val subpanelRoomButtons = JPanel()
		subpanelRoomTopBar.add(subpanelRoomButtons, "Buttons")

		// ***** Withdrawal button
		val btnRoomButtonsLeave = JButton(getUIText("Room_Leave"))
		btnRoomButtonsLeave.addActionListener(this)
		btnRoomButtonsLeave.actionCommand = "Room_Leave"
		btnRoomButtonsLeave.setMnemonic('L')
		btnRoomButtonsLeave.toolTipText = getUIText("Room_Leave_Tip")
		subpanelRoomButtons.add(btnRoomButtonsLeave)

		// ***** Participation in a war button
		btnRoomButtonsJoin = JButton(getUIText("Room_Join"))
		btnRoomButtonsJoin.addActionListener(this)
		btnRoomButtonsJoin.actionCommand = "Room_Join"
		btnRoomButtonsJoin.setMnemonic('J')
		btnRoomButtonsJoin.toolTipText = getUIText("Room_Join_Tip")
		btnRoomButtonsJoin.isVisible = false
		subpanelRoomButtons.add(btnRoomButtonsJoin)

		// ***** Withdrawal button
		btnRoomButtonsSitOut = JButton(getUIText("Room_SitOut"))
		btnRoomButtonsSitOut.addActionListener(this)
		btnRoomButtonsSitOut.actionCommand = "Room_SitOut"
		btnRoomButtonsSitOut.setMnemonic('W')
		btnRoomButtonsSitOut.toolTipText = getUIText("Room_SitOut_Tip")
		btnRoomButtonsSitOut.isVisible = false
		subpanelRoomButtons.add(btnRoomButtonsSitOut)

		// ***** Change team button
		btnRoomButtonsTeamChange = JButton(getUIText("Room_TeamChange"))
		btnRoomButtonsTeamChange.addActionListener(this)
		btnRoomButtonsTeamChange.actionCommand = "Room_TeamChange"
		btnRoomButtonsTeamChange.setMnemonic('T')
		btnRoomButtonsTeamChange.toolTipText = getUIText("Room_TeamChange_Tip")
		subpanelRoomButtons.add(btnRoomButtonsTeamChange)

		// **** Panel change team
		val subpanelRoomTeam = JPanel(BorderLayout())
		subpanelRoomTopBar.add(subpanelRoomTeam, "Team")

		// ***** Team name input Column
		txtfldRoomTeam = JTextField()
		subpanelRoomTeam.add(txtfldRoomTeam, BorderLayout.CENTER)

		// ***** Team nameChange buttonPanel
		val subpanelRoomTeamButtons = JPanel()
		subpanelRoomTeam.add(subpanelRoomTeamButtons, BorderLayout.EAST)

		// ****** Team nameChangeOK
		val btnRoomTeamOK = JButton(getUIText("Room_TeamChange_OK"))
		btnRoomTeamOK.addActionListener(this)
		btnRoomTeamOK.actionCommand = "Room_TeamChange_OK"
		btnRoomTeamOK.setMnemonic('O')
		subpanelRoomTeamButtons.add(btnRoomTeamOK)

		// ****** Team nameChangeCancel
		val btnRoomTeamCancel = JButton(getUIText("Room_TeamChange_Cancel"))
		btnRoomTeamCancel.addActionListener(this)
		btnRoomTeamCancel.actionCommand = "Room_TeamChange_Cancel"
		btnRoomTeamCancel.setMnemonic('C')
		subpanelRoomTeamButtons.add(btnRoomTeamCancel)

		// ***** Setting confirmation button
		btnRoomButtonsViewSetting = JButton(getUIText("Room_ViewSetting"))
		btnRoomButtonsViewSetting.addActionListener(this)
		btnRoomButtonsViewSetting.actionCommand = "Room_ViewSetting"
		btnRoomButtonsViewSetting.setMnemonic('V')
		btnRoomButtonsViewSetting.toolTipText = getUIText("Room_ViewSetting_Tip")
		subpanelRoomButtons.add(btnRoomButtonsViewSetting)

		// ***** Leaderboard button
		btnRoomButtonsRanking = JButton(getUIText("Room_Ranking"))
		btnRoomButtonsRanking.addActionListener(this)
		btnRoomButtonsRanking.actionCommand = "Room_Ranking"
		btnRoomButtonsRanking.setMnemonic('K')
		btnRoomButtonsRanking.toolTipText = getUIText("Room_Ranking_Tip")
		btnRoomButtonsRanking.isVisible = false
		subpanelRoomButtons.add(btnRoomButtonsRanking)

		// *** Game stats area
		gameStatCardLayout = CardLayout()
		subpanelGameStat = JPanel(gameStatCardLayout)
		subpanelRoomTop.add(subpanelGameStat, BorderLayout.CENTER)

		// **** Multiplayer game stats table
		strGameStatTableColumnNames = Array(STATTABLE_COLUMNNAMES.size) {getUIText(STATTABLE_COLUMNNAMES[it])}
		tablemodelGameStat = DefaultTableModel(strGameStatTableColumnNames, 0)
		tableGameStat = JTable(tablemodelGameStat)
		tableGameStat.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
		tableGameStat.setDefaultEditor(Any::class.java, null)
		tableGameStat.autoResizeMode = JTable.AUTO_RESIZE_OFF
		tableGameStat.tableHeader.reorderingAllowed = false
		tableGameStat.componentPopupMenu = TablePopupMenu(tableGameStat)

		var tm2 = tableGameStat.columnModel
		tm2.getColumn(0).preferredWidth = propConfig.getProperty("tableGameStat.width.rank", 30) // Rank
		tm2.getColumn(1).preferredWidth = propConfig.getProperty("tableGameStat.width.name", 100) // Name
		tm2.getColumn(2).preferredWidth = propConfig.getProperty("tableGameStat.width.attack", 55) // Attack count
		tm2.getColumn(3).preferredWidth = propConfig.getProperty("tableGameStat.width.apl", 55) // APL
		tm2.getColumn(4).preferredWidth = propConfig.getProperty("tableGameStat.width.apm", 55) // APM
		tm2.getColumn(5).preferredWidth = propConfig.getProperty("tableGameStat.width.lines", 55) // Line count
		tm2.getColumn(6).preferredWidth = propConfig.getProperty("tableGameStat.width.lpm", 55) // LPM
		tm2.getColumn(7).preferredWidth = propConfig.getProperty("tableGameStat.width.piece", 55) // Piece count
		tm2.getColumn(8).preferredWidth = propConfig.getProperty("tableGameStat.width.pps", 55) // PPS
		tm2.getColumn(9).preferredWidth = propConfig.getProperty("tableGameStat.width.time", 65) // Time
		tm2.getColumn(10).preferredWidth = propConfig.getProperty("tableGameStat.width.ko", 40) // KO
		tm2.getColumn(11).preferredWidth = propConfig.getProperty("tableGameStat.width.wins", 55) // Win
		tm2.getColumn(12).preferredWidth = propConfig.getProperty("tableGameStat.width.games", 55) // Games

		val spTableGameStat = JScrollPane(tableGameStat)
		spTableGameStat.minimumSize = Dimension(0, 0)
		subpanelGameStat.add(spTableGameStat, "GameStatMP")

		// **** Single player game stats table
		strGameStatTableColumnNames1P = Array(STATTABLE1P_COLUMNNAMES.size) {
			getUIText(STATTABLE1P_COLUMNNAMES[it])
		}
		tablemodelGameStat1P = DefaultTableModel(strGameStatTableColumnNames1P, 0)
		tableGameStat1P = JTable(tablemodelGameStat1P)
		tableGameStat1P.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
		tableGameStat1P.setDefaultEditor(Any::class.java, null)
		tableGameStat1P.autoResizeMode = JTable.AUTO_RESIZE_OFF
		tableGameStat1P.tableHeader.reorderingAllowed = false
		tableGameStat1P.componentPopupMenu = TablePopupMenu(tableGameStat1P)

		tm2 = tableGameStat1P.columnModel
		tm2.getColumn(0).preferredWidth = propConfig.getProperty("tableGameStat1P.width.description", 100) // Description
		tm2.getColumn(1).preferredWidth = propConfig.getProperty("tableGameStat1P.width.value", 100) // Value

		val spTxtpaneGameStat1P = JScrollPane(tableGameStat1P)
		spTxtpaneGameStat1P.minimumSize = Dimension(0, 0)
		subpanelGameStat.add(spTxtpaneGameStat1P, "GameStat1P")

		// ** Chat panel(Under)
		val subpanelRoomChat = JPanel(BorderLayout())
		subpanelRoomChat.minimumSize = Dimension(0, 0)
		splitRoom?.bottomComponent = subpanelRoomChat

		// *** Chat logAndPlayerPartition line of the list(Room screen)
		splitRoomChat = JSplitPane().apply {
			dividerLocation = propConfig.getProperty("room.splitRoomChat.location", 350)
		}
		subpanelRoomChat.add(splitRoomChat, BorderLayout.CENTER)

		// **** Chat log(Room screen)
		txtpaneRoomChatLog = JTextPane()
		txtpaneRoomChatLog.componentPopupMenu = LogPopupMenu(txtpaneRoomChatLog)
		txtpaneRoomChatLog.addKeyListener(LogKeyAdapter())
		val spTxtpaneRoomChatLog = JScrollPane(txtpaneRoomChatLog)
		spTxtpaneRoomChatLog.minimumSize = Dimension(0, 0)
		splitRoomChat?.leftComponent = spTxtpaneRoomChatLog

		// **** PlayerList(Room screen)
		sameRoomPlayerInfoList = LinkedList()
		listmodelRoomChatPlayerList = DefaultListModel()
		listboxRoomChatPlayerList = JList(listmodelRoomChatPlayerList)
		listboxRoomChatPlayerList.componentPopupMenu = ListBoxPopupMenu(listboxRoomChatPlayerList)
		val spListboxRoomChatPlayerList = JScrollPane(listboxRoomChatPlayerList)
		spListboxRoomChatPlayerList.minimumSize = Dimension(0, 0)
		splitRoomChat?.rightComponent = spListboxRoomChatPlayerList

		// *** Chat input Column panel(Room screen)
		val subpanelRoomChatInputArea = JPanel(BorderLayout())
		subpanelRoomChat.add(subpanelRoomChatInputArea, BorderLayout.SOUTH)

		// **** Chat input Column(Room screen)
		txtfldRoomChatInput = JTextField()
		txtfldRoomChatInput.componentPopupMenu = TextComponentPopupMenu(txtfldRoomChatInput)
		subpanelRoomChatInputArea.add(txtfldRoomChatInput, BorderLayout.CENTER)

		// **** Submit chat button(Room screen)
		btnRoomChatSend = JButton(getUIText("Room_ChatSend"))
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
		val labelHost = JLabel(getUIText("ServerAdd_Host"))
		subpanelHost.add(labelHost, BorderLayout.WEST)

		// *** Host name input Column
		txtfldServerAddHost = JTextField()
		txtfldServerAddHost.componentPopupMenu = TextComponentPopupMenu(txtfldServerAddHost)
		subpanelHost.add(txtfldServerAddHost, BorderLayout.CENTER)

		// **  buttonPanel type
		val subpanelButtons = JPanel()
		subpanelButtons.layout = BoxLayout(subpanelButtons, BoxLayout.X_AXIS)
		containerpanelServerAdd.add(subpanelButtons)

		// *** OK button
		btnServerAddOK = JButton(getUIText("ServerAdd_OK"))
		btnServerAddOK.addActionListener(this)
		btnServerAddOK.actionCommand = "ServerAdd_OK"
		btnServerAddOK.setMnemonic('O')
		btnServerAddOK.maximumSize = Dimension(java.lang.Short.MAX_VALUE.toInt(), btnServerAddOK.maximumSize.height)
		subpanelButtons.add(btnServerAddOK)

		// *** Cancel button
		val btnServerAddCancel = JButton(getUIText("ServerAdd_Cancel"))
		btnServerAddCancel.addActionListener(this)
		btnServerAddCancel.actionCommand = "ServerAdd_Cancel"
		btnServerAddCancel.setMnemonic('C')
		btnServerAddCancel.maximumSize = Dimension(java.lang.Short.MAX_VALUE.toInt(), btnServerAddCancel.maximumSize.height)
		subpanelButtons.add(btnServerAddCancel)
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
		val labelWaiting = JLabel(getUIText("CreateRated_Waiting_Text"))
		subpanelText.add(labelWaiting, BorderLayout.CENTER)

		// ** Subpanel for cancel button
		val subpanelButtons = JPanel()
		mainpanelCreateRatedWaiting.add(subpanelButtons, BorderLayout.SOUTH)

		// *** Cancel Button
		btnCreateRatedWaitingCancel = JButton(getUIText("CreateRated_Waiting_Cancel"))
		btnCreateRatedWaitingCancel.addActionListener(this)
		btnCreateRatedWaitingCancel.actionCommand = "CreateRated_Waiting_Cancel"
		btnCreateRatedWaitingCancel.setMnemonic('C')
		btnCreateRatedWaitingCancel.maximumSize =
			Dimension(java.lang.Short.MAX_VALUE.toInt(), btnCreateRatedWaitingCancel.maximumSize.height)
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
		val labelName = JLabel(getUIText("CreateRated_Name"))
		subpanelName.add(labelName, BorderLayout.WEST)

		// *** Room name textfield
		txtfldCreateRatedName = JTextField()
		txtfldCreateRatedName.componentPopupMenu = TextComponentPopupMenu(txtfldCreateRatedName)
		txtfldCreateRatedName.toolTipText = getUIText("CreateRated_Name_Tip")
		subpanelName.add(txtfldCreateRatedName, BorderLayout.CENTER)

		// ** Subpanel for preset selection
		val subpanelPresetSelect = JPanel(BorderLayout())
		containerpanelCreateRated.add(subpanelPresetSelect)

		// *** "Preset:" label
		val labelWaiting = JLabel(getUIText("CreateRated_Preset"))
		subpanelPresetSelect.add(labelWaiting, BorderLayout.WEST)

		// *** Presets
		comboboxCreateRatedPresets = JComboBox(arrayOf("Select..."))
		comboboxCreateRatedPresets.selectedIndex = propConfig.getProperty("createrated.defaultPreset", 0)
		comboboxCreateRatedPresets.preferredSize = Dimension(200, 20)
		comboboxCreateRatedPresets.toolTipText = getUIText("CreateRated_Preset_Tip")
		subpanelPresetSelect.add(comboboxCreateRatedPresets, BorderLayout.EAST)

		// ** Number of players panel
		val subpanelMaxPlayers = JPanel(BorderLayout())
		containerpanelCreateRated.add(subpanelMaxPlayers)

		// *** Number of players label
		val labelMaxPlayers = JLabel(getUIText("CreateRated_MaxPlayers"))
		subpanelMaxPlayers.add(labelMaxPlayers, BorderLayout.WEST)

		// *** Number of players textfield
		val defaultMaxPlayers = propConfig.getProperty("createrated.defaultMaxPlayers", 6)
		spinnerCreateRatedMaxPlayers = JSpinner(SpinnerNumberModel(defaultMaxPlayers, 2, 6, 1))
		spinnerCreateRatedMaxPlayers.preferredSize = Dimension(200, 20)
		spinnerCreateRatedMaxPlayers.toolTipText = getUIText("CreateRated_MaxPlayers_Tip")
		subpanelMaxPlayers.add(spinnerCreateRatedMaxPlayers, BorderLayout.EAST)

		// ** Subpanel for buttons
		val subpanelButtons = JPanel()
		mainpanelCreateRated.add(subpanelButtons, BorderLayout.SOUTH)

		// *** OK button
		btnCreateRatedOK = JButton(getUIText("CreateRated_OK"))
		btnCreateRatedOK.addActionListener(this)
		btnCreateRatedOK.actionCommand = "CreateRated_OK"
		btnCreateRatedOK.setMnemonic('O')
		btnCreateRatedOK.maximumSize = Dimension(java.lang.Short.MAX_VALUE.toInt(), btnCreateRatedOK.maximumSize.height)
		subpanelButtons.add(btnCreateRatedOK)

		// *** Custom button
		btnCreateRatedCustom = JButton(getUIText("CreateRated_Custom"))
		btnCreateRatedCustom.addActionListener(this)
		btnCreateRatedCustom.actionCommand = "CreateRated_Custom"
		btnCreateRatedCustom.setMnemonic('U')
		btnCreateRatedCustom.maximumSize =
			Dimension(java.lang.Short.MAX_VALUE.toInt(), btnCreateRatedCustom.maximumSize.height)
		subpanelButtons.add(btnCreateRatedCustom)

		// *** Cancel Button
		btnCreateRatedCancel = JButton(getUIText("CreateRated_Cancel"))
		btnCreateRatedCancel.addActionListener(this)
		btnCreateRatedCancel.actionCommand = "CreateRated_Cancel"
		btnCreateRatedCancel.setMnemonic('C')
		btnCreateRatedCancel.maximumSize =
			Dimension(java.lang.Short.MAX_VALUE.toInt(), btnCreateRatedCancel.maximumSize.height)
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

		// * Speed ​​setting panel(Stretching for prevention)
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

		// * Speed ​​setting panel(Body)
		val containerpanelCreateRoomMain = JPanel()
		containerpanelCreateRoomMain.layout = BoxLayout(containerpanelCreateRoomMain, BoxLayout.Y_AXIS)
		containerpanelCreateRoomMainOwner.add(containerpanelCreateRoomMain, BorderLayout.NORTH)

		// ** Panel Room name
		val subpanelName = JPanel(BorderLayout())
		containerpanelCreateRoomMain.add(subpanelName)

		// *** Name &quot;Room:&quot;Label
		val labelName = JLabel(getUIText("CreateRoom_Name"))
		subpanelName.add(labelName, BorderLayout.WEST)

		// *** Room name input Column
		txtfldCreateRoomName = JTextField()
		txtfldCreateRoomName.componentPopupMenu = TextComponentPopupMenu(txtfldCreateRoomName)
		txtfldCreateRoomName.toolTipText = getUIText("CreateRoom_Name_Tip")
		subpanelName.add(txtfldCreateRoomName, BorderLayout.CENTER)

		// ** Game Mode panel
		val subpanelMode = JPanel(BorderLayout())
		containerpanelCreateRoomMain.add(subpanelMode)

		// *** Mode label
		val labelMode = JLabel(getUIText("CreateRoom_Mode"))
		subpanelMode.add(labelMode, BorderLayout.WEST)

		// *** Mode Combobox
		val modelMode = DefaultComboBoxModel<String>()
		loadModeList(modelMode, "config/list/netlobby_multimode.lst")
		comboboxCreateRoomMode = JComboBox(modelMode).apply {
			preferredSize = Dimension(200, 20)
			toolTipText = getUIText("CreateRoom_Mode_Tip")
		}
		subpanelMode.add(comboboxCreateRoomMode, BorderLayout.EAST)

		// ** People participatecountPanel
		val subpanelMaxPlayers = JPanel(BorderLayout())
		containerpanelCreateRoomMain.add(subpanelMaxPlayers)

		// *** Human participation &quot;count:&quot;Label
		val labelMaxPlayers = JLabel(getUIText("CreateRoom_MaxPlayers"))
		subpanelMaxPlayers.add(labelMaxPlayers, BorderLayout.WEST)

		// *** People participatecountSelection
		val defaultMaxPlayers = propConfig.getProperty("createroom.defaultMaxPlayers", 6)
		spinnerCreateRoomMaxPlayers = JSpinner(SpinnerNumberModel(defaultMaxPlayers, 2, 6, 1))
		spinnerCreateRoomMaxPlayers.preferredSize = Dimension(200, 20)
		spinnerCreateRoomMaxPlayers.toolTipText = getUIText("CreateRoom_MaxPlayers_Tip")
		subpanelMaxPlayers.add(spinnerCreateRoomMaxPlayers, BorderLayout.EAST)

		// ** HurryupSecondcountPanel
		val subpanelHurryupSeconds = JPanel(BorderLayout())
		containerpanelCreateRoomMain.add(subpanelHurryupSeconds)

		// *** &#39;HURRY UPSeconds before the startcount:&quot;Label
		val labelHurryupSeconds = JLabel(getUIText("CreateRoom_HurryupSeconds"))
		subpanelHurryupSeconds.add(labelHurryupSeconds, BorderLayout.WEST)

		// *** HurryupSecondcount
		val defaultHurryupSeconds = propConfig.getProperty("createroom.defaultHurryupSeconds", 180)
		spinnerCreateRoomHurryupSeconds = JSpinner(SpinnerNumberModel(defaultHurryupSeconds, -1, 999, 1))
		spinnerCreateRoomHurryupSeconds.preferredSize = Dimension(200, 20)
		spinnerCreateRoomHurryupSeconds.toolTipText = getUIText("CreateRoom_HurryupSeconds_Tip")
		subpanelHurryupSeconds.add(spinnerCreateRoomHurryupSeconds, BorderLayout.EAST)

		// ** HurryupPanel spacing
		val subpanelHurryupInterval = JPanel(BorderLayout())
		containerpanelCreateRoomMain.add(subpanelHurryupInterval)

		// *** &#39;HURRY UPLater, Interval overcall the floor:&quot;Label
		val labelHurryupInterval = JLabel(getUIText("CreateRoom_HurryupInterval"))
		subpanelHurryupInterval.add(labelHurryupInterval, BorderLayout.WEST)

		// *** HurryupInterval
		val defaultHurryupInterval = propConfig.getProperty("createroom.defaultHurryupInterval", 5)
		spinnerCreateRoomHurryupInterval = JSpinner(SpinnerNumberModel(defaultHurryupInterval, 1, 99, 1))
		spinnerCreateRoomHurryupInterval.preferredSize = Dimension(200, 20)
		spinnerCreateRoomHurryupInterval.toolTipText = getUIText("CreateRoom_HurryupInterval_Tip")
		subpanelHurryupInterval.add(spinnerCreateRoomHurryupInterval, BorderLayout.EAST)

		// ** MapSetIDPanel
		val subpanelMapSetID = JPanel(BorderLayout())
		containerpanelCreateRoomMain.add(subpanelMapSetID)

		// *** &#39;MapSetID:&quot;Label
		val labelMapSetID = JLabel(getUIText("CreateRoom_MapSetID"))
		subpanelMapSetID.add(labelMapSetID, BorderLayout.WEST)

		// *** MapSetID
		val defaultMapSetID = propConfig.getProperty("createroom.defaultMapSetID", 0)
		spinnerCreateRoomMapSetID = JSpinner(SpinnerNumberModel(defaultMapSetID, 0, 99, 1))
		spinnerCreateRoomMapSetID!!.preferredSize = Dimension(200, 20)
		spinnerCreateRoomMapSetID!!.toolTipText = getUIText("CreateRoom_MapSetID_Tip")
		subpanelMapSetID.add(spinnerCreateRoomMapSetID!!, BorderLayout.EAST)

		// ** Map is enabled
		chkboxCreateRoomUseMap = JCheckBox(getUIText("CreateRoom_UseMap"))
		chkboxCreateRoomUseMap.setMnemonic('P')
		chkboxCreateRoomUseMap.isSelected = propConfig.getProperty("createroom.defaultUseMap", false)
		chkboxCreateRoomUseMap.toolTipText = getUIText("CreateRoom_UseMap_Tip")
		containerpanelCreateRoomMain.add(chkboxCreateRoomUseMap)

		// ** Of all fixed rules
		chkboxCreateRoomRuleLock = JCheckBox(getUIText("CreateRoom_RuleLock"))
		chkboxCreateRoomRuleLock.setMnemonic('L')
		chkboxCreateRoomRuleLock.isSelected = propConfig.getProperty("createroom.defaultRuleLock", false)
		chkboxCreateRoomRuleLock.toolTipText = getUIText("CreateRoom_RuleLock_Tip")
		containerpanelCreateRoomMain.add(chkboxCreateRoomRuleLock)

		// speed tab

		// * Speed ​​setting panel(Body)
		val containerpanelCreateRoomSpeed = JPanel()
		containerpanelCreateRoomSpeed.layout = BoxLayout(containerpanelCreateRoomSpeed, BoxLayout.Y_AXIS)
		containerpanelCreateRoomSpeedOwner.add(containerpanelCreateRoomSpeed, BorderLayout.NORTH)

		// ** Fall velocity(Molecule)Panel
		val subpanelGravity = JPanel(BorderLayout())
		containerpanelCreateRoomSpeed.add(subpanelGravity)

		// *** Fall velocity &quot;(Molecule):&quot;Label
		val labelGravity = JLabel(getUIText("CreateRoom_Gravity"))
		subpanelGravity.add(labelGravity, BorderLayout.WEST)

		// *** Fall velocity(Molecule)
		val defaultGravity = propConfig.getProperty("createroom.defaultGravity", 1)
		spinnerCreateRoomGravity = JSpinner(SpinnerNumberModel(defaultGravity, -1, 99999, 1))
		spinnerCreateRoomGravity.preferredSize = Dimension(200, 20)
		subpanelGravity.add(spinnerCreateRoomGravity, BorderLayout.EAST)

		// ** Fall velocity(Denominator)Panel
		val subpanelDenominator = JPanel(BorderLayout())
		containerpanelCreateRoomSpeed.add(subpanelDenominator)

		// *** Fall velocity &quot;(Denominator):&quot;Label
		val labelDenominator = JLabel(getUIText("CreateRoom_Denominator"))
		subpanelDenominator.add(labelDenominator, BorderLayout.WEST)

		// *** Fall velocity(Denominator)
		val defaultDenominator = propConfig.getProperty("createroom.defaultDenominator", 60)
		spinnerCreateRoomDenominator = JSpinner(SpinnerNumberModel(defaultDenominator, 0, 99999, 1))
		spinnerCreateRoomDenominator.preferredSize = Dimension(200, 20)
		subpanelDenominator.add(spinnerCreateRoomDenominator, BorderLayout.EAST)

		// ** AREPanel
		val subpanelARE = JPanel(BorderLayout())
		containerpanelCreateRoomSpeed.add(subpanelARE)

		// *** &#39;ARE:&quot;Label
		val labelARE = JLabel(getUIText("CreateRoom_ARE"))
		subpanelARE.add(labelARE, BorderLayout.WEST)

		// *** ARE
		val defaultARE = propConfig.getProperty("createroom.defaultARE", 0)
		spinnerCreateRoomARE = JSpinner(SpinnerNumberModel(defaultARE, 0, 99, 1))
		spinnerCreateRoomARE.preferredSize = Dimension(200, 20)
		subpanelARE.add(spinnerCreateRoomARE, BorderLayout.EAST)

		// ** ARE after line clearPanel
		val subpanelARELine = JPanel(BorderLayout())
		containerpanelCreateRoomSpeed.add(subpanelARELine)

		// *** &#39;ARE after line clear:&quot;Label
		val labelARELine = JLabel(getUIText("CreateRoom_ARELine"))
		subpanelARELine.add(labelARELine, BorderLayout.WEST)

		// *** ARE after line clear
		val defaultARELine = propConfig.getProperty("createroom.defaultARELine", 0)
		spinnerCreateRoomARELine = JSpinner(SpinnerNumberModel(defaultARELine, 0, 99, 1))
		spinnerCreateRoomARELine.preferredSize = Dimension(200, 20)
		subpanelARELine.add(spinnerCreateRoomARELine, BorderLayout.EAST)

		// ** Line clear timePanel
		val subpanelLineDelay = JPanel(BorderLayout())
		containerpanelCreateRoomSpeed.add(subpanelLineDelay)

		// *** &#39;Line clear time:&quot;Label
		val labelLineDelay = JLabel(getUIText("CreateRoom_LineDelay"))
		subpanelLineDelay.add(labelLineDelay, BorderLayout.WEST)

		// *** Line clear time
		val defaultLineDelay = propConfig.getProperty("createroom.defaultLineDelay", 0)
		spinnerCreateRoomLineDelay = JSpinner(SpinnerNumberModel(defaultLineDelay, 0, 99, 1))
		spinnerCreateRoomLineDelay.preferredSize = Dimension(200, 20)
		subpanelLineDelay.add(spinnerCreateRoomLineDelay, BorderLayout.EAST)

		// ** Fixation timePanel
		val subpanelLockDelay = JPanel(BorderLayout())
		containerpanelCreateRoomSpeed.add(subpanelLockDelay)

		// *** &quot;Fixed time:&quot;Label
		val labelLockDelay = JLabel(getUIText("CreateRoom_LockDelay"))
		subpanelLockDelay.add(labelLockDelay, BorderLayout.WEST)

		// *** Fixation time
		val defaultLockDelay = propConfig.getProperty("createroom.defaultLockDelay", 30)
		spinnerCreateRoomLockDelay = JSpinner(SpinnerNumberModel(defaultLockDelay, 0, 98, 1))
		spinnerCreateRoomLockDelay.preferredSize = Dimension(200, 20)
		subpanelLockDelay.add(spinnerCreateRoomLockDelay, BorderLayout.EAST)

		// ** Panel horizontal reservoir
		val subpanelDAS = JPanel(BorderLayout())
		containerpanelCreateRoomSpeed.add(subpanelDAS)

		// *** Horizontal reservoir &quot;:&quot;Label
		val labelDAS = JLabel(getUIText("CreateRoom_DAS"))
		subpanelDAS.add(labelDAS, BorderLayout.WEST)

		// *** Horizontal reservoir
		val defaultDAS = propConfig.getProperty("createroom.defaultDAS", 11)
		spinnerCreateRoomDAS = JSpinner(SpinnerNumberModel(defaultDAS, 0, 99, 1))
		spinnerCreateRoomDAS.preferredSize = Dimension(200, 20)
		subpanelDAS.add(spinnerCreateRoomDAS, BorderLayout.EAST)

		// bonus tab

		// bonus panel
		val containerpanelCreateRoomBonus = JPanel()
		containerpanelCreateRoomBonus.layout = BoxLayout(containerpanelCreateRoomBonus, BoxLayout.Y_AXIS)
		containerpanelCreateRoomBonusOwner.add(containerpanelCreateRoomBonus, BorderLayout.NORTH)

		// ** Spin bonusPanel
		val subpanelTSpinEnableType = JPanel(BorderLayout())
		containerpanelCreateRoomBonus.add(subpanelTSpinEnableType)

		// *** &quot;Spin bonus:&quot;Label
		val labelTSpinEnableType = JLabel(getUIText("CreateRoom_TSpinEnableType"))
		subpanelTSpinEnableType.add(labelTSpinEnableType, BorderLayout.WEST)

		// *** Spin bonus
		val strSpinBonusNames = arrayOfNulls<String>(COMBOBOX_SPINBONUS_NAMES.size)
		for(i in strSpinBonusNames.indices)
			strSpinBonusNames[i] = getUIText(COMBOBOX_SPINBONUS_NAMES[i])
		comboboxCreateRoomTSpinEnableType = JComboBox(strSpinBonusNames)
		comboboxCreateRoomTSpinEnableType.selectedIndex = propConfig.getProperty("createroom.defaultTspinEnableType", 2)
		comboboxCreateRoomTSpinEnableType.preferredSize = Dimension(200, 20)
		comboboxCreateRoomTSpinEnableType.toolTipText = getUIText("CreateRoom_TSpinEnableType_Tip")
		subpanelTSpinEnableType.add(comboboxCreateRoomTSpinEnableType, BorderLayout.EAST)

		// ** Spin check type panel
		val subpanelSpinCheckType = JPanel(BorderLayout())
		containerpanelCreateRoomBonus.add(subpanelSpinCheckType)

		// *** Spin check type label
		val labelSpinCheckType = JLabel(getUIText("CreateRoom_SpinCheckType"))
		subpanelSpinCheckType.add(labelSpinCheckType, BorderLayout.WEST)

		// *** Spin check type combobox
		val strSpinCheckTypeNames = arrayOfNulls<String>(COMBOBOX_SPINCHECKTYPE_NAMES.size)
		for(i in strSpinCheckTypeNames.indices)
			strSpinCheckTypeNames[i] = getUIText(COMBOBOX_SPINCHECKTYPE_NAMES[i])
		comboboxCreateRoomSpinCheckType = JComboBox(strSpinCheckTypeNames)
		comboboxCreateRoomSpinCheckType.selectedIndex = propConfig.getProperty("createroom.defaultSpinCheckType", 1)
		comboboxCreateRoomSpinCheckType.preferredSize = Dimension(200, 20)
		comboboxCreateRoomSpinCheckType.toolTipText = getUIText("CreateRoom_SpinCheckType_Tip")
		subpanelSpinCheckType.add(comboboxCreateRoomSpinCheckType, BorderLayout.EAST)

		// ** EZ Spin checkbox
		chkboxCreateRoomTSpinEnableEZ = JCheckBox(getUIText("CreateRoom_TSpinEnableEZ"))
		chkboxCreateRoomTSpinEnableEZ.setMnemonic('E')
		chkboxCreateRoomTSpinEnableEZ.isSelected = propConfig.getProperty("createroom.defaultTSpinEnableEZ", false)
		chkboxCreateRoomTSpinEnableEZ.toolTipText = getUIText("CreateRoom_TSpinEnableEZ_Tip")
		containerpanelCreateRoomBonus.add(chkboxCreateRoomTSpinEnableEZ)

		// ** Flag for enabling B2B
		chkboxCreateRoomB2B = JCheckBox(getUIText("CreateRoom_B2B"))
		chkboxCreateRoomB2B.setMnemonic('B')
		chkboxCreateRoomB2B.isSelected = propConfig.getProperty("createroom.defaultB2B", true)
		chkboxCreateRoomB2B.toolTipText = getUIText("CreateRoom_B2B_Tip")
		containerpanelCreateRoomBonus.add(chkboxCreateRoomB2B)

		// ** Flag for enabling combos
		chkboxCreateRoomCombo = JCheckBox(getUIText("CreateRoom_Combo"))
		chkboxCreateRoomCombo.setMnemonic('M')
		chkboxCreateRoomCombo.isSelected = propConfig.getProperty("createroom.defaultCombo", true)
		chkboxCreateRoomCombo.toolTipText = getUIText("CreateRoom_Combo_Tip")
		containerpanelCreateRoomBonus.add(chkboxCreateRoomCombo)

		// ** Bravo bonus
		chkboxCreateRoomBravo = JCheckBox(getUIText("CreateRoom_Bravo"))
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
		val labelGarbagePercent = JLabel(getUIText("CreateRoom_GarbagePercent"))
		subpanelGarbagePercent.add(labelGarbagePercent, BorderLayout.WEST)

		// ** Spinner for garbage change rate
		val defaultGarbagePercent = propConfig.getProperty("createroom.defaultGarbagePercent", 90)
		spinnerCreateRoomGarbagePercent = JSpinner(SpinnerNumberModel(defaultGarbagePercent, 0, 100, 10))
		spinnerCreateRoomGarbagePercent.preferredSize = Dimension(200, 20)
		spinnerCreateRoomGarbagePercent.toolTipText = getUIText("CreateRoom_GarbagePercent_Tip")
		subpanelGarbagePercent.add(spinnerCreateRoomGarbagePercent, BorderLayout.EAST)

		// ** Target timer panel
		val subpanelTargetTimer = JPanel(BorderLayout())
		containerpanelCreateRoomGarbage.add(subpanelTargetTimer)

		// ** Label for target timer
		val labelTargetTimer = JLabel(getUIText("CreateRoom_TargetTimer"))
		subpanelTargetTimer.add(labelTargetTimer, BorderLayout.WEST)

		// ** Spinner for target timer
		val defaultTargetTimer = propConfig.getProperty("createroom.defaultTargetTimer", 60)
		spinnerCreateRoomTargetTimer = JSpinner(SpinnerNumberModel(defaultTargetTimer, 0, 3600, 1))
		spinnerCreateRoomTargetTimer.preferredSize = Dimension(200, 20)
		spinnerCreateRoomTargetTimer.toolTipText = getUIText("CreateRoom_TargetTimer_Tip")
		subpanelTargetTimer.add(spinnerCreateRoomTargetTimer, BorderLayout.EAST)

		// ** Set garbage type
		chkboxCreateRoomGarbageChangePerAttack = JCheckBox(getUIText("CreateRoom_GarbageChangePerAttack"))
		chkboxCreateRoomGarbageChangePerAttack.setMnemonic('G')
		chkboxCreateRoomGarbageChangePerAttack.isSelected =
			propConfig.getProperty("createroom.defaultGarbageChangePerAttack", true)
		chkboxCreateRoomGarbageChangePerAttack.toolTipText = getUIText("CreateRoom_GarbageChangePerAttack_Tip")
		containerpanelCreateRoomGarbage.add(chkboxCreateRoomGarbageChangePerAttack)

		// ** Divide change rate by live players/teams
		chkboxCreateRoomDivideChangeRateByPlayers = JCheckBox(getUIText("CreateRoom_DivideChangeRateByPlayers"))
		chkboxCreateRoomDivideChangeRateByPlayers.isSelected =
			propConfig.getProperty("createroom.defaultDivideChangeRateByPlayers", false)
		chkboxCreateRoomDivideChangeRateByPlayers.toolTipText = getUIText("CreateRoom_DivideChangeRateByPlayers_Tip")
		containerpanelCreateRoomGarbage.add(chkboxCreateRoomDivideChangeRateByPlayers)

		// ** B2B chunk
		chkboxCreateRoomB2BChunk = JCheckBox(getUIText("CreateRoom_B2BChunk"))
		chkboxCreateRoomB2BChunk.setMnemonic('B')
		chkboxCreateRoomB2BChunk.isSelected = propConfig.getProperty("createroom.defaultB2BChunk", false)
		chkboxCreateRoomB2BChunk.toolTipText = getUIText("CreateRoom_B2BChunk_Tip")
		containerpanelCreateRoomGarbage.add(chkboxCreateRoomB2BChunk)

		// ** Rensa/Combo Block
		chkboxCreateRoomRensaBlock = JCheckBox(getUIText("CreateRoom_RensaBlock"))
		chkboxCreateRoomRensaBlock.setMnemonic('E')
		chkboxCreateRoomRensaBlock.isSelected = propConfig.getProperty("createroom.defaultRensaBlock", true)
		chkboxCreateRoomRensaBlock.toolTipText = getUIText("CreateRoom_RensaBlock_Tip")
		containerpanelCreateRoomGarbage.add(chkboxCreateRoomRensaBlock)

		// ** Garbage countering
		chkboxCreateRoomCounter = JCheckBox(getUIText("CreateRoom_Counter"))
		chkboxCreateRoomCounter.setMnemonic('C')
		chkboxCreateRoomCounter.isSelected = propConfig.getProperty("createroom.defaultCounter", true)
		chkboxCreateRoomCounter.toolTipText = getUIText("CreateRoom_Counter_Tip")
		containerpanelCreateRoomGarbage.add(chkboxCreateRoomCounter)

		// ** 3If I live more than Attack Reduce the force
		chkboxCreateRoomReduceLineSend = JCheckBox(getUIText("CreateRoom_ReduceLineSend"))
		chkboxCreateRoomReduceLineSend.setMnemonic('R')
		chkboxCreateRoomReduceLineSend.isSelected = propConfig.getProperty("createroom.defaultReduceLineSend", true)
		chkboxCreateRoomReduceLineSend.toolTipText = getUIText("CreateRoom_ReduceLineSend_Tip")
		containerpanelCreateRoomGarbage.add(chkboxCreateRoomReduceLineSend)

		// ** Fragmentarygarbage blockUsing the system
		chkboxCreateRoomUseFractionalGarbage = JCheckBox(getUIText("CreateRoom_UseFractionalGarbage"))
		chkboxCreateRoomUseFractionalGarbage.setMnemonic('F')
		chkboxCreateRoomUseFractionalGarbage.isSelected =
			propConfig.getProperty("createroom.defaultUseFractionalGarbage", false)
		chkboxCreateRoomUseFractionalGarbage.toolTipText = getUIText("CreateRoom_UseFractionalGarbage_Tip")
		containerpanelCreateRoomGarbage.add(chkboxCreateRoomUseFractionalGarbage)

		// *** Use target system
		chkboxCreateRoomIsTarget = JCheckBox(getUIText("CreateRoom_IsTarget"))
		chkboxCreateRoomIsTarget.setMnemonic('T')
		chkboxCreateRoomIsTarget.isSelected = propConfig.getProperty("createroom.defaultIsTarget", false)
		chkboxCreateRoomIsTarget.toolTipText = getUIText("CreateRoom_IsTarget_Tip")
		containerpanelCreateRoomGarbage.add(chkboxCreateRoomIsTarget)

		// misc tab

		// misc panel
		val containerpanelCreateRoomMisc = JPanel()
		containerpanelCreateRoomMisc.layout = BoxLayout(containerpanelCreateRoomMisc, BoxLayout.Y_AXIS)
		containerpanelCreateRoomMiscOwner.add(containerpanelCreateRoomMisc, BorderLayout.NORTH)

		// ** To wait before auto-start timePanel
		val subpanelAutoStartSeconds = JPanel(BorderLayout())
		containerpanelCreateRoomMisc.add(subpanelAutoStartSeconds)

		// *** To wait before auto-start &quot; time:&quot;Label
		val labelAutoStartSeconds = JLabel(getUIText("CreateRoom_AutoStartSeconds"))
		subpanelAutoStartSeconds.add(labelAutoStartSeconds, BorderLayout.WEST)

		// *** To wait before auto-start time
		val defaultAutoStartSeconds = propConfig.getProperty("createroom.defaultAutoStartSeconds", 15)
		spinnerCreateRoomAutoStartSeconds = JSpinner(SpinnerNumberModel(defaultAutoStartSeconds, 0, 999, 1))
		spinnerCreateRoomAutoStartSeconds.preferredSize = Dimension(200, 20)
		spinnerCreateRoomAutoStartSeconds.toolTipText = getUIText("CreateRoom_AutoStartSeconds_Tip")
		subpanelAutoStartSeconds.add(spinnerCreateRoomAutoStartSeconds, BorderLayout.EAST)

		// ** TNET2TypeAutomatically start timerI use
		chkboxCreateRoomAutoStartTNET2 = JCheckBox(getUIText("CreateRoom_AutoStartTNET2"))
		chkboxCreateRoomAutoStartTNET2.setMnemonic('A')
		chkboxCreateRoomAutoStartTNET2.isSelected = propConfig.getProperty("createroom.defaultAutoStartTNET2", false)
		chkboxCreateRoomAutoStartTNET2.toolTipText = getUIText("CreateRoom_AutoStartTNET2_Tip")
		containerpanelCreateRoomMisc.add(chkboxCreateRoomAutoStartTNET2)

		// ** SomeoneCancelWasTimerInvalidation
		chkboxCreateRoomDisableTimerAfterSomeoneCancelled =
			JCheckBox(getUIText("CreateRoom_DisableTimerAfterSomeoneCancelled"))
		chkboxCreateRoomDisableTimerAfterSomeoneCancelled.setMnemonic('D')
		chkboxCreateRoomDisableTimerAfterSomeoneCancelled.isSelected =
			propConfig.getProperty("createroom.defaultDisableTimerAfterSomeoneCancelled", false)
		chkboxCreateRoomDisableTimerAfterSomeoneCancelled.toolTipText =
			getUIText("CreateRoom_DisableTimerAfterSomeoneCancelled_Tip")
		containerpanelCreateRoomMisc.add(chkboxCreateRoomDisableTimerAfterSomeoneCancelled)

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
		val labelPresetID = JLabel(getUIText("CreateRoom_PresetID"))
		subpanelPresetID.add(labelPresetID, BorderLayout.WEST)

		// *** Preset number selector
		val defaultPresetID = propConfig.getProperty("createroom.defaultPresetID", 0)
		spinnerCreateRoomPresetID = JSpinner(SpinnerNumberModel(defaultPresetID, 0, 999, 1))
		spinnerCreateRoomPresetID.preferredSize = Dimension(200, 20)
		subpanelPresetID.add(spinnerCreateRoomPresetID, BorderLayout.EAST)

		// ** Save button
		val btnPresetSave = JButton(getUIText("CreateRoom_PresetSave"))
		btnPresetSave.alignmentX = 0f
		btnPresetSave.addActionListener(this)
		btnPresetSave.actionCommand = "CreateRoom_PresetSave"
		btnPresetSave.setMnemonic('S')
		btnPresetSave.maximumSize = Dimension(java.lang.Short.MAX_VALUE.toInt(), btnPresetSave.maximumSize.height)
		containerpanelCreateRoomPreset.add(btnPresetSave)

		// ** Load button
		val btnPresetLoad = JButton(getUIText("CreateRoom_PresetLoad"))
		btnPresetLoad.alignmentX = 0f
		btnPresetLoad.addActionListener(this)
		btnPresetLoad.actionCommand = "CreateRoom_PresetLoad"
		btnPresetLoad.setMnemonic('L')
		btnPresetLoad.maximumSize = Dimension(java.lang.Short.MAX_VALUE.toInt(), btnPresetLoad.maximumSize.height)
		containerpanelCreateRoomPreset.add(btnPresetLoad)

		// ** Preset code panel
		val subpanelPresetCode = JPanel(BorderLayout())
		subpanelPresetCode.alignmentX = 0f
		containerpanelCreateRoomPreset.add(subpanelPresetCode)

		// *** "Preset code:" Label
		val labelPresetCode = JLabel(getUIText("CreateRoom_PresetCode"))
		subpanelPresetCode.add(labelPresetCode, BorderLayout.WEST)

		// *** Preset code textfield
		txtfldCreateRoomPresetCode = JTextField()
		txtfldCreateRoomPresetCode.componentPopupMenu = TextComponentPopupMenu(txtfldCreateRoomPresetCode)
		subpanelPresetCode.add(txtfldCreateRoomPresetCode, BorderLayout.CENTER)

		// *** Preset code export
		val btnPresetCodeExport = JButton(getUIText("CreateRoom_PresetCodeExport"))
		btnPresetCodeExport.alignmentX = 0f
		btnPresetCodeExport.addActionListener(this)
		btnPresetCodeExport.actionCommand = "CreateRoom_PresetCodeExport"
		btnPresetCodeExport.setMnemonic('E')
		btnPresetCodeExport.maximumSize = Dimension(java.lang.Short.MAX_VALUE.toInt(), btnPresetCodeExport.maximumSize.height)
		containerpanelCreateRoomPreset.add(btnPresetCodeExport)

		// *** Preset code import
		val btnPresetCodeImport = JButton(getUIText("CreateRoom_PresetCodeImport"))
		btnPresetCodeImport.alignmentX = 0f
		btnPresetCodeImport.addActionListener(this)
		btnPresetCodeImport.actionCommand = "CreateRoom_PresetCodeImport"
		btnPresetCodeImport.setMnemonic('I')
		btnPresetCodeImport.maximumSize = Dimension(java.lang.Short.MAX_VALUE.toInt(), btnPresetCodeImport.maximumSize.height)
		containerpanelCreateRoomPreset.add(btnPresetCodeImport)

		// buttons

		// **  buttonPanel type
		val subpanelButtons = JPanel()
		subpanelButtons.layout = BoxLayout(subpanelButtons, BoxLayout.X_AXIS)
		//containerpanelCreateRoom.add(subpanelButtons);
		mainpanelCreateRoom.add(subpanelButtons, BorderLayout.SOUTH)

		// *** OK button
		btnCreateRoomOK = JButton(getUIText("CreateRoom_OK"))
		btnCreateRoomOK.addActionListener(this)
		btnCreateRoomOK.actionCommand = "CreateRoom_OK"
		btnCreateRoomOK.setMnemonic('O')
		btnCreateRoomOK.maximumSize = Dimension(java.lang.Short.MAX_VALUE.toInt(), btnCreateRoomOK.maximumSize.height)
		subpanelButtons.add(btnCreateRoomOK)

		// *** Participation in a war button
		btnCreateRoomJoin = JButton(getUIText("CreateRoom_Join"))
		btnCreateRoomJoin.addActionListener(this)
		btnCreateRoomJoin.actionCommand = "CreateRoom_Join"
		btnCreateRoomJoin.setMnemonic('J')
		btnCreateRoomJoin.maximumSize = Dimension(java.lang.Short.MAX_VALUE.toInt(), btnCreateRoomJoin.maximumSize.height)
		subpanelButtons.add(btnCreateRoomJoin)

		// *** Participation in a war button
		btnCreateRoomWatch = JButton(getUIText("CreateRoom_Watch"))
		btnCreateRoomWatch.addActionListener(this)
		btnCreateRoomWatch.actionCommand = "CreateRoom_Watch"
		btnCreateRoomWatch.setMnemonic('W')
		btnCreateRoomWatch.maximumSize = Dimension(java.lang.Short.MAX_VALUE.toInt(), btnCreateRoomWatch.maximumSize.height)
		subpanelButtons.add(btnCreateRoomWatch)

		// *** Cancel Button
		btnCreateRoomCancel = JButton(getUIText("CreateRoom_Cancel"))
		btnCreateRoomCancel.addActionListener(this)
		btnCreateRoomCancel.actionCommand = "CreateRoom_Cancel"
		btnCreateRoomCancel.setMnemonic('C')
		btnCreateRoomCancel.maximumSize = Dimension(java.lang.Short.MAX_VALUE.toInt(), btnCreateRoomCancel.maximumSize.height)
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

		labelCreateRoom1PGameMode = JLabel(getUIText("CreateRoom1P_Mode_Label"))
		pModeList.add(labelCreateRoom1PGameMode, BorderLayout.NORTH)

		// ** Game mode listbox
		listmodelCreateRoom1PModeList = DefaultListModel()
		loadModeList(listmodelCreateRoom1PModeList, "config/list/netlobby_singlemode.lst")

		listboxCreateRoom1PModeList = JList(listmodelCreateRoom1PModeList)
		listboxCreateRoom1PModeList.addListSelectionListener {e ->
			val strMode = listboxCreateRoom1PModeList.selectedValue as String
			labelCreateRoom1PGameMode.text = getModeDesc(strMode)
		}
		listboxCreateRoom1PModeList.setSelectedValue(propConfig.getProperty("createroom1p.listboxCreateRoom1PModeList.value", ""), true)
		val spCreateRoom1PModeList = JScrollPane(listboxCreateRoom1PModeList)
		pModeList.add(spCreateRoom1PModeList, BorderLayout.CENTER)

		// * Rule list panel
		val pRuleList = JPanel(BorderLayout())
		mainpanelCreateRoom1P.add(pRuleList)

		// ** "Rule:" label
		val lCreateRoom1PRuleList = JLabel(getUIText("CreateRoom1P_Rule_Label"))
		pRuleList.add(lCreateRoom1PRuleList, BorderLayout.NORTH)

		// ** Rule list listbox
		listmodelCreateRoom1PRuleList = DefaultListModel<String>()
		listboxCreateRoom1PRuleList = JList(listmodelCreateRoom1PRuleList)
		val spCreateRoom1PRuleList = JScrollPane(listboxCreateRoom1PRuleList)
		pRuleList.add(spCreateRoom1PRuleList, BorderLayout.CENTER)

		// * Buttons panel
		val subpanelButtons = JPanel()
		subpanelButtons.layout = BoxLayout(subpanelButtons, BoxLayout.X_AXIS)
		mainpanelCreateRoom1P.add(subpanelButtons)

		// ** OK button
		btnCreateRoom1POK = JButton(getUIText("CreateRoom1P_OK"))
		btnCreateRoom1POK.addActionListener(this)
		btnCreateRoom1POK.actionCommand = "CreateRoom1P_OK"
		btnCreateRoom1POK.setMnemonic('O')
		btnCreateRoom1POK.maximumSize = Dimension(java.lang.Short.MAX_VALUE.toInt(), btnCreateRoom1POK.maximumSize.height)
		subpanelButtons.add(btnCreateRoom1POK)

		// ** Cancel button
		btnCreateRoom1PCancel = JButton(getUIText("CreateRoom1P_Cancel"))
		btnCreateRoom1PCancel.addActionListener(this)
		btnCreateRoom1PCancel.actionCommand = "CreateRoom1P_Cancel"
		btnCreateRoom1PCancel.setMnemonic('C')
		btnCreateRoom1PCancel.maximumSize =
			Dimension(java.lang.Short.MAX_VALUE.toInt(), btnCreateRoom1PCancel.maximumSize.height)
		subpanelButtons.add(btnCreateRoom1PCancel)
	}

	/** MPRanking screen initialization */
	private fun initMPRankingUI() {
		// Main panel for MPRanking
		val mainpanelMPRanking = JPanel(BorderLayout())
		contentPane.add(mainpanelMPRanking, SCREENCARD_NAMES[SCREENCARD_MPRANKING])

		// * Tab
		tabMPRanking = JTabbedPane()
		mainpanelMPRanking.add(tabMPRanking, BorderLayout.CENTER)

		// ** Leaderboard Table
		strMPRankingTableColumnNames = Array(MPRANKING_COLUMNNAMES.size) {getUIText(MPRANKING_COLUMNNAMES[it])}

		tableMPRanking = Array(GameEngine.MAX_GAMESTYLE) {
			JTable(tablemodelMPRanking[it]).apply {
				setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
				setDefaultEditor(Any::class.java, null)
				autoResizeMode = JTable.AUTO_RESIZE_OFF
				tableHeader.reorderingAllowed = false
				componentPopupMenu = TablePopupMenu(tableMPRanking[it])
			}
		}
		tablemodelMPRanking = Array(GameEngine.MAX_GAMESTYLE) {DefaultTableModel(strMPRankingTableColumnNames, 0)}

		for(i in 0 until GameEngine.MAX_GAMESTYLE) {
			val tm = tableMPRanking[i].columnModel
			tm.getColumn(0).preferredWidth = propConfig.getProperty("tableMPRanking.width.rank", 30) // Rank
			tm.getColumn(1).preferredWidth = propConfig.getProperty("tableMPRanking.width.name", 200) // Name
			tm.getColumn(2).preferredWidth = propConfig.getProperty("tableMPRanking.width.rating", 60) // Rating
			tm.getColumn(3).preferredWidth = propConfig.getProperty("tableMPRanking.width.play", 60) // Play
			tm.getColumn(4).preferredWidth = propConfig.getProperty("tableMPRanking.width.win", 60) // Win

			val spMPRanking = JScrollPane(tableMPRanking[i])
			tabMPRanking.addTab(GameEngine.GAMESTYLE_NAMES[i], spMPRanking)

			if(i!=GameEngine.GAMESTYLE_TETROMINO) tabMPRanking.setEnabledAt(i, false) // TODO: Add non-tetromino leaderboard
		}

		// * OK Button
		btnMPRankingOK = JButton(getUIText("MPRanking_OK"))
		btnMPRankingOK.addActionListener(this)
		btnMPRankingOK.actionCommand = "MPRanking_OK"
		btnMPRankingOK.setMnemonic('O')
		mainpanelMPRanking.add(btnMPRankingOK, BorderLayout.SOUTH)
	}

	/** Rule change screen initialization */
	private fun initRuleChangeUI() {
		// Main panel for RuleChange
		val mainpanelRuleChange = JPanel(BorderLayout())
		contentPane.add(mainpanelRuleChange, SCREENCARD_NAMES[SCREENCARD_RULECHANGE])

		// * Tab
		tabRuleChange = JTabbedPane()
		mainpanelRuleChange.add(tabRuleChange, BorderLayout.CENTER)

		// ** Rule Listboxes
		listboxRuleChangeRuleList = Array(GameEngine.MAX_GAMESTYLE) {JList(extractRuleListFromRuleEntries(it))}
		for(i in 0 until GameEngine.MAX_GAMESTYLE) {
			val spRuleList = JScrollPane(listboxRuleChangeRuleList[i])
			tabRuleChange.addTab(GameEngine.GAMESTYLE_NAMES[i], spRuleList)
		}

		// ** Tuning Tab
		val subpanelTuning = JPanel()
		subpanelTuning.layout = BoxLayout(subpanelTuning, BoxLayout.Y_AXIS)
		tabRuleChange.addTab(getUIText("RuleChange_Tab_Tuning"), subpanelTuning)

		// *** A button rotate
		val pTuningRotateButtonDefaultRight = JPanel()
		//pTuningRotateButtonDefaultRight.setLayout(new BoxLayout(pTuningRotateButtonDefaultRight, BoxLayout.Y_AXIS));
		pTuningRotateButtonDefaultRight.alignmentX = Component.LEFT_ALIGNMENT
		subpanelTuning.add(pTuningRotateButtonDefaultRight)

		val lTuningRotateButtonDefaultRight = JLabel(getUIText("GameTuning_RotateButtonDefaultRight_Label"))
		pTuningRotateButtonDefaultRight.add(lTuningRotateButtonDefaultRight)

		val strArrayTuningRotateButtonDefaultRight = arrayOfNulls<String>(TUNING_ABUTTON_ROTATE.size)
		for(i in TUNING_ABUTTON_ROTATE.indices)
			strArrayTuningRotateButtonDefaultRight[i] = getUIText(TUNING_ABUTTON_ROTATE[i])
		comboboxTuningRotateButtonDefaultRight = JComboBox(strArrayTuningRotateButtonDefaultRight)
		pTuningRotateButtonDefaultRight.add(comboboxTuningRotateButtonDefaultRight)

		// *** Diagonal move
		val pTuningMoveDiagonal = JPanel()
		pTuningMoveDiagonal.alignmentX = Component.LEFT_ALIGNMENT
		subpanelTuning.add(pTuningMoveDiagonal)

		val lTuningMoveDiagonal = JLabel(getUIText("GameTuning_MoveDiagonal_Label"))
		pTuningMoveDiagonal.add(lTuningMoveDiagonal)

		val strArrayTuningMoveDiagonal = arrayOfNulls<String>(TUNING_COMBOBOX_GENERIC.size)
		for(i in TUNING_COMBOBOX_GENERIC.indices)
			strArrayTuningMoveDiagonal[i] = getUIText(TUNING_COMBOBOX_GENERIC[i])
		comboboxTuningMoveDiagonal = JComboBox(strArrayTuningMoveDiagonal)
		pTuningMoveDiagonal.add(comboboxTuningMoveDiagonal)

		// *** Show Outline Only
		val pTuningBlockShowOutlineOnly = JPanel()
		pTuningBlockShowOutlineOnly.alignmentX = Component.LEFT_ALIGNMENT
		subpanelTuning.add(pTuningBlockShowOutlineOnly)

		val lTuningBlockShowOutlineOnly = JLabel(getUIText("GameTuning_BlockShowOutlineOnly_Label"))
		pTuningBlockShowOutlineOnly.add(lTuningBlockShowOutlineOnly)

		val strArrayTuningBlockShowOutlineOnly = arrayOfNulls<String>(TUNING_COMBOBOX_GENERIC.size)
		for(i in TUNING_COMBOBOX_GENERIC.indices)
			strArrayTuningBlockShowOutlineOnly[i] = getUIText(TUNING_COMBOBOX_GENERIC[i])
		comboboxTuningBlockShowOutlineOnly = JComboBox(strArrayTuningBlockShowOutlineOnly)
		pTuningBlockShowOutlineOnly.add(comboboxTuningBlockShowOutlineOnly)

		// *** Outline Type
		val pTuningOutlineType = JPanel()
		pTuningOutlineType.alignmentX = Component.LEFT_ALIGNMENT
		subpanelTuning.add(pTuningOutlineType)

		val lTuningOutlineType = JLabel(getUIText("GameTuning_OutlineType_Label"))
		pTuningOutlineType.add(lTuningOutlineType)

		val strArrayTuningOutlineType = arrayOfNulls<String>(TUNING_OUTLINE_TYPE_NAMES.size)
		for(i in TUNING_OUTLINE_TYPE_NAMES.indices)
			strArrayTuningOutlineType[i] = getUIText(TUNING_OUTLINE_TYPE_NAMES[i])
		val modelTuningOutlineType = DefaultComboBoxModel(strArrayTuningOutlineType)
		comboboxTuningBlockOutlineType = JComboBox(modelTuningOutlineType)
		pTuningOutlineType.add(comboboxTuningBlockOutlineType)

		// *** Skin
		val pTuningSkin = JPanel()
		pTuningSkin.alignmentX = Component.LEFT_ALIGNMENT
		subpanelTuning.add(pTuningSkin)

		val lTuningSkin = JLabel(getUIText("GameTuning_Skin_Label"))
		pTuningSkin.add(lTuningSkin)

		val model = DefaultComboBoxModel<ComboLabel>()
		model.addElement(ComboLabel(getUIText("GameTuning_Skin_Auto")))
		for(i in imgTuningBlockSkins.indices)
			model.addElement(ComboLabel(""+i, ImageIcon(imgTuningBlockSkins[i])))

		comboboxTuningSkin = JComboBox(model)
		comboboxTuningSkin.renderer = ComboLabelCellRenderer()
		comboboxTuningSkin.preferredSize = Dimension(190, 30)
		pTuningSkin.add(comboboxTuningSkin)

		// *** Minimum DAS
		val pTuningMinDAS = JPanel()
		pTuningMinDAS.alignmentX = Component.LEFT_ALIGNMENT
		subpanelTuning.add(pTuningMinDAS)

		val lTuningMinDAS = JLabel(getUIText("GameTuning_MinDAS_Label"))
		pTuningMinDAS.add(lTuningMinDAS)

		txtfldTuningMinDAS = JTextField(5)
		pTuningMinDAS.add(txtfldTuningMinDAS)

		// *** Maximum DAS
		val pTuningMaxDAS = JPanel()
		pTuningMaxDAS.alignmentX = Component.LEFT_ALIGNMENT
		subpanelTuning.add(pTuningMaxDAS)

		val lTuningMaxDAS = JLabel(getUIText("GameTuning_MaxDAS_Label"))
		pTuningMaxDAS.add(lTuningMaxDAS)

		txtfldTuningMaxDAS = JTextField(5)
		pTuningMaxDAS.add(txtfldTuningMaxDAS)

		// *** DAS delay
		val pTuningDasDelay = JPanel()
		pTuningDasDelay.alignmentX = Component.LEFT_ALIGNMENT
		subpanelTuning.add(pTuningDasDelay)

		val lTuningDasDelay = JLabel(getUIText("GameTuning_DasDelay_Label"))
		pTuningDasDelay.add(lTuningDasDelay)

		txtfldTuningDasDelay = JTextField(5)
		pTuningDasDelay.add(txtfldTuningDasDelay)

		// *** Reverse Up/Down
		val pTuningReverseUpDown = JPanel()
		pTuningReverseUpDown.alignmentX = Component.LEFT_ALIGNMENT
		subpanelTuning.add(pTuningReverseUpDown)

		val lTuningReverseUpDown = JLabel(getUIText("GameTuning_ReverseUpDown_Label"))
		pTuningReverseUpDown.add(lTuningReverseUpDown)

		chkboxTuningReverseUpDown = JCheckBox()
		pTuningReverseUpDown.add(chkboxTuningReverseUpDown)

		// * Buttons panel
		val subpanelButtons = JPanel()
		subpanelButtons.layout = BoxLayout(subpanelButtons, BoxLayout.X_AXIS)
		mainpanelRuleChange.add(subpanelButtons, BorderLayout.SOUTH)

		// ** OK button
		btnRuleChangeOK = JButton(getUIText("RuleChange_OK"))
		btnRuleChangeOK.addActionListener(this)
		btnRuleChangeOK.actionCommand = "RuleChange_OK"
		btnRuleChangeOK.setMnemonic('O')
		btnRuleChangeOK.maximumSize = Dimension(java.lang.Short.MAX_VALUE.toInt(), btnRuleChangeOK.maximumSize.height)
		subpanelButtons.add(btnRuleChangeOK)

		// ** Cancel button
		btnRuleChangeCancel = JButton(getUIText("RuleChange_Cancel"))
		btnRuleChangeCancel.addActionListener(this)
		btnRuleChangeCancel.actionCommand = "RuleChange_Cancel"
		btnRuleChangeCancel.setMnemonic('C')
		btnRuleChangeCancel.maximumSize = Dimension(java.lang.Short.MAX_VALUE.toInt(), btnRuleChangeCancel.maximumSize.height)
		subpanelButtons.add(btnRuleChangeCancel)
	}

	/** Load block skins */
	private fun loadBlockSkins() {
		val skindir = propGlobal.getProperty("custom.skin.directory", "res")

		var numSkins = 0
		while(File("$skindir/graphics/blockskin/normal/n$numSkins.png").canRead()) {
			numSkins++
		}
		log.debug(numSkins.toString()+" block skins found")

		imgTuningBlockSkins = Array(numSkins) {i ->
			val imgBlock = loadImage(getURL("$skindir/graphics/blockskin/normal/n$i.png"))
			val isSticky = imgBlock!=null&&imgBlock.width>=400&&imgBlock.height>=304
			if(isSticky)
				for(j in 0..8)
					imgTuningBlockSkins[i].graphics.drawImage(imgBlock, j*16, 0, j*16+16, 16, 0, j*16, 16, j*16+16, null)
			else
				imgTuningBlockSkins[i].graphics.drawImage(imgBlock, 0, 0, 144, 16, 0, 0, 144, 16, null)

			return@Array BufferedImage(144, 16, BufferedImage.TYPE_INT_RGB)

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
			log.debug("Loaded image from $url")
		} catch(e:IOException) {
			log.error("Failed to load image from "+url!!, e)
		}

		return img
	}

	/** Get the URL of filename
	 * @param str Filename
	 * @return URL of the filename
	 */
	private fun getURL(str:String):URL? {
		val url:URL

		try {
			val sep = File.separator[0]
			var file = str.replace(sep, '/')

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
		var str2 = str.replace(' ', '_')
		str2 = str2.replace('(', 'l')
		str2 = str2.replace(')', 'r')
		return propModeDesc.getProperty(str2) ?: propDefaultModeDesc.getProperty(str2, str2) ?: str2
	}

	/** Screen switching
	 * @param cardNumber Card switching destination screen number
	 */
	fun changeCurrentScreenCard(cardNumber:Int) {
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
			// TODO: There are several threading issue here
			log.debug("changeCurrentScreenCard failed; Possible threading issue", e)
		}

	}

	/** Create String from a Calendar (for chat log)
	 * @param cal Calendar
	 * @param showDate true to show date
	 * @return String created from Calendar
	 */
	@JvmOverloads fun getTimeAsString(cal:Calendar?, showDate:Boolean = false):String {
		if(cal==null) return if(showDate) "????-??-?? ??:??:??" else "??:??:??"
		val strFormat = if(showDate) "yyyy-MM-dd HH:mm:ss" else "HH:mm:ss"
		val dfm = SimpleDateFormat(strFormat)
		return dfm.format(cal.time)
	}

	/** PlayerOfNameObtained by converting symbol trip
	 * @param pInfo PlayerInformation
	 * @return PlayerOfName(Translated symbol trip)
	 */
	fun getPlayerNameWithTripCode(pInfo:NetPlayerInfo?):String = convTripCode(pInfo!!.strName)

	/** Convert the symbol trip
	 * @param s String to be converted(MostName)
	 * @return The converted string
	 */
	fun convTripCode(s:String):String {
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
	@JvmOverloads fun addSystemChatLog(txtpane:JTextPane, str:String?, fgcolor:Color? = null) {
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

			if(txtpane===txtpaneRoomChatLog) {
				if(writerRoomLog!=null) {
					writerRoomLog!!.println("[$strTime] $str")
					writerRoomLog!!.flush()
				}
			} else if(writerLobbyLog!=null) {
				writerLobbyLog!!.println("[$strTime] $str")
				writerLobbyLog!!.flush()
			}
		} catch(e:Exception) {
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
	fun addSystemChatLogLater(txtpane:JTextPane, str:String?, fgcolor:Color) {
		SwingUtilities.invokeLater {addSystemChatLog(txtpane, str, fgcolor)}
	}

	/** Add a user chat to log pane
	 * @param txtpane JTextPane to add this chat log
	 * @param username User name
	 * @param calendar Time
	 * @param str Message
	 */
	fun addUserChatLog(txtpane:JTextPane, username:String, calendar:Calendar?, str:String) {
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

			if(txtpane===txtpaneRoomChatLog) {
				if(writerRoomLog!=null) {
					writerRoomLog!!.println("[$strTime]<$username> $str")
					writerRoomLog!!.flush()
				}
			} else if(writerLobbyLog!=null) {
				writerLobbyLog!!.println("[$strTime]<$username> $str")
				writerLobbyLog!!.flush()
			}
		} catch(e:Exception) {
		}

	}

	/** Add a user chat to log pane (for multi threading)
	 * @param txtpane JTextPane to add this chat log
	 * @param username User name
	 * @param calendar Time
	 * @param str Message
	 */
	fun addUserChatLogLater(txtpane:JTextPane, username:String, calendar:Calendar?,
		str:String) {
		SwingUtilities.invokeLater {addUserChatLog(txtpane, username, calendar, str)}
	}

	/** Add a recorded user chat to log pane
	 * @param txtpane JTextPane to add this chat log
	 * @param username User name
	 * @param calendar Time
	 * @param str Message
	 */
	fun addRecordedUserChatLog(txtpane:JTextPane, username:String, calendar:Calendar?, str:String) {
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

			if(txtpane===txtpaneRoomChatLog) {
				if(writerRoomLog!=null) {
					writerRoomLog!!.println("[$strTime]<$username> $str")
					writerRoomLog!!.flush()
				}
			} else if(writerLobbyLog!=null) {
				writerLobbyLog!!.println("[$strTime]<$username> $str")
				writerLobbyLog!!.flush()
			}
		} catch(e:Exception) {
		}

	}

	/** Add a recorded user chat to log pane (for multi threading)
	 * @param txtpane JTextPane to add this chat log
	 * @param username User name
	 * @param calendar Time
	 * @param str Message
	 */
	fun addRecordedUserChatLogLater(txtpane:JTextPane, username:String, calendar:Calendar?,
		str:String) {
		SwingUtilities.invokeLater {addRecordedUserChatLog(txtpane, username, calendar, str)}
	}

	/** FileDefaultListModelRead on
	 * @param listModel Which to readDefaultListModel
	 * @param filename Filename
	 * @return The successtrue
	 */
	fun loadListToDefaultListModel(listModel:DefaultListModel<String>, filename:String):Boolean {
		try {
			val `in` = BufferedReader(FileReader(filename))
			listModel.clear()

			`in`.readLines().forEach {str ->
				if(str.isNotEmpty()) listModel.addElement(str)
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
	fun loadModeList(listModel:DefaultListModel<String>, filename:String):Boolean {
		try {
			val `in` = BufferedReader(FileReader(filename))
			listModel.clear()

			`in`.readLines().forEach {str ->
				if(str.isEmpty()||str.startsWith("#")) {
					// Empty line or comment line. Ignore it.
				} else if(str.startsWith(":")) {
					// Game style tag. Currently unused.
				} else {
					// Game mode name
					val commaIndex = str.indexOf(',')
					if(commaIndex!=-1) listModel.addElement(str.substring(0, commaIndex))
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
	fun loadModeList(listModel:DefaultComboBoxModel<String>, filename:String):Boolean {
		try {
			val `in` = BufferedReader(FileReader(filename))

			listModel.removeAllElements()

			`in`.readLines().forEach {str ->
				if(str.isEmpty()||str.startsWith("#")) {
					// Empty line or comment line. Ignore it.
				} else if(str.startsWith(":")) {
					// Game style tag. Currently unused.
				} else {
					// Game mode name
					val commaIndex = str.indexOf(',')
					if(commaIndex!=-1) listModel.addElement(str.substring(0, commaIndex))
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
	fun saveListFromDefaultListModel(listModel:DefaultListModel<*>, filename:String):Boolean {
		try {
			val out = PrintWriter(filename)
			for(i in 0 until listModel.size())
				out.println(listModel.get(i))
			out.flush()
			out.close()
		} catch(e:IOException) {
			log.debug("Failed to save server list", e)
			return false
		}

		return true
	}

	/** Set enabled state of lobby buttons
	 * @param mode 0=Disable all 1=Full Lobby 2=Inside Room
	 */
	fun setLobbyButtonsEnabled(mode:Int) {
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
	fun setRoomButtonsEnabled(b:Boolean) {
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
	fun setRoomJoinButtonVisible(b:Boolean) {
		btnRoomButtonsJoin.isVisible = b
		btnRoomButtonsJoin.isEnabled = true
		btnRoomButtonsSitOut.isVisible = !b
		btnRoomButtonsSitOut.isEnabled = true
	}

	/** Room list tableRow dataCreate
	 * @param r Room Information
	 * @return Line data
	 */
	fun createRoomListRowData(r:NetRoomInfo):Array<String> = arrayOf(
		Integer.toString(r.roomID)
		, r.strName
		, if(r.rated) getUIText("RoomTable_Rated_True") else getUIText("RoomTable_Rated_False")
		, if(r.ruleLock) r.ruleName.toUpperCase() else getUIText("RoomTable_RuleName_Any")
		, r.strMode
		, if(r.playing) getUIText("RoomTable_Status_Playing") else getUIText("RoomTable_Status_Waiting")
		, r.playerSeatedCount.toString()+"/"+r.maxPlayers
		, Integer.toString(r.spectatorCount))

	/** Entered the room that you specify
	 * @param roomID RoomID
	 * @param watch When true,Watching only
	 */
	fun joinRoom(roomID:Int, watch:Boolean) {
		tabLobbyAndRoom.setEnabledAt(1, true)
		tabLobbyAndRoom.selectedIndex = 1

		if(netPlayerClient!!.yourPlayerInfo!!.roomID!=roomID) {
			txtpaneRoomChatLog.text = ""
			setRoomButtonsEnabled(false)
			netPlayerClient!!.send("roomjoin\t$roomID\t$watch\n")
		}

		changeCurrentScreenCard(SCREENCARD_LOBBY)
	}

	/** Change UI type of multiplayer room create screen
	 * @param isDetailMode true=View Detail, false=Create Room
	 * @param roomInfo Room Information (only used when isDetailMode is true)
	 */
	fun setCreateRoomUIType(isDetailMode:Boolean, roomInfo:NetRoomInfo?) {
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
					hurryupSeconds = propConfig.getProperty("createroom.defaultHurryupSeconds", 180)
					hurryupInterval = propConfig.getProperty("createroom.defaultHurryupInterval", 5)
					garbagePercent = propConfig.getProperty("createroom.defaultGarbagePercent", 90)
					ruleLock = propConfig.getProperty("createroom.defaultRuleLock", false)
					tspinEnableType = propConfig.getProperty("createroom.defaultTspinEnableType", 2)
					spinCheckType = propConfig.getProperty("createroom.defaultSpinCheckType", 1)
					tspinEnableEZ = propConfig.getProperty("createroom.defaultTspinEnableEZ", true)
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
	fun setCreateRoom1PUIType(isDetailMode:Boolean, roomInfo:NetRoomInfo?) {
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
	fun updateLobbyUserList() {
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
					if(pInfo.uid==netPlayerClient!!.playerUID) name = "*"+getPlayerNameWithTripCode(pInfo)+" - "+pInfo.strTeam
				}

				// Rating
				name += " |"+pInfo.rating[0]+"|"
				/* name += " |T:" + pInfo.rating[0] + "|";
				 * name += "A:" + pInfo.rating[1] + "|";
				 * name += "P:" + pInfo.rating[2] + "|";
				 * name += "S:" + pInfo.rating[3] + "|"; */

				// Country code
				if(pInfo.strCountry.isNotEmpty()) name += " ("+pInfo.strCountry+")"

				/* XXX Hostname
				 * if(pInfo.strHost.length() > 0) {
				 * name += " {" + pInfo.strHost + "}";
				 * } */

				if(pInfo.roomID==-1)
					listmodelLobbyChatPlayerList.addElement(name)
				else
					listmodelLobbyChatPlayerList.addElement("{"+pInfo.roomID+"} "+name)
			}
		}
	}

	/** Screen RoomPlayerList update */
	fun updateRoomUserList() {
		val roomInfo = netPlayerClient!!.getRoomInfo(netPlayerClient!!.yourPlayerInfo!!.roomID) ?: return

		val pList = LinkedList(netPlayerClient!!.playerInfoList)

		if(!pList.isEmpty()) {
			listmodelRoomChatPlayerList.clear()

			for(i in 0 until roomInfo.maxPlayers)
				listmodelRoomChatPlayerList.addElement("["+(i+1)+"]")

			for(pInfo in pList) {
				if(pInfo.roomID==roomInfo.roomID) {
					// Name
					var name = getPlayerNameWithTripCode(pInfo)
					if(pInfo.uid==netPlayerClient!!.playerUID) name = "*"+getPlayerNameWithTripCode(pInfo)

					// Team
					if(pInfo.strTeam.isNotEmpty()) {
						name = getPlayerNameWithTripCode(pInfo)+" - "+pInfo.strTeam
						if(pInfo.uid==netPlayerClient!!.playerUID) name = "*"+getPlayerNameWithTripCode(pInfo)+" - "+pInfo.strTeam
					}

					// Rating
					name += " |"+pInfo.rating[roomInfo.style]+"|"

					// Country code
					if(pInfo.strCountry.isNotEmpty()) name += " ("+pInfo.strCountry+")"

					/* XXX Hostname
					 * if(pInfo.strHost.length() > 0) {
					 * name += " {" + pInfo.strHost + "}";
					 * } */

					// Status
					if(pInfo.playing)
						name += getUIText("RoomUserList_Playing")
					else if(pInfo.ready) name += getUIText("RoomUserList_Ready")

					if(pInfo.seatID>=0&&pInfo.seatID<roomInfo.maxPlayers)
						listmodelRoomChatPlayerList.set(pInfo.seatID, "["+(pInfo.seatID+1)+"] "+name)
					else if(pInfo.queueID!=-1)
						listmodelRoomChatPlayerList.addElement((pInfo.queueID+1).toString()+". "+name)
					else
						listmodelRoomChatPlayerList.addElement(name)
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
	fun sendMyRuleDataToServer() {
		if(ruleOptPlayer==null) ruleOptPlayer = RuleOptions()

		val prop = CustomProperties()
		ruleOptPlayer!!.writeProperty(prop, 0)
		val strRuleTemp = prop.encode("RuleData") ?: ""
		val strRuleData = NetUtil.compressString(strRuleTemp)
		log.debug("RuleData uncompressed:"+strRuleTemp.length+" compressed:"+strRuleData.length)

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
		propConfig.setProperty("lobby.splitLobby.location", splitLobby?.dividerLocation ?: 0)
		propConfig.setProperty("lobby.splitLobbyChat.location", splitLobbyChat?.dividerLocation ?: 0)
		propConfig.setProperty("room.splitRoom.location", splitRoom?.dividerLocation ?: 0)
		propConfig.setProperty("room.splitRoomChat.location", splitRoomChat?.dividerLocation ?: 0)
		propConfig.setProperty("serverselect.txtfldPlayerName.text", txtfldPlayerName.text)
		propConfig.setProperty("serverselect.txtfldPlayerTeam.text", txtfldPlayerTeam.text)

		val listboxServerListSelectedValue = listboxServerList.selectedValue
		if(listboxServerListSelectedValue!=null&&listboxServerListSelectedValue is String)
			propConfig.setProperty("serverselect.listboxServerList.value", listboxServerListSelectedValue)
		else
			propConfig.setProperty("serverselect.listboxServerList.value", "")

		var tm = tableRoomList.columnModel
		propConfig.setProperty("tableRoomList.width.id", tm.getColumn(0).width)
		propConfig.setProperty("tableRoomList.width.name", tm.getColumn(1).width)
		propConfig.setProperty("tableRoomList.width.rated", tm.getColumn(2).width)
		propConfig.setProperty("tableRoomList.width.rulename", tm.getColumn(3).width)
		propConfig.setProperty("tableRoomList.width.modename", tm.getColumn(4).width)
		propConfig.setProperty("tableRoomList.width.status", tm.getColumn(5).width)
		propConfig.setProperty("tableRoomList.width.players", tm.getColumn(6).width)
		propConfig.setProperty("tableRoomList.width.spectators", tm.getColumn(7).width)

		tm = tableGameStat.columnModel
		propConfig.setProperty("tableGameStat.width.rank", tm.getColumn(0).width)
		propConfig.setProperty("tableGameStat.width.name", tm.getColumn(1).width)
		propConfig.setProperty("tableGameStat.width.attack", tm.getColumn(2).width)
		propConfig.setProperty("tableGameStat.width.apl", tm.getColumn(3).width)
		propConfig.setProperty("tableGameStat.width.apm", tm.getColumn(4).width)
		propConfig.setProperty("tableGameStat.width.lines", tm.getColumn(5).width)
		propConfig.setProperty("tableGameStat.width.lpm", tm.getColumn(6).width)
		propConfig.setProperty("tableGameStat.width.piece", tm.getColumn(7).width)
		propConfig.setProperty("tableGameStat.width.pps", tm.getColumn(8).width)
		propConfig.setProperty("tableGameStat.width.time", tm.getColumn(9).width)
		propConfig.setProperty("tableGameStat.width.ko", tm.getColumn(10).width)
		propConfig.setProperty("tableGameStat.width.wins", tm.getColumn(11).width)
		propConfig.setProperty("tableGameStat.width.games", tm.getColumn(12).width)

		tm = tableGameStat1P.columnModel
		propConfig.setProperty("tableGameStat1P.width.description", tm.getColumn(0).width)
		propConfig.setProperty("tableGameStat1P.width.value", tm.getColumn(1).width)

		tm = tableMPRanking[0].columnModel
		propConfig.setProperty("tableMPRanking.width.rank", tm.getColumn(0).width)
		propConfig.setProperty("tableMPRanking.width.name", tm.getColumn(1).width)
		propConfig.setProperty("tableMPRanking.width.rating", tm.getColumn(2).width)
		propConfig.setProperty("tableMPRanking.width.play", tm.getColumn(3).width)
		propConfig.setProperty("tableMPRanking.width.win", tm.getColumn(4).width)

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
			propConfig.setProperty("createroom.defaultGarbagePercent", it.garbagePercent)
			propConfig.setProperty("createroom.defaultTargetTimer", it.targetTimer)
			propConfig.setProperty("createroom.defaultHurryupSeconds", it.hurryupSeconds)
			propConfig.setProperty("createroom.defaultHurryupInterval", it.hurryupInterval)
			propConfig.setProperty("createroom.defaultRuleLock", it.ruleLock)
			propConfig.setProperty("createroom.defaultTSpinEnableType", it.tspinEnableType)
			propConfig.setProperty("createroom.defaultSpinCheckType", it.spinCheckType)
			propConfig.setProperty("createroom.defaultTSpinEnableEZ", it.tspinEnableEZ)
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
			propConfig.setProperty("createroom.defaultMapSetID", spinnerCreateRoomMapSetID!!.value as Int)
		}

		val listboxCreateRoom1PModeListSelectedValue = listboxCreateRoom1PModeList.selectedValue
		if(listboxCreateRoom1PModeListSelectedValue!=null&&listboxCreateRoom1PModeListSelectedValue is String)
			propConfig.setProperty("createroom1p.listboxCreateRoom1PModeList.value", listboxCreateRoom1PModeListSelectedValue)
		else
			propConfig.setProperty("createroom1p.listboxCreateRoom1PModeList.value", "")

		val listboxCreateRoom1PRuleListSelectedValue = listboxCreateRoom1PRuleList.selectedValue
		if(listboxCreateRoom1PRuleListSelectedValue!=null&&listboxCreateRoom1PRuleListSelectedValue is String&&
			listboxCreateRoom1PRuleList.selectedIndex>=1)
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
	fun saveGlobalConfig() {
		try {
			val out = FileOutputStream("config/setting/global.cfg")
			propGlobal.store(out, "NullpoMino Global Config")
			out.close()
		} catch(e:IOException) {
			log.warn("Failed to save global config file", e)
		}

	}

	/** End processing */
	fun shutdown() {
		if(splitLobby!=null) saveConfig()

		if(writerLobbyLog!=null) {
			writerLobbyLog!!.flush()
			writerLobbyLog!!.close()
			writerLobbyLog = null
		}
		if(writerRoomLog!=null) {
			writerRoomLog!!.flush()
			writerRoomLog!!.close()
			writerRoomLog = null
		}

		// Cut
		if(netPlayerClient!=null) {
			if(netPlayerClient!!.isConnected) netPlayerClient!!.send("disconnect\n")
			netPlayerClient!!.threadRunning = false
			netPlayerClient!!.interrupt()
			netPlayerClient = null
		}

		// ListenerCall
		if(listeners!=null) {
			for(l in listeners!!)
				l.netlobbyOnExit(this)
			listeners = null
		}
		if(netDummyMode!=null) {
			netDummyMode!!.netlobbyOnExit(this)
			netDummyMode = null
		}

		dispose()
	}

	/** Delete server buttonWhen processing is pressed */
	fun serverSelectDeleteButtonClicked() {
		val index = listboxServerList.selectedIndex
		if(index!=-1) {
			val server = listboxServerList.selectedValue as String
			val answer = JOptionPane.showConfirmDialog(this, getUIText("MessageBody_ServerDelete")+"\n"
				+server, getUIText("MessageTitle_ServerDelete"), JOptionPane.YES_NO_OPTION)
			if(answer==JOptionPane.YES_OPTION) {
				listmodelServerList.remove(index)
				saveListFromDefaultListModel(listmodelServerList, "config/setting/netlobby_serverlist.cfg")
			}
		}
	}

	/** Server connection buttonWhen processing is pressed */
	fun serverSelectConnectButtonClicked() {
		val index = listboxServerList.selectedIndex
		if(index!=-1) {
			val strServer = listboxServerList.selectedValue as String
			var portSpliter = strServer.indexOf(":")
			if(portSpliter==-1) portSpliter = strServer.length

			val strHost = strServer.substring(0, portSpliter)
			log.debug("Host:$strHost")

			var port = NetBaseClient.DEFAULT_PORT
			try {
				val strPort = strServer.substring(portSpliter+1, strServer.length)
				port = Integer.parseInt(strPort)
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

			val strHost = strServer.substring(0, portSpliter)
			log.debug("Host:$strHost")

			var port = NetBaseClient.DEFAULT_PORT
			try {
				val strPort = strServer.substring(portSpliter+1, strServer.length)
				port = Integer.parseInt(strPort)
			} catch(e2:Exception) {
				log.debug("Failed to get port number; Try to use default port")
			}

			log.debug("Port:$port")

			val answer = JOptionPane.showConfirmDialog(this, getUIText("MessageBody_SetObserver")+"\n"
				+strServer, getUIText("MessageTitle_SetObserver"), JOptionPane.YES_NO_OPTION)

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
	fun sendChat(roomchat:Boolean, strMsg:String) {
		var msg = strMsg
		if(msg.startsWith("/team")) {
			msg = msg.replaceFirst("/team".toRegex(), "")
			msg = msg.trim {it<=' '}
			netPlayerClient!!.send("changeteam\t"+NetUtil.urlEncode(msg)+"\n")
		} else if(roomchat)
			netPlayerClient!!.send("chat\t"+NetUtil.urlEncode(msg)+"\n")
		else
			netPlayerClient!!.send("lobbychat\t"+NetUtil.urlEncode(msg)+"\n")
	}

	/** Creates NetRoomInfo from Create Room screen
	 * @return NetRoomInfo
	 */
	fun exportRoomInfoFromCreateRoomScreen():NetRoomInfo? {
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
			val tspinEnableType = comboboxCreateRoomTSpinEnableType.selectedIndex
			val spinCheckType = comboboxCreateRoomSpinCheckType.selectedIndex
			val tspinEnableEZ = chkboxCreateRoomTSpinEnableEZ.isSelected
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
			roomInfo.hurryupSeconds = integerHurryupSeconds
			roomInfo.hurryupInterval = integerHurryupInterval
			roomInfo.ruleLock = rulelock
			roomInfo.tspinEnableType = tspinEnableType
			roomInfo.spinCheckType = spinCheckType
			roomInfo.tspinEnableEZ = tspinEnableEZ
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
			roomInfo.garbagePercent = integerGarbagePercent
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
	fun importRoomInfoToCreateRoomScreen(r:NetRoomInfo?) {
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
			spinnerCreateRoomHurryupSeconds.value = r.hurryupSeconds
			spinnerCreateRoomHurryupInterval.value = r.hurryupInterval
			spinnerCreateRoomGarbagePercent.value = r.garbagePercent
			spinnerCreateRoomTargetTimer.value = r.targetTimer
			chkboxCreateRoomUseMap.isSelected = r.useMap
			chkboxCreateRoomRuleLock.isSelected = r.ruleLock
			comboboxCreateRoomTSpinEnableType.selectedIndex = r.tspinEnableType
			comboboxCreateRoomSpinCheckType.selectedIndex = r.spinCheckType
			chkboxCreateRoomTSpinEnableEZ.isSelected = r.tspinEnableEZ
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
	fun createRuleEntries(filelist:Array<String>) {
		ruleEntries = LinkedList()

		for(element in filelist) {
			val entry = RuleEntry()

			val file = File("config/rule/$element")
			entry.filename = element
			entry.filepath = file.path

			val prop = CustomProperties()
			try {
				val `in` = FileInputStream("config/rule/$element")
				prop.load(`in`)
				`in`.close()
				entry.rulename = prop.getProperty("0.ruleopt.strRuleName", "")
				entry.style = prop.getProperty("0.ruleopt.style", 0)
			} catch(e:Exception) {
				entry.rulename = ""
				entry.style = -1
			}

			ruleEntries.add(entry)
		}
	}

	/** Get subset of rule entries (for rule change screen)
	 * @param currentStyle Current style
	 * @return Subset of rule entries
	 */
	private fun getSubsetEntries(currentStyle:Int):LinkedList<RuleEntry> {
		val subEntries = LinkedList<RuleEntry>()
		for(ruleEntry in ruleEntries) if(ruleEntry.style==currentStyle) subEntries.add(ruleEntry)
		return subEntries
	}

	/** Get rule name + file name list as String[] (for rule change screen)
	 * @param currentStyle Current style
	 * @return Rule name + file name list
	 */
	fun extractRuleListFromRuleEntries(currentStyle:Int):Array<String> {
		val subEntries = getSubsetEntries(currentStyle)

		return Array(subEntries.size) {
			val entry = subEntries[it]
			return@Array entry.rulename+" ("+entry.filename+")"
		}
	}

	/** Enter rule change screen */
	fun enterRuleChangeScreen() {
		// Set rule selections
		val strCurrentFileName = Array<String>(GameEngine.MAX_GAMESTYLE) {
			if(it==0) propGlobal.getProperty(0.toString()+".rulefile", "")
			else propGlobal.getProperty(0.toString()+".rulefile."+it, "")
		}

		for(i in 0 until GameEngine.MAX_GAMESTYLE) {
			val subEntries = getSubsetEntries(i)
			for(j in subEntries.indices)
				if(subEntries[j].filename==strCurrentFileName[i]) listboxRuleChangeRuleList[i].selectedIndex = j
		}

		// Tuning
		val owRotateButtonDefaultRight = propGlobal.getProperty(0.toString()+".tuning.owRotateButtonDefaultRight", -1)+1
		comboboxTuningRotateButtonDefaultRight.selectedIndex = owRotateButtonDefaultRight
		val owMoveDiagonal = propGlobal.getProperty(0.toString()+".tuning.owMoveDiagonal", -1)+1
		comboboxTuningMoveDiagonal.selectedIndex = owMoveDiagonal
		val owBlockShowOutlineOnly = propGlobal.getProperty(0.toString()+".tuning.owBlockShowOutlineOnly", -1)+1
		comboboxTuningBlockShowOutlineOnly.selectedIndex = owBlockShowOutlineOnly
		val owSkin = propGlobal.getProperty(0.toString()+".tuning.owSkin", -1)+1
		comboboxTuningSkin.selectedIndex = owSkin
		val owBlockOutlineType = propGlobal.getProperty(0.toString()+".tuning.owBlockOutlineType", -1)+1
		comboboxTuningBlockOutlineType.selectedIndex = owBlockOutlineType

		txtfldTuningMinDAS.text = propGlobal.getProperty(0.toString()+".tuning.owMinDAS", "-1")
		txtfldTuningMaxDAS.text = propGlobal.getProperty(0.toString()+".tuning.owMaxDAS", "-1")
		txtfldTuningDasDelay.text = propGlobal.getProperty(0.toString()+".tuning.owDasDelay", "-1")
		chkboxTuningReverseUpDown.isSelected = propGlobal.getProperty(0.toString()+".tuning.owReverseUpDown", false)

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
			// TODO:Quick Start
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
				netPlayerClient!!.send("changeteam\t"+NetUtil.urlEncode(txtfldRoomListTeam.text)+"\n")
				roomListTopBarCardLayout.first(subpanelRoomListTopBar)
			}
		// Change teamCancel(Lobby screen)
		if(e.actionCommand=="Lobby_TeamChange_Cancel")
			roomListTopBarCardLayout.first(subpanelRoomListTopBar)
		// Withdrawal button
		if(e.actionCommand=="Room_Leave") {
			if(netPlayerClient!=null&&netPlayerClient!!.isConnected) netPlayerClient!!.send("roomjoin\t-1\tfalse\n")

			tablemodelGameStat.rowCount = 0
			tablemodelGameStat1P.rowCount = 0

			tabLobbyAndRoom.selectedIndex = 0
			tabLobbyAndRoom.setEnabledAt(1, false)
			tabLobbyAndRoom.setTitleAt(1, getUIText("Lobby_Tab_NoRoom"))

			changeCurrentScreenCard(SCREENCARD_LOBBY)

			// Listener call
			for(l in listeners!!)
				l.netlobbyOnRoomLeave(this, netPlayerClient!!)
			if(netDummyMode!=null) netDummyMode!!.netlobbyOnRoomLeave(this, netPlayerClient!!)
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
				roomTopBarCardLayout.next(subpanelRoomTopBar)
			}
		// Change teamOK(Room screen)
		if(e.actionCommand=="Room_TeamChange_OK")
			if(netPlayerClient!=null&&netPlayerClient!!.isConnected) {
				netPlayerClient!!.send("changeteam\t"+NetUtil.urlEncode(txtfldRoomTeam.text)+"\n")
				roomTopBarCardLayout.first(subpanelRoomTopBar)
			}
		// Change teamCancel(Room screen)
		if(e.actionCommand=="Room_TeamChange_Cancel") roomTopBarCardLayout.first(subpanelRoomTopBar)
		// Confirmation rule(Room screen)
		if(e.actionCommand=="Room_ViewSetting") viewRoomDetail(netPlayerClient!!.yourPlayerInfo!!.roomID)
		// In the Add Server screenOK button
		if(e.actionCommand=="ServerAdd_OK") {
			if(txtfldServerAddHost.text.isNotEmpty()) {
				listmodelServerList.addElement(txtfldServerAddHost.text)
				saveListFromDefaultListModel(listmodelServerList, "config/setting/netlobby_serverlist.cfg")
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

				val msg = ("ratedroomcreate\t"+NetUtil.urlEncode(r.strName)+"\t"
					+spinnerCreateRatedMaxPlayers.value+"\t"+presetIndex+"\t"
					+NetUtil.urlEncode("NET-VS-BATTLE")+"\n")

				txtpaneRoomChatLog.text = ""
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

				var msg:String
				msg = "roomcreate\t"+NetUtil.urlEncode(r!!.strName)+"\t"
				msg += NetUtil.urlEncode(r.exportString())+"\t"
				msg += NetUtil.urlEncode(r.strMode)+"\t"

				// Map send
				if(r.useMap) {
					val setID = currentSelectedMapSetID
					log.debug("MapSetID:$setID")

					mapList.clear()
					val propMap = CustomProperties()
					try {
						val `in` = FileInputStream("config/values/vsbattle/$setID.values")
						propMap.load(`in`)
						`in`.close()
					} catch(e2:IOException) {
						log.error("Map set $setID not found", e2)
					}

					val maxMap = propMap.getProperty("values.maxMapNumber", 0)
					log.debug("Number of maps:$maxMap")

					val strMap = StringBuilder()

					for(i in 0 until maxMap) {
						val strMapTemp = propMap.getProperty("values.$i", "")
						mapList.add(strMapTemp)
						strMap.append(strMapTemp)
						if(i<maxMap-1) strMap.append("\t")
					}

					val strCompressed = NetUtil.compressString(strMap.toString())
					log.debug("Map uncompressed:"+strMap.length+" compressed:"+strCompressed.length)

					msg += strCompressed
				}

				msg += "\n"

				txtpaneRoomChatLog.text = ""
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
				strPresetCode = strPresetCode.replace("[^a-zA-Z0-9+/=]".toRegex(), "")
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

					txtpaneRoomChatLog.text = ""
					setRoomButtonsEnabled(false)
					tabLobbyAndRoom.setEnabledAt(1, true)
					tabLobbyAndRoom.selectedIndex = 1
					changeCurrentScreenCard(SCREENCARD_LOBBY)

					netPlayerClient!!.send("singleroomcreate\t"+"\t"+NetUtil.urlEncode(strMode)+"\t"+NetUtil.urlEncode(strRule)+"\n")
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
			val strPrevTetrominoRuleFilename = propGlobal.getProperty(0.toString()+".rule", "")

			for(i in 0 until GameEngine.MAX_GAMESTYLE) {
				val id = listboxRuleChangeRuleList[i].selectedIndex
				val subEntries = getSubsetEntries(i)
				var entry:RuleEntry? = null
				if(id>=0) entry = subEntries[id]

				if(i==0) {
					if(id>=0) {
						propGlobal.setProperty(0.toString()+".rule", entry!!.filepath)
						propGlobal.setProperty(0.toString()+".rulefile", entry.filename)
						propGlobal.setProperty(0.toString()+".rulename", entry.rulename)
					} else {
						propGlobal.setProperty(0.toString()+".rule", "")
						propGlobal.setProperty(0.toString()+".rulefile", "")
						propGlobal.setProperty(0.toString()+".rulename", "")
					}
				} else if(id>=0) {
					propGlobal.setProperty(0.toString()+".rule."+i, entry!!.filepath)
					propGlobal.setProperty(0.toString()+".rulefile."+i, entry.filename)
					propGlobal.setProperty(0.toString()+".rulename."+i, entry.rulename)
				} else {
					propGlobal.setProperty(0.toString()+".rule."+i, "")
					propGlobal.setProperty(0.toString()+".rulefile."+i, "")
					propGlobal.setProperty(0.toString()+".rulename."+i, "")
				}
			}

			// Tuning
			val owRotateButtonDefaultRight = comboboxTuningRotateButtonDefaultRight.selectedIndex-1
			propGlobal.setProperty(0.toString()+".tuning.owRotateButtonDefaultRight", owRotateButtonDefaultRight)

			val owMoveDiagonal = comboboxTuningMoveDiagonal.selectedIndex-1
			propGlobal.setProperty(0.toString()+".tuning.owMoveDiagonal", owMoveDiagonal)

			val owBlockShowOutlineOnly = comboboxTuningBlockShowOutlineOnly.selectedIndex-1
			propGlobal.setProperty(0.toString()+".tuning.owBlockShowOutlineOnly", owBlockShowOutlineOnly)

			val owSkin = comboboxTuningSkin.selectedIndex-1
			propGlobal.setProperty(0.toString()+".tuning.owSkin", owSkin)

			val owBlockOutlineType = comboboxTuningBlockOutlineType.selectedIndex-1
			propGlobal.setProperty(0.toString()+".tuning.owBlockOutlineType", owBlockOutlineType)

			val owMinDAS = getIntTextField(-1, txtfldTuningMinDAS)
			propGlobal.setProperty(0.toString()+".tuning.owMinDAS", owMinDAS)
			val owMaxDAS = getIntTextField(-1, txtfldTuningMaxDAS)
			propGlobal.setProperty(0.toString()+".tuning.owMaxDAS", owMaxDAS)
			val owDasDelay = getIntTextField(-1, txtfldTuningDasDelay)
			propGlobal.setProperty(0.toString()+".tuning.owDasDelay", owDasDelay)
			val owReverseUpDown = chkboxTuningReverseUpDown.isSelected
			propGlobal.setProperty(0.toString()+".tuning.owReverseUpDown", owReverseUpDown)

			// Save
			saveGlobalConfig()

			// Load rule
			val strFileName = propGlobal.getProperty(0.toString()+".rule", "")
			if(strPrevTetrominoRuleFilename!=strFileName) {
				val propRule = CustomProperties()
				try {
					val `in` = FileInputStream(strFileName)
					propRule.load(`in`)
					`in`.close()
				} catch(e2:Exception) {
				}

				ruleOptPlayer = RuleOptions()
				ruleOptPlayer!!.readProperty(propRule, 0)

				// Send rule
				if(netPlayerClient!=null&&netPlayerClient!!.isConnected) sendMyRuleDataToServer()
			}

			changeCurrentScreenCard(SCREENCARD_LOBBY)
		}
	}

	/* Message reception */
	@Throws(IOException::class)
	override fun netOnMessage(client:NetBaseClient, message:Array<String>) {
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
						String.format("log/lobby_%04d_%02d_%02d_%02d_%02d_%02d.txt", currentTime.get(Calendar.YEAR), month, currentTime.get(Calendar.DATE), currentTime.get(Calendar.HOUR_OF_DAY), currentTime.get(Calendar.MINUTE), currentTime.get(Calendar.SECOND))
					writerLobbyLog = PrintWriter(filename)
				} catch(e:Exception) {
					log.warn("Failed to create lobby log file", e)
				}

			if(writerRoomLog==null)
				try {
					val currentTime = GregorianCalendar()
					val month = currentTime.get(Calendar.MONTH)+1
					val filename =
						String.format("log/room_%04d_%02d_%02d_%02d_%02d_%02d.txt", currentTime.get(Calendar.YEAR), month, currentTime.get(Calendar.DATE), currentTime.get(Calendar.HOUR_OF_DAY), currentTime.get(Calendar.MINUTE), currentTime.get(Calendar.SECOND))
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
			addSystemChatLogLater(txtpaneLobbyChatLog, getUIText("SysMsg_YourNickname")+convTripCode(NetUtil.urlDecode(message[1])), Color.blue)
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
				for(i in 1 until message.size)
					reason.append(message[i]).append(" ")
				addSystemChatLogLater(txtpaneLobbyChatLog, getUIText("SysMsg_LoginFail")+reason, Color.red)
			}
		}
		// Banned
		if(message[0]=="banned") {
			setLobbyButtonsEnabled(0)

			val cStart = GeneralUtil.importCalendarString(message[1])
			val cExpire = if(message.size>2&&message[2].isNotEmpty()) GeneralUtil.importCalendarString(message[2]) else null

			val strStart = if(cStart!=null) GeneralUtil.getCalendarString(cStart) else "???"
			val strExpire = if(cExpire!=null) GeneralUtil.getCalendarString(cExpire) else getUIText("SysMsg_Banned_Permanent")

			addSystemChatLogLater(txtpaneLobbyChatLog, String.format(getUIText("SysMsg_Banned"), strStart, strExpire), Color.red)
		}
		// Rule dataTransmission success
		if(message[0]=="ruledatasuccess") {
			addSystemChatLogLater(txtpaneLobbyChatLog, getUIText("SysMsg_SendRuleDataOK"), Color.blue)

			// ListenerCall
			for(l in listeners!!)
				l.netlobbyOnLoginOK(this, netPlayerClient!!)
			if(netDummyMode!=null) netDummyMode!!.netlobbyOnLoginOK(this, netPlayerClient!!)

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

			log.info("Received rule data ("+ruleOptLock!!.strRuleName+")")
		}
		// Rated-game rule list
		if(message[0]=="rulelist") {
			val style = Integer.parseInt(message[1])

			if(style<listRatedRuleName.size) {
				listRatedRuleName[style].clear()

				for(i in 0 until message.size-2) {
					val name = NetUtil.urlDecode(message[2+i])
					listRatedRuleName[style].add(name)
				}
			}

			if(style==0) {
				listmodelCreateRoom1PRuleList.clear()
				listmodelCreateRoom1PRuleList.addElement(getUIText("CreateRoom1P_YourRule"))
				listboxCreateRoom1PRuleList.selectedIndex = 0

				for(i in 0 until listRatedRuleName[style].size) {
					val name = listRatedRuleName[style][i]
					listmodelCreateRoom1PRuleList.addElement(name)
				}

				listboxCreateRoom1PRuleList.setSelectedValue(propConfig.getProperty("createroom1p.listboxCreateRoom1PRuleList.value", ""), true)
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
					val p2 = netPlayerClient!!.yourPlayerInfo
					if(p!=null&&p2!=null&&p.roomID==p2.roomID) {
						val strTemp:String = if(p.strHost.isNotEmpty())
							String.format(getUIText("SysMsg_LeaveRoomWithHost"), getPlayerNameWithTripCode(p), p.strHost)
						else
							String.format(getUIText("SysMsg_LeaveRoom"), getPlayerNameWithTripCode(p))
						addSystemChatLogLater(txtpaneRoomChatLog, strTemp, Color.blue)
					}
				}
			}
		}
		// PlayerEntering a room
		if(message[0]=="playerenter") {
			val uid = Integer.parseInt(message[1])
			val pInfo = netPlayerClient!!.getPlayerInfoByUID(uid)

			if(pInfo!=null) {
				val strTemp:String = if(pInfo.strHost.isNotEmpty())
					String.format(getUIText("SysMsg_EnterRoomWithHost"), getPlayerNameWithTripCode(pInfo), pInfo.strHost)
				else
					String.format(getUIText("SysMsg_EnterRoom"), getPlayerNameWithTripCode(pInfo))
				addSystemChatLogLater(txtpaneRoomChatLog, strTemp, Color.blue)
			}
		}
		// PlayerWithdrawal
		if(message[0]=="playerleave") {
			val uid = Integer.parseInt(message[1])
			val pInfo = netPlayerClient!!.getPlayerInfoByUID(uid)

			if(pInfo!=null) {
				val strTemp:String = if(pInfo.strHost.isNotEmpty())
					String.format(getUIText("SysMsg_LeaveRoomWithHost"), getPlayerNameWithTripCode(pInfo), pInfo.strHost)
				else
					String.format(getUIText("SysMsg_LeaveRoom"), getPlayerNameWithTripCode(pInfo))
				addSystemChatLogLater(txtpaneRoomChatLog, strTemp, Color.blue)
			}
		}
		// Change team
		if(message[0]=="changeteam") {
			val uid = Integer.parseInt(message[1])
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
			val size = Integer.parseInt(message[1])

			tablemodelRoomList.rowCount = 0
			for(i in 0 until size) {
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
					for(i in 1 until message.size) {
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

			for(i in 0 until tablemodelRoomList.rowCount) {
				val strID = tablemodelRoomList.getValueAt(i, columnID) as String
				val roomID = Integer.parseInt(strID)

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

			for(i in 0 until tablemodelRoomList.rowCount) {
				val strID = tablemodelRoomList.getValueAt(i, columnID) as String
				val roomID = Integer.parseInt(strID)

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
			val roomID = Integer.parseInt(message[1])
			val seatID = Integer.parseInt(message[2])
			val queueID = Integer.parseInt(message[3])

			netPlayerClient!!.yourPlayerInfo!!.roomID = roomID
			netPlayerClient!!.yourPlayerInfo!!.seatID = seatID
			netPlayerClient!!.yourPlayerInfo!!.queueID = queueID

			if(roomID!=-1) {
				val roomInfo = netPlayerClient!!.getRoomInfo(roomID)
				val pInfo = netPlayerClient!!.yourPlayerInfo

				if(seatID==-1&&queueID==-1) {
					val strTemp = String.format(getUIText("SysMsg_StatusChange_Spectator"), getPlayerNameWithTripCode(pInfo))
					addSystemChatLogLater(txtpaneRoomChatLog, strTemp, Color.blue)
					setRoomJoinButtonVisible(true)
				} else if(seatID==-1) {
					val strTemp = String.format(getUIText("SysMsg_StatusChange_Queue"), getPlayerNameWithTripCode(pInfo))
					addSystemChatLogLater(txtpaneRoomChatLog, strTemp, Color.blue)
					setRoomJoinButtonVisible(false)
				} else {
					val strTemp = String.format(getUIText("SysMsg_StatusChange_Joined"), getPlayerNameWithTripCode(pInfo))
					addSystemChatLogLater(txtpaneRoomChatLog, strTemp, Color.blue)
					setRoomJoinButtonVisible(false)
				}

				if(netPlayerClient!=null&&netPlayerClient!!.getRoomInfo(roomID)!=null)
					if(netPlayerClient!!.getRoomInfo(roomID)!!.singleplayer) {
						btnRoomButtonsJoin.isVisible = false
						btnRoomButtonsSitOut.isVisible = false
						btnRoomButtonsRanking.isVisible = false
						gameStatCardLayout.show(subpanelGameStat, "GameStat1P")
					} else {
						btnRoomButtonsRanking.isVisible = netPlayerClient!!.getRoomInfo(roomID)!!.rated
						gameStatCardLayout.show(subpanelGameStat, "GameStatMP")
					}

				SwingUtilities.invokeLater {
					setRoomButtonsEnabled(true)
					updateRoomUserList()
				}

				val strTitle = roomInfo!!.strName
				title = getUIText("Title_NetLobby")+" - "+strTitle
				tabLobbyAndRoom.setTitleAt(1, getUIText("Lobby_Tab_Room")+strTitle)

				addSystemChatLogLater(txtpaneRoomChatLog, getUIText("SysMsg_RoomJoin_Title")+strTitle, Color.blue)
				addSystemChatLogLater(txtpaneRoomChatLog, getUIText("SysMsg_RoomJoin_ID")+roomInfo.roomID, Color.blue)
				if(roomInfo.ruleLock)
					addSystemChatLogLater(txtpaneRoomChatLog, getUIText("SysMsg_RoomJoin_Rule")+roomInfo.ruleName, Color.blue)

				setLobbyButtonsEnabled(2)
				changeCurrentScreenCard(SCREENCARD_LOBBY)

				// ListenerCall
				for(l in listeners!!)
					l.netlobbyOnRoomJoin(this, netPlayerClient!!, roomInfo)
				if(netDummyMode!=null) netDummyMode!!.netlobbyOnRoomJoin(this, netPlayerClient!!, roomInfo)
			} else {
				addSystemChatLogLater(txtpaneRoomChatLog, getUIText("SysMsg_RoomJoin_Lobby"), Color.blue)

				title = getUIText("Title_NetLobby")
				tabLobbyAndRoom.selectedIndex = 0
				tabLobbyAndRoom.setEnabledAt(1, false)
				tabLobbyAndRoom.setTitleAt(1, getUIText("Lobby_Tab_NoRoom"))

				setLobbyButtonsEnabled(1)
				changeCurrentScreenCard(SCREENCARD_LOBBY)

				// ListenerCall
				for(l in listeners!!)
					l.netlobbyOnRoomLeave(this, netPlayerClient!!)
				if(netDummyMode!=null) netDummyMode!!.netlobbyOnRoomLeave(this, netPlayerClient!!)
			}
		}
		// Entry Room failure
		if(message[0]=="roomjoinfail")
			addSystemChatLogLater(txtpaneRoomChatLog, getUIText("SysMsg_RoomJoinFail"), Color.red)
		// Kicked from a room
		if(message[0]=="roomkicked") {
			val strKickMsg = String.format(getUIText("SysMsg_Kicked_"+message[1]), NetUtil.urlDecode(message[3]), message[2])
			addSystemChatLogLater(txtpaneLobbyChatLog, strKickMsg, Color.red)
		}
		// Map receive
		if(message[0]=="values") {
			val strDecompressed = NetUtil.decompressString(message[1])
			val strMaps = strDecompressed.split("\t".toRegex()).dropLastWhile {it.isEmpty()}.toTypedArray()

			mapList.clear()

			val maxMap = strMaps.size
			Collections.addAll(mapList, *strMaps)

			log.debug("Received "+mapList.size+" maps")
		}
		// Lobby chat
		if(message[0]=="lobbychat") {
			val uid = Integer.parseInt(message[1])
			val pInfo = netPlayerClient!!.getPlayerInfoByUID(uid)

			if(pInfo!=null) {
				val calendar = GeneralUtil.importCalendarString(message[3])
				val strMsgBody = NetUtil.urlDecode(message[4])
				addUserChatLogLater(txtpaneLobbyChatLog, getPlayerNameWithTripCode(pInfo), calendar, strMsgBody)
			}
		}
		// Room chat
		if(message[0]=="chat") {
			val uid = Integer.parseInt(message[1])
			val pInfo = netPlayerClient!!.getPlayerInfoByUID(uid)

			if(pInfo!=null) {
				val calendar = GeneralUtil.importCalendarString(message[3])
				val strMsgBody = NetUtil.urlDecode(message[4])
				addUserChatLogLater(txtpaneRoomChatLog, getPlayerNameWithTripCode(pInfo), calendar, strMsgBody)
			}
		}
		// Lobby chat/Room chat (history)
		if(message[0]=="lobbychath"||message[0]=="chath") {
			val strUsername = convTripCode(NetUtil.urlDecode(message[1]))
			val calendar = GeneralUtil.importCalendarString(message[2])
			val strMsgBody = NetUtil.urlDecode(message[3])
			val txtpane = if(message[0]=="lobbychath") txtpaneLobbyChatLog else txtpaneRoomChatLog
			addRecordedUserChatLogLater(txtpane, strUsername, calendar, strMsgBody)
		}
		// Participation status change
		if(message[0]=="changestatus") {
			val uid = Integer.parseInt(message[2])
			val pInfo = netPlayerClient!!.getPlayerInfoByUID(uid)

			if(pInfo!=null)
				if(message[1]=="watchonly") {
					val strTemp = String.format(getUIText("SysMsg_StatusChange_Spectator"), getPlayerNameWithTripCode(pInfo))
					addSystemChatLogLater(txtpaneRoomChatLog, strTemp, Color.blue)
					if(uid==netPlayerClient!!.playerUID) setRoomJoinButtonVisible(true)
				} else if(message[1]=="joinqueue") {
					val strTemp = String.format(getUIText("SysMsg_StatusChange_Queue"), getPlayerNameWithTripCode(pInfo))
					addSystemChatLogLater(txtpaneRoomChatLog, strTemp, Color.blue)
					if(uid==netPlayerClient!!.playerUID) setRoomJoinButtonVisible(false)
				} else if(message[1]=="joinseat") {
					val strTemp = String.format(getUIText("SysMsg_StatusChange_Joined"), getPlayerNameWithTripCode(pInfo))
					addSystemChatLogLater(txtpaneRoomChatLog, strTemp, Color.blue)
					if(uid==netPlayerClient!!.playerUID) setRoomJoinButtonVisible(false)
				}

			SwingUtilities.invokeLater {updateRoomUserList()}
		}
		// Automatically start timerStart
		if(message[0]=="autostartbegin") {
			val strTemp = String.format(getUIText("SysMsg_AutoStartBegin"), message[1])
			addSystemChatLogLater(txtpaneRoomChatLog, strTemp, Color(64, 128, 0))
		}
		// game start
		if(message[0]=="start") {
			addSystemChatLogLater(txtpaneRoomChatLog, getUIText("SysMsg_GameStart"), Color(0, 128, 0))
			tablemodelGameStat.rowCount = 0
			tablemodelGameStat1P.rowCount = 0

			if(netPlayerClient!!.yourPlayerInfo!!.seatID!=-1) {
				btnRoomButtonsSitOut.isEnabled = false
				btnRoomButtonsTeamChange.isEnabled = false
				itemLobbyMenuTeamChange.isEnabled = false
				roomTopBarCardLayout.first(subpanelRoomTopBar)
			}
		}
		// Death
		if(message[0]=="dead") {
			val uid = Integer.parseInt(message[1])
			val name = convTripCode(NetUtil.urlDecode(message[2]))

			if(message.size>6) {
				val strTemp = String.format(getUIText("SysMsg_KO"), convTripCode(NetUtil.urlDecode(message[6])), name)
				addSystemChatLogLater(txtpaneRoomChatLog, strTemp, Color(0, 128, 0))
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
			val myRank = Integer.parseInt(message[4])

			rowdata[0] = Integer.toString(myRank) // Rank
			rowdata[1] = convTripCode(NetUtil.urlDecode(message[3])) // Name
			rowdata[2] = message[5] // Attack count
			rowdata[3] = message[6] // APL
			rowdata[4] = message[7] // APM
			rowdata[5] = message[8] // Line count
			rowdata[6] = message[9] // LPM
			rowdata[7] = message[10] // Piece count
			rowdata[8] = message[11] // PPS
			rowdata[9] = GeneralUtil.getTime(Integer.parseInt(message[12]).toFloat()) //  Time
			rowdata[10] = message[13] // KO
			rowdata[11] = message[14] // Win
			rowdata[12] = message[15] // Games

			var insertPos = 0
			for(i in 0 until tablemodelGameStat.rowCount) {
				val strRank = tablemodelGameStat.getValueAt(i, 0) as String
				val rank = Integer.parseInt(strRank)

				if(myRank>rank) insertPos = i+1
			}

			tablemodelGameStat.insertRow(insertPos, rowdata)

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
			val rowData = strRowData.split("\t".toRegex()).dropLastWhile {it.isEmpty()}.toTypedArray()

			if(writerRoomLog!=null) writerRoomLog!!.print("[$currentTimeAsString]\n")

			tablemodelGameStat1P.rowCount = 0
			for(element in rowData) {
				val strTempArray = element.split(";".toRegex()).dropLastWhile {it.isEmpty()}.toTypedArray()
				tablemodelGameStat1P.addRow(strTempArray)

				if(writerRoomLog!=null&&strTempArray.size>1)
					writerRoomLog!!.print(" "+strTempArray[0]+":"+strTempArray[1]
						+"\n")
			}

			if(writerRoomLog!=null) writerRoomLog!!.flush()
		}
		// game finished
		if(message[0]=="finish") {
			addSystemChatLogLater(txtpaneRoomChatLog, getUIText("SysMsg_GameEnd"), Color(0, 128, 0))

			if(message.size>3&&message[3].isNotEmpty()) {
				var flagTeamWin = false
				if(message.size>4) flagTeamWin = java.lang.Boolean.parseBoolean(message[4])

				val strWinner:String
				strWinner = if(flagTeamWin)
					String.format(getUIText("SysMsg_WinnerTeam"), NetUtil.urlDecode(message[3]))
				else
					String.format(getUIText("SysMsg_Winner"), convTripCode(NetUtil.urlDecode(message[3])))
				addSystemChatLogLater(txtpaneRoomChatLog, strWinner, Color(0, 128, 0))
			}

			btnRoomButtonsSitOut.isEnabled = true
			btnRoomButtonsTeamChange.isEnabled = true
			itemLobbyMenuTeamChange.isEnabled = true
		}
		// Rating change
		if(message[0]=="rating") {
			val strPlayerName = convTripCode(NetUtil.urlDecode(message[3]))
			val ratingNow = Integer.parseInt(message[4])
			val ratingChange = Integer.parseInt(message[5])
			val strTemp = String.format(getUIText("SysMsg_Rating"), strPlayerName, ratingNow, ratingChange)
			addSystemChatLogLater(txtpaneRoomChatLog, strTemp, Color(0, 128, 0))
		}
		// Multiplayer Leaderboard
		if(message[0]=="mpranking") {
			val style = Integer.parseInt(message[1])
			val myRank = Integer.parseInt(message[2])

			tablemodelMPRanking[style].rowCount = 0

			val strPData = NetUtil.decompressString(message[3])
			val strPDataA = strPData.split("\t".toRegex()).dropLastWhile {it.isEmpty()}.toTypedArray()

			for(element in strPDataA) {
				val strRankData = element.split(";".toRegex()).dropLastWhile {it.isEmpty()}.toTypedArray()
				val strRowData = arrayOfNulls<String>(MPRANKING_COLUMNNAMES.size)
				val rank = Integer.parseInt(strRankData[0])
				if(rank==-1)
					strRowData[0] = "N/A"
				else
					strRowData[0] = Integer.toString(rank+1)
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
			val strMessage = "["+strTime+"]<ADMIN>:"+NetUtil.urlDecode(message[1])
			addSystemChatLogLater(currentChatLogTextPane, strMessage, Color(255, 32, 0))
		}
		// Single player replay download
		if(message[0]=="spdownload") {
			val sChecksum = java.lang.Long.parseLong(message[1])
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
				l.netlobbyOnMessage(this, netPlayerClient!!, message)
		if(netDummyMode!=null) netDummyMode!!.netlobbyOnMessage(this, netPlayerClient!!, message)
	}

	/* When it is cut */
	override fun netOnDisconnect(client:NetBaseClient, ex:Throwable?) {
		SwingUtilities.invokeLater {
			setLobbyButtonsEnabled(0)
			setRoomButtonsEnabled(false)
			tablemodelRoomList.rowCount = 0
		}

		if(ex!=null) {
			addSystemChatLogLater(currentChatLogTextPane, getUIText("SysMsg_DisconnectedError")+"\n"
				+ex.localizedMessage, Color.red)
			log.info("Server Disconnected", ex)
		} else {
			addSystemChatLogLater(currentChatLogTextPane, getUIText("SysMsg_DisconnectedOK"), Color(128, 0, 0))
			log.info("Server Disconnected (null)")
		}

		// ListenerCall
		if(listeners!=null)
			for(l in listeners!!)
				l.netlobbyOnDisconnect(this, netPlayerClient!!, ex)
		if(netDummyMode!=null) netDummyMode!!.netlobbyOnDisconnect(this, netPlayerClient!!, ex)
	}

	/** Add an new NetLobbyListener, but don't add NetDummyMode!
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
			val field = c as JTextComponent
			val flg = field.selectedText!=null
			cutAction.isEnabled = flg
			copyAction.isEnabled = flg
			deleteAction.isEnabled = flg
			selectAllAction.isEnabled = field.isFocusOwner
			super.show(c, x, y)
		}

		private val serialVersionUID = 1L

	}

	/** Pop-up box for the listMenu */
	private inner class ListBoxPopupMenu(private val listbox:JList<*>?):JPopupMenu() {
		private val copyAction:Action = object:AbstractAction(getUIText("Popup_Copy")) {
			private val serialVersionUID = 1L

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

		private val serialVersionUID = 1L

	}

	/** Pop-up list box for server selectionMenu */
	private inner class ServerSelectListBoxPopupMenu:JPopupMenu() {

		private val connectAction:Action = object:AbstractAction(getUIText("Popup_ServerSelect_Connect")) {
			private val serialVersionUID = 1L

			override fun actionPerformed(e:ActionEvent) {
				serverSelectConnectButtonClicked()
			}
		}
		private val deleteAction:Action = object:AbstractAction(getUIText("Popup_ServerSelect_Delete")) {
			private val serialVersionUID = 1L

			override fun actionPerformed(e:ActionEvent) {
				serverSelectDeleteButtonClicked()
			}
		}
		private val setObserverAction:Action = object:AbstractAction(getUIText("Popup_ServerSelect_SetObserver")) {
			private val serialVersionUID = 1L

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

		private val serialVersionUID = 1L

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
			private val serialVersionUID = 1L

			override fun actionPerformed(evt:ActionEvent) {
				val row = tableRoomList.selectedRow
				if(row!=-1) {
					val columnID = tablemodelRoomList.findColumn(getUIText(ROOMTABLE_COLUMNNAMES[0]))
					val strRoomID = tablemodelRoomList.getValueAt(row, columnID) as String
					val roomID = Integer.parseInt(strRoomID)
					joinRoom(roomID, false)
				}
			}
		}
		private val watchAction:Action = object:AbstractAction(getUIText("Popup_RoomTable_Watch")) {
			private val serialVersionUID = 1L

			override fun actionPerformed(evt:ActionEvent) {
				val row = tableRoomList.selectedRow
				if(row!=-1) {
					val columnID = tablemodelRoomList.findColumn(getUIText(ROOMTABLE_COLUMNNAMES[0]))
					val strRoomID = tablemodelRoomList.getValueAt(row, columnID) as String
					val roomID = Integer.parseInt(strRoomID)
					joinRoom(roomID, true)
				}
			}
		}
		private val detailAction:Action = object:AbstractAction(getUIText("Popup_RoomTable_Detail")) {
			private val serialVersionUID = 1L

			override fun actionPerformed(evt:ActionEvent) {
				val row = tableRoomList.selectedRow
				if(row!=-1) {
					val columnID = tablemodelRoomList.findColumn(getUIText(ROOMTABLE_COLUMNNAMES[0]))
					val strRoomID = tablemodelRoomList.getValueAt(row, columnID) as String
					val roomID = Integer.parseInt(strRoomID)
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

		private val serialVersionUID = 1L

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
					val roomID = Integer.parseInt(strRoomID)
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
					val roomID = Integer.parseInt(strRoomID)
					joinRoom(roomID, false)
				}
				e.consume()
			}
		}
	}

	/** Pop-up display field for logMenu */
	private inner class LogPopupMenu(field:JTextComponent):JPopupMenu() {

		private val copyAction:Action = object:AbstractAction(getUIText("Popup_Copy")) {
			private val serialVersionUID = 1L

			override fun actionPerformed(evt:ActionEvent) {
				field.copy()
			}
		}
		private val selectAllAction:Action = object:AbstractAction(getUIText("Popup_SelectAll")) {
			private val serialVersionUID = 1L

			override fun actionPerformed(evt:ActionEvent) {
				field.selectAll()
			}
		}
		private val clearAction:Action = object:AbstractAction(getUIText("Popup_Clear")) {
			private val serialVersionUID = 1L

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

		private val serialVersionUID = 1L

	}

	/** Display field for logKeyAdapter */
	private inner class LogKeyAdapter:KeyAdapter() {
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
			private val serialVersionUID = 1L

			override fun actionPerformed(e:ActionEvent) {
				val row = table.selectedRow

				if(row!=-1) {
					val strCopy = StringBuilder()

					for(column in 0 until table.columnCount) {
						val selectedObject = table.getValueAt(row, column)
						if(selectedObject is String)
							if(column==0)
								strCopy.append(selectedObject)
							else
								strCopy.append(",").append(selectedObject)
					}

					val ss = StringSelection(strCopy.toString())
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

		private val serialVersionUID = 1L

	}

	/** Rule entry for rule change screen */
	private inner class RuleEntry {
		/** File name */
		var filename:String = ""
		/** File path */
		var filepath:String = ""
		/** Rule name */
		var rulename:String = ""
		/** Game style */
		var style:Int = 0
	}

	/** Each label of Image Combobox<br></br>
	 * [Source](http://www.javadrive.jp/tutorial/jcombobox/index20.html) */
	private inner class ComboLabel constructor(var text:String = "", var icon:Icon? = null) {
	}

	/** ListCellRenderer for Image Combobox<br></br>
	 * [Source](http://www.javadrive.jp/tutorial/jcombobox/index20.html) */
	private inner class ComboLabelCellRenderer:JLabel(), ListCellRenderer<Any> {
		init {
			isOpaque = true
		}

		override fun getListCellRendererComponent(list:JList<out Any>?, value:Any?, index:Int, isSelected:Boolean, cellHasFocus:Boolean):Component? {
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

		private val serialVersionUID = 1L

	}

	companion object {
		/** Serial Version ID */
		private const val serialVersionUID = 1L

		/** Room-table column names. These strings will be passed to
		 * getUIText(String) subroutine. */
		val ROOMTABLE_COLUMNNAMES =
			arrayOf("RoomTable_ID", "RoomTable_Name", "RoomTable_Rated", "RoomTable_RuleName", "RoomTable_ModeName", "RoomTable_Status", "RoomTable_Players", "RoomTable_Spectators")

		/** End-of-game statistics column names. These strings will be passed to
		 * getUIText(String) subroutine. */
		val STATTABLE_COLUMNNAMES =
			arrayOf("StatTable_Rank", "StatTable_Name", "StatTable_Attack", "StatTable_APL", "StatTable_APM", "StatTable_Lines", "StatTable_LPM", "StatTable_Piece", "StatTable_PPS", "StatTable_Time", "StatTable_KO", "StatTable_Wins", "StatTable_Games")

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
		val TUNING_ABUTTON_ROTATE =
			arrayOf("GameTuning_RotateButtonDefaultRight_Auto", "GameTuning_RotateButtonDefaultRight_Left", "GameTuning_RotateButtonDefaultRight_Right")

		/** Tuning: Outline type names (before translation) */
		val TUNING_OUTLINE_TYPE_NAMES =
			arrayOf("GameTuning_OutlineType_Auto", "GameTuning_OutlineType_None", "GameTuning_OutlineType_Normal", "GameTuning_OutlineType_Connect", "GameTuning_OutlineType_SameColor")

		/** Spin bonus names */
		val COMBOBOX_SPINBONUS_NAMES = arrayOf("CreateRoom_TSpin_Disable", "CreateRoom_TSpin_TOnly", "CreateRoom_TSpin_All")

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
			arrayOf("ServerSelect", "Lobby", "ServerAdd", "CreateRatedWaiting", "CreateRated", "CreateRoom", "CreateRoom1P", "MPRanking", "RuleChange")

		/** Log */
		internal val log = Logger.getLogger(NetLobbyFrame::class.java)

		/** Get int value from JTextField
		 * @param value Default Value (used if convertion fails)
		 * @param txtfld JTextField
		 * @return int value (or default value if fails)
		 */
		fun getIntTextField(value:Int, txtfld:JTextField):Int {
			var v = value

			try {
				v = Integer.parseInt(txtfld.text)
			} catch(e:NumberFormatException) {
			}

			return v
		}

		/** Main functioncount
		 * @param args CommandLinesArgumentcount
		 */
		@JvmStatic fun main(args:Array<String>) {
			PropertyConfigurator.configure("config/etc/log.cfg")
			val frame = NetLobbyFrame()
			frame.init()
			frame.isVisible = true
		}
	}
}