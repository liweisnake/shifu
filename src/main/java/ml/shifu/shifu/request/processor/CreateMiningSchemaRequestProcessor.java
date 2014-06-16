package ml.shifu.shifu.request.processor;

import com.google.inject.Guice;
import com.google.inject.Injector;
import ml.shifu.shifu.di.module.SimpleModule;
import ml.shifu.shifu.di.service.MiningSchemaService;
import ml.shifu.shifu.request.RequestObject;
import ml.shifu.shifu.util.PMMLUtils;
import org.dmg.pmml.*;

import java.util.Map;

public class CreateMiningSchemaRequestProcessor {



    public static void run(RequestObject req) throws Exception {


        String pathPMML = (String) req.getParams().get("pathPMML");

        SimpleModule module = new SimpleModule();
        module.setBindings((Map<String, String>) req.getParams().get("bindings"));
        Injector injector = Guice.createInjector(module);
        MiningSchemaService service = injector.getInstance(MiningSchemaService.class);

        PMML pmml = PMMLUtils.loadPMML(pathPMML);

        String selectedModelName = (String) req.getParams().get("modelName", null);

        for (Model model : pmml.getModels()) {
            if (selectedModelName == null || model.getModelName().equalsIgnoreCase(selectedModelName)) {
                model.setMiningSchema(service.createMiningSchema(model, pmml, req));
            }
        }

        PMMLUtils.savePMML(pmml, (String) req.getParams().get("pathPMMLOutput", pathPMML));


    }
}
