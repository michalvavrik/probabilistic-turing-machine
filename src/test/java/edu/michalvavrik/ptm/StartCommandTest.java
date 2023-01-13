package edu.michalvavrik.ptm;

import edu.michalvavrik.ptm.core.TransitionFunction;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;

public class StartCommandTest {

    @Test
    void testInputDataSet() throws IOException {
        final var cmd = new StartCommand();
        cmd.setInputDataPath("src/test/resources/simple-input-data.txt");
        Assertions.assertEquals("abcdefghijklmnopqrstuvwxyz", cmd.inputData);
    }

    @Test
    void testTransitionFunctionParsing() throws IOException {
        final var cmd = new StartCommand();
        cmd.setTransitionFunction("src/test/resources/simple-transition-function.txt");
        validateParsingResult(cmd);
    }

    @Test
    void testSubroutineParsing() throws IOException {
        final var cmd = new StartCommand();
        cmd.setTransitionFunction("src/test/resources/subroutine-parent.txt");
        validateParsingResult(cmd);
    }

    @Test
    void testAnyChar() throws IOException {
        final var cmd = new StartCommand();
        cmd.setAdditionalInputAlphabetSymbols("a,c,d,e,f,g,h");
        cmd.setTransitionFunction("src/test/resources/any-char-function.txt");
        final var inputData = new char[]{'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h'};
        final var turingMachine = cmd.turingMachineBuilder.build();
        final var configurations = turingMachine.compute(inputData);
        final var actual = configurations[configurations.length - 1].tape();
        final var expected = new char[]{'b', 'b', 'b', 'b', 'b', 'b', 'b', 'b'};
        Assertions.assertArrayEquals(expected, actual);
    }

    @Test
    void testDecimalToBinary() {
        // detect decimal numbers in string and convert them to binary numbers
        String str = "22#10";
        String actual = StartCommand.decimalToBinary(str.toCharArray());
        String expected = "10110#1010";
        Assertions.assertEquals(expected, actual);
    }

    private static void validateParsingResult(StartCommand cmd) {
        Assertions.assertEquals('A', cmd.turingMachineBuilder.getInitialState());
        Assertions.assertEquals(2, cmd.turingMachineBuilder.getFinalStates().size());
        Assertions.assertTrue(cmd.turingMachineBuilder.getFinalStates().stream().anyMatch(state -> state == 'C'));
        Assertions.assertTrue(cmd.turingMachineBuilder.getFinalStates().stream().anyMatch(state -> state == 'D'));
        Assertions.assertEquals(4, cmd.turingMachineBuilder.getStates().size());
        for (char state : new char[]{'A', 'B', 'C', 'D'}) {
            Assertions.assertTrue(cmd.turingMachineBuilder.getStates().stream().anyMatch(s -> s == state));
        }
        Assertions.assertEquals(5, cmd.turingMachineBuilder.getSpecialSymbols().size());
        for (char specialSymbol : new char[]{',', '-', ';', '@', '.'}) {
            Assertions.assertTrue(cmd.turingMachineBuilder.getSpecialSymbols().stream().anyMatch(s -> s == specialSymbol));
        }
        Assertions.assertEquals(3, cmd.turingMachineBuilder.getInputAlphabet().size());
        for (char inputSymbol : new char[]{'a', 'b', 'c'}) {
            Assertions.assertTrue(cmd.turingMachineBuilder.getInputAlphabet().stream().anyMatch(is -> is == inputSymbol));
        }

        // test created transition function
        final TransitionFunction fun = cmd.turingMachineBuilder.getTransitionFunction();
        verifyTransition('A', 'a', 'A', 'b', TransitionFunction.Move.RIGHT, fun);
        verifyTransition('A', '#', 'B', ';', TransitionFunction.Move.NEUTRAL, fun);
        verifyTransition('B', ';', 'B', ';', TransitionFunction.Move.LEFT, fun);
        verifyTransition('B', 'b', 'B', 'c', TransitionFunction.Move.LEFT, fun);
        verifyTransition('B', '#', 'C', 'c', TransitionFunction.Move.NEUTRAL, fun);
        verifyTransition('B', '-', 'D', 'c', TransitionFunction.Move.NEUTRAL, fun);
        // unknown symbol
        Assertions.assertNull(fun.project('B', 'z'));
        // unknown state
        Assertions.assertNull(fun.project('Z', 'a'));
        // known state and symbol, but unknown combination
        Assertions.assertNull(fun.project('B', 'a'));
    }

    private static void verifyTransition(char fromState, char fromSymbol, char toState, char toSymbol,
                                               TransitionFunction.Move move, TransitionFunction fun) {
        Assertions.assertEquals(new TransitionFunction.Action(toState, toSymbol, move),
                fun.project(fromState, fromSymbol));
    }

}
