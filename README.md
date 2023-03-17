## 怎么用

- 1 配置仓库地址

   ```kotlin
       maven {
           url = uri("https://maven.pkg.github.com/5hmlA/sparkj")
           credentials {
               username = "\u005a\u0075\u0059\u0075\u006e"
               password = "\u0067\u0068\u0070\u005f\u0031\u0063\u0062\u0064\u004d\u004a\u0073\u005a\u0042\u0057\u0033\u006a\u0077\u0057\u0053\u004b\u006e\u0066\u0037\u0042\u0053\u0069\u0044\u006d\u0061\u0030\u0066\u0044\u0048\u0076\u0031\u007a\u0059\u0050\u0073\u0044"
           }
       }
   ```

- 2 添加依赖

   ```kotlin
   buildscript {
       dependencies {
           //只实现了plugin和transform分发没做任何修改 需要在buildSrc下自己实现
           classpath "ospl.sparkj.plugin:surgery:1.0.4"
           //1:Arouter，2:为每个方法首尾插入trace
           classpath "ospl.sparkj.plugin:surgery-doctor-tryfinally:1.0"
           classpath "ospl.sparkj.plugin:surgery-doctor-arouter:1.0"
       }
   }
   //使用插件
   plugins {
       id 'surgery'
   }
   ```

- 3 按需修改字节码

  - 1 创建buildSrc

    build.gradle.kts如下配置

    ```kotlin
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
        maven {
            url = uri("https://maven.pkg.github.com/5hmlA/sparkj")
            credentials {
                username = "ZuYun"
                password = "\u0067\u0068\u0070\u005f\u0031\u0063\u0062\u0064\u004d\u004a\u0073\u005a\u0042\u0057\u0033\u006a\u0077\u0057\u0053\u004b\u006e\u0066\u0037\u0042\u0053\u0069\u0044\u006d\u0061\u0030\u0066\u0044\u0048\u0076\u0031\u007a\u0059\u0050\u0073\u0044"
            }
        }
    }
    
    plugins {
        kotlin("jvm") version "1.6.21"
        id("com.google.devtools.ksp") version "1.6.21-1.0.5"
    }
    
    
    dependencies{
        ksp("dev.zacsweers.autoservice:auto-service-ksp:+")
        implementation("com.google.auto.service:auto-service-annotations:1.0.1")
        implementation(kotlin("stdlib-jdk8"))
        implementation("org.ow2.asm:asm:9.3")
        implementation("org.ow2.asm:asm-analysis:9.3")
        implementation("org.ow2.asm:asm-commons:9.3")
        implementation("org.ow2.asm:asm-tree:9.3")
        implementation("org.ow2.asm:asm-util:9.3")
        implementation("commons-io:commons-io:2.10.0")
    
        implementation("ospl.sparkj.plugin:surgery-api:1.0.4")
        implementation("ospl.sparkj.plugin:surgery-helper:1.0.3")
//        implementation("ospl.sparkj.plugin:surgery-doctor-arouter:1.0")
//        implementation("ospl.sparkj.plugin:surgery-doctor-tryfinally:1.0")
    }
    ```

  - 为方法首尾插入代码继承TryFinallyDoctor，会把原方法插入到try finally代码块内

    ```kotlin
    //通过AutoService通知上层此实现
    @AutoService(ClassTreeDoctor::class) 
    class Trace: TryFinallyDoctor() {
        override fun configMethodActions(): List<TryFinally> {
            //为方法添加 trace
            return listOf(MethodTrace())
            //为方法添加耗时输出
            return listOf(MethodTimeLog())
        }
    }
    ```

### FilterAction

```kotlin
 enum class FilterAction {
     noTransform//不处理, transformLast//最后处理, transformNow//现在处理, transformNowLast//现在处理最后也处理
 }
```

#### ClassTreeDoctor

```kotlin
class Test: ClassTreeDoctor() {

    override fun surgeryPrepare() {
    }
    
    override fun filterByJar(jar: File): FilterAction {
       //通过jar来判断是否需要对其处理
    }

    override fun filterByClassName(file: File, className: () -> String): FilterAction {
        //通过file来判断是否需要对其处理
    }

    override fun surgery(classNode: ClassNode): ClassNode {
        //通过asm来完成字节码处理
    }

    override fun surgeryOver() {
    }
}
```

