package io.izzel.ambershop.trade;

public enum TransactionResult {

    SUCCESS("trade.transaction-results.success", false),
    SOLD_OUT("trade.transaction-results.sold-out", true),
    ECONOMY_ISSUE("trade.transaction-results.economy-issue", true),
    INVENTORY_FULL("trade.transaction-results.inventory-full", true);

    public final String text;
    public final boolean rollback;

    TransactionResult(String text, boolean rollback) {
        this.text = text;
        this.rollback = rollback;
    }


}
