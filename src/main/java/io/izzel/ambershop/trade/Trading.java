package io.izzel.ambershop.trade;

import java.util.function.Supplier;

public interface Trading extends Supplier<TransactionResult> {
    /**
     * @return result and text.
     */
    TransactionResult performTransaction();

    @Override
    default TransactionResult get() {
        return performTransaction();
    }
}
