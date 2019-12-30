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
package pl.sklepmc.plugin.sponge;

import org.slf4j.Logger;
import org.spongepowered.api.Game;
import pl.sklepmc.client.ShopContext;
import pl.sklepmc.client.shop.TransactionInfo;
import pl.sklepmc.plugin.shared.ShopExecutionTask;

import javax.inject.Inject;
import java.util.List;

public class PurchaseExecutionTask extends ShopExecutionTask {

    @Inject
    private Logger logger;

    @Inject
    private Game game;

    @Inject
    private SmSpongePlugin plugin;

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
        return this.game.getServer().getPlayer(name).isPresent();
    }

    @Override
    public void executeCommand(String command) {
        this.game.getScheduler().createTaskBuilder().execute(() -> this.game.getCommandManager().process(this.game.getServer().getConsole(), command));
    }

    @Override
    public void warning(String message) {
        this.logger.warn(message);
    }

    @Override
    public void callPurchaseExecuted(TransactionInfo transaction, List<String> commands) {
        // TODO: zaimplementowac event dla Sponge
    }
}
