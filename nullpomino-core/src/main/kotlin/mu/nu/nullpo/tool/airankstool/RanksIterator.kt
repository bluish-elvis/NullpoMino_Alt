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

import java.awt.BorderLayout
import java.awt.GridLayout
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.beans.PropertyChangeEvent
import java.beans.PropertyChangeListener
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.util.concurrent.ExecutionException
import javax.swing.BorderFactory
import javax.swing.JButton
import javax.swing.JDialog
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JProgressBar
import javax.swing.SwingWorker
import javax.swing.WindowConstants

class RanksIterator(parent:JFrame, inputFile:String, private val outputFile:String, private val numIterations:Int,
	private val getUIText:(String)->String):
	JDialog(parent, getUIText("Progress_Message")), PropertyChangeListener, ActionListener {

	private var ranks:Ranks? = null
	private var ranksFrom:Ranks? = null
	private var iteration = 0

	private val progressLabel:JLabel
	private val progressBar:JProgressBar
	private val cancelButton:JButton
	private val allIterations:AllIterations
	private var oneIteration:OneIteration? = null

	internal inner class OneIteration(private val totalParts:Int, private var ranks:Ranks?):SwingWorker<Void, String>() {
		private var ranksIteratorPart:Array<RanksIteratorPart> = emptyArray()
		private var cancelled = false

		init {
			cancelled = false

		}

		fun iterate() {

			if(ranks!!.completionPercentageIncrease()) progress = ranks!!.completionPercentage
		}

		override fun doInBackground():Void? {
			ranksIteratorPart = Array(totalParts) {i ->
				RanksIteratorPart(this, ranks!!, i, totalParts)
			}
			for(i in 0 until totalParts)
				try {
					ranksIteratorPart[i].start()
					ranksIteratorPart[i].join()
				} catch(e:InterruptedException) {
					// TODO Auto-generated catch block
					e.printStackTrace()
				}

			if(cancelled) {
				//System.out.println("cancelled !");
				ranks = ranks!!.ranksFrom
				allIterations.cancelTask()

			}
			progress = 100
			return null
		}

		fun cancelTask() {
			cancelled = true
			ranksIteratorPart.forEach {it.interrupt()}
		}

	}

	internal inner class AllIterations(private val totalParts:Int, private val ranksIterator:RanksIterator,
		private val inputFile:String):SwingWorker<Void, String>() {
		var cancelled = false

		init {
			cancelled = false
			progress = 0

		}

		public override fun doInBackground():Void? {
			progressLabel.text = getUIText("Progress_Note_Load_File")

			val fis:FileInputStream
			val `in`:ObjectInputStream
			if(inputFile.trim {it<=' '}.isEmpty())
				ranksFrom = Ranks(4, 9)
			else
				try {
					fis = FileInputStream(AIRanksConstants.RANKSAI_DIR+inputFile)
					`in` = ObjectInputStream(fis)
					ranksFrom = `in`.readObject() as Ranks
					`in`.close()

				} catch(e:FileNotFoundException) {
					ranksFrom = Ranks(4, 9)
				} catch(e:IOException) {
					// TODO Auto-generated catch block
					e.printStackTrace()
				} catch(e:ClassNotFoundException) {
					e.printStackTrace()
				}

			ranks = Ranks(ranksFrom)

			for(n in 0 until numIterations) {
				iteration = n

				oneIteration = OneIteration(totalParts, ranks)
				oneIteration!!.addPropertyChangeListener(ranksIterator)
				oneIteration!!.execute()
				try {
					oneIteration!!.get()
				} catch(e:InterruptedException) {
					// TODO Auto-generated catch block
					e.printStackTrace()
				} catch(e:ExecutionException) {
					e.printStackTrace()
				}

				if(cancelled)
				//System.out.println("cancelled !");
				//ranks=ranks.getRanksFrom();
				//allIterations.cancelTask();
					break

				ranks!!.scaleRanks()
				//lastError=ranks.getErrorPercentage();
				//lastErrorMax=ranks.getMaxError();
				if(n!=numIterations-1) {
					ranksFrom = ranks!!.ranksFrom
					ranksFrom!!.ranksFrom = ranks
					ranks = ranksFrom
				}

			}
			//System.out.println("save file !");
			progressLabel.text = getUIText("Progress_Note_Save_File")

			try {
				val ranksAIDir = File(AIRanksConstants.RANKSAI_DIR)
				if(!ranksAIDir.exists()) ranksAIDir.mkdirs()
				val fos = FileOutputStream(AIRanksConstants.RANKSAI_DIR+outputFile)
				val out = ObjectOutputStream(fos)
				ranks!!.freeRanksFrom()
				out.writeObject(ranks)
				out.close()

			} catch(e:Exception) {
				e.printStackTrace()
			}

			ranks = null
			ranksFrom = null
			progress = 100

			return null
		}

		fun cancelTask() {
			cancelled = true

		}

		override fun done() {

			dispose()

			//new RanksResult(parent,ranks,100,false);

		}

	}

	init {
		defaultCloseOperation = WindowConstants.DO_NOTHING_ON_CLOSE

		progressLabel = JLabel(String.format(getUIText("Progress_Note"), 1, 0, numIterations, 0))
		val message = String.format(getUIText("Progress_Note"), 100, 100, 100, 100)
		progressLabel.text = message

		progressBar = JProgressBar(0, 100)
		cancelButton = JButton(getUIText("Progress_Cancel_Button"))
		cancelButton.actionCommand = "cancel"
		cancelButton.addActionListener(this)
		val mainPane = JPanel(BorderLayout())
		val pane = JPanel(GridLayout(0, 1))
		mainPane.border = BorderFactory.createEmptyBorder(20, 20, 20, 20)

		pane.add(progressLabel)
		pane.add(progressBar)
		pane.add(cancelButton)
		mainPane.add(pane, BorderLayout.CENTER)
		add(mainPane)
		pack()
		isVisible = true

		//size=ranks.getSize();
		val numProcessors = Runtime.getRuntime().availableProcessors()
		//System.out.println(numProcessors);

		allIterations = this.AllIterations(numProcessors, this, inputFile)
		//allIterations.addPropertyChangeListener(this);
		allIterations.execute()

	}

	override fun propertyChange(evt:PropertyChangeEvent) {
		if("progress"==evt.propertyName) {
			val totalCompletion = (100*iteration+ranks!!.completionPercentage)/numIterations
			progressBar.value = totalCompletion

			val message =
				String.format(getUIText("Progress_Note"), iteration+1, ranks!!.completionPercentage, numIterations, totalCompletion)
			progressLabel.text = message

		}

	}

	override fun actionPerformed(arg0:ActionEvent) {
		oneIteration!!.cancelTask()
	}

	companion object {
		/** */
		private const val serialVersionUID = 1L
	}

}
