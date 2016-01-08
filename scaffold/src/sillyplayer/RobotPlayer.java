//Archons use too many bytecodes and blow up right now, maybe other units

package sillyplayer;

import battlecode.common.*;

import java.util.Arrays;
import java.util.Random;

public class RobotPlayer {

    /**
     * run() is the method that is called when a robot is instantiated in the Battlecode world.
     * If this method returns, the robot dies!
     * @throws GameActionException 
     **/
    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {
        // You can instantiate variables here.
        Direction[] directions = {Direction.NORTH, Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST,
                Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST};
        RobotType[] robotTypes = {RobotType.ARCHON, RobotType.SCOUT, RobotType.TURRET, RobotType.SOLDIER, RobotType.VIPER, RobotType.GUARD};
        RobotType[] clumpingTypes = {RobotType.SOLDIER, RobotType.GUARD, RobotType.VIPER};
        Random random = new Random(rc.getID());
        int myAttackRange = 0;
        Team myTeam = rc.getTeam();
        Team enemyTeam = myTeam.opponent();
        
        RobotType rcType = rc.getType();
        
        // Set up messaging so after 100 times same message, stop doing it
        // Need to check core delay, weapon delays more
        
        if (rcType == RobotType.ARCHON) {
        	while(true) {
        		if(! tellOfEnemiesAndRun(rc, enemyTeam)) {
        			if(Math.random() < 0.33) {
        				moveRandomly(rc, directions, random);
        			} else {
        				if(Math.random() > 0.4) {
        					addTurretToFortress(rc, random, directions);
        				} else {
        					buildRandomUnit(rc, random, clumpingTypes, directions);
        				}
        			}
        		}
        	// If you see an enemy then broadcast its loc and try to move away
        	// Else some of the time try to move around, go where lots of parts
        	// Some of the time build
        		// If around turrets, mostly build fortress of turrets
        		// Build other units weighted by usefulness, every now and then a turret
        		Clock.yield();
        	}
        } else if (rcType == RobotType.SCOUT) {
        	while(true) {
        		if(! tellOfEnemies(rc, enemyTeam))
        		{
        			moveRandomly(rc, directions, random);
        		}
        		Clock.yield();
        	}
        } else if (rcType == RobotType.TURRET) {
        	while(true) {
        		boolean unused = tryAttackAndTell(rc, enemyTeam, random);
        		Clock.yield();
        	}
        	// Can move around? Go near other turrets? If packed and see enemy then unpack
       
        // } else if (rcType == RobotType.SOLDIER) {
        // Special things for ranged guys?
        // } else if (rcType == RobotType.VIPER) {
        	// Only attack sometimes? Don't want lots of zombies.
        
        } else if ((rcType == RobotType.GUARD) || (rcType == RobotType.VIPER) || (rcType == RobotType.SOLDIER)) {
        	while(true) {
        		if (! tryAttackAndTell(rc, enemyTeam, random)) {
        			// Same cost as sensing just ones you can attack? Init above and use for attack?
        			RobotInfo[] nearbyRobots = rc.senseNearbyRobots();
        			if (isArchonGuard(rc)) {
        				guardFriendlyArchon(nearbyRobots, rc, myTeam);
        			} else if (! tryCheckMessagesAndInvestigate(rc, myTeam))
        				if (! trySeeAndTellAndInvestigate(nearbyRobots, rc, enemyTeam)) {
        					if (! tryGoTowardsFriendlies(nearbyRobots, rc, myTeam, clumpingTypes)) {
        						moveRandomly(rc, directions, random);
        					}
        				}
        		}
        		Clock.yield();
        	}
        }
    }
    
	public static void attack(RobotController rc, MapLocation loc) throws GameActionException {
		if(rc.canAttackLocation(loc) && rc.isCoreReady() && rc.isWeaponReady()) {
			rc.attackLocation(loc);
		}
	}
    
    public static boolean tellOfEnemiesAndRun(RobotController rc, Team enemyTeam) throws GameActionException {
    	MapLocation yourLoc = rc.getLocation();
    	RobotInfo[] nearbyHostiles = rc.senseHostileRobots(yourLoc, rc.getType().sensorRadiusSquared);
    	Direction fleeDir;
    	while(nearbyHostiles.length > 0) {	
    		// Tell of enemy
    		rc.broadcastSignal(conflictMessagePriority(rc, nearbyHostiles[0]));
    		
    		int bufferRounds = 5; //extra rounds to run from hostiles even if can't see them anymore

    		//get average loc of hostiles
    		//could also just run away from the closest hostile
    		//neither one of those would have you go up or down if you have enemies directly to
    		//your left and right
    		int n_hostiles = 0;
    		int x_sum = 0;
    		int y_sum = 0;
    		for(RobotInfo robot : nearbyHostiles) {
    			if((robot.team == Team.ZOMBIE) || (robot.team == rc.getTeam().opponent())) {
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
    		fleeDir = tryToFlee(rc, fleeDir);

    		bufferRounds --;

    		if(bufferRounds == 0) {
    			for(int i = 0; i < bufferRounds; i ++) {
    				fleeDir = tryToFlee(rc, fleeDir);
    			}
    			return true;
    		}

    		Clock.yield(); //so doesn't use too much bytecode
    		yourLoc = rc.getLocation();
    		nearbyHostiles = rc.senseHostileRobots(yourLoc, rc.getType().sensorRadiusSquared);
    	}
		return false;
    }
    
    public static Direction tryToFlee(RobotController rc, Direction direction) throws GameActionException {
    	if (rc.isCoreReady()) {
    		if(rc.canMove(direction)) {
    			rc.move(direction);
    			return direction;
    		} else { //turn a little and try again
    			Direction newDirection;
    			if (Math.random() > 0.5) {
    				newDirection = direction.rotateLeft();
    			} else {
    				newDirection = direction.rotateRight();
    			}
    			tryToFlee(rc, newDirection);
    		}
    	}
    	Clock.yield();
    	tryToFlee(rc, direction); //try again
    	return direction; //never reached but compiler wouldn't eat this without it
    }
    
    public static void buildRandomUnit(RobotController rc, Random random, RobotType[] units, Direction[] directions) throws GameActionException {
		if (rc.getCoreDelay() < 1) {
			// Choose a random unit to build
            RobotType typeToBuild = units[random.nextInt(units.length)];
            buildUnit(rc, random, typeToBuild, directions);
		}
	}
    
    public static void buildUnit(RobotController rc, Random random, RobotType typeToBuild, Direction[] directions) throws GameActionException {
    	// Check for sufficient parts
    	if (rc.hasBuildRequirements(typeToBuild)) {
    		// Choose a random direction to try to build in
    		Direction dirToBuild = directions[random.nextInt(directions.length)];
    		for (int i = 0; i < 8; i++) {
    			// If possible, build in this direction
    			if (rc.canBuild(dirToBuild, typeToBuild)) {
    				rc.build(dirToBuild, typeToBuild);
    				break;
    			} else {
    				// Rotate the direction to try
    				dirToBuild = dirToBuild.rotateLeft();
    			}
    		}
    	}
    }

    // Not fancy at all yet
    public static void addTurretToFortress(RobotController rc, Random random, Direction[] directions) throws GameActionException {
		buildUnit(rc, random, RobotType.TURRET, directions);
	}

	// Checks if a robot has been chosen as an Archon guard (to stay with the Archons)
    public static boolean isArchonGuard(RobotController rc) {
    	return rc.getID() % 3 == 0;
    }
    
    // Messages your location if there are any hostiles around you
    public static boolean tellOfEnemies(RobotController rc, Team myTeam) throws GameActionException {
    	RobotInfo[] hostiles = rc.senseHostileRobots(rc.getLocation(), rc.getType().sensorRadiusSquared);
    	if(hostiles.length > 0) {
    		rc.broadcastSignal(conflictMessagePriority(rc, hostiles[0]));
    	}
    	return false;
    }
    
    // Try to attack any enemies and message their locs, return whether it worked out
    public static boolean tryAttackAndTell(RobotController rc, Team enemyTeam, Random random) throws GameActionException {
    	RobotInfo[] foes = rc.senseHostileRobots(rc.getLocation(), rc.getType().attackRadiusSquared);
    	if(foes.length > 0) {
    		RobotInfo foe = foes[random.nextInt(foes.length)];
    		attack(rc, foe.location);
    		rc.broadcastSignal(conflictMessagePriority(rc, foe));
    		return true;
    	}
    	return false;
    }

    // Check for a message from someone on your team with a location to head towards, return whether it worked out
    public static boolean tryCheckMessagesAndInvestigate(RobotController rc, Team myTeam) throws GameActionException {
    	//int closeEnoughSquared = 25; //how close is close enough to the location mentioned in the message
    	// If there is a message, share it too and head towards the mentioned loc
    	Signal[] incomingSignals = rc.emptySignalQueue();
    	for(Signal signal : incomingSignals) {
    		if(signal.getTeam() == myTeam) {
    			MapLocation goalLoc = signal.getLocation();
    			moveInDir(rc, rc.getLocation().directionTo(goalLoc));
    			return true;
    		}
    	}
    	return false;
    }
    
	// If already close enough to Archon, stay still, else move towards
    public static void guardFriendlyArchon(RobotInfo[] nearbyRobots, RobotController rc, Team myTeam) throws GameActionException {
    	int closeEnoughSquared = 9;
    	MapLocation rcLoc = rc.getLocation();
    	for (RobotInfo robot : nearbyRobots) {
    		if ((robot.type == RobotType.ARCHON) && (robot.team == myTeam)) {
    			MapLocation archonLoc = robot.location;
    			if (rcLoc.distanceSquaredTo(archonLoc) > closeEnoughSquared) {
    				moveInDir(rc, rcLoc.directionTo(archonLoc));
    			}
    		}
    	}
    }
    
    // Move in direction if you can, clear rubble if in the way
    public static void moveInDir(RobotController rc, Direction dirToMove) throws GameActionException {
    	if(rc.getCoreDelay() < 1) {
    		// Check the rubble in that direction
    		if (rc.senseRubble(rc.getLocation().add(dirToMove)) >= GameConstants.RUBBLE_OBSTRUCTION_THRESH) {
    			// Too much rubble, so I should clear it
    			rc.clearRubble(dirToMove);
    			// Check if I can move in this direction
    		} else if (rc.canMove(dirToMove)) {
    			// Move
    			rc.move(dirToMove);
    		}

    	}
    }
    
    // Moves around randomly, clears rubble if in the way
    public static void moveRandomly(RobotController rc, Direction[] directions, Random random) throws GameActionException {
    	Direction direction = directions[random.nextInt(directions.length)];
    	moveInDir(rc, direction);
    }
    
    // If you see an enemy then broadcast your loc and go towards it, return whether it works out
    public static boolean trySeeAndTellAndInvestigate(RobotInfo[] nearbyRobots, RobotController rc, Team enemyTeam) throws GameActionException {
    	for(RobotInfo robot : nearbyRobots) {
    		if(robot.team == enemyTeam || robot.team == Team.ZOMBIE) {
       			rc.broadcastSignal(conflictMessagePriority(rc, robot));
    			moveInDir(rc, rc.getLocation().directionTo(robot.location));
    			return true;
    		}
    	}
    	return false;
    }
    
    // Go towards specific friendlies if there are any around you, return whether it works out
    public static boolean tryGoTowardsFriendlies(RobotInfo[] nearbyRobots, RobotController rc, Team myTeam, RobotType[] friendlyTypes) throws GameActionException {
    	for(RobotInfo robot : nearbyRobots) {
    		if(robot.team == myTeam) {
    			//some better way to check if robot.type is in friendlyTypes? contains doesn't work
    			for(RobotType friendlyType : friendlyTypes) {
    				if(robot.type == friendlyType) {
    	    			moveInDir(rc, rc.getLocation().directionTo(robot.location));
    	    			return true;
    				}
    			}
    		}
    	}
    	return false;
    }
    
    // Figures out how important a message is based on the enemy and who could be attacked, value is used for radius of message
    // Can maybe do something with strengthWeight here
    public static int conflictMessagePriority(RobotController rc, RobotInfo enemy) {
    	// If Archons are involved, highest priority
    	if ((rc.getType() == RobotType.ARCHON) || (enemy.type == RobotType.ARCHON)) {
    		return 144;
    	} else {
    		return 100;
    	}
	}
}
