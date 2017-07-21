# EVLib
EVLib is a library for the management and the simulation of EV activities in a charging station level which makes use
of a set of available energy sources. It is implemented in the JAVA programming language, and its main goal is to man-
age the charging, discharging and battery swap functions and support their integration into a single charging station.
The library supports a large number of functions to properly manage EV-related activities. There are three main
functions, as well as a number of secondary ones, while each function is executed in 2 phases, namely the pre-processing
and the execution.
## Use
The library has to be compiled using Maven.

## Main Functions

### Charging: 
There are 2 types of charging depending on the charging time, namely the fast and the slow charging. The execution of a charging event requires first the pre-processing phase where a quest for an empty charger and available energy is performed. If the pre-processing phase is successful, the execution phase begins.

### DisCharging: 
Similarly to a charging event, a discharging event first demands the pre-processing phase where a quest for an empty dis-charger is made. If this phase is successful then the execution begins.

### Battery Exchange: 
The pre-processing phase requires for a battery with enough range to be available in the charging station. If such a battery is found, the execution function can be called and the battery is swapped into the EV.

## Examples
```
  String[] kinds = new string[4] { "slow", "fast", "fast", "slow" };
  String[] sources = new string[4] { "geothermal", "nonrenewable", "wind", "wave" };
  float[][] energyAm = new float[4][ 5];
  for (int i = 0; i<4; i++)
      for (int j = 0; j<5; j++)
        energyAm [i][j] = 150;
  
  ChargingStation station = new ChargingStation(1, "Miami", kinds, sources, energyAm);
  DisCharger dsc = new DisCharger(7, station);
  ExchangeHandler handler = new ExchangeHandler(5, station);
	
  station.insertExchangeHandler(handler);

  station.insertDisCharger(dsc);

  //Sets the space between every update in milliseconds.
  station.setUpdateSpace(10000);
  
  station.setChargingRatioFast(0.01);
  station.setDisChargingRatio(0.1);

  //Sets the duration of a battery exchange in milliseconds
  station.setTimeofExchange(5000);

  Driver a = new Driver(4, "Tom");

  ElectricVehicle vec1 = new ElectricVehicle(1, "Honda", 1950);
  ElectricVehicle vec2 = new ElectricVehicle(2, "Toyota", 1400);
  ElectricVehicle vec3 = new ElectricVehicle(3, "Mitsubishi", 1500);
  ElectricVehicle vec4 = new ElectricVehicle(4, "Fiat", 1600);

  Battery bat1 = new Battery(8, 1500, 5000);
  Battery bat2 = new Battery(9, 2000, 6000);
  Battery bat3 = new Battery(10, 2500, 6000);
  Battery bat4 = new Battery(82, 800, 3000);
  Battery bat5 = new Battery(7, 0, 800);

  //Links a battery with a charging station for the exchange battery function
  station.joinBattery(bat4);

  vec1.setDriver(a);
  vec1.vehicleJoinBattery(bat1);
  vec2.setDriver(a);
  vec2.vehicleJoinBattery(bat2);
  vec3.setDriver(a);
  vec3.vehicleJoinBattery(bat3);
  vec4.setDriver(a);
  vec4.vehicleJoinBattery(bat5);

  ChargingEvent ev1 = new ChargingEvent(station, vec1, 300, "fast");
  ChargingEvent ev2 = new ChargingEvent(station, vec2, 600, "fast");
  ChargingEvent ev3 = new ChargingEvent(station, vec3, 200, "fast");
  ChargingEvent ev5 = new ChargingEvent(station, vec1, 300, "fast");
  ChargingEvent ev7 = new ChargingEvent(station, vec4, "exchange");

  DisChargingEvent ev4 = new DisChargingEvent(station, vec1, 500);
  DisChargingEvent ev6 = new DisChargingEvent(station, vec1, 800);

  //Sets the maximum time a vehicle can wait in milliseconds
  ev3.setWaitingTime(50000);
  ev5.setWaitingTime(120000);
  ev6.setWaitingTime(450000);

  ev1.preProcessing();
  ev1.execution();

  ev2.preProcessing();
  ev2.execution();

  ev3.preProcessing();
  ev3.execution();

  ev4.preProcessing();
  ev4.execution();

  ev5.preProcessing();
  ev5.execution();

  ev6.preProcessing();
  ev6.execution();

  ev7.preProcessing();
  ev7.execution();
```
