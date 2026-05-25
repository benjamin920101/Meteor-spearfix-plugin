plugins {
    id("fabric-loom") version "1.11-SNAPSHOT"
}

base {
    archivesName = properties["archives_base_name"] as String
    version = properties["mod_version"] as String
    group = properties["maven_group"] as String
}

repositories {
    maven {
        name = "meteor-maven"
        url = uri("https://maven.meteordev.org/releases")
    }
    maven {
        name = "meteor-maven-snapshots"
        url = uri("https://maven.meteordev.org/snapshots")
    }
}

dependencies {
    // Fabric
    minecraft("com.mojang:minecraft:${properties["minecraft_version"] as String}")
    mappings("net.fabricmc:yarn:${properties["yarn_mappings"] as String}:v2")
    modImplementation("net.fabricmc:fabric-loader:${properties["loader_version"] as String}")

    // Meteor
    modImplementation("meteordevelopment:meteor-client:${properties["minecraft_version"] as String}-SNAPSHOT")

    // ASM — 提供 ClassReader / ClassWriter 等位元組碼操作 API
    // 用於 SpearFixAgent 的 decode() try-catch 植入
    // 使用 compileOnly 避免將 ASM 打包進 mod JAR。
    // ASM 在執行期由 Minecraft 啟動器的 classpath 提供（Fabric Loader 依賴），
    // 若重複打包會與系統 classloader 衝突導致 LinkageError。
    compileOnly("org.ow2.asm:asm:9.7.1")
    compileOnly("org.ow2.asm:asm-commons:9.7.1")
    compileOnly("org.ow2.asm:asm-tree:9.7.1")
}

tasks {
    processResources {
        val propertyMap = mapOf(
            "version" to project.version,
            "mc_version" to project.property("minecraft_version"),
        )

        inputs.properties(propertyMap)

        filteringCharset = "UTF-8"

        filesMatching("fabric.mod.json") {
            expand(propertyMap)
        }
    }

    jar {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        inputs.property("archivesName", project.base.archivesName.get())

        from("LICENSE") {
            rename { "${it}_${inputs.properties["archivesName"]}" }
        }

        // Java Agent: ASM 由執行期 classpath 提供（Fabric Loader / 啟動器），
        // 不再重複打包以避免 classloader 衝突。
        manifest {
            attributes(
                "Premain-Class" to "com.spearfix.addon.SpearFixAgent",
                "Can-Retransform-Classes" to "true"
            )
        }
    }

    java {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.release = 21
        options.compilerArgs.add("-Xlint:deprecation")
        options.compilerArgs.add("-Xlint:unchecked")
    }
}
