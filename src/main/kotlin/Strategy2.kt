//import mu.KLogging
import org.json.JSONObject
import kotlin.math.PI
import kotlin.math.sqrt

class Strategy2 {
    //    companion object: KLogging()
    var tick = 1

    fun go() {
        val config = JSONObject(readLine())
        val world = World(config)
        val data = Data()

        var idlePoint: Pair<Float, Float>? = null
        val prevEnemyPositions = mutableMapOf<String, Pair<Float, Float>?>()
        val enemySpeedVectors = mutableMapOf<String, Pair<Float, Float>?>()

        while (true) {
            val tickData = JSONObject(readLine())
            data.parse(tickData, world)

            if (data.me.isEmpty()) return

            val meMap = data.me.associateBy({ it.id }, { it })

            //очищаем вектора скоростей у противков пропавших из виду
            enemySpeedVectors.filter { !data.enemy.map { e -> e.id }.contains(it.key) }.forEach { enemySpeedVectors[it.key] = null }
            prevEnemyPositions.filter { !data.enemy.map { e -> e.id }.contains(it.key) }.forEach { prevEnemyPositions[it.key] = null }

            if (!data.enemy.isEmpty()) {
                val enemyMap = data.enemy.associateBy({ it.id }, { it })

                //обновляем вектора скоростей врагов
                data.enemy.forEach {
                    val prevEnemyPos = prevEnemyPositions[it.id]
                    if (prevEnemyPos == null) {
                        prevEnemyPositions[it.id] = Pair(it.x, it.y)
                    } else {
                        enemySpeedVectors[it.id] = Pair(it.x - prevEnemyPos.first, it.y - prevEnemyPos.second)
                        prevEnemyPositions[it.id] = Pair(it.x, it.y)
                    }
                }
            }

            if (data.food.isEmpty()) {
                idlePoint = getIdlePoint(data, world, idlePoint)
                println(JSONObject(mapOf("X" to idlePoint.first, "Y" to idlePoint.second)))
            } else {
                idlePoint = null
                val doEatPosition = doEat(data, world)
                println(JSONObject(mapOf("X" to doEatPosition.first, "Y" to doEatPosition.second)))
            }

//            println(JSONObject(mapOf("X" to world.width / 2, "Y" to world.height / 2)))
            tick++
        }
    }

    private fun getIdlePoint(data: Data, world: World, idlePoint: Pair<Float, Float>?): Pair<Float, Float> {
        val player = getLeaderFragment(data.me)

        return if (idlePoint == null) {
            getNewIdleRotatePoint(player, world, PI.toFloat() / 5)
        } else {
            val dist = Utils.dist(player.x, player.y, idlePoint.first, idlePoint.second)
            if (dist < player.r) {
                val newIdle = getNewIdleRotatePoint(player, world, PI.toFloat() / 5)
                if (idlePoint.first == newIdle.first && idlePoint.second == newIdle.second) {
                    getNewIdleRotatePoint(player, world, PI.toFloat())
                } else {
                    newIdle
                }
            } else {
                idlePoint
            }
        }
    }

    private fun getNewIdleRotatePoint(player: Me, world: World, angle: Float): Pair<Float, Float> {
        val currentAngle = Utils.getAngle(player.sx, player.sy)
        return Utils.rotate(player.x, player.y, player.r, 1000f, currentAngle + angle, world, true)
    }

    private fun doEat(data: Data, world: World): Pair<Float, Float> {

        val start = System.currentTimeMillis()
        val total = mutableMapOf<Pair<Float, Float>, Float>()
        var oper = 0

        val me = getLeaderFragment(data.me)
        val testFoods = data.food.map { TestFood(it) }

        Utils.rotatingPoints(me, 1000f, world, true).forEach { d ->
            val testPlayer = TestPlayer(me)
            testFoods.forEach { it.eaten = false }

            var eaten = testFoods.size
            var eat = 0

            var ppt = 0f
            var tick = 0

            while (Utils.dist(testPlayer.x, testPlayer.y, d.first, d.second) > testPlayer.r && eaten > 0 && tick <= 30) {

                Utils.applyDirect(d.first, d.second, testPlayer, world)
                Utils.move(testPlayer, world)

                testFoods.forEach { f ->
                    if (!f.eaten && Utils.canEat(testPlayer, f)) {
                        eat++
                        eaten--
                        f.eaten = true
                        testPlayer.m += world.foodMass
                        testPlayer.r = 2 * sqrt(testPlayer.m)
                        ppt = eat / tick.toFloat()
                    }
                }

                tick++
                oper++
            }
            total[d] = ppt
        }

//        logger.trace { "$tick calc: ${System.currentTimeMillis() - start} ms; oper: $oper. Radius: ${data.me[0].r}" }

        val max = getMaxScore(total)
        return Pair(max.first, max.second)
    }

    private fun getMaxScore(scoreMap: Map<Pair<Float, Float>, Float>): Pair<Float, Float> =
            scoreMap.maxBy { it.value }?.key ?: Pair(0f, 0f)

    private fun getLeaderFragment(me: List<Me>): Me {
        return me.minBy { it.id.toFloat() } ?: me[0]
    }
}