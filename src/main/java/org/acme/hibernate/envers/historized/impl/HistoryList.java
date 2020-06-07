package org.acme.hibernate.envers.historized.impl;

import lombok.Data;
import lombok.NonNull;

import java.util.List;

@Data
public class HistoryList<T> {
    @NonNull
    List<History<T>> history;
}
