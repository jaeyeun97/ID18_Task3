package uk.ac.cam.cl.id.group18.task3;

import java.net.URL;
import java.net.MalformedURLException;
import java.io.*;
import java.util.*;
import com.google.gson.*;
import javafx.scene.image.Image;
/**
 * Created by Leo Williams.
 * Written by Leo Williams, Charles Yoon.
 */
public class ImageRequest {

	public static String defaultTime = "";
	private static JsonArray jArray = null;

	public static Image getImage(String layer, String time, int timeStep) throws IOException{
		 InputStream is = DataTools.request("layer/wxfcs/" + layer + "/png?RUN=" + time + "Z&FORECAST=" + timeStep + "&").openStream();
		return new Image(is);
	}

	static {
        try {
            URL url = DataTools.request("layer/wxfcs/all/json/capabilities");
            InputStream is = url.openStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String buildString = "";
            String line;
            while((line = br.readLine()) != null)
                buildString += line;

            br.close();
            is.close();

            String data = cleanup(buildString);

            jArray = new Gson().fromJson(data, JsonArray.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    //timeStep should be the number of hours into the future for the forecast. If it doesn't go that far forward,
    //A NullPointerException will be thrown. This is expected behaviour.
    //On success, it returns a map from MapType to Image.
    public static Map<MapType, Image> getLayers(int timeStep) throws IOException {
        HashMap<MapType, Image> layers = new HashMap<>();

        for(int i = 0; i < jArray.size(); i++){
            JsonObject jObj = jArray.get(i).getAsJsonObject().getAsJsonObject("Service");
            String layer = jObj.getAsJsonPrimitive("LayerName").getAsString();
            JsonObject timeSteps = jObj.getAsJsonObject("Timesteps");
            defaultTime = timeSteps.getAsJsonPrimitive("defaultTime").getAsString();
            Integer[] timeStepsArray = new Gson().fromJson(timeSteps.get("Timestep"), Integer[].class);

            timeStep = timeStepsArray[timeStep/timeStepsArray[1]]; //Forces timestep onto a suitable value, scaled by the first non-zero val

            MapType map = null;
            switch(layer) {
                case "Precipitation_Rate":
                    map = MapType.RAINFALL;
                    break;
                case "Total_Cloud_Cover":
                    map = MapType.CLOUD;
                    break;
                case "Temperature":
                    map = MapType.TEMP;
                    break;
                case "Atlantic":
                    map = MapType.PRESSURE;
                    break;
            }
            if(map != null) {
                Image im = getImage(layer, defaultTime, timeStep);
                layers.put(map, im);
            }
        }

        return layers;
    }

    private static String cleanup(String inputData){
        String partCleanedData = inputData.replaceAll("@", "");

        int start = partCleanedData.indexOf('[');
        int end = partCleanedData.lastIndexOf(']');

        String cleanedData = partCleanedData.substring(start, end + 1);

        return cleanedData;
    }
}
