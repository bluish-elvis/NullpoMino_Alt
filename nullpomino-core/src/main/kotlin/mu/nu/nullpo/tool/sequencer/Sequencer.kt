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
package mu.nu.nullpo.tool.sequencer

import mu.nu.nullpo.game.component.Piece
import mu.nu.nullpo.util.CustomProperties
import net.omegaboshi.nullpomino.game.subsystem.randomizer.Randomizer
import org.apache.logging.log4j.LogManager
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.GridLayout
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import java.io.BufferedWriter
import java.io.File
import java.io.FileInputStream
import java.io.FileReader
import java.io.FileWriter
import java.io.IOException
import java.util.Locale
import java.util.Vector
import java.util.zip.GZIPInputStream
import javax.swing.*
import javax.swing.filechooser.FileFilter

/** NullpoMino Sequence Viewer
 * (Original from NullpoUE build 010210 by Zircean) */
class Sequencer:JFrame(), ActionListener {
	/** Config File */
	val propConfig = CustomProperties()

	/** Default language file */
	private val propLangDefault = CustomProperties()

	/** UI Language File */
	private val propLang = CustomProperties()

	//----------------------------------------------------------------------
	/** Rand-seed textfield */
	private val txtfldSeed:JTextField = JTextField()

	/** Sequence Length textfield */
	private val txtfldSeqLength:JTextField = JTextField()

	/** Sequence Section Size textfield */
	private val txtfldSeqSize:JTextField = JTextField()

	/** Sequence Offset textfield */
	private val txtfldSeqOffset:JTextField = JTextField()

	/** Randomizer combobox */
	private var comboboxRandomizer:JComboBox<*>? = null

	/** Randomizer list */
	private var vectorRandomizer:Vector<String>? = null

	/** Generate button */
	private val btnGenerate:JButton = JButton()

	/** Generated Sequence textarea */
	private val txtareaSequence:JTextArea = JTextArea()

	//----------------------------------------------------------------------
	/** Generated Sequence */
	private var sequence = IntArray(0)

	/** Enabled Pieces */
	private var nextPieceEnable = MutableList(Piece.PIECE_COUNT) {it<Piece.PIECE_STANDARD_COUNT}

	/** Constructor */
	init {
		// Load config file
		try {
			FileInputStream("config/setting/swing.properties").let {
				propConfig.load(it)
				it.close()
			}
		} catch(_:IOException) {
		}

		// Load UI Language file
		try {
			FileInputStream("config/lang/sequencer_default.xml").let {
				propLangDefault.loadFromXML(it)
				it.close()
			}
		} catch(e:IOException) {
			log.error("Couldn't load default UI language file", e)
		}

		try {
			FileInputStream("config/lang/sequencer_${Locale.getDefault().country}.xml").let {
				propLang.loadFromXML(it)
				it.close()
			}
		} catch(_:IOException) {
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
		nextPieceEnable = MutableList(Piece.PIECE_COUNT) {it<Piece.PIECE_STANDARD_COUNT}

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
		val mFile = JMenu(getUIText("JMenu_File")).apply {
			setMnemonic('F')
			menuBar.add(this)
		}

		// New
		JMenuItem(getUIText("JMenuItem_New")).also {
			it.setMnemonic('N')
			it.accelerator = KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK)
			it.actionCommand = "New"
			it.addActionListener(this)
			//mFile.add(it);
		}

		// Open
		JMenuItem(getUIText("JMenuItem_Open")).also {
			it.setMnemonic('O')
			it.accelerator = KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK)
			it.actionCommand = "Open"
			it.addActionListener(this)
			mFile.add(it)
		}

		// Save
		JMenuItem(getUIText("JMenuItem_Save")).also {
			it.setMnemonic('S')
			it.accelerator = KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK)
			it.actionCommand = "Save"
			it.addActionListener(this)
			mFile.add(it)
		}

		// Reset
		JMenuItem(getUIText("JMenuItem_Reset")).also {
			it.setMnemonic('R')
			it.accelerator = KeyStroke.getKeyStroke(KeyEvent.VK_R, InputEvent.CTRL_DOWN_MASK)
			it.actionCommand = "Reset"
			it.addActionListener(this)
			mFile.add(it)
		}

		// Exit
		JMenuItem(getUIText("JMenuItem_Exit")).also {
			it.setMnemonic('X')
			it.actionCommand = "Exit"
			it.addActionListener(this)
			mFile.add(it)
		}

		// Options menu
		val mOptions = JMenu(getUIText("JMenu_Options")).apply {
			setMnemonic('P')
			menuBar.add(this)
		}

		// Set piece enable
		JMenuItem(getUIText("JMenuItem_SetPieceEnable")).also {
			it.setMnemonic('E')
			it.actionCommand = "Set piece enable"
			it.addActionListener(this)
			mOptions.add(it)
		}

		// Set up content pane ------------------------------
		contentPane.layout = BoxLayout(contentPane, BoxLayout.Y_AXIS)

		// Seed
		JPanel().apply {
			contentPane.add(this)
			add(JLabel(getUIText("Option_Seed")))
			add(txtfldSeed.apply {
				text = "0"
				columns = 15
			})
		}

		// Sequence Length
		JPanel().apply {
			contentPane.add(this)
			add(JLabel(getUIText("Option_SequenceLength")))
			add(txtfldSeqLength.apply {
				text = "100"
				columns = 6
			})
		}
		// Sequence Size
		JPanel().apply {
			contentPane.add(this)
			add(JLabel(getUIText("Option_SequenceSize")))
			add(txtfldSeqSize.apply {
				text = "7"
				columns = 3
			})
		}
		// Sequence Offset
		JPanel().apply {
			contentPane.add(this)
			add(JLabel(getUIText("Option_SequenceOffset")))
			add(txtfldSeqOffset.apply {
				text = "0"
				columns = 6
			})
		}

		// Randomizer
		JPanel().apply {
			contentPane.add(this)
			add(JLabel(getUIText("Option_Randomizer")))
			vectorRandomizer = this::class.java.getResource("../randomizer.lst")?.path?.let {getTextFileVector(it)}
			comboboxRandomizer = JComboBox(createShortStringVector(vectorRandomizer)).apply {
				preferredSize = Dimension(222, 30)
				selectedIndex = 0
			}
			add(comboboxRandomizer)
		}
		// Generate
		JPanel().apply {
			contentPane.add(this)
			add(btnGenerate.also {
				it.text = getUIText("Option_Generate")
				it.setMnemonic('G')
				it.actionCommand = "Generate"
				it.addActionListener(this@Sequencer)
			})
		}

		// Sequence
		contentPane.add(
			JScrollPane(
				txtareaSequence.apply {
					rows = 10
					columns = 37
					lineWrap = true
					isEditable = false
				}, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
				ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
			)
		)
	}

	private fun getTextFileVector(filename:String):Vector<String> {
		val vec = Vector<String>()

		try {
			FileReader(filename).buffered().use {
				while(true) {
					val str = it.readLine()
					if(str==null||str.isEmpty()) break
					vec.add(str)
				}
			}
		} catch(_:IOException) {
		}

		return vec
	}

	private fun createShortStringVector(vecSrc:Vector<String>?):Vector<String> {
		val vec = Vector<String>()

		vecSrc?.forEach {aVecSrc ->
			vec.add(createShortString(aVecSrc))
		}

		return vec
	}

	private fun createShortString(str:String):String {
		val last = str.lastIndexOf('.')

		return if(last!=-1) str.substring(last+1) else str
	}

	private fun readReplayToUI(prop:CustomProperties, playerID:Int) {
		txtfldSeed.text = prop.getProperty("$playerID.replay.randSeed", 16L).toString()
		comboboxRandomizer?.selectedItem = createShortString(prop.getProperty("$playerID.ruleOpt.strRandomizer", null))
	}

	@Throws(IOException::class)
	fun load(filename:String):CustomProperties {
		log.info("Loading replay file from $filename")
		val prop = CustomProperties()

		val `in` = GZIPInputStream(FileInputStream(filename))
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
		out.write(txtareaSequence.text)
		out.close()
	}

	/** Get translated text from UI Language file
	 * @param str Text
	 * @return Translated text (If translated text is NOT available, it will return str itself)
	 */
	private fun getUIText(str:String):String = propLang.getProperty(str, propLangDefault.getProperty(str, str)) ?: ""

	/** Get int value from a JTextField
	 * @param txtfld JTextField
	 * @return An int value from JTextField (If fails, it will return zero)
	 */
	private fun getIntTextField(txtfld:JTextField?):Int = txtfld?.text?.toIntOrNull() ?: 0

	/** Get long value from a JTextField
	 * @param txtfld JTextField
	 * @return A long value from JTextField (If fails, it will return zero)
	 */
	private fun getLongTextField(txtfld:JTextField?):Long = txtfld?.text?.toLongOrNull() ?: 0L

	private fun generate() {
		val randomizerClass:Class<*>
		val randomizerObject:Randomizer

		val name = vectorRandomizer!![comboboxRandomizer!!.selectedIndex]

		try {
			randomizerClass = Class.forName(name)
			randomizerObject = randomizerClass.getDeclaredConstructor().newInstance() as Randomizer
			randomizerObject.setState(nextPieceEnable, getLongTextField(txtfldSeed))
			sequence = IntArray(getIntTextField(txtfldSeqLength)) {
				for(i in 0..<getIntTextField(txtfldSeqOffset))
					randomizerObject.next()
				return@IntArray randomizerObject.next()
			}
		} catch(e:Exception) {
			log.error("Randomizer class $name load failed", e)
		}
	}

	fun display() {
		if(txtareaSequence.text!="") txtareaSequence.text = ""
		val ct = getIntTextField(txtfldSeqSize)
		for(i in 1..sequence.size) {
			txtareaSequence.append(getUIText("PieceName${sequence[i-1]}"))
			if(i%(ct*5)==0) txtareaSequence.append("\n")
			else if(i%ct==0) txtareaSequence.append(" ")
		}
	}

	fun reset() {
		txtfldSeed.text = "0"
		txtfldSeqLength.text = "100"
		txtfldSeqSize.text = "7"
		txtfldSeqOffset.text = "0"
		comboboxRandomizer?.selectedIndex = 0
		txtareaSequence.text = ""
		sequence = IntArray(0)
	}

	override fun actionPerformed(e:ActionEvent) {
		if(e.actionCommand=="New") {
			// New
		} else if(e.actionCommand=="Open") {
			// Open
			val c = JFileChooser("${System.getProperty("user.dir")}/replay")
			c.fileFilter = FileFilterREP()

			if(c.showOpenDialog(this)==JFileChooser.APPROVE_OPTION) {
				val file = c.selectedFile
				val prop:CustomProperties

				try {
					prop = load(file.path)
				} catch(e2:IOException) {
					log.error("Failed to load replay data", e2)
					JOptionPane.showMessageDialog(
						this, "${getUIText("Message_FileLoadFailed")}\n$e2", getUIText("Title_FileLoadFailed"),
						JOptionPane.ERROR_MESSAGE
					)
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
					JOptionPane.showMessageDialog(
						this, "${getUIText("Message_FileSaveFailed")}\n$e2",
						getUIText("Title_FileSaveFailed"), JOptionPane.ERROR_MESSAGE
					)
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

	private fun setPieceEnable() {
		val setPieceEnableFrame = JFrame(getUIText("Title_SetPieceEnable"))
		setPieceEnableFrame.contentPane.layout = GridLayout(0, 2, 10, 10)
		val chkboxEnable = arrayOfNulls<JCheckBox>(Piece.PIECE_COUNT)
		for(i in 0..<Piece.PIECE_COUNT) {
			chkboxEnable[i] = JCheckBox("Piece ${getUIText("PieceName$i")}").apply {
				isSelected = nextPieceEnable[i]
			}
			setPieceEnableFrame.contentPane.add(chkboxEnable[i])
		}
		//if(Piece.PIECE_COUNT%2==0) setPieceEnableFrame.getContentPane().add(new JLabel(""));
		val btnConfirm = JButton(getUIText("Button_Confirm"))
		btnConfirm.addActionListener {
			for(i in 0..<Piece.PIECE_COUNT)
				nextPieceEnable[i] = chkboxEnable[i]?.isSelected ?: false
			setPieceEnableFrame.dispose()
		}
		setPieceEnableFrame.contentPane.add(btnConfirm)
		setPieceEnableFrame.pack()
		setPieceEnableFrame.isVisible = true
	}

	private inner class FileFilterREP:FileFilter() {
		override fun accept(f:File):Boolean = if(f.isDirectory) true else f.name.endsWith(".rep")

		override fun getDescription():String = getUIText("FileChooser_ReplayFile")
	}

	private inner class FileFilterTXT:FileFilter() {
		override fun accept(f:File):Boolean = if(f.isDirectory) true else f.name.endsWith(".txt")

		override fun getDescription():String = getUIText("FileChooser_TextFile")
	}

	companion object {
		/** Log */
		internal val log = LogManager.getLogger()

		@JvmStatic
		fun main(args:Array<String>) {
			org.apache.logging.log4j.core.config.Configurator.initialize(log.name, "config/etc/log.xml")
			log.debug("Sequencer start")
			Sequencer()
		}
	}
}
