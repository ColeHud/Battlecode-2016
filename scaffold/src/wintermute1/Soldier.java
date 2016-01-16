package wintermute1;
import battlecode.common.*;

import java.util.*;

/* Rough overview of what the soldiers will do: 
 * 
 */

//has momentum but can set to 0
//don't constantly try to give archons space, but do move away from them when spawned
//could make them clump or swarm more?

public class Soldier
{
	public static Random rand;
	public static RobotController rc;
	public static Direction[] directions = {Direction.NORTH, Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST,
			Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST};
	public static int numDirections = directions.length;
	
	public static double probMove = 0.2; //how often to move if can, maybe make lower for protectors?
	
	public static int maxMomentum = 0; //how many turns to keep going in a direction, if no guidance to change it
	//no momentum right now
	
	public static int closeEnoughSquared = 4; //how close you have to get to a goalLoc (squared)
	public static int startStepsLeft = 50; //max number of steps to take to get to a goalLoc (don't want to try forever)
	
	//doesn't seem to work yet?
	public static double probProtector = 0; //might change based on GameConstants.NUMBER_OF_ARCHONS_MAX
	
	public static double probIgnoreRubbleIfNotTooMuch = 0.2;
	/* how much rubble there has to be so that the
	 * soldiers don't try to clear it, increases the more a
	 * specific soldier sees lots of rubble */
	public static double startTooMuchRubble = 500;

	public static int foeSignalRadiusSquared = 1000; //play around with this some
	public static double probSignal = 0.1;
	
	public static void run() throws GameActionException
	{
		rc = RobotPlayer.rc;
		Team myTeam = rc.getTeam();
		rand = new Random(rc.getID()); //ever used?
		
		boolean isProtector = Math.random() < probProtector;

		double tooMuchRubble = startTooMuchRubble; //how much rubble there has to be so that this soldier won't try to clear it
		double rubbleToleranceGrowthFactor = 2;
		
		MapLocation goalLoc = null;
		Direction dirToMove = Direction.NONE;
		int stepsLeft = startStepsLeft;
		int momentum = maxMomentum;
		boolean offCourse = false; //whether the soldier turned in getting to a location
		//means will have to recompute the direction to the goal
		
		//in code depends on distance from myLoc to goalLoc
		boolean foesMaybeNearby = true; //used to restart while loop
		MapLocation myLoc = rc.getLocation();
		int makerArchonID = 0;
		RobotInfo makerArchon = null;
		
		//move a little away from archon
		int movesAwayFromArchon = 2;
		RobotInfo[] nearbyRobots = rc.senseNearbyRobots(movesAwayFromArchon*movesAwayFromArchon);
		for (RobotInfo robot : nearbyRobots)
		{
			if(robot.type == RobotType.ARCHON)
			{
				makerArchonID = robot.ID;
				makerArchon = robot;
				break;
			}
		}
		if(makerArchon != null)
		{
			dirToMove = makerArchon.location.directionTo(myLoc); //away from archon
			while(movesAwayFromArchon > 0)
			{
				if(rc.isCoreReady())
				{
					//movement code copied below (a little specialized)
					int timesRotated = 0;
					boolean done = false;
					boolean turnLeft = rand.nextBoolean(); //if true keep turning left, if false keep turning right
					while((timesRotated < numDirections) && (! done))
					{
						double rubble = rc.senseRubble(myLoc.add(dirToMove));
						if(rubble > GameConstants.RUBBLE_OBSTRUCTION_THRESH)
						{
							if(rubble >= tooMuchRubble && Math.random() < probIgnoreRubbleIfNotTooMuch) //try another direction
							{
								tooMuchRubble *= rubbleToleranceGrowthFactor;
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
								if(timesRotated > 0)
								{
									momentum = maxMomentum; //so tries to go around the wall?
								}
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
					movesAwayFromArchon --;
				}
				Clock.yield();
			}
		}

		while(true)
		{
			if(isProtector)
			{
				try
				{
					makerArchon = rc.senseRobot(makerArchonID);
					goalLoc = makerArchon.location;
				}
				catch (Exception GameActionException)
				{
					//nothing
				}
			}

			//try to attack, if successful then finish turn
			//does not prioritize who to attack for now
			if(foesMaybeNearby)
			{
				if(rc.isWeaponReady()) //maybe different flow here
				{
					RobotInfo[] foes = rc.senseHostileRobots(myLoc, RobotPlayer.myType.attackRadiusSquared);
					
					double lowestHealth = 0;
					RobotInfo weakestFoe = null;
					for(RobotInfo foe : foes)
					{
						if(lowestHealth == 0 || foe.health < lowestHealth)
						{
							weakestFoe = foe;
							lowestHealth = foe.health;
						}
					}
					
					if(foes.length > 0 && weakestFoe != null)
					{
						rc.attackLocation(weakestFoe.location);
						if(Math.random() < probSignal)
						{
							rc.broadcastSignal(foeSignalRadiusSquared);
						}
					}
					else //no foes nearby
					{
						foesMaybeNearby = false;
						continue;
					}
				}
			}
			else
			{
				if(goalLoc == null)
				{
					boolean gotNewGoalLoc = false;
					Signal[] signals = rc.emptySignalQueue();
					for(Signal signal : signals)
					{
						if((signal.getMessage() == null) && (signal.getTeam() == myTeam))
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
						if(rc.isCoreReady() && Math.random() < probMove)
						{
							int timesRotated = 0;
							boolean done = false;
							boolean turnLeft = rand.nextBoolean(); //if true keep turning left, if false keep turning right
							if(momentum <= 0 || dirToMove == Direction.NONE)
							{
								dirToMove = directions[rand.nextInt(directions.length)]; //random dir
								momentum = maxMomentum; //reset momentum
							}
							else
							{
								momentum --; //should do this here or only when have moved?
							}
							while((timesRotated < numDirections) && (! done))
							{
								double rubble = rc.senseRubble(myLoc.add(dirToMove));
								if(rubble > GameConstants.RUBBLE_OBSTRUCTION_THRESH) //can't get through it
								{
									if(rubble >= tooMuchRubble && Math.random() < probIgnoreRubbleIfNotTooMuch) //try another direction
									{
										tooMuchRubble *= rubbleToleranceGrowthFactor;
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
						if((myLoc.distanceSquaredTo(goalLoc) <= closeEnoughSquared) || (stepsLeft <= 0)) //done
						{
							goalLoc = null;
							dirToMove = Direction.NONE;
							stepsLeft = startStepsLeft;
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
									if(rubble >= tooMuchRubble && Math.random() < probIgnoreRubbleIfNotTooMuch) //try another direction
									{
										tooMuchRubble *= rubbleToleranceGrowthFactor;
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
				}
				foesMaybeNearby = true;
			}
			Clock.yield(); //end after if statement
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
}
