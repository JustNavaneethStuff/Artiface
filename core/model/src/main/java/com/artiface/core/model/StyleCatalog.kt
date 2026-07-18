package com.artiface.core.model

object StyleCatalog {
    val ComicBurst = CaricatureStyle(
        id = StyleId("comic_burst"),
        name = "Comic Burst",
        description = "Ink-splashed drama with eyebrows doing overtime.",
        previewResource = "style_comic_burst",
    )
    val RoyalAbsurdity = CaricatureStyle(
        id = StyleId("royal_absurdity"),
        name = "Royal Absurdity",
        description = "Velvet pomp for people who snack like monarchs.",
        previewResource = "style_royal_absurdity",
    )
    val NeonMischief = CaricatureStyle(
        id = StyleId("neon_mischief"),
        name = "Neon Mischief",
        description = "Glow-in-the-dark chaos with excellent posture.",
        previewResource = "style_neon_mischief",
    )
    val StorybookChaos = CaricatureStyle(
        id = StyleId("storybook_chaos"),
        name = "Storybook Chaos",
        description = "Fairytale energy, plot optional.",
        previewResource = "style_storybook_chaos",
    )
    val RetroPoster = CaricatureStyle(
        id = StyleId("retro_poster"),
        name = "Retro Poster",
        description = "Vintage propaganda for questionable decisions.",
        previewResource = "style_retro_poster",
    )
    val SurpriseMe = CaricatureStyle(
        id = StyleId("surprise_me"),
        name = "Surprise Me",
        description = "Let ARTIFACE roll the stylistic dice.",
        previewResource = "style_surprise_me",
    )

    val all: List<CaricatureStyle> = listOf(
        ComicBurst,
        RoyalAbsurdity,
        NeonMischief,
        StorybookChaos,
        RetroPoster,
        SurpriseMe,
    )

    val selectable: List<CaricatureStyle> = all.filter { it.id != SurpriseMe.id }

    fun require(id: StyleId): CaricatureStyle =
        all.firstOrNull { it.id == id } ?: error("Unknown style: ${id.value}")

    fun resolveSelection(id: StyleId): CaricatureStyle {
        if (id == SurpriseMe.id) return selectable.random()
        return require(id)
    }
}
