package uj.wmii.pwj.anns;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class MyTestEngine {

    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_CYAN = "\u001B[36m";

    private final String className;

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Please specify test class name");
            System.out.println("No arguments provided. Running default suite...");
            args = new String[]{"uj.wmii.pwj.anns.MyBeautifulTestSuite"};
        }
        
        printAsciiArt();
        
        String className = args[0].trim();
        System.out.printf("Loading test suite: %s\n", className);
        System.out.println("--------------------------------------------------");

        MyTestEngine engine = new MyTestEngine(className);
        engine.runTests();
    }

    public MyTestEngine(String className) {
        this.className = className;
    }

    public void runTests() {
        final Object unit = getObject(className);
        if (unit == null) return;

        List<Method> testMethods = getTestMethods(unit);
        System.out.printf("Found %d test methods. Starting execution...\n\n", testMethods.size());

        int passCount = 0;
        int failCount = 0;
        int errorCount = 0;

        for (Method m : testMethods) {
            List<TestResult> results = launchSingleMethod(m, unit);
            
            for (TestResult r : results) {
                switch (r) {
                    case PASS:
                        passCount++;
                        break;
                    case FAIL:
                        failCount++;
                        break;
                    case ERROR:
                        errorCount++;
                        break;
                }
            }
        }

        printSummary(passCount, failCount, errorCount);
    }

    private List<TestResult> launchSingleMethod(Method m, Object unit) {
        List<TestResult> results = new ArrayList<>();
        MyTest annotation = m.getAnnotation(MyTest.class);
        String[] params = annotation.params();
        String[] expectedResults = annotation.results();

        if (params.length == 0) {
            String expected = expectedResults.length > 0 ? expectedResults[0] : null;
            results.add(executeTest(m, unit, null, expected));
        } else {
            for (int i = 0; i < params.length; i++) {
                String input = params[i];
                String expected = (i < expectedResults.length) ? expectedResults[i] : null;
                results.add(executeTest(m, unit, input, expected));
            }
        }
        return results;
    }

    private TestResult executeTest(Method m, Object unit, String inputParam, String expectedOutput) {
        System.out.print("Test [" + m.getName() + "]");
        if (inputParam != null) System.out.print(" with args (" + inputParam + ")");
        System.out.print(" ... ");

        try {
            Object result;
            if (inputParam != null) {
                Object typedArg = convert(m.getParameterTypes()[0], inputParam);
                result = m.invoke(unit, typedArg);
            } else {
                result = m.invoke(unit);
            }

            if (expectedOutput == null) {
                printResult(TestResult.PASS, "OK (No expectation)");
                return TestResult.PASS;
            }

            String actualString = String.valueOf(result);
            
            if (actualString.equals(expectedOutput)) {
                printResult(TestResult.PASS, "OK");
                return TestResult.PASS;
            } else {
                printResult(TestResult.FAIL, "Expected: " + expectedOutput + ", Got: " + actualString);
                return TestResult.FAIL;
            }

        } catch (InvocationTargetException e) {
            printResult(TestResult.ERROR, "Exception: " + e.getCause().getClass().getSimpleName());
            return TestResult.ERROR;
        } catch (Exception e) {
            printResult(TestResult.ERROR, "Engine Error: " + e.getMessage());
            return TestResult.ERROR;
        }
    }
    
    private void printResult(TestResult result, String msg) {
        switch (result) {
            case PASS:
                System.out.println(ANSI_GREEN + "[PASS] " + ANSI_RESET + msg);
                break;
            case FAIL:
                System.out.println(ANSI_RED + "[FAIL] " + ANSI_RESET + msg);
                break;
            case ERROR:
                System.out.println(ANSI_YELLOW + "[ERROR] " + ANSI_RESET + msg);
                break;
        }
    }

    private Object convert(Class<?> targetType, String value) {
        if (targetType.equals(String.class)) return value;
        if (targetType.equals(int.class) || targetType.equals(Integer.class)) return Integer.parseInt(value);
        if (targetType.equals(boolean.class) || targetType.equals(Boolean.class)) return Boolean.parseBoolean(value);
        if (targetType.equals(double.class) || targetType.equals(Double.class)) return Double.parseDouble(value);
        return value;
    }

    private static void printAsciiArt() {
        System.out.println("  _______        _     ______             _            ");
        System.out.println(" |__   __|      | |   |  ____|           (_)           ");
        System.out.println("    | | ___  ___| |_  | |__   _ __   __ _ _ _ __   ___ ");
        System.out.println("    | |/ _ \\/ __| __| |  __| | '_ \\ / _` | | '_ \\ / _ \\");
        System.out.println("    | |  __/\\__ \\ |_  | |____| | | | (_| | | | | |  __/");
        System.out.println("    |_|\\___||___/\\__| |______|_| |_|\\__, |_|_| |_|\\___|");
        System.out.println("                                     __/ |             ");
        System.out.println("                                    |___/              ");
    }

    private void printSummary(int pass, int fail, int error) {
        System.out.println("\n==================================================");
        System.out.println("                  TEST SUMMARY                    ");
        System.out.println("==================================================");
        System.out.println("PASS:  " + ANSI_GREEN + pass + ANSI_RESET);
        System.out.println("FAIL:  " + ANSI_RED + fail + ANSI_RESET);
        System.out.println("ERROR: " + ANSI_YELLOW + error + ANSI_RESET);
        System.out.println("TOTAL: " + (pass + fail + error));
        System.out.println("==================================================");
    }

    private static List<Method> getTestMethods(Object unit) {
        Method[] methods = unit.getClass().getDeclaredMethods();
        return Arrays.stream(methods)
                .filter(m -> m.getAnnotation(MyTest.class) != null)
                .collect(Collectors.toList());
    }

    private static Object getObject(String className) {
        try {
            Class<?> unitClass = Class.forName(className);
            return unitClass.getConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            e.printStackTrace();
            return null;
        }
    }
}