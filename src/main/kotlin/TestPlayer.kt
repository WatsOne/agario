class TestPlayer(val id: String?, var x: Float, var y: Float, var r: Float, var m: Float, var sx: Float, var sy: Float, var speed: Float = Float.NaN, var angle: Float = Float.NaN, var isFast: Boolean = false, var needUpdateMass: Boolean = false, var ttf: Int = 50000, var isActual: Boolean = true) {
    constructor(testPlayer: TestPlayer) : this(id = testPlayer.id, x = testPlayer.x, y = testPlayer.y, r = testPlayer.r, m = testPlayer.m, sx = testPlayer.sx, sy = testPlayer.sy, ttf = testPlayer.ttf)
    constructor(me: Me) : this(id = me.id, x = me.x, y = me.y, r = me.r, m = me.m, sx = me.sx, sy = me.sy, ttf = me.ttf)
    constructor(enemy: Enemy, enemySx: Float, enemySy: Float) : this(enemy.id, enemy.x, enemy.y, enemy.r, enemy.m, enemySx, enemySy)

    override fun toString(): String {
        return "TestPlayer(id=$id, x=$x, y=$y, r=$r, m=$m, sx=$sx, sy=$sy, speed=$speed, angle=$angle, isFast=$isFast)"
    }

    fun copy(): TestPlayer {
        return TestPlayer(id, x, y, r, m, sx, sy, speed, angle, isFast, needUpdateMass, ttf)
    }
}