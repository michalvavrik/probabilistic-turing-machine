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
        verify(cmd, 3);
        verify(cmd, 4);
        verify(cmd, 5);
        verify(cmd, 6);
        verify(cmd, 7);
        verify(cmd, 8);
        verify(cmd, 9);
        verify(cmd, 10);
        verify(cmd, 11);
        verify(cmd, 12);
        verify(cmd, 13);
        verify(cmd, 14);
    }

    private void verify(StartCommand cmd, int primeCandidate) {
        cmd.setInputData(Integer.toString(primeCandidate));
        final var result = cmd.computeInputData().tape();
        Assertions.assertEquals(1, result.length);
        Assertions.assertEquals(1, Integer.parseInt(new String(result)));
    }

}
