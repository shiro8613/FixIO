package dev.shiro8613.fixio.fabric.mixin;

import dev.shiro8613.fixio.nativeapi.compute.NativeCompute;
import dev.shiro8613.fixio.nativeapi.compute.NativeLongArray;
import dev.shiro8613.fixio.nativeapi.compute.NativeReqCapArray;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.minecraft.util.AbortableIterationConsumer;
import net.minecraft.world.level.entity.EntityAccess;
import net.minecraft.world.level.entity.EntitySection;
import net.minecraft.world.level.entity.EntitySectionStorage;
import net.minecraft.world.phys.AABB;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntitySectionStorage.class)
public class EntitySectionStorageMixin<T extends EntityAccess> {

    @Shadow
    private Long2ObjectMap<EntitySection<T>> sections;

    @Unique
    private final NativeLongArray keyArray = new NativeLongArray(1024);

    @Unique
    private final NativeReqCapArray longResultArray = new NativeReqCapArray(1024, Long.BYTES);

    @Inject(
        method = "createSection", at = @At("RETURN")
    )
    private void onCreateSectionAddId(long sectionPos, CallbackInfoReturnable<EntitySection<T>> cir) {
        keyArray.add(sectionPos);
    }

    @Inject(method = "remove", at = @At("RETURN"))
    private void fixIo$remove(final long sectionKey, CallbackInfo ci) {
        keyArray.remove(sectionKey);
    }

    /**
     * @author shiro8613
     * @reason fixSupportNativeSearch e.g. AVX2, AVX-512
     */
    @Overwrite
    public void forEachAccessibleNonEmptySection(final AABB bb, final AbortableIterationConsumer<EntitySection<T>> output) {

        int hitCount = NativeCompute.searchSections(
            keyArray.getAddress(),
            keyArray.getCount(),
            bb.minX, bb.minY, bb.minZ,
            bb.maxX, bb.maxY, bb.maxZ,
            longResultArray
        );

        long baseAddress = longResultArray.getAddress();
        for (int i = 0; i < hitCount; i++) {
            long sectionKey = MemoryUtil.memGetLong(baseAddress + ((long) i * Long.BYTES));

            EntitySection<T> entitySection = this.sections.get(sectionKey);
            if (entitySection != null && !entitySection.isEmpty() && entitySection.getStatus().isAccessible()) {
                if (output.accept(entitySection).shouldAbort()) {
                    return;
                }
            }
        }
    }

}
