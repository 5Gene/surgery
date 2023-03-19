package osp.surgery.api

/**
 * @author yun.
 * @date 2022/5/22
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Priority(val value: Int = 0)
