package ml.shifu.shifu.core.pmml;

import ml.shifu.shifu.combo.CsvFile;
import ml.shifu.shifu.core.pmml.builder.creator.AbstractSpecifCreator;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.PMML;
import org.jpmml.evaluator.*;
import org.jpmml.evaluator.mining.MiningModelEvaluator;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.*;

/**
 * Created by zhanhu on 2/8/17.
 */
public class PMMLScoreGenTest {

    public static final double EPS = 1e-6;

    //@Test
    public void testBaggingPmml() throws Exception {
        verifyPmml("/Users/zhanhu/temp/TestNN/pmmls/TestNN.pmml",
                "/Users/zhanhu/temp/TestNN/evals/Eval1/EvalScore",
                "|", "mean");
    }

    private void verifyPmml(String pmmlPath, String evalDataPath, String delimiter, String scoreField)
            throws Exception {
        CsvFile csvFile = new CsvFile(evalDataPath, delimiter, true);
        PMML pmml = PMMLUtils.loadPMML(pmmlPath);
        MiningModelEvaluator evaluator = new MiningModelEvaluator(pmml);

        int totalRecordCnt = 0;
        int matchRecordCnt = 0;

        Iterator<Map<String, String>> iterator = csvFile.iterator();
        while(iterator.hasNext()) {
            Map<String, String> rawInput = iterator.next();
            boolean match = scoreAndMatch(evaluator, rawInput, scoreField);
            totalRecordCnt++;
            if(match) {
                matchRecordCnt++;
            }
        }

        String result = (matchRecordCnt == totalRecordCnt) ? "SUCCESS" : "FAIL";
        System.out.println(result + "! " + matchRecordCnt + " out of " + totalRecordCnt + " are matched.");
        Assert.assertTrue(matchRecordCnt == totalRecordCnt);
    }

    private boolean scoreAndMatch(MiningModelEvaluator evaluator, Map<String, String> rawInput, String scoreName) {
        List<TargetField> targetFields = evaluator.getTargetFields();
        Map<FieldName, FieldValue> maps = convertRawIntoInput(evaluator, rawInput);
        List<Double> scores = new ArrayList<Double>();

        switch(evaluator.getModel().getMiningFunction()) {
            case REGRESSION:
                if(targetFields.size() == 1) {
                    Map<FieldName, Double> regressionTerm = (Map<FieldName, Double>) evaluator.evaluate(maps);
                    scores.add(regressionTerm.get(evaluator.getTargetField().getName()));
                } else {
                    Map<FieldName, Double> regressionTerm = (Map<FieldName, Double>) evaluator.evaluate(maps);
                    List<FieldName> outputFieldList = new ArrayList<FieldName>(regressionTerm.keySet());
                    Collections.sort(outputFieldList, new Comparator<FieldName>() {
                        @Override public int compare(FieldName a, FieldName b) {
                            return a.getValue().compareTo(b.getValue());
                        }
                    });
                    for(int i = 0; i < outputFieldList.size(); i++) {
                        FieldName fieldName = outputFieldList.get(i);
                        if(fieldName.getValue().startsWith(AbstractSpecifCreator.FINAL_RESULT)) {
                            scores.add(regressionTerm.get(fieldName));
                        }
                    }
                }
                break;
            case CLASSIFICATION:
                Map<FieldName, Classification<Double>> classificationTerm = (Map<FieldName, Classification<Double>>) evaluator
                        .evaluate(maps);
                for(Classification<Double> cMap : classificationTerm.values())
                    for(Map.Entry<String, Value<Double>> entry : cMap.getValues().entrySet())
                        System.out.println(entry.getValue().getValue() * 1000);
                break;
            default:
                break;
        }

        double expectScore = Double.parseDouble(rawInput.get(scoreName));
        return Math.abs(expectScore - scores.get(0)) < EPS;
    }

    private Map<FieldName, FieldValue> convertRawIntoInput(MiningModelEvaluator evaluator,
            Map<String, String> rawInput) {
        Map<FieldName, FieldValue> arguments = new HashMap<FieldName, FieldValue>();
        for(InputField inputField : evaluator.getInputFields()) {
            FieldName name = inputField.getName();
            if(rawInput.containsKey(name.getValue())) {
                arguments.put(inputField.getName(), CsvUtil.prepare(inputField, rawInput.get(name.getValue())));
            } else {
                arguments.put(inputField.getName(), CsvUtil.prepare(inputField, null));
            }
        }

        return arguments;
    }
}
