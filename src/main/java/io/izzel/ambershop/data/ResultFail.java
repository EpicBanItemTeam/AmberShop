package io.izzel.ambershop.data;

import org.spongepowered.api.text.Text;

public class ResultFail implements OperateResult {

    private final Text result;

    public ResultFail(Text text) {
        this.result = text;
    }

    @Override
    public boolean success() {
        return false;
    }

    @Override
    public Text reason() {
        return result;
    }

}
