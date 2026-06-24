package org.guanzon.cas.inv.warehouse.services;

import org.guanzon.appdriver.base.GRiderCAS;
import org.guanzon.cas.inv.warehouse.model.Model_Inv_Stock_Request_Detail;
import org.guanzon.cas.inv.warehouse.model.Model_Inv_Stock_Request_Master;
import org.guanzon.cas.inv.warehouse.model.Model_Inventory_Adjustment;
import org.guanzon.cas.inv.warehouse.model.Model_Inventory_Count_Detail;
import org.guanzon.cas.inv.warehouse.model.Model_Inventory_Count_Master;

public class InvWarehouseModels {
    public InvWarehouseModels(GRiderCAS applicationDriver){
        poGRider = applicationDriver;
    }
    
    public Model_Inv_Stock_Request_Master InventoryStockRequestMaster(){
        if (poGRider == null){
            System.err.println("InvWarehouseModels.InventoryStockRequestMaster: Application driver is not set.");
            return null;
        }
        
        if (poInvRequestMaster == null){
            poInvRequestMaster = new Model_Inv_Stock_Request_Master();
            poInvRequestMaster.setApplicationDriver(poGRider);
            poInvRequestMaster.setXML("Model_Inv_Stock_Request_Master");
            poInvRequestMaster.setTableName("Inv_Stock_Request_Master");
            poInvRequestMaster.initialize();
        }

        return poInvRequestMaster;
    }
    
    public Model_Inv_Stock_Request_Detail InventoryStockRequestDetail(){
        if (poGRider == null){
            System.err.println("InvWarehouseModels.InventoryStockRequestDetail: Application driver is not set.");
            return null;
        }
        
        if (poInvRequestDetail == null){
            poInvRequestDetail = new Model_Inv_Stock_Request_Detail();
            poInvRequestDetail.setApplicationDriver(poGRider);
            poInvRequestDetail.setXML("Model_Inv_Stock_Request_Detail");
            poInvRequestDetail.setTableName("Inv_Stock_Request_Detail");
            poInvRequestDetail.initialize();
        }

        return poInvRequestDetail;
    }
    
    public Model_Inventory_Count_Master InventoryCountMaster(){
        if (poGRider == null){
            System.err.println("InvWarehouseModels.InventoryCountMaster: Application driver is not set.");
            return null;
        }
        
        if (poInventoryCountMaster == null){
            poInventoryCountMaster = new Model_Inventory_Count_Master();
            poInventoryCountMaster.setApplicationDriver(poGRider);
            poInventoryCountMaster.setXML("Model_Inventory_Count_Master");
            poInventoryCountMaster.setTableName("Inventory_Count_Master");
            poInventoryCountMaster.initialize();
        }

        return poInventoryCountMaster;
    }
    
    public Model_Inventory_Count_Detail InventoryCountDetail(){
        if (poGRider == null){
            System.err.println("InvWarehouseModels.ModelInventoryCountDetail: Application driver is not set.");
            return null;
        }
        
        if (poInventoryCountDetail == null){
            poInventoryCountDetail = new Model_Inventory_Count_Detail();
            poInventoryCountDetail.setApplicationDriver(poGRider);
            poInventoryCountDetail.setXML("Model_Inventory_Count_Detail");
            poInventoryCountDetail.setTableName("Inventory_Count_Detail");
            poInventoryCountDetail.initialize();
        }

        return poInventoryCountDetail;
    }
    
      public Model_Inventory_Adjustment InventoryAdjustment(){
        if (poGRider == null){
            System.err.println("InvWarehouseModels.ModelInventoryAdjustment: Application driver is not set.");
            return null;
        }
        
        if (poInventoryAdjustment == null){
            poInventoryAdjustment = new Model_Inventory_Adjustment();
            poInventoryAdjustment.setApplicationDriver(poGRider);
            poInventoryAdjustment.setXML("Model_Inventory_Adjustment");
            poInventoryAdjustment.setTableName("Inventory_Adjustment");
            poInventoryAdjustment.initialize();
        }

        return poInventoryAdjustment;
    }
    
    private final GRiderCAS poGRider;
    
    private Model_Inv_Stock_Request_Master poInvRequestMaster;
    private Model_Inv_Stock_Request_Detail poInvRequestDetail;
    private Model_Inventory_Count_Master poInventoryCountMaster;
    private Model_Inventory_Count_Detail poInventoryCountDetail;
    private Model_Inventory_Adjustment poInventoryAdjustment;
}
