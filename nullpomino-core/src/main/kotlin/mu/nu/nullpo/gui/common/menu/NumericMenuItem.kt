/*
 * Copyright (c) 2010-2021, NullNoname
 * Kotlin converted and modified by Venom=Nhelv
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of NullNoname nor the names of its
 *       contributors may be used to endorse or promote products derived from
 *       this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package mu.nu.nullpo.gui.common.menu

open class NumericMenuItem(
	name:String,
	color:Int,
	state:Int = 50,
	private val minValue:Int = 0,
	private val maxValue:Int = 100,
	private val step:Int = 1,
	private val arithmeticStyle:Int = ARITHSTYLE_MODULAR
):MenuItem(name) {
	init {
		this.color = color
		this.state = state
	}

	override fun changeState(change:Int) {
		state += step*change
		val range = maxValue-minValue
		if(state>maxValue)
			when(arithmeticStyle) {
				ARITHSTYLE_MODULAR -> do
					state -= range
				while(state>maxValue)
				ARITHSTYLE_SATURATE -> state = maxValue
			}
		else if(state<minValue)
			when(arithmeticStyle) {
				ARITHSTYLE_MODULAR -> do
					state += range
				while(state<maxValue)
				ARITHSTYLE_SATURATE -> state = minValue
			}
	}

	companion object {
		const val ARITHSTYLE_MODULAR = 0
		const val ARITHSTYLE_SATURATE = 1
	}
}