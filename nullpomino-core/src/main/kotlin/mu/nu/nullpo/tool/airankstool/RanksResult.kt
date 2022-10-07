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

package mu.nu.nullpo.tool.airankstool

import java.awt.BorderLayout
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.beans.PropertyChangeEvent
import java.beans.PropertyChangeListener
import java.util.Collections
import javax.swing.BorderFactory
import javax.swing.JButton
import javax.swing.JDialog
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.ProgressMonitor
import javax.swing.SwingWorker
import javax.swing.WindowConstants
import kotlin.math.pow

class RanksResult(parent:JFrame, private var ranks:Ranks?, private val bestNRanks:Int, ascendant:Boolean,
	private val getUIText:(String)->String):JDialog(parent, true), ActionListener, PropertyChangeListener {
	private var surfaceComponent:SurfaceComponent? = null
	private var surfaceComponentMirrored:SurfaceComponent? = null
	private var labelScore:JLabel? = null
	private var labelScoreMirrored:JLabel? = null
	private var buttonNext:JButton? = null
	private var buttonPrevious:JButton? = null
	private val progressMonitor:ProgressMonitor

	private var currentSurface = 0
	private var currentSurfaceMirrored = 0
	private var indexSurface = 0
	private val maxJump:Int get() = ranks?.maxJump ?: 0
	private val stackWidth:Int get() = ranks?.stackWidth ?: 0

	private val factorCompare:Int
	private val task:Task

	private var surfaceRanksBests:Array<SurfaceRank>? = null
	private var surfaceRanksBestsMirrored:Array<SurfaceRank>? = null

	//private JFrame parent;
	internal inner class SurfaceComparator:Comparator<Int> {
		override fun compare(o1:Int?, o2:Int?):Int =
			(factorCompare*ranks!!.getRankValue(o2!!)).compareTo(factorCompare*ranks!!.getRankValue(o1!!))
	}

	internal inner class SurfaceRank(val surface:Int, val rank:Int):Comparable<SurfaceRank> {
		override fun compareTo(other:SurfaceRank):Int = (factorCompare*other.rank).compareTo(factorCompare*rank)
	}

	internal inner class Task:SwingWorker<Void, Void>() {
		public override fun doInBackground():Void? {
			var progress = 0
			setProgress(0)
			val surfaceRankBestsList = ArrayList<SurfaceRank>(bestNRanks+1)
			for(i in 0 until bestNRanks) {
				val rank = ranks!!.getRankValue(i)
				surfaceRankBestsList.add(SurfaceRank(i, rank))
			}
			var iMin = surfaceRankBestsList.indexOf(Collections.min(surfaceRankBestsList))
			var iMax = surfaceRankBestsList.indexOf(Collections.max(surfaceRankBestsList))

			for(i in 0 until ranks!!.size) {
				val rank = ranks!!.getRankValue(i)

				val surfaceRank = SurfaceRank(i, rank)

				if(surfaceRank<surfaceRankBestsList[iMax]) {
					surfaceRankBestsList.add(SurfaceRank(i, rank))
					surfaceRankBestsList.removeAt(iMax)
					if(surfaceRank<surfaceRankBestsList[iMin]) iMin = surfaceRankBestsList.size-1

					iMax = surfaceRankBestsList.indexOf(Collections.max(surfaceRankBestsList))
				}

				if(0==i%(ranks!!.size/100)&&i>=ranks!!.size/100) {
					progress++
					setProgress(progress)
				}
			}
			surfaceRankBestsList.sort()

			surfaceRanksBests = Array(bestNRanks) {surfaceRankBestsList[it]}
			surfaceRanksBestsMirrored = Array(bestNRanks) {
				val mirroredSurface = getMirroredSurface(surfaceRankBestsList[it].surface)
				SurfaceRank(mirroredSurface, ranks!!.getRankValue(mirroredSurface))
			}

			ranks = null

			return null
		}

		public override fun done() {
			title = getUIText("Result_Title")
			initUI()
			pack()
			isVisible = true
			ranks = null
		}
	}

	init {
		defaultCloseOperation = WindowConstants.DISPOSE_ON_CLOSE

		factorCompare = if(ascendant) -1 else 1
		progressMonitor = ProgressMonitor(parent, getUIText("Result_Progress_Message"), "", 0, 100)
		progressMonitor.setProgress(0)
		task = Task()
		task.addPropertyChangeListener(this)
		task.execute()
	}//this.parent=parent;

	private fun getMirroredSurface(surface:Int):Int {
		var surfaceWork = surface
		var surfaceMirrored = 0

		var factorD = (2*maxJump+1).toDouble().pow((stackWidth-2).toDouble()).toInt()
		for(i in 0 until stackWidth-1) {
			val `val` = surfaceWork%(2*maxJump+1)
			surfaceMirrored += factorD*(2*maxJump-`val`)
			surfaceWork /= 2*maxJump+1
			factorD /= 9
		}
		return surfaceMirrored
	}

	private fun initUI() {
		indexSurface = 0
		currentSurface = surfaceRanksBests!![indexSurface].surface

		surfaceComponent = SurfaceComponent(maxJump, stackWidth, currentSurface)
		labelScore = JLabel(getUIText("Result_Score")+surfaceRanksBests!![indexSurface].rank)

		currentSurfaceMirrored = surfaceRanksBestsMirrored!![indexSurface].surface
		surfaceComponentMirrored = SurfaceComponent(maxJump, stackWidth, currentSurfaceMirrored)
		labelScoreMirrored = JLabel(getUIText("Result_Score")+surfaceRanksBestsMirrored!![indexSurface].rank)

		buttonNext = JButton(getUIText("Result_Next"))
		buttonNext!!.actionCommand = "next"
		buttonNext!!.addActionListener(this)
		buttonNext!!.setMnemonic('N')
		buttonPrevious = JButton(getUIText("Result_Previous"))
		buttonPrevious!!.actionCommand = "previous "
		buttonPrevious!!.isEnabled = false
		buttonPrevious!!.addActionListener(this)
		buttonPrevious!!.setMnemonic('P')

		val pane = JPanel(BorderLayout())
		val surfacePane = JPanel(BorderLayout())
		surfacePane.add(surfaceComponent!!, BorderLayout.CENTER)
		surfacePane.add(labelScore!!, BorderLayout.SOUTH)
		val surfacePaneMirrored = JPanel(BorderLayout())
		surfacePaneMirrored.add(surfaceComponentMirrored!!, BorderLayout.CENTER)
		surfacePaneMirrored.add(labelScoreMirrored!!, BorderLayout.SOUTH)
		val highPane = JPanel()
		highPane.add(surfacePane)
		highPane.add(surfacePaneMirrored)
		val buttonsPane = JPanel()

		buttonsPane.add(buttonPrevious)
		buttonsPane.add(buttonNext)
		pane.border = BorderFactory.createEmptyBorder(20, 20, 20, 20)
		pane.add(highPane, BorderLayout.CENTER)
		pane.add(buttonsPane, BorderLayout.SOUTH)
		contentPane.add(pane)

		//getContentPane().add(surfaceComponent);
	}

	override fun actionPerformed(e:ActionEvent) {
		if("next"==e.actionCommand) {
			if(indexSurface<bestNRanks-1) {
				indexSurface++
				currentSurface = surfaceRanksBests!![indexSurface].surface
				surfaceComponent!!.setSurface(currentSurface)
				labelScore!!.text = getUIText("Result_Score")+surfaceRanksBests!![indexSurface].rank

				currentSurfaceMirrored = surfaceRanksBestsMirrored!![indexSurface].surface
				surfaceComponentMirrored!!.setSurface(currentSurfaceMirrored)
				labelScoreMirrored!!.text = getUIText("Result_Score")+surfaceRanksBestsMirrored!![indexSurface].rank

				if(indexSurface>0) buttonPrevious!!.isEnabled = true
				if(indexSurface==bestNRanks-1) buttonNext!!.isEnabled = false
			}
		} else if(indexSurface>0) {
			indexSurface--
			currentSurface = surfaceRanksBests!![indexSurface].surface
			surfaceComponent!!.setSurface(currentSurface)
			labelScore!!.text = getUIText("Result_Score")+surfaceRanksBests!![indexSurface].rank

			currentSurfaceMirrored = getMirroredSurface(currentSurface)
			surfaceComponentMirrored!!.setSurface(currentSurfaceMirrored)
			labelScoreMirrored!!.text = getUIText("Result_Score")+surfaceRanksBestsMirrored!![indexSurface].rank

			if(indexSurface<bestNRanks-1) buttonNext!!.isEnabled = true
			if(indexSurface==0) buttonPrevious!!.isEnabled = false
		}
	}

	override fun propertyChange(evt:PropertyChangeEvent) {
		if("progress"==evt.propertyName) {
			val progress = evt.newValue as Int
			progressMonitor.setProgress(progress)
			val message = String.format(getUIText("Result_Progress_Note"), progress)
			progressMonitor.note = message
		}

		if(progressMonitor.isCanceled) task.cancel(true)
	}

	companion object {
		/** */
		private const val serialVersionUID = 1L
	}
}
