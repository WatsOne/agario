class TestPlayer(val id: String?, var x: Float, var y: Float, var r: Float, var m: Float, var sx: Float, var sy: Float, var speed: Float = Float.NaN, var angle: Float = Float.NaN, var isFast: Boolean = false) {
    constructor(testPlayer: TestPlayer) : this(null, testPlayer.x, testPlayer.y, testPlayer.r, testPlayer.m, testPlayer.sx, testPlayer.sy)
    constructor(me: Me) : this(me.id, me.x, me.y, me.r, me.m, me.sx, me.sy)
    constructor(enemy: Enemy, enemySx: Float, enemySy: Float) : this(enemy.id, enemy.x, enemy.y, enemy.r, enemy.m, enemySx, enemySy)

    override fun toString(): String {
        return "TestPlayer(id=$id, x=$x, y=$y, r=$r, m=$m, sx=$sx, sy=$sy, speed=$speed, angle=$angle, isFast=$isFast)"
    }


}