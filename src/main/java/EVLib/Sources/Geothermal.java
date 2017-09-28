package EVLib.Sources;

import java.util.ArrayList;

public class Geothermal extends EnergySource
{
    private final ArrayList<Double> energyAmount;

    public Geothermal(double[] energyAmoun)
        {
            energyAmount = new ArrayList<>();
            for (double anEnergyAmoun : energyAmoun)
                energyAmount.add(anEnergyAmoun);
        }

    public Geothermal() { energyAmount = new ArrayList<>(); }

    public double popAmount() {
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