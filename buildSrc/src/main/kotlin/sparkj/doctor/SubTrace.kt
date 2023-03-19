package sparkj.doctor

import com.google.auto.service.AutoService
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.commons.AdviceAdapter
import osp.surgery.api.ClassTreeDoctor
import osp.surgery.api.ClassVisitorDoctor
import osp.surgery.api.FilterAction
import java.io.File
import osp.surgery.helper.*
import osp.surgery.doctors.TryFinallyDoctor

/**
 * @author yun.
 * @date 2022/5/22
 * @des [一句话描述]
 * @since [https://github.com/5hmlA]
 * <p><a href="https://github.com/5hmlA">github</a>
 */
@AutoService(ClassVisitorDoctor::class)
class SubTrace: TryFinallyDoctor() {

    override fun filterByClassName(file: File, className: () -> String): FilterAction {
        if (file.isJar()){
            println("============= ${className()}")
        }
        return FilterAction.noTransform
    }
}