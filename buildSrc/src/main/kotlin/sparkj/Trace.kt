package sparkj

import ospl.surgery.doctors.tree.TryFinallyDoctor
import ospl.surgery.doctors.tryfinally.TryFinally
import ospl.surgery.doctors.tryfinally.actions.MethodTrace

/**
 * @author yun.
 * @date 2022/4/30
 * @des [一句话描述]
 * @since [https://github.com/mychoices]
 * <p><a href="https://github.com/mychoices">github</a>
 */
class Trace: TryFinallyDoctor() {
    override fun configMethodActions(): List<TryFinally> {
        return listOf(MethodTrace())
    }
}