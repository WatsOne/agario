import mu.KLogging
import org.json.JSONObject
import kotlin.math.sqrt

class Strategy {
    companion object: KLogging()
    var tick = 1

    fun go() {
        val config = JSONObject(readLine())
        val world = World(config)
        val data = Data()

        var emptyPoint: Pair<Float, Float>? = null

        while (true) {
            try {
                val tickData = JSONObject(readLine())
                data.parse(tickData)

                if (data.food.isEmpty() && emptyPoint == null) {
                    logger.trace { "$tick EMPTY INITIAL" }
                    emptyPoint = Utils.rotatingPoints(data.me[0], world).shuffled()[0]
                }

                if (!data.food.isEmpty()) {
                    logger.trace { "$tick CALCULATE" }
                    emptyPoint = null
                    println(onTick(data, world))
                }

                if (emptyPoint != null) {
                    logger.trace { "$tick EMPTY GO: $emptyPoint" }
                    println(JSONObject(mapOf("X" to emptyPoint.first, "Y" to emptyPoint.second)))

                    val dist = Utils.dist(data.me[0].x, data.me[0].y, emptyPoint.first, emptyPoint.second)
                    val rr = data.me[0].r * 1.6

                    if (dist < rr) {
                        emptyPoint = null
                    }
                }
                tick++
            } catch (ex: Exception) {
                logger.trace { "$ex" }
            }
        }
    }

    fun onTick(data: Data, world: World): JSONObject {

        val start = System.currentTimeMillis()

        val total = mutableMapOf<Pair<Float, Float>, Float>()
        Utils.rotatingPoints(data.me[0], world).forEach { d ->
            val testPlayer = TestPlayer(data.me[0])
            val testFoods = data.food.map { TestFood(it) }
            var eat = 0

            var ppt = 0f
            var tick = 0

            while (Utils.dist(testPlayer.x, testPlayer.y, d.first, d.second) > testPlayer.r) {
                Utils.applyDirect(d.first, d.second, testPlayer, world)
                testFoods.forEach { f ->
                    if (!f.eaten && Utils.canEat(testPlayer, f)) {
                        eat++
                        f.eaten = true
                        testPlayer.m++
                        testPlayer.r = 2 * sqrt(testPlayer.m)
                        ppt = eat / tick.toFloat()
                    }
                }
                Utils.move(testPlayer, world)
                tick++
            }
            total[d] = ppt
        }

        val maxTotal = total.maxBy { it.value }

        val xMax = maxTotal?.key?.first ?: 0f
        val yMax = maxTotal?.key?.second ?: 0f


        logger.trace { "$tick MAX: $maxTotal calc: ${System.currentTimeMillis() - start} ms. Radius: ${data.me[0].r}" }

        return JSONObject(mapOf("X" to xMax, "Y" to yMax))
    }
}