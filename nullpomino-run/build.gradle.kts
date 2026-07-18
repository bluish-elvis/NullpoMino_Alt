import org.gradle.jvm.tasks.Jar

plugins {
	base
}

// Resources are packaged into a JAR and extracted by the root assembleInstall task into /target/install/
tasks.register<Jar>("jar") {
	archiveBaseName.set(project.name)
	from("config") { into("config") }
	from("doc"){into("doc")}
	from(rootProject.file("README.md")) {into("doc")}
	from("res") {
		into("res")
		include("bgm/**/*.ogg")
		include("bgm/**/*.txt")
		include("font/*.ttf")
		include("graphics/**/*.png")
		include("icons/*.icns")
		include("icons/*.ico")
		include("icons/*.png")
		include("jingle/*.ogg")
		include("se/*.ogg")
		include("se/*.wav")
	}
	from("scripts")
}

tasks.named("assemble") {
	dependsOn("jar")
}
