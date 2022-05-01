package sparkj

import com.google.auto.service.AutoService
import org.objectweb.asm.tree.ClassNode
import ospl.surgery.api.ClassTreeDoctor
import ospl.surgery.api.FilterAction
import ospl.surgery.doctors.tree.TryFinallyDoctor
import ospl.surgery.doctors.tryfinally.TryFinally
import ospl.surgery.doctors.tryfinally.actions.MethodTrace
import java.io.File

/**
 * @author yun.
 * @date 2022/4/30
 * @des [一句话描述]
 * @since [https://github.com/mychoices]
 * <p><a href="https://github.com/mychoices">github</a>
 */
@AutoService(ClassTreeDoctor::class)
class Trace: TryFinallyDoctor() {
    override fun configMethodActions(): List<TryFinally> {
        return listOf(MethodTrace())
    }
}
