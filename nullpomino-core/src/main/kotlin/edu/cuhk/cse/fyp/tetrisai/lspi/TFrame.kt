package edu.cuhk.cse.fyp.tetrisai.lspi

import java.awt.Graphics2D
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import java.awt.image.BufferedImage
import java.io.File
import java.io.IOException
import java.util.Locale
import javax.imageio.ImageIO
import javax.swing.JFrame

class TFrame:JFrame, KeyListener {
	var label = TLabel(300, 700)
	var s:State?
	var orient = 0
	var slot = 0
	var mode = MANUAL
	//constructor
	constructor(s:State) {
		this.s = s
		s.label = label
		isResizable = false
		defaultCloseOperation = EXIT_ON_CLOSE // closes all windows when this is closed
		title = "Tetris BKW"
		contentPane = label.draw
		pack()
		label.border = .05
		label.setXscale(0.0, State.COLS.toDouble())
		label.setYscale(0.0, (State.ROWS+5).toDouble())
		addKeyListener(this) //may be unnecessary (not certain)
		isVisible = true
	}
	//constructor with title
	constructor(s:State, title:String?) {
		this.s = s
		s.label = label
		isResizable = false
		defaultCloseOperation = EXIT_ON_CLOSE // closes all windows when this is closed
		setTitle(title)
		contentPane = label.draw
		pack()
		label.border = .05
		label.setXscale(0.0, State.COLS.toDouble())
		label.setYscale(0.0, (State.ROWS+5).toDouble())
		addKeyListener(this) //may be unnecessary (not certain)
		isVisible = true
	}
	//switches which state is attached to this TFrame
	fun bindState(s:State?) {
		if(s!=null) s.label = null
		this.s = s
		s!!.label = label
	}

	override fun keyPressed(e:KeyEvent) {
		when(mode) {
			MANUAL -> {
				when(e.keyCode) {
					KeyEvent.VK_RIGHT -> {
						if(slot<State.COLS-State.pWidth[s!!.nextPiece][orient]) slot++
						s!!.clearNext()
						s!!.drawNext(slot, orient)
					}
					KeyEvent.VK_LEFT -> {
						if(slot>0) slot--
						s!!.clearNext()
						s!!.drawNext(slot, orient)
					}
					KeyEvent.VK_UP -> {
						orient++
						if(orient%State.pOrients[s!!.nextPiece]==0) orient = 0
						if(slot>State.COLS-State.pWidth[s!!.nextPiece][orient]) slot =
							State.COLS-State.pWidth[s!!.nextPiece][orient]
						s!!.clearNext()
						s!!.drawNext(slot, orient)
					}
					KeyEvent.VK_DOWN -> {
						if(!s!!.makeMove(orient, slot)) mode = NONE
						if(orient>=State.pOrients[s!!.nextPiece]) orient = 0
						if(slot>State.COLS-State.pWidth[s!!.nextPiece][orient]) slot =
							State.COLS-State.pWidth[s!!.nextPiece][orient]
						s!!.draw()
						if(mode==NONE) {
							label.text(State.COLS/2.0, State.ROWS/2.0, "You Lose")
						}
						s!!.clearNext()
						s!!.drawNext(slot, orient)
					}
					else -> {}
				}
			}
			NONE -> {}
			else -> println("unknown mode")
		}
	}

	override fun keyReleased(e:KeyEvent) {}
	override fun keyTyped(e:KeyEvent) {}
	fun save(filename:String) {
		val file = File(filename)
		val suffix = filename.substring(filename.lastIndexOf('.')+1)
		val bImage = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
		val graphic = bImage.graphics as Graphics2D
		paint(graphic)
		graphic.drawImage(bImage, 0, 0, null)
		//			png files
		if(suffix.lowercase(Locale.getDefault())=="png") {
			try {
				ImageIO.write(bImage, suffix, file)
			} catch(e:IOException) {
				e.printStackTrace()
			}
		} else println("unknown extension")
	}

	companion object {
		const val MANUAL = 0
		const val NONE = 1
		@JvmStatic fun main(args:Array<String>) {
			val s = State()
			val t = TFrame(s)
			s.draw()
			s.drawNext(0, 0)
			//t.save("picture.png");
		}
	}
}
