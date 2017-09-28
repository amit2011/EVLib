package EVLib.Events;

import EVLib.EV.ElectricVehicle;
import EVLib.Station.ChargingStation;
import EVLib.Station.DisCharger;
import EVLib.Station.WaitList;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class DisChargingEvent
{
    private int id;
    private static final AtomicInteger idGenerator = new AtomicInteger(0);
    private ElectricVehicle vehicle;
    private final ChargingStation station;
    private final String chargingStationName;
    private double amountOfEnergy;
    private long disChargingTime;
    private long remainingDisChargingTime;
    private String condition;
    private int disChargerId;
    private long waitingTime;
    private long maxWaitingTime;
    private long timestamp;
    private double profit;
    public static final List<DisChargingEvent> dischargingLog = new ArrayList<>();

    public DisChargingEvent(ChargingStation station, ElectricVehicle vehicle, double amEnerg)
    {
        this.id = idGenerator.incrementAndGet();
        this.amountOfEnergy = amEnerg;
        this.station = station;
        this.vehicle = vehicle;
        this.disChargerId = -1;
        this.condition = "arrived";
        this.chargingStationName = station.getName();
    }

    /**
     * Sets the time the ElectricVehicle can wait, in case it is inserted in the queue.
     * @param time The time to wait.
     */
    public void setWaitingTime(long time)
    {
        this.waitingTime = time;
    }

    /**
     * @return The time the ElectricVehicle can wait.
     */
    public long getWaitingTime()
    {
        return waitingTime;
    }

    /**
     * @return The ElectricVehicle of the ChargingEvent.
     */
    public ElectricVehicle getElectricVehicle()
    {
        return vehicle;
    }

    /**
     * Sets the vehicle of the DisChargingEvent.
     * @param vehicle The vehicle to be set.
     */
    public void setElectricVehicle(ElectricVehicle vehicle) { this.vehicle = vehicle; }

    /**
     * Executes the pre-processing phase. Checks for any DisCharger
     * and calculates the discharging time. If there is not any empty DisCharger
     * the DisChargingEvent object is inserted in the WaitingList.
     */
    public void preProcessing()
    {
        if (getElectricVehicle().getBattery().getActive()) {
            if ((condition.equals("arrived")) || (condition.equals("wait"))) {
                int qwe = station.checkDisChargers();
                if ((qwe != -1) && (qwe != -2)) {
                    disChargerId = qwe;
                    DisCharger dsc = station.searchDischarger(disChargerId);
                    dsc.setDisChargingEvent(this);
                    disChargingTime = (long) (amountOfEnergy / station.getDisChargingRatio());
                    setCondition("ready");
                    profit = amountOfEnergy * station.getDisUnitPrice();
                } else if (qwe == -2)
                    setCondition("nonExecutable");
                else
                    if(!condition.equals("wait")) {
                        maxWaitingTime = calDisWaitingTime();
                        if (maxWaitingTime < waitingTime) {
                            if (!condition.equals("wait"))
                                station.updateDisChargingQueue(this);
                            setCondition("wait");
                        } else
                            setCondition("nonExecutable");
                    }
            }
        }
        else
            setCondition("nonExecutable");
    }

    /**
     * It starts the execution of the DisChargingEvent.
     * If the DisChargingEvent is in the WaitingList it does not do anything.
     */
    public void execution()
    {
        if(condition.equals("ready"))
        {
            setCondition("discharging");
            station.searchDischarger(disChargerId).executeDisChargingEvent();
        }
    }

    /**
     * Sets a value to the condition of the DisChargingEvent.
     * @param condition The value of the condition.
     */
    public void setCondition(String condition)
    {
        this.condition = condition;
    }

    /**
     * @return The ChargingStation object.
     */
    public ChargingStation getStation()
    {
        return station;
    }

    /**
     * @return The name of the ChargingStation.
     */
    public String getChargingStationName() {
        return chargingStationName;
    }

    /**
     * @return The amount of energy to be given.
     */
    public double getAmountOfEnergy()
    {
        return amountOfEnergy;
    }

    /**
     * Sets the amount of energy the DisChargingEvent will give.
     * @param energy The energy to be set.
     */
    public void setAmountOfEnergy(double energy) { this.amountOfEnergy = energy; }

    /**
     * @return The condition of the DisChargingEvent.
     */
    public String getCondition()
    {
        return condition;
    }

    /**
     * @return The remaining discharging time.
     */
    public long getRemainingDisChargingTime()
    {
        long diff = System.currentTimeMillis() - timestamp;
        if ((disChargingTime - diff >= 0)&&(!condition.equals("arrived")))
            this.remainingDisChargingTime = disChargingTime - diff;
        else
            return 0;
        return remainingDisChargingTime;
    }

    /**
     * Sets the time of the discharging.
     * @param disChargingTime The time of discharging.
     */
    public void setDisChargingTime(long disChargingTime){
        timestamp = System.currentTimeMillis();
        this.disChargingTime = disChargingTime;
    }

    /**
     * Sets the id of DisCharger that is going to be discharged.
     * @param id The id of the DisCharger in which the DisChargingEvent is attached.
     */
    public void setDisChargerId(int id)
    {
        disChargerId = id;
    }

    /**
     * @return The waiting time.
     */
    public long getMaxWaitingTime() { return maxWaitingTime; }

    /**
     * Sets the maximum time the DisChargingEvent should wait to be discharged.
     * @param maxWaitingTime The time to be set.
     */
    public void setMaxWaitingTime(long maxWaitingTime) { this.maxWaitingTime = maxWaitingTime; }

    /**
     * @return The discharging time of the DisChargingEvent
     */
    public long getDisChargingTime()
    {
        return disChargingTime;
    }

    /**
     * @return The time the ElectricVehicle has to wait.
     */
    private long calDisWaitingTime()
    {
        long[] counter1 = new long[station.getDisChargers().length];
        long min = 1000000000;
        int index = 1000000000;
        for (int i = 0; i<station.getDisChargers().length; i++)
        {
            long diff = station.getDisChargers()[i].getDisChargingEvent().getRemainingDisChargingTime();
            if (min > diff)if (min > diff) {
                min = diff;
                index = i;
            }
            counter1[i] = diff;
        }
        WaitList o = station.getDischarging();
        DisChargingEvent e;
        for(int i = 0; i < o.getSize() ;i++)
        {
            e = (DisChargingEvent) o.get(i);
            counter1[index] = counter1[index] + ((long) (e.getAmountOfEnergy()/station.getDisChargingRatio()));
            for(int j=0; j<station.getDisChargers().length; j++)
                if ((counter1[j]<counter1[index])&&(counter1[j]!=0))
                    index = j;
        }
        return counter1[index];
    }

    /**
     * @return The id of this DisChargingEvent.
     */
    public int getId()
    {
       return id;
    }

    /**
     * @return The profit of this DisChargingEvent.
     */
    public double getProfit()
    {
        return profit;
    }

    /**
     * Sets the profit for this DisChargingEvent.
     * @param profit The profit to be set.
     */
    public void setProfit(double profit) { this.profit = profit; }

    /**
     * Sets the id for this DisChargingEvent.
     * @param id The id to be set.
     */
    public void setId(int id) { this.id = id; }
}