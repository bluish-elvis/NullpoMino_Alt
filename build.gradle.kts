import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.Delete
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.jvm.tasks.Jar

plugins {
	base
}

group = "mu.nu.nullpo"
version = "7.7.2026"

allprojects {
	group = rootProject.group
	version = rootProject.version
}

val installDir = layout.buildDirectory.dir("install")
val mavenInstallDir = layout.projectDirectory.dir("target/install")
tasks.register<Sync>("assembleInstall") {
	description = "Assemble the install directory"
	mustRunAfter("cleanInstallPreservedDirs")
	val coreProject = project(":nullpomino-core")
	val runProject = project(":nullpomino-run")

	dependsOn(":nullpomino-core:jar", ":nullpomino-run:jar")
	into(installDir)
	preserve {
		include("config/**", "scores/**", "replay/**")
	}
	duplicatesStrategy = DuplicatesStrategy.EXCLUDE

	// 1. Main application JAR → NullpoMino.jar at install root
	from(coreProject.tasks.named("jar")) {
		rename { "NullpoMino.jar" }
	}

	// 2. nullpomino-run resources (config/, res/, scripts, doc/) → install root
	//    strip the jar-internal lib/ prefix so the files land at install root
	from(runProject.tasks.named<Jar>("jar").map { zipTree(it.archiveFile) }) {
		exclude("META-INF/**")
		eachFile {
			if(path.startsWith("lib/"))
				path = path.removePrefix("lib/")
		}
	}

	// Eagerly resolve runtimeClasspath to only regular JAR files (acceptable for assembly task)
	val classpathJars = coreProject.configurations.named("runtimeClasspath").get()
		.files.filter { it.isFile && it.extension == "jar" }

	// 3. Dependency JARs → lib/
	from(classpathJars) {
		into("lib")
	}

	// 4. Native DLLs / SOs / dylibs extracted from all JARs → install/lib
	//    (java.library.path = target/install/lib)
	from(classpathJars.map { zipTree(it) }) {
		into("lib")
		include("**/*.dll", "**/*.so", "**/*.jnilib", "**/*.dylib")
		includeEmptyDirs = false
	}

	// Mirror Gradle install output into Maven-style target/install
	doLast {
		copy {
			from(installDir)
			into(mavenInstallDir)
			duplicatesStrategy = DuplicatesStrategy.INCLUDE
		}
	}
}

tasks.named("assemble") {
	dependsOn("assembleInstall")
}

tasks.register<Delete>("cleanInstallPreservedDirs") {
	description = "Delete preserved directories"
	delete(
		mavenInstallDir.dir("config") ,
		mavenInstallDir.dir("scores") ,
		mavenInstallDir.dir("replay") ,
	)
}
tasks.register("assembleReInstall") {
	description = "Re-assemble the install directory, with cleaning preserved setting directories"
	dependsOn("cleanInstallPreservedDirs", "assembleInstall")
}
