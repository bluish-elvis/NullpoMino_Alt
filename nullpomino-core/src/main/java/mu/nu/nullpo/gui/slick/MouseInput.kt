package mu.nu.nullpo.gui.slick

import mu.nu.nullpo.gui.MouseInputDummy
import org.newdawn.slick.Input

object MouseInput:MouseInputDummy() {

	fun update(input:Input) {
		mouseX = input.mouseX
		mouseY = input.mouseY
		if(input.isMouseButtonDown(Input.MOUSE_LEFT_BUTTON))
			mousePressed[0]++ else mousePressed[0] = 0
		if(input.isMouseButtonDown(Input.MOUSE_MIDDLE_BUTTON))
			mousePressed[1]++ else mousePressed[1] = 0
		if(input.isMouseButtonDown(Input.MOUSE_RIGHT_BUTTON))
			mousePressed[2]++ else mousePressed[2] = 0
	}
}
