package osp.surgery.helper

/**
 * 统一配置管理类
 * 集中管理所有硬编码的配置和常量
 * 
 * @author yun.
 * @date 2024
 */
object SurgeryConfig {
    
    /**
     * 需要跳过的JAR文件名称模式
     */
    val skipJarNames = setOf(
        "R.jar",
        "0.jar"
    )
    
    /**
     * 需要跳过的JAR文件前缀模式
     */
    val skipJarPrefixes = setOf(
        "jetified-",
        "core-",
        "drawerlayout-",
        "vectordrawable-",
        "dynamicanimation-",
        "localbroadcastmanager-",
        "navigation-",
        "viewpager-",
        "coordinatorlayout-",
        "legacy-",
        "loader-",
        "customview-",
        "recyclerview-",
        "swiperefreshlayout-",
        "transition-",
        "cardview-",
        "slidingpanelayout-",
        "versionedparcelable-",
        "constraintlayout-",
        "material-",
        "appcompat-",
        "annotation-",
        "lifecycle-",
        "print-",
        "collection-",
        "cursoradapter-",
        "media-",
        "asynclayoutinflater-",
        "fragment-",
        "interpolator-"
    )
    
    /**
     * 判断是否应该跳过JAR文件
     */
    fun shouldSkipJar(fileName: String): Boolean {
        return skipJarNames.contains(fileName) || 
               skipJarPrefixes.any { fileName.startsWith(it) }
    }
    
    /**
     * 判断是否应该跳过class文件
     */
    fun shouldSkipClass(fileName: String): Boolean {
        return !fileName.endsWith(".class") ||
                fileName == "BuildConfig.class" ||
                fileName.endsWith("Binding.class") ||
                fileName.startsWith("R$") ||
                fileName.startsWith("R.")
    }
    
    /**
     * 判断是否是模块JAR
     */
    fun isModuleJar(fileName: String): Boolean {
        return fileName == "classes.jar"
    }
    
    /**
     * 流式处理的阈值（字节），大于此值的文件使用流式处理
     */
    const val STREAM_THRESHOLD = 1024 * 1024 // 1MB
    
    /**
     * 缓冲区大小
     */
    const val BUFFER_SIZE = 8192 * 4 // 32KB
    
    /**
     * ThreadLocal池的最大大小
     */
    const val THREAD_LOCAL_POOL_SIZE = 10
}

