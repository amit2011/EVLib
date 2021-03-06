package evlib.station;

import evlib.ev.Battery;
import evlib.ev.ElectricVehicle;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

public class ChargingEvent
{
    private int id;
    private static final AtomicInteger idGenerator = new AtomicInteger(0);
    private final ChargingStation station;
    private double amountOfEnergy;
    private String kindOfCharging;
    private long waitingTime;
    private ElectricVehicle vehicle;
    private long chargingTime;
    private String condition;
    private Battery givenBattery;
    private Charger charger;
    private double energyToBeReceived;
    private long maxWaitingTime;
    private long timestamp;
    private double cost;
    private ExchangeHandler exchange;
    long accumulatorOfChargingTime = 0;
    private static final List<ChargingEvent> chargingLog = new ArrayList<>();
    private static final List<ChargingEvent> exchangeLog = new ArrayList<>();

    /**
     * Constructs a new ChargingEvent object. It sets the condition of the event to "arrived".
     * @param stat The ChargingStation object the event visited.
     * @param veh The ElectricVehicle of the event.
     * @param amEnerg The amount of energy the events asks.
     * @param kindOfCharg The kind of charging the event demands.
     */
    public ChargingEvent(final ChargingStation stat, final ElectricVehicle veh, final double amEnerg, final String kindOfCharg) {
        this.id = idGenerator.incrementAndGet();
        this.station = stat;
        this.amountOfEnergy = amEnerg;
        this.kindOfCharging = kindOfCharg;
        this.vehicle = veh;
        this.condition = "arrived";
        chargingLog.add(this);
        this.charger = null;
        this.exchange = null;
    }

    /**
     * Constructs a new ChargingEvent object. It sets the condition of the event
     *  to "arrived". Calculates the amount of energy the event will take.
     * @param stat The charging station the event visited.
     * @param veh The electric vehicle of the event.
     * @param kindOfCharg The kind of charging the event demands.
     * @param money The monetary fee the event desires to give for energy.
     */
    public ChargingEvent(final ChargingStation stat, final ElectricVehicle veh, final String kindOfCharg, final double money) {
        this.id = idGenerator.incrementAndGet();
        this.station = stat;
        this.vehicle = veh;
        this.kindOfCharging = kindOfCharg;
        this.condition = "arrived";
        if (money / station.getUnitPrice() <= station.getTotalEnergy())
            this.amountOfEnergy = money / station.getUnitPrice();
        else
            this.amountOfEnergy = station.getTotalEnergy();
        chargingLog.add(this);
        this.charger = null;
        this.exchange = null;
    }

    /**
     * Constructs a new ChargingEvent object. This constructor is for the events
     *  wanting battery swapping. It sets the condition of the event
     *  to "arrived".
     * @param stat The charging station the event visited.
     * @param veh The electric vehicle of the event.
     */
    public ChargingEvent(final ChargingStation stat, final ElectricVehicle veh) {
        this.id = idGenerator.incrementAndGet();
        this.station = stat;
        this.kindOfCharging = "exchange";
        this.vehicle = veh;
        this.chargingTime = station.getTimeOfExchange();
        this.condition = "arrived";
        exchangeLog.add(this);
        this.charger = null;
        this.exchange = null;
    }

    /**
     * Executes the pre-processing phase. Checks for any Charger or exchange slot and assignes to it if any.
     * It calculates the energy to be given to the ElectricVehicle and calculates the charging time.
     * If there is not any empty Charger or exchange slot the ChargingEvent is inserted
     * in the respectively waiting list, if the waiting time is less than the set waiting time of the Driver.
     **/
    public void preProcessing() {
        if ((kindOfCharging.equals("fast") && station.FAST_CHARGERS == 0) ||
        (kindOfCharging.equals("slow") && station.SLOW_CHARGERS == 0) ||
        (kindOfCharging.equals("exchange") && station.getExchangeHandlers().length == 0)) {
            setCondition("nonExecutable");
            return;
        }
        if (vehicle.getBattery().getActive()) {
            if ((condition.equals("arrived")) || (condition.equals("wait"))) {
                if (!"exchange".equalsIgnoreCase(kindOfCharging)) {
                    station.assignCharger(this);
                    if (charger != null) {
                        if (amountOfEnergy < station.getTotalEnergy()) {
                            if (amountOfEnergy <= (vehicle.getBattery().getCapacity() - vehicle.getBattery().getRemAmount()))
                                energyToBeReceived = amountOfEnergy;
                            else
                                energyToBeReceived = vehicle.getBattery().getCapacity() - vehicle.getBattery().getRemAmount();
                        } else {
                            if (station.getTotalEnergy() <= (vehicle.getBattery().getCapacity() - vehicle.getBattery().getRemAmount()))
                                energyToBeReceived = station.getTotalEnergy();
                            else
                                energyToBeReceived = vehicle.getBattery().getCapacity() - vehicle.getBattery().getRemAmount();
                        }
                        if (energyToBeReceived == 0) {
                            condition = "nonExecutable";
                            charger.setChargingEvent(null);
                            charger = null;
                            return;
                        }
                        if ("fast".equalsIgnoreCase(kindOfCharging))
                            chargingTime = ((long) (energyToBeReceived * 3600000 / station.getChargingRateFast()));
                        else
                            chargingTime = ((long) (energyToBeReceived * 3600000 / station.getChargingRateSlow()));
                        this.cost = station.calculatePrice(this);
                        setCondition("ready");
                        double sdf;
                        sdf = energyToBeReceived;
                        for (String s : station.getSources()) {
                            if (sdf < station.getMap().get(s)) {
                                double ert = station.getMap().get(s) - sdf;
                                station.setSpecificAmount(s, ert);
                                break;
                            } else {
                                sdf -= station.getMap().get(s);
                                station.setSpecificAmount(s, 0);
                            }
                        }
                    }
                    else
                        if (!condition.equals("wait")) {
                            maxWaitingTime = calWaitingTime();
                            if ((maxWaitingTime < waitingTime) && (maxWaitingTime > -1)) {
                                if (!condition.equals("wait"))
                                    station.updateQueue(this);
                                setCondition("wait");
                            } else
                                setCondition("nonExecutable");
                        }
                } else {
                    station.assignExchangeHandler(this);
                    if (givenBattery == null) {
                        station.assignBattery(this);
                        if (givenBattery == null) {
                            setCondition("nonExecutable");
                            return;
                        }
                    }
                    if (exchange != null) {
                        chargingTime = station.getTimeOfExchange();
                        this.cost = station.getExchangePrice();
                        setCondition("ready");
                    }
                    else
                        if (!condition.equals("wait"))
                        {
                            maxWaitingTime = calWaitingTime();
                            if (maxWaitingTime < waitingTime && maxWaitingTime > -1) {
                                setCondition("wait");
                                station.updateQueue(this);
                            } else
                                setCondition("nonExecutable");
                        }
                }
            }
        }
        else
            setCondition("nonExecutable");
    }

    /**
     * It starts the execution of the ChargingEvent. Increases the number of chargings of the Battery by one.
     * The pre-condition for the execution is the condition of the event to be "ready".
     */
    public void execution()
    {
        if (condition.equals("ready"))
            if (!kindOfCharging.equalsIgnoreCase("exchange")) {
                setCondition("charging");
                vehicle.getBattery().addCharging();
                charger.startCharger();
            }
            else {
                setCondition("swapping");
                exchange.startExchangeHandler();
            }
    }

    /**
     * @return The ElectricVehicle of the event.
     */
    public ElectricVehicle getElectricVehicle() {
        return vehicle;
    }

    /**
     * Sets an ElectricVehicle to the ChargingEvent.
     * @param veh The ElectricVehicle to be set.
     */
    public void setElectricVehicle(final ElectricVehicle veh) { this.vehicle = veh; }

    /**
     * @return The kind of charging of the ChargingEvent.
     */
    public String getKindOfCharging()
    {
        return kindOfCharging;
    }

    /**
     * @return The ChargingStation the ChargingEvent wants to be executed.
     */
    public ChargingStation getStation()
    {
        return station;
    }

    /**
     * Sets the energy to be received by the vehicle.
     * @param energy The energy to be set.
     */
    public void setEnergyToBeReceived(final double energy) {
        this.energyToBeReceived = energy;
    }

    /**
     * @return The energy to be received by ElectricVehicle.
     */
    public double getEnergyToBeReceived()
    {
        return energyToBeReceived;
    }

    /**
     * @return The amount of energy the ElectricVehicle asks for.
     */
    public double getAmountOfEnergy() {
        return amountOfEnergy;
    }

    /**
     * Sets the maximum time a ChargingEvent has to wait in the waiting list. The time has to be in milliseconds
     * @param max The time to be set in milliseconds.
     */
    public void setMaxWaitingTime(final long max) { this.maxWaitingTime = max; }

    /**
     * Sets the waiting time the Driver can wait in milliseconds.
     * @param w The waiting time in milliseconds.
     */
    public void setWaitingTime(final long w) {
        this.waitingTime = w;
    }

    /**
     * Sets the amount of energy the ChargingEvent demands.
     * @param energy The energy to be set.
     */
    public void setAmountOfEnergy(final double energy) {
        this.amountOfEnergy = energy;
    }

    /**
     * @return The condition of the ChargingEvent.
     */
    public String getCondition() {
        return condition;
    }

    /**
     * @return The waiting time of the ChargingEvent in milliseconds.
     */
    public long getWaitingTime()
    {
        return waitingTime;
    }

    /**
     * @return The remaining charging time of the ChargingEvent in milliseconds.
     */
    public long getRemainingChargingTime()
    {
        long diff = System.currentTimeMillis() - timestamp;
        long remainingChargingTime;
        if ((chargingTime - diff >= 0) && (condition.equals("charging") || condition.equals("swapping")))
            remainingChargingTime = chargingTime - diff;
        else
            return 0;
        return remainingChargingTime;
    }

    /**
     * Sets the condition of the ChargingEvent.
     * @param cond The condition to be set.
     */
    public void setCondition(final String cond) {
        this.condition = cond;
    }

    /**
     * @return The maximum time the vehicle should wait in milliseconds.
     */
    public long getMaxWaitingTime()
    {
        return maxWaitingTime;
    }

    /**
     * @return The charging time of the ChargingEvent in milliseconds.
     */
    public long getChargingTime() {
        return chargingTime;
    }

    /**
     * Sets the charging time of the ChargingEvent in milliseconds. It also starts counting the reamining time of the charging.
     *
     * @param time The charging time in milliseconds.
     */
    public void setChargingTime(final long time) {
        timestamp = System.currentTimeMillis();
        this.chargingTime = time;
    }

    /**
     * Calculates the amount of time a Driver has to wait until his ElectricVehicle
     * will be charged. This calculation happens in case an ElectricVehicle should
     * be added in the WaitingList.
     * @return The waiting time in milliseconds.
     */
    private long calWaitingTime()
    {
        if (station.getChargers().length == 0)
            return -1;
        long[] counter1 = new long[station.getChargers().length];
        long[] counter2 = new long[station.getExchangeHandlers().length];
        long min = -1;
        int index = -1;
        if (!"exchange".equalsIgnoreCase(getKindOfCharging()))
            for (int i = 0; i < station.getChargers ().length; i++) {
                if (Objects.equals(getKindOfCharging(), station.getChargers()[i].getKindOfCharging())) {
                    if (station.getChargers()[i].getChargingEvent() != null) {
                        if (min == -1) {
                            min = station.getChargers()[i].getChargingEvent().getRemainingChargingTime();
                            index = i;
                        }
                        long diff = station.getChargers()[i].getChargingEvent().getRemainingChargingTime();
                        if (min > diff) {
                            min = diff;
                            index = i;
                        }
                        counter1[i] = diff;
                    } else
                        return 0;
                }
            }
        else
            for (int i = 0; i < station.getExchangeHandlers().length; i++) {
                if (station.getExchangeHandlers()[i].getChargingEvent() != null) {
                    if (min == -1) {
                        min = station.getExchangeHandlers()[i].getChargingEvent().getRemainingChargingTime();
                        index = i;
                    }
                    long diff = station.getExchangeHandlers()[i].getChargingEvent().getRemainingChargingTime();
                    if (min > diff) {
                        min = diff;
                        index = i;
                    }
                    counter2[i] = diff;
                } else
                    return 0;
            }
        ChargingEvent e;
        if ("slow".equalsIgnoreCase(getKindOfCharging()))
        {
            WaitList o = station.getSlow();
            for (int i = 0;i < o.getSize(); i++)
            {
                e = (ChargingEvent) o.get(i);
                counter1[index] = counter1[index] + ((long) (e.getAmountOfEnergy() * 3600000 / station.getChargingRateSlow()));
                for (int j = 0; j < station.getChargers().length; j++)
                    if ((counter1[j] < counter1[index]) && (counter1[j] != 0))
                        index = j;
            }
            return counter1[index];
        }
        if ("fast".equalsIgnoreCase(getKindOfCharging()))
        {
            WaitList o = station.getFast();
            for (int i = 0; i < o.getSize(); i++)
            {
                e = (ChargingEvent) o.get(i);
                counter1[index] = counter1[index] + ((long) (e.getAmountOfEnergy() * 3600000 / station.getChargingRateFast()));
                for (int j = 0; j < station.getChargers().length; j++)
                    if ((counter1[j] < counter1[index]) && (counter1[j] != 0))
                        index = j;
            }
            return counter1[index];
        }
        if ("exchange".equalsIgnoreCase(getKindOfCharging()))
        {
            for (int i = 0; i < station.getExchange().getSize(); i++)
            {
                counter2[index] = counter2[index] + station.getTimeOfExchange();
                for (int j = 0; j < station.getChargers().length; j++)
                    if ((counter2[j] < counter2[index]) && (counter2[j] != 0))
                        index = j;
            }
            return counter2[index];
        }
        return 0;
    }

    /**
     * @return The id of the ChargingEvent.
     */
    public int getId()
    {
        return id;
    }

    /**
     * Sets the id for the ChargingEvent.
     *
     * @param d The id to be set.
     */
    public void setId(final int d) {
        this.id = d;
    }

    /**
     * @return The cost of the ChargingEvent.
     */
    public double getCost() {
        return this.cost;
    }

    /**
     * Sets the cost for the ChargingEvent.
     * @param c The cost to be set.
     */
    public void setCost(final double c)
    {
        this.cost = c;
    }

    /**
     * Returns the list with all created charging events.
     * @return The list with all created charging events.
     */
    public static List<ChargingEvent> getChargingLog() {
        return chargingLog;
    }

    /**
     * Returns the list with all created battery exchange events.
     * @return The list with all created battery exchange events.
     */
    public static List<ChargingEvent> getExchangeLog() {
        return exchangeLog;
    }

    /**
     * Sets a charger to the event for charging.
     * @param ch The charger to be assigned.
     */
    void setCharger(Charger ch) {
        this.charger = ch;
    }

    /**
     * Sets an exchange handler to the event for the battery exchange function.
     * @param exch The exchange handler to be assigned.
     */
    void setExchange(ExchangeHandler exch) {
        this.exchange = exch;
    }

    /**
     * Sets a battery to the event for the battery exchange function.
     * @param bat The battery to be assigned.
     */
    void setBattery(Battery bat) {
        this.givenBattery = bat;
    }

    Battery getGivenBattery() {
        return givenBattery;
    }
}