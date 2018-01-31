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
package mu.nu.nullpo.tool.sequencer

import mu.nu.nullpo.game.component.Piece
import mu.nu.nullpo.util.CustomProperties
import net.omegaboshi.nullpomino.game.subsystem.randomizer.Randomizer
import org.apache.log4j.Logger
import org.apache.log4j.PropertyConfigurator
import java.awt.*
import java.awt.event.*
import java.io.*
import java.util.*
import javax.swing.*
import javax.swing.filechooser.FileFilter

/** NullpoMino Sequence Viewer
 * (Original from NullpoUE build 010210 by Zircean) */
class Sequencer:JFrame(), ActionListener {

	/** Config File */
	val propConfig:CustomProperties = CustomProperties()

	/** Default language file */
	private val propLangDefault:CustomProperties = CustomProperties()

	/** UI Language File */
	private val propLang:CustomProperties = CustomProperties()

	//----------------------------------------------------------------------
	/** Rand-seed textfield */
	private var txtfldSeed:JTextField? = null

	/** Sequence Length textfield */
	private var txtfldSeqLength:JTextField? = null

	/** Sequence Offset textfield */
	private var txtfldSeqOffset:JTextField? = null

	/** Randomizer combobox */
	private var comboboxRandomizer:JComboBox<*>? = null

	/** Randomizer list */
	private var vectorRandomizer:Vector<String>? = null

	/** Generate button */
	private var btnGenerate:JButton? = null

	/** Generated Sequence textarea */
	private var txtareaSequence:JTextArea? = null

	//----------------------------------------------------------------------
	/** Generated Sequence */
	private var sequence:IntArray = IntArray(0)

	/** Enabled Pieces */
	private var nextPieceEnable:BooleanArray = BooleanArray(Piece.PIECE_COUNT){it<Piece.PIECE_STANDARD_COUNT}

	/** Constructor */
	init {

		// Load config file
		try {
			val `in` = FileInputStream("config/setting/swing.xmk")
			propConfig.load(`in`)
			`in`.close()
		} catch(e:IOException) {
		}

		// Load UI Language file
		try {
			val `in` = FileInputStream("config/lang/sequencer_default.xml")
			propLangDefault.load(`in`)
			`in`.close()
		} catch(e:IOException) {
			log.error("Couldn't load default UI language file", e)
		}

		try {
			val `in` = FileInputStream(
				"config/lang/sequencer_"+Locale.getDefault().country+".xml")
			propLang.load(`in`)
			`in`.close()
		} catch(e:IOException) {
		}

		// Set Look&Feel
		if(propConfig.getProperty("option.usenativelookandfeel", true))
			try {
				UIManager.getInstalledLookAndFeels()
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
			} catch(e:Exception) {
				log.warn("Failed to set native look&feel", e)
			}

		// Initialize enabled pieces
		nextPieceEnable = BooleanArray(Piece.PIECE_COUNT){it<Piece.PIECE_STANDARD_COUNT}

		title = getUIText("Title_Sequencer")
		defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE

		initUI()
		pack()


		isVisible = true
	}

	/** Init GUI */
	private fun initUI() {
		contentPane.layout = BorderLayout()

		// Menubar --------------------------------------------------
		val menuBar = JMenuBar()
		jMenuBar = menuBar

		// File menu
		val mFile = JMenu(getUIText("JMenu_File"))
		mFile.setMnemonic('F')
		menuBar.add(mFile)

		// New
		val miNew = JMenuItem(getUIText("JMenuItem_New"))
		miNew.setMnemonic('N')
		miNew.accelerator = KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK)
		miNew.actionCommand = "New"
		miNew.addActionListener(this)
		//mFile.add(miNew);

		// Open
		val miOpen = JMenuItem(getUIText("JMenuItem_Open"))
		miOpen.setMnemonic('O')
		miOpen.accelerator = KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK)
		miOpen.actionCommand = "Open"
		miOpen.addActionListener(this)
		mFile.add(miOpen)

		// Save
		val miSave = JMenuItem(getUIText("JMenuItem_Save"))
		miSave.setMnemonic('S')
		miSave.accelerator = KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK)
		miSave.actionCommand = "Save"
		miSave.addActionListener(this)
		mFile.add(miSave)

		// Reset
		val miReset = JMenuItem(getUIText("JMenuItem_Reset"))
		miReset.setMnemonic('R')
		miReset.accelerator = KeyStroke.getKeyStroke(KeyEvent.VK_R, InputEvent.CTRL_DOWN_MASK)
		miReset.actionCommand = "Reset"
		miReset.addActionListener(this)
		mFile.add(miReset)

		// Exit
		val miExit = JMenuItem(getUIText("JMenuItem_Exit"))
		miExit.setMnemonic('X')
		miExit.actionCommand = "Exit"
		miExit.addActionListener(this)
		mFile.add(miExit)

		// Options menu
		val mOptions = JMenu(getUIText("JMenu_Options"))
		mOptions.setMnemonic('P')
		menuBar.add(mOptions)

		// Set piece enable
		val miSetPieceEnable = JMenuItem(getUIText("JMenuItem_SetPieceEnable"))
		miSetPieceEnable.setMnemonic('E')
		miSetPieceEnable.actionCommand = "Set piece enable"
		miSetPieceEnable.addActionListener(this)
		mOptions.add(miSetPieceEnable)

		// Set up content pane ------------------------------
		contentPane.layout = BoxLayout(contentPane, BoxLayout.Y_AXIS)

		// Seed
		val pSeed = JPanel()
		contentPane.add(pSeed)

		val lSeed = JLabel(getUIText("Option_Seed"))
		pSeed.add(lSeed)

		txtfldSeed = JTextField("0", 15)
		pSeed.add(txtfldSeed)

		// Sequence Length
		val pSeqLength = JPanel()
		contentPane.add(pSeqLength)

		val lSeqLength = JLabel(getUIText("Option_SequenceLength"))
		pSeqLength.add(lSeqLength)

		txtfldSeqLength = JTextField("100", 6)
		pSeqLength.add(txtfldSeqLength)

		// Sequence Offset
		val pSeqOffset = JPanel()
		contentPane.add(pSeqOffset)

		val lSeqOffset = JLabel(getUIText("Option_SequenceOffset"))
		pSeqOffset.add(lSeqOffset)

		txtfldSeqOffset = JTextField("0", 6)
		pSeqOffset.add(txtfldSeqOffset)

		// Randomizer
		val pRandomizer = JPanel()
		contentPane.add(pRandomizer)

		val lRandomizer = JLabel(getUIText("Option_Randomizer"))
		pRandomizer.add(lRandomizer)

		vectorRandomizer = getTextFileVector("config/list/randomizer.lst")
		comboboxRandomizer = JComboBox(createShortStringVector(vectorRandomizer))
		comboboxRandomizer!!.preferredSize = Dimension(200, 30)
		comboboxRandomizer!!.selectedIndex = 0
		pRandomizer.add(comboboxRandomizer)

		// Generate
		val pGenerate = JPanel()
		contentPane.add(pGenerate)

		btnGenerate = JButton(getUIText("Option_Generate"))
		btnGenerate!!.setMnemonic('G')
		btnGenerate!!.actionCommand = "Generate"
		btnGenerate!!.addActionListener(this)
		pGenerate.add(btnGenerate)

		// Sequence
		txtareaSequence = JTextArea(10, 37)
		txtareaSequence!!.lineWrap = true
		txtareaSequence!!.isEditable = false

		val pSequence = JScrollPane(txtareaSequence, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER)
		contentPane.add(pSequence)

	}

	fun getTextFileVector(filename:String):Vector<String> {
		val vec = Vector<String>()

		try {
			val `in` = BufferedReader(FileReader(filename))

			while(true) {
				val str = `in`.readLine()
				if(str==null||str.isEmpty()) break
				vec.add(str)
			}
			`in`.close()
		} catch(e:IOException) {
		}

		return vec
	}

	fun createShortStringVector(vecSrc:Vector<String>?):Vector<String> {
		val vec = Vector<String>()

		for(aVecSrc in vecSrc!!) vec.add(createShortString(aVecSrc))

		return vec
	}

	fun createShortString(str:String):String {
		val last = str.lastIndexOf('.')

		val newStr:String
		newStr = if(last!=-1)
			str.substring(last+1)
		else
			str
		return newStr
	}

	fun readReplayToUI(prop:CustomProperties, playerID:Int) {
		txtfldSeed!!.text = java.lang.Long.parseLong(prop.getProperty(playerID.toString()+".replay.randSeed", "0"), 16).toString()
		comboboxRandomizer!!.selectedItem = createShortString(prop.getProperty(playerID.toString()+".ruleopt.strRandomizer", null))
	}

	@Throws(IOException::class)
	fun load(filename:String):CustomProperties {
		log.info("Loading replay file from $filename")
		val prop = CustomProperties()

		val `in` = FileInputStream(filename)
		prop.load(`in`)
		`in`.close()

		return prop
	}

	@Throws(IOException::class)
	fun save(filename:String) {
		log.info("Saving piece sequence file to $filename")
		val out = BufferedWriter(FileWriter(filename))
		out.write("# NullpoMino Piece Sequence")
		out.newLine()
		out.write(txtareaSequence!!.text)
		out.close()
	}

	/** Get translated text from UI Language file
	 * @param str Text
	 * @return Translated text (If translated text is NOT available, it will
	 * return str itself)
	 */
	fun getUIText(str:String):String? {
		var result:String? = propLang.getProperty(str)
		if(result==null) result = propLangDefault.getProperty(str, str)
		return result
	}

	/** Get int value from a JTextField
	 * @param txtfld JTextField
	 * @return An int value from JTextField (If fails, it will return zero)
	 */
	fun getIntTextField(txtfld:JTextField?):Int {
		var v = 0

		try {
			v = Integer.parseInt(txtfld!!.text)
		} catch(e:Exception) {
		}

		return v
	}

	/** Get long value from a JTextField
	 * @param txtfld JTextField
	 * @return A long value from JTextField (If fails, it will return zero)
	 */
	fun getLongTextField(txtfld:JTextField?):Long {
		var v = 0L

		try {
			v = java.lang.Long.parseLong(txtfld!!.text)
		} catch(e:Exception) {
		}

		return v
	}

	fun generate() {
		val randomizerClass:Class<*>
		val randomizerObject:Randomizer

		val name = vectorRandomizer!![comboboxRandomizer!!.selectedIndex]

		try {
			randomizerClass = Class.forName(name)
			randomizerObject = randomizerClass.newInstance() as Randomizer
			randomizerObject.setState(nextPieceEnable, getLongTextField(txtfldSeed))
			sequence = IntArray(getIntTextField(txtfldSeqLength)){
				for(i in 0 until getIntTextField(txtfldSeqOffset))
					randomizerObject.next()
				return@IntArray randomizerObject.next()
			}

		} catch(e:Exception) {
			log.error("Randomizer class $name load failed", e)
		}

	}

	fun display() {
		if(txtareaSequence!!.text!="") txtareaSequence!!.text = ""
		for(i in 1..sequence!!.size) {
			txtareaSequence!!.append(getUIText("PieceName"+sequence!![i-1]))
			if(i%5==0) txtareaSequence!!.append(" ")
			if(i%60==0) txtareaSequence!!.append("\n")
		}
	}

	fun reset() {
		txtfldSeed!!.text = "0"
		txtfldSeqLength!!.text = "100"
		txtfldSeqOffset!!.text = "0"
		comboboxRandomizer!!.selectedIndex = 0
		txtareaSequence!!.text = ""
		sequence = IntArray(0)
	}

	override fun actionPerformed(e:ActionEvent) {
		if(e.actionCommand=="New") {
			// New
		} else if(e.actionCommand=="Open") {
			// Open
			val c = JFileChooser(System.getProperty("user.dir")+"/replay")
			c.fileFilter = FileFilterREP()

			if(c.showOpenDialog(this)==JFileChooser.APPROVE_OPTION) {
				val file = c.selectedFile
				var prop = CustomProperties()

				try {
					prop = load(file.path)
				} catch(e2:IOException) {
					log.error("Failed to load replay data", e2)
					JOptionPane.showMessageDialog(this, getUIText("Message_FileLoadFailed")+"\n"
						+e2, getUIText("Title_FileLoadFailed"), JOptionPane.ERROR_MESSAGE)
					return
				}

				readReplayToUI(prop, 0)
			}
		} else if(e.actionCommand=="Save") {
			// Save
			generate()
			display()
			val c = JFileChooser(System.getProperty("user.dir"))
			c.fileFilter = FileFilterTXT()

			if(c.showSaveDialog(this)==JFileChooser.APPROVE_OPTION) {
				val file = c.selectedFile
				var filename = file.path
				if(!filename.endsWith(".txt")) filename = "$filename.txt"

				try {
					save(filename)
				} catch(e2:Exception) {
					log.error("Failed to save sequence data", e2)
					JOptionPane.showMessageDialog(this, getUIText("Message_FileSaveFailed")+"\n"
						+e2, getUIText("Title_FileSaveFailed"), JOptionPane.ERROR_MESSAGE)
					return
				}

			}
		} else if(e.actionCommand=="Reset")
		// Reset
			reset()
		else if(e.actionCommand=="Set piece enable")
		// Set piece enable
			setPieceEnable()
		else if(e.actionCommand=="Generate") {
			// Generate
			generate()
			display()
		} else if(e.actionCommand=="Exit")
		// Exit
			dispose()
	}

	fun setPieceEnable() {
		val setPieceEnableFrame = JFrame(getUIText("Title_SetPieceEnable"))
		setPieceEnableFrame.contentPane.layout = GridLayout(0, 2, 10, 10)
		val chkboxEnable = arrayOfNulls<JCheckBox>(Piece.PIECE_COUNT)
		for(i in 0 until Piece.PIECE_COUNT) {
			chkboxEnable[i] = JCheckBox("Piece "+getUIText("PieceName$i")!!).apply {
				isSelected = nextPieceEnable[i]
			}
			setPieceEnableFrame.contentPane.add(chkboxEnable[i])
		}
		//if(Piece.PIECE_COUNT%2==0) setPieceEnableFrame.getContentPane().add(new JLabel(""));
		val btnConfirm = JButton(getUIText("Button_Confirm"))
		btnConfirm.addActionListener {e ->
			for(i in 0 until Piece.PIECE_COUNT)
				nextPieceEnable[i] = chkboxEnable[i]?.isSelected?:false
			setPieceEnableFrame.dispose()
		}
		setPieceEnableFrame.contentPane.add(btnConfirm)
		setPieceEnableFrame.pack()
		setPieceEnableFrame.isVisible = true
	}

	protected inner class FileFilterREP:FileFilter() {
		override fun accept(f:File):Boolean = if(f.isDirectory) true else f.name.endsWith(".rep")

		override fun getDescription():String? = getUIText("FileChooser_ReplayFile")
	}

	protected inner class FileFilterTXT:FileFilter() {
		override fun accept(f:File):Boolean = if(f.isDirectory) true else f.name.endsWith(".txt")

		override fun getDescription():String? = getUIText("FileChooser_TextFile")
	}

	companion object {
		/** Serial Version UID */
		private const val serialVersionUID = 1L

		/** Log */
		internal val log = Logger.getLogger(Sequencer::class.java)

		@JvmStatic
		fun main(args:Array<String>) {
			PropertyConfigurator.configure("config/etc/log.cfg")
			log.debug("Sequencer start")
			Sequencer()
		}
	}
}
