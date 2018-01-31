package net.omegaboshi.nullpomino.game.subsystem.randomizer

class GameBoyRandomizer:Randomizer {

	internal var id:Int =  r.nextInt(pieces.size)
	internal var roll:Int = 6*pieces.size-3

	constructor():super()

	constructor(pieceEnable:BooleanArray, seed:Long):super(pieceEnable, seed)

	override fun next():Int {
		id = (id+r.nextInt(roll)/5+1)%pieces.size
		return pieces[id]
	}

}
