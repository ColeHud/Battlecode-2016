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
				if(rc.isWeaponReady()) //maybe different flow here?
				{
					RobotInfo[] foes = rc.senseHostileRobots(myLoc, RobotPlayer.myType.attackRadiusSquared);

					if(foes.length > 0)
					{
						RobotInfo targetFoe = null;
						double lowestHealth = 0;
						for(RobotInfo foe : foes)
						{
							if(foe.type == RobotType.ARCHON) //highest priority
							{
								targetFoe = foe;
								//should send out a huge signal?
								break;
							}
							if((lowestHealth == 0) || (foe.health < lowestHealth))
							{
								targetFoe = foe;
								lowestHealth = foe.health;
							}
						}

						/*//code to go towards turrets, as Cole said, really horrible unless maybe with lots of friends
						  //turrets just destroy soldiers, soldiers never get a chance to fire
						 if(weakestFoe.type == RobotType.TURRET && myLoc.distanceSquaredTo(weakestFoe.location) > GameConstants.TURRET_MINIMUM_RANGE) //move closer
						 {
						 	//move towards it
						 	dirToMove = myLoc.directionTo(weakestFoe.location);
						 	simpleTryMove(dirToMove);
						 	if(rc.isWeaponReady()); //may not need this, will never be ready?
						 	{
						 		rc.attackLocation(weakestFoe.location);
						 	}
						 }
						 */

						//kiting in a while loop approach
						//other kiting implementations may be much better!
						//move until you're just at the edge of your own attack range, and then fire!
						//if your foe has a greater attack range or the same attack range, just attack it
						if(targetFoe.type != RobotType.ZOMBIEDEN  //useless to try to kite
						&& targetFoe.type.attackRadiusSquared < RobotPlayer.myType.attackRadiusSquared
						&& myLoc.distanceSquaredTo(targetFoe.location) < RobotPlayer.myType.attackRadiusSquared)
						{
							//set a countdown for kiting? Just fire at some point? In case cornered?
							while(myLoc.distanceSquaredTo(targetFoe.location) < RobotPlayer.myType.attackRadiusSquared)
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
										if(rc.isCoreReady())
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

									//maybe should remove to let go back, but stops endless loop?
									break;
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
									//nothing
									//continue?
								}
							}
						}
						else
						{
							if(rc.canSenseRobot(targetFoe.ID)) //may be $$$, but fixes some misfirings?
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

						//could do min thing here too, or ID thing, but $$$?
						if(foesYouCanOnlySee.length > 0)
						{
							RobotInfo targetFoe = foesYouCanOnlySee[0];
							goalLoc = targetFoe.location;
							roundsLeft = (int) Math.sqrt(myLoc.distanceSquaredTo(targetFoe.location));
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
						//could follow enemy team signals to kill messengers
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
						roundsLeft = (int) Math.sqrt(smallestCloseness); //how many rounds to pursue goal for, not sure what would be better
						continue;
					}
					else //follow friends
					{
						RobotInfo[] friends = rc.senseNearbyRobots(friendFindingRadiusSquared, myTeam);
						if(friends.length > 0)
						{
							goalLoc = friends[0].location; //should choose closest, something else?
							roundsLeft = roundsToFollowAFriend;
						}
					}

					/*//follow signal of robot with smallest ID, so all coordinate
					Signal[] signals = rc.emptySignalQueue();
					Signal chosenSignal = null;
					int smallestID = 0;
					for(Signal signal : signals)
					{
						int ID = signal.getID();

						//follow only your own team's signals, keep organized
						if((smallestID == 0 || ID < smallestID)
								&& signal.getMessage() == null && signal.getTeam() == myTeam)
						{
							chosenSignal = signal;
							smallestID = ID;
						}
					}
					if(chosenSignal != null)
					{
						goalLoc = chosenSignal.getLocation();
						dirToMove =  myLoc.directionTo(goalLoc);
						roundsLeft = myLoc.distanceSquaredTo(goalLoc); //not sure what would be better
						continue;
					}
					 */
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

	//never used?
	public static void simpleTryMove(Direction dirToMove) throws GameActionException
	{
		if(rc.isCoreReady() && rc.canMove(dirToMove))
		{
			rc.move(dirToMove);
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
}
