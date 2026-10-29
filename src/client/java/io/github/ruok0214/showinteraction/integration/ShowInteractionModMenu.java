package io.github.ruok0214.showinteraction.integration;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import io.github.ruok0214.showinteraction.ShowInteractionClient;

/** Loaded by Mod Menu only, so the base mod remains usable without it. */
public final class ShowInteractionModMenu implements ModMenuApi {
    @Override public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return ShowInteractionClient::createSettingsScreen;
    }
}
