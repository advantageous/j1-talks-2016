package io.advantageous.reakt.examples.template;

import com.datastax.driver.core.Row;

/**
 * Created by jasondaniel on 8/25/16.
 */
public interface RowMapper<T> {
    T map(final Row row);
}
