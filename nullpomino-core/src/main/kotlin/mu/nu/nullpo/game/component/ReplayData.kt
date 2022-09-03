/*
 * Copyright (c) 2010-2022, NullNoname
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
package mu.nu.nullpo.game.component

import mu.nu.nullpo.util.CustomProperties
import java.io.Serializable

/** リプレイで使用する button input dataのクラス */
class ReplayData:Serializable {

	/** Button input data */
	var inputDataArray:ArrayList<Int> = ArrayList(DEFAULT_ARRAYLIST_SIZE)

	/** Default constructor */
	constructor() {
		reset()
	}

	/** Copy constructor
	 * @param r Copy source
	 */
	constructor(r:ReplayData) {
		replace(r)
	}

	/** Reset to defaults */
	fun reset() {
		inputDataArray.clear()
	}

	/** 設定を[r]からコピー */
	fun replace(r:ReplayData) {
		reset()
		r.inputDataArray.forEachIndexed {i, it ->
			inputDataArray.add(i, it)
		}

	}

	/** button input状況を設定
	 * @param input button input状況のビット flag
	 * @param frame frame (経過 time）
	 */
	fun setInputData(input:Int, frame:Int) {
		if(frame<0||frame>=inputDataArray.size) inputDataArray.add(input)
		else inputDataArray[frame] = input
	}

	/** button input状況を取得
	 * @param frame frame (経過 time）
	 * @return button input状況のビット flag
	 */
	fun getInputData(frame:Int):Int = if(frame<0||frame>=inputDataArray.size) 0 else inputDataArray[frame]

	/** プロパティセットに保存
	 * @param p プロパティセット
	 * @param id 任意のID (Player IDなど）
	 * @param maxFrame 保存する frame count (-1で全部保存）
	 */
	fun writeProperty(p:CustomProperties, id:Int, maxFrame:Int) {
		var max = maxFrame
		if(maxFrame<0||maxFrame>inputDataArray.size) max = inputDataArray.size

		for(i in 0 until max) {
			val input = getInputData(i)
			val previous = getInputData(i-1)
			if(input!=previous) p.setProperty("$id.r.$i", input)
		}
		p.setProperty("$id.r.max", max)
	}

	/** プロパティセットから読み込み
	 * @param p プロパティセット
	 * @param id 任意のID (Player IDなど）
	 */
	fun readProperty(p:CustomProperties, id:Int) {
		reset()
		val max = p.getProperty("$id.r.max", 0)
		var input = 0

		for(i in 0 until max) {
			val data = p.getProperty("$id.r.$i", -1)
			if(data!=-1) input = data
			setInputData(input, i)
		}
	}

	companion object {
		/** Serial version ID */
		private const val serialVersionUID = 737226985994393117L

		/** Button input dataの default の長さ */
		const val DEFAULT_ARRAYLIST_SIZE = 60*60*15
	}
}
