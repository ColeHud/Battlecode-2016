//REMEMBER TO UPDATE ME!

package wintermute1nokiting;
import battlecode.common.*;

import java.util.*;

/* Rough overview of what the soldiers will do: 
 * If enemies in attack range, attack
 * 		Signal if you attack so others can come help
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

	public static double probMove = 0.1; //how often to move if can, maybe make lower for protectors?

	public static int maxMomentum = 0; //how many turns to keep going in a direction, if no guidance to change it
	//0 because turned off right now

	public static int closeEnoughSquared = 4; //how close you have to get to a goalLoc (squared)

	public static double probProtector = 0; //might make change based on GameConstants.NUMBER_OF_ARCHONS_MAX

	public static double probIgnoreRubbleIfNotTooMuch = 0.2;
	/* how much rubble there has to be so that the
	 * soldiers don't try to clear it, increases the more a
	 * specific soldier sees lots of rubble */
	public static double startTooMuchRubble = 500;

	public static int foeSignalRadiusSquared = 1000; //play around with this some
	public static double probSignal = 0.15;

	public static void run() throws GameActionException
	{
		rc = RobotPlayer.rc;
		Team myTeam = rc.getTeam();
		rand = new Random(rc.getID()); //make sure this works

		boolean isProtector = Math.random() < probProtector;

		//how much rubble there has to be so that this soldier won't try to clear it
		double tooMuchRubble = startTooMuchRubble;
		
		//every time you decide to go around rubble, multiply rubble tolerance by this
		//so if there's really lots and lots of rubble, eventually you'll go through it
		double rubbleToleranceGrowthFactor = 2; 
		
		MapLocation goalLoc = null;
		Direction dirToMove = Direction.NONE;
		//how many turns to spend trying to get to a goal location
		//below value is reasonable but never used, depends on initial distance to goal
		int turnsLeft = 50;
		int momentum = maxMomentum; //start off momentum at max
		
		//whether the soldier turned some in getting to a location
		//means will have to recompute the direction to the goal
		boolean offCourse = false;
		
		boolean anyFoesToAttack = true; //if false, then move around and do other non-killing stuff
		MapLocation myLoc = rc.getLocation();
		int makerArchonID = 0;
		RobotInfo makerArchon = null;

		//ENTERING THE ACTUAL CODE
		
		//move a little away from your maker archon, to give it space
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
			//protectors mostly aren't used, at least now
			if(isProtector)
			{
				try
				{
					makerArchon = rc.senseRobot(makerArchonID);
					goalLoc = makerArchon.location;
					turnsLeft = myLoc.distanceSquaredTo(goalLoc);
				}
				catch (Exception GameActionException)
				{
					//nothing
				}
			}
			
			//try to attack weakest foe, if successful then finish turn
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
							if(lowestHealth == 0 || foe.health < lowestHealth)
							{
								targetFoe = foe;
								lowestHealth = foe.health;
							}
						}
						
						/*//code to go towards turrets, as Cole said, really horrible unless with lots of friends
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
						
						/*//first kiting code below is best
						//others may be good with tweaking but seems unlikely
						
						//kiting in a while loop approach
						//move until out of foe's attack range, then fire
						//if your foe has a greater attack range or the same attack range, just attack it
						if(targetFoe.type.attackRadiusSquared < RobotPlayer.myType.attackRadiusSquared
						   && myLoc.distanceSquaredTo(targetFoe.location) <= targetFoe.type.attackRadiusSquared)
						{
							Direction away = targetFoe.location.directionTo(myLoc);
							simpleTryMove(away);
							boolean enemyStillThere = true;
							while(myLoc.distanceSquaredTo(targetFoe.location) <= targetFoe.type.attackRadiusSquared)
							{
								try
								{
									targetFoe = rc.senseRobot(targetFoe.ID);
									away = targetFoe.location.directionTo(myLoc);
									simpleTryMove(away);
								}
								catch (Exception GameActionException)
								{
									enemyStillThere = false;
									simpleTryMove(away.opposite());
									break;
								}
								myLoc = rc.getLocation();

							}
							if(enemyStillThere && rc.isWeaponReady() && rc.canAttackLocation(targetFoe.location))
							{
								rc.attackLocation(targetFoe.location);
							}
						}
						*/
						
						/*//stay as far away as you can as long as you can still attack any foes that can attack you
						//kiting in a while loop approach
						if(targetFoe.type.canAttack())
						{
							if(RobotPlayer.myType.attackRadiusSquared - myLoc.distanceSquaredTo(targetFoe.location) > 100)
							{
								System.out.println("too close");
								Direction away = targetFoe.location.directionTo(myLoc);
								simpleTryMove(away);
								boolean enemyStillThere = true;
								while(RobotPlayer.myType.attackRadiusSquared - myLoc.distanceSquaredTo(targetFoe.location) > 100)
								{
									try
									{
										targetFoe = rc.senseRobot(targetFoe.ID);
										away = targetFoe.location.directionTo(myLoc);
										simpleTryMove(away);
									}
									catch (Exception GameActionException)
									{
										enemyStillThere = false;
										simpleTryMove(away.opposite());
										break;
									}
									myLoc = rc.getLocation();

								}
								if(enemyStillThere && rc.isWeaponReady() && rc.canAttackLocation(targetFoe.location))
								{
									rc.attackLocation(targetFoe.location);
								}
							}
							else
							{
								if(rc.isWeaponReady() && rc.canAttackLocation(targetFoe.location))
								{
									rc.attackLocation(targetFoe.location);
								}
							}
						}
						*/
						
						/*//first sort of kiting
						//move until out of foe's attack range, then fire
						//if your foe has a greater attack range or the same attack range, just attack it
						if(targetFoe.type.attackRadiusSquared < RobotPlayer.myType.attackRadiusSquared
					       && myLoc.distanceSquaredTo(targetFoe.location) <= targetFoe.type.attackRadiusSquared)
						{
							simpleTryMove(targetFoe.location.directionTo(myLoc));
						}
						*/
						
						/*//second sort of kiting
						//kite, move away from foe, then try to fire, then move away, try to fire, move away
						//when out of foe's attack range, just fire
						//if your foe has a greater attack range or the same attack range, just attack it
						if(targetFoe.type.attackRadiusSquared < RobotPlayer.myType.attackRadiusSquared
						&& myLoc.distanceSquaredTo(targetFoe.location) <= targetFoe.type.attackRadiusSquared)
						{
							simpleTryMove(targetFoe.location.directionTo(myLoc));
							if(rc.isWeaponReady())
							{
								try
								{
									rc.attackLocation(rc.senseRobot(targetFoe.ID).location);
								}
								catch (Exception GameActionException)
								{
									//nothing
								}
							}
						}
						*/
						
						/*//just kites for BIGZOMBIE
						if(targetFoe.type == RobotType.BIGZOMBIE && myLoc.distanceSquaredTo(targetFoe.location) <= RobotType.BIGZOMBIE.attackRadiusSquared)
						{
							//move away from it
							simpleTryMove(targetFoe.location.directionTo(myLoc));
							if(rc.isWeaponReady())
							{
								try
								{
									rc.attackLocation(rc.senseRobot(targetFoe.ID).location);
								}
								catch (Exception GameActionException)
								{
									//nothing
								}
							}
						}
						*/
						
						/*//if using any kiting, free up this section, else comment out
						else
						{
							if(rc.isWeaponReady() && rc.canAttackLocation(targetFoe.location))
							{
								rc.attackLocation(targetFoe.location);
							}
						}
						*/
						
						rc.attackLocation(targetFoe.location);
						
						if(Math.random() < probSignal)
						{
							rc.broadcastSignal(foeSignalRadiusSquared);
						}
					}
					else //no foes in attack range
					{
						anyFoesToAttack = false;
						RobotInfo[] foesYouCanOnlySee = rc.senseHostileRobots(myLoc, RobotPlayer.myType.sensorRadiusSquared);
						//could do min thing here too, but $$$?
						if(foesYouCanOnlySee.length > 0)
						{
							goalLoc = foesYouCanOnlySee[0].location;

							if(Math.random() < probSignal)
							{
								rc.broadcastSignal(foeSignalRadiusSquared);
							}
						}
						continue;
					}
				}
			}
			else
			{
				if(goalLoc == null)
				{
					//follow signal closest to you
					Signal[] signals = rc.emptySignalQueue();
					MapLocation closestSignalLoc = null;
					double smallestCloseness = 0;
					for(Signal signal : signals)
					{
						MapLocation signalLoc = signal.getLocation();
						double signalCloseness = myLoc.distanceSquaredTo(signalLoc);
						//follow enemy signals to kill messengers or your own team's signals to group up
						if((smallestCloseness == 0 || signalCloseness < smallestCloseness) && (signal.getMessage() == null || signal.getTeam() != myTeam))
						{
							closestSignalLoc = signalLoc;
							smallestCloseness = signalCloseness;
						}
					}
					if(closestSignalLoc != null)
					{
						goalLoc = closestSignalLoc;
						dirToMove =  myLoc.directionTo(goalLoc);
						turnsLeft = (int) smallestCloseness; //not sure what would be better
						continue;
					}
					
					/* //old way of doing it, just gets first acceptable signal
					boolean gotNewGoalLoc = false;
					Signal[] signals = rc.emptySignalQueue();
					for(Signal signal : signals)
					{
						//can check && (signal.getTeam() == myTeam), this way explores all messages
						if(signal.getMessage() == null || signal.getTeam() != myTeam)
						{
							goalLoc = signal.getLocation();
							turnsLeft = myLoc.distanceSquaredTo(goalLoc); //not sure what would be better
							dirToMove =  myLoc.directionTo(goalLoc);
							gotNewGoalLoc = true;
						}
					}
					if(gotNewGoalLoc)
					{
						continue;
					}
					 */
					
					else //move randomly
						 //this code is copied some below
						 //maybe change to move towards friends
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
										turnsLeft --;
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
						if((myLoc.distanceSquaredTo(goalLoc) <= closeEnoughSquared) || (turnsLeft <= 0)) //done
						{
							goalLoc = null;
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
									if(rubble >= tooMuchRubble && Math.random() < probIgnoreRubbleIfNotTooMuch) //try another direction
									{
										tooMuchRubble *= rubbleToleranceGrowthFactor;
										dirToMove = turn(dirToMove, turnLeft);
										timesRotated ++;
										offCourse = true; //means you have to recompute direction to goalLoc
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
										turnsLeft --;
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
				anyFoesToAttack = true;
			}
			Clock.yield(); //end after if statement
		}
	}

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
