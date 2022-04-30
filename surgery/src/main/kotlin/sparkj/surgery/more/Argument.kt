package sparkj.surgery.more

import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project


open class Argument(val project: Project) {
    var message = "def message from plugin"
    var buildTimeCheck = false
    var sender = "def sender from plugin"
    var innerExt = AppleArg()
    var buildTypes: NamedDomainObjectContainer<MyBuildType> = project.container(MyBuildType::class.java)

    override fun toString(): String {
        return "Argument(project=$project, message='$message', sender='$sender') > inner $innerExt"
    }

    //推荐方式
    //创建嵌套 extensions 2
    //创建内部Extension，名称为方法名 phone
    fun phone(action: Action<AppleArg>) {
        action.execute(innerExt)
    }

    //让其支持 Gradle DSL 语法
    open fun myBuildTypes(action: Action<NamedDomainObjectContainer<MyBuildType>>) {
        action.execute(this.buildTypes)
    }

    fun read(){
        println("message = $message")
        println("sender = $sender")
        println("innerExt = $innerExt")
        buildTypes.all {
            println("===> ${it.name}")
        }
        buildTypes.forEach {
            println(it.name)
        }
    }
}

//构造函数必须有一个 name 参数
//构造函数必须有一个 name 参数
//构造函数必须有一个 name 参数
open class MyBuildType constructor(var name: String = "buildType") {
    var version: Int = 10
}

open class AppleArg {
    var name = "apple phone"
    var phone = 13285960627


    override fun toString(): String {
        return "AppleArg(name='$name', phone=$phone)"
    }
}