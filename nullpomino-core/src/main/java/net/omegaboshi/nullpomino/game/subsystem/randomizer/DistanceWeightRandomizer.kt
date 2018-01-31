package net.omegaboshi.nullpomino.game.subsystem.randomizer

import mu.nu.nullpo.game.component.Piece

abstract class DistanceWeightRandomizer:Randomizer {

	internal val initWeights = intArrayOf(3, 3, 0, 0, 3, 3, 0, 2, 2, 2, 2)
	internal var weights:IntArray = IntArray(pieces.size)
	internal var cumulative:IntArray= IntArray(pieces.size)
	internal var sum:Int = 0
	internal var id:Int = 0

	internal var firstPiece = true

	constructor():super()

	constructor(pieceEnable:BooleanArray, seed:Long):super(pieceEnable, seed)

	init {
		for(i in pieces.indices)
			weights[i] = initWeights[pieces[i]]
	}

	override fun next():Int {
		sum = 0
		for(i in pieces.indices) {
			sum += getWeight(i)
			cumulative[i] = sum
		}
		id = r.nextInt(sum)
		for(i in pieces.indices)
			if(id<cumulative[i]) {
				id = i
				break
			}
		weights[id] = 0
		for(i in pieces.indices)
			if(firstPiece&&pieces[i]==Piece.PIECE_O)
				weights[i] = 3
			else if(!isAtDistanceLimit(i)) weights[i]++
		firstPiece = false
		return id
	}

	protected abstract fun getWeight(i:Int):Int

	protected abstract fun isAtDistanceLimit(i:Int):Boolean

}
