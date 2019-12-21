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
package pl.sklepmc.plugin.shared;

import pl.sklepmc.client.ApiError;
import pl.sklepmc.client.ApiException;
import pl.sklepmc.client.ShopContext;
import pl.sklepmc.client.shop.ExecutionCommandInfo;
import pl.sklepmc.client.shop.ExecutionInfo;
import pl.sklepmc.client.shop.ExecutionTaskInfo;
import pl.sklepmc.client.shop.TransactionInfo;

import java.util.List;

public abstract class ShopExecutionTask implements Runnable {

    public abstract ShopContext getShopContext();

    public abstract int getServerId();

    public abstract boolean isPlayerOnline(String name);

    public abstract void executeCommand(String command);

    public abstract void warning(String message);

    @Override
    public void run() {

        ShopContext shopContext = this.getShopContext();
        int serverId = this.getServerId();
        ExecutionInfo executionInfo;

        try {
            executionInfo = ExecutionInfo.get(shopContext, serverId);
        } catch (ApiException exception) {
            ApiError apiError = exception.getApiError();
            this.warning("Nie udalo sie sprawdzic transakcji oczekujacych wykonania: " + apiError.getType() + ", " + apiError.getMessage());
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
                if (requireOnline && !this.isPlayerOnline(command.getTarget())) {
                    continue task_execution;
                }

                String commandText = command.getText();
                this.executeCommand(commandText);
            }

            // change transaction status to COMPLETED
            boolean updated;
            try {
                updated = TransactionInfo.updateStatus(shopContext, transactionId, TransactionInfo.TransactionStatus.COMPLETED.name());
            } catch (ApiException exception) {
                ApiError apiError = exception.getApiError();
                this.warning("Nie udalo sie zmienic statusu transakcji "
                        + transactionId + ", przerwano wykonywanie: " + apiError.getType() + ", " + apiError.getMessage());
                continue;
            }

            // handle failure just for information
            // should not really be a case
            if (!updated) {
                this.warning("Nie udalo sie zmienic statusu transakcji " + transactionId + ".");
            }
        }
    }
}
