package org.acme.hibernate.envers.historized.api;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.acme.hibernate.envers.historized.impl.History;
import org.acme.hibernate.envers.historized.impl.HistoryList;

import java.time.LocalDateTime;

/**
 * Container for the rest response to a GET {id}
 *
 * @param <T>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Historized<T extends Historizable<I>, I> {
    History<T, I> active;
    History<T, I> edited;
    HistoryList<T, I> timeline;
    final LocalDateTime fetchDate = LocalDateTime.now();
}