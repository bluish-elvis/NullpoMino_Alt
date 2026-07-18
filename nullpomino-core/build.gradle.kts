import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.compile.JavaCompile
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
	alias(libs.plugins.kotlin.jvm)
	alias(libs.plugins.kotlin.serialization)
	`java-library`
}

repositories {
	mavenCentral()
	maven(url = "https://dev.webswing.org/public/nexus/repository/webswing-3rd-parties/")
	maven(url = uri("lib"))
}

java {
	toolchain {
		languageVersion.set(JavaLanguageVersion.of(26))
	}
}

kotlin {
	compilerOptions {
		jvmTarget.set(JvmTarget.fromTarget("26"))
		val compileVer = org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_4
		languageVersion.set(compileVer)
		apiVersion.set(compileVer)
		freeCompilerArgs.add("-Xconsistent-data-class-copy-visibility")
	}
}

/*sourceSets.main {
	java.exclude("target/**")
}*/*/

dependencies {
	val kotlinVer = libs.versions.kotlin.get()
	api("org.lwjgl.lwjgl:lwjgl:2.9.3")
	api("org.lwjgl.lwjgl:lwjgl_util:2.9.3")

	implementation("org.slick2d:slick2d-core:1.0.2") {
		exclude(group = "javax.jnlp", module = "jnlp-api")
	}
	implementation("org.jcraft:jorbis:0.0.17")
	implementation("org.swinglabs:swing-worker:1.1")
	implementation("org.swinglabs:swing-layout:1.0.3")
	implementation("net.java.jinput:jinput:2.0.9")
	implementation("net.java.jinput:jinput-platform:2.0.7")

	implementation("org.apache.logging.log4j:log4j-core:2.25.4")
	implementation("org.apache.logging.log4j:log4j-1.2-api:2.25.4")
	implementation("commons-io:commons-io:2.15.1")
	implementation("org.jline:jline:3.25.1")

//	implementation(kotlin("stdlib", kotlinVer))
	implementation("org.jetbrains.kotlinx:kotlinx-serialization-json-jvm:1.10.0")
	implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlinVer")
	implementation("org.ninia:jep:4.3.1")

	testImplementation(kotlin("test", kotlinVer))
	testImplementation("org.junit.jupiter:junit-jupiter:6.0.3")
}

tasks.withType<JavaCompile>().configureEach {
	options.release.set(26)
}

tasks.test {
	useJUnitPlatform()
	testLogging {
		exceptionFormat = TestExceptionFormat.FULL
	}
}
