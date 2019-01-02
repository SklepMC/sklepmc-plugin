/*
 * SklepMC Plugin
 * Copyright (C) 2019 SklepMC
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package pl.daffit.sklepmc.plugin;

import com.google.inject.Inject;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.spongepowered.api.Game;
import org.spongepowered.api.config.DefaultConfig;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.plugin.Plugin;
import pl.daffit.sklepmc.api.ApiContext;

import java.io.File;
import java.io.IOException;

@Plugin(id = "sklepmc", name = "SklepMC", version = "1.0-SNAPSHOT", description = "SklepMC Sponge Plugin")
public class SmSpongePlugin {

    private ApiContext apiContext;
    private int serverId;
    private ConfigurationNode config;

    @Inject
    private Logger logger;

    @Inject
    private Game game;

    @Inject
    @DefaultConfig(sharedRoot = true)
    private File configFile;

    @Inject
    @DefaultConfig(sharedRoot = true)
    private ConfigurationLoader<CommentedConfigurationNode> configManager;

    public ApiContext getApiContext() {
        return this.apiContext;
    }

    public int getServerId() {
        return this.serverId;
    }

    public ConfigurationNode getConfig() {
        return this.config;
    }

    @Listener
    public void onServerStart(GameStartedServerEvent event) {

        // save default configuration if config.yml does not exists
        this.saveDefaultConfig();

        // validate configuration and create ApiContext
        String key = this.config.getNode("key").getString("key");
        this.serverId = this.config.getNode("server-id").getInt();

        if (key == null) {
            this.logger.error("Nie znaleziono poprawnie ustawionej wartosci 'key' w config.yml," +
                    " nalezy ja ustawic i zrestatowac serwer.");
            return;
        }

        if (this.serverId == 0) {
            this.logger.error("Nie znaleziono poprawnie ustawionej wartosci 'server-id' w config.yml," +
                    " nalezy ja ustawic i zrestatowac serwer.");
            return;
        }

        String[] keyParts = StringUtils.split(key, '-');
        if (keyParts.length != 2) {
            this.logger.error("Wprowadzona wartosc 'key' w config.yml jest w nieprawidlowym formacie," +
                    " nalezy ja skorygowac i zrestatowac serwer.");
            return;
        }

        String shopId = keyParts[0];
        String secret = keyParts[1];
        this.apiContext = new ApiContext(shopId, secret);

        // custom api url
        String apiUrl = this.config.getNode("api-url").getString();
        if (apiUrl != null) {
            this.apiContext.setMainUrl(apiUrl);
        }

        // start task for checking transactions in EXECUTION state
        // check every 30 seconds, one second lasts approximately 20 ticks
        long checkEvery = 30 * 20;
        this.game.getScheduler().createTaskBuilder().async().intervalTicks(checkEvery).execute(new PurchaseExecutionTask());
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void saveDefaultConfig() {
        try {
            if (this.configFile.exists()) {
                this.config = this.configManager.load();
                return;
            }

            this.configFile.createNewFile();
            this.config = this.configManager.load();
            this.config.getNode("key").setValue("");
            this.config.getNode("server-id").setValue(0);
            this.configManager.save(this.config);
        }
        catch (IOException exception) {
            this.logger.error("Nie udalo sie stworzyc pliku konfiguracji. Prosze zweryfikowac uprawnienia.");
        }
    }
}
