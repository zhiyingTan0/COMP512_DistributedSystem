# Distributed_system
The project this semester is a Travel Reservation system, where customers can reserve flights, cars and rooms for their vacation. 

### 1. Distributed Application: 
https://github.com/zhiyingTan0/Distributed_system/tree/main/Distributed%20Application

Distribute the user's request through the middleware to different server. Both RMI and TCP are used to implement the distribution. We have 3 servers with different port number: Cars, Rooms and Flights.For example, if client A wants to book a flight, then his request will be submitted to the middleware. Then sent to flight server and handled by the FlightResourceManager. If client B wants to cancel a room reservation, then his request will be sent to Room server and handled by the RoomResourceManager.The following diagram shows the implementation with RMI.

<img width="892" alt="Screen Shot 2021-04-02 at 8 27 16 PM" src="https://user-images.githubusercontent.com/50588149/113463085-fb73be00-93f1-11eb-9186-76e602e1f18d.png">



### 2. Transaction and concurrency control

https://github.com/zhiyingTan0/COMP512_DistributedSystem/tree/main/Transaction%20and%20Concurrency%20Control

Implement the lock manager, centralized transactions and Distributed Transaction(strict 2PL) in the middleware layer to manipulate different requests from client. Process all the transactions concurrently.

<img width="813" alt="Screen Shot 2021-04-02 at 8 25 41 PM" src="https://user-images.githubusercontent.com/50588149/113463010-b059ab00-93f1-11eb-916a-654df5093fd5.png">

### 3. Zookeeper and Distributed Computing

https://github.com/zhiyingTan0/COMP512_DistributedSystem/tree/main/Zookeeper%26Distributed%20Computing

Use the distrbuted computation process to sample the data and calculate pi value. If a client submit a task, then it will be stored in the /dist08/task node. The watcher in /dist08/task will trigger /dist08/master node to assign the task to one of the 'idle' workers under /dist08/workers. Once the job get finished, it will be moved to /dist08/finished node. The callback function will be called and the worker become avaliable again.

<img width="845" alt="Screen Shot 2021-04-02 at 8 43 39 PM" src="https://user-images.githubusercontent.com/50588149/113463581-8d7cc600-93f4-11eb-826f-55ca054ffdd3.png">


