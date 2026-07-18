pluginManagement {
	repositories {
		gradlePluginPortal()
		mavenCentral()
	}
}

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
	repositoriesMode.set(RepositoriesMode.PREFER_PROJECT)
	repositories {
		mavenCentral()
		maven(url = "https://dev.webswing.org/public/nexus/repository/webswing-3rd-parties/")
	}
}

rootProject.name = "NullpoMino_Alt"
//rootProject.group = "mu.nu.nullpo"
//rootProject.version = "7.7.2026"

include("nullpomino-core", "nullpomino-run")
