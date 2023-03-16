/*
 * Copyright (c) 2010-2022, NullNoname
 * Kotlin converted and modified by Venom=Nhelv.
 * THIS WAS NOT MADE IN ASSOCIATION WITH THE GAME CREATOR.
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
package mu.nu.nullpo.game.subsystem.mode

import mu.nu.nullpo.game.component.Block
import mu.nu.nullpo.game.component.Block.COLOR
import mu.nu.nullpo.game.component.Controller
import mu.nu.nullpo.game.component.Field
import mu.nu.nullpo.game.event.EventReceiver
import mu.nu.nullpo.game.play.GameEngine
import mu.nu.nullpo.game.play.GameManager
import mu.nu.nullpo.util.CustomProperties
import java.util.LinkedList
import kotlin.random.Random

/** TOOL-VS MAP EDIT */
class ToolMapEditor:AbstractMode() {
	/** Map dataI went into theProperty file */
	private var propMap = CustomProperties()

	/** Current MapAll contained in the filefield data */
	private var listFields:LinkedList<Field>? = null

	/** Current MapSetID */
	private var nowMapSetID = 0

	/** Current MapID */
	private var nowMapID = 0

	/* Mode name */
	override val name = "TOOL-VS MAP EDIT"

	/* Mode initialization */
	override fun modeInit(manager:GameManager) {
		super.modeInit(manager)
		propMap = CustomProperties()
		listFields = LinkedList()
		nowMapSetID = 0
		nowMapID = 0
	}

	/** MapRead into #[id]:[field] from [prop] */
	private fun loadMap(field:Field, prop:CustomProperties, id:Int) {
		field.reset()
		//field.readProperty(prop, id);
		field.stringToField(prop.getProperty("values.$id", ""))
		field.setAllAttribute(true, Block.ATTRIBUTE.VISIBLE, Block.ATTRIBUTE.OUTLINE)
		field.setAllAttribute(false, Block.ATTRIBUTE.SELF_PLACED)
	}

	/** MapSave from #[id]:[field] into [prop] */
	private fun saveMap(field:Field, prop:CustomProperties, id:Int) {
		//field.writeProperty(prop, id);
		prop.setProperty("values.$id", field.fieldToString())
	}

	/** AllMapRead
	 * @param setID MapSetID
	 */
	private fun loadAllMaps(setID:Int) {
		propMap = receiver.loadProperties("config/map/vsbattle/$setID.map") ?: CustomProperties()

		listFields!!.clear()

		val maxMap = propMap.getProperty("values.maxMapNumber", 0)
		for(i in 0 until maxMap) {
			val fld = Field()
			loadMap(fld, propMap, i)
			listFields!!.add(fld)
		}
	}

	/** AllMapSave
	 * @param setID MapSetID
	 */
	private fun saveAllMaps(setID:Int) {
		propMap = CustomProperties()

		val maxMap = listFields!!.size
		propMap.setProperty("values.maxMapNumber", maxMap)

		for(i in 0 until maxMap)
			saveMap(listFields!![i], propMap, i)

		propMap.save("config/map/vsbattle/$setID.map")
	}

	private fun grayToRandomColor(field:Field) {
		val rand = Random.Default

		for(i in field.hiddenHeight*-1 until field.height)
			for(j in 0 until field.width) {
				val col = field.getBlockColor(j, i)
				if(col==COLOR.BLACK||col==COLOR.WHITE) {
					var color:COLOR
					do
						color = COLOR.colors()[rand.nextInt(COLOR.COLOR_NUM)]
					while(color==field.getBlockColor(j-1, i)||color==field.getBlockColor(j+1, i)||
						color==field.getBlockColor(j, i-1)||color==field.getBlockColor(j, i-1))
					field.setBlockColor(j, i, color)
				}
			}
	}

	/* Initialization for each player */
	override fun playerInit(engine:GameEngine) {
		engine.frameColor = GameEngine.FRAME_COLOR_GRAY
		engine.createFieldIfNeeded()
		loadAllMaps(nowMapSetID)
	}

	/* Called at settings screen */
	override fun onSetting(engine:GameEngine):Boolean {
		// Configuration changes
		val change = updateCursor(engine, 7)

		if(change!=0) {
			engine.playSE("change")

			when(menuCursor) {
				0, 1, 2 -> {
				}
				3, 4, 5 -> {
					nowMapID += change
					if(nowMapID<0) nowMapID = listFields!!.size
					if(nowMapID>listFields!!.size) nowMapID = 0
				}
				6, 7 -> {
					nowMapSetID += change
					if(nowMapSetID<0) nowMapSetID = 99
					if(nowMapSetID>99) nowMapSetID = 0
				}
			}
		}

		// 決定
		if(menuTime<5) menuTime++ else if(engine.ctrl.isPush(Controller.BUTTON_A)) {
			engine.playSE("decide")

			if(menuCursor==0)
			// EDIT
				engine.enterFieldEdit()
			else if(menuCursor==1)
			// WHITE->?
				grayToRandomColor(engine.field)
			else if(menuCursor==2)
			// CLEAR
				engine.field.reset()
			else if(menuCursor==3) {
				// SAVE
				if(nowMapID>=0&&nowMapID<listFields!!.size)
					listFields!![nowMapID].replace(engine.field)
				else
					listFields!!.add(Field(engine.field))
			} else if(menuCursor==4) {
				// LOAD
				if(nowMapID>=0&&nowMapID<listFields!!.size) {
					engine.field.replace(listFields!![nowMapID])
					engine.field.setAllSkin(engine.skin)
				} else
					engine.field.reset()
			} else if(menuCursor==5) {
				// DELETE
				if(nowMapID>=0&&nowMapID<listFields!!.size) {
					listFields!!.removeAt(nowMapID)
					if(nowMapID>=listFields!!.size) nowMapID = listFields!!.size
				}
			} else if(menuCursor==6)
			// WRITE
				saveAllMaps(nowMapSetID)
			else if(menuCursor==7) {
				// READ
				loadAllMaps(nowMapSetID)
				nowMapID = 0
				engine.field.reset()
			}
		} else if(engine.ctrl.isPress(Controller.BUTTON_D)&&engine.ctrl.isPress(Controller.BUTTON_E))
		// 終了
			engine.quitFlag = true

		return true
	}

	/* Setting screen drawing */
	override fun renderSetting(engine:GameEngine) {
		receiver.drawMenuFont(engine, 0, 1, "FIELD EDIT", EventReceiver.COLOR.COBALT)
		if(menuCursor in 0..2)
			receiver.drawMenuFont(engine, 0, 2+menuCursor, "\u0082", EventReceiver.COLOR.RED)
		receiver.drawMenuFont(engine, 1, 2, "[EDIT]", menuCursor==0)
		receiver.drawMenuFont(engine, 1, 3, "[WHITE->?]", menuCursor==1)
		receiver.drawMenuFont(engine, 1, 4, "[CLEAR]", menuCursor==2)

		receiver.drawMenuFont(engine, 0, 6, "MAP DATA", EventReceiver.COLOR.COBALT)
		if(listFields!!.size>0)
			receiver.drawMenuFont(engine, 0, 7, "$nowMapID"+"/"+(listFields!!.size-1), menuCursor in 3..5)
		else
			receiver.drawMenuFont(engine, 0, 7, "NO MAPS", menuCursor in 3..5)
		if(menuCursor in 3..5)
			receiver.drawMenuFont(engine, 0, 8+menuCursor-3, "\u0082", EventReceiver.COLOR.RED)
		receiver.drawMenuFont(engine, 1, 8, "[SAVE]", menuCursor==3)
		receiver.drawMenuFont(engine, 1, 9, "[LOAD]", menuCursor==4)
		receiver.drawMenuFont(engine, 1, 10, "[DELETE]", menuCursor==5)

		receiver.drawMenuFont(engine, 0, 12, "MAP FILE", EventReceiver.COLOR.COBALT)
		receiver.drawMenuFont(engine, 0, 13, "$nowMapSetID/99", menuCursor in 6..7)
		if(menuCursor in 6..7)
			receiver.drawMenuFont(engine, 0, 14+menuCursor-6, "\u0082", EventReceiver.COLOR.RED)
		receiver.drawMenuFont(engine, 1, 14, "[WRITE]", menuCursor==6)
		receiver.drawMenuFont(engine, 1, 15, "[READ]", menuCursor==7)

		receiver.drawMenuFont(engine, 0, 19, "EXIT-> D+E", EventReceiver.COLOR.ORANGE)
	}

	/* fieldEdit screen */
	override fun renderFieldEdit(engine:GameEngine) {
		receiver.drawScoreFont(engine, 0, 2, "X POS", EventReceiver.COLOR.BLUE)
		receiver.drawScoreFont(engine, 0, 3, ""+engine.mapEditX)
		receiver.drawScoreFont(engine, 0, 4, "Y POS", EventReceiver.COLOR.BLUE)
		receiver.drawScoreFont(engine, 0, 5, ""+engine.mapEditY)
	}
}
