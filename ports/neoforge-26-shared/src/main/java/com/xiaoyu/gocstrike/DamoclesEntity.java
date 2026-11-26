package com.xiaoyu.gocstrike;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class DamoclesEntity extends Entity {
    private static final float MAX_LIFT = 9;
    private static final double FLIGHT_SPEED = 1.8;
    private static final int FLIGHT_TICKS = 36;
    private static final int TICKING_RADIUS = 2;
    // 新版票据按区块和类型去重。同一区块中最后一场演出结束后再释放。
    private static final Map<ServerLevel, Map<ChunkPos, Integer>> SHOW_TICKETS = new WeakHashMap<>();
    private static final EntityDataAccessor<Long> START = SynchedEntityData.defineId(DamoclesEntity.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Float> LIFT = SynchedEntityData.defineId(DamoclesEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> HEAD = SynchedEntityData.defineId(DamoclesEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> CAPTIVE_ID = SynchedEntityData.defineId(DamoclesEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Long> IMPACT_AT = SynchedEntityData.defineId(DamoclesEntity.class, EntityDataSerializers.LONG);
    private UUID owner;
    private LivingEntity captive;
    private ChunkPos showChunk;
    private float safeLift;

    public DamoclesEntity(EntityType<? extends DamoclesEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
    }

    public void begin(Player player) {
        owner = player.getUUID();
        snapTo(player.getEyePosition().subtract(0, 0.12, 0), player.getYRot(), player.getXRot());
        setDeltaMovement(player.getLookAngle().scale(FLIGHT_SPEED));
    }

    public boolean isOwnedBy(Player player) {
        return player.getUUID().equals(owner);
    }

    public boolean hasLocked() {
        return entityData.get(START) >= 0;
    }

    public float elapsed(float partialTick) {
        return hasLocked() ? Math.max(0, (level().getGameTime() - entityData.get(START) + partialTick) / 20.0F) : 0;
    }

    public float renderTime(float partialTick) {
        long impact = entityData.get(IMPACT_AT);
        // 客户端的插值时钟可能早一帧。等服务端确认下刺接触后，再开始闪光和分解。
        if (impact < 0) {
            return Math.min(elapsed(partialTick), Math.nextDown(DamoclesTimeline.IMPACT));
        }
        return DamoclesTimeline.IMPACT + Math.max(0, (level().getGameTime() - impact + partialTick) / 20.0F);
    }

    public float lift(float time) {
        return entityData.get(LIFT) * DamoclesTimeline.ease(time, DamoclesTimeline.LIFT_START, DamoclesTimeline.LIFT_END);
    }

    public float captiveHeight() {
        return entityData.get(HEAD);
    }

    public Vec3 captiveFeet(float partialTick) {
        Entity tracked = level().getEntity(entityData.get(CAPTIVE_ID));
        if (tracked instanceof LivingEntity living && living.isAlive()) {
            return living.getPosition(partialTick);
        }
        // 远处客户端可能只追踪特效实体；终击后目标也已经移除。此时沿锁定轴线保持徽记的位置。
        return getPosition(partialTick).add(0, lift(elapsed(partialTick)), 0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(START, -1L);
        builder.define(LIFT, 0F);
        builder.define(HEAD, 0F);
        builder.define(CAPTIVE_ID, -1);
        builder.define(IMPACT_AT, -1L);
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel server)) {
            return;
        }
        var caster = server.getPlayerByUUID(owner);
        if (entityData.get(IMPACT_AT) < 0 && (caster == null || !caster.isAlive())) {
            cancel();
        } else if (hasLocked()) {
            tickLocked(server);
        } else {
            tickFlight(server);
        }
    }

    private void tickFlight(ServerLevel server) {
        Vec3 from = position();
        Vec3 to = from.add(getDeltaMovement());
        if (tickCount > FLIGHT_TICKS || !server.hasChunkAt(BlockPos.containing(to))) {
            discard();
            return;
        }
        // 先截到方块碰撞位置，再检查这一段遇到的第一个生物，避免穿墙锁定。
        var wall = server.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        var hit = ProjectileUtil.getEntityHitResult(server, this, from, wall.getLocation(),
                getBoundingBox().expandTowards(wall.getLocation().subtract(from)).inflate(0.3),
                entity -> entity instanceof LivingEntity living && living.isAlive() && living.isPickable()
                        && !living.isSpectator() && !living.getUUID().equals(owner), 0.3F);
        if (hit != null && hit.getEntity() instanceof LivingEntity living) {
            if (server.getEntities(GOCStrike.DAMOCLES.get(), sword -> sword.captive == living).isEmpty()) {
                lock(server, living);
            } else {
                discard();
            }
        } else if (wall.getType() != HitResult.Type.MISS) {
            discard();
        } else {
            setPos(to);
        }
    }

    private void lock(ServerLevel server, LivingEntity living) {
        captive = living;
        living.stopRiding();
        living.setDeltaMovement(Vec3.ZERO);
        living.resetFallDistance();
        setPos(living.position());
        setDeltaMovement(Vec3.ZERO);

        // 从生物的实际碰撞箱逐段检查上方空间，接近天花板时缩短升空距离。
        AABB body = living.getBoundingBox();
        float lift = 0;
        for (float next = 0.25F; next <= MAX_LIFT; next += 0.25F) {
            if (body.maxY + next >= (server.getMinY() + server.getHeight()) || !server.noCollision(living, body.move(0, next, 0))) {
                break;
            }
            lift = next;
        }
        entityData.set(LIFT, lift);
        entityData.set(HEAD, living.getBbHeight());
        entityData.set(CAPTIVE_ID, living.getId());
        // 特效锚点和目标共用此列区块。观众后退时保留服务端实体 tick，收尾或取消时立即释放。
        showChunk = chunkPosition();
        var tickets = SHOW_TICKETS.computeIfAbsent(server, ignored -> new HashMap<>());
        if (tickets.merge(showChunk, 1, Integer::sum) == 1) {
            server.getChunkSource().addTicketWithRadius(GOCStrike.SHOW_TICKET.get(), showChunk, TICKING_RADIUS);
        }
        entityData.set(START, server.getGameTime());
    }

    private void tickLocked(ServerLevel server) {
        float time = elapsed(0);
        if (entityData.get(IMPACT_AT) >= 0) {
            if (renderTime(0) >= DamoclesTimeline.END) {
                discard();
            }
            return;
        }
        if (validCaptive()) {
            float height = lift(time);
            Vec3 destination = position().add(0, height, 0);
            // 只移动到安全高度。升空路径后来被挡住时，把表演留在已到达的位置。
            if (spaceForCaptive(destination)) {
                moveCaptive(destination);
                safeLift = height;
            } else if (height > safeLift) {
                float progress = DamoclesTimeline.ease(time, DamoclesTimeline.LIFT_START, DamoclesTimeline.LIFT_END);
                entityData.set(LIFT, safeLift / progress);
            }
        } else if (captive != null) {
            // 目标死亡、卸载或换维度时保留演出，终击只作用于仍被锁定的原目标。
            captive = null;
            entityData.set(CAPTIVE_ID, -1);
        }
        if (time >= DamoclesTimeline.IMPACT) {
            entityData.set(IMPACT_AT, server.getGameTime());
            LivingEntity target = captive;
            captive = null;
            // 音效仍从剑尖传出。原版用音量参数扩展传播和衰减距离，让远观整个法阵的玩家也听得见。
            double contactY = getY() + lift(time) + captiveHeight();
            server.playSound(null, getX(), contactY, getZ(),
                    SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 10.0F, 0.66F);
            server.playSound(null, getX(), contactY, getZ(),
                    SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.PLAYERS, 9.0F, 1.16F);
            if (target != null) {
                try {
                    StrikeEntity.eraseTarget(target);
                } catch (RuntimeException exception) {
                    GOCStrike.LOGGER.error("Damocles failed to erase entity {}", target.getUUID(), exception);
                }
            }
        }
    }

    private boolean validCaptive() {
        return captive != null && captive.level() == level() && captive.isAlive() && !captive.isSpectator();
    }

    private boolean spaceForCaptive(Vec3 destination) {
        return level().noCollision(captive, captive.getBoundingBox().move(destination.subtract(captive.position())));
    }

    private void moveCaptive(Vec3 destination) {
        captive.stopRiding();
        captive.setDeltaMovement(Vec3.ZERO);
        captive.resetFallDistance();
        if (captive.position().distanceToSqr(destination) > 0.0004) {
            captive.teleportTo(destination.x, destination.y, destination.z);
        }
    }

    public void cancel() {
        if (validCaptive() && spaceForCaptive(position())) {
            moveCaptive(position());
        }
        captive = null;
        discard();
    }

    @Override
    public void onRemovedFromLevel() {
        if (showChunk != null && level() instanceof ServerLevel server) {
            var tickets = SHOW_TICKETS.get(server);
            if (tickets != null) {
                int remaining = tickets.getOrDefault(showChunk, 1) - 1;
                if (remaining == 0) {
                    tickets.remove(showChunk);
                    server.getChunkSource().removeTicketWithRadius(GOCStrike.SHOW_TICKET.get(), showChunk, TICKING_RADIUS);
                } else {
                    tickets.put(showChunk, remaining);
                }
                if (tickets.isEmpty()) {
                    SHOW_TICKETS.remove(server);
                }
            }
            showChunk = null;
        }
        super.onRemovedFromLevel();
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < (hasLocked() ? 256 * 256 : 128 * 128);
    }

    public AABB renderBounds() {
        return hasLocked()
                ? getBoundingBox().inflate(DamoclesTimeline.AFTERSHOCK_RADIUS + 4, 7,
                        DamoclesTimeline.AFTERSHOCK_RADIUS + 4).expandTowards(0,
                        MAX_LIFT + captiveHeight() + DamoclesTimeline.GIANT_LENGTH + DamoclesTimeline.CORE_OVERTRAVEL + 12, 0)
                : getBoundingBox().inflate(2.2);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        // 演出锚点的生命周期由对应的物品和时间轴控制。
        return false;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
    }
}
