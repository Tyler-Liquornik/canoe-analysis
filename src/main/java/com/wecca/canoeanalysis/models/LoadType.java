package com.wecca.canoeanalysis.models;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter @AllArgsConstructor
public enum LoadType {
    POINT("Point Load"),
    UNIFORMLY_DISTRIBUTED("Uniformly Distributed Load"),
    DISCRETE_DISTRIBUTION("Discrete Distribution");

    public final String value;
}
