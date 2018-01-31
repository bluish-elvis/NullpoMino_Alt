package net.omegaboshi.nullpomino.game.subsystem.randomizer

class NintendoRandomizer:Randomizer {

	internal var prev:Int = pieces.size
	internal var roll:Int = pieces.size+1

	constructor():super()

	constructor(pieceEnable:BooleanArray, seed:Long):super(pieceEnable, seed)

	override fun next():Int {
		var id = r.nextInt(roll)
		if(id==prev||id==pieces.size) id = r.nextInt(pieces.size)
		prev = id
		return pieces[id]
	}

}
