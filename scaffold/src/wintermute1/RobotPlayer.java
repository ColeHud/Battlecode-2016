package wintermute1;
import battlecode.common.*;

import java.util.*;

//By Joseph Gnehm and Cole Hudson
//wintermute 1 builds off of things learned from wintermute 0, badplayer, and sillyplayer

public class RobotPlayer 
{
	public static RobotType myType;
	
	public static void run(RobotController rc) throws Exception
	{
		myType = rc.getType();
		
		//run methods
        if(myType == RobotType.ARCHON)
        {
        	
        }
        else if(myType == RobotType.SCOUT)
        {
        	
        }
        else if(myType == RobotType.SOLDIER)
        {
        	
        }
        else if(myType == RobotType.TURRET)
        {
        	
        }
        else
        {
        	throw new Exception("That's not a robot type I'm familiar with.");
        }
	}
}
