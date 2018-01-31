package net.omegaboshi.nullpomino.game.subsystem.randomizer

class BagBonusBagRandomizer:BagBonusRandomizer {

	private var bonusbag:IntArray = IntArray(pieces.size)
	private var bonuspt:Int = 0

	constructor():super()

	constructor(pieceEnable:BooleanArray, seed:Long):super(pieceEnable, seed)

	init {
		for(i in pieces.indices) {
			bag[i] = pieces[i]
			bonusbag[i] = pieces[i]
		}
		shuffleBonus()
		shuffle()
	}

	override fun shuffle() {
		bag[bonus] = bonusbag[bonuspt]
		for(i in baglen downTo 2) {
			val j = r.nextInt(i)
			val temp = bag[i-1]
			bag[i-1] = bag[j]
			bag[j] = temp
			if(bonus==i-1)
				bonus = j
			else if(bonus==j) bonus = i-1
		}
	}

	private fun shuffleBonus() {
		for(i in pieces.size downTo 2) {
			val j = r.nextInt(i)
			val temp = bonusbag[i-1]
			bonusbag[i-1] = bonusbag[j]
			bonusbag[j] = temp
		}
	}

	override fun next():Int {
		val id = bag[pt]
		pt++
		if(pt==baglen) {
			pt = 0
			bonuspt++
			if(bonuspt==pieces.size) {
				bonuspt = 0
				shuffleBonus()
			}
			shuffle()
		}
		return id
	}
}
