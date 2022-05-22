package sparkj.doctor

import com.google.auto.service.AutoService
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.commons.AdviceAdapter
import ospl.surgery.api.ClassTreeDoctor
import ospl.surgery.api.FilterAction
import java.io.File
import ospl.surgery.helper.*

/**
 * @author yun.
 * @date 2022/5/22
 * @des [一句话描述]
 * @since [https://github.com/mychoices]
 * <p><a href="https://github.com/mychoices">github</a>
 */
@AutoService(ClassTreeDoctor::class)
class SubTrace: ospl.surgery.doctors.tree.TryFinallyDoctor() {

    override fun filterByClassName(file: File, className: () -> String): FilterAction {
        if (file.isJar()){
            println("============= ${className()}")
        }
        return FilterAction.noTransform
    }
}