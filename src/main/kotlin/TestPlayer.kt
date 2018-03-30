class TestPlayer(var x: Float, var y: Float, var r: Float, var m: Float, var sx: Float, var sy: Float, var speed: Float = Float.NaN, var angle: Float = Float.NaN) {
    constructor(me: Me) : this(me.x, me.y, me.r, me.m, me.sx, me.sy)

    override fun toString(): String {
        return "TestPlayer(x=$x, y=$y, r=$r, m=$m, sx=$sx, sy=$sy, speed=$speed, angle=$angle)"
    }


}