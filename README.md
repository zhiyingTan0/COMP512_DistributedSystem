# Distributed_system
The project this semester is a Travel Reservation system, where customers can reserve flights, cars and rooms for their vacation. 

### 1. Distributed Application: https://github.com/zhiyingTan0/Distributed_system/tree/main/Distributed%20Application
Distribute the user's request through the middleware to different server. Both RMI and TCP are used to implement the distribution. We have 3 servers with different port number: Cars, Rooms and Flights.For example, if client A wants to book a flight, then his request will be submitted to the middleware and then sent to flight server. If client B wants to cancel a room reservation, then his request will be sent to Room server.

### 2. Transaction and concurrency control
