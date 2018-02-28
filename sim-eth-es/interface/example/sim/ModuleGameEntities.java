/*
 * $Id$
 * 
 * Copyright (c) 2016, Simsilica, LLC
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions 
 * are met:
 * 
 * 1. Redistributions of source code must retain the above copyright 
 *    notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright 
 *    notice, this list of conditions and the following disclaimer in 
 *    the documentation and/or other materials provided with the 
 *    distribution.
 * 
 * 3. Neither the name of the copyright holder nor the names of its 
 *    contributors may be used to endorse or promote products derived 
 *    from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS 
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT 
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS 
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE 
 * COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, 
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES 
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR 
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) 
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, 
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED 
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package example.sim;

import com.simsilica.mathd.*;
import com.simsilica.es.*;

import example.es.*;
import example.es.Spawner.SpawnType;
import java.util.HashSet;
import org.dyn4j.collision.Filter;
import org.dyn4j.dynamics.BodyFixture;
import org.dyn4j.dynamics.Force;
import org.dyn4j.geometry.Convex;
import org.dyn4j.geometry.Geometry;
import org.dyn4j.geometry.Vector2;
import org.ini4j.Ini;

/**
 * Utility methods for creating the common game entities used by the simulation.
 * In cases where a game entity may have multiple specific componnets or
 * dependencies used to create it, it can be more convenient to have a
 * centralized factory method. Especially if those objects are widely used. For
 * entities with only a few components or that are created by one system and
 * only consumed by one other, then this is not necessarily true.
 *
 */
public class ModuleGameEntities {
    
    //TODO: All constants should come through the parameters - for now, they come from the constants
    //TODO: All parameters should be dumb types and should be the basis of the complex types used in the backend
    public static EntityId createShip(EntityId parent, EntityData ed, Ini settings) {
        EntityId result = ed.createEntity();
        Name name = ed.getComponent(parent, Name.class);
        ed.setComponent(result, name);

        ed.setComponents(result,
                ViewTypes.ship_warbird(ed),
                ShipTypes.warbird(ed),
                PhysicsMassTypes.normal(ed),
                PhysicsShapes.ship(settings)) ;

        ed.setComponent(result, new Frequency(1));
        ed.setComponent(result, new Gold(0));
        ed.setComponent(result, new HitPoints(settings.get("Warbird", "StartingHelath", int.class)));
        //ed.setComponent(result, new HitPoints(GameConstants.SHIPHEALTH));

        return result;
    }

    public static EntityId createGravSphere(Vec3d pos, double radius, EntityData ed, Ini settings) {
        EntityId result = ed.createEntity();
        ed.setComponents(result, ViewTypes.gravSphereType(ed),
                new Position(pos, new Quatd().fromAngles(-Math.PI * 0.5, 0, 0), 0.0),
                new SphereShape(radius, new Vec3d()));
        return result;
    }

    public static EntityId createBounty(Vec3d pos, EntityData ed, Ini settings) {
        EntityId result = ed.createEntity();

        ed.setComponents(result, ViewTypes.bounty(ed),
                new Position(pos, new Quatd(), 0f),
                //new Bounty(GameConstants.BOUNTYVALUE),
                new Bounty(settings.get("Bounties", "DefaultValue", int.class)),
                PhysicsShapes.bounty(settings),
                new SphereShape(settings.get("Bounties", "ViewSize", int.class)),
                //new SphereShape(ViewConstants.BOUNTYSIZE, new Vec3d()),
                new Decay(settings.get("Bounties", "Decay", int.class)));
                //new Decay(GameConstants.BOUNTYDECAY));
        return result;
    }

    public static EntityId createBountySpawner(Vec3d pos, double radius, EntityData ed, Ini settings) {
        EntityId result = ed.createEntity();
        ed.setComponents(result,
                //Possible to add model if we want the players to be able to see the spawner
                new Position(pos, new Quatd(), 0f),
                //new Spawner(GameConstants.BOUNTYMAXCOUNT, Spawner.SpawnType.Bounties));
                new Spawner(settings.get("BountySpawners", "MaxCount", int.class), Spawner.SpawnType.Bounties));
        return result;
    }

    public static EntityId createBomb(Vec3d location, Quatd quatd, double rotation, Vector2 linearVelocity, long decayMillis, EntityData ed, Ini settings) {
        EntityId lastBomb = ed.createEntity();

        ed.setComponents(lastBomb, ViewTypes.bomb(ed),
                new Position(location, quatd, rotation),
                new PhysicsVelocity(new Vector2(linearVelocity.x, linearVelocity.y)),
                new Decay(decayMillis),
                ProjectileTypes.bomb(ed),
                PhysicsMassTypes.normal_bullet(ed),
                PhysicsShapes.bomb(settings));

        return lastBomb;
    }

    public static EntityId createDelayedBomb(Vec3d location, Quatd quatd, double rotation, Vector2 linearVelocity, long decayMillis, long scheduledMillis, HashSet<EntityComponent> delayedComponents, EntityData ed, Ini settings) {

        EntityId lastDelayedBomb = ModuleGameEntities.createBomb(location, quatd, rotation, linearVelocity, decayMillis, ed, settings);

        ed.setComponents(lastDelayedBomb, new Delay(scheduledMillis, delayedComponents, Delay.SET));
        ed.setComponents(lastDelayedBomb, ProjectileTypes.gravityBomb(ed));

        return lastDelayedBomb;
    }

    public static EntityId createBullet(Vec3d location, Quatd quatd, double rotation, Vector2 linearVelocity, long decayMillis, EntityData ed, Ini settings) {
        EntityId lastBomb = ed.createEntity();

        ed.setComponents(lastBomb, ViewTypes.bullet(ed),
                new Position(location, quatd, rotation),
                new PhysicsVelocity(new Vector2(linearVelocity.x, linearVelocity.y)),
                new Decay(decayMillis),
                PhysicsMassTypes.normal_bullet(ed),
                ProjectileTypes.bullet(ed),
                PhysicsShapes.bullet(settings));

        return lastBomb;
    }

    public static EntityId createArena(int arenaId, EntityData ed, Ini settings) { //TODO: Should have Position as a parameter in case we want to create more than one arena
        EntityId lastArena = ed.createEntity();

        ed.setComponents(lastArena, ViewTypes.arena(ed),
                new Position(new Vec3d(0, 0, arenaId), new Quatd(), 0f)
        );

        return lastArena;
    }

    public static EntityId createMapTile(String tileSet, short tileIndex, Vec3d location, Convex c, double invMass, EntityData ed, Ini settings) {
        EntityId lastTileInfo = ed.createEntity();

        ed.setComponents(lastTileInfo, ViewTypes.mapTile(ed),
                new Position(location, new Quatd(), 0f),
                new TileInfo(tileSet, tileIndex), //Tile set and tile index
                PhysicsMassTypes.infinite(ed),
                PhysicsShapes.mapTile(c));

        return lastTileInfo;
    }

    /*
    public static EntityId createMapTile(MapTileType mapTileType, Vec3d location, EntityData ed, Convex c) {
        EntityId lastMapTile = ed.createEntity();

        ed.setComponents(lastMapTile, ObjectTypes.mapTile(ed),
                new Position(location, new Quatd(), 0f),
                mapTileType, //TODO: Should be parameterized
                new MassProperties(PhysicsConstants.MAPTILEMASS), //for Physics
                new PhysicsShape(new BodyFixture(c))); //for physics - for now, only 1 by 1 tiles created (square)

        return lastMapTile;
    }
     */
    //Explosion is for now only visual, so only object type and position
    public static EntityId createExplosion2(Vec3d location, Quatd quat, EntityData ed, Ini settings) {
        EntityId lastExplosion = ed.createEntity();

        ed.setComponents(lastExplosion,
                ViewTypes.explosion2(ed),
                new Position(location, quat, 0f),
                //new Decay(ViewConstants.EXPLOSION2DECAY));
                new Decay(settings.get("Explosions", "Type2Decay", int.class)));

        return lastExplosion;
    }

    public static EntityId createWormhole(Vec3d location, double radius, double targetAreaRadius, double force, String gravityType, Vec3d warpTargetLocation, EntityData ed, Ini settings) {
        EntityId lastWormhole = ed.createEntity();

        ed.setComponents(lastWormhole,
                ViewTypes.wormhole(ed),
                new Position(location, new Quatd(), 0f),
                PhysicsMassTypes.infinite(ed),
                new GravityWell(radius, force, gravityType),
                PhysicsShapes.wormhole(settings),
                new WarpTouch(warpTargetLocation));

        return lastWormhole;
    }

    public static EntityId createAttack(EntityId owner, String attackType, EntityData ed, Ini settings) {
        EntityId lastAttack = ed.createEntity();
        ed.setComponents(lastAttack,
                new Attack(owner),
                ProjectileType.create(attackType, ed),
                new Damage(-20));

        return lastAttack;
    }

    public static EntityId createForce(EntityId owner, Force force, Vector2 forceWorldCoords, EntityData ed, Ini settings) {
        EntityId lastForce = ed.createEntity();
        ed.setComponents(lastForce,
                new PhysicsForce(owner, force, forceWorldCoords));

        return lastForce;
    }

    public static EntityId createOver5(Vec3d location, double radius, double force, String gravityType, EntityData ed, Ini settings) {
        EntityId lastOver5 = ed.createEntity();

        ed.setComponents(lastOver5,
                ViewTypes.over5(ed),
                new Position(location, new Quatd(), 0f),
                PhysicsMassTypes.infinite(ed),
                new GravityWell(radius, force, gravityType),
                PhysicsShapes.over5(settings));

        return lastOver5;
    }

    /**
     * Small asteroid with animation
     *
     * @param location the Vec3d position of the asteroid
     * @param ed the entitydata set to create the entity in
     * @return the entityid of the created entity
     */
    public static EntityId createOver1(Vec3d location, EntityData ed, Ini settings) {
        EntityId lastOver1 = ed.createEntity();

        ed.setComponents(lastOver1,
                ViewTypes.over1(ed),
                new Position(location, new Quatd(), 0f),
                PhysicsMassTypes.normal(ed),
                PhysicsShapes.over1(settings));

        return lastOver1;
    }

    /**
     * Medium asteroid with animation
     *
     * @param location the Vec3d position of the asteroid
     * @param ed the entitydata set to create the entity in
     * @return the entityid of the created entity
     */
    public static EntityId createOver2(Vec3d location, EntityData ed, Ini settings) {
        EntityId lastOver2 = ed.createEntity();

        ed.setComponents(lastOver2,
                ViewTypes.over2(ed),
                new Position(location, new Quatd(), 0f),
                PhysicsMassTypes.normal(ed),
                PhysicsShapes.over2(settings));

        return lastOver2;
    }

    public static EntityId createWarpEffect(Vec3d location, EntityData ed, Ini settings) {
        EntityId lastWarpTo = ed.createEntity();

        ed.setComponents(lastWarpTo,
                ViewTypes.warp(ed),
                new Position(location, new Quatd(), 0f),
                //new Decay(ViewConstants.WARPDECAY));
                new Decay(settings.get("Warp", "Decay", int.class)));

        return lastWarpTo;
    }

    public static EntityId createRepelEffect(Vec3d location, EntityData ed, Ini settings) {
        EntityId lastWarpTo = ed.createEntity();

        ed.setComponents(lastWarpTo,
                ViewTypes.warp(ed),
                new Position(location, new Quatd(), 0f),
                //new Decay(ViewConstants.REPELDECAY));
                new Decay(settings.get("Repel", "Decay", int.class)));

        return lastWarpTo;
    }

    public static EntityId createCaptureTheFlag(Vec3d location, EntityData ed, Ini settings) {
        EntityId lastFlag = ed.createEntity();

        ed.setComponents(lastFlag,
                ViewTypes.flag_theirs(ed),
                new Position(location, new Quatd(), 0f),
                PhysicsShapes.flag(settings),
                PhysicsMassTypes.infinite(ed),
                new Flag(),
                new Frequency(0));

        return lastFlag;
    }

    public static EntityId createMob(Vec3d location, EntityData ed, Ini settings) {
        EntityId lastMob = ed.createEntity();

        ed.setComponents(lastMob,
                ViewTypes.mob(ed),
                MobTypes.mob1(ed),
                //new Drivable(), //old
                new Steerable(),
                new Position(location, new Quatd(), 0f),
                PhysicsShapes.mob(settings),
                PhysicsMassTypes.normal(ed),
                //new HitPoints(GameConstants.MOBHEALTH));
                new Decay(settings.get("Enemies", "Health", int.class)));

        return lastMob;
    }

    public static EntityId createTower(Vec3d location, EntityData ed, Ini settings) { // add tower-type ?
        EntityId lastTower = ed.createEntity();
        double rand = Math.random() * Math.PI;

        ed.setComponents(lastTower,
                ViewTypes.tower(ed), // this
                TowerTypes.tower1(ed, lastTower),
                //new Position(location, new Quatd(0,0,Math.sin(rand),Math.cos(rand)), 0f),
                new Position(location, new Quatd(0, 0, -0.5, 0.5), 0f),
                PhysicsShapes.tower(settings), // and this moved to type creation, for modularity ?
                PhysicsMassTypes.infinite(ed),
                new Damage(20)); //The amount of damage the tower does

        return lastTower;
    }

    public static EntityId createBase(Vec3d location, EntityData ed, Ini settings) {
        EntityId lastBase = ed.createEntity();

        ed.setComponents(lastBase,
                ViewTypes.base(ed),
                BaseTypes.base1(ed),
                new SteeringSeekable(),
                new Position(location, new Quatd(), 0f),
                PhysicsShapes.base(settings),
                PhysicsMassTypes.infinite(ed),
                //new HitPoints(GameConstants.BASEHEALTH));
                new Decay(settings.get("Base", "Health", int.class)));

        return lastBase;
    }

    public static EntityId createHealthBuff(int healthChange, EntityId target, EntityData ed, Ini settings) {

        EntityId lastHealthBuff = ed.createEntity();

        ed.setComponents(lastHealthBuff,
                new HealthChange(healthChange), //apply the damage
                new Buff(target, 0)); //apply right away

        return lastHealthBuff;
    }
}
