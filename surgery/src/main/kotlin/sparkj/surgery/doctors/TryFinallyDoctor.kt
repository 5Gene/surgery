package sparkj.surgery.doctors

import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.commons.AdviceAdapter
import sparkj.surgery.doctors.tryfinally.TryFinally
import sparkj.surgery.doctors.tryfinally.TryFinallyVisitorDoctor
import sparkj.surgery.doctors.tryfinally.actions.MethodTimeLog
import sparkj.surgery.doctors.tryfinally.actions.MethodTrace
import sparkj.surgery.more.*

open class TryFinallyDoctor : TryFinallyVisitorDoctor() {

    private val enterActions: List<TryFinally> by lazy {
        configMethodActions()
    }

    private val exitActions: List<TryFinally> by lazy {
        enterActions.reversed()
    }

    open fun configMethodActions(): List<TryFinally> = listOf<TryFinally>(MethodTrace(), MethodTimeLog())

    override fun onMethodEnter(className: String, methodName: String, mv: MethodVisitor, adapter: AdviceAdapter) {
        enterActions.forEach {
            it.methodEnter(className, methodName, mv, adapter)
        }
    }

    override fun onMethodReturn(className: String, methodName: String, mv: MethodVisitor, adapter: AdviceAdapter) {
        onMethodExit(className, methodName, mv, adapter)
    }

    override fun onMethodError(className: String, methodName: String, mv: MethodVisitor, adapter: AdviceAdapter) {
        onMethodExit(className, methodName, mv, adapter)
    }

    override fun onMethodExit(className: String, methodName: String, mv: MethodVisitor, adapter: AdviceAdapter) {
        exitActions.forEach {
            it.methodExit(className, methodName, mv, adapter)
        }
    }
}