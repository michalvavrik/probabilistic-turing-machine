package edu.michalvavrik.ptm;

import org.jboss.logging.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.io.IOException;

import static edu.michalvavrik.ptm.core.TuringMachine.BLANK;

public class FermatCongruenceModuloTest {

    private static final Logger LOG = Logger.getLogger(FermatCongruenceModuloTest.class);

    @Test
    void testCongruence() throws IOException {
        final var cmd = new StartCommand();
        cmd.converseDecimalToBinary = false;
        cmd.setAdditionalInputAlphabetSymbols("%");
        cmd.setTransitionFunction("src/test/resources/fermat-congruence-modulo-test.txt");
        verify(cmd, 2, 3);
        verify(cmd, 2, 4);
        verify(cmd, 2, 5);
        verify(cmd, 2, 6);
        verify(cmd, 2, 7);
        verify(cmd, 2, 8);
        verify(cmd, 2, 9);
        verify(cmd, 2, 10);
        verify(cmd, 2, 11);
        verify(cmd, 2, 12);
        verify(cmd, 2, 13);
        verify(cmd, 2, 14);
        verify(cmd, 3, 4);
        verify(cmd, 4, 5);
        verify(cmd, 5, 6);
        verify(cmd, 6, 7);
    }

    @EnabledIfSystemProperty(named = "congruence-full", matches = "true")
    @Test
    void fullCongruenceTest() throws IOException {
        final var cmd = new StartCommand();
        cmd.converseDecimalToBinary = false;
        cmd.setAdditionalInputAlphabetSymbols("%");
        cmd.setTransitionFunction("src/test/resources/fermat-congruence-modulo-test.txt");
        // it takes almost 30 minutes to compute the below line
        verify(cmd, 7, 8);

        // it takes almost 1 hours to compute the below line (it goes down to the division operation)
        verify(cmd, 8, 9);

        // tests below either don't work due to bug in division or takes ridiculously long to compute
        // I never had patience to wait long enough
//        verify(cmd, 9, 10);
//        verify(cmd, 7, 11);
//        verify(cmd, 7, 12);
//        verify(cmd, 7, 13);
//        verify(cmd, 2, 14);
//        verify(cmd, 6, 15);
//        verify(cmd, 5, 16);
//        verify(cmd, 4, 17);
//        verify(cmd, 3, 18);
//        verify(cmd, 5, 19);
//        verify(cmd, 2, 20);
//        verify(cmd, 5, 21);
//        verify(cmd, 4, 22);
//        verify(cmd, 5, 23);
//        verify(cmd, 2, 24);
//        verify(cmd, 3);
//        verify(cmd, 9, 20);
    }

    private void verify(StartCommand cmd, int base, int exponent) {
        cmd.setInputData(Integer.toBinaryString(base) + BLANK + Integer.toBinaryString(exponent));
        var result = new String(cmd.computeInputData().tape());
        LOG.debugf("Verify result for base %d, exponent %d", base, exponent);
        final int expected = (Math.pow(base, exponent-1)%exponent) == 1 ? 1 : 0;
        Assertions.assertEquals(expected, Integer.parseInt(result, 2));
    }

}
