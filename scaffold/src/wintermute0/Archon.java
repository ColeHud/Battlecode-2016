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
			escapeIfNecessary();
			
			moveToParts();

			checkMessageForParts();

			buildArchonEyes();

			buildTurrets();

			Clock.yield();
		}
	}

	//build some turrets
	public static void buildTurrets() throws GameActionException
	{
		if(rc.getTeamParts() >= RobotType.TURRET.partCost && Math.random() > .75)
		{	
			Direction dirToBuild = Robot.directions[Robot.random.nextInt(Robot.directions.length)];
			if(rc.canBuild(dirToBuild, RobotType.TURRET) && rc.isCoreReady())
			{
				rc.build(dirToBuild, RobotType.TURRET);
			}
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
			tryToMove(dirToParts);
		}
	}

	public static void checkMessageForParts()
	{
		Signal signal = rc.readSignal();

		if(signal != null && parts == null)
		{
			MapLocation signalLoc = signal.getLocation();
			int[] message = signal.getMessage();
			//if it's a parts signal
			if(message != null && message[0] == 94572)
			{
				parts = signalLoc;
			}
		}	
	}

	//build some scouts for each archon
	public static void buildArchonEyes() throws GameActionException
	{
		int numArchonEyes = 2;
		if(numScouts < numArchonEyes)
		{
			for(Direction d : Robot.directions)
			{
				if(rc.canBuild(d, RobotType.SCOUT) && numScouts < numArchonEyes && rc.isCoreReady())
				{
					rc.build(d, RobotType.SCOUT);
					numScouts++;
					break;
				}
			}
		}
	}
	
	//go into escape mode until a little away from enemies
	//if there's a turret-heavy base, should try to go towards that?
	public static void escapeIfNecessary() throws GameActionException
	{
		MapLocation yourLoc = rc.getLocation();
		RobotInfo[] nearbyHostiles = rc.senseHostileRobots(yourLoc, rc.getType().sensorRadiusSquared);
		Direction fleeDir;
		while(nearbyHostiles.length > 0)
		{	
			int bufferRounds = 5; //extra rounds to run from hostiles even if can't see them anymore
			
			//get average loc of hostiles
			//could also just run away from the closest hostile
			//neither one of those would have you go up or down if you have enemies directly to
			//your left and right
			int n_hostiles = 0;
			int x_sum = 0;
			int y_sum = 0;
			for(RobotInfo robot : nearbyHostiles)
			{
				if((robot.team == Team.ZOMBIE) || (robot.team == Robot.enemy))
				{
					n_hostiles ++;
					MapLocation robotLoc = robot.location;
					x_sum += robotLoc.x;
					y_sum += robotLoc.y;
				}
			}
			int x = (int) ((float)x_sum / n_hostiles + 0.5);
			int y = (int) ((float)y_sum / n_hostiles + 0.5);
			MapLocation hostileLoc = new MapLocation(x, y);
			
			fleeDir = hostileLoc.directionTo(yourLoc);
			fleeDir = tryToFlee(fleeDir);
			
			bufferRounds --;
			
			if(bufferRounds == 0)
			{
				for(int i = 0; i < bufferRounds; i ++)
				{
					fleeDir = tryToFlee(fleeDir);
				}
			}
			
			Clock.yield(); //so doesn't use too much bytecode
			yourLoc = rc.getLocation();
			nearbyHostiles = rc.senseHostileRobots(yourLoc, Robot.type.sensorRadiusSquared);
		}
	}
	
	public static void tryToMove(Direction direction) throws GameActionException
	{
		if (rc.isCoreReady())
		{
			if(rc.canMove(direction))
			{
				rc.move(direction);
			}
			//want 50 or GameConstants.RUBBLE_OBSTRUCTION_THRESH?
			else if(rc.senseRubble(rc.getLocation().add(direction)) >= 50)
			{
				rc.clearRubble(direction);
			}
			else //turn a little and try again
			{
				Direction newDirection;
				if (Math.random() > 0.5) {
					newDirection = direction.rotateLeft();
				}
				else
				{
					newDirection = direction.rotateRight();
				}
				tryToMove(newDirection);
			}
		}
	}
	
	//like tryToRun only does not bother to clear rubble, just run, run away!
	//also returns last direction so can be used to run in same direction later on
	public static Direction tryToFlee(Direction direction) throws GameActionException
	{
		if (rc.isCoreReady())
		{
			if(rc.canMove(direction))
			{
				rc.move(direction);
				return direction;
			}
			else //turn a little and try again
			{
				Direction newDirection;
				if (Math.random() > 0.5) {
					newDirection = direction.rotateLeft();
				}
				else
				{
					newDirection = direction.rotateRight();
				}
				tryToFlee(newDirection);
			}
		}
		Clock.yield();
		tryToFlee(direction); //try again
		return direction; //never reached but compiler wouldn't eat this without it
	}
	
	//get to a loc and come back, if see enemies then just run
	public static void cautiousBoomerang(MapLocation goalLoc) throws GameActionException
	{
		//record initial location
		MapLocation initialLoc = rc.getLocation();
		
		tiptoeToLoc(goalLoc);
		tiptoeToLoc(initialLoc);
	}
	
	//get to loc, if you see any enemies then run away
	public static void tiptoeToLoc(MapLocation goalLoc) throws GameActionException
	{
		//need some limit, might be impossible to get there for some reason
		int maxMoves = 20;
		MapLocation currentLoc = rc.getLocation();
		while(currentLoc != goalLoc && maxMoves > 0)
		{
			escapeIfNecessary();
			Direction dirToGoal = currentLoc.directionTo(goalLoc);
			tryToMove(dirToGoal);
			currentLoc = rc.getLocation();
			maxMoves --;
		}
	}
}
