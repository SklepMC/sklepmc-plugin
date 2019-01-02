package pl.daffit.sklepmc.plugin;

import org.apache.commons.lang.StringUtils;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import pl.daffit.sklepmc.api.ApiContext;

import java.util.logging.Level;

public class SmBukkitPlugin extends JavaPlugin {

    private ApiContext apiContext;
    private int serverId;

    public ApiContext getApiContext() {
        return this.apiContext;
    }

    public int getServerId() {
        return this.serverId;
    }

    @Override
    public void onEnable() {

        // save default configuration if config.yml does not exists
        this.saveDefaultConfig();

        // validate configuration and create ApiContext
        FileConfiguration config = this.getConfig();
        String key = config.getString("key");
        this.serverId = config.getInt("server-id");

        if (key == null) {
            this.getLogger().log(Level.SEVERE, "Nie znaleziono poprawnie ustawionej wartosci 'key' w config.yml," +
                    " nalezy ja ustawic i zrestatowac serwer.");
            this.getServer().getPluginManager().disablePlugin(this);
            return;
        }

        if (this.serverId == 0) {
            this.getLogger().log(Level.SEVERE, "Nie znaleziono poprawnie ustawionej wartosci 'server-id' w config.yml," +
                    " nalezy ja ustawic i zrestatowac serwer.");
            this.getServer().getPluginManager().disablePlugin(this);
            return;
        }

        String[] keyParts = StringUtils.split(key, '-');
        if (keyParts.length != 2) {
            this.getLogger().log(Level.SEVERE, "Wprowadzona wartosc 'key' w config.yml jest w nieprawidlowym formacie," +
                    " nalezy ja skorygowac i zrestatowac serwer.");
            this.getServer().getPluginManager().disablePlugin(this);
            return;
        }

        String shopId = keyParts[0];
        String secret = keyParts[1];
        this.apiContext = new ApiContext(shopId, secret);

        // custom api url
        String apiUrl = this.getConfig().getString("api-url");
        if (apiUrl != null) {
            this.apiContext.setMainUrl(apiUrl);
        }

        // start task for checking transactions in EXECUTION state
        // check every 30 seconds, one second lasts approximately 20 ticks
        long checkEvery = 30 * 20;
        this.getServer().getScheduler().runTaskTimerAsynchronously(this, new PurchaseExecutionTask(this), checkEvery, checkEvery);
    }
}
