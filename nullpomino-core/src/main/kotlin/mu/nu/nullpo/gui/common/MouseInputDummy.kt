/*
 Copyright (c) 2010-2024, NullNoname
 All rights reserved.

 Converted to Kotlin and modified by Venom_Nhelv as bluish-elvis
 THIS IS NOT MADE IN ASSOCIATION WITH THE GAME CREATOR.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions are met:
     * Redistributions of source code must retain the above copyright
       notice, this list of conditions and the following disclaimer.
     * Redistributions in binary form must reproduce the above copyright
       notice, this list of conditions and the following disclaimer in the
       documentation and/or other materials provided with the distribution.
     * Neither the name of NullNoname nor the names of its
       contributors may be used to endorse or promote products derived from
       this software without specific prior written permission.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 POSSIBILITY OF SUCH DAMAGE.
 */

package mu.nu.nullpo.gui.common

abstract class MouseInputDummy protected constructor() {
	val das = 27
	val arr = 3
	var mouseX = 0; protected set
	var mouseY = 0; protected set
	protected val mousePressed = IntArray(3)

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
		get() = if(mousePressed[0]<das) mousePressed[0] else das+((mousePressed[0]-das)%arr)
	val mouseHoldMiddle:Int
		get() = if(mousePressed[1]<das) mousePressed[1] else das+((mousePressed[1]-das)%arr)
	val mouseHoldRight:Int
		get() = if(mousePressed[2]<das) mousePressed[2] else das+((mousePressed[2]-das)%arr)
}
