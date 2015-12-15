package org.aksw.openqa.qald.benchmark;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.aksw.openqa.component.answerformulation.synthesizer.impl.ResourceSynthesizer;
import org.aksw.openqa.component.service.cache.impl.CacheService;
import org.aksw.openqa.main.OpenQA;
import org.aksw.openqa.manager.plugin.PluginManager;
import org.aksw.openqa.qald.QALDBenchmark;
import org.aksw.openqa.qald.QALDBenchmarkResult;
import org.aksw.openqa.qald.schema.Dataset;

public class Benchmark {	
	
	public static void main(String[] args) throws IOException {
		 String workingDir = System.getProperty("user.dir");
		 String qaldDirPath = workingDir + "/qaldFiles";
		 String pluginsPath = workingDir + "/plugins";
		 String resultFilesPath = workingDir + "/resultFiles";
		 File qaldDir = new File(qaldDirPath);
		 File resultDir = new File(resultFilesPath);
		 File[] qaldBenchmarkTests = qaldDir.listFiles(); // listing all QALD testing files
		 
		 // registering implementation combination
		 List<String> systemsIDsList = new ArrayList<String>();
//		 systemsIDsList.add("TBSLQueryParser v0.1.7-beta"); // TBSL
		 systemsIDsList.add("SINAQueryParser v0.1.7-beta"); // SINA 
//		 systemsIDsList.add("TBSLQueryParser v0.1.7-beta, SINAQueryParser v0.1.7-beta"); // TBSL & SINA
		 Map<String, List<Double>> systemsResults = benchmark(systemsIDsList, qaldBenchmarkTests, resultDir, pluginsPath, "en", "string");
		 
		 // printing results	 
		 System.out.println("\t\t\t Fmeasure \t Precision \t Recall");
		 for(Entry<String, List<Double>> systemResult : systemsResults.entrySet()) {
			 System.out.print(systemResult.getKey() + "\t" + 
						 	systemResult.getValue().get(0) + "\t" +
						 	systemResult.getValue().get(1) + "\t" +
						 	systemResult.getValue().get(2)
					 	);
			 System.out.println();
		 }
	}
	
	public static Map<String, List<Double>> benchmark(List<String> systemsIDsList, File[] qaldBenchmarkTests, File resultDir, String pluginDirPath, String lang, String queryType) throws IOException {
		Map<String, List<Double>> systemsResults = new HashMap<String, List<Double>>();

		for(String systemsIDs : systemsIDsList) {			
			PluginManager pluginManager = newPluginManager(pluginDirPath); // instantiate the plugin Manager with default components
			pluginManager.setActive(true, systemsIDs); // activate the current systems
			for(File qaldBenchmkarkTest : qaldBenchmarkTests) {
				QALDBenchmark qaldBenchmrk = new QALDBenchmark();
				Dataset systemAnswer;
				try {
					systemAnswer = qaldBenchmrk.evaluate(qaldBenchmkarkTest, lang, queryType, pluginManager);
					Dataset qaldQuestionAnswer = QALDBenchmark.deserialize(qaldBenchmkarkTest);
					QALDBenchmarkResult qaldBenchmarkResult = QALDBenchmark.evaluate(systemAnswer, qaldQuestionAnswer);
					File outputFile = new File(resultDir.getPath() + "/" + 
										systemsIDs.trim().replace(",", "-") + "." + qaldQuestionAnswer.getId().trim());
					QALDBenchmark.serialize(systemAnswer, outputFile);
					List<Double> results = new ArrayList<Double>();
					results.add(qaldBenchmarkResult.getFmeasure());
					results.add(qaldBenchmarkResult.getPrecision());
					results.add(qaldBenchmarkResult.getRecall());
					systemsResults.put(systemsIDs + " - " + qaldQuestionAnswer.getId().trim() , results); // adding the results
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		
		return systemsResults;
	}
	
	public static PluginManager newPluginManager(String defaultPluginDir) throws IOException {
		OpenQA openQA = new OpenQA();
		ClassLoader contextClassLoader = openQA.getClass().getClassLoader();
		PluginManager pluginManager = new PluginManager(defaultPluginDir, contextClassLoader);
		pluginManager.setActive(false); // deactivating all active components
		pluginManager.register(new ResourceSynthesizer(null)); // using default plug-in conf
		pluginManager.register(new CacheService(null)); // using default plug-in conf
		pluginManager.setActive(true, "RDFSynthesizer v0.1.7-beta", // activating the useful components (Synthesizer and Retriever) 
				"TriplestoreRetriever v0.1.7-beta",
				"undefined v0.1.7-beta");
		
		return pluginManager;
	}
	
	

}
