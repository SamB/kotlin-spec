rootProject.name = "kotlin-spec"

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        jcenter()
    }
}

val withGrammarProject: String? by settings

include("docs")
include("web")

if (withGrammarProject?.toBoolean() != false) {
    include("grammar")
}
