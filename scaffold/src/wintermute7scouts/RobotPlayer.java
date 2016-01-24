package wintermute7scouts;
import battlecode.common.*;

public class RobotPlayer 
{
	public static RobotController rc;
	public static void run(RobotController rc) throws Exception
	{
		RobotPlayer.rc = rc;
		RobotType myType = rc.getType();
		
		//run methods
        if(myType == RobotType.ARCHON)
        {
        	Archon.run();
        }
        else if(myType == RobotType.SCOUT)
        {
        	Scout.run();
        }
        else if(myType == RobotType.SOLDIER)
        {
        	Soldier.run();
        }
        else if(myType == RobotType.TURRET)
        {
        	Turret.run();
        }
        else if(myType == RobotType.GUARD)
        {
        	Guard.run();
        }
        else if(myType == RobotType.VIPER)
        {
        	Viper.run();
        }
        else
        {
        	throw new Exception("That's not a robot type I'm familiar with.");
        }
	}
}
