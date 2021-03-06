package evlib.station;

import evlib.ev.Battery;
import evlib.ev.Driver;
import evlib.ev.ElectricVehicle;
import evlib.sources.*;

import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

public class ChargingStation {
    private int id;
    private String name;
    private final WaitList<ChargingEvent> fast;
    private final WaitList<ChargingEvent> slow;
    private final WaitList<DisChargingEvent> discharging;
    private final WaitList<ChargingEvent> exchange;
    private double chargingRateFast;
    private double chargingRateSlow;
    private double disChargingRate;
    private final ArrayList<Charger> chargers;
    private final ArrayList<EnergySource> n;
    private ArrayList<DisCharger> dischargers;
    private final ArrayList<Battery> batteries;
    private final ArrayList<ExchangeHandler> exchangeHandlers;
    private final ArrayList<ParkingSlot> parkingSlots;
    private final HashMap<String, Double> amounts;
    private final ArrayList<String> sources;
    private double unitPrice;
    private double disUnitPrice;
    private double inductivePrice;
    private double exchangePrice;
    private boolean automaticQueueHandling;
    private int updateSpace;
    private long timeOfExchange;
    private double inductiveChargingRate;
    private static final AtomicInteger idGenerator = new AtomicInteger(0);
    private long timestamp;
    private PricingPolicy policy;
    private boolean automaticUpdate;
    private final Statistics statistics = new Statistics();
    private Timer timer;
    private boolean deamon;
    private final Lock lock1 = new ReentrantLock();
    private final Lock lock2 = new ReentrantLock();
    private final Lock lock3 = new ReentrantLock();
    private final Lock lock4 = new ReentrantLock();
    private final Lock lock5 = new ReentrantLock();
    private final Lock lock6 = new ReentrantLock();
    private final Lock lock7 = new ReentrantLock();
    private final Lock lock8 = new ReentrantLock();
    public int FAST_CHARGERS;
    public int SLOW_CHARGERS;
    final ArrayList<ChargingEvent> events = new ArrayList<>();
    final ArrayList<Integer> numberOfChargers = new ArrayList<>();
    boolean execEvents;

    private class CheckUpdate extends TimerTask {
        public void run() {
            Thread.currentThread().setName("UpdateStorageTimer" + id);
            updateStorage();
        }
    }

    /**
     * Creates a new ChargingStation instance. It sets the handling of the queue to automatic, as well. The fast charging rate,
     * slow charging rate, discharging rate and inductive charging rate are set to 0.01 Watt/millisecond. The battery duration is set to 1000 milliseconds.
     * Regarding the energy sources, creates all the desired energy objects, assigning the energy packages to them.
     * Finally, initializes all the waiting lists.
     * @param nam The name of the Charging Station.
     * @param kinds An array with the initial kind of chargers. The value "fast" means a charger charging with fast rate,
     * while "slow" signifies a Charger charging with slow rate.
     * @param source An array with the desired EnergySource objects. The values need to be exactly as the names
     * of the sources package, in order to be created an object.
     * @param energyAmounts A two-dimension array with all the initial energy packages for the station's source.
     * The first dimension must be equal to the length of the kinds array parameter. The second dimension shows
     * the number of energy packages we provide.
     */
    public ChargingStation(final String nam, final String[] kinds, final String[] source, final double[][] energyAmounts) {
        this.amounts = new HashMap<>();
        this.id = idGenerator.incrementAndGet();
        this.name = nam;
        this.automaticQueueHandling = true;
        this.fast = new WaitList<>();
        this.slow = new WaitList<>();
        this.exchange = new WaitList<>();
        this.discharging = new WaitList<>();
        this.chargers = new ArrayList<>();
        this.dischargers = new ArrayList<>();
        this.exchangeHandlers = new ArrayList<>();
        this.parkingSlots = new ArrayList<>();
        this.n = new ArrayList<>();
        this.sources = new ArrayList<>();
        this.batteries = new ArrayList<>();
        this.chargingRateFast = 0.01;
        this.chargingRateSlow = 0.01;
        this.disChargingRate = 0.01;
        this.inductiveChargingRate = 0.01;
        this.timeOfExchange = 1000;
        Collections.addAll(sources, source);
        this.sources.add("Discharging");
        setSpecificAmount("Discharging", 0.0);
        for (int i = 0; i < source.length; i++) {
            if (source[i].equalsIgnoreCase("Solar")) {
                n.add(i, new Solar(energyAmounts[i]));
                setSpecificAmount("Solar", 0.0);

            } else if (source[i].equalsIgnoreCase("Wind")) {
                n.add(i, new Wind(energyAmounts[i]));
                setSpecificAmount("Wind", 0.0);

            } else if (source[i].equalsIgnoreCase("Geothermal")) {
                n.add(i, new Geothermal(energyAmounts[i]));
                setSpecificAmount("Geothermal", 0.0);

            } else if (source[i].equalsIgnoreCase("Wave")) {
                n.add(i, new Wave(energyAmounts[i]));
                setSpecificAmount("Wave", 0.0);

            } else if (source[i].equalsIgnoreCase("Hydroelectric")) {
                n.add(i, new Hydroelectric(energyAmounts[i]));
                setSpecificAmount("Hydroelectric", 0.0);

            } else if (source[i].equalsIgnoreCase("Nonrenewable")) {
                n.add(i, new Nonrenewable(energyAmounts[i]));
                setSpecificAmount("Nonrenewable", 0.0);

            }
        }
        for (String kind : kinds) {
            if (kind.equalsIgnoreCase("fast")) {
                chargers.add(new Charger(this, "fast"));
                ++FAST_CHARGERS;
            } else if (kind.equalsIgnoreCase("slow")) {
                chargers.add(new Charger(this, "slow"));
                ++SLOW_CHARGERS;
            } else if (kind.equals("exchange")) {
                exchangeHandlers.add(new ExchangeHandler(this));
            } else if (kind.equals("park")) {
                parkingSlots.add(new ParkingSlot(this));
            }
        }
    }

    /**
     * Creates a new ChargingStation instance. It also sets the handling of the queue to automatic. The fast charging rate,
     * slow charging rate, discharging rate and inductive charging rate are set to 0.01 Watt/millisecond. The battery
     * duration is set to 1000 milliseconds. Regarding the energy sources, creates all the desired
     * energy objects, assigning the energy packages to them. Finally, initializes all the waiting lists.
     * @param nam The name of the ChargingStation object.
     * @param kinds An array with the initial kind of chargers. The value "fast" means a charger for fast charging,
     * while "slow" signifies a Charger for slow charging.
     * @param source An array with the desired EnergySource objects. The values need to be exactly as the names
     * of the sources package, in order to be created an object.
     */
    public ChargingStation(final String nam, final String[] kinds, final String[] source) {
        this.amounts = new HashMap<>();
        this.id = idGenerator.incrementAndGet();
        this.name = nam;
        this.fast = new WaitList<>();
        this.slow = new WaitList<>();
        this.exchange = new WaitList<>();
        this.discharging = new WaitList<>();
        this.automaticQueueHandling = true;
        this.chargers = new ArrayList<>();
        this.dischargers = new ArrayList<>();
        this.batteries = new ArrayList<>();
        this.exchangeHandlers = new ArrayList<>();
        this.parkingSlots = new ArrayList<>();
        this.n = new ArrayList<>();
        this.sources = new ArrayList<>();
        Collections.addAll(sources, source);
        this.sources.add("Discharging");
        this.chargingRateFast = 0.01;
        this.chargingRateSlow = 0.01;
        this.disChargingRate = 0.01;
        this.inductiveChargingRate = 0.01;
        this.timeOfExchange = 1000;
        setSpecificAmount("Discharging", 0.0);
        for (int i = 0; i < source.length; i++) {
            switch (source[i]) {
                case "Solar":
                    n.add(i, new Solar());
                    setSpecificAmount("Solar", 0.0);
                    break;
                case "Wind":
                    n.add(i, new Wind());
                    setSpecificAmount("Wind", 0.0);
                    break;
                case "Geothermal":
                    n.add(i, new Geothermal());
                    setSpecificAmount("Geothermal", 0.0);
                    break;
                case "Wave":
                    n.add(i, new Wave());
                    setSpecificAmount("Wave", 0.0);
                    break;
                case "Hydroelectric":
                    n.add(i, new Hydroelectric());
                    setSpecificAmount("Hydroelectric", 0.0);
                    break;
                case "Nonrenewable":
                    n.add(i, new Nonrenewable());
                    setSpecificAmount("Nonrenewable", 0.0);
                    break;
                default:
                    break;
            }
        }
        for (String kind : kinds) {
            switch (kind) {
                case "fast":
                case "slow":
                    chargers.add(new Charger(this, kind));
                    break;
                case "exchange":
                    exchangeHandlers.add(new ExchangeHandler(this));
                    break;
                case "park":
                    parkingSlots.add(new ParkingSlot(this));
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * Creates a new ChargingStation instance. It also sets the handling of the queue to automatic. The fast charging rate,
     * slow charging rate, discharging rate and inductive charging rate are set to 0.01 Watt/millisecond. The battery duration
     * is set to 1000 milliseconds. Regarding the energy sources, creates all the desired
     * energy objects, assigning the energy packages to them. Finally, initializes all the waiting lists.
     * @param nam The name of the ChargingStation.
     */
    public ChargingStation(final String nam) {
        this.id = idGenerator.incrementAndGet();
        this.name = nam;
        this.fast = new WaitList<>();
        this.slow = new WaitList<>();
        this.exchange = new WaitList<>();
        this.discharging = new WaitList<>();
        this.parkingSlots = new ArrayList<>();
        this.amounts = new HashMap<>();
        this.chargers = new ArrayList<>();
        this.dischargers = new ArrayList<>();
        this.batteries = new ArrayList<>();
        this.exchangeHandlers = new ArrayList<>();
        this.dischargers = new ArrayList<>();
        this.n = new ArrayList<>();
        this.sources = new ArrayList<>();
        this.sources.add("Discharging");
        setSpecificAmount("Discharging", 0.0);
        this.automaticQueueHandling = true;
        this.chargingRateFast = 0.01;
        this.chargingRateSlow = 0.01;
        this.disChargingRate = 0.01;
        this.inductiveChargingRate = 0.01;
        this.timeOfExchange = 1000;
    }

    /**
     * Sets a name to the ChargingStation.
     * @param nam The name to be set.
     */
    public void setName(final String nam)
    {
        this.name = nam;
    }

    /**
     * @return The id of the ChargingStation.
     */
    public int getId() {
        return this.id;
    }

    /**
     * Sets the id for the ChargingStation.
     * @param d The id to be set.
     */
    public void setId(final int d) {
        this.id = d;
    }

    /**
     * Adds a ChargingEvent to the corresponding waiting list.
     * @param event The ChargingEvent to be added.
     */
    public void updateQueue(final ChargingEvent event) {
        lock2.lock();
        try {
            switch (event.getKindOfCharging()) {
                case "exchange":
                    exchange.add(event);
                    break;
                case "fast":
                    fast.add(event);
                    break;
                case "slow":
                    slow.add(event);
                    break;
                default:
                    break;
            }
        } finally {
            lock2.unlock();
        }
    }

    /**
     * Adds a DisChargingEvent in the waiting list.
     * @param event The DisChargingEvent to be added.
     */
    public void updateDisChargingQueue(final DisChargingEvent event) {
        lock3.lock();
        try {
            discharging.add(event);
        } finally {
            lock3.unlock();
        }
    }

    /**
     * @return The WaitngList for fast charging.
     */
    public WaitList getFast() {
        return fast;
    }

    /**
     * @return The WaitingList for slow charging.
     */
    public WaitList getSlow() {
        return slow;
    }

    /**
     * @return The WaitingList for battery exchange.
     */
    public WaitList getExchange() {
        return exchange;
    }

    /**
     * @return The WaitingList for discharging.
     */
    public WaitList getDischarging() {
        return discharging;
    }

    /**
     * Looks for an empty Charger. If there is one, the event is assigned to it.
     * @param event The event that looks for a Charger.
     * @return The Charger that was assigned, or null if not any available Charger found.
     */
    public Charger assignCharger(final ChargingEvent event) {
        lock4.lock();
        int i = 0;
        Charger ch = null;
        boolean flag = false;
        try {
            if (chargers.size() != 0)
                while (!flag && i < chargers.size()) {
                    if (event.getKindOfCharging().equalsIgnoreCase(chargers.get(i).getKindOfCharging()))
                        if (chargers.get(i).getChargingEvent() == null) {
                            chargers.get(i).setChargingEvent(event);
                            flag = true;
                            ch = chargers.get(i);
                            event.setCharger(chargers.get(i));
                        }
                    ++i;
                }
        } finally {
            lock4.unlock();
        }
        return ch;
    }

    /**
     * Looks for an empty DisCharger. If there is one, the event is assigned to it.
     * @param event The event that looks for a DisCharger.
     * @return The DisCharger that was assigned, or null if not any available DisCharger found.
     */
    public DisCharger assignDisCharger(final DisChargingEvent event) {
        lock5.lock();
        int i = 0;
        DisCharger dsch = null;
        boolean flag = false;
        try {
            if (dischargers.size() != 0)
                while (!flag && i < dischargers.size()) {
                    if (dischargers.get(i).getDisChargingEvent() == null) {
                        dischargers.get(i).setDisChargingEvent(event);
                        flag = true;
                        dsch = dischargers.get(i);
                        event.setDisCharger(dischargers.get(i));
                    }
                    ++i;
                }
        } finally {
            lock5.unlock();
        }
        return dsch;
    }

    /**
     * Looks for an empty ExchangeHandler. If there is one, the event is assigned to it.
     * @param event The event that looks for an ExchangeHandler.
     * @return The ExchangeHandler that was assigned, or null if not any available ExchangeHandler found.
     */
    public ExchangeHandler assignExchangeHandler(final ChargingEvent event) {
        lock6.lock();
        int i = 0;
        ExchangeHandler ch = null;
        boolean flag = false;
        try {
            if (exchangeHandlers.size() != 0)
                while (!flag && i < exchangeHandlers.size()) {
                    if (exchangeHandlers.get(i).getChargingEvent() == null) {
                        exchangeHandlers.get(i).setChargingEvent(event);
                        flag = true;
                        ch = exchangeHandlers.get(i);
                        event.setExchange(exchangeHandlers.get(i));
                    }
                    ++i;
                }
        } finally {
            lock6.unlock();
        }
        return ch;
    }

    /**
     * Looks for any empty ParkingSlot. If there is one, the event is assigned to it.
     * @param event The event that looks for a ParkingSlot.
     * @return The ParkingSLot that was assigned, or null if not any available ParkingSlot found.
     */
    public ParkingSlot assignParkingSlot(final ParkingEvent event) {
        lock7.lock();
        int i = 0;
        ParkingSlot ch = null;
        boolean flag = false;
        try {
            if (parkingSlots.size() != 0)
                while (!flag && i < parkingSlots.size()) {
                    if (parkingSlots.get(i).getParkingEvent() == null) {
                        if (event.getAmountOfEnergy() > 0) {
                            if (parkingSlots.get(i).getInSwitch()) {
                                parkingSlots.get(i).setParkingEvent(event);
                                flag = true;
                                ch = parkingSlots.get(i);
                                event.setParkingSlot(parkingSlots.get(i));
                            }
                        } else {
                            parkingSlots.get(i).setParkingEvent(event);
                            flag = true;
                            ch = parkingSlots.get(i);
                            event.setParkingSlot(parkingSlots.get(i));
                        }
                    }
                    ++i;
                }
        } finally {
            lock7.unlock();
        }
        return ch;
    }

    /**
     * Looks for any available Battery. If there is one and the remaining amount is greater than 0,
     * the battery is returned.
     * @param event The event that wants the battery.
     * @return The assigned Battery, or null if no Battery found.
     */
    public Battery assignBattery(final ChargingEvent event) {
        lock8.lock();
        int i = 0;
        Battery bat = null;
        boolean flag = false;
        try {
            if (batteries.size() != 0)
                while (!flag && i < batteries.size()) {
                    if (batteries.get(i).getRemAmount() > 0) {
                        flag = true;
                        bat = batteries.get(i);
                        event.setBattery(batteries.remove(i));
                    }
                    ++i;
                }
        } finally {
            lock8.unlock();
        }
        return bat;
    }

    /**
     * @return Returns all the ExchangeHandler.
     */
    public ExchangeHandler[] getExchangeHandlers() {
        ExchangeHandler[] g = new ExchangeHandler[exchangeHandlers.size()];
        for (int i = 0; i < exchangeHandlers.size(); i++)
            g[i] = exchangeHandlers.get(i);
        return g;
    }

    /**
     * Adds a Charger to the ChargingStation.
     * @param charger The Charger to be added.
     */
    public void addCharger(final Charger charger) {
        chargers.add(charger);
        if (charger.getKindOfCharging().equalsIgnoreCase("fast"))
            ++FAST_CHARGERS;
        else if (charger.getKindOfCharging().equalsIgnoreCase("slow"))
            ++SLOW_CHARGERS;
    }

    /**
     * Adds a Discharger to the ChargingStation.
     * @param discharger The DisCharger to be added.
     */
    public void addDisCharger(final DisCharger discharger) {
        dischargers.add(discharger);
    }

    /**
     * @return Returns all the ParkingSlot.
     */
    public ParkingSlot[] getParkingSlots() {
        ParkingSlot[] g = new ParkingSlot[parkingSlots.size()];
        for (int i = 0; i < parkingSlots.size(); i++)
            g[i] = parkingSlots.get(i);
        return g;
    }

    /**
     * Inserts a ParkingSlot in the ChargingStation
     * @param slot The ParkingSlot to be added.
     */
    public void addParkingSlot(final ParkingSlot slot)
    {
        parkingSlots.add(slot);
    }

    /**
     * Adds a new EnergySource to the ChargingStation.
     * @param source The EnergySource to be added.
     */
    public void addEnergySource(final EnergySource source) {
        if ((source instanceof Solar) && (!sources.contains("Solar"))) {
            sources.add("Solar");
            setSpecificAmount("Solar", 0.0);
        } else if ((source instanceof Wave) && (!sources.contains("Wave"))) {
            sources.add("Wave");
            setSpecificAmount("Wave", 0.0);
        } else if ((source instanceof Wind) && (!sources.contains("Wind"))) {
            sources.add("Wind");
            setSpecificAmount("Wind", 0.0);
        } else if ((source instanceof Hydroelectric) && (!sources.contains("Hydroelectric"))) {
            sources.add("Hydroelectric");
            setSpecificAmount("Hydroelectric", 0.0);
        } else if ((source instanceof Geothermal) && (!sources.contains("Geothermal"))) {
            sources.add("Geothermal");
            setSpecificAmount("Geothermal", 0.0);
        } else if ((source instanceof Nonrenewable) && (!sources.contains("Nonrenewable"))) {
            sources.add("Nonrenewable");
            setSpecificAmount("Nonrenewable", 0.0);
        }
        else
            return;
        n.add(source);
    }

    /**
     * Deletes an EnergySource from the ChargingStation.
     * @param source The EnergySource to be removed.
     */
    public void deleteEnergySource(final EnergySource source) {
        n.remove(source);
        if (source instanceof Solar) {
            amounts.remove("Solar");
            sources.remove("Solar");
        } else if (source instanceof Wave) {
            amounts.remove("Wave");
            sources.remove("Wave");
        } else if (source instanceof Wind) {
            amounts.remove("Wind");
            sources.remove("Wind");
        } else if (source instanceof Hydroelectric) {
            amounts.remove("Hydroelectric");
            sources.remove("Hydroelectric");
        } else if (source instanceof Nonrenewable) {
            amounts.remove("Nonrenewable");
            sources.remove("Nonrenewable");
        } else if (source instanceof Geothermal) {
            amounts.remove("Geothermal");
            sources.remove("Geothermal");
        }
    }

    /**
     * Removes a specific Charger.
     * @param charger The Charger to be removed.
     */
    public void deleteCharger(final Charger charger)
    {

        chargers.remove(charger);
        if (charger.getKindOfCharging().equalsIgnoreCase("fast"))
            FAST_CHARGERS--;
        else
            SLOW_CHARGERS--;
    }

    /**
     * Removes a specific DisCharger.
     * @param disCharger The DisCharger to be removed.
     */
    public void deleteDisCharger(final DisCharger disCharger)
    {
        dischargers.remove(disCharger);
    }

    /**
     * Removes a specific ExchangeHandler.
     * @param exchangeHandler The ExchangeHandler to be removed.
     */
    public void deleteExchangeHandler(final ExchangeHandler exchangeHandler)
    {
        exchangeHandlers.remove(exchangeHandler);
    }

    /**
     * Removes a specific ParkingSlot.
     * @param parkingSlot The ParkingSLot to be removed.
     */
    public void deleteParkingSlot(final ParkingSlot parkingSlot)
    {
        parkingSlots.remove(parkingSlot);
    }

    /**
     * Inserts a new ExchangeHandler in the ChargingStation.
     * @param handler The ExchangeHandler to be added.
     */
    public void addExchangeHandler(final ExchangeHandler handler) {
        exchangeHandlers.add(handler);
    }

    /**
     * Adds a Battery to the ChargingStation for the battery exchange function.
     * @param battery The Battery is going to be added.
     */
    public void joinBattery(final Battery battery) {
        batteries.add(battery);
    }

    /**
     * Sorts the energies sources according to an order defined by the user.
     * @param energies It is a String array that defines the energies' order.
     */
    public void customEnergySorting(final String[] energies) {
        sources.clear();
        for (int i = 0; i < energies.length; i++)
            sources.add(i, energies[i]);
    }

    /**
     * @return An array with the Battery for the battery exchange function.
     */
    public Battery[] getBatteries() {
        Battery[] g = new Battery[batteries.size()];
        batteries.forEach(bat -> g[batteries.indexOf(bat)] = bat);
        return g;
    }

    /**
     * Deletes a Battery from the batteries for the battery exchange function.
     * @param battery The battery to be removed.
     * @return True if the deletion was successfull, false if it was unsuccessfull.
     */
    public boolean deleteBattery(final Battery battery) {
        return batteries.remove(battery);
    }

    /**
     * @return Returns an array with all the DisCharger of the ChargingStation.
     */
    public DisCharger[] getDisChargers() {
        DisCharger[] g = new DisCharger[dischargers.size()];
        for (int i = 0; i < dischargers.size(); i++)
            g[i] = dischargers.get(i);
        return g;
    }

    /**
     * @return An array with all the Charger.
     */
    public Charger[] getChargers() {
        Charger[] g = new Charger[chargers.size()];
        for (int i = 0; i < chargers.size(); i++)
            g[i] = chargers.get(i);
        return g;
    }

    /**
     * @return An array with all sources that give energy to the ChargingStation.
     */
    public String[] getSources() {
        String[] g = new String[sources.size()];
        for (int i = 0; i < sources.size(); i++)
            g[i] = sources.get(i);
        return g;
    }

    /**
     * @return A HashMap with the amounts of each energy source.
     */
    public HashMap<String, Double> getMap() {
        return amounts;
    }

    /**
     * @param source The source of energy.
     * @return The energy of the source.
     */
    public double getSpecificAmount(final String source) {
        if (!amounts.containsKey(source))
            return 0.0;
        return amounts.get(source);
    }

    /**
     * Sets an amount in a specific source.
     * @param source The source the energy will be added.
     * @param amount The amount of energy to be added.
     */
    public void setSpecificAmount(final String source, final double amount) {
        lock1.lock();
        try {
            amounts.put(source, amount);
        } finally {
            lock1.unlock();
        }
    }

    /**
     * @return The total energy of the ChargingStation.
     */
    public double getTotalEnergy() {
        double counter = 0;
        for (String energy: getSources())
            counter += getMap().get(energy);
        return counter;
    }

    /**
     * @return The fast charging rate of the ChargingStation.
     */
    public double getChargingRateFast() {
        return chargingRateFast;
    }

    /**
     * Sets the charging rate of the fast charging.
     * @param chargingRate The fast charging rate.
     */
    public void setChargingRateFast(final double chargingRate) {
        chargingRateFast = chargingRate;
    }

    /**
     * Sets a charging rate for the slow charging.
     *
     * @param chargingRate The slow charging rate.
     */
    public void setChargingRateSlow(final double chargingRate) {
        chargingRateSlow = chargingRate;
    }

    /**
     * Sets a discharging rate.
     * @param disChargingRat The discharging rate.
     */
    public void setDisChargingRate(final double disChargingRat) {
        this.disChargingRate = disChargingRat;
    }

    /**
     * @return The slow charging rate of the ChargingStation.
     */
    public double getChargingRateSlow() {
        return chargingRateSlow;
    }

    /**
     * Sets the rate of inductive charging.
     * @param inductiveChargingRat The rate of charging during inductive charging.
     */
    public void setInductiveChargingRate(final double inductiveChargingRat) {
        this.inductiveChargingRate = inductiveChargingRat;
    }

    /**
     * @return The rate of charging during inductive charging.
     */
    public double getInductiveRate() {
        return inductiveChargingRate;
    }

    /**
     * @return The discharging rate of the ChargingStation.
     */
    public double getDisChargingRate() {
        return disChargingRate;
    }

    /**
     * @return The price of an energy unit for the inductive charging.
     */
    public double getInductivePrice() {
        return inductivePrice;
    }

    /**
     * Sets the price for an energy unit during the inductive charging.
     *
     * @param price The price of an energy unit.
     */
    public void setInductivePrice(final double price) {
        inductivePrice = price;
    }

    /**
     * Searches for the EnergySource of the given source.
     * @param source The source the EnergySource object is asked.
     * @return The asking EnergySource.
     */
    public EnergySource getEnergySource(final String source) {
        if ("Solar".equalsIgnoreCase(source)) {
            for (EnergySource aN : n)
                if (aN instanceof Solar)
                    return aN;
        }
        else if ("Wind".equalsIgnoreCase(source)) {
            for (EnergySource aN : n)
                if (aN instanceof Wind)
                    return aN;
        }
        else if ("Wave".equalsIgnoreCase(source)) {
            for (EnergySource aN : n)
                if (aN instanceof Wave)
                    return aN;
        }
        else if ("Hydroelectric".equalsIgnoreCase(source)) {
            for (EnergySource aN : n)
                if (aN instanceof Hydroelectric)
                    return aN;
        }
        else if ("Geothermal".equalsIgnoreCase(source)) {
            for (EnergySource aN : n)
                if (aN instanceof Geothermal)
                    return aN;
        }
        else if ("Nonrenewable".equalsIgnoreCase(source)) {
            for (EnergySource aN : n)
                if (aN instanceof Nonrenewable)
                    return aN;
        }
        return null;
    }

    /**
     * @return The energy unit price of the ChargingStation.
     */
    public double getUnitPrice() {
        return unitPrice;
    }

    /**
     * Sets the default price for an energy unit. This price stands for each charging, given that
     * no PricingPolicy is linked with the ChargingStation.
     * @param price The default price for each energy unit.
     */
    public void setUnitPrice(final double price) {
        this.unitPrice = price;
    }

    /**
     * @return The price of the energy unit for the discharging function.
     */
    public double getDisUnitPrice() {
        return disUnitPrice;
    }

    /**
     * Sets the default price for the energy unit for each discharging. The price always stands, since there is
     * no PricingPolicy for the discharging operation.
     * @param disUnitPric The price for every energy unit.
     */
    public void setDisUnitPrice(final double disUnitPric) {
        this.disUnitPrice = disUnitPric;
    }

    /**
     * Sets the price for a battery exchange.
     * @param price The price the exchange costs.
     */
    public void setExchangePrice(final double price) {
        exchangePrice = price;
    }

    /**
     * @return The price of a battery exchange function.
     */
    public double getExchangePrice() {
        return exchangePrice;
    }

    /**
     * Sets the management of the WaitingList.
     * @param value The choice of queue handling's. If true the WaitingList is handled
     * automatic by the library. If false the waiting list should be handled manually.
     */
    public void setAutomaticQueueHandling(final boolean value) {
        automaticQueueHandling = value;
    }

    /**
     * @return True if the WaitingList is handled automatic by the library.
     * False if the waiting list is handled manually.
     */
    public boolean getQueueHandling() {
        return automaticQueueHandling;
    }

    /**
     * @return The time among each energy storage update in milliseconds.
     */
    public int getUpdateSpace() {
        return updateSpace;
    }

    /**
     * Sets the space for the next energy storage update.
     * @param updateSpac The time space in milliseconds.
     */
    public void setUpdateSpace(final int updateSpac) {
        if (timer != null) {
            timer.cancel();
            timer.purge(); }
        if (getUpdateMode() && updateSpac != 0) {
            this.updateSpace = updateSpac;
            timer = new Timer(true);
            timer.schedule(new CheckUpdate(), 0, this.updateSpace); }
        else
            this.updateSpace = 0;
    }

    /**
     * Checks the batteries which are for battery exchange to confirm which of them
     * need charging. Then, it charges as many as available Charger objects there are.
     * @param kind The kind of charging the user wants to charge the batteries.
     **/
    public void batteriesCharging(final String kind) {
        ChargingEvent e;
        ElectricVehicle r;
        Driver driver;
        for (Battery battery : batteries)
            if (battery.getRemAmount() < battery.getCapacity()) {
                r = new ElectricVehicle("Station");
                r.setBattery(battery);
                driver = new Driver("StationDriver");
                r.setDriver(driver);
                e = new ChargingEvent(this, r, battery.getCapacity() - battery.getRemAmount(), kind);
                e.preProcessing();
                e.execution();
            }
    }

    /**
     * Calculates the waiting time the ElectricVehicle should wait and returning it in milliseconds.
     * @param kind The kind of operation for which the waiting time should be calculated. The acceptable values are: "fast" for
     * fast charging, "slow" for slow charging, "exchange" for battery exchange function, "discharging" for
     * the discharging function and "parking" for the parking/inductive charging function.
     * @return The time an ElectricVehicle should wait, to be executed in milliseconds, or -1 if the asked function is not supported.
     */
    public long getWaitingTime(final String kind) {
        long[] counter1 = new long[chargers.size()];
        long[] counter2 = new long[exchangeHandlers.size()];
        long[] counter3 = new long[dischargers.size()];
        long min = -1;
        int index = -1;
        if ("slow".equalsIgnoreCase(kind) || "fast".equalsIgnoreCase(kind)) {
            for (int i = 0; i < chargers.size(); i++) {
                if (Objects.equals(kind, chargers.get(i).getKindOfCharging())) {
                    if (chargers.get(i).getChargingEvent() != null) {
                        if (min == -1) {
                            min = chargers.get(i).getChargingEvent().getRemainingChargingTime();
                            index = i;
                        }
                        long diff = chargers.get(i).getChargingEvent().getRemainingChargingTime();
                        long counter = 0;
                        if (chargers.get(i).planTime.size() != 0) {
                            for (long time : chargers.get(i).planTime)
                                counter += time;
                            diff += counter;
                        }
                        if (min > diff) {
                            min = diff;
                            index = i;
                        }
                        counter1[i] = diff;
                    } else
                        return 0;
                }
            }
        }
        else if ("exchange".equalsIgnoreCase(kind))
            for (int i = 0; i < exchangeHandlers.size(); i++) {
                if (exchangeHandlers.get(i).getChargingEvent() != null) {
                    if (min == -1) {
                        min = exchangeHandlers.get(i).getChargingEvent().getRemainingChargingTime();
                        index = i;
                    }
                    long diff = exchangeHandlers.get(i).getChargingEvent().getRemainingChargingTime();
                    if (min > diff) {
                        min = diff;
                        index = i;
                    }
                    counter2[i] = diff;
                } else
                    return 0;
            }
        else if ("discharging".equalsIgnoreCase(kind))
            for (int i = 0; i < dischargers.size(); i++) {
                if (dischargers.get(i).getDisChargingEvent() != null) {
                    if (min == -1) {
                        min = dischargers.get(i).getDisChargingEvent().getRemainingDisChargingTime();
                        index = i;
                    }
                    long diff = dischargers.get(i).getDisChargingEvent().getRemainingDisChargingTime();
                    if (min > diff) {
                        min = diff;
                        index = i;
                    }
                    counter3[i] = diff;
                } else
                    return 0;
            }
        else if ("parking".equalsIgnoreCase(kind)) {
            for (ParkingSlot parkingSlot : parkingSlots) {
                if (parkingSlot.getParkingEvent() != null) {
                    if ((min == -1 && parkingSlot.getParkingEvent().getCondition().equals("charging")) ||
                            (min > (parkingSlot.getParkingEvent().getRemainingChargingTime() +
                                    parkingSlot.getParkingEvent().getParkingTime() -
                                    parkingSlot.getParkingEvent().getChargingTime()) &&
                                    parkingSlot.getParkingEvent().getCondition().equals("charging")))

                        min = parkingSlot.getParkingEvent().getRemainingChargingTime() +
                                parkingSlot.getParkingEvent().getParkingTime() -
                                parkingSlot.getParkingEvent().getChargingTime();

                    else if ((min == -1 && parkingSlot.getParkingEvent().getCondition().equals("parking")) ||
                            (min > parkingSlot.getParkingEvent().getRemainingParkingTime() &&
                                    parkingSlot.getParkingEvent().getCondition().equals("parking")))

                        min = parkingSlot.getParkingEvent().getRemainingParkingTime();
                } else
                    return 0;
            }
            return min;
        }
        else
            return -1;
        if (min == -1)
            return min;
        ChargingEvent e;
        DisChargingEvent ey;
        if ("fast".equalsIgnoreCase(kind)) {
            WaitList o = this.fast;
            for (int i = 0; i < o.getSize() ; i++) {
                e = (ChargingEvent) o.get(i);
                counter1[index] = counter1[index] + ((long) (e.getAmountOfEnergy() * 3600000 / chargingRateFast));
                for (int j = 0; j < chargers.size(); j++)
                    if ((counter1[j] < counter1[index]) && (counter1[j] != 0))
                        index = j;
            }
            return counter1[index];
        }
        if ("slow".equalsIgnoreCase(kind)) {
            WaitList o = this.slow;
            for (int i = 0; i < o.getSize() ; i++) {
                e = (ChargingEvent) o.get(i);
                counter1[index] = counter1[index] + ((long) (e.getAmountOfEnergy() * 3600000 / chargingRateSlow));
                for (int j = 0; j < chargers.size(); j++)
                    if ((counter1[j] < counter1[index]) && (counter1[j] != 0))
                        index = j;
            }
            return counter1[index];
        }
        if ("exchange".equalsIgnoreCase(kind)) {
            for (int i = 0; i < this.exchange.getSize(); i++) {
                counter2[index] = counter2[index] + timeOfExchange;
                for (int j = 0; j < exchangeHandlers.size(); j++)
                    if ((counter2[j] < counter2[index]) && (counter2[j] != 0))
                        index = j;
            }
            return counter2[index];
        }
        if ("discharging".equalsIgnoreCase(kind)) {
            WaitList o = this.discharging;
            for (int i = 0; i < o.getSize(); i++) {
                ey = (DisChargingEvent) o.get(i);
                counter3[index] = counter3[index] + ((long) (ey.getAmountOfEnergy() * 3600000 / disChargingRate));
                for (int j = 0; j < dischargers.size(); j++)
                    if ((counter3[j] < counter3[index]) && (counter3[j] != 0))
                        index = j;
            }
            return counter3[index];
        }
        return 0;
    }

    /**
     * @return An array with the EnergySource objects of the ChargingStation.
     */
    public EnergySource[] getEnergySources() {
        EnergySource[] g = new EnergySource[n.size()];
        for (int i = 0; i < n.size(); i++)
            g[i] = n.get(i);
        return g;
    }

    /**
     * Sets the time a battery exchange function endures in milliseconds.
     * @param time The time the battery exchange endures in milliseconds.
     */
    public void setTimeofExchange(final long time) {
        timeOfExchange = time;
    }

    /**
     * @return The duration of the battery exchange in milliseconds.
     */
    public long getTimeOfExchange() {
        return timeOfExchange;
    }

    /**
     * Updates the storage of the ChargingStation with the new amounts of energy for each source.
     * The amount of energy is subtracted from the energy inventory for each EnergySource.
     * For each source if the addition is non-zero, then in the next report will be a line.
     * The line shows the EnergySource, the given amount of energy and the date the addition was
     * made to the station.
     */
    public void updateStorage() {
        double energy;
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        for (int j = 0; j < getEnergySources().length; j++) {
            energy = getEnergySources()[j].popAmount();
            if (energy != 0) {
                if (getEnergySources()[j] instanceof Solar) {
                    Calendar calendar = Calendar.getInstance();
                    statistics.addEnergy("Solar, " + energy + ", " + dateFormat.format(calendar.getTime()));
                    energy += getSpecificAmount("Solar");
                    setSpecificAmount("Solar", energy);
                } else if (getEnergySources()[j] instanceof Geothermal) {
                    Calendar calendar = Calendar.getInstance();
                    statistics.addEnergy("Geothermal, " + energy + ", " + dateFormat.format(calendar.getTime()));
                    energy += getSpecificAmount("Geothermal");
                    setSpecificAmount("Geothermal", energy);
                } else if (getEnergySources()[j] instanceof Nonrenewable) {
                    Calendar calendar = Calendar.getInstance();
                    statistics.addEnergy("Nonrenewable, " + energy + ", " + dateFormat.format(calendar.getTime()));
                    energy += getSpecificAmount("Nonrenewable");
                    setSpecificAmount("Nonrenewable", energy);
                } else if (getEnergySources()[j] instanceof Hydroelectric) {
                    Calendar calendar = Calendar.getInstance();
                    statistics.addEnergy("Hydroelectric, " + energy + ", " + dateFormat.format(calendar.getTime()));
                    energy += getSpecificAmount("Hydroelectric");
                    setSpecificAmount("Hydroelectric", energy);
                } else if (getEnergySources()[j] instanceof Wave) {
                    Calendar calendar = Calendar.getInstance();
                    statistics.addEnergy("Wave, " + energy + ", " + dateFormat.format(calendar.getTime()));
                    energy += getSpecificAmount("Wave");
                    setSpecificAmount("Wave", energy);
                } else if (getEnergySources()[j] instanceof Wind) {
                    Calendar calendar = Calendar.getInstance();
                    statistics.addEnergy("Wind, " + energy + ", " + dateFormat.format(calendar.getTime()));
                    energy += getSpecificAmount("Wind");
                    setSpecificAmount("Wind", energy);
                }
            }
        }
    }

    /**
     * @return The current standing price for the charging function.
     */
    public double getCurrentPrice()
    {
        double diff = System.currentTimeMillis() - timestamp;
        if (getPricingPolicy() == null)
            return unitPrice;
        else if (diff > policy.getDurationOfPolicy())
            return unitPrice;
        else
            if (policy.getSpace() != 0)
                return policy.getSpecificPrice((int) (diff / policy.getSpace()));
            else
            {
                double accumulator = 0;
                int counter = 0;
                while (accumulator <= diff) {
                    accumulator += policy.getSpecificTimeSpace(counter);
                    if (accumulator <= diff)
                        counter++;
                }
                return policy.getSpecificPrice(counter);
            }
    }

    /**
     * Calculates the cost for a charging.
     * @param event The ChargingEvent to calculate the cost.
     * @return The cost of the charging.
     */
    public double calculatePrice(final ChargingEvent event)
    {
        if (policy == null)
            if (!"exchange".equalsIgnoreCase(event.getKindOfCharging()))
                return event.getEnergyToBeReceived() * getUnitPrice();
            else
                return getExchangePrice();
        else if (policy.getDurationOfPolicy() < System.currentTimeMillis() - timestamp)
            if (!"exchange".equalsIgnoreCase(event.getKindOfCharging()))
                return event.getEnergyToBeReceived() * getUnitPrice();
            else
                return getExchangePrice();
        else {
            long diff = System.currentTimeMillis() - timestamp;
            if (policy.getSpace() != 0) {
                return event.getEnergyToBeReceived() * policy.getSpecificPrice((int) (diff / policy.getSpace()));
            }
            else {
                double accumulator = 0;
                int counter = 0;
                while (accumulator <= diff)
                {
                    accumulator += policy.getSpecificTimeSpace(counter);
                    if (accumulator <= diff)
                        counter++;
                }
                return event.getEnergyToBeReceived() * policy.getSpecificPrice(counter);
            }
        }
    }

    /**
     * @return The PricingPolicy of the ChargingStation.
     */
    public PricingPolicy getPricingPolicy() {
        return policy;
    }

    /**
     * Links a PricingPolicy with the ChargingStation.
     * @param polic The policy to be linked with.
     */
    public void setPricingPolicy(final PricingPolicy polic) {
        timestamp = System.currentTimeMillis();
        this.policy = polic;
    }

    /**
     * Sets the way the energy storage will become. If the update becomes automatically, then a Timer object
     * starts. The Timer calls the updateStorage() function every "updateSpace" milliseconds.
     * @param update The way the update will become. False means manually, true means automatic.
     */
    public void setAutomaticUpdateMode(final boolean update) {
        if (!update) {
            this.automaticUpdate = false;
            this.updateSpace = 0;
            if (timer != null) {
                timer.cancel();
                timer.purge();
            }
        }
        else
            this.automaticUpdate = true;
    }

    /**
     * @return The way the energy storage updateis made. True if automatically, false for manually.
     */
    public boolean getUpdateMode() {
        return automaticUpdate;
    }

    /**
     * Generates a report with all the recent traffic in the charging station.
     * It also records the current situation of the station.
     * @param filePath The absolute path where the user wants to save the report. The file has to be .txt.
     */
    public void genReport(final String filePath) {
        statistics.generateReport(filePath);
    }

    /**
     * @return True if the created threads are deamons, false if not.
     */
    public boolean getDeamon()
    {
        return deamon;
    }

    /**
     * @return The name of the ChargingStation.
     */
    public String getName()
    {
        return name;
    }

    /**
     * The method is responsible for the partial execution of a predefined plan of chargings. The plan is given through a text(.txt) file.
     * There can only be only one simultaneous execution of a plan. We assume that there are adequate resources(energy and chargers)
     * for the successful completion of the plan.
     * @param filepath The file with plan of chargings.
     * @throws FileNotFoundException In case the file was not found.
     */
    public void execEvents(final String filepath) throws FileNotFoundException {
        if (!execEvents) {
            execEvents = true;
            String line;
            String[] tokens;
            try (BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(filepath), java.nio.charset.StandardCharsets.UTF_8))){
                while ((line = in.readLine()) != null) {
                    tokens = line.split(",");
                    ElectricVehicle vehicle;
                    if (tokens[0].equals("ev")) {
                        vehicle = new ElectricVehicle("Station");
                        vehicle.setDriver(new Driver("StationDriver"));
                        ChargingEvent event = new ChargingEvent(this, vehicle, Double.parseDouble(tokens[1]), "partial");
                        event.setEnergyToBeReceived(Double.parseDouble(tokens[1]));
                        event.setCost(calculatePrice(event));
                        event.setCondition("interrupted");
                        events.add(event);
                        double sdf;
                        sdf = Double.parseDouble(tokens[1]);
                        for (String s : sources) {
                            if (sdf < amounts.get(s)) {
                                double ert = amounts.get(s) - sdf;
                                amounts.put(s, ert);
                                break;
                            } else {
                                sdf -= amounts.get(s);
                                amounts.put(s, 0.0);
                            }
                        }
                    } else if (tokens[0].equals("de")) {
                        boolean flag = false;
                        int j = 0;
                        while (!flag) {
                            if (chargers.get(j).getChargingEvent() == null)
                                flag = true;
                            if (!flag)
                                j++;
                        }
                        numberOfChargers.add(j);
                        for (int i = 1; i < tokens.length; i++) {
                            switch (tokens[i]) {
                                case "ch":
                                    if (chargers.get(j).planEvent.size() == 0)
                                        chargers.get(j).setChargingEvent(events.get(Integer.parseInt(tokens[i + 1]) - 1));
                                    chargers.get(j).planEvent.add(Integer.parseInt(tokens[i + 1]));
                                    chargers.get(j).planTime.add(Long.parseLong(tokens[i + 2]));
                                    break;
                                case "int":
                                    if (chargers.get(j).planEvent.size() == 0)
                                        chargers.get(j).setChargingEvent(new ChargingEvent(this, null, 0, null));
                                    chargers.get(j).planEvent.add(-1);
                                    chargers.get(j).planTime.add(Long.parseLong(tokens[i + 1]));
                                    break;
                                default:
                                    break;
                            }
                        }
                    }
                }
            } catch (IOException e) {
                System.out.println("Broken file");
            }
            numberOfChargers.forEach(e -> chargers.get(e).startCharger());
        }
    }

    /**
     * Sets if the created threads are deamons or not.
     *
     * @param deam The value to be set. True means deamon, false not deamons.
     */
    public void setDeamon(final boolean deam) {
        this.deamon = deam;
    }

    private class Statistics {
        private final List<String> energyLog;

        Statistics() {
            energyLog = new ArrayList<>();
        }

        void addEnergy(final String energy) {
            energyLog.add(energy);
        }

        void generateReport(final String filePath) {
            List<String> content = new ArrayList<>();
            AtomicInteger counter = new AtomicInteger(0);
            content.add("********************");
            content.add("");
            content.add("Id: " + id);
            content.add("Name: " + name);
            content.add("Remaining energy: " + getTotalEnergy());
            content.add("Fast chargers: " + FAST_CHARGERS);
            content.add("Slow chargers: " + SLOW_CHARGERS);
            content.add("Dischargers: " + dischargers.size());
            content.add("Exchange handlers: " + exchangeHandlers.size());
            content.add("Parking slots: " + parkingSlots.size());
            Consumer<ChargingEvent> consumer1 = e -> {
                if ((e.getStation().getName().equals(name)) && (e.getCondition().equals("finished")) && (e.getKindOfCharging().equals("fast")))
                    counter.incrementAndGet();
            };
            ChargingEvent.getChargingLog().forEach(consumer1);
            content.add("Completed fast chargings: " + counter);
            counter.set(0);
            consumer1 = e -> {
                if ((e.getStation().getName().equals(name)) && (e.getCondition().equals("finished")) && (e.getKindOfCharging().equals("slow")))
                    counter.incrementAndGet();
            };
            ChargingEvent.getChargingLog().forEach(consumer1);
            content.add("Completed slow chargings: " + counter);
            counter.set(0);
            Consumer<DisChargingEvent> consumer2 = e -> {
                if ((e.getStation().getName().equals(name)) && (e.getCondition().equals("finished")))
                    counter.incrementAndGet();
            };
            DisChargingEvent.getDischargingLog().forEach(consumer2);
            content.add("Completed dischargings: " + counter);
            counter.set(0);
            Consumer<ChargingEvent> consumer3 = e -> {
                if ((e.getStation().getName().equals(name)) && (e.getCondition().equals("finished")))
                    counter.incrementAndGet();
            };
            ChargingEvent.getExchangeLog().forEach(consumer3);
            content.add("Completed battery swappings: " + counter);
            counter.set(0);
            Consumer<ParkingEvent> consumer4 = e -> {
                if ((e.getStation().getName().equals(name)) && (e.getCondition().equals("finished")))
                    counter.incrementAndGet();
            };
            ParkingEvent.getParkLog().forEach(consumer4);
            content.add("Completed parkings: " + counter);
            content.add("Vehicles waiting for fast charging: " + fast.getSize());
            content.add("Vehicles waiting for slow charging: " + slow.getSize());
            content.add("Vehicles waiting for discharging: " + discharging.getSize());
            content.add("Vehicles waiting for battery swapping: " + exchange.getSize());
            content.add("Energy amounts: ");
            for (String s : getSources())
                content.add("  " + s + ": " + getSpecificAmount(s));
            content.add("");
            content.add("***Charging events***");
            for (ChargingEvent ev : ChargingEvent.getChargingLog()) {
                if (ev.getStation().getName().equals(name)) {
                    content.add("");
                    content.add("Id: " + ev.getId());
                    content.add("Station name: " + ev.getStation().getName());
                    content.add("Asking energy: " + ev.getAmountOfEnergy());
                    content.add("Received energy: " + ev.getEnergyToBeReceived());
                    content.add("Condition: " + ev.getCondition());
                    content.add("Charging time: " + ev.getChargingTime());
                    content.add("Waiting time: " + ev.getWaitingTime());
                    content.add("Maximum waiting time: " + ev.getMaxWaitingTime());
                    content.add("Cost: " + ev.getCost());
                }
            }
            content.add("");
            content.add("***Discharging events***");
            for (DisChargingEvent ev : DisChargingEvent.getDischargingLog()) {
                if (ev.getStation().getName().equals(name)) {
                    content.add("");
                    content.add("Id: " + ev.getId());
                    content.add("Station name: " + ev.getStation().getName());
                    content.add("Asking energy: " + ev.getAmountOfEnergy());
                    content.add("Condition: " + ev.getCondition());
                    content.add("Discharging time: " + ev.getDisChargingTime());
                    content.add("Waiting time: " + ev.getWaitingTime());
                    content.add("Maximum waiting time: " + ev.getMaxWaitingTime());
                    content.add("Profit: " + ev.getProfit());
                }
            }
            content.add("");
            content.add("***Exchange events***");
            for (ChargingEvent ev : ChargingEvent.getExchangeLog()) {
                if (ev.getStation().getName().equals(name)) {
                    content.add("");
                    content.add("Id: " + ev.getId());
                    content.add("Station name: " + ev.getStation().getName());
                    content.add("Condition: " + ev.getCondition());
                    content.add("Waiting time: " + ev.getWaitingTime());
                    content.add("Maximum waiting time: " + ev.getMaxWaitingTime());
                    content.add("Cost: " + ev.getCost());
                }
            }
            content.add("");
            content.add("***Parking events***");
            for (ParkingEvent ev : ParkingEvent.getParkLog()) {
                if (ev.getStation().getName().equals(name)) {
                    content.add("");
                    content.add("Id: " + ev.getId());
                    content.add("Station name: " + ev.getStation().getName());
                    content.add("Amount of energy: " + ev.getAmountOfEnergy());
                    content.add("Received energy: " + ev.getEnergyToBeReceived());
                    content.add("Condition: " + ev.getCondition());
                    content.add("Parking time: " + ev.getParkingTime());
                    content.add("Charging time: " + ev.getChargingTime());
                    content.add("Cost: " + ev.getCost());
                }
            }
            content.add("");
            content.add("***Energy additions***");
            for (String s: energyLog) {
                content.add("");
                content.add(s);
            }
            content.add("");
            content.add("********************");
            Writer writer = null;
            try {
                writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filePath), "utf-8"));
                for (String line : content) {
                    line += System.getProperty("line.separator");
                    writer.write(line);
                }
            } catch (IOException ignored) {

            }
            finally {
                if (writer != null)
                    try {
                        writer.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
            }
        }
    }
}
