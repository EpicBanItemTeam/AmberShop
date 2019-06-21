package io.izzel.ambershop.data;

import com.google.inject.ImplementedBy;

import java.sql.Connection;
import java.util.List;
import java.util.concurrent.Future;

@ImplementedBy(SqlStorage.class)
public interface Storage {

    void init();

    Connection connection();

    Future<List<ShopRecord>> allRecords();

}
