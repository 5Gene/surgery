package transform

class RemoveAllMethod(val aa: Int = 0, val bb: String = "", val cc: Double = 1.0) {
    init {
        println(aa.toString() + bb + cc)
    }
}

class RemoveAllMethod2(val aa: Int = 1, val bb: String, val cc: Double, a: List<String>) {
    init {
        println(aa.toString() + bb + cc)
    }
}