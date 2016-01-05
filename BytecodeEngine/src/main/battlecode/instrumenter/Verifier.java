package battlecode.instrumenter;

/**
 * Used to verify that a team's submission will pass the instrumenter when it's run.
 * <p/>
 * Usage:<br>
 * <code>java Verifier teamXXX</code><br>
 * If the submission passes, the Java process will return 0, and there will be no output to stderr.  If the
 * submission fails, the Java process will return 1, and there will be error messages in stderr.
 *
 * @author adamd
 */
public class Verifier {
    public static void main(String[] args) {
        String teamPackageName = args[0];
        try {
            IndividualClassLoader icl = new IndividualClassLoader(teamPackageName);
            icl.loadClass(teamPackageName + ".RobotPlayer");
        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace(System.out);
            System.exit(1);
        }
    }
}
