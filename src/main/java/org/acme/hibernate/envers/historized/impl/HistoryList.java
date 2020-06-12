package org.acme.hibernate.envers.historized.impl;

import lombok.Data;
import lombok.NonNull;
import org.acme.hibernate.envers.historized.api.Historizable;

import java.util.List;

@Data
public class HistoryList<T extends Historizable<I>, I> {
    @NonNull
    List<History<T, I>> history;
}
