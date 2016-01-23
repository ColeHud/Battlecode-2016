package wintermute1focusedclone;
import battlecode.common.*;

import java.util.*;

/* Rough overview of what the soldiers will do: 
 * If enemies just in attack range, attack
 * 		Signal if you attack so others can come help
 * Otherwise kite (if enemies can hurt you)
 * Otherwise look for enemies and move towards any you see
 * Otherwise continue along a goal location you got in a message
 * 		If you're close enough or have spent a while on it, reset goal location
 * Otherwise (if you don't have a goal location) check your messages for one
 * Otherwise just move around randomly
 * (Yes there's other stuff and it's not all pretty)
 */

public class Soldier
{
	public static Random rand;
	public static RobotController rc;

	public static Direction[] directions = {Direction.NORTH, Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST,
			Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST};
	public static int numDirections = directions.length;

	public static int kitingTolerance = 5; //at 3 starts bouncing a little more, even at 4 bounces a little, at 5 none at all
	
	//right now no moving
	public static double probMove = 0.4; //how often to move if can, maybe make lower for protectors?

	public static int closeEnoughSquared = 4; //how close you have to get to a goalLoc (squared)

	public static double probIgnoreRubbleIfNotTooMuch = 0.2;
	/* how much rubble there has to be so that the
	 * soldiers don't try to clear it, increases the more a
	 * specific soldier sees lots of rubble */
	public static double startTooMuchRubble = 500;

	public static int foeSignalRadiusSquared = 1000; //play around with this some
	public static double probSignal = 0.15;

	public static int friendFindingRadiusSquared = 49;
	public static int roundsToFollowAFriend = 1;
	
	public static void run() throws GameActionException
	{
		rc = RobotPlayer.rc;
		Team myTeam = rc.getTeam();
		rand = new Random(rc.getID()); //make sure this works

		//how much rubble there has to be so that this soldier won't try to clear it
		double tooMuchRubble = startTooMuchRubble;

		//every time you decide to go around rubble, multiply rubble tolerance by this
		//so if there's really lots and lots of rubble, eventually you'll go through it
		double rubbleToleranceGrowthFactor = 2; 

		MapLocation goalLoc = null;
		Direction dirToMove = Direction.NONE;
		//how many rounds to spend trying to get to a goal location
		//below value is reasonable but never used, depends on initial distance to goal
		int roundsLeft = 50;

		boolean anyFoesToAttack = true; //if false, then move around and do other non-killing stuff
		MapLocation myLoc = rc.getLocation();
		RobotInfo makerArchon = null;

		//ENTERING THE ACTUAL CODE

		//move a little away from your maker archon, to give it space
		int movesAwayFromArchon = 3; //could make this increase as more soldiers made?
		RobotInfo[] nearbyRobots = rc.senseNearbyRobots(movesAwayFromArchon*movesAwayFromArchon);
		for (RobotInfo robot : nearbyRobots)
		{
			if(robot.type == RobotType.ARCHON)
			{
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
					//start in a direction, choose a random way to turn, turn that way until you've tried all the directions
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
			//try to attack weakest foe, if successful then finish turn
			//could use more fancy way to choose foe, wouldn't be too much more $$$
			if(anyFoesToAttack)
			{
				//could prioritize guys to attack here, archons, all that
				
				if(rc.isWeaponReady()) //maybe different flow here?
				{
					RobotInfo[] foes = rc.senseHostileRobots(myLoc, RobotPlayer.myType.attackRadiusSquared);

					if(foes.length > 0)
					{
						//could getClosestRobot
						RobotInfo targetFoe = getClosestRobot(foes, myLoc);
						
						//or getMinHealthRobot here
						//RobotInfo targetFoe = getMinHealthRobot(foes);

						//kiting in a while loop
						//other kiting implementations may be much better!
						//move until you're just at the edge of your own attack range, and then fire!
						
						if(myLoc.distanceSquaredTo(targetFoe.location) < RobotPlayer.myType.attackRadiusSquared - kitingTolerance)
						{
							//set a countdown for kiting? Just fire at some point? In case cornered?
							while(myLoc.distanceSquaredTo(targetFoe.location) < RobotPlayer.myType.attackRadiusSquared - kitingTolerance)
							{
								if(rc.canSenseRobot(targetFoe.ID))
								{
									//should be done after all this?
									targetFoe = rc.senseRobot(targetFoe.ID);
									dirToMove = targetFoe.location.directionTo(myLoc); //away from foe

									int timesRotated = 0;
									boolean done = false; //whether or not has moved or cleared some rubble
									boolean turnLeft = rand.nextBoolean(); //if true keep turning left, if false keep turning right

									while((timesRotated < numDirections) && (! done))
									{
										if(rc.isCoreReady() && rc.canMove(dirToMove))
										{
											rc.move(dirToMove);
											done = true;
											myLoc = rc.getLocation();
										}
										else
										{
											//if there's rubble, we don't try to clean it up
											//digging is too slow for a fight

											dirToMove = turn(dirToMove, turnLeft);
											timesRotated ++;
										}
									}

									if(done) //you moved
									{
										Clock.yield();
									}
									else //there's nowhere to go, just fight
									{
										break;
									}
								}
								else
								{
									//move back towards enemy, get in range again
									dirToMove = dirToMove.opposite();
									if(rc.canMove(dirToMove))
									{
										if(rc.isCoreReady() && rc.canMove(dirToMove))
										{
											rc.move(dirToMove);
											myLoc = rc.getLocation();
											Clock.yield();
										}
									}
									else
									{
										//turn some (not lots) and try to get him?
										//or give up as lost? (what we do right now)
										break;
									}

									//break; //would make it so only turns in once
								}
							}

							if(rc.isWeaponReady())
							{
								if(rc.canSenseRobot(targetFoe.ID))
								{
									targetFoe = rc.senseRobot(targetFoe.ID);
									if(rc.canAttackLocation(targetFoe.location))
									{
										rc.attackLocation(targetFoe.location);
									}
									if(Math.random() < probSignal)
									{
										rc.broadcastSignal(foeSignalRadiusSquared);
									}
								}
								else
								{
									//continue?
								}
							}
						}
						else
						{
							if(rc.canSenseRobot(targetFoe.ID)) //may be $$$, but stops some misfirings
							{
								rc.attackLocation(targetFoe.location);
							}
							if(Math.random() < probSignal)
							{
								rc.broadcastSignal(foeSignalRadiusSquared);
							}
						}
					}
					else //no foes in attack range
					{
						anyFoesToAttack = false;
						RobotInfo[] foesYouCanOnlySee = rc.senseHostileRobots(myLoc, RobotPlayer.myType.sensorRadiusSquared);

						//could do min thing here too, but $$$?
						if(foesYouCanOnlySee.length > 0)
						{
							RobotInfo targetFoe = getClosestRobot(foesYouCanOnlySee, myLoc);
							goalLoc = targetFoe.location;
							roundsLeft = (int) Math.sqrt(myLoc.distanceSquaredTo(goalLoc));
						}

						continue; //will make it follow enemy that it sees
					}
				}
			}
			else
			{
				if(goalLoc == null)
				{
					//should choose latest signal?

					//follow signal closest to you
					Signal[] signals = rc.emptySignalQueue();
					MapLocation chosenSignalLoc = null;
					double smallestCloseness = 0;
					for(Signal signal : signals)
					{						
						//right now follows only own team's signals to group up
						//could follow enemy team signals too to kill messengers
						//but seems to spread out group too much
						
						if((signal.getMessage() == null) && (signal.getTeam() == myTeam))
						{
							MapLocation signalLoc = signal.getLocation();
							double signalCloseness = myLoc.distanceSquaredTo(signalLoc);

							if((smallestCloseness == 0) || (signalCloseness < smallestCloseness))
							{
								chosenSignalLoc = signalLoc;
								smallestCloseness = signalCloseness;
							}
						}
					}
					if(chosenSignalLoc != null)
					{
						goalLoc = chosenSignalLoc;
						dirToMove =  myLoc.directionTo(goalLoc);
						roundsLeft = (int) Math.sqrt(myLoc.distanceSquaredTo(goalLoc)); //how many rounds to pursue goal for, not sure what would be better
						continue;
					}
					else //follow friends
					{
						RobotInfo[] friends = rc.senseNearbyRobots(friendFindingRadiusSquared, myTeam);
						if(friends.length > 0)
						{
							goalLoc = getClosestRobot(friends, myLoc).location; //should choose closest, something else?
							roundsLeft = roundsToFollowAFriend;
						}
					}

				}
				else //continue towards goalLoc
				{
					if(rc.isCoreReady())
					{
						if((myLoc.distanceSquaredTo(goalLoc) <= closeEnoughSquared) || (roundsLeft <= 0)) //done
						{
							goalLoc = null;
							dirToMove = Direction.NONE;
							continue; //didn't use that much bytecode to get here, still might be a mistake
						}
						else
						{
							dirToMove =  myLoc.directionTo(goalLoc);
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
										roundsLeft --;
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
				anyFoesToAttack = true;
			}
			Clock.yield(); //end after if statement
		}
	}

	public static RobotInfo getMinHealthRobot(RobotInfo[] robots)
	{
		RobotInfo minHealthRobot = null;
		double lowestHealth = -1;
		for(RobotInfo robot : robots)
		{
			if((lowestHealth == -1) || (robot.health < lowestHealth))
			{
				minHealthRobot = robot;
				lowestHealth = robot.health;
			}
		}
		return minHealthRobot;
	}
	
	public static RobotInfo getClosestRobot(RobotInfo[] robots, MapLocation myLoc)
	{
		RobotInfo closestRobot = null;
		double smallestDistance = -1;
		for(RobotInfo robot : robots)
		{
			if((smallestDistance == -1) || (myLoc.distanceSquaredTo(robot.location) < smallestDistance))
			{
				closestRobot = robot;
				smallestDistance = robot.health;
			}
		}
		return closestRobot;
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
