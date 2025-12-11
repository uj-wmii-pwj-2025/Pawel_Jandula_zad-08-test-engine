package uj.wmii.pwj.anns;

public class MyBeautifulTestSuite {

    // --- PRZYPADKI PASS ---

    @MyTest(params = {"10", "5"}, results = {"100", "25"})
    public int square(int x) {
        return x * x;
    }

    @MyTest(params = {"Java", "Engine"}, results = {"JAVA", "ENGINE"})
    public String toUpper(String s) {
        return s.toUpperCase();
    }

    // --- PRZYPADKI FAIL ---

    @MyTest(params = {"2", "3"}, results = {"4", "10"}) // FAIL
    public int squareFail(int x) {
        return x * x;
    }

    @MyTest(params = {"true"}, results = {"false"}) // nie fail
    public boolean negate(boolean b) {
        return !b;
    }

    // --- PRZYPADKI ERROR ---

    @MyTest(params = {"some text"})
    public void boom(String s) {
        throw new RuntimeException("Something exploded!");
    }
    
    // --- BEZ PARAMETRÓW ---
    
    @MyTest(results = {"7"})
    public int returnSeven() {
        return 7;
    }
}