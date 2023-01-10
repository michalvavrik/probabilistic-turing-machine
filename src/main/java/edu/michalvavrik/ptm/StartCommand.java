package edu.michalvavrik.ptm;

import edu.michalvavrik.ptm.core.ProbabilisticTuringMachine.ProbabilisticTuringMachineBuilder;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "run", mixinStandardHelpOptions = true)
public class RunCommand implements Runnable {

    @Parameters(paramLabel = "<name>", defaultValue = "picocli",
            description = "Your name.")
    String name;

    @Override
    public void run() {
        new ProbabilisticTuringMachineBuilder()
                .build()
                .run();
    }

}
