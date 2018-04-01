import org.json.JSONObject

class Data {
    val me = mutableListOf<Me>()
    val food = mutableListOf<Food>()
    val ejection = mutableListOf<Ejection>()
    val virus = mutableListOf<Virus>()
    val enemy = mutableListOf<Enemy>()

    private fun clear() {
        me.clear()
        food.clear()
        ejection.clear()
        virus.clear()
        enemy.clear()
    }

    fun parse(data: JSONObject, world: World) {

        clear()

        val meArray = data.getJSONArray("Mine")
        val objectArray = data.getJSONArray("Objects")

        for (i in 0 until meArray.length()) {
            val mine = meArray.getJSONObject(i)
            me.add(Me(
                    id = mine.getString("Id"),
                    x = mine.getFloat("X"),
                    y = mine.getFloat("Y"),
                    r = mine.getFloat("R"),
                    m = mine.getFloat("M"),
                    sx = mine.getFloat("SX"),
                    sy = mine.getFloat("SY")))
        }

        for (i in 0 until objectArray.length()) {
            val obj = objectArray.getJSONObject(i)
            when (obj.getString("T")) {
                "F" -> food.add(Food(obj.getFloat("X"), obj.getFloat("Y")))
                "E" -> ejection.add(Ejection(obj.getFloat("X"), obj.getFloat("Y")))
                "V" -> virus.add(Virus(
                        id = obj.getString("Id"),
                        x = obj.getFloat("X"),
                        y = obj.getFloat("Y"),
                        m = obj.getFloat("M"),
                        r = world.virusRadius))
                "P" -> enemy.add(Enemy(
                        id = obj.getString("Id"),
                        x = obj.getFloat("X"),
                        y = obj.getFloat("Y"),
                        r = obj.getFloat("R"),
                        m = obj.getFloat("M")))
            }
        }
    }

    override fun toString(): String {
        return "Data(me=$me, food=$food, ejection=$ejection, virus=$virus, enemy=$enemy)"
    }


}