package wintermute1;
import battlecode.common.*;

import java.util.*;

/* Rough overview of what the turrets will do: 
 * 
 */

//do not move away from archons if too close to them yet

//move constants and all that inside or outside

public class Turret 
{
	public static Random rand;
	public static RobotController rc;

	public static Direction[] directions = {Direction.NORTH, Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST,
			Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST};
	public static int numDirections = directions.length;
	public static double tooMuchRubble = 50; //how much rubble there has to be so that the soldiers don't try to clear it

	public static void run() throws GameActionException
	{
		rc = RobotPlayer.rc;
		rand = new Random(rc.getID());
		Team enemyTeam = rc.getTeam().opponent();
		boolean isPacked = false; //whether or not the turret is packed (is a TTM)
		int maxTurnsToRunAway = 10; //how many turns TTMs should run away for before standing their ground
		int turnsToRunAway = maxTurnsToRunAway; //doesn't need to start with a value, but compiler complained

		while(true)
		{	
			if(isPacked)
			{
				//check if far away enough from enemies now
				MapLocation myLoc = rc.getLocation();
				RobotInfo[] foesTooCloseToMe = rc.senseHostileRobots(myLoc, GameConstants.TURRET_MINIMUM_RANGE);
				if(foesTooCloseToMe.length == 0 || turnsToRunAway <= 0) //can stop running
				{
					rc.unpack();
					isPacked = false;
				}
				else if(rc.isCoreReady()) //run away from them!
				{
					//get average loc of hostiles
					//could also just run away from the closest hostile
					//neither one of those would have you go up or down if you have enemies directly to
					//your left and right
					int n_hostiles = 0;
					int x_sum = 0;
					int y_sum = 0;
					for(RobotInfo robot : foesTooCloseToMe)
					{
						n_hostiles ++;
						MapLocation robotLoc = robot.location;
						x_sum += robotLoc.x;
						y_sum += robotLoc.y;
					}
					int x = (int) ((float)x_sum / n_hostiles + 0.5);
					int y = (int) ((float)y_sum / n_hostiles + 0.5);
					MapLocation hostileLoc = new MapLocation(x, y);

					Direction dirToMove = hostileLoc.directionTo(myLoc);

					int timesRotated = 0;
					boolean done = false;
					boolean turnLeft = rand.nextBoolean(); //if true keep turning left, if false keep turning right

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
					turnsToRunAway --;
				}
			}
			else
			{
				//attack any foes sending messages if you can
				//should save locs of all foes? May get outdated fast
				if(rc.isWeaponReady())
				{
					Signal[] signals = rc.emptySignalQueue();
					for(Signal signal : signals)
					{
						Team signalTeam = signal.getTeam();
						if(signalTeam == enemyTeam || signalTeam == Team.ZOMBIE)
						{
							MapLocation foeLoc = signal.getLocation();
							if(rc.canAttackLocation(foeLoc))
							{
								rc.attackLocation(foeLoc);
								break;
							}
						}
					}
				}
				//maybe some different flow here?
				//otherwise get foes in your attackRange
				RobotInfo[] foes = rc.senseHostileRobots(rc.getLocation(), RobotPlayer.myType.attackRadiusSquared);
				if(foes.length > 0)
				{
					MapLocation myLoc = rc.getLocation();
					MapLocation foeLoc = foes[0].location;
					int meToFoe = myLoc.distanceSquaredTo(foeLoc);
					if(meToFoe >= GameConstants.TURRET_MINIMUM_RANGE)
					{
						if(rc.isWeaponReady())
						{
							rc.attackLocation(foeLoc);
						}
					}
					else
					{
						turnsToRunAway = maxTurnsToRunAway;
						rc.pack();
						isPacked = true;
						//continue; //might be able to get some running away in
					}
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
}
