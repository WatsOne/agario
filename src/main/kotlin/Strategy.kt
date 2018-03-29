import mu.KLogging
import org.json.JSONObject
import kotlin.math.hypot
import kotlin.math.min
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
                    emptyPoint = Utils.rotatingPoints(data.me[0].x, data.me[0].y, data.me[0].r, world).shuffled()[0]
                }

                if (!data.food.isEmpty()) {
                    logger.trace { "$tick CALCULATE" }
                    emptyPoint = null
                    println(onTick(data, world))
                }

                if (emptyPoint != null) {
                    logger.trace { "$tick EMPTY GO: $emptyPoint" }
                    println(JSONObject(mapOf("X" to emptyPoint.first, "Y" to emptyPoint.second)))

                    if (Utils.dist(data.me[0].x, data.me[0].y, emptyPoint.first, emptyPoint.second) < data.me[0].r / 2) {
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

        val total = mutableMapOf<Pair<Float, Float>, Int>()
        Utils.rotatingPoints(data.me[0].x, data.me[0].y, data.me[0].r, world).forEach { d ->
            val testPlayer = TestPlayer(data.me[0])
            val testFoods = data.food.map { TestFood(it) }
            var eat = 0
            (1..(4*data.me[0].r.toInt() + 10)).forEach {
                Utils.applyDirect(d.first, d.second, testPlayer, world)
                testFoods.forEach { f ->
                    if (!f.eaten && Utils.canEat(testPlayer, f)) {
                        eat++
                        f.eaten = true
                        testPlayer.m++
                        testPlayer.r = 2 * sqrt(testPlayer.m)
                    }
                }
                Utils.move(testPlayer, world)
            }
            total[d] = eat
        }

        val maxTotal = total.maxBy { it.value }

        val xMax = maxTotal?.key?.first ?: 0f
        val yMax = maxTotal?.key?.second ?: 0f

        logger.trace { "$tick MAX: $maxTotal" }

        return JSONObject(mapOf("X" to xMax, "Y" to yMax))
    }
}