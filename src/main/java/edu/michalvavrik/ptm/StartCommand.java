package edu.michalvavrik.ptm;

import edu.michalvavrik.ptm.core.TransitionFunction;
import edu.michalvavrik.ptm.core.TransitionFunction.Action;
import edu.michalvavrik.ptm.core.TuringMachine;
import edu.michalvavrik.ptm.core.TuringMachine.TuringMachineBuilder;
import org.jboss.logging.Logger;
import picocli.CommandLine;
import picocli.CommandLine.Command;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static edu.michalvavrik.ptm.core.TuringMachine.ANY;
import static edu.michalvavrik.ptm.core.TuringMachine.BLANK;
import static java.lang.String.format;
import static java.util.Map.entry;

@Command(name = "turing-machine", mixinStandardHelpOptions = true)
public class StartCommand implements Runnable {

    private static final Pattern NUMBER_PATTERN = Pattern.compile("\\d");
    private static final Logger LOG = Logger.getLogger(StartCommand.class);
    String inputData;
    final TuringMachineBuilder turingMachineBuilder = new TuringMachineBuilder();

    @CommandLine.Option(names = {"--binary-mode"}, description = "Detect all decimal numbers and converse them to binary",
            defaultValue = "true")
    boolean converseDecimalToBinary;

    @CommandLine.Option(names = {"--additional-input-alphabet-symbols"}, description = """
            Comma separated list of additional input alphabet symbols.
            
            Input alphabet symbols are auto-detected as symbols used by transition function not explicitly marked as
            special symbols. This option allows you to define additional symbols that can't be detected from the rules.
            That is useful mainly when you have the rule to match ANY character.
            """)
    public void setAdditionalInputAlphabetSymbols(String symbols) throws IOException {
        for (String symbol : symbols.split(",")) {
            symbol = symbol.trim();
            if (symbol.length() != 1) {
                throw new IllegalArgumentException("Input alphabet symbol must be exactly one character");
            }
            final char symbolChar = symbol.charAt(0);
            if (symbolChar == ANY) {
                throw new IllegalArgumentException(format("Input alphabet must not contain '%s' symbol", ANY));
            }
            if (symbolChar == BLANK) {
                throw new IllegalArgumentException(format("Input alphabet must not contain '%s' symbol", BLANK));
            }
            turingMachineBuilder.addInputAlphabetSymbol(symbolChar);
        }
    }

    @CommandLine.Option(names = {"--input-file-path", "-ip"})
    public void setInputDataPath(String path) throws IOException {
        inputData = Files.readString(Path.of(path));
    }

    @CommandLine.Option(names = {"--input-data", "-in"})
    public void setInputData(String inputData) {
        this.inputData = inputData;
    }

    @CommandLine.Option(names = {"--transition-function-file-path", "-tf"}, description = """
            Path to the file with transition function.
            
            Transition function must be defined as `δ : (Q \\ F ) × Γ → Q × Γ × {L, R, N }`, that is a projection
            from current state and symbol to next symbol, next state and a direction in which head should be moved
            (that determines which symbol is going to be read next).
            
            Q - character set of all states; Q element must be a single character
            F - set of all finite states or accepting states; F element must be a single character
            Γ - a symbol from the tape alphabet; Γ element must be a single character
            {L, R, N } - is direction (left, right, neutral [AKA do not move]}

            There must be maximum number of one transition rule per line, white spaces and lines that does not start
            with a character 'δ' are ignored. See an example below:
            
            # Rewrite 'aaaaa' to 'bbbbb;'
            δ : A × a → A × b × R
            δ : A × # → B × ; × N
            
            # Rewrite 'bbbbb;' to 'ccccc;'
            δ : B × ; → B × ; × L
            δ : B × b → B × c × L
            δ : B × # → C × c × N
            
            Please bear in mind that '×' (multiplication sign) and '→' (arrow U+2192) must not be part of the tape
            alphabet. The symbol '#' is used to mark the blank symbol.
            
            Be default Q, F, Γ elements state are auto-detected from transition function rules, that is final states are
            are states only found on the right side of the '→' and so on. The initial state can't be auto-detected, so
            that you need to define it like this:
            
            initial-state A
            
            The tape alphabet consist of input alphabet and special symbols, you can explicitly declare special symbols
            like that (just add the line anywhere):

            special-symbols , - ; @ .
            
            Parsed special symbols will be ',' ,'-', ';', '@' and '.'.
            
            Divide and conquer - many very complicated tasks can be reduced to subroutines of a Turing machine.
            The subroutine is a small set of states in the Turing machine that performs a small computation. You can
            like this:
            
            import-subroutine src/main/resources/subroutines/decrement.txt
            
            Imported subroutines are parsed identically as are lines in the main (this) file. It's simply an option to
            re-use transition rules at multiple places.
            
            In case you need to re-use subroutine in different situations and end with different state (as in different
            situations, you want to continue with different state), you can define subroutine instance entry and final 
            state like this
            
            import-subroutine [replace state ß with A and è with B ] src/main/resources/subroutines/copy.txt
            
            Statements above import 'decrement' subroutine and replace states ß and è for each instance import. Thus you
            have 3 possibly entry points for this subroutine and you can bind different follow-up states.
            
            Sometimes you need to match any character, in such case you can use special symbol '−'. For example when you
            want to reach the right-most non-blank symbols, you can do:

            δ : A × − → A × − × R
            # more concrete match has priority
            δ : A × # → B × # × L
            
            Probabilistic function is defined similarly to its deterministic twin:
            
            random δ : A × # → B × # × L
            random δ : A × # → C × # × L
            
            It must not change a symbol the tape head is on.
            
            """)
    public void setTransitionFunction(String path) throws IOException {
        final List<String> transitionRules = Files.readAllLines(Path.of(path));
        List<List<String>> subroutines = importSubroutines(transitionRules);
        if (subroutines.isEmpty()) {
            parseTransitionRules(transitionRules);
        } else {
            // prepend subroutines prior to transition rules
            parseTransitionRules(
                    Stream.concat(subroutines.stream().flatMap(List::stream), transitionRules.stream()).toList()
            );
        }
    }

    private List<List<String>> importSubroutines(List<String> transitionRules) {
        return transitionRules
                .stream()
                .filter(line -> line.startsWith("import-subroutine "))
                .map(line -> line.replace("import-subroutine ", "").trim())
                .map(line -> {
                    final String path;
                    // state replacements
                    final Map<Character, Character> oldStateToNewState;
                    if (line.startsWith("[replace state ")) {
                        oldStateToNewState = new HashMap<>();
                        var split = line.split("]");
                        if (split.length != 2) {
                            throw new IllegalArgumentException("Illegal definition of replacement state: "
                                    + Arrays.toString(split));
                        }
                        path = split[1].trim();
                        var stateReplacements = split[0].replace("[replace state ", "").split("and");
                        if (stateReplacements.length == 0) {
                            throw new IllegalArgumentException("At least one state replacement must be defined, got none");
                        }
                        for (String stateReplacement : stateReplacements) {
                            final var states = stateReplacement.trim().split("with");
                            if (states.length != 2) {
                                throw new IllegalArgumentException("Illegal state replacement definition, got "
                                        + stateReplacement);
                            }
                            final char replaceState = states[0].trim().charAt(0);
                            final char replacementState = states[1].trim().charAt(0);
                            oldStateToNewState.put(replaceState, replacementState);
                        }
                    } else {
                        path = line;
                        oldStateToNewState = Map.of();
                    }

                    try {
                        final Path of = Path.of(path);
                        if (oldStateToNewState.isEmpty()) {
                            return Files.readAllLines(of);
                        } else {
                            return Files.readAllLines(of)
                                    .stream()
                                    .map(line1 -> {
                                        if (line1.startsWith("δ")) {
                                            String newLine = "";
                                            for (char c1 : line1.toCharArray()) {
                                                newLine += oldStateToNewState.getOrDefault(c1, c1);
                                            }
                                            return newLine;
                                        } else {
                                            return line1;
                                        }
                                    })
                                    .toList();
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                })
                .map(subroutineLines -> {
                    var nestedSubroutines = importSubroutines(subroutineLines);
                    if (nestedSubroutines.isEmpty()) {
                        return subroutineLines;
                    } else {
                        // append nested subroutines to the end of the file
                        return Stream.concat(
                                subroutineLines.stream(), nestedSubroutines.stream().flatMap(List::stream)).toList();
                    }
                })
                .toList();
    }

    private void parseTransitionRules(List<String> transitionRules) {
        var fun = new TransitionFunction() {

            record From(char fromState, char fromSymbol) { }

            private final Map<Character, List<Action>> probabilisticStateToActions = parseProbabilisticMap(transitionRules);
            private final boolean hasProbabilisticTransitionFunctions = probabilisticStateToActions != null;
            private final Map<From, Action> transitionMap = parseMap(transitionRules);

            private static Map<Character, List<Action>> parseProbabilisticMap(List<String> transitionRules) {
                final Map<Character, List<Map.Entry<Character, Action>>> map = parseTransitionRules(transitionRules.stream()
                        .filter(line -> line != null && line.startsWith("random δ"))
                        .map(line -> line.replace("random ", "")))
                        .map(entry -> entry(entry.getKey().fromState(), entry.getValue()))
                        .collect(Collectors.groupingBy(new Function<Map.Entry<Character, Action>, Character>() {
                            @Override
                            public Character apply(Map.Entry<Character, Action> characterActionEntry) {
                                return characterActionEntry.getKey();
                            }
                        }));
                return map.entrySet().stream()
                        .map(entry -> entry(entry.getKey(), entry.getValue().stream().map(Map.Entry::getValue).toList()))
                        .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
            }


            private static Map<From, Action> parseMap(List<String> transitionRules) {
                return parseTransitionRules(transitionRules.stream())
                        .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
            }

            private static Stream<Map.Entry<From, Action>> parseTransitionRules(Stream<String> transitionRules) {
                record Parser(From from, Action action) {
                    private static final String SPLIT_BY = "×";

                    Parser(String[] arr) {
                        this(arr[0].split(Parser.SPLIT_BY), arr[1].split(Parser.SPLIT_BY));
                    }

                    Parser(String[] leftSideSplit, String[] rightSideSplit) {
                        this(new From(checkChar(leftSideSplit, 0), checkChar(leftSideSplit, 1)),
                                new Action(checkChar(rightSideSplit, 0), checkChar(rightSideSplit, 1),
                                        parseMove(rightSideSplit[2])));
                    }

                    private static Move parseMove(String move) {
                        return switch (move.trim()) {
                            case "N" -> Move.NEUTRAL;
                            case "L" -> Move.LEFT;
                            case "R" -> Move.RIGHT;
                            default -> throw new RuntimeException(
                                    format("'%s' is not a valid move, accepted values are L, R, N", move));
                        };
                    }

                    private static char checkChar(String[] split, int x) {
                        var character = split[x].trim();
                        if (character.length() != 1) {
                            throw new IllegalStateException(split[x] + " is not valid character");
                        }
                        return character.charAt(0);
                    }
                }

                return transitionRules
                        .filter(line -> line != null && line.startsWith("δ"))
                        // δ : B × b → B × c × L    =>    B × b → B × c × L
                        .map(line -> line.substring(line.indexOf(":") + 1).trim())
                        // B × b → B × c × L    =>    new String[] {"B × b", "B × c × L");
                        .map(line -> line.split("→"))
                        .peek(arr -> {
                            // validate
                            if (arr.length != 2 || arr[0].split(Parser.SPLIT_BY).length != 2 ||
                                    arr[1].split(Parser.SPLIT_BY).length != 3) {
                                throw new RuntimeException("Detected incorrect transition function format: " +
                                        String.join("→", arr));
                            }
                        })
                        .map(Parser::new)
                        .map(parser -> entry(parser.from(), parser.action()));
            }

            @Override
            public Action project(char state, char symbol) {
                if (hasProbabilisticTransitionFunctions && probabilisticStateToActions.containsKey(state)) {
                    // this is an analogy to additional tape used by probabilistic turing machines
                    // usually you would expect to have additional tape with random bits used to decide next state
                    // however there is no practical difference to using pseudorandom numbers to decide it as below
                    final var resultCandidates = probabilisticStateToActions.get(state);
                    if (resultCandidates.isEmpty()) {
                        throw new IllegalStateException("There must be at least one probabilistic transition function");
                    }
                    if (resultCandidates.size() == 1) {
                        throw new IllegalStateException(String.format("There must be at least 2 probabilistic " +
                                "transition functions for the state '%s'", state));
                    }
                    final var actionIndex = ThreadLocalRandom.current().nextInt(0, resultCandidates.size());
                    final var action = resultCandidates.get(actionIndex);
                    if (action.symbol() != symbol && action.symbol() != ANY) {
                        throw new IllegalStateException("Random transition function must not write symbol as only " +
                                "state is selected randomly; symbol can be changed in next step");
                    }
                    return new Action(action.state(), symbol, action.move());
                }

                var action = transitionMap.get(new From(state, symbol));
                if (action == null) {
                    action = transitionMap.get(new From(state, ANY));
                    if (action != null && action.symbol() == ANY) {
                        // if we are transiting to ANY, we want to keep previous symbol
                        action = new Action(action.state(), symbol, action.move());
                    }
                }
                return action;
            }
        };
        turingMachineBuilder.transitionFunction(fun);

        // extract initial state
        transitionRules
                .stream()
                .filter(line -> line != null && line.startsWith("initial-state "))
                .map(line -> line.replace("initial-state ", "").trim())
                .peek(state -> {
                    if (state.length() != 1) {
                        throw new RuntimeException(format("Initial state must be exactly one character, got '%s'",
                                state));
                    }
                })
                .map(state -> state.charAt(0))
                .forEach(turingMachineBuilder::initialState);

        // extract final states
        fun.transitionMap.values()
                .stream()
                .map(Action::state)
                .filter(state -> fun.transitionMap.keySet().stream().noneMatch(from -> from.fromState() == state))
                .filter(state -> fun.probabilisticStateToActions.keySet().stream()
                        .noneMatch(fromState -> fromState.charValue() == state))
                .forEach(turingMachineBuilder::addFinalState);

        // extract all states
        fun.transitionMap.entrySet().stream().<Character>mapMulti((entry, consumer) -> {
            consumer.accept(entry.getKey().fromState());
            consumer.accept(entry.getValue().state());
        }).forEach(turingMachineBuilder::addState);

        // extract special symbols
        final var specialSymbols = transitionRules
                .stream()
                .filter(line -> line != null && line.startsWith("special-symbols "))
                .map(line -> line.replace("special-symbols ", "").trim())
                .map(line -> line.split(" "))
                .flatMap(Stream::of)
                .map(String::trim)
                .filter(symbol -> !symbol.isEmpty())
                .peek(symbol -> {
                    if (symbol.length() != 1) {
                        throw new RuntimeException(
                                format("Special symbol must be exactly one character, but was '%s'", symbol));
                    }
                })
                .map(symbol -> symbol.charAt(0))
                .collect(Collectors.toUnmodifiableSet());
        final boolean hasSpecialSymbols;
        if (!specialSymbols.isEmpty()) {
            hasSpecialSymbols = true;
            turingMachineBuilder.specialSymbols(specialSymbols);
        } else {
            hasSpecialSymbols = false;
        }

        // extract input alphabet
        fun.transitionMap.entrySet().stream().<Character>mapMulti((entry, consumer) -> {
            final char fromSymbol = entry.getKey().fromSymbol();
            final char toSymbol = entry.getValue().symbol();
            if (hasSpecialSymbols) {
                if (!specialSymbols.contains(fromSymbol)) {
                    consumer.accept(fromSymbol);
                }
                if (!specialSymbols.contains(toSymbol)) {
                    consumer.accept(toSymbol);
                }
            } else {
                consumer.accept(fromSymbol);
                consumer.accept(toSymbol);
            }
        })
                .filter(symbol -> symbol != BLANK)
                .filter(symbol -> symbol != ANY)
                .forEach(turingMachineBuilder::addInputAlphabetSymbol);
    }

    @Override
    public void run() {
        final var configuration = computeInputData();
        LOG.infof("Final configuration - state '%s', tape '%s', problem instance time complexity '%d' and " +
                        "memory complexity '%d'", configuration.state(), new String(configuration.tape()),
                configuration.timeComplexity(), configuration.memoryComplexity());
        // FIXME count complexity as we know exact number of steps
    }

    TuringMachine.Configuration computeInputData() {
        Objects.requireNonNull(inputData);
        inputData = inputData.trim();
        final var turingMachine = turingMachineBuilder.build();
        final TuringMachine.Configuration configuration;

        // if string contains digits and conversion is enabled, converse them to binary numbers
        if (converseDecimalToBinary && NUMBER_PATTERN.matcher(inputData).find()) {

            configuration = turingMachine.compute(decimalToBinary(inputData.toCharArray()).toCharArray());
        } else {

            configuration = turingMachine.compute(inputData.toCharArray());
        }

        return configuration;
    }

    static String decimalToBinary(char[] charArr) {
        AtomicInteger maxBinaryNumberLength = new AtomicInteger(0);

        StringBuilder conversed = new StringBuilder();
        int digitStart = -1;
        for (int i = 0; i < charArr.length; i++) {
            if (Character.isDigit(charArr[i])) {
                if (digitStart == -1) {
                    digitStart = i;
                }
            } else if (digitStart != -1) {
                digitStart = addBinaryNumber(charArr, conversed, digitStart, i, maxBinaryNumberLength);
            } else {
                conversed.append(charArr[i]);
            }
        }

        // handle remainder
        if (digitStart != -1) {
            addBinaryNumber(charArr, conversed, digitStart, charArr.length, maxBinaryNumberLength);
        }

        return conversed.toString();
    }

    private static int addBinaryNumber(char[] charArr, StringBuilder conversed, int digitStart, int i,
                                       AtomicInteger maxBinaryNumberLength) {
        // converse decimal to binary number
        StringBuilder digitAsStr = new StringBuilder();
        for (int j = digitStart; j < i; j++) {
            digitAsStr.append(charArr[j]);
        }
        String binaryNumber = Long.toBinaryString(Long.parseLong(digitAsStr.toString()));
        // add decimal number as binary
        if (maxBinaryNumberLength.get() == 0) {
            conversed.append(binaryNumber);
            maxBinaryNumberLength.set(binaryNumber.length());
        } else {
            // this is primitive mechanism to ensure binary numbers are going to be of same length,
            // but it only works as long as the left-most number is the largest one
            // it's here in order to assure division (tests) works
            if (maxBinaryNumberLength.get() > binaryNumber.length()) {

                // here we add extra zero: max binary number length - this binary number length
                final var extraZeros =  "0".repeat(maxBinaryNumberLength.get() - binaryNumber.length());
                conversed.append(extraZeros);

                conversed.append(binaryNumber);
            } else {
                maxBinaryNumberLength.set(binaryNumber.length());
                conversed.append(binaryNumber);
            }
        }

        // reset
        digitStart = -1;

        // append current char if there is any
        // the condition is false when this is the last char of the string
        if (i < charArr.length) {
            conversed.append(charArr[i]);
        }

        return digitStart;
    }
}
