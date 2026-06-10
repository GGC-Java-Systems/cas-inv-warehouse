package org.guanzon.cas.inv.warehouse.status;

import java.util.Arrays;
import java.util.List;

public class InventoryCountStatus {

    public static final String OPEN = "0";
    public static final String CONFIRMED = "1";
    public static final String POSTED = "2";
    public static final String CANCELLED = "3";
    public static final String VOID = "4";
    public static final List<String> STATUS = Arrays.asList(
            "OPEN",
            "CONFIRMED",
            "POSTED", 
            "CANCELLED", 
            "VOID"
    );
}
