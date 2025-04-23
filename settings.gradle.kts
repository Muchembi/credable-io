rootProject.name = "credable-io"

include("loan-management-system")

dependencyResolutionManagement {
    versionCatalogs {
        create("springboot") {
            library("bom", "org.springframework.boot:spring-boot-dependencies:3.3.9")
        }
    }

}
