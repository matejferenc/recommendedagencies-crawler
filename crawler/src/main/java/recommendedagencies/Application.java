package recommendedagencies;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Application {

    private static final String BASE_URL = "http://search.recommendedagencies.com/api/v1/search?services[]=digital/web-development/&offset=";

    public static void main(String[] args) throws IOException {
        getPage();
    }

    private static void getPage() throws IOException {
        ClassLoader classLoader = Application.class.getClassLoader();
        File file = new File(classLoader.getResource("main_data.json").getFile());
        ObjectMapper mapper = new ObjectMapper();
        HashMap map = mapper.readValue(file, HashMap.class);
        ((List) map.get("response"))
                .forEach(company -> {
                    String page = HttpUtils.get((String)((Map) company).get("url"));
                });
        boolean stop = true;
    }

    private static void getAllData() throws IOException {
        String s = HttpUtils.get("http://search.recommendedagencies.com/api/v1/search?services[]=digital/web-development/&offset=0&limit=2400");
        ObjectMapper mapper = new ObjectMapper();
        Object o = mapper.readValue(s, Object.class);
        mapper.writeValue(new File("main_data.json"), o);
    }

}
