package ml.shifu.shifu.di.builtin.miningSchema;

import ml.shifu.shifu.di.spi.MiningSchemaUpdater;
import ml.shifu.shifu.util.PMMLUtils;
import ml.shifu.shifu.util.Params;
import org.dmg.pmml.*;

import java.util.*;

public class IVVariableSelector implements MiningSchemaUpdater {

    public void update(Model model, Params params) {
        if (model.getMiningSchema() == null) {
            throw new RuntimeException("No MiningSchema found in model: " + model.getModelName() + ", create MiningSchema first.");
        }



        Map<FieldName, MiningField> miningFieldMap = PMMLUtils.getMiningFieldMap(model.getMiningSchema());

        List<UnivariateStats> statsList = new ArrayList<UnivariateStats>();

        MiningSchema updatedMiningSchema = new MiningSchema();



        for (UnivariateStats stats : model.getModelStats().getUnivariateStats()) {
            if (miningFieldMap.get(stats.getField()).getOptype().equals(OpType.CONTINUOUS)) {
                statsList.add(stats);
            }
        }

        Integer numSelected = (Integer)params.get("numSelected");
        if (numSelected > statsList.size()) {
            return;
        }

        Collections.sort(statsList, new Comparator<UnivariateStats>() {
            public int compare(UnivariateStats a, UnivariateStats b) {
                Double va = Double.valueOf(PMMLUtils.getExtension(a.getContStats().getExtensions(), "IV").getValue());
                Double vb = Double.valueOf(PMMLUtils.getExtension(b.getContStats().getExtensions(), "IV").getValue());
                return vb.compareTo(va);
            }
        });

        Set<FieldName> selectedFields = new HashSet<FieldName>();

        for (int i = 0; i < numSelected; i++) {
            selectedFields.add(statsList.get(i).getField());
        }

        for (MiningField miningField : model.getMiningSchema().getMiningFields()) {
            if (!miningField.getOptype().equals(OpType.CONTINUOUS) || selectedFields.contains(miningField.getName())) {
                updatedMiningSchema.withMiningFields(miningField);
            }
        }

        model.setMiningSchema(updatedMiningSchema);

    }



}
