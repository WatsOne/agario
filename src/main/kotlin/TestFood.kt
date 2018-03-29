class TestFood(val x: Float, val y: Float, var eaten: Boolean = false) {
    constructor(food: Food) : this(food.x, food.y)
}