rootProject.name = "credable-io"

include("loan-management-system")

dependencyResolutionManagement {
    versionCatalogs {
        create("springboot") {
            library("bom", "org.springframework.boot:spring-boot-dependencies:3.3.9")
        }

        create("fasterxml") {
            version("jackson", "2.18.3")

            library("jackson-databind", "com.fasterxml.jackson.core", "jackson-databind").versionRef("jackson")
        }
    }

}