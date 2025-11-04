# Surgery 🏥 - Android 字节码插桩框架

> 一个基于 ASM 的 Android 字节码插桩框架，采用 Gradle Plugin 机制，支持通过 SPI（Service Provider Interface）动态加载 Doctor 实现类进行字节码转换。

## 📖 项目简介

Surgery 是一个高性能、易扩展的 Android 字节码插桩框架。它提供了灵活的字节码操作能力，让你可以在编译期对 Android 应用的字节码进行修改，实现各种功能增强，如：

- 🎯 方法追踪和性能监控
- 🔗 路由框架自动注册（如 ARouter）
- 🛡️ 方法异常捕获和日志记录
- 📊 埋点统计和性能分析
- ✨ 其他自定义字节码增强

## ✨ 核心优点

### 🚀 高性能
- **并发处理**：使用并发集合和协程支持，大幅提升处理速度
- **智能缓存**：ServiceLoader 结果缓存，避免重复扫描 classpath
- **优化 I/O**：减少重复读取，使用缓冲流提升 I/O 性能
- **线程安全**：使用 ThreadLocal 和并发集合，确保线程安全

### 🔧 易扩展
- **SPI 机制**：通过 AutoService 自动发现和加载 Doctor 实现
- **多种模式**：支持 ClassTree 和 ClassVisitor 两种字节码操作模式
- **灵活过滤**：支持按 JAR 和类名进行精确过滤
- **延迟处理**：支持立即处理和延迟处理两种策略

### 💪 高质量
- **异常处理**：完善的异常处理机制，单点失败不影响整体流程
- **资源管理**：正确清理 ThreadLocal，防止内存泄漏
- **统一配置**：集中管理配置，易于维护和扩展
- **代码质量**：清晰的代码结构，良好的可维护性

### 📦 模块化设计
- **职责分离**：API、Plugin、Helper、Doctors 模块清晰分离
- **低耦合**：模块间通过接口通信，易于测试和维护
- **可插拔**：Doctor 实现可独立开发和部署

## 🏗️ 代码层级结构

```
surgery-master/
├── surgery-api/                    # API 层：定义核心接口和抽象类
│   └── src/main/kotlin/osp/surgery/api/
│       ├── Doctor.kt               # Doctor 接口和抽象类（ClassTreeDoctor、ClassVisitorDoctor）
│       ├── FilterAction.kt        # 过滤动作枚举（noTransform、transformNow、transformLast）
│       ├── ClassSurgery.kt         # 字节码处理接口
│       ├── Priority.kt             # 优先级注解
│       └── SurgeryException.kt     # 异常处理类
│
├── surgery/                         # 插件层：Gradle Plugin 实现
│   └── src/main/kotlin/osp/surgery/plugin/
│       ├── Hospital.kt             # Gradle Plugin 入口
│       ├── SurgeryTask.kt          # 同步任务实现
│       ├── SurgeryTask2.kt         # 协程任务实现
│       └── plan/
│           ├── ProjectSurgery.kt   # 项目级别字节码处理
│           ├── ClassBytesSurgery.kt # 类级别字节码处理（ClassTreeSurgery、ClassVisitorSurgery）
│           └── FilterResult.kt    # 过滤结果数据类
│
├── surgery-helper/                  # 工具层：工具类和辅助函数
│   └── src/main/kotlin/osp/surgery/helper/
│       ├── SurgeryConfig.kt        # 统一配置管理
│       ├── DoctorRegistry.kt     # Doctor 注册表（ServiceLoader 缓存）
│       ├── FileHelper.kt          # 文件操作工具
│       └── StrHelper.kt           # 字符串工具
│
├── surgery-doctors/                 # Doctor 实现示例
│   └── src/main/kotlin/osp/surgery/doctors/
│       ├── tree/
│       │   ├── ArouteDoctor.kt    # ARouter 路由注册实现
│       │   └── TryFinallyDoctor.kt # Try-Finally 模式实现
│       └── tryfinally/
│           └── TryFinallyVisitorDoctor.kt
│
└── app/                             # 示例应用
```

### 核心组件说明

#### 1. Doctor（医生）
负责具体的字节码转换逻辑，有两种实现方式：
- **ClassTreeDoctor**：使用 ClassNode 树结构，适合复杂的字节码操作
- **ClassVisitorDoctor**：使用 ClassVisitor 访问者模式，性能更高

#### 2. Surgery（手术）
负责协调和管理 Doctor，包括：
- **ProjectSurgery**：项目级别的字节码处理协调
- **ClassBytesSurgery**：类级别的字节码处理实现

#### 3. Filter（过滤）
支持精细化的过滤策略：
- **filterByJar**：按 JAR 文件过滤
- **filterByClassName**：按类名过滤
- **FilterAction**：处理时机（立即处理、延迟处理、不处理）

## 🚀 快速开始

### 1. 添加依赖

在项目根目录的 `build.gradle.kts` 中：

```kotlin
buildscript {
    repositories {
        mavenCentral()
        google()
        maven {
            url = uri("https://maven.pkg.github.com/5hmlA/sparkj")
            credentials {
                username = "ZuYun"
                password = "your_token_here"
            }
        }
    }
    dependencies {
        // Surgery 核心插件
        classpath("ospl.sparkj.plugin:surgery:1.0.4")
    }
}

// 在 app 模块的 build.gradle.kts 中应用插件
plugins {
    id("com.android.application")
    id("surgery")
}
```

### 2. 配置 buildSrc

创建 `buildSrc` 目录并配置：

**buildSrc/build.gradle.kts:**
```kotlin
repositories {
    gradlePluginPortal()
    google()
    mavenCentral()
    maven {
        url = uri("https://maven.pkg.github.com/5hmlA/sparkj")
        credentials {
            username = "ZuYun"
            password = "your_token_here"
        }
    }
}

plugins {
    kotlin("jvm") version "1.6.21"
    id("com.google.devtools.ksp") version "1.6.21-1.0.5"
}

dependencies {
    ksp("dev.zacsweers.autoservice:auto-service-ksp:+")
    implementation("com.google.auto.service:auto-service-annotations:1.0.1")
    implementation(kotlin("stdlib-jdk8"))
    implementation("org.ow2.asm:asm:9.3")
    implementation("org.ow2.asm:asm-commons:9.3")
    implementation("org.ow2.asm:asm-tree:9.3")
    implementation("ospl.sparkj.plugin:surgery-api:1.0.4")
    implementation("ospl.sparkj.plugin:surgery-helper:1.0.3")
}
```

## 📝 使用示例

### 示例 1：方法追踪（Method Trace）

为所有方法添加进入和退出日志：

```kotlin
// buildSrc/src/main/kotlin/com/example/MethodTraceDoctor.kt
package com.example

import com.google.auto.service.AutoService
import org.objectweb.asm.tree.*
import osp.surgery.api.ClassTreeDoctor
import osp.surgery.api.FilterAction
import java.io.File

@AutoService(ClassTreeDoctor::class)
class MethodTraceDoctor : ClassTreeDoctor() {

    override fun surgeryPrepare() {
        println("$tag === MethodTraceDoctor 初始化 ===")
    }

    override fun filterByJar(jar: File): FilterAction {
        // 只处理应用自己的代码，跳过第三方库
        return if (jar.name == "classes.jar") {
            FilterAction.transformNow
        } else {
            FilterAction.noTransform
        }
    }

    override fun filterByClassName(fileName: String, compileClassName: String): FilterAction {
        // 跳过系统生成的类
        return if (compileClassName.startsWith("android/") ||
                   compileClassName.startsWith("kotlin/") ||
                   fileName == "BuildConfig.class" ||
                   fileName.startsWith("R\$")) {
            FilterAction.noTransform
        } else {
            FilterAction.transformNow
        }
    }

    override fun surgery(classNode: ClassNode): ClassNode {
        classNode.methods.forEach { method ->
            // 跳过构造函数和静态初始化块
            if (method.name == "<init>" || method.name == "<clinit>") {
                return@forEach
            }

            // 在方法开始处插入日志
            val startInsns = method.instructions
            startInsns.insert(createLogInsn("Enter: ${method.name}"))
            
            // 在所有返回处插入日志
            var insn = startInsns.first
            while (insn != null) {
                val next = insn.next
                if (insn is InsnNode && 
                    (insn.opcode in listOf(RETURN, IRETURN, ARETURN, LRETURN, FRETURN, DRETURN))) {
                    startInsns.insertBefore(insn, createLogInsn("Exit: ${method.name}"))
                }
                insn = next
            }
        }
        return classNode
    }

    private fun createLogInsn(message: String): InsnList {
        val list = InsnList()
        list.add(FieldInsnNode(
            GETSTATIC, 
            "java/lang/System", 
            "out", 
            "Ljava/io/PrintStream;"
        ))
        list.add(LdcInsnNode("[$tag] $message"))
        list.add(MethodInsnNode(
            INVOKEVIRTUAL,
            "java/io/PrintStream",
            "println",
            "(Ljava/lang/String;)V",
            false
        ))
        return list
    }

    override fun surgeryOver() {
        println("$tag === MethodTraceDoctor 完成 ===")
    }
}
```

### 示例 2：性能监控（Method Timing）

使用方法执行耗时统计：

```kotlin
// buildSrc/src/main/kotlin/com/example/MethodTimingDoctor.kt
package com.example

import com.google.auto.service.AutoService
import org.objectweb.asm.commons.AdviceAdapter
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import osp.surgery.api.ClassVisitorDoctor
import osp.surgery.api.FilterAction
import java.io.File

@AutoService(ClassVisitorDoctor::class)
class MethodTimingDoctor : ClassVisitorDoctor() {

    override fun surgeryPrepare() {
        println("$tag === MethodTimingDoctor 初始化 ===")
    }

    override fun filterByJar(jar: File): FilterAction {
        return if (jar.name == "classes.jar") {
            FilterAction.transformNow
        } else {
            FilterAction.noTransform
        }
    }

    override fun filterByClassName(fileName: String, compileClassName: String): FilterAction {
        // 只监控特定包下的方法
        return if (compileClassName.startsWith("com/example/app/")) {
            FilterAction.transformNow
        } else {
            FilterAction.noTransform
        }
    }

    override fun surgery(visitor: ClassVisitor): ClassVisitor {
        return object : ClassVisitor(Opcodes.ASM9, visitor) {
            override fun visitMethod(
                access: Int,
                name: String?,
                descriptor: String?,
                signature: String?,
                exceptions: Array<out String>?
            ): MethodVisitor {
                val mv = cv.visitMethod(access, name, descriptor, signature, exceptions)
                
                // 跳过构造函数和静态初始化块
                if (name == "<init>" || name == "<clinit>") {
                    return mv
                }

                return object : AdviceAdapter(Opcodes.ASM9, mv, access, name, descriptor) {
                    override fun onMethodEnter() {
                        // 记录开始时间
                        mv.visitMethodInsn(
                            INVOKESTATIC,
                            "java/lang/System",
                            "currentTimeMillis",
                            "()J",
                            false
                        )
                        mv.visitVarInsn(LSTORE, maxLocals)
                    }

                    override fun onMethodExit(opcode: Int) {
                        // 计算耗时并输出
                        mv.visitFieldInsn(
                            GETSTATIC,
                            "java/lang/System",
                            "out",
                            "Ljava/io/PrintStream;"
                        )
                        mv.visitMethodInsn(
                            INVOKESTATIC,
                            "java/lang/System",
                            "currentTimeMillis",
                            "()J",
                            false
                        )
                        mv.visitVarInsn(LLOAD, maxLocals)
                        mv.visitInsn(LSUB)
                        mv.visitMethodInsn(
                            INVOKEVIRTUAL,
                            "java/io/PrintStream",
                            "println",
                            "(J)V",
                            false
                    }
                }
            }
        }
    }

    override fun surgeryOver() {
        println("$tag === MethodTimingDoctor 完成 ===")
    }
}
```

### 示例 3：使用 TryFinallyDoctor（简化版）

Surgery 提供了 `TryFinallyDoctor` 基类，可以更方便地实现方法前后插桩：

```kotlin
// buildSrc/src/main/kotlin/com/example/MyTraceDoctor.kt
package com.example

import com.google.auto.service.AutoService
import osp.surgery.doctors.TryFinallyDoctor
import osp.surgery.doctors.tryfinally.TryFinally
import osp.surgery.doctors.tryfinally.actions.MethodTrace
import osp.surgery.doctors.tryfinally.actions.MethodTimeLog

@AutoService(ClassVisitorDoctor::class)
class MyTraceDoctor : TryFinallyDoctor() {
    
    override fun configMethodActions(): List<TryFinally> {
        // 返回需要执行的动作列表
        // MethodTrace: 输出方法名
        // MethodTimeLog: 输出执行时间
        return listOf(
            MethodTrace(),
            MethodTimeLog()
        )
    }
}
```

### 示例 4：ARouter 路由注册

自动注册 ARouter 路由：

```kotlin
// buildSrc/src/main/kotlin/com/example/ARouterDoctor.kt
package com.example

import com.google.auto.service.AutoService
import org.objectweb.asm.tree.*
import osp.surgery.api.ClassTreeDoctor
import osp.surgery.api.FilterAction
import java.io.File

@AutoService(ClassTreeDoctor::class)
class ARouterDoctor : ClassTreeDoctor() {

    private val routes = mutableSetOf<String>()

    override fun surgeryPrepare() {
        routes.clear()
    }

    override fun filterByJar(jar: File): FilterAction {
        // 只处理包含 ARouter 相关类的 JAR
        return if (jar.name.contains("arouter") || jar.name == "classes.jar") {
            FilterAction.transformNow
        } else {
            FilterAction.noTransform
        }
    }

    override fun filterByClassName(fileName: String, compileClassName: String): FilterAction {
        // 处理 ARouter$$Routes 类
        if (fileName.startsWith("ARouter$$")) {
            return FilterAction.transformLast // 延迟处理，确保所有路由类都已收集
        }
        return FilterAction.noTransform
    }

    override fun surgery(classNode: ClassNode): ClassNode {
        // 收集路由信息
        if (classNode.name.startsWith("com/alibaba/android/arouter/routes/")) {
            classNode.methods.forEach { method ->
                if (method.name == "loadInto") {
                    // 解析并收集路由信息
                    // ... 具体实现
                }
            }
        }
        return classNode
    }

    override fun surgeryOver() {
        // 所有路由收集完成后，生成汇总类
        println("$tag === 收集到 ${routes.size} 个路由 ===")
        // 生成 LogisticsCenter 的 loadRouterMap 方法
    }
}
```

## 🔍 FilterAction 详解

```kotlin
enum class FilterAction {
    noTransform,      // 不处理，直接跳过
    transformNow,     // 立即处理
    transformLast     // 延迟处理，在所有类处理完成后统一处理
}
```

**使用场景：**
- `transformNow`：适合大多数场景，可以立即修改字节码
- `transformLast`：适合需要收集所有信息后再处理的场景（如路由注册）
- `noTransform`：跳过不需要处理的类，提升性能

## 🛠️ 高级用法

### 自定义配置

通过 `SurgeryConfig` 可以自定义配置：

```kotlin
// 在 Doctor 实现中使用
if (SurgeryConfig.shouldSkipJar(jar.name)) {
    return FilterAction.noTransform
}

if (SurgeryConfig.shouldSkipClass(fileName)) {
    return FilterAction.noTransform
}
```

### 异常处理

使用 `SurgeryException` 进行精确的异常处理：

```kotlin
try {
    // 字节码处理
} catch (e: IOException) {
    throw SurgeryException.BytecodeReadException(className, e)
} catch (e: Exception) {
    throw SurgeryException.DoctorExecutionException(doctorName, className, e)
}
```

## 📊 性能优化

Surgery 框架经过精心优化，具有以下性能特点：

- ✅ 并发处理：使用并发集合和协程，充分利用多核 CPU
- ✅ 智能缓存：ServiceLoader 结果缓存，避免重复扫描
- ✅ 内存安全：正确清理 ThreadLocal，防止内存泄漏
- ✅ I/O 优化：减少重复读取，使用缓冲流

## 🤝 贡献

欢迎提交 Issue 和 Pull Request！

## 📄 许可证

[添加您的许可证信息]

## 🔗 相关链接

- [ASM 官方文档](https://asm.ow2.io/)
- [Gradle Plugin 开发指南](https://docs.gradle.org/current/userguide/custom_plugins.html)
- [Service Provider Interface](https://docs.oracle.com/javase/tutorial/ext/basics/spi.html)

---

**Made with ❤️ by Surgery Team**
