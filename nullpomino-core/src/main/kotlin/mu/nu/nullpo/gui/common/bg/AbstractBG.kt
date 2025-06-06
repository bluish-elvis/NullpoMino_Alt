/*
 Copyright (c) 2022-2024, NullNoname
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

package mu.nu.nullpo.gui.common.bg

import zeroxfc.nullpo.custom.libs.Interpolation
import zeroxfc.nullpo.custom.libs.Vector.Companion.almostEqual

abstract class AbstractBG<T:Any?>(val img:mu.nu.nullpo.gui.common.ResourceImage<T>,val addBGFX:AbstractBG<*>? = null) {
	open val res:T get() = img.res
	/** Speed Multiplier: Recommended .5f-1f-2f*/
	open var speed = 1f
	protected var spdN = 0f
	open var tick = 0
	/** Performs an update tick on the background. Advisably used in onLast.*/
	open fun update(){
		addBGFX?.update()
		if(spdN!=speed)
			spdN = if(almostEqual(spdN, speed, .001f/100)) speed else Interpolation.lerp(spdN, speed, .05f)
	}
	/** Resets the background to its base state.*/
	abstract fun reset()
	/** Draws the background to the game screen.*/
	abstract fun draw(render:mu.nu.nullpo.gui.common.AbstractRenderer,bg:Boolean = true) /*{
		bufI.draw()
	}*/
	open fun drawLite() =
		img.draw(0, 0, 640, 480, 0, 0, img.width, img.height)

	protected val log:org.apache.logging.log4j.Logger = org.apache.logging.log4j.LogManager.getLogger()

	init {
		log.debug("${this::class.java.name} created: ${img.name}")
//		img.load()
	}

	companion object {
		const val RG = (kotlin.math.PI/180).toFloat()
	}
}
