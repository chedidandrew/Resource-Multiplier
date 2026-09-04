package net.fabricmc.fabric.api.gametest.v1;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import net.minecraft.world.level.block.Rotation;

/** Test-source mirror of Fabric's 1.21.10 GameTest annotation for shared tests. */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Documented
public @interface GameTest {
    String environment() default "minecraft:default";
    String structure() default "fabric-gametest-api-v1:empty";
    int maxTicks() default 20;
    int setupTicks() default 0;
    boolean required() default true;
    Rotation rotation() default Rotation.NONE;
    boolean manualOnly() default false;
    int maxAttempts() default 1;
    int requiredSuccesses() default 1;
    boolean skyAccess() default false;
}
