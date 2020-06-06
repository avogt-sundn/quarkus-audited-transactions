package org.acme.hibernate.orm.envers;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
@AllArgsConstructor
public class History<T> {
    @NonNull
    T ref;
    Number revision;
    CustomRevisionEntity info;
}
