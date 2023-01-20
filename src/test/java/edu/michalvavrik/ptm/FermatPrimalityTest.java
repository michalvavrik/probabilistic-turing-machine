package edu.michalvavrik.ptm;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;

public class FermatPrimalityTest {

    private static final int PRIME = 1;
    private static final int NOT_PRIME = 0;

    @Test
    void primalitySmokeTest() throws IOException {
        // WARNING: this test is by design flaky as Fermat Primality Test also produce Fermat liars
        // and because of probabilistic step, the test is not deterministic, therefore sometimes it detects
        // Fermat witness, and sometimes it does not
        final var cmd = new StartCommand();
        cmd.setTransitionFunction("src/test/resources/fermat-primality-test.txt");
        cmd.converseDecimalToBinary = true;
        verify(cmd, 3, PRIME);
        verify(cmd, 4, NOT_PRIME);
        verify(cmd, 5, PRIME);
        verify(cmd, 6, NOT_PRIME);
        verify(cmd, 7, PRIME);
        verify(cmd, 8, NOT_PRIME);
        verify(cmd, 9, NOT_PRIME);
    }

    @Test
    void bigNumbers() throws IOException {
        // flaky by design, see above
        final var cmd = new StartCommand();
        cmd.setTransitionFunction("src/test/resources/fermat-primality-test.txt");
        cmd.converseDecimalToBinary = true;
        verify(cmd, 10, NOT_PRIME);
        verify(cmd, 11, PRIME);
        verify(cmd, 12, NOT_PRIME);
        verify(cmd, 13, PRIME);
        verify(cmd, 14, NOT_PRIME);
    }

    private void verify(StartCommand cmd, int primeCandidate, int expected) {
        cmd.setInputData(Integer.toString(primeCandidate));
        final var result = cmd.computeInputData().tape();
        Assertions.assertEquals(1, result.length);
        Assertions.assertEquals(expected, Integer.parseInt(new String(result)));
    }

}
