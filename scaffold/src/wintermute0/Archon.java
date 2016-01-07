package wintermute0;
import battlecode.common.*;

public class Archon 
{
	//variables
	public static RobotController rc = Robot.rc;
	public static int numScouts = 0;
	
	public static MapLocation parts;
	
	public static String[] possibleGoals = {"Go to parts", "evade enemy"};
	public static int goalNum;

	public static void run() throws GameActionException
	{
		while(true)
		{
			moveToParts();

			checkMessageForParts();

			buildTwoScouts();

			Clock.yield();
		}
	}

	//move to parts SUPER LAME, NEEDS TO BE FIXED
	public static void moveToParts() throws GameActionException 
	{
		if(parts != null)
		{	
			rc.setIndicatorString(1, "In the loop");
			if(rc.getLocation().equals(parts))
			{
				rc.setIndicatorString(0, "Guess who got the parts!?!?!");
				rc.setIndicatorString(1, "Out of the loop");
				//reset the parts location
				parts = null;
				return;
			}

			Direction dirToParts = rc.getLocation().directionTo(parts);
			if(rc.canMove(dirToParts) && rc.isCoreReady())
			{
				rc.move(dirToParts);
			}

			if(rc.senseRubble(rc.getLocation().add(dirToParts)) >= 50)
			{
				if(rc.isCoreReady())
				{
					rc.clearRubble(dirToParts);
				}
			}
		}
	}

	public static void checkMessageForParts()
	{
		Signal signal = rc.readSignal();

		if(signal != null && parts == null)
		{
			MapLocation signalLoc = signal.getLocation();
			//if it's a parts signal
			if(signal.getMessage()[0] == 94572)
			{
				parts = signalLoc;
			}
		}	
	}

	//build 2 scouts for each archon
	public static void buildTwoScouts() throws GameActionException
	{
		if(numScouts < 2)
		{
			for(Direction d : Robot.directions)
			{
				if(rc.canBuild(d, RobotType.SCOUT) && numScouts < 2 && rc.isCoreReady())
				{
					rc.build(d, RobotType.SCOUT);
					numScouts++;
					break;
				}
			}
		}
	}
}
