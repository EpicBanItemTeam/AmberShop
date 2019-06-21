package io.izzel.ambershop.data;

import org.spongepowered.api.text.Text;

public interface OperateResult {

    boolean success();

    /**
     * @return A AmberShop locale node. Null if success.
     */
    Text reason();

}
