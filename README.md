# Probabilistic Turing machine - Fermat primality test

This project includes Turing Machine implemented in Java (JDK 17) and convenient CLI interface that allows you to pass
Turing machine definition and input data. The project leverages Quarkus so that you can create native application that
runs independently on JVM (as any other native app). To simply run Fermat primality test, please execute following
commands in project root directory:

```shell script
mvn clean package
# is 14 a prime number? 
# (division operation implemented here is not efficient, do not use big numbers unless you have hours...)
java -jar target/quarkus-app/quarkus-run.jar -in=14 -tf=fermat-primality-test.txt
```

In case you are not familiar with Quarkus or run into any obstacle, following [build guide](https://github.com/michalvavrik/probabilistic-turing-machine/blob/master/BUILD_MANUAL.md) will help you hit the ground running.
If you want to see all configuration options you can [read `@CommandLine.Option` here](https://github.com/michalvavrik/probabilistic-turing-machine/blob/master/src/main/java/edu/michalvavrik/ptm/StartCommand.java), or just run:

```shell script
java -jar target/quarkus-app/quarkus-run.jar --help
```

The source code is documented (contract is established) through unit tests, so if in doubt about what's expected behavior, 
or if you run into any issues at all, I'd strongly suggest to check out tests.