package org.acme.hibernate.envers.historized.impl;

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
    @NonNull
    Number revision;
    CustomRevisionEntity info;
}
