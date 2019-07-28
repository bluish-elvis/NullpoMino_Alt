package mu.nu.nullpo.tool.airankstool

import java.io.*

object AIRanksValue {

	/** @param args arguments
	 */
	@JvmStatic
	fun main(args:Array<String>) {
		val fis:FileInputStream
		val `in`:ObjectInputStream
		var ranks:Ranks
		val inputFile = "${AIRanksConstants.RANKSAI_DIR}ranks20"

		if(inputFile.trim {it<=' '}.isEmpty())
			ranks = Ranks(4, 9)
		else
			try {
				fis = FileInputStream(inputFile)
				`in` = ObjectInputStream(fis)
				ranks = `in`.readObject() as Ranks
				`in`.close()
				val surface1 = intArrayOf(0, 1, 1, -1, -1, 1, -3, -2)
				val surface2 = intArrayOf(0, 1, 1, -1, -1, 4, -4, 2)

				val rank1 = ranks.getRankValue(ranks.encode(surface1))
				val rank2 = ranks.getRankValue(ranks.encode(surface2))
				println(rank1)
				println(rank2)
			} catch(e:FileNotFoundException) {
				ranks = Ranks(4, 9)
			} catch(e:IOException) {
				// TODO Auto-generated catch block
				e.printStackTrace()
			} catch(e:ClassNotFoundException) {
				e.printStackTrace()
			}

	}

}
