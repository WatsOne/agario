open class Circle(val x: Float, val y: Float, val r: Float, val m: Float)

class Me(val id: String, x: Float, y: Float, r: Float, m: Float, val sx: Float, val sy: Float, val ttf: Int) : Circle(x, y, r, m)
class Food(x: Float, y: Float, m: Float) : Circle(x, y, FOOD_RADIUS, m)
class Ejection(x: Float, y: Float) : Circle(x, y, EJECT_RADIUS, EJECT_MASS)
class Virus(val id: String, x: Float, y: Float, m: Float, r: Float) : Circle (x, y, r, m)
class Enemy(val id: String, x: Float, y: Float, r: Float, m: Float) : Circle(x, y, r, m)