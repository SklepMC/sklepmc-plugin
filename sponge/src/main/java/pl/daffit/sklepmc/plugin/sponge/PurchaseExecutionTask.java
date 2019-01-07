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
package pl.daffit.sklepmc.plugin.sponge;

import org.slf4j.Logger;
import org.spongepowered.api.Game;
import org.spongepowered.api.entity.living.player.Player;
import pl.daffit.sklepmc.api.ApiContext;
import pl.daffit.sklepmc.api.ApiError;
import pl.daffit.sklepmc.api.ApiException;
import pl.daffit.sklepmc.api.shop.ExecutionCommandInfo;
import pl.daffit.sklepmc.api.shop.ExecutionInfo;
import pl.daffit.sklepmc.api.shop.ExecutionTaskInfo;
import pl.daffit.sklepmc.api.shop.TransactionInfo;

import javax.inject.Inject;
import java.util.List;
import java.util.Optional;

public class PurchaseExecutionTask implements Runnable {

    @Inject
    private Logger logger;

    @Inject
    private Game game;

    @Inject
    private SmSpongePlugin plugin;

    @Override
    public void run() {

        ApiContext apiContext = this.plugin.getApiContext();
        int serverId = this.plugin.getServerId();
        ExecutionInfo executionInfo;

        try {
            executionInfo = ExecutionInfo.get(apiContext, serverId);
        } catch (ApiException exception) {
            ApiError apiError = exception.getApiError();
            this.logger.warn("Nie udalo sie sprawdzic transakcji oczekujacych wykonania: "
                    + apiError.getType() + ", " + apiError.getMessage());
            return;
        }

        List<ExecutionTaskInfo> executionTasks = executionInfo.getExecutionTasks();
        task_execution:
        for (ExecutionTaskInfo executionTask : executionTasks) {

            List<ExecutionCommandInfo> commands = executionTask.getCommands();
            String transactionId = executionTask.getTransactionId();
            boolean requireOnline = executionTask.isRequireOnline();

            // run commands
            for (ExecutionCommandInfo command : commands) {

                // execution requires target to be online, skipping
                if (requireOnline) {
                    Optional<Player> player = this.game.getServer().getPlayer(command.getTarget());
                    if (!player.isPresent()) {
                        continue task_execution;
                    }
                }

                String commandText = command.getText();
                this.dispatchCommand(commandText);
            }

            // change transaction status to COMPLETED
            boolean updated;
            try {
                updated = TransactionInfo.updateStatus(apiContext, transactionId, TransactionInfo.TransactionStatus.COMPLETED.name());
            } catch (ApiException exception) {
                ApiError apiError = exception.getApiError();
                this.logger.warn("Nie udalo sie zmienic statusu transakcji "
                        + transactionId + ", przerwano wykonywanie: " + apiError.getType() + ", " + apiError.getMessage());
                continue;
            }

            // handle failure just for information
            // should not really be a case
            if (!updated) {
                this.logger.warn("Nie udalo sie zmienic statusu transakcji " + transactionId + ".");
            }
        }
    }

    // as task is run asynchronously we need to run execution of command synchronized
    // command dispatching is not thread-safe and can cause server crash
    private void dispatchCommand(String command) {
        this.game.getScheduler().createTaskBuilder().execute(() -> this.game.getCommandManager().process(this.game.getServer().getConsole(), command));
    }
}
