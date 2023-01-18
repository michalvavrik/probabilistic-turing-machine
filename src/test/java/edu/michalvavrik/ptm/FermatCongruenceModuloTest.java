package edu.michalvavrik.ptm;

import org.jboss.logging.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static java.lang.String.format;

public class FermatCongruenceModuloTest {

    private static final Logger LOG = Logger.getLogger(FermatCongruenceModuloTest.class);

    @Test
    void testCongruence() throws IOException {
        final var cmd = new StartCommand();
        cmd.converseDecimalToBinary = false;
        cmd.setAdditionalInputAlphabetSymbols("%");
        cmd.setTransitionFunction("src/test/resources/fermat-congruence-modulo-test.txt");
        for (int base = 2; base < 10; base++) {
            verify(cmd, base, 4, 25);
            verify(cmd, base, 3, 25);
            verify(cmd, base, 5, 25);
            verify(cmd, base, 20, 25);
        }
    }

    private void verify(StartCommand cmd, int base, int exponent, int extra) {
        cmd.setInputData(format("%s#%s####~%s", Integer.toBinaryString(base), Integer.toBinaryString(exponent),
                Integer.toBinaryString(extra)));
        var result = new String(cmd.computeInputData().tape()).split(Character.toString('~'));
        LOG.debugf("Verify result for base %d, exponent %d, extra %d", base, exponent, extra);
        Assertions.assertEquals(extra, Integer.parseInt(result[1], 2));
        Assertions.assertEquals((Math.pow(base, exponent-1)%exponent), Integer.parseInt(result[0], 2));
    }

}
