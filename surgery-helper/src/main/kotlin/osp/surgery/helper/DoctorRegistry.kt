package osp.surgery.helper

import osp.surgery.api.ClassDoctor
import java.util.ServiceLoader

/**
 * Doctor注册表，缓存ServiceLoader结果，避免重复扫描classpath
 * 
 * @author yun.
 * @date 2024
 */
object DoctorRegistry {
    
    private val cache = mutableMapOf<Class<*>, List<ClassDoctor>>()
    
    /**
     * 加载指定类型的Doctor实现
     * 使用缓存避免重复ServiceLoader扫描
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : ClassDoctor> loadDoctors(clazz: Class<T>): List<T> {
        return cache.computeIfAbsent(clazz) {
            ServiceLoader.load(clazz).iterator().asSequence().toList()
        } as List<T>
    }
    
    /**
     * 清除缓存
     * 在surgeryPrepare时调用，确保每次构建都重新加载
     */
    fun clearCache() {
        cache.clear()
    }
    
}

/**
 * 过滤重复的Doctor实现
 * 如果一个Doctor的实现类已经在superclass列表中出现，则过滤掉
 */
fun <T : ClassDoctor> Sequence<T>.filterDuplicates(): Sequence<T> {
    val supers = mutableSetOf<String>()
    return onEach { 
        supers.add(it.javaClass.superclass.name) 
    }.filter { doctor ->
        !supers.contains(doctor.javaClass.name)
    }
}

