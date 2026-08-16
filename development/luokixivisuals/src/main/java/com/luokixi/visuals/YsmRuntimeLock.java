package com.luokixi.visuals;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** Forces YSM's native player model switching off without a compile-time YSM dependency. */
public final class YsmRuntimeLock {
    private static final String CONFIG_CLASS = "com.elfmcys.yesstevemodel.config.ServerConfig";
    private static final String FIELD_NAME = "CAN_SWITCH_MODEL";

    private YsmRuntimeLock() {}

    public static LockResult enforce() {
        try {
            Class<?> cls = Class.forName(CONFIG_CLASS, false, YsmRuntimeLock.class.getClassLoader());
            Field field = cls.getField(FIELD_NAME);
            Object value = field.get(null);
            if (value == null) return new LockResult(false, "YSM CanSwitchModel is not initialized yet");

            Method get = value.getClass().getMethod("get");
            Method set = null;
            for (Method method : value.getClass().getMethods()) {
                if (method.getName().equals("set") && method.getParameterCount() == 1) {
                    set = method;
                    break;
                }
            }
            if (set == null) return new LockResult(false, "YSM config set(...) method not found");
            set.invoke(value, Boolean.FALSE);
            Object after = get.invoke(value);
            boolean locked = after instanceof Boolean && !((Boolean) after);
            return new LockResult(locked, locked ? "YSM CanSwitchModel=false" : "YSM refused CanSwitchModel=false");
        } catch (ClassNotFoundException missing) {
            return new LockResult(false, "YSM is not installed");
        } catch (ReflectiveOperationException | LinkageError error) {
            return new LockResult(false, "YSM lock failed: " + error.getClass().getSimpleName());
        }
    }

    public record LockResult(boolean locked, String message) {}
}
