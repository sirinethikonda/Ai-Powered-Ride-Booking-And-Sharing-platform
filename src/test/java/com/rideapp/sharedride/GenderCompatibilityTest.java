package com.rideapp.sharedride;

import com.rideapp.user.entity.Gender;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GenderCompatibilityTest {

    @Test
    @DisplayName("Male with Male passenger matching is strictly permitted")
    void testMaleWithMaleEligible() {
        assertTrue(Gender.MALE.isStrictlyCompatibleWith(Gender.MALE));
    }

    @Test
    @DisplayName("Female with Female passenger matching is strictly permitted")
    void testFemaleWithFemaleEligible() {
        assertTrue(Gender.FEMALE.isStrictlyCompatibleWith(Gender.FEMALE));
    }

    @Test
    @DisplayName("Male with Female passenger matching is strictly FORBIDDEN")
    void testMaleWithFemaleIneligible() {
        assertFalse(Gender.MALE.isStrictlyCompatibleWith(Gender.FEMALE));
        assertFalse(Gender.FEMALE.isStrictlyCompatibleWith(Gender.MALE));
    }

    @Test
    @DisplayName("Null gender matching safely returns false")
    void testNullGenderHandled() {
        assertFalse(Gender.MALE.isStrictlyCompatibleWith(null));
        assertFalse(Gender.FEMALE.isStrictlyCompatibleWith(null));
    }
}
