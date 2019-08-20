package mu.nu.nullpo.gui.slick.img

import mu.nu.nullpo.gui.slick.ResourceHolder
import kotlin.math.cos
import kotlin.math.pow

object RenderBG {


 fun spinBG(bg:Int) {
				val bgmax = ResourceHolder.bGmax
				val bg = bg%bgmax
				if(bg in 0 until bgmax) {
					val bgi = ResourceHolder.imgPlayBG[bg]
						val sc = (1-cos(bgi.rotation/Math.PI.pow(3.0))/Math.PI).toFloat()*1024f/minOf(bgi.width, bgi.height)
						val cx = bgi.width/2*sc
						val cy = bgi.height/2*sc
						bgi.setCenterOfRotation(cx, cy)
						bgi.rotate(0.04f)
						bgi.draw(320-cx, 240-cy, sc)
				}
			}
	fun kaleidoSquare(){TODO()}
	fun waterFall(){TODO()}
	fun abyss(){TODO()}
}