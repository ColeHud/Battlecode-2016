package wintermute1focused;
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

	public static int closeEnoughSquared = 4; //how close you have to get to a goalLoc (squared)

	public static double probIgnoreRubbleIfNotTooMuch = 0.2;
	/* how much rubble there has to be so that the
	 * soldiers don't try to clear it, increases the more a
	 * specific soldier sees lots of rubble */
	public static double startTooMuchRubble = 500;
	//every time you decide to go around rubble, multiply rubble tolerance by this
	//so if there's really lots and lots of rubble, eventually you'll go through it
	public static double rubbleToleranceGrowthFactor = 2; 

	public static int foeSignalRadiusSquared = 1000; //play around with this some
	public static double probSignal = 0.15;

	public static double probClump = 0.1;
	public static int friendFindingRadiusSquared = RobotPlayer.rc.getType().sensorRadiusSquared;
	public static int roundsToFollowAFriend = 1;

	//how many rounds to spend trying to get to a goal location
	//below value is reasonable but never used, actually depends on initial distance to goal
	public static int roundsLeft = 50;

	public static int[] friendlyArchonIDs = {-1, -1, -1, -1};

	//how far away to be from archon
	public static int movesAwayFromArchon = 3; //could make this increase as more soldiers made?

	public static void run() throws GameActionException
	{
		rc = RobotPlayer.rc;
		RobotType myType = rc.getType();
		Team myTeam = rc.getTeam();
		rand = new Random(rc.getID());

		//how much rubble there has to be so that this soldier won't try to clear it
		double tooMuchRubble = startTooMuchRubble;

		MapLocation goalLoc = null;
		Direction dirToMove = Direction.NONE;

		boolean anyFoesToAttack = true; //if false, then move around and do other non-killing stuff
		MapLocation myLoc = rc.getLocation();

		//ENTERING THE ACTUAL CODE

		while(true)
		{
			//try to attack weakest foe, if successful then finish turn
			//could use more fancy way to choose foe, wouldn't be too much more $$$
			if(anyFoesToAttack)
			{
				//could prioritize guys to attack here, archons, all that

				if(rc.isWeaponReady()) //maybe different flow here?
				{
					RobotInfo[] foes = rc.senseHostileRobots(myLoc, myType.attackRadiusSquared);

					if(foes.length > 0)
					{
						//could getClosestRobot
						//RobotInfo targetFoe = getClosestRobot(foes, myLoc);

						//or getMinHealthRobot here
						//RobotInfo targetFoe = getMinHealthRobot(foes);

						//or one with most attack power, and among those, lowest health
						RobotInfo targetFoe = getRobotWithMostAttackPower(foes);

						if(targetFoe.type == RobotType.ZOMBIEDEN && rc.isCoreReady())
						{
							//Just get close and kill it
							dirToMove = myLoc.directionTo(targetFoe.location);
							if(rc.canMove(dirToMove))
							{
								rc.move(dirToMove);
							}
							else if(rc.isWeaponReady() && rc.canAttackLocation(targetFoe.location))
							{
								rc.attackLocation(targetFoe.location);
								if(rand.nextFloat() < probSignal)
								{
									rc.broadcastSignal(foeSignalRadiusSquared);
								}
							}
						}

						//kiting in a while loop
						//other kiting implementations may be much better!
						//move until you're just at the edge of your own attack range, and then fire!

						else if(myLoc.distanceSquaredTo(targetFoe.location) < myType.attackRadiusSquared - kitingTolerance)
						{
							//set a countdown for kiting? Just fire at some point? In case cornered?
							while(myLoc.distanceSquaredTo(targetFoe.location) < myType.attackRadiusSquared - kitingTolerance)
							{
								if(rc.canSenseRobot(targetFoe.ID))
								{
									//should be done after all this?
									targetFoe = rc.senseRobot(targetFoe.ID);

									dirToMove = targetFoe.location.directionTo(myLoc); //away from foe
									Direction leftDir = dirToMove;
									Direction rightDir = dirToMove;
									boolean turnLeft = rand.nextFloat() < 0.5; //if true keep turning left, if false keep turning right
									int directionsTried = 1;

									boolean done = false; //whether or not has moved or cleared some rubble

									while((directionsTried < numDirections - 1) && (! done)) //don't try dir towards foe
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

											directionsTried++;
											if(turnLeft)
											{
												leftDir = leftDir.rotateLeft();
												dirToMove = leftDir;
												if(directionsTried % 2 == 0)
												{
													turnLeft = rand.nextFloat() < 0.5;
												}
												else
												{
													turnLeft = false;
												}
											}
											else
											{
												rightDir = rightDir.rotateRight();
												dirToMove = rightDir;
												if(directionsTried % 2 == 0)
												{
													turnLeft = rand.nextFloat() < 0.5;
												}
												else
												{
													turnLeft = true;
												}
											}
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
										if(rc.isCoreReady() && rc.canMove(dirToMove)) //not sure why have to check canMove again
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
										if(rand.nextFloat() < probSignal)
										{
											rc.broadcastSignal(foeSignalRadiusSquared);
										}
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
							if(rc.canSenseRobot(targetFoe.ID) && rc.canAttackLocation(targetFoe.location)) //may be $$$, but stops some misfirings
							{
								rc.attackLocation(targetFoe.location);
								if(rand.nextFloat() < probSignal)
								{
									rc.broadcastSignal(foeSignalRadiusSquared);
								}
							}
						}
					}
					else //no foes in attack range
					{
						anyFoesToAttack = false;
						RobotInfo[] foesYouCanOnlySee = rc.senseHostileRobots(myLoc, myType.sensorRadiusSquared);

						if(foesYouCanOnlySee.length > 0)
						{
							//RobotInfo targetFoe = foesYouCanOnlySee[foesYouCanOnlySee.length - 1]; //last foe is the one that moves latest
							//could also getMinHealthRobot(foesYouCanOnlySee) or getClosestRobot(foesYouCanOnlySee, myLoc)
							RobotInfo targetFoe = getRobotWithMostAttackPower(foesYouCanOnlySee);
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
					Signal[] signals = rc.emptySignalQueue();
					MapLocation chosenSignalLoc = null;

					/*//go towards source of latest signal?
					if(signals.length > 0)
					{
						for(int i = signals.length - 1; i >= 0; i--)
						{
							Signal signal = signals[i];
							if((signal.getMessage() == null) && (signal.getTeam() == myTeam))
							{
								chosenSignalLoc = signals[i].getLocation();
								break;
							}
						}
					}
					 */

					//go towards closest signal source
					double smallestCloseness = -1;
					for(Signal signal : signals)
					{										
						//want to check if it's an archon ID to prioritize messages from archons
						//horribly horribly inefficient
						if(friendlyArchonIDs == addToFriendlyArchonIDs(signal.getID(), friendlyArchonIDs)) //check if a known friendly archon
						{
							chosenSignalLoc = signal.getLocation();
							smallestCloseness = myLoc.distanceSquaredTo(chosenSignalLoc); //just to get this to be the number of steps
							break;
						}

						//right now follows only own team's signals to group up
						//could follow enemy team signals too to kill messengers
						//but seems to spread out group too much

						if((signal.getMessage() == null) && (signal.getTeam() == myTeam))
						{
							MapLocation signalLoc = signal.getLocation();
							double signalCloseness = myLoc.distanceSquaredTo(signalLoc);

							if((smallestCloseness == -1) || (signalCloseness < smallestCloseness))
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
						roundsLeft = (int) Math.sqrt(smallestCloseness); //how many rounds to pursue goal for, not sure what would be better
						continue;
					}
					else //follow friends
					{
						RobotInfo[] friends = rc.senseNearbyRobots(friendFindingRadiusSquared, myTeam);
						if(friends.length > 0)
						{
							RobotInfo nearbyFriendlyArchon = findFriendlyArchon(friends, myTeam);
							if(nearbyFriendlyArchon == null) //no nearby friendly archons
							{
								if(rand.nextFloat() < probClump)
								{
									goalLoc = friends[rand.nextInt(friends.length)].location; //should choose closest, something else?
									roundsLeft = roundsToFollowAFriend;
								}
							}
							else //make sure archon has some space
							{
								friendlyArchonIDs = addToFriendlyArchonIDs(nearbyFriendlyArchon.ID, friendlyArchonIDs);
								dirToMove = nearbyFriendlyArchon.location.directionTo(myLoc); //away from archon
								goalLoc = myLoc.add(dirToMove, movesAwayFromArchon);
								roundsLeft = 2*movesAwayFromArchon;
							}
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
							Direction leftDir = dirToMove;
							Direction rightDir = dirToMove;
							boolean turnLeft = rand.nextFloat() < 0.5; //if true keep turning left, if false keep turning right
							int directionsTried = 1;

							boolean done = false; //whether or not has moved or cleared some rubble

							while((directionsTried < numDirections) && (! done))
							{
								myLoc = rc.getLocation();
								MapLocation actionLoc = myLoc.add(dirToMove);
								double rubble = rc.senseRubble(actionLoc);
								if(rubble >= GameConstants.RUBBLE_OBSTRUCTION_THRESH)
								{
									if(rubble >= tooMuchRubble && rand.nextFloat() < probIgnoreRubbleIfNotTooMuch) //try another direction
									{
										tooMuchRubble *= rubbleToleranceGrowthFactor;
										directionsTried++;
										if(turnLeft)
										{
											leftDir = leftDir.rotateLeft();
											dirToMove = leftDir;
											if(directionsTried % 2 == 0)
											{
												turnLeft = rand.nextFloat() < 0.5;
											}
											else
											{
												turnLeft = false;
											}
										}
										else
										{
											rightDir = rightDir.rotateRight();
											dirToMove = rightDir;
											if(directionsTried % 2 == 0)
											{
												turnLeft = rand.nextFloat() < 0.5;
											}
											else
											{
												turnLeft = true;
											}
										}
									}
									else if(rc.onTheMap(actionLoc)) //clear the rubble
									{
										rc.clearRubble(dirToMove);
										done = true;
									}
								}
								else
								{
									if(rc.canMove(dirToMove)) //still on map, try to move
									{
										rc.move(dirToMove);
										roundsLeft --;
										done = true;
										myLoc = rc.getLocation();
									}
									else
									{
										directionsTried++;
										if(turnLeft)
										{
											leftDir = leftDir.rotateLeft();
											dirToMove = leftDir;
											if(directionsTried % 2 == 0)
											{
												turnLeft = rand.nextFloat() < 0.5;
											}
											else
											{
												turnLeft = ! turnLeft;
											}
										}
										else
										{
											rightDir = rightDir.rotateRight();
											dirToMove = rightDir;
											if(directionsTried % 2 == 0)
											{
												turnLeft = rand.nextFloat() < 0.5;
											}
											else
											{
												turnLeft = ! turnLeft;
											}
										}
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

	//if same attack power, gets one with lowest health (could also do closest, but this is cheaper)
	public static RobotInfo getRobotWithMostAttackPower(RobotInfo[] robots)
	{
		RobotInfo robotWithMostAttackPower = null;
		for(RobotInfo robot : robots)
		{
			if(robotWithMostAttackPower == null || robot.attackPower > robotWithMostAttackPower.attackPower
					|| (robot.attackPower == robotWithMostAttackPower.attackPower && robot.health < robotWithMostAttackPower.health))
			{
				robotWithMostAttackPower = robot;
			}
		}
		return robotWithMostAttackPower;
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

	public static RobotInfo findFriendlyArchon(RobotInfo[] robots, Team myTeam)
	{
		for(RobotInfo robot : robots)
		{
			if((robot.type == RobotType.ARCHON) && (robot.team == myTeam))
			{
				return robot;
			}
		}
		return null;
	}

	public static int[] addToFriendlyArchonIDs(int newArchonID, int[] friendlyArchonIDs)
	{
		int[] updatedFriendlyArchonIDs = friendlyArchonIDs;
		for(int i = 0; i < friendlyArchonIDs.length; i++)
		{
			if(friendlyArchonIDs[i] == newArchonID)
			{
				break; //already included
			}
			else if(friendlyArchonIDs[i] == -1)
			{
				updatedFriendlyArchonIDs[i] = newArchonID;
				break;
			}
		}
		return updatedFriendlyArchonIDs;
	}
}
