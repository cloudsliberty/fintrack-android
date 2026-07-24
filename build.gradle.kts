// Top-level build file. Individual module build files declare their own
// dependencies; this just wires up the plugins used anywhere in the project
// so subprojects can `apply` them without re-declaring versions.
plugins {
    id("com.android.application") version "8.5.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.24" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.24" apply false
}
