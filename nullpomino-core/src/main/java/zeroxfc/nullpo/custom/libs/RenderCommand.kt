package zeroxfc.nullpo.custom.libs

internal class RenderCommand {
	var renderType:RenderType? = null
	var args:Array<Any> = emptyArray()

	private constructor() {
		// Use other constructor.
	}

	constructor(renderType:RenderType?, args:Array<Any>) {
		this.renderType = renderType
		this.args = args
	}

	enum class RenderType {
		Rectangle, Arc, Oval
	}
}