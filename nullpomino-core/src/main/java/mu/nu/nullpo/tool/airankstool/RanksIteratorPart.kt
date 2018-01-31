package mu.nu.nullpo.tool.airankstool

import mu.nu.nullpo.tool.airankstool.RanksIterator.OneIteration

class RanksIteratorPart internal constructor(private val oneIteration:OneIteration, private var ranks:Ranks?, i:Int,
	totalParts:Int):Thread() {

	private val sMin:Int
	private val sMax:Int
	private val size:Int = this.ranks!!.size
	private val surface:IntArray
	private val surfaceDecodedWork:IntArray

	init {

		sMin = i*size/totalParts
		sMax = if(i==totalParts-1) size else (i+1)*size/totalParts
		surface = IntArray(this.ranks!!.stackWidth-1)
		this.ranks!!.decode(sMin, surface)
		surfaceDecodedWork = IntArray(ranks!!.stackWidth-1)
		this.ranks!!.decode(sMin, surfaceDecodedWork)
		priority = Thread.MIN_PRIORITY
	}

	override fun run() {

		for(s in sMin until sMax) {
			ranks!!.iterateSurface(surface, surfaceDecodedWork)

			synchronized(this) {
				oneIteration.iterate()
			}
			if(Thread.interrupted()) {

				ranks = null
				break

			}

		}

	}
}
