package osp.surgery.api

/**
 * Surgery框架异常基类
 * 提供更精确的异常分类，便于错误处理和调试
 * 
 * @author yun.
 * @date 2024
 */
sealed class SurgeryException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause) {
    
    /**
     * 字节码读取异常
     */
    class BytecodeReadException(
        className: String,
        cause: Throwable? = null
    ) : SurgeryException(
        "Failed to read bytecode for class: $className",
        cause
    )
    
    /**
     * 字节码写入异常
     */
    class BytecodeWriteException(
        className: String,
        cause: Throwable? = null
    ) : SurgeryException(
        "Failed to write bytecode for class: $className",
        cause
    )
    
    /**
     * Doctor执行异常
     */
    class DoctorExecutionException(
        doctorName: String,
        className: String,
        cause: Throwable? = null
    ) : SurgeryException(
        "Doctor '$doctorName' failed to process class: $className",
        cause
    )
    
    /**
     * 类加载异常
     */
    class ClassLoadException(
        className: String,
        cause: Throwable? = null
    ) : SurgeryException(
        "Failed to load class: $className",
        cause
    )
    
    /**
     * JAR处理异常
     */
    class JarProcessingException(
        jarPath: String,
        cause: Throwable? = null
    ) : SurgeryException(
        "Failed to process JAR file: $jarPath",
        cause
    )
}

