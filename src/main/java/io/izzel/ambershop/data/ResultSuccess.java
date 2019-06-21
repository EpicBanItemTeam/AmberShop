package io.izzel.ambershop.data;

import org.spongepowered.api.text.Text;

public class ResultSuccess implements OperateResult {

    private final Text result;

    public ResultSuccess(Text result) {
        this.result = result;
    }

    @Override
    public boolean success() {
        return true;
    }

    @Override
    public Text reason() {
        return result;
    }

}
