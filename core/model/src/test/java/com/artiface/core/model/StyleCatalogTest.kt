package com.artiface.core.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class StyleCatalogTest {

    @Test
    fun surprise_me_resolves_to_concrete_style() {
        val resolved = StyleCatalog.resolveSelection(StyleCatalog.SurpriseMe.id)
        assertThat(resolved.id).isNotEqualTo(StyleCatalog.SurpriseMe.id)
        assertThat(StyleCatalog.selectable).contains(resolved)
    }

    @Test
    fun catalog_contains_expected_styles() {
        assertThat(StyleCatalog.all.map { it.id.value }).containsAtLeast(
            "comic_burst",
            "royal_absurdity",
            "neon_mischief",
            "storybook_chaos",
            "retro_poster",
            "surprise_me",
        )
    }
}
