package org.acme.hibernate.envers.historized.impl;

import org.acme.hibernate.envers.historized.api.Historizable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
@AllArgsConstructor
public class History<T extends Historizable<I>, I> {
    @NonNull
    T ref;
    @NonNull
    Number revision;
    CustomRevisionEntity info;
}
