plugins {
    java
    id("io.freefair.lombok")
}

repositories {
    mavenCentral()
}


tasks.withType<Jar> {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.withType<JavaCompile> {
    //-parameters compiler argument is needed so that method parameter names can be compiled into the app
    // as opposed to the arbitrary 'arg0', 'arg1'... names.
    //This is useful so that we can get the argument name when throwing ConstraintViolationException exceptions
    //so that we can give a more meaningful error message to the user
    options.compilerArgs.add("-parameters")
    // enable compilation in a separate daemon process
    options.isFork = true
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

