package edu.michalvavrik.ptm;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;

public class FermatPrimalityTest {

    @Test
    void primalityTest() throws IOException {
        final var cmd = new StartCommand();
        cmd.setTransitionFunction("src/test/resources/fermat-primality-test.txt");
        cmd.converseDecimalToBinary = true;
        final int primeCandidate = 5;
        cmd.setInputData(Integer.toString(primeCandidate));
        final var result = cmd.computeInputData().tape();
        Assertions.assertEquals(1, result.length);
        Assertions.assertEquals(1, Integer.parseInt(new String(result)));
    }

}
