package mu.nu.nullpo.game.net

/** This will be thrown when a client requests disconnection. Used by
 * NetServer. */
class NetServerDisconnectRequestedException:RuntimeException {

	constructor()

	constructor(message:String):super(message)

	constructor(cause:Throwable):super(cause)

	constructor(message:String, cause:Throwable):super(message, cause)

	companion object {
		private const val serialVersionUID = 1L
	}
}
