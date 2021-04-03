# Distributed_system
The project this semester is a Travel Reservation system, where customers can reserve flights, cars and rooms for their vacation. 

### 1. Distributed Application: 
https://github.com/zhiyingTan0/Distributed_system/tree/main/Distributed%20Application

Distribute the user's request through the middleware to different server. Both RMI and TCP are used to implement the distribution. We have 3 servers with different port number: Cars, Rooms and Flights.For example, if client A wants to book a flight, then his request will be submitted to the middleware and then sent to flight server. If client B wants to cancel a room reservation, then his request will be sent to Room server.
<img width="892" alt="Screen Shot 2021-04-02 at 8 27 16 PM" src="https://user-images.githubusercontent.com/50588149/113463085-fb73be00-93f1-11eb-9186-76e602e1f18d.png">



### 2. Transaction and concurrency control

Implement the lock manager, centralized transactions and Distributed Transaction to manipulate the requests from client.
<img width="813" alt="Screen Shot 2021-04-02 at 8 25 41 PM" src="https://user-images.githubusercontent.com/50588149/113463010-b059ab00-93f1-11eb-916a-654df5093fd5.png">

