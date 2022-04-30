package ospl.surgery.doctors

//import com.google.auto.service.AutoService
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.commons.AdviceAdapter
import ospl.surgery.doctors.tryfinally.TryFinally
import ospl.surgery.doctors.tryfinally.TryFinallyVisitorDoctor
import ospl.surgery.doctors.tryfinally.actions.MethodTimeLog
import ospl.surgery.doctors.tryfinally.actions.MethodTrace

//@AutoService(ClassVisitorDoctor::class)
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