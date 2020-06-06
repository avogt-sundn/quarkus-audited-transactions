package org.acme.hibernate.orm.historized;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.acme.hibernate.orm.envers.History;
import org.acme.hibernate.orm.envers.HistoryList;

import java.time.LocalDateTime;
import java.util.List;

@Data
@RequiredArgsConstructor
@NoArgsConstructor
public class Historized<T> {
    @NonNull
    History<T> active;
    History<T>  edited;
    HistoryList<T> timeline;
    final LocalDateTime fetchDate = LocalDateTime.now();
}