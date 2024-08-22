package com.example.demo.service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class WebScrapper {
    @Autowired
    private BddEditor bddEditor;

    private static final String USER_AGENT = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_11_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/50.0.2661.102 Safari/537.36";

    public Map<String, Object> queryProcess(String number) throws Exception {
        String radNumber = number;
        String consultaUrl = "https://consultaprocesos.ramajudicial.gov.co:448/api/v2/Procesos/Consulta/NumeroRadicacion?numero=" + radNumber + "&SoloActivos=false&pagina=1";
        JSONObject result = new JSONObject(sendGetRequest(consultaUrl));

        // Manejo de idProceso como Object
        Object idProcesoObject = result.getJSONArray("procesos").getJSONObject(0).opt("idProceso");
        String procesoId = idProcesoObject != null ? idProcesoObject.toString() : null;
        System.out.println("a"+procesoId);
        if (procesoId == null) {
            throw new Exception("ID del proceso no encontrado");
        }

        String detalleUrl = "https://consultaprocesos.ramajudicial.gov.co:448/api/v2/Proceso/Detalle/" + procesoId;
        JSONObject resultDetail = new JSONObject(sendGetRequest(detalleUrl));
        System.out.println("b");
        Map<String, Object> process = new HashMap<>();
        Map<String, Object> processDetails = new HashMap<>();
        processDetails.put("key_procces", radNumber);
        processDetails.put("source", resultDetail.optString("recurso", "N/A"));
        processDetails.put("radication_date", resultDetail.optString("fechaProceso", "N/A"));
        processDetails.put("office", resultDetail.optString("despacho", "N/A"));
        processDetails.put("speaker", resultDetail.optString("ponente", "N/A"));
        List<Map<String, String>> actions = new ArrayList<>();
        processDetails.put("actions", actions);
        process.put("process", processDetails);
        System.out.println("c");
        String actuacionesUrl = "https://consultaprocesos.ramajudicial.gov.co:448/api/v2/Proceso/Actuaciones/" + procesoId;
        JSONObject resultAction = new JSONObject(sendGetRequest(actuacionesUrl));
        int pages = resultAction.getJSONObject("paginacion").getInt("cantidadPaginas");
        System.out.println("d");
        for (int pag = 0; pag < pages; pag++) {
            if (pages > 1) {
                resultAction = new JSONObject(sendGetRequest(actuacionesUrl + "?pagina=" + (pag + 1)));
            }

            if (!resultAction.has("StatusCode")) {
                JSONArray actuaciones = resultAction.getJSONArray("actuaciones");
                for (int i = 0; i < actuaciones.length(); i++) {
                    JSONObject act = actuaciones.getJSONObject(i);
                    Map<String, String> action = new HashMap<>();
                    action.put("action_date", act.optString("fechaActuacion", "N/A"));
                    action.put("action", act.optString("actuacion", "N/A"));
                    action.put("annotation", act.optString("anotacion", "N/A"));
                    action.put("start_date", act.optString("fechaInicial", "N/A"));
                    action.put("end_date", act.optString("fechaFinal", "N/A"));
                    action.put("registration_date", act.optString("fechaRegistro", "N/A"));
                    actions.add(action);
                }
            }
        }
        bddEditor.printAndSaveActions(process,radNumber);
        return process;
    }

    private String sendGetRequest(String urlString) throws Exception {
        URI uri = new URI(urlString);
        URL url = uri.toURL();
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("User-Agent", USER_AGENT);
        int responseCode = connection.getResponseCode();

        if (responseCode == HttpURLConnection.HTTP_OK) {
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String inputLine;
            StringBuilder response = new StringBuilder();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
            return response.toString();
        } else {
            throw new Exception("GET request not worked");
        }
    }

    public void procesarBdd() {
        // Obtener la lista de números de radicado con estado 'activo'
        List<String> radicadosActivos = bddEditor.getRadicadosActivos();
    
        // Ordenar la lista en un array
        String[] radicadosArray = radicadosActivos.toArray(new String[0]);

        // Procesar cada radicado
        for (String radicado : radicadosArray) {
            int contador =0;
            System.out.println(radicado);
            try{
                this.queryProcess(radicado);
            }catch(Exception e){
                contador++;
                if(e.getMessage().contains("GET request not worked")){
                    System.out.println("error get");
                }else if(e.getMessage().contains("JSONArray[0]")){
                    System.out.println("error json");
                }
                //e.printStackTrace();
            }
            System.out.println("contador:"+contador);
        }
    }
    @Scheduled(cron = "0 0 5 * * ?")
    public void scheduledProcesarBdd() {
        try {
            procesarBdd();
        } catch (Exception e) {
            // Manejo de excepciones
            e.printStackTrace();
        }
    }
}

