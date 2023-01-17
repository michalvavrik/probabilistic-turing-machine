package edu.michalvavrik.ptm;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Collectors;

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
//        multiplyAndVerify(5, 5, 5);
    }

    private void multiplyAndVerify(long... products) throws IOException {
        final var cmd = new StartCommand();
        cmd.converseDecimalToBinary = true;
        cmd.setTransitionFunction("src/test/resources/multiplication-test.txt");
        var inputData = Arrays.stream(products).mapToObj(Long::toString).collect(Collectors.joining("*"));
        cmd.setInputData(inputData);
        final long actual = Long.parseLong(new String(cmd.computeInputData().tape()), 2);
        final long expected = Arrays.stream(products).reduce(1, (p1, p2) -> p1 * p2);
        Assertions.assertEquals(expected, actual);
    }

}
