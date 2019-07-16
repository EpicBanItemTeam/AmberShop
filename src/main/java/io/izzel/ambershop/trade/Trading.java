package io.izzel.ambershop.trade;

import io.izzel.ambershop.util.OperationResult;

import java.util.function.Supplier;

public interface Trading extends Supplier<OperationResult> {

    /**
     * @return result and text.
     */
    OperationResult performTransaction();

    @Override
    default OperationResult get() {
        return performTransaction();
    }
}
