import at.phatbl.shellexec.ShellExec

plugins {
    id("org.jetbrains.kotlin.jvm") version "1.4.31" apply false
    id("at.phatbl.shellexec") version "1.1.3"
}

val htmlSourceDir = "$projectDir/docs/build/spec/html"
val htmlTargetDir = "$buildDir/spec/html"

val pdfSourceDir = "$projectDir/docs/build/spec/pdf"
val pdfTargetDir = "$buildDir/spec/pdf"

val resourcesSourceDir = "$projectDir/web/resources"
val resourcesTargetDir = "$htmlTargetDir/resources"

val jsDistributionDir = "$projectDir/web/build/distributions"
val jsTargetDir = "$resourcesTargetDir/js"

val katexDistDir = "$buildDir/js/node_modules/katex/dist"
val katexTargetDir = "$jsTargetDir/katex"

tasks.create<Copy>("copyStatic") {
    group = "internal"

    from(resourcesSourceDir)
    into(resourcesTargetDir)
}

tasks.create<Copy>("copyBuiltJs") {
    group = "internal"

    mustRunAfter("web:build")

    from(jsDistributionDir)
    into(jsTargetDir)
}

tasks.create<Copy>("copyKatex") {
    group = "internal"

    mustRunAfter("web:build")

    from(katexDistDir)
    into(katexTargetDir)
}

tasks.create<Copy>("copyHtml") {
    group = "internal"

    mustRunAfter("docs:buildHtml", "docs:buildHtmlBySections")

    from(htmlSourceDir)
    into(htmlTargetDir)
}

tasks.create<Copy>("copyPdf") {
    group = "internal"

    mustRunAfter("docs:buildPdf", "docs:buildPdfBySections")

    from(pdfSourceDir)
    into(pdfTargetDir)
}

tasks.create<Copy>("copyStubIndexToRedirectToIntroduction") {
    group = "internal"

    mustRunAfter("docs:buildPdf", "docs:buildPdfBySections")

    from("$resourcesSourceDir/html")
    into(htmlTargetDir)
}

tasks.create("buildJs") {
    group = "internal"

    dependsOn("copyStatic")
    dependsOn("copyBuiltJs")
    dependsOn("copyKatex")
    dependsOn("web:build")

    doFirst {
        File(jsTargetDir).mkdirs()
        File(katexTargetDir).mkdirs()
    }
}

tasks.create("buildWeb") {
    group = "build"

    dependsOn("docs:buildHtml")
    dependsOn("docs:buildHtmlBySections")
    dependsOn("copyHtml")
    dependsOn("buildJs")

    // finalizedBy("web:clean", "docs:clean")
}

tasks.create("buildWebFullOnly") {
    group = "build"

    dependsOn("docs:buildHtml")
    dependsOn("copyHtml")
    dependsOn("buildJs")
}

tasks.create("buildWebBySectionsOnly") {
    group = "build"

    dependsOn("docs:buildHtmlBySections")
    dependsOn("copyHtml")
    dependsOn("buildJs")
    dependsOn("copyStubIndexToRedirectToIntroduction")
}

tasks.create("buildPdf") {
    group = "build"

    dependsOn("docs:buildPdf")
    dependsOn("docs:buildPdfBySections")
    dependsOn("copyPdf")

    // finalizedBy("web:clean", "docs:clean")
}

tasks.create<ShellExec>("syncGrammarWithKotlinGrammarApache2Repo") {
    group = "internal"
    command = """echo -e Run the following command: git checkout release \&\& git subtree push --prefix grammar/src/main/antlr git@github.com:Kotlin/kotlin-grammar-apache2 release"""
}
