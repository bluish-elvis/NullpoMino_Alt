package net.omegaboshi.nullpomino.game.subsystem.randomizer

class NintendoRandomizer:Randomizer {
	private var prev:Int = pieces.size
	internal var roll:Int = pieces.size+1

	constructor():super()
	constructor(pieceEnable:BooleanArray, seed:Long):super(pieceEnable, seed)

	override fun init() {
		prev = pieces.size
	}

	override fun next():Int {
		var id:Int = r.nextInt(roll)
		if(id==prev||id>=pieces.size) id = r.nextInt(pieces.size)
		prev = id
		return pieces[id]
	}

}
