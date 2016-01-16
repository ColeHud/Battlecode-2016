package wintermute4squad;
import java.util.*;

import battlecode.common.*;

public class Turret 
{
	public static RobotController rc;

	//slug trail for pathing
	public static ArrayList<MapLocation> slugTrail = new ArrayList<MapLocation>(20);
	
	//goal maplocation
	public static MapLocation goal = null;
	
	public static Random rand;

	public static void run() throws GameActionException
	{
		rc = RobotPlayer.rc;
		rand = new Random(rc.getID());
		while(true)
		{

			Clock.yield();
		}
	}
	
	//move to open adjacent location
	public static void moveToOpenAdjacentLocation() throws GameActionException
	{
		ArrayList<Direction> directions = Utility.arrayListOfDirections();
		while(directions.size() > 0)
		{
			int index = rand.nextInt(directions.size());
			if(rc.canMove(directions.get(index)) && rc.isCoreReady())
			{
				rc.move(directions.get(index));
			}
			else
			{
				directions.remove(index);
			}
		}
	}

	//move to a location
	public static void moveToLocation(MapLocation location) throws GameActionException
	{	
		if(rc.isCoreReady())
		{
			if(rc.getType() == RobotType.TURRET)
			{
				rc.pack();
			}

			MapLocation currentLocation = rc.getLocation();

			//trim the slug trail to size 20
			slugTrail.add(currentLocation);
			if(slugTrail.size() > 20)
			{
				slugTrail.remove(0);
			}

			Direction candidateDirection = currentLocation.directionTo(location);
			MapLocation locationInDirection = currentLocation.add(candidateDirection);
			double rubbleAtLocation = rc.senseRubble(locationInDirection);

			if(slugTrail.contains(locationInDirection))//if you've just been there
			{
				for(int i = 0; i < 8; i++)
				{
					candidateDirection = candidateDirection.rotateRight();
					locationInDirection = currentLocation.add(candidateDirection);
					rubbleAtLocation = rc.senseRubble(locationInDirection);

					if(rc.isCoreReady() && rc.canMove(candidateDirection) && slugTrail.contains(locationInDirection) == false && rubbleAtLocation < GameConstants.RUBBLE_OBSTRUCTION_THRESH)//move there then return
					{
						rc.setIndicatorString(0, "Trying to move");
						rc.move(candidateDirection);
						return;
					}

				}
			}
			else
			{
				if(rc.isCoreReady() && rc.canMove(candidateDirection) && rubbleAtLocation < GameConstants.RUBBLE_OBSTRUCTION_THRESH)
				{
					rc.move(candidateDirection);
				}
				else
				{
					for(int i = 0; i < 8; i++)
					{
						candidateDirection = candidateDirection.rotateRight();
						locationInDirection = currentLocation.add(candidateDirection);
						rubbleAtLocation = rc.senseRubble(locationInDirection);

						if(rc.isCoreReady() && rc.canMove(candidateDirection) && slugTrail.contains(locationInDirection) == false && rubbleAtLocation < GameConstants.RUBBLE_OBSTRUCTION_THRESH)//move there then return
						{
							rc.setIndicatorString(0, "Trying to move");
							rc.move(candidateDirection);
							return;
						}

					}
				}
			}
		}
	}
}
