package badplayer;

import battlecode.common.*;

import java.util.Random;

public class RobotPlayer {

    /**
     * run() is the method that is called when a robot is instantiated in the Battlecode world.
     * If this method returns, the robot dies!
     **/
    @SuppressWarnings("unused")
    public static void run(RobotController rc) {
        // You can instantiate variables here.
        Direction[] directions = {Direction.NORTH, Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST,
                Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST};
        RobotType[] robotTypes = {RobotType.SCOUT, RobotType.SOLDIER, RobotType.SOLDIER, RobotType.SOLDIER,
                RobotType.GUARD, RobotType.GUARD, RobotType.VIPER, RobotType.TURRET};
        RobotType[] mostlyTurrets = {RobotType.TURRET, RobotType.TURRET, RobotType.TURRET, RobotType.TURRET,
        		RobotType.SOLDIER, RobotType.SOLDIER, RobotType.SCOUT, RobotType.SCOUT};
        Random rand = new Random(rc.getID());
        int myAttackRangeSquared = 0;
        Team myTeam = rc.getTeam();
        Team enemyTeam = myTeam.opponent();

        if (rc.getType() == RobotType.ARCHON) {
            try {
                // Any code here gets executed exactly once at the beginning of the game.
            } catch (Exception e) {
                // Throwing an uncaught exception makes the robot die, so we need to catch exceptions.
                // Caught exceptions will result in a bytecode penalty.
                System.out.println(e.getMessage());
                e.printStackTrace();
            }

            while (true) {
                // This is a loop to prevent the run() method from returning. Because of the Clock.yield()
                // at the end of it, the loop will iterate once per game round.
                try {
                    int fate = rand.nextInt(1000);

                    // Can get MapLocation of Archon from this, but stay away?
                    getAndSendNearbyEnemyArchonLoc(rc, myTeam);

                    if (rc.isCoreReady()) {
                        if (fate < 50 || notEnoughSpace(rc, directions)) {
                            // Choose a random direction to try to move in
                            Direction dirToMove = directions[fate % 8];
                            tryToMoveOrExcavate(rc, dirToMove);
                        } else {
                        	RobotType typeToBuild;
                        	// Try to continue a wall or fortress of turrets
                        	if (turretNearby(rc)) {
                        		typeToBuild = mostlyTurrets[fate % 8];
                        	} else {
                                // Choose a random unit to build
                        		typeToBuild = robotTypes[fate % 8];
                        	}
                            
                            // Check for sufficient parts
                            if (rc.hasBuildRequirements(typeToBuild)) {
                                // Choose a random direction to try to build in
                                Direction dirToBuild = directions[rand.nextInt(8)];
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
                    }

                    Clock.yield();
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                    e.printStackTrace();
                }
            }
        
        } else if (rc.getType() != RobotType.TURRET) {
            try {
                // Any code here gets executed exactly once at the beginning of the game.
                myAttackRangeSquared = rc.getType().attackRadiusSquared;
            } catch (Exception e) {
                // Throwing an uncaught exception makes the robot die, so we need to catch exceptions.
                // Caught exceptions will result in a bytecode penalty.
                System.out.println(e.getMessage());
                e.printStackTrace();
            }

            while (true) {
                // This is a loop to prevent the run() method from returning. Because of the Clock.yield()
                // at the end of it, the loop will iterate once per game round.
                try {
                    int fate = rand.nextInt(1000);

                    /*
                    if (fate % 5 == 3) {
                        // Send a normal signal
                        rc.broadcastSignal(80);
                    }
                    */
                    
                    boolean shouldAttack = false;

                    // If this robot type can attack, check for enemies within range and attack one
                    if (myAttackRangeSquared > 0) {
                        RobotInfo[] enemiesWithinRange = rc.senseNearbyRobots(myAttackRangeSquared, enemyTeam);
                        RobotInfo[] zombiesWithinRange = rc.senseNearbyRobots(myAttackRangeSquared, Team.ZOMBIE);
                        if (enemiesWithinRange.length > 0) {
                            shouldAttack = true;
                            // Check if weapon is ready
                            if (rc.isWeaponReady()) {
                                rc.attackLocation(getMinHealthRobotOrArchon(enemiesWithinRange).location);
                            }
                        } else if (zombiesWithinRange.length > 0) {
                            shouldAttack = true;
                            // Check if weapon is ready
                            if (rc.isWeaponReady()) {
                                rc.attackLocation(getMinHealthRobot(zombiesWithinRange).location);
                            }
                        }
                    }

                    if (!shouldAttack) {
                        if (rc.isCoreReady()) {
                        	MapLocation possibleEnemyArchonLoc;
                            if (rc.getType() == RobotType.SCOUT) {
                            	 possibleEnemyArchonLoc = getAndSendNearbyEnemyArchonLoc(rc, myTeam);
                            } else {
                            	 possibleEnemyArchonLoc = getMessageLoc(rc, myTeam);
                            }
                            if (foundSomething(possibleEnemyArchonLoc)) {
                            	Direction dirToMove = rc.getLocation().directionTo(possibleEnemyArchonLoc);
                            	tryToMoveOrExcavate(rc, dirToMove);
                            } else if (fate < 600) {
                            	// Choose a random direction to try to move in
                            	Direction dirToMove = directions[fate % 8];
                            	tryToMoveOrExcavate(rc, dirToMove);
                            }
                        }
                    }

                    Clock.yield();
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                    e.printStackTrace();
                }
            }
        } else if (rc.getType() == RobotType.TURRET) {
            try {
            	myAttackRangeSquared = rc.getType().attackRadiusSquared;
            } catch (Exception e) {
                System.out.println(e.getMessage());
                e.printStackTrace();
            }

            while (true) {
                // This is a loop to prevent the run() method from returning. Because of the Clock.yield()
                // at the end of it, the loop will iterate once per game round.
                try {
                    // If this robot type can attack, check for enemies within range and attack one
                    if (rc.isWeaponReady()) {
                        RobotInfo[] enemiesWithinRange = rc.senseNearbyRobots(myAttackRangeSquared, enemyTeam);
                        RobotInfo[] zombiesWithinRange = rc.senseNearbyRobots(myAttackRangeSquared, Team.ZOMBIE);
                        if (enemiesWithinRange.length > 0) {
                            for (RobotInfo enemy : enemiesWithinRange) {
                                // Check whether the enemy is in a valid attack range (turrets have a minimum range)
                                if (rc.canAttackLocation(enemy.location)) {
                                    rc.attackLocation(enemy.location);
                                    break;
                                }
                            }
                        } else if (zombiesWithinRange.length > 0) {
                            for (RobotInfo zombie : zombiesWithinRange) {
                                if (rc.canAttackLocation(zombie.location)) {
                                    rc.attackLocation(zombie.location);
                                    break;
                                }
                            }
                        }
                    }

                    Clock.yield();
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                    e.printStackTrace();
                }
            }
        }
    }

    // Checks if there is a turret right around the RobotController (next to it really)
    public static boolean turretNearby(RobotController rc) throws GameActionException {
		MapLocation[] locsAround = MapLocation.getAllMapLocationsWithinRadiusSq(rc.getLocation(), 9);
		for (MapLocation locAround : locsAround) {
			RobotInfo robotFound = rc.senseRobotAtLocation(locAround);
			if (robotFound != null) {
				if (robotFound.type == RobotType.TURRET) {
					return true;
				}
			}
		}
		return false;
	}

	public static boolean notEnoughSpace(RobotController rc, Direction[] directions) {
    	int freeDirs = 0;
    	for (Direction direction : directions) {
    		if (rc.canMove(direction)) {
				freeDirs ++;
			}
		}
		return freeDirs < 3;
	}

	public static void tryToMoveOrExcavate(RobotController rc, Direction dirToMove) throws GameActionException {
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

	// Checks whether or not a location is the "we didn't find anything" location
    public static boolean foundSomething(MapLocation loc) {
    	return loc.x != -1 || loc.y != -1;
    }
    
    // Gets the robot with the lowest health
    public static RobotInfo getMinHealthRobot(RobotInfo[] robots) {
    	double minHealth = robots[0].health;
    	RobotInfo target = robots[0];
    	for (RobotInfo robot : robots) {
    		double currentHealth = robot.health;
    		if (currentHealth < minHealth) {
    			minHealth = currentHealth;
    			target = robot;
    		}
    	}
    	return target;
    }
    
    // Gets the robot with the lowest health or an Archon if there's an Archon
    public static RobotInfo getMinHealthRobotOrArchon(RobotInfo[] robots) {
    	double minHealth = robots[0].health;
    	RobotInfo target = robots[0];
    	for (RobotInfo robot : robots) {
    		if (robot.type == RobotType.ARCHON) {
    			return robot;
    		} else {
    			double currentHealth = robot.health;
    			if (currentHealth < minHealth) {
    				minHealth = currentHealth;
    				target = robot;
    			}
    		}
    	}
    	return target;
    }
    
    public static MapLocation getMessageLoc(RobotController rc, Team myTeam) {
    	Signal[] signals = rc.emptySignalQueue();
    	if (signals.length > 0) {
            for (Signal signal : signals) {
                if (signal.getTeam() == myTeam) {
                    int[] coords = signals[0].getMessage();
                    return new MapLocation(coords[0], coords[1]);
                }
            }
        }
        return new MapLocation(-1, -1); // not found
    }
    
    public static MapLocation getAndSendNearbyEnemyArchonLoc(RobotController rc, Team myTeam) throws GameActionException {
    	MapLocation loc = getMessageLoc(rc, myTeam);
    	int broadcastRadiusSquared = 16;
    	if (foundSomething(loc)) {
    		rc.broadcastMessageSignal(loc.x, loc.y, broadcastRadiusSquared);
    		return loc;
    	} else {
    		// Find Archons
    		MapLocation nearbyEnemyArchonLoc = getNearbyEnemyArchonLoc(rc, myTeam);
    		// Send location of any Archons if you see any of them
    		if (foundSomething(nearbyEnemyArchonLoc)) { 			
    			rc.broadcastMessageSignal(nearbyEnemyArchonLoc.x, nearbyEnemyArchonLoc.y, broadcastRadiusSquared);
        		return nearbyEnemyArchonLoc;
    		}
    		return new MapLocation(-1, -1);
    	}
    }

    public static MapLocation getNearbyEnemyArchonLoc(RobotController rc, Team myTeam) {
    	RobotInfo[] robots = rc.senseNearbyRobots();
    	for (RobotInfo robot : robots) {
    		//if (robot.team == enemyTeam && robot.type == RobotType.ARCHON) {
        	if (robot.team != myTeam) {
    			return robot.location;
    		}
    	}
    	return new MapLocation(-1, -1);
    }
}
