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
                    x = mine.getDouble("X").toFloat(),
                    y = mine.getDouble("Y").toFloat(),
                    r = mine.getDouble("R").toFloat(),
                    m = mine.getDouble("M").toFloat(),
                    sx = mine.getDouble("SX").toFloat(),
                    sy = mine.getDouble("SY").toFloat()))
        }

        for (i in 0 until objectArray.length()) {
            val obj = objectArray.getJSONObject(i)
            when (obj.getString("T")) {
                "F" -> food.add(Food(obj.getDouble("X").toFloat(), obj.getDouble("Y").toFloat(), world.foodMass))
                "E" -> ejection.add(Ejection(obj.getDouble("X").toFloat(), obj.getDouble("Y").toFloat()))
                "V" -> virus.add(Virus(
                        id = obj.getString("Id"),
                        x = obj.getDouble("X").toFloat(),
                        y = obj.getDouble("Y").toFloat(),
                        m = obj.getDouble("M").toFloat(),
                        r = world.virusRadius))
                "P" -> enemy.add(Enemy(
                        id = obj.getString("Id"),
                        x = obj.getDouble("X").toFloat(),
                        y = obj.getDouble("Y").toFloat(),
                        r = obj.getDouble("R").toFloat(),
                        m = obj.getDouble("M").toFloat()))
            }
        }
    }

    override fun toString(): String {
        return "Data(me=$me, food=$food, ejection=$ejection, virus=$virus, enemy=$enemy)"
    }


}