package mu.nu.nullpo.gui.slick

import mu.nu.nullpo.game.event.EventReceiver.COLOR
import org.apache.log4j.Logger
import org.newdawn.slick.Graphics
import java.io.*

object RenderStaffRoll {
	/** Log */
	internal val log = Logger.getLogger(RenderStaffRoll::class.java)

	internal lateinit var BG:Graphics
		private set

	/** Folder names list */

	init {
		try {
			val strList = BufferedReader(FileReader("config/list/staff.lst")).readLines()
			BG = Graphics(160, strList.size*16)

			var dx = 0
			var dy = 0
			val scale = .5f
			var fontColor = COLOR.WHITE
			strList.forEach lit@{line ->
				var str = line.trim {it<=' '} // Trim the space
				when {
					str.startsWith("#") -> { // Commment-line. Ignore it.
						str = ""
					}
					str.startsWith(":") -> {//Category Tags
						fontColor = COLOR.GREEN
					}
					str.isNotEmpty() -> {// Member name
					}
				}
				str.forEach {
					val chr = it.toInt()
					var sx = (chr-32)%32
					var sy = (chr-32)/32+fontColor.ordinal*3
					val sz = (12*scale).toInt()
					sx *= 12
					sy *= 14
					BG.drawImage(ResourceHolder.imgFontNano, dx.toFloat(), dy.toFloat(), dx+sz*scale, dy+sz*scale, sx.toFloat(), sy.toFloat(), (sx+12).toFloat(), (sy+14).toFloat())
					dx += sz
				}
				dy += 20
			}

		} catch(e:IOException) {
			log.error("Failed to load Staffroll list file", e)
		}

	}

}

