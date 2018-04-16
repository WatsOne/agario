import kotlin.math.*

object Utils {

    fun rotate(x: Float, y: Float, r: Float, length: Float, angle: Float, world: World, useCut: Boolean): Pair<Float, Float> {
        val tX = 0
        val cosA = cos(angle + PI.toFloat()/2)
        val sinA = sin(angle + PI.toFloat()/2)
        val rotateX = (tX*cosA + length*sinA + x)
        val rotateY = (-tX*sinA + y - length*cosA)

        return if (useCut) {
            Pair(max(r, min(world.width.toFloat() - r, rotateX)), max(r, min(world.height.toFloat() - r, rotateY)))
        } else {
            Pair(max(0f, min(world.width.toFloat(), rotateX)), max(0f, min(world.height.toFloat(), rotateY)))
        }
    }

    fun rotatingPoints(player: Me, world: World, useCut: Boolean): List<Pair<Float, Float>> {
        return rotatingPoints(player, world, 4*player.r + 10, (180 * (15 / player.r)).toInt(), useCut)
    }

    fun rotatingPoints(player: Me, length: Float, world: World, useCut: Boolean): List<Pair<Float, Float>> {
        return rotatingPoints(player, world, length, (180 * (15 / player.r)).toInt(), useCut)
    }

    private fun rotatingPoints(player: Me, world: World, length: Float, rotateCount: Int, useCut: Boolean): List<Pair<Float, Float>> {
        val step = 2*PI.toFloat() / rotateCount
        val startAngle = getAngle(player.sx, player.sy)
        val points = mutableListOf<Pair<Float, Float>>()

        (1..rotateCount).forEach {
            points.add(Utils.rotate(player.x, player.y, player.r, length,startAngle + (step*it), world, useCut))
        }
        return points
    }

    fun dist(player: TestPlayer, target: TestPlayer): Float {
        return dist(player.x, player.y, target.x, target.y)
    }

    fun dist(player: Circle, target: TestPlayer): Float {
        return dist(player.x, player.y, target.x, target.y)
    }

    fun dist(player: TestPlayer, target: Circle): Float {
        return dist(player.x, player.y, target.x, target.y)
    }

    fun dist(player: Circle, target: Circle): Float {
        return dist(player.x, player.y, target.x, target.y)
    }

    fun dist(xFrom: Float, yFrom: Float, xTarget: Float, yTarget: Float): Float {
        val dx = xTarget - xFrom
        val dy = yTarget - yFrom
        return sqrt(dx*dx + dy*dy)
    }

    fun qDist(xFrom: Float, yFrom: Float, xTarget: Float, yTarget: Float): Float {
        val dx = xTarget - xFrom
        val dy = yTarget - yFrom
        return dx*dx + dy*dy
    }

    fun dist(xFrom: Int, yFrom: Int, xTarget: Int, yTarget: Int): Float {
        val dx = xTarget - xFrom
        val dy = yTarget - yFrom
        return sqrt((dx*dx + dy*dy).toFloat())
    }

    fun getDistToEnemies(fragments: List<Me>, enemies: List<Enemy>): Map<String, List<Pair<String, Float>>> {
        val result = mutableMapOf<String, MutableList<Pair<String, Float>>>()
        fragments.forEach { result[it.id] = mutableListOf() }

        fragments.forEach { f ->
            enemies.filter { canEatPotential(it, f) }.forEach {
                result[f.id]!!.add(Pair(it.id, dist(f, it)))
            }
        }

        return result
    }

    fun getDistToEnemiesTest(fragments: List<TestPlayer>, enemies: List<TestPlayer>): MutableMap<String, Float> {
        val result = mutableMapOf<String, Float>()

        fragments.forEach { f ->
            enemies.forEach {
                result[f.id + it.id] = dist(f, it)
            }
        }

        return result
    }

    fun getNearestMeFoodPair(players: List<Me>, targets: List<Enemy>): Pair<Me, Enemy>? {
        var minDist = 10000f
        var nearPlayer = players[0]
        var nearTarget = targets[0]

        players.forEach { p ->
            targets.filter { Utils.canEatPotentialForHunting(p, it) }.forEach {
                val dist = dist(p, it)
                if (dist < minDist) {
                    nearPlayer = p
                    nearTarget = it
                    minDist = dist
                }
            }
        }

        return if (minDist < 10000f) Pair(nearPlayer, nearTarget) else null
    }

    fun canEatPotentialForHunting(player: Circle, food: Circle): Boolean {
        return player.m > food.m * (MASS_EAT_FACTOR + 0.10f)
    }

    fun canEatPotentialForHunting(player: TestPlayer, food: Circle): Boolean {
        return player.m > food.m * (MASS_EAT_FACTOR + 0.20f)
    }

    fun canEatPotential(player: Circle, food: Circle): Boolean {
        return player.m > food.m * MASS_EAT_FACTOR
    }

    fun canEatPotential(player: TestPlayer, food: Circle): Boolean {
        return player.m > food.m * MASS_EAT_FACTOR
    }

    fun canEatPotential(player: Circle, food: TestPlayer): Boolean {
        return player.m > food.m * MASS_EAT_FACTOR
    }

    fun canEatPotential(player: TestPlayer, food: TestPlayer): Boolean {
        return player.m > food.m * MASS_EAT_FACTOR
    }

    fun canEat(player: Circle, food: Circle): Boolean {
        return canEat(player.x, player.y, player.r, player.m, food.x, food.y, food.r, food.m)
    }

    fun canEat(player: TestPlayer, food: TestFood): Boolean {
        return canEat(player.x, player.y, player.r, player.m, food.x, food.y, food.r, food.m)
    }

    fun canEat(player: Circle, food: TestPlayer): Boolean {
        return canEat(player.x, player.y, player.r, player.m, food.x, food.y, food.r, food.m)
    }

    fun canEat(player: TestPlayer, food: TestPlayer): Boolean {
        return canEat(player.x, player.y, player.r, player.m, food.x, food.y, food.r, food.m)
    }

    private fun canEat(playerX: Float, playerY: Float, playerR: Float, playerM: Float, foodX: Float, foodY: Float, foodR: Float, foodM: Float): Boolean {
        if (playerM > foodM * MASS_EAT_FACTOR) {
            val dist = dist(playerX, playerY, foodX, foodY)
            if (dist - foodR + (foodR * 2) * DIAM_EAT_FACTOR < playerR) {
                return true
            }
        }

        return false
    }

    fun canSplit(player: Me, fragmentCount: Int, world: World): Boolean {
        if (fragmentCount + 1 <= world.maxFragment) {
            if (player.m > MIN_SPLIT_MASS) {
                return true
            }
        }

        return false
    }

    private fun applyViscosity(player: TestPlayer, maxSpeed: Float, world: World) {
        if (player.speed - world.viscosity > maxSpeed) {
            player.speed -= world.viscosity
        } else {
            player.speed = maxSpeed
            player.isFast = false
        }
    }

    fun move(player: TestPlayer, world: World) {
        val rb = player.x + player.r
        val lb = player.x - player.r
        val db = player.y + player.r
        val ub = player.y - player.r

        val dx = player.speed * cos(player.angle)
        val dy = player.speed * sin(player.angle)

        if (rb + dx < world.width && lb + dx > 0) {
            player.x += dx
        } else {
            player.x = max(player.r, min(world.width.toFloat() - player.r, player.x + dx))
            player.speed = abs(dy)
            player.angle = if (dy >= 0) (PI.toFloat() / 2.0f) else (-PI.toFloat() / 2.0f)
        }

        if (db + dy < world.height && ub + dy > 0) {
            player.y += dy
        } else {
            player.y = max(player.r, min(world.height.toFloat() - player.r, player.y + dy))
            player.speed = abs(dx)
            player.angle = if (dx >= 0) 0f else PI.toFloat()
        }

        if (player.isFast) {
            val maxSpeed = world.speed / sqrt(player.m)
            applyViscosity(player, maxSpeed, world)
        }

        if (player.ttf > 0) player.ttf--
    }

    fun applyDirect(x: Float, y: Float, player: TestPlayer, world: World) {
        if (player.isFast) return

        val maxSpeed = world.speed / sqrt(player.m)
        val dy = y - player.y
        val dx = x - player.x
        val dist = sqrt(dx*dx + dy*dy)
        val ny = if (dist > 0) (dy / dist) else 0f
        val nx = if (dist > 0) (dx / dist) else 0f

        val speedX = if (player.angle.isNaN() && player.speed.isNaN()) {
            player.sx
        } else {
            player.speed * cos(player.angle)
        }

        val speedY = if (player.angle.isNaN() && player.speed.isNaN()) {
            player.sy
        } else {
            player.speed * sin(player.angle)
        }

        player.sx += (nx*maxSpeed - speedX) * world.inertion / player.m
        player.sy += (ny*maxSpeed - speedY) * world.inertion / player.m

        player.angle = getAngle(player.sx, player.sy)
        player.speed = min(maxSpeed, sqrt(player.sx*player.sx + player.sy*player.sy))
    }

    fun getAngle(sx: Float, sy: Float): Float {
        return atan2(sy, sx)
    }

    fun calculateCollision(player: TestPlayer, fragment: TestPlayer) {
        if (player.isFast || fragment.isFast) return

        val dist = dist(player, fragment)
        if (dist >= player.r + fragment.r) {
            return
        }

        var collisionVectorX = player.x - fragment.x
        var collisionVectorY = player.y - fragment.y

        val vectorLen = sqrt(collisionVectorX * collisionVectorX + collisionVectorY * collisionVectorY)
        if (vectorLen < 0.00000001) return
        collisionVectorX /= vectorLen
        collisionVectorY /= vectorLen

        var collisionForce = 1.0f - dist / (player.r + fragment.r)
        collisionForce *= collisionForce
        collisionForce *= COLLISION_POWER

        val sumMass = player.m + fragment.m

        //for us
        val currPart = fragment.m / sumMass
        var dx = player.speed * cos(player.angle)
        var dy = player.speed * sin(player.angle)
        dx += collisionForce * currPart * collisionVectorX
        dy += collisionForce * currPart * collisionVectorY
        player.speed = sqrt(dx*dx + dy*dy)
        player.angle = atan2(dy, dx)

        //for fragment
        val fragmentPart = player.m / sumMass
        dx = fragment.speed * cos(fragment.angle)
        dy = fragment.speed * sin(fragment.angle)
        dx -= collisionForce * fragmentPart * collisionVectorX
        dy -= collisionForce * fragmentPart * collisionVectorY
        fragment.speed = sqrt(dx*dx + dy*dy)
        fragment.angle = atan2(dy, dx)
    }

    fun getPotentialVictims(data: Data): List<Pair<String, String>> {
        val res = mutableListOf<Pair<String, String>>()
        data.me.forEach { m ->
            data.enemy.forEach {
                if (canEatPotential(m, it)) {
                    res.add(Pair(m.id, it.id))
                }
            }
        }

        return res
    }

    fun getPotentialHunters(data: Data): List<Pair<String, String>> {
        val res = mutableListOf<Pair<String, String>>()

        data.enemy.forEach { e ->
            data.me.forEach {
                if (canEatPotential(e, it)) {
                    res.add(Pair(e.id, it.id))
                }
            }
        }

        return res
    }

    fun getPotentialVictims(me: List<TestPlayer>, enemies: List<Enemy>): List<Pair<String, String>> {
        val res = mutableListOf<Pair<String, String>>()
        me.forEach { m ->
            enemies.forEach {
                if (canEatPotentialForHunting(m, it)) {
                    res.add(Pair(m.id!!, it.id))
                }
            }
        }

        return res
    }

    fun getPotentialVictimsTestTest(me: List<TestPlayer>, enemies: List<TestPlayer>): List<Pair<String, String>> {
        val res = mutableListOf<Pair<String, String>>()
        me.forEach { m ->
            enemies.forEach {
                if (canEatPotential(m, it)) {
                    res.add(Pair(m.id!!, it.id!!))
                }
            }
        }

        return res
    }

    fun getPotentialHunters(me: List<TestPlayer>, enemies: List<Enemy>): List<Pair<String, String>> {
        val res = mutableListOf<Pair<String, String>>()

        enemies.forEach { e ->
            me.forEach {
                if (canEatPotential(e, it)) {
                    res.add(Pair(e.id, it.id!!))
                }
            }
        }

        return res
    }

    fun getPotentialHuntersTest(me: List<Me>, enemies: List<Enemy>): List<Pair<String, String>> {
        val res = mutableListOf<Pair<String, String>>()

        enemies.forEach { e ->
            me.forEach {
                if (canEatPotential(e, it)) {
                    res.add(Pair(e.id, it.id))
                }
            }
        }

        return res
    }

    fun getPotentialHuntersTestTest(me: List<TestPlayer>, enemies: List<TestPlayer>): List<Pair<String, String>> {
        val res = mutableListOf<Pair<String, String>>()

        enemies.forEach { e ->
            me.forEach {
                if (canEatPotential(e, it)) {
                    res.add(Pair(e.id!!, it.id!!))
                }
            }
        }

        return res
    }

    fun rotatingPointsForSimulation(playerForAngle: Me, world: World, rotateCount: Int): List<Pair<Float, Float>> {
        val step = 2*PI.toFloat() / rotateCount
        val startAngle = getAngle(playerForAngle.sx, playerForAngle.sy)
        val points = mutableListOf<Pair<Float, Float>>()
        val rotateLength = dist(world.width / 2, world.height / 2, world.width, world.height)

        (1..rotateCount).forEach {
            points.add(rotateForSimulation(world.width / 2.toFloat(), world.height / 2.toFloat(),rotateLength,startAngle + (step*it), world))
        }
        return points
    }

    private fun rotateForSimulation(x: Float, y: Float, length: Float, angle: Float, world: World): Pair<Float, Float> {
        val tX = 0
        val cosA = cos(angle + PI.toFloat()/2)
        val sinA = sin(angle + PI.toFloat()/2)
        val rotateX = (tX*cosA + length*sinA + x)
        val rotateY = (-tX*sinA + y - length*cosA)

        return Pair(max(0f, min(world.width.toFloat(), rotateX)), max(0f, min(world.height.toFloat(), rotateY)))
    }

    fun canSplit(fragments: List<Me>, world: World): Boolean {
        if (fragments.size <= world.maxFragment) {
            return fragments.any { it.m > MIN_SPLIT_MASS }
        }

        return false
    }

    fun split(me: TestPlayer): List<TestPlayer> {
        return mutableListOf(
                TestPlayer(me.id + me.id, me.x, me.y, 2 * sqrt(me.m / 2), me.m / 2, me.sx, me.sy, SPLIT_START_SPEED, Utils.getAngle(me.sx, me.sy), true),
                TestPlayer(me.id, me.x, me.y, 2 * sqrt(me.m / 2), me.m / 2, me.sx, me.sy)
        )
    }

    fun canShrink(player: TestPlayer): Boolean {
        return player.m > MIN_SHRINK_MASS
    }

    fun shrink(player: TestPlayer) {
        player.m -= ((player.m - MIN_SHRINK_MASS) * SHRINK_FACTOR)
        player.r = 2 * sqrt(player.m)
    }

    fun canFuse(player: TestPlayer, fragment: TestPlayer): Boolean {
        if (player.ttf != 0 || fragment.ttf != 0) return false

        val dist = dist(player, fragment)
        val nr = player.r + fragment.r

        return dist <= nr
    }

    fun fusion(player: TestPlayer, fragment: TestPlayer) {
        val sumMass = player.m + fragment.m
        val fragInfluence = fragment.m / sumMass
        val playerInfluence = player.m / sumMass

        player.x = player.x * playerInfluence + fragment.x * fragInfluence
        player.y = player.y * playerInfluence + fragment.y * fragInfluence

        player.sx = player.sx * playerInfluence + fragment.sx * fragInfluence
        player.sy = player.sy * playerInfluence + fragment.sy * fragInfluence

        player.speed = sqrt(player.sx * player.sx + player.sy * player.sy)
        player.angle = atan2(player.sy, player.sx)

        player.m += fragment.m
    }

    fun updateByMass(player: TestPlayer, world: World) {
        player.r = 2 * sqrt(player.m)
        val newSpeed = world.speed / sqrt(player.m)
        if (player.speed > newSpeed && !player.isFast) {
            player.speed = newSpeed
        }

        if (player.x - player.r < 0) {
            player.x += (player.r - player.x)
        }

        if (player.y - player.r < 0) {
            player.y += (player.r - player.y)
        }

        if (player.x + player.r > world.width) {
            player.x -= (player.r + player.x - world.width)
        }

        if (player.y + player.r > world.height) {
            player.y -= (player.r + player.y - world.height)
        }
    }
}