package edu.michalvavrik.ptm;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;

public class MultiplicationTest {

    @Test
    void testBinaryMultiplication() throws IOException {
        multiplyAndVerify(22, 10);
        multiplyAndVerify(10, 22);
        multiplyAndVerify(1, 1);
        multiplyAndVerify(0, 0);
        multiplyAndVerify(1, 0);
        multiplyAndVerify(0, 1);
        multiplyAndVerify(500, 1);
        multiplyAndVerify(500, 1000);
        multiplyAndVerify(2358966, 6482231);
        multiplyAndVerify(752, 74396);
        multiplyAndVerify(999989898, 998879876);
    }

    private void multiplyAndVerify(long left, long right) throws IOException {
        final var cmd = new StartCommand();
        cmd.converseDecimalToBinary = true;
        cmd.setTransitionFunction("src/test/resources/multiplication-test.txt");
        cmd.setInputData(String.format("%s#%s", left, right)); // 22 * 10
        final var configurations = cmd.computeInputData();
        final long actual = Long.parseLong(new String(configurations[configurations.length - 1].tape()), 2);
        final long expected = left * right;
        Assertions.assertEquals(expected, actual);
    }

}
