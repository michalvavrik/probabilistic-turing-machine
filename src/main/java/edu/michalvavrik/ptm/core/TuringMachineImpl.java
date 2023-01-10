package edu.michalvavrik.ptm.core;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

final class ProbabilisticTuringMachineImpl implements ProbabilisticTuringMachine {

    private final TransitionFunction transitionFunction;

    /**
     * Tape is used as a memory.
     */


    private final Set<String> tapeAlphabet;

    /**
     * Set of final states or accepting states.
     */
    private final Set<String> finalStates;
    private final String initialState;

    ProbabilisticTuringMachineImpl(TransitionFunction transitionFunction, Set<String> inputAlphabet, String initialState, Set<String> finalStates, Set<String> specialSymbols) {
        this.transitionFunction = Objects.requireNonNull(transitionFunction);
        if (inputAlphabet == null || inputAlphabet.isEmpty()) {
            throw new IllegalArgumentException("Input alphabet must have at least one letter");
        }
        if (inputAlphabet.contains(BLANK)) {
            throw new IllegalArgumentException(String.format("Input alphabet must not contain blank symbol '%s'", BLANK));
        }
        Set<String> tapeAlphabetList = new HashSet<>(inputAlphabet);
        if (specialSymbols != null && !specialSymbols.isEmpty()) {
            if (specialSymbols.contains(BLANK)) {
                throw new IllegalArgumentException(String.format("Special symbols must not contain blank symbol '%s'", BLANK));
            }
            if (specialSymbols.stream().anyMatch(inputAlphabet::contains)) {
                throw new IllegalArgumentException("Special symbols and input alphabet must not overlay");
            }
            tapeAlphabetList.addAll(specialSymbols);
        }
        tapeAlphabetList.add(BLANK);
        this.tapeAlphabet = Set.copyOf(tapeAlphabetList);
        if (initialState == null || initialState.isBlank()) {
            throw new IllegalStateException("Initial state is not defined");
        }
        this.initialState = initialState;
        if (finalStates == null || finalStates.isEmpty()) {
            throw new IllegalArgumentException("At least one final state must be specified");
        }
        this.finalStates = Set.copyOf(finalStates);
    }

    @Override
    public void compute(char[] inputData) {
        String currentState = initialState;
        // FIXME: validate input!!!1 mainly symbols, format etc.; if (!accepts()) { throw new blabla();}
        // FIXME: IMPL. ME!
    }
}
