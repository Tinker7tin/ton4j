package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Builder
@Getter
@Setter
@ToString
public class ShardState {
    ShardStateUnsplit left;
    ShardStateUnsplit right;
}