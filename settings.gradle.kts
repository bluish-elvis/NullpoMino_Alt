pluginManagement {
	repositories {
		gradlePluginPortal()
		mavenCentral()
	}
}

dependencyResolutionManagement {
	repositoriesMode.set(RepositoriesMode.PREFER_PROJECT)
	repositories {
		mavenCentral()
		maven(url = "https://dev.webswing.org/public/nexus/repository/webswing-3rd-parties/")
	}
}

rootProject.name = "NullpoMino_Alt"

include(":nullpomino-core")
include(":nullpomino-run")
