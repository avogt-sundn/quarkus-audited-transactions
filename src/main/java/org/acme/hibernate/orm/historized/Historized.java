package org.acme.hibernate.orm.historized;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.acme.hibernate.orm.envers.History;
import org.acme.hibernate.orm.envers.HistoryList;

import java.time.LocalDateTime;

@Data
@RequiredArgsConstructor
@NoArgsConstructor
@AllArgsConstructor
public class Historized<T> {
    @NonNull
    History<T> active;
    History<T> edited;
    HistoryList<T> timeline;
    final LocalDateTime fetchDate = LocalDateTime.now();
}