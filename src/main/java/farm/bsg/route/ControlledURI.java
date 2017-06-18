package farm.bsg.route;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public abstract class ControlledURI {

    public String href(String... args) {
        if (args.length == 0) {
            return href(Collections.emptyMap());
        } else {
            HashMap<String, String> map = new HashMap<>();
            for (int k = 0; k + 1 < args.length; k+=2) {
                map.put(args[k], args[k+1]);
            }
            return href(map);
        }
    }
    
    protected abstract String href(Map<String, String> map);
}