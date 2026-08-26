package gov.takx.api.aspects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

/**
 * Aspect to trap any calls made by plugins to {@link System#exit(int)}.
 *
 * @since 1.45
 */
public aspect TrapSystemExit
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public pointcut sysExit(): !within(gov.takx.module..*) && !within(gov.takx.core..*) && !within(gov.takx.client.platform..*) && call(void java.lang.System.exit(..));

    void around(): sysExit() {
        logger.error("System.exit called!");
        throw new RuntimeException("THOU SHALT NOT CALL SYSTEM.EXIT!!");
    }
}
