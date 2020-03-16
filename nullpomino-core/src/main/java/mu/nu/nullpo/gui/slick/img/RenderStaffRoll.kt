package mu.nu.nullpo.gui.slick.img

import mu.nu.nullpo.game.event.EventReceiver.COLOR
import mu.nu.nullpo.gui.slick.ResourceHolder
import org.apache.log4j.Logger
import org.newdawn.slick.*
import java.io.*

object RenderStaffRoll {
	/** Log */
	internal val log = Logger.getLogger(RenderStaffRoll::class.java)
	lateinit var img:Image; private set
	val BG get() = img.graphics
	val height get() = img.height

	private fun cmp(c:Char):Float = when(c) {
		' ', ':' -> .5f
		'(',')','.' -> .33f
		'/','I' -> .25f
		else -> 0f
	}

	/** Folder names list */
	init {
		try {
			val strList = BufferedReader(FileReader("config/list/staff.lst")).readLines()
			val scale = .5f
			val w = 12f*scale
			val h = 14f*scale

			img = Image(160, (strList.size*20*scale).toInt())
			var dy = 0f
			strList.forEach lit@{line ->
				line.trim {it<=' '}.let {str ->
					if(!str.startsWith('#')&&!str.startsWith("//")) { // Commment-line. Ignore it.
						var dx = img.width/2f-(str.length-str.sumByDouble {cmp(it).toDouble()}.toFloat())*.5f*w
						str.forEachIndexed {i, it ->
							if(i>0||it!=':') {
								val fontColor = when(str.first()) {
									':' -> if(it.isUpperCase()) COLOR.BLUE else COLOR.GREEN
									else -> if(it.isUpperCase()) COLOR.ORANGE else COLOR.WHITE
								}
								val chr = it.toUpperCase().toInt()
								var sx = (chr-32)%32
								var sy = (chr-32)/32+fontColor.ordinal*3
								sx *= 12
								sy *= 14
								dx -= w*cmp(it)/2
								BG.drawImage(ResourceHolder.imgFontNano, dx, dy, dx+w, dy+h, sx.toFloat(), sy.toFloat(), sx+12f, sy+14f)
								dx += w*(1f-cmp(it)/2)
							}
						}
						dy += 20*scale
					}
				}
			}
		} catch(e:IOException) {
			log.error("Failed to load Staffroll list file", e)
		}

	}

	fun draw() = img.draw()
	fun draw(x:Float, y:Float, sy:Float, h:Float, filter:Color = Color.white) =
		img.draw(x, y, x+img.width.toFloat(), y+h, 0f, sy, img.width.toFloat(), sy+h, filter)

}

