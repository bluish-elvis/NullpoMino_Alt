package mu.nu.nullpo.gui

abstract class MouseInputDummy protected constructor() {
	val das=27
	val arr=3
	var mouseX:Int = 0
		protected set
	var mouseY:Int = 0
		protected set
	protected val mousePressed:IntArray = IntArray(3)

	val isMouseClicked:Boolean
		get() = mousePressed[0]==1

	val isMouseMiddleClicked:Boolean
		get() = mousePressed[1]==1

	val isMouseRightClicked:Boolean
		get() = mousePressed[2]==1

	val isMousePressed:Boolean
		get() = mousePressed[0]>0

	val isMouseMiddlePressed:Boolean
		get() = mousePressed[1]>0

	val isMouseRightPressed:Boolean
		get() = mousePressed[2]>0

	val isMenuRepeatLeft:Boolean
		get() = mousePressed[0]>das&&(mousePressed[0]-das) and arr==0

	val isMenuRepeatMiddle:Boolean
		get() = mousePressed[1]>das&&(mousePressed[1]-das) and arr==0

	val isMenuRepeatRight:Boolean
		get() = mousePressed[2]>das&&(mousePressed[2]-das) and arr==0

	val mouseHold:Int
		get() = if(mousePressed[0]<das)mousePressed[0] else das+((mousePressed[0]-das)%arr)
	val mouseHoldMiddle:Int
		get() = if(mousePressed[1]<das)mousePressed[1] else das+((mousePressed[1]-das)%arr)
	val mouseHoldRight:Int
		get() = if(mousePressed[2]<das)mousePressed[2] else das+((mousePressed[2]-das)%arr)

}
