package org.acme.hibernate.orm.envers;

import lombok.Data;
import lombok.NonNull;

import java.util.List;

@Data
public class HistoryList<T> {
    @NonNull
    List<History<T>> history;
}
