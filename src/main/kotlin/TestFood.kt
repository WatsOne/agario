class TestFood(val x: Float, val y: Float, val r: Float, val m: Float, var eaten: Boolean = false) {
    constructor(food: Food) : this(food.x, food.y, food.r, food.m)
}