import org.json.JSONObject

fun main(args: Array<String>) {
    val world = World(JSONObject("{\"FOOD_MASS\":2.9655562695739772,\"GAME_HEIGHT\":990,\"GAME_TICKS\":7500,\"GAME_WIDTH\":990,\"INERTION_FACTOR\":18.5108745710798,\"MAX_FRAGS_CNT\":11,\"SPEED_FACTOR\":76.422059743979531,\"TICKS_TIL_FUSION\":463,\"VIRUS_RADIUS\":32.724595861119653,\"VIRUS_SPLIT_MASS\":86.328488327266058,\"VISCOSITY\":0.14508487492822297}\n"))
    val data = Data()
    data.parse(JSONObject("{\"Mine\":[{\"Id\":\"4.2\",\"M\":118.99157339508811,\"R\":21.816651749990246,\"SX\":-4.8037040616463926,\"SY\":1.90465717602548,\"TTF\":204,\"X\":80.508171764903835,\"Y\":850.02661920943524},{\"Id\":\"4.1\",\"M\":121.7835915388417,\"R\":22.071120636600373,\"SX\":-3.7020940079696092,\"SY\":3.0132642428656138,\"TTF\":204,\"X\":75.814500288835035,\"Y\":813.26471222874977}],\"Objects\":[{\"T\":\"F\",\"X\":52,\"Y\":848},{\"Id\":\"2.4\",\"M\":90.535995818058211,\"R\":19.030081010658702,\"T\":\"P\",\"X\":116.987560570225,\"Y\":934.33409598894707},{\"Id\":\"21\",\"M\":40,\"T\":\"V\",\"X\":308.44919172223933,\"Y\":67.449191722239306},{\"Id\":\"22\",\"M\":40,\"T\":\"V\",\"X\":681.55080827776067,\"Y\":67.449191722239306},{\"Id\":\"23\",\"M\":40,\"T\":\"V\",\"X\":681.55080827776067,\"Y\":922.55080827776067},{\"Id\":\"24\",\"M\":40,\"T\":\"V\",\"X\":308.44919172223933,\"Y\":922.55080827776067},{\"Id\":\"265\",\"M\":40,\"T\":\"V\",\"X\":164.44919172223931,\"Y\":120.44919172223931},{\"Id\":\"266\",\"M\":40,\"T\":\"V\",\"X\":825.55080827776067,\"Y\":120.44919172223931},{\"Id\":\"267\",\"M\":40,\"T\":\"V\",\"X\":825.55080827776067,\"Y\":869.55080827776067},{\"Id\":\"268\",\"M\":40,\"T\":\"V\",\"X\":164.44919172223931,\"Y\":869.55080827776067}]}\n" +
            "{\"X\":116.98756400000001,\"Y\":934.33410000000003}\n"), world)

    val enemyDist = Utils.getDistToEnemies(data.me, data.enemy)
    println(enemyDist)

    val testFragments = enemyDist.keys.map { getById(it, data.me) }.map { TestPlayer(it) }
    val testEnemies = enemyDist.values.flatten()
            .map { getById(it.first, data.enemy) }
            .map { TestPlayer(it, 0f, 0f) }

    val nearPairs = mutableListOf<String>()
    repeat(10, {
        testFragments.forEach {
            Utils.applyDirect(it.x + it.sx *2 , it.y + it.sy * 2, it, world)
            Utils.move(it, world)
        }
        testEnemies.forEach {
            Utils.applyDirect(it.x + it.sx *2 , it.y + it.sy * 2, it, world)
            Utils.move(it, world)
        }

        testFragments.forEach { f ->
            testEnemies.forEach {
                if (Utils.dist(f, it) < f.r + it.r) {
                    nearPairs.add(f.id + it.id)
                }
            }
        }
    })

    val newDist = Utils.getDistToEnemiesTest(testFragments, testEnemies)
    nearPairs.forEach { newDist[it] = -1000f }

    //считаем разницу дистанций и выбираем максимальную (при наличии)
    var maxDeltaDist = 0f
    var dangerPair: Pair<String, String>? = null

    enemyDist.forEach { d ->
        d.value.forEach {
            val deltaDist = it.second - (newDist[d.key + it.first] ?: it.second)
            if (deltaDist > maxDeltaDist) {
                maxDeltaDist = deltaDist
                dangerPair = Pair(d.key, it.first)
            }
        }
    }

    //если такая имеется то начинаем убегать
    if (maxDeltaDist > 0) {
        println(">>>>>>>>!!!")
    }

    val nearestPair = Utils.getNearestMeFoodPair(data.me, data.enemy)
    println(nearestPair)

}

private fun getById(id: String, me: List<Me>): Me {
    return me.find { it.id == id } ?: me[0]
}

private fun getById(id: String, enemies: List<Enemy>): Enemy {
    return enemies.find { it.id == id } ?: enemies[0]
}