package org.eclipse.ecsp.uidam.usermanagement.utilities;

import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertEquals;

class UserAccountRoleAssociationValidatorTest {

    @Test
    void getAccountIdFromPath() {
        assertEquals("1", UserAccountRoleAssociationValidator.getAccountIdFromPath("/account/1/roleName"));
        assertEquals("2", UserAccountRoleAssociationValidator.getAccountIdFromPath("/account/2"));
    }
}
