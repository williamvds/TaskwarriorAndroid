package kvj.taskw.data;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by vorobyev on 11/19/15.
 */
public class ReportInfo {

    public Map<String, Boolean> sort = new LinkedHashMap<>();
    public Map<String, String> fields = new LinkedHashMap<>();
    public String query = "";
    public String description = "Untitled";
    public List<String> priorities = new ArrayList<>();

    @NotNull
    @Override
    public String toString() {
        return String.format("ReportInfo: %s [%s %s] %s", query, fields.toString(), sort.toString(), description);
    }

    public void sort(List<Task> list) {
        Collections.sort(list, new Comparator<Task>() {
            @Override
            public int compare(Task lhs, Task rhs) {
                for (Map.Entry<String, Boolean> entry : sort.entrySet()) {
                    String fieldName = entry.getKey();
                    Object lo, ro;

                    try {
                        Field field = Task.class.getField(fieldName);
                        lo = field.get(lhs);
                        ro = field.get(rhs);
                    } catch (Exception e) {
                        android.util.Log.e(ReportInfo.class.getName(), "Couldn't access task field: " + fieldName);
                        continue;
                    }

                    if (lo == null && ro != null) {
                        return 1;
                    }
                    if (lo != null && ro == null) {
                        return -1;
                    }
                    if (lo == null && ro == null) {
                        continue;
                    }
                    if (lo instanceof Number) {
                        double ld = ((Number)lo).doubleValue();
                        double rd = ((Number)ro).doubleValue();
                        if (ld == rd)
                            continue;
                        return (ld > rd? 1: -1) * (entry.getValue()? 1: -1);
                    }
                    if (lo instanceof String) {
                        int result = ((String)lo).compareTo((String) ro);
                        if (result == 0)
                            continue;
                        return result * (entry.getValue()? 1: -1);
                    }
                }
                return 0;
            }
        });
    }
}
