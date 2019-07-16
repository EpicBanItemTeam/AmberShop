package io.izzel.ambershop.util;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@ToString
public class OperationResult {

    @Getter private final String path;
    @Getter private final Object[] args;
    @Getter private final boolean fail;

    public static OperationResult of(String path, Object... args) {
        return new OperationResult(path, args, false);
    }

    public static OperationResult fail(String path, Object... args) {
        return new OperationResult(path, args, true);
    }

}
