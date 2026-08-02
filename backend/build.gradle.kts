plugins {
    id("com.diffplug.spotless") version "6.25.0" apply false
    id("org.springframework.boot") version "3.3.5" apply false
    id("io.spring.dependency-management") version "1.1.6" apply false
}

allprojects {
    group = "com.bidstream"
    version = "0.1.0"

    repositories {
        mavenCentral()
    }
}

tasks.register("checkAll") {
    dependsOn(subprojects.map { it.tasks.named("check") })
}
