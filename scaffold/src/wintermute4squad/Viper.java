package wintermute4squad;
import battlecode.common.*;

public class Viper 
{
	public static RobotController rc;

	public static void run()
	{
		rc = RobotPlayer.rc;
		while(true)
		{
			Clock.yield();
		}
	}
}
