package edu.michalvavrik.ptm.core;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Set;

public class TuringMachineImplTest {

    @Test
    void testSimpleTuringMachine() {
        // this is just simplification, usually we expect transition function to be defined by set of rules
        var transitionFunction = new TransitionFunction() {

            @Override
            public Action project(char state, char symbol) {
                if (state == 'a') {
                    return new Action('c', symbol, Move.NEUTRAL);
                }

                if (symbol == ',') {
                    return new Action(state, '@', Move.RIGHT);
                }

                final char nextSymbol = switch (symbol) {
                    case '1' -> 'c';
                    case '2' -> 'd';
                    case '3' -> 'e';
                    case '4' -> 'f';
                    case '5' -> 'g';
                    case '6' -> 'h';
                    case '7' -> 'i';
                    case '8' -> 'j';
                    case '9' -> 'k';
                    case '0' -> 'l';
                    default -> Assertions.fail();
                };
                // we just replace number with (state) alphabet letter
                Assertions.assertEquals(state, nextSymbol);

                return new Action(++state, nextSymbol, Move.RIGHT);
            }
        };
        var tm = new TuringMachineImpl(transitionFunction, Set.of('1', '2', '3', '4', '5', '6','7', '8', '9', '0', ',',
                'a','b','c', 'd','e', 'f', 'g', 'h', 'i','j','k', 'l'), 'a', Set.of('b', 'm'), Set.of('@'),
                Set.of('a','b','c', 'd','e', 'f', 'g', 'h', 'i','j','k', 'l', 'm'));
        var result = tm.compute("1,2,3,4,5,6,7,8,9,0".toCharArray());
        var resultStr = new String(result.tape());
        Assertions.assertEquals("c@d@e@f@g@h@i@j@k@l", resultStr);
    }

    @Test
    void testGrowTapeOnLeft() {
        final char[] oldTape = new char[] {'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k'};
        final char[] newTape = TuringMachineImpl.growTapeOnLeft(oldTape);
        Assertions.assertTrue(newTape.length > oldTape.length);
        for (int i = 0; i < oldTape.length; i++) {
            Assertions.assertEquals(oldTape[i], newTape[newTape.length - oldTape.length + i]);
        }
        for (int i = 0; i < newTape.length - oldTape.length; i++) {
            Assertions.assertEquals(TuringMachine.BLANK, newTape[i]);
        }
    }

    @Test
    void testGrowTapeOnRight() {
        final char[] oldTape = new char[] {'1', '2', '3', '4', '5', '6', '7', '8', '9', '0'};
        final char[] newTape = TuringMachineImpl.growTapeOnRight(oldTape);
        Assertions.assertTrue(newTape.length > oldTape.length);
        for (int i = 0; i < newTape.length; i++) {
            if (i < oldTape.length) {
                Assertions.assertEquals(oldTape[i], newTape[i]);
            } else {
                Assertions.assertEquals(TuringMachine.BLANK, newTape[i]);
            }
        }
    }

}
