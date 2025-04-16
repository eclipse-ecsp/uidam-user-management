package org.eclipse.ecsp.uidam.common.utils;

import org.eclipse.ecsp.uidam.accountmanagement.enums.AccountStatus;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class AccountStatusTest {
    @Test
    void testToString() {
        assertEquals("PENDING", AccountStatus.PENDING.toString());
        assertEquals("SUSPENDED", AccountStatus.SUSPENDED.toString());
        assertEquals("ACTIVE", AccountStatus.ACTIVE.toString());
        assertEquals("BLOCKED", AccountStatus.BLOCKED.toString());
        assertEquals("DELETED", AccountStatus.DELETED.toString());
    }
}