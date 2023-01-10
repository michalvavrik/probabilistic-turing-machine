package edu.michalvavrik.ptm.core;

public interface TransitionFunction {

    enum Move {
        /**
         * Move head to the left
         */
        LEFT,
        /**
         * Move head to the right
         */
        RIGHT,
        /**
         * Do not move the head
         */
        NEUTRAL
    }

    /**
     * The action that the machine should take.
     *
     * @param state - represents resulting configuration of the machine; the machine must move to this state
     * @param symbol - input symbol should be overwritten with this symbol
     * @param move - move the tape head in this direction
     */
    record Action(char state, char symbol, Move move) {}

    /**
     * Projection that determines the Turing machine behavior.
     *
     * @param state char - current state
     * @param symbol char - input symbol currently read by the tape head
     * @return TransitionResult - action that should be taken at this step.
     */
    Action project(char state, char symbol);

}
