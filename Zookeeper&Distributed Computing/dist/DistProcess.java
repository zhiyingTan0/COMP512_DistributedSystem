import java.io.*;

import java.nio.charset.StandardCharsets;
import java.sql.SQLOutput;
import java.util.*;

// To get the name of the host.
import java.net.*;

//To get the process id.
import java.lang.management.*;

import org.apache.zookeeper.*;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.KeeperException.*;
import org.apache.zookeeper.data.*;
import org.apache.zookeeper.KeeperException.Code;

// TODO
// Replace XX with your group number.
// You may have to add other interfaces such as for threading, etc., as needed.
// This class will contain the logic for both your master process as well as the worker processes.
//  Make sure that the callbacks and watch do not conflict between your master's logic and worker's logic.
//		This is important as both the master and worker may need same kind of callbacks and could result
//			with the same callback functions.
//	For a simple implementation I have written all the code in a single class (including the callbacks).
//		You are free it break it apart into multiple classes, if that is your programming style or helps
//		you manage the code more modularly.
//	REMEMBER !! ZK client library is single thread - Watches & CallBacks should not be used for time consuming tasks.
//		Ideally, Watches & CallBacks should only be used to assign the "work" to a separate thread inside your program.

// some general design of our program
// the workers are placed under the workers
// the data of the worker znode would represent the status (idle or busy)
public class DistProcess implements Watcher, AsyncCallback.ChildrenCallback {
    ZooKeeper zk;
    String zkServer, pinfo;
    boolean isMaster = false;

    // these two would only be used if it is a worker node
    String workID = "";
    String path = "";

    HashMap<String, Boolean> workers = new HashMap<>();
    TaskMonitor monitor = new TaskMonitor();

    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_RESET = "\u001B[0m";

    DistProcess(String zkhost) {
        zkServer = zkhost;
        pinfo = ManagementFactory.getRuntimeMXBean().getName();
        System.out.println("DISTAPP : ZK Connection information : " + zkServer);
        System.out.println("DISTAPP : Process information : " + pinfo);
    }

    void startProcess() throws IOException, UnknownHostException, KeeperException, InterruptedException {
        zk = new ZooKeeper(zkServer, 1000, this); //connect to ZK.
        try {
            runForMaster();    // See if you can become the master (i.e, no other master exists)
            isMaster = true;
            // start to monitor the workers
            getWorkers();
            getTasks(); // Install monitoring on any new tasks that will be created.
            getFinished();
        } catch (NodeExistsException nee) {
            // this means that this would become a worker process
            isMaster = false;
            registerAsWorker();
        }
        print("DISTAPP : Role : " + " I will be functioning as " + (isMaster ? "master" : "worker"));
    }

    // Master fetching task znodes...
    void getTasks() {
        zk.getChildren("/dist08/tasks", this, this, null);
    }

    // this method is responsible for fetching the workers
    // also set itself as a watcher if the a new worker comes in
    void getWorkers() {
        zk.getChildren("/dist08/workers", this, this, null);
    }

    // get the workers that finished their work
    // and install the watch
    void getFinished(){
        zk.getChildren("/dist08/finished", this, this, null);
    }

    void registerAsWorker() {
        // this would create a worker ephemeral znode
        // the status of the worker would be denoted by the data
        try {
            path = zk.create("/dist08/workers/worker-",
                    "idle".getBytes(),
                    Ids.OPEN_ACL_UNSAFE,
                    CreateMode.EPHEMERAL_SEQUENTIAL
                    );
            print("WORKER: My name is " + path);
            workID = path.replace("/dist08/workers/worker-", "");
            zk.getData(path, this, null, null);
        } catch (KeeperException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    // Try to become the master.
    void runForMaster() throws UnknownHostException, KeeperException, InterruptedException {
        //Try to create an ephemeral node to be the master, put the hostname and pid of this process as the data.
        // This is an example of Synchronous API invocation as the function waits for the execution and no callback is involved..
        zk.create("/dist08/master", pinfo.getBytes(), Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
    }

    public void process(WatchedEvent e) {
        //Get watcher notifications.
        System.out.println("DISTAPP : Event received : " + e);
        // Master should be notified if any new znodes are added to tasks.
        if (e.getType() == Watcher.Event.EventType.NodeChildrenChanged && e.getPath().equals("/dist08/tasks")) {
            // There has been changes to the children of the node.
            // We are going to re-install the Watch as well as request for the list of the children.
            if (isMaster) {
                getTasks();
            }
        }

        if (e.getType() == Watcher.Event.EventType.NodeChildrenChanged && e.getPath().equals("/dist08/workers")) {
            // There has been changes to the children of the node.
            // We are going to re-install the Watch as well as request for the list of the children for workers!!!
            if (isMaster) {
                getWorkers();
            }
        }

        if (e.getType() == Event.EventType.NodeChildrenChanged && e.getPath().startsWith("/dist08/finished")) {
            // There has been changes to the children of the node.
            // We are going to re-install the Watch as well as request for the list of the children for workers!!!
            if (isMaster) {
                zk.getChildren("/dist08/finished", this, this, null);
            }
        }

        if (!isMaster) {
            // only for worker
            if (e.getType() == Event.EventType.NodeDataChanged && e.getPath().equals("/dist08/workers/worker-" + workID)) {
                // check whether we are going to work
                //System.out.println("start to work");
                zk.getData("/dist08/workers/worker-" + workID, this, getWorkerDataCallback, null);
            }
        }
    }

    //Asynchronous callback that is invoked by the zk.getChildren request.
    public void processResult(int rc, String path, Object ctx, List<String> children) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                System.out.println("DISTAPP : processResult : " + rc + ":" + path + ":" + ctx);
                if ("/dist08/tasks".equals(path)) {
                    // start to assign the tasks
                    if (isMaster) {
                        // check the availability of each worker
                        // and try to assign the tasks
                        for (String c : children) {
                            boolean willAssign = false;
                            synchronized (monitor.getAssignedTasks()){
                                if (!monitor.getAssignedTasks().contains("/dist08/tasks/" + c)){
                                    monitor.getAssignedTasks().add("/dist08/tasks/" + c);
                                    willAssign = true;
                                }
                            }
                            if (willAssign) {
                                monitor.getAssignedTasks().add("/dist08/tasks/" + c);
                                boolean hasBeenAssigned = false;
                                synchronized (workers){
                                    for (String m : workers.keySet()) {
                                        if (!workers.get(m)) {
                                            print("MASTER: Assigned " + c  + " to worker " + m);
                                            hasBeenAssigned = true;
                                            zk.setData(m, c.getBytes(),
                                                    -1, null, c);
                                            workers.put(m, true);
                                            break;
                                        }
                                    }
                                }
                                if (hasBeenAssigned){
                                    break;
                                }
                                // there is no available workers
                                // just wait
                                print("MASTER: No available worker left...");
                                boolean hasPrinted = false;
                                boolean willGo = true;
                                while (willGo) {
                                    if (!hasPrinted){
                                        print("MASTER: Waiting for available workers for task " + c + " ...");
                                        hasPrinted = true;
                                    }
                                    synchronized (workers){
                                        for (String m : workers.keySet()) {
                                            if (!workers.get(m)) {
                                                print("MASTER: Assigned " + c  + " to worker " + m + " finally");
                                                monitor.getAssignedTasks().add("/dist08/tasks/" + c);
                                                zk.setData(m, c.getBytes(),
                                                        -1, null, c);
                                                workers.put(m, true);
                                                willGo = false;
                                                break;
                                            }
                                        }
                                    }
                                    try {
                                        Thread.sleep(1000);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                        }
                    }
                }
                if ("/dist08/workers".equals(path)) {
                    // keep track of the workers
                    if (isMaster) {
                        for (String c : children) {
                            if (!workers.keySet().contains("/dist08/workers/" + c)) {
                                //System.out.println(c);
                                // add this worker to the dataset
                                synchronized (workers){
                                    workers.put("/dist08/workers/" + c, false);
                                }
                                print("MASTER:" + " /dist08/workers/" + c + " just joined!");
                            }
                        }
                    }
                }
                if ("/dist08/finished".equals(path)) {
                    if (isMaster) {
                        for (String c : children) {
                            // add this worker to the dataset as it is available again
                            zk.delete("/dist08/finished/" + c, -1, null, null);
                            synchronized (workers){
                                workers.put("/dist08/workers/" + c, false);
                            }
                            print("MASTER: /dist08/workers/" + c + " is now idle again");
                        }
                    }
                }

            }
        }).start();
    }

    public void print(String info){
        System.out.println(ANSI_GREEN + info + ANSI_RESET);
    }

    public static void main(String args[]) throws Exception {
        //Create a new process
        //Read the ZooKeeper ensemble information from the environment variable.
        DistProcess dt = new DistProcess(System.getenv("ZKSERVER"));
        dt.startProcess();

        //Replace this with an approach that will make sure that the process is up and running forever.
        //Thread.sleep(10000);
        while (true) {
            //System.out.println("Running...");
            Thread.sleep(1000);
        }
    }


    // ---------------------------------------------------------------------->
    // this is the part for all the types of the call backs
    DataCallback getWorkerDataCallback = new DataCallback() {
        @Override
        public void processResult(int i, String s, Object o, byte[] bytes, Stat stat) {
            switch (Code.get(i)) {
                case OK:
                    String taskID = new String(bytes, StandardCharsets.UTF_8);
                    if ("idle".equals(taskID)) {
                        return;
                    }
                    print("WORKER: Try to query the task!");
                    zk.getData("/dist08/tasks/" + taskID, null, getTaskDataCallback, this);
                    break;
                case CONNECTIONLOSS:
                    System.out.println("WORKER: Some connection error has occurred...");
                    System.out.println("WORKER: Try to reconnect...");
            }
        }
    };

    DataCallback getTaskDataCallback = new DataCallback() {
        @Override
        public void processResult(int i, String s, Object o, byte[] bytes, Stat stat) {
            switch (Code.get(i)) {
                case OK:
                    try {
                        // Re-construct our task object.
                        ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
                        ObjectInput in = new ObjectInputStream(bis);
                        DistTask dt = (DistTask) in.readObject();
                        print("WORKER: Start to compute!");
                        //Execute the task.
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                dt.compute();
                                // Serialize our Task object back to a byte array!
                                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                                ObjectOutputStream oos = null;
                                try {
                                    oos = new ObjectOutputStream(bos);
                                    oos.writeObject(dt);
                                    oos.flush();
                                    byte[] taskSerial = bos.toByteArray();

                                    // Store it inside the result node.
                                    zk.create(s + "/result", taskSerial, Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
                                    //zk.create("/dist08/tasks/"+c+"/result", ("Hello from "+pinfo).getBytes(), Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);

                                    // change the work status back to idle
                                    zk.setData(path, "idle".getBytes(), -1);
                                    // a notifier for the work has been finished
                                    zk.create("/dist08/finished/worker-" + workID, "".getBytes(), Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
                                    print("WORKER: Finished task for " + s);
                                } catch (KeeperException e) {
                                    e.printStackTrace();
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        }).start();
                    } catch (IOException io) {
                        System.out.println(io);
                    } catch (ClassNotFoundException cne) {
                        System.out.println(cne);
                    }
                    break;
                case CONNECTIONLOSS:
                    System.out.println("WORKER: Some connection error has occurred...");
                    System.out.println("WORKER: Try to reconnect...");
                    break;
                case NONODE:
                    System.out.println("Get znode data failed...");
                    print("The task has already been done");
                    break;
            }
        }
    };
}
