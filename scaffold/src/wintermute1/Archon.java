package wintermute1;
import battlecode.common.*;

import java.util.*;

public class Archon 
{
	public static Random rand;
	public static boolean hasBuiltScout = false;
	public static RobotController rc;

	public static void run() throws GameActionException
	{
		rc = RobotPlayer.rc;
		rand = new Random(rc.getID());

		while(true)
		{
			buildRobots();

			Clock.yield();
		}
	}
	
	//try to build a single robot
	public static void tryToBuildARobot(RobotType type) throws GameActionException
	{
		boolean hasBuilt = false;
		int index = 0;
		Direction[] directions = Direction.values();
		while(hasBuilt == false && index < directions.length)
		{
			Direction dirToTry = directions[index];
			if(rc.canBuild(dirToTry, type) && rc.isCoreReady())
			{
				rc.build(dirToTry, type);
				hasBuilt = true;
			}
			index++;
		}
	}

	//build robots
	public static void buildRobots() throws GameActionException
	{
		//build A scout if you haven't yet
		if(hasBuiltScout == false && rc.hasBuildRequirements(RobotType.SCOUT))
		{
			tryToBuildARobot(RobotType.SCOUT);
			hasBuiltScout = true;
		}
		else
		{
			double whatToBuild = Math.random();
			if(whatToBuild > .1 && rc.hasBuildRequirements(RobotType.SOLDIER))//build a soldier
			{
				rc.setIndicatorString(0, "Trying to build a soldier");
				tryToBuildARobot(RobotType.SOLDIER);
			}
			else if(rc.hasBuildRequirements(RobotType.TURRET))//build a turret
			{
				rc.setIndicatorString(0, "Trying to build a turret");
				tryToBuildARobot(RobotType.TURRET);
			}
		}
	}
}
