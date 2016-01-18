package wintermute5kiting;
import battlecode.common.*;

public class Viper 
{
	public static RobotController rc;

	public static void run() throws GameActionException
	{
		rc = RobotPlayer.rc;
		while(true)
		{
			if(rc.getHealth() < 10)
			{
				if(rc.isCoreReady() && rc.isWeaponReady() && rc.canAttackLocation(rc.getLocation()))
				{
					rc.attackLocation(rc.getLocation());
				}
			}
			Clock.yield();
		}
	}
}
