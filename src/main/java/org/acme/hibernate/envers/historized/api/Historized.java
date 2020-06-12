package org.acme.hibernate.envers.historized.api;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.acme.hibernate.envers.historized.impl.History;
import org.acme.hibernate.envers.historized.impl.HistoryList;

import java.time.LocalDateTime;

/**
 * Container for the rest response to a GET {id}
 *
 * @param <T>
 */
@Data
@RequiredArgsConstructor
@NoArgsConstructor
@AllArgsConstructor
public class Historized<T extends Historizable<I>, I> {
    @NonNull
    History<T, I> active;
    History<T, I> edited;
    HistoryList<T, I> timeline;
    final LocalDateTime fetchDate = LocalDateTime.now();
}