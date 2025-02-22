import at.phatbl.shellexec.ShellExec
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.nio.file.Paths
import java.net.URI

plugins {
    application
    kotlin("jvm")
    id("at.phatbl.shellexec")
}

group = "org.jetbrains.kotlin.spec"
version = "0.1"

val htmlBuildDir = "$buildDir/spec/html"
val pdfBuildDir = "$buildDir/spec/pdf"
val resourcesBuildDir = "$htmlBuildDir/resources"
val jsBuildDir = "$resourcesBuildDir/js"
val scriptsDir = "$projectDir/scripts/build"
val ls: String = System.lineSeparator()

fun getScriptText(scriptName: String): String {
    val disableTODOS = project.findProperty("disableTODOS") != null
    val enableStaticMath = project.findProperty("enableStaticMath") != null

    val buildTemplate = File("$scriptsDir/$scriptName.sh").readText()

    val res = with(StringBuilder()) {
        append("PROJECT_DIR=$projectDir$ls")
        if (disableTODOS) append("TODO_OPTION=--disable-todos$ls")
        else append("TODO_OPTION=--enable-todos$ls")

        if (enableStaticMath) append("STATIC_MATH_OPTION=--enable-static-math$ls")
        else append("STATIC_MATH_OPTION=--disable-static-math$ls")

        append(buildTemplate)
    }

    return "$res"
}

repositories {
    maven {
        url = URI("https://maven.vorpal-research.science")
    }
    mavenCentral()
}

sourceSets {
    main {
        java.srcDir("src/main/kotlin")
    }
}

dependencies {
    implementation("ru.spbstu:g4-to-ebnf:0.0.0.4")
    implementation("ru.spbstu:kotlin-pandoc:0.0.15")
    implementation("ru.spbstu:simple-diagrammer:0.0.0.7")
    implementation("com.github.ajalt:clikt:1.7.0")
    implementation("com.zaxxer:nuprocess:2.0.1")
    implementation("org.antlr:antlr4:4.+")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

tasks.create<Jar>("filtersJar") {
    from(
        sourceSets.main.get().output,
        *configurations.runtimeClasspath.get().map { if (it.isDirectory()) it else zipTree(it) }.toTypedArray()
    )
    archiveFileName.set("filters.jar")
    with(tasks.jar.get())

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.create<JavaExec>("convertGrammar") {
    val grammarsDir = "$rootDir/grammar/src/main/antlr"
    val lexerGrammar = "KotlinLexer.g4"
    val parserGrammar = "KotlinParser.g4"
    val outputFile = "./src/md/kotlin.core/grammar.generated.md"

    inputs.files("$grammarsDir/$lexerGrammar", "$grammarsDir/$parserGrammar")
    outputs.file(outputFile)

    classpath = sourceSets["main"].runtimeClasspath
    main = "org.jetbrains.kotlin.spec.ConvertGrammarKt"
    args = listOf("-d", grammarsDir, "-l", lexerGrammar, "-p", parserGrammar, "-o", outputFile)
}

tasks.create("prepareShell") {
    group = "internal"

    val disableTODOS = project.findProperty("disableTODOS") != null
    val enableStaticMath = project.findProperty("enableStaticMath") != null

    if (enableStaticMath) {
        dependsOn(":kotlinNpmInstall") // we need npm to install Katex to run it
    }

    doFirst {
        val res = with(StringBuilder()) {
            appendln("PROJECT_DIR=$projectDir")
            if (disableTODOS) appendln("TODO_OPTION=--disable-todos")
            else appendln("TODO_OPTION=--enable-todos")

            if (enableStaticMath) {
                appendln("STATIC_MATH_OPTION=--enable-static-math")
                appendln("KATEX_BIN_OPTION=\"--katex=${rootProject.buildDir}/js/node_modules/.bin/katex\"")
            }
            else appendln("STATIC_MATH_OPTION=--disable-static-math")
        }

        File("$buildDir/prepare.sh").writeText("$res")
    }

}

tasks.create("buildPdf") {
    group = "internal"
    dependsOn("pdf:build")
}

tasks.create("buildPdfBySections") {
    group = "internal"
    dependsOn("pdfSections:build")
}

tasks.create("buildHtml") {
    group = "internal"
    dependsOn("html:build")
}

tasks.create("buildHtmlBySections") {
    group = "internal"
    dependsOn("htmlSections:build")
}

tasks.create<JavaExec>("execute") {
    group = "internal"

    classpath = sourceSets["main"].runtimeClasspath

    main = project.findProperty("mainClass") as? String ?: ""
    args = (project.findProperty("args") as? String)?.split(" ") ?: emptyList()
    val wd = project.findProperty("workDir") as? String
    workingDir = wd?.let { File(it) } ?: File(".")

    standardInput = System.`in`
    standardOutput = System.out
    errorOutput = System.err
}
