package edu.michalvavrik.ptm.core;

import java.util.HashSet;
import java.util.Set;

public interface ProbabilisticTuringMachine {

    /**
     * Special symbol used to swipe alphabet. We consider untouched tape parts to be blank symbols.
     */
    String BLANK = "#";

    void compute(char[] inputData);

    final class ProbabilisticTuringMachineBuilder {

        private final Set<String> inputAlphabet = new HashSet<>();
        private final Set<String> finalStates = new HashSet<>();
        private final Set<String> specialSymbols = new HashSet<>();
        private TransitionFunction transitionFunction;
        private String initialState;

        public ProbabilisticTuringMachine build() {
            return new ProbabilisticTuringMachineImpl(transitionFunction, inputAlphabet, initialState, finalStates, specialSymbols);
        }
    }
}
