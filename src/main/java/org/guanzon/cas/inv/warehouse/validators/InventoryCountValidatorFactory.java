package org.guanzon.cas.inv.warehouse.validators;

import org.guanzon.appdriver.iface.GValidator;

public class InventoryCountValidatorFactory {

    public static GValidator make(String industryId) {
        switch (industryId) {
            case "00": //Mobile Phone
                return new InventoryCount_MP();
            case "01": //Motorcycle
                return new InventoryCount_MC();
            case "02": //Vehicle
            case "05":
            case "06":
                return new InventoryCount_Vehicle();
            case "03": //Monarch
                return new InventoryCount_Monarch();
            case "04": //Los Pedritos
                return new InventoryCount_LP();
            case "07": //Main Office / General
            case "08":
            case "09":
            case "10":
                return new InventoryCount_General();

            case "":
                return new InventoryCount_Appliance();
            default:
                return new InventoryCount_General();
        }
    }

}
