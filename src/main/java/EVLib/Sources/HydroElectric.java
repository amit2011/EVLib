package EVLib.Sources;

import EVLib.Station.ChargingStation;

import java.util.ArrayList;

public class HydroElectric extends EnergySource{
    private final ArrayList<Double> energyAmount;

    public HydroElectric(double[] energyAmoun){
        energyAmount = new ArrayList<>();
        for (double anEnergyAmoun : energyAmoun) energyAmount.add(anEnergyAmoun);
    }

    public HydroElectric()
    {
        energyAmount = new ArrayList<>();
    }

    public double popAmount()
    {
        if ((energyAmount == null)||(energyAmount.size() == 0))
            return 0;
        else
            return energyAmount.remove(0);
        }

    public void insertAmount(double am)
    {
        energyAmount.add(am);
    }
}