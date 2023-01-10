package edu.michalvavrik.ptm.core;

import java.util.HashSet;
import java.util.Set;

public interface TuringMachine {

    /**
     * Special symbol used to swipe alphabet. We consider untouched tape parts to be blank symbols.
     */
    char BLANK = '#';

    char[] compute(char[] inputData);

    final class TuringMachineBuilder {

        private final Set<Character> inputAlphabet = new HashSet<>();
        private final Set<Character> finalStates = new HashSet<>();
        private final Set<Character> specialSymbols = new HashSet<>();
        private final Set<Character> states = new HashSet<>();
        private TransitionFunction transitionFunction = null;
        private char initialState;

        public TuringMachine build() {
            return new TuringMachineImpl(transitionFunction, inputAlphabet, initialState, finalStates, specialSymbols,
                    states);
        }
    }
}
