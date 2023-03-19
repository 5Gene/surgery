package osp.surgery.plugin.or

import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * @author yun.
 * @date 2021/9/2
 * @des [一句话描述]
 * @since [https://github.com/5hmlA]
 * <p><a href="https://github.com/5hmlA">github</a>
 */
class OperatingRoom {
    private val cpuCount = Runtime.getRuntime().availableProcessors()
    private val workExecutor: ExecutorService = Executors.newWorkStealingPool(cpuCount)

    private val features: MutableList<java.util.concurrent.Future<*>> = mutableListOf()

    fun submit(task: Runnable) {
//        task.run()
        val feature = workExecutor.submit(task)
        features.add(feature)
    }

    fun await() {
        while (features.size > 0) {
            features.removeFirst().get()
        }
    }
}