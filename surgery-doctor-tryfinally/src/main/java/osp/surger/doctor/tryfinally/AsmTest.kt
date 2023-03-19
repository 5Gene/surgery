package osp.surger.doctor.tryfinally

/**
 * @author yun.
 * @date 2022/5/28
 * @des [一句话描述]
 * @since [https://github.com/5hmlA]
 * <p><a href="https://github.com/5hmlA">github</a>
 */
class AsmTest {
    fun asmTest(){
        try {
            println("======== content =========")
        } catch (e: Exception) {
            println("====== catch =====")
        }
    }
    fun asmTest222(){
        try {
            println("======== content =========")
        } catch (e: Exception) {
            println("====== catch =====")
        }
        println("====== next ====")
    }
}