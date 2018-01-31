package mu.nu.nullpo.tool.airankstool

import mu.nu.nullpo.util.CustomProperties
import org.apache.log4j.Logger
import org.jdesktop.layout.GroupLayout
import java.awt.BorderLayout
import java.awt.event.*
import java.io.*
import java.util.*
import javax.swing.*

class AIRanksTool:JFrame(), ActionListener {

	/** UI */

	//****************************
	//Tab 1 (Generation) variables
	//****************************

	// Input File
	private var inputFileLabel:JLabel? = null
	private var inputFileComboBox:JComboBox<Any>? = null

	// Output File
	private var outputFileLabel:JLabel? = null
	private var outputFileField:JTextField? = null

	//Number of Iterations
	private var numIterationsLabel:JLabel? = null
	private var spinModel:SpinnerNumberModel? = null
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
	private var ranksFileUsedComboBox:JComboBox<Any>? = null

	//Number of previews Used
	private var numPreviewsLabel:JLabel? = null
	private var numPreviewsSpinModel:SpinnerNumberModel? = null
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

	private var newFileText:String = ""

	init {

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
		inputFileComboBox = JComboBox(ranksList)
		inputFileComboBox!!.selectedIndex = fileIndex+1
		inputFileComboBox!!.toolTipText = getUIText("Main_Input_Tip")
		inputFileComboBox!!.actionCommand = "input"
		inputFileComboBox!!.addActionListener(this)

		// Output File
		outputFileLabel = JLabel(getUIText("Main_Output_Label"))
		outputFileField = JTextField(AIRanksConstants.DEFAULT_RANKS_FILE)
		outputFileField!!.columns = 20
		if(inputFileComboBox!!.selectedIndex>0) {
			outputFileField!!.text = inputFileComboBox!!.selectedItem as String
		}
		outputFileField!!.toolTipText = getUIText("Main_Output_Tip")

		//Number of iterations to run
		numIterationsLabel = JLabel(getUIText("Main_Iterations_Label"))
		spinModel = SpinnerNumberModel(1, 1, Integer.MAX_VALUE, 1)
		numIterationsSpinner = JSpinner(spinModel!!)
		numIterationsSpinner!!.toolTipText = getUIText("Main_Iterations_Tip")

		//Go button (starts the generation)
		goButton = JButton(getUIText("Main_Go_Label"))
		goButton!!.actionCommand = "go"
		goButton!!.addActionListener(this)
		goButton!!.toolTipText = getUIText("Main_Go_Tip")
		goButton!!.setMnemonic('G')

		// View Bests Button
		viewBestsButton = JButton(getUIText("Main_Bests_Label"))
		viewBestsButton!!.actionCommand = "bests"
		viewBestsButton!!.addActionListener(this)
		viewBestsButton!!.toolTipText = getUIText("Main_Bests_Tip")
		viewBestsButton!!.setMnemonic('B')

		// View worsts Button
		viewWorstsButton = JButton(getUIText("Main_Worsts_Label"))
		viewWorstsButton!!.actionCommand = "worsts"
		viewWorstsButton!!.addActionListener(this)
		viewWorstsButton!!.toolTipText = getUIText("Main_Worsts_Tip")
		viewWorstsButton!!.setMnemonic('W')

		//*******************************************************************

		//Tab 2

		//Ranks File Used
		ranksFileUsedLabel = JLabel(getUIText("Main_Ranks_File_Used_Label"))

		if(fileIndex>=0) {
			ranksFileUsedComboBox = JComboBox(children!!)

			ranksFileUsedComboBox!!.setSelectedIndex(fileIndex)
		} else {

			if(children==null||children.isEmpty()) {
				val list = arrayOf(" ")
				ranksFileUsedComboBox = JComboBox(list)
			}

			ranksFileUsedComboBox!!.setSelectedIndex(0)

		}
		ranksFileUsedComboBox!!.toolTipText = getUIText("Main_Ranks_File_Used_Tooltip")
		ranksFileUsedComboBox!!.actionCommand = "input2"
		ranksFileUsedComboBox!!.addActionListener(this)

		//Number of previews to use
		numPreviewsLabel = JLabel(getUIText("Main_Num_Previews_Label"))
		numPreviewsSpinModel = SpinnerNumberModel(2, 0, Integer.MAX_VALUE, 1)
		numPreviewsSpinner = JSpinner(numPreviewsSpinModel!!)
		if(numPreviews>=0) {
			numPreviewsSpinner!!.value = numPreviews
		}
		numPreviewsSpinner!!.toolTipText = getUIText("Main_Num_Previews_Tip")

		//Switch to allow hold
		allowHoldLabel = JLabel(getUIText("Main_Allow_Hold"))
		allowHoldCheckBox = JCheckBox()
		allowHoldCheckBox!!.isSelected = allowHold
		allowHoldCheckBox!!.toolTipText = getUIText("Main_Allow_Hold_Tip")

		//Speed Limit
		speedLimitLabel = JLabel(getUIText("Main_Speed_Limit_Label"))
		speedLimitField = JFormattedTextField(speedLimit)
		speedLimitField!!.toolTipText = getUIText("Main_Speed_Limit_Tip")

		// Save config Button
		saveAIConfigButton = JButton(getUIText("Main_Set_Default_Label"))
		saveAIConfigButton!!.actionCommand = "default"
		saveAIConfigButton!!.addActionListener(this)
		saveAIConfigButton!!.toolTipText = getUIText("Main_Set_Default_Tip")
		saveAIConfigButton!!.setMnemonic('S')

		//*************************************************************************
		// Generates the panels

		// Tab 1
		val formPane = JPanel()
		val layout = GroupLayout(formPane)
		formPane.layout = layout
		layout.autocreateGaps = true
		layout.autocreateContainerGaps = true
		val hGroup = layout.createSequentialGroup()

		val labelsPg = layout.createParallelGroup()
		labelsPg.add(inputFileLabel)
		labelsPg.add(outputFileLabel)
		labelsPg.add(numIterationsLabel)
		hGroup.add(labelsPg)

		val fieldsPg = layout.createParallelGroup()
		fieldsPg.add(inputFileComboBox)
		fieldsPg.add(outputFileField)
		fieldsPg.add(numIterationsSpinner)
		hGroup.add(fieldsPg)

		layout.horizontalGroup = hGroup

		val vGroup = layout.createSequentialGroup()

		vGroup.add(layout.createParallelGroup(GroupLayout.BASELINE).add(inputFileLabel).add(inputFileComboBox))
		vGroup.add(layout.createParallelGroup(GroupLayout.BASELINE).add(outputFileLabel).add(outputFileField))
		vGroup.add(layout.createParallelGroup(GroupLayout.BASELINE).add(numIterationsLabel).add(numIterationsSpinner))

		layout.verticalGroup = vGroup

		val buttonsPane = JPanel()
		buttonsPane.add(goButton)
		buttonsPane.add(viewBestsButton)
		buttonsPane.add(viewWorstsButton)

		val pane1 = JPanel(BorderLayout())

		pane1.border = BorderFactory.createEmptyBorder(20, 20, 20, 20)
		pane1.add(formPane, BorderLayout.CENTER)
		pane1.add(buttonsPane, BorderLayout.SOUTH)

		// Tab 2

		val formPane2 = JPanel()
		val layout2 = GroupLayout(formPane2)
		formPane2.layout = layout2
		layout2.autocreateGaps = true
		layout2.autocreateContainerGaps = true
		val hGroup2 = layout2.createSequentialGroup()

		val labelsPg2 = layout2.createParallelGroup()
		labelsPg2.add(ranksFileUsedLabel)
		labelsPg2.add(numPreviewsLabel)
		labelsPg2.add(allowHoldLabel)
		labelsPg2.add(speedLimitLabel)
		hGroup2.add(labelsPg2)

		val fieldsPg2 = layout2.createParallelGroup()
		fieldsPg2.add(ranksFileUsedComboBox)
		fieldsPg2.add(numPreviewsSpinner)
		fieldsPg2.add(allowHoldCheckBox)
		fieldsPg2.add(speedLimitField)
		hGroup2.add(fieldsPg2)

		layout2.horizontalGroup = hGroup2

		val vGroup2 = layout2.createSequentialGroup()

		vGroup2.add(layout2.createParallelGroup(GroupLayout.BASELINE).add(ranksFileUsedLabel).add(ranksFileUsedComboBox))
		vGroup2.add(layout2.createParallelGroup(GroupLayout.BASELINE).add(numPreviewsLabel).add(numPreviewsSpinner))
		vGroup2.add(layout2.createParallelGroup(GroupLayout.BASELINE).add(allowHoldLabel).add(allowHoldCheckBox))
		vGroup2.add(layout2.createParallelGroup(GroupLayout.BASELINE).add(speedLimitLabel).add(speedLimitField))
		layout2.verticalGroup = vGroup2

		val buttonsPane2 = JPanel()
		buttonsPane2.add(saveAIConfigButton)

		val pane2 = JPanel(BorderLayout())

		pane2.border = BorderFactory.createEmptyBorder(20, 20, 20, 20)
		pane2.add(formPane2, BorderLayout.CENTER)
		pane2.add(buttonsPane2, BorderLayout.SOUTH)

		//Tab 3

		tabbedPane = JTabbedPane()
		tabbedPane!!.addTab(getUIText("Main_Generation_Tab_Title"), pane1)
		tabbedPane!!.addTab(getUIText("Main_AI_Config_Tab_Title"), pane2)
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

			val ranksIterator = RanksIterator(this, inputFile, outputFile, numIterationsSpinner!!.value as Int)

			ranksIterator.addWindowListener(object:WindowAdapter() {

				override fun windowClosed(e:WindowEvent?) {
					var isInCombo = false
					var index = 0
					for(i in 1 until inputFileComboBox!!.itemCount) {
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

				val results = RanksResult(this, ranks!!, 100, "worsts"==e.actionCommand)
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
		viewBestsButton!!.isEnabled = b
		viewWorstsButton!!.isEnabled = b

	}

	companion object {
		private const val serialVersionUID = 1L
		/** Log */
		internal val log = Logger.getLogger(AIRanksConstants::class.java)

		/** Default language file */
		private val propLangDefault:CustomProperties = CustomProperties()
		/** Primary language file */
		private val propLang:CustomProperties = CustomProperties()

		fun getUIText(str:String):String = propLang.getProperty(str) ?: propLangDefault.getProperty(str, str) ?: str

		@JvmStatic
		fun main(args:Array<String>) {
			// Load language files
			try {
				val `in` = FileInputStream("config/lang/airankstool_default.xml")
				propLangDefault.load(`in`)
				`in`.close()
			} catch(e:IOException) {
				System.err.println("Couldn't load default UI language file")
				e.printStackTrace()
			}

			try {
				val `in` = FileInputStream("config/lang/airankstool_"+Locale.getDefault().country+".xml")
				propLang.load(`in`)
				`in`.close()
			} catch(e:IOException) {
			}

			// Start
			AIRanksTool()
		}
	}
}
