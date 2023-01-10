package edu.michalvavrik.ptm;

import edu.michalvavrik.ptm.core.TransitionFunction;
import edu.michalvavrik.ptm.core.TuringMachine.TuringMachineBuilder;
import picocli.CommandLine;
import picocli.CommandLine.Command;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

@Command(name = "turing-machine", mixinStandardHelpOptions = true)
public class StartCommand implements Runnable {

    private String inputData;
    private TransitionFunction transitionFunction;

    @CommandLine.Option(names = {"--input-file-path", "-in"})
    public void setInputData(String path) throws IOException {
        inputData = Files.readString(Path.of(path));
    }

    @CommandLine.Option(names = {"--transition-function-file-path", "-tf"}, description = """
            
            """)
    public void setTransitionFunction(String path) throws IOException {
        final String transitionRules = Files.readString(Path.of(path));
        parseTransitionRules(transitionRules);
    }

    private void parseTransitionRules(String transitionRules) {

    }

    @Override
    public void run() {
        Objects.requireNonNull(inputData);
        final var turingMachine = new TuringMachineBuilder().build();
        final var result = turingMachine.compute(inputData.trim().toCharArray());
    }

}
