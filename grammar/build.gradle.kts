import at.phatbl.shellexec.ShellExec
import java.io.File
import java.net.URI
import java.util.regex.Pattern

plugins {
    idea
    id("org.jetbrains.intellij") version "0.6.5"
    antlr
    `maven-publish`
    id("org.jetbrains.kotlin.jvm") // version "1.4.31"
    id("at.phatbl.shellexec")
}

group = "org.jetbrains.kotlin.spec.grammar"
version = "0.1"

val jar: Jar by tasks
val archivePrefix = "kotlin-grammar-parser"

repositories {
    maven {
        url = URI("https://maven.pkg.github.com/vorpal-research/kotlin-maven")
        credentials {
            username = "vorpal-reseacher"
            password = "\u0031\u0030\u0062\u0037\u0064\u0066\u0031\u0032\u0063\u0064" +
                    "\u0035\u0034\u0038\u0037\u0034\u0065\u0030\u0034\u0035\u0035" +
                    "\u0038\u0031\u0063\u0039\u0039\u0062\u0031\u0066\u0032\u0030" +
                    "\u0038\u0065\u0031\u0061\u0035\u0033\u0065\u0036\u0032\u0038"
        }
    }
    mavenCentral()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = group as String
            artifactId = archivePrefix
            version = version as String

            from(components["java"])
        }
    }
}

sourceSets {
    main {
        java.srcDir("src/main")
    }
    test {
        java.srcDir("src/test")
    }
}

dependencies {
    compile("junit:junit:4.12")
    antlr("org.antlr:antlr4:4.+")
}

tasks.compileKotlin {
    dependsOn("generateGrammarSource")
}

intellij {
    version = "IC-2020.2"
}

tasks.withType<AntlrTask> {
    outputDirectory =
        File("${project.rootDir}/grammar/src/main/java/org/jetbrains/kotlin/spec/grammar/parser").also { it.mkdirs() }

    arguments.add("-package")
    arguments.add("org.jetbrains.kotlin.spec.grammar")
}

tasks.withType<Test> {
    workingDir = File("${project.rootDir}/${project.name}")
    ignoreFailures = project.hasProperty("teamcity")
}

tasks.create("removeCompilerTestData") {
    doFirst {
        File("${project.rootDir}/${project.name}/testData").walkTopDown().forEach {
            if (!it.name.endsWith("antlrtree.txt"))
                it.delete()
        }
    }
}

tasks.create<ShellExec>("downloadCompilerTests") {
    doFirst {
        workingDir = File("$projectDir")
        command = File("$projectDir/scripts/build/downloadCompilerTests.sh").readText()
    }
}

tasks.create("syncWithCompilerTests") {
    doFirst {
        File("${project.rootDir}/${project.name}/testData").walkTopDown().forEach {
            if (it.name.endsWith("antlrtree.txt") && !File("${it.parent}/${it.name.substringBefore(".antlrtree.txt")}.kt").exists())
                it.delete()
        }
    }
}

tasks.create("prepareDiagnosticsCompilerTests") {
    doFirst {
        //language=RegExp
        val individualDiagnostic = """[^!,]*"""
        val rangeStartOrEndPattern = Pattern.compile("(<!(([^>])|((?<!\\!)\\>))+!>)|(<!>)")
        val filePattern = Pattern.compile("""// ?FILE: ?""")
        val ls = System.lineSeparator()
        val sourceCodeByFilePattern =
            Pattern.compile("""^(?<filename>.*?)\.(?<extension>kts?|java)($ls)*(?<code>[\s\S]*)$""")

        File("${project.rootDir}/${project.name}/testData/diagnostics").walkTopDown().forEach {
            println("Processing file $it")
            if (it.name.endsWith(".fir.kt")) {
                it.delete()
            } else if (it.extension == "kt") {
                val sourceCode = it.readText()
                val sourceCodeByFiles = sourceCode.split(filePattern)

                if (sourceCodeByFiles.size > 1) {
                    sourceCodeByFiles.forEachIndexed { index, content ->
                        if (index == 0)
                            return@forEachIndexed

                        val matcher = sourceCodeByFilePattern.matcher(content).apply { find() }
                        val filename = matcher.group("filename").replace("/", "_")
                        val extension = matcher.group("extension")
                        val code = matcher.group("code")

                        if (extension == "kt" || extension == "kts")
                            File("${it.parent}/${it.nameWithoutExtension}.$filename.$extension").writeText(
                                rangeStartOrEndPattern.matcher(code).replaceAll("")
                            )
                    }
                    it.delete()
                } else {
                    it.writeText(rangeStartOrEndPattern.matcher(it.readText()).replaceAll(""))
                }
            } else if (!it.name.endsWith("antlrtree.txt")) {
                it.delete()
            }
        }
    }
}

jar.archiveName = "$archivePrefix-$version.jar"

jar.manifest {
    attributes(
        mapOf(
            "Class-Path" to configurations.runtime.files.joinToString(" ") { it.name }
        )
    )
}

jar.from(configurations.runtime.files.map { if (it.isDirectory) it else zipTree(it) })
