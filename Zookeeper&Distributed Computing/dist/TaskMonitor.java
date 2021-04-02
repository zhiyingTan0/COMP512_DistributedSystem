import java.util.ArrayList;
import java.util.List;

public class TaskMonitor {
    List<String> assignedTasks = new ArrayList<>();

    public TaskMonitor(){}

    List<String> getAssignedTasks(){
        return assignedTasks;
    }
}
