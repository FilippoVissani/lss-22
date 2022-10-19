import io.kotest.assertions.fail
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldNotBe
import org.gradle.internal.impldep.org.junit.rules.TemporaryFolder
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import java.io.File
import java.nio.file.Path
import kotlin.io.path.createTempDirectory

class TestGreetingPlugin : StringSpec (
    {
        "the plugin should load" {
            val buildFile = """
                plugins {
                    id("it.unibo.greetings.pluto")
                }
                
                greetings {
                    "pippo".asGreeting()
                }
            """.trimIndent()
            val directory: File = createTempDirectory().toFile()
            directory.mkdirs()
            with(File(directory, "build.gradle.kts")) {
                writeText(buildFile);
            }
            val result = GradleRunner.create()
                .withProjectDir(directory)
                .withPluginClasspath()
                .withArguments("greet")
                .build()
            println(result.output)
            result.tasks.forEach { it.outcome shouldNotBe TaskOutcome.FAILED }
        }
    }
)