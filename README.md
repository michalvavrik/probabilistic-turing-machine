# Probabilistic Turing machine - Fermat primality test

This project includes Turing Machine implemented in Java (JDK 17) and convenient CLI interface that allows you to pass
Turing machine definition and input data. The project leverages Quarkus so that you can create native application that
runs independently on JVM (as any other native app). To simply run Fermat primality test, please execute following
commands in project root directory:

```shell script
mvn clean package
# is 14 a prime number? 
# (division operation implemented here is not efficient, do not use big numbers unless you have hours...)
java -jar target/quarkus-app/quarkus-run.jar -in=14 -tf=fermat-primality-test.txt
# if you need to print out all Turing machine configurations, just run
# but please keep in mind logging has heavy impact on performance
java -jar target/quarkus-app/quarkus-run.jar -in=14 -tf=fermat-primality-test.txt -Dquarkus.log.level=DEBUG -Dquarkus.log.min-level=DEBUG
```

In case you are not familiar with Quarkus or run into any obstacle, following [build guide](https://github.com/michalvavrik/probabilistic-turing-machine/blob/master/BUILD_MANUAL.md) will help you hit the ground running.
If you want to see all configuration options you can [read `@CommandLine.Option` here](https://github.com/michalvavrik/probabilistic-turing-machine/blob/master/src/main/java/edu/michalvavrik/ptm/StartCommand.java), or just run:

```shell script
java -jar target/quarkus-app/quarkus-run.jar --help
```

The source code is documented (contract is established) through unit tests, so if in doubt about what's expected behavior, 
or if you run into any issues at all, I'd strongly recommend to check out tests.

# Pravděpodobnostní Turingův stroj

Pravděpodobnostní Turingův stroj (PTS) umožňuje aby se vybrané přechody mezi stavy řídili podle výsledku nějakého náhodného jevu.
Stejně tak je možné upřednostnit či upozadit některé volby (následné stavy) pomocí váhových koeficientů. Tato verze Turingova
stroje (TS) umožňuje řešit stejnou třídu problémů jako deterministické TS a za určitých okolností dojít k výsledku rychleji.
Klasický TS nabízí reprodukovatelnost a jistotu, že dojde k výsledku, oproti tomu PTS umožňuje (potenciálně) rychlejší výsledek.

Prakticky si lze PTS představit jako TS s přidanou páskou, která obsahuje náhodně vygenerované bity. PTS může tuto pásku
ve vybraných případech využít k volbě následujícího stavu a pohybu hlavy, a proto pro jedno vstupní slovo může existovat více výpočtů.
Při takovémto zpracování PTS nemůže dojít za poslední náhodný bit, to jest, nemůže testovat více pravděpodobnostních předchodů než je délka přidané pásky.

V tomto projektu je PTS zpracován jako deterministický TS jež umožňuje vybrané instance přechodových funkcí označit za pravděpodobnostní.
Pro každý stav je možné definovat 2 a více následujících stavů, přičemž [konkrétní stav je vybrán](https://github.com/michalvavrik/probabilistic-turing-machine/blob/master/src/main/java/edu/michalvavrik/ptm/StartCommand.java#L332) 
pomocí [ThreadLocalRandom](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/ThreadLocalRandom.html#nextInt--)
jenž využívá lineární kongruenční generátor pseudonáhodných čísel D. H. Lehmera.

## Pravděpodobnostní test prvočísel

Deterministické testy prvočíselnosti jako je test hrubou silou (exponenciální složitost) a polynomiální test prvočíselnosti (polynomiální složitost)
zaručují správný výsledek - určí se 100 % přesností zda-li je číslo prvočíslem nebo ne. Potíž je v únosnosti takového testování, kupříkladu
pokud by počítač vykonal miliardu dělení za sekundu, pak otestování prvočíselnosti 100-místného čísla trvalo prvně zmíněným algoritmem 10^33 let 
a 10^7 deterministickým AKS algoritmem, viz Alena Gollová - Testy prvočíselnosti.

Pravděpodobnostní testy prvočíselnosti vlastností, které pokud je zvolené číslo prvočíslo, tak jsou platné pro všechny prvky testovací množiny (označmě číslo `a`, přičemž `a ∈ Z*` a `Z*` je uzávěra testovací množina).
Pokud zvolené číslo není prvočíslem, pak platí jen pro některé `a` (tzv. falešné svědky), což ale znamená, že v závislosti na volbě
určitých `a` můžeme dospět k chybnému výsledku - označit složené číslo za prvočíslo. Opačně se však zmýlit nelze (označit prvočíslo za složené číslo),
proto se jedná o testy s jednostrannou chybou.

### Fermatův test

V případě Fermatova testu je testovací množina množinou celých čísel daná Malou Fermatovou větou.
Ta tvrdí, že pro každé prvočíslo `p` a každé celé číslo `a` platí `a^(n-1)≡1 (mod n)`. Volně přeloženo, pokud je `n` prvočíslo,
pak libovolné celé číslo umocněné na `n-1` bude mít zbytek po celočíselném dělení roven `1`. Existují též složené číslo,
pro jejichž každé `a` je splněna podmínka výše, tyto čísla se pak nazývají Carmichaelova čísla.
Carmichaelova čísla jsou od prvočísel Fermatovým testem k nerozeznání.

Odhad časové složitosti pro Fermatův test je `n^3` (při použití algoritmu opakovaných čtverců, viz Gollová, Testy prvočíselnosti).
Algoritmus který se používá volí složená čísla která nejsou Carmichaelovými čísly a počítá výše uvedený vzorec `k` krát (`k` odpovídá počtu vybraných `a`), 
z čehož vyplývá že časová náročnost je `k*n^3`. Pravděpodobnost chyby (pravděpodobnost že jsme nalezli Fermatovo pseudoprimární číslo) je pak `1/2^k`.

Algoritmus který jsem [implementoval já (míním tento projekt)](https://github.com/michalvavrik/probabilistic-turing-machine/blob/master/fermat-primality-test.txt) 
se od výše popsaného liší v jednom kroku - nevylučuje Carmichaelova čísla (ze zjevného důvodu - jediné Carmichaelovo číslo 
menší než 1000 je 561 a nedává smysl zkoušet vyšší čísla kvůli délce vypočtu, viz níže) a vždy volím `k=1` (testuji právě jedno `a`).
Definiční obor implementovaného PTS je `n>2` a přestože algoritmus je obecný (věřím že funguje i pro vetší čísla), je díky operaci dělení
takřka nepoužilený pro vyšší čísla. Například test kongruence pro `a=7` a `n=8` trvá na mém notebooku půl hodiny a pro `a=8, n=9` hodinu.
A proto aby byl dělenec co nejmenší, je algoritmus pro volbu `a` následující: polož `a=2` a proveď pravděpodobnostní přechod na následující stav.
Následující stavy jsou 2 - první přejde ke kongruenci, druhý inkrementuje `a` a opakuje pravděpodobnostní přechod. 
Pokud je však `a=n-1`, tak vždy přejdi ke kongruenci (neinkrementuj - bez této podmníky by se nejednalo o Fermatův test).
Tímto způsobem se zvyšuje pravděpodobnost malého `a`.

Program rozhodne konečným stavem pásky, 1 pokud je n prvočíslo a 0 pokud n prvočíslo není. Časová složitost (počet kroků - počet aplikací přechodové funkce) 
i paměťová složitost (počet kroků kdy se změnil zapisovaný symbol) instance problému je vypsaná do STD OUT (zpravidla konzole).
Provést odhad časové složitosti tohoto algoritmu pro určitou délku slova je obtížně, protože vzhledem ke komplexnosti algoritmických operací (realisticky) nelze určit nejhorší scénář.

## Použité elektronické zdroje

- [Pravděpodobnostní Turingův stroj](https://www2.karlin.mff.cuni.cz/~holub/soubory/qc/node14.html)
- [Testy prvočíselnosti](https://math.fel.cvut.cz/en/people/gollova/mkr/mkr9a.pdf)
- [Generování náhodných prvočísel](https://math.fel.cvut.cz/en/people/gollova/mkr/mkr8a.pdf)
- [Polynomiální hierarchie - 5.3 Třída BPP](http://www.cs.cas.cz/~savicky/vyuka/vypsl/vypsl2016ls2.pdf)
- [Vyčíslitelnost a složitost 1, 2](https://web.osu.cz/~Habiballa/vyuka/vycislitelnost-a-slozitost-1/)
- [Turing Machines lectures](https://www.cs.princeton.edu/courses/archive/fall13/cos126/lectures/18-Turing-2x2.pdf)
- [Binary Division Turing Machine](https://github.com/Rishabhkandoi/Binary-Division-Turing-Machine)
- [Turing Machine Simulation](https://math.hws.edu/eck/js/turing-machine/TM-info.html)
- [Turing Machines supplement](https://cs.uwaterloo.ca/~nishi/360W16/Resources/tmsupplement.pdf)
- [The Turing Machine and Computational Complexity](http://iiitdm.ac.in/old/Faculty_Teaching/Sadagopan/pdf/ADSA/new/scribe-TM-intro.pdf)
- [Turing machines and computabile functions](https://www.hse.ru/mirror/pubs/share/167262307)
- [Formal Language Theory](https://people.cs.umass.edu/~marius/class/cs501/lec34-nup.pdf)
- [Complexity theory](https://www.cse.iitd.ac.in/~rjaiswal/2014/csl853/Notes/Week-09/lec-1.pdf)
- [Fermat primality test](https://en.wikipedia.org/wiki/Fermat_primality_test)
- [Turing machines - Java](https://introcs.cs.princeton.edu/java/52turing/)
- [Turing Machine Review](https://www.usna.edu/Users/cs/wcbrown/courses/F00SI472/classes/C26/Class/Class.html)
- [Turing Machines (Small)](http://web.stanford.edu/class/archive/cs/cs103/cs103.1172/lectures/21/Small21.pdf)
- [Lecture T1: Turing Machines](https://www.cs.princeton.edu/courses/archive/spr03/cs126/lectures/T1-4up.pdf)
- [Turing Machines part II](https://web.stanford.edu/class/archive/cs/cs103/cs103.1142/lectures/19/Small19.pdf)
