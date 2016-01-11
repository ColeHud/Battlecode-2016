package wintermute1;
import battlecode.common.*;

import java.util.*;

/* Rough overview of what the soldiers will do: 
 * 
 */

//has momentum but can set to 0
//protectors not implemented yet, first see how the basic guys work
//all have to stay a little away from archons

public class Soldier 
{
	public static Random rand;
	public static RobotController rc;
	public static Direction[] directions = {Direction.NORTH, Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST,
											Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST};
	public static int numDirections = directions.length;
	public static int maxMomentum = 0; //how many turns to keep going in a direction, if no guidance to change it
	public static int momentum = maxMomentum;
	//public static float probProtector = 0.2; //might change based on GameConstants.NUMBER_OF_ARCHONS_MAX
											   //not sure if that's the max for the specific map
											   //should protect a specific archon? Don't think that's a great idea
	// continues might be a bad idea
	public static void run() throws GameActionException
	{
		rc = RobotPlayer.rc;
		rand = new Random(rc.getID());
		
		//NOT DONE: make some (20%) into protectors
		//boolean isProtector = rand.nextFloat() < probProtector;
		
		MapLocation goalLoc = new MapLocation(-1, -1); // MapLocation(-1, -1) used as "no location" or null
		Direction dirToMove = Direction.NONE;
		boolean offCourse = false; //whether the soldier turned in getting to a location
								   //means will have to recompute the direction to the goal
		int closeEnoughSquared = 4; //how close you have to get to a goalLoc (squared)
		int stepsLeft = 100; //max number of steps to take to get to a goalLoc (don't want to try forever)
		                     //in code depends on distance from myLoc to goalLoc
		double tooMuchRubble = 50; //how much rubble there has to be so that the soldiers don't try to clear it
		boolean foesMaybeNearby = true; //used to restart while loop
		MapLocation myLoc = rc.getLocation();
		
		//should also try to get closer to enemies that a soldier can just sense but not attack?
		while(true)
		{
			//try to attack, if successful then finish turn
			//does not prioritize who to attack for now
			if(foesMaybeNearby && rc.isWeaponReady())
			{
				RobotInfo[] foes = rc.senseHostileRobots(myLoc, RobotPlayer.myType.attackRadiusSquared);
				if(foes.length > 0)
				{
					for(RobotInfo foe : foes) //does randomizing make a difference here?
					{
						MapLocation foeLoc = foe.location;
						if(rc.canAttackLocation(foeLoc))
						{
							rc.attackLocation(foeLoc);
							break;
						}
					}
				}
				else //no foes nearby
				{
					foesMaybeNearby = false;
					continue;
				}
			}
			else
			{
				if(isLocNull(goalLoc))
				{
					boolean gotNewGoalLoc = false;
					Signal[] signals = rc.emptySignalQueue();
					for(Signal signal : signals)
					{
						if((signal.getMessage() != null) && (signal.getMessage()[0] == Utility.SOLDIER_HELP_CODE))
						{
							goalLoc = signal.getLocation();
							stepsLeft = myLoc.distanceSquaredTo(goalLoc); //not sure what would be better
							dirToMove =  myLoc.directionTo(goalLoc);
							gotNewGoalLoc = true;
							break;
						}
					}
					if(gotNewGoalLoc)
					{
						continue;
					}
					else //move randomly
						 //this code is copied some below
					{
						if(rc.isCoreReady())
						{
							int timesRotated = 0;
							boolean done = false;
							boolean turnLeft = rand.nextBoolean(); //if true keep turning left, if false keep turning right
							if(momentum <= 0 || dirToMove == Direction.NONE)
							{
								dirToMove = directions[rand.nextInt(directions.length - 1)]; //random dir
								momentum = maxMomentum; //reset momentum
							}
							else
							{
								momentum --; //should do this here or only when have moved?
							}
							while((timesRotated < numDirections) && (! done))
							{
								double rubble = rc.senseRubble(myLoc.add(dirToMove));
								if(rubble > GameConstants.RUBBLE_OBSTRUCTION_THRESH)
								{
									if(rubble >= tooMuchRubble) //try another direction
									{
										dirToMove = turn(dirToMove, turnLeft);
										timesRotated ++;
									}
									else //clear the rubble
									{
										rc.clearRubble(dirToMove);
										done = true;
									}
								}
								else
								{
									if(rc.canMove(dirToMove))
									{
										rc.move(dirToMove);
										stepsLeft --;
										done = true;
										myLoc = rc.getLocation();
									}
									else
									{
										dirToMove = turn(dirToMove, turnLeft);
										timesRotated ++;
									}
								}
							}
						}
					}
				}
				else //continue towards goalLoc
				{
					if(rc.isCoreReady())
					{
						if((myLoc.distanceSquaredTo(goalLoc) <= closeEnoughSquared) || (stepsLeft <= 0)) // done
						{
							goalLoc = new MapLocation(-1, -1); //the null loc
							dirToMove = Direction.NONE;
							continue; //didn't use that much bytecode to get here, still might be a mistake
						}
						else
						{
							if(offCourse)
							{
								dirToMove =  myLoc.directionTo(goalLoc);
								offCourse = false;
							}
							//most of the code is copied from moving randomly (see above)
							//but has no momentum, start direction is towards goal, and have to toggle offCourse
							int timesRotated = 0;
							boolean done = false; //whether or not has moved or cleared some rubble
							boolean turnLeft = rand.nextBoolean(); //if true keep turning left, if false keep turning right
							while((timesRotated < numDirections) && (! done))
							{
								double rubble = rc.senseRubble(myLoc.add(dirToMove));
								if(rubble >= GameConstants.RUBBLE_OBSTRUCTION_THRESH)
								{
									if(rubble >= tooMuchRubble) //try another direction
									{
										dirToMove = turn(dirToMove, turnLeft);
										timesRotated ++;
										offCourse = true;
									}
									else //clear the rubble
									{
										rc.clearRubble(dirToMove);
										done = true;
									}
								}
								else
								{
									if(rc.canMove(dirToMove))
									{
										rc.move(dirToMove);
										stepsLeft --;
										done = true;
										myLoc = rc.getLocation();
									}
									else
									{
										dirToMove = turn(dirToMove, turnLeft);
										timesRotated ++;
										offCourse = true;
									}
								}
							}
						}
					}
					foesMaybeNearby = true;
				}
			}
			Clock.yield();
		}
	}

	//turnLeft says whether or not to turnLeft
	public static Direction turn(Direction dir, boolean turnLeft)
	{
		if(turnLeft)
		{
			return dir.rotateLeft();
		}
		else
		{
			return dir.rotateRight();
		}
	}
	
	public static boolean isLocNull(MapLocation loc)
	{
		return loc.x == -1 || loc.y == -1;
	}
}
