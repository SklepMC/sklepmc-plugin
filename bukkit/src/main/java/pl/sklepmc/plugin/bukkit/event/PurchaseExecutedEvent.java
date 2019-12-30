package pl.sklepmc.plugin.bukkit.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import pl.sklepmc.client.shop.TransactionInfo;

import java.util.List;

public class PurchaseExecutedEvent extends Event {

    private static final HandlerList HANDLER_LIST = new HandlerList();
    private final TransactionInfo transaction;
    private final List<String> commands;

    public PurchaseExecutedEvent(TransactionInfo transaction, List<String> commands) {
        super(true);
        this.transaction = transaction;
        this.commands = commands;
    }

    public TransactionInfo getTransaction() {
        return this.transaction;
    }

    public List<String> getCommands() {
        return this.commands;
    }
    @Override
    public HandlerList getHandlers() {
        return HANDLER_LIST;
    }

    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }
}
