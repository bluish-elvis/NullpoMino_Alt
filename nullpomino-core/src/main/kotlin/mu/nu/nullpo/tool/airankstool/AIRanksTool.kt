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

package mu.nu.nullpo.tool.airankstool

import mu.nu.nullpo.tool.sequencer.Sequencer
import mu.nu.nullpo.util.CustomProperties
import org.apache.logging.log4j.LogManager
import org.jdesktop.layout.GroupLayout
import java.awt.BorderLayout
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.ObjectInputStream
import java.util.Locale
import javax.swing.*

class AIRanksTool:JFrame(), ActionListener {
	/** Config File */
	val propConfig = CustomProperties()

	/** Default language file */
	private val propLangDefault = CustomProperties()
	/** Primary language file */
	private val propLang = CustomProperties()

	/** Get translated text from UI Language file
	 * @param str Text
	 * @return Translated text (If translated text is NOT available, it will return str itself)
	 */
	private fun getUIText(str:String):String = propLang.getProperty(str, propLangDefault.getProperty(str, str)) ?: ""

	/** UI */

	//****************************
	//Tab 1 (Generation) variables
	//****************************

	// Input File
	private var inputFileLabel:JLabel? = null
	private var inputFileComboBox:JComboBox<String>? = null

	// Output File
	private var outputFileLabel:JLabel? = null
	private var outputFileField:JTextField? = null

	//Number of Iterations
	private var numIterationsLabel:JLabel? = null
	private var numIterationsSpinner:JSpinner? = null

	// Generation Button
	private var goButton:JButton? = null

	// View the best surfaces button
	private var viewBestsButton:JButton? = null

	// View the worst surfaces button
	private var viewWorstsButton:JButton? = null

	//***************************
	//Tab 2 (AI Config) variables
	//***************************

	//Ranks File Used
	private var ranksFileUsedLabel:JLabel? = null
	private var ranksFileUsedComboBox:JComboBox<String>? = null

	//Number of previews Used
	private var numPreviewsLabel:JLabel? = null
	private var numPreviewsSpinner:JSpinner? = null

	//Allow Hold or not
	private var allowHoldLabel:JLabel? = null
	private var allowHoldCheckBox:JCheckBox? = null

	//Speed Limit
	private var speedLimitLabel:JLabel? = null
	private var speedLimitField:JFormattedTextField? = null

	// Save AI Config Button
	private var saveAIConfigButton:JButton? = null
	private var tabbedPane:JTabbedPane? = null

	//***************************
	//Tab 3 (Ranks Info) variables
	//***************************

	//Ranks File To get info from
	//private JLabel ranksFileInfoLabel;
	//private JComboBox ranksFileInfoComboBox;

	//*****************
	//Default variables
	//*****************

	private var newFileText = ""

	init {
		// Load config file
		propConfig.loadXML("config/setting/swing.xml")

		// Set Look&Feel
		if(propConfig.getProperty("option.usenativelookandfeel", true))
			try {
				UIManager.getInstalledLookAndFeels()
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
			} catch(e:Exception) {
				Sequencer.log.warn("Failed to set native look&feel", e)
			}

		// Load language files
		if(propLangDefault.loadXML("config/lang/airankstool_default.xml")==null)
			System.err.println("Couldn't load default UI language file")
		if(propLang.loadXML("config/lang/airankstool_${Locale.getDefault().country}.xml")==null)
			System.err.println("Couldn't load ${Locale.getDefault().country} UI language file")


		title = getUIText("Main_Title")
		defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
		initUI()
		pack()

		isVisible = true
	}

	private fun initUI() {
		// Loads Ranks AI property file, to populate the fields
		val propRanksAI = CustomProperties()
		try {
			val `in` = FileInputStream(AIRanksConstants.RANKSAI_CONFIG_FILE)
			propRanksAI.load(`in`)
			`in`.close()
		} catch(e:IOException) {
			System.err.println("load failed: ${AIRanksConstants.RANKSAI_CONFIG_FILE}")
		}

		//Ranks File used
		val ranksFile = propRanksAI.getProperty("ranksai.file")

		//Number of previews used
		val numPreviews = propRanksAI.getProperty("ranksai.numpreviews", 2)

		//Allow Hold ?
		val allowHold = propRanksAI.getProperty("ranksai.allowhold", false)

		//Speed Limit
		val speedLimit = propRanksAI.getProperty("ranksai.speedlimit", 0)

		// Loads the ranks file list from the ranksAI directory (/res/ranksai)
		val children = File(AIRanksConstants.RANKSAI_DIR).list()

		var fileIndex = -1

		//Find the index of default Ranks File
		if(children!=null) {
			if(ranksFile!=null) {
				fileIndex = -1
				for(i in children.indices) {
					if(children[i]==ranksFile) {
						fileIndex = i
					}
				}
			}
			val ranksList:Array<String> = Array(children.size+1) {""}
			System.arraycopy(children, 0, ranksList, 1, children.size)
		}

		//Tab 1 (Generation)

		// Input File Label
		inputFileLabel = JLabel(getUIText("Main_Input_Label"))

		// Add the New File entry to combobox list
		val ranksList:Array<String>
		if(children!=null) {
			ranksList = Array(children.size+1) {""}
			System.arraycopy(children, 0, ranksList, 1, children.size)
		} else {
			ranksList = Array(1) {""}
		}
		newFileText = getUIText("Main_New_File_Text")
		ranksList[0] = newFileText

		// Creates the combo box
		inputFileComboBox = JComboBox(ranksList).also {
			it.selectedIndex = fileIndex+1
			it.toolTipText = getUIText("Main_Input_Tip")
			it.actionCommand = "input"
			it.addActionListener(this)
		}

		// Output File
		outputFileLabel = JLabel(getUIText("Main_Output_Label"))
		outputFileField = JTextField(AIRanksConstants.DEFAULT_RANKS_FILE).apply {
			columns = 20
			if((inputFileComboBox?.selectedIndex ?: 0)>0) {
				text = inputFileComboBox!!.selectedItem as String
			}
			toolTipText = getUIText("Main_Output_Tip")
		}

		//Number of iterations to run
		numIterationsLabel = JLabel(getUIText("Main_Iterations_Label"))
		numIterationsSpinner = JSpinner(SpinnerNumberModel(1, 1, Integer.MAX_VALUE, 1)).apply {
			toolTipText = getUIText("Main_Iterations_Tip")
		}

		//Go button (starts the generation)
		goButton = JButton(getUIText("Main_Go_Label")).also {
			it.actionCommand = "go"
			it.addActionListener(this)
			it.toolTipText = getUIText("Main_Go_Tip")
			it.setMnemonic('G')
		}

		// View Bests Button
		viewBestsButton = JButton(getUIText("Main_Bests_Label")).also {
			it.actionCommand = "bests"
			it.addActionListener(this)
			it.toolTipText = getUIText("Main_Bests_Tip")
			it.setMnemonic('B')
		}

		// View worsts Button
		viewWorstsButton = JButton(getUIText("Main_Worsts_Label")).also {
			it.actionCommand = "worsts"
			it.addActionListener(this)
			it.toolTipText = getUIText("Main_Worsts_Tip")
			it.setMnemonic('W')
		}

		//*******************************************************************

		//Tab 2

		//Ranks File Used
		ranksFileUsedLabel = JLabel(getUIText("Main_Ranks_File_Used_Label"))


		ranksFileUsedComboBox = JComboBox(children ?: arrayOf(" ")).also {
			it.selectedIndex = maxOf(0, fileIndex)
			it.toolTipText = getUIText("Main_Ranks_File_Used_Tooltip")
			it.actionCommand = "input2"
			it.addActionListener(this)
		}
		//Number of previews to use
		numPreviewsLabel = JLabel(getUIText("Main_Num_Previews_Label"))
		numPreviewsSpinner = JSpinner(SpinnerNumberModel(2, 0, Integer.MAX_VALUE, 1)).apply {
			value = maxOf(0, numPreviews)
			toolTipText = getUIText("Main_Num_Previews_Tip")
		}

		//Switch to allow hold
		allowHoldLabel = JLabel(getUIText("Main_Allow_Hold"))
		allowHoldCheckBox = JCheckBox().apply {
			isSelected = allowHold
			toolTipText = getUIText("Main_Allow_Hold_Tip")
		}

		//Speed Limit
		speedLimitLabel = JLabel(getUIText("Main_Speed_Limit_Label"))
		speedLimitField = JFormattedTextField(speedLimit).apply {
			toolTipText = getUIText("Main_Speed_Limit_Tip")
		}

		// Save config Button
		saveAIConfigButton = JButton(getUIText("Main_Set_Default_Label")).also {
			it.actionCommand = "default"
			it.addActionListener(this)
			it.toolTipText = getUIText("Main_Set_Default_Tip")
			it.setMnemonic('S')
		}

		//*************************************************************************
		// Generates the panels

		// Tab 1
		val formPane = JPanel()
		val layout = GroupLayout(formPane)
		formPane.layout = layout
		layout.autocreateGaps = true
		layout.autocreateContainerGaps = true
		layout.horizontalGroup = layout.createSequentialGroup().also {hGroup ->

			hGroup.add(layout.createParallelGroup().also {labelsPg ->
				labelsPg.add(inputFileLabel)
				labelsPg.add(outputFileLabel)
				labelsPg.add(numIterationsLabel)
			})

			hGroup.add(layout.createParallelGroup().also {fieldsPg ->
				fieldsPg.add(inputFileComboBox)
				fieldsPg.add(outputFileField)
				fieldsPg.add(numIterationsSpinner)
			})
		}

		layout.verticalGroup = layout.createSequentialGroup().also {vGroup ->
			vGroup.add(layout.createParallelGroup(GroupLayout.BASELINE).add(inputFileLabel).add(inputFileComboBox))
			vGroup.add(layout.createParallelGroup(GroupLayout.BASELINE).add(outputFileLabel).add(outputFileField))
			vGroup.add(layout.createParallelGroup(GroupLayout.BASELINE).add(numIterationsLabel).add(numIterationsSpinner))
		}
		val buttonsPane = JPanel().apply {
			add(goButton)
			add(viewBestsButton)
			add(viewWorstsButton)
		}
		val pane1 = JPanel(BorderLayout()).apply {
			border = BorderFactory.createEmptyBorder(20, 20, 20, 20)
			add(formPane, BorderLayout.CENTER)
			add(buttonsPane, BorderLayout.SOUTH)
		}

		// Tab 2

		val formPane2 = JPanel()
		val layout2 = GroupLayout(formPane2)
		formPane2.layout = layout2
		layout2.autocreateGaps = true
		layout2.autocreateContainerGaps = true

		layout2.horizontalGroup = layout2.createSequentialGroup().also {
			it.add(layout2.createParallelGroup().apply {
				add(ranksFileUsedLabel)
				add(numPreviewsLabel)
				add(allowHoldLabel)
				add(speedLimitLabel)
			})
			it.add(layout2.createParallelGroup().apply {
				add(ranksFileUsedComboBox)
				add(numPreviewsSpinner)
				add(allowHoldCheckBox)
				add(speedLimitField)
			})
		}

		layout2.verticalGroup = layout2.createSequentialGroup().apply {
			add(layout2.createParallelGroup(GroupLayout.BASELINE).add(ranksFileUsedLabel).add(ranksFileUsedComboBox))
			add(layout2.createParallelGroup(GroupLayout.BASELINE).add(numPreviewsLabel).add(numPreviewsSpinner))
			add(layout2.createParallelGroup(GroupLayout.BASELINE).add(allowHoldLabel).add(allowHoldCheckBox))
			add(layout2.createParallelGroup(GroupLayout.BASELINE).add(speedLimitLabel).add(speedLimitField))
		}

		val buttonsPane2 = JPanel().apply {
			add(saveAIConfigButton)
		}
		val pane2 = JPanel(BorderLayout()).apply {
			border = BorderFactory.createEmptyBorder(20, 20, 20, 20)
			add(formPane2, BorderLayout.CENTER)
			add(buttonsPane2, BorderLayout.SOUTH)
		}
		//Tab 3

		tabbedPane = JTabbedPane().apply {
			addTab(getUIText("Main_Generation_Tab_Title"), pane1)
			addTab(getUIText("Main_AI_Config_Tab_Title"), pane2)
		}
		add(tabbedPane)
	}

	override fun actionPerformed(e:ActionEvent) {
		var inputFile:String = inputFileComboBox!!.selectedItem as String
		if(inputFile==newFileText) {
			inputFile = ""
		}
		if("go"==e.actionCommand) {
			val outputFile = outputFileField!!.text
			goButton!!.isEnabled = false

			val ranksIterator = RanksIterator(this, inputFile, outputFile, numIterationsSpinner!!.value as Int) {getUIText(it)}

			ranksIterator.addWindowListener(object:WindowAdapter() {
				override fun windowClosed(e:WindowEvent?) {
					var isInCombo = false
					var index = 0
					for(i in 1..<inputFileComboBox!!.itemCount) {
						if(outputFile==inputFileComboBox!!.getItemAt(i)) {
							isInCombo = true
							index = i
							break
						}
					}
					if(!isInCombo) {
						inputFileComboBox!!.addItem(outputFile)
						ranksFileUsedComboBox!!.run {
							addItem(outputFile)
							selectedIndex = ranksFileUsedComboBox!!.itemCount-1
						}
					} else {
						ranksFileUsedComboBox!!.setSelectedIndex(index)
					}

					setDefaults()
					goButton!!.isEnabled = true
				}
			})
		} else {
			if("bests"==e.actionCommand||"worsts"==e.actionCommand) {
				setEnabledBWButtons(false)
				var ranks:Ranks? = null

				val fis:FileInputStream
				val `in`:ObjectInputStream

				if(inputFile.trim {it<=' '}.isEmpty())
					ranks = Ranks(4, 9)
				else {
					try {
						fis = FileInputStream(AIRanksConstants.RANKSAI_DIR+inputFile)
						`in` = ObjectInputStream(fis)
						ranks = `in`.readObject() as Ranks
						`in`.close()
					} catch(e1:FileNotFoundException) {
						ranks = Ranks(4, 9)
					} catch(e1:IOException) {
						// TODO Auto-generated catch block
						e1.printStackTrace()
					} catch(e1:ClassNotFoundException) {
						e1.printStackTrace()
					}
				}

				val results = RanksResult(this, ranks!!, 100, "worsts"==e.actionCommand) {getUIText(it)}
				results.addWindowListener(object:WindowAdapter() {
					override fun windowClosed(e:WindowEvent?) {
						setEnabledBWButtons(true)
					}
				})
			} else {
				if("default"==e.actionCommand) {
					setDefaults()
				} else {
					if("input"==e.actionCommand) {
						if(inputFileComboBox!!.selectedIndex>0) {
							outputFileField!!.text = inputFileComboBox!!.selectedItem as String
						} else {
							outputFileField!!.text = AIRanksConstants.DEFAULT_RANKS_FILE
						}
					}
				}
			}
		}
	}

	fun setDefaults() {
		val ranksAIConfig = CustomProperties()
		ranksAIConfig.setProperty("ranksai.file", ranksFileUsedComboBox!!.selectedItem as String)
		ranksAIConfig.setProperty("ranksai.numpreviews", numPreviewsSpinner!!.value as Int)
		ranksAIConfig.setProperty("ranksai.allowhold", allowHoldCheckBox!!.isSelected)
		ranksAIConfig.setProperty("ranksai.speedlimit", speedLimitField!!.value as Int)
		try {
			val out = FileOutputStream(AIRanksConstants.RANKSAI_CONFIG_FILE)
			ranksAIConfig.store(out, "Ranks AI Config")
		} catch(exc:IOException) {
			log.error("Failed to save RanksAI config file", exc)
		}
	}

	fun setEnabledBWButtons(b:Boolean) {
		viewBestsButton?.isEnabled = b
		viewWorstsButton?.isEnabled = b
	}

	companion object {
		/** Log */
		internal val log = LogManager.getLogger()

		@JvmStatic
		fun main(args:Array<String>) {
			// Start
			AIRanksTool()
		}
	}
}
