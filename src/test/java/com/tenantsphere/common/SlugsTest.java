package com.tenantsphere.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SlugsTest {

    @Test
    void aTitleWithNothingLeftForASlugTakesTheFallback() {
        String hindi = new String(Character.toChars(0x0928)) + new String(Character.toChars(0x092E))
                + new String(Character.toChars(0x0938)) + new String(Character.toChars(0x094D))
                + new String(Character.toChars(0x0924)) + new String(Character.toChars(0x0947));
        assertThat(Slugs.of(hindi)).isEmpty();
        assertThat(Slugs.of(hindi, "post")).isEqualTo("post");
        assertThat(Slugs.of("!!!", "tag")).isEqualTo("tag");
        assertThat(Slugs.of("Café opening hours", "post")).isEqualTo("cafe-opening-hours");
        assertThat(Slugs.of("Water " + hindi + " tank", "post")).isEqualTo("water-tank");
    }
}
