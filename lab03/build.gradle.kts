import org.gradle.internal.jvm.Jvm
import org.gradle.kotlin.dsl.registering

tasks.register("pluto") {
    println("configuration, phase 1")
    doLast {
        println("configured first")
    }
}

tasks.named("pluto") {
    println("configuration, phase 2")
    doFirst {
        println("configured last")
    }
}

open class Pino : Exec() {
    override fun exec() {
        println("'so Pino")
        super.exec()
        println("ero Pino")
    }
}

object Util {
    fun File.allThoseWithExtension(extension: String) = walkTopDown()
        .filter { it.extension == extension }
        .map { it.absolutePath }
        .toList()
        .toTypedArray()
}

val compileClasspath by configurations.creating
val runtimeClasspath by configurations.creating
dependencies {
    with(Util) {
        project.file("lib").allThoseWithExtension("jar").forEach {
            compileClasspath(files(it))
            runtimeClasspath(files(it))
        }
    }
    runtimeClasspath(files(File(project.buildDir, "bin")))
}

open class Javac @Inject constructor() : Exec() {
    init {
        group = "Custom LSS plugin"
        description = "Compiles all java files in src"
        val javacPath = Jvm.current().javacExecutable.absolutePath
        with(Util) {
            val allJavaFiles = project.file("src").allThoseWithExtension("java")
            if (allJavaFiles.isNotEmpty()) {
                val classpath = project.configurations.named("compileClasspath").get().resolve()
                    .map { it.absolutePath }
                    .joinToString(File.pathSeparator)
                val destination = File(project.buildDir, "bin")
                commandLine(
                    javacPath,
                    "-d",
                    destination.absolutePath,
                    "-cp",
                    classpath,
                    *allJavaFiles,
                )
                doFirst {
                    destination.mkdirs()
                    check(destination.exists() && destination.isDirectory)
                }
            }
        }
    }
}

tasks.register<Pino>("printJavaVersion") {
    val javaExecutable = Jvm.current().javaExecutable
    commandLine(javaExecutable.absolutePath, "-version")
    doLast {
        println("^^ Java version up there ^^")
    }
    doFirst {
        println("Ready to print the version of Java.")
    }
}

val compile by tasks.registering(Javac::class)

with(Util) {
    project.file("src")
        .allThoseWithExtension("java")
        .map { File(it).readText() }
        .filter { it.contains(Regex("\\s*public\\s+static\\s+void\\s+main")) }
        .mapNotNull {
            Regex("class\\s+(\\w+)\\s*\\{").find(it)?.groups?.get(1)?.value
        }
        .forEach { mainClass ->
            tasks.register<Exec>("run$mainClass") {
                this.dependsOn(compile)
                val java = Jvm.current().javaExecutable.absolutePath
                val classpath = runtimeClasspath.resolve()
                    .map { it.absolutePath }
                    .joinToString(File.pathSeparator)
                commandLine(java, "-cp", classpath, mainClass)
            }
        }
}

tasks.register("clean") {
    doLast {
        println("Deleting folders")
        buildDir.deleteRecursively()
    }
    compile.get().mustRunAfter(this)
}

subprojects {
    tasks.register("everywhere") {
        group = "Tasks in all projects"
        description = "prints the project name"
        doLast {
            println(project.name)
        }
    }
}

allprojects {
}

