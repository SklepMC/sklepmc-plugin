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
package pl.sklepmc.plugin.bukkit;

import org.bukkit.Server;
import pl.sklepmc.client.shop.TransactionInfo;
import pl.sklepmc.plugin.bukkit.event.PurchaseExecutedEvent;
import pl.sklepmc.plugin.shared.ShopExecutionTask;
import pl.sklepmc.client.ShopContext;

import java.util.List;

public class PurchaseExecutionTask extends ShopExecutionTask {

    private final SmBukkitPlugin plugin;
    private final Server server;

    public PurchaseExecutionTask(SmBukkitPlugin plugin) {
        this.plugin = plugin;
        this.server = plugin.getServer();
    }

    @Override
    public ShopContext getShopContext() {
        return this.plugin.getShopContext();
    }

    @Override
    public int getServerId() {
        return this.plugin.getServerId();
    }

    @Override
    public boolean isPlayerOnline(String name) {
        return this.server.getPlayerExact(name) != null;
    }

    @Override
    public void executeCommand(String command) {
        // as task is run asynchronously we need to run execution of command synchronized
        // command dispatching is not thread-safe and can cause server crash
        this.server.getScheduler().runTask(this.plugin, () -> this.server.dispatchCommand(this.server.getConsoleSender(), command));
    }

    @Override
    public void warning(String message) {
        this.server.getLogger().warning(message);
    }

    @Override
    public void callPurchaseExecuted(TransactionInfo transaction, List<String> commands) {
        this.server.getPluginManager().callEvent(new PurchaseExecutedEvent(transaction, commands));
    }
}
