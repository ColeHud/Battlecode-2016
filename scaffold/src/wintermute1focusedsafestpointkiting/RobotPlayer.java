package wintermute1focusedsafestpointkiting;
import battlecode.common.*;

import java.util.*;

//By Joseph Gnehm and Cole Hudson
//wintermute 1 builds off of things learned from wintermute 0, badplayer, and sillyplayer

public class RobotPlayer 
{
	public static RobotType myType;
	public static RobotController rc;
	
	public static void run(RobotController robotController) throws Exception
	{
		rc = robotController;
		myType = rc.getType();
		
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
        else
        {
        	throw new Exception("That's not a robot type I'm familiar with.");
        }
	}
}
