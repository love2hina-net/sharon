import com.google.common.base.Joiner
import com.google.common.collect.Iterables
import org.gradle.api.internal.plugins.DefaultTemplateBasedStartScriptGenerator
import org.gradle.api.internal.plugins.UnixStartScriptGenerator
import java.io.Writer

buildscript {
    val versions by extra { mapOf<String, Any>(
        "kotlin" to "1.6.0",
        "java" to JavaVersion.VERSION_11,
        "target_jvm" to "11",
        "junit" to "5.8.2",
        "mockito" to "4.1.0",
        "doma" to "2.50.0",
        "h2" to "2.0.204"
    )}
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath("com.h2database:h2:${versions["h2"]}")
    }
}

plugins {
    val versions: Map<String, Any> by extra

    id("java")
    id("application")
    id("org.seasar.doma.codegen") version "1.4.1"
    id("org.seasar.doma.compile") version "1.1.0"
    kotlin("jvm") version (versions["kotlin"] as String)
    kotlin("kapt") version (versions["kotlin"] as String)
    kotlin("plugin.serialization") version (versions["kotlin"] as String)
}

group = "net.love2hina"
version = "0.3.0-SNAPSHOT"

val versions: Map<String, Any> by extra

repositories {
    mavenCentral()
}

dependencies {
    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:${versions["kotlin"]}")
    implementation("org.jetbrains.kotlin:kotlin-reflect:${versions["kotlin"]}")

    // Json Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.2")

    // Java Parser
    implementation("com.github.javaparser:javaparser-core:3.23.1")

    // H2 Database engine
    runtimeOnly("com.h2database:h2:${versions["h2"]}")

    // logback
    runtimeOnly("ch.qos.logback:logback-classic:1.2.10")

    // Doma2
    kapt("org.seasar.doma:doma-processor:${versions["doma"]}")
    implementation("org.seasar.doma:doma-kotlin:${versions["doma"]}")
    implementation("org.seasar.doma:doma-slf4j:${versions["doma"]}")

    // テスト
    testImplementation("org.junit.jupiter:junit-jupiter-api:${versions["junit"]}")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:${versions["junit"]}")

    testImplementation("org.mockito:mockito-inline:${versions["mockito"]}")
    testImplementation("org.mockito:mockito-junit-jupiter:${versions["mockito"]}")
    testImplementation("org.mockito.kotlin:mockito-kotlin:4.0.0")
}

java {
    sourceCompatibility = (versions["java"] as JavaVersion)
    targetCompatibility = (versions["java"] as JavaVersion)
}

application {
    mainClass.set("net.love2hina.kotlin.sharon.MainKt")
    applicationDistribution.from("src/main/powershell") {
        into("bin")
    }
    applicationDistribution.from("LICENSE.txt") {
        into("")
    }
}

tasks {

    compileKotlin {
        kotlinOptions.jvmTarget = (versions["target_jvm"]!! as String)
    }

    compileTestKotlin {
        kotlinOptions.jvmTarget = (versions["target_jvm"]!! as String)
    }

    test {
        useJUnitPlatform()
    }

    jar {
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
    }

    open class CreateStartScriptsEx: CreateStartScripts() {
        override fun getWindowsScript(): File? = File(outputDir, "$applicationName.ps1")

        override fun generate() {
            super.generate()

            // Unix用ファイルは不要
            unixScript.delete()
        }
    }

    open class PowerShellTemplateBindingFactory: Transformer<Map<String, String?>, JavaAppStartScriptGenerationDetails> {

        override fun transform(details: JavaAppStartScriptGenerationDetails): Map<String, String?> =
            hashMapOf(
                "applicationName" to details.applicationName,
                "optsEnvironmentVar" to details.optsEnvironmentVar,
                "exitEnvironmentVar" to details.exitEnvironmentVar,
                "mainClassName" to details.mainClassName,
                "defaultJvmOpts" to "",
                "appNameSystemProperty" to details.appNameSystemProperty,
                "appHomeRelativePath" to createJoinedAppHomeRelativePath(details.scriptRelPath),
                "classpath" to createJoinedPath(details.classpath),
                "modulePath" to createJoinedPath(details.modulePath),
            )

        private fun createJoinedPath(path: Iterable<String?>): String =
            Joiner.on(";").join(Iterables.transform(path) { "\$appHome\\${it?.replace("/", "\\") ?: ""}" })

        private fun createJoinedAppHomeRelativePath(scriptRelPath: String?): String {
            val depth = scriptRelPath?.sumOf { if (it == '/') 1L else 0L  } ?: 0L
            val builder = StringBuilder()
            for (i in 1..depth) {
                if (i > 1) builder.append("\\")
                builder.append("..")
            }
            return builder.toString()
        }

    }

    open class EmptyStartScriptGenerator: ScriptGenerator {
        override fun generateScript(details: JavaAppStartScriptGenerationDetails, destination: Writer) {
            // なにもしない
        }
    }

    open class PowerShellStartScriptGenerator: DefaultTemplateBasedStartScriptGenerator(
        "\r\n",
        PowerShellTemplateBindingFactory(),
        utf8ClassPathResource(UnixStartScriptGenerator::class.java, "windowsStartScript.ps1")
    )

    replace("startScripts", CreateStartScriptsEx::class.java)
    withType<CreateStartScriptsEx> {
        val generator = PowerShellStartScriptGenerator()
        generator.template = resources.text.fromFile("src/main/template/windowsStartScript.ps1", "UTF-8")
        windowsStartScriptGenerator = generator
        unixStartScriptGenerator = EmptyStartScriptGenerator()
    }

}
