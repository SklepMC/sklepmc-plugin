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

import org.bukkit.Server;
import org.bukkit.entity.Player;
import pl.daffit.sklepmc.api.ApiContext;
import pl.daffit.sklepmc.api.ApiError;
import pl.daffit.sklepmc.api.ApiException;
import pl.daffit.sklepmc.api.shop.ExecutionCommandInfo;
import pl.daffit.sklepmc.api.shop.ExecutionInfo;
import pl.daffit.sklepmc.api.shop.ExecutionTaskInfo;
import pl.daffit.sklepmc.api.shop.TransactionInfo;

import java.util.List;

public class PurchaseExecutionTask implements Runnable {

    private final SmBukkitPlugin plugin;
    private final Server server;

    public PurchaseExecutionTask(SmBukkitPlugin plugin) {
        this.plugin = plugin;
        this.server = plugin.getServer();
    }

    @Override
    public void run() {

        ApiContext apiContext = this.plugin.getApiContext();
        int serverId = this.plugin.getServerId();
        ExecutionInfo executionInfo;

        try {
            executionInfo = ExecutionInfo.get(apiContext, serverId);
        } catch (ApiException exception) {
            ApiError apiError = exception.getApiError();
            this.plugin.getLogger().warning("Nie udalo sie sprawdzic transakcji oczekujacych wykonania: "
                    + apiError.getType() + ", " + apiError.getMessage());
            return;
        }

        List<ExecutionTaskInfo> executionTasks = executionInfo.getExecutionTasks();
        for (ExecutionTaskInfo executionTask : executionTasks) {

            List<ExecutionCommandInfo> commands = executionTask.getCommands();
            String transactionId = executionTask.getTransactionId();
            boolean requireOnline = executionTask.isRequireOnline();

            // change transaction status to COMPLETED
            boolean updated;
            try {
                updated = TransactionInfo.updateStatus(apiContext, transactionId, TransactionInfo.TransactionStatus.COMPLETED.name());
            } catch (ApiException exception) {
                ApiError apiError = exception.getApiError();
                this.plugin.getLogger().warning("Nie udalo sie zmienic statusu transakcji "
                        + transactionId + ", przerwano wykonywanie: " + apiError.getType() + ", " + apiError.getMessage());
                continue;
            }

            // handle failure to prevent multiple executions
            if (!updated) {
                this.plugin.getLogger().warning("Nie udalo sie zmienic statusu transakcji " + transactionId + ", przerwano wykonywanie.");
                continue;
            }

            // run commands
            for (ExecutionCommandInfo command : commands) {

                // execution requires target to be online, skipping
                if (requireOnline) {
                    Player playerExact = this.plugin.getServer().getPlayerExact(command.getTarget());
                    if (playerExact == null) {
                        continue;
                    }
                }

                String commandText = command.getText();
                this.dispatchCommand(commandText);
            }
        }
    }

    // as task is run asynchronously we need to run execution of command synchronized
    // command dispatching is not thread-safe and can cause server crash
    private void dispatchCommand(String command) {
        this.server.getScheduler().runTask(this.plugin, () -> this.server.dispatchCommand(this.server.getConsoleSender(), command));
    }
}
