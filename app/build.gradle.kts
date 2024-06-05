import com.android.build.api.instrumentation.AsmClassVisitorFactory
import com.android.build.api.instrumentation.ClassContext
import com.android.build.api.instrumentation.ClassData
import com.android.build.api.instrumentation.FramesComputationMode
import com.android.build.api.instrumentation.InstrumentationParameters
import com.android.build.api.instrumentation.InstrumentationScope
import org.objectweb.asm.ClassVisitor

//https://developer.android.google.cn/studio/intro/studio-config?hl=zh-cn
plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("kapt")
    alias(wings.plugins.android)
    id("surgery")
}
// ./gradlew transformDebugClassesWithAsm
//apply<ExamplePlugin>()

android {
    namespace = "spark.surgery"
    defaultConfig {
        javaCompileOptions {
            annotationProcessorOptions {
                arguments["AROUTER_MODULE_NAME"] = project.getName()
            }
        }
    }
    buildTypes {
        viewBinding {
            enable = true
        }
    }
}
interface ParametersImpl : InstrumentationParameters {
    @get:Input
    val intValue: Property<Int>

    @get:Internal
    val listOfStrings: ListProperty<String>
}

abstract class LogAsmClassVisitorFactory : AsmClassVisitorFactory<ParametersImpl> {

    override fun createClassVisitor(classContext: ClassContext, nextClassVisitor: ClassVisitor): ClassVisitor {
        return nextClassVisitor
    }

    override fun isInstrumentable(classData: ClassData): Boolean {
        //https://cs.android.com/android-studio/platform/tools/base/+/mirror-goog-studio-main:build-system/gradle-core/src/main/java/com/android/build/gradle/internal/instrumentation/AsmInstrumentationManager.kt;l=65?q=AsmInstrumentationManager&sq=
        println("xxx >> ${classData.className}")
        return false
    }
}

androidComponents {
    onVariants { variants ->
        variants.instrumentation.setAsmFramesComputationMode(FramesComputationMode.COPY_FRAMES)
        variants.instrumentation.transformClassesWith(
            LogAsmClassVisitorFactory::class.java,
            InstrumentationScope.ALL
        ) { params ->
            // parameters configuration
            params.intValue.set(1)
            params.listOfStrings.set(listOf("a", "b"))
        }
    }
}
//https://github.com/gradle/kotlin-dsl-samples
dependencies {
//    implementation("androidx.core:core-ktx:1.10.1")
//    implementation("androidx.appcompat:appcompat:1.6.1")
//    implementation("com.google.android.material:material:1.9.0")
//    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
//    implementation("androidx.navigation:navigation-fragment-ktx:2.6.0")
//    implementation("androidx.navigation:navigation-ui-ktx:2.6.0")
    implementation("com.alibaba:arouter-api:1.5.2")
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    kapt("com.alibaba:arouter-compiler:1.5.2")
}

