package recommendedagencies;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;

public class JsonFetch {

    public static void main(String[] args) throws IOException {
        getAllData();
    }

    private static void getAllData() throws IOException {
        String s = HttpUtils.get("http://search.recommendedagencies.com/api/v1/search?services[]=digital/web-development/&offset=0&limit=2400");
        ObjectMapper mapper = new ObjectMapper();
        Object o = mapper.readValue(s, Object.class);
        mapper.writeValue(new File("main_data.json"), o);
    }

}
