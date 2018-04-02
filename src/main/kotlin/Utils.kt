import kotlin.math.*

object Utils {

    fun rotate(x: Float, y: Float, r: Float, length: Float, angle: Float, world: World): Pair<Float, Float> {
        val tX = 0
        val cosA = cos(angle + PI.toFloat()/2)
        val sinA = sin(angle + PI.toFloat()/2)
        val rotateX = (tX*cosA + length*sinA + x)
        val rotateY = (-tX*sinA + y - length*cosA)
        return Pair(max(r, min(world.width.toFloat() - r, rotateX)), max(r, min(world.height.toFloat() - r, rotateY)))
    }

    fun rotatingPoints(player: Me, world: World): List<Pair<Float, Float>> {
        val rotateCount = 180 * (15 / player.r)
        val step = 2*PI.toFloat() / rotateCount
        val startAngle = getAngle(player.sx, player.sy)
        val points = mutableListOf<Pair<Float, Float>>()

        (1..rotateCount.toInt()).forEach {
            points.add(Utils.rotate(player.x, player.y, player.r, 4*player.r + 10, startAngle + (step*it), world))
        }
        return points
    }

    fun dist(player: TestPlayer, target: TestPlayer): Float {
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

    fun getNearestMeEnemyPair(players: List<Me>, targets: List<Enemy>): Pair<Me, Enemy> {
        var minDist = 10000f
        var nearPlayer = players[0]
        var nearTarget = targets[0]

        players.forEach { p ->
            targets.forEach {
                val dist = dist(p, it)
                if (dist < minDist) {
                    nearPlayer = p
                    nearTarget = it
                    minDist = dist
                }
            }
        }

        return Pair(nearPlayer, nearTarget)
    }

    fun canEatPotential(player: Circle, food: Circle): Boolean {
        return player.m > food.m * MASS_EAT_FACTOR
    }

    fun canEat(player: Circle, food: Circle): Boolean {
        return canEat(player.x, player.y, player.r, player.m, food.x, food.y, food.r, food.m)
    }

    fun canEat(player: TestPlayer, food: TestFood): Boolean {
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
            player.x = max(0f, min(world.width.toFloat(), player.x + dx))
            player.speed = abs(dy)
            player.angle = if (dy >= 0) (PI.toFloat() / 2.0f) else (-PI.toFloat() / 2.0f)
        }

        if (db + dy < world.height && ub + dy > 0) {
            player.y += dy
        } else {
            player.y = max(0f, min(world.height.toFloat(), player.y + dy))
            player.speed = abs(dx)
            player.angle = if (dx >= 0) 0f else PI.toFloat()
        }
    }

    fun applyDirect(x: Float, y: Float, player: TestPlayer, world: World) {
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
}