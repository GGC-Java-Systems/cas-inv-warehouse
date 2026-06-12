package org.guanzon.cas.inv.warehouse.services;

import org.guanzon.appdriver.base.GRiderCAS;
import org.guanzon.appdriver.base.LogWrapper;
import org.guanzon.cas.inv.warehouse.InventoryCount;
import org.guanzon.cas.inv.warehouse.StockRequest;

public class InvWarehouseControllers {

    public InvWarehouseControllers(GRiderCAS applicationDriver, LogWrapper logWrapper) {
        poGRider = applicationDriver;
        poLogWrapper = logWrapper;
    }

    public StockRequest StockRequest() {
        if (poGRider == null) {
            poLogWrapper.severe("InvWarehouseControllers.StockRequest: Application driver is not set.");
            return null;
        }

        if (poStockRequest != null) {
            return poStockRequest;
        }

        poStockRequest = new StockRequest();
        poStockRequest.setApplicationDriver(poGRider);
        poStockRequest.setBranchCode(poGRider.getBranchCode());
        poStockRequest.setVerifyEntryNo(true);
        poStockRequest.setWithParent(false);
        poStockRequest.setLogWrapper(poLogWrapper);
        return poStockRequest;
    }

    public InventoryCount InventoryCount() {
        if (poGRider == null) {
            poLogWrapper.severe("InvWarehouseControllers.InventoryCount: Application driver is not set.");
            return null;
        }

        if (poInventoryCount != null) {
            return poInventoryCount;
        }

        poInventoryCount = new InventoryCount();
        poInventoryCount.setApplicationDriver(poGRider);
        poInventoryCount.setBranchCode(poGRider.getBranchCode());
        poInventoryCount.setVerifyEntryNo(true);
        poInventoryCount.setWithParent(false);
        poInventoryCount.setLogWrapper(poLogWrapper);
        return poInventoryCount;
    }

    @Override
    protected void finalize() throws Throwable {
        try {
            poStockRequest = null;
            poInventoryCount = null;

            poLogWrapper = null;
            poGRider = null;
        } finally {
            super.finalize();
        }
    }

    private GRiderCAS poGRider;
    private LogWrapper poLogWrapper;

    private StockRequest poStockRequest;
    private InventoryCount poInventoryCount;
}
