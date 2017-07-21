package Sources;

/**
 *
 * @author Sotiris Karapostolakis
 */
import Station.ChargingStation;
import EV.*;
import java.util.ArrayList;

public class NonRenewable extends EnergySource{
    private ArrayList<Float> energyAmount;

    public NonRenewable(int id, ChargingStation station, float[] energyAmoun)
    {
        super(id, station);
        energyAmount = new ArrayList<Float>();
        for (int i=0; i<energyAmoun.length; i++)
            energyAmount.add(energyAmoun[i]);
    }

    public NonRenewable(int id, ChargingStation station)
    {
        super(id, station);
        energyAmount = new ArrayList<Float>();
    }

    public float popAmount()
    {
        if ((energyAmount == null)||(energyAmount.size() == 0))
            return 0;
        else
            return energyAmount.get(0);
    }

    public void insertAmount(float am)
    {
        energyAmount.add(am);
    }
}