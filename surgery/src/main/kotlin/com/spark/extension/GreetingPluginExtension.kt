package com.spark.extension

import org.gradle.api.provider.Property

/**
 * @author yun.
 * @date 2021/7/19
 * @des [一句话描述]
 * @since [https://github.com/ZuYun]
 * <p><a href="https://github.com/ZuYun">github</a>
 */
abstract class GreetingPluginExtension {
    abstract val message: Property<String>

    init {
        message.convention("Hello from GreetingPlugin")
    }
}