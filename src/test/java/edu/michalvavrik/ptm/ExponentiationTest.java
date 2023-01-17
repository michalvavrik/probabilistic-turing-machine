package edu.michalvavrik.ptm;

import edu.michalvavrik.ptm.core.TuringMachine;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.io.IOException;
import java.math.BigInteger;

public class ExponentiationTest {

    private static final Logger LOG = Logger.getLogger(ExponentiationTest.class);

    @Test
    void smokeTest() throws IOException {
        final var cmd = new StartCommand();
        cmd.converseDecimalToBinary = true;
        cmd.setTransitionFunction("src/test/resources/exponentiation-test.txt");
        for (int exponent = 30; exponent > 1; exponent--) {
            for (int base = 10; base > 1; base--) {
                powAndVerify(cmd, exponent, base);
                powAndVerifyWithNeighbouringSymbols(cmd, exponent, base, exponent);
            }
        }
    }

    /**
     * This test is very slow! You can enable it on demand and go to lunch.
     */
    @EnabledIfSystemProperty(named = "exponentiation-full", matches = "true")
    @Test
    void testBigNumbers() throws IOException {
        final var cmd = new StartCommand();
        cmd.converseDecimalToBinary = true;
        cmd.setTransitionFunction("src/test/resources/exponentiation-test.txt");
        powAndVerify(cmd, 100, 100);
        powAndVerifyWithNeighbouringSymbols(cmd, 53, 38, 25);
    }

    private void powAndVerify(StartCommand cmd, long exponent, long base) {
        cmd.setInputData(base + "^" + exponent);
        final var result = new String(cmd.computeInputData().tape());
        LOG.debugf("Verify result of %d^#%d", base, exponent);
        double actual = new BigInteger(result, 2).doubleValue();
        double expected = Math.pow((double) base, (double) exponent);
        Assertions.assertEquals(expected, actual);
    }

    private void powAndVerifyWithNeighbouringSymbols(StartCommand cmd, long exponent, long base, int neighbour) {
        cmd.setInputData(base + "^" + exponent + TuringMachine.BLANK + neighbour);
        var result = new String(cmd.computeInputData().tape()).split(Character.toString(TuringMachine.BLANK));
        final int actualNeighbour = Integer.parseInt(result[1].trim(), 2);
        LOG.debugf("Verify result of %d^%d%s%d", base, exponent, TuringMachine.BLANK, neighbour);
        Assertions.assertEquals(neighbour, actualNeighbour);
        double actual = new BigInteger(result[0].trim(), 2).doubleValue();
        double expected = Math.pow((double) base, (double) exponent);
        Assertions.assertEquals(expected, actual);
    }

}
