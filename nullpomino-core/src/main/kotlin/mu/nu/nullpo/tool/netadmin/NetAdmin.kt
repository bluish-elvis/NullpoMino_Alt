/*
 * Copyright (c) 2010-2022, NullNoname
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

package mu.nu.nullpo.tool.netadmin

import biz.source_code.base64Coder.Base64Coder
import mu.nu.nullpo.game.net.*
import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.game.play.GameManager
import mu.nu.nullpo.util.CustomProperties
import mu.nu.nullpo.util.GeneralUtil
import mu.nu.nullpo.util.GeneralUtil.strDateTime
import net.clarenceho.crypto.RC4
import org.apache.logging.log4j.LogManager
import java.awt.*
import java.awt.datatransfer.StringSelection
import java.awt.event.*
import java.io.*
import java.util.Locale
import java.util.Vector
import javax.swing.*
import javax.swing.table.DefaultTableModel
import javax.swing.text.JTextComponent
import javax.swing.text.SimpleAttributeSet
import javax.swing.text.StyleConstants
import kotlin.system.exitProcess

/** NetAdmin - NetServer admin tool */
class NetAdmin:JFrame(), ActionListener, NetMessageListener {

	//***** Main GUI elements *****
	/** Layout manager for main screen */
	private val contentPaneCardLayout:CardLayout = CardLayout()

	/** Current screen-card number */
	private var currentScreenCardNumber = 0

	//***** Login screen elements *****
	/** Login Message label */
	private val labelLoginMessage:JLabel = JLabel()

	/** Server textbox */
	private val txtfldServer:JTextField = JTextField()

	/** Username textbox */
	private val txtfldUsername:JTextField = JTextField()

	/** Password textbox */
	private val passfldPassword:JPasswordField = JPasswordField()

	/** Remember Username checkbox */
	private val chkboxRememberUsername:JCheckBox = JCheckBox()

	/** Remember Password checkbox */
	private val chkboxRememberPassword:JCheckBox = JCheckBox()

	/** Login button */
	private val btnLogin:JButton = JButton()

	//***** Room list screen elements *****
	/** Room list data */
	private val tablemodelRoomList:DefaultTableModel = DefaultTableModel()

	/** Room list table */
	private val tableRoomList:JTable = JTable()

	//***** Lobby screen elements *****
	/** Console Log textpane */
	private val txtpaneConsoleLog:JTextPane = JTextPane()

	/** Console Command textbox */
	private val txtfldConsoleCommand:JTextField = JTextField()

	/** Console Command Execute button */
	private val btnConsoleCommandExecute:JButton = JButton()

	/** Users table data */
	private val tablemodelUsers:DefaultTableModel = DefaultTableModel()

	/** Users table component */
	private val tableUsers:JTable = JTable()

	/** MPRanking table data */
	private val tablemodelMPRanking:Array<DefaultTableModel> = Array(GameEngine.MAX_GAMESTYLE) {DefaultTableModel()}

	/** MPRanking table component */
	private var tableMPRanking:Array<JTable> = emptyArray()

	/** Load/Refresh Ranking button */
	private val btnRankingLoad:JButton = JButton()

	/** Constructor */
	init {
		init()
	}

	/** Init */
	private fun init() {
		// Load config file
		try {
			val `in` = FileInputStream("config/setting/netadmin.cfg")
			propConfig.load(`in`)
			`in`.close()
		} catch(e:IOException) {
		}

		// Load language files
		try {
			val `in` = FileInputStream("config/lang/netadmin_default.xml")
			propLangDefault.loadFromXML(`in`)
			`in`.close()
		} catch(e:IOException) {
			log.error("Failed to load default UI language file", e)
		}

		try {
			val `in` = FileInputStream("config/lang/netadmin_${Locale.getDefault().country}.xml")
			propLang.loadFromXML(`in`)
			`in`.close()
		} catch(e:IOException) {
		}

		// Set look&feel
		try {
			val propSwingConfig = CustomProperties()
			val `in` = FileInputStream("config/setting/swing.cfg")
			propSwingConfig.load(`in`)
			`in`.close()

			if(propSwingConfig.getProperty("option.usenativelookandfeel", true))
				try {
					UIManager.getInstalledLookAndFeels()
					UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
				} catch(e:Exception) {
					log.warn("Failed to set native look&feel", e)
				}

		} catch(e:Exception) {
		}

		// Set close action
		defaultCloseOperation = WindowConstants.DO_NOTHING_ON_CLOSE
		addWindowListener(object:WindowAdapter() {
			override fun windowClosing(e:WindowEvent?) {
				shutdown()
			}
		})

		title = getUIText("Title_ServerAdmin")
		initUI()

		this.setSize(propConfig.getProperty("mainwindow.width", 500), propConfig.getProperty("mainwindow.height", 450))
		this.setLocation(propConfig.getProperty("mainwindow.x", 0), propConfig.getProperty("mainwindow.y", 0))
		isVisible = true
	}

	/** Init GUI */
	private fun initUI() {
		contentPane.layout = contentPaneCardLayout

		initLoginUI()
		initLobbyUI()

		changeCurrentScreenCard(SCREENCARD_LOGIN)
	}

	/** Init login screen */
	private fun initLoginUI() {
		// Main panel
		val mpLoginOwner = JPanel(BorderLayout())
		contentPane.add(mpLoginOwner, SCREENCARD_NAMES[SCREENCARD_LOGIN])
		val mpLogin = JPanel()
		mpLogin.layout = BoxLayout(mpLogin, BoxLayout.Y_AXIS)
		mpLoginOwner.add(mpLogin, BorderLayout.NORTH)

		// * Login Message label
		labelLoginMessage.text = getUIText("Login_Message_Default")
		labelLoginMessage.alignmentX = 0f
		mpLogin.add(labelLoginMessage)

		// * Server panel
		val spServer = JPanel(BorderLayout())
		spServer.alignmentX = 0f
		mpLogin.add(spServer)

		// ** Server label
		spServer.add(JLabel(getUIText("Login_Server")), BorderLayout.WEST)

		// ** Server textbox
		txtfldServer.apply {
			columns = 30
			text = propConfig.getProperty("login.server", "")
			componentPopupMenu = TextComponentPopupMenu(this)
		}
		spServer.add(txtfldServer, BorderLayout.EAST)

		// * Username panel
		val spUsername = JPanel(BorderLayout())
		spUsername.alignmentX = 0f
		mpLogin.add(spUsername)

		// ** Username label
		spUsername.add(JLabel(getUIText("Login_Username")), BorderLayout.WEST)

		// ** Username textbox
		txtfldUsername.apply {
			columns = 30
			text = propConfig.getProperty("login.username", "")
			componentPopupMenu = TextComponentPopupMenu(this)
		}
		spUsername.add(txtfldUsername, BorderLayout.EAST)

		// * Password panel
		val spPassword = JPanel(BorderLayout())
		spPassword.alignmentX = 0f
		mpLogin.add(spPassword)

		// ** Password label
		spPassword.add(JLabel(getUIText("Login_Password")), BorderLayout.WEST)

		// ** Password textbox
		passfldPassword.apply {
			columns = 30
			val strPassword = propConfig.getProperty("login.password", "")
			if(strPassword.isNotEmpty()) text = NetUtil.decompressString(strPassword)
			componentPopupMenu = TextComponentPopupMenu(this)
		}
		spPassword.add(passfldPassword, BorderLayout.EAST)

		// * Remember Username checkbox
		chkboxRememberUsername.apply {
			text = getUIText("Login_RememberUsername")
			isSelected = propConfig.getProperty("login.rememberUsername", false)
			alignmentX = 0f
		}
		mpLogin.add(chkboxRememberUsername)

		// * Remember Password checkbox
		chkboxRememberPassword.apply {
			text = getUIText("Login_RememberPassword")
			isSelected = propConfig.getProperty("login.rememberPassword", false)
			alignmentX = 0f
		}
		mpLogin.add(chkboxRememberPassword)

		// * Buttons panel
		val spButtons = JPanel().apply {
			layout = BoxLayout(this, BoxLayout.X_AXIS)
			alignmentX = 0f
		}
		mpLogin.add(spButtons)

		// ** Login button
		spButtons.add(btnLogin.also {
			it.text = getUIText("Login_Login")
			it.setMnemonic('L')
			it.maximumSize = Dimension(java.lang.Short.MAX_VALUE.toInt(), it.maximumSize.height)
			it.actionCommand = "Login_Login"
			it.addActionListener(this)
		})

		// ** Quit button
		spButtons.add(JButton().also {
			it.text = getUIText("Login_Quit")
			it.setMnemonic('Q')
			it.maximumSize = Dimension(java.lang.Short.MAX_VALUE.toInt(), it.maximumSize.height)
			it.actionCommand = "Login_Quit"
			it.addActionListener(this)
		})
	}

	/** Init lobby screen */
	private fun initLobbyUI() {
		// Main panel
		val mpLobby = JPanel(BorderLayout())
		contentPane.add(mpLobby, SCREENCARD_NAMES[SCREENCARD_LOBBY])

		// * Tab
		val tabLobby = JTabbedPane()
		mpLobby.add(tabLobby, BorderLayout.CENTER)

		// ** Console tab
		val spConsole = JPanel(BorderLayout())
		tabLobby.addTab(getUIText("Lobby_Tab_Console"), spConsole)

		// *** Console log textpane
		txtpaneConsoleLog.apply {
			componentPopupMenu = LogPopupMenu(this)
			addKeyListener(LogKeyAdapter())
		}
		val sConsoleLog = JScrollPane(txtpaneConsoleLog)
		spConsole.add(sConsoleLog, BorderLayout.CENTER)

		// *** Command panel
		val spConsoleCommand = JPanel(BorderLayout())
		spConsole.add(spConsoleCommand, BorderLayout.SOUTH)

		// *** Command textbox
		txtfldConsoleCommand.apply {
			componentPopupMenu = TextComponentPopupMenu(this)
			spConsoleCommand.add(this, BorderLayout.CENTER)
		}

		// *** Command Execute button
		btnConsoleCommandExecute.also {
			it.text = getUIText("Lobby_Console_Execute")
			it.setMnemonic('E')
			it.actionCommand = "Lobby_Console_Execute"
			it.addActionListener(this)
			spConsoleCommand.add(it, BorderLayout.EAST)
		}

		// ** Users tab
		val spUsers = JPanel(BorderLayout())
		tabLobby.addTab(getUIText("Lobby_Tab_Users"), spUsers)

		// *** Users table
		tablemodelUsers.setColumnIdentifiers(Vector(USERTABLE_COLUMNNAMES.map {getUIText(it)}))
		tableUsers.apply {
			model = tablemodelUsers
			setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
			setDefaultEditor(Any::class.java, null)
			autoResizeMode = JTable.AUTO_RESIZE_OFF
			tableHeader.reorderingAllowed = false
			componentPopupMenu = UserPopupMenu(this)
			addMouseListener(object:MouseAdapter() {
				override fun mouseClicked(e:MouseEvent?) {
					e ?: return
					if(e.clickCount>=2) {
						val rowNumber = this@apply.selectedRow
						if(rowNumber!=-1) {
							val strIP = getValueAt(rowNumber, 0) as String
							openBanDialog(strIP)
						}
					}
				}
			})
		}
		val tmUsers = tableUsers.columnModel
		tmUsers.getColumn(0).preferredWidth = propConfig.getProperty("tableUsers.width.ip", 90) // IP
		tmUsers.getColumn(1).preferredWidth = propConfig.getProperty("tableUsers.width.host", 140) // Hostname
		tmUsers.getColumn(2).preferredWidth = propConfig.getProperty("tableUsers.width.type", 60) // Type
		tmUsers.getColumn(3).preferredWidth = propConfig.getProperty("tableUsers.width.name", 150) // Name

		val sUsers = JScrollPane(tableUsers)
		spUsers.add(sUsers, BorderLayout.CENTER)

		// ** Multiplayer Leaderboard tab
		val spMPRanking = JPanel(BorderLayout())
		tabLobby.addTab(getUIText("Lobby_Tab_MPRanking"), spMPRanking)

		// *** Game Style tab
		val tabMPRanking = JTabbedPane()
		spMPRanking.add(tabMPRanking, BorderLayout.CENTER)

		// ** Room List tab
		val spRoomList = JPanel(BorderLayout())
		tabLobby.addTab(getUIText("Lobby_Tab_RoomList"), spRoomList)

		// *** Room list table

		tablemodelRoomList.setColumnIdentifiers(Vector(ROOMTABLE_COLUMNNAMES.map {getUIText(it)}))
		tableRoomList.apply {
			model = tablemodelRoomList
			setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
			setDefaultEditor(Any::class.java, null)
			autoResizeMode = JTable.AUTO_RESIZE_OFF
			tableHeader.reorderingAllowed = false
			componentPopupMenu = RoomTablePopupMenu(this)
		}

		val tmRooms = tableRoomList.columnModel
		tmRooms.getColumn(0).preferredWidth = propConfig.getProperty("tableRoomList.width.id", 35) // ID
		tmRooms.getColumn(1).preferredWidth = propConfig.getProperty("tableRoomList.width.name", 155) // Name
		tmRooms.getColumn(2).preferredWidth = propConfig.getProperty("tableRoomList.width.rated", 50) // Rated
		tmRooms.getColumn(3).preferredWidth = propConfig.getProperty("tableRoomList.width.rulename", 105) // Rule name
		tmRooms.getColumn(4).preferredWidth = propConfig.getProperty("tableRoomList.width.status", 55) // Status
		tmRooms.getColumn(5).preferredWidth = propConfig.getProperty("tableRoomList.width.players", 65) // Players
		tmRooms.getColumn(6).preferredWidth = propConfig.getProperty("tableRoomList.width.spectators", 65) // Spectators

		val spTableRoomList = JScrollPane(tableRoomList)
		spRoomList.add(spTableRoomList, BorderLayout.CENTER)

		// *** Leaderboard table
		tablemodelMPRanking.forEach {item ->
			item.setColumnIdentifiers(Vector(MPRANKING_COLUMNNAMES.map {getUIText(it)}))
		}
		tableMPRanking = Array(GameEngine.MAX_GAMESTYLE) {
			JTable(tablemodelMPRanking[it]).apply {
				setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
				setDefaultEditor(Any::class.java, null)
				autoResizeMode = JTable.AUTO_RESIZE_OFF
				tableHeader.reorderingAllowed = false
				componentPopupMenu = MPRankingPopupMenu(this)
			}
		}

		for(i in 0 until GameEngine.MAX_GAMESTYLE) {
			tableMPRanking[i].columnModel.run {
				getColumn(0).preferredWidth = propConfig.getProperty("tableMPRanking.width.rank", 30) // Rank
				getColumn(1).preferredWidth = propConfig.getProperty("tableMPRanking.width.name", 200) // Name
				getColumn(2).preferredWidth = propConfig.getProperty("tableMPRanking.width.rating", 60) // Rating
				getColumn(3).preferredWidth = propConfig.getProperty("tableMPRanking.width.play", 60) // Play
				getColumn(4).preferredWidth = propConfig.getProperty("tableMPRanking.width.win", 60) // Win
			}

			tabMPRanking.addTab(GameEngine.GAMESTYLE_NAMES[i], JScrollPane(tableMPRanking[i]))
		}

		// *** Load/Refresh Ranking button
		spMPRanking.add(btnRankingLoad.also {
			it.text = getUIText("MPRanking_Button_LoadRanking")
			it.setMnemonic('L')
			it.actionCommand = "MPRanking_Button_LoadRanking"
			it.addActionListener(this)
		}, BorderLayout.SOUTH)
	}

	/** Save settings */
	private fun saveConfig() {
		propConfig.setProperty("mainwindow.width", size.width)
		propConfig.setProperty("mainwindow.height", size.height)
		propConfig.setProperty("mainwindow.x", location.x)
		propConfig.setProperty("mainwindow.y", location.y)

		val tmUsers = tableUsers.columnModel
		propConfig.setProperty("tableUsers.width.ip", tmUsers.getColumn(0).width)
		propConfig.setProperty("tableUsers.width.host", tmUsers.getColumn(1).width)
		propConfig.setProperty("tableUsers.width.type", tmUsers.getColumn(2).width)
		propConfig.setProperty("tableUsers.width.name", tmUsers.getColumn(3).width)

		for(i in 0 until GameEngine.MAX_GAMESTYLE) {
			val tm = tableMPRanking[i].columnModel
			propConfig.setProperty("tableMPRanking.width.rank", tm.getColumn(0).width)
			propConfig.setProperty("tableMPRanking.width.name", tm.getColumn(1).width)
			propConfig.setProperty("tableMPRanking.width.rating", tm.getColumn(2).width)
			propConfig.setProperty("tableMPRanking.width.play", tm.getColumn(3).width)
			propConfig.setProperty("tableMPRanking.width.win", tm.getColumn(4).width)
		}

		try {
			val out = FileOutputStream("config/setting/netadmin.cfg")
			propConfig.store(out, "NullpoMino NetAdmin Config")
			out.close()
		} catch(e:IOException) {
			log.warn("Failed to save netlobby config file", e)
		}

	}

	/** Change current screen card
	 * @param cardNumber Screen card ID
	 */
	private fun changeCurrentScreenCard(cardNumber:Int) {
		contentPaneCardLayout.show(contentPane, SCREENCARD_NAMES[cardNumber])
		currentScreenCardNumber = cardNumber

		// Set default button
		var defaultButton:JButton? = null
		when(cardNumber) {
			SCREENCARD_LOGIN -> defaultButton = btnLogin
			SCREENCARD_LOBBY -> defaultButton = btnConsoleCommandExecute
		}

		if(defaultButton!=null) getRootPane().defaultButton = defaultButton
	}

	/** Enable/Disable Login screen UI elements
	 * @param b true to enable, false to disable
	 */
	private fun setLoginUIEnabled(b:Boolean) {
		txtfldServer.isEnabled = b
		txtfldUsername.isEnabled = b
		passfldPassword.isEnabled = b
		chkboxRememberUsername.isEnabled = b
		chkboxRememberPassword.isEnabled = b
		btnLogin.isEnabled = b
	}

	/** Disconnect from the server */
	private fun logout() {
		if(client!=null) {
			if(client!!.isConnected) client!!.send("disconnect\n")
			client!!.removeListener(this)
			client!!.threadRunning = false
			client!!.interrupt()
			client = null
		}

		SwingUtilities.invokeLater {
			setLoginUIEnabled(true)
			changeCurrentScreenCard(SCREENCARD_LOGIN)
		}
	}

	/** Shutdown this program */
	fun shutdown() {
		logout()
		saveConfig()
		exitProcess(0)
	}

	/** Send admin command
	 * @param msg Command to send
	 * @return true if successful
	 */
	private fun sendCommand(msg:String):Boolean {
		if(client==null||!client!!.isConnected) return false
		val strCommand = NetUtil.compressString(msg)
		return client!!.send("admin\t$strCommand\n")
	}

	/** Add message to console
	 * @param str Message
	 * @param fgcolor Text cint (can be null)
	 */
	private fun addConsoleLog(str:String?, fgcolor:Color? = null) {
		var sas:SimpleAttributeSet? = null
		if(fgcolor!=null) {
			sas = SimpleAttributeSet()
			StyleConstants.setForeground(sas, fgcolor)
		}
		try {
			val doc = txtpaneConsoleLog.document
			str?.let {doc.insertString(doc.length, "$it\n", sas)}
			txtpaneConsoleLog.caretPosition = doc.length
		} catch(e:Exception) {
		}

	}

	/** Execute a console command
	 * @param commands Command line (split by every single space)
	 * @param fullCommandLine Command line (raw String)
	 */
	private fun executeConsoleCommand(commands:Array<String>, fullCommandLine:String) {
		if(commands.isEmpty()||fullCommandLine.isEmpty()) return

		addConsoleLog(">$fullCommandLine", Color.blue)

		// help/h/?
		if(commands[0].equals("help", ignoreCase = true)||commands[0].equals("h", ignoreCase = true)||commands[0].equals(
				"?",
				ignoreCase = true
			))
			try {
				val reader:InputStreamReader = try {
					InputStreamReader(FileInputStream("config/lang/netadmin_help_${Locale.getDefault().country}.txt"), "UTF-8")
				} catch(e2:IOException) {
					InputStreamReader(FileInputStream("config/lang/netadmin_help_default.txt"), "UTF-8")
				}

				BufferedReader(reader).readLines().forEach {addConsoleLog(it)}

				reader.close()
			} catch(e:IOException) {
				log.error("Failed to load help file", e)
				addConsoleLog(String.format(getUIText("Console_Help_Error"), "$e"), Color.red)
			}
		else if(commands[0].equals("echo", ignoreCase = true)) {
			val strTemp = GeneralUtil.stringCombine(commands, " ", 1)
			addConsoleLog(strTemp)
		} else if(commands[0].equals("cls", ignoreCase = true))
			txtpaneConsoleLog.text = null
		else if(commands[0].equals("logout", ignoreCase = true)||commands[0].equals("logoff", ignoreCase = true)
			||commands[0].equals("disconnect", ignoreCase = true)) {
			addConsoleLog(getUIText("Console_Logout"))
			labelLoginMessage.foreground = Color.black
			labelLoginMessage.text = getUIText("Login_Message_LoggingOut")
			logout()
		} else if(commands[0].equals("quit", ignoreCase = true)||commands[0].equals("exit", ignoreCase = true))
			shutdown()
		else if(commands[0].equals("shutdown", ignoreCase = true)) {
			addConsoleLog(getUIText("Console_Shutdown"))
			isWantedDisconnect = true
			isShutdownRequested = true
			sendCommand("shutdown")
		} else if(commands[0].equals("announce", ignoreCase = true)) {
			val strTemp = GeneralUtil.stringCombine(commands, " ", 1)
			if(strTemp.isNotEmpty()) {
				sendCommand("announce\t${NetUtil.urlEncode(strTemp)}")
				addConsoleLog(getUIText("Console_Announce")+strTemp)
			}
		} else if(commands[0].equals("myip", ignoreCase = true)) addConsoleLog(strMyIP)
		else if(commands[0].equals("myhost", ignoreCase = true)) addConsoleLog(strMyHostname)
		else if(commands[0].equals("serverip", ignoreCase = true)) addConsoleLog(client!!.ip)
		else if(commands[0].equals("serverhost", ignoreCase = true)) addConsoleLog(strServerHost)
		else if(commands[0].equals("serverport", ignoreCase = true)) addConsoleLog("$serverPort")
		else if(commands[0].equals("version", ignoreCase = true)) {
			addConsoleLog("Client:"+GameManager.versionString)
			addConsoleLog("Server:$serverFullVer")
		} else if(commands[0].equals("bangui", ignoreCase = true)) {
			if(commands.size>1) openBanDialog(commands[1])
			else openBanDialog("")
		} else if(commands[0].equals("ban", ignoreCase = true)) {
			when {
				commands.size>2 -> {
					var banLength = -1
					try {
						banLength = commands[2].toInt()
					} catch(e:NumberFormatException) {
						addConsoleLog(String.format(getUIText("Console_Ban_InvalidLength"), commands[2]))
						return
					}

					if(banLength<-1||banLength>6) {
						addConsoleLog(String.format(getUIText("Console_Ban_InvalidLength"), commands[2]))
						return
					}
					requestBanFromGUI(commands[1], banLength, false)
				}
				commands.size>1 -> requestBanFromGUI(commands[1], -1, false)
				else -> addConsoleLog(getUIText("Console_Ban_NoParams"))
			}
		} else if(commands[0].equals("banlist", ignoreCase = true))
			sendCommand("banlist")
		else if(commands[0].equals("unban", ignoreCase = true)) {
			if(commands.size>1) sendCommand("unban\t${commands[1]}")
			else addConsoleLog(getUIText("Console_UnBan_NoParams"))
		} else if(commands[0].equals("playerdelete", ignoreCase = true)||commands[0].equals("pdel", ignoreCase = true)) {
			val strTemp = GeneralUtil.stringCombine(commands, " ", 1)
			if(strTemp.isNotEmpty()) sendCommand("playerdelete\t$strTemp")
			else addConsoleLog(getUIText("Console_PlayerDelete_NoParams"))
		} else if(commands[0].equals("roomdelete", ignoreCase = true)||commands[0].equals("rdel", ignoreCase = true)) {
			if(commands.size>1) sendCommand("roomdelete\t${commands[1]}")
			else addConsoleLog(getUIText("Console_RoomDelete_NoParams"))
		} else addConsoleLog(String.format(getUIText("Console_UnknownCommand"), commands[0]))// roomdelete/rdef
		// playerdelete/pdel
		// banlist
		// ban
		// bangui
		// myip
		// announce
		// quit/exit/shutdown
		// cls
	}

	/** Sets a ban.
	 * @param strIP IP
	 * @param banLength Length of ban (-1:Kick only)
	 * @param showMessage true if display a confirm dialog
	 */
	private fun requestBanFromGUI(strIP:String?, banLength:Int, showMessage:Boolean) {
		if(strIP==null||strIP.isEmpty()) return

		if(banLength==-1) {
			var answer = JOptionPane.YES_OPTION

			if(showMessage)
				answer = JOptionPane.showConfirmDialog(
					this, getUIText("Message_ConfirmKick")+"\n"
						+strIP, getUIText("Title_ConfirmKick"), JOptionPane.YES_NO_OPTION
				)

			if(answer==JOptionPane.YES_OPTION) sendCommand("ban\t$strIP")
		} else {
			var answer = JOptionPane.YES_OPTION

			if(showMessage)
				answer =
					JOptionPane.showConfirmDialog(
						this,
						String.format(getUIText("Message_ConfirmBan"), getUIText("BanType$banLength"))+"\n"+strIP,
						getUIText("Title_ConfirmBan"), JOptionPane.YES_NO_OPTION
					)

			if(answer==JOptionPane.YES_OPTION) sendCommand("ban\t$strIP\t$banLength")
		}
	}

	/** Open ban dialog
	 * @param strIP Default IP
	 */
	private fun openBanDialog(strIP:String?) {
		// Dialog box
		val dialogBan = JDialog()
		dialogBan.title = getUIText("Title_BanDialog")
		dialogBan.contentPane.layout = BoxLayout(dialogBan.contentPane, BoxLayout.Y_AXIS)

		// IP options
		val pBanIP = JPanel()
		dialogBan.contentPane.add(pBanIP)

		val lBanIP = JLabel(getUIText("Ban_IP"))
		pBanIP.add(lBanIP)

		val txtfldBanIP = JTextField(16)
		if(strIP!=null) txtfldBanIP.text = strIP
		txtfldBanIP.componentPopupMenu = TextComponentPopupMenu(txtfldBanIP)
		pBanIP.add(txtfldBanIP)

		// Ban length Options
		val pBanLength = JPanel()
		dialogBan.contentPane.add(pBanLength)

		// Ban length
		val lBanLength = JLabel(getUIText("Ban_Length"))
		pBanLength.add(lBanLength)

		val comboboxBanLength =
			JComboBox((-1 until NetServerBan.BANLENGTH_TOTAL).map {getUIText("BanType$it")}.toTypedArray()).apply {
				toolTipText = getUIText("Ban_Length_Tip")
				pBanLength.add(this)
			}

		// Buttons
		val pButtons = JPanel()
		dialogBan.contentPane.add(pButtons)

		val btnConfirm = JButton(getUIText("Ban_Confirm"))
		btnConfirm.setMnemonic('O')
		btnConfirm.addActionListener {
			requestBanFromGUI(txtfldBanIP.text, comboboxBanLength.selectedIndex-1, false)
			dialogBan.dispose()
		}
		pButtons.add(btnConfirm)

		val btnCancel = JButton(getUIText("Ban_Cancel"))
		btnCancel.setMnemonic('C')
		btnCancel.addActionListener {dialogBan.dispose()}
		pButtons.add(btnCancel)

		// Set frame vitals
		dialogBan.defaultCloseOperation = WindowConstants.DISPOSE_ON_CLOSE
		dialogBan.setLocationRelativeTo(null)
		dialogBan.isModal = true
		dialogBan.isResizable = false
		dialogBan.rootPane.defaultButton = btnConfirm
		dialogBan.pack()
		dialogBan.isVisible = true
	}

	/* Button clicked */
	override fun actionPerformed(e:ActionEvent) {
		// Login
		if(e.actionCommand=="Login_Login")
			if(txtfldUsername.text.isNotEmpty()&&passfldPassword.password.isNotEmpty()) {
				setLoginUIEnabled(false)
				labelLoginMessage.foreground = Color.black
				labelLoginMessage.text = getUIText("Login_Message_Connecting")

				// Get hostname and port number
				var strHost = txtfldServer.text
				if(strHost.isEmpty()) strHost = "127.0.0.1"
				var portSpliter = strHost.indexOf(':')
				if(portSpliter==-1) portSpliter = strHost.length

				strServerHost = strHost.take(portSpliter)

				serverPort = NetBaseClient.DEFAULT_PORT
				try {
					val strPort = strHost.substring(portSpliter+1, strHost.length)
					serverPort = strPort.toInt()
				} catch(e2:Exception) {
				}

				// Begin connect
				isWantedDisconnect = false
				isShutdownRequested = false
				client = NetBaseClient(strServerHost, serverPort)
				client!!.isDaemon = true
				client!!.addListener(this)
				client!!.start()
			}
		// Quit
		if(e.actionCommand=="Login_Quit") shutdown()
		// Execute console command
		if(e.actionCommand=="Lobby_Console_Execute") {
			val commandline = txtfldConsoleCommand.text
			val commands = commandline.split(" ".toRegex()).dropLastWhile {it.isEmpty()}.toTypedArray()
			executeConsoleCommand(commands, commandline)
			txtfldConsoleCommand.text = ""
		}
		// Load/Refresh Ranking
		if(e.actionCommand=="MPRanking_Button_LoadRanking") {
			btnRankingLoad.isEnabled = false
			client!!.send("mpranking\t0\n")
		}
	}

	/* Received a message */
	override fun netOnMessage(client:NetBaseClient, message:Array<String>) {
		//if(message.length > 0) log.debug(message[0]);

		// Welcome
		if(message[0]=="welcome") {
			//welcome\t[MAJOR VERSION]\t[PLAYERS]\t[OBSERVERS]\t[MINOR VERSION]\t[FULL VERSION]\t[PING INTERVAL]\t[DEV BUILD]
			labelLoginMessage.foreground = Color.black
			labelLoginMessage.text = getUIText("Login_Message_LoggingIn")

			// Version check
			val clientMajorVer = GameManager.versionMajor
			val serverMajorVer = message[1].toFloat()

			if(clientMajorVer!=serverMajorVer) {
				labelLoginMessage.foreground = Color.red
				labelLoginMessage.text = String.format(getUIText("Login_Message_VersionError"), clientMajorVer, serverMajorVer)
				isWantedDisconnect = true
				logout()
				return
			}

			// Build type check
			val clientBuildType = GameManager.isDevBuild
			val serverBuildType = message[7].toBoolean()

			if(clientBuildType!=serverBuildType) {
				val strClientBuildType = GameManager.getBuildTypeString(clientBuildType)
				val strServerBuildType = GameManager.getBuildTypeString(serverBuildType)
				labelLoginMessage.foreground = Color.red
				labelLoginMessage.text =
					String.format(getUIText("Login_Message_BuildTypeError"), strClientBuildType, strServerBuildType)
				isWantedDisconnect = true
				logout()
				return
			}

			serverFullVer = message[5]

			// Ping interval
			val pingInterval:Long =
				if(message.size>6) message[6].toLong() else NetBaseClient.PING_INTERVAL
			if(pingInterval!=NetBaseClient.PING_INTERVAL) client.startPingTask(pingInterval)

			// Send login message
			val strUsername = txtfldUsername.text

			val rc4 = RC4(passfldPassword.password)
			val ePassword = rc4.rc4(NetUtil.stringToBytes(strUsername))
			val b64Password = Base64Coder.encode(ePassword)

			val strLogin = ("adminlogin\t$clientMajorVer\t$strUsername\t${String(b64Password)}\t"
				+clientBuildType+"\n")
			log.debug("Send login message:$strLogin")
			client.send(strLogin)
		}
		// Login failed
		if(message[0]=="adminloginfail") {
			isWantedDisconnect = true
			logout()

			labelLoginMessage.foreground = Color.red

			labelLoginMessage.text = getUIText(
				if(message.size>1&&message[1]=="DISABLE") "Login_Message_DisabledError" else "Login_Message_LoginError"
			)
			return
		}
		// Banned
		if(message[0]=="banned") {
			isWantedDisconnect = true
			logout()

			labelLoginMessage.foreground = Color.red

			val cStart = GeneralUtil.importCalendarString(message[1])
			val cExpire = if(message.size>2&&message[2].isNotEmpty()) GeneralUtil.importCalendarString(message[2]) else null

			val strStart = cStart?.strDateTime ?: "???"
			val strExpire = cExpire?.strDateTime ?: getUIText("Login_Message_Banned_Permanent")

			labelLoginMessage.text = String.format(getUIText("Login_Message_Banned"), strStart, strExpire)
			return
		}
		// Login successful
		if(message[0]=="adminloginsuccess") {
			strMyIP = message[1]
			strMyHostname = message[2]

			propConfig.setProperty("login.rememberUsername", chkboxRememberUsername.isSelected)
			propConfig.setProperty("login.rememberPassword", chkboxRememberPassword.isSelected)

			propConfig.setProperty("login.server", txtfldServer.text)

			propConfig.setProperty("login.username", if(chkboxRememberUsername.isSelected) txtfldUsername.text else "")
			propConfig.setProperty(
				"login.password", if(chkboxRememberPassword.isSelected)
					NetUtil.compressString(String(passfldPassword.password)) else ""
			)

			addConsoleLog(String.format(getUIText("Console_LoginOK"), strServerHost, serverPort))

			SwingUtilities.invokeLater {changeCurrentScreenCard(SCREENCARD_LOBBY)}
		}
		// Multiplayer Leaderboard
		if(message[0]=="mpranking") {
			btnRankingLoad.isEnabled = true

			val style = message[1].toInt()

			tablemodelMPRanking[style].rowCount = 0

			val strPData = NetUtil.decompressString(message[3])
			val strPDataA = strPData.split("\t".toRegex()).dropLastWhile {it.isEmpty()}.toTypedArray()

			for(element in strPDataA) {
				val strRankData = element.split(";".toRegex()).dropLastWhile {it.isEmpty()}.toTypedArray()

				if(strRankData.size>=MPRANKING_COLUMNNAMES.size) {
					val strRowData = arrayOfNulls<String>(MPRANKING_COLUMNNAMES.size)
					val rank = strRankData[0].toInt()
					if(rank==-1)
						strRowData[0] = "N/A"
					else
						strRowData[0] = (rank+1).toString()
					strRowData[1] = NetUtil.urlDecode(strRankData[1])
					strRowData[2] = strRankData[2]
					strRowData[3] = strRankData[3]
					strRowData[4] = strRankData[4]
					tablemodelMPRanking[style].addRow(strRowData)
				}
			}
		}
		// Room List
		if(message[0]=="roomlist") {
			val size = message[1].toInt()

			tablemodelRoomList.rowCount = 0
			for(i in 0 until size) {
				val r = NetRoomInfo(message[2+i])
				tablemodelRoomList.addRow(createRoomListRowData(r))
			}
		}
		// New room appeared
		if(message[0]=="roomcreate") {
			val r = NetRoomInfo(message[1])
			tablemodelRoomList.addRow(createRoomListRowData(r))
		}
		// Room update
		if(message[0]=="roomupdate") {
			val r = NetRoomInfo(message[1])
			val columnID = tablemodelRoomList.findColumn(getUIText(ROOMTABLE_COLUMNNAMES[0]))

			for(i in 0 until tablemodelRoomList.rowCount) {
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
		// Room delete
		if(message[0]=="roomdelete") {
			val r = NetRoomInfo(message[1])
			val columnID = tablemodelRoomList.findColumn(getUIText(ROOMTABLE_COLUMNNAMES[0]))

			for(i in 0 until tablemodelRoomList.rowCount) {
				val strID = tablemodelRoomList.getValueAt(i, columnID) as String
				val roomID = strID.toInt()

				if(roomID==r.roomID) {
					tablemodelRoomList.removeRow(i)
					break
				}
			}
		}
		// Admin command result
		if(message[0]=="adminresult")
			if(message.size>1) {
				val strAdminResultTemp = NetUtil.decompressString(message[1])
				val strAdminResultArray = strAdminResultTemp.split("\t".toRegex()).dropLastWhile {it.isEmpty()}.toTypedArray()
				onAdminResultMessage(client, strAdminResultArray)
			}
	}

	/** Create a row of room list
	 * @param r NetRoomInfo
	 * @return Row data
	 */
	private fun createRoomListRowData(r:NetRoomInfo):Array<String> = arrayOf(
		r.roomID.toString(), r.strName, if(r.rated) getUIText("RoomTable_Rated_True") else getUIText("RoomTable_Rated_False"),
		if(r.ruleLock) r.ruleName.uppercase(Locale.getDefault()) else getUIText("RoomTable_RuleName_Any"),
		if(r.playing) getUIText("RoomTable_Status_Playing") else getUIText("RoomTable_Status_Waiting"),
		"${r.playerSeatedCount}/${r.maxPlayers}", "${r.spectatorCount}"
	)

	/** When received an admin command result
	 * @param client NetBaseClient
	 * @param message Message
	 */
	private fun onAdminResultMessage(client:NetBaseClient, message:Array<String>) {
		// Client list
		if(message[0]=="clientlist") {
			// Get current selected IP and Type
			var strSelectedIP:String? = null
			var strSelectedType:String? = null
			if(tableUsers.selectedRow!=-1) {
				strSelectedIP = tablemodelUsers.getValueAt(tableUsers.selectedRow, 0) as String
				strSelectedType = tablemodelUsers.getValueAt(tableUsers.selectedRow, 2) as String
			}
			tableUsers.selectionModel.clearSelection()

			// Set number of rows
			if(tablemodelUsers.rowCount>message.size-1) tablemodelUsers.rowCount = message.size-1

			for(i in 1 until message.size) {
				val strClientData = message[i].split("\\|".toRegex()).dropLastWhile {it.isEmpty()}.toTypedArray()

				val strIP = strClientData[0] // IP
				val strHost = strClientData[1] // Hostname

				// Type of the client
				val type = strClientData[2].toInt()
				var strType = getUIText(USERTABLE_USERTYPES[type])
				if(strIP==strMyIP&&strHost==strMyHostname&&type==3)
					strType = "*"+getUIText(USERTABLE_USERTYPES[type])

				// Player info
				var pInfo:NetPlayerInfo? = null
				if(type==1&&strClientData.size>3) {
					val strPlayerInfoTemp = strClientData[3]
					pInfo = NetPlayerInfo(strPlayerInfoTemp)
				}

				// Create a row data
				val strTableData = arrayOfNulls<String>(tablemodelUsers.columnCount)
				strTableData[0] = strIP
				strTableData[1] = strHost
				strTableData[2] = strType
				strTableData[3] = pInfo?.strName ?: ""

				// Add the row data
				val rowNumber = i-1
				val maxRow = tablemodelUsers.rowCount
				if(rowNumber<maxRow) {
					// Modify an existing row
					for(j in strTableData.indices)
						tablemodelUsers.setValueAt(strTableData[j], rowNumber, j)

					// Set selected row
					if(strSelectedIP!=null&&strSelectedType!=null&&
						strSelectedIP==strIP&&strSelectedType==strType) {
						tableUsers.selectionModel.setSelectionInterval(rowNumber, rowNumber)
						strSelectedIP = null
						strSelectedType = null
					}
				} else {
					// Add an new row
					tablemodelUsers.addRow(strTableData)

					// Set selected row
					if(strSelectedIP!=null&&strSelectedType!=null&&
						strSelectedIP==strIP&&strSelectedType==strType) {
						tableUsers.selectionModel.setSelectionInterval(maxRow, maxRow)
						strSelectedIP = null
						strSelectedType = null
					}
				}
			}
		}
		// Ban
		if(message[0]=="ban")
			if(message.size>3) {
				val strBanLength = getUIText("BanType"+message[2])
				addConsoleLog(String.format(getUIText("Console_Ban_Result"), message[1], strBanLength, message[3]), Color(0, 64, 64))
			}
		// Ban List
		if(message[0]=="banlist")
			if(message.size<2)
				addConsoleLog(getUIText("Console_BanList_Result_None"), Color(0, 64, 64))
			else
				for(i in 0 until message.size-1) {
					val ban = NetServerBan()
					ban.importString(message[i+1])

					val strBanLength = getUIText("BanType"+ban.banLength)
					val strDate = ban.startDate.strDateTime

					addConsoleLog(String.format(getUIText("Console_BanList_Result"), ban.addr, strBanLength, strDate), Color(0, 64, 64))
				}
		// Un-Ban
		if(message[0]=="unban")
			if(message.size>2)
				addConsoleLog(String.format(getUIText("Console_UnBan_Result"), message[1], message[2]), Color(0, 64, 64))
		// Player Delete
		if(message[0]=="playerdelete")
			if(message.size>1)
				addConsoleLog(String.format(getUIText("Console_PlayerDelete_Result"), message[1]), Color(0, 64, 64))
		// Room Delete (OK)
		if(message[0]=="roomdeletesuccess")
			if(message.size>2)
				addConsoleLog(String.format(getUIText("Console_RoomDelete_OK"), message[1], message[2]), Color(0, 64, 64))
		// Room Delete (NG)
		if(message[0]=="roomdeletefail")
			if(message.size>1) addConsoleLog(String.format(getUIText("Console_RoomDelete_NG"), message[1]), Color(0, 64, 64))
		// Diagnostics
		if(message[0]=="diag") if(message.size>1) addConsoleLog(message[1], Color(0, 64, 64))
	}

	/* Disconnected */
	override fun netOnDisconnect(client:NetBaseClient, ex:Throwable?) {
		if(isShutdownRequested) {
			log.info("Server shutdown completed")
			labelLoginMessage.foreground = Color.black
			labelLoginMessage.text = getUIText("Login_Message_Shutdown")
		} else if(isWantedDisconnect)
			log.info("Disconnected from the server")
		else {
			labelLoginMessage.foreground = Color.red
			if(ex==null) {
				log.warn("ERROR Disconnected! (null)")
				labelLoginMessage.text = String.format(getUIText("Login_Message_UnwantedDisconnect"), "(null)")
			} else {
				log.error("ERROR Disconnected!", ex)
				labelLoginMessage.text = String.format(getUIText("Login_Message_UnwantedDisconnect"), "$ex")
			}
		}
		logout()
	}

	/** Popup menu for text components */
	private class TextComponentPopupMenu(field:JTextComponent):JPopupMenu() {

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

	/** Popup menu for console log */
	private class LogPopupMenu(field:JTextComponent):JPopupMenu() {

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

	/** Popup menu for users table */
	private inner class UserPopupMenu(table:JTable):JPopupMenu() {

		private val copyAction:Action = object:AbstractAction(getUIText("Popup_Copy")) {

			override fun actionPerformed(e:ActionEvent) {
				copyTableRowToClipboard(table)
			}
		}
		private val kickAction:Action = object:AbstractAction(getUIText("Popup_Kick")) {

			override fun actionPerformed(evt:ActionEvent) {
				val rowNumber = table.selectedRow
				val strIP = table.getValueAt(rowNumber, 0) as String
				requestBanFromGUI(strIP, -1, true)
			}
		}
		private val banAction:Action = object:AbstractAction(getUIText("Popup_Ban")) {

			override fun actionPerformed(evt:ActionEvent) {
				val rowNumber = table.selectedRow
				val strIP = table.getValueAt(rowNumber, 0) as String
				openBanDialog(strIP)
			}
		}

		init {

			add(copyAction)
			add(kickAction)
			add(banAction)
		}

		override fun show(c:Component, x:Int, y:Int) {
			val table = c as JTable
			val flg = table.selectedRow!=-1
			copyAction.isEnabled = flg
			kickAction.isEnabled = flg
			banAction.isEnabled = flg
			super.show(c, x, y)
		}

	}

	/** Popup menu for leaderboard table */
	private inner class MPRankingPopupMenu(table:JTable):JPopupMenu() {

		private val copyAction:Action = object:AbstractAction(getUIText("Popup_Copy")) {

			override fun actionPerformed(e:ActionEvent) {
				copyTableRowToClipboard(table)
			}
		}
		private val deleteAction:Action = object:AbstractAction(getUIText("Popup_Delete")) {

			override fun actionPerformed(evt:ActionEvent) {
				val rowNumber = table.selectedRow
				val strName = table.getValueAt(rowNumber, 1) as String
				sendCommand("playerdelete\t$strName")
				client!!.send("mpranking\t0\n")
			}
		}

		init {
			add(copyAction)
			add(deleteAction)
		}

		override fun show(c:Component, x:Int, y:Int) {
			val table = c as JTable
			val flg = table.selectedRow!=-1
			copyAction.isEnabled = flg
			deleteAction.isEnabled = flg
			super.show(c, x, y)
		}

	}

	/** Popup menu for room list table */
	private inner class RoomTablePopupMenu(table:JTable):JPopupMenu() {

		private val copyAction:Action = object:AbstractAction(getUIText("Popup_Copy")) {

			override fun actionPerformed(e:ActionEvent) {
				copyTableRowToClipboard(table)
			}
		}
		private val deleteAction:Action = object:AbstractAction(getUIText("Popup_Delete")) {

			override fun actionPerformed(evt:ActionEvent) {
				val rowNumber = table.selectedRow
				val strID = table.getValueAt(rowNumber, 0) as String
				sendCommand("roomdelete\t$strID")
			}
		}

		init {

			add(copyAction)
			add(deleteAction)
		}

		override fun show(c:Component, x:Int, y:Int) {
			val table = c as JTable
			val flg = table.selectedRow!=-1
			copyAction.isEnabled = flg
			deleteAction.isEnabled = flg
			super.show(c, x, y)
		}

	}

	/** KeyAdapter for console log */
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

	companion object {
		//***** Constants *****
		/** Serial Version ID */
		private const val serialVersionUID = 1L

		/** Constants for each screen-card */
		private const val SCREENCARD_LOGIN = 0
		private const val SCREENCARD_LOBBY = 1

		/** Names for each screen-card */
		private val SCREENCARD_NAMES = arrayOf("Login", "Lobby")

		/** User type names */
		private val USERTABLE_USERTYPES =
			arrayOf("UserTable_Type_Guest", "UserTable_Type_Player", "UserTable_Type_Observer", "UserTable_Type_Admin")

		/** User table column names. These strings will be passed to
		 * getUIText(String) subroutine. */
		private val USERTABLE_COLUMNNAMES = arrayOf("UserTable_IP", "UserTable_Hostname", "UserTable_Type", "UserTable_Name")

		/** Multiplayer leaderboard column names. These strings will be passed to
		 * getUIText(String) subroutine. */
		private val MPRANKING_COLUMNNAMES =
			arrayOf("MPRanking_Rank", "MPRanking_Name", "MPRanking_Rating", "MPRanking_PlayCount", "MPRanking_WinCount")

		/** Room-table column names. These strings will be passed to
		 * getUIText(String) subroutine. */
		private val ROOMTABLE_COLUMNNAMES =
			arrayOf(
				"RoomTable_ID", "RoomTable_Name", "RoomTable_Rated", "RoomTable_RuleName", "RoomTable_Status",
				"RoomTable_Players", "RoomTable_Spectators"
			)

		//***** Variables *****
		/** Log */
		internal val log = LogManager.getLogger()

		/** ServerAdmin properties */
		private val propConfig = CustomProperties()

		/** Default language file */
		private val propLangDefault = CustomProperties()

		/** Property file for GUI translations */
		private val propLang = CustomProperties()

		/** NetBaseClient */
		private var client:NetBaseClient? = null

		/** True if disconnection is intended (If false, it will display error
		 * message) */
		private var isWantedDisconnect = false

		/** True if server shutdown is requested */
		private var isShutdownRequested = false

		/** Hostname of the server */
		private var strServerHost = ""

		/** Port-number of the server */
		private var serverPort = 0

		/** Your IP */
		private var strMyIP = ""

		/** Your Hostname */
		private var strMyHostname = ""

		/** Server's version */
		private var serverFullVer = ""

		/** Get translated GUI text
		 * @param str String
		 * @return Translated GUI text
		 */
		private fun getUIText(str:String):String = propLang.getProperty(str) ?: propLangDefault.getProperty(str, str) ?: str

		/** Copy the selected row to clipboard
		 * @param table JTable
		 */
		private fun copyTableRowToClipboard(table:JTable) {
			val row = table.selectedRow

			if(row!=-1) {
				val strCopy = StringBuilder()

				for(column in 0 until table.columnCount) {
					val selectedObject = table.getValueAt(row, column)
					if(selectedObject is String)
						if(column==0) strCopy.append(selectedObject)
						else strCopy.append(",").append(selectedObject)
				}

				val ss = StringSelection("$strCopy")
				val clipboard = Toolkit.getDefaultToolkit().systemClipboard
				clipboard.setContents(ss, ss)
			}
		}

		/** Program entry point
		 * @param args Command line options
		 */
		@JvmStatic
		fun main(args:Array<String>) {
			org.apache.logging.log4j.core.config.Configurator.initialize(log.name, "config/etc/log.xml")
			NetAdmin()
		}
	}
}
